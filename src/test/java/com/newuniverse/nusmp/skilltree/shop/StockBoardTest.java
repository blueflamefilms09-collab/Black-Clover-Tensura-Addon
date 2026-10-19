package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the shelf is built from three things that change independently (the data file, the routes that exist, what has been bought), and the rules that join them must hold
 * for any combination: the flagship is never offered twice, a route that does not exist has no book, a new route appears on the all-routes line with no edit, a line that rolled
 * "absent" leaves no hole in the ids, and the restock clock is counted in whole in-game days of game time.
 */
class StockBoardTest {
    private static final long SEED = 42L;

    private static StockEntry route(String id, String route, boolean all) {
        return new StockEntry(id, StockEntry.Kind.ROUTE_BOOK, route, all, "", 2_000_000, 3_000_000, 0.0, 1, 1, 100, false);
    }

    private static StockEntry weighted(String id, String route, int weight) {
        return new StockEntry(id, StockEntry.Kind.ROUTE_BOOK, route, false, "", 2_000_000, 3_000_000, 0.0, 3, 5, weight, false);
    }

    private static StockEntry sp(String id, String kind, int weight) {
        return new StockEntry(id, StockEntry.Kind.SP_BOOK, "", false, kind, 100_000, 200_000, 0.0, 3, 5, weight, false);
    }

    private static List<String> ids(List<StockBoard.Slot> slots) {
        List<String> out = new ArrayList<>();
        for (StockBoard.Slot s : slots) out.add(s.id());
        return out;
    }

    @Test
    void theAllRoutesLineSkipsTheRoutesThatHaveALineOfTheirOwn() {
        List<StockEntry> entries = List.of(route("fang", "nusmp:smoldering_fang_of_insanity", false), route("books", "", true), sp("archery", "beginner_archery", 100));
        List<String> routes = List.of("nusmp:smoldering_fang_of_insanity", "nusmp:lady_luck", "nusmp:basic_swordsmanship");
        List<StockBoard.Slot> slots = StockBoard.slots(entries, routes);
        assertEquals(List.of("fang", "books@nusmp:lady_luck", "books@nusmp:basic_swordsmanship"), ids(slots), "an old SP line makes no slot any more (1.96.0)");
        assertEquals("nusmp:smoldering_fang_of_insanity", slots.get(0).ref());
        assertEquals("nusmp:basic_swordsmanship", slots.get(2).ref());
    }

    @Test
    void aLineForARouteThatDoesNotExistMakesNoSlot() {
        List<StockEntry> entries = List.of(route("fang", "nusmp:smoldering_fang_of_insanity", false), route("books", "", true));
        assertEquals(List.of("books@nusmp:lady_luck"), ids(StockBoard.slots(entries, List.of("nusmp:lady_luck"))), "the missing flagship route is not stocked, nor taken from the all-routes line");
        assertTrue(StockBoard.slots(entries, List.of()).isEmpty(), "with no routes nothing can stock the shelf");
    }

    @Test
    void aSlotTheShopPacketCouldNotCarryIsLeftOffTheShelf() {
        String huge = "nusmp:" + "x".repeat(StockBoard.MAX_ID);
        List<StockEntry> entries = List.of(route("books", "", true));
        assertEquals(List.of("books@nusmp:ok"), ids(StockBoard.slots(entries, List.of(huge, "nusmp:ok"))), "a route id too long for the wire is skipped, the others stay");
        String edge = "nusmp:" + "y".repeat(StockBoard.MAX_ID - "books@nusmp:".length());
        assertEquals(1, StockBoard.slots(entries, List.of(edge)).size(), "a slot id of exactly the limit is kept");
        assertTrue(StockBoard.slots(entries, List.of(edge + "z")).isEmpty(), "one character more is not");
    }

    @Test
    void aNewRouteAppearsWithNoEditToTheFile() {
        List<StockEntry> entries = List.of(route("books", "", true));
        assertEquals(1, StockBoard.slots(entries, List.of("nusmp:a")).size());
        assertEquals(2, StockBoard.slots(entries, List.of("nusmp:a", "nusmp:b")).size());
    }

