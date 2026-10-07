package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.panel;
import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.stand;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.44: the Painting Magic remake (palette & brush), held to the Time Magic standard: wet glossy paint with a specular
 * highlight, drips and droplets with weight, a bloom core and a clear beginning, middle and end. Textures from
 * tools/gen_paint_studio_textures.py (greyscale, tinted per quad).
 * <ul>
 *   <li>PAINT_PALETTE: the palette manifests at the off hand ('from', board facing 'to'): the wooden board pops in, six blobs
 *       of mana paint arc up out of the air one after another and land in it as rippling, glossy pools.</li>
 *   <li>PAINT_TRAIL: a brush sweep from 'from' to 'to': a thick wet ribbon painted on fast along a slight arc, a glinting
 *       specular streak riding on it, heavy drips falling off, a glowing head and a splash where it ends. power = width.</li>
 *   <li>PAINT_EMERGE: a living illustration. Line art is painted flat on the ground at 'from', the canvas hinges up to stand
 *       facing 'to', and the drawing swells into a glowing wet-paint outline that breathes and sheds drips. The kind of drawing
 *       is in the seed's low two bits (0 beast, 1 knight, 2 giant). power = height.</li>
 *   <li>PAINT_SHIFT: the paint changes element at 'from': every colour on the palette swirls in and blends into the new one,
 *       which bursts out as droplets.</li>
 * </ul>
 */
