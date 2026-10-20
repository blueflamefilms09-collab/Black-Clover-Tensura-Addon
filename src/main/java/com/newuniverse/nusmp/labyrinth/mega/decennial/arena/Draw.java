package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Element;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;

/**
 * Why: thirty-six halls are drawn from the same dozen gestures (fill a box, lay a disc or a ring, raise a column, a pillar with a base and a capital, a line, a stair, a dome), and writing each
 * gesture once keeps every hall short enough to read and every gesture right in one place (a stair always names its facing and half, a slab its type). All the functions are pure: they only
 * call {@link BossArenaStructure#set}, which refuses a cell the floor's template owns and counts a write outside the footprint (a test asserts there are none). Block names are written
 * without the namespace here ({@link #b}) so a hall's palette reads as the materials it is made of.
 */
public final class Draw {
    private Draw() {}

    // ================================================================ names
    public static String b(String name) { return "minecraft:" + name; }

    /** A block with properties: {@code st("lantern", "hanging=false")}. */
    public static String st(String name, String props) { return "minecraft:" + name + "[" + props + "]"; }

    public static String stair(String family, String facing, String half) { return "minecraft:" + family + "[facing=" + facing + ",half=" + half + "]"; }

    public static String slab(String family, String type) { return "minecraft:" + family + "[type=" + type + "]"; }

    public static String axis(String block, String axis) { return "minecraft:" + block + "[axis=" + axis + "]"; }

    public static String light(int level) { return "minecraft:light[level=" + Math.max(1, Math.min(15, level)) + "]"; }

