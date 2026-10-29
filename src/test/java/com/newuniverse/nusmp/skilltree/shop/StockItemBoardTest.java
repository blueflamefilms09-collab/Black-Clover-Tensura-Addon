package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the shelf of item lines must be what the owner asked: ONE item rolled per slot per restock, the same for every player and every restart (a pure function of the world seed, the restock
 * period, the restock counter and the slot id), different after a restock, never the same item twice on one shelf, a stack no bigger than the item allows, a per-item price that stays inside the line's
 * band, an empty pool that makes no slot, and a shelf that can never outgrow its cap.
 */
class StockItemBoardTest {
    private static final long SEED = 123456789L;

    private static List<StockBoard.PoolItem> pool(String... ids) {
        List<StockBoard.PoolItem> out = new ArrayList<>();
        for (String id : ids) out.add(new StockBoard.PoolItem(id, id.endsWith("sword") ? 1 : 64));
        return out;
    }

    private static StockEntry line(String id, String item, long min, long max, int countMin, int countMax, int stockMin, int stockMax, int weight) {
        return new StockEntry(id, StockEntry.Kind.ITEM, "", false, "", min, max, 0.0, stockMin, stockMax, weight, false, item, countMin, countMax, List.of());
    }

    private static List<StockBoard.Slot> slots(StockEntry... lines) {
        return StockBoard.slots(List.of(lines), List.of(), e -> pool("a:one", "a:two", "a:three", "a:four", "a:five", "a:six"), "");
    }

    @Test
    void theSameInputsGiveTheSameShelfAndARestockChangesIt() {
        List<StockBoard.Slot> s = slots(line("one", "#a:t", 1000, 3000, 1, 16, 1, 4, 100));
        List<StockBoard.Offer> a = StockBoard.build(SEED, 5, 0, s, id -> 0);
        assertEquals(a, StockBoard.build(SEED, 5, 0, s, id -> 0), "every player and every restart sees the same shelf");
        Set<String> seen = new HashSet<>();
        for (int period = 0; period < 60; period++) for (StockBoard.Offer o : StockBoard.build(SEED, period, 0, s, id -> 0)) seen.add(o.ref());
        assertTrue(seen.size() >= 4, "over many restocks the line shows most of its pool, got " + seen);
        boolean epochChanges = false;
        for (int epoch = 1; epoch < 20 && !epochChanges; epoch++) epochChanges = !StockBoard.build(SEED, 5, epoch, s, id -> 0).equals(a);
        assertTrue(epochChanges, "the operator's restock command gives a new shelf");
        assertNotEquals(StockBoard.build(1L, 5, 0, s, id -> 0).get(0).ref() + StockBoard.build(1L, 6, 0, s, id -> 0).get(0).ref() + StockBoard.build(2L, 5, 0, s, id -> 0).get(0).ref(),
                a.get(0).ref() + a.get(0).ref() + a.get(0).ref(), "a different world rolls a different shelf");
    }

    @Test
    void theRolledItemComesFromThePoolAndTheOfferIsWellFormed() {
        StockEntry e = line("one", "#a:t", 1000, 3000, 4, 12, 2, 5, 100);
        List<StockBoard.Slot> s = slots(e);
        Set<String> ids = new HashSet<>();
        for (StockBoard.PoolItem p : s.get(0).pool()) ids.add(p.id());
        for (int period = 0; period < 300; period++) {
            for (StockBoard.Offer o : StockBoard.build(SEED, period, 0, s, id -> 0)) {
                assertEquals(StockEntry.Kind.ITEM, o.kind());
                assertTrue(ids.contains(o.ref()), o.ref());
                assertTrue(o.amount() >= 4 && o.amount() <= 12, "amount " + o.amount());
                assertTrue(o.total() >= 2 && o.total() <= 5);
                long perItem = o.price() / o.amount();
                assertEquals(0, o.price() % o.amount(), "the stack price is a whole multiple of the per-item price");
                assertTrue(perItem >= 1000 && perItem <= 3000, "per item " + perItem + " stays in the band");
                assertEquals("one", o.id());
            }
        }
    }

