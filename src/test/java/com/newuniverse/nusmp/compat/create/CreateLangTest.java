package com.newuniverse.nusmp.compat.create;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the lang fragment tools/lang/create_compat.json (merged into en_us.json by the lead; this builder does not edit that file) must cover every message the Create code can
 * show, say what the code's fallback says, and be flat. Also keeps the package honest about its one hard rule: no source file of it imports a Create class.
 */
class CreateLangTest {
    private static final Path FRAGMENT = Paths.get("tools/lang/create_compat.json"), EN_US = Paths.get("src/main/resources/assets/nusmp/lang/en_us.json");
    private static final Path DIR = Paths.get("src/main/java/com/newuniverse/nusmp/compat/create");

    private static JsonObject read(Path p) throws IOException { return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject(); }

    private static String sources() throws IOException {
        StringBuilder b = new StringBuilder();
        try (var s = Files.list(DIR)) { for (Path p : s.filter(x -> x.toString().endsWith(".java")).sorted().toList()) b.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n'); }
        return b.toString();
    }

    @Test
    void everyFallbackMatchesTheFragment() throws IOException {
        JsonObject frag = read(FRAGMENT);
        String src = sources();
        List<String> bad = new ArrayList<>();
        int seen = 0;
        Matcher full = Pattern.compile("translatableWithFallback\\(\\s*\"(message\\.nusmp\\.create\\.[a-z_.]+)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(src);
        while (full.find()) { seen++; check(frag, full.group(1), full.group(2), bad); }
        Matcher t = Pattern.compile("\\bt\\(\\s*\"([a-z_.]+)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(src);
        while (t.find()) { seen++; check(frag, "message.nusmp.create." + t.group(1), t.group(2), bad); }
        assertTrue(seen >= 14, "the scan should find the messages, found " + seen);
        assertTrue(bad.isEmpty(), "lang fragment out of step with the code: " + bad);
    }

    private static void check(JsonObject frag, String key, String fallback, List<String> bad) {
        String code = fallback.replace("\\\"", "\"").replace("\\'", "'");
        if (!frag.has(key)) bad.add("missing key " + key);
        else if (!frag.get(key).getAsString().equals(code)) bad.add(key + ": fragment '" + frag.get(key).getAsString() + "' vs code '" + code + "'");
    }

    @Test
    void theFragmentIsFlatAndNeverContradictsEnUs() throws IOException {
        JsonObject en = read(EN_US), frag = read(FRAGMENT);
        for (var e : frag.entrySet()) {
            assertTrue(e.getValue().isJsonPrimitive(), e.getKey() + " is not a plain string");
            assertTrue(e.getKey().startsWith("message.nusmp.create."), e.getKey());
            if (en.has(e.getKey())) assertTrue(en.get(e.getKey()).getAsString().equals(e.getValue().getAsString()), e.getKey() + " differs from en_us.json");
        }
    }

    @Test
    void noSourceFileImportsACreateClass() throws IOException {
        String src = sources();
        assertFalse(Pattern.compile("(?m)^import\\s+com\\.simibubi\\.").matcher(src).find(), "the compatibility must not compile against Create");
        // the only places that name a Create class are strings handed to reflection, and comments
        for (String line : src.split("\n")) {
            if (!line.contains("com.simibubi")) continue;
            String t = line.trim();
            assertTrue(t.startsWith("*") || t.startsWith("//") || t.startsWith("/**") || t.contains("\"com.simibubi"), "a Create class name outside a string or a comment: " + t);
        }
    }
}
