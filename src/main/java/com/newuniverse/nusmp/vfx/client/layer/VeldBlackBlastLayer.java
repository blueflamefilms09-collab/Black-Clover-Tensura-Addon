package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.30.0 Veldanava's Black Lightning Blast, the area version of Black Lightning: black, crimson, hot violet and a little white. {@code from = to} = the caster's feet,
 * {@code inst.power} = the blast radius (~16), 26 ticks.
 * <ul>
 *   <li>0-9: a magic circle spins open on the ground (violet and crimson sigils over a scorched dark disc) and a black orb gathers at chest height, pulling in thin black arcs.</li>
 *   <li>9-11: the orb collapses to a point and flashes (a white-violet star).</li>
 *   <li>11-19: the blast: a ring races out to the full radius, ten long jagged black bolts with crimson edges lash out along the ground and up (they re-jitter every two
 *       ticks), a dark dome of smoke flickers over the middle.</li>
 *   <li>19-26: ring and bolts fade, arcs crawl on the ground, black motes lift.</li>
 * </ul>
 * Worst case about 360 of the 400 vertices (all quads, tested with {@code buf.hasBudget(4)} before each), constant loop counts, nothing random that is not derived from the
 * seed and the age; {@link VfxRenderContext#seg} only thins the optional parts (arcs, bolt segments, motes). Black parts use ALPHA (ADD cannot draw black), the glow ADD.
 */
public class VeldBlackBlastLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation ORB = t("darkfx_orb"), CORONA = t("darkfx_corona"), UMBRA = t("bfx_umbra"), RUNE = t("darkrb_rune"), CIRCLE = t("magic_circle"),
            DEVIL = t("devil_circle"), SHOCK = t("darkfx_shock"), HALO = t("bfx_halo"), BOLT = t("curse_bolt"), ARC = t("lightningfx_arc"), FLASH = t("lightningfx_flash"),
            GLOW = t("glow"), SPARK = t("lightningfx_spark"), MOTE = t("darkfx_mote"), CRACK = t("bfx_ground_crack"), SMOKE = t("darkfx_ripple"), ELRING = t("lightningfx_ring");

    private static final int BLACK = 0xFF07030C, CRIMSON = 0xFFE0153C, DEEP_RED = 0xFF8E0A26, VIOLET = 0xFFA64CFF, DEEP_VIOLET = 0xFF5B1B9E, WHITE = 0xFFF6EBFF;
    private static final int ARCS_IN = 6, BOLTS = 10, BOLT_SEGS = 3, CRAWLS = 5, MOTES = 7;
    private static final float COLLAPSE = 8.0f, BLAST = 11.0f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_BLACK_BLAST); }
    @Override public int defaultDuration(VfxShape s) { return 26; }
    @Override public int defaultColor(VfxShape s) { return CRIMSON; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (size > 0.02f && buf.hasBudget(4)) buf.billboard(ctx, tex, bl, new Vector3f(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(new Vector3f(c).add(0f, y, 0f)).spin(spin), radius, col);
    }

    /** A ribbon from p to q turned to face the camera round its own axis; texture length along u (flat sprites such as the arc stripe). */
    private static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f p, Vector3f q, float w0, float w1, int col) {
        if (!buf.hasBudget(4)) return;
        float dx = q.x - p.x, dy = q.y - p.y, dz = q.z - p.z;
        float mx = (p.x + q.x) * -0.5f, my = (p.y + q.y) * -0.5f, mz = (p.z + q.z) * -0.5f;
        float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-5f) return;
        sx /= sl; sy /= sl; sz /= sl;
        int c = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(p.x - sx * w0 * 0.5f, p.y - sy * w0 * 0.5f, p.z - sz * w0 * 0.5f), new Vector3f(q.x - sx * w1 * 0.5f, q.y - sy * w1 * 0.5f, q.z - sz * w1 * 0.5f),
                new Vector3f(q.x + sx * w1 * 0.5f, q.y + sy * w1 * 0.5f, q.z + sz * w1 * 0.5f), new Vector3f(p.x + sx * w0 * 0.5f, p.y + sy * w0 * 0.5f, p.z + sz * w0 * 0.5f),
                0f, 0f, 1f, 1f, c, c);
    }

    /** The same ribbon for the tall bolt texture (length along v, tip at the bottom), u flipped when 'flip'. */
    private static void bolt(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f p, Vector3f q, float w, boolean flip, int col) {
        if (!buf.hasBudget(4)) return;
        float dx = q.x - p.x, dy = q.y - p.y, dz = q.z - p.z;
        float mx = (p.x + q.x) * -0.5f, my = (p.y + q.y) * -0.5f, mz = (p.z + q.z) * -0.5f;
        float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-5f) return;
        float k = w * 0.5f / sl;
        sx *= k; sy *= k; sz *= k;
        int c = bl.grade(col, 1f);
        float u0 = flip ? 1f : 0f, u1 = flip ? 0f : 1f;
        buf.quad(tex, bl, new Vector3f(p.x - sx, p.y - sy, p.z - sz), new Vector3f(p.x + sx, p.y + sy, p.z + sz), new Vector3f(q.x + sx, q.y + sy, q.z + sz), new Vector3f(q.x - sx, q.y - sy, q.z - sz),
                u0, 0f, u1, 1f, c, c);
    }

    /** The point i of bolt k (0..BOLT_SEGS) at this roll: out along its heading, jittered sideways and up. Writes into 'o'. */
    private static Vector3f boltPoint(Vector3f o, Vector3f c, long seed, int roll, int k, int i, float reach, float R) {
        float head = Mth.TWO_PI * k / BOLTS + 0.5f * (hash(seed, k, 1) - 0.5f);
        boolean up = k % 3 == 1;
        float elev = up ? 0.7f + 0.5f * hash(seed, k, 2) : 0.1f + 0.14f * hash(seed, k, 2);
        float f = i / (float) BOLT_SEGS, r = reach * (0.06f + 0.94f * f);
        float jit = R * 0.1f * (i == 0 ? 0.2f : 1f) * Mth.sin(Mth.PI * Math.min(1f, f * 1.1f) + 0.3f);
        float side = (hash(seed + roll * 977L, k, 10 + i) - 0.5f) * 2f * jit, lift = (hash(seed + roll * 983L, k, 20 + i) - 0.5f) * 2f * jit * 0.5f;
        float ca = Mth.cos(head), sa = Mth.sin(head), ce = Mth.cos(elev), se = Mth.sin(elev);
        float x = ca * r * ce - sa * side, z = sa * r * ce + ca * side;
        float y = 1.0f + r * se + lift + (up ? 0f : 0.15f * R / 16f * (i % 2));
        return o.set(c.x + x, Math.max(c.y + 0.12f, c.y + y), c.z + z);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(4f, inst.power), s = R / 16f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f centre = new Vector3f(c.x, c.y + 1.2f, c.z);
        float out = 1f - sub(age, 21f, 26f);
        if (out <= 0.01f) return;
        int roll = (int) (age / 2f);

        float gather = sub(age, 0f, COLLAPSE), collapse = sub(age, COLLAPSE, BLAST - 0.8f), blastT = sub(age, BLAST, BLAST + 8f);
        float wave = VfxAnim.easeOutCubic(sub(age, BLAST - 0.3f, 19.5f));
        float flash = 1f - sub(age, BLAST - 1.2f, BLAST + 4.5f);

        // ---- ground: a scorched dark disc, then the circle that spins open (violet over crimson, turning against each other)
        float open = VfxAnim.easeOutBack(sub(age, 0f, 7f));
        float dark = sub(age, 0f, 4f) * (1f - 0.6f * sub(age, 20f, 26f));
        ground(buf, UMBRA, VfxBlend.ALPHA, c, 0.04f, R * (0.45f + 0.55f * wave) * (0.4f + 0.6f * sub(age, 0f, 5f)), 0f, a(BLACK, 0.55f * dark * out));
        float circ = (0.34f + 0.2f * VfxAnim.easeInOutSine(gather)) * (1f - 0.35f * sub(age, BLAST, BLAST + 6f)) ;
        float cs = sub(age, 0.5f, 3f) * (1f - 0.8f * sub(age, BLAST + 3f, 24f));
        ground(buf, RUNE, VfxBlend.ALPHA, c, 0.06f, R * circ * open, -age * 0.09f, a(DEEP_RED, 0.95f * cs * out));
        ground(buf, RUNE, VfxBlend.ADD, c, 0.07f, R * circ * open, -age * 0.09f, a(CRIMSON, 0.4f * cs * out));
        ground(buf, CIRCLE, VfxBlend.ALPHA, c, 0.08f, R * circ * 0.74f * open, age * 0.13f, a(DEEP_VIOLET, 0.95f * cs * out));
        ground(buf, CIRCLE, VfxBlend.ADD, c, 0.09f, R * circ * 0.74f * open, age * 0.13f, a(VIOLET, 0.4f * cs * out));
        ground(buf, SHOCK, VfxBlend.ADD, c, 0.1f, R * (circ * 1.12f) * open, age * 0.03f, a(DEEP_VIOLET, 0.6f * cs * out * (1f - sub(age, BLAST, BLAST + 2f))));

        // ---- the orb gathering at chest height, tugging thin black arcs in
        float pulse = 0.5f + 0.5f * Mth.sin(age * 1.7f);
        float orbR = s * (0.4f + 2.3f * VfxAnim.easeOutCubic(gather)) * (1f - 0.94f * collapse * collapse) * (1f + 0.07f * pulse * (1f - collapse));
        if (age < BLAST - 0.8f && orbR > 0.03f) {
            sprite(ctx, buf, CORONA, VfxBlend.ADD, centre.x, centre.y, centre.z, orbR * 3.6f, age * 0.12f, a(VIOLET, 0.75f * (0.5f + 0.5f * gather) * (1f - collapse * 0.6f)));
            sprite(ctx, buf, ORB, VfxBlend.ALPHA, centre.x, centre.y, centre.z, orbR * 2.0f, 0f, a(0xFF1A0833, 1f));
            sprite(ctx, buf, ORB, VfxBlend.ALPHA, centre.x, centre.y, centre.z, orbR * 1.55f, 0f, a(BLACK, 1f));
            sprite(ctx, buf, GLOW, VfxBlend.ADD, centre.x, centre.y, centre.z, orbR * 2.6f, 0f, a(CRIMSON, 0.55f * gather));
        }
        float arcFade = sub(age, 0.8f, 2.5f) * (1f - sub(age, COLLAPSE, COLLAPSE + 2.2f));
        if (arcFade > 0.01f) {
            int n = ctx.seg(ARCS_IN, 3);
            for (int k = 0; k < n; k++) {
                float ang = Mth.TWO_PI * k / n + 0.7f * hash(seed, k, 40), rr = R * (0.28f + 0.2f * hash(seed, k, 41)) * (1f - 0.55f * VfxAnim.easeInCubic(gather));
                float hy = (0.4f + 2.6f * hash(seed, k, 42)) * s;
                float cx0 = c.x + Mth.cos(ang) * rr, cz0 = c.z + Mth.sin(ang) * rr, cy0 = c.y + hy;
                float tx = Mth.lerp(0.78f, cx0, centre.x), ty = Mth.lerp(0.78f, cy0, centre.y), tz = Mth.lerp(0.78f, cz0, centre.z);
                float j = R * 0.035f * (0.5f + hash(seed + roll * 31L, k, 43));
                float mxp = (cx0 + tx) * 0.5f + (hash(seed + roll * 37L, k, 44) - 0.5f) * 2f * j, myp = (cy0 + ty) * 0.5f + (hash(seed + roll * 41L, k, 45) - 0.5f) * 2f * j * 0.6f,
                        mzp = (cz0 + tz) * 0.5f + (hash(seed + roll * 43L, k, 46) - 0.5f) * 2f * j;
                Vector3f p0 = new Vector3f(cx0, cy0, cz0), p1 = new Vector3f(mxp, myp, mzp), p2 = new Vector3f(tx, ty, tz), p3 = new Vector3f(centre);
                float w = s * 0.85f, al = arcFade * (0.7f + 0.3f * hash(seed + roll * 5L, k, 47));
                strip(buf, ARC, VfxBlend.ADD, p0, p1, w * 2.4f, w * 2.0f, a(DEEP_VIOLET, 0.55f * al));
                strip(buf, ARC, VfxBlend.ADD, p1, p2, w * 2.0f, w * 1.5f, a(DEEP_VIOLET, 0.55f * al));
                strip(buf, ARC, VfxBlend.ALPHA, p0, p1, w * 1.1f, w, a(BLACK, 0.95f * al));
                strip(buf, ARC, VfxBlend.ALPHA, p1, p2, w, w * 0.8f, a(BLACK, 0.95f * al));
                strip(buf, ARC, VfxBlend.ALPHA, p2, p3, w * 0.8f, w * 0.5f, a(BLACK, 0.95f * al));
            }
        }

        // ---- the collapse and the flash
        if (flash > 0f && age >= BLAST - 1.2f) {
            float fe = 1f - flash;
            sprite(ctx, buf, FLASH, VfxBlend.ADD, centre.x, centre.y, centre.z, R * (0.5f + 0.8f * fe), age * 0.15f, a(WHITE, 0.95f * flash));
            sprite(ctx, buf, GLOW, VfxBlend.ADD, centre.x, centre.y, centre.z, R * (0.45f + 0.7f * fe), 0f, a(CRIMSON, 0.9f * flash));
        } else if (collapse > 0.3f && age < BLAST - 1.2f) {
            sprite(ctx, buf, GLOW, VfxBlend.ADD, centre.x, centre.y, centre.z, s * (1.5f + 3f * collapse), 0f, a(WHITE, 0.8f * collapse));
        }

        // ---- the blast: shock ring, a dark smoky dome, then the long jagged bolts
        if (wave > 0f) {
            float wf = 1f - sub(age, 17f, 25f);
            ground(buf, SHOCK, VfxBlend.ADD, c, 0.13f, R * 1.03f * wave, age * 0.05f, a(CRIMSON, 0.95f * wf * out));
            ground(buf, ELRING, VfxBlend.ADD, c, 0.12f, R * 0.98f * wave, -age * 0.08f, a(VIOLET, 0.85f * wf * out));
            float dome = sub(age, BLAST, BLAST + 1.5f) * (1f - sub(age, 17f, 23f));
            if (dome > 0.01f) {
                float fl = 0.7f + 0.3f * hash(seed + roll * 13L, 0, 50);
                sprite(ctx, buf, UMBRA, VfxBlend.ALPHA, c.x, c.y + R * 0.2f, c.z, R * (0.6f + 0.9f * VfxAnim.easeOutCubic(blastT)), 0f, a(BLACK, 0.5f * dome * fl));
                sprite(ctx, buf, CORONA, VfxBlend.ADD, c.x, c.y + R * 0.2f, c.z, R * (0.5f + 0.8f * VfxAnim.easeOutCubic(blastT)), age * 0.09f, a(DEEP_VIOLET, 0.8f * dome * fl));
                sprite(ctx, buf, SMOKE, VfxBlend.ALPHA, c.x, c.y + R * 0.12f, c.z, R * (0.5f + 0.4f * blastT), age * 0.05f, a(BLACK, 0.4f * dome * (1f - fl * 0.4f)));
            }
        }
        float boltOut = 1f - sub(age, 18.5f, 24f);
        if (blastT > 0f && boltOut > 0.01f) {
            Vector3f[] pt = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
            int nb = ctx.seg(BOLTS, 6);
            for (int k = 0; k < nb; k++) {
                float st = BLAST + 0.35f * (k % 5) * 0.5f;
                float grow = VfxAnim.easeOutCubic(sub(age, st - 0.2f, st + 3.6f));
                if (grow <= 0f) continue;
                boolean up = k % 3 == 1;
                float reach = R * (up ? 0.62f + 0.3f * hash(seed, k, 3) : 0.78f + 0.24f * hash(seed, k, 3)) * grow;
                float flick = 0.65f + 0.35f * hash(seed + roll * 17L, k, 4);
                int segs = ctx.seg(BOLT_SEGS, 2);
                for (int i = 0; i <= BOLT_SEGS; i++) boltPoint(pt[i], c, seed, roll, k, i, reach, R);
                float w = R * (up ? 0.115f : 0.13f) * (0.85f + 0.3f * hash(seed, k, 5));
                for (int i = 0; i < segs; i++) {
                    float taper = 1f - 0.45f * i / BOLT_SEGS;
                    bolt(buf, BOLT, VfxBlend.ALPHA, pt[i], pt[i + 1], w * taper, ((k + i) & 1) == 0, a(CRIMSON, 0.98f * boltOut * flick));
                    strip(buf, ARC, VfxBlend.ADD, pt[i], pt[i + 1], w * 0.8f * taper, w * 0.6f * taper, a(VIOLET, 0.8f * boltOut * flick));
                }
                // tiny white where the bolt ends
                sprite(ctx, buf, SPARK, VfxBlend.ADD, pt[segs].x, pt[segs].y, pt[segs].z, R * 0.1f * (0.6f + 0.4f * flick), age * 0.4f + k, a(WHITE, 0.95f * boltOut));
            }
        }

        // ---- the aftermath: arcs crawl on the ground, black motes lift
        float after = sub(age, 17.5f, 20f) * out;
        if (after > 0.01f) {
            int n = ctx.seg(CRAWLS, 3);
            for (int k = 0; k < n; k++) {
                float ang = hash(seed, k, 60) * Mth.TWO_PI, rr = R * (0.12f + 0.78f * hash(seed + roll * 7L, k, 61));
                float len = R * (0.07f + 0.07f * hash(seed + roll * 11L, k, 62)), da = ang + (hash(seed + roll * 19L, k, 63) - 0.5f) * 1.2f;
                Vector3f p = new Vector3f(c.x + Mth.cos(ang) * rr, c.y + 0.15f, c.z + Mth.sin(ang) * rr);
                Vector3f q = new Vector3f(p.x + Mth.cos(da) * len, c.y + 0.2f + 0.3f * s * hash(seed + roll * 23L, k, 64), p.z + Mth.sin(da) * len);
                float al = after * (0.5f + 0.5f * hash(seed + roll * 29L, k, 65));
                strip(buf, ARC, VfxBlend.ADD, p, q, s * 0.8f, s * 0.6f, a(k % 3 == 0 ? CRIMSON : VIOLET, 0.9f * al));
                strip(buf, ARC, VfxBlend.ALPHA, p, q, s * 0.4f, s * 0.3f, a(BLACK, 0.9f * al));
            }
            int nm = ctx.seg(MOTES, 4);
            for (int k = 0; k < nm; k++) {
                float ang = hash(seed, k, 70) * Mth.TWO_PI, rr = R * 0.85f * Mth.sqrt(hash(seed, k, 71));
                float rise = (0.4f + 1.8f * hash(seed, k, 72)) * s + 0.2f * s * (age - 17.5f) * (0.6f + hash(seed, k, 73));
                float sz = (0.5f + 0.7f * hash(seed, k, 74)) * s;
                sprite(ctx, buf, MOTE, VfxBlend.ALPHA, c.x + Mth.cos(ang) * rr, c.y + rise, c.z + Mth.sin(ang) * rr, sz, hash(seed, k, 75) * 6.28f + age * 0.05f, a(BLACK, 0.85f * after));
            }
        }
    }
}
