package com.newuniverse.nusmp.labyrinth.mega;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the floor arithmetic is the one place where the owner's words ("every 10th floor", "Floor +1 / -1", "one relic per 10 floors") become numbers, and
 * the alignment lock, the System overlay, the encounter tables, the loot and the Rukh flow are all written in parallel against it. Pinned here so a drift
 * in the sign, the band of a floor, the decennial rule or the banding of room depths fails the build instead of showing up as a wrong floor in game.
 * Also pins the Hub's gate lookup, because three modules rely on it finding the right gate from a position alone.
 */
public class FloorMapTest {

    @Test
    public void floorsAreSignedAndNeverZero() {
        assertEquals(1, FloorMap.floorOf(Polarity.HOLY, 0, 1));
        assertEquals(10, FloorMap.floorOf(Polarity.HOLY, 0, 10));
        assertEquals(11, FloorMap.floorOf(Polarity.HOLY, 1, 1));
        assertEquals(-1, FloorMap.floorOf(Polarity.ABYSSAL, 0, 1));
        assertEquals(-20, FloorMap.floorOf(Polarity.ABYSSAL, 1, 10));
        for (Polarity p : Polarity.values()) {
            for (int chain = 0; chain < 6; chain++) {
                for (int band = 1; band <= 10; band++) {
                    int f = FloorMap.floorOf(p, chain, band);
                    assertTrue(f != 0, "never Floor 0");
                    assertEquals(p, Polarity.ofFloor(f), "the sign is the polarity");
                    assertEquals(band, FloorMap.band(f), "band round-trips");
                    assertEquals(chain, FloorMap.chainIndex(f), "chain round-trips");
                }
            }
        }
    }

    @Test
    public void bandAndChainClampTheirInput() {
        assertEquals(1, FloorMap.floorOf(Polarity.HOLY, 0, 0), "a band below 1 reads as 1");
        assertEquals(10, FloorMap.floorOf(Polarity.HOLY, 0, 99), "a band above the last reads as the last");
        assertEquals(1, FloorMap.floorOf(Polarity.HOLY, -3, 1), "a negative chain index reads as 0");
        assertEquals(0, FloorMap.band(0));
        assertEquals(-1, FloorMap.chainIndex(0));
    }

    @Test
    public void everyTenthFloorIsDecennial() {
        for (int f : new int[] {10, 20, 30, -10, -20, -30}) assertTrue(FloorMap.isDecennial(f), f + " is decennial");
        for (int f : new int[] {0, 1, 9, 11, 19, -1, -9, -11}) assertFalse(FloorMap.isDecennial(f), f + " is not decennial");
        assertTrue(FloorMap.isDecennial(5, 5), "a server may use shorter regions");
        assertEquals(FloorMap.lastFloor(Polarity.HOLY, 2, 10), 30);
        assertEquals(FloorMap.lastFloor(Polarity.ABYSSAL, 0, 10), -10);
        assertEquals(FloorMap.firstFloor(Polarity.ABYSSAL, 1, 10), -11);
    }

    @Test
    public void depthsAreSpreadOverTheBandsAndTheArenaIsTheLast() {
        // 30 doorways from the Landing to the arena
        assertEquals(1, FloorMap.bandOfDepth(0, 30), "the Landing is floor 1 of its sector");
        assertEquals(10, FloorMap.bandOfDepth(30, 30), "the arena is the tenth floor");
        assertEquals(10, FloorMap.bandOfDepth(45, 30), "beyond the arena (vault, Way Onward) is still the tenth floor");
        assertEquals(10, FloorMap.bandOfDepth(-1, 30), "an unreachable room does not pose as an early floor");
        assertEquals(9, FloorMap.bandOfDepth(29, 30), "the last room before the arena is floor 9");
        int previous = 0;
        for (int d = 0; d <= 40; d++) {
            int b = FloorMap.bandOfDepth(d, 30);
            assertTrue(b >= previous, "monotone at depth " + d);
            assertTrue(b >= 1 && b <= 10, "in range at depth " + d);
            previous = b;
        }
        // every band 1..9 is used when the arena is deep enough
        boolean[] used = new boolean[11];
        for (int d = 0; d < 30; d++) used[FloorMap.bandOfDepth(d, 30)] = true;
        for (int b = 1; b <= 9; b++) assertTrue(used[b], "band " + b + " has rooms");
        assertEquals(10, FloorMap.bandOfDepth(0, 0), "a degenerate arena depth makes everything the last floor");
        assertEquals(1, FloorMap.bandOfDepth(0, 5, 1), "a one-floor region is always its floor 1");
    }

