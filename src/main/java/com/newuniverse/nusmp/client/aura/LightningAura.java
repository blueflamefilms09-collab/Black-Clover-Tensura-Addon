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
 * 1.93.0 Lightning Magic (Luck Voltia: the Raging Lightning armour): the temporary armour of Aura.LIGHTNING. White-and-gold enamel plate with a glowing bolt inlay, every piece
 * forged in and shattered out by {@link EquipAnimation} like the Djinn full equip ({@link WaterAuraRig}): a winged circlet with a crown of bolts, a cuirass with gold chevrons,
 * tassets, pauldrons with swept spikes, bracers, gauntlets with gold finger plates, thunder boots with winged heels, wings and a tail of zig-zag bolts, a ring of electrodes
 * over the head and the Raijin thunder-drum ring behind the back. Models: tools/gen_lightning_aura_geo.py (geo + texture + glow map per piece).
 * <pre>
 *   pass                          render type          scale / order            why
 *   1 enamel skin shell           translucent          1.04 x body, first       a white-blue film over the body (the head is left alone: the face stays visible)
 *   2 plates, wings, tail ...     cutout / translucent per piece geo            solid enamel with alpha holes; translucent while forging / shattering
 *   3 glow maps                   additive, full bright per piece geo           the bolt inlay of every piece, flickering
 *   4 electric haze               additive, full bright 1.08 x body             last body pass
 *   5 arcs, sparks, ring          additive, full bright round the body, feet    jagged arcs that re-roll every three ticks, sparks, the ground ring, the drum sigil
 * </pre>
 * Styles: 0 = the Raging Lightning armour (white enamel, electric blue); 1 = Black Lightning (the enamel goes iron-grey, the glow blood red).
 */
public final class LightningAura {
    private LightningAura() {}

    private static final ResourceLocation SHELL = AuraRender.tex("aura/lightning_shell"), SHELL_GLOW = AuraRender.tex("aura/lightning_shell_glow"), HAZE = AuraRender.tex("aura/lightning_haze");
    private static final ResourceLocation ARC = AuraRender.tex("particle/lightningfx_arc"), RING = AuraRender.tex("particle/lightningfx_ring"), SPARK = AuraRender.tex("particle/lightningfx_spark"),
            SIGIL = AuraRender.tex("particle/lightningfx_sigil"), FLASH = AuraRender.tex("particle/lightningfx_flash");
    private static final int FB = PropDraw.FULL_BRIGHT;

