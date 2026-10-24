package com.newuniverse.nusmp.client.ui.gs;

import com.newuniverse.nusmp.client.ui.theme.UiMath;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Draws a block of page text with the ink-to-matrix compile animation ({@link GsCompile}). The wrapped lines are built once per page and kept
 * ({@link Lines}: the plain text and an italic "ink" version), so a frame allocates nothing at rest; while the 2-second compile runs, a line inside
 * the band draws two layers and one short scrambled string (at most a handful of small strings per frame, only during the animation).
 * On quality LOW the band and the scramble are skipped and the whole block cross-fades from ink to settled text.
 */
public final class GsCompileText {
    private GsCompileText() {}

    /** The wrapped text of one page, built once. */
    public static final class Lines {
        public final String[] plain;
        public final FormattedCharSequence[] ink;

        Lines(String[] plain, FormattedCharSequence[] ink) { this.plain = plain; this.ink = ink; }
    }

    /**
     * Wraps 'text' to 'width' GUI pixels in at most 'maxLines' lines, keeps within {@link GsCompile#PAGE_GLYPHS} drawn glyphs in the worst frame (a line is
     * shortened with "..." only if the text would not fit the budget), and builds the italic ink version.
     */
    public static Lines build(Font font, String text, int width, int maxLines, int lineH) {
        List<FormattedText> split = font.getSplitter().splitLines(text, width, Style.EMPTY);
        int n = Math.max(1, Math.min(maxLines, split.size()));
        int longest = 1;
        for (int i = 0; i < Math.min(n, split.size()); i++) longest = Math.max(longest, split.get(i).getString().length());
        int cap = GsCompile.glyphCap(n, longest, lineH, GsCompile.PAGE_GLYPHS);
        String[] plain = new String[n];
        FormattedCharSequence[] ink = new FormattedCharSequence[n];
        for (int i = 0; i < n; i++) {
            String s = i < split.size() ? split.get(i).getString() : "";
            if (i == n - 1 && split.size() > n) s += "...";                       // there was more text than room
            s = GsCompile.fit(s, cap);
            plain[i] = s;
            ink[i] = Component.literal(s).withStyle(ChatFormatting.ITALIC).getVisualOrderText();
        }
        return new Lines(plain, ink);
    }

    private static final char[] BUF = new char[200];

    /**
     * Draws the block with its top-left at (x, y). 'age' is the ticks since the compile started (use a value of {@link GsCompile#DURATION} or more
     * for "settled"). Colours are ARGB; 'high' is quality HIGH.
     */
    public static void draw(GuiGraphics g, Font font, Lines lines, int x, int y, int lineH, float age, int ink, int settled, int data, boolean high) {
        int n = lines.plain.length;
        if (GsCompile.done(age)) {
            for (int i = 0; i < n; i++) g.drawString(font, lines.plain[i], x, y + i * lineH, settled, false);
            return;
        }
        float p = GsCompile.progress(age);
        float h = n * lineH;
        int step = (int) (age * 0.6f);
        for (int i = 0; i < n; i++) {
            float mix = high ? GsCompile.lineMix(i * lineH, p, h) : UiMath.smooth(p);
            int ly = y + i * lineH;
            switch (GsCompile.layers(mix)) {
                case 0 -> g.drawString(font, lines.ink[i], x, ly, ink, false);
                case 1 -> g.drawString(font, lines.plain[i], x, ly, settled, false);
                default -> {
                    int inkC = UiMath.scaleAlpha(ink, 1f - mix);
                    if ((inkC >>> 24) >= 8) g.drawString(font, lines.ink[i], x, ly, inkC, false);          // alpha under 4 would draw opaque
                    if (high) {
                        int dataC = UiMath.scaleAlpha(data, Math.min(1f, mix * 1.6f));
                        if ((dataC >>> 24) >= 8) g.drawString(font, scrambled(lines.plain[i], i, step, GsCompile.scrambleFrac(mix)), x, ly, dataC, false);
                        int setC = UiMath.scaleAlpha(settled, Math.max(0f, mix * 2f - 1f));
                        if ((setC >>> 24) >= 8) g.drawString(font, lines.plain[i], x, ly, setC, false);
                    } else {
                        int setC = UiMath.scaleAlpha(settled, mix);
                        if ((setC >>> 24) >= 8) g.drawString(font, lines.plain[i], x, ly, setC, false);
                    }
                }
            }
        }
    }

    private static String scrambled(String s, int line, int step, float frac) {
        int len = Math.min(s.length(), BUF.length);
        for (int k = 0; k < len; k++) {
            char c = s.charAt(k);
            BUF[k] = c == ' ' ? ' ' : GsCompile.scrambled(k + line * 37, step, frac) ? GsCompile.scrambleGlyph(k + line * 37, step) : c;
        }
        return new String(BUF, 0, len);
    }
}
