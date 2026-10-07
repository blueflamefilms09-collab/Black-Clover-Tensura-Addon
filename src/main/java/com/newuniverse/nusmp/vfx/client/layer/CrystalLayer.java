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
 * Crystal Magic, drawn after the anime still of a giant curved fang of pale lavender-white translucent crystal: flat facet bands,
 * faint curved veins, a soft bright rim. Everything is lavender-white glass with a cool blue-cyan glow; the vertex colour
 * ({@code inst.color}) is mixed halfway into that palette, so a white tint still reads as crystal.
 * <ul>
 *   <li>CRYSTAL_FX1 (cast / projectile): Crystal Lance. A ring of facets converges on 'from' into a prism flare, then a hex-prism
 *       lance of glass streaks to 'to' with two ghost afterimages, a lattice trail and tumbling chips behind it, ending in a
 *       prism flash and a ring of glints at 'to'. power = size of the lance (length about 2.4 x power), duration = flight ticks.</li>
 *   <li>CRYSTAL_FX2 (zone / field / dome): Crystal Field. A cut-gem sigil on the ground (turning one way, a smaller one the other
 *       way), a pulse that rolls out from the centre, a rim wall of crystal teeth, a crown of curved fangs growing up around the
 *       edge and leaning in, motes of glint rising. 'from' = centre, power = radius, duration = life.</li>
 *   <li>CRYSTAL_FX3 (impact / burst / signature): Crystal Fang. A prism flash, a sigil shockwave on the ground, the big curved
 *       fang of the still erupting at 'from' (two smaller fangs beside it) with a glow ghost, twelve shards and chips thrown out,
 *       glints lingering as the afterglow. 'to - from' (horizontal part) leans the fangs and the debris that way. power = scale.</li>
 * </ul>
 */
