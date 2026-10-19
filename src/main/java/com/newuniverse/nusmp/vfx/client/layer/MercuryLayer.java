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
 * Mercury Magic (Nozel Silva), 1.93.0 rebuild of the stub: liquid metal, dense and bright, that flows, beads and rings like a pool. The older MERCURY_DOME / _SPEAR / _EAGLE shapes stay
 * registered in SlashCompassMercuryLayer; the book now draws through these three. Textures (tools/gen_mercury_vfx_textures.py): mercury_ripple, _droplet, _streak, _crown, _spearhead, _grid.
 * <ul>
 *   <li>MERCURY_FX1, CAST / PROJECTILE ("Silver Spear", "Silver Bullet", "Silver Blade" strikes, "Silver Rain" drops). A ripple opens at 'from'; a hyper-dense spear (its head from
 *       the spearhead texture, a lit core) flies 'from' to 'to' dragging a liquid tail of beaded ribbon that thins out behind it, shedding droplets that fall; a crown of splash opens
 *       where it lands. from = origin, to = target, power = size (1.0 about a block), duration = flight ticks (default 16).</li>
 *   <li>MERCURY_FX2, ZONE / FIELD ("Silver Guardian" dome, "Silver Star of Execution" cage, "Silver Rain" pool). A pool of rippling mercury spreads on the ground with two more ripple
 *       planes turning above it; a wall of liquid lattice stands on the rim as a hoop; six liquid bars rise and a ring of mercury closes on their tops; droplets rise from the pool.
 *       from = centre on the ground, power = RADIUS, duration = life ticks (default 80; fades in over 8, out over 12).</li>
 *   <li>MERCURY_FX3, IMPACT / SIGNATURE ("Silver Eagle" landing, a spear lands, the dome breaks). A white flash, a splash crown on the ground and a second one standing across the hint,
 *       three ripple rings, twelve droplets thrown out in arcs. from = centre, to = optional direction hint, power = scale, duration = life ticks (default 28).</li>
 * </ul>
 * inst.color is mixed 25 percent into the quicksilver tint (0xC8E8FF).
 */
