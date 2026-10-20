package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the arithmetic every part of the feature leans on (which level a floor is, what grade a level pays, how the Evil half is cut into the seven gates of the Underworld Root) and the strict
 * readers (a profile, the lair table) must say plainly what is wrong with a bad file and clamp what is merely too big, because a datapack author will get them wrong. This pins both.
 */
class DecennialRulesTest {
    @Test
    void levelsAndFloorsConvertBothWays() {
        assertEquals(30, DecennialRules.floorOf(3));
        assertEquals(-30, DecennialRules.floorOf(-3));
        assertEquals(3, DecennialRules.levelOfFloor(30));
        assertEquals(-100, DecennialRules.levelOfFloor(-1000));
        assertEquals(0, DecennialRules.levelOfFloor(0));
        assertEquals(0, DecennialRules.levelOfFloor(25));
        assertEquals(0, DecennialRules.levelOfFloor(1010));
        assertEquals(1, DecennialRules.levelContaining(1));
        assertEquals(1, DecennialRules.levelContaining(10));
        assertEquals(2, DecennialRules.levelContaining(11));
        assertEquals(-2, DecennialRules.levelContaining(-11));
        assertEquals(-100, DecennialRules.levelContaining(-1000));
        assertEquals(0, DecennialRules.levelContaining(-1001));
        assertTrue(DecennialRules.validLevel(-100) && DecennialRules.validLevel(1) && !DecennialRules.validLevel(0) && !DecennialRules.validLevel(101));
        for (int l = -100; l <= 100; l++) {
            if (l == 0) continue;
            assertEquals(l, DecennialRules.levelOfFloor(DecennialRules.floorOf(l)));
            assertEquals(l, DecennialRules.levelOfId(DecennialRules.idOf(l)));
            assertEquals(l < 0 ? "evil" : "holy", DecennialRules.side(l));
        }
        assertEquals("evil_007", DecennialRules.idOf(-7));
        assertEquals("holy_100", DecennialRules.idOf(100));
        assertEquals(0, DecennialRules.levelOfId("holy_000"));
        assertEquals(0, DecennialRules.levelOfId("holy_101"));
        assertEquals(0, DecennialRules.levelOfId("dark_001"));
        assertEquals(0, DecennialRules.levelOfId("holy_0x1"));
        assertEquals(0, DecennialRules.levelOfId(null));
    }

    @Test
    void theGradeOnlyEverRises() {
        int last = -1;
        for (int n = 1; n <= 100; n++) {
            int g = DecennialRules.gradeIndex(DecennialRules.gradeOf(n));
            assertTrue(g >= last);
            last = g;
            assertEquals(DecennialRules.gradeOf(n), DecennialRules.gradeOf(-n), "both sides pay the same grade at the same depth");
        }
        assertEquals("special", DecennialRules.gradeOf(1));
        assertEquals("genesis", DecennialRules.gradeOf(100));
        assertEquals(1, DecennialRules.tierOf(1));
        assertEquals(1, DecennialRules.tierOf(20));
        assertEquals(2, DecennialRules.tierOf(21));
        assertEquals(5, DecennialRules.tierOf(81));
        assertEquals(5, DecennialRules.tierOf(-100));
        assertEquals(35, DecennialRules.damageRange("special")[0]);
        assertEquals(350, DecennialRules.damageRange("genesis")[1]);
    }

    @Test
    void theBandsOfTheUnderworldRootCoverTheEvilHalfInSevenGates() {
        assertEquals(1, DecennialRules.bandOf(1));
        assertEquals(1, DecennialRules.bandOf(14));
        assertEquals(2, DecennialRules.bandOf(15));
        assertEquals(4, DecennialRules.bandOf(57));
        assertEquals(5, DecennialRules.bandOf(58));
        assertEquals(7, DecennialRules.bandOf(86));
        assertEquals(7, DecennialRules.bandOf(100));
        assertEquals(0, DecennialRules.bandOf(0));
        assertEquals(0, DecennialRules.bandOf(101));
        int last = 0;
        for (int d = 1; d <= 100; d++) {
            int b = DecennialRules.bandOf(d);
            assertTrue(b == last || b == last + 1);
            last = b;
        }
        int gates = 0;
        for (int d = 1; d <= 100; d++) if (DecennialRules.gateBandOf(d) > 0) gates++;
        assertEquals(7, gates);
        assertEquals("qliphoth_gate_3", DecennialRules.qliphothTheme(42));
        assertEquals("qliphoth_gate_7", DecennialRules.qliphothTheme(97));
        assertEquals("qliphoth_root_throne", DecennialRules.qliphothTheme(98));
        assertEquals("qliphoth_root_throne", DecennialRules.qliphothTheme(100));
        assertNull(DecennialRules.qliphothTheme(50));
        assertEquals(8, DecennialRules.QLIPHOTH_THEMES.size());
        // every gate level closes its band: the next level starts the next band
        for (int i = 0; i < DecennialRules.GATE_LEVELS.length - 1; i++) assertEquals(DecennialRules.GATE_LEVELS[i] + 1, DecennialRules.BAND_START[i + 1]);
    }

    private static JsonObject profile() {
        DecennialRoster.loadShipped();
        return JsonParser.parseString(readShipped("evil_005")).getAsJsonObject();
    }

