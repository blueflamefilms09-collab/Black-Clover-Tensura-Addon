package com.newuniverse.nusmp.client.elite.records;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.records.RecordsNet;
import com.newuniverse.nusmp.elite.records.RecordsText;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Why: the one client entry point of the records system, started from {@code RecordsSystem.init} on a client only. It registers the key that opens the Records screen (default I, rebindable under
 * Controls), the toast overlay, the menu button ({@link RecordsMenuInjection}, the same pattern as the Skill Tree's button but a separate class that touches no line of it) and the cleanup on logout (the
 * synced definitions and state belong to one world and must not leak into the next). Opening the screen asks the server for a fresh state; the server answers, and the screen draws what arrived.
 */
public final class RecordsClient {
    private RecordsClient() {}

    private static final String CATEGORY = "key.categories.nusmp";
    public static final KeyMapping OPEN = new KeyMapping(RecordsText.KEY_OPEN, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "records_toast"), RecordToastOverlay::render));
        NeoForge.EVENT_BUS.addListener(RecordsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            RecordsClientState.reset();
            RecordToastOverlay.reset();
            RecordsMenuInjection.forget();
        });
        RecordsMenuInjection.init();
    }

    private static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        while (OPEN.consumeClick()) if (mc.screen == null && !mc.player.isSpectator()) open(null);
    }

    /** Opens the screen over {@code parent} (null = the game) and asks the server for the state. */
    public static void open(net.minecraft.client.gui.screens.Screen parent) {
        request();
        Minecraft.getInstance().setScreen(new RecordsScreen(parent));
    }

    static void request() {
        try {
            PacketDistributor.sendToServer(new RecordsNet.Request());
        } catch (RuntimeException ignored) {
            // not connected to a server that has the system: the screen stays on "waiting"
        }
    }
}