    @Test
    public void threatAndLabels() {
        assertEquals(1, FloorMap.threat(0));
        assertEquals(1, FloorMap.threat(1));
        assertEquals(7, FloorMap.threat(-7));
        assertEquals("+12", FloorMap.label(12));
        assertEquals("-3", FloorMap.label(-3));
        assertEquals("0", FloorMap.label(0));
    }

    @Test
    public void polarityIdsRoundTrip() {
        for (Polarity p : Polarity.values()) {
            assertEquals(p, Polarity.fromId(p.id()));
            assertEquals(p, Polarity.fromId(p.id().toUpperCase()));
            assertEquals(p.opposite().opposite(), p);
            assertTrue(p.sign() == 1 || p.sign() == -1);
        }
        assertNull(Polarity.fromId("nonsense"));
        assertNull(Polarity.fromId(null));
        assertNull(Polarity.ofFloor(0));
        assertEquals(Polarity.HOLY, Polarity.ofFloor(3));
        assertEquals(Polarity.ABYSSAL, Polarity.ofFloor(-3));
    }

    @Test
    public void theLanesNeverMeet() {
        assertEquals(0, MegaGrid.laneZ(Polarity.HOLY));
        assertEquals(MegaGrid.LANE_Z, MegaGrid.laneZ(Polarity.ABYSSAL));
        // a chain of 500 regions of 19 modules plus the gap is far shorter than the lane distance
        long longest = 500L * (MegaGrid.REGION_MODULES * MegaGrid.MODULE + MegaGrid.REGION_GAP);
        assertTrue(longest * 2 < MegaGrid.LANE_Z * 2L, "two chains of the longest length stay apart");
        assertTrue(longest < MegaGrid.LANE_Z, "a chain cannot reach the other lane");
    }

    @Test
    public void theHubGatesAreFoundFromAPositionAlone() {
        assertEquals(HubLayout.GateKind.HOME, HubLayout.gateNear(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z));
        assertEquals(HubLayout.GateKind.HOLY, HubLayout.gateNear(HubLayout.HOLY_GATE_X + 2, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z - 1));
        assertEquals(HubLayout.GateKind.ABYSSAL, HubLayout.gateNear(HubLayout.ABYSS_GATE_X - 3, HubLayout.ABYSS_GATE_Y + 1, HubLayout.ABYSS_GATE_Z));
        assertNull(HubLayout.gateNear(HubLayout.PLAZA_X + 30, HubLayout.SURFACE_Y, HubLayout.PLAZA_Z + 30), "open ground belongs to no gate");
        assertNull(HubLayout.polarityOf(HubLayout.GateKind.HOME));
        assertEquals(Polarity.HOLY, HubLayout.polarityOf(HubLayout.GateKind.HOLY));
        assertEquals(Polarity.ABYSSAL, HubLayout.polarityOf(HubLayout.GateKind.ABYSSAL));
    }

    @Test
    public void theHubLayoutFitsTheDimensionAndTheLandmarksDoNotOverlap() {
        // dimension type: min_y -64, height 384 -> highest block 319
        assertTrue(HubLayout.HOLY_GATE_Y + 4 <= 319, "the Holy gate plane fits under the build limit");
        assertTrue(HubLayout.ABYSS_GATE_Y - 2 >= -64, "the Abyssal gate plane fits above the bottom");
        assertTrue(HubLayout.SURFACE_Y - HubLayout.CHASM_DEPTH >= -64, "the chasm floor is above the bottom");
        // the plaza (flattened disc plus 24 blend) must not touch the landmarks' outer rims
        int plazaReach = HubLayout.PLAZA_RADIUS + 24;
        assertTrue(HubLayout.SPIRE_X - HubLayout.SPIRE_BASE_RADIUS > plazaReach, "the Spire stands clear of the plaza");
        assertTrue(HubLayout.CHASM_X + HubLayout.CHASM_RIM_RADIUS < -plazaReach, "the Chasm lies clear of the plaza");
        assertTrue(HubLayout.SPIRE_TOP_RADIUS < HubLayout.SPIRE_BASE_RADIUS && HubLayout.CHASM_FLOOR_RADIUS < HubLayout.CHASM_RIM_RADIUS, "both taper");
        // the doorways are on the plaza side
        assertTrue(HubLayout.SPIRE_DOOR_X < HubLayout.SPIRE_X && HubLayout.CHASM_DOOR_X > HubLayout.CHASM_X, "doors face the plaza");
        assertEquals(HubLayout.SURFACE_Y + 1, HubLayout.SPAWN_Y);
    }
}
