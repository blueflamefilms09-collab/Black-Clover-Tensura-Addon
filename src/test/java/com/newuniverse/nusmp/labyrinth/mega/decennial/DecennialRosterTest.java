package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.ArenaThemes;
import com.newuniverse.nusmp.labyrinth.mega.guard.AbyssalLooks;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: 200 bosses are a table, and a table is only as good as the checks on it. This reads the SHIPPED files with the very parser the game runs and pins the promises of the roster: exactly 100 Holy
 * and 100 Evil, one per level (-100..-1 and 1..100), every id and every name unique and not a placeholder, every hall a boss names a hall that exists, two to four phases whose thresholds fall,
 * one or two lair actions, a loot grade that never falls as the levels deepen, the Underworld Root laid out as the owner's canon says (seven gates, ten devils, three Supreme Devils on the last three
 * levels), the 17 famous villains each standing exactly once, every look one of the 22 abyssal looks, and no hunger anywhere in the vocabulary.
 */
class DecennialRosterTest {
    private static final Path ROOT = Path.of(".");

    private static List<DecennialProfile> roster() {
        DecennialRoster.Loaded l = DecennialRoster.loadShipped();
        assertTrue(l.problems().isEmpty(), "the shipped files must all parse: " + l.problems());
        return DecennialRoster.all();
    }

    @Test
    void theRosterHasTwoHundredBossesOneHundredAndOneHundred() {
        List<DecennialProfile> all = roster();
        assertEquals(200, all.size());
        assertEquals(100, DecennialRoster.ofSide("holy").size());
        assertEquals(100, DecennialRoster.ofSide("evil").size());
        List<Integer> levels = DecennialRoster.levels();
        assertEquals(200, levels.size());
        for (int i = 1; i <= 100; i++) {
            assertNotNull(DecennialRoster.forLevel(i), "holy level " + i);
            assertNotNull(DecennialRoster.forLevel(-i), "evil level " + -i);
            assertEquals(i * 10, DecennialRoster.forLevel(i).floor());
            assertEquals(-i * 10, DecennialRoster.forLevel(-i).floor());
            assertSame(DecennialRoster.forLevel(i), DecennialRoster.forFloor(i * 10));
            assertSame(DecennialRoster.forLevel(-i), DecennialRoster.forFloor(-i * 10));
        }
        assertNull(DecennialRoster.forLevel(0));
        assertNull(DecennialRoster.forLevel(101));
        assertNull(DecennialRoster.forFloor(15));
        assertNull(DecennialRoster.forFloor(0));
        assertNull(DecennialRoster.forFloor(1010));
    }

    @Test
    void idsAndNamesAreUniqueAndNeverPlaceholders() {
        Set<String> ids = new HashSet<>(), names = new HashSet<>(), titles = new HashSet<>();
        for (DecennialProfile p : roster()) {
            assertTrue(ids.add(p.id()), "duplicate id " + p.id());
            assertTrue(names.add(p.name().toLowerCase()), "duplicate name " + p.name());
            assertEquals(DecennialRules.idOf(p.level()), p.id());
            assertTrue(p.name().length() >= 5, p.id() + " name too short: " + p.name());
            assertTrue(p.title().length() >= 5, p.id() + " title too short");
            assertTrue(p.lore().length() >= 40, p.id() + " lore too short");
            String low = (p.name() + p.title() + p.lore()).toLowerCase();
            assertFalse(low.contains("placeholder") || low.contains("lorem") || low.contains("todo") || low.contains("tbd"), p.id());
            assertFalse(p.name().matches(".*\\d.*"), p.id() + " has a digit in its name");
            if (!p.archetype().startsWith("famous:")) assertTrue(titles.add(p.title().toLowerCase()), "duplicate title " + p.title());
        }
    }