    @Test
    void buildAppliesTheRollTheWeightAndThePurchases() {
        List<StockEntry> entries = List.of(weighted("archery", "nusmp:basic_swordsmanship", 100), weighted("ghost", "nusmp:oath_of_the_heavy_armor", 0), route("fang", "nusmp:smoldering_fang_of_insanity", false));
        List<StockBoard.Slot> slots = StockBoard.slots(entries, List.of("nusmp:basic_swordsmanship", "nusmp:oath_of_the_heavy_armor", "nusmp:smoldering_fang_of_insanity"));
        List<StockBoard.Offer> shelf = StockBoard.build(SEED, 5, 0, slots, id -> 0);
        assertEquals(2, shelf.size(), "the weight-0 line is not on the shelf");
        assertNull(StockBoard.find(shelf, "ghost"));
        StockBoard.Offer a = StockBoard.find(shelf, "archery");
        assertNotNull(a);
        assertEquals(StockEntry.Kind.ROUTE_BOOK, a.kind());
        assertTrue(a.total() >= 3 && a.total() <= 5);
        assertEquals(a.total(), a.left());

        Map<String, Integer> bought = Map.of("archery", 2, "fang", 5);
        List<StockBoard.Offer> after = StockBoard.build(SEED, 5, 0, slots, id -> bought.getOrDefault(id, 0));
        assertEquals(a.price(), StockBoard.find(after, "archery").price(), "buying does not move the price");
        assertEquals(Math.max(0, a.total() - 2), StockBoard.find(after, "archery").left());
        assertEquals(0, StockBoard.find(after, "fang").left(), "more bought than stocked is just sold out, never negative");
    }

    @Test
    void aNewPeriodGivesANewShelfWithNothingBoughtYet() {
        List<StockBoard.Slot> slots = StockBoard.slots(List.of(route("books", "", true)), List.of("nusmp:a", "nusmp:b", "nusmp:c"));
        List<StockBoard.Offer> p1 = StockBoard.build(SEED, 1, 0, slots, id -> 1);
        List<StockBoard.Offer> p2 = StockBoard.build(SEED, 2, 0, slots, id -> 0);
        assertEquals(0, p1.get(0).left());
        assertEquals(1, p2.get(0).left());
        boolean anyPriceMoved = false;
        for (int i = 0; i < p1.size(); i++) if (p1.get(i).price() != p2.get(i).price()) anyPriceMoved = true;
        assertTrue(anyPriceMoved, "the market moved between two periods");
    }

    @Test
    void theRestockClockCountsWholeGameDays() {
        assertEquals(0, StockBoard.period(0, 3));
        assertEquals(0, StockBoard.period(3 * 24_000L - 1, 3));
        assertEquals(1, StockBoard.period(3 * 24_000L, 3));
        assertEquals(5, StockBoard.period(17 * 24_000L, 3));
        assertEquals(17, StockBoard.period(17 * 24_000L, 1));
        assertEquals(0, StockBoard.period(-5, 3), "a negative time reads as the start");
        assertEquals(StockBoard.period(48_000, 1), StockBoard.period(48_000, 0), "a restock length below one day reads as one day");
        assertEquals(StockBoard.period(48_000, 1), StockBoard.period(48_000, -4));

        assertEquals(72_000, StockBoard.ticksUntilRestock(0, 3));
        assertEquals(1, StockBoard.ticksUntilRestock(71_999, 3));
        assertEquals(72_000, StockBoard.ticksUntilRestock(72_000, 3), "at the very tick of a restock a whole period lies ahead");
        assertEquals(24_000, StockBoard.ticksUntilRestock(0, 1));

        assertEquals(0, StockBoard.dayOf(23_999));
        assertEquals(1, StockBoard.dayOf(24_000));
        assertEquals(0, StockBoard.dayOf(-1));
    }

    @Test
    void findOnAnEmptyShelfOrNullIsNull() {
        assertNull(StockBoard.find(List.of(), "x"));
        assertNull(StockBoard.find(List.of(), null));
    }
}
