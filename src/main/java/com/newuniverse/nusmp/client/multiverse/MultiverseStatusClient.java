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
    /** 0.48: screens where only the placeholder text was found: a "Four Kingdoms" label sits in the text's own space instead. */
    private static final Map<Screen, int[]> LABELS = new WeakHashMap<>();

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

    /** True while a target screen is open and has no panel yet (the text hook only looks then). */
    public static boolean watching() {
        Screen s = Minecraft.getInstance().screen;
        return target(s) && !PANELS.containsKey(s);                 // with a label the text stays hidden every frame
    }

    /**
     * Called by the text-draw hook (mixin.client.ComingSoonTextMixin) for text drawn while a target screen is open. If it is the
     * placeholder text ("Coming Soon"), the text is skipped and the status panel takes its spot: a panel centred on where the
     * text was, at least 130 x 56. Returns true to skip drawing the text.
     */
    public static boolean placeholderText(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y) {
        try {
            return placeholderTextUnsafe(g, font, text, x, y);
        } catch (RuntimeException e) {
            warnOnce(e);
            return false;                        // never break another mod's screen: just draw the text normally
        }
    }

    private static boolean placeholderTextUnsafe(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y) {
        Screen s = Minecraft.getInstance().screen;
        if (!target(s)) return false;
        String want = MultiverseClientConfig.get(MultiverseClientConfig.PANEL_TEXT).toLowerCase();
        if (want.isEmpty() || !text.trim().toLowerCase().equals(want)) return false;
        if (PANELS.containsKey(s)) return true;
        // 0.48 fix: the placeholder text alone gives no room (a 130 x 56 panel there covered the preset slots and icons), so a small
        // "Four Kingdoms" label takes exactly the text's place and opens the menu (which has the Status button).
        org.joml.Matrix4f m = g.pose().last().pose();
        org.joml.Vector4f a = m.transform(new org.joml.Vector4f(x, y, 0, 1));
        org.joml.Vector4f b = m.transform(new org.joml.Vector4f(x + font.width(text), y + font.lineHeight, 0, 1));
        LABELS.put(s, new int[]{(int) Math.min(a.x(), b.x()), (int) Math.min(a.y(), b.y()), (int) Math.max(1, Math.abs(b.x() - a.x())),
                (int) Math.max(1, Math.abs(b.y() - a.y()))});
        return true;
    }

    private static boolean target(Screen s) {
        String prefix = MultiverseClientConfig.get(MultiverseClientConfig.SCREEN_PREFIX);
        return s != null && !(s instanceof MultiverseStatusScreen) && !prefix.isEmpty() && s.getClass().getName().startsWith(prefix);
    }

    private static boolean warned;

    /** Our hooks sit inside other mods' screens: a surprise there is logged once and the hook steps aside, never a crash. */
    private static void warnOnce(RuntimeException e) {
        if (warned) return;
        warned = true;
        com.mojang.logging.LogUtils.getLogger().warn("[nusmp] Multiverse status panel skipped a screen it could not read", e);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        try { onScreenInitUnsafe(event); } catch (RuntimeException e) { warnOnce(e); }
    }

    private static void onScreenInitUnsafe(ScreenEvent.Init.Post event) {
        Screen s = event.getScreen();
        PANELS.remove(s);
        LABELS.remove(s);
        if (!target(s)) return;
        int[] rect = parseRect(MultiverseClientConfig.get(MultiverseClientConfig.PANEL_RECT));
        if (rect == null) {
            String text = MultiverseClientConfig.get(MultiverseClientConfig.PANEL_TEXT).toLowerCase();
            for (GuiEventListener l : event.getListenersList()) {
                if (!(l instanceof AbstractWidget w) || text.isEmpty()) continue;
                Component msg = w.getMessage();          // some mods' widgets have no message at all (null): skip them
                if (msg != null && msg.getString().toLowerCase().contains(text)) {
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
        try { onScreenRenderUnsafe(event); } catch (RuntimeException e) { warnOnce(e); }
    }

    private static void onScreenRenderUnsafe(ScreenEvent.Render.Post event) {
        int[] l = LABELS.get(event.getScreen());
        if (l != null) {                                              // the label: "Four Kingdoms", scaled into the old text's box
            GuiGraphics g = event.getGuiGraphics();
            var font = Minecraft.getInstance().font;
            String label = "Four Kingdoms";
            float sc = Math.min(1f, Math.min(l[2] / (float) font.width(label), l[3] / (float) font.lineHeight));
            boolean hover = inside(l, event.getMouseX(), event.getMouseY());
            g.pose().pushPose();
            g.pose().translate(l[0] + (l[2] - font.width(label) * sc) / 2f, l[1] + (l[3] - font.lineHeight * sc) / 2f, 200);
            g.pose().scale(sc, sc, 1f);
            g.drawString(font, label, 0, 0, hover ? 0xFFFFE9A8 : 0xFFE8C468, true);
            g.pose().popPose();
            return;
        }
        int[] r = PANELS.get(event.getScreen());
        if (r == null) return;
        GuiGraphics g = event.getGuiGraphics();
        MultiverseStatusScreen.drawCompact(g, Minecraft.getInstance().font, r[0], r[1], r[2], r[3], status,
                inside(r, event.getMouseX(), event.getMouseY()));
    }

    private static void onScreenClick(ScreenEvent.MouseButtonPressed.Pre event) {
        try { onScreenClickUnsafe(event); } catch (RuntimeException e) { warnOnce(e); }
    }

    private static void onScreenClickUnsafe(ScreenEvent.MouseButtonPressed.Pre event) {
        int[] l = LABELS.get(event.getScreen());
        if (l != null && event.getButton() == 0 && inside(l, event.getMouseX(), event.getMouseY())) {
            Minecraft.getInstance().setScreen(new FourKingdomsScreen(event.getScreen()));
            event.setCanceled(true);
            return;
        }
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
