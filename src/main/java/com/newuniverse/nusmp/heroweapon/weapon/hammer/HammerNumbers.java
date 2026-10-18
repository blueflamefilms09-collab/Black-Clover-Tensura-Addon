package com.newuniverse.nusmp.heroweapon.weapon.hammer;

import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec.Kind;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Why: every number of the Vassal Hammer in one pure table, so the owner's balance budget (design.md section 5) can be checked by a unit test and a later edit that breaks it
 * fails the build instead of the game. The Hammer is the slow, readable weapon of the set: each heavy blow shows a ring on the ground a moment before it lands, and every blow
 * builds the foe's POISE damage until the foe breaks (see {@link PoiseMath}). Distances are blocks, times are ticks (20 a second), poise is in points of a foe's gauge, the
 * shares are of the wielder's maximum magicules. The raw damage of a hit is before the level factor of the bond (1.0 to 2.2) and before the balance law.
 */
public final class HammerNumbers {
    private HammerNumbers() {}

    // ------------------------------------------------------------------ the techniques (budget rows)
    public static final SkillSpec OVERHEAD_SMASH = new SkillSpec("hammer.overhead_smash", Kind.HEAVY, 11, 1, 80, 0.015, 12);
    public static final SkillSpec SHOCKWAVE = new SkillSpec("hammer.shockwave", Kind.DAMAGE, 7, 1, 100, 0.02, 12);
    public static final SkillSpec ARMOR_CRACK = new SkillSpec("hammer.armor_crack", Kind.HEAVY, 10, 1, 120, 0.015, 1);
    public static final SkillSpec QUAKE_STOMP = new SkillSpec("hammer.quake_stomp", Kind.DAMAGE, 9, 1, 140, 0.025, 12);
    public static final SkillSpec SKYWARD_SMASH = new SkillSpec("hammer.skyward_smash", Kind.DAMAGE, 8, 1, 90, 0.015, 1);
    public static final SkillSpec FORGE_AEGIS = new SkillSpec("hammer.forge_aegis", Kind.UTILITY, 0, 0, 300, 0.03, 10);
    public static final SkillSpec TITAN_FALL = new SkillSpec("hammer.titan_fall", Kind.ULTIMATE, 24, 1, 900, 0.065, 12);

    /** Every row, in the order the logic lists the skills. */
    public static final List<SkillSpec> SPECS = List.of(OVERHEAD_SMASH, SHOCKWAVE, ARMOR_CRACK, QUAKE_STOMP, SKYWARD_SMASH, FORGE_AEGIS, TITAN_FALL);

    /** The ids of all rows, and the ids of the ultimates among them (what the forms file may name). */
    public static final Set<String> IDS = SPECS.stream().map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());
    public static final Set<String> ULTIMATES = SPECS.stream().filter(SkillSpec::ultimate).map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());

    // ------------------------------------------------------------------ the passives (ids the forms file lists)
    public static final String HEAVY_BLOWS = "heavy_blows", RUBBLE = "rubble", ANVIL_STANCE = "anvil_stance", CRUSHING_WEIGHT = "crushing_weight", SYNTHESIS = "synthesis";
    public static final Set<String> PASSIVES = Set.of(HEAVY_BLOWS, RUBBLE, ANVIL_STANCE, CRUSHING_WEIGHT, SYNTHESIS);

    // ------------------------------------------------------------------ shapes (blocks)
    /** Overhead Smash: the ring lands this far in front of the wielder, and is this wide. */
    public static final double SMASH_REACH = 2.8, SMASH_RADIUS = 3.4;
    /** Shockwave: the wave runs this far, this wide on each side of its line. */
    public static final double SHOCK_LENGTH = 11.0, SHOCK_HALF_WIDTH = 1.2;
    /** Armor Crack and Skyward Smash pick one foe within this reach. */
    public static final double CRACK_RANGE = 4.2, SKYWARD_RANGE = 3.8;
    /** Quake Stomp and Titan's Fall: the radius round the wielder / the landing. Forge Aegis: the radius of the allies it tempers. */
    public static final double QUAKE_RADIUS = 5.2, TITAN_RADIUS = 7.0, AEGIS_RADIUS = 7.0;
    /** A rubble splash round the foe a full swing struck. */
    public static final double RUBBLE_RADIUS = 2.2;

    // ------------------------------------------------------------------ timing (ticks)
    /** The tell is drawn at the cast, the blow lands this many ticks later. */
    public static final int SMASH_DELAY = 12, SHOCK_DELAY = 8, CRACK_DELAY = 7, QUAKE_DELAY = 14, SKYWARD_DELAY = 5;
    /** Titan's Fall: the leap may last this long, and the landing is not looked for before the wielder has been up this long. */
    public static final int TITAN_MAX_AIR = 40, TITAN_MIN_AIR = 6;
    /** How long Forge Aegis lasts on those it tempers. */
    public static final int AEGIS_TICKS = 160;

    // ------------------------------------------------------------------ poise (points of a foe's gauge)
    public static final float SMASH_POISE = 42f, SHOCK_POISE = 24f, CRACK_POISE = 36f, QUAKE_POISE = 30f, SKYWARD_POISE = 26f, TITAN_POISE = 160f;
    /** A plain blow of the hammer / a blow with the attack bar full / the splash of the rubble passive. */
    public static final float SWING_POISE = 3f, FULL_SWING_POISE = 8f, RUBBLE_POISE = 4f;

    // ------------------------------------------------------------------ passives
    /** Raw damage of the rubble splash, and how many foes it may touch besides the one struck. */
    public static final float RUBBLE_RAW = 2.5f;
    public static final int RUBBLE_TARGETS = 3;
    /** The share of a blow the wielder still takes while sneaking with Anvil Stance. */
    public static final double ANVIL_STANCE_FACTOR = 0.82;
    /** Every this many full swings, Crushing Weight cracks the armour of the foe struck; the share of armour it takes and how long. */
    public static final int CRUSH_EVERY = 3, CRUSH_TICKS = 100;
    public static final double CRUSH_ARMOR = -0.30, CRACK_ARMOR = -0.45, CRACK_TOUGHNESS = -0.30;
    public static final int CRACK_TICKS = 160;

    // ------------------------------------------------------------------ the break
    /** How the other effects of a break are timed (the break itself is {@link PoiseMath#BREAK_TICKS}). */
    public static final int BREAK_WEAKNESS_TICKS = 100, BREAK_FRAGILITY_TICKS = 60, BREAK_ROOT_BASE = 30;
}
