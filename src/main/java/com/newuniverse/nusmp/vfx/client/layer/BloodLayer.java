package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Blood Magic, drawn after the anime still of the Blood Cage: a giant translucent red sphere with a pink glowing rim, full of white
 * capillary fibres and a few dark clots; dozens of thin dark-red threads arcing from the caster on the ground up to the sphere and
 * crossing each other into a web; black ink tendrils pooling at the caster's feet.
 * <ul>
 *   <li>BLOOD_FX1 (cast / projectile, Jets of Blood): at 'from' a small vein sigil turns towards the target while droplets are drawn
 *       into a hot flare (first quarter); then a glossy comet of blood with a pointed head flies to 'to', trailed by a wide pink
 *       afterglow and two dark-red threads twisting round each other like a helix, spitting droplets; at 'to' it bursts into a
 *       splatter with a flash and droplets. power = size scale (1 normal), colour = tint of the blood.</li>
 *   <li>BLOOD_FX2 (zone / dome, the Blood Cage): at 'from' a black ink pool with creeping tendrils lies under a double ring of vein
 *       sigils (counter-rotating, pulsing outwards). A blood cell sphere (translucent red, pink rim, two counter-rotating fibre layers,
 *       clots) hovers over the centre; thin dark-red threads grow from the pool up to the sphere and out over the rim in crossing
 *       arcs; droplets rise through the field. power = radius in blocks, duration = life; fades in over 8 ticks, out over 12.</li>
 *   <li>BLOOD_FX3 (impact / burst, Blood Burst): a hot flash, a blood cell that swells and ruptures, a splatter on the ground that
 *       stays as a dark stain, a vein ring expanding, droplets and dark clots flying out under gravity, threads whipping out, a pink
 *       afterglow. 'to - from' leans the splash in that direction. power = scale.</li>
 * </ul>
 */
