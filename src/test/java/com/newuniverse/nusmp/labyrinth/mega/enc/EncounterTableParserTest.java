package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the tables are hand-edited data in a game nobody can run here, so the worst failure is silence. These tests pin that a good file is read completely,
 * that each kind of mistake is reported in words naming the entry, that a bad entry costs only itself, and that nothing a file contains can throw.
 */
class EncounterTableParserTest {
    private static final String GOOD = """
            {
              "id": "demo",
              "description": "a table",
              "comment": "why it is like this",
              "rollBonus": 1,
              "budgetScale": 1.5,
              "defaultFaction": "undead",
              "entries": [
                {
                  "id": "goblin_patrol",
                  "weight": 12,
                  "band": "standard",
                  "minThreat": 2,
                  "maxThreat": 6,
                  "faction": "goblin",
                  "mobs": [
                    {"type": "faction", "count": "2-4"},
                    {"type": "entity", "id": "tensura:goblin", "count": 1},
                    {"type": "faction", "faction": "ogre", "count": "0-1"}
                  ],
                  "needs": {"dark": false, "noisy": true},
                  "scaleWithParty": false,
                  "minParty": 2,
                  "message": "message.nusmp.mega.enc.goblin_patrol",
                  "range": [20, 40],
                  "cells": 3,
                  "hunt": 900,
                  "cue": {"sound": "minecraft:entity.zombie.ambient", "volume": 0.5, "pitch": 0.6, "particle": "minecraft:smoke", "count": 8},
                  "comment": "scouts first"
                },
                {"id": "min", "band": "minor", "mobs": [{"type": "entity", "id": "minecraft:silverfish"}]}
              ]
            }
            """;

    private static EncounterTableParser.Parsed parse(String json) { return EncounterTableParser.parse("test", json); }

    private static boolean mentions(List<String> problems, String text) {
        for (String p : problems) if (p.contains(text)) return true;
        return false;
    }

    @Test
    void aGoodFileIsReadCompletely() {
        EncounterTableParser.Parsed p = parse(GOOD);
        assertTrue(p.clean(), p.problems().toString());
        EncounterTable t = p.table();
        assertEquals("test", t.id(), "the id is the file's, whatever the file says");
        assertEquals(1, t.rollBonus());
        assertEquals(1.5, t.budgetScale(), 1e-9);
        assertEquals("undead", t.defaultFaction());
        assertEquals(2, t.entries().size());
        EncounterTable.Entry e = t.byId("goblin_patrol");
        assertNotNull(e);
        assertEquals(12, e.weight());
        assertEquals(Band.STANDARD, e.band());
        assertTrue(e.fitsThreat(2) && e.fitsThreat(6));
        assertFalse(e.fitsThreat(1) || e.fitsThreat(7));
        assertEquals("goblin", e.faction());
        assertEquals(3, e.mobs().size());
        assertEquals(EncounterTable.Kind.FACTION, e.mobs().get(0).kind());
        assertEquals("2-4", e.mobs().get(0).count().text());
        assertEquals(EncounterTable.Kind.ENTITY, e.mobs().get(1).kind());
        assertEquals("tensura:goblin", e.mobs().get(1).entity());
        assertEquals("ogre", e.mobs().get(2).faction());
        assertEquals(Boolean.FALSE, e.needs().dark());
        assertNull(e.needs().lit());
        assertEquals(Boolean.TRUE, e.needs().noisy());
        assertTrue(e.needs().any());
        assertFalse(e.scaleWithParty());
        assertEquals(2, e.minParty());
        assertEquals("message.nusmp.mega.enc.goblin_patrol", e.message());
        assertEquals(20.0, e.minRange(), 0.0);
        assertEquals(40.0, e.maxRange(), 0.0);
        assertEquals(3, e.cells());
        assertEquals(900, e.hunt());
        assertEquals("minecraft:entity.zombie.ambient", e.cue().sound());
        assertEquals(0.5f, e.cue().volume(), 1e-6f);
        assertEquals(0.6f, e.cue().pitch(), 1e-6f);
        assertEquals("minecraft:smoke", e.cue().particle());
        assertEquals(8, e.cue().particles());
        assertTrue(e.usesFaction());
        EncounterTable.Entry m = t.byId("min");
        assertEquals(10, m.weight(), "the default weight");
        assertEquals(1, m.minThreat());
        assertTrue(m.fitsThreat(99999));
        assertTrue(m.scaleWithParty());
        assertEquals(2, m.cells());
        assertEquals(-1, m.hunt());
        assertEquals(0.0, m.minRange(), 0.0);
        assertSame(EncounterTable.Cue.NONE, m.cue());
        assertFalse(m.needs().any());
        assertFalse(m.usesFaction());
        assertNull(t.byId("nope"));
        assertNull(t.byId(null));
    }

    @Test
    void notJsonAtAllGivesNoTableAndAProblem() {
        for (String bad : new String[] {"", "{", "[]", "42", "\"x\"", "{\"entries\": "}) {
            EncounterTableParser.Parsed p = parse(bad);
            assertNull(p.table(), bad);
            assertFalse(p.problems().isEmpty(), bad);
        }
        assertNull(EncounterTableParser.parse("x", null).table());
    }

