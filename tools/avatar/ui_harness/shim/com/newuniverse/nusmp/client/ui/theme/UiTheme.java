package com.newuniverse.nusmp.client.ui.theme;

import net.minecraft.client.gui.Font;

/** Why: harness shim of the theme: the same palette, and a clock the scenario advances by hand. */
public final class UiTheme {
    private UiTheme() {}

    public static final int PANEL_TOP = 0xCC111C34, PANEL_BOTTOM = 0xCC080D1B, PANEL_DEEP = 0xD9060912;
    public static final int CYAN = 0xFF5CD8FF, VIOLET = 0xFF9A7CFF, GOLD = 0xFFE8C468, GREEN = 0xFF7CF0B0, RED = 0xFFFF6A6A, WHITE = 0xFFFFFFFF;
    public static final int TEXT = 0xFFE6F2FF, TEXT_DIM = 0xFF8FA3BF, TEXT_FAINT = 0xFF5C6E88, SAGE = 0xFFBFEFFF, SAGE_HEAD = 0xFF5CD8FF;

    public static float clock = 40f;
    public static boolean motion = true;

    public static boolean anime() { return true; }
    public static boolean motion() { return motion; }
    public static float now() { return motion ? clock : 40f; }
    public static float realNow() { return clock; }
    public static Font font() { return new Font(); }
}
