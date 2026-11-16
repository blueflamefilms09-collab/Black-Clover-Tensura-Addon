package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.WorldDevil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The devil looks (docs/devil_looks.md), checked with no game: every devil of the roster has a look; skins are 256x256; every atlas and every face rectangle is inside its texture; the cost budget; the
 * head stays in proportion; the option is a CLIENT config with the old figures one switch away; the new sources have nothing server-unsafe and nothing that allocates per frame; the lang fragment, the
 * self-test, the doc and the previews are in place.
 */
class DevilPlayerModelsTest {
    private static final String ASSETS = "src/main/resources/assets/anime_multiverse/";
    private static final String PKG = "src/main/java/com/newuniverse/nusmp/client/playermodel/";
    private static final String DEVIL = "src/main/java/com/newuniverse/nusmp/client/devil/";

    private static InputStream open(String path) {
        try {
            File f = new File(ASSETS + path);
            return f.isFile() ? Files.newInputStream(f.toPath()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }

    private static List<String> roster() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < WorldDevil.count(); i++) ids.add(WorldDevil.at(i).id());
        return ids;
    }

    /** The text of a method of {@code src} from its signature to the next member marker. */
    private static String section(String src, String from, String to) {
        int a = src.indexOf(from);
        assertTrue(a >= 0, from + " is missing");
        int b = to == null ? src.length() : src.indexOf(to, a);
        assertTrue(b > a, to + " is missing");
        return src.substring(a, b);
    }

    @Test
    void auditOfTheFilesFindsNothing() {
        List<String> bad = DevilLooksAudit.run(DevilPlayerModelsTest::open, roster());
        assertTrue(bad.isEmpty(), "problems: " + bad);
    }

    @Test
    void everyDevilOfTheRosterHasALookInRosterOrder() {
        List<String> ids = roster();
        assertEquals(26, ids.size(), "the roster has 26 devils (docs/devil_worlds.md)");
        assertEquals(ids, DevilLooks.devilIds(), "DevilLooks lists the roster, in its order");
        Map<String, PlayerLook> looks = PlayerLookTables.load(DevilPlayerModelsTest::open).looks();
        for (String id : ids) {
            assertEquals("dv_" + id, DevilLooks.of(id));
            assertNotNull(looks.get("dv_" + id), "look of " + id);
        }
        assertNull(DevilLooks.of("nobody"));
        assertNull(DevilLooks.of(null));
    }

    @Test
    void theOldLooksAreKeptAndTheNewTableComesLast() throws Exception {
        assertEquals("playermodel/looks.json", PlayerLookTables.FILES.get(0));
        assertTrue(PlayerLookTables.FILES.indexOf("playermodel/looks_dv.json") > PlayerLookTables.FILES.indexOf("playermodel/looks_bc.json"), "appended after the Black Clover table");
        Map<String, PlayerLook> looks = PlayerLookTables.load(DevilPlayerModelsTest::open).looks();
        for (String old : BcLooks.oldIds()) assertTrue(looks.containsKey(old) && looks.containsKey(BcLooks.of(old)), "the Black Clover looks are untouched: " + old);
        assertTrue(looks.containsKey("bc_lucifero"), "bc_lucifero is kept next to dv_lucifero (rule 2)");
    }

    @Test
    void theHeadStaysInProportionAndTheBudgetsAreWrittenDown() throws Exception {
        JsonObject stats = JsonParser.parseString(read("tools/bc_playermodels/stats_dv.json")).getAsJsonObject();
        for (String id : roster()) {
            JsonObject s = stats.getAsJsonObject("dv_" + id);
            assertNotNull(s, "stats of " + id);
            assertTrue(s.get("hair_shell_px").getAsFloat() <= 9.0f, id + ": the hair shell is at most 9 px wide (the skull is 8: 1.1x vanilla)");
            int quads = 0;
            for (var e : s.getAsJsonObject("quads").entrySet()) quads += e.getValue().getAsInt();
            assertTrue(quads <= DevilLooksAudit.MAX_QUADS, id + " quads " + quads);
            assertTrue(s.getAsJsonObject("glow_quads").get("fx").getAsInt() <= DevilLooksAudit.MAX_FX_QUADS, id + " glow quads");
        }
    }