    @Test
    void aStackNeverExceedsWhatTheItemAllows() {
        StockEntry e = line("blades", "#a:swords", 1000, 2000, 10, 20, 1, 1, 100);
        List<StockBoard.Slot> s = StockBoard.slots(List.of(e), List.of(), x -> pool("a:iron_sword", "a:gold_sword"), "");
        for (int period = 0; period < 50; period++) for (StockBoard.Offer o : StockBoard.build(SEED, period, 0, s, id -> 0)) assertEquals(1, o.amount(), "a sword comes one at a time");
    }

    @Test
    void twoSlotsNeverPutOutTheSameItem() {
        List<StockBoard.Slot> s = StockBoard.slots(List.of(line("one", "#a:t", 1000, 2000, 1, 1, 1, 1, 100), line("two", "#a:t", 1000, 2000, 1, 1, 1, 1, 100), line("three", "#a:t", 1000, 2000, 1, 1, 1, 1, 100)),
                List.of(), e -> pool("a:x", "a:y", "a:z"), "");
        for (int period = 0; period < 200; period++) {
            List<StockBoard.Offer> offers = StockBoard.build(SEED, period, 0, s, id -> 0);
            Set<String> refs = new HashSet<>();
            for (StockBoard.Offer o : offers) assertTrue(refs.add(o.ref()), "period " + period + " put out " + o.ref() + " twice");
            assertEquals(3, offers.size(), "three items, three slots: all fit");
        }
    }

    @Test
    void aSlotWithNoFreeItemLeftIsDroppedNotDuplicated() {
        List<StockBoard.Slot> s = StockBoard.slots(List.of(line("one", "minecraft:diamond", 1000, 2000, 1, 1, 1, 1, 100), line("two", "minecraft:diamond", 1000, 2000, 1, 1, 1, 1, 100)),
                List.of(), e -> pool("minecraft:diamond"), "");
        List<StockBoard.Offer> offers = StockBoard.build(SEED, 1, 0, s, id -> 0);
        assertEquals(1, offers.size());
        assertEquals("one", offers.get(0).id());
    }

    @Test
    void aLineWithNoPoolMakesNoSlot() {
        List<StockBoard.Slot> none = StockBoard.slots(List.of(line("ghost", "#nobody:nothing", 1000, 2000, 1, 1, 1, 1, 100), line("real", "#a:t", 1000, 2000, 1, 1, 1, 1, 100)), List.of(),
                e -> e.id().equals("ghost") ? List.of() : pool("a:x"), "");
        assertEquals(List.of("real"), none.stream().map(StockBoard.Slot::id).toList(), "an empty or unknown tag is skipped, the rest of the shelf stands");
        assertTrue(StockBoard.slots(List.of(line("g", "#a:t", 1000, 2000, 1, 1, 1, 1, 100)), List.of()).isEmpty(), "with no pool resolver an item line makes no slot");
        assertTrue(StockBoard.slots(List.of(line("g", "#a:t", 1000, 2000, 1, 1, 1, 1, 100)), List.of(), e -> null, "").isEmpty(), "a null pool is an empty pool");
    }

    @Test
    void theWeightDecidesWhetherASlotIsOnTheShelf() {
        List<StockBoard.Slot> never = slots(line("never", "#a:t", 1000, 2000, 1, 1, 1, 1, 0));
        List<StockBoard.Slot> always = slots(line("always", "#a:t", 1000, 2000, 1, 1, 1, 1, 100));
        List<StockBoard.Slot> half = slots(line("half", "#a:t", 1000, 2000, 1, 1, 1, 1, 50));
        int present = 0;
        for (int period = 0; period < 400; period++) {
            assertTrue(StockBoard.build(SEED, period, 0, never, id -> 0).isEmpty());
            assertEquals(1, StockBoard.build(SEED, period, 0, always, id -> 0).size());
            present += StockBoard.build(SEED, period, 0, half, id -> 0).size();
        }
        assertTrue(present > 140 && present < 260, "half of 400 restocks, got " + present);
    }

