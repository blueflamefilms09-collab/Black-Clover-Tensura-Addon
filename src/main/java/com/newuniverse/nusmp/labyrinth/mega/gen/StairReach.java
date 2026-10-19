package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Why: the owner's second request is "remove the random being locked in a room or add a way to get out", and the quietest way a dungeon can lock somebody in is a room
 * the generator placed that has NO way out: a closed pocket behind a wall, a room whose only doorway was capped, a stairwell nobody can walk to. The audit already
 * counts rooms that cannot be reached from the arrival; this is the explicit, stand-alone form of the rule the owner states: EVERY placed room has a path to the
 * stairs. It is a flood fill over the room graph (rooms are nodes, doorways are edges) with no Minecraft type, so it runs in a unit test over thousands of seeds, on a
 * plan before anything is placed (the generator's audit calls {@link #problems}) and on the saved sector of a running game (the escape rules and the
 * {@code lock.escape} self-test call {@link #analyse}). It answers four questions: which rooms cannot be reached from the arrival, which rooms have no path to any
 * stairwell, whether the arrival and the forward stairs are joined, and, for the rescue, which room is the NEAREST one that satisfies a test (a safe room, a stairwell)
 * from where a player stands. It also finds the rooms that every walk from the arrival to the forward stairs must pass through ({@link #pinchRooms}), which tells how
 * much of a floor depends on one doorway; a lockable room (a trial) on such a pinch is the case where a stuck trial blocks progress (never traps: the release rules
 * open it), and the doc reports how rare the generator keeps it.
 */
public final class StairReach {
    private StairReach() {}

    /**
     * The result of a flood fill.
     *
     * @param rooms        the number of rooms
     * @param arrival      the room the walk starts from (-1 when there is none)
     * @param stairs       the rooms that are stairs
     * @param cutOff       rooms the arrival cannot reach, ascending
     * @param noStairs     rooms with no path to any stairwell, ascending
     * @param stairsJoined every stairwell can be walked to from the arrival
     * @param farthest     the largest walking distance (doorways) from the arrival to any reachable room
     */
    public record Report(int rooms, int arrival, List<Integer> stairs, List<Integer> cutOff, List<Integer> noStairs, boolean stairsJoined, int farthest) {
        public boolean ok() { return rooms == 0 || (arrival >= 0 && cutOff.isEmpty() && noStairs.isEmpty() && stairsJoined); }
    }

    /** The flood fill on plain numbers: {@code n} rooms 0..n-1, undirected doorways, the arrival and the stairs. Bad edges are ignored (see {@link WalkGraph}). */
    public static Report analyse(int n, List<int[]> edges, int arrival, List<Integer> stairs) {
        int rooms = Math.max(0, n);
        int[] fromArrival = WalkGraph.distances(rooms, edges, arrival);
        int[] toStairs = multi(rooms, edges, stairs);
        List<Integer> cut = new ArrayList<>(), none = new ArrayList<>();
        int far = 0;
        for (int i = 0; i < rooms; i++) {
            if (fromArrival[i] < 0) cut.add(i);
            else far = Math.max(far, fromArrival[i]);
            if (toStairs[i] < 0) none.add(i);
        }
        boolean joined = true;
        for (int s : stairs) if (s < 0 || s >= rooms || fromArrival[s] < 0) joined = false;
        return new Report(rooms, arrival < 0 || arrival >= rooms ? -1 : arrival, List.copyOf(stairs), cut, none, joined, far);
    }

    /** Walking distances (doorways) from the nearest of {@code sources}, -1 for a room none of them reaches. */
    public static int[] multi(int n, List<int[]> edges, List<Integer> sources) {
        int[] best = new int[Math.max(0, n)];
        Arrays.fill(best, -1);
        for (int s : sources) {
            int[] d = WalkGraph.distances(best.length, edges, s);
            for (int i = 0; i < best.length; i++) if (d[i] >= 0 && (best[i] < 0 || d[i] < best[i])) best[i] = d[i];
        }
        return best;
    }

    /**
     * The room nearest to {@code from} (by doorways, ties to the lower id) for which {@code goal} is true, or -1 when none can be reached. {@code from} itself counts
     * (distance 0) when it satisfies the test.
     */
    public static int nearest(int n, List<int[]> edges, int from, IntPredicate goal) {
        int[] d = WalkGraph.distances(n, edges, from);
        int best = -1;
        for (int i = 0; i < d.length; i++) {
            if (d[i] < 0 || !goal.test(i)) continue;
            if (best < 0 || d[i] < d[best]) best = i;
        }
        return best;
    }

    /**
     * The rooms that EVERY walk from {@code from} to {@code to} must pass through (not counting the two ends): remove one and the two ends fall apart. Found the plain way
     * (remove each room in turn and flood again), which is quick enough for a floor of a few dozen rooms and impossible to get wrong.
     */
    public static List<Integer> pinchRooms(int n, List<int[]> edges, int from, int to) {
        List<Integer> out = new ArrayList<>();
        if (from < 0 || to < 0 || from >= n || to >= n || from == to) return out;
        if (WalkGraph.distances(n, edges, from)[to] < 0) return out;
        for (int skip = 0; skip < n; skip++) {
            if (skip == from || skip == to) continue;
            List<int[]> rest = new ArrayList<>();
            for (int[] e : edges) if (e != null && e.length >= 2 && e[0] != skip && e[1] != skip) rest.add(e);
            if (WalkGraph.distances(n, rest, from)[to] < 0) out.add(skip);
        }
        return out;
    }

    // ================================================================ a generated plan
    /** The rooms that count as stairs in a plan: the stairwells of the floor model, else the Landing (the old sector model has no stairs; its way out is the Landing). */
    public static List<Integer> stairRooms(Result r) {
        int[] ids = r.roomIds();
        List<Integer> out = new ArrayList<>();
        for (Placed p : r.pieces) if (p.shape.kind == RoomKind.STAIRWELL && ids[p.index] >= 0) out.add(ids[p.index]);
        if (out.isEmpty() && r.landing >= 0 && ids[r.landing] >= 0) out.add(ids[r.landing]);
        return out;
    }

    /** The flood fill of a finished plan: the arrival is the first room (the arrival stairwell, or the Landing). */
    public static Report of(Result r) {
        int[] ids = r.roomIds();
        int rooms = 0;
        for (int id : ids) if (id >= 0) rooms++;
        int arrival = r.landing >= 0 ? ids[r.landing] : -1;
        return analyse(rooms, r.edges(), arrival, stairRooms(r));
    }

    /** The problems of a plan, as sentences without a semicolon (the audit joins them): empty means every room has a walk to the stairs. */
    public static List<String> problems(Result r) {
        List<String> out = new ArrayList<>();
        if (r == null || !r.assembled()) return out;
        Report rep = of(r);
        if (rep.rooms() == 0) return out;
        if (rep.arrival() < 0) out.add("no arrival room to start a walk from");
        if (!rep.cutOff().isEmpty()) out.add(rep.cutOff().size() + " room(s) cannot be walked to from the arrival (first: room " + rep.cutOff().get(0) + ")");
        if (!rep.noStairs().isEmpty()) out.add(rep.noStairs().size() + " room(s) have no walkable path to a stairwell (first: room " + rep.noStairs().get(0) + ")");
        if (!rep.stairsJoined()) out.add("a stairwell cannot be walked to from the arrival");
        return out;
    }

    /** The lockable rooms (a trial, a puzzle hall) that every walk from the arrival to the forward stairs must pass through. Information for the doc and the tests; not an error. */
    public static List<Integer> lockablePinch(Result r) {
        List<Integer> stairs = stairRooms(r);
        if (stairs.size() < 2) return List.of();
        int[] ids = r.roomIds();
        int arrival = ids[r.landing];
        int forward = -1;
        for (int s : stairs) if (s != arrival) forward = s;
        int rooms = 0;
        for (int id : ids) if (id >= 0) rooms++;
        List<Integer> out = new ArrayList<>();
        List<Integer> pinch = pinchRooms(rooms, r.edges(), arrival, forward);
        for (Placed p : r.pieces) {
            int id = ids[p.index];
            if (id >= 0 && pinch.contains(id) && (p.shape.kind == RoomKind.TRIAL || p.shape.kind == RoomKind.PUZZLE)) out.add(id);
        }
        return out;
    }

    /** The edges of an adjacency given as pairs, ignoring anything malformed (a saved sector's list is trusted less than a fresh plan). */
    public static List<int[]> cleanEdges(List<int[]> edges, int n) {
        List<int[]> out = new ArrayList<>();
        if (edges != null) for (int[] e : edges) if (e != null && e.length >= 2 && e[0] >= 0 && e[1] >= 0 && e[0] < n && e[1] < n && e[0] != e[1]) out.add(new int[] {e[0], e[1]});
        return out;
    }
}