    @Test
    void theOptionIsAClientConfigWithTheOldFiguresOneSwitchAway() throws Exception {
        String o = read(PKG + "PlayerModelOptions.java");
        assertTrue(o.contains(".define(\"devilPlayerModel\", true)"), "default on");
        assertTrue(o.contains("ModConfigSpec.Builder"), "the look options are a client spec");
        assertTrue(o.contains("nusmp-playermodel-client.toml"));
        assertFalse(o.contains("ModConfig.Type.SERVER") || o.contains("Type.COMMON"));
        assertTrue(read(PKG + "DevilExtras.java").contains("PlayerModelOptions.devilPlayerModel()"), "lookFor asks the option");
        String npc = read(DEVIL + "DevilNpcRenderer.java");
        assertTrue(npc.contains("DevilExtras.lookFor(") && npc.contains("fallback(ps, buffers, light, a255, rgb, age)") && npc.contains("mesh.draw(ps, buffers, light"), "the mesh and the generic devil are still there");
        String form = read(DEVIL + "DevilFormLayer.java");
        assertTrue(form.contains("DevilExtras.lookFor(") && form.contains("if (look == null) mesh(v.devil).drawParts("), "the mesh parts are drawn when the option is off");
    }

    @Test
    void theNewClassesHaveNothingServerUnsafeAndNoPerFrameAllocation() throws Exception {
        for (String f : new String[]{"DevilLooks.java", "DevilLooksAudit.java"}) {
            String s = read(PKG + f);
            assertFalse(s.contains("Minecraft.getInstance()"), f);
            assertFalse(s.contains("net.minecraft.client"), f);
            assertFalse(s.contains("com.mojang.blaze3d"), f);
        }
        assertFalse(read(PKG + "DevilExtras.java").contains("Minecraft.getInstance()"));
        assertFalse(section(read(PKG + "DevilExtras.java"), "public static void draw(", null).contains("new "), "DevilExtras.draw must not allocate");
        assertFalse(section(read(DEVIL + "DevilNpcRenderer.java"), "private void drawLook(", "/** The old generic devil").contains("new "), "drawLook must not allocate");
        String lod = section(read(PKG + "DevilExtras.java"), "public static void draw(", null);
        assertTrue(lod.contains("ExtrasDraw.FINE_SUFFIX"), "the fine layer is dropped beyond the detail distance");
        assertTrue(read(DEVIL + "DevilFormLayer.java").contains("NoelleFormRules.budgeted("), "the crowd budget still applies to the worn look");
    }

    @Test
    void theLangFragmentIsValidJsonWithTheSettingKeys() throws Exception {
        JsonObject o = JsonParser.parseString(read("tools/lang/devil_looks.json")).getAsJsonObject();
        for (String k : new String[]{"anime_multiverse.configuration.devilPlayerModel", "anime_multiverse.configuration.devilPlayerModel.tooltip"}) {
            assertTrue(o.has(k) && !o.get(k).getAsString().isBlank(), k);
        }
    }

    @Test
    void theSelfTestIsRegisteredUnderTheSmokePrefixAndTheGeoTestKnowsTheLayers() throws Exception {
        assertTrue(read(PKG + "PlayerModelSelfTests.java").contains("SelfTests.register(\"bcmodel.devils\""));
        assertTrue(read(".github/workflows/smoke.yml").contains(",bcmodel,"), "the bcmodel prefix is in the smoke list");
        assertTrue(read("src/test/java/com/newuniverse/nusmp/client/geo/GeoAssetsTest.java").contains("pmx_dv_"), "GeoAssetsTest maps the layers to the atlas");
    }

    @Test
    void theDocTheGeneratorAndThePreviewsAreThere() {
        assertTrue(new File("docs/devil_looks.md").isFile());
        for (String f : new String[]{"gen_dv.py", "dvlib.py", "dvskin.py", "dvchars.py", "dvtable.py", "preview_dv.py"}) assertTrue(new File("tools/bc_playermodels/" + f).isFile(), f);
        assertTrue(new File("docs/devil_looks_previews/overview.png").isFile());
        assertTrue(new File("docs/devil_looks_previews/overview_worn.png").isFile());
        for (String id : roster()) assertTrue(new File("docs/devil_looks_previews/dv_" + id + ".png").isFile(), "preview sheet of " + id);
    }
}
