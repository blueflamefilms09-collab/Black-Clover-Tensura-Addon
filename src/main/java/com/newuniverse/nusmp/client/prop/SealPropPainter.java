package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 0.54 Seal Magic: how this magic's props look.
 * <ul>
 *   <li>SEALING_1, the basic seal: a flat rune circle (one of four designs, picked by the seed) with counter-rotating outer ring, seal shape and
 *       inner ornament, a soft disc, an opening pulse and 0 to 5 glowing orbs with trails. param = shape | tint << 2 | orbs << 4
 *       (shape 0 hexagram, 1 concentric rings, 2 star; tint 0 red, 1 blue, 2 purple). The circle radius is e.scale().</li>
 *   <li>SEALING_2, the Eternal Prison: a translucent glowing cube of edge e.scale() round the prisoner: two glass shells, glowing edges, corner
 *       flares, white speed lines and sparks shooting off the edges, a slow rune cross inside and a crack web over the last 20 ticks.</li>
 * </ul>
 * Render types: translucent for the two glass shells (the outer faces out, the inner faces in, at 1.0 and 0.965 of the half edge: no coplanar faces),
 * additive for every glow (no depth write), all of it flat sprites or camera-facing ribbons: a few hundred vertices.
 */
public final class SealPropPainter {
    private SealPropPainter() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final int[] MAIN = {0xFF4A38, 0x3C9CFF, 0xA252FF};
    private static final int[] HI = {0xFFB898, 0xB8E6FF, 0xE4C4FF};
    private static final int CYAN = 0x4FD8FF, ICE = 0xB8F2FF;

    private static final String[] DESIGNS = {"fantasy", "scifi", "gothic", "anime"};
    private static final String[] SHAPES = {"hexagram", "rings", "star"};
    private static final ResourceLocation[] OUTER = new ResourceLocation[4], INNER = new ResourceLocation[4], SHAPE = new ResourceLocation[3], CRACK = new ResourceLocation[3];
    private static ResourceLocation disc, orb, trail, flare, edge, line, pane, cross;

