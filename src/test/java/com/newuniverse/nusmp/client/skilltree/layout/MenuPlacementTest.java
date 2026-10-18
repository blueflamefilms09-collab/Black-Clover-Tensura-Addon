package com.newuniverse.nusmp.client.skilltree.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: nothing about Tensura's own layout can be checked here, so the placement is built to be right WHATEVER the frame is: the panel always lies inside the window, never covers Tensura's frame when it is docked
 * beside or below it, lies inside the frame when it is "inside", and falls back to the window edge when the frame is missing or nonsense (what a failed reflection gives). These invariants are checked over a grid of
 * window sizes (every GUI scale of a 1080p screen and smaller), frame sizes and every configured mode, so a combination nobody pictured cannot put the panel off screen.
 */
class MenuPlacementTest {
    private static final int[][] WINDOWS = {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1280, 720}};
    private static final int[][] FRAME_SIZES = {{200, 150}, {256, 200}, {330, 210}, {400, 230}, {520, 300}, {700, 400}};
    private static final String[] WORDS = {"auto", "right", "left", "below", "inside", "garbage", null};

    private static MenuPlacement.Frame centered(int sw, int sh, int[] f) {
        int l = (sw - f[0]) / 2, t = (sh - f[1]) / 2;
        return new MenuPlacement.Frame(l, t, l + f[0], t + f[1]);
    }

    @Test
    void thePanelIsAlwaysInsideTheWindowAndNeverCoversTheFrameWhenDocked() {
        for (int[] win : WINDOWS) {
            for (int[] fs : FRAME_SIZES) {
                MenuPlacement.Frame f = centered(win[0], win[1], fs);
                if (!f.valid(win[0], win[1])) continue;
                MenuPlacement.Inner inner = new MenuPlacement.Inner(f.left() + 90, f.top() + 20, f.h() - 40);
                for (String word : WORDS) {
                    String ctx = win[0] + "x" + win[1] + " frame " + fs[0] + "x" + fs[1] + " " + word;
                    MenuPlacement.Result r = MenuPlacement.place(win[0], win[1], f, inner, word, 210);
                    assertTrue(r.panel().isInside(win[0], win[1], 0), ctx + " panel " + r.panel() + " leaves the window");
                    assertTrue(r.panel().w() >= 40 && r.panel().h() >= 60, ctx + " panel too small " + r.panel());
                    switch (r.mode()) {
                        case RIGHT, LEFT, BELOW -> assertFalse(r.panel().intersects(f.rect()), ctx + " docks " + r.mode() + " but covers the frame");
                        case INSIDE -> {
                            assertTrue(f.rect().inset(-1).contains(r.panel().x(), r.panel().y()) && f.rect().inset(-1).contains(r.panel().right() - 1, r.panel().bottom() - 1), ctx + " inside, but not inside the frame");
                            assertTrue(r.panel().x() >= inner.listRight(), ctx + " inside, but over the list");
                        }
                        case EDGE -> assertEquals(win[0] - MenuPlacement.MARGIN, r.panel().right(), ctx + " the edge fallback hugs the right edge");
                    }
                }
            }
        }
    }

    @Test
    void aWideWindowDocksBesideTheFrameAndTheWordChoosesTheMode() {
        MenuPlacement.Frame f = centered(1280, 720, new int[]{330, 210});
        assertEquals(MenuPlacement.Mode.RIGHT, MenuPlacement.place(1280, 720, f, null, "auto", 210).mode());
        assertEquals(MenuPlacement.Mode.LEFT, MenuPlacement.place(1280, 720, f, null, "left", 210).mode());
        assertEquals(MenuPlacement.Mode.BELOW, MenuPlacement.place(1280, 720, f, null, "below", 210).mode());
        MenuPlacement.Result right = MenuPlacement.place(1280, 720, f, null, "right", 210);
        assertEquals(f.right() + MenuPlacement.GAP, right.panel().x());
        assertEquals(210, right.panel().w());
        MenuPlacement.Inner inner = new MenuPlacement.Inner(f.left() + 100, f.top() + 10, f.h() - 20);
        assertEquals(MenuPlacement.Mode.INSIDE, MenuPlacement.place(1280, 720, f, inner, "inside", 210).mode());
        assertEquals(MenuPlacement.Mode.RIGHT, MenuPlacement.place(1280, 720, f, null, "inside", 210).mode(), "inside needs the list geometry; without it the next mode is used");
    }

    @Test
    void aMissingOrNonsenseFrameFallsBackToTheWindowEdge() {
        assertEquals(MenuPlacement.Mode.EDGE, MenuPlacement.place(854, 480, null, null, "auto", 210).mode());
        assertEquals(MenuPlacement.Mode.EDGE, MenuPlacement.place(854, 480, new MenuPlacement.Frame(0, 0, 0, 0), null, "auto", 210).mode(), "an unset static gives zeros");
        assertEquals(MenuPlacement.Mode.EDGE, MenuPlacement.place(854, 480, new MenuPlacement.Frame(-10, 0, 300, 200), null, "auto", 210).mode());
        assertEquals(MenuPlacement.Mode.EDGE, MenuPlacement.place(854, 480, new MenuPlacement.Frame(100, 100, 5000, 200), null, "auto", 210).mode(), "bigger than the window");
        MenuPlacement.Result r = MenuPlacement.place(854, 480, null, null, "auto", 210);
        assertTrue(r.panel().isInside(854, 480, 0));
        assertEquals(854 - MenuPlacement.MARGIN, r.panel().right());
    }

    @Test
    void theWidthIsClampedAndShrinksToFit() {
        assertEquals(MenuPlacement.MIN_W, MenuPlacement.place(1280, 720, null, null, "auto", 10).panel().w());
        assertEquals(MenuPlacement.MAX_W, MenuPlacement.place(1280, 720, null, null, "auto", 9999).panel().w());
        assertTrue(MenuPlacement.place(200, 150, null, null, "auto", 210).panel().w() <= 200 - 2 * MenuPlacement.MARGIN);
    }

    @Test
    void theAdminSwitchSitsBesideTheFrameAndNeverOverTheFrameOrThePanel() {
        for (int[] win : WINDOWS) {
            for (int[] fs : FRAME_SIZES) {
                MenuPlacement.Frame f = centered(win[0], win[1], fs);
                if (!f.valid(win[0], win[1])) continue;
                for (String word : WORDS) {
                    Rect panel = MenuPlacement.place(win[0], win[1], f, null, word, 210).panel();
                    Rect t = MenuPlacement.toggleRect(win[0], win[1], f, panel, 90, 14);
                    String ctx = win[0] + "x" + win[1] + " frame " + fs[0] + "x" + fs[1] + " " + word + " switch " + t;
                    assertTrue(t.isInside(win[0], win[1], 0), ctx + " leaves the window");
                    if (win[0] >= 427 && win[1] >= 240 && f.top() >= 16) {                       // room for the strip above the frame exists at every normal size
                        assertFalse(t.intersects(f.rect()), ctx + " covers the frame");
                        assertFalse(t.intersects(panel), ctx + " covers the panel");
                    }
                }
            }
        }
    }

    @Test
    void withoutAFrameTheSwitchGoesToACorner() {
        Rect t = MenuPlacement.toggleRect(854, 480, null, new Rect(600, 4, 210, 472), 90, 14);
        assertEquals(new Rect(MenuPlacement.MARGIN, MenuPlacement.MARGIN, 90, 14), t);
    }
}
