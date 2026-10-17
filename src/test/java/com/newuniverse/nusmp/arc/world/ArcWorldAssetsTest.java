package com.newuniverse.nusmp.arc.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Chronicle Sigil and the story arenas' dimension are drawn and declared by resource files that no Java compiler checks, and a missing model shows in
 * game as the purple-and-black cube or an invisible item, a missing dimension file as a story arena that is never loaded. This test reads the files (no game
 * needed) and fails when the sigil lacks its icon, its two models, its mesh and skins, its row in the class table (both copies) or the Better Combat manifest,
 * its place in the renderer's list, or when the arena's dimension and dimension type are missing or malformed.
 */
class ArcWorldAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final String DATA = "src/main/resources/data/nusmp/";

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void theSigilHasAnIconOfSixteenPixelsWithSomethingInIt() throws Exception {
        File f = new File(ASSETS + "textures/item/chronicle_sigil.png");
        assertTrue(f.isFile(), "the inventory icon is missing");
        BufferedImage img = ImageIO.read(f);
        assertEquals(16, img.getWidth());
        assertEquals(16, img.getHeight());
        int opaque = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((img.getRGB(x, y) >>> 24) > 128) opaque++;
        assertTrue(opaque > 80 && opaque < 230, "the icon should be a seal, not empty and not a full square: " + opaque + " opaque pixels");
    }

    @Test
    void theTwoModelsAreTheHandMeshAndTheFlatIcon() throws Exception {
        JsonObject hand = json(ASSETS + "models/item/chronicle_sigil.json");
        assertEquals("minecraft:builtin/entity", hand.get("parent").getAsString(), "the in-hand model is drawn by the weapon renderer");
        assertTrue(hand.has("display") && hand.getAsJsonObject("display").has("gui") && hand.getAsJsonObject("display").has("thirdperson_righthand") && hand.getAsJsonObject("display").has("firstperson_righthand"));
        JsonObject icon = json(ASSETS + "models/item/chronicle_sigil_icon.json");
        assertEquals("nusmp:item/chronicle_sigil", icon.getAsJsonObject("textures").get("layer0").getAsString(), "the flat model keeps the old sprite as the icon");
    }

    @Test
    void theMeshAndItsSkinsExist() throws Exception {
        Path mesh = Path.of(ASSETS + "weapon_meshes/chronicle_sigil.txt");
        assertTrue(Files.isRegularFile(mesh), "the in-hand mesh is missing: run tools/gen_weapon_models.py chronicle_sigil");
        String text = Files.readString(mesh);
        assertTrue(text.contains("\nplace "), "the mesh has no placement line");
        assertTrue(text.contains("\nglow "), "the sigil glows: the mesh has no glow line");
        long quads = text.lines().filter(l -> l.startsWith("q ")).count();
        assertTrue(quads >= 100, "only " + quads + " quads: the model is not what it should be");
        assertTrue(new File(ASSETS + "textures/entity/weapon/chronicle_sigil.png").isFile(), "the skin is missing");
        assertTrue(new File(ASSETS + "textures/entity/weapon/chronicle_sigil_glow.png").isFile(), "the glow layer is missing");
    }

    @Test
    void theSigilIsAHandItemInBothClassTablesAndNotAWeaponForBetterCombat() throws Exception {
        for (String table : new String[] {ASSETS + "weapon_classes.json", "tools/relics/weapon_classes.json"}) {
            JsonObject weapons = json(table).getAsJsonObject("weapons");
            assertEquals("handitem", weapons.get("chronicle_sigil").getAsString(), table);
        }
        JsonObject excluded = json("tools/bettercombat/manifest.json").getAsJsonObject("excluded");
        assertTrue(excluded.has("chronicle_sigil"), "the Better Combat manifest should exclude the sigil (it is not a weapon)");
    }

    @Test
    void theRendererDrawsIt() throws Exception {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/WeaponRenderer.java"));
        assertTrue(src.contains("ArcWorldItems.CHRONICLE_SIGIL"), "client/WeaponRenderer.BASE_WEAPONS does not list the sigil");
    }

    @Test
    void theArenaDimensionAndItsTypeAreDeclared() throws Exception {
        JsonObject dim = json(DATA + "dimension/arc_arena.json");
        assertEquals("nusmp:arc_arena", dim.get("type").getAsString());
        assertEquals("minecraft:flat", dim.getAsJsonObject("generator").get("type").getAsString());
        JsonObject type = json(DATA + "dimension_type/arc_arena.json");
        assertEquals(0, type.get("min_y").getAsInt(), "the arena sites are built at y 64 up: a dimension from y 0 is enough");
        assertTrue(type.get("height").getAsInt() >= 192, "the pit's roof reaches y 86 and the obelisk is not here: 256 is generous");
        assertFalse(type.get("bed_works").getAsBoolean(), "nobody sleeps through a boss fight");
        assertFalse(type.get("has_raids").getAsBoolean());
    }
}
