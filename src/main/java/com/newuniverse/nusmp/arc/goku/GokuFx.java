package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.perf.PerfBudgets;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: every particle of the Goku boss (the form aura, the power-up, the Kamehameha, the Spirit Bomb) goes through ONE place that takes its count from the shared per-tick particle budget
 * ({@code PerfBudgets.particles}), so many players and several effects at once can never flood the network (rule 11). Server side only: they are ordinary particle packets to the players near
 * the point, no client class is involved. Colours are 0xRRGGBB (the chart's form colours). Worst case per call: {@link #MAX_POINTS} particle packets, fewer when the budget is spent.
 */
final class GokuFx {
    private GokuFx() {}

    /** The most packets one call may send, whatever it asks for. */
    static final int MAX_POINTS = 24;

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    private static int take(ServerLevel sl, int wanted) { return PerfBudgets.particles(sl.getGameTime(), Math.max(0, Math.min(MAX_POINTS, wanted))); }

    /** The steady aura round the body: a cloud of dust in the form's colour and, for the lightning forms, a few sparks. One packet each, at most 8 particles. */
    static void aura(ServerLevel sl, LivingEntity e, int rgb, boolean electric) {
        int n = take(sl, 6);
        if (n > 0) sl.sendParticles(dust(rgb, 1.5f), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), n, e.getBbWidth() * 0.6, e.getBbHeight() * 0.45, e.getBbWidth() * 0.6, 0.01);
        if (electric) {
            int s = take(sl, 2);
            if (s > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), s, e.getBbWidth() * 0.7, e.getBbHeight() * 0.5, e.getBbWidth() * 0.7, 0.2);
        }
    }

    /** A flat ring of dust (the shock of a power-up, the ground mark of a Spirit Bomb). */
    static void ring(ServerLevel sl, Vec3 c, double radius, int rgb, int points) {
        int n = take(sl, points);
        if (n <= 0) return;
        DustParticleOptions d = dust(rgb, 1.4f);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(d, c.x + Math.cos(a) * radius, c.y + 0.1, c.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Motes gathering round a point (a power-up, a charge): one packet. */
    static void gather(ServerLevel sl, Vec3 at, double radius, int rgb, int count) {
        int n = take(sl, count);
        if (n > 0) sl.sendParticles(dust(rgb, 1.2f), at.x, at.y, at.z, n, radius, radius * 0.6, radius, 0.0);
    }

    /** A burst of light (a form taking hold, an impact): one packet. */
    static void burst(ServerLevel sl, Vec3 at, int rgb, int count) {
        int n = take(sl, count);
        if (n > 0) {
            sl.sendParticles(dust(rgb, 2.0f), at.x, at.y, at.z, n, 0.5, 0.5, 0.5, 0.2);
            Fx.spray(sl, FxThemes.WORLD, FxOld.END_ROD, at.x, at.y, at.z, Math.max(1, n / 3), 0.4, 0.4, 0.4, 0.15);
        }
    }

    /** A beam: up to {@link #MAX_POINTS} points along the line, two particles each. */
    static void beam(ServerLevel sl, Vec3 from, Vec3 to, int rgb) {
        int n = take(sl, MAX_POINTS);
        if (n <= 0) return;
        DustParticleOptions d = dust(rgb, 2.2f);
        Vec3 v = to.subtract(from);
        for (int i = 1; i <= n; i++) {
            Vec3 p = from.add(v.scale(i / (double) (n + 1)));
            sl.sendParticles(d, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.0);
        }
    }

    /** The silver flicker that tells the players his Ultra Instinct dodge is ready (and the afterimage when it fires). */
    static void shimmer(ServerLevel sl, LivingEntity e, int count) {
        int n = take(sl, count);
        if (n > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.END_ROD, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), n, 0.5, 0.8, 0.5, 0.05);
    }
}
