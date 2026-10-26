package com.newuniverse.nusmp.skilltree.line;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (3.26.0, the Evolution skills): the Debt ledger is the memory of a mechanic that runs on a public server, so its bounds are tested without a game: the share stored, the cap of a share of the foe's maximum
 * health, at most 20 foes per player (the oldest is pushed out), the expiry after the hold time, what a tick may take and how often, consuming, the burst's own cap, the choice of who a burst reaches, and every way
 * out (a foe died, the owner left or died, the server stopped) in a world with two owners that must never see each other's Debt.
 */
class DebtLedgerTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a"), B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static UUID foe(int i) { return new UUID(0xF0E0L, i); }

    @Test
    void aBlowStoresItsShareAndTheDebtIsHeldAtACapOfTheFoesHealth() {
        DebtLedger l = new DebtLedger();
        assertEquals(5.0, l.store(A, foe(1), 10, 0.5, 0.5, 20, 100, 200, 0), 1e-9);
        assertEquals(5.0, l.debtOf(A, foe(1), 100), 1e-9);
        assertEquals(5.0, l.store(A, foe(1), 100, 0.5, 0.5, 20, 101, 200, 0), 1e-9, "50 of damage would store 50, the room under the cap of 10 is 5");
        assertEquals(10.0, l.debtOf(A, foe(1), 101), 1e-9);
        assertEquals(0.0, l.store(A, foe(1), 100, 0.5, 0.5, 20, 102, 200, 0), 1e-9, "at the cap nothing more is stored");
        assertEquals(10.0, l.debtOf(A, foe(1), 102), 1e-9);
        assertEquals(1, l.size(A));
    }

    @Test
    void nonsenseInputsStoreNothingAndNeverThrow() {
        DebtLedger l = new DebtLedger();
        assertEquals(0.0, l.store(null, foe(1), 10, 0.5, 0.5, 20, 0, 100, 0));
        assertEquals(0.0, l.store(A, null, 10, 0.5, 0.5, 20, 0, 100, 0));
        assertEquals(0.0, l.store(A, foe(1), -10, 0.5, 0.5, 20, 0, 100, 0));
        assertEquals(0.0, l.store(A, foe(1), Double.NaN, 0.5, 0.5, 20, 0, 100, 0));
        assertEquals(0.0, l.store(A, foe(1), 10, 0.0, 0.5, 20, 0, 100, 0));
        assertEquals(0.0, l.store(A, foe(1), 10, Double.POSITIVE_INFINITY, 0.5, 20, 0, 100, 0));
        assertEquals(0.0, l.store(A, foe(1), 10, 0.5, 0.5, 0, 0, 100, 0), "a foe with no health");
        assertEquals(0.0, l.store(A, foe(1), 10, 0.5, 0.0, 20, 0, 100, 0), "a cap of nothing");
        assertEquals(10.0 * 1.0, l.store(A, foe(2), 100, 1.0, 5.0, 10, 0, 100, 0), 1e-9, "a cap above the foe's whole health is held at the whole health");
        assertTrue(l.size(A) == 1 && l.totalEntries() == 1, "only the one sound store left an entry");
    }

    @Test
    void atMostTwentyFoesAreTrackedForOnePlayerAndTheOldestIsPushedOut() {
        DebtLedger l = new DebtLedger();
        for (int i = 0; i < 30; i++) l.store(A, foe(i), 10, 0.5, 0.5, 20, i, 1000, 0);
        assertEquals(DebtLedger.MAX_FOES, l.size(A));
        assertEquals(20, DebtLedger.MAX_FOES);
        assertFalse(l.has(A, foe(0), 29), "the first foes were pushed out");
        assertTrue(l.has(A, foe(29), 29), "the newest is kept");
        for (int i = 0; i < 25; i++) l.store(B, foe(100 + i), 10, 0.5, 0.5, 20, i, 1000, 0);
        assertEquals(20, l.size(B), "another player has a ledger of their own");
        assertEquals(20, l.size(A), "and does not push the first one's foes out");
        assertEquals(40, l.totalEntries());
    }

    @Test
    void anEntryExpiresAfterTheHoldTimeAndEveryNewBlowRenewsIt() {
        DebtLedger l = new DebtLedger();
        l.store(A, foe(1), 10, 0.5, 0.5, 20, 0, 100, 0);
        assertTrue(l.has(A, foe(1), 99));
        assertFalse(l.has(A, foe(1), 100), "gone at the hold time");
        assertEquals(0.0, l.debtOf(A, foe(1), 5000), 1e-9);
        l.store(A, foe(2), 10, 0.5, 0.5, 20, 0, 100, 0);
        l.store(A, foe(2), 1, 0.5, 0.5, 20, 90, 100, 0);
        assertTrue(l.has(A, foe(2), 150), "a new blow renewed it");
        assertEquals(1, l.prune(A, 150));
        assertEquals(0, l.prune(A, 190), "pruned to nothing, and the owner is forgotten");
        assertFalse(l.hasOwner(A));
        assertEquals(0, l.prune(B, 0), "pruning an owner with nothing is harmless");
    }

    @Test
    void consumingTakesAllTheDebtOnceAndAnExpiredDebtIsWorthNothing() {
        DebtLedger l = new DebtLedger();
        l.store(A, foe(1), 10, 0.5, 0.5, 20, 0, 100, 0);
        assertEquals(5.0, l.consume(A, foe(1), 50), 1e-9);
        assertEquals(0.0, l.consume(A, foe(1), 50), 1e-9, "taken once");
        assertFalse(l.hasOwner(A), "the owner with nothing left is forgotten");
        l.store(A, foe(2), 10, 0.5, 0.5, 20, 0, 100, 0);
        assertEquals(0.0, l.consume(A, foe(2), 100), 1e-9, "an expired Debt is not worth anything");
        assertEquals(0.0, l.consume(B, foe(2), 0), 1e-9, "another player's Debt is not yours");
    }

    @Test
    void aTickTakesAQuarterAtMostFivePercentOfTheFoesHealthAndNeverMoreThanOnceASecond() {
        DebtLedger l = new DebtLedger();
        l.store(A, foe(1), 400, 1.0, 1.0, 400, 0, 1000, 5);                  // 400 owed on a foe of 400 health; a tick asked for every 5 ticks is held to every 20
        assertTrue(l.due(A, 10, 4).isEmpty(), "not due yet (the interval is never below a second)");
        List<DebtLedger.Tick> t = l.due(A, 20, 4);
        assertEquals(1, t.size());
        assertEquals(20.0, t.get(0).amount(), 1e-9, "a quarter is 100 but 5 % of 400 is 20");
        assertEquals(380.0, l.debtOf(A, foe(1), 20), 1e-9);
        assertTrue(l.due(A, 39, 4).isEmpty(), "once a second at most");
        assertEquals(1, l.due(A, 40, 4).size());
        DebtLedger small = new DebtLedger();
        small.store(A, foe(2), 10, 1.0, 1.0, 400, 0, 1000, 20);
        assertEquals(2.5, small.due(A, 20, 4).get(0).amount(), 1e-9, "a quarter of 10 owed");
        DebtLedger none = new DebtLedger();
        none.store(A, foe(3), 10, 1.0, 1.0, 400, 0, 1000, 0);
        assertTrue(none.due(A, 100000, 4).isEmpty(), "a Debt that does not tick never does");
    }

    @Test
    void aTickRespectsTheCallersBudgetAndASpentDebtLeavesTheLedger() {
        DebtLedger l = new DebtLedger();
        for (int i = 0; i < 10; i++) l.store(A, foe(i), 40, 1.0, 1.0, 40, 0, 1000, 20);
        assertEquals(4, l.due(A, 20, 4).size(), "at most the budget");
        assertEquals(4, l.due(A, 20, 4).size(), "the others are still due, the next beat takes them");
        assertEquals(2, l.due(A, 20, 4).size());
        assertTrue(l.due(A, 20, 4).isEmpty());
        assertTrue(l.due(A, 20, 0).isEmpty(), "a budget of nothing");
        DebtLedger tiny = new DebtLedger();
        tiny.store(A, foe(1), 0.012, 1.0, 1.0, 100, 0, 1000, 20);
        assertEquals(1, tiny.due(A, 20, 4).size());
        tiny.due(A, 40, 4);
        tiny.due(A, 60, 4);
        tiny.due(A, 80, 4);
        assertTrue(tiny.isEmpty() || tiny.debtOf(A, foe(1), 80) < 0.012, "a Debt that dwindles below the minimum is dropped");
    }

    @Test
    void theBurstExtraIsAMultipleOfTheDebtButNeverMoreThanTheFoesHealth() {
        assertEquals(15.0, DebtLedger.burstExtra(10, 1.5, 100, false), 1e-9);
        assertEquals(100.0, DebtLedger.burstExtra(90, 5.0, 100, false), 1e-9, "never more than the whole health");
        assertEquals(25.0, DebtLedger.burstExtra(90, 5.0, 100, true), 1e-9, "a boss takes at most a quarter");
        assertEquals(0.0, DebtLedger.burstExtra(0, 5.0, 100, false));
        assertEquals(0.0, DebtLedger.burstExtra(10, 0, 100, false));
        assertEquals(0.0, DebtLedger.burstExtra(Double.NaN, 5, 100, false));
        assertEquals(0.0, DebtLedger.burstExtra(10, 5, Double.POSITIVE_INFINITY, false));
        assertEquals(0.0, DebtLedger.burstExtra(10, 5, 0, false));
    }

    @Test
    void aBurstReachesFoesThatCarryDebtFirstThenTheNearest() {
        List<String> nearestFirst = List.of("a", "b", "c", "d", "e");
        List<String> picked = DebtLedger.prefer(nearestFirst, s -> s.equals("d") || s.equals("e"), 3);
        assertEquals(List.of("d", "e", "a"), picked);
        assertEquals(List.of("d"), DebtLedger.prefer(nearestFirst, s -> s.equals("d") || s.equals("e"), 1));
        assertEquals(List.of("a", "b"), DebtLedger.prefer(nearestFirst, s -> false, 2));
        assertTrue(DebtLedger.prefer(nearestFirst, s -> true, 0).isEmpty());
        assertTrue(DebtLedger.prefer(List.<String>of(), s -> true, 3).isEmpty());
        assertEquals(5, DebtLedger.prefer(nearestFirst, s -> false, 50).size(), "no more than there are");
    }

    @Test
    void everyWayOutForgetsAndTwoOwnersNeverSeeEachOthersDebt() {
        DebtLedger l = new DebtLedger();
        l.store(A, foe(1), 10, 0.5, 0.5, 20, 0, 1000, 0);
        l.store(A, foe(2), 10, 0.5, 0.5, 20, 0, 1000, 0);
        l.store(B, foe(1), 10, 0.5, 0.5, 20, 0, 1000, 0);
        assertEquals(5.0, l.debtOf(B, foe(1), 0), 1e-9);
        assertEquals(0.0, l.debtOf(B, foe(2), 0), 1e-9);
        l.forgetFoe(foe(1));                                              // a foe died: nobody owes through it
        assertFalse(l.has(A, foe(1), 0) || l.has(B, foe(1), 0));
        assertTrue(l.has(A, foe(2), 0));
        assertFalse(l.hasOwner(B), "an owner with nothing left is forgotten");
        l.forget(A, foe(2));                                              // the foe left the owner's world
        assertFalse(l.hasOwner(A));
        l.store(A, foe(3), 10, 0.5, 0.5, 20, 0, 1000, 0);
        l.store(B, foe(3), 10, 0.5, 0.5, 20, 0, 1000, 0);
        l.forgetOwner(A);                                                 // the owner died, left or changed dimension
        assertFalse(l.hasOwner(A));
        assertTrue(l.hasOwner(B));
        l.clear();                                                        // the server stopped
        assertTrue(l.isEmpty());
        assertEquals(0, l.ownerCount());
        l.forgetFoe(null);
        l.forget(null, null);
        l.forgetOwner(null);
    }

    @Test
    void manyOwnersAndFoesStayBounded() {
        DebtLedger l = new DebtLedger();
        List<UUID> owners = new ArrayList<>();
        for (int o = 0; o < 100; o++) owners.add(new UUID(1, o));
        for (UUID o : owners) for (int f = 0; f < 200; f++) l.store(o, foe(f), 10, 0.5, 0.5, 20, f, 1000, 20);
        assertEquals(100, l.ownerCount());
        assertEquals(100 * DebtLedger.MAX_FOES, l.totalEntries(), "a hundred players hold at most twenty foes each, whatever they hit");
        int ticks = 0;
        for (UUID o : owners) ticks += l.due(o, 300, 4).size();
        assertEquals(400, ticks, "a beat costs at most four Debt ticks per player");
    }
}
