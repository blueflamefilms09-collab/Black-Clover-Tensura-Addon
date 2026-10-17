package com.newuniverse.nusmp.labyrinth.mega.safe;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the compat tables are data the owner will edit after looking inside another mod's jar, so two things must hold. The reader must be
 * forgiving (a broken or half-written file gives null, never an exception that would take a data reload down) and the files that SHIP must be
 * well-formed: a mod id, hints from the set the templates actually use, ids that are real {@code namespace:path} strings in the file's own
 * namespace (a copy-paste from the other file would silently never match), tokens only from the two the processor knows, and every file
 * actually able to pick something when its mod is present. Reading the shipped files from the source tree (or the class path) is what turns
 * "I forgot a comma" into a test failure instead of a silent no-swap in game.
 */
class CompatJsonTest {
    private static final String DIR = "data/nusmp/mega/compat/";
    private static final List<String> SHIPPED = List.of("farmersdelight.json", "refurbished_furniture.json");

    private static String read(String file) {
        try (InputStream in = CompatJsonTest.class.getClassLoader().getResourceAsStream(DIR + file)) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) { /* fall through to the source tree */ }
        for (String root : new String[] {"src/main/resources/", "../src/main/resources/", "../../src/main/resources/"}) {
            Path p = Paths.get(root + DIR + file);
            try {
                if (Files.exists(p)) return Files.readString(p, StandardCharsets.UTF_8);
            } catch (IOException ignored) { /* try the next root */ }
        }
        return null;
    }

    @Test void aWellFormedFileParses() {
        CompatTable.Source s = CompatJson.parse("{\"mod\":\"m\",\"swaps\":{\"stove\":[\"m:stove\",7,null],\"chair\":\"m:chair\",\"_note\":[\"x:y\"]},\"flip_facing\":[\"chair\"],\"facing_property\":\"direction\"}");
        assertNotNull(s);
        assertEquals("m", s.mod());
        assertEquals(List.of("m:stove"), s.swaps().get("stove"), "non-strings are skipped");
        assertEquals(List.of("m:chair"), s.swaps().get("chair"), "a single string stands for a list of one");
        assertFalse(s.swaps().containsKey("_note"), "underscore keys are comments");
        assertTrue(s.flipFacing().contains("chair"));
        assertEquals("direction", s.facingProperty());
    }

    @Test void brokenInputGivesNullAndNeverThrows() {
        assertNull(CompatJson.parse("not json at all {{{"));
        assertNull(CompatJson.parse(""));
        assertNull(CompatJson.parse("[]"));
        assertNull(CompatJson.parse("{}"));
        assertNull(CompatJson.parse("{\"mod\":\"m\"}"));
        assertNull(CompatJson.parse("{\"swaps\":{}}"));
        assertNull(CompatJson.parse("{\"mod\":\"\",\"swaps\":{}}"));
        assertNull(CompatJson.parse("{\"mod\":\"m\",\"swaps\":[1,2]}"));
        assertNull(CompatJson.parse((String) null));
    }

    @Test void theOptionalFieldsMayBeAbsent() {
        CompatTable.Source s = CompatJson.parse("{\"mod\":\"m\",\"swaps\":{}}");
        assertNotNull(s);
        assertTrue(s.flipFacing().isEmpty());
        assertNull(s.facingProperty());
        assertTrue(CompatTable.of(List.of(s)).isEmpty());
    }

    @Test void theShippedFilesAreWellFormed() {
        for (String file : SHIPPED) {
            String text = read(file);
            assertNotNull(text, file + " must ship under " + DIR);
            CompatTable.Source s = CompatJson.parse(text);
            assertNotNull(s, file + " must parse");
            assertFalse(s.swaps().isEmpty(), file + " must offer at least one hint");
            assertEquals(file.substring(0, file.length() - 5), s.mod(), "the file is named after its mod");
            for (Map.Entry<String, List<String>> e : s.swaps().entrySet()) {
                assertTrue(CompatTable.KNOWN_HINTS.contains(e.getKey()), file + ": unknown hint " + e.getKey());
                assertFalse(e.getValue().isEmpty(), file + ": hint " + e.getKey() + " has no candidates");
                for (String id : e.getValue()) {
                    String expanded = CompatTable.expand(id, CompatTable.tokensFor("oak_planks"));
                    assertNotNull(expanded, file + ": " + id + " uses a token other than {wood} and {color}");
                    assertTrue(CompatTable.validId(expanded), file + ": " + id + " is not a lower-case namespace:path id");
                    assertTrue(expanded.startsWith(s.mod() + ":"), file + ": " + id + " is not in the mod's own namespace");
                }
            }
            for (String flipped : s.flipFacing()) assertTrue(s.swaps().containsKey(flipped), file + ": flip_facing names a hint with no candidates: " + flipped);
        }
    }

    @Test void everyShippedHintCanPickWhenItsModIsPresent() {
        for (String file : SHIPPED) {
            CompatTable.Source s = CompatJson.parse(read(file));
            assertNotNull(s);
            CompatTable t = CompatTable.of(List.of(s));
            for (String hint : s.swaps().keySet()) {
                assertNotNull(t.pick(hint, m -> true, b -> true, CompatTable.tokensFor("dark_oak_stairs")), file + ": " + hint + " cannot pick even when every block exists");
                assertNull(t.pick(hint, m -> false, b -> true, CompatTable.tokensFor("dark_oak_stairs")), file + ": " + hint + " must not pick when the mod is absent");
            }
        }
    }

    @Test void theFarmersDelightFileCoversTheKitchenHints() {
        CompatTable.Source s = CompatJson.parse(read("farmersdelight.json"));
        assertNotNull(s);
        for (String hint : List.of("stove", "skillet", "cutting_board", "cooking_pot")) assertTrue(s.swaps().containsKey(hint), "farmersdelight.json must offer " + hint);
        assertEquals(List.of("farmersdelight:stove"), s.swaps().get("stove"));
    }

    @Test void theFurnitureFileCoversSeatsTablesAndStorageAndFlipsTheSeats() {
        CompatTable.Source s = CompatJson.parse(read("refurbished_furniture.json"));
        assertNotNull(s);
        for (String hint : List.of("chair", "sofa", "table", "counter", "storage", "bed")) assertTrue(s.swaps().containsKey(hint), "refurbished_furniture.json must offer " + hint);
        assertTrue(s.flipFacing().contains("chair"));
        assertTrue(s.flipFacing().contains("sofa"));
        assertFalse(s.flipFacing().contains("table"));
    }
}
