package com.newuniverse.nusmp.relics.djinn.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The vessel overlay (the fix of the owner's "the Djinn weapon moves my weapon vessel over and the equip is removed") in the cases a game produces:
 * enchantments and components survive, a full inventory, the hotbar scrolled, a save and a read back (logout, a dimension change), death, creative,
 * and 4000 random sequences in which no item (the vessel above all) is ever lost, copied or left in a slot but its own. Items are small value records
 * standing in for item stacks: equality is exactly "the same item with the same enchantments and components", which is what a stack must keep.
 */
class WeaponLoanOverlayTest {
    private static final int SIZE = 42;

    /** A stand-in for an ItemStack: an id (0 empty, below 0 the Djinn weapon), a kind, enchantments and data components. */
    private record Stack(int id, String kind, Map<String, Integer> enchants, Map<String, String> comps) {
        static final Stack EMPTY = new Stack(0, "", Map.of(), Map.of());

        Stack copy() { return new Stack(id, kind, new TreeMap<>(enchants), new TreeMap<>(comps)); }          // an NBT write and read back
    }

    private static Stack plain(int id) { return new Stack(id, "item", Map.of(), Map.of()); }

    private static Stack vessel(int id) {
        Map<String, Integer> e = new TreeMap<>();
        e.put("sharpness", 5);
        e.put("unbreaking", 3);
        e.put("tensura:severance", 2);
        Map<String, String> c = new TreeMap<>();
        c.put("nusmp:djinn_bound", "baal");
        c.put("nusmp:djinn_serial", "0b5e8d57-0000-4000-8000-00000000c0de");
        c.put("custom_name", "Warden's Edge");
        c.put("damage", "37");
        return new Stack(id, "vessel", e, c);
    }

    private static final Stack WEAPON = new Stack(-1, "djinn_weapon", Map.of(), Map.of("nusmp:djinn_temp", "owner"));

    private static final WeaponLoan.Ops<Stack> OPS = new WeaponLoan.Ops<Stack>() {
        public Stack empty() { return Stack.EMPTY; }
        public boolean isEmpty(Stack t) { return t.id() == 0; }
        public boolean isWeapon(Stack t) { return t.id() < 0; }
        public boolean same(Stack a, Stack b) { return a.equals(b); }
        public boolean overlays(Stack t) { return t.kind().equals("vessel"); }
    };

    private static final java.util.function.Predicate<Stack> CURRENT = t -> t.id() == -1;

    private static final class Inv implements WeaponLoan.Slots<Stack> {
        final Stack[] a = new Stack[SIZE];
        Inv() { java.util.Arrays.fill(a, Stack.EMPTY); }
        public int size() { return SIZE; }
        public Stack get(int i) { return a[i]; }
        public void set(int i, Stack t) { a[i] = t; }
    }

    /** What the engine's "is the vessel on you" asks: held in a hand, or under the Djinn weapon. (The offhand is slot 36 of the flat view.) */
    private static boolean vesselOnBody(Inv inv, WeaponLoan<Stack> loan, int selected) {
        return inv.a[selected].kind().equals("vessel") || inv.a[36].kind().equals("vessel") || (loan.overlaid() != null && loan.overlaid().kind().equals("vessel"));
    }

    /** What a save to the player's record and a read back does. */
    private static WeaponLoan<Stack> saveAndLoad(WeaponLoan<Stack> loan) {
        WeaponLoan<Stack> b = new WeaponLoan<>(OPS);
        List<Stack> stash = new ArrayList<>();
        for (Stack s : loan.stash()) stash.add(s.copy());
        b.load(loan.home(), loan.moved(), loan.movedItem() == null ? null : loan.movedItem().copy(), stash, loan.overlaid() == null ? null : loan.overlaid().copy());
        return b;
    }

    @Test
    void enchantmentsAndComponentsOfTheVesselSurviveTheWholeEquipExactly() {
        Inv inv = new Inv();
        Stack v = vessel(1000);
        Stack original = v.copy();
        inv.a[4] = v;
        WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 4, () -> WEAPON, CURRENT);
        loan = saveAndLoad(loan);                                      // the Equip goes on across a relog or a dimension change
        loan.ensure(inv, () -> WEAPON, CURRENT);
        loan = saveAndLoad(loan);
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(original, inv.a[4], "the vessel is back in its slot with every enchantment and component");
        assertEquals(1, count(inv, "vessel"));
        assertEquals(0, count(inv, "djinn_weapon"));
    }

    @Test
    void theVesselCountsAsOnTheBodyWhileTheWeaponCoversItAndAfterAScroll() {
        Inv inv = new Inv();
        inv.a[2] = vessel(1000);
        WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
        assertTrue(vesselOnBody(inv, loan, 2), "before the Equip the vessel is in the hand");
        loan.summon(inv, 2, () -> WEAPON, CURRENT);
        assertTrue(vesselOnBody(inv, loan, 2), "the weapon covers the vessel: it must not read as lost (the engine ends the Equip when it does)");
        assertTrue(vesselOnBody(inv, loan, 7), "scrolling to another hotbar slot keeps it");
        assertEquals(-1, inv.a[2].id());
        loan.release(inv);
        assertTrue(vesselOnBody(inv, loan, 2), "after the Equip it is in the hand again");
    }

    @Test
    void creativeDeathAndLogoutAllHandTheVesselBack() {
        for (String how : new String[]{"creative", "death", "logout", "dimension"}) {
            Inv inv = new Inv();
            inv.a[0] = vessel(1000);
            for (int i = 1; i < 36; i++) inv.a[i] = plain(i);
            WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
            loan.summon(inv, 0, () -> WEAPON, CURRENT);
            if (how.equals("logout") || how.equals("dimension")) loan = saveAndLoad(loan);
            List<Stack> over = loan.release(inv);                      // creative mode, a death before the drops, a logout, or the login that follows a crash
            assertTrue(over.isEmpty(), how);
            assertEquals(vessel(1000), inv.a[0], how);
            assertEquals(0, count(inv, "djinn_weapon"), how);
            assertFalse(loan.active(), how);
        }
    }

    @Test
    void aSecondItemPutInTheHomeSlotIsDisplacedAndTheVesselStillComesBack() {
        Inv inv = new Inv();
        inv.a[1] = vessel(1000);
        WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 1, () -> WEAPON, CURRENT);
        inv.a[1] = plain(55);                                          // the weapon was swapped out for a stone
        loan.ensure(inv, () -> WEAPON, CURRENT);
        assertEquals(-1, inv.a[1].id());
        assertEquals(1, count(inv, "item"), "the stone was displaced, not deleted");
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(vessel(1000), inv.a[1], "the vessel is in its own slot");
        assertEquals(1, count(inv, "item"));
    }

    @Test
    void aLoanWhoseHomeWasLostStillGivesTheVesselBack() {
        Inv inv = new Inv();
        WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
        loan.load(-1, -1, null, List.of(), vessel(1000));              // a record with the overlay but no home
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(1, count(inv, "vessel"));
        Inv fresh = new Inv();
        WeaponLoan<Stack> again = new WeaponLoan<>(OPS);
        again.load(-1, -1, null, List.of(), vessel(1000));
        fresh.a[0] = plain(3);
        again.summon(fresh, 0, () -> WEAPON, CURRENT);                 // a new Equip over a stale overlay does not delete it
        int where = count(fresh, "vessel") + (again.overlaid() != null ? 1 : 0) + (int) again.stash().stream().filter(s -> s.kind().equals("vessel")).count();
        assertEquals(1, where, "the stale vessel is somewhere safe (a slot, the overlay or the stash)");
        assertTrue(again.release(fresh).isEmpty());
        assertEquals(1, count(fresh, "vessel"));
        assertEquals(1, count(fresh, "item"));
    }

    @Test
    void aVesselPutIntoTheHomeSlotDuringTheEquipIsOverlaidToo() {
        Inv inv = new Inv();
        inv.a[0] = plain(9);
        inv.a[20] = vessel(1000);
        WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
        loan.summon(inv, 0, () -> WEAPON, CURRENT);                    // the vessel was not in the hand, so nothing is overlaid yet
        assertNull(loan.overlaid());
        inv.a[0] = inv.a[20];                                          // the player swaps the weapon out for the vessel
        inv.a[20] = WEAPON;
        loan.ensure(inv, () -> WEAPON, CURRENT);
        assertNotNull(loan.overlaid(), "the vessel is under the weapon now");
        assertEquals(0, count(inv, "vessel"), "it was not shoved into another slot");
        assertEquals(-1, inv.a[0].id());
        assertTrue(loan.release(inv).isEmpty());
        assertEquals(vessel(1000), inv.a[0]);
    }

    private static int count(Inv inv, String kind) {
        int n = 0;
        for (Stack s : inv.a) if (s.kind().equals(kind)) n++;
        return n;
    }

    // ---------------------------------------------------------------- the property
    private static Map<Integer, Integer> census(Inv inv, WeaponLoan<Stack> loan, List<Stack> world) {
        Map<Integer, Integer> m = new HashMap<>();
        for (Stack t : inv.a) if (t.id() > 0) m.merge(t.id(), 1, Integer::sum);
        for (Stack t : loan.stash()) if (t.id() > 0) m.merge(t.id(), 1, Integer::sum);
        if (loan.overlaid() != null) m.merge(loan.overlaid().id(), 1, Integer::sum);
        for (Stack t : world) if (t.id() > 0) m.merge(t.id(), 1, Integer::sum);
        return m;
    }

    private static int weapons(Inv inv) {
        int n = 0;
        for (Stack t : inv.a) if (t.id() < 0) n++;
        return n;
    }

    /** Across any order of: summon, ensure, release, the player moving and dropping things, a save and read back, creative, death, a scroll to another slot,
     *  nothing is ever lost or copied, the vessel is never in a slot other than its own while the weapon covers it, and it ends up exactly as it started. */
    @Test
    void neverLosesOrCopiesAnItemAndNeverLeavesTheVesselInAnotherSlot() {
        for (int seed = 0; seed < 4000; seed++) {
            Random r = new Random(seed);
            Inv inv = new Inv();
            int next = 1;
            int fill = r.nextInt(4) == 0 ? 36 : r.nextInt(37);
            for (int i = 0; i < fill; i++) inv.a[r.nextInt(36)] = plain(next++);
            int vslot = r.nextInt(9);
            Stack v = vessel(1000);
            if (inv.a[vslot].id() != 0) next++;                       // the vessel replaces whatever was there
            inv.a[vslot] = v;
            Stack original = v.copy();
            WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
            List<Stack> world = new ArrayList<>();
            Map<Integer, Integer> start = census(inv, loan, world);
            int selected = vslot;
            boolean vesselOut = false;                                // the vessel left the player (dropped by the player on purpose)
            for (int step = 0; step < 60; step++) {
                int op = r.nextInt(13);
                switch (op) {
                    case 0, 1 -> loan.summon(inv, selected, () -> WEAPON, CURRENT);
                    case 2, 3, 4 -> loan.ensure(inv, () -> WEAPON, CURRENT);
                    case 5 -> world.addAll(loan.release(inv));                                       // an Equip that ends, creative mode
                    case 6, 7 -> { int i = r.nextInt(SIZE), j = r.nextInt(SIZE); Stack t = inv.a[i]; inv.a[i] = inv.a[j]; inv.a[j] = t; }
                    case 8 -> { int i = r.nextInt(SIZE); if (inv.a[i].id() > 0) { world.add(inv.a[i]); inv.a[i] = Stack.EMPTY; } }
                    case 9 -> loan = saveAndLoad(loan);                                              // logout / login, a dimension change
                    case 10 -> selected = r.nextInt(9);                                              // the scroll wheel
                    case 11 -> {                                                                     // death: the Equip ends first, then the inventory drops
                        world.addAll(loan.release(inv));
                        for (int i = 0; i < SIZE; i++) if (inv.a[i].id() > 0) { world.add(inv.a[i]); inv.a[i] = Stack.EMPTY; }
                        for (int k = world.size() - 1; k >= 0; k--) if (world.get(k).id() > 0 && r.nextInt(2) == 0) {                           // the player picks some back up
                            int free = -1;
                            for (int i = 0; i < 36 && free < 0; i++) if (inv.a[i].id() == 0) free = i;
                            if (free >= 0) inv.a[free] = world.remove(k);
                        }
                    }
                    default -> { }                                                                   // nothing happens (time passes)
                }
                assertEquals(start, census(inv, loan, world), "seed " + seed + " step " + step + " op " + op);
                assertTrue(weapons(inv) <= 1, "seed " + seed + " step " + step);
                if (loan.overlaid() != null) {
                    assertEquals(0, count(inv, "vessel"), "while the weapon covers the vessel it is in no slot (seed " + seed + " step " + step + " op " + op + ")");
                    assertEquals(original, loan.overlaid(), "the overlaid vessel is the original, unchanged");
                }
            }
            world.addAll(loan.release(inv));
            assertEquals(start, census(inv, loan, world), "seed " + seed + " after release");
            assertEquals(0, weapons(inv));
            assertFalse(loan.active());
            assertNull(loan.overlaid());
            for (Stack t : inv.a) if (t.kind().equals("vessel")) assertEquals(original, t, "a vessel is only ever returned as it was (seed " + seed + ")");
        }
    }

    @Test
    void theVesselReturnsToItsExactSlotWhenNothingElseTookIt() {
        for (int slot = 0; slot < 9; slot++) {
            Inv inv = new Inv();
            for (int i = 0; i < 36; i++) inv.a[i] = plain(i + 1);
            inv.a[slot] = vessel(1000);
            WeaponLoan<Stack> loan = new WeaponLoan<>(OPS);
            loan.summon(inv, slot, () -> WEAPON, CURRENT);
            for (int k = 0; k < 10; k++) loan.ensure(inv, () -> WEAPON, CURRENT);
            loan = saveAndLoad(loan);
            loan.ensure(inv, () -> WEAPON, CURRENT);
            assertTrue(loan.release(inv).isEmpty());
            assertEquals(vessel(1000), inv.a[slot], "slot " + slot);
            for (int i = 0; i < 36; i++) if (i != slot) assertEquals(plain(i + 1), inv.a[i], "slot " + i + " was never touched");
        }
    }
}
