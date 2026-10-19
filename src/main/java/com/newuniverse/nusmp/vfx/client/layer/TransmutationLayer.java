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
 * Transmutation Magic (Grey, the alchemist), 1.93.0 rebuild, drawn as alchemy: a circle that turns, matter that changes colour as it changes substance (brass gold to teal green),
 * spikes of iron, sparkles of the transformation. Textures (tools/gen_transmutation_vfx_textures.py): transmutation_circle, _rune_band, _spike, _star, _petal, _ring.
 * <ul>
 *   <li>TRANSMUTATION_FX1, CAST / PROJECTILE ("Iron Spikes", "Magic Convert" bolts). A circle opens at 'from' facing the target and turns, a second smaller one in front of it
 *       turns against it (first quarter); a bolt of matter flies 'from' to 'to' as a beam whose colour runs from brass gold at the tail to teal at the head, a four-pointed
 *       star on its head, sparkles shed along the path alternating the two colours; a ring flashes at 'to'. from = origin, to = target, power = size (1.0 about a block),
 *       duration = flight ticks (default 16).</li>
 *   <li>TRANSMUTATION_FX2, ZONE / FIELD ("Quagmire", "Magic Convert" circle, "Grand Transmutation"). The great circle turns on the ground inside a pulsing ring, a gold circle
 *       turns against it inside; two ribbons of glyphs climb the zone in hoops and fade at the top; iron spikes rise round the rim; glyph sparkles drift up. from = centre on the
 *       ground, power = RADIUS in blocks, duration = life ticks (default 80; fades in over 8, out over 12).</li>
 *   <li>TRANSMUTATION_FX3, IMPACT / SIGNATURE. A white-teal flash, a ground shock ring and a standing ring, the circle stamped on the ground, eight spikes thrown outward and
 *       a ring of sparkles; with the colour 0xFF9AD0 (Flowering) the spikes are petals that fall slowly and the colours turn rose. from = centre, to = optional direction hint,
 *       power = scale (0.6 to 3.0; the manager draws FX1 and FX3 at 3x), duration = life ticks (default 28).</li>
 * </ul>
 * inst.color is mixed 35 percent into the teal, the gold is fixed. Vertex use stays under about 260 per instance (budget 400).
 */
