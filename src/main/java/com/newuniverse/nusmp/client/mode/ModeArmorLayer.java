package com.newuniverse.nusmp.client.mode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The transformation armour overlays (0.38), drawn on the player model while a mode is active ({@link ModeArmor}). Each piece
 * is bound to a model part (head, body, arms, legs) through {@link ModelPart#translateAndRotate}, so it follows every
 * animation. Coordinates are model pixels in part space: y points down, -z is the front, -x is the wearer's right.
 *
 * <p>Pieces (textures from tools/gen_mode_armor_textures.py, textures/entity/mode):
 * <ul>
 *   <li>Wind Spirit Dive (Yuno): fur-lined high-collared coat with coat tails and fur cuffs, a floating crown of star blades,
 *       two semi-transparent wind wings.</li>
 *   <li>Fire Spirit Dive (Salamander): asymmetric clawed gauntlets (the right one heavier, with a spiked pauldron), a dragon
 *       wing frame with flame membranes on the shoulders, flame hair strands.</li>
 *   <li>Anti-Magic Demon Mode (Asta): one jagged black horn, one tattered wing on the left shoulder, the black arm, floating
 *       dark pixel wisps.</li>
 *   <li>Lightning God Mode (Luck): angular runic chest and shoulder plating with glowing circuit lines, a crackling crown of
 *       bolts that re-strikes every two ticks.</li>
 *   <li>Valkyrie Dress (Noelle): crystalline water plating (chest, skirt plates, gauntlets, greaves), helmet wings, avian
 *       water wings and the spinning drill lance in the right hand.</li>
 * </ul>
 * The overlay grows in over 6 ticks when the mode starts and fades over the last 10.
 */
public class ModeArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int FULL = LightTexture.FULL_BRIGHT;

    public ModeArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/mode/" + name + ".png");
    }

    private static final ResourceLocation WIND_PLATE = tex("wind_plate"), WIND_GLOW = tex("wind_glow"), WIND_FUR = tex("wind_fur"),
            WIND_WING = tex("wind_wing"), WIND_CREST = tex("wind_crest");
    private static final ResourceLocation FIRE_PLATE = tex("fire_plate"), FIRE_GLOW = tex("fire_glow"), FIRE_WING = tex("fire_wing"),
            FIRE_CREST = tex("fire_crest");
    private static final ResourceLocation DEMON_PLATE = tex("demon_plate"), DEMON_GLOW = tex("demon_glow"), DEMON_WING = tex("demon_wing"),
            DEMON_CREST = tex("demon_crest"), DEMON_WISP = tex("demon_wisp");
    private static final ResourceLocation BOLT_PLATE = tex("lightning_plate"), BOLT_GLOW = tex("lightning_glow"), BOLT_CREST = tex("lightning_crest");
    private static final ResourceLocation VALK_PLATE = tex("valkyrie_plate"), VALK_GLOW = tex("valkyrie_glow"), VALK_WING = tex("valkyrie_wing"),
            VALK_CREST = tex("valkyrie_crest"), VALK_LANCE = tex("valkyrie_lance");

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partial, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || Minecraft.getInstance().level == null) return;
        long gt = Minecraft.getInstance().level.getGameTime();
        ModeArmor.Mode mode = ModeArmorClient.mode(player.getId(), gt);
        if (mode == ModeArmor.Mode.NONE) return;
        float now = gt + partial;
        float grow = Mth.clamp(ModeArmorClient.age(player.getId(), now) / 6f, 0.05f, 1f);
        float fade = Mth.clamp(ModeArmorClient.left(player.getId(), now) / 10f, 0f, 1f);
        float k = grow * fade;
        if (k <= 0.01f) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        Ctx c = new Ctx(pose, buffers, light, now, k, limbSwingAmount);
        switch (mode) {
            case WIND_SPIRIT_DIVE -> wind(c, m);
            case FIRE_SPIRIT_DIVE -> fire(c, m);
            case DEMON -> demon(c, m);
            case LIGHTNING_GOD -> lightning(c, m);
            case VALKYRIE -> valkyrie(c, m);
            default -> { }
        }
    }

    /** What every piece needs. {@code k} = grow-in x fade-out (0..1). */
    private record Ctx(PoseStack pose, MultiBufferSource buffers, int light, float now, float k, float swing) {
        VertexConsumer solid(ResourceLocation t) { return buffers.getBuffer(RenderType.entityCutoutNoCull(t)); }
        VertexConsumer clear(ResourceLocation t) { return buffers.getBuffer(RenderType.entityTranslucent(t)); }
        VertexConsumer glow(ResourceLocation t) { return buffers.getBuffer(RenderType.eyes(t)); }
        int alpha(float a) { return (int) (255 * Mth.clamp(a * k, 0f, 1f)); }
    }

    /** Push the pose into a model part, in pixels. */
    private static void enter(PoseStack pose, ModelPart part) {
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
    }

    // ================================================================ Wind Spirit Dive (Yuno)
    private static void wind(Ctx c, PlayerModel<AbstractClientPlayer> m) {
        PoseStack pose = c.pose;
        int a = c.alpha(1f);
        // the coat over the body, its tall fur collar and the tails
        enter(pose, m.body);
        box(c.solid(WIND_PLATE), pose, -4.45f, -0.2f, -2.45f, 4.45f, 12.3f, 2.45f, 255, a, c.light);
        box(c.glow(WIND_GLOW), pose, -4.5f, -0.25f, -2.5f, 4.5f, 12.35f, 2.5f, 255, c.alpha(0.8f), FULL);
        box(c.solid(WIND_FUR), pose, -4.9f, -2.2f, -3.0f, 4.9f, 0.9f, -2.3f, 255, a, c.light);      // collar front
        box(c.solid(WIND_FUR), pose, -4.9f, -3.6f, 2.3f, 4.9f, 0.9f, 3.0f, 255, a, c.light);        // collar back, standing tall
        box(c.solid(WIND_FUR), pose, -4.9f, -3.0f, -2.3f, -4.2f, 0.9f, 2.3f, 255, a, c.light);
        box(c.solid(WIND_FUR), pose, 4.2f, -3.0f, -2.3f, 4.9f, 0.9f, 2.3f, 255, a, c.light);
        float tail = 6f + 2.5f * c.swing;                                           // the tails flare as you run
        flap(c.solid(WIND_PLATE), pose, -4.45f, 11.5f, 2.45f, 4.45f, 11.5f + 8f, 2.45f + tail * 0.35f, a, c.light);
        flap(c.solid(WIND_PLATE), pose, -4.45f, 11.5f, -2.45f, -0.6f, 11.5f + 7f, -2.45f - tail * 0.2f, a, c.light);
        flap(c.solid(WIND_PLATE), pose, 0.6f, 11.5f, -2.45f, 4.45f, 11.5f + 7f, -2.45f - tail * 0.2f, a, c.light);
        // the wings: blade feathers of wind, semi-transparent
        float beat = Mth.sin(c.now * 0.18f) * 9f;
        wings(c, pose, c.clear(WIND_WING), 27f * c.k, 25f * c.k, 30f + beat, -4f + beat * 0.5f, c.alpha(0.75f), FULL, true);
        pose.popPose();
        // sleeves and fur cuffs
        for (ModelPart arm : new ModelPart[]{m.rightArm, m.leftArm}) {
            enter(pose, arm);
            float x0 = arm == m.rightArm ? -3.3f : -1.3f, x1 = arm == m.rightArm ? 1.3f : 3.3f;
            box(c.solid(WIND_PLATE), pose, x0, -2.3f, -2.3f, x1, 6.6f, 2.3f, 255, a, c.light);
            box(c.solid(WIND_FUR), pose, x0 - 0.5f, 6.4f, -2.8f, x1 + 0.5f, 8.6f, 2.8f, 255, a, c.light);
            pose.popPose();
        }
        // the crown: eight star blades floating over the head, slowly turning
        enter(pose, m.head);
        pose.translate(0, -9.2f - 0.5f * Mth.sin(c.now * 0.1f), 0);
        pose.mulPose(Axis.YP.rotationDegrees(c.now * 1.2f));
        for (int i = 0; i < 8; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 45f));
            pose.translate(0, 0, -4.6f);
            float h = (i % 2 == 0 ? 7f : 5f) * c.k;
            card(c.glow(WIND_CREST), pose, -1.6f, 0f, 1.6f, -h, 0, c.alpha(1f), FULL);
            pose.popPose();
        }
        pose.popPose();
    }

    // ================================================================ Fire Spirit Dive (Salamander)
    private static void fire(Ctx c, PlayerModel<AbstractClientPlayer> m) {
        PoseStack pose = c.pose;
        int a = c.alpha(1f);
        // right: the heavy gauntlet, a spiked pauldron and four claws
        enter(pose, m.rightArm);
        box(c.solid(FIRE_PLATE), pose, -3.8f, 4.2f, -2.8f, 1.8f, 10.7f, 2.8f, 255, a, c.light);
        box(c.glow(FIRE_GLOW), pose, -3.85f, 4.15f, -2.85f, 1.85f, 10.75f, 2.85f, 255, c.alpha(1f), FULL);
        box(c.solid(FIRE_PLATE), pose, -4.2f, -3.0f, -3.0f, 2.0f, 1.4f, 3.0f, 255, a, c.light);
        spike(c.solid(FIRE_PLATE), pose, -3.0f, -3.0f, 0f, 1.4f, -5.5f, -8.5f, 0f, a, c.light);
        spike(c.solid(FIRE_PLATE), pose, -0.6f, -3.0f, 0f, 1.1f, -2.2f, -7.4f, 0.8f, a, c.light);
        for (int i = 0; i < 4; i++) {
            float x = -3.0f + i * 1.45f;
            spike(c.solid(FIRE_PLATE), pose, x, 10.6f, -1.6f, 0.55f, x - 0.3f, 14.8f * c.k + 10.6f * (1 - c.k), -3.2f, a, c.light);
        }
        pose.popPose();
        // left: the lighter gauntlet with three claws
        enter(pose, m.leftArm);
        box(c.solid(FIRE_PLATE), pose, -1.5f, 5.6f, -2.5f, 3.5f, 10.5f, 2.5f, 255, a, c.light);
        box(c.glow(FIRE_GLOW), pose, -1.55f, 5.55f, -2.55f, 3.55f, 10.55f, 2.55f, 255, c.alpha(0.8f), FULL);
        box(c.solid(FIRE_PLATE), pose, -1.6f, -2.6f, -2.6f, 3.4f, 0.4f, 2.6f, 255, a, c.light);
        for (int i = 0; i < 3; i++) {
            float x = -0.8f + i * 1.6f;
            spike(c.solid(FIRE_PLATE), pose, x, 10.5f, -1.5f, 0.5f, x + 0.2f, 13.6f * c.k + 10.5f * (1 - c.k), -2.8f, a, c.light);
        }
        pose.popPose();
        // the dragon wing frame, flame membranes glowing
        enter(pose, m.body);
        float beat = Mth.sin(c.now * 0.22f) * 7f;
        wings(c, pose, c.clear(FIRE_WING), 18f * c.k, 18f * c.k, 38f + beat, 4f, c.alpha(0.95f), FULL, true);
        pose.popPose();
        // flame hair: strands licking up from the head, flickering
        enter(pose, m.head);
        for (int i = 0; i < 7; i++) {
            pose.pushPose();
            float ang = -75f + i * 25f;
            pose.translate(0, -7.5f, 0);
            pose.mulPose(Axis.YP.rotationDegrees(ang + 180f));
            pose.translate(0, 0, -3.6f);
            pose.mulPose(Axis.XP.rotationDegrees(25f));                 // lean out and back
            float h = (5.5f + 1.8f * Mth.sin(c.now * 0.6f + i * 1.7f)) * c.k;
            card(c.glow(FIRE_CREST), pose, -1.8f, 0.5f, 1.8f, -h, 0, c.alpha(1f), FULL);
            pose.popPose();
        }
        pose.popPose();
    }

    // ================================================================ Anti-Magic Demon Mode (Asta)
    private static void demon(Ctx c, PlayerModel<AbstractClientPlayer> m) {
        PoseStack pose = c.pose;
        int a = c.alpha(1f);
        // the horn: four jagged segments from the left of the forehead, curving out and up
        enter(pose, m.head);
        pose.translate(2.4f, -7.4f, -2.2f);
        pose.mulPose(Axis.ZP.rotationDegrees(18f));
        pose.mulPose(Axis.XP.rotationDegrees(-12f));
        float w = 1.6f;
        for (int i = 0; i < 4; i++) {
            float len = (3.2f - i * 0.3f) * c.k;
            box(c.solid(DEMON_PLATE), pose, -w, -len, -w, w, 0.2f, w, 255, a, c.light);
            pose.translate(0, -len, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(i % 2 == 0 ? 16f : 6f));
            pose.mulPose(Axis.XP.rotationDegrees(i % 2 == 0 ? -6f : 8f));
            w *= 0.74f;
        }
        spike(c.solid(DEMON_PLATE), pose, 0, 0.2f, 0, w, 0.5f, -3.4f * c.k, 0, a, c.light);
        pose.popPose();
        // the black arm (right) with red veins
        enter(pose, m.rightArm);
        box(c.solid(DEMON_PLATE), pose, -3.35f, -2.35f, -2.35f, 1.35f, 10.35f, 2.35f, 255, a, c.light);
        box(c.glow(DEMON_GLOW), pose, -3.4f, -2.4f, -2.4f, 1.4f, 10.4f, 2.4f, 255, c.alpha(1f), FULL);
        pose.popPose();
        // one tattered wing on the left shoulder
        enter(pose, m.body);
        float beat = Mth.sin(c.now * 0.12f) * 6f;
        pose.pushPose();
        pose.translate(2.2f, 1.5f, 2.2f);
        pose.mulPose(Axis.YP.rotationDegrees(-(32f + beat)));
        pose.mulPose(Axis.ZP.rotationDegrees(-6f));
        card(c.solid(DEMON_WING), pose, 0f, 0f, 24f * c.k, -23f * c.k, 0, a, c.light);
        pose.popPose();
        // dark pixel wisps rising and drifting round the body
        for (int i = 0; i < 14; i++) {
            float t = (c.now * 0.04f + i * 0.137f) % 1f;
            float ang = i * 2.39f + c.now * 0.03f;
            float r = 6.5f + 3f * Mth.sin(i * 1.7f);
            float x = Mth.cos(ang) * r, z = Mth.sin(ang) * r, y = 22f - t * 30f;
            float s = (i % 3 == 0 ? 1.4f : 0.9f) * c.k * Mth.sin(t * Mth.PI);
            if (s > 0.05f) box(c.solid(DEMON_WISP), pose, x - s, y - s, z - s, x + s, y + s, z + s, 255, a, c.light);
        }
        pose.popPose();
    }

    // ================================================================ Lightning God Mode (Luck)
    private static void lightning(Ctx c, PlayerModel<AbstractClientPlayer> m) {
        PoseStack pose = c.pose;
        int a = c.alpha(1f);
        float pulse = 0.65f + 0.35f * Mth.sin(c.now * 0.9f);
        // angular runic chest plate
        enter(pose, m.body);
        box(c.solid(BOLT_PLATE), pose, -4.6f, -0.5f, -2.7f, 4.6f, 7.6f, 2.7f, 255, a, c.light);
        box(c.glow(BOLT_GLOW), pose, -4.65f, -0.55f, -2.75f, 4.65f, 7.65f, 2.75f, 255, c.alpha(pulse), FULL);
        pose.popPose();
        // shoulder plates with swept spikes
        for (ModelPart arm : new ModelPart[]{m.rightArm, m.leftArm}) {
            boolean right = arm == m.rightArm;
            float x0 = right ? -4.0f : -2.0f, x1 = right ? 2.0f : 4.0f, out = right ? -1 : 1;
            enter(pose, arm);
            box(c.solid(BOLT_PLATE), pose, x0, -2.9f, -2.9f, x1, 1.8f, 2.9f, 255, a, c.light);
            box(c.glow(BOLT_GLOW), pose, x0 - 0.05f, -2.95f, -2.95f, x1 + 0.05f, 1.85f, 2.95f, 255, c.alpha(pulse), FULL);
            float cx = (x0 + x1) / 2;
            spike(c.solid(BOLT_PLATE), pose, cx, -2.9f, -1.0f, 1.0f, cx + out * 2.6f, -2.9f - 4.8f * c.k, -1.0f, a, c.light);
            spike(c.solid(BOLT_PLATE), pose, cx, -2.9f, 1.4f, 0.8f, cx + out * 2.0f, -2.9f - 3.6f * c.k, 1.9f, a, c.light);
            pose.popPose();
        }
        // the crown of crackling bolts, re-striking every two ticks
        enter(pose, m.head);
        pose.translate(0, -8.6f, 0);
        int strike = (int) (c.now / 2f);
        for (int i = 0; i < 7; i++) {
            float jitter = hash(strike * 31 + i);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * (360f / 7f) + jitter * 14f));
            pose.translate(0, 0, -4.4f);
            pose.mulPose(Axis.XP.rotationDegrees(8f + jitter * 12f));      // lean out like a crown
            float h = (4.5f + 3.5f * jitter) * c.k;
            card(c.glow(BOLT_CREST), pose, -1.5f, 0.6f, 1.5f, -h, jitter > 0.5f ? 1 : 0, c.alpha(0.6f + 0.4f * jitter), FULL);
            pose.popPose();
        }
        pose.popPose();
    }

    // ================================================================ Valkyrie Dress (Noelle)
    private static void valkyrie(Ctx c, PlayerModel<AbstractClientPlayer> m) {
        PoseStack pose = c.pose;
        int a = c.alpha(0.88f);
        // crystalline chest plate and the flared skirt plates
        enter(pose, m.body);
        box(c.clear(VALK_PLATE), pose, -4.5f, -0.4f, -2.6f, 4.5f, 7.2f, 2.6f, 255, a, c.light);
        box(c.glow(VALK_GLOW), pose, -4.55f, -0.45f, -2.65f, 4.55f, 7.25f, 2.65f, 255, c.alpha(0.7f), FULL);
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            pose.translate(0, 10f, 0);
            pose.mulPose(Axis.YP.rotationDegrees(i * 60f + 30f));
            pose.translate(0, 0, -3.4f);
            pose.mulPose(Axis.XP.rotationDegrees(-22f - 6f * c.swing));     // flared out, more when running
            card(c.clear(VALK_PLATE), pose, -2.6f, 0f, 2.6f, 6.5f * c.k, 0, a, c.light);
            pose.popPose();
        }
        // avian water wings
        float beat = Mth.sin(c.now * 0.2f) * 8f;
        wings(c, pose, c.clear(VALK_WING), 29f * c.k, 27f * c.k, 34f + beat, -2f + beat * 0.4f, c.alpha(0.8f), FULL, true);
        pose.popPose();
        // gauntlets
        for (ModelPart arm : new ModelPart[]{m.rightArm, m.leftArm}) {
            enter(pose, arm);
            float x0 = arm == m.rightArm ? -3.5f : -1.5f, x1 = arm == m.rightArm ? 1.5f : 3.5f;
            box(c.clear(VALK_PLATE), pose, x0, 4.5f, -2.5f, x1, 10.5f, 2.5f, 255, a, c.light);
            box(c.clear(VALK_PLATE), pose, x0 - 0.3f, -2.7f, -2.7f, x1 + 0.3f, 0.8f, 2.7f, 255, a, c.light);
            pose.popPose();
        }
        // the drill lance in the right hand, held forward, spinning
        enter(pose, m.rightArm);
        pose.translate(-1f, 10.5f, -1f);
        box(c.clear(VALK_PLATE), pose, -0.6f, -0.6f, -1f, 0.6f, 0.6f, 6f, 255, a, c.light);              // the grip, running back past the hand
        box(c.clear(VALK_PLATE), pose, -3.2f, -3.2f, -2.2f, 3.2f, 3.2f, -1.4f, 255, a, c.light);          // the guard
        pose.mulPose(Axis.ZP.rotationDegrees(c.now * 24f));
        cone(c.clear(VALK_LANCE), pose, 2.9f, -2.2f, -2.2f - 24f * c.k, 8, a, FULL);
        pose.popPose();
        // greaves
        for (ModelPart leg : new ModelPart[]{m.rightLeg, m.leftLeg}) {
            enter(pose, leg);
            box(c.clear(VALK_PLATE), pose, -2.5f, 4.8f, -2.6f, 2.5f, 12.3f, 2.5f, 255, a, c.light);
            box(c.glow(VALK_GLOW), pose, -2.55f, 4.75f, -2.65f, 2.55f, 12.35f, 2.55f, 255, c.alpha(0.6f), FULL);
            pose.popPose();
        }
        // helmet wings
        enter(pose, m.head);
        for (int side : new int[]{-1, 1}) {
            pose.pushPose();
            pose.translate(side * 4.2f, -5.5f, 0.5f);
            pose.scale(side, 1, 1);
            pose.mulPose(Axis.YP.rotationDegrees(-70f));
            pose.mulPose(Axis.ZP.rotationDegrees(-15f));
            card(c.clear(VALK_CREST), pose, 0f, 0f, 6.5f * c.k, -6.5f * c.k, 0, c.alpha(0.95f), FULL);
            pose.popPose();
        }
        pose.popPose();
    }

    // ================================================================ geometry
    /**
     * A pair of wings from the upper back (already inside the body part): each a {@code w} x {@code h} card whose root is the
     * texture's bottom-left corner, swept back by {@code sweep} degrees and raised by {@code lift} degrees. The right wing is the
     * left one mirrored.
     */
    private static void wings(Ctx c, PoseStack pose, VertexConsumer vc, float w, float h, float sweep, float lift, int alpha, int light, boolean both) {
        for (int side : both ? new int[]{1, -1} : new int[]{1}) {
            pose.pushPose();
            pose.translate(side * 1.6f, 2.0f, 2.3f);
            pose.scale(side, 1, 1);
            pose.mulPose(Axis.YP.rotationDegrees(-sweep));
            pose.mulPose(Axis.ZP.rotationDegrees(-lift));
            card(vc, pose, 0f, 0f, w, -h, 0, alpha, light);
            pose.popPose();
        }
    }

    /** A flat card in the local XY plane from (x0, y0) to (x1, y1); UV (0,1) at (x0, y0), (1,0) at (x1, y1); flip = mirror U. */
    private static void card(VertexConsumer vc, PoseStack pose, float x0, float y0, float x1, float y1, int flip, int alpha, int light) {
        PoseStack.Pose p = pose.last();
        float u0 = flip == 1 ? 1 : 0, u1 = 1 - u0;
        v(vc, p, x0, y0, 0, u0, 1, alpha, light, 0, 0, -1);
        v(vc, p, x1, y0, 0, u1, 1, alpha, light, 0, 0, -1);
        v(vc, p, x1, y1, 0, u1, 0, alpha, light, 0, 0, -1);
        v(vc, p, x0, y1, 0, u0, 0, alpha, light, 0, 0, -1);
    }

    /** A hanging panel from the edge (x0, y0, z0)-(x1, y0, z0) down to y1, swung out to z1 at its hem. */
    private static void flap(VertexConsumer vc, PoseStack pose, float x0, float y0, float z0, float x1, float y1, float z1, int alpha, int light) {
        PoseStack.Pose p = pose.last();
        v(vc, p, x0, y0, z0, 0, 0, alpha, light, 0, 0, 1);
        v(vc, p, x0, y1, z1, 0, 1, alpha, light, 0, 0, 1);
        v(vc, p, x1, y1, z1, 1, 1, alpha, light, 0, 0, 1);
        v(vc, p, x1, y0, z0, 1, 0, alpha, light, 0, 0, 1);
    }

    /** An axis-aligned box, the whole texture on every face. */
    static void box(VertexConsumer vc, PoseStack pose, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int alpha, int light) {
        PoseStack.Pose p = pose.last();
        // -z (front)
        v(vc, p, x0, y1, z0, 0, 1, alpha, light, 0, 0, -1); v(vc, p, x1, y1, z0, 1, 1, alpha, light, 0, 0, -1);
        v(vc, p, x1, y0, z0, 1, 0, alpha, light, 0, 0, -1); v(vc, p, x0, y0, z0, 0, 0, alpha, light, 0, 0, -1);
        // +z (back)
        v(vc, p, x1, y1, z1, 0, 1, alpha, light, 0, 0, 1); v(vc, p, x0, y1, z1, 1, 1, alpha, light, 0, 0, 1);
        v(vc, p, x0, y0, z1, 1, 0, alpha, light, 0, 0, 1); v(vc, p, x1, y0, z1, 0, 0, alpha, light, 0, 0, 1);
        // -x
        v(vc, p, x0, y1, z1, 0, 1, alpha, light, -1, 0, 0); v(vc, p, x0, y1, z0, 1, 1, alpha, light, -1, 0, 0);
        v(vc, p, x0, y0, z0, 1, 0, alpha, light, -1, 0, 0); v(vc, p, x0, y0, z1, 0, 0, alpha, light, -1, 0, 0);
        // +x
        v(vc, p, x1, y1, z0, 0, 1, alpha, light, 1, 0, 0); v(vc, p, x1, y1, z1, 1, 1, alpha, light, 1, 0, 0);
        v(vc, p, x1, y0, z1, 1, 0, alpha, light, 1, 0, 0); v(vc, p, x1, y0, z0, 0, 0, alpha, light, 1, 0, 0);
        // -y (top, the model's y points down)
        v(vc, p, x0, y0, z0, 0, 1, alpha, light, 0, -1, 0); v(vc, p, x1, y0, z0, 1, 1, alpha, light, 0, -1, 0);
        v(vc, p, x1, y0, z1, 1, 0, alpha, light, 0, -1, 0); v(vc, p, x0, y0, z1, 0, 0, alpha, light, 0, -1, 0);
        // +y (bottom)
        v(vc, p, x0, y1, z1, 0, 1, alpha, light, 0, 1, 0); v(vc, p, x1, y1, z1, 1, 1, alpha, light, 0, 1, 0);
        v(vc, p, x1, y1, z0, 1, 0, alpha, light, 0, 1, 0); v(vc, p, x0, y1, z0, 0, 0, alpha, light, 0, 1, 0);
    }

    /** A four-sided spike: a square base of half-size {@code r} centred at (bx, by, bz) in the y plane, to the tip (tx, ty, tz). */
    static void spike(VertexConsumer vc, PoseStack pose, float bx, float by, float bz, float r, float tx, float ty, float tz, int alpha, int light) {
        PoseStack.Pose p = pose.last();
        float[][] base = {{bx - r, bz - r}, {bx + r, bz - r}, {bx + r, bz + r}, {bx - r, bz + r}};
        for (int i = 0; i < 4; i++) {
            float[] q0 = base[i], q1 = base[(i + 1) % 4];
            Vector3f n = new Vector3f(q1[0] - q0[0], 0, q1[1] - q0[1]).cross(new Vector3f(tx - q0[0], ty - by, tz - q0[1]));
            if (n.lengthSquared() < 1e-6f) n.set(0, 1, 0); else n.normalize();
            v(vc, p, q0[0], by, q0[1], 0, 1, alpha, light, n.x, n.y, n.z);
            v(vc, p, q1[0], by, q1[1], 1, 1, alpha, light, n.x, n.y, n.z);
            v(vc, p, tx, ty, tz, 0.5f, 0, alpha, light, n.x, n.y, n.z);
            v(vc, p, tx, ty, tz, 0.5f, 0, alpha, light, n.x, n.y, n.z);
        }
    }

    /** A cone along z from a base ring of radius {@code r} at z0 to the tip at z1, {@code n} sides. */
    static void cone(VertexConsumer vc, PoseStack pose, float r, float z0, float z1, int n, int alpha, int light) {
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n, am = (a0 + a1) / 2;
            float nx = Mth.cos(am), ny = Mth.sin(am);
            v(vc, p, Mth.cos(a0) * r, Mth.sin(a0) * r, z0, (float) i / n, 1, alpha, light, nx, ny, 0);
            v(vc, p, Mth.cos(a1) * r, Mth.sin(a1) * r, z0, (float) (i + 1) / n, 1, alpha, light, nx, ny, 0);
            v(vc, p, 0, 0, z1, (i + 0.5f) / n, 0, alpha, light, nx, ny, 0);
            v(vc, p, 0, 0, z1, (i + 0.5f) / n, 0, alpha, light, nx, ny, 0);
        }
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float vv, int alpha, int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, alpha).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** 0..1, stable per seed. */
    static float hash(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65535f;
    }
}
