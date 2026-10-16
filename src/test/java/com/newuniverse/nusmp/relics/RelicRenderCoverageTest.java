package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the part 4 relics (waves 20 to 30) were added by data in the wave sources, and nothing checked that the shared mechanism reaches each of them: a Curios
 * relic whose slot and look the worn-model catalog does not know is drawn as plain boxes (or looks absent), a Curios relic missing from its slot tag cannot be
 * worn at all, and a held weapon without its mesh and item model is invisible in hand. This test reads the wave sources and the asset files (no game needed)
 * and fails when any relic of any wave misses one of those steps.
 */
class RelicRenderCoverageTest {
    private static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final Path TAGS = Path.of("src/main/resources/data/curios/tags/item");
    private static final Pattern CONST = Pattern.compile("(\\w+)\\s*=\\s*\"([a-z0-9_]+)\"");
    private static final Pattern DEF = Pattern.compile("RelicDef\\s*\\.\\s*of\\s*\\(\\s*([^,]+?)\\s*,\\s*(?:\"(?:[^\"\\\\]|\\\\.)*\"|[^,]+),\\s*Grade\\.\\w+\\s*,\\s*RelicSlot\\.(\\w+)");

    private record Def(String id, String slot, String look, boolean adopted, int wave) {}

    private static List<Def> defs() throws IOException {
        Map<String, String> consts = new HashMap<>();
        Map<Path, String> texts = new HashMap<>();
        try (Stream<Path> s = Files.list(WAVES)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                String t = Files.readString(p);
                texts.put(p, t);
                Matcher m = CONST.matcher(t);
                String cls = p.getFileName().toString().replace(".java", "");
                while (m.find()) consts.putIfAbsent(cls + "." + m.group(1), m.group(2));      // keyed by class: two waves both have an ID_MIMIC
            }
        }
        List<Def> out = new ArrayList<>();
        for (Map.Entry<Path, String> e : texts.entrySet()) {
            Matcher wave = Pattern.compile("Wave(\\d+)\\.java").matcher(e.getKey().getFileName().toString());
            int n = wave.matches() ? Integer.parseInt(wave.group(1)) : -1;
            String t = e.getValue();
            Matcher m = DEF.matcher(t);
            while (m.find()) {
                String arg = m.group(1).trim();
                String id = arg.startsWith("\"") ? arg.substring(1, arg.length() - 1) : consts.get(arg.contains(".") ? arg : e.getKey().getFileName().toString().replace(".java", "") + "." + arg);
                if (id == null) continue;                                           // a def built from a variable: the lang and tag checks of its own wave cover it
                int end = t.indexOf(";", m.end());
                String chain = t.substring(m.start(), end < 0 ? t.length() : end);
                Matcher lk = Pattern.compile("\\.look\\(\\s*Look\\.(\\w+)").matcher(chain);
                String slot = m.group(2);
                String look = lk.find() ? lk.group(1) : Look.defaultFor(RelicSlot.valueOf(slot)).name();
                out.add(new Def(id, slot, look, chain.contains(".adopt("), n));
            }
        }
        return out;
    }

    @Test
    void sourcesAreFound() throws IOException {
        long part4 = defs().stream().filter(d -> d.wave() >= 20).count();
        assertTrue(part4 >= 100, "the parser should see the 124 relics of waves 20 to 30, saw " + part4);
    }

    @Test
    void everyWornRelicHasAWornModelInTheCatalog() throws IOException {
        WornCatalog cat = WornCatalog.parse(Files.readAllLines(ASSETS.resolve("relic_worn/catalog.txt")));
        List<String> missing = new ArrayList<>();
        for (Def d : defs()) {
            if (d.adopted() || d.slot().equals("MAIN_HAND") || d.slot().equals("CONSUMABLE")) continue;
            if (cat.piecesFor(d.id(), d.slot(), d.look()).isEmpty()) missing.add(d.id() + " (" + d.slot() + " " + d.look() + ")");
        }
        assertTrue(missing.isEmpty(), "no worn model in relic_worn/catalog.txt for: " + missing + " (add a look line in tools/relics/gen_worn_all.py)");
    }

    @Test
    void everyCuriosRelicIsInItsSlotTag() throws IOException {
        Map<String, String> tag = Map.of("NECK", "necklace", "BACK", "back", "BELT", "belt", "HANDS", "hands", "CHARM", "charm");
        Map<String, String> tagText = new HashMap<>();
        for (String t : tag.values()) tagText.put(t, Files.readString(TAGS.resolve(t + ".json")));
        List<String> missing = new ArrayList<>();
        for (Def d : defs()) {
            String t = tag.get(d.slot());
            if (t == null || d.adopted()) continue;
            if (!tagText.get(t).contains("\"nusmp:" + d.id() + "\"")) missing.add(d.id() + " not in " + t);
        }
        assertTrue(missing.isEmpty(), "not wearable in a Curios slot: " + missing + " (run tools/relics/gen_curios_tags.py)");
    }

    @Test
    void everyHeldWeaponHasItsMeshAndItemModel() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Def d : defs()) {
            if (d.adopted() || !d.slot().equals("MAIN_HAND")) continue;
            if (!Files.exists(ASSETS.resolve("weapon_meshes/" + d.id() + ".txt"))) missing.add(d.id() + " mesh");
            if (!Files.exists(ASSETS.resolve("models/item/" + d.id() + ".json"))) missing.add(d.id() + " item model");
        }
        assertTrue(missing.isEmpty(), "held weapon assets missing: " + missing);
    }
}
