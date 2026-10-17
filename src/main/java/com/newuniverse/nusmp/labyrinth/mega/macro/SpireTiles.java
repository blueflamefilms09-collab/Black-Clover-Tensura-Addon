package com.newuniverse.nusmp.labyrinth.mega.macro;

import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.AIR;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.CHISELED_QUARTZ;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.CHAIN_Y;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.GLASS;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.GOLD_TRIM;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.LANTERN;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.LANTERN_HANGING;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.NATIVE;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.QUARTZ_PILLAR;
import static com.newuniverse.nusmp.labyrinth.mega.macro.BlockKind.SMOOTH_QUARTZ;

/**
 * Why: these are the TRIAL CHAMBERS of the Holy Spire, the "assemble trial chambers upwards" half of the brief's Wave Function Collapse. A gallery is a ring of
 * cells round the shaft (a collar of masonry that bulges off the tower's wall) two cells high; the solver decides, cell by cell, whether the collar there is a
 * solid pier, a corridor room with doorways to its neighbours, a room with a pillared floor, an altar nook, a sealed vault seen through glass, or the two
 * halves of a double-height hall. The tileset is small on purpose (11 tiles): each tile is a distinct, readable piece of architecture, and the sockets
 * (below) are the whole grammar, so the table in the docs is the complete specification of what a gallery can look like.
 *
 * <p>Sockets (a socket matches itself; the table in the docs says what each means). Tangential faces (to the next room round the ring): {@link #T_WALL} a
 * closed wall, {@link #T_DOOR} a 2 x 3 doorway. Vertical faces: {@link #V_SLAB} a solid floor, {@link #V_OPEN} a 4 x 4 opening (a double-height hall).
 * Faces towards the shaft: {@link #I_WALL}, {@link #I_WIN} an arched window, {@link #I_DOOR} the 4 x 4 doorway onto a staircase landing. Faces towards the
 * outside: {@link #O_WALL}, {@link #O_WIN}, {@link #O_DOOR} (the way in at the foot of the tower). The two door sockets towards the shaft and the outside are
 * kept out of the edge masks of every cell the caller did not pre-collapse, so a doorway can only ever appear where there is a landing (or the front door)
 * behind it, never onto a sheer drop.
 *
 * <p>Pure: no Minecraft import. The patterns are authored at 6 x 6 x 6 in the {@link TilePattern} axes (a along the ring, b up, c outwards from the shaft)
 * and use {@link BlockKind#NATIVE} for "the masonry of the tower", which the model resolves to a mix of quartz bricks, smooth quartz and calcite.
 */
public final class SpireTiles {
    private SpireTiles() {}

    // socket ids
    public static final int T_WALL = 0, T_DOOR = 1, V_SLAB = 2, V_OPEN = 3, I_WALL = 4, I_DOOR = 5, I_WIN = 6, O_WALL = 7, O_WIN = 8, O_DOOR = 9;

    // tile indices
    public static final int PIER = 0, HALL_DOOR = 1, PASSAGE = 2, ROOM_THROUGH = 3, ROOM_PILLARS = 4, ROOM_END_R = 5, ROOM_END_L = 6, ROOM_ALTAR = 7,
            HALL_LOW = 8, HALL_HIGH = 9, ROOM_SEALED = 10;

    /** The tiles that exist only on cells the caller pre-collapsed. */
    public static final long FIXED_ONLY = (1L << HALL_DOOR) | (1L << PASSAGE);

    public static final Wfc3D.Tileset TILESET;
    public static final TilePattern[] PATTERNS;

