package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * How many creatures the Golden City's garden holds and when it brings in another (docs/living_dungeon.md, "The Golden City": apex
 * predators roam freely in a thriving, balanced ecosystem). The garden is the peak of the food web: more apex predators than anywhere else
 * in the Dungeon, kept alive by herds of tier 1 and 2 grazers, never more than a hard cap of either, and brought back slowly when too few
 * are left. Pure: the glue ({@link GoldenWild}) counts the creatures, asks {@link #decide}, and makes or removes one.
 */
final class GoldenEcology {
    private GoldenEcology() {}

    /** What the garden aims for and the most it will hold: apex predators (tier 3, legendary ones counted apart) and prey (tier 1 and 2). */
    static final int APEX_TARGET = 5, APEX_CAP = 8, LEGEND_CAP = 1, PREY_FLOOR = 8, PREY_TARGET = 26, PREY_CAP = 40;
    /** Game ticks that must pass between two births of each kind, so a hunt that empties the garden is made good slowly. */
    static final long PREY_EVERY = 600L, APEX_EVERY = 1800L, LEGEND_EVERY = 18000L;
    /** How many blocks from every player a creature must be put down (or taken away from). */
    static final double SPAWN_CLEAR = 24.0, CULL_CLEAR = 40.0, GATE_CLEAR = 14.0;

    enum Act { NONE, PREY, APEX, LEGEND, CULL_PREY, CULL_APEX }

    /** The creatures of the garden now: prey (tier 1 and 2), apex predators (tier 3, not legendary), legendary ones. */
    record Census(int prey, int apex, int legend) {}

    /** How many prey the apex predators of the garden need: a floor, and a few more for each one of them, up to the target. */
    static int preyWanted(int apex) { return Math.max(PREY_FLOOR, Math.min(PREY_TARGET, PREY_FLOOR + 3 * apex)); }

    /** Ticks since `last`; a clock that went backwards (another world) or a birth that never happened counts as long ago. */
    static long since(long now, long last) { return last == Long.MIN_VALUE || now < last ? Long.MAX_VALUE / 4 : now - last; }

    /**
     * The one thing the garden does this time round. A cull first (too many apex predators, or far too many prey), then a legendary one
     * (rarely, only in a thriving garden), then prey if the herd is short, then an apex predator if the herd could feed one more.
     *
     * @param legendsExist whether the roster has any legendary creature at all
     * @param roll         a number in [0, 1) for the chances
     */
    static Act decide(Census c, long sincePrey, long sinceApex, long sinceLegend, boolean legendsExist, double roll) {
        if (c.apex() > APEX_CAP) return Act.CULL_APEX;
        if (c.prey() > PREY_CAP) return Act.CULL_PREY;
        if (legendsExist && c.legend() < LEGEND_CAP && c.apex() >= 3 && c.prey() >= 16 && sinceLegend >= LEGEND_EVERY && roll < 0.35) return Act.LEGEND;
        if (c.prey() < preyWanted(c.apex()) && sincePrey >= PREY_EVERY) return Act.PREY;
        if (c.apex() < APEX_TARGET && c.prey() >= 3 * c.apex() + 4 && sinceApex >= APEX_EVERY) return Act.APEX;
        return Act.NONE;
    }

    /**
     * The first of the candidate places, starting at the one the roll picks, that is at least `playerClear` blocks from every player and
     * `gateClear` from every gate; -1 if there is none. Places are {x, y, z} (block coordinates), players and gates {x, z}.
     */
    static int pick(int[][] places, double[][] players, double[][] gates, double playerClear, double gateClear, double roll) {
        int n = places.length;
        if (n == 0) return -1;
        int start = (int) (Math.max(0.0, Math.min(0.999999999, roll)) * n);
        for (int k = 0; k < n; k++) {
            int i = (start + k) % n;
            if (clear(places[i], players, playerClear) && clear(places[i], gates, gateClear)) return i;
        }
        return -1;
    }

    private static boolean clear(int[] p, double[][] others, double distance) {
        for (double[] o : others) {
            double dx = p[0] + 0.5 - o[0], dz = p[2] + 0.5 - o[1];
            if (dx * dx + dz * dz < distance * distance) return false;
        }
        return true;
    }

    /**
     * Which of the creatures (by their distance to the nearest player, squared) to take away when there are too many: the one farthest
     * from everybody, and only if it is at least {@link #CULL_CLEAR} away; -1 if every one is too near to vanish unseen.
     */
    static int cull(double[] nearestPlayerSq) {
        int best = -1;
        double far = CULL_CLEAR * CULL_CLEAR;
        for (int i = 0; i < nearestPlayerSq.length; i++) {
            if (nearestPlayerSq[i] >= far && (best < 0 || nearestPlayerSq[i] > nearestPlayerSq[best])) best = i;
        }
        return best;
    }
}
