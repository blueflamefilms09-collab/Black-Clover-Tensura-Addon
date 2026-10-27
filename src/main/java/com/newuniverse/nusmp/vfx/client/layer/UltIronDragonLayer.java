package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0: the unique Ultimate effect ULT_IRON_DRAGON, Gajeel Redfox's "Iron Shadow Dragon Mode" (Fairy Tail). Iron scales and living shadow fuse; a huge blow to every foe
 * around, the rust cashed in. Palette: gunmetal grey, dull steel, black shadow with violet-white edge glints, rust orange sparks.
 * <ul>
 *   <li>ticks 0-6 (build-up): jagged iron plates crawl up out of the ground in two ranks round the caster, a black shadow pool spreads under the feet, and a coiling dragon body
 *       (a strip of iron scales over a wider strip of living shadow) winds up round the caster in a helix, its head rising at the end of it.</li>
 *   <li>ticks 6-12 (peak): the iron dragon head snaps outward with the neck stretching after it, jaws wide, a roar-streak out of its mouth; a ray flash and a rivet-plated
 *       shock ring run across the ground to the rim, rust-orange cracks flare, a ring of iron shards flies outward.</li>
 *   <li>ticks 12-20 (aftermath): the iron rusts (grey to brown), the plates sink, the body and head dissolve into the shadow, rust flakes fall and sparks fade.</li>
 * </ul>
 * Cost (worst case): about 380 vertices of 400 (95 quads), nothing allocated per quad, all counts constant (scaled down by the particle setting), deterministic from the seed and
 * the age. The head is a camera-plane drawing (like every other billboard); the body, plates, shards and rings are world geometry.
 */
