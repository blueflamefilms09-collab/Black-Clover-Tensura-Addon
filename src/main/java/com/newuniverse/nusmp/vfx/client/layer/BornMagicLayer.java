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
 * 1.16.0: the procedural look of a wizard's own magic (book.SignatureSpell): the Signature Spell, and the Fusion and Born spells of a
 * grimoire with two attributes. Nothing here belongs to one attribute: the server sends the first parent's colour (the payload's colour),
 * the second parent's colour (the high 32 bits of the payload's seed) and a style (the low byte of the seed), and the layer builds the
 * effect from them, so a magic that is born of any two attributes already has effects, in its own two colours.
 * <ul>
 *   <li>style bits 0-2: the motif of the motes (sparks, shards, motes, glints, glow, petals-as-shards);</li>
 *   <li>style bit 3 (8): BORN_BEAM is a slow orb instead of a lance; bit 4 (16): its strands are a wide corkscrew.</li>
 * </ul>
 * <ul>
 *   <li>BORN_BEAM: a bright core with the first colour at the head fading to the second at the tail; two strands of motes (one per
 *       parent) wind round it; a flash at 'to'. 'from' = hand, 'to' = end, power = size.</li>
 *   <li>BORN_NOVA: a circle in the first colour and a counter-turning rune ring in the second on the ground at 'from', a shock ring that
 *       changes colour as it spreads, and motes thrown out of the centre. power = radius.</li>
 *   <li>BORN_WAVE: four rings alternating the two colours race out along 'from' -> 'to', widening like a cone, over a core streak.</li>
 *   <li>BORN_RAIN: a bolt falls from 'from' to 'to' (first colour at the head, second at the tail); at the ground a ring spreads, motes
 *       fly and a flash blooms. power = size.</li>
 * </ul>
 */
