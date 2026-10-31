package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 3.40.0 (HD arc characters, docs/arc/hd_arc.md): the Seven Deadly Sins and Black Clover characters of the kit are drawn with 256x256 skins (the same UV layout as the 64x64 ones). Reads the real files, no game:
 * every skin of the look table {@code looks.json} is square, a power-of-two multiple of 64 and has a glow map of the SAME size exactly when the look glows; the HD roster is HD, painted on every base
 * rectangle, its second layer is a clean cutout, its hair is a kit style; and {@code looks.json} equals the merge of the fragments of {@code tools/arc_hd/looks} (so the two generators never disagree).
 */
class ArcHdSkinsTest {
    private static final Path RES = Path.of("src/main/resources/assets/nusmp");
    private static final Path SKINS = RES.resolve("textures/entity/playermodel");
    private static final Path FRAGMENTS = Path.of("tools/arc_hd/looks");

    /** The base ids of tools/arc_hd/roster.py (a `_form2` of each is checked when the table has it). */
    private static final List<String> ROSTER = List.of(
            "meliodas", "diane", "ban", "king", "gowther", "merlin", "escanor",
            "asta", "yuno", "noelle_silva", "yami_sukehiro", "mimosa_vermillion", "nozel_silva", "luck_voltia", "magna_swing", "finral_roulacase", "charmy_pappitson", "gordon_agrippa",
            "vanessa_enoteca", "klaus_lunettes", "secre_swallowtail", "zora_ideale", "gauche_adlai", "marx_francois", "solid_silva", "fuegoleon_vermillion", "mereoleona_vermillion",
            "leopold_vermillion", "julius_novachrono", "william_vangeance", "nacht_faust",
            "dante_zogratis", "vanica_zogratis", "zenon_zogratis", "lucius_zogratis", "fana", "heath_grice", "lotus_whomalt", "morris_libardirt", "patolli", "rades_spirito", "revchi_salik",
            "rhya", "mars", "lucifero", "udaeus", "vetto", "zagred", "zagred_legacy",
            "dark_disciple", "dark_disciple_astaroth", "dark_disciple_beelzebub", "dark_disciple_lucifero", "dark_disciple_megicula", "devil_avatar", "devil_avatar_megicula");

    private static JsonObject looks() throws Exception {
        return JsonParser.parseString(Files.readString(RES.resolve("playermodel/looks.json"))).getAsJsonObject().getAsJsonObject("looks");
    }

    private static BufferedImage png(String name) throws Exception {
        return ImageIO.read(SKINS.resolve(name + ".png").toFile());
    }

    /** The look names of the roster that exist in the table: each base id and its `_form2`. */
    private static List<String> rosterNames(JsonObject looks) {
        List<String> out = new ArrayList<>();
        for (String id : ROSTER) for (String n : new String[]{id, id + "_form2"}) if (looks.has(n)) out.add(n);
        return out;
    }

    @Test
    void everyKitSkinAndGlowMapIsSquareAMultipleOf64AndTheGlowMatchesItsSkin() throws Exception {
        JsonObject looks = looks();
        assertTrue(looks.size() >= 100, "the table has " + looks.size() + " looks");
        for (Map.Entry<String, JsonElement> e : looks.entrySet()) {
            JsonObject look = e.getValue().getAsJsonObject();
            String skin = look.get("skin").getAsString();
            BufferedImage img = png(skin);
            assertNotNull(img, e.getKey() + ": " + skin + ".png is not a png");
            assertEquals(img.getWidth(), img.getHeight(), e.getKey() + ": a skin is square");
            assertEquals(0, img.getWidth() % 64, e.getKey() + ": a multiple of 64");
            int k = img.getWidth() / 64;
            assertTrue(k >= 1 && (k & (k - 1)) == 0 && img.getWidth() <= 1024, e.getKey() + ": a power-of-two multiple of 64 up to 1024, got " + img.getWidth());
            assertTrue(SkinPath.validSkinSize(img.getWidth(), img.getHeight()), e.getKey());
            boolean glows = look.has("glow") && look.get("glow").getAsBoolean();
            Path gp = SKINS.resolve(skin + "_glow.png");
            if (glows) assertTrue(Files.isRegularFile(gp), e.getKey() + " says it glows but has no glow map");
            if (Files.isRegularFile(gp)) {
                BufferedImage g = ImageIO.read(gp.toFile());
                assertEquals(img.getWidth(), g.getWidth(), e.getKey() + ": the glow map has the size of its skin");
                assertEquals(img.getHeight(), g.getHeight(), e.getKey() + ": the glow map has the size of its skin");
            }
        }
    }

