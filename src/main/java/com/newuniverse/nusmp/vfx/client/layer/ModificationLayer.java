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
 * Modification Magic (Morris Libardirt), 1.93.0 rebuild: structure edited. Rune rings that carry a lattice, needles that thread through matter, clawed tendrils that climb out of the
 * ground, clean cuts, fragments of what was taken apart. Textures (tools/gen_modification_vfx_textures.py): modification_ring, _needle, _tendril, _lattice, _slice, _shard.
 * <ul>
 *   <li>MODIFICATION_FX1, CAST / PROJECTILE ("Dissection Needles"). An edit ring opens at 'from' facing the target; three needles fly 'from' to 'to' a few ticks apart, each a long
 *       pin with its ring head trailing and a hot tip leading, leaving a pale flare; a small cut flashes where the first lands. from = origin, to = target, power = size (1.0 about a
 *       block), duration = flight ticks (default 16).</li>
 *   <li>MODIFICATION_FX2, ZONE / FIELD ("Structural Unravel", "Dissection Theatre"). The edit ring turns on the ground, a hexagonal lattice turns the other way over it; six clawed
 *       tendrils climb out of the ground on the rim and sway, fragments rise and fall, a pulse ring rolls out every 28 ticks. from = centre on the ground, power = RADIUS,
 *       duration = life ticks (default 80; fades in over 8, out over 12).</li>
 *   <li>MODIFICATION_FX3, IMPACT / SIGNATURE ("Structure Shock", "Re-Structure"). An orange-white flash, the lattice bursts open as a plane standing across the hint direction, the ring
 *       as a shock wave on the ground, six cuts shoot out from the middle, ten fragments fly apart spinning. from = centre, to = optional direction hint, power = scale, duration = life
 *       ticks (default 28).</li>
 * </ul>
 * inst.color is mixed 30 percent into the orange; the pale accent (0xFFD9B0) is fixed.
 */
