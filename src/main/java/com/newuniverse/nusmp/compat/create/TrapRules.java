package com.newuniverse.nusmp.compat.create;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: every decision about the Create traps that does not need a world, in one pure class so a test pins it: which rooms may hold a trap and how likely, which family a
 * room draws for its theme, depth and kind, how long and how high a trap is, what a hit does and the limits that keep a trap from ever being a death sentence (a cap on one
 * hit, a cap on one run, mercy for the nearly dead). The numbers are the owner's "better traps" tuned to the dungeon's own scale: a hit costs about a quarter of a full
 * health bar on the first floor and about a third (never more) deeper down, because the depth scaling is spent on the warning times and the number of bands, not on
 * single-hit death.
 */
public final class TrapRules {
    private TrapRules() {}

    // ---- geometry limits (the site finder and the drawings agree on these)
    public static final int LANE_WIDTH = 3, MIN_CORE = 6, MAX_CORE = 8, MIN_HEADROOM = 4, MAX_HEADROOM = 8, MAX_PARTS = 96;
    /** The depth of wall a trap needs on each side of the lane (columns +-2 and +-3 are used). */
    public static final int WALL_DEPTH = 2;

    // ---- timing (ticks)
    public static final int RUN_TICKS = 160, COOLDOWN_TICKS = 100;
    /** A band hurts for at most this share of its cycle, is quiet for at least this many ticks of it and warns for at least this many. */
    public static final int MAX_DUTY_PERCENT = 45, MIN_QUIET = 8, MIN_TELEGRAPH = 10;
    /** Blocks per tick the belt carries (a player walks at 0.22 and sprints at 0.28, so the belt can always be walked against). */
    public static final double BELT_STRENGTH = 0.06, MAX_BELT_STRENGTH = 0.09;

    // ---- hurt limits
    /** One hit takes at most this share of the victim's maximum health. */
    public static final double MAX_HIT_SHARE = 0.35;
    /** One run of one trap takes at most this share from one victim in all. */
    public static final double MAX_RUN_SHARE = 0.70;
    /** Below this share of maximum health a trap only throws the victim: it does not hurt (mercy). */
    public static final double MERCY_SHARE = 0.20;

    // ---- who may hold a trap
    /** The chance a room of this kind (the folder name of {@code RoomKind}) is chosen, before depth and the density setting. 0 = never. */
    public static double baseChance(String folder) {
        if (folder == null) return 0.0;
        return switch (folder) {
            case "corridor" -> 0.14;
            case "link" -> 0.08;
            case "junction" -> 0.06;
            case "treasure" -> 0.08;
            case "trap" -> 0.30;
            case "hall" -> 0.02;
            default -> 0.0;                                                   // landing, safe, stairwell, arena, antechamber, vault, onward, set, trial, puzzle, cavern, cap, other
        };
    }

    /** Does this room kind ever hold a trap? */
    public static boolean eligible(String folder) { return baseChance(folder) > 0.0; }

    /** The chance for a room: the base chance of its kind, raised by up to 80% with depth (200 floors), times the density setting, clamped to 0..0.9. */
    public static double chance(String folder, int depth, double density) {
        double c = baseChance(folder) * (1.0 + 0.8 * Math.min(Math.max(depth, 0), 200) / 200.0) * Math.max(0.0, density);
        return Math.max(0.0, Math.min(0.9, c));
    }

    /** How many traps a sector of this many rooms may hold at most: 7% of its rooms times the density, never more than 24; at least one when it has 8 rooms and the density is at least a half. */
    public static int maxPerSector(int rooms, double density) {
        if (rooms <= 0 || density <= 0.0) return 0;
        int n = (int) Math.round(rooms * 0.07 * density);
        if (n < 1 && rooms >= 8 && density >= 0.5) n = 1;
        return Math.min(24, n);
    }

    // ---- which family
    private static final TrapFamily[] FAMILIES = TrapFamily.values();

