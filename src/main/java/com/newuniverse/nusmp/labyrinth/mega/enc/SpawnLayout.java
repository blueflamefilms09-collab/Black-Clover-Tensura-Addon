package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Dungeon Master's search for a place to put monsters used to read the saved {@code Sector} directly, a class made of Minecraft types, so none of the
 * ways it can come up empty (a ring of 24 to 48 blocks that holds no hall, a floor made mostly of corridors, a party spread over two rooms) could be tested
 * without a game. This is the same information as plain numbers: the room boxes with their kinds, the spawn markers, the doorways between rooms. The game glue
 * ({@code CellSource.layoutOf}) copies a sector into it, the tests copy a floor planned from the real template catalog into it, and everything that chooses cells
 * ({@link CellFinder}, {@link SpawnSearch}) works on it. Pure: no Minecraft import.
 */
public final class SpawnLayout {
    /** A room's box (wall layers included) and what kind of room it is. */
    public record RoomBox(int id, RoomKind kind, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) { return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ; }

        public boolean containsXZ(int x, int z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }

        public int spanX() { return maxX - minX + 1; }

        public int spanZ() { return maxZ - minZ + 1; }

        public long volume() { return (long) spanX() * (maxY - minY + 1) * spanZ(); }

        /**
         * The feet row of a monster standing on the floor of this room: one above the floor row of its template ({@link MegaGrid#FLOOR_ROW} rows above the bottom of
         * the box, which is {@link MegaGrid#FLOOR_Y} + 1 in the world for every piece of the standard grid), kept inside the box. Measured from the box, not taken as the
         * absolute row 65, so a floor built at another height (a stack of floors, a lane raised for a test) is looked at where its own floor is.
         */
        public int standY() { return Math.max(minY + 1, Math.min(minY + MegaGrid.FLOOR_ROW + 1, maxY - 2)); }

        public boolean safe() { return Sanctuary.is(kind); }
    }

    /** A spawn marker: the standing cell the template designer chose, and how much it is preferred. */
    public record Spot(int x, int y, int z, double weight) {}

    private final List<RoomBox> rooms;
    private final List<Spot> spots;
    private final List<int[]> edges;
    private final Map<Integer, RoomBox> byId = new HashMap<>();
    private final Map<Integer, List<Integer>> adjacent = new HashMap<>();

    public SpawnLayout(List<RoomBox> rooms, List<Spot> spots, List<int[]> edges) {
        this.rooms = List.copyOf(rooms);
        this.spots = List.copyOf(spots);
        this.edges = List.copyOf(edges);
        for (RoomBox r : this.rooms) {
            byId.putIfAbsent(r.id(), r);
            adjacent.putIfAbsent(r.id(), new ArrayList<>());
        }
        for (int[] e : this.edges) {
            if (e.length < 2 || e[0] == e[1] || !byId.containsKey(e[0]) || !byId.containsKey(e[1])) continue;
            if (!adjacent.get(e[0]).contains(e[1])) adjacent.get(e[0]).add(e[1]);
            if (!adjacent.get(e[1]).contains(e[0])) adjacent.get(e[1]).add(e[0]);
        }
    }

    public List<RoomBox> rooms() { return rooms; }

    public List<Spot> spots() { return spots; }

    public List<int[]> edges() { return edges; }

    public RoomBox room(int id) { return byId.get(id); }

    /** The room whose box holds the point, the smallest box winning (the same rule as {@code Sector.roomAt}); null when none does. */
    public RoomBox roomAt(int x, int y, int z) {
        RoomBox best = null;
        long bestVolume = Long.MAX_VALUE;
        for (RoomBox r : rooms) {
            if (!r.contains(x, y, z)) continue;
            long v = r.volume();
            if (v < bestVolume) {
                best = r;
                bestVolume = v;
            }
        }
        return best;
    }

    /** The room that holds the column (x, z) at any height, the smallest box winning: where a player is when only the floor plan is known. */
    public RoomBox roomAtXZ(int x, int z) {
        RoomBox best = null;
        long bestVolume = Long.MAX_VALUE;
        for (RoomBox r : rooms) {
            if (!r.containsXZ(x, z)) continue;
            long v = r.volume();
            if (v < bestVolume) {
                best = r;
                bestVolume = v;
            }
        }
        return best;
    }

    /** The rooms joined to this one by a doorway. */
    public List<Integer> neighbours(int id) {
        List<Integer> a = adjacent.get(id);
        return a == null ? List.of() : a;
    }

    /**
     * How many doorways each room is from the nearest of the given rooms (0 for those rooms themselves); a room that cannot be reached is absent from the map.
     * Safe rooms are walked through like any other: a monster does not stand in one, but the way to a hall often passes beside it.
     */
    public Map<Integer, Integer> hopsFrom(Collection<Integer> from) {
        Map<Integer, Integer> hops = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int id : from) {
            if (byId.containsKey(id) && hops.putIfAbsent(id, 0) == null) queue.add(id);
        }
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            int h = hops.get(cur);
            for (int n : neighbours(cur)) {
                if (hops.putIfAbsent(n, h + 1) == null) queue.add(n);
            }
        }
        return hops;
    }
}
