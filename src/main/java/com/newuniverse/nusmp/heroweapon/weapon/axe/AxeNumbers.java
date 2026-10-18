package com.newuniverse.nusmp.heroweapon.weapon.axe;

import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec.Kind;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Why: every number of the Vassal Axe in one pure table, so the owner's balance budget (design.md section 5) can be checked by a unit test and a later edit that breaks it
 * fails the build instead of the game. The Axe is the cleaving weapon: wide arcs that hit many foes, a heavy line that cuts through a crowd, a thrown axe that comes back, and an
 * execution that finishes a wounded foe. Its canon power-up is muscle enhancement (body strengthening); here that is BRAWN, a build-up the wielder earns by fighting and spends
 * on Shine Axe Burst (see {@link BrawnMath}). A table row is the highest raw damage ONE hit can reach before Brawn; Brawn can raise a hit by up to 16 percent, but every hit is
 * clamped to the cap of its kind, so the budget holds whatever the wielder has stacked. Distances are blocks, times are ticks, the shares are of the wielder's maximum magicules.
 */
public final class AxeNumbers {
    private AxeNumbers() {}

    // ------------------------------------------------------------------ the techniques (budget rows)
    public static final SkillSpec CRESCENT_BREAK = new SkillSpec("axe.crescent_break", Kind.DAMAGE, 8, 1, 70, 0.015, 12);
    public static final SkillSpec POWER_BREAK = new SkillSpec("axe.power_break", Kind.HEAVY, 10, 1, 110, 0.02, 1);
    /** Four pulses of 3.5. */
    public static final SkillSpec TORNADO_AXE = new SkillSpec("axe.tornado_axe", Kind.DAMAGE, 3.5, 4, 140, 0.025, 12);
    public static final SkillSpec MOUNTAIN_BREAK = new SkillSpec("axe.mountain_break", Kind.HEAVY, 12, 1, 120, 0.025, 12);
    /** The shoulder charge on the way is 5, the smash at the end 9. */
    public static final SkillSpec ACCELERATOR_SMASH = new SkillSpec("axe.accelerator_smash", Kind.DAMAGE, 9, 2, 120, 0.02, 12);
    /** Seven going out, five coming back. */
    public static final SkillSpec AXE_THROW = new SkillSpec("axe.axe_throw", Kind.DAMAGE, 7, 2, 100, 0.02, 4);
    /** Nine on a healthy foe ({@link #GUILLOTINE_BASE}), 19.8 on a wounded one (see {@link ExecuteMath}); the row is the higher, and a hit is clamped to the heavy cap of 20. */
    public static final SkillSpec GUILLOTINE = new SkillSpec("axe.guillotine", Kind.HEAVY, 19.8, 1, 110, 0.02, 1);
    public static final float GUILLOTINE_BASE = 9f;
    /** Fourteen plus 2.2 for each Brawn, so 31.6 at eight; see {@link BrawnMath#shineRaw}. */
    public static final SkillSpec SHINE_AXE_BURST = new SkillSpec("axe.shine_axe_burst", Kind.ULTIMATE, 31.6, 1, 900, 0.07, 12);
    /** The curse form's technique: eleven, then a wound of five a moment later. */
    public static final SkillSpec CURSED_PENETRATION = new SkillSpec("axe.cursed_penetration", Kind.HEAVY, 11, 2, 120, 0.03, 1);

