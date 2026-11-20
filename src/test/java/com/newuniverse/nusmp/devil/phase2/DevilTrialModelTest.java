package com.newuniverse.nusmp.devil.phase2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.WorldDevil;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the trial devil's own model (docs/devil_trial_model.md) is data plus a renderer nobody can see in a game here, so the files that make it are pinned: the geometry is well formed and within its draw budget,
 * every devil has a painted sheet and a glow mask of the declared size, the clips the entity plays exist and name real bones, the Blockbench-format source matches the game file, the client option is a
 * client config that is on by default with the old stand-in one switch away, and the entity keeps its server rules. Pure (file reads, no game); the working directory is the project root.
 */
class DevilTrialModelTest {
    private static final File ASSETS = new File("src/main/resources/assets/anime_multiverse");
    private static final String ROOT = "src/main/java/com/newuniverse/nusmp/";

    private static JsonObject json(File f) throws Exception { return JsonParser.parseString(Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject(); }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    private static JsonObject geo() throws Exception { return json(new File(ASSETS, "geo/entity/devil_trial.geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject(); }

    @Test
    void theGeometryIsWellFormedAndWithinItsBudget() throws Exception {
        JsonObject g = geo();
        JsonObject desc = g.getAsJsonObject("description");
        int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
        assertEquals(256, tw);
        assertEquals(256, th);
        Set<String> names = new HashSet<>();
        int cubes = 0;
        JsonArray bones = g.getAsJsonArray("bones");
        for (JsonElement e : bones) names.add(e.getAsJsonObject().get("name").getAsString());
        assertEquals(bones.size(), names.size(), "bone names are unique");
        for (JsonElement e : bones) {
            JsonObject b = e.getAsJsonObject();
            if (b.has("parent")) assertTrue(names.contains(b.get("parent").getAsString()), b.get("name") + " has an unknown parent");
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                cubes++;
                JsonObject uvs = ce.getAsJsonObject().getAsJsonObject("uv");
                for (String face : new String[]{"north", "south", "east", "west", "up", "down"}) {
                    JsonObject f = uvs.getAsJsonObject(face);
                    assertNotNull(f, "a cube lacks the uv of " + face);
                    double u = f.getAsJsonArray("uv").get(0).getAsDouble(), v = f.getAsJsonArray("uv").get(1).getAsDouble();
                    double w = f.getAsJsonArray("uv_size").get(0).getAsDouble(), h = f.getAsJsonArray("uv_size").get(1).getAsDouble();
                    assertTrue(u >= 0 && v >= 0 && u + w <= tw && v + h <= th, "uv outside the sheet in " + b.get("name"));
                }
            }
        }
        assertTrue(cubes >= 60 && cubes <= 160, "the model's cube count is bounded (60..160), it is " + cubes);
        assertTrue(cubes * 6 <= 960, "at most 960 quads per devil (at most trial.max = 4 exist)");
        for (String must : new String[]{"root", "body", "chest", "head", "jaw", "arm_r", "arm_l", "arm_r_low", "arm_r_hand", "leg_r", "leg_l", "leg_r_low", "leg_r_foot", "wing_r", "wing_l", "wing_r_tip", "tail1", "tail2", "tail3", "horn_r", "horn_l", "lock_r", "lock_l"})
            assertTrue(names.contains(must), "the model lacks the bone " + must);
    }

    @Test
    void everyDevilHasAPaintedSheetAndAGlowMaskOfTheDeclaredSize() throws Exception {
        File tex = new File(ASSETS, "textures/entity");
        Set<String> stems = new HashSet<>();
        stems.add("devil_trial");
        for (int i = 0; i < WorldDevil.count(); i++) stems.add("devil_trial_" + WorldDevil.at(i).id());
        assertEquals(WorldDevil.count() + 1, stems.size());
        for (String stem : stems) {
            for (String suffix : new String[]{"", "_glowmask"}) {
                File f = new File(tex, stem + suffix + ".png");
                assertTrue(f.isFile(), "missing " + f.getName());
                BufferedImage img = ImageIO.read(f);
                assertEquals(256, img.getWidth(), f.getName());
                assertEquals(256, img.getHeight(), f.getName());
                if (suffix.isEmpty()) {
                    int opaque = 0;
                    for (int y = 0; y < 256; y += 3) for (int x = 0; x < 256; x += 3) if ((img.getRGB(x, y) >>> 24) > 0) opaque++;
                    assertTrue(opaque > 1500, f.getName() + " is mostly empty");
                } else {
                    int lit = 0;
                    for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) if ((img.getRGB(x, y) >>> 24) > 0) lit++;
                    assertTrue(lit > 60 && lit < 4000, f.getName() + " glow pixels " + lit + " (a few: eyes, brands, gems, horn tips)");
                }
            }
        }
    }

    @Test
    void theClipsTheEntityPlaysExistAndNameRealBones() throws Exception {
        JsonObject anims = json(new File(ASSETS, "animations/entity/devil_trial.animation.json")).getAsJsonObject("animations");
        Set<String> bones = new HashSet<>();
        for (JsonElement e : geo().getAsJsonArray("bones")) bones.add(e.getAsJsonObject().get("name").getAsString());
        String entity = read(ROOT + "devil/phase2/DevilTrialEntity.java");
        for (String clip : new String[]{"idle", "walk", "attack"}) {
            String name = "animation.devil_trial." + clip;
            assertTrue(anims.has(name), "the clip " + name + " is missing");
            assertTrue(entity.contains("\"" + name + "\""), "the entity does not play " + name);
            JsonObject c = anims.getAsJsonObject(name);
            assertTrue(c.get("animation_length").getAsDouble() > 0);
            for (String b : c.getAsJsonObject("bones").keySet()) assertTrue(bones.contains(b), name + " moves the unknown bone " + b);
        }
        assertTrue(anims.getAsJsonObject("animation.devil_trial.idle").get("loop").getAsBoolean(), "idle loops");
        assertTrue(anims.getAsJsonObject("animation.devil_trial.walk").get("loop").getAsBoolean(), "walk loops");
        assertFalse(anims.getAsJsonObject("animation.devil_trial.attack").get("loop").getAsBoolean(), "attack plays once");
    }

    @Test
    void theBlockbenchFormatSourceMatchesTheGameFile() throws Exception {
        File f = new File("tools/blockbench/devil_trial.bbmodel");
        assertTrue(f.isFile(), "the .bbmodel source is kept (rule 15)");
        JsonObject bb = json(f);
        int cubes = 0;
        for (JsonElement e : geo().getAsJsonArray("bones")) cubes += e.getAsJsonObject().getAsJsonArray("cubes").size();
        assertEquals(cubes, bb.getAsJsonArray("elements").size(), "the same cubes");
        assertEquals(1, bb.getAsJsonArray("textures").size());
        assertTrue(bb.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString().startsWith("data:image/png;base64,"), "the sheet is embedded");
        assertEquals(5, bb.getAsJsonArray("animations").size(), "idle, walk, attack and the rise and roar of the summoning figure");
        assertEquals("free", bb.getAsJsonObject("meta").get("model_format").getAsString());
        String gen = read("tools/blockbench/gen_devil_trial.py");
        assertTrue(gen.contains("Blockbench could not run") && gen.contains("FALLBACK") && gen.contains("NOT opened in Blockbench"), "the generator says plainly it is the fallback");
    }

    @Test
    void theOptionIsAClientConfigOnByDefaultWithTheOldStandInOneSwitchAway() throws Exception {
        String o = read(ROOT + "client/playermodel/PlayerModelOptions.java");
        assertTrue(o.contains(".define(\"devilTrialModel\", true)"), "default on");
        assertTrue(o.contains("public static boolean devilTrialModel()"));
        String r = read(ROOT + "client/devil/DevilTrialRenderer.java");
        assertTrue(r.contains("PlayerModelOptions.devilTrialModel()") && r.contains("textures/entity/zombie/zombie.png") && r.contains("HumanoidMobRenderer"), "the old stand-in is kept behind the option");
        String g = read(ROOT + "client/devil/DevilTrialGeoRenderer.java");
        assertTrue(g.contains("AutoGlowingGeoLayer") && g.contains("devil_trial.geo.json") && g.contains("devil_trial.animation.json"));
        assertFalse(g.contains("Minecraft.getInstance()"), "no static game access in the renderer");
        String render = g.substring(g.indexOf("public void render("));
        assertFalse(render.contains(" new "), "render allocates nothing per frame");
        assertTrue(read(ROOT + "client/devil/DevilClient.java").contains("DevilTrialRenderer::new"), "the renderer is registered");
        for (String k : new String[]{"anime_multiverse.configuration.devilTrialModel", "anime_multiverse.configuration.devilTrialModel.tooltip"}) {
            assertTrue(read("tools/lang/devil_trial.json").contains(k), k + " in the fragment");
            assertTrue(read("src/main/resources/assets/anime_multiverse/lang/en_us.json").contains("\"" + k + "\""), k + " in en_us.json");
        }
    }

    @Test
    void theEntityKeepsItsServerRulesAndSyncsOnlyTheDevilId() throws Exception {
        String e = read(ROOT + "devil/phase2/DevilTrialEntity.java");
        assertTrue(e.contains("implements GeoEntity") && e.contains("EntityDataSerializers.STRING"), "a GeckoLib entity with one synced string");
        assertEquals(1, e.split("SynchedEntityData\\.defineId", -1).length - 1, "only the devil id is synced");
        assertTrue(e.contains("shouldBeSaved() { return false; }") && e.contains("Trials.check(this)"), "never saved, still checked every second");
        assertFalse(e.contains("net.minecraft.client") || e.contains("com.newuniverse.nusmp.client") || e.contains("software.bernie.geckolib.renderer"), "no client class in the entity");
        assertFalse(e.contains("triggerAnim"), "no packet of its own: the attack clip follows the swing flag");
    }

    @Test
    void theBetrayalFloorIsAServerSettingWithTheOldBehaviourAsDefault() throws Exception {
        String c = read(ROOT + "devil/phase2/Phase2Config.java");
        assertTrue(c.contains("defineInRange(\"betrayalLeaves\", 2, 0, 20)"), "default 2 = the earlier floor of one heart");
        assertTrue(read(ROOT + "devil/phase2/Phase2.java").contains("q.getHealth() - Phase2Config.betrayalLeaves()"), "the strike reads the setting");
    }
}
