package com.newuniverse.nusmp.client.ui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The Anime Multiverse theme: the palette, the mode switch and the shared clock. {@link #anime()} is the one question every screen asks:
 * true draws the new look, false runs the old drawing code exactly as it was.
 *
 * <h3>Palette</h3>
 * Deep blue-black translucent panels, cyan as the "analysis" accent, violet for magic, the mod's gold for names and headings, white-blue
 * text. All ARGB ints; panels are never opaque, so the world (or the dimmed screen) stays visible through them.
 */
public final class UiTheme {
    private UiTheme() {}

    public static final int PANEL_TOP = 0xCC111C34, PANEL_BOTTOM = 0xCC080D1B, PANEL_DEEP = 0xD9060912;
    public static final int CYAN = 0xFF5CD8FF, VIOLET = 0xFF9A7CFF, GOLD = 0xFFE8C468, GREEN = 0xFF7CF0B0, RED = 0xFFFF6A6A, WHITE = 0xFFFFFFFF;
    public static final int TEXT = 0xFFE6F2FF, TEXT_DIM = 0xFF8FA3BF, TEXT_FAINT = 0xFF5C6E88, SAGE = 0xFFBFEFFF, SAGE_HEAD = 0xFF5CD8FF;

    private static final long T0 = System.nanoTime();

    /** True when the anime look is on (the config {@code ui.theme}). */
    public static boolean anime() { return UiThemeOptions.mode() == UiThemeMode.ANIME; }

    /** True when animation is allowed ({@code ui.motion}). */
    public static boolean motion() { return UiThemeOptions.motion(); }

    /**
     * The shared animation clock in ticks, continuous and independent of the game being paused (a screen can animate while the game stands
     * still). With motion off it is frozen at a fixed pleasant frame, so circles and bars stand still.
     */
    public static float now() {
        if (!motion()) return 40f;
        return (System.nanoTime() - T0) / 50_000_000f;
    }

    /** The same clock but never frozen (timers that must run out, e.g. a toast, use this). */
    public static float realNow() { return (System.nanoTime() - T0) / 50_000_000f; }

    /** A translatable line in the Great Sage's voice, e.g. {@code sage("nusmp.ui.sage.result", rank)}. */
    public static Component sage(String key, Object... args) { return Component.translatable(key, args); }

    /** The current font (menus may call this when they hold no font reference). */
    public static Font font() { return Minecraft.getInstance().font; }
}
