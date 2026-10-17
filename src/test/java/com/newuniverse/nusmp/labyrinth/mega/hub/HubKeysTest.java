package com.newuniverse.nusmp.labyrinth.mega.hub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Hub writes to the player's persistent data and two of the keys are not its own: the gate cooldown keys belong to {@code LabyrinthTravel}
 * (package-private there) and the Labyrinth's gate block reads them. If the Hub spelled them differently the cooldown would silently not work and a gate
 * would send the player straight back (or refuse them once per tick). The strings are pinned here, they are proved to appear in {@code LabyrinthTravel.java}
 * (the test reads the source, so a rename there fails this test instead of breaking the game), and every key the Hub owns is checked not to collide with
 * another one.
 */
class HubKeysTest {

    @Test
    void theNamesAreExactlyThese() {
        assertEquals("nusmp_hub_origin", HubKeys.ORIGIN);
        assertEquals("nusmp_labyrinth_gate", HubKeys.GATE_USED);
        assertEquals("nusmp_dungeon_gate", HubKeys.LEGACY_GATE_USED);
        assertEquals("nusmp_hub_allowed", HubKeys.ALLOWED);
        assertEquals("nusmp_hub_told", HubKeys.TOLD);
        assertEquals("nusmp_hub", HubKeys.DATA_NAME);
        assertEquals("Dim", HubKeys.DIM);
    }

    @Test
    void theCooldownKeysAreTheOnesTheLabyrinthTravelWritesAndTheGateReads() throws IOException {
        String src = HubTestIo.read(Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/gate/LabyrinthTravel.java"));
        assertTrue(src.contains("GATE_USED = \"" + HubKeys.GATE_USED + "\""), "LabyrinthTravel.GATE_USED is no longer " + HubKeys.GATE_USED);
        assertTrue(src.contains("LEGACY_GATE_USED = \"" + HubKeys.LEGACY_GATE_USED + "\""), "LabyrinthTravel.LEGACY_GATE_USED is no longer " + HubKeys.LEGACY_GATE_USED);
        assertTrue(src.contains("ORIGIN = \"nusmp_labyrinth_origin\""), "the Labyrinth's own origin key is the one the Hub's differs from");
    }

    @Test
    void theHubsOwnKeysAreDistinctAndNotTheLabyrinthsOrigin() {
        Set<String> seen = new HashSet<>();
        for (String k : new String[]{HubKeys.ORIGIN, HubKeys.GATE_USED, HubKeys.LEGACY_GATE_USED, HubKeys.ALLOWED, HubKeys.TOLD, HubKeys.DATA_NAME}) {
            assertTrue(seen.add(k), "duplicate key " + k);
            assertTrue(k.startsWith("nusmp_"), "every key is namespaced: " + k);
        }
        assertTrue(!seen.contains("nusmp_labyrinth_origin") && !seen.contains("nusmp_labyrinth_pad"), "the Hub never touches the Labyrinth's way back");
    }
}
