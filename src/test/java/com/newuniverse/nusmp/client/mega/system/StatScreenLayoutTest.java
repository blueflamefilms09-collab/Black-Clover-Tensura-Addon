package com.newuniverse.nusmp.client.mega.system;

import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Act;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Hit;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Rect;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Row;
import com.newuniverse.nusmp.labyrinth.mega.system.Stat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the stat screen is all mouse. A button that overlaps another, hangs off the panel, or sits in the wrong row sends a click to the wrong stat, and nobody
 * sees it until the game runs. These tests check, over a grid of screens and effect-line counts, that every button is inside the panel, inside its own row, that
 * no two buttons overlap, that the rows go down the panel in stat order, that a click finds exactly the button drawn there, and that the panel fits a 427 x 240
 * screen (GUI scale 3 on 720p) by using the compact form.
 */
class StatScreenLayoutTest {
    private static final int[] ONE = {1, 1, 1, 1, 1}, SENSE_TWO = {1, 1, 1, 1, 2}, WORDY = {2, 2, 3, 2, 4};

    private static List<Rect> buttons(StatScreenLayout.Layout l) {
        List<Rect> out = new ArrayList<>();
        for (Row r : l.rows()) {
            out.add(r.add1());
            out.add(r.add5());
            out.add(r.max());
        }
        out.add(l.reset());
        out.add(l.move());
        out.add(l.close());
        return out;
    }

