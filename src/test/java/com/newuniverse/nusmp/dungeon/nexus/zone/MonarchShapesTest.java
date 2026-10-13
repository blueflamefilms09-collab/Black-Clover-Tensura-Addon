package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
}
