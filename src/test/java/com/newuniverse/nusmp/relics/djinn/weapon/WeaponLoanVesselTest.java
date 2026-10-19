package com.newuniverse.nusmp.relics.djinn.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The owner's bug: "when you have a weapon as vessel the djinn weapon doesn't overlay the weapon, it moves it over for a new type, causing the equip to
 * be removed". The vessel is found only while it is held (hands), worn or in a Curios slot; the loan used to displace whatever stood in the selected
 * hotbar slot to the first free slot, so a vessel that was a weapon in that slot left the hand, the engine no longer found it, and the Equip ended with
 * "your vessel left you". The fix is that the weapon OVERLAYS the vessel: the vessel is kept in the loan record, no inventory slot but the home slot is
 * touched, and the vessel returns to the home slot unchanged.
 *
 * <p>This class uses only the loan's original calls (summon / ensure / release), so it also compiles, and fails, against the displacing loan.
 * Items are ints: 0 empty, below 0 the weapon, 1 to 999 a plain item, 1000 and up a vessel.
 */
class WeaponLoanVesselTest {
    private static final int SIZE = 42;

    private static final WeaponLoan.Ops<Integer> OPS = new WeaponLoan.Ops<Integer>() {
        public Integer empty() { return 0; }
        public boolean isEmpty(Integer t) { return t == 0; }
        public boolean isWeapon(Integer t) { return t < 0; }
        public boolean same(Integer a, Integer b) { return a.equals(b); }
        public boolean overlays(Integer t) { return t >= 1000; }
    };

    private static final class Inv implements WeaponLoan.Slots<Integer> {
        final int[] a = new int[SIZE];
        public int size() { return SIZE; }
        public Integer get(int i) { return a[i]; }
        public void set(int i, Integer t) { a[i] = t; }
    }

    private static final java.util.function.Predicate<Integer> CURRENT = t -> t == -1;

    private static int slotOf(Inv inv, int token) {
        for (int i = 0; i < SIZE; i++) if (inv.a[i] == token) return i;
        return -1;
    }

    @Test
    void theVesselWeaponIsNotMovedToAnotherSlot() {
        Inv inv = new Inv();
        inv.a[3] = 1001;                                              // the vessel is a sword in the selected hotbar slot
        inv.a[0] = 7;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 3, () -> -1, CURRENT);
        assertEquals(-1, inv.a[3], "the Djinn weapon is in the slot the vessel was in");
        assertEquals(-1, slotOf(inv, 1001), "the vessel was not shuffled to any other slot (that is what made the engine lose it)");
        assertEquals(7, inv.a[0], "no other slot was touched");
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(1001, inv.a[3], "the vessel is back in its own slot");
        assertEquals(7, inv.a[0]);
        assertEquals(-1, slotOf(inv, -1));
    }

    @Test
    void aFullInventoryNeedsNoStashForTheVessel() {
        Inv inv = new Inv();
        for (int i = 0; i < 36; i++) inv.a[i] = i + 1;
        inv.a[5] = 1001;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 5, () -> -1, CURRENT);
        assertTrue(loan.stash().isEmpty(), "the vessel is overlaid, not stashed");
        assertEquals(-1, inv.a[5]);
        assertEquals(-1, slotOf(inv, 1001));
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(1001, inv.a[5]);
        for (int i = 0; i < 36; i++) if (i != 5) assertEquals(i + 1, inv.a[i]);
    }

    @Test
    void ensureKeepsTheVesselOutOfEverySlotWhileTheWeaponIsWhole() {
        Inv inv = new Inv();
        inv.a[0] = 1001;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 0, () -> -1, CURRENT);
        for (int i = 0; i < 20; i++) loan.ensure(inv, () -> -1, CURRENT);
        loan.summon(inv, 6, () -> -1, CURRENT);                       // the player scrolled to another slot and the engine applied again
        assertEquals(-1, inv.a[0]);
        assertEquals(-1, slotOf(inv, 1001));
        assertEquals(0, loan.home(), "the home slot never follows the scroll wheel");
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(1001, inv.a[0]);
    }

    @Test
    void anotherItemInTheHomeSlotIsStillDisplacedLikeBefore() {
        Inv inv = new Inv();
        inv.a[2] = 5;
        WeaponLoan<Integer> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 2, () -> -1, CURRENT);
        assertEquals(5, inv.a[0], "a plain item is still displaced to the first free slot (the old behaviour is kept)");
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(5, inv.a[2]);
    }
}
