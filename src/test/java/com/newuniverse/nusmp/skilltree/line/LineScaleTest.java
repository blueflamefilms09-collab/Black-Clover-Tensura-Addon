package com.newuniverse.nusmp.skilltree.line;

import com.google.gson.JsonElement;
import com.newuniverse.nusmp.client.skilltree.layout.LineVisibility;
import com.newuniverse.nusmp.client.skilltree.layout.PassiveRows;
import com.newuniverse.nusmp.skilltree.data.RouteIndex;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.RouteWire;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.SimpleTreeState;
import org.junit.jupiter.api.Test;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: part F of the job: the engine was designed for 283 routes, and a design that only works for 8 is not a design. This test creates 300 lines in the generator's shape (written to a temporary folder as
 * real files and read back with the real parser, so the file handling is in the measurement), loads them through the registry's own builder, and checks the things that decide whether the scale rules
 * hold: the whole set loads clean in a sane time; a player who owns 3 lines receives a payload a few percent the size of the whole set while an owner of everything is cut into packets that each respect the
 * payload limits; the cards of all 300 lines are one small payload; the client registry holds the 3 owned trees in full and a node-less stub of the other 297; the library lists only the owned lines and
 * never so much as looks at the nodes of the others (a route whose node list explodes when touched proves it); and the Passives model finds the rows on screen in logarithmic time over thousands of rows.
 * The timings are printed so the report can quote them, and bounded generously so a slow machine does not make the build flaky.
 */
class LineScaleTest {
    private static final int LINES = 300;

