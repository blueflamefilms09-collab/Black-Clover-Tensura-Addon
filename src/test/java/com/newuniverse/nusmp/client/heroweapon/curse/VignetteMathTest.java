package com.newuniverse.nusmp.client.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the screen tint is the first thing a cursed wielder feels, and it must never blind them: it stays inside the configured cap whatever the corruption, the
 * outer band is the strongest, the heartbeat quickens as corruption rises, and a whisper line fades in, holds, fades out and is silent between. These are pinned
 * with plain numbers.
 */
class VignetteMathTest {
    @Test
    void theTintGrowsWithCorruptionAndNeverPassesTheCap() {
        float last = -1f;
        for (int c = 0; c <= 1000; c += 50) {
            float s = VignetteMath.strength(c, false, false, 0.0, 0.8f);
            assertTrue(s >= 0f && s <= 0.8f + 1e-6f, "corruption " + c + " -> " + s);
            assertTrue(s >= last - 1e-6f || c >= 600, "rises with corruption below the heartbeat: " + c);
            last = s;
        }
        for (double t = 0; t < 10; t += 0.07) {
            assertTrue(VignetteMath.strength(1000, true, false, t, 0.8f) <= 0.8f + 1e-6f, "the heartbeat pulse stays under the cap");
            assertTrue(VignetteMath.strength(1000, true, true, t, 2.0f) <= VignetteMath.HARD_CAP + 1e-6f, "a cap above the hard cap is cut to it");
            assertTrue(VignetteMath.strength(1000, true, true, t, 0.5f) <= 0.5f + 1e-6f);
        }
        assertEquals(0f, VignetteMath.strength(0, false, false, 1.0, 0.8f), 1e-6f, "no corruption, no tint");
        assertEquals(0f, VignetteMath.strength(900, false, false, 1.0, 0.0f), 1e-6f, "a cap of zero switches the tint off");
        assertEquals(0f, VignetteMath.strength(900, false, false, 1.0, Float.NaN), 1e-6f);
        assertTrue(VignetteMath.strength(0, true, false, 0.0, 0.8f) > 0.1f, "an open offer glows even with no corruption");
        assertTrue(VignetteMath.strength(100, false, true, 0.0, 0.8f) > 0.7f, "an Overrun floods the screen");
        assertEquals(0f, VignetteMath.strength(-50, false, false, 0.0, 0.8f), 1e-6f, "a negative corruption is no tint");
    }

    @Test
    void theOuterBandIsTheStrongestAndTheInnerOnesFade() {
        int rings = VignetteMath.rings(0.8f, false);
        assertTrue(rings > VignetteMath.rings(0.0f, false), "a stronger tint is drawn in more bands");
        assertTrue(VignetteMath.rings(0.5f, true) > VignetteMath.rings(0.5f, false), "a narrowed sight (Pride) is drawn in more bands");
        float prev = Float.MAX_VALUE;
        for (int i = 0; i < rings; i++) {
            float a = VignetteMath.ringAlpha(i, rings, 0.8f);
            assertTrue(a >= 0f && a <= 0.8f + 1e-6f && a < prev, "band " + i + " -> " + a);
            prev = a;
        }
        assertEquals(0.8f, VignetteMath.ringAlpha(0, rings, 0.8f), 1e-6f);
        assertEquals(0f, VignetteMath.ringAlpha(rings, rings, 0.8f), 1e-6f);
        assertEquals(0f, VignetteMath.ringAlpha(-1, rings, 0.8f), 1e-6f);
        assertEquals(0f, VignetteMath.ringAlpha(0, 0, 0.8f), 1e-6f);
    }

    @Test
    void theFramesNeverGoPastTheMiddleOfAnyScreenAndTheInnermostIsFaint() {
        int[][] screens = {{640, 360}, {480, 270}, {960, 540}, {1920, 1080}, {427, 240}, {100, 60}, {60, 100}, {20, 20}, {4, 4}, {2, 2}, {0, 0}, {-5, 7}};
        for (int[] sc : screens) {
            int shorter = Math.max(2, Math.min(sc[0], sc[1]));
            for (boolean narrow : new boolean[] {false, true}) {
                for (int step = 0; step <= 10; step++) {
                    float s = step / 10f;
                    VignetteMath.Layout l = VignetteMath.layout(sc[0], sc[1], s, narrow);
                    String at = sc[0] + "x" + sc[1] + " narrow " + narrow + " strength " + s + " -> " + l;
                    assertTrue(l.rings() >= 1 && l.band() >= 1, at);
                    assertTrue(l.depth() <= Math.max(1, shorter / 2 - 1), "the frames stop before the middle: " + at);
                    if (shorter >= 60) assertTrue(VignetteMath.ringAlpha(l.rings() - 1, l.rings(), VignetteMath.HARD_CAP) < 0.02f, "the innermost frame is faint, so no line shows where they end: " + at);
                }
            }
        }
    }

