package com.newuniverse.nusmp.heroweapon.weapon.staff;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;

import java.util.List;

/**
 * Why: every number of the Vassal Staff in one place that has no game classes in it, so a test can judge the whole table against the design's budget
 * (raw damage per application and per foe, cooldown, cost, targets, heal) and a later edit that makes the staff too strong fails the build instead of
 * reaching the players. The table is also what the live skills are built from ({@code StaffLogic} reads the cooldown, the cost and the ultimate flag from the
 * {@link Budget.Spec}), so there is no second copy that could drift. The staff's marks add {@link #MARK_BONUS} to its own spells' damage, so the strongest
 * value of each technique ({@code rawMax}) already counts the mark and the passive that raises it; the test proves that even then one foe takes no more than
 * the design allows.
 */
public final class StaffNumbers {
    private StaffNumbers() {}

    // ---------------------------------------------------------------- the techniques (the budget table)
    /**
     * Hitscan bolt that pierces: 6 raw, +0.5 with Sharp Focus, +15 percent on a marked foe (7.475 at most, so it never needs the wind-up that a blow above 8 raw
     * does); the prism can add three more foes (four with the Wide Prism passive).
     */
    public static final Budget.Spec ARCANE_BOLT = Budget.damage("staff.arcane_bolt", 80, 0.015, 6.0, 7.475, 1, 7);
    /** A spark that jumps from foe to foe (up to four jumps), 5 raw each. */
    public static final Budget.Spec CHAIN_SPARK = Budget.damage("staff.chain_spark", 110, 0.02, 5.0, 5.75, 1, 5);
    /** A rune field that pulses three times, 3.2 raw each pulse. */
    public static final Budget.Spec RUNE_FIELD = Budget.damage("staff.rune_field", 160, 0.025, 3.2, 3.68, 3, 12);
    /** A ring of force round the wielder, 8 raw. */
    public static final Budget.Spec ARCANE_BURST = Budget.damage("staff.arcane_burst", 120, 0.02, 8.0, 9.2, 1, 12);
    /** A prism in the air that splits the wielder's bolts. */
    public static final Budget.Spec MAGIC_PRISM = Budget.utility("staff.magic_prism", 240, 0.02, 1);
    /** A mirror that negates incoming shots and sends 3 raw back at up to four shooters. */
    public static final Budget.Spec FLOAT_MIRROR = Budget.utilityHit("staff.float_mirror", 320, 0.025, 3.0, 4, 4);
    /** Quickens the staff's own cooldowns for eight seconds. */
    public static final Budget.Spec FENRIR_ROD = Budget.utility("staff.fenrir_rod", 360, 0.03, 1);
    /** Marks up to six foes in a cone. */
    public static final Budget.Spec ANALYZE = Budget.utility("staff.analyze", 200, 0.015, 6);
    /** Heals the wielder and allies by 14 percent and cleanses one harmful effect each. */
    public static final Budget.Spec HEALING_LIGHT = Budget.utility("staff.healing_light", 420, 0.03, 8).withHeal(0.14);
    /** The ultimate: six salvos of 4.5 raw on a field. */
    public static final Budget.Spec BOMBARDMENT = Budget.ultimate("staff.bombardment", 900, 0.06, 4.5, 5.175, 6, 12);

    /** Every technique of the staff, in the order the logic lists them. */
    public static final List<Budget.Spec> ALL = List.of(ARCANE_BOLT, CHAIN_SPARK, RUNE_FIELD, ARCANE_BURST, MAGIC_PRISM, FLOAT_MIRROR, FENRIR_ROD, ANALYZE,
            HEALING_LIGHT, BOMBARDMENT);

    // ---------------------------------------------------------------- marks (Analyze Magic)
    /** A marked foe takes this share more from the staff's own damage spells. */
    public static final double MARK_BONUS = 0.15;
    /** Ticks a mark lasts; the Star Mark passive adds {@link #MARK_EXTRA_TICKS}. */
    public static final int MARK_TICKS = 200, MARK_EXTRA_TICKS = 100;

    // ---------------------------------------------------------------- casting rhythm and haste
    /** Ticks within which the next spell continues a chain. */
    public static final int RHYTHM_WINDOW = 80;
    /** Most previous casts of a chain that still shorten the next cooldown, and by how much each. */
    public static final int RHYTHM_MAX = 3;
    public static final double RHYTHM_STEP = 0.05;
    /** Fenrir Rod: how long it lasts and what share of the cooldown is left while it does. */
    public static final int HASTE_TICKS = 160;
    public static final double HASTE_FACTOR = 0.65;
    /** No combination of rhythm and haste shortens a cooldown below this share. */
    public static final double MIN_FACTOR = 0.55;

    // ---------------------------------------------------------------- the techniques' shapes
    public static final double BOLT_RANGE = 24.0, BOLT_WIDTH = 0.45;
    public static final int BOLT_PIERCE = 2, BOLT_PIERCE_SHARP = 3;
    public static final double SHARP_BONUS = 0.5;

    public static final double CHAIN_RANGE = 22.0, CHAIN_WIDTH = 0.9, CHAIN_JUMP = 7.0, CHAIN_POOL = 18.0;
    public static final int CHAIN_JUMPS = 4;

    public static final double RUNE_REACH = 20.0, RUNE_RADIUS = 4.0;
    public static final int RUNE_TELL = 20, RUNE_PULSES = 3, RUNE_INTERVAL = 12, RUNE_SLOW_TICKS = 30;

    public static final double BURST_RADIUS = 5.0, BURST_PUSH = 0.9;
    public static final int BURST_DELAY = 6;

    public static final double PRISM_REACH = 18.0, PRISM_RADIUS = 1.8, PRISM_COPY_RANGE = 14.0, PRISM_COPY_SHARE = 0.6;
    public static final int PRISM_TICKS = 160, PRISM_WIDE_TICKS = 80, PRISM_CHARGES = 3, PRISM_COPIES = 3, PRISM_COPIES_WIDE = 4;

    public static final double MIRROR_REACH = 1.6, MIRROR_SIZE = 2.4, MIRROR_RETURN_RANGE = 40.0;
    public static final int MIRROR_TICKS = 80, MIRROR_DEEP_TICKS = 40, MIRROR_INTERVAL = 2, MIRROR_NEGATE = 6;
    public static final int MIRROR_RETURNS = 4, MIRROR_DEEP_NEGATE = 8;

    public static final double ANALYZE_RANGE = 24.0, ANALYZE_HALF_ANGLE = 22.0;

    public static final double HEAL_RADIUS = 7.0;

    public static final double BOMB_REACH = 24.0, BOMB_RADIUS = 4.5;
    public static final int BOMB_TELL = 24, BOMB_SALVOS = 6, BOMB_INTERVAL = 10;

    // ---------------------------------------------------------------- pure helpers the glue and the tests share
    /** The share of its nominal cooldown a staff spell waits now: Fenrir Rod and the chain bonus, never below {@link #MIN_FACTOR}. */
    public static double cooldownFactor(boolean hasted, int chainStacks, boolean quickChant) {
        double f = 1.0;
        if (hasted) f *= HASTE_FACTOR;
        if (quickChant) f *= 1.0 - RHYTHM_STEP * Math.max(0, Math.min(RHYTHM_MAX, chainStacks));
        return Math.max(MIN_FACTOR, f);
    }

    /** The raw damage of one application on a foe that is (or is not) marked. */
    public static double marked(double raw, boolean isMarked) { return isMarked ? raw * (1.0 + MARK_BONUS) : raw; }
}
