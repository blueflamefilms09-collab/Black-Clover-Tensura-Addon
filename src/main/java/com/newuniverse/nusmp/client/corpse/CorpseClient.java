package com.newuniverse.nusmp.client.corpse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.corpse.CorpseEntities;
import com.newuniverse.nusmp.corpse.NamedCorpses;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

/**
 * Why: the client half of Soul Corpse Magic in one place. It registers the renderer of the three named puppets and adds the decay layer
 * ({@link CorpseDecayLayer}) to every living-entity renderer, including both player skins. Wire it in the client part of NUSMP:
 * {@code modEventBus.addListener(CorpseClient::register)} and {@code modEventBus.addListener(CorpseClient::addLayers)}.
 */
public final class CorpseClient {
    private CorpseClient() {}

    private static final Logger LOG = LogUtils.getLogger();

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CorpseEntities.JIMMY.get(), NamedCorpseRenderer::new);
        e.registerEntityRenderer(CorpseEntities.ALFREDO.get(), NamedCorpseRenderer::new);
        e.registerEntityRenderer(CorpseEntities.CARL.get(), NamedCorpseRenderer::new);
        e.registerEntityRenderer(CorpseEntities.DAVID.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.MICHAEL_CAESAL.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.SOUL_ZOMBIE.get(), ZombieRenderer::new);          // the zombie of Soul Corpse Magic is drawn as a zombie; the decay layer stitches it
    }

    /**
     * Registered last: the dungeon player-model kit also registers a renderer for Jimmy, Alfredo and Carl, and the one registered last stays. Unless
     * {@code corpse.classicLooks} is set, the three get the new custom looks that follow the series art ({@link RadesUndeadRenderer}); the old renderers and assets stay in the tree.
     * <p>
     * 3.7.1 (docs/magic/rades_cast_fix.md, the root cause of "Rades's Number Summons are just magic circles" for No.4, No.2 and No.1): this used to be added to the mod bus at
     * {@code EventPriority.LOWEST} in the NUSMP constructor BEFORE {@code PlayerModelRegistry.init}, which adds the kit's listener at the SAME priority; listeners of one priority run in the order
     * they were added, so the kit ran after this and its renderer (the old player-model puppet, or the old geo puppet) stayed. NUSMP now also hands this method to
     * {@code PlayerModelRegistry.extend}, which the kit runs INSIDE its own listener after its dungeon registrations: the order is by construction, not by the order of two lines in a constructor.
     * The listener at the old place stays (a second registration of the same renderer is harmless).
     */
    public static void registerLooks(EntityRenderersEvent.RegisterRenderers e) {
        if (CorpseClientConfig.classicLooks()) return;
        e.registerEntityRenderer(CorpseEntities.JIMMY.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.ALFREDO.get(), RadesUndeadRenderer::new);
        e.registerEntityRenderer(CorpseEntities.CARL.get(), RadesUndeadRenderer::new);
    }

    /**
     * Writes one client-log line per numbered undead naming the renderer that really stays after every registration (the event that fires after all of them), so "which look did I get" is a
     * line in latest.log: info when it is the new look (or {@code corpse.classicLooks} asks for the old one), error when another renderer won.
     */
    static void report(EntityRenderersEvent.AddLayers e) {
        for (NamedCorpses k : NamedCorpses.values()) {
            EntityRenderer<?> r = e.getRenderer(CorpseEntities.type(k));
            String cls = r == null ? "none" : r.getClass().getName();
            boolean ours = r instanceof RadesUndeadRenderer;
            if (ours || CorpseClientConfig.classicLooks()) LOG.info("[nusmp] numbered undead renderer: {} is drawn by {}{}", k.id, cls, ours ? "" : " (corpse.classicLooks asks for the old look)");
            else LOG.error("[nusmp] numbered undead renderer: {} is drawn by {}, not by the new look (RadesUndeadRenderer): another registration won", k.id, cls);
        }
    }

    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        try {
            report(e);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] numbered undead renderer report failed: {}", ex.toString());
        }
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
