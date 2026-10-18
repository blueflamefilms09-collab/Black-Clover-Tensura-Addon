package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Anchor;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Piece;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Flame Magic (Fuegoleon / Mereoleona: the Crimson Lion armour): the temporary armour of Aura.FLAME. A real set of pieces on the player's model, all forged in and
 * shattered out by {@link EquipAnimation} exactly like the Djinn full equip ({@link WaterAuraRig}): a lion's helm with a crest of flame, a flame mane round the head,
 * breastplate with the sun-lion, tassets, lion-head pauldrons, finned vambraces, clawed gauntlets, spiked greaves, flame wings, a lion's tail, a crown of flames
 * and the sun-lion disc turning behind the back. Models: tools/gen_flame_aura_geo.py (geo + texture + emissive glow map per piece).
 * <pre>
 *   pass                          render type          scale / order            why
 *   1 ember skin shell            translucent          1.04 x body, first       the dark ember skin under the plates (cracks glow through)
 *   2 plates, mane, wings, ...    cutout / translucent per piece geo            solid plate with alpha holes; translucent while forging / shattering and for flame sheets
 *   3 glow maps                   additive, full bright per piece geo            the emissive part of every piece, pulsing
 *   4 heat haze                   additive, full bright 1.08 x body              last: a hot shimmer round the whole body
 *   5 ring of fire, embers        additive, full bright feet / rising            the ground ring and the rising sparks (sprites, never the skin)
 * </pre>
 * The style number is unused (one look); the aura lasts as long as the spell says (5 s for an ordinary cast).
 */
public final class FlameAura {
    private FlameAura() {}

    private static final ResourceLocation SHELL = AuraRender.tex("aura/flame_shell"), SHELL_GLOW = AuraRender.tex("aura/flame_shell_glow"), HEAT = AuraRender.tex("aura/flame_heat");
    private static final ResourceLocation RING = AuraRender.tex("particle/flamefx_ring"), EMBER = AuraRender.tex("particle/flamefx_ember"),
            TONGUE = AuraRender.tex("particle/flamefx_tongue"), LION = AuraRender.tex("particle/flamefx_lion");
    private static final int FB = PropDraw.FULL_BRIGHT, GLOW = 0xFF8A2A, HOT = 0xFFD070, FLAME_BODY = 0xFF5A14, FLAME_CORE = 0xFFA838;

