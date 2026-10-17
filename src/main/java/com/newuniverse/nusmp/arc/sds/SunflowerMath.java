package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.FourSinsMath;

/**
 * Why: Chastiefol's Sunflower is a flower that fires energy beams from its centre, and the way to make that a fight a player can read is the oldest one: the petals
 * are lanes that leave a point at even angles, the beams fire together, and the safe place is BETWEEN two petals. It is only fair if the gap is wide enough for a
 * body at a sensible distance from the centre, so that is checked here (not left to feel): the gap between two neighbouring lanes at a radius, and the nearest radius at
 * which a body fits. The centre itself is never safe (the lanes overlap there), which is the point: the party spreads out between the petals. Pure: positions are numbers.
 */
public final class SunflowerMath {
    private SunflowerMath() {}

    /** The bearings of {@code petals} lanes spaced evenly round the centre, the first at {@code phase} radians. */
    public static double[] angles(int petals, double phase) {
        int n = Math.max(1, petals);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = phase + Math.PI * 2 * i / n;
        return out;
    }

    /** Is the point (dx, dz), measured from the flower's centre, inside any petal: within {@code halfWidth} of a lane {@code length} blocks long? */
    public static boolean inPetal(double dx, double dz, int petals, double phase, double halfWidth, double length) {
        if (Math.hypot(dx, dz) < halfWidth) return true;
        for (double a : angles(petals, phase))
            if (FourSinsMath.distToSegment(dx, dz, 0, 0, Math.cos(a) * length, Math.sin(a) * length) <= halfWidth) return true;
        return false;
    }

    /** The free space between two neighbouring lanes at radius {@code r}: the arc between their axes minus the two half-widths (negative where they overlap). */
    public static double gap(int petals, double halfWidth, double r) { return Math.PI * 2 * r / Math.max(1, petals) - 2 * halfWidth; }

    /** The nearest radius at which a body {@code bodyWidth} wide fits between two lanes with {@code margin} blocks to spare. */
    public static double minSafeRadius(int petals, double halfWidth, double bodyWidth, double margin) {
        return (bodyWidth + margin + 2 * halfWidth) * Math.max(1, petals) / (Math.PI * 2);
    }
}
