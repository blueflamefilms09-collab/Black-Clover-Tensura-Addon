package com.newuniverse.nusmp.client.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.system.FloorCard;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorCardLines;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the floor report window cannot be looked at until the game runs, so the next best thing is arithmetic over the list of marks the pure layout produces: nothing leaves the
 * screen at any size, every text fits inside the panel, a mechanic with a description of any length is wrapped and not overflowed, lines that do not fit are dropped with a
 * marker (the footer first) instead of running off the bottom, the fade scales every colour by one factor, and the window is centred. It runs with two fixed-width stand-ins for
 * the font (6 and 8 pixels a character).
 */
class FloorCardLayoutTest {
    private static final ToIntFunction<String> SIX = s -> s.length() * 6;
    private static final ToIntFunction<String> EIGHT = s -> s.length() * 8;

    private static FloorCard.Mech mech(int i, String desc) {
        return new FloorCard.Mech("floor.m" + i, "Mechanic number " + i, desc, i % 2 == 0 ? "none" : "kill_all", "Kill every warden it spawns.", "floor", "armed");
    }

    private static FloorCard card(int mechs, String desc) {
        List<FloorCard.Mech> list = new ArrayList<>();
        for (int i = 0; i < mechs; i++) list.add(mech(i, desc));
        return new FloorCard(FloorInfoRules.Reason.FORWARD.code(), 12, 11, true, 12, false, "holy_church", "Holy Church", "A great cathedral with a nave, pews, stained glass, a bell tower and a crypt.",
                "holy_only", 90, 12, 8, 1, 109, true, 2, list, List.of());
    }

    private static List<FloorCardLines.Line> lines(FloorCard c) { return FloorCardLines.compose(c, FloorCardLines.english(), "[.]", true); }

    private static void insideScreen(FloorCardLayout.Frame f, int w, int h, ToIntFunction<String> font) {
        assertTrue(f.x() >= 0 && f.y() >= 0 && f.x() + f.w() <= w && f.y() + f.h() <= h, "the window " + f.x() + "," + f.y() + " " + f.w() + "x" + f.h() + " leaves a " + w + "x" + h + " screen");
        for (Op op : f.ops()) {
            if (op instanceof Op.Fill fl) assertTrue(fl.x1() >= f.x() && fl.y1() >= f.y() && fl.x2() <= f.x() + f.w() && fl.y2() <= f.y() + f.h(), "fill " + fl + " is outside " + f);
            if (op instanceof Op.Gradient gr) assertTrue(gr.x1() >= f.x() && gr.y1() >= f.y() && gr.x2() <= f.x() + f.w() && gr.y2() <= f.y() + f.h(), "gradient is outside");
            if (op instanceof Op.Text t) {
                assertTrue(t.x() >= f.x() && t.x() + font.applyAsInt(t.text()) <= f.x() + f.w(), "text '" + t.text() + "' leaves the panel");
                assertTrue(t.y() >= f.y() && t.y() + 8 <= f.y() + f.h(), "text '" + t.text() + "' is above or below the panel");
            }
        }
    }

    @Test void nothingLeavesThePanelOrTheScreen() {
        for (ToIntFunction<String> font : List.of(SIX, EIGHT)) {
            FloorCardLayout lay = new FloorCardLayout(font);
            for (int[] size : new int[][] {{854, 480}, {1920, 1080}, {427, 240}, {320, 200}}) {
                for (int mechs : new int[] {0, 1, 3, 6, 7}) {
                    for (String desc : new String[] {"Short.", "A very long description that a datapack author wrote without thinking about a window, going on and on and on about nothing in particular, again and again."}) {
                        FloorCardLayout.Frame f = lay.layout(lines(card(mechs, desc)), "[ SYSTEM ]", 1, size[0], size[1], 0.67f, 1f, 33f);
                        insideScreen(f, size[0], size[1], font);
                    }
                }
            }
        }
    }

    @Test void theWindowIsCentredAndNoWiderThanTheLimit() {
        FloorCardLayout lay = new FloorCardLayout(SIX);
        FloorCardLayout.Frame f = lay.layout(lines(card(2, "d")), "[ SYSTEM ]", 1, 854, 480, 0.67f, 1f, 0f);
        assertTrue(f.w() <= FloorCardLayout.MAX_W);
        assertEquals((854 - f.w()) / 2, f.x());
        FloorCardLayout.Frame narrow = lay.layout(lines(card(2, "d")), "[ SYSTEM ]", 1, 200, 480, 0.67f, 1f, 0f);
        assertTrue(narrow.w() <= 200 - 28, "narrows on a small screen: " + narrow.w());
    }

