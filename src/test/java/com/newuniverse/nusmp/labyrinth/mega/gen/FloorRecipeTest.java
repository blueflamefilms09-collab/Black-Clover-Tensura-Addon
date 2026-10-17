package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the recipe of a floor is data the owner will edit, exactly like the sector recipe, and it has the same two silent failures (a typo that keeps the default, a
 * shipped file that drifts from the numbers compiled into {@link Recipe#forFloors}). So the shipped {@code data/nusmp/mega/recipe/floor.json} is read with the reader the
 * game uses, laid over the same base, and compared with the compiled numbers; and the floor numbers are checked against what a small floor can keep (a floor of 28
 * rooms cannot promise two treasure closets and three traps).
 */
class FloorRecipeTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/mega/recipe/floor.json");

    @Test
    @SuppressWarnings("unchecked")
    void theShippedFileIsTheCompiledFloorRecipe() throws IOException {
        Map<String, Object> json = (Map<String, Object>) MiniJson.parse(Files.readString(FILE, StandardCharsets.UTF_8));
        assertTrue(json.containsKey("_why"), "every data file says why it exists");
        Recipe file = Recipe.fromMap(json, Recipe.forFloors()), code = Recipe.forFloors();
        assertEquals(java.util.List.of(), file.unknownKeys(), "every key of the file is one the loader knows");
        assertEquals(code.maxDepth, file.maxDepth);
        assertEquals(code.minRooms, file.minRooms);
        assertEquals(code.minLoops, file.minLoops);
        assertEquals(code.arenaMinDistance, file.arenaMinDistance);
        assertEquals(code.arenaMinDepth, file.arenaMinDepth);
        assertEquals(code.extraSafe, file.extraSafe);
        assertEquals(code.safeMinDistance, file.safeMinDistance);
        assertEquals(code.safeMaxDistance, file.safeMaxDistance);
        assertEquals(code.chainSockets, file.chainSockets);
        assertEquals(code.chainLeaves, file.chainLeaves);
        assertEquals(code.capReserve, file.capReserve);
        assertEquals(code.stairMinWalk, file.stairMinWalk);
        for (RoomKind k : RoomKind.values()) {
            assertEquals(code.min(k), file.min(k), "min " + k);
            assertEquals(code.max(k), file.max(k), "max " + k);
        }
    }

    @Test
    void aFloorPromisesLessThanAnOldSectorAndTheDefaultsAreUntouched() {
        Recipe floor = Recipe.forFloors(), old = new Recipe();
        assertTrue(floor.minRooms < old.minRooms && floor.minLoops < old.minLoops);
        assertTrue(floor.min(RoomKind.TREASURE) < old.min(RoomKind.TREASURE));
        assertTrue(floor.safeMaxDistance < old.safeMaxDistance, "the safe room lies nearer on a small floor");
        assertEquals(40, old.minRooms, "the sector recipe of 1.54.0 is unchanged");
        assertEquals(4, floor.stairMinWalk);
        Recipe over = Recipe.fromMap(Map.of("stairMinWalk", 7L), Recipe.forFloors());
        assertEquals(7, over.stairMinWalk);
        assertEquals(12, over.minRooms, "a file laid over the floor recipe keeps the floor numbers it does not mention");
    }
}
