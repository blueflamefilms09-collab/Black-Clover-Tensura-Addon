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
 * 1.93.0 Flame Magic (the Crimson Lion: Mereoleona / Fuegoleon), drawn toward the Time Magic layers' quality: layered, eased, every sprite from textures/particle/flamefx_*.png
 * (tools/gen_flame_fx_textures.py).
 * <ul>
 *   <li>FLAMEFX_CAST: a charge flash and the sun-lion sigil at 'from' (first quarter), then a head of fire runs to 'to' trailing three winding strands of flame round a straight core
 *       and shedding embers; a flash and a ring where it arrives. 'from' = origin, 'to' = target, power = size.</li>
 *   <li>FLAMEFX_ZONE: the sun-lion sigil turning on the ground at 'from', a ring of fire at the rim with flame tongues standing round it, a heat glow and embers rising. power = radius.</li>
 *   <li>FLAMEFX_BURST: a starburst flash, a ring of fire running out, ten flames thrown outward and up, embers on arcs and smoke puffs. power = size.</li>
 * </ul>
 * Render types: every flame is additive (VfxBlend.ADD, glows land on top), the smoke is alpha blended. At most 400 vertices an activation (budgeted with hasBudget, counts with ctx.seg).
 */
public class FlameFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation FLAME = t("flamefx_flame"), TONGUE = t("flamefx_tongue"), RING = t("flamefx_ring"), EMBER = t("flamefx_ember"), LION = t("flamefx_lion"),
            BURST = t("flamefx_burst"), SMOKE = t("flamefx_smoke");
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.FLAMEFX_CAST, VfxShape.FLAMEFX_ZONE, VfxShape.FLAMEFX_BURST); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.FLAMEFX_ZONE ? 80 : s == VfxShape.FLAMEFX_BURST ? 28 : 22; }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF8A2A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.FLAMEFX_BURST && inst.power >= 1.2f) VfxShake.add(inst.payload.from(), 0.9f * inst.power, 10);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case FLAMEFX_CAST -> cast(inst, ctx, buf);
            case FLAMEFX_ZONE -> zone(inst, ctx, buf);
            case FLAMEFX_BURST -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    /** 1 while running, ramping 0 to 1 over the first {@code in} ticks and 1 to 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** A ribbon from a to b (camera facing), the texture lying along u (tail a = 0, head b = 1), cut in n pieces so the colour can fade from ca to cb along it. */
    private static void strip(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float wa, float wb, int n, int ca, int cb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f toCam = new Vector3f(a).add(b).mul(0.5f).negate().normalize();
        Vector3f side = new Vector3f(dir).cross(toCam);
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize();
        for (int i = 0; i < n; i++) {
            float t0 = (float) i / n, t1 = (float) (i + 1) / n;
            Vector3f c0 = new Vector3f(dir).mul(t0).add(a), c1 = new Vector3f(dir).mul(t1).add(a);
            Vector3f s0 = new Vector3f(side).mul(Mth.lerp(t0, wa, wb) * 0.5f), s1 = new Vector3f(side).mul(Mth.lerp(t1, wa, wb) * 0.5f);
            int col = blend.grade(VfxVertexBuffer.lerpColor(ca, cb, (t0 + t1) * 0.5f), 1f - t0);
            buf.quad(tex, blend, new Vector3f(c0).sub(s0), new Vector3f(c1).sub(s1), new Vector3f(c1).add(s1), new Vector3f(c0).add(s0), t0, 0, t1, 1, col, col);
        }
    }

    /** A flame standing on 'base' (camera relative), facing the camera about the vertical axis. */
    private static void tongue(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, Vector3f base, float w, float h, int argb) {
        Vector3f r = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0);
        r.normalize().mul(w * 0.5f);
        int col = VfxBlend.ADD.grade(argb, 1f);
        buf.quad(tex, VfxBlend.ADD, new Vector3f(base).sub(r), new Vector3f(base).add(r), new Vector3f(base).add(r).add(0, h, 0), new Vector3f(base).sub(r).add(0, h, 0), 0, 0, 1, 1, col, col);
    }

    private static Vector3f ease(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    // ------------------------------------------------------------------ FLAMEFX_CAST
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, fade = life(inst, age, 0, 6);
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.55f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 0.05f) { dir.set(0, 0, 1); len = 0.05f; }
        dir.div(len);
        float charge = Mth.clamp(age / (inst.duration * 0.25f), 0, 1), run = Mth.clamp((age - inst.duration * 0.12f) / (inst.duration * 0.7f), 0, 1);
        float head = VfxAnim.easeOutCubic(run), tail = Math.max(0f, head - 0.55f);
        // the charge: the sun-lion sigil facing along the cast, a bloom
        float open = VfxAnim.easeOutBack(charge) * (1f - 0.6f * Mth.clamp((age - inst.duration * 0.3f) / (inst.duration * 0.3f), 0, 1));
        VfxPose face = VfxPose.facing(new Vector3f(from).add(new Vector3f(dir).mul(0.25f * p)), dir);
        if (open > 0.02f) {
            buf.plane(LION, VfxBlend.ADD, face.spin(age * 0.07f), 0.65f * p * open, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
            buf.plane(RING, VfxBlend.ADD, face.lift(0.04f).spin(-age * 0.11f), 0.5f * p * open, VfxVertexBuffer.withAlpha(hot, 0.8f * fade));
            VfxBloom.glow(ctx, buf, from, 0.5f * p * open, col, 0.8f * fade * (1f - run * 0.6f));
        }
        if (head <= 0.001f) return;
        // the stream: a straight core and three strands winding round it, from the tail to the head
        Vector3f up = Math.abs(dir.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f side = new Vector3f(dir).cross(up).normalize(), up2 = new Vector3f(side).cross(dir).normalize();
        Vector3f hp = ease(from, to, head), tp = ease(from, to, tail);
        float width = 0.7f * p;
        int segs = ctx.seg(8, 4);
        strip(ctx, buf, FLAME, VfxBlend.ADD, tp, hp, width * 0.3f, width, segs, VfxVertexBuffer.withAlpha(col, 0f), VfxVertexBuffer.withAlpha(hot, 0.95f * fade));
        for (int k = 0; k < 3; k++) {
            Vector3f prev = null;
            int pieces = ctx.seg(6, 3);
            for (int i = 0; i <= pieces; i++) {
                float s = Mth.lerp(i / (float) pieces, tail, head);
                float ang = s * 14f - age * 0.35f + k * Mth.TWO_PI / 3f, rad = 0.36f * p * (0.5f + 0.5f * (i / (float) pieces)) * (1f - 0.35f * head);
                Vector3f pos = ease(from, to, s).add(new Vector3f(side).mul(Mth.cos(ang) * rad)).add(new Vector3f(up2).mul(Mth.sin(ang) * rad));
                if (prev != null) strip(ctx, buf, FLAME, VfxBlend.ADD, prev, pos, width * 0.4f * (i / (float) pieces), width * 0.45f * ((i + 1) / (float) pieces), 1,
                        VfxVertexBuffer.withAlpha(col, 0.5f * fade * (i / (float) pieces)), VfxVertexBuffer.withAlpha(col, 0.8f * fade));
                prev = pos;
            }
        }
        // the head of the fire and the embers it sheds
        VfxBloom.glow(ctx, buf, hp, 0.95f * p, col, fade);
        buf.billboard(ctx, BURST, VfxBlend.ADD, hp, 1.7f * p, age * 0.2f, VfxVertexBuffer.withAlpha(hot, 0.85f * fade));
        RandomSource r = inst.random();
        int n = ctx.seg(10, 4);
        for (int i = 0; i < n; i++) {
            float s = tail + (head - tail) * r.nextFloat(), life = Mth.frac(age * 0.07f + r.nextFloat());
            Vector3f q = ease(from, to, s).add(new Vector3f(side).mul((r.nextFloat() - 0.5f) * 0.6f * p)).add(new Vector3f(up2).mul((r.nextFloat() - 0.2f) * 0.5f * p + life * 0.5f));
            buf.billboard(ctx, EMBER, VfxBlend.ADD, q, 0.34f * p * (1f - life) + 0.06f, age * 0.3f + i, VfxVertexBuffer.withAlpha(i % 2 == 0 ? hot : col, Mth.sin(life * Mth.PI) * fade));
        }
        if (head > 0.88f) {                                                                     // the arrival
            float k = (head - 0.88f) / 0.12f;
            buf.billboard(ctx, BURST, VfxBlend.ADD, to, 1.4f * p * k, age * 0.1f, VfxVertexBuffer.withAlpha(hot, (1f - 0.4f * k) * fade));
            buf.plane(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(to).add(0, 0.08f, 0)), 0.5f * p + 0.9f * p * k, VfxVertexBuffer.withAlpha(col, (1f - k) * 0.8f * fade));
        }
    }

    // ------------------------------------------------------------------ FLAMEFX_ZONE
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.55f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        VfxBloom.planeGlow(buf, ground.lift(-0.02f), R * open, col, 0.55f * fade);
        buf.plane(LION, VfxBlend.ADD, ground.spin(age * 0.018f), R * 0.92f * open, VfxVertexBuffer.withAlpha(col, 0.85f * fade));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.03f).spin(-age * 0.03f), R * 1.04f * open, VfxVertexBuffer.withAlpha(hot, 0.95f * fade * (0.8f + 0.2f * Mth.sin(age * 0.3f))));
        // flames standing round the rim, each flickering on its own beat; a second, smaller ring inside
        RandomSource r = inst.random();
        int n = ctx.seg(14, 6);
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + r.nextFloat() * 0.2f, ph = r.nextFloat() * Mth.TWO_PI, rad = R * (0.98f + 0.04f * r.nextFloat());
            float h = (0.55f + 0.35f * r.nextFloat()) * Math.max(1f, R * 0.35f) * (0.8f + 0.2f * Mth.sin(age * 0.5f + ph)) * open;
            tongue(ctx, buf, TONGUE, new Vector3f(c).add(Mth.cos(a) * rad, 0.05f, Mth.sin(a) * rad), h * 0.5f, h, VfxVertexBuffer.withAlpha(i % 2 == 0 ? col : hot, 0.85f * fade));
        }
        int m = ctx.seg(6, 3);
        for (int i = 0; i < m; i++) {
            float a = i * Mth.TWO_PI / m + age * 0.02f, rad = R * 0.45f;
            float h = 0.5f * Math.max(1f, R * 0.3f) * (0.8f + 0.2f * Mth.sin(age * 0.6f + i));
            tongue(ctx, buf, TONGUE, new Vector3f(c).add(Mth.cos(a) * rad, 0.05f, Mth.sin(a) * rad), h * 0.5f, h, VfxVertexBuffer.withAlpha(hot, 0.7f * fade));
        }
        int e = ctx.seg(16, 6);
        for (int i = 0; i < e; i++) {                                                           // embers rising off the whole disc
            float life = Mth.frac(age * 0.022f * (1f + 0.4f * r.nextFloat()) + r.nextFloat()), a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R;
            buf.billboard(ctx, EMBER, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(a) * d, 0.2f + life * (1.2f + R * 0.3f), Mth.sin(a) * d), 0.15f * (1f - life) + 0.04f, age * 0.2f + i,
                    VfxVertexBuffer.withAlpha(i % 2 == 0 ? hot : col, Mth.sin(life * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ FLAMEFX_BURST
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick), p = inst.power, fade = 1f - VfxAnim.easeInCubic(t);
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.6f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float flash = Mth.clamp(1f - age / 7f, 0, 1);
        VfxBloom.glow(ctx, buf, c, 0.9f * p * (0.5f + VfxAnim.easeOutCubic(t)), col, fade);
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, (1.3f + 2.4f * VfxAnim.easeOutCubic(Mth.clamp(t * 2f, 0, 1))) * p, age * 0.05f, VfxVertexBuffer.withAlpha(hot, Math.min(1f, 0.35f + flash) * fade));
        float rr = (0.4f + 2.4f * VfxAnim.easeOutCubic(t)) * p;
        buf.plane(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.07f, 0)).spin(age * 0.08f), rr, VfxVertexBuffer.withAlpha(col, 0.95f * fade));
        buf.plane(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.09f, 0)).spin(-age * 0.1f), rr * 0.62f, VfxVertexBuffer.withAlpha(hot, 0.7f * fade));
        // ten flames thrown outward and up, then falling off
        RandomSource r = inst.random();
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + r.nextFloat() * 0.4f, reach = (0.9f + 0.9f * r.nextFloat()) * p, up = (0.5f + 0.7f * r.nextFloat()) * p;
            float e = VfxAnim.easeOutCubic(Mth.clamp(t * 1.5f, 0, 1));
            Vector3f from = new Vector3f(c).add(Mth.cos(a) * 0.15f * p, 0.1f, Mth.sin(a) * 0.15f * p);
            Vector3f tip = new Vector3f(c).add(Mth.cos(a) * reach * e, 0.1f + up * Mth.sin(Mth.clamp(t * 1.4f, 0, 1) * Mth.PI) , Mth.sin(a) * reach * e);
            strip(ctx, buf, FLAME, VfxBlend.ADD, from, tip, 0.1f * p, 0.38f * p * (1f - t * 0.5f), 3, VfxVertexBuffer.withAlpha(hot, 0.9f * fade), VfxVertexBuffer.withAlpha(col, 0.4f * fade));
        }
        int e = ctx.seg(14, 5);
        for (int i = 0; i < e; i++) {                                                           // embers on ballistic arcs
            float a = r.nextFloat() * Mth.TWO_PI, v = (1.2f + 2.4f * r.nextFloat()) * p, vy = (1.4f + 2.2f * r.nextFloat()) * p, tt = t * 1.2f;
            Vector3f q = new Vector3f(c).add(Mth.cos(a) * v * tt * 0.7f, 0.1f + vy * tt - 2.6f * tt * tt * p, Mth.sin(a) * v * tt * 0.7f);
            buf.billboard(ctx, EMBER, VfxBlend.ADD, q, 0.2f * p * (1f - t) + 0.04f, age * 0.3f + i, VfxVertexBuffer.withAlpha(i % 2 == 0 ? hot : col, fade));
        }
        int s = ctx.seg(5, 2);
        for (int i = 0; i < s; i++) {                                                           // smoke puffs lifting off the ground
            float a = r.nextFloat() * Mth.TWO_PI, d = (0.2f + 0.8f * r.nextFloat()) * p * VfxAnim.easeOutCubic(t);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(a) * d, 0.3f + t * 1.2f * p, Mth.sin(a) * d), (0.6f + 0.8f * t) * p, a + age * 0.02f,
                    VfxVertexBuffer.withAlpha(0xFF3A2A24, 0.45f * Mth.sin(Mth.clamp(t * 1.2f, 0, 1) * Mth.PI)));
        }
    }
}
