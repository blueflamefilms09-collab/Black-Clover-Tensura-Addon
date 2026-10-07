package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 0.55 Chain Magic, after the owner's reference frame (pack_chain): long bright steel chains, every link catching a highlight, whipping
 * and coiling round a white burst of light with grey smoke in it; each chain ends in a flat grey-blue kunai blade. Textures from
 * tools/gen_chain_textures.py (chain_strip, chain_band, chain_blade, chain_flash, chain_smoke, chain_sigil, chain_seal, chain_cuff,
 * chain_shard, chain_spark, chain_shock). The chains are real ribbons of linked steel (the strip texture repeats every four links, so the
 * links keep their proportions at any length), the light is additive, the smoke and steel are solid ALPHA layers.
 * <ul>
 *   <li>CHAIN_FX1 (cast / projectile): at 'from' a white charge flash gathers with sparks drawn inward; then three (four above power 1.6)
 *       chains are thrown towards 'to', braided round the flight line with a twist that runs along them, tails still anchored at 'from',
 *       each ending in a kunai blade, over a soft additive streak and trailing sparks. At 'to' the strands close in, a flash and shock ring
 *       burst, a steel manacle snaps shut round the point and a puff of grey smoke rises. power = thickness and size scale
 *       (0.6 to 3.2), duration = flight ticks (default 16), color = tint mixed 20 percent into the steel and 30 percent into the glow.</li>
 *   <li>CHAIN_FX2 (zone / field / dome): 'from' = centre on the ground, power = radius. A dark underlay and the glowing chain sigil
 *       (links round the rim, hexagram of tiny links) turn slowly on the ground, an eight-bladed seal counter-rotates inside it, a real
 *       chain band crawls round the rim, shock pulses ring outward every 26 ticks, and six (seven above radius 5) chains grow out of the
 *       rim and arch inward as a cage, twisting, each ending in a hanging blade pointing at the middle. Glints rise inside, smoke gathers
 *       low at the rim. Fades in over 8 ticks and out over the last 12.</li>
 *   <li>CHAIN_FX3 (impact / burst / signature): the reference burst. A white flash with grey smoke billows at 'from'; six to eight chains
 *       lash out in all directions (biased towards 'to - from' when given), wobbling, each with a blade on its end, then snap back and
 *       fade; two steel manacles slam shut on the centre; link shards and sparks fly out, a shock ring runs along the ground and across
 *       the view, an afterglow lingers. power = scale (chain reach about 3.2 blocks at 1.0), duration = life (default 28).</li>
 * </ul>
 */
