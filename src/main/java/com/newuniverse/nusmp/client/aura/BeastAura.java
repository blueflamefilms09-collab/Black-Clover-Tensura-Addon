package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.BeastDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 0.59 Beast Magic: the player render layer (Aura.BEAST), the spectral beast form. Layer space: y points down, the neck line is y = 0 and the feet y = 1.5.
 * Style 0, Beast Form: the player grows a beast. Passes, in draw order:
 * <ol>
 *   <li>the fur skin (textures/aura/beast_skin, a 64x64 skin layout of fur tufts with holes) over the posed model at 1.04: CUTOUT, TRANSLUCENT while it fades in or out;</li>
 *   <li>the fur collar, back ridge, hip fur and the long flame tail (geo beast_form_body, on the body part): CUTOUT + its glow map ADDITIVE;</li>
 *   <li>the ears, brow, cheek ruffs, fangs and flame crest (geo beast_form_head, on the head part so it turns with the head): CUTOUT + glow, not drawn for
 *       the local player in first person;</li>
 *   <li>a claw hand with a fur cuff on each arm (geo beast_form_claw, on the arm part): CUTOUT + glow;</li>
 *   <li>the ember skin shell (beast_skin_glow at 1.07, the stripes glowing like embers) and flame wisps round the body and hands (beast_flame): ADDITIVE.</li>
 * </ol>
 * Style 1, the great beast: the same fur, tail and claws without the ears, and a ghost beast head and shoulders (geo beast_form_ghost: TRANSLUCENT body,
 * ADDITIVE glow) hovering over and behind the player, plus a halo of wisps; the ghost is not drawn for the local player in first person.
 * Draw order is opaque, then translucent, then additive, and no two coplanar passes share a render type (the passes differ in scale or geometry).
 */
public final class BeastAura {
    private BeastAura() {}

    private static final GeoSpec HEAD = GeoSpec.of("beast", "form_head"), BODY = GeoSpec.of("beast", "form_body"), CLAW = GeoSpec.of("beast", "form_claw"),
            GHOST = GeoSpec.of("beast", "form_ghost");
    private static final int FB = PropDraw.FULL_BRIGHT;
    private static ResourceLocation skin, skinGlow, flame, licks, haze;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        skin = AuraRender.tex("aura/beast_skin");
        skinGlow = AuraRender.tex("aura/beast_skin_glow");
        flame = AuraRender.tex("particle/beast_flame");
        licks = AuraRender.tex("particle/beast_licks");
        haze = AuraRender.tex("particle/beast_haze");
        PlayerAuraClient.register(Aura.BEAST, BeastAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        boolean self1p = c.player() == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        boolean great = c.style() == 1;
        PoseStack pose = c.pose();
        float age = c.age(), pulse = 0.5f + 0.5f * Mth.sin(age * 0.2f);
        int white = AuraRender.alpha(0xFFFFFFFF, f);
        int glowTint = AuraRender.alpha(0xFFFFC890, (0.45f + 0.15f * pulse) * f);
        GeoDraw.Layer solid = f < 0.999f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT;

        // 1. the fur skin, 4 % bigger than the body (cutout; translucent while fading)
        AuraRender.model(c, f < 0.999f ? AuraRender.translucent(skin) : AuraRender.cutout(skin), 1.04f, 1.04f, 1.04f, white);
        // 2. the body pieces: collar, back ridge, hips, tail
        pose.pushPose();
        c.model().body.translateAndRotate(pose);
        GeoDraw.paint(pose, c.buffers(), BODY, "idle", age / 20f, GeoDraw.Space.PLAYER, solid, c.light(), white, glowTint);
        pose.popPose();
        // 3. the head pieces (ears, brow, ruffs, crest) turn with the head; none for the local player in first person, none for the great beast
        if (!self1p && !great) {
            pose.pushPose();
            c.model().head.translateAndRotate(pose);
            GeoDraw.paint(pose, c.buffers(), HEAD, "idle", age / 20f, GeoDraw.Space.PLAYER, solid, c.light(), white, glowTint);
            pose.popPose();
        }
        // 4. the claw hands follow the posed arms
        claw(c, c.model().rightArm, -0.75f / 16f, white, glowTint, solid);
        claw(c, c.model().leftArm, 0.75f / 16f, white, glowTint, solid);
        // 5. the great beast: a ghost head and shoulders hover over and behind the player (translucent body, additive glow)
        if (great && !self1p) {
            pose.pushPose();
            pose.translate(0f, 1.5f - 9f / 16f - 0.04f * Mth.sin(age * 0.05f), 6f / 16f);        // lifted over the head and set back, 80 % size about the feet
            pose.scale(0.8f, 0.8f, 0.8f);
            pose.translate(0f, -1.5f, 0f);
            GeoDraw.paint(pose, c.buffers(), GHOST, "idle", age / 20f, GeoDraw.Space.PLAYER, GeoDraw.Layer.TRANSLUCENT, FB,
                    AuraRender.alpha(0xFFFFFFFF, 0.8f * f), AuraRender.alpha(0xFFFFD8A0, (0.55f + 0.2f * pulse) * f));
            pose.popPose();
        }
        // 6. additive: the ember shell and the wisps
        AuraRender.modelBright(c, AuraRender.additive(skinGlow), 1.07f, 1.07f, 1.07f, AuraRender.alpha(0xFFFFFFFF, (0.55f + 0.25f * pulse) * f));
        wisps(c, f, great);
    }

