package com.newuniverse.nusmp.elite.hub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Merchant's prices are data and the purchase check decides on the server, so the shipped file is parsed here (every row must read, the Dungeon Map must be on sale, all four of the brief's
 * kinds of ware must exist, a price is whole coins), the reader is shown to survive bad rows, and the purchase verdict is walked over every refusal in the order the player is told them.
 */
class ShopTableTest {
    private static ShopTable shipped() throws IOException {
        ShopTable.Parsed p = ShopTable.parse(Files.readString(Path.of("src/main/resources/data/nusmp/elite/hub/shop.json")));
        assertTrue(p.problems().isEmpty(), "the shipped shop reads clean: " + p.problems());
        return p.table();
    }

    @Test
    void theShippedShopSellsGearFoodUtilityAndTheDungeonMap() throws IOException {
        ShopTable t = shipped();
        assertTrue(t.size() >= 30, "a real shop, got " + t.size());
        for (String c : new String[] {"gear", "food", "utility", "map"}) assertTrue(t.categories().contains(c), "category " + c);
        ShopTable.Offer map = t.get("dungeon_map");
        assertNotNull(map, "the Dungeon Map is on sale");
        assertEquals("nusmp:dungeon_map", map.item());
        Set<String> ids = new HashSet<>();
        for (ShopTable.Offer o : t.offers()) {
            assertTrue(ids.add(o.id()));
            assertTrue(o.price() > 0, o.id() + " is not free");
            assertTrue(o.count() >= 1 && o.count() <= 64);
        }
        assertTrue(t.get("iron_sword").price() < t.get("diamond_sword").price(), "better gear costs more");
        assertTrue(t.get("diamond_sword").minFloor() > 0, "the best gear is locked behind depth");
    }

    @Test
    void badRowsAreSkippedWithASentence() {
        String json = "{\"offers\":[" +
                "{\"id\":\"ok\",\"item\":\"minecraft:bread\",\"count\":4,\"price\":40,\"category\":\"food\"}," +
                "{\"id\":\"Bad\",\"item\":\"minecraft:bread\",\"price\":1}," +
                "{\"id\":\"noitem\",\"item\":\"bread\",\"price\":1}," +
                "{\"id\":\"free_neg\",\"item\":\"minecraft:bread\",\"price\":-1}," +
                "{\"id\":\"huge_count\",\"item\":\"minecraft:bread\",\"price\":1,\"count\":500}," +
                "{\"id\":\"ok\",\"item\":\"minecraft:stick\",\"price\":2}," +
                "{\"id\":\"badcat\",\"item\":\"minecraft:stick\",\"price\":2,\"category\":\"Bad Cat!\"}," +
                "7]}";
        ShopTable.Parsed p = ShopTable.parse(json);
        assertEquals(1, p.table().size(), p.problems().toString());
        assertEquals(40L, p.table().get("ok").price());
        assertEquals(7, p.problems().size(), p.problems().toString());
        assertTrue(ShopTable.parse("nope").table().isEmpty());
        assertTrue(ShopTable.parse("[]").table().isEmpty());
        assertEquals(1, p.table().filtered(i -> i.equals("minecraft:bread")).size());
        assertEquals(0, p.table().filtered(i -> false).size());
    }

    @Test
    void thePriceIsScaledAndTheStockIsPerPlayer() {
        ShopTable.Offer o = new ShopTable.Offer("x", "minecraft:bread", 4, 40, "food", 3, 0);
        assertEquals(40L, ShopRules.price(o, 1.0));
        assertEquals(60L, ShopRules.price(o, 1.5));
        assertEquals(0L, ShopRules.price(o, -2));
        assertEquals(3, ShopRules.remaining(o, 0));
        assertEquals(1, ShopRules.remaining(o, 2));
        assertEquals(0, ShopRules.remaining(o, 9));
        assertEquals(-1, ShopRules.remaining(new ShopTable.Offer("y", "minecraft:bread", 1, 1, "food", 0, 0), 99), "no limit");
    }

    @Test
    void theVerdictTellsTheFirstThingInTheWay() {
        ShopTable.Offer o = new ShopTable.Offer("x", "minecraft:bread", 4, 40, "food", 2, 10);
        assertEquals(ShopRules.Verdict.SHOP_OFF, ShopRules.decide(false, o, 0, 99, 999, 1.0));
        assertEquals(ShopRules.Verdict.UNKNOWN_OFFER, ShopRules.decide(true, null, 0, 99, 999, 1.0));
        assertEquals(ShopRules.Verdict.LOCKED, ShopRules.decide(true, o, 0, 9, 999, 1.0));
        assertEquals(ShopRules.Verdict.SOLD_OUT, ShopRules.decide(true, o, 2, 10, 999, 1.0));
        assertEquals(ShopRules.Verdict.CANNOT_AFFORD, ShopRules.decide(true, o, 1, 10, 39, 1.0));
        assertEquals(ShopRules.Verdict.OK, ShopRules.decide(true, o, 1, 10, 40, 1.0));
        assertEquals(ShopRules.Verdict.CANNOT_AFFORD, ShopRules.decide(true, o, 1, 10, 40, 2.0), "the scale applies to the check too");
    }
}
