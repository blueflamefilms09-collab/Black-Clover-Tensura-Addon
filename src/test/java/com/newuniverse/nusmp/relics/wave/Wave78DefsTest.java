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
 * Why: wave 78 (1.99.0) is the 13 boss weapons of the exclusive pocket tombs, written by a generator from a table, and the owner's rules for a weapon are checkable without the game: the grade
 * counts (4 Legend, 8 God, 1 Genesis), a unique id across every wave, a base damage inside the grade's range of the owner's table, Tensura engravings that exist (the #tensura:engraving tag), two acts
 * and a passive rule per weapon, a lang entry per weapon in the tombs' fragment, no Djinn vessel and no hunger anywhere, and the tie to the tombs: each weapon is the boss drop of exactly one tomb,
 * that tomb's boss loot table names it, and no other loot table of the mod does.
 */
class Wave78DefsTest {
    private static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    private static final Path LANG = Path.of("tools/lang/rift_pockets.json");
    private static final Path POCKETS = Path.of("src/main/resources/data/nusmp/mega/pockets");
    private static final Path LOOT = Path.of("src/main/resources/data/nusmp/loot_table");
    private static final Set<String> TENSURA_ENGRAVINGS = Set.of("barrier_piercing", "breathing_support", "crushing", "dead_end_rainbow", "elemental_boost", "elemental_resistance",
            "energy_protection", "energy_steal", "enervation", "growth", "holy_coat", "holy_weapon", "intangibility", "lethargy", "magic_capacity", "magic_interference",
            "magic_protection", "magic_weapon", "magicule_absorption", "restoration", "ruination", "sealing", "severance", "severance_protection", "slotting", "soul_eater",
            "spiritual_protection", "stagnation", "sturdy", "swift", "transcendence", "tsukumogami", "vigor", "vitality");

    private record Def(String id, String name, String grade, String block) {}

    private static String wave78() throws IOException { return Files.readString(WAVES.resolve("Wave78.java")); }

    static List<Def> defs() throws IOException {
        String t = wave78();
        Matcher m = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\",\\s*\"([^\"]+)\",\\s*Grade\\.(LEGEND|GOD|GENESIS),\\s*RelicSlot\\.MAIN_HAND\\)").matcher(t);
        List<int[]> at = new ArrayList<>();
        List<String[]> head = new ArrayList<>();
        while (m.find()) {
            at.add(new int[]{m.start()});
            head.add(new String[]{m.group(1), m.group(2), m.group(3)});
        }
        List<Def> out = new ArrayList<>();
        for (int i = 0; i < at.size(); i++) {
            int end = i + 1 < at.size() ? at.get(i + 1)[0] : t.indexOf("    }", at.get(i)[0]);
            out.add(new Def(head.get(i)[0], head.get(i)[1], head.get(i)[2], t.substring(at.get(i)[0], end)));
        }
        return out;
    }

    @Test
    void thereAreFourLegendEightGodAndOneGenesisWeapon() throws IOException {
        Map<String, Integer> n = new HashMap<>();
        for (Def d : defs()) n.merge(d.grade(), 1, Integer::sum);
        assertEquals(4, (int) n.getOrDefault("LEGEND", 0), "legend " + n);
        assertEquals(8, (int) n.getOrDefault("GOD", 0), "god " + n);
        assertEquals(1, (int) n.getOrDefault("GENESIS", 0), "genesis " + n);
        assertEquals(13, defs().size());
    }

    @Test
    void theIdsAreListedAndUniqueAcrossEveryWave() throws IOException {
        Set<String> seen = new HashSet<>();
        for (Def d : defs()) assertTrue(seen.add(d.id()), "twice in the wave: " + d.id());
        String t = wave78();
        Matcher ids = Pattern.compile("\"([a-z0-9_]+)\"").matcher(t.substring(t.indexOf("IDS = {"), t.indexOf("};", t.indexOf("IDS = {"))));
        Set<String> listed = new HashSet<>();
        while (ids.find()) listed.add(ids.group(1));
        assertEquals(seen, listed, "Wave78.IDS is the list of the defs");
        Pattern any = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");
        List<String> clash = new ArrayList<>();
        try (Stream<Path> files = Files.list(WAVES)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java") && !x.getFileName().toString().equals("Wave78.java"))::iterator) {
                Matcher m = any.matcher(Files.readString(f));
                while (m.find()) if (seen.contains(m.group(1))) clash.add(m.group(1) + " in " + f.getFileName());
            }
        }
        assertEquals(List.of(), clash, "an id that another wave already has");
    }

    @Test
    void everyWeaponIsA3DMainHandWeaponWithEngravingsTwoActsAndNoVessel() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Def d : defs()) {
            String b = d.block();
            if (!b.contains(".model3d()")) bad.add(d.id() + " is not 3D");
            if (!b.contains(".attune()")) bad.add(d.id() + " needs attunement from Legend up");
            if (b.contains(".vessel()")) bad.add(d.id() + " is a Djinn vessel: a tomb's weapon is the tomb's alone");
            Matcher e = Pattern.compile("\\.engrave\\(([^)]*)\\)").matcher(b);
            if (!e.find()) {
                bad.add(d.id() + " has no engravings");
                continue;
            }
            Matcher one = Pattern.compile("\"([a-z_]+)(?:@(\\d))?\"").matcher(e.group(1));
            int count = 0;
            while (one.find()) {
                count++;
                if (!TENSURA_ENGRAVINGS.contains(one.group(1))) bad.add(d.id() + ": " + one.group(1) + " is not a Tensura engraving");
            }
            if (count < 3) bad.add(d.id() + " has only " + count + " engravings");
            int acts = b.split("\\.act\\(Act\\.of\\(", -1).length - 1;
            if (acts != 2) bad.add(d.id() + " has " + acts + " acts");
            if (!b.contains("Passive.flag(\"w78_" + d.id() + "\")")) bad.add(d.id() + " has no passive flag");
            if (!Wave78Passives.RULES.containsKey(d.id())) bad.add(d.id() + " has no passive rule");
            if (b.toLowerCase().contains("hunger") || b.toLowerCase().contains("saturation")) bad.add(d.id() + " mentions hunger");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyBaseDamageIsInsideTheRangeOfItsGrade() throws IOException {
        String t = wave78();
        Matcher m = Pattern.compile("DMG_([A-Z0-9_]+) = GradeDamage\\.damage\\(\"(legend|god|genesis)\", \"([a-z]+)\"\\)").matcher(t);
        int n = 0;
        while (m.find()) {
            n++;
            double d = GradeDamage.damage(m.group(2), m.group(3));
            int[] r = GradeDamage.range(m.group(2));
            assertTrue(d >= r[0] && d <= r[1], m.group(1) + " " + d + " outside " + r[0] + "-" + r[1]);
        }
        assertEquals(13, n);
        for (Def d : defs()) {
            Matcher melee = Pattern.compile("\\.melee\\(DMG_([A-Z0-9_]+),").matcher(d.block());
            assertTrue(melee.find(), d.id() + " has no .melee(DMG_..)");
            assertEquals(d.id().toUpperCase(), melee.group(1), "each weapon uses its own damage constant");
        }
    }

    @Test
    void theBowIsMarkedAndTheRestAreNot() throws IOException {
        for (Def d : defs()) {
            assertEquals(d.id().endsWith("_bow"), d.block().contains(".bow()"), d.id());
            assertFalse(d.block().contains(".crossbow()"), d.id());
        }
    }

    @Test
    void everyWeaponHasItsLangEntriesAndItsActNamesAreInTheCard() throws IOException {
        String lang = Files.readString(LANG);
        for (Def d : defs()) {
            for (String k : new String[]{"item.nusmp." + d.id(), "item.nusmp." + d.id() + ".desc", "item.nusmp." + d.id() + ".card"})
                assertTrue(lang.contains("\"" + k + "\": \""), "no lang key " + k);
            assertTrue(lang.contains("\"item.nusmp." + d.id() + "\": " + "\"" + d.name() + "\""), "the lang name of " + d.id() + " is " + d.name());
            Matcher m = Pattern.compile("Act\\.of\\(\"[a-z_]+\", \"([^\"]+)\"").matcher(d.block());
            int found = 0;
            while (m.find()) {
                found++;
                assertTrue(lang.contains(m.group(1) + " ("), d.id() + ": the card text does not describe " + m.group(1));
            }
            assertEquals(2, found, d.id());
        }
        for (int i = 0; i < lang.length(); i++) assertTrue(lang.charAt(i) < 128, "ASCII only: char " + (int) lang.charAt(i) + " at " + i);
        assertFalse(lang.toLowerCase().contains("claude") || lang.toLowerCase().contains("anthropic"), "no assistant names in text");
    }

    @Test
    void eachWeaponIsTheBossDropOfExactlyOneTombAndOfNoOtherTable() throws IOException {
        Map<String, String> tombOf = new HashMap<>();
        Matcher t = Pattern.compile("TOMBS = \\{(.*?)\\};", Pattern.DOTALL).matcher(wave78());
        Matcher i = Pattern.compile("IDS = \\{(.*?)\\};", Pattern.DOTALL).matcher(wave78());
        assertTrue(t.find() && i.find());
        List<String> ids = new ArrayList<>(), tombs = new ArrayList<>();
        Matcher a = Pattern.compile("\"([a-z0-9_]+)\"").matcher(i.group(1));
        while (a.find()) ids.add(a.group(1));
        Matcher b = Pattern.compile("\"([a-z0-9_]+)\"").matcher(t.group(1));
        while (b.find()) tombs.add(b.group(1));
        assertEquals(ids.size(), tombs.size());
        for (int k = 0; k < ids.size(); k++) tombOf.put(ids.get(k), tombs.get(k));
        for (Map.Entry<String, String> e : tombOf.entrySet()) {
            String pocket = Files.readString(POCKETS.resolve(e.getValue() + ".json"));
            assertTrue(pocket.contains("\"" + e.getKey() + "\""), e.getValue() + " does not list " + e.getKey() + " among its relics");
            String boss = Files.readString(LOOT.resolve("pocket/" + e.getValue() + "/boss.json"));
            assertTrue(boss.contains("\"relic\": \"" + e.getKey() + "\"") && boss.contains("\"pocket\": \"" + e.getValue() + "\""), e.getValue() + "'s boss table does not drop " + e.getKey());
        }
        try (Stream<Path> files = Files.walk(LOOT)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".json"))::iterator) {
                String s = Files.readString(f);
                for (String id : ids) {
                    if (!s.contains("\"" + id + "\"")) continue;
                    String rel = LOOT.relativize(f).toString().replace('\\', '/');
                    assertEquals("pocket/" + tombOf.get(id) + "/boss.json", rel, id + " appears in " + rel);
                }
            }
        }
    }
}
