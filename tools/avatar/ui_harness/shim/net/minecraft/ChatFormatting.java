package net.minecraft;

/** Why: harness shim: the colours of the chat formats the screen uses. */
public enum ChatFormatting {
    BLACK(0x000000), DARK_BLUE(0x0000AA), DARK_GREEN(0x00AA00), DARK_AQUA(0x00AAAA), DARK_RED(0xAA0000), DARK_PURPLE(0xAA00AA), GOLD(0xFFAA00), GRAY(0xAAAAAA),
    DARK_GRAY(0x555555), BLUE(0x5555FF), GREEN(0x55FF55), AQUA(0x55FFFF), RED(0xFF5555), LIGHT_PURPLE(0xFF55FF), YELLOW(0xFFFF55), WHITE(0xFFFFFF),
    ITALIC(-1), BOLD(-1);
    public final int rgb;
    ChatFormatting(int rgb) { this.rgb = rgb; }
}
