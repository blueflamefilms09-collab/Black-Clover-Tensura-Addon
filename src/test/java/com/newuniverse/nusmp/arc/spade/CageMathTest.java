package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Dissection Theatre's cage: it closes, it always has doors, the doors are spread, and a door is never a wall. */
class CageMathTest {
    private static boolean[] allOpen(int n) {
        boolean[] o = new boolean[n];
        java.util.Arrays.fill(o, true);
        return o;
    }

    @Test
    void theRingClosesLinearlyAndStopsAtTheEnd() {
        assertEquals(CageMath.START, CageMath.radius(0, 120), 1e-9);
        assertEquals(CageMath.END, CageMath.radius(120, 120), 1e-9);
        assertEquals(CageMath.END, CageMath.radius(500, 120), 1e-9);
        assertEquals((CageMath.START + CageMath.END) / 2, CageMath.radius(60, 120), 1e-9);
        assertEquals(CageMath.START, CageMath.radius(-4, 120), 1e-9);
        double last = CageMath.START;
        for (int a = 0; a <= 120; a += 6) {
            double r = CageMath.radius(a, 120);
            assertTrue(r <= last + 1e-12, "it only closes");
            last = r;
        }
    }

    @Test
    void theNumbersAreTheOnesTheDocumentationPromises() {
        assertEquals(16.0, CageMath.START, 1e-9);
        assertEquals(5.0, CageMath.END, 1e-9);
        assertEquals(1.25, CageMath.WALL, 1e-9);
        assertEquals(3, CageMath.DOORS, "three doors");
        assertEquals(40.0, Math.toDegrees(CageMath.DOOR_HALF) * 2, 1e-9, "each door is forty degrees wide");
        assertEquals(10.94, CageMath.doorWidth(CageMath.START), 0.01, "about eleven blocks at the start");
        assertEquals(3.42, CageMath.doorWidth(CageMath.END), 0.01, "about three and a half at the end");
    }

    @Test
    void theRingClosesSlowerThanAPlayerWalks() {
        double perTick = (CageMath.START - CageMath.END) / 120.0;
        assertTrue(perTick < 0.216 / 2, "the wall moves at " + perTick + " blocks a tick: nobody is run down");
    }

    @Test
    void aCageAlwaysHasItsDoorsSpreadRoundIt() {
        double[] d = CageMath.doors(allOpen(12), CageMath.DOORS);
        assertEquals(CageMath.DOORS, d.length);
        for (int i = 0; i < d.length; i++)
            for (int j = i + 1; j < d.length; j++)
                assertTrue(CageMath.angularDistance(d[i], d[j]) >= Math.toRadians(89), "doors apart: " + d[i] + " " + d[j]);
        // nowhere open: the doors are spread evenly anyway
        double[] none = CageMath.doors(new boolean[12], CageMath.DOORS);
        assertEquals(CageMath.DOORS, none.length);
        for (int i = 0; i < none.length; i++)
            for (int j = i + 1; j < none.length; j++)
                assertEquals(Math.toRadians(120), CageMath.angularDistance(none[i], none[j]), 1e-6);
        assertEquals(CageMath.DOORS, CageMath.doors(null, CageMath.DOORS).length);
    }

    @Test
    void doorsPreferTheDirectionsWithStandingRoom() {
        boolean[] open = new boolean[12];
        open[3] = true;
        open[7] = true;
        double[] d = CageMath.doors(open, 3);
        assertEquals(3, d.length);
        boolean has3 = false, has7 = false;
        for (double a : d) {
            if (CageMath.angularDistance(a, 3 * Math.PI * 2 / 12) < 1e-6) has3 = true;
            if (CageMath.angularDistance(a, 7 * Math.PI * 2 / 12) < 1e-6) has7 = true;
        }
        assertTrue(has3 && has7, "both open directions became doors");
        // a single open direction is a door, the others spread from it
        boolean[] one = new boolean[12];
        one[5] = true;
        double[] e = CageMath.doors(one, 3);
        assertEquals(3, e.length);
        assertEquals(5 * Math.PI * 2 / 12, e[0], 1e-9, "the open one first");
    }

    @Test
    void aFoeInADoorIsNeverTouchedAndOneOnTheWallIs() {
        double[] doors = {0.0, 2.0, 4.0};
        assertFalse(CageMath.touches(10, 0.0, 10, doors), "in a door on the wall");
        assertFalse(CageMath.touches(10, CageMath.DOOR_HALF - 0.01, 10, doors), "at the edge of the door");
        assertTrue(CageMath.touches(10, 1.0, 10, doors), "between two doors, on the wall");
        assertTrue(CageMath.touches(10 + CageMath.WALL - 0.01, 1.0, 10, doors), "the wall has a thickness");
        assertFalse(CageMath.touches(10 + CageMath.WALL + 0.01, 1.0, 10, doors), "beyond it is outside");
        assertFalse(CageMath.touches(7, 1.0, 10, doors), "inside the ring is not the wall");
        assertTrue(CageMath.touches(10, 1.0, 10, null), "no doors, every angle is wall");
    }

    @Test
    void aDoorIsWideEnoughToWalkThroughEvenAtTheEnd() {
        assertTrue(CageMath.doorWidth(CageMath.START) > 10);
        assertTrue(CageMath.doorWidth(CageMath.END) > 3.0, "still wider than two players: " + CageMath.doorWidth(CageMath.END));
    }

    @Test
    void anglesWrapTheRightWay() {
        assertEquals(Math.PI / 2, CageMath.angle(0, 1), 1e-9);
        assertEquals(Math.PI * 1.5, CageMath.angle(0, -1), 1e-9);
        assertEquals(0.2, CageMath.angularDistance(0.1, Math.PI * 2 - 0.1), 1e-9, "across the seam");
        assertEquals(Math.PI, CageMath.angularDistance(0, Math.PI), 1e-9);
    }
}