    private static String readShipped(String id) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/nusmp/mega/decennial/bosses/" + id + ".json"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String problemOf(String id, JsonObject o) {
        try {
            DecennialProfile.parse(id, o);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Test
    void aBadProfileIsRefusedInPlainWordsAndAGoodOneParses() {
        assertNull(problemOf("evil_005", profile()));
        assertTrue(problemOf("evil_500", profile()).contains("id must be"));
        assertTrue(problemOf("holy_005", profile()).contains("level"), "an evil file under a holy id");
        JsonObject o = profile();
        o.addProperty("theme", "Bad Theme");
        assertTrue(problemOf("evil_005", o).contains("arena id"));
        o = profile();
        o.addProperty("creature", "chair");
        assertTrue(problemOf("evil_005", o).contains("creature"));
        o = profile();
        o.addProperty("signature", "nope");
        assertTrue(problemOf("evil_005", o).contains("signature"));
        o = profile();
        o.addProperty("name", "TODO");
        assertNotNull(problemOf("evil_005", o));
        o = profile();
        o.getAsJsonArray("phases").remove(1);
        assertTrue(problemOf("evil_005", o).contains("phases"), "one phase is not enough");
        o = profile();
        o.getAsJsonArray("phases").get(1).getAsJsonObject().addProperty("at", 0.99);
        assertTrue(problemOf("evil_005", o).contains("too close"));
        o = profile();
        o.getAsJsonArray("lair_actions").get(0).getAsJsonObject().addProperty("id", "hunger_pangs");
        assertTrue(problemOf("evil_005", o).contains("unknown lair"));
        o = profile();
        o.getAsJsonObject("loot").addProperty("grade", "genesis");
        assertTrue(problemOf("evil_005", o).contains("grade"));
        o = profile();
        o.addProperty("tint", "red");
        assertTrue(problemOf("evil_005", o).contains("tint"));
        o = profile();
        o.getAsJsonArray("phases").get(1).getAsJsonObject().getAsJsonArray("abilities").add("hunger");
        assertTrue(problemOf("evil_005", o).contains("unknown ability"));
        o = profile();
        o.addProperty("scale", 9.0);
        DecennialProfile clamped = DecennialProfile.parse("evil_005", o);
        assertEquals(1.5, clamped.scale(), 1e-9, "a silly size is clamped, not obeyed");
        o = profile();
        o.getAsJsonArray("phases").get(1).getAsJsonObject().addProperty("damage", 9.0);
        o.getAsJsonArray("phases").get(1).getAsJsonObject().addProperty("speed", 9.0);
        DecennialProfile p = DecennialProfile.parse("evil_005", o);
        assertEquals(DecennialRules.MAX_DAMAGE_MULT, p.phase(2).damage(), 1e-9);
        assertEquals(DecennialRules.MAX_SPEED_MULT, p.phase(2).speed(), 1e-9);
    }

    @Test
    void theRosterSkipsABadFileAndKeepsTheRest() {
        Map<String, String> files = new HashMap<>();
        files.put("evil_005", readShipped("evil_005"));
        files.put("evil_006", "{\"side\": \"evil\"}");
        files.put("evil_007", "not json");
        DecennialRoster.Loaded l = DecennialRoster.parseAll(files);
        assertEquals(1, l.profiles().size());
        assertEquals(2, l.problems().size());
        assertTrue(l.problems().get(0).startsWith("evil_006"));
        Map<String, String> twice = new HashMap<>();
        twice.put("evil_005", readShipped("evil_005"));
        assertEquals(0, DecennialRoster.parseAll(twice).problems().size());
    }

    @Test
    void theLairTableIsClampedAndRefusesWhatItDoesNotKnow() {
        DecennialVocab.Lair l = DecennialVocab.parseLairs("{\"x_test\": {\"pattern\": \"ring_pulse\", \"effect\": \"blind\", \"share\": 9, \"telegraph\": 1, \"radius\": 99, \"effect_ticks\": 999, \"text\": \"t\"}}").get("x_test");
        assertEquals(DecennialRules.MAX_LAIR_SHARE, l.share(), 1e-9);
        assertEquals(16, l.telegraph(), "at least 0.8 s of warning");
        assertEquals(20.0, l.radius(), 1e-9);
        assertEquals(40, l.effectTicks(), "two seconds of blindness at most");
        String[] bad = {"{\"a_b\": {\"pattern\": \"nope\", \"text\": \"t\"}}", "{\"a_b\": {\"pattern\": \"zone\", \"effect\": \"hunger\", \"text\": \"t\"}}",
                "{\"a_b\": {\"pattern\": \"zone\", \"particle\": \"lava_drip_x\", \"text\": \"t\"}}", "{\"a_b\": {\"pattern\": \"zone\"}}", "{\"A\": {\"pattern\": \"zone\", \"text\": \"t\"}}"};
        for (String b : bad) {
            boolean threw = false;
            try {
                DecennialVocab.parseLairs(b);
            } catch (IllegalArgumentException e) {
                threw = true;
            }
            assertTrue(threw, b);
        }
        boolean threw = false;
        try {
            DecennialVocab.parseSignatures("{\"s\": {\"kind\": \"starve\", \"text\": \"t\"}}");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw);
    }
}
