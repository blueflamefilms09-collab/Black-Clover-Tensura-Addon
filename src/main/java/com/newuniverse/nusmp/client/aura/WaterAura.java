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
 * 1.93.0 Water Magic (Noelle Silva: the Valkyrie Dress / Sea Dragon armour): the temporary armour of Aura.WATER. A set of crystal-blue scale plates trimmed with pearl, all
 * forged in and shattered out by {@link EquipAnimation} like the Djinn full equip ({@link WaterAuraRig}): a circlet with a fin crest and swept temple fins, a scale cuirass
 * with the shell on the chest, a flaring crystal skirt, pauldrons with dragon fins, finned bracers and boots, webbed gauntlets, wings of water feathers, the sea dragon's tail
 * ending in a fish fin, a halo of droplets and the pearl ring with the trident behind the back. Models: tools/gen_water_aura_geo.py (geo + texture + glow map per piece).
 * <pre>
 *   pass                          render type          scale / order            why
 *   1 water skin shell            translucent          1.04 x body, first       a glassy film of water over the body (the head is left alone: the face stays visible)
 *   2 plates, wings, tail ...     translucent (glass)  per piece geo            every water piece is crystal: always alpha blended, lit up so it reads in the dark
 *   3 glow maps                   additive, full bright per piece geo           pearl seams and crystal edges
 *   4 haze                        additive, full bright 1.08 x body             last body pass: the sheen of running water
 *   5 ripples, stream, droplets   additive / translucent feet, round the body   ripple rings on the ground, two streams winding up the body, drops falling off the hands
 * </pre>
 * Styles: 0 = the usual cast; 1 = the Valkyrie Dress (the plates take a deeper blue tint, the glow is a notch brighter and pearl-white).
 */
public final class WaterAura {
    private WaterAura() {}

    private static final ResourceLocation SHELL = AuraRender.tex("aura/water_shell"), HAZE = AuraRender.tex("aura/water_haze");
    private static final ResourceLocation RIPPLE = AuraRender.tex("particle/waterfx_ripple"), STREAM = AuraRender.tex("particle/waterfx_stream"),
            DROP = AuraRender.tex("particle/waterfx_drop"), SIGIL = AuraRender.tex("particle/waterfx_sigil");
    private static final int FB = PropDraw.FULL_BRIGHT, GLOW = 0x5CE6FF, PALE = 0xCFF8FF;

