package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@code stock.json} is hand-edited, so the validator is tested with a good file and with deliberately bad ones (negative controls): each rule must reject exactly the
 * mistake it names, say which line, and leave the rest of the file loading. A rule that never fires would let a price of zero or a book nobody can name onto the shelf.
 */
class StockSchemaTest {
    private static String file(String... entries) {
        return "{\"entries\":[" + String.join(",", entries) + "]}";
    }

    private static final String GOOD_ROUTE = "{\"id\":\"fang\",\"kind\":\"route_book\",\"route\":\"nusmp:fire_dragon_slayer\",\"price_min\":2000000,\"price_max\":3000000,\"band_low\":0.6667,\"stock\":1,\"weight\":100,\"flagship\":true}";
    private static final String GOOD_SP = "{\"id\":\"archery\",\"kind\":\"sp_book\",\"sp_kind\":\"beginner_archery\",\"price_min\":120000,\"price_max\":180000,\"stock\":[3,5]}";

    private static boolean mentions(List<String> problems, String word) {
        for (String p : problems) if (p.contains(word)) return true;
        return false;
    }

    @Test
    void aGoodFileLoadsEveryLineWithItsDefaults() {
        StockSchema.Parsed p = StockSchema.parse(file(GOOD_ROUTE, GOOD_SP));
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        assertEquals(2, p.entries().size());
        StockEntry fang = p.entries().get(0);
        assertEquals("fang", fang.id());
        assertEquals(StockEntry.Kind.ROUTE_BOOK, fang.kind());
        assertEquals("nusmp:fire_dragon_slayer", fang.route());
        assertFalse(fang.allRoutes());
        assertEquals(2_000_000L, fang.priceMin());
        assertEquals(3_000_000L, fang.priceMax());
        assertEquals(0.6667, fang.bandLow(), 1e-9);
        assertTrue(fang.flagship());
        assertEquals(1, fang.stockMin());
        assertEquals(1, fang.stockMax());
        StockEntry sp = p.entries().get(1);
        assertEquals(StockEntry.Kind.SP_BOOK, sp.kind());
        assertEquals("beginner_archery", sp.spKind());
        assertEquals(3, sp.stockMin());
        assertEquals(5, sp.stockMax());
        assertEquals(100, sp.weight(), "the default weight: always on the shelf");
        assertEquals(0.0, sp.bandLow(), 0.0);
        assertFalse(sp.flagship());
    }

    @Test
    void theAllRoutesLineIsAccepted() {
        StockSchema.Parsed p = StockSchema.parse(file("{\"id\":\"books\",\"kind\":\"route_book\",\"routes\":\"all\",\"price_min\":2000000,\"price_max\":3000000}"));
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        assertTrue(p.entries().get(0).allRoutes());
        assertEquals("", p.entries().get(0).route());
    }

    @Test
    void notJsonAndWrongShapesAreReportedNotThrown() {
        assertFalse(StockSchema.parse("this is { not json").problems().isEmpty());
        assertTrue(StockSchema.parse("this is { not json").entries().isEmpty());
        assertTrue(mentions(StockSchema.parse("[]").problems(), "\"entries\""));
        assertTrue(mentions(StockSchema.parse("{\"entries\":{}}").problems(), "\"entries\""));
        assertTrue(mentions(StockSchema.parse((String) null).problems(), "\"entries\""));
        assertTrue(mentions(StockSchema.parse(file("5")).problems(), "must be an object"));
    }

