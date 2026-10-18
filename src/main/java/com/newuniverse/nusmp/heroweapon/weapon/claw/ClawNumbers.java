package com.newuniverse.nusmp.heroweapon.weapon.claw;

import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec.Kind;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Why: every number of the Vassal Claw in one pure table, so the owner's balance budget (design.md section 5) can be checked by a unit test and a later edit that breaks it fails
 * the build instead of the game. The Claw is the lightest and fastest of the four striking weapons: it cuts for little, but every cut opens a BLEED that ticks on its own, and
 * its techniques either add wounds or cash them in. Its canon power-up, Skill Mastery (the more a skill is used, the better it gets), is modelled as five mastery levels per
 * technique; the table's raw damage is the highest a hit can reach BEFORE mastery, and the budget test multiplies it by the top mastery so the cap holds at the weapon's best.
 * Distances are blocks, times are ticks, the shares are of the wielder's maximum magicules.
 */
public final class ClawNumbers {
    private ClawNumbers() {}

    // ------------------------------------------------------------------ the techniques (budget rows; raw is the best a hit can do before mastery)
    public static final SkillSpec AIR_STRIKE_SLASH = new SkillSpec("claw.air_strike_slash", Kind.DAMAGE, 6, 1, 60, 0.01, 3);
    /** Five per cut, seven with the combo (see {@link #SECOND_BASE}, {@link #COMBO_MULT}); two cuts. */
    public static final SkillSpec SECOND_SLASH = new SkillSpec("claw.second_slash", Kind.DAMAGE, 7, 2, 70, 0.015, 6);
    /** The burst of a Hemorrhage grows with the wounds it opens, up to {@link BleedMath#BURST_MAX}. */
    public static final SkillSpec HEMORRHAGE = new SkillSpec("claw.hemorrhage", Kind.HEAVY, 15, 1, 100, 0.02, 1);
    public static final SkillSpec POUNCE = new SkillSpec("claw.pounce", Kind.DAMAGE, 6, 1, 100, 0.015, 8);
    public static final SkillSpec SHREDDING_RUSH = new SkillSpec("claw.shredding_rush", Kind.DAMAGE, 3, 4, 140, 0.02, 1);
    public static final SkillSpec PREDATOR_INSTINCT = new SkillSpec("claw.predator_instinct", Kind.UTILITY, 0, 0, 260, 0.02, 12);
    public static final SkillSpec DRAGON_REND = new SkillSpec("claw.dragon_rend", Kind.ULTIMATE, 7, 3, 900, 0.065, 8);
    /** The curse form's technique. */
    public static final SkillSpec WAHNSINN_CLAW = new SkillSpec("claw.wahnsinn_claw", Kind.HEAVY, 10, 1, 160, 0.03, 6);

    public static final List<SkillSpec> SPECS = List.of(AIR_STRIKE_SLASH, SECOND_SLASH, HEMORRHAGE, POUNCE, SHREDDING_RUSH, PREDATOR_INSTINCT, DRAGON_REND, WAHNSINN_CLAW);
    public static final Set<String> IDS = SPECS.stream().map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());
    public static final Set<String> ULTIMATES = SPECS.stream().filter(SkillSpec::ultimate).map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());

    // ------------------------------------------------------------------ the passives (ids the forms file lists)
    public static final String RENDING_BLOWS = "rending_blows", SKILL_MASTERY = "skill_mastery", SCENT_OF_BLOOD = "scent_of_blood", ROPE_HOOK = "rope_hook",
            DRAGON_FANGS = "dragon_fangs", MADNESS = "madness";
    public static final Set<String> PASSIVES = Set.of(RENDING_BLOWS, SKILL_MASTERY, SCENT_OF_BLOOD, ROPE_HOOK, DRAGON_FANGS, MADNESS);

    // ------------------------------------------------------------------ shapes (blocks) and timing (ticks)
    /** Air Strike Slash: how far the crescent flies, how fast (blocks a tick), how wide a line it cuts on each side. */
    public static final double SLASH_RANGE = 12.0, SLASH_SPEED = 1.5, SLASH_HALF_WIDTH = 0.9;
    /** Second Slash: the reach of a plain one and of one that follows an Air Strike Slash, the half-angle of the fan, and the base damage of one cut. */
    public static final double SECOND_RANGE = 3.4, SECOND_RANGE_COMBO = 5.0, SECOND_HALF_ANGLE = 55.0;
    public static final float SECOND_BASE = 5f;
    public static final double COMBO_MULT = 1.4;
    /** How long after an Air Strike Slash found a foe the next Second Slash counts as its follow-up. */
    public static final int COMBO_WINDOW = 100;
    /** Hemorrhage: the reach, and the delay between the mark and the burst. */
    public static final double HEMORRHAGE_RANGE = 4.6;
    public static final int HEMORRHAGE_DELAY = 5;
    /** Pounce: the plain leap, the lock-on reach of the Rope Claws, the radius of the landing and how long to wait for the landing. */
    public static final double POUNCE_SPEED = 0.9, POUNCE_LIFT = 0.5, POUNCE_LOCK_RANGE = 12.0, POUNCE_RADIUS = 2.8;
    public static final int POUNCE_MAX_AIR = 16, POUNCE_MIN_AIR = 3;
    /** Shredding Rush: the reach it starts from, the reach of each cut, the ticks to the first cut and between cuts, the armour one cut takes (a share, up to four cuts). */
    public static final double RUSH_RANGE = 6.5, RUSH_REACH = 4.6;
    public static final int RUSH_FIRST = 5, RUSH_GAP = 3, RUSH_CUTS = 4, RUSH_ARMOR_TICKS = 100;
    public static final double RUSH_ARMOR_STEP = -0.07;
    /** Predator's Instinct: how long it lasts and how far it sees a wounded foe. */
    public static final int INSTINCT_TICKS = 160;
    public static final double INSTINCT_SIGHT = 20.0;
    /** Dragon Rend: the length of the run, the half-width of the path, the warning before the run, the number of passes and the ticks to the first and between them. */
    public static final double REND_LENGTH = 12.0, REND_HALF_WIDTH = 1.8;
    public static final int REND_WARN = 6, REND_PASSES = 3, REND_FIRST = 2, REND_GAP = 4;
    /** Wahnsinn Claw: the length of the beam, its half-width, the wind-up, the corruption it costs the wielder and how long the curse it lays lasts. */
    public static final double MADNESS_LENGTH = 14.0, MADNESS_HALF_WIDTH = 1.3;
    public static final int MADNESS_DELAY = 8, MADNESS_CORRUPTION = 25, MADNESS_CURSE_TICKS = 160;

    // ------------------------------------------------------------------ passives
    /** Scent of Blood: a blow on a foe with at least this many wounds strikes once more for this much raw. */
    public static final int SCENT_STACKS = 3;
    public static final float SCENT_RAW = 2.5f;
    /** Rope Hook: the half-angle in which the leap looks for a foe to lock on. */
    public static final double ROPE_ANGLE = 25.0;
}
