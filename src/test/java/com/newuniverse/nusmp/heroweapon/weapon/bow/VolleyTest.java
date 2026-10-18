package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: a rain of arrows must be spread over its circle, the same every time it is computed, and different from wave to wave; these are the properties the
 * golden-angle layout promises, checked against a count of points, the radius, the spacing and the salt.
 */
class VolleyTest {
    @Test
    void everyPointIsInsideTheCircle() {
        for (int n : new int[]{1, 6, 10, 40}) {
            double[][] pts = Volley.scatter(n, 5.0, 3);
            assertEquals(n, pts.length);
            for (double[] p : pts) assertTrue(Math.sqrt(p[0] * p[0] + p[1] * p[1]) <= 5.0 + 1e-9, "outside the circle: " + p[0] + ", " + p[1]);
        }
    }

    @Test
    void noTwoPointsSitOnTopOfEachOther() {
        double[][] pts = Volley.scatter(10, 8.0, 0);
        for (int i = 0; i < pts.length; i++)
            for (int j = i + 1; j < pts.length; j++)
                assertTrue(Math.hypot(pts[i][0] - pts[j][0], pts[i][1] - pts[j][1]) > 0.4, "points " + i + " and " + j + " are nearly the same");
    }

    @Test
    void theSameInputGivesTheSamePointsAndTheSaltChangesThem() {
        double[][] a = Volley.scatter(6, 5.0, 1), b = Volley.scatter(6, 5.0, 1), c = Volley.scatter(6, 5.0, 2);
        for (int i = 0; i < 6; i++) {
            assertEquals(a[i][0], b[i][0], 0.0);
            assertEquals(a[i][1], b[i][1], 0.0);
        }
        assertTrue(a[0][0] != c[0][0], "the next wave falls in other places");
    }

    @Test
    void zeroOrNegativeCountsGiveNothing() {
        assertEquals(0, Volley.scatter(0, 5.0, 0).length);
        assertEquals(0, Volley.scatter(-3, 5.0, 0).length);
    }
}
