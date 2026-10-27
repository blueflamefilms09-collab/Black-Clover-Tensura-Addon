package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Ultimate "Devouring Shadow" (The Beginning After the End, Regis Leywin: the shadow wolf's one great bite). {@code inst.power} = burst radius R.
 * <ul>
 *   <li>0-6 (build-up): the ground blackens into a pool of living shadow (indigo veins, a spiral dragging inward), a thorny rim, shadow tendrils crawl in
 *       from outside the circle, the lower row of ten shadow fangs thrusts up round the rim and a wolf head of soft shadow with pale slit eyes rises
 *       out of the pool on the far side.</li>
 *   <li>6-12 (peak): the upper row of ten fangs falls from above between the lower ones and the whole maw slams shut inward at tick 10: a violet-blue
 *       flash, a ring of light closing across the floor, the eyes flare.</li>
 *   <li>12-20 (aftermath): jaws held for a beat, then the fangs, head and pool are pulled down into the centre and collapse into one black point that
 *       fades, the tendrils retreating after it.</li>
 * </ul>
 * Palette: black, deep indigo, violet-blue glow, pale eyes (the caster colour is only a 12 % accent on the glow). Cost: at most 400 vertices (about
 * 350 at full detail), constant loops, no allocation beyond the scratch vectors; everything is a pure function of seed and age.
 */
public class UltDevouringShadowLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation POOL = t("black_oil_pool"), UMBRA = t("bfx_umbra"), RIM = t("black_oil_rim"), SWIRL = t("darkrb_hole"),
            SPEAR = t("darkfx_spear"), SPEAR_GLOW = t("darkfx_spear_glow"), PUFF = t("smoke_billow"), WISP = t("darkrb_wisp"), TENDRIL = t("darkrb_tendril"),
            EYE = t("demon_beast_eye"), GLOW = t("glow"), SHOCK = t("darkfx_shock"), FLARE = t("darkfx_flare"), TEAR = t("dark_slash_tear");

    private static final int BLACK = 0xFF05030C, DEEP = 0xFF140B30, INDIGO = 0xFF2C1C66, VEIN = 0xFF4A34A0, VIOLET = 0xFF8456FF, BLUE = 0xFF5C86FF, PALE = 0xFFE6ECFF;

    private static final int N = 8;                                                     // fangs per row

    // scratch vectors (the render thread only): no per-frame allocation
    private final Vector3f vA = new Vector3f(), vB = new Vector3f(), vC = new Vector3f(), vD = new Vector3f(), vE = new Vector3f(), vF = new Vector3f();

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_DEVOURING_SHADOW); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFF8456FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        VfxShake.add(inst.payload.from(), 0.5f, 9);
    }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    // ------------------------------------------------------------------------------------------------ state of one frame
    private float R, age, fade, cx, cy, cz, sr, sv;
    private long seed;

    /** The collapse of ticks 13-19 pulls a point into the centre (horizontally by sr, vertically by sv). */
    private Vector3f pull(Vector3f p) {
        p.x = cx + (p.x - cx) * sr;
        p.z = cz + (p.z - cz) * sr;
        p.y = cy + (p.y - cy) * sv;
        return p;
    }

    private Vector3f at(Vector3f out, float dx, float y, float dz) { return pull(out.set(cx + dx, cy + y, cz + dz)); }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        age = inst.ageTicks(ctx.partialTick);
        float dur = Math.max(10f, inst.duration);
        R = Mth.clamp(inst.power, 4f, 20f);
        seed = inst.seed;
        fade = cl(age / 1.5f) * (1f - sub(age, dur - 3f, dur));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        cx = c.x; cy = c.y; cz = c.z;
        float col = VfxAnim.easeInCubic(sub(age, 12.8f, dur - 0.8f));                  // the collapse into the centre
        sr = 1f - 0.93f * col;
        sv = 1f - 0.88f * col;
        int glow = VfxVertexBuffer.lerpColor(VIOLET, inst.color | 0xFF000000, 0.12f);

        float open = VfxAnim.easeOutCubic(sub(age, 0f, 5f));                              // the pool spreads
        float bite = VfxAnim.easeInCubic(sub(age, 3.5f, 10f));                            // the upper jaw falls, 1 = shut
        float shut = 1f - sub(age, 10f, 13f);                                             // glow of the closing blow fades after tick 10

        pool(ctx, buf, glow, open, col);
        tendrils(buf, glow, open, col);
        head(ctx, buf, glow, col);
        jaws(ctx, buf, glow, open, bite);
        gums(ctx, buf, open, bite);
        impact(ctx, buf, glow, bite, shut, col);
    }

    // ------------------------------------------------------------------------------------------------ the pool of living shadow
    private void pool(VfxRenderContext ctx, VfxVertexBuffer buf, int glow, float open, float col) {
        float r = R * (0.25f + 0.78f * open);
        float al = fade * (1f - 0.55f * col);
        vA.set(cx, cy + 0.05f, cz);
        if (buf.hasBudget(4)) buf.plane(UMBRA, VfxBlend.ALPHA, VfxPose.ground(vA), r * 1.12f, a(BLACK, 0.92f * al));
        vA.set(cx, cy + 0.08f, cz);
        if (buf.hasBudget(4)) buf.plane(POOL, VfxBlend.ALPHA, VfxPose.ground(vA).spin(age * 0.018f), r, a(INDIGO, 0.95f * al));
        vA.set(cx, cy + 0.10f, cz);
        if (buf.hasBudget(4)) buf.plane(RIM, VfxBlend.ALPHA, VfxPose.ground(vA).spin(-age * 0.01f), r * 1.02f, a(DEEP, 0.95f * al));
        vA.set(cx, cy + 0.12f, cz);
        if (buf.hasBudget(4)) buf.plane(SWIRL, VfxBlend.ADD, VfxPose.ground(vA).spin(-age * 0.11f), r * 0.98f, a(VEIN, 0.5f * al * (0.7f + 0.3f * Mth.sin(age * 0.5f))));
    }

    // ------------------------------------------------------------------------------------------------ tendrils dragging in from outside
    private void tendrils(VfxVertexBuffer buf, int glow, float open, float col) {
        int tn = 5;
        for (int k = 0; k < tn; k++) {
            float a0 = Mth.TWO_PI * k / tn + 0.4f * hash(seed, k, 1), curl = 0.9f + 0.4f * hash(seed, k, 2);
            float start = 0.15f * hash(seed, k, 3);
            float head = VfxAnim.easeInOutSine(sub(age, 0.5f + start * 6f, 11.5f));    // how far in the tip has dragged
            float tail = Math.max(0f, head - 0.55f);
            if (head <= 0.01f) continue;
            float prev = 0f, px = 0f, pz = 0f;
            for (int s = 0; s <= 2; s++) {                                              // 2 segments, 3 points on a curl from rim*1.25 to the centre
                float u = Mth.lerp(s / 2f, tail, head);
                float rr = R * (1.3f - 1.22f * u), an = a0 + curl * u * 1.6f;
                float x = Mth.cos(an) * rr, z = Mth.sin(an) * rr;
                if (s > 0 && buf.hasBudget(4)) {
                    float dx = x - px, dz = z - pz, len = Mth.sqrt(dx * dx + dz * dz);
                    if (len > 0.05f) {
                        float w = R * 0.11f * (1f - 0.25f * u), nx = -dz / len * w, nz = dx / len * w;
                        float al = fade * (1f - sub(age, 13f, 19f)) * (s == 1 ? 0.85f : 1f);
                        vA.set(cx + px - nx, cy + 0.11f, cz + pz - nz); pull(vA);
                        vB.set(cx + px + nx, cy + 0.11f, cz + pz + nz); pull(vB);
                        vC.set(cx + x + nx, cy + 0.11f, cz + z + nz); pull(vC);
                        vD.set(cx + x - nx, cy + 0.11f, cz + z - nz); pull(vD);
                        int cc = a(VfxVertexBuffer.lerpColor(0xFF8A7AC0, 0xFFFFFFFF, 0.3f * u), 0.75f * al);
                        buf.quad(SPEAR, VfxBlend.ALPHA, vA, vB, vC, vD, 0f, 1f - s * 0.5f, 1f, 1f - (s - 1) * 0.5f, cc, cc);
                    }
                }
                px = x; pz = z;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ the two rows of fangs
    private void jaws(VfxRenderContext ctx, VfxVertexBuffer buf, int glow, float open, float bite) {
        float lean = 0.18f + 0.62f * bite;
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < N; i++) {
                float h1 = hash(seed, i + 40 * row, 11), h2 = hash(seed, i + 40 * row, 12);
                float an = Mth.TWO_PI * (i + 0.5f * row) / N + 0.1f * (h1 - 0.5f);
                float grow = row == 0 ? VfxAnim.easeOutBack(sub(age, 0.4f + 2.8f * h2, 6f + 1.5f * h2))
                                      : VfxAnim.easeOutCubic(sub(age, 3.5f + 1.2f * h2, 8f));
                if (grow <= 0.01f) continue;
                float len = R * (0.46f + 0.16f * h1) * grow * (row == 0 ? 1f : 1.1f), wd = R * (0.27f + 0.06f * h2);
                float rr = R * (row == 0 ? 0.80f : 0.76f) * (0.35f + 0.65f * open) * (1f - 0.1f * bite);
                float ux = Mth.cos(an), uz = Mth.sin(an);
                float ly = lean * (0.85f + 0.3f * h1);
                float dl = Mth.sqrt(1f + ly * ly);
                if (row == 0) {
                    at(vA, ux * rr, 0.05f, uz * rr);
                    at(vB, ux * (rr - len * ly / dl), len / dl, uz * (rr - len * ly / dl));
                } else {
                    float top = len / dl + R * 0.10f + (1f - bite) * R * 0.85f * (1.0f + 0.15f * h2);   // the tips meet the floor between the lower fangs when shut
                    at(vA, ux * rr, top, uz * rr);
                    at(vB, ux * (rr - len * ly / dl), top - len / dl, uz * (rr - len * ly / dl));
                }
                fang(buf, vA, vB, wd * (0.6f + 0.4f * Math.min(1f, sr + 0.1f)), ux, uz, a(0xFFFFFFFF, fade), a(glow, fade * 0.55f));
            }
        }
    }

    /**
     * One shadow fang from base to tip: two crossed blades (one across the ring, one along it) so it has body from every side, black with a violet
     * edge (the spear texture's upper half: wide root, sharp point). 12 vertices.
     */
    private void fang(VfxVertexBuffer buf, Vector3f base, Vector3f tip, float w, float ux, float uz, int tint, int gl) {
        if (!buf.hasBudget(12)) return;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? -uz : ux, sz = k == 0 ? ux : uz, hw = (k == 0 ? w : w * 0.7f) * 0.5f;
            vD.set(base.x - sx * hw, base.y, base.z - sz * hw);
            vE.set(base.x + sx * hw, base.y, base.z + sz * hw);
            vF.set(tip.x + sx * hw * 0.04f, tip.y, tip.z + sz * hw * 0.04f);
            vA2.set(tip.x - sx * hw * 0.04f, tip.y, tip.z - sz * hw * 0.04f);
            buf.quad(SPEAR, VfxBlend.ALPHA, vD, vE, vF, vA2, 0f, 0f, 1f, 0.58f, tint, tint);
            if (k == 0) buf.quad(SPEAR_GLOW, VfxBlend.ADD, vD, vE, vF, vA2, 0f, 0f, 1f, 0.58f, gl, gl);
        }
    }

    private final Vector3f vA2 = new Vector3f();

    /** Soft lumps of shadow at the root of each row (the gums), so the fangs grow out of something. */
    private void gums(VfxRenderContext ctx, VfxVertexBuffer buf, float open, float bite) {
        float rr = R * 0.78f * (0.35f + 0.65f * open) * (1f - 0.1f * bite);
        for (int row = 0; row < 1; row++) {                                              // the lower row only: the upper one grows out of the dark above the view
            float g = row == 0 ? sub(age, 0.5f, 4f) : sub(age, 3.5f, 8f);
            if (g <= 0f) continue;
            for (int i = 0; i < 6; i++) {
                float an = Mth.TWO_PI * (i + 0.5f * row + 0.2f * hash(seed, i, 20 + row)) / 6f;
                float y = row == 0 ? 0.3f : R * 0.12f + R * 0.5f + (1f - bite) * R * 0.85f;
                at(vA, Mth.cos(an) * rr, y, Mth.sin(an) * rr);
                if (buf.hasBudget(4)) buf.billboard(ctx, UMBRA, VfxBlend.ALPHA, vA, R * 0.34f * g, 0f, a(BLACK, 0.7f * fade));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ the wolf: a silhouette of soft shadow with pale slit eyes
    private float hX, hY, hZ, hS, hCol;
    private Vector3f hr, hu;

    /** vA = the point (x, y) of the head plane, in units of the head size, sunk into the centre with the collapse. */
    private Vector3f hp(float x, float y) {
        put(vA, hX, hY, hZ, hr, hu, x * hS, y * hS);
        vA.x = cx + (vA.x - cx) * (1f - 0.97f * hCol);
        vA.z = cz + (vA.z - cz) * (1f - 0.97f * hCol);
        vA.y = cy + (vA.y - cy) * (1f - 0.9f * hCol);
        return vA;
    }

    private void head(VfxRenderContext ctx, VfxVertexBuffer buf, int glow, float col) {
        float rise = VfxAnim.easeOutCubic(sub(age, 1.5f, 8f));
        if (rise <= 0.01f) return;
        float dx = cx, dz = cz, dl = Mth.sqrt(dx * dx + dz * dz);
        if (dl < 0.01f) { dx = ctx.camRight.x; dz = ctx.camRight.z; dl = Math.max(0.01f, Mth.sqrt(dx * dx + dz * dz)); }
        dx /= dl; dz /= dl;                                                            // away from the camera
        hS = R * 1.0f * (0.6f + 0.4f * rise);
        hX = cx + dx * R * 0.40f; hZ = cz + dz * R * 0.40f;
        hY = cy + R * (-0.12f + 0.52f * rise) + R * 0.02f * Mth.sin(age * 0.4f);
        hCol = col; hr = ctx.camRight; hu = ctx.camUp;
        float al = fade * (1f - sub(age, 12f, 19f));
        int body = a(BLACK, 0.96f * al), deep = a(DEEP, 0.92f * al), white = a(0xFFFFFFFF, 0.98f * al);
        // flame-like wisps of violet-blue behind the head
        for (int k = 0; k < 3; k++) {
            float ph = (age * 0.09f + hash(seed, k, 30)) % 1f;
            hp((k - 1) * 0.36f + 0.06f * Mth.sin(ph * 6f + k), 0.30f + ph * 0.5f);
            if (buf.hasBudget(4)) buf.billboard(ctx, WISP, VfxBlend.ADD, vA, hS * (0.5f + 0.3f * ph), hash(seed, k, 31) * 6.28f + age * 0.1f, a(k == 1 ? BLUE : VIOLET, 0.6f * (1f - ph) * al));
        }
        // ears, rooted inside the skull
        for (int s = -1; s <= 1; s += 2) { hp(s * 0.27f, 0.22f); earQuad(buf, vA, hr, hu, s * 0.2f, hS * 0.66f, hS * 0.36f, white); }
        // fur spikes of the ruff, thrown out sideways and down
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 0; k < 2; k++) { hp(s * (0.34f + 0.05f * k), -0.12f - 0.12f * k); earQuad(buf, vA, hr, hu, s * (1.75f + 0.5f * k), hS * 0.5f, hS * 0.2f, white); }
        }
        // skull, cheeks, muzzle
        hp(0f, 0.03f);
        if (buf.hasBudget(4)) buf.billboard(ctx, PUFF, VfxBlend.ALPHA, vA, hS * 0.95f, 0.3f, body);
        hp(0f, 0.1f);
        if (buf.hasBudget(4)) buf.billboard(ctx, PUFF, VfxBlend.ALPHA, vA, hS * 0.7f, 2.1f, body);
        for (int s = -1; s <= 1; s += 2) {
            hp(s * 0.27f, -0.14f);
            if (buf.hasBudget(4)) buf.billboard(ctx, PUFF, VfxBlend.ALPHA, vA, hS * 0.62f, 1.2f * s, deep);
        }
        hp(0f, -0.26f);
        if (buf.hasBudget(4)) buf.billboard(ctx, PUFF, VfxBlend.ALPHA, vA, hS * 0.5f, 0.9f, body);
        // eyes: a glow and an almond with a slit pupil, slanted fiercely
        float ey = VfxAnim.easeOutCubic(sub(age, 4f, 7f));
        float flare = 1f + 0.9f * (1f - Math.min(1f, Math.abs(age - 10f) / 2.2f));
        for (int s = -1; s <= 1; s += 2) {
            hp(s * 0.19f, 0.05f);
            if (buf.hasBudget(8)) {
                buf.billboard(ctx, GLOW, VfxBlend.ADD, vA, hS * 0.46f * flare * ey, 0f, a(glow, 0.5f * al * ey));
                buf.billboard(ctx, EYE, VfxBlend.ADD, vA, hS * 0.25f * ey, s * 0.5f, a(PALE, Math.min(1f, 0.95f * flare) * al * ey));
            }
        }
    }

    private static void put(Vector3f out, float hx, float hy, float hz, Vector3f r, Vector3f u, float x, float y) {
        out.set(hx + r.x * x + u.x * y, hy + r.y * x + u.y * y, hz + r.z * x + u.z * y);
    }

    /** A camera-plane quad along an axis tilted by phi (radians from straight up): an ear. The centre is the base, it points along the axis. */
    private void earQuad(VfxVertexBuffer buf, Vector3f base, Vector3f r, Vector3f u, float phi, float len, float wid, int tint) {
        if (!buf.hasBudget(8)) return;
        float sp = Mth.sin(phi), cp = Mth.cos(phi);
        float ax = r.x * sp + u.x * cp, ay = r.y * sp + u.y * cp, az = r.z * sp + u.z * cp;
        float bx = (r.x * cp - u.x * sp) * wid * 0.5f, by = (r.y * cp - u.y * sp) * wid * 0.5f, bz = (r.z * cp - u.z * sp) * wid * 0.5f;
        vC.set(base).sub(ax * len * 0.15f, ay * len * 0.15f, az * len * 0.15f);
        vD.set(vC).sub(bx, by, bz);
        vE.set(vC).add(bx, by, bz);
        vF.set(base).add(ax * len * 0.85f + bx * 0.02f, ay * len * 0.85f + by * 0.02f, az * len * 0.85f + bz * 0.02f);
        vA2.set(base).add(ax * len * 0.85f - bx * 0.02f, ay * len * 0.85f - by * 0.02f, az * len * 0.85f - bz * 0.02f);
        buf.quad(SPEAR, VfxBlend.ALPHA, vD, vE, vF, vA2, 0f, 0f, 1f, 1f, tint, tint);
    }

    // ------------------------------------------------------------------------------------------------ the closing blow
    private void impact(VfxRenderContext ctx, VfxVertexBuffer buf, int glow, float bite, float shut, float col) {
        // a ring of violet-blue light closing across the floor from the fang circle to the centre
        float cl = sub(age, 8.5f, 14f);
        if (cl > 0f && cl < 1f && buf.hasBudget(4)) {
            vA.set(cx, cy + 0.16f, cz);
            buf.plane(SHOCK, VfxBlend.ADD, VfxPose.ground(vA).spin(age * 0.2f), R * (1.0f - 0.9f * VfxAnim.easeInCubic(cl)), a(glow, 0.9f * Mth.sin(cl * Mth.PI) * fade));
        }
        // the flash of the bite, low and wide, with a star
        float fl = Math.max(0f, 1f - Math.abs(age - 10.3f) / 2.6f);
        if (fl > 0f) {
            vA.set(cx, cy + R * 0.12f, cz); pull(vA);
            if (buf.hasBudget(8)) {
                buf.billboard(ctx, GLOW, VfxBlend.ADD, vA, R * (0.9f + 0.9f * fl), 0f, a(BLUE, 0.7f * fl));
                buf.billboard(ctx, FLARE, VfxBlend.ADD, vA, R * (1.2f + 0.8f * fl), 0f, a(PALE, 0.8f * fl));
            }
        }
        // the point the shadow collapses into
        if (col > 0.05f && buf.hasBudget(8)) {
            vA.set(cx, cy + R * 0.1f * (1f - col), cz);
            buf.billboard(ctx, UMBRA, VfxBlend.ALPHA, vA, R * (0.2f + 0.5f * (1f - col)), 0f, a(BLACK, fade));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, vA, R * 0.4f * (1f - col * 0.6f), 0f, a(VIOLET, 0.55f * fade * col));
        }
    }
}
