package com.newuniverse.nusmp.labyrinth.mega.macro;

/**
 * Why: these are the CAVERNS of the Abyssal Chasm, the "sprawling caverns downwards" half of the brief's Wave Function Collapse. Behind the wall of the pit lies
 * a block of rock three cells deep; the solver decides, cell by cell, which of those cells are solid and which are cave, and how the caves connect, so the
 * result is a believable network of chambers and tunnels (not a random scatter) that opens onto the pit through MOUTHS. A cell's shape is just which of its six
 * faces have a round hole in them; two cells fit exactly when the faces that touch are both holes or both solid. The tileset has 19 tiles: solid rock; the 15
 * ways of opening a nonempty subset of the four horizontal faces (round the pit, and into the rock); and three chimney tiles that join levels (a shaft that starts,
 * continues, ends). Why not all 64 subsets of six faces: a small set keeps the table readable and the owner asked for a small tileset; a set that is not
 * complete can contradict, and the solver's backtracking, restarts and documented fallback exist for exactly that (measured rate in the docs).
 *
 * <p>Interior dressing. The shape of a cell is chosen by the solver, but what grows in it (a pillar, dripstone, sculk, moss and glow lichen, a small sealed pool, a
 * ledge) is a deterministic function of the cell's position and the seed ({@link #flavour}): putting the dressing into the tileset too would multiply the tiles
 * by seven and break the "small" requirement, while a position hash is stable, free and never makes two neighbouring caves incompatible, because the dressing
 * never touches a face layer except to replace a solid floor block by another non-walkable block (sculk, moss, water), so the openings always line up. Water is
 * only placed on a closed floor (no hole in the bottom face) inside a ring of rock, so it can never flow. No lava, ever.
 *
 * <p>Pure: no Minecraft import. The axes are those of {@link TilePattern}: {@code a} along the pit's rim, {@code b} up, {@code c} outwards into the rock.
 */
public final class ChasmTiles {
    private ChasmTiles() {}

    public static final int S_ROCK = 0, S_OPEN = 1;
    /** Bits of a horizontal shape: open towards +a, -a, +c (deeper into the rock), -c (towards the pit). */
    public static final int A_PLUS = 1, A_MINUS = 2, C_PLUS = 4, C_MINUS = 8;
    public static final int ROCK = 0, CHIMNEY_LOW = 16, CHIMNEY_MID = 17, CHIMNEY_HIGH = 18;
    /** A tunnel straight through from the pit into the rock: what the landings' caves are pre-collapsed to. */
    public static final int MOUTH = C_PLUS | C_MINUS;

    public static final int FLAVOURS = 7;
    public static final int PLAIN = 0, PILLAR = 1, DRIP = 2, SCULK_PATCH = 3, MOSS_GARDEN = 4, POOL = 5, LEDGE = 6;
    public static final String[] FLAVOUR_NAMES = {"plain", "pillar", "drip", "sculk", "moss", "pool", "ledge"};

    public static final Wfc3D.Tileset TILESET;
    /** PATTERNS[tile][flavour]. */
    public static final TilePattern[][] PATTERNS;

    static {
        Wfc3D.Tile[] tiles = new Wfc3D.Tile[19];
        for (int m = 0; m < 16; m++) {
            int open = Integer.bitCount(m);
            double w = open == 0 ? 14.0 : open == 1 ? 0.9 : open == 2 ? 1.6 : open == 3 ? 1.2 : 0.6;
            tiles[m] = new Wfc3D.Tile(shapeName(m), w, sock(m, A_PLUS), sock(m, A_MINUS), S_ROCK, S_ROCK, sock(m, C_PLUS), sock(m, C_MINUS));
        }
        tiles[CHIMNEY_LOW] = new Wfc3D.Tile("chimney_low", 0.3, S_ROCK, S_ROCK, S_OPEN, S_ROCK, S_ROCK, S_ROCK);
        tiles[CHIMNEY_MID] = new Wfc3D.Tile("chimney_mid", 0.3, S_ROCK, S_ROCK, S_OPEN, S_OPEN, S_ROCK, S_ROCK);
        tiles[CHIMNEY_HIGH] = new Wfc3D.Tile("chimney_high", 0.3, S_ROCK, S_ROCK, S_ROCK, S_OPEN, S_ROCK, S_ROCK);
        TILESET = new Wfc3D.Tileset(tiles, null);
        PATTERNS = new TilePattern[19][FLAVOURS];
        for (int t = 0; t < 19; t++) {
            TilePattern base = baseShape(t);
            for (int f = 0; f < FLAVOURS; f++) PATTERNS[t][f] = t == ROCK ? base : dress(base, t, f);
        }
    }

