package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: if the witch sold an item for less than she pays for it, one player could print gold forever and the economy would be worthless. This pins the rule in three ways: the pure functions
 * (negative controls included), the rule applied to the REAL data files (every item line's cheapest price is above her buy price for the same item; a pool holding every item she buys keeps none
 * she would lose on), and the margin the owner's numbers were chosen with.
 */
class ShopLoopGuardTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/black_market");

    private static PriceTable prices() throws IOException {
        PriceTable.Parsed p = PriceTable.parse(Files.readString(DIR.resolve("prices.json")));
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        return p.table();
    }

    private static List<StockEntry> stock() throws IOException {
        StockSchema.Parsed p = StockSchema.parse(Files.readString(DIR.resolve("stock.json")));
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        return p.entries();
    }

    @Test
    void sellingBackForAsMuchOrMoreIsALoop() {
        assertTrue(ShopLoopGuard.loops(1000, 1, 1000), "even money is a loop (no loss, endless churn, and rounding can tip it)");
        assertTrue(ShopLoopGuard.loops(1000, 1, 1500));
        assertTrue(ShopLoopGuard.loops(10_000, 8, 1300), "8 x 1300 = 10400 back for 10000 paid");
        assertFalse(ShopLoopGuard.loops(10_000, 8, 1200), "8 x 1200 = 9600 back for 10000 paid: she keeps a margin");
        assertFalse(ShopLoopGuard.loops(1000, 1, 0), "she pays nothing for it");
        assertFalse(ShopLoopGuard.loops(1000, 0, 5000), "no items, nothing to sell back");
        assertTrue(ShopLoopGuard.loops(5, 2, Long.MAX_VALUE), "no overflow on the way");
    }

    @Test
    void anItemIsSafeOnlyBelowTheLinesCheapestPrice() {
        assertTrue(ShopLoopGuard.safe(260_000, 200_000), "diamond sells at 260,000, she pays 200,000");
        assertFalse(ShopLoopGuard.safe(200_000, 200_000), "the same price is not safe");
        assertFalse(ShopLoopGuard.safe(9_000, 200_000), "a cheap line must not carry diamond");
        assertTrue(ShopLoopGuard.safe(1_000, 0), "an item she does not buy is always safe");
    }

    @Test
    void aPoolLosesTheItemsThatWouldLoopAndKeepsTheOrder() {
        List<StockBoard.PoolItem> pool = List.of(new StockBoard.PoolItem("minecraft:amethyst_shard", 64), new StockBoard.PoolItem("minecraft:diamond", 64),
                new StockBoard.PoolItem("minecraft:emerald", 64), new StockBoard.PoolItem("minecraft:quartz", 64));
        Map<String, Long> buy = Map.of("minecraft:diamond", 200_000L, "minecraft:emerald", 60_000L);
        List<StockBoard.PoolItem> safe = ShopLoopGuard.safePool(pool, 9_000, id -> buy.getOrDefault(id, 0L));
        assertEquals(List.of("minecraft:amethyst_shard", "minecraft:quartz"), safe.stream().map(StockBoard.PoolItem::id).toList());
        assertEquals(3, ShopLoopGuard.safePool(pool, 100_000, id -> buy.getOrDefault(id, 0L)).size(), "a dearer line keeps the emerald");
        assertEquals(4, ShopLoopGuard.safePool(pool, 260_000, id -> buy.getOrDefault(id, 0L)).size(), "a flagship line keeps all");
    }

    @Test
    void everyItemLineOfTheShippedShelfSellsAboveWhatTheWitchPays() throws IOException {
        PriceTable prices = prices();
        int checked = 0;
        for (StockEntry e : stock()) {
            if (e.kind() != StockEntry.Kind.ITEM || e.isTag()) continue;
            PriceTable.Price buy = prices.get(e.item());
            if (buy == null) continue;
            checked++;
            assertTrue(e.priceMin() > buy.gold(), e.id() + ": the cheapest sell price " + e.priceMin() + " must be above the buy price " + buy.gold() + " of " + e.item());
            assertTrue(e.priceMin() >= buy.gold() * 1.25, e.id() + ": keep a 25% margin over the buy price so rounding and scales cannot tip it");
            assertFalse(e.exclude().contains(e.item()));
        }
        assertTrue(checked >= 8, "the shelf sells several of the goods she buys, at a profit (checked " + checked + ")");
    }

    @Test
    void aPoolOfEverythingTheWitchBuysLeavesNothingTheShelfCouldLoopOn() throws IOException {
        PriceTable prices = prices();
        List<StockBoard.PoolItem> everything = new ArrayList<>();
        for (String id : prices.rows().keySet()) everything.add(new StockBoard.PoolItem(id, 64));
        everything.add(new StockBoard.PoolItem("minecraft:cobblestone", 64));
        for (StockEntry e : stock()) {
            if (e.kind() != StockEntry.Kind.ITEM) continue;
            for (StockBoard.PoolItem p : ShopLoopGuard.safePool(everything, e.priceMin(), id -> prices.get(id) == null ? 0 : prices.get(id).gold())) {
                PriceTable.Price buy = prices.get(p.id());
                assertTrue(buy == null || e.priceMin() > buy.gold(), e.id() + " would sell " + p.id() + " at a loss");
            }
        }
    }

    @Test
    void everyRolledOfferOfTheShippedShelfBeatsTheBuyPriceForEveryPoolItem() throws IOException {
        // the whole chain with the real numbers: every restock of a long run, every line, the rolled price of a stack against what she pays back for that stack
        PriceTable prices = prices();
        List<StockBoard.PoolItem> pool = new ArrayList<>();
        for (String id : prices.rows().keySet()) pool.add(new StockBoard.PoolItem(id, 64));
        int rolled = 0;
        for (StockEntry e : stock()) {
            if (e.kind() != StockEntry.Kind.ITEM) continue;
            List<StockBoard.PoolItem> safe = ShopLoopGuard.safePool(pool, e.priceMin(), id -> prices.get(id).gold());
            if (safe.isEmpty()) continue;
            StockBoard.Slot slot = new StockBoard.Slot(e.id(), new StockEntry(e.id(), e.kind(), "", false, "", e.priceMin(), e.priceMax(), e.bandLow(), e.stockMin(), e.stockMax(), 100, false,
                    e.item(), e.countMin(), e.countMax(), e.exclude()), "", safe);
            for (int period = 0; period < 200; period++) {
                for (StockBoard.Offer o : StockBoard.build(7L, period, 0, List.of(slot), id -> 0)) {
                    rolled++;
                    long paid = ShopRules.buyPrice(o.price(), 1.0);
                    long back = ShopRules.sellUnit(prices.get(o.ref()).gold(), 1.0);
                    assertFalse(ShopLoopGuard.loops(paid, o.amount(), back), e.id() + " period " + period + ": " + o.amount() + " x " + o.ref() + " for " + paid + " sells back at " + back + " each");
                }
            }
        }
        assertTrue(rolled > 100, "the sweep really rolled offers (" + rolled + ")");
    }
}
