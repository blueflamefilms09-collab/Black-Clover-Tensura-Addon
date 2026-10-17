package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The shapes on the floor: cones, lanes, ring waves, fans, spokes and the plan of marks. */
class DmGeometryTest {
    private static final double[] SOUTH = {0, 1}, EAST = {1, 0};

    @Test
    void yawsFollowMinecraft() {
        assertEquals(0.0, DmGeometry.yawOf(0, 1), 1e-9);
        assertEquals(-90.0, DmGeometry.yawOf(1, 0), 1e-9);
        assertEquals(90.0, DmGeometry.yawOf(-1, 0), 1e-9);
        assertEquals(180.0, Math.abs(DmGeometry.yawOf(0, -1)), 1e-9);
        for (double yaw = -170; yaw <= 170; yaw += 17) {
            double[] d = DmGeometry.dirOfYaw(yaw);
            assertEquals(1.0, Math.hypot(d[0], d[1]), 1e-9);
            assertEquals(yaw, DmGeometry.yawOf(d[0], d[1]), 1e-6, "round trip at " + yaw);
        }
        assertEquals(0.0, DmGeometry.yawDiffDeg(10, 10), 1e-9);
        assertEquals(20.0, DmGeometry.yawDiffDeg(170, -170), 1e-9, "across the seam");
        assertEquals(90.0, DmGeometry.yawDiffDeg(0, 270), 1e-9);
        assertEquals(180.0, DmGeometry.yawDiffDeg(90, -90), 1e-9);
    }

    @Test
    void rotationTurnsFromXTowardsZ() {
        double[] r = DmGeometry.rotate(1, 0, Math.PI / 2);
        assertEquals(0.0, r[0], 1e-9);
        assertEquals(1.0, r[1], 1e-9);
        double[] back = DmGeometry.rotate(r[0], r[1], -Math.PI / 2);
        assertEquals(1.0, back[0], 1e-9);
        assertEquals(0.0, back[1], 1e-9);
        double[] u = DmGeometry.unit(0, 0, SOUTH);
        assertEquals(1.0, u[1], 1e-12, "a zero vector falls back");
        double[] v = DmGeometry.unit(3, 4, SOUTH);
        assertEquals(0.6, v[0], 1e-12);
        assertEquals(0.8, v[1], 1e-12);
    }

    @Test
    void anglesBetweenVectors() {
        assertEquals(90.0, DmGeometry.angleBetweenDeg(1, 0, 0, 1), 1e-9);
        assertEquals(0.0, DmGeometry.angleBetweenDeg(2, 0, 5, 0), 1e-9);
        assertEquals(180.0, DmGeometry.angleBetweenDeg(1, 0, -3, 0), 1e-9);
        assertEquals(0.0, DmGeometry.angleBetweenDeg(0, 0, 1, 0), 1e-9, "no direction, no angle");
    }

    @Test
    void aConeHasReachAndAHalfAngleAndAMercyAtTheAttackersFeet() {
        // facing south, 4.4 blocks, 55 degrees to each side
        assertTrue(DmGeometry.inCone(0, 3, 0, 1, 55, 4.4, 0.3), "straight ahead");
        assertFalse(DmGeometry.inCone(0, 5.0, 0, 1, 55, 4.4, 0.3), "beyond the reach plus the body");
        assertTrue(DmGeometry.inCone(0, 4.6, 0, 1, 55, 4.4, 0.3), "the body radius lengthens the reach");
        assertFalse(DmGeometry.inCone(0, -3, 0, 1, 55, 4.4, 0.3), "behind");
        assertFalse(DmGeometry.inCone(3.5, 0.5, 0, 1, 55, 4.4, 0.3), "to the side, outside the angle");
        assertTrue(DmGeometry.inCone(2.5, 2.5, 0, 1, 55, 4.4, 0.3), "45 degrees off the axis");
        assertFalse(DmGeometry.inCone(3.2, 1.0, 0, 1, 55, 4.4, 0.0), "73 degrees off the axis with no body");
        assertTrue(DmGeometry.inCone(1.0, -0.2, 0, 1, 10, 4.4, 0.0), "standing on the attacker is always inside");
        assertFalse(DmGeometry.inCone(0, 3, 1, 0, 20, 4.4, 0.0), "facing east, a target south is out");
        assertTrue(DmGeometry.inCone(3, 0, 1, 0, 20, 4.4, 0.0), "facing east, a target east is in");
        // symmetric left and right
        for (double off = 0.5; off < 4; off += 0.5)
            assertEquals(DmGeometry.inCone(off, 3, 0, 1, 40, 5, 0.3), DmGeometry.inCone(-off, 3, 0, 1, 40, 5, 0.3), "mirror at " + off);
    }

