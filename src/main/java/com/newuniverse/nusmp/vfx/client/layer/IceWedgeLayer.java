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
 * Ice Wedge Magic (Black Clover, Ice Wedge: driven-in wedges of ice), drawn after the owner's anime still: ice that is cut into chunky,
 * chiselled WEDGES with a bright cyan-white facet, a patch of deeper blue inside, a dark blue outline and a white-hot rim glow; a tall flat
 * FAN of wedges radiating from a bright spine, and a crown of long spikes. Everything is tinted from {@code inst.color} but mixed
 * with the magic's own palette (pale cyan, deep blue, white), so it reads as ice even when the tint is white.
 * <ul>
 *   <li>ICE_WEDGE_FX1 (cast / projectile): "Ice Wedge Lance". A small crown of three wedges gathers at {@code from} with a frost glint over
 *       the first quarter, then a chiselled wedge (flanked by two smaller ones, ghost afterimages behind it) drives along the line
 *       {@code from} -> {@code to} on a crystalline streak with snow sparkles falling off the trail, and bites into {@code to} with a
 *       glint, a spiked shock ring and flying shards. power = size (1.0 normal), duration = flight ticks (default 16).</li>
 *   <li>ICE_WEDGE_FX2 (zone / field): "Wedge Field". A ground sigil (rings, inward wedge teeth, a hexagram of wedges, a snowflake) turns
 *       slowly with a counter-turning bright copy, ice cracks spread across the floor, spiked shock rings pulse outwards, and a wall of
 *       upright fans of wedges (the still's leaf-shaped fan) stands around the rim with short spikes between them and a crown of spikes
 *       in the middle; snow motes rise. {@code from} = centre on the ground, power = radius, duration = life (default 80), fades in 8
 *       and out 12 ticks.</li>
 *   <li>ICE_WEDGE_FX3 (impact / signature): "Wedge Burst". A white flash and a rotating frost glint, a big fan of wedges erupting along the
 *       {@code to - from} direction (up when zero) like the fan in the still, a sunburst of twelve wedges flying outwards, shock rings,
 *       tumbling shards with gravity, a crack-marked frost disc and snow that lingers. power = scale, duration = life (default 28).</li>
 * </ul>
 */
public class IceWedgeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation FAN = t("ice_wedge_fan");
    public static final ResourceLocation SPIKE = t("ice_wedge_spike");
    public static final ResourceLocation CLUSTER = t("ice_wedge_cluster");
    public static final ResourceLocation SHARD = t("ice_wedge_shard");
    public static final ResourceLocation SIGIL = t("ice_wedge_sigil");
    public static final ResourceLocation CRACK = t("ice_wedge_crack");
    public static final ResourceLocation RING = t("ice_wedge_ring");
    public static final ResourceLocation SNOW = t("ice_wedge_snow");
    public static final ResourceLocation GLINT = t("ice_wedge_glint");
    public static final ResourceLocation STREAK = t("ice_wedge_streak");

    private static final int BASE = 0xFFA6E2FF;
    private static final int DEEP = 0xFF3C78D8;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ICE_WEDGE_FX1, VfxShape.ICE_WEDGE_FX2, VfxShape.ICE_WEDGE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case ICE_WEDGE_FX1 -> 16;
            case ICE_WEDGE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFC0F0FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case ICE_WEDGE_FX1 -> cast(inst, ctx, buf);
            case ICE_WEDGE_FX2 -> field(inst, ctx, buf);
            case ICE_WEDGE_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ palette and small helpers
    private static int body(VfxInstance inst) { return VfxVertexBuffer.lerpColor(BASE, inst.color | 0xFF000000, 0.35f) | 0xFF000000; }
    private static int deep(int body) { return VfxVertexBuffer.lerpColor(DEEP, body, 0.25f) | 0xFF000000; }
    private static int pale(int body) { return VfxVertexBuffer.whiten(body, 0.75f); }
    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float sat(float v) { return Mth.clamp(v, 0f, 1f); }

    /** Chiselled wedge standing between two points (a camera-facing ribbon, the texture's tip at {@code tip}). width = visible wedge width. */
    private static void wedge(VfxRenderContext ctx, VfxVertexBuffer buf, VfxBlend blend, Vector3f base, Vector3f tip, float width, int argb) {
        float w = width / 0.46f;     // the spike sprite fills the middle 46 % of its width
        buf.beam(ctx, SPIKE, blend, base, tip, w, w, 1, 0, argb, argb);
    }

    /** A spike lying on the screen plane: base at the centre-offset, tip at angle theta (screen radians), length len. */
    private static void screenSpike(VfxRenderContext ctx, VfxVertexBuffer buf, VfxBlend blend, Vector3f c, float r0, float theta, float len, int argb) {
        Vector3f d = new Vector3f(ctx.camRight).mul(Mth.cos(theta)).add(new Vector3f(ctx.camUp).mul(Mth.sin(theta)));
        Vector3f centre = new Vector3f(c).add(d.mul(r0 + len * 0.5f));
        buf.billboard(ctx, SPIKE, blend, centre, len, theta - Mth.HALF_PI, argb);
    }

    /** Upright fan of wedges standing at ground point {@code base}, width along {@code tangent}. */
    private static void standingFan(VfxVertexBuffer buf, VfxBlend blend, Vector3f base, Vector3f tangent, float height, float halfWidth, int argb) {
        Vector3f l = new Vector3f(base).sub(new Vector3f(tangent).mul(halfWidth));
        Vector3f r = new Vector3f(base).add(new Vector3f(tangent).mul(halfWidth));
        int c = blend.grade(argb, 1f);
        buf.quad(FAN, blend, l, r, new Vector3f(r).add(0, height, 0), new Vector3f(l).add(0, height, 0), 0, 0, 1, 1, c, c);
    }

    private static void sparkle(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p, float size, float rot, int argb) {
        buf.billboard(ctx, SNOW, VfxBlend.ADD, p, size, rot, argb);
    }

    // ------------------------------------------------------------------ FX1: the wedge lance
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float t = sat(age / dur);
        float sc = Mth.clamp(inst.power, 0.6f, 3f);
        int body = body(inst), deep = deep(body), pale = pale(body);
        RandomSource r = inst.random();
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        if (dist < 0.05f) { dir.set(0, 0, 1); dist = 0.05f; } else dir.div(dist);
        Vector3f lat = new Vector3f(ctx.camRight).sub(new Vector3f(dir).mul(ctx.camRight.dot(dir)));
        if (lat.lengthSquared() < 1e-4f) lat.set(ctx.camUp); else lat.normalize();

        float q = 0.25f, landing = 0.8f;
        float charge = sat(t / q);
        float chargeFade = t < q ? 1f : sat(1f - (t - q) / 0.12f);
        float flight = sat((t - q) / (landing - q));
        float e = 0.3f * flight + 0.7f * flight * flight * flight;      // a driven wedge starts slow and bites in fast
        float imp = sat((t - landing) / (1f - landing));
        Vector3f head = new Vector3f(from).lerp(to, e);

        // --- charge: three wedges closing in on the hand, a glow and a spinning frost glint
        if (chargeFade > 0.01f) {
            float ease = VfxAnim.easeOutCubic(charge);
            VfxBloom.glow(ctx, buf, from, 0.35f * sc * (0.4f + ease), body, chargeFade * (0.5f + 0.5f * ease));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, from, 1.4f * sc * (0.3f + ease), age * 0.15f, a(WHITE, chargeFade * ease));
            for (int k = 0; k < 3; k++) {
                float ang = Mth.TWO_PI * k / 3f + age * 0.18f;
                float rad = (1f - ease) * 0.9f * sc + 0.08f * sc;
                Vector3f o = new Vector3f(ctx.camRight).mul(Mth.cos(ang) * rad).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * rad)).add(from);
                wedge(ctx, buf, VfxBlend.ALPHA, new Vector3f(o).sub(new Vector3f(dir).mul(0.35f * sc)), new Vector3f(o).add(new Vector3f(dir).mul(0.45f * sc * ease)),
                        0.14f * sc, a(body, chargeFade * ease));
            }
        }

        if (flight > 0.001f || imp > 0f) {
            float travel = e * dist;
            float lance = 1.6f * sc * sat(flight * 6f + imp);
            float wide = 0.4f * sc;
            float gone = 1f - imp * imp;                                  // the wedge stays driven in and melts into the flash
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(Math.min(travel, 5f * sc) + lance * 0.5f));

            // streak of cold light behind the wedge, then ghost afterimages
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, new Vector3f(head).sub(new Vector3f(dir).mul(lance * 0.3f)), 0.05f * sc, 0.9f * sc, 1, 0,
                    a(deep, 0f), a(body, 0.8f * gone * sat(flight * 4f)));
            for (int k = 1; k <= 3; k++) {
                Vector3f h = new Vector3f(head).sub(new Vector3f(dir).mul(k * 0.7f * sc * sat(flight * 3f)));
                wedge(ctx, buf, VfxBlend.ADD, new Vector3f(h).sub(new Vector3f(dir).mul(lance)), h, wide * (1.1f + 0.1f * k), a(pale, (0.26f - 0.07f * k) * gone));
            }
            // two smaller wedges flanking the main one, trailing a little
            for (int s = -1; s <= 1; s += 2) {
                Vector3f h = new Vector3f(head).sub(new Vector3f(dir).mul(0.55f * sc)).add(new Vector3f(lat).mul(s * 0.42f * sc * (0.6f + 0.4f * flight)));
                wedge(ctx, buf, VfxBlend.ALPHA, new Vector3f(h).sub(new Vector3f(dir).mul(lance * 0.6f)), h, wide * 0.55f, a(deep, gone));
            }
            // the main wedge: dark-blue body, lit overlay, white-hot core
            wedge(ctx, buf, VfxBlend.ALPHA, new Vector3f(head).sub(new Vector3f(dir).mul(lance)), head, wide, a(body, gone));
            wedge(ctx, buf, VfxBlend.ADD, new Vector3f(head).sub(new Vector3f(dir).mul(lance * 1.03f)), new Vector3f(head).add(new Vector3f(dir).mul(0.06f * sc)),
                    wide * 1.35f, a(deep, 0.32f * gone));
            VfxBloom.glow(ctx, buf, new Vector3f(head).sub(new Vector3f(dir).mul(0.1f * sc)), 0.3f * sc, body, 0.85f * gone);

            // snow sparkles dropping off the trail
            int n = ctx.seg(8, 4);
            for (int i = 0; i < n; i++) {
                float f = r.nextFloat(), ph = r.nextFloat() * 6.28f, sz = 0.14f + r.nextFloat() * 0.18f;
                if (f > e) continue;
                Vector3f p = new Vector3f(from).lerp(head, f).add(0, -0.15f * sc * (e - f) * 6f, 0).add(new Vector3f(lat).mul((r.nextFloat() - 0.5f) * 0.5f * sc));
                float tw = Mth.sin(age * 0.7f + ph);
                tw = Math.max(0f, tw);
                sparkle(ctx, buf, p, sz * sc, ph + age * 0.05f, a(pale, 0.85f * tw * (1f - f * 0.5f) * gone));
            }
        }

        // --- impact: glint, a spiked ring, shards
        if (imp > 0f) {
            float fl = 1f - imp;
            Vector3f at = to;
            VfxBloom.glow(ctx, buf, at, 0.6f * sc * (0.6f + imp), body, fl * 1.1f);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, at, sc * (1.0f + 1.8f * imp), age * 0.2f, a(WHITE, fl));
            buf.billboard(ctx, RING, VfxBlend.ADD, at, sc * (0.5f + 2.2f * VfxAnim.easeOutCubic(imp)), age * 0.04f, a(body, fl * 0.9f));
            for (int k = 0; k < 6; k++) {
                float ang = Mth.TWO_PI * (k + r.nextFloat() * 0.6f) / 6f;
                float len = 0.5f * sc * (0.7f + 0.6f * r.nextFloat());
                screenSpike(ctx, buf, VfxBlend.ALPHA, at, 0.2f * sc * imp, ang, len * VfxAnim.easeOutCubic(sat(imp * 3f)), a(body, fl));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: the wedge field
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float fade = Math.min(sat(age / 8f), sat((dur - age) / 12f));
        float R = Math.max(0.6f, inst.power);
        if (fade <= 0.001f) return;
        int body = body(inst), deep = deep(body), pale = pale(body);
        RandomSource r = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose floor = VfxPose.ground(new Vector3f(c).add(0, 0.04f, 0));
        float open = VfxAnim.easeOutCubic(sat(age / 14f));
        float seedRot = r.nextFloat() * Mth.TWO_PI;

        // floor: cold light, cracks spreading, the sigil turning against its bright copy, pulses running out to the rim
        VfxBloom.planeGlow(buf, floor, R, body, 0.55f * fade);
        buf.plane(CRACK, VfxBlend.ALPHA, floor.lift(0.01f).spin(seedRot), R * 1.55f * open, a(deep, 0.85f * fade));
        buf.plane(SIGIL, VfxBlend.ALPHA, floor.lift(0.02f).spin(age * 0.012f), R * 1.03f * open, a(body, 0.92f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, floor.lift(0.03f).spin(-age * 0.025f + 1f), R * 1.03f * open, a(pale, 0.4f * fade));
        for (int k = 0; k < 2; k++) {
            float s = ((age + k * 20f) % 40f) / 40f;
            buf.plane(RING, VfxBlend.ADD, floor.lift(0.05f), R * (0.25f + 0.78f * s) / 0.42f, a(body, (1f - s) * 0.8f * fade));
        }

        // the wall: upright fans of wedges around the rim, a spike between each pair, stronger light on the fans
        int n = Mth.clamp(Math.round(R * 2.2f), 6, 12);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + seedRot * 0.3f;
            float hs = 0.82f + 0.4f * r.nextFloat();
            float grow = VfxAnim.easeOutBack(sat((age - 2f - i * 0.8f) / 9f));
            if (grow > 0.01f) {
                float h = (1.3f + 0.4f * Mth.sqrt(R)) * hs * grow;
                Vector3f p = new Vector3f(c).add(Mth.cos(ang) * R * 0.94f, 0.02f, Mth.sin(ang) * R * 0.94f);
                Vector3f tan = new Vector3f(-Mth.sin(ang), 0, Mth.cos(ang));
                float shine = 0.18f + 0.14f * Mth.sin(age * 0.25f + i * 1.7f);
                standingFan(buf, VfxBlend.ALPHA, p, tan, h, h * 0.36f, a(body, fade));
                standingFan(buf, VfxBlend.ADD, p, tan, h, h * 0.4f, a(deep, shine * fade));
            }
            // a short wedge between two fans, driven in at a slant towards the centre
            float g2 = VfxAnim.easeOutCubic(sat((age - 5f - i * 0.8f) / 7f));
            if (g2 > 0.01f && buf.hasBudget(4)) {
                float ang2 = ang + Mth.PI / n;
                Vector3f b = new Vector3f(c).add(Mth.cos(ang2) * R * 0.8f, 0, Mth.sin(ang2) * R * 0.8f);
                Vector3f tip = new Vector3f(b).add(-Mth.cos(ang2) * 0.25f, (0.7f + 0.2f * R * 0.1f) * g2, -Mth.sin(ang2) * 0.25f);
                wedge(ctx, buf, VfxBlend.ALPHA, b, tip, 0.22f + 0.02f * R, a(deep, fade));
            }
        }

        // the crown of spikes in the middle, light behind it
        float cs = (1.2f + 0.35f * R) * VfxAnim.easeOutBack(sat((age - 4f) / 12f));
        if (cs > 0.05f) {
            Vector3f cc = new Vector3f(c).add(0, cs * 0.5f - 0.02f, 0);
            buf.billboard(ctx, CLUSTER, VfxBlend.ALPHA, cc, cs, 0, a(body, fade));
            buf.billboard(ctx, CLUSTER, VfxBlend.ADD, cc, cs * 1.06f, 0, a(deep, 0.28f * fade));
        }

        // snow motes rising through the field
        int m = ctx.seg(10, 4);
        for (int i = 0; i < m; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = Mth.sqrt(r.nextFloat()) * R * 0.9f, sp = 0.012f + r.nextFloat() * 0.02f;
            float ph = r.nextFloat(), top = 1.4f + R * 0.18f;
            float y = ((age * sp + ph) % 1f) * top;
            float env = Mth.sin(((age * sp + ph) % 1f) * Mth.PI);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang + age * 0.01f) * rad, y + 0.1f, Mth.sin(ang + age * 0.01f) * rad);
            sparkle(ctx, buf, p, 0.18f + 0.12f * r.nextFloat(), age * 0.04f + i, a(pale, env * 0.9f * fade));
        }
    }

    // ------------------------------------------------------------------ FX3: the wedge burst
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float t = sat(age / dur);
        float sc = Mth.clamp(inst.power, 0.5f, 4f);
        int body = body(inst), deep = deep(body), pale = pale(body);
        RandomSource r = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        Vector3f dir = hint.lengthSquared() < 0.0025f ? new Vector3f(0, 1, 0) : hint.normalize();
        float late = sat((1f - t) / 0.4f);                     // everything solid leaves in the last 40 %

        // floor first: a frost disc with cracks that outlives the burst
        VfxPose floor = VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0));
        float disc = VfxAnim.easeOutCubic(sat(t * 5f));
        float seedRot = r.nextFloat() * Mth.TWO_PI;
        buf.plane(CRACK, VfxBlend.ALPHA, floor.spin(seedRot), sc * 1.9f * disc, a(deep, 0.9f * sat((1f - t) / 0.55f)));
        buf.plane(GLINT, VfxBlend.ADD, floor.lift(0.01f), sc * 1.8f * disc, a(body, 0.55f * sat((1f - t) / 0.5f)));

        // the sunburst: twelve wedges flung outward, long and short ones alternating
        float outT = VfxAnim.easeOutCubic(sat(t / 0.55f));
        float burstFade = sat((0.85f - t) / 0.35f);
        for (int i = 0; i < 12; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * r.nextFloat()) / 12f + 0.2f;
            float len = sc * ((i & 1) == 0 ? 1.5f : 0.95f) * (0.85f + 0.3f * r.nextFloat()) * VfxAnim.easeOutBack(sat(t / 0.14f));
            screenSpike(ctx, buf, VfxBlend.ALPHA, c, sc * (0.2f + 1.5f * outT), ang, len, a(i % 3 == 0 ? pale : body, burstFade));
        }

        // the signature: the big fan of wedges standing up along the hint direction
        float fe = VfxAnim.easeOutBack(sat(t / 0.18f));
        float fanSize = sc * 3.1f * fe;
        if (fanSize > 0.05f) {
            float ax = dir.dot(ctx.camRight), ay = dir.dot(ctx.camUp);
            float theta = (ax * ax + ay * ay < 0.01f) ? Mth.HALF_PI : (float) Math.atan2(ay, ax);
            Vector3f dscr = new Vector3f(ctx.camRight).mul(Mth.cos(theta)).add(new Vector3f(ctx.camUp).mul(Mth.sin(theta)));
            Vector3f fc = new Vector3f(c).add(dscr.mul(fanSize * 0.32f));
            float fa = sat((0.9f - t) / 0.4f);
            buf.billboard(ctx, FAN, VfxBlend.ALPHA, fc, fanSize, theta - Mth.HALF_PI, a(body, fa));
            buf.billboard(ctx, FAN, VfxBlend.ADD, fc, fanSize * 1.05f, theta - Mth.HALF_PI, a(deep, 0.35f * fa));
        }

        // flash and rings: the first few ticks
        float flash = sat(1f - t / 0.3f);
        if (flash > 0.01f) {
            VfxBloom.glow(ctx, buf, c, sc * (0.9f + 1.6f * t * 3f), body, flash * 1.2f);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, c, sc * 4.4f * (0.5f + 0.5f * VfxAnim.easeOutCubic(sat(t * 4f))), age * 0.12f, a(WHITE, flash));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, c, sc * 2.6f, -age * 0.2f + 0.8f, a(pale, flash * 0.8f));
        }
        float ringT = VfxAnim.easeOutCubic(sat(t * 1.3f));
        buf.billboard(ctx, RING, VfxBlend.ADD, c, sc * (0.6f + 4.2f * ringT), age * 0.03f, a(body, sat(1f - t * 1.4f)));
        buf.plane(RING, VfxBlend.ADD, floor.lift(0.02f), sc * (0.5f + 3.6f * VfxAnim.easeOutCubic(sat(t * 1.1f))) / 0.42f, a(pale, 0.8f * sat(1f - t * 1.2f)));

        // shards tumbling out under gravity
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float yaw = r.nextFloat() * Mth.TWO_PI, pitch = (r.nextFloat() - 0.2f) * 1.2f, sp = (0.9f + 1.4f * r.nextFloat()) * sc;
            Vector3f v = new Vector3f(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch)).mul(sp).add(new Vector3f(dir).mul(0.5f * sc));
            float tt = age / 20f;
            float drag = 1f - 0.5f * t;
            Vector3f p = new Vector3f(c).add(new Vector3f(v).mul(tt * drag)).add(0, -2.2f * tt * tt * sc * 0.6f, 0);
            float size = (0.2f + 0.2f * r.nextFloat()) * sc;
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, p, size, age * (0.2f + r.nextFloat() * 0.3f) + i, a(i % 2 == 0 ? body : pale, late));
        }

        // snow that stays behind
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = (0.3f + 1.6f * r.nextFloat()) * sc, ph = r.nextFloat() * 6.28f;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad * (0.4f + 0.6f * outT), 0.2f * sc + 0.6f * sc * t * (0.5f + r.nextFloat()) - 0.3f * sc * t * t, Mth.sin(ang) * rad * (0.4f + 0.6f * outT));
            float tw = Math.max(0f, Mth.sin(age * 0.6f + ph));
            sparkle(ctx, buf, p, (0.16f + 0.14f * r.nextFloat()) * sc, ph + age * 0.05f, a(pale, tw * sat(t * 6f) * late));
        }
    }
}
