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
 * The redesigned Black Clover NPC and Midnight Sun looks (docs/npcs_bc.md), checked with no game: every listed character has a new look and keeps the old one, the geometry is valid (every cube face
 * inside its atlas, UV in range), skins are 256x256, the quad budget holds, the table is merged add-only and last, the setting is the existing client one, and the new classes have nothing server-unsafe.
 */
class BcNpcPlayerModelsTest {
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
        List<String> bad = BcNpcAudit.run(BcNpcPlayerModelsTest::open);
        assertTrue(bad.isEmpty(), "problems: " + bad);
    }

    @Test
    void everyListedCharacterHasALookAndTheOldOneIsKept() throws Exception {
        Map<String, PlayerLook> looks = PlayerLookTables.load(BcNpcPlayerModelsTest::open).looks();
        assertEquals(38, BcNpcLooks.oldIds().size(), "the Midnight Sun (4 x 2 forms) and 30 Black Clover characters");
        JsonObject table = JsonParser.parseString(read(ASSETS + "playermodel/looks_bc_npc.json")).getAsJsonObject().getAsJsonObject("looks");
        assertEquals(BcNpcLooks.oldIds().size(), table.size(), "looks_bc_npc.json and BcNpcLooks list the same characters");
        JsonObject stats = JsonParser.parseString(read("tools/bc_playermodels/stats_npc.json")).getAsJsonObject();
        for (String old : BcNpcLooks.oldIds()) {
            String id = BcNpcLooks.of(old);
            assertEquals("bc_" + old, id);
            assertTrue(looks.containsKey(old), "old look kept: " + old);
            assertNotNull(looks.get(id), "new look: " + id);
            assertTrue(table.has(id), "in looks_bc_npc.json: " + id);
            assertTrue(stats.has(id), "generator stats of " + id);
            assertFalse(looks.get(id).extras().isEmpty(), id + " has parts");
            assertNotNull(BcNpcLooks.of(old));
        }
        assertEquals(null, BcNpcLooks.of("not_a_look"));
        assertEquals(null, BcNpcLooks.of(null));
    }

    @Test
    void noBossLookIsCapturedByTheNpcTable() {
        for (String old : BcNpcLooks.oldIds()) assertEquals(null, BcLooks.of(old), old + " is a boss look: the boss table owns it");
    }

    @Test
    void theNpcTableIsMergedLastAndAddOnly() {
        List<String> f = PlayerLookTables.FILES;
        assertEquals("playermodel/looks.json", f.get(0));
        assertTrue(f.indexOf("playermodel/looks_bc_npc.json") >= f.size() - 3, "appended near the end (only the devil and the Goku-Sinbad tables may follow): earlier files win a duplicate");
        assertTrue(f.indexOf("playermodel/looks_bc_npc.json") > f.indexOf("playermodel/looks_bc.json"));
    }

    @Test
    void theNpcBodiesAskForTheNewLookThroughTheExistingClientOption() throws Exception {
        String bodies = read(PKG + "PlayerBodies.java");
        int at = bodies.indexOf("class ArcNpc");
        assertTrue(at > 0);
        assertTrue(bodies.substring(at).contains("PlayerAssets.bcId(e.characterId())"), "ArcNpc asks bcId, so the option restores the old look");
        String a = read(PKG + "PlayerAssets.java");
        assertTrue(a.contains("BcNpcLooks.of(id)") && a.contains("PlayerModelOptions.bcPlayerModel()"));
    }

    @Test
    void theNewClassesHaveNothingServerUnsafe() throws Exception {
        for (String f : new String[]{"BcNpcLooks.java", "BcNpcAudit.java"}) {
            String s = read(PKG + f);
            assertFalse(s.contains("Minecraft.getInstance()"), f);
            assertFalse(s.contains("net.minecraft.client"), f);
            assertFalse(s.contains("com.mojang.blaze3d"), f);
        }
    }

    @Test
    void theSelfTestIsRegistered() throws Exception {
        assertTrue(read(PKG + "PlayerModelSelfTests.java").contains("SelfTests.register(\"bcnpc.looks\""));
    }

    @Test
    void theDocGeneratorAndPreviewsAreThere() {
        assertTrue(new File("docs/npcs_bc.md").isFile());
        assertTrue(new File("tools/bc_playermodels/specs_npc.py").isFile());
        assertTrue(new File("docs/npcs_bc_previews/overview.png").isFile());
        for (String old : BcNpcLooks.oldIds()) assertTrue(new File("docs/npcs_bc_previews/bc_" + old + ".png").isFile(), "preview sheet of " + old);
    }
}
