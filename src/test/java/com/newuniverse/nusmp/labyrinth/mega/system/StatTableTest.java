package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@code stats.json} decides what a point of strength or sense DOES, and a misspelt operation or attribute id is a stat that silently does nothing. This
 * reads the real shipped file, checks the five stats are all there with sane numbers (the best build is a strong player, not a god), checks the arithmetic of
 * a point and its ceiling, and feeds the parser broken rows to prove each check fires.
 */
class StatTableTest {
    static final Path STATS = Path.of("src/main/resources/data/nusmp/mega/system/stats.json");

    static Map<String, Object> tree() throws IOException { return MiniJson.object(Files.readString(STATS, StandardCharsets.UTF_8)); }

    static StatParser.Result shipped() throws IOException { return StatParser.parse(tree()); }

    static StatTable table() throws IOException { return new StatTable(shipped().defs()); }

    @Test void theShippedFileParsesWithNoComplaint() throws IOException {
        StatParser.Result r = shipped();
        assertTrue(r.problems().isEmpty(), "problems: " + r.problems());
        assertEquals(Stat.COUNT, r.defs().size());
    }

    @Test void everyStatHasAtLeastOneEffectAndTheFileSaysWhy() throws IOException {
        assertTrue(tree().get("why") instanceof String w && w.length() >= 120, "the file needs its WHY");
        StatTable t = table();
        for (Stat s : Stat.values()) {
            assertNotNull(t.def(s), s.name());
            assertFalse(t.def(s).effects().isEmpty(), s.name());
            assertFalse(t.def(s).name().isEmpty(), s.name());
        }
    }

    @Test void theBriefsAttributesAreWiredToTheRightStats() throws IOException {
        StatTable t = table();
        assertEquals("minecraft:generic.attack_damage", t.def(Stat.STR).effects().get(0).attribute());
        assertEquals("minecraft:generic.movement_speed", t.def(Stat.AGI).effects().get(0).attribute());
        assertEquals("minecraft:generic.max_health", t.def(Stat.VIT).effects().get(0).attribute());
        assertEquals("tensura:max_magicule", t.def(Stat.INT).effects().get(0).attribute());
        assertEquals("minecraft:generic.luck", t.def(Stat.SENSE).effects().get(0).attribute());
        assertEquals("tensura:presence_sense_radius", t.def(Stat.SENSE).effects().get(1).attribute());
    }

    @Test void theBestBuildIsAStrongPlayerNotAGod() throws IOException {
        StatTable t = table();
        for (Stat s : Stat.values()) {
            for (StatTable.Effect e : t.def(s).effects()) {
                assertTrue(e.cap() > 0, s + " " + e.attribute() + " needs a ceiling");
                double best = e.amount(30);
                assertTrue(best > 0 && best <= e.cap() + 1e-9, s + " " + e.attribute());
                if (e.op() != StatTable.Op.ADD_VALUE) assertTrue(best <= 0.6 + 1e-9, s + " multiplies by at most 60 percent");
            }
        }
        assertEquals(6.0, t.def(Stat.STR).effects().get(0).amount(30), 1e-9);
        assertEquals(0.25, t.def(Stat.AGI).effects().get(0).amount(30), 1e-9, "25 percent faster, reached at 25 points");
        assertEquals(20.0, t.def(Stat.VIT).effects().get(0).amount(30), 1e-9);
    }

    @Test void aPointAddsPerPointUntilTheCeiling() {
        StatTable.Effect e = new StatTable.Effect("minecraft:generic.luck", StatTable.Op.ADD_VALUE, 0.25, 5.0);
        assertEquals(0.0, e.amount(0), 1e-9);
        assertEquals(0.75, e.amount(3), 1e-9);
        assertEquals(5.0, e.amount(20), 1e-9);
        assertEquals(5.0, e.amount(500), 1e-9, "never past the ceiling");
        assertEquals(0.0, e.amount(-4), 1e-9, "negative points count as none");
        StatTable.Effect free = new StatTable.Effect("a:b", StatTable.Op.ADD_VALUE, 2.0, 0.0);
        assertEquals(2000.0, free.amount(1000), 1e-9, "cap 0 = no ceiling");
        StatTable.Effect curse = new StatTable.Effect("a:b", StatTable.Op.ADD_VALUE, -0.5, 3.0);
        assertEquals(-3.0, curse.amount(100), 1e-9, "a negative effect is cut by its magnitude and keeps its sign");
    }

    @Test void operationsAreReadByTheirSerialisedNames() {
        assertEquals(StatTable.Op.ADD_VALUE, StatTable.Op.fromId("add_value"));
        assertEquals(StatTable.Op.ADD_MULTIPLIED_BASE, StatTable.Op.fromId(" ADD_MULTIPLIED_BASE "));
        assertEquals(StatTable.Op.ADD_MULTIPLIED_TOTAL, StatTable.Op.fromId("add_multiplied_total"));
        assertNull(StatTable.Op.fromId("multiply"));
        assertNull(StatTable.Op.fromId(null));
    }

    @Test void brokenRowsAreReportedAndOnlyTheBrokenPartIsDropped() {
        String json = "{\"stats\":{"
                + "\"STR\":{\"name\":\"S\",\"effects\":[{\"attribute\":\"Bad Id\",\"operation\":\"add_value\",\"perPoint\":1},"
                + "{\"attribute\":\"minecraft:generic.luck\",\"operation\":\"sideways\",\"perPoint\":1},"
                + "{\"attribute\":\"minecraft:generic.luck\",\"operation\":\"add_value\",\"perPoint\":0},"
                + "{\"attribute\":\"minecraft:generic.luck\",\"operation\":\"add_value\",\"perPoint\":1,\"cap\":-2},"
                + "{\"attribute\":\"minecraft:generic.luck\",\"operation\":\"add_value\",\"perPoint\":1,\"cap\":9}]},"
                + "\"WIS\":{\"effects\":[]},"
                + "\"AGI\":{\"effects\":[]}}}";
        StatParser.Result r = StatParser.parse(MiniJson.object(json));
        assertEquals(2, r.defs().size(), "STR and AGI survive, WIS is unknown");
        StatTable.Def str = r.defs().get(0);
        assertEquals(1, str.effects().size(), "only the last effect is valid");
        assertEquals(9.0, str.effects().get(0).cap(), 1e-9);
        assertTrue(r.problems().size() >= 6, r.problems().toString());
    }

    @Test void aMissingRootOrStatsObjectIsReported() {
        assertFalse(StatParser.parse(null).problems().isEmpty());
        assertFalse(StatParser.parse(MiniJson.object("{}")).problems().isEmpty());
        assertFalse(StatParser.parse(MiniJson.object("{\"stats\":[]}")).problems().isEmpty());
    }

    @Test void aLaterTableRowReplacesAnEarlierOne() {
        StatTable.Def a = new StatTable.Def(Stat.STR, "A", List.of(new StatTable.Effect("a:b", StatTable.Op.ADD_VALUE, 1, 0)));
        StatTable.Def b = new StatTable.Def(Stat.STR, "B", List.of(new StatTable.Effect("a:c", StatTable.Op.ADD_VALUE, 2, 0)));
        StatTable t = new StatTable(List.of(a, b));
        assertEquals(1, t.size());
        assertEquals("B", t.def(Stat.STR).name());
        assertTrue(StatTable.EMPTY.isEmpty());
    }
}
