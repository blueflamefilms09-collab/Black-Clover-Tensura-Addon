package com.newuniverse.nusmp.compat.maps;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The one-line hooks that connect the map compatibility to the rest of the mod are still there (a source check: the hooks live in code that needs the game to run). */
class MapHooksTest {
    private static final Path ROOT = Paths.get("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void theMomentsCallTheShareHelper() throws IOException {
        Map<String, String> hooks = Map.of(
                "labyrinth/mega/hub/HubTravel.java", "MapShare.gate(",
                "labyrinth/mega/rift/WaypointService.java", "MapShare.returnWaypoint(",
                "labyrinth/mega/rift/RiftService.java", "MapShare.rift(",
                "labyrinth/mega/safe/RestEvents.java", "MapShare.safeRoom(");
        for (Map.Entry<String, String> e : hooks.entrySet())
            assertTrue(read(e.getKey()).contains("compat.maps." + e.getValue()), e.getKey() + " no longer calls compat.maps." + e.getValue());
        assertTrue(read("labyrinth/mega/hub/HubTravel.java").contains("compat.maps.MapShare.hubGates("));
    }

    @Test
    void theEntryPointsAreWired() throws IOException {
        assertTrue(read("NUSMP.java").contains("compat.maps.MapsCompat.init(modContainer)"));
        assertTrue(read("NUSMP.java").contains("compat.maps.MapsClientConfig.SPEC"));
        assertTrue(read("multiverse/MultiverseCommands.java").contains("compat.maps.MapsCommand.waypointNode()"));
        assertTrue(read("compat/CompatCommand.java").contains("MapsCommand.compatMapsNode()"));
    }

    @Test
    void theServerConfigFileNameIsTheDocumentedOne() throws IOException {
        assertTrue(read("compat/maps/MapsCompat.java").contains("\"nusmp-compat-maps-server.toml\""));
        assertTrue(read("NUSMP.java").contains("\"nusmp-compat-maps-client.toml\""));
    }

    @Test
    void noMapModClassIsReferencedAtCompileTime() throws IOException {
        // the integration is soft: no import of, and no reflection into, a map mod. One exception since 1.90.0: FtbClaimBlock hooks FTB Chunks' own cancellable claim events by reflection (it
        // is the only supported way to refuse a claim, and the mod has no compile-time dependency on FTB Chunks), and nothing else in the package may.
        try (var files = Files.walk(ROOT.resolve("compat/maps"))) {
            for (Path p : files.filter(x -> x.toString().endsWith(".java")).toList()) {
                String s = Files.readString(p, StandardCharsets.UTF_8);
                for (String banned : new String[]{"import xaero", "import dev.ftb.mods.ftbchunks", "import journeymap", "Class.forName"}) {
                    if (banned.equals("Class.forName") && p.getFileName().toString().equals("FtbClaimBlock.java")) continue;
                    assertFalse(s.contains(banned), p.getFileName() + " must stay soft but contains " + banned);
                }
            }
        }
    }
}