public class TransmutationLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation CIRCLE = t("transmutation_circle");
    public static final ResourceLocation BAND = t("transmutation_rune_band");
    public static final ResourceLocation SPIKE = t("transmutation_spike");
    public static final ResourceLocation STAR = t("transmutation_star");
    public static final ResourceLocation PETAL = t("transmutation_petal");
    public static final ResourceLocation RING = t("transmutation_ring");

    private static final int TEAL = 0xFF7AF0D8, GOLD = 0xFFFFDC70, ROSE = 0xFFFF9AD0, WHITE = 0xFFFFFFFF;
    private static final float BAND_ASPECT = 16f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.TRANSMUTATION_FX1, VfxShape.TRANSMUTATION_FX2, VfxShape.TRANSMUTATION_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.TRANSMUTATION_FX1 ? 16 : s == VfxShape.TRANSMUTATION_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return TEAL; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case TRANSMUTATION_FX1 -> cast(inst, ctx, buf);
            case TRANSMUTATION_FX2 -> zone(inst, ctx, buf);
            case TRANSMUTATION_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static float fract(float x) { return x - (float) Math.floor(x); }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    private static int teal(VfxInstance inst) { return VfxVertexBuffer.lerpColor(TEAL, inst.color | 0xFF000000, 0.35f); }

    /** The screen rotation that turns an upright sprite so that it points along the world direction d (camera relative). */
    private static float pointing(VfxRenderContext ctx, Vector3f d) {
        return (float) Mth.atan2(d.dot(ctx.camUp), d.dot(ctx.camRight)) - Mth.HALF_PI;
    }

    /** A ribbon standing on a circle round the pose normal: U wraps {@code repeats} times and scrolls. */
    private static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfWidth, int segments, float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n), u0, 0, u1, 1, col, col);
        }
    }

    // ------------------------------------------------------------------ FX1: cast / projectile
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.4f, 4f) * 0.55f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) return;
        dir.div(dist);
        int teal = teal(inst);
        // 1. the circles at the hand: they open in the first quarter and close as the bolt leaves
        float open = VfxAnim.easeOutBack(Mth.clamp(p / 0.22f, 0, 1)) * Mth.clamp((0.5f - p) / 0.25f, 0, 1);
        if (open > 0.02f) {
            VfxPose face = VfxPose.facing(from, dir);
            buf.plane(CIRCLE, VfxBlend.ADD, face.spin(age * 0.18f), 0.62f * size * open, VfxVertexBuffer.withAlpha(teal, 0.95f * open));
            VfxPose front = VfxPose.facing(new Vector3f(from).add(new Vector3f(dir).mul(0.7f * size)), dir);
            buf.plane(CIRCLE, VfxBlend.ADD, front.spin(-age * 0.26f), 0.38f * size * open, VfxVertexBuffer.withAlpha(GOLD, 0.9f * open));
            VfxBloom.glow(ctx, buf, from, 0.5f * size, teal, 0.6f * open);
        }
        // 2. the bolt: gold tail to teal head
        float head = VfxAnim.easeOutCubic(Mth.clamp((p - 0.12f) / 0.7f, 0, 1));
        float tail = Mth.clamp(head - 0.4f, 0, 1) * (p > 0.82f ? Mth.clamp(1f - (p - 0.82f) / 0.18f * 0.2f, 0, 1) : 1f);
        float fade = Mth.clamp((1f - p) / 0.2f, 0, 1);
        Vector3f hp = lerp(from, to, head), tp = lerp(from, to, p > 0.82f ? head : tail);
        if (p > 0.12f && head > 0.01f) {
            int k0 = VfxVertexBuffer.withAlpha(GOLD, 0.0f), k1 = VfxVertexBuffer.withAlpha(teal, 0.95f * fade);
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tp, hp, 0.05f * size, 0.5f * size, 3, 0, k0, k1);
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tp, hp, 0.02f * size, 0.2f * size, 2, 0, VfxVertexBuffer.withAlpha(WHITE, 0f), VfxVertexBuffer.withAlpha(WHITE, 0.9f * fade));
            buf.billboard(ctx, STAR, VfxBlend.ADD, hp, 0.9f * size, age * 0.25f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(teal, 0.4f), fade));
            VfxBloom.glow(ctx, buf, hp, 0.45f * size, teal, 0.8f * fade);
        }
        // 3. sparkles shed along the path, alternating the two substances
        RandomSource r = inst.random();
        for (int i = 0; i < 6 && buf.hasBudget(8); i++) {
            float s = r.nextFloat() * 0.9f, off = (r.nextFloat() - 0.5f) * 0.5f * size, off2 = (r.nextFloat() - 0.5f) * 0.5f * size, ph = r.nextFloat() * 6.28f;
            if (s > head - 0.02f || p < 0.12f) continue;
            Vector3f q = lerp(from, to, s).add(ctx.camRight.x * off, ctx.camRight.y * off + off2, ctx.camRight.z * off);
            float tw = Mth.clamp(1f - (head - s) * 2.2f, 0, 1) * (0.55f + 0.45f * Mth.sin(age * 0.7f + ph)) * fade;
            buf.billboard(ctx, STAR, VfxBlend.ADD, q, 0.28f * size * tw + 0.05f, age * 0.1f + ph, VfxVertexBuffer.withAlpha(i % 2 == 0 ? GOLD : teal, tw));
        }
        // 4. the flash where it lands
        if (p > 0.72f) {
            float f = Mth.clamp((p - 0.72f) / 0.28f, 0, 1);
            buf.billboard(ctx, RING, VfxBlend.ADD, to, (0.4f + 1.4f * f) * size, 0, VfxVertexBuffer.withAlpha(teal, (1f - f) * 0.9f));
            VfxBloom.glow(ctx, buf, to, 0.4f * size * (1f - f * 0.5f), WHITE, (1f - f));
        }
    }

    // ------------------------------------------------------------------ FX2: zone
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), fade = life(inst, age, 8, 12), R = Math.max(0.8f, inst.power);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        if (open <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int teal = teal(inst);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, ground, R * open, teal, 0.35f * fade);
        buf.plane(CIRCLE, VfxBlend.ADD, ground.spin(age * 0.02f), R * open, VfxVertexBuffer.withAlpha(teal, 0.95f * fade));
        buf.plane(CIRCLE, VfxBlend.ADD, ground.lift(0.01f).spin(-age * 0.035f), R * 0.62f * open, VfxVertexBuffer.withAlpha(GOLD, 0.85f * fade));
        float pulse = fract(age / 26f);
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.02f), R * (0.95f + 0.05f * pulse) * open, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(teal, 0.4f), (0.7f - 0.5f * pulse) * fade));
        // two ribbons of glyphs climbing the zone in hoops
        for (int k = 0; k < 2; k++) {
            float rise = fract(age / 60f + k * 0.5f), h = 0.15f + rise * 2.2f;
            float a = Mth.sin(rise * Mth.PI) * fade;
            float halfW = 0.16f * Math.max(1f, R / 3f);
            float bandR = R * (0.78f - 0.1f * k) * open;
            float repeats = Math.max(1, Math.round(Mth.TWO_PI * bandR / (BAND_ASPECT * halfW * 2)));
            if (a > 0.02f)
                hoop(buf, BAND, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, h, 0)), bandR, halfW, ctx.seg(16, 8), repeats, (k == 0 ? 1 : -1) * age * 0.012f,
                        VfxVertexBuffer.withAlpha(k == 0 ? teal : GOLD, 0.9f * a));
        }
        // iron spikes rising round the rim and glyph sparkles drifting up
        RandomSource r = inst.random();
        int spikes = ctx.seg(8, 4);
        for (int i = 0; i < spikes && buf.hasBudget(8); i++) {
            float a = r.nextFloat() * Mth.TWO_PI, d = (0.55f + 0.4f * r.nextFloat()) * R * open, delay = 4 + r.nextFloat() * 14, hh = (0.5f + r.nextFloat() * 0.6f) * Math.min(1.6f, 0.5f + R * 0.25f);
            float show = VfxAnim.easeOutBack(Mth.clamp((age - delay) / 7f, 0, 1));
            if (show <= 0.01f) continue;
            Vector3f base = new Vector3f(Mth.cos(a) * d, 0, Mth.sin(a) * d).add(c).add(0, hh * 0.5f * show, 0);
            buf.billboard(ctx, SPIKE, VfxBlend.ALPHA, base, hh * show * 1.6f, 0, VfxVertexBuffer.withAlpha(0xFFB8C8D6, 0.95f * fade));
        }
        for (int i = 0; i < ctx.seg(10, 4) && buf.hasBudget(4); i++) {
            float a = r.nextFloat() * Mth.TWO_PI, d = r.nextFloat() * R * 0.9f, ph = r.nextFloat();
            float rise = fract(age / 50f + ph);
            Vector3f q = new Vector3f(Mth.cos(a) * d, 0.1f + rise * 2.4f, Mth.sin(a) * d).add(c);
            buf.billboard(ctx, STAR, VfxBlend.ADD, q, 0.26f, age * 0.05f + ph * 6f, VfxVertexBuffer.withAlpha(i % 2 == 0 ? GOLD : teal, Mth.sin(rise * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ FX3: impact / signature
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.5f, 4f) * 0.55f;
        boolean flowers = (inst.color & 0xFFFFFF) == 0xFF9AD0;
        Vector3f c = ctx.rel(inst.from(ctx)), hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        int teal = flowers ? ROSE : teal(inst), gold = flowers ? 0xFFFFE0A0 : GOLD;
        float fade = Mth.clamp((1f - p) / 0.4f, 0, 1);
        // the flash and the ground
        float flash = Mth.clamp(1f - p / 0.22f, 0, 1);
        if (flash > 0.01f) VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f * size, 0), 1.1f * size, VfxVertexBuffer.whiten(teal, 0.6f), flash);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        float grow = VfxAnim.easeOutCubic(Mth.clamp(p / 0.55f, 0, 1));
        buf.plane(RING, VfxBlend.ADD, ground, (0.3f + 2.2f * grow) * size, VfxVertexBuffer.withAlpha(teal, (1f - grow * 0.8f) * fade));
        buf.plane(CIRCLE, VfxBlend.ADD, ground.lift(0.01f).spin(age * 0.1f), 1.1f * size * VfxAnim.easeOutBack(Mth.clamp(p / 0.2f, 0, 1)), VfxVertexBuffer.withAlpha(gold, 0.85f * Mth.clamp((1f - p) / 0.5f, 0, 1)));
        buf.billboard(ctx, RING, VfxBlend.ADD, new Vector3f(c).add(0, 0.5f * size, 0), (0.5f + 3.0f * grow) * size, 0, VfxVertexBuffer.withAlpha(WHITE, 0.6f * (1f - grow) * fade));
        // eight spikes (or petals) thrown outward, biased by the hint
        RandomSource r = inst.random();
        for (int i = 0; i < 8 && buf.hasBudget(8); i++) {
            float a = Mth.TWO_PI * i / 8f + r.nextFloat() * 0.5f, up = 0.4f + r.nextFloat() * 0.9f, spd = (0.9f + r.nextFloat() * 0.9f) * size;
            Vector3f d = new Vector3f(Mth.cos(a), up, Mth.sin(a));
            if (hint.lengthSquared() > 0.01f) d.add(new Vector3f(hint).normalize().mul(0.6f));
            float tt = Mth.clamp(p * (flowers ? 0.55f : 1f), 0, 1), air = VfxAnim.easeOutCubic(tt);
            Vector3f q = new Vector3f(c).add(d.x * spd * air * 1.4f, d.y * spd * air * 1.2f - (flowers ? 0.7f : 1.8f) * size * tt * tt, d.z * spd * air * 1.4f);
            float sz = (flowers ? 0.34f : 0.55f) * size * (0.7f + 0.3f * r.nextFloat());
            if (flowers) buf.billboard(ctx, PETAL, VfxBlend.ALPHA, q, sz, age * 0.15f + i, VfxVertexBuffer.withAlpha(0xFFFFB0DC, fade));
            else buf.billboard(ctx, SPIKE, VfxBlend.ALPHA, q, sz * 1.6f, pointing(ctx, new Vector3f(d.x, d.y - 1.2f * tt, d.z)), VfxVertexBuffer.withAlpha(0xFFB8C8D6, fade));
        }
        for (int i = 0; i < 6 && buf.hasBudget(4); i++) {
            float a = Mth.TWO_PI * i / 6f + 0.4f, rr = (0.4f + 1.6f * grow) * size;
            Vector3f q = new Vector3f(c).add(Mth.cos(a) * rr, 0.3f * size + 0.5f * size * p, Mth.sin(a) * rr);
            buf.billboard(ctx, STAR, VfxBlend.ADD, q, 0.4f * size, age * 0.2f + i, VfxVertexBuffer.withAlpha(i % 2 == 0 ? gold : teal, Mth.sin(Mth.clamp(p * 1.3f, 0, 1) * Mth.PI) * fade));
        }
    }
}
