package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: the block primitives (place, floor_swap, wall_blink, slider, cage ...) all describe their footprint as a named shape and a radius, and the same shapes must come out of
 * the same words everywhere, deterministically from the instance's seed. This pure class turns a shape name into the list of (dx, dz) offsets from the centre cell, so JUnit can
 * pin the counts and the block code only decides what to put there and at which height. Offsets are bounded by the radius (at most 12), so a shape never exceeds 625 cells.
 */
public final class Shapes {
    private Shapes() {}

    public static final int MAX_RADIUS = 12;

    /** The offsets of a shape. {@code count} is used by the random shapes (scatter, pillars). */
    public static List<int[]> offsets(String shape, int radius, int count, SplittableRandom rng) {
        int r = Math.max(1, Math.min(MAX_RADIUS, radius));
        List<int[]> out = new ArrayList<>();
        switch (shape) {
            case "ring" -> {
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d >= r - 0.5 && d < r + 0.5) out.add(new int[] {x, z});
                }
            }
            case "cross" -> {
                for (int i = -r; i <= r; i++) {
                    out.add(new int[] {i, 0});
                    if (i != 0) out.add(new int[] {0, i});
                }
            }
            case "line" -> { for (int i = -r; i <= r; i++) out.add(new int[] {i, 0}); }
            case "square" -> { for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) out.add(new int[] {x, z}); }
            case "walls" -> {
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                    boolean edge = Math.abs(x) == r || Math.abs(z) == r;
                    boolean gap = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                    if (edge && !gap) out.add(new int[] {x, z});
                }
            }
            case "grid" -> {
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) if (x * x + z * z <= r * r && Math.floorMod(x, 2) == 0 && Math.floorMod(z, 2) == 0) out.add(new int[] {x, z});
            }
            case "checker" -> {
                for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) if (x * x + z * z <= r * r && Math.floorMod(x + z, 2) == 0) out.add(new int[] {x, z});
            }
            case "scatter", "pillars" -> {
                List<int[]> disc = offsets("disc", r, 0, rng);
                for (int i = disc.size() - 1; i > 0; i--) {
                    int j = rng.nextInt(i + 1);
                    int[] t = disc.get(i);
                    disc.set(i, disc.get(j));
                    disc.set(j, t);
                }
                int n = Math.max(1, Math.min(count, disc.size()));
                out.addAll(disc.subList(0, n));
            }
            default -> { for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) if (x * x + z * z <= r * r + r) out.add(new int[] {x, z}); }   // disc
        }
        return out;
    }
}
