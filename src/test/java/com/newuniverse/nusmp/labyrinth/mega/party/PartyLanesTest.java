package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the lanes are what keeps two parties out of each other's floors, so the arithmetic is pinned for ALL 256 parties at once: party 0 stands exactly where the
 * dungeon always stood (an old world and {@code party.mode = shared} are untouched), no two lanes of any road or party are the same or near each other, the Holy and the
 * Abyssal lanes never meet, every lane is far enough from its neighbours for a floor and the reach of its door caps never to touch another lane, the inverse questions
 * (which party owns a z, and on which road) agree with the lanes, and the numbers the documentation quotes for the default 64 parties and for the world border are the
 * numbers the code computes.
 */
public class PartyLanesTest {
    @Test
    public void partyZeroStandsWhereTheDungeonAlwaysStood() {
        assertEquals(0, MegaGrid.laneZ(Polarity.HOLY, 0));
        assertEquals(300_000, MegaGrid.laneZ(Polarity.ABYSSAL, 0));
        assertEquals(MegaGrid.laneZ(Polarity.HOLY), MegaGrid.laneZ(Polarity.HOLY, 0));
        assertEquals(MegaGrid.laneZ(Polarity.ABYSSAL), MegaGrid.laneZ(Polarity.ABYSSAL, 0));
        assertEquals(MegaGrid.LANE_Z, MegaGrid.laneZ(Polarity.ABYSSAL, 0));
        assertEquals(0, MegaGrid.laneZ(Polarity.HOLY, -5), "a negative index is read as 0");
        assertEquals(MegaGrid.LANE_Z, MegaGrid.laneZ(Polarity.ABYSSAL, -1));
    }

    @Test
    public void sharedModeIsTheOldBehaviourForEveryPlayer() {
        // shared: whatever the book says, everyone is entered under party 0, so every lane is the old lane
        for (int bookIndex = -1; bookIndex < 70; bookIndex++) {
            int p = PartyPolicy.entryIndex(false, bookIndex);
            assertEquals(0, p);
            assertEquals(MegaGrid.laneZ(Polarity.HOLY), MegaGrid.laneZ(Polarity.HOLY, p));
            assertEquals(MegaGrid.laneZ(Polarity.ABYSSAL), MegaGrid.laneZ(Polarity.ABYSSAL, p));
        }
    }

    @Test
    public void noTwoLanesOfAnyPartyOrRoadAreTheSame() {
        Set<Integer> seen = new HashSet<>();
        for (int party = 0; party < MegaGrid.MAX_PARTIES; party++) {
            for (Polarity road : Polarity.values()) assertTrue(seen.add(MegaGrid.laneZ(road, party)), "lane of party " + party + " on the " + road.id() + " road is taken twice");
        }
        assertEquals(2 * MegaGrid.MAX_PARTIES, seen.size());
    }

    @Test
    public void neighbouringLanesAreAFullStepApartAndTheRoadsNeverMeet() {
        for (int party = 1; party < MegaGrid.MAX_PARTIES; party++) {
            for (Polarity road : Polarity.values()) {
                int gap = Math.abs(MegaGrid.laneZ(road, party) - MegaGrid.laneZ(road, party - 1));
                assertEquals(MegaGrid.PARTY_STEP, gap, "party " + party + " on the " + road.id() + " road");
            }
        }
        int holyHighest = MegaGrid.laneZ(Polarity.HOLY, 0);
        int abyssalLowest = MegaGrid.laneZ(Polarity.ABYSSAL, 0);
        assertTrue(abyssalLowest - holyHighest >= MegaGrid.LANE_Z, "the Holy lanes run away from the Abyssal ones");
        for (int a = 0; a < MegaGrid.MAX_PARTIES; a++) {
            assertTrue(MegaGrid.laneZ(Polarity.HOLY, a) <= holyHighest);
            assertTrue(MegaGrid.laneZ(Polarity.ABYSSAL, a) >= abyssalLowest);
        }
    }

    @Test
    public void theReachOfAFloorNeverTouchesTheNextLane() {
        // a floor reaches REACH blocks from its lane (half its largest region, a door cap and the gap): two lanes are PARTY_STEP apart, so even two floors that reach
        // towards each other leave thousands of blocks free, and a player can see at most 512 blocks
        assertTrue(PartyLanes.REACH > MegaGrid.blocks(MegaGrid.REGION_MODULES) / 2);
        assertTrue(MegaGrid.PARTY_STEP - 2 * PartyLanes.REACH > 3000, "free space between the reach of two neighbouring lanes");
        assertTrue(MegaGrid.PARTY_STEP >= 8 * 32 * 16, "at least eight render distances of 32 chunks (512 blocks) between two lanes");
    }