    @Test
    void whatWasBoughtIsCountedAgainstTheSlotAndNotTheItem() {
        List<StockBoard.Slot> s = slots(line("one", "#a:t", 1000, 2000, 1, 1, 3, 3, 100));
        StockBoard.Offer o = StockBoard.build(SEED, 1, 0, s, id -> id.equals("one") ? 2 : 0).get(0);
        assertEquals(3, o.total());
        assertEquals(1, o.left());
    }

    @Test
    void anotherShopsSlotIdsCarryItsPrefixAndRollApart() {
        StockEntry e = line("one", "#a:t", 1000, 3000, 1, 16, 1, 4, 100);
        List<StockBoard.Slot> witch = StockBoard.slots(List.of(e), List.of(), x -> pool("a:one", "a:two", "a:three", "a:four", "a:five", "a:six"), "");
        List<StockBoard.Slot> smith = StockBoard.slots(List.of(e), List.of(), x -> pool("a:one", "a:two", "a:three", "a:four", "a:five", "a:six"), "smith/");
        assertEquals("one", witch.get(0).id(), "the witch's counters are the bare ids, as they always were");
        assertEquals("smith/one", smith.get(0).id());
        int same = 0;
        for (int period = 0; period < 100; period++) {
            if (StockBoard.build(SEED, period, 0, witch, id -> 0).get(0).ref().equals(StockBoard.build(SEED, period, 0, smith, id -> 0).get(0).ref())) same++;
        }
        assertTrue(same < 60, "two shops with the same line do not stock the same thing every time (" + same + "/100)");
    }

    @Test
    void theShelfIsCutAtTheSlotCap() {
        List<StockEntry> many = new ArrayList<>();
        for (int i = 0; i < StockSchema.MAX_ENTRIES; i++) many.add(line("line_" + i, "#a:t", 1000, 2000, 1, 1, 1, 1, 100));
        List<StockBoard.Slot> s = StockBoard.slots(many, List.of(), e -> pool("a:x"), "");
        assertEquals(StockBoard.MAX_SLOTS, s.size());
        assertTrue(StockBoard.MAX_SLOTS <= ShopSnapshot_MAX_OFFERS, "the cap fits in one shop picture");
        assertEquals("line_0", s.get(0).id(), "the first lines win");
    }

    private static final int ShopSnapshot_MAX_OFFERS = 128;

    @Test
    void theOldBookLinesStillBuildTheirOldOffersWithOneItemPerPurchase() {
        StockEntry sp = new StockEntry("a", StockEntry.Kind.SP_BOOK, "", false, "x", 100, 200, 0.0, 1, 1, 100, false);
        StockEntry route = new StockEntry("fang", StockEntry.Kind.ROUTE_BOOK, "nusmp:fang", false, "", 100, 200, 0.0, 1, 1, 100, false);
        List<StockBoard.Slot> s = StockBoard.slots(List.of(sp, route), List.of("nusmp:fang"));
        assertEquals(List.of("fang"), s.stream().map(StockBoard.Slot::id).toList(), "an SP line still makes no slot");
        StockBoard.Offer o = StockBoard.build(SEED, 1, 0, s, id -> 0).get(0);
        assertEquals(1, o.amount());
        assertEquals("nusmp:fang", o.ref());
        assertFalse(o.flagship());
    }

    @Test
    void rollItemIsPureAndStaysInsideItsRanges() {
        for (int size = 1; size <= 7; size++) {
            for (int period = 0; period < 100; period++) {
                StockRoll.ItemRoll r = StockRoll.rollItem(SEED, period, 0, "slot", size, 3, 9);
                assertEquals(r, StockRoll.rollItem(SEED, period, 0, "slot", size, 3, 9));
                assertTrue(r.index() >= 0 && r.index() < size);
                assertTrue(r.amount() >= 3 && r.amount() <= 9);
            }
        }
        assertEquals(0, StockRoll.rollItem(SEED, 1, 0, "slot", 0, 1, 1).amount(), "an empty pool rolls nothing");
        assertEquals(1, StockRoll.rollItem(SEED, 1, 0, "slot", 3, 0, 0).amount(), "a nonsense range still gives one item");
        assertEquals(5, StockRoll.rollItem(SEED, 1, 0, "slot", 3, 5, 2).amount(), "min above max collapses to min");
    }
}
