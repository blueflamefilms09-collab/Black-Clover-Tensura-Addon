package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the price table decides how fast a player can earn the millions a route book costs, and it is edited by hand, so the reader is tested with both row shapes, with the
 * mistakes a hand-edit makes (a price of zero, a decimal, a capital letter in an item id, a bad category), and with the limit on the number of rows.
 */
class PriceTableTest {
    private static boolean mentions(List<String> problems, String word) {
        for (String p : problems) if (p.contains(word)) return true;
        return false;
    }

    @Test
    void bothRowShapesAreRead() {
        PriceTable.Parsed p = PriceTable.parse("{\"prices\":{\"minecraft:diamond\":200000,\"nusmp:magic_stone\":{\"gold\":50000,\"category\":\"stones\"},\"minecraft:emerald\":{\"gold\":60000}}}");
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        PriceTable t = p.table();
        assertEquals(3, t.size());
        assertEquals(200_000L, t.get("minecraft:diamond").gold());
        assertEquals(PriceTable.DEFAULT_CATEGORY, t.get("minecraft:diamond").category());
        assertEquals("stones", t.get("nusmp:magic_stone").category());
        assertEquals(PriceTable.DEFAULT_CATEGORY, t.get("minecraft:emerald").category());
        assertNull(t.get("minecraft:dirt"));
        assertNull(t.get(null));
    }

    @Test
    void keysThatStartWithAnUnderscoreAreCommentsAtEveryLevelAndAreNotRows() {
        PriceTable.Parsed p = PriceTable.parse("{\"_why\":\"the file explains itself\",\"prices\":{\"_why\":\"so does the table\",\"minecraft:diamond\":{\"gold\":200000,\"_why\":\"and a row\"}}}");
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        assertEquals(1, p.table().size());
        assertEquals(200_000L, p.table().get("minecraft:diamond").gold());
        assertNull(p.table().get("_why"));
    }

    @Test
    void categoriesComeInFirstSeenOrder() {
        PriceTable t = PriceTable.parse("{\"prices\":{\"a:one\":{\"gold\":1,\"category\":\"meat\"},\"a:two\":{\"gold\":2,\"category\":\"stones\"},\"a:three\":{\"gold\":3,\"category\":\"meat\"}}}").table();
        assertEquals(List.of("meat", "stones"), new ArrayList<>(t.categories()));
        assertEquals(List.of("a:one", "a:two", "a:three"), new ArrayList<>(t.rows().keySet()), "file order is kept");
    }

    @Test
    void theMistakesOfAHandEditAreSkippedWithASentence() {
        String json = "{\"prices\":{"
                + "\"minecraft:ok\":10,"
                + "\"minecraft:zero\":0,"
                + "\"minecraft:negative\":-4,"
                + "\"minecraft:decimal\":1.5,"
                + "\"minecraft:text\":\"cheap\","
                + "\"minecraft:huge\":99999999999999,"
                + "\"Minecraft:Upper\":5,"
                + "\"no_namespace\":5,"
                + "\"minecraft:nogold\":{\"category\":\"stones\"},"
                + "\"minecraft:badcat\":{\"gold\":7,\"category\":\"Not Snake\"},"
                + "\"minecraft:extra\":{\"gold\":8,\"colour\":\"red\"}"
                + "}}";
        PriceTable.Parsed p = PriceTable.parse(json);
        PriceTable t = p.table();
        assertNotNull(t.get("minecraft:ok"));
        assertNull(t.get("minecraft:zero"));
        assertNull(t.get("minecraft:negative"));
        assertNull(t.get("minecraft:decimal"));
        assertNull(t.get("minecraft:text"));
        assertNull(t.get("minecraft:huge"));
        assertNull(t.get("Minecraft:Upper"));
        assertNull(t.get("no_namespace"));
        assertNull(t.get("minecraft:nogold"));
        assertEquals(PriceTable.DEFAULT_CATEGORY, t.get("minecraft:badcat").category(), "a bad category falls back, the price stays");
        assertEquals(8, t.get("minecraft:extra").gold(), "an unknown key only warns");
        assertTrue(mentions(p.problems(), "unknown key 'colour'"));
        assertTrue(mentions(p.problems(), "minecraft:zero"));
        assertTrue(mentions(p.problems(), "Minecraft:Upper"));
        assertTrue(mentions(p.problems(), "no_namespace"));
        assertTrue(mentions(p.problems(), "Not Snake") || mentions(p.problems(), "\"category\""));
    }

