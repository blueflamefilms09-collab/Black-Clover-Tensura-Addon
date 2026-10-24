package com.newuniverse.nusmp.star;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Star Magic nodes, phase 2: stars sometimes die to Anti-Magic (docs/vfx_remake/star_ingame.md). The pure rules of the dispel hook, with no Minecraft class so tests can run them anywhere.
 *
 * <p>What the owner decided: when the mod's Anti-Magic reaches a star node or a Quartile Scutum pane (the dispel is {@code Nullification.shatterBarriers}, which every Anti-Magic slash, the Nihility zone and the
 * other dispel-style arts call), the star or pane breaks with a CHANCE, not always. The chance is the server setting {@code star.dispelChance} (default {@link #DEFAULT_CHANCE}), higher for a pane and the
 * end-stars that hold it ({@link #PANE_FACTOR}: 0.35 becomes 0.6), lower for a Grinberryall heir's ({@link #HEIR_FACTOR}: half). Each star or pane is rolled once per dispel and, when it survived the roll,
 * is left alone for {@link #GRACE_TICKS} ticks, so a lingering zone (the Nihility zone dispels every 10 ticks) cannot wear a star down in one second.
 *
 * <p>Cost bound (rule 11): one pass over the in-memory registries (at most 512 stars and 8 panes, no entity query, no world scan), at most {@link #MAX_CANDIDATES} stars considered and
 * {@link #MAX_BREAKS} broken per dispel, so at most {@link #MAX_BREAKS} small effect packets per dispel. Nothing at all while no star or pane exists.
 */
public final class StarDispelRules {
    private StarDispelRules() {}

    public static final String CONFIG_KEY = "dispelChance";
    public static final double DEFAULT_CHANCE = 0.35, MIN_CHANCE = 0.0, MAX_CHANCE = 1.0;
    /** A pane and the end-stars that hold it are rolled at base times this (0.35 x 12/7 = 0.6). */
    public static final double PANE_FACTOR = 12.0 / 7.0;
    /** A Grinberryall heir's stars and panes are rolled at base times this. */
    public static final double HEIR_FACTOR = 0.5;
    /** Ticks a star or pane that survived a roll is left alone. */
    public static final int GRACE_TICKS = 40;
    /** The most stars considered, the most things broken (and so the most effect packets) by one dispel. */
    public static final int MAX_CANDIDATES = 16, MAX_BREAKS = 8;
    /** A star within this many blocks of its owner's pane box is an end-star of that pane. */
    public static final double END_STAR_TOLERANCE = 1.0;
    /** Ceiling of the grace table (a bound on memory; far above the 512 + 8 things that can exist). */
    public static final int GRACE_CAP = 640;

    /** What is being rolled for: a free star, or a pane / one of the end-stars holding a pane. */
    public enum Role { NODE, PANE }

    /** The setting as read, clamped to 0..1 (a hand-edited file or NaN cannot break the bound). */
    public static double clampChance(double v) {
        if (Double.isNaN(v)) return DEFAULT_CHANCE;
        return Math.max(MIN_CHANCE, Math.min(MAX_CHANCE, v));
    }

    /** The chance that this star or pane breaks in one dispel: base, raised for a pane, halved for an heir, always within 0..1. */
    public static double chance(double base, Role role, boolean heir) {
        double c = clampChance(base);
        if (role == Role.PANE) c *= PANE_FACTOR;
        if (heir) c *= HEIR_FACTOR;
        return Math.max(0.0, Math.min(1.0, c));
    }

    /** One roll: {@code r01} is a uniform number in [0, 1). A chance of 0 never breaks, a chance of 1 always does. */
    public static boolean breaks(double chance, double r01) {
        if (chance <= 0.0) return false;
        if (chance >= 1.0) return true;
        return r01 < chance;
    }

    /** Squared distance from a point to a box {minX, minY, minZ, maxX, maxY, maxZ} (0 inside). */
    public static double boxDistSq(double[] b, double x, double y, double z) {
        double dx = Math.max(Math.max(b[0] - x, 0.0), x - b[3]);
        double dy = Math.max(Math.max(b[1] - y, 0.0), y - b[4]);
        double dz = Math.max(Math.max(b[2] - z, 0.0), z - b[5]);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * The "left alone for a while" table: id to the tick it may be rolled again. Bounded by {@link #GRACE_CAP}; expired entries are dropped when it fills. Server thread only; cleared with the registries.
     */
    public static final class Grace {
        private final Map<UUID, Long> until = new HashMap<>();

        public boolean ready(UUID id, long now) {
            Long t = until.get(id);
            return t == null || now >= t;
        }

        /** Remembers that this one survived a roll at {@code now}. A full table drops its expired entries first and, if still full, simply does not remember (the thing is rolled again next time). */
        public void survived(UUID id, long now) {
            if (until.size() >= GRACE_CAP) prune(now);
            if (until.size() >= GRACE_CAP && !until.containsKey(id)) return;
            until.put(id, now + GRACE_TICKS);
        }

        public void forget(UUID id) { until.remove(id); }

        public int prune(long now) {
            int n = 0;
            for (Iterator<Map.Entry<UUID, Long>> it = until.entrySet().iterator(); it.hasNext(); ) {
                if (now >= it.next().getValue()) { it.remove(); n++; }
            }
            return n;
        }

        public int size() { return until.size(); }

        public void clear() { until.clear(); }
    }
}
