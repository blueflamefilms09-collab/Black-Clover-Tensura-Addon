package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the roster file and the threat scaling decide which bodies walk the halls and how tough they are; these tests pin the parser's reports, the weighted
 * draw within a threat window and the capped growth of health and damage per threat level.
 */
class WandererRosterTest {
    private static final String GOOD = """
            {
              "comment": "why",
              "entries": [
                {"entity": "nusmp:floor_warden", "weight": 5, "minThreat": 1, "name": "The Warden", "healthScale": 1.5, "speed": 0.9, "comment": "ours"},
                {"entity": "minecraft:ravager", "weight": 3, "minThreat": 3, "maxThreat": 12},
                {"entity": "tensura:orc_lord", "weight": 2, "minThreat": 8}
              ]
            }
            """;

    private static boolean mentions(List<String> problems, String text) {
        for (String p : problems) if (text != null && p.contains(text)) return true;
        return false;
    }

    @Test
    void aGoodRosterIsReadCompletelyWithItsDefaults() {
        WandererRoster.Parsed p = WandererRoster.parse(GOOD);
        assertTrue(p.clean(), p.problems().toString());
        List<WandererRoster.Entry> e = p.roster().entries();
        assertEquals(3, e.size());
        assertEquals("nusmp:floor_warden", e.get(0).entity());
        assertEquals("The Warden", e.get(0).name());
        assertEquals(1.5, e.get(0).healthScale(), 1e-9);
        assertEquals(0.9, e.get(0).speed(), 1e-9);
        assertEquals("ravager", e.get(1).name(), "the name defaults to the entity's path");
        assertEquals(1.0, e.get(1).healthScale(), 0.0);
        assertEquals(1.0, e.get(1).speed(), 0.0);
        assertTrue(e.get(1).fitsThreat(3) && e.get(1).fitsThreat(12));
        assertFalse(e.get(1).fitsThreat(2) || e.get(1).fitsThreat(13));
        assertTrue(e.get(2).fitsThreat(100000));
    }

    @Test
    void problemsAreReportedAndBadEntriesDropped() {
        WandererRoster.Parsed p = WandererRoster.parse("""
                {"colour": 1, "entries": [
                  {"entity": "Warden"},
                  {"entity": "minecraft:ravager", "weight": 0, "healthScale": 99, "speed": "fast", "wieght": 2},
                  {"entity": "minecraft:ravager"},
                  "text",
                  {"entity": "minecraft:husk", "minThreat": 9, "maxThreat": 2}
                ]}
                """);
        assertEquals(2, p.roster().entries().size());
        assertEquals("minecraft:ravager", p.roster().entries().get(0).entity());
        assertEquals(1, p.roster().entries().get(0).weight(), "a zero weight is clamped to one");
        assertEquals(6.0, p.roster().entries().get(0).healthScale(), 0.0);
        assertEquals(1.0, p.roster().entries().get(0).speed(), 0.0);
        assertEquals(100000, p.roster().entries().get(1).maxThreat());
        assertTrue(mentions(p.problems(), "unknown key 'colour'"));
        assertTrue(mentions(p.problems(), "unknown key 'wieght'"));
        assertTrue(mentions(p.problems(), "listed twice"));
        assertTrue(mentions(p.problems(), "'entity' must be"));
        assertTrue(mentions(p.problems(), "is not an object"));
        assertTrue(mentions(p.problems(), "'speed' must be a number"));
        assertFalse(p.clean());
    }

    @Test
    void notJsonGivesNoRoster() {
        for (String bad : new String[] {"", "[]", "{", "7"}) assertNull(WandererRoster.parse(bad).roster(), bad);
        assertNull(WandererRoster.parse(null).roster());
        WandererRoster.Parsed noEntries = WandererRoster.parse("{}");
        assertTrue(noEntries.roster().entries().isEmpty());
        assertTrue(mentions(noEntries.problems(), "'entries' must be an array"));
    }

    @Test
    void pickingRespectsTheThreatWindowTheWeightsAndTheUsablePredicate() {
        WandererRoster roster = WandererRoster.parse(GOOD).roster();
        Random r = new Random(6);
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < 12000; i++) seen.merge(roster.pick(10, e -> true, r).entity(), 1, Integer::sum);
        assertEquals(3, seen.size());
        int warden = seen.get("nusmp:floor_warden"), ravager = seen.get("minecraft:ravager"), orc = seen.get("tensura:orc_lord");
        assertTrue(warden > ravager && ravager > orc, warden + " " + ravager + " " + orc);
        assertTrue(Math.abs(warden - 6000) < 400, "5 of 10 is half: " + warden);
        for (int i = 0; i < 200; i++) assertEquals("nusmp:floor_warden", roster.pick(1, e -> true, r).entity());
        for (int i = 0; i < 200; i++) assertFalse(roster.pick(20, e -> true, r).entity().equals("minecraft:ravager"));
        for (int i = 0; i < 200; i++) assertEquals("tensura:orc_lord", roster.pick(10, e -> e.entity().startsWith("tensura:"), r).entity());
        assertNull(roster.pick(10, e -> false, r));
        assertNull(WandererRoster.EMPTY.pick(10, null, r));
    }

    @Test
    void theThreatScalingAddsAFewPercentPerLevelAboveOneAndIsCapped() {
        assertEquals(0.0, ThreatScale.amount(1, 0.06), 0.0);
        assertEquals(0.0, ThreatScale.amount(0, 0.06), 0.0);
        assertEquals(0.06, ThreatScale.amount(2, 0.06), 1e-9);
        assertEquals(0.30, ThreatScale.amount(6, 0.06), 1e-9);
        assertEquals(0.04 * 9, ThreatScale.amount(10, 0.04), 1e-9);
        assertEquals(ThreatScale.CAP, ThreatScale.amount(100000, 0.06), 0.0);
        assertEquals(0.0, ThreatScale.amount(50, 0.0), 0.0);
        assertEquals(0.0, ThreatScale.amount(50, -1.0), 0.0);
        assertEquals(0.0, ThreatScale.amount(50, Double.NaN), 0.0);
    }
}
