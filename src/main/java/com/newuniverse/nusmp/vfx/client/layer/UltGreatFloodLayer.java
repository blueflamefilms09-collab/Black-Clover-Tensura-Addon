package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Ultimate effect ULT_GREAT_FLOOD, Juvia Lockser's "Water Body: Great Flood" (Fairy Tail). 20 ticks, power = radius in blocks, centred on the caster's feet.
 * <ul>
 *   <li>0-6 build-up: grey rain streaks fall over the whole field, the field floods (a swirling dark pool grows over the ground), a ring-wall of water
 *       rises at the radius, and a woman-shaped crest of water (skirt, waist, shoulders, head, raised arms, streaming hair, helical rising ribbons)
 *       starts to stand up in the middle in glass-blue light.</li>
 *   <li>6-10 surge: the wall rushes inward, shrinking and lowering, while the pool vortex spins up; the woman reaches her full height.</li>
 *   <li>10-12 peak: a flash of glass-blue light, the woman bursts, and a huge crashing wave ring (two-piece wall with a curling overhang, foam on the
 *       lip, thrown droplets and glints) leaves the middle.</li>
 *   <li>12-20 aftermath: the wave runs out to the rim and drops, foam rings and ripples fade over the ground, the rain stops.</li>
 * </ul>
 * Own palette (deep blue, teal, foam white, rain grey, glass blue); inst.color is only a faint accent. Cost: no more than 400 vertices (the budget line of the
 * preview shows the worst frame), constant loop counts, scratch vectors reused, deterministic from seed and age.
 */
