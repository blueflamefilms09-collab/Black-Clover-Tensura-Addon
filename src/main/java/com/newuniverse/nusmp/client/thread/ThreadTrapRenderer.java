package com.newuniverse.nusmp.client.thread;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.thread.ThreadTrapEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: a thread trap must be nearly invisible, or it is not a trap, yet it has to be findable by someone who looks for it. Each node
 * is drawn as a few hair-thin red filaments crossing at knee height with very low alpha that rises as the viewer comes within
 * ten blocks and shimmers slowly, so a fleeing enemy does not see a wall of red but a careful player sees where the threads are.
 * It draws with the vanilla line render type, so there is no texture or model to load.
 */
public class ThreadTrapRenderer extends EntityRenderer<ThreadTrapEntity> {
    public ThreadTrapRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/rouge_cat.png");     // 1.99.0: built once, not per entity per frame

    @Override public ResourceLocation getTextureLocation(ThreadTrapEntity e) { return TEXTURE; }

    @Override
    public void render(ThreadTrapEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        double d = Math.sqrt(this.entityRenderDispatcher.distanceToSqr(e));
        float near = Mth.clamp(1f - (float) d / 10f, 0f, 1f);
        float age = e.tickCount + partial;
        float alpha = 0.10f + 0.45f * near + 0.05f * Mth.sin(age * 0.07f);
        if (e.tickCount < 10) alpha *= e.tickCount / 10f;
        int al = (int) (255 * Mth.clamp(alpha, 0f, 0.7f));
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        var last = pose.last();
        float r = 0.7f;
        for (int h = 0; h < 3; h++) {
            float y = 0.15f + 0.3f * h;
            for (int k = 0; k < 4; k++) {
                float a = k * Mth.PI / 4f + h * 0.2f;
                float x0 = Mth.cos(a) * r, z0 = Mth.sin(a) * r;
                float sag = -0.04f * Mth.sin(age * 0.1f + k + h);
                vc.addVertex(last, x0, y + sag, z0).setColor(255, 60, 90, al).setNormal(last, -x0, 0f, -z0);
                vc.addVertex(last, -x0, y - sag, -z0).setColor(255, 60, 90, al).setNormal(last, x0, 0f, z0);
            }
        }
    }
}