    /** Theme multipliers, in the order of {@link TrapFamily} (saw, press, drill, treadmill, pour, sequence). Unlisted themes use 1 for all. */
    private static final Map<String, double[]> THEME = Map.ofEntries(
            Map.entry("dwarven_forgehold", new double[] {2.0, 3.0, 2.5, 2.0, 2.5, 1.0}),
            Map.entry("infernal_foundry", new double[] {1.5, 2.0, 2.0, 1.5, 3.0, 0.8}),
            Map.entry("flame", new double[] {1.0, 1.5, 1.2, 1.0, 2.0, 0.8}),
            Map.entry("storm", new double[] {1.0, 1.0, 1.0, 1.5, 0.5, 3.0}),
            Map.entry("crystal_geode", new double[] {0.4, 0.4, 0.4, 0.4, 0.3, 2.0}),
            Map.entry("spatial", new double[] {0.7, 0.7, 0.7, 0.7, 0.5, 2.0}),
            Map.entry("astral", new double[] {0.7, 0.7, 0.7, 0.7, 0.5, 2.0}),
            Map.entry("sunken_temple", new double[] {1.0, 1.0, 0.8, 1.5, 0.3, 1.0}),
            Map.entry("frost", new double[] {1.0, 1.0, 1.0, 1.0, 0.3, 1.0}),
            Map.entry("necropolis", new double[] {1.5, 0.8, 0.8, 0.7, 0.7, 0.7}),
            Map.entry("haunted_manor", new double[] {1.5, 0.8, 0.7, 0.7, 0.7, 0.8}),
            Map.entry("cursed_archive", new double[] {1.4, 0.8, 0.7, 0.7, 0.7, 1.2}),
            Map.entry("vampire_castle", new double[] {1.4, 1.0, 0.8, 0.8, 0.8, 1.0}),
            Map.entry("holy_church", new double[] {0.35, 0.35, 0.35, 0.35, 0.35, 1.0}),
            Map.entry("celestial_court", new double[] {0.35, 0.35, 0.35, 0.35, 0.35, 1.0}),
            Map.entry("sky_citadel", new double[] {0.4, 0.4, 0.4, 1.0, 0.3, 1.0}),
            Map.entry("mountain_monastery", new double[] {0.35, 0.35, 0.35, 0.35, 0.35, 0.8}),
            Map.entry("radiant", new double[] {0.35, 0.35, 0.35, 0.35, 0.35, 1.0}),
            Map.entry("feywild_grove", new double[] {0.3, 0.3, 0.3, 0.3, 0.3, 0.8}),
            Map.entry("verdant", new double[] {0.7, 0.5, 0.5, 0.5, 0.4, 0.5}),
            Map.entry("fungal_underdark", new double[] {0.8, 0.6, 0.6, 0.6, 0.5, 0.5}),
            Map.entry("drowned_crypt", new double[] {0.8, 0.8, 0.6, 0.8, 0.2, 0.7}),
            Map.entry("spider_warrens", new double[] {0.6, 0.5, 0.5, 0.5, 0.4, 0.4}));

    /** The multiplier of a family in a theme (1 when the theme is not listed). */
    public static double themeAffinity(String themeId, TrapFamily family) {
        double[] m = themeId == null ? null : THEME.get(themeId.toLowerCase(Locale.ROOT));
        return m == null ? 1.0 : m[family.ordinal()];
    }

    /** The multiplier of a family for the kind of room (a belt wants a corridor; a hall has room for little). */
    public static double roomAffinity(String folder, TrapFamily family) {
        if (folder == null) return 1.0;
        return switch (folder) {
            case "corridor", "link" -> family == TrapFamily.TREADMILL ? 2.0 : family == TrapFamily.SEQUENCE ? 1.5 : family == TrapFamily.DRILL_WALL ? 1.3 : 1.0;
            case "treasure" -> family == TrapFamily.POUR ? 1.5 : family == TrapFamily.TREADMILL ? 0.3 : 1.0;
            case "hall" -> family == TrapFamily.TREADMILL ? 0.2 : 1.0;
            default -> 1.0;
        };
    }

