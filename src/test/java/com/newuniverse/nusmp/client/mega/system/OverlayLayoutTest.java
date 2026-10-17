package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the holographic window is only worth anything if it looks right, and nobody can look at it until the game runs. This test is the next best thing: the layout is a
 * pure function from "what to say" to a list of marks, so every way it could look broken is checked as arithmetic over that list. Nothing leaves its panel or the
 * screen, no two lines of text land on each other, a long title takes at most three lines, the bar's segments add up to its width exactly, the Abyssal numbers widen
 * the panel instead of overflowing it, the four anchors hold the four corners at every scale, the glitch happens only in its frames, and the toasts come in from the left,
 * type, and are gone when their life is over. It runs with two fixed-width stand-ins for the font (6 and 8 pixels a character), so a wider font than expected is covered.
 */
class OverlayLayoutTest {
    private static final ToIntFunction<String> SIX = s -> s.length() * 6;
    private static final ToIntFunction<String> EIGHT = s -> s.length() * 8;

    // ---------------------------------------------------------------- scenes
    private static OverlayLayout.Stats stats(int a, int b, int c, int d, int e) {
        return new OverlayLayout.Stats(new String[]{"STR", "AGI", "VIT", "INT", "SENSE"}, new int[]{a, b, c, d, e});
    }

    private static OverlayLayout.Quest quest(int state, int mode, String title, int progress, int target, int timeTicks) {
        return new OverlayLayout.Quest(state, mode, title, -1, false, "UNHARMED", "COMPLETE", "FAILED", "", progress, target, "TIME", timeTicks, Math.max(1, target * 20), true);
    }

    private static OverlayLayout.Panel panel(int floor, int road, int threat, boolean boss, OverlayLayout.Quest q, OverlayLayout.Stats st) {
        return new OverlayLayout.Panel("[ SYSTEM ]", "FLOOR", floor, road, road == 2 ? "ABYSSAL" : "HOLY", "THREAT", threat, boss, "BOSS FLOOR: NO QUEST", "NO QUEST ON THIS FLOOR", q, st);
    }