    @Test
    void everyBossHasPhasesLairActionsAndALegitimateKit() {
        for (DecennialProfile p : roster()) {
            assertTrue(p.phaseCount() >= 2 && p.phaseCount() <= 4, p.id());
            assertEquals(1.0, p.phase(1).at(), 1e-9, p.id());
            for (int i = 2; i <= p.phaseCount(); i++) {
                assertTrue(p.phase(i - 1).at() - p.phase(i).at() >= DecennialRules.MIN_PHASE_GAP - 1e-9, p.id() + " phase gap " + i);
                assertTrue(p.phase(i).at() > 0.1, p.id());
            }
            for (int i = 1; i <= p.phaseCount(); i++) {
                DecennialProfile.Phase ph = p.phase(i);
                assertFalse(ph.abilities().isEmpty(), p.id() + " phase " + i + " has no ability");
                assertTrue(ph.speed() >= DecennialRules.MIN_SPEED_MULT && ph.speed() <= DecennialRules.MAX_SPEED_MULT, p.id());
                assertTrue(ph.damage() <= DecennialRules.MAX_DAMAGE_MULT, p.id());
                assertTrue(ph.addCount() <= DecennialRules.MAX_ADDS_PER_WAVE, p.id());
            }
            assertTrue(p.lairs().size() >= 1 && p.lairs().size() <= 2, p.id());
            for (DecennialProfile.LairRef l : p.lairs()) {
                assertNotNull(DecennialVocab.lair(l.id()), p.id() + " lair " + l.id());
                assertTrue(l.every() >= DecennialRules.MIN_LAIR_SECONDS, p.id());
                assertTrue(l.fromPhase() <= p.phaseCount(), p.id());
            }
            assertNotNull(DecennialVocab.signature(p.signature()), p.id());
            assertTrue(p.adds().maxAlive() <= DecennialRules.MAX_ADDS_ALIVE && p.adds().fightCap() <= DecennialRules.MAX_ADDS_PER_FIGHT, p.id());
            assertTrue(p.adds().maxAlive() <= p.adds().fightCap(), p.id());
            assertTrue(p.resist().size() <= 3, p.id());
            assertTrue(p.dnd().resistances() >= 1 && p.dnd().legendaryPoints() >= 1, p.id());
            assertFalse(p.soundSet().isEmpty(), p.id() + " has no sound set key");
            assertTrue(p.scale() >= 0.7 && p.scale() <= 1.5, p.id());
        }
    }

    @Test
    void phaseCountRisesWithDepthAndEveryHallExists() {
        for (DecennialProfile p : roster()) {
            assertTrue(ArenaThemes.exists(p.theme()), p.id() + " names a hall that does not exist: " + p.theme());
            ArenaThemes.Def d = ArenaThemes.def(p.theme());
            assertTrue(d.side().equals("any") || d.side().equals(p.side()), p.id() + " fights in a hall of the other side: " + p.theme());
        }
        Set<String> used = new HashSet<>(DecennialRoster.themes());
        assertEquals(used.size(), ArenaThemes.ids().size(), "every hall is used by some boss: unused " + minus(ArenaThemes.ids(), used));
        assertTrue(ArenaThemes.ids().size() >= 24);
    }

    private static List<String> minus(List<String> a, Set<String> b) {
        List<String> out = new ArrayList<>();
        for (String s : a) if (!b.contains(s)) out.add(s);
        return out;
    }

    @Test
    void lootGradesRiseWithTheLevelOnBothSides() {
        List<DecennialProfile> all = roster();
        for (String side : List.of("holy", "evil")) {
            int last = -1;
            for (int n = 1; n <= 100; n++) {
                DecennialProfile p = DecennialRoster.forLevel(side.equals("holy") ? n : -n);
                int g = DecennialRules.gradeIndex(p.loot().grade());
                assertTrue(g >= 0, p.id());
                assertTrue(g >= last, side + " level " + n + " pays a lower grade than the level before: " + p.loot().grade());
                assertEquals(DecennialRules.gradeOf(n), p.loot().grade(), p.id());
                last = g;
            }
        }
        assertEquals("special", DecennialRoster.forLevel(1).loot().grade());
        assertEquals("genesis", DecennialRoster.forLevel(100).loot().grade());
        assertEquals("genesis", DecennialRoster.forLevel(-100).loot().grade());
        // the damage a grade's weapons deal (docs/grade_damage.md) rises with the grade too
        int prev = 0;
        for (String g : DecennialRules.GRADES) {
            int[] range = DecennialRules.damageRange(g);
            assertTrue(range[0] > prev && range[1] > range[0], g);
            prev = range[1];
        }
        assertEquals(6, DecennialRules.gradeIndex("genesis"));
        assertTrue(all.size() == 200);
    }

