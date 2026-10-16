package com.newuniverse.nusmp.client.shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the four Devil Unite modes of Nacht Faust (Equus, Canis, Felis, Gallus) wear a temporary armour that must follow every animation of the
 * player and use no armour slot. The owner wants Blockbench style .geo.json models, so each mode is a few GeoLite models (tools/gen_devil_assets.py)
 * authored on the standing player and attached to the posed model part they decorate (head, body, arms, legs), exactly as the corundum armour does.
 * Models are cached by GeoModels (loaded once per resource reload, nothing allocated per frame here); the specs are built once in static fields.
 * Each model draws cutout with its glow map additive on top; while the mode grows in or fades the piece is translucent and scaled about the part pivot.
 */
public final class DevilOverlay {
    private DevilOverlay() {}

    private static GeoSpec spec(String id) {
        return new GeoSpec(rl("geo/devils/devil_" + id + ".geo.json"), rl("animations/devils/devil_" + id + ".animation.json"),
                rl("textures/entity/devils/devil_" + id + ".png"), rl("textures/entity/devils/devil_" + id + "_glow.png"));
    }

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath("nusmp", p); }

    private static final GeoSpec EQ_HEAD = spec("equus_head"), EQ_BODY = spec("equus_body"), EQ_ARM_R = spec("equus_arm_r"), EQ_ARM_L = spec("equus_arm_l"),
            EQ_LEG_R = spec("equus_leg_r"), EQ_LEG_L = spec("equus_leg_l");
    private static final GeoSpec CA_HEAD = spec("canis_head"), CA_BODY = spec("canis_body"), CA_ARM_R = spec("canis_arm_r"), CA_ARM_L = spec("canis_arm_l");
    private static final GeoSpec FE_HEAD = spec("felis_head"), FE_BODY = spec("felis_body"), FE_ARM_R = spec("felis_arm_r"), FE_ARM_L = spec("felis_arm_l"),
            FE_LEG_R = spec("felis_leg_r"), FE_LEG_L = spec("felis_leg_l");
    private static final GeoSpec GA_HEAD = spec("gallus_head"), GA_BODY = spec("gallus_body"), GA_ARM_R = spec("gallus_arm_r"), GA_ARM_L = spec("gallus_arm_l");

    /** Draws the overlay of {@code mode}; {@code k} is grow-in times fade-out (0..1), {@code now} the animation clock in ticks. */
    public static void draw(ModeArmor.Mode mode, PoseStack pose, MultiBufferSource buf, int light, PlayerModel<AbstractClientPlayer> m, float now, float k, float swing) {
        float pulse = 0.75f + 0.25f * Mth.sin(now * 0.2f);
        int argb = ((int) (255 * Mth.clamp(k, 0f, 1f)) << 24) | 0xFFFFFF, glow = ((int) (255 * Mth.clamp(k * pulse, 0f, 1f)) << 24) | 0xFFFFFF;
        GeoDraw.Layer layer = k >= 0.98f ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT;
        float s = 0.6f + 0.4f * Mth.clamp(k, 0f, 1f);
        float t = now / 20f;
        switch (mode) {
            case DEVIL_EQUUS -> {
                part(pose, buf, m.head, EQ_HEAD, t, layer, light, argb, glow, s);
                part(pose, buf, m.body, EQ_BODY, t, layer, light, argb, glow, s);
                part(pose, buf, m.rightArm, EQ_ARM_R, t, layer, light, argb, glow, s);
                part(pose, buf, m.leftArm, EQ_ARM_L, t, layer, light, argb, glow, s);
                part(pose, buf, m.rightLeg, EQ_LEG_R, t, layer, light, argb, glow, s);
                part(pose, buf, m.leftLeg, EQ_LEG_L, t, layer, light, argb, glow, s);
            }
            case DEVIL_CANIS -> {
                part(pose, buf, m.head, CA_HEAD, t, layer, light, argb, glow, s);
                part(pose, buf, m.body, CA_BODY, t, layer, light, argb, glow, s);
                part(pose, buf, m.rightArm, CA_ARM_R, t, layer, light, argb, glow, s);
                part(pose, buf, m.leftArm, CA_ARM_L, t, layer, light, argb, glow, s);
            }
            case DEVIL_FELIS -> {
                part(pose, buf, m.head, FE_HEAD, t, layer, light, argb, glow, s);
                part(pose, buf, m.body, FE_BODY, t, layer, light, argb, glow, s);
                part(pose, buf, m.rightArm, FE_ARM_R, t, layer, light, argb, glow, s);
                part(pose, buf, m.leftArm, FE_ARM_L, t, layer, light, argb, glow, s);
                part(pose, buf, m.rightLeg, FE_LEG_R, t, layer, light, argb, glow, s);
                part(pose, buf, m.leftLeg, FE_LEG_L, t, layer, light, argb, glow, s);
            }
            case DEVIL_GALLUS -> {
                part(pose, buf, m.head, GA_HEAD, t, layer, light, argb, glow, s);
                part(pose, buf, m.body, GA_BODY, t, layer, light, argb, glow, s);
                part(pose, buf, m.rightArm, GA_ARM_R, t, layer, light, argb, glow, s);
                part(pose, buf, m.leftArm, GA_ARM_L, t, layer, light, argb, glow, s);
            }
            default -> { }
        }
    }

    /** A model authored on the standing player, turned with the posed part about the part's own pivot (the file's absolute coordinates then apply). */
    private static void part(PoseStack pose, MultiBufferSource buf, ModelPart p, GeoSpec spec, float seconds, GeoDraw.Layer layer, int light, int argb, int glow, float scale) {
        pose.pushPose();
        p.translateAndRotate(pose);
        pose.scale(scale, scale, scale);
        pose.translate(-p.x / 16f, -p.y / 16f, -p.z / 16f);
        GeoDraw.paint(pose, buf, spec, "idle", seconds, GeoDraw.Space.PLAYER, layer, light, argb, glow);
        pose.popPose();
    }
}
