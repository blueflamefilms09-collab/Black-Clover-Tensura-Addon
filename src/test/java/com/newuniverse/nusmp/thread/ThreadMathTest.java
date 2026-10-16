package com.newuniverse.nusmp.thread;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Trap geometry and caps, the smooth pull, the skin's upkeep and the bind clamp. */
class ThreadMathTest {
    private static void same(double[] want, double[] got) { same(want, got, "vector"); }

    private static void same(double[] want, double[] got, String what) {
        assertEquals(want.length, got.length, what);
        for (int i = 0; i < want.length; i++) assertEquals(want[i], got[i], 1e-9, what + "[" + i + "]");
    }

    @Test
    void aTrapLineRunsAlongTheDirectionAtTheSpacing() {
        List<double[]> line = ThreadMath.line(10, 20, 3, 0, 4, 1.5, 2.0);
        assertEquals(4, line.size());
        same(new double[]{12.0, 20.0}, line.get(0));
        same(new double[]{13.5, 20.0}, line.get(1));
        same(new double[]{16.5, 20.0}, line.get(3));
        List<double[]> diag = ThreadMath.line(0, 0, 1, 1, 2, 1.0, 0.0);
        assertEquals(Math.sqrt(0.5), diag.get(1)[0], 1e-9, "the direction is normalised");
        assertTrue(ThreadMath.line(0, 0, 0, 0, 5, 1, 0).isEmpty(), "no direction, no line");
        assertTrue(ThreadMath.line(0, 0, 1, 0, 0, 1, 0).isEmpty());
    }

    @Test
    void aTrapRingIsEvenlySpacedAtTheRadius() {
        List<double[]> ring = ThreadMath.ring(5, -5, 3, 8, 0);
        assertEquals(8, ring.size());
        for (double[] p : ring) assertEquals(3.0, Math.hypot(p[0] - 5, p[1] + 5), 1e-9);
        double chord = Math.hypot(ring.get(0)[0] - ring.get(1)[0], ring.get(0)[1] - ring.get(1)[1]);
        double chord2 = Math.hypot(ring.get(3)[0] - ring.get(4)[0], ring.get(3)[1] - ring.get(4)[1]);
        assertEquals(chord, chord2, 1e-9);
        assertTrue(ThreadMath.ring(0, 0, 3, 0, 0).isEmpty());
        assertEquals(1.5, ThreadMath.ringRadius(1, 2.0), 1e-9);
        assertEquals(1.5, ThreadMath.ringRadius(2, 0.5), 1e-9, "never tighter than 1.5");
        assertEquals(8 * 2.0 / (Math.PI * 2), ThreadMath.ringRadius(8, 2.0), 1e-9);
    }

    @Test
    void trapsExpireAfterTheirLife() {
        assertFalse(ThreadMath.trapExpired(1799, 1800));
        assertTrue(ThreadMath.trapExpired(1800, 1800));
    }

    @Test
    void aPullIsSmoothCappedAndStopsAtTheDestination() {
        double[] pos = {0, 0, 0}, dest = {8, 0, 0};
        double[] v = ThreadMath.pullVelocity(pos, dest, 8, 5.0);
        assertEquals(1.0, v[0], 1e-9, "8 blocks in 8 ticks");
        assertEquals(0.0, v[1], 1e-9);
        double[] capped = ThreadMath.pullVelocity(pos, new double[]{80, 0, 0}, 8, 1.2);
        assertEquals(1.2, capped[0], 1e-9, "never faster than the cap");
        double[] up = ThreadMath.pullVelocity(pos, new double[]{0, 4, 3}, 5, 9.0);
        assertEquals(1.0, Math.sqrt(up[0] * up[0] + up[1] * up[1] + up[2] * up[2]), 1e-9, "5 blocks away in 5 ticks");
        assertEquals(0.8, up[1], 1e-9);
        double[] there = ThreadMath.pullVelocity(new double[]{7.9, 0, 0}, dest, 3, 1.2);
        same(new double[]{0, 0, 0}, there, "arrived");
        same(new double[]{0, 0, 0}, ThreadMath.pullVelocity(pos, dest, 0, 1.2), "out of time");
    }

    @Test
    void simulatingThePullLandsOnTheDestination() {
        double[] pos = {0, 0, 0}, dest = {6, 1, -3};
        int steps = 8;
        for (int age = 0; age < steps; age++) {
            double[] v = ThreadMath.pullVelocity(pos, dest, steps - age, 1.2);
            for (int k = 0; k < 3; k++) pos[k] += v[k];
        }
        assertEquals(0.0, Math.sqrt(Math.pow(pos[0] - dest[0], 2) + Math.pow(pos[1] - dest[1], 2) + Math.pow(pos[2] - dest[2], 2)), 0.3);
    }

    @Test
    void theSkinsUpkeepIsAPercentOfMaxAndStopsWhenEmpty() {
        assertEquals(15.0, ThreadMath.reinforceDrain(1000, 1.5), 1e-9);
        assertEquals(0.0, ThreadMath.reinforceDrain(-5, 1.5), 1e-9);
        assertEquals(0.0, ThreadMath.reinforceDrain(1000, -2), 1e-9);
        assertTrue(ThreadMath.canKeepSkin(15, 15));
        assertFalse(ThreadMath.canKeepSkin(14.9, 15));
        assertFalse(ThreadMath.canKeepSkin(0, 0), "an empty pool ends it even if the drain is zero");
    }

    @Test
    void theBindNeverOutlastsTheBalanceClamp() {
        assertEquals(80, ThreadMath.bindTicks(80, 100));
        assertEquals(20, ThreadMath.bindTicks(80, 20), "a boss gets one second");
        assertEquals(1, ThreadMath.bindTicks(80, 0));
    }
}
