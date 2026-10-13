package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The resource files of the Dungeon Delving Gear agree with the Java: the model overrides read the item properties the way FlameMode and
 * the compass frames say, every sprite and mesh they name exists, the tags list only optional entries of other mods, the recipe and the
 * carcass blockstate cover what the code can produce.
 */
class NavAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final String DATA = "src/main/resources/data/nusmp/";

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static boolean exists(String path) { return new File(path).isFile(); }

    /** "nusmp:item/x" -> the model file of it. */
    private static String modelFile(String id) { return ASSETS + "models/" + id.substring(id.indexOf(':') + 1) + ".json"; }

    /**
     * The model that vanilla's item overrides pick for a property value: the last override whose threshold the value reaches, or null for
     * the base model. The game compares floats with floats, so this does too.
     */
    private static String pick(JsonObject model, String property, float value) {
        String picked = null;
        for (JsonElement e : model.getAsJsonArray("overrides")) {
            JsonObject o = e.getAsJsonObject();
            if (value >= o.getAsJsonObject("predicate").get(property).getAsFloat()) picked = o.get("model").getAsString();
        }
        return picked;
    }

    private static String sprite(JsonObject model) { return model.getAsJsonObject("textures").get("layer0").getAsString(); }

    // ---------------------------------------------------------------- the lantern
    @Test
    void theLanternIconShowsTheSpriteOfTheFlameThatBurns() throws Exception {
        JsonObject icon = json(ASSETS + "models/item/ki_resonance_lantern_icon.json");
        for (int i = 0; i <= 1000; i++) {
            float value = i / 1000f;
            FlameMode mode = FlameMode.ofProperty(value);
            String picked = pick(icon, "nusmp:flame", value);
            String shown = sprite(picked == null ? icon : json(modelFile(picked)));
            assertEquals("nusmp:item/" + mode.meshId(), shown, "flame value " + value);
        }
        for (FlameMode m : FlameMode.values()) {
            assertTrue(exists(ASSETS + "textures/item/" + m.meshId() + ".png"), "sprite of " + m);
            assertTrue(exists(ASSETS + "weapon_meshes/" + m.meshId() + ".txt"), "mesh of " + m);
            assertTrue(exists(ASSETS + "textures/entity/weapon/" + m.meshId() + ".png"), "skin of " + m);
        }
    }

    @Test
    void theLanternIsAnInHandMeshWithItsOwnIconModel() throws Exception {
        JsonObject marker = json(ASSETS + "models/item/ki_resonance_lantern.json");
        assertEquals("minecraft:builtin/entity", marker.get("parent").getAsString());
        JsonObject icon = json(ASSETS + "models/item/ki_resonance_lantern_icon.json");
        assertEquals(3, icon.getAsJsonArray("overrides").size(), "amber, blue and red; out is the base model");
        for (JsonElement e : icon.getAsJsonArray("overrides")) {
            JsonObject o = e.getAsJsonObject();
            assertTrue(o.getAsJsonObject("predicate").has("nusmp:flame"));
            assertTrue(exists(modelFile(o.get("model").getAsString())), o.get("model").getAsString());
        }
    }

    // ---------------------------------------------------------------- the compass
    @Test
    void theCompassIconTurnsItsNeedleLikeVanillasCompass() throws Exception {
        JsonObject icon = json(ASSETS + "models/item/anti_bird_compass_icon.json");
        assertEquals(33, icon.getAsJsonArray("overrides").size(), "32 frames, and the first one again for the last half step");
        for (int i = 0; i < 1000; i++) {
            float angle = i / 1000f;
            int frame = Math.floorMod((int) Math.floor(angle * 32.0 + 0.5), 32);
            String picked = pick(icon, "nusmp:angle", angle);
            assertEquals(String.format("nusmp:item/anti_bird_compass_%02d", frame), picked, "needle " + angle);
        }
        Set<String> frames = new HashSet<>();
        for (int k = 0; k < 32; k++) {
            String name = String.format("anti_bird_compass_%02d", k);
            assertEquals("nusmp:item/" + name, sprite(json(ASSETS + "models/item/" + name + ".json")));
            assertTrue(exists(ASSETS + "textures/item/" + name + ".png"), name);
            assertTrue(frames.add(name));
        }
        assertEquals(32, frames.size());
        assertTrue(exists(ASSETS + "weapon_meshes/anti_bird_compass.txt"));
        assertTrue(exists(ASSETS + "textures/entity/weapon/anti_bird_compass.png"));
        assertEquals("minecraft:builtin/entity", json(ASSETS + "models/item/anti_bird_compass.json").get("parent").getAsString());
    }

    // ---------------------------------------------------------------- the venom gland
    @Test
    void theVenomGlandHasItsFlatModelAndASixteenPixelSprite() throws Exception {
        JsonObject model = json(ASSETS + "models/item/venom_gland.json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals("nusmp:item/venom_gland", sprite(model));
        BufferedImage img = ImageIO.read(new File(ASSETS + "textures/item/venom_gland.png"));
        assertEquals(16, img.getWidth());
        assertEquals(16, img.getHeight());
        int opaque = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((img.getRGB(x, y) >>> 24) != 0) opaque++;
        assertTrue(opaque > 60 && opaque < 230, "a drawn sprite, not a blank or a filled square: " + opaque);
    }

    // ---------------------------------------------------------------- tags and recipe
    @Test
    void theModdedEntriesOfTheTagsAreAllOptional() throws Exception {
        for (String tag : new String[]{"tags/item/lantern_fuel.json", "tags/entity_type/dungeon/avian.json"}) {
            JsonObject t = json(DATA + tag);
            assertFalse(t.get("replace").getAsBoolean(), tag);
            Set<String> ids = new HashSet<>();
            for (JsonElement e : t.getAsJsonArray("values")) {
                JsonObject v = e.getAsJsonObject();
                assertFalse(v.get("required").getAsBoolean(), tag + " " + v);
                assertTrue(ids.add(v.get("id").getAsString()), "listed once: " + v);
            }
            if (tag.contains("lantern_fuel")) {
                assertTrue(ids.containsAll(Set.of("tensura:magic_stone", "tensura:magic_ore_shard", "tensura:low_quality_magic_crystal",
                        "minecraft:slime_ball", "tensura:chilled_slime")), ids.toString());
                for (String id : ids) assertTrue(LanternFuel.ticksFor(id) > 0, id);
            } else {
                assertTrue(ids.containsAll(Set.of("tensura:dragon_peacock", "tensura:one_eyed_owl", "tensura:giant_bat",
                        "iceandfire:stymphalian_bird", "iceandfire:hippogryph")), ids.toString());
            }
        }
    }

    @Test
    void theLanternIsCraftedFromIronGlassAndASlimeCore() throws Exception {
        JsonObject recipe = json(DATA + "recipe/ki_resonance_lantern.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("nusmp:ki_resonance_lantern", recipe.getAsJsonObject("result").get("id").getAsString());
        JsonObject key = recipe.getAsJsonObject("key");
        Set<Character> used = new HashSet<>();
        for (JsonElement row : recipe.getAsJsonArray("pattern")) {
            assertEquals(3, row.getAsString().length());
            for (char c : row.getAsString().toCharArray()) if (c != ' ') used.add(c);
        }
        for (char c : used) assertTrue(key.has(String.valueOf(c)), "key for " + c);
        assertEquals(used.size(), key.size(), "no unused key");
        for (String k : key.keySet()) {
            JsonObject ing = key.getAsJsonObject(k);
            if (ing.has("tag")) {
                String tag = ing.get("tag").getAsString();
                assertTrue(tag.startsWith("nusmp:"), "a tag of this mod, so it is certain to exist: " + tag);
                assertTrue(exists(DATA + "tags/item/" + tag.substring("nusmp:".length()) + ".json"), tag);
            }
        }
        JsonObject cores = json(DATA + "tags/item/lantern_cores.json");
        Set<String> ids = new HashSet<>();
        for (JsonElement e : cores.getAsJsonArray("values")) ids.add(e.getAsJsonObject().get("id").getAsString());
        assertTrue(ids.contains("minecraft:slime_ball") && ids.contains("tensura:chilled_slime"), ids.toString());
    }

    // ---------------------------------------------------------------- the carcass
    @Test
    void everyKindOfCarcassTheOrgansKnowHasAModel() throws Exception {
        JsonObject variants = json(ASSETS + "blockstates/carcass.json").getAsJsonObject("variants");
        for (int kind = 0; kind <= OrganRules.ARACHNID; kind++) {
            assertTrue(variants.has("kind=" + kind), "kind " + kind);
            assertTrue(exists(modelFile(variants.getAsJsonObject("kind=" + kind).get("model").getAsString())), "kind " + kind);
        }
        assertEquals(OrganRules.ARACHNID + 1, variants.size(), "the blockstate has no kind that the organs do not know");
    }
}
