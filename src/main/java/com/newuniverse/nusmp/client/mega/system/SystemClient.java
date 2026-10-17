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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one client entry point of the System, so the lead adds ONE line to the client block of the mod class and the rest wires itself, the way
 * {@code PageUnlockOverlay.init} and {@code SongClient.init} do. It (1) registers the window as a GUI layer above everything with {@code RegisterGuiLayersEvent}
 * (NeoForge 1.21's replacement of {@code IGuiOverlay}), (2) puts the keys in the Controls screen under their own category "Multiverse: System" (1.58.0): the toggle
 * key (default Y, hides or shows the window for this session; the client config's {@code enabled} is the lasting switch) and the keys that MOVE the window
 * (edit mode on U, the arrow keys to nudge, Home to reset, End to change the corner; see {@link SystemEditMode}), (3) registers the client config file, (4) forgets the
 * picture and ends edit mode when the player leaves the world, (5) feeds edit mode its keys every tick and its mouse events (a drag works while a screen is open),
 * draws the edit-mode window over open screens, and (6) receives the two payloads: {@link #receive} and {@link #receiveToast} are what {@code SystemNet} calls on the
 * client thread. Client only: it is reached from the client block of the mod class and, by name inside a lambda, from the payload registration, so a dedicated server
 * never loads it.
 *
 * <p>The defaults were chosen against every key the game and this mod bind: U, Home and End are unused by vanilla 1.21.1 and by the mod; the arrow keys are unused
 * outside screens; R (the Use Relic key of the mod) and C (vanilla's hotbar-save key) were rejected for Reset and Corner because pressing them in edit mode would also
 * fire a relic or clash in the Controls list.
 */
public final class SystemClient {
    private SystemClient() {}

    /** The Controls category of the System's keys: "Multiverse: System". */
    public static final String CATEGORY = SystemText.KEY_CATEGORY;

    /** Toggle the System window (default Y). */
    public static final KeyMapping TOGGLE = new KeyMapping(SystemText.KEY_TOGGLE, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_Y, CATEGORY);
    /** Toggle edit mode, in which the window can be moved (default U). */
    public static final KeyMapping EDIT = new KeyMapping(SystemText.KEY_EDIT, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_U, CATEGORY);
    /** Edit mode: nudge the window one pixel (ten with Shift); the arrow keys. */
    public static final KeyMapping LEFT = new KeyMapping(SystemText.KEY_LEFT, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, CATEGORY);
    public static final KeyMapping RIGHT = new KeyMapping(SystemText.KEY_RIGHT, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, CATEGORY);
    public static final KeyMapping UP = new KeyMapping(SystemText.KEY_UP, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_UP, CATEGORY);
    public static final KeyMapping DOWN = new KeyMapping(SystemText.KEY_DOWN, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN, CATEGORY);
    /** Edit mode: put the window back in its default place (default Home). */
    public static final KeyMapping RESET = new KeyMapping(SystemText.KEY_RESET, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_HOME, CATEGORY);
    /** Edit mode: move the window to the next corner (default End). */
    public static final KeyMapping CORNER = new KeyMapping(SystemText.KEY_CORNER, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_END, CATEGORY);

    /** Every mapping of the System, in the order of the Controls list. */
    static final KeyMapping[] KEYS = {TOGGLE, EDIT, LEFT, RIGHT, UP, DOWN, RESET, CORNER};

    /** Call once from the client section of the mod constructor. Registers the client config too, so that is the only line needed. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SystemClientConfig.SPEC, "nusmp-labyrinth-mega-system-client.toml");
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            for (KeyMapping k : KEYS) e.register(k);
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "system_overlay"), SystemOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            SystemEditMode.leave();
            ClientSystem.clear();
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> SystemEditMode.onMousePressed(e));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseDragged.Pre e) -> SystemEditMode.onMouseDragged(e));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonReleased.Pre e) -> SystemEditMode.onMouseReleased(e));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> SystemOverlay.renderOnScreen(e.getGuiGraphics(), e.getPartialTick()));
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
        SystemEditMode.tick(mc);
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
