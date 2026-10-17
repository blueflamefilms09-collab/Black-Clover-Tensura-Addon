package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the owner talks about FLOORS (Floor +1, "clear 5 rooms on this floor", a boss on every tenth), but the engine of 1.54.0 builds a SECTOR: an
 * unlabelled maze of about a hundred rooms with a Landing at one end and the Guardian's arena at the other. Nothing in a room says how far along the
 * sector it is, so the floor has to be measured, and the one honest measure the graph offers is the number of doorways a walker needs from the Landing
 * (the breadth-first depth). Banding that depth into {@code perRegion} floors makes the Landing floor 1 and the arena floor 10 (see
 * {@link FloorMap#bandOfDepth}), keeps every floor connected to the one before it, and costs the template and generator builders nothing: no template
 * needs a floor marker, and a regenerated sector is banded again from its own graph. It is a PURE class (plain ints, a list of node records and the
 * contract's pure {@link RoomKind} and {@link FloorMap}; no Minecraft import) so JUnit can pin every rule, including the odd sectors a generator can
 * produce (no arena, an unreachable room, loops, a one-room sector), and so the same input always gives the same bands: the result depends on the
 * graph alone, never on the order of the rooms or of the edges.
 *
 * <p>Rules common to both strategies, in the order they are applied to each room:
 * <ol>
 *   <li>The Landing (the root of the search) is band 1, whatever the arena depth turns out to be.</li>
 *   <li>The arena and everything behind it ({@link #isBeyondArena}: arena, antechamber, vault, the Way Onward) is the LAST band, whatever its depth: the
 *       boss and the Crucible belong to the decennial floor even when a loop made the walk to them short.</li>
 *   <li>A link tunnel ({@link RoomKind#LINK}) is band 1: it only carries the player from the previous sector to this Landing.</li>
 *   <li>A room that cannot be reached from the Landing is the last band (it cannot be walked to, so it must not pose as an early floor).</li>
 *   <li>Every other room ("plain") gets its band from its depth, in one of two ways ({@link Strategy}).</li>
 * </ol>
 *
 * <p>{@link Strategy#ARENA} is the rule of the design brief: {@link FloorMap#bandOfDepth} of the room's depth over the ARENA's depth, so the depths from the
 * Landing to the arena are spread over bands 1..perRegion-1 and everything as deep as the arena is the last band. With no usable arena (none exists, it
 * cannot be reached, or it stands in the Landing's own cell) the deepest reachable room plays the arena, so a failed or odd sector still gets a sensible
 * spread and never an exception. It is exact when the arena is at least perRegion-1 doorways from the Landing and few rooms are deeper than the arena.
 * The assembler does not promise that: it fills a region with about a hundred rooms and many loops (rooms that merely touch share a doorway), so on the
 * simulator's output (200 sectors of the assembler over its stand-in catalog, measured for this class: 123 rooms, the arena 5.4 doorways from the Landing on
 * average, the deepest room 10.4) three or four of the nine early floors have no room at all and more than half of the rooms (all the ones as deep as the
 * arena) pile up on the last floor.
 *
 * <p>{@link Strategy#EVEN} keeps the same invariants but spreads the plain rooms by RANK instead of by raw depth: they are sorted by (depth, room id) and
 * cut into perRegion-1 runs of (nearly) equal size, so every one of the floors 1..perRegion-1 holds about the same number of rooms (the "about ten rooms
 * per floor" the design counts on), the last floor is the boss chain alone, and a deeper room never has a lower band (the sort puts every shallower room
 * first), so bands still never fall along a shortest path. The price is that two rooms at the SAME depth may land on neighbouring floors (the cut can
 * fall inside a depth level); the order inside a level is the room id, which is placement order and so keeps siblings together. The result depends only on
 * the graph (depths and ids), never on the order the rooms or edges are listed in. On the same 200 simulated sectors it puts 13 rooms on each of the nine
 * early floors and 4 on the last (the arena, the antechamber, the vault and the Way Onward) and leaves no floor empty.
 */
public final class FloorBands {
    private FloorBands() {}

    /** "No such room": room ids are never negative (the generator counts them from 0), so -1 can stand for "none" in the arguments below. */
    public static final int NONE = -1;

    /**
     * Why: the pure view of one room: its id and its {@link RoomKind}, which is all the banding needs to know about it. The game-side class that owns the
     * real rooms ({@code FloorAssigner}) copies them into these so this class needs no Minecraft type.
     */
    public record Node(int id, RoomKind kind) {}

    /**
     * Why: the design brief's way of turning depth into floors (the arena's depth is the scale) does not use the floors evenly on the sectors the assembler
     * really builds (see the class comment), so the choice is explicit, stored nowhere (the numbers are saved with the sector) and switched by one config key.
     */
    public enum Strategy {
        /** The brief's rule: {@link FloorMap#bandOfDepth} over the arena's depth. */
        ARENA("arena"),
        /** Plain rooms cut into runs of equal size by (depth, id): every floor is used and holds about the same number of rooms. */
        EVEN("even");

        private final String id;

        Strategy(String id) { this.id = id; }

        /** The id used in the config and the docs ({@code arena}, {@code even}). */
        public String id() { return id; }

        /** The strategy with this id (case-insensitive), or null for null or an unknown id (the caller decides the fallback). */
        public static Strategy fromId(String id) {
            if (id == null) return null;
            for (Strategy s : values()) if (s.id.equalsIgnoreCase(id.trim())) return s;
            return null;
        }
    }

    /** Kinds that stand at or behind the Guardian's arena: they are always the last band. */
    public static boolean isBeyondArena(RoomKind kind) {
        return kind == RoomKind.ARENA || kind == RoomKind.ANTECHAMBER || kind == RoomKind.VAULT || kind == RoomKind.ONWARD;
    }

    /**
     * Why: floors are counted {@code chainIndex * perRegion + band}, so every sector of one road must be the same length or two sectors would claim the same
     * floor numbers (a 5-floor sector after a 10-floor one would reuse +6..+10, and the lock, the quests and the boss rule all assume one floor means one
     * place). This is the one rule that decides the length of a sector that is about to be numbered, kept pure so it can be pinned. In order: the length of
     * another numbered sector of the same road wins ({@code otherNumbered}, 0 when there is none: the chain stays consistent); else the length the sector
     * already carries wins when it is not the constructor's default ({@code stored}: a sector that was numbered before and then regenerated keeps its length
     * even though its rooms are new and unnumbered, so a config change never renumbers it); else the config's value ({@code configured}). Never below 1.
     */
    public static int lengthFor(int stored, int otherNumbered, int configured) {
        if (otherNumbered > 0) return otherNumbered;
        if (stored > 0 && stored != FloorMap.DEFAULT_PER_REGION) return stored;
        return Math.max(1, configured);
    }

    /**
     * The number of doorways from the Landing to every room (breadth-first over the undirected doorway graph), keyed by room id in the order of
     * {@code rooms}; -1 for a room that cannot be reached, and -1 for every room when {@code landingId} is not one of the rooms. An edge with an unknown
     * end, an edge from a room to itself and a repeated edge are ignored.
     */
    public static Map<Integer, Integer> depths(List<Node> rooms, int landingId, List<int[]> edges) {
        Map<Integer, Integer> depth = new LinkedHashMap<>();
        if (rooms == null) return depth;
        for (Node n : rooms) depth.put(n.id(), -1);
        Map<Integer, List<Integer>> adjacent = new HashMap<>();
        if (edges != null) {
            for (int[] e : edges) {
                if (e == null || e.length < 2) continue;
                int a = e[0], b = e[1];
                if (a == b || !depth.containsKey(a) || !depth.containsKey(b)) continue;
                adjacent.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
                adjacent.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
            }
        }
        if (!depth.containsKey(landingId)) return depth;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        depth.put(landingId, 0);
        queue.add(landingId);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            int next = depth.get(current) + 1;
            List<Integer> around = adjacent.get(current);
            if (around == null) continue;
            for (int other : around) {
                if (depth.get(other) < 0) {
                    depth.put(other, next);
                    queue.add(other);
                }
            }
        }
        return depth;
    }

    /** {@link #bands(List, int, int, List, int, Strategy)} with the brief's rule ({@link Strategy#ARENA}). */
    public static Map<Integer, Integer> bands(List<Node> rooms, int landingId, int arenaId, List<int[]> edges, int perRegion) {
        return bands(rooms, landingId, arenaId, edges, perRegion, Strategy.ARENA);
    }

    /**
     * The band (1..{@code perRegion}) of every room, keyed by room id in the order of {@code rooms}. {@code arenaId} is the Guardian's arena or
     * {@link #NONE}; {@code landingId} the root of the search. Never throws: bad input degrades to the last band, and an empty input to an empty map.
     */
    public static Map<Integer, Integer> bands(List<Node> rooms, int landingId, int arenaId, List<int[]> edges, int perRegion, Strategy strategy) {
        int per = Math.max(1, perRegion);
        Map<Integer, Integer> depth = depths(rooms, landingId, edges);
        Map<Integer, Integer> band = strategy == Strategy.EVEN ? evenBands(rooms, landingId, arenaId, depth, per) : arenaBands(rooms, landingId, arenaId, depth, per);
        Map<Integer, Integer> out = new LinkedHashMap<>();
        if (rooms == null) return out;
        for (Node n : rooms) out.put(n.id(), band.get(n.id()));                       // in the order the caller listed the rooms
        return out;
    }

    /**
     * Why: the assigner's last step (a band becomes a SIGNED floor of a chain: the road's sign, the sector's place in the chain) is arithmetic that decides
     * whether the Abyssal road counts downwards and whether the second sector of a chain starts at +11, so it lives here, pure and tested, and the assigner only
     * copies the answer into the rooms. {@code landingId} may be {@link #NONE} (a sector whose Landing cannot be found is walked from its lowest room id, so a
     * damaged sector still gets floors); a null polarity reads as HOLY. The result is {@code FloorMap.floorOf(polarity, chainIndex, band, perRegion)} for every
     * room, keyed by room id in the order of {@code rooms}.
     */
    public static Map<Integer, Integer> floors(List<Node> rooms, int landingId, int arenaId, List<int[]> edges, Polarity polarity, int chainIndex, int perRegion, Strategy strategy) {
        int per = Math.max(1, perRegion);
        Polarity road = polarity == null ? Polarity.HOLY : polarity;
        int root = landingId;
        if (root == NONE && rooms != null && !rooms.isEmpty()) {
            root = rooms.get(0).id();
            for (Node n : rooms) root = Math.min(root, n.id());
        }
        Map<Integer, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : bands(rooms, root, arenaId, edges, per, strategy).entrySet()) out.put(e.getKey(), FloorMap.floorOf(road, chainIndex, e.getValue(), per));
        return out;
    }

    /** The rule of the brief: depth over the arena's depth. */
    private static Map<Integer, Integer> arenaBands(List<Node> rooms, int landingId, int arenaId, Map<Integer, Integer> depth, int per) {
        int arenaDepth = arenaId == NONE ? -1 : depth.getOrDefault(arenaId, -1);
        if (arenaDepth <= 0) {
            arenaDepth = 0;
            for (int d : depth.values()) arenaDepth = Math.max(arenaDepth, d);
        }
        Map<Integer, Integer> out = new HashMap<>();
        if (rooms == null) return out;
        for (Node n : rooms) {
            int band;
            if (n.id() == landingId) band = 1;
            else if (isBeyondArena(n.kind())) band = per;
            else if (n.kind() == RoomKind.LINK) band = 1;
            else band = FloorMap.bandOfDepth(depth.get(n.id()), arenaDepth, per);
            out.put(n.id(), band);
        }
        return out;
    }

    /** Plain rooms cut by rank into perRegion-1 runs of equal size; the special rooms as in the common rules. */
    private static Map<Integer, Integer> evenBands(List<Node> rooms, int landingId, int arenaId, Map<Integer, Integer> depth, int per) {
        Map<Integer, Integer> out = new HashMap<>();
        if (rooms == null) return out;
        List<Node> plain = new ArrayList<>();
        for (Node n : rooms) {
            int id = n.id();
            if (id == landingId) out.put(id, 1);
            else if (id == arenaId || isBeyondArena(n.kind())) out.put(id, per);
            else if (n.kind() == RoomKind.LINK) out.put(id, 1);
            else if (depth.get(id) < 0) out.put(id, per);
            else plain.add(n);
        }
        plain.sort(Comparator.<Node>comparingInt(n -> depth.get(n.id())).thenComparingInt(Node::id));
        int open = per - 1;
        long total = plain.size();
        for (int i = 0; i < plain.size(); i++) {
            // the middle of rank i on a scale of 'total', mapped onto bands 1..open: runs of total/open rooms, each at most one room larger than another
            int band = open <= 0 ? per : Math.min(open, 1 + (int) ((2L * i + 1) * open / (2L * total)));
            out.put(plain.get(i).id(), band);
        }
        return out;
    }
}
