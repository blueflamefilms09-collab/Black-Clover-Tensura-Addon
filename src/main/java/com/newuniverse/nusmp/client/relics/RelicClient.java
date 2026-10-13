package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the relics, registered in one call from the client section of the mod constructor: {@code RelicClient.init(modEventBus)}.
 * The key and its HUD line, the tooltip, the run-time textures of the worn armour, the Curios worn layer on both player skins, and the
 * particles that mark a God or Genesis relic.
 */
public final class RelicClient {
    private RelicClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(RelicActKeys::register);
        modBus.addListener(RelicTooltip::factories);
        modBus.addListener(RelicClient::addLayers);
        modBus.addListener((RegisterClientReloadListenersEvent e) ->
                e.registerReloadListener((ResourceManagerReloadListener) mgr -> RelicTextures.registerAll()));          // a reload wipes the painted textures
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_hud"), (graphics, delta) -> RelicActKeys.renderHud(graphics)));
        NeoForge.EVENT_BUS.addListener(RelicActKeys::onClientTick);
        NeoForge.EVENT_BUS.addListener(RelicTooltip::onGather);
        NeoForge.EVENT_BUS.addListener(RelicTooltip::onScroll);
        NeoForge.EVENT_BUS.addListener(RelicGradeFx::onEntityTick);
        NeoForge.EVENT_BUS.addListener(RelicGradeFx::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> RelicTextures.registerAll());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            RelicClientState.clear();
            Relics.clearCaches();
        });
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer r) r.addLayer(new RelicWornLayer(r));
        }
    }
}