public class ChainLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation STRIP = t("chain_strip"), BAND = t("chain_band"), BLADE = t("chain_blade"), FLASH = t("chain_flash"),
            SMOKE = t("chain_smoke"), SIGIL = t("chain_sigil"), SEAL = t("chain_seal"), CUFF = t("chain_cuff"), SHARD = t("chain_shard"),
            SPARK = t("chain_spark"), SHOCK = t("chain_shock");

    /** Bright steel, the blue-white light round it, the dark underlay, the smoke grey. */
    static final int STEEL = 0xFFE6ECFA, GLOW = 0xFFB4C8FF, DARK = 0xFF30323C, SMOKE_GREY = 0xFFB4BAC8;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CHAIN_FX1, VfxShape.CHAIN_FX2, VfxShape.CHAIN_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case CHAIN_FX1 -> 16;
            case CHAIN_FX2 -> 80;
            case CHAIN_FX3 -> 28;
            default -> 30;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFC0C8E0; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.CHAIN_FX3) VfxShake.add(inst.payload.from(), 0.25f * Mth.clamp(inst.power, 0.6f, 2.5f), 6);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case CHAIN_FX1 -> cast(inst, ctx, buf);
            case CHAIN_FX2 -> zone(inst, ctx, buf);
            case CHAIN_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small helpers

    private static int steelCol(VfxInstance i) { return VfxVertexBuffer.lerpColor(STEEL, i.color | 0xFF000000, 0.2f); }
    private static int glowCol(VfxInstance i) { return VfxVertexBuffer.lerpColor(GLOW, i.color | 0xFF000000, 0.3f); }

    private static float ss(float a, float b, float x) {
        float u = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return u * u * (3 - 2 * u);
    }

    /** Deterministic 0..1 hash of (seed, i). */
    private static float h(long seed, int i) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L;
        x ^= x >>> 30; x *= 0x94D049BB133111EBL; x ^= x >>> 27; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 31;
        return (x >>> 40) / 16777216f;
    }

    private static Vector3f sideOf(VfxRenderContext ctx, Vector3f dir, Vector3f at, float len) {
        Vector3f s = new Vector3f(dir).cross(new Vector3f(at).negate());
        if (s.lengthSquared() < 1e-8f) s.set(ctx.camRight); else s.normalize();
        return s.mul(len);
    }

    /**
     * A chain of n segments through the n + 1 camera-relative points, as one continuous ribbon of steel links (width w; the links
     * keep their proportions because V runs 1 per 4 widths). alpha runs aStart (first point) to aEnd (last point).
     */
    static void strand(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f[] p, int n, float w, int col, float aStart, float aEnd) {
        Vector3f[] side = new Vector3f[n + 1];
        for (int i = 0; i <= n; i++) {
            Vector3f tan = new Vector3f(p[Math.min(n, i + 1)]).sub(p[Math.max(0, i - 1)]);
            side[i] = sideOf(ctx, tan, p[i], w * 0.5f);
        }
        float v = 0f;
        for (int i = 0; i < n; i++) {
            if (!buf.hasBudget(4)) return;
            float seg = p[i].distance(p[i + 1]);
            float vb = v + seg / (4f * w);
            int c0 = VfxVertexBuffer.withAlpha(col, Mth.lerp((float) i / n, aStart, aEnd));
            int c1 = VfxVertexBuffer.withAlpha(col, Mth.lerp((float) (i + 1) / n, aStart, aEnd));
            buf.quad(STRIP, VfxBlend.ALPHA, new Vector3f(p[i]).sub(side[i]), new Vector3f(p[i]).add(side[i]),
                    new Vector3f(p[i + 1]).add(side[i + 1]), new Vector3f(p[i + 1]).sub(side[i + 1]), 0f, vb, 1f, v, c0, c1);
            v = vb;
        }
    }

    /** A kunai blade whose eyelet sits at 'eye' and whose tip points along dir. */
    static void blade(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f eye, Vector3f dir, float len, int col, float alpha) {
        if (!buf.hasBudget(4) || alpha <= 0.01f) return;
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() < 1e-8f) d.set(0, 1, 0);
        d.normalize();
        Vector3f base = new Vector3f(eye).sub(new Vector3f(d).mul(len * 0.164f));
        Vector3f tip = new Vector3f(base).add(new Vector3f(d).mul(len));
        Vector3f side = sideOf(ctx, d, eye, len * 0.25f);
        int c = VfxVertexBuffer.withAlpha(col, alpha);
        buf.quad(BLADE, VfxBlend.ALPHA, new Vector3f(base).sub(side), new Vector3f(base).add(side), new Vector3f(tip).add(side),
                new Vector3f(tip).sub(side), 0f, 0f, 1f, 1f, c, c);
    }

    private static void spark(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, float size, float rot, int col, float alpha) {
        if (!buf.hasBudget(4) || alpha <= 0.01f) return;
        buf.billboard(ctx, SPARK, VfxBlend.ADD, at, size, rot, VfxVertexBuffer.withAlpha(col, alpha));
    }

    private static void glow(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, float size, int col, float alpha) {
        if (!buf.hasBudget(4) || alpha <= 0.01f) return;
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, at, size, 0f, VfxVertexBuffer.withAlpha(col, alpha));
    }

    private static void flash(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, float size, float rot, int col, float alpha) {
        if (!buf.hasBudget(4) || alpha <= 0.01f) return;
        buf.billboard(ctx, FLASH, VfxBlend.ADD, at, size, rot, VfxVertexBuffer.withAlpha(col, alpha));
    }

    private static void smoke(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, float size, float rot, int col, float alpha) {
        if (!buf.hasBudget(4) || alpha <= 0.01f) return;
        buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, at, size, rot, VfxVertexBuffer.withAlpha(col, alpha));
    }

    private static Vector3f onScreen(VfxRenderContext ctx, Vector3f at, float ang, float r) {
        return new Vector3f(at).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * r)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * r));
    }

    // ------------------------------------------------------------------ FX1: cast / projectile

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float p = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float pw = Mth.clamp(inst.power, 0.6f, 3.2f);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f axis = new Vector3f(b).sub(a);
        float len = axis.length();
        if (len < 0.4f) { axis.set(0, 0, 1); len = 3f; } else axis.div(len);
        b = new Vector3f(a).add(new Vector3f(axis).mul(len));
        Vector3f p1 = new Vector3f(axis).cross(new Vector3f(0, 1, 0));
        if (p1.lengthSquared() < 1e-4f) p1.set(1, 0, 0);
        p1.normalize();
        Vector3f p2 = new Vector3f(axis).cross(p1).normalize();
        int steel = steelCol(inst), gl = glowCol(inst);

        float charge = Mth.clamp(p / 0.22f, 0f, 1f);
        float q = Mth.clamp((p - 0.14f) / 0.58f, 0f, 1f);
        float e = 1f - (float) Math.pow(1f - q, 2.2f);
        float head = len * e;
        float alpha = ss(0.05f, 0.18f, p) * Mth.clamp((1f - p) / 0.22f, 0f, 1f);

        // 1. charge: a white flash gathering at the caster, sparks drawn into it
        float ca = (charge < 1f ? charge : 1f - ss(0.22f, 0.6f, p));
        flash(ctx, buf, a, pw * (0.7f + 1.5f * VfxAnim.easeOutCubic(charge)), age * 0.1f, gl, 0.9f * ca);
        glow(ctx, buf, a, pw * (0.9f + 0.9f * charge), gl, 0.55f * ca);
        if (charge < 1f) {
            for (int k = 0; k < 4; k++) {
                Vector3f sp = onScreen(ctx, a, k * Mth.HALF_PI + 0.6f + age * 0.15f, pw * 1.1f * (1f - charge));
                spark(ctx, buf, sp, 0.34f * pw, k * 0.7f, STEEL, charge);
            }
        }

        // 2. additive streak behind the heads (the afterimage)
        Vector3f headPt = new Vector3f(a).add(new Vector3f(axis).mul(head));
        if (head > 0.1f && buf.hasBudget(16)) {
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, headPt, 0.12f * pw, 0.5f * pw, 4, 0f,
                    VfxVertexBuffer.withAlpha(gl, 0.2f * alpha), VfxVertexBuffer.withAlpha(gl, 0.75f * alpha));
        }

        // 3. the braided chains with their blades
        int strands = pw > 1.6f ? 4 : 3;
        int n = ctx.seg(8, 5);
        float w = 0.17f + 0.07f * pw;
        float amp = pw * 0.34f * (1f - 0.8f * ss(0.8f, 1.0f, q));
        for (int s = 0; s < strands; s++) {
            Vector3f[] pts = new Vector3f[n + 1];
            float phi = Mth.TWO_PI * s / strands + inst.seed % 7;
            for (int j = 0; j <= n; j++) {
                float u = (float) j / n;
                float aa = amp * (0.45f * (1f - u) + 0.9f * Mth.sin(Mth.PI * u));
                float th = phi - u * 5.2f + age * 0.42f;
                pts[j] = new Vector3f(a).add(new Vector3f(axis).mul(head * (1f - u)))
                        .add(new Vector3f(p1).mul(Mth.cos(th) * aa)).add(new Vector3f(p2).mul(Mth.sin(th) * aa));
            }
            strand(ctx, buf, pts, n, w, steel, alpha * alpha, alpha);
            Vector3f tan = new Vector3f(pts[0]).sub(pts[1]);
            blade(ctx, buf, pts[0], tan.lengthSquared() < 1e-8f ? axis : tan, 0.95f * pw, steel, alpha);
        }
        glow(ctx, buf, headPt, 0.9f + 0.5f * pw, gl, 0.7f * alpha);

        // 4. sparks shed along the flight line
        for (int k = 0; k < 6; k++) {
            float u = 0.1f + 0.8f * h(inst.seed, k);
            Vector3f sp = new Vector3f(a).add(new Vector3f(axis).mul(head * (1f - u)))
                    .add(new Vector3f(p1).mul((h(inst.seed, k + 9) - 0.5f) * 0.8f * pw)).add(new Vector3f(p2).mul((h(inst.seed, k + 19) - 0.5f) * 0.8f * pw));
            spark(ctx, buf, sp, 0.28f * pw * (1f - 0.5f * u), h(inst.seed, k + 29) * 6f + age * 0.1f, STEEL, 0.8f * (1f - u) * alpha);
        }

        // 5. impact at the target: flash, shock ring, the manacle snapping shut, smoke, outward sparks
        float on = ss(0.66f, 0.74f, p), ip = Mth.clamp((p - 0.72f) / 0.28f, 0f, 1f);
        if (on > 0f) {
            float eo = VfxAnim.easeOutCubic(ip);
            float fade = on * (1f - ip);
            smoke(ctx, buf, new Vector3f(b).add(0f, 0.25f * pw * ip, 0f), 1.4f * pw * (0.4f + 0.9f * ip), age * 0.03f, SMOKE_GREY, 0.5f * on * (1f - ip));
            buf.billboard(ctx, CUFF, VfxBlend.ALPHA, b, pw * (2.0f - 0.9f * VfxAnim.easeOutCubic(Math.min(1f, ip * 2.5f))), age * 0.15f,
                    VfxVertexBuffer.withAlpha(steel, on * (1f - ip * ip)));
            flash(ctx, buf, b, 2.6f * pw * (0.5f + 0.5f * eo), age * 0.05f, gl, Math.min(1f, fade * 1.3f));
            if (buf.hasBudget(4)) buf.billboard(ctx, SHOCK, VfxBlend.ADD, b, 3.2f * pw * eo + 0.3f, 0f, VfxVertexBuffer.withAlpha(gl, 0.8f * fade));
            for (int k = 0; k < 4; k++) {
                Vector3f sp = onScreen(ctx, b, k * Mth.HALF_PI + 0.5f + h(inst.seed, k + 40) * 0.6f, pw * 1.6f * eo);
                spark(ctx, buf, sp, 0.4f * pw * (1f - ip), k * 1.3f, STEEL, fade);
            }
        }
    }

    // ------------------------------------------------------------------ FX2: zone / field / dome

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1f, inst.power);
        float fade = Math.min(ss(0f, 8f, age), ss(0f, 12f, inst.duration - age));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int steel = steelCol(inst), gl = glowCol(inst);
        VfxPose ground = VfxPose.ground(c);

        // ground: dark underlay, the glowing sigil, the counter-rotating blade seal
        buf.plane(SIGIL, VfxBlend.ALPHA, ground.lift(0.02f).spin(age * 0.012f), R * 1.04f, VfxVertexBuffer.withAlpha(DARK, 0.6f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.035f).spin(age * 0.012f), R, VfxVertexBuffer.withAlpha(gl, 0.9f * fade));
        buf.plane(SEAL, VfxBlend.ADD, ground.lift(0.05f).spin(-age * 0.03f), R * 0.62f, VfxVertexBuffer.withAlpha(gl, 0.85f * fade));

        // a real chain band round the rim, crawling
        float wd = 0.2f + 0.04f * R;
        int seg = ctx.seg(24, 12);
        float rr = R * 0.955f;
        float reps = Math.max(3f, Math.round(Mth.TWO_PI * rr / (4f * wd)));
        if (buf.hasBudget(seg * 4)) {
            buf.ring(BAND, VfxBlend.ALPHA, ground.lift(0.07f), rr - wd * 0.5f, rr + wd * 0.5f, seg, reps, age * 0.004f, VfxVertexBuffer.withAlpha(steel, fade));
        }

        // shock pulses running out over the ground
        for (int k = 0; k < 2; k++) {
            float ph = (((age - k * 13f) % 26f) + 26f) % 26f / 26f;
            float eo = VfxAnim.easeOutCubic(ph);
            if (buf.hasBudget(4)) {
                buf.plane(SHOCK, VfxBlend.ADD, ground.lift(0.09f), R * (0.25f + 0.78f * eo), VfxVertexBuffer.withAlpha(gl, 0.55f * fade * (1f - ph)));
            }
        }

        // the cage: chains growing out of the rim, arching inward, twisting, each ending in a hanging blade
        int cages = R > 5f ? 7 : 6;
        float H = Math.min(0.95f * R + 0.6f, 9f);
        float wc = 0.17f + 0.07f * (float) Math.sqrt(R);
        int n = ctx.seg(6, 4);
        for (int k = 0; k < cages; k++) {
            float g = VfxAnim.easeOutCubic(Mth.clamp((age - 3f - k * 0.7f) / 22f, 0f, 1f));
            if (g <= 0.02f) continue;
            Vector3f[] pts = new Vector3f[n + 1];
            float phi = Mth.TWO_PI * k / cages + age * 0.006f + 0.4f;
            for (int j = 0; j <= n; j++) {
                float wv = (float) j / n * g;
                float rho = R * (1f - 0.78f * (float) Math.pow(wv, 1.3f));
                float ang = phi + wv * 1.1f;
                float y = H * (float) Math.pow(Mth.sin(wv * Mth.HALF_PI), 0.8f) + 0.07f * Mth.sin(age * 0.15f + k + wv * 6f);
                pts[j] = new Vector3f(c).add(rho * Mth.cos(ang), y, rho * Mth.sin(ang));
            }
            strand(ctx, buf, pts, n, wc, steel, fade, fade);
            Vector3f tan = new Vector3f(pts[n]).sub(pts[n - 1]);
            blade(ctx, buf, pts[n], tan, 0.5f + 0.05f * R, steel, fade * ss(0.3f, 0.9f, g));
        }

        // rising glints inside the field
        for (int k = 0; k < 8; k++) {
            float rad = R * (0.15f + 0.8f * h(inst.seed, k));
            float ang = h(inst.seed, k + 11) * Mth.TWO_PI + age * 0.01f;
            float life = ((age * 0.025f * (0.6f + h(inst.seed, k + 21)) + h(inst.seed, k + 31)) % 1f);
            Vector3f sp = new Vector3f(c).add(rad * Mth.cos(ang), 0.2f + life * H * 0.8f, rad * Mth.sin(ang));
            spark(ctx, buf, sp, 0.3f + 0.04f * R, h(inst.seed, k + 41) * 6f, STEEL, fade * Mth.sin(life * Mth.PI) * 0.9f);
        }

        // smoke low at the rim, a heart-glow in the middle
        for (int k = 0; k < 4; k++) {
            float ang = Mth.TWO_PI * k / 4f + h(inst.seed, k + 51) + age * 0.004f;
            smoke(ctx, buf, new Vector3f(c).add(R * 0.8f * Mth.cos(ang), 0.3f + 0.05f * R, R * 0.8f * Mth.sin(ang)), 0.9f + 0.1f * R, age * 0.01f + k,
                    SMOKE_GREY, 0.3f * fade);
        }
        float pulse = 0.75f + 0.25f * VfxAnim.pulse(age, 1.2f);
        glow(ctx, buf, new Vector3f(c).add(0f, 0.15f, 0f), (1.2f + 0.15f * R) * pulse, gl, 0.6f * fade);
    }

    // ------------------------------------------------------------------ FX3: impact / burst / signature

    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float p = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float pw = Mth.clamp(inst.power, 0.5f, 3.5f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        boolean hinted = hint.lengthSquared() > 0.04f;
        if (hinted) hint.normalize();
        int steel = steelCol(inst), gl = glowCol(inst);
        float eo = VfxAnim.easeOutCubic(Mth.clamp(p * 3f, 0f, 1f));
        float life = 1f - p;

        // 1. grey smoke billowing behind the light
        for (int k = 0; k < 4; k++) {
            float ang = h(inst.seed, k) * Mth.TWO_PI;
            float rad = 0.5f * pw * h(inst.seed, k + 5) * (0.3f + p);
            Vector3f at = new Vector3f(c).add(rad * Mth.cos(ang), (h(inst.seed, k + 9) - 0.3f) * 0.6f * pw * (0.3f + p), rad * Mth.sin(ang));
            smoke(ctx, buf, at, 1.8f * pw * (0.5f + 0.8f * eo), age * 0.02f * (k % 2 == 0 ? 1 : -1) + k, SMOKE_GREY, 0.55f * (float) Math.pow(life, 1.5f));
        }

        // 2. the manacles: two steel rings slam shut on the centre
        for (int k = 0; k < 2; k++) {
            float close = VfxAnim.easeOutCubic(Mth.clamp(p * 2.2f, 0f, 1f));
            float size = pw * (k == 0 ? 3.0f : 2.0f) * (1.3f - 0.8f * close);
            float al = ss(0f, 0.08f, p) * (1f - ss(0.55f, 0.95f, p));
            if (buf.hasBudget(4)) buf.billboard(ctx, CUFF, VfxBlend.ALPHA, c, size, age * (k == 0 ? 0.05f : -0.08f) + k * 1.2f, VfxVertexBuffer.withAlpha(steel, al));
        }

        // 3. the lashing chains, each with a blade
        int strands = pw > 2f ? 8 : pw < 0.9f ? 6 : 7;
        int n = ctx.seg(6, 4);
        float w = 0.12f + 0.05f * pw;
        float ext = VfxAnim.easeOutCubic(Mth.clamp(p / 0.38f, 0f, 1f)) * (1f - 0.7f * ss(0.62f, 0.95f, p));
        float alpha = Mth.clamp(life / 0.25f, 0f, 1f);
        for (int s = 0; s < strands; s++) {
            float y = 0.8f - 1.6f * (s + 0.5f) / strands;
            float rad = Mth.sqrt(Math.max(0f, 1f - y * y));
            float th = s * 2.399963f + h(inst.seed, s) * 0.5f;
            Vector3f d = new Vector3f(rad * Mth.cos(th), y, rad * Mth.sin(th));
            if (hinted) d.add(new Vector3f(hint).mul(0.9f)).normalize();
            Vector3f q1 = new Vector3f(d).cross(new Vector3f(0, 1, 0));
            if (q1.lengthSquared() < 1e-4f) q1.set(1, 0, 0);
            q1.normalize();
            Vector3f q2 = new Vector3f(d).cross(q1).normalize();
            float reach = 3.2f * pw * (0.8f + 0.4f * h(inst.seed, s + 20));
            float ph = h(inst.seed, s + 30) * Mth.TWO_PI;
            Vector3f[] pts = new Vector3f[n + 1];
            for (int j = 0; j <= n; j++) {
                float u = (float) j / n;
                float wob = 0.35f * pw * u * (1f - 0.35f * ext);
                pts[j] = new Vector3f(c).add(new Vector3f(d).mul(reach * ext * u))
                        .add(new Vector3f(q1).mul(Mth.sin(u * 5f + age * 0.28f + ph) * wob))
                        .add(new Vector3f(q2).mul(Mth.cos(u * 4f + age * 0.23f + ph) * wob * 0.85f))
                        .add(0f, -0.25f * pw * u * u * p * 2f, 0f);
            }
            strand(ctx, buf, pts, n, w, steel, alpha, alpha);
            Vector3f tan = new Vector3f(pts[n]).sub(pts[n - 1]);
            blade(ctx, buf, pts[n], tan.lengthSquared() < 1e-8f ? d : tan, 0.7f * pw, steel, alpha * ss(0.02f, 0.15f, p));
        }

        // 4. the white burst with smoke in it, ringed by shock waves
        float fa = Mth.clamp(1f - p * 1.15f, 0f, 1f);
        flash(ctx, buf, c, 4.2f * pw * (0.3f + 0.7f * eo), age * 0.02f, gl, fa);
        flash(ctx, buf, c, 2.4f * pw * (0.3f + 0.7f * eo), -age * 0.05f, VfxVertexBuffer.whiten(gl, 0.6f), 0.8f * fa);
        float re = VfxAnim.easeOutCubic(Mth.clamp(p * 1.2f, 0f, 1f));
        if (buf.hasBudget(4)) buf.plane(SHOCK, VfxBlend.ADD, VfxPose.ground(c).lift(0.02f), 2.5f * pw * re + 0.2f, VfxVertexBuffer.withAlpha(gl, 0.8f * Math.max(0f, 1f - p * 1.1f)));
        if (buf.hasBudget(4)) buf.billboard(ctx, SHOCK, VfxBlend.ADD, c, 3.6f * pw * re + 0.2f, age * 0.03f, VfxVertexBuffer.withAlpha(gl, 0.7f * Math.max(0f, 1f - p * 1.4f)));

        // 5. link shards and sparks thrown out
        for (int k = 0; k < 9; k++) {
            float ang = h(inst.seed, k + 60) * Mth.TWO_PI, el = (h(inst.seed, k + 70) - 0.35f) * 1.6f;
            Vector3f d = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el), Mth.sin(ang) * Mth.cos(el));
            if (hinted) d.add(new Vector3f(hint).mul(0.5f)).normalize();
            float sp = (1.6f + 2.6f * h(inst.seed, k + 80)) * pw;
            float te = VfxAnim.easeOutCubic(Mth.clamp(p * 1.5f, 0f, 1f));
            Vector3f at = new Vector3f(c).add(new Vector3f(d).mul(sp * te)).add(0f, -0.9f * pw * p * p, 0f);
            if (buf.hasBudget(4)) {
                buf.billboard(ctx, SHARD, VfxBlend.ALPHA, at, (0.22f + 0.2f * h(inst.seed, k + 90)) * pw + 0.08f,
                        age * (0.2f + h(inst.seed, k + 100) * 0.3f) * (k % 2 == 0 ? 1 : -1), VfxVertexBuffer.withAlpha(steel, (float) Math.pow(life, 0.6f)));
            }
        }
        for (int k = 0; k < 8; k++) {
            float ang = h(inst.seed, k + 110) * Mth.TWO_PI;
            float sp = (2.4f + 3f * h(inst.seed, k + 120)) * pw * VfxAnim.easeOutCubic(Mth.clamp(p * 1.8f, 0f, 1f));
            Vector3f at = onScreen(ctx, c, ang, sp);
            spark(ctx, buf, at, 0.36f * pw * life + 0.05f, ang * 2f, STEEL, Math.max(0f, 1f - p * 1.3f));
        }

        // 6. afterglow
        glow(ctx, buf, c, 2.4f * pw, gl, 0.4f * life * life);
    }
}
