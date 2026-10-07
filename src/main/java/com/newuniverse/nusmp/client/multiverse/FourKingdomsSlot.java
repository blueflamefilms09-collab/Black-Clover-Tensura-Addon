package com.newuniverse.nusmp.client.multiverse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 0.46: the Four Kingdoms button in Tensura's magic screen. Tensura's ability-categories screen (the Magic tab's hexagon of
 * category icons) draws one slot with its shared "coming soon" icon, {@code ScreenHelper.COMING_SOON_ICON} (the grey "?").
 * The blit hook (mixin.client.FourKingdomsSlotMixin) catches exactly that texture being drawn while that screen is open, draws
 * the Four Kingdoms logo in its place and records where it went; clicking there opens {@link FourKingdomsScreen}, and hovering
 * shows a tooltip. Every other draw and click in the screen is Tensura's own. If Tensura is missing or changes, nothing happens.
 */
public final class FourKingdomsSlot {
    private FourKingdomsSlot() {}

    static final String SCREEN = "io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen";
    private static final Map<Screen, int[]> SLOTS = new WeakHashMap<>();
    private static ResourceLocation comingSoon;
    private static boolean looked;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(FourKingdomsSlot::onClick);
        NeoForge.EVENT_BUS.addListener(FourKingdomsSlot::onRenderPost);
    }

    /** Tensura's "coming soon" icon (read once from ScreenHelper), or null if it can't be found. */
    static ResourceLocation comingSoon() {
        if (!looked) {
            looked = true;
            try {
                Object v = Class.forName("io.github.manasmods.tensura.util.client.ScreenHelper").getField("COMING_SOON_ICON").get(null);
                if (v instanceof ResourceLocation rl) comingSoon = rl;
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        return comingSoon;
    }

    static boolean onTensuraScreen() {
        Screen s = Minecraft.getInstance().screen;
        return s != null && s.getClass().getName().equals(SCREEN);
    }

    /**
     * Called by the blit hook for every textured quad drawn. If it is the "coming soon" icon on Tensura's categories screen,
     * draws the logo there instead, remembers the slot (in screen pixels) and returns true so the original is skipped.
     */
    public static boolean replace(GuiGraphics g, ResourceLocation tex, int x1, int x2, int y1, int y2) {
        try {
            ResourceLocation cs = comingSoon();
            if (cs == null || !cs.equals(tex) || !onTensuraScreen()) return false;
            int w = x2 - x1, h = y2 - y1;
            g.blit(FourKingdomsScreen.LOGO, x1, y1, w, h, 0, 0, 256, 256, 256, 256);
            org.joml.Matrix4f m = g.pose().last().pose();
            org.joml.Vector4f a = m.transform(new org.joml.Vector4f(x1, y1, 0, 1)), b = m.transform(new org.joml.Vector4f(x2, y2, 0, 1));
            SLOTS.put(Minecraft.getInstance().screen, new int[]{(int) Math.min(a.x(), b.x()), (int) Math.min(a.y(), b.y()),
                    (int) Math.abs(b.x() - a.x()), (int) Math.abs(b.y() - a.y())});
            return true;
        } catch (RuntimeException e) {
            return false;                                                   // never break Tensura's screen
        }
    }

    static boolean inside(int[] r, double x, double y) { return r != null && x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3]; }

    private static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        int[] r = SLOTS.get(e.getScreen());
        if (e.getButton() != 0 || !inside(r, e.getMouseX(), e.getMouseY())) return;
        Minecraft.getInstance().setScreen(new FourKingdomsScreen(e.getScreen()));
        e.setCanceled(true);
    }

    private static void onRenderPost(ScreenEvent.Render.Post e) {
        int[] r = SLOTS.get(e.getScreen());
        if (!inside(r, e.getMouseX(), e.getMouseY())) return;
        GuiGraphics g = e.getGuiGraphics();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x30FFFFFF);          // hover highlight
        g.renderTooltip(Minecraft.getInstance().font, Component.literal("Four Kingdoms"), e.getMouseX(), e.getMouseY());
    }
}
