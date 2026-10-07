package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Iron Magic, drawn like the owner's manga still of the Iron God of War: dull steel with heavy black ink outlines, rows of rivets and
 * studs, screentone dots, bold ink impact strokes, long speed lines and pale glassy shards. Colours: steel grey 0xB0B8C8, dark iron
 * 0x5A5E66, ink black, a cold white-blue heat and a few orange friction sparks. Three shapes, each with its own silhouette and texture set:
 * <ul>
 *   <li>IRON_FX1, CAST / PROJECTILE, "Iron Bullet". A spinning iron sigil and a contracting crown-halo gather at 'from' while iron
 *       chips are pulled in; then a riveted iron casing (the bullet) flies from 'from' to 'to' trailing a hanging chain, brushed
 *       speed lines, three ghost afterimages, a sigil disc spinning around its nose and shed shards, and detonates at 'to' in a
 *       white starburst with ink strokes, a screentone puff and glass-like shards. from = origin, to = target, power = size of the
 *       casing (1.0 = about 1.5 blocks long), duration = flight ticks (default 16: charge 0..22%, flight to 80%, impact to 100%).</li>
 *   <li>IRON_FX2, ZONE / FIELD, "Immovable Guardian Deity". A dark screentone shadow and a turning iron wheel (dharma wheel inside
 *       a chain band) lie on the ground inside a belt of bolted plates; a huge studded bell with a carved seated figure drops over
 *       the centre (translucent, so you see inside) in front of a slowly turning crown-halo, four chains tauten from the rim to its
 *       knob, iron spikes jut up around the edge, glints rise and a ring rolls out every 26 ticks. from = centre on the ground,
 *       power = RADIUS in blocks (the bell is about 1.8 radii wide), duration = life ticks (default 80; fades in over 8, out over 12).</li>
 *   <li>IRON_FX3, IMPACT / SIGNATURE, "Iron God of War". A crowned halo opens behind a studded gauntlet fist that rises, then slams
 *       down onto 'from' with speed lines and ghost fists; white flash, a manga starburst, bold ink strokes and screentone, a shock
 *       wheel and ink cracks in the ground, shards and orange friction sparks fly out, glints linger in the afterglow.
 *       from = centre, to = optional direction hint (to - from biases the flying shards and sparks; zero = all around), power = scale
 *       (0.6 to 3.0; 1.0 = a fist about 2.6 blocks tall), duration = life ticks (default 28; the hit lands at 30%).</li>
 * </ul>
 * inst.color is mixed 20 percent into the steel palette, so a white tint still reads as iron.
 */
