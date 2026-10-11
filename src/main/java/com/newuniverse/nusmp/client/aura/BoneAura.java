package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Bone Armor: textured rib plates, a spine, arm guards and shin plates over the player's posed model. */
public final class BoneAura {
    private BoneAura() {}

    private static final ResourceLocation BONE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/bone_block_side.png");
    private static final ResourceLocation GLOW = AuraRender.tex("particle/bone_sigil");

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.BONE, BoneAura::paint);
    }

    private static void paint(AuraContext c) {
        float fade = c.fade();
        if (fade <= 0.002f) return;

        VertexConsumer bone = c.buffers().getBuffer(AuraRender.cutout(BONE));
        PoseStack pose = c.pose();
        int light = c.light();
        int color = AuraRender.alpha(0xFFF4EEDB, fade);

        pose.pushPose();
        c.model().body.translateAndRotate(pose);
        // Rib cage and sternum follow torso movement; the paired bars wrap around the front and back.
        for (int rib = 0; rib < 5; rib++) {
            float y = 0.17f + rib * 0.105f;
            float width = 0.245f - rib * 0.012f;
            for (int side : new int[]{-1, 1}) {
                float z = side * 0.145f;
                PropDraw.cylinder(pose, bone, -width, y, z, -0.018f, y + 0.025f, z * 1.12f,
                        0.026f, 0.019f, 7, false, color, light);
                PropDraw.cylinder(pose, bone, 0.018f, y + 0.025f, z * 1.12f, width, y, z,
                        0.026f, 0.019f, 7, false, color, light);
            }
        }
        PropDraw.cylinder(pose, bone, 0f, 0.12f, -0.19f, 0f, 0.72f, -0.19f,
                0.035f, 0.025f, 8, false, color, light);
        PropDraw.cylinder(pose, bone, 0f, 0.12f, 0.19f, 0f, 0.72f, 0.19f,
                0.035f, 0.025f, 8, false, color, light);
        pose.popPose();

        arm(pose, c.model().rightArm, bone, color, light, -0.045f);
        arm(pose, c.model().leftArm, bone, color, light, 0.045f);
        leg(pose, c.model().rightLeg, bone, color, light);
        leg(pose, c.model().leftLeg, bone, color, light);

        VertexConsumer glow = c.buffers().getBuffer(AuraRender.additive(GLOW));
        int glowColor = AuraRender.alpha(0xFFD8CCFF, (0.32f + 0.18f * Mth.sin(c.age() * 0.12f)) * fade);
        pose.pushPose();
        c.model().body.translateAndRotate(pose);
        for (int rib = 0; rib < 3; rib++) {
            float y = 0.22f + rib * 0.2f;
            PropDraw.torus(pose, glow, 0f, y, 0f, 0f, 1f, 0f, 0.19f, 0.008f, 18, 4,
                    glowColor, PropDraw.FULL_BRIGHT);
        }
        pose.popPose();
    }

    private static void arm(PoseStack pose, ModelPart arm, VertexConsumer bone, int color, int light, float offset) {
        pose.pushPose();
        arm.translateAndRotate(pose);
        for (int side : new int[]{-1, 1}) {
            float x = offset + side * 0.045f;
            PropDraw.cylinder(pose, bone, x, 0f, 0f, x * 0.85f, 0.64f, 0f,
                    0.035f, 0.022f, 7, false, color, light);
        }
        for (float y : new float[]{0.18f, 0.48f}) {
            PropDraw.torus(pose, bone, 0f, y, 0f, 0f, 1f, 0f, 0.057f, 0.018f, 12, 5, color, light);
        }
        pose.popPose();
    }

    private static void leg(PoseStack pose, ModelPart leg, VertexConsumer bone, int color, int light) {
        pose.pushPose();
        leg.translateAndRotate(pose);
        for (int side : new int[]{-1, 1}) {
            PropDraw.cylinder(pose, bone, side * 0.045f, 0.04f, 0f, side * 0.035f, 0.69f, 0f,
                    0.038f, 0.027f, 7, false, color, light);
        }
        PropDraw.torus(pose, bone, 0f, 0.43f, 0f, 0f, 1f, 0f, 0.07f, 0.022f, 12, 5, color, light);
        pose.popPose();
    }
}
