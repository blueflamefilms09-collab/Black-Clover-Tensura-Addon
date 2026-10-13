package com.newuniverse.nusmp.dungeon.nexus.collapse;

/**
 * The frenzied horde of the Treasury Collapse (docs/living_dungeon.md section 8) as numbers: when the waves come, how many creatures each
 * brings, how a wave is split between the ways out and the players inside, how strong the frenzy makes them, and where they are put when no
 * wall can birth them. The horde bypasses the room caps of the sentient Dungeon on purpose and keeps a hard ceiling of its own, so a
 * collapse can never flood the server: at most {@link #MAX_ALIVE} alive at once, {@link #PER_EXIT_ALIVE} near one exit or one player,
 * {@link #MAX_SPAWNED} in all. Pure.
 */
public final class GuardPlan {
    private GuardPlan() {}

    /** A wave: when (ticks since the trigger), how many are born at each exit, the chance in percent that a creature of the fallback roster is tier 3 and not tier 2. */
    public record Wave(long at, int perExit, int tier3Percent) {}

    /**
     * Six waves, growing: the first at +20 seconds, none in the last 35 seconds, so the final dash for a gate is never blocked by a newcomer.
     * Every wave also brings about half as many round the one player who is farthest from every exit ({@link #playerShare}).
     */
    public static final Wave[] WAVES = {
            new Wave(400L, 2, 0),
            new Wave(1000L, 2, 0),
            new Wave(1600L, 3, 15),
            new Wave(2100L, 3, 25),
            new Wave(2500L, 4, 35),
            new Wave(2900L, 3, 50)
    };

    /** The collapse's own ceiling: alive at once, alive near one exit or one player, born in all. (They ignore the room caps of the sentient Dungeon.) */
    public static final int MAX_ALIVE = 24, PER_EXIT_ALIVE = 6, MAX_SPAWNED = 56;
    /** A wall cracks this many ticks before the creature steps out of it (the delay handed to the wall birth). */
    public static final int TELEGRAPH = 40;
    /** No creature is put closer than this to a player. */
    public static final double MIN_PLAYER_GAP = 5.0;
    /** The walls are searched from EXIT_MIN to EXIT_MAX blocks round an exit, and from PLAYER_MIN to PLAYER_MAX blocks round a player. */
    public static final int EXIT_MIN = 4, EXIT_MAX = 16, PLAYER_MIN = 7, PLAYER_MAX = 18;
    /** When no wall can birth one, a ring of RING_MIN to RING_MAX blocks round the exit is used (round a player, PLAYER_RING more). */
    public static final int RING_MIN = 3, RING_MAX = 7, PLAYER_RING = 4;
    /** A creature counts as near a player within this many blocks (for the per-player ceiling). */
    public static final double NEAR_PLAYER = 20.0;
    /** The slot of a creature born round a player and not at an exit (exits are numbered from 0). */
    public static final int PLAYER = -1;

    /** The frenzy: speed +30% and attack damage +25% of the base (the attack held to FRENZY_ATTACK_CAP in all), and FRENZY_SIGHT more blocks of follow range so it can find a player across the hall. */
    public static final double FRENZY_SPEED = 0.30, FRENZY_ATTACK = 0.25, FRENZY_ATTACK_CAP = 40.0, FRENZY_SIGHT = 48.0;

    /** The newest wave that has come due among those not sent yet (`next` is the first unsent); -1 if none. */
    public static int dueWave(int next, long elapsed) {
        int found = -1;
        for (int i = Math.max(0, next); i < WAVES.length; i++) {
            if (elapsed >= WAVES[i].at()) found = i;
            else break;
        }
        return found;
    }

    // ---------------------------------------------------------------- how many
    /** What wave `wave` brings to the one player it follows: about half of what an exit gets (a round up), and none for a wave that does not exist. */
    public static int playerShare(int wave) {
        if (wave < 0 || wave >= WAVES.length) return 0;
        return (WAVES[wave].perExit() + 1) / 2;
    }

    /** What wave `wave` wants before any cap: its share for every exit, and the player's share when there is a player to follow. */
    public static int wanted(int wave, int exits, boolean player) {
        if (wave < 0 || wave >= WAVES.length) return 0;
        return WAVES[wave].perExit() * Math.max(exits, 0) + (player ? playerShare(wave) : 0);
    }

    /** How many creatures wave `wave` may bring: what it wants, held to the room left under MAX_ALIVE and MAX_SPAWNED given how many are alive (or on the way) and were made. */
    public static int count(int wave, int exits, boolean player, int alive, int spawned) {
        long room = Math.min((long) MAX_ALIVE - alive, (long) MAX_SPAWNED - spawned);
        return (int) Math.max(0L, Math.min(wanted(wave, exits, player), room));
    }

    /** May one more creature stand at an exit (or near a player) that already has `aliveThere`? */
    public static boolean roomAtExit(int aliveThere) { return aliveThere < PER_EXIT_ALIVE; }

