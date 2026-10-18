package com.newuniverse.nusmp.elite.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.world.rules.CrystalStage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The files of the elite world engine hold together: every JSON parses, every blockstate points at a model, every model at a texture that exists, every GeoLite model has its texture, glow
 * and the clips its renderer asks for, every key the Java code translates is in the lang fragment, and the stage ids of the growth rules are the ids of the blockstates. Reads the source tree
 * (the working directory of a test run is the project directory).
 */
class WorldAssetsTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path ASSETS = RES.resolve("assets/nusmp");
    private static final Path DATA = RES.resolve("data/nusmp");

    private static JsonObject json(Path p) throws Exception {
        assertTrue(Files.exists(p), "missing file " + p);
        JsonElement e = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8));
        assertTrue(e.isJsonObject(), p + " is a JSON object");
        return e.getAsJsonObject();
    }

    private static Path textureOf(String ref) {
        String path = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        return ASSETS.resolve("textures").resolve(path + ".png");
    }

    private static Path modelOf(String ref) {
        String path = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        return ASSETS.resolve("models").resolve(path + ".json");
    }

    @Test
    void everyCrystalStageHasABlockstateModelTextureLootAndItemModel() throws Exception {
        for (CrystalStage s : CrystalStage.values()) {
            JsonObject state = json(ASSETS.resolve("blockstates/" + s.blockId + ".json"));
            assertTrue(state.has("variants"), s.blockId + " has variants");
            Set<String> models = new HashSet<>();
            for (var e : state.getAsJsonObject("variants").entrySet()) models.add(e.getValue().getAsJsonObject().get("model").getAsString());
            assertFalse(models.isEmpty());
            for (String m : models) {
                JsonObject model = json(modelOf(m));
                for (var t : model.getAsJsonObject("textures").entrySet()) assertTrue(Files.exists(textureOf(t.getValue().getAsString())), s.blockId + " texture " + t.getValue());
            }
            assertTrue(Files.exists(modelOf("nusmp:item/" + s.blockId)), "item model of " + s.blockId);
            JsonObject loot = json(DATA.resolve("loot_table/blocks/" + s.blockId + ".json"));
            assertEquals("minecraft:block", loot.get("type").getAsString());
        }
        JsonObject mother = json(ASSETS.resolve("blockstates/mother_rock.json")).getAsJsonObject("variants");
        for (int c = 0; c <= 3; c++) assertTrue(mother.has("charge=" + c), "mother rock charge " + c);
        JsonObject bud = json(ASSETS.resolve("blockstates/magic_bud_small.json")).getAsJsonObject("variants");
        for (String f : List.of("up", "down", "north", "south", "east", "west")) assertTrue(bud.has("facing=" + f), "facing " + f);
    }

    @Test
    void theHandItemsAreRealThreeDModelsWithTheirPalette() throws Exception {
        for (String n : List.of("magicule_gauge", "calamity_core")) {
            JsonObject m = json(ASSETS.resolve("models/item/" + n + ".json"));
            assertTrue(m.getAsJsonArray("elements").size() >= 8, n + " is a model of real cubes");
            assertTrue(m.has("display") && m.getAsJsonObject("display").has("thirdperson_righthand") && m.getAsJsonObject("display").has("firstperson_righthand"), n + " has hand transforms");
            assertTrue(Files.exists(textureOf(m.getAsJsonObject("textures").get("p").getAsString())), n + " palette");
            for (JsonElement e : m.getAsJsonArray("elements")) {
                JsonObject faces = e.getAsJsonObject().getAsJsonObject("faces");
                assertEquals(6, faces.size());
                for (var f : faces.entrySet()) {
                    var uv = f.getValue().getAsJsonObject().getAsJsonArray("uv");
                    for (JsonElement v : uv) assertTrue(v.getAsDouble() >= 0 && v.getAsDouble() <= 16, "uv inside the 16 px palette");
                }
            }
        }
        assertTrue(Files.exists(ASSETS.resolve("textures/item/magicule_shard.png")));
        json(ASSETS.resolve("models/item/magicule_shard.json"));
    }

    @Test
    void geoModelsHaveTexturesGlowAndTheClipsTheirRenderersAsk() throws Exception {
        String[][] models = {
                {"gravebound_colossus", "idle walk windup cast strike transform death"},
                {"lich", "idle walk windup cast strike transform death"},
                {"phylactery", "idle"},
                {"gaia_spike", "rise sink"},
                {"void_bolt", "spin"}};
        for (String[] m : models) {
            String id = "elite_" + m[0];
            JsonObject geo = json(ASSETS.resolve("geo/entity/" + id + ".geo.json"));
            JsonObject g = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            Set<String> bones = new HashSet<>();
            for (JsonElement b : g.getAsJsonArray("bones")) bones.add(b.getAsJsonObject().get("name").getAsString());
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + id + ".png")), id + " texture");
            assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + id + "_glow.png")), id + " glow");
            JsonObject anims = json(ASSETS.resolve("animations/entity/" + id + ".animation.json")).getAsJsonObject("animations");
            for (String clip : m[1].split(" ")) {
                assertTrue(anims.has(clip), id + " has the clip " + clip);
                for (String bone : anims.getAsJsonObject(clip).getAsJsonObject("bones").keySet()) assertTrue(bones.contains(bone), id + "/" + clip + " animates the unknown bone " + bone);
            }
            for (String clip : anims.keySet()) assertTrue(anims.getAsJsonObject(clip).get("animation_length").getAsDouble() > 0.0, id + "/" + clip + " has a length");
        }
        JsonObject spike = json(ASSETS.resolve("animations/entity/elite_gaia_spike.animation.json")).getAsJsonObject("animations");
        assertFalse(spike.getAsJsonObject("rise").get("loop").getAsBoolean(), "the rise plays once");
        JsonObject colossus = json(ASSETS.resolve("animations/entity/elite_gravebound_colossus.animation.json")).getAsJsonObject("animations");
        assertTrue(colossus.getAsJsonObject("idle").get("loop").getAsBoolean() && colossus.getAsJsonObject("walk").get("loop").getAsBoolean());
        assertFalse(colossus.getAsJsonObject("windup").get("loop").getAsBoolean(), "the windup is held on its last frame, not looped");
    }

    @Test
    void everyTranslatedKeyOfTheCodeIsInTheLangFragment() throws Exception {
        JsonObject lang = json(Path.of("tools/elite/world_lang.json"));
        Pattern literal = Pattern.compile("translatable(?:WithFallback)?\\(\\s*\"([a-z0-9_.]+)\"");
        Pattern prefixed = Pattern.compile("translatable(?:WithFallback)?\\(\\s*\"([a-z0-9_.]+\\.)\"\\s*\\+");
        Set<String> missing = new HashSet<>();
        try (Stream<Path> files = Stream.concat(Files.walk(Path.of("src/main/java/com/newuniverse/nusmp/elite/world")), Files.walk(Path.of("src/main/java/com/newuniverse/nusmp/client/elite")))) {
            for (Path p : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String src = Files.readString(p, StandardCharsets.UTF_8);
                Matcher m = literal.matcher(src);
                while (m.find()) {
                    String k = m.group(1);
                    if (k.endsWith(".")) continue;                       // a prefix: checked below
                    if (!lang.has(k) && !src.contains("translatableWithFallback(\"" + k + "\"")) missing.add(k + " (" + p.getFileName() + ")");
                }
                Matcher pm = prefixed.matcher(src);
                while (pm.find()) {
                    String prefix = pm.group(1);
                    boolean any = lang.keySet().stream().anyMatch(k -> k.startsWith(prefix));
                    boolean fallback = src.contains("translatableWithFallback(\"" + prefix);
                    if (!any && !fallback) missing.add(prefix + "* (" + p.getFileName() + ")");
                }
            }
        }
        assertTrue(missing.isEmpty(), "keys without a lang entry: " + missing);
        for (CrystalStage s : CrystalStage.values()) assertTrue(lang.has((s == CrystalStage.MOTHER_ROCK ? "block.nusmp." : "block.nusmp.") + s.blockId), "block name of " + s.blockId);
        for (String e : List.of("gravebound_colossus", "lich_boss", "soul_phylactery", "gaia_spike", "void_bolt")) assertTrue(lang.has("entity.nusmp." + e), "entity name of " + e);
        for (String c : List.of("gravebound_colossus", "lich_calamity")) for (String kind : List.of("omen", "spawn", "phase", "defeat", "core")) assertTrue(lang.has("nusmp.elite.calamity." + c + "." + kind), c + "." + kind);
    }

    @Test
    void theShippedCalamitiesAndTheLootAreCoherent() throws Exception {
        for (String c : List.of("gravebound_colossus", "lich_calamity")) {
            JsonObject d = json(DATA.resolve("elite/world/calamities/" + c + ".json"));
            assertEquals(c, d.get("id").getAsString());
            assertTrue(d.get("entity").getAsString().startsWith("nusmp:"));
            assertTrue(d.getAsJsonArray("phases").size() == 2, "two thresholds, three phases");
        }
        JsonObject pick = json(RES.resolve("data/minecraft/tags/block/mineable/pickaxe.json"));
        String all = pick.getAsJsonArray("values").toString();
        for (CrystalStage s : CrystalStage.values()) assertTrue(all.contains("nusmp:" + s.blockId), s.blockId + " is mined with a pickaxe");
        JsonObject recipe = json(DATA.resolve("recipe/magicule_gauge.json"));
        assertEquals("nusmp:magicule_gauge", recipe.getAsJsonObject("result").get("id").getAsString());
    }
}
