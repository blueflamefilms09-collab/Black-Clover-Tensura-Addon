package com.newuniverse.nusmp.shishou;

/**
 * Why: the numbers of Shishou's Akashic GUI (2.1.0) in one pure place: the regions of the texture atlas {@code textures/gui/shishou/akashic.png} (made by {@code tools/shishou/gen_shishou_gui.py}), the colours,
 * the geometry of the Stomach screen and the HUD, and the motion of the light motes. The game's {@code client/shishou/AkashicTheme} draws with these, the preview mock
 * ({@code tools/shishou/shishou_gui_preview.py}) repeats them, and a pure test pins the geometry (every slot inside the panel, the gauges inside their rows, the mote count bounded). No game classes.
 */
public final class ShishouGuiLayout {
    private ShishouGuiLayout() {}

    public static final int ATLAS = 256;

    /** A region of the atlas: u, v, width, height in pixels. */
    public record Region(int u, int v, int w, int h) {}

    public static final Region STAR = new Region(0, 0, 64, 64), EMBLEM = new Region(64, 0, 32, 32), MOTE = new Region(96, 0, 8, 8),
            CORNER_TL = new Region(104, 0, 16, 16), CORNER_TR = new Region(120, 0, 16, 16), CORNER_BL = new Region(136, 0, 16, 16), CORNER_BR = new Region(152, 0, 16, 16),
            PRISM = new Region(0, 64, 128, 2), SLOTROW = new Region(0, 96, 162, 18), GAUGE = new Region(0, 128, 64, 8);

    // ---------------------------------------------------------------- colours (0xRRGGBB)
    public static final int CYAN = 0x5CD8FF, CYAN_HI = 0x7FEBFF, VIOLET = 0x9B6BFF, GOLD = 0xFFD76A, INK_TOP = 0x060818, INK_BOT = 0x1A0F3A, LOW = 0xFF6A6A;
    /** Gauge colours: experience in gold, mana in cyan (a low pool turns red). */
    public static final int EXP_LOW = 0xB8862B, EXP_HIGH = 0xFFE9A0, MANA_LOW = 0x2D8FC4, MANA_HIGH = 0xBFF4FF;

    // ---------------------------------------------------------------- bounded drawing
    /** Light motes drifting slowly in the Stomach screen and in the HUD; the star tile is blitted at most {@link #maxStarBlits} times per panel. */
    public static final int MOTES_PANEL = 14, MOTES_HUD = 6;

    public static int maxStarBlits(int w, int h) { return ((w + STAR.w() - 1) / STAR.w() + 1) * ((h + STAR.h() - 1) / STAR.h() + 1); }

    /** The x of mote {@code i} at animation time {@code t} (ticks) in a panel {@code w} wide: a fixed spread plus a slow sideways drift of 0.05 to 0.13 pixels a tick, wrapped. */
    public static float moteX(int i, float t, int w) {
        float base = (i * 47 + 13) % Math.max(1, w);
        float v = 0.05f + 0.02f * (i % 5);
        return wrap(base + t * v, w);
    }

    /** The y of mote {@code i}: it rises slowly (0.04 to 0.09 pixels a tick) and wraps. */
    public static float moteY(int i, float t, int h) {
        float base = (i * 29 + 7) % Math.max(1, h);
        float v = 0.04f + 0.0125f * (i % 5);
        return wrap(base - t * v, h);
    }

    /** The brightness of mote {@code i}: it breathes between 0.15 and 0.6, each on its own beat. */
    public static float moteAlpha(int i, float t) { return 0.375f + 0.225f * (float) Math.sin(t * (0.03 + 0.007 * (i % 4)) + i * 1.7); }

    private static float wrap(float v, int size) {
        float s = Math.max(1, size);
        float r = v % s;
        return r < 0f ? r + s : r;
    }

    // ---------------------------------------------------------------- the Stomach screen
    public static final int STOMACH_WIDTH = 176;
    /** The rows of the chest part follow the server config (3 to 6); the height is the vanilla chest's: 114 plus 18 per row. */
    public static int stomachHeight(int rows) { return 114 + 18 * Math.max(1, Math.min(6, rows)); }

    /** The title row (above the first slot row, which sits at y = 18): the emblem, the title, the level, and two slim gauges at the right. */
    public static final int EMBLEM_X = 5, EMBLEM_Y = 1, EMBLEM_SIZE = 16, TITLE_X = 24, TITLE_Y = 6, LEVEL_X = 70;
    public static final int GAUGE_X = 118, GAUGE_W = 50, GAUGE_H = 4, EXP_GAUGE_Y = 4, MANA_GAUGE_Y = 10;
    public static final int FIRST_SLOT_Y = 18;

    /** The sword's emblem and gauges need his state: without a recent state packet the gauges are drawn empty and the level is not shown. */
    public static int gaugePixels(float frac, int w) { return Math.round(Math.max(0f, Math.min(1f, frac)) * w); }

    // ---------------------------------------------------------------- the HUD
    public static final int HUD_W = 156, HUD_PAD = 5, HUD_ROW = 11, HUD_BAR = 5;
}
