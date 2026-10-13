package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** What the Monarch's shapes hit and miss. */
class MonarchShapesTest {
    @Test
    void aRingHasEvenlySpacedPointsAtItsRadius() {
        double[][] pts = MonarchShapes.ring(10, -4, 6, 12, 0.3);
        assertEquals(12, pts.length);
        for (double[] p : pts) assertEquals(6.0, Math.hypot(p[0] - 10, p[1] + 4), 1e-9);
        assertEquals(10 + 6 * Math.cos(0.3), pts[0][0], 1e-9);
        double chord = Math.hypot(pts[1][0] - pts[0][0], pts[1][1] - pts[0][1]);
        for (int i = 1; i < pts.length; i++) assertEquals(chord, Math.hypot(pts[(i + 1) % 12][0] - pts[i][0], pts[(i + 1) % 12][1] - pts[i][1]), 1e-9);
    }

    @Test
    void aSpreadingFrontNeverSkipsABody() {
        double speed = MonarchRules.SLAM_SPEED, band = MonarchRules.SLAM_BAND;
        for (double dist = 0; dist <= 18; dist += 0.37) {
            int hits = 0;
            double prev = 0;
            for (int t = 1; t < 80; t++) {
                double now = MonarchShapes.front(t, speed);
                if (MonarchShapes.passes(dist, prev, now, band)) hits++;
                prev = now;
            }
            assertTrue(hits >= 1, "a body at " + dist + " is passed over at least once");
            assertTrue(hits <= 4, "and only in the few ticks the band takes to cross it: " + hits);
        }
        assertFalse(MonarchShapes.passes(30, 0, 10, 1));
        assertFalse(MonarchShapes.passes(2, 20, 21, 1));
        assertEquals(0.0, MonarchShapes.front(-3, 0.75));
        assertEquals(7.5, MonarchShapes.front(10, 0.75), 1e-9);
    }

    @Test
    void aJumpClearsTheRingButStandingDoesNot() {
        assertTrue(MonarchShapes.ringHurts(true, 65.0, 65.0, 4.5), "standing on the floor of the ring");
        assertFalse(MonarchShapes.ringHurts(false, 66.0, 65.0, 4.5), "in the air: over it");
        assertFalse(MonarchShapes.ringHurts(true, 60.0, 65.0, 4.5), "on a lower level of the hall");
        assertFalse(MonarchShapes.ringHurts(true, 71.0, 65.0, 4.5), "on a higher one");
        assertTrue(MonarchShapes.ringHurts(true, 62.0, 65.0, 4.5), "on the steps of the dais below it");
    }

    @Test
    void aLineHitsWhatLiesOnItAndMissesWhatDoesNot() {
        assertEquals(3.0, MonarchShapes.distToSegment(5, 3, 0, 0, 10, 0), 1e-9);
        assertEquals(5.0, MonarchShapes.distToSegment(-3, 4, 0, 0, 10, 0), 1e-9, "beyond the start the distance is to the start");
        assertEquals(5.0, MonarchShapes.distToSegment(13, 4, 0, 0, 10, 0), 1e-9);
        assertEquals(2.0, MonarchShapes.distToSegment(1, 1, 0, 0, 0, 0) - Math.sqrt(2) + 2.0, 1e-9, "a segment of no length is a point");
        double[] end = MonarchShapes.end(0, 0, 0.0, 44);
        assertEquals(44.0, end[0], 1e-9);
        assertTrue(MonarchShapes.beamHits(20, 64, 0.5, 0, 64, 0, end[0], end[1], MonarchRules.BEAM_HALF, 2.5));
        assertFalse(MonarchShapes.beamHits(20, 64, 3.0, 0, 64, 0, end[0], end[1], MonarchRules.BEAM_HALF, 2.5), "two blocks to the side is safe");
        assertFalse(MonarchShapes.beamHits(20, 70, 0.0, 0, 64, 0, end[0], end[1], MonarchRules.BEAM_HALF, 2.5), "high above is safe");
        assertFalse(MonarchShapes.beamHits(50, 64, 0.0, 0, 64, 0, end[0], end[1], MonarchRules.BEAM_HALF, 2.5), "past its end is safe");
        assertEquals(Math.PI / 2, MonarchShapes.angle(0, 0, 0, 5), 1e-9);
    }

