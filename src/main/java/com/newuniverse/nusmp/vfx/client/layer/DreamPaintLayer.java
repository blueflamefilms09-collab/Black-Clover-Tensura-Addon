package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.stand;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.40: Dream Magic and Painting Magic, held to the Time Magic standard: layered iridescent colour, motion in every piece, a bloom
 * core, and a clear beginning, middle and end. Textures from tools/gen_dream_paint_vfx_textures.py (greyscale, tinted per quad).
 * <ul>
 *   <li>DREAM_TRANSITION: Glamour World opening. Pastel mist puffs spiral outward to the edge of the dream, starlight bursts
 *       out with them, a shimmering ring races across the ground; then the mist folds back in and rises, swallowing everyone.</li>
 *   <li>DREAM_DOME: the dream's sky. An iridescent aurora dome of veil bands that shift pink -> lilac -> sky -> mint as they
 *       turn, a shimmer ribbon at its foot, soap bubbles rising and stars twinkling inside.</li>
 *   <li>DREAM_MANIFEST: imagination becoming matter. Smoke spirals in and condenses, then a flash, a pop of stars and a
 *       shockwave ring as it turns solid. From high above 'to' it is a falling star that lands at 'to'.</li>
 *   <li>DREAM_SHATTER: the dream breaking from inside. The dome flashes, cracks into shards that burst outward and fall, the
 *       veil tears away.</li>
 *   <li>PAINT_STROKE: a wet brushstroke painted through the air from 'from' to 'to' (the stroke's texture lays down along
 *       it), a glossy highlight along its wet edge, droplets flicked off, a splash where it lands.</li>
 *   <li>PAINT_SPLAT: sticky ink splashed on the ground, drips running outward, then a lacquer gloss sweeping over it as it
 *       hardens.</li>
 *   <li>PAINT_BEAST: a painting comes to life. Flat brushstrokes lift off the ground, wrap round into the outline of a
 *       creature and close into a glowing wet outline that breathes.</li>
 *   <li>PAINT_CAMO: Camouflage. A refraction ripple slides over the caster in bands, with a few flecks of paint drifting off.</li>
 * </ul>
 */
