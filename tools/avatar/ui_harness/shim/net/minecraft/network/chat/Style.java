package net.minecraft.network.chat;

/** Why: harness shim: only the colour of a text style. */
public class Style {
    public static final Style EMPTY = new Style(-1);
    final int rgb;
    Style(int rgb) { this.rgb = rgb; }
}
