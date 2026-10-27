package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Ultimate "Final Dose" (Omniscient Reader's Viewpoint, Lee Seolhwa, the Needle and Blade): a clinical, cold poison bloom. Nothing green and slimy: bone-white needles,
 * pale jade mist, a little deep violet venom.
 * <ul>
 *   <li>0-6: dozens of fine pale needles (and a few syringe-like stakes with a ring at the top) rain onto a ring round the caster and stick in the ground, leaning in; drops of venom
 *       run off and ripple the ground; a pale jade mist ring opens.</li>
 *   <li>6-9: the needles pull out and close on one point at chest height like a shutting sea urchin, tips first.</li>
 *   <li>9-12: the pale flash at the point, a ring of needle-ticks and a thin shock ring race out to the full radius while a flower of venom opens on the ground: eight jade petals,
 *       eight deep violet petals between them, a bone-white core.</li>
 *   <li>12-20: the petals droop and fade, the mist thins out and a cold haze of spores drifts up and away.</li>
 * </ul>
 * {@code inst.power} = the burst radius (12-13). Worst case about 330 of the 400 vertices (all quads), constant loop counts, deterministic from seed and age; the
 * {@link VfxRenderContext#seg} detail scale only thins the optional spore haze.
 */
public class UltFinalDoseLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation STREAK = t("spike_streak"), SYRINGE = t("modification_needle"), PETAL = t("briar_petal"), BLOOM = t("transmutation_petal"),
            RING = t("judgment_ring"), HALO = t("bfx_halo"), MIST = t("dream_mist"), GLOW = t("grand_zone_spore"), FLARE = t("spike_flare"), DROP = t("mercury_droplet");

    private static final int BONE = 0xFFF3EFE2, JADE = 0xFF8FE3C2, PALE_JADE = 0xFFC4F2DE, COLD = 0xFFDDF8F0, VIOLET = 0xFF7240B0;
    private static final int NEEDLES = 28, DROPS = 5, PETALS = 8, SPORES = 12, MISTS = 11;
    private static final float FALL_TICKS = 2.2f, PEAK = 9.0f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_FINAL_DOSE); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFFC4F2DE; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, new Vector3f(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(new Vector3f(c).add(0f, y, 0f)).spin(spin), radius, col);
    }

    /** A ribbon from the tip (texture bottom) to the tail (texture top), turned to face the camera round its own axis. */
    private static void needle(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f tip, Vector3f tail, float w, int col) {
        if (!buf.hasBudget(4)) return;
        Vector3f axis = new Vector3f(tail).sub(tip);
        Vector3f mid = new Vector3f(tip).add(tail).mul(0.5f);
        Vector3f side = axis.cross(new Vector3f(mid).negate(), new Vector3f());
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize().mul(w * 0.5f);
        int c = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(tip).sub(side), new Vector3f(tip).add(side), new Vector3f(tail).add(side), new Vector3f(tail).sub(side), 0f, 0f, 1f, 1f, c, c);
    }

    /** A petal lying on the ground: its pointed end at 'hub' from the centre, its wide end 'len' further out and raised by 'lift'. */
    private static void petal(VfxVertexBuffer buf, Vector3f c, float ang, float hub, float len, float width, float lift, int col) {
        if (len < 0.05f || !buf.hasBudget(4)) return;
        float cx = Mth.cos(ang), cz = Mth.sin(ang), tx = -cz * width * 0.5f, tz = cx * width * 0.5f;
        float ix = c.x + cx * hub, iz = c.z + cz * hub, ox = c.x + cx * (hub + len), oz = c.z + cz * (hub + len);
        float iy = c.y + 0.06f, oy = c.y + 0.06f + lift;
        int cc = VfxBlend.ALPHA.grade(col, 1f);
        buf.quad(PETAL, VfxBlend.ALPHA, new Vector3f(ox - tx, oy, oz - tz), new Vector3f(ix - tx * 0.15f, iy, iz - tz * 0.15f), new Vector3f(ix + tx * 0.15f, iy, iz + tz * 0.15f), new Vector3f(ox + tx, oy, oz + tz),
                0f, 0f, 1f, 1f, cc, cc);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(4f, inst.power), s = R / 12f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float out = 1f - sub(age, 15.5f, 20f);
        if (out <= 0.01f) return;
        Vector3f centre = new Vector3f(c.x, c.y + 1.4f, c.z);

        float wave = VfxAnim.easeOutCubic(sub(age, PEAK - 0.2f, PEAK + 4.2f));
        float open = VfxAnim.easeOutBack(sub(age, PEAK - 0.4f, PEAK + 4.5f));
        float flash = 1f - sub(age, PEAK - 0.2f, PEAK + 3.3f);

        // ---- ground: the cold mist ring that opens first, and the violet venom ring under the flower
        float ringIn = VfxAnim.easeOutCubic(sub(age, 1.5f, 9f));
        ground(buf, RING, VfxBlend.ALPHA, c, 0.05f, R * (0.3f + 0.7f * ringIn), 0f, a(JADE, 0.55f * out * sub(age, 1.2f, 3.5f)));
        ground(buf, RING, VfxBlend.ALPHA, c, 0.07f, R * 0.55f * open, 0f, a(VIOLET, 0.7f * out * sub(age, PEAK, PEAK + 2f)));

        // ---- the petals of venom: deep violet ones first (they sit under), then the jade ones
        float droop = 1f - 0.55f * sub(age, 13f, 20f);
        for (int k = 0; k < PETALS; k++) {
            float ang = Mth.TWO_PI * (k + 0.5f) / PETALS + 0.12f * (hash(seed, k, 31) - 0.5f);
            petal(buf, c, ang, R * 0.07f, R * (0.62f + 0.07f * hash(seed, k, 32)) * open, R * 0.38f * Math.min(open, 1.1f), R * 0.1f * open * droop, a(VIOLET, 0.92f * out));
        }
        for (int k = 0; k < PETALS; k++) {
            float ang = Mth.TWO_PI * k / PETALS + 0.1f * (hash(seed, k, 33) - 0.5f);
            petal(buf, c, ang, R * 0.1f, R * (0.86f + 0.1f * hash(seed, k, 34)) * open, R * 0.46f * Math.min(open, 1.1f), R * 0.17f * open * droop, a(k % 2 == 0 ? JADE : PALE_JADE, 0.9f * out));
        }
        ground(buf, BLOOM, VfxBlend.ALPHA, c, 0.2f, R * 0.34f * open, age * 0.03f, a(BONE, 0.95f * out * sub(age, PEAK, PEAK + 1.5f)));
        ground(buf, BLOOM, VfxBlend.ADD, c, 0.26f, R * 0.4f * open, -age * 0.04f, a(COLD, 0.55f * out * sub(age, PEAK, PEAK + 1.5f)));

        // ---- the needles
        for (int k = 0; k < NEEDLES; k++) {
            float ang = Mth.TWO_PI * k / NEEDLES + 0.45f * (hash(seed, k, 1) - 0.5f), rad = R * (0.3f + 0.62f * hash(seed, k, 2));
            float land = FALL_TICKS + 3.2f * hash(seed, k, 3);
            float f = cl((age - (land - FALL_TICKS)) / FALL_TICKS);
            if (f <= 0f) continue;
            float cx = Mth.cos(ang), cz = Mth.sin(ang);
            float len = (3.8f + 2.0f * hash(seed, k, 4)) * s;
            boolean syringe = k % 4 == 1;
            // the stuck pose: tip in the ground, leaning in (tail outward)
            float tx0 = c.x + cx * rad, tz0 = c.z + cz * rad, ty0 = c.y + 0.05f;
            float dx = cx * 0.3f, dy = 1f, dz = cz * 0.3f, dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float tipX = tx0, tipY = ty0 + 10f * s * (1f - f), tipZ = tz0, L = len * (1f + 0.7f * (1f - f));
            float q = sub(age, 6.0f + 0.9f * hash(seed, k, 5), PEAK);
            float alpha = 0.95f * (1f - sub(age, PEAK, PEAK + 1.3f)) * out;
            if (q > 0f) {                                                                   // pulled out and closing on the point
                float e = q * q * (0.4f + 0.6f * q);
                tipX = Mth.lerp(e, tx0, centre.x); tipY = Mth.lerp(e, ty0, centre.y); tipZ = Mth.lerp(e, tz0, centre.z);
                float ax = tx0 - centre.x, ay = ty0 - centre.y, az = tz0 - centre.z, al = Mth.sqrt(ax * ax + ay * ay + az * az) + 1e-4f;
                float m = VfxAnim.easeInOutSine(sub(q, 0f, 0.7f));
                dx = Mth.lerp(m, dx, ax / al); dy = Mth.lerp(m, dy, ay / al * 0.5f + 0.35f); dz = Mth.lerp(m, dz, az / al);
                dl = Mth.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4f; dx /= dl; dy /= dl; dz /= dl;
                L *= 1f - 0.2f * e;
            }
            if (alpha <= 0.01f) continue;
            Vector3f tip = new Vector3f(tipX, tipY, tipZ);
            Vector3f tail = new Vector3f(tipX + dx * L, Math.max(c.y + 0.1f, tipY + dy * L), tipZ + dz * L);
            if (syringe) needle(buf, SYRINGE, VfxBlend.ALPHA, tip, tail, L * 0.3f, a(PALE_JADE, alpha));
            else needle(buf, STREAK, VfxBlend.ALPHA, tip, tail, L * 0.26f, a(BONE, alpha));
        }

        // ---- venom drips off the stuck needles, each one ripples the ground where it lands
        for (int k = 0; k < DROPS; k++) {
            float ang = Mth.TWO_PI * (k + 0.3f * hash(seed, k, 6)) / DROPS, rad = R * (0.4f + 0.45f * hash(seed, k, 7));
            float st = 4.6f + 1.5f * hash(seed, k, 8), f = sub(age, st, st + 1.5f);
            if (f <= 0f || age > 9.5f) continue;
            float x = c.x + Mth.cos(ang) * rad, z = c.z + Mth.sin(ang) * rad;
            int col = k % 2 == 0 ? VIOLET : JADE;
            if (f < 1f) sprite(ctx, buf, DROP, VfxBlend.ALPHA, x, c.y + 2.2f * s * (1f - f * f) + 0.2f, z, 0.75f * s, 0f, a(col, 0.95f));
            else ground(buf, RING, VfxBlend.ALPHA, new Vector3f(x, c.y, z), 0.09f, s * (0.3f + 1.3f * sub(age, st + 1.5f, st + 3.2f)), 0f, a(col, 0.8f * (1f - sub(age, st + 1.5f, st + 3.2f))));
        }

        // ---- the mist that hangs round the ring
        for (int k = 0; k < MISTS; k++) {
            float ph = hash(seed, k, 9), ang = Mth.TWO_PI * k / MISTS + age * 0.018f + ph, rr = R * (0.45f + 0.5f * ringIn * (0.6f + 0.4f * hash(seed, k, 10)));
            float al = 0.34f * sub(age, 2f, 5f) * out;
            sprite(ctx, buf, MIST, VfxBlend.ALPHA, c.x + Mth.cos(ang) * rr, c.y + 0.5f * s + 0.35f * s * Mth.sin(age * 0.2f + k), c.z + Mth.sin(ang) * rr, R * (0.2f + 0.08f * ph), ph * 6.28f + age * 0.03f, a(k % 3 == 0 ? COLD : PALE_JADE, al));
        }

        // ---- the peak: a pale flash at the point, the needle-tick ring and the shock ring racing out
        if (flash > 0f && age >= PEAK - 0.3f) {
            float fe = 1f - flash;
            sprite(ctx, buf, FLARE, VfxBlend.ADD, centre.x, centre.y, centre.z, R * (0.5f + 0.9f * fe), age * 0.08f, a(BONE, 0.95f * flash));
            sprite(ctx, buf, GLOW, VfxBlend.ADD, centre.x, centre.y, centre.z, R * (0.4f + 0.9f * fe), 0f, a(PALE_JADE, 0.8f * flash));
        }
        if (wave > 0f) {
            float w = 1f - sub(age, PEAK + 1.5f, PEAK + 5f);
            ground(buf, HALO, VfxBlend.ADD, c, 0.12f, R * 1.02f * wave, age * 0.05f, a(COLD, 0.8f * w * out));
            ground(buf, RING, VfxBlend.ALPHA, c, 0.1f, R * 1.04f * wave, 0f, a(BONE, 0.9f * w * out));
        }

        // ---- the cold spore haze that drifts up afterwards
        float hz = sub(age, 11f, 14f) * out;
        if (hz > 0f) {
            int n = ctx.seg(SPORES, 5);
            for (int k = 0; k < n; k++) {
                float ang = hash(seed, k, 11) * Mth.TWO_PI, rr = R * 0.9f * Mth.sqrt(hash(seed, k, 12));
                float rise = (0.5f + 2.6f * hash(seed, k, 13)) * s + 0.14f * s * (age - 11f) * (0.6f + hash(seed, k, 14));
                float tw = 0.65f + 0.35f * Mth.sin(age * 0.5f + k * 2.1f);
                sprite(ctx, buf, GLOW, VfxBlend.ADD, c.x + Mth.cos(ang) * rr + 0.3f * s * Mth.sin(age * 0.15f + k), c.y + rise, c.z + Mth.sin(ang) * rr, (0.5f + 0.5f * hash(seed, k, 15)) * s * 1.1f, 0f, a(k % 4 == 0 ? VIOLET : COLD, 0.85f * hz * tw));
            }
        }
    }
}
