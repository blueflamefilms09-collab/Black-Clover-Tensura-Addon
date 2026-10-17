package com.newuniverse.nusmp.avatar.menu;

/**
 * Why: the Soul Forge screen fills the whole window, and a window can be anything from the smallest legal GUI (320 x 240 after the GUI scale) to a 4K
 * monitor at scale 2. Where the five round category buttons, the three columns (the module that slides open, the 3D Avatar, the ledger of costs), the
 * bottom bar and the icon grids go is arithmetic, so it lives here, pure and unit tested, away from the widgets: a test sweeps hundreds of window sizes and
 * proves nothing overlaps, nothing leaves the window and every grid keeps at least three columns. {@code SoulForgeScreen} only asks for the rectangles. The
 * numbers were chosen by looking at the picture the preview tool draws from this very class (tools/avatar/preview_forge_layout.py).
 */
public final class ForgeLayout {
    private ForgeLayout() {}

    /** An axis-aligned box in GUI pixels. */
    public record Rect(int x, int y, int w, int h) {
        public static final Rect EMPTY = new Rect(0, 0, 0, 0);

        public int right() { return x + w; }

        public int bottom() { return y + h; }

        public int cx() { return x + w / 2; }

        public int cy() { return y + h / 2; }

        public boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }

        public boolean isEmpty() { return w <= 0 || h <= 0; }

        public Rect inset(int n) { return new Rect(x + n, y + n, Math.max(0, w - 2 * n), Math.max(0, h - 2 * n)); }