public class DreamPaintLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation MIST = t("dream_mist"), STAR = t("dream_star"), BUBBLE = t("dream_bubble"), VEIL = t("dream_veil"),
            BAND = t("dream_band"), STROKE = t("paint_stroke"), STROKE2 = t("paint_stroke2"), SPLAT = t("paint_splat"),
            OUTLINE = t("paint_outline"), CANVAS = t("paint_canvas"), REFRACT = t("paint_refract");

    /** The dream palette: pink, lilac, sky, mint. */
    private static final int[] PASTEL = {0xFFFFB0E0, 0xFFD8B4FF, 0xFFB0DCFF, 0xFFC0FFE6};

    /** Iridescent pastel at phase x (wraps every 1.0). */
    static int iris(float x) {
        x = x - Mth.floor(x);
        float f = x * PASTEL.length;
        int i = (int) f;
        return VfxVertexBuffer.lerpColor(PASTEL[i % PASTEL.length], PASTEL[(i + 1) % PASTEL.length], f - i);
    }

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.DREAM_TRANSITION, VfxShape.DREAM_DOME, VfxShape.DREAM_MANIFEST, VfxShape.DREAM_SHATTER,
                VfxShape.PAINT_STROKE, VfxShape.PAINT_SPLAT, VfxShape.PAINT_BEAST, VfxShape.PAINT_CAMO);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DREAM_TRANSITION, DREAM_SHATTER -> 30;
            case DREAM_DOME -> 600;
            case DREAM_MANIFEST -> 24;
            case PAINT_STROKE -> 20;
            case PAINT_SPLAT -> 100;
            case PAINT_BEAST -> 36;
            case PAINT_CAMO -> 200;
            default -> 30;
        };
    }

    @Override
    public int defaultColor(VfxShape s) {
        return switch (s) {
            case PAINT_STROKE, PAINT_SPLAT, PAINT_BEAST -> 0xFF3A7BFF;
            case PAINT_CAMO -> 0xFFE8F4FF;
            default -> 0xFFF4A8E0;
        };
    }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.DREAM_SHATTER) VfxShake.add(inst.payload.from(), 0.7f, 12);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DREAM_TRANSITION -> transition(inst, ctx, buf);
            case DREAM_DOME -> dome(inst, ctx, buf);
            case DREAM_MANIFEST -> manifest(inst, ctx, buf);
            case DREAM_SHATTER -> shatter(inst, ctx, buf);
            case PAINT_STROKE -> stroke(inst, ctx, buf);
            case PAINT_SPLAT -> splat(inst, ctx, buf);
            case PAINT_BEAST -> beast(inst, ctx, buf);
            case PAINT_CAMO -> camo(inst, ctx, buf);
            default -> { }
        }
    }

    // ================================================================ Dream Magic
    private void transition(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.05f, 0);
        float out = VfxAnim.easeOutCubic(Mth.clamp(t / 0.55f, 0, 1)), in = VfxAnim.easeInCubic(Mth.clamp((t - 0.55f) / 0.45f, 0, 1));
        float fade = life(inst, age, 2, 6);
        // the shimmer ring racing out over the ground
        VfxPose ground = VfxPose.ground(g);
        float rr = R * out * (1 - 0.35f * in);
        buf.ring(BAND, VfxBlend.ADD, ground, Math.max(0, rr - 0.9f), rr + 0.3f, ctx.seg(24, 12), 4, age * 0.03f,
                VfxVertexBuffer.withAlpha(iris(age * 0.02f), 0.8f * fade * (1 - in)));
        // mist puffs: spiral out, then fold in and rise
        int n = ctx.seg(22, 10);
        for (int i = 0; i < n; i++) {
            float h = hash(inst.seed, i, 1), ang = Mth.TWO_PI * i / n + h * 0.6f + age * (0.08f + 0.05f * h);
            float rad = R * (0.25f + 0.75f * h) * out * (1 - 0.8f * in);
            float y = 0.4f + 1.6f * h * out + 3.5f * in * (0.5f + h);
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rad, y, Mth.sin(ang) * rad);
            float size = (1.2f + 1.6f * h) * (0.6f + 0.6f * out) * (1 + 0.6f * in) * (0.6f + R * 0.12f);
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, size, age * 0.02f + i, VfxVertexBuffer.withAlpha(iris(h + age * 0.015f), 0.75f * fade));
        }
        // starlight bursting out with it
        int m = ctx.seg(16, 8);
        for (int i = 0; i < m; i++) {
            float h = hash(inst.seed, i, 2), ang = Mth.TWO_PI * h + i, rad = R * 1.1f * VfxAnim.easeOutCubic(Mth.clamp(t * 1.5f, 0, 1)) * (0.5f + 0.5f * h);
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * rad, 0.5f + 3f * hash(inst.seed, i, 3) + 1.5f * in, Mth.sin(ang) * rad);
            float tw = 0.6f + 0.4f * Mth.sin(age * 0.8f + i * 1.7f);
            buf.billboard(ctx, STAR, VfxBlend.ADD, q, (0.4f + 0.5f * h) * tw, age * 0.05f, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(iris(h), 0.5f), fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 1, 0), 2.2f + R * 0.15f, iris(age * 0.02f), 0.6f * fade);
    }

    private void dome(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power;
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 24f, 0, 1)), fade = life(inst, age, 0, 20);
        Vector3f c = ctx.rel(inst.from(ctx));
        float r = R * open;
        // the aurora dome: latitude bands of veil, coloured by angle so the colours slide round as it turns
        int ns = ctx.seg(14, 8), nl = 4;
        float latMax = 1.35f, spin = age * 0.004f;
        for (int k = 0; k < nl; k++) {
            float l0 = latMax * k / nl, l1 = latMax * (k + 1) / nl;
            float y0 = Mth.sin(l0) * r, y1 = Mth.sin(l1) * r, c0 = Mth.cos(l0) * r, c1 = Mth.cos(l1) * r;
            for (int i = 0; i < ns; i++) {
                float a0 = Mth.TWO_PI * i / ns + spin, a1 = Mth.TWO_PI * (i + 1) / ns + spin;
                float hue = (float) i / ns + age * 0.003f + k * 0.12f;
                float pulse = 0.55f + 0.2f * Mth.sin(age * 0.05f + i * 0.9f + k);
                int col0 = VfxVertexBuffer.withAlpha(iris(hue), pulse * fade), col1 = VfxVertexBuffer.withAlpha(iris(hue + 0.15f), pulse * fade);
                buf.quad(VEIL, VfxBlend.ADD,
                        new Vector3f(c).add(Mth.cos(a0) * c0, y0, Mth.sin(a0) * c0), new Vector3f(c).add(Mth.cos(a1) * c0, y0, Mth.sin(a1) * c0),
                        new Vector3f(c).add(Mth.cos(a1) * c1, y1, Mth.sin(a1) * c1), new Vector3f(c).add(Mth.cos(a0) * c1, y1, Mth.sin(a0) * c1),
                        i * 0.5f, 1 - l1 / latMax, i * 0.5f + 0.5f, 1 - l0 / latMax, col0, col1);
            }
        }
        // a shimmering ribbon round the foot of the dome
        hoop(buf, BAND, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.6f, 0)), r * 0.99f, 0.6f, ctx.seg(14, 8), 3, -age * 0.01f,
                VfxVertexBuffer.withAlpha(iris(age * 0.004f + 0.5f), 0.6f * fade));
        // soap bubbles rising inside
        int nb = ctx.seg(10, 5);
        for (int i = 0; i < nb; i++) {
            float lt = ((age * 0.004f) + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI + lt * 2;
            float rad = r * 0.75f * hash(inst.seed, i, 3);
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rad, 0.5f + lt * r * 0.8f, Mth.sin(ang) * rad);
            buf.billboard(ctx, BUBBLE, VfxBlend.ADD, q, 0.5f + 0.9f * hash(inst.seed, i, 4), 0, VfxVertexBuffer.withAlpha(iris(lt + i * 0.2f), 0.7f * fade * Mth.sin(lt * Mth.PI)));
        }
        // stars twinkling under the dome
        int nt = ctx.seg(14, 6);
        for (int i = 0; i < nt; i++) {
            float ang = hash(inst.seed, i, 5) * Mth.TWO_PI + age * 0.002f, el = 0.35f + 0.9f * hash(inst.seed, i, 6);
            float rad = r * 0.88f;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * Mth.cos(el) * rad, Mth.sin(el) * rad, Mth.sin(ang) * Mth.cos(el) * rad);
            float tw = Mth.clamp(0.5f + 0.5f * Mth.sin(age * 0.15f + i * 2.3f), 0, 1);
            buf.billboard(ctx, STAR, VfxBlend.ADD, q, 0.5f + 0.6f * tw, age * 0.01f + i, VfxVertexBuffer.withAlpha(WHITE, tw * fade));
        }
    }

    private void manifest(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        boolean falling = a.y - b.y > 2f;
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.55f);
        float fade = life(inst, age, 0, 8);
        Vector3f c;
        if (falling) {                                                // a star falling from 'from' onto 'to'
            float f = VfxAnim.easeInCubic(Mth.clamp(t / 0.5f, 0, 1));
            c = new Vector3f(a).lerp(b, f);
            if (t < 0.5f) {
                streak(buf, ctx, BAND, VfxBlend.ADD, new Vector3f(a).lerp(b, Math.max(0, f - 0.35f)), c, 0.5f * p, VfxVertexBuffer.withAlpha(light, fade));
                buf.billboard(ctx, STAR, VfxBlend.ADD, c, 1.4f * p, age * 0.3f, VfxVertexBuffer.withAlpha(WHITE, fade));
                VfxBloom.glow(ctx, buf, c, 0.9f * p, col, fade);
                return;
            }
            t = (t - 0.5f) / 0.5f;
            c = new Vector3f(b).add(0, 0.3f, 0);
        } else {
            c = new Vector3f(a).add(0, 0.9f * p, 0);
            // smoke spiralling in and condensing
            float in = Mth.clamp(t / 0.55f, 0, 1);
            int n = ctx.seg(12, 6);
            for (int i = 0; i < n; i++) {
                float h = hash(inst.seed, i, 1), ang = Mth.TWO_PI * i / n + in * 4f + h;
                float rad = 2.2f * p * (1 - VfxAnim.easeInCubic(in)) + 0.2f;
                Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rad, (h - 0.5f) * 1.6f * p * (1 - in), Mth.sin(ang) * rad);
                buf.billboard(ctx, MIST, VfxBlend.ALPHA, q, (0.8f + 0.8f * h) * p * (1 - 0.4f * in), ang, VfxVertexBuffer.withAlpha(iris(h + age * 0.03f), 0.7f * fade * (1 - Mth.clamp((t - 0.55f) * 4, 0, 1))));
            }
            t = Mth.clamp((t - 0.5f) / 0.5f, 0, 1);
            if (t <= 0) { VfxBloom.glow(ctx, buf, c, 0.6f * p * in, col, 0.5f * fade); return; }
        }
        // solid: the flash, a pop of stars and a shockwave ring
        float pop = VfxAnim.easeOutCubic(t);
        VfxBloom.glow(ctx, buf, c, (1.2f + 1.2f * (1 - t)) * p, col, fade * (1 - t * 0.6f));
        buf.billboard(ctx, STAR, VfxBlend.ADD, c, 2.4f * p * (1 - t), age * 0.2f, VfxVertexBuffer.withAlpha(WHITE, fade * (1 - t)));
        int m = ctx.seg(10, 5);
        for (int i = 0; i < m; i++) {
            float ang = Mth.TWO_PI * i / m + hash(inst.seed, i, 3), el = (hash(inst.seed, i, 4) - 0.3f) * 1.2f;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * Mth.cos(el) * 1.8f * p * pop, Mth.sin(el) * 1.8f * p * pop, Mth.sin(ang) * Mth.cos(el) * 1.8f * p * pop);
            buf.billboard(ctx, STAR, VfxBlend.ADD, q, 0.45f * p * (1 - t * 0.5f), age * 0.2f + i, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(iris(i * 0.13f), 0.4f), fade * (1 - t)));
        }
        buf.ring(BAND, VfxBlend.ADD, VfxPose.ground(new Vector3f(c.x, (falling ? b.y : a.y) + 0.06f, c.z)), 2.4f * p * pop, 2.4f * p * pop + 0.5f,
                ctx.seg(16, 10), 3, 0, VfxVertexBuffer.withAlpha(light, fade * (1 - t)));
    }

    private void shatter(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, t = age / inst.duration;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 0, 10);
        // the flash and the torn veil
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, R * 0.5f, 0), R * 0.9f * (1 - t * 0.5f), 0xFFFFE8F8, 0.8f * (1 - t));
        int ns = ctx.seg(12, 6);
        for (int i = 0; i < ns; i++) {
            float a0 = Mth.TWO_PI * i / ns, a1 = Mth.TWO_PI * (i + 1) / ns, r = R * (1 + 0.6f * t), h = R * (0.9f - 0.6f * t);
            buf.quad(VEIL, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(a0) * r, 0, Mth.sin(a0) * r), new Vector3f(c).add(Mth.cos(a1) * r, 0, Mth.sin(a1) * r),
                    new Vector3f(c).add(Mth.cos(a1) * r * 0.6f, h, Mth.sin(a1) * r * 0.6f), new Vector3f(c).add(Mth.cos(a0) * r * 0.6f, h, Mth.sin(a0) * r * 0.6f),
                    i * 0.5f, 0, i * 0.5f + 0.5f, 1, VfxVertexBuffer.withAlpha(iris(i / (float) ns), 0.6f * (1 - t)), VfxVertexBuffer.withAlpha(iris(i / (float) ns + 0.2f), 0.3f * (1 - t)));
        }
        // shards of the dome bursting outward and falling
        int n = ctx.seg(28, 12);
        for (int i = 0; i < n; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI, el = 0.15f + 1.2f * hash(inst.seed, i, 2);
            float d = R * (1 + 0.8f * VfxAnim.easeOutCubic(t) * (0.5f + hash(inst.seed, i, 3)));
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * Mth.cos(el) * d, Mth.sin(el) * R - 6f * t * t, Mth.sin(ang) * Mth.cos(el) * d);
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, q, (0.6f + 0.9f * hash(inst.seed, i, 4)) * Math.min(2f, R * 0.12f), age * 0.3f * (hash(inst.seed, i, 5) - 0.5f) + i,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(iris(hash(inst.seed, i, 6)), 0.3f), fade));
        }
    }

    // ================================================================ Painting Magic
    /** A section of the stroke texture laid between a and b, U from u0 to u1 (so the stroke paints on rather than stretching). */
    static void strokePart(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float u0, float u1,
                           float width, int argb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f toCam = new Vector3f(a).add(b).mul(0.5f).negate();
        Vector3f s = new Vector3f(dir).cross(toCam);
        if (s.lengthSquared() < 1e-8f) return;
        s.normalize().mul(width * 0.5f);
        int col = blend.grade(argb, 0.8f);
        buf.quad(tex, blend, new Vector3f(a).sub(s), new Vector3f(b).sub(s), new Vector3f(b).add(s), new Vector3f(a).add(s), u0, 0, u1, 1, col, col);
    }

    private void stroke(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), w = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        int col = inst.color, wet = VfxVertexBuffer.whiten(col, 0.65f);
        float paint = VfxAnim.easeInOutSine(Mth.clamp(t / 0.5f, 0, 1)), fade = life(inst, age, 0, 6);
        // a gentle arc so it reads as a hand's stroke, painted on in segments
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.01f) return;
        Vector3f bow = side(new Vector3f(dir).div(len)).mul(len * 0.12f).add(0, len * 0.08f, 0);
        int seg = 6;
        Vector3f prev = new Vector3f(a);
        for (int k = 1; k <= seg; k++) {
            float f = (float) k / seg;
            if (f - 1f / seg > paint) break;
            float ff = Math.min(f, paint);
            Vector3f cur = new Vector3f(a).lerp(b, ff).add(new Vector3f(bow).mul(4 * ff * (1 - ff)));
            float u0 = (f - 1f / seg), u1 = ff;
            strokePart(buf, ctx, STROKE, VfxBlend.ALPHA, prev, cur, u0, u1, w, VfxVertexBuffer.withAlpha(col, fade));
            strokePart(buf, ctx, STROKE, VfxBlend.ADD, prev, cur, u0, u1, w * 0.35f, VfxVertexBuffer.withAlpha(wet, 0.45f * fade));   // the wet gloss
            prev = cur;
        }
        // droplets flicked off the brush
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float f = hash(inst.seed, i, 1) * paint, lt = Mth.clamp((paint - f) * 3, 0, 1);
            Vector3f q = new Vector3f(a).lerp(b, f).add(new Vector3f(bow).mul(4 * f * (1 - f)))
                    .add((hash(inst.seed, i, 2) - 0.5f) * w * 2 * lt, -1.2f * lt * lt, (hash(inst.seed, i, 3) - 0.5f) * w * 2 * lt);
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, q, 0.25f * w, i, VfxVertexBuffer.withAlpha(col, fade * (1 - lt * 0.5f)));
        }
        // the splash where it lands
        if (paint >= 1f) {
            float s = VfxAnim.easeOutBack(Mth.clamp((t - 0.45f) / 0.25f, 0, 1));
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, b, 1.6f * w * s, inst.seed % 7, VfxVertexBuffer.withAlpha(col, fade));
            buf.billboard(ctx, SPLAT, VfxBlend.ADD, b, 1.2f * w * s, inst.seed % 7, VfxVertexBuffer.withAlpha(wet, 0.35f * fade));
        }
    }

    private void splat(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.04f, 0);
        int col = inst.color, gloss = VfxVertexBuffer.whiten(col, 0.7f);
        float hit = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0, 1)), fade = life(inst, age, 0, 15);
        float dry = Mth.clamp((t - 0.3f) / 0.4f, 0, 1);                   // wet ink -> hard lacquer
        VfxPose ground = VfxPose.ground(g).spin(hash(inst.seed, 0, 1) * Mth.TWO_PI);
        buf.plane(SPLAT, VfxBlend.ALPHA, ground, r * hit, VfxVertexBuffer.withAlpha(ElementFx.mulRgb(col, 0.85f + 0.15f * dry), 0.95f * fade));
        buf.plane(SPLAT, VfxBlend.ADD, ground.lift(0.01f), r * hit * 0.92f, VfxVertexBuffer.withAlpha(gloss, (0.2f + 0.25f * dry) * fade));
        // drips running outward from the splash
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + hash(inst.seed, i, 2), run = Mth.clamp(age / 20f, 0, 1) * (0.4f + 0.6f * hash(inst.seed, i, 3));
            Vector3f s0 = new Vector3f(g).add(Mth.cos(ang) * r * 0.6f, 0.005f, Mth.sin(ang) * r * 0.6f);
            Vector3f s1 = new Vector3f(g).add(Mth.cos(ang) * r * (0.6f + 0.7f * run), 0.005f, Mth.sin(ang) * r * (0.6f + 0.7f * run));
            groundStrip(buf, STROKE2, VfxBlend.ALPHA, s0, s1, 0.25f * r, 0, 1, VfxVertexBuffer.withAlpha(col, fade));
        }
        // the lacquer gloss sweeping over it as it hardens
        if (dry > 0 && dry < 1) {
            float sweep = dry * 2.2f * r - 1.1f * r;
            Vector3f q0 = new Vector3f(g).add(sweep - 0.3f * r, 0.02f, -r), q1 = new Vector3f(g).add(sweep + 0.3f * r, 0.02f, r);
            groundStrip(buf, VfxTextures.GLOW, VfxBlend.ADD, q0, q1, 0.5f * r, 0, 1, VfxVertexBuffer.withAlpha(WHITE, 0.5f * Mth.sin(dry * Mth.PI) * fade));
        }
    }

    private void beast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color, wet = VfxVertexBuffer.whiten(col, 0.6f);
        float fade = life(inst, age, 0, 8);
        float lift = VfxAnim.easeOutCubic(Mth.clamp(t / 0.5f, 0, 1)), wrap = VfxAnim.easeInOutSine(Mth.clamp((t - 0.25f) / 0.5f, 0, 1));
        // flat strokes on the ground lift off and wrap round into the creature's outline
        int n = 7;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + wrap * 2.4f, h = hash(inst.seed, i, 1);
            float rad = p * (1.6f - 0.9f * wrap);
            float y0 = 0.05f + lift * p * (0.2f + 1.3f * h), y1 = y0 + 0.5f * p * wrap;
            Vector3f a = new Vector3f(g).add(Mth.cos(ang) * rad, y0, Mth.sin(ang) * rad);
            Vector3f b = new Vector3f(g).add(Mth.cos(ang + 0.9f) * rad, y1, Mth.sin(ang + 0.9f) * rad);
            strokePart(buf, ctx, i % 2 == 0 ? STROKE : STROKE2, VfxBlend.ALPHA, a, b, 0, 1, 0.45f * p, VfxVertexBuffer.withAlpha(col, fade * (1 - Mth.clamp((t - 0.8f) * 5, 0, 1) * 0.6f)));
        }
        // the glowing wet outline, breathing
        float show = Mth.clamp((t - 0.55f) / 0.25f, 0, 1);
        if (show > 0) {
            float breathe = 1 + 0.05f * Mth.sin(age * 0.5f);
            stand(ctx, buf, OUTLINE, VfxBlend.ADD, new Vector3f(g).add(0, 0.02f, 0), 2.4f * p * breathe, 2.4f * p * breathe, VfxVertexBuffer.withAlpha(wet, 0.8f * show * fade));
            buf.plane(OUTLINE, VfxBlend.ADD, VfxPose.ground(new Vector3f(g).add(0, 0.05f, 0)).spin(age * 0.02f), 1.6f * p, VfxVertexBuffer.withAlpha(col, 0.6f * show * fade));
            VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, p, 0), 1.6f * p, col, 0.5f * show * fade);
        }
        // paint flecks thrown off as it comes to life
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float lt = Mth.clamp(t * 1.5f - hash(inst.seed, i, 4) * 0.5f, 0, 1), ang = hash(inst.seed, i, 5) * Mth.TWO_PI;
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * 2f * p * lt, p * (0.5f + 1.2f * lt) - 1.5f * lt * lt, Mth.sin(ang) * 2f * p * lt);
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, q, 0.3f * p, i, VfxVertexBuffer.withAlpha(col, fade * Mth.sin(lt * Mth.PI)));
        }
    }

    private void camo(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f g = ctx.rel(inst.from(ctx));
        float fade = life(inst, age, 8, 12);
        // the refraction ripple sliding up the body in bands
        for (int k = 0; k < 3; k++) {
            float y = ((age * 0.03f + k / 3f) % 1f) * 2.1f * p;
            float s = 1.1f * p * (0.8f + 0.2f * Mth.sin(age * 0.2f + k));
            buf.billboard(ctx, REFRACT, VfxBlend.ALPHA, new Vector3f(g).add(0, y, 0), s, age * 0.03f + k, VfxVertexBuffer.withAlpha(inst.color, 0.18f * fade));
            buf.billboard(ctx, REFRACT, VfxBlend.ADD, new Vector3f(g).add(0, y, 0), s * 0.9f, -age * 0.02f + k, VfxVertexBuffer.withAlpha(WHITE, 0.12f * fade));
        }
        // a few flecks of paint drifting off
        int n = ctx.seg(6, 3);
        for (int i = 0; i < n; i++) {
            float lt = ((age * 0.02f) + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI + lt;
            Vector3f q = new Vector3f(g).add(Mth.cos(ang) * 0.6f * p, 0.3f + 1.8f * p * lt, Mth.sin(ang) * 0.6f * p);
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, q, 0.12f * p, i, VfxVertexBuffer.withAlpha(iris(hash(inst.seed, i, 3)), 0.6f * fade * Mth.sin(lt * Mth.PI)));
        }
    }
}
