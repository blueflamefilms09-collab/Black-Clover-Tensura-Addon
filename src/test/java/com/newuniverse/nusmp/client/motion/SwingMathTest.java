package com.newuniverse.nusmp.client.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Why: the whip / flail swing (docs/vassal_forms_v2.md) is a damped spring chain driven by the swing's progress. The maths is pure, so it is pinned here: nothing moves without a swing,
 * the head trails behind a forward strike and swings past when the arm stops, every bone stays inside its limit, the ring-out decays, a bad number never poisons the chain, and the
 * trajectory equals the one tools/weaponmotion/swing_math.py computes (the preview strips are drawn from that file, so the strips are what the game does).
 */
class SwingMathTest {
    private static final int N = 3;

    private static float[] fill(float v) {
        float[] a = new float[N];
        Arrays.fill(a, v);
        return a;
    }

    /** Runs the 0.3 s strike of the golden case at 60 fps for {@code frames} frames; returns the angles at the frames in {@code report}. */
    private static float[][] strike(int frames, int... report) {
        float[] th = new float[N], thv = new float[N];
        float[] w = fill(11f), zeta = fill(0.22f), gain = fill(1f), max = fill((float) Math.toRadians(9.0));
        float[][] out = new float[report.length][];
        float dt = 1f / 60f;
        for (int f = 1; f <= frames; f++) {
            float u = f * dt / 0.3f;
            SwingMath.step(th, thv, N, w, zeta, gain, max, SwingMath.armAccel(u, 1f / 0.3f), dt);
            for (int k = 0; k < report.length; k++) if (report[k] == f) out[k] = th.clone();
        }
        return out;
    }

    @Test
    void noSwingNoMotion() {
        float[] th = new float[N], thv = new float[N];
        for (int i = 0; i < 600; i++) SwingMath.step(th, thv, N, fill(11f), fill(0.2f), fill(1f), fill(0.2f), SwingMath.armAccel(0f, 3f), 1f / 60f);
        assertEquals(0f, th[0], 0f);
        assertEquals(0f, th[N - 1], 0f);
        assertEquals(0f, SwingMath.armAccel(0.5f, 0f), 0f, "a swing that does not advance gives no force");
        assertEquals(0f, SwingMath.armAccel(1f, 3f), 0f, "after the swing");
        assertEquals(0f, SwingMath.armAccel(Float.NaN, 3f), 0f);
    }

    @Test
    void theDriveIsBoundedAndChangesSign() {
        float first = SwingMath.armAccel(0.1f, 3.3f), last = SwingMath.armAccel(0.9f, 3.3f);
        assertTrue(first > 0f && last < 0f, "the arm speeds up, then slows down: " + first + " / " + last);
        for (float u = 0.001f; u < 1f; u += 0.01f) {
            assertTrue(Math.abs(SwingMath.armAccel(u, 1000f)) <= SwingMath.MAX_ARM_ACCEL + 1e-3f, "a huge rate is clamped");
        }
    }

    @Test
    void theHeadTrailsAForwardStrikeThenSwingsPast() {
        float[][] r = strike(40, 3, 12, 18);
        assertTrue(r[0][0] < 0f, "the first bone lags behind the arm at the start of the strike: " + r[0][0]);
        float min = 0f, max = 0f;
        float[] th = new float[N], thv = new float[N];
        float[] w = fill(11f), zeta = fill(0.22f), gain = fill(1f), mx = fill((float) Math.toRadians(9.0));
        for (int f = 1; f <= 120; f++) {
            SwingMath.step(th, thv, N, w, zeta, gain, mx, SwingMath.armAccel(f / 60f / 0.3f, 1f / 0.3f), 1f / 60f);
            min = Math.min(min, th[N - 1]);
            max = Math.max(max, th[N - 1]);
        }
        assertTrue(min < -0.02f && max > 0.02f, "the tip lags and then whips past: " + min + " .. " + max);
    }

    @Test
    void everyBoneStaysInsideItsLimit() {
        float lim = (float) Math.toRadians(9.0);
        float[] th = new float[N], thv = new float[N];
        for (int f = 1; f <= 400; f++) {
            float u = (f % 40) / 40f;
            SwingMath.step(th, thv, N, fill(11f), fill(0.22f), fill(3f), fill(lim), SwingMath.armAccel(u, 6f), 1f / 60f);
            for (int i = 0; i < N; i++) assertTrue(Math.abs(th[i]) <= lim + 1e-6f, "bone " + i + " at frame " + f + ": " + th[i]);
        }
    }

