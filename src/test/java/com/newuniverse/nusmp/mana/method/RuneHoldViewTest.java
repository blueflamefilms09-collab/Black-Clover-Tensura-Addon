package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout.Layout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What the clients draw of a held rune: the timeline of the four ring groups, what a release completes, and the staged layout that follows it. */
class RuneHoldViewTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;
    private static final RuneSpec SPEC = new RuneSpec(RuneElement.FIRE, RuneMods.ALL, true);
    private static final Frame FRAME = RuneGeneratorUtil.frameFacing(0, 70, 0, 0, 0, 1);

    private static RuneHoldView view() { return new RuneHoldView(7, SPEC, MIN, MAX, FRAME); }

    private static double[] written(RuneHoldView v, double age) {
        double[] w = new double[4];
        v.written(age, w);
        return w;
    }

    @Test
    void theRingsAreWrittenOneAfterTheOtherWhileTheKeyIsHeld() {
        RuneHoldView v = view();
        double[] w = written(v, 0);
        assertArrayEquals(new double[]{0, 0, 0, 0}, w, 1e-12);
        w = written(v, MIN / 2.0);
        assertEquals(0.5, w[0], 1e-9);
        assertEquals(0.0, w[1], 1e-12, "the middle ring waits for the hand ring");
        w = written(v, MIN);
        assertEquals(1.0, w[0], 1e-12, "the hand ring is finished exactly at the minimum hold");
        assertEquals(0.0, w[1], 1e-12);
        w = written(v, MAX);
        assertArrayEquals(new double[]{1, 1, 1, 1}, w, 1e-12, "everything is written at the full hold");
        w = written(v, MAX + 500);
        assertArrayEquals(new double[]{1, 1, 1, 1}, w, 1e-12, "holding longer changes nothing");
    }

    @Test
    void theProgressNeverGoesBackWhileHeld() {
        RuneHoldView v = view();
        double[] last = new double[4], w;
        for (double age = 0; age <= MAX + 20; age += 0.5) {
            w = written(v, age);
            for (int g = 0; g < 4; g++) {
                assertTrue(w[g] >= last[g] - 1e-12, "group " + g + " went back at " + age);
                assertTrue(w[g] >= 0 && w[g] <= 1);
            }
            last = w;
        }
    }

    @Test
    void aTapReleasedFinishesOnlyTheHandRing() {
        RuneHoldView v = view();
        v.release(2f, 2);
        assertTrue(v.released());
        double[] w = written(v, 2 + RuneHoldView.COMPLETE_TICKS + 3);
        assertEquals(1.0, w[0], 1e-12, "the basic spell is a finished hand ring");
        assertEquals(0.0, w[1], 1e-12);
        assertEquals(0.0, w[2], 1e-12);
        assertEquals(0.0, w[3], 1e-12);
        assertEquals(RuneCharge.Stage.BASIC, v.stageAt(50));
        assertEquals(1, v.tierFor(2));
    }

    @Test
    void aChargedReleaseFinishesTwoRingsAndLeavesTheOuterOneAsFarAsItGot() {
        RuneHoldView v = view();
        int held = (MIN + MAX) / 2;
        v.release(held, held);
        double[] w = written(v, held + RuneHoldView.COMPLETE_TICKS + 3);
        assertEquals(1.0, w[0], 1e-12);
        assertEquals(1.0, w[1], 1e-12, "the middle ring is completed by the release");
        assertTrue(w[2] > 0.0 && w[2] < 1.0, "the outer ring stays as far as the hold got it: " + w[2]);
        assertEquals((held - MIN) / (double) (MAX - MIN), w[2], 1e-9);
        assertEquals(RuneCharge.Stage.CHARGED, v.stageAt(1000));
        assertEquals(2, v.tierFor(held));
    }

    @Test
    void aFullReleaseHasEverything() {
        RuneHoldView v = view();
        v.release(MAX + 4, MAX + 4);
        double[] w = written(v, MAX + 20);
        assertArrayEquals(new double[]{1, 1, 1, 1}, w, 1e-12);
        assertEquals(RuneCharge.Stage.FULL, v.stageAt(5));
        assertEquals(3, v.tierFor(MAX));
    }

    @Test
    void theClockStopsAtTheReleaseAndASecondReleaseChangesNothing() {
        RuneHoldView v = view();
        v.release(10f, 10);
        v.release(30f, 30);
        assertEquals(10, v.holdAtRelease);
        assertEquals(10f, v.releaseAge);
        assertEquals(10, v.heldAt(500));
        assertEquals(12, view().heldAt(12.7), "while held the clock is the age");
        RuneHoldView capped = view();
        assertEquals(RuneCharge.HOLD_CAP, capped.heldAt(99999));
    }

    @Test
    void theLimitsAreKeptInTheSameRangesAsTheSettings() {
        RuneHoldView v = new RuneHoldView(1, SPEC, -4, 1, FRAME);
        assertEquals(RuneCharge.MIN_HOLD_LOW, v.minHold);
        assertTrue(v.maxHold >= v.minHold + RuneCharge.MIN_SPAN);
        assertEquals(v.maxHold + RuneHoldView.LIFE_AFTER_FULL, v.lifetime());
    }

    @Test
    void theFlareAndTheFadeFollowTheLaunch() {
        RuneHoldView v = view();
        assertEquals(0.0, v.flare(5));
        assertEquals(1f, v.fade(5), "no end known: the rune is bright for its whole life");
        v.launchAge = 20f;
        v.endAge = 30f;
        assertEquals(0.0, v.flare(20));
        assertEquals(0.5, v.flare(20 + RuneRules.FLARE_TICKS / 2.0), 1e-9);
        assertEquals(1.0, v.flare(40));
        assertEquals(1f, v.fade(30 - RuneHoldView.FADE));
        assertEquals(0.5f, v.fade(30 - RuneHoldView.FADE / 2.0), 1e-6f);
        assertEquals(0f, v.fade(30));
        assertEquals(0f, v.fade(1000));
    }

    @Test
    void thePenSpeedFinishesEachRingInTheTicksItIsAllotted() {
        RuneHoldView v = view();
        for (int g = 0; g < 4; g++) {
            assertTrue(v.ticksPerProgress(g) >= 1.0, "group " + g);
            assertTrue(v.ticksPerProgress(g) < 400.0);
        }
        double[] prog = new double[4], scale = new double[4], scratch = new double[4];
        v.progress(MIN / 2.0, prog, scale, scratch);
        assertEquals(RuneGlyphLayout.progressOf(0, 0.5), prog[0], 1e-9);
        assertEquals(RuneGlyphLayout.progressOf(1, 0.0), prog[1], 1e-9);
    }

    // ---------------------------------------------------------------- the registry
    @Test
    void theRegistryHandsOutKeysThatTheSeedCarries() {
        RuneHoldView a = view(), b = view();
        int ka = RuneHoldView.register(a), kb = RuneHoldView.register(b);
        assertTrue(ka >= 1 && ka <= 255 && kb >= 1 && kb <= 255 && ka != kb);
        assertSame(a, RuneHoldView.lookup(ka));
        assertSame(b, RuneHoldView.lookup(kb));
        long seed = RuneHoldView.seedOf(SPEC.pack(), ka);
        assertEquals(ka, RuneHoldView.keyOf(seed));
        assertEquals(SPEC.pack(), (int) (seed & 63L), "the spec is still in bits 0..5, where the old layer reads it");
        assertEquals(0, RuneHoldView.keyOf(SPEC.pack()), "the old seed (no key) has key 0");
        assertNull(RuneHoldView.lookup(0));
        assertNull(RuneHoldView.lookup(-3));
        assertNull(RuneHoldView.lookup(9999));
        RuneHoldView.forgetAll();
        assertNull(RuneHoldView.lookup(ka));
        for (int i = 0; i < 600; i++) {
            int k = RuneHoldView.register(view());
            assertTrue(k >= 1 && k <= 255, "keys wrap round 1..255");
        }
        RuneHoldView.forgetAll();
    }

    // ---------------------------------------------------------------- the staged layout
    private static Layout staged(RuneHoldView v, double age) {
        Layout l = new Layout();
        double[] prog = new double[4], scale = new double[4], scratch = new double[4];
        v.progress(age, prog, scale, scratch);
        Params p = RuneGeneratorUtil.paramsFor(SPEC);
        RuneGlyphLayout.fillStaged(l, SPEC, p, FRAME, age, prog, scale, v.flare(age), RuneGlyphLayout.LEVEL_FULL, 5, 72, 5);
        return l;
    }

    private static RuneHoldView released(int held) {
        RuneHoldView v = view();
        v.release(held, held);
        return v;
    }

    @Test
    void theStagesDrawMoreAndMoreGlyphsAndAFullHoldIsTheWholeOldPicture() {
        int tap = staged(released(1), 40).strips, charged = staged(released((MIN + MAX) / 2), 80).strips, full = staged(released(MAX), 120).strips;
        assertTrue(tap > 0, "a tap draws its hand ring");
        assertTrue(tap < charged, "two rings are more than one: " + tap + " < " + charged);
        assertTrue(charged < full, "three rings are more than two: " + charged + " < " + full);
        Layout old = new Layout();
        RuneGlyphLayout.fill(old, SPEC, RuneGeneratorUtil.paramsFor(SPEC), FRAME, 200, 18, RuneGlyphLayout.LEVEL_FULL, 5, 72, 5);
        assertEquals(old.strips, full, "a full hold writes exactly the strokes the 3.4.0 rune ends with");
        assertEquals(old.lines, staged(released(MAX), 120).lines);
    }

    @Test
    void whileHeldTheStrokesNeverDisappear() {
        RuneHoldView v = view();
        int last = 0;
        for (double age = 0; age <= MAX + 10; age += 1.0) {
            Layout l = staged(v, age);
            assertTrue(l.strips >= last, "strokes vanished at " + age + ": " + l.strips + " < " + last);
            assertTrue(l.strips <= RuneGlyphLayout.MAX_STRIPS && l.pens <= RuneGlyphLayout.MAX_PENS && l.lines <= RuneGlyphLayout.MAX_LINES);
            last = l.strips;
        }
    }

    @Test
    void theStagedLayoutIsBoundedAtEveryLevelAndNothingIsDrawnAtLevelNone() {
        RuneHoldView v = released(MAX);
        double[] prog = new double[4], scale = new double[4], scratch = new double[4];
        v.progress(60, prog, scale, scratch);
        for (int level : new int[]{RuneGlyphLayout.LEVEL_FULL, RuneGlyphLayout.LEVEL_MEDIUM, RuneGlyphLayout.LEVEL_LOW, RuneGlyphLayout.LEVEL_MIN}) {
            Layout l = new Layout();
            RuneGlyphLayout.fillStaged(l, SPEC, RuneGeneratorUtil.paramsFor(SPEC), FRAME, 60, prog, scale, 1.0, level, 5, 72, 5);
            assertTrue(l.strips + l.pens + l.lines + l.arcQuads + 1 <= RuneGlyphLayout.MAX_QUADS, "level " + level);
        }
        Layout none = new Layout();
        RuneGlyphLayout.fillStaged(none, SPEC, RuneGeneratorUtil.paramsFor(SPEC), FRAME, 60, prog, scale, 1.0, RuneGlyphLayout.LEVEL_NONE, 5, 72, 5);
        assertEquals(0, none.strips + none.pens + none.lines + none.arcQuads);
    }

    @Test
    void everyCoordinateIsFinite() {
        RuneHoldView v = released(MAX);
        for (double age : new double[]{0, 1, 17.5, 60, 200}) {
            Layout l = staged(v, age);
            for (int i = 0; i < l.strips * 12; i++) assertTrue(Float.isFinite(l.sq[i]), "strip coordinate at age " + age);
            for (int i = 0; i < l.lines * 12; i++) assertTrue(Float.isFinite(l.lq[i]));
        }
    }
}
