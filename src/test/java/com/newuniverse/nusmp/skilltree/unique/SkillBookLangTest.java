package com.newuniverse.nusmp.skilltree.unique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every text the skill books show is read with {@code translatableWithFallback(key, english)}, and the lang fragment the lead merges must carry the very same words, or the merged file would show other
 * words than the game falls back to. This test reads the Java sources of the books (the unique skill, the item, the use logic, the client banner and tooltip) and checks that every key built with a literal English
 * fallback is in the fragment, or already in {@code en_us.json}, with the same text; that the fragment is flat; and that the generator's list of removed lines is the same as {@link LegacyIds}.
 */
class SkillBookLangTest {
    private static final Pattern CALL = Pattern.compile("translatableWithFallback\\(\\s*\"([^\"]+)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static JsonObject read(Path p) throws IOException {
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static List<Path> sources() throws IOException {
        List<Path> out = new ArrayList<>();
        for (String dir : new String[] {"src/main/java/com/newuniverse/nusmp/skilltree/unique", "src/main/java/com/newuniverse/nusmp/skilltree/item", "src/main/java/com/newuniverse/nusmp/client/skilltree"}) {
            try (Stream<Path> s = Files.walk(Path.of(dir))) {
                s.filter(p -> p.toString().endsWith(".java")).forEach(out::add);
            }
        }
        return out;
    }

    @Test
    void everyLiteralFallbackKeyOfTheBooksIsInTheFragmentOrTheMainFileWithTheSameWords() throws IOException {
        JsonObject fragment = read(Path.of("tools/lang/skillbooks_unique.json"));
        // 3.26.0: the other fragments of the books (skillbooks_rarity.json ...) count too; the unique one wins on a clash
        try (Stream<Path> others = Files.list(Path.of("tools/lang"))) {
            for (Path p : (Iterable<Path>) others.filter(q -> q.getFileName().toString().startsWith("skillbooks_") && q.toString().endsWith(".json")).sorted()::iterator) {
                JsonObject extra = read(p);
                for (String k : extra.keySet()) if (!fragment.has(k)) fragment.add(k, extra.get(k));
            }
        }
        JsonObject main = read(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"));
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (Path p : sources()) {
            Matcher m = CALL.matcher(Files.readString(p, StandardCharsets.UTF_8));
            while (m.find()) {
                String key = m.group(1);
                String english = JsonParser.parseString("\"" + m.group(2) + "\"").getAsString();
                checked++;
                if (fragment.has(key)) {
                    if (!fragment.get(key).getAsString().equals(english)) bad.add(p.getFileName() + ": " + key + " says '" + english + "' but the fragment says '" + fragment.get(key).getAsString() + "'");
                } else if (main.has(key)) {
                    if (!main.get(key).getAsString().equals(english)) bad.add(p.getFileName() + ": " + key + " says '" + english + "' but en_us.json says '" + main.get(key).getAsString() + "' (use a new key)");
                } else {
                    bad.add(p.getFileName() + ": " + key + " is in neither the fragment nor en_us.json");
                }
            }
        }
        assertTrue(checked > 10, "the scan found the calls: " + checked);
        assertTrue(bad.isEmpty(), String.join("\n", bad));
    }

    @Test
    void theFragmentIsFlatAndCoversTheEngineTexts() throws IOException {
        JsonObject fragment = read(Path.of("tools/lang/skillbooks_unique.json"));
        for (String k : fragment.keySet()) assertTrue(fragment.get(k).isJsonPrimitive() && fragment.get(k).getAsJsonPrimitive().isString(), "flat key -> text: " + k);
        JsonObject engine = read(Path.of("tools/lines/lang_engine.json"));
        for (String k : engine.keySet()) assertEquals(engine.get(k).getAsString(), fragment.has(k) ? fragment.get(k).getAsString() : null, "the fragment carries the engine text " + k);
    }

    @Test
    void theGeneratorListsTheSameRemovedMagicsAsTheCode() throws IOException {
        String py = Files.readString(Path.of("tools/skillbooks/gen_unique_books.py"), StandardCharsets.UTF_8);
        int start = py.indexOf("REMOVED_LINES = [");
        int end = py.indexOf("]", start);
        Matcher m = Pattern.compile("\"([a-z_]+)\"").matcher(py.substring(start, end));
        List<String> listed = new ArrayList<>();
        while (m.find()) listed.add(m.group(1));
        assertEquals(LegacyIds.REMOVED_LINES, listed);
    }
}
