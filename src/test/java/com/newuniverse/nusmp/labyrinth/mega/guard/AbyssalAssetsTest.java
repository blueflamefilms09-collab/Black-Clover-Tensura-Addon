package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a look whose model, texture or glow layer is missing shows in the game as an invisible boss or a purple-and-black cube, and nothing at compile time sees it. This reads the files (no game
 * needed) and fails when any look of assets/nusmp/abyssal/looks.json lacks one: every phase's model, atlas and glow layer (the same size as the model's sheet), the animation file with the eight
 * clips the pose clock plays and no track for a bone no phase has, every bone linked to a parent that exists, every cube face inside its sheet; and for a humanoid the real 64x64 player skin and
 * its glow skin, and an extras model whose anchor bones are named after the player's parts. Also that the phases look different (the textures differ), and that the glow layer of every look
 * lights something.
 */
class AbyssalAssetsTest {
    private static final String ASSETS = "src/main/resources/assets/nusmp/";
    private static final Set<String> CLIPS = Set.of("idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "death");
    private static final Set<String> ROOTS = Set.of("x_head", "x_body", "x_arm_r", "x_arm_l", "x_leg_r", "x_leg_l");
    private static final Set<String> KIT_KEYS = Set.of("skin", "slim", "glow", "glow_color", "hair", "hair_color", "scale", "alpha", "extras", "cape", "hold_r", "hold_l");

    private static JsonObject json(String path) throws Exception {
        try (Reader r = new FileReader(new File(path))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    private static boolean exists(String path) { return new File(path).isFile(); }

    @Test
    void everyLookHasEveryFileItNeeds() {
        assertTrue(AbyssalLooks.all().size() >= 22);
        java.util.List<String> problems = AbyssalLooks.problems(p -> exists("src/main/resources/" + p));
        assertTrue(problems.isEmpty(), "missing files: " + problems);
    }

    @Test
    void modelsAreLinkedAndInsideTheirSheetsAndTexturesMatch() throws Exception {
        for (AbyssalLooks.Look l : AbyssalLooks.all().values()) {
            for (AbyssalLooks.Phase p : l.phases()) {
                checkModel(l.id(), "geo/entity/abyssal_" + p.model() + ".geo.json", "textures/entity/abyssal_" + p.model(), false);
                if (l.humanoid()) {
                    checkModel(l.id(), "geo/entity/" + p.extrasModel() + ".geo.json", "textures/entity/" + p.extrasModel(), true);
                    for (String s : new String[]{"", "_glow"}) {
                        BufferedImage skin = ImageIO.read(new File(ASSETS + "textures/entity/" + p.skin() + s + ".png"));
                        assertEquals(64, skin.getWidth(), l.id() + " skin" + s + " width");
                        assertEquals(64, skin.getHeight(), l.id() + " skin" + s + " height");
                    }
                }
            }
        }
    }

    private static void checkModel(String look, String geoPath, String texStem, boolean extras) throws Exception {
        JsonObject geo = json(ASSETS + geoPath).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject d = geo.getAsJsonObject("description");
        int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
        Set<String> names = new HashSet<>();
        for (JsonElement b : geo.getAsJsonArray("bones")) names.add(b.getAsJsonObject().get("name").getAsString());
        int cubes = 0;
        for (JsonElement be : geo.getAsJsonArray("bones")) {
            JsonObject b = be.getAsJsonObject();
            if (b.has("parent")) assertTrue(names.contains(b.get("parent").getAsString()), look + " " + geoPath + ": bone " + b.get("name") + " has a missing parent");
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                cubes++;
                JsonObject uv = ce.getAsJsonObject().getAsJsonObject("uv");
                for (String face : uv.keySet()) {
                    JsonArray at = uv.getAsJsonObject(face).getAsJsonArray("uv"), size = uv.getAsJsonObject(face).getAsJsonArray("uv_size");
                    assertTrue(at.get(0).getAsFloat() >= 0 && at.get(1).getAsFloat() >= 0, look + " " + geoPath + ": a face starts outside the sheet");
                    assertTrue(at.get(0).getAsFloat() + size.get(0).getAsFloat() <= tw && at.get(1).getAsFloat() + size.get(1).getAsFloat() <= th, look + " " + geoPath + ": a face leaves the sheet");
                }
            }
        }
        assertTrue(cubes >= 5, look + " " + geoPath + " has only " + cubes + " cubes");
        if (extras) {                                                    // the player-model kit's format: every root bone is named for the part it is drawn with
            boolean any = false;
            for (JsonElement be : geo.getAsJsonArray("bones")) {
                JsonObject b = be.getAsJsonObject();
                if (b.has("parent")) continue;
                any = true;
                assertTrue(ROOTS.contains(b.get("name").getAsString()), look + " extras model " + geoPath + " has the root bone " + b.get("name") + ", which is no x_<part>");
            }
            assertTrue(any, look + " extras model " + geoPath + " has no root bone");
        }
        BufferedImage tex = ImageIO.read(new File(ASSETS + texStem + ".png")), glow = ImageIO.read(new File(ASSETS + texStem + "_glow.png"));
        assertEquals(tw, tex.getWidth(), look + " " + texStem + " width");
        assertEquals(th, tex.getHeight(), look + " " + texStem + " height");
        assertEquals(tw, glow.getWidth(), look + " " + texStem + "_glow width");
        assertEquals(th, glow.getHeight(), look + " " + texStem + "_glow height");
        boolean lit = false;
        for (int y = 0; y < glow.getHeight() && !lit; y++) for (int x = 0; x < glow.getWidth(); x++) if ((glow.getRGB(x, y) >>> 24) != 0 && (glow.getRGB(x, y) & 0xFFFFFF) != 0) { lit = true; break; }
        assertTrue(lit, look + " " + texStem + "_glow lights nothing");
    }

    @Test
    void theKitTableHoldsEveryHumanoidPhaseInTheKitsFormat() throws Exception {
        JsonObject looks = json(ASSETS + "playermodel/looks_abyssal.json").getAsJsonObject("looks");
        int n = 0;
        for (AbyssalLooks.Look l : AbyssalLooks.all().values()) {
            if (!l.humanoid()) continue;
            for (int p = 1; p <= 3; p++) {
                String id = AbyssalLooks.kitId(l.id(), p);
                assertTrue(looks.has(id), "playermodel/looks_abyssal.json has no entry " + id);
                JsonObject e = looks.getAsJsonObject(id);
                for (String k : e.keySet()) assertTrue(KIT_KEYS.contains(k), id + " has the key " + k + " which the kit's table does not know");
                assertEquals(id, e.get("skin").getAsString(), id + " skin");
                assertTrue(exists(ASSETS + "textures/entity/playermodel/" + id + ".png") && exists(ASSETS + "textures/entity/playermodel/" + id + "_glow.png"), id + " skin or glow skin missing");
                assertEquals(l.slim(), e.has("slim") && e.get("slim").getAsBoolean(), id + " slim flag");
                JsonObject x = e.getAsJsonArray("extras").get(0).getAsJsonObject();
                assertEquals(l.phase(p).extrasModel(), x.get("model").getAsString(), id + " extras model");
                assertTrue(e.get("scale").getAsFloat() > 0.5f && e.get("scale").getAsFloat() < 2f, id + " scale");
                n++;
            }
        }
        assertEquals(n, looks.size(), "the kit table holds entries that are no humanoid phase");
        assertEquals(27, n, "nine humanoids of three phases");
    }

    @Test
    void theClipsAreThereAndTouchNoBoneThatDoesNotExist() throws Exception {
        for (AbyssalLooks.Look l : AbyssalLooks.all().values()) {
            Set<String> bones = new HashSet<>();
            for (AbyssalLooks.Phase p : l.phases()) {
                JsonObject geo = json(ASSETS + "geo/entity/abyssal_" + p.model() + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                for (JsonElement b : geo.getAsJsonArray("bones")) bones.add(b.getAsJsonObject().get("name").getAsString());
            }
            JsonObject anim = json(ASSETS + "animations/entity/abyssal_" + l.animation() + ".animation.json").getAsJsonObject("animations");
            for (String c : CLIPS) assertTrue(anim.has(c), l.id() + " has no clip " + c);
            for (String c : anim.keySet()) {
                JsonObject clip = anim.getAsJsonObject(c);
                assertTrue(clip.get("animation_length").getAsFloat() > 0f, l.id() + " clip " + c + " has no length");
                for (String b : clip.getAsJsonObject("bones").keySet()) assertTrue(bones.contains(b), l.id() + " clip " + c + " moves bone " + b + " which no phase has");
            }
            assertTrue(anim.getAsJsonObject("idle").get("loop").getAsBoolean(), l.id() + " idle must loop");
            assertFalse(anim.getAsJsonObject("death").get("loop").getAsBoolean(), l.id() + " death must not loop");
        }
    }

    @Test
    void thePhasesLookDifferent() throws Exception {
        for (AbyssalLooks.Look l : AbyssalLooks.all().values()) {
            long[] sums = new long[3];
            long[] glows = new long[3];
            for (int i = 0; i < 3; i++) {
                BufferedImage tex = ImageIO.read(new File(ASSETS + "textures/entity/abyssal_" + l.phase(i + 1).model() + ".png"));
                BufferedImage glow = ImageIO.read(new File(ASSETS + "textures/entity/abyssal_" + l.phase(i + 1).model() + "_glow.png"));
                sums[i] = hash(tex);
                glows[i] = hash(glow);
            }
            assertTrue(sums[0] != sums[1] || glows[0] != glows[1], l.id() + ": phases 1 and 2 have the same textures");
            assertTrue(sums[1] != sums[2] || glows[1] != glows[2], l.id() + ": phases 2 and 3 have the same textures");
        }
    }

    private static long hash(BufferedImage im) {
        long h = 1125899906842597L;
        for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++) h = 31 * h + im.getRGB(x, y);
        return h;
    }
}