    private static final WaterAuraRig.Set SET = new WaterAuraRig.Set("lightning",
            Piece.of("chest", Anchor.BODY, 1, 0, 0, -14),
            Piece.of("skirt", Anchor.BODY, 5, 0, 10, -6).clip("idle", 1f).glow(0f).detail(),
            Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0),
            Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0),
            Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6).glow(0f),
            Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6).glow(0f),
            Piece.of("gauntlet_r", Anchor.ARM_R, 5, -10, 8, -6),
            Piece.of("gauntlet_l", Anchor.ARM_L, 5, 10, 8, -6),
            Piece.of("greave_r", Anchor.LEG_R, 6, -3, 14, -6),
            Piece.of("greave_l", Anchor.LEG_L, 7, 3, 14, -6),
            Piece.of("helm", Anchor.HEAD, 0, 0, -16, 0).head(),
            Piece.of("wing_r", Anchor.BODY, 8, -10, -4, 12).clip("idle", 1f).flap().glow(1.2f),
            Piece.of("wing_l", Anchor.BODY, 8, 10, -4, 12).clip("idle", 1f).flap().glow(1.2f),
            Piece.of("tail", Anchor.BODY, 9, 0, 6, 14).clip("idle", 1f).glow(1.2f).detail(),
            Piece.of("halo", Anchor.HEAD, 10, 0, -16, 0).clip("idle", 1f).detail().head(),
            Piece.of("backdrop", Anchor.BODY, 12, 0, 0, 12).clip("idle", 1f).detail());

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.LIGHTNING, LightningAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        boolean black = c.style() == 1;
        int glow = black ? 0xFF3A3A : 0x6AB4FF, pale = black ? 0xFF9090 : 0xE8F6FF;
        float age = c.age(), flick = 0.7f + 0.3f * Mth.sin(age * 1.7f) * Mth.sin(age * 0.43f) + 0.15f * Mth.sin(age * 0.25f);
        float forge = WaterAuraRig.forge(c, 0f);
        WaterAuraRig.shell(c, AuraRender.translucent(SHELL), 1.04f, 1.025f, 1.04f, AuraRender.alpha(black ? 0xFF5A5A6E : 0xFFFFFFFF, 0.75f * forge), false, true);
        WaterAuraRig.shell(c, AuraRender.additive(SHELL_GLOW), 1.045f, 1.03f, 1.045f, AuraRender.alpha(0xFF000000 | glow, 0.75f * flick * forge), true, true);
        WaterAuraRig.draw(c, SET, glow, 0.8f * flick + 0.3f, black ? 0x5A5A6E : 0xFFFFFF);
        WaterAuraRig.shell(c, AuraRender.additive(HAZE), 1.085f, 1.05f, 1.085f, AuraRender.alpha(0xFF000000 | glow, 0.17f * flick * forge), true, true);
        extras(c, forge, flick, glow, pale);
    }

    /** Hash to 0..1 that changes every three ticks (the arcs re-roll: lightning never holds still). */
    private static float roll(AuraContext c, int i, int k) { return SealDraw.rnd(c.player().getId() * 31 + (int) (c.age() / 3f), i, k); }

    private static void extras(AuraContext c, float f, float flick, int glow, int pale) {
        if (f <= 0.01f) return;
        float age = c.age();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float grow = 0.55f + 0.45f * f;
        pose.pushPose();                                                                          // the electric ring on the ground, the drum sigil over it
        pose.translate(0f, 1.478f, 0f);
        VertexConsumer ring = c.buffers().getBuffer(AuraRender.additive(RING));
        SealDraw.flat(pose, ring, 1.1f * grow, age * 2.2f, SealDraw.argb(0.85f * f * flick, glow), FB);
        VertexConsumer sig = c.buffers().getBuffer(AuraRender.additive(SIGIL));
        SealDraw.flat(pose, sig, 0.85f * grow, -age * 1.4f, SealDraw.argb(0.7f * f, pale), FB);
        pose.popPose();
        if (WaterAuraRig.far(c)) return;
        VertexConsumer arc = c.buffers().getBuffer(AuraRender.additive(ARC));
        for (int i = 0; i < 6; i++) {                                                             // arcs jumping between points round the body
            if (roll(c, i, 0) < 0.35f) continue;
            float a0 = roll(c, i, 1) * Mth.TWO_PI, a1 = a0 + (roll(c, i, 2) - 0.5f) * 2.4f;
            float y0 = 1.45f - roll(c, i, 3) * 2.0f, y1 = y0 + (roll(c, i, 4) - 0.5f) * 0.9f, r0 = 0.45f + 0.15f * roll(c, i, 5), r1 = 0.5f + 0.35f * roll(c, i, 6);
            SealDraw.ribbon(pose, arc, Mth.cos(a0) * r0, y0, Mth.sin(a0) * r0, Mth.cos(a1) * r1, y1, Mth.sin(a1) * r1, 0.07f, 0f, 1f, SealDraw.argb(0.95f * f, i % 2 == 0 ? pale : glow), FB);
        }
        VertexConsumer spark = c.buffers().getBuffer(AuraRender.additive(SPARK));
        for (int i = 0; i < 12; i++) {
            float ph = frac(age * 0.03f * (1f + 0.4f * (i % 3)) + i * 0.37f), a = i * 2.399f + age * 0.05f, r = 0.4f + 0.3f * Mth.sin(i * 1.9f) + 0.15f * ph;
            SealDraw.glow(pose, spark, Mth.cos(a) * r, 1.45f - ph * 2.2f, Mth.sin(a) * r, 0.07f + 0.05f * roll(c, i, 9), a * 3f, SealDraw.argb(Mth.sin(ph * Mth.PI) * f * flick, i % 3 == 0 ? pale : glow), FB);
        }
        VertexConsumer flash = c.buffers().getBuffer(AuraRender.additive(FLASH));
        for (int side = -1; side <= 1; side += 2) {                                               // a crackling flash in each palm and arcs from the hands to the ground
            ModelPart part = side < 0 ? c.model().rightArm : c.model().leftArm;
            pose.pushPose();
            part.translateAndRotate(pose);
            SealDraw.camera(pose);
            SealDraw.glow(pose, flash, 0f, 0.78f, 0f, 0.13f + 0.05f * roll(c, side + 5, 1), age * 0.3f, SealDraw.argb(0.85f * f, pale), FB);
            if (roll(c, side + 7, 2) > 0.3f) SealDraw.ribbon(pose, arc, 0f, 0.78f, 0f, side * 0.1f * roll(c, side + 3, 3), 1.25f + 0.3f * roll(c, side, 4), 0.12f * (roll(c, side + 9, 5) - 0.5f) * 2f, 0.06f, 0f, 1f,
                    SealDraw.argb(0.9f * f, glow), FB);
            pose.popPose();
        }
        float back = WaterAuraRig.forge(c, 12f);
        if (back > 0.01f) {                                                                       // the drum sigil spinning behind the back
            pose.pushPose();
            c.model().body.translateAndRotate(pose);
            VertexConsumer sg = c.buffers().getBuffer(AuraRender.additive(SIGIL));
            float r = 0.55f * (0.6f + 0.4f * back), a = age * 0.05f, ux = Mth.cos(a) * r, uy = Mth.sin(a) * r;
            int col = SealDraw.argb(0.5f * back * flick, glow);
            SealDraw.plane(pose, sg, 0f, 0.31f, 0.80f, ux, uy, 0f, -uy, ux, 0f, false, col, FB);
            SealDraw.plane(pose, sg, 0f, 0.31f, 0.80f, ux, uy, 0f, -uy, ux, 0f, true, col, FB);
            pose.popPose();
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
