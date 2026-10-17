package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: holding an arrow key in edit mode must move the window steadily, a tap must move it exactly once, and letting go must stop it at once. The key mapping gives
 * only "clicked since the last look" and "held now", so the timing is counted by {@link KeyRepeat} and pinned here: one step on the press, a pause of
 * {@link KeyRepeat#DELAY} ticks, then a step every tick; a tap between two ticks still counts; two taps between ticks are two steps; release resets.
 */
class KeyRepeatTest {
    @Test void aTapIsExactlyOneStep() {
        KeyRepeat r = new KeyRepeat();
        assertEquals(1, r.step(true, true));
        assertEquals(0, r.step(false, false), "released: nothing more");
        assertEquals(0, r.step(false, false));
    }

    @Test void aTapThatCameAndWentBetweenTwoTicksStillCounts() {
        KeyRepeat r = new KeyRepeat();
        assertEquals(1, r.step(true, false));
        assertEquals(0, r.held());
    }

    @Test void aHeldKeyPausesThenRepeatsEveryTick() {
        KeyRepeat r = new KeyRepeat();
        int total = r.step(true, true);
        assertEquals(1, total);
        for (int tick = 2; tick <= KeyRepeat.DELAY; tick++) assertEquals(0, r.step(false, true), "still waiting at tick " + tick);
        for (int tick = KeyRepeat.DELAY + 1; tick <= KeyRepeat.DELAY + 20; tick++) assertEquals(1, r.step(false, true), "repeating at tick " + tick);
    }

    @Test void releasingResetsTheWait() {
        KeyRepeat r = new KeyRepeat();
        r.step(true, true);
        for (int i = 0; i < 30; i++) r.step(false, true);
        assertEquals(0, r.step(false, false));
        assertEquals(1, r.step(true, true), "a new press is one step again");
        assertEquals(0, r.step(false, true), "and waits again");
    }

    @Test void aSecondTapDuringTheWaitIsAStepOfItsOwn() {
        KeyRepeat r = new KeyRepeat();
        r.step(true, true);
        assertEquals(1, r.step(true, true));
        assertEquals(0, r.step(false, true));
    }

    @Test void aTapDuringTheRepeatDoesNotDoubleTheStep() {
        KeyRepeat r = new KeyRepeat();
        r.step(true, true);
        for (int i = 0; i < KeyRepeat.DELAY + 3; i++) r.step(false, true);
        assertEquals(1, r.step(true, true));
    }

    @Test void resetForgetsTheHold() {
        KeyRepeat r = new KeyRepeat();
        r.step(true, true);
        for (int i = 0; i < 15; i++) r.step(false, true);
        r.reset();
        assertEquals(0, r.held());
        assertEquals(1, r.step(false, true), "a key still held after a reset starts over with a step");
    }
}
