package com.newuniverse.nusmp.skilltree.menu;

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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the new strings live in {@code MenuText} (read with a fallback, so a missing language entry never shows a raw key) and in {@code tools/tensura_menu/lang_tensura_menu.json} (what the lead merges into
 * the language file later, because this job may not edit {@code en_us.json}). If the two drift, the merged file would say something else than the game falls back to. This test compares them in both directions
 * by reading the sources (no Minecraft needed), checks the file is flat and complete (one name per line for the stand-in skills), that every string of the code is actually used, that the one place that
 * spells the ON/OFF keys by hand agrees with {@code MenuText}, and that the new code builds no translatable without a fallback.
 */
class MenuTextTest {
    private static final Path DIR = Path.of("src/main/java/com/newuniverse/nusmp/skilltree/menu");

    private static Map<String, String> codeStrings() throws IOException {
        String src = Files.readString(DIR.resolve("MenuText.java"), StandardCharsets.UTF_8);
        Map<String, String> code = new LinkedHashMap<>();
        Matcher m = Pattern.compile("def\\(\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\)").matcher(src);
        while (m.find()) code.put(m.group(1), JsonParser.parseString("\"" + m.group(2) + "\"").getAsString());
        return code;
    }

    @Test
    void theLanguageFragmentAndTheCodeAgreeInBothDirections() throws IOException {
        Map<String, String> code = codeStrings();
        assertFalse(code.isEmpty());
        JsonObject file = JsonParser.parseString(Files.readString(Path.of("tools/tensura_menu/lang_tensura_menu.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        for (Map.Entry<String, String> e : code.entrySet()) {
            assertTrue(file.has(e.getKey()), "lang_tensura_menu.json lacks " + e.getKey() + " (run tools/tensura_menu/gen_lang.py)");
            assertEquals(e.getValue(), file.get(e.getKey()).getAsString(), e.getKey());
        }
        int names = 0;
        for (String k : file.keySet()) {
            assertTrue(file.get(k).isJsonPrimitive() && file.get(k).getAsJsonPrimitive().isString(), "the file is flat key -> text: " + k);
            if (k.startsWith("nusmp.skill.line_")) names++;
            else assertTrue(code.containsKey(k), "lang_tensura_menu.json has " + k + ", which the code does not use");
        }
        for (LineIndex.Line l : LineIndex.load()) {
            String key = "nusmp.skill.line_" + l.bare();            // the shape MenuText.standInNameKey builds (checked below by reading its source)
            assertTrue(file.has(key), "no name for the stand-in " + l.bare());
            assertEquals(l.name(), file.get(key).getAsString(), key);
        }
        assertEquals(LineIndex.load().size(), names, "one name per stand-in and nothing else under nusmp.skill.line_");
        String text = Files.readString(DIR.resolve("MenuText.java"), StandardCharsets.UTF_8);
        assertTrue(text.contains("return \"nusmp.skill.\" + LineIndex.skillPath(bare);"), "standInNameKey builds nusmp.skill.line_<bare>");
        assertEquals("line_x", LineIndex.skillPath("x"));
    }

    @Test
    void everyStringOfTheCodeIsUsedAndTheHandSpelledKeysAgree() throws IOException {
        String text = Files.readString(DIR.resolve("MenuText.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("public static final String (\\w+) = def\\(\"([^\"]+)\"").matcher(text);
        StringBuilder everythingElse = new StringBuilder();
        try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/newuniverse/nusmp"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java") && !f.getFileName().toString().equals("MenuText.java")).toList()) {
                String t = Files.readString(p, StandardCharsets.UTF_8);
                if (t.contains("MenuText") || t.contains("gui.nusmp.tmenu.")) everythingElse.append(t);
            }
        }
        String used = everythingElse.toString();
        String checkbox = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/skilltree/layout/MenuCheckbox.java"), StandardCharsets.UTF_8);
        Map<String, String> keysByConstant = new LinkedHashMap<>();
        while (m.find()) keysByConstant.put(m.group(1), m.group(2));
        assertFalse(keysByConstant.isEmpty());
        for (Map.Entry<String, String> e : keysByConstant.entrySet())
            assertTrue(used.contains("MenuText." + e.getKey()) || used.contains("\"" + e.getValue() + "\""), "MenuText." + e.getKey() + " is declared but never used");
        assertEquals("gui.nusmp.tmenu.on", keysByConstant.get("ON"));
        assertEquals("gui.nusmp.tmenu.off", keysByConstant.get("OFF"));
        assertTrue(checkbox.contains("\"" + keysByConstant.get("ON") + "\"") && checkbox.contains("\"" + keysByConstant.get("OFF") + "\""), "MenuCheckbox spells the ON and OFF keys of MenuText");
    }

    @Test
    void theNewCodeBuildsNoTranslatableWithoutAFallbackAndLeavesTheMainLangFileAlone() throws IOException {
        try (Stream<Path> s = Files.list(DIR)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList())
                assertFalse(Files.readString(p, StandardCharsets.UTF_8).contains("Component.translatable(\""), p + " builds a translatable from a literal key without a fallback");
        }
        for (String n : new String[]{"TensuraMenuPanel", "TensuraMenuClient", "TensuraCheckbox", "TensuraScreenAccess"})
            assertFalse(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/skilltree/" + n + ".java"), StandardCharsets.UTF_8).contains("Component.translatable(\""), n);
        String lang = Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"), StandardCharsets.UTF_8);
        assertTrue(lang.contains("gui.nusmp.tmenu."), "1.92.0: the lead merged tools/tensura_menu/lang_tensura_menu.json into en_us.json");
    }
}