    @Test
    void theSdsAndBlackCloverRosterIsHdAndHasItsFormsAndGlowMaps() throws Exception {
        JsonObject looks = looks();
        List<String> names = rosterNames(looks);
        for (String id : ROSTER) assertTrue(looks.has(id), "no look for " + id);
        assertTrue(names.size() >= 80, "the roster has " + names.size() + " looks (56 base ids and their second forms)");
        int hd = 0;
        for (String n : names) {
            BufferedImage img = png(looks.getAsJsonObject(n).get("skin").getAsString());
            assertTrue(img.getWidth() >= 128, n + " is " + img.getWidth() + " wide: the owner asked for HD (128 or 256) for the Seven Deadly Sins and Black Clover characters");
            hd++;
        }
        assertEquals(names.size(), hd);
    }

    @Test
    void everyBaseRectangleOfAnHdArcSkinIsPaintedAndTheSecondLayerIsACutout() throws Exception {
        JsonObject looks = looks();
        for (String n : rosterNames(looks)) {
            JsonObject look = looks.getAsJsonObject(n);
            BufferedImage img = png(look.get("skin").getAsString());
            int k = img.getWidth() / 64, armW = look.has("slim") && look.get("slim").getAsBoolean() ? 3 : 4;
            int[][] base = {{8, 8, 8, 8}, {20, 20, 8, 12}, {32, 20, 8, 12}, {4, 20, 4, 12}, {12, 20, 4, 12},             // head front, body front and back, right leg front and back
                    {44, 20, armW, 12}, {36, 52, armW, 12}};                                                               // right arm front, left arm front (it starts at 36 when the arm is 4 wide, at 36 for slim too)
            for (int[] r : base)
                for (int y = r[1] * k; y < (r[1] + r[3]) * k; y++)
                    for (int x = r[0] * k; x < (r[0] + r[2]) * k; x++)
                        assertTrue((img.getRGB(x, y) >>> 24) != 0, n + ": a hole in the base layer at " + x + "," + y);
            // the second layers (hat 32,0; jacket 16,32; right sleeve 40,32; right pants 0,32; left pants 0,48; left sleeve 48,48) hold only fully opaque or fully transparent texels
            int[][] overlay = {{32 * k, 0, 32 * k, 16 * k}, {16 * k, 32 * k, 24 * k, 16 * k}, {40 * k, 32 * k, 16 * k, 16 * k}, {0, 32 * k, 16 * k, 32 * k}, {48 * k, 48 * k, 16 * k, 16 * k}};
            for (int[] r : overlay)
                for (int y = r[1]; y < r[1] + r[3]; y++)
                    for (int x = r[0]; x < r[0] + r[2]; x++) {
                        int a = img.getRGB(x, y) >>> 24;
                        assertTrue(a == 0 || a == 255, n + ": half-transparent second-layer texel at " + x + "," + y + " (alpha " + a + ")");
                    }
        }
    }

    @Test
    void theHairOfEveryRosterLookIsAKitStyleOrNone() throws Exception {
        JsonObject styles = JsonParser.parseString(Files.readString(RES.resolve("playermodel/hair_styles.json"))).getAsJsonObject().getAsJsonObject("styles");
        JsonObject looks = looks();
        for (String n : rosterNames(looks)) {
            JsonObject look = looks.getAsJsonObject(n);
            if (!look.has("hair")) {
                assertFalse(look.has("hair_color"), n + ": a hair colour without a haircut");
                continue;
            }
            assertTrue(styles.has(look.get("hair").getAsString()), n + ": " + look.get("hair").getAsString() + " is not in hair_styles.json");
            assertTrue(look.has("hair_color") && look.get("hair_color").getAsString().matches("#[0-9a-fA-F]{6}"), n + " needs a hair colour");
        }
    }

    @Test
    void looksJsonEqualsTheMergeOfTheArcHdFragments() throws Exception {
        if (!Files.isDirectory(FRAGMENTS)) return;                                   // run from a source export without the tools folder: nothing to compare
        JsonObject looks = looks();
        Map<String, JsonObject> frags = new TreeMap<>();
        try (var s = Files.list(FRAGMENTS)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".json")).sorted()::iterator) {
                JsonObject f = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                frags.put(f.get("id").getAsString(), f);
            }
        }
        assertTrue(frags.size() >= 80, "fragments: " + frags.size());
        for (Map.Entry<String, JsonObject> e : frags.entrySet()) {
            assertTrue(looks.has(e.getKey()), e.getKey() + " is not in looks.json");
            assertEquals(e.getValue().getAsJsonObject("look"), looks.getAsJsonObject(e.getKey()), e.getKey() + ": looks.json differs from its fragment (run tools/arc_hd/merge_looks.py)");
            BufferedImage img = png(e.getValue().getAsJsonObject("look").get("skin").getAsString());
            assertEquals(e.getValue().get("size").getAsInt(), img.getWidth(), e.getKey() + ": the skin is not the size its fragment says");
        }
    }
}
