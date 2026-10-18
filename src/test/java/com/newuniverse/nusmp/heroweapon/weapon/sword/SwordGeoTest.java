package com.newuniverse.nusmp.heroweapon.weapon.sword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: the Sword's techniques are shapes on the ground, and a shape that is a little wrong either misses what it should hit or hits what it should not. The
 * geometry, the mark table and the per-target cap are pure, so their edges are checked here: the directions of a fan, the points of a ring and of a scatter, the
 * strip of a fissure, the back half of a foe, the length of a dash, the yaw of a player who looks along a vector, and the bounded tables.
 */
class SwordGeoTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes());

    @Test
    void aFanSpreadsEvenlyAndKeepsUnitLength() {
        double[][] fan = SwordGeo.fan(0, 1, 3, 12);
        assertEquals(3, fan.length);
        for (double[] d : fan) assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1]), 1e-9);
        assertEquals(0.0, fan[1][0], 1e-9, "the middle one is straight ahead");
        assertEquals(1.0, fan[1][1], 1e-9);
        assertEquals(-fan[0][0], fan[2][0], 1e-9, "the outer two are mirror images");
        assertEquals(fan[0][1], fan[2][1], 1e-9);
        double angle = Math.toDegrees(Math.acos(fan[2][1]));
        assertEquals(12.0, angle, 1e-6);
        assertEquals(1, SwordGeo.fan(0, 1, 1, 12).length);
        assertEquals(1, SwordGeo.fan(0, 1, 0, 12).length, "a count under one still gives one");
    }

    @Test
    void aZeroDirectionFacesSouth() {
        double[] u = SwordGeo.unit(0, 0);
        assertEquals(0.0, u[0], 1e-9);
        assertEquals(1.0, u[1], 1e-9);
        double[] t = SwordGeo.turn(0, 0, 90);
        assertEquals(-1.0, t[0], 1e-9);
        assertEquals(0.0, t[1], 1e-9);
    }

    @Test
    void aRingHasEvenlySpacedPointsAtTheRadius() {
        double[][] ring = SwordGeo.ring(10, -5, 2.0, 4, 0);
        assertEquals(4, ring.length);
        for (double[] p : ring) assertEquals(2.0, Math.hypot(p[0] - 10, p[1] + 5), 1e-9);
        assertEquals(12.0, ring[0][0], 1e-9);
        assertEquals(-5.0, ring[0][1], 1e-9);
        assertEquals(10.0, ring[1][0], 1e-9);
        assertEquals(-3.0, ring[1][1], 1e-9);
        assertEquals(1, SwordGeo.ring(0, 0, 1, 0, 0).length);
    }

    @Test
    void aScatterStaysInsideTheDiscAndIsRepeatable() {
        double[][] a = SwordGeo.scatter(100, 200, 5.0, 40, 12345L), b = SwordGeo.scatter(100, 200, 5.0, 40, 12345L), c = SwordGeo.scatter(100, 200, 5.0, 40, 999L);
        assertEquals(40, a.length);
        boolean differs = false;
        for (int i = 0; i < a.length; i++) {
            assertTrue(Math.hypot(a[i][0] - 100, a[i][1] - 200) <= 5.0 + 1e-9, "inside the disc");
            assertEquals(a[i][0], b[i][0], 0.0);
            assertEquals(a[i][1], b[i][1], 0.0);
            if (a[i][0] != c[i][0]) differs = true;
        }
        assertTrue(differs, "another seed gives another rain");
        assertEquals(0, SwordGeo.scatter(0, 0, 5, 0, 1).length);
        assertEquals(0, SwordGeo.scatter(0, 0, 5, -3, 1).length);
    }

    @Test
    void theStripCoversTheLengthAndHalfWidthOnly() {
        assertTrue(SwordGeo.onStrip(0, 0, 0, 1, 10, 1.2, 0.0, 5.0));
        assertTrue(SwordGeo.onStrip(0, 0, 0, 1, 10, 1.2, 1.1, 10.0), "at the far end, near the edge");
        assertFalse(SwordGeo.onStrip(0, 0, 0, 1, 10, 1.2, 1.3, 5.0), "too far to the side");
        assertFalse(SwordGeo.onStrip(0, 0, 0, 1, 10, 1.2, 0.0, 10.1), "past the end");
        assertFalse(SwordGeo.onStrip(0, 0, 0, 1, 10, 1.2, 0.0, -0.1), "behind the start");
        assertTrue(SwordGeo.onStrip(0, 0, 1, 1, 10, 1.0, 3.0, 3.0), "a diagonal strip");
        assertEquals(3.0, SwordGeo.along(0, 0, 0, 5, 1, 3), 1e-9, "a direction need not be a unit vector");
        assertEquals(1.0, SwordGeo.aside(0, 0, 0, 5, -1, 3), 1e-9, "the distance to the side is never negative");
    }

    @Test
    void backOfAFoeMeansMoreThanAboutAHundredDegreesFromWhereItLooks() {
        assertTrue(SwordGeo.behind(0, 1, 0, -3), "straight behind");
        assertTrue(SwordGeo.behind(0, 1, 2, -1), "behind and a little aside");
        assertFalse(SwordGeo.behind(0, 1, 0, 3), "in front");
        assertFalse(SwordGeo.behind(0, 1, 3, 0), "straight at its side");
        assertFalse(SwordGeo.behind(0, 1, 3, -0.2), "just past its side is not yet its back");
        assertFalse(SwordGeo.behind(0, 0, 0, -3), "a foe with no facing has no back");
        assertFalse(SwordGeo.behind(0, 1, 0, 0), "on the same spot");
    }

    @Test
    void aDashStopsBeforeWhatBlocksIt() {
        assertEquals(7.0, SwordGeo.dashLength(7, -1, 0.5), 1e-9, "nothing in the way");
        assertEquals(7.0, SwordGeo.dashLength(7, 20, 0.5), 1e-9, "a wall further than the dash");
        assertEquals(3.5, SwordGeo.dashLength(7, 4, 0.5), 1e-9);
        assertEquals(0.0, SwordGeo.dashLength(7, 0.3, 0.5), 1e-9, "a wall at once");
        assertEquals(0.0, SwordGeo.dashLength(-2, -1, 0.5), 1e-9);
    }

    @Test
    void yawFollowsMinecraftsConvention() {
        assertEquals(0.0, SwordGeo.yawOf(0, 1), 1e-4, "south is yaw 0");
        assertEquals(90.0, SwordGeo.yawOf(-1, 0), 1e-4, "west (-x) is 90");
        assertEquals(-90.0, SwordGeo.yawOf(1, 0), 1e-4, "east (+x) is -90");
        assertEquals(180.0, Math.abs(SwordGeo.yawOf(0, -1)), 1e-4, "north is 180");
    }

    // ------------------------------------------------------------------ the mark table
    @Test
    void aMarkLastsItsTimeAndIsConsumedOnce() {
        MarkBook m = new MarkBook();
        m.mark(A, 100, 120);
        assertTrue(m.has(A, 100));
        assertTrue(m.has(A, 219));
        assertFalse(m.has(A, 220), "ended");
        m.mark(B, 300, 120);
        assertTrue(m.consume(B, 310));
        assertFalse(m.consume(B, 311), "a mark is spent by the first blow");
        assertFalse(m.has(B, 311));
    }

    @Test
    void aLongerMarkIsNeverShortenedAndNothingIsMarkedForNothing() {
        MarkBook m = new MarkBook();
        m.mark(A, 100, 120);
        m.mark(A, 105, 20);
        assertTrue(m.has(A, 200), "the first mark still stands");
        m.mark(B, 100, 0);
        m.mark(null, 100, 120);
        assertEquals(1, m.size());
    }

    @Test
    void theMarkTableIsBoundedAndTheSoonestEndingMakesRoom() {
        MarkBook m = new MarkBook();
        for (int i = 0; i < SwordNumbers.MARK_CAP; i++) m.mark(UUID.nameUUIDFromBytes(("f" + i).getBytes()), 100, 60 + i);
        assertEquals(SwordNumbers.MARK_CAP, m.size());
        UUID extra = UUID.nameUUIDFromBytes("extra".getBytes());
        m.mark(extra, 100, SwordNumbers.GILDED_TICKS);
        assertEquals(SwordNumbers.MARK_CAP, m.size(), "the table did not grow");
        assertTrue(m.has(extra, 101));
        assertFalse(m.has(UUID.nameUUIDFromBytes("f0".getBytes()), 101), "the mark that ended soonest was dropped");
        assertTrue(m.has(UUID.nameUUIDFromBytes("f5".getBytes()), 101));
    }

    @Test
    void aMarkFromAClockThatWentBackIsGone() {
        MarkBook m = new MarkBook();
        m.mark(A, 1_000_000, 100);
        assertFalse(m.has(A, 10));
    }

    // ------------------------------------------------------------------ the per-target cap
    @Test
    void aTargetTakesNoMoreThanTheCapAndOnlyTwelveTargetsAreTouched() {
        HitCap cap = new HitCap(12);
        assertEquals(5.0, cap.take(A, 5), 1e-9);
        assertEquals(5.0, cap.take(A, 5), 1e-9);
        assertEquals(2.0, cap.take(A, 5), 1e-9);
        assertEquals(0.0, cap.take(A, 5), 1e-9);
        assertEquals(12.0, cap.total(A), 1e-9);
        HitCap crowd = new HitCap(12);
        for (int i = 0; i < 12; i++) assertEquals(1.0, crowd.take(UUID.nameUUIDFromBytes(("t" + i).getBytes()), 1), 1e-9);
        assertEquals(0.0, crowd.take(UUID.nameUUIDFromBytes("thirteenth".getBytes()), 1), 1e-9);
        assertEquals(0.0, crowd.take(null, 1), 1e-9);
        assertEquals(0.0, crowd.take(A, Double.NaN), 1e-9);
        assertEquals(12, crowd.targets());
    }
}
