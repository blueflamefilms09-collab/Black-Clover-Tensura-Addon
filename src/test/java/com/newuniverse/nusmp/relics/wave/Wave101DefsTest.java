package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.GradeDamage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: wave 101 (1.111.0) adds fifteen Genesis grade relics (the seven Millennium Items and eight artifacts of eight other franchises) to balance the grade pyramid, and the request is
 * checkable without the game: exactly fifteen defs and all of the Genesis grade, ids no other wave has, none of the relics that existed before changed its grade, the Millennium Items in seven different slots
 * and without attunement (so the set can be worn at once), the other eight with attunement and in eight franchises, the three weapons 3D with Tensura engravings that exist and a base damage inside the
 * Genesis range, two or three acts each, a passive flag the events read, a lang entry with the name, the description and the card. The test reads the source (no game needed).
 */
class Wave101DefsTest {
    private static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    private static final Path LANG = Path.of("tools/lang/genesis_new.json");
    private static final Path BEFORE = Path.of("src/test/resources/relics_before_wave101.txt");
    private static final Set<String> TENSURA_ENGRAVINGS = Set.of("barrier_piercing", "breathing_support", "crushing", "dead_end_rainbow", "elemental_boost", "elemental_resistance",
            "energy_protection", "energy_steal", "enervation", "growth", "holy_coat", "holy_weapon", "intangibility", "lethargy", "magic_capacity", "magic_interference",
            "magic_protection", "magic_weapon", "magicule_absorption", "restoration", "ruination", "sealing", "severance", "severance_protection", "slotting", "soul_eater",
            "spiritual_protection", "stagnation", "sturdy", "swift", "transcendence", "tsukumogami", "vigor", "vitality");
    private static final List<String> MILLENNIUM = List.of("millennium_puzzle", "millennium_eye", "millennium_ring", "millennium_rod", "millennium_key", "millennium_scale", "millennium_necklace");
    private static final Map<String, String> SLOTS = Map.ofEntries(Map.entry("millennium_puzzle", "NECK"), Map.entry("millennium_eye", "HEAD"), Map.entry("millennium_ring", "HANDS"),
            Map.entry("millennium_rod", "MAIN_HAND"), Map.entry("millennium_key", "BELT"), Map.entry("millennium_scale", "CHARM"), Map.entry("millennium_necklace", "CHEST"),
            Map.entry("ea_sword_of_rupture", "MAIN_HAND"), Map.entry("requiem_stand_arrow", "MAIN_HAND"), Map.entry("truth_seeking_orbs_of_the_sage", "BACK"),
            Map.entry("hogyoku_of_the_crystal_dissolution", "CHARM"), Map.entry("seven_dragon_balls_of_shenron", "BELT"), Map.entry("straw_hat_of_joy_boy", "HEAD"),
            Map.entry("law_of_cycles_gown", "CHEST"), Map.entry("greed_island_original_binder", "CHARM"));

    private record Def(String id, String name, String grade, String slot, String block) {}

    private static String wave() throws IOException { return Files.readString(WAVES.resolve("Wave101.java")); }

