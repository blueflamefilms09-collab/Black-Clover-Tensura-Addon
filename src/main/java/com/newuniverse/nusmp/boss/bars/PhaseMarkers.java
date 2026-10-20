package com.newuniverse.nusmp.boss.bars;

import java.util.List;

/**
 * Why: the phase notches on the bar and the phase number beside the name, as pure maths. A marker sits at the percent of health where the next phase begins; the notch is "passed" once the
 * health is at or below it; the current phase read from the health agrees with the boss's own count (the boss tells the real phase, this is what the bar shows between two packets).
 */
public final class PhaseMarkers {
    private PhaseMarkers() {}

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

    /** The phase (1-based) a health share is in, given the thresholds (percent of health at which phase 2, 3, ... begin). */
    public static int phaseAt(float hp, List<Integer> thresholds) {
        int phase = 1;
        for (int t : thresholds) if (hp * 100f <= t + 1e-4f) phase++;
        return phase;
    }

    /** The notch's pixel inside the window. */
    public static int markerX(int percent) { return BarLayoutMath.markerX(percent); }

    /** Has the health passed this notch? */
    public static boolean passed(float hp, int percent) { return hp * 100f <= percent + 1e-4f; }

    public static String roman(int n) { return ROMAN[Math.max(0, Math.min(ROMAN.length - 1, n))]; }

    /** Thresholds (percent) from the phase start shares of a profile (1.0, 0.66, 0.33 -> 66, 33): the first entry (the opening phase) is dropped. */
    public static List<Integer> fromShares(double... starts) {
        java.util.ArrayList<Integer> out = new java.util.ArrayList<>();
        for (int i = 1; i < starts.length; i++) out.add((int) Math.round(starts[i] * 100.0));
        return out;
    }
}
