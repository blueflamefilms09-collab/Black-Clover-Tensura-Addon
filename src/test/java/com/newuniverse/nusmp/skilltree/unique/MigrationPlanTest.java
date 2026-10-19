package com.newuniverse.nusmp.skilltree.unique;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a player who owned lines in a world saved before 1.96.0 must get the matching unique skills once, must not get a skill of a magic that was removed, and must never crash the login with an id the old record
 * holds by mistake. The mapping is a pure function, so every shape of the old record is tried here: owned lines that still exist (taught, sorted, no duplicates), owned lines of removed magics (dropped), ids with
 * no book (dropped), namespaced and bare ids, null and empty entries, an empty or missing record, and the real shipped table against every removed id.
 */
class MigrationPlanTest {
    private static final List<String> KNOWN = List.of("lady_luck", "oath_of_the_heavy_armor", "great_sage_analysis", "basic_swordsmanship");

    @Test
    void anOwnedLineThatStillExistsBecomesItsSkill() {
        MigrationPlan.Plan p = MigrationPlan.plan(List.of("nusmp:lady_luck", "nusmp:great_sage_analysis"), KNOWN);
        assertEquals(List.of("great_sage_analysis", "lady_luck"), p.grant(), "sorted");
        assertTrue(p.dropped().isEmpty());
    }

    @Test
    void anOwnedLineOfARemovedMagicIsDroppedNotTaught() {
        MigrationPlan.Plan p = MigrationPlan.plan(List.of("nusmp:fire_dragon_slayer", "nusmp:lady_luck", "nusmp:celestial_spirit_magic"), KNOWN);
        assertEquals(List.of("lady_luck"), p.grant());
        assertEquals(List.of("celestial_spirit_magic", "fire_dragon_slayer"), p.dropped());
    }

    @Test
    void aRemovedMagicIsDroppedEvenIfTheTableStillListedIt() {
        List<String> tableWithRemoved = new ArrayList<>(KNOWN);
        tableWithRemoved.add("ice_make");
        MigrationPlan.Plan p = MigrationPlan.plan(List.of("nusmp:ice_make"), tableWithRemoved);
        assertTrue(p.grant().isEmpty(), "the removed list wins over the table");
        assertEquals(List.of("ice_make"), p.dropped());
    }

    @Test
    void anUnknownIdIsDroppedAndNeverThrows() {
        MigrationPlan.Plan p = MigrationPlan.plan(List.of("othermod:something", "nusmp:no_such_line", "", "lady_luck"), KNOWN);
        assertEquals(List.of("lady_luck"), p.grant(), "a bare id works too");
        assertEquals(List.of("no_such_line", "othermod:something"), p.dropped());
    }

    @Test
    void aRouteOfAnotherNamespaceNeverBecomesOneOfOurSkills() {
        MigrationPlan.Plan p = MigrationPlan.plan(List.of("othermod:lady_luck"), KNOWN);
        assertTrue(p.grant().isEmpty());
        assertEquals(List.of("othermod:lady_luck"), p.dropped());
    }

    @Test
    void duplicatesCollapse() {
        MigrationPlan.Plan p = MigrationPlan.plan(List.of("nusmp:lady_luck", "lady_luck", "nusmp:lady_luck"), KNOWN);
        assertEquals(List.of("lady_luck"), p.grant());
    }

    @Test
    void anEmptyOrMissingRecordGivesAnEmptyPlan() {
        assertTrue(MigrationPlan.plan(List.of(), KNOWN).grant().isEmpty());
        assertTrue(MigrationPlan.plan(null, KNOWN).grant().isEmpty());
        assertTrue(MigrationPlan.plan(List.of("nusmp:lady_luck"), List.of()).grant().isEmpty(), "with no books nothing is taught");
    }

    @Test
    void everyRemovedIdIsDroppedAgainstTheRealShippedTable() {
        List<String> known = new ArrayList<>();
        for (LineIndex.Line l : LineIndex.shipped()) known.add(l.bare());
        assertTrue(known.size() > 200, "the shipped table has the books: " + known.size());
        List<String> owned = new ArrayList<>();
        for (String r : LegacyIds.REMOVED_LINES) owned.add("nusmp:" + r);
        MigrationPlan.Plan p = MigrationPlan.plan(owned, known);
        assertTrue(p.grant().isEmpty(), "no removed magic becomes a skill: " + p.grant());
        assertEquals(LegacyIds.REMOVED_LINES.size(), p.dropped().size());
    }

    @Test
    void everyShippedBookIsTaughtWhenOwned() {
        List<String> known = new ArrayList<>(), owned = new ArrayList<>();
        for (LineIndex.Line l : LineIndex.shipped()) {
            known.add(l.bare());
            owned.add(l.routeId());
        }
        MigrationPlan.Plan p = MigrationPlan.plan(owned, known);
        assertEquals(known.size(), p.grant().size());
        assertTrue(p.dropped().isEmpty());
    }

    @Test
    void theRoutePartOfAStoredKeyIsEverythingBeforeTheHash() {
        assertEquals("nusmp:lady_luck", MigrationPlan.routePart("nusmp:lady_luck#cats_paw"));
        assertEquals("nusmp:lady_luck", MigrationPlan.routePart("nusmp:lady_luck"));
        assertEquals("", MigrationPlan.routePart("#x"));
    }
}
