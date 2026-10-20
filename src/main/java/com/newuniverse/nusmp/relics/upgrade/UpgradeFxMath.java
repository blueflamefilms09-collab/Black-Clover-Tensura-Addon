package com.newuniverse.nusmp.relics.upgrade;

/**
 * The arithmetic of the ambient layers of an upgraded relic (1.102.0): where one particle of a layer appears, how many appear this tick, how much the distance and the effect quality
 * thin them. Pure, allocation free (results go into caller arrays), so every rule is a unit test and the client tick allocates nothing here.
 */
public final class UpgradeFxMath {
    private UpgradeFxMath() {}

    /** Within this many blocks of the camera every layer runs at full rate; the rate falls linearly to zero at {@link #FAR}. */
    public static final double NEAR = 12.0, FAR = 24.0;

    /** 1 up to {@link #NEAR} blocks, 0 from {@link #FAR}, linear between (a squared distance in, no square root unless needed). */
    public static float distanceFactor(double distSq) {        if (distSq <= NEAR * NEAR) return 1f;
        if (distSq >= FAR * FAR) return 0f;
        double d = Math.sqrt(distSq);
        return (float) ((FAR - d) / (FAR - NEAR));
    }

    /** The share of a layer's rate each effect quality keeps: low 0.3, medium 0.6, high 1 (the {@code VfxQuality} ordinal order: LOW, MEDIUM, HIGH). */
    public static float qualityFactor(int ordinal) {
        return switch (ordinal) {
            case 0 -> 0.3f;
            case 1 -> 0.6f;
            default -> 1.0f;
        };
    }

    /** How many particles a layer spawns this tick: its rate a tick times the factors, the whole part plus one more with the fractional chance {@code rnd} (0..1), capped by {@code maxPerTick}. */
    public static int emitCount(float perSecond, float distanceFactor, float qualityFactor, double rnd, int maxPerTick) {
        double expected = perSecond / 20.0 * distanceFactor * qualityFactor;
        if (expected <= 0.0) return 0;
        int whole = (int) expected;
        int n = whole + (rnd < expected - whole ? 1 : 0);
        return Math.min(Math.max(0, maxPerTick), n);
    }

    /**
     * Local position of one particle of a layer (x to the wearer's left, y up from the feet, z behind), written to {@code out[0..2]}.
     *
     * @param tick  the game time; {@code k} of {@code n}: this is the k-th particle of this tick (spreads ring points evenly); r1..r3: random numbers 0..1
     * @param speed the wearer's horizontal speed in blocks a tick (a {@code trail} only appears while moving)
     * @return false when the layer wants nothing this tick (a trail while standing still)
     */
    public static boolean point(UpgradeFxData.Layer l, long tick, int k, int n, double r1, double r2, double r3, double speed, double[] out) {
        double[] b = l.box(), c = l.center(), f = l.from(), t = l.to();
        switch (l.kind()) {
            case "motes" -> {
                out[0] = b[0] + (b[3] - b[0]) * r1;
                out[1] = b[1] + (b[4] - b[1]) * r2;
                out[2] = b[2] + (b[5] - b[2]) * r3;
            }
            case "ring", "orbit" -> {
                double a = 2 * Math.PI * (l.spin() * tick / 20.0 + (double) k / Math.max(1, n) + (l.kind().equals("orbit") ? r1 * 0.05 : 0.0));
                boolean vertical = "vertical".equals(l.plane());
                out[0] = c[0] + l.radius() * Math.cos(a);
                out[1] = vertical ? c[1] + l.radius() * Math.sin(a) : c[1] + (l.kind().equals("orbit") ? 0.25 * Math.sin(tick * 0.11 + k) : 0.0);
                out[2] = vertical ? c[2] : c[2] + l.radius() * Math.sin(a);
            }
            case "line" -> {
                out[0] = f[0] + (t[0] - f[0]) * r1 + (r2 - 0.5) * 0.06;
                out[1] = f[1] + (t[1] - f[1]) * r1;
                out[2] = f[2] + (t[2] - f[2]) * r1 + (r3 - 0.5) * 0.06;
            }
            case "trail" -> {
                if (speed < 0.06) return false;
                out[0] = c[0] + (r1 - 0.5) * 0.5;
                out[1] = c[1] + (r2 - 0.5) * (l.radius() > 0 ? l.radius() : 0.5);
                out[2] = c[2] + 0.2 + r3 * (0.6 + 6.0 * speed);
            }
            case "ground" -> {
                double a = 2 * Math.PI * r1;
                double rr = l.radius() * (0.55 + 0.45 * r2);
                out[0] = c[0] + rr * Math.cos(a);
                out[1] = 0.05;
                out[2] = c[2] + rr * Math.sin(a);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** World offsets of a local point for a wearer whose body faces {@code yawDeg} (Minecraft: 0 faces +z, the left hand is +x): writes dx, dy, dz to {@code out}. */
    public static void toWorld(double yawDeg, double lx, double ly, double lz, double[] out) {
        double y = Math.toRadians(yawDeg);
        double cos = Math.cos(y), sin = Math.sin(y);
        // forward = (-sin, cos); left = (cos, sin); back = -forward = (sin, -cos)
        out[0] = cos * lx + sin * lz;
        out[1] = ly;
        out[2] = sin * lx - cos * lz;
    }
}
