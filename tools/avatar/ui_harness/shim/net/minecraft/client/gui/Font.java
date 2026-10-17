package net.minecraft.client.gui;

/** Why: harness shim: Minecraft's default font widths (a glyph plus its one pixel gap), so text takes the same room as in the game. */
public class Font {
    public final int lineHeight = 9;

    public static int charWidth(char c) {
        switch (c) {
            case 'i': case '!': case '.': case ',': case ':': case ';': case '\'': case '|': return 2;
            case 'l': case '`': return 3;
            case 'I': case 't': case '[': case ']': case ' ': return 4;
            case '"': case '*': case '(': case ')': case '{': case '}': case '<': case '>': case 'f': case 'k': return 5;
            case '@': case '~': return 7;
            default: return 6;
        }
    }

    public int width(String s) {
        if (s == null) return 0;
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += charWidth(s.charAt(i));
        return w;
    }

    public String plainSubstrByWidth(String s, int width) {
        if (s == null) return "";
        int w = 0, i = 0;
        for (; i < s.length(); i++) {
            int cw = charWidth(s.charAt(i));
            if (w + cw > width) break;
            w += cw;
        }
        return s.substring(0, i);
    }
}
