package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget.Row;

import java.util.List;
import java.util.Set;

/**
 * Why: every number the Vassal Harpoon casts with lives in this one pure table, so the balance budget of design.md section 5 can be pinned by a test and a later
 * edit that breaks it fails the build. The logic builds its skills from the {@link Row}s (so the table and the game cannot differ) and reads every other constant
 * from here. Raw damage is before the level factor (1.0 to 2.2) and the balance law. Design, not canon: the figures are the author's; the technique names are the
 * wiki's (Lightning Strike Dragon, Whale Hunting Harpoon, Brave Fish Blaster, Lightning Shock Harpoon, Crucifixion, Harpoon 0).
 */
public final class HarpoonNumbers {
    private HarpoonNumbers() {}

    // ------------------------------------------------------------------ the budget rows (one per skill)
    public static final Row THROW = Row.damage("harpoon.harpoon_throw", Kind.ORDINARY, 7, 7, 2, 60, 0.01);
    public static final Row REEL_IN = Row.damage("harpoon.reel_in", Kind.UTILITY, 4, 4, 1, 120, 0.015);
    public static final Row DRAGON = Row.damage("harpoon.lightning_strike_dragon", Kind.ORDINARY, 7, 12, 6, 160, 0.02);
    public static final Row WHALE = Row.damage("harpoon.whale_hunting_harpoon", Kind.HEAVY, 17.5, 17.5, 1, 220, 0.025);
    public static final Row CRUCIFIXION = Row.damage("harpoon.crucifixion", Kind.ORDINARY, 4, 12, 1, 260, 0.03);
    public static final Row SHOCK = Row.damage("harpoon.lightning_shock_harpoon", Kind.ORDINARY, 7, 7, 4, 100, 0.02);
    public static final Row ZERO = Row.utility("harpoon.harpoon_zero", 200, 0.01).reaching(4);
    public static final Row FISH_BLASTER = Row.damage("harpoon.brave_fish_blaster", Kind.ULTIMATE, 16, 16, 8, 700, 0.07);

    public static final List<Row> ALL = List.of(THROW, REEL_IN, DRAGON, WHALE, CRUCIFIXION, SHOCK, ZERO, FISH_BLASTER);

    /** The ids of the ultimates (one per form at most). */
    public static final Set<String> ULTIMATES = Set.of(FISH_BLASTER.id());

    // ------------------------------------------------------------------ the throw and the tether
    public static final double THROW_RANGE = 18.0, TETHER_RANGE = 30.0, ARC_RANGE = 4.5;
    /** The Storm Charged passive: the throw arcs to one more foe near the target for this much. */
    public static final double ARC_RAW = 3.0;

    // ------------------------------------------------------------------ Reel In
    public static final double REEL_SPEED = 1.3, REEL_STOP = 2.5;
    public static final int REEL_TICKS = 14;

    // ------------------------------------------------------------------ Lightning Strike Dragon
    public static final double DRAGON_DASH = 8.0, DRAGON_WIDTH = 1.4, DRAGON_BACK_RAW = 5.0;
    /** The turn at the end of the charge comes this many ticks later: more than the hurt-resistance window, or the second hit would do nothing. */
    public static final int DRAGON_TURN = 12;

    // ------------------------------------------------------------------ Whale Hunting Harpoon
    public static final double WHALE_RANGE = 24.0, WHALE_BASE = 14.0, WHALE_BIG_BONUS = 1.25, WHALE_BIG_HEALTH = 40.0, WHALE_RADIUS = 2.5;
    public static final int WHALE_TELL = 12, WHALE_STAGGER = 20;

    // ------------------------------------------------------------------ Crucifixion
    public static final double CRUCIFY_PULSE_RAW = 4.0;
    public static final int CRUCIFY_TICKS = 60, CRUCIFY_PULSES = 3, CRUCIFY_EVERY = 15;

    // ------------------------------------------------------------------ Lightning Shock Harpoon
    public static final double SHOCK_ARC_RAW = 4.0, SHOCK_ARC_RANGE = 6.0;
    public static final int SHOCK_ARCS = 3;

    // ------------------------------------------------------------------ Harpoon 0
    public static final double ZERO_RANGE = 18.0, ZERO_SPLASH_RADIUS = 5.0;
    public static final int ZERO_STRIP = 3, ZERO_STRIP_OTHERS = 1, ZERO_OTHERS = 3;

    // ------------------------------------------------------------------ Brave Fish Blaster
    public static final double FISH_LENGTH = 20.0, FISH_WIDTH = 2.2;
    public static final int FISH_TELL = 20, FISH_STAGGER = 20;

    // ------------------------------------------------------------------ small pure rules
    /** What the Whale Hunting throw is worth: 14, a quarter more against a body with 40 health or more. */
    public static double whaleRaw(double targetMaxHealth) {
        return targetMaxHealth >= WHALE_BIG_HEALTH ? WHALE_BASE * WHALE_BIG_BONUS : WHALE_BASE;
    }
}