    @Test
    void eachRuleRejectsExactlyItsMistake() {
        String[][] bad = {
                {"{\"id\":\"Bad Id\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2}", "\"id\""},
                {"{\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2}", "\"id\""},
                {"{\"id\":\"a\",\"kind\":\"potion\",\"price_min\":1,\"price_max\":2}", "\"kind\""},
                {"{\"id\":\"a\",\"kind\":\"route_book\",\"price_min\":1,\"price_max\":2}", "exactly one of"},
                {"{\"id\":\"a\",\"kind\":\"route_book\",\"route\":\"nusmp:x\",\"routes\":\"all\",\"price_min\":1,\"price_max\":2}", "exactly one of"},
                {"{\"id\":\"a\",\"kind\":\"route_book\",\"routes\":\"some\",\"price_min\":1,\"price_max\":2}", "can only be \"all\""},
                {"{\"id\":\"a\",\"kind\":\"route_book\",\"route\":\"Not A Route\",\"price_min\":1,\"price_max\":2}", "\"route\""},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"price_min\":1,\"price_max\":2}", "sp_kind"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":0,\"price_max\":2}", "between 1 and"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":-5,\"price_max\":2}", "between 1 and"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":9999999999999}", "between 1 and"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":9,\"price_max\":2}", "above price_max"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1.5,\"price_max\":2}", "whole number"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":\"1\",\"price_max\":2}", "whole number"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_max\":2}", "\"price_min\" is missing"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"band_low\":1}", "band_low"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"band_low\":-0.1}", "band_low"},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"stock\":0}", "\"stock\""},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"stock\":100}", "\"stock\""},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"stock\":[5,3]}", "\"stock\""},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"stock\":[1,2,3]}", "\"stock\""},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"weight\":101}", "\"weight\""},
                {"{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"weight\":-1}", "\"weight\""},
        };
        for (String[] b : bad) {
            StockSchema.Parsed p = StockSchema.parse(file(b[0]));
            assertTrue(p.entries().isEmpty(), "this line must be rejected: " + b[0]);
            assertTrue(mentions(p.problems(), b[1]), "the problem should say '" + b[1] + "' for " + b[0] + " but said " + p.problems());
        }
    }

    @Test
    void aBadLineDoesNotStopTheGoodOnes() {
        StockSchema.Parsed p = StockSchema.parse(file("{\"id\":\"a\",\"kind\":\"potion\"}", GOOD_SP, "7", GOOD_ROUTE));
        assertEquals(2, p.entries().size());
        assertEquals("archery", p.entries().get(0).id());
        assertEquals("fang", p.entries().get(1).id());
        assertEquals(2, p.problems().size(), p.problems().toString());
    }

    @Test
    void duplicatesAreCaught() {
        StockSchema.Parsed p = StockSchema.parse(file(GOOD_SP, GOOD_SP));
        assertEquals(1, p.entries().size());
        assertTrue(mentions(p.problems(), "used by an earlier line"));

        String sameRoute = "{\"id\":\"fang2\",\"kind\":\"route_book\",\"route\":\"nusmp:fire_dragon_slayer\",\"price_min\":1,\"price_max\":2}";
        p = StockSchema.parse(file(GOOD_ROUTE, sameRoute));
        assertEquals(1, p.entries().size());
        assertTrue(mentions(p.problems(), "already has a line"));

        String all1 = "{\"id\":\"all1\",\"kind\":\"route_book\",\"routes\":\"all\",\"price_min\":1,\"price_max\":2}";
        String all2 = "{\"id\":\"all2\",\"kind\":\"route_book\",\"routes\":\"all\",\"price_min\":1,\"price_max\":2}";
        p = StockSchema.parse(file(all1, all2));
        assertEquals(1, p.entries().size());
        assertTrue(mentions(p.problems(), "only one line may stock all routes"));
    }

    @Test
    void anUnknownKeyOnlyWarns() {
        StockSchema.Parsed p = StockSchema.parse(file("{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2,\"prise_max\":9}"));
        assertEquals(1, p.entries().size(), "the line is still usable");
        assertTrue(mentions(p.problems(), "unknown key 'prise_max'"));
    }

    @Test
    void keysThatStartWithAnUnderscoreAreCommentsAndNeverAProblem() {
        StockSchema.Parsed p = StockSchema.parse("{\"_why\":\"the file explains itself\",\"entries\":[{\"_why\":\"and so does a line\",\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"magic_booster\","
                + "\"price_min\":10,\"price_max\":20,\"stock\":1,\"_note\":3}]}");
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        assertEquals(1, p.entries().size());
    }

    @Test
    void conflictingKindKeysWarn() {
        StockSchema.Parsed p = StockSchema.parse(file("{\"id\":\"a\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"route\":\"nusmp:y\",\"price_min\":1,\"price_max\":2}"));
        assertEquals(1, p.entries().size());
        assertTrue(mentions(p.problems(), "mean nothing on an SP book"));
    }

    @Test
    void onlyTheFirstSixtyFourLinesAreRead() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < StockSchema.MAX_ENTRIES + 3; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"id\":\"e").append(i).append("\",\"kind\":\"sp_book\",\"sp_kind\":\"x\",\"price_min\":1,\"price_max\":2}");
        }
        StockSchema.Parsed p = StockSchema.parse("{\"entries\":[" + sb + "]}");
        assertEquals(StockSchema.MAX_ENTRIES, p.entries().size());
        assertTrue(mentions(p.problems(), "only the first 64"));
    }
}
