package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Revchi's Magic Binding: who is bound, how long, and how a foe tears the chain. */
class BindLedgerTest {
    private final UUID a = new UUID(0, 1), b = new UUID(0, 2);

    @Test
    void aBindRunsItsTimeAndThenFallsAway() {
        BindLedger l = new BindLedger(2, 15);
        assertFalse(l.bound(a, 0));
        assertTrue(l.bind(a, 100, 70));
        assertTrue(l.bound(a, 100));
        assertTrue(l.bound(a, 169));
        assertEquals(170, l.until(a));
        assertFalse(l.bound(a, 170), "ran out");
        assertEquals(0, l.size(), "an expired bind is forgotten when asked about");
        assertFalse(l.bind(a, 0, 0), "no time, no bind");
        assertFalse(l.bind(null, 0, 10));
    }

    @Test
    void aSecondChainNeverExtendsTheFirst() {
        BindLedger l = new BindLedger(2, 15);
        assertTrue(l.bind(a, 0, 60));
        assertFalse(l.bind(a, 30, 60), "still bound: no chain-lock");
        assertEquals(60, l.until(a));
        assertTrue(l.bind(b, 30, 60), "someone else can be bound");
        assertTrue(l.bind(a, 60, 60), "once it has run out, a new one may come");
    }

    @Test
    void blowsTearLinksAndTheLastOneFreesThem() {
        BindLedger l = new BindLedger(2, 15);
        l.bind(a, 0, 70);
        assertEquals(2, l.linksLeft(a));
        assertEquals(BindLedger.Struggle.NONE, l.struggle(a, 5, 6), "a small blow gets partway");
        assertEquals(BindLedger.Struggle.LINK, l.struggle(a, 6, 10), "enough for one link");
        assertEquals(1, l.linksLeft(a));
        assertEquals(BindLedger.Struggle.FREE, l.struggle(a, 7, 40), "a big blow cuts the rest in one go");
        assertFalse(l.bound(a, 8));
        assertEquals(0, l.linksLeft(a));
    }

    @Test
    void aFoeWhoIsNotBoundDoesNotStruggle() {
        BindLedger l = new BindLedger(2, 15);
        assertEquals(BindLedger.Struggle.NONE, l.struggle(a, 0, 500));
        l.bind(b, 0, 70);
        assertEquals(BindLedger.Struggle.NONE, l.struggle(a, 1, 500), "another foe's blows do nothing for this one");
        assertEquals(BindLedger.Struggle.NONE, l.struggle(b, 1, 0), "a blow of no size");
        assertEquals(BindLedger.Struggle.NONE, l.struggle(b, 80, 500), "after the bind ran out");
    }

    @Test
    void antiMagicCutsTheWholeChain() {
        BindLedger l = new BindLedger(3, 100);
        l.bind(a, 0, 70);
        assertTrue(l.free(a));
        assertFalse(l.bound(a, 1));
        assertFalse(l.free(a), "nothing left to cut");
        l.bind(a, 2, 70);
        l.bind(b, 2, 70);
        l.clear();
        assertEquals(0, l.size());
    }

    @Test
    void theLinkCostScalesWithHisHealth() {
        assertEquals(15.0, BindLedger.linkDamage(750, 0.02, 10), 1e-9);
        assertEquals(10.0, BindLedger.linkDamage(100, 0.02, 10), 1e-9, "never under the floor");
        assertEquals(26.0, BindLedger.linkDamage(1300, 0.02, 10), 1e-9);
    }
}
