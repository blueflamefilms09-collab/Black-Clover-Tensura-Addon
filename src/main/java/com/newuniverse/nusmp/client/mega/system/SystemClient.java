package com.newuniverse.nusmp.client.mega.system;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemNet;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one client entry point of the System, so the lead adds ONE line to the client block of the mod class and the rest wires itself, the way
 * {@code PageUnlockOverlay.init} and {@code SongClient.init} do. It (1) registers the window as a GUI layer above everything with {@code RegisterGuiLayersEvent}
 * (NeoForge 1.21's replacement of {@code IGuiOverlay}), (2) puts the toggle key in the Controls screen (default Y, under "Multiverse of Anime"; it hides or shows
 * the window for this session, and the client config's {@code enabled} is the lasting switch), (3) registers the client config file, (4) forgets the picture when
 * the player leaves the world, and (5) receives the two payloads: {@link #receive} and {@link #receiveToast} are what {@code SystemNet} calls on the client
 * thread. Client only: it is reached from the client block of the mod class and, by name inside a lambda, from the payload registration, so a dedicated server
 * never loads it.
 */
public final class SystemClient {
    private SystemClient() {}

    /** Toggle the System window (default Y). */
    public static final KeyMapping TOGGLE = new KeyMapping("key.nusmp.system_toggle", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_Y, "key.categories.nusmp");

    /** Call once from the client section of the mod constructor. Registers the client config too, so that is the only line needed. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SystemClientConfig.SPEC, "nusmp-labyrinth-mega-system-client.toml");
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(TOGGLE));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "system_overlay"), SystemOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientSystem.clear());
    }

    private static void tick() {
        while (TOGGLE.consumeClick()) {
            boolean shown = ClientSystem.toggle();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String key = shown ? SystemText.WINDOW_SHOWN : SystemText.WINDOW_HIDDEN;
                mc.player.displayClientMessage(Component.translatableWithFallback(key, SystemText.english(key)), true);
            }
        }
    }

    /** The server's picture of the window (called on the client thread). */
    public static void receive(SystemNet.Sync p) {
        Minecraft mc = Minecraft.getInstance();
        ClientSystem.apply(p, mc.level == null ? 0L : mc.level.getGameTime());
    }

    /** One pop-up line (called on the client thread); dropped at once when the player switched pop-ups off, so none waits to appear later. */
    public static void receiveToast(SystemNet.Toast p) {
        if (SystemClientConfig.toasts()) ClientSystem.pushToast(p);
    }
}
