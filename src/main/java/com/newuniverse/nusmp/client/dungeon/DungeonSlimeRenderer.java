package com.newuniverse.nusmp.client.dungeon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.dungeon.DungeonSlimeEntity;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;

/** The Dungeon slime: the game's own slime body in a teal gel of its own (tools/gen_dungeon_assets.py), three sizes by growth. */
public class DungeonSlimeRenderer extends MobRenderer<DungeonSlimeEntity, SlimeModel<DungeonSlimeEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/dungeon_slime.png");

    public DungeonSlimeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SlimeModel<>(ctx.bakeLayer(ModelLayers.SLIME)), 0.25f);
        addLayer(new SlimeOuterLayer<>(this, ctx.getModelSet()));
    }

    @Override public ResourceLocation getTextureLocation(DungeonSlimeEntity e) { return TEXTURE; }

    @Override
    protected void scale(DungeonSlimeEntity e, PoseStack pose, float partial) {
        float s = (1f + 0.6f * e.growth()) * 0.999f;
        pose.scale(s, s, s);
        pose.translate(0f, 0.001f, 0f);
    }
}
