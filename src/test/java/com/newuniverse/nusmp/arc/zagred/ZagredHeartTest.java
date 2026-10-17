package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the closed and open heart are the whole defence of the last act (closed it lets almost nothing through, open it takes extra, anti-magic always reaches it), so the factors and the windows are pinned here. */
class ZagredHeartTest {
    @Test
    void aClosedHeartLetsLessThanHalfThroughExceptAntiMagic() {
        ZagredHeart h = new ZagredHeart();
        assertFalse(h.isOpen(0));
        assertEquals(ZagredHeart.CLOSED, h.factor(0, false), 1e-6f);
        assertEquals(ZagredHeart.CLOSED_ANTI_MAGIC, h.factor(0, true), 1e-6f);
        assertTrue(ZagredHeart.CLOSED < 0.5f && ZagredHeart.CLOSED > 0f);
    }

    @Test
    void anOpenHeartTakesExtraForEveryoneAndForTheWindowOnly() {
        ZagredHeart h = new ZagredHeart();
        h.open(10, 70);
        assertTrue(h.isOpen(10));
        assertEquals(ZagredHeart.OPEN, h.factor(10, false), 1e-6f);
        assertEquals(ZagredHeart.OPEN, h.factor(10, true), 1e-6f);
        assertTrue(h.isOpen(79));
        assertFalse(h.isOpen(80));
        assertEquals(ZagredHeart.CLOSED, h.factor(80, false), 1e-6f);
        assertEquals(70, h.openLeft(10));
    }

    @Test
    void aWindowNeverShortensAnOpenOne() {
        ZagredHeart h = new ZagredHeart();
        h.open(0, 100);
        h.open(10, 20);
        assertTrue(h.isOpen(99));
        h.open(50, 100);
        assertTrue(h.isOpen(149));
        assertFalse(h.isOpen(150));
    }

    @Test
    void anAnchoredHeartOpensForLessAfterAWord() {
        assertEquals(ZagredHeart.AFTER_WORD, ZagredHeart.windowAfterWord(false));
        assertEquals(ZagredHeart.AFTER_WORD_ANCHORED, ZagredHeart.windowAfterWord(true));
        assertTrue(ZagredHeart.AFTER_WORD_ANCHORED < ZagredHeart.AFTER_WORD);
        assertTrue(ZagredHeart.AFTER_CUT > ZagredHeart.AFTER_WORD, "cutting a Word is worth more than waiting for one");
    }

    @Test
    void closeShutsItAtOnce() {
        ZagredHeart h = new ZagredHeart();
        h.open(0, 100);
        h.close();
        assertFalse(h.isOpen(1));
    }
}
