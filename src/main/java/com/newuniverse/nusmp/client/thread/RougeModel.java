package com.newuniverse.nusmp.client.thread;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.thread.RougeEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Why: Rouge on the ground has to move like a small cat made of thread: sit with a swaying tail, scurry, leap, tap an ally with a
 * raised paw, come apart and weave herself back. The geometry is the one the head cat already uses (baked from
 * {@code RougeCatModel.LAYER}, so there is one texture and one layer definition to keep), and this class only poses its parts from
 * the synced state and the age of that state. All poses are computed into the existing parts, nothing is allocated per frame.
 */
public class RougeModel {
    private final ModelPart root, body, head, tail, tailTip, legR, legL, haunchR, haunchL;

    public RougeModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.tailTip = tail.getChild("tail_tip");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
        this.haunchR = root.getChild("haunch_r");
        this.haunchL = root.getChild("haunch_l");
    }

    /**
     * Poses the parts. 'state' is a {@link RougeEntity} state, 't' the ticks since it began (with the partial tick), 'age' the entity's
     * age for idle motion, 'limb' and 'speed' the walk cycle and how fast she is going (0..1), 'headYaw' and 'headPitch' where she looks.
     */
    public void pose(int state, float t, float age, float limb, float speed, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(headYaw, -50f, 50f) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -30f, 30f) * Mth.DEG_TO_RAD;
        tail.yRot += Mth.sin(age * 0.11f) * 0.25f;
        tailTip.yRot += Mth.sin(age * 0.13f + 1f) * 0.3f;
        switch (state) {
            case RougeEntity.SCURRY, RougeEntity.HOP -> {
                float swing = Mth.cos(limb * 1.5f) * 1.3f * Math.max(0.35f, speed);
                legR.xRot = swing; legL.xRot = -swing;
                haunchR.xRot = -swing * 0.6f; haunchL.xRot = swing * 0.6f;
                body.xRot += 0.35f;
                head.xRot += 0.15f;
                tail.xRot = -0.9f + Mth.sin(age * 0.5f) * 0.12f;
            }
            case RougeEntity.LEAP -> {
                legR.xRot = -1.35f; legL.xRot = -1.15f;
                haunchR.xRot = 0.9f; haunchL.xRot = 0.9f;
                body.xRot += -0.35f;
                tail.xRot = 0.5f;
                tailTip.xRot = 0.4f;
            }
            case RougeEntity.TAP -> {
                float p = Mth.clamp(t / RougeEntity.TAP_TICKS, 0f, 1f);
                legR.xRot = -2.5f * Mth.sin(p * Mth.PI) - 0.2f * Mth.sin(p * Mth.PI * 4f);     // the paw reaches out and taps
                body.xRot += 0.25f * Mth.sin(p * Mth.PI);
                head.xRot += 0.3f * Mth.sin(p * Mth.PI);
                tail.xRot = -0.7f;
            }
            case RougeEntity.UNRAVEL -> {
                float u = Mth.clamp(t / RougeEntity.UNRAVEL_TICKS, 0f, 1f);
                tail.zScale = 1f + u * 2.5f; tailTip.zScale = 1f + u * 3f;                   // the tail pays out as a loose thread
                head.zRot = Mth.sin(t * 0.9f) * 0.5f * u;
                legR.xRot = Mth.sin(t * 1.1f) * 0.8f * u; legL.xRot = -Mth.sin(t * 1.1f) * 0.8f * u;
            }
            case RougeEntity.REKNIT -> {
                float u = 1f - Mth.clamp(t / RougeEntity.REKNIT_TICKS, 0f, 1f);
                tail.zScale = 1f + u * 2.5f; tailTip.zScale = 1f + u * 3f;
                head.zRot = Mth.sin(t * 0.9f) * 0.5f * u;
            }
            default -> {                                                                   // sitting: a slow look round
                head.yRot += Mth.sin(age * 0.04f) * 0.6f;
                head.xRot += Mth.sin(age * 0.07f) * 0.08f;
            }
        }
    }

    public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) { root.render(pose, vc, light, overlay, color); }
}
