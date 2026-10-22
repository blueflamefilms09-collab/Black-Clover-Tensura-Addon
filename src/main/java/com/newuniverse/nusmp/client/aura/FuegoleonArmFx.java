package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Spirit Dive remake: the flames of Fuegoleon's arm, shared by the third person painter ({@link FuegoleonArmAura}) and the first person hand ({@link FireArmFirstPerson}). Everything is drawn in
 * the ARM frame: the pose is at the shoulder pivot of the right arm, in model pixels (y down, the arm hangs from y = 0 to 12, x = -2..2, z = -2..2). Additive, full bright (the SafeAdditive rules:
 * sprites black under their alpha, {@code AuraRender.additive}), nothing allocated per frame.
 * <ul>
 *   <li><b>flame sheets</b>: flat sheets of the animated flame ({@code aura/sdv_flame_sheet}, four frames chosen by the clock, about 11 frames a second) standing round the arm and rising, each
 *       double sided and swaying; they are crossed planes, not billboards, so they look right in the hand too;</li>
 *   <li><b>embers</b>: small glowing coals rising off the arm;</li>
 *   <li><b>shimmer</b>: a soft large glow behind the arm that breathes (the heat haze; a real refraction is not possible with the mod's render types);</li>
 *   <li><b>mane strands</b> (third person): ribbons of fire streaming off the shoulder, back and up (the fire mane of the reference).</li>
 * </ul>
 * The budgets are {@link FuegoleonArmRules#MAX_SHEETS}, {@link FuegoleonArmRules#MAX_EMBERS}, {@link FuegoleonArmRules#MAX_STRANDS}; the level of detail halves the sheets and drops the embers.
 */
public final class FuegoleonArmFx {
    private FuegoleonArmFx() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final ResourceLocation FLAME = AuraRender.tex("aura/sdv_flame_sheet"), GLOW = AuraRender.tex("particle/glow"), EMBER = AuraRender.tex("particle/flamefx_ember"),
            WISP = AuraRender.tex("aura/sdv_wisp");
    /** Colours (RGB): the ember, the mane strand and its hot root, the shimmer. */
    private static final int C_SHEET = 0xFFA45A, C_EMBER = 0xFFB040, C_MANE = 0xFF4A18, C_MANE_HOT = 0xFFD060, C_SHIMMER = 0xFF6A20;
    private static final int STRAND_SEGMENTS = 4;

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static VertexConsumer add(net.minecraft.client.renderer.MultiBufferSource b, ResourceLocation tex) { return b.getBuffer(AuraRender.additive(tex)); }

    /**
     * Draws the sheets, embers and shimmer of the arm.
     *
     * @param k      0..1 the strength (the viewer's setting and the state of the wearer)
     * @param flare  0..1 how much the flames leap (casting, diving)
     * @param full   the full level of detail (all sheets, the embers); false = half the sheets, no embers
     * @param fp     first person: the camera facing glows use the hand's own camera basis
     */
    public static void draw(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, float age, float k, float flare, boolean full, boolean fp) {
        if (k <= 0.01f) return;
        if (fp) cameraFirstPerson(pose); else SealDraw.camera(pose);
        float lift = 1f + 0.45f * flare;
        // the heat haze
        VertexConsumer glow = add(buffers, GLOW);
        float sh = 7.5f * (1f + 0.08f * Mth.sin(age * 0.21f) + 0.25f * flare);
        SealDraw.glow(pose, glow, 0f, 6f, 0f, sh, 0f, SealDraw.argb(0.16f * k * lift, C_SHIMMER), FB);
        // the flame sheets
        VertexConsumer fl = add(buffers, FLAME);
        int n = full ? FuegoleonArmRules.MAX_SHEETS : FuegoleonArmRules.MAX_SHEETS / 2;
        n = Math.max(1, Math.round(n * (0.5f + 0.5f * k)));
        for (int i = 0; i < n; i++) {
            float ang = i * Mth.TWO_PI / n + 0.7f;
            float r = 3.1f + 0.5f * Mth.sin(i * 2.1f);
            float y0 = 1.5f + 9.5f * (i * 0.618f % 1f);                                   // where along the arm the sheet stands (golden ratio spread)
            int frame = (int) (age * 0.55f + i * 1.7f) & 3;
            float u0 = frame * 0.25f, u1 = u0 + 0.25f;
            float hh = (7.0f + 4.0f * frac(i * 0.381f)) * lift * (0.75f + 0.25f * k);
            float hw = 3.2f + 0.8f * frac(i * 0.57f);
            float sway = Mth.sin(age * 0.17f + i * 1.3f) * 1.6f;
            float cx = Mth.cos(ang) * r, cz = Mth.sin(ang) * r;
            float ux = -Mth.sin(ang) * hw, uz = Mth.cos(ang) * hw;
            float tx = sway * Mth.cos(ang + 1.6f) * 0.6f + 0.4f, tz = 1.8f + sway * 0.3f;                    // the tips lean back and sway
            int col = SealDraw.argb(Mth.clamp(0.78f * k * lift * (0.8f + 0.2f * Mth.sin(age * 0.4f + i)), 0f, 1f), C_SHEET);
            sheet(pose, fl, cx, y0 + 1.5f, cz, ux, uz, hh, tx, tz, u0, u1, col);
        }
        if (!full) return;
        // the embers rising off the arm
        VertexConsumer em = add(buffers, EMBER);
        for (int i = 0; i < FuegoleonArmRules.MAX_EMBERS; i++) {
            float ph = frac(age * 0.021f * (1f + 0.3f * (i % 3)) + i * 0.137f);
            float x = (i % 2 == 0 ? -1f : 1f) * (2.6f + 1.8f * frac(i * 0.73f)) + Mth.sin(age * 0.1f + i * 2f) * 0.9f;
            float z = Mth.sin(i * 1.9f) * 3f + Mth.cos(age * 0.08f + i) * 0.8f;
            float y = 11f * frac(i * 0.381f) - ph * 18f * lift;
            SealDraw.glow(pose, em, x, y, z, 0.9f + 0.5f * frac(i * 0.9f), age * 0.1f + i, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.9f * k, C_EMBER), FB);
        }
    }

    /** One double sided flame sheet standing on (cx, yb, cz): half width vector (ux, 0, uz), height hh (up = -y), tips shifted by (tx, tz), UV frame u0..u1. */
    private static void sheet(PoseStack pose, VertexConsumer vc, float cx, float yb, float cz, float ux, float uz, float hh, float tx, float tz, float u0, float u1, int argb) {
        float ax = cx - ux, az = cz - uz, bx = cx + ux, bz = cz + uz;
        PropDraw.quad(pose, vc, ax, yb, az, bx, yb, bz, bx + tx, yb - hh, bz + tz, ax + tx, yb - hh, az + tz, u0, 0f, u1, 1f, argb, FB);
        PropDraw.quad(pose, vc, bx, yb, bz, ax, yb, az, ax + tx, yb - hh, az + tz, bx + tx, yb - hh, bz + tz, u1, 0f, u0, 1f, argb, FB);
    }

    /**
     * Third person: the ribbons of the fire mane streaming off the shoulder (arm frame; the pose is at the arm pivot, the camera already read by {@link #draw}'s caller order is not needed: it reads it).
     * At most {@link FuegoleonArmRules#MAX_STRANDS} strands of {@value #STRAND_SEGMENTS} segments.
     */
    public static void mane(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, float age, float k, float flare, float swing) {
        if (k <= 0.01f) return;
        SealDraw.camera(pose);
        VertexConsumer vc = add(buffers, WISP);
        float lift = 1f + 0.4f * flare;
        for (int s = 0; s < FuegoleonArmRules.MAX_STRANDS; s++) {
            float px = -1.0f - s * 0.9f, py = -1.0f + s * 1.1f, pz = 1.5f + s * 0.6f, seed = s * 1.9f;
            for (int seg = 0; seg < STRAND_SEGMENTS; seg++) {
                float wave = Mth.sin(age * 0.16f + seed + seg * 0.9f) * (0.8f + 0.6f * seg);
                float nx = px - 1.4f + wave * 0.6f, ny = py - (2.2f - 0.3f * seg) * lift, nz = pz + (3.0f + 1.5f * Math.min(1f, swing)) * lift;
                float w = (1.7f - 0.3f * seg) * (0.8f + 0.2f * k);
                int col = SealDraw.argb(0.8f * k * (1f - 0.18f * seg), SealDraw.mix(C_MANE_HOT, C_MANE, seg / (float) STRAND_SEGMENTS));
                SealDraw.ribbon(pose, vc, nx, ny, nz, px, py, pz, w, 1f - (seg + 1f) / STRAND_SEGMENTS, 1f - (float) seg / STRAND_SEGMENTS, col, FB);
                px = nx; py = ny; pz = nz;
            }
        }
    }

    // ------------------------------------------------------------------ the camera of the hand
    /**
     * The first person hand is drawn in view space (the camera looks down -z, right = +x, up = +y), not in the world: {@link SealDraw#camera} would read the world camera. Here the basis of
     * the view is carried back into the pose's local space with the transpose of its rotation, and written to {@link SealDraw#CAM} (right, up, look), where the glow / ribbon helpers read it.
     */
    public static void cameraFirstPerson(PoseStack pose) {
        org.joml.Matrix3f n = pose.last().normal();
        // local = transpose(normal) * view; the columns of normal are the images of the local axes, so the rows give the view axes in local space
        float[] cam = SealDraw.CAM;
        cam[0] = n.m00; cam[1] = n.m10; cam[2] = n.m20;                    // view +x (right)
        cam[3] = n.m01; cam[4] = n.m11; cam[5] = n.m21;                    // view +y (up)
        cam[6] = -n.m02; cam[7] = -n.m12; cam[8] = -n.m22;                 // view look = -z
        for (int i = 0; i < 9; i += 3) {
            float l = Mth.sqrt(cam[i] * cam[i] + cam[i + 1] * cam[i + 1] + cam[i + 2] * cam[i + 2]);
            if (l > 1e-6f) { cam[i] /= l; cam[i + 1] /= l; cam[i + 2] /= l; }
        }
    }
}
