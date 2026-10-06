package com.newuniverse.nusmp.client.multiverse;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side of the Multiverse status panel. Holds the latest status the server sent (read-only), and puts the panel into the
 * Tensura status / magic menu: the placeholder widget reading "Coming Soon" is hidden and the compact panel is drawn in its place
 * (click it for the full status screen). Without such a widget a small "Multiverse" button is added instead, and a configured
 * rectangle always wins. The "Multiverse Status" key (unbound by default) opens the full screen anywhere.
 */
public final class MultiverseStatusClient {
    public static final KeyMapping KEY = new KeyMapping("key.nusmp.multiverse_status", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.nusmp");

    private static CompoundTag status = new CompoundTag();
    /** Where the compact panel goes on each open screen. */
    private static final Map<Screen, int[]> PANELS = new WeakHashMap<>();

    private MultiverseStatusClient() {}

    public static CompoundTag status() { return status; }

    /** Network handler (client thread). */
    public static void receive(CompoundTag tag) { status = tag; }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (KEY.consumeClick()) Minecraft.getInstance().setScreen(new MultiverseStatusScreen(Minecraft.getInstance().screen));
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> status = new CompoundTag());
        NeoForge.EVENT_BUS.addListener(MultiverseStatusClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(MultiverseStatusClient::onScreenRender);
        NeoForge.EVENT_BUS.addListener(MultiverseStatusClient::onScreenClick);
    }

    private static boolean target(Screen s) {
        String prefix = MultiverseClientConfig.get(MultiverseClientConfig.SCREEN_PREFIX);
        return s != null && !(s instanceof MultiverseStatusScreen) && !prefix.isEmpty() && s.getClass().getName().startsWith(prefix);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen s = event.getScreen();
        PANELS.remove(s);
        if (!target(s)) return;
        int[] rect = parseRect(MultiverseClientConfig.get(MultiverseClientConfig.PANEL_RECT));
        if (rect == null) {
            String text = MultiverseClientConfig.get(MultiverseClientConfig.PANEL_TEXT).toLowerCase();
            for (GuiEventListener l : event.getListenersList()) {
                if (l instanceof AbstractWidget w && !text.isEmpty() && w.getMessage().getString().toLowerCase().contains(text)) {
                    rect = new int[]{w.getX(), w.getY(), w.getWidth(), w.getHeight()};
                    w.visible = false;           // the panel takes the placeholder's place
                    w.active = false;
                    break;
                }
            }
        }
        if (rect != null && rect[2] >= 60 && rect[3] >= 30) {
            PANELS.put(s, rect);
            return;
        }
        if (MultiverseClientConfig.get(MultiverseClientConfig.BUTTON_FALLBACK)) {
            event.addListener(Button.builder(Component.literal("Multiverse"), b -> Minecraft.getInstance().setScreen(new MultiverseStatusScreen(s)))
                    .bounds(s.width - 84, 4, 80, 16).build());
        }
    }

    private static void onScreenRender(ScreenEvent.Render.Post event) {
        int[] r = PANELS.get(event.getScreen());
        if (r == null) return;
        GuiGraphics g = event.getGuiGraphics();
        MultiverseStatusScreen.drawCompact(g, Minecraft.getInstance().font, r[0], r[1], r[2], r[3], status,
                inside(r, event.getMouseX(), event.getMouseY()));
    }

    private static void onScreenClick(ScreenEvent.MouseButtonPressed.Pre event) {
        int[] r = PANELS.get(event.getScreen());
        if (r == null || event.getButton() != 0 || !inside(r, event.getMouseX(), event.getMouseY())) return;
        Minecraft.getInstance().setScreen(new MultiverseStatusScreen(event.getScreen()));
        event.setCanceled(true);
    }

    private static boolean inside(int[] r, double x, double y) { return x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3]; }

    private static int[] parseRect(String s) {
        if (s == null || s.isBlank()) return null;
        String[] p = s.split(",");
        if (p.length != 4) return null;
        try {
            return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim())};
        } catch (NumberFormatException e) { return null; }
    }
}