    static {
        // (name, weight, +a, -a, +b, -b, +c, -c)
        Wfc3D.Tile[] tiles = {
                new Wfc3D.Tile("pier", 4.0, T_WALL, T_WALL, V_SLAB, V_SLAB, O_WALL, I_WALL),
                new Wfc3D.Tile("hall_door", 1.0, T_DOOR, T_DOOR, V_SLAB, V_SLAB, O_WALL, I_DOOR),
                new Wfc3D.Tile("passage", 1.0, T_WALL, T_WALL, V_SLAB, V_SLAB, O_DOOR, I_DOOR),
                new Wfc3D.Tile("room_through", 3.0, T_DOOR, T_DOOR, V_SLAB, V_SLAB, O_WIN, I_WIN),
                new Wfc3D.Tile("room_pillars", 2.0, T_DOOR, T_DOOR, V_SLAB, V_SLAB, O_WALL, I_WIN),
                new Wfc3D.Tile("room_end_r", 2.0, T_DOOR, T_WALL, V_SLAB, V_SLAB, O_WIN, I_WALL),
                new Wfc3D.Tile("room_end_l", 2.0, T_WALL, T_DOOR, V_SLAB, V_SLAB, O_WIN, I_WALL),
                new Wfc3D.Tile("room_altar", 1.0, T_DOOR, T_WALL, V_SLAB, V_SLAB, O_WIN, I_WALL),
                new Wfc3D.Tile("hall_low", 1.5, T_WALL, T_WALL, V_OPEN, V_SLAB, O_WIN, I_WIN),
                new Wfc3D.Tile("hall_high", 1.5, T_WALL, T_WALL, V_SLAB, V_OPEN, O_WIN, I_WIN),
                new Wfc3D.Tile("room_sealed", 1.0, T_WALL, T_WALL, V_SLAB, V_SLAB, O_WIN, I_WIN),
        };
        TILESET = new Wfc3D.Tileset(tiles, null);
        PATTERNS = new TilePattern[tiles.length];
        PATTERNS[PIER] = pier();
        PATTERNS[HALL_DOOR] = hallDoor();
        PATTERNS[PASSAGE] = passage();
        PATTERNS[ROOM_THROUGH] = roomThrough();
        PATTERNS[ROOM_PILLARS] = roomPillars();
        PATTERNS[ROOM_END_R] = roomEnd(true);
        PATTERNS[ROOM_END_L] = roomEnd(false);
        PATTERNS[ROOM_ALTAR] = roomAltar();
        PATTERNS[HALL_LOW] = hallLow();
        PATTERNS[HALL_HIGH] = hallHigh();
        PATTERNS[ROOM_SEALED] = roomSealed();
    }

    // ---- art helpers

    /** An empty room: masonry all round, a 4 x 4 x 4 hollow, a smooth quartz floor, quartz pillars on the four vertical edges of the cell. */
    private static TilePattern shell() {
        TilePattern p = TilePattern.filled(NATIVE);
        p.fill(1, 4, 1, 4, 1, 4, AIR);
        p.fill(1, 4, 0, 0, 1, 4, SMOOTH_QUARTZ);
        for (int a : new int[] {0, 5}) for (int c : new int[] {0, 5}) p.fill(a, a, 1, 4, c, c, QUARTZ_PILLAR);
        return p;
    }

    /** A 2 x 3 doorway through the side layer {@code a}. */
    private static void sideDoor(TilePattern p, int a) {
        p.fill(a, a, 1, 3, 2, 3, AIR);
    }

    /** The 4 x 4 doorway (arched: the top row is only 2 wide) through the layer {@code c} (0 = onto the shaft, 5 = outside). */
    private static void bigDoor(TilePattern p, int c) {
        p.fill(1, 4, 1, 3, c, c, AIR);
        p.fill(2, 3, 4, 4, c, c, AIR);
    }

    /** An arched window of glass in the layer {@code c}. */
    private static void window(TilePattern p, int c) {
        p.fill(2, 3, 2, 4, c, c, GLASS);
    }

    /** A chain with a hanging lantern at (a, c), hung from the ceiling of a room. */
    private static void chandelier(TilePattern p, int a, int c) {
        p.set(a, 4, c, CHAIN_Y);
        p.set(a, 3, c, LANTERN_HANGING);
    }

    private static TilePattern pier() {
        TilePattern p = TilePattern.filled(NATIVE);
        for (int b = 1; b <= 4; b++) p.set(2, b, 5, SMOOTH_QUARTZ).set(3, b, 5, SMOOTH_QUARTZ);
        return p;
    }

    private static TilePattern hallDoor() {
        TilePattern p = shell();
        sideDoor(p, 0);
        sideDoor(p, 5);
        bigDoor(p, 0);
        p.fill(1, 4, 0, 0, 0, 0, GOLD_TRIM);
        chandelier(p, 2, 3);
        chandelier(p, 3, 2);
        return p;
    }

