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
 * Curse-Warding Magic: pale-violet warding runes, silver seals, braided rune threads and seals that snap. The counter to curses:
 * everything is built from angular runes and ward seals, over a dark ink under-layer so the light reads against a bright sky.
 * <ul>
 *   <li>CURSE_WARDING_FX1 = WARD LANCE (cast / projectile). 'from' = hand, 'to' = target, power = size (0.6 to 3), duration = flight ticks (16).
 *       First quarter: a broken seal ring contracts onto 'from' while a ward star ignites and two runes orbit it. Then a slender
 *       silver-violet lance (ink outline, bright spine) flies to 'to', wrapped by a double helix of rune thread, a braided strand
 *       trailing behind it, three thin echoes beside it and runes shed along its path. It ends in a small flash with a snapped ring
 *       and four shards at 'to'. Silhouette: a needle with a coil, always moving along one line.</li>
 *   <li>CURSE_WARDING_FX2 = WARD FIELD (zone / dome). 'from' = ground centre, power = RADIUS, duration = life ticks (80). Fades in over
 *       8 ticks, out over the last 12. An ink shadow and a rotating ward seal on the ground, a counter-rotating hex lattice hung overhead
 *       as the dome ceiling, two scrolling rune walls at the rim (opposite directions), eight rune-thread curtains rising from the rim,
 *       runes and motes drifting up, a floating ward star, and rings that pulse outward from the centre.</li>
 *   <li>CURSE_WARDING_FX3 = CURSE BREAK (impact / burst / signature). 'from' = centre, 'to' = optional direction hint (shards and
 *       streaks lean that way), power = scale, duration = life ticks (28). A dark ink halo and a hot flash, a star and a burst
 *       counter-rotating, two broken seal rings expanding with a ground seal and a tilted seal (gyroscope), silver shards and runes
 *       flying out, lance streaks, then a lingering violet glow with rising motes.</li>
 * </ul>
 * 'color' is a tint mixed 40 % into the palette (violet / silver / ink); with a white tint the effect keeps its pale-violet identity.
 * Each shape draws at most about 330 vertices.
 */