    @Test void aTallReportIsCutWithAMarkerAndTheFooterGoesFirst() {
        FloorCardLayout lay = new FloorCardLayout(SIX);
        FloorCardLayout.Frame big = lay.layout(lines(card(6, "A description that takes a couple of lines of the window if it is long enough to wrap.")), "[ SYSTEM ]", 1, 854, 140, 0.67f, 1f, 0f);
        insideScreen(big, 854, 140, SIX);
        boolean dots = false, stuck = false;
        for (Op op : big.ops()) {
            if (op instanceof Op.Text t) {
                if (t.text().equals("...")) dots = true;
                if (t.text().contains("Stuck?")) stuck = true;
            }
        }
        assertTrue(dots, "a report that does not fit ends with a marker");
        assertFalse(stuck, "the footer is the first to go");
    }

    @Test void aLongDescriptionIsWrappedToAtMostThreeLines() {
        FloorCardLayout lay = new FloorCardLayout(SIX);
        String desc = "word ".repeat(200);
        FloorCardLayout.Frame f = lay.layout(lines(card(1, desc)), "[ SYSTEM ]", 1, 1920, 1080, 0.67f, 1f, 0f);
        int body = 0;
        for (Op op : f.ops()) if (op instanceof Op.Text t && t.text().startsWith("word")) body++;
        assertEquals(3, body);
        boolean cut = false;
        for (Op op : f.ops()) if (op instanceof Op.Text t && t.text().endsWith("...")) cut = true;
        assertTrue(cut, "the third line ends with an ellipsis");
    }

    @Test void theFadeScalesEveryColourByOneFactor() {
        FloorCardLayout lay = new FloorCardLayout(SIX);
        FloorCardLayout.Frame full = lay.layout(lines(card(1, "d")), "[ SYSTEM ]", 1, 854, 480, 1f, 1f, 0f);
        FloorCardLayout.Frame half = lay.layout(lines(card(1, "d")), "[ SYSTEM ]", 1, 854, 480, 1f, 0.5f, 0f);
        assertEquals(full.ops().size(), half.ops().size() - 0, "same marks, other opacity");
        for (int i = 0; i < full.ops().size(); i++) {
            if (full.ops().get(i) instanceof Op.Text a && half.ops().get(i) instanceof Op.Text b) {
                assertEquals(a.text(), b.text());
                assertTrue((b.argb() >>> 24) < (a.argb() >>> 24), "a faded text is more transparent");
            }
        }
    }

    @Test void theFadeCurveGoesInAndOut() {
        assertEquals(0f, FloorCardLayout.fade(-1, 100));
        assertEquals(0f, FloorCardLayout.fade(0, 100));
        assertTrue(FloorCardLayout.fade(3, 100) > 0f && FloorCardLayout.fade(3, 100) < 1f);
        assertEquals(1f, FloorCardLayout.fade(50, 100));
        assertTrue(FloorCardLayout.fade(95, 100) < 1f);
        assertEquals(0f, FloorCardLayout.fade(100, 100));
        assertEquals(0f, FloorCardLayout.fade(500, 100));
    }

    @Test void theTitleTakesTheColourOfTheRoad() {
        assertEquals(OverlayStyle.GOLD, FloorCardLayout.colour(FloorCardLines.Kind.TITLE, 1));
        assertEquals(OverlayStyle.VIOLET, FloorCardLayout.colour(FloorCardLines.Kind.TITLE, 2));
        for (FloorCardLines.Kind k : FloorCardLines.Kind.values()) {
            assertTrue(FloorCardLayout.colour(k, 1) != 0, "every kind has a colour: " + k);
            assertTrue(FloorCardLayout.maxLines(k) >= 1);
        }
    }

    @Test void anEmptyReportStillDrawsAFrame() {
        FloorCardLayout lay = new FloorCardLayout(SIX);
        FloorCardLayout.Frame f = lay.layout(List.of(), "[ SYSTEM ]", 0, 854, 480, 0.67f, 1f, 0f);
        assertTrue(f.w() > 0 && f.h() > 0);
        insideScreen(f, 854, 480, SIX);
    }
}
