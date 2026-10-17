package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.util.Random;

/**
 * Why: the Hub's macro terrain (the plains' hills, the roughness of the Spire's shell, the strata and the caverns of the Chasm) needs smooth 3D
 * noise, and SIMPLEX noise is the better tool than classic Perlin here: it has no axis-aligned artefacts (Perlin's lattice shows as faint
 * "grid-lined" ridges, which on a tower or a pit wall looks like brickwork that was never asked for), and in 3D it blends 4 corners per sample
 * instead of 8, so the few million samples the chunk generator needs cost about half. The permutation table is shuffled from the world seed, and
 * every sample is offset by seed-derived amounts, so two seeds give unrelated worlds (plain simplex is exactly 0 at the lattice origin whatever the
 * permutation). Deterministic for a given seed on every JVM ({@link Random} is specified), immutable after construction and therefore safe to share
 * between the chunk generator's threads. Pure: no Minecraft import. Algorithm: Stefan Gustavson's public-domain "Simplex noise demystified".
 */
public final class Simplex3D {
    private static final int[][] GRAD3 = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0), G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
    private static final double F3 = 1.0 / 3.0, G3 = 1.0 / 6.0;

    private final short[] perm = new short[512];
    private final short[] permMod12 = new short[512];
    private final double offX, offY, offZ;

    public Simplex3D(long seed) {
        Random rnd = new Random(seed);
        short[] p = new short[256];
        for (short i = 0; i < 256; i++) p[i] = i;
        for (int i = 255; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            short t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
            permMod12[i] = (short) (perm[i] % 12);
        }
        offX = rnd.nextDouble() * 512.0;
        offY = rnd.nextDouble() * 512.0;
        offZ = rnd.nextDouble() * 512.0;
    }

    private static int floor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    private static double dot(int[] g, double x, double y, double z) { return g[0] * x + g[1] * y + g[2] * z; }

    /** 3D simplex noise, in [-1, 1]. */
    public double noise(double xin, double yin, double zin) {
        xin += offX;
        yin += offY;
        zin += offZ;
        double s = (xin + yin + zin) * F3;
        int i = floor(xin + s), j = floor(yin + s), k = floor(zin + s);
        double t = (i + j + k) * G3;
        double x0 = xin - (i - t), y0 = yin - (j - t), z0 = zin - (k - t);
        int i1, j1, k1, i2, j2, k2;
        if (x0 >= y0) {
            if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
            else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; }
            else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; }
        } else {
            if (y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1; }
            else if (x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1; }
            else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
        }
        double x1 = x0 - i1 + G3, y1 = y0 - j1 + G3, z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0 * G3, y2 = y0 - j2 + 2.0 * G3, z2 = z0 - k2 + 2.0 * G3;
        double x3 = x0 - 1.0 + 3.0 * G3, y3 = y0 - 1.0 + 3.0 * G3, z3 = z0 - 1.0 + 3.0 * G3;
        int ii = i & 255, jj = j & 255, kk = k & 255;
        int gi0 = permMod12[ii + perm[jj + perm[kk]]];
        int gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]];
        int gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]];
        int gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]];
        double n0, n1, n2, n3;
        double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
        if (t0 < 0) n0 = 0.0; else { t0 *= t0; n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0, z0); }
        double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
        if (t1 < 0) n1 = 0.0; else { t1 *= t1; n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1, z1); }
        double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
        if (t2 < 0) n2 = 0.0; else { t2 *= t2; n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2, z2); }
        double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
        if (t3 < 0) n3 = 0.0; else { t3 *= t3; n3 = t3 * t3 * dot(GRAD3[gi3], x3, y3, z3); }
        return clamp(32.0 * (n0 + n1 + n2 + n3));
    }

    /** 2D simplex noise (the plains are a heightfield), in [-1, 1]. */
    public double noise(double xin, double yin) {
        xin += offX;
        yin += offY;
        double s = (xin + yin) * F2;
        int i = floor(xin + s), j = floor(yin + s);
        double t = (i + j) * G2;
        double x0 = xin - (i - t), y0 = yin - (j - t);
        int i1, j1;
        if (x0 > y0) { i1 = 1; j1 = 0; } else { i1 = 0; j1 = 1; }
        double x1 = x0 - i1 + G2, y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2, y2 = y0 - 1.0 + 2.0 * G2;
        int ii = i & 255, jj = j & 255;
        int gi0 = permMod12[ii + perm[jj]];
        int gi1 = permMod12[ii + i1 + perm[jj + j1]];
        int gi2 = permMod12[ii + 1 + perm[jj + 1]];
        double n0, n1, n2;
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 < 0) n0 = 0.0; else { t0 *= t0; n0 = t0 * t0 * (GRAD3[gi0][0] * x0 + GRAD3[gi0][1] * y0); }
        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 < 0) n1 = 0.0; else { t1 *= t1; n1 = t1 * t1 * (GRAD3[gi1][0] * x1 + GRAD3[gi1][1] * y1); }
        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 < 0) n2 = 0.0; else { t2 *= t2; n2 = t2 * t2 * (GRAD3[gi2][0] * x2 + GRAD3[gi2][1] * y2); }
        return clamp(70.0 * (n0 + n1 + n2));
    }

    /**
     * Fractal sum of 3D octaves, normalised back into [-1, 1]: each octave multiplies the frequency by {@code lacunarity} and the amplitude by
     * {@code gain}. Why normalised: the callers think in "plus or minus this many blocks", not in "the sum of a geometric series".
     */
    public double fbm(double x, double y, double z, int octaves, double lacunarity, double gain) {
        double sum = 0, amp = 1, norm = 0, f = 1;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise(x * f, y * f, z * f);
            norm += amp;
            amp *= gain;
            f *= lacunarity;
        }
        return norm == 0 ? 0 : clamp(sum / norm);
    }

    /** The 2D fractal sum, normalised into [-1, 1]. */
    public double fbm(double x, double y, int octaves, double lacunarity, double gain) {
        double sum = 0, amp = 1, norm = 0, f = 1;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise(x * f, y * f);
            norm += amp;
            amp *= gain;
            f *= lacunarity;
        }
        return norm == 0 ? 0 : clamp(sum / norm);
    }

    private static double clamp(double v) { return v < -1.0 ? -1.0 : (v > 1.0 ? 1.0 : v); }
}
