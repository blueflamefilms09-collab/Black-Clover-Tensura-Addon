package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.48 Genesis Demon-Slayer: black anti-magic with a crimson rim. Textures from tools/gen_demon_slayer_textures.py.
 * <ul>
 *   <li>DEMON_METEOR (Black Meteorite): a black meteor with a crimson rim crosses 'from' -> 'to' in the first third of its life,
 *       torn void streaks and smoke behind it, shards and sparks shed off the path; a shock ring where it lands, then the path
 *       smoulders as a fading black scar with crimson embers.</li>
 *   <li>NIHILITY_ZONE: radius power round 'from' for the whole duration: a black wall rising round the edge with crimson cracks
 *       crawling up it, a cracked ground disc, a pulse ring every second, and magic motes drifting in and snuffing out.</li>
 * </ul>
 */
public class DemonSlayerLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation METEOR = t("demon_meteor"), WALL = t("demon_void_wall"), CRACKS = t("demon_cracks"), GROUND = t("demon_ground"),
            SMOKE = t("koto_smoke"), SHARD = VfxTextures.SHARD, GLOW = VfxTextures.GLOW, SPARK = VfxTextures.SPARK, RING = t("koto_ring");
    static final int CRIMSON = 0xFFC0102A, EMBER = 0xFFFF3A4A, INK = 0xFF070305;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DEMON_METEOR, VfxShape.NIHILITY_ZONE); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.NIHILITY_ZONE ? 200 : 24; }

    @Override
    public int defaultColor(VfxShape s) { return CRIMSON; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.DEMON_METEOR) meteor(inst, ctx, buf);
        else zone(inst, ctx, buf);
    }

    private void meteor(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = inst.power, travel = inst.duration * 0.33f;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        float fly = VfxAnim.easeOutCubic(Mth.clamp(age / travel, 0, 1)), fade = life(inst, age, 0, 8);
        Vector3f head = new Vector3f(a).lerp(b, fly);
        if (age < travel + 2) {
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(Math.min(len * fly, 4.5f * P)));
            DreamPaintLayer.strokePart(buf, ctx, METEOR, VfxBlend.ALPHA, tail, head, 0, 1, 1.5f * P, VfxVertexBuffer.withAlpha(INK, 0.95f));
            DreamPaintLayer.strokePart(buf, ctx, METEOR, VfxBlend.ADD, tail, head, 0, 1, 1.65f * P, VfxVertexBuffer.withAlpha(inst.color, 0.9f));
            VfxBloom.glow(ctx, buf, head, 1.6f * P, inst.color, 0.7f);
            Vector3f sd = side(dir);
            for (int k = 0; k < 4; k++) {                                       // torn void streaks round the path
                float off = (hash(inst.seed, k, 1) - 0.5f) * 1.4f * P, up = (hash(inst.seed, k, 2) - 0.5f) * 1.2f * P;
                Vector3f o = new Vector3f(sd).mul(off).add(0, up, 0);
                Vector3f s0 = new Vector3f(a).lerp(head, 0.3f + 0.3f * hash(inst.seed, k, 3)).add(o), s1 = new Vector3f(head).add(new Vector3f(o).mul(0.4f));
                streak(buf, ctx, METEOR, VfxBlend.ALPHA, s0, s1, 0.18f * P, VfxVertexBuffer.withAlpha(INK, 0.7f));
            }
        }
        for (int k = 0; k < 8; k++) {                                           // shards and sparks shed off the path
            float u = hash(inst.seed, k, 4), born = u * travel;
            if (age < born) continue;
            float tt = (age - born) / 14f;
            if (tt > 1) continue;
            Vector3f d = new Vector3f(hash(inst.seed, k, 5) - 0.5f, hash(inst.seed, k, 6) * 0.6f, hash(inst.seed, k, 7) - 0.5f).normalize().mul(1.6f * tt);
            Vector3f p = new Vector3f(a).lerp(b, u).add(d).add(0, -1.2f * tt * tt, 0);
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, p, 0.28f * P, age * 0.5f + k, VfxVertexBuffer.withAlpha(INK, 1 - tt));
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.22f * P, k, VfxVertexBuffer.withAlpha(EMBER, 1 - tt));
        }
        if (age >= travel) {                                                    // the landing: shock ring, then a smouldering scar
            float s = Mth.clamp((age - travel) / 10f, 0, 1);
            VfxPose g = VfxPose.ground(new Vector3f(b).add(0, -0.9f, 0));
            buf.ring(RING, VfxBlend.ADD, g.spin(age * 0.05f), 0.3f + 3.6f * s * P, 0.8f + 4.2f * s * P, ctx.seg(20, 10), 4, 0, VfxVertexBuffer.withAlpha(inst.color, (1 - s) * fade));
            buf.ring(SMOKE, VfxBlend.ALPHA, g, 0.2f + 3f * s * P, 1f + 4f * s * P, ctx.seg(12, 8), 2, age * 0.02f, VfxVertexBuffer.withAlpha(INK, 0.6f * (1 - s)));
            VfxBloom.glow(ctx, buf, b, 2.4f * P, inst.color, 0.9f * (1 - s));
            DreamPaintLayer.strokePart(buf, ctx, METEOR, VfxBlend.ALPHA, a, b, 0, 1, 0.35f * P, VfxVertexBuffer.withAlpha(INK, 0.6f * fade));
            DreamPaintLayer.strokePart(buf, ctx, METEOR, VfxBlend.ADD, a, b, 0, 1, 0.4f * P, VfxVertexBuffer.withAlpha(inst.color, 0.35f * fade));
        }
    }

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1)), fade = life(inst, age, 0, 20), r = R * open;
        float h = Math.min(6f, R * 0.45f);
        VfxPose mid = VfxPose.ground(new Vector3f(c).add(0, h * 0.5f, 0));
        int seg = ctx.seg(24, 12);
        hoop(buf, WALL, VfxBlend.ALPHA, mid, r, r * 0.9f, h * 0.5f, seg, 3, age * 0.004f, VfxVertexBuffer.withAlpha(INK, 0.85f * fade));
        float crawl = 0.55f + 0.45f * Mth.sin(age * 0.15f);
        hoop(buf, CRACKS, VfxBlend.ADD, mid, r * 1.002f, r * 0.902f, h * 0.5f, seg, 4, -age * 0.003f, VfxVertexBuffer.withAlpha(inst.color, 0.75f * crawl * fade));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.04f, 0));
        buf.plane(GROUND, VfxBlend.ALPHA, ground, r, VfxVertexBuffer.withAlpha(INK, 0.7f * fade));
        buf.plane(GROUND, VfxBlend.ADD, ground.lift(0.01f).spin(0.3f), r, VfxVertexBuffer.withAlpha(inst.color, 0.45f * crawl * fade));
        float pulse = (age % 20f) / 20f;                                        // a ring sweeping out every second
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.02f), r * pulse * 0.9f, r * pulse, ctx.seg(16, 10), 6, age * 0.02f, VfxVertexBuffer.withAlpha(inst.color, (1 - pulse) * 0.6f * fade));
        for (int k = 0; k < 10; k++) {                                          // magic motes drawn in and snuffed out
            float ph = (age * 0.02f + hash(inst.seed, k, 1)) % 1f, ang = hash(inst.seed, k, 2) * Mth.TWO_PI;
            float rr = r * (1 - ph);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rr, 0.5f + 2f * hash(inst.seed, k, 3), Mth.sin(ang) * rr);
            buf.billboard(ctx, GLOW, VfxBlend.ADD, p, 0.25f * (1 - ph), 0, VfxVertexBuffer.withAlpha(0xFF9AA8FF, (1 - ph) * fade));
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, p, 0.4f * ph, ang, VfxVertexBuffer.withAlpha(INK, ph * 0.6f * fade));
        }
    }
}
