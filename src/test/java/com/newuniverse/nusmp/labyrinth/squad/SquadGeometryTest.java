package com.newuniverse.nusmp.labyrinth.squad;

import com.newuniverse.nusmp.labyrinth.squad.SquadGeometry.V2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The geometry of {@link SquadGeometry}: the intercept point, the flank point, the retreat point and the formation frame. */
class SquadGeometryTest {
    private static final double EPS = 1e-9;

    @Test
    void facingFollowsMinecraftYaw() {
        V2 south = SquadGeometry.facing(0);
        assertEquals(0.0, south.x(), EPS);
        assertEquals(1.0, south.z(), EPS);
        V2 west = SquadGeometry.facing(90);
        assertEquals(-1.0, west.x(), 1e-9);
        assertEquals(0.0, west.z(), 1e-9);
        V2 r = SquadGeometry.right(0);
        assertEquals(-1.0, r.x(), EPS, "facing south, the right hand is west (-x)");
        assertEquals(90.0, SquadGeometry.yawToward(new V2(0, 0), new V2(-5, 0)), 1e-9);
        assertEquals(0.0, SquadGeometry.yawToward(new V2(0, 0), new V2(0, 5)), 1e-9);
        assertEquals(-170.0, SquadGeometry.angleDiff(190.0, 0.0), 1e-9);
        assertEquals(20.0, SquadGeometry.angleDiff(10.0, -10.0), 1e-9);
    }

    @Test
    void theInterceptPointIsOneAndAHalfBlocksFromTheWardOnTheSegmentToTheTarget() {
        V2 ward = new V2(10, 10), target = new V2(10, 30);
        V2 p = SquadGeometry.interceptPoint(ward, target, 1.5);
        assertEquals(1.5, p.dist(ward), 1e-9);
        assertEquals(10.0, p.x(), 1e-9);
        assertEquals(11.5, p.z(), 1e-9);
        assertEquals(0.0, SquadGeometry.distToSegment(p, ward, target), 1e-9, "the point lies on the segment");
        V2 diag = SquadGeometry.interceptPoint(new V2(0, 0), new V2(30, 40), 1.5);
        assertEquals(1.5, diag.dist(new V2(0, 0)), 1e-9);
        assertEquals(0.0, SquadGeometry.distToSegment(diag, new V2(0, 0), new V2(30, 40)), 1e-9);
        assertEquals(0.9, diag.x(), 1e-9);
        assertEquals(1.2, diag.z(), 1e-9);
    }

    @Test
    void theInterceptPointNeverPassesTheTarget() {
        V2 p = SquadGeometry.interceptPoint(new V2(0, 0), new V2(2, 0), 1.5);
        assertEquals(1.0, p.x(), 1e-9, "closer than twice the gap: the middle");
        assertEquals(new V2(5, 5), SquadGeometry.interceptPoint(new V2(5, 5), new V2(5, 5), 1.5));
    }

    @Test
    void coveringMeansBetweenTheWardAndTheTarget() {
        V2 ward = new V2(0, 0), target = new V2(0, 20);
        assertTrue(SquadGeometry.covers(new V2(0.5, 3), ward, target, 1.6));
        assertFalse(SquadGeometry.covers(new V2(6, 3), ward, target, 1.6), "off to the side");
        assertFalse(SquadGeometry.covers(new V2(0, -6), ward, target, 1.6), "behind the ward");
        assertTrue(SquadGeometry.covers(new V2(0, -1), ward, target, 1.6), "right at the ward");
        assertEquals(0.0, SquadGeometry.projectT(new V2(0, -5), ward, target), EPS);
        assertEquals(1.0, SquadGeometry.projectT(new V2(0, 50), ward, target), EPS);
        assertEquals(0.5, SquadGeometry.projectT(new V2(7, 10), ward, target), EPS);
        assertEquals(0.0, SquadGeometry.projectT(new V2(3, 3), ward, ward), EPS, "a zero-length segment");
    }

