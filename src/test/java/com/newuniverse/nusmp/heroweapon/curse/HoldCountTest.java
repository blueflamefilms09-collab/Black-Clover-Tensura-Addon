package com.newuniverse.nusmp.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Why: the third hero key accepts an offer only after three seconds of Sneak and the key held, seen at looks ten ticks apart; an accidental press must never accept a
 * curse. These pin that the count needs the right number of looks in a row, that one look without the gesture starts it over, that a hold that goes on does not accept
 * twice, that players are counted apart, and that the looks the code asks for really span three seconds.
 */
class HoldCountTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    @Test
    void theHoldIsAcceptedOnTheSeventhLookInARow() {
        HoldCount c = new HoldCount();
        for (int look = 1; look < HoldCount.LOOKS; look++) {
            assertFalse(c.step(A, true, HoldCount.LOOKS), "look " + look + " is too early");
            assertEquals(look, c.count(A));
        }
        assertTrue(c.step(A, true, HoldCount.LOOKS), "the seventh look completes the hold");
        assertEquals(0, c.count(A), "the count starts over");
    }

    @Test
    void oneLookWithoutTheGestureStartsTheCountOver() {
        HoldCount c = new HoldCount();
        for (int i = 0; i < 6; i++) assertFalse(c.step(A, true, 7));
        assertFalse(c.step(A, false, 7));
        assertEquals(0, c.count(A));
        for (int i = 0; i < 6; i++) assertFalse(c.step(A, true, 7), "six more are not enough after the break");
        assertTrue(c.step(A, true, 7));
    }

    @Test
    void aHoldThatGoesOnDoesNotAcceptTwiceInARow() {
        HoldCount c = new HoldCount();
        int accepted = 0;
        for (int look = 0; look < 13; look++) if (c.step(A, true, 7)) accepted++;
        assertEquals(1, accepted, "thirteen looks hold the gesture once complete and six more: one acceptance");
        assertTrue(c.step(A, true, 7), "the fourteenth look would complete a second hold");
    }

    @Test
    void playersAreCountedApartAndForgottenOnRequest() {
        HoldCount c = new HoldCount();
        for (int i = 0; i < 5; i++) {
            c.step(A, true, 7);
            if (i < 2) c.step(B, true, 7);
        }
        assertEquals(5, c.count(A));
        assertEquals(2, c.count(B));
        c.forget(A);
        assertEquals(0, c.count(A));
        assertEquals(2, c.count(B));
        c.clear();
        assertEquals(0, c.count(B));
        assertFalse(c.step(null, false, 7), "a missing player with no gesture is a quiet no");
    }

    @Test
    void anAbsurdNeedStillMeansAtLeastOneLookAndTheLooksMakeThreeSeconds() {
        HoldCount c = new HoldCount();
        assertTrue(c.step(A, true, 0), "a need of zero or less is one look");
        assertTrue(c.step(A, true, -9));
        assertEquals(3 * 20, (HoldCount.LOOKS - 1) * HoldCount.LOOK_TICKS, "the first look and the last are sixty ticks, three seconds, apart");
    }
}
