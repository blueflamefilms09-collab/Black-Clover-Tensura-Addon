package com.newuniverse.nusmp.client.relics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Look;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the relics worn in Curios slots on a player: a pendant and chain at the neck, a cloak or cape on the back, a belt with a buckle, rings on the
 * forearms, a charm at the hip. Small boxes tinted from the def's palette (main, trim, glow), built from the player model's own parts so they follow the
 * body; the glow parts are drawn full bright. Reads the player's relics through the 10-tick cache of {@link Relics}, so it costs a few boxes a frame.
 * Without Curios nothing is worn in these slots and nothing is drawn.
 */
public class RelicWornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int FULL = LightTexture.FULL_BRIGHT;

    public RelicWornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        if (p.isSpectator() || p.isInvisible()) return;
        var worn = Relics.wornList(p);
        if (worn.isEmpty()) return;
        VertexConsumer vc = null;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        for (Relics.Worn w : worn) {
            RelicDef d = w.def();
            RelicSlot slot = d.slot();
            if (!slot.isCurio()) continue;
            if (vc == null) vc = buffers.getBuffer(RenderType.entityCutoutNoCull(RelicTextures.WHITE));
            int main = d.paletteMain(), trim = d.paletteTrim(), glow = d.paletteGlow();
            boolean shine = d.grade().atLeast(Grade.GOD);
            if (slot == RelicSlot.NECK) neck(pose, vc, light, model.body, d.look(), main, trim, glow);
            else if (slot == RelicSlot.BACK) back(pose, vc, light, model.body, d.look(), main, trim, glow, p.isCrouching());
            else if (slot == RelicSlot.BELT) belt(pose, vc, light, model.body, main, trim, glow);
            else if (slot == RelicSlot.HANDS) { hands(pose, vc, light, model.rightArm, true, main, trim, glow); hands(pose, vc, light, model.leftArm, false, main, trim, glow); }
            else if (slot == RelicSlot.CHARM) charm(pose, vc, light, model.body, main, trim, glow, shine);
        }
    }

    // ---------------------------------------------------------------- the worn pieces (model units: 16 to a block, y down, front is -z)
    private static void neck(PoseStack pose, VertexConsumer vc, int light, ModelPart body, Look look, int main, int trim, int glow) {
        pose.pushPose();
        body.translateAndRotate(pose);
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < 4; i++) {                                                  // the chain: two short runs from the collar to the pendant
            float y = -0.2f + i * 0.7f, x = 2.3f - i * 0.5f;
            Boxes.box(p, vc, light, -x - 0.3f, y, -2.35f, -x + 0.3f, y + 0.6f, -2.0f, trim);
            Boxes.box(p, vc, light, x - 0.3f, y, -2.35f, x + 0.3f, y + 0.6f, -2.0f, trim);
        }
        float size = look == Look.LOCKET ? 3.0f : look == Look.PENDANT ? 2.2f : 2.8f;
        float h = size / 2f;
        Boxes.box(p, vc, light, -h, 2.4f, -2.75f, h, 2.4f + size, -2.2f, trim);        // the setting
        Boxes.box(p, vc, light, -h + 0.5f, 2.9f, -2.95f, h - 0.5f, 2.4f + size - 0.5f, -2.7f, main);
        Boxes.box(p, vc, FULL, -0.5f, 2.4f + size / 2f - 0.5f, -3.05f, 0.5f, 2.4f + size / 2f + 0.5f, -2.9f, glow);   // the stone, lit from inside
        pose.popPose();
    }

    private static void back(PoseStack pose, VertexConsumer vc, int light, ModelPart body, Look look, int main, int trim, int glow, boolean crouching) {
        pose.pushPose();
        body.translateAndRotate(pose);
        PoseStack.Pose p = pose.last();
        boolean cape = look == Look.CAPE, mantle = look == Look.MANTLE;
        float len = cape ? 13f : mantle ? 5f : 18f;                                    // the hem, in pixels down from the shoulders
        float lift = crouching ? 0.6f : 0f;
        Boxes.box(p, vc, light, -4.3f, -0.4f, 2.05f, 4.3f, len, 2.7f + lift, main);     // the drop
        Boxes.box(p, vc, light, -4.3f, len - 1.0f, 2.05f, 4.3f, len, 2.8f + lift, trim);   // the hem
        Boxes.box(p, vc, light, -4.6f, -0.8f, -2.4f, 4.6f, 1.0f, 2.75f, main);          // the shoulders and the collar
        Boxes.box(p, vc, light, -4.65f, 0.6f, -2.45f, 4.65f, 1.1f, 2.8f, trim);
        Boxes.box(p, vc, FULL, -0.6f, 0.0f, -2.6f, 0.6f, 1.2f, -2.4f, glow);            // the clasp
        if (mantle) {                                                                  // a mantle hangs over the arms a little
            Boxes.box(p, vc, light, -5.2f, -0.6f, -2.3f, -3.9f, 3.6f, 2.5f, main);
            Boxes.box(p, vc, light, 3.9f, -0.6f, -2.3f, 5.2f, 3.6f, 2.5f, main);
            Boxes.box(p, vc, light, -5.25f, 2.9f, -2.35f, -3.85f, 3.6f, 2.55f, trim);
            Boxes.box(p, vc, light, 3.85f, 2.9f, -2.35f, 5.25f, 3.6f, 2.55f, trim);
        }
        pose.popPose();
    }

    private static void belt(PoseStack pose, VertexConsumer vc, int light, ModelPart body, int main, int trim, int glow) {
        pose.pushPose();
        body.translateAndRotate(pose);
        PoseStack.Pose p = pose.last();
        Boxes.box(p, vc, light, -4.3f, 9.2f, -2.3f, 4.3f, 11.2f, 2.3f, main);
        Boxes.box(p, vc, light, -4.35f, 9.1f, -2.35f, 4.35f, 9.5f, 2.35f, trim);
        Boxes.box(p, vc, light, -1.4f, 8.9f, -2.6f, 1.4f, 11.5f, -2.3f, trim);          // the buckle
        Boxes.box(p, vc, FULL, -0.6f, 9.7f, -2.75f, 0.6f, 10.7f, -2.55f, glow);
        for (int i = -1; i <= 1; i += 2) Boxes.box(p, vc, light, i * 3.0f - 0.7f, 11.2f, -2.45f, i * 3.0f + 0.7f, 13.0f, -2.1f, main);   // two straps hanging at the hips
        pose.popPose();
    }

    private static void hands(PoseStack pose, VertexConsumer vc, int light, ModelPart arm, boolean right, int main, int trim, int glow) {
        pose.pushPose();
        arm.translateAndRotate(pose);
        PoseStack.Pose p = pose.last();
        float x0 = right ? -3.45f : -1.45f, x1 = right ? 1.45f : 3.45f;
        Boxes.box(p, vc, light, x0, 6.6f, -2.45f, x1, 10.4f, 2.45f, main);             // the gauntlet's cuff
        Boxes.box(p, vc, light, x0 - 0.05f, 6.5f, -2.5f, x1 + 0.05f, 7.1f, 2.5f, trim);
        Boxes.box(p, vc, light, x0 - 0.05f, 10.0f, -2.5f, x1 + 0.05f, 10.5f, 2.5f, trim);
        float mid = (x0 + x1) / 2f;
        Boxes.box(p, vc, FULL, mid - 0.5f, 8.0f, -2.6f, mid + 0.5f, 9.0f, -2.45f, glow);
        pose.popPose();
    }

    private static void charm(PoseStack pose, VertexConsumer vc, int light, ModelPart body, int main, int trim, int glow, boolean shine) {
        pose.pushPose();
        body.translateAndRotate(pose);
        PoseStack.Pose p = pose.last();
        Boxes.box(p, vc, light, 4.0f, 8.8f, -0.4f, 4.4f, 10.6f, 0.4f, trim);            // the cord at the left hip
        Boxes.box(p, vc, light, 4.0f, 10.4f, -1.2f, 4.9f, 12.6f, 1.2f, main);           // the charm itself
        Boxes.box(p, vc, shine ? FULL : light, 4.8f, 10.9f, -0.6f, 5.0f, 12.0f, 0.6f, glow);
        pose.popPose();
    }

    /** Axis-aligned boxes written straight into a vertex consumer, tinted by a colour; the white texture is sampled at its middle. */
    static final class Boxes {
        private Boxes() {}

        static void box(PoseStack.Pose pose, VertexConsumer vc, int light, float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
            float s = 1f / 16f;
            x0 *= s; y0 *= s; z0 *= s; x1 *= s; y1 *= s; z1 *= s;
            quad(pose, vc, light, rgb, 1.0f, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);   // top (y is down in model space, so the smaller y is up)
            quad(pose, vc, light, rgb, 0.62f, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);   // bottom
            quad(pose, vc, light, rgb, 0.86f, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);   // front
            quad(pose, vc, light, rgb, 0.80f, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);    // back
            quad(pose, vc, light, rgb, 0.74f, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);   // one side
            quad(pose, vc, light, rgb, 0.74f, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);    // the other side
        }

        private static void quad(PoseStack.Pose pose, VertexConsumer vc, int light, int rgb, float shade, float nx, float ny, float nz,
                                 float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
            int r = (int) ((rgb >> 16 & 255) * shade), g = (int) ((rgb >> 8 & 255) * shade), b = (int) ((rgb & 255) * shade);
            float[] v = {ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz};
            float[] uv = {0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f};
            for (int i = 0; i < 4; i++) {
                vc.addVertex(pose, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).setColor(r, g, b, 255).setUv(uv[i * 2], uv[i * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
            }
        }
    }
}