public class MercuryLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RIPPLE = t("mercury_ripple");
    public static final ResourceLocation DROPLET = t("mercury_droplet");
    public static final ResourceLocation STREAK = t("mercury_streak");
    public static final ResourceLocation CROWN = t("mercury_crown");
    public static final ResourceLocation SPEARHEAD = t("mercury_spearhead");
    public static final ResourceLocation GRID = t("mercury_grid");

    private static final int SILVER = 0xFFC8E8FF, STEEL = 0xFF9FB4CC, WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MERCURY_FX1, VfxShape.MERCURY_FX2, VfxShape.MERCURY_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.MERCURY_FX1 ? 16 : s == VfxShape.MERCURY_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return SILVER; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MERCURY_FX1 -> spear(inst, ctx, buf);
            case MERCURY_FX2 -> zone(inst, ctx, buf);
            case MERCURY_FX3 -> impact(inst, ctx, buf);
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

    private static int silver(VfxInstance inst) { return VfxVertexBuffer.lerpColor(SILVER, inst.color | 0xFF000000, 0.25f); }

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

    // ------------------------------------------------------------------ FX1: a spear of mercury
    private void spear(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.4f, 4f) * 0.55f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) return;
        dir.div(dist);
        int si = silver(inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(p / 0.2f, 0, 1)) * Mth.clamp((0.45f - p) / 0.25f, 0, 1);
        if (open > 0.02f) {
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.facing(from, dir).spin(age * 0.1f), 0.55f * size * open, VfxVertexBuffer.withAlpha(si, 0.9f * open));
            VfxBloom.glow(ctx, buf, from, 0.4f * size, si, 0.5f * open);
        }
        float head = VfxAnim.easeInCubic(Mth.clamp((p - 0.08f) / 0.72f, 0, 1)) * 0.3f + VfxAnim.easeOutCubic(Mth.clamp((p - 0.08f) / 0.72f, 0, 1)) * 0.7f;
        float fade = Mth.clamp((1.0f - p) / 0.18f, 0, 1);
        if (head > 0.005f) {
            Vector3f tip = lerp(from, to, head);
            float spearLen = 1.3f * size, tailLen = Math.min(2.6f * size, dist * head + 0.3f);
            Vector3f back = new Vector3f(tip).sub(new Vector3f(dir).mul(spearLen)), tailEnd = new Vector3f(back).sub(new Vector3f(dir).mul(tailLen));
            // the spearhead texture has its point at the top: beam(a = back, b = tip) puts the top at b
            buf.beam(ctx, SPEARHEAD, VfxBlend.ALPHA, back, tip, 0.34f * size, 0.34f * size, 1, 0, VfxVertexBuffer.withAlpha(WHITE, fade), VfxVertexBuffer.withAlpha(WHITE, fade));
            buf.beam(ctx, STREAK, VfxBlend.ALPHA, tailEnd, back, 0.02f, 0.42f * size, 3, -age * 0.05f, VfxVertexBuffer.withAlpha(STEEL, 0f), VfxVertexBuffer.withAlpha(si, 0.9f * fade));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, back, tip, 0.12f * size, 0.22f * size, 1, 0, VfxVertexBuffer.withAlpha(WHITE, 0.5f * fade), VfxVertexBuffer.withAlpha(si, 0.9f * fade));
            VfxBloom.glow(ctx, buf, tip, 0.3f * size, si, 0.8f * fade);
            RandomSource r = inst.random();
            for (int i = 0; i < 6 && buf.hasBudget(4); i++) {                                    // droplets shed behind the spear, falling
                float s = Mth.clamp(head - 0.04f - r.nextFloat() * 0.28f, 0, 1), off = (r.nextFloat() - 0.5f) * 0.3f * size, fall = (head - s) * 1.4f * size;
                Vector3f q = lerp(from, to, s).add(off, -fall, off * 0.5f);
                buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, q, (0.16f + 0.12f * r.nextFloat()) * size, 0, VfxVertexBuffer.withAlpha(si, fade * Mth.clamp(1f - (head - s) * 3f, 0, 1)));
            }
        }
        if (p > 0.74f) {
            float f = Mth.clamp((p - 0.74f) / 0.26f, 0, 1);
            buf.billboard(ctx, CROWN, VfxBlend.ALPHA, to, (0.5f + 1.4f * f) * size, 0, VfxVertexBuffer.withAlpha(si, 1f - f));
            VfxBloom.glow(ctx, buf, to, 0.4f * size * (1f - f * 0.5f), WHITE, 1f - f);
        }
    }

    // ------------------------------------------------------------------ FX2: the pool, the wall, the bars
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), fade = life(inst, age, 8, 12), R = Math.max(0.8f, inst.power);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        if (open <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int si = silver(inst);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, ground, R * open, si, 0.3f * fade);
        buf.plane(RIPPLE, VfxBlend.ALPHA, ground.spin(age * 0.01f), R * open, VfxVertexBuffer.withAlpha(0xFFB8CCE0, 0.85f * fade));
        buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.012f).spin(-age * 0.02f), R * 0.8f * open, VfxVertexBuffer.withAlpha(si, 0.6f * fade));
        float pulse = fract(age / 24f);
        buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.02f), R * (0.2f + 0.8f * pulse) * open, VfxVertexBuffer.withAlpha(WHITE, (0.7f - 0.6f * pulse) * fade));
        // the wall: a hoop of liquid lattice standing on the rim, it rises over the first 14 ticks
        float wall = VfxAnim.easeOutCubic(Mth.clamp(age / 14f, 0, 1)), hh = Math.min(3.2f, 1.2f + R * 0.5f) * wall;
        if (hh > 0.05f)
            hoop(buf, GRID, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c).add(0, hh * 0.5f, 0)), R * 0.97f * open, hh * 0.5f, ctx.seg(16, 8),
                    Math.max(2, Math.round(Mth.TWO_PI * R / Math.max(0.5f, hh))), age * 0.004f, VfxVertexBuffer.withAlpha(si, 0.5f * fade));
        // the bars and the ring that closes on them
        for (int i = 0; i < 6 && buf.hasBudget(8); i++) {
            float a = Mth.TWO_PI * i / 6f, rise = VfxAnim.easeOutBack(Mth.clamp((age - 4f - i * 2f) / 10f, 0, 1));
            if (rise <= 0.01f) continue;
            float h = Math.min(3.4f, 1.4f + R * 0.5f) * rise;
            Vector3f base = new Vector3f(Mth.cos(a) * R * 0.97f, 0, Mth.sin(a) * R * 0.97f).add(c), top = new Vector3f(base).add(0, h, 0);
            buf.beam(ctx, STREAK, VfxBlend.ALPHA, base, top, 0.3f, 0.2f, 2, age * 0.03f, VfxVertexBuffer.withAlpha(si, 0.95f * fade), VfxVertexBuffer.withAlpha(WHITE, 0.95f * fade));
            buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, top, 0.3f, 0, VfxVertexBuffer.withAlpha(WHITE, fade * rise));
        }
        float ringUp = Mth.clamp((age - 14f) / 12f, 0, 1);
        if (ringUp > 0.01f) {
            float h = Math.min(3.4f, 1.4f + R * 0.5f);
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, h, 0)).spin(age * 0.03f), R * 0.97f * ringUp, VfxVertexBuffer.withAlpha(si, 0.5f * fade * ringUp));
        }
        RandomSource r = inst.random();
        for (int i = 0; i < 8 && buf.hasBudget(4); i++) {                                        // drops rising out of the pool
            float a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.9f, ph = r.nextFloat(), rise = fract(age / 40f + ph);
            Vector3f q = new Vector3f(Mth.cos(a) * d, 0.1f + 2.0f * Mth.sin(rise * Mth.PI * 0.5f), Mth.sin(a) * d).add(c);
            buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, q, 0.2f, 0, VfxVertexBuffer.withAlpha(si, Mth.sin(rise * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ FX3: a splash
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.5f, 4f) * 0.55f;
        Vector3f c = ctx.rel(inst.from(ctx)), hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        int si = silver(inst);
        float fade = Mth.clamp((1f - p) / 0.45f, 0, 1), flash = Mth.clamp(1f - p / 0.2f, 0, 1), grow = VfxAnim.easeOutCubic(Mth.clamp(p / 0.5f, 0, 1));
        Vector3f mid = new Vector3f(c).add(0, 0.4f * size, 0);
        if (flash > 0.01f) VfxBloom.glow(ctx, buf, mid, 1.0f * size, WHITE, flash);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        buf.plane(CROWN, VfxBlend.ALPHA, ground, (0.3f + 1.8f * grow) * size, VfxVertexBuffer.withAlpha(si, 0.95f * fade));
        Vector3f face = hint.lengthSquared() > 0.01f ? new Vector3f(hint) : new Vector3f(0, 0, 1);
        buf.plane(CROWN, VfxBlend.ALPHA, VfxPose.facing(mid, face), (0.2f + 1.1f * grow) * size, VfxVertexBuffer.withAlpha(WHITE, 0.8f * (1f - grow * 0.6f) * fade));
        for (int k = 0; k < 3; k++) {
            float rp = Mth.clamp((p - k * 0.12f) / 0.6f, 0, 1);
            if (rp > 0.01f && rp < 1f) buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.01f + 0.005f * k), (0.2f + 2.2f * VfxAnim.easeOutCubic(rp)) * size, VfxVertexBuffer.withAlpha(si, (1f - rp) * 0.8f));
        }
        RandomSource r = inst.random();
        for (int i = 0; i < 12 && buf.hasBudget(4); i++) {
            float a = Mth.TWO_PI * i / 12f + r.nextFloat() * 0.5f, up = 0.7f + r.nextFloat() * 1.3f, spd = (0.8f + r.nextFloat() * 0.9f) * size;
            Vector3f d = new Vector3f(Mth.cos(a), up, Mth.sin(a));
            if (hint.lengthSquared() > 0.01f) d.add(new Vector3f(hint).normalize().mul(0.5f));
            float air = VfxAnim.easeOutCubic(Mth.clamp(p, 0, 1));
            Vector3f q = new Vector3f(c).add(d.x * spd * air * 1.3f, d.y * spd * air * 1.2f - 2.4f * size * p * p, d.z * spd * air * 1.3f);
            buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, q, (0.2f + 0.2f * r.nextFloat()) * size, 0, VfxVertexBuffer.withAlpha(si, fade));
        }
    }
}
