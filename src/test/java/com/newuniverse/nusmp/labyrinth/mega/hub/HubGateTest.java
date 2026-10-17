package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the three gates of Floor 0 are the SAME block ({@code nusmp:labyrinth_gate}); what a player who steps into one gets (home, up, down, or a polite
 * refusal) is decided by {@link HubLayout#gateNear} from the block's position alone. If a gate plane cell fell outside the reach of its own centre, or
 * the reach of one gate touched another, the wrong road would open, so this test walks every cell of every gate plane the Hub can have (the home gate
 * the template writes, the two planes the terrain generator writes at the same layout constants), proves each classifies as itself, that the three
 * reaches are disjoint with plenty of room, that the cells the player walks through on the way in (one and two blocks in front of the home plane) are
 * still the home gate, and that the roads the kinds lead to are the right polarities. The contract's own test pins the centres; this one pins the whole
 * planes and the distances the Hub's protection and travel code rely on.
 */
class HubGateTest {

    private static HubLayout.GateKind kindOf(int x, int y, int z) { return HubLayout.gateNear(x, y, z); }

    @Test
    void everyCellOfTheHomeGatePlaneIsTheHomeGate() {
        int cells = 0;
        for (int x = HubLayout.HOME_GATE_X - HubPlazaLayout.GATE_HALF_WIDTH; x <= HubLayout.HOME_GATE_X + HubPlazaLayout.GATE_HALF_WIDTH; x++) {
            for (int y = HubPlazaLayout.gateBottomY(); y <= HubPlazaLayout.gateTopY(); y++) {
                assertEquals(HubLayout.GateKind.HOME, kindOf(x, y, HubLayout.HOME_GATE_Z), "home plane cell " + x + "," + y);
                cells++;
            }
        }
        assertEquals(12, cells, "3 wide, 4 high");
    }

    @Test
    void theHolyAndAbyssalPlanesOfTheSameShapeClassifyAsTheirOwnRoads() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                assertEquals(HubLayout.GateKind.HOLY, kindOf(HubLayout.HOLY_GATE_X + dx, HubLayout.HOLY_GATE_Y + dy, HubLayout.HOLY_GATE_Z), "holy " + dx + "," + dy);
                assertEquals(HubLayout.GateKind.ABYSSAL, kindOf(HubLayout.ABYSS_GATE_X + dx, HubLayout.ABYSS_GATE_Y + dy, HubLayout.ABYSS_GATE_Z), "abyssal " + dx + "," + dy);
            }
        }
    }

    @Test
    void theCellsAPlayerWalksThroughOnTheWayInStillBelongToTheHomeGate() {
        for (int dz = 1; dz <= 3; dz++) {
            assertEquals(HubLayout.GateKind.HOME, kindOf(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z + dz), "a player leaning " + dz + " in front of the plane");
        }
        assertEquals(HubLayout.GateKind.HOME, kindOf(HubLayout.HOME_GATE_X + 3, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z), "a gate block reported two blocks beyond the edge of the plane");
    }

    @Test
    void theThreeReachesAreDisjointAndFarFromTheArrivalCellAndTheSignposts() {
        int[][] centres = {{HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z},
                {HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z},
                {HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z}};
        for (int i = 0; i < centres.length; i++) {
            for (int j = i + 1; j < centres.length; j++) {
                double d = Math.sqrt(sq(centres[i][0] - centres[j][0]) + sq(centres[i][1] - centres[j][1]) + sq(centres[i][2] - centres[j][2]));
                assertTrue(d > 4 * HubLayout.GATE_REACH, "gates " + i + " and " + j + " are only " + d + " apart: their reaches would meet");
            }
        }
        assertNull(kindOf(HubLayout.SPAWN_X, HubLayout.SPAWN_Y, HubLayout.SPAWN_Z), "the arrival cell is not a gate: a newcomer must not be sent anywhere");
        assertNull(kindOf(HubLayout.PLAZA_X + 12, HubLayout.SURFACE_Y + 1, HubLayout.PLAZA_Z - 3), "the east signpost is not a gate");
        assertNull(kindOf(HubLayout.PLAZA_X - 12, HubLayout.SURFACE_Y + 1, HubLayout.PLAZA_Z - 3), "the west signpost is not a gate");
        assertNull(kindOf(HubLayout.PLAZA_X, HubLayout.SURFACE_Y, HubLayout.PLAZA_Z), "the fountain is not a gate");
    }

    @Test
    void theRoadsTheKindsLeadToAreTheRightPolarities() {
        assertNull(HubLayout.polarityOf(HubLayout.GateKind.HOME), "the way home leads to no road");
        assertEquals(Polarity.HOLY, HubLayout.polarityOf(HubLayout.GateKind.HOLY));
        assertEquals(Polarity.ABYSSAL, HubLayout.polarityOf(HubLayout.GateKind.ABYSSAL));
        assertEquals(1, HubLayout.polarityOf(HubLayout.GateKind.HOLY).sign(), "up is positive");
        assertEquals(-1, HubLayout.polarityOf(HubLayout.GateKind.ABYSSAL).sign(), "down is negative");
        assertTrue(HubLayout.HOLY_GATE_Y > HubLayout.SURFACE_Y && HubLayout.ABYSS_GATE_Y < HubLayout.SURFACE_Y, "the road up starts above the plains and the road down below");
    }

    @Test
    void theProtectedSpheresCoverTheWholePlanesTheKindsCome() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                assertTrue(HubProtect.isProtected(HubLayout.HOLY_GATE_X + dx, HubLayout.HOLY_GATE_Y + dy, HubLayout.HOLY_GATE_Z), "holy plane cell is protected");
                assertTrue(HubProtect.isProtected(HubLayout.ABYSS_GATE_X + dx, HubLayout.ABYSS_GATE_Y + dy, HubLayout.ABYSS_GATE_Z), "abyssal plane cell is protected");
            }
        }
    }

    private static double sq(int v) { return (double) v * v; }
}