    @Test
    void aFanSpreadsTheLinesEvenlyAboutTheAim() {
        assertEquals(List.of(1.0), MonarchShapes.fan(1.0, 1, 0.5));
        List<Double> two = MonarchShapes.fan(1.0, 2, 0.5);
        assertEquals(0.75, two.get(0), 1e-9);
        assertEquals(1.25, two.get(1), 1e-9);
        List<Double> three = MonarchShapes.fan(1.0, 3, 0.5);
        assertEquals(0.5, three.get(0), 1e-9);
        assertEquals(1.0, three.get(1), 1e-9);
        assertEquals(1.5, three.get(2), 1e-9);
        // a fan of three leaves room to stand between its lines at a distance
        double a = three.get(1), b = three.get(2);
        double[] e1 = MonarchShapes.end(0, 0, a, 30), e2 = MonarchShapes.end(0, 0, b, 30);
        assertTrue(Math.hypot(e1[0] - e2[0], e1[1] - e2[1]) > 2 * MonarchRules.BEAM_HALF + 2.0);
    }

    @Test
    void theRainFallsUnderTheFoesAndOverTheFloorOnly() {
        List<double[]> foes = List.of(new double[]{5, 5}, new double[]{-8, 3}, new double[]{12, -9});
        for (int seed = 0; seed < 100; seed++) {
            for (int ph = 1; ph <= 3; ph++) {
                int count = MonarchRules.rainCircles(ph, foes.size());
                List<double[]> c = MonarchShapes.rain(seed, foes, count, 0, 0, 38, MonarchRules.RAIN_GAP);
                assertEquals(c.size(), MonarchShapes.rain(seed, foes, count, 0, 0, 38, MonarchRules.RAIN_GAP).size(), "deterministic");
                assertTrue(c.size() >= 3 && c.size() <= count, "got " + c.size() + " of " + count);
                for (double[] f : foes) assertTrue(c.stream().anyMatch(p -> p[0] == f[0] && p[1] == f[1]), "one circle under each foe");
                for (int i = 0; i < c.size(); i++) {
                    assertTrue(Math.hypot(c.get(i)[0], c.get(i)[1]) <= 38.0 + 1e-9, "inside the arena");
                    for (int j = i + 1; j < c.size(); j++) assertTrue(Math.hypot(c.get(i)[0] - c.get(j)[0], c.get(i)[1] - c.get(j)[1]) >= MonarchRules.RAIN_GAP - 1e-9, "no two overlap");
                }
            }
        }
        List<double[]> none = MonarchShapes.rain(1, new ArrayList<>(), 6, 0, 0, 30, 3.0);
        assertEquals(6, none.size(), "with no foes it is scattered");
        assertTrue(MonarchShapes.rain(2, List.of(new double[]{200, 200}), 4, 0, 0, 30, 3.0).stream().noneMatch(p -> p[0] > 100), "a foe outside the arena gets no circle");
        assertTrue(MonarchShapes.inside(1, 1, 0, 0, 2) && !MonarchShapes.inside(2, 2, 0, 0, 2));
    }
    // ---------------------------------------------------------------- the fast arts
    @Test
    void anAngleIsBroughtIntoRangeAndHeadingsAreComparedTheShortWay() {
        assertEquals(0.0, MonarchShapes.wrap(0.0), 1e-12);
        assertEquals(Math.PI, MonarchShapes.wrap(Math.PI), 1e-12);
        assertEquals(Math.PI, MonarchShapes.wrap(-Math.PI), 1e-12, "the range is (-pi, pi]");
        assertEquals(-Math.PI / 2, MonarchShapes.wrap(3 * Math.PI / 2), 1e-12);
        assertEquals(0.5, MonarchShapes.wrap(0.5 + 6 * Math.PI), 1e-9);
        assertEquals(-0.5, MonarchShapes.wrap(-0.5 - 8 * Math.PI), 1e-9);
        assertEquals(0.2, MonarchShapes.angleBetween(Math.PI - 0.1, -Math.PI + 0.1), 1e-12, "across the cut at pi");
        assertEquals(Math.PI, MonarchShapes.angleBetween(0, Math.PI), 1e-12);
    }