public class CrystalLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation BLADE = t("crystal_blade");
    public static final ResourceLocation SHARD = t("crystal_shard");
    public static final ResourceLocation FLARE = t("crystal_flare");
    public static final ResourceLocation SIGIL = t("crystal_sigil");
    public static final ResourceLocation BAND = t("crystal_band");
    public static final ResourceLocation STREAK = t("crystal_streak");
    public static final ResourceLocation CHIP = t("crystal_chip");
    public static final ResourceLocation GLINT = t("crystal_glint");

    private static final int LAVENDER = 0xFFE4E0FF;
    private static final int GLOW = 0xFFA0E0FF;
    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CRYSTAL_FX1, VfxShape.CRYSTAL_FX2, VfxShape.CRYSTAL_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case CRYSTAL_FX1 -> 16;
            case CRYSTAL_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return GLOW; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case CRYSTAL_FX1 -> lance(inst, ctx, buf);
            case CRYSTAL_FX2 -> field(inst, ctx, buf);
            case CRYSTAL_FX3 -> fang(inst, ctx, buf);
            default -> { }
        }
    }

    private static int body(VfxInstance inst) { return VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, LAVENDER, 0.5f); }
    private static int glow(VfxInstance inst) { return VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, GLOW, 0.4f); }
    private static int a(int c, float f) { return VfxVertexBuffer.withAlpha(c, Mth.clamp(f, 0f, 1f)); }

    // ------------------------------------------------------------------ FX1: Crystal Lance
    private void lance(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), pw = Math.max(0.5f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 1e-3f) dir.set(0, 0, 1); else dir.div(len);
        int body = body(inst), glow = glow(inst);
        RandomSource r = inst.random();

        // charge: facets converge on the hand and flare
        float charge = Mth.clamp(p / 0.22f, 0, 1);
        float chargeFade = 1f - Mth.clamp((p - 0.22f) / 0.2f, 0, 1);
        if (chargeFade > 0.01f) {
            float k = VfxAnim.easeOutCubic(charge);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, from, (0.5f + 1.1f * k) * pw, age * 0.25f, a(WHITE, k * chargeFade));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, from, (0.3f + 0.7f * k) * pw, -age * 0.4f, a(glow, 0.8f * k * chargeFade));
            VfxBloom.glow(ctx, buf, from, 0.45f * pw * k, glow, 0.9f * chargeFade);
            for (int i = 0; i < 6; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, el = (r.nextFloat() - 0.5f) * 1.6f;
                float d = (1f - k) * 1.1f * pw + 0.1f;
                Vector3f o = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el), Mth.sin(ang) * Mth.cos(el)).mul(d).add(from);
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, o, 0.2f * pw, ang + age * 0.3f, a(body, 0.9f * charge * chargeFade));
            }
        }

        // flight
        float fq = Mth.clamp((p - 0.2f) / 0.58f, 0, 1);
        float qq = fq * (0.35f + 0.65f * fq);
        if (p >= 0.2f && fq < 1f) {
            Vector3f head = new Vector3f(dir).mul(len * qq).add(from);
            float L = 2.4f * pw, W = 0.55f * pw;
            // lattice trail from the head back toward the hand
            Vector3f tailEnd = new Vector3f(dir).mul(Math.max(0f, len * qq - 4.2f * pw)).add(from);
            buf.beam(ctx, STREAK, VfxBlend.ADD, head, tailEnd, 0.75f * pw, 0.15f * pw, 1, 0, a(glow, 0.85f), a(glow, 0.1f));
            // two ghost afterimages (wider, cooler, fading)
            for (int i = 2; i >= 1; i--) {
                float gq = Math.max(0f, qq - 0.075f * i);
                Vector3f gh = new Vector3f(dir).mul(len * gq).add(from);
                Vector3f gt = new Vector3f(gh).sub(new Vector3f(dir).mul(L));
                buf.beam(ctx, SHARD, VfxBlend.ADD, gt, gh, W * (1.2f + 0.25f * i), W * (1.2f + 0.25f * i), 1, 0, a(glow, 0.5f / i), a(glow, 0.5f / i));
            }
            // the lance
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(L));
            buf.beam(ctx, SHARD, VfxBlend.ALPHA, tail, head, W, W, 1, 0, a(body, 0.95f), a(body, 0.95f));
            buf.beam(ctx, SHARD, VfxBlend.ADD, tail, head, W * 0.55f, W * 0.55f, 1, 0, a(WHITE, 0.55f), a(WHITE, 0.55f));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 1.2f * pw, age * 0.35f, a(WHITE, 0.9f));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, head, 0.8f * pw, -age * 0.5f, a(glow, 0.7f));
            VfxBloom.glow(ctx, buf, head, 0.35f * pw, glow, 0.8f);
            // tumbling chips shed behind the lance
            for (int i = 0; i < 5; i++) {
                float ct = qq - 0.05f * (i + 1);
                float ox = (r.nextFloat() - 0.5f) * 0.7f * pw, oy = (r.nextFloat() - 0.5f) * 0.7f * pw, rot = r.nextFloat() * 6f;
                if (ct <= 0f) continue;
                Vector3f cp = new Vector3f(dir).mul(len * ct).add(from).add(ox, oy - (qq - ct) * 2.2f * pw, 0);
                buf.billboard(ctx, CHIP, VfxBlend.ALPHA, cp, 0.26f * pw * (1f - 0.12f * i), rot + age * 0.4f, a(body, 0.85f - 0.14f * i));
            }
        }

        // impact flash at 'to'
        if (p > 0.76f) {
            float k = Mth.clamp((p - 0.76f) / 0.24f, 0, 1), e = VfxAnim.easeOutCubic(k);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, to, (0.6f + 1.8f * e) * pw, age * 0.2f, a(WHITE, 1f - k));
            VfxBloom.glow(ctx, buf, to, 0.5f * pw * (0.5f + e), glow, 1f - k);
            for (int i = 0; i < 6; i++) {
                float ang = Mth.TWO_PI * i / 6 + 0.4f;
                Vector3f q = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang))).mul(0.9f * pw * e).add(to);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.45f * pw * (1f - 0.5f * k), ang + age * 0.2f, a(WHITE, 1f - k));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Crystal Field
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = Math.min(Mth.clamp(age / 8f, 0, 1), Mth.clamp((inst.duration - age) / 12f, 0, 1));
        if (fade <= 0.01f) return;
        float R = Math.max(1.5f, inst.power), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 14f, 0, 1));
        Vector3f c = ctx.rel(inst.from(ctx));
        int body = body(inst), glow = glow(inst);
        RandomSource r = inst.random();
        Vector3f floor = new Vector3f(c).add(0, 0.05f, 0);
        VfxPose pose = VfxPose.ground(floor);

        // floor: glow, the big sigil turning one way, a small one the other way, a pulse rolling outwards
        VfxBloom.planeGlow(buf, VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0)), R, glow, 0.45f * fade);
        buf.plane(SIGIL, VfxBlend.ADD, pose.lift(0.01f).spin(age * 0.012f), R * grow, a(VfxVertexBuffer.whiten(glow, 0.4f), 0.85f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, pose.lift(0.03f).spin(-age * 0.03f), R * 0.5f * grow, a(WHITE, 0.6f * fade));
        float pu = (age % 36f) / 36f;
        buf.plane(SIGIL, VfxBlend.ADD, pose.lift(0.02f).spin(pu), R * (0.15f + 0.85f * pu), a(glow, 0.5f * (1f - pu) * fade));

        // rim wall of crystal teeth, rising
        float wallH = (0.55f + 0.12f * R) * grow;
        int segs = ctx.seg(16, 10);
        float halfW = wallH * 0.5f;
        float repeats = Math.max(1, Math.round(Mth.TWO_PI * R / (4f * halfW * 2f)));
        hoop(buf, BAND, VfxBlend.ALPHA, new Vector3f(c).add(0, halfW, 0), R * 0.985f, halfW, segs, repeats, age * 0.004f, a(body, 0.8f * fade));

        // crown of curved fangs around the edge, leaning inwards, growing in a stagger
        Vector3f right = ground(ctx.camRight);
        int n = Mth.clamp(Math.round(R * 1.3f), 6, 9);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + r.nextFloat() * 0.3f, h = (1.4f + 0.38f * R) * (0.7f + 0.5f * r.nextFloat());
            float delay = 2f + r.nextFloat() * 8f, e = VfxAnim.easeOutBack(Mth.clamp((age - delay) / 12f, 0, 1));
            if (e <= 0.02f) continue;
            float rr = R * 0.93f;
            Vector3f base = new Vector3f(Mth.cos(ang) * rr, 0, Mth.sin(ang) * rr).add(c);
            Vector3f lean = new Vector3f(-Mth.cos(ang), 0, -Mth.sin(ang)).mul(0.18f * h);
            standFang(buf, BLADE, VfxBlend.ALPHA, base, right, h * 0.42f * e, h * e, lean, (i & 1) == 0, a(body, 0.92f * fade));
        }
        // a small inner cluster
        for (int i = 0; i < 3; i++) {
            float ang = i * Mth.TWO_PI / 3 + age * 0.004f + 0.5f, h = 0.7f + 0.2f * R;
            float e = VfxAnim.easeOutBack(Mth.clamp((age - 10f - i * 3f) / 12f, 0, 1));
            if (e <= 0.02f) continue;
            Vector3f base = new Vector3f(Mth.cos(ang) * R * 0.3f, 0, Mth.sin(ang) * R * 0.3f).add(c);
            standFang(buf, BLADE, VfxBlend.ALPHA, base, right, h * 0.42f * e, h * e, new Vector3f(), (i & 1) == 1, a(body, 0.85f * fade));
        }
        buf.billboard(ctx, FLARE, VfxBlend.ADD, new Vector3f(c).add(0, 0.5f, 0), (0.8f + 0.25f * VfxAnim.pulse(age, 1.2f)) * (0.6f + 0.2f * R), age * 0.05f, a(WHITE, 0.6f * fade));

        // glints rising from the floor
        for (int i = 0; i < 8; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.95f, ph = r.nextFloat(), sp = 0.012f + 0.01f * r.nextFloat();
            float rise = (age * sp + ph) % 1f;
            Vector3f q = new Vector3f(Mth.cos(ang) * d, 0.1f + rise * (1.6f + 0.2f * R), Mth.sin(ang) * d).add(c);
            float tw = Mth.sin(rise * Mth.PI);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.35f + 0.1f * R, age * 0.1f + ph * 6f, a(WHITE, tw * fade));
        }
    }

    // ------------------------------------------------------------------ FX3: Crystal Fang
    private void fang(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick), pw = Math.max(0.5f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        hint.y = 0;
        float hl = hint.length();
        if (hl > 1e-3f) hint.div(hl); else hint.set(0, 0, 0);
        hint.mul(Math.min(1f, hl / 3f));
        int body = body(inst), glow = glow(inst);
        RandomSource r = inst.random();
        float out = 1f - Mth.clamp((p - 0.62f) / 0.38f, 0, 1);

        // floor: shockwave sigil rolls out, a second one right behind it
        Vector3f floor = new Vector3f(c).add(0, 0.05f, 0);
        VfxPose pose = VfxPose.ground(floor);
        float e1 = VfxAnim.easeOutCubic(Mth.clamp(p / 0.55f, 0, 1));
        buf.plane(SIGIL, VfxBlend.ADD, pose.spin(age * 0.05f), (0.4f + 2.8f * e1) * pw, a(glow, 0.9f * (1f - e1 * 0.8f) * out));
        float e2 = VfxAnim.easeOutCubic(Mth.clamp((p - 0.1f) / 0.6f, 0, 1));
        if (p > 0.1f) buf.plane(SIGIL, VfxBlend.ADD, pose.lift(0.02f).spin(-age * 0.04f), (0.3f + 1.7f * e2) * pw, a(WHITE, 0.7f * (1f - e2) * out));

        // the fangs: a big one at the centre and a smaller pair beside it; the glow ghost sits just behind
        Vector3f right = ground(ctx.camRight);
        float rise = VfxAnim.easeOutCubic(Mth.clamp(p / 0.22f, 0, 1));
        float H = 3.6f * pw;
        float shatter = Mth.clamp((p - 0.6f) / 0.4f, 0, 1);
        float fa = 1f - shatter;
        Vector3f lean = new Vector3f(hint).mul(0.35f * H);
        if (fa > 0.02f) {
            standFang(buf, BLADE, VfxBlend.ADD, c, right, H * 0.46f * 1.15f * rise, H * 1.08f * rise, lean, false, a(glow, 0.45f * fa));
            standFang(buf, BLADE, VfxBlend.ALPHA, c, right, H * 0.46f * rise, H * rise, lean, false, a(body, 0.95f * fa));
            for (int s = -1; s <= 1; s += 2) {
                float e = VfxAnim.easeOutCubic(Mth.clamp((p - 0.04f) / 0.2f, 0, 1));
                Vector3f base = new Vector3f(right).mul(s * 0.9f * pw).add(c);
                Vector3f l2 = new Vector3f(lean).mul(0.6f).add(new Vector3f(right).mul(s * 0.35f * pw));
                standFang(buf, BLADE, VfxBlend.ALPHA, base, right, H * 0.26f * e, H * 0.58f * e, l2, s > 0, a(body, 0.9f * fa));
            }
        }

        // flash
        float fl = Mth.clamp(p / 0.12f, 0, 1) * (1f - Mth.clamp((p - 0.12f) / 0.4f, 0, 1));
        if (fl > 0.01f) {
            Vector3f mid = new Vector3f(c).add(0, 0.6f * pw, 0);
            buf.billboard(ctx, FLARE, VfxBlend.ADD, mid, (1.6f + 2.4f * VfxAnim.easeOutCubic(p / 0.5f > 1 ? 1 : p / 0.5f)) * pw, age * 0.15f, a(WHITE, fl));
            buf.billboard(ctx, FLARE, VfxBlend.ADD, mid, 1.8f * pw, -age * 0.25f, a(glow, 0.7f * fl));
            VfxBloom.glow(ctx, buf, mid, 0.9f * pw, glow, fl);
        }

        // twelve shards thrown out on arcs, then chips tumbling
        float tt = age / 10f;
        for (int i = 0; i < 12; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, el = 0.25f + r.nextFloat() * 0.85f, sp = (2.2f + 2.6f * r.nextFloat()) * pw;
            Vector3f v = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el), Mth.sin(ang) * Mth.cos(el)).mul(sp).add(new Vector3f(hint).mul(1.5f * pw));
            if (i >= 8) continue;   // shards 0..7 use the long streak, 8..11 are drawn as chips below
            Vector3f hd = pos(c, v, tt, 1.6f * pw), tl = pos(c, v, Math.max(0f, tt - 0.18f), 1.6f * pw);
            if (hd.y < c.y - 0.2f) continue;
            buf.beam(ctx, SHARD, VfxBlend.ALPHA, tl, hd, 0.3f * pw, 0.3f * pw, 1, 0, a(body, 0.95f * out), a(body, 0.95f * out));
        }
        for (int i = 0; i < 8; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, el = 0.1f + r.nextFloat() * 0.7f, sp = (1.2f + 2.0f * r.nextFloat()) * pw, rot = r.nextFloat() * 6f;
            Vector3f v = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el), Mth.sin(ang) * Mth.cos(el)).mul(sp).add(new Vector3f(hint).mul(pw));
            Vector3f q = pos(c, v, tt, 1.6f * pw);
            if (q.y < c.y - 0.3f) continue;
            buf.billboard(ctx, CHIP, VfxBlend.ALPHA, q, 0.24f * pw, rot + age * (0.3f + 0.3f * (i & 1)), a(body, 0.9f * out));
        }

        // afterglow: glints lingering above the spot, a soft glow in the middle
        float late = Mth.clamp((p - 0.3f) / 0.2f, 0, 1) * (1f - Mth.clamp((p - 0.85f) / 0.15f, 0, 1));
        if (late > 0.01f) {
            VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f * pw, 0), 0.6f * pw, glow, 0.5f * late);
            for (int i = 0; i < 6; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, d = (0.4f + r.nextFloat() * 1.8f) * pw, ph = r.nextFloat();
                Vector3f q = new Vector3f(Mth.cos(ang) * d, 0.3f * pw + (0.8f + ph) * pw * (p - 0.3f) * 1.6f, Mth.sin(ang) * d).add(c);
                float tw = Mth.sin(age * 0.5f + ph * 6f);
                buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.45f * pw, age * 0.1f, a(WHITE, late * (0.5f + 0.5f * tw * tw)));
            }
        }
    }

    // ------------------------------------------------------------------ primitives
    private static Vector3f pos(Vector3f c, Vector3f v, float t, float gravity) {
        return new Vector3f(c.x + v.x * t, c.y + v.y * t - gravity * t * t, c.z + v.z * t);
    }

    private static Vector3f ground(Vector3f v) {
        Vector3f h = new Vector3f(v.x, 0, v.z);
        return h.lengthSquared() < 1e-6f ? new Vector3f(1, 0, 0) : h.normalize();
    }

    /** A fang standing on {@code base}: upright quad turned around the vertical axis towards the camera, the tip shifted by {@code lean}. */
    private static void standFang(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, Vector3f right,
                             float width, float height, Vector3f lean, boolean mirror, int argb) {
        if (width <= 0.01f || height <= 0.01f) return;
        Vector3f w = new Vector3f(right).mul(width * 0.5f);
        Vector3f top = new Vector3f(base).add(0, height, 0).add(lean);
        int col = blend.grade(argb, 1f);
        float u0 = mirror ? 1 : 0, u1 = mirror ? 0 : 1;
        buf.quad(tex, blend, new Vector3f(base).sub(w), new Vector3f(base).add(w), new Vector3f(top).add(w), new Vector3f(top).sub(w),
                u0, 0, u1, 1, col, col);
    }

    /** A ribbon standing on a circle (a wall): width runs vertical, U wraps {@code repeats} times around. */
    private static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f centre, float radius, float halfH,
                             int segments, float repeats, float uScroll, int argb) {
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f m0 = new Vector3f(Mth.cos(a0) * radius, 0, Mth.sin(a0) * radius).add(centre);
            Vector3f m1 = new Vector3f(Mth.cos(a1) * radius, 0, Mth.sin(a1) * radius).add(centre);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, new Vector3f(m0).add(0, -halfH, 0), new Vector3f(m1).add(0, -halfH, 0),
                    new Vector3f(m1).add(0, halfH, 0), new Vector3f(m0).add(0, halfH, 0), u0, 0, u1, 1, col, col);
        }
    }
}
