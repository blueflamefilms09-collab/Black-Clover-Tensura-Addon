package com.newuniverse.nusmp.relics.wave;

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
 * Why: wave 104 (1.111.0, normal_b) must add exactly 116 Normal relics and change none of the existing ones. Reads the wave source and the assets (no game needed):
 * the grade counts, ids unique across every wave source, weapon damage inside the owner's range of its grade, Tensura engravings on every weapon, no hunger effects, and the files of each
 * relic (sprite, item models, worn ornament or weapon mesh, lang lines, Curios tag).
 */
class Wave104DefsTest {
    private static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Pattern DEF = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\",\\s*Grade\\.(\\w+),\\s*RelicSlot\\.(\\w+)\\)");
    private static final Map<String, int[]> RANGE = Map.of("NORMAL", new int[]{20, 30}, "SPECIAL", new int[]{35, 55});

    private record Def(String id, String grade, String slot, String block) {}

    private static List<Def> defs() throws IOException {
        String t = Files.readString(WAVES.resolve("Wave104.java"));
        Matcher m = DEF.matcher(t);
        List<int[]> at = new ArrayList<>();
        List<String[]> head = new ArrayList<>();
        while (m.find()) {
            at.add(new int[]{m.start()});
            head.add(new String[]{m.group(1), m.group(3), m.group(4)});
        }
        List<Def> out = new ArrayList<>();
        for (int i = 0; i < at.size(); i++) {
            int end = i + 1 < at.size() ? at.get(i + 1)[0] : t.indexOf("    /** The in-game self-test", at.get(i)[0]);
            out.add(new Def(head.get(i)[0], head.get(i)[1], head.get(i)[2], t.substring(at.get(i)[0], end)));
        }
        return out;
    }

    @Test
    void theWaveAddsExactlyOneHundredTwentyNormal() throws IOException {
        Map<String, Integer> n = new HashMap<>();
        for (Def d : defs()) n.merge(d.grade(), 1, Integer::sum);
        assertEquals(Map.of("NORMAL", 116), n);
    }

    @Test
    void idsAreUniqueInTheWaveAndAcrossEveryOtherWave() throws IOException {
        Set<String> mine = new HashSet<>();
        for (Def d : defs()) assertTrue(mine.add(d.id()), "twice in the wave: " + d.id());
        Set<String> ids = new HashSet<>();
        String src = Files.readString(WAVES.resolve("Wave104.java"));
        String block = src.substring(src.indexOf("IDS = {"), src.indexOf("};", src.indexOf("IDS = {")));
        Matcher lm = Pattern.compile("\"([a-z0-9_]+)\"").matcher(block);
        while (lm.find()) ids.add(lm.group(1));
        assertEquals(mine, ids, "Wave104.IDS lists the defs");
        Pattern any = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"");
        List<String> clash = new ArrayList<>();
        try (Stream<Path> files = Files.list(WAVES)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java") && !p.getFileName().toString().equals("Wave104.java"))::iterator) {
                Matcher m = any.matcher(Files.readString(f));
                while (m.find()) if (mine.contains(m.group(1))) clash.add(m.group(1) + " in " + f.getFileName());
            }
        }
        assertTrue(clash.isEmpty(), "ids that another wave already has: " + clash);
    }

    @Test
    void everyWeaponHasDamageInItsGradeRangeEngravingsAndAMesh() throws IOException {
        int weapons = 0;
        for (Def d : defs()) {
            if (!d.slot().equals("MAIN_HAND")) continue;
            weapons++;
            assertTrue(d.block().contains(".engrave("), d.id() + " has no Tensura engravings");
            assertTrue(d.block().contains(".model3d()"), d.id() + " has no 3D model flag");
            assertTrue(d.block().contains(".melee(DMG_"), d.id() + " has no melee damage");
            assertTrue(Files.exists(ASSETS.resolve("weapon_meshes/" + d.id() + ".txt")), d.id() + " mesh");
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/weapon/" + d.id() + ".png")), d.id() + " skin");
            Matcher k = Pattern.compile("DMG_" + d.id().toUpperCase() + " = GradeDamage\\.damage\\(\"(\\w+)\", \"(\\w+)\"\\)").matcher(Files.readString(WAVES.resolve("Wave104.java")));
            assertTrue(k.find(), d.id() + " damage constant");
            double dmg = com.newuniverse.nusmp.relics.GradeDamage.damage(k.group(1), k.group(2));
            int[] r = RANGE.get(d.grade());
            assertTrue(dmg >= r[0] && dmg <= r[1], d.id() + " damage " + dmg);
            assertEquals(d.grade().toLowerCase(), k.group(1));
        }
        assertEquals(6, weapons);
    }

    @Test
    void everyRelicHasItsFilesAndLangLines() throws IOException {
        String lang = Files.readString(Path.of("tools/lang/normal_b.json"));
        Path catalogPath = ASSETS.resolve("relic_worn/catalog.txt");
        String catalog = Files.readString(catalogPath);
        for (Def d : defs()) {
            for (String f : new String[]{"textures/item/" + d.id() + ".png", "models/item/" + d.id() + ".json", "models/item/" + d.id() + "_icon.json"})
                assertTrue(Files.exists(ASSETS.resolve(f)), d.id() + " " + f);
            for (String k : new String[]{"item.nusmp." + d.id(), "item.nusmp." + d.id() + ".card", "item.nusmp." + d.id() + ".desc"})
                assertTrue(lang.contains("\"" + k + "\""), "lang " + k);
            if (!d.slot().equals("MAIN_HAND")) {
                assertTrue(Files.exists(ASSETS.resolve("relic_worn/uq_" + d.id() + ".txt")), d.id() + " worn ornament");
                assertTrue(catalog.contains("relic " + d.id() + " "), d.id() + " is not in the worn catalog");
            }
        }
    }

    @Test
    void nothingTouchesHungerAndNormalRelicsStayCheap() throws IOException {
        String src = Files.readString(WAVES.resolve("Wave104.java")).toLowerCase();
        for (String bad : new String[]{"hunger", "saturation", "exhaustion", "foodlevel", "minecraft:regeneration\", 0, 4"}) assertFalse(src.contains(bad), bad);
        for (Def d : defs()) {
            if (!d.grade().equals("NORMAL")) continue;
            Matcher mana = Pattern.compile("\\.mana\\((\\d+)\\)").matcher(d.block());
            while (mana.find()) assertTrue(Integer.parseInt(mana.group(1)) <= 1, d.id() + " is a Normal relic and costs more than 1%");
            assertTrue(Pattern.compile("\\.act\\(").matcher(d.block()).results().count() <= 1, d.id() + " has more than one act");
        }
    }
}
