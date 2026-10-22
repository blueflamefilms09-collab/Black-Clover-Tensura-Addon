package com.newuniverse.nusmp.client.corpse;

import com.newuniverse.nusmp.corpse.CorpseEntities;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
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
        e.registerEntityRenderer(CorpseEntities.DAVID.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.MICHAEL_CAESAL.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.SOUL_ZOMBIE.get(), ZombieRenderer::new);          // the zombie of Soul Corpse Magic is drawn as a zombie; the decay layer stitches it
    }

    /**
     * Registered last (EventPriority.LOWEST): the dungeon player-model kit also registers a renderer for Jimmy, Alfredo and Carl, and the one registered last stays. Unless
     * {@code corpse.classicLooks} is set, the three get the new custom looks that follow the series art ({@link RadesUndeadRenderer}); the old renderers and assets stay in the tree.
     */
    public static void registerLooks(EntityRenderersEvent.RegisterRenderers e) {
        if (CorpseClientConfig.classicLooks()) return;
        e.registerEntityRenderer(CorpseEntities.JIMMY.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.ALFREDO.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.CARL.get(), RadesUndeadRenderer::new);
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
