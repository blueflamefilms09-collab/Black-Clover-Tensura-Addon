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
 * Why (3.26.0, the Evolution skills): the words the Evolution feature says live in {@code EvolutionText} (read with an English fallback, so a missing language entry never shows a raw key) and in
 * {@code tools/lang/evolution.json} (the flat file the lead merges into the language file; a builder never edits {@code en_us.json}). If the two drift the merged file would say something other than the
 * game falls back to, or lack a key the code asks for. This reads both sources (no Minecraft needed) and compares them in both directions, checks the file is flat, and that every format string takes
 * the number of arguments the code passes (a {@code %s} too many shows as a raw "%s" on screen).
 */
class EvolutionTextTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp/skilltree/line/EvolutionText.java");
    private static final Path FILE = Path.of("tools/lang/evolution.json");

    private static Map<String, String> code() throws IOException {
        String src = Files.readString(SRC, StandardCharsets.UTF_8);
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = Pattern.compile("def\\(\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\)").matcher(src);
        while (m.find()) out.put(m.group(1), JsonParser.parseString("\"" + m.group(2) + "\"").getAsString());
        return out;
    }

    @Test
    void theLanguageFragmentAndTheCodeAgreeInBothDirections() throws IOException {
        Map<String, String> code = code();
        JsonObject file = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(code.isEmpty());
        assertEquals(code.size(), file.size(), "the same number of keys");
        for (Map.Entry<String, String> e : code.entrySet()) {
            assertTrue(file.has(e.getKey()), "tools/lang/evolution.json lacks " + e.getKey());
            assertEquals(e.getValue(), file.get(e.getKey()).getAsString(), e.getKey());
        }
        for (String k : file.keySet()) {
            assertTrue(code.containsKey(k), "evolution.json has " + k + ", which the code does not use");
            assertTrue(file.get(k).isJsonPrimitive() && file.get(k).getAsJsonPrimitive().isString(), "the file is flat key -> text: " + k);
        }
    }

    @Test
    void theCompiledTableIsTheSameAsTheSource() throws IOException {
        assertEquals(code(), EvolutionText.all(), "EvolutionText.all() is the table the file is checked against");
        assertEquals("%s is locked: %s (%s).", EvolutionText.english(EvolutionText.LOCKED));
        assertEquals("nope", EvolutionText.english("nope"), "an unknown key answers itself, never null");
    }

    @Test
    void everyFormatTakesTheArgumentsTheCodePasses() {
        assertEquals(3, args(EvolutionText.LOCKED), "art, requirement, progress");
        assertEquals(1, args(EvolutionText.NEEDS_MASTERY));
        assertEquals(2, args(EvolutionText.EVOLVED), "skill, evolved name");
        assertEquals(2, args(EvolutionText.MODE_PROGRESS), "art, progress");
        assertEquals(1, args(EvolutionText.MODE_NEEDS_MASTERY));
        assertEquals(1, args(EvolutionText.REQUIREMENT));
        assertEquals(1, args(EvolutionText.EVOLVED_NAME));
    }

    private static int args(String key) {
        Matcher m = Pattern.compile("%s").matcher(EvolutionText.english(key));
        int n = 0;
        while (m.find()) n++;
        return n;
    }
}
