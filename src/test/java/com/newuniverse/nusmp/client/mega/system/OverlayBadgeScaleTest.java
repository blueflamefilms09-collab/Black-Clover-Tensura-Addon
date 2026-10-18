package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the System window gained two things the layout must get right without being seen. A badge in the header when free stat points are waiting (it must appear
 * only then, carry the words when they fit and the short {@code +N} when they do not, and never touch the title, the diamond or the panel's edge), and a size from 0.5 to
 * 2.5 plus a place that can be centred (the window must stay on the screen at every scale, its rectangle must scale with the setting, and a view that does not know the
 * new fields must draw exactly as before). The mouse hit boxes are the window rectangle times the scale, so that is pinned too.
 */
class OverlayBadgeScaleTest {
    private static final ToIntFunction<String> SIX = s -> s.length() * 6;
    private static final ToIntFunction<String> EIGHT = s -> s.length() * 8;

    private static OverlayLayout.Panel panel(int free, String freeText) {
        OverlayLayout.Stats st = new OverlayLayout.Stats(new String[]{"STR", "AGI", "VIT", "INT", "SENSE"}, new int[]{3, 1, 0, 2, 0});
        return new OverlayLayout.Panel("[ SYSTEM ]", "FLOOR", -5, 1, "HOLY", "THREAT", 5, false, "BOSS", "NO QUEST", null, st, free, freeText);          // floor -5: its label starts with "-", so every text that starts with "+" is the badge
    }

    private static OverlayLayout.View view(float scale, int anchor, int offX, int offY, boolean cx, boolean cy) {
        return new OverlayLayout.View(640, 360, scale, anchor, 0.67f, 10f, offX, offY, cx, cy);
    }

    private static List<Op.Text> texts(OverlayLayout.Frame f) {
        List<Op.Text> out = new ArrayList<>();
        for (Op op : f.ops()) if (op instanceof Op.Text t) out.add(t);
        return out;
    }

    @Test void noFreePointsNoBadgeAndTheOldPanelIsUnchanged() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Panel old = new OverlayLayout.Panel("[ SYSTEM ]", "FLOOR", -5, 1, "HOLY", "THREAT", 5, false, "BOSS", "NO QUEST", null, panel(0, "").stats());
        OverlayLayout.Frame a = lay.panel(old, view(1f, 0, 0, 0, false, false));
        OverlayLayout.Frame b = lay.panel(panel(0, "+5 PTS [I]"), view(1f, 0, 0, 0, false, false));
        assertEquals(a.ops(), b.ops(), "free = 0 draws nothing extra, whatever the text");
        for (Op.Text t : texts(b)) assertFalse(t.text().contains("PTS"));
    }

    @Test void theBadgeShowsItsWordsWhenTheyFit() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame f = lay.panel(panel(3, "+3 PTS [I]"), view(1f, 0, 0, 0, false, false));
        boolean found = false;
        for (Op.Text t : texts(f)) found |= t.text().equals("+3 PTS [I]");
        assertTrue(found, "the badge text is drawn");
        assertTrue(lay.panel(panel(3, "+3 PTS [I]"), view(1f, 0, 0, 0, false, false)).ops().size() > lay.panel(panel(0, ""), view(1f, 0, 0, 0, false, false)).ops().size());
    }

    @Test void aTextThatDoesNotFitFallsBackToTheShortForm() {
        OverlayLayout lay = new OverlayLayout(EIGHT);
        OverlayLayout.Frame f = lay.panel(panel(12, "+12 POINTS WAITING FOR YOU [I]"), view(1f, 0, 0, 0, false, false));
        boolean longShown = false, shortShown = false;
        for (Op.Text t : texts(f)) {
            longShown |= t.text().contains("WAITING");
            shortShown |= t.text().equals("+12");
        }
        assertFalse(longShown, "the long words would touch the title");
        assertTrue(shortShown || f.w() > 164, "the short +N is drawn instead");
    }

    @Test void theBadgeStaysInsideThePanelAndClearOfTheTitle() {
        for (ToIntFunction<String> font : List.of(SIX, EIGHT)) {
            OverlayLayout lay = new OverlayLayout(font);
            for (String text : new String[]{"+1 PTS [I]", "+30 PTS [I]", "+999", "", null}) {
                for (int free : new int[]{1, 9, 30, 150}) {
                    OverlayLayout.Frame f = lay.panel(panel(free, text), view(1f, 0, 0, 0, false, false));
                    for (Op op : f.ops()) {
                        if (op instanceof Op.Text t && (t.text().startsWith("+") || t.text().contains("PTS"))) {
                            int x1 = t.x() - f.x(), x2 = x1 + font.applyAsInt(t.text());
                            assertTrue(x1 >= OverlayStyle.PAD_X + font.applyAsInt("[ SYSTEM ]") + 8, "clear of the title: " + t.text() + " at " + x1);
                            assertTrue(x2 <= f.w() - OverlayStyle.PAD_X - 4, "clear of the diamond: " + t.text() + " ends " + x2 + " of " + f.w());
                        }
                    }
                }
            }
        }
    }

    @Test void theBadgeBreathesButStaysVisible() {
        OverlayLayout lay = new OverlayLayout(SIX);
        for (float time = 0; time < 100; time += 1.7f) {
            OverlayLayout.View v = new OverlayLayout.View(640, 360, 1f, 0, 0.67f, time);
            for (Op.Text t : texts(lay.panel(panel(4, "+4 PTS [I]"), v))) {
                if (t.text().startsWith("+4")) assertTrue(OverlayMath.drawable(t.argb()), "drawable at " + time);
            }
        }
    }

    // ---------------------------------------------------------------- scale and place
    @Test void theWindowKeepsItsSizeInItsOwnPixelsAtEveryScale() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame base = lay.panel(panel(0, ""), view(1f, 0, 0, 0, false, false));
        for (float s : new float[]{0.5f, 0.75f, 1f, 1.5f, 2f, 2.5f}) {
            OverlayLayout.Frame f = lay.panel(panel(0, ""), view(s, 0, 0, 0, false, false));
            assertEquals(base.w(), f.w());
            assertEquals(base.h(), f.h());
        }
    }

    @Test void theHitBoxIsTheFrameTimesTheScaleAndAlwaysOnTheScreen() {
        OverlayLayout lay = new OverlayLayout(SIX);
        for (float s : new float[]{0.5f, 1f, 1.7f, 2.5f}) {
            for (int a = 0; a < 4; a++) {
                for (boolean c : new boolean[]{false, true}) {
                    for (int off : new int[]{-500, 0, 33, 4096}) {
                        OverlayLayout.Frame f = lay.panel(panel(2, "+2 PTS [I]"), view(s, a, off, off, c, c));
                        double l = f.x() * (double) s, t = f.y() * (double) s, w = f.w() * (double) s, h = f.h() * (double) s;
                        assertTrue(l >= -0.5 && t >= -0.5, "top-left on screen at scale " + s);
                        assertTrue(l + w <= 640 + s && t + h <= 360 + s, "bottom-right on screen at scale " + s + ": " + (l + w) + "," + (t + h));
                    }
                }
            }
        }
    }

    @Test void aCentredViewPutsTheWindowInTheMiddleAndAnOldViewIsUnchanged() {
        OverlayLayout lay = new OverlayLayout(SIX);
        OverlayLayout.Frame c = lay.panel(panel(0, ""), view(1f, 0, 0, 0, true, true));
        assertEquals((640 - c.w()) / 2, c.x());
        assertEquals((360 - c.h()) / 2, c.y());
        OverlayLayout.Frame old = lay.panel(panel(0, ""), new OverlayLayout.View(640, 360, 1f, 3, 0.67f, 10f, 5, 7));
        OverlayLayout.Frame neu = lay.panel(panel(0, ""), view(1f, 3, 5, 7, false, false));
        assertEquals(old, neu, "the eight-argument view is the ten-argument one with the flags off");
        assertEquals(640 - old.w() - OverlayStyle.MARGIN - 5, old.x());
    }

    @Test void aBiggerScaleMeansASmallerScreenInWindowPixels() {
        OverlayLayout.View v = view(2.5f, 0, 0, 0, false, false);
        assertEquals(256, v.w());
        assertEquals(144, v.h());
        assertEquals(1280, view(0.5f, 0, 0, 0, false, false).w());
    }
}
