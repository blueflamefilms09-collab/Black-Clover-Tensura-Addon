package com.newuniverse.nusmp.arc.spade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: Falling Heavens drops a rain of crushing debris on marked spots, and a rain is only fair if its marks are laid by rules the players can learn:
 * the first is on whoever he is looking at, the next on the others (so the whole party is asked something, not only the tank), the rest scattered round
 * the target, and no two so close that they merge into one unreadable blot. Dante's singularity planner caps at five marks; a rain wants a dozen, so
 * this is its own small, seeded, pure planner with the rules pinned by tests.
 */
public final class RainMath {
    private RainMath() {}

    /** The most marks one rain lays. */
    public static final int MAX_MARKS = 12;

    /**
     * Plans up to {@code n} marks (x, z): the first at the target (tx, tz); then one on each of the other foes {@code foes} (x, z pairs, in order) that is
     * not within {@code 1.4 * radius} of a mark already laid; then random spots within {@code spread} blocks of the target, tried up to 10 times each,
     * accepted when at least {@code 1.4 * radius} from every mark. A mark that cannot be placed is left out, so the result can be shorter than {@code n}.
     */
    public static List<double[]> plan(int n, double tx, double tz, double[][] foes, double radius, double spread, long seed) {
        List<double[]> out = new ArrayList<>();
        int want = Math.max(0, Math.min(MAX_MARKS, n));
        if (want == 0) return out;
        double apart = 1.4 * radius;
        out.add(new double[]{tx, tz});
        if (foes != null) {
            for (double[] f : foes) {
                if (out.size() >= want) break;
                if (f == null || f.length < 2 || tooClose(out, f[0], f[1], apart)) continue;
                out.add(new double[]{f[0], f[1]});
            }
        }
        Random rng = new Random(seed);
        int guard = 0;
        while (out.size() < want && guard++ < want * 10) {
            double a = rng.nextDouble() * Math.PI * 2, d = radius + rng.nextDouble() * Math.max(0.1, spread - radius);
            double x = tx + Math.cos(a) * d, z = tz + Math.sin(a) * d;
            if (!tooClose(out, x, z, apart)) out.add(new double[]{x, z});
        }
        return out;
    }

    private static boolean tooClose(List<double[]> marks, double x, double z, double min) {
        for (double[] m : marks) if (Math.hypot(m[0] - x, m[1] - z) < min) return true;
        return false;
    }

    /** Marks of a rain at a volley size 1..4: 6, 6, 8, 10, 12 across the five power steps (volley size 1 twice). */
    public static int marksFor(int castSlots) { return Math.max(4, Math.min(MAX_MARKS, 4 + 2 * Math.max(1, castSlots))); }
}
