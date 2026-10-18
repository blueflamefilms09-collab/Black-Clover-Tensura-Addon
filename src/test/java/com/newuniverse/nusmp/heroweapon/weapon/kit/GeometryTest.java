package com.newuniverse.nusmp.heroweapon.weapon.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Why: these few functions decide which foes a thrown blade, a punch and a ricochet reach and how a carriage turns, so their edges are pinned: the closest
 * point of a segment clamps at both ends, a cone includes its apex and excludes what is just outside it, a turn of yaw follows Minecraft's sense, angles wrap
 * the short way, a fan is centred, and a chain visits the nearest foe first, never the same one twice, and stops at a gap.
 */
class GeometryTest {
    @Test
    void theNearestPointOfASegmentClampsAtBothEnds() {
        assertEquals(0.5, Geometry.segmentT(0, 0, 0, 10, 0, 0, 5, 3, 0), 1e-9);
        assertEquals(0.0, Geometry.segmentT(0, 0, 0, 10, 0, 0, -4, 1, 0), 1e-9);
        assertEquals(1.0, Geometry.segmentT(0, 0, 0, 10, 0, 0, 14, 0, 0), 1e-9);
        assertEquals(0.0, Geometry.segmentT(2, 2, 2, 2, 2, 2, 9, 9, 9), 1e-9, "a segment that is a point");
        assertEquals(9.0, Geometry.distToSegmentSq(0, 0, 0, 10, 0, 0, 5, 3, 0), 1e-9);
        assertEquals(25.0, Geometry.distToSegmentSq(0, 0, 0, 10, 0, 0, 13, 4, 0), 1e-9, "beyond the end the distance is to the end");
    }

    @Test
    void aConeIncludesItsApexAndExcludesWhatIsOutside() {
        assertTrue(Geometry.inCone(0, 0, 5, 0, 0, 1, 10));
        assertTrue(Geometry.inCone(5, 0, 5, 0, 0, 1, 50), "45 degrees off is inside a 50 degree half angle");
        assertFalse(Geometry.inCone(5, 0, 5, 0, 0, 1, 40), "45 degrees off is outside a 40 degree half angle");
        assertFalse(Geometry.inCone(0, 0, -5, 0, 0, 1, 80), "behind is outside");
        assertTrue(Geometry.inCone(0, 0, 0, 0, 0, 1, 5), "the apex itself counts as inside");
        assertFalse(Geometry.inCone(1, 0, 0, 0, 0, 0, 90), "a cone without a direction holds nothing");
    }

    @Test
    void yawFollowsMinecraftsSense() {
        double[] west = Geometry.turnYaw(0, 1, 90);          // looking south, turned a quarter: west
        assertEquals(-1.0, west[0], 1e-9);
        assertEquals(0.0, west[1], 1e-9);
        double[] back = Geometry.turnYaw(0, 1, 180);
        assertEquals(0.0, back[0], 1e-9);
        assertEquals(-1.0, back[1], 1e-9);
        assertEquals(0.0, Geometry.yawOf(0, 1), 1e-9);
        assertEquals(90.0, Geometry.yawOf(-1, 0), 1e-9);
        assertEquals(-90.0, Geometry.yawOf(1, 0), 1e-9);
        assertEquals(0.0, Geometry.yawOf(0, 0), 1e-9, "no direction is yaw 0");
        // turning a vector by the yaw of another lands on it
        double[] to = Geometry.turnYaw(0, 1, Geometry.yawOf(3, 4));
        assertEquals(0.6, to[0], 1e-9);
        assertEquals(0.8, to[1], 1e-9);
    }

    @Test
    void anglesWrapAndApproachTheShortWayRound() {
        assertEquals(-170.0, Geometry.wrapDegrees(190), 1e-9);
        assertEquals(170.0, Geometry.wrapDegrees(-190), 1e-9);
        assertEquals(-180.0, Geometry.wrapDegrees(180), 1e-9);
        assertEquals(0.0, Geometry.wrapDegrees(720), 1e-9);
        assertEquals(355.0, Geometry.approachAngle(350, 10, 5), 1e-9, "from 350 towards 10 goes up through 360");
        assertEquals(-10.0, Geometry.approachAngle(10, 350, 100), 1e-9, "a small turn arrives exactly");
        assertEquals(40.0, Geometry.approachAngle(40, 40, 6), 1e-9);
        assertEquals(34.0, Geometry.approachAngle(40, 0, 6), 1e-9);
    }

    @Test
    void aFanIsCentredAndEven() {
        double[] f = Geometry.fan(5, 40);
        assertEquals(5, f.length);
        assertEquals(-20.0, f[0], 1e-9);
        assertEquals(-10.0, f[1], 1e-9);
        assertEquals(0.0, f[2], 1e-9);
        assertEquals(20.0, f[4], 1e-9);
        assertEquals(1, Geometry.fan(1, 40).length);
        assertEquals(0.0, Geometry.fan(1, 40)[0], 1e-9);
        assertEquals(1, Geometry.fan(0, 40).length, "never an empty fan");
    }

    @Test
    void aChainVisitsTheNearestFirstNeverTwiceAndStopsAtAGap() {
        double[][] pts = {{2, 0, 0}, {1, 0, 0}, {10, 0, 0}, {3, 0, 0}};
        assertEquals(List.of(1, 0, 3), Geometry.chain(0, 0, 0, pts, 5, 3.0), "the far point at 10 is a gap too wide");
        assertEquals(List.of(1, 0), Geometry.chain(0, 0, 0, pts, 2, 3.0), "at most two visits");
        assertEquals(List.of(), Geometry.chain(0, 0, 0, pts, 5, 0.5), "nothing within the first jump");
        assertEquals(List.of(1, 0, 3, 2), Geometry.chain(0, 0, 0, pts, 5, 8.0), "with a long enough jump the far one is last");
        assertEquals(List.of(), Geometry.chain(0, 0, 0, new double[0][], 3, 5.0));
    }

    @Test
    void clampKeepsAValueInside() {
        assertEquals(1.0, Geometry.clamp(5, 0, 1), 1e-9);
        assertEquals(0.0, Geometry.clamp(-5, 0, 1), 1e-9);
        assertEquals(0.4, Geometry.clamp(0.4, 0, 1), 1e-9);
    }
}
