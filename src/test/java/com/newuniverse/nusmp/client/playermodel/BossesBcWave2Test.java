package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wave 2 of the redesigned Black Clover boss looks (docs/bosses_bc_wave2.md), checked with no game: every wave 2 old look is still there and has its {@code bc_} look; the audit of the files finds
 * nothing (256 skins, atlases and face rectangles inside, no kit hair, the quad budget); the kept props of the two looks that drop old wings and horns are real; the God of Order has its own
 * switch (default off, the Blender body stays); wave 1's table is untouched; the doc, previews and lang keys exist; the new class has nothing server-unsafe.
 */
class BossesBcWave2Test {
    private static final String ASSETS = "src/main/resources/assets/anime_multiverse/";
    private static final String PKG = "src/main/java/com/newuniverse/nusmp/client/playermodel/";
    private static final Set<String> WAVE2 = Set.of("dark_disciple", "dark_disciple_lucifero", "dark_disciple_megicula", "dark_disciple_beelzebub", "dark_disciple_astaroth",
            "devil_avatar", "devil_avatar_form2", "devil_avatar_megicula", "devil_avatar_megicula_form2", "god_of_order", "god_of_order_form2", "heath_grice", "heath_grice_form2", "lotus_whomalt",
            "mars", "mars_form2", "revchi_salik", "vetto", "vetto_form2", "morris_libardirt", "morris_libardirt_form2");