    private static final WaterAuraRig.Set SET = new WaterAuraRig.Set("water",
            Piece.of("chest", Anchor.BODY, 1, 0, 0, -14).glass(),
            Piece.of("skirt", Anchor.BODY, 5, 0, 10, -6).glass().clip("idle", 1f).detail(),
            Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0).glass(),
            Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0).glass(),
            Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6).glass().glow(0.5f),
            Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6).glass().glow(0.5f),
            Piece.of("gauntlet_r", Anchor.ARM_R, 5, -10, 8, -6).glass(),
            Piece.of("gauntlet_l", Anchor.ARM_L, 5, 10, 8, -6).glass(),
            Piece.of("greave_r", Anchor.LEG_R, 6, -3, 14, -6).glass().glow(0.5f),
            Piece.of("greave_l", Anchor.LEG_L, 7, 3, 14, -6).glass().glow(0.5f),
            Piece.of("helm", Anchor.HEAD, 0, 0, -16, 0).glass().head(),
            Piece.of("wing_r", Anchor.BODY, 8, -10, -4, 12).clip("idle", 1f).flap().glass(),
            Piece.of("wing_l", Anchor.BODY, 8, 10, -4, 12).clip("idle", 1f).flap().glass(),
            Piece.of("tail", Anchor.BODY, 9, 0, 6, 14).clip("idle", 1f).glass().detail(),
            Piece.of("halo", Anchor.HEAD, 10, 0, -16, 0).clip("idle", 1f).glass().detail().head(),
            Piece.of("backdrop", Anchor.BODY, 12, 0, 0, 12).clip("idle", 1f).glass().detail());

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.WATER, WaterAura::paint);
    }

    private static void paint(AuraContext c) {
        if (NoelleFormsAura.paint(c)) return;      // 1.99.0: Noelle's forms (styles 1 to 4 remade, docs/magic/noelle_forms.md); style 0 and the classic switch fall through to the first look below
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.17f) + 0.08f * Mth.sin(age * 0.53f);
        float forge = WaterAuraRig.forge(c, 0f);
        WaterAuraRig.shell(c, AuraRender.translucent(SHELL), 1.04f, 1.025f, 1.04f, AuraRender.alpha(0xFFFFFFFF, 0.55f * forge), false, true);
        boolean valk = c.style() == 1;
        WaterAuraRig.draw(c, SET, valk ? PALE : GLOW, 0.75f * pulse + (valk ? 0.5f : 0.3f), valk ? 0xA8C8FF : 0xFFFFFF);
        WaterAuraRig.shell(c, AuraRender.additive(HAZE), 1.08f + 0.012f * Mth.sin(age * 0.13f), 1.05f, 1.08f + 0.012f * Mth.sin(age * 0.13f), AuraRender.alpha(0xFF7CD8FF, 0.16f * pulse * forge), true, true);
        extras(c, forge, pulse);
    }

    private static void extras(AuraContext c, float f, float pulse) {
        if (f <= 0.01f) return;
        float age = c.age();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float grow = 0.55f + 0.45f * f;
        pose.pushPose();                                                                          // ripples on the ground, three rings running out in turn
        pose.translate(0f, 1.478f, 0f);
        VertexConsumer ripple = c.buffers().getBuffer(AuraRender.additive(RIPPLE));
        for (int k = 0; k < 3; k++) {
            float ph = frac(age * 0.012f + k / 3f);
            SealDraw.flat(pose, ripple, (0.45f + 1.0f * ph) * grow, age * 0.4f, SealDraw.argb((1f - ph) * 0.85f * f, k == 1 ? PALE : GLOW), FB);
        }
        VertexConsumer sigil = c.buffers().getBuffer(AuraRender.additive(SIGIL));
        SealDraw.flat(pose, sigil, 0.95f * grow, -age * 0.9f, SealDraw.argb(0.55f * f * pulse, GLOW), FB);
        pose.popPose();
        if (WaterAuraRig.far(c)) return;
        VertexConsumer stream = c.buffers().getBuffer(AuraRender.additive(STREAM));
        for (int s = 0; s < 2; s++) {                                                             // two streams winding up the body
            float px = 0f, py = 0f, pz = 0f;
            for (int i = 0; i <= 9; i++) {
                float t = i / 9f, phi = age * 0.09f + t * Mth.TWO_PI * 1.4f + s * Mth.PI, r = 0.52f + 0.1f * Mth.sin(t * Mth.PI);
                float x = Mth.cos(phi) * r, y = 1.45f - t * 2.0f, z = Mth.sin(phi) * r;
                if (i > 0) SealDraw.ribbon(pose, stream, px, py, pz, x, y, z, 0.075f, (i - 1) / 9f, i / 9f, SealDraw.argb((0.35f + 0.6f * Mth.sin(t * Mth.PI)) * f, i % 2 == 0 ? PALE : GLOW), FB);
                px = x; py = y; pz = z;
            }
        }
        VertexConsumer drop = c.buffers().getBuffer(AuraRender.additive(DROP));
        for (int i = 0; i < 12; i++) {                                                            // drops lifting off and falling again round the body
            float ph = frac(age * 0.02f * (1f + 0.3f * (i % 3)) + i * 0.41f), a = i * 2.399f + age * 0.02f, r = 0.5f + 0.2f * Mth.sin(i * 1.7f);
            float y = 1.5f - 2.3f * Mth.sin(ph * Mth.PI) * 0.8f - 0.1f;
            SealDraw.glow(pose, drop, Mth.cos(a) * r, y + 1.0f * (ph > 0.5f ? ph - 0.5f : 0f), Mth.sin(a) * r, 0.05f + 0.03f * Mth.sin(i), 0f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.9f * f, PALE), FB);
        }
        // the sea-dragon sigil standing behind the back, a slow turn
        float back = WaterAuraRig.forge(c, 12f);
        if (back > 0.01f) {
            pose.pushPose();
            c.model().body.translateAndRotate(pose);
            VertexConsumer sg = c.buffers().getBuffer(AuraRender.additive(SIGIL));
            float r = 0.6f * (0.6f + 0.4f * back), a = age * 0.012f, ux = Mth.cos(a) * r, uy = Mth.sin(a) * r;
            int col = SealDraw.argb(0.55f * back, GLOW);
            SealDraw.plane(pose, sg, 0f, 0.31f, 0.60f, ux, uy, 0f, -uy, ux, 0f, false, col, FB);
            SealDraw.plane(pose, sg, 0f, 0.31f, 0.60f, ux, uy, 0f, -uy, ux, 0f, true, col, FB);
            pose.popPose();
        }
        // drops falling from the hands
        float arm = WaterAuraRig.forge(c, 5f);
        if (arm > 0.01f) for (int side = -1; side <= 1; side += 2) {
            ModelPart part = side < 0 ? c.model().rightArm : c.model().leftArm;
            pose.pushPose();
            part.translateAndRotate(pose);
            SealDraw.camera(pose);
            for (int k = 0; k < 3; k++) {
                float ph = frac(age * 0.035f + k * 0.34f + (side > 0 ? 0.17f : 0f));
                SealDraw.glow(pose, drop, side * 0.02f * k, 0.78f + ph * 0.9f, 0.01f * k, 0.05f * (1f - 0.5f * ph), 0f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.9f * arm, PALE), FB);
            }
            pose.popPose();
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