public class CurseWardingLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName("curse_warding_" + n); }

    public static final ResourceLocation SIGIL = t("sigil");
    public static final ResourceLocation BAND = t("rune_band");
    public static final ResourceLocation STRAND = t("strand");
    public static final ResourceLocation LANCE = t("lance");
    public static final ResourceLocation STAR = t("star");
    public static final ResourceLocation RING = t("broken_ring");
    public static final ResourceLocation SHARD = t("shard");
    public static final ResourceLocation HEX = t("hex");
    public static final ResourceLocation BURST = t("burst");
    public static final ResourceLocation RUNE_A = t("rune_a");
    public static final ResourceLocation RUNE_B = t("rune_b");
    public static final ResourceLocation MOTE = t("mote");

    private static final int VIOLET = 0xFFC08AFF;
    private static final int SILVER = 0xFFDDE6F5;
    private static final int INK = 0xFF1C0F38;
    private static final float BAND_ASPECT = 16f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CURSE_WARDING_FX1, VfxShape.CURSE_WARDING_FX2, VfxShape.CURSE_WARDING_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case CURSE_WARDING_FX1 -> 16;
            case CURSE_WARDING_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return VIOLET; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case CURSE_WARDING_FX1 -> cast(inst, ctx, buf);
            case CURSE_WARDING_FX2 -> field(inst, ctx, buf);
            case CURSE_WARDING_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small helpers
    private static float clamp01(float v) { return Mth.clamp(v, 0f, 1f); }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : clamp01(age / in);
        float b = out <= 0 ? 1 : clamp01((inst.duration - age) / out);
        return Math.min(a, b);
    }

    private static int tint(VfxInstance inst) { return VfxVertexBuffer.lerpColor(VIOLET, inst.color | 0xFF000000, 0.4f); }

    /** Point at screen-plane offset (radius, angle) around c. */
    private static Vector3f around(VfxRenderContext ctx, Vector3f c, float radius, float ang) {
        return new Vector3f(ctx.camRight).mul(Mth.cos(ang) * radius).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * radius)).add(c);
    }

    private static ResourceLocation rune(int i) { return (i & 1) == 0 ? RUNE_A : RUNE_B; }

    private static Vector3f randUnit(RandomSource r) {
        float u = r.nextFloat() * 2 - 1, phi = r.nextFloat() * Mth.TWO_PI, s = Mth.sqrt(Math.max(0f, 1 - u * u));
        return new Vector3f(s * Mth.cos(phi), u, s * Mth.sin(phi));
    }

    private static void sparkle(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p, float size, float rot, int col, float a) {
        if (a <= 0.02f) return;
        buf.billboard(ctx, MOTE, VfxBlend.ADD, p, size, rot, VfxVertexBuffer.withAlpha(col, a));
    }

    // ================================================================== FX1: Ward Lance
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float p = clamp01(age / Math.max(4f, inst.duration));
        float pw = Mth.clamp(inst.power, 0.4f, 3.5f);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float total = dir.length();
        if (total < 0.05f) { dir.set(0, 0, 1); total = 0.05f; } else dir.div(total);
        int col = tint(inst), pale = VfxVertexBuffer.whiten(col, 0.65f);
        RandomSource r = inst.random();

        float charge = clamp01(p / 0.25f), chargeFade = 1f - clamp01((p - 0.22f) / 0.14f);
        float fly = clamp01((p - 0.2f) / 0.58f), hit = clamp01((p - 0.78f) / 0.22f);
        float ease = fly * fly * (1.4f - 0.4f * fly);              // accelerates into the target
        float lanceA = (1f - hit) * clamp01(fly * 8f);

        // -- charge: a broken ring contracting onto the hand, a star igniting, two runes in orbit
        if (chargeFade > 0.02f) {
            float k = VfxAnim.easeOutCubic(charge);
            buf.billboard(ctx, RING, VfxBlend.ADD, a, pw * (1.7f - 1.15f * k), age * 0.32f, VfxVertexBuffer.withAlpha(col, 0.9f * chargeFade * (0.3f + 0.7f * charge)));
            buf.billboard(ctx, RING, VfxBlend.ADD, a, pw * (1.0f - 0.45f * k), -age * 0.45f, VfxVertexBuffer.withAlpha(pale, 0.7f * chargeFade));
            VfxBloom.glow(ctx, buf, a, pw * 0.3f * (0.4f + k), col, (0.5f + 0.9f * charge) * chargeFade);
            buf.billboard(ctx, STAR, VfxBlend.ADD, a, pw * (0.3f + 1.0f * VfxAnim.easeOutBack(charge)), -age * 0.12f, VfxVertexBuffer.withAlpha(pale, chargeFade));
            for (int i = 0; i < 2; i++) {
                Vector3f q = around(ctx, a, pw * 0.62f * (1f - 0.5f * k), age * 0.5f + i * Mth.PI);
                buf.billboard(ctx, rune(i), VfxBlend.ADD, q, pw * 0.34f, age * 0.5f + i, VfxVertexBuffer.withAlpha(col, 0.9f * chargeFade * charge));
            }
        }

        // -- flight geometry
        float travelled = total * ease;
        Vector3f head = new Vector3f(dir).mul(travelled).add(a);
        float lanceLen = Math.min(pw * 2.1f, travelled + pw * 0.3f);
        Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(lanceLen));
        float trailLen = Math.min(travelled, pw * 5f);
        Vector3f trailStart = new Vector3f(tail).sub(new Vector3f(dir).mul(trailLen));

        if (lanceA > 0.02f) {
            // ink outline first (so the lance reads against the sky), then the braided trail
            buf.beam(ctx, LANCE, VfxBlend.ALPHA, tail, head, pw * 0.85f, pw * 0.85f, 1, 0,
                    VfxVertexBuffer.withAlpha(INK, 0.55f * lanceA), VfxVertexBuffer.withAlpha(INK, 0.6f * lanceA));
            if (trailLen > 0.1f)
                buf.beam(ctx, STRAND, VfxBlend.ADD, trailStart, tail, pw * 0.1f, pw * 0.6f, 2, -age * 0.12f,
                        VfxVertexBuffer.withAlpha(col, 0f), VfxVertexBuffer.withAlpha(col, 0.9f * lanceA));

            // double helix of rune thread winding around the lance and its trail
            Vector3f u = new Vector3f(dir).cross(Math.abs(dir.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0)).normalize();
            Vector3f v = new Vector3f(dir).cross(u).normalize();
            float helixLen = Math.min(travelled + lanceLen, pw * 3.6f);
            int n = ctx.seg(8, 5);
            for (int strand = 0; strand < 2; strand++) {
                Vector3f prev = null;
                for (int i = 0; i <= n; i++) {
                    float s = (float) i / n;                                 // 0 at the head, 1 at the far end of the helix
                    float ph = s * 6.5f + age * 0.55f + strand * Mth.PI;
                    float rad = pw * 0.24f * (0.5f + 0.5f * Mth.sin(Mth.PI * Math.min(1f, s * 1.4f))) * (1f - 0.55f * s);
                    Vector3f q = new Vector3f(dir).mul(-helixLen * s).add(head)
                            .add(new Vector3f(u).mul(Mth.cos(ph) * rad)).add(new Vector3f(v).mul(Mth.sin(ph) * rad));
                    if (prev != null) {
                        float fa = (1f - s) * lanceA;
                        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, prev, q, pw * 0.1f, pw * 0.1f, 1, 0,
                                VfxVertexBuffer.withAlpha(strand == 0 ? pale : col, 0.95f * fa), VfxVertexBuffer.withAlpha(strand == 0 ? pale : col, 0.7f * fa));
                    }
                    prev = q;
                }
            }

            // echoes: three thin afterimages of the lance drifting beside it
            for (int e = 1; e <= 3; e++) {
                float ee = clamp01(fly - 0.07f * e), eEase = ee * ee * (1.4f - 0.4f * ee);
                Vector3f eh = new Vector3f(dir).mul(total * eEase).add(a)
                        .add(new Vector3f(u).mul(Mth.cos(age * 0.3f + e * 2.1f) * pw * 0.2f)).add(new Vector3f(v).mul(Mth.sin(age * 0.3f + e * 2.1f) * pw * 0.2f));
                Vector3f et = new Vector3f(eh).sub(new Vector3f(dir).mul(Math.min(lanceLen * 0.8f, total * eEase)));
                buf.beam(ctx, LANCE, VfxBlend.ADD, et, eh, pw * 0.2f, pw * 0.2f, 1, 0,
                        VfxVertexBuffer.withAlpha(col, 0.35f * lanceA / e), VfxVertexBuffer.withAlpha(pale, 0.45f * lanceA / e));
            }

            // runes shed along the path, each drifting off its spot
            for (int i = 0; i < 4; i++) {
                float at = 0.12f + 0.2f * i + r.nextFloat() * 0.08f;
                float sx = (r.nextFloat() - 0.5f) * 0.6f, sy = r.nextFloat() - 0.5f;
                float born = at * 0.58f;                               // fly value at which the head passes this spot
                float age2 = clamp01((fly - born) / 0.4f);
                if (fly < born) continue;
                Vector3f q = new Vector3f(dir).mul(total * at * 0.9f).add(a).add(new Vector3f(u).mul(sx * pw * (0.3f + age2))).add(new Vector3f(v).mul((sy * 0.4f + 0.15f * age2) * pw));
                float al = Mth.sin(Mth.PI * Math.min(1f, age2 * 1.1f)) * (1f - hit);
                buf.billboard(ctx, rune(i), VfxBlend.ADD, q, pw * 0.34f, i * 1.7f + age2, VfxVertexBuffer.withAlpha(col, 0.85f * al));
            }
            for (int i = 0; i < 5; i++) {                              // motes around the trail
                float s = r.nextFloat(), ph = r.nextFloat() * Mth.TWO_PI;
                Vector3f q = new Vector3f(dir).mul(-s * Math.min(travelled + lanceLen, pw * 4f)).add(head)
                        .add(new Vector3f(u).mul(Mth.cos(ph + age * 0.2f) * pw * 0.3f)).add(new Vector3f(v).mul(Mth.sin(ph + age * 0.2f) * pw * 0.3f));
                sparkle(ctx, buf, q, pw * 0.2f, age * 0.1f + i, i % 2 == 0 ? SILVER : pale, (1f - s) * lanceA * (0.5f + 0.5f * Mth.sin(age * 0.9f + i * 2f)));
            }

            // head: bright core with coloured halo and a spinning ward star
            VfxBloom.glow(ctx, buf, head, pw * 0.34f, col, 0.85f * lanceA);
            buf.beam(ctx, LANCE, VfxBlend.ADD, tail, head, pw * 0.58f, pw * 0.58f, 1, 0,
                    VfxVertexBuffer.withAlpha(col, 0.75f * lanceA), VfxVertexBuffer.withAlpha(0xFFFFFFFF, lanceA));
            buf.billboard(ctx, STAR, VfxBlend.ADD, head, pw * 0.55f, age * 0.3f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.9f * lanceA));
        }

        // -- impact: flash, a snapped ring, a burst, four shards
        if (hit > 0f) {
            RandomSource r2 = inst.random();
            float k = VfxAnim.easeOutCubic(hit), inv = 1f - hit;
            VfxBloom.glow(ctx, buf, b, pw * (0.4f + 0.9f * k), col, 1.3f * inv);
            buf.billboard(ctx, BURST, VfxBlend.ADD, b, pw * (0.5f + 1.5f * k), age * 0.1f, VfxVertexBuffer.withAlpha(pale, inv));
            buf.billboard(ctx, RING, VfxBlend.ADD, b, pw * (0.35f + 1.5f * k), -age * 0.3f, VfxVertexBuffer.withAlpha(col, 0.9f * inv));
            for (int i = 0; i < 4; i++) {
                float ang = r2.nextFloat() * Mth.TWO_PI, d = (0.35f + r2.nextFloat() * 0.5f) * pw * k;
                buf.billboard(ctx, SHARD, VfxBlend.ALPHA, around(ctx, b, d, ang), pw * 0.2f, ang * 3f + age * 0.4f, VfxVertexBuffer.withAlpha(SILVER, 0.95f * inv));
            }
        }
    }

    // ================================================================== FX2: Ward Field
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float f = life(inst, age, 8, 12);
        float R = Math.max(1f, inst.power);
        float grow = VfxAnim.easeOutCubic(clamp01(age / 10f));
        if (f <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = tint(inst), pale = VfxVertexBuffer.whiten(col, 0.6f);
        RandomSource r = inst.random();
        float spin = age * 0.012f;
        float hb = Mth.clamp(R * 0.18f, 0.4f, 0.95f);              // height of the rune wall band
        float domeH = Mth.clamp(R * 0.8f, 1.6f, 3.4f);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.04f, 0));

        // ground: ink shadow, soft light, the ward seal and a counter-turning inner seal
        buf.plane(VfxTextures.GLOW, VfxBlend.ALPHA, ground, R * 1.08f * grow, VfxVertexBuffer.withAlpha(INK, 0.5f * f));
        VfxBloom.planeGlow(buf, ground.lift(0.01f), R * 0.8f * grow, col, 0.55f * f);
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(spin).lift(0.02f), R * grow, VfxVertexBuffer.withAlpha(col, 0.95f * f));
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(-spin * 1.7f + 1f).lift(0.03f), R * 0.5f * grow, VfxVertexBuffer.withAlpha(pale, 0.5f * f));

        // pulses rolling out from the middle: snapped rings
        for (int k = 0; k < 2; k++) {
            float ph = ((age + k * 20f) / 40f) % 1f;
            float pe = VfxAnim.easeOutCubic(ph);
            buf.plane(RING, VfxBlend.ADD, ground.spin(age * 0.05f * (k == 0 ? 1 : -1)).lift(0.04f), R * (0.12f + 0.9f * pe) * grow,
                    VfxVertexBuffer.withAlpha(k == 0 ? pale : col, 0.8f * (1f - ph) * f * clamp01(age / 6f)));
        }

        // the dome ceiling: a hex ward lattice hanging overhead, turning against the seal
        VfxPose roof = VfxPose.facing(new Vector3f(c).add(0, domeH * grow, 0), new Vector3f(0, -1, 0));
        buf.plane(HEX, VfxBlend.ADD, roof.spin(-spin * 0.8f), R * 0.92f * grow, VfxVertexBuffer.withAlpha(col, 0.38f * f));

        // two rune walls at the rim, scrolling opposite ways; bright at the foot, melting upwards
        wall(buf, ctx.seg(24, 12), c, R * 0.985f * grow, 0.06f, hb * grow, R, age * 0.0045f, VfxVertexBuffer.withAlpha(pale, 0.95f * f), VfxVertexBuffer.withAlpha(col, 0.15f * f), hb);
        wall(buf, ctx.seg(16, 10), c, R * 0.93f * grow, hb * 1.25f * grow, hb * 1.85f * grow, R, -age * 0.0035f + 0.3f, VfxVertexBuffer.withAlpha(col, 0.6f * f), VfxVertexBuffer.withAlpha(col, 0f), hb * 0.6f);

        // curtains of rune thread rising from the rim
        for (int i = 0; i < 8; i++) {
            float ang = i * Mth.TWO_PI / 8f + spin * 4f;
            float fl = 0.6f + 0.4f * Mth.sin(age * 0.25f + i * 1.9f);
            Vector3f foot = new Vector3f(Mth.cos(ang) * R * 0.97f * grow, 0.05f, Mth.sin(ang) * R * 0.97f * grow).add(c);
            Vector3f top = new Vector3f(foot).add(0, domeH * 0.9f * grow, 0);
            buf.beam(ctx, STRAND, VfxBlend.ADD, foot, top, 0.26f * hb * 2f, 0.1f, 2, -age * 0.02f + i * 0.3f,
                    VfxVertexBuffer.withAlpha(col, 0.7f * fl * f), VfxVertexBuffer.withAlpha(col, 0f));
        }

        // runes and motes drifting up inside
        for (int i = 0; i < 9; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = Mth.sqrt(r.nextFloat()) * R * 0.88f;
            float per = 36f + r.nextFloat() * 26f, off = r.nextFloat() * per, sz = 0.28f + r.nextFloat() * 0.22f, sway = r.nextFloat() * Mth.TWO_PI;
            float u = ((age + off) % per) / per;
            Vector3f q = new Vector3f(Mth.cos(ang + age * 0.004f) * rad + Mth.sin(u * 5f + sway) * 0.12f, 0.15f + u * domeH * 0.95f,
                    Mth.sin(ang + age * 0.004f) * rad).add(c);
            buf.billboard(ctx, rune(i), VfxBlend.ADD, q, sz * Math.min(1.5f, 0.8f + R * 0.08f), Mth.sin(u * 4f + sway) * 0.3f,
                    VfxVertexBuffer.withAlpha(i % 3 == 0 ? pale : col, Mth.sin(Mth.PI * u) * 0.85f * f * grow));
        }
        for (int i = 0; i < 7; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = Mth.sqrt(r.nextFloat()) * R * 0.95f;
            float per = 24f + r.nextFloat() * 20f, off = r.nextFloat() * per;
            float u = ((age + off) % per) / per;
            Vector3f q = new Vector3f(Mth.cos(ang) * rad, 0.1f + u * domeH, Mth.sin(ang) * rad).add(c);
            sparkle(ctx, buf, q, 0.22f + 0.1f * r.nextFloat(), age * 0.06f + i, i % 2 == 0 ? SILVER : pale, Mth.sin(Mth.PI * u) * f * grow);
        }

        // the floating ward star at the heart, breathing
        float breathe = 0.5f + 0.5f * Mth.sin(age * 0.12f);
        Vector3f heart = new Vector3f(c).add(0, 0.9f + 0.1f * breathe, 0);
        VfxBloom.glow(ctx, buf, heart, 0.4f + 0.12f * breathe, col, 0.8f * f * grow);
        buf.billboard(ctx, STAR, VfxBlend.ADD, heart, (0.9f + 0.3f * breathe) * Math.min(1.6f, 0.7f + R * 0.1f), age * 0.04f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.9f * f * grow));
    }

    /** A vertical ribbon around a circle: bottom edge at y0 with colour cb, top edge at y1 with colour ct. */
    private static void wall(VfxVertexBuffer buf, int segs, Vector3f c, float radius, float y0, float y1, float bigR, float scroll, int cb, int ct, float bandH) {
        if (radius < 0.05f || y1 <= y0) return;
        float circ = Mth.TWO_PI * radius;
        float repeats = Math.max(1, Math.round(circ / (Math.max(0.1f, y1 - y0) * BAND_ASPECT)));
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            float u0 = scroll + repeats * i / segs, u1 = scroll + repeats * (i + 1) / segs;
            Vector3f b0 = new Vector3f(Mth.cos(a0) * radius, y0, Mth.sin(a0) * radius).add(c);
            Vector3f b1 = new Vector3f(Mth.cos(a1) * radius, y0, Mth.sin(a1) * radius).add(c);
            Vector3f t1 = new Vector3f(Mth.cos(a1) * radius, y1, Mth.sin(a1) * radius).add(c);
            Vector3f t0 = new Vector3f(Mth.cos(a0) * radius, y1, Mth.sin(a0) * radius).add(c);
            buf.quad(BAND, VfxBlend.ADD, b0, b1, t1, t0, u0, 0, u1, 1, VfxBlend.ADD.grade(cb, 0.6f), VfxBlend.ADD.grade(ct, 0.6f));
        }
    }

    // ================================================================== FX3: Curse Break
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float dur = Math.max(8f, inst.duration);
        float p = clamp01(age / dur);
        float s = Mth.clamp(inst.power, 0.5f, 3.5f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean hasHint = hint.lengthSquared() > 0.04f;
        if (hasHint) hint.normalize(); else hint.set(0, 0, 0);
        int col = tint(inst), pale = VfxVertexBuffer.whiten(col, 0.65f);
        RandomSource r = inst.random();

        float e6 = VfxAnim.easeOutCubic(clamp01(age / 6f));
        float flashA = 1f - clamp01(age / 11f);
        float glowA = (1f - p) * (1f - p);

        // dark ink halo behind everything: the curse being swallowed
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, c, s * (1.0f + 1.8f * VfxAnim.easeOutCubic(clamp01(age / 14f))), 0,
                VfxVertexBuffer.withAlpha(INK, 0.6f * (1f - clamp01((age - 3f) / (dur - 3f)))));

        // the seals: a flat one on the ground plane, a tilted one along the direction hint (gyroscope)
        float k18 = VfxAnim.easeOutCubic(clamp01(age / 18f)), fadeR = (1f - clamp01(age / 22f));
        VfxPose flat = VfxPose.ground(new Vector3f(c));
        buf.plane(SIGIL, VfxBlend.ADD, flat.spin(age * 0.08f), s * (0.5f + 1.9f * k18), VfxVertexBuffer.withAlpha(col, 0.8f * fadeR));
        VfxPose tilt = VfxPose.facing(new Vector3f(c), hasHint ? hint : new Vector3f(0.3f, 0.2f, 1f));
        buf.plane(RING, VfxBlend.ADD, tilt.spin(-age * 0.14f), s * (0.4f + 2.3f * VfxAnim.easeOutCubic(clamp01(age / 16f))), VfxVertexBuffer.withAlpha(pale, 0.8f * fadeR));

        // burst and star, counter-rotating
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, s * (0.9f + 3.4f * e6), age * 0.05f, VfxVertexBuffer.withAlpha(pale, 0.95f * flashA));
        buf.billboard(ctx, STAR, VfxBlend.ADD, c, s * (1.0f + 2.8f * e6), -age * 0.1f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, flashA));

        // the snapped seal rings sweeping out
        float k1 = VfxAnim.easeOutCubic(clamp01(age / 18f)), k2 = VfxAnim.easeOutCubic(clamp01((age - 3f) / 18f));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, s * (0.5f + 4.0f * k1), age * 0.2f, VfxVertexBuffer.withAlpha(col, 0.95f * (1f - clamp01(age / 20f))));
        if (age > 3f)
            buf.billboard(ctx, RING, VfxBlend.ADD, c, s * (0.4f + 3.0f * k2), -age * 0.27f, VfxVertexBuffer.withAlpha(pale, 0.85f * (1f - clamp01((age - 3f) / 18f))));

        // hot flash
        VfxBloom.glow(ctx, buf, c, s * (0.7f + 1.6f * e6), col, 1.3f * flashA + 0.35f * glowA);
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, s * 0.9f * (1f - 0.5f * p), 0, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.9f * flashA));

        // shards of the broken ward
        for (int i = 0; i < 11; i++) {
            Vector3f d = randUnit(r);
            if (hasHint) d.add(new Vector3f(hint).mul(1.1f)).normalize();
            float speed = 0.7f + r.nextFloat() * 0.9f, size = 0.16f + r.nextFloat() * 0.2f, spinR = (r.nextFloat() - 0.5f) * 0.7f, delay = r.nextFloat() * 2f;
            float a2 = Math.max(0f, age - delay);
            float k = VfxAnim.easeOutCubic(clamp01(a2 / (13f + 8f * speed)));
            Vector3f q = new Vector3f(d).mul(s * (0.35f + 3.0f * speed * k)).add(c).add(0, -0.35f * s * (a2 / 20f) * (a2 / 20f), 0);
            float al = 1f - clamp01((a2 - 8f) / (dur - 8f));
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, q, s * size * 1.5f, a2 * spinR + i, VfxVertexBuffer.withAlpha(i % 3 == 0 ? pale : SILVER, al));
        }

        // runes thrown out more slowly, glowing
        for (int i = 0; i < 6; i++) {
            Vector3f d = randUnit(r);
            if (hasHint) d.add(new Vector3f(hint).mul(0.6f)).normalize();
            float speed = 0.5f + r.nextFloat() * 0.6f, spinR = (r.nextFloat() - 0.5f) * 0.3f;
            float k = VfxAnim.easeOutCubic(clamp01(age / 22f));
            Vector3f q = new Vector3f(d).mul(s * (0.5f + 2.2f * speed * k)).add(c);
            float al = Mth.sin(Mth.PI * clamp01(0.15f + 0.85f * age / dur)) * 0.9f;
            buf.billboard(ctx, rune(i), VfxBlend.ADD, q, s * 0.5f, age * spinR, VfxVertexBuffer.withAlpha(i % 2 == 0 ? col : pale, al * (1f - p)));
        }

        // lance streaks shooting out
        for (int i = 0; i < 8; i++) {
            Vector3f d = randUnit(r);
            if (hasHint) d.add(new Vector3f(hint).mul(0.9f)).normalize();
            float speed = 0.8f + r.nextFloat() * 0.8f, len = 0.7f + r.nextFloat() * 0.9f, delay = r.nextFloat() * 2f;
            float a2 = Math.max(0f, age - delay);
            float kk = clamp01(a2 / 12f), head = VfxAnim.easeOutCubic(kk), tl = VfxAnim.easeInCubic(clamp01(a2 / 14f)) * 0.9f;
            float far = s * (0.4f + 3.2f * speed * head), near = s * (0.4f + 3.2f * speed * head) * tl;
            float al = 1f - clamp01((a2 - 4f) / 10f);
            if (al <= 0.02f || far <= near + 0.02f) continue;
            Vector3f pa = new Vector3f(d).mul(near).add(c), pb = new Vector3f(d).mul(far).add(c);
            buf.beam(ctx, LANCE, VfxBlend.ADD, pa, pb, s * 0.22f * len, s * 0.22f * len, 1, 0,
                    VfxVertexBuffer.withAlpha(col, 0.5f * al), VfxVertexBuffer.withAlpha(0xFFFFFFFF, al));
        }

        // afterglow: a wide faint halo and rising motes
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, s * 3.2f, 0, VfxVertexBuffer.withAlpha(col, 0.3f * glowA));
        for (int i = 0; i < 8; i++) {
            Vector3f d = randUnit(r);
            float rad = 0.5f + r.nextFloat() * 1.1f, ph = r.nextFloat() * Mth.TWO_PI;
            float grow = VfxAnim.easeOutCubic(clamp01(age / 16f));
            Vector3f q = new Vector3f(d).mul(s * rad * grow).add(c).add(0, 0.015f * age * s + 0.1f * s * Mth.sin(age * 0.2f + ph), 0);
            float tw = 0.5f + 0.5f * Mth.sin(age * 0.7f + ph);
            sparkle(ctx, buf, q, s * 0.28f, age * 0.05f + i, i % 2 == 0 ? SILVER : pale, tw * glowA * 1.2f * clamp01(age / 4f));
        }
    }
}