    @Test
    void theRingOutDecays() {
        float[] th = new float[N], thv = new float[N];
        float[] w = fill(11f), zeta = fill(0.22f), gain = fill(1f), mx = fill(0.5f);
        for (int f = 1; f <= 20; f++) SwingMath.step(th, thv, N, w, zeta, gain, mx, SwingMath.armAccel(f / 20f, 3.3f), 1f / 60f);
        float early = Math.abs(th[0]) + Math.abs(th[1]) + Math.abs(th[2]);
        for (int f = 0; f < 240; f++) SwingMath.step(th, thv, N, w, zeta, gain, mx, 0f, 1f / 60f);
        float late = Math.abs(th[0]) + Math.abs(th[1]) + Math.abs(th[2]);
        assertTrue(early > 0.01f, "there was something to ring out");
        assertTrue(late < early * 0.1f, "4 s later the chain is nearly still: " + early + " -> " + late);
    }

    @Test
    void aBadNumberNeverPoisonsTheChain() {
        float[] th = new float[N], thv = new float[N];
        SwingMath.step(th, thv, N, fill(11f), fill(0.2f), fill(1f), fill(0.2f), Float.NaN, 1f / 60f);
        for (int i = 0; i < N; i++) assertFalse(Float.isNaN(th[i]) || Float.isNaN(thv[i]), "bone " + i);
        SwingMath.step(th, thv, N, fill(11f), fill(0.2f), fill(1f), fill(0.2f), 100f, -1f);
        SwingMath.step(th, thv, N, fill(11f), fill(0.2f), fill(1f), fill(0.2f), 100f, 0f);
        assertEquals(0f, th[0], 0f, "a non-positive step does nothing");
    }

    @Test
    void aLongFrameIsCappedAndSubsteppedLightly() {
        assertEquals(1, SwingMath.substeps(0.001f));
        assertEquals(SwingMath.MAX_SUBSTEPS, SwingMath.substeps(10f));
        float[] a = new float[N], av = new float[N], b = new float[N], bv = new float[N];
        SwingMath.step(a, av, N, fill(11f), fill(0.2f), fill(1f), fill(1f), 50f, 10f);
        SwingMath.step(b, bv, N, fill(11f), fill(0.2f), fill(1f), fill(1f), 50f, SwingMath.MAX_STEP);
        assertEquals(b[0], a[0], 0f, "a ten second frame is simulated as MAX_STEP");
    }

    @Test
    void theIdleSwayIsGentleAndTravelsDownTheChain() {
        float peak = 0f;
        for (float t = 0f; t < 20f; t += 0.05f) peak = Math.max(peak, Math.abs(SwingMath.idle(t, 3, 1.2f, 1f)));
        assertTrue(peak <= 1f + 1e-5f && peak > 0.5f, "peak " + peak);
        assertTrue(Math.abs(SwingMath.idle(1f, 0, 0f, 0f)) <= 0.35f + 1e-5f, "standing still sways less than walking");
        assertFalse(SwingMath.idle(1f, 0, 0f, 1f) == SwingMath.idle(1f, 5, 0f, 1f), "bones are out of step");
    }

    @Test
    void farAwayWeaponsAreRigid() {
        assertTrue(SwingMath.near(23f * 23f));
        assertFalse(SwingMath.near(25f * 25f));
        assertEquals(24f, SwingMath.LOD_DISTANCE, 0f);
    }

    /** The values below come from {@code python3 tools/weaponmotion/swing_math.py} (float64); float32 stays within 2e-3 of them over 2 s. */
    @Test
    void theTrajectoryEqualsThePythonTwin() {
        float[][] r = strike(120, 6, 12, 18, 30, 60, 120);
        float[][] want = {
                {-0.157080f, -0.072166f, -0.011882f},
                {-0.066164f, -0.149559f, -0.116174f},
                {0.151800f, 0.047457f, -0.122176f},
                {-0.056980f, 0.004129f, 0.119528f},
                {0.003425f, 0.085737f, 0.141580f},
                {0.002055f, 0.009022f, -0.038821f}};
        for (int k = 0; k < want.length; k++) for (int i = 0; i < N; i++) assertEquals(want[k][i], r[k][i], 2e-3f, "golden " + k + " bone " + i);
    }
}