    @Test
    void aMissingEntriesArrayIsAProblemButTheTableStillExists() {
        EncounterTableParser.Parsed p = parse("{}");
        assertNotNull(p.table());
        assertTrue(p.table().entries().isEmpty());
        assertTrue(mentions(p.problems(), "'entries' must be an array"));
        assertFalse(p.clean());
    }

    @Test
    void anEntryWithABadBandOrNoMonstersIsDroppedAndTheRestSurvives() {
        EncounterTableParser.Parsed p = parse("""
                {"entries": [
                  {"id": "bad_band", "band": "hrad", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]},
                  {"id": "no_band", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]},
                  {"id": "nothing_band", "band": "nothing", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]},
                  {"id": "empty_mobs", "band": "minor", "mobs": []},
                  {"id": "no_mobs", "band": "minor"},
                  {"id": "good", "band": "minor", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]}
                ]}
                """);
        assertEquals(1, p.table().entries().size());
        assertEquals("good", p.table().entries().get(0).id());
        assertTrue(mentions(p.problems(), "bad_band") && mentions(p.problems(), "no_band") && mentions(p.problems(), "nothing_band"));
        assertTrue(mentions(p.problems(), "empty_mobs") && mentions(p.problems(), "no_mobs"));
        assertEquals(5, p.problems().size(), p.problems().toString());
    }

    @Test
    void badIdsAndDuplicateIdsAreCaught() {
        EncounterTableParser.Parsed p = parse("""
                {"entries": [
                  {"band": "minor", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]},
                  {"id": "Has Space", "band": "minor", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]},
                  {"id": "twin", "band": "minor", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]},
                  {"id": "twin", "band": "hard", "mobs": [{"type": "entity", "id": "minecraft:husk"}]}
                ]}
                """);
        assertEquals(1, p.table().entries().size());
        assertEquals(Band.MINOR, p.table().entries().get(0).band(), "the first twin wins");
        assertTrue(mentions(p.problems(), "used twice"));
        assertTrue(mentions(p.problems(), "'id' must be"));
    }

    @Test
    void aBadMobKillsItsEntryWithAnExactMessage() {
        String[][] cases = {
                {"{\"type\": \"entity\"}", "entity id"},
                {"{\"type\": \"entity\", \"id\": \"Zombie\"}", "entity id"},
                {"{\"type\": \"entity\", \"id\": \"minecraft:zombie\", \"count\": \"2--4\"}", "'count'"},
                {"{\"type\": \"entity\", \"id\": \"minecraft:zombie\", \"count\": true}", "'count'"},
                {"{\"type\": \"faction\", \"faction\": \"dragons\"}", "not a faction"},
                {"{\"type\": \"mob\"}", "'type' must be"},
                {"{}", "'type' must be"},
        };
        for (String[] c : cases) {
            EncounterTableParser.Parsed p = parse("{\"entries\": [{\"id\": \"e\", \"band\": \"minor\", \"mobs\": [" + c[0] + "]}]}");
            assertTrue(p.table().entries().isEmpty(), c[0]);
            assertTrue(mentions(p.problems(), c[1]), c[0] + " -> " + p.problems());
        }
    }

    @Test
    void detailsAreClampedAndReportedButTheEntryIsKept() {
        EncounterTableParser.Parsed p = parse("""
                {"rollBonus": 9, "budgetScale": 100, "entries": [
                  {"id": "e", "band": "minor", "weight": 5000, "minThreat": 9, "maxThreat": 3, "cells": 9, "hunt": 99999,
                   "range": [1, 500], "mobs": [{"type": "entity", "id": "minecraft:zombie"}]}
                ]}
                """);
        assertEquals(1, p.table().entries().size());
        EncounterTable.Entry e = p.table().entries().get(0);
        assertEquals(1000, e.weight());
        assertEquals(100000, e.maxThreat(), "an upside-down window is ignored");
        assertEquals(4, e.cells());
        assertEquals(12000, e.hunt());
        assertEquals(0.0, e.minRange(), 0.0, "a bad range falls back to the default");
        assertEquals(3, p.table().rollBonus());
        assertEquals(4.0, p.table().budgetScale(), 0.0);
        assertTrue(p.problems().size() >= 7, p.problems().toString());
    }

    @Test
    void wrongTypesAreProblemsNotExceptions() {
        EncounterTableParser.Parsed p = parse("""
                {"rollBonus": "high", "description": 5, "entries": [
                  {"id": "e", "band": "minor", "weight": "heavy", "scaleWithParty": "yes", "minParty": 1.5, "message": 7, "needs": [], "cue": 3, "range": "far",
                   "mobs": [{"type": "entity", "id": "minecraft:zombie"}]},
                  "not an object"
                ]}
                """);
        assertEquals(1, p.table().entries().size());
        EncounterTable.Entry e = p.table().entries().get(0);
        assertEquals(10, e.weight());
        assertTrue(e.scaleWithParty());
        assertEquals("", e.message());
        assertFalse(e.needs().any());
        assertTrue(p.problems().size() >= 9, p.problems().toString());
    }

