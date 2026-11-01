package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: the 3D parts of a devil's body (3.48.0): the two horns (three chained segments each, curving back and out), the two bat wings (upper arm, forearm, three fingers, a membrane wedge on each finger)
 * and the copy of the player's body that carries the Weg markings. All are plain vanilla {@link ModelPart} trees baked from {@link LayerDefinition}s, so the layer draws them with the game's own model
 * code (no meshes of ours, no allocation per frame). The textures are tileable 32x32 / 64x64 files, so a replacement made in Blockbench only has to keep the sizes (see docs/devil_contract.md).
 *
 * <p>Layout of {@code wings.png} (64x64): the left half (x 0 to 31) is the bone material, the right half (x 32 to 63) the membrane. Every bone box and every membrane box has its texture offset in one of those
 * halves, so a texture that keeps the two halves can be redrawn freely. Not seen in a running game by its author.
 */
public final class DevilModels {
    private DevilModels() {}

    public static final ModelLayerLocation HORNS = loc("devil_horns", "main"), WINGS = loc("devil_wings", "main"), WEG_WIDE = loc("devil_weg", "wide"), WEG_SLIM = loc("devil_weg", "slim");

    private static ModelLayerLocation loc(String name, String part) { return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("nusmp", name), part); }

    /** Game bus on the mod bus: registers the four layer definitions. */
    static void register(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(HORNS, Horns::create);
        e.registerLayerDefinition(WINGS, Wings::create);
        e.registerLayerDefinition(WEG_WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.02F), false), 64, 64));
        e.registerLayerDefinition(WEG_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.02F), true), 64, 64));
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    // ================================================================================================ horns
    /** Two horns on the top of the head. The layer scales them about the head top as the level grows. Pixels, y down, the head top at y = -8. */
    public static final class Horns {
        private final ModelPart root;

        public Horns(ModelPart root) { this.root = root; }

        static LayerDefinition create() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            for (int i = 0; i < 2; i++) {
                float s = i == 0 ? -1f : 1f;                                  // -1 = the player's right (negative x), +1 = the left
                String n = i == 0 ? "r" : "l";
                PartDefinition a = root.addOrReplaceChild("horn_" + n + "_1", CubeListBuilder.create().texOffs(0, 0).addBox(-1f, -3f, -1f, 2f, 3f, 2f),
                        PartPose.offsetAndRotation(s * 2.6f, -7.7f, -1.2f, -0.30f, 0f, s * 0.42f));                       // out of the head, leaning out and back
                PartDefinition b = a.addOrReplaceChild("horn_" + n + "_2", CubeListBuilder.create().texOffs(0, 8).addBox(-1f, -3f, -1f, 2f, 3f, 2f, new CubeDeformation(-0.15f)),
                        PartPose.offsetAndRotation(0f, -3f, 0f, -0.32f, 0f, s * 0.26f));                                     // the curve goes on
                b.addOrReplaceChild("horn_" + n + "_3", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5f, -3f, -0.5f, 1f, 3f, 1f),
                        PartPose.offsetAndRotation(0f, -2.85f, 0f, -0.34f, 0f, s * 0.12f));                                  // the tip
            }
            return LayerDefinition.create(mesh, 32, 32);
        }

        public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) { root.render(pose, vc, light, overlay, color); }
    }

    // ================================================================================================ wings
    /**
     * Two wings on the back. {@link #pose} sets how far they are spread and the beat of the flap; the layer draws the whole tree. Pixels, y down, the back of the body at z = +2.
     * Each wing is a chain of bones (upper arm, forearm, three fingers) and four membrane wedges that all start at the shoulder and run to the wrist, the three finger tips and a point on the body, so the
     * membrane fills the wing from the body to the tips. A wedge is a staircase of three thin boxes (a triangle in blocks) that {@link #pose} turns and stretches to its two corners every frame.
     */
    public static final class Wings {
        private static final float HUM = 9f, FORE = 12f, NOM_L = 22f, NOM_W = 15.2f, BODY_X = 0.5f, BODY_Y = 14f;
        private static final float[] FINGER = {13f, 14f, 12f}, STEP_X = {1f, 8f, 15f}, STEP_W = {3.4f, 8.8f, 15.2f};

        private final ModelPart root;
        private final Side[] sides = new Side[2];                              // 0 = the player's right wing, 1 = the left
        private final float[] ax = new float[5], ay = new float[5];            // the wedge corners in the wing plane (scratch, no allocation per frame)

        private record Side(float s, ModelPart wing, ModelPart hum, ModelPart fore, ModelPart[] finger, ModelPart[] wedge) {}

        public Wings(ModelPart root) {
            this.root = root;
            for (int i = 0; i < 2; i++) {
                String n = i == 0 ? "r" : "l";
                ModelPart wing = root.getChild("wing_" + n), hum = wing.getChild("hum"), fore = hum.getChild("fore");
                ModelPart[] f = new ModelPart[3], w = new ModelPart[4];
                for (int k = 0; k < 3; k++) f[k] = fore.getChild("f" + k);
                for (int k = 0; k < 4; k++) w[k] = wing.getChild("m" + k);
                sides[i] = new Side(i == 0 ? -1f : 1f, wing, hum, fore, f, w);
            }
        }

        static LayerDefinition create() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            for (int i = 0; i < 2; i++) {
                float s = i == 0 ? -1f : 1f;
                String n = i == 0 ? "r" : "l";
                PartDefinition wing = root.addOrReplaceChild("wing_" + n, CubeListBuilder.create(), PartPose.offset(s * 2.2f, 2.2f, 2.6f));      // at the shoulder blade
                PartDefinition hum = wing.addOrReplaceChild("hum", CubeListBuilder.create().texOffs(0, 0).addBox(s > 0 ? 0f : -HUM, -1f, -1f, HUM, 2f, 2f), PartPose.ZERO);
                PartDefinition fore = hum.addOrReplaceChild("fore", CubeListBuilder.create().texOffs(0, 6).addBox(s > 0 ? 0f : -FORE, -1f, -1f, FORE, 2f, 2f), PartPose.offset(s * HUM, 0f, 0f));
                for (int k = 0; k < 3; k++)
                    fore.addOrReplaceChild("f" + k, CubeListBuilder.create().texOffs(0, 12 + 4 * k).addBox(s > 0 ? 0f : -FINGER[k], -0.5f, -0.5f, FINGER[k], 1f, 1f),
                            PartPose.offset(s * FORE, 0f, 0f));                                                                                        // the three fingers fan out from the wrist
                for (int k = 0; k < 4; k++) {                                                                                                      // the membrane: four wedges from the shoulder
                    CubeListBuilder cb = CubeListBuilder.create();
                    for (int st = 0; st < 3; st++)
                        cb.texOffs(32 + 16 * (k % 2), 20 * st).addBox(s > 0 ? STEP_X[st] : -(STEP_X[st] + 7f), -STEP_W[st] / 2f, -0.25f, 7f, STEP_W[st], 0.5f);
                    wing.addOrReplaceChild("m" + k, cb, PartPose.offset(0f, 0f, 0.08f * k));                                                       // a hair apart in depth: no flicker where wedges overlap near the shoulder
                }
            }
            return LayerDefinition.create(mesh, 64, 64);
        }

        /**
         * @param spread 0 = folded against the back, 1 = fully spread
         * @param flap   -1 to 1, the beat (positive = down stroke)
         * @param size   a uniform scale about the shoulders
         */
        public void pose(float spread, float flap, float size) {
            float sp = Math.max(0f, Math.min(1f, spread));
            float yaw = -(0.2f + 0.3f * sp);                                                   // the tips swing back as the wings open (left wing; the right mirrors)
            float hum = lerp(-1.25f, -0.9f, sp) + flap * 0.42f;                                // negative = up
            float elbow = lerp(2.3f, 0.65f, sp) - flap * 0.12f;
            float foreAbs = hum + elbow;
            float f0 = lerp(1.3f, 0.15f, sp) + flap * 0.3f, f1 = lerp(1.4f, 0.7f, sp) + flap * 0.4f, f2 = lerp(1.5f, 1.25f, sp) + flap * 0.5f;      // absolute finger angles (positive = down)
            // the corners of the membrane in the left wing's plane: the wrist, the three finger tips, a point on the body
            float wx = HUM * (float) Math.cos(hum) + FORE * (float) Math.cos(foreAbs), wy = HUM * (float) Math.sin(hum) + FORE * (float) Math.sin(foreAbs);
            ax[0] = wx;
            ay[0] = wy;
            ax[1] = wx + FINGER[0] * (float) Math.cos(f0);
            ay[1] = wy + FINGER[0] * (float) Math.sin(f0);
            ax[2] = wx + FINGER[1] * (float) Math.cos(f1);
            ay[2] = wy + FINGER[1] * (float) Math.sin(f1);
            ax[3] = wx + FINGER[2] * (float) Math.cos(f2);
            ay[3] = wy + FINGER[2] * (float) Math.sin(f2);
            ax[4] = BODY_X;
            ay[4] = BODY_Y;
            for (Side sd : sides) {
                float s = sd.s();
                sd.wing().yRot = s * yaw;
                sd.wing().xScale = sd.wing().yScale = sd.wing().zScale = size;
                sd.hum().zRot = s * hum;
                sd.fore().zRot = s * elbow;
                sd.finger()[0].zRot = s * (f0 - foreAbs);
                sd.finger()[1].zRot = s * (f1 - foreAbs);
                sd.finger()[2].zRot = s * (f2 - foreAbs);
            }
            for (int k = 0; k < 4; k++) {
                float x0 = ax[k], y0 = ay[k], x1 = ax[k + 1], y1 = ay[k + 1];
                float aim = ((float) Math.atan2(y0, x0) + (float) Math.atan2(y1, x1)) * 0.5f;
                float len = ((float) Math.hypot(x0, y0) + (float) Math.hypot(x1, y1)) * 0.5f, chord = (float) Math.hypot(x1 - x0, y1 - y0);
                for (Side sd : sides) {
                    ModelPart m = sd.wedge()[k];
                    m.zRot = sd.s() * aim;
                    m.xScale = Math.max(0.1f, len / NOM_L);
                    m.yScale = Math.max(0.06f, chord / NOM_W);
                }
            }
        }

        public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) { root.render(pose, vc, light, overlay, color); }
    }
}
