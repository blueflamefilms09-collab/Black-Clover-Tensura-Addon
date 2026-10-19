package com.newuniverse.nusmp.client.relics.djinn.gui;

/**
 * Where everything of the panel sits, in GUI pixels relative to the panel's top-left corner, and where the panel sits on the screen. Pure numbers.
 *
 * <pre>
 *  +------------------------------------------------------+
 *  | (  medallion  )  Name  -  title                       |
 *  | (   60 x 60   )  [pips] Phase 42s                     |
 *  | (  emblem and )  [==== Magoi ====]                    |
 *  | (    timer    )  [= assimilation =]                   |
 *  | (    ring     )  [==== timer =====]                   |
 *  |                  [lock] [cooldown]  Trust 40          |
 *  +------------------------------------------------------+
 * </pre>
 */
public final class GuiLayout {
    private GuiLayout() {}

    /** Where on the screen the panel is anchored. */
    public enum Anchor { BOTTOM_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT }

    public static final int W = 206, H = 68;
    /** The medallion: a disc of 60 pixels at (MED_X, MED_Y). */
    public static final int MED_X = 4, MED_Y = 4, MED = 60;
    public static final int MED_CX = MED_X + MED / 2, MED_CY = MED_Y + MED / 2;
    /** The text and bar column. */
    public static final int COL_X = 70, COL_W = W - COL_X - 7;
    public static final int TITLE_Y = 5, PIPS_Y = 16, MAGOI_Y = 27, MAGOI_H = 7, ASSIM_Y = 36, ASSIM_H = 3, TIMER_Y = 41, TIMER_H = 5,
            LOCK_Y = 48, COOL_Y = 51, THIN_H = 2, FOOT_Y = 55;
    /** Pixels the screen edge and the hotbar status icons are kept clear. */
    public static final int EDGE = 4, LIFT = 62;

    /** The panel's x for a screen of the given GUI width. */
    public static int originX(Anchor a, int guiW) {
        int x = switch (a == null ? Anchor.BOTTOM_CENTER : a) {
            case BOTTOM_LEFT -> EDGE;
            case BOTTOM_RIGHT -> guiW - W - EDGE;
            default -> (guiW - W) / 2;
        };
        return Math.max(0, x);
    }

    /** The panel's y: above the status icons at the bottom centre, in the corner otherwise. */
    public static int originY(Anchor a, int guiH) {
        int y = (a == null || a == Anchor.BOTTOM_CENTER) ? guiH - LIFT - H : guiH - H - EDGE;
        return Math.max(0, y);
    }

    /** The width of a bar's filled part. */
    public static int fillWidth(int w, float frac) {
        if (!(frac > 0f)) return 0;
        return Math.min(w, Math.round(w * Math.min(1f, frac)));
    }
}
