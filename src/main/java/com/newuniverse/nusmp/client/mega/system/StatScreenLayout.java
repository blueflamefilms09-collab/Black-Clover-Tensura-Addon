package com.newuniverse.nusmp.client.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.system.Stat;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the stat screen is all mouse, so WHERE each button is decides whether a click does what it says, and a button that overlaps its neighbour, hangs off the
 * panel or sits under the wrong row is a bug nobody sees until a click goes to the wrong stat. So the geometry is a pure function (no Minecraft type): the
 * screen size and the number of text lines each stat's effect needs go in, rectangles come out (the panel, five stat rows each with [+1] [+5] [MAX], and the
 * footer buttons RESET / MOVE WINDOW / CLOSE), and {@link Layout#hit} answers which button a click lands on. The screen class only draws these rectangles and
 * sends what the hit says. The panel is centred and as wide as the text needs up to {@link #MAX_W}; when the screen is too short for the comfortable spacing,
 * the compact form drops the gaps and the mode line, so the panel still fits a 427 x 240 screen (GUI scale 3 on 720p). Pure.
 */
public final class StatScreenLayout {
    private StatScreenLayout() {}

    public static final int MAX_W = 380, MIN_W = 250, PAD = 8, GAP = 4, BTN_H = 12, LINE = 10, ROW_HEAD = 14;
    /** Widths of the [+1], [+5] and [MAX] buttons and the space between them. */
    public static final int W_ADD1 = 24, W_ADD5 = 24, W_MAX = 32, BTN_GAP = 3;
    /** Footer buttons. */
    public static final int FOOT_H = 14, FOOT_GAP = 6;

    /** What a click can do. */
    public enum Act { ADD1, ADD5, MAX, RESET, MOVE, CLOSE }

    /** An integer rectangle in GUI pixels. */
    public record Rect(int x, int y, int w, int h) {
        public boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }

        public int right() { return x + w; }

        public int bottom() { return y + h; }
    }

    /** One stat's row: the whole row, its three buttons, where its name and its effect lines start, and how many effect lines it has. */
    public record Row(Stat stat, Rect rect, Rect add1, Rect add5, Rect max, int textX, int nameY, int effectY, int lines) {}

    /** A click target: the action and, for the row buttons, the stat (null for the footer). */
    public record Hit(Act act, Stat stat) {}

    /**
     * The whole screen.
     *
     * @param panel   the panel behind everything
     * @param titleY  y of the title line; x is {@code panel.x + PAD}
     * @param freeY   y of the free-points line
     * @param modeY   y of the point-mode line, or -1 when it is left out (compact)
     * @param rows    the five stat rows in {@link Stat} order
     * @param reset   the RESET button
     * @param move    the MOVE WINDOW button
     * @param close   the CLOSE button
     * @param compact the spacing was squeezed to fit a short screen
     */
    public record Layout(Rect panel, int titleY, int freeY, int modeY, List<Row> rows, Rect reset, Rect move, Rect close, boolean compact) {
        /** The button under the point, or null. Row buttons win over the row; the footer buttons are separate. */
        public Hit hit(double mx, double my) {
            if (reset.contains(mx, my)) return new Hit(Act.RESET, null);
            if (move.contains(mx, my)) return new Hit(Act.MOVE, null);
            if (close.contains(mx, my)) return new Hit(Act.CLOSE, null);
            for (Row r : rows) {
                if (r.add1().contains(mx, my)) return new Hit(Act.ADD1, r.stat());
                if (r.add5().contains(mx, my)) return new Hit(Act.ADD5, r.stat());
                if (r.max().contains(mx, my)) return new Hit(Act.MAX, r.stat());
            }
            return null;
        }

        /** The row the point is over (for the mouse wheel and the row tooltip), or null. */
        public Row rowAt(double mx, double my) {
            for (Row r : rows) if (r.rect().contains(mx, my)) return r;
            return null;
        }
    }

    /** The panel's width on this screen: as wide as {@link #MAX_W} allows, never past the screen less a margin each side, never below {@link #MIN_W} unless the screen is narrower. */
    public static int panelWidth(int screenW) { return Math.max(Math.min(MIN_W, screenW), Math.min(MAX_W, screenW - 16)); }

    /** The width the effect lines may use, for wrapping them before the layout is made. */
    public static int textWidth(int screenW) { return panelWidth(screenW) - 2 * PAD - 4; }

    /**
     * Lays the screen out.
     *
     * @param effectLines for each stat, how many lines its effect text takes (at least 1 is used)
     */
    public static Layout of(int screenW, int screenH, int[] effectLines) {
        Layout roomy = build(screenW, screenH, effectLines, false);
        if (roomy.panel().h() <= screenH - 8) return roomy;
        return build(screenW, screenH, effectLines, true);
    }

    private static Layout build(int screenW, int screenH, int[] effectLines, boolean compact) {
        int w = panelWidth(screenW);
        int gap = compact ? 1 : GAP;
        int headH = compact ? 24 : 38;                                                   // title, free points, (mode line)
        int total = PAD + headH;
        int[] lines = new int[Stat.COUNT];
        for (int i = 0; i < Stat.COUNT; i++) {
            lines[i] = Math.max(1, i < effectLines.length ? effectLines[i] : 1);
            total += ROW_HEAD + lines[i] * LINE + gap;
        }
        total += FOOT_GAP + FOOT_H + PAD;
        int h = total;
        int x = (screenW - w) / 2;
        int y = Math.max(0, (screenH - h) / 2);
        Rect panel = new Rect(x, y, w, h);
        int cy = y + PAD;
        int titleY = cy;
        int freeY = cy + 12;
        int modeY = compact ? -1 : cy + 26;
        cy += headH;
        List<Row> rows = new ArrayList<>(Stat.COUNT);
        int right = x + w - PAD;
        for (int i = 0; i < Stat.COUNT; i++) {
            int rowH = ROW_HEAD + lines[i] * LINE;
            int by = cy + (ROW_HEAD - BTN_H) / 2 - 1;
            Rect max = new Rect(right - W_MAX, by, W_MAX, BTN_H);
            Rect add5 = new Rect(max.x() - BTN_GAP - W_ADD5, by, W_ADD5, BTN_H);
            Rect add1 = new Rect(add5.x() - BTN_GAP - W_ADD1, by, W_ADD1, BTN_H);
            rows.add(new Row(Stat.values()[i], new Rect(x + PAD / 2, cy - 1, w - PAD, rowH + 1), add1, add5, max, x + PAD, cy + 2, cy + ROW_HEAD, lines[i]));
            cy += rowH + gap;
        }
        cy += FOOT_GAP;
        int third = (w - 2 * PAD - 2 * BTN_GAP * 2) / 3;
        Rect reset = new Rect(x + PAD, cy, third, FOOT_H);
        Rect move = new Rect(reset.right() + BTN_GAP * 2, cy, third, FOOT_H);
        Rect close = new Rect(move.right() + BTN_GAP * 2, cy, w - 2 * PAD - 2 * third - 4 * BTN_GAP, FOOT_H);
        return new Layout(panel, titleY, freeY, modeY, List.copyOf(rows), reset, move, close, compact);
    }
}