    private static List<OverlayLayout.Panel> allScenes() {
        String longTitle = "Survive the ambush: defeat 12 of the creatures that hunt you and then some more of them, all of them, and every one of their friends too";
        List<OverlayLayout.Panel> l = new ArrayList<>();
        OverlayLayout.Stats st = stats(3, 1, 0, 2, 0);
        l.add(panel(12, 1, 12, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Clear 5 rooms without taking magic damage", 3, 5, -1), st));
        l.add(panel(31, 1, 31, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, longTitle, 7, 12, -1), stats(30, 30, 30, 30, 30)));
        l.add(panel(7, 1, 7, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_CLOCK, "Reach the next floor within 286 seconds", 0, 286, 4210), st));
        l.add(panel(4, 1, 4, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_STATE, "Reach the next floor without taking any damage", 0, 1, -1), st));
        l.add(panel(12, 1, 12, false, quest(OverlayLayout.Q_DONE, OverlayLayout.MODE_COUNT, "Clear 5 rooms", 5, 5, -1), st));
        l.add(panel(12, 1, 12, false, quest(OverlayLayout.Q_FAILED, OverlayLayout.MODE_COUNT, "Clear 5 rooms", 3, 5, -1), st));
        l.add(panel(10, 1, 10, true, null, st));
        l.add(panel(8, 1, 8, false, null, null));
        l.add(panel(-120, 2, 120, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Defeat 20 monsters on this floor", 11, 20, -1), stats(30, 30, 30, 30, 30)));
        return l;
    }

    private static OverlayLayout.View view(int w, int h, float scale, int anchor, float time) { return new OverlayLayout.View(w, h, scale, anchor, 0.67f, time); }

    private static int textWidth(Op.Text t, ToIntFunction<String> width) { return width.applyAsInt(t.text()); }

    // ---------------------------------------------------------------- inside the panel and the screen
    @Test void nothingLeavesThePanel() {
        for (ToIntFunction<String> font : List.of(SIX, EIGHT)) {
            OverlayLayout lay = new OverlayLayout(font);
            for (OverlayLayout.Panel p : allScenes()) {
                for (float time : new float[]{0.2f, 33f, 91.5f}) {
                    OverlayLayout.Frame f = lay.panel(p, view(640, 360, 1f, 0, time));
                    for (Op op : f.ops()) {
                        if (op instanceof Op.Fill r) {
                            assertTrue(r.x1() >= f.x() - 1 && r.y1() >= f.y() - 1 && r.x2() <= f.x() + f.w() + 1 && r.y2() <= f.y() + f.h() + 1, "fill outside the panel: " + r + " in " + f);
                            assertTrue(r.x2() > r.x1() && r.y2() > r.y1(), "empty fill " + r);
                        } else if (op instanceof Op.Gradient g) {
                            assertTrue(g.x1() >= f.x() && g.y1() >= f.y() && g.x2() <= f.x() + f.w() && g.y2() <= f.y() + f.h(), "gradient outside the panel: " + g);
                        } else if (op instanceof Op.Text t) {
                            assertTrue(t.x() >= f.x() + 1 - 1 && t.x() + textWidth(t, font) <= f.x() + f.w() - 1 + 1, "text outside the panel: " + t + " in " + f);
                            assertTrue(t.y() >= f.y() && t.y() + OverlayStyle.TEXT_H <= f.y() + f.h(), "text above or below the panel: " + t);
                            assertTrue(OverlayMath.drawable(t.argb()), "an undrawable (alpha under 8) text was emitted: " + t);
                        }
                    }
                }
            }
        }
    }

    @Test void thePanelStaysOnEveryScreenAtEveryAnchorAndScale() {
        OverlayLayout lay = new OverlayLayout(SIX);
        int[][] screens = {{320, 180}, {427, 240}, {640, 360}, {960, 540}};
        for (OverlayLayout.Panel p : allScenes()) {
            for (int[] s : screens) {
                for (float scale : new float[]{0.5f, 1f, 1.5f, 2f}) {
                    for (int anchor = 0; anchor < 4; anchor++) {
                        OverlayLayout.View v = view(s[0], s[1], scale, anchor, 10f);
                        OverlayLayout.Frame f = lay.panel(p, v);
                        if (f.w() + 2 * OverlayStyle.MARGIN > v.w() || f.h() + 2 * OverlayStyle.MARGIN > v.h()) continue;   // a screen too small for the panel: nothing can be promised
                        assertTrue(f.x() >= 0 && f.y() >= 0 && f.x() + f.w() <= v.w() && f.y() + f.h() <= v.h(), "off screen: " + f + " on " + v.w() + "x" + v.h() + " anchor " + anchor);
                    }
                }
            }
        }
    }

    @Test void theFourAnchorsHoldTheFourCorners() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Panel p = allScenes().get(0);
        int m = OverlayStyle.MARGIN;
        OverlayLayout.Frame tl = lay.panel(p, view(640, 360, 1f, 0, 1f)), tr = lay.panel(p, view(640, 360, 1f, 1, 1f));
        OverlayLayout.Frame bl = lay.panel(p, view(640, 360, 1f, 2, 1f)), br = lay.panel(p, view(640, 360, 1f, 3, 1f));
        assertEquals(m, tl.x());
        assertEquals(m, tl.y());
        assertEquals(640 - m, tr.x() + tr.w());
        assertEquals(m, tr.y());
        assertEquals(m, bl.x());
        assertEquals(360 - m, bl.y() + bl.h());
        assertEquals(640 - m, br.x() + br.w());
        assertEquals(360 - m, br.y() + br.h());
    }