    @Test
    void aCrossIsTwoLinesAtARightAngleCentredOnTheTarget() {
        double[][] ls = MonarchShapes.crossLines(10, -4, 0.3, 13);
        assertEquals(2, ls.length);
        for (double[] l : ls) {
            assertEquals(26.0, Math.hypot(l[2] - l[0], l[3] - l[1]), 1e-9, "26 blocks long");
            assertEquals(10.0, (l[0] + l[2]) / 2, 1e-9, "centred on the target, not on the boss");
            assertEquals(-4.0, (l[1] + l[3]) / 2, 1e-9);
        }
        double a0 = Math.atan2(ls[0][3] - ls[0][1], ls[0][2] - ls[0][0]), a1 = Math.atan2(ls[1][3] - ls[1][1], ls[1][2] - ls[1][0]);
        assertEquals(Math.PI / 2, MonarchShapes.angleBetween(a0, a1), 1e-9, "at a right angle");
        assertTrue(MonarchShapes.crossHits(10, -4, 10, -4, 0.3, 13, 1.5), "standing in the middle is the worst place");
        assertTrue(MonarchShapes.crossHits(10 + 12 * Math.cos(0.3), -4 + 12 * Math.sin(0.3), 10, -4, 0.3, 13, 1.5), "near the end of a line");
        assertTrue(MonarchShapes.crossHits(10 + 14 * Math.cos(0.3), -4 + 14 * Math.sin(0.3), 10, -4, 0.3, 13, 1.5), "a body touching the rounded end of a line is cut");
        assertFalse(MonarchShapes.crossHits(10 + 15 * Math.cos(0.3), -4 + 15 * Math.sin(0.3), 10, -4, 0.3, 13, 1.5), "past the end of a line");
    }

