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
 * Why: wave 71 (1.97.0) is 35 weapons of Legend, God and Genesis grade written by a generator from a table, and the owner's rules for a weapon are checkable without the game:
 * the grade counts (at least 12 / 12 / 10), a unique id, a base damage inside the grade's range of the owner's table (relics/GradeDamage), Tensura engravings that exist (the
 * #tensura:engraving tag), two or three acts and a passive rule per weapon, a lang entry per weapon. This test reads the generated source (no game needed).
 */
class Wave71DefsTest {
    private static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    private static final Path LANG = Path.of("tools/lang/new_weapons_197.json");
    /** Tensura's engravings (the enchantments of the #tensura:engraving tag). */
    private static final Set<String> TENSURA_ENGRAVINGS = Set.of("barrier_piercing", "breathing_support", "crushing", "dead_end_rainbow", "elemental_boost", "elemental_resistance",
            "energy_protection", "energy_steal", "enervation", "growth", "holy_coat", "holy_weapon", "intangibility", "lethargy", "magic_capacity", "magic_interference",
            "magic_protection", "magic_weapon", "magicule_absorption", "restoration", "ruination", "sealing", "severance", "severance_protection", "slotting", "soul_eater",
            "spiritual_protection", "stagnation", "sturdy", "swift", "transcendence", "tsukumogami", "vigor", "vitality");

    private record Def(String id, String name, String grade, String block) {}

    private static String wave71() throws IOException { return Files.readString(WAVES.resolve("Wave71.java")); }

    static List<Def> defs() throws IOException {
        String t = wave71();
        Pattern p = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\",\\s*\"([^\"]+)\",\\s*Grade\\.(LEGEND|GOD|GENESIS),\\s*RelicSlot\\.MAIN_HAND\\)");
        Matcher m = p.matcher(t);
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
    void thereAreAtLeastTwelveLegendTwelveGodAndTenGenesisWeapons() throws IOException {
        Map<String, Integer> n = new HashMap<>();
        for (Def d : defs()) n.merge(d.grade(), 1, Integer::sum);
        assertTrue(n.getOrDefault("LEGEND", 0) >= 12, "legend " + n);
        assertTrue(n.getOrDefault("GOD", 0) >= 12, "god " + n);
        assertTrue(n.getOrDefault("GENESIS", 0) >= 10, "genesis " + n);
        assertEquals(35, defs().size());
    }

    @Test
    void theIdsOfTheWaveAreListedAndUniqueAcrossEveryWave() throws IOException {
        List<Def> defs = defs();
        Set<String> seen = new HashSet<>();
        for (Def d : defs) assertTrue(seen.add(d.id()), "twice in the wave: " + d.id());
        Matcher ids = Pattern.compile("\"([a-z0-9_]+)\"").matcher(wave71().substring(wave71().indexOf("IDS = {"), wave71().indexOf("};")));
        Set<String> listed = new HashSet<>();
        while (ids.find()) listed.add(ids.group(1));
        assertEquals(seen, listed, "Wave71.IDS is the list of the defs");
        Pattern any = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");
        List<String> clash = new ArrayList<>();
        try (Stream<Path> files = Files.list(WAVES)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java") && !x.getFileName().toString().equals("Wave71.java"))::iterator) {
                Matcher m = any.matcher(Files.readString(f));
                while (m.find()) if (seen.contains(m.group(1))) clash.add(m.group(1) + " in " + f.getFileName());
            }
        }
        assertEquals(List.of(), clash, "an id that another wave already has");
    }

    @Test
    void everyWeaponIsA3DMainHandWeaponWithEngravingsAndTwoOrThreeActs() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Def d : defs()) {
            String b = d.block();
            if (!b.contains(".model3d()")) bad.add(d.id() + " is not 3D");
            if (!b.contains(".attune()")) bad.add(d.id() + " needs attunement from Legend up");
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
            if (acts < 2 || acts > 3) bad.add(d.id() + " has " + acts + " acts");
            if (!b.contains("Passive.flag(\"w71_" + d.id() + "\")")) bad.add(d.id() + " has no passive flag");
            if (!Wave71Passives.RULES.containsKey(d.id())) bad.add(d.id() + " has no passive rule");
            if (b.contains(".usesPerDay(") && !b.contains("Act.of")) bad.add(d.id() + " uses per day without an act");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyBaseDamageIsInsideTheRangeOfItsGradeAndFollowsTheWeaponType() throws IOException {
        String t = wave71();
        Matcher m = Pattern.compile("DMG_([A-Z0-9_]+) = GradeDamage\\.damage\\(\"(legend|god|genesis)\", \"([a-z]+)\"\\)").matcher(t);
        Map<String, double[]> seen = new HashMap<>();
        int n = 0;
        while (m.find()) {
            n++;
            double d = GradeDamage.damage(m.group(2), m.group(3));
            int[] r = GradeDamage.range(m.group(2));
            assertTrue(d >= r[0] && d <= r[1], m.group(1) + " " + d + " outside " + r[0] + "-" + r[1]);
            seen.put(m.group(1), new double[]{d});
        }
        assertEquals(35, n);
        for (Def d : defs()) {
            Matcher melee = Pattern.compile("\\.melee\\(DMG_([A-Z0-9_]+),").matcher(d.block());
            assertTrue(melee.find(), d.id() + " has no .melee(DMG_..)");
            assertEquals(d.id().toUpperCase(), melee.group(1), "each weapon uses its own damage constant");
        }
        // the owner's table: the ranges are disjoint and a light weapon sits below a heavy one inside every grade
        assertEquals(130, GradeDamage.range("legend")[0]);
        assertEquals(175, GradeDamage.range("legend")[1]);
        assertEquals(190, GradeDamage.range("god")[0]);
        assertEquals(240, GradeDamage.range("god")[1]);
        assertEquals(260, GradeDamage.range("genesis")[0]);
        assertEquals(350, GradeDamage.range("genesis")[1]);
        for (String g : new String[]{"legend", "god", "genesis"}) {
            assertTrue(GradeDamage.damage(g, "dagger") < GradeDamage.damage(g, "longsword"), g);
            assertTrue(GradeDamage.damage(g, "longsword") < GradeDamage.damage(g, "greatsword"), g);
        }
    }

    @Test
    void rangedWeaponsAreMarkedAndTheRestAreNot() throws IOException {
        for (Def d : defs()) {
            boolean bow = d.id().endsWith("_bow"), cross = d.id().endsWith("_crossbow");
            assertEquals(bow, d.block().contains(".bow()"), d.id());
            assertEquals(cross, d.block().contains(".crossbow()"), d.id());
        }
    }

    @Test
    void everyWeaponHasItsLangEntries() throws IOException {
        String lang = Files.readString(LANG);
        assertTrue(lang.startsWith("{"));
        for (Def d : defs()) {
            for (String k : new String[]{"item.nusmp." + d.id(), "item.nusmp." + d.id() + ".desc", "item.nusmp." + d.id() + ".card"})
                assertTrue(lang.contains("\"" + k + "\": \""), "no lang key " + k);
            assertTrue(lang.contains("\"item.nusmp." + d.id() + "\": " + "\"" + d.name() + "\""), "the lang name of " + d.id() + " is " + d.name());
        }
        for (int i = 0; i < lang.length(); i++) assertTrue(lang.charAt(i) < 128, "ASCII only: char " + (int) lang.charAt(i) + " at " + i);
        assertFalse(lang.toLowerCase().contains("claude") || lang.toLowerCase().contains("anthropic"), "no assistant names in text");
    }

    @Test
    void everyActNameIsInItsCardText() throws IOException {
        String lang = Files.readString(LANG);
        for (Def d : defs()) {
            Matcher m = Pattern.compile("Act\\.of\\(\"[a-z_]+\", \"([^\"]+)\"").matcher(d.block());
            int found = 0;
            while (m.find()) {
                found++;
                assertTrue(lang.contains(m.group(1) + " ("), d.id() + ": the card text does not describe " + m.group(1));
            }
            assertTrue(found >= 2, d.id());
        }
    }
}
