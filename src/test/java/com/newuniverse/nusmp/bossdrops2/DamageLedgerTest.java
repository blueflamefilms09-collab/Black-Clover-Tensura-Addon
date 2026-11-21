package com.newuniverse.nusmp.bossdrops2;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why (docs/boss_drops_worthwhile.md): the damage ledger adds up hits per player, forgets a boss when it is paid, and can never grow past its caps. Pure. */
class DamageLedgerTest {
    private static UUID id(long n) { return new UUID(1L, n); }

    @Test
    void hitsAddUpPerPlayerAndPerBoss() {
        DamageLedger l = new DamageLedger();
        l.add(id(1), id(10), 5.0);
        l.add(id(1), id(10), 7.5);
        l.add(id(1), id(11), 2.0);
        l.add(id(2), id(10), 100.0);
        Map<UUID, Double> m = l.take(id(1));
        assertEquals(12.5, m.get(id(10)));
        assertEquals(2.0, m.get(id(11)));
        assertEquals(1, l.bosses(), "taking a boss forgets it");
        assertTrue(l.take(id(1)).isEmpty());
    }

    @Test
    void zeroNegativeNanAndInfiniteHitsAreIgnored() {
        DamageLedger l = new DamageLedger();
        l.add(id(1), id(10), 0.0);
        l.add(id(1), id(10), -3.0);
        l.add(id(1), id(10), Double.NaN);
        l.add(id(1), id(10), Double.POSITIVE_INFINITY);
        l.add(null, id(10), 1.0);
        l.add(id(1), null, 1.0);
        assertEquals(0, l.bosses());
        assertTrue(l.take(null).isEmpty());
    }

    @Test
    void aBossHasAtMostSixteenCountedPlayers() {
        DamageLedger l = new DamageLedger();
        for (int i = 0; i < 40; i++) l.add(id(1), id(100 + i), 1.0);
        l.add(id(1), id(100), 4.0);                                                  // a counted player keeps adding
        Map<UUID, Double> m = l.take(id(1));
        assertEquals(DamageLedger.MAX_PLAYERS, m.size());
        assertEquals(5.0, m.get(id(100)));
    }

    @Test
    void atMostSixtyFourBossesAreRememberedTheOldestIsForgottenFirst() {
        DamageLedger l = new DamageLedger();
        for (int i = 0; i < 500; i++) l.add(id(i), id(1000), 1.0);
        assertEquals(DamageLedger.MAX_BOSSES, l.bosses());
        assertTrue(l.take(id(0)).isEmpty(), "the first boss fell out");
        assertFalse(l.take(id(499)).isEmpty(), "the latest is still there");
    }

    @Test
    void aBossIsClaimedForPaymentOnlyOnceAndTheMemoryIsBounded() {
        DamageLedger l = new DamageLedger();
        assertTrue(l.claim(id(1)));
        assertFalse(l.claim(id(1)), "a second death event of the same boss pays nothing");
        assertFalse(l.claim(null));
        for (int i = 0; i < 5000; i++) l.claim(id(10 + i));
        assertTrue(l.claim(id(1)), "the oldest claim was forgotten once the memory was full (bounded, not unbounded)");
    }

    @Test
    void clearForgetsEverything() {
        DamageLedger l = new DamageLedger();
        l.add(id(1), id(2), 3.0);
        l.claim(id(1));
        l.clear();
        assertEquals(0, l.bosses());
        assertTrue(l.claim(id(1)));
    }
}
