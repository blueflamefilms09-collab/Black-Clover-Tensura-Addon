package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a data file with a wrong field stops the SERVER from booting (the datapack registries fail to load), and the author cannot run the game. These
 * tests are the compile step for the four data files of Floor 0: the dimension, its type, the biome and the biome modifier. They parse the real files and
 * check every required field of 1.21.1's codecs, the ranges the dimension type's codec enforces, that the dimension names a type and a biome that
 * exist as files, that every entity id in the biome's spawners is a vanilla ANIMAL (the list is generated from the API dump), that no monster or ambient
 * spawner is listed, and that every placed feature is one of the ids seen in real 1.21.1 plains-like biome data. The HubLayout numbers are checked
 * against the dimension type (the highest gate must fit under the build limit, the deepest above the bottom).
 */
class HubDataFilesTest {
    private static final Path DIM = HubTestIo.DATA.resolve("dimension/sanctuary.json");
    private static final Path TYPE = HubTestIo.DATA.resolve("dimension_type/sanctuary.json");
    private static final Path BIOME = HubTestIo.DATA.resolve("worldgen/biome/sanctuary_plains.json");
    private static final Path MODIFIER = HubTestIo.DATA.resolve("neoforge/biome_modifier/sanctuary_peace.json");

    /** Placed features that appear in the real 1.21.1 data of plains-like biomes (copied from vanilla's own biome JSON), the only ones the plains may use. */
    private static final Set<String> KNOWN_FEATURES = Set.of("minecraft:patch_tall_grass_2", "minecraft:trees_plains", "minecraft:flower_plains", "minecraft:patch_grass_plain",
            "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", "minecraft:patch_sugar_cane", "minecraft:patch_pumpkin", "minecraft:glow_lichen");

    private static Set<String> animals() throws IOException {
        return new HashSet<>(Files.readAllLines(Path.of("src/test/resources/mega/hub/vanilla_animals.txt")));
    }

    @Test
    void theDimensionNamesItsTypeItsGeneratorAndItsOneBiome() throws IOException {
        Map<String, Object> d = HubTestIo.obj(HubTestIo.json(DIM));
        assertEquals(Set.of("type", "generator"), d.keySet());
        assertEquals("nusmp:sanctuary", d.get("type"), "the dimension type id of the contract");
        assertTrue(Files.exists(TYPE), "the type exists as a file");
        Map<String, Object> g = HubTestIo.obj(d.get("generator"));
        assertEquals(Set.of("type", "biome_source"), g.keySet(), "the chunk generator codec has exactly one field, biome_source");
        assertEquals("nusmp:sanctuary", g.get("type"), "the chunk generator type id of the contract");
        Map<String, Object> src = HubTestIo.obj(g.get("biome_source"));
        assertEquals(Set.of("type", "biome"), src.keySet());
        assertEquals("minecraft:fixed", src.get("type"));
        assertEquals("nusmp:sanctuary_plains", src.get("biome"));
        assertTrue(Files.exists(BIOME), "the biome exists as a file");
    }

