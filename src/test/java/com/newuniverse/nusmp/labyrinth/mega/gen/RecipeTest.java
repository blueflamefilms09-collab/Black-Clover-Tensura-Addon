package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Sector Recipe is data the owner will edit, and the two ways that goes wrong silently are a typo (the key is ignored and the default stays) and
 * a default that drifts away from the shipped file. So unknown keys are reported, values are clamped to sane ranges, and the shipped
 * {@code data/nusmp/mega/recipe/default.json} is read with the same reader the game uses and compared field by field with the defaults compiled into
 * {@link Recipe}: the file is documentation that cannot lie.
 */
class RecipeTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/mega/recipe/default.json");

    @Test
    void defaultsAreTheDesignDocumentsRecipe() {
        Recipe r = new Recipe();
        assertEquals(24, r.maxDepth);
        assertEquals(40, r.minRooms);
        assertEquals(3, r.minLoops);
        assertEquals(8.0, r.arenaMinDistance);
        assertEquals(1, r.extraSafe);
        assertEquals(5.0, r.safeMinDistance);
        assertEquals(12.0, r.safeMaxDistance);
        assertEquals(1, r.min(RoomKind.TRIAL));
        assertEquals(2, r.min(RoomKind.TREASURE));
        assertEquals(1, r.max(RoomKind.SET_PIECE));
        assertEquals(0, r.min(RoomKind.HALL));
        assertEquals(Integer.MAX_VALUE, r.max(RoomKind.HALL));
        assertEquals(0, r.shapeMax("nusmp:mega/set/anything"));
    }

    @Test
    void theRoomBuildersNotesAreInTheDefaults() {
        Recipe r = new Recipe();
        for (RoomKind k : new RoomKind[] {RoomKind.TRIAL, RoomKind.TRAP, RoomKind.PUZZLE}) assertEquals(2, r.max(k), "about one per sector, never more than two: " + k);
        assertEquals(1, r.shapeMax("nusmp:mega/hall/great_hall"));
        assertEquals(1, r.shapeMax("nusmp:mega/set/cathedral_rotunda"));
        assertEquals(1, r.shapeMax("nusmp:mega/trap/dark_room"));
        assertEquals(0, r.shapeMax("nusmp:mega/hall/crypt_a"), "an ordinary hall may repeat");
        Recipe own = Recipe.fromMap(Map.of("shapeMax", Map.of("nusmp:mega/hall/great_hall", 0L)));
        assertEquals(0, own.shapeMax("nusmp:mega/hall/great_hall"), "a data pack can lift a default limit with 0");
        assertEquals(1, own.shapeMax("nusmp:mega/set/cathedral_rotunda"), "and only that one");
    }

    @Test
    void fromMapOverridesAndReportsWhatItDoesNotKnow() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("minRooms", 60L);
        m.put("arenaMinDistance", 10.5);
        m.put("requireMarkers", Boolean.FALSE);
        m.put("min", Map.of("trial", 3L));
        m.put("max", Map.of("set", 0L));
        m.put("shapeMax", Map.of("nusmp:mega/set/rotunda", 1L));
        m.put("minRoom", 5L);                                   // a typo
        m.put("_comment", "ignored");
        m.put("min2", Map.of());
        Recipe r = Recipe.fromMap(m);
        assertEquals(60, r.minRooms);
        assertEquals(10.5, r.arenaMinDistance);
        assertFalse(r.requireMarkers);
        assertEquals(3, r.min(RoomKind.TRIAL));
        assertEquals(2, r.min(RoomKind.TREASURE), "an unmentioned kind keeps its default");
        assertEquals(0, r.max(RoomKind.SET_PIECE));
        assertEquals(1, r.shapeMax("nusmp:mega/set/rotunda"));
        assertTrue(r.unknownKeys().contains("minRoom"), "a typo is reported: " + r.unknownKeys());
        assertTrue(r.unknownKeys().contains("min2"));
        assertFalse(r.unknownKeys().contains("_comment"), "underscore keys are comments");
    }

    @Test
    void valuesAreClampedAndBadKindsAreReported() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("minRooms", -5L);
        m.put("maxDepth", 9999L);
        m.put("safeMinDistance", 20.0);
        m.put("safeMaxDistance", 3.0);
        m.put("min", Map.of("dragon", 1L));
        m.put("chainLeaves", "many");
        Recipe r = Recipe.fromMap(m);
        assertEquals(1, r.minRooms);
        assertEquals(64, r.maxDepth);
        assertTrue(r.safeMaxDistance >= r.safeMinDistance, "the range is never inverted");
        assertTrue(r.unknownKeys().contains("min.dragon"));
        assertEquals(10, r.chainLeaves, "a value of the wrong type keeps the default");
        assertEquals(0, Recipe.fromMap(null).unknownKeys().size());
    }

    @Test
    void copiesAreIndependent() {
        Recipe a = new Recipe();
        Recipe b = a.copy();
        b.minRooms = 99;
        b.setMin(RoomKind.TRIAL, 7);
        assertEquals(40, a.minRooms);
        assertEquals(1, a.min(RoomKind.TRIAL));
        assertEquals(7, b.min(RoomKind.TRIAL));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theShippedFileMatchesTheCompiledDefaults() throws IOException {
        assertTrue(Files.exists(FILE), "the shipped recipe must exist at " + FILE.toAbsolutePath());
        Map<String, Object> json = (Map<String, Object>) MiniJson.parse(Files.readString(FILE, StandardCharsets.UTF_8));
        assertTrue(json.containsKey("_why"), "every data file says why it exists");
        Recipe file = Recipe.fromMap(json), code = new Recipe();
        assertEquals(java.util.List.of(), file.unknownKeys(), "every key of the file is one the loader knows");
        assertEquals(code.maxDepth, file.maxDepth);
        assertEquals(code.minRooms, file.minRooms);
        assertEquals(code.minLoops, file.minLoops);
        assertEquals(code.arenaMinDistance, file.arenaMinDistance);
        assertEquals(code.arenaMinDepth, file.arenaMinDepth);
        assertEquals(code.reserveDepth, file.reserveDepth);
        assertEquals(code.extraSafe, file.extraSafe);
        assertEquals(code.safeMinDistance, file.safeMinDistance);
        assertEquals(code.safeMaxDistance, file.safeMaxDistance);
        assertEquals(code.chainSockets, file.chainSockets);
        assertEquals(code.chainLeaves, file.chainLeaves);
        assertEquals(code.stepBudget, file.stepBudget);
        assertEquals(code.capReserve, file.capReserve);
        assertEquals(code.requireMarkers, file.requireMarkers);
        for (RoomKind k : RoomKind.values()) {
            assertEquals(code.min(k), file.min(k), "min " + k);
            assertEquals(code.max(k), file.max(k), "max " + k);
        }
        for (String id : new String[] {"nusmp:mega/hall/great_hall", "nusmp:mega/set/cathedral_rotunda", "nusmp:mega/trap/dark_room", "nusmp:mega/hall/crypt_a"}) {
            assertEquals(code.shapeMax(id), file.shapeMax(id), "shapeMax " + id);
        }
    }
}