    private static void claw(AuraContext c, ModelPart arm, float dx, int white, int glowTint, GeoDraw.Layer solid) {
        PoseStack pose = c.pose();
        pose.pushPose();
        arm.translateAndRotate(pose);
        pose.translate(dx, -1.5f, 0f);                         // the model's file frame has the shoulder at the origin; GeoDraw adds the 24 px of the feet
        GeoDraw.paint(pose, c.buffers(), CLAW, "idle", c.age() / 20f, GeoDraw.Space.PLAYER, solid, c.light(), white, glowTint);
        pose.popPose();
    }

    /** Flame tongues rising round the body (and licks off the hands), camera facing, additive. */
    private static void wisps(AuraContext c, float f, boolean great) {
        PoseStack pose = c.pose();
        float age = c.age();
        BeastDraw.camera(pose);
        VertexConsumer fv = c.buffers().getBuffer(AuraRender.additive(flame));
        int n = great ? 9 : 7;
        for (int i = 0; i < n; i++) {
            float ph = (age * 0.012f + i / (float) n) % 1f;                      // 0..1 along its rise
            float a = i * 2.4f + age * 0.02f;
            float r = 0.42f + 0.1f * Mth.sin(a * 1.7f);
            float y = 1.35f - ph * 1.7f;                                         // from the feet up (y points down)
            float h = 0.5f + 0.25f * Mth.sin(age * 0.3f + i);
            float al = Mth.sin(ph * Mth.PI) * 0.75f * f;
            BeastDraw.tongue(pose, fv, Mth.cos(a) * r, y, Mth.sin(a) * r * 0.8f, 0.17f, h, -1f, AuraRender.alpha(0xFFFFFFFF, Mth.clamp(al, 0f, 1f)), FB);
        }
        // licks of flame from the hands
        VertexConsumer lv = c.buffers().getBuffer(AuraRender.additive(licks));
        wristLicks(c, c.model().rightArm, lv, f);
        wristLicks(c, c.model().leftArm, lv, f);
        if (great) {
            BeastDraw.camera(pose);
            VertexConsumer hv = c.buffers().getBuffer(AuraRender.additive(haze));
            BeastDraw.glow(pose, hv, 0f, -0.55f, 0.1f, 1.1f, age * 0.01f, AuraRender.alpha(0xFFFF9030, 0.16f * f), FB);
        }
    }

    private static void wristLicks(AuraContext c, ModelPart arm, VertexConsumer lv, float f) {
        PoseStack pose = c.pose();
        pose.pushPose();
        arm.translateAndRotate(pose);
        BeastDraw.camera(pose);
        for (int i = 0; i < 3; i++) {
            float ph = (c.age() * 0.05f + i * 0.33f) % 1f;
            float y = 0.55f + ph * 0.35f;
            float al = Mth.sin(ph * Mth.PI) * 0.8f * f;
            float cu = (i % 4) * 0.25f;
            BeastDraw.cell(pose, lv, (i - 1) * 0.05f, y, 0f, 0.13f, cu, AuraRender.alpha(0xFFFFFFFF, Mth.clamp(al, 0f, 1f)), FB);
        }
        pose.popPose();
    }
}
