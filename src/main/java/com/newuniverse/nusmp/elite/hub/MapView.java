package com.newuniverse.nusmp.elite.hub;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Why: the Dungeon Map is drawn on the client from facts only the server knows (the rooms of the floor and which doors join them), so the server flattens the generator's {@code Sector} (rooms with
 * a template, a kind and a bounding box; edges between room ids) into this compact value, which is what travels in the packet. Building it is pure, so a test can prove the rules without a
 * game: only the rooms of the CURRENT floor (a sector of the floor model is one floor; a sector of the old ten-floor banding is cut by each room's {@code floor}), only the edges whose two ends
 * were kept, a safety cap on the room count (the first rooms by id win, so the Landing, which has the lowest ids, is never the one dropped), and a visited flag per room taken from the player's
 * memory so the screen can dim what they have not seen. Coordinates are absolute block x and z. {@code kind} is a {@code RoomKind} ordinal (the enum only ever grows at its end).
 */
public record MapView(int floor, String road, List<Room> rooms, List<int[]> edges, int arrivalX, int arrivalZ, int exitX, int exitZ, boolean hasExit) {
    /** One room as the screen needs it. */
    public record Room(int id, int kind, int minX, int minZ, int maxX, int maxZ, boolean visited) {
        public int centerX() { return (minX + maxX) / 2; }

        public int centerZ() { return (minZ + maxZ) / 2; }
    }

    /** The generator's room, as plain numbers (the game side copies a {@code Room} into this). */
    public record Source(int id, int kind, int floor, int minX, int minZ, int maxX, int maxZ) {}

    /** The most rooms the wire format ever carries. */
    public static final int HARD_CAP = 2000;

    /**
     * @param floor    the signed floor to draw
     * @param firstFloor the signed first floor of the sector: a room whose own floor is 0 (a sector saved before the floors were banded) belongs to it
     * @param maxRooms the most rooms to keep (also clamped to {@link #HARD_CAP})
     * @param visited  says whether the player has stood in the room with this id
     */
    public static MapView build(List<Source> sources, List<int[]> allEdges, int floor, int firstFloor, String road, int maxRooms, IntPredicate visited,
                                int arrivalX, int arrivalZ, boolean hasExit, int exitX, int exitZ) {
        int cap = Math.max(1, Math.min(HARD_CAP, maxRooms));
        List<Source> kept = new ArrayList<>();
        for (Source s : sources) {
            int rf = s.floor() == 0 ? firstFloor : s.floor();
            if (rf == floor) kept.add(s);
        }
        kept.sort((a, b) -> Integer.compare(a.id(), b.id()));
        if (kept.size() > cap) kept = new ArrayList<>(kept.subList(0, cap));
        Set<Integer> ids = new HashSet<>();
        List<Room> rooms = new ArrayList<>();
        for (Source s : kept) {
            ids.add(s.id());
            rooms.add(new Room(s.id(), s.kind(), Math.min(s.minX(), s.maxX()), Math.min(s.minZ(), s.maxZ()), Math.max(s.minX(), s.maxX()), Math.max(s.minZ(), s.maxZ()), visited.test(s.id())));
        }
        List<int[]> edges = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int[] e : allEdges) {
            if (e == null || e.length < 2 || !ids.contains(e[0]) || !ids.contains(e[1]) || e[0] == e[1]) continue;
            long key = ((long) Math.min(e[0], e[1]) << 32) | Math.max(e[0], e[1]);
            if (seen.add(key)) edges.add(new int[] {e[0], e[1]});
        }
        return new MapView(floor, road == null ? "" : road, rooms, edges, arrivalX, arrivalZ, exitX, exitZ, hasExit);
    }

    /** The smallest box round every room: {minX, minZ, maxX, maxZ}; {0,0,0,0} for an empty map. */
    public int[] bounds() {
        if (rooms.isEmpty()) return new int[] {0, 0, 0, 0};
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (Room r : rooms) {
            x0 = Math.min(x0, r.minX());
            z0 = Math.min(z0, r.minZ());
            x1 = Math.max(x1, r.maxX());
            z1 = Math.max(z1, r.maxZ());
        }
        return new int[] {x0, z0, x1, z1};
    }

    public int visitedCount() {
        int n = 0;
        for (Room r : rooms) if (r.visited()) n++;
        return n;
    }

    /** The room that holds a block position (horizontally), or null. */
    public Room roomAt(int x, int z) {
        for (Room r : rooms) if (x >= r.minX() && x <= r.maxX() && z >= r.minZ() && z <= r.maxZ()) return r;
        return null;
    }
}
