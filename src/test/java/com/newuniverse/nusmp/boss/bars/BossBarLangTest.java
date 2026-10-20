package com.newuniverse.nusmp.boss.bars;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "all text through lang keys". The fragment {@code tools/lang/boss_bars.json} is flat, and every {@code nusmp.bossbar.*} key the code asks for is in it (the code also gives a fallback text,
 * but a missing key would show English in every language), including one name for each lair action the Decennial vocabulary and the D&D layer know.
 */
class BossBarLangTest {
    private static JsonObject fragment() throws IOException {
        return JsonParser.parseString(Files.readString(Path.of("tools/lang/boss_bars.json"))).getAsJsonObject();
    }

    @Test
    void theFragmentIsFlatAndHasAKeyForEveryLairAction() throws IOException {
        JsonObject f = fragment();
        for (var e : f.entrySet()) {
            assertTrue(e.getKey().startsWith("nusmp.bossbar."), e.getKey());
            assertTrue(e.getValue().isJsonPrimitive() && e.getValue().getAsString().length() > 0, e.getKey());
        }
        JsonObject lairs = JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/mega/decennial/lairs.json"))).getAsJsonObject();
        for (String id : lairs.keySet()) if (!id.startsWith("_")) assertTrue(f.has("nusmp.bossbar.lair." + id), "no name for the lair action " + id);
        for (String id : List.of("tremor", "anti_magic_field", "searing_light", "falling_stars", "necrotic_mist")) assertTrue(f.has("nusmp.bossbar.lair." + id), "no name for the D&D lair action " + id);
    }

    @Test
    void everyKeyTheCodeNamesIsInTheFragment() throws IOException {
        JsonObject f = fragment();
        Pattern key = Pattern.compile("\"(nusmp\\.bossbar\\.[a-z_.]+)\"");
        List<String> missing = new ArrayList<>();
        for (Path root : List.of(Path.of("src/main/java/com/newuniverse/nusmp/client/gui/bossbar"), Path.of("src/main/java/com/newuniverse/nusmp/boss/bars"))) {
            try (Stream<Path> s = Files.walk(root)) {
                for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) {
                    Matcher m = key.matcher(Files.readString(p));
                    while (m.find()) {
                        String k = m.group(1);
                        if (k.endsWith(".")) continue;                              // a prefix the code completes (nusmp.bossbar.lair.<id>)
                        if (!f.has(k)) missing.add(p.getFileName() + " " + k);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "keys the code names that tools/lang/boss_bars.json lacks: " + missing);
    }

    @Test
    void theTextsWithArgumentsKeepTheirPlaceholders() throws IOException {
        JsonObject f = fragment();
        assertEquals(2, f.get("nusmp.bossbar.phase_named").getAsString().split("%s", -1).length - 1);
        assertTrue(f.get("nusmp.bossbar.phase").getAsString().contains("%s"));
        assertTrue(f.get("nusmp.bossbar.adds").getAsString().contains("%s"));
        assertTrue(f.get("nusmp.bossbar.fighters").getAsString().contains("%s"));
        assertTrue(f.get("nusmp.bossbar.lair_warning").getAsString().contains("%s"));
        assertFalse(f.get("nusmp.bossbar.fighter").getAsString().contains("%s"));
    }
}
