package com.newuniverse.nusmp.arc.arena;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the six arenas share a small toolbox of arithmetic (a distance, an angle, a hash that makes scatter the same every time, a ring of arrival spots) and a
 * few palette conventions (a staircase block needs four facings), and writing each twice would let them drift apart. Everything here is deterministic and
 * pure: a hash of a column and a salt (never a {@code Random}), so an arena built twice, on two servers, or repaired next week is the same arena.
 */
final class ArenaKit {
    private ArenaKit() {}

    /** The horizontal distance of a column from the arena's centre. */
    static double r(int dx, int dz) { return Math.sqrt((double) dx * dx + (double) dz * dz); }

    /** The direction of a column from the centre in degrees, 0 to 360: 0 is east (+x), 90 south (+z), 180 west, 270 north. */
    static double deg(int dx, int dz) {
        double a = Math.toDegrees(Math.atan2(dz, dx));
        return a < 0 ? a + 360.0 : a;
    }

    /** A 32-bit hash of a column and a salt. */
    static int hash(int x, int z, int salt) {
        int h = x * 374761393 + z * 668265263 + salt * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        return h ^ (h >>> 15);
    }

    /** A number in [0, 1) that is the same for the same column and salt. */
    static double n(int x, int z, int salt) { return (hash(x, z, salt) & 0x7FFFFFFF) / 2147483648.0; }

    /** The column nearest to the point at distance {@code radius} and direction {@code angleDeg} from the centre. */
    static int[] at(double radius, double angleDeg) {
        double a = Math.toRadians(angleDeg);
        return new int[] {(int) Math.round(radius * Math.cos(a)), (int) Math.round(radius * Math.sin(a))};
    }

    /**
     * {@code n} columns on a circle of {@code radius} about the centre, side by side round the direction {@code centreDeg}: the first on the direction, the next
     * on either side of it, about {@code spacing} blocks apart. Duplicates (a tight circle) are dropped, so the result may be shorter than asked for when the
     * circle is too small; the callers use circles large enough for sixteen.
     */
    static int[][] arc(int n, double radius, double centreDeg, double spacing) {
        double step = Math.toDegrees(spacing / Math.max(1.0, radius));
        Set<Long> seen = new LinkedHashSet<>();
        int[][] out = new int[Math.max(0, n)][];
        int made = 0;
        for (int i = 0; made < out.length && i < out.length * 4; i++) {
            int k = (i + 1) / 2;
            double a = centreDeg + (i % 2 == 0 ? k : -k) * step;
            int[] p = at(radius, a);
            if (seen.add(((long) p[0] << 32) ^ (p[1] & 0xFFFFFFFFL))) out[made++] = p;
        }
        if (made < out.length) {
            int[][] cut = new int[made][];
            System.arraycopy(out, 0, cut, 0, made);
            return cut;
        }
        return out;
    }

    /** {@code n} columns in a row across the arena at depth {@code z}, centred on x 0, {@code spacing} apart (the first in the middle, the next either side). */
    static int[][] row(int n, int z, int spacing) {
        int[][] out = new int[Math.max(0, n)][];
        for (int i = 0; i < out.length; i++) {
            int k = (i + 1) / 2;
            out[i] = new int[] {(i % 2 == 0 ? k : -k) * spacing, z};
        }
        return out;
    }

    /** Is the column within {@code rad} (inclusive) of the point? */
    static boolean near(int dx, int dz, int px, int pz, double rad) {
        double ex = dx - px, ez = dz - pz;
        return ex * ex + ez * ez <= rad * rad;
    }

    /** Is the column on the ray from the centre in direction {@code angleDeg}, within {@code half} blocks of its line, and on its forward side? */
    static boolean onRay(int dx, int dz, double angleDeg, double half) {
        double a = Math.toRadians(angleDeg), cx = Math.cos(a), sz = Math.sin(a);
        return dx * cx + dz * sz > 0 && Math.abs(dx * sz - dz * cx) <= half;
    }

    /** The staircase part that ascends towards the centre from this column (its high side faces the middle), by the dominant axis. */
    static ArenaPart stepToCentre(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? ArenaPart.STEP_W : ArenaPart.STEP_E;
        return dz > 0 ? ArenaPart.STEP_N : ArenaPart.STEP_S;
    }

    /** A palette from alternating part, text arguments. */
    static Map<ArenaPart, String> palette(Object... kv) {
        Map<ArenaPart, String> m = new EnumMap<>(ArenaPart.class);
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((ArenaPart) kv[i], (String) kv[i + 1]);
        return m;
    }

    /** Adds the four facings of a stair block to a palette: STEP_N ascends towards the north and so on. */
    static Map<ArenaPart, String> withStairs(Map<ArenaPart, String> m, String stair) {
        m.put(ArenaPart.STEP_N, stair + "[facing=north,half=bottom]");
        m.put(ArenaPart.STEP_E, stair + "[facing=east,half=bottom]");
        m.put(ArenaPart.STEP_S, stair + "[facing=south,half=bottom]");
        m.put(ArenaPart.STEP_W, stair + "[facing=west,half=bottom]");
        return m;
    }
}
