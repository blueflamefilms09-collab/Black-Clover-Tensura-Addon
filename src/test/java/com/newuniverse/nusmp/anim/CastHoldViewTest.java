package com.newuniverse.nusmp.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The client look of a hold circle: smooth, never restarted by a sign of life. */
class CastHoldViewTest {
    private static final float FAR = 1000f;          // the newest message has plenty of life left

    @Test
    void opensSmoothlyAndStaysOpen() {
        CastHoldView v = CastHoldView.open(100, 0.7f);
        assertEquals(0f, v.alpha(100, FAR), 1e-6f);
        float prev = 0f;
        for (float t = 100; t <= 105; t += 0.25f) {
            float a = v.alpha(t, FAR);
            assertTrue(a >= prev - 1e-6f, "monotonic fade in");
            prev = a;
        }
        assertEquals(0.5f, v.alpha(102.5f, FAR), 1e-6f, "five ticks to open");
        assertEquals(1f, v.alpha(105, FAR), 1e-6f);
        assertEquals(1f, v.alpha(400, FAR), 1e-6f);
    }

    @Test
    void renewalsAreInvisible() {
        CastHoldView v = CastHoldView.open(100, 0.7f);
        CastHoldView w = CastHoldView.open(100, 0.7f);
        for (long now = 110; now <= 300; now += 30) w.renew(now, 0.7f);          // the keep-alive of a long hold
        for (float t = 100; t <= 300; t += 3.3f) {
            assertEquals(v.alpha(t, FAR), w.alpha(t, FAR), 1e-6f, "alpha at " + t);
            assertEquals(v.power(t), w.power(t), 1e-6f, "power at " + t);
            assertEquals(v.age(t), w.age(t), 1e-6f, "the spin clock is never restarted");
        }
    }

    @Test
    void aChargeStepEasesTheSizeInsteadOfJumping() {
        CastHoldView v = CastHoldView.open(100, 0.625f);
        v.renew(120, 0.8f);
        assertEquals(0.625f, v.power(120), 1e-6f, "no jump at the moment of the step");
        float prev = v.power(120);
        for (float t = 120; t <= 126; t += 0.5f) {
            float p = v.power(t);
            assertTrue(p >= prev - 1e-6f, "monotonic growth");
            assertTrue(p - prev < 0.1f, "small steps only: " + (p - prev));
            prev = p;
        }
        assertEquals(0.8f, v.power(126), 1e-6f);
        assertEquals(0.8f, v.power(500), 1e-6f);
        assertEquals(0.625f + 0.175f * 0.15625f, v.power(121.5f), 1e-5f, "eased (smoothstep), not linear: a quarter of the way is 15.6 %");
        assertEquals(0.7125f, v.power(123), 1e-5f, "halfway in six ticks");
        assertEquals(30f, v.age(130), 1e-6f, "the spin clock still counts from the first opening");
    }

    @Test
    void aSecondStepBeforeTheFirstIsDoneStartsFromWhereTheCircleIs() {
        CastHoldView v = CastHoldView.open(100, 0.625f);
        v.renew(120, 0.8f);
        v.renew(123, 1.0f);                                                        // halfway to 0.8
        assertEquals(0.7125f, v.power(123), 1e-5f, "no jump to the old target");
        assertEquals(1.0f, v.power(129), 1e-5f);
    }

    @Test
    void aSmallerPowerIsFollowedToo() {
        CastHoldView v = CastHoldView.open(100, 1.0f);
        v.renew(120, 0.5f);
        assertEquals(0.5f, v.power(130), 1e-6f);
        assertEquals(1.0f, v.power(120), 1e-6f);
    }

    @Test
    void releaseHoldsThroughTheWindUpThenFades() {
        CastHoldView v = CastHoldView.open(100, 1.15f);
        v.end(200, CastHold.endDuration(7));                                      // 13 ticks: 7 of wind-up, 6 of fade
        assertTrue(v.ending());
        assertEquals(1f, v.alpha(200, FAR), 1e-6f);
        assertEquals(1f, v.alpha(207, FAR), 1e-6f, "full through the wind-up");
        assertEquals(0.5f, v.alpha(210, FAR), 1e-5f);
        assertEquals(0f, v.alpha(213, FAR), 1e-6f);
        assertEquals(0f, v.alpha(300, FAR), 1e-6f);
    }

    @Test
    void aCircleBroughtBackWhileFadingEasesUpFromWhereItWas() {
        CastHoldView v = CastHoldView.open(100, 1.15f);
        v.end(200, 13);
        float before = v.alpha(210, FAR);                                          // 0.5, mid fade
        v.renew(210, 1.15f);                                                       // pressed again
        assertFalse(v.ending());
        assertEquals(before, v.alpha(210, FAR), 1e-5f, "no jump at the moment it is brought back");
        float prev = before;
        for (float t = 210; t <= 214; t += 0.5f) {
            float a = v.alpha(t, FAR);
            assertTrue(a >= prev - 1e-6f);
            prev = a;
        }
        assertEquals(0.75f, v.alpha(212, FAR), 1e-5f, "four ticks to come back: half way from 0.5");
        assertEquals(1f, v.alpha(214, FAR), 1e-6f);
    }

    @Test
    void aCircleThatHearsNothingFadesOutByItself() {
        CastHoldView v = CastHoldView.open(100, 0.7f);
        assertEquals(1f, v.alpha(150, CastHold.END_TAIL + 1), 1e-6f);
        assertEquals(0.5f, v.alpha(150, CastHold.END_TAIL / 2f), 1e-6f);
        assertEquals(0f, v.alpha(150, 0f), 1e-6f);
        assertEquals(0f, v.alpha(150, -3f), 1e-6f, "never negative");
    }

    @Test
    void aStaleFadedCircleResumesFromNothing() {
        CastHoldView v = CastHoldView.open(100, 0.7f);
        v.end(200, 13);
        v.renew(250, 0.7f);                                                        // long after it had faded away
        assertEquals(0f, v.alpha(250, FAR), 1e-6f);
        assertEquals(1f, v.alpha(254, FAR), 1e-6f);
    }
}
