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
 * 1.93.0 Dark Magic (Yami Sukehiro: the Dark Cloaked style): the temporary armour of Aura.DARK. Obsidian plate with violet inlay, every piece forged in and shattered out by
 * {@link EquipAnimation} like the Djinn full equip ({@link WaterAuraRig}): a horned hood that leaves the face open, a cuirass with a slit of the void and a high cloak collar,
 * a tattered cape that sways, spiked pauldrons, bracers, clawed gauntlets and greaves, wings of tattered shadow, three tendrils of darkness at the waist, the black moon turning
 * over the head, two crossed katanas on the back and the black hole disc behind them. Models: tools/gen_dark_aura_geo.py (geo + texture + glow map per piece).
 * <pre>
 *   pass                          render type          scale / order            why
 *   1 shadow skin shell           translucent          1.04 x body, first       smoke-black film over the body (the head is left alone: the face stays visible)
 *   2 plates, cape, wings ...     cutout / translucent per piece geo            solid obsidian with alpha holes (cape, wings, tendrils); translucent while forging / shattering
 *   3 glow maps                   additive, full bright per piece geo           the violet inlay of every piece
 *   4 violet haze                 additive, full bright 1.08 x body             last body pass
 *   5 black hole sigil, wisps     alpha blend / additive feet, round the body   the sigil on the ground, rising shadow, tendrils off the hands, the disc behind the back
 * </pre>
 * Styles: 0 = the ordinary Dark Cloaked cast; 1 = the Equinox / dimension cut (the haze turns white-violet).
 */
public final class DarkAura {
    private DarkAura() {}

    private static final ResourceLocation SHELL = AuraRender.tex("aura/dark_shell"), SHELL_GLOW = AuraRender.tex("aura/dark_shell_glow"), HAZE = AuraRender.tex("aura/dark_haze");
    private static final ResourceLocation HOLE = AuraRender.tex("particle/darkrb_hole"), RUNE = AuraRender.tex("particle/darkrb_rune"), WISP = AuraRender.tex("particle/darkrb_wisp"),
            TENDRIL = AuraRender.tex("particle/darkrb_tendril"), MOON = AuraRender.tex("particle/darkrb_moon");
    private static final int FB = PropDraw.FULL_BRIGHT, GLOW = 0xA24CFF, PALE = 0xE0B8FF;