public class ModificationLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("modification_ring");
    public static final ResourceLocation NEEDLE = t("modification_needle");
    public static final ResourceLocation TENDRIL = t("modification_tendril");
    public static final ResourceLocation LATTICE = t("modification_lattice");
    public static final ResourceLocation SLICE = t("modification_slice");
    public static final ResourceLocation SHARD = t("modification_shard");

    private static final int ORANGE = 0xFFFF9B58, PALE = 0xFFFFD9B0, WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MODIFICATION_FX1, VfxShape.MODIFICATION_FX2, VfxShape.MODIFICATION_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.MODIFICATION_FX1 ? 16 : s == VfxShape.MODIFICATION_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return ORANGE; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MODIFICATION_FX1 -> needles(inst, ctx, buf);
            case MODIFICATION_FX2 -> zone(inst, ctx, buf);
            case MODIFICATION_FX3 -> impact(inst, ctx, buf);
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

    private static int orange(VfxInstance inst) { return VfxVertexBuffer.lerpColor(ORANGE, inst.color | 0xFF000000, 0.3f); }

    // ------------------------------------------------------------------ FX1: dissection needles
    private void needles(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.4f, 4f) * 0.55f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) return;
        dir.div(dist);
        int or = orange(inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(p / 0.2f, 0, 1)) * Mth.clamp((0.5f - p) / 0.25f, 0, 1);
        if (open > 0.02f) {
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(from, dir).spin(age * 0.2f), 0.6f * size * open, VfxVertexBuffer.withAlpha(or, 0.95f * open));
            VfxBloom.glow(ctx, buf, from, 0.4f * size, or, 0.5f * open);
        }
        Vector3f side = new Vector3f(dir).cross(0, 1, 0);
        if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
        side.normalize();
        float len = 1.5f * size;
        for (int k = 0; k < 3 && buf.hasBudget(16); k++) {
            float start = 0.12f + 0.07f * k, flight = VfxAnim.easeInOutSine(Mth.clamp((p - start) / 0.62f, 0, 1));
            if (flight <= 0.001f) continue;
            float lat = (k - 1) * 0.5f * size, fade = Mth.clamp((1.02f - p) / 0.2f, 0, 1);
            Vector3f off = new Vector3f(side).mul(lat * (1f - 0.6f * flight));
            Vector3f tip = lerp(from, to, flight).add(off);
            Vector3f tail = new Vector3f(tip).sub(new Vector3f(dir).mul(Math.min(len, dist * flight + 0.2f)));
            // the needle texture has its ring head at the top: beam(a = tip, b = tail) puts the bottom (the point) at a
            buf.beam(ctx, NEEDLE, VfxBlend.ADD, tip, tail, 0.2f * size, 0.2f * size, 1, 0, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(or, 0.5f), fade), VfxVertexBuffer.withAlpha(or, fade));
            Vector3f flareEnd = new Vector3f(tip).sub(new Vector3f(dir).mul(len * 2.6f));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tip, flareEnd, 0.12f * size, 0.01f, 2, 0, VfxVertexBuffer.withAlpha(PALE, 0.7f * fade), VfxVertexBuffer.withAlpha(or, 0f));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, tip, 0.35f * size, age * 0.3f, VfxVertexBuffer.withAlpha(WHITE, fade));
        }
        if (p > 0.7f) {
            float f = Mth.clamp((p - 0.7f) / 0.3f, 0, 1);
            Vector3f cut = new Vector3f(to).sub(new Vector3f(dir).mul(0.1f));
            buf.billboard(ctx, SLICE, VfxBlend.ADD, cut, (0.6f + 1.2f * f) * size, 0.7f, VfxVertexBuffer.withAlpha(PALE, 1f - f));
            buf.billboard(ctx, SLICE, VfxBlend.ADD, cut, (0.5f + 1.0f * f) * size, -0.7f, VfxVertexBuffer.withAlpha(or, 1f - f));
        }
    }

    // ------------------------------------------------------------------ FX2: the structure is laid bare
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), fade = life(inst, age, 8, 12), R = Math.max(0.8f, inst.power);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        if (open <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int or = orange(inst);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, ground, R * open, or, 0.3f * fade);
        buf.plane(RING, VfxBlend.ADD, ground.spin(age * 0.02f), R * open, VfxVertexBuffer.withAlpha(or, 0.95f * fade));
        buf.plane(LATTICE, VfxBlend.ADD, ground.lift(0.015f).spin(-age * 0.014f), R * 0.9f * open, VfxVertexBuffer.withAlpha(PALE, 0.55f * fade));
        float pulse = fract(age / 28f);
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.03f), R * (0.5f + 0.5f * pulse) * open, VfxVertexBuffer.withAlpha(WHITE, (0.6f - 0.5f * pulse) * fade));
        RandomSource r = inst.random();
        for (int i = 0; i < 6 && buf.hasBudget(12); i++) {                                       // clawed tendrils out of the ground on the rim
            float a = Mth.TWO_PI * i / 6f + r.nextFloat() * 0.4f, d = R * (0.72f + 0.2f * r.nextFloat());
            float show = VfxAnim.easeOutBack(Mth.clamp((age - 6f - i * 2.5f) / 10f, 0, 1)), hh = Math.min(3.2f, 1.2f + R * 0.35f) * show;
            if (show <= 0.01f) continue;
            float sway = Mth.sin(age * 0.09f + i * 1.7f) * 0.5f;
            Vector3f base = new Vector3f(Mth.cos(a) * d, 0, Mth.sin(a) * d).add(c);
            Vector3f tip = new Vector3f(base).add(-Mth.cos(a) * 0.5f + sway * -Mth.sin(a), hh, -Mth.sin(a) * 0.5f + sway * Mth.cos(a));
            // texture: the claws at the top; beam(a = base, b = tip) puts the bottom at a
            buf.beam(ctx, TENDRIL, VfxBlend.ALPHA, base, tip, 0.5f, 0.35f, 1, 0, VfxVertexBuffer.withAlpha(0xFFD8C8C0, 0.95f * fade), VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.95f * fade));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, tip, 0.35f, 0, VfxVertexBuffer.withAlpha(or, 0.6f * fade * show));
        }
        for (int i = 0; i < 6 && buf.hasBudget(4); i++) {                                        // fragments rise and fall
            float a = r.nextFloat() * Mth.TWO_PI, d = r.nextFloat() * R * 0.85f, ph = r.nextFloat(), rise = fract(age / 44f + ph);
            Vector3f q = new Vector3f(Mth.cos(a) * d, 0.2f + 1.8f * Mth.sin(rise * Mth.PI), Mth.sin(a) * d).add(c);
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, q, 0.3f, age * 0.07f + ph * 6f, VfxVertexBuffer.withAlpha(0xFFE8D4C0, Mth.sin(rise * Mth.PI) * 0.9f * fade));
        }
    }

    // ------------------------------------------------------------------ FX3: structure shock
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), size = Mth.clamp(inst.power, 0.5f, 4f) * 0.55f;
        Vector3f c = ctx.rel(inst.from(ctx)), hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        int or = orange(inst);
        float fade = Mth.clamp((1f - p) / 0.4f, 0, 1), flash = Mth.clamp(1f - p / 0.2f, 0, 1), grow = VfxAnim.easeOutCubic(Mth.clamp(p / 0.5f, 0, 1));
        Vector3f mid = new Vector3f(c).add(0, 0.6f * size, 0);
        if (flash > 0.01f) VfxBloom.glow(ctx, buf, mid, 1.1f * size, VfxVertexBuffer.whiten(or, 0.6f), flash);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        buf.plane(RING, VfxBlend.ADD, ground.spin(age * 0.06f), (0.3f + 2.1f * grow) * size, VfxVertexBuffer.withAlpha(or, (1f - grow * 0.85f) * fade));
        Vector3f face = hint.lengthSquared() > 0.01f ? new Vector3f(hint) : new Vector3f(0, 1, 0);
        buf.plane(LATTICE, VfxBlend.ADD, VfxPose.facing(mid, face).spin(age * 0.05f), (0.5f + 1.7f * grow) * size, VfxVertexBuffer.withAlpha(PALE, 0.8f * (1f - grow * 0.7f) * fade));
        RandomSource r = inst.random();
        for (int i = 0; i < 6 && buf.hasBudget(8); i++) {                                        // the cuts
            float a = Mth.TWO_PI * i / 6f + r.nextFloat() * 0.6f, len = (0.8f + 1.4f * grow) * size * (0.6f + 0.6f * r.nextFloat());
            Vector3f d = new Vector3f(Mth.cos(a), (r.nextFloat() - 0.3f) * 0.8f, Mth.sin(a));
            Vector3f e = new Vector3f(mid).add(new Vector3f(d).mul(len));
            buf.beam(ctx, SLICE, VfxBlend.ADD, mid, e, 0.3f * size, 0.06f * size, 1, 0, VfxVertexBuffer.withAlpha(WHITE, 0.95f * fade), VfxVertexBuffer.withAlpha(or, 0.5f * fade));
        }
        for (int i = 0; i < 10 && buf.hasBudget(4); i++) {
            float a = Mth.TWO_PI * i / 10f + r.nextFloat() * 0.5f, up = 0.5f + r.nextFloat() * 1.0f, spd = (0.9f + r.nextFloat() * 1.0f) * size;
            Vector3f d = new Vector3f(Mth.cos(a), up, Mth.sin(a));
            if (hint.lengthSquared() > 0.01f) d.add(new Vector3f(hint).normalize().mul(0.5f));
            float air = VfxAnim.easeOutCubic(Mth.clamp(p, 0, 1));
            Vector3f q = new Vector3f(c).add(d.x * spd * air * 1.5f, d.y * spd * air * 1.2f - 2.0f * size * p * p, d.z * spd * air * 1.5f);
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, q, 0.5f * size, age * 0.3f * (i % 2 == 0 ? 1 : -1) + i, VfxVertexBuffer.withAlpha(0xFFE8D4C0, fade));
        }
    }
}
