package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The redesigned Black Clover boss looks (docs/bc_playermodels.md), checked with no game: every old look is still there and every one has a new look; skins are 256x256; every atlas and every face
 * rectangle is inside its texture; the cost budget; the theme of every look exists; the option is a CLIENT config with the old rendering one switch away; the source has nothing server-unsafe
 * and nothing that allocates per frame; the lang fragment and the smoke list are in place.
 */
class BcPlayerModelsTest {
    private static final String ASSETS = "src/main/resources/assets/anime_multiverse/";
    private static final String PKG = "src/main/java/com/newuniverse/nusmp/client/playermodel/";

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
    void auditOfTheFilesFindsNothing() {
        List<String> bad = BcAudit.run(BcPlayerModelsTest::open);
        assertTrue(bad.isEmpty(), "problems: " + bad);
    }

    @Test
    void everyBlackCloverBossLookHasANewLookAndTheOldOneIsKept() {
        Map<String, PlayerLook> looks = PlayerLookTables.load(BcPlayerModelsTest::open).looks();
        assertEquals(14, BcLooks.oldIds().size(), "Lucius, Dante, Vanica, Zenon, Zagred, Zagred legacy and Lucifero, two forms each");
        for (String old : BcLooks.oldIds()) {
            assertTrue(looks.containsKey(old), "old look kept: " + old);
            String id = BcLooks.of(old);
            assertEquals("bc_" + old, id);
            assertNotNull(looks.get(id), "new look: " + id);
            assertTrue(BcLooks.isBc(id));
            assertNotNull(BcLooks.theme(id), "theme of " + id);
        }
        for (String k : looks.keySet()) if (BcLooks.isBc(k)) assertTrue(BcLooks.allOldIds().contains(k.substring(3)), k + " replaces a look that does not exist");
    }

    @Test
    void theNewTablesAreMergedAddOnlyAfterTheOldOnes() throws Exception {
        String t = read(PKG + "PlayerLookTables.java");
        assertTrue(t.contains("playermodel/looks_bc.json"));
        assertTrue(PlayerLookTables.FILES.indexOf("playermodel/looks_bc.json") > 0, "the new table never comes first: earlier files win a duplicate");
        assertEquals("playermodel/looks.json", PlayerLookTables.FILES.get(0));
    }

    @Test
    void everyThemeIsInTheParticleThemeFile() throws Exception {
        JsonObject themes = JsonParser.parseString(read("src/main/resources/data/anime_multiverse/particle_themes.json")).getAsJsonObject().getAsJsonObject("themes");
        for (String old : BcLooks.oldIds()) {
            String t = BcLooks.theme(BcLooks.of(old));
            assertTrue(themes.has(t), t + " is not a theme of particle_themes.json");
        }
    }

    @Test
    void theOptionIsAClientConfigWithTheOldRenderingOneSwitchAway() throws Exception {
        String o = read(PKG + "PlayerModelOptions.java");
        assertTrue(o.contains("ModConfigSpec.Builder"), "the look options are a client spec");
        assertTrue(o.contains(".define(\"bcPlayerModel\", true)"), "default on");
        assertTrue(o.contains("b.push(\"bossLooks\")"));
        assertTrue(o.contains("!bcPlayerModel() && LUCIFERO_BLENDER.get()"), "off restores the Blender Lucifero");
        assertTrue(o.contains("nusmp-playermodel-client.toml"), "the file stays the client one");
        assertFalse(o.contains("ModConfig.Type.SERVER") || o.contains("Type.COMMON"));
        String a = read(PKG + "PlayerAssets.java");
        assertTrue(a.contains("PlayerModelOptions.bcPlayerModel()"), "bcId asks the option");
        String bodies = read(PKG + "PlayerBodies.java");
        assertTrue(bodies.contains("PlayerAssets.bcId("), "the bodies ask for the new look through the option");
    }

    @Test
    void theNewClassesHaveNothingServerUnsafeAndNoPerFrameAllocation() throws Exception {
        for (String f : new String[]{"BcLooks.java", "BcAudit.java", "BcShimmer.java"}) {
            String s = read(PKG + f);
            assertFalse(s.contains("Minecraft.getInstance()"), f);
            assertFalse(s.contains("net.minecraft.client"), f);
            assertFalse(s.contains("com.mojang.blaze3d"), f);
        }
        String shimmer = read(PKG + "BcShimmer.java");
        int body = shimmer.indexOf("public static void tick(");
        assertTrue(body > 0);
        String tick = shimmer.substring(body);
        assertFalse(tick.contains("new "), "BcShimmer.tick must not allocate");
        assertFalse(tick.contains("String.format") || tick.contains("\" +") || tick.contains("+ \""), "no string built per frame");
        assertTrue(tick.contains("PlayerLod.FULL"), "full detail only");
        assertTrue(tick.contains("PERIOD"), "pulses are rate limited");
        assertTrue(tick.contains("isClientSide"), "client only");
        assertTrue(shimmer.contains("Fx.send("), "through the mod's own particle facade (budgets)");
        assertTrue(shimmer.contains("new int[64]"), "bounded state");
        assertTrue(read(PKG + "PlayerModelRenderer.java").contains("BcShimmer.tick(e, look, lod);"));
    }

    @Test
    void theFineLayerIsDroppedBeyondTheDetailDistance() throws Exception {
        String d = read(PKG + "ExtrasDraw.java");
        assertTrue(d.contains("FINE_SUFFIX") && d.contains("_fine"));
        assertTrue(read(PKG + "ExtrasLayer.java").contains("owner.lod()"));
    }

    @Test
    void theLangFragmentIsValidJsonWithTheSettingKeys() throws Exception {
        JsonObject o = JsonParser.parseString(read("tools/lang/bc_playermodels.json")).getAsJsonObject();
        for (String k : new String[]{"anime_multiverse.configuration.bcPlayerModel", "anime_multiverse.configuration.bcPlayerModel.tooltip", "anime_multiverse.configuration.bossLooks"}) {
            assertTrue(o.has(k) && !o.get(k).getAsString().isBlank(), k);
        }
    }

    @Test
    void theSelfTestIsRegisteredAndInTheSmokeListWithTheTailUnchanged() throws Exception {
        assertTrue(read(PKG + "PlayerModelSelfTests.java").contains("SelfTests.register(\"bcmodel.looks\""));
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains(",bcmodel,"), "bcmodel is in the smoke list");
        assertEquals(1, smoke.split(",bcmodel,", -1).length - 1);
        assertTrue(smoke.contains(",scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the tail of the list is as it was");
    }

    @Test
    void theDocAndTheGeneratorAreThere() {
        assertTrue(new File("docs/bc_playermodels.md").isFile());
        assertTrue(new File("tools/bc_playermodels/gen_bc.py").isFile());
        assertTrue(new File("docs/bc_playermodels_previews/overview.png").isFile());
        for (String old : BcLooks.oldIds()) assertTrue(new File("docs/bc_playermodels_previews/bc_" + old + ".png").isFile(), "preview sheet of " + old);
    }
}
