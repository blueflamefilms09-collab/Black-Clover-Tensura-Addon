package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.RecombinationBudget.Ledger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (CLAUDE.md rule 11, no lag on a public server): everything Recombination Magic puts in the world is capped per caster and per dimension, and the oldest construct ends first when a cap is
 * reached. The ledger that does the counting is pure, so this test plays a crowd of casters against it: it never holds more than its caps, it evicts oldest first, it never evicts the construct just
 * added, and a key removed some other way frees its place.
 */
class RecombinationBudgetTest {
    @Test
    void oneGolemPerCasterTheOldestEnds() {
        Ledger<Integer> l = new Ledger<>(RecombinationBudget.GOLEMS_PER_OWNER, RecombinationBudget.GOLEMS_PER_LEVEL);
        assertTrue(l.add("alice", 1).isEmpty());
        assertEquals(List.of(1), l.add("alice", 2), "a second Bull of the same caster ends the first");
        assertTrue(l.add("bob", 3).isEmpty(), "another caster has his own");
        assertEquals(2, l.size());
        assertFalse(l.has(1));
        assertTrue(l.has(2) && l.has(3));
    }

    @Test
    void theDimensionCapHoldsAgainstACrowd() {
        Ledger<Integer> l = new Ledger<>(RecombinationBudget.GOLEMS_PER_OWNER, RecombinationBudget.GOLEMS_PER_LEVEL);
        int evicted = 0, key = 0;
        for (int p = 0; p < 100; p++) {
            for (int c = 0; c < 3; c++) evicted += l.add("player" + p, key++).size();
            assertTrue(l.size() <= RecombinationBudget.GOLEMS_PER_LEVEL, "never more than the dimension cap: " + l.size());
        }
        assertEquals(RecombinationBudget.GOLEMS_PER_LEVEL, l.size());
        assertEquals(300 - RecombinationBudget.GOLEMS_PER_LEVEL, evicted, "everything over the cap was handed back to end");
    }

    @Test
    void roomsBoardsAndDrillsHaveTheirOwnCaps() {
        for (int kind : new int[]{RecombinationBudget.ROOM, RecombinationBudget.BOARDS, RecombinationBudget.DRILL}) {
            Ledger<Integer> l = new Ledger<>(RecombinationBudget.perOwner(kind), RecombinationBudget.perLevel(kind));
            for (int i = 0; i < 500; i++) {
                l.add("one caster", i);
                assertTrue(l.size() <= RecombinationBudget.perOwner(kind), "kind " + kind + " holds " + l.size());
            }
            Ledger<Integer> many = new Ledger<>(RecombinationBudget.perOwner(kind), RecombinationBudget.perLevel(kind));
            for (int i = 0; i < 500; i++) many.add("caster " + i, i);
            assertEquals(RecombinationBudget.perLevel(kind), many.size());
        }
        assertTrue(RecombinationBudget.perOwner(RecombinationBudget.ROOM) >= 7, "Bulwark's four corners and a Room Swap's two ends fit");
    }

    @Test
    void oldestFirstAndNeverTheOneJustAdded() {
        Ledger<Integer> l = new Ledger<>(3, 100);
        for (int i = 0; i < 3; i++) l.add("a", i);
        List<Integer> out = new ArrayList<>();
        for (int i = 3; i < 8; i++) out.addAll(l.add("a", i));
        assertEquals(List.of(0, 1, 2, 3, 4), out);
        assertTrue(l.has(5) && l.has(6) && l.has(7));
        assertTrue(l.add("a", 7).isEmpty(), "adding a key twice changes nothing");
    }

    @Test
    void nobodysConstructsShareOneBucket() {
        Ledger<Integer> l = new Ledger<>(2, 100);
        l.add(null, 1);
        l.add(null, 2);
        assertEquals(List.of(1), l.add(null, 3), "constructs with no caster count together");
        assertTrue(l.add("someone", 4).isEmpty());
    }

    @Test
    void aKeyThatWasRemovedFreesItsPlace() {
        Ledger<Integer> l = new Ledger<>(1, 5);
        l.add("a", 1);
        l.remove(1);
        assertTrue(l.add("a", 2).isEmpty(), "the first one ended by itself, nothing to evict");
        l.retain(k -> k != 2);
        assertEquals(0, l.size());
        assertTrue(l.add("a", 3).isEmpty());
        l.add("b", 4);
        l.retain(k -> k == 4);
        assertEquals(1, l.size());
    }

    @Test
    void bulwarkPlacesAtMostEightBlocksATick() {
        assertEquals(0, RecombinationBudget.placementTicks(0));
        assertEquals(0, RecombinationBudget.placementTicks(8));
        assertEquals(1, RecombinationBudget.placementTicks(9));
        assertEquals(3, RecombinationBudget.placementTicks(32));
        assertEquals(3, RecombinationBudget.placementTicks(500), "the ring is capped at 32 blocks");
        assertTrue(RecombinationBudget.BULWARK_MAX_BLOCKS >= 32, "the whole ring (two blocks high on the 16 columns of a 5 x 5 square) fits");
        assertEquals(RecombinationBudget.BULWARK_MAX_BLOCKS / RecombinationBudget.BULWARK_BLOCKS_PER_TICK, RecombinationBudget.placementTicks(32) + 1);
    }

    @Test
    void temporaryBlocksAreCappedPerCasterAndOnTheServer() {
        RecombinationBudget.BlockLedger b = new RecombinationBudget.BlockLedger();
        assertTrue(b.tryAdd("alice", 0, 32, 205));
        assertTrue(b.tryAdd("alice", 160, 32, 205), "a second Bulwark of the same caster 160 ticks later: 64 stand");
        assertFalse(b.tryAdd("alice", 170, 8, 205), "a third would pass 64 for one caster");
        assertEquals(64, b.liveOf("alice", 170));
        assertTrue(b.tryAdd("bob", 170, 32, 205), "another caster has his own 64");
        assertTrue(b.tryAdd("alice", 206, 32, 205), "the first batch has been removed by then (205 ticks), so there is room again");
        assertEquals(96, b.live(206), "live blocks of the server at tick 206: alice 32 + 32, bob 32");
        for (int i = 0; i < 40; i++) b.tryAdd("caster" + i, 300, 32, 205);
        assertTrue(b.live(300) <= RecombinationBudget.BULWARK_LIVE_PER_SERVER, "the server never has more than 512 standing: " + b.live(300));
        assertTrue(b.live(300 + 205) == 0, "everything has gone after the time is up");
        assertTrue(b.tryAdd("x", 600, 0, 205), "nothing to book is always fine");
    }

    @Test
    void theFoeScanAndTheOwnerCheckAreBounded() {
        assertTrue(RecombinationBudget.FOE_SCAN_MAX <= 16);
        assertTrue(RecombinationBudget.OWNER_CHECK_TICKS >= 5 && RecombinationBudget.OWNER_CHECK_TICKS <= 20);
        assertTrue(RecombinationBudget.GOLEMS_PER_OWNER == 1, "the Bull stays one per caster");
    }
}
