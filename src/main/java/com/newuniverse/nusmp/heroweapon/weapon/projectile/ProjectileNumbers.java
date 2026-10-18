package com.newuniverse.nusmp.heroweapon.weapon.projectile;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;

import java.util.List;

/**
 * Why: every number of the Vassal Projectile in one place without game classes, so a test can judge the whole table against the design's budget. The strongest
 * value of each technique ({@code rawMax}, {@code hitsPerTarget}, {@code maxTargets}) already counts the passives that raise it (a second blade, one more
 * ricochet), so the budget holds in the best form of the weapon, not only in the first one. The thrown blades are hitscan lines with a return line drawn back to
 * the wielder; nothing here is an entity.
 */
public final class ProjectileNumbers {
    private ProjectileNumbers() {}

    // ---------------------------------------------------------------- the techniques (the budget table)
    /** One blade along the look line, 6 raw, pierces two foes; with Second Throw a second blade follows (so one foe can take two). */
    public static final Budget.Spec INSTANT_THROW = Budget.damage("projectile.instant_throw", 60, 0.01, 6.0, 6.0, 2, 4);
    /** A blade that goes out and comes back by another path: 4 raw out and 4 raw back. */
    public static final Budget.Spec BOOMERANG = Budget.damage("projectile.boomerang", 80, 0.015, 4.0, 4.0, 2, 4);
    /** A blade that jumps from foe to foe (or off a wall to the nearest foe), 5 raw each. */
    public static final Budget.Spec RICOCHET = Budget.damage("projectile.ricochet", 100, 0.02, 5.0, 5.0, 1, 5);
    /** A fan of seven knives, 3 raw each; one foe takes at most four. */
    public static final Budget.Spec KNIFE_RAIN = Budget.damage("projectile.knife_rain", 120, 0.02, 3.0, 3.0, 4, 7);
    /** Three blades whirl round one foe, 4 raw each. */
    public static final Budget.Spec TORNADO_THROW = Budget.damage("projectile.tornado_throw", 150, 0.02, 4.0, 4.0, 3, 1);
    /** Bolas that slow a foe and burst, 8 raw. */
    public static final Budget.Spec BOLAS_BLAST = Budget.damage("projectile.bolas_blast", 180, 0.025, 8.0, 8.0, 1, 6);
    /** Three blades in quick turn at up to three foes, 4 raw each. */
    public static final Budget.Spec DRITTE_THROW = Budget.damage("projectile.dritte_throw", 140, 0.02, 4.0, 4.0, 3, 3);
    /** Every marked foe in range is struck by a blade that returns to the wielder, 5 raw. */
    public static final Budget.Spec RECALL = Budget.damage("projectile.recall", 200, 0.02, 5.0, 5.0, 1, 6);
    /** Pins one foe in place for a short time. */
    public static final Budget.Spec SHADOW_BIND = Budget.utility("projectile.shadow_bind", 300, 0.02, 1);
    /** The ultimate: eight volleys of blades on a field, 4 raw each. */
    public static final Budget.Spec THOUSAND_BLADES = Budget.ultimate("projectile.thousand_blades", 900, 0.06, 4.0, 4.0, 8, 12);

    /** Every technique of the projectile, in the order the logic lists them. */
    public static final List<Budget.Spec> ALL = List.of(INSTANT_THROW, BOOMERANG, RICOCHET, KNIFE_RAIN, TORNADO_THROW, BOLAS_BLAST, DRITTE_THROW, RECALL, SHADOW_BIND,
            THOUSAND_BLADES);

    // ---------------------------------------------------------------- marks
    /** How long a foe stays marked by a blade that hit it (Recall strikes the marked). */
    public static final int MARK_TICKS = 160;
    /** Over Custom: every cooldown is multiplied by this (never below the floor of its kind). */
    public static final double CUSTOM_COOLDOWN = 0.9;

    // ---------------------------------------------------------------- the techniques' shapes
    public static final double THROW_RANGE = 26.0, THROW_WIDTH = 0.5;
    public static final int THROW_PIERCE = 2, SECOND_DELAY = 6;

    public static final double BOOMERANG_RANGE = 22.0, BOOMERANG_SIDE = 3.0;
    public static final int BOOMERANG_PIERCE = 4, BOOMERANG_RETURN = 10;

    public static final double RICOCHET_RANGE = 24.0, RICOCHET_JUMP = 9.0, RICOCHET_WIDE_JUMP = 2.0;
    public static final int RICOCHET_JUMPS = 3;

    public static final double KNIFE_RANGE = 14.0, KNIFE_WIDTH = 0.45, KNIFE_SPREAD = 36.0;
    public static final int KNIFE_COUNT = 7, KNIFE_CAP = 4;
    /** The fan is shown (its two edges and its middle line) and the knives fly this many ticks later: one foe can take up to 12 raw, so the throw needs a tell. */
    public static final int KNIFE_DELAY = 5;

    public static final double TORNADO_RANGE = 22.0;
    public static final int TORNADO_TICKS = 20, TORNADO_INTERVAL = 10, TORNADO_SLOW = 40, TORNADO_HEAVY_SLOW = 40;

    public static final double BOLAS_RANGE = 20.0, BOLAS_RADIUS = 3.0, BOLAS_HEAVY_RADIUS = 1.0;
    public static final int BOLAS_DELAY = 24, BOLAS_SLOW = 50;

    public static final double DRITTE_RANGE = 20.0, DRITTE_HALF_ANGLE = 50.0;
    public static final int DRITTE_COUNT = 3, DRITTE_STAGGER = 4;

    public static final double BIND_RANGE = 20.0;
    /** The nominal binding time; {@code BalanceLaw.controlTicks} caps it again for the target (3 s on a player, 5 s on a mob, 1 s on a boss). */
    public static final int BIND_TICKS = 50;

    public static final double RECALL_RANGE = 28.0;

    public static final double BLADES_REACH = 22.0, BLADES_RADIUS = 5.0;
    public static final int BLADES_TELL = 24, BLADES_VOLLEYS = 8, BLADES_INTERVAL = 4;

    /** The cooldown of a technique under Over Custom (or its nominal value without), never below the floor of its kind. */
    public static int cooldown(Budget.Spec spec, boolean overCustom) {
        return overCustom ? Budget.scaled(spec.cooldown(), CUSTOM_COOLDOWN, Budget.floor(spec.kind())) : spec.cooldown();
    }
}
