package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Kind;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget.Spec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every number of the Vassal Scythe (damage, reach, rests, costs, souls, heals) is stated once, here, with no game imports, so the skills read them from
 * one place and one test ({@code ScytheNumbersTest}) can hold the whole weapon to the owner's balance budget (docs/heroweapon/design.md section 5,
 * {@code Budget}). Damage is RAW (before the level factor of 1.0 to 2.2 and before the balance law, which also caps a hit on a player). The weapon's play is
 * reach and the reaper's arc: a full circle, a thrown disc, a push, a rain of swings, a floating second scythe, the finisher that reaps the weak (doubled
 * damage on a non-boss mob under 30 percent health, a capped heal for each one it kills), the thrust whose damage grows with the foe's armour, the ultimate
 * that spends the souls it has collected, and the Scythe 0 that cuts a foe's blessings away and does no damage.
 */
public final class ScytheNumbers {
    private ScytheNumbers() {}

    // ---- skill ids
    public static final String FULL_MOON_ARMY = "scythe.full_moon_army", FLYING_CIRCLE = "scythe.flying_circle", WIND_SLICE = "scythe.wind_slice", REAP = "scythe.reap",
            PROTECTION_BREAK = "scythe.protection_break", DEADLY_DANCE = "scythe.deadly_dance", FLOATING_SCYTHE = "scythe.floating_scythe",
            SOUL_HARVEST = "scythe.soul_harvest", SCYTHE_ZERO = "scythe.scythe_zero";

    // ---- passive ids
    public static final String P_SOUL_TITHE = "soul_tithe", P_DRAGON_SOUL = "dragon_soul", P_NULL_EDGE = "null_edge";

    // ---- Full Moon Army: the full circle
    public static final double MOON_RAW = 6.0, MOON_RADIUS = 4.8, MOON_SHOVE = 0.6;
    public static final int MOON_TARGETS = 12;

    // ---- Flying Circle: the disc that goes out and comes back
    public static final double CIRCLE_RAW_OUT = 5.0, CIRCLE_RAW_BACK = 5.0, CIRCLE_RANGE = 16.0, CIRCLE_WIDTH = 1.1;
    public static final int CIRCLE_TARGETS = 6, CIRCLE_BACK_AFTER = 10;

    // ---- Wind Slice: the gust that pushes away
    public static final double SLICE_RAW = 4.0, SLICE_RANGE = 4.8, SLICE_HALF_ANGLE = 65.0, SLICE_SHOVE = 1.1;
    public static final int SLICE_TARGETS = 8;

    // ---- Soul Reap: the finisher
    public static final double REAP_BASE = 10.0, REAP_WEAK_FACTOR = 2.0, REAP_THRESHOLD = 0.30, REAP_RANGE = 6.0, REAP_HALF_ANGLE = 50.0;
    public static final double REAP_HEAL_PER_FOE = 0.03, REAP_HEAL_CAP = 0.12;
    public static final int REAP_TARGETS = 6, REAP_DELAY = 8;

    // ---- Protection Break: the thrust that grows with armour
    public static final double BREAK_BASE = 9.0, BREAK_PER_ARMOR = 0.6, BREAK_BONUS_CAP = 8.0, BREAK_RANGE = 5.5;
    public static final int BREAK_DELAY = 10;

    // ---- Deadly Dance: the rain of swings
    public static final double DANCE_RAW = 3.0, DANCE_RANGE = 4.6, DANCE_HALF_ANGLE = 70.0, DANCE_SHOVE = 0.25;
    public static final int DANCE_SWINGS = 4, DANCE_INTERVAL = 4, DANCE_TARGETS = 8;

    // ---- Floating Scythe: the second scythe at your side
    public static final double FLOAT_RAW = 3.0, FLOAT_RANGE = 5.5;
    public static final int FLOAT_STRIKES = 6, FLOAT_INTERVAL = 20;