public class UltIronDragonLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation SHARD = t("spike_shard"), PLATES = t("iron_ring"), SIGIL = t("iron_sigil"), BURST = t("iron_burst"), STREAK = t("iron_streak"),
            HALO = t("iron_halo"), SCALES = t("butoh_scales"), UMBRA = t("bfx_umbra"), CRACK = t("bfx_ground_crack"), CHUNK = t("earthfam_rock_chunk"),
            EMBER = t("smoke_ember"), GLINT = t("glint_0"), FLECKS = t("iron_flecks");

    private static final int GUN = 0xFF9096A2, STEEL = 0xFFE4E8F2, DARKST = 0xFF565B66, SHADOW = 0xFF0C0814, VIOLET = 0xFFB89CFF, WHITEV = 0xFFEDE6FF,
            RUST = 0xFFE0661F, RUSTD = 0xFF8E421A, HOT = 0xFFFFB76A;

    private static final int PLATES_OUT = 11, PLATES_IN = 5, SHARDS = 10, FLAKES = 8;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_IRON_DRAGON); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFF8E96A3; }

    @Override
    public void onTick(VfxInstance inst) {
        if (inst.age() == 7) VfxShake.add(inst.payload.from(), 1.1f, 12);                       // the head snap: a short mild thump
    }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static int mix(int c0, int c1, float t) { return VfxVertexBuffer.lerpColor(c0, c1, cl(t)); }

    /** The scratch the layer draws with (one per draw call, reused for every quad of it). */
    private static final class Scratch {
        final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();
        final Vector3f pos = new Vector3f(), tmp = new Vector3f();
        float rx, rz;                                                                     // the horizontal camera-right (plates face it)
    }

    /** A quad from a base centre to a tip centre with a half-width vector across, the texture's tip (v = 0) at the tip. */
    private static void strip(VfxVertexBuffer buf, Scratch s, ResourceLocation tex, VfxBlend bl, float bx, float by, float bz, float tx, float ty, float tz,
                              float sx, float sy, float sz, float u0, float u1, int cBase, int cTip) {
        if (!buf.hasBudget(4)) return;
        s.p0.set(bx - sx, by - sy, bz - sz);
        s.p1.set(bx + sx, by + sy, bz + sz);
        s.p2.set(tx + sx, ty + sy, tz + sz);
        s.p3.set(tx - sx, ty - sy, tz - sz);
        buf.quad(tex, bl, s.p0, s.p1, s.p2, s.p3, u0, 0f, u1, 1f, bl.grade(cBase, 1f), bl.grade(cTip, 1f));
    }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, Scratch s, float size, float rot, int col) {
        if (!buf.hasBudget(4) || (col >>> 24) < 3) return;
        buf.billboard(ctx, tex, bl, s.tmp.set(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, Scratch s, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (!buf.hasBudget(4) || radius < 0.05f || (col >>> 24) < 3) return;
        buf.plane(tex, bl, VfxPose.ground(s.tmp.set(c.x, c.y + y, c.z)).spin(spin), radius, col);
    }

    /** The body helix at 0..1 (tail to head), relative to the centre, spun by 'spin'; 'out' gets the point and (cs, sn) the radial direction. */
    private static void helix(float tt, float spin, float swell, Vector3f out, float[] radial) {
        float ang = spin + (1f - tt) * 10.4f;
        float r = Mth.lerp(tt, 4.8f, 1.9f) * swell + 0.18f * Mth.sin(tt * 13f + spin * 2f);
        radial[0] = Mth.cos(ang);
        radial[1] = Mth.sin(ang);
        out.set(radial[0] * r, 0.3f + tt * 8.0f + 0.2f * Mth.sin(tt * 9f - spin), radial[1] * r);
    }

    /** One quad of the head, in the head's own plane (fw = the way it faces, uh = its up): centre (cx, cy) and length / width in head units, the long axis at 'ang' from fw. */
    private static void headPart(VfxVertexBuffer buf, Scratch s, ResourceLocation tex, VfxBlend bl, Vector3f h, Vector3f fw, Vector3f uh, float hs,
                                 float cx, float cy, float ang, float len, float wid, int cell, float widGain, int cBase, int cTip) {
        float ca = Mth.cos(ang), sa = Mth.sin(ang);
        float ax = fw.x * ca + uh.x * sa, ay = fw.y * ca + uh.y * sa, az = fw.z * ca + uh.z * sa;      // the long axis
        float px = -fw.x * sa + uh.x * ca, py = -fw.y * sa + uh.y * ca, pz = -fw.z * sa + uh.z * ca;    // across it
        float mx = h.x + (fw.x * cx + uh.x * cy) * hs, my = h.y + (fw.y * cx + uh.y * cy) * hs, mz = h.z + (fw.z * cx + uh.z * cy) * hs;
        float hl = len * hs * 0.5f, hw = wid * hs * 0.5f * widGain;
        float u0 = 0f, u1 = 1f;
        strip(buf, s, tex, bl, mx - ax * hl, my - ay * hl, mz - az * hl, mx + ax * hl, my + ay * hl, mz + az * hl, px * hw, py * hw, pz * hw, u0, u1, cBase, cTip);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float T = inst.progress(ctx.partialTick) * 20f;                                   // the 20-tick design time, whatever the duration
        float R = Math.max(2f, inst.power);                                               // the burst radius, 12-13
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        Scratch s = new Scratch();
        float cr = (float) Math.sqrt(ctx.camRight.x * ctx.camRight.x + ctx.camRight.z * ctx.camRight.z);
        s.rx = cr < 1e-3f ? 1f : ctx.camRight.x / cr;
        s.rz = cr < 1e-3f ? 0f : ctx.camRight.z / cr;

        float rust = sub(T, 10f, 18f) * 0.75f;                                            // the iron turns to rust in the aftermath
        float live = 1f - sub(T, 14.5f, 19.8f);
        float lunge = VfxAnim.easeOutCubic(sub(T, 6f, 8.6f));
        float roar = sub(T, 7.5f, 9.8f) * (1f - sub(T, 12f, 15f));
        float shockP = sub(T, 6.2f, 13.5f);
        float shockR = R * VfxAnim.easeOutCubic(shockP);
        float flash = 1f - sub(T, 6.2f, 10.5f);
        boolean flashOn = T >= 6.1f && flash > 0f;
        int iron = mix(GUN, RUSTD, rust), steel = mix(STEEL, 0xFFB5764A, rust * 0.8f), dark = mix(DARKST, 0xFF3A1E12, rust * 0.7f);

        // ---------------------------------------------------------------------------------------------------- the ground: shadow pool, sigil, crack glow
        float pool = VfxAnim.easeOutCubic(sub(T, 0f, 6f));
        ground(buf, s, UMBRA, VfxBlend.ALPHA, c, 0.03f, R * (0.28f + 0.62f * pool), 0f, a(SHADOW, 0.8f * pool * live));
        ground(buf, s, SIGIL, VfxBlend.ALPHA, c, 0.05f, R * 0.78f * pool, T * 0.035f, a(dark, 0.55f * pool * live));

        // ---------------------------------------------------------------------------------------------------- the plates crawl up out of the ground
        int nO = ctx.seg(PLATES_OUT, 6), nI = ctx.seg(PLATES_IN, 3);
        for (int k = 0; k < nO + nI; k++) {
            boolean outer = k < nO;
            int kk = outer ? k : k - nO;
            float ang = (outer ? Mth.TWO_PI * kk / nO : Mth.TWO_PI * (kk + 0.5f) / nI) + (hash(seed, k, 1) - 0.5f) * 0.45f;
            float rr = outer ? R * (0.74f + 0.2f * hash(seed, k, 2)) : R * (0.3f + 0.12f * hash(seed, k, 2));
            float delay = outer ? 0.8f + 3.4f * hash(seed, k, 3) : 0.2f + 2.2f * hash(seed, k, 3);
            float gr = VfxAnim.easeOutBack(sub(T, delay, delay + 3.6f));
            if (gr <= 0.01f) continue;
            float sink = sub(T, 13f, 19.5f);
            float h = (outer ? 3.1f + 2.0f * hash(seed, k, 4) : 2.3f + 1.4f * hash(seed, k, 4)) * gr * (1f - 0.9f * sink);
            float w = (outer ? 1.35f : 1.0f) * (0.8f + 0.4f * hash(seed, k, 5));
            float ca = Mth.cos(ang), sa = Mth.sin(ang), lean = h * (0.12f + 0.2f * hash(seed, k, 6));
            float bx = c.x + ca * rr, bz = c.z + sa * rr;
            float hw = w * 0.5f * 1.2f;                                                    // the kite fills ~40% of its cell: widen to the wanted width
            int cell = (int) (hash(seed, k, 7) * 3.99f);
            int col = mix(iron, steel, hash(seed, k, 8) * 0.55f);
            strip(buf, s, SHARD, VfxBlend.ALPHA, bx, c.y - 0.15f, bz, bx + ca * lean, c.y + h, bz + sa * lean, s.rx * hw, 0f, s.rz * hw, 0f, 1f,
                    a(dark, 0.95f * live), a(col, 0.97f * live));
        }

        // ---------------------------------------------------------------------------------------------------- the dragon body: living shadow under iron scales
        float spin = T * 0.3f;
        float swell = 1f + 0.22f * VfxAnim.easeOutCubic(sub(T, 6f, 8f)) * (1f - sub(T, 9f, 13f));
        float g = VfxAnim.easeOutCubic(sub(T, 0.4f, 8f));
        float tail = sub(T, 11.5f, 18f) * 0.92f;
        float bodyA = sub(T, 0f, 2.5f) * live;
        float[] rad = new float[2];
        Vector3f hp = new Vector3f();                                                      // the head: the top of the helix, thrown outward by the lunge
        helix(1f, spin, swell, hp, rad);
        float sgn = hash(seed, 99, 1) < 0.5f ? 1f : -1f;
        float dx = s.rx * sgn, dz = s.rz * sgn;
        float outD = R * 0.6f;
        float hx = Mth.lerp(lunge, hp.x, dx * outD), hy = Mth.lerp(lunge, hp.y, 5.6f + 0.6f * lunge), hz = Mth.lerp(lunge, hp.z, dz * outD);
        float follow = (1f - sub(T, 12f, 20f) * 0.5f);
        hx *= follow; hz *= follow;
        float headHy = hy;
        int nb = ctx.seg(12, 7);
        float tMin = Math.min(tail, g - 0.05f), tMax = Math.max(tMin + 0.05f, g);
        Vector3f bp0 = new Vector3f(), bp1 = new Vector3f();
        float[] r0 = new float[2], r1 = new float[2];
        float hh0 = 0f;
        if (bodyA > 0.01f && g > 0.02f) {
            for (int pass = 0; pass < 2; pass++) {                                         // pass 0: the shadow strip (wider, black), pass 1: the iron scales over it
                for (int i = 0; i < nb; i++) {
                    float t0 = Mth.lerp((float) i / nb, tMin, tMax), t1 = Mth.lerp((float) (i + 1) / nb, tMin, tMax);
                    helix(t0, spin, swell, bp0, r0);
                    helix(t1, spin, swell, bp1, r1);
                    float w0 = lunge * VfxAnim.easeInOutSine(sub(t0, 0.7f, 1f)), w1 = lunge * VfxAnim.easeInOutSine(sub(t1, 0.7f, 1f));
                    float x0 = Mth.lerp(w0, bp0.x, hx), y0 = Mth.lerp(w0, bp0.y, hy), z0 = Mth.lerp(w0, bp0.z, hz);
                    float x1 = Mth.lerp(w1, bp1.x, hx), y1 = Mth.lerp(w1, bp1.y, hy), z1 = Mth.lerp(w1, bp1.z, hz);
                    float th0 = 0.8f + 0.95f * Mth.sin(Mth.PI * cl(t0 * 1.15f)), th1 = 0.8f + 0.95f * Mth.sin(Mth.PI * cl(t1 * 1.15f));
                    if (pass == 0) { th0 = th0 * 1.15f + 0.15f; th1 = th1 * 1.15f + 0.15f; }
                    float e0 = i == 0 ? sub(T, 0f, 3f) : 1f;
                    float ra0 = 0.45f * th0, ra1 = 0.45f * th1;
                    if (!buf.hasBudget(4)) break;
                    float u0 = t0 * 3.3f - T * 0.04f, u1 = t1 * 3.3f - T * 0.04f;
                    // a quad across the strip: the vertical band (slanted a little outward) between two helix points
                    // the band stands vertical on the helix and turns to face the camera as the neck stretches out to the head
                    float v0x = ctx.camUp.x * w0, v0y = (1f - w0) + ctx.camUp.y * w0, v0z = ctx.camUp.z * w0;
                    float v1x = ctx.camUp.x * w1, v1y = (1f - w1) + ctx.camUp.y * w1, v1z = ctx.camUp.z * w1;
                    float rr0 = ra0 * (1f - w0), rr1 = ra1 * (1f - w1);
                    Vector3f q0 = s.p0.set(c.x + x0 - v0x * th0, c.y + y0 - v0y * th0, c.z + z0 - v0z * th0), q1 = s.p1.set(c.x + x1 - v1x * th1, c.y + y1 - v1y * th1, c.z + z1 - v1z * th1);
                    Vector3f q2 = s.p2.set(c.x + x1 + v1x * th1 + r1[0] * rr1, c.y + y1 + v1y * th1, c.z + z1 + v1z * th1 + r1[1] * rr1);
                    Vector3f q3 = s.p3.set(c.x + x0 + v0x * th0 + r0[0] * rr0, c.y + y0 + v0y * th0, c.z + z0 + v0z * th0 + r0[1] * rr0);
                    if (pass == 0) {
                        int cs = a(SHADOW, 0.5f * bodyA * e0);
                        buf.quad(UMBRA, VfxBlend.ALPHA, q0, q1, q2, q3, 0.18f, 0.18f, 0.82f, 0.82f, cs, cs);
                    } else {
                        float lit = 0.45f + 0.55f * Mth.sin(t0 * 7f - spin * 1.5f) * 0.5f + 0.25f;
                        int lo = mix(dark, iron, lit), hi = mix(iron, steel, lit);
                        buf.quad(SCALES, VfxBlend.ALPHA, q0, q1, q2, q3, u0, 0f, u1, 1f, a(lo, 0.97f * bodyA * e0), a(hi, 0.97f * bodyA * e0));
                    }
                }
            }
        }

        // ---------------------------------------------------------------------------------------------------- the head (a camera-plane drawing)
        float headA = sub(T, 3.4f, 6f) * (1f - sub(T, 12.5f, 17f));
        float hs = Mth.lerp(VfxAnim.easeOutCubic(sub(T, 3.4f, 9.2f)), 2.3f, 6.4f) * (1f + 0.06f * Mth.sin(T * 2.4f) * roar);
        Vector3f hv = new Vector3f(c.x + hx, c.y + headHy, c.z + hz);
        if (headA > 0.02f) {
            float ph = 0.16f - 0.18f * lunge;
            float cp = Mth.cos(ph), sp = Mth.sin(ph);
            Vector3f fw = new Vector3f(ctx.camRight).mul(sgn * cp).add(s.tmp.set(ctx.camUp).mul(sp));
            Vector3f uh = new Vector3f(ctx.camUp).mul(cp).sub(s.tmp.set(ctx.camRight).mul(sgn * sp));
            float jaw = 0.12f + 0.62f * roar;
            // the shadow mantle behind the skull
            sprite(ctx, buf, UMBRA, VfxBlend.ALPHA, hv.x - fw.x * hs * 0.1f, hv.y - fw.y * hs * 0.1f, hv.z - fw.z * hs * 0.1f, s, hs * 1.15f, 0f, a(SHADOW, 0.8f * headA));
            sprite(ctx, buf, EMBER, VfxBlend.ADD, hv.x, hv.y, hv.z, s, hs * 1.9f, 0f, a(VIOLET, 0.22f * headA));        // the violet edge light round it
            int hi = a(mix(steel, 0xFFC9CEDA, 0.35f), headA), md = a(iron, headA), lo = a(dark, headA);
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, -0.06f, 0.1f, -0.08f, 0.95f, 0.62f, 3, 1.25f, lo, md);            // skull / cheek
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, -0.36f, 0.4f, Mth.PI - 0.55f, 0.95f, 0.17f, 0, 1.25f, md, hi);      // horn up
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, -0.44f, 0.16f, Mth.PI - 0.12f, 0.85f, 0.15f, 2, 1.25f, md, hi);     // horn back
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, 0.42f, 0.12f, 0.03f, 1.1f, 0.4f, 1, 1.25f, md, hi);                  // upper snout
            float jx = Mth.cos(-jaw), jy = Mth.sin(-jaw);
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, 0.02f + jx * 0.42f, -0.02f + jy * 0.42f, -jaw, 0.95f, 0.28f, 2, 1.25f, lo, md);   // lower jaw
            int fang = a(0xFFE6EAF2, headA);
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, 0.5f, -0.07f, -Mth.HALF_PI - 0.05f, 0.3f, 0.1f, 0, 1.25f, fang, fang);      // fangs
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, 0.78f, -0.04f, -Mth.HALF_PI - 0.1f, 0.26f, 0.09f, 1, 1.25f, fang, fang);
            headPart(buf, s, SHARD, VfxBlend.ALPHA, hv, fw, uh, hs, 0.02f + jx * 0.7f, -0.02f + jy * 0.7f + 0.1f, Mth.HALF_PI - jaw * 0.5f, 0.27f, 0.09f, 0, 1.25f, fang, fang);
            // the eye, the mouth glow, the roar streaks
            float ex = hv.x + fw.x * hs * 0.16f + uh.x * hs * 0.21f, ey = hv.y + fw.y * hs * 0.16f + uh.y * hs * 0.21f, ez = hv.z + fw.z * hs * 0.16f + uh.z * hs * 0.21f;
            sprite(ctx, buf, EMBER, VfxBlend.ADD, ex, ey, ez, s, hs * 0.42f, 0f, a(VIOLET, 0.95f * headA));
            sprite(ctx, buf, GLINT, VfxBlend.ADD, ex, ey, ez, s, hs * 0.5f, 0.4f, a(WHITEV, 0.95f * headA));
            float mx = hv.x + fw.x * hs * 0.5f + uh.x * hs * -0.12f, my = hv.y + fw.y * hs * 0.5f + uh.y * hs * -0.12f, mz = hv.z + fw.z * hs * 0.5f + uh.z * hs * -0.12f;
            sprite(ctx, buf, EMBER, VfxBlend.ADD, mx, my, mz, s, hs * (0.4f + 0.7f * roar), 0f, a(RUST, (0.35f + 0.55f * roar) * headA));
            if (roar > 0.02f) {
                float sl = hs * (0.5f + 1.7f * roar), sw = hs * 0.5f * (0.6f + 0.5f * roar);
                float ox = hv.x + fw.x * hs * 0.9f, oy = hv.y + fw.y * hs * 0.9f, oz = hv.z + fw.z * hs * 0.9f;
                for (int k = 0; k < 2; k++) {
                    float sp2 = k == 0 ? 0.3f : -0.3f, cs2 = Mth.cos(sp2), sn2 = Mth.sin(sp2);
                    float ax = fw.x * cs2 + uh.x * sn2, ay = fw.y * cs2 + uh.y * sn2, az = fw.z * cs2 + uh.z * sn2;
                    float px = -fw.x * sn2 + uh.x * cs2, py = -fw.y * sn2 + uh.y * cs2, pz = -fw.z * sn2 + uh.z * cs2;
                    strip(buf, s, STREAK, VfxBlend.ADD, ox, oy, oz, ox + ax * sl, oy + ay * sl, oz + az * sl, px * sw * 0.5f, py * sw * 0.5f, pz * sw * 0.5f, 0f, 1f,
                            a(WHITEV, 0.1f * roar * headA), a(VIOLET, 0.5f * roar * headA));
                }
            }
        }

        // ---------------------------------------------------------------------------------------------------- the blow: shock ring, ground rays, rust cracks, flying shards
        if (shockP > 0f) {
            float fade = 1f - sub(T, 10.5f, 15f);
            int ns = ctx.seg(10, 6);
            float in = Math.max(0.1f, shockR - 1.5f), out = shockR + 0.35f;
            if (fade > 0f && shockR > 0.4f && buf.hasBudget(ns * 4)) {
                buf.ring(PLATES, VfxBlend.ALPHA, VfxPose.ground(s.tmp.set(c.x, c.y + 0.09f, c.z)), in, out, ns, 5f, -T * 0.05f, a(mix(steel, dark, 0.2f), 0.95f * fade));
            }
            ground(buf, s, HALO, VfxBlend.ADD, c, 0.12f, shockR * 1.02f, T * 0.02f, a(VIOLET, 0.42f * fade));
        }
        if (flashOn) {
            float fe = VfxAnim.easeOutCubic(sub(T, 6.1f, 9f));
            ground(buf, s, BURST, VfxBlend.ADD, c, 0.14f, R * (0.45f + 0.5f * fe), 0.2f, a(WHITEV, 0.8f * flash));
            sprite(ctx, buf, BURST, VfxBlend.ADD, hv.x, hv.y, hv.z, s, hs * (0.9f + 1.2f * fe), 0.3f, a(WHITEV, 0.5f * flash));
        }
        float crackA = VfxAnim.easeOutCubic(sub(T, 6.4f, 9.5f)) * (1f - sub(T, 13f, 19.5f));
        ground(buf, s, CRACK, VfxBlend.ADD, c, 0.16f, R * (0.55f + 0.42f * VfxAnim.easeOutCubic(sub(T, 6.4f, 11f))), 0.5f, a(RUST, 0.78f * crackA));

        int nsh = ctx.seg(SHARDS, 5);
        float shp = VfxAnim.easeOutCubic(sub(T, 6.3f, 14f));
        float shA = sub(T, 6.2f, 7f) * (1f - sub(T, 12.5f, 16f));
        if (shA > 0.02f) {
            for (int k = 0; k < nsh; k++) {
                float ang = Mth.TWO_PI * k / nsh + hash(seed, k, 20) * 0.5f;
                float ca = Mth.cos(ang), sa = Mth.sin(ang);
                float rr = 1.4f + (R * 0.96f - 1.4f) * shp * (0.72f + 0.28f * hash(seed, k, 21));
                float yy = 0.7f + 2.1f * hash(seed, k, 22) * (0.5f + 0.5f * Mth.sin(shp * Mth.PI)) + 0.3f * shp;
                float ax = ca * ctx.camRight.x + sa * ctx.camRight.z, ay = ca * ctx.camUp.x + sa * ctx.camUp.z;              // outward direction in the camera plane
                ax = ca * ctx.camRight.x + sa * ctx.camRight.z;
                float dxr = ca * ctx.camRight.x + sa * ctx.camRight.z, dyu = ca * ctx.camUp.x + sa * ctx.camUp.z + 0.12f;
                float dl = (float) Math.sqrt(dxr * dxr + dyu * dyu);
                if (dl < 1e-3f) { dxr = 1f; dyu = 0f; dl = 1f; }
                dxr /= dl; dyu /= dl;
                float len = 1.4f + 1.0f * hash(seed, k, 23), wd = 0.34f + 0.2f * hash(seed, k, 24);
                float mx = c.x + ca * rr, my = c.y + yy, mz = c.z + sa * rr;
                float vx = (ctx.camRight.x * dxr + ctx.camUp.x * dyu), vy = (ctx.camRight.y * dxr + ctx.camUp.y * dyu), vz = (ctx.camRight.z * dxr + ctx.camUp.z * dyu);
                float qx = -(ctx.camRight.x * dyu) + ctx.camUp.x * dxr, qy = -(ctx.camRight.y * dyu) + ctx.camUp.y * dxr, qz = -(ctx.camRight.z * dyu) + ctx.camUp.z * dxr;
                int cell = (int) (hash(seed, k, 25) * 3.99f);
                int col = mix(iron, steel, 0.5f + 0.5f * hash(seed, k, 26));
                strip(buf, s, SHARD, VfxBlend.ALPHA, mx - vx * len * 0.5f, my - vy * len * 0.5f, mz - vz * len * 0.5f, mx + vx * len * 0.5f, my + vy * len * 0.5f, mz + vz * len * 0.5f,
                        qx * wd * 0.6f, qy * wd * 0.6f, qz * wd * 0.6f, 0f, 1f, a(dark, shA), a(col, shA));
            }
        }

        // ---------------------------------------------------------------------------------------------------- violet-white glints on the body edge, rust sparks, falling rust
        int ng = ctx.seg(6, 3);
        if (bodyA > 0.1f && g > 0.1f) {
            for (int k = 0; k < ng; k++) {
                float tt = Mth.lerp((k + 0.5f) / ng, tMin, tMax);
                helix(tt, spin, swell, bp0, r0);
                float wk = lunge * VfxAnim.easeInOutSine(sub(tt, 0.7f, 1f));
                float th = 0.8f + 0.95f * Mth.sin(Mth.PI * cl(tt * 1.15f));
                float tw = 0.5f + 0.5f * Mth.sin(T * 1.7f + k * 2.3f);
                sprite(ctx, buf, GLINT, VfxBlend.ADD, c.x + Mth.lerp(wk, bp0.x, hx) + r0[0] * 0.45f * th, c.y + Mth.lerp(wk, bp0.y, hy) + th, c.z + Mth.lerp(wk, bp0.z, hz) + r0[1] * 0.45f * th, s,
                        0.7f + 0.8f * tw, T * 0.1f + k, a(k % 2 == 0 ? WHITEV : VIOLET, (0.45f + 0.5f * tw) * bodyA));
            }
        }
        for (int k = 0; k < 4; k++) {                                                      // rust sparks thrown off the shock
            float ph = sub(T, 6.4f + 0.3f * k, 14.5f + 0.3f * k);
            if (ph <= 0f || ph >= 1f) continue;
            float ang = hash(seed, k, 30) * Mth.TWO_PI, rr = (1.5f + R * 0.8f * hash(seed, k, 31)) * VfxAnim.easeOutCubic(ph);
            sprite(ctx, buf, EMBER, VfxBlend.ADD, c.x + Mth.cos(ang) * rr, c.y + 0.4f + 2.6f * Mth.sin(ph * Mth.PI) * (0.5f + hash(seed, k, 32)), c.z + Mth.sin(ang) * rr, s,
                    0.5f + 0.4f * hash(seed, k, 33), 0f, a(k % 2 == 0 ? RUST : HOT, 0.95f * (1f - ph)));
        }
        for (int k = 0; k < FLAKES; k++) {                                                 // rust flakes falling in the aftermath
            float st = 9.5f + 4.5f * hash(seed, k, 40);
            float ph = sub(T, st, st + 9f);
            if (ph <= 0f || ph >= 1f) continue;
            float ang = hash(seed, k, 41) * Mth.TWO_PI, rr = R * 0.8f * (float) Math.sqrt(hash(seed, k, 42));
            float yy = (6.5f + 2.5f * hash(seed, k, 43)) * (1f - ph * ph) + 0.1f;
            sprite(ctx, buf, CHUNK, VfxBlend.ALPHA, c.x + Mth.cos(ang) * rr + 0.4f * Mth.sin(ph * 9f + k), c.y + yy, c.z + Mth.sin(ang) * rr, s,
                    0.34f + 0.3f * hash(seed, k, 44), T * 0.3f + k * 1.7f, a(k % 3 == 0 ? RUST : RUSTD, 0.9f * Mth.sin(ph * Mth.PI) * (1f + 0.2f)));
        }
        if (flashOn && !buf.hasBudget(8)) return;
        float fl = sub(T, 6.4f, 7.5f) * (1f - sub(T, 7.5f, 12f));
        if (fl > 0.02f) sprite(ctx, buf, FLECKS, VfxBlend.ADD, c.x, c.y + 1.4f, c.z, s, R * 0.55f, T * 0.05f, a(STEEL, 0.5f * fl));
    }
}