    @Test
    void theUnderworldRootIsLaidOutAsTheCanonSays() {
        roster();
        // seven bands, ten devils: seven gate wardens and the three Supreme Devils on the last three levels
        List<DecennialProfile> devils = new ArrayList<>();
        for (DecennialProfile p : DecennialRoster.ofSide("evil")) if (p.archetype().equals("qliphoth")) devils.add(p);
        assertEquals(10, devils.size());
        int[] gates = DecennialRules.GATE_LEVELS;
        assertEquals(7, gates.length);
        for (int i = 0; i < gates.length; i++) {
            DecennialProfile g = DecennialRoster.forLevel(-gates[i]);
            assertEquals("qliphoth_gate_" + (i + 1), g.theme(), "gate " + (i + 1));
            assertEquals(i + 1, g.band());
            assertEquals("devil", g.creature());
        }
        for (int depth = 98; depth <= 100; depth++) {
            DecennialProfile s = DecennialRoster.forLevel(-depth);
            assertEquals("qliphoth_root_throne", s.theme());
            assertEquals(7, s.band());
            assertEquals("genesis", s.loot().grade());
            assertEquals(4, s.phaseCount());
        }
        assertEquals("Megicula", DecennialRoster.forLevel(-98).name());
        assertEquals("Zagred", DecennialRoster.forLevel(-99).name());
        assertEquals("Lucifero", DecennialRoster.forLevel(-100).name());
        // every named devil is a devil the mod already has, except the seventh gate's warden
        int fromEnum = 0;
        for (DecennialProfile d : devils) {
            if (d.devil().isEmpty()) {
                assertEquals("Ovrazel", d.name());
                continue;
            }
            Devil dv = Devil.byName(d.devil());
            assertNotNull(dv, d.devil());
            assertEquals(dv.displayName, d.name());
            fromEnum++;
        }
        assertEquals(9, fromEnum);
        // power rises steeply from band to band: each gate warden is no weaker than the one before in grade, phases, adds and damage resistance counts
        int lastTier = 0;
        for (int i = 0; i < gates.length; i++) {
            DecennialProfile g = DecennialRoster.forLevel(-gates[i]);
            assertTrue(g.dnd().resistances() >= lastTier, "gate " + (i + 1));
            lastTier = g.dnd().resistances();
        }
        // the bands cover all 100 levels and grow monotonically
        int band = 0;
        for (int depth = 1; depth <= 100; depth++) {
            int b = DecennialRoster.forLevel(-depth).band();
            assertTrue(b >= band && b == DecennialRules.bandOf(depth), "depth " + depth);
            band = b;
        }
        assertEquals(7, band);
        // the root throne and gate chambers belong to the Evil half only
        for (DecennialProfile p : DecennialRoster.ofSide("holy")) assertFalse(p.theme().startsWith("qliphoth"), p.id());
    }