    @Test
    void wrongShapesAndBrokenJsonAreReportedNotThrown() {
        assertTrue(PriceTable.parse("{ nope").table().isEmpty());
        assertFalse(PriceTable.parse("{ nope").problems().isEmpty());
        assertTrue(mentions(PriceTable.parse("[]").problems(), "\"prices\""));
        assertTrue(mentions(PriceTable.parse("{\"prices\":[]}").problems(), "\"prices\""));
        assertTrue(mentions(PriceTable.parse((String) null).problems(), "\"prices\"") || !PriceTable.parse((String) null).problems().isEmpty());
        assertTrue(PriceTable.EMPTY.isEmpty());
    }

    @Test
    void onlyTheFirstFiveHundredAndTwelveRowsAreRead() {
        StringBuilder sb = new StringBuilder("{\"prices\":{");
        for (int i = 0; i < PriceTable.MAX_ROWS + 5; i++) {
            if (i > 0) sb.append(',');
            sb.append("\"x:item").append(i).append("\":").append(i + 1);
        }
        PriceTable.Parsed p = PriceTable.parse(sb.append("}}").toString());
        assertEquals(PriceTable.MAX_ROWS, p.table().size());
        assertTrue(mentions(p.problems(), "only the first 512"));
    }

    @Test
    void filteredDropsTheRowsTheGameDoesNotKnow() {
        PriceTable t = PriceTable.parse("{\"prices\":{\"minecraft:diamond\":5,\"othermod:gem\":9}}").table();
        PriceTable f = t.filtered(id -> id.startsWith("minecraft:"));
        assertEquals(1, f.size());
        assertNotNull(f.get("minecraft:diamond"));
        assertNull(f.get("othermod:gem"));
        assertEquals(2, t.size(), "the original is unchanged");
    }

    @Test
    void ofBuildsATableFromRows() {
        PriceTable t = PriceTable.of(java.util.Map.of("a:b", new PriceTable.Price(3, "x")));
        assertEquals(3, t.get("a:b").gold());
        assertEquals("x", t.get("a:b").category());
    }

    @Test
    void onlyTheListedCategoriesMayBeSoldInBulk() {
        PriceTable.Parsed p = PriceTable.parse("{\"bulk\":[\"stones\",\"meat\",\"nothing_here\",5,\"Bad Name\"],\"prices\":{"
                + "\"a:stone\":{\"gold\":1,\"category\":\"stones\"},\"a:meat\":{\"gold\":2,\"category\":\"meat\"},\"a:star\":{\"gold\":3,\"category\":\"treasure\"}}}");
        PriceTable t = p.table();
        assertTrue(t.isBulk("stones"));
        assertTrue(t.isBulk("meat"));
        assertFalse(t.isBulk("treasure"), "a category that is not listed is never sold in bulk");
        assertFalse(t.isBulk("nothing_here"), "a bulk name that no row uses is ignored");
        assertFalse(t.isBulk(null));
        assertEquals(List.of("stones", "meat"), new ArrayList<>(t.bulkCategories()));
        assertTrue(mentions(p.problems(), "nothing_here"));
        assertTrue(mentions(p.problems(), "Bad Name"));
        assertTrue(mentions(p.problems(), "5"));
        assertFalse(PriceTable.parse("{\"prices\":{\"a:b\":1}}").table().isBulk("goods"), "without a bulk list nothing is bulk");
        assertTrue(mentions(PriceTable.parse("{\"bulk\":\"stones\",\"prices\":{\"a:b\":1}}").problems(), "\"bulk\" must be an array"));
        assertTrue(t.filtered(id -> true).isBulk("stones"), "filtering rows keeps the bulk list");
        assertTrue(PriceTable.of(java.util.Map.of(), java.util.Set.of("x")).isBulk("x"));
    }
}