    @Test
    void theDimensionTypeHasEveryFieldTheCodecRequiresInRange() throws IOException {
        Map<String, Object> t = HubTestIo.obj(HubTestIo.json(TYPE));
        Set<String> required = Set.of("ultrawarm", "natural", "coordinate_scale", "has_skylight", "has_ceiling", "ambient_light", "monster_spawn_light_level",
                "monster_spawn_block_light_limit", "piglin_safe", "bed_works", "respawn_anchor_works", "has_raids", "logical_height", "min_y", "height", "infiniburn", "effects");
        assertEquals(required, t.keySet(), "exactly the fields of the 1.21.1 dimension type (fixed_time is optional and not used: the Hub keeps the Overworld's day)");
        for (String k : new String[]{"ultrawarm", "natural", "has_skylight", "has_ceiling", "piglin_safe", "bed_works", "respawn_anchor_works", "has_raids"}) {
            assertTrue(t.get(k) instanceof Boolean, k + " is a boolean");
        }
        int minY = ((Double) t.get("min_y")).intValue(), height = ((Double) t.get("height")).intValue(), logical = ((Double) t.get("logical_height")).intValue();
        assertEquals(-64, minY);
        assertEquals(384, height);
        assertEquals(384, logical);
        assertEquals(0, minY % 16, "min_y is a multiple of 16");
        assertEquals(0, height % 16, "height is a multiple of 16");
        assertTrue(height >= 16 && logical <= height && minY + height <= 2032, "the codec's range checks");
        assertTrue(t.get("infiniburn").toString().startsWith("#"), "a tag reference needs the #");
        assertEquals("minecraft:overworld", t.get("effects"));
        assertEquals(Boolean.TRUE, t.get("has_skylight"), "a normal sky");
        assertEquals(Boolean.FALSE, t.get("has_ceiling"));
        assertEquals(Boolean.TRUE, t.get("bed_works"), "players sleep in the base they build");
        assertEquals(Boolean.FALSE, t.get("has_raids"));
        double ambient = (Double) t.get("ambient_light");
        assertTrue(ambient >= 0.0 && ambient <= 1.0);
        double scale = (Double) t.get("coordinate_scale");
        assertTrue(scale >= 1.0E-5 && scale <= 3.0E7);
        // the layout fits the type: the highest gate plane (4 high) under the build limit, the deepest gate above the bottom
        assertTrue(HubLayout.HOLY_GATE_Y + 4 <= minY + height - 1, "the Holy gate fits under the build limit");
        assertTrue(HubLayout.ABYSS_GATE_Y - 2 >= minY, "the Abyssal gate fits above the bottom");
        assertTrue(HubLayout.SURFACE_Y - HubLayout.CHASM_DEPTH >= minY, "the chasm floor is above the bottom");
        assertEquals(HubPlazaLayout.FLOOR_ROW + 1, HubLayout.SURFACE_Y - HubPlazaLayout.originY(), "the standing level is the row above the paving");
    }

    @Test
    void theBiomeIsAPlainsCloneWithOnlyGentleCreaturesAndVegetation() throws IOException {
        Map<String, Object> b = HubTestIo.obj(HubTestIo.json(BIOME));
        for (String k : new String[]{"has_precipitation", "temperature", "downfall", "effects", "spawners", "spawn_costs", "carvers", "features"}) assertTrue(b.containsKey(k), "biome field " + k);
        assertEquals(0.8, (Double) b.get("temperature"), 1.0E-9, "vanilla plains");
        assertEquals(0.4, (Double) b.get("downfall"), 1.0E-9, "vanilla plains");
        assertEquals(Boolean.TRUE, b.get("has_precipitation"));
        Map<String, Object> fx = HubTestIo.obj(b.get("effects"));
        for (String k : new String[]{"fog_color", "sky_color", "water_color", "water_fog_color"}) assertTrue(fx.get(k) instanceof Double, "effects." + k + " is a number");
        assertEquals(12638463.0, fx.get("fog_color"), "the plains' fog");
        assertEquals(7907327.0, fx.get("sky_color"), "the plains' sky");
        assertEquals(4159204.0, fx.get("water_color"), "the plains' water");
        assertEquals(329011.0, fx.get("water_fog_color"));
        double probability = (Double) b.get("creature_spawn_probability");
        assertTrue(probability >= 0.0 && probability < 1.0, "the codec's float range");
        assertTrue(HubTestIo.obj(b.get("carvers")).isEmpty(), "no carvers: nothing is cut into the plains");
        assertTrue(HubTestIo.obj(b.get("spawn_costs")).isEmpty());
        Map<String, Object> sp = HubTestIo.obj(b.get("spawners"));
        Set<String> categories = Set.of("monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature", "water_ambient", "misc");
        assertEquals(categories, sp.keySet(), "every category is listed (vanilla lists them all)");
        for (String c : categories) {
            if (!c.equals("creature")) assertTrue(HubTestIo.arr(sp.get(c)).isEmpty(), "no " + c + " spawner: only passive land creatures live here");
        }
        List<Object> creatures = HubTestIo.arr(sp.get("creature"));
        assertFalse(creatures.isEmpty());
        Set<String> animals = animals();
        Set<String> seen = new HashSet<>();
        for (Object o : creatures) {
            Map<String, Object> e = HubTestIo.obj(o);
            assertEquals(Set.of("type", "weight", "minCount", "maxCount"), e.keySet());
            String type = (String) e.get("type");
            assertTrue(animals.contains(type), type + " is a vanilla animal of the API dump");
            assertTrue(seen.add(type), "duplicate spawner " + type);
            int w = ((Double) e.get("weight")).intValue(), lo = ((Double) e.get("minCount")).intValue(), hi = ((Double) e.get("maxCount")).intValue();
            assertTrue(w >= 1 && lo >= 1 && hi >= lo, "weights and counts of " + type);
        }
        assertTrue(seen.containsAll(Set.of("minecraft:sheep", "minecraft:cow", "minecraft:pig", "minecraft:chicken", "minecraft:horse", "minecraft:donkey", "minecraft:rabbit")), "the creatures the brief names");
        for (String hostile : new String[]{"zombie", "skeleton", "creeper", "spider", "enderman", "witch", "slime", "bat", "drowned"}) {
            assertFalse(seen.contains("minecraft:" + hostile), hostile + " must not be listed");
        }
    }

