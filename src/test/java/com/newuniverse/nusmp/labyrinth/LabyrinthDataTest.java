package com.newuniverse.nusmp.labyrinth;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Labyrinth is Jigsaw, and a Jigsaw structure fails SILENTLY when a data file is missing: a pool element whose template does not
 * exist is logged once at world creation and the piece is simply skipped, a missing fallback pool leaves a hole in a wall, a processor
 * list that does not exist makes the whole piece fail to load. None of that is a compile error, so this test is the compile step for the
 * data: it walks the JSON and the template files the way the game's resource loader would resolve them and fails the build on a dangling
 * reference. It is pure (java.nio and regular expressions over the JSON text, no Minecraft classes, no JSON library on the test class
 * path) and reads from the project root, which is the working directory of Gradle's test task.
 */
class LabyrinthDataTest {
    private static final Path DATA = Path.of("src/main/resources/data/nusmp");
    private static final Path POOLS = DATA.resolve("worldgen/template_pool");
    private static final Path LABYRINTH_POOLS = POOLS.resolve("labyrinth");
    private static final Path STRUCTURES = DATA.resolve("structure");
    private static final Path STRUCTURE_JSON = DATA.resolve("worldgen/structure");
    private static final Path PROCESSOR_LISTS = DATA.resolve("worldgen/processor_list");

    private static final Pattern LOCATION = Pattern.compile("\"location\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern FALLBACK = Pattern.compile("\"fallback\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PROCESSORS = Pattern.compile("\"processors\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern START_POOL = Pattern.compile("\"start_pool\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PROCESSOR_TYPE = Pattern.compile("\"processor_type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern POOL_NAME_IN_NBT = Pattern.compile("nusmp:labyrinth/[a-z0-9_/]+");

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

    /** nusmp:labyrinth/side -> .../template_pool/labyrinth/side.json */
    private static Path poolFile(String id) {
        assertTrue(id.startsWith("nusmp:"), "pool " + id + " is not in the nusmp namespace");
        return POOLS.resolve(id.substring("nusmp:".length()) + ".json");
    }

    private static Path templateFile(String id) {
        assertTrue(id.startsWith("nusmp:"), "template " + id + " is not in the nusmp namespace (the example pack's chapel is not a pool element of the jar)");
        return STRUCTURES.resolve(id.substring("nusmp:".length()) + ".nbt");
    }

    @Test
    void theTwelvePoolsOfTheSpireExist() {
        for (String name : List.of("base", "ascent_1", "ascent_2", "ascent_3", "ascent_4", "warden", "peak", "vault", "crown", "side", "wildcard", "caps"))
            assertTrue(Files.exists(LABYRINTH_POOLS.resolve(name + ".json")), "missing pool labyrinth/" + name);
    }

    @Test
    void everyPoolElementHasATemplate() throws IOException {
        List<Path> pools = files(LABYRINTH_POOLS, ".json");
        assertFalse(pools.isEmpty(), "no labyrinth pools found: is the working directory the project root?");
        for (Path pool : pools) {
            List<String> locations = all(LOCATION, read(pool));
            for (String loc : locations) assertTrue(Files.exists(templateFile(loc)), pool.getFileName() + " names template " + loc + " but " + templateFile(loc) + " does not exist");
        }
    }

    @Test
    void everyFallbackIsAPoolOrEmpty() throws IOException {
        for (Path pool : files(LABYRINTH_POOLS, ".json")) {
            List<String> fallbacks = all(FALLBACK, read(pool));
            assertEquals(1, fallbacks.size(), pool.getFileName() + " needs exactly one fallback");
            String fb = fallbacks.get(0);
            assertTrue(fb.equals("minecraft:empty") || Files.exists(poolFile(fb)), pool.getFileName() + " falls back to " + fb + " which is not a pool");
        }
    }

    @Test
    void everyBranchPoolFallsBackToCapsSoNoDoorwayIsLeftOpen() throws IOException {
        for (String name : List.of("ascent_1", "ascent_2", "ascent_3", "ascent_4", "warden", "peak", "vault", "crown", "side", "side_end", "wildcard"))
            assertEquals("nusmp:labyrinth/caps", all(FALLBACK, read(LABYRINTH_POOLS.resolve(name + ".json"))).get(0), name + " must fall back to the caps pool");
        assertEquals("minecraft:empty", all(FALLBACK, read(LABYRINTH_POOLS.resolve("caps.json"))).get(0), "the caps pool ends the chain");
    }

    @Test
    void theWildcardPoolIsNeverEmpty() throws IOException {
        // Vanilla skips a pool with zero elements WITHOUT consulting its fallback, so the jar's wildcard pool carries one plug element.
        assertFalse(all(LOCATION, read(LABYRINTH_POOLS.resolve("wildcard.json"))).isEmpty(), "an empty wildcard pool would leave wild_out doorways open");
    }

    @Test
    void everyProcessorListNamedByAPoolExists() throws IOException {
        for (Path pool : files(LABYRINTH_POOLS, ".json")) {
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
    void theSpireStartsInTheBasePool() throws IOException {
        List<String> start = all(START_POOL, read(STRUCTURE_JSON.resolve("labyrinth_spire.json")));
        assertEquals(List.of("nusmp:labyrinth/base"), start);
    }

    @Test
    void labyrinthInteriorChainsOnlyThePaletteAndTheVeins() throws IOException {
        List<String> types = all(PROCESSOR_TYPE, read(PROCESSOR_LISTS.resolve("labyrinth_interior.json")));
        assertEquals(List.of("nusmp:biome_palette", "nusmp:magicule_veins"), types, "palette first, veins second: the veins only grow in masonry that survived the palette swap");
    }

    @Test
    void everyJigsawPoolNamedInATemplateExists() throws IOException {
        // The pool of a jigsaw is a string inside the gzip NBT; scanning the decompressed bytes for nusmp:labyrinth/... finds all of them.
        TreeSet<String> missing = new TreeSet<>();
        int templates = 0;
        for (Path nbt : files(STRUCTURES.resolve("labyrinth"), ".nbt")) {
            templates++;
            byte[] raw = Files.readAllBytes(nbt);
            byte[] plain;
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                plain = in.readAllBytes();
            }
            Matcher m = POOL_NAME_IN_NBT.matcher(new String(plain, StandardCharsets.ISO_8859_1));
            while (m.find()) {
                String id = m.group();
                if (!Files.exists(poolFile(id))) missing.add(nbt.getFileName() + " -> " + id);
            }
        }
        assertTrue(templates >= 25, "expected the spire templates under structure/labyrinth, found " + templates);
        assertTrue(missing.isEmpty(), "jigsaws name pools that do not exist: " + missing);
    }
}
