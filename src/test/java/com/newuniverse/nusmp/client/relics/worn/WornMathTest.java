package com.newuniverse.nusmp.client.relics.worn;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** The worn boss relics' maths: orbits, the crown's pull and burst, the ring's flare and the Mimic's clip clock. */
class WornMathTest {
    @Test
    void anglesWrapAndInterpolateTheShortWay() {
        assertEquals(10.0, WornMath.wrap(370f), 1e-4);
        assertEquals(350.0, WornMath.wrap(-10f), 1e-4);
        assertEquals(0.0, WornMath.wrap(720f), 1e-4);
        assertEquals(355.0, WornMath.lerpAngle(350f, 10f, 0.25f), 1e-3, "350 to 10 goes through 0, not back through 180");
        assertEquals(365.0, WornMath.lerpAngle(350f, 10f, 0.75f), 1e-3, "the result is not wrapped: callers wrap when they need to");
        assertEquals(45.0, WornMath.lerpAngle(0f, 90f, 0.5f), 1e-3);
    }

    @Test
    void shardsAreSpreadEvenlyAndTurnSteadily() {
        for (int n : new int[]{3, 6}) {
            float step = 360f / n;
            for (int i = 1; i < n; i++) assertEquals(step, WornMath.orbit(0f, i, n, 2.4f) - WornMath.orbit(0f, i - 1, n, 2.4f), 1e-3);
        }
        assertEquals(WornMath.orbit(0f, 0, 6, 2.4f) + 24f, WornMath.orbit(10f, 0, 6, 2.4f), 1e-3, "2.4 degrees a tick");
        float prev = WornMath.orbit(100f, 2, 6, 2.4f), next = WornMath.orbit(100.5f, 2, 6, 2.4f);
        assertEquals(1.2f, next - prev, 1e-3, "a half tick is half a step: partial ticks interpolate");
        assertTrue(Math.abs(WornMath.bob(5f, 1, 0.9f)) <= 0.9f + 1e-6);
        assertNotEquals(WornMath.bob(5f, 0, 1f), WornMath.bob(5f, 1, 1f), "out of step");
    }

    @Test
    void curvesAreHeldOutsideTheirKeysAndHitThem() {
        float[] t = {0f, 10f, 20f}, v = {1f, 3f, 2f};
        assertEquals(1.0, WornMath.curve(t, v, -5f), 1e-6);
        assertEquals(3.0, WornMath.curve(t, v, 10f), 1e-6);
        assertEquals(2.0, WornMath.curve(t, v, 99f), 1e-6);
        float mid = WornMath.curve(t, v, 5f);
        assertTrue(mid > 1f && mid < 3f);
        assertEquals(2.0, mid, 1e-6, "eased: halfway is the midpoint");
        assertEquals(0.0, WornMath.curve(new float[0], new float[0], 3f), 1e-6);
    }

    @Test
    void theCrownPullsInHoldsBurstsAndSettlesBackToRest() {
        assertEquals(1.0, WornMath.wellRadius(-1f), 1e-6, "no cue: rest");
        assertEquals(1.0, WornMath.wellRadius(0f), 1e-6, "the beat starts at rest");
        assertEquals(0.35, WornMath.wellRadius(14f), 1e-6, "pulled in");
        assertEquals(0.35, WornMath.wellRadius(16f), 1e-6, "held");
        assertTrue(WornMath.wellRadius(26f) > 1.4f, "burst out");
        assertEquals(1.0, WornMath.wellRadius(40f), 1e-6, "settled when the beat ends");
        assertEquals(1.0, WornMath.wellRadius(500f), 1e-6);
        float last = WornMath.wellRadius(0f);
        for (float x = 0.5f; x <= 40f; x += 0.5f) {
            float r = WornMath.wellRadius(x);
            assertTrue(Math.abs(r - last) < 0.2f, "no jumps at " + x);
            last = r;
        }
        assertTrue(WornMath.wellGlow(16f) > 2f);
        assertEquals(1.0, WornMath.wellGlow(-1f), 1e-6);
        assertEquals(0.0, WornMath.wellTurn(-1f), 1e-6);
        assertEquals(360.0, WornMath.wellTurn(WornMath.WELL_TICKS), 1e-6, "ends on a whole turn: no snap back");
        assertEquals(360.0, WornMath.wellTurn(900f), 1e-6);
    }

