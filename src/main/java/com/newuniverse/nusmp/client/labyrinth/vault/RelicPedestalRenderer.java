package com.newuniverse.nusmp.client.labyrinth.vault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Why: the vault reveal: the held relic floats 0.9 above the pedestal's base, bobs, and turns once every six seconds, with a soft glow in the
 * colour of its grade behind it (white for a non-relic). The item is drawn with the vanilla item renderer in the GROUND context (the same
 * transform a dropped item uses, so every relic's existing model and 3D weapon model look right without a new transform set). The glow is one
 * camera-facing translucent emissive quad on the mod's existing soft glow texture, full-bright, so it reads as light rather than as a sprite,
 * and it is drawn before the item so the item sits in front of it. The phase comes from the block entity's client ticker plus the partial tick:
 * no per-frame allocation, no system clock, and every pedestal in view is in step.
 */
public final class RelicPedestalRenderer implements BlockEntityRenderer<RelicPedestalBlockEntity> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/glow.png");
    private static final float TURN_TICKS = 120f;        // one turn in six seconds
    private static final float HOVER = 0.9f;             // the pivot of the item above the block's bottom
    private static final float BOB = 0.05f;

    private final BlockEntityRenderDispatcher dispatcher;

    public RelicPedestalRenderer(BlockEntityRendererProvider.Context ctx) { this.dispatcher = ctx.getBlockEntityRenderDispatcher(); }

    @Override
    public void render(RelicPedestalBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = be.getHeld();
        if (stack.isEmpty()) return;
        float t = be.phase() + partialTick;
        float bob = Mth.sin(t * (float) (Math.PI * 2.0 / 80.0)) * BOB;
        float yaw = (t % TURN_TICKS) / TURN_TICKS * 360f;
        int rgb = be.glowRgb();
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float pulse = 0.42f + 0.1f * Mth.sin(t * (float) (Math.PI * 2.0 / 50.0));

        pose.pushPose();
        pose.translate(0.5, HOVER + 0.12 + bob, 0.5);

        // the glow: a camera-facing quad behind the item
        pose.pushPose();
        if (dispatcher != null && dispatcher.camera != null) pose.mulPose(dispatcher.camera.rotation());
        glow(pose, buffers.getBuffer(com.newuniverse.nusmp.client.SafeAdditive.emissive(GLOW)), 0.55f, r, g, b, pulse);
        glow(pose, buffers.getBuffer(com.newuniverse.nusmp.client.SafeAdditive.emissive(GLOW)), 0.28f, r, g, b, pulse * 0.8f);
        pose.popPose();

        // the relic itself
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }

    private static void glow(PoseStack pose, VertexConsumer vc, float s, float r, float g, float b, float a) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        int light = LightTexture.FULL_BRIGHT;
        vc.addVertex(m, -s, -s, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, -s, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, s, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, -s, s, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
    }
}