    @Test
    void theBiomesFeaturesAreAShortListOfVegetationOnTheVegetalStep() throws IOException {
        Map<String, Object> b = HubTestIo.obj(HubTestIo.json(BIOME));
        List<Object> steps = HubTestIo.arr(b.get("features"));
        assertTrue(steps.size() <= 11, "there are eleven decoration steps");
        int vegetal = 9;
        int total = 0;
        for (int i = 0; i < steps.size(); i++) {
            List<Object> step = HubTestIo.arr(steps.get(i));
            if (i != vegetal) assertTrue(step.isEmpty(), "step " + i + " is empty: only vegetation is placed, and vegetation belongs to step 9 (VEGETAL_DECORATION)");
            for (Object f : step) {
                total++;
                assertTrue(KNOWN_FEATURES.contains(f), f + " is an id seen in real 1.21.1 biome data");
            }
        }
        assertTrue(steps.size() > vegetal, "the vegetal step is present");
        assertTrue(total >= 3 && total <= 6, "a short list: " + total);
        assertTrue(HubTestIo.arr(steps.get(vegetal)).contains("minecraft:trees_plains"), "a few trees");
        assertTrue(HubTestIo.arr(steps.get(vegetal)).contains("minecraft:flower_plains"), "plains flowers");
        assertTrue(HubTestIo.arr(steps.get(vegetal)).contains("minecraft:patch_grass_plain"), "grass patches");
    }

    @Test
    void theBiomeModifierNamesItsSerializerAndTheSanctuaryBiomeOnly() throws IOException {
        Map<String, Object> m = HubTestIo.obj(HubTestIo.json(MODIFIER));
        assertEquals(Set.of("type", "biomes", "keep_namespaces"), m.keySet());
        assertEquals("nusmp:sanctuary_peace", m.get("type"), "the id SanctuaryPeaceModifier registers");
        assertEquals("nusmp:sanctuary_plains", m.get("biomes"), "this biome and no other: a modifier on every biome would strip the Overworld");
        assertEquals(List.of("minecraft"), m.get("keep_namespaces"));
    }

    @Test
    void theSerializerIdTheCodecFieldsAndTheFileNamesMatchTheJava() throws IOException {
        String src = HubTestIo.read(Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/mega/hub/SanctuaryPeaceModifier.java"));
        assertTrue(src.contains("register(\"sanctuary_peace\""), "registered as sanctuary_peace");
        assertTrue(src.contains("fieldOf(\"biomes\")") && src.contains("optionalFieldOf(\"keep_namespaces\""), "the codec reads the fields the JSON writes");
        assertTrue(Arrays.asList("dimension/sanctuary.json", "dimension_type/sanctuary.json", "worldgen/biome/sanctuary_plains.json", "neoforge/biome_modifier/sanctuary_peace.json").stream()
                .allMatch(f -> Files.exists(HubTestIo.DATA.resolve(f))), "all four files exist");
        assertNotNull(HubLayout.GateKind.valueOf("HOME"));
    }
}
