package com.newuniverse.nusmp.client.skilltree.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner wants the Skill Tree inside the menu the player already uses, with no keybind, and the existing menus must stay exactly as they are (add the new, keep the old). The
 * button is therefore added from outside, by a screen event, and nothing in those screens knows it exists, so its position has to be derived from what they draw. Each menu lays itself
 * out from its window size with a few constants that are package-private to {@code client.multiverse} and {@code client.ui}; this class mirrors those formulas and puts the button in
 * the one place each menu leaves free (the strip under the Codex's book beside "Grimoire Slot", the header row beside "Status" in the Four Kingdoms panel, the top-right corner of the
 * status screen), then checks the result against what is really on the screen: it must lie fully inside the window and touch no widget the menu has registered, and when the mirrored
 * spot fails either test (a menu was redesigned, the window is odd) the next candidate is a corner of the window, so the button is always reachable and never on top of a control.
 * Pure on purpose: the arithmetic is the least testable part of the injection in the real game, so it is tested here at every window size from 320 x 240 to 3840 x 2160 and against the
 * GUI scale rule, and a source-scan test fails the build if a menu's constants change so the mirror cannot go stale silently.
 */
public final class ButtonPlacement {
    private ButtonPlacement() {}

    /** The menus the button is added to. */
    public enum Menu {
        /** {@code client.ui.CodexScreen}: the one menu the Four Kingdoms slot and the Multiverse key open since 1.11.0. */
        CODEX,
        /** {@code client.multiverse.FourKingdomsScreen}: the older Four Kingdoms panel (kept in the code, reachable only if something opens it). */
        FOUR_KINGDOMS,
        /** {@code client.multiverse.MultiverseStatusScreen}: the status screen the Four Kingdoms panel opens. */
        STATUS
    }

    public static final int BUTTON_H = 16, MIN_W = 56, MAX_W = 96, GAP = 4;
    /** The button must keep at least this many pixels between itself and the window edge. */
    public static final int EDGE = 2;

    // Mirrors of the host menus' own numbers (verified by ButtonPlacementTest against their source text).
    static final int CODEX_SIDE = 56, CODEX_MAX_W = 440, CODEX_TOP_BOTTOM = 48, CODEX_MAX_H = 260, CODEX_SHIFT = 10, CODEX_FOOT = 20;
    static final int CODEX_SLOT_FROM_RIGHT = 94, CODEX_STRIP_DROP = 4;
    static final int FK_PANEL_W = 340, FK_PANEL_H = 236, FK_STATUS_FROM_RIGHT = 64, FK_ROW_TOP = 8;
    static final int STATUS_MARGIN = 8, STATUS_TOP = 4;

    /** The button width for a label of {@code labelWidth} pixels: the label plus padding, kept between {@link #MIN_W} and {@link #MAX_W}. */
    public static int buttonWidth(int labelWidth) { return Math.max(MIN_W, Math.min(MAX_W, labelWidth + 16)); }

    /**
     * Where to put the button in {@code menu} for a window of {@code sw} by {@code sh} GUI pixels.
     *
     * @param labelWidth the label's width in pixels (the font's, at run time)
     * @param occupied   the rectangles of the widgets the menu already has (read from the screen's own widget list)
     */
    public static Rect place(Menu menu, int sw, int sh, int labelWidth, List<Rect> occupied) {
        int w = buttonWidth(labelWidth);
        List<Rect> candidates = new ArrayList<>();
        switch (menu) {
            case CODEX -> {
                int bw = Math.min(sw - CODEX_SIDE, CODEX_MAX_W), bh = Math.min(sh - CODEX_TOP_BOTTOM, CODEX_MAX_H);
                int bx = (sw - bw) / 2 - CODEX_SHIFT, by = (sh - bh - CODEX_FOOT) / 2;
                candidates.add(new Rect(bx + bw - CODEX_SLOT_FROM_RIGHT - GAP - w, by + bh + CODEX_STRIP_DROP, w, BUTTON_H));
            }
            case FOUR_KINGDOMS -> {
                int left = (sw - FK_PANEL_W) / 2, top = (sh - FK_PANEL_H) / 2;
                candidates.add(new Rect(left + FK_PANEL_W - FK_STATUS_FROM_RIGHT - GAP - w, top + FK_ROW_TOP, w, BUTTON_H));
            }
            case STATUS -> candidates.add(new Rect(sw - w - STATUS_MARGIN, STATUS_TOP, w, BUTTON_H));
        }
        int far = 4;                                                     // the corners, tried when the mirrored spot is not usable
        candidates.add(new Rect(sw - w - far, far, w, BUTTON_H));
        candidates.add(new Rect(sw - w - far, sh - BUTTON_H - far, w, BUTTON_H));
        candidates.add(new Rect(far, far, w, BUTTON_H));
        candidates.add(new Rect(far, sh - BUTTON_H - far, w, BUTTON_H));
        for (Rect c : candidates) if (usable(c, sw, sh, occupied)) return c;
        int x = Math.max(0, Math.min(sw - w, sw - w - far));              // nothing is free: the top-right corner, kept inside the window
        return new Rect(x, far, w, BUTTON_H);
    }

    private static boolean usable(Rect r, int sw, int sh, List<Rect> occupied) {
        if (!r.isInside(sw, sh, EDGE)) return false;
        Rect padded = r.inset(-1);                                        // keep a one pixel gap to every widget
        for (Rect o : occupied) if (padded.intersects(o)) return false;
        return true;
    }
}
