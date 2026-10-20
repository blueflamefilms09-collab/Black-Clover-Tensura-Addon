package com.newuniverse.nusmp.requip;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Armoury is where a player's gear lives; the rules must give the right number of slots and must never lose or double a stack, whatever is stored, taken or moved. */
class ArmouryRulesTest {
    /** A stack for the tests: an item name, a count and the most one cell holds ("sword" does not stack). */
    record St(String item, int n) {}

    static final ArmouryRules.Ops<St> OPS = new ArmouryRules.Ops<>() {
        static final St NONE = new St("", 0);
        @Override public St empty() { return NONE; }
        @Override public boolean isEmpty(St s) { return s == null || s.n() <= 0; }
        @Override public int count(St s) { return s.n(); }
        @Override public int max(St s) { return s.item().startsWith("sword") ? 1 : 64; }
        @Override public boolean same(St a, St b) { return a.item().equals(b.item()); }
        @Override public St withCount(St s, int c) { return c <= 0 ? NONE : new St(s.item(), c); }
    };

    static List<St> cells(int n) {
        List<St> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(OPS.empty());
        return l;
    }

    @Test
    void capacityGrowsWithMasteryAndPocketCapacityAndIsHeldByEp() {
        assertEquals(18, ArmouryRules.capacity(0.0, 25_000, false));               // mastery 0, EP 25k: 18 (EP would allow 27)
        assertEquals(33, ArmouryRules.capacity(0.5, 1_000_000, false));            // 18 + 9 from mastery, +6 from Pocket Capacity
        // the passive tiers
        assertEquals(0, ArmouryRules.pocketBonus(0.34));
        assertEquals(6, ArmouryRules.pocketBonus(0.35));
        assertEquals(12, ArmouryRules.pocketBonus(0.6));
        assertEquals(18, ArmouryRules.pocketBonus(0.85));
        assertEquals(36, ArmouryRules.masteryCap(1.0));
        // a full-mastery user at 1M EP has all 54 cells
        assertEquals(ArmouryRules.MAX_SLOTS, ArmouryRules.capacity(1.0, 1_000_000, false));
        // ... but the same user at 25k EP only 27, at 50k 36, at 100k 45
        assertEquals(27, ArmouryRules.capacity(1.0, 25_000, false));
        assertEquals(36, ArmouryRules.capacity(1.0, 50_000, false));
        assertEquals(45, ArmouryRules.capacity(1.0, 100_000, false));
        // an operator has the whole grid, and nobody ever has fewer than the minimum
        assertEquals(ArmouryRules.MAX_SLOTS, ArmouryRules.capacity(0.0, 0, true));
        assertEquals(ArmouryRules.MIN_CAPACITY, ArmouryRules.capacity(0.0, 0, false));
        assertEquals(ArmouryRules.MIN_CAPACITY, ArmouryRules.capacity(Double.NaN, Double.NaN, false));
        // capacity never goes down as mastery or EP goes up
        int last = 0;
        for (double m = 0; m <= 1.0; m += 0.01) { int c = ArmouryRules.capacity(m, 400_000, false); assertTrue(c >= last); last = c; }
        last = 0;
        for (double ep = 0; ep <= 2_000_000; ep += 10_000) { int c = ArmouryRules.capacity(1.0, ep, false); assertTrue(c >= last); last = c; }
    }

    @Test
    void storingMergesThenFillsAndStopsAtTheCapacity() {
        List<St> a = cells(ArmouryRules.MAX_SLOTS);
        assertEquals(OPS.empty(), ArmouryRules.insert(a, 4, new St("dirt", 60), OPS));
        assertEquals(OPS.empty(), ArmouryRules.insert(a, 4, new St("dirt", 10), OPS));            // tops the first up to 64, 6 in the next cell
        assertEquals(new St("dirt", 64), a.get(0));
        assertEquals(new St("dirt", 6), a.get(1));
        // swords do not stack: three swords take three cells; the fifth cell is past the capacity of 4
        assertEquals(OPS.empty(), ArmouryRules.insert(a, 4, new St("sword_a", 1), OPS));
        assertEquals(OPS.empty(), ArmouryRules.insert(a, 4, new St("sword_a", 1), OPS));
        St left = ArmouryRules.insert(a, 4, new St("sword_a", 1), OPS);
        assertEquals(new St("sword_a", 1), left);                                                  // no room: the stack comes back
        assertTrue(OPS.isEmpty(a.get(4)));                                                         // nothing leaked past the capacity
    }

