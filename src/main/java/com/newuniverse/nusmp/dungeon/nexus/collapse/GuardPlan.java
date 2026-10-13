package com.newuniverse.nusmp.dungeon.nexus.collapse;

/**
 * The mutated dungeon guards of the collapse (docs/living_dungeon.md section 8) as numbers: when the waves come, how many, of which
 * tier, how strong the mutation makes them, and where round an exit they appear. Everything is capped, so a collapse can never be
 * flooded: at most {@link #MAX_ALIVE} guards at once, {@link #PER_EXIT_ALIVE} at one exit, {@link #MAX_SPAWNED} in all. Pure.
 */
public final class GuardPlan {
    private GuardPlan() {}

    /** A wave: when (ticks since the trigger), how many per exit, the chance in percent that a guard is tier 3 and not tier 2. */
    public record Wave(long at, int perExit, int tier3Percent) {}

    /** Five waves: none in the first half minute (time to read the room) and none in the last 35 seconds, so the final dash for a gate is never blocked by a newcomer. */
    public static final Wave[] WAVES = {
            new Wave(600L, 1, 0),
            new Wave(1500L, 1, 0),
            new Wave(2200L, 2, 25),
            new Wave(2600L, 2, 35),
            new Wave(2900L, 1, 50)
    };

    public static final int MAX_ALIVE = 10, PER_EXIT_ALIVE = 3, MAX_SPAWNED = 24;
    /** A guard shows as a ring of portal light this many ticks before it stands there. */
    public static final int TELEGRAPH = 40;
    /** No guard appears closer than this to a player, and rings of 3 to 7 blocks round the exit are tried. */
    public static final double MIN_PLAYER_GAP = 5.0;
    public static final int RING_MIN = 3, RING_MAX = 7;

    /** The newest wave that has come due among those not sent yet (`next` is the first unsent); -1 if none. */
    public static int dueWave(int next, long elapsed) {
        int found = -1;
        for (int i = Math.max(0, next); i < WAVES.length; i++) {
            if (elapsed >= WAVES[i].at()) found = i;
            else break;
        }
        return found;
    }

    /** How many guards wave `wave` may bring: its share for every exit, held to the caps given how many are alive and were made. */
    public static int count(int wave, int exits, int alive, int spawned) {
        if (wave < 0 || wave >= WAVES.length || exits <= 0) return 0;
        int want = WAVES[wave].perExit() * exits;
        return Math.max(0, Math.min(want, Math.min(MAX_ALIVE - alive, MAX_SPAWNED - spawned)));
    }

    /**
     * Which exit each guard of wave `wave` is made for: the exits are taken in turn from a start that moves with the wave, an exit that
     * already holds PER_EXIT_ALIVE (`atExit` has the alive and announced guards of every exit) is passed over, and the number is
     * {@link #count} for the guards alive and made so far. Never more than the caps; empty if nothing may be made.
     */
    public static int[] assign(int wave, int exits, int[] atExit, int alive, int spawned) {
        int want = count(wave, exits, alive, spawned);
        int[] load = new int[Math.max(exits, 0)];
        for (int i = 0; i < load.length && atExit != null && i < atExit.length; i++) load[i] = atExit[i];
        int[] out = new int[want];
        int made = 0;
        for (int k = 0; k < want; k++) {
            int pick = -1;
            for (int j = 0; j < exits && pick < 0; j++) {
                int e = Math.floorMod(wave + made + j, exits);
                if (roomAtExit(load[e])) pick = e;
            }
            if (pick < 0) break;
            load[pick]++;
            out[made++] = pick;
        }
        return made == out.length ? out : java.util.Arrays.copyOf(out, made);
    }

    /** May one more guard stand at an exit that already has `aliveThere`? */
    public static boolean roomAtExit(int aliveThere) { return aliveThere < PER_EXIT_ALIVE; }

    /** The tier (2 or 3) of a guard of this wave, from a roll of 0 to 99. */
    public static int tier(int wave, int roll) {
        if (wave < 0 || wave >= WAVES.length) return 2;
        return roll < WAVES[wave].tier3Percent() ? 3 : 2;
    }

    // ---------------------------------------------------------------- the mutation
    /** A mutation: extra health and attack as fractions of the base, extra pace as a fraction, and the absolute caps they are held to. */
    public record Mutation(double health, double attack, double speed, double healthCap, double attackCap) {}

    public static Mutation mutation(int tier) {
        return tier >= 3 ? new Mutation(0.40, 0.20, 0.08, 400.0, 36.0) : new Mutation(0.60, 0.30, 0.10, 240.0, 24.0);
    }

    /** What to add to a base value to grow it by `fraction`, but never past `cap` (and never below the base: a stronger creature stays as it is). */
    public static double extra(double base, double fraction, double cap) {
        if (base <= 0.0 || fraction <= 0.0) return 0.0;
        return Math.max(0.0, Math.min(base * fraction, cap - base));
    }

    // ---------------------------------------------------------------- where
    private static final int[][] RING = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    /** The offset {dx, dz} from an exit to try for guard `n` of exit `exit`, on its `attempt`-th try: a spot on a ring of RING_MIN to RING_MAX blocks, never the same twice in a row. */
    public static int[] offset(int exit, int n, int attempt) {
        int[] dir = RING[Math.floorMod(exit * 5 + n * 3 + attempt * 7, RING.length)];
        int r = RING_MIN + Math.floorMod(n + attempt * 2, RING_MAX - RING_MIN + 1);
        return new int[]{dir[0] * r, dir[1] * r};
    }
}
