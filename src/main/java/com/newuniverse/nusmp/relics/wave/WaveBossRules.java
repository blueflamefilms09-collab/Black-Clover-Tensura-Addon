package com.newuniverse.nusmp.relics.wave;

/**
 * The boss relics' pure rules (no Minecraft types, unit tested): the shape of Dante's gravity well, the geometry of Zenon's twin gates, the
 * Mimic's bite target and the chance a boss drops its relic. The glue that plays them is {@link WaveBossActs} and {@link WaveBossEvents}.
 */
public final class WaveBossRules {
    private WaveBossRules() {}

    // ---------------------------------------------------------------- Dante's Gravity Crown: Weight of the Crown
    /** Blocks round the well in which foes are held. */
    public static final double WELL_RADIUS = 8.0;
    /** Ticks the well lasts (4 seconds) and how often it acts. */
    public static final int WELL_TICKS = 80, WELL_STEP = 4;
    /** The best a well can drag a foe towards its centre, in blocks per tick, and the horizontal speed it never exceeds. */
    public static final double PULL_MAX = 0.16, SPEED_CAP = 0.45;
    /** Foes closer than this to the centre are not pulled any more (nothing is squashed into the caster). */
    public static final double CORE = 1.2;

    /** How hard a foe at {@code dist} blocks is dragged inwards (blocks per tick): strongest at mid range, none at the rim or inside the core. */
    public static double pull(double dist, double radius) {
        if (radius <= 0 || dist >= radius || dist <= CORE) return 0.0;
        double f = dist / radius;
        double edge = 1.0 - f * f;                                  // fades to nothing at the rim
        double inner = Math.min(1.0, (dist - CORE) / 2.0);          // and eases off as it nears the core
        return PULL_MAX * edge * inner;
    }

    /** The Slowness amplifier at {@code dist} blocks: III at the heart, II halfway, I at the edge, none outside. */
    public static int slowAmp(double dist, double radius) {
        if (radius <= 0 || dist > radius) return -1;
        double f = dist / radius;
        return f < 0.4 ? 2 : f < 0.75 ? 1 : 0;
    }

    /** The sink speed (blocks per tick, negative) that keeps fliers and jumpers down, strongest at the heart. */
    public static double sink(double dist, double radius) {
        if (radius <= 0 || dist > radius) return 0.0;
        return -0.06 - 0.10 * (1.0 - dist / radius);
    }

    /** The damage of the crush that closes the well (a plain number; the relic engine caps it for players). */
    public static final float CRUSH = 4.0f;
    /** The well crushes only foes within this fraction of its radius. */
    public static final double CRUSH_FRACTION = 0.6;

    /** True on the step in which the well closes. */
    public static boolean closing(int age) { return age >= WELL_TICKS - WELL_STEP && age < WELL_TICKS; }

    // ---------------------------------------------------------------- Zenon's Spatial Ring: Rift Step and Twin Gates
    /** Rift Step: how far it tries, then the shorter tries when the longer finds no room. */
    public static final double[] STEP_TRIES = {10.0, 6.0, 3.0};
    /** Twin Gates: lifetime, how far ahead the first gate opens, the range of the second, and the radius of a gate. */
    public static final int GATES_TICKS = 300;
    public static final double GATE_NEAR = 2.5, GATE_FAR_MAX = 12.0, GATE_FAR_MIN = 4.0, GATE_RADIUS = 0.9, GATE_HEIGHT = 1.9;
    /** Ticks an entity must wait before a gate takes it again. */
    public static final int GATE_REENTRY = 30;

    /** A point {@code dist} blocks along the horizontal heading (dx, dz), which need not be a unit vector; falls back to +z for a zero heading. */
    public static double[] ahead(double x, double z, double dx, double dz, double dist) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-6) return new double[]{x, z + dist};
        return new double[]{x + dx / len * dist, z + dz / len * dist};
    }

    /** The distances to try for the far gate, nearest last: 12, 10, 8, 6, 4 blocks (the first one that has room wins). */
    public static double[] farTries() {
        int n = (int) Math.round((GATE_FAR_MAX - GATE_FAR_MIN) / 2.0) + 1;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = GATE_FAR_MAX - 2.0 * i;
        return out;
    }

    /** Is the point (ex, ey, ez) inside the gate standing at (gx, gy, gz)? */
    public static boolean inGate(double ex, double ey, double ez, double gx, double gy, double gz) {
        double dx = ex - gx, dz = ez - gz, dy = ey - gy;
        return dx * dx + dz * dz <= GATE_RADIUS * GATE_RADIUS && dy >= -0.6 && dy <= GATE_HEIGHT;
    }

    /** 0 = outside both gates, 1 = in the first, 2 = in the second. A thing in the first goes out of the second and the other way round. */
    public static int which(double ex, double ey, double ez, double[] a, double[] b) {
        if (inGate(ex, ey, ez, a[0], a[1], a[2])) return 1;
        if (inGate(ex, ey, ez, b[0], b[1], b[2])) return 2;
        return 0;
    }

    /** May this entity use a gate now? {@code lastUse} is the game time of its last use (or a very old time). */
    public static boolean reentryOk(long now, long lastUse) { return now - lastUse >= GATE_REENTRY || now < lastUse - 4000; }

    // ---------------------------------------------------------------- the Artifact Mimic: Bite
    /** Ticks from the cast to the snap (the jaw is open and the belt coils for this long: the telegraph), reach, minimum alignment with the look, damage. */
    public static final int BITE_DELAY = 12;
    public static final double BITE_REACH = 4.0, BITE_ALIGN = 0.35;
    public static final float BITE_DAMAGE = 6.0f;

    /**
     * Chooses the foe to bite: the nearest within {@link #BITE_REACH} whose direction lines up with the look by at least {@link #BITE_ALIGN}.
     * {@code rel} holds the vectors from the biter's eye to each candidate; (lx, ly, lz) is the unit look vector. Returns an index, or -1.
     */
    public static int pickTarget(double[][] rel, double lx, double ly, double lz) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < rel.length; i++) {
            double[] r = rel[i];
            double d = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
            if (d < 1.0e-6 || d > BITE_REACH) continue;
            double dot = (r[0] * lx + r[1] * ly + r[2] * lz) / d;
            if (dot < BITE_ALIGN) continue;
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** The speed (blocks per tick) of the lunge towards a foe {@code dist} blocks away: it ends about at the foe. */
    public static double lungeSpeed(double dist) { return Math.max(0.0, Math.min(0.9, dist * 0.32)); }

    // ---------------------------------------------------------------- where the relics come from
    /** The chance a Zogratis boss drops its relic when a player kills it: default, and the bounds a setting is kept in. */
    public static final double DROP_DEFAULT = 0.08, DROP_MAX = 1.0;

    public static double clampChance(double c) { return Double.isNaN(c) ? DROP_DEFAULT : Math.max(0.0, Math.min(DROP_MAX, c)); }

    /** {@code roll} is a uniform number in [0, 1). */
    public static boolean drops(double chance, double roll) { return roll < clampChance(chance); }
}