public class UltGreatFloodLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation FLOW = t("water_flow"), POOL = t("demon_water_pool"), SPIRAL = t("vortex_spiral"), RIPPLE = t("waterfx_ripple"),
            RING = t("vortex_ring"), STREAK = t("vortex_streak"), GLOW = t("glow"), SPHERE = t("water_sphere"), DROP = t("water_drop"),
            FOAM = t("bubble_foam_puff"), HALO = t("bubble_halo"), GLINT = t("bubble_glint");

    private static final int DEEP = 0xFF0A2A5E, BLUE = 0xFF1B66BE, TEAL = 0xFF1FB3B6, FOAMC = 0xFFEAF8FF, RAINC = 0xFFB4C4D6, GLASS = 0xFF9ADCFF;

    private static final int WALL_N = 16, RAIN_N = 12, DROP_N = 9;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_GREAT_FLOOD); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFF2E86E0; }

    // scratch vectors: the layer draws on the render thread only, one activation at a time
    private final Vector3f q0 = new Vector3f(), q1 = new Vector3f(), q2 = new Vector3f(), q3 = new Vector3f(), tmp = new Vector3f(), tmp2 = new Vector3f();

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    /** A free quad from plain floats (p0 p1 bottom edge, p2 p3 top edge). */
    private void quad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float u0, float u1, int cBottom, int cTop) {
        if (!buf.hasBudget(4)) return;
        buf.quad(tex, bl, q0.set(x0, y0, z0), q1.set(x1, y1, z1), q2.set(x2, y2, z2), q3.set(x3, y3, z3), u0, 0f, u1, 1f, bl.grade(cBottom, 1f), bl.grade(cTop, 1f));
    }

    private void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, tmp.set(x, y, z), size, rot, col);
    }

    private void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(tmp2.set(c.x, c.y + y, c.z)).spin(spin), radius, col);
    }

    /** A standing quad facing the camera round the vertical axis: bottom width wb at height y0, top width wt at y1, top shifted sideways by sx. */
    private void stand(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float rx, float rz, float cx, float cz, float y0, float y1, float wb, float wt, float sx,
                       float u0, int cb, int ct) {
        quad(buf, tex, bl, cx - rx * wb * 0.5f, y0, cz - rz * wb * 0.5f, cx + rx * wb * 0.5f, y0, cz + rz * wb * 0.5f,
                cx + rx * (wt * 0.5f + sx), y1, cz + rz * (wt * 0.5f + sx), cx + rx * (-wt * 0.5f + sx), y1, cz + rz * (-wt * 0.5f + sx), u0, u0 + 0.5f, cb, ct);
    }

    /** One ring of water (wall / crashing wave) round c at radius rad, height h, with ragged lip; curl > 0 leans the top part outward (overhang). */
    private void ringWave(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float rad, float h, float alpha, float curl, boolean twoPiece, long seed, float age) {
        if (alpha <= 0.02f || h < 0.05f || rad < 0.2f) return;
        for (int k = 0; k < WALL_N; k++) {
            float a0 = Mth.TWO_PI * k / WALL_N, a1 = Mth.TWO_PI * (k + 1) / WALL_N;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float h0 = h * (0.78f + 0.42f * hash(seed, k % WALL_N, 1) + 0.12f * Mth.sin(k * 1.7f + age * 0.5f));
            float h1 = h * (0.78f + 0.42f * hash(seed, (k + 1) % WALL_N, 1) + 0.12f * Mth.sin((k + 1) * 1.7f + age * 0.5f));
            float u0 = 0.5f * hash(seed, k, 2);
            float x0 = c.x + c0 * rad, z0 = c.z + s0 * rad, x1 = c.x + c1 * rad, z1 = c.z + s1 * rad, y = c.y;
            if (!twoPiece) {
                quad(buf, FLOW, VfxBlend.ALPHA, x0, y, z0, x1, y, z1, x1, y + h1, z1, x0, y + h0, z0, u0, u0 + 0.5f, a(DEEP, 0.92f * alpha), a(TEAL, 0.78f * alpha));
            } else {
                float m = 0.62f, o = curl * h;
                float xm0 = c.x + c0 * (rad + 0.15f * o), zm0 = c.z + s0 * (rad + 0.15f * o), xm1 = c.x + c1 * (rad + 0.15f * o), zm1 = c.z + s1 * (rad + 0.15f * o);
                float xt0 = c.x + c0 * (rad + o), zt0 = c.z + s0 * (rad + o), xt1 = c.x + c1 * (rad + o), zt1 = c.z + s1 * (rad + o);
                quad(buf, FLOW, VfxBlend.ALPHA, x0, y, z0, x1, y, z1, xm1, y + m * h1, zm1, xm0, y + m * h0, zm0, u0, u0 + 0.5f, a(DEEP, 0.92f * alpha), a(BLUE, 0.85f * alpha));
                quad(buf, FLOW, VfxBlend.ALPHA, xm0, y + m * h0, zm0, xm1, y + m * h1, zm1, xt1, y + h1, zt1, xt0, y + h0, zt0, u0 + 0.25f, u0 + 0.75f, a(BLUE, 0.85f * alpha), a(FOAMC, 0.8f * alpha));
            }
        }
        for (int k = 0; k < WALL_N; k += 3) {                                     // foam on the lip, every other segment
            float ang = Mth.TWO_PI * (k + 0.5f) / WALL_N;
            float lr = rad + curl * h * 0.9f;
            float hh = h * (0.85f + 0.3f * hash(seed, k, 1));
            sprite(ctx, buf, FOAM, VfxBlend.ALPHA, c.x + Mth.cos(ang) * lr, c.y + hh, c.z + Mth.sin(ang) * lr, (0.9f + 0.5f * hash(seed, k, 3)) * Math.min(1.5f, 0.5f + h * 0.2f), 0f, a(FOAMC, 0.55f * alpha));
        }
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(4f, inst.power), s = R / 12f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float cx = c.x, cy = c.y, cz = c.z;
        float rx = ctx.camRight.x, rz = ctx.camRight.z, rl = Mth.sqrt(rx * rx + rz * rz);
        if (rl < 1e-3f) { rx = 1f; rz = 0f; } else { rx /= rl; rz /= rl; }

        float tail = 1f - sub(age, 16f, 20f);                                      // everything that is left fades out at the end
        float flood = VfxAnim.easeOutCubic(sub(age, 0f, 8f));
        float spinUp = 1f + 2.5f * sub(age, 5f, 11f);

        // --- the flooded ground: a dark swirling pool over the field, a bright counter-turning spiral, ripples
        ground(buf, HALO, VfxBlend.ALPHA, c, 0.04f, R * (0.4f + 0.7f * flood), 0f, a(BLUE, 0.7f * tail));
        ground(buf, POOL, VfxBlend.ALPHA, c, 0.05f, R * (0.3f + 0.72f * flood), age * 0.045f * spinUp, a(0xFF1A5C9A, 0.42f * tail * (1f - 0.7f * sub(age, 13f, 18f))));
        ground(buf, POOL, VfxBlend.ALPHA, c, 0.07f, R * (0.2f + 0.5f * flood), -age * 0.06f * spinUp, a(TEAL, 0.45f * tail * sub(age, 1f, 5f)));
        float spiralA = sub(age, 2f, 6f) * (1f - sub(age, 10.5f, 13f)) * 0.75f;
        ground(buf, SPIRAL, VfxBlend.ADD, c, 0.1f, R * 0.78f, -age * 0.12f * spinUp, a(GLASS, spiralA));
        for (int k = 0; k < 2; k++) {                                                // ripples travelling in towards the pool, then out
            float rp = (age * 0.07f + 0.5f * k) % 1f;
            float rr = age < 10.5f ? R * (1.05f - 0.9f * rp) : R * (0.15f + 1.0f * VfxAnim.easeOutCubic(sub(age, 10.5f + 1.5f * k, 19f)));
            float ra = age < 10.5f ? Mth.sin(rp * Mth.PI) * 0.5f * sub(age, 1f, 4f) : 0.55f * (1f - sub(age, 12f + k, 20f));
            ground(buf, RIPPLE, VfxBlend.ADD, c, 0.12f + 0.01f * k, rr, 0f, a(k == 0 ? FOAMC : GLASS, ra * tail));
        }

        // --- the ring-wall of water that rises at the radius, then rushes inward
        if (age < 10.8f) {
            float up = VfxAnim.easeOutCubic(sub(age, 0f, 6f));
            float in = VfxAnim.easeInOutSine(sub(age, 6.2f, 10.6f));
            float wr = R * (0.97f - 0.84f * in);
            float wh = 6.2f * s * up * (1f - 0.65f * in) + 0.25f * s * Mth.sin(age * 1.3f) * up;
            float wa = 1f - sub(age, 9.6f, 10.8f);
            ringWave(ctx, buf, c, wr, wh, wa * Math.min(1f, age / 2f), 0f, false, seed + 11, age);
        }

        // --- the woman-shaped crest of water standing up in the middle
        float rise = VfxAnim.easeOutCubic(sub(age, 3f, 9.2f));
        float wom = rise * (1f - sub(age, 10.2f, 11.2f));
        if (wom > 0.02f) {
            float H = 10.5f * s * rise, wd = s * 1.3f * (0.55f + 0.45f * rise) * (1f + 0.6f * sub(age, 9.8f, 11.5f));
            float sway = 0.18f * s * Mth.sin(age * 0.55f);
            float u = 0.5f * hash(seed, 1, 5);
            float y0 = cy, yA = cy + H * 0.30f, yB = cy + H * 0.57f, yC = cy + H * 0.76f;
            stand(buf, FLOW, VfxBlend.ALPHA, rx, rz, cx, cz, y0, yA, 4.4f * wd, 3.0f * wd, sway * 0.3f, u, a(DEEP, 1f * wom), a(BLUE, 0.95f * wom));     // skirt, lower
            stand(buf, FLOW, VfxBlend.ALPHA, rx, rz, cx, cz, yA, yB, 3.0f * wd, 1.15f * wd, sway * 0.3f, u + 0.2f, a(BLUE, 0.85f * wom), a(TEAL, 0.85f * wom));   // skirt, upper to the waist
            stand(buf, FLOW, VfxBlend.ALPHA, rx, rz, cx + rx * sway * 0.4f, cz + rz * sway * 0.4f, yB, yC, 1.25f * wd, 1.9f * wd, sway * 0.5f, u + 0.1f, a(TEAL, 0.85f * wom), a(GLASS, 0.85f * wom));   // torso to the shoulders
            stand(buf, FLOW, VfxBlend.ADD, rx, rz, cx, cz, y0, yB, 4.9f * wd, 1.4f * wd, -sway * 0.4f, u + 0.3f, a(TEAL, 0.38f * wom), a(GLASS, 0.5f * wom));       // light inside the dress
            float hx = cx + rx * sway, hz = cz + rz * sway, hy = cy + H * 0.88f;
            sprite(ctx, buf, SPHERE, VfxBlend.ALPHA, hx, hy, hz, 1.35f * wd, age * 0.1f, a(FOAMC, 0.95f * wom));                                              // head
            for (int sd = -1; sd <= 1; sd += 2) {                                                                                                               // arms raised, swept up and out
                tmp.set(cx + rx * sd * 0.85f * wd + rx * sway * 0.6f, cy + H * 0.74f, cz + rz * sd * 0.85f * wd + rz * sway * 0.6f);
                tmp2.set(cx + rx * sd * 2.7f * wd, cy + H * 0.88f + 0.4f * s * Mth.sin(age * 0.7f + sd), cz + rz * sd * 2.7f * wd);
                buf.beam(ctx, FLOW, VfxBlend.ALPHA, tmp, tmp2, 0.9f * wd, 0.3f * wd, 1, 0f, a(TEAL, 0.9f * wom), a(FOAMC, 0.8f * wom));
            }
            for (int k = 0; k < 3; k++) {                                                                                                                       // hair streaming down and back
                float side = (k - 1) * 0.55f * wd, fl = 0.5f * s * Mth.sin(age * 0.6f + k * 1.6f);
                for (int sgm = 0; sgm < 1; sgm++) {
                    float f0 = 0f, f1 = 1f;
                    tmp.set(hx + rx * (side * (1f + f0) + fl * f0), hy - f0 * H * 0.4f, hz + rz * (side * (1f + f0) + fl * f0));
                    tmp2.set(hx + rx * (side * (1f + f1) + fl * f1), hy - f1 * H * 0.4f, hz + rz * (side * (1f + f1) + fl * f1));
                    buf.beam(ctx, FLOW, VfxBlend.ALPHA, tmp, tmp2, (0.55f - 0.25f * f0) * wd, (0.55f - 0.25f * f1) * wd, 1, 0f, a(GLASS, 0.85f * wom), a(TEAL, 0.5f * wom));
                }
            }
            for (int k = 0; k < 3; k++) {                                                                                                                       // helical ribbons rising round her
                float base = Mth.TWO_PI * k / 3f + age * 0.42f;
                float px = 0, py = 0, pz = 0;
                for (int j = 0; j < 3; j++) {
                    float fj = j / 2f, ang = base + fj * 2.1f, rad = (3.1f - 1.5f * fj) * wd;
                    float x = cx + Mth.cos(ang) * rad, y = cy + H * (0.04f + 0.8f * fj) , z = cz + Mth.sin(ang) * rad;
                    if (j > 0) {
                        tmp.set(px, py, pz);
                        tmp2.set(x, y, z);
                        buf.beam(ctx, FLOW, VfxBlend.ALPHA, tmp, tmp2, 0.85f * wd * (1f - 0.18f * (j - 1)), 0.85f * wd * (1f - 0.18f * j), 1, 0f, a(k == 1 ? FOAMC : GLASS, 0.8f * wom), a(TEAL, 0.65f * wom));
                    }
                    px = x; py = y; pz = z;
                }
            }
            sprite(ctx, buf, GLOW, VfxBlend.ADD, cx, cy + H * 0.5f, cz, 9f * s * rise, 0f, a(GLASS, 0.4f * wom));                                            // the glass-blue light behind her
        }

        // --- rain streaks, falling vertical lines
        float rainA = sub(age, 0f, 3f) * (1f - sub(age, 10.5f, 17f));
        if (rainA > 0.02f) {
            int rn = ctx.seg(RAIN_N, 6);
            for (int k = 0; k < rn; k++) {
                float ph = (age * 0.3f + hash(seed, k, 20)) % 1f, ang = hash(seed, k, 21) * Mth.TWO_PI, rad = R * 1.02f * Mth.sqrt(hash(seed, k, 22));
                float len = (2.4f + 1.4f * hash(seed, k, 23)) * s, top = (9.5f - 10.5f * ph) * s + len;
                float y1 = Math.min(top, 10f * s), y0 = Math.max(0f, top - len);
                if (y1 - y0 < 0.3f) continue;
                float x = cx + Mth.cos(ang) * rad, z = cz + Mth.sin(ang) * rad, w = 0.26f * s + 0.1f * hash(seed, k, 24);
                stand(buf, STREAK, VfxBlend.ALPHA, rx, rz, x, z, cy + y0, cy + y1, w, w * 0.4f, 0f, 0f, a(0xFFD0DEEC, 0.95f * rainA), a(RAINC, 0.3f * rainA));
            }
        }

        // --- the peak: a flash of glass-blue light, then the huge crashing wave ring
        float flash = sub(age, 8.6f, 10.6f) * (1f - sub(age, 10.6f, 13f));
        if (flash > 0.01f) sprite(ctx, buf, GLOW, VfxBlend.ADD, cx, cy + 2.2f * s, cz, R * (0.9f + 0.9f * flash), 0f, a(GLASS, 0.85f * flash));
        float cr = sub(age, 10f, 19.5f);
        if (cr > 0f) {
            float rc = R * (0.1f + 1.04f * VfxAnim.easeOutCubic(cr));
            float hc = 8.6f * s * Mth.sqrt(sub(age, 10f, 11.8f)) * (1f - cr * cr * 0.9f) * (1f - 0.35f * cr);
            float ca = 1f - sub(cr, 0.72f, 1f);
            ringWave(ctx, buf, c, rc, hc, ca, 0.12f * (1f - cr), true, seed + 29, age);
            ground(buf, RING, VfxBlend.ADD, c, 0.14f, rc * 1.02f, 0f, a(FOAMC, 0.45f * ca));                       // the white front of foam over the ground
            ground(buf, RING, VfxBlend.ADD, c, 0.16f, rc * 0.84f, 0f, a(GLASS, 0.25f * ca));
            float dp = sub(age, 10.5f, 19.5f);
            for (int k = 0; k < DROP_N; k++) {                                                                       // droplets thrown off the lip, then falling
                float ang = hash(seed, k, 30) * Mth.TWO_PI, lead = 1f + 0.12f * hash(seed, k, 31) + 0.14f * dp;
                float hgt = hc * 0.8f + (7f * dp * (1f - dp) + 1.2f * hash(seed, k, 32)) * s * (1f - 0.6f * dp);
                float da = Mth.sin(Mth.clamp(dp, 0f, 1f) * Mth.PI) * 0.95f;
                sprite(ctx, buf, DROP, VfxBlend.ALPHA, cx + Mth.cos(ang) * rc * lead, cy + Math.max(0.1f, hgt), cz + Mth.sin(ang) * rc * lead, (0.5f + 0.4f * hash(seed, k, 33)) * s, 0f, a(GLASS, da));
            }
            for (int k = 0; k < 4; k++) {                                                                            // glints on the crest
                float ang = Mth.TWO_PI * (k + hash(seed, k, 34)) / 4f;
                sprite(ctx, buf, GLINT, VfxBlend.ADD, cx + Mth.cos(ang) * rc, cy + hc * (0.9f + 0.25f * Mth.sin(age + k)), cz + Mth.sin(ang) * rc, 1.6f * s, age * 0.2f + k, a(FOAMC, 0.85f * ca));
            }
        }
    }
}