    @Test
    void aWideConeBehavesLikeASweep() {
        // 100 degrees to each side: nearly a half circle of 8 blocks, but never behind
        assertTrue(DmGeometry.inCone(6, 0, 0, 1, 100, 8, 0.3), "straight to the side");
        assertTrue(DmGeometry.inCone(5, -1, 0, 1, 100, 8, 0.3), "slightly behind the side line");
        assertFalse(DmGeometry.inCone(0, -6, 0, 1, 100, 8, 0.3), "directly behind");
    }

    @Test
    void aLaneIsLongAndNarrow() {
        assertTrue(DmGeometry.inLane(0, 10, 0, 1, 18, 3.2, 0.3), "in the middle");
        assertTrue(DmGeometry.inLane(1.8, 10, 0, 1, 18, 3.2, 0.3), "at the edge, the body radius counts");
        assertFalse(DmGeometry.inLane(2.2, 10, 0, 1, 18, 3.2, 0.3), "beside the lane");
        assertFalse(DmGeometry.inLane(0, 19, 0, 1, 18, 3.2, 0.3), "past the end");
        assertTrue(DmGeometry.inLane(0, 18.2, 0, 1, 18, 3.2, 0.3), "the end is lengthened by the body");
        assertTrue(DmGeometry.inLane(0, -0.2, 0, 1, 18, 3.2, 0.3), "just behind the start, still the body");
        assertFalse(DmGeometry.inLane(0, -1, 0, 1, 18, 3.2, 0.3), "behind");
        assertTrue(DmGeometry.inLane(10, 0, 1, 0, 18, 3.2, 0.3), "a lane along x");
        assertEquals(10.0, DmGeometry.along(10, 3, 1, 0), 1e-12);
        assertEquals(3.0, DmGeometry.lateral(10, 3, 1, 0), 1e-12);
        assertEquals(3.0, DmGeometry.lateral(10, -3, 1, 0), 1e-12);
    }

    @Test
    void aRingWaveHitsOnlyWhereItsFrontIs() {
        assertTrue(DmGeometry.ringHit(5.0, 5.5, 1.3));
        assertFalse(DmGeometry.ringHit(3.0, 5.5, 1.3));
        assertFalse(DmGeometry.ringHit(8.0, 5.5, 1.3));
        assertEquals(0.5, DmGeometry.waveRadius(0, 0.5, 1.1), 1e-12);
        assertEquals(6.0, DmGeometry.waveRadius(5, 0.5, 1.1), 1e-12);
        assertEquals(0.5, DmGeometry.waveRadius(-3, 0.5, 1.1), 1e-12, "no negative age");
        // the wave outlives its reach by a little: ceil((8.5 + 1.3 - 0.5) / 1.1) + 1 = 10
        assertEquals(10, DmGeometry.waveTicks(8.5, 0.5, 1.1, 1.3));
        // every foe within the reach is crossed by the front at some tick of the wave
        for (double d = 0.5; d <= 8.5; d += 0.25) {
            boolean crossed = false;
            for (int age = 0; age < DmGeometry.waveTicks(8.5, 0.5, 1.1, 1.3); age++) crossed |= DmGeometry.ringHit(d, DmGeometry.waveRadius(age, 0.5, 1.1), 1.3);
            assertTrue(crossed, "the wave reaches " + d);
        }
    }