    @Test
    void theRingFlaresAndSpinsWholeTurns() {
        assertEquals(1.0, WornMath.ringExpand(-1f, WaveBossKinds.STEP), 1e-6);
        assertEquals(1.0, WornMath.ringExpand(0f, WaveBossKinds.STEP), 1e-6);
        float peak = 1f;
        for (float t = 0; t < WornMath.STEP_TICKS; t += 0.25f) peak = Math.max(peak, WornMath.ringExpand(t, WaveBossKinds.STEP));
        assertTrue(peak > 1.7f && peak < 2.0f, "nearly double: " + peak);
        assertEquals(1.0, WornMath.ringExpand(WornMath.STEP_TICKS, WaveBossKinds.STEP), 1e-6, "back to size");
        assertEquals(1.0, WornMath.ringExpand(5f, 0), 1e-6, "no kind, no flare");
        assertEquals(360.0, WornMath.ringTurn(WornMath.STEP_TICKS, WaveBossKinds.STEP), 1e-6);
        assertEquals(720.0, WornMath.ringTurn(WornMath.GATES_TICKS + 3, WaveBossKinds.GATES), 1e-6, "the gates turn it twice");
        assertEquals(0.0, WornMath.ringTurn(-1f, WaveBossKinds.GATES), 1e-6);
        assertTrue(WornMath.ringGlow(WornMath.STEP_TICKS / 2f, WaveBossKinds.STEP) > 2.5f);
        assertEquals(1.0, WornMath.ringGlow(99f, WaveBossKinds.STEP), 1e-6);
    }

    @Test
    void theMimicPlaysItsBeatOnceThenIdles() {
        assertEquals(WornMath.IDLE, WornMath.mimicClip(0, -1f));
        assertEquals(WornMath.BITE, WornMath.mimicClip(WaveBossKinds.BITE, 0f));
        assertEquals(WornMath.BITE, WornMath.mimicClip(WaveBossKinds.BITE, WornMath.BITE_TICKS - 0.1f));
        assertEquals(WornMath.IDLE, WornMath.mimicClip(WaveBossKinds.BITE, WornMath.BITE_TICKS), "the bite is over");
        assertEquals(WornMath.YAWN, WornMath.mimicClip(WaveBossKinds.YAWN, 5f));
        assertEquals(WornMath.IDLE, WornMath.mimicClip(WaveBossKinds.YAWN, WornMath.YAWN_TICKS + 1));
        assertEquals(1.0, WornMath.mimicSeconds(WornMath.BITE, 20f, 999f), 1e-6, "a beat runs on its own clock, 20 ticks a second");
        assertEquals(5.0, WornMath.mimicSeconds(WornMath.IDLE, -1f, 100f), 1e-6, "the idle loop runs on the wearer's age");
        assertEquals(28, WornMath.mimicTicks(WornMath.BITE));
        assertEquals(0, WornMath.mimicTicks(WornMath.IDLE));
    }

    @Test
    void cueAgeCountsFromTheStampAndPartialTick() {
        assertEquals(-1.0, WornMath.since(100L, -1L, 0.5f), 1e-6, "no stamp");
        assertEquals(10.5, WornMath.since(110L, 100L, 0.5f), 1e-6);
        assertTrue(WornMath.active(0f, 10));
        assertFalse(WornMath.active(10f, 10));
        assertFalse(WornMath.active(-1f, 10));
    }

    /** The clip lengths in the Mimic's animation file (written by tools/relics/waveboss_mimic_geo.py) are the ones this class plays by. */
    @Test
    void theAnimationFileHasTheClipsAndLengthsTheClassPlays() throws Exception {
        try (InputStream in = WornMathTest.class.getResourceAsStream("/assets/nusmp/animations/entity/relic_mimic_belt.animation.json")) {
            assertNotNull(in, "the Mimic's animation file is a resource");
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(WornMath.BITE_TICKS / 20.0, length(json, WornMath.BITE), 1e-4);
            assertEquals(WornMath.YAWN_TICKS / 20.0, length(json, WornMath.YAWN), 1e-4);
            assertTrue(length(json, WornMath.IDLE) > 1.0, "the idle loop exists");
            assertTrue(json.contains("\"lid\""), "the lid is animated");
        }
    }

    private static double length(String json, String clip) {
        Matcher m = Pattern.compile("\"" + clip + "\"\\s*:\\s*\\{[^{}]*?\"animation_length\"\\s*:\\s*([0-9.]+)").matcher(json);
        assertTrue(m.find(), "clip " + clip + " is in the file");
        return Double.parseDouble(m.group(1));
    }
}
