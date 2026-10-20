package com.newuniverse.nusmp.client.relics.djinn.tiny;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The generated art of the Tiny Djinn (tools/gen_tiny_djinn.py) is all there and well formed: every one of the 36 Djinn has a spec in the hand-curated table (with the
 * colours of the enum), a geo model with the shared 25 bones and a sane cube count, a 64 x 64 texture and a glow map for the light Djinn and for the Dark Djinn, the
 * shared animation file has the six clips and moves only bones every model has, and the looks differ from one another.
 */
class TinyDjinnAssetsTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");
    private static final File SPECS = new File("tools/tiny_djinn/specs.json");

    private static JsonObject json(File f) throws Exception {
        try (Reader r = new FileReader(f)) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    private static File asset(String path) { return new File(ASSETS, path); }

    private static Map<String, JsonObject> specs() throws Exception {
        Map<String, JsonObject> m = new HashMap<>();
        for (JsonElement e : json(SPECS).getAsJsonArray("djinn")) m.put(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject());
        return m;
    }

    @Test
    void everyDjinnHasASpecWithItsOwnColours() throws Exception {
        Map<String, JsonObject> specs = specs();
        assertEquals(Djinn.values().length, specs.size(), "one spec per Djinn of the enum, no extra");
        for (Djinn d : Djinn.values()) {
            JsonObject s = specs.get(d.id());
            assertNotNull(s, d.id() + ": no spec");
            assertEquals(d.displayName(), s.get("name").getAsString(), d.id());
            assertEquals(d.color(), Integer.parseInt(s.get("color").getAsString(), 16), d.id() + ": body colour differs from the enum");
            assertEquals(d.glow(), Integer.parseInt(s.get("glow").getAsString(), 16), d.id() + ": glow colour differs from the enum");
            for (String key : new String[]{"skin", "eyes", "mouth", "chest", "pauldron", "hair", "ears", "cape", "weapon", "tail", "acc", "note", "pal"})
                assertTrue(s.has(key), d.id() + ": spec lacks " + key);
            assertTrue(s.getAsJsonArray("head").size() >= 1, d.id() + ": no headgear");
            JsonObject pal = s.getAsJsonObject("pal");
            for (String key : new String[]{"hair", "cloth", "cloth2", "trim", "metal", "accent", "glow", "eye"}) assertTrue(pal.has(key), d.id() + ": palette lacks " + key);
            assertEquals(s.get("color").getAsString(), pal.get("accent").getAsString(), d.id() + ": the accent is the body colour");
        }
    }

    @Test
    void theLooksAreAllDifferent() throws Exception {
        Map<String, JsonObject> specs = specs();
        Set<String> kits = new HashSet<>(), notes = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            JsonObject s = specs.get(d.id());
            String kit = s.get("head") + "|" + s.get("weapon").getAsString() + "|" + s.get("acc").getAsString() + "|" + s.get("hair").getAsString();
            assertTrue(kits.add(kit), d.id() + " shares its headgear, hair, weapon and accessory with another Djinn: " + kit);
            assertTrue(notes.add(s.get("note").getAsString()), d.id() + ": the same note as another");
        }
    }

    @Test
    void everyDjinnHasAWellFormedModelWithTheSharedBones() throws Exception {
        Set<String> bones = new HashSet<>(List.of(TinyDjinnAssets.BONES));
        assertEquals(25, bones.size());
        for (Djinn d : Djinn.values()) {
            File f = asset(TinyDjinnAssets.model(d));
            assertTrue(f.isFile(), d.id() + ": no model " + f);
            GeoModelData m = GeoModelData.parse(f.getName(), json(f));
            assertEquals(TinyDjinnAssets.TEXTURE_SIZE, m.texW, d.id());
            assertEquals(TinyDjinnAssets.TEXTURE_SIZE, m.texH, d.id());
            assertEquals(bones, m.bones.keySet(), d.id() + ": the bone set is the shared one");
            int cubes = m.cubeCount();
            assertTrue(cubes >= TinyDjinnAssets.MIN_CUBES && cubes <= TinyDjinnAssets.MAX_CUBES, d.id() + " has " + cubes + " cubes");
            for (GeoModelData.Bone b : m.bones.values()) {
                if (b.parent != null) assertTrue(m.bones.containsKey(b.parent), d.id() + ": " + b.name + " has the unknown parent " + b.parent);
                for (GeoModelData.Cube c : b.cubes) {
                    assertTrue(c.sx >= 0 && c.sy >= 0 && c.sz >= 0, d.id() + ": negative size in " + b.name);
                    for (float[] uv : c.uv) {
                        if (uv == null) continue;
                        for (int i = 0; i < 4; i += 2) assertTrue(uv[i] >= -0.01f && uv[i] <= m.texW + 0.01f, d.id() + ": u outside the texture in " + b.name);
                        for (int i = 1; i < 4; i += 2) assertTrue(uv[i] >= -0.01f && uv[i] <= m.texH + 0.01f, d.id() + ": v outside the texture in " + b.name);
                    }
                }
            }
            // the head carries the hair, the crown and the dark-only crest; the weapon bone holds the Djinn's weapon
            assertTrue(m.bones.get("weapon").cubes.size() >= 3, d.id() + ": an empty hand");
            assertTrue(m.bones.get("dark").cubes.size() >= 8, d.id() + ": the dark crest is missing");
            assertEquals(1, m.bones.get("head").cubes.size() >= 1 ? 1 : 0, d.id() + ": no head");
        }
    }

    @Test
    void everyDjinnHasTextureAndGlowForBothTheLightAndTheDarkDjinn() throws Exception {
        Set<Integer> lightSums = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            BufferedImage light = null, dark = null;
            for (boolean isDark : new boolean[]{false, true}) {
                File t = asset(TinyDjinnAssets.texture(d, isDark)), g = asset(TinyDjinnAssets.glow(d, isDark));
                assertTrue(t.isFile(), d.id() + ": no texture " + t.getName());
                assertTrue(g.isFile(), d.id() + ": no glow " + g.getName());
                BufferedImage img = ImageIO.read(t), glow = ImageIO.read(g);
                assertEquals(TinyDjinnAssets.TEXTURE_SIZE, img.getWidth(), t.getName());
                assertEquals(TinyDjinnAssets.TEXTURE_SIZE, img.getHeight(), t.getName());
                assertEquals(img.getWidth(), glow.getWidth(), g.getName());
                assertEquals(img.getHeight(), glow.getHeight(), g.getName());
                int lit = 0, opaque = 0;
                for (int y = 0; y < glow.getHeight(); y++) for (int x = 0; x < glow.getWidth(); x++) if ((glow.getRGB(x, y) >>> 24) != 0) lit++;
                for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) if ((img.getRGB(x, y) >>> 24) > 200) opaque++;
                assertTrue(lit >= 20, d.id() + ": the glow map of " + t.getName() + " has only " + lit + " lit pixels");
                assertTrue(opaque >= 400, d.id() + ": " + t.getName() + " is nearly empty");
                if (isDark) dark = img; else light = img;
            }
            // the swatch strips of the thorn crown and the eye crest (strips 30 and 31, x 60..63, y 56..63): clear in the light texture, painted in the dark one
            for (int x = 60; x < 64; x++) for (int y = 56; y < 64; y++) {
                assertEquals(0, light.getRGB(x, y) >>> 24, d.id() + ": the dark-only strip is visible in the light texture");
                assertEquals(255, dark.getRGB(x, y) >>> 24, d.id() + ": the dark-only strip is missing in the dark texture");
            }
            int diff = 0;
            for (int y = 0; y < 56; y += 2) for (int x = 0; x < 64; x += 2) if (light.getRGB(x, y) != dark.getRGB(x, y)) diff++;
            assertTrue(diff > 150, d.id() + ": the dark palette barely differs from the light one (" + diff + ")");
            int sum = 0;
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) sum = sum * 31 + light.getRGB(x, y);
            assertTrue(lightSums.add(sum), d.id() + ": the same texture as another Djinn");
        }
    }

    @Test
    void theSharedAnimationHasTheSixClipsAndMovesOnlyRealBones() throws Exception {
        File f = asset(TinyDjinnAssets.ANIMATION);
        assertTrue(f.isFile(), "no animation file");
        GeoAnim a = GeoAnim.parse(json(f));
        Set<String> bones = new HashSet<>(List.of(TinyDjinnAssets.BONES));
        for (String clip : TinyDjinnAssets.CLIPS) {
            GeoAnim.Clip c = a.clip(clip);
            assertNotNull(c, "no clip " + clip);
            assertTrue(c.length > 0.3f && c.length < 7f, clip + " length " + c.length);
            assertFalse(c.bones.isEmpty(), clip + " moves nothing");
            for (String b : c.bones.keySet()) assertTrue(bones.contains(b), "clip " + clip + " moves the unknown bone " + b);
        }
        assertTrue(a.clip("idle").loop, "idle loops");
        for (String once : new String[]{"cast", "attack", "hop", "poof", "exit"}) assertFalse(a.clip(once).loop, once + " plays once");
        // the clip lengths the state machine counts in ticks
        assertEquals(TinyDjinnActor.CAST_TICKS / 20f, a.clip("cast").length, 1e-4f);
        assertEquals(TinyDjinnActor.ATTACK_TICKS / 20f, a.clip("attack").length, 1e-4f);
        assertEquals(TinyDjinnActor.HOP_TICKS / 20f, a.clip("hop").length, 1e-4f);
        assertEquals(TinyDjinnActor.ENTER_TICKS / 20f, a.clip("poof").length, 1e-4f);
        assertEquals(TinyDjinnActor.EXIT_TICKS / 20f, a.clip("exit").length, 1e-4f);
    }

    @Test
    void theIdleClipLoopsWithoutAJump() throws Exception {
        JsonObject idle = json(asset(TinyDjinnAssets.ANIMATION)).getAsJsonObject("animations").getAsJsonObject("idle");
        for (Map.Entry<String, JsonElement> be : idle.getAsJsonObject("bones").entrySet()) {
            for (String kind : new String[]{"rotation", "position", "scale"}) {
                JsonElement tr = be.getValue().getAsJsonObject().get(kind);
                if (tr == null || !tr.isJsonObject()) continue;
                JsonObject keys = tr.getAsJsonObject();
                double first = Double.MAX_VALUE, last = -1;
                JsonArray fv = null, lv = null;
                for (Map.Entry<String, JsonElement> k : keys.entrySet()) {
                    double t = Double.parseDouble(k.getKey());
                    if (t < first) { first = t; fv = k.getValue().getAsJsonArray(); }
                    if (t > last) { last = t; lv = k.getValue().getAsJsonArray(); }
                }
                if (kind.equals("rotation") && be.getKey().equals("acc")) continue;             // a full turn (0 to 360) closes the loop
                for (int i = 0; i < 3; i++)
                    assertEquals(fv.get(i).getAsDouble(), lv.get(i).getAsDouble(), 0.01, "idle " + be.getKey() + "." + kind + " does not close its loop");
            }
        }
    }

    @Test
    void everyClipKeepsTheArmStanceOfTheIdlePose() throws Exception {
        JsonObject anims = json(asset(TinyDjinnAssets.ANIMATION)).getAsJsonObject("animations");
        for (String clip : new String[]{"cast", "attack", "hop", "poof", "exit"}) {
            for (String arm : new String[]{"arm_r", "arm_l"}) {
                JsonObject tr = anims.getAsJsonObject(clip).getAsJsonObject("bones").getAsJsonObject(arm);
                assertNotNull(tr, clip + " has no " + arm + " track, so it would pop out of the idle stance");
                assertTrue(tr.has("rotation"), clip + "." + arm);
            }
        }
    }

    @Test
    void theNameOnTheFigureIsTheDjinnsOwnAndPathsAreWhatTheGeneratorWrites() {
        Djinn a = Djinn.ALLOCES;
        assertEquals("Alloces", TinyDjinnAssets.label(a));
        assertEquals("tinydjinn_alloces", TinyDjinnAssets.key(a));
        assertEquals("geo/entity/tinydjinn_alloces.geo.json", TinyDjinnAssets.model(a));
        assertEquals("textures/entity/tinydjinn_alloces.png", TinyDjinnAssets.texture(a, false));
        assertEquals("textures/entity/tinydjinn_alloces_dark_glow.png", TinyDjinnAssets.glow(a, true));
        assertEquals("tinydjinn_glasya_labolas", TinyDjinnAssets.key(Djinn.GLASYA_LABOLAS));
    }
}