    private static final WaterAuraRig.Set SET = new WaterAuraRig.Set("dark",
            Piece.of("chest", Anchor.BODY, 1, 0, 0, -14),
            Piece.of("cape", Anchor.BODY, 5, 0, -4, 12).clip("idle", 1f).glass().glow(0.6f).detail(),
            Piece.of("blades", Anchor.BODY, 9, 0, -6, 12).detail().glow(0f),
            Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0),
            Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0),
            Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6).glow(0.5f),
            Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6).glow(0.5f),
            Piece.of("gauntlet_r", Anchor.ARM_R, 5, -10, 8, -6),
            Piece.of("gauntlet_l", Anchor.ARM_L, 5, 10, 8, -6),
            Piece.of("greave_r", Anchor.LEG_R, 6, -3, 14, -6).glow(0.6f),
            Piece.of("greave_l", Anchor.LEG_L, 7, 3, 14, -6).glow(0.6f),
            Piece.of("helm", Anchor.HEAD, 0, 0, -16, 0).head(),
            Piece.of("wing_r", Anchor.BODY, 8, -10, -4, 12).clip("idle", 1f).flap().glass(),
            Piece.of("wing_l", Anchor.BODY, 8, 10, -4, 12).clip("idle", 1f).flap().glass(),
            Piece.of("tendrils", Anchor.BODY, 9, 0, 6, 14).clip("idle", 1f).glass().detail(),
            Piece.of("halo", Anchor.HEAD, 10, 0, -16, 0).clip("idle", 1f).detail().head(),
            Piece.of("backdrop", Anchor.BODY, 12, 0, 0, 12).clip("idle", 1f).detail());

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.DARK, DarkAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.75f + 0.25f * Mth.sin(age * 0.15f) + 0.08f * Mth.sin(age * 0.67f);
        float forge = WaterAuraRig.forge(c, 0f);
        boolean equinox = c.style() == 1;
        WaterAuraRig.shell(c, AuraRender.translucent(SHELL), 1.04f, 1.025f, 1.04f, AuraRender.alpha(0xFFFFFFFF, 0.85f * forge), false, true);
        WaterAuraRig.shell(c, AuraRender.additive(SHELL_GLOW), 1.045f, 1.03f, 1.045f, AuraRender.alpha(0xFFA24CFF, 0.6f * pulse * forge), true, true);
        WaterAuraRig.draw(c, SET, equinox ? PALE : GLOW, 0.8f * pulse + 0.3f);
        WaterAuraRig.shell(c, AuraRender.additive(HAZE), 1.085f + 0.012f * Mth.sin(age * 0.17f), 1.05f, 1.085f + 0.012f * Mth.sin(age * 0.17f),
                AuraRender.alpha(equinox ? 0xFFE0C0FF : 0xFF8A40E0, 0.17f * pulse * forge), true, true);
        extras(c, forge, pulse, equinox);
    }

    private static void extras(AuraContext c, float f, float pulse, boolean equinox) {
        if (f <= 0.01f) return;
        float age = c.age();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float grow = 0.55f + 0.45f * f;
        pose.pushPose();                                                                          // the black hole on the ground and the dark sigil over it
        pose.translate(0f, 1.478f, 0f);
        VertexConsumer hole = c.buffers().getBuffer(AuraRender.additive(HOLE));
        SealDraw.flat(pose, hole, 1.35f * grow, age * 1.1f, SealDraw.argb(0.7f * f, GLOW), FB);
        VertexConsumer rune = c.buffers().getBuffer(AuraRender.additive(RUNE));
        SealDraw.flat(pose, rune, 1.0f * grow, -age * 0.7f, SealDraw.argb(0.8f * f * pulse, equinox ? PALE : GLOW), FB);
        pose.popPose();
        if (WaterAuraRig.far(c)) return;
        VertexConsumer wisp = c.buffers().getBuffer(AuraRender.translucent(WISP));
        int lit = SealDraw.lift(c.light(), 4);
        for (int i = 0; i < 10; i++) {                                                            // black smoke rising round the body
            float ph = frac(age * 0.014f * (1f + 0.3f * (i % 3)) + i * 0.37f), a = i * 2.399f + age * 0.02f, r = 0.5f + 0.2f * Mth.sin(i * 1.7f) + 0.15f * ph;
            SealDraw.glow(pose, wisp, Mth.cos(a) * r, 1.4f - ph * 2.1f, Mth.sin(a) * r, 0.16f + 0.14f * ph, a, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.7f * f, 0x0A0612), lit);
        }
        VertexConsumer tendril = c.buffers().getBuffer(AuraRender.additive(TENDRIL));
        for (int side = -1; side <= 1; side += 2) {                                               // tendrils of darkness running off the hands
            ModelPart part = side < 0 ? c.model().rightArm : c.model().leftArm;
            pose.pushPose();
            part.translateAndRotate(pose);
            SealDraw.camera(pose);
            for (int k = 0; k < 2; k++) {
                float ph = frac(age * 0.03f + k * 0.5f + (side > 0 ? 0.2f : 0f)), sway = Mth.sin(age * 0.17f + k * 2f + side) * 0.12f;
                SealDraw.ribbon(pose, tendril, side * 0.02f, 0.72f, 0.0f, side * 0.12f + sway, 0.72f + 0.35f + 0.25f * ph, 0.06f * (k == 0 ? 1 : -1) + sway, 0.07f, 0f, 1f,
                        SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.8f * f, k == 0 ? GLOW : PALE), FB);
            }
            pose.popPose();
        }
        float back = WaterAuraRig.forge(c, 12f);
        if (back > 0.01f) {                                                                       // the black hole behind the back and the moon over the head
            pose.pushPose();
            c.model().body.translateAndRotate(pose);
            VertexConsumer hh = c.buffers().getBuffer(AuraRender.additive(HOLE));
            float r = 0.45f * (0.6f + 0.4f * back), a = -age * 0.03f, ux = Mth.cos(a) * r, uy = Mth.sin(a) * r;
            int col = SealDraw.argb(0.7f * back, GLOW);
            SealDraw.plane(pose, hh, 0f, 0.31f, 0.78f, ux, uy, 0f, -uy, ux, 0f, false, col, FB);
            SealDraw.plane(pose, hh, 0f, 0.31f, 0.78f, ux, uy, 0f, -uy, ux, 0f, true, col, FB);
            pose.popPose();
        }
        float halo = WaterAuraRig.forge(c, 10f);
        if (halo > 0.01f) {
            pose.pushPose();
            c.model().head.translateAndRotate(pose);
            SealDraw.camera(pose);
            VertexConsumer moon = c.buffers().getBuffer(AuraRender.additive(MOON));
            SealDraw.glow(pose, moon, 0f, -0.66f, 0f, 0.20f * (0.7f + 0.3f * halo), age * 0.01f, SealDraw.argb(0.22f * halo, GLOW), FB);
            pose.popPose();
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
