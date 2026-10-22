package com.newuniverse.nusmp.client.aura;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.book.NoelleFormRules.Form;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: Noelle Silva's armour forms (docs/magic/noelle_forms.md) are made of files that nothing can look at without the game, so the repository proves what it can: every piece the rules list
 * has its geo model (linked bones, uvs inside the texture), its texture and glow map of the model's size, and an animation that moves only real bones when the painter plays a clip on it; the painter
 * lists exactly the pieces the rules list (standard and character), with the character ones flagged; every sprite and skin the painter draws exists, tiles or fades at its border and carries no colour
 * under a transparent texel (the additive passes need that, docs/vfx_square_bug.md); the whole set stays under an asset-size budget; the generators are there to rebuild everything; the lang fragment
 * has the three appended pages; the policy file and the page source agree; the Water book appended (never reordered) its pages. Pure: reads the source tree.
 */
class NoelleFormAssetsTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");
    private static final File JAVA = new File("src/main/java/com/newuniverse/nusmp");
    private static final Form[] FORMS = {Form.VALKYRIE, Form.MERMAID, Form.SEA_DRAGON, Form.BLACK_DRAGON};

    private static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f, StandardCharsets.UTF_8)) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    /** The part of the painter's source that lists the pieces of a form (the dragon forms share dragonSet). */
    private static String bodyOf(String src, String key) {
        if (key.equals("ndg") || key.equals("nbk")) return src.substring(src.indexOf("private static NoelleFormRig.Set dragonSet"), src.indexOf("private static final Look L_VAL"));
        int at = src.indexOf("new NoelleFormRig.Set(\"" + key + "\"");
        return src.substring(at, src.indexOf(");", at));
    }

    private static List<String> all(Form f) {
        List<String> l = new ArrayList<>(List.of(NoelleFormRules.standardPieces(f)));
        l.addAll(List.of(NoelleFormRules.characterPieces(f)));
        return l;
    }

    // ---------------------------------------------------------------- the models
    @Test
    void everyPieceHasItsModelTextureGlowAndAnimation() throws Exception {
        String painter = read(new File(JAVA, "client/aura/NoelleFormsAura.java"));
        int models = 0;
        for (Form f : FORMS) {
            for (String n : all(f)) {
                String id = NoelleFormRules.key(f) + "_arm_" + n;
                models++;
                File geo = new File(ASSETS, "geo/entity/" + id + ".geo.json");
                assertTrue(geo.isFile(), "missing geo model " + id);
                JsonObject g = json(geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                JsonObject desc = g.getAsJsonObject("description");
                int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
                assertTrue(tw <= 1024 && th <= 1024, id + ": texture " + tw + "x" + th + " is larger than 1024");
                Set<String> bones = new HashSet<>();
                for (JsonElement b : g.getAsJsonArray("bones")) bones.add(b.getAsJsonObject().get("name").getAsString());
                int cubes = 0;
                for (JsonElement be : g.getAsJsonArray("bones")) {
                    JsonObject b = be.getAsJsonObject();
                    if (b.has("parent")) assertTrue(bones.contains(b.get("parent").getAsString()), id + ": bone " + b.get("name") + " has a missing parent");
                    for (JsonElement ce : b.getAsJsonArray("cubes")) {
                        cubes++;
                        JsonElement uv = ce.getAsJsonObject().get("uv");
                        if (uv.isJsonObject()) for (var face : uv.getAsJsonObject().entrySet()) {
                            JsonArray at = face.getValue().getAsJsonObject().getAsJsonArray("uv"), size = face.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                            assertTrue(at.get(0).getAsInt() >= 0 && at.get(1).getAsInt() >= 0, id + ": negative uv");
                            assertTrue(at.get(0).getAsInt() + size.get(0).getAsInt() <= tw && at.get(1).getAsInt() + size.get(1).getAsInt() <= th, id + ": uv outside the texture");
                        }
                    }
                }
                assertTrue(cubes > 0 && cubes <= 90, id + ": " + cubes + " cubes");
                for (String suffix : new String[]{".png", "_glow.png"}) {
                    File tex = new File(ASSETS, "textures/entity/" + id + suffix);
                    assertTrue(tex.isFile(), "missing " + tex.getName());
                    BufferedImage img = ImageIO.read(tex);
                    assertEquals(tw, img.getWidth(), id + suffix + " width");
                    assertEquals(th, img.getHeight(), id + suffix + " height");
                }
                // the glow map carries no colour under a fully transparent texel (the additive pass of the glow)
                BufferedImage glow = ImageIO.read(new File(ASSETS, "textures/entity/" + id + "_glow.png"));
                for (int y = 0; y < glow.getHeight(); y += 3)
                    for (int x = 0; x < glow.getWidth(); x += 3) {
                        int argb = glow.getRGB(x, y);
                        if ((argb >>> 24) == 0) assertEquals(0, argb & 0xFFFFFF, id + "_glow: colour under alpha 0 at " + x + "," + y);
                    }
                File anim = new File(ASSETS, "animations/entity/" + id + ".animation.json");
                boolean plays = Pattern.compile("Piece\\.of\\(\"" + n + "\"[^\\n]*clip\\(").matcher(bodyOf(painter, NoelleFormRules.key(f))).find();
                if (plays) assertTrue(anim.isFile(), "the painter plays a clip on " + id + " but there is no animation");
                if (anim.isFile()) for (var clip : json(anim).getAsJsonObject("animations").entrySet()) {
                    assertTrue(clip.getValue().getAsJsonObject().get("animation_length").getAsFloat() > 0f, id + ": a clip has no length");
                    for (var bone : clip.getValue().getAsJsonObject().getAsJsonObject("bones").entrySet())
                        assertTrue(bones.contains(bone.getKey()), id + ": animation " + clip.getKey() + " names the unknown bone " + bone.getKey());
                }
            }
        }
        assertTrue(models >= 60, "the four forms are made of " + models + " models");
    }

    // ---------------------------------------------------------------- the painter and the rules agree
    @Test
    void thePainterListsExactlyThePiecesTheRulesList() throws Exception {
        String src = read(new File(JAVA, "client/aura/NoelleFormsAura.java"));
        for (Form f : FORMS) {
            String key = NoelleFormRules.key(f);
            // the dragon forms share one list built by dragonSet(key); the others have their own Set("key", ...)
            String body = bodyOf(src, key);
            Set<String> listed = new HashSet<>(), flagged = new HashSet<>();
            Matcher m = Pattern.compile("Piece\\.of\\(\"([a-z_0-9]+)\",[^\\n]*").matcher(body);
            while (m.find()) {
                assertTrue(listed.add(m.group(1)), key + ": " + m.group(1) + " is listed twice");
                if (m.group().contains(".character()")) flagged.add(m.group(1));
            }
            assertEquals(new HashSet<>(all(f)), listed, key + ": the painter's pieces differ from the rules' list");
            assertEquals(new HashSet<>(List.of(NoelleFormRules.characterPieces(f))), flagged, key + ": the character pieces are not exactly the flagged ones");
        }
    }

    @Test
    void theEveryFormDrawsOnlyThroughTheSafeAdditiveTypesAndTheSharedRig() throws Exception {
        String aura = read(new File(JAVA, "client/aura/NoelleFormsAura.java")), rig = read(new File(JAVA, "client/aura/NoelleFormRig.java"));
        for (String s : new String[]{aura, rig}) {
            assertFalse(s.contains("RenderType.eyes"), "an additive pass must go through AuraRender / SafeAdditive");
            assertFalse(s.contains("RenderType.lightning"), "no raw render types");
            assertFalse(s.contains("Minecraft.getInstance().level"), "no world access from a painter");
        }
        assertTrue(aura.contains("AuraRender.additive(") && aura.contains("NoelleFormRig.draw("));
        assertTrue(read(new File(JAVA, "client/aura/WaterAura.java")).contains("NoelleFormsAura.paint(c)"), "the Water painter hands the remade styles on");
    }

    // ---------------------------------------------------------------- the sprites
    @Test
    void everySpriteExistsFadesAtItsBorderAndHidesNoColourUnderAlphaZero() throws Exception {
        for (String s : NoelleFormRules.SPRITES) {
            File f = new File(ASSETS, "textures/aura/" + s + ".png");
            assertTrue(f.isFile(), "missing sprite " + s);
            BufferedImage img = ImageIO.read(f);
            assertTrue(img.getWidth() >= 32 && img.getWidth() <= 512 && img.getHeight() >= 32 && img.getHeight() <= 512, s + " has an odd size");
            boolean band = s.equals("noelle_ring") || s.startsWith("noelle_ribbon");          // the bands run along u: only their top and bottom edge must be clear
            boolean skin = s.endsWith("_shell") || s.equals("noelle_haze");                    // the skin sheets cover a whole skin layout: no border rule
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    if ((argb >>> 24) == 0) assertEquals(0, argb & 0xFFFFFF, s + ": colour under alpha 0 at " + x + "," + y);
                    boolean edge = band ? (y == 0 || y == img.getHeight() - 1) : (x == 0 || y == 0 || x == img.getWidth() - 1 || y == img.getHeight() - 1);
                    if (!skin && edge) assertTrue((argb >>> 24) <= 2, s + ": a lit texel on the border at " + x + "," + y);
                }
            if (skin) {
                File glow = new File(ASSETS, "textures/aura/" + s + "_glow.png");
                assertTrue(glow.isFile(), "missing glow map of " + s);
                BufferedImage g = ImageIO.read(glow);
                assertEquals(img.getWidth(), g.getWidth());
                assertEquals(img.getHeight(), g.getHeight());
            }
        }
    }

    @Test
    void theWholeSetStaysUnderTheAssetBudget() throws Exception {
        long bytes = 0;
        for (Form f : FORMS) for (String n : all(f)) {
            String id = NoelleFormRules.key(f) + "_arm_" + n;
            for (String p : new String[]{"geo/entity/" + id + ".geo.json", "textures/entity/" + id + ".png", "textures/entity/" + id + "_glow.png", "animations/entity/" + id + ".animation.json"}) {
                File file = new File(ASSETS, p);
                if (file.isFile()) bytes += file.length();
            }
        }
        for (String s : NoelleFormRules.SPRITES) {
            File file = new File(ASSETS, "textures/aura/" + s + ".png");
            if (file.isFile()) bytes += file.length();
            File glow = new File(ASSETS, "textures/aura/" + s + "_glow.png");
            if (glow.isFile()) bytes += glow.length();
        }
        assertTrue(bytes < 8L * 1024 * 1024, "the forms weigh " + bytes / 1024 + " KiB (budget 8192)");
    }

    // ---------------------------------------------------------------- the generators and the text
    @Test
    void theGeneratorsAndThePreviewScenesAreThere() {
        for (String f : new String[]{"tools/gen_noelle_forms_geo.py", "tools/gen_noelle_forms_val.py", "tools/gen_noelle_forms_mer.py", "tools/gen_noelle_forms_drg.py", "tools/gen_noelle_forms_blk.py",
                "tools/noelle_dragon_kit.py", "tools/noelle_forms_lib.py", "tools/gen_noelle_forms_textures.py", "tools/render_preview/scenes.d/noelle_forms.json", "tools/render_preview/gen_noelle_scenes.py"})
            assertTrue(new File(f).isFile(), "missing " + f);
    }

    @Test
    void theLangFragmentHasTheThreeAppendedPagesAndTheMessages() throws Exception {
        JsonObject o = json(new File("tools/lang/noelle_forms.json"));
        for (String p : new String[]{"mermaid_form", "sea_dragon_god_form", "black_dragon_form", "valkyrie_dress"}) {
            String key = "nusmp.page.book_water." + p;
            assertTrue(o.has(key) && o.get(key).getAsString().length() > 60, "missing text for " + key);
        }
        for (String k : new String[]{"nusmp.noelle.limit", "nusmp.noelle.black_end", "nusmp.noelle.black_exhausted"}) assertTrue(o.has(k), "missing " + k);
        for (var e : o.entrySet()) assertFalse(e.getValue().getAsString().contains("\n"), e.getKey() + " has a line break");
    }

    @Test
    void thePolicyTheBookAndTheLocksAgree() throws Exception {
        JsonObject water = json(new File("src/main/resources/data/nusmp/armour_policy.json")).getAsJsonObject("magics").getAsJsonObject("WATER");
        JsonObject pages = water.getAsJsonObject("armour_pages");
        assertEquals(1, pages.get("valkyrie_dress").getAsInt());
        assertEquals(0, pages.get("scale_mail").getAsInt());
        assertEquals(2, pages.get("mermaid_form").getAsInt());
        assertEquals(3, pages.get("sea_dragon_god_form").getAsInt());
        assertEquals(4, pages.get("black_dragon_form").getAsInt());
        assertEquals("character", water.get("style_by").getAsString());
        Set<String> appended = new HashSet<>();
        for (JsonElement e : water.getAsJsonArray("new_pages")) appended.add(e.getAsString());
        assertTrue(appended.containsAll(Set.of("mermaid_form", "sea_dragon_god_form", "black_dragon_form")), "the three forms are among the appended pages (the pilots' and the basic grimoires' pages are listed there too): " + appended);
        String book = read(new File(JAVA, "book/WaterBook.java"));
        int torrent = book.indexOf("BookPage.zone(\"torrent\"");
        assertTrue(torrent > 0 && book.indexOf("BookPage.zone(\"mermaid_form\"") > torrent && book.indexOf("BookPage.signature(\"sea_dragon_god_form\"") > book.indexOf("\"mermaid_form\"")
                && book.indexOf("BookPage.signature(\"black_dragon_form\"") > book.indexOf("\"sea_dragon_god_form\""), "the three pages are appended after torrent, in order");
        assertTrue(book.indexOf("BookPage.signature(\"valkyrie_dress\"") < torrent, "the Valkyrie Dress page keeps its place");
        String locks = read(new File(JAVA, "book/CharacterSpells.java"));
        assertTrue(locks.contains("lock(\"book_water\", \"sea_dragon_god_form\", CanonBook.NOELLE)") && locks.contains("lock(\"book_water\", \"black_dragon_form\", CanonBook.NOELLE)"));
        assertFalse(locks.contains("\"mermaid_form\""), "the Mermaid Form is open to every Water mage");
    }

    @Test
    void theClassicSwitchIsAClientConfigWithAGuardedRead() throws Exception {
        String cfg = read(new File(JAVA, "client/aura/NoelleFormsClientConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT") && cfg.contains("classicLook") && cfg.contains("nusmp-noelle-forms-client.toml"));
        assertTrue(cfg.contains("IllegalStateException"), "a read before the config is loaded falls back");
        assertFalse(read(new File(JAVA, "client/aura/NoelleFormsAura.java")).contains("NoelleFormsClientConfig"), "the painter reads the plain option holder, never the config class");
    }

    @Test
    void theServerSideHoldsNoClientClasses() throws Exception {
        for (String f : new String[]{"book/NoelleFormRules.java", "book/NoelleFormState.java", "book/NoelleFormsSelfTests.java"}) {
            String s = read(new File(JAVA, f));
            assertFalse(s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("nusmp.client"), f + " touches a client class");
            assertFalse(s.contains("Minecraft.getInstance"), f + " reaches for the client singleton");
        }
        String state = read(new File(JAVA, "book/NoelleFormState.java"));
        assertFalse(state.toLowerCase().contains("foodlevel") || state.toLowerCase().contains("exhaustion(") || state.contains("MobEffects.HUNGER") || state.contains("addExhaustion"), "no form may touch the food bar");
        assertTrue(state.contains("p.tickCount % 20 != 3"), "the per-player tick stops early for everyone");
    }
}
