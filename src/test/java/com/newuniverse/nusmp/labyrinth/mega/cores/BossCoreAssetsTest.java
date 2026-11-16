package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.franchise.FranchiseTabs;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Kit;
import com.newuniverse.nusmp.labyrinth.mega.cores.BossCoreRules.Piece;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Every core and piece has its icon, model, recipe and name; every set has its two worn layers; the creative tabs and the franchise rule know the items. */
class BossCoreAssetsTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path ASSETS = RES.resolve("assets/anime_multiverse");
    private static final Path RECIPES = RES.resolve("data/anime_multiverse/recipe");

    private static BufferedImage png(Path p) throws IOException {
        assertTrue(Files.isRegularFile(p), "missing " + p);
        return ImageIO.read(p.toFile());
    }

    private static void itemFiles(String id, String lang) throws IOException {
        BufferedImage icon = png(ASSETS.resolve("textures/item/" + id + ".png"));
        assertEquals(16, icon.getWidth(), id);
        assertEquals(16, icon.getHeight(), id);
        JsonObject model = JsonParser.parseString(Files.readString(ASSETS.resolve("models/item/" + id + ".json"))).getAsJsonObject();
        assertEquals("anime_multiverse:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString(), id);
        assertTrue(lang.contains("\"item.anime_multiverse." + id + "\""), "no name for " + id);
    }

    @Test
    void everyCoreAndPieceHasIconModelAndName() throws IOException {
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        for (Kit k : Kit.values()) {
            itemFiles(k.coreId(), lang);
            assertTrue(lang.contains("\"item.anime_multiverse." + k.coreId() + ".lore\""), "no lore for " + k.coreId());
            assertTrue(lang.contains("\"tooltip.anime_multiverse.boss_set." + k.id() + "\""), "no set text for " + k.id());
            for (Piece p : Piece.values()) itemFiles(k.pieceId(p), lang);
        }
        assertTrue(lang.contains("\"tooltip.anime_multiverse.boss_core.drop\""));
    }

    @Test
    void everySetHasTwoWornLayersOfTheVanillaSize() throws IOException {
        for (Kit k : Kit.values()) {
            for (int n = 1; n <= 2; n++) {
                BufferedImage layer = png(ASSETS.resolve("textures/models/armor/bosscore_" + k.id() + "_layer_" + n + ".png"));
                assertEquals(64, layer.getWidth());
                assertEquals(32, layer.getHeight());
            }
        }
    }

    @Test
    void everyPieceHasARecipeMadeOfItsCore() throws IOException {
        for (Kit k : Kit.values()) {
            for (Piece p : Piece.values()) {
                JsonObject r = JsonParser.parseString(Files.readString(RECIPES.resolve("boss_core_" + k.pieceId(p) + ".json"))).getAsJsonObject();
                assertEquals("anime_multiverse:" + k.pieceId(p), r.getAsJsonObject("result").get("id").getAsString());
                JsonArray in = r.getAsJsonArray("ingredients");
                assertTrue(in.size() >= 3 && in.size() <= 9, k.pieceId(p) + " has " + in.size() + " ingredients");
                for (int i = 0; i < in.size(); i++) assertEquals("anime_multiverse:" + k.coreId(), in.get(i).getAsJsonObject().get("item").getAsString());
            }
        }
    }

    @Test
    void creativeTabsAndFranchiseRuleKnowTheItems() throws IOException {
        String tabs = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUCreativeTab.java"));
        assertTrue(tabs.contains("BossCoreItems.addTo(o.apply(G.DUNGEON), o.apply(G.ARMOR))"));
        for (Kit k : Kit.values()) {
            assertEquals("tensura", FranchiseTabs.tabOf(k.coreId(), null), k.coreId());
            assertTrue(BossCoreRules.isBossCoreId(k.coreId()));
            for (Piece p : Piece.values()) {
                assertEquals("tensura", FranchiseTabs.tabOf(k.pieceId(p), null), k.pieceId(p));
                assertTrue(BossCoreRules.isBossCoreId(k.pieceId(p)));
            }
        }
        assertNull(FranchiseTabs.tabOf("aegis_of_the_abyss", null));
        assertFalse(BossCoreRules.isBossCoreId("aegis_of_the_abyss"));
    }
}
