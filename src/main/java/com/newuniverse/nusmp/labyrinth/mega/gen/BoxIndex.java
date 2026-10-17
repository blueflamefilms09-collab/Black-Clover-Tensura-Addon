package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Why: the assembler asks "does this candidate box overlap anything already placed?" for every candidate of every socket, which is tens of thousands
 * of questions per sector. Walking all 450 placed boxes each time would make a sector cost a visible fraction of a second and a 200-seed test cost
 * minutes, so the placed boxes live in a grid whose cells are one module (9 blocks) wide: a query looks only at the few cells the candidate covers.
 * The grid is two-dimensional (every piece shares the same floor row, so y never separates two pieces) but the final test is on full 3-D boxes, so a
 * future multi-level sector would still be correct, only slower. An id is the position in insertion order, which the assembler keeps equal to the
 * piece index; {@link #removeLast} undoes the newest insertion, which is all the backtracking search of the boss chain needs. Only lookups by key are
 * used (never an iteration over the map), so results do not depend on hash order.
 */
public final class BoxIndex {
    private static final int CELL = MegaGrid.MODULE;

    private final HashMap<Long, IntList> cells = new HashMap<>();
    private final List<Box> boxes = new ArrayList<>();

    /** Number of boxes held. */
    public int size() { return boxes.size(); }

    public Box get(int id) { return boxes.get(id); }

    /** Adds a box and returns its id (the next integer). */
    public int add(Box b) {
        int id = boxes.size();
        boxes.add(b);
        int x1 = Math.floorDiv(b.minX(), CELL), x2 = Math.floorDiv(b.maxX(), CELL), z1 = Math.floorDiv(b.minZ(), CELL), z2 = Math.floorDiv(b.maxZ(), CELL);
        for (int cx = x1; cx <= x2; cx++) {
            for (int cz = z1; cz <= z2; cz++) cells.computeIfAbsent(key(cx, cz), k -> new IntList()).add(id);
        }
        return id;
    }

    /** Removes the box added last. */
    public void removeLast() {
        int id = boxes.size() - 1;
        Box b = boxes.remove(id);
        int x1 = Math.floorDiv(b.minX(), CELL), x2 = Math.floorDiv(b.maxX(), CELL), z1 = Math.floorDiv(b.minZ(), CELL), z2 = Math.floorDiv(b.maxZ(), CELL);
        for (int cx = x1; cx <= x2; cx++) {
            for (int cz = z1; cz <= z2; cz++) {
                IntList l = cells.get(key(cx, cz));
                if (l != null && l.size() > 0 && l.get(l.size() - 1) == id) l.removeLast();
            }
        }
    }

    /** The id of the first stored box that shares a cell with {@code b}, or -1. */
    public int firstOverlap(Box b) {
        int x1 = Math.floorDiv(b.minX(), CELL), x2 = Math.floorDiv(b.maxX(), CELL), z1 = Math.floorDiv(b.minZ(), CELL), z2 = Math.floorDiv(b.maxZ(), CELL);
        for (int cx = x1; cx <= x2; cx++) {
            for (int cz = z1; cz <= z2; cz++) {
                IntList l = cells.get(key(cx, cz));
                if (l == null) continue;
                for (int i = 0; i < l.size(); i++) {
                    int id = l.get(i);
                    if (boxes.get(id).overlaps(b)) return id;
                }
            }
        }
        return -1;
    }

    public boolean overlapsAny(Box b) { return firstOverlap(b) >= 0; }

    private static long key(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    /** A minimal growable int list (no boxing: this is the hot path of the search). */
    private static final class IntList {
        private int[] a = new int[4];
        private int n;

        void add(int v) {
            if (n == a.length) a = java.util.Arrays.copyOf(a, n * 2);
            a[n++] = v;
        }

        int size() { return n; }

        int get(int i) { return a[i]; }

        void removeLast() { n--; }
    }
}