    private static boolean inside(Rect inner, Rect outer) {
        return inner.x() >= outer.x() && inner.y() >= outer.y() && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    private static boolean overlap(Rect a, Rect b) {
        return a.x() < b.right() && b.x() < a.right() && a.y() < b.bottom() && b.y() < a.bottom();
    }

    @Test void everyButtonIsInsideThePanelAndNoTwoOverlap() {
        for (int sw : new int[]{320, 427, 640, 854, 1920}) {
            for (int sh : new int[]{240, 270, 360, 480, 1080}) {
                for (int[] lines : new int[][]{ONE, SENSE_TWO, WORDY}) {
                    StatScreenLayout.Layout l = StatScreenLayout.of(sw, sh, lines);
                    List<Rect> b = buttons(l);
                    for (Rect r : b) assertTrue(inside(r, l.panel()), r + " inside " + l.panel() + " on " + sw + "x" + sh);
                    for (int i = 0; i < b.size(); i++) {
                        for (int j = i + 1; j < b.size(); j++) assertFalse(overlap(b.get(i), b.get(j)), "buttons " + i + " and " + j + " overlap on " + sw + "x" + sh);
                    }
                }
            }
        }
    }

    @Test void theRowButtonsAreInsideTheirOwnRowAndTheRowsGoDownInStatOrder() {
        StatScreenLayout.Layout l = StatScreenLayout.of(640, 360, SENSE_TWO);
        assertEquals(Stat.COUNT, l.rows().size());
        int lastBottom = -1;
        for (int i = 0; i < Stat.COUNT; i++) {
            Row r = l.rows().get(i);
            assertEquals(Stat.values()[i], r.stat());
            assertTrue(inside(r.add1(), r.rect()) && inside(r.add5(), r.rect()) && inside(r.max(), r.rect()), "buttons inside row " + i);
            assertTrue(r.rect().y() >= lastBottom - 1, "row " + i + " starts under the previous one");
            lastBottom = r.rect().bottom();
            assertTrue(r.add1().x() < r.add5().x() && r.add5().x() < r.max().x(), "+1, +5, MAX from left to right");
            assertTrue(r.effectY() > r.nameY());
        }
        assertEquals(2, l.rows().get(4).lines());
        assertTrue(l.rows().get(4).rect().h() > l.rows().get(3).rect().h(), "a row with more effect lines is taller");
        assertTrue(l.reset().y() >= lastBottom, "the footer is under the rows");
    }

    @Test void aClickFindsExactlyTheButtonDrawnThere() {
        StatScreenLayout.Layout l = StatScreenLayout.of(640, 360, ONE);
        for (Row r : l.rows()) {
            assertEquals(new Hit(Act.ADD1, r.stat()), l.hit(r.add1().x() + 1, r.add1().y() + 1));
            assertEquals(new Hit(Act.ADD5, r.stat()), l.hit(r.add5().x() + r.add5().w() - 1, r.add5().y() + r.add5().h() - 1));
            assertEquals(new Hit(Act.MAX, r.stat()), l.hit(r.max().x() + 3, r.max().y() + 3));
            assertNull(l.hit(r.rect().x() + 2, r.effectY() + 1), "the text area of a row is not a button");
        }
        assertEquals(new Hit(Act.RESET, null), l.hit(l.reset().x() + 2, l.reset().y() + 2));
        assertEquals(new Hit(Act.MOVE, null), l.hit(l.move().x() + 2, l.move().y() + 2));
        assertEquals(new Hit(Act.CLOSE, null), l.hit(l.close().x() + 2, l.close().y() + 2));
        assertNull(l.hit(-5, -5));
        assertNull(l.hit(l.panel().right() + 3, l.panel().bottom() + 3));
    }

    @Test void everyPixelOfAButtonHitsSomething() {
        StatScreenLayout.Layout l = StatScreenLayout.of(427, 240, SENSE_TWO);
        for (Rect panelRect : buttons(l)) {
            for (int x = panelRect.x(); x < panelRect.right(); x++) {
                for (int y = panelRect.y(); y < panelRect.bottom(); y++) assertNotNull(l.hit(x + 0.5, y + 0.5), "(" + x + "," + y + ") of " + panelRect);
            }
        }
    }

    @Test void theWholeRowAnswersTheWheel() {
        StatScreenLayout.Layout l = StatScreenLayout.of(640, 360, SENSE_TWO);
        for (Row r : l.rows()) {
            assertSame(r, l.rowAt(r.rect().x() + 1, r.rect().y() + 1));
            assertSame(r, l.rowAt(r.add1().x() + 1, r.add1().y() + 1), "over a button too");
            assertSame(r, l.rowAt(r.rect().right() - 1, r.rect().bottom() - 1));
        }
        assertNull(l.rowAt(l.panel().x() - 10, l.panel().y()));
        assertNull(l.rowAt(l.reset().x() + 1, l.reset().y() + 1), "the footer is no stat row");
    }

    @Test void thePanelIsCentredAndWithinTheWidthLimits() {
        for (int sw : new int[]{250, 320, 427, 640, 1920}) {
            StatScreenLayout.Layout l = StatScreenLayout.of(sw, 480, ONE);
            Rect p = l.panel();
            assertEquals(StatScreenLayout.panelWidth(sw), p.w());
            assertEquals((sw - p.w()) / 2, p.x());
            assertTrue(p.w() <= StatScreenLayout.MAX_W);
            if (sw >= StatScreenLayout.MIN_W) assertTrue(p.x() >= 0 && p.right() <= sw);
        }
        assertEquals(StatScreenLayout.MAX_W, StatScreenLayout.panelWidth(5000));
        assertEquals(StatScreenLayout.MIN_W, StatScreenLayout.panelWidth(260));
        assertEquals(200, StatScreenLayout.panelWidth(200), "a screen narrower than the minimum gets the whole screen");
        assertTrue(StatScreenLayout.textWidth(640) > 100);
    }

    @Test void aShortScreenGetsTheCompactFormAndStillFits() {
        StatScreenLayout.Layout tall = StatScreenLayout.of(640, 480, SENSE_TWO);
        assertFalse(tall.compact());
        assertTrue(tall.modeY() >= 0, "the mode line is shown when there is room");
        StatScreenLayout.Layout small = StatScreenLayout.of(427, 240, SENSE_TWO);
        assertTrue(small.panel().y() >= 0 && small.panel().bottom() <= 240, "fits 427x240: " + small.panel());
        assertTrue(small.panel().h() <= tall.panel().h());
        StatScreenLayout.Layout cramped = StatScreenLayout.of(427, 150, SENSE_TWO);
        assertTrue(cramped.compact());
        assertEquals(-1, cramped.modeY());
        assertEquals(0, cramped.panel().y(), "a panel taller than the screen starts at the top");
    }

    @Test void missingOrZeroLineCountsAreTreatedAsOneLine() {
        StatScreenLayout.Layout a = StatScreenLayout.of(640, 480, new int[]{0, -3});
        StatScreenLayout.Layout b = StatScreenLayout.of(640, 480, ONE);
        assertEquals(b.panel(), a.panel());
        assertEquals(Stat.COUNT, a.rows().size());
    }
}
