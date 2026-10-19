package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the two files the game ships ({@code stock.json}, {@code prices.json}) ARE the owner's economy, so they are tested as the specification they implement: they load with
 * no problem at all, the flagship sits where the owner put it (the Ember Knight book in the top third of 2 to 3 million), the primers carry the owner's bands, the price
 * table has the owner's defaults, bulk selling never reaches a valuable, and every {@code nusmp:} item the witch buys really is registered by the mod (read from the source,
 * because the item classes import the game and cannot be loaded here).
 */
class ShopDataFilesTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/black_market");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(Path p) throws IOException {
        assertTrue(Files.isRegularFile(p), "missing file " + p.toAbsolutePath());
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private static Map<String, StockEntry> stock() throws IOException {
        StockSchema.Parsed p = StockSchema.parse(read(DIR.resolve("stock.json")));
        assertTrue(p.problems().isEmpty(), "stock.json has problems: " + p.problems());
        Map<String, StockEntry> byId = new HashMap<>();
        for (StockEntry e : p.entries()) byId.put(e.id(), e);
        assertEquals(p.entries().size(), byId.size());
        return byId;
    }

    private static PriceTable prices() throws IOException {
        PriceTable.Parsed p = PriceTable.parse(read(DIR.resolve("prices.json")));
        assertTrue(p.problems().isEmpty(), "prices.json has problems: " + p.problems());
        return p.table();
    }

    @Test
    void theFlagshipIsTheFangInTheTopThirdOfTwoToThreeMillion() throws IOException {
        StockEntry fang = stock().get("fang_of_smoldering_insanity");
        assertNotNull(fang);
        assertEquals("nusmp:smoldering_fang_of_insanity", fang.route());      // 1.81.0: the Fang is its own route, not the Ember Knight
        assertEquals(StockEntry.Kind.ROUTE_BOOK, fang.kind());
        assertEquals(2_000_000L, fang.priceMin());
        assertEquals(3_000_000L, fang.priceMax());
        assertTrue(fang.bandStart() >= 2_000_000 + 1_000_000 * (2.0 / 3.0), "the owner asked for the top third of the band");
        assertTrue(fang.flagship());
        assertEquals(1, fang.stockMin());
        assertEquals(1, fang.stockMax());
        assertEquals(100, fang.weight(), "the owner's named target is always on the shelf");
    }

    @Test
    void everyOtherRouteIsOnSaleAtTheSameBandThroughTheAllRoutesLine() throws IOException {
        StockEntry all = stock().get("route_books");
        assertNotNull(all);
        assertTrue(all.allRoutes());
        assertEquals(2_000_000L, all.priceMin());
        assertEquals(3_000_000L, all.priceMax());
        assertEquals(1, all.stockMax(), "one copy of each route book per restock");
    }

    @Test
    void theSkillPointPrimersAreGoneFromTheShelf() throws IOException {
        for (StockEntry e : stock().values()) assertTrue(e.kind() != StockEntry.Kind.SP_BOOK, e.id() + ": the SP Books were removed with the Skill Points");
        assertEquals(2, stock().size(), "the flagship and the all-books line");
    }

    @Test
    void thePriceTableHasTheOwnersDefaults() throws IOException {
        PriceTable t = prices();
        Object[][] want = {
                {"nusmp:magic_stone", 50_000L}, {"nusmp:raw_monster_meat", 4_000L}, {"minecraft:diamond", 200_000L}, {"minecraft:emerald", 60_000L},
                {"minecraft:nether_star", 1_500_000L}, {"minecraft:netherite_scrap", 400_000L}, {"minecraft:gold_ingot", 25_000L},
                {"minecraft:enchanted_golden_apple", 600_000L}};
        for (Object[] w : want) {
            assertNotNull(t.get((String) w[0]), (String) w[0]);
            assertEquals((long) (Long) w[1], t.get((String) w[0]).gold(), (String) w[0]);
        }
        assertNotNull(t.get("minecraft:ancient_debris"));
        assertTrue(t.get("minecraft:ancient_debris").gold() <= t.get("minecraft:netherite_scrap").gold(), "raw ore is never worth more than the scrap it smelts into");
    }

    @Test
    void bulkSellingNeverReachesAValuable() throws IOException {
        PriceTable t = prices();
        assertEquals(Set.of("stones", "meat", "materials"), t.bulkCategories());
        for (Map.Entry<String, PriceTable.Price> e : t.rows().entrySet()) {
            if (t.isBulk(e.getValue().category())) assertTrue(e.getValue().gold() <= 400_000L, e.getKey() + " is too valuable for a Sell-all button");
        }
        assertTrue(!t.isBulk(t.get("minecraft:nether_star").category()), "a nether star is sold on purpose, one at a time");
        assertTrue(!t.isBulk(t.get("minecraft:diamond").category()));
    }

    @Test
    void everyModItemTheWitchBuysIsRegisteredInTheSource() throws IOException {
        String registry = read(SRC.resolve("item/NUItems.java")) + read(SRC.resolve("dungeon/DungeonFood.java"));
        for (String id : prices().rows().keySet()) {
            if (!id.startsWith("nusmp:")) continue;
            String path = id.substring("nusmp:".length());
            assertTrue(registry.contains("\"" + path + "\""), id + " is not registered by NUItems or DungeonFood");
        }
    }

    @Test
    void everyPriceIsAMarketNumberTheScreenCanShow() throws IOException {
        for (Map.Entry<String, PriceTable.Price> e : prices().rows().entrySet()) {
            long g = e.getValue().gold();
            assertTrue(g >= 1 && g <= PriceTable.MAX_PRICE, e.getKey());
            assertEquals(g, StockRoll.nice(g), e.getKey() + " should be a round market number");
        }
    }
}
