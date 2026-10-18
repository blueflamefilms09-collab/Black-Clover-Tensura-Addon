package com.newuniverse.nusmp.heroweapon.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: the Curse Series asks for sneak plus the third hero key held for three seconds, so "how long has this key been down" must be right: it counts from the
 * press, it survives heartbeats, it ends on release, and a key whose release packet was lost expires on its own instead of staying held for ever.
 */
class KeyHoldTest {
    private static final UUID P = UUID.nameUUIDFromBytes("p".getBytes()), Q = UUID.nameUUIDFromBytes("q".getBytes());

    @Test
    void aHeldKeyCountsFromThePress() {
        KeyHold k = new KeyHold();
        assertEquals(0, k.heldTicks(P, 2, 100));
        k.press(P, 2, 100);
        assertTrue(k.isHeld(P, 2, 100));
        assertEquals(0, k.heldTicks(P, 2, 100), "pressed this very tick");
        k.beat(P, 2, 110);
        k.beat(P, 2, 120);
        assertEquals(30, k.heldTicks(P, 2, 130));
        assertTrue(k.heldTicks(P, 2, 160) == 0, "no heartbeat for 40 ticks: expired");
    }

    @Test
    void releaseEndsTheHoldAndKeysAreIndependent() {
        KeyHold k = new KeyHold();
        k.press(P, 0, 10);
        k.press(P, 2, 12);
        k.release(P, 0);
        assertFalse(k.isHeld(P, 0, 15));
        assertTrue(k.isHeld(P, 2, 15));
        assertFalse(k.isHeld(Q, 2, 15), "another player");
        assertFalse(k.isHeld(P, 1, 15), "a key never pressed");
    }

    @Test
    void aHeartbeatWithoutAPressStartsTheHoldAtOnce() {
        KeyHold k = new KeyHold();
        k.beat(P, 1, 50);
        assertTrue(k.isHeld(P, 1, 50));
        assertEquals(10, k.heldTicks(P, 1, 60));
    }

    @Test
    void pressingAgainRestartsTheClock() {
        KeyHold k = new KeyHold();
        k.press(P, 2, 0);
        k.beat(P, 2, 10);
        k.release(P, 2);
        k.press(P, 2, 100);
        assertEquals(5, k.heldTicks(P, 2, 105));
    }

    @Test
    void badSlotsAreIgnored() {
        KeyHold k = new KeyHold();
        k.press(P, -1, 0);
        k.press(P, 3, 0);
        k.beat(P, 7, 0);
        assertFalse(k.isHeld(P, -1, 0));
        assertFalse(k.isHeld(P, 3, 0));
        assertEquals(0, k.heldTicks(P, 7, 0));
    }

    @Test
    void forgetAndClearDropEverything() {
        KeyHold k = new KeyHold();
        k.press(P, 0, 0);
        k.press(Q, 0, 0);
        k.forget(P);
        assertFalse(k.isHeld(P, 0, 1));
        assertTrue(k.isHeld(Q, 0, 1));
        k.clear();
        assertFalse(k.isHeld(Q, 0, 1));
    }

    @Test
    void aClockSetBackExpiresTheHold() {
        KeyHold k = new KeyHold();
        k.press(P, 0, 5000);
        assertFalse(k.isHeld(P, 0, 100));
    }
}
