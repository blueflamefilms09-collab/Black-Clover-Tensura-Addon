package com.newuniverse.nusmp.skilltree.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the server's staging and the client's banner stay in step only through the numbers of {@link UnlockTiming}, so the relations between them are pinned: the
 * circle's duration is chosen so that its own burst (60% of its life) falls on the burst tick the banner waits for, the banner lives three seconds, and the aura
 * outlasts the banner's first second. A change to one constant that breaks the sync fails here, not as a flash that arrives a second after the pillar.
 */
class UnlockTimingTest {
    @Test
    void theCirclesBurstFallsOnTheBurstTick() {
        assertEquals(UnlockTiming.BURST_TICK, UnlockTiming.circleBurstTick());
        assertTrue(UnlockTiming.circleTicks() * UnlockTiming.CIRCLE_CHARGE_SHARE >= UnlockTiming.BURST_TICK);
        assertTrue((UnlockTiming.circleTicks() - 1) * UnlockTiming.CIRCLE_CHARGE_SHARE < UnlockTiming.BURST_TICK, "the circle is not longer than it has to be");
    }

    @Test
    void theBannerLivesThreeSeconds() {
        assertEquals(60, UnlockTiming.BANNER_TICKS);
    }

    @Test
    void theBurstComesAfterTheStartAndBeforeTheBannerEnds() {
        assertTrue(UnlockTiming.BURST_TICK > 0);
        assertTrue(UnlockTiming.BURST_TICK < 60, "the burst is about a second, not a minute");
        assertTrue(UnlockTiming.AURA_TICKS >= 20);
    }
}