    private static int sock(int mask, int bit) { return (mask & bit) != 0 ? S_OPEN : S_ROCK; }

    static String shapeName(int m) {
        if (m == 0) return "rock";
        StringBuilder sb = new StringBuilder("cave");
        if ((m & A_PLUS) != 0) sb.append("_a+");
        if ((m & A_MINUS) != 0) sb.append("_a-");
        if ((m & C_PLUS) != 0) sb.append("_deep");
        if ((m & C_MINUS) != 0) sb.append("_mouth");
        return sb.toString();
    }

    // ---- the geometry of a cell

    /** Is (u, v) inside the round hole (radius 1.9 round the centre) that every open face shows? */
    private static boolean hole(int u, int v) {
        double du = u - 2.5, dv = v - 2.5;
        return du * du + dv * dv <= 3.6;
    }

    private static boolean chamber(int a, int b, int c) {
        double da = (a - 2.5) / 2.4, db = (b - 2.5) / 2.1, dc = (c - 2.5) / 2.4;
        return da * da + db * db + dc * dc <= 1.0;
    }

    private static TilePattern baseShape(int t) {
        TilePattern p = TilePattern.filled(BlockKind.NATIVE);
        if (t == ROCK) return p;
        boolean ap = false, am = false, cp = false, cm = false, bp = false, bm = false, shaft = false;
        if (t < 16) {
            ap = (t & A_PLUS) != 0;
            am = (t & A_MINUS) != 0;
            cp = (t & C_PLUS) != 0;
            cm = (t & C_MINUS) != 0;
        } else {
            shaft = true;
            bp = t == CHIMNEY_LOW || t == CHIMNEY_MID;
            bm = t == CHIMNEY_MID || t == CHIMNEY_HIGH;
        }
        for (int a = 0; a < 6; a++)
            for (int b = 0; b < 6; b++)
                for (int c = 0; c < 6; c++) {
                    boolean air = !shaft && chamber(a, b, c);
                    if (ap && a >= 3 && hole(b, c)) air = true;
                    if (am && a <= 2 && hole(b, c)) air = true;
                    if (cp && c >= 3 && hole(a, b)) air = true;
                    if (cm && c <= 2 && hole(a, b)) air = true;
                    if (bp && b >= 3 && hole(a, c)) air = true;
                    if (bm && b <= 2 && hole(a, c)) air = true;
                    if (shaft && ((b >= 1 && b <= 4) || (b == 0 && bm) || (b == 5 && bp)) && hole(a, c)) air = true;
                    if (air) p.set(a, b, c, BlockKind.AIR);
                }
        return p;
    }

    private static boolean floorAt(TilePattern p, int a, int c) {
        return a >= 1 && a <= 4 && c >= 1 && c <= 4 && !p.get(a, 0, c).passable && p.get(a, 1, c) == BlockKind.AIR;
    }

    private static boolean ceilAt(TilePattern p, int a, int c) {
        return a >= 1 && a <= 4 && c >= 1 && c <= 4 && !p.get(a, 5, c).passable && p.get(a, 4, c) == BlockKind.AIR;
    }

