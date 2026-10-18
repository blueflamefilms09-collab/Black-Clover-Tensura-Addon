package com.newuniverse.nusmp.client.labyrinth.foundry;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Why: the client half of the Mob Foundry, called by one line in {@code LabyrinthClient}: it registers the ONE renderer of the ONE entity type ({@link FoundryRenderer}) and the
 * reload listener that reads {@code assets/nusmp/foundry/index.json} (which animation file, glow colour and size each of the 400 mobs uses) every time resources reload. A missing
 * or broken index is logged and leaves the table empty: the mobs are then not drawn, nothing crashes.
 */
public final class FoundryClient {
    private FoundryClient() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation INDEX = ResourceLocation.fromNamespaceAndPath("nusmp", "foundry/index.json");

    public static void init(IEventBus modBus) {
        modBus.addListener(FoundryClient::renderers);
        modBus.addListener(FoundryClient::reloadListeners);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(FoundryRegistry.MOB.get(), FoundryRenderer::new);
    }

    private static void reloadListeners(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((ResourceManagerReloadListener) FoundryClient::load);
    }

    private static void load(ResourceManager manager) {
        try {
            Optional<Resource> res = manager.getResource(INDEX);
            if (res.isEmpty()) {
                LOG.warn("[nusmp] {} is missing: the foundry mobs are not drawn", INDEX);
                FoundryIndex.install(java.util.Map.of());
                return;
            }
            try (InputStream in = res.get().open()) {
                FoundryIndex.install(FoundryIndex.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            }
            LOG.info("[nusmp] Mob Foundry client: {} models in the index", FoundryIndex.size());
        } catch (Exception ex) {
            LOG.warn("[nusmp] the foundry index could not be read: {}", ex.toString());
            FoundryIndex.install(java.util.Map.of());
        }
    }
}
