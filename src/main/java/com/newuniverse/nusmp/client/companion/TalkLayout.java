package com.newuniverse.nusmp.client.companion;

/**
 * Why: where everything on the talk screen sits, as pure arithmetic (no Minecraft type), so the drawing, the clicks and the tests share one answer and the screen fits any GUI scale: the panel is at most 300 by 232 and
 * shrinks to the window with an 4 pixel margin; inside it come the header, the four tabs, the box with her words, a status strip (the Bond and Pouch tabs: rapport and stance), the list of choices (scrollable), and a
 * Close button; on the Pouch tab the nine slots take the right side of the list. Every rectangle is an {@code int[4]} of x, y, width, height.
 */
public final class TalkLayout {
    public static final int TABS = 4, SLOTS = 9, SLOT = 18, ROW_H = 14, ROW_PITCH = 15, PAD = 8;
    public static final int MAX_W = 300, MAX_H = 232, MARGIN = 4;

    public final int tab;
    public final int[] panel, header, tabs0, text, status, list, close, grid;
    public final int tabW, visibleRows, listRowsW;

    private TalkLayout(int tab, int[] panel, int[] header, int[] tabs0, int tabW, int[] text, int[] status, int[] list, int visibleRows, int listRowsW, int[] close, int[] grid) {
        this.tab = tab;
        this.panel = panel;
        this.header = header;
        this.tabs0 = tabs0;
        this.tabW = tabW;
        this.text = text;
        this.status = status;
        this.list = list;
        this.visibleRows = visibleRows;
        this.listRowsW = listRowsW;
        this.close = close;
        this.grid = grid;
    }

    /** Lays the screen out for a window of {@code guiW} by {@code guiH} (GUI pixels) and the shown tab (0 Talk, 1 Interact, 2 Bond, 3 Pouch). */
    public static TalkLayout compute(int guiW, int guiH, int tab) {
        tab = Math.max(0, Math.min(TABS - 1, tab));
        int w = Math.max(160, Math.min(MAX_W, guiW - 2 * MARGIN)), h = Math.max(110, Math.min(MAX_H, guiH - 2 * MARGIN));
        int x = (guiW - w) / 2, y = (guiH - h) / 2;
        int[] panel = {x, y, w, h};
        int[] header = {x, y, w, 30};
        int ty = y + 32;
        int[] tabs0 = {x + PAD, ty, w - 2 * PAD, 14};
        int tabW = (w - 2 * PAD) / TABS;
        int textY = ty + 14 + 4;
        boolean statusShown = tab == 2 || tab == 3;
        int textH = Math.max(24, Math.min(statusShown ? 40 : 52, (h - 130) / 2 + 24));
        int[] text = {x + PAD, textY, w - 2 * PAD, textH};
        int statusY = textY + textH + 3;
        int[] status = statusShown ? new int[]{x + PAD, statusY, w - 2 * PAD, 18} : new int[]{x + PAD, statusY, w - 2 * PAD, 0};
        int listY = statusShown ? statusY + 18 + 3 : statusY;
        int footer = 16;
        int listBottom = y + h - PAD - footer;
        int listH = Math.max(ROW_PITCH, listBottom - listY);
        boolean grid = tab == 3;
        int listW = w - 2 * PAD - (grid ? 3 * SLOT + 8 : 0);
        int[] list = {x + PAD, listY, listW, listH};
        int rows = Math.max(1, listH / ROW_PITCH);
        int[] close = {x + w - PAD - 56, y + h - PAD - 14, 56, 14};
        int[] gridRect = grid ? new int[]{x + w - PAD - 3 * SLOT, listY, 3 * SLOT, 3 * SLOT} : new int[]{0, 0, 0, 0};
        return new TalkLayout(tab, panel, header, tabs0, tabW, text, status, list, rows, listW, close, gridRect);
    }

    public static boolean in(int[] r, int mx, int my) { return r[2] > 0 && r[3] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]; }

    /** The tab chip at the point, or -1. */
    public int tabAt(int mx, int my) {
        if (!in(tabs0, mx, my)) return -1;
        int i = (mx - tabs0[0]) / tabW;
        return i >= 0 && i < TABS ? i : -1;
    }

    /** The rectangle of tab chip {@code i}. */
    public int[] tabRect(int i) { return new int[]{tabs0[0] + i * tabW, tabs0[1], tabW - 1, tabs0[3]}; }

    /** The rectangle of the row at position {@code slot} (0 = first visible). */
    public int[] rowRect(int slot) { return new int[]{list[0], list[1] + slot * ROW_PITCH, listRowsW - 6, ROW_H}; }

    /** The index (into the options of this tab, counting from the first) of the row at the point, or -1; {@code scroll} is the first shown index. */
    public int rowAt(int mx, int my, int scroll, int count) {
        for (int s = 0; s < visibleRows; s++) {
            int i = scroll + s;
            if (i >= count) break;
            if (in(rowRect(s), mx, my)) return i;
        }
        return -1;
    }

    /** The pouch slot (0..8) at the point, or -1 (only on the Pouch tab). */
    public int slotAt(int mx, int my) {
        if (tab != 3 || !in(grid, mx, my)) return -1;
        int col = (mx - grid[0]) / SLOT, row = (my - grid[1]) / SLOT;
        return row * 3 + col;
    }

    /** The rectangle of pouch slot {@code i}. */
    public int[] slotRect(int i) { return new int[]{grid[0] + (i % 3) * SLOT, grid[1] + (i / 3) * SLOT, SLOT, SLOT}; }

    public boolean closeAt(int mx, int my) { return in(close, mx, my); }

    /** The largest first-shown index for a list of {@code count} rows. */
    public int maxScroll(int count) { return Math.max(0, count - visibleRows); }
}
