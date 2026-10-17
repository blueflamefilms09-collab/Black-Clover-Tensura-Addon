package com.newuniverse.nusmp.labyrinth.mega.guard;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the geometry behind the Guardian's theme traits, with no Minecraft types, so each shape can be tested: where a blink lands behind a
 * target, which players a chain of lightning visits and for how much, where a ring of stars falls, and which cells a walking fire leaves
 * burning. The entity glue ({@link GuardianTraits}) asks these questions and does the world-facing part (checking the floor, hurting
 * players, sending particles), so a mistake in a formula is found by a unit test and not by watching a boss in game. Everything is plain
 * {@code double} / {@code int} arrays because positions here are only coordinates, never blocks.
 */
public final class TraitMath {
    private TraitMath() {}

    /** Each hop of the chain keeps this share of the previous hop's damage. */
    public static final double CHAIN_DECAY = 0.75;
    /** How far the chain jumps from one player to the next, in blocks. */
    public static final double CHAIN_HOP = 6.0;

    /**
     * The four cells a blink may land on next to a target at ({@code tx}, {@code tz}) that looks along ({@code lookX}, {@code lookZ}): right
     * behind it, to its left, to its right, and (last resort) in front of it, each {@code dist} blocks from it. A look with no horizontal part
     * (looking straight up or down) counts as +x. Same order and distance rule as the Floor Warden's Shadow Step, so the two read alike.
     */
    public static double[][] blinkSpots(double tx, double tz, double lookX, double lookZ, double dist) {
        double len = Math.hypot(lookX, lookZ);
        double fx = len < 1e-3 ? 1.0 : lookX / len, fz = len < 1e-3 ? 0.0 : lookZ / len;
        return new double[][] {
                {tx - fx * dist, tz - fz * dist},
                {tx + fz * dist, tz - fx * dist},
                {tx - fz * dist, tz + fx * dist},
                {tx + fx * dist, tz + fz * dist}};
    }

    /**
     * The order a chain of lightning visits points {@code {x, y, z}}: from ({@code sx}, {@code sy}, {@code sz}) to the nearest point within
     * {@code firstReach}, then from each point to the nearest not yet visited one within {@code hopReach}, at most {@code maxTargets} points.
     * Returns indexes into {@code pts} in visiting order (empty when nothing is in reach). Ties go to the lower index, so it is deterministic.
     */
    public static int[] chainOrder(double sx, double sy, double sz, double[][] pts, double firstReach, double hopReach, int maxTargets) {
        List<Integer> order = new ArrayList<>();
        boolean[] seen = new boolean[pts.length];
        double cx = sx, cy = sy, cz = sz;
        double reach = firstReach;
        for (int hop = 0; hop < maxTargets; hop++) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < pts.length; i++) {
                if (seen[i]) continue;
                double dx = pts[i][0] - cx, dy = pts[i][1] - cy, dz = pts[i][2] - cz;
                double d = dx * dx + dy * dy + dz * dz;
                if (d <= reach * reach && d < bestD) { best = i; bestD = d; }
            }
            if (best < 0) break;
            seen[best] = true;
            order.add(best);
            cx = pts[best][0];
            cy = pts[best][1];
            cz = pts[best][2];
            reach = hopReach;
        }
        int[] out = new int[order.size()];
        for (int i = 0; i < out.length; i++) out[i] = order.get(i);
        return out;
    }

    /** Damage of the {@code hop}-th target of a chain (0 = the first): the base, weakened {@link #CHAIN_DECAY} per hop. */
    public static double chainDamage(double base, int hop) { return base * Math.pow(CHAIN_DECAY, Math.max(0, hop)); }

    /** {@code count} points evenly round ({@code cx}, {@code cz}) at {@code radius}, the first one at angle {@code phase} (radians). */
    public static double[][] starRing(double cx, double cz, double radius, int count, double phase) {
        int n = Math.max(0, count);
        double[][] out = new double[n][];
        for (int k = 0; k < n; k++) {
            double a = phase + 2.0 * Math.PI * k / n;
            out[k] = new double[] {cx + Math.cos(a) * radius, cz + Math.sin(a) * radius};
        }
        return out;
    }

    /**
     * The cells a walking fire leaves burning. A flame Guardian does not place fire blocks (the dungeon's blocks are held by the Dungeon
     * Guard and a real fire would spread to the banners): it keeps this short list of floor cells, each alive for a while, and hurts
     * whoever stands on one. The list never grows past {@code max}; the oldest cell is dropped first; walking on the same cell refreshes it.
     */
    public static final class FireTrail {
        private final List<int[]> cells = new ArrayList<>();
        private final List<Long> expires = new ArrayList<>();

        /** Adds (or refreshes) the cell {@code (x, y, z)}, alive until game tick {@code expiresAt}; keeps at most {@code max} cells. */
        public void add(int x, int y, int z, long expiresAt, int max) {
            for (int i = 0; i < cells.size(); i++) {
                int[] c = cells.get(i);
                if (c[0] == x && c[1] == y && c[2] == z) { expires.set(i, Math.max(expires.get(i), expiresAt)); return; }
            }
            cells.add(new int[] {x, y, z});
            expires.add(expiresAt);
            while (cells.size() > Math.max(1, max)) { cells.remove(0); expires.remove(0); }
        }

        /** Forgets every cell that has burnt out at game tick {@code now}. */
        public void prune(long now) {
            for (int i = cells.size() - 1; i >= 0; i--) {
                if (expires.get(i) <= now) { cells.remove(i); expires.remove(i); }
            }
        }

        public int size() { return cells.size(); }

        public int[] cell(int i) { return cells.get(i); }

        public void clear() { cells.clear(); expires.clear(); }

        /** Is the point inside any cell still burning at {@code now}: horizontally within {@code halfWidth} of the cell's middle, vertically from its floor up two blocks? */
        public boolean burnsAt(double px, double py, double pz, double halfWidth, long now) {
            for (int i = 0; i < cells.size(); i++) {
                if (expires.get(i) <= now) continue;
                int[] c = cells.get(i);
                if (Math.abs(px - (c[0] + 0.5)) <= 0.5 + halfWidth && Math.abs(pz - (c[2] + 0.5)) <= 0.5 + halfWidth && py >= c[1] - 0.2 && py <= c[1] + 2.0) return true;
            }
            return false;
        }
    }
}
