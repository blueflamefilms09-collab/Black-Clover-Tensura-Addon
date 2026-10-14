package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * 1.35.0 Lucius Zogratis (the divine and the soul): gold light that turns purple as he enrages. The tint is the payload colour (gold, a gold-purple
 * blend, or purple: the server picks it from his power); the layer only adds a lighter core of the same hue.
 * <ul>
 *   <li>DIVINE_PILLAR: a pillar of light at 'from' with halo rings climbing it; power = radius. Telegraph form (seed bit 0): a thin dim column
 *       and a sigil on the ground; strike form: the full column flashes.</li>
 *   <li>DIVINE_HALO: a halo and fanned feather blades behind the followed body (from = the head); power = size.</li>
 *   <li>SOUL_VORTEX: soul wisps spiralling in to 'from'; power = radius.</li>
 *   <li>DIVINE_RAY: a beam from 'from' to 'to'; power = width. Telegraph: a thin pulsing line; strike: a wide beam with flashes at both ends.</li>
 *   <li>DIVINE_SIGIL: a holy sigil on the ground at 'from', turning, its rim closing over the life; light rays stand up round it in the last
 *       third (not in the telegraph form); power = radius.</li>
 * </ul>
 */
public class ZogratisDivineLayer extends AbstractVfxLayer {
    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.DIVINE_PILLAR, VfxShape.DIVINE_HALO, VfxShape.SOUL_VORTEX, VfxShape.DIVINE_RAY, VfxShape.DIVINE_SIGIL);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DIVINE_PILLAR -> 20;
            case DIVINE_HALO -> 40;
            case SOUL_VORTEX -> 40;
            case DIVINE_RAY -> 12;
            default -> 40;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFD24A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DIVINE_PILLAR -> pillar(inst, ctx, buf);
            case DIVINE_HALO -> halo(inst, ctx, buf);
            case SOUL_VORTEX -> vortex(inst, ctx, buf);
            case DIVINE_RAY -> ray(inst, ctx, buf);
            case DIVINE_SIGIL -> sigil(inst, ctx, buf);
            default -> { }
        }
    }

    private void pillar(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(0.8f, inst.power);
        boolean tell = telegraph(inst);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = ElementFx.life(inst, age, 3, Math.min(10, inst.duration / 2f));
        float H = 16f, open = outCubic(Mth.clamp(age / 5f, 0f, 1f));
        float w = R * (tell ? 0.22f : 1.0f) * open * (1f - 0.35f * ramp(t, 0.5f, 1f));
        float al = (tell ? 0.4f : 0.9f) * fade;
        for (int k = 0; k < 2; k++) {                                                                              // two crossed planes
            float ang = k * Mth.HALF_PI + age * 0.02f;
            Vector3f side = new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang));
            Vector3f lo = new Vector3f(c).sub(new Vector3f(side).mul(w)), hi = new Vector3f(c).add(new Vector3f(side).mul(w));
            float sc = -age * 0.03f;
            buf.quad(PILLAR, VfxBlend.ADD, lo, hi, new Vector3f(hi).add(0, H, 0), new Vector3f(lo).add(0, H, 0), 0, sc, 1, sc + 1.2f, a(col, al), a(col, al * 0.15f));
        }
        Vector3f s0 = new Vector3f(1, 0, 0);
        buf.quad(PILLAR, VfxBlend.ADD, new Vector3f(c).sub(new Vector3f(s0).mul(w * 0.4f)), new Vector3f(c).add(new Vector3f(s0).mul(w * 0.4f)),
                new Vector3f(c).add(new Vector3f(s0).mul(w * 0.4f)).add(0, H, 0), new Vector3f(c).sub(new Vector3f(s0).mul(w * 0.4f)).add(0, H, 0),
                0, 0, 1, 1, a(WHITE, al * 0.9f), a(hot(col, 0.5f), al * 0.1f));
        ground(buf, DIVINE_SIGIL, VfxBlend.ADD, c, 0.04f, R * (tell ? 1f : 1.15f) * open, age * 0.04f, a(col, 0.8f * fade));
        if (!tell) {
            for (int k = 0; k < 3; k++) {
                float ph = (age * 0.08f + k / 3f) % 1f;
                ground(buf, HALO, VfxBlend.ADD, c, 0.3f + ph * 9f, R * (0.9f + 0.7f * ph), age * 0.1f, a(hot(col, 0.4f), 0.7f * fade * (1f - ph)));
            }
            float flash = 1f - ramp(t, 0f, 0.25f);
            if (flash > 0f) glow(buf, ctx, new Vector3f(c).add(0, 0.8f, 0), R * (1.5f + 2f * flash), a(WHITE, 0.6f * flash));
        }
        int n = ctx.seg(8, 3);
        for (int k = 0; k < n; k++) {
            float ph = (age * 0.05f + hash(inst.seed, k, 1)) % 1f, ang = hash(inst.seed, k, 2) * Mth.TWO_PI;
            float rad = R * (0.3f + 0.7f * hash(inst.seed, k, 3));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, ph * 9f, Mth.sin(ang) * rad);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.26f, age * 0.2f, a(k % 2 == 0 ? col : WHITE, 0.9f * fade * (1f - ph)));
        }
    }

    private void halo(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), S = Math.max(0.5f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = ElementFx.life(inst, age, 8, Math.min(14, inst.duration / 3f)), pulse = 0.8f + 0.2f * Mth.sin(age * 0.25f);
        buf.billboard(ctx, HALO, VfxBlend.ADD, new Vector3f(c).add(0, 0.2f * S, 0), S * 2.2f * pulse, age * 0.015f, a(col, 0.9f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, new Vector3f(c).add(0, 0.2f * S, 0), S * 2.6f, 0f, a(hot(col, 0.4f), 0.25f * fade * pulse));
        int blades = ctx.seg(6, 4);
        for (int k = 0; k < blades; k++) {                                                                         // blades fanned in two wings
            int side = k % 2 == 0 ? 1 : -1;
            float f = (k / 2 + 1f) / (blades / 2f + 0.5f);
            float ang = Mth.HALF_PI * (0.25f + 0.85f * f);
            Vector3f dirv = new Vector3f(ctx.camRight).mul(side * Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * 0.6f - 0.15f));
            Vector3f p = new Vector3f(c).add(new Vector3f(dirv).mul(S * (1.7f + 0.6f * f)));
            float flap = 1f + 0.06f * Mth.sin(age * 0.2f + k);
            along(buf, ctx, FEATHER, VfxBlend.ADD, p, dirv, S * (0.9f + 0.35f * f) * flap, a(k % 3 == 0 ? WHITE : col, 0.8f * fade));
        }
    }

    private void vortex(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = ElementFx.life(inst, age, 6, Math.min(10, inst.duration / 3f));
        ground(buf, UMBRA, VfxBlend.ALPHA, c, 0.03f, R * 0.6f, 0f, a(0xFF0A0418, 0.5f * fade));
        ground(buf, WARP_DISC, VfxBlend.ADD, c, 0.05f, R, -age * 0.06f, a(col, 0.45f * fade));
        int n = ctx.seg(16, 6);
        for (int k = 0; k < n; k++) {
            float ph = (age * 0.03f + hash(inst.seed, k, 1)) % 1f;
            float rad = R * (1f - ph * 0.95f), ang = hash(inst.seed, k, 2) * Mth.TWO_PI + ph * 5f;
            float h = 0.3f + ph * 2.2f * (1f + hash(inst.seed, k, 3));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, h, Mth.sin(ang) * rad);
            Vector3f v = new Vector3f(-Mth.sin(ang), 0.4f, Mth.cos(ang)).sub(new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang)).mul(0.6f));
            along(buf, ctx, WISP, VfxBlend.ADD, p, v, 1.6f + 0.8f * ph, a(k % 4 == 0 ? WHITE : col, 0.9f * fade * Mth.sin(Mth.PI * ph)));
        }
        glow(buf, ctx, new Vector3f(c).add(0, 1f, 0), R * 0.35f * (0.6f + t), a(hot(col, 0.5f), 0.3f * fade));
    }

    private void ray(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), W = Math.max(0.2f, inst.power);
        boolean tell = telegraph(inst);
        int col = inst.color | 0xFF000000;
        Vector3f a0 = ctx.rel(inst.from(ctx)), b0 = ctx.rel(inst.to(ctx));
        if (new Vector3f(b0).sub(a0).lengthSquared() < 0.1f) return;
        float fade = ElementFx.life(inst, age, 2, Math.min(6, inst.duration / 3f));
        if (tell) {
            float pulse = 0.6f + 0.4f * Mth.sin(age * 0.6f);
            ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, a0, b0, 0.35f * W + 0.1f, a(col, 0.7f * fade * pulse));
            glow(buf, ctx, a0, 0.7f * W + 0.3f, a(hot(col, 0.5f), 0.8f * fade));
            ground(buf, LENS, VfxBlend.ADD, new Vector3f(b0).add(0, -0.8f, 0), 0.04f, 0.7f * W + 0.3f, 0f, a(col, 0.6f * fade * pulse));
            return;
        }
        float open = outCubic(Mth.clamp(t * 4f, 0f, 1f)) * (1f - ease(ramp(t, 0.5f, 1f)));
        ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, a0, b0, 1.9f * W * open, a(col, 0.55f * fade));
        ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, a0, b0, 0.9f * W * open, a(hot(col, 0.6f), 0.95f * fade));
        ElementFx.streak(buf, ctx, BAND, VfxBlend.ADD, a0, b0, 0.35f * W * open, a(WHITE, fade));
        glow(buf, ctx, a0, 1.6f * W * open + 0.4f, a(WHITE, 0.8f * fade));
        glow(buf, ctx, b0, 2.2f * W * open + 0.4f, a(hot(col, 0.6f), 0.8f * fade));
        int n = ctx.seg(6, 3);
        for (int k = 0; k < n; k++) {
            float s = (hash(inst.seed, k, 1) + age * 0.12f) % 1f;
            Vector3f p = new Vector3f(a0).lerp(b0, s).add((hash(inst.seed, k, 2) - 0.5f) * W, (hash(inst.seed, k, 3) - 0.5f) * W, 0f);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.3f, age * 0.3f, a(WHITE, 0.9f * fade));
        }
    }

    private void sigil(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        boolean tell = telegraph(inst);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = ElementFx.life(inst, age, 6, Math.min(10, inst.duration / 3f));
        float open = outCubic(Mth.clamp(age / 8f, 0f, 1f)), r = R * open;
        ground(buf, DIVINE_SIGIL, VfxBlend.ADD, c, 0.05f, r, age * 0.03f, a(col, (tell ? 0.6f : 0.95f) * fade));
        ground(buf, DIVINE_SIGIL, VfxBlend.ADD, c, 0.06f, r * 0.62f, -age * 0.05f, a(hot(col, 0.5f), 0.5f * fade));
        ground(buf, HALO, VfxBlend.ADD, c, 0.07f, R * (1f - 0.88f * t) / 0.62f, 0f, a(WHITE, 0.7f * fade));         // closing ring: time left
        if (!tell && t > 0.55f) {
            float up = ease(ramp(t, 0.55f, 0.95f));
            int n = ctx.seg(10, 4);
            for (int k = 0; k < n; k++) {
                float ang = (k + hash(inst.seed, k, 1) * 0.4f) / n * Mth.TWO_PI, rad = R * (0.25f + 0.7f * hash(inst.seed, k, 2));
                Vector3f base = new Vector3f(c).add(Mth.cos(ang) * rad, 0f, Mth.sin(ang) * rad);
                float h = (3f + 3f * hash(inst.seed, k, 3)) * up;
                float an = ctxAngle(ctx, base), dx = 0.35f * Mth.cos(an), dz = 0.35f * Mth.sin(an);
                buf.quad(PILLAR, VfxBlend.ADD, new Vector3f(base).sub(dx, 0, dz), new Vector3f(base).add(dx, 0, dz),
                        new Vector3f(base).add(dx, h, dz), new Vector3f(base).add(-dx, h, -dz), 0, 0, 1, 1, a(col, 0.8f * fade), a(col, 0.1f * fade));
            }
        }
    }

    /** The horizontal angle perpendicular to the view of a point (so a standing quad faces the camera). */
    private static float ctxAngle(VfxRenderContext ctx, Vector3f p) {
        return (float) Math.atan2(p.x, -p.z);
    }
}
