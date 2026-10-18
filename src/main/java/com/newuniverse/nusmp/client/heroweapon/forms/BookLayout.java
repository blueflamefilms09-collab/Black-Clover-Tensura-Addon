package com.newuniverse.nusmp.client.heroweapon.forms;

/**
 * Why: the Weapon Book has to fit every window a player might have, from a small laptop at a large GUI scale (about 284 by 160 GUI pixels) to a big monitor
 * (about 960 by 540), and a layout that overlaps or runs off the edge on one of them is a bug nobody on the team would see. The rectangles are computed here by plain
 * integer arithmetic with no Minecraft types, so a test can check, for a grid of window sizes, that every part lies inside the panel, nothing overlaps and the two
 * panes keep a usable size; the screen only reads them. The panel is a header (the weapon, its level), a body of two panes (the form list on the left, the details
 * of the chosen form on the right) and a footer (the curse state on the left, as tall as the footer; the buttons under the details, one row).
 */
public record BookLayout(Rect panel, Rect header, Rect list, Rect pane, Rect footerLeft, Rect footerRight) {
    /** A rectangle in GUI pixels. */
    public record Rect(int x, int y, int w, int h) {
        public int right() { return x + w; }

        public int bottom() { return y + h; }

        public boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }

        public boolean inside(Rect o) { return x >= o.x && y >= o.y && right() <= o.right() && bottom() <= o.bottom(); }

        public boolean overlaps(Rect o) { return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom(); }
    }

    public static final int HEADER_H = 32, FOOTER_H = 24, PAD = 6, GAP = 6, ROW_H = 14, BUTTON_H = 16;
    public static final int MIN_PANEL_W = 240, MAX_PANEL_W = 420, MIN_PANEL_H = 150, MAX_PANEL_H = 264, MIN_LIST_W = 112, MAX_LIST_W = 160;

    /** The layout for a window of {@code sw} by {@code sh} GUI pixels. */
    public static BookLayout of(int sw, int sh) {
        int pw = Math.min(sw, Math.max(MIN_PANEL_W, Math.min(MAX_PANEL_W, sw - 16)));
        int ph = Math.min(sh, Math.max(MIN_PANEL_H, Math.min(MAX_PANEL_H, sh - 16)));
        int px = (sw - pw) / 2, py = (sh - ph) / 2;
        Rect panel = new Rect(px, py, pw, ph);
        Rect header = new Rect(px + PAD, py + 5, pw - 2 * PAD, HEADER_H - 7);
        int listW = Math.max(MIN_LIST_W, Math.min(MAX_LIST_W, pw * 36 / 100));
        int bodyY = py + HEADER_H, bodyH = ph - HEADER_H - FOOTER_H;
        Rect list = new Rect(px + PAD, bodyY, listW, bodyH);
        int paneX = list.right() + GAP;
        Rect pane = new Rect(paneX, bodyY, px + pw - PAD - paneX, bodyH);
        int footY = py + ph - FOOTER_H + (FOOTER_H - BUTTON_H) / 2;
        Rect footerLeft = new Rect(list.x(), py + ph - FOOTER_H, list.w(), FOOTER_H);
        Rect footerRight = new Rect(pane.x(), footY, pane.w(), BUTTON_H);
        return new BookLayout(panel, header, list, pane, footerLeft, footerRight);
    }

    /** How many list rows fit in the list pane. */
    public int visibleRows() { return Math.max(1, (list.h() - 2) / ROW_H); }

    /** The rectangle of one of {@code n} buttons laid side by side across the footer (n of 1 to 3). */
    public Rect button(int index, int n) {
        int count = Math.max(1, n), gap = 4;
        int w = (footerRight.w() - gap * (count - 1)) / count;
        return new Rect(footerRight.x() + index * (w + gap), footerRight.y(), w, BUTTON_H);
    }
}
