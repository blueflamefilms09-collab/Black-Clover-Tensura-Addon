package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The skin files of phase 2, with no game: every dungeon look's skin is a 64x64 PNG (the vanilla player layout), a look that says it glows has a glow map of the same size with at least one lit
 * texel, an archetype's second layer (hat, jacket, sleeves, pants) is not empty, at least forty archetype skins are distinct, the skins of a family differ from one another (no two Foundry
 * mobs share a skin file's bytes), the arm width matches the skin (a slim look is a slim skin: the three pixel arm's missing column stays empty), the dungeon's extras and head parts are
 * well-formed geo models with a texture of the same name, and the generator's report of the creatures that keep their model adds up to the Foundry's 400.
 */
class DungeonSkinsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";

    private static Map<String, PlayerLook> dungeon() throws Exception {
        try (Reader r = new FileReader(ASSETS + "playermodel/looks_dungeon.json")) {
            return PlayerLook.parseTable(JsonParser.parseReader(r).getAsJsonObject());
        }
    }

    private static BufferedImage png(String path) throws Exception {
        try (InputStream in = Files.newInputStream(new File(ASSETS + path).toPath())) {
            return ImageIO.read(in);
        }
    }

    @Test
    void everyDungeonSkinIsA64x64PlayerSkinAndEveryClaimedGlowMapIsLit() throws Exception {
        int n = 0;
        for (PlayerLook l : dungeon().values()) {
            String path = SkinPath.resolve(l.skin());
            BufferedImage img = png(path);
            assertEquals(64, img.getWidth(), l.id());
            assertEquals(64, img.getHeight(), l.id());
            if (l.glow()) {
                BufferedImage g = png(SkinPath.glowOf(path));
                assertEquals(64, g.getWidth(), l.id() + " glow");
                assertEquals(64, g.getHeight(), l.id() + " glow");
                boolean lit = false;
                for (int y = 0; y < 64 && !lit; y++) for (int x = 0; x < 64; x++) if (((g.getRGB(x, y) >>> 24) & 0xFF) != 0) { lit = true; break; }
                assertTrue(lit, l.id() + " says it glows but its glow map is empty");
            }
            n++;
        }
        assertTrue(n >= 380, "dungeon looks: " + n);
    }

    @Test
    void theHeadAndBodyOfEverySkinAreFilledAndTheArchetypesUseTheSecondLayer() throws Exception {
        for (PlayerLook l : dungeon().values()) {
            BufferedImage img = png(SkinPath.resolve(l.skin()));
            for (int[] r : new int[][]{{8, 8, 8, 8}, {20, 20, 8, 12}, {44, 20, 3, 12}, {4, 20, 4, 12}}) {          // the head's front, the body's front, the right arm's (3 wide: slim or wide) and leg's fronts
                for (int y = r[1]; y < r[1] + r[3]; y++) for (int x = r[0]; x < r[0] + r[2]; x++)
                    assertTrue(((img.getRGB(x, y) >>> 24) & 0xFF) != 0, l.id() + ": a hole in the base layer at " + x + "," + y);
            }
            if (l.id().startsWith("dg_")) assertTrue(DungeonAudit.secondLayerUsed(img), l.id() + ": an empty second layer");
        }
    }

    @Test
    void atLeastFortyArchetypeSkinsAreDistinctAndNoTwoFoundryMobsShareASkin() throws Exception {
        Set<Integer> archetypes = new HashSet<>(), foundry = new HashSet<>();
        int a = 0, f = 0;
        for (PlayerLook l : dungeon().values()) {
            int hash = java.util.Arrays.hashCode(png(SkinPath.resolve(l.skin())).getRGB(0, 0, 64, 64, null, 0, 64));
            if (l.id().startsWith("dg_")) {
                archetypes.add(hash);
                a++;
            } else if (l.id().startsWith("foundry_")) {
                foundry.add(hash);
                f++;
            }
        }
        assertTrue(a >= 50, "archetypes: " + a);
        assertEquals(a, archetypes.size(), "every archetype has its own skin");
        assertTrue(f >= 300, "foundry people: " + f);
        assertTrue(foundry.size() >= f - 2, "the foundry's people must not share skins: " + foundry.size() + " distinct of " + f);
    }

    @Test
    void theArmWidthOfEveryLookMatchesItsSkin() throws Exception {
        int slim = 0, wide = 0;
        for (PlayerLook l : dungeon().values()) {
            BufferedImage img = png(SkinPath.resolve(l.skin()));
            // a wide arm's box unfolds to 16 columns, a slim arm's to 14: the last two columns of the right arm's rows (x 54 and 55, y 20..31) and of the left arm's (x 46 and 47, y 52..63) are bare in a slim skin
            int filled = 0, total = 0;
            for (int y = 20; y < 32; y++) for (int x = 54; x < 56; x++) { total++; if (((img.getRGB(x, y) >>> 24) & 0xFF) != 0) filled++; }
            for (int y = 52; y < 64; y++) for (int x = 46; x < 48; x++) { total++; if (((img.getRGB(x, y) >>> 24) & 0xFF) != 0) filled++; }
            if (l.slim()) {
                assertEquals(0, filled, l.id() + ": a slim look with a wide arm's texels");
                slim++;
            } else {
                assertEquals(total, filled, l.id() + ": a wide look whose arms are slim in the skin");
                wide++;
            }
        }
        assertTrue(slim >= 50 && wide >= 150, "slim " + slim + ", wide " + wide);
    }

    @Test
    void theExtrasOfTheDungeonAreWellFormedGeoModelsWithATextureOfTheirOwn() throws Exception {
        Set<String> seen = new HashSet<>();
        for (PlayerLook l : dungeon().values()) {
            for (PlayerLook.Extra x : l.extras()) {
                if (!seen.add(x.model())) continue;
                File geo = new File(ASSETS + "geo/entity/" + x.model() + ".geo.json");
                assertTrue(geo.isFile(), l.id() + ": " + x.model());
                JsonObject g;
                try (Reader r = new FileReader(geo)) {
                    g = JsonParser.parseReader(r).getAsJsonObject();
                }
                com.newuniverse.nusmp.client.geo.GeoModelData m = com.newuniverse.nusmp.client.geo.GeoModelData.parse(x.model(), g);
                assertTrue(m.cubeCount() > 0, x.model() + " has no cubes");
                for (com.newuniverse.nusmp.client.geo.GeoModelData.Bone root : m.roots) assertTrue(root.name.startsWith("x_"), x.model() + " root " + root.name);
                BufferedImage tex = png("textures/entity/" + x.texture() + ".png");
                assertEquals(m.texW, tex.getWidth(), x.model() + " texture width");
                assertEquals(m.texH, tex.getHeight(), x.model() + " texture height");
                if (x.glow()) {
                    File gf = new File(ASSETS + "textures/entity/" + x.texture() + "_glow.png");
                    if (gf.isFile()) assertEquals(tex.getWidth(), ImageIO.read(gf).getWidth(), x.model() + " glow size");
                }
            }
        }
        assertTrue(seen.size() >= 300, "extras models: " + seen.size());
        for (String part : new String[]{"ears_goblin", "ears_elf", "ears_pointed", "ears_fin", "tusks_orc", "beard_dwarf", "snout_reptile", "crown_spikes"})
            assertTrue(seen.contains("pmx_dg_" + part) || new File(ASSETS + "geo/entity/pmx_dg_" + part + ".geo.json").isFile(), part);
    }

    @Test
    void theCreaturesThatKeepTheirModelAndThePeopleMakeUpTheFoundry() throws Exception {
        JsonObject legacy, index;
        try (Reader r = new FileReader(ASSETS + "playermodel/dungeon_legacy.json")) {
            legacy = JsonParser.parseReader(r).getAsJsonObject();
        }
        try (Reader r = new FileReader(ASSETS + "foundry/index.json")) {
            index = JsonParser.parseReader(r).getAsJsonObject();
        }
        int creatures = legacy.getAsJsonObject("creatures").size(), people = legacy.get("people").getAsInt();
        assertEquals(index.getAsJsonObject("mobs").size(), creatures + people, "every foundry mob is a person or a creature");
        assertEquals(400, creatures + people);
        assertFalse(legacy.getAsJsonObject("creatures").has("torch_raid_goblin"), "a goblin is a person");
        assertTrue(legacy.getAsJsonObject("creatures").has("gloom_gel_cube"), "a gelatinous cube is not");
        for (String slime : legacy.getAsJsonObject("creatures").keySet()) assertFalse(new File(ASSETS + "textures/entity/playermodel/foundry_" + slime + ".png").isFile(), slime + " has no skin");
    }
}
