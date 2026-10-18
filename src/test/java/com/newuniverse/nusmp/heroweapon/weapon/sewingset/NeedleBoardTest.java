package com.newuniverse.nusmp.heroweapon.weapon.sewingset;

import com.newuniverse.nusmp.heroweapon.weapon.sewingset.NeedleBoard.Side;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Scissor Cut, the Jumping Needle, the Stitch Mend and Ariadne's Thread all use the needles, so the marks and the planted needle are proved exactly: expiry, the size limit, using a mark up, and the two sides kept apart. */
class NeedleBoardTest {
    private static UUID id(int n) { return UUID.nameUUIDFromBytes(("m" + n).getBytes()); }

    @Test
    void aMarkHoldsForItsLifeThenFallsOut() {
        NeedleBoard b = new NeedleBoard();
        b.mark(id(1), Side.FOE, 100, 160);
        assertTrue(b.has(id(1), 100));
        assertTrue(b.has(id(1), 259));
        assertFalse(b.has(id(1), 260));
        assertEquals(0, b.size(260));
    }

    @Test
    void markingAgainRefreshesTheMarkInsteadOfDoublingIt() {
        NeedleBoard b = new NeedleBoard();
        b.mark(id(1), Side.FOE, 0, 100);
        b.mark(id(1), Side.FOE, 90, 100);
        assertTrue(b.has(id(1), 150));
        assertEquals(1, b.size(150));
    }

    @Test
    void theNinthMarkPushesTheOldestOut() {
        NeedleBoard b = new NeedleBoard();
        for (int i = 0; i < 9; i++) b.mark(id(i), Side.FOE, i, 1000);
        assertEquals(NeedleBoard.MAX_MARKS, b.size(10));
        assertFalse(b.has(id(0), 10), "the first one fell out");
        assertTrue(b.has(id(1), 10));
        assertTrue(b.has(id(8), 10));
    }

    @Test
    void usingAMarkUpReportsItOnceOnly() {
        NeedleBoard b = new NeedleBoard();
        b.mark(id(1), Side.FOE, 0, 100);
        assertTrue(b.consume(id(1), 10));
        assertFalse(b.consume(id(1), 10), "it is gone");
        assertFalse(b.consume(id(2), 10), "an unmarked creature reports nothing");
        b.mark(id(3), Side.FOE, 0, 10);
        assertFalse(b.consume(id(3), 50), "a mark that already fell out does not count");
    }

    @Test
    void theTwoSidesAreListedApartOldestFirstAndOnlyWhileLive() {
        NeedleBoard b = new NeedleBoard();
        b.mark(id(1), Side.ALLY, 0, 400);
        b.mark(id(2), Side.FOE, 1, 160);
        b.mark(id(3), Side.ALLY, 2, 400);
        b.mark(id(4), Side.ALLY, 3, 20);
        assertEquals(List.of(id(1), id(3), id(4)), b.ids(Side.ALLY, 10));
        assertEquals(List.of(id(1), id(3)), b.ids(Side.ALLY, 100), "id 4 fell out at 23");
        assertEquals(List.of(id(2)), b.ids(Side.FOE, 100));
        assertEquals(List.of(), b.ids(Side.FOE, 500));
    }

    @Test
    void aPlantedNeedleLivesItsLifeAndIsReplacedByTheNextOne() {
        NeedleBoard b = new NeedleBoard();
        assertFalse(b.placeLive(0));
        b.plant(1, 2, 3, 100, 400);
        assertTrue(b.placeLive(499));
        assertFalse(b.placeLive(500));
        assertEquals(1.0, b.placeX(), 1e-9);
        b.plant(7, 8, 9, 200, 400);
        assertEquals(7.0, b.placeX(), 1e-9);
        assertEquals(9.0, b.placeZ(), 1e-9);
        b.clearPlace();
        assertFalse(b.placeLive(201));
    }

    @Test
    void clearForgetsMarksAndPlace() {
        NeedleBoard b = new NeedleBoard();
        b.mark(id(1), Side.ALLY, 0, 100);
        b.plant(0, 0, 0, 0, 100);
        b.clear();
        assertEquals(0, b.size(1));
        assertFalse(b.placeLive(1));
    }

    @Test
    void theDrawingBeatFiresOnceEveryForty() {
        NeedleBoard b = new NeedleBoard();
        assertTrue(b.drawDue(1000, 40), "the first call is due");
        assertFalse(b.drawDue(1010, 40));
        assertFalse(b.drawDue(1039, 40));
        assertTrue(b.drawDue(1040, 40));
        assertFalse(b.drawDue(1041, 40));
    }
}