    @Test
    void aCellPastTheCapacityStillGivesItsStackBack() {
        List<St> a = cells(ArmouryRules.MAX_SLOTS);
        a.set(40, new St("diamond", 5));                                                           // capacity fell below 40 after a mastery loss
        List<St> inv = cells(36);
        assertEquals(ArmouryRules.Result.OK, ArmouryRules.retrieve(a, 40, inv, OPS));
        assertTrue(OPS.isEmpty(a.get(40)));
        assertEquals(5, ArmouryRules.total(inv, OPS));
        assertEquals(ArmouryRules.Result.EMPTY, ArmouryRules.retrieve(a, 40, inv, OPS));
        assertEquals(ArmouryRules.Result.BAD_INDEX, ArmouryRules.retrieve(a, 99, inv, OPS));
    }

    @Test
    void storeAndRetrieveMoveEveryItemExactlyOnce() {
        List<St> inv = cells(36);
        List<St> a = cells(ArmouryRules.MAX_SLOTS);
        inv.set(3, new St("dirt", 64));
        inv.set(4, new St("sword_b", 1));
        long before = ArmouryRules.total(inv, OPS) + ArmouryRules.total(a, OPS);
        assertEquals(ArmouryRules.Result.OK, ArmouryRules.store(inv, 3, a, 18, OPS));
        assertEquals(ArmouryRules.Result.OK, ArmouryRules.store(inv, 4, a, 18, OPS));
        assertEquals(before, ArmouryRules.total(inv, OPS) + ArmouryRules.total(a, OPS));
        assertEquals(0, ArmouryRules.total(inv, OPS));
        assertEquals(ArmouryRules.Result.OK, ArmouryRules.retrieve(a, 1, inv, OPS));
        assertEquals(before, ArmouryRules.total(inv, OPS) + ArmouryRules.total(a, OPS));
        assertEquals(ArmouryRules.Result.EMPTY, ArmouryRules.store(inv, 20, a, 18, OPS));
        assertEquals(ArmouryRules.Result.BAD_INDEX, ArmouryRules.store(inv, -1, a, 18, OPS));
    }

    @Test
    void aFullArmouryRefusesAndKeepsTheStackInTheInventory() {
        List<St> a = cells(ArmouryRules.MAX_SLOTS);
        for (int i = 0; i < 3; i++) a.set(i, new St("sword_" + i, 1));
        List<St> inv = cells(36);
        inv.set(0, new St("sword_x", 1));
        assertEquals(ArmouryRules.Result.FULL, ArmouryRules.store(inv, 0, a, 3, OPS));
        assertEquals(new St("sword_x", 1), inv.get(0));                                            // nothing was removed
        assertEquals(3, ArmouryRules.total(a, OPS));
        // a partly fitting stack stores what fits and keeps the rest
        a = cells(ArmouryRules.MAX_SLOTS);
        a.set(0, new St("dirt", 60));
        inv.set(1, new St("dirt", 10));
        assertEquals(ArmouryRules.Result.PARTIAL, ArmouryRules.store(inv, 1, a, 1, OPS));
        assertEquals(new St("dirt", 64), a.get(0));
        assertEquals(new St("dirt", 6), inv.get(1));
        assertEquals(71, ArmouryRules.total(inv, OPS) + ArmouryRules.total(a, OPS));                // 60 + 10 + the sword still in the inventory: nothing lost, nothing doubled
    }