    private static TilePattern passage() {
        TilePattern p = shell();
        bigDoor(p, 0);
        bigDoor(p, 5);
        p.fill(1, 4, 0, 0, 0, 0, GOLD_TRIM);
        p.fill(1, 4, 0, 0, 5, 5, GOLD_TRIM);
        p.fill(1, 4, 5, 5, 5, 5, GOLD_TRIM);
        chandelier(p, 2, 2);
        chandelier(p, 3, 3);
        return p;
    }

    private static TilePattern roomThrough() {
        TilePattern p = shell();
        sideDoor(p, 0);
        sideDoor(p, 5);
        window(p, 0);
        window(p, 5);
        chandelier(p, 2, 3);
        return p;
    }

    private static TilePattern roomPillars() {
        TilePattern p = shell();
        sideDoor(p, 0);
        sideDoor(p, 5);
        window(p, 0);
        for (int a : new int[] {1, 4}) for (int c : new int[] {1, 4}) p.fill(a, a, 1, 4, c, c, QUARTZ_PILLAR);
        p.fill(2, 3, 0, 0, 2, 3, CHISELED_QUARTZ);
        return p;
    }

    private static TilePattern roomEnd(boolean doorOnPlusA) {
        TilePattern p = shell();
        sideDoor(p, doorOnPlusA ? 5 : 0);
        window(p, 0);
        window(p, 5);
        chandelier(p, doorOnPlusA ? 3 : 2, 3);
        return p;
    }

    private static TilePattern roomAltar() {
        TilePattern p = shell();
        sideDoor(p, 5);
        window(p, 0);
        window(p, 5);
        p.fill(1, 2, 1, 1, 2, 3, CHISELED_QUARTZ);
        p.set(1, 2, 2, LANTERN).set(2, 2, 3, LANTERN);
        return p;
    }

    /** The lower half of a double-height hall: no ceiling over the 4 x 4 hollow, tall windows towards the shaft and the outside. */
    private static TilePattern hallLow() {
        TilePattern p = shell();
        p.fill(1, 4, 5, 5, 1, 4, AIR);
        window(p, 0);
        window(p, 5);
        return p;
    }

    /** The upper half of a double-height hall: no floor under the hollow, windows, and a long chain with a lantern at its end. */
    private static TilePattern hallHigh() {
        TilePattern p = shell();
        p.fill(1, 4, 0, 0, 1, 4, AIR);
        window(p, 0);
        window(p, 5);
        p.set(2, 4, 2, CHAIN_Y).set(2, 3, 2, CHAIN_Y).set(2, 2, 2, CHAIN_Y).set(2, 1, 2, LANTERN_HANGING);
        return p;
    }

    private static TilePattern roomSealed() {
        TilePattern p = shell();
        window(p, 0);
        window(p, 5);
        chandelier(p, 2, 3);
        p.fill(2, 3, 1, 1, 2, 3, CHISELED_QUARTZ);
        return p;
    }

    // ---- the problem of one gallery

    /**
     * The Wave Function Collapse problem of one gallery: a ring of {@code sectors} sectors, two cells high, one cell deep. Cell (0, 0, 0) is the
     * pre-collapsed cell behind the landing: the front door ({@link #PASSAGE}) on the ground gallery, the landing's doorway ({@link #HALL_DOOR}) on the
     * others. The edge masks say what the open faces may show: floors and roofs solid, windows or walls towards the shaft and the outside.
     */
    public static Wfc3D.Problem problem(int sectors, boolean ground) {
        Wfc3D.Problem p = new Wfc3D.Problem(sectors, 2, 1, true);
        long freeTiles = ~FIXED_ONLY & ((1L << TILESET.size()) - 1);
        for (int x = 0; x < sectors; x++) for (int y = 0; y < 2; y++) {
            p.setFree(x, y, 0);
            p.allowOnly(x, y, 0, freeTiles);
        }
        p.setFixed(0, 0, 0, ground ? PASSAGE : HALL_DOOR);
        p.restrictEdge(Wfc3D.NY, V_SLAB);
        p.restrictEdge(Wfc3D.PY, V_SLAB);
        p.restrictEdge(Wfc3D.NZ, I_WALL, I_WIN, I_DOOR);
        if (ground) p.restrictEdge(Wfc3D.PZ, O_WALL, O_WIN, O_DOOR);
        else p.restrictEdge(Wfc3D.PZ, O_WALL, O_WIN);
        return p;
    }
}
