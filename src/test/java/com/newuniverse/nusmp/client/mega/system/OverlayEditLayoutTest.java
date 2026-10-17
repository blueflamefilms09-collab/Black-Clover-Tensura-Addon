package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: 1.58.0 changed the layout in three ways that nobody can see until the game runs: the window is placed by the player's offset (so it must still never leave
 * the screen, at any corner, scale or size), the Hub shows a status window with no threat number (the row must still add up), and edit mode draws a frame and a hint
 * box over it (the box and its wrapped lines must stay on the screen wherever the window is, and never cover the window when there is room). These tests run the real
 * layout class over the same grid of screens, corners and fonts as the older layout tests.
 */
class OverlayEditLayoutTest {
    private static final ToIntFunction<String> SIX = s -> s.length() * 6;
    private static final ToIntFunction<String> EIGHT = s -> s.length() * 8;

    private static OverlayLayout.Panel panel(int floor, int road, int threat) {
        OverlayLayout.Stats st = new OverlayLayout.Stats(new String[]{"STR", "AGI", "VIT", "INT", "SENSE"}, new int[]{3, 1, 0, 2, 0});
        OverlayLayout.Quest q = new OverlayLayout.Quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Defeat 5 monsters on this floor", -1, false, "UNHARMED", "COMPLETE", "FAILED", "",
                2, 5, "TIME", -1, 100, false);
        return new OverlayLayout.Panel("[ SYSTEM ]", "FLOOR", floor, road, road == 2 ? "ABYSSAL" : "HOLY", "THREAT", threat, false, "BOSS FLOOR: NO QUEST", "NO QUEST ON THIS FLOOR",
                threat == 0 ? null : q, st);
    }

    private static final List<String> HINT = List.of("EDIT MODE  TOP LEFT  X 6  Y 6",
            "Arrow keys move 1 px (Shift: 10)   End corner   Home reset   U done", "Open the chat or the inventory to drag the window with the mouse.");

    private static OverlayLayout.View view(int w, int h, float scale, int anchor, int offX, int offY) {
        return new OverlayLayout.View(w, h, scale, anchor, 0.67f, 12f, offX, offY);
    }

    // ---------------------------------------------------------------- the player's offset
    @Test void theWindowStaysOnTheScreenForEveryCornerScaleSizeAndOffset() {
        int[][] screens = {{854, 480}, {640, 360}, {427, 240}, {320, 180}};
        int[] offs = {-5000, -20, 0, 15, 120, 4096};
        for (ToIntFunction<String> font : List.of(SIX, EIGHT)) {
            OverlayLayout lay = new OverlayLayout(font);
            for (int[] sc : screens) {
                for (float scale : new float[]{0.5f, 1f, 2f}) {
                    for (int anchor = 0; anchor < 4; anchor++) {
                        for (int ox : offs) {
                            for (int oy : offs) {
                                OverlayLayout.View v = view(sc[0], sc[1], scale, anchor, ox, oy);
                                OverlayLayout.Frame f = lay.panel(panel(12, 1, 12), v);
                                assertTrue(f.x() >= 0 && f.y() >= 0, "off the top-left: " + f + " anchor " + anchor + " offset " + ox + "," + oy);
                                if (f.w() <= v.w()) assertTrue(f.x() + f.w() <= v.w(), "off the right: " + f + " on " + v.w());
                                if (f.h() <= v.h()) assertTrue(f.y() + f.h() <= v.h(), "off the bottom: " + f + " on " + v.h());
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void theOffsetMovesTheWindowTheWayTheMathSays() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame home = lay.panel(panel(3, 1, 3), view(640, 360, 1f, 0, 0, 0));
        OverlayLayout.Frame moved = lay.panel(panel(3, 1, 3), view(640, 360, 1f, 0, 25, 40));
        assertEquals(home.x() + 25, moved.x());
        assertEquals(home.y() + 40, moved.y());
        assertEquals(home.w(), moved.w());
        OverlayLayout.Frame right = lay.panel(panel(3, 1, 3), view(640, 360, 1f, 3, 25, 40));
        assertEquals(640 - right.w() - OverlayStyle.MARGIN - 25, right.x());
        assertEquals(360 - right.h() - OverlayStyle.MARGIN - 40, right.y());
    }

    @Test void everyMarkMovesWithTheWindow() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame a = lay.panel(panel(3, 1, 3), view(640, 360, 1f, 0, 0, 0));
        OverlayLayout.Frame b = lay.panel(panel(3, 1, 3), view(640, 360, 1f, 0, 30, 20));
        assertEquals(a.ops().size(), b.ops().size());
        for (int i = 0; i < a.ops().size(); i++) {
            Op p = a.ops().get(i), q = b.ops().get(i);
            if (p instanceof Op.Fill f) assertEquals(f.x1() + 30, ((Op.Fill) q).x1(), "mark " + i);
            else if (p instanceof Op.Text t) assertEquals(t.y() + 20, ((Op.Text) q).y(), "mark " + i);
        }
    }

    @Test void theOlderSixArgumentViewIsTheWindowInItsCorner() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame old = lay.panel(panel(3, 1, 3), new OverlayLayout.View(640, 360, 1f, 1, 0.67f, 12f));
        OverlayLayout.Frame now = lay.panel(panel(3, 1, 3), view(640, 360, 1f, 1, 0, 0));
        assertEquals(now.x(), old.x());
        assertEquals(now.y(), old.y());
    }

    // ---------------------------------------------------------------- the Hub
    @Test void theHubWindowHasNoThreatNumberAndStillFitsItsPanel() {
        for (ToIntFunction<String> font : List.of(SIX, EIGHT)) {
            OverlayLayout lay = new OverlayLayout(font);
            OverlayLayout.Frame f = lay.panel(new OverlayLayout.Panel("[ SYSTEM ]", "FLOOR", 0, 0, "HOLY", "THREAT", 0, false, "BOSS", "SANCTUARY: NO QUESTS HERE", null,
                    new OverlayLayout.Stats(new String[]{"STR", "AGI", "VIT", "INT", "SENSE"}, new int[]{1, 2, 3, 4, 5})), view(640, 360, 1f, 0, 0, 0));
            boolean floorZero = false;
            for (Op op : f.ops()) {
                if (op instanceof Op.Text t) {
                    assertFalse(t.text().contains("THREAT"), "the Hub has no threat: " + t);
                    assertTrue(t.x() >= f.x() && t.x() + font.applyAsInt(t.text()) <= f.x() + f.w(), "text outside the panel: " + t);
                    if (t.text().equals("0")) floorZero = true;
                }
            }
            assertTrue(floorZero, "the floor number 0 is shown");
        }
    }

    @Test void aFloorWithAThreatStillShowsIt() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame f = lay.panel(panel(5, 1, 5), view(640, 360, 1f, 0, 0, 0));
        boolean threat = false;
        for (Op op : f.ops()) if (op instanceof Op.Text t && t.text().equals("THREAT")) threat = true;
        assertTrue(threat);
    }

    // ---------------------------------------------------------------- edit mode
    private static Op.Fill hintBox(List<Op> ops) {
        for (Op op : ops) if (op instanceof Op.Fill f && (f.argb() & 0xFFFFFF) == OverlayStyle.TOAST_BG) return f;
        return null;
    }

    @Test void theEditMarksAndTheHintBoxStayOnTheScreen() {
        int[][] screens = {{854, 480}, {640, 360}, {427, 240}, {320, 180}};
        for (ToIntFunction<String> font : List.of(SIX, EIGHT)) {
            OverlayLayout lay = new OverlayLayout(font);
            for (int[] sc : screens) {
                for (int anchor = 0; anchor < 4; anchor++) {
                    for (int off : new int[]{0, 60, 4096}) {
                        OverlayLayout.View v = view(sc[0], sc[1], 1f, anchor, off, off);
                        OverlayLayout.Frame f = lay.panel(panel(12, 1, 12), v);
                        List<Op> marks = lay.edit(f, v, HINT).ops();
                        assertFalse(marks.isEmpty());
                        Op.Fill box = hintBox(marks);
                        assertNotNull(box, "the hint box is there");
                        assertTrue(box.x1() >= 0 && box.y1() >= 0 && box.x2() <= v.w() && box.y2() <= v.h(), "hint box off screen: " + box + " on " + v.w() + "x" + v.h() + " anchor " + anchor);
                        for (Op op : marks) {
                            if (op instanceof Op.Text t) {
                                assertTrue(t.x() >= box.x1() && t.x() + font.applyAsInt(t.text()) <= box.x2(), "text leaves the box: " + t + " box " + box);
                                assertTrue(t.y() >= box.y1() && t.y() + OverlayStyle.TEXT_H <= box.y2(), "text above or below the box: " + t + " box " + box);
                                assertTrue(OverlayMath.drawable(t.argb()));
                            }
                            if (op instanceof Op.Fill fl) assertTrue(fl.x2() > fl.x1() && fl.y2() > fl.y1(), "empty fill " + fl);
                        }
                    }
                }
            }
        }
    }

    @Test void theHintBoxDoesNotCoverTheWindowWhenThereIsRoomForIt() {
        OverlayLayout lay = new OverlayLayout(SIX);
        for (int anchor = 0; anchor < 4; anchor++) {
            OverlayLayout.View v = view(854, 480, 1f, anchor, 0, 0);
            OverlayLayout.Frame f = lay.panel(panel(12, 1, 12), v);
            Op.Fill box = hintBox(lay.edit(f, v, HINT).ops());
            boolean apart = box.y1() >= f.y() + f.h() || box.y2() <= f.y() || box.x1() >= f.x() + f.w() || box.x2() <= f.x();
            assertTrue(apart, "the hint box " + box + " covers the window " + f + " at corner " + anchor);
        }
    }

    @Test void theHintBoxGoesBelowAWindowAtTheTopAndAboveAWindowAtTheBottom() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.View top = view(854, 480, 1f, 0, 0, 0), bottom = view(854, 480, 1f, 2, 0, 0);
        OverlayLayout.Frame ft = lay.panel(panel(12, 1, 12), top), fb = lay.panel(panel(12, 1, 12), bottom);
        assertTrue(hintBox(lay.edit(ft, top, HINT).ops()).y1() >= ft.y() + ft.h());
        assertTrue(hintBox(lay.edit(fb, bottom, HINT).ops()).y2() <= fb.y());
    }

    @Test void aLongHintLineIsWrappedNotCutOffTheScreen() {
        OverlayLayout lay = new OverlayLayout(EIGHT);
        OverlayLayout.View v = view(320, 180, 1f, 0, 0, 0);
        OverlayLayout.Frame f = lay.panel(panel(12, 1, 12), v);
        List<Op> marks = lay.edit(f, v, HINT).ops();
        int texts = 0;
        for (Op op : marks) if (op instanceof Op.Text) texts++;
        assertTrue(texts > HINT.size(), "the long lines were wrapped into more lines: " + texts);
        Op.Fill box = hintBox(marks);
        assertTrue(box.x2() <= v.w());
    }

    @Test void withNoHintLinesOnlyTheFrameIsDrawn() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.View v = view(640, 360, 1f, 0, 0, 0);
        OverlayLayout.Frame f = lay.panel(panel(3, 1, 3), v);
        List<Op> marks = lay.edit(f, v, new ArrayList<>()).ops();
        assertEquals(8, marks.size(), "four edges and four corner handles");
        assertNull(hintBox(marks));
        for (Op op : marks) assertTrue(op instanceof Op.Fill);
    }

    @Test void theFrameSurroundsTheWindowWithoutLeavingTheScreen() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.View v = view(640, 360, 1f, 0, 0, 0);
        OverlayLayout.Frame f = lay.panel(panel(3, 1, 3), v);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Op op : lay.edit(f, v, new ArrayList<>()).ops()) {
            Op.Fill fl = (Op.Fill) op;
            minX = Math.min(minX, fl.x1());
            minY = Math.min(minY, fl.y1());
            maxX = Math.max(maxX, fl.x2());
            maxY = Math.max(maxY, fl.y2());
        }
        assertTrue(minX <= f.x() - 2 && minY <= f.y() - 2 && maxX >= f.x() + f.w() + 2 && maxY >= f.y() + f.h() + 2, "the frame encloses the window: " + minX + "," + minY + " to " + maxX + "," + maxY);
    }

    @Test void theResultCarriesTheHintBoxRectangleForTheToastsToKeepClearOf() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.View v = view(854, 480, 1f, 0, 0, 0);
        OverlayLayout.Frame f = lay.panel(panel(12, 1, 12), v);
        OverlayLayout.EditMarks m = lay.edit(f, v, HINT);
        Op.Fill box = hintBox(m.ops());
        assertEquals(box.x1(), m.x());
        assertEquals(box.y1(), m.y());
        assertEquals(box.x2() - box.x1(), m.w());
        assertEquals(box.y2() - box.y1(), m.h());
        OverlayLayout.EditMarks none = lay.edit(f, v, new ArrayList<>());
        assertEquals(0, none.w());
        assertEquals(0, none.h());
    }

    @Test void theFramePulsesButNeverDisappears() {
        OverlayLayout lay = new OverlayLayout(SIX);
        for (float time = 0; time < 100; time += 3.7f) {
            OverlayLayout.View v = new OverlayLayout.View(640, 360, 1f, 0, 0.67f, time, 0, 0);
            OverlayLayout.Frame f = lay.panel(panel(3, 1, 3), v);
            assertEquals(8, lay.edit(f, v, new ArrayList<>()).ops().size(), "time " + time);
        }
    }
}
