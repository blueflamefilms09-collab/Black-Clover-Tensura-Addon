package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the shape of one exclusive pocket dungeon as a graph on a grid, with no Minecraft type in it, so the generator that makes it ({@link LayoutGen}) and the checks that prove it
 * ({@link LayoutCheck}) are unit-tested over thousands of seeds. A layout is rooms on a square grid (side 5 or 7, cells 24 blocks apart, so a tomb 168 blocks across stands behind a tower
 * five blocks wide), and doors between neighbouring rooms; exactly one door is SEALED: the one between the mega-boss's lair and the treasury. Everything else a delver walks is open, so
 * there is no way to lock oneself in or out: the entry reaches every room but the treasury without crossing the seal, the key room (the Sigil) and the boss are among them, and the seal is
 * the only gate. Coordinates are LOCAL: the centre of the pocket's cell is (0, 0), x east, z south, and a room's footprint is {@code cx - half .. cx + half} (its wall one block outside that).
 */
public final class PocketLayout {
    /** Distance between the centres of two neighbouring cells, in blocks. */
    public static final int PITCH = 24;
    /** Width of an ordinary doorway and corridor, and the sealed door's. */
    public static final int DOOR_WIDTH = 3, SEAL_WIDTH = 5;
    /** Height of an ordinary doorway, and of the sealed door. */
    public static final int DOOR_HEIGHT = 4, SEAL_HEIGHT = 6;

    /** What a room is. The order is the order of the saved numbers; values are only ever appended. */
    public enum Kind {
        ENTRY(5, 7), COMBAT(7, 8), TRAP(6, 7), INVERTED(7, 14), NEGATED(7, 16), RIVER(8, 10), RUIN(6, 9), KEY(5, 8), BOSS(10, 16), TREASURY(6, 10), ALCOVE(3, 6);

        private final int half, height;

        Kind(int half, int height) {
            this.half = half;
            this.height = height;
        }

        /** Half the interior width (the interior is {@code 2 * half + 1} blocks across); a boss lair's is set by the definition. */
        public int half() { return half; }

        /** Interior height (blocks of air above the floor). */
        public int height() { return height; }

        /** A room with a gravity rule of its own. */
        public boolean gravity() { return this == INVERTED || this == NEGATED; }

        /** A room the player must be able to enter freely and see clearly: nothing odd may stand next to its door. */
        public boolean plain() { return this == ENTRY || this == BOSS || this == KEY || this == TREASURY; }
    }

    /** One room: its place on the grid, its kind, the half width of its interior, its interior height and, for a trap room, the element of its traps. */
    public record Room(int id, int gx, int gz, Kind kind, int half, int height, String element) {
        /** Local x of the centre of the room, for a grid of this side. */
        public int cx(int grid) { return (gx - grid / 2) * PITCH; }

        /** Local z of the centre of the room. */
        public int cz(int grid) { return (gz - grid / 2) * PITCH; }
    }

    /** A door between two neighbouring rooms ({@code a < b}); {@code sealed} is the treasury's. */
    public record Door(int a, int b, boolean sealed) {}

    private final long seed;
    private final int grid;
    private final List<Room> rooms;
    private final List<Door> doors;
    private final int entry, boss, treasury, key;

    PocketLayout(long seed, int grid, List<Room> rooms, List<Door> doors, int entry, int boss, int treasury, int key) {
        this.seed = seed;
        this.grid = grid;
        this.rooms = List.copyOf(rooms);
        this.doors = List.copyOf(doors);
        this.entry = entry;
        this.boss = boss;
        this.treasury = treasury;
        this.key = key;
    }

    public long seed() { return seed; }

    public int grid() { return grid; }

    public List<Room> rooms() { return rooms; }

    public List<Door> doors() { return doors; }

    public Room room(int id) { return rooms.get(id); }

    public Room entry() { return rooms.get(entry); }

    public Room boss() { return rooms.get(boss); }

    public Room treasury() { return rooms.get(treasury); }

    public Room key() { return rooms.get(key); }

    public int entryId() { return entry; }

    public int bossId() { return boss; }

    public int treasuryId() { return treasury; }

    public int keyId() { return key; }

    /** The ids of the rooms with a door to {@code id}; the sealed door counts only when {@code includeSealed}. */
    public List<Integer> neighbours(int id, boolean includeSealed) {
        List<Integer> out = new ArrayList<>();
        for (Door d : doors) {
            if (d.sealed() && !includeSealed) continue;
            if (d.a() == id) out.add(d.b());
            else if (d.b() == id) out.add(d.a());
        }
        return out;
    }

    public int degree(int id, boolean includeSealed) { return neighbours(id, includeSealed).size(); }

    /** The door between two rooms, or null. */
    public Door doorBetween(int a, int b) {
        for (Door d : doors) if ((d.a() == a && d.b() == b) || (d.a() == b && d.b() == a)) return d;
        return null;
    }

    /** The id of the room whose footprint, wall included, holds the local point; -1 in a corridor or outside. */
    public int roomAt(double lx, double lz) {
        for (Room r : rooms) {
            int cx = r.cx(grid), cz = r.cz(grid), w = r.half() + 1;
            if (lx >= cx - w && lx < cx + w + 1 && lz >= cz - w && lz < cz + w + 1) return r.id();
        }
        return -1;
    }

    /** Length of the corridor between two door-connected rooms (the blocks of air between their walls); 0 when they are not neighbours on the grid. */
    public int corridorLength(Room a, Room b) {
        if (Math.abs(a.gx() - b.gx()) + Math.abs(a.gz() - b.gz()) != 1) return 0;
        return PITCH - a.half() - b.half() - 3;
    }

    /** Is a corridor between these two rooms a tall one (an inverted room's gravity zone reaches into it)? */
    public static boolean tall(Room a, Room b) { return a.kind() == Kind.INVERTED || b.kind() == Kind.INVERTED; }

    /** The number of rooms by kind. */
    public int count(Kind kind) {
        int n = 0;
        for (Room r : rooms) if (r.kind() == kind) n++;
        return n;
    }
}
