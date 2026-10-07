package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Glass Magic (Verre Fleur, Verre Epee, Verre Detection, Le Chateau de Verre), drawn after the owner's still of Verre Detection:
 * an almost clear, faceted crystal solid with razor-thin white edge lines, a soft white bloom hugging its silhouette and pale light
 * shafts behind it. Everything is white-blue clear glass (palette 0x8AC0D0 halo, 0xE0FFFF edge); never opaque, never coloured fire.
 * <ul>
 *   <li>GLASS_FX1 (cast / projectile): a ring of tiny shards spirals into 'from' while a four-point glint and a small spinning
 *       crystal gather; then one long needle shard flies to 'to' with three offset afterimage shards spiralling behind it and a
 *       soft light-shaft trail; at 'to' a glint, a fracture web and a puff of splinters. 'from' = hand, 'to' = target,
 *       power = size (0.6 to 3), duration = flight ticks.</li>
 *   <li>GLASS_FX2 (zone / dome): a hex-faceted ground ring turning one way over a second ring turning the other, a caustic light
 *       net on the floor, a wall of clear crystal prisms growing around the rim with a light shaft behind each, a pulse ring
 *       sweeping outward, glints rising, and a hovering crystal in the middle. 'from' = ground centre, power = radius.</li>
 *   <li>GLASS_FX3 (impact / burst / signature): a white flash with a big glint, a faceted crystal that swells and shatters, a
 *       fracture web, expanding facet rings, needle shards and splinters flying outward (biased along 'to - from'), glints, light
 *       columns and a lingering bloom. 'from' = centre, 'to' = direction hint, power = scale.</li>
 * </ul>
 * 'color' (inst.color) tints the halo; the white edge colour is always kept, so the effect reads as glass whatever the tint.
 */
