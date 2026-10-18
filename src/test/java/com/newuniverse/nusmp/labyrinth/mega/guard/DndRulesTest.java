package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the D&D layer of the Decennial bosses is only fair if its rules are facts, so this pins them without a game: the saving throw (natural 20 and 1), the profile each floor gets (ten a
 * side, the right tier, the last one past floor 1000), the order of legendary actions (a cycle, never the unaffordable), who the Word of Ending may name and when it fails, how much
 * magic is eaten, and the phase from health; and it reads the twenty SHIPPED profile files with the real parser so a typo in a file fails the build and not a player's boss fight.
 */
class DndRulesTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/mega/dnd_bosses");

    @Test
    void aSaveHasNaturalTwentyAndNaturalOne() {
        assertTrue(DndRules.save(15, 20, -50));
        assertFalse(DndRules.save(15, 1, 50));
        assertTrue(DndRules.save(15, 10, 5));
        assertFalse(DndRules.save(15, 10, 4));
        assertEquals(8, DndRules.saveBonus(200, 50));
        assertEquals(0, DndRules.saveBonus(-3, -2));
        assertEquals(5, DndRules.saveBonus(20, 0));
    }

    @Test
    void everyFloorGetsOneOfTenProfilesPerSide() {
        Set<String> holy = new HashSet<>(), evil = new HashSet<>();
        for (int f = 1; f <= 1100; f++) {
            holy.add(DndRules.idForFloor(f));
            evil.add(DndRules.idForFloor(-f));
        }
        assertEquals(10, holy.size());
        assertEquals(10, evil.size());
        assertEquals("holy_t1a", DndRules.idForFloor(10));
        assertEquals("holy_t1b", DndRules.idForFloor(110));
        assertEquals("holy_t2a", DndRules.idForFloor(201));
        assertEquals("evil_t5b", DndRules.idForFloor(-1000));
        assertEquals("evil_t5b", DndRules.idForFloor(-5000));
        assertEquals("holy_t1a", DndRules.idForFloor(0));
    }

    @Test
    void legendaryActionsCycleAndNeverTakeTheUnaffordable() {
        int[] costs = {1, 1, 2};
        assertEquals(1, DndRules.nextAction(costs, 3, 0));
        assertEquals(2, DndRules.nextAction(costs, 3, 1));
        assertEquals(0, DndRules.nextAction(costs, 3, 2));
        assertEquals(1, DndRules.nextAction(costs, 1, 0));       // the 2-point move is skipped
        assertEquals(0, DndRules.nextAction(costs, 1, 1));
        assertEquals(-1, DndRules.nextAction(costs, 0, 0));
        assertEquals(-1, DndRules.nextAction(new int[0], 5, -1));
    }

    @Test
    void theWordOfEndingNamesOnlyThoseUnderTheThresholdAndCanFail() {
        assertTrue(DndRules.executionTarget(7, 20));
        assertFalse(DndRules.executionTarget(8, 20));
        assertFalse(DndRules.executionTarget(0, 20));
        assertTrue(DndRules.executionLands(5, 20, true, 0, 1000));
        assertFalse(DndRules.executionLands(15, 20, true, 0, 1000), "healed above the threshold");
        assertFalse(DndRules.executionLands(5, 20, false, 0, 1000), "broke line of sight");
        assertFalse(DndRules.executionLands(5, 20, true, 31, 1000), "the boss lost concentration");
    }

    @Test
    void aQuarterOfMagicIsEatenAndNothingElse() {
        float[] magic = DndRules.absorb(40f, true);
        assertEquals(30f, magic[0], 1e-4);
        assertEquals(10f, magic[1], 1e-4);
        float[] steel = DndRules.absorb(40f, false);
        assertEquals(40f, steel[0], 1e-4);
        assertEquals(0f, steel[1], 1e-4);
    }

    @Test
    void thePhasesFollowHealthAndTheMythicTrait() {
        assertEquals(1, DndRules.phase(900, 1000, false));
        assertEquals(2, DndRules.phase(400, 1000, false));
        assertEquals(3, DndRules.phase(900, 1000, true));
        assertEquals(5, DndRules.clampResistances(99));
        assertEquals(5, DndRules.points(5, true));
        assertEquals(4, DndRules.points(3, true));
    }

    @Test
    void theTwentyShippedProfilesParseAndEscalate() throws Exception {
        Set<String> seen = new HashSet<>();
        int lastResist = 0;
        for (String side : new String[] {"holy", "evil"}) {
            for (int tier = 1; tier <= 5; tier++) {
                for (String v : new String[] {"a", "b"}) {
                    String id = side + "_t" + tier + v;
                    Path p = DIR.resolve(id + ".json");
                    assertTrue(Files.exists(p), id + " is missing");
                    JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                    DndProfile prof = DndProfile.parse(id, o);
                    assertEquals(side, prof.side());
                    assertEquals(tier, prof.tier());
                    assertEquals(v.equals("a") ? 0 : 1, prof.variant());
                    assertEquals(3, prof.forms().size());
                    assertFalse(prof.legendary().isEmpty(), id + " has no legendary action");
                    assertFalse(prof.lair().isEmpty(), id + " has no lair action");
                    assertTrue(prof.resistances() >= 2 && prof.resistances() <= 5);
                    if (tier >= 5) assertTrue(prof.execution() && prof.antiMagicAura(), id + ": the deepest bosses carry the execution and the anti-magic aura");
                    if (tier <= 2) assertFalse(prof.execution(), id + ": the first tiers do not execute");
                    assertTrue(seen.add(prof.name()), "name used twice: " + prof.name());
                    assertEquals(id, DndRules.idForFloor((side.equals("evil") ? -1 : 1) * (tier * 200 - (v.equals("a") ? 150 : 50))));
                    if (v.equals("a") && side.equals("holy")) { assertTrue(prof.resistances() >= lastResist); lastResist = prof.resistances(); }
                }
            }
        }
        assertEquals(20, seen.size());
    }

    @Test
    void theFamousBossesParseAndCoverEveryTierOfBothSides() throws Exception {
        Path dir = DIR.resolve("famous");
        java.util.List<DndProfile> all = new java.util.ArrayList<>();
        Set<String> names = new HashSet<>();
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files.sorted()::iterator) {
                String id = "famous/" + p.getFileName().toString().replace(".json", "");
                DndProfile prof = DndProfile.parse(id, JsonParser.parseString(Files.readString(p)).getAsJsonObject());
                assertTrue(prof.famous(), id);
                assertTrue(prof.tierMin() <= prof.tierMax());
                assertFalse(prof.legendary().isEmpty(), id);
                assertTrue(names.add(prof.name()), "name used twice: " + prof.name());
                if (prof.legendary().contains("dragon_breath")) assertTrue(DndRules.BREATHS.contains(prof.breath()));
                all.add(prof);
            }
        }
        assertTrue(all.size() >= 15, "at least fifteen famous bosses, found " + all.size());
        for (String side : new String[] {"holy", "evil"}) {
            for (int tier = 2; tier <= 5; tier++) {
                int t = tier;
                assertTrue(all.stream().anyMatch(p -> (p.side().equals("any") || p.side().equals(side)) && t >= p.tierMin() && t <= p.tierMax()), "no famous boss for " + side + " tier " + tier);
            }
        }
        assertTrue(all.stream().anyMatch(DndProfile::reflective), "the Tarrasque's carapace");
        assertTrue(all.stream().anyMatch(p -> p.execution() && p.antiMagicAura()), "a Vecna");
    }

    @Test
    void theFamousPickIsFixedByTheFloorAndTheSectorAndFollowsThePercent() {
        int famous = 0;
        for (int sector = 0; sector < 1000; sector++) {
            int a = DndRules.pickFamous(450, sector, 50, 6), b = DndRules.pickFamous(450, sector, 50, 6);
            assertEquals(a, b, "the same sector and floor must give the same boss");
            assertTrue(a >= -1 && a < 6);
            if (a >= 0) famous++;
        }
        assertTrue(famous > 400 && famous < 600, "about half should be famous, were " + famous);
        assertEquals(-1, DndRules.pickFamous(450, 3, 0, 6));
        assertEquals(-1, DndRules.pickFamous(450, -1, 100, 6), "a Guardian with no sector is always ordinary");
        assertEquals(-1, DndRules.pickFamous(450, 3, 100, 0));
        assertTrue(DndRules.pickFamous(450, 3, 100, 6) >= 0);
        assertEquals(5, DndRules.tierOf(-5000));
        assertEquals(1, DndRules.tierOf(10));
        assertEquals(3, DndRules.tierOf(450));
    }

    @Test
    void aBadProfileSaysWhatIsWrong() {
        JsonObject o = JsonParser.parseString("{\"side\":\"holy\",\"tier\":1,\"variant\":0,\"name\":\"x\",\"forms\":[\"a\",\"b\",\"c\"],\"legendary_actions\":[{\"id\":\"fireball\",\"cost\":1}]}").getAsJsonObject();
        assertTrue(failure("bad", o).contains("fireball"));
        JsonObject two = JsonParser.parseString("{\"side\":\"holy\",\"tier\":1,\"variant\":0,\"name\":\"x\",\"forms\":[\"a\"]}").getAsJsonObject();
        assertTrue(failure("bad", two).contains("three"));
    }

    private static String failure(String id, JsonObject o) {
        try {
            DndProfile.parse(id, o);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        throw new AssertionError("the profile should have been refused");
    }
}
