package com.newuniverse.nusmp.dungeon.nexus;

import java.util.ArrayList;
import java.util.List;

/**
 * The ground a Nexus covers: an oval or a rectangle round a centre column, given by its half-widths. Pure, so the planner, the saved
 * record and the tests all agree on "is this column inside?" and "which chunks does it touch?". Edges are inclusive.
 */
public record Footprint(Shape shape, int centreX, int centreZ, int halfX, int halfZ) {
    public enum Shape { OVAL, RECT }

    public Footprint {
        if (shape == null) throw new IllegalArgumentException("a footprint needs a shape");
        if (halfX < 1 || halfZ < 1) throw new IllegalArgumentException("a footprint needs half-widths of at least 1: " + halfX + " x " + halfZ);
    }

    public static Footprint oval(int centreX, int centreZ, int halfX, int halfZ) { return new Footprint(Shape.OVAL, centreX, centreZ, halfX, halfZ); }

    public static Footprint rect(int centreX, int centreZ, int halfX, int halfZ) { return new Footprint(Shape.RECT, centreX, centreZ, halfX, halfZ); }

    /** Is the offset (u, v) from the centre inside the oval of half-widths a and b? Integer arithmetic only; a half-width of 0 is a line, a negative one nothing. */
    public static boolean inside(long u, long v, long a, long b) {
        if (a < 0 || b < 0) return false;
        if (a == 0 || b == 0) return Math.abs(u) <= a && Math.abs(v) <= b;
        return u * u * b * b + v * v * a * a <= a * a * b * b;
    }

    /** The largest u with (u, v) inside the oval of half-widths a and b; -1 if the row v is outside it. */
    public static int extent(int v, int a, int b) {
        if (a < 0 || b < 0 || Math.abs((long) v) > b) return -1;
        if (a == 0) return 0;
        if (b == 0) return a;
        long rhs = (long) a * a * ((long) b * b - (long) v * v);
        long t = rhs / ((long) b * b);
        int u = (int) Math.sqrt((double) t);
        while ((long) u * u > t) u--;
        while ((long) (u + 1) * (u + 1) <= t) u++;
        return u;
    }

    public boolean contains(int x, int z) { return contains(x, z, 0); }

    /** Is this column inside, with the half-widths grown by margin blocks (a negative margin shrinks them)? */
    public boolean contains(int x, int z, int margin) {
        int a = halfX + margin, b = halfZ + margin;
        long u = (long) x - centreX, v = (long) z - centreZ;
        if (shape == Shape.RECT) return a >= 0 && b >= 0 && Math.abs(u) <= a && Math.abs(v) <= b;
        return inside(u, v, a, b);
    }

    public int minX() { return centreX - halfX; }

    public int maxX() { return centreX + halfX; }

    public int minZ() { return centreZ - halfZ; }

    public int maxZ() { return centreZ + halfZ; }

    private static int gap(int c, int lo, int hi) { return c < lo ? lo - c : Math.max(0, c - hi); }

    /** Does any column of this chunk lie inside? */
    public boolean touchesChunk(int chunkX, int chunkZ) {
        int du = gap(centreX, chunkX * 16, chunkX * 16 + 15), dv = gap(centreZ, chunkZ * 16, chunkZ * 16 + 15);
        return shape == Shape.RECT ? du <= halfX && dv <= halfZ : inside(du, dv, halfX, halfZ);
    }

    /** The chunks (x, z) the footprint touches: all of them must be loaded before building. */
    public List<int[]> chunks() {
        List<int[]> out = new ArrayList<>();
        for (int cx = Math.floorDiv(minX(), 16); cx <= Math.floorDiv(maxX(), 16); cx++) {
            for (int cz = Math.floorDiv(minZ(), 16); cz <= Math.floorDiv(maxZ(), 16); cz++) {
                if (touchesChunk(cx, cz)) out.add(new int[]{cx, cz});
            }
        }
        return out;
    }
}
