package com.newuniverse.nusmp.entity.zogratis;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Server-side telegraph dust for the Zogratis fights (rings, lines, columns, gathering motes). Colours are 0xRRGGBB (see
 * {@link DevilStyle}). Everything goes out as ordinary particle packets to the players nearby, so no client code is involved.
 */
public final class Tells {
    private Tells() {}

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), size);
    }

    /** A flat ring of dust on the ground around 'center' (about 3 points per block of circumference). */
    public static void ring(ServerLevel sl, Vec3 center, double radius, int rgb) {
        ring(sl, center, radius, rgb, 1.3f, Math.max(12, Math.min(96, (int) (radius * 6))));
    }

    public static void ring(ServerLevel sl, Vec3 center, double radius, int rgb, float size, int points) {
        DustParticleOptions d = dust(rgb, size);
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            sl.sendParticles(d, center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** A straight line of dust (a bolt's path, a lance's aim). */
    public static void line(ServerLevel sl, Vec3 from, Vec3 to, int rgb) {
        DustParticleOptions d = dust(rgb, 1.0f);
        Vec3 v = to.subtract(from);
        int n = (int) Math.max(2, Math.min(64, v.length() * 1.5));
        for (int i = 1; i < n; i++) {
            Vec3 p = from.add(v.scale(i / (double) n));
            sl.sendParticles(d, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** A column of dust rising from 'base' (where a pillar or a strike will land). */
    public static void column(ServerLevel sl, Vec3 base, double height, int rgb) {
        DustParticleOptions d = dust(rgb, 1.2f);
        int n = (int) Math.max(3, Math.min(30, height * 2));
        for (int i = 0; i < n; i++) sl.sendParticles(d, base.x, base.y + height * i / n, base.z, 1, 0.15, 0, 0.15, 0);
    }

    /** Motes gathered around a point (a boss charging). */
    public static void gather(ServerLevel sl, Vec3 at, double radius, int rgb, int count) {
        sl.sendParticles(dust(rgb, 1.0f), at.x, at.y, at.z, count, radius, radius * 0.6, radius, 0);
    }

    /** A burst of motes (a release). */
    public static void burst(ServerLevel sl, Vec3 at, int rgb, int count) {
        sl.sendParticles(dust(rgb, 1.6f), at.x, at.y, at.z, count, 0.4, 0.4, 0.4, 0.15);
    }
}
