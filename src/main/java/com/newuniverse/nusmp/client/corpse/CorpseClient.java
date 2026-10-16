package com.newuniverse.nusmp.client.corpse;

import com.newuniverse.nusmp.corpse.CorpseEntities;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Why: the client half of Soul Corpse Magic in one place. It registers the renderer of the three named puppets and adds the decay layer
 * ({@link CorpseDecayLayer}) to every living-entity renderer, including both player skins. Wire it in the client part of NUSMP:
 * {@code modEventBus.addListener(CorpseClient::register)} and {@code modEventBus.addListener(CorpseClient::addLayers)}.
 */
public final class CorpseClient {
    private CorpseClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CorpseEntities.JIMMY.get(), NamedCorpseRenderer::new);
        e.registerEntityRenderer(CorpseEntities.ALFREDO.get(), NamedCorpseRenderer::new);
        e.registerEntityRenderer(CorpseEntities.CARL.get(), NamedCorpseRenderer::new);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (EntityType<?> type : e.getEntityTypes()) {
            EntityRenderer<?> r = e.getRenderer(type);
            if (r instanceof LivingEntityRenderer<?, ?> living) attach(living);
        }
        for (var skin : e.getSkins()) {
            EntityRenderer<?> r = e.getSkin(skin);
            if (r instanceof LivingEntityRenderer<?, ?> living) attach(living);
        }
    }

    /** The raw call is the only way to add a layer for "some" entity type; the layer checks the entity itself before drawing. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void attach(LivingEntityRenderer renderer) {
        renderer.addLayer(new CorpseDecayLayer(renderer));
    }
}
