package com.newuniverse.nusmp.firecast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The files the fire casting styles need: the Salamander models (three levels and a glow-only model per form) with their sheets and animations, the cast clips, Magna's Soul Bat (3D model, class, grip, Better Combat, engravings, franchise), the lang text, the previews. */
class FireCastAssetsTest {
    private static final Path RES = Path.of("src", "main", "resources"), ASSETS = RES.resolve("assets/nusmp");
    private static final String BAT = "magnas_soul_bat";

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static JsonObject json(Path p) throws IOException { return JsonParser.parseString(read(p)).getAsJsonObject(); }

    private static JsonObject geo(String id) throws IOException {
        return json(ASSETS.resolve("geo/entity/" + id + ".geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static Map<String, JsonObject> bones(JsonObject geo) {
        Map<String, JsonObject> out = new HashMap<>();
        for (JsonElement b : geo.getAsJsonArray("bones")) out.put(b.getAsJsonObject().get("name").getAsString(), b.getAsJsonObject());
        return out;
    }

    // ------------------------------------------------------------------ the Salamander
    @Test
    void everyModelOfBothFormsIsWellFormedAndHasItsOwnSheet() throws IOException {
        for (String form : new String[]{"salamander", "dragon"}) {
            int fullW = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/firecast_" + form + ".png").toFile()).getWidth();
            for (String suffix : new String[]{"", "_lod1", "_lod2", "_fx"}) {
                String id = "firecast_" + form + suffix;
                JsonObject g = geo(id);
                JsonObject d = g.getAsJsonObject("description");
                int tw = d.get("texture_width").getAsInt(), th = d.get("texture_height").getAsInt();
                BufferedImage tex = javax.imageio.ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
                assertEquals(tex.getWidth(), tw, id + " declares the size of its own sheet");
                assertEquals(tex.getHeight(), th, id);
                int want = suffix.equals("") ? fullW : suffix.equals("_lod1") ? fullW / 2 : suffix.equals("_lod2") ? fullW / 4 : fullW / 2;
                assertEquals(want, tw, id + ": a lower level of detail reads a smaller copy of the sheet; the glow-only model the half size glow sheet");
                assertFalse(Files.exists(ASSETS.resolve("textures/entity/" + id + "_glow.png")), id + " has no separate glow map (the glow-only model has its own sheet)");
                Map<String, JsonObject> bones = bones(g);
                for (JsonObject b : bones.values()) {
                    if (b.has("parent")) assertTrue(bones.containsKey(b.get("parent").getAsString()), id + " bone " + b.get("name") + " has a parent");
                    for (JsonElement ce : b.getAsJsonArray("cubes")) {
                        for (var face : ce.getAsJsonObject().getAsJsonObject("uv").entrySet()) {
                            JsonObject f = face.getValue().getAsJsonObject();
                            JsonArray uv = f.getAsJsonArray("uv"), sz = f.getAsJsonArray("uv_size");
                            double u0 = uv.get(0).getAsDouble(), v0 = uv.get(1).getAsDouble(), u1 = u0 + sz.get(0).getAsDouble(), v1 = v0 + sz.get(1).getAsDouble();
                            assertTrue(Math.min(u0, u1) >= -0.01 && Math.max(u0, u1) <= tw + 0.01 && Math.min(v0, v1) >= -0.01 && Math.max(v0, v1) <= th + 0.01, id + " uv inside the sheet: " + uv + " " + sz);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theLodAndGlowModelsUseTheBonesOfTheFullModelUnchanged() throws IOException {
        for (String form : new String[]{"salamander", "dragon"}) {
            Map<String, JsonObject> full = bones(geo("firecast_" + form));
            for (String suffix : new String[]{"_lod1", "_lod2", "_fx"}) {
                for (var e : bones(geo("firecast_" + form + suffix)).entrySet()) {
                    JsonObject f = full.get(e.getKey());
                    assertNotNull(f, form + suffix + " bone " + e.getKey() + " exists in the full model");
                    if (suffix.equals("_fx")) {                                                      // the glow-only model is posed exactly like the full one; a LOD has a shorter neck and tail, its own pivots
                        assertEquals(f.getAsJsonArray("pivot"), e.getValue().getAsJsonArray("pivot"), form + suffix + " pivot of " + e.getKey() + " (so the one animation file drives it)");
                        if (suffix.equals("_fx")) assertEquals(f.has("parent") ? f.get("parent") : null, e.getValue().has("parent") ? e.getValue().get("parent") : null, form + suffix + " parent of " + e.getKey());
                    }
                }
            }
        }
    }

    @Test
    void theAnimationFilesHaveTheFourClipsAndDriveRealBones() throws IOException {
        for (String form : new String[]{"salamander", "dragon"}) {
            JsonObject anim = json(ASSETS.resolve("animations/entity/firecast_" + form + ".animation.json")).getAsJsonObject("animations");
            Map<String, JsonObject> bones = bones(geo("firecast_" + form));
            for (String clip : new String[]{"idle", "perch", "fly", "breathe"}) {
                assertTrue(anim.has(clip), form + " clip " + clip);
                JsonObject c = anim.getAsJsonObject(clip);
                double len = c.get("animation_length").getAsDouble();
                assertTrue(len > 0);
                for (var be : c.getAsJsonObject("bones").entrySet()) {
                    assertTrue(bones.containsKey(be.getKey()), form + "/" + clip + " bone " + be.getKey());
                    for (var track : be.getValue().getAsJsonObject().entrySet())
                        for (var key : track.getValue().getAsJsonObject().entrySet()) assertTrue(Double.parseDouble(key.getKey()) <= len + 1e-6, form + "/" + clip + " keyframe " + key.getKey() + " is inside the clip");
                }
            }
            assertTrue(anim.getAsJsonObject("idle").get("loop").getAsBoolean() && anim.getAsJsonObject("perch").get("loop").getAsBoolean() && anim.getAsJsonObject("fly").get("loop").getAsBoolean());
            assertFalse(anim.getAsJsonObject("breathe").get("loop").getAsBoolean(), "the breath plays once");
        }
    }

    @Test
    void theWingsAndTailAreMovedByBoneRotationOnly() throws IOException {
        JsonObject anim = json(ASSETS.resolve("animations/entity/firecast_dragon.animation.json")).getAsJsonObject("animations");
        for (String clip : new String[]{"idle", "perch", "fly", "breathe"})
            for (var be : anim.getAsJsonObject(clip).getAsJsonObject("bones").entrySet())
                if (be.getKey().startsWith("wing") || be.getKey().startsWith("t")) assertTrue(be.getValue().getAsJsonObject().has("rotation"), clip + " " + be.getKey());
    }

    // ------------------------------------------------------------------ the redo after the reference stills (rig, cards, kept old models)
    private static double[] arr3(JsonArray a) { return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()}; }

    /** The flat cards of a model: cubes with one size component 0 (one quad each: spikes, claws, fangs, the wing membrane panels, the frill, the eyes). */
    private static int cards(JsonObject geo, String boneStartsWith) {
        int n = 0;
        for (JsonElement be : geo.getAsJsonArray("bones")) {
            JsonObject b = be.getAsJsonObject();
            if (!b.get("name").getAsString().startsWith(boneStartsWith)) continue;
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                double[] sz = arr3(ce.getAsJsonObject().getAsJsonArray("size"));
                if (sz[0] == 0 || sz[1] == 0 || sz[2] == 0) {
                    n++;
                    assertEquals(1, ce.getAsJsonObject().getAsJsonObject("uv").size(), "a card is one quad");
                }
            }
        }
        return n;
    }

    @Test
    void theOldModelsAreKeptAsV1CopiesAfterTheRedo() throws IOException {
        for (String form : new String[]{"salamander", "dragon"}) {
            for (String f : new String[]{"geo/entity/firecast_" + form + "_v1.geo.json", "animations/entity/firecast_" + form + "_v1.animation.json", "textures/entity/firecast_" + form + "_v1.png"})
                assertTrue(Files.exists(ASSETS.resolve(f)), "kept old version: " + f);
            assertFalse(read(ASSETS.resolve("geo/entity/firecast_" + form + ".geo.json")).equals(read(ASSETS.resolve("geo/entity/firecast_" + form + "_v1.geo.json"))), "the live model is the new one");
        }
    }

    @Test
    void bothFormsHaveTheNewRigWithWingFingersAndTailChain() throws IOException {
        for (String form : new String[]{"salamander", "dragon"}) {
            Map<String, JsonObject> b = bones(geo("firecast_" + form));
            for (String n : new String[]{"body", "head", "jaw", "neck1", "tail_tip", "t0", "t2", "t4", "t6"}) assertTrue(b.containsKey(n), form + " bone " + n);
            for (String side : new String[]{"R", "L"}) {
                for (String n : new String[]{"arm_", "fore_", "hand_", "leg_", "shin_", "foot_", "wing_", "wingout_", "wf1_", "wf2_", "wf3_", "wf4_"}) assertTrue(b.containsKey(n + side), form + " bone " + n + side);
            }
            // the wings are mirrored and raised: the right wing turns one way, the left the other
            double rz = arr3(b.get("wing_R").getAsJsonArray("rotation"))[2], lz = arr3(b.get("wing_L").getAsJsonArray("rotation"))[2];
            assertTrue(rz < 0 && lz > 0 && Math.abs(rz + lz) < 1e-6, form + " wings raised in mirror");
        }
        Map<String, JsonObject> big = bones(geo("firecast_dragon"));
        for (String n : new String[]{"chest", "neck2", "neck3", "neck4", "t13"}) assertTrue(big.containsKey(n), "the big form's longer neck and tail: " + n);
        assertTrue(arr3(big.get("jaw").getAsJsonArray("rotation"))[0] < 0, "the big form's mouth is open at rest (the roar of the still)");
    }

    @Test
    void theSilhouetteIsBuiltFromFlatCardsAndEveryWingHasFourMembranePanelsAndFourFingerBones() throws IOException {
        for (String form : new String[]{"salamander", "dragon"}) {
            JsonObject g = geo("firecast_" + form);
            assertTrue(cards(g, "") >= (form.equals("dragon") ? 90 : 90), form + ": spikes, claws, fangs, membrane panels and eyes are cards");
            Map<String, JsonObject> b = bones(g);
            for (String side : new String[]{"R", "L"}) {
                int panels = cards(g, "wing_" + side) + cards(g, "wf1_" + side) + cards(g, "wf2_" + side) + cards(g, "wf3_" + side) + cards(g, "wf4_" + side);
                assertTrue(panels >= 4, form + " " + side + ": four membrane panels (plus claws): " + panels);
                for (int k = 1; k <= 4; k++) {
                    int boxes = 0;
                    for (JsonElement ce : b.get("wf" + k + "_" + side).getAsJsonArray("cubes")) {
                        double[] sz = arr3(ce.getAsJsonObject().getAsJsonArray("size"));
                        if (sz[0] > 0 && sz[1] > 0 && sz[2] > 0) boxes++;
                    }
                    assertTrue(boxes >= 1, form + " finger bone " + k + side + " is a real box");
                }
            }
        }
    }

    @Test
    void theLevelsOfDetailKeepTheSilhouetteAndTheGlowModelsKeepEyesAndMouth() throws IOException {
        for (String form : new String[]{"salamander", "dragon"}) {
            Map<String, JsonObject> lod1 = bones(geo("firecast_" + form + "_lod1")), lod2 = bones(geo("firecast_" + form + "_lod2"));
            for (String n : new String[]{"wing_R", "wing_L", "head", "jaw", "tail_tip", "t0"}) assertTrue(lod1.containsKey(n) && lod2.containsKey(n), form + " levels keep " + n);
            assertTrue(cards(geo("firecast_" + form + "_lod1"), "") >= 20, form + " half level keeps spikes, claws and panels");
            Map<String, JsonObject> fx = bones(geo("firecast_" + form + "_fx"));
            assertEquals(2, fx.get("head").getAsJsonArray("cubes").size(), form + " glow-only: two eyes");
            assertEquals(1, fx.get("jaw").getAsJsonArray("cubes").size(), form + " glow-only: the furnace of the mouth");
        }
    }

    @Test
    void theCastClipsAreAddedAndTheOldOnesAreKept() throws IOException {
        JsonObject all = json(ASSETS.resolve("animations/player/cast.animation.json")).getAsJsonObject("animations");
        for (String clip : new String[]{"fire_bat_swing", "fire_punch_a", "fire_punch_b", "fire_punch_finisher", "fire_charge", "fire_charge_release"}) assertTrue(all.has(clip), clip);
        for (String clip : new String[]{"cast_chant", "cast_mana_zone", "cast_release_thrust", "cast_release_sweep", "cast_release_side", "cast_release_up", "cast_release_slam", "cast_signature", "cast_fail"})
            assertTrue(all.has(clip), "kept: " + clip);
        assertTrue(all.getAsJsonObject("fire_charge").get("loop").getAsBoolean(), "the charge loops until the key is released");
        // the bat swing's contact keyframe is the tick the server tests the swing on
        JsonObject body = all.getAsJsonObject("fire_bat_swing").getAsJsonObject("bones").getAsJsonObject("body").getAsJsonObject("rotation");
        assertTrue(body.has(String.format("%.2f", FireCastRules.SWING_IMPACT_TICK / 20.0)), "a body keyframe at the contact tick " + FireCastRules.SWING_IMPACT_TICK);
        assertEquals(FireCastRules.SWING_CLIP_TICKS, Math.round(all.getAsJsonObject("fire_bat_swing").get("animation_length").getAsDouble() * 20), "the clip is 16 ticks long (0.8 s)");
    }

    // ------------------------------------------------------------------ the bat
    @Test
    void theBatHasItsModelsMeshSkinsAndIcon() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String f : new String[]{"models/item/" + BAT + ".json", "models/item/" + BAT + "_icon.json", "weapon_meshes/" + BAT + ".txt", "textures/entity/weapon/" + BAT + ".png",
                "textures/entity/weapon/" + BAT + "_glow.png", "textures/item/" + BAT + ".png"})
            if (!Files.exists(ASSETS.resolve(f))) missing.add(f);
        assertTrue(missing.isEmpty(), "missing " + missing);
        assertEquals("minecraft:builtin/entity", json(ASSETS.resolve("models/item/" + BAT + ".json")).get("parent").getAsString(), "the 3D model draws through the weapon renderer");
        assertEquals("nusmp:item/" + BAT, json(ASSETS.resolve("models/item/" + BAT + "_icon.json")).getAsJsonObject("textures").get("layer0").getAsString(), "the old sprite is the inventory icon");
        BufferedImage icon = javax.imageio.ImageIO.read(ASSETS.resolve("textures/item/" + BAT + ".png").toFile());
        assertEquals(16, icon.getWidth());
        assertEquals(16, icon.getHeight());
    }

    @Test
    void theMeshIsAClubHeldOnItsGrip() throws IOException {
        String mesh = read(ASSETS.resolve("weapon_meshes/" + BAT + ".txt"));
        assertTrue(mesh.contains("\ngripxyz "), "the hand holds the wrapped grip: a gripxyz line");
        assertTrue(mesh.lines().filter(l -> l.startsWith("qn ")).count() > 3000, "a real model: thousands of quads");
        assertTrue(mesh.lines().anyMatch(l -> l.startsWith("glow ")), "an additive glow skin");
        assertTrue(read(ASSETS.resolve("weapon_classes.json")).contains("\"" + BAT + "\": \"club\""), "class row (game copy)");
        assertTrue(read(Path.of("tools", "relics", "weapon_classes.json")).contains("\"" + BAT + "\": {"), "class row (tools copy)");
        String golden = read(Path.of("tools", "weapon_grip", "grip_golden.json"));
        int at = golden.indexOf("\"" + BAT + "\"");
        assertTrue(at > 0, "the grip golden knows the bat");
        assertTrue(golden.substring(at, at + 200).contains("\"source\": \"gripxyz\""), "the grip comes from the mesh's own point");
    }

    @Test
    void betterCombatKnowsTheBatAsAClub() throws IOException {
        JsonObject bc = json(RES.resolve("data/nusmp/weapon_attributes/" + BAT + ".json"));
        assertEquals("bettercombat:mace", bc.get("parent").getAsString());
        String manifest = read(Path.of("tools", "bettercombat", "manifest.json"));
        int at = manifest.indexOf("\"" + BAT + "\": {");
        assertTrue(at > 0);
        assertTrue(manifest.substring(at, at + 120).contains("\"class\": \"club\""));
    }

    @Test
    void theBatIsBlackCloverAndEngravedWithTensuraEngravings() throws IOException {
        assertTrue(read(RES.resolve("data/nusmp/relics/franchise_map.json")).contains("\"" + BAT + "\": \"black_clover\""));
        String eng = read(Path.of("src", "main", "java", "com", "newuniverse", "nusmp", "item", "WeaponEngravings.java"));
        int at = eng.indexOf("Map.entry(\"" + BAT + "\"");
        assertTrue(at > 0);
        String line = eng.substring(at, eng.indexOf('\n', at));
        for (String e : new String[]{"crushing", "elemental_boost", "magic_weapon", "sturdy"}) assertTrue(line.contains("e(\"" + e + "\""), e);
    }

    // ------------------------------------------------------------------ text and pictures
    @Test
    void theLangFragmentHasEveryString() throws IOException {
        JsonObject lang = json(Path.of("tools", "lang", "fire_casting.json"));
        for (String k : new String[]{"item.nusmp." + BAT, "item.nusmp." + BAT + ".lore", "item.nusmp." + BAT + ".use", "key.nusmp.fire_salamander", "nusmp.firecast.spent", "nusmp.firecast.spirit.full",
                "nusmp.firecast.spirit.linked", "nusmp.firecast.spirit.unlinked"})
            assertTrue(lang.has(k), k);
        assertEquals("Magna's Soul Bat", lang.get("item.nusmp." + BAT).getAsString());
        for (var e : lang.entrySet()) {
            String v = e.getValue().getAsString().toLowerCase();
            assertFalse(v.contains("hunger") || v.contains("starv") || v.contains("food"), e.getKey());
        }
    }

    @Test
    void theHeadlessPreviewScenesAndPicturesAreThere() throws IOException {
        JsonObject props = json(Path.of("tools", "render_preview", "scenes.d", "firecast_props.json"));
        for (String s : new String[]{"firecast_shoulder", "firecast_shoulder_breathe", "firecast_dragon_over_caster", "firecast_dragon_breath", "firecast_spiral_1", "firecast_spiral_4", "firecast_ball"})
            assertTrue(props.has(s), s);
        for (String f : new String[]{"magnas_soul_bat.png", "salamander_small.png", "salamander_dragon.png", "salamander_vs_reference.png", "spiral_levels.png", "tossed_ball.png", "punch_finisher.png"})
            assertTrue(Files.exists(Path.of("docs", "previews", "fire_casting", f)), f);
        assertTrue(Files.exists(Path.of("docs", "magic", "fire_casting.md")));
    }
}
