package com.newuniverse.nusmp.client.dungeon;

import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** A Living Armor: a humanoid mass of dark mollusk flesh (tools/gen_dungeon_assets.py) wearing whatever armour it found. */
public class LivingArmorRenderer extends HumanoidMobRenderer<LivingArmorEntity, HumanoidModel<LivingArmorEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/living_armor.png");

    public LivingArmorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override public ResourceLocation getTextureLocation(LivingArmorEntity e) { return TEXTURE; }
}
