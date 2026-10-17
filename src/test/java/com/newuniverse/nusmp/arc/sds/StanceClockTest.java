package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Diane's size changes need rules a party can learn (the opening, the growth, being cut down, no room, the Gates), so the clock that decides them is tested. */
class StanceClockTest {
    @Test
    void sheOpensAtHumanSizeAndWantsToGrowAfterTheOpening() {
        StanceClock c = new StanceClock();
        c.start(1000);
        assertEquals(StanceClock.Stance.HUMAN, c.stance());
        assertFalse(c.giant());
        assertFalse(c.wantsGrow(1000 + StanceClock.OPENING_HUMAN - 1));
        assertTrue(c.wantsGrow(1000 + StanceClock.OPENING_HUMAN));
        assertFalse(c.wantsShrink(1000 + 100000), "a human does not shrink");
        assertFalse(c.cutDown());
    }

    @Test
    void aGiantShrinksByHerselfAfterTheLongestShestaysOne() {
        StanceClock c = new StanceClock();
        c.start(0);
        c.becomeGiant(500);
        assertTrue(c.giant());
        assertFalse(c.wantsGrow(100000), "a giant does not grow");
        assertFalse(c.wantsShrink(500 + StanceClock.GIANT_MAX - 1));
        assertTrue(c.wantsShrink(500 + StanceClock.GIANT_MAX));
        assertEquals(StanceClock.GIANT_MAX, c.age(500 + StanceClock.GIANT_MAX));
    }

    @Test
    void beingCutDownGivesTheEarnedWindowAndThenShesGrowsAgain() {
        StanceClock c = new StanceClock();
        c.start(0);
        c.becomeGiant(100);
        c.becomeHuman(700, true);
        assertTrue(c.cutDown());
        assertFalse(c.wantsGrow(700 + StanceClock.CUT_DOWN_HUMAN - 1));
        assertTrue(c.wantsGrow(700 + StanceClock.CUT_DOWN_HUMAN));
        assertEquals(StanceClock.CUT_DOWN_HUMAN - 50, c.cutDownLeft(750));
        assertEquals(0, c.cutDownLeft(700 + 1000));
        assertTrue(StanceClock.CUT_DOWN_HUMAN > StanceClock.OPENING_HUMAN, "the window the party earned is longer than the opening");
        c.becomeHuman(2000, false);
        assertFalse(c.cutDown());
        assertEquals(0, c.cutDownLeft(2001));
    }

    @Test
    void aGrowthThatFindsNoRoomIsTriedAgainLater() {
        StanceClock c = new StanceClock();
        c.start(0);
        assertTrue(c.wantsGrow(200));
        c.blockGrowth(200);
        assertFalse(c.wantsGrow(200 + StanceClock.NO_ROOM_RETRY - 1));
        assertTrue(c.wantsGrow(200 + StanceClock.NO_ROOM_RETRY));
    }

    @Test
    void startingAgainForgetsEverything() {
        StanceClock c = new StanceClock();
        c.start(0);
        c.becomeGiant(10);
        c.becomeHuman(500, true);
        c.blockGrowth(500);
        c.start(9000);
        assertEquals(StanceClock.Stance.HUMAN, c.stance());
        assertFalse(c.cutDown());
        assertTrue(c.wantsGrow(9000 + StanceClock.OPENING_HUMAN));
    }

    @Test
    void theGatesMakeAHumanDianeGrowAtOnce() {
        StanceClock c = new StanceClock();
        c.start(0);
        c.blockGrowth(10);
        assertFalse(c.wantsGrow(20));
        c.growNow(20);
        assertTrue(c.wantsGrow(20));
        assertFalse(c.cutDown());
        StanceClock g = new StanceClock();
        g.start(0);
        g.becomeGiant(5);
        g.growNow(6);
        assertFalse(g.wantsGrow(6), "a giant has nothing to grow");
        assertTrue(g.giant());
    }
}