    /**
     * Where each creature of wave `wave` is born: an exit number from 0, or {@link #PLAYER}. The exits are taken in turn from a start that
     * moves with the wave and an exit that already holds PER_EXIT_ALIVE (`atExit` has the alive and announced creatures of every exit) is
     * passed over; with a player to follow (`player`, who has `atPlayer` of them near already) the player's share is added, at most up to
     * PER_EXIT_ALIVE there. Exits and player alternate, so when a ceiling cuts the wave short both still get some. The length is never
     * more than {@link #count} for the creatures alive and made so far; empty if nothing may be made.
     */
    public static int[] assign(int wave, int exits, int[] atExit, boolean player, int atPlayer, int alive, int spawned) {
        int n = Math.max(exits, 0);
        int allowed = count(wave, n, player, alive, spawned);
        if (allowed == 0) return new int[0];
        int[] load = new int[n];
        for (int i = 0; i < n && atExit != null && i < atExit.length; i++) load[i] = atExit[i];
        int wantExit = WAVES[wave].perExit() * n;
        int[] byExit = new int[wantExit];
        int madeExit = 0;
        for (int k = 0; k < wantExit; k++) {
            int pick = -1;
            for (int j = 0; j < n && pick < 0; j++) {
                int e = Math.floorMod(wave + madeExit + j, n);
                if (roomAtExit(load[e])) pick = e;
            }
            if (pick < 0) break;
            load[pick]++;
            byExit[madeExit++] = pick;
        }
        int wantPlayer = player ? Math.min(playerShare(wave), Math.max(0, PER_EXIT_ALIVE - Math.max(0, atPlayer))) : 0;
        int[] out = new int[Math.min(allowed, madeExit + wantPlayer)];
        int ie = 0, ip = 0, made = 0;
        boolean exitsTurn = true;
        while (made < out.length) {
            boolean haveExit = ie < madeExit, havePlayer = ip < wantPlayer;
            if (!haveExit && !havePlayer) break;
            if (haveExit && (exitsTurn || !havePlayer)) {
                out[made++] = byExit[ie++];
            } else {
                out[made++] = PLAYER;
                ip++;
            }
            exitsTurn = !exitsTurn;
        }
        return made == out.length ? out : java.util.Arrays.copyOf(out, made);
    }

    /**
     * Which of the players (their positions given) the horde follows in wave `wave`: the one farthest from every exit (the greatest distance
     * to the nearest exit; the first of equals), so the creatures come out of the walls where people are and not only at the gates. With no
     * exits the players are taken in turn by wave. -1 if there are no players. Exit positions are blocks (the middle of the block counts).
     */
    public static int targetPlayer(int wave, double[] px, double[] pz, int[] ex, int[] ez) {
        int n = Math.min(px == null ? 0 : px.length, pz == null ? 0 : pz.length);
        if (n == 0) return -1;
        int m = Math.min(ex == null ? 0 : ex.length, ez == null ? 0 : ez.length);
        if (m == 0) return Math.floorMod(wave, n);
        int best = 0;
        double bestD = -1.0;
        for (int i = 0; i < n; i++) {
            double d = Double.MAX_VALUE;
            for (int j = 0; j < m; j++) {
                double dx = px[i] - (ex[j] + 0.5), dz = pz[i] - (ez[j] + 0.5);
                d = Math.min(d, dx * dx + dz * dz);
            }
            if (d > bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /** The tier (2 or 3) of a creature of the fallback roster in this wave, from a roll of 0 to 99. */
    public static int tier(int wave, int roll) {
        if (wave < 0 || wave >= WAVES.length) return 2;
        return roll < WAVES[wave].tier3Percent() ? 3 : 2;
    }

    // ---------------------------------------------------------------- the frenzy
    /** What to add to a base value to grow it by `fraction`, but never past `cap` (and never below the base: a stronger creature stays as it is). */
    public static double extra(double base, double fraction, double cap) {
        if (base <= 0.0 || fraction <= 0.0) return 0.0;
        return Math.max(0.0, Math.min(base * fraction, cap - base));
    }

    /** A mutation (1.21.0, the portal-light guards; no longer applied): extra health and attack as fractions of the base, extra pace as a fraction, and the absolute caps they are held to. */
    public record Mutation(double health, double attack, double speed, double healthCap, double attackCap) {}

    public static Mutation mutation(int tier) {
        return tier >= 3 ? new Mutation(0.40, 0.20, 0.08, 400.0, 36.0) : new Mutation(0.60, 0.30, 0.10, 240.0, 24.0);
    }

    // ---------------------------------------------------------------- where, when no wall can birth one
    private static final int[][] RING = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    /** The offset {dx, dz} from an exit to try for creature `n` of slot `exit`, on its `attempt`-th try: a spot on a ring of RING_MIN to RING_MAX blocks, never the same twice in a row. */
    public static int[] offset(int exit, int n, int attempt) {
        int[] dir = RING[Math.floorMod(exit * 5 + n * 3 + attempt * 7, RING.length)];
        int r = RING_MIN + Math.floorMod(n + attempt * 2, RING_MAX - RING_MIN + 1);
        return new int[]{dir[0] * r, dir[1] * r};
    }
}