    /** Scratch for the cube edges (render thread only). */
    private static final float[] ET = new float[3], EH = new float[3];

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        for (int i = 0; i < 4; i++) {
            OUTER[i] = AuraRender.tex("prop/seal_outer_" + DESIGNS[i]);
            INNER[i] = AuraRender.tex("prop/seal_inner_" + DESIGNS[i]);
        }
        for (int i = 0; i < 3; i++) {
            SHAPE[i] = AuraRender.tex("prop/seal_shape_" + SHAPES[i]);
            CRACK[i] = AuraRender.tex("prop/seal_cube_crack_" + (i + 1));
        }
        disc = AuraRender.tex("prop/seal_disc");
        orb = AuraRender.tex("prop/seal_orb");
        trail = AuraRender.tex("prop/seal_trail");
        flare = AuraRender.tex("prop/seal_flare");
        edge = AuraRender.tex("prop/seal_edge");
        line = AuraRender.tex("prop/seal_line");
        pane = AuraRender.tex("prop/seal_cube_pane");
        cross = AuraRender.tex("prop/seal_cube_cross");
        PropPainters.register(PropKind.SEALING_1, SealPropPainter::circle);
        PropPainters.register(PropKind.SEALING_2, SealPropPainter::prison);
    }

    // ================================================================================================ SEALING_1: the basic seal circle
    private static void circle(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int p = e.param();
        int shape = (p & 3) % 3, tint = ((p >> 2) & 3) % 3, orbs = Mth.clamp((p >> 4) & 7, 0, 5);
        int design = Math.floorMod(e.seed(), 4);
        float maxL = Math.max(1f, e.maxLife());
        float fin = SealDraw.smooth(age / 10f), f = Math.min(fin, SealDraw.smooth((maxL - age) / 14f));
        if (f <= 0.002f) return;
        float r = Math.max(0.4f, e.scale()) * (0.55f + 0.45f * fin);
        float breathe = 0.85f + 0.15f * Mth.sin(age * 0.18f);
        int main = MAIN[tint], hi = HI[tint], soft = SealDraw.mix(main, hi, 0.35f), bright = SealDraw.mix(hi, 0xFFFFFF, 0.5f);
        boolean target = e.target() != null;
        SealDraw.camera(pose);
        pose.pushPose();
        pose.translate(0f, 0.03f, 0f);
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(disc));
        SealDraw.flat(pose, vc, r, 0f, SealDraw.argb(0.42f * f * breathe, main), FB);
        vc = buffers.getBuffer(AuraRender.additive(OUTER[design]));
        float spin = age * 0.9f;
        SealDraw.flat(pose, vc, r, -spin, SealDraw.argb(0.95f * f, soft), FB);
        SealDraw.flat(pose, vc, r * 1.045f, -spin, SealDraw.argb(0.28f * f * breathe, main), FB);
        if (age < 16f) {                                                                 // the opening pulse
            float t = age / 16f;
            SealDraw.flat(pose, vc, r * (0.7f + 0.9f * t), -spin * 2f, SealDraw.argb(0.55f * (1f - t) * fin, bright), FB);
        }
        vc = buffers.getBuffer(AuraRender.additive(SHAPE[shape]));
        SealDraw.flat(pose, vc, r * 0.76f, age * 1.6f, SealDraw.argb(0.92f * f, hi), FB);
        vc = buffers.getBuffer(AuraRender.additive(INNER[design]));
        SealDraw.flat(pose, vc, r * 0.46f, -age * 3.2f, SealDraw.argb(0.95f * f, bright), FB);
        SealDraw.flat(pose, vc, r * 0.5f, -age * 3.2f, SealDraw.argb(0.3f * f * breathe, main), FB);
        pose.popPose();
        if (orbs > 0) orbs(pose, buffers, age, orbs, r, target, f, main, bright, e.seed());
    }

    private static void orbs(PoseStack pose, MultiBufferSource buffers, float age, int n, float r, boolean target, float f, int main, int bright, int seed) {
        float orbit = r * (target ? 0.78f : 0.93f), base = target ? 0.95f : 0.4f, dir = (seed & 8) == 0 ? 1f : -1f;
        float w = 0.075f * dir, hw = 0.04f + 0.012f * Math.min(r, 3f);
        VertexConsumer vc = buffers.getBuffer(AuraRender.additive(trail));
        for (int i = 0; i < n; i++) {
            float a = age * w + Mth.TWO_PI * i / n, bob = base + 0.12f * Mth.sin(age * 0.11f + i * 1.7f);
            for (int k = 0; k < 3; k++) {
                float a0 = a - dir * 0.17f * (k + 1), a1 = a - dir * 0.17f * k;
                SealDraw.ribbon(pose, vc, Mth.cos(a0) * orbit, bob, Mth.sin(a0) * orbit, Mth.cos(a1) * orbit, bob, Mth.sin(a1) * orbit, hw * (1f - 0.18f * k),
                        1f - (k + 1) / 3f, 1f - k / 3f, SealDraw.argb(0.8f * f, main), FB);
            }
        }
        vc = buffers.getBuffer(AuraRender.additive(orb));
        for (int i = 0; i < n; i++) {
            float a = age * w + Mth.TWO_PI * i / n, bob = base + 0.12f * Mth.sin(age * 0.11f + i * 1.7f);
            float px = Mth.cos(a) * orbit, pz = Mth.sin(a) * orbit, pulse = 1f + 0.18f * Mth.sin(age * 0.3f + i * 2.1f);
            SealDraw.glow(pose, vc, px, bob, pz, 0.3f * pulse, 0f, SealDraw.argb(0.85f * f, main), FB);
            SealDraw.glow(pose, vc, px, bob, pz, 0.12f * pulse, 0f, SealDraw.argb(f, bright), FB);
        }
    }

    // ================================================================================================ SEALING_2: Eternal Prison
    private static void prison(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.6f, e.scale());
        float maxL = Math.max(1f, e.maxLife());
        float fin = SealDraw.smooth(age / 6f), f = Math.min(fin, SealDraw.smooth((maxL - age) / 5f));
        if (f <= 0.002f) return;
        float pop = 0.82f + 0.18f * fin + 0.05f * Mth.sin(Mth.clamp(age / 8f, 0f, 1f) * Mth.PI);
        float prog = Mth.clamp((age - (maxL - 20f)) / 20f, 0f, 1f);
        float shake = 0.014f * s * prog * Mth.sin(age * 5.3f);
        float h = s * 0.5f * pop, pulse = 0.86f + 0.14f * Mth.sin(age * 0.4f);
        int seed = e.seed(), glass = SealDraw.lift(light, 11);
        pose.pushPose();
        pose.translate(shake, s * 0.5f - 0.1f, shake * 0.7f);
        SealDraw.camera(pose);
        // 1. glass: the outer shell faces out, the inner shell faces in (translucent, no culling, 3.5 % apart)
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(pane));
        shell(pose, vc, h, false, SealDraw.argb(0.62f * f, 0xFFFFFF), glass);
        shell(pose, vc, h * 0.965f, true, SealDraw.argb(0.42f * f, 0xFFFFFF), glass);
        // 2. glowing edges: a wide halo and a thin hot core on each of the 12 edges
        vc = buffers.getBuffer(AuraRender.additive(edge));
        float hw = 0.07f + 0.025f * s;
        int halo = SealDraw.argb(0.55f * f * pulse, CYAN), core = SealDraw.argb(0.95f * f, ICE);
        for (int i = 0; i < 12; i++) {
            wholeEdge(i, h);
            SealDraw.ribbon(pose, vc, ET[0], ET[1], ET[2], EH[0], EH[1], EH[2], hw, 0f, 1f, halo, FB);
            SealDraw.ribbon(pose, vc, ET[0], ET[1], ET[2], EH[0], EH[1], EH[2], hw * 0.4f, 0f, 1f, core, FB);
        }
        // 3. flares on the corners and sparks off the edges
        vc = buffers.getBuffer(AuraRender.additive(flare));
        for (int i = 0; i < 8; i++) {
            float sz = (0.13f + 0.05f * Mth.sin(age * 0.25f + i * 2.1f)) * (0.8f + 0.2f * s);
            SealDraw.glow(pose, vc, (i & 1) == 0 ? -h : h, (i & 2) == 0 ? -h : h, (i & 4) == 0 ? -h : h, sz, age * 0.03f + i, SealDraw.argb(0.9f * f, ICE), FB);
        }
        for (int i = 0; i < 10; i++) {
            float tt = age * (0.045f + 0.03f * SealDraw.rnd(seed, 20 + i, 1)) + SealDraw.rnd(seed, 20 + i, 2);
            int cyc = Mth.floor(tt);
            float t = tt - cyc, fade = Mth.sin(t * Mth.PI);
            outPoints(Math.min(11, (int) (SealDraw.rnd(seed, 20 + i, 3 + cyc * 7) * 12f)), h, (SealDraw.rnd(seed, 20 + i, 4 + cyc * 7) * 2f - 1f) * h * 0.92f,
                    0f, 0.03f * s + t * s * 0.2f);
            SealDraw.glow(pose, vc, EH[0], EH[1], EH[2], (0.06f + 0.05f * SealDraw.rnd(seed, 20 + i, 5)) * (0.7f + 0.3f * s), age * 0.2f + i, SealDraw.argb(f * fade, 0xFFFFFF), FB);
        }
        // 4. white speed lines shooting away from the edges
        vc = buffers.getBuffer(AuraRender.additive(line));
        float lw = 0.03f + 0.01f * s;
        for (int i = 0; i < 22; i++) {
            float tt = age * (0.035f + 0.03f * SealDraw.rnd(seed, i, 1)) + SealDraw.rnd(seed, i, 2);
            int cyc = Mth.floor(tt);
            float t = tt - cyc;
            float d1 = 0.03f * s + t * s * (0.45f + 0.5f * SealDraw.rnd(seed, i, 5));
            float d0 = Math.max(0.02f * s, d1 - s * (0.25f + 0.35f * SealDraw.rnd(seed, i, 6)));
            outPoints(Math.min(11, (int) (SealDraw.rnd(seed, i, 3 + cyc * 7) * 12f)), h, (SealDraw.rnd(seed, i, 4 + cyc * 7) * 2f - 1f) * h * 0.92f, d0, d1);
            SealDraw.ribbon(pose, vc, ET[0], ET[1], ET[2], EH[0], EH[1], EH[2], lw, 0f, 1f, SealDraw.argb(0.95f * f * Mth.sin(t * Mth.PI), 0xFFFFFF), FB);
        }
        // 5. the slow inner rune cross: three rune planes turning about different axes
        vc = buffers.getBuffer(AuraRender.additive(cross));
        float cs = 0.3f * s * pop;
        int rune = SealDraw.argb(0.85f * f * pulse, ICE);
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(age * 1.1f + i * 90f));
            SealDraw.plane(pose, vc, 0f, 0f, 0f, cs, 0f, 0f, 0f, cs, 0f, false, rune, FB);
            SealDraw.plane(pose, vc, 0f, 0f, 0f, cs, 0f, 0f, 0f, cs, 0f, true, rune, FB);
            pose.popPose();
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-age * 1.7f));
        SealDraw.plane(pose, vc, 0f, 0f, 0f, cs, 0f, 0f, 0f, 0f, cs, false, rune, FB);
        SealDraw.plane(pose, vc, 0f, 0f, 0f, cs, 0f, 0f, 0f, 0f, cs, true, rune, FB);
        pose.popPose();
        // 6. the crack web grows over the last 20 ticks, in three steps that sit on top of each other
        if (prog > 0f) {
            for (int i = 0; i < 3; i++) {
                float a = SealDraw.smooth((prog - 0.3f * i) / 0.4f) * f;
                if (a <= 0.01f) continue;
                vc = buffers.getBuffer(AuraRender.additive(CRACK[i]));
                shell(pose, vc, h * 1.004f, false, SealDraw.argb(a, 0xFFFFFF), FB);
            }
        }
        pose.popPose();
    }

    /** ET to EH = the whole edge i (0..11) of a cube of half edge h: it runs along axis i / 4 at the corner given by the two low bits. */
    private static void wholeEdge(int i, float h) {
        int a = i >> 2, b = (a + 1) % 3, c = (a + 2) % 3;
        ET[a] = -h; EH[a] = h;
        ET[b] = EH[b] = ((i & 1) * 2f - 1f) * h;
        ET[c] = EH[c] = (((i >> 1) & 1) * 2f - 1f) * h;
    }

    /** ET and EH = the point 'along' edge i, pushed diagonally away from the cube by dTail and by dHead blocks (speed lines and sparks). */
    private static void outPoints(int i, float h, float along, float dTail, float dHead) {
        int a = i >> 2, b = (a + 1) % 3, c = (a + 2) % 3;
        float sb = (i & 1) * 2f - 1f, sc = ((i >> 1) & 1) * 2f - 1f;
        ET[a] = EH[a] = along;
        ET[b] = sb * (h + dTail * 0.7071f); EH[b] = sb * (h + dHead * 0.7071f);
        ET[c] = sc * (h + dTail * 0.7071f); EH[c] = sc * (h + dHead * 0.7071f);
    }

    /** Six faces of a cube of half edge h, outward (inward = false) or inward facing. */
    private static void shell(PoseStack pose, VertexConsumer vc, float h, boolean inward, int argb, int light) {
        for (int n = 0; n < 3; n++) {
            int ua = (n + 1) % 3, va = (n + 2) % 3;
            float ux = ua == 0 ? h : 0f, uy = ua == 1 ? h : 0f, uz = ua == 2 ? h : 0f;
            float vx = va == 0 ? h : 0f, vy = va == 1 ? h : 0f, vz = va == 2 ? h : 0f;
            for (int sg = -1; sg <= 1; sg += 2) {
                SealDraw.plane(pose, vc, n == 0 ? sg * h : 0f, n == 1 ? sg * h : 0f, n == 2 ? sg * h : 0f, ux, uy, uz, vx, vy, vz, (sg < 0) != inward, argb, light);
            }
        }
    }
}
