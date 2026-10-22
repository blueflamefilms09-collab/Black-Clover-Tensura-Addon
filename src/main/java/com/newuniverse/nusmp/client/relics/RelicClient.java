package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
        com.newuniverse.nusmp.client.relics.worn.WornRenderers.init();                  // the per-relic worn renderers (the boss relics)
        com.newuniverse.nusmp.client.relics.worn.lib.WornLibClient.init(modBus);         // 1.29.0: the worn model library, its reload hook and the ambient particles
        com.newuniverse.nusmp.client.relics.item.ItemFxClient.init(modBus);             // relic bow / crossbow item properties, holder tracking, state variants
        modBus.addListener(RelicActKeys::register);
        modBus.addListener(RelicTooltip::factories);
        modBus.addListener(RelicClient::addLayers);
        modBus.addListener((RegisterClientReloadListenersEvent e) ->
                e.registerReloadListener((ResourceManagerReloadListener) mgr -> RelicTextures.registerAll()));          // a reload wipes the painted textures
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_hud"), (graphics, delta) -> RelicActKeys.renderHud(graphics)));
        NeoForge.EVENT_BUS.addListener(RelicActKeys::onClientTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> RelicTextures.onClientTick());      // 1.111.0: the painted armour layers are checked at the first world frame and every ten seconds, and painted again when one is gone
        NeoForge.EVENT_BUS.addListener(RelicTooltip::onGather);
        NeoForge.EVENT_BUS.addListener(RelicTooltip::onScroll);
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.client.relics.worn.WornDebugCommand::register);          // 1.110.0: /nusmp debug worn
        NeoForge.EVENT_BUS.addListener(RelicWearClient::onClientTick);                 // relics_wear: a fresh look at the local player while a screen is open (the preview and the paper doll)
        NeoForge.EVENT_BUS.addListener(RelicWearClient::onLeave);                     // relics_wear: the server's list for an entity is dropped when the entity leaves the client's level
        NeoForge.EVENT_BUS.addListener(RelicWearClient::registerCommands);            // relics_wear: /multiverse relic show <on|off|toggle> (relicfx.showWorn)
        NeoForge.EVENT_BUS.addListener(RelicLayerGuard::onPre);                      // 1.99.0: attaches the worn layer to a player renderer the layer event never reached
        NeoForge.EVENT_BUS.addListener(RelicGradeFx::onEntityTick);
        NeoForge.EVENT_BUS.addListener(RelicGradeFx::onClientTick);
        com.newuniverse.nusmp.client.ex.ExSingularityClient.init(modBus);                  // EX-Singularity (above Genesis): the slot frame and the motes (docs/relics/ex_singularity.md)
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> RelicTextures.registerAll());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            RelicClientState.clear();
            com.newuniverse.nusmp.client.relics.worn.WornCues.clear();
            com.newuniverse.nusmp.client.relics.worn.ArmorTrace.clear();                   // 1.111.0: what was really drawn for the armour relics
            com.newuniverse.nusmp.client.relics.worn.WornTrace.clear();                    // 1.99.0: what the worn layer last did, and what the server said wearers carry
            com.newuniverse.nusmp.relics.WornSync.clearClient();
            RelicWearClient.onLogout();
            Relics.clearCaches();
        });
    }

    /** How many player renderers got the worn layer in the last renderer (re)build, and which skins were refused (for {@code /multiverse relic debug}). */
    private static volatile int layersAdded;
    private static volatile String layersRefused = "";

    private static volatile int humanoidLayers;

    public static int layersAdded() { return layersAdded; }

    /** How many non-player humanoid renderers got {@link HumanoidRelicLayer} in the last renderer build. */
    public static int humanoidLayers() { return humanoidLayers; }

    private static <E extends Entity> boolean addHumanoid(EntityRenderersEvent.AddLayers event, EntityType<E> type) {
        try {
            if (event.getRenderer(type) instanceof LivingEntityRenderer<?, ?> r && !(r instanceof PlayerRenderer) && r.getModel() instanceof HumanoidModel<?>) {
                attachHumanoid(r);
                return true;
            }
        } catch (RuntimeException | LinkageError oddRenderer) {
            // a renderer that cannot take a layer is left as it is
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void attachHumanoid(LivingEntityRenderer renderer) { renderer.addLayer(new HumanoidRelicLayer(renderer)); }

    public static String layersRefused() { return layersRefused; }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        int added = 0;
        StringBuilder refused = new StringBuilder();
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer r) {
                r.addLayer(new RelicWornLayer(r));
                RelicLayerGuard.known(r);
                added++;
            } else {                                                                    // 1.99.0: said out loud; this used to be a silent "no worn relics on that skin type"
                refused.append(skin).append('=').append(renderer == null ? "null" : renderer.getClass().getName()).append(' ');
            }
        }
        layersAdded = added;
        layersRefused = refused.toString().trim();
        int humanoids = 0;
        for (EntityType<?> type : event.getEntityTypes()) {                           // 1.99.0: armour stands, zombies, skeletons, piglins, humanoid mobs of other mods: a relic helmet or pair of boots has a blank painted box, so they need this layer
            if (type != EntityType.PLAYER && addHumanoid(event, type)) humanoids++;
        }
        humanoidLayers = humanoids;
        if (refused.length() > 0) com.mojang.logging.LogUtils.getLogger().error("[nusmp] the worn relic layer was NOT added to: {} (only PlayerRenderer instances take it)", layersRefused);
        else com.mojang.logging.LogUtils.getLogger().info("[nusmp] the worn relic layer was added to {} player renderers and {} humanoid renderers", added, humanoids);
    }
}
