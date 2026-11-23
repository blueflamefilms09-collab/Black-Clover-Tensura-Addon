package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
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
        if (GokuBossConfig.richFx()) auraRich(sl, e, rgb);
        int n = take(sl, 6);
        if (n > 0) sl.sendParticles(dust(rgb, 1.5f), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), n, e.getBbWidth() * 0.6, e.getBbHeight() * 0.45, e.getBbWidth() * 0.6, 0.01);
        if (electric) {
            int s = take(sl, 2);
            if (s > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), s, e.getBbWidth() * 0.7, e.getBbHeight() * 0.5, e.getBbWidth() * 0.7, 0.2);
        }
    }

    /** A flat ring of dust (the shock of a power-up, the ground mark of a Spirit Bomb). 4.107.0 (richFx): a second, inner ring of white-ish dust and a few rising motes. */
    static void ring(ServerLevel sl, Vec3 c, double radius, int rgb, int points) {
        if (GokuBossConfig.richFx() && radius > 1.0) ringInner(sl, c, radius, rgb, points);
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
        if (GokuBossConfig.richFx()) beamCore(sl, from, v, rgb);
    }

    /** The silver flicker that tells the players his Ultra Instinct dodge is ready (and the afterimage when it fires). */
    static void shimmer(ServerLevel sl, LivingEntity e, int count) {
        int n = take(sl, count);
        if (n > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.END_ROD, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), n, 0.5, 0.8, 0.5, 0.05);
    }

    // ------------------------------------------------------------------ 5.1.0: the Dragon Ball Ki layer (setting kiVfx). One effect packet per call through VfxSpawn (effect budget, players within 96 blocks).

    /** The Kamehameha: the Ki beam from his hands to where it ended (power = half-width). Once per shot. */
    static void kiBeam(ServerLevel sl, Vec3 from, Vec3 to, int rgb, float halfWidth) {
        if (GokuBossConfig.kiVfx()) VfxSpawn.send(sl, VfxShape.KI_BEAM, from, to, rgb, 30, Math.max(0.6f, Math.min(2f, halfWidth)));
    }

    /** The charge in his hands (re-sent every 10 ticks by the caller, 14 ticks long, follows him); {@code charge} 0..1. */
    static void kiCharge(ServerLevel sl, LivingEntity e, Vec3 hands, int rgb, float charge) {
        if (GokuBossConfig.kiVfx()) VfxSpawn.sendFollowing(sl, VfxShape.KI_CHARGE, e, hands, rgb, 14, Math.max(0f, Math.min(1f, charge)));
    }

    /** The Spirit Bomb gathering over his head (re-sent every 20 ticks by the caller, 24 ticks long); {@code radius} in blocks. */
    static void kiSphere(ServerLevel sl, LivingEntity e, Vec3 over, int rgb, float radius) {
        if (GokuBossConfig.kiVfx()) VfxSpawn.sendFollowing(sl, VfxShape.KI_SPHERE, e, over, rgb, 24, Math.max(0.5f, Math.min(8f, radius)));
    }

    /** A landing blast / the power-up shock at {@code at}; {@code radius} in blocks. */
    static void kiShock(ServerLevel sl, Vec3 at, int rgb, float radius) {
        if (GokuBossConfig.kiVfx()) VfxSpawn.send(sl, VfxShape.KI_SHOCKWAVE, at, at, rgb, 20, Math.max(1f, Math.min(12f, radius)));
    }

    /** A fast blow: a Rock punch flash from his fist to the foe. */
    static void kiStrike(ServerLevel sl, Vec3 from, Vec3 to, int rgb) {
        if (GokuBossConfig.kiVfx()) VfxSpawn.send(sl, VfxShape.KI_STRIKE, from, to, rgb, 12, 1f);
    }

    /** The dodge: he leaves an afterimage where he stood. */
    static void kiAfterimage(ServerLevel sl, Vec3 feet, Vec3 other, int rgb) {
        if (GokuBossConfig.kiVfx()) VfxSpawn.send(sl, VfxShape.KI_AFTERIMAGE, feet, other, rgb, 12, 1f);
    }

    // ------------------------------------------------------------------ 4.107.0: the richer ki visuals (setting richFx; each call still goes through the particle budget)

    private static int mix(int rgb, int toward, double t) {
        int r = (int) Math.round((rgb >> 16 & 255) * (1 - t) + (toward >> 16 & 255) * t), g = (int) Math.round((rgb >> 8 & 255) * (1 - t) + (toward >> 8 & 255) * t), b = (int) Math.round((rgb & 255) * (1 - t) + (toward & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** The layers of the aura round the body: a bright core, rising motes (ki flames) and a pulsing ground ring. At most about 14 packets per call (every fourth tick). */
    private static void auraRich(ServerLevel sl, LivingEntity e, int rgb) {
        double x = e.getX(), y = e.getY(), z = e.getZ(), h = e.getBbHeight(), w = e.getBbWidth();
        int core = take(sl, 4);
        if (core > 0) sl.sendParticles(dust(mix(rgb, 0xFFFFFF, 0.6), 0.9f), x, y + h * 0.5, z, core, w * 0.25, h * 0.35, w * 0.25, 0.0);
        int rise = take(sl, 4);
        if (rise > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.END_ROD, x, y + 0.2, z, rise, w * 0.7, 0.1, w * 0.7, 0.09);
        int flame = take(sl, 3);
        if (flame > 0) sl.sendParticles(dust(mix(rgb, 0xFFFFFF, 0.25), 1.8f), x, y + h * 1.05, z, flame, w * 0.35, 0.25, w * 0.35, 0.0);   // the hair of ki above the head
        double pulse = 0.9 + 0.35 * Math.sin(sl.getGameTime() * 0.35);
        ringPoints(sl, x, y + 0.08, z, pulse * w * 1.4, mix(rgb, 0xFFFFFF, 0.35), 6, 1.0f);
    }

    private static void ringPoints(ServerLevel sl, double cx, double cy, double cz, double radius, int rgb, int points, float size) {
        int n = take(sl, points);
        if (n <= 0) return;
        DustParticleOptions d = dust(rgb, size);
        double off = (sl.getGameTime() % 40) * 0.157;
        for (int i = 0; i < n; i++) {
            double a = off + i * Math.PI * 2 / n;
            sl.sendParticles(d, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void ringInner(ServerLevel sl, Vec3 c, double radius, int rgb, int points) {
        ringPoints(sl, c.x, c.y + 0.15, c.z, radius * 0.6, mix(rgb, 0xFFFFFF, 0.55), Math.max(4, points / 2), 1.1f);
        int up = take(sl, 3);
        if (up > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.END_ROD, c.x, c.y + 0.2, c.z, up, radius * 0.5, 0.1, radius * 0.5, 0.12);
    }

    /** The bright core of a beam: a thin white line half a step off the shell, with a few sparks along it. One packet per point, at most {@link #MAX_POINTS}/2 points. */
    private static void beamCore(ServerLevel sl, Vec3 from, Vec3 v, int rgb) {
        int n = take(sl, MAX_POINTS / 2);
        if (n <= 0) return;
        DustParticleOptions d = dust(mix(rgb, 0xFFFFFF, 0.8), 1.3f);
        for (int i = 0; i < n; i++) {
            Vec3 p = from.add(v.scale((i + 0.5) / n));
            sl.sendParticles(d, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        int sp = take(sl, 3);
        if (sp > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, from.x + v.x * 0.5, from.y + v.y * 0.5, from.z + v.z * 0.5, sp, Math.abs(v.x) * 0.4 + 0.2, Math.abs(v.y) * 0.4 + 0.2, Math.abs(v.z) * 0.4 + 0.2, 0.3);
    }

    /** A helix of ki rising round the body while he powers up: {@code turn} advances it (pass the countdown). Two strands, up to 12 packets. */
    static void helix(ServerLevel sl, Vec3 base, double height, double radius, int rgb, int turn) {
        if (!GokuBossConfig.richFx()) return;
        int n = take(sl, 12);
        if (n <= 0) return;
        DustParticleOptions d = dust(mix(rgb, 0xFFFFFF, 0.3), 1.5f);
        for (int i = 0; i < n; i++) {
            double t = (i + 0.5) / n, a = turn * 0.45 + t * Math.PI * 4 + (i % 2) * Math.PI;
            sl.sendParticles(d, base.x + Math.cos(a) * radius, base.y + t * height, base.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** The shock of a form taking hold: a pillar of light, a flash and a second expanding ring. About 20 packets once. */
    static void shockwave(ServerLevel sl, Vec3 at, int rgb, double radius) {
        if (!GokuBossConfig.richFx()) return;
        int n = take(sl, 10);
        if (n > 0) {
            DustParticleOptions d = dust(mix(rgb, 0xFFFFFF, 0.5), 2.4f);
            for (int i = 0; i < n; i++) sl.sendParticles(d, at.x, at.y + i * 1.1, at.z, 2, 0.25, 0.1, 0.25, 0.0);
        }
        int f = take(sl, 2);
        if (f > 0) {
            Fx.spray(sl, FxThemes.WORLD, FxOld.FLASH, at.x, at.y + 1.0, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            Fx.spray(sl, FxThemes.WORLD, FxOld.SONIC_BOOM, at.x, at.y + 0.3, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        ringPoints(sl, at.x, at.y + 0.1, at.z, radius * 1.3, mix(rgb, 0xFFFFFF, 0.2), 16, 1.6f);
    }

    /** The streak of a fast blow or dash: a short line of dust from where the fist started to where it landed. At most 8 packets. */
    static void trail(ServerLevel sl, Vec3 from, Vec3 to, int rgb) {
        if (!GokuBossConfig.richFx()) return;
        int n = take(sl, 8);
        if (n <= 0) return;
        DustParticleOptions d = dust(mix(rgb, 0xFFFFFF, 0.5), 1.2f);
        Vec3 v = to.subtract(from);
        for (int i = 0; i < n; i++) {
            Vec3 p = from.add(v.scale((i + 0.5) / n));
            sl.sendParticles(d, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    /** True damage landed: a white-blue flash on the victim (white dust, crit sparks, electric sparks). At most 12 packets, and the caller limits it to one per victim per second. */
    static void trueHit(ServerLevel sl, LivingEntity victim) {
        Vec3 c = victim.position().add(0, victim.getBbHeight() * 0.6, 0);
        int a = take(sl, 5);
        if (a > 0) sl.sendParticles(dust(0xFFFFFF, 1.7f), c.x, c.y, c.z, a, 0.3, 0.4, 0.3, 0.02);
        int b = take(sl, 5);
        if (b > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.CRIT, c.x, c.y, c.z, b, 0.35, 0.45, 0.35, 0.35);
        int e = take(sl, 2);
        if (e > 0) Fx.spray(sl, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, c.x, c.y, c.z, e, 0.3, 0.4, 0.3, 0.3);
    }
}
