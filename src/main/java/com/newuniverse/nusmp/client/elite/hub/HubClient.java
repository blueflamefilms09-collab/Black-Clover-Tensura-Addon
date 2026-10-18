package com.newuniverse.nusmp.client.elite.hub;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.elite.hub.HubMenus;
import com.newuniverse.nusmp.elite.hub.HubNet;
import com.newuniverse.nusmp.elite.hub.HubNpcs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Why: the one client entry point of the Hub economy (1.83.0), reached by name from {@code HubRegistry.init} inside a client-only branch, so a dedicated server never loads it. It (1) registers the
 * two menu screens (a menu type with no screen crashes the client the moment the NPC is clicked), (2) the two villager-model renderers of the NPCs, (3) the quest tracker as a GUI layer above
 * everything ({@code RegisterGuiLayersEvent}, NeoForge 1.21's replacement of {@code IGuiOverlay}) and its toggle key, and (4) holds what the server last told this client (the overlay's lines, the
 * dialogue's board, the shop's content) as plain static fields written on the client thread by {@link #hud}, {@link #board}, {@link #shop} and {@link #map}, which {@code HubNet}'s handlers call. All
 * of it is forgotten when the player leaves the world.
 */
public final class HubClient {
    private HubClient() {}

    /** Toggle the quest tracker (default apostrophe: no other key of the game or the mod uses it). */
    public static final KeyMapping TOGGLE_TRACKER = new KeyMapping("key.nusmp.hub.toggle_tracker", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_APOSTROPHE, "key.categories.nusmp.hub");

    // what the server last said
    static volatile List<HubNet.HudEntry> hud = List.of();
    static volatile HubNet.BoardSync board = new HubNet.BoardSync(0, List.of());
    static volatile HubNet.ShopSync shop = new HubNet.ShopSync(0, "coins", List.of());
    static volatile boolean trackerShown = true;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(HubMenus.QUEST.get(), QuestScreen::new);
            e.register(HubMenus.SHOP.get(), ShopScreen::new);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(HubNpcs.QUEST_GIVER.get(), ctx -> new HubNpcRenderer(ctx, "quest_giver_npc"));
            e.registerEntityRenderer(HubNpcs.MERCHANT.get(), ctx -> new HubNpcRenderer(ctx, "merchant_npc"));
        });
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(TOGGLE_TRACKER));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "hub_quest_tracker"), QuestOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE_TRACKER.consumeClick()) {
            trackerShown = !trackerShown;
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatableWithFallback(trackerShown ? "message.nusmp.hub.tracker.shown" : "message.nusmp.hub.tracker.hidden",
                        trackerShown ? "Quest tracker shown." : "Quest tracker hidden."), true);
            }
        }
    }

    static void clear() {
        hud = List.of();
        board = new HubNet.BoardSync(0, List.of());
        shop = new HubNet.ShopSync(0, "coins", List.of());
    }

    // ================================================================ from the packets (client thread)
    public static void hud(HubNet.HudSync p) { hud = List.copyOf(p.entries()); }

    public static void board(HubNet.BoardSync p) { board = p; }

    public static void shop(HubNet.ShopSync p) { shop = p; }

    public static void map(HubNet.MapSync p) { Minecraft.getInstance().setScreen(new DungeonMapScreen(p)); }
}