    /** The pieces, in draw order. The delays and the fly-in directions are the Djinn full equip's (head from above, arms from their side, greaves from below, the back from behind). */
    private static final WaterAuraRig.Set SET = new WaterAuraRig.Set("flame",
            Piece.of("chest", Anchor.BODY, 1, 0, 0, -14),
            Piece.of("skirt", Anchor.BODY, 5, 0, 10, -6).detail().glow(0f),
            Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0),
            Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0),
            Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6).glow(0f),
            Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6).glow(0f),
            Piece.of("gauntlet_r", Anchor.ARM_R, 5, -10, 8, -6),
            Piece.of("gauntlet_l", Anchor.ARM_L, 5, 10, 8, -6),
            Piece.of("greave_r", Anchor.LEG_R, 6, -3, 14, -6).glow(0f),
            Piece.of("greave_l", Anchor.LEG_L, 7, 3, 14, -6).glow(0f),
            Piece.of("helm", Anchor.HEAD, 0, 0, -16, 0).head(),
            Piece.of("wing_r", Anchor.BODY, 8, -10, -4, 12).clip("idle", 1f).flap().glass().phase(0.0f),
            Piece.of("wing_l", Anchor.BODY, 8, 10, -4, 12).clip("idle", 1f).flap().glass().phase(0.0f),
            Piece.of("tail", Anchor.BODY, 9, 0, 6, 14).clip("idle", 1f).detail(),
            Piece.of("halo", Anchor.HEAD, 10, 0, -16, 0).clip("idle", 1f).glass().detail().head(),
            Piece.of("backdrop", Anchor.BODY, 12, 0, 0, 12).clip("idle", 1f).glass().detail());

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.FLAME, FlameAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.78f + 0.22f * Mth.sin(age * 0.23f) + 0.1f * Mth.sin(age * 0.71f);
        float forge = WaterAuraRig.forge(c, 0f);
        // 1. the dark ember skin: solid-ish, 4 % over the body, first
        WaterAuraRig.shell(c, AuraRender.translucent(SHELL), 1.04f, 1.025f, 1.04f, AuraRender.alpha(0xFFFFFFFF, 0.9f * forge), false, true);
        WaterAuraRig.shell(c, AuraRender.additive(SHELL_GLOW), 1.045f, 1.03f, 1.045f, AuraRender.alpha(0xFFFF7A22, 0.7f * pulse * forge), true, true);
        // 2 + 3. the armour
        WaterAuraRig.draw(c, SET, GLOW, 0.8f * pulse + 0.35f);
        // 4. the heat haze round everything, last of the body passes
        WaterAuraRig.shell(c, AuraRender.additive(HEAT), 1.085f + 0.01f * Mth.sin(age * 0.2f), 1.05f, 1.085f + 0.01f * Mth.sin(age * 0.2f), AuraRender.alpha(0xFFFF8A2A, 0.20f * pulse * forge), true, true);
        // 5. the ring of fire at the feet, the sun-lion on the ground and the rising embers
        extras(c, forge, pulse);
        flames(c);
    }

    /**
     * The flames that stand on the armour (the crest, the mane round the head, the shoulder horns, the crown): camera-facing ribbons textured with the flame lick, drawn in the local
     * frame of the part they belong to (so they follow it), additive, full bright. They forge in with the piece they sit on (its delay) and shatter out with it.
     */
    private static void flames(AuraContext c) {
        float age = c.age();
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(TONGUE));
        boolean near = !WaterAuraRig.far(c);
        float head = WaterAuraRig.forge(c, 3f), halo = WaterAuraRig.forge(c, 10f), arm = WaterAuraRig.forge(c, 2f);
        pose.pushPose();                                                                          // the head: the crest, the mane, the crown
        c.model().head.translateAndRotate(pose);
        SealDraw.camera(pose);
        if (head > 0.01f) {
            for (int k = -1; k <= 1; k++) {                                                       // three flames along the crest
                float fl = 0.5f + 0.5f * Mth.sin(age * 0.35f + k * 2.1f);
                flame(pose, vc, 0f, -0.56f, k * 0.2f, 0.03f * Mth.sin(age * 0.2f + k), -0.56f - 0.30f - 0.07f * fl - (k == 0 ? 0.08f : 0f), k * 0.2f, 0.09f, head, age + k * 3f, FLAME_BODY, FLAME_CORE);
            }
            if (near) for (int i = 0; i < 9; i++) {                                               // the mane streaming out round the back of the head
                float ang = (i - 4) * 0.62f, fl = 0.5f + 0.5f * Mth.sin(age * 0.3f + i * 1.7f);
                float bx = Mth.sin(ang) * 0.27f, bz = Mth.cos(ang) * 0.27f, len = 0.40f + 0.1f * fl;
                flame(pose, vc, bx, -0.25f, bz, bx * 0.0f + Mth.sin(ang) * (0.27f + len), -0.25f - 0.16f - 0.06f * fl, Mth.cos(ang) * (0.27f + len), 0.11f, head, age + i * 2f, FLAME_BODY, FLAME_CORE);
            }
        }
        if (halo > 0.01f && near) {                                                               // the crown of flames on the spinning ring
            float spin = age / 20f * 0.785f;
            for (int i = 0; i < 8; i++) {
                float a = spin + i * Mth.TWO_PI / 8f, fl = 0.5f + 0.5f * Mth.sin(age * 0.4f + i * 1.3f);
                float x = Mth.cos(a) * 0.46f, z = Mth.sin(a) * 0.46f;
                flame(pose, vc, x, -0.66f, z, x, -0.66f - 0.30f - 0.1f * fl, z, 0.08f, halo, age + i * 5f, FLAME_BODY, FLAME_CORE);
            }
        }
        pose.popPose();
        float back = WaterAuraRig.forge(c, 12f);
        if (back > 0.01f && near) {                                                               // the sun-lion sigil turning behind the back (twice, counter-rotating)
            pose.pushPose();
            c.model().body.translateAndRotate(pose);
            VertexConsumer lion = c.buffers().getBuffer(AuraRender.additive(LION));
            for (int k = 0; k < 2; k++) {
                float r = (k == 0 ? 0.62f : 0.42f) * (0.6f + 0.4f * back), a = (k == 0 ? 1f : -1.7f) * age * 0.018f;
                float ux = Mth.cos(a) * r, uy = Mth.sin(a) * r;
                int col = SealDraw.argb((k == 0 ? 0.5f : 0.75f) * back, k == 0 ? GLOW : HOT);
                SealDraw.plane(pose, lion, 0f, 0.31f, 0.60f + 0.01f * k, ux, uy, 0f, -uy, ux, 0f, false, col, FB);
                SealDraw.plane(pose, lion, 0f, 0.31f, 0.60f + 0.01f * k, ux, uy, 0f, -uy, ux, 0f, true, col, FB);
            }
            pose.popPose();
        }
        if (arm > 0.01f) for (int side = -1; side <= 1; side += 2) {                              // the shoulder horns
            ModelPart part = side < 0 ? c.model().rightArm : c.model().leftArm;
            pose.pushPose();
            part.translateAndRotate(pose);
            SealDraw.camera(pose);
            for (int k = -1; k <= 1; k += 2) {
                float fl = 0.5f + 0.5f * Mth.sin(age * 0.33f + k * 2f + side);
                flame(pose, vc, side * 0.125f, -0.30f, k * 0.12f, side * 0.15f, -0.30f - 0.26f - 0.08f * fl, k * 0.12f, 0.075f, arm, age + k * 4f + side * 7f, FLAME_BODY, FLAME_CORE);
            }
            pose.popPose();
        }
    }

    /** One flame: a tall ribbon from the base to the tip (two layers: the red body and a narrower pale core). */
    private static void flame(PoseStack pose, VertexConsumer vc, float bx, float by, float bz, float tx, float ty, float tz, float hw, float k, float t, int body, int core) {
        float sway = Mth.sin(t * 0.37f) * 0.025f;
        SealDraw.ribbon(pose, vc, bx, by, bz, tx + sway, ty, tz + sway, hw, 0f, 1f, SealDraw.argb(0.7f * k, body), FB);
        SealDraw.ribbon(pose, vc, bx, by, bz, tx + sway * 0.5f, by + (ty - by) * 0.72f, tz + sway * 0.5f, hw * 0.5f, 0f, 0.8f, SealDraw.argb(0.8f * k, core), FB);
    }

    private static void extras(AuraContext c, float f, float pulse) {
        if (f <= 0.01f) return;
        float age = c.age();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float grow = 0.55f + 0.45f * f;
        pose.pushPose();
        pose.translate(0f, 1.475f, 0f);
        VertexConsumer lion = c.buffers().getBuffer(AuraRender.additive(LION));
        SealDraw.flat(pose, lion, 1.25f * grow, age * 0.9f, SealDraw.argb(0.8f * f, GLOW), FB);
        VertexConsumer ring = c.buffers().getBuffer(AuraRender.additive(RING));
        SealDraw.flat(pose, ring, 0.9f * grow, -age * 1.6f, SealDraw.argb(0.9f * f * pulse, HOT), FB);
        pose.popPose();
        if (WaterAuraRig.far(c)) return;
        VertexConsumer ember = c.buffers().getBuffer(AuraRender.additive(EMBER));
        for (int i = 0; i < 14; i++) {
            float ph = frac(age * 0.016f * (1f + 0.35f * (i % 3)) + i * 0.37f), a = i * 2.399f + age * 0.035f;
            float r = 0.45f + 0.22f * Mth.sin(i * 1.9f) + 0.1f * ph;
            SealDraw.glow(pose, ember, Mth.cos(a) * r, 1.45f - ph * 2.2f, Mth.sin(a) * r, 0.06f + 0.045f * (1f - ph), a, SealDraw.argb(Mth.sin(ph * Mth.PI) * f, i % 2 == 0 ? HOT : GLOW), FB);
        }
        VertexConsumer tongue = c.buffers().getBuffer(AuraRender.additive(TONGUE));
        for (int side = -1; side <= 1; side += 2) {                                              // flames licking off the shoulders and the back of the hands
            for (int k = 0; k < 2; k++) {
                float ph = frac(age * 0.045f + k * 0.5f + (side > 0 ? 0.23f : 0f)), sway = Mth.sin(age * 0.2f + k * 2f + side) * 0.06f;
                float x = side * (0.38f + 0.04f * k), y = 0.18f - ph * 0.5f + k * 1.0f;
                SealDraw.ribbon(pose, tongue, x, y + 0.34f, 0.05f, x + sway + side * 0.07f, y - 0.18f - 0.3f * ph, 0.08f, 0.13f * (1f - 0.4f * ph), 0f, 1f,
                        SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * f, GLOW), FB);
            }
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
