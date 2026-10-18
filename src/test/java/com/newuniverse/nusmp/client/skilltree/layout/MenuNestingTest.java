package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.SimpleTreeState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "the arts and passives of a line are NESTED under its entry" is a promise about ORDER and CONTENT, so it is tested as one: three sections in a fixed order, a header before its entries, an owned line
 * listing only what is bought, an unowned line (an operator's inspect) listing everything read-only, a checkbox exactly on the switchable entries with the real switch state, and dimming for a line that is not
 * the equipped one. Then the same rules are run over every shipped route so a route shape nobody thought of cannot break the panel.
 */
class MenuNestingTest {
    private static final BiPredicate<String, String> ALL_ON = (r, n) -> true;

    private static SimpleTreeState owner(SkillRoute r, boolean equipped, String... bought) {
        SimpleTreeState s = new SimpleTreeState().unlock(r.id()).withClass(equipped ? r.classId() : "", 0);
        for (String n : bought) s.setLevel(r.id(), n, 1);
        return s;
    }

    private static List<MenuNesting.Kind> kinds(List<MenuNesting.Row> rows) { return rows.stream().map(MenuNesting.Row::kind).toList(); }

    @Test
    void anOwnedLineShowsArtsThenPassivesThenStatsAndOnlyWhatIsBought() {
        SkillRoute r = MenuFixtures.line();
        List<MenuNesting.Row> rows = MenuNesting.build(r, owner(r, true, "stat_a", "pas_b", "art_a", "stance", "link"), ALL_ON, true);
        assertEquals(List.of(MenuNesting.Kind.HEADER_ARTS, MenuNesting.Kind.STANCE, MenuNesting.Kind.ART, MenuNesting.Kind.HEADER_PASSIVES, MenuNesting.Kind.PASSIVE,
                MenuNesting.Kind.HEADER_STATS, MenuNesting.Kind.STAT), kinds(rows), "route-file order inside each section, sections in a fixed order, no connector node, nothing unbought");
        assertEquals(2, rows.get(0).count(), "the header counts its entries");
        assertEquals("stance", rows.get(1).node().id());
        assertEquals("art_a", rows.get(2).node().id());
        assertEquals(1, rows.get(3).count(), "the passives header counts its one bought passive");
    }

    @Test
    void onlySwitchableEntriesGetACheckboxAndItShowsTheRealSwitch() {
        SkillRoute r = MenuFixtures.line();
        BiPredicate<String, String> off = (route, node) -> !node.equals("pas_b");
        List<MenuNesting.Row> rows = MenuNesting.build(r, owner(r, true, "stance", "art_a", "pas_a", "pas_b", "stat_a"), off, true);
        for (MenuNesting.Row row : rows) {
            if (row.header()) {
                assertFalse(row.checkbox());
                continue;
            }
            boolean switchable = row.kind() == MenuNesting.Kind.PASSIVE || row.kind() == MenuNesting.Kind.STAT;
            assertEquals(switchable, row.checkbox(), row.node().id());
            if (!switchable) assertFalse(row.checked(), "an art has no state");
        }
        assertTrue(find(rows, "pas_a").checked(), "ON is a ticked box");
        assertFalse(find(rows, "pas_b").checked(), "OFF is an empty box");
        assertTrue(find(rows, "stat_a").checked());
    }

    @Test
    void aLineThatIsNotEquippedIsDimmedButStillSwitchable() {
        SkillRoute r = MenuFixtures.line();
        List<MenuNesting.Row> rows = MenuNesting.build(r, owner(r, false, "art_a", "pas_a"), ALL_ON, true);
        for (MenuNesting.Row row : rows) if (!row.header()) assertTrue(row.dim(), "asleep: " + row.node().id());
        assertTrue(find(rows, "pas_a").checkbox(), "a switch is remembered even while the line sleeps");
        List<MenuNesting.Row> awake = MenuNesting.build(r, owner(r, true, "art_a", "pas_a"), ALL_ON, true);
        for (MenuNesting.Row row : awake) assertFalse(row.dim());
    }

    @Test
    void aLineThatIsNotOwnedShowsEveryNodeReadOnly() {
        SkillRoute r = MenuFixtures.line();
        List<MenuNesting.Row> rows = MenuNesting.build(r, new SimpleTreeState(), ALL_ON, false);
        int entries = 0;
        for (MenuNesting.Row row : rows) {
            if (row.header()) continue;
            entries++;
            assertTrue(row.dim());
            assertFalse(row.checkbox(), "nothing of a line you do not own can be switched");
            assertEquals(0, row.level());
        }
        assertEquals(6, entries, "all six real nodes, not the connector");
    }

    @Test
    void nothingBoughtMeansNoRowsAndAMissingTreeMeansNoRows() {
        SkillRoute r = MenuFixtures.line();
        assertTrue(MenuNesting.build(r, owner(r, true), ALL_ON, true).isEmpty());
        assertTrue(MenuNesting.build(null, new SimpleTreeState(), ALL_ON, true).isEmpty());
        SkillRoute stub = new SkillRoute("nusmp:s", "x", "S", "", "b", "B", "", 0, "", "s", 0, 0, List.of(), "x", "", "", "");
        assertTrue(MenuNesting.build(stub, new SimpleTreeState(), ALL_ON, false).isEmpty(), "a node-less card shows nothing until the server sends the tree");
    }

    @Test
    void theSummaryCountsBoughtAndTotal() {
        SkillRoute r = MenuFixtures.line();
        MenuNesting.Summary s = MenuNesting.summary(r, owner(r, true, "art_a", "pas_a"));
        assertEquals(2, s.bought());
        assertEquals(7, s.total());
        assertEquals(0, MenuNesting.summary(null, new SimpleTreeState()).total());
    }

    @Test
    void theGeometryFindsTheRowUnderTheMouse() {
        SkillRoute r = MenuFixtures.line();
        List<MenuNesting.Row> rows = MenuNesting.build(r, owner(r, true, "art_a", "pas_a"), ALL_ON, true);        // header, art, header, passive
        assertEquals(4, rows.size());
        assertEquals(2 * MenuNesting.HEADER_H + 2 * MenuNesting.ROW_H, MenuNesting.contentHeight(rows));
        assertEquals(0, MenuNesting.rowAt(rows, 0));
        assertEquals(1, MenuNesting.rowAt(rows, MenuNesting.HEADER_H));
        assertEquals(1, MenuNesting.rowAt(rows, MenuNesting.HEADER_H + MenuNesting.ROW_H - 1));
        assertEquals(2, MenuNesting.rowAt(rows, MenuNesting.HEADER_H + MenuNesting.ROW_H));
        assertEquals(-1, MenuNesting.rowAt(rows, MenuNesting.contentHeight(rows)));
        assertEquals(-1, MenuNesting.rowAt(rows, -1));
        assertEquals(MenuNesting.HEADER_H + MenuNesting.ROW_H + MenuNesting.HEADER_H, MenuNesting.top(rows, 3));
        assertEquals(0, MenuNesting.clampScroll(rows, 50, 1000), "content shorter than the view never scrolls");
        assertEquals(MenuNesting.contentHeight(rows) - 40, MenuNesting.clampScroll(rows, 9999, 40));
        assertEquals(0, MenuNesting.clampScroll(rows, -5, 40));
    }

    @Test
    void everyShippedRouteNestsCorrectlyWhetherOwnedOrNot() {
        List<SkillRoute> all = MenuFixtures.realRoutes();
        assertTrue(all.size() >= 283, "the shipped routes: " + all.size());
        for (SkillRoute r : all) {
            Set<String> real = new HashSet<>();
            for (SkillNode n : r.nodes()) if (MenuNesting.entryKind(n) != null) real.add(n.id());
            assertFalse(real.isEmpty(), r.id() + " has something to show");

            SimpleTreeState everything = new SimpleTreeState().unlock(r.id()).withClass(r.classId(), 0);
            for (SkillNode n : r.nodes()) everything.setLevel(r.id(), n.id(), 1);
            check(r, MenuNesting.build(r, everything, ALL_ON, true), real, true);
            check(r, MenuNesting.build(r, new SimpleTreeState(), ALL_ON, false), real, false);
            assertTrue(MenuNesting.build(r, new SimpleTreeState().unlock(r.id()).withClass(r.classId(), 0), ALL_ON, true).isEmpty(), r.id() + ": an owned line with nothing bought lists nothing");
        }
    }

    private static void check(SkillRoute r, List<MenuNesting.Row> rows, Set<String> real, boolean owned) {
        Set<String> seen = new HashSet<>();
        MenuNesting.Kind section = null;
        int inSection = 0, claimed = 0;
        List<MenuNesting.Kind> order = new ArrayList<>();
        for (MenuNesting.Row row : rows) {
            if (row.header()) {
                if (section != null) assertEquals(claimed, inSection, r.id() + ": a header counts its entries");
                section = row.kind();
                claimed = row.count();
                inSection = 0;
                order.add(section);
                continue;
            }
            assertNotNull(section, r.id() + ": an entry before any header");
            inSection++;
            assertTrue(seen.add(row.node().id()), r.id() + ": " + row.node().id() + " listed twice");
            assertTrue(real.contains(row.node().id()), r.id() + ": " + row.node().id() + " is a connector node");
            boolean switchable = row.kind() == MenuNesting.Kind.PASSIVE || row.kind() == MenuNesting.Kind.STAT;
            assertEquals(owned && row.node().hasToggle() && switchable, row.checkbox(), r.id() + ": " + row.node().id() + " checkbox");
            assertEquals(!owned, row.dim(), r.id() + ": " + row.node().id() + " dim (every line is equipped here, so only ownership dims)");
        }
        if (section != null) assertEquals(claimed, inSection, r.id());
        assertEquals(real, seen, r.id() + ": every showable node is listed exactly once");
        for (int i = 1; i < order.size(); i++) assertTrue(order.get(i - 1).ordinal() < order.get(i).ordinal(), r.id() + ": sections out of order " + order);
    }

    private static MenuNesting.Row find(List<MenuNesting.Row> rows, String id) {
        for (MenuNesting.Row r : rows) if (!r.header() && r.node().id().equals(id)) return r;
        throw new AssertionError("no row for " + id);
    }
}
