package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.30.0 Veldanava's Disintegration: matter coming apart into dust, a zone being UNMADE (nothing burns, nothing is fired). Ash grey, bone white, a cold pale-blue glow, touches of dark.
 * <ul>
 *   <li>0-5: an ash-grey magic circle stamps itself onto the ground (it starts a little too big and settles, pulsing) with a bright thin rim; hairline cracks run out from the centre and
 *       a dark, dull patch of ground spreads under it.</li>
 *   <li>5-22: three slow ripple rings (every ~6.5 ticks) travel from the centre to the rim, and the ground / air is eroded exactly where each ring passes: flakes and chunks
 *       peel off (some off the ground, some hanging in the air), tumble upward, crumble into puffs of dust and drifting pale motes. A faint column of dust rises over the centre,
 *       dust clouds and clusters of fragments climb, the rim flickers each time a ripple reaches it.</li>
 *   <li>22-30: the circle and cracks fade, the column thins, the dust hangs and the last motes drift up (everything is gone by tick 30, so a second copy 28 ticks later starts softly).</li>
 * </ul>
 * {@code inst.power} = the zone radius (~8). Worst case about 340 of the 400 vertices (all quads, optional motes dropped first by the budget), constant loop counts, deterministic from seed
 * and age; {@link VfxRenderContext#seg} thins the optional motes and fragment clusters only. {@link VfxInstance#color} tints the glow and the rim (default pale cold blue).
 */
public class VeldDisintegrationLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation CIRCLE = t("magic_circle"), CELESTIAL = t("celestial_circle"), THIN_RING = t("rune_line_ring"),
            CRACK_A = t("bfx_ground_crack"), CRACK_B = t("judgment_cracks_ground"), CRACK_C = t("trial_cracks"),
            PATCH = t("stv_b_dust_c"), GLOW_BLUE = t("stv_b_glow_blue"), GLOW = t("glow"), MOTE = t("spatial_mote"),
            DUST_A = t("stv_b_dust_a"), DUST_B = t("stv_b_dust_b"), CLOUD = t("ash_cloud"), BONE_DUST = t("bone_dust"), FRAGS = t("curse_flakes"),
            FLAKE_A = t("ash_flake"), FLAKE_B = t("ash_flake_1");
    private static final ResourceLocation[] CHUNKS = {FLAKE_A, FLAKE_B, FLAKE_B, FLAKE_A};

    private static final int ASH = 0xFFA7A9B0, BONE = 0xFFEBE8DF, ASH_DARK = 0xFF73767F, DARK = 0xFF14161C, COLD = 0xFFB4D2F0;
    private static final int PARTICLES = 34, CLOUDS = 7, FRAG_CLUSTERS = 7, MOTES = 14;
    private static final float[] PULSE_START = {4.0f, 10.5f, 17.0f};
    private static final float PULSE_TRAVEL = 6.5f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_DISINTEGRATION); }
    @Override public int defaultDuration(VfxShape s) { return 30; }
    @Override public int defaultColor(VfxShape s) { return 0xFFB4D2F0; }

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

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float half, float spin, int col) {
        if (half > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(new Vector3f(c).add(0f, y, 0f)).spin(spin), half, col);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(3f, inst.power), s = R / 8f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int glow = inst.color;
        float out = 1f - VfxAnim.easeInOutSine(sub(age, 22f, 30f));                 // circle, cracks, rim
        float outP = 1f - sub(age, 26f, 30f);                                        // the dust: it hangs a little longer
        if (out <= 0.005f && outP <= 0.005f) return;

        // ---- the ripples: where each one is (0 = not started / done) and how bright
        float[] pf = new float[3];
        float rimBeat = 0f;
        for (int p = 0; p < 3; p++) {
            pf[p] = sub(age, PULSE_START[p], PULSE_START[p] + PULSE_TRAVEL);
            rimBeat += Mth.sin(Mth.PI * sub(age, PULSE_START[p] + PULSE_TRAVEL - 1.5f, PULSE_START[p] + PULSE_TRAVEL + 1.5f)) * (age > PULSE_START[p] + PULSE_TRAVEL - 1.5f ? 1f : 0f);
        }
        rimBeat = cl(rimBeat);
        float coreFlash = 0f;
        for (int p = 0; p < 3; p++) if (age >= PULSE_START[p] && age < PULSE_START[p] + 2.5f) coreFlash = Math.max(coreFlash, 1f - (age - PULSE_START[p]) / 2.5f);

        // ================================================================== the ground (essentials first, so the budget never drops them)
        float appear = sub(age, 0f, 2.2f);
        float settle = VfxAnim.easeOutCubic(sub(age, 0f, 5f));
        float stampPulse = 0.72f + 0.28f * Mth.sin(age * 2.3f);
        float flick = 0.62f + 0.38f * Mth.sin(age * 3.1f + 1.3f) * Mth.sin(age * 1.9f + 0.4f);
        float flickRim = age < 5f ? stampPulse : flick;

        // dull, dark patch under everything: the ground losing its colour
        float patch = VfxAnim.easeOutCubic(sub(age, 0.5f, 7f));
        ground(buf, PATCH, VfxBlend.ALPHA, c, 0.04f, R * 1.08f * patch, 0.3f, a(DARK, 0.75f * out * appear));
        ground(buf, PATCH, VfxBlend.ALPHA, c, 0.045f, R * 0.8f * patch, 2.1f, a(ASH_DARK, 0.42f * out * appear));
        // the cold glow in the zone
        ground(buf, GLOW_BLUE, VfxBlend.ADD, c, 0.1f, R * 0.95f * patch, 0f, a(glow, (0.16f + 0.14f * coreFlash) * out * appear));

        // the circle: ash-grey, stamps in a little too big, then slowly turns
        float cr = R * (1.0f + 0.22f * (1f - settle));
        ground(buf, CIRCLE, VfxBlend.ALPHA, c, 0.07f, cr * 1.03f, age * 0.022f, a(ASH, 0.7f * out * appear * stampPulse));
        ground(buf, CIRCLE, VfxBlend.ADD, c, 0.09f, cr * 1.03f, age * 0.022f, a(BONE, 0.12f * out * appear * (0.6f + 0.4f * stampPulse)));
        ground(buf, CELESTIAL, VfxBlend.ADD, c, 0.11f, cr * 0.7f, -age * 0.035f, a(glow, 0.3f * out * appear));
        // the bright thin rim, flickering, flaring each time a ripple reaches it
        ground(buf, THIN_RING, VfxBlend.ADD, c, 0.13f, cr * 1.035f, 0f, a(BONE, (0.4f + 0.4f * rimBeat) * flickRim * out * appear));
        ground(buf, THIN_RING, VfxBlend.ALPHA, c, 0.12f, cr * 1.035f, 0.2f, a(COLD, 0.12f * out * appear));

        // hairline cracks running out of the centre (the grow IS the spreading)
        float spread = VfxAnim.easeOutCubic(sub(age, 0.3f, 7f));
        ground(buf, CRACK_B, VfxBlend.ALPHA, c, 0.055f, R * 1.02f * spread, 0.6f, a(DARK, 0.85f * out * appear));
        ground(buf, CRACK_A, VfxBlend.ALPHA, c, 0.075f, R * 0.98f * spread, 0.1f, a(ASH, 0.5f * out * appear));
        ground(buf, CRACK_C, VfxBlend.ALPHA, c, 0.08f, R * 0.8f * spread, 1.7f, a(ASH, 0.4f * out * appear));
        ground(buf, CRACK_A, VfxBlend.ADD, c, 0.14f, R * 0.98f * spread, 0.1f, a(glow, (0.05f + 0.16f * coreFlash + 0.1f * rimBeat) * out * appear));

        // the ripple rings: a bright thin leading edge and a soft ash-grey wake
        for (int p = 0; p < 3; p++) {
            float f = pf[p];
            if (f <= 0f || f >= 1f) continue;
            float rr = R * (0.06f + 0.97f * (1f - (1f - f) * (1f - f) * 0.55f - 0.45f * (1f - f)));
            float al = Mth.sin(Mth.PI * Math.min(1f, f * 1.15f)) * (0.55f + 0.45f * f);
            ground(buf, THIN_RING, VfxBlend.ADD, c, 0.16f, rr * 1.03f, 0f, a(BONE, 0.9f * al * out));
            ground(buf, THIN_RING, VfxBlend.ALPHA, c, 0.15f, rr * 0.9f * 1.03f, 0f, a(COLD, 0.55f * al * out));
        }

        // the dust the ripple kicks up: low puffs riding the ring
        for (int p = 0; p < 3; p++) {
            float f = pf[p];
            if (f <= 0.02f || f >= 1f) continue;
            float rr = R * (0.06f + 0.97f * (1f - (1f - f) * (1f - f) * 0.55f - 0.45f * (1f - f)));
            float al = Mth.sin(Mth.PI * f) * outP;
            for (int k = 0; k < 2; k++) {
                int q = p * 2 + k;
                float ang = Mth.TWO_PI * (q + hash(seed, q, 70)) / 6f + f * 0.6f;
                sprite(ctx, buf, q % 2 == 0 ? DUST_A : DUST_B, VfxBlend.ALPHA, c.x + Mth.cos(ang) * rr * 0.95f, c.y + (0.5f + 1.2f * f) * s, c.z + Mth.sin(ang) * rr * 0.95f,
                        (2.6f + 1.2f * f) * s, hash(seed, q, 71) * 6.28f, a(q % 2 == 0 ? ASH_DARK : ASH, 0.6f * al));
            }
        }

        // ================================================================== the rising column of pale dust over the centre
        float col = sub(age, 5f, 10f) * (1f - sub(age, 20f, 29f));
        if (col > 0.01f) {
            for (int k = 0; k < 4; k++) {
                float ph = hash(seed, k, 90), rise = ((age * 0.09f + k * 0.27f + ph) % 1f);
                float y = (0.4f + rise * 10f) * s, wob = 0.35f * s * Mth.sin(age * 0.17f + k * 2.3f);
                float fade = Mth.sin(Mth.PI * rise);
                sprite(ctx, buf, k % 2 == 0 ? DUST_B : DUST_A, VfxBlend.ALPHA, c.x + wob, c.y + y, c.z - wob * 0.6f, (3.2f + 1.8f * rise) * s, ph * 6.28f + age * 0.02f, a(k % 2 == 1 ? ASH_DARK : ASH, 0.55f * col * fade));
            }
            sprite(ctx, buf, GLOW_BLUE, VfxBlend.ADD, c.x, c.y + 1.2f * s, c.z, (3.0f + 1.0f * coreFlash) * s, 0f, a(glow, (0.32f + 0.3f * coreFlash) * col));
        }

        // ================================================================== the erosion: flakes and chunks peel off where a ripple passes, crumble to dust and motes
        for (int k = 0; k < PARTICLES; k++) {
            int p = k % 3;
            float h1 = hash(seed, k, 1), h2 = hash(seed, k, 2), h3 = hash(seed, k, 3), h4 = hash(seed, k, 4);
            float frac = k % 6 == 0 ? 0.9f + 0.08f * h1 : 0.08f + 0.86f * Mth.sqrt(h1);      // some come off the rim
            float born = PULSE_START[p] + PULSE_TRAVEL * frac + 0.9f * (h2 - 0.5f);
            float life = 8.5f + 3.5f * h3;
            float tt = (age - born) / life;
            if (tt <= 0f || tt >= 1f) continue;
            float ang = h2 * Mth.TWO_PI + tt * 0.5f * (h4 - 0.5f) * 2f;
            float rad = R * frac * 0.98f;
            boolean air = k % 2 == 1;
            float y0 = air ? (0.5f + 1.7f * h4) * s : 0.1f;
            float rise = (1.2f + 4.2f * h3) * s * (0.25f * tt + 0.75f * tt * tt) + 0.9f * s * tt;
            float sway = 0.3f * s * Mth.sin(age * 0.2f + k * 1.7f) * tt;
            float x = c.x + Mth.cos(ang) * rad + sway, z = c.z + Mth.sin(ang) * rad - sway * 0.7f, y = c.y + y0 + rise;
            float fadeIn = sub(tt, 0f, 0.08f);
            float chunk = fadeIn * (1f - sub(tt, 0.3f, 0.52f));
            if (chunk > 0.01f) {                                                       // the solid piece: it shrinks and loses colour as it goes
                float sz = (0.95f + 0.7f * h3) * s * (1.1f - 0.45f * sub(tt, 0.1f, 0.5f));
                int col2 = k % 5 == 0 ? ASH_DARK : VfxVertexBuffer.lerpColor(ASH, BONE, h4);
                sprite(ctx, buf, CHUNKS[k & 3], VfxBlend.ALPHA, x, y, z, sz, h2 * 6.28f + tt * (4f + 6f * h3) * (k % 2 == 0 ? 1f : -1f), a(col2, 0.95f * chunk * outP));
            }
            float dust = sub(tt, 0.28f, 0.5f) * (1f - sub(tt, 0.62f, 1f));
            if (dust > 0.01f) {
                float u = sub(tt, 0.28f, 1f);
                if (k % 3 == 0) {                                                       // a small puff of dust where the chunk was
                    sprite(ctx, buf, k % 2 == 0 ? BONE_DUST : DUST_B, VfxBlend.ALPHA, x, y, z, (1.1f + 2.4f * u) * s * (0.8f + 0.5f * h1), h1 * 6.28f + u, a(k % 9 == 0 ? ASH_DARK : BONE, 0.75f * dust * outP));
                } else {                                                                // a pale mote that drifts on up
                    float tw = 0.7f + 0.3f * Mth.sin(age * 0.9f + k * 2.1f);
                    sprite(ctx, buf, MOTE, VfxBlend.ADD, x, y + 0.3f * s * u, z, (0.7f + 0.4f * h1) * s, 0f, a(k % 4 == 0 ? BONE : COLD, 0.95f * dust * tw * outP));
                }
            }
        }

        // ================================================================== clouds of dust low in the zone, drifting up and slowly round
        float cloudIn = sub(age, 5f, 9f) * outP;
        if (cloudIn > 0.01f) {
            for (int k = 0; k < CLOUDS; k++) {
                float ph = hash(seed, k, 20), ang = Mth.TWO_PI * k / CLOUDS + ph + age * 0.02f, rr = R * (0.2f + 0.7f * hash(seed, k, 21));
                float up = ((age * 0.06f + ph * 3f) % 1f);
                float al = 0.42f * cloudIn * Mth.sin(Mth.PI * up);
                sprite(ctx, buf, k % 3 == 0 ? CLOUD : (k % 2 == 0 ? DUST_A : DUST_B), VfxBlend.ALPHA, c.x + Mth.cos(ang) * rr, c.y + (0.3f + 6.0f * up) * s, c.z + Mth.sin(ang) * rr,
                        (3.4f + 2.4f * hash(seed, k, 22)) * s, ph * 6.28f, a(k % 3 == 0 ? ASH_DARK : (k % 2 == 0 ? ASH : BONE), al * (0.7f + 0.4f * up)));
            }
        }

        // ================================================================== clusters of fragments (curse_flakes) climbing and thinning
        float fragIn = sub(age, 5.5f, 9f) * outP;
        if (fragIn > 0.01f) {
            int n = ctx.seg(FRAG_CLUSTERS, 3);
            for (int k = 0; k < n; k++) {
                float ph = hash(seed, k, 30), ang = Mth.TWO_PI * (k + ph) / FRAG_CLUSTERS, rr = R * (0.15f + 0.78f * hash(seed, k, 31));
                float up = ((age * 0.05f + ph * 4f) % 1f);
                sprite(ctx, buf, FRAGS, VfxBlend.ALPHA, c.x + Mth.cos(ang + up * 0.4f) * rr, c.y + (0.5f + 5f * up) * s, c.z + Mth.sin(ang + up * 0.4f) * rr, (2.4f + 1.6f * hash(seed, k, 32)) * s,
                        ph * 6.28f + age * 0.06f * (k % 2 == 0 ? 1f : -1f), a(k % 3 == 0 ? ASH_DARK : BONE, 0.95f * fragIn * Mth.sin(Mth.PI * up)));
            }
        }

        // ================================================================== the last motes: small cold sparks that keep climbing while everything else thins
        float moteIn = sub(age, 8f, 12f) * outP;
        if (moteIn > 0.01f) {
            int n = ctx.seg(MOTES, 4);
            for (int k = 0; k < n; k++) {
                float ph = hash(seed, k, 40), ang = hash(seed, k, 41) * Mth.TWO_PI, rr = R * 0.95f * Mth.sqrt(hash(seed, k, 42));
                float up = ((age * 0.045f + ph * 5f) % 1f);
                float tw = 0.55f + 0.45f * Mth.sin(age * 0.7f + k * 2.4f);
                sprite(ctx, buf, k % 3 == 0 ? GLOW : MOTE, VfxBlend.ADD, c.x + Mth.cos(ang) * rr + 0.4f * s * Mth.sin(age * 0.13f + k), c.y + (0.3f + 6.5f * up) * s, c.z + Mth.sin(ang) * rr,
                        (0.5f + 0.4f * hash(seed, k, 43)) * s, 0f, a(k % 2 == 0 ? BONE : COLD, 0.85f * moteIn * tw * Mth.sin(Mth.PI * up)));
            }
        }
    }
}
