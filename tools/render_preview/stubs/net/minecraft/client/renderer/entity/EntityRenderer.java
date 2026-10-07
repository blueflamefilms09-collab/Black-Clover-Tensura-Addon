package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Preview stub of EntityRenderer (the base of the mod's MagicPropRenderer, compiled for real). */
public abstract class EntityRenderer<T extends Entity> {
    protected float shadowRadius;
    protected float shadowStrength = 1.0F;

    protected EntityRenderer(EntityRendererProvider.Context context) {}

    public abstract ResourceLocation getTextureLocation(T entity);

    public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) { return true; }

    public Vec3 getRenderOffset(T entity, float partialTicks) { return Vec3.ZERO; }

    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {}
}
