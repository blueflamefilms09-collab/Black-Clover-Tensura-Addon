package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;

/**
 * Why: the Weapons tab of the hub (the Weapon page redrawn in the hub's style) must put every chip, the head card, every option row and every mastery bar inside its width with nothing overlapping, for any number of
 * weapons (up to 6), options (up to 24), bars (up to 4) and other mastery tracks, and scroll when it is taller than the body. The old two-column page could overlap on a narrow spread; this one is a single column of
 * sections stacked from the top, so a section can only start where the one above ends. The rectangles are computed here by plain integer arithmetic with no Minecraft types and tested for a grid of widths and counts
 * ({@code HubWeaponLayoutTest}); the tab only reads them. All y values are from the top of the scrolled content (the screen subtracts the scroll). A record: the tab keeps the current one and rebuilds it only when
 * the width or a count changes.
 */
public record HubWeaponLayout(int w, int chips, int options, int bars, int extras) {
    public static final int CHIP_H = 14, CHIP_GAP = 3, MIN_CHIP_W = 44, MAX_CHIP_W = 96, HEAD_H = 46, ROW_H = 14, BAR_H = 20, HEADING_H = 13, GAP = 6;

    public static HubWeaponLayout of(int w, int chips, int options, int bars, int extras) {
        return new HubWeaponLayout(Math.max(MIN_CHIP_W, w), Math.max(0, chips), Math.max(0, options), Math.max(0, bars), Math.max(0, extras));
    }

    // ---------------------------------------------------------------- chips (only when there are two or more weapons)
    public boolean hasChips() { return chips > 1; }

    /** How many chips share one row (at least one, at most all of them). */
    public int chipsPerRow() { return Math.max(1, Math.min(Math.max(1, chips), (w + CHIP_GAP) / (MIN_CHIP_W + CHIP_GAP))); }

    public int chipRows() { return hasChips() ? (chips + chipsPerRow() - 1) / chipsPerRow() : 0; }

    public int chipW() { return Math.max(MIN_CHIP_W, Math.min(MAX_CHIP_W, (w - CHIP_GAP * (chipsPerRow() - 1)) / chipsPerRow())); }

    public int chipsH() { return hasChips() ? chipRows() * CHIP_H + (chipRows() - 1) * CHIP_GAP : 0; }

    public Rect chip(int i) {
        int per = chipsPerRow();
        return new Rect((i % per) * (chipW() + CHIP_GAP), (i / per) * (CHIP_H + CHIP_GAP), chipW(), CHIP_H);
    }

    // ---------------------------------------------------------------- sections, top to bottom
    public int headY() { return hasChips() ? chipsH() + GAP : 0; }

    public Rect head() { return new Rect(0, headY(), w, HEAD_H); }

    public int optionsHeadingY() { return headY() + HEAD_H + GAP; }

    public Rect option(int i) { return new Rect(0, optionsHeadingY() + HEADING_H + i * ROW_H, w, ROW_H - 1); }

    private int optionsEnd() { return options > 0 ? optionsHeadingY() + HEADING_H + options * ROW_H : headY() + HEAD_H; }

    public int barsHeadingY() { return optionsEnd() + GAP; }

    /** The slot of mastery bar {@code i}: the label line is its top 10 units, the bar the next 5. */
    public Rect bar(int i) { return new Rect(0, barsHeadingY() + HEADING_H + i * BAR_H, w, BAR_H - 3); }

    private int barsEnd() { return bars > 0 ? barsHeadingY() + HEADING_H + bars * BAR_H : optionsEnd(); }

    public int extrasHeadingY() { return barsEnd() + GAP; }

    public Rect extra(int i) { return new Rect(0, extrasHeadingY() + HEADING_H + i * BAR_H, w, BAR_H - 3); }

    /** The content height: the bottom of the last section. */
    public int height() {
        int end = extras > 0 ? extrasHeadingY() + HEADING_H + extras * BAR_H : barsEnd();
        return end + GAP;
    }

    // ---------------------------------------------------------------- clicks (content coordinates)
    /** The chip under the point, or -1. */
    public int chipAt(double x, double y) {
        if (!hasChips()) return -1;
        for (int i = 0; i < chips; i++) if (chip(i).contains(x, y)) return i;
        return -1;
    }

    /** The option row under the point, or -1. */
    public int optionAt(double x, double y) {
        for (int i = 0; i < options; i++) if (option(i).contains(x, y)) return i;
        return -1;
    }
}
