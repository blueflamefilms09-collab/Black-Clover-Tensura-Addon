package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the codemod ({@code tools/particles/codemod.py}) is driven by {@code tools/particles/map.json} and writes two generated Java files from it ({@code FxOld} and {@code FxThemes}).
 * If someone edits the map and forgets to run the codemod, or edits a generated file by hand, the build still compiles but the rewritten call sites mean something else than the map
 * says. This test reads the three files and checks that every vanilla particle of the map is an entry of {@code FxOld} with the slot, fixed particle and colour the map gives, that no entry
 * exists without the map, that every theme of the map has its {@code FxThemes} constant, and that the fixed particles are real library particles.
 */
class FxCodemodMapTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static JsonObject map() throws IOException {
        return JsonParser.parseString(Files.readString(ROOT.resolve("tools/particles/map.json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String read(String rel) throws IOException {
        return Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/" + rel), StandardCharsets.UTF_8);
    }

    @Test
    void everyVanillaParticleOfTheMapIsAnEntryOfTheGeneratedEnumAndNothingElse() throws IOException {
        String old = read("particle/fx/FxOld.java");
        Set<String> names = new HashSet<>();
        for (Map.Entry<String, JsonElement> e : map().getAsJsonObject("particles").entrySet()) {
            if (e.getKey().equals("comment")) continue;
            names.add(e.getKey());
            JsonObject v = e.getValue().getAsJsonObject();
            String slot = v.has("slot") ? v.get("slot").getAsString() : "MOTE";
            FxSlot.valueOf(slot);
            String fixed = v.has("fixed") ? "\"" + v.get("fixed").getAsString() + "\"" : "null";
            if (v.has("fixed")) assertTrue(FxCatalog.has(v.get("fixed").getAsString()), e.getKey() + " maps to the unknown particle " + v.get("fixed").getAsString());
            int rgb = v.has("rgb") ? Integer.decode(v.get("rgb").getAsString()) : -1;
            String themed = v.has("themed") && v.get("themed").getAsBoolean() ? "true" : "false";
            String line = "    " + e.getKey() + "(FxSlot." + slot + ", " + fixed + ", " + rgb + ", " + themed + ")";
            assertTrue(old.contains(line), "FxOld.java is out of date for " + e.getKey() + " (run tools/particles/codemod.py --gen-only); wanted: " + line.trim());
            assertTrue(old.contains("case " + e.getKey() + " -> ParticleTypes." + e.getKey() + ";"), e.getKey() + " has no vanilla fallback");
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^    ([A-Z_]+)\\(FxSlot\\.", java.util.regex.Pattern.MULTILINE).matcher(old);
        while (m.find()) assertTrue(names.contains(m.group(1)), "FxOld has an entry the map does not: " + m.group(1));
        assertTrue(names.contains("END_ROD") && names.contains("REVERSE_PORTAL") && names.contains("ENCHANT"), "the most used vanilla particles must be mapped");
    }

    @Test
    void everyThemeOfTheMapHasItsConstant() throws IOException {
        String themes = read("particle/fx/FxThemes.java");
        Set<String> ids = new HashSet<>();
        map().getAsJsonArray("themes").forEach(e -> ids.add(e.getAsJsonArray().get(1).getAsString()));
        ids.add(map().get("default_theme").getAsString());
        for (String id : ids) {
            String c = id.replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
            assertTrue(themes.contains("ThemeKey " + c + " = ThemeKey.of(\"" + id + "\");"), "FxThemes.java is out of date for " + id + " (run tools/particles/codemod.py --gen-only)");
        }
    }

    @Test
    void theSkipListKeepsTheTimeMagicEffectsAndTheLibrariesOut() throws IOException {
        String skip = map().getAsJsonObject("skip").getAsJsonArray("globs").toString();
        for (String must : new String[]{"book/TimeBook.java", "vfx/client/layer/TimeMagicLayer.java", "particle/rukh/**", "client/particle/rukh/**", "particle/fx/**", "client/particle/fx/**"})
            assertTrue(skip.contains("\"" + must + "\""), "the codemod must skip " + must);
        String keep = map().getAsJsonObject("rukh_keep").getAsJsonArray("globs").toString();
        for (String must : new String[]{"relics/djinn/**", "client/relics/djinn/**", "labyrinth/**", "dungeon/**"})
            assertTrue(keep.contains("\"" + must + "\""), "Rukh stays in " + must);
    }
}