public class BornMagicLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BORN_BEAM, VfxShape.BORN_NOVA, VfxShape.BORN_WAVE, VfxShape.BORN_RAIN); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BORN_BEAM -> 14;
            case BORN_NOVA -> 24;
            case BORN_WAVE -> 16;
            default -> 14;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFC8A0FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.BORN_NOVA) VfxShake.add(inst.payload.from(), 0.5f * Mth.clamp(inst.power / 4f, 0.5f, 2f), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BORN_BEAM -> beam(inst, ctx, buf);
            case BORN_NOVA -> nova(inst, ctx, buf);
            case BORN_WAVE -> wave(inst, ctx, buf);
            case BORN_RAIN -> rain(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ the palette and style packed into the payload
    private static int first(VfxInstance inst) { return inst.color | 0xFF000000; }

    private static int second(VfxInstance inst) {
        int c = (int) (inst.seed >>> 32);
        return c == 0 ? VfxVertexBuffer.whiten(first(inst), 0.5f) : c | 0xFF000000;
    }

    private static int flags(VfxInstance inst) { return (int) (inst.seed & 0xFF); }

    private static ResourceLocation mote(int motif) {
        return switch (Math.floorMod(motif & 7, 6)) {
            case 1, 5 -> VfxTextures.SHARD;
            case 2 -> VfxTextures.MANA_MOTE;
            case 4 -> VfxTextures.GLOW;
            default -> VfxTextures.SPARK;
        };
    }

    private static float sat(float v) { return Mth.clamp(v, 0f, 1f); }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    /** Two unit vectors perpendicular to dir (and to each other). */
    private static Vector3f[] frame(Vector3f dir) {
        Vector3f right = new Vector3f(dir).cross(0f, 1f, 0f);
        if (right.lengthSquared() < 1e-4f) right.set(1f, 0f, 0f);
        right.normalize();
        Vector3f up = new Vector3f(dir).cross(right).normalize();
        return new Vector3f[]{right, up};
    }

    // ------------------------------------------------------------------ BORN_BEAM
    private void beam(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float s = Mth.clamp(inst.power, 0.6f, 3f);
        int ca = first(inst), cb = second(inst), fl = flags(inst);
        boolean orb = (fl & 8) != 0, spiral = (fl & 16) != 0;
        Vector3f f = ctx.rel(inst.from(ctx)), g = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(g).sub(f);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        Vector3f[] fr = frame(dir);
        ResourceLocation m = mote(fl);

        float e = orb ? sat(t / 0.9f) : VfxAnim.easeOutCubic(sat(t / 0.4f));
        float tailE = orb ? Math.max(0f, e - 0.28f) : Math.min(e, sat((t - 0.15f) / 0.85f));
        float fade = orb ? 1f - sat((t - 0.92f) / 0.08f) : 1f - 0.7f * sat((t - 0.45f) / 0.55f);
        Vector3f head = lerp(f, g, e), tail = lerp(f, g, tailE);

        // the beam: first colour at the head, second at the tail, a white-hot core inside it
        if (!orb) {
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, tail, 0.55f * s, 0.14f * s, 1, 0f, VfxVertexBuffer.withAlpha(ca, 0.9f * fade), VfxVertexBuffer.withAlpha(cb, 0.5f * fade));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, tail, 0.2f * s, 0.06f * s, 1, 0f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(ca, 0.85f), fade), VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(cb, 0.7f), 0.6f * fade));
        }

        // two strands of motes, one for each parent, winding round the beam (wide for the corkscrew)
        int n = orb ? 8 : 11;
        float rad = (spiral ? 0.6f : 0.3f) * s;
        for (int strand = 0; strand < 2; strand++) {
            int col = strand == 0 ? ca : cb;
            for (int i = 0; i < n; i++) {
                float k = (i + 0.5f) / n;
                Vector3f p = lerp(tail, head, k);
                float ph = k * (spiral ? 7f : 10f) + age * 0.5f + strand * Mth.PI;
                float r = rad * (0.4f + 0.6f * Mth.sin(k * Mth.PI));
                p.add(new Vector3f(fr[0]).mul(Mth.cos(ph) * r)).add(new Vector3f(fr[1]).mul(Mth.sin(ph) * r));
                buf.billboard(ctx, m, VfxBlend.ADD, p, 0.3f * s * (0.5f + 0.5f * k), ph, VfxVertexBuffer.withAlpha(col, fade * (0.35f + 0.65f * k)));
            }
        }

        // the head: a glow in both colours (a big sphere for the orb)
        float hs = orb ? 1.7f * s : 0.8f * s;
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, hs, 0f, VfxVertexBuffer.withAlpha(ca, 0.8f * fade));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, hs * 0.55f, 0f, VfxVertexBuffer.withAlpha(cb, 0.8f * fade));
        buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, head, hs * 0.8f, age * 0.3f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(ca, 0.9f), fade));
        if (orb) {                                                         // motes circling the orb, alternating the two colours
            for (int i = 0; i < 6; i++) {
                float ph = age * 0.35f + Mth.TWO_PI * i / 6f;
                Vector3f p = new Vector3f(head).add(new Vector3f(fr[0]).mul(Mth.cos(ph) * 0.8f * s)).add(new Vector3f(fr[1]).mul(Mth.sin(ph) * 0.8f * s));
                buf.billboard(ctx, m, VfxBlend.ADD, p, 0.32f * s, ph, VfxVertexBuffer.withAlpha(i % 2 == 0 ? ca : cb, fade));
            }
        }

        // the flash where it ends
        if (t > 0.7f) {
            float u = sat((t - 0.7f) / 0.3f), out = 1f - u * u;
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, g, (1.0f + 1.6f * VfxAnim.easeOutCubic(u)) * s, 0f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(ca, cb, u), 0.8f * out));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, g, 1.8f * s * VfxAnim.easeOutCubic(u), age * 0.2f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(cb, 0.8f), out));
        }
    }

    // ------------------------------------------------------------------ BORN_NOVA
    private void nova(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float R = Math.max(1.5f, inst.power);
        int ca = first(inst), cb = second(inst), fl = flags(inst);
        float lf = Math.min(sat(age / 4f), sat((inst.duration - age) / 8f));
        if (lf <= 0f) return;
        float open = VfxAnim.easeOutCubic(sat(age / 10f));
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0f, 0.06f, 0f));
        RandomSource rnd = inst.random();
        float spin0 = rnd.nextFloat() * Mth.TWO_PI;

        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, ground.spin(age * 0.06f + spin0), R * open * 0.9f, VfxVertexBuffer.withAlpha(ca, 0.9f * lf));
        buf.plane(VfxTextures.RUNE_RING, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.09f), R * open * 0.62f, VfxVertexBuffer.withAlpha(cb, 0.9f * lf));
        float sh = sat(t / 0.6f);
        buf.plane(VfxTextures.RUNE_RING, VfxBlend.ADD, ground.lift(0.03f), R * (0.2f + 0.9f * sh), VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(ca, cb, sh), 0.8f * (1f - sh)));

        float fly = sat(t / 0.7f);
        ResourceLocation m = mote(fl);
        for (int i = 0; i < 16; i++) {
            float ang = Mth.TWO_PI * i / 16f + spin0;
            float r = R * VfxAnim.easeOutCubic(fly) * (0.5f + 0.5f * ((i * 7) % 5) / 4f);
            float h = 0.2f + 1.8f * Mth.sin(fly * Mth.PI) * (0.4f + 0.6f * ((i * 3) % 4) / 3f);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * r, h, Mth.sin(ang) * r);
            buf.billboard(ctx, m, VfxBlend.ADD, p, 0.34f, ang, VfxVertexBuffer.withAlpha(i % 2 == 0 ? ca : cb, 1f - t));
        }
        float fi = 1f - sat(t / 0.3f);
        if (fi > 0f) buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c).add(0f, 0.6f, 0f), R * 0.9f * fi, 0f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(ca, cb, 0.5f), 0.8f * fi));
    }

    // ------------------------------------------------------------------ BORN_WAVE
    private void wave(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        int ca = first(inst), cb = second(inst), fl = flags(inst);
        Vector3f f = ctx.rel(inst.from(ctx)), g = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(g).sub(f);
        float len = dir.length();
        if (len < 1e-3f) return;
        dir.div(len);
        float lf = Math.min(sat(age / 3f), sat((inst.duration - age) / 6f));
        if (lf <= 0f) return;
        int seg = ctx.seg(10, 6);
        for (int j = 0; j < 4; j++) {
            float d = len * sat(t * 1.25f - j * 0.15f);
            if (d <= 0.05f) continue;
            float radius = 0.3f + d * 0.42f;
            VfxPose pose = VfxPose.facing(lerp(f, g, d / len), dir).spin(age * 0.2f * (j % 2 == 0 ? 1f : -1f));
            buf.ring(VfxTextures.RUNE_RING, VfxBlend.ADD, pose, radius * 0.78f, radius, seg, 2f, age * 0.02f,
                    VfxVertexBuffer.withAlpha(j % 2 == 0 ? ca : cb, (1f - d / len) * lf));
        }
        Vector3f head = lerp(f, g, VfxAnim.easeOutCubic(sat(t / 0.5f)));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, f, head, 0.22f, 0.08f, 1, 0f, VfxVertexBuffer.withAlpha(ca, 0.7f * lf), VfxVertexBuffer.withAlpha(cb, 0.4f * lf));
        Vector3f[] fr = frame(dir);
        ResourceLocation m = mote(fl);
        for (int i = 0; i < 10; i++) {                                      // motes on the cone's skin
            float k = sat(t * 1.2f - 0.05f * i) * (0.3f + 0.7f * ((i * 5) % 7) / 6f);
            float ang = Mth.TWO_PI * i / 10f + age * 0.15f;
            float r = 0.3f + len * k * 0.4f;
            Vector3f p = lerp(f, g, k).add(new Vector3f(fr[0]).mul(Mth.cos(ang) * r)).add(new Vector3f(fr[1]).mul(Mth.sin(ang) * r));
            buf.billboard(ctx, m, VfxBlend.ADD, p, 0.3f, ang, VfxVertexBuffer.withAlpha(i % 2 == 0 ? ca : cb, (1f - k) * lf));
        }
    }

    // ------------------------------------------------------------------ BORN_RAIN
    private void rain(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick);
        float s = Mth.clamp(inst.power, 0.6f, 3f);
        int ca = first(inst), cb = second(inst), fl = flags(inst);
        Vector3f top = ctx.rel(inst.from(ctx)), ground = ctx.rel(inst.to(ctx));
        float fall = VfxAnim.easeInCubic(sat(t / 0.5f));
        Vector3f head = lerp(top, ground, fall), tail = lerp(top, ground, sat((t - 0.1f) / 0.4f));
        float fade = 1f - sat((t - 0.6f) / 0.4f);
        if (t < 0.55f) {
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, tail, 0.5f * s, 0.12f * s, 1, 0f, VfxVertexBuffer.withAlpha(ca, 0.9f), VfxVertexBuffer.withAlpha(cb, 0.5f));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, tail, 0.18f * s, 0.05f * s, 1, 0f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(ca, 0.85f), 1f), VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(cb, 0.7f), 0.5f));
        }
        if (t > 0.4f) {
            float u = sat((t - 0.4f) / 0.6f);
            VfxPose pose = VfxPose.ground(new Vector3f(ground).add(0f, 0.06f, 0f));
            buf.plane(VfxTextures.RUNE_RING, VfxBlend.ADD, pose.spin(age * 0.1f), 1.8f * s * VfxAnim.easeOutCubic(u), VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(ca, cb, u), 0.9f * fade));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(ground).add(0f, 0.5f, 0f), 2.0f * s * (1f - u), 0f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(ca, cb, 0.5f), 0.8f * (1f - u)));
            ResourceLocation m = mote(fl);
            for (int i = 0; i < 8; i++) {
                float ang = Mth.TWO_PI * i / 8f + age * 0.1f;
                float r = 1.6f * s * VfxAnim.easeOutCubic(u);
                float h = 0.2f + 1.2f * s * Mth.sin(u * Mth.PI);
                buf.billboard(ctx, m, VfxBlend.ADD, new Vector3f(ground).add(Mth.cos(ang) * r, h, Mth.sin(ang) * r), 0.3f * s, ang, VfxVertexBuffer.withAlpha(i % 2 == 0 ? ca : cb, 1f - u));
            }
        }
    }
}
