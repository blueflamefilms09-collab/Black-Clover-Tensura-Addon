package com.newuniverse.nusmp.arc.early;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: Heath's Cage of Infinite Hail rains icicles on a whole disc from every direction, and a pattern that is only random can leave nowhere to stand,
 * which would make the move a coin toss instead of a lesson. This pure class lays the whole cage out in advance from a seed, in waves, with one rule that
 * is always true and is shown to the party in the wind-up: a corridor of {@code corridorHalf} blocks either side runs from the middle of the cage out through
 * its rim in the direction of the opening (a capsule: its middle end is round), and no icicle of any wave lands within its own radius of that corridor. So there is always a way through, the
 * opening faces Heath (the way out leads to him, which is also where his Crystal Ice Shield is weakest), and the same seed always gives the same cage
 * (so a test can prove the corridor is clear for hundreds of seeds, and the boss can show wave 1 before it falls).
 */
public final class HailPattern {
    private HailPattern() {}

    /** One icicle: the wave it belongs to (0 first) and where it lands. */
    public record Strike(int wave, double x, double z) {}

    /**
     * The cage.
     *
     * @param seed          the same seed always gives the same cage
     * @param radius        radius of the disc
     * @param waves         how many waves fall, one after the other
     * @param perWave       icicles per wave (fewer if the disc is too crowded to place them all)
     * @param impactRadius  radius of one icicle's landing
     * @param opening       direction of the open corridor, in radians from +x towards +z
     * @param corridorHalf  half width of the open corridor
     */
    public static List<Strike> plan(long seed, double cx, double cz, double radius, int waves, int perWave, double impactRadius, double opening, double corridorHalf) {
        Random rnd = new Random(seed);
        List<Strike> out = new ArrayList<>();
        double ox = Math.cos(opening), oz = Math.sin(opening);
        double spacing = impactRadius * 1.8;
        for (int w = 0; w < Math.max(0, waves); w++) {
            List<double[]> placed = new ArrayList<>();
            for (int n = 0; n < Math.max(0, perWave); n++) {
                for (int attempt = 0; attempt < 60; attempt++) {
                    double r = radius * Math.sqrt(rnd.nextDouble()), a = rnd.nextDouble() * Math.PI * 2;
                    double dx = Math.cos(a) * r, dz = Math.sin(a) * r;
                    if (!clearOfCorridor(dx, dz, ox * radius, oz * radius, impactRadius, corridorHalf)) continue;
                    boolean crowded = false;
                    for (double[] p : placed) if (Math.hypot(p[0] - dx, p[1] - dz) < spacing) { crowded = true; break; }
                    if (crowded) continue;
                    placed.add(new double[]{dx, dz});
                    out.add(new Strike(w, cx + dx, cz + dz));
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Is an icicle landing at the offset (dx, dz) from the middle clear of the open corridor? The corridor is the capsule round the axis from the middle to
     * (axisX, axisZ) (the rim, in the direction of the opening): a landing must be at least {@code corridorHalf + impactRadius} from that axis, so a foe
     * standing anywhere on it, even at the very middle, is out of reach of every icicle.
     */
    public static boolean clearOfCorridor(double dx, double dz, double axisX, double axisZ, double impactRadius, double corridorHalf) {
        return EarlyGeometry.segmentDistance(dx, dz, 0, 0, axisX, axisZ) >= corridorHalf + impactRadius;
    }

    /** The strikes of one wave. */
    public static List<Strike> wave(List<Strike> all, int wave) {
        List<Strike> out = new ArrayList<>();
        for (Strike s : all) if (s.wave() == wave) out.add(s);
        return out;
    }
}