public class IronLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation BULLET = t("iron_bullet");
    public static final ResourceLocation STREAK = t("iron_streak");
    public static final ResourceLocation BELL = t("iron_bell");
    public static final ResourceLocation RING = t("iron_ring");
    public static final ResourceLocation SIGIL = t("iron_sigil");
    public static final ResourceLocation SHARD = t("iron_shard");
    public static final ResourceLocation CHAIN = t("iron_chain");
    public static final ResourceLocation BURST = t("iron_burst");
    public static final ResourceLocation INK = t("iron_ink");
    public static final ResourceLocation FIST = t("iron_fist");
    public static final ResourceLocation HALO = t("iron_halo");
    public static final ResourceLocation TONE = t("iron_halftone");
    public static final ResourceLocation FLECKS = t("iron_flecks");

    private static final int STEEL = 0xFFB0B8C8;
    private static final int PLATE = 0xFF9AA3B2;
    private static final int INKC = 0xFF1B1E24;
    private static final int HOT = 0xFFE6F0FF;
    private static final int SPARKC = 0xFFFFB45A;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.IRON_FX1, VfxShape.IRON_FX2, VfxShape.IRON_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case IRON_FX1 -> 16;
            case IRON_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return STEEL; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case IRON_FX1 -> cast(inst, ctx, buf);
            case IRON_FX2 -> zone(inst, ctx, buf);
            case IRON_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small helpers
    private static int tint(VfxInstance inst) { return VfxVertexBuffer.lerpColor(STEEL, inst.color | 0xFF000000, 0.2f); }
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

    private static Vector3f dirOf(float u, float v, boolean up) {
        float z = up ? 0.15f + 0.85f * u : u * 2f - 1f;
        float s = Mth.sqrt(Math.max(0f, 1f - z * z)), ang = v * Mth.TWO_PI;
        return new Vector3f(Mth.cos(ang) * s, z, Mth.sin(ang) * s);
    }

    /** Camera-facing rectangle w x hgt with an optional sub-rectangle of the texture; rot spins it. */
    private static void bb(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float w, float hgt, float rot, int argb,
                           float u0, float v0, float u1, float v1) {
        float co = Mth.cos(rot), si = Mth.sin(rot);
        Vector3f rx = new Vector3f(ctx.camRight).mul(co * w * 0.5f).add(new Vector3f(ctx.camUp).mul(si * w * 0.5f));
        Vector3f ry = new Vector3f(ctx.camUp).mul(co * hgt * 0.5f).sub(new Vector3f(ctx.camRight).mul(si * hgt * 0.5f));
        int col = bl.grade(argb, 1f);
        buf.quad(tex, bl, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                u0, v0, u1, v1, col, col);
    }

    private static void bb(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float size, float rot, int argb) {
        bb(ctx, buf, tex, bl, c, size, size, rot, argb, 0, 0, 1, 1);
    }

    /** Ribbon from a (texture bottom) to b (texture top) facing the camera; the texture V repeats 'vRep' times along it. */
    private static void strut(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f a, Vector3f b, float halfW, int argb, float vRep,
                              float u0, float u1) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(halfW);
        int col = bl.grade(argb, 0.9f);
        buf.quad(tex, bl, new Vector3f(a).sub(side), new Vector3f(a).add(side), new Vector3f(b).add(side), new Vector3f(b).sub(side), u0, 1f - vRep, u1, 1f, col, col);
    }

    private static void strut(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f a, Vector3f b, float halfW, int argb) {
        strut(buf, tex, bl, a, b, halfW, argb, 1f, 0f, 1f);
    }

    /** One of the four shards of the atlas as a strut (tip at b). */
    private static void shardStrut(VfxVertexBuffer buf, int idx, Vector3f a, Vector3f b, float halfW, int argb) {
        strut(buf, SHARD, VfxBlend.ALPHA, a, b, halfW, argb, 1f, (idx & 3) * 0.25f, (idx & 3) * 0.25f + 0.25f);
    }

    // ------------------------------------------------------------------ FX1: Iron Bullet
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration);
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) { dir.set(0, 0, 1); dist = 0.05f; } else dir.mul(1f / dist);
        int col = tint(inst), hot = VfxVertexBuffer.whiten(col, 0.65f);

        float tl = 0.22f * d, ta = 0.8f * d;
        float s = Mth.clamp((age - tl) / (ta - tl), 0, 1), se = 0.5f * s + 0.5f * s * s;
        float ti = Mth.clamp((age - ta) / Math.max(1f, d - ta), 0, 1);
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);

        // ---- charge: crown halo contracts, sigil spins up, chips are drawn in
        float c = Mth.clamp(age / tl, 0, 1), cf = 1f - sm(tl, tl + 0.12f * d, age);
        if (cf > 0.01f) {
            float e = VfxAnim.easeOutCubic(c);
            Vector3f hand = new Vector3f(dir).mul(0.25f * P).add(from);
            bb(ctx, buf, HALO, VfxBlend.ADD, hand, P * Mth.lerp(e, 2.6f, 0.9f), age * 0.35f, a(col, 0.8f * c * cf));
            bb(ctx, buf, HALO, VfxBlend.ADD, hand, P * Mth.lerp(e, 1.5f, 0.5f), -age * 0.5f, a(hot, 0.7f * c * cf));
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.facing(new Vector3f(dir).mul(0.45f * P).add(from), dir).spin(age * 0.45f), P * (0.35f + 0.5f * e), a(col, 0.85f * cf));
            bb(ctx, buf, BURST, VfxBlend.ADD, hand, P * (0.2f + 1.0f * e * e), age * 0.2f, a(hot, 0.55f * c * cf));
            bb(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, hand, P * (0.4f + 1.1f * e), 0f, a(col, 0.6f * c * cf));
            bb(ctx, buf, FLECKS, VfxBlend.ADD, hand, P * 1.8f * (1.2f - 0.5f * e), age * 0.1f, a(hot, 0.7f * c * cf));
            for (int i = 0; i < 4; i++) {
                Vector3f u = dirOf(h(inst, i, 1), h(inst, i, 2), false);
                float rr = (1f - e) * (0.9f + 0.9f * h(inst, i, 3)) * P;
                Vector3f p = new Vector3f(u).mul(rr).add(hand);
                bb(ctx, buf, SHARD, VfxBlend.ALPHA, p, 0.22f * P, 0.4f * P, h(inst, i, 4) * 6f + age * 0.2f, a(PLATE, 0.9f * cf), (i & 3) * 0.25f, 0, (i & 3) * 0.25f + 0.25f, 1);
            }
        }

        // ---- flight
        if (age > tl * 0.85f && ti <= 0f) {
            float fin = sm(tl * 0.85f, tl + 1.2f, age);
            float len = Math.min(dist * se + 0.2f, 3.4f * P);
            Vector3f tail = new Vector3f(dir).mul(-len).add(head);
            // speed lines
            strut(buf, STREAK, VfxBlend.ADD, tail, new Vector3f(dir).mul(0.3f * P).add(head), 0.34f * P, a(hot, 0.85f * fin));
            strut(buf, STREAK, VfxBlend.ADD, new Vector3f(dir).mul(-len * 0.7f).add(head), head, 0.2f * P, a(HOT, 0.8f * fin), 1f, 0.2f, 0.8f);
            // hanging chain: dark, fading toward the tail
            float cl = Math.min(len, 2.6f * P), hw = 0.15f * P, rep = cl / (4f * hw);
            Vector3f mid = new Vector3f(dir).mul(-cl * 0.5f).add(head), end = new Vector3f(dir).mul(-cl).add(head);
            strut(buf, CHAIN, VfxBlend.ALPHA, mid, head, hw, a(PLATE, 0.9f * fin), rep * 0.5f, 0f, 1f);
            strut(buf, CHAIN, VfxBlend.ALPHA, end, mid, hw, a(PLATE, 0.35f * fin), rep * 0.5f, 0f, 1f);
            // ghost afterimages (pale, additive)
            for (int k = 1; k <= 3; k++) {
                Vector3f g = new Vector3f(dir).mul(-0.85f * P * k).add(head);
                strut(buf, BULLET, VfxBlend.ADD, new Vector3f(dir).mul(-1.5f * P).add(g), g, 0.32f * P, a(col, 0.42f / k * fin));
            }
            // the casing
            strut(buf, BULLET, VfxBlend.ALPHA, new Vector3f(dir).mul(-1.5f * P).add(head), head, 0.36f * P, a(VfxVertexBuffer.whiten(col, 0.2f), fin));
            // sigil disc spinning around the nose, glow
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.facing(new Vector3f(dir).mul(-0.1f * P).add(head), dir).spin(-age * 0.6f), 0.8f * P, a(col, 0.75f * fin));
            bb(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, head, 1.2f * P, 0f, a(hot, 0.5f * fin));
            // shed shards, falling behind
            for (int i = 0; i < 5; i++) {
                float lag = (0.5f + 0.55f * i) * P + h(inst, i, 11) * 0.3f;
                Vector3f p = new Vector3f(dir).mul(-lag).add(head);
                Vector3f o = dirOf(h(inst, i, 12), h(inst, i, 13), false).mul(0.25f * P * (0.4f + 0.2f * i));
                p.add(o).add(0, -0.012f * i * age, 0);
                bb(ctx, buf, SHARD, VfxBlend.ALPHA, p, 0.2f * P, 0.38f * P, age * (0.2f + 0.05f * i) + i, a(PLATE, 0.8f * fin * (1f - i * 0.12f)), (i & 3) * 0.25f, 0, (i & 3) * 0.25f + 0.25f, 1);
            }
        }

        // ---- impact at 'to'
        if (ti > 0f || age >= ta) {
            float e = VfxAnim.easeOutCubic(ti), fo = 1f - ti;
            bb(ctx, buf, TONE, VfxBlend.ALPHA, to, P * (1.2f + 2.4f * e), 0.4f, a(INKC, 0.55f * fo));
            bb(ctx, buf, INK, VfxBlend.ALPHA, to, P * (0.7f + 2.1f * e), h(inst, 0, 21) * 6f, a(INKC, 0.95f * fo * fo));
            bb(ctx, buf, BURST, VfxBlend.ADD, to, P * (0.9f + 2.8f * e), h(inst, 1, 21) * 6f, a(hot, 0.95f * fo));
            bb(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, to, P * (1.4f + 2.2f * e), 0f, a(col, 0.8f * fo * fo));
            bb(ctx, buf, HALO, VfxBlend.ADD, to, P * (0.5f + 2.4f * e), ti * 3f, a(col, 0.7f * fo));
            for (int i = 0; i < 6; i++) {
                Vector3f u = dirOf(h(inst, i, 31), h(inst, i, 32), false);
                Vector3f p0 = new Vector3f(u).mul(e * 1.5f * P * (0.6f + 0.6f * h(inst, i, 33))).add(to);
                Vector3f p1 = new Vector3f(u).mul(0.7f * P).add(p0);
                shardStrut(buf, i, p0, p1, 0.12f * P, a(PLATE, 0.95f * fo));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Immovable Guardian Deity
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Mth.clamp(inst.power, 1.5f, 24f);
        float L = life(inst, age, 8f, 12f);
        if (L <= 0.002f) return;
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int col = tint(inst), hot = VfxVertexBuffer.whiten(col, 0.6f);

        // ---- ground: screentone shadow, belt of plates, turning wheel
        buf.plane(TONE, VfxBlend.ALPHA, VfxPose.ground(new Vector3f(c0).add(0, 0.02f, 0)).spin(0.4f), R * 1.15f, a(INKC, 0.5f * L));
        VfxPose g = VfxPose.ground(new Vector3f(c0).add(0, 0.04f, 0));
        buf.plane(SIGIL, VfxBlend.ADD, g.spin(age * 0.012f), R, a(col, 0.8f * L));
        buf.plane(SIGIL, VfxBlend.ADD, g.lift(0.02f).spin(-age * 0.02f + 1f), R * 0.6f, a(hot, 0.5f * L));
        int plates = Math.max(8, Math.round(Mth.TWO_PI * R / 0.95f));
        buf.ring(RING, VfxBlend.ALPHA, g.lift(0.01f), R * 0.9f, R * 1.0f, ctx.seg(18, 10), plates / 8f, age * 0.003f, a(PLATE, 0.95f * L));
        // pulse ring every 26 ticks
        float ph = (age / 26f) % 1f;
        buf.plane(SIGIL, VfxBlend.ADD, g.lift(0.03f).spin(ph), R * (0.25f + 0.75f * VfxAnim.easeOutCubic(ph)), a(hot, 0.5f * (1f - ph) * L));

        // ---- halo behind the bell, bell drops with a bounce
        float fall = Mth.clamp(age / 7f, 0, 1), fe = VfxAnim.easeOutBack(fall);
        float bellW = 1.8f * R, drop = (1f - Math.min(1f, fe)) * R * 2.4f;
        float pul = 1f + 0.015f * Mth.sin(age * 0.5f);
        Vector3f bc = new Vector3f(c0).add(0, bellW * 0.5f + drop, 0);
        bb(ctx, buf, HALO, VfxBlend.ADD, new Vector3f(c0).add(0, bellW * 0.62f, 0), bellW * 1.45f, age * 0.012f, a(col, 0.42f * L));
        bb(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c0).add(0, bellW * 0.4f, 0), bellW * 1.3f, 0f, a(col, (0.14f + 0.06f * Mth.sin(age * 0.25f)) * L));

        // ---- chains from the rim to the bell knob, iron spikes around the rim
        Vector3f knob = new Vector3f(c0).add(0, bellW * 0.96f + drop, 0);
        float cw = Mth.clamp(0.05f * R, 0.07f, 0.2f);
        for (int i = 0; i < 4; i++) {
            float ang = Mth.HALF_PI * i + 0.7f + age * 0.002f;
            Vector3f foot = new Vector3f(c0).add(Mth.cos(ang) * R * 0.95f, 0.05f, Mth.sin(ang) * R * 0.95f);
            Vector3f top = new Vector3f(knob);
            float lenC = foot.distance(top);
            strut(buf, CHAIN, VfxBlend.ALPHA, foot, top, cw, a(PLATE, 0.8f * L * sm(0.1f, 0.4f, fall)), lenC / (4f * cw), 0f, 1f);
        }
        int nsp = ctx.seg(10, 6);
        for (int k = 0; k < nsp; k++) {
            float ang = Mth.TWO_PI * k / nsp + 0.2f;
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - 5f - k * 0.7f) / 9f, 0, 1));
            Vector3f foot = new Vector3f(c0).add(Mth.cos(ang) * R * 0.99f, 0.02f, Mth.sin(ang) * R * 0.99f);
            float hgt = R * (0.26f + 0.12f * h(inst, k, 41)) * grow;
            Vector3f tip = new Vector3f(foot).add(Mth.cos(ang) * hgt * 0.25f, hgt, Mth.sin(ang) * hgt * 0.25f);
            shardStrut(buf, k, foot, tip, Math.max(0.1f, 0.045f * R), a(PLATE, 0.95f * L));
        }

        // ---- the bell itself (translucent so the people inside stay visible)
        bb(ctx, buf, BELL, VfxBlend.ALPHA, bc, bellW * pul, 0f, a(VfxVertexBuffer.lerpColor(PLATE, col, 0.35f), 0.62f * L));
        // landing shock: white flash ring on the ground
        if (age < 16f) {
            float lt = Mth.clamp((age - 6f) / 10f, 0, 1);
            if (age > 6f) buf.plane(SIGIL, VfxBlend.ADD, g.lift(0.05f).spin(-lt), R * (0.3f + 1.0f * VfxAnim.easeOutCubic(lt)), a(HOT, 0.9f * (1f - lt)));
        }

        // ---- rising glints
        for (int i = 0; i < 8; i++) {
            float ang = h(inst, i, 51) * Mth.TWO_PI, rr = Mth.sqrt(h(inst, i, 52)) * R * 0.95f;
            float tt = (age * 0.025f + h(inst, i, 53)) % 1f;
            Vector3f p = new Vector3f(c0).add(Mth.cos(ang) * rr, 0.1f + tt * R * 1.1f, Mth.sin(ang) * rr);
            bb(ctx, buf, FLECKS, VfxBlend.ADD, p, 0.4f + 0.012f * R, age * 0.05f + i, a(hot, 0.9f * Mth.sin(tt * Mth.PI) * L));
        }
    }

    // ------------------------------------------------------------------ FX3: Iron God of War
    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(8f, inst.duration);
        float P = Mth.clamp(inst.power, 0.4f, 4f);
        Vector3f c0 = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c0);
        boolean hasHint = hint.lengthSquared() > 0.01f;
        if (hasHint) hint.normalize();
        int col = tint(inst), hot = VfxVertexBuffer.whiten(col, 0.65f);
        float th = 0.3f * d;
        float tp = Mth.clamp(age / th, 0, 1);
        float ti = Mth.clamp((age - th) / Math.max(1f, d - th), 0, 1), fo = 1f - ti, e = VfxAnim.easeOutCubic(ti);
        Vector3f ground = new Vector3f(c0).add(0, 0.05f, 0);

        // ---- crown halo behind (opens during the wind-up)
        float open = sm(0f, th, age);
        Vector3f hc = new Vector3f(c0).add(0, 1.5f * P + 0.4f * P * ti, 0);
        bb(ctx, buf, HALO, VfxBlend.ADD, hc, P * (2.2f + 3.0f * open + 1.2f * ti), age * 0.03f, a(col, 0.75f * open * (1f - ti * ti)));
        bb(ctx, buf, HALO, VfxBlend.ADD, hc, P * (1.3f + 1.6f * open), -age * 0.06f, a(hot, 0.5f * open * fo));

        // ---- the fist: anticipation (a small rise), then the slam
        float rise = tp < 0.22f ? -0.22f * Mth.sin(tp / 0.22f * Mth.HALF_PI) : -0.22f + 1.22f * VfxAnim.easeInCubic((tp - 0.22f) / 0.78f);
        float H = 4.4f * P * (1f - rise) * (1f - rise * 0.0f);
        if (age >= th) H = 0f;
        float fs = 2.6f * P * (1f + 0.06f * ti);
        Vector3f fc = new Vector3f(c0).add(0, fs * 0.5f + H, 0);
        float fvis = age < th ? 1f : Mth.clamp(1f - (age - th) / (0.45f * d), 0, 1);
        if (fvis > 0.01f) {
            if (age < th) {
                for (int k = 1; k <= 2; k++)
                    bb(ctx, buf, FIST, VfxBlend.ADD, new Vector3f(fc).add(0, 0.9f * P * k * tp, 0), fs, fs, Mth.PI, a(col, 0.28f / k * tp), 0, 0, 1, 1);
                strut(buf, STREAK, VfxBlend.ADD, new Vector3f(fc).add(0, 0.5f * fs, 0), new Vector3f(fc).add(0, 0.5f * fs + 4.6f * P * tp, 0), 0.5f * P, a(hot, 0.8f * tp));
                strut(buf, STREAK, VfxBlend.ADD, new Vector3f(fc).add(0.7f * P, 0.4f * fs, 0), new Vector3f(fc).add(0.7f * P, 0.4f * fs + 3.6f * P * tp, 0), 0.22f * P, a(col, 0.6f * tp), 1f, 0.2f, 0.8f);
                strut(buf, STREAK, VfxBlend.ADD, new Vector3f(fc).add(-0.7f * P, 0.4f * fs, 0), new Vector3f(fc).add(-0.7f * P, 0.4f * fs + 3.6f * P * tp, 0), 0.22f * P, a(col, 0.6f * tp), 1f, 0.2f, 0.8f);
            }
            bb(ctx, buf, FIST, VfxBlend.ALPHA, fc, fs, fs, Mth.PI, a(VfxVertexBuffer.whiten(PLATE, 0.1f), fvis), 0, 0, 1, 1);
        }

        // ---- the hit
        if (age >= th * 0.9f) {
            float hit = sm(th * 0.9f, th + 1f, age);
            bb(ctx, buf, TONE, VfxBlend.ALPHA, new Vector3f(c0).add(0, 0.5f * P, 0), P * (2.2f + 5.2f * e), 0.4f, a(INKC, 0.6f * fo * hit));
            bb(ctx, buf, INK, VfxBlend.ALPHA, new Vector3f(c0).add(0, 0.5f * P, 0), P * (1.4f + 4.4f * e), h(inst, 0, 61) * 6f, a(INKC, 0.95f * (1f - ti * ti) * hit));
            bb(ctx, buf, BURST, VfxBlend.ADD, new Vector3f(c0).add(0, 0.5f * P, 0), P * (1.8f + 5.2f * e), h(inst, 1, 61) * 6f, a(hot, 0.95f * fo * hit));
            bb(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c0).add(0, 0.4f * P, 0), P * (2.6f + 3.6f * e), 0f, a(col, 0.9f * fo * fo * hit));
            bb(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c0).add(0, 0.3f * P, 0), P * (1.0f + 1.4f * e), 0f, a(HOT, 1.0f * (1f - Math.min(1f, ti * 2.5f)) * hit));
            // ground: shock wheel, cracks
            buf.plane(INK, VfxBlend.ALPHA, VfxPose.ground(ground).spin(h(inst, 2, 61) * 6f), P * (0.7f + 2.6f * e), a(INKC, 0.9f * (1f - 0.6f * ti) * hit));
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.02f, 0)).spin(ti * 1.5f), P * (0.6f + 3.4f * e), a(col, 0.85f * fo * hit));
            buf.plane(HALO, VfxBlend.ADD, VfxPose.ground(new Vector3f(ground).add(0, 0.03f, 0)).spin(-ti * 2f), P * (0.4f + 4.2f * e), a(hot, 0.6f * fo * fo * hit));
            // shards and friction sparks fly out
            for (int i = 0; i < 12; i++) {
                Vector3f u = dirOf(h(inst, i, 71), h(inst, i, 72), true);
                if (hasHint) u.mul(0.55f).add(new Vector3f(hint).mul(0.6f)).normalize();
                float sp = (1.3f + 1.6f * h(inst, i, 73)) * P;
                Vector3f p0 = new Vector3f(u).mul(sp * e + 0.2f * P).add(c0).add(0, 0.3f * P - 1.2f * P * ti * ti * (0.5f + h(inst, i, 74)), 0);
                Vector3f p1 = new Vector3f(u).mul((0.55f + 0.4f * h(inst, i, 75)) * P).add(p0);
                shardStrut(buf, i, p0, p1, 0.13f * P, a(PLATE, (0.98f - 0.9f * ti * ti) * hit));
            }
            for (int i = 0; i < 6; i++) {
                Vector3f u = dirOf(h(inst, i, 81), h(inst, i, 82), true);
                if (hasHint) u.mul(0.5f).add(new Vector3f(hint).mul(0.7f)).normalize();
                float sp = (2.4f + 2.4f * h(inst, i, 83)) * P;
                Vector3f p0 = new Vector3f(u).mul(sp * e + 0.2f * P).add(c0).add(0, 0.2f * P, 0);
                Vector3f p1 = new Vector3f(u).mul((0.9f + 0.7f * (1f - ti)) * P).add(p0);
                strut(buf, STREAK, VfxBlend.ADD, p0, p1, 0.07f * P, a(SPARKC, fo * hit));
            }
            // afterglow: lingering glints
            bb(ctx, buf, FLECKS, VfxBlend.ADD, new Vector3f(c0).add(0, 0.8f * P + 0.6f * P * ti, 0), P * (3.0f + 1.2f * ti), age * 0.04f, a(hot, 0.9f * Math.min(1f, ti * 3f) * fo * hit));
        }
    }
}