    @Test
    public void theInverseAgreesWithTheLanes() {
        for (int party = 0; party < MegaGrid.MAX_PARTIES; party++) {
            for (Polarity road : Polarity.values()) {
                int z = MegaGrid.laneZ(road, party);
                assertEquals(road, PartyLanes.roadAt(z));
                assertEquals(party, PartyLanes.partyAt(z, MegaGrid.MAX_PARTIES), "party of the lane z " + z);
                assertEquals(party, PartyLanes.partyAt(z + PartyLanes.REACH, MegaGrid.MAX_PARTIES), "a floor's reach still belongs to its lane");
                assertEquals(party, PartyLanes.partyAt(z - PartyLanes.REACH, MegaGrid.MAX_PARTIES));
                assertEquals(party, PartyLanes.partyAt(z + MegaGrid.PARTY_STEP / 2 - 1, MegaGrid.MAX_PARTIES), "the nearest lane owns a z, up to half a step");
            }
        }
    }

    @Test
    public void aZBeyondTheLastLaneBelongsToNobody() {
        assertEquals(-1, PartyLanes.partyAt(MegaGrid.laneZ(Polarity.HOLY, 64), 64), "party 64 does not exist when 64 parties are allowed");
        assertEquals(63, PartyLanes.partyAt(MegaGrid.laneZ(Polarity.HOLY, 63), 64));
        assertEquals(-1, PartyLanes.partyAt(MegaGrid.laneZ(Polarity.ABYSSAL, 64), 64));
        assertEquals(-1, PartyLanes.partyAt(150_000, 64), "the empty middle between the roads");
        assertEquals(-1, PartyLanes.partyAt(5_000, 64), "above the Holy lane of party 0");
        assertEquals(0, PartyLanes.partyAt(1_000, 64), "a floor of party 0 reaching a little to the positive side");
        assertEquals(-1, PartyLanes.partyAt(-10_000_000, 64));
    }

    @Test
    public void aRegionInsideItsPartysBandIsSafeAndOneThatWanderedIsNot() {
        for (int party : new int[] {0, 1, 7, 255}) {
            for (Polarity road : Polarity.values()) {
                int lane = MegaGrid.laneZ(road, party);
                assertTrue(PartyLanes.insideBand(lane - PartyLanes.REACH, lane + PartyLanes.REACH, party), "a floor of the floor model is always inside its band");
                assertTrue(PartyLanes.insideBand(lane - 2000, lane + 2000, party), "a legacy chain that wandered 2000 blocks is still inside");
                assertFalse(PartyLanes.insideBand(lane - 2000, lane + 2100, party), "past half a step it is nearer to the next lane");
                assertFalse(PartyLanes.insideBand(lane, lane + MegaGrid.PARTY_STEP, party), "a chain that reached the next lane is not inside");
                assertFalse(PartyLanes.insideBand(lane - 100, lane + 100, (party + 1) % MegaGrid.MAX_PARTIES), "a region is not inside another party's band");
            }
        }
    }

    @Test
    public void theNumbersTheDocumentationQuotesAreTheNumbersComputed() {
        assertEquals(558_048, PartyLanes.maxZ(64));
        assertEquals(-258_048, PartyLanes.minZ(64));
        assertEquals(300_000, PartyLanes.maxZ(1));
        assertEquals(0, PartyLanes.minZ(1));
        assertEquals(1_344_480, PartyLanes.maxZ(MegaGrid.MAX_PARTIES));
        assertEquals(-1_044_480, PartyLanes.minZ(MegaGrid.MAX_PARTIES));
        assertEquals(558_048 + PartyLanes.REACH, PartyLanes.farthest(64));
        assertEquals(PartyLanes.maxZ(MegaGrid.MAX_PARTIES), PartyLanes.maxZ(5000), "more than the maths knows is read as the most it knows");
        assertEquals(PartyLanes.maxZ(1), PartyLanes.maxZ(0), "no party at all is read as one");
    }

    @Test
    public void theWorldBorderBoundsTheParties() {
        assertTrue(PartyLanes.fits(MegaGrid.MAX_PARTIES, 29_999_984.0), "even 256 parties fit the vanilla border");
        assertFalse(PartyLanes.fits(64, 10_000.0), "a border of 10 000 blocks holds no Abyssal lane at all");
        assertEquals(0, PartyLanes.partiesThatFit(10_000.0));
        assertEquals(MegaGrid.MAX_PARTIES, PartyLanes.partiesThatFit(29_999_984.0));
        assertEquals(171, PartyLanes.partiesThatFit(1_000_000.0));
        assertTrue(PartyLanes.fits(171, 1_000_000.0));
        assertFalse(PartyLanes.fits(172, 1_000_000.0));
        // monotone: more parties never fit where fewer do not
        boolean stopped = false;
        for (int n = 1; n <= MegaGrid.MAX_PARTIES; n++) {
            boolean f = PartyLanes.fits(n, 700_000.0);
            if (!f) stopped = true;
            assertFalse(stopped && f, "fits() must not come back after it stopped fitting (n=" + n + ")");
        }
    }
}
