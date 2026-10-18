package com.newuniverse.nusmp.client.skilltree.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the screen's five regions and its buttons must never overlap and never leave the panel, at ANY GUI size the game can produce, because an overlap is invisible in code review and
 * only shows as a button that cannot be clicked on somebody's window. The sizes come from the game's own scale rule ({@link GuiScales}), not from round numbers, and the reference
 * size 427 x 240 is pinned number by number so that a change of a constant is a deliberate act.
 */
class ScreenLayoutTest {

    private static boolean within(Rect inner, Rect outer) {
        return inner.x() >= outer.x() && inner.y() >= outer.y() && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    @Test
    void everyGuiSizeGivesDisjointRegionsInsideThePanel() {
        List<int[]> sizes = GuiScales.allSizes();
        assertTrue(sizes.size() >= 20, "the size table must cover many sizes, got " + sizes.size());
        for (int[] s : sizes) {
            String at = s[0] + "x" + s[1];
            ScreenLayout l = ScreenLayout.of(s[0], s[1]);
            assertTrue(l.panel.isInside(s[0], s[1], ScreenLayout.MARGIN), "panel inside the window at " + at);
            List<Rect> regions = List.of(l.header, l.routes, l.strip, l.canvas, l.detail, l.footer);
            for (Rect r : regions) {
                assertFalse(r.isEmpty(), "region not empty at " + at);
                assertTrue(within(r, l.panel), "region inside the panel at " + at + ": " + r);
            }
            for (int i = 0; i < regions.size(); i++)
                for (int j = i + 1; j < regions.size(); j++) assertFalse(regions.get(i).intersects(regions.get(j)), "regions " + i + " and " + j + " overlap at " + at);
            assertTrue(within(l.closeButton, l.header), "close button in the header at " + at);
            assertTrue(within(l.routeList, l.routes), "route list in the route column at " + at);
            assertTrue(within(l.setActiveButton, l.routes), "set-active button in the route column at " + at);
            assertFalse(l.setActiveButton.intersects(l.routeList), "set-active button clear of the scrolling list at " + at);
            assertTrue(within(l.buyButton, l.detail), "buy button in the detail card at " + at);
            assertTrue(within(l.detailText, l.detail), "detail text in the detail card at " + at);
            assertFalse(l.detailText.intersects(l.buyButton), "detail text clear of the buy button at " + at);
            List<Rect> chips = List.of(l.zoomOut, l.zoomIn, l.zoomFit);
            for (Rect c : chips) assertTrue(within(c, l.strip), "view chip inside the strip (never over the canvas) at " + at);
            for (int i = 0; i < chips.size(); i++)
                for (int j = i + 1; j < chips.size(); j++) assertFalse(chips.get(i).intersects(chips.get(j)), "chips overlap at " + at);
            assertTrue(l.canvas.w() >= ScreenLayout.CANVAS_MIN, "canvas keeps its minimum width at " + at + ": " + l.canvas.w());
            assertTrue(l.canvas.h() >= 100, "canvas keeps a usable height at " + at + ": " + l.canvas.h());
            assertTrue(l.routeList.h() >= 100, "route list keeps a usable height at " + at + ": " + l.routeList.h());
            assertTrue(l.detailText.h() >= 60, "detail text area keeps a usable height at " + at + ": " + l.detailText.h());
        }
    }

    @Test
    void referenceSizeIsPinned() {
        ScreenLayout l = ScreenLayout.of(427, 240);                       // a 1280 x 720 window at GUI scale 3
        assertEquals(new Rect(8, 8, 411, 224), l.panel);
        assertEquals(new Rect(14, 14, 399, 20), l.header);
        assertEquals(new Rect(14, 38, 92, 174), l.routes);
        assertEquals(new Rect(14, 38, 92, 156), l.routeList);
        assertEquals(new Rect(17, 195, 86, 16), l.setActiveButton);
        assertEquals(new Rect(110, 38, 191, 22), l.strip);
        assertEquals(new Rect(110, 64, 191, 148), l.canvas);
        assertEquals(new Rect(305, 38, 108, 174), l.detail);
        assertEquals(new Rect(14, 216, 399, 10), l.footer);
    }

    @Test
    void panelIsCappedOnHugeWindows() {
        ScreenLayout l = ScreenLayout.of(3840, 2160);                     // a 4K window at GUI scale 1
        assertEquals(ScreenLayout.MAX_W, l.panel.w());
        assertEquals(ScreenLayout.MAX_H, l.panel.h());
        assertEquals((3840 - ScreenLayout.MAX_W) / 2, l.panel.x());
        assertEquals((2160 - ScreenLayout.MAX_H) / 2, l.panel.y());
        assertEquals(ScreenLayout.LEFT_MAX, l.routes.w());
        assertEquals(ScreenLayout.RIGHT_MAX, l.detail.w());
    }

    @Test
    void layoutIsAPureFunctionOfTheWindowSize() {
        assertEquals(ScreenLayout.of(480, 270).canvas, ScreenLayout.of(480, 270).canvas);
        assertTrue(ScreenLayout.of(854, 480).canvas.w() > ScreenLayout.of(427, 240).canvas.w(), "a bigger window gives the canvas more room");
    }

    @Test
    void tinyWindowsNeverThrowAndNeverGoNegative() {
        int[][] tiny = {{1, 1}, {50, 40}, {120, 90}, {175, 119}, {200, 100}, {320, 100}};
        for (int[] s : tiny) {
            ScreenLayout l = ScreenLayout.of(s[0], s[1]);
            for (Rect r : List.of(l.panel, l.header, l.routes, l.routeList, l.strip, l.canvas, l.detail, l.footer, l.closeButton, l.setActiveButton, l.buyButton, l.detailText))
                assertTrue(r.w() >= 1 && r.h() >= 1, "no empty rectangle at " + s[0] + "x" + s[1] + ": " + r);
        }
    }

    @Test
    void columnsGiveWayBeforeTheCanvasDoes() {
        ScreenLayout narrow = ScreenLayout.of(320, 240);
        assertTrue(narrow.canvas.w() >= ScreenLayout.CANVAS_MIN);
        assertTrue(narrow.detail.w() >= ScreenLayout.RIGHT_FLOOR);
        assertTrue(narrow.routes.w() >= ScreenLayout.LEFT_FLOOR);
        List<Integer> widths = new ArrayList<>();
        for (int w = 320; w <= 960; w += 40) widths.add(ScreenLayout.of(w, 300).canvas.w());
        for (int i = 1; i < widths.size(); i++) assertTrue(widths.get(i) >= widths.get(i - 1), "the canvas never shrinks as the window grows");
    }
}
