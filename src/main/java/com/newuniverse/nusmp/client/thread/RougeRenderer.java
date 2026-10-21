package com.newuniverse.nusmp.client.thread;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.RougeCatModel;
import com.newuniverse.nusmp.thread.RougeEntity;
import com.newuniverse.nusmp.thread.RougePerch;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: draws Rouge, the red thread cat, with the cat texture the head cat already has, plus a faint additive second pass so she
 * glows like thread in the dark (the same trick the Time effects use for their bright cores). One model is baked per renderer and
 * reused for every cat; the pose is written into its parts each frame, so nothing is allocated while drawing. Scale shrinks to
 * nothing as she unravels and grows back (with a small overshoot) as she reknits.
 */
public class RougeRenderer extends EntityRenderer<RougeEntity> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/rouge_cat.png");
    private static final float SCALE = 0.45f;
    private final RougeModel model;

    public RougeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new RougeModel(ctx.bakeLayer(RougeCatModel.LAYER));
        this.shadowRadius = 0.15f;
    }

    @Override public ResourceLocation getTextureLocation(RougeEntity e) { return TEX; }

    @Override
    public void render(RougeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        int state = e.state();
        // 1.46.0: settled on the head, the head-cat layer draws her; the entity only supplies the name plate (no second model)
        if (!RougePerch.bodyDrawn(state == RougeEntity.HEAD, e.stateAge())) { super.render(e, yaw, partial, pose, buffers, light); return; }
        float t = e.stateAge() + partial, age = e.tickCount + partial;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot) - body;
        float limb = e.walkAnimation.position(partial), speed = Math.min(1f, e.walkAnimation.speed(partial) * 2.5f);
        model.pose(state, t, age, limb, speed, headYaw, Mth.lerp(partial, e.xRotO, e.getXRot()));

        float s = SCALE, alpha = 1f;
        if (state == RougeEntity.UNRAVEL) {
            float u = Mth.clamp(t / RougeEntity.UNRAVEL_TICKS, 0f, 1f);
            s *= 1f - u * u; alpha = 1f - u;
        } else if (state == RougeEntity.REKNIT) {
            float u = Mth.clamp(t / RougeEntity.REKNIT_TICKS, 0f, 1f);
            s *= Mth.clamp(1.15f - 0.15f * (float) Math.cos(u * Math.PI) - (1f - u) * (1f - u) * 1.15f, 0.02f, 1.1f);
            alpha = Math.min(1f, 0.3f + u);
        } else if (state == RougeEntity.HOP) {
            s *= 0.85f;
        }
        if (s <= 0.01f) return;
        int a = (int) (255 * alpha);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - body));
        pose.scale(-s, -s, s);
        pose.translate(0f, -1.501f, 0f);
        model.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEX)), light, OverlayTexture.NO_OVERLAY, (a << 24) | 0xFFFFFF);
        model.render(pose, buffers.getBuffer(com.newuniverse.nusmp.client.SafeAdditive.additive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, ((int) (a * 0.3f) << 24) | 0xFF8090);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                    // the name plate of a named Rouge
    }
}
