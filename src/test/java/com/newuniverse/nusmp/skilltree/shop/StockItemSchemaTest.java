package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the ITEM kind is what the shop sells now, and its lines are hand-edited like the old ones, so the validator is tested with a good line, with every mistake it names (negative controls) and
 * with the shop TYPE rule (a line outside the shop's type is rejected), and the shipped files are read exactly as the game reads them: the witch's {@code stock.json} against the real
 * {@code types.json}, with no problem, a shelf of a sensible size, only tags and items that make sense for a general store.
 */
class StockItemSchemaTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/black_market");

    private static String shop(String type, String... entries) {
        return "{" + (type == null ? "" : "\"type\":\"" + type + "\",") + "\"entries\":[" + String.join(",", entries) + "]}";
    }

    private static String item(String id, String ref, String extra) {
        return "{\"id\":\"" + id + "\",\"kind\":\"item\",\"item\":\"" + ref + "\",\"price_min\":1000,\"price_max\":2000" + (extra.isEmpty() ? "" : "," + extra) + "}";
    }

    private static ShopType.Parsed types() throws IOException {
        ShopType.Parsed t = ShopType.parse(Files.readString(DIR.resolve("types.json")));
        assertTrue(t.problems().isEmpty(), t.problems().toString());
        return t;
    }

    private static boolean mentions(List<String> problems, String word) {
        for (String p : problems) if (p.contains(word)) return true;
        return false;
    }

    @Test
    void aGoodItemLineKeepsEveryField() {
        StockSchema.Parsed p = StockSchema.parse(shop(null, "{\"id\":\"ingots\",\"kind\":\"item\",\"item\":\"#c:ingots\",\"exclude\":[\"minecraft:netherite_ingot\"],\"count\":[8,32],\"price_min\":3000,\"price_max\":5000,\"stock\":[2,4],\"weight\":90,\"flagship\":true}",
                item("pearl", "minecraft:ender_pearl", "")));
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        StockEntry e = p.entries().get(0);
        assertEquals(StockEntry.Kind.ITEM, e.kind());
        assertEquals("#c:ingots", e.item());
        assertTrue(e.isTag());
        assertEquals("c:ingots", e.itemOrTagId());
        assertEquals(List.of("minecraft:netherite_ingot"), e.exclude());
        assertEquals(8, e.countMin());
        assertEquals(32, e.countMax());
        assertEquals(3000L, e.priceMin());
        assertEquals(2, e.stockMin());
        assertEquals(4, e.stockMax());
        assertEquals(90, e.weight());
        assertTrue(e.flagship());
        StockEntry single = p.entries().get(1);
        assertFalse(single.isTag());
        assertEquals("minecraft:ender_pearl", single.itemOrTagId());
        assertEquals(1, single.countMin(), "default: one item per purchase");
        assertEquals(1, single.countMax());
        assertTrue(single.exclude().isEmpty());
    }

    @Test
    void theOldKindsAreStillThereAndTheNewOneIsAppended() {
        assertEquals(StockEntry.Kind.ROUTE_BOOK, StockEntry.Kind.values()[0]);
        assertEquals(StockEntry.Kind.SP_BOOK, StockEntry.Kind.values()[1]);
        assertEquals(StockEntry.Kind.ITEM, StockEntry.Kind.values()[2], "enum entries are only appended (the ordinal travels in the shop packet)");
        assertEquals(StockEntry.Kind.ITEM, StockEntry.Kind.fromId("item"));
        assertEquals(3, StockEntry.Kind.values().length);
    }

    @Test
    void eachItemRuleRejectsExactlyItsMistake() {
        String[][] bad = {
                {"{\"id\":\"a\",\"kind\":\"item\",\"price_min\":1,\"price_max\":2}", "\"item\""},
                {item("a", "diamond", ""), "\"item\""},                                                        // no namespace
                {item("a", "Minecraft:Diamond", ""), "\"item\""},                                              // upper case
                {item("a", "#", ""), "\"item\""},
                {item("a", "minecraft:diamond", "\"count\":0"), "\"count\""},
                {item("a", "minecraft:diamond", "\"count\":65"), "\"count\""},
                {item("a", "minecraft:diamond", "\"count\":[5,2]"), "\"count\""},
                {item("a", "minecraft:diamond", "\"count\":1.5"), "\"count\""},
                {item("a", "minecraft:diamond", "\"count\":\"many\""), "\"count\""},
                {"{\"id\":\"a\",\"kind\":\"item\",\"item\":\"minecraft:diamond\",\"price_min\":0,\"price_max\":2}", "prices must be between"},
                {"{\"id\":\"a\",\"kind\":\"item\",\"item\":\"minecraft:diamond\",\"price_min\":5,\"price_max\":2}", "above price_max"},
        };
        for (String[] b : bad) {
            StockSchema.Parsed p = StockSchema.parse(shop(null, b[0]));
            assertTrue(p.entries().isEmpty(), b[0] + " should be skipped");
            assertTrue(mentions(p.problems(), b[1]), b[0] + " should complain about '" + b[1] + "' but said " + p.problems());
        }
    }

    @Test
    void softMistakesWarnAndKeepTheLine() {
        StockSchema.Parsed p = StockSchema.parse(shop(null, item("a", "minecraft:diamond", "\"exclude\":\"minecraft:dirt\""), item("b", "minecraft:diamond", "\"exclude\":[\"Bad\", 5, \"minecraft:dirt\"]"),
                item("c", "minecraft:diamond", "\"route\":\"nusmp:x\"")));
        assertEquals(3, p.entries().size(), "a bad exclude entry or a stray key only warns");
        assertEquals(List.of("minecraft:dirt"), p.entries().get(1).exclude());
        assertTrue(mentions(p.problems(), "\"exclude\" must be an array"));
        assertTrue(mentions(p.problems(), "is not an item id"));
        assertTrue(mentions(p.problems(), "mean nothing on an item line"));
        StockSchema.Parsed book = StockSchema.parse(shop(null, "{\"id\":\"fang\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"item\":\"minecraft:dirt\"}"));
        assertEquals(1, book.entries().size());
        assertTrue(mentions(book.problems(), "mean nothing on a book line"));
    }

    @Test
    void excludeIsCutAtTheLimit() {
        StringBuilder many = new StringBuilder("\"exclude\":[");
        for (int i = 0; i < StockSchema.MAX_EXCLUDE + 5; i++) many.append(i == 0 ? "" : ",").append("\"minecraft:item_").append(i).append("\"");
        StockSchema.Parsed p = StockSchema.parse(shop(null, item("a", "#c:ores", many + "]")));
        assertEquals(StockSchema.MAX_EXCLUDE, p.entries().get(0).exclude().size());
        assertTrue(mentions(p.problems(), "cut at"));
    }

    @Test
    void aLineOutsideTheShopsTypeIsRejected() throws IOException {
        ShopType.Parsed types = types();
        // a food shop
        StockSchema.ShopFile f = StockSchema.parseShop(shop("food", item("bread", "#c:foods", ""), item("cooked", "#c:foods/cooked_meat", ""), item("ore", "#c:ores", ""),
                item("sword", "minecraft:diamond_sword", ""), item("cane", "minecraft:sugar", "")), types, null);
        assertEquals("food", f.type());
        assertEquals(List.of("bread", "cooked", "sword", "cane"), f.entries().stream().map(StockEntry::id).toList(),
                "a tag child and an allowed id pass; an ore tag is rejected; a single item whose tags are not known yet passes the load check (the pool filter decides in the world)");
        assertTrue(mentions(f.problems(), "'ore'") && mentions(f.problems(), "not something a 'food' shop sells"), f.problems().toString());
        // with the tags known the sword is rejected too
        StockSchema.ShopFile known = StockSchema.parseShop(shop("food", item("sword", "minecraft:diamond_sword", "")), types, id -> List.of("minecraft:swords"));
        assertTrue(known.entries().isEmpty());
        assertTrue(mentions(known.problems(), "'sword'"));
        // the general store takes it all
        StockSchema.ShopFile general = StockSchema.parseShop(shop("general", item("ore", "#c:ores", ""), item("sword", "minecraft:diamond_sword", "")), types, null);
        assertEquals(2, general.entries().size());
        assertTrue(general.problems().isEmpty(), general.problems().toString());
        // a banned id is rejected even by the general store
        StockSchema.ShopFile banned = StockSchema.parseShop(shop("general", item("block", "minecraft:command_block", ""), item("catalyst", "tensura:mithril_ingot", "")), types, null);
        assertTrue(banned.entries().isEmpty(), "creative-only blocks and the upgrade catalysts are never for sale");
    }

    @Test
    void aShopWithoutATypeIsTheGeneralStoreAndAnUnknownTypeSellsNoItems() throws IOException {
        ShopType.Parsed types = types();
        StockSchema.ShopFile none = StockSchema.parseShop(shop(null, item("ore", "#c:ores", "")), types, null);
        assertEquals("general", none.type());
        assertEquals(1, none.entries().size());
        StockSchema.ShopFile unknown = StockSchema.parseShop(shop("blacksmithing", item("ore", "#c:ores", "")), types, null);
        assertEquals("blacksmithing", unknown.type());
        assertTrue(unknown.entries().isEmpty());
        assertTrue(mentions(unknown.problems(), "not in types.json"));
        StockSchema.ShopFile oldBook = StockSchema.parseShop(shop("blacksmithing", "{\"id\":\"fang\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2}"), types, null);
        assertEquals(1, oldBook.entries().size(), "only item lines depend on the type");
        StockSchema.ShopFile notString = StockSchema.parseShop("{\"type\":5,\"entries\":[]}", types, null);
        assertEquals("general", notString.type());
        assertTrue(mentions(notString.problems(), "\"type\" must be a string"));
        assertTrue(StockSchema.parseShop("not json", types, null).entries().isEmpty());
        assertEquals("general", StockSchema.parseShop(null, types, null).type());
    }

    @Test
    void theShippedWitchFileReadsCleanAsAGeneralShopOfASensibleSize() throws IOException {
        StockSchema.ShopFile f = StockSchema.parseShop(Files.readString(DIR.resolve("stock.json")), types(), null);
        assertTrue(f.problems().isEmpty(), f.problems().toString());
        assertEquals("general", f.type());
        assertTrue(f.entries().size() >= 12 && f.entries().size() <= 20, "a shelf of about twelve to twenty slots, got " + f.entries().size());
        assertTrue(f.entries().size() <= StockBoard.MAX_SLOTS, "every line can become a slot");
        Set<String> ids = new HashSet<>();
        int tags = 0;
        int singles = 0;
        for (StockEntry e : f.entries()) {
            assertEquals(StockEntry.Kind.ITEM, e.kind(), e.id());
            assertTrue(ids.add(e.id()), "duplicate id " + e.id());
            assertTrue(e.priceMin() >= 500, e.id() + ": nothing is given away");
            assertTrue(e.stockMax() <= 6, e.id());
            assertTrue(e.countMax() <= 64, e.id());
            assertTrue(e.weight() >= 1 && e.weight() <= 100, e.id());
            if (e.isTag()) tags++;
            else singles++;
        }
        assertTrue(tags >= 6, "mostly convention-tag pools (" + tags + ")");
        assertTrue(singles >= 6, "and some explicit goods (" + singles + ")");
        long flagships = f.entries().stream().filter(StockEntry::flagship).count();
        assertTrue(flagships >= 1 && flagships <= 3, "one or two showpieces");
    }

    @Test
    void everyTagTheWitchNamesIsAConventionTag() throws IOException {
        // the tags are looked up in the real game by the self-test; here: only vanilla and "c" tags, which exist in every modpack, never a guessed mod tag
        StockSchema.ShopFile f = StockSchema.parseShop(Files.readString(DIR.resolve("stock.json")), types(), null);
        for (StockEntry e : f.entries()) {
            if (e.isTag()) assertTrue(e.itemOrTagId().startsWith("c:") || e.itemOrTagId().startsWith("minecraft:"), e.id() + " names the tag " + e.item());
        }
    }
}
