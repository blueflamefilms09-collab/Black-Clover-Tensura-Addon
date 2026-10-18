package com.newuniverse.nusmp.skilltree.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the registry of stand-in skills is built from the jar's table of lines BEFORE any world exists, so the table is the contract between the route files (server data) and the registry (frozen at start).
 * This test fails the build when they drift: one stand-in per route file, every id usable as a registry path and unique, every stand-in with its icon file, and the parser tolerant of the bad entries a hand
 * edit could leave in the table (the game must start with one fewer line, never fail to start).
 */
class LineIndexTest {
    private static final Path TREES = Path.of("src/main/resources/data/nusmp/skill_trees");
    private static final Path ICONS = Path.of("src/main/resources/assets/nusmp/textures/skill/line");

    @Test
    void theTableHasOneEntryPerRouteFileAndNothingElse() throws IOException {
        Set<String> files = new HashSet<>();
        try (Stream<Path> s = Files.list(TREES)) {
            s.filter(p -> p.toString().endsWith(".json")).forEach(p -> files.add(p.getFileName().toString().replace(".json", "")));
        }
        Set<String> table = new HashSet<>();
        for (LineIndex.Line l : LineIndex.load()) table.add(l.bare());
        assertFalse(files.isEmpty());
        assertEquals(files, table, "assets/nusmp/skill_books.json and data/nusmp/skill_trees must list the same lines (run tools/lines/gen_book_index.py)");
    }

    @Test
    void everyLineHasAUsableUniqueSkillPathAnIconAndAName() {
        Set<String> paths = new HashSet<>();
        List<LineIndex.Line> lines = LineIndex.load();
        assertTrue(lines.size() >= 283, "all 283 routes get a stand-in, got " + lines.size());
        for (LineIndex.Line l : lines) {
            assertTrue(LineIndex.validBare(l.bare()), l.bare());
            assertTrue(l.skillPath().matches("[a-z0-9_]+"), "a registry path: " + l.skillPath());
            assertTrue(paths.add(l.skillPath()), "duplicate skill path " + l.skillPath());
            assertEquals("nusmp:" + l.bare(), l.routeId());
            assertFalse(l.name().isBlank(), l.bare());
            assertTrue(l.color() >= 0 && l.color() <= 0xFFFFFF);
            assertTrue(Files.isRegularFile(ICONS.resolve(l.bare() + ".png")), "missing icon textures/skill/line/" + l.bare() + ".png (run tools/tensura_menu/gen_line_icons.py)");
        }
    }

    @Test
    void theOrderIsTheIdOrderWhateverTheFileOrderIs() {
        JsonObject root = JsonParser.parseString("{\"lines\":{\"zeta\":{\"name\":\"Z\"},\"alpha\":{\"name\":\"A\"},\"mid\":{\"name\":\"M\"}}}").getAsJsonObject();
        List<LineIndex.Line> l = LineIndex.parse(root);
        assertEquals(List.of("alpha", "mid", "zeta"), l.stream().map(LineIndex.Line::bare).toList());
    }

    @Test
    void aBadEntryIsSkippedAndNeverBreaksTheGame() {
        JsonObject root = JsonParser.parseString("{\"lines\":{\"ok_one\":{\"name\":\"One\",\"color\":\"#102030\",\"franchise\":\"tensura\"},\"Bad Id\":{\"name\":\"x\"},\"not_object\":3,\"with-dash\":{},\"fine_two\":{}}}").getAsJsonObject();
        List<LineIndex.Line> l = LineIndex.parse(root);
        assertEquals(List.of("fine_two", "ok_one"), l.stream().map(LineIndex.Line::bare).toList());
        assertEquals("fine_two", l.get(0).name(), "a line without a name is called by its id");
        assertEquals(0xFFFFFF, l.get(0).color(), "a missing colour is white");
        assertEquals(0x102030, l.get(1).color());
        assertEquals("tensura", l.get(1).franchise());
        assertTrue(LineIndex.parse(null).isEmpty());
        assertTrue(LineIndex.parse(new JsonObject()).isEmpty());
    }

    @Test
    void idHelpersConvertBothWays() {
        assertEquals("line_fire", LineIndex.skillPath("fire"));
        assertEquals("nusmp:fire", LineIndex.routeId("fire"));
        assertEquals("nusmp:fire", LineIndex.routeId("nusmp:fire"));
        assertEquals("fire", LineIndex.bare("nusmp:fire"));
        assertEquals("fire", LineIndex.bare("fire"));
    }
}
