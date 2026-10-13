package com.newuniverse.nusmp.dungeon.nexus.zone;

import java.util.ArrayList;
import java.util.List;

/**
 * The lane of the Blood-Red Lunge, planned with no Minecraft in it: from where the Commander stands, along a heading, sample by sample, as far as
 * the ground lets it go and no further than the cap. The world is asked through {@link Ground} (can a body of its size stand here, and at what
 * height), so the plan stops at the first wall, the first drop it would not take and the first step it could not climb, and the dash that follows
 * can never pass through a block: it only ever moves between samples the ground has answered for. The lane that is drawn on the floor is the
 * lane that is run, to the block.
 */
final class MonarchLunge {
    private MonarchLunge() {}

    /** What the planner asks of the world. */
    interface Ground {
        /**
         * The height at which a body can stand at (x, z), coming from height y (a step up or a short drop is allowed), or NaN when it cannot:
         * a wall in the way, no floor, an unloaded chunk.
         */
        double stand(double x, double y, double z);
    }

    /** The distance between two samples of a lane: half a block, less than the width of the body, so nothing thin can lie between two of them. */
    static final double SPACING = 0.5;

    /**
     * The samples {x, y, z} of the lane from (sx, sy, sz) along the unit heading (dirX, dirZ), the first one the start itself, then one every
     * {@link #SPACING} blocks until `maxLen` is reached or the ground refuses a sample. A start with no way forward gives just the start.
     */
    static List<double[]> path(double sx, double sy, double sz, double dirX, double dirZ, double maxLen, Ground ground) {
        List<double[]> out = new ArrayList<>();
        out.add(new double[]{sx, sy, sz});
        double len = Math.hypot(dirX, dirZ);
        if (len < 1e-9) return out;
        double ux = dirX / len, uz = dirZ / len, y = sy;
        int steps = (int) Math.floor(Math.max(0.0, maxLen) / SPACING + 1e-9);
        for (int i = 1; i <= steps; i++) {
            double x = sx + ux * SPACING * i, z = sz + uz * SPACING * i;
            double ny = ground.stand(x, y, z);
            if (Double.isNaN(ny)) break;
            out.add(new double[]{x, ny, z});
            y = ny;
        }
        return out;
    }

    /** How far the lane runs (blocks across the floor), start to last sample. */
    static double length(List<double[]> path) {
        if (path.size() < 2) return 0.0;
        double[] a = path.get(0), b = path.get(path.size() - 1);
        return Math.hypot(b[0] - a[0], b[2] - a[2]);
    }

    /** The point {x, y, z} `d` blocks along the lane, between the two samples it falls between; before the start it is the start, past the end the end. */
    static double[] at(List<double[]> path, double d) {
        double[] first = path.get(0);
        if (d <= 0.0 || path.size() < 2) return first.clone();
        double[] last = path.get(path.size() - 1);
        if (d >= length(path)) return last.clone();
        double f = d / SPACING;
        int i = (int) Math.floor(f);
        double t = f - i;
        double[] a = path.get(Math.min(i, path.size() - 1)), b = path.get(Math.min(i + 1, path.size() - 1));
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /**
     * The points {x, y, z} that draw the lane on the floor: its two edges and its middle, about `spacing` apart along each, at most `maxPerRow`
     * to a row, at the height of the lane under each. Edges first, the middle last.
     */
    static List<double[]> rows(List<double[]> path, double dirX, double dirZ, double half, double spacing, int maxPerRow) {
        List<double[]> out = new ArrayList<>();
        double len = length(path), h = Math.hypot(dirX, dirZ);
        if (len < 1e-9 || h < 1e-9 || maxPerRow < 2) return out;
        double nx = -dirZ / h, nz = dirX / h;
        int n = (int) Math.max(2, Math.min(maxPerRow, Math.floor(len / Math.max(0.1, spacing)) + 1));
        for (int row = 0; row < 3; row++) {
            double off = row == 0 ? -half : row == 1 ? half : 0.0;
            for (int i = 0; i < n; i++) {
                double[] p = at(path, len * i / (n - 1));
                out.add(new double[]{p[0] + nx * off, p[1], p[2] + nz * off});
            }
        }
        return out;
    }
}
