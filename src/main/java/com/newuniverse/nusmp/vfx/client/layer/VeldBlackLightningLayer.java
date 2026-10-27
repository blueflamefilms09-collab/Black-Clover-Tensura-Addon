package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.30.0 Veldanava's BLACK LIGHTNING (Tensura): a bolt of black lightning with a crimson glow. Black first, crimson second, a thread of white-hot only in the very heart.
 * It is built to read against a bright sky as well: the body is drawn with the normal alpha blend (a black ribbon on a crimson rim), only the glows are additive, and the
 * strike frame also inverts the picture round the bolt (NEGATIVE blend), so a white sky goes dark for a moment.
 * <ul>
 *   <li>0-2.4: the charge. A thin red-violet line crackles down the line of the coming bolt, a small crimson glow burns at the top, a black circle forms on the ground with a
 *       thin crimson ring and a few sparks crawl on it.</li>
 *   <li>2.4-4.4: the strike. The jagged bolt is there at once (it draws itself down in half a tick): crimson rim, black body, a faint crimson glow and a white-hot thread,
 *       with four forks. Ground flash, the world inverts round the bolt for a moment, the black circle spreads into a scorch.</li>
 *   <li>4.4-8: it re-strikes twice (dims, re-jags, strikes again, a little thinner each time, forks re-rolled). A spiked ring of shock races out to the radius
 *       (explosion-circle style: black ring, crimson ring over it), black and crimson arcs crawl along the ground, sparks fly.</li>
 *   <li>8-12: the bolt is gone, the cracks and the scorch stay and cool, embers lift off the ground and fade.</li>
 * </ul>
 * {@code inst.from} = a point high above, {@code inst.to} = where it strikes, {@code inst.power} = the impact radius (about 3). Worst case about 340 of the 400 vertices (all
 * quads; every group asks {@code hasBudget} first, the most important ones are drawn first), constant loop counts, deterministic from seed and age; {@link VfxRenderContext#seg}
 * only thins the sparks and embers. No per-frame allocation beyond a few JOML vectors: the ribbons use static scratch arrays (client render thread only).
 */
public class VeldBlackLightningLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation SHAFT = t("judgment_shaft"), THIN = t("judgment_beam"), UMBRA = t("bfx_umbra"), SPLAT = t("black_oil_splat"), FLASH = t("lightningfx_flash"),
            GLOW = t("glow"), SPARK = t("lightningfx_spark"), ARC = t("lightningfx_arc"), SPIKE_RING = t("lightningfx_ring"), SOFT_RING = t("black_oil_ripple"), THIN_RING = t("curse_ring"),
            CRACKS = t("demon_cracks"), INVERSE = t("demon_fire_inverse"), ASH = t("ash_ember");

    private static final int BLACK = 0xFF06030A, CRIMSON = 0xFFD0102E, WHITE_HOT = 0xFFFFE6EA, VIOLET = 0xFFC41E66;
    private static final int N = 8, BRANCHES = 4, BN = 2, ARCS = 4, SPARKS = 10, EMBERS = 9;
    private static final float STRIKE = 2.4f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_BLACK_LIGHTNING); }
    @Override public int defaultDuration(VfxShape s) { return 12; }
    @Override public int defaultColor(VfxShape s) { return CRIMSON; }

    // ------------------------------------------------------------------ helpers
    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static int shade(int argb, float k) {
        return (argb & 0xFF000000) | ((int) (((argb >> 16) & 255) * k) << 16) | ((int) (((argb >> 8) & 255) * k) << 8) | (int) ((argb & 255) * k);
    }
    /** Vertex colour for the NEGATIVE blend (alpha is ignored there, the RGB is how hard the picture behind is inverted), with a faint crimson lean. */
    private static int negative(float g) { return 0xFF000000 | ((int) (cl(g) * 235f) << 16) | ((int) (cl(g) * 205f) << 8) | (int) (cl(g) * 215f); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (size > 0.01f && buf.hasBudget(4)) buf.billboard(ctx, tex, bl, new Vector3f(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(new Vector3f(c).add(0f, y, 0f)).spin(spin), radius, col);
    }

    // scratch arrays for the ribbons (the client render thread is the only one that draws)
    private static final float[] PX = new float[N + 2], PY = new float[N + 2], PZ = new float[N + 2], PW = new float[N + 2];
    private static final float[][] BX = new float[BRANCHES][BN + 2], BY = new float[BRANCHES][BN + 2], BZ = new float[BRANCHES][BN + 2];
    private static final float[] SX = new float[N + 2], SY = new float[N + 2], SZ = new float[N + 2];
    private static final Vector3f Q0 = new Vector3f(), Q1 = new Vector3f(), Q2 = new Vector3f(), Q3 = new Vector3f();

    /**
     * A continuous ribbon through the points 0..n (camera-facing, the sides shared between neighbouring legs so there are no gaps at the kinks). 'w' is the full width at each
     * point times 'wm'; colour fades from 'cTop' (point 0) to 'cBot' (point n).
     */
    private static void ribbon(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float[] x, float[] y, float[] z, float[] w, int n, float wm, int cTop, int cBot) {
        for (int i = 0; i <= n; i++) {
            int i0 = Math.max(0, i - 1), i1 = Math.min(n, i + 1);
            float tx = x[i1] - x[i0], ty = y[i1] - y[i0], tz = z[i1] - z[i0];
            float vl = Mth.sqrt(x[i] * x[i] + y[i] * y[i] + z[i] * z[i]) + 1e-4f;
            float vx = -x[i] / vl, vy = -y[i] / vl, vz = -z[i] / vl;
            float sx = ty * vz - tz * vy, sy = tz * vx - tx * vz, sz = tx * vy - ty * vx;
            float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
            if (sl < 1e-4f) { sx = ctx.camRight.x; sy = ctx.camRight.y; sz = ctx.camRight.z; sl = 1f; }
            float k = 0.5f * w[i] * wm / sl;
            SX[i] = sx * k; SY[i] = sy * k; SZ[i] = sz * k;
        }
        for (int i = 0; i < n; i++) {
            if (!buf.hasBudget(4)) return;
            int c0 = bl.grade(VfxVertexBuffer.lerpColor(cTop, cBot, i / (float) n), 1f), c1 = bl.grade(VfxVertexBuffer.lerpColor(cTop, cBot, (i + 1) / (float) n), 1f);
            Q0.set(x[i] - SX[i], y[i] - SY[i], z[i] - SZ[i]);
            Q1.set(x[i] + SX[i], y[i] + SY[i], z[i] + SZ[i]);
            Q2.set(x[i + 1] + SX[i + 1], y[i + 1] + SY[i + 1], z[i + 1] + SZ[i + 1]);
            Q3.set(x[i + 1] - SX[i + 1], y[i + 1] - SY[i + 1], z[i + 1] - SZ[i + 1]);
            buf.quad(tex, bl, Q0, Q1, Q2, Q3, 0f, 0f, 1f, 1f, c0, c1);
        }
    }

    /** A ribbon lying flat on the ground (a crack of lightning crawling over it) as one quad of the jagged arc texture, from 'c' outward at angle 'ang'. */
    private static void arcQuad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float ang, float from, float len, float width, int col) {
        if (len < 0.1f || !buf.hasBudget(4)) return;
        float cx = Mth.cos(ang), cz = Mth.sin(ang), sx = -cz * width * 0.5f, sz = cx * width * 0.5f;
        float ix = c.x + cx * from, iz = c.z + cz * from, ox = c.x + cx * (from + len), oz = c.z + cz * (from + len);
        int cc = bl.grade(col, 1f);
        Q0.set(ix - sx, c.y + y, iz - sz);
        Q1.set(ox - sx, c.y + y, oz - sz);
        Q2.set(ox + sx, c.y + y, oz + sz);
        Q3.set(ix + sx, c.y + y, iz + sz);
        buf.quad(tex, bl, Q0, Q1, Q2, Q3, 0f, 0f, 1f, 1f, cc, cc);
    }

    // ------------------------------------------------------------------ the bolt
    /** The jagged path from 'top' to 'hit' into PX..PZ (pinned at both ends, the zig-zag alternates sides so the kinks are sharp) and the width factors into PW. */
    private static void path(long seed, int roll, Vector3f top, Vector3f hit, float amp, float taper) {
        float dx = hit.x - top.x, dy = hit.y - top.y, dz = hit.z - top.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4f;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        // two axes square to the bolt
        float ux = Math.abs(ny) > 0.9f ? 1f : 0f, uy = Math.abs(ny) > 0.9f ? 0f : 1f;
        float e1x = ny * 0f - nz * uy, e1y = nz * ux - nx * 0f, e1z = nx * uy - ny * ux;
        float el = Mth.sqrt(e1x * e1x + e1y * e1y + e1z * e1z) + 1e-4f;
        e1x /= el; e1y /= el; e1z /= el;
        float e2x = ny * e1z - nz * e1y, e2y = nz * e1x - nx * e1z, e2z = nx * e1y - ny * e1x;
        float ang = hash(seed, roll, 1) * Mth.TWO_PI;
        for (int i = 0; i <= N; i++) {
            float tt = i / (float) N;
            // the legs are not evenly long: a few long runs and a few short, hard kinks
            float u = tt + (i > 0 && i < N ? (hash(seed, roll * 31 + i, 2) - 0.5f) * 0.07f : 0f);
            float env = i == 0 || i == N ? 0f : (float) Math.pow(Mth.sin(Mth.clamp(tt, 0f, 1f) * Mth.PI), 0.55);
            ang += Mth.PI + (hash(seed, roll * 31 + i, 3) - 0.5f) * 1.5f;
            float mag = amp * env * (0.45f + 0.55f * hash(seed, roll * 31 + i, 4));
            float ox = Mth.cos(ang) * mag, oz = Mth.sin(ang) * mag;
            PX[i] = top.x + dx * u + e1x * ox + e2x * oz;
            PY[i] = top.y + dy * u + e1y * ox + e2y * oz;
            PZ[i] = top.z + dz * u + e1z * ox + e2z * oz;
            PW[i] = Mth.lerp(tt, taper, 1f);
        }
        PX[N] = hit.x; PY[N] = hit.y; PZ[N] = hit.z;
    }

    // ------------------------------------------------------------------ render
    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.5f, inst.power), u = R / 3f;
        long seed = inst.seed;
        int crim = 0xFF000000 | (inst.color & 0xFFFFFF);
        int rim = shade(crim, 0.72f), glowC = crim;
        Vector3f top = ctx.rel(inst.from(ctx)), hit = ctx.rel(inst.to(ctx));
        float len = hit.distance(top);
        if (len < 1f || age >= 12f) return;
        Vector3f gc = new Vector3f(hit.x, hit.y, hit.z);                                  // the impact point (on the ground)

        // ---- the envelope of the strikes: strike at 2.4, dips at 4.4 and 6.5 (each re-strike is a new jag), gone by 8.3
        int roll = age < 4.5f ? 0 : age < 6.5f ? 1 : 2;
        float dip1 = 1f - 0.9f * (1f - sub(Math.abs(age - 4.45f), 0f, 0.5f)), dip2 = 1f - 0.9f * (1f - sub(Math.abs(age - 6.5f), 0f, 0.45f));
        float bolt = (age < STRIKE ? 0f : 1f) * (1f - sub(age, 6.9f, 8.4f)) * dip1 * dip2 * (roll == 2 ? 0.78f : 1f);
        float flick = 0.88f + 0.12f * hash(seed, (int) (age * 3f), 5);
        bolt *= flick;
        float reveal = sub(age, STRIKE, STRIKE + 0.45f);                                  // it draws itself down in half a tick
        float wsc = roll == 0 ? 1f : roll == 1 ? 0.8f : 0.62f;

        // ---- 1. the bolt: crimson rim, black body, faint crimson glow, white-hot thread (submitted in this order: later layers land on top)
        if (bolt > 0.02f && reveal > 0f) {
            path(seed, roll, top, hit, 0.065f * len + 0.3f * R, 0.5f);
            int n = N;
            if (reveal < 1f) {                                                             // the tip is still on its way down
                float ft = reveal * N;
                n = Math.min(N, (int) Math.ceil(ft));
                int m = Math.max(1, n);
                float f = ft - (m - 1);
                PX[m] = Mth.lerp(f, PX[m - 1], PX[m]); PY[m] = Mth.lerp(f, PY[m - 1], PY[m]); PZ[m] = Mth.lerp(f, PZ[m - 1], PZ[m]);
                n = m;
            }
            float w = u * wsc;
            ribbon(ctx, buf, SHAFT, VfxBlend.ALPHA, PX, PY, PZ, PW, n, 2.5f * w, a(rim, 0.0f), a(rim, 1f * bolt));
            ribbon(ctx, buf, SHAFT, VfxBlend.ALPHA, PX, PY, PZ, PW, n, 1.8f * w, a(BLACK, 0.0f), a(BLACK, 1f * bolt));
            ribbon(ctx, buf, SHAFT, VfxBlend.ALPHA, PX, PY, PZ, PW, n, 1.45f * w, a(BLACK, 0.0f), a(BLACK, 1f * bolt));
            ribbon(ctx, buf, THIN, VfxBlend.ADD, PX, PY, PZ, PW, n, 0.3f * w, a(WHITE_HOT, 0.0f), a(WHITE_HOT, 0.95f * bolt));

            // ---- the forks (4 on the first strike, 3 on the re-strikes), re-rolled each strike
            if (reveal >= 1f) {
                int forks = roll == 0 ? BRANCHES : BRANCHES - 1;
                for (int k = 0; k < forks; k++) {
                    int i0 = 2 + (int) (hash(seed, roll * 7 + k, 11) * (N - 3));
                    float ang = hash(seed, roll * 7 + k, 12) * Mth.TWO_PI, fl = (0.10f + 0.07f * hash(seed, roll * 7 + k, 13)) * len;
                    BX[k][0] = PX[i0]; BY[k][0] = PY[i0]; BZ[k][0] = PZ[i0];
                    float dirA = ang;
                    for (int j = 1; j <= BN; j++) {
                        dirA += (hash(seed, roll * 7 + k, 20 + j) - 0.5f) * 2.0f;
                        float step = fl / BN * (0.7f + 0.6f * hash(seed, roll * 7 + k, 30 + j));
                        BX[k][j] = BX[k][j - 1] + Mth.cos(dirA) * step * 0.85f;
                        BY[k][j] = BY[k][j - 1] - step * 0.55f;
                        BZ[k][j] = BZ[k][j - 1] + Mth.sin(dirA) * step * 0.85f;
                    }
                    BRW[0] = 0.7f; BRW[1] = 0.4f; BRW[2] = 0.12f;
                    float fa = bolt * (0.85f - 0.1f * k);
                    ribbon(ctx, buf, SHAFT, VfxBlend.ALPHA, BX[k], BY[k], BZ[k], BRW, BN, 2.0f * w, a(rim, 0.95f * fa), a(rim, 0.0f));
                    ribbon(ctx, buf, SHAFT, VfxBlend.ALPHA, BX[k], BY[k], BZ[k], BRW, BN, 1.15f * w, a(BLACK, 1f * fa), a(BLACK, 0.0f));
                }
            }
        }

        // ---- 2. the charge: the thin red-violet line, the glow at the top, the black circle forming on the ground, a few sparks on it
        float pre = (1f - sub(age, STRIKE - 0.3f, STRIKE + 0.2f)) * sub(age, 0f, 0.7f);
        if (pre > 0.01f) {
            float tw = 0.6f + 0.4f * Mth.sin(age * 5.3f + hash(seed, 0, 6) * 6f);
            PX[0] = top.x; PY[0] = top.y; PZ[0] = top.z;
            PX[1] = hit.x + (top.x - hit.x) * 0.5f + 0.35f * u * Mth.sin(age * 3.1f);
            PY[1] = (top.y + hit.y) * 0.5f;
            PZ[1] = hit.z + (top.z - hit.z) * 0.5f + 0.35f * u * Mth.cos(age * 2.7f);
            PX[2] = hit.x; PY[2] = hit.y; PZ[2] = hit.z;
            PW[0] = 0.5f; PW[1] = 0.8f; PW[2] = 1f;
            ribbon(ctx, buf, THIN, VfxBlend.ALPHA, PX, PY, PZ, PW, 2, 0.8f * u, a(0xFF2A0618, 0.0f), a(0xFF2A0618, 0.8f * pre * tw));
            ribbon(ctx, buf, THIN, VfxBlend.ADD, PX, PY, PZ, PW, 2, 0.55f * u, a(VIOLET, 0.0f), a(VIOLET, 0.95f * pre * tw));
        }
        float form = VfxAnim.easeOutCubic(sub(age, 0f, STRIKE));
        float spread = VfxAnim.easeOutCubic(sub(age, STRIKE, STRIKE + 3.2f));
        float circleOut = 1f - sub(age, 9f, 12f);
        if (circleOut > 0.01f) {
            float cr = R * (0.22f + 0.3f * form + 0.5f * spread);
            ground(buf, UMBRA, VfxBlend.ALPHA, gc, 0.04f, cr * 1.25f, 0f, a(BLACK, (0.55f * sub(age, 0f, 1.6f) + 0.4f * spread) * circleOut));
            float ringA = sub(age, 0.2f, 1.2f) * (1f - sub(age, STRIKE, STRIKE + 0.8f));
            ground(buf, THIN_RING, VfxBlend.ADD, gc, 0.06f, R * (0.3f + 0.28f * form), age * 0.35f, a(VIOLET, 0.9f * ringA));
            ground(buf, THIN_RING, VfxBlend.ALPHA, gc, 0.055f, R * (0.3f + 0.28f * form) * 1.02f, age * 0.35f, a(BLACK, 0.8f * ringA));
        }
        if (age < STRIKE + 0.5f) {
            float g = sub(age, 0.4f, 1.4f) * (1f - sub(age, STRIKE, STRIKE + 0.5f));
            sprite(ctx, buf, GLOW, VfxBlend.ADD, top.x, top.y, top.z, 3.2f * u * (0.8f + 0.2f * Mth.sin(age * 7f)), 0f, a(VIOLET, 0.6f * g));
            int nsp = ctx.seg(4, 2);
            for (int k = 0; k < nsp; k++) {                                                    // crackle sparks hopping over the circle
                float ph = hash(seed, k, 7), ang = ph * Mth.TWO_PI + age * (1.5f + ph), rr = R * (0.15f + 0.35f * hash(seed, k, 8));
                float on = 0.5f + 0.5f * Mth.sin(age * 9f + k * 2.3f);
                sprite(ctx, buf, SPARK, VfxBlend.ADD, gc.x + Mth.cos(ang) * rr, gc.y + 0.15f + 0.25f * u * on, gc.z + Mth.sin(ang) * rr, 0.45f * u * (0.5f + on), age * 2f + k,
                        a(k % 2 == 0 ? VIOLET : crim, 0.9f * sub(age, 0.3f, 1.0f) * (1f - sub(age, STRIKE, STRIKE + 0.4f))));
            }
        }

        // ---- 3. the impact: ground flash, the shock ring, the inverted flash, the cracks and the scorch
        if (age >= STRIKE) {
            float f = age - STRIKE;
            float flash = 1f - sub(age, STRIKE, STRIKE + 2.4f);
            float restrike = 1f - sub(Math.abs(age - 4.95f), 0f, 0.9f) + (1f - sub(Math.abs(age - 6.95f), 0f, 0.8f)) * 0.6f;
            float fl = Math.max(flash, 0.55f * Math.min(1f, restrike) * (1f - sub(age, 7f, 8.6f)));
            if (fl > 0.02f) {
                float fe = VfxAnim.easeOutCubic(sub(f, 0f, 2.4f));
                sprite(ctx, buf, FLASH, VfxBlend.ADD, gc.x, gc.y + 0.3f, gc.z, R * (1.1f + 1.2f * fe), age * 0.2f, a(WHITE_HOT, 0.8f * fl));
                sprite(ctx, buf, GLOW, VfxBlend.ADD, gc.x, gc.y + 0.4f, gc.z, R * (1.4f + 1.8f * fe), 0f, a(glowC, 0.75f * fl));
                float ng = fl * (age < 4.4f ? 1f : 0.5f);
                sprite(ctx, buf, INVERSE, VfxBlend.NEGATIVE, gc.x, gc.y + 0.5f, gc.z, R * (1.6f + 1.4f * fe), 0f, negative(0.8f * ng));
            }
            // the scorch (it is the black circle that stays), then the cracks over it
            float scorchIn = sub(age, STRIKE + 0.4f, STRIKE + 2.2f);
            if (scorchIn > 0f && circleOut > 0.01f) {
                ground(buf, SPLAT, VfxBlend.ALPHA, gc, 0.05f, R * (0.55f + 0.4f * scorchIn), hash(seed, 0, 9) * Mth.TWO_PI, a(BLACK, 0.92f * scorchIn * circleOut));
                float crackHeat = 1f - sub(age, 4f, 11.5f);
                ground(buf, CRACKS, VfxBlend.ALPHA, gc, 0.07f, R * (0.55f + 0.45f * scorchIn), hash(seed, 0, 10) * Mth.TWO_PI, a(BLACK, 0.85f * circleOut));
                ground(buf, CRACKS, VfxBlend.ADD, gc, 0.09f, R * (0.5f + 0.42f * scorchIn), hash(seed, 0, 10) * Mth.TWO_PI, a(glowC, 0.9f * crackHeat * circleOut));
            }
            // the ring of shock racing out (explosion-circle style: black ring, the crimson ring on top of it, a second smaller one on the first re-strike)
            float wave = VfxAnim.easeOutCubic(sub(age, STRIKE + 0.2f, 8.2f));
            float waveA = 1f - sub(age, 5.5f, 8.8f);
            if (wave > 0f && waveA > 0.01f) {
                float rr = R * (0.18f + 1.12f * wave);
                ground(buf, SOFT_RING, VfxBlend.ALPHA, gc, 0.1f, rr * 1.08f, 0f, a(BLACK, 0.95f * waveA));
                ground(buf, SPIKE_RING, VfxBlend.ALPHA, gc, 0.11f, rr, age * 0.1f, a(BLACK, 0.85f * waveA));
                ground(buf, SPIKE_RING, VfxBlend.ADD, gc, 0.12f, rr * 0.99f, age * 0.1f, a(glowC, 0.95f * waveA));
            }
            float wave2 = VfxAnim.easeOutCubic(sub(age, 4.6f, 8.6f)), wave2A = 1f - sub(age, 6.2f, 8.8f);
            if (wave2 > 0f && wave2A > 0.01f) ground(buf, THIN_RING, VfxBlend.ADD, gc, 0.13f, R * (0.15f + 0.6f * wave2), -age * 0.2f, a(VIOLET, 0.8f * wave2A));

            // ---- black and crimson arcs crawling over the ground (they re-jag with every strike)
            float arcLife = sub(age, STRIKE + 0.3f, STRIKE + 1.0f) * (1f - sub(age, 7f, 10f));
            if (arcLife > 0.02f) {
                int arcRoll = (int) (age * 1.4f);
                for (int k = 0; k < ARCS; k++) {
                    float ang = Mth.TWO_PI * (k + 0.8f * hash(seed, arcRoll * 5 + k, 14)) / ARCS + (float) arcRoll * 0.4f;
                    float reach = R * (0.55f + 0.5f * hash(seed, arcRoll * 5 + k, 15)) * VfxAnim.easeOutCubic(sub(age, STRIKE + 0.3f, STRIKE + 2.6f));
                    float fa = arcLife * (0.55f + 0.45f * hash(seed, arcRoll * 5 + k, 16));
                    arcQuad(buf, ARC, VfxBlend.ALPHA, gc, 0.14f, ang, R * 0.12f, reach, R * 0.32f, a(BLACK, 0.95f * fa));
                    arcQuad(buf, ARC, VfxBlend.ADD, gc, 0.15f, ang, R * 0.12f, reach, R * 0.26f, a(glowC, 0.95f * fa));
                }
            }

            // ---- sparks thrown out (black ones and crimson ones), fast, on short arcs
            float sk = sub(age, STRIKE, 8f);
            if (sk > 0f && sk < 1f) {
                int ns = ctx.seg(SPARKS, 4);
                for (int k = 0; k < ns; k++) {
                    float ph = hash(seed, k, 17), ang = hash(seed, k, 18) * Mth.TWO_PI, sp = R * (0.5f + 1.1f * hash(seed, k, 19));
                    float e = VfxAnim.easeOutCubic(sk), life = 1f - sk;
                    float x = gc.x + Mth.cos(ang) * sp * e, z = gc.z + Mth.sin(ang) * sp * e, y = gc.y + 0.2f + R * (0.35f + 0.8f * ph) * Mth.sin(Mth.PI * Math.min(1f, e * 1.1f)) * 0.8f;
                    boolean dark = k % 2 == 0;
                    sprite(ctx, buf, SPARK, dark ? VfxBlend.ALPHA : VfxBlend.ADD, x, y, z, (0.5f + 0.4f * ph) * u * (0.4f + 0.6f * life), age * 1.3f + k, dark ? a(BLACK, 0.95f * life) : a(k % 4 == 1 ? WHITE_HOT : glowC, 0.95f * life));
                }
            }
        }

        // ---- 4. embers lifting off the scorch while it cools
        float em = sub(age, 7.5f, 9.5f) * (1f - sub(age, 11f, 12f));
        if (em > 0.01f) {
            int ne = ctx.seg(EMBERS, 3);
            for (int k = 0; k < ne; k++) {
                float ang = hash(seed, k, 21) * Mth.TWO_PI, rr = R * 0.85f * Mth.sqrt(hash(seed, k, 22));
                float rise = (0.3f + 1.4f * hash(seed, k, 23)) * u + 0.22f * u * (age - 7.5f) * (0.5f + hash(seed, k, 24));
                float tw = 0.55f + 0.45f * Mth.sin(age * 1.6f + k * 2.1f);
                sprite(ctx, buf, ASH, VfxBlend.ADD, gc.x + Mth.cos(ang) * rr + 0.2f * u * Mth.sin(age * 0.6f + k), gc.y + 0.1f + rise, gc.z + Mth.sin(ang) * rr, (0.28f + 0.2f * hash(seed, k, 25)) * u * 1.8f, 0f, a(k % 3 == 0 ? WHITE_HOT : glowC, 0.95f * em * tw));
            }
        }
    }

    private static final float[] BRW = new float[BN + 2];
}
