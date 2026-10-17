package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the guard builder's blocks and item are drawn by resource files that no Java compiler checks, and a missing model or texture shows in
 * game as the purple-and-black cube or an invisible core. This test reads the files (no game needed) and fails when the sector door state
 * has no model, a model names a texture that does not exist, the altar or the Mana Core lacks a piece of its look, or the Mana Core is
 * missing from the class table and the Better Combat manifest that decide how a held item is sized and whether it swings.
 */
class GuardAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static boolean exists(String path) { return new File(path).isFile(); }

    /** {@code nusmp:block/x} to the file of that model; vanilla ids resolve to null (they are the game's). */
    private static String modelFile(String id) {
        if (!id.startsWith("nusmp:")) return null;
        return ASSETS + "models/" + id.substring(6) + ".json";
    }

    private static String textureFile(String id) {
        if (!id.startsWith("nusmp:")) return null;
        return ASSETS + "textures/" + id.substring(6) + ".png";
    }

    /** Every texture a model names (the values of its "textures" object that are not references to another slot). */
    private static List<String> texturesOf(JsonObject model) {
        List<String> out = new ArrayList<>();
        if (!model.has("textures")) return out;
        for (Map.Entry<String, JsonElement> e : model.getAsJsonObject("textures").entrySet()) {
            String v = e.getValue().getAsString();
            if (!v.startsWith("#")) out.add(v);
        }
        return out;
    }

    private static void assertModelComplete(String modelId) throws Exception {
        String file = modelFile(modelId);
        if (file == null) return;
        assertTrue(exists(file), "model " + modelId + " is missing (" + file + ")");
        JsonObject model = json(file);
        for (String tex : texturesOf(model)) {
            String tf = textureFile(tex);
            if (tf != null) assertTrue(exists(tf), "model " + modelId + " names the texture " + tex + ", which is missing (" + tf + ")");
        }
        if (model.has("parent")) assertModelComplete(model.get("parent").getAsString());
    }

    // ---------------------------------------------------------------- the sector door
    @Test
    void theGatekeeperDoorHasAModelForEveryStateOfKindSector() throws Exception {
        JsonObject variants = json(ASSETS + "blockstates/gatekeeper_door.json").getAsJsonObject("variants");
        for (String open : new String[] {"false", "true"}) {
            for (String half : new String[] {"lower", "upper"}) {
                String key = "kind=sector,open=" + open + ",half=" + half;
                assertTrue(variants.has(key), "the blockstate has no variant " + key);
                assertModelComplete(variants.getAsJsonObject(key).get("model").getAsString());
            }
        }
        // the three older kinds are still there, unchanged in number (the owner's rule: keep the old)
        for (String kind : new String[] {"warden", "boss", "puzzle"}) {
            for (String open : new String[] {"false", "true"}) {
                for (String half : new String[] {"lower", "upper"}) {
                    assertTrue(variants.has("kind=" + kind + ",open=" + open + ",half=" + half), "the older door state kind=" + kind + " lost a variant");
                }
            }
        }
        assertEquals(16, variants.size(), "4 kinds x open x half");
    }

    @Test
    void theClosedSectorDoorHasItsOwnFaceAndTheLowerHalfIsAnimated() throws Exception {
        JsonObject variants = json(ASSETS + "blockstates/gatekeeper_door.json").getAsJsonObject("variants");
        String lower = variants.getAsJsonObject("kind=sector,open=false,half=lower").get("model").getAsString();
        String upper = variants.getAsJsonObject("kind=sector,open=false,half=upper").get("model").getAsString();
        assertTrue(!lower.equals(upper), "the two halves of the door show two different parts of the face");
        for (String other : new String[] {"warden", "boss", "puzzle"}) {
            assertTrue(!lower.equals(variants.getAsJsonObject("kind=" + other + ",open=false,half=lower").get("model").getAsString()), "the sector door must not look like the " + other + " door");
        }
        BufferedImage bottom = ImageIO.read(new File(ASSETS + "textures/block/gatekeeper_door_sector_bottom.png"));
        BufferedImage top = ImageIO.read(new File(ASSETS + "textures/block/gatekeeper_door_sector_top.png"));
        assertEquals(16, bottom.getWidth());
        assertEquals(0, bottom.getHeight() % 16, "an animation strip is a whole number of frames");
        assertTrue(bottom.getHeight() >= 32, "the lower half is animated (a strip of frames)");
        assertTrue(exists(ASSETS + "textures/block/gatekeeper_door_sector_bottom.png.mcmeta"), "the animated strip needs its mcmeta");
        assertEquals(16, top.getWidth());
        assertEquals(16, top.getHeight());
    }

    // ---------------------------------------------------------------- the altar
    @Test
    void theGuardianAltarHasABlockstateAModelAnItemModelAndItsTextures() throws Exception {
        JsonObject state = json(ASSETS + "blockstates/guardian_altar.json").getAsJsonObject("variants");
        assertTrue(state.has(""), "a block without properties has one empty variant");
        assertModelComplete(state.getAsJsonObject("").get("model").getAsString());
        assertModelComplete("nusmp:item/guardian_altar");
        for (String t : new String[] {"side", "top", "pillar"}) {
            BufferedImage img = ImageIO.read(new File(ASSETS + "textures/block/guardian_altar_" + t + ".png"));
            assertEquals(16, img.getWidth(), t);
            assertEquals(16, img.getHeight(), t);
        }
    }

    // ---------------------------------------------------------------- the Mana Core
    @Test
    void theManaCoreIsAThreeDimensionalItemWithItsIconItsMeshAndItsSkins() throws Exception {
        JsonObject held = json(ASSETS + "models/item/mana_core.json");
        assertEquals("minecraft:builtin/entity", held.get("parent").getAsString(), "the in-hand model is drawn by the weapon renderer");
        assertTrue(held.getAsJsonObject("display").has("thirdperson_righthand") && held.getAsJsonObject("display").has("firstperson_righthand"), "the hand transforms are written");
        assertModelComplete("nusmp:item/mana_core_icon");
        BufferedImage icon = ImageIO.read(new File(ASSETS + "textures/item/mana_core.png"));
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
        assertTrue(exists(ASSETS + "weapon_meshes/mana_core.txt"), "the mesh");
        assertTrue(exists(ASSETS + "textures/entity/weapon/mana_core.png"), "the body skin");
        assertTrue(exists(ASSETS + "textures/entity/weapon/mana_core_glow.png"), "the glow skin");
    }

    @Test
    void theManaCoreIsAHandItemInBothClassTablesAndNotAMeleeWeapon() throws Exception {
        JsonElement game = json(ASSETS + "weapon_classes.json").getAsJsonObject("weapons").get("mana_core");
        assertNotNull(game, "the game's class table names the Mana Core");
        assertEquals("handitem", game.getAsString());
        JsonObject tools = json("tools/relics/weapon_classes.json").getAsJsonObject("weapons");
        JsonElement entry = tools.get("mana_core");
        assertNotNull(entry, "the tools copy of the class table names the Mana Core");
        assertEquals("handitem", entry.isJsonObject() ? entry.getAsJsonObject().get("class").getAsString() : entry.getAsString());
        JsonObject manifest = json("tools/bettercombat/manifest.json");
        assertTrue(manifest.getAsJsonObject("excluded").has("mana_core"), "Better Combat must not give a key a swing");
        assertFalse(manifest.getAsJsonObject("weapons").has("mana_core"));
    }

    @Test
    void theWeaponRendererDrawsTheManaCore() throws Exception {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/newuniverse/nusmp/client/WeaponRenderer.java"));
        assertTrue(src.contains("GuardRegistry.MANA_CORE"), "WeaponRenderer.BASE_WEAPONS lists the Mana Core, or the held model is never drawn");
    }
}
