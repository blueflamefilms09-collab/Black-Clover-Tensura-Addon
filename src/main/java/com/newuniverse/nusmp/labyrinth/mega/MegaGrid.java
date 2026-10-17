package com.newuniverse.nusmp.labyrinth.mega;

/**
 * Why: every template, pool, processor and generator of the Mega-Dungeon (1.54.0, docs/labyrinth/mega.md) must agree on ONE set of
 * numbers, or two pieces that were drawn by different builders will not line up when the assembler glues them. They live here, as plain
 * constants and tiny pure helpers (no Minecraft types, so JUnit can use them), and the Python toolchain ({@code tools/labyrinth/mega_lib.py})
 * repeats exactly these values. The grid is ODD on purpose: a 9-block module has a real centre cell (offset 4), so a 3-wide doorway is
 * symmetric about its jigsaw cell, and it stays on the same cell when a piece is turned by 180 degrees (offset 4 maps to 9k-1-4, which is 4
 * modulo 9); with an even module the two halves of a doorway would be one block out of line whenever the neighbour is rotated. Every piece
 * footprint is a whole number of modules, so pieces glued door-to-door always tile on the same grid, and two rooms that merely touch have
 * doorways that either line up (a free loop in the map) or do not (a plain wall).
 */
public final class MegaGrid {
    private MegaGrid() {}

    /** Edge of one grid module, in blocks. Every footprint (x and z) of every piece is a multiple of this. */
    public static final int MODULE = 9;
    /** Offset of a module's centre cell: sockets sit at {@code module * k + CENTER} along a side. */
    public static final int CENTER = 4;
    /** Template row (y) of the floor layer: the same row in EVERY piece, so a floor is level wherever pieces meet. */
    public static final int FLOOR_ROW = 8;
    /** Template height (y size) of every piece: 8 rows under the floor (pits, crypts, cisterns) and 39 above it (domes, 2-storey halls). */
    public static final int HEIGHT = 48;
    /** World y of the floor layer in the Mega-Dungeon dimension (template row 0 is at {@link #ORIGIN_Y}). */
    public static final int FLOOR_Y = 64;
    public static final int ORIGIN_Y = FLOOR_Y - FLOOR_ROW;
    /** Thickness of an outer wall: a doorway is a hole through both layers, so an unused one is a two-deep niche. */
    public static final int WALL = 2;
    /** The doorway: 3 wide, 4 tall, the jigsaw in its bottom-centre cell of the OUTER wall layer. */
    public static final int DOOR_W = 3, DOOR_H = 4;
    /** Template row of the jigsaw cell of every door: the first row above the floor. */
    public static final int DOOR_ROW = FLOOR_ROW + 1;
    /** Edge of one sector's region, in modules (the assembler's allowed box is this many modules square). */
    public static final int REGION_MODULES = 19;
    /** Extra blocks a door cap may protrude past the region (a cap is at most 8 deep; regions are kept at least {@link #REGION_GAP} apart, which is more than this). */
    public static final int CAP_MARGIN = 8;
    /** Minimum clear distance between the regions of two sectors, so no cap of one can touch a piece of another. */
    public static final int REGION_GAP = 12;

    /** Jigsaw names and pools every builder must use (the connector contract of docs/labyrinth/mega.md). */
    public static final String DOOR = "nusmp:mega_door";
    public static final String P_SIDE = "nusmp:mega/side", P_CAP = "nusmp:mega/cap", P_LANDING = "nusmp:mega/landing",
            P_ARENA = "nusmp:mega/arena", P_ANTECHAMBER = "nusmp:mega/antechamber", P_VAULT = "nusmp:mega/vault",
            P_ONWARD = "nusmp:mega/onward", P_SAFE = "nusmp:mega/safe", P_LINK = "nusmp:mega/link", P_EMPTY = "minecraft:empty";
    /** Names of the chain sockets (arena -> antechamber -> vault / onward); the generator can force their pool. */
    public static final String CHAIN = "nusmp:mega_chain", VAULT = "nusmp:mega_vault", ONWARD = "nusmp:mega_onward";

    /** Footprint in blocks of {@code n} modules. */
    public static int blocks(int modules) { return modules * MODULE; }

    /** Is this footprint edge a whole number of modules? */
    public static boolean onGrid(int edge) { return edge > 0 && edge % MODULE == 0; }

    /** Offsets (along a side of {@code edge} blocks) where a door socket may sit: the centre of every module. */
    public static int[] socketOffsets(int edge) {
        int n = edge / MODULE;
        int[] out = new int[n];
        for (int k = 0; k < n; k++) out[k] = k * MODULE + CENTER;
        return out;
    }

    /** Is {@code offset} a legal socket offset along a side of {@code edge} blocks? */
    public static boolean isSocketOffset(int offset, int edge) {
        return onGrid(edge) && offset >= 0 && offset < edge && offset % MODULE == CENTER;
    }
}
