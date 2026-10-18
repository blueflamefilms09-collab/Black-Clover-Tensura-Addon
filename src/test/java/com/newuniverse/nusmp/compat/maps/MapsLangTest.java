package com.newuniverse.nusmp.compat.maps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** The lang fragment tools/compat/maps_lang.json: it covers every message key the map code uses, says what the code's fallback says, and adds no key en_us.json already has. */
class MapsLangTest {
    private static final Path FRAGMENT = Paths.get("tools/compat/maps_lang.json"), EN_US = Paths.get("src/main/resources/assets/nusmp/lang/en_us.json");
    private static final Path DIR = Paths.get("src/main/java/com/newuniverse/nusmp/compat/maps");

    private static JsonObject read(Path p) throws IOException { return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject(); }

    private static String sources() throws IOException {
        StringBuilder b = new StringBuilder();
        try (var s = Files.list(DIR)) { for (Path p : s.filter(x -> x.toString().endsWith(".java")).sorted().toList()) b.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n'); }
        return b.toString();
    }

    @Test
    void everySimpleFallbackMatchesTheFragment() throws IOException {
        JsonObject frag = read(FRAGMENT);
        String src = sources();
        List<String> bad = new ArrayList<>();
        int seen = 0;
        Matcher full = Pattern.compile("translatableWithFallback\\(\\s*\"(message\\.nusmp\\.maps\\.[a-z_.]+)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(src);
        while (full.find()) { seen++; check(frag, full.group(1), full.group(2), bad); }
        Matcher t = Pattern.compile("\\bt\\(\\s*\"([a-z_.]+)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(src);
        while (t.find()) { seen++; check(frag, "message.nusmp.maps." + t.group(1), t.group(2), bad); }
        assertTrue(seen >= 20, "the scan should find the messages, found " + seen);
        assertTrue(bad.isEmpty(), "lang fragment out of step with the code: " + bad);
    }

    private static void check(JsonObject frag, String key, String fallback, List<String> bad) {
        String code = fallback.replace("\\\"", "\"").replace("\\'", "'");
        if (!frag.has(key)) bad.add("missing key " + key);
        else if (!frag.get(key).getAsString().equals(code)) bad.add(key + ": fragment '" + frag.get(key).getAsString() + "' vs code '" + code + "'");
    }

    @Test
    void theTernaryShareMessagesAreInTheFragment() throws IOException {
        JsonObject frag = read(FRAGMENT);
        for (String k : List.of("share.on", "share.off")) assertTrue(frag.has("message.nusmp.maps." + k), k);
    }

    @Test
    void theFragmentNeverContradictsEnUs() throws IOException {
        JsonObject en = read(EN_US), frag = read(FRAGMENT);
        List<String> dup = new ArrayList<>();
        for (String k : frag.keySet()) if (!k.startsWith("_") && en.has(k) && !en.get(k).getAsString().equals(frag.get(k).getAsString())) dup.add(k);   // same text = already merged, fine
        assertTrue(dup.isEmpty(), "keys that en_us.json already has with a different text (merge them by hand): " + dup);
    }

    @Test
    void everyBiomeAndDimensionOfTheModHasAName() throws IOException {
        JsonObject en = read(EN_US), frag = read(FRAGMENT);
        List<String> missing = new ArrayList<>();
        try (var s = Files.list(Paths.get("src/main/resources/data/nusmp/worldgen/biome"))) {
            for (Path p : s.toList()) { String k = "biome.nusmp." + p.getFileName().toString().replace(".json", ""); if (!en.has(k) && !frag.has(k)) missing.add(k); }
        }
        for (String n : DimensionAudit.NAMES) { String k = "dimension.nusmp." + n; if (!en.has(k) && !frag.has(k)) missing.add(k); }
        assertTrue(missing.isEmpty(), "a map mod would show these raw: " + missing);
    }
}