    private static InputStream open(String path) {
        try {
            File f = new File(ASSETS + path);
            return f.isFile() ? Files.newInputStream(f.toPath()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }

    @Test
    void theTableIsTheTwentyOneLooksAndWaveOneIsUntouched() {
        assertEquals(WAVE2, BcWave2.oldIds());
        assertEquals(14, BcLooks.oldIds().size(), "wave 1 stays the 14 looks of 4.14.0");
        for (String old : WAVE2) {
            assertFalse(BcLooks.oldIds().contains(old), old + " is wave 2's, not wave 1's");
            assertEquals("bc_" + old, BcWave2.of(old));
            assertEquals("bc_" + old, BcLooks.of(old), "BcLooks.of falls through to wave 2");
            assertNotNull(BcLooks.theme("bc_" + old), "theme of " + old);
            assertTrue(BcLooks.allOldIds().contains(old));
        }
        assertEquals(BcLooks.oldIds().size() + WAVE2.size(), BcLooks.allOldIds().size());
        assertNull(BcWave2.of("meliodas"), "the Seven Deadly Sins bosses are another franchise: not in this wave");
        assertNull(BcWave2.of(null));
    }

    @Test
    void everyWaveTwoLookExistsAndTheOldOneIsKept() {
        Map<String, PlayerLook> looks = PlayerLookTables.load(BossesBcWave2Test::open).looks();
        for (String old : WAVE2) {
            assertTrue(looks.containsKey(old), "the old look stays: " + old);
            PlayerLook l = looks.get("bc_" + old);
            assertNotNull(l, "bc_" + old + " in looks_bc.json");
            assertFalse(l.hasHair(), "bc_" + old + " has cube hair, not a kit hair style");
            assertEquals("bc_" + old, l.skin());
            assertEquals(looks.get(old).scale(), l.scale(), 1e-6f, "the size of the old look is kept");
        }
    }

    @Test
    void theAuditOfTheFilesFindsNothing() {
        List<String> bad = BcAudit.run(BossesBcWave2Test::open);
        assertTrue(bad.isEmpty(), "problems: " + bad);
    }

    @Test
    void everyLookIsInsideTheQuadBudgetFromItsOwnGeoFiles() throws Exception {
        Map<String, PlayerLook> looks = PlayerLookTables.load(BossesBcWave2Test::open).looks();
        for (String old : WAVE2) {
            int quads = 0, fx = 0;
            for (PlayerLook.Extra x : looks.get("bc_" + old).extras()) {
                if (!x.model().startsWith("pmx_bc_")) continue;
                JsonObject g = JsonParser.parseString(read(ASSETS + "geo/entity/" + x.model() + ".geo.json")).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                int q = 0;
                for (JsonElement be : g.getAsJsonArray("bones")) {
                    JsonObject b = be.getAsJsonObject();
                    if (b.has("cubes")) for (JsonElement ce : b.getAsJsonArray("cubes")) q += ce.getAsJsonObject().getAsJsonObject("uv").size();
                }
                quads += q;
                if (x.model().endsWith("_fx")) fx += q;
            }
            assertTrue(quads > 0 && quads <= BcAudit.MAX_QUADS, old + ": " + quads + " quads");
            assertTrue(fx <= BcAudit.MAX_FX_QUADS, old + ": " + fx + " glowing quads");
        }
    }

    @Test
    void theKeptPropsOfHeathAndMorrisAreRealAndTheCrudeWingsAndHornsAreGone() throws Exception {
        Map<String, PlayerLook> looks = PlayerLookTables.load(BossesBcWave2Test::open).looks();
        for (String old : new String[]{"heath_grice_form2", "morris_libardirt_form2"}) {
            boolean keep = false;
            for (PlayerLook.Extra x : looks.get("bc_" + old).extras()) {
                assertFalse(x.model().equals("pmx_" + old), "the whole old model is not kept for " + old);
                if (!x.model().endsWith("_keep")) continue;
                keep = true;
                JsonObject g = JsonParser.parseString(read(ASSETS + "geo/entity/" + x.model() + ".geo.json")).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                Set<String> names = new HashSet<>();
                JsonArray bones = g.getAsJsonArray("bones");
                for (JsonElement be : bones) names.add(be.getAsJsonObject().get("name").getAsString());
                for (JsonElement be : bones) {
                    JsonObject b = be.getAsJsonObject();
                    String n = b.get("name").getAsString();
                    assertFalse(n.startsWith("wing") || n.equals("horns") || n.equals("halo"), old + " keeps " + n + " which the new look replaces");
                    if (b.has("parent")) assertTrue(names.contains(b.get("parent").getAsString()), n + " lost its parent");
                }
                assertTrue(new File(ASSETS + "textures/entity/" + x.texture() + ".png").isFile(), "the copied atlas of " + x.model());
            }
            assertTrue(keep, old + " keeps its held props");
        }
    }

    @Test
    void theGodOfOrderHasItsOwnSwitchOffByDefaultAndTheBlenderBodyStays() throws Exception {
        assertTrue(BcWave2.isOrder("god_of_order") && BcWave2.isOrder("god_of_order_form2"));
        assertFalse(BcWave2.isOrder("mars") || BcWave2.isOrder(null));
        String o = read(PKG + "PlayerModelOptions.java");
        assertTrue(o.contains(".define(\"godOfOrderPlayerModel\", false)"), "off by default: the Blender body stays");
        assertTrue(o.contains("GOD_OF_ORDER_BLENDER.get() && !godOfOrderPlayerModel()"), "the player-model look wins only while it is on");
        assertTrue(o.contains("ModConfigSpec.Builder") && !o.contains("Type.SERVER") && !o.contains("Type.COMMON"), "a CLIENT config");
        assertTrue(read(PKG + "PlayerAssets.java").contains("BcWave2.isOrder(id) ? PlayerModelOptions.godOfOrderPlayerModel() : PlayerModelOptions.bcPlayerModel()"));
        assertTrue(read(PKG + "PlayerBodies.java").contains("PlayerAssets.bcId(devil == null ? \"dark_disciple\""), "the Dark Disciple body asks for the new look through the option");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/client/bossmesh/BossMeshRouting.java").contains("PlayerModelOptions.blender(id)"), "the Blender routing still asks the option");
    }

    @Test
    void theNewClassHasNothingServerUnsafe() throws Exception {
        String s = read(PKG + "BcWave2.java");
        assertFalse(s.contains("net.minecraft") || s.contains("Minecraft.getInstance()") || s.contains("com.mojang.blaze3d"));
    }

    @Test
    void docPreviewsLangAndGeneratorAreThere() throws Exception {
        assertTrue(new File("docs/bosses_bc_wave2.md").isFile());
        for (String f : new String[]{"bcchars2.py", "bcparts4.py", "bcskin2.py", "gen_bc.py"}) assertTrue(new File("tools/bc_playermodels/" + f).isFile(), f);
        assertTrue(new File("docs/bosses_bc_wave2_previews/overview.png").isFile());
        for (String old : WAVE2) assertTrue(new File("docs/bosses_bc_wave2_previews/bc_" + old + ".png").isFile(), "preview sheet of " + old);
        JsonObject lang = JsonParser.parseString(read("tools/lang/bosses_bc_wave2.json")).getAsJsonObject();
        for (String k : new String[]{"anime_multiverse.configuration.godOfOrderPlayerModel", "anime_multiverse.configuration.godOfOrderPlayerModel.tooltip"}) assertTrue(lang.has(k) && !lang.get(k).getAsString().isBlank(), k);
    }
}