    public static final List<SkillSpec> SPECS = List.of(CRESCENT_BREAK, POWER_BREAK, TORNADO_AXE, MOUNTAIN_BREAK, ACCELERATOR_SMASH, AXE_THROW, GUILLOTINE, SHINE_AXE_BURST, CURSED_PENETRATION);
    public static final Set<String> IDS = SPECS.stream().map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());
    public static final Set<String> ULTIMATES = SPECS.stream().filter(SkillSpec::ultimate).map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());

    // ------------------------------------------------------------------ the passives (ids the forms file lists)
    public static final String MUSCLE = "muscle_enhancement", CLEAVING = "cleaving", HEADSMAN = "headsman", RADIANCE = "radiance", SELF_REPAIR = "self_repair";
    public static final Set<String> PASSIVES = Set.of(MUSCLE, CLEAVING, HEADSMAN, RADIANCE, SELF_REPAIR);

    // ------------------------------------------------------------------ shapes (blocks) and timing (ticks)
    /** Crescent Break: the reach and the half-angle of the sweep (a half circle), and the ticks of warning. */
    public static final double CRESCENT_RANGE = 4.6, CRESCENT_HALF_ANGLE = 90.0;
    public static final int CRESCENT_DELAY = 6;
    /** Power Break: the reach, the warning, how long it weakens and slows. */
    public static final double POWER_RANGE = 4.4;
    public static final int POWER_DELAY = 7, POWER_WEAK_TICKS = 100, POWER_SLOW_TICKS = 60;
    /** Tornado Axe: the radius of the spin, the length of it and the ticks between two pulses. */
    public static final double TORNADO_RADIUS = 3.8;
    public static final int TORNADO_TICKS = 24, TORNADO_GAP = 6;
    /** Mountain Break: the length of the cut, its half-width, the warning. */
    public static final double MOUNTAIN_LENGTH = 12.0, MOUNTAIN_HALF_WIDTH = 1.1;
    public static final int MOUNTAIN_DELAY = 10;
    /** Accelerator Smash: the run, the half-width of the shoulder charge, the damage of the charge, the radius of the smash, the ticks from the dash to the smash. */
    public static final double SMASH_RUN = 6.0, SMASH_HALF_WIDTH = 1.3, SMASH_RADIUS = 3.0;
    public static final float SMASH_CHARGE_RAW = 5f;
    public static final int SMASH_DELAY = 7;
    /** Axe Throw: the flight (blocks, blocks a tick), the half-width of the flight line, the damage of each leg. */
    public static final double THROW_RANGE = 14.0, THROW_SPEED = 1.3, THROW_HALF_WIDTH = 0.9;
    public static final float THROW_OUT_RAW = 7f, THROW_BACK_RAW = 5f;
    /** Guillotine: the reach and the warning. */
    public static final double GUILLOTINE_RANGE = 4.6;
    public static final int GUILLOTINE_DELAY = 8;
    /** Shine Axe Burst: the radius, the warning, how long the wielder is held still. */
    public static final double SHINE_RADIUS = 8.0;
    public static final int SHINE_DELAY = 16;
    /** Cursed Penetration: the reach, the warning, the ticks until the wound opens, the raw of the wound, the corruption it costs. */
    public static final double PENETRATION_RANGE = 4.8;
    public static final int PENETRATION_DELAY = 8, WOUND_DELAY = 20, PENETRATION_CORRUPTION = 15;
    public static final float WOUND_RAW = 5f;

    // ------------------------------------------------------------------ passives
    /** Cleaving: a full swing also cuts up to this many foes within this radius of the one struck, for this much raw. */
    public static final int CLEAVE_TARGETS = 3;
    public static final double CLEAVE_RADIUS = 2.2;
    public static final float CLEAVE_RAW = 3f;
    /** Headsman: a full swing on a foe an execution would take cuts once more for this much raw. */
    public static final float HEADSMAN_RAW = 3.5f;
    /**
     * Self-repair (the Curse Axe): every this many ticks the wielder mends this share of their maximum health, through the healing law. The law cuts a heal to 40 percent when
     * the target was healed in the last 20 seconds, so a steady 4 percent every two seconds is really 1.6 percent: about 0.8 percent of the maximum health a second.
     */
    public static final int REPAIR_EVERY = 40;
    public static final double REPAIR_SHARE = 0.04;
    /** Radiance (the Shine Axe): Brawn fades half as fast, and the axe shows a glow while the wielder has at least this many stacks. */
    public static final int RADIANCE_STACKS = 6;
}
