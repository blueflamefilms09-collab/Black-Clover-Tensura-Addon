package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.ModeArmor;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
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
 *   <li><b>Look.</b> Fire, Wind and Water keep the transformation overlay of {@link ModeArmor} (clawed gauntlets and dragon frame, wings and crown, Valkyrie plates and avian wings);
 *       Earth is drawn with its armour layer (the Gnome Plate). Any other armour the mage wears gives way to the dive.</li>
 * </ul>
 */
public final class SpiritDiveForm {
    private SpiritDiveForm() {}

    /** The id of the universal Spirit Dive page. */
    public static final String PAGE = "spirit_dive";
    /** How far ahead the dive is renewed every successful second (it can never lapse between two upkeep ticks). */
    static final int LEASE_TICKS = 60;
    static final String DIVES = "DivesDone";

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
        int style = 0;
        switch (ArmourRules.formStart(held, e.magic())) {
            case SWAP -> ArmourState.takeOff(p);
            case RESTYLE -> { if (e.formAura() && held != null) style = held.style(); else ArmourState.takeOff(p); }   // the layer of the form is the armour's: it stays
            default -> { }
        }
        if (style == 0 && e.formAura()) style = e.armourPages().values().stream().findFirst().orElse(0);
        ArmourState.putOn(p, e, style, true, stage);
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
    }
}