    @Test void noTwoLinesOfTextLandOnEachOther() {
        for (ToIntFunction<String> font : List.of(SIX, EIGHT)) {
            OverlayLayout lay = new OverlayLayout(font);
            for (OverlayLayout.Panel p : allScenes()) {
                for (float time : new float[]{0.2f, 50f}) {
                    List<Op.Text> texts = new ArrayList<>();
                    for (Op op : lay.panel(p, view(640, 360, 1f, 0, time)).ops()) if (op instanceof Op.Text t) texts.add(t);
                    for (int i = 0; i < texts.size(); i++) {
                        for (int j = i + 1; j < texts.size(); j++) {
                            Op.Text a = texts.get(i), b = texts.get(j);
                            if (a.text().equals(b.text()) && a.y() == b.y()) continue;           // the two ghost copies of a glitching header
                            boolean across = a.x() < b.x() + textWidth(b, font) && b.x() < a.x() + textWidth(a, font);
                            boolean down = a.y() < b.y() + OverlayStyle.TEXT_H && b.y() < a.y() + OverlayStyle.TEXT_H;
                            assertFalse(across && down, "two texts overlap: " + a + " and " + b);
                        }
                    }
                }
            }
        }
    }

    @Test void theWindowIsCheap() {
        OverlayLayout lay = new OverlayLayout(SIX);
        int worst = 0;
        for (OverlayLayout.Panel p : allScenes()) worst = Math.max(worst, lay.panel(p, view(640, 360, 1f, 0, 0.2f)).ops().size());
        assertTrue(worst <= 130, "the busiest panel needs " + worst + " marks");
        assertTrue(worst >= 30, "suspiciously few marks: " + worst);
    }

    // ---------------------------------------------------------------- the pieces
    @Test void aLongTitleTakesAtMostThreeLines() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Stats st = stats(0, 0, 0, 0, 0);
        int one = lay.panel(panel(5, 1, 5, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Short", 1, 5, -1), st), view(640, 360, 1f, 0, 1f)).h();
        int three = lay.panel(panel(5, 1, 5, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "x ".repeat(300).trim().replace("x", "word"), 1, 5, -1), st), view(640, 360, 1f, 0, 1f)).h();
        assertEquals(one + 2 * OverlayStyle.LINE, three, "an endless title adds exactly two lines to a one-line title");
    }

