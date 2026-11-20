package com.newuniverse.nusmp.client.weaponpage;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;

/**
 * Why: the Weapon page of the Codex draws inside whatever spread the Codex gives it (about 190 by 220 GUI pixels on a small window up to 400 by 220), scrolls when it is taller, and must put every chip, row
 * and bar inside it without overlap, for any number of weapons, options and bars. The rectangles are computed here by plain integer arithmetic with no Minecraft types, so a test can check them for a grid of
 * widths and counts ({@code WeaponPageLayoutTest}); the page only reads them. Left column: the weapon's head (name, grade, level, form) and its option rows. Right column: its mastery bars, then the other
 * mastery tracks. Above both, one chip per weapon when the player owns more than one. A record; the page keeps the current one and rebuilds it only when its area changes, and draws from the int
 * accessors, so a frame builds no rectangle.
 */
public record WeaponPageLayout(int x, int y, int w, int leftW, int rightX, int rightW) {
    public static final int CHIP_H = 14, HEAD_H = 40, ROW_H = 13, BAR_H = 18, HEADING_H = 12, GAP = 8, CHIP_GAP = 3, MAX_CHIP_W = 84, MIN_CHIP_W = 18;

    /** The layout of a page whose area starts at (x, y) and is {@code w} wide. */
    public static WeaponPageLayout of(int x, int y, int w) {
        int width = Math.max(2 * 40 + GAP, w);
        int leftW = (width - GAP) / 2;
        return new WeaponPageLayout(x, y, width, leftW, x + leftW + GAP, width - leftW - GAP);
    }

    // The page draws from these int answers (no object per row per frame); the Rect forms below are for the click and the tests, and are built from the same numbers.

    /** The width of one chip out of {@code n}: they share the width, none wider than {@link #MAX_CHIP_W}. */
    public int chipW(int n) { return Math.max(MIN_CHIP_W, Math.min(MAX_CHIP_W, (w - CHIP_GAP * (Math.max(1, n) - 1)) / Math.max(1, n))); }

    public int chipX(int i, int n) { return x + i * (chipW(n) + CHIP_GAP); }

    public int optionY(int i, int chips) { return bodyY(chips) + HEAD_H + GAP + i * ROW_H; }

    public int barY(int i, int chips) { return bodyY(chips) + HEADING_H + i * BAR_H; }

    public int extraY(int i, int bars, int chips) { return bodyY(chips) + HEADING_H + bars * BAR_H + HEADING_H + i * BAR_H; }

    /** The chip of weapon {@code i} out of {@code n}. */
    public Rect chip(int i, int n) { return new Rect(chipX(i, n), y, chipW(n), CHIP_H); }

    /** The top of the body: under the chips when there are two or more weapons, at the top of the page otherwise. */
    public int bodyY(int chips) { return chips > 1 ? y + CHIP_H + GAP : y; }

    public Rect head(int chips) { return new Rect(x, bodyY(chips), leftW, HEAD_H); }

    /** The row of option {@code i} (left column, under the head). */
    public Rect option(int i, int chips) { return new Rect(x, optionY(i, chips), leftW, ROW_H - 1); }

    /** The slot of mastery bar {@code i} (right column): the label line is the top 10 pixels, the bar the next 5. */
    public Rect bar(int i, int chips) { return new Rect(rightX, barY(i, chips), rightW, BAR_H - 3); }

    /** The slot of other-track bar {@code i} after {@code bars} weapon bars and a heading. */
    public Rect extra(int i, int bars, int chips) { return new Rect(rightX, extraY(i, bars, chips), rightW, BAR_H - 3); }

    /** The y of the heading above the weapon bars, and above the other tracks. */
    public int barsHeadingY(int chips) { return bodyY(chips); }

    public int extraHeadingY(int bars, int chips) { return bodyY(chips) + HEADING_H + bars * BAR_H; }

    /** The y extent the page wants (from the top of the area) for this many chips, options, bars and other tracks. */
    public int height(int chips, int options, int bars, int extras) {
        int top = bodyY(chips) - y;
        int left = HEAD_H + (options > 0 ? GAP + options * ROW_H : 0);
        int right = HEADING_H + bars * BAR_H + (extras > 0 ? HEADING_H + extras * BAR_H : 0);
        return top + Math.max(left, right);
    }

    /** The chip under the point, or -1. */
    public int chipAt(double mx, double my, int n) {
        for (int i = 0; i < n; i++) if (chip(i, n).contains(mx, my)) return i;
        return -1;
    }

    /** The option row under the point, or -1. */
    public int optionAt(double mx, double my, int options, int chips) {
        if (mx < x || mx >= x + leftW) return -1;
        int top = bodyY(chips) + HEAD_H + GAP;
        if (my < top) return -1;
        int i = (int) ((my - top) / ROW_H);
        return i < options && option(i, chips).contains(mx, my) ? i : -1;
    }
}