    static List<Def> defs() throws IOException {
        String t = wave();
        Matcher m = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\",\\s*\"([^\"]+)\",\\s*Grade\\.(\\w+),\\s*RelicSlot\\.(\\w+)\\)").matcher(t);
        List<int[]> at = new ArrayList<>();
        List<String[]> head = new ArrayList<>();
        while (m.find()) {
            at.add(new int[]{m.start()});
            head.add(new String[]{m.group(1), m.group(2), m.group(3), m.group(4)});
        }
        List<Def> out = new ArrayList<>();
        for (int i = 0; i < at.size(); i++) {
            int end = i + 1 < at.size() ? at.get(i + 1)[0] : t.indexOf("    }", at.get(i)[0]);
            out.add(new Def(head.get(i)[0], head.get(i)[1], head.get(i)[2], head.get(i)[3], t.substring(at.get(i)[0], end)));
        }
        return out;
    }

    @Test
    void thereAreExactlyFifteenDefsAndAllAreGenesis() throws IOException {
        List<Def> defs = defs();
        assertEquals(15, defs.size());
        for (Def d : defs) assertEquals("GENESIS", d.grade(), d.id());
        assertEquals(SLOTS.keySet(), defs.stream().map(Def::id).collect(java.util.stream.Collectors.toSet()));
        for (Def d : defs) assertEquals(SLOTS.get(d.id()), d.slot(), d.id() + " slot");
    }

    @Test
    void theIdsAreListedInOrderAndNoOtherWaveHasThem() throws IOException {
        List<Def> defs = defs();
        String t = wave();
        Matcher ids = Pattern.compile("\"([a-z0-9_]+)\"").matcher(t.substring(t.indexOf("IDS = {"), t.indexOf("};")));
        List<String> listed = new ArrayList<>();
        while (ids.find()) listed.add(ids.group(1));
        assertEquals(defs.stream().map(Def::id).toList(), listed, "Wave101.IDS is the list of the defs, in order");
        Set<String> mine = new HashSet<>(listed);
        Pattern any = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");
        List<String> clash = new ArrayList<>();
        try (Stream<Path> files = Files.list(WAVES)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java") && !x.getFileName().toString().equals("Wave101.java"))::iterator) {
                Matcher m = any.matcher(Files.readString(f));
                while (m.find()) if (mine.contains(m.group(1))) clash.add(m.group(1) + " in " + f.getFileName());
            }
        }
        assertEquals(List.of(), clash, "an id that another wave already has");
    }

    @Test
    void noRelicOfTheOlderListChangedItsGradeAndNoneOfMineWasInIt() throws IOException {
        Map<String, String> before = new HashMap<>();
        for (String l : Files.readAllLines(BEFORE)) {
            String[] v = l.trim().split("\\s+");
            if (v.length == 2) before.put(v[0], v[1]);
        }
        assertEquals(653, before.size(), "the relics before wave 101");
        Map<String, Long> grades = new HashMap<>();
        for (String g : before.values()) grades.merge(g, 1L, Long::sum);
        assertEquals(20, grades.get("GENESIS").intValue());
        for (Def d : defs()) assertFalse(before.containsKey(d.id()), d.id() + " existed before");
        // every older relic whose id is a plain string literal still has its grade in the wave sources
        Pattern def = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\",\\s*\"(?:[^\"\\\\]|\\\\.)*\",\\s*Grade\\.(\\w+),");
        Map<String, String> now = new HashMap<>();
        try (Stream<Path> files = Files.list(WAVES)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                Matcher m = def.matcher(Files.readString(f));
                while (m.find()) now.put(m.group(1), m.group(2));
            }
        }
        List<String> changed = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<String, String> e : before.entrySet()) {
            String g = now.get(e.getKey());
            if (g == null) continue;                                      // an id written through a constant or an adopted older item: not a literal
            checked++;
            if (!g.equals(e.getValue())) changed.add(e.getKey() + " " + e.getValue() + " -> " + g);
        }
        assertTrue(checked > 300, "literal defs checked: " + checked);
        assertEquals(List.of(), changed);
    }

    @Test
    void theMillenniumItemsSitInSevenSlotsAndNeedNoAttunementTheOthersDo() throws IOException {
        Set<String> slots = new HashSet<>();
        for (Def d : defs()) {
            boolean attune = d.block().substring(0, Math.min(d.block().length(), 420)).contains(".attune()");
            if (MILLENNIUM.contains(d.id())) {
                slots.add(d.slot());
                assertFalse(attune, d.id() + " must work together with the other six: no attunement (the cap is three attuned relics)");
            } else {
                assertTrue(attune, d.id() + " needs attunement like the other Genesis relics");
            }
        }
        assertEquals(7, slots.size());
        assertEquals(MILLENNIUM, java.util.Arrays.asList(Wave101Rules.SET));
    }

    @Test
    void everyRelicHasPassivesTwoOrThreeActsAndItsFlagIsRead() throws IOException {
        List<String> bad = new ArrayList<>();
        String events = Files.readString(WAVES.resolve("Wave101Events.java")) + Files.readString(WAVES.resolve("Wave101Passives.java"));
        for (Def d : defs()) {
            int acts = d.block().split("\\.act\\(Act\\.of\\(", -1).length - 1;
            if (acts < 2 || acts > 3) bad.add(d.id() + " has " + acts + " acts");
            if (!d.block().contains("Passive.attr(") && !d.block().contains("Passive.resist(")) bad.add(d.id() + " has no passives");
            Matcher f = Pattern.compile("Passive\\.flag\\(\"(w101_[a-z_]+)\"\\)").matcher(d.block());
            while (f.find()) {
                String flag = f.group(1);
                boolean weapon = d.slot().equals("MAIN_HAND");
                boolean read = weapon ? flag.equals("w101_" + d.id()) && events.contains("\"" + d.id() + "\"") : events.contains("\"" + flag + "\"");
                if (!read) bad.add(d.id() + " has the flag " + flag + " that nothing reads");
            }
            if (d.id().equals("millennium_puzzle") && !d.block().contains("w101_puzzle")) bad.add("the puzzle has no second soul flag");
            if (d.block().contains("hungry") || d.block().toLowerCase().contains("minecraft:hunger") || d.block().contains("exhaustion")) bad.add(d.id() + " touches hunger");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void theThreeWeaponsAre3DWithEngravingsThatExistAndABaseDamageInsideTheGenesisRange() throws IOException {
        String t = wave();
        Matcher m = Pattern.compile("DMG_([A-Z0-9_]+) = GradeDamage\\.damage\\(\"genesis\", \"([a-z]+)\"\\)").matcher(t);
        int n = 0;
        while (m.find()) {
            n++;
            double d = GradeDamage.damage("genesis", m.group(2));
            int[] r = GradeDamage.range("genesis");
            assertTrue(d >= r[0] && d <= r[1], m.group(1) + " " + d);
            assertTrue(d >= 260 && d <= 350);
        }
        assertEquals(3, n);
        for (Def d : defs()) {
            if (!d.slot().equals("MAIN_HAND")) {
                assertFalse(d.block().contains(".model3d()") || d.block().contains(".melee("), d.id() + " is no weapon");
                continue;
            }
            assertTrue(d.block().contains(".model3d()"), d.id() + " is not 3D");
            Matcher melee = Pattern.compile("\\.melee\\(DMG_([A-Z0-9_]+),").matcher(d.block());
            assertTrue(melee.find(), d.id() + " has no .melee(DMG_..)");
            assertEquals(d.id().toUpperCase(), melee.group(1));
            Matcher e = Pattern.compile("\\.engrave\\(([^)]*)\\)").matcher(d.block());
            assertTrue(e.find(), d.id() + " has no engravings");
            Matcher one = Pattern.compile("\"([a-z_]+)(?:@(\\d))?\"").matcher(e.group(1));
            int count = 0;
            while (one.find()) {
                count++;
                assertTrue(TENSURA_ENGRAVINGS.contains(one.group(1)), d.id() + ": " + one.group(1) + " is not a Tensura engraving");
            }
            assertTrue(count >= 4, d.id() + " has only " + count + " engravings");
        }
    }

    @Test
    void everyRelicHasItsLangEntriesAndTheCardNamesEveryAct() throws IOException {
        String lang = Files.readString(LANG);
        for (Def d : defs()) {
            for (String k : new String[]{"item.nusmp." + d.id(), "item.nusmp." + d.id() + ".desc", "item.nusmp." + d.id() + ".card"})
                assertTrue(lang.contains("\"" + k + "\": \""), "no lang key " + k);
            assertTrue(lang.contains("\"item.nusmp." + d.id() + "\": \"" + d.name() + "\""), "the lang name of " + d.id() + " is " + d.name());
            Matcher m = Pattern.compile("Act\\.of\\(\"[a-z_]+\", \"([^\"]+)\"").matcher(d.block());
            while (m.find()) assertTrue(lang.contains(m.group(1)), d.id() + ": the card does not name the act " + m.group(1));
        }
        for (int i = 0; i < lang.length(); i++) assertTrue(lang.charAt(i) < 128, "ASCII only: char " + (int) lang.charAt(i) + " at " + i);
        assertFalse(lang.toLowerCase().contains("claude") || lang.toLowerCase().contains("anthropic"), "no assistant names in text");
    }

    @Test
    void theWaveStaysServerFriendly() throws IOException {
        // rule 9: no client class, no Minecraft.getInstance, no static "current player"; per-player state is keyed by uuid or kept in the player's own data
        for (String f : new String[]{"Wave101.java", "Wave101Acts.java", "Wave101Events.java", "Wave101Marks.java", "Wave101Set.java", "Wave101Passives.java", "Wave101Rules.java", "Wave101SelfTest.java"}) {
            String t = Files.readString(WAVES.resolve(f));
            assertFalse(t.contains("net.minecraft.client") || t.contains("com.mojang.blaze3d") || t.contains("com.newuniverse.nusmp.client"), f + " touches client classes");
            assertFalse(t.contains("Minecraft.getInstance"), f);
            assertFalse(t.contains("addExhaustion") || t.contains("FoodData") || t.contains("setFoodLevel"), f + " touches hunger");
        }
        String marks = Files.readString(WAVES.resolve("Wave101Marks.java"));
        assertTrue(marks.contains("Map<UUID"), "marks are keyed by uuid");
        assertTrue(marks.contains("CAP"), "the table is bounded");
        assertTrue(Files.readString(WAVES.resolve("Wave101Events.java")).contains("ServerStoppingEvent"), "the table is cleared when the server stops");
    }
}
