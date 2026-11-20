package com.newuniverse.nusmp.client.hub;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.client.ui.CodexScreen;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the client entry point of the Multiverse Hub (docs/ui_hub.md). It holds the hub key, the one question every old entry point now asks ({@link #menu}: the hub when {@code ui.hub} is on, the old Codex book
 * when it is off, exactly as before), and the way back from a screen that has no parent of its own (Tensura's skill screens): the hub remembers the screen it opened and, when that screen closes to the game, opens the
 * hub again. It adds no tick handler of its own: the key is polled from the status key's existing client tick ({@link #pollKey}, called by {@code MultiverseStatusClient}). Everything here is client only and keeps no
 * state about other players; it forgets the pending return at logout.
 */
public final class MultiverseHubClient {
    private MultiverseHubClient() {}

    /**
     * The hub key. The first plan was H, but H is the default of the Djinn's Rukh key ({@code DjinnKeys.RUKH}) and every letter is taken by this mod or the game, so the default is F7 (next to the GUI Size
     * key F8 and the GUI Move key F9); rebind it under Controls > Multiverse of Anime. It does nothing while {@code ui.hub} is false.
     */
    public static final KeyMapping KEY = new KeyMapping("key.anime_multiverse.multiverse_hub", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_F7, "key.categories.anime_multiverse");

    /** The screen the hub opened that has no parent (so its Esc would drop to the game), and the hub to come back to. */
    private static Screen watched;
    private static Screen back;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        NeoForge.EVENT_BUS.addListener(MultiverseHubClient::onClosing);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> forget());
    }

    /** The screen an old entry point (the Multiverse Status key, the Four Kingdoms slot, the status panel) opens: the hub, or the old Codex book when {@code ui.hub} is false. */
    public static Screen menu(Screen parent) {
        return UiThemeOptions.hub() ? new MultiverseHubScreen(parent) : new CodexScreen(parent);
    }

    /**
     * Hub v2: the screen every old "open the Codex book" entry point opens, on the Codex page whose tab title is {@code startTitle} (null = the first page): the hub on its Codex page when {@code ui.hub} is on,
     * the old Codex book on that tab when it is off, exactly as before.
     */
    public static Screen openCodex(Screen parent, String startTitle) {
        return UiThemeOptions.hub() ? new MultiverseHubScreen(parent).startOnCodex(startTitle) : new CodexScreen(parent).startOn(startTitle);
    }

    /** Called every client tick by the status key's handler: opens the hub on the hub key (only with {@code ui.hub} on, in a world). */
    public static void pollKey() {
        boolean pressed = false;
        while (KEY.consumeClick()) pressed = true;
        Minecraft mc = Minecraft.getInstance();
        if (pressed && mc.player != null && mc.screen == null && UiThemeOptions.hub()) mc.setScreen(new MultiverseHubScreen(null));
    }

    // ---------------------------------------------------------------- the way back
    /** Opens {@code target} and, when it closes to the game, opens {@code hub} again. */
    static void openAndReturn(Screen target, Screen hub) {
        watched = target;
        back = hub;
        Minecraft.getInstance().setScreen(target);
    }

    private static void forget() {
        watched = null;
        back = null;
    }

    private static void onClosing(ScreenEvent.Closing e) {
        if (watched == null || e.getScreen() != watched) return;
        Screen hub = back;
        forget();
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            if (mc.player == null || hub == null) return;
            if (mc.screen == null) mc.setScreen(hub);                                  // it closed to the game: back to the hub
            else if (mc.screen != hub) {                                               // it moved on to another screen of the chain (a sub-menu): keep following that one
                watched = mc.screen;
                back = hub;
            }
        });
    }
}
