package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the rules that need ALL the files at once (two routes may not share a book, a class or a skill; the number and total size of the routes are capped so the sync packet always fits) can
 * only be proved against a set of files, and the snapshot's lookups are what every other system calls. These tests build snapshots from small synthetic files and check that the lookups are
 * right, that the order in which the game happens to list the files never decides which of two clashing routes survives, that every refusal says who it clashed with, that a file that is not
 * even JSON is reported, and that the caps hold at their edges.
 */
class RouteIndexTest {
    /** A tiny valid route: one root node, optionally granting a skill. */
    private static JsonObject small(String name, String book, String classId, String skill) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("book", book);
        o.addProperty("class_id", classId);
        o.addProperty("color", "#102030");
        JsonArray nodes = new JsonArray();
        JsonObject root = new JsonObject();
        root.addProperty("id", "root");
        if (skill != null) root.addProperty("skill", skill);
        nodes.add(root);
        o.add("nodes", nodes);
        return o;
    }

    private static Map<String, JsonElement> files(Object... idAndJson) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        for (int i = 0; i < idAndJson.length; i += 2) m.put((String) idAndJson[i], (JsonElement) idAndJson[i + 1]);
        return m;
    }

    private static RouteIndex.Entry entry(RouteIndex idx, String id) {
        for (RouteIndex.Entry e : idx.report()) if (e.id().equals(id)) return e;
        throw new AssertionError("no report entry for " + id);
    }

    // ------------------------------------------------------------------ lookups
    @Test
    void theLookupsFindARouteByIdBookClassAndSkill() {
        RouteIndex idx = RouteIndex.build(files(
                "nusmp:frost_knight", small("Frost Knight", "nusmp:frost_knight_book", "frost_knight", "nusmp:test_lance"),
                "nusmp:ember_knight", JsonParser.parseString(RouteSchemaTest.GOOD)), List.of());
        assertEquals(2, idx.all().size());
        assertEquals("nusmp:ember_knight", idx.all().get(0).id(), "routes come in id order, not insertion order");
        assertEquals("nusmp:frost_knight", idx.all().get(1).id());
        assertNotNull(idx.byId("nusmp:frost_knight"));
        assertEquals("nusmp:frost_knight", idx.byBook("nusmp:frost_knight_book").id());
        assertEquals("nusmp:ember_knight", idx.byClass("ember_knight").id());
        String[] hit = idx.bySkill("nusmp:test_roar");
        assertNotNull(hit);
        assertEquals("nusmp:ember_knight", hit[0]);
        assertEquals("roar", hit[1]);
        hit[0] = "tampered";
        assertEquals("nusmp:ember_knight", idx.bySkill("nusmp:test_roar")[0], "the snapshot hands out copies, so a caller cannot change it");
        assertNull(idx.byId("nusmp:nothing"));
        assertNull(idx.byBook("nusmp:nothing"));
        assertNull(idx.byClass("nothing"));
        assertNull(idx.bySkill("nusmp:nothing"));
        assertNull(idx.byId(null));
        assertNull(idx.bySkill(null));
        assertNull(idx.bySkill(""), "a node that grants nothing is not indexed under the empty skill");
        assertEquals(0, idx.problemFiles());
        assertTrue(idx.bytes() > 0);
    }

    @Test
    void theEmptySnapshotHasNothing() {
        assertTrue(RouteIndex.EMPTY.isEmpty());
        assertTrue(RouteIndex.EMPTY.all().isEmpty());
        assertTrue(RouteIndex.EMPTY.report().isEmpty());
        assertEquals(0, RouteIndex.EMPTY.problemFiles());
        assertNull(RouteIndex.EMPTY.byId("nusmp:x"));
        RouteIndex none = RouteIndex.build(new LinkedHashMap<>(), null);
        assertTrue(none.isEmpty());
    }

    // ------------------------------------------------------------------ the rules that need every file
    @Test
    void theLaterRouteLosesWhenTwoShareABookAndNeverTheOrderOfListing() {
        JsonObject a = small("A", "nusmp:shared_book", "alpha", null);
        JsonObject b = small("B", "nusmp:shared_book", "beta", null);
        for (boolean reversed : new boolean[]{false, true}) {
            Map<String, JsonElement> m = reversed ? files("nusmp:b_route", b, "nusmp:a_route", a) : files("nusmp:a_route", a, "nusmp:b_route", b);
            RouteIndex idx = RouteIndex.build(m, List.of());
            assertEquals(1, idx.all().size(), "reversed=" + reversed);
            assertEquals("nusmp:a_route", idx.all().get(0).id(), "the earlier id keeps the book");
            RouteIndex.Entry lost = entry(idx, "nusmp:b_route");
            assertFalse(lost.loaded());
            assertEquals(0, lost.nodes());
            assertTrue(String.join("|", lost.problems()).contains("its book 'nusmp:shared_book' is already the book of route 'nusmp:a_route'"), String.join("|", lost.problems()));
            assertEquals(1, idx.problemFiles());
        }
    }

    @Test
    void twoRoutesMayNotShareAClassIdOrASkill() {
        RouteIndex classes = RouteIndex.build(files(
                "nusmp:a_route", small("A", "nusmp:a_book", "same_class", null),
                "nusmp:b_route", small("B", "nusmp:b_book", "same_class", null)), List.of());
        assertEquals(1, classes.all().size());
        assertTrue(String.join("|", entry(classes, "nusmp:b_route").problems()).contains("class_id 'same_class' is already used by route 'nusmp:a_route'"));

        RouteIndex skills = RouteIndex.build(files(
                "nusmp:a_route", small("A", "nusmp:a_book", "alpha", "nusmp:test_shared"),
                "nusmp:b_route", small("B", "nusmp:b_book", "beta", "nusmp:test_shared")), List.of());
        assertEquals(1, skills.all().size());
        assertTrue(String.join("|", entry(skills, "nusmp:b_route").problems()).contains("grants 'nusmp:test_shared', which route 'nusmp:a_route' already grants"));

        RouteIndex fine = RouteIndex.build(files(
                "nusmp:a_route", small("A", "nusmp:a_book", "alpha", "nusmp:test_one"),
                "nusmp:b_route", small("B", "nusmp:b_book", "beta", "nusmp:test_two")), List.of());
        assertEquals(2, fine.all().size(), "the control: distinct books, classes and skills all load");
    }

    @Test
    void aRouteWithSkippedNodesStillLoadsAndTheReportSaysWhatWasSkipped() {
        JsonObject o = JsonParser.parseString(RouteSchemaTest.GOOD).getAsJsonObject();
        o.getAsJsonArray("nodes").get(1).getAsJsonObject().addProperty("cost", 0);       // roar is bad; the lotus needs it
        o.addProperty("maxLevel", 3);                                                    // and an unknown field
        RouteIndex idx = RouteIndex.build(files("nusmp:ember_knight", o), List.of());
        assertEquals(1, idx.all().size());
        RouteIndex.Entry e = entry(idx, "nusmp:ember_knight");
        assertTrue(e.loaded());
        assertFalse(e.clean());
        assertEquals(2, e.nodes(), "iron_fist and appetite are left");
        assertFalse(e.problems().isEmpty());
        assertFalse(e.warnings().isEmpty());
        assertEquals(1, idx.problemFiles());
    }

    @Test
    void aFileThatIsNotJsonIsReportedAndLoadsNothing() {
        RouteIndex idx = RouteIndex.build(files("nusmp:ok_route", small("Ok", "nusmp:ok_book", "ok", null)), List.of("nusmp:broken_route"));
        assertEquals(1, idx.all().size());
        RouteIndex.Entry broken = entry(idx, "nusmp:broken_route");
        assertFalse(broken.loaded());
        assertTrue(broken.problems().get(0).contains("not valid JSON"));
        assertEquals(2, idx.report().size());
        assertEquals("nusmp:broken_route", idx.report().get(0).id(), "the report is in id order");
        assertEquals(1, idx.problemFiles());
    }

    @Test
    void aFileThatIsNotAnObjectIsReportedAndSkipped() {
        RouteIndex idx = RouteIndex.build(files("nusmp:list_route", new JsonArray(), "nusmp:ok_route", small("Ok", "nusmp:ok_book", "ok", null)), List.of());
        assertEquals(List.of("nusmp:ok_route"), idx.all().stream().map(SkillRoute::id).toList());
        assertTrue(String.join("|", entry(idx, "nusmp:list_route").problems()).contains("must be a JSON object"));
    }

    // ------------------------------------------------------------------ the caps
    /** A route that is as big as the schema allows: 40 nodes with 12 tables of 10 numbers under 30-character names. */
    private static JsonObject big(String name, String book, String classId, int tables) {
        JsonObject o = small(name, book, classId, null);
        JsonArray nodes = new JsonArray();
        for (int i = 0; i < 40; i++) {
            JsonObject n = new JsonObject();
            n.addProperty("id", "n" + i);
            n.addProperty("x", i % 13);
            n.addProperty("y", i / 13);
            JsonObject values = new JsonObject();
            for (int t = 0; t < tables; t++) {
                JsonArray a = new JsonArray();
                for (int k = 0; k < 10; k++) a.add(1234.5678 + k);
                values.add(("t" + t + "_" + "q".repeat(28)).substring(0, 30), a);
            }
            n.add("values", values);
            nodes.add(n);
        }
        o.add("nodes", nodes);
        return o;
    }

    @Test
    void aRouteTooBigForOnePacketIsRefused() {
        RouteIndex idx = RouteIndex.build(files("nusmp:huge_route", big("Huge", "nusmp:huge_book", "huge", 16)), List.of());
        assertTrue(idx.isEmpty());
        String p = String.join("|", entry(idx, "nusmp:huge_route").problems());
        assertTrue(p.contains("is too big to send to players: ") && p.contains("the limit is 65536"), p);
    }

    @Test
    void allTheRoutesTogetherAreHeldUnderTheTotalCap() {
        // find a table count whose route fits one packet, then install more of them than the total allows
        int tables = 12;
        RouteSchema.Parsed probe = RouteSchema.parse("nusmp:probe", big("Probe", "nusmp:probe_book", "probe", tables));
        assertTrue(probe.clean(), String.join("|", probe.problems()));
        int size = RouteWire.size(probe.route());
        assertTrue(size > 20_000 && size <= RouteWire.MAX_ROUTE_BYTES, "the probe must be one big but legal route: " + size);
        assertTrue(RouteWire.MAX_ROUTE_BYTES * 2 > size && size * RouteWire.MAX_ROUTES > RouteWire.MAX_TOTAL_BYTES, "and 64 of them must not fit under the total, or this test proves nothing");
        int fit = RouteWire.MAX_TOTAL_BYTES / size;
        Map<String, JsonElement> m = new LinkedHashMap<>();
        for (int i = 0; i < fit + 2; i++) {
            String id = "nusmp:big_" + (char) ('a' + i / 26) + (char) ('a' + i % 26);
            m.put(id, big("Big " + i, "nusmp:big_book_" + i, "big_" + i, tables));
        }
        RouteIndex idx = RouteIndex.build(m, List.of());
        assertEquals(Math.min(fit, RouteWire.MAX_ROUTES), idx.all().size(), "as many as fit under the total, and no more than the route cap");
        assertTrue(idx.bytes() <= RouteWire.MAX_TOTAL_BYTES);
        boolean said = false;
        for (RouteIndex.Entry e : idx.report()) if (!e.loaded() && String.join("|", e.problems()).contains("all routes together")) said = true;
        assertTrue(said, "the refused routes say that all routes together are too many bytes");
    }

    @Test
    void noMoreThanSixtyFourRoutesAreLoaded() {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        for (int i = 0; i < 65; i++) {
            String id = "nusmp:r_" + (char) ('a' + i / 26) + (char) ('a' + i % 26);
            m.put(id, small("R" + i, "nusmp:book_" + i, "cls_" + i, null));
        }
        RouteIndex idx = RouteIndex.build(m, List.of());
        assertEquals(64, idx.all().size());
        int refused = 0;
        for (RouteIndex.Entry e : idx.report()) if (!e.loaded()) refused++;
        assertEquals(1, refused);
        assertTrue(String.join("|", entry(idx, "nusmp:r_cm").problems()).contains("more than 64 routes"), "the 65th by id order is the one that is left out");
    }

    // ------------------------------------------------------------------ the client's copy
    @Test
    void theClientCopyKeepsTheServersRoutesAndDropsDuplicatesFirstWins() {
        SkillRoute a = RouteSchema.parse("nusmp:a_route", small("A", "nusmp:a_book", "alpha", "nusmp:test_one")).route();
        SkillRoute dup = RouteSchema.parse("nusmp:b_route", small("B", "nusmp:a_book", "alpha", "nusmp:test_one")).route();
        RouteIndex idx = RouteIndex.of(List.of(a, dup));
        assertEquals(2, idx.all().size(), "the list is shown as the server sent it");
        assertSame(a, idx.byBook("nusmp:a_book"), "but a lookup by book finds the first");
        assertSame(a, idx.byClass("alpha"));
        assertEquals("nusmp:a_route", idx.bySkill("nusmp:test_one")[0]);
        assertEquals(0, idx.problemFiles());
        assertEquals(2, idx.report().size());
        assertNotEquals(a, dup);
    }

    @Test
    void theIdsOfTheRoutesAreTheNamespaceAndTheFileName() {
        RouteIndex idx = RouteIndex.build(files("othermod:frost_knight", small("Ice", "othermod:ice_book", "ice", null), "nusmp:frost_knight", small("Ice", "nusmp:ice_book", "ice2", null)), List.of());
        assertEquals(List.of("nusmp:frost_knight", "othermod:frost_knight"), idx.all().stream().map(SkillRoute::id).toList(), "the same file name in two namespaces are two routes");
        List<String> ids = new ArrayList<>();
        for (RouteIndex.Entry e : idx.report()) ids.add(e.id());
        assertEquals(List.of("nusmp:frost_knight", "othermod:frost_knight"), ids);
    }
}
