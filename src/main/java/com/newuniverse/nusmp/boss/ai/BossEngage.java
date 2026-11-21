package com.newuniverse.nusmp.boss.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Why: bosses of this mod stood still and watched because they only ever took a target through vanilla's "nearest player I can see" goal (line of sight, the
 * player's visibility modifier, a one-in-ten random roll) or after being hit; with a wall, a pillar, an invisible or sneaking player or a lost line the target stayed
 * null, so the fight (which starts only when a target exists) never began and the look-at goal made the boss stare. The engage rule is this pure chooser (unit
 * tested): given the players near a boss it answers the one the boss should fight, or none. Rules: alive, not a spectator, not creative; within the notice range;
 * inside {@link #SEE_FREE_RANGE} blocks (or reaching distance) no line of sight is needed; farther out it needs a line of sight or a walkable path. Nearest first,
 * at most {@link #MAX_CANDIDATES} considered, the path question asked at most once. No Minecraft types, so a dedicated server and the tests share it.
 */
public final class BossEngage {
    private BossEngage() {}

    public static final int MIN_RANGE = 8, MAX_RANGE = 96, DEFAULT_RANGE = 32;
    /** Inside this many blocks a player is noticed through walls (an arena is never wider than this where the boss stands). */
    public static final double SEE_FREE_RANGE = 12.0;
    /** A boss re-picks its target at most this often (ticks); the scan is one box query. */
    public static final int SCAN_INTERVAL = 10;
    /** The path question is asked at most this often (ticks) per boss. */
    public static final int PATH_RETRY = 40;
    public static final int MAX_CANDIDATES = 10;
    /** A boss that has started no move for this long (ticks) closes in and melees instead of waiting for one. */
    public static final int QUIET_TICKS = 80;

    /** A player near the boss, as plain numbers. {@code inReach}: inside the boss's own striking distance. */
    public record Candidate(int id, double distSq, boolean alive, boolean spectator, boolean creative, boolean inReach) {}

    public static int clampRange(int range) { return Math.max(MIN_RANGE, Math.min(MAX_RANGE, range)); }

    /** Is this player a possible target at all (the creative and spectator rule, and range)? */
    public static boolean fair(Candidate c, double range) {
        return c != null && c.alive() && !c.spectator() && !c.creative() && c.distSq() <= range * range;
    }

    /** Is it time to look again? Pure compare, so the goal costs two long reads between scans. */
    public static boolean due(long now, long nextScan) { return now >= nextScan; }

    /**
     * The id of the player to fight, or -1. {@code sees(id)} is asked only for a candidate beyond {@link #SEE_FREE_RANGE} and out of reach;
     * {@code canPath(id)} only for the first such candidate that cannot be seen, and only when {@code pathAllowed}.
     */
    public static int choose(List<Candidate> all, double range, boolean pathAllowed, IntPredicate sees, IntPredicate canPath) {
        List<Candidate> near = new ArrayList<>(Math.min(all.size(), MAX_CANDIDATES));
        for (Candidate c : all) if (fair(c, range)) near.add(c);
        near.sort(Comparator.comparingDouble(Candidate::distSq));
        boolean pathAsked = false;
        int n = 0;
        for (Candidate c : near) {
            if (n++ >= MAX_CANDIDATES) break;
            if (c.inReach() || c.distSq() <= SEE_FREE_RANGE * SEE_FREE_RANGE || sees.test(c.id())) return c.id();
            if (pathAllowed && !pathAsked) {
                pathAsked = true;
                if (canPath.test(c.id())) return c.id();
            }
        }
        return -1;
    }

    /** Has a boss gone without a move long enough that it should walk up and strike? */
    public static boolean rush(long now, long lastCast) { return now - lastCast >= QUIET_TICKS; }
}
