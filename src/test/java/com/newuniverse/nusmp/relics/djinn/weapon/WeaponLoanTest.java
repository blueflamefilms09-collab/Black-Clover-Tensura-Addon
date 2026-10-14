package com.newuniverse.nusmp.relics.djinn.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The weapon loan never loses an item, whatever the player does while the weapon is out (items are ints: 0 empty, below 0 the weapon, above 0 a real item). */
class WeaponLoanTest {
    private static final int SIZE = 42;                       // 36 main, offhand, 4 armour, the cursor

    private static final WeaponLoan.Ops<Integer> OPS = new WeaponLoan.Ops<>() {
        public Integer empty() { return 0; }
        public boolean isEmpty(Integer t) { return t == 0; }
        public boolean isWeapon(Integer t) { return t < 0; }
        public boolean same(Integer a, Integer b) { return a.equals(b); }
    };

    private static final class Inv implements WeaponLoan.Slots<Integer> {
        final int[] a = new int[SIZE];
        public int size() { return SIZE; }
        public Integer get(int i) { return a[i]; }
        public void set(int i, Integer t) { a[i] = t; }
    }

    private static Map<Integer, Integer> count(Inv inv, WeaponLoan<Integer> loan, List<Integer> world) {
        Map<Integer, Integer> m = new HashMap<>();
        for (int t : inv.a) if (t > 0) m.merge(t, 1, Integer::sum);
        for (int t : loan.stash()) if (t > 0) m.merge(t, 1, Integer::sum);
        for (int t : world) if (t > 0) m.merge(t, 1, Integer::sum);
        return m;
    }

    private static int weapons(Inv inv) {
        int n = 0;
        for (int t : inv.a) if (t < 0) n++;
        return n;
    }

    private static final java.util.function.Predicate<Integer> CURRENT = t -> t == -1;

