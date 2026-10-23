package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.Lod;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.State;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Fuegoleon's flame arm (3.1): the part of the Djinn-style flame limb that is not a model: the FLAME TONGUES curling off the shoulder, the elbow (the big hooked one), the forearm and the wrist,
 * the sparks drifting off the arm, the soft haze behind it and the flash of each piece being forged in. Drawn in the ARM frame (the pose is at the shoulder pivot of the right arm, in model pixels,
 * y down, -x outward) by {@link FuegoleonArmDjinn}, in third and first person.
 * <ul>
 *   <li><b>tongues</b>: each is a short strip of {@link FuegoleonArmDjinnRules#TONGUE_SEGMENTS} quads that bends along a curl (the hook) and sways, turned about its own axis to face the camera
 *       (a cylindrical billboard, so it reads as a flat cel flame from the side and in the hand); the texture {@code aura/fad_tongue} has four frames (a 4 frame flicker loop, about
 *       {@code 0.42 * 20} frames a second) painted in the reference's flat bands with a dark red outline, alpha blended (not additive: additive would drop the outline). The shape table is
 *       {@link FuegoleonArmDjinnRules#TONGUE}; a tongue grows with the forge-in of the piece it belongs to;</li>
 *   <li><b>sparks, haze, flashes</b>: additive ({@code AuraRender.additive}, the SafeAdditive rule); sparks are small coals rising off the arm, the haze one soft glow, a flash one glow per piece
 *       while it is being forged.</li>
 * </ul>
 * The counts come from {@link FuegoleonArmDjinnRules} (state and level of detail); the worst case is {@link FuegoleonArmDjinnRules#worstQuads()}. Nothing is allocated per frame: the strip points
 * live in the scratch arrays below, which only the render thread touches.
 */
public final class FuegoleonArmDjinnFx {
    private FuegoleonArmDjinnFx() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final ResourceLocation TONGUE = AuraRender.tex("aura/fad_tongue"), GLOW = AuraRender.tex("particle/glow"), EMBER = AuraRender.tex("particle/flamefx_ember");
    private static final int C_FLASH = 0xFFC060, C_SPARK = 0xFFB040, C_HAZE = 0xFF6A20;
    private static final int MAXP = FuegoleonArmDjinnRules.TONGUE_SEGMENTS + 1;
    private static final float[] PX = new float[MAXP], PY = new float[MAXP], PZ = new float[MAXP], WX = new float[MAXP], WY = new float[MAXP], WZ = new float[MAXP];

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static VertexConsumer add(MultiBufferSource b, ResourceLocation tex) { return b.getBuffer(AuraRender.additive(tex)); }

    /**
     * Draws the tongues, sparks, haze and flashes.
     *
     * @param pose     at the arm's shoulder pivot, scaled to model pixels
     * @param age      the animation clock (ticks, partial tick included)
     * @param st       the strength of the arm
     * @param lod      the level of detail (CULLED draws nothing)
     * @param fp       first person: the camera basis of the hand ({@link FuegoleonArmFx#cameraFirstPerson})
     * @param assembled how far each piece is forged in ({@code EquipAnimation.assembled}), indexed by piece
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, float age, State st, Lod lod, boolean fp, float[] assembled) {
        if (lod == Lod.CULLED) return;
        if (fp) FuegoleonArmFx.cameraFirstPerson(pose); else SealDraw.camera(pose);
        float[] cam = SealDraw.CAM;
        float lx = cam[6], ly = cam[7], lz = cam[8];
        tongues(pose, buffers, age, st, lod, lx, ly, lz, assembled);
        if (lod != Lod.FULL) return;
        VertexConsumer glow = add(buffers, GLOW);
        float haze = FuegoleonArmDjinnRules.haze(st);
        float sh = 9.0f * (1f + 0.07f * Mth.sin(age * 0.21f));
        SealDraw.glow(pose, glow, -1f, 6f, 0f, sh, 0f, SealDraw.argb(haze, C_HAZE), FB);
        for (int i = 0; i < FuegoleonArmDjinnRules.MAX_FLASHES; i++) {                      // the flash of a piece being forged: a glow on its centre
            float f = FuegoleonArmDjinnRules.flash(assembled[i]);
            if (f <= 0.01f) continue;
            float[] c = FuegoleonArmDjinnRules.PIECE_CENTRE[i];
            SealDraw.glow(pose, glow, c[0], c[1], c[2], 3.5f + 3.5f * f, 0f, SealDraw.argb(0.75f * f, C_FLASH), FB);
        }
        sparks(pose, buffers, age, st);
    }

    private static void tongues(PoseStack pose, MultiBufferSource buffers, float age, State st, Lod lod, float lx, float ly, float lz, float[] assembled) {
        int n = FuegoleonArmDjinnRules.tonguesAt(st, lod), segs = FuegoleonArmDjinnRules.segmentsAt(lod);
        if (n <= 0) return;
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(TONGUE));
        float lenK = FuegoleonArmDjinnRules.tongueLength(st);
        float[][] tab = FuegoleonArmDjinnRules.TONGUE;
        for (int i = 0; i < n; i++) {
            float[] r = tab[i];
            float a = assembled[(int) r[FuegoleonArmDjinnRules.T_PIECE]];
            float grow = a >= 1f ? 1f : a > 0f ? a : a < 0f ? 1f + a : 0f;                   // forging in grows the tongue, shattering shrinks it
            if (grow <= 0.02f) continue;
            float len = r[FuegoleonArmDjinnRules.T_LEN] * lenK * (0.35f + 0.65f * grow);
            float wid = r[FuegoleonArmDjinnRules.T_WID] * (0.55f + 0.45f * Math.min(1f, lenK)) * (0.5f + 0.5f * grow);
            float ph = r[FuegoleonArmDjinnRules.T_PHASE];
            int frame = (int) (age * FuegoleonArmDjinnRules.FRAME_SPEED + ph * 1.7f) & (FuegoleonArmDjinnRules.FRAMES - 1);
            float u0 = frame / (float) FuegoleonArmDjinnRules.FRAMES, u1 = u0 + 1f / FuegoleonArmDjinnRules.FRAMES;
            float step = len / segs;
            float hx0 = r[FuegoleonArmDjinnRules.T_DX], hy0 = r[FuegoleonArmDjinnRules.T_DY], hz0 = r[FuegoleonArmDjinnRules.T_DZ];
            float cx = r[FuegoleonArmDjinnRules.T_CX], cy = r[FuegoleonArmDjinnRules.T_CY], cz = r[FuegoleonArmDjinnRules.T_CZ];
            float sw = age * FuegoleonArmDjinnRules.SWAY_SPEED + ph;
            PX[0] = r[FuegoleonArmDjinnRules.T_X]; PY[0] = r[FuegoleonArmDjinnRules.T_Y]; PZ[0] = r[FuegoleonArmDjinnRules.T_Z];
            for (int s = 0; s < segs; s++) {
                float t = (s + 0.5f) / segs;
                float hx = hx0 + cx * t * t + 0.28f * t * Mth.sin(sw + t * 2.4f), hy = hy0 + cy * t * t, hz = hz0 + cz * t * t + 0.28f * t * Mth.cos(sw * 0.8f + t * 2.0f);
                float inv = 1f / Math.max(1e-4f, Mth.sqrt(hx * hx + hy * hy + hz * hz));
                PX[s + 1] = PX[s] + hx * inv * step; PY[s + 1] = PY[s] + hy * inv * step; PZ[s + 1] = PZ[s] + hz * inv * step;
            }
            for (int s = 0; s <= segs; s++) {                                              // the width vector at each joint: across the strip's axis and the camera's look
                int a0 = Math.max(0, s - 1), a1 = Math.min(segs, s + 1);
                float tx = PX[a1] - PX[a0], ty = PY[a1] - PY[a0], tz = PZ[a1] - PZ[a0];
                float wx = ty * lz - tz * ly, wy = tz * lx - tx * lz, wz = tx * ly - ty * lx;
                float wl = Mth.sqrt(wx * wx + wy * wy + wz * wz);
                float k = wl < 1e-5f ? 0f : wid * 0.5f / wl;
                WX[s] = wx * k; WY[s] = wy * k; WZ[s] = wz * k;
            }
            int col = SealDraw.argb(Mth.clamp(grow * 1.1f, 0f, 1f), 0xFFFFFF);
            for (int s = 0; s < segs; s++) {
                float vTop = 1f - (s + 1f) / segs, vBot = 1f - s / (float) segs;
                PropDraw.quad(pose, vc, PX[s] - WX[s], PY[s] - WY[s], PZ[s] - WZ[s], PX[s] + WX[s], PY[s] + WY[s], PZ[s] + WZ[s],
                        PX[s + 1] + WX[s + 1], PY[s + 1] + WY[s + 1], PZ[s + 1] + WZ[s + 1], PX[s + 1] - WX[s + 1], PY[s + 1] - WY[s + 1], PZ[s + 1] - WZ[s + 1], u0, vTop, u1, vBot, col, FB);
            }
        }
    }

    private static void sparks(PoseStack pose, MultiBufferSource buffers, float age, State st) {
        int n = FuegoleonArmDjinnRules.sparksAt(st, Lod.FULL);
        if (n <= 0) return;
        VertexConsumer em = add(buffers, EMBER);
        float lift = st == State.FLARE ? 1.35f : 1f;
        for (int i = 0; i < n; i++) {
            float ph = frac(age * 0.021f * (1f + 0.3f * (i % 3)) + i * 0.137f);
            float x = -1f + (i % 2 == 0 ? -1f : 1f) * (2.8f + 1.8f * frac(i * 0.73f)) + Mth.sin(age * 0.1f + i * 2f) * 0.9f;
            float z = Mth.sin(i * 1.9f) * 3f + Mth.cos(age * 0.08f + i) * 0.8f;
            float y = 12f * frac(i * 0.381f) - ph * 20f * lift;
            SealDraw.glow(pose, em, x, y, z, 0.8f + 0.5f * frac(i * 0.9f), age * 0.1f + i, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.9f, C_SPARK), FB);
        }
    }
}
