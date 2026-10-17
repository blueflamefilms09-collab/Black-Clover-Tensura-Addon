package com.newuniverse.nusmp.avatar.menu;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The d20: a true icosahedron, a real d20's numbering, culling of the far side, and a roll that always lands with the wanted number in front. */
class DiceSpinTest {
    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    private static double dist(double[] a, double[] b) { double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2]; return Math.sqrt(x * x + y * y + z * z); }

    @Test
    void theSolidHasTwelveUnitVerticesAndTwentyFaces() {
        assertEquals(12, DiceSpin.vertexCount());
        assertEquals(20, DiceSpin.faces());
        for (int i = 0; i < 12; i++) {
            double[] v = DiceSpin.vertex(i);
            assertEquals(1.0, Math.sqrt(dot(v, v)), 1.0e-9);
        }
    }

    @Test
    void everyEdgeHasTheSameLengthAndBelongsToExactlyTwoFaces() {
        java.util.Map<Long, Integer> uses = new java.util.HashMap<>();
        double edge = -1;
        for (int f = 0; f < 20; f++) {
            int[] t = DiceSpin.face(f);
            for (int k = 0; k < 3; k++) {
                int a = t[k], b = t[(k + 1) % 3];
                double len = dist(DiceSpin.vertex(a), DiceSpin.vertex(b));
                if (edge < 0) edge = len;
                assertEquals(edge, len, 1.0e-9, "regular: all edges equal");
                uses.merge(((long) Math.min(a, b) << 8) | Math.max(a, b), 1, Integer::sum);
            }
        }
        assertEquals(30, uses.size(), "an icosahedron has 30 edges");
        for (int n : uses.values()) assertEquals(2, n, "closed surface: each edge in two faces");
    }

    @Test
    void everyFaceIsWoundSoItsNormalPointsOutward() {
        for (int f = 0; f < 20; f++) {
            int[] t = DiceSpin.face(f);
            double[] a = DiceSpin.vertex(t[0]), b = DiceSpin.vertex(t[1]), c = DiceSpin.vertex(t[2]);
            double[] centre = {(a[0] + b[0] + c[0]) / 3, (a[1] + b[1] + c[1]) / 3, (a[2] + b[2] + c[2]) / 3};
            double[] n = DiceSpin.normal(f);
            assertEquals(1.0, Math.sqrt(dot(n, n)), 1.0e-9);
            assertTrue(dot(n, centre) > 0.7, "outward, face " + f);
            double[] ab = {b[0] - a[0], b[1] - a[1], b[2] - a[2]}, ac = {c[0] - a[0], c[1] - a[1], c[2] - a[2]};
            double[] cr = {ab[1] * ac[2] - ab[2] * ac[1], ab[2] * ac[0] - ab[0] * ac[2], ab[0] * ac[1] - ab[1] * ac[0]};
            assertTrue(dot(cr, n) > 0, "counter-clockwise from outside, face " + f);
        }
    }

    @Test
    void theNumbersAreARealD20() {
        Set<Integer> seen = new HashSet<>();
        for (int f = 0; f < 20; f++) {
            int n = DiceSpin.numberOf(f);
            assertTrue(n >= 1 && n <= 20);
            assertTrue(seen.add(n), "each number once");
            assertEquals(f, DiceSpin.faceOf(n), "faceOf is the inverse");
            // find the opposite face
            int opp = -1;
            double best = 2;
            for (int g = 0; g < 20; g++) { double d = dot(DiceSpin.normal(f), DiceSpin.normal(g)); if (d < best) { best = d; opp = g; } }
            assertEquals(-1.0, best, 1.0e-9, "a true opposite face exists");
            assertEquals(21, n + DiceSpin.numberOf(opp), "opposite faces add up to 21");
        }
        assertEquals(20, seen.size());
        assertEquals(DiceSpin.faceOf(1), DiceSpin.faceOf(0), "out of range numbers clamp to the ends");
        assertEquals(DiceSpin.faceOf(20), DiceSpin.faceOf(99));
    }

    @Test
    void aRollAlwaysEndsWithTheWantedNumberInFront() {
        for (int seed = 0; seed < 6; seed++) {
            for (int want = 1; want <= 20; want++) {
                DiceSpin d = new DiceSpin(seed);
                d.roll(want, 40);
                for (int i = 0; i < 40; i++) d.advance(1f);
                assertFalse(d.rolling());
                assertEquals(want, d.value());
                for (float partial : new float[]{0f, 0.5f}) {
                    DiceSpin.View v = d.view(partial, 100f, 100f, 40f);
                    assertEquals(want, v.frontNumber(), "number " + want + " seed " + seed);
                    assertTrue(v.labelScale() > 0.9f, "head on when landed: " + v.labelScale());
                    assertEquals(100f, v.labelX(), 3f, "label in the middle");
                    assertEquals(100f, v.labelY(), 4f);
                }
            }
        }
    }

    @Test
    void afterLandingTheSwayNeverPushesTheNumberOffTheFront() {
        for (int want : new int[]{1, 7, 13, 20}) {
            DiceSpin d = new DiceSpin(5);
            d.roll(want, 30);
            for (int i = 0; i < 400; i++) {
                d.advance(1f);
                if (i >= 30) assertEquals(want, d.view(0.3f, 0f, 0f, 30f).frontNumber(), "tick " + i);
            }
        }
    }

    @Test
    void beforeAnyRollTheDieIdlesOnTwentyAndStaysThere() {
        DiceSpin d = new DiceSpin(1);
        assertFalse(d.rolling());
        for (int i = 0; i < 300; i++) {
            d.advance(1f);
            assertEquals(20, d.view(0f, 0f, 0f, 30f).frontNumber());
        }
    }

    @Test
    void whileRollingTheFrontFaceChangesButTheLastFrameIsExact() {
        DiceSpin d = new DiceSpin(3);
        d.roll(9, 50);
        Set<Integer> fronts = new HashSet<>();
        for (int i = 0; i < 49; i++) {
            d.advance(1f);
            fronts.add(d.view(0f, 0f, 0f, 30f).frontFace());
        }
        assertTrue(fronts.size() >= 5, "the die tumbles through several faces, saw " + fronts.size());
        d.advance(1f);
        assertEquals(9, d.view(0f, 0f, 0f, 30f).frontNumber());
        assertEquals(1.0, d.progress(0f), 1.0e-9);
    }

    @Test
    void onlyFrontFacesAreDrawnAndTheyAreBetweenSevenAndTenAtRest() {
        DiceSpin d = new DiceSpin(2);
        d.roll(12, 20);
        for (int i = 0; i < 20; i++) d.advance(1f);
        DiceSpin.View v = d.view(0f, 50f, 50f, 40f);
        assertTrue(v.tris().size() >= 7 && v.tris().size() <= 11, "an icosahedron shows 7 to 10 faces from one side, saw " + v.tris().size());
        for (int i = 1; i < v.tris().size(); i++) assertTrue(v.tris().get(i).face() != v.tris().get(i - 1).face());
        assertFalse(v.edges().isEmpty());
        assertTrue(v.edges().size() >= 12 && v.edges().size() <= 20);
    }

    @Test
    void triangleSizeFollowsTheRadiusAndStaysAroundTheCentre() {
        DiceSpin d = new DiceSpin(2);
        d.roll(5, 12);
        for (int i = 0; i < 12; i++) d.advance(1f);
        float cx = 200f, cy = 120f, r = 50f;
        DiceSpin.View v = d.view(0f, cx, cy, r);
        float minX = 1e9f, maxX = -1e9f, minY = 1e9f, maxY = -1e9f;
        for (DiceSpin.Tri t : v.tris()) {
            for (float[] p : new float[][]{{t.x0(), t.y0()}, {t.x1(), t.y1()}, {t.x2(), t.y2()}}) {
                minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]); minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
            }
        }
        assertTrue(maxX - minX < 2.5f * r && maxY - minY < 2.5f * r, "inside about a diameter and a quarter of the radius");
        assertTrue(maxX - minX > 1.2f * r, "and not tiny");
        assertEquals(cx, (minX + maxX) / 2, 0.35f * r);
        assertEquals(cy, (minY + maxY) / 2, 0.35f * r);
    }

    @Test
    void shadesAreInRangeAndFacesTowardTheLightAreBrighter() {
        DiceSpin d = new DiceSpin(2);
        d.roll(3, 12);
        for (int i = 0; i < 12; i++) d.advance(1f);
        DiceSpin.View v = d.view(0f, 0f, 0f, 40f);
        float lo = 2f, hi = -1f;
        for (DiceSpin.Tri t : v.tris()) {
            assertTrue(t.shade() >= 0.29f && t.shade() <= 1.0f, "shade " + t.shade());
            lo = Math.min(lo, t.shade());
            hi = Math.max(hi, t.shade());
        }
        assertTrue(hi - lo > 0.2f, "the facets must differ in brightness or the die looks flat: " + (hi - lo));
    }

    @Test
    void theSameSeedRollsTheSameDieAndTimeNeverRunsBackwards() {
        DiceSpin a = new DiceSpin(9), b = new DiceSpin(9);
        a.roll(14, 30);
        b.roll(14, 30);
        for (int i = 0; i < 10; i++) { a.advance(1f); b.advance(1f); }
        assertEquals(a.view(0f, 0f, 0f, 30f).frontFace(), b.view(0f, 0f, 0f, 30f).frontFace());
        DiceSpin.View before = a.view(0f, 0f, 0f, 30f);
        a.advance(-5f);
        DiceSpin.View after = a.view(0f, 0f, 0f, 30f);
        assertEquals(before.frontFace(), after.frontFace());
    }

    private static void assertSameView(DiceSpin.View a, DiceSpin.View b, String why) {
        assertEquals(a.frontFace(), b.frontFace(), why);
        assertEquals(a.tris().size(), b.tris().size(), why);
        for (int i = 0; i < a.tris().size(); i++) {
            DiceSpin.Tri p = a.tris().get(i), q = b.tris().get(i);
            assertEquals(p.face(), q.face(), why);
            assertEquals(p.x0(), q.x0(), 1.0e-2f, why);
            assertEquals(p.y0(), q.y0(), 1.0e-2f, why);
            assertEquals(p.x1(), q.x1(), 1.0e-2f, why);
            assertEquals(p.y2(), q.y2(), 1.0e-2f, why);
        }
    }

    @Test
    void tumblingKeepsTheDieSpinningThroughManyFaces() {
        DiceSpin d = new DiceSpin(4);
        d.tumble();
        assertTrue(d.rolling());
        assertTrue(d.tumbling());
        assertEquals(0.0, d.progress(0f), 0.0);
        Set<Integer> fronts = new HashSet<>();
        for (int i = 0; i < 60; i++) {
            d.advance(1f);
            fronts.add(d.view(0f, 0f, 0f, 30f).frontFace());
        }
        assertTrue(fronts.size() >= 8, "a free spin shows many faces, saw " + fronts.size());
        assertTrue(d.tumbling(), "it never stops by itself: the number has to come from the server");
    }

    @Test
    void aRollAfterATumbleStartsExactlyWhereTheDieWasAndLandsOnTheNumber() {
        for (int seed = 0; seed < 8; seed++) {
            for (int want : new int[]{1, 11, 17, 20}) {
                DiceSpin d = new DiceSpin(seed);
                d.tumble();
                for (int i = 0; i < 7; i++) d.advance(1f);
                d.advance(0.4f);
                DiceSpin.View before = d.view(0f, 100f, 100f, 40f);
                d.roll(want, 36);
                assertFalse(d.tumbling());
                assertTrue(d.rolling());
                assertSameView(before, d.view(0f, 100f, 100f, 40f), "no jump at the start of the roll, seed " + seed);
                for (int i = 0; i < 36; i++) d.advance(1f);
                assertFalse(d.rolling());
                assertEquals(want, d.view(0f, 100f, 100f, 40f).frontNumber(), "lands on " + want);
            }
        }
    }

    @Test
    void aRollStartedInTheMiddleOfAnotherRollDoesNotJumpEither() {
        DiceSpin d = new DiceSpin(6);
        d.roll(5, 40);
        for (int i = 0; i < 13; i++) d.advance(1f);
        DiceSpin.View before = d.view(0f, 0f, 0f, 30f);
        d.roll(9, 40);
        assertSameView(before, d.view(0f, 0f, 0f, 30f), "restart in flight");
        for (int i = 0; i < 40; i++) d.advance(1f);
        assertEquals(9, d.view(0f, 0f, 0f, 30f).frontNumber());
        assertEquals(9, d.value());
    }

    @Test
    void aRollAskedForTheMomentAfterATumbleBeganStillLands() {
        DiceSpin d = new DiceSpin(2);
        d.tumble();
        d.roll(13, 30);
        for (int i = 0; i < 30; i++) d.advance(1f);
        assertEquals(13, d.view(0f, 0f, 0f, 30f).frontNumber());
        d.tumble();
        d.roll(3, 30);
        for (int i = 0; i < 30; i++) d.advance(1f);
        assertEquals(3, d.view(0f, 0f, 0f, 30f).frontNumber());
    }

    @Test
    void thePopHappensOnlyJustAfterLanding() {
        DiceSpin d = new DiceSpin(1);
        assertEquals(1f, d.pop(0f), 0f);
        d.roll(5, 20);
        for (int i = 0; i < 10; i++) d.advance(1f);
        assertEquals(1f, d.pop(0f), 0f, "still rolling");
        for (int i = 0; i < 10; i++) d.advance(1f);
        d.advance(5f);
        assertTrue(d.pop(0f) > 1.05f, "popping");
        d.advance(20f);
        assertEquals(1f, d.pop(0f), 0f);
    }
}
