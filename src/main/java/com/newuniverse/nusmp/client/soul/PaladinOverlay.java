package com.newuniverse.nusmp.client.soul;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;

/**
 * Why: Tensura's mobs are drawn by GeckoLib, whose renderers are not {@code LivingEntityRenderer}s and take no layers, and they are
 * the creatures this mod's players will convert most. So the halo and wings are also drawn from the world render stage, for every
 * dressed creature the layer did not draw this frame ({@link PaladinLayer#DRAWN}) and that is inside the view frustum and within
 * 64 blocks. The frame is rebuilt exactly as vanilla's living renderer builds it (translate to the interpolated feet, turn by the
 * interpolated body yaw, flip, lower by 1.501) so {@link PaladinDraw} draws the same picture in both paths. The list of candidates is
 * the client level's own entity list, filtered by the cheap marker check; no map of entities is kept here.
 */
public final class PaladinOverlay {
    private PaladinOverlay() {}

    private static final double RANGE_SQR = 64.0 * 64.0;

    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { PaladinLayer.DRAWN.clear(); return; }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = null;
        boolean any = false;
        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof LivingEntity e) || !PaladinDraw.dressed(e) || PaladinLayer.DRAWN.contains(e.getId())) continue;
            if (firstPerson && e == mc.getCameraEntity()) continue;
            if (e.distanceToSqr(cam) > RANGE_SQR || !event.getFrustum().isVisible(e.getBoundingBoxForCulling())) continue;
            if (pose == null) {
                Matrix4fStack modelView = RenderSystem.getModelViewStack();
                modelView.pushMatrix();
                modelView.set(event.getModelViewMatrix());
                RenderSystem.applyModelViewMatrix();
                pose = new PoseStack();
            }
            any = true;
            pose.pushPose();
            pose.translate(Mth.lerp(partial, e.xOld, e.getX()) - cam.x, Mth.lerp(partial, e.yOld, e.getY()) - cam.y, Mth.lerp(partial, e.zOld, e.getZ()) - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            pose.scale(-1f, -1f, 1f);
            pose.translate(0f, -PaladinDraw.FEET, 0f);
            PaladinDraw.draw(pose, buffers, e, e.tickCount + partial);
            pose.popPose();
        }
        PaladinLayer.DRAWN.clear();
        if (any) {
            buffers.endBatch();
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