    // ================================================================ solids
    /** Fills the box x0..x1, y0..y1, z0..z1 (inclusive, in any order). */
    public static void fill(BossArenaStructure a, int x0, int y0, int z0, int x1, int y1, int z1, String spec) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) a.set(x, y, z, spec);
    }

    public static void clear(BossArenaStructure a, int x0, int y0, int z0, int x1, int y1, int z1) { fill(a, x0, y0, z0, x1, y1, z1, BossArenaStructure.AIR); }

    /** A vertical column of one block from y0 to y1. */
    public static void column(BossArenaStructure a, int x, int z, int y0, int y1, String spec) { fill(a, x, y0, z, x, y1, z, spec); }

    /** A horizontal disc of radius r about (cx, cz) at height y. */
    public static void disc(BossArenaStructure a, double cx, double cz, double r, int y, String spec) {
        int R = (int) Math.ceil(r);
        for (int z = Math.max(0, (int) Math.floor(cz) - R); z <= Math.min(a.size - 1, (int) Math.ceil(cz) + R); z++)
            for (int x = Math.max(0, (int) Math.floor(cx) - R); x <= Math.min(a.size - 1, (int) Math.ceil(cx) + R); x++)
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= r * r) a.set(x, y, z, spec);
    }

    /** A flat ring between radius r0 (exclusive) and r1 (inclusive) at height y. */
    public static void ring(BossArenaStructure a, double cx, double cz, double r0, double r1, int y, String spec) {
        int R = (int) Math.ceil(r1);
        for (int z = Math.max(0, (int) Math.floor(cz) - R); z <= Math.min(a.size - 1, (int) Math.ceil(cz) + R); z++)
            for (int x = Math.max(0, (int) Math.floor(cx) - R); x <= Math.min(a.size - 1, (int) Math.ceil(cx) + R); x++) {
                double d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
                if (d2 > r0 * r0 && d2 <= r1 * r1) a.set(x, y, z, spec);
            }
    }

    /** A solid cylinder of radius r from y0 to y1. */
    public static void cyl(BossArenaStructure a, double cx, double cz, double r, int y0, int y1, String spec) {
        for (int y = y0; y <= y1; y++) disc(a, cx, cz, r, y, spec);
    }

    /** A hollow wall of a cylinder (between r-t and r) from y0 to y1. */
    public static void tube(BossArenaStructure a, double cx, double cz, double r, double t, int y0, int y1, String spec) {
        for (int y = y0; y <= y1; y++) ring(a, cx, cz, r - t, r, y, spec);
    }

    /** A straight line of blocks between two cells (a 3D digital line). */
    public static void line(BossArenaStructure a, int x0, int y0, int z0, int x1, int y1, int z1, String spec) {
        int n = Math.max(Math.abs(x1 - x0), Math.max(Math.abs(y1 - y0), Math.abs(z1 - z0)));
        if (n == 0) {
            a.set(x0, y0, z0, spec);
            return;
        }
        for (int i = 0; i <= n; i++) a.set(x0 + Math.round((x1 - x0) * (float) i / n), y0 + Math.round((y1 - y0) * (float) i / n), z0 + Math.round((z1 - z0) * (float) i / n), spec);
    }

    /** A box shell (walls, floor and roof of the box x0..x1, y0..y1, z0..z1) of one block, hollow inside (the inside is left alone). */
    public static void shell(BossArenaStructure a, int x0, int y0, int z0, int x1, int y1, int z1, String spec) {
        for (int y = y0; y <= y1; y++)
            for (int z = z0; z <= z1; z++)
                for (int x = x0; x <= x1; x++)
                    if (x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1) a.set(x, y, z, spec);
    }

    // ================================================================ features
    /** A pillar: a base block, a shaft of height h and a capital, on (x, z) from y0. {@code thick} makes it 3 x 3 with a rounded plan. */
    public static void pillar(BossArenaStructure a, int x, int z, int y0, int h, String shaft, String base, String cap, boolean thick) {
        if (thick) {
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) == 2) continue;                       // the corners are cut: a round-ish pillar
                    a.set(x + dx, y0, z + dz, base);
                    for (int y = y0 + 1; y < y0 + h - 1; y++) a.set(x + dx, y, z + dz, shaft);
                    a.set(x + dx, y0 + h - 1, z + dz, cap);
                }
        } else {
            a.set(x, y0, z, base);
            for (int y = y0 + 1; y < y0 + h - 1; y++) a.set(x, y, z, shaft);
            a.set(x, y0 + h - 1, z, cap);
        }
    }

    /** A dome shell of radius r about (cx, y0, cz), the upper half only, one block thick. */
    public static void dome(BossArenaStructure a, int cx, int y0, int cz, double r, String spec) {
        int R = (int) Math.ceil(r);
        for (int y = 0; y <= R; y++)
            for (int z = -R; z <= R; z++)
                for (int x = -R; x <= R; x++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d <= r && d > r - 1.2) a.set(cx + x, y0 + y, cz + z, spec);
                }
    }

    /** A stair block facing a horizontal direction name ("north", ...), bottom half by default. */
    public static void stairAt(BossArenaStructure a, int x, int y, int z, String family, String facing, boolean top) { a.set(x, y, z, stair(family, facing, top ? "top" : "bottom")); }

    /** The opposite horizontal direction name. */
    public static String opposite(String dir) {
        return switch (dir) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            default -> "east";
        };
    }

    /** The direction name towards the centre from a cell; a stair with this facing climbs towards the middle (a stair's facing is the way it ascends, towards its tall back). */
    public static String towardsCentre(BossArenaStructure a, int x, int z) {
        int dx = a.c - x, dz = a.c - z;
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? "east" : "west";
        return dz > 0 ? "south" : "north";
    }

    // ================================================================ geometry questions
    public static double dist(BossArenaStructure a, int x, int z) { return Math.hypot(x - a.c, z - a.c); }

    /** Is the column inside the processional lane (the strip of three columns on the hall's axis) or the boss clearing (radius 8 about the altar)? Features keep these free. */
    public static boolean keepClear(BossArenaStructure a, int x, int z) { return Math.abs(x - a.c) <= 2 || dist(a, x, z) <= 8.5; }

    /** Is the column clear of the great gate of the Underworld Root halls (its leaf fills the north wall, nine wide, with a clear apron in front)? */
    public static boolean awayFromGate(BossArenaStructure a, int x, int z) { return !(z < 9 && Math.abs(x - a.c) <= 7); }

    /** Is the column within {@code m} blocks of the inside of a wall? */
    public static boolean nearWall(BossArenaStructure a, int x, int z, int m) { return x < 2 + m || z < 2 + m || x > a.size - 3 - m || z > a.size - 3 - m; }

    // ================================================================ interactive elements
    /**
     * A tombstone: a two-block grave marker on the ground that, once broken, lets {@code adds} of the hall's dead out of the ground. Leaning (a stair) or upright (a wall piece on a slab base).
     */
    public static Element tombstone(BossArenaStructure a, int x, int z, int adds, String stone, String top) {
        Element e = a.beginElement(Kind.TOMBSTONE, Effect.ADDS, adds);
        a.set(x, 1, z, stone);
        a.set(x, 2, z, top);
        a.endElement();
        a.spot(x, z);
        return e;
    }
}