public class PaintStudioLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation GLOSS = t("paint_gloss"), SPEC = t("paint_spec"), DROP = t("paint_drop"), POOL = t("paint_pool"),
            BOARD = t("paint_palette_vfx"), SWIRL = t("paint_swirl"), SPLAT = t("paint_splat"), RIPPLE = t("mirror_ripple"),
            OUTLINE = t("paint_outline");
    static final ResourceLocation[] SKETCH = {t("paint_sketch_beast"), t("paint_sketch_knight"), t("paint_sketch_giant")};

    /** The palette's paints, in pool order: ink, fire, water, ice, wind, earth (lightning is mixed on the brush). */
    static final int[] PAINTS = {0xFF3A7BFF, 0xFFFF5A3A, 0xFF4AA8FF, 0xFF8AE6FF, 0xFF7CF0B0, 0xFFB0864A, 0xFFFFE65A};
    static final int WOOD = 0xFFC08A55;

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.PAINT_PALETTE, VfxShape.PAINT_TRAIL, VfxShape.PAINT_EMERGE, VfxShape.PAINT_SHIFT);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case PAINT_PALETTE -> 30;
            case PAINT_TRAIL -> 16;
            case PAINT_EMERGE -> 30;
            default -> 14;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return 0xFF3A7BFF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case PAINT_PALETTE -> palette(inst, ctx, buf);
            case PAINT_TRAIL -> trail(inst, ctx, buf);
            case PAINT_EMERGE -> emerge(inst, ctx, buf);
            case PAINT_SHIFT -> shift(inst, ctx, buf);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- PAINT_PALETTE
    private void palette(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f c = ctx.rel(inst.from(ctx)), n = ctx.rel(inst.to(ctx)).sub(c);
        if (n.lengthSquared() < 1e-4f) n.set(0, 1, 0);
        n.normalize();
        Vector3f right = side(n), up = new Vector3f(right).cross(n).normalize();
        float pop = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0, 1)), fade = life(inst, age, 0, 8);
        // the board, a warm rim light round it
        panel(buf, BOARD, VfxBlend.ALPHA, c, right, up, 0.5f * s * pop, 0.5f * s * pop, VfxVertexBuffer.withAlpha(WOOD, fade));
        panel(buf, BOARD, VfxBlend.ADD, new Vector3f(c).add(new Vector3f(n).mul(0.01f)), right, up, 0.53f * s * pop, 0.53f * s * pop,
                VfxVertexBuffer.withAlpha(0xFFFFE0B0, 0.18f * fade));
        // six blobs arc up and land in their pools one after another
        for (int k = 0; k < 6; k++) {
            float ang = -0.2f + k * 0.62f;                                            // round the board, clear of the thumb hole
            Vector3f slot = new Vector3f(c).add(new Vector3f(right).mul(Mth.cos(ang) * 0.3f * s)).add(new Vector3f(up).mul(Mth.sin(ang) * 0.24f * s))
                    .add(new Vector3f(n).mul(0.02f));
            float land = Mth.clamp((age - 4 - k * 2.2f) / 7f, 0, 1);
            int col = PAINTS[k];
            if (land <= 0) continue;
            if (land < 1) {                                                           // in flight: a droplet on an arc
                Vector3f from = new Vector3f(c).add(new Vector3f(right).mul((k - 2.5f) * 0.5f * s)).add(new Vector3f(n).mul(-0.6f * s)).add(0, -0.4f * s, 0);
                Vector3f q = new Vector3f(from).lerp(slot, VfxAnim.easeInOutSine(land)).add(new Vector3f(n).mul(Mth.sin(land * Mth.PI) * 0.5f * s));
                buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.09f * s, 0, VfxVertexBuffer.withAlpha(col, fade));
                buf.billboard(ctx, DROP, VfxBlend.ADD, q, 0.11f * s, 0, VfxVertexBuffer.withAlpha(col, 0.4f * fade));
                continue;
            }
            // landed: a rippling pool with a gloss, a ring spreading out once
            float since = age - 4 - k * 2.2f - 7, wob = 1 + 0.08f * Mth.sin(age * 0.5f + k) * Mth.clamp(1 - since / 30f, 0.3f, 1);
            float splash = VfxAnim.easeOutBack(Mth.clamp(since / 4f, 0, 1));
            VfxPose pose = new VfxPose(slot, right, up, n).spin(k * 1.3f);
            buf.plane(POOL, VfxBlend.ALPHA, pose, 0.095f * s * splash * wob, VfxVertexBuffer.withAlpha(col, fade));
            buf.plane(POOL, VfxBlend.ADD, pose.lift(0.004f), 0.1f * s * splash * wob, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), 0.5f * fade));
            float rp = Mth.clamp(since / 8f, 0, 1);
            if (rp < 1) buf.plane(RIPPLE, VfxBlend.ADD, pose.lift(0.006f), 0.06f * s + 0.12f * s * rp, VfxVertexBuffer.withAlpha(col, 0.5f * (1 - rp) * fade));
        }
        // the manifest flash and a few drifting motes of pigment
        float flash = Mth.clamp(1 - age / 6f, 0, 1);
        if (flash > 0) VfxBloom.glow(ctx, buf, c, 1.4f * s, 0xFFFFF0D0, flash * fade);
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float lt = (t * 1.4f + hash(inst.seed, i, 1)) % 1f, a = hash(inst.seed, i, 2) * Mth.TWO_PI;
            Vector3f q = new Vector3f(c).add(Mth.cos(a) * 0.5f * s, (lt - 0.3f) * 0.8f * s, Mth.sin(a) * 0.5f * s);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.06f * s, age * 0.1f, VfxVertexBuffer.withAlpha(PAINTS[i % PAINTS.length], fade * Mth.sin(lt * Mth.PI)));
        }
    }

    // ---------------------------------------------------------------- PAINT_TRAIL
    private void trail(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), w = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.01f) return;
        int col = inst.color, wet = VfxVertexBuffer.whiten(col, 0.7f);
        float paint = VfxAnim.easeOutCubic(Mth.clamp(t / 0.3f, 0, 1)), fade = life(inst, age, 0, 6);
        Vector3f bow = side(new Vector3f(dir).div(len)).mul(len * 0.1f).add(0, len * 0.06f, 0);
        int seg = ctx.seg(10, 5);
        Vector3f prev = new Vector3f(a);
        float dry = Mth.clamp((t - 0.5f) / 0.5f, 0, 1);                               // the gloss dulls as it dries
        for (int k = 1; k <= seg; k++) {
            float f = (float) k / seg;
            if (f - 1f / seg > paint) break;
            float ff = Math.min(f, paint);
            Vector3f cur = new Vector3f(a).lerp(b, ff).add(new Vector3f(bow).mul(4 * ff * (1 - ff)));
            float u0 = f - 1f / seg, u1 = ff, taper = 0.6f + 0.4f * Mth.sin(ff * Mth.PI);
            DreamPaintLayer.strokePart(buf, ctx, GLOSS, VfxBlend.ALPHA, prev, cur, u0, u1, w * taper, VfxVertexBuffer.withAlpha(col, fade));
            DreamPaintLayer.strokePart(buf, ctx, GLOSS, VfxBlend.ADD, prev, cur, u0, u1, w * taper * 1.25f, VfxVertexBuffer.withAlpha(col, 0.25f * fade));
            Vector3f lift = new Vector3f(0, w * 0.12f, 0);                             // the specular streak sits on the upper side
            DreamPaintLayer.strokePart(buf, ctx, SPEC, VfxBlend.ADD, new Vector3f(prev).add(lift), new Vector3f(cur).add(lift), u0 + age * 0.04f, u1 + age * 0.04f,
                    w * 0.35f, VfxVertexBuffer.withAlpha(wet, 0.8f * (1 - 0.7f * dry) * fade));
            prev = cur;
        }
        // the glowing head while it paints
        if (paint < 1) VfxBloom.glow(ctx, buf, prev, 0.9f * w + 0.3f, col, 0.7f * fade);
        // heavy drips falling off the ribbon
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float f = hash(inst.seed, i, 1);
            if (f > paint) continue;
            float fall = Mth.clamp((t - f * 0.3f - 0.05f) * 2.2f, 0, 1);
            Vector3f q = new Vector3f(a).lerp(b, f).add(new Vector3f(bow).mul(4 * f * (1 - f))).add(0, -w * 0.3f - 1.8f * fall * fall, 0);
            buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, (0.12f + 0.08f * hash(inst.seed, i, 2)) * (0.6f + w), 0, VfxVertexBuffer.withAlpha(col, fade * (1 - fall * 0.6f)));
        }
        // the splash where it lands
        if (paint >= 1) {
            float sp = VfxAnim.easeOutBack(Mth.clamp((t - 0.3f) / 0.2f, 0, 1));
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, b, 1.4f * (0.4f + w) * sp, inst.seed % 7, VfxVertexBuffer.withAlpha(col, fade));
            buf.billboard(ctx, SPLAT, VfxBlend.ADD, b, 1.1f * (0.4f + w) * sp, inst.seed % 7, VfxVertexBuffer.withAlpha(wet, 0.35f * fade));
        }
    }

    // ---------------------------------------------------------------- PAINT_EMERGE
    private void emerge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), h = inst.power, t = age / inst.duration;
        int kind = (int) (inst.seed & 3) % SKETCH.length;
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.03f, 0), face = ctx.rel(inst.to(ctx)).sub(ctx.rel(inst.from(ctx)));
        face.y = 0;
        if (face.lengthSquared() < 1e-4f) face.set(0, 0, 1);
        face.normalize();
        Vector3f right = new Vector3f(face).cross(0, 1, 0).normalize();
        int col = inst.color, wet = VfxVertexBuffer.whiten(col, 0.6f);
        float fade = life(inst, age, 0, 6);
        float draw = Mth.clamp(t / 0.35f, 0, 1), hinge = VfxAnim.easeInOutSine(Mth.clamp((t - 0.3f) / 0.3f, 0, 1));
        float swell = VfxAnim.easeOutBack(Mth.clamp((t - 0.6f) / 0.25f, 0, 1));
        // the canvas: flat on the ground (normal up), hinged at its front edge up to standing (normal = face)
        float ang = hinge * Mth.HALF_PI;
        Vector3f up = new Vector3f(face).mul(Mth.cos(ang)).add(0, Mth.sin(ang), 0).normalize();          // along the ground -> upright
        Vector3f n = new Vector3f(0, Mth.cos(ang), 0).add(new Vector3f(face).mul(-Mth.sin(ang))).normalize();
        float half = 0.5f * h;
        Vector3f center = new Vector3f(g).add(new Vector3f(up).mul(half));
        // painted on: the sketch revealed by a wipe, under a wet ink wash
        float reveal = draw;
        panel(buf, SKETCH[kind], VfxBlend.ALPHA, center, right, up, half * Math.max(0.05f, reveal), half,
                VfxVertexBuffer.withAlpha(col, fade * (1 - 0.85f * swell)));
        panel(buf, SKETCH[kind], VfxBlend.ADD, new Vector3f(center).add(new Vector3f(n).mul(0.01f)), right, up, half * Math.max(0.05f, reveal), half,
                VfxVertexBuffer.withAlpha(wet, (0.25f + 0.5f * hinge) * (1 - 0.7f * swell) * fade));
        if (draw < 1) {                                                              // the brush tip running along as it paints
            Vector3f tip = new Vector3f(center).add(new Vector3f(right).mul(half * (2 * draw - 1)));
            VfxBloom.glow(ctx, buf, tip, 0.6f * h, col, 0.6f * fade);
        }
        // swelling into a solid, breathing outline (the drawing stands up off the canvas)
        if (swell > 0) {
            float breathe = 1 + 0.04f * Mth.sin(age * 0.6f);
            stand(ctx, buf, SKETCH[kind], VfxBlend.ADD, g, 1.05f * h * swell * breathe, 1.05f * h * swell * breathe, VfxVertexBuffer.withAlpha(wet, 0.75f * fade));
            stand(ctx, buf, OUTLINE, VfxBlend.ADD, g, 1.3f * h * swell, 1.3f * h * swell, VfxVertexBuffer.withAlpha(col, 0.35f * fade));
            buf.plane(SPLAT, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(g)).spin(inst.seed % 7), 0.9f * h * swell, VfxVertexBuffer.withAlpha(col, 0.8f * fade));
            VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.5f * h, 0), 1.2f * h, col, 0.5f * swell * fade);
        }
        // drips shed off it as it rises
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float lt = Mth.clamp((t - 0.45f) * 2.5f - hash(inst.seed, i, 3) * 0.6f, 0, 1);
            if (lt <= 0 || lt >= 1) continue;
            float x = (hash(inst.seed, i, 4) - 0.5f) * h, y0 = 0.2f * h + 0.7f * h * hash(inst.seed, i, 5);
            Vector3f q = new Vector3f(g).add(new Vector3f(right).mul(x)).add(0, y0 - (y0 + 0.05f) * lt * lt, 0);
            buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.1f * h * 0.6f + 0.05f, 0, VfxVertexBuffer.withAlpha(col, fade * (1 - lt * 0.5f)));
        }
    }

    // ---------------------------------------------------------------- PAINT_SHIFT
    private void shift(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color;
        float fade = life(inst, age, 0, 4), in = VfxAnim.easeInCubic(Mth.clamp(t / 0.6f, 0, 1));
        // every colour of the palette swirls in, blending to the new one
        for (int k = 0; k < 3; k++) {
            int mix = VfxVertexBuffer.lerpColor(PAINTS[(k * 2 + (int) (inst.seed & 7)) % PAINTS.length], col, in);
            float size = s * (0.9f - 0.55f * in) * (1 - 0.15f * k);
            buf.billboard(ctx, SWIRL, VfxBlend.ALPHA, c, size, age * (0.35f + 0.1f * k) + k, VfxVertexBuffer.withAlpha(mix, 0.7f * fade * (1 - Mth.clamp((t - 0.6f) * 3, 0, 1))));
        }
        buf.billboard(ctx, SWIRL, VfxBlend.ADD, c, s * (0.8f - 0.5f * in), -age * 0.3f, VfxVertexBuffer.withAlpha(col, 0.5f * fade));
        // it pops: the new colour bursts out as droplets
        float burst = Mth.clamp((t - 0.6f) / 0.4f, 0, 1);
        if (burst > 0) {
            VfxBloom.glow(ctx, buf, c, 1.2f * s, col, (1 - burst) * fade);
            int n = ctx.seg(10, 5);
            for (int i = 0; i < n; i++) {
                float a = Mth.TWO_PI * i / n + hash(inst.seed, i, 1), r = s * 0.8f * VfxAnim.easeOutCubic(burst);
                Vector3f q = new Vector3f(c).add(Mth.cos(a) * r, 0.3f * s * Mth.sin(i * 1.7f) - 0.4f * s * burst * burst, Mth.sin(a) * r);
                buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.13f * s, 0, VfxVertexBuffer.withAlpha(col, fade * (1 - burst * 0.5f)));
            }
        }
    }
}