    /** Dresses a cave cell with one flavour. Never touches a face layer except to swap a solid floor block for another non-walkable block. */
    private static TilePattern dress(TilePattern base, int tile, int flavour) {
        TilePattern p = base.copy();
        boolean floorClosed = tile < 16; // chimneys have holes in the floor or ceiling: no water, no ledges
        switch (flavour) {
            case PILLAR -> {
                boolean ok = true;
                for (int b = 1; b <= 4; b++) ok &= p.get(3, b, 2) == BlockKind.AIR;
                if (ok && floorClosed) for (int b = 1; b <= 4; b++) p.set(3, b, 2, BlockKind.NATIVE);
            }
            case DRIP -> {
                int[][] up = {{1, 3}, {4, 2}, {2, 1}};
                int[][] down = {{2, 3}, {3, 4}, {4, 1}, {1, 2}};
                for (int[] ac : up) if (floorAt(p, ac[0], ac[1])) p.set(ac[0], 1, ac[1], BlockKind.POINTED_DRIPSTONE_UP);
                for (int[] ac : down) if (ceilAt(p, ac[0], ac[1])) p.set(ac[0], 4, ac[1], BlockKind.POINTED_DRIPSTONE_DOWN);
            }
            case SCULK_PATCH -> {
                for (int a = 1; a <= 4; a++) for (int c = 1; c <= 4; c++) if (floorAt(p, a, c)) p.set(a, 0, c, BlockKind.SCULK);
            }
            case MOSS_GARDEN -> {
                for (int a = 1; a <= 4; a++)
                    for (int c = 1; c <= 4; c++)
                        if (floorAt(p, a, c)) {
                            p.set(a, 0, c, BlockKind.MOSS);
                            int h = (a * 3 + c) % 3;
                            if (h == 0) p.set(a, 1, c, BlockKind.GLOW_LICHEN);
                            else if (h == 1) p.set(a, 1, c, BlockKind.MOSS_CARPET);
                        }
            }
            case POOL -> {
                if (floorClosed) {
                    boolean ok = true;
                    for (int a = 2; a <= 3; a++) for (int c = 2; c <= 3; c++) ok &= floorAt(p, a, c);
                    if (ok) for (int a = 2; a <= 3; a++) for (int c = 2; c <= 3; c++) p.set(a, 0, c, BlockKind.WATER);
                }
            }
            case LEDGE -> {
                if (floorClosed)
                    for (int a = 3; a <= 4; a++) for (int c = 1; c <= 4; c++)
                        if (p.get(a, 1, c) == BlockKind.AIR && !p.get(a, 0, c).passable && p.get(a, 2, c) == BlockKind.AIR) p.set(a, 1, c, BlockKind.NATIVE);
            }
            default -> { }
        }
        return p;
    }

    /**
     * The flavour of the dressing of one cell: a stable hash of the world seed and the cell's place in the grid, with weights (plain is the commonest). The
     * hash does not look at the neighbours, which is why it can never make a seam.
     */
    public static int flavour(long seed, int sector, int level, int ring) {
        long h = seed * 0x9E3779B97F4A7C15L + sector * 0xC2B2AE3D27D4EB4FL + level * 0x165667B19E3779F9L + ring * 0xD1B54A32D192ED03L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        int r = (int) ((h >>> 8) & 0xFFFF) % 100;
        if (r < 30) return PLAIN;
        if (r < 42) return PILLAR;
        if (r < 60) return DRIP;
        if (r < 70) return SCULK_PATCH;
        if (r < 84) return MOSS_GARDEN;
        if (r < 92) return POOL;
        return LEDGE;
    }

    // ---- the problem of the chasm

    /**
     * The Wave Function Collapse problem of the chasm's rock: {@code sectors} round the pit (wrapping), {@code levels} high (index 0 is the deepest), three rings
     * deep. The edge masks say: rock at the top, the bottom and the deepest ring; towards the pit a cell may be solid or a mouth.
     */
    public static Wfc3D.Problem problem(int sectors, int levels) {
        Wfc3D.Problem p = new Wfc3D.Problem(sectors, levels, 3, true);
        for (int x = 0; x < sectors; x++) for (int y = 0; y < levels; y++) for (int z = 0; z < 3; z++) p.setFree(x, y, z);
        p.restrictEdge(Wfc3D.NY, S_ROCK);
        p.restrictEdge(Wfc3D.PY, S_ROCK);
        p.restrictEdge(Wfc3D.PZ, S_ROCK);
        p.restrictEdge(Wfc3D.NZ, S_ROCK, S_OPEN);
        return p;
    }
}
