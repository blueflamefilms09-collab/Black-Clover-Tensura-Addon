package com.newuniverse.nusmp.heroweapon.weapon.whip;

import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec.Kind;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Why: every number of the Vassal Whip in one pure table, so the owner's balance budget (design.md section 5) can be checked by a unit test and a later edit that breaks it
 * fails the build instead of the game. The Whip is the weapon of reach and control: a lash along the look, a bind, a pull, a swing from a tether, chain lightning, a portal, a
 * heavy morning star and, above all, the canon special ability of the Vassal Whip: it controls the power of monsters. That is COMMAND, which turns up to three hostile monsters
 * (never a boss, never a player) against their own side for six seconds, and it is held to the owner's limits: it needs the shared control lock, it lasts six seconds, three
 * monsters at most, and the monsters can never hurt the wielder or their friends while it lasts. Distances are blocks, times are ticks, the shares are of the wielder's maximum
 * magicules. The Whip carries no curse series.
 */
public final class WhipNumbers {
    private WhipNumbers() {}

    // ------------------------------------------------------------------ the techniques (budget rows)
    public static final SkillSpec LASH = new SkillSpec("whip.lash", Kind.DAMAGE, 6, 1, 60, 0.01, 3);
    public static final SkillSpec BIND_WHIP = new SkillSpec("whip.bind_whip", Kind.UTILITY, 3, 1, 160, 0.02, 1);
    public static final SkillSpec REEL_IN = new SkillSpec("whip.reel_in", Kind.UTILITY, 3, 1, 140, 0.02, 1);
    /** 140, so the 20 percent that Hourglass Flow takes off still leaves 112, above the utility floor of 100. */
    public static final SkillSpec TETHER_SWING = new SkillSpec("whip.tether_swing", Kind.UTILITY, 0, 0, 140, 0.015, 1);
    /** Seven on the first foe, then four-fifths of the last at every jump, up to five foes. */
    public static final SkillSpec LIGHTNING_WHIP = new SkillSpec("whip.lightning_whip", Kind.DAMAGE, 7, 1, 100, 0.02, 5);
    public static final SkillSpec COMMAND = new SkillSpec("whip.command", Kind.UTILITY, 0, 0, 400, 0.03, 3);
    public static final SkillSpec GIGANTIC_STAR = new SkillSpec("whip.gigantic_star", Kind.HEAVY, 12, 1, 120, 0.025, 12);
    /** 140, as the tether swing, for the same reason. */
    public static final SkillSpec PORTAL_WHIP = new SkillSpec("whip.portal_whip", Kind.UTILITY, 6, 1, 140, 0.02, 8);
    public static final SkillSpec DIMENSION_WHIP = new SkillSpec("whip.dimension_whip", Kind.ULTIMATE, 14, 1, 900, 0.06, 12);

    public static final List<SkillSpec> SPECS = List.of(LASH, BIND_WHIP, REEL_IN, TETHER_SWING, LIGHTNING_WHIP, COMMAND, GIGANTIC_STAR, PORTAL_WHIP, DIMENSION_WHIP);
    public static final Set<String> IDS = SPECS.stream().map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());
    public static final Set<String> ULTIMATES = SPECS.stream().filter(SkillSpec::ultimate).map(SkillSpec::id).collect(Collectors.toUnmodifiableSet());

    // ------------------------------------------------------------------ the passives (ids the forms file lists)
    public static final String COIL = "coil", STATIC_CHARGE = "static_charge", STAR_WEIGHT = "star_weight", TASKMASTER = "taskmaster", HOURGLASS_FLOW = "hourglass_flow";
    public static final Set<String> PASSIVES = Set.of(COIL, STATIC_CHARGE, STAR_WEIGHT, TASKMASTER, HOURGLASS_FLOW);

    // ------------------------------------------------------------------ shapes (blocks) and timing (ticks)
    /** Lash: how far the whip reaches and how wide on each side; Coil slows the foe for this long. */
    public static final double LASH_RANGE = 7.0, LASH_HALF_WIDTH = 0.8;
    public static final int COIL_TICKS = 20;
    /** Bind Whip: the reach, the base time of the root (the balance law shortens it), the slow if the control lock is busy. */
    public static final double BIND_RANGE = 9.0;
    public static final int BIND_TICKS = 40, BIND_SLOW_TICKS = 40;
    /** Reel In: the reach. */
    public static final double REEL_RANGE = 11.0;
    /** Tether Swing: the reach, and the slow falling that keeps the landing soft. */
    public static final double TETHER_RANGE = 16.0;
    public static final int TETHER_SOFT_TICKS = 40;
    /** Lightning Whip: the reach of the first lash, the jump between foes, the number of foes, the falloff at each jump, the warning, and static charge's bonus and window. */
    public static final double LIGHTNING_RANGE = 9.0, LIGHTNING_JUMP = 5.5, LIGHTNING_FALLOFF = 0.8, CHARGE_BONUS = 1.3;
    public static final int LIGHTNING_FOES = 5, LIGHTNING_DELAY = 5, CHARGE_TICKS = 120;
    /** Command: the reach, the number of monsters, how long they obey, the ticks between two steers, and the strength a Taskmaster lends them. */
    public static final double COMMAND_RANGE = 14.0, COMMAND_SIGHT = 12.0;
    public static final int COMMAND_MAX = 3, COMMAND_TICKS = 120, COMMAND_STEER = 4;
    /** Gigantic Star: how far ahead the star lands, its radius, the warning, and the bonus Star Weight gives against a bound foe. */
    public static final double STAR_REACH = 4.5, STAR_RADIUS = 3.0, STAR_BONUS = 1.25;
    public static final int STAR_DELAY = 10, BOUND_TICKS = 100;
    /** Portal Whip: how far the wielder steps through, the radius of the blow on arrival, and the Hourglass Flow cooldown factor. */
    public static final double PORTAL_RANGE = 14.0, PORTAL_RADIUS = 3.2;
    public static final double HOURGLASS_FACTOR = 0.8;
    /** Dimension Whip: the radius, the warning, the base time of the bind. */
    public static final double DIMENSION_RADIUS = 11.0;
    public static final int DIMENSION_DELAY = 14, DIMENSION_BIND_TICKS = 50;
}
