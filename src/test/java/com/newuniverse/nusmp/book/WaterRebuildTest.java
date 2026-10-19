package com.newuniverse.nusmp.book;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.prop.WaterPropMath;
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
 * 1.93.0 (the Water / Dark / Lightning / Flame rebuild): what can be proved without the game. The pure rules (the armour's end time, the prop param word and its yaw / pitch), and the
 * files the rebuild is made of, read from the source tree: every armour piece an Aura painter lists has its geo model, texture, glow map and animation (bones that exist, uvs inside the
 * texture), every summon model a prop painter names does too, the books keep their legacy pages in their legacy order with the new ones appended (at most 24), the enums only grew at
 * the end, and every appended page has its lang text in the fragment.
 */
class WaterRebuildTest {
    private static final String[] KEYS = {"water", "dark", "lightning", "flame"};
    private static final String[] PASCAL = {"Water", "Dark", "Lightning", "Flame"};
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");
    private static final File JAVA = new File("src/main/java/com/newuniverse/nusmp");

    private static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f, StandardCharsets.UTF_8)) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    // ---------------------------------------------------------------- the pure rules
    @Test
    void aShortCastNeverCutsALongArmourShort() {
        assertEquals(1100L, WaterArmourRule.nextEnd(1000, 0, 100, false, false));                // nothing on: now + ticks
        assertEquals(-1L, WaterArmourRule.nextEnd(1000, 1600, 100, true, true));                  // 30 s armour on, a 5 s cast: left alone
        assertEquals(1100L, WaterArmourRule.nextEnd(1000, 1050, 100, true, true));                // the old one ends first: refreshed
        assertEquals(1100L, WaterArmourRule.nextEnd(1000, 1600, 100, true, false));               // another style always replaces
        assertEquals(WaterArmourRule.CAST_TICKS, 100);
    }

    @Test
    void thePropParamWordRoundTrips() {
        assertEquals(1, WaterPropMath.variant(WaterPropMath.HOLD | 1));
        assertEquals(0, WaterPropMath.variant(WaterPropMath.FOLLOW | WaterPropMath.BOB));
        assertEquals(22, WaterPropMath.stopTick(WaterPropMath.stopAfter(22) | WaterPropMath.BOB));
        assertEquals(1023, WaterPropMath.stopTick(WaterPropMath.stopAfter(5000)));
        assertEquals(0, WaterPropMath.stopTick(WaterPropMath.FOLLOW | 2));
        assertEquals(2, WaterPropMath.variant(WaterPropMath.FOLLOW | 2));
    }

    @Test
    void yawAndPitchFaceTheDirection() {
        assertEquals(0f, WaterPropMath.yawOf(0, 1), 1e-3f);                                       // +z is yaw 0
        assertEquals(90f, WaterPropMath.yawOf(-1, 0), 1e-3f);                                      // -x is yaw 90 (west)
        assertEquals(-90f, WaterPropMath.yawOf(1, 0), 1e-3f);
        assertEquals(180f, Math.abs(WaterPropMath.yawOf(0, -1)), 1e-3f);
        assertEquals(45f, WaterPropMath.pitchOf(0, -1, 1), 1e-3f);                                 // falling: looking down is positive
        assertEquals(-90f, WaterPropMath.pitchOf(0, 1, 0), 1e-3f);
        assertEquals(1.0f, WaterPropMath.centre("DARK_1", 0), 0f);
        assertEquals(1.25f, WaterPropMath.centre("DARK_2", 1), 0f);
        assertEquals(0f, WaterPropMath.centre("FLAME_1", 0), 0f);
    }

    // ---------------------------------------------------------------- the model files
    private static void checkModel(String id, boolean needsAnimation) throws Exception {
        File geoFile = new File(ASSETS, "geo/entity/" + id + ".geo.json");
        assertTrue(geoFile.isFile(), "missing geo model " + id);
        JsonObject g = json(geoFile).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject desc = g.getAsJsonObject("description");
        int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
        Set<String> bones = new HashSet<>();
        for (JsonElement b : g.getAsJsonArray("bones")) bones.add(b.getAsJsonObject().get("name").getAsString());
        int cubes = 0;
        for (JsonElement be : g.getAsJsonArray("bones")) {
            JsonObject b = be.getAsJsonObject();
            if (b.has("parent")) assertTrue(bones.contains(b.get("parent").getAsString()), id + ": bone " + b.get("name") + " has a missing parent");
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                cubes++;
                JsonElement uv = ce.getAsJsonObject().get("uv");
                if (uv.isJsonObject()) {
                    for (var face : uv.getAsJsonObject().entrySet()) {
                        JsonArray at = face.getValue().getAsJsonObject().getAsJsonArray("uv"), size = face.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                        assertTrue(at.get(0).getAsInt() >= 0 && at.get(1).getAsInt() >= 0, id + ": negative uv");
                        assertTrue(at.get(0).getAsInt() + size.get(0).getAsInt() <= tw && at.get(1).getAsInt() + size.get(1).getAsInt() <= th, id + ": uv outside the texture");
                    }
                }
            }
        }
        assertTrue(cubes > 0, id + " has no cubes");
        for (String suffix : new String[]{".png", "_glow.png"}) {
            File tex = new File(ASSETS, "textures/entity/" + id + suffix);
            assertTrue(tex.isFile(), "missing " + tex.getName());
            BufferedImage img = ImageIO.read(tex);
            assertEquals(tw, img.getWidth(), id + suffix + " width");
            assertEquals(th, img.getHeight(), id + suffix + " height");
        }
        File anim = new File(ASSETS, "animations/entity/" + id + ".animation.json");
        if (needsAnimation) assertTrue(anim.isFile(), "missing animation " + id);
        if (anim.isFile()) {
            for (var clip : json(anim).getAsJsonObject("animations").entrySet()) {
                for (var bone : clip.getValue().getAsJsonObject().getAsJsonObject("bones").entrySet())
                    assertTrue(bones.contains(bone.getKey()), id + ": animation " + clip.getKey() + " names the unknown bone " + bone.getKey());
                assertTrue(clip.getValue().getAsJsonObject().get("animation_length").getAsFloat() > 0f, id + ": a clip has no length");
            }
        }
    }

    @Test
    void everyArmourPieceHasItsModelTextureGlowAndAnimation() throws Exception {
        for (int k = 0; k < KEYS.length; k++) {
            String src = read(new File(JAVA, "client/aura/" + PASCAL[k] + "Aura.java"));
            Matcher all = Pattern.compile("Piece\\.of\\(\"([a-z_]+)\"").matcher(src);
            List<String> names = new ArrayList<>();
            while (all.find()) names.add(all.group(1));
            assertTrue(names.size() >= 14, KEYS[k] + " armour has only " + names.size() + " pieces");
            assertTrue(names.contains("helm") && names.contains("chest") && names.contains("wing_r") && names.contains("wing_l") && names.contains("halo") && names.contains("backdrop"), KEYS[k] + " armour lacks a main piece");
            assertEquals(new HashSet<>(names).size(), names.size(), KEYS[k] + ": a piece is listed twice");
            for (String n : names) checkModel(KEYS[k] + "_arm_" + n, src.contains("Piece.of(\"" + n + "\"") && Pattern.compile("Piece\\.of\\(\"" + n + "\"[^\\n]*clip\\(").matcher(src).find());
            for (String tex : new String[]{"_shell", "_haze"})
                assertTrue(new File(ASSETS, "textures/aura/" + KEYS[k] + tex + ".png").isFile() && new File(ASSETS, "textures/aura/" + KEYS[k] + tex + "_glow.png").isFile(), KEYS[k] + tex + " texture");
        }
    }

    @Test
    void everySummonModelHasItsFiles() throws Exception {
        int models = 0;
        for (int k = 0; k < KEYS.length; k++) {
            String src = read(new File(JAVA, "client/prop/" + PASCAL[k] + "PropPainter.java"));
            Matcher m = Pattern.compile("GeoSpec\\.of\\(\"([a-z]+)\", \"([a-z_]+)\"\\)").matcher(src);
            while (m.find()) {
                checkModel(m.group(1) + "_" + m.group(2), true);
                models++;
            }
        }
        assertEquals(10, models, "the ten summon models");
    }

    // ---------------------------------------------------------------- the books keep their legacy pages
    private static List<String> pageIds(String bookFile) throws Exception {
        String src = read(new File(JAVA, "book/" + bookFile));
        String list = src.substring(src.indexOf("private final List<BookPage> pages = List.of("), src.indexOf("public " + bookFile.replace(".java", "") + "()"));
        Matcher m = Pattern.compile("BookPage\\.(?:starter|mid|zone|signature|daily)\\(\"([a-z_]+)\"").matcher(list);
        List<String> ids = new ArrayList<>();
        while (m.find()) ids.add(m.group(1));
        return ids;
    }

    private static void checkBook(String file, String[] legacy, String[] added) throws Exception {
        List<String> ids = pageIds(file);
        assertEquals(legacy.length + added.length, ids.size(), file + ": page count");
        for (int i = 0; i < legacy.length; i++) assertEquals(legacy[i], ids.get(i), file + ": legacy page " + i + " moved");
        for (int i = 0; i < added.length; i++) assertEquals(added[i], ids.get(legacy.length + i), file + ": a new page is not appended in order");
        assertTrue(ids.size() <= 24, file + ": at most 24 family pages");
        assertEquals(new HashSet<>(ids).size(), ids.size(), file + ": duplicate page id");
    }

    @Test
    void theBooksKeepTheirLegacyPagesInOrderAndAppendTheNewOnes() throws Exception {
        checkBook("WaterBook.java", new String[]{"waterball", "roar", "cradle", "aqua_javelin", "sea_dragons_nest", "valkyrie_dress"},
                new String[]{"sea_dragons_wrath", "tide_lance", "scale_mail", "torrent"});
        checkBook("DarkBook.java", new String[]{"avidya_slash", "black_hole", "dimension_slash", "death_thrust", "black_blade", "avidya_wild_slash", "black_moon", "iai_slash", "dimension_slash_equinox"},
                new String[]{"dark_cloak", "shadow_embrace", "black_hole_collapse"});
        checkBook("LightningBook.java", new String[]{"chain", "thunder_boots", "thunder_lance", "thunder_fiend", "pulsaranta", "rising_salim", "thunderbird_cavalry", "black_lightning_battle_fiend"},
                new String[]{"raging_lightning", "thunder_drum", "lightning_surge"});
        checkBook("FireBook.java", new String[]{"exploding_fireball", "sol_linea", "calderos", "leo_rugiens", "spiral_flame", "wild_bursting_flame", "ignis_columna", "calidos_brachium"},
                new String[]{"crimson_lion_king", "flame_armour", "phoenix_flare"});
    }

    // ---------------------------------------------------------------- the shared enums only grew at the end
    private static List<String> enumTail(File f, String after, int n) throws Exception {
        String src = read(f).replaceAll("//[^\\n]*", "").replaceAll("/\\*.*?\\*/", "");
        String body = src.substring(src.indexOf(after));
        Matcher m = Pattern.compile("\\b([A-Z][A-Z0-9_]+)\\b").matcher(body.substring(0, body.indexOf(';') > 0 && body.indexOf(';') < body.indexOf('}') ? body.indexOf(';') : body.indexOf('}')));
        List<String> out = new ArrayList<>();
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Test
    void theEnumsOnlyGrewAtTheEnd() throws Exception {
        List<String> aura = enumTail(new File(JAVA, "aura/Aura.java"), "WIKI", 5);
        assertEquals(List.of("WIKI", "WATER", "DARK", "LIGHTNING", "FLAME"), aura);
        List<String> props = enumTail(new File(JAVA, "prop/PropKind.java"), "SHADOW_PACK_1", 9);
        assertEquals(List.of("SHADOW_PACK_1", "WATER_1", "WATER_2", "DARK_1", "DARK_2", "LIGHTNING_1", "LIGHTNING_2", "FLAME_1", "FLAME_2"), props);
        List<String> shapes = enumTail(new File(JAVA, "vfx/VfxShape.java"), "BUTOH_AFTERIMAGE", 13);
        assertEquals(List.of("BUTOH_AFTERIMAGE", "WATERFX_CAST", "WATERFX_ZONE", "WATERFX_BURST", "DARKFX_CAST", "DARKFX_ZONE", "DARKFX_BURST", "LIGHTNINGFX_CAST", "LIGHTNINGFX_ZONE", "LIGHTNINGFX_BURST",
                "FLAMEFX_CAST", "FLAMEFX_ZONE", "FLAMEFX_BURST"), shapes.subList(0, 13));
    }

    // ---------------------------------------------------------------- the lang fragments
    @Test
    void everyAppendedPageHasItsText() throws Exception {
        String[][] pages = {{"water", "book_water", "sea_dragons_wrath", "tide_lance", "scale_mail", "torrent"},
                {"dark", "book_dark", "dark_cloak", "shadow_embrace", "black_hole_collapse"},
                {"lightning", "book_lightning", "raging_lightning", "thunder_drum", "lightning_surge"},
                {"flame", "book_fire", "crimson_lion_king", "flame_armour", "phoenix_flare"}};
        for (String[] p : pages) {
            File f = new File("tools/lang/" + p[0] + "_rebuild.json");
            assertTrue(f.isFile(), "missing lang fragment " + f);
            JsonObject o = json(f);
            for (int i = 2; i < p.length; i++) {
                String key = "nusmp.page." + p[1] + "." + p[i];
                assertTrue(o.has(key) && o.get(key).getAsString().length() > 20, "missing text for " + key);
            }
            for (var e : o.entrySet()) assertFalse(e.getValue().getAsString().contains("\n"), e.getKey() + " has a line break");
        }
    }
}
