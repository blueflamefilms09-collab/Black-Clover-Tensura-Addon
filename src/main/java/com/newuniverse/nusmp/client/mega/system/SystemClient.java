package com.newuniverse.nusmp.client.mega.system;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.labyrinth.mega.system.PointsText;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemNet;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemPointsNet;
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
 * (NeoForge 1.21's replacement of {@code IGuiOverlay}), (2) puts the keys in the Controls screen under their own category "Multiverse: System": the toggle key
 * (default Y, hides or shows the window for this session; the client config's {@code enabled} is the lasting switch), the EDIT key (default U: toggles edit mode,
 * which is the {@link SystemEditScreen} where the window is moved and resized with the MOUSE only) and the stat-point key (default I: the {@link SystemStatsScreen}),
 * (3) registers the client config file, (4) forgets the pictures when the player leaves the world, and (5) receives the payloads: {@link #receive},
 * {@link #receivePoints} and {@link #receiveToast} are what {@code SystemNet} / {@code SystemPointsNet} call on the client thread. Client only: it is reached from
 * the client block of the mod class and, by name inside a lambda, from the payload registration, so a dedicated server never loads it.
 *
 * <p>The keys that used to nudge the window (arrows, Home, End) were removed on the owner's request: only the toggle of edit mode is a key now. U, I are unused by
 * vanilla 1.21.1 and by the mod (O is the Curse embrace key, so the stat screen took I).
 */
public final class SystemClient {
    private SystemClient() {}

    /** The Controls category of the System's keys: "Multiverse: System". */
    public static final String CATEGORY = SystemText.KEY_CATEGORY;

    /** Toggle the System window (default Y). */
    public static final KeyMapping TOGGLE = new KeyMapping(SystemText.KEY_TOGGLE, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_Y, CATEGORY);
    /** Toggle edit mode (default U): opens the screen where the window is moved and resized with the mouse; pressed again, it saves and closes. */
    public static final KeyMapping EDIT = new KeyMapping(SystemText.KEY_EDIT, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_U, CATEGORY);
    /** Open the stat point screen (default I). */
    public static final KeyMapping POINTS = new KeyMapping(PointsText.KEY_POINTS, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_I, CATEGORY);

    /** Every mapping of the System, in the order of the Controls list. */
    static final KeyMapping[] KEYS = {TOGGLE, EDIT, POINTS};

    /** Call once from the client section of the mod constructor. Registers the client config too, so that is the only line needed. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SystemClientConfig.SPEC, "nusmp-labyrinth-mega-system-client.toml");
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            for (KeyMapping k : KEYS) e.register(k);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "system_overlay"), SystemOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientSystem.clear();
            ClientPoints.clear();
        });
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE.consumeClick()) {
            boolean shown = ClientSystem.toggle();
            if (mc.player != null) {
                String key = shown ? SystemText.WINDOW_SHOWN : SystemText.WINDOW_HIDDEN;
                mc.player.displayClientMessage(Component.translatableWithFallback(key, SystemText.english(key)), true);
            }
        }
        while (EDIT.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new SystemEditScreen(null));
        }
        while (POINTS.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new SystemStatsScreen(null));
        }
    }

    /** The server's picture of the window (called on the client thread). */
    public static void receive(SystemNet.Sync p) {
        Minecraft mc = Minecraft.getInstance();
        ClientSystem.apply(p, mc.level == null ? 0L : mc.level.getGameTime());
    }

    /** The facts of the stat screen (called on the client thread): the free pool, the cap, respec, the mode and what a point does. */
    public static void receivePoints(SystemPointsNet.PointsSync p) {
        ClientPoints.apply(p);
    }

    /** One pop-up line (called on the client thread); dropped at once when the player switched pop-ups off, so none waits to appear later. */
    public static void receiveToast(SystemNet.Toast p) {
        if (SystemClientConfig.toasts()) ClientSystem.pushToast(p);
    }
}
