package com.newuniverse.nusmp.thread;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the new Thread pages share small pieces of geometry and bookkeeping that are easy to get subtly wrong in the game loop (an
 * off-by-one in a trap line, a pull that overshoots, a drain that goes negative). They live here as plain arithmetic on doubles so the
 * tests can pin them down; {@link ThreadSpells} only feeds them positions.
 */
public final class ThreadMath {
    private ThreadMath() {}

    /** A trap line: 'count' points from 'start' along the unit direction (dx, dz), 'spacing' apart, the first one 'first' blocks out. */
    public static List<double[]> line(double sx, double sz, double dx, double dz, int count, double spacing, double first) {
        List<double[]> out = new ArrayList<>();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-6 || count <= 0) return out;
        double ux = dx / len, uz = dz / len;
        for (int i = 0; i < count; i++) out.add(new double[]{sx + ux * (first + i * spacing), sz + uz * (first + i * spacing)});
        return out;
    }

    /** A trap ring: 'count' points round (cx, cz) at 'radius', the first at angle 'phase' (radians). */
    public static List<double[]> ring(double cx, double cz, double radius, int count, double phase) {
        List<double[]> out = new ArrayList<>();
        if (count <= 0) return out;
        for (int i = 0; i < count; i++) {
            double a = phase + Math.PI * 2.0 * i / count;
            out.add(new double[]{cx + Math.cos(a) * radius, cz + Math.sin(a) * radius});
        }
        return out;
    }

    /** The radius at which 'count' nodes sit 'spacing' apart on a ring (never tighter than 1.5 blocks). */
    public static double ringRadius(int count, double spacing) {
        if (count <= 1) return 1.5;
        return Math.max(1.5, count * spacing / (Math.PI * 2.0));
    }

    /** True once a trap has outlived its time. */
    public static boolean trapExpired(int ageTicks, int lifeTicks) { return ageTicks >= lifeTicks; }

    /**
     * One step of a smooth pull: the velocity (blocks per tick) that carries an entity from 'pos' to 'dest' in 'ticksLeft' more ticks,
     * never faster than 'maxSpeed'. Done (zero) at the destination.
     */
    public static double[] pullVelocity(double[] pos, double[] dest, int ticksLeft, double maxSpeed) {
        double dx = dest[0] - pos[0], dy = dest[1] - pos[1], dz = dest[2] - pos[2];
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 0.25 || ticksLeft <= 0) return new double[]{0, 0, 0};
        double speed = Math.min(maxSpeed, d / ticksLeft);
        return new double[]{dx / d * speed, dy / d * speed, dz / d * speed};
    }

    /** The mana a toggle burns each second: a percentage of the maximum, never less than zero. */
    public static double reinforceDrain(double maxMana, double percentPerSecond) {
        return Math.max(0.0, maxMana) * Math.max(0.0, percentPerSecond) / 100.0;
    }

    /** Does the caster have the mana for another second of the skin? */
    public static boolean canKeepSkin(double mana, double drain) { return mana > 0.0 && mana >= drain; }

    /** The binding time of Arresting Dark Weave: the base time, but never beyond the clamp the balance law gave. */
    public static int bindTicks(int baseTicks, int clampedTicks) { return Math.max(1, Math.min(baseTicks, clampedTicks)); }
}
