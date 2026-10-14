package com.newuniverse.nusmp.client.ui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Great-Sage text block: a header line ("Analysis complete."), the analysis wrapped to a width and typed out letter by letter with a
 * blinking cursor, and a result line ("Result: ...") that types last. One instance per place it is shown; {@link #set} re-wraps and restarts
 * the typing only when the text or the width changed, so calling it every frame is free. The wrapping and the typewriter arithmetic are the
 * pure {@link SageLayout} and {@link UiMath#typed}.
 */
public final class SageBlock {
    private static final float CHARS_PER_TICK = 1.7f;

    private String head = "", body = "", result = "";
    private int width = -1;
    private String[] lines = {""};
    private int headLines, bodyLines;
    private int total;
    private float startedAt = -1f;

    /** Sets the content; the typing restarts at 'now' when anything changed. Any of the three texts may be empty. */
    public void set(Font font, String head, String body, String result, int width, float now) {
        head = head == null ? "" : head;
        body = body == null ? "" : body;
        result = result == null ? "" : result;
        if (width == this.width && head.equals(this.head) && body.equals(this.body) && result.equals(this.result)) return;
        this.head = head; this.body = body; this.result = result; this.width = width;
        SageLayout.Metrics m = font::width;
        String[] h = head.isEmpty() ? new String[0] : SageLayout.wrap(head, m, width);
        String[] b = body.isEmpty() ? new String[0] : SageLayout.wrap(body, m, width);
        String[] r = result.isEmpty() ? new String[0] : SageLayout.wrap(result, m, width);
        lines = new String[h.length + b.length + r.length];
        System.arraycopy(h, 0, lines, 0, h.length);
        System.arraycopy(b, 0, lines, h.length, b.length);
        System.arraycopy(r, 0, lines, h.length + b.length, r.length);
        headLines = h.length;
        bodyLines = b.length;
        total = SageLayout.totalChars(lines);
        startedAt = now;
    }

    /** Restarts the typing without changing the text. */
    public void restart(float now) { startedAt = now; }

    public int lineCount() { return lines.length; }

    public int height(Font font) { return lines.length * (font.lineHeight + 1); }

    public boolean finished(float now) { return typed(now) >= total; }

    private int typed(float now) {
        if (!UiThemeOptions.typing() || !UiTheme.motion() || startedAt < 0f) return total;
        return UiMath.typed(now - startedAt, CHARS_PER_TICK, total);
    }

    /** Draws the block at (x, y) and returns the y below it. The typing runs on the clock passed as 'now' (see {@link #set}). */
    public int draw(GuiGraphics g, Font font, int x, int y, float now, float alpha) {
        int n = typed(now);
        int lh = font.lineHeight + 1;
        int cursor = n < total ? SageLayout.cursorLine(lines, n) : -1;
        for (int i = 0; i < lines.length; i++) {
            int vis = SageLayout.visibleInLine(lines, i, n);
            if (vis <= 0 && i != cursor) { y += lh; continue; }
            int color = i < headLines ? UiTheme.SAGE_HEAD : i < headLines + bodyLines ? UiTheme.SAGE : UiTheme.GOLD;
            color = UiMath.scaleAlpha(color, alpha);
            String s = lines[i];
            String shown = vis >= s.length() ? s : s.substring(0, vis);
            if ((color >>> 24) >= 8) g.drawString(font, shown, x, y, color, false);
            if (i == cursor && ((int) (now / 5f) & 1) == 0)
                g.fill(x + font.width(shown) + 1, y, x + font.width(shown) + 4, y + font.lineHeight - 1, UiMath.scaleAlpha(UiTheme.SAGE_HEAD, alpha));
            y += lh;
        }
        return y;
    }
}