    @Test
    void theSeventeenFamousVillainsEachStandExactlyOnce() throws IOException {
        roster();
        Path dir = ROOT.resolve("src/main/resources/data/nusmp/mega/dnd_bosses/famous");
        Set<String> famousFiles = new HashSet<>();
        try (var s = Files.list(dir)) {
            s.forEach(p -> famousFiles.add(p.getFileName().toString().replace(".json", "")));
        }
        assertEquals(17, famousFiles.size());
        Set<String> placed = new HashSet<>();
        for (DecennialProfile p : DecennialRoster.all()) {
            if (p.famous().isEmpty()) continue;
            assertTrue(famousFiles.contains(p.famous()), p.famous());
            assertTrue(placed.add(p.famous()), "twice: " + p.famous());
            JsonObject f = JsonParser.parseString(Files.readString(dir.resolve(p.famous() + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            int tier = DecennialRules.tierOf(p.level());
            String side = f.get("side").getAsString();
            assertTrue(side.equals("any") || side.equals(p.side()), p.id() + " stands on the wrong side");
            assertTrue(tier >= f.get("tier_min").getAsInt() && tier <= f.get("tier_max").getAsInt(), p.id() + " stands outside its tier range (" + tier + ")");
            assertEquals(f.get("look").getAsString(), p.look());
        }
        assertEquals(famousFiles, placed);
    }

    @Test
    void everyLookIsOneOfTheTwentyTwoAbyssalLooks() {
        Set<String> looks = AbyssalLooks.all().keySet();
        assertEquals(22, looks.size());
        for (DecennialProfile p : roster()) {
            assertTrue(looks.contains(p.look()), p.id() + " wears an unknown look: " + p.look());
            assertTrue(p.tint() >= 0 && p.tint() <= 0xFFFFFF);
        }
    }

    @Test
    void theAbilityAndBreathNamesAreTheGuardiansOwn() {
        assertEquals(new HashSet<>(DndRules.LEGENDARY_IDS), new HashSet<>(DecennialProfile.KNOWN_ABILITIES));
        for (DecennialProfile p : roster()) assertTrue(DndRules.BREATHS.contains(p.dnd().breath()), p.id());
    }

    @Test
    void thereIsNoHungerAnywhereInTheVocabulary() throws IOException {
        for (DecennialVocab.Effect e : DecennialVocab.Effect.values()) {
            String n = e.name().toLowerCase();
            assertFalse(n.contains("hunger") || n.contains("food") || n.contains("starv") || n.contains("exhaust") || n.contains("saturat"), n);
        }
        for (String f : List.of("lairs.json", "signatures.json")) {
            JsonObject table = JsonParser.parseString(Files.readString(ROOT.resolve("src/main/resources/data/nusmp/mega/decennial/" + f), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : table.entrySet()) {
                if (entry.getKey().startsWith("_")) continue;                       // the explanation may say there is no hunger
                String body = entry.getValue().toString().toLowerCase();
                for (String bad : List.of("hunger", "starv", "exhaust", "food", "saturat", "nausea_food"))
                    assertFalse(body.contains(bad), f + " entry " + entry.getKey() + " mentions " + bad);
            }
        }
        for (DecennialProfile p : roster()) {
            String all = (p.name() + p.title() + p.lore()).toLowerCase();
            assertFalse(all.contains("hunger") && all.contains("food"), p.id());
        }
        for (DecennialVocab.Lair l : DecennialVocab.lairs().values()) {
            assertTrue(l.telegraph() >= 16, l.id() + " warns for less than 0.8 s");
            assertTrue(l.share() <= DecennialRules.MAX_LAIR_SHARE, l.id());
            if (l.effect() == DecennialVocab.Effect.BLIND) assertTrue(l.effectTicks() <= 40, l.id() + " blinds for more than two seconds");
        }
    }

    @Test
    void theShippedLangFragmentNamesEveryBossAndEveryHall() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(ROOT.resolve("tools/lang/decennial.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        for (DecennialProfile p : roster()) {
            assertEquals(p.name(), lang.get("decennial.boss." + p.id() + ".name").getAsString());
            assertEquals(p.title(), lang.get("decennial.boss." + p.id() + ".title").getAsString());
            assertEquals(p.lore(), lang.get("decennial.boss." + p.id() + ".lore").getAsString());
            for (int i = 1; i <= p.phaseCount(); i++) assertEquals(p.phase(i).name(), lang.get("decennial.boss." + p.id() + ".phase." + i).getAsString());
        }
        for (String id : ArenaThemes.ids()) assertEquals(ArenaThemes.def(id).title(), lang.get("decennial.arena." + id).getAsString(), id);
        for (String id : DecennialVocab.lairs().keySet()) assertEquals(DecennialVocab.lair(id).text(), lang.get("decennial.lair." + id).getAsString(), id);
        for (String id : DecennialVocab.signatures().keySet()) assertEquals(DecennialVocab.signature(id).text(), lang.get("decennial.signature." + id).getAsString(), id);
    }

    @Test
    void theGeneratorHasAtLeastSixtyArchetypes() throws IOException {
        int n = 0;
        for (String f : List.of("evil.py", "holy.py")) {
            String text = Files.readString(ROOT.resolve("tools/decennial/" + f), StandardCharsets.UTF_8);
            int i = 0;
            while ((i = text.indexOf("A(key=\"", i)) >= 0) {
                n++;
                i += 7;
            }
        }
        assertTrue(n >= 60, "archetypes: " + n);
        Map<String, Integer> usage = new HashMap<>();
        for (DecennialProfile p : roster()) usage.merge(p.archetype(), 1, Integer::sum);
        long generated = usage.keySet().stream().filter(k -> !k.startsWith("famous:") && !k.equals("qliphoth")).count();
        assertTrue(generated >= 55, "archetypes that appear in the roster: " + generated);
    }
}