    @Test void theBarSegmentsAddUpToTheBarWidthExactly() {
        OverlayLayout lay = new OverlayLayout(SIX);
        for (int n = 1; n <= 40; n++) {
            OverlayLayout.Frame f = lay.panel(panel(5, 1, 5, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "T", n / 2, n, -1), null), view(640, 360, 1f, 0, 1f));
            List<Op.Fill> segs = new ArrayList<>();
            for (Op op : f.ops()) if (op instanceof Op.Fill r && r.y2() - r.y1() == OverlayStyle.BAR_H && r.x2() - r.x1() > 1 && r.x1() > f.x() + 2 && r.x2() < f.x() + f.w() - 2) segs.add(r);
            int count = OverlayMath.totalSegments(n);
            assertEquals(count, segs.size(), "segments for target " + n);
            int width = 0;
            for (Op.Fill s : segs) width += s.x2() - s.x1();
            int gaps = count > 1 ? OverlayStyle.BAR_GAP * (count - 1) : 0;
            assertEquals(f.w() - 2 * OverlayStyle.PAD_X - OverlayStyle.BAR_RESERVE, width + gaps, "bar width for target " + n);
        }
    }

    @Test void theAbyssalNumbersWidenThePanelInsteadOfOverflowing() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame small = lay.panel(panel(3, 1, 3, false, null, null), view(640, 360, 1f, 0, 1f));
        assertEquals(OverlayStyle.PANEL_MIN_W, small.w(), "an ordinary panel has the minimum width");
        OverlayLayout wide = new OverlayLayout(EIGHT);
        OverlayLayout.Frame a = wide.panel(panel(3, 1, 3, false, null, null), view(640, 360, 1f, 0, 1f));
        OverlayLayout.Frame deep = wide.panel(panel(-120, 2, 120, false, null, null), view(640, 360, 1f, 0, 1f));
        assertTrue(deep.w() > a.w(), "a wide information row widens the panel");
        assertTrue(a.w() > OverlayStyle.PANEL_MIN_W, "a font wider than expected widens the panel too");
    }

    @Test void theHeaderGlitchesOnlyInItsFramesAndTheCursorBlinks() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Panel p = allScenes().get(0);
        assertEquals(3, countText(lay.panel(p, view(640, 360, 1f, 0, 0.2f)), "[ SYSTEM ]"), "a glitch frame draws the header three times");
        assertEquals(1, countText(lay.panel(p, view(640, 360, 1f, 0, 60f)), "[ SYSTEM ]"), "any other frame draws it once");
        int on = lay.panel(p, view(640, 360, 1f, 0, 3f)).ops().size();
        int off = lay.panel(p, view(640, 360, 1f, 0, 13f)).ops().size();
        assertTrue(on != off, "the cursor shows in half of the seconds");
    }

    private static int countText(OverlayLayout.Frame f, String text) {
        int n = 0;
        for (Op op : f.ops()) if (op instanceof Op.Text t && t.text().equals(text)) n++;
        return n;
    }

    private static boolean hasText(OverlayLayout.Frame f, String text) { return countText(f, text) > 0; }

    @Test void theStateWordsReplaceTheCountAndTheClockShowsTime() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame done = lay.panel(panel(12, 1, 12, false, quest(OverlayLayout.Q_DONE, OverlayLayout.MODE_COUNT, "T", 5, 5, -1), null), view(640, 360, 1f, 0, 1f));
        assertTrue(hasText(done, "COMPLETE"));
        assertFalse(hasText(done, "5 / 5"));
        OverlayLayout.Frame failed = lay.panel(panel(12, 1, 12, false, quest(OverlayLayout.Q_FAILED, OverlayLayout.MODE_COUNT, "T", 3, 5, -1), null), view(640, 360, 1f, 0, 1f));
        assertTrue(hasText(failed, "FAILED"));
        OverlayLayout.Frame active = lay.panel(panel(12, 1, 12, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "T", 3, 5, -1), null), view(640, 360, 1f, 0, 1f));
        assertTrue(hasText(active, "3 / 5"));
        OverlayLayout.Frame timed = lay.panel(panel(7, 1, 7, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_CLOCK, "T", 0, 286, 4210), null), view(640, 360, 1f, 0, 1f));
        assertTrue(hasText(timed, "TIME " + OverlayMath.countdown(4210)));
        OverlayLayout.Frame unharmed = lay.panel(panel(4, 1, 4, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_STATE, "T", 0, 1, -1), null), view(640, 360, 1f, 0, 1f));
        assertTrue(hasText(unharmed, "UNHARMED"));
    }

    @Test void theStateWordsAreColouredByTheirMeaning() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame done = lay.panel(panel(12, 1, 12, false, quest(OverlayLayout.Q_DONE, OverlayLayout.MODE_COUNT, "T", 5, 5, -1), null), view(640, 360, 1f, 0, 1f));
        OverlayLayout.Frame failed = lay.panel(panel(12, 1, 12, false, quest(OverlayLayout.Q_FAILED, OverlayLayout.MODE_COUNT, "T", 3, 5, -1), null), view(640, 360, 1f, 0, 1f));
        OverlayLayout.Frame active = lay.panel(panel(12, 1, 12, false, quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "T", 3, 5, -1), null), view(640, 360, 1f, 0, 1f));
        assertEquals(OverlayStyle.GREEN, rgbOf(done, "COMPLETE"));
        assertEquals(OverlayStyle.RED, rgbOf(failed, "FAILED"));
        assertEquals(OverlayStyle.TEXT, rgbOf(active, "3 / 5"));
        assertEquals(OverlayStyle.GREEN_TEXT, rgbOf(done, "T"), "a finished quest's title turns green");
        assertEquals(OverlayStyle.RED_TEXT, rgbOf(failed, "T"), "a failed quest's title turns red");
    }

    private static int rgbOf(OverlayLayout.Frame f, String text) {
        for (Op op : f.ops()) if (op instanceof Op.Text t && t.text().equals(text)) return t.argb() & 0xFFFFFF;
        return -1;
    }

    @Test void bossAndEmptyFloorsSayWhyThereIsNoQuest() {
        OverlayLayout lay = new OverlayLayout(SIX);
        assertTrue(hasText(lay.panel(panel(10, 1, 10, true, null, null), view(640, 360, 1f, 0, 1f)), "BOSS FLOOR: NO QUEST"));
        assertTrue(hasText(lay.panel(panel(8, 1, 8, false, null, null), view(640, 360, 1f, 0, 1f)), "NO QUEST ON THIS FLOOR"));
    }

    @Test void theQuestOfAnotherFloorIsNamedAndDimmed() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Quest here = quest(OverlayLayout.Q_ACTIVE, OverlayLayout.MODE_COUNT, "Clear 5 rooms", 1, 5, -1);
        OverlayLayout.Quest away = new OverlayLayout.Quest(here.state(), here.mode(), here.title(), -1, false, "", "COMPLETE", "FAILED", "QUEST OF FLOOR +3", 1, 5, "TIME", -1, 100, false);
        OverlayLayout.Frame a = lay.panel(panel(2, 1, 2, false, here, null), view(640, 360, 1f, 0, 1f));
        OverlayLayout.Frame b = lay.panel(panel(2, 1, 2, false, away, null), view(640, 360, 1f, 0, 1f));
        assertFalse(hasText(a, "QUEST OF FLOOR +3"));
        assertTrue(hasText(b, "QUEST OF FLOOR +3"));
        assertEquals(a.h() + OverlayStyle.LINE, b.h(), "the extra line takes one line");
    }

    @Test void aZeroStatIsDimAndAPositiveOneIsBright() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame f = lay.panel(panel(5, 1, 5, false, null, stats(3, 0, 0, 0, 0)), view(640, 360, 1f, 0, 1f));
        int bright = -1, dim = -1;
        for (Op op : f.ops()) {
            if (op instanceof Op.Text t && t.text().equals("3")) bright = t.argb() & 0xFFFFFF;
            if (op instanceof Op.Text t && t.text().equals("0")) dim = t.argb() & 0xFFFFFF;
        }
        assertEquals(OverlayStyle.TEXT, bright);
        assertEquals(OverlayStyle.CYAN_FAINT, dim);
    }

    @Test void theOpacityScalesThePanelButKeepsTheTextReadable() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Panel p = allScenes().get(0);
        OverlayLayout.Frame low = lay.panel(p, new OverlayLayout.View(640, 360, 1f, 0, 0.1f, 5f));
        OverlayLayout.Frame full = lay.panel(p, new OverlayLayout.View(640, 360, 1f, 0, 1f, 5f));
        Op.Gradient gl = (Op.Gradient) low.ops().get(0), gf = (Op.Gradient) full.ops().get(0);
        assertTrue((gl.top() >>> 24) < (gf.top() >>> 24), "a lower opacity makes the fill thinner");
        assertEquals(255, gf.top() >>> 24);
        for (Op op : low.ops()) if (op instanceof Op.Text t) assertTrue((t.argb() >>> 24) >= 140, "text stays readable at the lowest opacity: " + t);
    }

    // ---------------------------------------------------------------- toasts
    private static List<OverlayLayout.ToastView> toasts(float... ages) {
        List<OverlayLayout.ToastView> l = new ArrayList<>();
        for (float a : ages) l.add(new OverlayLayout.ToastView("[SYSTEM]", "QUEST COMPLETE", 1, a));
        return l;
    }

    @Test void aToastSlidesInFromTheLeftTypesAndIsGoneWhenItsLifeIsOver() {
        OverlayLayout lay = new OverlayLayout(SIX);
        assertTrue(lay.toasts(toasts(0f), 6, 20).isEmpty(), "at age 0 nothing is visible yet");
        List<Op> early = lay.toasts(toasts(1f), 6, 20);
        Op.Fill box = (Op.Fill) early.get(0);
        assertTrue(box.x1() < 6, "a new toast is still left of its place");
        List<Op> settled = lay.toasts(toasts(40f), 6, 20);
        assertEquals(6, ((Op.Fill) settled.get(0)).x1(), "a settled toast rests at its place");
        int typedEarly = typedLength(lay.toasts(toasts(8f), 6, 20)), typedLate = typedLength(settled);
        assertTrue(typedEarly < typedLate, "the text is typed out over time");
        assertEquals("[SYSTEM] QUEST COMPLETE".length(), typedLate);
        assertTrue(lay.toasts(toasts(OverlayStyle.TOAST_H + ToastTimeline.LIFE_TICKS), 6, 20).isEmpty(), "a toast is gone when its life is over");
        float near = ToastTimeline.LIFE_TICKS - 4f;
        int a1 = ((Op.Fill) lay.toasts(toasts(40f), 6, 20).get(0)).argb() >>> 24, a2 = ((Op.Fill) lay.toasts(toasts(near), 6, 20).get(0)).argb() >>> 24;
        assertTrue(a2 < a1, "a toast fades before it goes");
    }

    private static int typedLength(List<Op> ops) {
        int n = 0;
        for (Op op : ops) if (op instanceof Op.Text t) n += t.text().length() + (n > 0 ? 1 : 0);
        return n;
    }

    @Test void toastsStackBelowEachOtherAndOnlyThreeAreShown() {
        OverlayLayout lay = new OverlayLayout(SIX);
        List<Op> ops = lay.toasts(toasts(40f, 40f, 40f, 40f, 40f), 6, 20);
        List<Integer> tops = new ArrayList<>();
        for (Op op : ops) if (op instanceof Op.Fill f && f.x2() - f.x1() > 50 && f.y2() - f.y1() == OverlayStyle.TOAST_H) tops.add(f.y1());
        assertEquals(List.of(20, 20 + OverlayStyle.TOAST_H + OverlayStyle.TOAST_GAP, 20 + 2 * (OverlayStyle.TOAST_H + OverlayStyle.TOAST_GAP)), tops);
    }

    @Test void eachKindOfToastHasItsOwnAccent() {
        java.util.Set<Integer> accents = new java.util.HashSet<>();
        for (int k = 0; k <= 4; k++) accents.add(OverlayStyle.toastAccent(k));
        assertEquals(5, accents.size());
        assertEquals(OverlayStyle.CYAN, OverlayStyle.toastAccent(99), "an unknown kind is plain information");
        assertEquals(OverlayStyle.TEXT, OverlayStyle.toastText(-3));
    }

    @Test void theColourTablesAreMonotoneAndTotal() {
        assertEquals(0x7CFFB0, OverlayStyle.threatColour(1));
        assertEquals(0xFF5A5A, OverlayStyle.threatColour(500));
        assertNotEquals(OverlayStyle.threatColour(5), OverlayStyle.threatColour(6));
        assertEquals(OverlayStyle.RED, OverlayStyle.clockColour(100));
        assertEquals(OverlayStyle.AMBER, OverlayStyle.clockColour(40 * 20));
        assertEquals(OverlayStyle.CYAN_BRIGHT, OverlayStyle.clockColour(200 * 20));
        assertEquals(OverlayStyle.GOLD, OverlayStyle.roadColour(1));
        assertEquals(OverlayStyle.VIOLET, OverlayStyle.roadColour(2));
        assertEquals("+12", OverlayLayout.label(12));
        assertEquals("-3", OverlayLayout.label(-3));
    }
}
