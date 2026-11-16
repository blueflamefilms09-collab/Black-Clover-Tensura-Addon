package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.RobloxMath;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The ten effects redrawn with the Roblox-style toolkit ({@link RobloxStyle}), each a few real meshes instead of a pile of camera-facing sprites (docs/vfx_roblox_pass.md). The old layer of each shape calls
 * {@link #draw} first and goes on with its own, unchanged drawing when it returns false (the client setting {@code vfx.robloxStyle} off, or a shape the toolkit does not own): the old effects stay, selectable.
 * <ul>
 *   <li>MAGIC_CIRCLE_EXPLOSION (the burst half, via {@link #circleBurst}): flash, two layered shock rings, a flip-book burst, a decal of the broken circle, a dozen stretched sparks.</li>
 *   <li>KI_BEAM (Kamehameha): scrolling-UV shell + crossed shell + mid + white-hot bolt core, two spiral arms, two rings running down it, a core + shell ball at the hands, flip-book impact.</li>
 *   <li>KI_SHOCKWAVE: flip-book flash, two layered ground shock rings, a dust ring, a rising swirl dome, a decal, ground rays.</li>
 *   <li>KI_BLAST: tapered trail + core + shell orb per shot, flip-book pop with two stretched sparks.</li>
 *   <li>LIGHTNING_SPEAR: jagged bolt as two ribbons (shell + hot core), a fork, a core + halo and flip-book at the end.</li>
 *   <li>EARTH_SPIKES: real 3D three-sided stone spikes (a cone mesh with the {@code rbx_rock} texture) erupting along the line, a decal and a glow at the head.</li>
 *   <li>ANTI_MAGIC_SLASH: a black swoosh crescent (two tapered ribbons from the tips to the belly) with a rim and a hot edge, two ghost crescents, smoke flip-books.</li>
 *   <li>WIND_TORNADO: two counter-turning twisting cones with a scrolling swirl texture, two spiral arms, a foot ring.</li>
 *   <li>ICE_FX3 (Shatter): flip-book flash, two layered shock rings, five ice-blade cones thrusting out, a frost decal, mist.</li>
 *   <li>WATER_BURST: two crown cones with a scrolling streak texture, two layered ripple rings, stretched droplets, a flip-book splash.</li>
 * </ul>
 * <b>Worst case (rule 11)</b>: {@link #MAX_QUADS} (summed from the primitives' fixed costs, at full detail; {@code ctx.seg} thins every ring, cone and ribbon by distance and by the particle setting), always under the
 * engine's own cap of 100 quads per effect, which {@link VfxVertexBuffer} enforces on top. No effect keeps any state or allocates per frame (static scratch of the render thread); every random is {@link RobloxMath#hash}
 * of the effect's seed, so every client sees the same picture.
 */
public final class RobloxFx {
    private RobloxFx() {}

    private static final float[] A = new float[3], B = new float[3], M = new float[3], S1 = new float[3], S2 = new float[3], R1 = new float[3], R2 = new float[3];
    private static final int WHITE = 0xFFFFFFFF, DUST = 0xFFC8BCA8, SMOKE = 0xFF0A0610, STONE = 0xFF9A8670, ICE = 0xFF9FE8FF;

    /** Worst-case quads per effect and the old effects' for comparison: the tables live in the pure {@link RobloxMath#MAX_QUADS} / {@link RobloxMath#OLD_QUADS} (pinned by RobloxStyleBudgetTest). */
    public static final int[] MAX_QUADS = RobloxMath.MAX_QUADS;

    /** true when the toolkit drew the effect (the old layer then draws nothing). */
    public static boolean draw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (!RobloxMath.enabled()) return false;
        switch (inst.shape) {
            case KI_BEAM -> kiBeam(inst, ctx, buf);
            case KI_SHOCKWAVE -> kiShockwave(inst, ctx, buf);
            case KI_BLAST -> kiBlast(inst, ctx, buf);
            case LIGHTNING_SPEAR -> lightning(inst, ctx, buf);
            case EARTH_SPIKES -> earthSpikes(inst, ctx, buf);
            case ANTI_MAGIC_SLASH -> antiSlash(inst, ctx, buf);
            case WIND_TORNADO -> tornado(inst, ctx, buf);
            case ICE_FX3 -> iceShatter(inst, ctx, buf);
            case WATER_BURST -> waterBurst(inst, ctx, buf);
            default -> { return false; }
        }
        return true;
    }

    // ------------------------------------------------------------------ helpers
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }

    private static int hot(int argb, float t) { return VfxVertexBuffer.whiten(argb, t); }

    private static void rel(VfxRenderContext ctx, Vec3 w, float[] o) {
        o[0] = (float) (w.x - ctx.camPos.x);
        o[1] = (float) (w.y - ctx.camPos.y);
        o[2] = (float) (w.z - ctx.camPos.z);
    }

    /** 1 while running, 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float x = in <= 0f ? 1f : Mth.clamp(age / in, 0f, 1f), y = out <= 0f ? 1f : Mth.clamp((inst.duration - age) / out, 0f, 1f);
        return Math.min(x, y);
    }

    private static float rnd(VfxInstance inst, int i, int k) { return RobloxMath.hash(inst.seed, i, k); }

    /** out = a unit vector, uniform on the sphere, from the effect's seed. */
    private static void randomDir(VfxInstance inst, int i, float[] o) {
        float u = rnd(inst, i, 11) * 2f - 1f, phi = rnd(inst, i, 12) * Mth.TWO_PI, s = Mth.sqrt(Math.max(0f, 1f - u * u));
        o[0] = s * Mth.cos(phi);
        o[1] = u;
        o[2] = s * Mth.sin(phi);
    }

    // ------------------------------------------------------------------ MAGIC_CIRCLE_EXPLOSION (burst half)
    /**
     * The burst of the grimoire explosion (b = 0..1 after the burst; the charging circle is still the old layer's). Origin and normal are the circle's pose. Returns false with the setting off.
     */
    public static boolean circleBurst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, org.joml.Vector3f origin, org.joml.Vector3f normal, float radius, float b) {
        if (!RobloxMath.enabled()) return false;
        float age = inst.ageTicks(ctx.partialTick), fade = 1f - b, P = inst.power;
        int col = inst.color, hotc = hot(col, 0.6f);
        float cx = origin.x + normal.x * 0.2f, cy = origin.y + normal.y * 0.2f, cz = origin.z + normal.z * 0.2f;
        RobloxStyle.coreShell(buf, ctx, cx, cy, cz, radius * 1.1f * (1f + b), col, hotc, fade * 1.3f, false);
        RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_BURST, VfxBlend.ADD, cx, cy, cz, radius * 3.4f, (float) inst.seed, RobloxMath.phase(b, 0f, 0.8f), true, a(hotc, 0.95f));
        RobloxStyle.shock(buf, origin.x, origin.y, origin.z, normal.x, normal.y, normal.z, b, radius * 4.2f, radius * 0.7f, 2, ctx.seg(18, 8), hot(col, 0.4f), 1f);
        RobloxStyle.decal(buf, origin.x + normal.x * b * 0.6f, origin.y + normal.y * b * 0.6f, origin.z + normal.z * b * 0.6f, normal.x, normal.y, normal.z, radius * (1f + b * 0.8f), age * 0.2f, a(col, fade * 0.7f));
        int n = ctx.seg(12, 6);
        float e = RobloxMath.easeOut(b);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + rnd(inst, i, 1) * 0.3f, tilt = (rnd(inst, i, 2) - 0.3f) * 1.0f;
            RobloxStyle.planeDir(normal.x, normal.y, normal.z, ang, R1);
            float dx = R1[0] + normal.x * tilt, dy = R1[1] + normal.y * tilt, dz = R1[2] + normal.z * tilt, dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float dist = radius * (0.6f + e * (2.4f + 2.4f * rnd(inst, i, 3)));
            RobloxStyle.streak(buf, origin.x + dx * dist, origin.y + dy * dist, origin.z + dz * dist, dx, dy, dz, radius * (0.9f - 0.5f * b), 0.3f * P * (1f - b * 0.6f), a(hot(col, rnd(inst, i, 4) * 0.5f), fade));
        }
        return true;
    }

    // ------------------------------------------------------------------ KI_BEAM
    private static void kiBeam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color, hotc = hot(col, 0.85f);
        rel(ctx, inst.from(ctx), A);
        rel(ctx, inst.to(ctx), B);
        float dx = B[0] - A[0], dy = B[1] - A[1], dz = B[2] - A[2], len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05f) return;
        dx /= len; dy /= len; dz /= len;
        float extend = RobloxMath.easeOut(age / 3f), fade = life(inst, age, 0f, 8f);
        float w = inst.power * (0.35f + 0.65f * fade) * (1f + 0.06f * Mth.sin(age * 2.3f));
        float L = len * extend, hx = A[0] + dx * L, hy = A[1] + dy * L, hz = A[2] + dz * L;
        RobloxStyle.coreShell(buf, ctx, A[0], A[1], A[2], w * 2.4f, col, hotc, fade, true);
        if (L > 0.05f) {
            float vs = -age * 0.35f, vr = Math.max(1f, L / 3f);
            // the crossed second plane: contains the beam, perpendicular to the camera-facing one
            float mx = (A[0] + hx) * 0.5f, my = (A[1] + hy) * 0.5f, mz = (A[2] + hz) * 0.5f;
            S1[0] = dy * mz - dz * my; S1[1] = dz * mx - dx * mz; S1[2] = dx * my - dy * mx;
            float sl = Mth.sqrt(S1[0] * S1[0] + S1[1] * S1[1] + S1[2] * S1[2]);
            if (sl < 1e-6f) { S1[0] = ctx.camRight.x; S1[1] = ctx.camRight.y; S1[2] = ctx.camRight.z; sl = 1f; }
            S1[0] /= sl; S1[1] /= sl; S1[2] /= sl;
            S2[0] = dy * S1[2] - dz * S1[1]; S2[1] = dz * S1[0] - dx * S1[2]; S2[2] = dx * S1[1] - dy * S1[0];
            RobloxStyle.line(2, A[0], A[1], A[2], hx, hy, hz);
            RobloxStyle.swoosh(buf, ctx, RobloxStyle.SWOOSH, VfxBlend.ADD, RobloxStyle.PATH, 2, w * 2.4f, w * 2.0f, 1f, vs, vs + vr, a(col, 0.55f * fade), a(col, 0.55f * fade));
            RobloxStyle.sideRibbon(buf, RobloxStyle.SWOOSH, VfxBlend.ADD, A[0], A[1], A[2], hx, hy, hz, S2[0], S2[1], S2[2], w * 2.4f, w * 2.0f, vs * 0.8f, vs * 0.8f + vr, a(col, 0.4f * fade), a(col, 0.4f * fade));
            RobloxStyle.swoosh(buf, ctx, RobloxStyle.SWOOSH, VfxBlend.ADD, RobloxStyle.PATH, 2, w * 1.5f, w * 1.3f, 1f, vs * 1.7f, vs * 1.7f + vr, a(hot(col, 0.5f), 0.8f * fade), a(hot(col, 0.5f), 0.8f * fade));
            RobloxStyle.swoosh(buf, ctx, RobloxStyle.BOLT, VfxBlend.ADD, RobloxStyle.PATH, 2, w * 0.85f, w * 0.7f, 1f, vs * 2.5f, vs * 2.5f + vr, a(WHITE, fade), a(WHITE, fade));
            // two spiral arms winding round it, flowing out
            int n = ctx.seg(8, 4);
            float turns = Mth.clamp(L / 5f, 0.5f, 3f);
            for (int k = 0; k < 2; k++) {
                RobloxStyle.spiral(n, A[0], A[1], A[2], hx, hy, hz, w * 1.35f, w * 1.35f, turns, age * 0.45f + k * Mth.PI);
                RobloxStyle.swoosh(buf, ctx, RobloxStyle.SWOOSH, VfxBlend.ADD, RobloxStyle.PATH, n, w * 0.45f, w * 0.28f, 1f, vs * 1.3f, vs * 1.3f + vr, a(hotc, 0.85f * fade), a(col, 0.2f * fade));
            }
            // two rings running down it
            int rs = ctx.seg(12, 6);
            for (int k = 0; k < 2; k++) {
                float ph = Mth.frac(age * 0.09f + k * 0.5f);
                RobloxStyle.ring(buf, RobloxStyle.RING, VfxBlend.ADD, A[0] + dx * L * ph, A[1] + dy * L * ph, A[2] + dz * L * ph, dx, dy, dz, w * 1.9f, w * 0.7f, rs, 2, age * 0.05f, a(hot(col, 0.4f), 0.55f * (1f - ph) * fade));
            }
        }
        if (extend < 1f) RobloxStyle.coreShell(buf, ctx, hx, hy, hz, w * 2.2f, col, hotc, 1f, true);
        if (age >= 3f) {                                                                         // the impact at the end
            float ia = age - 3f, span = Math.max(4f, inst.duration - 3f), f = 1f - ia / span;
            RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_BURST, VfxBlend.ADD, B[0], B[1], B[2], w * (4.2f + 0.6f * Mth.sin(age * 1.9f)), age * 0.2f, RobloxMath.phase(ia, 0f, 10f), true, a(hotc, f));
            RobloxStyle.coreShell(buf, ctx, B[0], B[1], B[2], w * 3f, col, hotc, 0.7f * f, false);
            float rr = Math.min(w * 6f, w * (1.4f + ia * 0.45f));
            RobloxStyle.ring(buf, RobloxStyle.RING, VfxBlend.ADD, B[0], B[1], B[2], dx, dy, dz, rr, rr * 0.3f, ctx.seg(12, 6), 3, 0f, a(hot(col, 0.5f), 0.8f * f));
            for (int i = 0; i < 4; i++) {
                randomDir(inst, i, R2);
                if (R2[0] * dx + R2[1] * dy + R2[2] * dz > 0f) { R2[0] = -R2[0]; R2[1] = -R2[1]; R2[2] = -R2[2]; }       // sparks fly back off the surface the beam hits
                float ph = Mth.frac(ia * 0.12f + rnd(inst, i, 5)), d = w * (0.5f + 3f * ph);
                RobloxStyle.streak(buf, B[0] + R2[0] * d, B[1] + R2[1] * d, B[2] + R2[2] * d, R2[0], R2[1], R2[2], w * 0.9f, w * 0.18f, a(hot(col, 0.7f), (1f - ph) * f));
            }
        }
    }

    // ------------------------------------------------------------------ KI_SHOCKWAVE
    private static void kiShockwave(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power), f = 1f - Mth.clamp(age / inst.duration, 0f, 1f);
        int col = inst.color, hotc = hot(col, 0.8f);
        rel(ctx, inst.from(ctx), A);
        float cx = A[0], cy = A[1], cz = A[2];
        if (age < 8f) {
            RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_BURST, VfxBlend.ADD, cx, cy + 0.6f, cz, R * 1.1f, age * 0.2f, age / 8f, true, a(hotc, 1f - age / 10f));
            RobloxStyle.coreShell(buf, ctx, cx, cy + 0.6f, cz, R * 0.5f, col, hotc, 0.8f * (1f - age / 8f), false);
        }
        float t = Mth.clamp(age / 14f, 0f, 1f);
        RobloxStyle.shock(buf, cx, cy + 0.08f, cz, 0f, 1f, 0f, t, R, R * 0.35f, 2, ctx.seg(16, 8), hot(col, 0.4f), f);
        float rd = R * 0.92f * RobloxMath.easeOut(age / 16f);
        RobloxStyle.ring(buf, RobloxStyle.RING, VfxBlend.ALPHA, cx, cy + 0.11f, cz, 0f, 1f, 0f, rd, R * 0.4f, ctx.seg(12, 8), 4, 0f, a(DUST, 0.55f * f));
        if (age < 12f) {                                                                         // a swirl dome of energy rising off the ground
            float e = RobloxMath.easeOut(age / 9f), k = 1f - age / 12f;
            RobloxStyle.cone(buf, RobloxStyle.SWIRL, VfxBlend.ADD, cx, cy + 0.1f, cz, 0f, 1f, 0f, R * 0.55f * e, R * 0.2f * e, R * 0.5f * e, 8, 1, 0.1f, age * 0.3f, 0.6f, 2f, 0f, -age * 0.08f, -age * 0.08f + 1f,
                    a(hot(col, 0.5f), 0.7f * k), a(col, 0.1f * k));
        }
        RobloxStyle.decal(buf, cx, cy + 0.05f, cz, 0f, 1f, 0f, R * 0.7f * RobloxMath.easeOut(age / 10f), age * 0.1f, a(col, 0.6f * f));
        if (age < 10f) {
            float g = age / 10f;
            for (int i = 0; i < ctx.seg(6, 3); i++) {
                float ang = rnd(inst, i, 6) * Mth.TWO_PI, ca = Mth.cos(ang), sa = Mth.sin(ang), d = R * (0.3f + 0.7f * g);
                RobloxStyle.streak(buf, cx + ca * d, cy + 0.12f, cz + sa * d, ca, 0f, sa, R * 0.35f, 0.3f, a(hot(col, 0.6f), 1f - g));
            }
        }
    }

    // ------------------------------------------------------------------ KI_BLAST
    private static void kiBlast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int col = inst.color, hotc = hot(col, 0.85f), n = Mth.clamp(Math.round(inst.power), 1, 8);
        rel(ctx, inst.from(ctx), A);
        rel(ctx, inst.to(ctx), B);
        float len = Mth.sqrt((B[0] - A[0]) * (B[0] - A[0]) + (B[1] - A[1]) * (B[1] - A[1]) + (B[2] - A[2]) * (B[2] - A[2]));
        float travel = Mth.clamp(len / 6f, 2f, 6f), spread = n > 1 ? Math.min(1.4f, 0.1f * len) : 0f;
        if (age < 3f) RobloxStyle.coreShell(buf, ctx, A[0], A[1], A[2], 0.9f, col, hotc, 1f - age / 3f, false);
        for (int i = 0; i < n; i++) {
            float ex = B[0] + (rnd(inst, i, 1) - 0.5f) * 2f * spread, ey = B[1] + (rnd(inst, i, 2) - 0.5f) * 1.2f * spread, ez = B[2] + (rnd(inst, i, 3) - 0.5f) * 2f * spread;
            if (i == 0) { ex = B[0]; ey = B[1]; ez = B[2]; }
            float t = (age - i * 1.2f) / travel;
            if (t < 0f) continue;
            if (t <= 1f) {
                float pxx = A[0] + (ex - A[0]) * t, pyy = A[1] + (ey - A[1]) * t, pzz = A[2] + (ez - A[2]) * t, t0 = Math.max(0f, t - 0.4f);
                float txx = A[0] + (ex - A[0]) * t0, tyy = A[1] + (ey - A[1]) * t0, tzz = A[2] + (ez - A[2]) * t0;
                RobloxStyle.line(3, txx, tyy, tzz, pxx, pyy, pzz);
                RobloxStyle.swoosh(buf, ctx, RobloxStyle.SWOOSH, VfxBlend.ADD, RobloxStyle.PATH, 3, 0.02f, 0.34f, 1.3f, -age * 0.5f, -age * 0.5f + 1f, a(col, 0.1f), a(hot(col, 0.3f), 0.9f));
                RobloxStyle.coreShell(buf, ctx, pxx, pyy, pzz, 0.5f, col, hotc, 1f, false);
            } else {
                float p = (age - i * 1.2f - travel) / 5f;
                if (p >= 1f) continue;
                float f = 1f - p;
                RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_BURST, VfxBlend.ADD, ex, ey, ez, 1.7f + 2.2f * p, rnd(inst, i, 4) * Mth.TWO_PI, p, false, a(hotc, f));
                for (int k = 0; k < 2; k++) {
                    randomDir(inst, i * 2 + k, R2);
                    float d = 0.3f + 1.1f * p;
                    RobloxStyle.streak(buf, ex + R2[0] * d, ey + R2[1] * d, ez + R2[2] * d, R2[0], R2[1], R2[2], 0.5f, 0.12f, a(hot(col, 0.6f), f));
                }
            }
        }
    }

    // ------------------------------------------------------------------ LIGHTNING_SPEAR
    private static void lightning(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), alpha = 1f - RobloxMath.easeIn(t), P = inst.power;
        int col = inst.color, core = hot(col, 0.8f);
        rel(ctx, inst.from(ctx), A);
        rel(ctx, inst.to(ctx), B);
        float dist = Mth.sqrt((B[0] - A[0]) * (B[0] - A[0]) + (B[1] - A[1]) * (B[1] - A[1]) + (B[2] - A[2]) * (B[2] - A[2]));
        if (dist < 0.05f) return;
        int n = ctx.seg(6, 4), step = inst.age() / 2;                                              // the zig-zag is re-rolled every 2 ticks
        RobloxStyle.jagged(n, A[0], A[1], A[2], B[0], B[1], B[2], dist * 0.05f, inst.seed, step);
        int mid = n / 2;
        M[0] = RobloxStyle.PATH[mid * 3]; M[1] = RobloxStyle.PATH[mid * 3 + 1]; M[2] = RobloxStyle.PATH[mid * 3 + 2];
        RobloxStyle.swoosh(buf, ctx, RobloxStyle.BOLT, VfxBlend.ADD, RobloxStyle.PATH, n, 0.45f * P, 0.45f * P, 1f, 0f, dist / 3f, a(col, alpha), a(col, alpha));
        RobloxStyle.swoosh(buf, ctx, RobloxStyle.BOLT, VfxBlend.ADD, RobloxStyle.PATH, n, 0.16f * P, 0.16f * P, 1f, 0f, dist / 3f, a(core, alpha), a(core, alpha));
        float fx = (B[0] - A[0]) / dist, fy = (B[1] - A[1]) / dist, fz = (B[2] - A[2]) / dist;       // a fork leaving the middle of the bolt
        randomDir(inst, step, R1);
        RobloxStyle.jagged(3, M[0], M[1], M[2], M[0] + (R1[0] * 0.6f + fx * 0.5f) * dist * 0.18f, M[1] + (R1[1] * 0.6f + fy * 0.5f) * dist * 0.18f, M[2] + (R1[2] * 0.6f + fz * 0.5f) * dist * 0.18f, dist * 0.02f, inst.seed, step + 5);
        RobloxStyle.swoosh(buf, ctx, RobloxStyle.BOLT, VfxBlend.ADD, RobloxStyle.PATH, 3, 0.22f * P, 0.05f * P, 1f, 0f, 1f, a(col, 0.8f * alpha), a(col, 0.4f * alpha));
        RobloxStyle.coreShell(buf, ctx, B[0], B[1], B[2], 0.7f * P, col, core, alpha, false);
        RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_BURST, VfxBlend.ADD, B[0], B[1], B[2], 1.6f * P, inst.seed, RobloxMath.phase(t, 0f, 0.55f), false, a(core, alpha));
        for (int i = 0; i < 2; i++) {
            randomDir(inst, i + 20, R2);
            float d = P * (0.3f + 0.9f * RobloxMath.easeOut(t));
            RobloxStyle.streak(buf, B[0] + R2[0] * d, B[1] + R2[1] * d, B[2] + R2[2] * d, R2[0], R2[1], R2[2], 0.5f * P, 0.1f * P, a(core, alpha));
        }
    }

    // ------------------------------------------------------------------ EARTH_SPIKES
    private static void earthSpikes(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), P = inst.power;
        int col = inst.color;
        rel(ctx, inst.from(ctx), A);
        rel(ctx, inst.to(ctx), B);
        int n = ctx.seg(6, 4);
        for (int i = 0; i < n; i++) {
            float at = (float) i / Math.max(1, n - 1), local = Mth.clamp(t * 2.2f - at, 0f, 1f);
            if (local <= 0f) continue;
            float rise = local < 0.5f ? RobloxMath.easeOutBack(local * 2f) : 1f - (local - 0.5f) * 2f;
            float bx = A[0] + (B[0] - A[0]) * at + (rnd(inst, i, 1) - 0.5f) * 0.6f, by = A[1] + (B[1] - A[1]) * at, bz = A[2] + (B[2] - A[2]) * at + (rnd(inst, i, 2) - 0.5f) * 0.6f;
            float h = (1.2f + rnd(inst, i, 3)) * P * Math.max(0.01f, rise);
            RobloxStyle.cone(buf, RobloxStyle.ROCK, VfxBlend.ALPHA, bx, by, bz, 0f, 1f, 0f, 0.4f * P, 0.02f * P, h, 3, 1, 0f, rnd(inst, i, 4) * Mth.TWO_PI, 0f, 1f, 0f, 0f, 1f,
                    a(hot(STONE, 0.0f), 1f), a(hot(col, 0.4f), 1f));
        }
        float head = Math.min(1f, t * 2f);
        float hx = A[0] + (B[0] - A[0]) * head, hy = A[1] + (B[1] - A[1]) * head, hz = A[2] + (B[2] - A[2]) * head;
        RobloxStyle.decal(buf, hx, hy + 0.05f, hz, 0f, 1f, 0f, 0.9f * P, t * 2f, a(col, 0.55f * (1f - t)));
        RobloxStyle.coreShell(buf, ctx, hx, hy + 0.1f, hz, 0.6f * P, col, hot(col, 0.6f), 1f - t, false);
        if (t < 0.6f) RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_FIRE, VfxBlend.ALPHA, hx, hy + 0.35f * P, hz, 1.4f * P, rnd(inst, 9, 9) * Mth.TWO_PI, t / 0.6f, false, a(DUST, 0.55f * (1f - t / 0.6f)));
    }

    // ------------------------------------------------------------------ ANTI_MAGIC_SLASH
    /** The sword's colour pushed up to a visible rim brightness (demon swords pass a near-black violet): a copy of AntiMagicLayer.brighten. */
    private static int brighten(int argb) {
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255, m = Math.max(1, Math.max(r, Math.max(g, b)));
        float k = Math.max(1f, 230f / m);
        return 0xFF000000 | Math.min(255, (int) (r * k)) << 16 | Math.min(255, (int) (g * k)) << 8 | Math.min(255, (int) (b * k));
    }

    /** A crescent: two ribbons from the two tips (width 0) to the belly at angle {@code ang}. 2 x (m - 1) quads. */
    private static void crescent(VfxVertexBuffer buf, VfxRenderContext ctx, VfxBlend blend, float cx, float cy, float cz, float nx, float ny, float nz, float radius, float ang, float half, int m,
                                 float bellyW, float v0, int argb) {
        for (int side = 0; side < 2; side++) {
            RobloxStyle.arcPath(m, cx, cy, cz, nx, ny, nz, radius, side == 0 ? ang - half : ang + half, side == 0 ? half : -half);
            RobloxStyle.swoosh(buf, ctx, RobloxStyle.SWOOSH, blend, RobloxStyle.PATH, m, 0f, bellyW, 1.2f, v0, v0 + 1.5f, a(argb, 0.15f), argb);
        }
    }

    private static void antiSlash(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), P = inst.power;
        rel(ctx, inst.from(ctx), A);
        rel(ctx, inst.to(ctx), B);
        float dx = B[0] - A[0], dy = B[1] - A[1], dz = B[2] - A[2], len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.1f) return;
        dx /= len; dy /= len; dz /= len;
        float nx, ny, nz;                                                                        // the blade plane contains the flight direction (the old layer's rule)
        if (Math.abs(dy) > 0.95f) { nx = 1f; ny = 0f; nz = 0f; }
        else { nx = -dz; ny = 0f; nz = dx; float nl = Mth.sqrt(nx * nx + nz * nz); nx /= nl; nz /= nl; }
        int rim = hot(brighten(inst.color), 0.2f), hotc = hot(brighten(inst.color), 0.7f);
        float alpha = VfxAnim.fadeInOut(t, 0.05f, 0.35f), r = 1.6f * P;
        int m = ctx.seg(5, 3);
        float ang = RobloxStyle.angleIn(nx, ny, nz, dx, dy, dz);
        for (int k = 2; k >= 0; k--) {
            float h = VfxAnim.slashHead(Math.max(0f, t - k * 0.06f));
            float cx = A[0] + (B[0] - A[0]) * h, cy = A[1] + (B[1] - A[1]) * h, cz = A[2] + (B[2] - A[2]) * h, ak = alpha * (k == 0 ? 1f : 0.35f / k);
            if (k == 0) {
                crescent(buf, ctx, VfxBlend.ALPHA, cx, cy, cz, nx, ny, nz, r, ang, 1.2f, m, 1.7f * P, -age * 0.15f, a(0xFF050207, 0.9f * ak));
                crescent(buf, ctx, VfxBlend.ADD, cx, cy, cz, nx, ny, nz, r * 1.06f, ang, 1.2f, m, 0.5f * P, -age * 0.2f, a(rim, ak));
                crescent(buf, ctx, VfxBlend.ADD, cx, cy, cz, nx, ny, nz, r * 1.08f, ang, 1.1f, m, 0.14f * P, -age * 0.25f, a(hotc, ak));
            } else {
                crescent(buf, ctx, VfxBlend.ADD, cx, cy, cz, nx, ny, nz, r * 1.04f, ang, 1.2f, Math.max(3, m - 1), 0.5f * P, -age * 0.15f, a(rim, ak));
            }
        }
        float hh = VfxAnim.slashHead(t), hx = A[0] + (B[0] - A[0]) * hh, hy = A[1] + (B[1] - A[1]) * hh, hz = A[2] + (B[2] - A[2]) * hh;
        RobloxStyle.streak(buf, hx, hy, hz, dx, dy, dz, Math.max(0.5f, len * hh), 0.14f * P, a(rim, 0.6f * alpha));
        for (int k = 0; k < 4; k++) {                                                              // torn black smoke
            float born = rnd(inst, k, 1) * 0.5f;
            if (t < born) continue;
            float u = Math.min(1f, (t - born) / 0.5f), bh = VfxAnim.slashHead(born), e = RobloxMath.easeOut(u);
            randomDir(inst, k, R1);
            float d = r * (0.6f + 1.4f * e);
            RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_FIRE, VfxBlend.ALPHA, A[0] + (B[0] - A[0]) * bh + R1[0] * d, A[1] + (B[1] - A[1]) * bh + R1[1] * d * 0.6f - 0.6f * u * u, A[2] + (B[2] - A[2]) * bh + R1[2] * d,
                    (0.9f + 0.6f * u) * P, age * 0.1f + k, u, false, a(SMOKE, 0.75f * (1f - u)));
        }
        for (int k = 0; k < 3; k++) {
            float born = rnd(inst, k + 8, 1) * 0.5f;
            if (t < born) continue;
            float u = Math.min(1f, (t - born) / 0.5f);
            randomDir(inst, k + 8, R2);
            float d = r * (0.5f + 1.2f * RobloxMath.easeOut(u));
            RobloxStyle.streak(buf, A[0] + (B[0] - A[0]) * VfxAnim.slashHead(born) + R2[0] * d, A[1] + (B[1] - A[1]) * VfxAnim.slashHead(born) + R2[1] * d, A[2] + (B[2] - A[2]) * VfxAnim.slashHead(born) + R2[2] * d,
                    R2[0], R2[1], R2[2], 0.6f * P, 0.12f * P, a(hotc, 1f - u));
        }
        if (t > 0.55f) {
            float c = (t - 0.55f) / 0.45f;
            RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_BURST, VfxBlend.ADD, B[0], B[1], B[2], (1.4f + 1.0f * c) * P, rnd(inst, 9, 9) * Mth.TWO_PI, c, false, a(rim, 1f - c));
        }
        RobloxStyle.coreShell(buf, ctx, hx, hy, hz, 1.0f * P, rim, hotc, alpha * 0.6f, false);
    }

    // ------------------------------------------------------------------ WIND_TORNADO
    private static void tornado(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power, open = RobloxMath.easeOut(age / 12f), fade = life(inst, age, 0f, 15f);
        if (open <= 0.01f) return;
        rel(ctx, inst.from(ctx), A);
        float height = 2.8f * r * open;
        int col = inst.color, pale = hot(col, 0.5f);
        float gx = A[0], gy = A[1], gz = A[2];
        // the funnel: a twisting cone with a scrolling swirl, narrow at the foot and wide at the top
        RobloxStyle.cone(buf, RobloxStyle.SWIRL, VfxBlend.ALPHA, gx, gy, gz, 0f, 1f, 0f, 0.3f * r * open, 1.05f * r * open, height, ctx.seg(10, 6), 2, 0.06f, age * 0.14f, 1.3f, 2f, 0f, -age * 0.04f, -age * 0.04f + 1.5f,
                a(pale, 0.8f * fade), a(col, 0.2f * fade));
        // an inner funnel turning the other way, additive
        RobloxStyle.cone(buf, RobloxStyle.SWIRL, VfxBlend.ADD, gx, gy, gz, 0f, 1f, 0f, 0.2f * r * open, 0.65f * r * open, height * 0.92f, ctx.seg(8, 5), 2, 0.05f, -age * 0.22f, -1.0f, 2f, 0.3f, -age * 0.07f, -age * 0.07f + 1.5f,
                a(hot(col, 0.8f), 0.6f * fade), a(pale, 0.12f * fade));
        // two bright streaks spiralling up the walls
        int n = ctx.seg(9, 5);
        for (int k = 0; k < 2; k++) {
            RobloxStyle.spiral(n, gx, gy + 0.05f, gz, gx, gy + height, gz, 0.4f * r * open, 1.1f * r * open, 0.75f, -age * 0.25f + k * Mth.PI);
            RobloxStyle.swoosh(buf, ctx, RobloxStyle.SWOOSH, VfxBlend.ADD, RobloxStyle.PATH, n, 0.25f * r, 0.5f * r, 1f, -age * 0.1f, -age * 0.1f + 1f, a(pale, 0.4f * fade), a(pale, 0.12f * fade));
        }
        RobloxStyle.ring(buf, RobloxStyle.RING, VfxBlend.ALPHA, gx, gy + 0.2f, gz, 0f, 1f, 0f, 0.95f * r * open, 0.45f * r, ctx.seg(8, 6), 4, -age * 0.05f, a(DUST, 0.6f * fade));
    }

    // ------------------------------------------------------------------ ICE_FX3 (Shatter)
    private static void iceShatter(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(4, inst.duration), t = Mth.clamp(age / D, 0f, 1f), P = Mth.clamp(inst.power, 1f, 12f);
        int ice = VfxVertexBuffer.lerpColor(ICE, inst.color | 0xFF000000, 0.3f), hotc = hot(ice, 0.7f);
        rel(ctx, inst.from(ctx), A);
        float cx = A[0], cy = A[1], cz = A[2];
        if (age < 8f) {
            RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_BURST, VfxBlend.ADD, cx, cy + 0.5f * P, cz, 2.6f * P, age * 0.3f, age / 8f, true, a(hotc, 1f));
            RobloxStyle.coreShell(buf, ctx, cx, cy + 0.5f * P, cz, 1.1f * P, ice, hotc, 1f - age / 8f, false);
        }
        RobloxStyle.shock(buf, cx, cy + 0.08f, cz, 0f, 1f, 0f, Mth.clamp(age / 16f, 0f, 1f), 2.6f * P, 0.7f * P, 2, ctx.seg(16, 8), ice, 1f - t);
        Vec3 to = inst.to(ctx), from = inst.from(ctx);
        float hx = (float) (to.x - from.x), hz = (float) (to.z - from.z), hl = Mth.sqrt(hx * hx + hz * hz);
        float lean = hl > 0.05f ? 1f : 0f;
        float grow = RobloxMath.easeOutBack(RobloxMath.phase(t, 0f, 0.3f)) * (1f - RobloxMath.smooth(RobloxMath.phase(t, 0.6f, 1f)));
        for (int k = 0; k < 5; k++) {                                                              // blades thrusting out (towards the hint when there is one)
            float ang = Mth.TWO_PI * k / 5f + rnd(inst, k, 1) * 0.5f;
            float ox = Mth.cos(ang), oz = Mth.sin(ang);
            float ax = ox * 0.55f + (hl > 0.05f ? hx / hl * 0.5f * lean : 0f), az = oz * 0.55f + (hl > 0.05f ? hz / hl * 0.5f * lean : 0f);
            RobloxStyle.cone(buf, RobloxStyle.SWOOSH, VfxBlend.ADD, cx + ox * 0.3f * P, cy, cz + oz * 0.3f * P, ax, 0.85f, az, 0.22f * P, 0.02f * P, (1.3f + 0.6f * rnd(inst, k, 2)) * P * Math.max(0f, grow), 3, 1, 0f, k, 0f, 1f, 0f, 0f, 1.2f,
                    a(hotc, 0.7f), a(ice, 0.5f));
        }
        RobloxStyle.decal(buf, cx, cy + 0.06f, cz, 0f, 1f, 0f, 1.8f * P * RobloxMath.easeOut(t * 2f), age * 0.05f, a(ice, 0.5f * (1f - t)));
        RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_FIRE, VfxBlend.ALPHA, cx, cy + 0.5f * P, cz, 2.2f * P, age * 0.04f, t, true, a(hot(ice, 0.6f), 0.35f * (1f - t)));
    }

    // ------------------------------------------------------------------ WATER_BURST
    private static void waterBurst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick), P = inst.power;
        int col = inst.color, light = hot(col, 0.6f);
        rel(ctx, inst.from(ctx), A);
        float cx = A[0], cy = A[1], cz = A[2];
        float rise = RobloxMath.easeOut(RobloxMath.phase(t, 0f, 0.35f)), fall = 1f - RobloxMath.smooth(RobloxMath.phase(t, 0.45f, 1f));
        if (rise > 0.02f) {
            RobloxStyle.cone(buf, RobloxStyle.SWOOSH, VfxBlend.ALPHA, cx, cy, cz, 0f, 1f, 0f, 0.35f * P, 1.5f * P * rise, 1.3f * P * rise, ctx.seg(10, 6), 1, 0.12f, age * 0.05f, 0.4f, 2f, 0f, -age * 0.06f, -age * 0.06f + 1f,
                    a(light, 0.8f * fall), a(col, 0.15f * fall));
            RobloxStyle.cone(buf, RobloxStyle.SWOOSH, VfxBlend.ADD, cx, cy, cz, 0f, 1f, 0f, 0.25f * P, 1.0f * P * rise, 0.9f * P * rise, ctx.seg(6, 4), 1, 0.1f, -age * 0.07f, -0.3f, 2f, 0.4f, -age * 0.08f, -age * 0.08f + 1f,
                    a(WHITE, 0.55f * fall), a(light, 0.1f * fall));
        }
        RobloxStyle.shock(buf, cx, cy + 0.07f, cz, 0f, 1f, 0f, t, 3.2f * P, 0.9f * P, 2, ctx.seg(12, 8), col, fall);
        float e = RobloxMath.easeOut(t);
        for (int i = 0; i < 5; i++) {
            float ang = rnd(inst, i, 1) * Mth.TWO_PI, up = 0.4f + rnd(inst, i, 2), d = 2.2f * P * e, ca = Mth.cos(ang), sa = Mth.sin(ang);
            float vy = up * 1.2f - t * 3f, vl = Mth.sqrt(0.25f + vy * vy);
            RobloxStyle.streak(buf, cx + ca * d, cy + up * d * 1.2f - t * t * 3f, cz + sa * d, ca * 0.5f / vl, vy / vl, sa * 0.5f / vl, 0.35f * P, 0.12f * P, a(light, fall));
        }
        RobloxStyle.flipbook(buf, ctx, RobloxStyle.FLIP_FIRE, VfxBlend.ALPHA, cx, cy + 0.5f * P, cz, 2.4f * P, rnd(inst, 7, 7) * Mth.TWO_PI, RobloxMath.phase(t, 0f, 0.6f), false, a(light, 0.5f * fall));
        RobloxStyle.coreShell(buf, ctx, cx, cy + 0.4f * P, cz, 1.0f * P, col, light, 1f - t, false);
    }
}
