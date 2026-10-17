package com.newuniverse.nusmp.arc.sds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

/**
 * Why: King's Disaster is a field in which life and death are decided by where you stand: every few seconds it pulses and strikes everyone in it except those inside a
 * garden of pollen (the Pollen Garden of Chastiefol, a barrier that "heals the wounds of those inside and eliminates any pain", see docs/arc/king.md). A field like that is
 * only fair if a garden is always within reach: it is a test of attention, never of luck. This pure planner places the gardens of one beat so that EVERY foe inside the
 * field has a garden within {@code maxWalk} blocks of where they stand when the gardens appear (the move gives them {@code warn} ticks to get there; the glue picks
 * {@code maxWalk} from the walking speed of a player and a reaction time), keeps the gardens inside the field and apart from one another, fills up to the wanted count with
 * further gardens at random, and asks the glue's {@code placeable} test (is there a floor here) before it uses a spot. It uses a seeded random so a plan can be repeated in a test.
 */
public final class SafeGardens {
    private SafeGardens() {}

    public record Garden(double x, double z) {}

    /** The most gardens one beat ever has (the dust of a crowd stays bounded). */
    public static final int MAX = 8;

    /**
     * Plans the gardens of one beat.
     *
     * @param foes         where the foes stand: {x, z} pairs
     * @param cx           centre of the field
     * @param cz           centre of the field
     * @param fieldRadius  radius of the field
     * @param count        how many gardens are wanted in all (at least; a garden is added for a foe who has none in reach)
     * @param gardenRadius radius of one garden
     * @param maxWalk      the farthest a foe may have to walk to the centre of a garden
     * @param minGap       the least distance between the centres of two gardens
     * @param placeable    is there floor at (x, z)? (the glue's test)
     */
    public static List<Garden> plan(double[][] foes, double cx, double cz, double fieldRadius, int count, double gardenRadius, double maxWalk, double minGap, Random rng,
                                    BiPredicate<Double, Double> placeable) {
        List<Garden> out = new ArrayList<>();
        double limit = Math.max(1.0, fieldRadius - gardenRadius * 0.4);
        // one garden within reach of every foe who is inside the field
        for (double[] f : foes) {
            if (out.size() >= MAX) break;
            if (Math.hypot(f[0] - cx, f[1] - cz) > fieldRadius) continue;                  // outside the field: nothing to be safe from
            if (reached(out, f, maxWalk)) continue;
            Garden g = null;
            for (int tries = 0; tries < 20 && g == null; tries++) {
                double a = rng.nextDouble() * Math.PI * 2, r = gardenRadius * 0.6 + rng.nextDouble() * Math.max(0.01, maxWalk * 0.8 - gardenRadius * 0.6);
                g = candidate(out, f[0] + Math.cos(a) * r, f[1] + Math.sin(a) * r, cx, cz, limit, minGap, placeable);
            }
            if (g == null) g = scan(out, f, cx, cz, limit, gardenRadius, maxWalk, minGap, placeable);                       // a systematic look when luck failed
            if (g == null) g = candidate(out, f[0], f[1], cx, cz, limit + gardenRadius, 0.0, placeable);                    // last resort: where the foe already stands
            if (g != null) out.add(g);
        }
        // then fill up to the wanted count anywhere in the field
        int guard = 0;
        while (out.size() < Math.min(MAX, Math.max(0, count)) && guard++ < 60) {
            double a = rng.nextDouble() * Math.PI * 2, r = Math.sqrt(rng.nextDouble()) * limit;
            Garden g = candidate(out, cx + Math.cos(a) * r, cz + Math.sin(a) * r, cx, cz, limit, minGap, placeable);
            if (g != null) out.add(g);
        }
        return out;
    }

    /** The nearest spot round the foe, looked for ring by ring and angle by angle (sixteen angles on three rings), that a garden can take. */
    private static Garden scan(List<Garden> have, double[] foe, double cx, double cz, double limit, double gardenRadius, double maxWalk, double minGap,
                               BiPredicate<Double, Double> placeable) {
        for (double r : new double[]{gardenRadius * 0.6, maxWalk * 0.5, maxWalk * 0.9}) {
            for (int k = 0; k < 16; k++) {
                double a = Math.PI * 2 * k / 16;
                Garden g = candidate(have, foe[0] + Math.cos(a) * r, foe[1] + Math.sin(a) * r, cx, cz, limit, minGap, placeable);
                if (g != null) return g;
            }
        }
        return null;
    }

    private static Garden candidate(List<Garden> have, double x, double z, double cx, double cz, double limit, double minGap, BiPredicate<Double, Double> placeable) {
        if (Math.hypot(x - cx, z - cz) > limit) return null;
        for (Garden g : have) if (Math.hypot(g.x() - x, g.z() - z) < minGap) return null;
        if (placeable != null && !placeable.test(x, z)) return null;
        return new Garden(x, z);
    }

    private static boolean reached(List<Garden> gardens, double[] foe, double maxWalk) {
        for (Garden g : gardens) if (Math.hypot(g.x() - foe[0], g.z() - foe[1]) <= maxWalk) return true;
        return false;
    }

    /** Is a body at (x, z) with half-width {@code halfWidth} safe: inside some garden (its centre within the garden's radius plus half the body)? */
    public static boolean isSafe(List<Garden> gardens, double x, double z, double gardenRadius, double halfWidth) {
        for (Garden g : gardens) if (Math.hypot(g.x() - x, g.z() - z) <= gardenRadius + halfWidth * 0.5) return true;
        return false;
    }

    /** The gardens a field of this many foes has: one for each, one over, never fewer than {@code floor} and never more than {@link #MAX}. */
    public static int gardenCount(int foes, int extra, int floor) { return Math.max(floor, Math.min(MAX, foes + extra)); }
}