    @Test
    void thereIsAlwaysAWedgeToStepIntoAndTheNextCrossTurnedFortyFiveDegreesTakesThemAll() {
        double w = MonarchRules.CROSS_HALF_WIDTH + 0.3, len = MonarchRules.CROSS_HALF_LEN;
        for (double base = 0; base < Math.PI; base += 0.1) {
            // the safe spot nearest to the middle of one cross lies on a bisector, w / sin(45 degrees) away
            double reach = w / Math.sin(Math.PI / 4) + 0.05;
            double bx = reach * Math.cos(base + Math.PI / 4), bz = reach * Math.sin(base + Math.PI / 4);
            assertFalse(MonarchShapes.crossHits(bx, bz, 0, 0, base, len, w), "a wedge " + reach + " blocks from the middle is safe, base " + base);
            assertTrue(reach < 2.4, "and it is two steps away, not ten: " + reach);
            // the same spot is cut by the second cross, turned 45 degrees: it is exactly on one of its lines
            assertTrue(MonarchShapes.crossHits(bx, bz, 0, 0, base + MonarchRules.CROSS_TURN, len, w), "the turned cross takes the old safe spot");
        }
        // both crosses together leave the wedges of the 8-pointed star, which start further out: 22.5 degrees off every line
        double far = w / Math.sin(Math.PI / 8) + 0.05;
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 8 + k * Math.PI / 4;
            double x = far * Math.cos(a), z = far * Math.sin(a);
            assertFalse(MonarchShapes.crossHits(x, z, 0, 0, 0.0, len, w) || MonarchShapes.crossHits(x, z, 0, 0, MonarchRules.CROSS_TURN, len, w), "wedge " + k);
        }
    }

    @Test
    void aStripIsAnEdgeOnEachSideAndAMiddleAndNeverTooMany() {
        List<double[]> pts = MonarchShapes.strip(0, 0, 40, 0, 1.5, 1.8, 16);
        assertEquals(48, pts.size(), "three rows of 16");
        for (int i = 0; i < pts.size(); i++) {
            double z = pts.get(i)[1];
            assertEquals(i < 16 ? -1.5 : i < 32 ? 1.5 : 0.0, z, 1e-9, "left edge, right edge, middle");
            assertTrue(pts.get(i)[0] >= -1e-9 && pts.get(i)[0] <= 40 + 1e-9);
        }
        assertEquals(0.0, pts.get(0)[0], 1e-9, "from the start");
        assertEquals(40.0, pts.get(15)[0], 1e-9, "to the end");
        assertEquals(3 * 2, MonarchShapes.strip(0, 0, 3, 0, 1.0, 1.8, 16).size(), "a short strip has few points: its two ends, in three rows");
        assertEquals(48, MonarchShapes.strip(0, 0, 500, 0, 1.0, 1.8, 16).size(), "a very long one is capped per row");
        assertTrue(MonarchShapes.strip(5, 5, 5, 5, 1.0, 1.8, 16).isEmpty(), "a strip of no length draws nothing");
        // turned: the edges are still perpendicular to the strip
        List<double[]> diag = MonarchShapes.strip(0, 0, 10, 10, 2.0, 1.8, 16);
        int perRow = diag.size() / 3;
        assertEquals(4.0, Math.hypot(diag.get(0)[0] - diag.get(perRow)[0], diag.get(0)[1] - diag.get(perRow)[1]), 1e-9, "the two edges are a full width apart");
        // the cross at its largest is bounded: two lines, three rows of 16
        assertTrue(2 * MonarchShapes.strip(0, 0, 26, 0, MonarchRules.CROSS_HALF_WIDTH, 1.8, 16).size() <= 96);
    }

    @Test
    void aFanIsTwoHundredDegreesOfARoundAndNothingBehind() {
        double h = 0.7, half = MonarchRules.SWEEP_HALF_ANGLE, r = 11;
        assertTrue(MonarchShapes.inFan(Math.cos(h) * 5, Math.sin(h) * 5, 0, 0, h, half, r, 0.3), "straight ahead");
        assertTrue(MonarchShapes.inFan(Math.cos(h + Math.toRadians(95)) * 8, Math.sin(h + Math.toRadians(95)) * 8, 0, 0, h, half, r, 0.3), "to the side, past a right angle");
        assertTrue(MonarchShapes.inFan(Math.cos(h - Math.toRadians(99)) * 8, Math.sin(h - Math.toRadians(99)) * 8, 0, 0, h, half, r, 0.0), "99 degrees is in");
        assertFalse(MonarchShapes.inFan(Math.cos(h + Math.toRadians(103)) * 8, Math.sin(h + Math.toRadians(103)) * 8, 0, 0, h, half, r, 0.0), "103 degrees is out");
        assertFalse(MonarchShapes.inFan(Math.cos(h + Math.PI) * 5, Math.sin(h + Math.PI) * 5, 0, 0, h, half, r, 0.3), "behind it is safe");
        assertFalse(MonarchShapes.inFan(Math.cos(h + Math.toRadians(150)) * 4, Math.sin(h + Math.toRadians(150)) * 4, 0, 0, h, half, r, 0.3), "the rear 160 degrees are safe");
        assertFalse(MonarchShapes.inFan(Math.cos(h) * 12, Math.sin(h) * 12, 0, 0, h, half, r, 0.3), "outside the radius is safe");
        assertTrue(MonarchShapes.inFan(Math.cos(h) * 11.2, Math.sin(h) * 11.2, 0, 0, h, half, r, 0.3), "a body counts by its edge");
        assertTrue(MonarchShapes.inFan(0.1, 0.1, 0, 0, h, half, r, 0.3), "right at the Commander's feet whichever way it lies");
        // the area really is a bit more than a half circle: 200 / 360
        Random rnd = new Random(5);
        int in = 0, n = 40000;
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
            if (MonarchShapes.inFan(Math.cos(a) * d, Math.sin(a) * d, 0, 0, h, half, r, 0.0)) in++;
        }
        assertEquals(200.0 / 360.0, (double) in / n, 0.01);
    }

    @Test
    void theBladeStartsAtOneEdgeAndEndsAtTheOtherAndReversesBack() {
        double half = MonarchRules.SWEEP_HALF_ANGLE;
        int total = MonarchRules.SWEEP_CUT;
        assertEquals(-half, MonarchShapes.bladeAt(-1, total, half, false), 1e-12, "before the first tick it is at the first edge");
        assertEquals(half, MonarchShapes.bladeAt(total - 1, total, half, false), 1e-12, "after the last it is at the far edge");
        assertEquals(half, MonarchShapes.bladeAt(-1, total, half, true), 1e-12, "the reverse starts where the first ended");
        assertEquals(-half, MonarchShapes.bladeAt(total - 1, total, half, true), 1e-12);
        double prev = -half;
        for (int i = 0; i < total; i++) {
            double a = MonarchShapes.bladeAt(i, total, half, false);
            assertTrue(a > prev, "it only ever moves on");
            prev = a;
        }
        assertEquals(half, MonarchShapes.bladeAt(99, total, half, false), 1e-12, "past the end it stays at the far edge");
        assertEquals(0.0, MonarchShapes.bladeAt(0, 1, 0.0, false), 1e-12);
    }

    @Test
    void aCutPassesOverEveryBodyInTheFanExactlyWhereverItIsAndNoBodyOutside() {
        Random rnd = new Random(77);
        double half = MonarchRules.SWEEP_HALF_ANGLE;
        for (int phase = 1; phase <= 3; phase++) {
            double radius = MonarchRules.sweepRadius(phase), heading = rnd.nextDouble() * 6 - 3;
            for (int i = 0; i < 4000; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * (radius + 3), margin = 0.3;
                double px = 3 + Math.cos(a) * d, pz = -2 + Math.sin(a) * d;
                boolean inFan = MonarchShapes.inFan(px, pz, 3, -2, heading, half, radius, margin);
                for (boolean reverse : new boolean[]{false, true}) {
                    int passes = 0;
                    for (int t = 0; t < MonarchRules.SWEEP_CUT; t++) {
                        double a0 = MonarchShapes.bladeAt(t - 1, MonarchRules.SWEEP_CUT, half, reverse), a1 = MonarchShapes.bladeAt(t, MonarchRules.SWEEP_CUT, half, reverse);
                        if (MonarchShapes.inSlice(px, pz, 3, -2, heading, a0, a1, radius, margin)) passes++;
                    }
                    if (inFan) assertTrue(passes >= 1, "a body in the fan is passed over (phase " + phase + ", reverse " + reverse + ")");
                    else assertEquals(0, passes, "a body outside the fan is never touched");
                    if (d >= 3.0) assertTrue(passes <= 2, "and only by neighbouring slices: " + passes);       // closer than that a body is in every slice, and the cut strikes it once
                }
            }
        }
    }

    @Test
    void anArcHasItsPointsOnItsRadiusAndIsBounded() {
        List<double[]> arc = MonarchShapes.arc(5, 5, 1.0, -MonarchRules.SWEEP_HALF_ANGLE, MonarchRules.SWEEP_HALF_ANGLE, 13, 1.4, 40);
        assertTrue(arc.size() >= 2 && arc.size() <= 40, "bounded: " + arc.size());
        for (double[] p : arc) assertEquals(13.0, Math.hypot(p[0] - 5, p[1] - 5), 1e-9);
        double[] first = arc.get(0), last = arc.get(arc.size() - 1);
        assertEquals(1.0 - MonarchRules.SWEEP_HALF_ANGLE, Math.atan2(first[1] - 5, first[0] - 5), 1e-9);
        assertEquals(1.0 + MonarchRules.SWEEP_HALF_ANGLE, Math.atan2(last[1] - 5, last[0] - 5), 1e-9);
        assertEquals(2, MonarchShapes.arc(0, 0, 0, 0, 0.0, 5, 1.0, 40).size(), "no arc is fewer than two points");
        assertEquals(10, MonarchShapes.arc(0, 0, 0, 0, 6.0, 50, 1.0, 10).size(), "and a huge one is cut to the cap");
        // the whole mark of the arc is bounded: rim, inner arc and two edges stay under ninety packets
        int rim = MonarchShapes.arc(0, 0, 0, -MonarchRules.SWEEP_HALF_ANGLE, MonarchRules.SWEEP_HALF_ANGLE, 13, 1.4, 40).size();
        int inner = MonarchShapes.arc(0, 0, 0, -MonarchRules.SWEEP_HALF_ANGLE, MonarchRules.SWEEP_HALF_ANGLE, 13 * 0.55, 1.8, 24).size();
        assertTrue(rim + inner + 2 * 8 <= 90, "dots per mark: " + (rim + inner + 16));
    }
}
