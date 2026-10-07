package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bronze Magic (the Sekke spells), tarnished bronze with verdigris green and a hot orange glow, drawn as hammered, engraved,
 * riveted metal. Three shapes, each with its own silhouette and its own texture set:
 * <ul>
 *   <li>BRONZE_FX1, CAST / PROJECTILE, "Sekke Magnum Cannonball". A muzzle sigil and ring contract at 'from' while bronze chips
 *       are pulled in; then a translucent engraved sphere with uneven spikes tumbles from 'from' to 'to', dragging a brushed-metal
 *       streak, three glowing afterimages, shock ripples and shed chips, and bursts into a starburst of peaks at 'to'.
 *       from = origin, to = target, power = size of the ball (1.0 = about a block), duration = flight ticks (default 16;
 *       charge 0..18%, flight to 78%, impact to 100%).</li>
 *   <li>BRONZE_FX2, ZONE / FIELD, "Bronze Forge Field". A filigree sigil turns on the ground inside a hammered molten ring; a
 *       wall of riveted plates rises along the rim, bronze spikes jut up around it, three dark Poison Lizards crawl in circles
 *       with a green glow under them, verdigris tarnish stains the floor, glints rise and a pulse ring rolls out every 26
 *       ticks. from = centre on the ground, power = RADIUS in blocks, duration = life ticks (default 80; fades in over 8, out over 12).</li>
 *   <li>BRONZE_FX3, IMPACT / SIGNATURE, "Sekke Shooting Star". A white-hot flash, a starburst, ground shock rings and spikes
 *       punched outwards in all directions (the cannonball's peaks), hammered chips and sparks flying out, a stamped sigil with
 *       verdigris tarnish, flakes falling in the afterglow; from power 1.1 up a pair of feathered bronze wings opens behind it.
 *       from = centre, to = optional direction hint (to - from biases debris and sparks; zero = all around), power = scale
 *       (0.6 to 3.0), duration = life ticks (default 28).</li>
 * </ul>
 * inst.color is mixed 30 percent into the bronze palette, so a white tint still reads as bronze.
 */
public class BronzeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation SPHERE = t("bronze_sphere");
    public static final ResourceLocation STREAK = t("bronze_streak");
    public static final ResourceLocation RING = t("bronze_ring");
    public static final ResourceLocation BAND = t("bronze_band");
    public static final ResourceLocation SIGIL = t("bronze_sigil");
    public static final ResourceLocation CHIP = t("bronze_chip");
    public static final ResourceLocation FLECKS = t("bronze_flecks");
    public static final ResourceLocation WING = t("bronze_wing");
    public static final ResourceLocation SPIKE = t("bronze_spike");
    public static final ResourceLocation LIZARD = t("bronze_lizard");
    public static final ResourceLocation PATINA = t("bronze_patina");
    public static final ResourceLocation BURST = t("bronze_burst");

    private static final int BRONZE = 0xFFD09A4A;
    private static final int VERDI = 0xFF4FB08E;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BRONZE_FX1, VfxShape.BRONZE_FX2, VfxShape.BRONZE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BRONZE_FX1 -> 16;
            case BRONZE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return BRONZE; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BRONZE_FX1 -> cast(inst, ctx, buf);
            case BRONZE_FX2 -> zone(inst, ctx, buf);
            case BRONZE_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small helpers
    private static int tint(VfxInstance inst) { return VfxVertexBuffer.lerpColor(BRONZE, inst.color | 0xFF000000, 0.3f); }
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }
    private static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float x = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float y = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(x, y);
    }

    private static float sm(float e0, float e1, float x) {
        float k = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return k * k * (3 - 2 * k);
    }

    /** A unit vector from two hashes: uniform on the sphere when up = false, on the upper hemisphere when up = true. */
    private static Vector3f dirOf(float u, float v, boolean up) {
        float z = up ? 0.12f + 0.88f * u : u * 2f - 1f;
        float s = Mth.sqrt(Math.max(0f, 1f - z * z)), ang = v * Mth.TWO_PI;
        return new Vector3f(Mth.cos(ang) * s, z, Mth.sin(ang) * s);
    }

    /** A camera-facing ribbon from a (texture bottom) to b (texture top), the sprite's V runs along it. Used for spikes and peaks. */
    private static void strut(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float halfW, int argb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(halfW);
        int col = blend.grade(argb, 0.8f);
        buf.quad(tex, blend, new Vector3f(a).sub(side), new Vector3f(a).add(side), new Vector3f(b).add(side), new Vector3f(b).sub(side), 0, 0, 1, 1, col, col);
    }

    /** A sprite lying on the ground, its right (u = 1) end pointing along 'heading' (radians in the x / z plane). */
    private static void flat(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, float heading, float len, float wid, int argb) {
        float fx = Mth.cos(heading), fz = Mth.sin(heading);
        Vector3f f = new Vector3f(fx * len * 0.5f, 0, fz * len * 0.5f), s = new Vector3f(-fz * wid * 0.5f, 0, fx * wid * 0.5f);
        int col = blend.grade(argb, 0.8f);
        buf.quad(tex, blend, new Vector3f(c).sub(f).add(s), new Vector3f(c).add(f).add(s), new Vector3f(c).add(f).sub(s), new Vector3f(c).sub(f).sub(s),
                0, 0, 1, 1, col, col);
    }

    // ------------------------------------------------------------------ FX1: Sekke Magnum Cannonball
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration);
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) { dir.set(0, 0, 1); dist = 0.05f; } else dir.mul(1f / dist);
        int col = tint(inst), hot = VfxVertexBuffer.whiten(col, 0.6f);

        float tl = 0.18f * d, ta = 0.78f * d;
        float s = Mth.clamp((age - tl) / (ta - tl), 0, 1), se = 0.55f * s + 0.45f * s * s;      // the ball accelerates
        float ti = Mth.clamp((age - ta) / Math.max(1f, d - ta), 0, 1);
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);

        // 1. charge: the muzzle sigil and ring contract on the caster, chips are drawn in
        float tc = Mth.clamp(age / tl, 0, 1);
        float cf = tc * (1f - Mth.clamp((age - tl) / (0.9f * tl + 1f), 0, 1));
        if (cf > 0.02f) {
            VfxPose mz = VfxPose.facing(new Vector3f(dir).mul(0.35f * P).add(from), dir);
            float shrink = 1.5f - 0.6f * tc;
            buf.plane(SIGIL, VfxBlend.ADD, mz.spin(age * 0.25f), 0.9f * P * shrink, a(col, 0.9f * cf));
            buf.plane(RING, VfxBlend.ADD, mz.lift(0.05f).spin(-age * 0.35f), 0.7f * P * shrink, a(hot, 0.8f * cf));
            VfxBloom.glow(ctx, buf, from, 0.5f * P * (0.4f + 0.6f * tc), col, cf);
            for (int i = 0; i < 6; i++) {
                Vector3f rv = dirOf(h(inst, i, 2), h(inst, i, 1), false);
                float rad = (1.5f * (1f - tc) + 0.12f) * P * (0.7f + 0.5f * h(inst, i, 3));
                Vector3f pos = rv.mul(rad).add(from);
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, pos, 0.17f * P, h(inst, i, 4) * 6f + age * 0.3f, a(VfxVertexBuffer.lerpColor(col, hot, tc), cf));
            }
        }
        // 2. muzzle flash as the ball leaves
        float mf = Mth.clamp((age - tl) / (0.3f * d), 0, 1);
        if (age >= tl && mf < 1f) {
            buf.billboard(ctx, BURST, VfxBlend.ADD, from, 2.2f * P * (0.5f + 0.5f * VfxAnim.easeOutCubic(mf)), age * 0.1f, a(hot, (1 - mf) * (1 - mf)));
        }

        // 3. flight
        if (s > 0f) {
            float live = 1f - ti;
            float actual = Math.min(Math.min(dist * 0.55f, 4.2f * P), dist * se) * (1f - 0.7f * ti);
            Vector3f tail = new Vector3f(dir).mul(-actual).add(head);
            Vector3f s1 = ElementFx.side(dir), s2 = new Vector3f(dir).cross(s1).normalize();

            // shock ripples left in the air behind the ball
            for (int k = 0; k < 3; k++) {
                float off = (0.7f * k + 0.5f) * P;
                if (off > actual) continue;
                VfxPose rp = VfxPose.facing(new Vector3f(dir).mul(-off).add(head), dir);
                buf.plane(RING, VfxBlend.ADD, rp.spin(age * 0.3f * (k % 2 == 0 ? 1 : -1)), P * (0.45f + 0.3f * k), a(col, (0.55f - 0.15f * k) * live));
            }
            // the brushed-metal streak: a wide bronze one with a thin white-hot one inside
            ElementFx.streak(buf, ctx, STREAK, VfxBlend.ADD, tail, head, 0.95f * P * (0.8f + 0.2f * live), a(col, 0.45f * live));
            Vector3f core = new Vector3f(dir).mul(-actual * 0.55f).add(head);
            ElementFx.streak(buf, ctx, STREAK, VfxBlend.ADD, core, head, 0.26f * P, a(hot, 0.4f * live));

            // afterimages of the ball
            for (int i = 1; i <= 3; i++) {
                float off = i * 0.5f * P;
                if (off > actual) continue;
                Vector3f gp = new Vector3f(dir).mul(-off).add(head);
                buf.billboard(ctx, SPHERE, VfxBlend.ADD, gp, P * (1.0f - 0.16f * i), -age * 0.2f + i * 1.3f, a(col, 0.2f * live / i));
            }
            // chips and glints shed along the trail
            for (int i = 0; i < 8; i++) {
                float u = (h(inst, i, 5) + age * 0.07f) % 1f;
                float lat = (0.25f + 0.6f * u) * P;
                Vector3f pos = new Vector3f(dir).mul(-u * actual).add(head)
                        .add(new Vector3f(s1).mul((h(inst, i, 6) - 0.5f) * 2f * lat)).add(new Vector3f(s2).mul((h(inst, i, 7) - 0.5f) * 2f * lat));
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, pos, 0.17f * P * (1f - 0.6f * u), h(inst, i, 8) * 6f + age * 0.4f * (0.5f + h(inst, i, 9)),
                        a(VfxVertexBuffer.lerpColor(hot, col, u), (1f - u) * sm(0f, 0.06f, u) * live));
            }
            for (int i = 0; i < 3; i++) {
                buf.billboard(ctx, FLECKS, VfxBlend.ADD, new Vector3f(dir).mul(-0.3f * P * i).add(head), 1.7f * P, h(inst, i, 10) * 6.28f + age * 0.05f * (i - 1), a(hot, 0.25f * live));
            }
            // the ball itself: engraved translucent sphere, spikes turning, a glow of hammered light over it
            float sz = P * (age < ta ? 1f : 1f - VfxAnim.easeInCubic(ti));
            buf.billboard(ctx, SPHERE, VfxBlend.ALPHA, head, sz, age * 0.22f, a(VfxVertexBuffer.lerpColor(col, hot, 0.1f), live));
            buf.billboard(ctx, SPHERE, VfxBlend.ADD, head, sz * 1.08f, -age * 0.3f, a(col, 0.22f * live));
            VfxBloom.glow(ctx, buf, head, 0.5f * P, col, 0.45f * live);
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, 0.45f * P, 0, a(hot, 0.4f * live));
        }

        // 4. impact: the sphere bursts into its peaks
        if (age >= ta) {
            float e = VfxAnim.easeOutCubic(ti), inv = 1f - ti;
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, to, 3.0f * P * (0.5f + 0.5f * e), 0, a(hot, 0.9f * inv));
            buf.billboard(ctx, BURST, VfxBlend.ADD, to, 0.4f * P + 3.2f * P * e, age * 0.08f, a(col, inv));
            buf.billboard(ctx, RING, VfxBlend.ADD, to, 3.6f * P * e, 0, a(hot, 0.8f * inv));
            for (int i = 0; i < 8; i++) {
                Vector3f rv = dirOf(h(inst, i, 11), h(inst, i, 12), false);
                Vector3f pos = rv.mul(e * 1.7f * P * (0.6f + 0.6f * h(inst, i, 13))).add(to);
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, pos, 0.2f * P * (1f - 0.5f * ti), h(inst, i, 14) * 6f + age * 0.5f, a(hot, inv));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Bronze Forge Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.5f, inst.power);
        float fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
        float Ro = R * open;
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.04f, 0);
        VfxPose ground = VfxPose.ground(g);
        int col = tint(inst), hot = VfxVertexBuffer.whiten(col, 0.55f), verdi = VfxVertexBuffer.lerpColor(VERDI, col, 0.1f);
        int dark = ElementFx.mulRgb(col, 0.55f);

        // verdigris tarnish on the floor and a light under everything
        buf.plane(PATINA, VfxBlend.ALPHA, ground.lift(-0.01f).spin(0.7f), Ro * 1.05f, a(WHITE, 0.6f * fade));
        VfxBloom.planeGlow(buf, ground.lift(0.01f), Ro * 0.7f, col, 0.5f * fade);
        // the engraved sigil: a dark body and a lit engraving turning one way, a smaller echo of it turning the other (parallax)
        buf.plane(SIGIL, VfxBlend.ALPHA, ground.lift(0.02f).spin(age * 0.012f), Ro * 0.86f, a(dark, 0.7f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.012f), Ro * 0.86f, a(col, 0.9f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.04f).spin(-age * 0.03f), Ro * 0.5f, a(hot, 0.45f * fade));
        // the hammered molten ring on the rim
        buf.plane(RING, VfxBlend.ALPHA, ground.lift(0.05f).spin(age * 0.006f), Ro * 1.14f, a(ElementFx.mulRgb(col, 0.85f), 0.95f * fade));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.06f).spin(age * 0.006f), Ro * 1.14f, a(col, 0.55f * fade * (0.75f + 0.25f * VfxAnim.pulse(age, 1.5f))));

        // the wall of riveted plates rising along the rim
        float H = Math.min(1.8f, 0.55f + 0.10f * R), wh = H * open;
        if (wh > 0.05f) {
            float repeats = Math.max(1, Math.round(Mth.TWO_PI * Ro / (16f * H)));
            ElementFx.hoop(buf, BAND, VfxBlend.ALPHA, ground.lift(wh * 0.5f), Ro * 0.985f, wh * 0.5f, ctx.seg(24, 12), repeats, age * 0.004f,
                    a(ElementFx.mulRgb(col, 0.9f), 0.9f * fade));
        }
        // bronze spikes standing up around the wall
        int n = ctx.seg(10, 6);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + 0.25f * h(inst, i, 1), rr = Ro * (0.93f + 0.06f * h(inst, i, 2));
            float rise = VfxAnim.easeOutBack(Mth.clamp((age - 2 - i * 0.7f) / 9f, 0, 1));
            float hh = (0.9f + 0.9f * h(inst, i, 3)) * (0.7f + 0.12f * Math.min(R, 8f)) * rise;
            if (hh < 0.05f) continue;
            Vector3f base = new Vector3f(g).add(Mth.cos(ang) * rr, -0.02f, Mth.sin(ang) * rr);
            strut(buf, SPIKE, VfxBlend.ALPHA, base, new Vector3f(base).add(0, hh, 0), hh * 0.22f, a(col, fade));
        }
        // three Poison Lizards crawling a circle, a green glow under each
        float llen = 0.9f + 0.22f * Mth.sqrt(R);
        for (int i = 0; i < 3; i++) {
            float ang = Mth.TWO_PI * i / 3 + age * 0.035f;
            Vector3f pos = new Vector3f(g).add(Mth.cos(ang) * Ro * 0.6f, 0.03f, Mth.sin(ang) * Ro * 0.6f);
            buf.plane(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(pos).lift(-0.01f), llen * 0.8f, a(verdi, 0.35f * fade * open));
            flat(buf, LIZARD, VfxBlend.ALPHA, pos, lizardHeading(ang, age, i), llen, llen * 0.5f, a(ElementFx.mulRgb(col, 0.8f), fade * open));
        }
        // pulses rolling out from the middle
        for (int k = 0; k < 2; k++) {
            float ph = ((age - k * 13f) / 26f);
            if (ph <= 0f) continue;
            ph = ph % 1f;
            buf.plane(RING, VfxBlend.ADD, ground.lift(0.07f), Math.max(0.2f, Ro * ph) / 0.84f, a(hot, Mth.sqrt(1f - ph) * 0.7f * fade));
        }
        // rising glint clouds and tumbling flakes
        for (int i = 0; i < 4; i++) {
            float f = (age * 0.018f + h(inst, i, 4)) % 1f;
            float ang = h(inst, i, 5) * Mth.TWO_PI, rr = Mth.sqrt(h(inst, i, 6)) * Ro * 0.8f;
            Vector3f pos = new Vector3f(g).add(Mth.cos(ang) * rr, f * (1.2f + 0.2f * H) * 2f, Mth.sin(ang) * rr);
            buf.billboard(ctx, FLECKS, VfxBlend.ADD, pos, Math.max(0.9f, 0.45f * Mth.sqrt(R)), h(inst, i, 7) * 6f, a(hot, Mth.sin(f * Mth.PI) * 0.8f * fade));
        }
        for (int i = 0; i < 6; i++) {
            float f = (age * 0.012f + h(inst, i, 8)) % 1f;
            float ang = h(inst, i, 9) * Mth.TWO_PI, rr = Mth.sqrt(h(inst, i, 10)) * Ro * 0.9f;
            Vector3f pos = new Vector3f(g).add(Mth.cos(ang) * rr, f * 2.2f + 0.1f, Mth.sin(ang) * rr);
            buf.billboard(ctx, CHIP, VfxBlend.ALPHA, pos, 0.2f, h(inst, i, 11) * 6f + age * 0.15f, a(col, Mth.sin(f * Mth.PI) * fade));
        }
        // the hub
        buf.billboard(ctx, BURST, VfxBlend.ADD, new Vector3f(g).add(0, 0.3f, 0), Math.max(1.2f, 0.5f * Ro), age * 0.015f, a(hot, 0.5f * fade * open));
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.3f, 0), 0.5f + 0.1f * R, col, 0.5f * fade * open);
    }

    private static float lizardHeading(float ang, float age, int i) {
        // the quad's right end points along the tangent of the circle (derivative of (cos, sin) is (-sin, cos)), plus a little body sway
        return (float) Math.atan2(Mth.cos(ang), -Mth.sin(ang)) + Mth.sin(age * 0.4f + i * 2f) * 0.12f;
    }

    // ------------------------------------------------------------------ FX3: Sekke Shooting Star
    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(10f, inst.duration);
        float S = Mth.clamp(inst.power, 0.5f, 4f);
        float fade = life(inst, age, 0, 12), tt = Mth.clamp(age / d, 0, 1);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        float hl = hint.length();
        Vector3f hd = hl > 0.2f ? hint.mul(1f / hl) : new Vector3f();
        int col = tint(inst), hot = VfxVertexBuffer.whiten(col, 0.6f), dark = ElementFx.mulRgb(col, 0.55f);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0));
        float e6 = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1)), e16 = VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1));

        // stamped sigil and tarnish on the ground
        float stamp = fade * (1f - 0.5f * tt);
        buf.plane(PATINA, VfxBlend.ALPHA, ground.lift(-0.01f).spin(1.3f), 2.2f * S * e6, a(WHITE, 0.55f * stamp));
        buf.plane(SIGIL, VfxBlend.ALPHA, ground.lift(0.01f).spin(age * 0.04f), 1.9f * S * e6, a(dark, 0.6f * stamp));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.02f).spin(age * 0.04f), 1.9f * S * e6, a(col, 0.8f * stamp));

        // wings (power 1.1 up): feathered bronze opening behind the burst
        float wk = sm(0.9f, 1.4f, S);
        if (wk > 0.05f) {
            float open = VfxAnim.easeOutBack(Mth.clamp((age - 1f) / 9f, 0, 1)) * (1f + 0.05f * Mth.sin(age * 0.5f));
            float wf = Mth.clamp((age - 1f) / 3f, 0, 1) * life(inst, age, 0, 10) * wk;
            Vector3f wc = new Vector3f(c).add(0, 0.5f * S, 0);
            buf.billboard(ctx, WING, VfxBlend.ALPHA, wc, 4.6f * S * open, 0, a(VfxVertexBuffer.lerpColor(col, hot, 0.2f), 0.95f * wf));
            buf.billboard(ctx, WING, VfxBlend.ADD, wc, 4.7f * S * open, 0, a(hot, 0.35f * wf));
        }

        // the cannonball's peaks punched outwards, then drawn back in
        int np = ctx.seg(9, 5);
        for (int i = 0; i < np; i++) {
            Vector3f pd = dirOf(h(inst, i, 1), h(inst, i, 2), false).add(new Vector3f(hd).mul(0.5f)).normalize();
            float out = VfxAnim.easeOutBack(Mth.clamp((age - 0.5f * (i % 3)) / 5f, 0, 1)) * (1f - sm(0.45f, 0.95f, tt));
            float len = (1.5f + 1.4f * h(inst, i, 3)) * S * out;
            if (len < 0.05f) continue;
            Vector3f base = new Vector3f(pd).mul(0.25f * S).add(c);
            strut(buf, SPIKE, VfxBlend.ALPHA, base, new Vector3f(pd).mul(len).add(c), len * 0.16f, a(col, fade));
        }

        // white-hot flash, starburst and ground shock rings
        float flash = (1f - VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1)));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 4.5f * S * (0.4f + 0.6f * e6), 0, a(hot, flash));
        float bf = 1f - sm(3f, 18f, age);
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, 5.0f * S * e6, age * 0.02f, a(col, bf));
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, 3.4f * S * e6, -age * 0.035f, a(hot, 0.8f * bf));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.05f), Math.max(0.3f, 4.2f * S * e16), a(hot, (1f - e16) * 0.9f));
        float e2 = VfxAnim.easeOutCubic(Mth.clamp((age - 3f) / 16f, 0, 1));
        if (age > 3f) buf.plane(RING, VfxBlend.ADD, ground.lift(0.06f), Math.max(0.3f, 3.0f * S * e2), a(col, (1f - e2) * 0.8f));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, 3.2f * S * e6, 0, a(col, (1f - e6) * 0.7f));

        // hammered chips thrown out under gravity
        int nc = ctx.seg(14, 7);
        float T = age / 20f;
        for (int i = 0; i < nc; i++) {
            Vector3f vd = dirOf(h(inst, i, 4), h(inst, i, 5), true).mul((1.4f + 2.2f * h(inst, i, 6)) * S).add(new Vector3f(hd).mul(2.2f * S));
            Vector3f pos = new Vector3f(vd).mul(T).add(c).add(0, -0.5f * 3.4f * T * T, 0);
            float life = 1f - sm(0.55f, 1f, tt);
            buf.billboard(ctx, CHIP, VfxBlend.ALPHA, pos, 0.24f * S * (0.7f + 0.5f * h(inst, i, 7)), h(inst, i, 8) * 6f + age * (0.3f + h(inst, i, 9)),
                    a(VfxVertexBuffer.lerpColor(hot, col, tt), life));
        }
        // sparks: comets racing outwards
        float us = Mth.clamp(age / 14f, 0, 1);
        if (us < 1f) {
            int ns = ctx.seg(10, 5);
            for (int i = 0; i < ns; i++) {
                Vector3f sd = dirOf(h(inst, i, 12), h(inst, i, 13), false).add(new Vector3f(hd).mul(0.6f)).normalize();
                float reach = S * (2.4f + 1.6f * h(inst, i, 14));
                Vector3f head = new Vector3f(sd).mul(reach * VfxAnim.easeOutCubic(us)).add(c);
                Vector3f tail = new Vector3f(sd).mul(reach * VfxAnim.easeOutCubic(Math.max(0f, us - 0.38f))).add(c);
                ElementFx.streak(buf, ctx, STREAK, VfxBlend.ADD, tail, head, 0.16f * S, a(hot, 1f - us));
            }
        }
        // afterglow: a lingering light and tarnish flakes drifting down
        float ag = (1f - tt) * (1f - tt);
        VfxBloom.glow(ctx, buf, c, 0.9f * S, col, 0.55f * ag);
        VfxBloom.planeGlow(buf, ground.lift(0.015f), 1.6f * S, col, 0.6f * ag);
        for (int i = 0; i < 6; i++) {
            float ang = h(inst, i, 15) * Mth.TWO_PI, rr = (0.4f + 1.4f * h(inst, i, 16)) * S;
            float yy = (0.9f + 1.0f * h(inst, i, 17)) * S - Math.max(0f, age - 4f) * 0.06f * S;
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * rr, Math.max(0.05f, yy), Mth.sin(ang) * rr);
            buf.billboard(ctx, PATINA, VfxBlend.ALPHA, pos, 0.34f * S, h(inst, i, 18) * 6f + age * 0.1f, a(WHITE, sm(2f, 7f, age) * fade * 0.9f));
        }
    }
}
