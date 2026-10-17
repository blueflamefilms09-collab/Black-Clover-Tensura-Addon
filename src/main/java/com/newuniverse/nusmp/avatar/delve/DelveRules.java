package com.newuniverse.nusmp.avatar.delve;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: sending an Avatar into the Labyrinth alone is the riskiest thing in this system, so every limit on it is a rule written down and tested: it only goes
 * when everything allows it and it is healthy, it never stays longer than its time budget, it comes home when it is hurt, when its pockets are full, when its
 * creator calls it back, or when the creator is no longer there to come home to; it never goes deeper than its creator has been and never deeper than the
 * configured cap; and the way it explores is a plain walk through the floor's rooms, nearest unvisited room first. No game types: the goal
 * ({@link AvatarDelveGoal}) reads the world and applies these answers.
 */
public final class DelveRules {
    private DelveRules() {}

    /** Why the Avatar turns back. */
    public enum Reason { NONE, HURT, TIME, POCKETS_FULL, TOLD, CREATOR_GONE, DISABLED, OUTMATCHED }

    /** The facts of the trip so far. */
    public record Status(double hpFraction, long elapsedTicks, boolean pocketsFull, boolean told, boolean creatorGone, boolean enabled) {}

    /** The reason to turn back now, or NONE. The order is the order of urgency: the switch, the creator, the call, the wounds, the foe, the load, the clock. */
    public static Reason mustReturn(Status s, double returnBelowHp, long budgetTicks, boolean outmatched) {
        if (!s.enabled()) return Reason.DISABLED;
        if (s.creatorGone()) return Reason.CREATOR_GONE;
        if (s.told()) return Reason.TOLD;
        if (s.hpFraction() < returnBelowHp) return Reason.HURT;
        if (outmatched) return Reason.OUTMATCHED;
        if (s.pocketsFull()) return Reason.POCKETS_FULL;
        if (s.elapsedTicks() >= budgetTicks) return Reason.TIME;
        return Reason.NONE;
    }

    /** The same without the foe check (nothing outmatches the Avatar). */
    public static Reason mustReturn(Status s, double returnBelowHp, long budgetTicks) { return mustReturn(s, returnBelowHp, budgetTicks, false); }

    /**
     * Is the foe one the Avatar should fight rather than leave? A boss only when it is no stronger than the Avatar; anything else up to three times the Avatar's
     * EP. An unknown EP (0 or less) is fought unless it is a boss.
     */
    public static boolean worthEngaging(double avatarEp, double foeEp, boolean boss) {
        if (avatarEp <= 0.0 || foeEp <= 0.0) return !boss;
        return foeEp <= avatarEp * (boss ? 1.0 : 3.0);
    }

    /** The one-line reason the creator is told when the Avatar comes home. */
    public static String describe(Reason r) {
        return switch (r) {
            case HURT -> "it was badly hurt";
            case TIME -> "its time was up";
            case POCKETS_FULL -> "its pockets were full";
            case TOLD -> "it was called back";
            case CREATOR_GONE -> "its creator was gone";
            case DISABLED -> "the delving was switched off";
            case OUTMATCHED -> "something stronger than it was near";
            case NONE -> "it finished its round";
        };
    }

    /** Ticks of a trip: the configured minutes, at least one. */
    public static long budgetTicks(int minutes) { return Math.max(1, minutes) * 1200L; }

    /**
     * May the Avatar start a trip? Everything has to be on: the switch, the Mega-Dungeon, a loyal Avatar told to delve, healthy enough, and out of the rest period
     * after its last trip.
     */
    public static boolean mayStart(boolean enabled, boolean dungeonAvailable, boolean stanceIsDelve, boolean loyal, double hpFraction, double minHp, long now, long notBefore) {
        return enabled && dungeonAvailable && stanceIsDelve && loyal && hpFraction >= minHp && now >= notBefore;
    }

    /**
     * The chain index of the Landing to arrive at: the deepest one the creator has reached on the road, but not beyond the newest playable one
     * ({@code playableCount} sectors, indices 0 .. count-1) and not beyond {@code cap}; -1 when no sector is playable.
     */
    public static int landingIndex(int creatorDeepest, int playableCount, int cap) {
        if (playableCount <= 0) return -1;
        int idx = Math.max(0, creatorDeepest);
        idx = Math.min(idx, playableCount - 1);
        return Math.min(idx, Math.max(0, cap));
    }

    // ---------------------------------------------------------------- exploring
    /**
     * The next room to walk to: the nearest room (fewest doorways) not yet visited, found by a breadth-first walk of the floor's room graph from {@code from};
     * neighbours are tried in the order of their ids so the walk is repeatable. -1 when every reachable room has been visited.
     */
    public static int nextRoom(Map<Integer, List<Integer>> graph, int from, Set<Integer> visited) {
        Deque<Integer> queue = new ArrayDeque<>();
        Set<Integer> seen = new HashSet<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur != from && !visited.contains(cur)) return cur;
            List<Integer> next = new ArrayList<>(graph.getOrDefault(cur, List.of()));
            next.sort(null);
            for (int n : next) if (seen.add(n)) queue.add(n);
        }
        return -1;
    }

    /** Has the Avatar hardly moved over a stretch of time (it is stuck behind a door or a trap)? */
    public static boolean stuck(double movedBlocks, long ticksObserved) { return ticksObserved >= 100 && movedBlocks < 1.5; }
}
