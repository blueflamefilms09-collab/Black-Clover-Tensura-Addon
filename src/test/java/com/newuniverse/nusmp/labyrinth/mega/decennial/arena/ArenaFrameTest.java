package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every part of the runtime (the placer, the element events, the seal, the sync to clients) turns plan cells into world cells and back through {@link ArenaFrame}; one wrong quarter turn would
 * put a tombstone in the wall or a seal in the wrong doorway, and nothing in the build would complain. So the conversion is pinned both ways, for all four turns and for every cell of a hall, and
 * it must agree with {@link BossArenaStructure#rotate}, the plan's own rotation.
 */
class ArenaFrameTest {
    @Test
    void worldAndLocalAreInverseForEveryTurnAndCell() {
        int[] w = new int[3], l = new int[3];
        for (int rot = 0; rot < 4; rot++) {
            ArenaFrame f = new ArenaFrame(1000, 64, -2000, 45, rot);
            for (int z = 0; z < 45; z += 4) {
                for (int x = 0; x < 45; x += 4) {
                    f.world(x, 3, z, w);
                    assertEquals(67, w[1]);
                    assertTrue(f.containsColumn(w[0], w[2]), "a cell of the plan stands inside the hall's footprint");
                    f.local(w[0], w[1], w[2], l);
                    assertEquals(x, l[0]);
                    assertEquals(3, l[1]);
                    assertEquals(z, l[2]);
                }
            }
        }
    }

    @Test
    void theTurnAgreesWithThePlansOwnRotation() {
        int[] w = new int[3], r = new int[2];
        for (int rot = 0; rot < 4; rot++) {
            ArenaFrame f = new ArenaFrame(0, 64, 0, 51, rot);
            for (int x = 0; x < 51; x += 5) {
                for (int z = 0; z < 51; z += 5) {
                    f.world(x, 0, z, w);
                    BossArenaStructure.rotate(x, z, 51, rot, r);
                    assertEquals(r[0] - 25, w[0], "x at turn " + rot);
                    assertEquals(r[1] - 25, w[2], "z at turn " + rot);
                }
            }
        }
    }

    @Test
    void theGateIsCarriedToTheWayTheFloorsGatekeeperFaces() {
        int[] w = new int[3];
        // the plan's Gatekeeper is on the middle column of the north wall (z = 1): it must land on the side the offset points to
        int[][] offsets = {{0, -20}, {20, 0}, {0, 20}, {-20, 0}};
        for (int[] o : offsets) {
            int rot = ArenaFrame.rotationFacing(o[0], o[1]);
            ArenaFrame f = new ArenaFrame(500, 64, 500, 45, rot);
            f.world(22, 1, 1, w);
            assertEquals(Integer.signum(o[0]), Integer.signum(w[0] - 500), "x side for offset " + o[0] + "," + o[1]);
            assertEquals(Integer.signum(o[1]), Integer.signum(w[2] - 500), "z side for offset " + o[0] + "," + o[1]);
        }
        assertEquals(0, ArenaFrame.rotationFacing(0, 0));
        assertEquals(3, ArenaFrame.rotationFacing(-5, 3), "the larger axis wins");
        assertEquals(new ArenaFrame(0, 0, 0, 45, -1).rot(), 3, "a negative turn wraps");
    }

    @Test
    void containsKnowsTheFootprintAndTheFoundation() {
        ArenaFrame f = new ArenaFrame(100, 64, 100, 45, 0);
        assertTrue(f.contains(100, 64, 100, 30));
        assertTrue(f.contains(78, 60, 122, 30), "the corner and the foundation row are inside");
        assertFalse(f.contains(77, 64, 100, 30));
        assertFalse(f.contains(100, 59, 100, 30));
        assertFalse(f.contains(100, 94, 100, 30));
        assertEquals(5.0, f.distance(103, 104), 1e-9);
    }
}
