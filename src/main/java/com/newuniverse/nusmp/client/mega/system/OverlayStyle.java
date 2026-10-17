package com.newuniverse.nusmp.client.mega.system;

/**
 * Why: every number that decides how the System window LOOKS (its size, its spacing, its colours) is a taste the owner will want to nudge after the first look, and
 * the brief fixes a few of them ("a translucent dark-cyan panel about 0xAA06222E, a 1 px bright cyan border 0xFF3DE0FF, four corner brackets, faint scanlines").
 * Keeping them all in one pure class, with a line of reason beside each, means a taste change is one edit in one place, the layout reads like a description
 * instead of a pile of literals, and the preview script draws the same numbers because it runs the same layout. Colours are written as RGB (no alpha): the layout
 * applies the player's opacity setting, because what is see-through (the panel) and what must stay crisp (text, lines) differ. The game's drawing calls take ARGB ints.
 * Pure.
 */
public final class OverlayStyle {
    private OverlayStyle() {}

    // ================================================================ sizes (GUI pixels, before the player's scale)
    /** The narrowest the panel goes: five stat cells of 30 pixels, and the info row fits with room to spare. It grows only when a row of the panel needs more. */
    public static final int PANEL_MIN_W = 164;
    /** Space between the border and the text, left and right. */
    public static final int PAD_X = 7;
    public static final int PAD_TOP = 6, PAD_BOTTOM = 6;
    /** Distance from the screen edge. */
    public static final int MARGIN = 6;
    /** The game's font is 8 pixels tall; a line of text takes this much with its spacing. */
    public static final int TEXT_H = 8, LINE = 10;
    /** Space between one block of the panel and the next, round a divider. */
    public static final int GAP = 4;
    /** The most lines a quest title may take. */
    public static final int MAX_TITLE_LINES = 3;
    /** The progress bar: height, space between segments, and the room kept at its right for the count or the word COMPLETE / FAILED. */
    public static final int BAR_H = 6, BAR_GAP = 2, BAR_RESERVE = 54;
    /** The thin bar that drains while a timed quest runs. */
    public static final int CLOCK_H = 2;
    /** The five stat cells of the bottom row. */
    public static final int STAT_CELLS = 5, STAT_CELL_W = 30;
    /** The corner brackets: how long an arm is and how thick. */
    public static final int BRACKET_LEN = 7, BRACKET_THICK = 2;
    /** Scanlines: one every this many pixels; the sweep band is this tall and takes this many ticks to cross the panel. */
    public static final int SCAN_SPACING = 5, SWEEP_H = 12, SWEEP_TICKS = 170;

    /** Edit mode (1.58.0): the room round the hint box's text, the side of the square handles on the frame's corners, and the width the toasts' column is assumed to take. */
    public static final int EDIT_PAD = 4, EDIT_HANDLE = 3, TOAST_COLUMN_W = 220;

    /** Toasts: height of one, space between two, the most shown at once, and the room its text keeps on each side. */
    public static final int TOAST_H = 15, TOAST_GAP = 3, TOAST_MAX = 3, TOAST_PAD = 8, TOAST_MIN_W = 96;

    // ================================================================ colours (RGB)
    /** The border, the lit part of the bar: the brief's bright cyan. */
    public static final int CYAN = 0x3DE0FF;
    /** The corner brackets and the header: a paler cyan so they read as the brightest thing on the panel. */
    public static final int CYAN_BRIGHT = 0x9BF3FF;
    /** Labels (FLOOR, THREAT, STR ...). */
    public static final int CYAN_DIM = 0x6FB6C8;
    /** A stat at 0 points, the unlit bar: there, but not shouting. */
    public static final int CYAN_FAINT = 0x3F7585;
    /** The main text. */
    public static final int TEXT = 0xE8FBFF;
    /** The panel's fill, top and bottom: the brief's 0x06222E, a little brighter at the top so the panel has depth. */
    public static final int BG_TOP = 0x0B3344, BG_BOTTOM = 0x051B26;
    /** A toast's fill. */
    public static final int TOAST_BG = 0x06202C;
    /** The Holy road, the Abyssal road. */
    public static final int GOLD = 0xFFE08A, VIOLET = 0xB48CFF;
    public static final int GREEN = 0x4DFF9A, GREEN_TEXT = 0x9BFFC8;
    public static final int RED = 0xFF5A6E, RED_TEXT = 0xFF9AA8;
    public static final int AMBER = 0xFFC24D;
    /** The two ghost copies of the header during a glitch frame: the colour split of a bad signal. */
    public static final int GLITCH_A = 0xFF3D6E, GLITCH_B = 0x3DE0FF;
    /** The scanlines and the sweep. */
    public static final int SCAN = 0xBFEFFF;

    // ================================================================ alpha rules
    /** The opacity of the lines and text for a given panel opacity: never as thin as the fill, or the text would vanish at the low end of the setting. */
    public static float crisp(float alpha) { return 0.55f + 0.45f * Math.max(0f, Math.min(1f, alpha)); }

    // ================================================================ colour tables
    /** The colour of the threat number: calm to alarming as the floors get deeper. */
    public static int threatColour(int threat) {
        if (threat <= 5) return 0x7CFFB0;
        if (threat <= 12) return 0xFFE066;
        if (threat <= 25) return 0xFFA040;
        return 0xFF5A5A;
    }

    /** The colour of the road word: gold for the Holy road, violet for the Abyssal one, plain for none. {@code road}: 1 holy, 2 abyssal. */
    public static int roadColour(int road) { return road == 1 ? GOLD : road == 2 ? VIOLET : TEXT; }

    /** The accent of a toast by its kind code (see {@code ToastKind}): 0 information, 1 success, 2 failure, 3 reward, 4 shadow. An unknown code is information. */
    public static int toastAccent(int kind) {
        return switch (kind) {
            case 1 -> GREEN;
            case 2 -> RED;
            case 3 -> 0xFFD25A;
            case 4 -> 0xB06CFF;
            default -> CYAN;
        };
    }

    /** The colour of a toast's message text by its kind code: the accent, lightened. */
    public static int toastText(int kind) {
        return switch (kind) {
            case 1 -> 0xBFFFD9;
            case 2 -> 0xFFC4CC;
            case 3 -> 0xFFF0C0;
            case 4 -> 0xE6D2FF;
            default -> TEXT;
        };
    }

    /** The colour of the clock row: calm, then amber under a minute, then red under twenty seconds. */
    public static int clockColour(int ticksLeft) {
        if (ticksLeft <= 20 * 20) return RED;
        if (ticksLeft <= 60 * 20) return AMBER;
        return CYAN_BRIGHT;
    }
}