    /**
     * The family a room draws, or null when none is allowed. {@code roll} is a uniform number in [0, 1). A family is out when it is not in {@code allowed}, or the floor
     * is shallower than its {@link TrapFamily#minDepth()}; the rest are weighted by the family's weight, the theme, the kind of room and a small rise with the sector's
     * threat (a deeper, more dangerous sector leans towards the heavier families). The result for one roll is always the same.
     */
    public static TrapFamily pickFamily(String themeId, int depth, int threat, String folder, double roll, Set<TrapFamily> allowed) {
        double[] w = new double[FAMILIES.length];
        double sum = 0;
        for (TrapFamily f : FAMILIES) {
            if (allowed != null && !allowed.contains(f)) continue;
            if (depth < f.minDepth()) continue;
            double heavy = f == TrapFamily.PRESS || f == TrapFamily.DRILL_WALL || f == TrapFamily.SEQUENCE ? 1.0 + 0.04 * Math.min(Math.max(threat, 1) - 1, 25) : 1.0;
            double x = f.weight() * themeAffinity(themeId, f) * roomAffinity(folder, f) * heavy;
            if (x <= 0) continue;
            w[f.ordinal()] = x;
            sum += x;
        }
        if (sum <= 0) return null;
        double r = Math.max(0.0, Math.min(0.999999999, roll)) * sum, acc = 0;
        TrapFamily last = null;
        for (TrapFamily f : FAMILIES) {
            double x = w[f.ordinal()];
            if (x <= 0) continue;
            last = f;
            acc += x;
            if (r < acc) return f;
        }
        return last;
    }

    /** The trigger of a trap: a plate more often than not, a tripwire sometimes, proximity now and then. */
    public static TriggerKind pickTrigger(double roll) {
        return roll < 0.55 ? TriggerKind.PLATE : roll < 0.80 ? TriggerKind.TRIPWIRE : TriggerKind.PROXIMITY;
    }

    /** The core length (6..8) from a roll in [0, 1). */
    public static int pickCore(double roll) {
        int span = MAX_CORE - MIN_CORE + 1;
        return MIN_CORE + Math.min(span - 1, (int) (Math.max(0.0, Math.min(0.999999, roll)) * span));
    }

    /** The whole strip a trap with this core needs: the core plus a margin cell at each end. */
    public static int stripLength(int core) { return core + 2; }

    // ---- what a hit does
    /** The damage of one hit of the given kind, before limits: a base that grows with the log of the depth, scaled by the kind, the sector threat and the server's multiplier. */
    public static double damage(HazardKind kind, int depth, int threat, double multiplier) {
        double base = 3.0 + 1.6 * (Math.log(1.0 + Math.max(depth, 0)) / Math.log(2.0));
        double t = Math.min(1.6, 1.0 + 0.03 * Math.max(0, threat - 1));
        return base * kind.damageScale() * t * Math.max(0.0, multiplier);
    }

    /**
     * The damage a victim actually takes: the wanted damage, but at most {@link #MAX_HIT_SHARE} of their maximum health, at most what is left of their share for this
     * run ({@link #MAX_RUN_SHARE}), and nothing at all when their health is already below {@link #MERCY_SHARE} of the maximum (the trap only throws them).
     * {@code takenThisRun} is what this run has taken from the same victim so far.
     */
    public static float safeDamage(double wanted, float health, float maxHealth, float takenThisRun) {
        if (wanted <= 0 || maxHealth <= 0) return 0f;
        if (health <= maxHealth * MERCY_SHARE) return 0f;
        double cap = Math.min(maxHealth * MAX_HIT_SHARE, maxHealth * MAX_RUN_SHARE - takenThisRun);
        if (cap <= 0) return 0f;
        double d = Math.min(wanted, cap);
        // never take the victim below the mercy line in one hit
        d = Math.min(d, health - maxHealth * MERCY_SHARE * 0.5);
        return (float) Math.max(0.0, d);
    }
}
