package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (3.61.0): the 3D cube forms of the 27 Beast Wills are generated files (tools/beastwill/remake/forms_export.py), and a model that is missing or outside its texture would silently draw nothing
 * (or a logged warning on every reload). This reads the real files and pins: the table has exactly the 27 stems of {@code BeastWillLook.STEM}; every listed piece has its geo file, every piece with a
 * bone has its animation, the shared sheet and its glow map exist at the size the geo declares; every cube's uv lies inside the sheet; the cost is bounded (a will has at most 320 cubes, the Acquire
 * piece at most 16); the body, head, both arms and both legs exist for every will; and the painter and the switch are wired. Pure (file reads); the working directory is the project root.
 */
class BeastWillFormsTest {
    private static final Path RES = Path.of("src/main/resources/assets/nusmp");

    private static JsonObject json(Path p) throws Exception { return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }

    @Test
    void theTableHoldsExactlyTheStemsOfTheLookKinds() {
        java.util.Set<String> want = new java.util.HashSet<>();
        for (int k = 1; k < BeastWillLook.COUNT; k++) want.add(BeastWillLook.STEM[k]);
        assertEquals(27, want.size());
        assertEquals(want, BeastWillFormTable.stems());
        for (String s : want) for (String p : new String[]{"head", "body", "armL", "armR", "legL", "legR", "acq"}) assertTrue(BeastWillFormTable.has(s, p), s + " lacks " + p);
        assertFalse(BeastWillFormTable.has("nope", "head"));
    }

    @Test
    void everyPieceHasItsModelSheetAndGlowAndStaysInsideTheSheetAndTheCubeBudget() throws Exception {
        for (String stem : BeastWillFormTable.stems()) {
            String key = "bwf_" + stem;
            File sheet = RES.resolve("textures/entity/" + key + ".png").toFile(), glow = RES.resolve("textures/entity/" + key + "_glow.png").toFile();
            assertTrue(sheet.exists() && glow.exists(), key + " sheet or glow missing");
            BufferedImage img = javax.imageio.ImageIO.read(sheet);
            int total = 0;
            for (String piece : BeastWillFormTable.parts(stem).split(" ")) {
                Path geo = RES.resolve("geo/entity/" + key + "_arm_" + piece + ".geo.json");
                assertTrue(Files.exists(geo), geo + " missing");
                JsonObject g = json(geo).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                JsonObject d = g.getAsJsonObject("description");
                assertEquals(img.getWidth(), d.get("texture_width").getAsInt(), key + " " + piece);
                assertEquals(img.getHeight(), d.get("texture_height").getAsInt(), key + " " + piece);
                int cubes = 0;
                boolean bones = false;
                for (JsonElement be : g.getAsJsonArray("bones")) {
                    JsonObject b = be.getAsJsonObject();
                    if (!b.get("name").getAsString().equals("root")) bones = true;
                    JsonArray cs = b.has("cubes") ? b.getAsJsonArray("cubes") : new JsonArray();
                    for (JsonElement ce : cs) {
                        cubes++;
                        for (var f : ce.getAsJsonObject().getAsJsonObject("uv").entrySet()) {
                            JsonArray uv = f.getValue().getAsJsonObject().getAsJsonArray("uv"), sz = f.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                            double x0 = uv.get(0).getAsDouble(), y0 = uv.get(1).getAsDouble(), x1 = x0 + sz.get(0).getAsDouble(), y1 = y0 + sz.get(1).getAsDouble();
                            assertTrue(Math.min(x0, x1) >= 0 && Math.max(x0, x1) <= img.getWidth() && Math.min(y0, y1) >= 0 && Math.max(y0, y1) <= img.getHeight(), key + " " + piece + ": uv outside the sheet");
                        }
                    }
                }
                assertTrue(cubes > 0, key + " " + piece + " is empty");
                if (piece.equals("acq")) assertTrue(cubes <= 16, key + ": the Acquire piece is " + cubes + " cubes");
                else total += cubes;
                if (bones) assertTrue(Files.exists(RES.resolve("animations/entity/" + key + "_arm_" + piece + ".animation.json")), key + " " + piece + " has bones but no animation");
            }
            assertTrue(total <= 320, key + " has " + total + " cubes (cap 320)");
        }
    }

    @Test
    void everyIdleClipLoopsAndOnlyMovesBonesOfItsOwnModelByASmallAmount() throws Exception {
        int clips = 0;
        for (String stem : BeastWillFormTable.stems()) {
            String key = "bwf_" + stem;
            for (String piece : BeastWillFormTable.parts(stem).split(" ")) {
                Path an = RES.resolve("animations/entity/" + key + "_arm_" + piece + ".animation.json");
                if (!Files.exists(an)) continue;
                clips++;
                java.util.Map<String, String> parent = new java.util.HashMap<>();
                for (JsonElement be : json(RES.resolve("geo/entity/" + key + "_arm_" + piece + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                    JsonObject b = be.getAsJsonObject();
                    parent.put(b.get("name").getAsString(), b.has("parent") ? b.get("parent").getAsString() : "");
                }
                for (String bn : parent.values()) assertTrue(bn.isEmpty() || parent.containsKey(bn), key + " " + piece + ": a parent bone is missing");
                JsonObject idle = json(an).getAsJsonObject("animations").getAsJsonObject("idle");
                assertTrue(idle.get("loop").getAsBoolean() && idle.get("animation_length").getAsDouble() == 4.8, key + " " + piece + ": idle must loop over 4.8 s");
                for (var e : idle.getAsJsonObject("bones").entrySet()) {
                    assertTrue(parent.containsKey(e.getKey()), key + " " + piece + ": the clip moves a bone the model does not have: " + e.getKey());
                    for (var tr : e.getValue().getAsJsonObject().entrySet()) {
                        JsonObject keys = tr.getValue().getAsJsonObject();
                        assertEquals(25, keys.size(), key + " " + e.getKey() + " " + tr.getKey());
                        JsonArray first = keys.get("0.00").getAsJsonArray(), last = keys.get("4.80").getAsJsonArray();
                        for (int i = 0; i < 3; i++) {
                            assertEquals(first.get(i).getAsDouble(), last.get(i).getAsDouble(), 1e-3, key + " " + e.getKey() + ": the loop must close");
                            for (var k : keys.entrySet()) {
                                double v = k.getValue().getAsJsonArray().get(i).getAsDouble();
                                double lim = tr.getKey().equals("rotation") ? 40 : tr.getKey().equals("scale") ? 1.2 : 2;
                                assertTrue(tr.getKey().equals("scale") ? v > 2 - lim && v < lim : Math.abs(v) <= lim, key + " " + e.getKey() + " " + tr.getKey() + " too big: " + v);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(clips >= 27, "every will breathes, so every will has at least its body clip: " + clips);
    }

    @Test
    void thePainterAndTheSwitchAreWired() throws Exception {
        String forms = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/aura/BeastWillForms.java"));
        String aura = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/aura/BeastWillAura.java"));
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/aura/BeastWillSpiritClientConfig.java"));
        assertTrue(aura.contains("BeastWillForms.paint(") && aura.contains("BeastWillSpiritOptions.flat()"), "the aura draws the forms unless the flat switch is on");
        assertTrue(cfg.contains("\"flatSpirits\"") && cfg.contains("setFlat("), "the flat switch is a client setting");
        assertTrue(forms.contains("NoelleFormRules.budgeted(") && forms.contains("NoelleFormRules.lod("), "the crowd budget and the distance levels are used");
        assertFalse(forms.contains("Minecraft.getInstance"), "no client singleton here (the rig reads the camera)");
    }
}
