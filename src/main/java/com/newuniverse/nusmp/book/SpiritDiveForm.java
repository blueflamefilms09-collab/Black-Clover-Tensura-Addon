package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.99.0: Spirit Dive as the FORM of the four spirit magics (Fire: Salamander, Water: Undine, Wind: Sylph, Earth: an unnamed earth spirit; {@link ArmourPolicy} kind FORM). The dive is
 * entered and left by its own page (GrimoireBook#spiritDive), never by an ordinary cast:
 * <ul>
 *   <li><b>Gauge.</b> The upkeep the dive always had (2 percent of the magicule pool a second, and the Spirit Lord contract must hold) is now matched by the spirit's ambient intake:
 *       every second the dive takes the policy's {@code absorb} share of the pool back from the natural mana around (config {@code armour.diveAbsorbScale}); the dive lasts as long as
 *       the pool can pay the difference instead of the old fixed 8 seconds.</li>
 *   <li><b>Stats.</b> The form bonus of the policy (speed, attack, a share of every blow cut, hidden night vision for the senses) while the dive lasts; the first dives of a mage
 *       are HALF dives at {@link ArmourRules#HALF_SCALE} strength ({@link ArmourRules#HALF_DIVES} dives or mastery make them full), Yuno's one-wing first dive.</li>
 *   <li><b>Look.</b> Until 1.98 Fire, Wind and Water drew the transformation overlay of {@link ModeArmor} and Earth the Gnome Plate. 1.99.0 (Great Spirits) draws every dive with its own render layer,
 *       {@code Aura.SPIRIT_DIVE} (client.aura.SpiritDiveAura): the dive keeps that layer alive with the same lease the armours use (60 more ticks every second, 20 more ticks when it ends so
 *       the pieces shatter out), and the style number of the lease carries element and stage ({@link SpiritDivePieces#style}). Wind first dive: only the LEFT half transforms (one wing, half a crown,
 *       a ring round the wrist); the full dive has two wings and a full crown; a half dive becomes full (the missing half forges in) when the book is mastered during the dive. Water at the full
 *       stage is the Saint Valkyrie Dress. Any other armour the mage wears gives way to the dive. The old overlays stay registered ({@link GrimoireBook#diveArmor} is null while
 *       {@link SpiritDivePieces#LOOK} is on) and the policy's armour layer of Earth (the Gnome Plate) is not started by the dive any more.</li>
 * </ul>
 */
public final class SpiritDiveForm {
    private SpiritDiveForm() {}

    /** The id of the universal Spirit Dive page. */
    public static final String PAGE = "spirit_dive";
    /** How far ahead the dive is renewed every successful second (it can never lapse between two upkeep ticks). */
    static final int LEASE_TICKS = 60;
    static final String DIVES = "DivesDone";
    /** Set in the book's tag when a half dive became full during the dive (the look upgrades: the missing half forges in); cleared at the start of every dive. */
    static final String UPGRADED = "DiveUpgraded";

    private static ArmourPolicy.Entry entry(GrimoireBook b) {
        ArmourPolicy.Entry e = ArmourPolicy.get().entry(b.magicNow().name());
        return e != null && e.kind() == ArmourPolicy.Kind.FORM && e.formPages().contains(PAGE) ? e : null;
    }

    /** The dive has just begun (all the page's checks passed). */
    public static void begin(ServerPlayer p, GrimoireBook b, ManasSkillInstance i) { start(p, b, i, true); }

    private static void start(ServerPlayer p, GrimoireBook b, ManasSkillInstance i, boolean counts) {
        ArmourPolicy.Entry e = entry(b);
        if (e == null) return;
        var tag = i.getOrCreateTag();
        int done = tag.getInt(DIVES);
        if (counts) tag.putInt(DIVES, Math.min(1000, done + 1));
        float stage = ArmourRules.diveScale(done, i.isMastered(p));
        ArmourRules.Held held = ArmourState.held(p);
        SpiritDivePieces.Element el = SpiritDivePieces.elementOf(e.magic());
        boolean ownLook = el != null;                                                                       // 1.99.0: the dive draws Aura.SPIRIT_DIVE itself, never the armour layer of its magic
        int style = 0;
        switch (ArmourRules.formStart(held, e.magic())) {
            case SWAP -> ArmourState.takeOff(p);
            case RESTYLE -> { if (e.formAura() && !ownLook && held != null) style = held.style(); else ArmourState.takeOff(p); }   // the layer of the form is the armour's: it stays (not for the four spirit dives)
            default -> { }
        }
        if (style == 0 && e.formAura() && !ownLook) style = e.armourPages().values().stream().findFirst().orElse(0);
        tag.remove(UPGRADED);
        ArmourState.putOn(p, ownLook ? withoutAura(e) : e, style, true, stage);
        if (el != null) look(p, el, SpiritDivePieces.stageFor(stage), LEASE_TICKS);
    }

    /** The policy entry without the form's armour layer (Earth's Gnome Plate): the dive puts the stats on and draws its own layer. */
    static ArmourPolicy.Entry withoutAura(ArmourPolicy.Entry e) {
        if (!e.formAura()) return e;
        return new ArmourPolicy.Entry(e.magic(), e.kind(), e.name(), e.formName(), e.aura(), false, e.armourPages(), e.formPages(), e.externalPages(), e.styleBy(), e.bonus(), e.formBonus(),
                e.legacyAuras(), e.shimmer(), e.newPages(), e.canon());
    }

    // ------------------------------------------------------------------ the look: Aura.SPIRIT_DIVE
    /** Tells the clients that see the player that the dive of this element and stage runs for {@code ticks} more ticks (a fake player without a connection only updates the server table). */
    static void look(ServerPlayer p, SpiritDivePieces.Element el, SpiritDivePieces.Stage st, int ticks) {
        try { PlayerAuras.set(p, Aura.SPIRIT_DIVE, ticks, SpiritDivePieces.style(el, st)); } catch (NullPointerException noConnection) { /* nothing to send */ }
    }

    /** The stage held in the armour state (the scale it was put on with: below 1 is a half dive). */
    static float storedStage(ServerPlayer p) {
        CompoundTag a = p.getPersistentData().getCompound(ArmourState.KEY);
        return a.contains("G") ? a.getFloat("G") : 1f;
    }

    /** The stage the look shows now: HALF while the stored scale is below 1, UPGRADE once a half dive became full during the dive, otherwise FULL. */
    static SpiritDivePieces.Stage stageNow(ServerPlayer p, ManasSkillInstance i) {
        if (storedStage(p) < 1f) return SpiritDivePieces.Stage.HALF;
        return i.getOrCreateTag().getBoolean(UPGRADED) ? SpiritDivePieces.Stage.UPGRADE : SpiritDivePieces.Stage.FULL;
    }

    /** The mastery came during a half dive: the stats go to full and the missing half of the look forges in. */
    private static void upgrade(ServerPlayer p, ArmourPolicy.Entry e, ManasSkillInstance i) {
        ArmourRules.Held h = ArmourState.held(p);
        i.getOrCreateTag().putBoolean(UPGRADED, true);
        ArmourState.putOn(p, withoutAura(e), h == null ? 0 : h.style(), true, 1f);
        p.displayClientMessage(Component.translatableWithFallback("nusmp.dive.upgrade", "The spirit's other half joins you: the full dive.").withStyle(ChatFormatting.AQUA), true);
    }

    /** The dive has ended (recast, the gauge ran dry, the spirit let go). */
    public static void end(ServerPlayer p, GrimoireBook b) {
        if (ArmourState.inForm(p, b.magicNow())) ArmourState.takeOff(p);
    }

    /**
     * The form was ended from outside the dive (the wearer died, was knocked out, lost the book): the dive itself ends with it, so its renewal can not bring the form back and the
     * overlay goes. Called by {@link ArmourState} when a form ends; it never starts the spirit lock (that belongs to a dive that was left on purpose or ran dry).
     */
    static void stop(ServerPlayer p, String magic) {
        try {
            if (SpiritDivePieces.elementOf(magic) != null) endLook(p);                                   // 1.99.0: the pieces shatter out (or vanish at once when the wearer died)
            com.newuniverse.nusmp.blackclover.MagicType m = com.newuniverse.nusmp.blackclover.MagicType.lookup(magic);
            GrimoireBook book = m == null ? null : com.newuniverse.nusmp.skill.NUSkills.bookFor(m);
            if (book == null) return;
            long now = p.level().getGameTime();
            io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(p).getSkill(book.getRegistryName()).ifPresent(i -> {
                if (now < i.getOrCreateTag().getLong("DiveUntil")) i.getOrCreateTag().putLong("DiveUntil", now);
            });
            ModeArmor.Mode overlay = book.diveArmor();
            if (overlay != null) ModeArmor.stop(p, overlay);
        } catch (RuntimeException | LinkageError ex) {
            org.slf4j.LoggerFactory.getLogger("nusmp").warn("[nusmp] spirit dive: could not end the dive of {}", magic, ex);
        }
    }

    /** The look ends: the pieces shatter out over the 20 ticks (the style stays, so the clients keep the start), or at once when the wearer died. */
    private static void endLook(ServerPlayer p) {
        if (!PlayerAuras.active(p, Aura.SPIRIT_DIVE)) return;
        int style = PlayerAuras.style(p, Aura.SPIRIT_DIVE);
        try {
            if (p.isAlive()) PlayerAuras.set(p, Aura.SPIRIT_DIVE, ArmourRules.SHATTER_TICKS, style);
            else PlayerAuras.clear(p, Aura.SPIRIT_DIVE);
        } catch (NullPointerException noConnection) { /* a fake player: the table is already written */ }
    }

    /**
     * Once a second while the dive holds (its upkeep was paid): the ambient intake, and the renewal of the dive and of its overlay. Returns nothing; the caller already paid the upkeep.
     */
    public static void second(ServerPlayer p, GrimoireBook b, ManasSkillInstance i, long now) {
        try {
            secondChecked(p, b, i, now);
        } catch (RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] spirit dive: the once-a-second step failed", ex);      // the upkeep is already paid: never break the player tick
        }
    }

    private static void secondChecked(ServerPlayer p, GrimoireBook b, ManasSkillInstance i, long now) {
        ArmourPolicy.Entry e = entry(b);
        if (e == null) return;
        double absorb = e.formBonus().absorb() * ArmourConfig.diveAbsorbScale();
        if (absorb > 0.0 && !p.isCreative()) {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null) {
                double max = EnergyHelper.getMaxMagicule(p);
                ex.setMagicule(ArmourRules.manaAfterSecond(ex.getMagicule(), max, 0.0, max * absorb));
                ex.markDirty();
            }
        }
        i.getOrCreateTag().putLong("DiveUntil", now + LEASE_TICKS);                                      // held as long as the upkeep is paid
        ModeArmor.Mode overlay = b.diveArmor();
        if (overlay != null && (ModeArmor.active(p) == overlay || ModeArmor.active(p) == ModeArmor.Mode.NONE)) ModeArmor.start(p, overlay, LEASE_TICKS);
        if (ArmourState.held(p) == null) start(p, b, i, false);                                           // the form is on, but its state was lost (a relog inside the lease): restore it
        SpiritDivePieces.Element el = SpiritDivePieces.elementOf(e.magic());
        if (el != null && ArmourState.held(p) != null) {
            if (storedStage(p) < 1f && i.isMastered(p)) upgrade(p, e, i);                                  // the book was mastered during a half dive: the full dive
            look(p, el, stageNow(p, i), LEASE_TICKS);                                                      // the lease of the look, renewed with the dive
        }
    }
}