    // ---- Soul Harvest: the ultimate
    public static final double HARVEST_BASE = 16.0, HARVEST_PER_SOUL = 1.0, HARVEST_WEAK_FACTOR = 1.2, HARVEST_RADIUS = 7.5, HARVEST_HEAL_PER_FOE = 0.02, HARVEST_HEAL_CAP = 0.20;
    public static final int HARVEST_DELAY = 24, HARVEST_TARGETS = 12;

    // ---- Scythe 0: the cut that strips blessings
    public static final double ZERO_RANGE = 5.5, ZERO_HALF_ANGLE = 60.0;
    public static final int ZERO_TARGETS = 8;
    /** Null Edge (Scythe 0): a melee hit strips one blessing from a foe, and the same foe is not stripped again for this long. */
    public static final int NULL_EDGE_GAP = 40;

    // ---- souls (the series' Spirit Enchantment: a monster's soul attached to the weapon)
    public static final int SOUL_CAP = 10, SOUL_CAP_DRAGON = 14, SOUL_FADE_TICKS = 2400;

    // ---- costs (share of the maximum magicules) and rests (ticks, before the level scaling)
    public static final int REST_MOON = 70, REST_CIRCLE = 100, REST_SLICE = 90, REST_REAP = 120, REST_BREAK = 140, REST_DANCE = 100, REST_FLOAT = 240, REST_HARVEST = 700,
            REST_ZERO = 160;
    public static final double COST_MOON = 0.015, COST_CIRCLE = 0.020, COST_SLICE = 0.015, COST_REAP = 0.025, COST_BREAK = 0.025, COST_DANCE = 0.025, COST_FLOAT = 0.025,
            COST_HARVEST = 0.065, COST_ZERO = 0.010;

    // ---- the table the budget test reads
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        add(new Spec(FULL_MOON_ARMY, Kind.ORDINARY, REST_MOON, COST_MOON, MOON_RAW, 1, MOON_TARGETS, 0));
        add(new Spec(FLYING_CIRCLE, Kind.ORDINARY, REST_CIRCLE, COST_CIRCLE, Math.max(CIRCLE_RAW_OUT, CIRCLE_RAW_BACK), 2, CIRCLE_TARGETS, CIRCLE_BACK_AFTER));
        add(new Spec(WIND_SLICE, Kind.ORDINARY, REST_SLICE, COST_SLICE, SLICE_RAW, 1, SLICE_TARGETS, 0));
        add(new Spec(REAP, Kind.HEAVY, REST_REAP, COST_REAP, ReapRules.reapRaw(true), 1, REAP_TARGETS, REAP_DELAY));
        add(new Spec(PROTECTION_BREAK, Kind.HEAVY, REST_BREAK, COST_BREAK, ReapRules.breakRaw(1000.0), 1, 1, BREAK_DELAY));
        add(new Spec(DEADLY_DANCE, Kind.ORDINARY, REST_DANCE, COST_DANCE, DANCE_RAW, DANCE_SWINGS, DANCE_TARGETS, DANCE_SWINGS * DANCE_INTERVAL));
        add(new Spec(FLOATING_SCYTHE, Kind.HEAVY, REST_FLOAT, COST_FLOAT, FLOAT_RAW, FLOAT_STRIKES, 1, FLOAT_STRIKES * FLOAT_INTERVAL));
        add(new Spec(SOUL_HARVEST, Kind.ULTIMATE, REST_HARVEST, COST_HARVEST, ReapRules.harvestRaw(SOUL_CAP_DRAGON, true, SOUL_CAP_DRAGON), 1, HARVEST_TARGETS, HARVEST_DELAY));
        add(new Spec(SCYTHE_ZERO, Kind.UTILITY, REST_ZERO, COST_ZERO, 0.0, 0, ZERO_TARGETS, 0));
    }

    private static void add(Spec s) { SPECS.put(s.id(), s); }

    /** Every technique of the Scythe, in the order the weapon lists them. */
    public static List<Spec> table() { return List.copyOf(SPECS.values()); }

    /** The row of a skill id; throws for an id the Scythe does not have (a typo is a bug, not a default). */
    public static Spec spec(String id) {
        Spec s = SPECS.get(id);
        if (s == null) throw new IllegalArgumentException("the Scythe has no skill " + id);
        return s;
    }
}
