package com.newuniverse.nusmp.client.hub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the hub's motion is pure maths (the intro timeline, the fixed mote pool) and its text fitting is a pure rule; all three are checked here with no game types: the timeline is monotone and ends complete, the
 * mote pool never grows and stays on the stage, and fitted text never exceeds its box and says when it was cut.
 */
class HubMotionTest {
    @Test
    void theIntroIsMonotoneAndEndsComplete() {
        float prevCore = -1f, prevRings = -1f, prevLabels = -1f;
        for (float age = 0f; age <= HubTimeline.INTRO + 20f; age += 0.5f) {
            assertTrue(HubTimeline.rings(age) >= prevRings && HubTimeline.labels(age) >= prevLabels, "rings and labels never go back");
            prevRings = HubTimeline.rings(age);
            prevLabels = HubTimeline.labels(age);
            for (int i = 0; i < HubNodes.COUNT; i++) {
                assertTrue(HubTimeline.node(age, i) >= 0f && HubTimeline.node(age, i) <= 1f);
                if (i > 0) assertTrue(HubTimeline.node(age, i) <= HubTimeline.node(age, i - 1) + 1e-6f, "node " + i + " lights after node " + (i - 1));
            }
            prevCore = HubTimeline.core(age);
        }
        float end = HubTimeline.INTRO;
        assertEquals(1f, HubTimeline.rings(end), 1e-6);
        assertEquals(1f, HubTimeline.labels(end), 1e-6);
        assertEquals(1f, HubTimeline.core(end), 1e-6);
        for (int i = 0; i < HubNodes.COUNT; i++) assertEquals(1f, HubTimeline.node(end, i), 1e-6, "node " + i + " is lit when the intro ends");
        assertTrue(HubTimeline.introDone(end) && !HubTimeline.introDone(end - 1f));
        assertTrue(prevCore == 1f);
    }

    @Test
    void theLastNodeIsLitBeforeTheLabelsFinishFading() {
        float lastLit = HubTimeline.NODE_START + (HubNodes.COUNT - 1) * HubTimeline.NODE_STEP + HubTimeline.NODE_FADE;
        assertTrue(lastLit <= HubTimeline.INTRO, "all nodes light inside the intro (" + lastLit + ")");
    }

    @Test
    void openSweepAndRippleRunForTheirTimeThenStop() {
        assertEquals(0f, HubTimeline.open(0f), 1e-6);
        assertEquals(1f, HubTimeline.open(HubTimeline.OPEN), 1e-6);
        assertEquals(320f, HubTimeline.OPEN * 50f, 1e-3, "the open sweep is 320 ms");
        assertEquals(520f, HubTimeline.RIPPLE * 50f, 1e-3, "the click ripple is 520 ms");
        assertTrue(HubTimeline.sweep(1f) > 0f && HubTimeline.sweep(1f) < 1f);
        assertEquals(-1f, HubTimeline.sweep(HubTimeline.OPEN + 0.1f));
        assertEquals(-1f, HubTimeline.ripple(HubTimeline.RIPPLE));
        assertEquals(-1f, HubTimeline.ripple(-1f));
        assertTrue(HubTimeline.ripple(5f) > 0f);
    }

    @Test
    void aSoundPlaysInTheFrameThatCrossesItsTick() {
        assertTrue(HubTimeline.crossed(1.9f, 2.1f, HubTimeline.SOUND_A));
        assertFalse(HubTimeline.crossed(2.1f, 2.3f, HubTimeline.SOUND_A), "only once");
        assertFalse(HubTimeline.crossed(0f, 1.9f, HubTimeline.SOUND_A));
    }

    @Test
    void theMotePoolIsFixedStaysOnTheStageAndIsDeterministic() {
        HubMotes a = new HubMotes(0x5EEDL, HubLayout.STAGE_W, HubLayout.STAGE_H), b = new HubMotes(0x5EEDL, HubLayout.STAGE_W, HubLayout.STAGE_H);
        assertEquals(24, HubMotes.COUNT);
        assertEquals(24, a.x.length);
        for (int step = 0; step < 5000; step++) {
            a.step(3f);
            b.step(3f);
            for (int i = 0; i < HubMotes.COUNT; i++) {
                assertTrue(a.x[i] >= 0f && a.x[i] < HubLayout.STAGE_W && a.y[i] >= 0f && a.y[i] < HubLayout.STAGE_H, "mote " + i + " left the stage at step " + step);
            }
        }
        for (int i = 0; i < HubMotes.COUNT; i++) {
            assertEquals(a.x[i], b.x[i], 0f);
            assertEquals(a.y[i], b.y[i], 0f);
            float t = a.twinkle(i, 123f);
            assertTrue(t >= 0f && t <= 1f && (a.size[i] == 1 || a.size[i] == 2));
        }
    }

    // six pixels a character, like the preview tool's stand-in font
    private static int width(String s) { return s.length() * 6; }

    @Test
    void fittedTextNeverExceedsItsBoxAndSaysWhenItWasCut() {
        String[] texts = {"", "Hi", "Change form: Hero's Resolve - ready", "A very long form name that cannot possibly fit in a small row", "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"};
        for (String t : texts) for (int max = 0; max <= 300; max += 7) {
            HubText.Fit f = HubText.fit(t, max, HubMotionTest::width);
            assertEquals(t, f.full());
            assertTrue(width(f.shown()) <= max || (f.shown().isEmpty()), "'" + f.shown() + "' (" + width(f.shown()) + ") over " + max);
            assertEquals(width(t) > max, f.cut(), "cut flag for '" + t + "' at " + max);
            if (!f.cut()) assertEquals(t, f.shown());
            else if (!f.shown().isEmpty()) assertTrue(f.shown().endsWith(HubText.ELLIPSIS));
        }
        assertEquals("Change form: He...", HubText.fit("Change form: Hero's Resolve - ready", 18 * 6, HubMotionTest::width).shown());
        assertEquals("", HubText.fit(null, 10, HubMotionTest::width).shown());
    }

    @Test
    void fittingKeepsTheLongestPrefixThatFits() {
        String t = "abcdefghijklmnopqrstuvwxyz";
        for (int max = 18; max < width(t); max += 6) {
            HubText.Fit f = HubText.fit(t, max, HubMotionTest::width);
            String shown = f.shown().substring(0, f.shown().length() - 3);
            assertTrue(width(shown) + 18 <= max && width(shown) + 6 + 18 > max, "prefix of '" + shown + "' is not the longest at " + max);
        }
    }
}
