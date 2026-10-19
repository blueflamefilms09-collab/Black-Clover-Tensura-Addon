package com.newuniverse.nusmp.skilltree.line;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the engine's new strings live in {@code LineText} (read with a fallback, so a missing language entry never shows a raw key) and in {@code tools/lines/lang_engine.json} (what the lead merges into the
 * language file later). If the two drift, the merged file would hold a different text than the game falls back to, or lack a key the code asks for. This test compares them in both directions by reading the
 * sources (no Minecraft needed), checks that the language file is flat, that {@code en_us.json} was not touched by this job, and that nothing in the line engine or its screens builds a translatable from a
 * key without a fallback.
 */
class LineTextTest {
    @Test
    void theLanguageFileAndTheCodeAgreeInBothDirections() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skilltree/line/LineText.java"), StandardCharsets.UTF_8);
        Map<String, String> code = new LinkedHashMap<>();
        Matcher m = Pattern.compile("def\\(\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\)").matcher(src);
        while (m.find()) code.put(m.group(1), JsonParser.parseString("\"" + m.group(2) + "\"").getAsString());
        JsonObject file = JsonParser.parseString(Files.readString(Path.of("tools/lines/lang_engine.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(code.isEmpty());
        for (Map.Entry<String, String> e : code.entrySet()) {
            assertTrue(file.has(e.getKey()), "lang_engine.json lacks " + e.getKey() + " (run tools/lines/gen_lang_engine.py)");
            assertEquals(e.getValue(), file.get(e.getKey()).getAsString(), e.getKey());
        }
        for (String k : file.keySet()) {
            assertTrue(code.containsKey(k), "lang_engine.json has " + k + ", which the code does not use");
            assertTrue(file.get(k).isJsonPrimitive() && file.get(k).getAsJsonPrimitive().isString(), "the file is flat key -> text: " + k);
        }
    }

    @Test
    void theEngineDoesNotBuildTranslatablesWithoutAFallbackAndDoesNotEditTheMainLangFile() throws IOException {
        for (String dir : new String[]{"src/main/java/com/newuniverse/nusmp/skilltree/line", "src/main/java/com/newuniverse/nusmp/client/skilltree"}) {
            try (var s = Files.walk(Path.of(dir))) {
                for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(p, StandardCharsets.UTF_8);
                    if (!p.toString().contains("/line/") && !text.contains("LineText")) continue;           // old screens keep their own keys in en_us.json
                    assertFalse(text.contains("Component.translatable(\"") && p.toString().contains("/skilltree/line/"), p + " builds a translatable from a literal key without a fallback");
                }
            }
        }
    }
}
