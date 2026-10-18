package com.newuniverse.nusmp.elite.forge.rules;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: this is the test of the rule "the client reports, the server clamps and plausibility-checks against elapsed time; the score can never exceed what the
 * sequence allows". It plays whole sequences with perfect input and shows that a result is accepted only after the time it claims has really passed, that an
 * impossible shape is refused, that an old session is refused, and that the total can never be above 1000 whatever is sent.
 */
class SessionCheckTest {
    static final List<SessionCheck.Step> STEPS = List.of(
            new SessionCheck.Step(MinigameType.HAMMER_TIMING, 2), new SessionCheck.Step(MinigameType.TEMPERATURE_FORGE, 1),
            new SessionCheck.Step(MinigameType.RUNE_ALIGNMENT, 3), new SessionCheck.Step(MinigameType.MAGICULE_FLOW, 4));
    static final long[] SEEDS = {11L, 22L, 33L, 44L};

    static List<int[]> perfect() {
        List<int[]> in = new ArrayList<>();
        for (int i = 0; i < STEPS.size(); i++) in.add(Minigames.of(STEPS.get(i).type()).perfectInput(SEEDS[i], STEPS.get(i).difficulty()));
        return in;
    }

    static long claimed(List<int[]> in) {
        long c = 0;
        for (int i = 0; i < STEPS.size(); i++) c += Minigames.of(STEPS.get(i).type()).claimedMillis(SEEDS[i], STEPS.get(i).difficulty(), in.get(i));
        return c;
    }

    @Test
    void anHonestSequenceIsAcceptedAfterItsTime() {
        List<int[]> in = perfect();
        long c = claimed(in);
        SessionCheck.Verdict v = SessionCheck.evaluate(STEPS, SEEDS, in, c + 500);
        assertTrue(v.ok(), v.reason());
        assertEquals(4, v.stepScores().length);
        assertTrue(v.total() >= 900 && v.total() <= 1000, "total " + v.total());
        for (int s : v.stepScores()) assertTrue(s >= 0 && s <= 1000);
    }

    @Test
    void aResultThatCameFasterThanItCanBePlayedIsRefused() {
        List<int[]> in = perfect();
        long c = claimed(in);
        assertTrue(c > 20_000, "the test sequence takes a while to play: " + c);
        SessionCheck.Verdict v = SessionCheck.evaluate(STEPS, SEEDS, in, 4000);
        assertFalse(v.ok());
        assertTrue(v.reason().contains("faster"), v.reason());
        // the slack allows the real network and tick rounding, nothing more
        assertTrue(SessionCheck.evaluate(STEPS, SEEDS, in, c - SessionCheck.SLACK_MS + 100).ok() || c < SessionCheck.SLACK_MS);
        assertFalse(SessionCheck.evaluate(STEPS, SEEDS, in, c / 2).ok());
    }

    @Test
    void anOldSessionIsRefused() {
        List<int[]> in = perfect();
        assertFalse(SessionCheck.evaluate(STEPS, SEEDS, in, SessionCheck.maxElapsedMillis(STEPS) + 1).ok());
        assertTrue(SessionCheck.evaluate(STEPS, SEEDS, in, SessionCheck.maxElapsedMillis(STEPS)).ok());
    }

    @Test
    void wrongShapesAreRefused() {
        List<int[]> in = perfect();
        long t = claimed(in) + 1000;
        assertFalse(SessionCheck.evaluate(STEPS, SEEDS, in.subList(0, 3), t).ok(), "a missing step");
        assertFalse(SessionCheck.evaluate(STEPS, new long[]{1L}, in, t).ok(), "seeds that do not match");
        assertFalse(SessionCheck.evaluate(List.of(), SEEDS, in, t).ok(), "no steps");
        List<int[]> bad = new ArrayList<>(in);
        int[] flow = bad.get(3).clone();
        flow[7] = 5000;
        bad.set(3, flow);
        SessionCheck.Verdict v = SessionCheck.evaluate(STEPS, SEEDS, bad, t);
        assertFalse(v.ok());
        assertTrue(v.reason().startsWith("step 4"), v.reason());
        bad.set(3, new int[100000]);
        assertFalse(SessionCheck.evaluate(STEPS, SEEDS, bad, t).ok(), "an oversized input");
        bad.set(3, null);
        assertFalse(SessionCheck.evaluate(STEPS, SEEDS, bad, t).ok(), "a null input");
        assertFalse(SessionCheck.evaluate(STEPS, SEEDS, in, -5).ok());
    }

    @Test
    void noInputCanScoreAboveTheMaximum() {
        java.util.Random rnd = new java.util.Random(9);
        for (int round = 0; round < 200; round++) {
            List<int[]> in = new ArrayList<>();
            for (SessionCheck.Step s : STEPS) {
                Minigame g = Minigames.of(s.type());
                int len = g.maxInputLength(s.difficulty());
                int[] a = new int[len];
                for (int i = 0; i < len; i++) a[i] = rnd.nextInt(4000) - 100;
                in.add(a);
            }
            SessionCheck.Verdict v = SessionCheck.evaluate(STEPS, SEEDS, in, 10_000_000L);
            if (v.ok()) {
                assertTrue(v.total() <= 1000 && v.total() >= 0);
                for (int s : v.stepScores()) assertTrue(s >= 0 && s <= 1000);
            }
        }
        // a lone perfect step is exactly the best a step can do, and a single step sequence is its own average
        SessionCheck.Step one = new SessionCheck.Step(MinigameType.HAMMER_TIMING, 1);
        int[] p = Minigames.HAMMER.perfectInput(5L, 1);
        SessionCheck.Verdict v = SessionCheck.evaluate(List.of(one), new long[]{5L}, List.of(p), 60_000);
        assertTrue(v.ok());
        assertEquals(v.stepScores()[0], v.total());
    }

    @Test
    void theDifficultyIsClampedInAStep() {
        assertEquals(5, new SessionCheck.Step(MinigameType.HAMMER_TIMING, 99).difficulty());
        assertEquals(1, new SessionCheck.Step(MinigameType.HAMMER_TIMING, -4).difficulty());
    }
}
