package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.List;

/**
 * The cube of Zenon's Spatial Mana Domination (pure, no Minecraft types, unit tested): where it is, who is inside it, how far the
 * nearest wall is, which way out is closest, and where its twelve edges run (the server draws them in dust, since the cube must be
 * as visible as it is exact). An axis-aligned box of side {@code 2 * half} around (cx, cy, cz); a point on a wall counts as inside.
 */
public record DomainCube(double cx, double cy, double cz, double half) {
    public static final double MIN_HALF = 3, MAX_HALF = 24;
    /** How far under the ground point the cube's floor lies, so a foe standing on the ground is never grazing the floor. */
    public static final double BELOW_FLOOR = 3;
    public static final int WEST = 0, EAST = 1, DOWN = 2, UP = 3, NORTH = 4, SOUTH = 5;

    public DomainCube {
        half = Double.isFinite(half) ? Math.max(MIN_HALF, Math.min(MAX_HALF, half)) : MIN_HALF;
    }

    /**
     * The cube around a point on the ground: centred on (x, z), the floor {@link #BELOW_FLOOR} blocks under {@code groundY}, so it
     * reaches {@code 2 * half - BELOW_FLOOR} blocks above the ground.
     */
    public static DomainCube around(double x, double groundY, double z, double half) {
        double h = Double.isFinite(half) ? Math.max(MIN_HALF, Math.min(MAX_HALF, half)) : MIN_HALF;
        return new DomainCube(x, groundY - BELOW_FLOOR + h, z, h);
    }

    public double minX() { return cx - half; }
    public double maxX() { return cx + half; }
    public double minY() { return cy - half; }
    public double maxY() { return cy + half; }
    public double minZ() { return cz - half; }
    public double maxZ() { return cz + half; }

    public boolean contains(double x, double y, double z) {
        return x >= minX() && x <= maxX() && y >= minY() && y <= maxY() && z >= minZ() && z <= maxZ();
    }

    /** The same test against the cube grown by 'pad' on every side (a negative pad shrinks it). */
    public boolean contains(double x, double y, double z, double pad) {
        double h = half + pad;
        return Math.abs(x - cx) <= h && Math.abs(y - cy) <= h && Math.abs(z - cz) <= h;
    }

    /** Distance to the nearest wall: positive inside, zero on a wall, negative outside (how far past the nearest wall plane it is). */
    public double wallDistance(double x, double y, double z) {
        return half - Math.max(Math.abs(x - cx), Math.max(Math.abs(y - cy), Math.abs(z - cz)));
    }

    /** The distance from a point inside to each of the six walls, indexed by {@link #WEST} .. {@link #SOUTH}. */
    public double[] faceDistances(double x, double y, double z) {
        return new double[]{x - minX(), maxX() - x, y - minY(), maxY() - y, z - minZ(), maxZ() - z};
    }

    /** The closest wall of any kind (ties go to the lower index). */
    public int nearestFace(double x, double y, double z) {
        double[] d = faceDistances(x, y, z);
        int best = 0;
        for (int i = 1; i < d.length; i++) if (d[i] < d[best]) best = i;
        return best;
    }

    /** The closest of the four side walls, the way out a walker can take. */
    public int nearestSideFace(double x, double z) {
        double[] d = faceDistances(x, cy, z);
        int best = WEST;
        for (int f : new int[]{EAST, NORTH, SOUTH}) if (d[f] < d[best]) best = f;
        return best;
    }

    public static String faceName(int face) {
        return switch (face) {
            case WEST -> "west";
            case EAST -> "east";
            case DOWN -> "down";
            case UP -> "up";
            case NORTH -> "north";
            case SOUTH -> "south";
            default -> "away";
        };
    }

    /** "west wall, 6 blocks": the nearest side wall of a point inside (distance rounded up, at least 1). */
    public String exitHint(double x, double y, double z) {
        int f = nearestSideFace(x, z);
        double d = faceDistances(x, y, z)[f];
        return faceName(f) + " wall, " + Math.max(1, (int) Math.ceil(d)) + (Math.ceil(d) <= 1 ? " block" : " blocks");
    }

    /** The unit step on the horizontal axes that leaves through a side wall (x, z). */
    public static double[] faceDirection(int face) {
        return switch (face) {
            case WEST -> new double[]{-1, 0};
            case EAST -> new double[]{1, 0};
            case NORTH -> new double[]{0, -1};
            case SOUTH -> new double[]{0, 1};
            default -> new double[]{0, 0};
        };
    }

    public static final int EDGES = 12;

    /** The twelve edges as {x0, y0, z0, x1, y1, z1}: four along X (indices 0..3), four along Y (4..7), four along Z (8..11). */
    public double[][] edges() {
        double[][] out = new double[EDGES][];
        int k = 0;
        for (int a = 0; a < 2; a++)
            for (int b = 0; b < 2; b++) {
                double y = a == 0 ? minY() : maxY(), z = b == 0 ? minZ() : maxZ();
                out[k++] = new double[]{minX(), y, z, maxX(), y, z};
            }
        for (int a = 0; a < 2; a++)
            for (int b = 0; b < 2; b++) {
                double x = a == 0 ? minX() : maxX(), z = b == 0 ? minZ() : maxZ();
                out[k++] = new double[]{x, minY(), z, x, maxY(), z};
            }
        for (int a = 0; a < 2; a++)
            for (int b = 0; b < 2; b++) {
                double x = a == 0 ? minX() : maxX(), y = b == 0 ? minY() : maxY();
                out[k++] = new double[]{x, y, minZ(), x, y, maxZ()};
            }
        return out;
    }

    /** Points along one edge, 'spacing' apart (both corners included); an unknown edge gives none. */
    public List<double[]> edgeSamples(int edge, double spacing) {
        List<double[]> out = new ArrayList<>();
        if (edge < 0 || edge >= EDGES) return out;
        double[] e = edges()[edge];
        double len = 2 * half, sp = Math.max(0.25, spacing);
        int n = Math.max(1, (int) Math.ceil(len / sp));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            out.add(new double[]{e[0] + (e[3] - e[0]) * t, e[1] + (e[4] - e[1]) * t, e[2] + (e[5] - e[2]) * t});
        }
        return out;
    }

    /**
     * A point on the wall plane 'face' that lies nearest to (x, y, z), for the shimmer that shows a player which way the wall is.
     * Coordinates are clamped to the face's rectangle.
     */
    public double[] wallPoint(int face, double x, double y, double z) {
        double px = Math.max(minX(), Math.min(maxX(), x)), py = Math.max(minY(), Math.min(maxY(), y)), pz = Math.max(minZ(), Math.min(maxZ(), z));
        return switch (face) {
            case WEST -> new double[]{minX(), py, pz};
            case EAST -> new double[]{maxX(), py, pz};
            case DOWN -> new double[]{px, minY(), pz};
            case UP -> new double[]{px, maxY(), pz};
            case NORTH -> new double[]{px, py, minZ()};
            default -> new double[]{px, py, maxZ()};
        };
    }
}
