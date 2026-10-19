package com.newuniverse.nusmp.skilltree.unique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner asked for two things that only the shipped files can prove: every Fairy Tail magic is GONE from the skill book system (no data file, no loot or shop entry, no line of the shipped table, no
 * fragment of the lang), and every book that remains has a real skill behind it (a route file with data, a skill id in the table, a name and a description in the lang fragment, an icon). The scan looks for
 * the removed ids as whole words, so an id that merely contains another ({@code water_body} inside a longer word) is not flagged, and it reads the data pack, the shipped table, the lang fragment and the loot
 * and shop data. The one exemption is the Java class {@link LegacyIds}, which MUST name them (that is how old saves keep loading). {@code en_us.json} is not scanned: the lead removes its stale keys when merging
 * the fragment (the list is {@code tools/lang/skillbooks_unique.remove.txt}); the game never asks for them.
 */
class UniqueBooksFilesTest {
    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path TREES = DATA.resolve("nusmp/skill_trees");
    private static final Path TABLE = Path.of("src/main/resources/assets/nusmp/skill_books.json");
    private static final Path FRAGMENT = Path.of("tools/lang/skillbooks_unique.json");
    private static final Path ICONS = Path.of("src/main/resources/assets/nusmp/textures/skill/line");

    private static List<Path> jsonUnder(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".json")).toList();
        }
    }

    /**
     * The ids of the removed magics as they are written when they are ids ({@code nusmp:<line>}, its book, its skill path, the technique ids). A bare word such as {@code gravity_change} is also the name
     * of an unrelated dungeon mechanic, so the bare form is only looked for in the skill book folders ({@link #removedTokens}).
     */
    private static List<String> removedIds() {
        List<String> out = new ArrayList<>();
        for (String r : LegacyIds.REMOVED_LINES) {
            out.add("nusmp:" + r);
            out.add("nusmp:" + r + "_book");
            out.add(LineIndex.skillPath(r));
        }
        out.addAll(LegacyIds.REMOVED_TECHNIQUES);
        return out;
    }

    /** The ids and the franchise words; for the folders that belong to the skill books only (other data, such as a boss's credits, may name the series). */
    private static List<String> removedTokens() {
        List<String> out = removedIds();
        out.addAll(LegacyIds.REMOVED_LINES);
        out.add("fairy_tail");
        out.add("Fairy Tail");
        return out;
    }

    private static List<String> hits(Path file, List<String> tokens) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        List<String> out = new ArrayList<>();
        for (String t : tokens) {
            if (Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(t) + "(?![A-Za-z0-9_])").matcher(text).find()) out.add(t);
        }
        return out;
    }

    @Test
    void noRemovedIdIsLeftAnywhereInTheData() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : jsonUnder(DATA)) {
            List<String> h = hits(p, removedIds());
            if (!h.isEmpty()) bad.add(DATA.relativize(p) + " names " + h);
        }
        assertTrue(bad.isEmpty(), "removed magics still named in the data: " + bad);
    }

    @Test
    void noFairyTailWordIsLeftInTheSkillBookDataTheShopOrTheLoot() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String dir : new String[] {"nusmp/skill_trees", "nusmp/black_market", "nusmp/loot_modifiers", "neoforge/loot_modifiers", "nusmp/loot_table"}) {
            Path root = DATA.resolve(dir);
            if (!Files.exists(root)) continue;
            for (Path p : jsonUnder(root)) {
                if (p.getFileName().toString().equals("fairy_tail_skill_book.json")) continue;   // 1.95.0: the loot of the standalone Fairy Tail skills (fairytail package), not of the removed skill-book lines
                List<String> h = hits(p, removedTokens());
                if (!h.isEmpty()) bad.add(DATA.relativize(p) + " names " + h);
            }
        }
        assertTrue(bad.isEmpty(), "Fairy Tail still named in the skill book data: " + bad);
    }

    @Test
    void noFairyTailIdIsLeftInTheShippedTableOrTheLangFragment() throws IOException {
        for (Path p : List.of(TABLE, FRAGMENT)) {
            List<String> h = hits(p, removedTokens());
            assertTrue(h.isEmpty(), p + " names the removed magics " + h);
        }
    }

    @Test
    void noRemovedMagicHasAnIconOrABookCoverTexture() {
        for (String r : LegacyIds.REMOVED_LINES) {
            assertTrue(!Files.exists(ICONS.resolve(r + ".png")), "the icon of the removed magic " + r + " is still shipped");
            assertTrue(!Files.exists(TREES.resolve(r + ".json")), "the data of the removed magic " + r + " is still shipped");
        }
        assertTrue(!Files.exists(Path.of("src/main/resources/assets/nusmp/textures/item/skill_book_3d/fairy_tail.png")), "the Fairy Tail cover is still shipped");
        assertTrue(!Files.exists(Path.of("src/main/resources/assets/nusmp/models/item/skill_book_3d_fairy_tail.json")));
        assertTrue(!Files.exists(Path.of("src/main/resources/assets/nusmp/textures/skill/ft")), "the technique icons of the removed classic routes are still shipped");
    }

    @Test
    void everyRemainingBookHasADataFileASkillIdAnIconAndALangEntry() throws IOException {
        JsonObject fragment = JsonParser.parseString(Files.readString(FRAGMENT, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> files = new HashSet<>();
        try (Stream<Path> s = Files.list(TREES)) {
            s.forEach(p -> files.add(p.getFileName().toString().replace(".json", "")));
        }
        List<LineIndex.Line> lines = LineIndex.shipped();
        assertEquals(files.size(), lines.size(), "one book per data file");
        Set<String> skillPaths = new HashSet<>();
        for (LineIndex.Line l : lines) {
            assertTrue(files.contains(l.bare()), l.bare() + " has no data file");
            assertTrue(skillPaths.add(l.skillPath()), "skill path used twice: " + l.skillPath());
            assertTrue(Files.exists(ICONS.resolve(l.bare() + ".png")), l.bare() + " has no skill icon");
            String name = SkillTexts.nameKey(l.bare()), desc = SkillTexts.descKey(l.bare());
            assertTrue(fragment.has(name) && !fragment.get(name).getAsString().isEmpty(), l.bare() + " has no name in the lang fragment");
            assertTrue(fragment.has(desc) && !fragment.get(desc).getAsString().isEmpty(), l.bare() + " has no description in the lang fragment");
            assertEquals(SkillTexts.description(l), fragment.get(desc).getAsString(), l.bare() + ": the fragment and the fallback must say the same words");
            assertEquals(l.name(), fragment.get(name).getAsString());
            assertEquals(Math.max(1, l.arts().size()), SkillTexts.modeNames(l).size(), l.bare() + ": one mode per art, or a single generic one");
        }
    }

    @Test
    void theLoaderWillSeeEveryBookTheShopAndTheLootCanHandOut() throws IOException {
        // the shop sells "all books" from the registry and the loot draws from it; both only hand out books a skill exists for, so a data file without a table entry would be dead stock
        JsonObject table = JsonParser.parseString(Files.readString(TABLE, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("lines");
        try (Stream<Path> s = Files.list(TREES)) {
            for (Path p : s.toList()) {
                String id = p.getFileName().toString().replace(".json", "");
                assertTrue(table.has(id), id + " has a data file but no entry in skill_books.json, so no skill is registered for it");
            }
        }
    }
}
