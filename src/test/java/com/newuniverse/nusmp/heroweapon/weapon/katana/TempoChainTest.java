package com.newuniverse.nusmp.heroweapon.weapon.katana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Why: the tempo is the Katana's rhythm, so its rules are pinned: the first cast of a chain has none, a different technique within four seconds raises it by
 * one, the same technique again does not, three is the top, a pause of more than four seconds lapses it, a parry jumps it to full, and spending resets it.
 */
class TempoChainTest {
    @Test
    void differentTechniquesBuildTempoAndTheSameOneDoesNot() {
        TempoChain t = new TempoChain();
        assertEquals(0, t.record("a", 100), "the first cast of a chain");
        assertEquals(1, t.record("b", 110), "a different technique");
        assertEquals(1, t.record("b", 120), "the same technique again");
        assertEquals(2, t.record("c", 130));
        assertEquals(3, t.record("a", 140));
        assertEquals(3, t.record("b", 150), "three is the top");
        assertEquals(3, t.level(150));
    }

    @Test
    void aPauseLapsesTheChain() {
        TempoChain t = new TempoChain();
        t.record("a", 0);
        t.record("b", 10);
        assertEquals(1, t.level(10 + KatanaNumbers.TEMPO_WINDOW), "still alive on the last tick of the window");
        assertEquals(0, t.level(10 + KatanaNumbers.TEMPO_WINDOW + 1), "lapsed one tick later");
        assertEquals(0, t.record("c", 10 + KatanaNumbers.TEMPO_WINDOW + 5), "a cast after the window starts a fresh chain");
        assertEquals(1, t.record("a", 10 + KatanaNumbers.TEMPO_WINDOW + 8));
    }

    @Test
    void aPerfectParryJumpsToFullAndSpendingResets() {
        TempoChain t = new TempoChain();
        t.perfect(500);
        assertEquals(KatanaNumbers.TEMPO_MAX, t.level(500));
        assertEquals(KatanaNumbers.TEMPO_MAX, t.record("instant", 510), "the next technique is cast at full tempo");
        assertEquals(0, t.level(500 + KatanaNumbers.TEMPO_WINDOW + 40), "and it lapses like any other");
        t.perfect(900);
        t.reset();
        assertEquals(0, t.level(901));
        assertEquals(0, t.record("a", 902), "after a reset the next cast starts a chain");
    }
}