    @Test
    void unknownKeysAreReportedToCatchTypos() {
        EncounterTableParser.Parsed p = parse("""
                {"colour": 1, "entries": [
                  {"id": "e", "band": "minor", "wieght": 5, "needs": {"darkk": true},
                   "cue": {"soundd": "x"}, "mobs": [{"type": "entity", "id": "minecraft:zombie", "cnt": 2}]}
                ]}
                """);
        assertEquals(1, p.table().entries().size());
        assertTrue(mentions(p.problems(), "unknown key 'colour'"));
        assertTrue(mentions(p.problems(), "unknown key 'wieght'"));
        assertTrue(mentions(p.problems(), "unknown key 'darkk'"));
        assertTrue(mentions(p.problems(), "unknown key 'soundd'"));
        assertTrue(mentions(p.problems(), "unknown key 'cnt'"));
    }

    @Test
    void anUnknownFactionIsRefusedEverywhere() {
        EncounterTableParser.Parsed p = parse("""
                {"defaultFaction": "pirates", "entries": [
                  {"id": "e", "band": "minor", "faction": "pirates", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]}
                ]}
                """);
        assertNull(p.table().defaultFaction());
        assertEquals("", p.table().entries().get(0).faction());
        assertEquals(2, p.problems().size(), p.problems().toString());
        for (String f : EncounterTable.FACTIONS) {
            EncounterTableParser.Parsed ok = parse("{\"entries\": [{\"id\": \"e\", \"band\": \"minor\", \"faction\": \"" + f + "\", \"mobs\": [{\"type\": \"faction\"}]}]}");
            assertTrue(ok.clean(), f);
        }
        assertEquals(11, EncounterTable.FACTIONS.size());
    }

    @Test
    void aFactionMonsterNeedsAFactionSomewhere() {
        String mob = "{\"type\": \"faction\"}";
        // no source at all: the entry could never be drawn, so it is dropped with an exact message
        EncounterTableParser.Parsed none = parse("{\"entries\": [{\"id\": \"e\", \"band\": \"minor\", \"mobs\": [" + mob + "]}]}");
        assertTrue(none.table().entries().isEmpty());
        assertTrue(mentions(none.problems(), "needs a faction"), none.problems().toString());
        assertTrue(mentions(none.problems(), "entry 'e' mob #0"), none.problems().toString());
        // the three sources, each alone, make it clean
        assertTrue(parse("{\"defaultFaction\": \"undead\", \"entries\": [{\"id\": \"e\", \"band\": \"minor\", \"mobs\": [" + mob + "]}]}").clean());
        assertTrue(parse("{\"entries\": [{\"id\": \"e\", \"band\": \"minor\", \"faction\": \"beast\", \"mobs\": [" + mob + "]}]}").clean());
        assertTrue(parse("{\"entries\": [{\"id\": \"e\", \"band\": \"minor\", \"mobs\": [{\"type\": \"faction\", \"faction\": \"insect\"}]}]}").clean());
        // an entity monster never needs one, and only the bad entry is lost, not the file
        EncounterTableParser.Parsed mixed = parse("""
                {"entries": [
                  {"id": "bad", "band": "minor", "mobs": [{"type": "entity", "id": "minecraft:zombie"}, {"type": "faction"}]},
                  {"id": "good", "band": "minor", "mobs": [{"type": "entity", "id": "minecraft:zombie"}]}
                ]}
                """);
        assertEquals(1, mixed.table().entries().size());
        assertEquals("good", mixed.table().entries().get(0).id());
        assertEquals(1, mixed.problems().size(), mixed.problems().toString());
    }

    @Test
    void theNeedsRecordMatchesTheThreeStates() {
        EncounterTable.Needs darkOnly = new EncounterTable.Needs(true, null, null);
        assertTrue(darkOnly.met(true, false, false));
        assertTrue(darkOnly.met(true, true, true));
        assertFalse(darkOnly.met(false, false, false));
        EncounterTable.Needs notDark = new EncounterTable.Needs(false, null, null);
        assertTrue(notDark.met(false, true, true));
        assertFalse(notDark.met(true, false, false));
        assertTrue(EncounterTable.Needs.NONE.met(true, true, true));
        assertTrue(EncounterTable.Needs.NONE.met(false, false, false));
        EncounterTable.Needs both = new EncounterTable.Needs(true, false, true);
        assertTrue(both.met(true, false, true));
        assertFalse(both.met(true, true, true));
    }

    @Test
    void tooManyEntriesAreCutAndSaid() {
        StringBuilder sb = new StringBuilder("{\"entries\": [");
        for (int i = 0; i < EncounterTableParser.MAX_ENTRIES + 5; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"id\": \"e").append(i).append("\", \"band\": \"minor\", \"mobs\": [{\"type\": \"entity\", \"id\": \"minecraft:zombie\"}]}");
        }
        sb.append("]}");
        EncounterTableParser.Parsed p = parse(sb.toString());
        assertEquals(EncounterTableParser.MAX_ENTRIES, p.table().entries().size());
        assertTrue(mentions(p.problems(), "more than"));
    }
}