    @Test
    void aCommonScreenGetsTheDesignedFrames() {
        assertEquals(new VignetteMath.Layout(60, 2), VignetteMath.layout(640, 360, 0.8f, false), "a 44th of 360 is 8, cut in three is 2; twenty bands cut in three is 60");
        assertEquals(new VignetteMath.Layout(84, 2), VignetteMath.layout(640, 360, 0.8f, true), "a narrowed sight asks for more and is squeezed in to fit");
        assertEquals(VignetteMath.rings(0.0f, false) * VignetteMath.SMOOTH, VignetteMath.layout(640, 360, 0.0f, false).rings());
        assertTrue(VignetteMath.layout(640, 360, 0.8f, true).depth() > VignetteMath.layout(640, 360, 0.8f, false).depth(), "a narrowed sight reaches further in");
    }

    @Test
    void colourAndOpacityPackIntoArgb() {
        assertEquals(0xFFC2181B, VignetteMath.argb(0xC2181B, 1.0f));
        assertEquals(0x00C2181B, VignetteMath.argb(0xC2181B, 0.0f));
        assertEquals(0x80C2181B, VignetteMath.argb(0xC2181B, 0.5f), "half opacity is 0x80");
        assertEquals(0xFF000001, VignetteMath.argb(0x1000001, 7f), "an opacity above one is clamped and stray high bits of the colour are dropped");
        assertEquals(0x00123456, VignetteMath.argb(0x123456, -3f));
    }

    @Test
    void theHeartbeatQuickensWithCorruption() {
        assertEquals(0, VignetteMath.heartbeatPeriodTicks(0));
        assertEquals(0, VignetteMath.heartbeatPeriodTicks(599));
        assertEquals(20, VignetteMath.heartbeatPeriodTicks(600), "60 beats a minute is one a second");
        int mid = VignetteMath.heartbeatPeriodTicks(800), top = VignetteMath.heartbeatPeriodTicks(1000);
        assertTrue(mid < 20 && top < mid, "faster as corruption rises: " + mid + ", " + top);
        assertEquals(8, top, "150 beats a minute");
        assertEquals(0.0, VignetteMath.beatsPerMinute(100), 1e-9);
        assertEquals(150.0, VignetteMath.beatsPerMinute(5000), 1e-9, "clamped at the top");
        float p0 = VignetteMath.pulse(0.0, 60.0), p1 = VignetteMath.pulse(0.5, 60.0);
        assertEquals(1f, p0, 1e-6f);
        assertTrue(p1 < 0.1f, "the beat falls away: " + p1);
        assertEquals(0f, VignetteMath.pulse(3.0, 0.0), 1e-9f);
        assertTrue(VignetteMath.pulse(-0.3, 60.0) >= 0f && VignetteMath.pulse(-0.3, 60.0) <= 1f, "a clock below zero still gives a beat in range");
    }

    @Test
    void aWhisperFadesInHoldsAndFadesOut() {
        assertEquals(0f, VignetteMath.whisperAlpha(-1, 100), 1e-6f);
        assertEquals(0f, VignetteMath.whisperAlpha(0, 100), 1e-6f);
        assertEquals(0.5f, VignetteMath.whisperAlpha(10, 100), 1e-6f);
        assertEquals(1f, VignetteMath.whisperAlpha(50, 100), 1e-6f);
        assertEquals(0.5f, VignetteMath.whisperAlpha(90, 100), 1e-6f);
        assertEquals(0f, VignetteMath.whisperAlpha(100, 100), 1e-6f);
        assertEquals(0f, VignetteMath.whisperAlpha(5, 0), 1e-6f);
        assertEquals(1f, VignetteMath.whisperAlpha(0, 1), 1e-6f, "a one-tick whisper has no fade");
        assertTrue(VignetteMath.whisperVisible(10, 160, 100));
        assertFalse(VignetteMath.whisperVisible(100, 160, 100));
        assertFalse(VignetteMath.whisperVisible(10, 0, 100));
        assertTrue(VignetteMath.whisperVisible(170, 160, 100), "the window repeats");
        assertEquals(10, VignetteMath.whisperAge(170, 160));
        assertEquals(150, VignetteMath.whisperAge(-10, 160), "a clock below zero still lands in the window");
    }

    @Test
    void theHoldProgressIsAShareOfTheWayThere() {
        assertEquals(0f, VignetteMath.holdProgress(0, 60), 1e-6f);
        assertEquals(0.5f, VignetteMath.holdProgress(30, 60), 1e-6f);
        assertEquals(1f, VignetteMath.holdProgress(60, 60), 1e-6f);
        assertEquals(1f, VignetteMath.holdProgress(500, 60), 1e-6f);
        assertEquals(1f, VignetteMath.holdProgress(5, 0), 1e-6f);
        assertEquals(0f, VignetteMath.holdProgress(-5, 60), 1e-6f);
    }
}
