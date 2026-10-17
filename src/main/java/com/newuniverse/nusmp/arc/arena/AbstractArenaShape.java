package com.newuniverse.nusmp.arc.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Why: every arena needs the same plumbing (its names, its palette, the bounds check that keeps a column outside the build empty, the clamp on the number of
 * arrivals, and the "keep clear" rule that stops scattered rubble, flowers and trees from landing on the boss's spot, on the arrival places or on the pads of
 * the ways home), so the six shapes only write what makes them different: how one column is built and where the travellers stand. The keep-clear rule is
 * what makes the standing places trustworthy: a decoration is never put within two blocks of where somebody is placed.
 */
abstract class AbstractArenaShape implements ArenaShape {
    private final String id, title;
    private final int radius;
    private final Map<ArenaPart, String> palette;
    private final int[] boss;
    private final int[][] sigils;
    private int[][] arrivals;

    protected AbstractArenaShape(String id, String title, int radius, Map<ArenaPart, String> palette, int[] boss, int[][] sigils) {
        this.id = id;
        this.title = title;
        this.radius = radius;
        this.palette = Collections.unmodifiableMap(palette);
        this.boss = boss.clone();
        this.sigils = sigils.clone();
    }

    @Override public final String id() { return id; }

    @Override public final String title() { return title; }

    @Override public final int radius() { return radius; }

    @Override public final Map<ArenaPart, String> palette() { return palette; }

    @Override public final int[] bossSpot() { return boss.clone(); }

    @Override public final int[][] sigils() {
        int[][] out = new int[sigils.length][];
        for (int i = 0; i < sigils.length; i++) out[i] = sigils[i].clone();
        return out;
    }

    @Override
    public final List<ArenaCell> column(int dx, int dz) {
        if (Math.abs(dx) > radius || Math.abs(dz) > radius) return List.of();
        List<ArenaCell> out = new ArrayList<>(8);
        fill(dx, dz, out);
        return out;
    }

    /** Builds one column inside the bounds: cells bottom to top. */
    protected abstract void fill(int dx, int dz, List<ArenaCell> out);

    /** The shape's own arrival rule for 1 to {@link #MAX_ARRIVALS} travellers. */
    protected abstract int[][] arrival(int n);

    @Override
    public final int[][] arrivalSpots(int n) {
        int count = Math.max(1, Math.min(MAX_ARRIVALS, n));
        int[][] out = arrival(count);
        int[][] copy = new int[out.length][];
        for (int i = 0; i < out.length; i++) copy[i] = out[i].clone();
        return copy;
    }

    // ---------------------------------------------------------------- keep clear
    /** All the places somebody is put (sixteen arrivals), computed once. */
    private int[][] allArrivals() {
        if (arrivals == null) arrivals = arrival(MAX_ARRIVALS);
        return arrivals;
    }

    /** Is the column on a pad of a way home (within one block of its centre, the pad is 3 x 3)? */
    protected final boolean onSigilPad(int dx, int dz) {
        for (int[] s : sigils) if (Math.abs(dx - s[0]) <= 1 && Math.abs(dz - s[1]) <= 1) return true;
        return false;
    }

    /** Is the column the centre of a way home? */
    protected final boolean onSigil(int dx, int dz) {
        for (int[] s : sigils) if (dx == s[0] && dz == s[1]) return true;
        return false;
    }

    /** May a scattered decoration (rubble, a flower, a tree, a lamp post) stand here: not near the boss, an arrival place or a pad? */
    protected final boolean clear(int dx, int dz) {
        if (ArenaKit.near(dx, dz, boss[0], boss[1], 3.0)) return false;
        for (int[] s : sigils) if (ArenaKit.near(dx, dz, s[0], s[1], 3.0)) return false;
        for (int[] a : allArrivals()) if (ArenaKit.near(dx, dz, a[0], a[1], 2.0)) return false;
        return true;
    }
}
