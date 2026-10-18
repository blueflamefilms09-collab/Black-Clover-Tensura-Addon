package com.newuniverse.nusmp.client.heroweapon.carriage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: if this mapping were wrong the carriage would drive sideways, upside down or sunk into the ground, and nobody could tell without the game. So it is
 * pinned with numbers: the nose of the mesh is the front of the entity, the roof is up, the floor is on the ground, the model is centred and scaled to the
 * chosen length, and the mapping is a rotation (it keeps the lengths of vectors and the handedness).
 */
class MeshFrameTest {
    // a box 10 long (y from -4 to 6), 8 tall (x from -3 to 5, the roof at -3), 6 wide (z from -2 to 4)
    private static final MeshFrame.Box BOX = new MeshFrame.Box(-3.0, 5.0, -4.0, 6.0, -2.0, 4.0);

    @Test
    void theNoseIsTheFrontTheRoofIsUpAndTheFloorIsTheGround() {
        double s = MeshFrame.scaleFor(BOX, 3.2);
        assertEquals(0.32, s, 1e-12);
        double[] nose = MeshFrame.point(0.0, 6.0, 1.0, BOX, s);
        double[] tail = MeshFrame.point(0.0, -4.0, 1.0, BOX, s);
        assertEquals(1.6, nose[2], 1e-9, "the nose is 1.6 blocks ahead of the middle");
        assertEquals(-1.6, tail[2], 1e-9, "the tail is 1.6 behind");
        double[] roof = MeshFrame.point(-3.0, 1.0, 1.0, BOX, s);
        double[] floor = MeshFrame.point(5.0, 1.0, 1.0, BOX, s);
        assertEquals(0.0, floor[1], 1e-9, "the lowest point of the wheels rests on the ground");
        assertEquals(8 * 0.32, roof[1], 1e-9, "the roof is the highest point");
        double[] mid = MeshFrame.point(1.0, 1.0, 1.0, BOX, s);
        assertEquals(0.0, mid[0], 1e-9, "centred on its own width: z = 1 is the middle of -2..4");
        assertEquals(0.0, mid[2], 1e-9, "centred on its own length: y = 1 is the middle of -4..6");
    }

    @Test
    void theWidthRunsAcrossTheCarriage() {
        double s = MeshFrame.scaleFor(BOX, 3.2);
        double[] one = MeshFrame.point(0.0, 1.0, 4.0, BOX, s);
        double[] other = MeshFrame.point(0.0, 1.0, -2.0, BOX, s);
        assertEquals(-3 * 0.32, one[0], 1e-9);
        assertEquals(3 * 0.32, other[0], 1e-9);
        assertEquals(one[1], other[1], 1e-9, "the two sides are at the same height");
        assertEquals(one[2], other[2], 1e-9, "and at the same place along the carriage");
    }

    @Test
    void theMappingIsARotation() {
        // directions: mesh +y (nose) -> +z, mesh -x (up) -> +y, mesh -z -> +x (left)
        assertArrayClose(new double[]{0, 0, 1}, MeshFrame.normal(0, 1, 0));
        assertArrayClose(new double[]{0, 1, 0}, MeshFrame.normal(-1, 0, 0));
        assertArrayClose(new double[]{1, 0, 0}, MeshFrame.normal(0, 0, -1));
        // a rotation keeps the length and the handedness: determinant +1
        double[] ex = MeshFrame.normal(1, 0, 0), ey = MeshFrame.normal(0, 1, 0), ez = MeshFrame.normal(0, 0, 1);
        double det = ex[0] * (ey[1] * ez[2] - ey[2] * ez[1]) - ex[1] * (ey[0] * ez[2] - ey[2] * ez[0]) + ex[2] * (ey[0] * ez[1] - ey[1] * ez[0]);
        assertEquals(1.0, det, 1e-12);
        double[] v = MeshFrame.normal(0.3, -0.5, 0.8);
        assertEquals(0.3 * 0.3 + 0.5 * 0.5 + 0.8 * 0.8, v[0] * v[0] + v[1] * v[1] + v[2] * v[2], 1e-12);
    }

    @Test
    void anEmptyBoxIsNotUsable() {
        MeshFrame.Box flat = new MeshFrame.Box(0, 0, 0, 0, 0, 0);
        assertTrue(!flat.valid());
        assertEquals(1.0, MeshFrame.scaleFor(flat, 3.2), 1e-12);
        assertTrue(BOX.valid());
    }

    private static void assertArrayClose(double[] expected, double[] actual) {
        for (int i = 0; i < 3; i++) assertEquals(expected[i], actual[i], 1e-12);
    }
}
