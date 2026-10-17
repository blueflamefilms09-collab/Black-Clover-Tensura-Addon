package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.util.Arrays;

/**
 * Why: a Wave Function Collapse tile is only a name and six sockets to the solver, but to the world it is a small block of voxels. This is that block:
 * {@value #N} x {@value #N} x {@value #N} {@link BlockKind}s indexed {@code (a, b, c)} where {@code a} runs along the ring (tangentially), {@code b} up and
 * {@code c} radially outwards from the landmark's inner face. It is authored at a fixed resolution so the art is written once, and {@link #sample} stretches
 * it to whatever cell size the server configured (every voxel of the output comes from exactly one authored voxel, the floor and the walls stay one voxel thick, so
 * doorways and pillars stay whole). {@link #openFace} reads the layer on a face as a mask of passable voxels, which is what the seam test compares: two tiles whose sockets
 * match must show the same opening where they touch, otherwise "seamless" would only be a claim.
 * Pure: no Minecraft import.
 */
public final class TilePattern {
    /** The authored resolution of every pattern, in voxels per side. */
    public static final int N = 6;

    private final BlockKind[] v = new BlockKind[N * N * N];

    public TilePattern() { Arrays.fill(v, BlockKind.AIR); }

    /** A pattern completely filled with one kind. */
    public static TilePattern filled(BlockKind k) {
        TilePattern p = new TilePattern();
        Arrays.fill(p.v, k);
        return p;
    }

    private static int idx(int a, int b, int c) { return (a * N + b) * N + c; }

    public BlockKind get(int a, int b, int c) { return v[idx(a, b, c)]; }

    public TilePattern set(int a, int b, int c, BlockKind k) {
        v[idx(a, b, c)] = k;
        return this;
    }

    /** Fills the box from (a0, b0, c0) to (a1, b1, c1), both ends included. */
    public TilePattern fill(int a0, int a1, int b0, int b1, int c0, int c1, BlockKind k) {
        for (int a = a0; a <= a1; a++) for (int b = b0; b <= b1; b++) for (int c = c0; c <= c1; c++) v[idx(a, b, c)] = k;
        return this;
    }

    /** A copy of this pattern. */
    public TilePattern copy() {
        TilePattern p = new TilePattern();
        System.arraycopy(v, 0, p.v, 0, v.length);
        return p;
    }

    /**
     * Maps an index of a cell of {@code s} voxels onto the authored pattern. Why not plain nearest neighbour: the first and the last layer of a pattern are its
     * floor and ceiling, its two side walls, its inner and outer wall, and doorways are cut in the layers between; a plain stretch would double the floor
     * row at some sizes and raise the sill of every doorway by a block. This keeps the first and the last layer one voxel thick at every cell size and stretches
     * only the layers between them (at the authored size 6 it is the identity).
     */
    static int map(int i, int s) {
        if (s <= 1 || i <= 0) return 0;
        if (i >= s - 1) return N - 1;
        if (s <= 2) return 0;
        return 1 + (i - 1) * (N - 2) / (s - 2);
    }

    /**
     * The voxel at (a, b, c) of a cell that is {@code sa} x {@code sb} x {@code sc} voxels in size, taken from the authored pattern (see {@link #map}). Indices
     * outside the cell are clamped onto it, so a caller that rounds a coordinate up cannot read outside the array.
     */
    public BlockKind sample(int a, int b, int c, int sa, int sb, int sc) {
        int ia = map(Math.min(Math.max(a, 0), Math.max(0, sa - 1)), sa);
        int ib = map(Math.min(Math.max(b, 0), Math.max(0, sb - 1)), sb);
        int ic = map(Math.min(Math.max(c, 0), Math.max(0, sc - 1)), sc);
        return v[idx(ia, ib, ic)];
    }

    /**
     * The layer on a face as a mask of passable voxels ({@code BlockKind.passable}). Faces use the {@link Wfc3D} direction numbers: 0 = +a (the layer
     * a = N-1), 1 = -a (a = 0), 2 = +b (top), 3 = -b (bottom), 4 = +c (outer), 5 = -c (inner). The two free coordinates run in the order a, b, c with the
     * fixed one left out, so the same face of two neighbouring tiles lines up index by index.
     */
    public boolean[] openFace(int dir) {
        boolean[] m = new boolean[N * N];
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++) {
                BlockKind k = switch (dir) {
                    case 0 -> get(N - 1, i, j);
                    case 1 -> get(0, i, j);
                    case 2 -> get(i, N - 1, j);
                    case 3 -> get(i, 0, j);
                    case 4 -> get(i, j, N - 1);
                    default -> get(i, j, 0);
                };
                m[i * N + j] = k.passable;
            }
        return m;
    }

    /** True when any voxel of the pattern has this kind. */
    public boolean contains(BlockKind kind) {
        for (BlockKind k : v) if (k == kind) return true;
        return false;
    }

    /** How many voxels have this kind. */
    public int count(BlockKind kind) {
        int n = 0;
        for (BlockKind k : v) if (k == kind) n++;
        return n;
    }
}
