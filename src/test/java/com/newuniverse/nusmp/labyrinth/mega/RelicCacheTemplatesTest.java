package com.newuniverse.nusmp.labyrinth.mega;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: relics can be found in the hidden (treasure) rooms of ordinary floors (1.75.0). Two things must stay true or the owner's one-capped-relic-per-region rule breaks: every chest of a
 * treasure room rolls the hidden cache table (tools/labyrinth/retarget_treasure_loot.py; the generators still name the old table, so regenerating a room silently undoes it), and the relics
 * that table can roll are of Rare grade or lower (the grades the God of Order's ledger does not cap). This reads the templates and the table straight from the resources.
 */
class RelicCacheTemplatesTest {
    private static final Path RES = Path.of("src", "main", "resources", "data", "nusmp");

    @Test
    void everyTreasureRoomChestRollsTheHiddenCache() throws IOException {
        List<String> bad = new ArrayList<>();
        int seen = 0;
        try (Stream<Path> s = Files.list(RES.resolve("structure/mega/treasure"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".nbt"))::iterator) {
                String raw;
                try (InputStream in = new GZIPInputStream(Files.newInputStream(p))) {
                    raw = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
                }
                seen++;
                if (raw.contains("nusmp:chests/labyrinth_trial")) bad.add(p.getFileName() + " still rolls the plain trial table");
                if (!raw.contains("nusmp:chests/hidden_cache") && !raw.contains("nusmp:chests/library_ruins")) bad.add(p.getFileName() + " has no hidden cache chest");
            }
        }
        assertTrue(seen >= 14, "the treasure rooms are found: " + seen);
        assertTrue(bad.isEmpty(), String.join("\n", bad) + "\n(run python3 tools/labyrinth/retarget_treasure_loot.py)");
    }

    @Test
    void theHiddenCacheRollsOnlyRelicsThatAreNotCapped() throws IOException {
        String json = Files.readString(RES.resolve("loot_table/chests/hidden_cache.json"));
        Matcher m = Pattern.compile("\"max_grade\"\\s*:\\s*\"(\\w+)\"").matcher(json);
        int n = 0;
        while (m.find()) {
            n++;
            String g = m.group(1);
            assertTrue(g.equals("normal") || g.equals("special") || g.equals("rare"), "the hidden cache may roll up to Rare, not " + g);
        }
        assertTrue(n >= 1, "the hidden cache has a relic entry");
        assertTrue(json.contains("nusmp:chests/labyrinth_trial"), "it keeps the trial loot");
    }
}
