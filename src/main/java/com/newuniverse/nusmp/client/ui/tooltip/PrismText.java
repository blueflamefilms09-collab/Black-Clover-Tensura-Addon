package com.newuniverse.nusmp.client.ui.tooltip;

import com.newuniverse.nusmp.relics.ExSingularity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Why: the EX-Singularity grade (above Genesis) is shown as prismatic text: every glyph of the grade row and of the "Akashic Relic - Grade: EX-Singularity" line takes its own hue of the full
 * spectrum, the hues turn with time and a white glint runs along the row ({@link ExSingularity#textRgb}). Client only (a tooltip draws only while a stack is hovered).
 * <p>
 * Cost, worst case: a glyph table is built ONCE per tooltip row (when the layout is first needed: one array of strings, one of offsets); a frame then costs one {@code drawString} per glyph
 * (at most {@link ExSingularity#MAX_GLYPHS}, a longer row is drawn in one colour) and NO allocation: the colours come from pure int arithmetic, the strings are the cached glyphs. The item's NAME
 * is the one vanilla draws, so it needs a component per glyph; that one is rebuilt only when the colour step (50 ms) changes and cached in between, so a hovered Shishou allocates its seven small
 * components at most twenty times a second, and nothing at all when nothing is hovered.
 */
public final class PrismText {
    private PrismText() {}

    /** A row's glyphs, split once: the cached one-character strings and their x offsets. */
    public static final class Glyphs {
        final String[] glyph;
        final int[] x;
        final int width;
        final String whole;

        Glyphs(String text, Font font) {
            this.whole = text;
            int n = text.codePointCount(0, text.length());
            if (n > ExSingularity.MAX_GLYPHS) {                       // too long to draw one by one: one colour, one draw
                glyph = null;
                x = null;
                width = font.width(text);
                return;
            }
            glyph = new String[n];
            x = new int[n];
            int px = 0, k = 0;
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                String g = new String(Character.toChars(cp));
                glyph[k] = g;
                x[k] = px;
                px += font.width(g);
                k++;
                i += Character.charCount(cp);
            }
            width = px;
        }

        public int width() { return width; }
    }

    /** Splits a row once (call when the layout is built, not per frame). */
    public static Glyphs of(Font font, String text) { return new Glyphs(text == null ? "" : text, font); }

    /** Draws a row at (x, y): glyph by glyph in the prismatic colours at {@code millis}; no allocation. */
    public static void draw(GuiGraphics g, Font font, Glyphs row, int x, int y, long millis, boolean shadow) {
        if (row.glyph == null) {
            g.drawString(font, row.whole, x, y, 0xFF000000 | ExSingularity.rowRgb(millis, 0f), shadow);
            return;
        }
        int n = row.glyph.length;
        for (int i = 0; i < n; i++) g.drawString(font, row.glyph[i], x + row.x[i], y, 0xFF000000 | ExSingularity.textRgb(i, n, millis), shadow);
    }

    // ---------------------------------------------------------------- the item's name (drawn by vanilla, so it is a component)
    private static long nameStep = Long.MIN_VALUE;
    private static String nameText = "";
    private static Component nameComponent = Component.empty();

    /** The name in the prismatic colours: a component with one coloured literal per glyph, rebuilt only when the 50 ms step or the text changes. */
    public static Component name(String text, long millis) {
        long step = millis / 50L;
        if (step != nameStep || !text.equals(nameText)) {
            MutableComponent root = Component.empty();
            int n = text.codePointCount(0, text.length());
            int k = 0;
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                root.append(Component.literal(new String(Character.toChars(cp))).withStyle(Style.EMPTY.withColor(ExSingularity.textRgb(k, n, step * 50L))));
                k++;
                i += Character.charCount(cp);
            }
            nameStep = step;
            nameText = text;
            nameComponent = root;
        }
        return nameComponent;
    }
}
