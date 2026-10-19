package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.mechanics.Seeds;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Why: where each cell of a {@link Layout} stands relative to the others, in whole blocks, as pure arithmetic (no world), so the tests can prove that EVERY puzzle at EVERY depth and party size
 * fits in the interior of the gallery rooms before any room exists. A group is a row, a grid, a ring, a scatter or a list of explicit points; groups are laid side by side with a two-block
 * margin, either stacked along the depth (z) or along the width (x). The first group's row runs along x and its rows along z, and nothing is ever rotated, so an arrow that says north really
 * points to -z. {@link PuzzleStage} adds the anchor and checks the real blocks.
 */
public final class PuzzleGeometry {
    private PuzzleGeometry() {}

    /** Cell {@code i} stands at offset ({@code x[i]}, {@code z[i]}) from the top-left corner of a box of {@code w} x {@code d} blocks. */
    public record Arrangement(int w, int d, int[] x, int[] z) {}

    private record Geo(int w, int d, int[] ox, int[] oz) {}

    public static final int MARGIN = 2;

    private static Geo geo(Layout.Group g, SplittableRandom rng) {
        int n = g.count();
        int[] ox = new int[n], oz = new int[n];
        int gap = Math.max(1, g.gap());
        switch (g.shape()) {
            case ROW -> {
                for (int i = 0; i < n; i++) ox[i] = i * gap;
                return new Geo((n - 1) * gap + 1, 1, ox, oz);
            }
            case GRID -> {
                for (int i = 0; i < n; i++) { ox[i] = (i % g.cols()) * gap; oz[i] = (i / g.cols()) * gap; }
                return new Geo((g.cols() - 1) * gap + 1, (g.rows() - 1) * gap + 1, ox, oz);
            }
            case RING -> {
                int r = Math.max(2, (int) Math.ceil(n * gap / (2 * Math.PI)));
                while (true) {
                    Set<Long> seen = new HashSet<>();
                    boolean ok = true;
                    for (int i = 0; i < n; i++) {
                        double a = i * 2 * Math.PI / n - Math.PI / 2;
                        ox[i] = r + (int) Math.round(Math.cos(a) * r);
                        oz[i] = r + (int) Math.round(Math.sin(a) * r);
                        ok &= seen.add(((long) ox[i] << 32) ^ oz[i]);
                        for (int j = 0; j < i; j++) if (Math.abs(ox[i] - ox[j]) + Math.abs(oz[i] - oz[j]) < 2) ok = false;
                    }
                    if (ok || r > 8) return new Geo(2 * r + 1, 2 * r + 1, ox, oz);
                    r++;
                }
            }
            case SCATTER -> {
                int radius = Math.max(3, Math.min(4, n));
                int size = 2 * radius + 1;
                for (int i = 0; i < n; i++) {
                    for (int tries = 0; tries < 80; tries++) {
                        int x = rng.nextInt(size), z = rng.nextInt(size);
                        boolean ok = true;
                        for (int j = 0; j < i; j++) if (Math.max(Math.abs(ox[j] - x), Math.abs(oz[j] - z)) < 2) ok = false;
                        if (!ok && tries < 79) continue;
                        ox[i] = x;
                        oz[i] = z;
                        break;
                    }
                }
                return new Geo(size, size, ox, oz);
            }
            default -> {
                int mx = 0, mz = 0;
                for (int i = 0; i < n; i++) { ox[i] = g.px()[i] * gap; oz[i] = g.pz()[i] * gap; mx = Math.max(mx, ox[i]); mz = Math.max(mz, oz[i]); }
                return new Geo(mx + 1, mz + 1, ox, oz);
            }
        }
    }

    /** The arrangement of a layout: stacked along the depth (z), or along the width (x) when {@code alongX}. The seed only matters for scatters. */
    public static Arrangement arrange(Layout lay, long seed, boolean alongX) {
        SplittableRandom rng = Seeds.rng(Seeds.mix(seed, 0x5CA77E4L));
        int n = lay.groups().size();
        Geo[] geos = new Geo[n];
        for (int i = 0; i < n; i++) geos[i] = geo(lay.groups().get(i), rng);
        int w = 0, d = 0;
        for (Geo g : geos) {
            if (alongX) { w += g.w(); d = Math.max(d, g.d()); }
            else { d += g.d(); w = Math.max(w, g.w()); }
        }
        if (alongX) w += MARGIN * (n - 1);
        else d += MARGIN * (n - 1);
        int[] x = new int[lay.total()], z = new int[lay.total()];
        int cursor = 0;
        for (int gi = 0; gi < n; gi++) {
            Geo g = geos[gi];
            int gx = alongX ? cursor : (w - g.w()) / 2;
            int gz = alongX ? (d - g.d()) / 2 : cursor;
            int first = lay.start(gi);
            for (int i = 0; i < g.ox().length; i++) {
                x[first + i] = gx + g.ox()[i];
                z[first + i] = gz + g.oz()[i];
            }
            cursor += (alongX ? g.w() : g.d()) + MARGIN;
        }
        return new Arrangement(w, d, x, z);
    }

    /** Does either arrangement fit in an interior of {@code w} x {@code d} blocks? */
    public static boolean fits(Layout lay, long seed, int w, int d) {
        for (boolean alongX : new boolean[] {false, true}) {
            Arrangement a = arrange(lay, seed, alongX);
            if (a.w() <= w && a.d() <= d) return true;
        }
        return false;
    }
}
