package com.newuniverse.nusmp.relics.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The corridor of the God's Writ and the facing of its appearance. */
class OrderGeometryTest {
    @Test
    void distanceToASegmentIsMeasuredToItsNearestPoint() {
        assertEquals(0.0, OrderGeometry.distanceToSegment(5, 0, 0, 0, 10, 0), 1e-9);
        assertEquals(3.0, OrderGeometry.distanceToSegment(5, 3, 0, 0, 10, 0), 1e-9);
        assertEquals(5.0, OrderGeometry.distanceToSegment(-3, 4, 0, 0, 10, 0), 1e-9, "past the start: to the start point");
        assertEquals(5.0, OrderGeometry.distanceToSegment(13, 4, 0, 0, 10, 0), 1e-9, "past the end: to the end point");
        assertEquals(5.0, OrderGeometry.distanceToSegment(3, 4, 0, 0, 0, 0), 1e-9, "a segment of no length is a point");
    }

    @Test
    void theCorridorHasALengthAWidthAndAHeight() {
        // a Writ from the origin along +x: 20 long, 1.4 to each side, 3 above and below
        assertTrue(OrderGeometry.inCorridor(10, 0, 1.3, 0, 0, 0, 1, 0, 20, 1.4, 3));
        assertFalse(OrderGeometry.inCorridor(10, 0, 1.5, 0, 0, 0, 1, 0, 20, 1.4, 3), "just out to the side");
        assertFalse(OrderGeometry.inCorridor(21.5, 0, 0, 0, 0, 0, 1, 0, 20, 1.4, 3), "past the end");
        assertFalse(OrderGeometry.inCorridor(-2, 0, 0, 0, 0, 0, 1, 0, 20, 1.4, 3), "behind the God");
        assertFalse(OrderGeometry.inCorridor(10, 3.5, 0, 0, 0, 0, 1, 0, 20, 1.4, 3), "on a ledge far above");
        assertTrue(OrderGeometry.inCorridor(10, -2.9, 0, 0, 0, 0, 1, 0, 20, 1.4, 3), "in a pit just below");
    }

    @Test
    void theYawFacesTheGivenDirection() {
        assertEquals(0f, OrderGeometry.yawToward(0, 1), 1e-4);
        assertEquals(90f, Math.abs(OrderGeometry.yawToward(-1, 0)), 1e-4);
        assertEquals(-90f, OrderGeometry.yawToward(1, 0), 1e-4);
        assertEquals(180f, Math.abs(OrderGeometry.yawToward(0, -1)), 1e-4);
    }
}
