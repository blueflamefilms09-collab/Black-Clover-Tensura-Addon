package com.newuniverse.nusmp.client.labyrinth.seal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.dungeon.LivingArmorRenderer;
import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the Floor Warden is a Living Armor grown huge, so its renderer IS the Living Armor's ({@link LivingArmorRenderer}: the zombie
 * humanoid model, the armour layer that draws what it wears) with two changes: the pose is scaled 1.75x in {@link #scale} (the hook
 * {@code LivingEntityRenderer} gives for exactly this, applied before the model is posed, so armour, held items and the shadow all grow
 * with it) and the skin is a dark violet re-tint of the Living Armor sheet ({@code textures/entity/floor_warden.png}, made by
 * {@code tools/gen_labyrinth_seal_assets.py}). A tint hook would have needed a custom render type or a model layer; a second 64x64 texture
 * is cheaper and lets an artist repaint the Warden later without touching Java. The renderer stays typed on {@code LivingArmorEntity}
 * because that is the parent's generic parameter; {@code registerEntityRenderer} accepts it for the Warden's type since the type is
 * {@code EntityType<? extends LivingArmorEntity>}.
 */
public class FloorWardenRenderer extends LivingArmorRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/floor_warden.png");
    private static final float SCALE = 1.75f;

    public FloorWardenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.5f * SCALE;
    }

    @Override
    protected void scale(LivingArmorEntity e, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
        super.scale(e, pose, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(LivingArmorEntity e) {
        return e instanceof FloorWardenEntity ? TEXTURE : super.getTextureLocation(e);
    }
}