    @Test
    void aFlankPointIsBetween110And150DegreesOffTheFacing() {
        V2 target = new V2(100, -40);
        for (double yaw : new double[]{0, 45, 90, 180, -135, 270}) {
            for (int i = 0; i < 12; i++) {
                for (boolean flip : new boolean[]{false, true}) {
                    double rel = SquadGeometry.flankAngle(i, flip);
                    double mag = Math.abs(rel);
                    assertTrue(mag >= 110.0 - 1e-9 && mag <= 150.0 + 1e-9, "angle " + rel);
                    V2 p = SquadGeometry.flankPoint(target, yaw, rel, 2.4);
                    assertEquals(2.4, p.dist(target), 1e-9);
                    double off = SquadGeometry.offFacing(target, yaw, p);
                    assertEquals(mag, off, 1e-6, "the point stands that many degrees off the facing");
                }
            }
        }
    }

    @Test
    void flankersTakeOppositeSidesAndDifferentAngles() {
        double a = SquadGeometry.flankAngle(0, false), b = SquadGeometry.flankAngle(1, false), c = SquadGeometry.flankAngle(2, false);
        assertTrue(a * b < 0, "neighbours come from opposite flanks");
        assertTrue(Math.abs(Math.abs(a) - Math.abs(c)) > 1.0, "two flankers on one side do not take the same angle");
        assertEquals(-a, SquadGeometry.flankAngle(0, true), EPS, "a flip turns the side");
    }

    @Test
    void theTargetSeesWhoIsInFrontOfItAndNotWhoIsBehind() {
        V2 target = new V2(0, 0);
        assertTrue(SquadGeometry.seenBy(target, 0, new V2(0, 5), 40, 8), "straight ahead, close");
        assertFalse(SquadGeometry.seenBy(target, 0, new V2(0, 12), 40, 8), "ahead but far");
        assertFalse(SquadGeometry.seenBy(target, 0, new V2(0, -5), 40, 8), "behind");
        assertFalse(SquadGeometry.seenBy(target, 0, new V2(5, 1), 40, 8), "to the side");
        V2 behind = SquadGeometry.flankPoint(target, 0, 130, 3);
        assertFalse(SquadGeometry.seenBy(target, 0, behind, 40, 8), "a flank point is out of the cone");
    }

    @Test
    void aHealerBacksAwayInsideTheBandAndStaysInsideIt() {
        V2 threat = new V2(0, 0), centre = new V2(0, -20);
        V2 self = new V2(0, -4);
        V2 p = SquadGeometry.retreatPoint(self, threat, centre, 8, 12);
        assertNotNull(p);
        assertEquals(10.0, p.dist(threat), 1e-6, "the middle of the band");
        assertTrue(p.dist(threat) > self.dist(threat), "farther than before");
        assertNull(SquadGeometry.retreatPoint(new V2(0, -10), threat, centre, 8, 12), "in the band: stay");
        V2 far = SquadGeometry.retreatPoint(new V2(0, -40), threat, centre, 8, 12);
        assertNotNull(far);
        assertEquals(10.0, far.dist(threat), 1e-6, "too far: come back to the band");
        V2 onTop = SquadGeometry.retreatPoint(new V2(0, 0), threat, centre, 8, 12);
        assertNotNull(onTop, "standing on the threat still gives a place");
        assertEquals(10.0, onTop.dist(threat), 1e-6);
    }

    @Test
    void aFormationSlotTurnsWithTheSquad() {
        V2 c = new V2(50, 50);
        V2 s0 = SquadGeometry.slot(c, 0, 3, 2);
        assertEquals(50 - 2, s0.x(), 1e-9, "facing south, 2 to the right is -x");
        assertEquals(53, s0.z(), 1e-9);
        V2 s90 = SquadGeometry.slot(c, 90, 3, 0);
        assertEquals(47, s90.x(), 1e-9, "facing west, 3 ahead is -x");
        assertEquals(50, s90.z(), 1e-9);
        assertEquals(new V2(1, 1), SquadGeometry.centroid(List.of(new V2(0, 0), new V2(2, 2))));
    }
}
