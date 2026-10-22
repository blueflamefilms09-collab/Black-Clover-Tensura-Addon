package com.newuniverse.nusmp.client.relics.worn.lib;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.relics.RelicWear;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Curios data of the mod, read from the resources folder: every slot type the relics use is DEFINED by the mod (not left to Curios), has a size, is given to the player and has a tag file,
 * and every relic of every wave (0 to 105 and the pairs, the Djinn and the forge items, which are defined in the same wave sources) is listed in the tag of its slot. The dungeon relic and
 * celestial key slots keep working. Same checks as the self-test {@code relic.wear}, which runs them on the registered relics.
 */
class RelicWearDataTest {
    static final Path RES = Path.of("src/main/resources");

    static RelicWear.Data data() throws IOException {
        return RelicWear.read(path -> {
            Path p = RES.resolve(path);
            return Files.exists(p) ? Files.newInputStream(p) : null;
        });
    }

    @Test
    void everySlotTheRelicsUseIsDefinedAssignedAndTagged() throws IOException {
        assertEquals(List.of(), RelicWear.checkSlots(data()));
    }

    @Test
    void everyWaveRelicWornInACuriosSlotCanBePutOn() throws IOException {
        Map<String, String> slots = new LinkedHashMap<>();
        for (WornCoverage.Entry r : WornCoverageTest.relicsFromSources()) slots.put("nusmp:" + r.id(), r.slot());
        assertTrue(slots.size() > 800, "relics read from the wave sources: " + slots.size());
        long curios = slots.values().stream().filter(s -> RelicWear.curiosSlot(s) != null).count();
        long armour = slots.values().stream().filter(s -> RelicWear.armourEquipSlot(s) != null).count();
        assertTrue(curios > 500 && armour > 40, "Curios " + curios + ", armour " + armour);
        List<String> problems = RelicWear.checkRelics(slots, data());
        assertEquals(List.of(), problems.size() > 5 ? problems.subList(0, 5) : problems, problems.size() + " relics cannot be put on");
    }

    @Test
    void noRelicIsDefinedOutsideTheWaveSourcesSoTheScanSeesThemAll() throws IOException {
        List<String> outside = new ArrayList<>();
        try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/newuniverse/nusmp"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                String rel = p.toString().replace('\\', '/');
                if (rel.contains("/relics/wave/") || rel.endsWith("/relics/RelicDef.java")) continue;
                if (Files.readString(p).matches("(?s).*RelicDef\\s*\\.\\s*of\\s*\\(\\s*[^)]*RelicSlot\\s*\\.\\s*(NECK|BACK|BELT|HANDS|CHARM).*")) outside.add(rel);
            }
        }
        // the layers and a self-test mention the slots in code; none of them DEFINES a relic
        outside.removeIf(f -> f.endsWith("RelicWornLayer.java") || f.endsWith("WornRender2SelfTests.java"));
        assertEquals(List.of(), outside);
    }

    @Test
    void everySlotFileHasASizeAnOrderAnIconAndAValidator() throws IOException {
        Set<Integer> orders = new HashSet<>();
        List<String> ids = new ArrayList<>(RelicWear.RELIC_SLOTS);
        ids.addAll(RelicWear.OTHER_SLOTS);
        for (String id : ids) {
            JsonObject o = JsonParser.parseString(Files.readString(RES.resolve("data/nusmp/curios/slots/" + id + ".json"))).getAsJsonObject();
            assertTrue(o.get("size").getAsInt() >= 1, id + " size");
            assertTrue(orders.add(o.get("order").getAsInt()), id + ": the order is shared with another slot of the mod");
            assertTrue(o.get("icon").getAsString().matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"), id + " icon");
            assertEquals("curios:tag", o.getAsJsonArray("validators").get(0).getAsString(), id + " validator");
        }
    }

    @Test
    void theDungeonRelicAndCelestialKeySlotsKeepWorking() throws IOException {
        RelicWear.Data d = data();
        assertEquals(2, (int) d.slotSizes().get("dungeon_relic"));
        assertEquals(12, (int) d.slotSizes().get("celestial_key"));
        assertTrue(d.assigned().contains("dungeon_relic") && d.assigned().contains("celestial_key"));
        assertTrue(d.tags().get("dungeon_relic").contains("nusmp:glutton_chalice"));
        assertTrue(d.tags().get("celestial_key").contains("nusmp:leo_key"));
    }

    @Test
    void everyEntityFileGivesItsSlotsToThePlayerAndOnlyListsDefinedSlots() throws IOException {
        Set<String> defined = new HashSet<>();
        try (Stream<Path> s = Files.list(RES.resolve("data/nusmp/curios/slots"))) {
            s.forEach(p -> defined.add(p.getFileName().toString().replace(".json", "")));
        }
        try (Stream<Path> s = Files.list(RES.resolve("data/nusmp/curios/entities"))) {
            for (Path p : s.toList()) {
                JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                assertTrue(o.getAsJsonArray("entities").toString().contains("player"), p + " must target the player");
                o.getAsJsonArray("slots").forEach(e -> assertTrue(defined.contains(e.getAsString()), p + ": slot " + e + " has no slot file in this mod"));
            }
        }
        assertTrue(RelicWear.ENTITY_FILES.stream().allMatch(f -> Files.exists(RES.resolve("data/nusmp/curios/entities/" + f + ".json"))));
    }

    @Test
    void theTagFilesAreWellFormedAndNeverReplaceWhatCuriosShips() throws IOException {
        for (String id : RelicWear.RELIC_SLOTS) {
            JsonObject o = JsonParser.parseString(Files.readString(RES.resolve("data/curios/tags/item/" + id + ".json"))).getAsJsonObject();
            assertFalse(o.has("replace") && o.get("replace").getAsBoolean(), id + ": a replacing tag would drop the items other mods put in this slot");
            assertTrue(o.getAsJsonArray("values").size() > 20, id);
        }
    }
}
