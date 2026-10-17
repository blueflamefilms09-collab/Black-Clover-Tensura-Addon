package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a stair is a promise about two places and nobody can walk it here, so every corner of the promise is pinned on plain numbers: where each stair leads from the
 * first floor, from a middle floor and from a damaged chain index; which states of a floor are a place a player may be sent to; and exactly where a player has to stand
 * to be on the landing pad of an exit (the middle of the pad, its edge, a step beside it, a jump, a slab, the platform at the other end of the staircase).
 */
class StairLinksTest {
    @Test
    void theFirstFloorsWayBackLeadsToTheHub() {
        StairLinks.Dest d = StairLinks.back(0);
        assertTrue(d.hub());
        assertEquals(-1, d.chainIndex());
        assertEquals(StairLinks.Dest.toHub(), d);
        assertTrue(StairLinks.back(-4).hub(), "a damaged chain index still leads out, never into the void");
    }

    @Test
    void theWayBackOfALaterFloorLeadsToTheForwardLandingOfTheFloorBefore() {
        for (int k = 1; k < 40; k++) {
            StairLinks.Dest d = StairLinks.back(k);
            assertFalse(d.hub(), "floor place " + k);
            assertEquals(k - 1, d.chainIndex());
            assertTrue(d.arrivesAtForward(), "you come up out of the forward stairwell of the floor you came from");
        }
    }

    @Test
    void theWayOnLeadsToTheArrivalOfTheNextFloor() {
        for (int k = 0; k < 40; k++) {
            StairLinks.Dest d = StairLinks.forward(k);
            assertFalse(d.hub());
            assertEquals(k + 1, d.chainIndex());
            assertFalse(d.arrivesAtForward(), "you arrive on the platform of the arrival stairwell");
        }
        assertEquals(1, StairLinks.forward(-3).chainIndex(), "a damaged chain index reads as the first floor");
    }

    @Test
    void backThenOnIsTheSameFloorAgain() {
        for (int k = 1; k < 30; k++) assertEquals(k, StairLinks.forward(StairLinks.back(k).chainIndex()).chainIndex());
    }

    @Test
    void aFloorIsAPlaceUnlessItIsBeingMadeOrFailed() {
        for (String s : new String[] {"ACTIVE", "GUARDIAN_DEAD", "UNSEALED", "COMPLETE"}) assertTrue(StairLinks.playable(s), s);
        assertFalse(StairLinks.playable("GENERATING"));
        assertFalse(StairLinks.playable("FAILED"));
        assertFalse(StairLinks.playable(null));
    }

    @Test
    void thePadIsStoodOnInTheMiddleAndAtItsEdge() {
        int mx = 100, my = 57, mz = -20;
        assertTrue(StairLinks.onPad(mx + 0.5, my, mz + 0.5, mx, my, mz), "the middle");
        assertTrue(StairLinks.onPad(mx + 0.5 + 1.4, my, mz + 0.5, mx, my, mz), "the east edge of the 3 x 3 pad");
        assertTrue(StairLinks.onPad(mx + 0.5 - 1.4, my, mz + 0.5 - 1.4, mx, my, mz), "a corner");
        assertFalse(StairLinks.onPad(mx + 0.5 + 1.6, my, mz + 0.5, mx, my, mz), "a step beside the pad");
        assertFalse(StairLinks.onPad(mx + 0.5, my, mz + 0.5 - 1.6, mx, my, mz));
        assertFalse(StairLinks.onPad(mx + 0.5 + 3, my, mz + 0.5 + 3, mx, my, mz));
    }

    @Test
    void theHeightOfTheFeetDecidesWhetherYouAreOnThePadOrOnTheStaircase() {
        int mx = 0, my = 41, mz = 0;
        assertTrue(StairLinks.onPad(0.5, my, 0.5, mx, my, mz), "feet at the marker cell");
        assertTrue(StairLinks.onPad(0.5, my - 0.5, 0.5, mx, my, mz), "a slab or a step down");
        assertTrue(StairLinks.onPad(0.5, my + 1.0, 0.5, mx, my, mz), "a jump");
        assertFalse(StairLinks.onPad(0.5, my - 1.0, 0.5, mx, my, mz), "a block below is the stair before the pad, not the pad");
        assertFalse(StairLinks.onPad(0.5, my + 2.0, 0.5, mx, my, mz), "two blocks up is a flight, not a jump");
        assertFalse(StairLinks.onPad(0.5, my - 8, 0.5, mx, my, mz), "the platform of a stairwell that drops 8 blocks");
        assertFalse(StairLinks.onPad(0.5, my - 32, 0.5, mx, my, mz), "the platform at the foot of a stairwell that climbs 32");
    }

    @Test
    void aPartyThatArrivesIsNeverStandingOnAPad() {
        // an arrival is the platform at the door: same x and z may even be close to the pad, but the heights of every stairwell kind differ by at least 8
        for (int rise : new int[] {8, 28, 32}) {
            for (double dx = -2; dx <= 2; dx += 0.5) {
                assertFalse(StairLinks.onPad(0.5 + dx, 41 - rise, 0.5, 0, 41, 0), "rise " + rise + " dx " + dx);
            }
        }
    }
}
