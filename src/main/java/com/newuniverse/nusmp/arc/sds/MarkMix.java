package com.newuniverse.nusmp.arc.sds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: Phantom Garden and the Nightmare Teller are Gowther's illusions, and an illusion is only a fair fight when it can be read. The rule is: a ring is a REAL
 * strike or an ILLUSION that does nothing; the real rings stand steady and gold, the illusions flicker and are violet (the glue draws them so), and there are always
 * more illusions than real rings (the misdirection is the crowd, not a trick of colour). This pure planner decides where the marks go: for every foe one real ring on
 * their feet (the foe leaves it by moving, as with every other mark of the Sins) and more real rings near them, then two illusions for every real ring scattered
 * round, never closer than a minimum gap to any other mark, never more than the cap in all (the illusions are dropped first, a real ring never is). It uses a
 * seeded random so a test can repeat a plan.
 */
public final class MarkMix {
    private MarkMix() {}

    /** One ring on the ground: where it is and whether it will strike. */
    public record Mark(double x, double z, boolean real) {}

    /**
     * Plans the rings.
     *
     * @param foes        where the foes stand: {x, z} pairs
     * @param realPerFoe  real rings for every foe (at least 1: the first on their feet)
     * @param falsePerReal illusions for every real ring
     * @param spread      how far from a foe the extra real rings may fall
     * @param minGap      the least distance between two rings
     * @param cap         the most rings in all
     */
    public static List<Mark> plan(double[][] foes, int realPerFoe, int falsePerReal, double spread, double minGap, int cap, Random rng) {
        List<Mark> out = new ArrayList<>();
        int maxMarks = Math.max(1, cap);
        int reals = Math.max(1, realPerFoe);
        // the real rings first: a foe's own feet, then near them
        for (double[] f : foes) {
            if (out.size() >= maxMarks) break;
            out.add(new Mark(f[0], f[1], true));
        }
        for (int k = 1; k < reals; k++) {
            for (double[] f : foes) {
                if (countReal(out) >= maxMarks) break;
                Mark m = place(out, f, spread * 0.4, spread, minGap, rng, true);
                if (m != null) out.add(m);
            }
        }
        // then the illusions, round the foes, until the cap
        int wantFalse = countReal(out) * Math.max(0, falsePerReal);
        int made = 0, guard = 0;
        while (made < wantFalse && out.size() < maxMarks && guard++ < wantFalse * 8 + 16 && foes.length > 0) {
            double[] f = foes[rng.nextInt(foes.length)];
            Mark m = place(out, f, spread * 0.4, spread * 1.6, minGap, rng, false);
            if (m != null) {
                out.add(m);
                made++;
            }
        }
        return out;
    }

    private static Mark place(List<Mark> placed, double[] around, double minR, double maxR, double minGap, Random rng, boolean real) {
        for (int tries = 0; tries < 12; tries++) {
            double a = rng.nextDouble() * Math.PI * 2, r = minR + rng.nextDouble() * Math.max(0.01, maxR - minR);
            double x = around[0] + Math.cos(a) * r, z = around[1] + Math.sin(a) * r;
            boolean clear = true;
            for (Mark m : placed) if (Math.hypot(m.x() - x, m.z() - z) < minGap) { clear = false; break; }
            if (clear) return new Mark(x, z, real);
        }
        return null;
    }

    public static int countReal(List<Mark> marks) {
        int n = 0;
        for (Mark m : marks) if (m.real()) n++;
        return n;
    }

    public static int countFalse(List<Mark> marks) { return marks.size() - countReal(marks); }
}
