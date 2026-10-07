package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Briar Magic, drawn after the owner's still: towering tangles of pale-yellow ribbon-vines, each a flat strap with fine parallel
 * grooves, a dark ink outline and small triangular thorns along the edges, coiling and looping; small blue roses (and a few
 * scarlet ones, the Queen's colours) bloom out of the tangle; a cyan-blue grimoire glow lights it from within. Three shapes, each
 * with its own silhouette and texture set:
 * <ul>
 *   <li>BRIAR_FX1, CAST / PROJECTILE, "Thorn Lash". A small rose and thorn sigil contracts at 'from' while pollen is drawn in; then
 *       two straps braid around the line from 'from' to 'to' like a cracking whip (a third, ghostly strand is its afterimage),
 *       thorns bristling off them, a rose blooming on the head and a blue-cyan glow running along the lash; the head lands at
 *       'to' in a thorn-star flash with petals and thorn shards, and the whip withdraws.
 *       from = origin, to = target, power = size (1.0 = a vine about 0.2 blocks wide), duration = flight ticks (default 16;
 *       charge 0..28%, lash 20..75%, impact from 74%).</li>
 *   <li>BRIAR_FX2, ZONE / FIELD, "Briar Garden" (Blue Rose Paradise, the Queen's mana array). A briar sigil turns on the ground
 *       inside a thorned strap ring; a wall of upright tangles grows along the rim with braided helix columns between them,
 *       thorn spikes jut from the floor, blue and scarlet roses bloom on the wall one after another, pollen rises and a soft
 *       ring pulses outwards every 26 ticks. from = centre on the ground, power = RADIUS in blocks, duration = life ticks
 *       (default 80; fades in over 8, out over 12).</li>
 *   <li>BRIAR_FX3, IMPACT / SIGNATURE, "Corpse-Hunting Briar Tree". A flash and a thorn star, a shock ring and sigil on the
 *       ground, then a tower of tangled vines erupts upwards (three stacked masses over a darker back layer, braided columns
 *       around it) while strap ribbons whip outwards, thorn shards fly, roses bloom on the tower and petals fall in the afterglow.
 *       from = centre, to = optional direction hint (to - from leans the tower and aims the shards; zero = upright),
 *       power = scale (0.6 to 3.0), duration = life ticks (default 28).</li>
 * </ul>
 * inst.color is mixed 22 percent into the pale strap colour and 25 percent into the glow, so a white tint still reads as briar.
 */
public class BriarLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation STRAP = t("briar_strap");
    public static final ResourceLocation BAND = t("briar_band");
    public static final ResourceLocation HELIX = t("briar_helix");
    public static final ResourceLocation TANGLE = t("briar_tangle");
    public static final ResourceLocation ROSE = t("briar_rose");
    public static final ResourceLocation PETAL = t("briar_petal");
    public static final ResourceLocation THORN = t("briar_thorn");
    public static final ResourceLocation SIGIL = t("briar_sigil");
    public static final ResourceLocation BURST = t("briar_burst");
    public static final ResourceLocation HALO = t("briar_halo");
    public static final ResourceLocation POLLEN = t("briar_pollen");

    private static final int PALE = 0xFFF0E9A8;
    private static final int BLUE = 0xFF4AA3FF;
    private static final int RED = 0xFFE8465A;
    private static final int CYAN = 0xFF3EE6FF;
    private static final int GREEN = 0xFF8AFF4A;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BRIAR_FX1, VfxShape.BRIAR_FX2, VfxShape.BRIAR_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BRIAR_FX1 -> 16;
            case BRIAR_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return PALE; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BRIAR_FX1 -> cast(inst, ctx, buf);
            case BRIAR_FX2 -> zone(inst, ctx, buf);
            case BRIAR_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small helpers
    private static int strapColor(VfxInstance inst) { return VfxVertexBuffer.lerpColor(PALE, inst.color | 0xFF000000, 0.22f); }
    private static int glowColor(VfxInstance inst) { return VfxVertexBuffer.lerpColor(CYAN, inst.color | 0xFF000000, 0.25f); }
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }
    private static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }

    private static int shade(int argb, float k) {
        int r = Mth.clamp((int) (((argb >> 16) & 255) * k), 0, 255), g = Mth.clamp((int) (((argb >> 8) & 255) * k), 0, 255), b = Mth.clamp((int) ((argb & 255) * k), 0, 255);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float x = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float y = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(x, y);
    }

    private static float sm(float e0, float e1, float x) {
        float k = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return k * k * (3 - 2 * k);
    }

    private static Vector3f dirOf(float u, float v, boolean up) {
        float z = up ? 0.12f + 0.88f * u : u * 2f - 1f;
        float s = Mth.sqrt(Math.max(0f, 1f - z * z)), ang = v * Mth.TWO_PI;
        return new Vector3f(Mth.cos(ang) * s, z, Mth.sin(ang) * s);
    }

    /** Camera-facing ribbon from a (texture bottom) to b (texture top); the sprite's V runs along it from vA at a to vB at b. */
    private static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float wA, float wB,
                              float vA, float vB, int cA, int cB) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-8f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (side.lengthSquared() < 1e-10f) return;
        side.normalize();
        Vector3f sa = new Vector3f(side).mul(wA * 0.5f), sb = new Vector3f(side).mul(wB * 0.5f);
        buf.quad(tex, blend, new Vector3f(a).sub(sa), new Vector3f(a).add(sa), new Vector3f(b).add(sb), new Vector3f(b).sub(sb),
                0, vB, 1, vA, blend.grade(cA, 0.8f), blend.grade(cB, 0.8f));
    }

    /** A thorn: texture base at a, tip at b. */
    private static void thorn(VfxVertexBuffer buf, Vector3f a, Vector3f b, float width, int argb) {
        strip(buf, THORN, VfxBlend.ALPHA, a, b, width, width * 0.75f, 1f, 0f, argb, argb);
    }

    /** An upright sprite that turns around the vertical axis to face the camera, base centre on the ground, lean shifts the top sideways. */
    private static void standing(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f base, float w, float hgt,
                                 boolean flip, float lean, int argb) {
        Vector3f r = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0); else r.normalize();
        Vector3f hw = new Vector3f(r).mul(w * 0.5f);
        Vector3f up = new Vector3f(r).mul(lean).add(0, hgt, 0);
        int top = blend.grade(argb, 0.8f), bot = blend.grade(blend == VfxBlend.ALPHA ? shade(argb, 0.86f) : argb, 0.5f);
        buf.quad(tex, blend, new Vector3f(base).sub(hw), new Vector3f(base).add(hw), new Vector3f(base).add(hw).add(up), new Vector3f(base).sub(hw).add(up),
                flip ? 1 : 0, 0, flip ? 0 : 1, 1, bot, top);
    }

    private static int roseColor(int k) { return k % 4 == 3 ? RED : BLUE; }

    // ------------------------------------------------------------------ FX1: Thorn Lash
    private static Vector3f vinePoint(Vector3f f, Vector3f dir, Vector3f u, Vector3f v, float s, float amp, float freq, float spin, float strand) {
        float env = (float) Math.pow(Mth.sin(Mth.PI * Mth.clamp(s, 0f, 1f)), 0.6);
        float ph = s * freq * Mth.TWO_PI - spin + strand * Mth.PI;
        float c = Mth.cos(ph) * amp * env, sn = Mth.sin(ph) * amp * env;
        return new Vector3f(f.x + dir.x * s + u.x * c + v.x * sn, f.y + dir.y * s + u.y * c + v.y * sn, f.z + dir.z * s + u.z * c + v.z * sn);
    }

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(4, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f F = ctx.rel(inst.from(ctx)), T = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(T).sub(F);
        if (dir.lengthSquared() < 0.09f) { dir.set(0, 0, 3); T = new Vector3f(F).add(dir); }
        float len = dir.length();
        Vector3f axis = new Vector3f(dir).div(len);
        Vector3f u = new Vector3f(axis).cross(0, 1, 0);
        if (u.lengthSquared() < 1e-4f) u.set(1, 0, 0);
        u.normalize();
        Vector3f v = new Vector3f(axis).cross(u).normalize();
        int strap = strapColor(inst), glow = glowColor(inst);
        float fade = life(inst, age, 0, D * 0.16f);

        // 1. charge: the sigil contracts on 'from', a rose sprouts, pollen is drawn in
        float cp = sm(0f, 0.08f, p) * (1f - sm(0.22f, 0.36f, p));
        if (cp > 0.01f) {
            float shrink = 1.45f - 0.95f * VfxAnim.easeOutCubic(Mth.clamp(p / 0.3f, 0, 1));
            VfxPose sig = VfxPose.facing(F, axis).spin(age * 0.33f);
            buf.plane(SIGIL, VfxBlend.ALPHA, sig, 0.42f * pw * shrink, a(strap, cp));
            buf.plane(SIGIL, VfxBlend.ADD, sig.spin(-age * 0.6f).lift(0.01f), 0.5f * pw * shrink, a(glow, cp * 0.55f));
            buf.billboard(ctx, GLOWTEX, VfxBlend.ADD, F, 0.9f * pw * cp, 0f, a(glow, cp * 0.8f));
            buf.billboard(ctx, ROSE, VfxBlend.ALPHA, F, 0.26f * pw * VfxAnim.easeOutBack(Mth.clamp(p / 0.22f, 0, 1)), age * 0.12f, a(BLUE, cp));
            for (int i = 0; i < 5; i++) {
                float k = (h(inst, i, 1) + age * 0.07f) % 1f;
                Vector3f d = dirOf(h(inst, i, 2), h(inst, i, 3), false).mul(0.9f * pw * (1f - k));
                buf.billboard(ctx, POLLEN, VfxBlend.ADD, new Vector3f(F).add(d), 0.14f * pw, k * 6f, a(i % 2 == 0 ? glow : GREEN, cp * Mth.sin(Mth.PI * k)));
            }
        }

        // 2. the lash
        float hp = Mth.clamp((p - 0.2f) / 0.55f, 0f, 1f);
        hp = (float) Math.pow(hp, 0.8);
        float tail = Mth.clamp((p - 0.55f) / 0.4f, 0f, 1f);
        tail = Math.min(tail * tail, hp - 0.04f);
        if (hp > 0.02f && tail < hp) {
            float vwid = 0.34f * pw;
            int n = ctx.seg(10, 5);
            float amp = 0.3f * pw, spin = age * 0.7f;
            for (int st = 0; st < 2; st++) {
                Vector3f prev = vinePoint(F, dir, u, v, tail, amp, 2.5f, spin, st);
                for (int i = 1; i <= n; i++) {
                    float s0 = Mth.lerp((i - 1f) / n, tail, hp), s1 = Mth.lerp((float) i / n, tail, hp);
                    Vector3f cur = vinePoint(F, dir, u, v, s1, amp, 2.5f, spin, st);
                    float f0 = (i - 1f) / n, f1 = (float) i / n;
                    float w0 = vwid * (0.45f + 0.55f * sm(0f, 0.6f, f0)), w1 = vwid * (0.45f + 0.55f * sm(0f, 0.6f, f1));
                    strip(buf, STRAP, VfxBlend.ALPHA, prev, cur, w0, w1, s0 * len / (4f * vwid), s1 * len / (4f * vwid),
                            a(strap, fade * (0.4f + 0.6f * f0)), a(strap, fade * (0.4f + 0.6f * f1)));
                    prev = cur;
                }
            }
            // thorns bristling off the strands
            for (int k = 0; k < 7; k++) {
                float s = Mth.lerp((k + 0.5f) / 7f, tail, hp);
                int st = k % 2;
                Vector3f pt = vinePoint(F, dir, u, v, s, amp, 2.5f, spin, st);
                Vector3f ax = new Vector3f(F).add(dir.x * s, dir.y * s, dir.z * s);
                Vector3f out = new Vector3f(pt).sub(ax);
                if (out.lengthSquared() < 1e-6f) out.set(u);
                out.normalize().mul(0.2f * pw * (0.7f + 0.3f * h(inst, k, 4)));
                thorn(buf, pt, new Vector3f(pt).add(out), 0.09f * pw, a(strap, fade));
            }
            // ghost strand = the afterimage, and the glow running along the lash
            Vector3f gprev = vinePoint(F, dir, u, v, tail, amp * 1.55f, 2.0f, spin * 0.6f + 1.3f, 0.5f);
            for (int i = 1; i <= 5; i++) {
                float f1 = i / 5f;
                Vector3f cur = vinePoint(F, dir, u, v, Mth.lerp(f1, tail, hp), amp * 1.55f, 2.0f, spin * 0.6f + 1.3f, 0.5f);
                strip(buf, STRAP, VfxBlend.ADD, gprev, cur, 0.15f * pw, 0.15f * pw, 0f, 1f, a(glow, 0.1f * fade * f1), a(glow, 0.3f * fade * f1));
                gprev = cur;
            }
            Vector3f ta = new Vector3f(F).add(dir.x * tail, dir.y * tail, dir.z * tail), hd = new Vector3f(F).add(dir.x * hp, dir.y * hp, dir.z * hp);
            buf.beam(ctx, GLOWTEX, VfxBlend.ADD, ta, hd, 0.25f * pw, 0.65f * pw, 4, 0f, a(glow, 0.0f), a(glow, 0.55f * fade));
            // the head: a rose in a glow
            float bloom = VfxAnim.easeOutBack(Mth.clamp((hp - 0.02f) / 0.2f, 0, 1));
            buf.billboard(ctx, GLOWTEX, VfxBlend.ADD, hd, 0.85f * pw * bloom, 0f, a(glow, 0.6f * fade));
            buf.billboard(ctx, ROSE, VfxBlend.ALPHA, hd, 0.34f * pw * bloom, age * 0.2f, a(BLUE, fade));
            // pollen shed from the lash
            for (int i = 0; i < 8; i++) {
                float s = Mth.lerp(h(inst, i, 5), tail, hp);
                float drift = (age * 0.03f + h(inst, i, 6)) % 1f;
                Vector3f pt = new Vector3f(F).add(dir.x * s, dir.y * s, dir.z * s);
                pt.add(u.x * (h(inst, i, 7) - 0.5f) * 0.6f * pw, u.y * (h(inst, i, 7) - 0.5f) * 0.6f * pw + drift * 0.3f * pw, u.z * (h(inst, i, 7) - 0.5f) * 0.6f * pw);
                buf.billboard(ctx, POLLEN, VfxBlend.ADD, pt, 0.12f * pw, age * 0.3f + i, a(i % 3 == 0 ? GREEN : glow, fade * (1f - drift)));
            }
        }

        // 3. impact at 'to'
        float imp = Mth.clamp((p - 0.74f) / 0.26f, 0f, 1f);
        if (imp > 0f) {
            float e = VfxAnim.easeOutCubic(imp), out = 1f - imp;
            buf.billboard(ctx, BURST, VfxBlend.ADD, T, (0.5f + 1.8f * e) * pw, age * 0.15f, a(VfxVertexBuffer.lerpColor(WHITE, glow, 0.35f), out));
            buf.billboard(ctx, GLOWTEX, VfxBlend.ADD, T, (0.6f + 1.2f * e) * pw, 0f, a(glow, out * 0.6f));
            buf.billboard(ctx, HALO, VfxBlend.ADD, T, (0.3f + 2.0f * e) * pw, 0f, a(GREEN, out * 0.5f));
            for (int i = 0; i < 6; i++) {
                Vector3f d = dirOf(h(inst, i, 8), h(inst, i, 9), false).mul(0.95f * pw * e);
                Vector3f ps = new Vector3f(T).add(d.x, d.y - 0.5f * imp * imp * pw, d.z);
                buf.billboard(ctx, PETAL, VfxBlend.ALPHA, ps, 0.22f * pw, age * 0.3f + i * 1.7f, a(i % 3 == 2 ? RED : BLUE, out));
            }
            for (int i = 0; i < 5; i++) {
                Vector3f d = dirOf(h(inst, i, 10), h(inst, i, 11), false);
                Vector3f a0 = new Vector3f(T).add(d.x * 0.15f * pw, d.y * 0.15f * pw, d.z * 0.15f * pw);
                Vector3f b0 = new Vector3f(T).add(d.x * (0.2f + 0.6f * e) * pw, d.y * (0.2f + 0.6f * e) * pw, d.z * (0.2f + 0.6f * e) * pw);
                thorn(buf, a0, b0, 0.09f * pw, a(strap, out));
            }
        }
    }

    private static final ResourceLocation GLOWTEX = VfxTextures.GLOW;

    // ------------------------------------------------------------------ FX2: Briar Garden
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.2f, inst.power);
        float f = life(inst, age, 8f, 12f);
        if (f <= 0.001f) return;
        Vector3f C = ctx.rel(inst.from(ctx));
        int strap = strapColor(inst), glow = glowColor(inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0f, 1f));
        float pulse = VfxAnim.pulse(age, 1.1f);
        VfxPose g = VfxPose.ground(new Vector3f(C).add(0, 0.03f, 0));

        // ground: green-cyan light under everything, the sigil, its cyan counter-turning copy, the thorn ring
        buf.plane(GLOWTEX, VfxBlend.ADD, g, R * 1.1f, a(GREEN, f * (0.2f + 0.08f * pulse)));
        buf.plane(SIGIL, VfxBlend.ALPHA, g.lift(0.02f).spin(age * 0.018f), R * 0.9f * open, a(strap, f * 0.95f));
        buf.plane(SIGIL, VfxBlend.ADD, g.lift(0.04f).spin(-age * 0.03f), R * 0.9f * open, a(glow, f * (0.25f + 0.2f * pulse)));
        int ringSeg = Mth.clamp(Math.round(R * 3f), 14, 22);
        buf.ring(BAND, VfxBlend.ALPHA, g.lift(0.01f), R - 0.3f, R + 0.3f, ringSeg, Mth.TWO_PI * R / 2.4f, -age * 0.004f, a(strap, f));

        // the wall: upright tangles with braided helix columns between them
        int n = Mth.clamp(Math.round(R * 1.7f), 6, 9);
        float hs = Mth.clamp(R * 0.5f, 1.3f, 3.0f);
        float wallW = Math.min(3.4f, Mth.TWO_PI * R / n * 1.3f);
        for (int k = 0; k < n; k++) {
            float ang = Mth.TWO_PI * k / n + 0.3f;
            float gk = VfxAnim.easeOutBack(Mth.clamp((age - 1.5f - k * 0.9f) / 12f, 0f, 1f));
            float gc = VfxAnim.easeOutBack(Mth.clamp((age - 4f - k * 0.9f) / 12f, 0f, 1f));
            if (gk > 0.01f) {
                Vector3f base = new Vector3f(C).add(Mth.cos(ang) * R, 0f, Mth.sin(ang) * R);
                float sway = 0.1f * hs * Mth.sin(age * 0.07f + k * 1.9f);
                standing(buf, ctx, TANGLE, VfxBlend.ALPHA, base, wallW, hs * (0.85f + 0.3f * h(inst, k, 1)) * gk, (k & 1) == 1, sway, a(strap, f));
            }
            if (gc > 0.01f) {
                float a2 = ang + Mth.PI / n;
                Vector3f base = new Vector3f(C).add(Mth.cos(a2) * R * 0.99f, 0f, Mth.sin(a2) * R * 0.99f);
                standing(buf, ctx, HELIX, VfxBlend.ALPHA, base, 0.6f + 0.1f * hs, hs * 1.3f * gc, (k & 1) == 0, 0f, a(strap, f));
            }
        }
        // thorn spikes jutting from the floor inside the ring
        for (int i = 0; i < 6; i++) {
            float ang = i * Mth.TWO_PI / 6f + 0.5f, gk = VfxAnim.easeOutBack(Mth.clamp((age - 6f - i * 1.4f) / 9f, 0f, 1f));
            if (gk < 0.02f) continue;
            float rr = R * (0.58f + 0.12f * h(inst, i, 2));
            Vector3f b0 = new Vector3f(C).add(Mth.cos(ang) * rr, 0f, Mth.sin(ang) * rr);
            thorn(buf, b0, new Vector3f(b0).add(0, 0.9f * hs * 0.6f * gk, 0), 0.22f, a(strap, f));
        }
        // roses blooming on the wall, one after another; a few are scarlet
        for (int k = 0; k < 10; k++) {
            float bloom = VfxAnim.easeOutBack(Mth.clamp((age - 9f - k * 2.6f) / 8f, 0f, 1f));
            if (bloom < 0.02f) continue;
            float ang = Mth.TWO_PI * (k + h(inst, k, 3) * 0.6f) / 10f + 0.3f, hf = 0.22f + 0.6f * h(inst, k, 4);
            Vector3f pt = new Vector3f(C).add(Mth.cos(ang) * (R - 0.2f), hs * hf, Mth.sin(ang) * (R - 0.2f));
            float sz = (0.36f + 0.18f * h(inst, k, 5)) * bloom;
            if (k < 5) buf.billboard(ctx, GLOWTEX, VfxBlend.ADD, pt, sz * 2.8f, 0f, a(roseColor(k) == RED ? RED : glow, f * (0.3f + 0.15f * pulse)));
            buf.billboard(ctx, ROSE, VfxBlend.ALPHA, pt, sz, 0.4f * k + age * 0.01f, a(roseColor(k), f));
        }
        // rising pollen
        for (int i = 0; i < 14; i++) {
            float ang = h(inst, i, 6) * Mth.TWO_PI, rr = R * 0.95f * Mth.sqrt(h(inst, i, 7));
            float k = (age * 0.018f * (0.6f + h(inst, i, 8)) + h(inst, i, 9)) % 1f;
            Vector3f pt = new Vector3f(C).add(Mth.cos(ang) * rr + 0.2f * Mth.sin(age * 0.1f + i), k * hs * 1.3f, Mth.sin(ang) * rr);
            buf.billboard(ctx, POLLEN, VfxBlend.ADD, pt, 0.16f + 0.08f * h(inst, i, 10), age * 0.1f + i, a(i % 3 == 0 ? GREEN : glow, f * Mth.sin(Mth.PI * k)));
        }
        // pulse rings rolling out
        for (int j = 0; j < 2; j++) {
            float ph = ((age + j * 13f) % 26f) / 26f;
            buf.plane(HALO, VfxBlend.ADD, g.lift(0.05f), R * (0.15f + 0.9f * VfxAnim.easeOutCubic(ph)), a(j == 0 ? glow : GREEN, f * (1f - ph) * 0.55f));
        }
    }

    // ------------------------------------------------------------------ FX3: Corpse-Hunting Briar Tree
    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(6, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, sc = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f C = ctx.rel(inst.from(ctx)), T = ctx.rel(inst.to(ctx));
        Vector3f hint = new Vector3f(T).sub(C);
        float hl = hint.length();
        if (hl > 0.05f) hint.div(hl); else hint.set(0, 0, 0);
        int strap = strapColor(inst), glow = glowColor(inst);
        float out = 1f - sm(0.55f, 1f, p);               // the whole tower fades over the last 45 %
        float flashK = Mth.clamp(1f - age / 7f, 0f, 1f);
        VfxPose g = VfxPose.ground(new Vector3f(C).add(0, 0.03f, 0));

        // 1. flash and thorn star
        if (flashK > 0.01f) {
            float e = VfxAnim.easeOutCubic(Mth.clamp(age / 4f, 0f, 1f));
            buf.billboard(ctx, BURST, VfxBlend.ADD, new Vector3f(C).add(0, 0.5f * sc, 0), (1.2f + 2.6f * e) * sc, age * 0.1f, a(VfxVertexBuffer.lerpColor(WHITE, glow, 0.3f), flashK));
            buf.billboard(ctx, GLOWTEX, VfxBlend.ADD, new Vector3f(C).add(0, 0.4f * sc, 0), 3.0f * sc * e, 0f, a(glow, flashK * 0.7f));
        }
        // 2. ground: sigil, shock rings
        buf.plane(SIGIL, VfxBlend.ALPHA, g.spin(age * 0.05f), 1.5f * sc * VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0f, 1f)), a(strap, out * 0.9f));
        buf.plane(SIGIL, VfxBlend.ADD, g.lift(0.01f).spin(-age * 0.08f), 1.6f * sc, a(glow, out * 0.35f));
        for (int j = 0; j < 2; j++) {
            float k = Mth.clamp((age - j * 3f) / 14f, 0f, 1f);
            if (k > 0f && k < 1f) buf.plane(HALO, VfxBlend.ADD, g.lift(0.04f), (0.4f + 2.6f * VfxAnim.easeOutCubic(k)) * sc, a(j == 0 ? glow : GREEN, (1f - k) * 0.7f));
        }

        // 3. the tower: a dark wide back layer for parallax, three stacked masses, braided columns
        float lean = 0.28f * sc;
        for (int k = 0; k < 2; k++) {
            float gk = VfxAnim.easeOutBack(Mth.clamp((age - 0.5f - k) / 8f, 0f, 1f));
            if (gk < 0.01f) continue;
            Vector3f b = new Vector3f(C).add(hint.x * 0.15f * sc * (k - 0.5f) + (k == 0 ? -0.5f : 0.5f) * sc, 0, hint.z * 0.15f * sc);
            standing(buf, ctx, TANGLE, VfxBlend.ALPHA, b, 2.6f * sc, 3.8f * sc * gk, k == 1, lean * 0.5f * gk, a(shade(strap, 0.62f), out));
        }
        float[] w = {2.5f, 1.9f, 1.35f}, hh = {2.3f, 2.0f, 1.6f}, y0 = {0f, 1.7f, 3.2f};
        for (int k = 0; k < 3; k++) {
            float gk = VfxAnim.easeOutBack(Mth.clamp((age - k * 1.6f) / 7f, 0f, 1f));
            if (gk < 0.01f) continue;
            float lk = lean * (0.3f + 0.7f * k) * gk;
            Vector3f b = new Vector3f(C).add(hint.x * 0.2f * y0[k] * sc, y0[k] * sc * gk, hint.z * 0.2f * y0[k] * sc);
            standing(buf, ctx, TANGLE, VfxBlend.ALPHA, b, w[k] * sc, hh[k] * sc * gk, (k & 1) == 0, lk * (h(inst, k, 20) > 0.5f ? 1 : -1), a(strap, out));
        }
        for (int k = 0; k < 3; k++) {
            float gk = VfxAnim.easeOutBack(Mth.clamp((age - 1f - k * 1.3f) / 9f, 0f, 1f));
            if (gk < 0.01f) continue;
            float ang = k * Mth.TWO_PI / 3f + 0.7f;
            Vector3f b = new Vector3f(C).add(Mth.cos(ang) * 0.85f * sc, 0, Mth.sin(ang) * 0.85f * sc);
            standing(buf, ctx, HELIX, VfxBlend.ALPHA, b, 0.7f * sc, 3.2f * sc * gk, k == 1, 0.15f * sc, a(strap, out));
        }

        // 4. strap ribbons whipping outwards
        for (int k = 0; k < 4; k++) {
            float ex = VfxAnim.easeOutCubic(Mth.clamp((age - k * 0.8f) / 9f, 0f, 1f));
            if (ex < 0.02f) continue;
            float ang = k * Mth.HALF_PI + h(inst, k, 21) * 0.8f + (hl > 0.05f ? Mth.atan2(hint.z, hint.x) * 0.3f : 0f);
            float reach = (1.6f + 0.9f * h(inst, k, 22)) * sc * ex, rise = (0.9f + 1.0f * h(inst, k, 23)) * sc;
            float swirl = 0.5f * Mth.sin(age * 0.2f + k);
            Vector3f prev = new Vector3f(C).add(0, 0.1f, 0);
            for (int i = 1; i <= 6; i++) {
                float s = i / 6f, aa = ang + swirl * s;
                Vector3f cur = new Vector3f(C).add(Mth.cos(aa) * reach * s, 0.1f + Mth.sin(s * Mth.PI * 0.85f) * rise * ex * (1f - 0.25f * s), Mth.sin(aa) * reach * s);
                float w0 = 0.3f * sc * (1.05f - 0.7f * (i - 1f) / 6f), w1 = 0.3f * sc * (1.05f - 0.7f * i / 6f);
                strip(buf, STRAP, VfxBlend.ALPHA, prev, cur, w0, w1, (i - 1f) * 1.2f + k, i * 1.2f + k, a(strap, out), a(strap, out * 0.85f));
                prev = cur;
            }
        }
        // 5. thorn shards flying out (biased by the hint)
        for (int i = 0; i < 8; i++) {
            Vector3f d = dirOf(h(inst, i, 24), h(inst, i, 25), true);
            d.add(hint.x * 0.5f, hint.y * 0.3f, hint.z * 0.5f).normalize();
            float e = VfxAnim.easeOutCubic(Mth.clamp(age / (D * 0.7f), 0f, 1f));
            float r0 = (0.5f + 2.3f * h(inst, i, 26)) * sc * e;
            Vector3f a0 = new Vector3f(C).add(d.x * r0, d.y * r0 - 0.9f * sc * e * e * (1f - 0.4f * d.y), d.z * r0);
            Vector3f b0 = new Vector3f(a0).add(d.x * 0.4f * sc, d.y * 0.4f * sc, d.z * 0.4f * sc);
            thorn(buf, a0, b0, 0.1f * sc, a(strap, out));
        }
        // 6. roses bloom on the tower, petals fall in the afterglow
        for (int k = 0; k < 6; k++) {
            float bloom = VfxAnim.easeOutBack(Mth.clamp((age - 7f - k * 1.5f) / 6f, 0f, 1f));
            if (bloom < 0.02f) continue;
            float ang = Mth.TWO_PI * k / 6f + 0.4f, yy = (0.4f + 2.2f * h(inst, k, 27)) * sc;
            Vector3f pt = new Vector3f(C).add(Mth.cos(ang) * (0.35f + 0.25f * h(inst, k, 28)) * sc, yy, Mth.sin(ang) * 0.5f * sc);
            // pushed toward the camera so it sits in front of the tower
            Vector3f toCam = new Vector3f(pt).negate();
            if (toCam.lengthSquared() > 1e-6f) pt.add(toCam.normalize().mul(0.5f * sc));
            float sz = (0.3f + 0.12f * h(inst, k, 29)) * sc * bloom;
            if (k < 3) buf.billboard(ctx, GLOWTEX, VfxBlend.ADD, pt, sz * 2.8f, 0f, a(roseColor(k) == RED ? RED : glow, out * 0.4f));
            buf.billboard(ctx, ROSE, VfxBlend.ALPHA, pt, sz, 0.5f * k, a(roseColor(k), out));
        }
        float fall = sm(0.3f, 1f, p);
        for (int i = 0; i < 10; i++) {
            float k = (fall * (0.7f + 0.6f * h(inst, i, 30)) + h(inst, i, 31) * 0.3f) % 1f;
            if (fall < 0.01f) break;
            float ang = h(inst, i, 32) * Mth.TWO_PI, rr = (0.3f + 1.8f * h(inst, i, 33)) * sc;
            Vector3f pt = new Vector3f(C).add(Mth.cos(ang) * rr + 0.25f * Mth.sin(age * 0.2f + i), (3.0f - 2.9f * k) * sc * (0.5f + 0.5f * h(inst, i, 34)), Mth.sin(ang) * rr);
            if (i < 6) buf.billboard(ctx, PETAL, VfxBlend.ALPHA, pt, 0.2f * sc, age * 0.25f + i * 1.3f, a(i % 3 == 2 ? RED : BLUE, out * Mth.sin(Mth.PI * k)));
            else buf.billboard(ctx, POLLEN, VfxBlend.ADD, pt, 0.2f * sc, age * 0.2f + i, a(i % 2 == 0 ? glow : GREEN, out * Mth.sin(Mth.PI * k)));
        }
    }
}