    @Test
    void aFanIsCentredAndSymmetric() {
        double[] one = DmGeometry.fan(1, 40);
        assertEquals(1, one.length);
        assertEquals(0.0, one[0], 1e-12);
        double[] five = DmGeometry.fan(5, 40);
        assertEquals(5, five.length);
        assertEquals(0.0, five[2], 1e-12);
        assertEquals(-five[0], five[4], 1e-12);
        assertEquals(Math.toRadians(40), five[4] - five[0], 1e-12);
        for (int i = 1; i < five.length; i++) assertTrue(five[i] > five[i - 1]);
        assertEquals(4, DmGeometry.fan(4, 0).length, "no spread, all straight");
        assertEquals(0.0, DmGeometry.fan(4, 0)[3], 1e-12);
        assertEquals(1, DmGeometry.fan(0, 30).length, "at least one");
    }

    @Test
    void spokesAreEvenAndLeaveGapsWorthStandingIn() {
        double[][] s = DmGeometry.spokes(5, 0.3);
        assertEquals(5, s.length);
        for (double[] d : s) assertEquals(1.0, Math.hypot(d[0], d[1]), 1e-9);
        assertEquals(72.0, DmGeometry.angleBetweenDeg(s[0][0], s[0][1], s[1][0], s[1][1]), 1e-6);
        // between two spokes at 10 blocks there is room; on a spoke there is none
        double midAngle = 0.3 + Math.PI / 5;
        double px = Math.cos(midAngle) * 10, pz = Math.sin(midAngle) * 10;
        assertFalse(DmGeometry.inAnySpoke(px, pz, s, 18, 2.4, 0.3), "the gap between two spokes is safe");
        assertTrue(DmGeometry.inAnySpoke(Math.cos(0.3) * 10, Math.sin(0.3) * 10, s, 18, 2.4, 0.3), "on a spoke");
        assertTrue(DmGeometry.inAnySpoke(0.4, 0.2, s, 18, 2.4, 0.3), "the middle is in every spoke");
        assertFalse(DmGeometry.inAnySpoke(Math.cos(midAngle) * 25, Math.sin(midAngle) * 25, s, 18, 2.4, 0.3), "beyond the end");
        assertTrue(DmGeometry.spokeGap(5, 10, 2.4) > 3.0, "five spokes leave 3.9 blocks between them at 10 blocks");
        assertTrue(DmGeometry.spokeGap(9, 4, 2.4) < 1.0, "nine spokes close to the middle leave no room: the gap is only useful further out");
        assertEquals(1, DmGeometry.spokes(0, 0).length);
    }

    @Test
    void marksNeverStackAndTheFirstIsOnTheTarget() {
        double[][] others = {{3, 0}, {20, 0}, {-18, 9}};
        for (int n = 1; n <= 9; n++) {
            List<double[]> m = DmGeometry.markPlan(n, 0, 0, others, 2.3, 0.4);
            assertEquals(n, m.size(), "all " + n + " fit");
            assertEquals(0.0, m.get(0)[0], 1e-12);
            assertEquals(0.0, m.get(0)[1], 1e-12);
            for (int i = 0; i < m.size(); i++)
                for (int j = i + 1; j < m.size(); j++)
                    assertTrue(Math.hypot(m.get(i)[0] - m.get(j)[0], m.get(i)[1] - m.get(j)[1]) >= 2.3 * 1.6 - 1e-9, "marks " + i + " and " + j + " of " + n);
        }
        // the near foe at 3 blocks is too close to the first mark to get its own, the far ones do
        List<double[]> two = DmGeometry.markPlan(3, 0, 0, others, 2.3, 0.4);
        assertEquals(20.0, two.get(1)[0], 1e-12);
        assertEquals(-18.0, two.get(2)[0], 1e-12);
        assertEquals(0, DmGeometry.markPlan(0, 0, 0, null, 2.3, 0).size());
        assertEquals(1, DmGeometry.markPlan(1, 5, 5, null, 2.3, 0).size());
        assertEquals(4, DmGeometry.markPlan(4, 5, 5, new double[][]{null, {1}}, 2.3, 0).size(), "bad foe entries are skipped");
    }
}