        public boolean overlaps(Rect o) { return !isEmpty() && !o.isEmpty() && x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom(); }
    }

    /**
     * All the rectangles of one window size. {@code categories} are the bounding squares of the five round buttons, in page order; {@code back}, {@code next} and
     * {@code finalizeButton} are the bottom bar's buttons and {@code message} the space between them for the one line that says what is missing; {@code ledger} is
     * where the cost panel goes (the right column when there is one, else a strip at the foot of the centre column); {@code icon} is the size icons in the
     * grids prefer and {@code slot} the size of the big round slots, both growing with the window so a 1080p screen is not a few tiny icons in a big box.
     */
    public record Layout(int width, int height, Rect top, Rect left, Rect center, Rect right, Rect bottom, Rect[] categories, boolean ledgerBeside, int margin,
                         Rect back, Rect next, Rect finalizeButton, Rect message, Rect ledger, Rect stage, int icon, int slot) {
        /** The area of a page's controls inside the left module (below its heading). */
        public Rect leftBody() { return new Rect(left.x() + 6, left.y() + 18, Math.max(0, left.w() - 12), Math.max(0, left.h() - 24)); }
    }

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /**
     * The whole-screen zoom: the scene is composed for a canvas of about 430 x 243 to 640 x 360 GUI pixels, and a bigger window shows the same scene at 2, 3 or 4
     * times the size (a whole number, so text stays crisp) instead of the same few controls lost in a big box. A 1080p window at GUI scale 2 (960 x 540) gets
     * 2, so its canvas is 480 x 270; a window that is already about the design size gets 1.
     */
    public static int uiScale(int w, int h) {
        float k = Math.min(Math.max(1, w) / 430f, Math.max(1, h) / 243f);
        return clamp((int) Math.floor(k + 0.1f), 1, 4);
    }

    /** The layout of a window of {@code w} x {@code h} GUI pixels. Pure and total: it never fails, however small the window. */
    public static Layout compute(int w, int h) {
        w = Math.max(160, w);
        h = Math.max(120, h);
        int margin = clamp(Math.round(w * 0.012f), 3, 12);
        int topH = clamp(Math.round(h * 0.15f), 30, 54);
        int botH = clamp(Math.round(h * 0.11f), 22, 38);
        int gap = margin;
        int bodyY = topH + gap, bodyH = Math.max(40, h - topH - botH - 2 * gap);
        boolean beside = w >= 420;
        int leftW = clamp(Math.round(w * (beside ? 0.36f : 0.46f)), 110, 340);
        int rightW = beside ? clamp(Math.round(w * 0.25f), 104, 240) : 0;
        int gaps = beside ? 2 * gap : gap;
        int centerW = w - 2 * margin - leftW - rightW - gaps;
        if (centerW < 80) {                               // squeeze the module first, then the ledger, before the Avatar loses its stage
            int need = 80 - centerW;
            int takeLeft = Math.min(need, Math.max(0, leftW - 110));
            leftW -= takeLeft;
            need -= takeLeft;
            int takeRight = Math.min(need, Math.max(0, rightW - 90));
            rightW -= takeRight;
            centerW = w - 2 * margin - leftW - rightW - gaps;
        }
        Rect left = new Rect(margin, bodyY, leftW, bodyH);
        Rect center = new Rect(left.right() + gap, bodyY, Math.max(1, centerW), bodyH);
        Rect right = beside ? new Rect(w - margin - rightW, bodyY, rightW, bodyH) : Rect.EMPTY;
        Rect top = new Rect(0, 0, w, topH), bottom = new Rect(0, h - botH, w, botH);

        // the bottom bar: Back, Next on the left, Finalize on the right, the message in between
        int bh = clamp(botH - 8, 14, 24);
        int by = bottom.y() + (botH - bh) / 2;
        int smallW = clamp(Math.round(w * 0.09f), 40, 70), bigW = clamp(Math.round(w * 0.2f), 80, 150);
        int avail = w - 2 * margin - 8;
        if (2 * smallW + bigW > avail) {                  // a very narrow window: the three buttons share what there is
            float k = avail / (float) (2 * smallW + bigW);
            smallW = Math.max(12, (int) (smallW * k));
            bigW = Math.max(24, (int) (bigW * k));
        }
        Rect back = new Rect(margin, by, smallW, bh);
        Rect nextB = new Rect(back.right() + 4, by, smallW, bh);
        Rect fin = new Rect(w - margin - bigW, by, bigW, bh);
        int msgX = nextB.right() + 8;
        Rect msg = new Rect(msgX, bottom.y(), Math.max(0, fin.x() - 8 - msgX), botH);

        // the cost panel: the whole right column, or (in a narrow window) a strip at the foot of the centre column
        Rect centre = center;
        Rect ledger, stage;
        if (beside) {
            ledger = right;
            stage = centre;
        } else {
            int ledgerH = clamp(Math.round(centre.h() * 0.34f), 38, 70);
            ledger = new Rect(centre.x(), centre.bottom() - ledgerH, centre.w(), ledgerH);
            stage = new Rect(centre.x(), centre.y(), centre.w(), Math.max(1, centre.h() - ledgerH - 2));
        }

        int d = clamp(topH - 10, 18, 38);
        int step = d + clamp(d / 2, 6, 22);
        while (4 * step + d > w - 2 * margin && d > 16) { d--; step = d + clamp(d / 2, 4, 22); }
        int total = 4 * step + d, x0 = (w - total) / 2, y0 = (topH - d) / 2;
        Rect[] cats = new Rect[5];
        for (int i = 0; i < 5; i++) cats[i] = new Rect(x0 + i * step, y0, d, d);
        int icon = clamp(Math.round(Math.min(w / 17f, h / 9.6f)), 26, 46);
        int slot = clamp(Math.round(icon * 1.35f), 30, 64);
        return new Layout(w, h, top, left, center, right, bottom, cats, beside, margin, back, nextB, fin, msg, ledger, stage, icon, slot);
    }

    /**
     * A row of {@code count} round slots across the top of {@code body} (the two skill slots, the aptitude slots): as big as {@code prefer} allows, never
     * bigger than the room, never smaller than 20 pixels, centred, with a gap that grows with the slot. Returns the bounding squares.
     */
    public static Rect[] slotRow(Rect body, int count, int prefer) {
        int n = Math.max(1, count);
        int gap = clamp(prefer / 3, 6, 18);
        int d = clamp(Math.min(prefer, (body.w() - (n - 1) * gap) / n), 20, 64);
        if (n > 1 && n * d + (n - 1) * gap > body.w()) gap = Math.max(2, (body.w() - n * d) / (n - 1));      // too narrow even for the smallest circles: close the gaps
        int total = n * d + (n - 1) * gap;
        int x0 = body.x() + Math.max(0, (body.w() - total) / 2);
        Rect[] out = new Rect[n];
        for (int i = 0; i < n; i++) out[i] = new Rect(x0 + i * (d + gap), body.y(), d, d);
        return out;
    }

    /** The height a slot row takes including the label line under the circles. */
    public static int slotRowHeight(Rect[] slots) { return slots.length == 0 ? 0 : slots[0].h() + 12; }

    /** The part of {@code body} below a slot row (and a small gap), where the icon grid goes. */
    public static Rect below(Rect body, int usedHeight) {
        int used = Math.min(body.h(), usedHeight + 4);
        return new Rect(body.x(), body.y() + used, body.w(), Math.max(0, body.h() - used));
    }

    /** A scrollable grid of square cells inside an area: how many columns fit, which cell is under the mouse, where a cell is drawn after scrolling by whole rows. */
    public record Grid(Rect area, int cell, int gap, int cols, int rows, int count) {
        /** Fits {@code count} cells of about {@code cellPref} pixels into {@code area}; never fewer than one column (and three when the width allows them). */
        public static Grid fit(Rect area, int cellPref, int gap, int count) {
            int cell = Math.max(8, cellPref);
            int cols = Math.max(1, (area.w() + gap) / (cell + gap));
            if (cols < 3 && area.w() >= 3 * 12 + 2 * gap) {          // very narrow: shrink the cell rather than show one icon per row
                cols = 3;
                cell = Math.max(10, (area.w() - 2 * gap) / 3);
            }
            int rows = count <= 0 ? 0 : (count + cols - 1) / cols;
            return new Grid(area, cell, gap, cols, rows, Math.max(0, count));
        }

        /** Rows that are fully visible at once. */
        public int visibleRows() { return Math.max(1, (area.h() + gap) / (cell + gap)); }

        /** The most rows the grid can be scrolled down by. */
        public int maxScroll() { return Math.max(0, rows - visibleRows()); }

        /** The box of cell {@code index} when the grid is scrolled by {@code scrollRows} (it may lie outside the area when scrolled out). */
        public Rect cellRect(int index, int scrollRows) {
            int r = index / cols - scrollRows, c = index % cols;
            int usedW = cols * cell + (cols - 1) * gap;
            int x0 = area.x() + Math.max(0, (area.w() - usedW) / 2);
            return new Rect(x0 + c * (cell + gap), area.y() + r * (cell + gap), cell, cell);
        }

        /** The index of the cell under a point, or -1 (outside the area, in a gap, or past the last cell). */
        public int indexAt(double px, double py, int scrollRows) {
            if (!area.contains(px, py)) return -1;
            for (int i = 0; i < count; i++) {
                int r = i / cols - scrollRows;
                if (r < 0 || r >= visibleRows()) continue;
                if (cellRect(i, scrollRows).contains(px, py)) return i;
            }
            return -1;
        }

        public int clampScroll(int s) { return Math.max(0, Math.min(maxScroll(), s)); }
    }
}
