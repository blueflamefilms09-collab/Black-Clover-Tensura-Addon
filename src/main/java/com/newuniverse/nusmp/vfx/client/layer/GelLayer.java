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
 * Gel Magic (Sally's gel salamander), drawn after the owner's still: a translucent violet-grey gel with a dark cel outline, lighter
 * swirl blotches inside, bright glossy streaks and ovals on top, a curling salamander, and huge white bars with a lavender edge
 * radiating out of it. The sprites are the {@code gel_cel_*} set (tools/gen_gel_ref_textures.py); the older mint jelly set
 * ({@code gel_blob}, {@code gel_puddle} ... with {@link GelFx}) stays registered. Gel bodies are drawn ALPHA (tinted violet), their
 * gloss and the bars ADD (so the white shines on top).
 * <ul>
 *   <li>GEL_FX1 = cast / projectile (Gel Dart). A violet swirl gathers at {@code from} with bubbles pulled into it and a flash, then a
 *       glossy gel blob flies to {@code to} on a slight lob, trailing a stretching sticky strand, fading afterimage blobs and drops
 *       that fall off it, and ends in a splat with a ripple. power = size scale, duration = flight ticks.</li>
 *   <li>GEL_FX2 = zone / field (Gel Domain). A translucent gel puddle covers the ground to the radius with a counter-rotating swirl
 *       and a curled salamander sigil, a dripping gel rim at the edge, glossy gel domes breathing along the rim, bubbles rising
 *       and a ripple pulsing out. power = radius in blocks, duration = life ticks (fades in 8, out 12).</li>
 *   <li>GEL_FX3 = impact / signature (Gel Burst). A flash, white-lavender bars radiating out of {@code from} (as in the still), a
 *       big gel splat blooming, a dripping gel shock ring and ripples on the ground, drops flying out on arcs, sparks and a lingering
 *       puddle with rising bubbles. to - from (if not zero) leans the bars and drops that way. power = scale.</li>
 * </ul>
 * colour: mixed a quarter into the violet gel palette, so a white tint still reads as gel.
 */
public class GelLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation BLOB = t("gel_cel_blob"), SWIRL = t("gel_cel_swirl"), GLOSS = t("gel_cel_gloss"), DROP = t("gel_cel_drop"),
            STRAND = t("gel_cel_strand"), RING = t("gel_cel_ring"), BAR = t("gel_cel_bar"), SPLAT = t("gel_cel_splat"),
            BUBBLE = t("gel_cel_bubble"), SALA = t("gel_cel_salamander"), RIPPLE = t("gel_cel_ripple");

    private static final int GEL = 0xFFB7A6F2, DEEP = 0xFF6A58A8, HI = 0xFFF4EEFF, BARC = 0xFFDCCFFF, GLOWC = 0xFFC9B8FF, MINT = 0xFF9CFFE4;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.GEL_FX1, VfxShape.GEL_FX2, VfxShape.GEL_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case GEL_FX1 -> 16;
            case GEL_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFB9A8F0; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        Pal pal = new Pal(inst);
        switch (inst.shape) {
            case GEL_FX1 -> cast(inst, ctx, buf, pal);
            case GEL_FX2 -> zone(inst, ctx, buf, pal);
            case GEL_FX3 -> burst(inst, ctx, buf, pal);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ palette and helpers
    private static final class Pal {
        final int gel, deep, hi, glow, bar, mint;
        Pal(VfxInstance inst) {
            int tint = inst.color | 0xFF000000;
            gel = VfxVertexBuffer.lerpColor(GEL, tint, 0.25f);
            deep = VfxVertexBuffer.lerpColor(DEEP, tint, 0.25f);
            hi = VfxVertexBuffer.lerpColor(HI, tint, 0.10f);
            glow = VfxVertexBuffer.lerpColor(GLOWC, tint, 0.30f);
            bar = VfxVertexBuffer.lerpColor(BARC, tint, 0.15f);
            mint = MINT;
        }
    }

    private static int al(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0f, 1f)); }

    private static float sstep(float a, float b, float x) {
        float u = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return u * u * (3f - 2f * u);
    }

    private static float c01(float x) { return Mth.clamp(x, 0f, 1f); }

    private static Vector3f at(Vector3f base, Vector3f off) { return new Vector3f(base).add(off); }

    /** Billboard offset in the screen plane. */
    private static Vector3f screen(VfxRenderContext ctx, Vector3f base, float x, float y) {
        return new Vector3f(base).add(new Vector3f(ctx.camRight).mul(x)).add(new Vector3f(ctx.camUp).mul(y));
    }

    /** Camera-facing ribbon through pts with widths w[i]; texture V runs 0..1 along it (straight, no per-segment flip). */
    private static void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f[] pts, float[] w, int c0, int c1) {
        int n = pts.length;
        Vector3f[] l = new Vector3f[n], r = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Vector3f d = new Vector3f(pts[Math.min(n - 1, i + 1)]).sub(pts[Math.max(0, i - 1)]);
            Vector3f toCam = new Vector3f(pts[i]).negate();
            Vector3f side = d.cross(toCam, new Vector3f());
            if (side.lengthSquared() < 1e-8f) side.set(0, 1, 0);
            side.normalize().mul(w[i] * 0.5f);
            l[i] = new Vector3f(pts[i]).sub(side);
            r[i] = new Vector3f(pts[i]).add(side);
        }
        for (int i = 0; i < n - 1; i++) {
            float v0 = (float) i / (n - 1), v1 = (float) (i + 1) / (n - 1);
            int a = VfxVertexBuffer.lerpColor(c0, c1, v0), b = VfxVertexBuffer.lerpColor(c0, c1, v1);
            buf.quad(tex, blend, l[i], r[i], r[i + 1], l[i + 1], 0, v1, 1, v0, a, b);
        }
    }

    /** A gel blob with its gloss: body ALPHA, light ADD. rot spins the body only (the gloss stays upright like cel shading). */
    private static void jelly(VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal, Vector3f c, float size, float rot, float alpha) {
        buf.billboard(ctx, BLOB, VfxBlend.ALPHA, c, size, rot, al(pal.gel, alpha));
        buf.billboard(ctx, GLOSS, VfxBlend.ADD, c, size * 0.9f, 0f, al(pal.hi, alpha * 0.9f));
    }

    private static float arcY(float s, float h) { return Mth.sin(s * Mth.PI) * h; }

    // ------------------------------------------------------------------ FX1: Gel Dart
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(4, inst.duration), p = c01(age / D);
        float pw = Mth.clamp(inst.power, 0.4f, 3.5f);
        RandomSource rng = inst.random();
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float len = dir.length();
        if (len < 0.5f) { dir.set(0, 0, 1).mul(3f); to = at(f, dir); len = 3f; }
        float lob = Math.min(len, 14f) * 0.07f;

        // 1. charge: swirl + converging bubbles + flash (first quarter)
        float pc = c01(p / 0.26f), fadeC = 1f - sstep(0.24f, 0.42f, p);
        if (fadeC > 0.01f) {
            buf.billboard(ctx, GLOW(), VfxBlend.ADD, f, pw * (0.5f + 1.3f * VfxAnim.easeOutCubic(pc)), 0f, al(pal.glow, 0.65f * fadeC));
            buf.billboard(ctx, SWIRL, VfxBlend.ALPHA, f, pw * (0.35f + 0.85f * VfxAnim.easeOutCubic(pc)), age * 0.55f, al(pal.gel, 0.95f * fadeC));
            buf.billboard(ctx, SWIRL, VfxBlend.ADD, f, pw * (0.3f + 0.5f * pc), -age * 0.8f, al(pal.hi, 0.55f * fadeC));
            for (int i = 0; i < 5; i++) {
                float a = rng.nextFloat() * Mth.TWO_PI + i, rr = (1.3f - 1.15f * pc) * pw * (0.7f + 0.3f * rng.nextFloat());
                Vector3f bp = screen(ctx, f, Mth.cos(a + pc) * rr, Mth.sin(a + pc) * rr);
                buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, bp, pw * 0.2f, 0f, al(pal.gel, 0.9f * fadeC));
            }
        }
        float flash = Math.max(0f, 1f - Math.abs(p - 0.24f) / 0.1f);
        if (flash > 0f) buf.billboard(ctx, GLOW(), VfxBlend.ADD, f, pw * 2.4f * flash, 0f, al(pal.hi, 0.85f * flash));

        // 2. the dart in flight
        float tf = c01((p - 0.14f) / 0.68f);
        float s = tf * 0.7f + tf * tf * (3f - 2f * tf) * 0.3f;
        float vis = sstep(0.12f, 0.19f, p) * (1f - sstep(0.88f, 0.97f, p));
        if (vis > 0.01f) {
            Vector3f head = at(at(f, new Vector3f(dir).mul(s)), new Vector3f(0, arcY(s, lob), 0));
            float tail = Math.max(0f, s - 0.32f);
            Vector3f[] pts = new Vector3f[5];
            float[] w = new float[5];
            for (int i = 0; i < 5; i++) {
                float k = i / 4f, ss = Mth.lerp(k, tail, s);
                float sag = Mth.sin(k * Mth.PI) * 0.12f * pw * Mth.sin(age * 1.3f + i * 0.9f);
                pts[i] = at(at(f, new Vector3f(dir).mul(ss)), new Vector3f(0, arcY(ss, lob) - sag - (1 - k) * 0.05f * pw, 0));
                w[i] = pw * (0.1f + 0.4f * Mth.sin(Mth.PI * (0.25f + 0.75f * k)) * (0.6f + 0.4f * k));
            }
            ribbon(buf, STRAND, VfxBlend.ALPHA, pts, w, al(pal.deep, 0.15f * vis), al(pal.gel, 0.95f * vis));
            // afterimage blobs left behind
            for (int k = 1; k <= 3; k++) {
                float ss = Math.max(0f, s - 0.09f * k);
                Vector3f ap = at(at(f, new Vector3f(dir).mul(ss)), new Vector3f(0, arcY(ss, lob) - 0.04f * k * pw, 0));
                buf.billboard(ctx, BLOB, VfxBlend.ALPHA, ap, pw * (0.9f - 0.17f * k), age * 0.2f + k, al(pal.deep, (0.55f - 0.15f * k) * vis));
            }
            // drops shed from the strand, falling
            for (int k = 0; k < 4; k++) {
                float born = 0.18f + 0.13f * k;
                if (p < born) continue;
                float u = (p - born) * D / 20f, ss = Math.max(0f, born - 0.14f) / 0.68f * 0.9f;
                Vector3f dp = at(at(f, new Vector3f(dir).mul(Math.min(1f, ss))), new Vector3f(0, arcY(ss, lob) - 3.5f * u * u - 0.1f * pw, 0));
                buf.billboard(ctx, DROP, VfxBlend.ALPHA, dp, pw * 0.2f, (k - 1.5f) * 0.3f, al(pal.gel, 0.9f * vis * (1f - sstep(0.5f, 1f, u * 2.5f))));
            }
            // head: halo, body, gloss; squash-stretch by spinning
            float wob = 1f + 0.1f * Mth.sin(age * 1.9f);
            buf.billboard(ctx, GLOW(), VfxBlend.ADD, head, pw * 2.0f, 0f, al(pal.glow, 0.4f * vis));
            jelly(ctx, buf, pal, head, pw * 1.15f * wob, age * 0.3f, vis);
        }

        // 3. impact splat at the target
        float ti = c01((p - 0.80f) / 0.2f);
        if (ti > 0f) {
            float e = VfxAnim.easeOutCubic(ti);
            buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, to, pw * (0.45f + 1.1f * e), 0.4f, al(pal.gel, 0.95f * (1f - ti * 0.5f)));
            buf.billboard(ctx, GLOW(), VfxBlend.ADD, to, pw * (0.8f + 1.4f * e), 0f, al(pal.hi, 0.8f * (1f - ti)));
            buf.billboard(ctx, RIPPLE, VfxBlend.ADD, to, pw * (0.6f + 2.0f * e), 0f, al(pal.glow, 0.7f * (1f - ti)));
            for (int k = 0; k < 6; k++) {
                float a = k * 1.047f + rng.nextFloat();
                float rr = pw * (0.3f + 1.2f * e) * (0.7f + 0.3f * rng.nextFloat());
                Vector3f dp = screen(ctx, to, Mth.cos(a) * rr, Mth.sin(a) * rr - 0.5f * ti * ti * pw);
                buf.billboard(ctx, DROP, VfxBlend.ALPHA, dp, pw * 0.16f, a - Mth.HALF_PI, al(pal.gel, 0.9f * (1f - ti)));
            }
        }
    }

    private static ResourceLocation GLOW() { return VfxTextures.GLOW; }

    // ------------------------------------------------------------------ FX2: Gel Domain
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(20, inst.duration);
        float R = Math.max(1f, inst.power);
        float fi = c01(age / 8f), fo = c01((D - age) / 12f), L = fi * fo;
        if (L <= 0.01f) return;
        float open = VfxAnim.easeOutBack(c01(age / 12f));
        RandomSource rng = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(c);

        // puddle, swirl, sigil (back to front, counter-rotating)
        buf.plane(BLOB, VfxBlend.ALPHA, ground.lift(0.03f).spin(age * 0.008f), R * 1.06f * open, al(pal.deep, 0.6f * L));
        buf.plane(SWIRL, VfxBlend.ALPHA, ground.lift(0.05f).spin(-age * 0.03f), R * 0.9f * open, al(pal.gel, 0.55f * L));
        buf.plane(SALA, VfxBlend.ALPHA, ground.lift(0.06f).spin(age * 0.02f), R * 0.72f * open, al(pal.hi, 0.5f * L));
        buf.plane(SALA, VfxBlend.ADD, ground.lift(0.07f).spin(age * 0.02f), R * 0.72f * open, al(pal.glow, 0.35f * L * (0.6f + 0.4f * VfxAnim.pulse(age, 1.2f))));
        // dripping rim
        int segs = ctx.seg(26, 12), rep = Math.max(1, Math.round(R * 0.9f));
        float bulge = 1f + 0.015f * Mth.sin(age * 0.2f);
        buf.ring(RING, VfxBlend.ALPHA, ground.lift(0.08f), R * 0.9f * open * bulge, R * 1.04f * open * bulge, segs, rep, age * 0.003f, al(pal.gel, 0.95f * L));
        // glossy domes breathing along the rim
        int domes = 7;
        for (int k = 0; k < domes; k++) {
            float a = Mth.TWO_PI * k / domes + age * 0.012f;
            float breathe = 0.7f + 0.3f * Mth.sin(age * 0.13f + k * 1.7f);
            float sz = Mth.clamp(R * 0.3f, 0.55f, 1.5f) * breathe * open;
            Vector3f dp = new Vector3f(c).add(Mth.cos(a) * R * 0.88f * open, sz * 0.38f, Mth.sin(a) * R * 0.88f * open);
            jelly(ctx, buf, pal, dp, sz, k * 1.3f, 0.85f * L);
        }
        // bubbles rising through the gel
        for (int k = 0; k < 9; k++) {
            float a = rng.nextFloat() * Mth.TWO_PI, rr = (0.15f + 0.75f * rng.nextFloat()) * R;
            float ph = (age * (0.012f + 0.01f * rng.nextFloat()) + rng.nextFloat()) % 1f;
            float sz = (0.14f + 0.16f * rng.nextFloat()) * Mth.clamp(R * 0.4f, 0.8f, 1.5f);
            Vector3f bp = new Vector3f(c).add(Mth.cos(a) * rr + Mth.sin(age * 0.1f + k) * 0.1f, 0.1f + ph * R * 0.55f, Mth.sin(a) * rr);
            buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, bp, sz * (0.6f + 0.6f * ph), 0f, al(pal.hi, 0.85f * L * (1f - sstep(0.7f, 1f, ph))));
        }
        // pulse ripples
        for (int k = 0; k < 2; k++) {
            float ph = ((age / 30f) + k * 0.5f) % 1f;
            buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.1f), R * (0.2f + 0.9f * VfxAnim.easeOutCubic(ph)), al(pal.glow, 0.7f * (1f - ph) * L));
        }
        buf.billboard(ctx, GLOW(), VfxBlend.ADD, new Vector3f(c).add(0, 0.2f, 0), R * 1.1f, 0f, al(pal.glow, 0.16f * L));
    }

    // ------------------------------------------------------------------ FX3: Gel Burst
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Pal pal) {
        float age = inst.ageTicks(ctx.partialTick), D = Math.max(8, inst.duration), p = c01(age / D);
        float pw = Mth.clamp(inst.power, 0.4f, 3.5f);
        RandomSource rng = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean hinted = hint.lengthSquared() > 0.04f;
        if (hinted) hint.normalize();
        VfxPose ground = VfxPose.ground(new Vector3f(c).sub(0, 0.0f, 0));

        // afterglow puddle (back)
        float pud = sstep(0.04f, 0.25f, p) * (1f - sstep(0.72f, 1f, p));
        buf.plane(BLOB, VfxBlend.ALPHA, ground.lift(0.03f).spin(age * 0.01f), pw * 1.7f * VfxAnim.easeOutCubic(c01(p / 0.35f)), al(pal.deep, 0.65f * pud));
        buf.plane(SWIRL, VfxBlend.ALPHA, ground.lift(0.05f).spin(-age * 0.05f), pw * 1.3f * VfxAnim.easeOutCubic(c01(p / 0.35f)), al(pal.gel, 0.5f * pud));

        // gel shock ring + ripples on the ground
        float q = c01(p / 0.6f), eq = VfxAnim.easeOutCubic(q);
        if (q < 1f) {
            float rr = pw * (0.5f + 2.6f * eq);
            buf.ring(RING, VfxBlend.ALPHA, ground.lift(0.06f), rr * 0.85f, rr, ctx.seg(20, 10), Math.max(1, Math.round(rr * 0.9f)), 0f, al(pal.gel, 0.95f * (1f - q * q)));
        }
        for (int k = 0; k < 2; k++) {
            float qk = c01((p - 0.06f * k) / 0.62f);
            if (qk < 1f) buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.09f), pw * (0.5f + 3.6f * VfxAnim.easeOutCubic(qk)), al(pal.glow, 0.8f * (1f - qk)));
        }

        // splat blooming
        float sp = c01(p / 0.3f);
        float sa = 0.92f * (1f - sstep(0.5f, 1f, p));
        Vector3f cc = new Vector3f(c).add(0, 0.25f * pw, 0);
        buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, cc, pw * (0.7f + 2.6f * VfxAnim.easeOutBack(sp)), rng.nextFloat() * 6f, al(pal.gel, sa));
        buf.billboard(ctx, SPLAT, VfxBlend.ADD, cc, pw * (0.5f + 2.3f * VfxAnim.easeOutBack(sp)), 1.2f, al(pal.hi, sa * 0.35f));
        buf.billboard(ctx, GLOSS, VfxBlend.ADD, cc, pw * (0.5f + 2.0f * VfxAnim.easeOutBack(sp)), 0f, al(pal.hi, sa));

        // bars radiating out of the centre, as in the still
        for (int k = 0; k < 9; k++) {
            float yy = 1f - (k + 0.5f) / 9f * 2f, rr = Mth.sqrt(Math.max(0f, 1f - yy * yy)), ang = k * 2.39996f + rng.nextFloat() * 0.5f;
            Vector3f d = new Vector3f(Mth.cos(ang) * rr, yy * 0.7f, Mth.sin(ang) * rr);
            if (hinted) d.add(new Vector3f(hint).mul(1.2f));
            if (d.lengthSquared() < 1e-4f) d.set(0, 1, 0);
            d.normalize();
            float stagger = 0.04f * (k % 3);
            float pk = c01((p - stagger) / 0.4f);
            float outer = pw * (0.5f + 3.8f * VfxAnim.easeOutCubic(pk)) * (0.8f + 0.4f * ((k * 37 % 10) / 10f));
            float inner = pw * 0.15f + outer * 0.9f * sstep(0.25f, 0.9f, p);
            float al = (1f - sstep(0.35f, 0.85f, p)) * (0.9f - 0.04f * k);
            if (al <= 0.01f || inner >= outer - 0.1f) continue;
            float wd = pw * (0.5f + 0.14f * (k % 3)) * (1f - 0.45f * p);
            Vector3f a = at(cc, new Vector3f(d).mul(outer)), b = at(cc, new Vector3f(d).mul(inner));
            buf.beam(ctx, BAR, VfxBlend.ADD, a, b, wd, wd * 0.35f, 1, 0f, al(pal.bar, al), al(pal.bar, al * 0.5f));
        }

        // flash on top
        float fl = Math.max(0f, 1f - p / 0.35f);
        buf.billboard(ctx, GLOW(), VfxBlend.ADD, cc, pw * (1.2f + 3.4f * VfxAnim.easeOutCubic(c01(p / 0.15f))), 0f, al(pal.hi, 0.85f * fl * fl));

        // drops flung on arcs
        for (int k = 0; k < 12; k++) {
            float a = rng.nextFloat() * Mth.TWO_PI, sp2 = (1.4f + 2.2f * rng.nextFloat()) * pw, up = (0.9f + 1.6f * rng.nextFloat()) * pw;
            Vector3f v = new Vector3f(Mth.cos(a) * sp2, up, Mth.sin(a) * sp2);
            if (hinted) v.add(new Vector3f(hint).mul(1.6f * pw));
            float u = age / 20f;
            Vector3f dp = at(cc, new Vector3f(v).mul(u)).sub(0, 4.8f * u * u * 0.5f * Math.max(1f, pw * 0.6f), 0);
            float da = 0.95f * (1f - sstep(0.55f, 1f, p));
            float phi = (float) Math.atan2(v.dot(ctx.camUp) - 4.8f * u, v.dot(ctx.camRight));
            buf.billboard(ctx, DROP, VfxBlend.ALPHA, dp, pw * (0.2f + 0.14f * rng.nextFloat()), phi - Mth.HALF_PI, al(pal.gel, da));
        }
        // sparks
        for (int k = 0; k < 6; k++) {
            float a = rng.nextFloat() * Mth.TWO_PI, rr = pw * (0.8f + 2.6f * VfxAnim.easeOutCubic(c01(p / 0.6f))) * (0.5f + 0.5f * rng.nextFloat());
            Vector3f sp3 = screen(ctx, cc, Mth.cos(a) * rr, Mth.sin(a) * rr * 0.8f + 0.2f * pw * p);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, sp3, pw * 0.22f, a, al(k % 2 == 0 ? pal.hi : pal.mint, 0.9f * (1f - sstep(0.3f, 0.9f, p))));
        }
        // bubbles rising out of the lingering puddle
        for (int k = 0; k < 4; k++) {
            float a = rng.nextFloat() * Mth.TWO_PI, rr = rng.nextFloat() * pw * 1.2f;
            float ph = c01((p - 0.25f - 0.08f * k) / 0.6f);
            Vector3f bp = new Vector3f(c).add(Mth.cos(a) * rr, 0.1f + ph * pw * 1.1f, Mth.sin(a) * rr);
            buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, bp, pw * 0.25f * (0.6f + 0.4f * ph), 0f, al(pal.hi, 0.85f * Math.min(1f, ph * 6f) * (1f - sstep(0.7f, 1f, ph))));
        }
    }
}
