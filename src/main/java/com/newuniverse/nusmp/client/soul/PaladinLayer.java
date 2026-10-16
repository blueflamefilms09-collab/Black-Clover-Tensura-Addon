package com.newuniverse.nusmp.client.soul;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: this is the render layer of the Paladin look, added to every {@link LivingEntityRenderer} (every creature type that has one,
 * and both player skins) in one pass of the {@code AddLayers} event, so no renderer has to know about Paladins. The layer draws
 * only when the creature wears the Paladin marker ({@link PaladinDraw#dressed}); for everything else it costs one effect lookup.
 * It never builds a model: {@link PaladinDraw} draws the two shared GeoLite models. Creatures whose renderer is not a
 * living-entity renderer (the GeckoLib ones) are dressed by {@link PaladinOverlay} instead; this layer reports which entities it
 * drew this frame ({@link #DRAWN}) so none is drawn twice.
 */
public final class PaladinLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    /** Entity ids this layer drew since the overlay last ran (rendered on the client thread only). */
    static final IntOpenHashSet DRAWN = new IntOpenHashSet();

    public PaladinLayer(RenderLayerParent<T, M> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial,
                       float age, float netHeadYaw, float headPitch) {
        if (!PaladinDraw.dressed(e)) return;
        DRAWN.add(e.getId());
        PaladinDraw.draw(pose, buffers, e, age);
    }

    /** The event: every type that has a living-entity renderer, and the player skins. */
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            if (type == EntityType.PLAYER) continue;                           // players are drawn through the skins below
            addTo(event, type);
        }
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer<?, ?> r) attach(r);
        }
    }

    private static <E extends Entity> void addTo(EntityRenderersEvent.AddLayers event, EntityType<E> type) {
        try {
            if (event.getRenderer(type) instanceof LivingEntityRenderer<?, ?> r) attach(r);
        } catch (RuntimeException | LinkageError oddRenderer) {
            // a renderer that cannot take a layer is simply not dressed by the layer; the overlay draws it
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void attach(LivingEntityRenderer renderer) { renderer.addLayer(new PaladinLayer(renderer)); }
}
