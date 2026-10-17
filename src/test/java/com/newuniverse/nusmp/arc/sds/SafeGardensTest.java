package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a field that strikes everyone outside a garden is only fair if a garden is always within reach of every foe, so that is tested over hundreds of random layouts, on floor only, with the gardens apart. */
class SafeGardensTest {
    private static final double CX = 0, CZ = 0, FIELD = 15, GR = 2.2, WALK = 4.5, GAP = 3.2;

    private static double[][] foes(Random r, int n, double radius) {
        double[][] f = new double[n][2];
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius;
            f[i] = new double[]{Math.cos(a) * d, Math.sin(a) * d};
        }
        return f;
    }

    @Test
    void everyFoeInTheFieldHasAGardenWithinAWalkAcrossManyLayouts() {
        Random layout = new Random(11);
        for (int trial = 0; trial < 300; trial++) {
            double[][] f = foes(layout, 1 + layout.nextInt(4), FIELD);
            List<SafeGardens.Garden> g = SafeGardens.plan(f, CX, CZ, FIELD, 2, GR, WALK, GAP, new Random(trial), (x, z) -> true);
            for (double[] foe : f) {
                if (Math.hypot(foe[0], foe[1]) > FIELD) continue;
                boolean reach = false;
                for (SafeGardens.Garden a : g) if (Math.hypot(a.x() - foe[0], a.z() - foe[1]) <= WALK + 1e-9) reach = true;
                assertTrue(reach, "foe " + foe[0] + "," + foe[1] + " (trial " + trial + ") has a garden in reach");
            }
        }
    }

    @Test
    void gardensStayInsideTheFieldApartFromEachOtherAndWithinTheCap() {
        Random layout = new Random(5);
        for (int trial = 0; trial < 200; trial++) {
            double[][] f = foes(layout, 4, FIELD);
            List<SafeGardens.Garden> g = SafeGardens.plan(f, CX, CZ, FIELD, 5, GR, WALK, GAP, new Random(trial * 7L), (x, z) -> true);
            assertTrue(g.size() <= SafeGardens.MAX);
            for (int i = 0; i < g.size(); i++) {
                assertTrue(Math.hypot(g.get(i).x() - CX, g.get(i).z() - CZ) <= FIELD - GR * 0.4 + GR + 1e-9, "inside the field (trial " + trial + ")");
                for (int j = i + 1; j < g.size(); j++)
                    assertTrue(Math.hypot(g.get(i).x() - g.get(j).x(), g.get(i).z() - g.get(j).z()) >= GAP - 1e-9, "apart (trial " + trial + ")");
            }
        }
    }

    @Test
    void theWantedCountIsMetWhenThereIsRoom() {
        for (int seed = 0; seed < 50; seed++) {
            List<SafeGardens.Garden> g = SafeGardens.plan(new double[][]{{1, 1}}, CX, CZ, FIELD, 5, GR, WALK, GAP, new Random(seed), (x, z) -> true);
            assertTrue(g.size() >= 5, "five wanted, " + g.size() + " made (seed " + seed + ")");
        }
    }

    @Test
    void aFoeOutsideTheFieldNeedsNoGarden() {
        List<SafeGardens.Garden> g = SafeGardens.plan(new double[][]{{40, 0}}, CX, CZ, FIELD, 0, GR, WALK, GAP, new Random(1), (x, z) -> true);
        assertTrue(g.isEmpty());
    }

    @Test
    void gardensAreOnlyPlacedWhereThereIsFloorAndAFoeStillGetsOneInReach() {
        for (int seed = 0; seed < 100; seed++) {
            double[][] f = {{-3, 0}, {6, 4}};
            List<SafeGardens.Garden> g = SafeGardens.plan(f, CX, CZ, FIELD, 3, GR, WALK, GAP, new Random(seed), (x, z) -> x >= 0);
            for (SafeGardens.Garden a : g) assertTrue(a.x() >= 0, "no garden over the pit (seed " + seed + ")");
            boolean reach = false;
            for (SafeGardens.Garden a : g) if (Math.hypot(a.x() + 3, a.z()) <= WALK + 1e-9) reach = true;
            assertTrue(reach, "the foe at the edge of the pit still has a garden in reach (seed " + seed + ")");
        }
    }

    @Test
    void theSamePlanComesFromTheSameSeed() {
        double[][] f = {{2, 3}, {-6, 4}};
        List<SafeGardens.Garden> a = SafeGardens.plan(f, CX, CZ, FIELD, 4, GR, WALK, GAP, new Random(9), (x, z) -> true);
        List<SafeGardens.Garden> b = SafeGardens.plan(f, CX, CZ, FIELD, 4, GR, WALK, GAP, new Random(9), (x, z) -> true);
        assertEquals(a, b);
    }

    @Test
    void safetyIsBeingInsideAGardenAndTheCountRuleIsBounded() {
        List<SafeGardens.Garden> g = List.of(new SafeGardens.Garden(0, 0), new SafeGardens.Garden(10, 0));
        assertTrue(SafeGardens.isSafe(g, 1.0, 1.0, GR, 0.6));
        assertTrue(SafeGardens.isSafe(g, 10, 2.4, GR, 0.6), "the body's edge may touch the garden");
        assertFalse(SafeGardens.isSafe(g, 5, 0, GR, 0.6));
        assertFalse(SafeGardens.isSafe(List.of(), 0, 0, GR, 0.6));
        assertEquals(3, SafeGardens.gardenCount(1, 2, 3));
        assertEquals(6, SafeGardens.gardenCount(4, 2, 3));
        assertEquals(SafeGardens.MAX, SafeGardens.gardenCount(50, 2, 3));
        assertEquals(2, SafeGardens.gardenCount(0, 0, 2));
    }
}
