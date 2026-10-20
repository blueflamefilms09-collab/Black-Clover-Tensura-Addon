package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every sentence of the {@code /dungeon} admin tree is a translation key whose English text is kept in {@link AdminText} (the fallback the game shows before a language file
 * has the key) and in {@code tools/lang/dungeon_admin.json} (the flat fragment the owner merges into the game's lang file; {@code en_us.json} is not edited). If the two differ, a
 * player sees one text in a fresh checkout and another after the merge, so this pins them identical, key for key, word for word and in the same order. It also pins that the
 * placeholders are numbered from 1 without gaps (a translation may reorder them), that no key already exists in the main lang file, that every key the code uses is in the map,
 * and that every key in the map is used.
 */
public class AdminTextTest {
    private static final Path FRAGMENT = Path.of("tools", "lang", "dungeon_admin.json");
    private static final Path MAIN = Path.of("src", "main", "resources", "assets", "nusmp", "lang", "en_us.json");
    private static final Path SOURCES = Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "labyrinth", "mega", "admin");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+)\\$s");

    private static JsonObject read(Path p) throws IOException { return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject(); }

    @Test
    public void theFragmentSaysExactlyWhatTheCodeSaysInTheSameOrder() throws IOException {
        JsonObject file = read(FRAGMENT);
        Map<String, String> code = AdminText.all();
        assertEquals(code.size(), file.size(), "same number of keys");
        List<String> fileKeys = new ArrayList<>(file.keySet());
        assertEquals(new ArrayList<>(code.keySet()), fileKeys, "same keys in the same order, so a diff is readable");
        for (Map.Entry<String, String> e : code.entrySet()) assertEquals(e.getValue(), file.get(e.getKey()).getAsString(), e.getKey());
    }

    @Test
    public void everyKeyHasThePrefixAndATextAndIsNotInTheMainLangFile() throws IOException {
        JsonObject main = read(MAIN);
        for (Map.Entry<String, String> e : AdminText.all().entrySet()) {
            assertTrue(e.getKey().startsWith(AdminText.PREFIX), e.getKey());
            assertFalse(e.getValue().isBlank(), e.getKey());
            assertFalse(main.has(e.getKey()), "already in en_us.json: " + e.getKey());
            assertEquals(e.getValue(), AdminText.english(e.getKey()));
            assertEquals(e.getValue(), AdminText.englishOrNull(e.getKey()));
        }
        assertNull(AdminText.englishOrNull("message.nusmp.dungeon.admin.no_such_key"));
        assertEquals("some.other.key", AdminText.english("some.other.key"), "an unknown key reads as itself, never null");
    }

    @Test
    public void placeholdersAreNumberedFromOneWithoutGaps() {
        for (Map.Entry<String, String> e : AdminText.all().entrySet()) {
            String v = e.getValue();
            assertFalse(v.contains("%s") || v.contains("%d"), "an unnumbered placeholder in " + e.getKey() + ": " + v);
            Matcher m = PLACEHOLDER.matcher(v);
            int max = 0;
            java.util.Set<Integer> seen = new java.util.TreeSet<>();
            while (m.find()) {
                int n = Integer.parseInt(m.group(1));
                max = Math.max(max, n);
                seen.add(n);
            }
            for (int i = 1; i <= max; i++) assertTrue(seen.contains(i), "placeholder " + i + " is missing in " + e.getKey() + ": " + v);
            String bare = v.replace("%%", "").replaceAll("%\\d+\\$s", "");
            assertFalse(bare.contains("%"), "a stray percent sign in " + e.getKey() + ": " + v);
        }
    }

    @Test
    public void everyKeyTheCodeUsesIsInTheMapAndEveryKeyInTheMapIsUsed() throws IOException {
        List<String> constants = new ArrayList<>();
        Pattern decl = Pattern.compile("public static final String ([A-Z0-9_]+) = put\\(");
        try (Stream<Path> files = Files.list(SOURCES)) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                if (p.getFileName().toString().equals("AdminText.java")) {
                    Matcher m = decl.matcher(Files.readString(p, StandardCharsets.UTF_8));
                    while (m.find()) constants.add(m.group(1));
                }
            }
        }
        assertEquals(AdminText.all().size(), constants.size(), "one constant per key");
        StringBuilder all = new StringBuilder();
        try (Stream<Path> files = Files.list(SOURCES)) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java") && !f.getFileName().toString().equals("AdminText.java"))::iterator) all.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n');
        }
        String code = all.toString();
        for (String c : constants) {
            assertTrue(code.contains("AdminText." + c), "the key AdminText." + c + " is not used by any class of the admin tree");
        }
        Matcher used = Pattern.compile("AdminText\\.([A-Z0-9_]+)").matcher(code);
        while (used.find()) {
            String c = used.group(1);
            if (c.equals("PREFIX")) continue;
            assertTrue(constants.contains(c), "AdminText." + c + " is used but does not exist");
        }
    }
}