    @Test
    void summonDisplacesTheMainHandToAFreeSlotAndReleaseBringsItBack() {
        Inv inv = new Inv();
        inv.a[3] = 7;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 3, () -> -1, CURRENT);
        assertEquals(-1, inv.a[3]);
        assertTrue(loan.active());
        assertEquals(7, inv.a[0], "the first free slot took the displaced item");
        List<Integer> over = loan.release(inv);
        assertTrue(over.isEmpty());
        assertEquals(7, inv.a[3], "the original is back in its own slot");
        assertEquals(0, inv.a[0]);
        assertEquals(0, weapons(inv));
        assertFalse(loan.active());
    }

    @Test
    void aFullInventoryStashesTheDisplacedItem() {
        Inv inv = new Inv();
        for (int i = 0; i < 36; i++) inv.a[i] = i + 1;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 4, () -> -1, CURRENT);
        assertEquals(1, loan.stash().size());
        assertEquals(5, loan.stash().get(0).intValue());
        assertEquals(-1, inv.a[4]);
        List<Integer> over = loan.release(inv);
        assertTrue(over.isEmpty());
        assertEquals(5, inv.a[4]);
        assertTrue(loan.stash().isEmpty());
    }

    @Test
    void whenNothingFitsTheOverflowIsReturnedNotLost() {
        Inv inv = new Inv();
        for (int i = 0; i < 36; i++) inv.a[i] = i + 1;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 0, () -> -1, CURRENT);                       // 1 is stashed
        loan.load(loan.home(), loan.moved(), loan.movedItem(), List.of(1, 99, 98));         // a stash of three, one slot will be free
        List<Integer> over = loan.release(inv);
        assertEquals(2, over.size(), "slot 0 took one, two are left over");
        assertEquals(1, inv.a[0]);
    }

    @Test
    void aWeaponThatStraysIsWipedAndTheHomeSlotIsMadeWhole() {
        Inv inv = new Inv();
        inv.a[2] = 5;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 2, () -> -1, CURRENT);
        int w = 2, other = 20;
        inv.a[other] = inv.a[w];                                      // the player carried the weapon to another slot
        inv.a[w] = 0;
        loan.ensure(inv, () -> -1, CURRENT);
        assertEquals(-1, inv.a[w]);
        assertEquals(0, inv.a[other]);
        assertEquals(1, weapons(inv));
    }

    @Test
    void anItemPutInTheHomeSlotIsDisplacedNotDeleted() {
        Inv inv = new Inv();
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 1, () -> -1, CURRENT);
        inv.a[1] = 42;                                                // the weapon was swapped out and a new item came in
        loan.ensure(inv, () -> -1, CURRENT);
        assertEquals(-1, inv.a[1]);
        boolean found = false;
        for (int i = 0; i < SIZE; i++) if (inv.a[i] == 42) found = true;
        for (int t : loan.stash()) if (t == 42) found = true;
        assertTrue(found);
    }

    @Test
    void aWeaponOfTheWrongLookIsReplacedInPlace() {
        Inv inv = new Inv();
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 0, () -> -2, t -> t == -2);
        loan.ensure(inv, () -> -3, t -> t == -3);
        assertEquals(-3, inv.a[0]);
        assertEquals(1, weapons(inv));
    }

    @Test
    void releaseWithoutALoanStillWipesStrayWeapons() {
        Inv inv = new Inv();
        inv.a[10] = -1;
        inv.a[41] = -1;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(0, weapons(inv));
    }

    @Test
    void stateRoundTripsThroughLoad() {
        Inv inv = new Inv();
        for (int i = 0; i < 36; i++) inv.a[i] = i + 1;
        WeaponLoan<Integer> a = new WeaponLoan<>(OPS);
        a.summon(inv, 6, () -> -1, CURRENT);
        WeaponLoan<Integer> b = new WeaponLoan<>(OPS);
        b.load(a.home(), a.moved(), a.movedItem(), new ArrayList<>(a.stash()));
        assertEquals(a.home(), b.home());
        assertEquals(a.stash(), b.stash());
        assertTrue(b.release(inv).isEmpty());
        assertEquals(7, inv.a[6]);
    }

    /** The property: across any sequence of events no real item is ever lost or duplicated and at most one weapon exists. */
    @Test
    void neverLosesAnItemAcrossRandomSequences() {
        for (int seed = 0; seed < 4000; seed++) {
            Random r = new Random(seed);
            Inv inv = new Inv();
            int next = 1;
            int fill = r.nextInt(5) == 0 ? 36 : r.nextInt(37);
            for (int i = 0; i < fill; i++) inv.a[r.nextInt(36)] = next++;
            for (int i = 36; i < 41; i++) if (r.nextInt(3) == 0) inv.a[i] = next++;
            WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
            List<Integer> world = new ArrayList<>();
            Map<Integer, Integer> start = count(inv, loan, world);
            for (int step = 0; step < 60; step++) {
                int op = r.nextInt(12);
                switch (op) {
                    case 0, 1 -> loan.summon(inv, r.nextInt(9), () -> -1, CURRENT);
                    case 2, 3, 4 -> loan.ensure(inv, () -> -1, CURRENT);
                    case 5 -> world.addAll(loan.release(inv));
                    case 6, 7, 8 -> { int i = r.nextInt(SIZE), j = r.nextInt(SIZE); int t = inv.a[i]; inv.a[i] = inv.a[j]; inv.a[j] = t; }     // the player moves things
                    case 9 -> { int i = r.nextInt(SIZE); if (inv.a[i] != 0) { world.add(inv.a[i]); inv.a[i] = 0; } }                           // the player drops something
                    case 10 -> { for (int i = 0; i < SIZE; i++) if (inv.a[i] == 0 && !world.isEmpty() && world.get(world.size() - 1) > 0) { inv.a[i] = world.remove(world.size() - 1); break; } }
                    default -> {                                                                                                             // the loan is saved and read back
                        WeaponLoan<Integer> b = new WeaponLoan<>(OPS);
                        b.load(loan.home(), loan.moved(), loan.movedItem(), new ArrayList<>(loan.stash()));
                        loan = b;
                    }
                }
                assertEquals(start, count(inv, loan, world), "seed " + seed + " step " + step + " op " + op);
                assertTrue(weapons(inv) <= 1, "seed " + seed + " step " + step + " op " + op);
                if (op <= 4 && op >= 0 && loan.active()) {
                    assertEquals(1, weapons(inv), "an active loan keeps exactly one weapon after summon / ensure: seed " + seed);
                    assertEquals(-1, inv.a[loan.home()]);
                }
            }
            world.addAll(loan.release(inv));                                      // death, logout, the Djinn lost: everything ends
            assertEquals(start, count(inv, loan, world), "seed " + seed + " after release");
            assertEquals(0, weapons(inv));
            assertFalse(loan.active());
            assertTrue(loan.stash().isEmpty());
        }
    }
}
