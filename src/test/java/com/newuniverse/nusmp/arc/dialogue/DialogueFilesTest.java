package com.newuniverse.nusmp.arc.dialogue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Every dialogue file the mod ships parses with no problem, belongs to a snake_case id, and its language keys follow the numbering the generator uses. */
class DialogueFilesTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/arc/dialogue");

    private static List<Path> files() throws IOException {
        assertTrue(Files.isDirectory(DIR), "the dialogue folder exists: " + DIR.toAbsolutePath());
        try (Stream<Path> s = Files.list(DIR)) {
            List<Path> out = new ArrayList<>(s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList());
            return out;
        }
    }

    private static String stem(Path p) {
        String n = p.getFileName().toString();
        return n.substring(0, n.length() - 5);
    }

    @Test
    void theFallbackExistsAndEveryFileParsesCleanly() throws IOException {
        Set<String> stems = new HashSet<>();
        for (Path p : files()) {
            String id = stem(p);
            stems.add(id);
            assertTrue(id.equals(DialogueBook.FALLBACK_ID) || id.matches("[a-z][a-z0-9_]*"), "a character id: " + id);
            DialogueBook.Parsed parsed = DialogueBook.parse(id, Files.readString(p, StandardCharsets.UTF_8));
            assertTrue(parsed.ok(), id + ": " + parsed.problems());
            assertTrue(parsed.problems().isEmpty(), id + ": " + parsed.problems());
        }
        assertTrue(stems.contains(DialogueBook.FALLBACK_ID), "the generic file every character without its own says");
    }

    @Test
    void theFallbackCanGreetHintAndFarewellAStranger() throws IOException {
        DialogueBook b = DialogueBook.parse(DialogueBook.FALLBACK_ID, Files.readString(DIR.resolve(DialogueBook.FALLBACK_ID + ".json"), StandardCharsets.UTF_8)).book();
        DialogueSituation newcomer = DialogueSituation.of(0, "", List.of(), 0, true);
        DialogueSituation later = DialogueSituation.of(7, "spade_raid", List.of("heath_grice"), 0, false);
        for (DialogueSituation s : new DialogueSituation[]{newcomer, later}) {
            assertTrue(b.has(DialogueContext.GREETING, s));
            assertTrue(b.has(DialogueContext.HINT, s));
        }
        assertNotNull(TalkPlan.next(true, 0, c -> b.has(c, newcomer)));
        assertNotNull(TalkPlan.next(false, 0, c -> b.has(c, later)));
    }

    @Test
    void keysFollowTheGeneratorsNumbering() throws IOException {
        for (Path p : files()) {
            String id = stem(p);
            DialogueBook b = DialogueBook.parse(id, Files.readString(p, StandardCharsets.UTF_8)).book();
            Set<String> seen = new HashSet<>();
            List<DialogueLine> all = b.all();
            for (int i = 0; i < all.size(); i++) {
                String key = all.get(i).key();
                if (key.isEmpty()) continue;
                assertEquals("arc.dialogue." + id + "." + (i + 1), key, id + " line " + (i + 1));
                assertTrue(seen.add(key), id + ": " + key + " twice");
            }
        }
    }
}
