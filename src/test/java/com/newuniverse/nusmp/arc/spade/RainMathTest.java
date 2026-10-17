package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The marks of Falling Heavens: the target first, the party next, the rest scattered, none stacked. */
class RainMathTest {
    @Test
    void theFirstMarkIsOnTheTargetAndTheNextOnTheOthers() {
        double[][] foes = {{10, 0}, {-6, 8}, {20, 20}};
        List<double[]> m = RainMath.plan(6, 0, 0, foes, 2.4, 10, 5);
        assertEquals(0.0, m.get(0)[0], 1e-9);
        assertEquals(0.0, m.get(0)[1], 1e-9);
        assertEquals(10.0, m.get(1)[0], 1e-9);
        assertEquals(-6.0, m.get(2)[0], 1e-9);
        assertEquals(20.0, m.get(3)[0], 1e-9);
        assertEquals(6, m.size());
    }

    @Test
    void noTwoMarksOverlapAcrossManySeeds() {
        double[][] foes = {{3, 1}, {-4, 2}, {1, -5}};
        for (long seed = 1; seed <= 300; seed++) {
            for (int n : new int[]{6, 8, 10, 12}) {
                List<double[]> m = RainMath.plan(n, 0, 0, foes, 2.4, 11, seed);
                assertTrue(m.size() <= n && m.size() >= Math.min(n, 4), "seed " + seed + " n " + n + " placed " + m.size());
                for (int i = 0; i < m.size(); i++)
                    for (int j = i + 1; j < m.size(); j++)
                        assertTrue(Math.hypot(m.get(i)[0] - m.get(j)[0], m.get(i)[1] - m.get(j)[1]) >= 1.4 * 2.4 - 1e-9, "marks apart");
            }
        }
    }

    @Test
    void aFoeStandingOnAMarkIsNotMarkedTwice() {
        double[][] foes = {{0.5, 0.5}, {9, 9}};
        List<double[]> m = RainMath.plan(3, 0, 0, foes, 2.4, 10, 1);
        assertEquals(0.0, m.get(0)[0], 1e-9);
        assertEquals(9.0, m.get(1)[0], 1e-9, "the foe on top of the first mark is skipped");
    }

    @Test
    void theNumberOfMarksIsCappedAndSeeded() {
        assertEquals(RainMath.MAX_MARKS, RainMath.plan(99, 0, 0, null, 1.0, 40, 3).size(), "asked for 99, laid the cap");
        assertTrue(RainMath.plan(0, 0, 0, null, 2.4, 10, 3).isEmpty());
        assertTrue(RainMath.plan(-4, 0, 0, null, 2.4, 10, 3).isEmpty());
        List<double[]> a = RainMath.plan(8, 1, 2, null, 2.4, 10, 42), b = RainMath.plan(8, 1, 2, null, 2.4, 10, 42);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i)[0], b.get(i)[0], 1e-12);
        assertEquals(6, RainMath.marksFor(1));
        assertEquals(8, RainMath.marksFor(2));
        assertEquals(10, RainMath.marksFor(3));
        assertEquals(12, RainMath.marksFor(4));
        assertEquals(12, RainMath.marksFor(40));
        assertEquals(6, RainMath.marksFor(0));
    }
}
