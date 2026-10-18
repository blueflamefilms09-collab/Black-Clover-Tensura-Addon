package com.newuniverse.nusmp.elite.forge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.forge.rules.Category;
import com.newuniverse.nusmp.elite.forge.rules.MinigameType;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import com.newuniverse.nusmp.elite.forge.rules.Specialization;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the forge's texts live in a lang FRAGMENT (tools/elite/forge_lang.json: en_us.json is not edited by this subsystem), so a key used in code but missing there
 * would show as a raw key in game once the lead merges the fragment. This test scans every source file of the forge (server and client) for literal keys, adds the
 * keys built from the enums (tier, quality, category, specialization, minigame type, block and item names) and requires all of them in the fragment; it also requires
 * that no key of the fragment already exists in en_us.json (a collision with the Relic Forge's {@code nusmp.forge.*} keys or anything else), and that no fragment key is
 * dead (unused by the code and not built from an enum).
 */
class ForgeLangTest {
    static final Path FRAGMENT = Path.of("tools/elite/forge_lang.json");
    static final Path[] SOURCES = {Path.of("src/main/java/com/newuniverse/nusmp/elite/forge"), Path.of("src/main/java/com/newuniverse/nusmp/client/elite")};

    static JsonObject fragment() throws IOException { return JsonParser.parseString(Files.readString(FRAGMENT)).getAsJsonObject(); }

    static List<String> literalKeys() throws IOException {
        List<String> keys = new ArrayList<>();
        Pattern p = Pattern.compile("\"(nusmp\\.eforge\\.[a-z0-9_.]*[a-z0-9_])\"");
        for (Path root : SOURCES) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                    Matcher m = p.matcher(Files.readString(f));
                    while (m.find()) if (!keys.contains(m.group(1))) keys.add(m.group(1));
                }
            }
        }
        return keys;
    }

    static List<String> enumKeys() {
        List<String> keys = new ArrayList<>();
        for (StationTier t : StationTier.values()) {
            keys.add("nusmp.eforge.tier." + t.id());
            keys.add("block.nusmp.forge_station_" + t.id());
            if (t.prev() != null) keys.add("item.nusmp.forge_upgrade_kit_" + t.id());
        }
        for (Quality q : Quality.values()) keys.add("nusmp.eforge.quality." + q.id());
        for (Category c : Category.values()) keys.add("nusmp.eforge.cat." + c.id());
        for (Specialization s : Specialization.values()) {
            keys.add("nusmp.eforge.spec." + s.id());
            keys.add("nusmp.eforge.spec." + s.id() + ".desc");
        }
        for (MinigameType t : MinigameType.values()) keys.add("nusmp.eforge.mg.type." + t.id());
        return keys;
    }

    @Test
    void everyKeyTheCodeUsesIsInTheFragment() throws IOException {
        JsonObject frag = fragment();
        List<String> missing = new ArrayList<>();
        for (String k : literalKeys()) if (!frag.has(k)) missing.add(k);
        for (String k : enumKeys()) if (!frag.has(k)) missing.add(k);
        assertTrue(missing.isEmpty(), "keys used by the forge but missing from tools/elite/forge_lang.json: " + missing);
    }

    @Test
    void noFragmentKeyIsDeadOrCollidesWithEnUs() throws IOException {
        JsonObject frag = fragment();
        JsonObject en = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"))).getAsJsonObject();
        List<String> used = literalKeys();
        used.addAll(enumKeys());
        List<String> dead = new ArrayList<>(), clash = new ArrayList<>();
        for (String k : frag.keySet()) {
            if (!used.contains(k)) dead.add(k);
            if (en.has(k) && !en.get(k).equals(frag.get(k))) clash.add(k);   // 1.86.0: the fragment is merged into en_us.json, so a key may exist there only with the same text
        }
        assertTrue(dead.isEmpty(), "fragment keys nothing uses: " + dead);
        assertTrue(clash.isEmpty(), "fragment keys that exist in en_us.json with a different text: " + clash);
    }

    @Test
    void everyValueIsNonEmptyAndFormatsAreBalanced() throws IOException {
        for (var e : fragment().entrySet()) {
            String v = e.getValue().getAsString();
            assertTrue(!v.isBlank(), e.getKey() + " is empty");
            int pct = 0;
            for (int i = 0; i < v.length(); i++) {
                if (v.charAt(i) == '%') {
                    char n = i + 1 < v.length() ? v.charAt(i + 1) : ' ';
                    assertTrue(n == 's' || n == '%' || n == 'd', e.getKey() + " has a bad format specifier");
                    pct++;
                    i++;
                }
            }
            assertTrue(pct <= 4, e.getKey() + " has too many arguments");
        }
    }
}
