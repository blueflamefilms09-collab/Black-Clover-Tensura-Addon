package com.newuniverse.nusmp.labyrinth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Gateway of the Mega-Dungeon is Jigsaw, and a Jigsaw structure fails SILENTLY when a data file is missing: a pool element whose template
 * does not exist is logged once at world creation and the piece is simply skipped, a missing fallback pool leaves a hole, a processor list that does
 * not exist makes the whole piece fail to load. None of that is a compile error, so this test is the compile step for the data: it walks the JSON and
 * the template files the way the game's resource loader would resolve them and fails the build on a dangling reference. It is pure (java.nio and
 * regular expressions over the JSON text, no Minecraft classes, no JSON library on the test class path) and reads from the project root, which is the
 * working directory of Gradle's test task. Until 1.58.0 it also walked the Spire's twelve pools; the Spire was removed with the old dungeon, and what
 * is left to check is the Gateway (the rooms of the Mega-Dungeon have their own catalog test).
 */
class LabyrinthDataTest {
    private static final Path DATA = Path.of("src/main/resources/data/nusmp");
    private static final Path POOLS = DATA.resolve("worldgen/template_pool");
    private static final Path GATEWAY_POOLS = POOLS.resolve("mega_gateway");
    private static final Path STRUCTURES = DATA.resolve("structure");
    private static final Path STRUCTURE_JSON = DATA.resolve("worldgen/structure");
    private static final Path PROCESSOR_LISTS = DATA.resolve("worldgen/processor_list");

    private static final Pattern LOCATION = Pattern.compile("\"location\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern FALLBACK = Pattern.compile("\"fallback\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PROCESSORS = Pattern.compile("\"processors\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern START_POOL = Pattern.compile("\"start_pool\"\\s*:\\s*\"([^\"]+)\"");

    private static String read(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private static List<Path> files(Path root, String suffix) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        }
    }

    private static List<String> all(Pattern p, String text) {
        List<String> out = new ArrayList<>();
        Matcher m = p.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /** nusmp:mega_gateway/base -> .../template_pool/mega_gateway/base.json */
    private static Path poolFile(String id) {
        assertTrue(id.startsWith("nusmp:"), "pool " + id + " is not in the nusmp namespace");
        return POOLS.resolve(id.substring("nusmp:".length()) + ".json");
    }

    private static Path templateFile(String id) {
        assertTrue(id.startsWith("nusmp:"), "template " + id + " is not in the nusmp namespace");
        return STRUCTURES.resolve(id.substring("nusmp:".length()) + ".nbt");
    }

    @Test
    void everyGatewayPoolElementHasATemplate() throws IOException {
        List<Path> pools = files(GATEWAY_POOLS, ".json");
        assertFalse(pools.isEmpty(), "no Gateway pools found: is the working directory the project root?");
        for (Path pool : pools) {
            List<String> locations = all(LOCATION, read(pool));
            assertFalse(locations.isEmpty(), pool.getFileName() + " has no element");
            for (String loc : locations) assertTrue(Files.exists(templateFile(loc)), pool.getFileName() + " names template " + loc + " but " + templateFile(loc) + " does not exist");
        }
    }

    @Test
    void everyGatewayFallbackIsAPoolOrEmpty() throws IOException {
        for (Path pool : files(GATEWAY_POOLS, ".json")) {
            List<String> fallbacks = all(FALLBACK, read(pool));
            assertEquals(1, fallbacks.size(), pool.getFileName() + " needs exactly one fallback");
            String fb = fallbacks.get(0);
            assertTrue(fb.equals("minecraft:empty") || Files.exists(poolFile(fb)), pool.getFileName() + " falls back to " + fb + " which is not a pool");
        }
    }

    @Test
    void everyProcessorListNamedByAGatewayPoolExists() throws IOException {
        for (Path pool : files(GATEWAY_POOLS, ".json")) {
            for (String proc : all(PROCESSORS, read(pool))) {
                if (proc.equals("minecraft:empty")) continue;
                assertTrue(proc.startsWith("nusmp:") && Files.exists(PROCESSOR_LISTS.resolve(proc.substring(6) + ".json")), pool.getFileName() + " uses processor list " + proc + " which does not exist");
            }
        }
    }

    @Test
    void everyStructureStartPoolExists() throws IOException {
        List<Path> structures = files(STRUCTURE_JSON, ".json");
        assertFalse(structures.isEmpty(), "no worldgen/structure files found");
        for (Path st : structures) {
            for (String pool : all(START_POOL, read(st))) assertTrue(Files.exists(poolFile(pool)), st.getFileName() + " starts in pool " + pool + " which does not exist");
        }
    }

    @Test
    void theGatewayStartsInItsBasePoolAndTheRemovedStructuresStayRemoved() throws IOException {
        assertEquals(List.of("nusmp:mega_gateway/base"), all(START_POOL, read(STRUCTURE_JSON.resolve("mega_gateway.json"))));
        assertFalse(Files.exists(STRUCTURE_JSON.resolve("labyrinth_spire.json")), "the Spire was removed in 1.58.0");
        assertFalse(Files.exists(STRUCTURE_JSON.resolve("dungeon_gate.json")), "the old Dungeon Gate arch was removed in 1.58.0");
    }
}
