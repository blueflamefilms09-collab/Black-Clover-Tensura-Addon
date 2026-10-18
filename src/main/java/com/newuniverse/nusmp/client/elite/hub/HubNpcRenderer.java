package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.elite.hub.HubNpcBase;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the look of the two Hub NPCs. The mod has an art kit for player-model characters (the arc NPCs: a Python model spec per character, a roster record, a dialogue file), but those are all
 * ONE shared entity type, and the brief asks for two entity classes of their own; wiring a new kit character to an entity that is not the shared one means touching the kit's renderer registry,
 * which is another builder's code. So these two are drawn with the vanilla VILLAGER model (a clean, familiar silhouette that reads as "a person you can talk to") and their own 64x64 skins,
 * painted by {@code tools/elite/gen_hub_textures.py}: a blue-and-gold scholar with a white beard for the Quest Giver, a green-aproned moustached trader for the Merchant. The name tag is shown
 * (the entity says so), the head turns with the look goal, and the hat parts of the model stay hidden.
 */
public class HubNpcRenderer extends MobRenderer<HubNpcBase, VillagerModel<HubNpcBase>> {
    private final ResourceLocation texture;

    public HubNpcRenderer(EntityRendererProvider.Context ctx, String skin) {
        super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
        this.texture = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/hub/" + skin + ".png");
        addLayer(new CustomHeadLayer<>(this, ctx.getModelSet(), ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(HubNpcBase entity) { return texture; }
}