    @Test
    void threeHundredLinesLoadFromFilesCleanAndFast() throws IOException {
        Path dir = Files.createTempDirectory("lines_scale");
        try {
            for (int i = 0; i < LINES; i++) Files.writeString(dir.resolve("scale_line_" + i + ".json"), LineFixtures.line(i).toString(), StandardCharsets.UTF_8);
            long t0 = System.nanoTime();
            Map<String, JsonElement> files = new LinkedHashMap<>();
            try (var s = Files.list(dir)) {
                for (Path f : s.sorted().toList()) {
                    String n = f.getFileName().toString();
                    files.put("nusmp:" + n.substring(0, n.length() - 5), com.google.gson.JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)));
                }
            }
            RouteIndex idx = RouteIndex.build(files, List.of(), RouteIndex.Caps.REGISTRY);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("[scale] " + LINES + " lines read and validated in " + ms + " ms; " + idx.bytes() + " bytes on the wire in all");
            assertEquals(LINES, idx.all().size());
            for (RouteIndex.Entry e : idx.report()) assertTrue(e.clean(), e.id() + ": " + e.problems());
            assertTrue(ms < 20_000, "loading 300 lines took " + ms + " ms");
            assertTrue(idx.bytes() > RouteWire.MAX_TOTAL_BYTES, "the whole set must be bigger than one classic payload, or the registry caps are not what this test is about");
        } finally {
            try (var s = Files.list(dir)) {
                for (Path f : s.toList()) Files.deleteIfExists(f);
            }
            Files.deleteIfExists(dir);
        }
    }

    private static List<SkillRoute> all() {
        List<SkillRoute> out = new ArrayList<>();
        for (int i = 0; i < LINES; i++) out.add(LineFixtures.route(i));
        return out;
    }

    @Test
    void anOwnerOfThreeLinesGetsAFractionOfWhatTheWholeSetWeighs() {
        List<SkillRoute> all = all();
        RouteIndex idx = RouteIndex.of(all);
        List<String> owned = List.of("nusmp:scale_line_4", "nusmp:scale_line_77", "nusmp:scale_line_299");
        List<SkillRoute> mine = LineSyncPlan.owned(owned, idx::byId);
        assertEquals(3, mine.size());
        List<List<SkillRoute>> three = LineSyncPlan.packets(mine);
        assertEquals(1, three.size(), "three lines are one packet");
        int threeBytes = 0;
        for (SkillRoute r : three.get(0)) threeBytes += RouteWire.size(r);
        List<List<SkillRoute>> every = LineSyncPlan.packets(all);
        int allBytes = 0, routes = 0;
        for (List<SkillRoute> p : every) {
            int b = 0;
            for (SkillRoute r : p) b += RouteWire.size(r);
            assertTrue(b <= LineSyncPlan.PAYLOAD_BYTES + RouteWire.MAX_ROUTE_BYTES && p.size() <= RouteWire.MAX_ROUTES, "a packet obeys both limits");
            assertTrue(b < 1_048_576, "and is under what a clientbound payload may carry");
            allBytes += b;
            routes += p.size();
        }
        assertEquals(LINES, routes);
        List<LineCard> cards = new ArrayList<>();
        for (SkillRoute r : all) cards.add(LineCard.of(r));
        int cardBytes = LineCard.size(cards);
        System.out.println("[scale] owner of 3 lines: " + threeBytes + " bytes in 1 packet; owner of all " + LINES + ": " + allBytes + " bytes in " + every.size() + " packets; the cards of all " + LINES + ": " + cardBytes + " bytes");
        assertTrue(threeBytes * 50 < allBytes, "an owner of 3 of 300 lines gets under 2 % of the bytes");
        assertTrue(cardBytes < LineSyncPlan.PAYLOAD_BYTES / 4, "the cards of every line are one small payload");
        assertTrue(cardBytes * 10 < allBytes, "and a tenth of the trees");
        assertTrue(every.size() > 1, "an owner of everything needs more than one packet");
    }

    @Test
    void theOwnedSignatureChangesOnlyWhenTheOwnedSetDoes() {
        int a = LineSyncPlan.signature(List.of("x:a", "x:b", "x:c"));
        assertEquals(a, LineSyncPlan.signature(List.of("x:c", "x:a", "x:b")), "order does not matter");
        assertTrue(a != LineSyncPlan.signature(List.of("x:a", "x:b")));
        assertTrue(a != LineSyncPlan.signature(List.of("x:a", "x:b", "x:d")));
        assertEquals(LineSyncPlan.signature(List.of()), LineSyncPlan.signature(List.of()));
    }

    @Test
    void theClientHoldsTheOwnedTreesInFullAndAStubOfEveryOtherLine() {
        RouteRegistry.clear();
        RouteRegistry.clearClient();
        try {
            List<SkillRoute> all = all();
            List<LineCard> cards = new ArrayList<>();
            for (SkillRoute r : all) cards.add(LineCard.of(r));
            RouteRegistry.setClientCards(cards);
            assertEquals(LINES, RouteRegistry.all().size());
            assertFalse(LineVisibility.isFullTree(RouteRegistry.byId("nusmp:scale_line_4")), "before the sync only the stub is there");
            assertEquals("Scale Book 4", RouteRegistry.byBook("nusmp:scale_line_4_book").bookName(), "a book can be named from its stub");
            RouteRegistry.setClient(List.of(all.get(4), all.get(77)));
            assertEquals(LINES, RouteRegistry.all().size(), "stubs and trees are one list");
            assertTrue(LineVisibility.isFullTree(RouteRegistry.byId("nusmp:scale_line_4")));
            assertTrue(LineVisibility.isFullTree(RouteRegistry.byId("nusmp:scale_line_77")));
            assertFalse(LineVisibility.isFullTree(RouteRegistry.byId("nusmp:scale_line_100")));
            RouteRegistry.addClient(List.of(all.get(299)));
            assertTrue(LineVisibility.isFullTree(RouteRegistry.byId("nusmp:scale_line_299")));
            assertTrue(LineVisibility.isFullTree(RouteRegistry.byId("nusmp:scale_line_4")), "adding a route keeps the others");
            RouteRegistry.setClient(List.of(all.get(5)));
            assertFalse(LineVisibility.isFullTree(RouteRegistry.byId("nusmp:scale_line_4")), "a new sync replaces the trees, and the line falls back to its stub");
            assertEquals(LINES, RouteRegistry.cards().size());
            RouteRegistry.clearClient();
            assertEquals(0, RouteRegistry.all().size());
        } finally {
            RouteRegistry.clearClient();
        }
    }

    /** A node list that explodes when it is looked at: proof that code which should not touch the nodes does not. */
    private static final class Poisoned extends AbstractList<SkillNode> {
        @Override public SkillNode get(int index) { throw new AssertionError("the nodes of a line the player does not own were looked at"); }

        @Override public int size() { throw new AssertionError("the nodes of a line the player does not own were counted"); }

        @Override public boolean isEmpty() { throw new AssertionError("the nodes of a line the player does not own were checked"); }
    }

    @Test
    void theLibraryListsOnlyOwnedLinesAndNeverLooksAtTheOthersNodes() {
        List<SkillRoute> list = new ArrayList<>();
        List<SkillRoute> real = new ArrayList<>();
        for (int i = 0; i < LINES; i++) {
            SkillRoute r = LineFixtures.route(i);
            real.add(r);
            boolean mine = i == 3 || i == 40 || i == 250;
            list.add(mine ? r : new SkillRoute(r.id(), r.magic(), r.name(), "", r.book(), r.bookName(), "", r.color(), r.icon(), r.classId(), 0, 0, new Poisoned(), r.franchise(), "", "", ""));
        }
        SimpleTreeState player = new SimpleTreeState().unlock("nusmp:scale_line_3").unlock("nusmp:scale_line_40").unlock("nusmp:scale_line_250").withClass("scale_line_40", 0);
        List<SkillRoute> visible = LineVisibility.visible(list, player, false);
        assertEquals(3, visible.size());
        assertSame(list, LineVisibility.visible(list, player, true), "inspect mode lists everything (and the list itself, so a cache can see nothing changed)");
        for (SkillNode n : real.get(40).nodes()) if (n.hasToggle()) player.setLevel("nusmp:scale_line_40", n.id(), 1);
        for (SkillNode n : real.get(3).nodes()) if (n.hasToggle()) player.setLevel("nusmp:scale_line_3", n.id(), 2);
        List<PassiveRows.Row> rows = PassiveRows.build(list, player, (a, b) -> true, "");
        assertEquals(2 + 12 + 12, rows.size(), "two headers and the 12 switchable nodes (8 passives and 4 stats) of two lines");
        assertTrue(rows.get(0).header() && rows.get(0).equipped() && rows.get(0).route().id().equals("nusmp:scale_line_40"), "the equipped line comes first");
        assertFalse(rows.get(13).equipped() || !rows.get(13).header(), "then the other");
    }

    @Test
    void thePassivesModelFindsTheRowsOnScreenInLogarithmicTimeOverThousandsOfRows() {
        List<SkillRoute> all = all();
        SimpleTreeState player = new SimpleTreeState().withClass("scale_line_7", 0);
        for (SkillRoute r : all) {
            player.unlock(r.id());
            for (SkillNode n : r.nodes()) if (n.hasToggle()) player.setLevel(r.id(), n.id(), 1);
        }
        long t0 = System.nanoTime();
        List<PassiveRows.Row> rows = PassiveRows.build(all, player, (a, b) -> !b.endsWith("pas_3"), "");
        PassiveRows.Layout layout = new PassiveRows.Layout(rows);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("[scale] the Passives model of " + rows.size() + " rows built in " + ms + " ms, content " + layout.total() + " px");
        assertEquals(LINES * 13, rows.size(), "300 headers and 300 x 12 entries");
        assertEquals("nusmp:scale_line_7", rows.get(0).route().id());
        assertTrue(rows.get(0).equipped() && !rows.get(13).equipped());
        assertTrue(ms < 10_000);
        // every pixel of the content maps to a row whose span contains it
        for (int y : new int[]{0, 1, 13, 14, 15, 33, 34, 35, layout.total() / 2, layout.total() - 1}) {
            int i = layout.rowAt(y);
            assertTrue(i >= 0 && layout.top(i) <= y && y < layout.top(i) + layout.height(i), "y=" + y + " -> row " + i);
        }
        assertEquals(-1, layout.rowAt(-1));
        assertEquals(-1, layout.rowAt(layout.total()));
        assertEquals(0, layout.clampScroll(-50, 200));
        assertEquals(layout.total() - 200, layout.clampScroll(Integer.MAX_VALUE / 2, 200));
        // the switch states come from the predicate
        int off = 0;
        for (PassiveRows.Row r : rows) if (!r.header() && !r.on()) off++;
        assertEquals(LINES, off, "one passive per line is switched off");
        // the filter narrows without touching headers of empty lines
        List<PassiveRows.Row> filtered = PassiveRows.build(all, player, (a, b) -> true, "Passive 3 of 299");
        assertEquals(2, filtered.size(), "one header and the one entry that matches");
        assertTrue(filtered.get(0).header() && !filtered.get(1).header() && filtered.get(1).name().equals("Passive 3 of 299"));
        assertEquals(0, PassiveRows.build(all, new SimpleTreeState(), (a, b) -> true, "").size(), "a player with no lines has no rows");
    }
}