public class GlassLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation FACET = t("glass_facet");
    public static final ResourceLocation SHARD = t("glass_shard");
    public static final ResourceLocation PANE = t("glass_pane");
    public static final ResourceLocation CRACK = t("glass_crack");
    public static final ResourceLocation SHAFT = t("glass_shaft");
    public static final ResourceLocation GLINT = t("glass_glint");
    public static final ResourceLocation RING = t("glass_ring");
    public static final ResourceLocation DUST = t("glass_dust");
    public static final ResourceLocation CAUSTIC = t("glass_caustic");
    public static final ResourceLocation PRISM = t("glass_prism");

    private static final int EDGE = 0xFFE0FFFF;
    private static final int HALO = 0xFF8AC0D0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.GLASS_FX1, VfxShape.GLASS_FX2, VfxShape.GLASS_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case GLASS_FX1 -> 16;
            case GLASS_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFE0FFFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.GLASS_FX3) VfxShake.add(inst.payload.from(), 0.7f * Mth.clamp(inst.power, 0.6f, 3f), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case GLASS_FX1 -> cast(inst, ctx, buf);
            case GLASS_FX2 -> zone(inst, ctx, buf);
            case GLASS_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static float sat(float v) { return Mth.clamp(v, 0f, 1f); }

    private static float life(VfxInstance inst, float age, float in, float out) {
        return Math.min(sat(age / in), sat((inst.duration - age) / out));
    }

    /** Halo tint: the magic's pale blue mixed half and half with the spell's colour. */
    private static int halo(VfxInstance inst) { return VfxVertexBuffer.lerpColor(HALO, inst.color | 0xFF000000, 0.4f); }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    private static Vector3f along(Vector3f p, Vector3f d, float k) { return new Vector3f(d).mul(k).add(p); }

    /** A shard whose tip points along 'tip' (tail end at p - dir*len*0.5 ... head at p + dir*len*0.5). */
    private static void shard(VfxVertexBuffer buf, VfxRenderContext ctx, VfxBlend blend, Vector3f p, Vector3f dir, float len, float width, int col) {
        Vector3f tail = along(p, dir, -len * 0.5f), head = along(p, dir, len * 0.5f);
        buf.beam(ctx, SHARD, blend, tail, head, width, width, 1, 0f, col, col);
    }

    // ------------------------------------------------------------------ FX1: cast / projectile
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float s = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f f = ctx.rel(inst.from(ctx)), g = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(g).sub(f);
        if (dir.lengthSquared() < 1e-4f) dir.set(0, 0, 1);
        dir.normalize();
        int tint = halo(inst);
        RandomSource rnd = inst.random();
        float spin0 = rnd.nextFloat() * Mth.TWO_PI;

        // 1. charge at the hand: shards spiral inward, a glint and a small crystal gather
        float q = sat(t / 0.25f);
        if (t < 0.32f) {
            float fade = 1f - sat((t - 0.25f) / 0.07f);
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, f, 0.9f * s * q, 0f, VfxVertexBuffer.withAlpha(tint, 0.55f * fade));
            for (int i = 0; i < 4; i++) {
                float a = spin0 + Mth.TWO_PI * i / 4f + q * 3.2f;
                float r = (1f - VfxAnim.easeOutCubic(q)) * 0.9f * s + 0.08f * s;
                Vector3f p = new Vector3f(f).add(new Vector3f(ctx.camRight).mul(Mth.cos(a) * r)).add(new Vector3f(ctx.camUp).mul(Mth.sin(a) * r));
                buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.42f * s, a + Mth.HALF_PI + Mth.PI, VfxVertexBuffer.withAlpha(EDGE, fade * q));
            }
            buf.billboard(ctx, FACET, VfxBlend.ADD, f, 0.55f * s * VfxAnim.easeOutBack(q), age * 0.25f, VfxVertexBuffer.withAlpha(EDGE, fade));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, f, 1.3f * s * q, age * 0.1f, VfxVertexBuffer.withAlpha(EDGE, fade));
        }

        // 2. the flight
        float fl = sat((t - 0.2f) / 0.7f);
        if (t >= 0.2f && t < 0.95f) {
            float e = fl * fl * (3f - 2f * fl) * 0.4f + fl * 0.6f;
            Vector3f head = lerp(f, g, e);
            float len = 1.5f * s;
            float out = 1f - sat((t - 0.9f) / 0.05f);
            // light-shaft trail (bright at the head)
            Vector3f tailEnd = along(head, dir, -Math.min(len * 2.4f, new Vector3f(head).sub(f).length() + 0.4f * s));
            buf.beam(ctx, SHAFT, VfxBlend.ADD, head, tailEnd, 0.7f * s, 0.2f * s, 1, 0f, VfxVertexBuffer.withAlpha(tint, 0.85f * out), VfxVertexBuffer.withAlpha(tint, 0.1f));
            // afterimage shards, spiralling and fading
            for (int k = 3; k >= 1; k--) {
                float ph = age * 0.55f + spin0 + k * 2.1f;
                float off = 0.16f * s * k;
                Vector3f p = along(head, dir, -len * (0.55f + 0.42f * k));
                p.add(new Vector3f(ctx.camRight).mul(Mth.cos(ph) * off)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ph) * off));
                shard(buf, ctx, VfxBlend.ADD, p, dir, len * (0.85f - 0.1f * k), 0.26f * s, VfxVertexBuffer.withAlpha(tint, 0.5f / k * out));
            }
            // main needle: a clear shard with its white core
            Vector3f mid = along(head, dir, -len * 0.5f);
            shard(buf, ctx, VfxBlend.ADD, mid, dir, len, 0.46f * s, VfxVertexBuffer.withAlpha(tint, 0.9f * out));
            shard(buf, ctx, VfxBlend.ADD, mid, dir, len * 0.96f, 0.3f * s, VfxVertexBuffer.withAlpha(EDGE, out));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, 0.75f * s, 0f, VfxVertexBuffer.withAlpha(tint, 0.6f * out));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, head, 0.9f * s, age * 0.3f, VfxVertexBuffer.withAlpha(EDGE, out));
            // shed splinters
            for (int i = 0; i < 3; i++) {
                float back = (0.3f + 0.7f * ((i * 0.37f + age * 0.09f) % 1f));
                Vector3f p = along(head, dir, -len * (0.8f + 1.4f * back));
                float sw = (i - 1) * 0.35f * s * back;
                p.add(new Vector3f(ctx.camUp).mul(sw));
                buf.billboard(ctx, DUST, VfxBlend.ADD, p, 0.5f * s, spin0 + i, VfxVertexBuffer.withAlpha(EDGE, (1f - back) * 0.8f * out));
            }
        }

        // 3. impact flash at the target
        if (t > 0.82f) {
            float u = sat((t - 0.82f) / 0.18f);
            float fade = 1f - u * u;
            buf.billboard(ctx, GLINT, VfxBlend.ADD, g, 2.0f * s * VfxAnim.easeOutCubic(u) + 0.4f * s, spin0, VfxVertexBuffer.withAlpha(EDGE, fade));
            buf.billboard(ctx, CRACK, VfxBlend.ADD, g, 1.5f * s * VfxAnim.easeOutCubic(u), spin0, VfxVertexBuffer.withAlpha(tint, fade * 0.9f));
            buf.billboard(ctx, DUST, VfxBlend.ADD, g, 1.8f * s * VfxAnim.easeOutCubic(u), -spin0, VfxVertexBuffer.withAlpha(EDGE, fade));
        }
    }

    // ------------------------------------------------------------------ FX2: zone / field / dome
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.0f, inst.power);
        float lf = life(inst, age, 8f, 12f);
        if (lf <= 0f) return;
        float open = VfxAnim.easeOutCubic(sat(age / 14f));
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.04f, 0));
        int tint = halo(inst);
        RandomSource rnd = inst.random();
        float spin0 = rnd.nextFloat() * Mth.TWO_PI;

        // 1. caustic light net on the floor
        buf.plane(CAUSTIC, VfxBlend.ADD, ground.spin(age * 0.008f + spin0), R * open, VfxVertexBuffer.withAlpha(tint, 0.5f * lf));
        // 2. faceted rings, counter-rotating (parallax: the inner one sits a little higher and turns faster)
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.01f).spin(age * 0.012f), R * 1.04f * open, VfxVertexBuffer.withAlpha(EDGE, 0.95f * lf));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.025f + 0.5f), R * 0.64f * open, VfxVertexBuffer.withAlpha(tint, 0.8f * lf));
        // 3. pulse ring sweeping outward every 28 ticks
        float pu = (age % 28f) / 28f;
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.03f).spin(spin0), R * (0.25f + 0.8f * pu) * open, VfxVertexBuffer.withAlpha(EDGE, 0.5f * (1f - pu) * lf));

        // 4. the wall: clear crystal prisms growing around the rim, a light shaft behind each
        int n = 8;
        float ph = Mth.clamp(age * 0.004f, 0, 1);
        for (int i = 0; i < n; i++) {
            float th = Mth.TWO_PI * (i + 0.5f * (i % 2)) / n + spin0 * 0.1f;
            float grow = VfxAnim.easeOutBack(sat((age - 3f - i * 1.4f) / 14f));
            float h = (1.0f + 0.8f * ((i * 5) % 3) / 2f) * Math.min(2.2f, 0.7f + R * 0.28f) * grow;
            float w = 0.28f * Math.min(1.8f, 0.8f + R * 0.2f);
            float rr = R * 0.97f;
            Vector3f base = new Vector3f(c).add(Mth.cos(th) * rr, 0.02f, Mth.sin(th) * rr);
            Vector3f tg = new Vector3f(-Mth.sin(th), 0, Mth.cos(th)).mul(w);
            Vector3f up = new Vector3f(0, h, 0);
            Vector3f bl = new Vector3f(base).sub(tg), br = new Vector3f(base).add(tg);
            int col = VfxVertexBuffer.withAlpha(i % 2 == 0 ? EDGE : tint, 0.9f * lf);
            buf.quad(PRISM, VfxBlend.ADD, bl, br, new Vector3f(br).add(up), new Vector3f(bl).add(up), 0, 0, 1, 1, col, col);
            Vector3f sb = new Vector3f(base).add(0, 0.05f, 0);
            Vector3f st = new Vector3f(sb).add(0, h * 2.1f, 0);
            buf.beam(ctx, SHAFT, VfxBlend.ADD, sb, st, w * 3.2f, w * 2.2f, 1, 0f,
                    VfxVertexBuffer.withAlpha(tint, 0.55f * lf * grow), VfxVertexBuffer.withAlpha(tint, 0.05f));
        }

        // 5. rising glints
        for (int i = 0; i < 9; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI, rr = (0.15f + 0.8f * rnd.nextFloat()) * R;
            float sp = 0.012f + 0.01f * rnd.nextFloat();
            float y = ((age * sp * 3f + rnd.nextFloat()) % 1f);
            float tw = 0.5f + 0.5f * Mth.sin(age * 0.3f + i * 1.7f);
            Vector3f p = new Vector3f(c).add(Mth.cos(a) * rr, 0.1f + y * (0.9f + 0.25f * R), Mth.sin(a) * rr);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, p, (0.28f + 0.2f * tw) * Math.min(2f, 0.6f + 0.2f * R), age * 0.05f + i,
                    VfxVertexBuffer.withAlpha(EDGE, lf * Mth.sin(y * Mth.PI) * (0.5f + 0.5f * tw)));
        }

        // 6. hovering crystal in the middle
        float bob = 0.1f * Mth.sin(age * 0.12f);
        Vector3f hp = new Vector3f(c).add(0, 0.9f + 0.05f * R + bob, 0);
        float cs = Math.min(1.4f, 0.55f + 0.1f * R);
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, hp, cs * 2.2f, 0f, VfxVertexBuffer.withAlpha(tint, 0.35f * lf));
        buf.billboard(ctx, FACET, VfxBlend.ADD, hp, cs * open, age * 0.04f, VfxVertexBuffer.withAlpha(EDGE, 0.95f * lf));
        buf.billboard(ctx, GLINT, VfxBlend.ADD, hp, cs * 1.6f, -age * 0.07f, VfxVertexBuffer.withAlpha(EDGE, 0.8f * lf * (0.6f + 0.4f * VfxAnim.pulse(age, 1.5f))));
    }

    // ------------------------------------------------------------------ FX3: impact / burst / signature
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float s = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vec3 d3 = inst.to(ctx).subtract(inst.from(ctx));
        Vector3f hint = new Vector3f((float) d3.x, (float) d3.y, (float) d3.z);
        float hl = hint.length();
        if (hl > 1e-3f) hint.div(hl); else hint.set(0, 0, 0);
        int tint = halo(inst);
        RandomSource rnd = inst.random();
        float spin0 = rnd.nextFloat() * Mth.TWO_PI;

        float eo = VfxAnim.easeOutCubic(sat(t / 0.7f));
        float fadeOut = 1f - sat((t - 0.55f) / 0.45f);

        // 1. lingering bloom (afterglow) and the flash
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 3.6f * s * (0.5f + 0.5f * eo), 0f, VfxVertexBuffer.withAlpha(tint, 0.22f * (1f - t * t)));
        float fl = 1f - sat(t / 0.3f);
        // 2. light columns (parallax: different heights)
        for (int i = 0; i < 4; i++) {
            float a = spin0 + Mth.HALF_PI * i + 0.4f * i;
            float rr = 0.5f * s * (1 + i % 2);
            Vector3f b = new Vector3f(c).add(Mth.cos(a) * rr, -0.4f * s, Mth.sin(a) * rr);
            Vector3f top = new Vector3f(b).add(0, (2.0f + 1.4f * (i % 3)) * s * VfxAnim.easeOutCubic(sat(t / 0.35f)), 0);
            buf.beam(ctx, SHAFT, VfxBlend.ADD, b, top, 0.9f * s, 0.5f * s, 1, 0f,
                    VfxVertexBuffer.withAlpha(tint, 0.6f * fadeOut), VfxVertexBuffer.withAlpha(tint, 0.05f));
        }
        // 3. the crystal: swells, then shatters at t = 0.22
        float swell = VfxAnim.easeOutBack(sat(t / 0.2f));
        float shatter = sat((t - 0.22f) / 0.2f);
        if (shatter < 1f) {
            buf.billboard(ctx, FACET, VfxBlend.ADD, c, 1.7f * s * swell * (1f + 0.5f * shatter), spin0 + t * 2f, VfxVertexBuffer.withAlpha(EDGE, 0.6f * (1f - shatter)));
        }
        // 4. fracture web, then facet rings expanding
        buf.billboard(ctx, CRACK, VfxBlend.ADD, c, 3.2f * s * VfxAnim.easeOutCubic(sat(t / 0.4f)), spin0, VfxVertexBuffer.withAlpha(EDGE, 0.95f * fadeOut));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, 4.6f * s * eo, t * 1.4f + spin0, VfxVertexBuffer.withAlpha(tint, 0.85f * fadeOut));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, 2.8f * s * VfxAnim.easeOutCubic(sat(t / 0.5f)), -t * 2.2f, VfxVertexBuffer.withAlpha(EDGE, 0.7f * fadeOut));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, -0.35f * s, 0));
        buf.plane(RING, VfxBlend.ADD, ground.spin(t * 1.5f), 3.4f * s * eo, VfxVertexBuffer.withAlpha(tint, 0.6f * fadeOut));

        // 5. needle shards flying outward (biased along the hint), splinters trailing behind
        int n = 14;
        for (int i = 0; i < n; i++) {
            Vector3f dr = new Vector3f(rnd.nextFloat() * 2 - 1, rnd.nextFloat() * 1.4f - 0.5f, rnd.nextFloat() * 2 - 1);
            if (dr.lengthSquared() < 1e-3f) dr.set(0, 1, 0);
            dr.normalize().add(new Vector3f(hint).mul(0.7f)).normalize();
            float speed = 0.7f + 0.6f * rnd.nextFloat();
            float u = sat((t - 0.12f) / 0.88f);
            float r = s * (0.5f + 3.4f * speed * VfxAnim.easeOutCubic(u));
            float len = s * (0.7f + 0.8f * rnd.nextFloat()) * (1f - 0.35f * u);
            float al = (1f - sat((u - 0.5f) / 0.5f)) * sat(t / 0.12f);
            Vector3f p = along(c, dr, r);
            p.y -= 0.6f * s * u * u;
            shard(buf, ctx, VfxBlend.ADD, p, dr, len, 0.3f * s, VfxVertexBuffer.withAlpha(i % 3 == 0 ? EDGE : tint, al));
        }
        buf.billboard(ctx, DUST, VfxBlend.ADD, c, 5.2f * s * eo, spin0, VfxVertexBuffer.withAlpha(EDGE, 0.5f * fadeOut));
        buf.billboard(ctx, DUST, VfxBlend.ADD, c, 3.4f * s * VfxAnim.easeOutCubic(sat(t / 0.5f)), -spin0 * 2f, VfxVertexBuffer.withAlpha(tint, 0.4f * fadeOut));

        // 6. the flash glints on top
        if (fl > 0f) buf.billboard(ctx, GLINT, VfxBlend.ADD, c, (2.2f + 3.2f * (1f - fl)) * s, spin0, VfxVertexBuffer.withAlpha(EDGE, 0.6f * fl));
        for (int i = 0; i < 6; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI, rr = (0.8f + 1.8f * rnd.nextFloat()) * s * (0.4f + eo);
            float tw = sat(Mth.sin(Mth.clamp(t * 1.4f - 0.1f * i, 0f, 1f) * Mth.PI));
            Vector3f p = new Vector3f(c).add(new Vector3f(ctx.camRight).mul(Mth.cos(a) * rr)).add(new Vector3f(ctx.camUp).mul(Mth.sin(a) * rr));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, p, 0.55f * s, age * 0.07f + i, VfxVertexBuffer.withAlpha(EDGE, tw));
        }
    }
}
