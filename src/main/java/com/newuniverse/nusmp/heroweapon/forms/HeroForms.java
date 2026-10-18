package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: every system needs the forms of a weapon and the form a bond is in, and none of them should know where the forms come from (the data pack on a server, a
 * packet on a remote client, a synthetic base form when a weapon has no file). This is the door. The reading calls ({@link #of}, {@link #get}, {@link #active}) are
 * cheap and never throw: the lists are never empty, the base form is first, a bound bond always has an active form. The acting calls are the ones the core makes
 * when the wielder changes form ({@link #setActive}, {@link #cycle}), when the loadout must be brought up to date ({@link #refresh}) and for the sneak + use route
 * of absorption ({@link #absorbOffhand}); the rules of each are in its Javadoc, and the work is in {@link FormLoadout}, {@link HeroAbsorb} and {@link FormLearning}.
 */
public final class HeroForms {
    private HeroForms() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** All forms of a weapon, the base form first, then by tier. Never empty (a null weapon gets an empty list). */
    public static List<HeroForm> of(HeroKind kind) { return kind == null ? List.of() : FormsStore.forms(kind); }

    /** The form with this id, or null. */
    public static HeroForm get(HeroKind kind, String id) {
        if (id == null) return null;
        for (HeroForm f : of(kind)) if (f.id().equals(id)) return f;
        return null;
    }

    /** The bond's active form; the base form when the id is unknown. Never null for a bound bond (null for a bond with no weapon). */
    public static HeroForm active(HeroBond bond) {
        if (bond.kind() == null) return null;
        HeroForm f = get(bond.kind(), bond.form());
        return f != null ? f : of(bond.kind()).get(0);
    }

    /** Client side: forget the forms a server sent (the player left the world); a later world sends its own. */
    public static void forgetSynced() { FormsStore.clearSynced(); }

    // ------------------------------------------------------------------------------------------------ the calls the core makes
    /** Makes a learned form the active one; false (with a line on the action bar saying why) when refused. Keeps the hand in step ({@link #refresh}). */
    public static boolean setActive(ServerPlayer player, String formId) {
        Component why = trySetActive(player, formId);
        if (why != null) FormsFeedback.bar(player, why);
        return why == null;
    }

    /**
     * The rules of changing form, with the reason as a result: {@code null} when the form is now active (or already was), else the line to show. Refused: no weapon, an
     * unknown form, a form not learned, a curse form whose series is not unlocked, a change while {@link HeroCurse#locked}, and a change within
     * {@code techniqueLockTicks} of a technique.
     */
    public static Component trySetActive(ServerPlayer player, String formId) { return change(player, formId, false); }

    /**
     * For the curse system when an Overrun ends: the same change without the two locks (the curse lock and the technique lock), because it is the curse itself that is letting
     * go. An unknown form, a form not learned and a sealed curse form are still refused. Returns true when the form is now active.
     */
    public static boolean forceActive(ServerPlayer player, String formId) { return change(player, formId, true) == null; }

    private static Component change(ServerPlayer player, String formId, boolean force) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound()) return FormsLang.t("hero.forms.no_weapon", "You have no hero weapon.");
        HeroForm to = get(bond.kind(), formId);
        if (to == null) return FormsLang.t("hero.forms.unknown", "There is no form '%s'.", String.valueOf(formId));
        if (!bond.isUnlocked(to.id())) return FormsLang.t("hero.forms.not_learned", "%s is not learned yet.", to.name());
        HeroForm from = active(bond);
        if (from.id().equals(to.id()) && bond.form().equals(to.id())) return null;
        if (FormTree.sealed(to, CurseLink.series(bond)))
            return FormsLang.t("hero.forms.sealed", "The %s series is sealed.", to.sin() == null ? "curse" : to.sin().label());
        if (!force) {
            if (HeroCurse.locked(player)) return FormsLang.t("hero.forms.curse_locked", "The weapon will not let you change form now.");
            int wait = FormLocks.remaining(player.getUUID(), player.level().getGameTime(), FormsConfig.get(FormsConfig.TECHNIQUE_LOCK_TICKS));
            if (wait > 0) return FormsLang.t("hero.forms.technique_lock", "You cannot change form for %s after a technique.", FormText.seconds(wait));
        }
        bond.setForm(to.id());
        refresh(player);
        FormsSync.markDirty(player);
        try {
            HeroCtx c = HeroCtx.of(player);
            if (c != null) HeroLogics.of(bond.kind()).onFormChanged(c, from, to);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] hero forms: onFormChanged of {} failed", bond.kind(), e);
        }
        return null;
    }

    /**
     * The hero key "cycle form": the next learned form after the active one in tree order (curse forms only when their series is unlocked, and only if the config
     * {@code cycleCurseForms} allows). Returns the new active form id, or null when there is nothing to change to or the change was refused; a refusal puts its reason on
     * the action bar at the end of the tick, so a "no other form" line the caller prints on null does not hide it.
     */
    public static String cycle(ServerPlayer player) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound()) return null;
        List<HeroForm> forms = of(bond.kind());
        HeroForm now = active(bond);
        int at = 0;
        for (int i = 0; i < forms.size(); i++) if (forms.get(i).id().equals(now.id())) at = i;
        boolean curses = FormsConfig.get(FormsConfig.CYCLE_CURSE_FORMS);
        for (int step = 1; step < forms.size(); step++) {
            HeroForm f = forms.get((at + step) % forms.size());
            if (!bond.isUnlocked(f.id())) continue;
            if (f.curse() && (!curses || FormTree.sealed(f, CurseLink.series(bond)))) continue;
            Component why = trySetActive(player, f.id());
            if (why == null) {
                FormsFeedback.bar(player, FormsLang.t("hero.forms.now", "Form: %s", f.name()));
                return f.id();
            }
            FormsFeedback.barLater(player, why);                    // a lock applies to every candidate alike: stop at the first; said last in the tick, after the core's own "no other form" line
            return null;
        }
        return null;
    }

    /**
     * Re-applies everything the active form puts on the wielder: the attribute modifiers while the weapon is summoned (and none when it is not), the permanent ability
     * bonuses of finished forms (always), and the {@code nusmp:hero_look} component of the summoned stack. Idempotent and cheap; the core calls it on summon,
     * dismiss, login, respawn and after any change of form or bond (this system also calls it on those events itself).
     */
    public static void refresh(ServerPlayer player) { FormLoadout.refresh(player); }

    /**
     * The sneak + use route of absorption: the stack in the OFF hand is absorbed into the gem when some form of the bonded weapon asks for the item (or the config lets
     * the gem take anything). Returns true when something was absorbed (the core then does not fire the sneak technique); false otherwise.
     */
    public static boolean absorbOffhand(ServerPlayer player) { return HeroAbsorb.offhand(player); }
}