public class BloodLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation SPHERE = t("blood_sphere");
    public static final ResourceLocation FIBRE = t("blood_fibre");
    public static final ResourceLocation THREAD = t("blood_thread");
    public static final ResourceLocation STREAK = t("blood_streak");
    public static final ResourceLocation DROPLET = t("blood_droplet");
    public static final ResourceLocation SPLAT = t("blood_splat");
    public static final ResourceLocation CLOT = t("blood_clot");
    public static final ResourceLocation TENDRIL = t("blood_tendril");
    public static final ResourceLocation SIGIL = t("blood_sigil");
    public static final ResourceLocation FLARE = t("blood_flare");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int PINK = 0xFFFF8CB4;
    private static final int DEEP = 0xFFB0121E;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BLOOD_FX1, VfxShape.BLOOD_FX2, VfxShape.BLOOD_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BLOOD_FX1 -> 16;
            case BLOOD_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFE0182C; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BLOOD_FX1 -> cast(inst, ctx, buf);
            case BLOOD_FX2 -> cage(inst, ctx, buf);
            case BLOOD_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    /** The tint: the spell's colour pulled towards white, so the texture's own reds stay in charge. */
    private static int soft(int col, float k) { return VfxVertexBuffer.lerpColor(WHITE, col | 0xFF000000, k); }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float x = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float y = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(x, y);
    }

    private static Vector3f bez(Vector3f p0, Vector3f p1, Vector3f p2, float s) {
        float u = 1 - s;
        return new Vector3f(p0).mul(u * u).add(new Vector3f(p1).mul(2 * u * s)).add(new Vector3f(p2).mul(s * s));
    }

    /**
     * Camera-facing ribbon through the points (camera-relative), V running 1 -> 0 from the first to the last point so a texture with
     * its head at the top points along the path. Width tapers w0 -> w1 and ends are softened when {@code round}.
     */
    private static void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f[] p, float w0, float w1, boolean round,
                               int colTail, int colHead) {
        int n = p.length;
        if (n < 2 || !buf.hasBudget((n - 1) * 4)) return;
        Vector3f[] l = new Vector3f[n], r = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Vector3f tan = new Vector3f(p[Math.min(n - 1, i + 1)]).sub(p[Math.max(0, i - 1)]);
            Vector3f side = tan.cross(new Vector3f(p[i]).negate());
            if (side.lengthSquared() < 1e-8f) return;
            side.normalize();
            float s = (float) i / (n - 1);
            float w = Mth.lerp(s, w0, w1) * 0.5f * (round ? 0.3f + 0.7f * Mth.sin(s * Mth.PI) : 1f);
            l[i] = new Vector3f(p[i]).sub(side.x * w, side.y * w, side.z * w);
            r[i] = new Vector3f(p[i]).add(side.x * w, side.y * w, side.z * w);
        }
        for (int i = 0; i < n - 1; i++) {
            float s0 = (float) i / (n - 1), s1 = (float) (i + 1) / (n - 1);
            int c0 = blend.grade(VfxVertexBuffer.lerpColor(colTail, colHead, s0), 1f), c1 = blend.grade(VfxVertexBuffer.lerpColor(colTail, colHead, s1), 1f);
            buf.quad(tex, blend, l[i], r[i], r[i + 1], l[i + 1], 0, 1 - s1, 1, 1 - s0, c0, c1);
        }
    }

    /** A thread along a quadratic bezier, grown to fraction {@code grow} of its length. */
    private static void thread(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p0, Vector3f p1, Vector3f p2, float grow, float width, int col) {
        if (grow <= 0.02f) return;
        int segs = ctx.seg(5, 3);
        Vector3f[] pts = new Vector3f[segs + 1];
        for (int i = 0; i <= segs; i++) pts[i] = bez(p0, p1, p2, grow * i / segs);
        ribbon(buf, THREAD, VfxBlend.ALPHA, pts, width, width, true, col, col);
    }

    private static Vector3f perp(Vector3f d, Vector3f hint) {
        Vector3f s = new Vector3f(d).cross(hint);
        if (s.lengthSquared() < 1e-6f) s.set(d).cross(new Vector3f(1, 0, 0));
        return s.normalize();
    }

    /** A flying droplet: sprite tip pointing along its screen-space velocity. */
    private static void drop(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f pos, Vector3f vel, float size, int col) {
        float vr = vel.dot(ctx.camRight), vu = vel.dot(ctx.camUp);
        buf.billboard(ctx, tex, blend, pos, size, (float) Math.atan2(-vr, vu), col);
    }

    // ------------------------------------------------------------------ BLOOD_FX1: Jets of Blood
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / Math.max(1, inst.duration), 0, 1);
        float p = Mth.clamp(inst.power, 0.4f, 4f);
        int col = inst.color | 0xFF000000, tint = soft(col, 0.45f);
        RandomSource r = inst.random();
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 0.5f) { dir.set(0, 0, 1).mul(0.5f); len = 0.5f; to = new Vector3f(from).add(dir); }
        dir.div(len);

        // 1) gathering: a vein sigil turning towards the target, droplets sucked into a hot flare
        float cp = Mth.clamp(t / 0.22f, 0, 1);
        float gather = Mth.clamp(1f - (t - 0.22f) / 0.2f, 0, 1);
        if (gather > 0) {
            VfxPose pose = VfxPose.facing(new Vector3f(from).add(dir.x * 0.25f * p, dir.y * 0.25f * p, dir.z * 0.25f * p), dir).spin(age * 0.35f);
            buf.plane(SIGIL, VfxBlend.ADD, pose, p * (0.75f - 0.25f * cp), a(PINK, 0.9f * gather * Math.min(1f, cp * 3f)));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, from, p * (0.4f + 1.4f * VfxAnim.easeOutCubic(cp)), age * 0.2f, a(soft(col, 0.6f), gather));
            for (int i = 0; i < 6; i++) {
                float ang = Mth.TWO_PI * i / 6 + r.nextFloat() + age * 0.1f;
                float rad = p * (1.3f * (1 - cp) + 0.1f) * (0.7f + 0.5f * r.nextFloat());
                Vector3f o = new Vector3f(ctx.camRight).mul(Mth.cos(ang) * rad).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * rad)).add(from);
                Vector3f v = new Vector3f(from).sub(o);
                drop(ctx, buf, DROPLET, VfxBlend.ALPHA, o, v, p * 0.3f, a(tint, gather));
            }
        }

        // 2) the jet: comet head, pink afterglow, two threads twisting round it
        float fl = Mth.clamp((t - 0.18f) / 0.68f, 0, 1);
        if (t > 0.16f && fl < 1f) {
            float h = VfxAnim.easeInOutSine(fl);
            float trail = Math.min(len * 0.55f, 3.4f * p);
            float hs = h * len, ts = Math.max(0f, hs - trail);
            Vector3f head = new Vector3f(dir).mul(hs).add(from), tail = new Vector3f(dir).mul(ts).add(from);
            float f = 1f - Mth.clamp((fl - 0.85f) / 0.15f, 0, 1) * 0.6f;
            int n = ctx.seg(5, 3);
            Vector3f[] line = new Vector3f[n + 1];
            for (int i = 0; i <= n; i++) line[i] = new Vector3f(tail).lerp(head, (float) i / n);
            ribbon(buf, STREAK, VfxBlend.ADD, line, 2.4f * p, 2.0f * p, false, a(PINK, 0f), a(PINK, 0.7f * f));
            ribbon(buf, STREAK, VfxBlend.ALPHA, line, 1.1f * p, 1.0f * p, false, a(tint, 0.3f), a(tint, f));
            Vector3f u1 = perp(dir, new Vector3f(0, 1, 0)), u2 = new Vector3f(dir).cross(u1).normalize();
            for (int k = 0; k < 2; k++) {
                int m = ctx.seg(6, 4);
                Vector3f[] hx = new Vector3f[m + 1];
                for (int i = 0; i <= m; i++) {
                    float s = (float) i / m;
                    float th = s * 7f - age * 0.7f + k * Mth.PI;
                    float rad = 0.26f * p * Mth.sin(Mth.PI * Math.min(1f, 0.15f + s));
                    hx[i] = new Vector3f(line[0]).lerp(line[n], s).add(u1.x * Mth.cos(th) * rad + u2.x * Mth.sin(th) * rad,
                            u1.y * Mth.cos(th) * rad + u2.y * Mth.sin(th) * rad, u1.z * Mth.cos(th) * rad + u2.z * Mth.sin(th) * rad);
                }
                ribbon(buf, THREAD, VfxBlend.ALPHA, hx, 0.4f * p, 0.26f * p, false, a(WHITE, 0.3f), a(WHITE, f));
            }
            for (int i = 0; i < 4; i++) {                         // droplets spat off the trail
                float s = 0.15f + 0.8f * r.nextFloat();
                Vector3f o = new Vector3f(tail).lerp(head, s).add(u1.x * (r.nextFloat() - 0.5f) * 0.7f * p, (r.nextFloat() - 0.2f) * 0.5f * p, u1.z * (r.nextFloat() - 0.5f) * 0.7f * p);
                drop(ctx, buf, DROPLET, VfxBlend.ALPHA, o, new Vector3f(dir).mul(-1f), 0.45f * p, a(tint, f * s));
            }
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 1.0f * p, age * 0.25f, a(soft(col, 0.55f), f));
            buf.billboard(ctx, CLOT, VfxBlend.ALPHA, head, 0.8f * p, age * 0.4f, a(tint, f));
        }

        // 3) impact: splatter, flash, a ring, droplets
        float ip = Mth.clamp((t - 0.8f) / 0.2f, 0, 1);
        if (t > 0.78f) {
            float e = VfxAnim.easeOutCubic(ip);
            Vector3f at = to;
            buf.billboard(ctx, FLARE, VfxBlend.ADD, at, p * 2.6f * (1f - ip * 0.7f), r.nextFloat() * 6f, a(PINK, 1f - ip));
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, at, p * 2.6f * e, r.nextFloat() * 6f, a(tint, 1f - ip * 0.4f));
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.facing(at, new Vector3f(dir).negate()).spin(ip), p * (0.5f + 1.7f * e), a(PINK, 0.8f * (1f - ip)));
            for (int i = 0; i < 6; i++) {
                float ang = Mth.TWO_PI * (i + r.nextFloat()) / 6;
                float sp = (0.9f + r.nextFloat()) * p;
                Vector3f v = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang))).mul(sp);
                Vector3f o = new Vector3f(v).mul(e * 0.9f).add(at).sub(0, ip * ip * 0.5f * p, 0);
                drop(ctx, buf, DROPLET, VfxBlend.ALPHA, o, v, 0.5f * p, a(tint, 1f - ip));
            }
        }
    }

    // ------------------------------------------------------------------ BLOOD_FX2: the Blood Cage
    private void cage(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        float R = Math.max(1.5f, inst.power);
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 14f, 0, 1));
        int col = inst.color | 0xFF000000, tint = soft(col, 0.4f);
        RandomSource r = inst.random();
        Vector3f g = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(new Vector3f(g).add(0, 0.03f, 0));
        float pulse = VfxAnim.pulse(age, 1.2f);

        // the ink pool and its tendrils, then the vein sigils above it
        buf.plane(TENDRIL, VfxBlend.ALPHA, ground.spin(age * 0.008f), R * 0.95f * open, a(WHITE, 0.95f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.02f).spin(age * 0.015f), R * open, a(soft(col, 0.7f), 0.8f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.03f).spin(-age * 0.03f), R * 0.6f * open, a(PINK, (0.45f + 0.3f * pulse) * fade));
        float pp = (age % 36f) / 36f;                                      // pulse ring rolling outwards
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.04f), R * (0.3f + 0.75f * VfxAnim.easeOutCubic(pp)) * open, a(col, 0.55f * (1f - pp) * fade));

        // the blood cell
        float sr = R * 0.78f * open * (1f + 0.02f * pulse);
        Vector3f c = new Vector3f(g).add(0, R * 0.95f * open + 0.4f, 0);
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, sr * 3.0f, 0, a(PINK, 0.28f * fade));
        buf.billboard(ctx, SPHERE, VfxBlend.ALPHA, c, sr * 2f, age * 0.004f, a(tint, 0.92f * fade));
        buf.billboard(ctx, FIBRE, VfxBlend.ADD, c, sr * 1.75f, -age * 0.012f, a(WHITE, (0.3f + 0.12f * pulse) * fade));
        buf.billboard(ctx, FIBRE, VfxBlend.ALPHA, c, sr * 1.45f, age * 0.017f, a(0xFFFFE0E8, 0.4f * fade));
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, sr * (0.5f + 0.25f * pulse), age * 0.03f, a(soft(col, 0.6f), 0.45f * fade));

        // the thread web: from the pool up to the cell, and long arcs out over the rim, crossing each other
        int nT = ctx.seg(11, 6);
        for (int i = 0; i < nT; i++) {
            float yaw = Mth.TWO_PI * (i + r.nextFloat() * 0.8f) / nT + age * 0.004f;
            float grow = VfxAnim.easeOutCubic(Mth.clamp((age - i * 1.1f) / 18f, 0, 1));
            float sway = Mth.sin(age * 0.07f + i * 1.7f) * 0.06f * R;
            float rho = (0.05f + 0.12f * r.nextFloat()) * R;
            Vector3f p0 = new Vector3f(g).add(Mth.cos(yaw) * rho, 0.05f, Mth.sin(yaw) * rho);
            boolean up = i % 3 != 2;
            Vector3f p1, p2;
            float dy = Mth.cos(yaw), dz = Mth.sin(yaw);
            if (up) {                                                       // to the cell, landing on its lower / side surface
                float pitch = -1.2f + 1.5f * r.nextFloat(), yw = yaw + (r.nextFloat() - 0.5f) * 1.6f;
                p2 = new Vector3f(c).add(Mth.cos(yw) * Mth.cos(pitch) * sr, Mth.sin(pitch) * sr, Mth.sin(yw) * Mth.cos(pitch) * sr);
                p1 = new Vector3f(p0).lerp(p2, 0.5f).add(dy * R * (0.25f + 0.3f * r.nextFloat()) + sway, R * 0.15f, dz * R * (0.25f + 0.3f * r.nextFloat()) - sway);
            } else {                                                        // over the rim and down to the ground outside
                float yw = yaw + (r.nextFloat() - 0.5f) * 2f;
                p2 = new Vector3f(g).add(Mth.cos(yw) * R * (0.95f + 0.2f * r.nextFloat()), 0.05f, Mth.sin(yw) * R * (0.95f + 0.2f * r.nextFloat()));
                p1 = new Vector3f(c).add(Mth.cos(yaw) * R * 0.9f + sway, R * 0.85f, Mth.sin(yaw) * R * 0.9f);
            }
            thread(ctx, buf, p0, p1, p2, grow, 0.2f + 0.045f * R, a(WHITE, (0.75f + 0.25f * Mth.sin(age * 0.15f + i)) * fade));
        }

        // droplets rising through the field towards the cell
        int nM = ctx.seg(10, 5);
        for (int i = 0; i < nM; i++) {
            float ph = (age * 0.018f + r.nextFloat()) % 1f;
            float ang = r.nextFloat() * Mth.TWO_PI + age * 0.01f, rad = R * (0.15f + 0.8f * r.nextFloat()) * (1f - 0.25f * ph);
            Vector3f o = new Vector3f(g).add(Mth.cos(ang) * rad, 0.1f + ph * R * 1.5f, Mth.sin(ang) * rad);
            buf.billboard(ctx, DROPLET, VfxBlend.ADD, o, 0.22f + 0.05f * R, age * 0.02f + i, a(PINK, Mth.sin(ph * Mth.PI) * 0.8f * fade));
        }
    }

    // ------------------------------------------------------------------ BLOOD_FX3: Blood Burst
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / Math.max(1, inst.duration), 0, 1);
        float p = Mth.clamp(inst.power, 0.4f, 4f);
        int col = inst.color | 0xFF000000, tint = soft(col, 0.45f);
        RandomSource r = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean lean = hint.lengthSquared() > 0.04f;
        if (lean) hint.normalize(); else hint.set(0, 0.6f, 0);
        float e = VfxAnim.easeOutCubic(Mth.clamp(t / 0.3f, 0, 1));
        float out = Mth.clamp((1f - t) / 0.35f, 0, 1);                   // global fade in the last third
        float flash = Mth.clamp(1f - t / 0.22f, 0, 1);
        float tau = age / 20f;

        // stain on the ground and the afterglow
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0));
        buf.plane(TENDRIL, VfxBlend.ALPHA, ground.spin(r.nextFloat() * 6f), 1.5f * p * e, a(WHITE, 0.9f * out));
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, 4.0f * p * (0.4f + 0.6f * e), 0, a(PINK, 0.12f * out));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.04f).spin(t), p * (0.3f + 3.3f * e), a(col, 0.8f * (1f - t) * (1f - t)));

        // the cell swells and ruptures
        float swell = VfxAnim.easeOutBack(Mth.clamp(t / 0.16f, 0, 1));
        float cellA = Mth.clamp(1f - (t - 0.14f) / 0.2f, 0, 1);
        if (cellA > 0) {
            float cs = p * (0.8f + 2.2f * Mth.clamp(t / 0.16f, 0, 1)) * (0.6f + 0.4f * swell);
            buf.billboard(ctx, SPHERE, VfxBlend.ALPHA, c, cs, age * 0.05f, a(tint, cellA));
            buf.billboard(ctx, FIBRE, VfxBlend.ADD, c, cs * 0.9f, -age * 0.1f, a(WHITE, 0.6f * cellA));
        }
        // flash and splatter
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, p * (1.5f + 3.5f * e), age * 0.1f, a(soft(col, 0.75f), flash));
        float sp = Mth.clamp((t - 0.1f) / 0.2f, 0, 1);
        if (sp > 0) {
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, c, 4.2f * p * VfxAnim.easeOutCubic(sp), r.nextFloat() * 6f, a(tint, 0.95f * out));
            buf.billboard(ctx, SPLAT, VfxBlend.ADD, c, 3.0f * p * VfxAnim.easeOutCubic(sp), r.nextFloat() * 6f, a(PINK, 0.4f * out * (1f - sp * 0.5f)));
        }

        // flying droplets and heavy clots, leaning along the hint
        int nD = ctx.seg(14, 7);
        for (int i = 0; i < nD; i++) {
            float yaw = r.nextFloat() * Mth.TWO_PI, up = 0.4f + 1.3f * r.nextFloat(), sd = (0.7f + 1.4f * r.nextFloat()) * p;
            Vector3f v = new Vector3f(Mth.cos(yaw), up, Mth.sin(yaw)).mul(sd);
            if (lean) v.add(new Vector3f(hint).mul(sd * 0.9f));
            float tt = tau * (0.6f + 0.5f * r.nextFloat()) * 1.5f;
            Vector3f o = new Vector3f(v).mul(tt).add(c).sub(0, 2.2f * tt * tt * p, 0);
            Vector3f vel = new Vector3f(v).sub(0, 4.4f * tt * p, 0);
            float life = Mth.clamp(1.1f - t * (0.9f + 0.3f * r.nextFloat()), 0, 1);
            if (life > 0) drop(ctx, buf, DROPLET, VfxBlend.ALPHA, o, vel, (0.5f + 0.35f * r.nextFloat()) * p, a(tint, life));
        }
        int nC = ctx.seg(6, 3);
        for (int i = 0; i < nC; i++) {
            float yaw = r.nextFloat() * Mth.TWO_PI, sd = (0.5f + r.nextFloat()) * p;
            Vector3f v = new Vector3f(Mth.cos(yaw) * sd, (0.6f + r.nextFloat()) * p, Mth.sin(yaw) * sd);
            float tt = tau * 1.2f;
            Vector3f o = new Vector3f(v).mul(tt).add(c).sub(0, 2.4f * tt * tt * p, 0);
            buf.billboard(ctx, CLOT, VfxBlend.ALPHA, o, (0.4f + 0.25f * r.nextFloat()) * p, age * 0.2f + i, a(WHITE, out));
        }

        // threads lashing out of the burst
        int nT = ctx.seg(5, 3);
        for (int i = 0; i < nT; i++) {
            float yaw = Mth.TWO_PI * (i + r.nextFloat() * 0.7f) / nT, reach = (1.8f + 1.5f * r.nextFloat()) * p;
            float gr = VfxAnim.easeOutCubic(Mth.clamp(t / 0.25f, 0, 1));
            Vector3f p2 = new Vector3f(c).add(Mth.cos(yaw) * reach, (r.nextFloat() - 0.2f) * p, Mth.sin(yaw) * reach);
            Vector3f p1 = new Vector3f(c).add(Mth.cos(yaw + 0.8f) * reach * 0.5f, reach * 0.6f, Mth.sin(yaw + 0.8f) * reach * 0.5f);
            thread(ctx, buf, c, p1, p2, gr, 0.16f * p + 0.06f, a(WHITE, out));
        }
    }
}