    @Test
    void insertAllIsAllOrNothing() {
        List<St> a = cells(ArmouryRules.MAX_SLOTS);
        a.set(0, new St("sword_a", 1));
        List<St> three = List.of(new St("sword_b", 1), new St("sword_c", 1), new St("sword_d", 1));
        // capacity 3: one cell is taken, only two are free: the three swords do not all fit, so no cell changes
        assertFalse(ArmouryRules.insertAll(a, 3, three, OPS));
        assertEquals(1, ArmouryRules.total(a, OPS));
        assertTrue(ArmouryRules.insertAll(a, 4, three, OPS));
        assertEquals(4, ArmouryRules.total(a, OPS));
        assertEquals(0, ArmouryRules.freeSlots(a, 4, OPS));
        assertTrue(ArmouryRules.fits(a, 5, new St("sword_e", 1), OPS));
        assertFalse(ArmouryRules.fits(a, 4, new St("sword_e", 1), OPS));
    }

    @Test
    void takeReturnsWhatWasRemovedAndNothingMore() {
        List<St> a = cells(ArmouryRules.MAX_SLOTS);
        a.set(2, new St("dirt", 10));
        assertEquals(new St("dirt", 4), ArmouryRules.take(a, 2, 4, OPS));
        assertEquals(new St("dirt", 6), a.get(2));
        assertEquals(new St("dirt", 6), ArmouryRules.take(a, 2, 99, OPS));
        assertTrue(OPS.isEmpty(a.get(2)));
        assertTrue(OPS.isEmpty(ArmouryRules.take(a, 2, 1, OPS)));
        assertTrue(OPS.isEmpty(ArmouryRules.take(a, -3, 1, OPS)));
    }

    /** The property that matters most: a long random run of stores and retrieves never changes the total (no stack is lost, none is doubled). */
    @Test
    void randomRunsNeverLoseOrDoubleAnything() {
        Random rnd = new Random(1999);
        String[] items = {"dirt", "stone", "sword_a", "sword_b", "arrow", "apple"};
        for (int run = 0; run < 200; run++) {
            List<St> inv = cells(36);
            List<St> a = cells(ArmouryRules.MAX_SLOTS);
            for (int i = 0; i < 36; i++) if (rnd.nextInt(3) > 0) inv.set(i, new St(items[rnd.nextInt(items.length)], 1 + rnd.nextInt(64)));
            for (int i = 0; i < inv.size(); i++) if (inv.get(i).item().startsWith("sword")) inv.set(i, new St(inv.get(i).item(), 1));
            long total = ArmouryRules.total(inv, OPS) + ArmouryRules.total(a, OPS);
            int cap = ArmouryRules.capacity(rnd.nextDouble(), 25_000 + rnd.nextInt(400_000), false);
            for (int step = 0; step < 300; step++) {
                if (rnd.nextBoolean()) ArmouryRules.store(inv, rnd.nextInt(36), a, cap, OPS);
                else ArmouryRules.retrieve(a, rnd.nextInt(ArmouryRules.MAX_SLOTS), inv, OPS);
                if (rnd.nextInt(20) == 0) ArmouryRules.insert(a, cap, new St("arrow", 1 + rnd.nextInt(5)), OPS);                // an outside stack added: the total changes by exactly that
                long now = ArmouryRules.total(inv, OPS) + ArmouryRules.total(a, OPS);
                assertTrue(now >= total, "run " + run + " step " + step + ": a stack was lost");
                total = now;
                for (int i = 0; i < inv.size(); i++) assertTrue(inv.get(i).n() <= 64 && (!inv.get(i).item().startsWith("sword") || inv.get(i).n() <= 1), "an over-full cell");
                for (int i = cap; i < ArmouryRules.MAX_SLOTS; i++) assertTrue(OPS.isEmpty(a.get(i)), "stored past the capacity");
            }
        }
    }
}
