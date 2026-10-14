package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * 1.35.0 Zenon Zogratis (Astaroth, space): spatial rifts, portals and glass-crack lines, cold white-cyan with a violet fringe.
 * <ul>
 *   <li>SPACE_CRACK: a pane of cracked space at 'from' facing 'to' (seed bit 1 = lying on the ground); power = size. The web of cracks spreads
 *       and holds, a second web behind it turns the other way; in the last quarter it shatters into shards.</li>
 *   <li>SPACE_TEAR: a slit torn in space from 'from' to 'to'; power = width. It opens, glows along its rim and closes; shards at both ends.</li>
 *   <li>SPACE_RINGGATE: a spinning portal ring at 'from' facing 'to' with a dark heart; power = radius.</li>
 *   <li>SPACE_SHATTER: space shatters at 'from': a flash, three tilted rings of glass running out, shards thrown; power = radius.</li>
 * </ul>
 */
public class ZogratisSpaceLayer extends AbstractVfxLayer {
    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.SPACE_CRACK, VfxShape.SPACE_TEAR, VfxShape.SPACE_RINGGATE, VfxShape.SPACE_SHATTER);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SPACE_CRACK -> 30;
            case SPACE_TEAR -> 20;
            case SPACE_RINGGATE -> 40;
            default -> 20;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFB8E8FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SPACE_CRACK -> crack(inst, ctx, buf);
            case SPACE_TEAR -> tear(inst, ctx, buf);
            case SPACE_RINGGATE -> gate(inst, ctx, buf);
            case SPACE_SHATTER -> shatter(inst, ctx, buf);
            default -> { }
        }
    }

    private static final int FRINGE = 0xFFA070FF;

    private void crack(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), S = Math.max(0.5f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f n = variant(inst) ? new Vector3f(0, 1, 0) : dir(c, to);
        float open = outCubic(Mth.clamp(age / 6f, 0f, 1f)), shatter = ramp(t, 0.72f, 1f);
        float fade = ElementFx.life(inst, age, 2, Math.min(8, inst.duration / 3f)) * (1f - 0.4f * shatter);
        float half = S * open * (1f + 0.08f * shatter);
        float spin = hash(inst.seed, 0, 1) * Mth.TWO_PI;
        facing(buf, UMBRA, VfxBlend.ALPHA, c, n, half * 0.95f, 0f, a(0xFF1A2030, 0.28f * fade));                 // the pane
        facing(buf, GLASS_WEB, VfxBlend.ADD, c, n, half, spin, a(col, 0.9f * fade));
        facing(buf, GLASS_WEB, VfxBlend.ADD, new Vector3f(c).add(new Vector3f(n).mul(0.06f)), n, half * 0.78f, -spin * 1.7f + age * 0.01f, a(FRINGE, 0.5f * fade));
        float flash = 1f - ramp(t, 0f, 0.2f);
        if (flash > 0f) glow(buf, ctx, c, S * (0.5f + flash), a(WHITE, 0.7f * flash));
        if (shatter > 0f) {
            int k = ctx.seg(8, 4);
            for (int i = 0; i < k; i++) {
                float ang = hash(inst.seed, i, 3) * Mth.TWO_PI, rr = half * (0.3f + 0.7f * hash(inst.seed, i, 4)) + shatter * S * 0.9f;
                Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rr, Mth.sin(ang * 1.3f) * rr * 0.6f - shatter * shatter * 1.2f, Mth.sin(ang) * rr);
                buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.4f, age * 0.3f + i, a(i % 2 == 0 ? col : FRINGE, 1f - shatter));
            }
        }
    }

    private void tear(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), W = Math.max(0.3f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f a0 = ctx.rel(inst.from(ctx)), b0 = ctx.rel(inst.to(ctx));
        float len = new Vector3f(b0).sub(a0).length();
        if (len < 0.3f) return;
        float open = ease(ramp(t, 0f, 0.3f)) * (1f - ease(ramp(t, 0.75f, 1f)));
        float w = W * open;
        ElementFx.streak(buf, ctx, TEAR, VfxBlend.ALPHA, a0, b0, w * 1.6f, a(0xFF020208, 0.92f * open));         // the dark inside
        ElementFx.streak(buf, ctx, TEAR, VfxBlend.ADD, a0, b0, w * 1.6f, a(col, 0.95f * open));
        ElementFx.streak(buf, ctx, TEAR, VfxBlend.ADD, a0, b0, w * 2.6f, a(FRINGE, 0.35f * open));
        int n = ctx.seg(6, 3);
        for (int k = 0; k < n; k++) {                                                                             // glints crawling on the rim
            float s = (hash(inst.seed, k, 1) + age * 0.03f * (1f + hash(inst.seed, k, 2))) % 1f;
            Vector3f p = new Vector3f(a0).lerp(b0, s);
            glow(buf, ctx, p, 0.3f + 0.3f * W, a(WHITE, 0.8f * open));
        }
        for (int e = 0; e < 2; e++) {
            Vector3f end = e == 0 ? a0 : b0;
            for (int k = 0; k < 3; k++) {
                float ang = hash(inst.seed, k + 5 * e, 3) * Mth.TWO_PI, dd = (0.3f + 0.8f * hash(inst.seed, k, 4)) * W * outCubic(t * 1.5f);
                Vector3f p = new Vector3f(end).add(Mth.cos(ang) * dd, Mth.sin(ang) * dd, Mth.sin(ang * 2f) * dd);
                buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.25f, age * 0.2f + k, a(col, open));
            }
        }
    }

    private void gate(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(0.8f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f n = dir(c, to);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0f, 1f)), fade = ElementFx.life(inst, age, 3, Math.min(10, inst.duration / 3f));
        float r = R * open;
        facing(buf, UMBRA, VfxBlend.ALPHA, c, n, r * 0.9f, 0f, a(BLACK, 0.95f * fade));
        facing(buf, PORTAL, VfxBlend.ADD, c, n, r, age * 0.12f, a(col, 0.95f * fade));
        facing(buf, LENS, VfxBlend.ADD, new Vector3f(c).add(new Vector3f(n).mul(0.05f)), n, r * 1.18f / 0.9f, -age * 0.05f, a(FRINGE, 0.6f * fade));
        glow(buf, ctx, c, r * 0.5f, a(hot(col, 0.5f), 0.35f * fade));
        VfxPose p = VfxPose.facing(c, n);
        int k = ctx.seg(8, 4);
        for (int i = 0; i < k; i++) {
            float ph = (age * 0.03f + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI + age * 0.08f;
            Vector3f pos = p.point(Mth.cos(ang) * r * (1.1f - 0.1f * ph), Mth.sin(ang) * r * (1.1f - 0.1f * ph)).add(new Vector3f(n).mul(ph * 0.8f));
            buf.billboard(ctx, SPARK, VfxBlend.ADD, pos, 0.25f, age * 0.2f, a(WHITE, 0.85f * fade * (1f - ph)));
        }
    }

    private void shatter(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), R = Math.max(1f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.9f, 0);
        float e = outCubic(Mth.clamp(t * 1.5f, 0f, 1f)), fade = 1f - ramp(t, 0.5f, 1f);
        float flash = 1f - ramp(t, 0f, 0.25f);
        if (flash > 0f) glow(buf, ctx, c, R * (0.8f + flash), a(WHITE, flash));
        Vector3f[] normals = {new Vector3f(0, 1, 0), new Vector3f(1, 0.2f, 0), new Vector3f(0.2f, 0.4f, 1)};
        for (int k = 0; k < 3; k++) {
            VfxPose pose = VfxPose.facing(c, normals[k]).spin(hash(inst.seed, k, 1) * Mth.TWO_PI);
            buf.plane(GLASS_WEB, VfxBlend.ADD, pose, R * e * (0.7f + 0.15f * k), a(k == 1 ? FRINGE : col, 0.8f * fade));
        }
        ground(buf, LENS, VfxBlend.ADD, new Vector3f(c).add(0, -0.85f, 0), 0.02f, R * e / 0.9f, 0f, a(col, 0.8f * fade));
        int n = ctx.seg(12, 5);
        for (int i = 0; i < n; i++) {
            float ang = hash(inst.seed, i, 3) * Mth.TWO_PI, el = (hash(inst.seed, i, 4) - 0.3f) * 1.4f, sp = 0.5f + hash(inst.seed, i, 5);
            float d = R * 0.9f * sp * e;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * Mth.cos(el) * d, Mth.sin(el) * d - t * t * 1.5f, Mth.sin(ang) * Mth.cos(el) * d);
            buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.35f, age * 0.4f + i, a(i % 3 == 0 ? FRINGE : col, fade));
        }
    }
}
