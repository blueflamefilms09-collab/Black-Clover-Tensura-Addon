package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the transformation engine (3.65.0, Stage 4). It knows nothing about any single form: every number comes from the {@link SaiyanForms} table (and the server's overrides in {@link SaiyanConfig}), so a
 * new form is a new row. It EARNS forms (a check every five seconds against the unlock rules: Battle Power, Zenkai breakthroughs, training, the mastery of the parent forms, the Goku Ultimate for the higher
 * branches, an operator flag for the Limit Breaker), ENTERS and LEAVES them (Ki to enter, Ki and stamina every second, a cool-down between forms), applies the speed change as transient attribute modifiers
 * ({@link SaiyanAttributes#applyForm}), hands the damage and defence factors to the combat code, tracks the Kaio-ken strain, and counts MASTERY from real fighting seconds under a daily cap (no idle
 * farming). Cost: one upkeep a second per transformed Saiyan, a three-particle aura twice a second, no entity.
 */
final class SaiyanFormEngine {
    private SaiyanFormEngine() {}

    static SaiyanForms.Form active(SaiyanManager.Run r) { return r.form.isEmpty() ? null : SaiyanForms.byId(r.form); }

    private static double effect(SaiyanState s, SaiyanForms.Form f) { return SaiyanForms.effect(s.mastery(f.id())); }

    /** The outgoing damage factor of the active form (1.0 in Base). */
    static double damageFactor(SaiyanState s, SaiyanManager.Run r) {
        SaiyanForms.Form f = active(r);
        if (f == null) return 1.0;
        double bonus = SaiyanConfig.formDamage(f) * effect(s, f);
        if (f == SaiyanForms.KAIOKEN) bonus *= SaiyanForms.kaioDamageMul(r.kaioStage);
        return 1.0 + bonus;
    }

    /** The share of incoming damage that still goes through in the active form (1.0 in Base). */
    static double takenFactor(SaiyanState s, SaiyanManager.Run r) {
        SaiyanForms.Form f = active(r);
        return f == null ? 1.0 : 1.0 / (1.0 + SaiyanConfig.formDefense(f) * effect(s, f));
    }

    /** The unlocked forms in roster order. */
    static List<SaiyanForms.Form> unlockedList(SaiyanState s) {
        List<SaiyanForms.Form> out = new ArrayList<>();
        for (SaiyanForms.Form f : SaiyanForms.ALL) if (s.unlocked.contains(f.id())) out.add(f);
        return out;
    }

    private static void msg(ServerPlayer p, String key, SaiyanForms.Form f, boolean overlay) {
        p.displayClientMessage(Component.translatable("nusmp.saiyan.msg." + key, Component.translatable(f.key())), overlay);
    }

    // ------------------------------------------------------------------ the keys
    /** The toggle key: leave the active form, or enter the selected one (the first unlocked if none is selected). */
    static boolean toggle(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        SaiyanForms.Form on = active(r);
        if (on != null) {
            deactivate(p, s, r, now, "form_off");
            return true;
        }
        if (!SaiyanConfig.formsEnabled()) {
            SaiyanAbilities.say(p, "forms_off");
            return false;
        }
        SaiyanForms.Form pick = SaiyanForms.byId(s.selectedForm);
        if (pick == null || !s.unlocked.contains(pick.id())) {
            List<SaiyanForms.Form> list = unlockedList(s);
            if (list.isEmpty()) {
                SaiyanAbilities.say(p, "no_forms");
                return false;
            }
            pick = list.get(0);
            s.selectedForm = pick.id();
        }
        return activate(p, s, r, pick, now, true);
    }

    /** The next / previous key: chooses among the unlocked forms (and switches at once if one is active). */
    static boolean cycle(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now, int dir) {
        List<SaiyanForms.Form> list = unlockedList(s);
        if (list.isEmpty()) {
            SaiyanAbilities.say(p, "no_forms");
            return false;
        }
        int at = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(s.selectedForm)) at = i;
        int to = at < 0 ? 0 : Math.floorMod(at + (dir >= 0 ? 1 : -1), list.size());
        SaiyanForms.Form f = list.get(to);
        s.selectedForm = f.id();
        r.dirty = true;
        if (active(r) != null && !f.id().equals(r.form)) return activate(p, s, r, f, now, false);
        msg(p, "form_selected", f, true);
        return true;
    }

    // ------------------------------------------------------------------ entering and leaving
    /** Enters a form if every condition holds (says why not otherwise). {@code fromBase}: the cool-down between forms applies. */
    static boolean activate(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, SaiyanForms.Form f, long now, boolean fromBase) {
        if (!SaiyanConfig.formsEnabled()) {
            SaiyanAbilities.say(p, "forms_off");
            return false;
        }
        if (!s.awakened) {
            SaiyanAbilities.say(p, "not_awakened");
            return false;
        }
        if (!s.unlocked.contains(f.id())) {
            msg(p, "form_locked", f, true);
            return false;
        }
        if (f.goku() && !GokuAbilities.isGoku(s)) {
            msg(p, "form_goku", f, true);
            return false;
        }
        if (p.isSpectator()) return false;
        if (fromBase && now < r.formReadyAt) {
            SaiyanAbilities.say(p, "form_cooldown");
            return false;
        }
        if (!p.isCreative() && s.stamina <= 0.0) {
            SaiyanAbilities.say(p, "no_stamina");
            return false;
        }
        if (!SaiyanKi.pay(p, f.kiCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        r.form = f.id();
        r.kaioStage = f == SaiyanForms.KAIOKEN ? SaiyanForms.kaioMaxStage(s.mastery(f.id())) : 1;
        double e = effect(s, f);
        SaiyanAttributes.applyForm(p, f.speed() * e, f.attackSpeed() * e);
        r.dirty = true;
        msg(p, "form_on", f, true);
        var level = p.serverLevel();
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.SPARK, f.rgb() & 0xFFFFFF, 1.2f, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.5, 0.9, 0.5, 0.12);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.3f);
        return true;
    }

    /** Leaves the active form (a reason key says why); the cool-down before another form starts. */
    static void deactivate(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now, String why) {
        SaiyanForms.Form f = active(r);
        r.form = "";
        r.kaioStage = 1;
        SaiyanAttributes.clearForm(p);
        r.dirty = true;
        if (f == null) return;
        r.formReadyAt = now + f.cooldown();
        msg(p, why, f, true);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    /** The silent version for death, logout and race removal (the attributes are cleared by the caller). */
    static void reset(SaiyanManager.Run r) {
        r.form = "";
        r.kaioStage = 1;
    }

    // ------------------------------------------------------------------ the upkeep
    static void second(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        SaiyanForms.Form f = active(r);
        boolean kaio = f == SaiyanForms.KAIOKEN;
        double before = s.strain;
        s.strain = SaiyanForms.strainAfter(s.strain, kaio, r.kaioStage, 1.0);
        if ((int) (before / 5.0) != (int) (s.strain / 5.0)) r.dirty = true;
        if (f != null) upkeep(p, s, r, now, f, kaio);
        if ((p.tickCount % 100) == 0 && SaiyanConfig.formsEnabled()) unlockCheck(p, s, r);
    }

    private static void upkeep(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now, SaiyanForms.Form f, boolean kaio) {
        if (!SaiyanConfig.formsEnabled() || !s.awakened || p.isSpectator()) {
            deactivate(p, s, r, now, "form_off");
            return;
        }
        double mastery = s.mastery(f.id());
        double pct = SaiyanConfig.formDrain(f) * SaiyanForms.drainFactor(mastery) * (kaio ? SaiyanForms.kaioDrainMul(r.kaioStage) : 1.0);
        if (!SaiyanKi.pay(p, pct)) {
            deactivate(p, s, r, now, "form_exhausted");
            return;
        }
        if (!p.isCreative() && f.staminaDrain() > 0.0) {
            s.stamina = SaiyanRules.stamina(s.stamina, -f.staminaDrain(), SaiyanManager.staminaMax(s));
            if (s.stamina <= 0.0) {
                deactivate(p, s, r, now, "form_tired");
                return;
            }
        }
        if (kaio && s.strain >= 100.0) {
            deactivate(p, s, r, now, "form_strain");
            return;
        }
        if (now - r.lastCombat <= 200) {
            double secs = Math.min(1.0, s.formRoom(now, SaiyanConfig.formMasteryCap()));
            if (secs > 0.0) {
                s.formSecondsToday += secs;
                s.addMastery(f.id(), SaiyanForms.masteryGain(secs, f.masterySeconds()));
                if (mastery < 1.0 && s.mastery(f.id()) >= 1.0) msg(p, "form_mastered", f, false);
                r.dirty = true;
            }
        }
    }

    /** Every five seconds: forms whose conditions are all met become the Saiyan's for good. */
    static void unlockCheck(ServerPlayer p, SaiyanState s, SaiyanManager.Run r) {
        SaiyanForms.Progress pr = s.progress();
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            if (s.unlocked.contains(f.id()) || !SaiyanForms.meets(f, SaiyanConfig.formMinBp(f), pr)) continue;
            if (s.unlock(f.id())) {
                r.dirty = true;
                msg(p, "form_unlocked", f, false);
                p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.0f);
            }
        }
    }

    /** The aura: a few tinted motes round the body, twice a second (the Fx budgets cap it further). */
    static void aura(ServerPlayer p, SaiyanManager.Run r) {
        SaiyanForms.Form f = active(r);
        if (f == null) return;
        Fx.sprayTinted(p.serverLevel(), FxThemes.WORLD, FxSlot.MOTE, f.rgb() & 0xFFFFFF, 1.0f, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.35, 0.8, 0.35, 0.02);
        if (f.aura() == SaiyanForms.Aura.ELECTRIC) Fx.spray(p.serverLevel(), FxThemes.WORLD, FxOld.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.8, 0.4, 0.05);
    }

    /** The share of the maximum stamina taken by the Kaio-ken strain, as the stamina code reads it. */
    static double strainPenalty(SaiyanState s) { return SaiyanForms.strainPenalty(s.strain); }
}
