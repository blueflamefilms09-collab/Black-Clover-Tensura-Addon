package com.newuniverse.nusmp.devil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the canon looks (docs/devil_trial_model.md, "Canon looks") are 26 models, sheets and glow masks plus two renderers nobody can see in a game here, so the files and the code that read them are pinned: every
 * roster devil has its own model whose bones, uvs and clips are consistent, its own painted sheet (256 or more, all different) and glow mask; the summoning figure is the same model at the same rank scale as
 * the trial devil, with its wings as bones of the body; the pure appearance numbers behave; the options are client configs that are on by default; the server side of the figure is bounded. Pure (file reads, no
 * game); the working directory is the project root.
 */
class DevilCanonLooksTest {
    private static final File ASSETS = new File("src/main/resources/assets/anime_multiverse");
    private static final String ROOT = "src/main/java/com/newuniverse/nusmp/";

    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    private static JsonObject json(File f) throws Exception { return JsonParser.parseString(Files.readString(f.toPath(), StandardCharsets.UTF_8)).getAsJsonObject(); }

    private static JsonObject geo(String id) throws Exception { return json(new File(ASSETS, DevilCanonRules.geoPath(id))).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject(); }

    // ---------------------------------------------------------------- the roster
    @Test
    void everyRosterDevilHasACanonEntryInRosterOrderAndNothingElseDoes() {
        assertEquals(WorldDevil.count(), DevilCanonRules.IDS.length, "one canon model per roster devil");
        for (int i = 0; i < WorldDevil.count(); i++) assertEquals(WorldDevil.at(i).id(), DevilCanonRules.IDS[i], "roster order at " + i);
        assertTrue(DevilCanonRules.has("liebe") && DevilCanonRules.has("god_hand"));
        assertFalse(DevilCanonRules.has("") || DevilCanonRules.has(null) || DevilCanonRules.has("a_pack_devil"), "an unknown devil keeps the generic brute");
        assertEquals("textures/entity/devil_canon_liebe.png", DevilCanonRules.texturePath("liebe"));
        assertEquals("textures/entity/devil_canon_liebe_glowmask.png", DevilCanonRules.glowPath("liebe"));
        assertEquals("geo/entity/devil_canon_liebe.geo.json", DevilCanonRules.geoPath("liebe"));
        assertEquals("animations/entity/devil_canon_liebe.animation.json", DevilCanonRules.animationPath("liebe"));
    }

    // ---------------------------------------------------------------- the geometry
    @Test
    void everyCanonModelIsWellFormedAndWithinItsDrawBudget() throws Exception {
        for (String id : DevilCanonRules.IDS) {
            JsonObject g = geo(id);
            JsonObject desc = g.getAsJsonObject("description");
            assertEquals("geometry." + DevilCanonRules.PREFIX + id, desc.get("identifier").getAsString(), id);
            int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
            assertTrue(tw >= 256 && tw == th, id + " sheet size " + tw + "x" + th);
            Set<String> names = new HashSet<>();
            JsonArray bones = g.getAsJsonArray("bones");
            for (JsonElement e : bones) names.add(e.getAsJsonObject().get("name").getAsString());
            assertEquals(bones.size(), names.size(), id + ": bone names are unique");
            int cubes = 0;
            for (JsonElement e : bones) {
                JsonObject b = e.getAsJsonObject();
                if (b.has("parent")) assertTrue(names.contains(b.get("parent").getAsString()), id + ": " + b.get("name") + " has an unknown parent");
                if (!b.has("cubes")) continue;
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    cubes++;
                    JsonObject uvs = ce.getAsJsonObject().getAsJsonObject("uv");
                    for (String face : new String[]{"north", "south", "east", "west", "up", "down"}) {
                        JsonObject f = uvs.getAsJsonObject(face);
                        assertNotNull(f, id + ": a cube lacks the uv of " + face);
                        double u = f.getAsJsonArray("uv").get(0).getAsDouble(), v = f.getAsJsonArray("uv").get(1).getAsDouble();
                        double w = f.getAsJsonArray("uv_size").get(0).getAsDouble(), h = f.getAsJsonArray("uv_size").get(1).getAsDouble();
                        assertTrue(u >= 0 && v >= 0 && u + w <= tw && v + h <= th, id + ": uv outside the sheet in " + b.get("name"));
                    }
                }
            }
            assertTrue(cubes >= 40, id + " has too few cubes to be a character: " + cubes);
            assertTrue(cubes * 6 <= 1100, id + ": at most 1100 quads per devil (at most trial.max = 4 and npc.max = 8 exist), it has " + cubes * 6);
            for (String must : new String[]{"root", "body", "head"}) assertTrue(names.contains(must), id + " lacks the bone " + must);
        }
    }

    @Test
    void theClipsExistAndOnlyMoveBonesTheModelHas() throws Exception {
        for (String id : DevilCanonRules.IDS) {
            Set<String> bones = new HashSet<>();
            for (JsonElement e : geo(id).getAsJsonArray("bones")) bones.add(e.getAsJsonObject().get("name").getAsString());
            JsonObject anims = json(new File(ASSETS, DevilCanonRules.animationPath(id))).getAsJsonObject("animations");
            for (String clip : new String[]{DevilCanonRules.CLIP_IDLE, DevilCanonRules.CLIP_WALK, DevilCanonRules.CLIP_ATTACK, DevilCanonRules.CLIP_RISE, DevilCanonRules.CLIP_ROAR}) {
                assertTrue(anims.has(clip), id + " lacks the clip " + clip);
                JsonObject c = anims.getAsJsonObject(clip);
                assertTrue(c.get("animation_length").getAsDouble() > 0, id + " " + clip);
                for (String b : c.getAsJsonObject("bones").keySet()) assertTrue(bones.contains(b), id + " " + clip + " moves the unknown bone " + b);
            }
            assertTrue(anims.getAsJsonObject(DevilCanonRules.CLIP_IDLE).get("loop").getAsBoolean(), id + ": idle loops");
            assertFalse(anims.getAsJsonObject(DevilCanonRules.CLIP_RISE).get("loop").getAsBoolean(), id + ": the rise plays once");
            assertFalse(anims.getAsJsonObject(DevilCanonRules.CLIP_ROAR).get("loop").getAsBoolean(), id + ": the roar plays once");
            double rise = anims.getAsJsonObject(DevilCanonRules.CLIP_RISE).get("animation_length").getAsDouble() * 20;
            double roar = anims.getAsJsonObject(DevilCanonRules.CLIP_ROAR).get("animation_length").getAsDouble() * 20;
            assertTrue(Math.abs(rise - DevilNpcRules.RISE_TICKS) <= 2, id + ": the rise clip (" + rise + " ticks) lasts as long as the figure's RISING state");
            assertTrue(Math.abs(roar - DevilNpcRules.ROAR_TICKS) <= 2, id + ": the roar clip (" + roar + " ticks) lasts as long as the roar of the offer");
        }
        JsonObject brute = json(new File(ASSETS, "animations/entity/devil_trial.animation.json")).getAsJsonObject("animations");
        assertTrue(brute.has(DevilCanonRules.CLIP_RISE) && brute.has(DevilCanonRules.CLIP_ROAR), "the generic brute (the fallback figure) has the two clips too");
    }

    // ---------------------------------------------------------------- the sheets
    @Test
    void everyCanonSheetIsPaintedAtLeast256AndEveryGlowMaskHasLitTexels() throws Exception {
        File tex = new File(ASSETS, "textures/entity");
        Set<String> hashes = new HashSet<>();
        for (String id : DevilCanonRules.IDS) {
            File f = new File(tex, DevilCanonRules.PREFIX + id + ".png"), g = new File(tex, DevilCanonRules.PREFIX + id + "_glowmask.png");
            assertTrue(f.isFile() && g.isFile(), "missing sheet or glow mask of " + id);
            BufferedImage img = ImageIO.read(f), glow = ImageIO.read(g);
            assertTrue(img.getWidth() >= 256 && img.getWidth() == img.getHeight(), id + " sheet size");
            assertEquals(img.getWidth(), glow.getWidth(), id + " glow mask has the sheet's size");
            assertEquals(img.getHeight(), glow.getHeight(), id + " glow mask has the sheet's size");
            int opaque = 0, step = 3;
            for (int y = 0; y < img.getHeight(); y += step) for (int x = 0; x < img.getWidth(); x += step) if ((img.getRGB(x, y) >>> 24) > 0) opaque++;
            assertTrue(opaque > 1200, id + " sheet is mostly empty");
            int lit = 0;
            for (int y = 0; y < glow.getHeight(); y++) for (int x = 0; x < glow.getWidth(); x++) if ((glow.getRGB(x, y) >>> 24) > 0) lit++;
            assertTrue(lit >= 8 && lit < 8000, id + " glow texels " + lit + " (a few: eyes, brands, gems, horn tips)");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Files.readAllBytes(f.toPath()));
            assertTrue(hashes.add(java.util.HexFormat.of().formatHex(md.digest())), id + " has the same sheet as another devil");
        }
        assertEquals(DevilCanonRules.IDS.length, hashes.size());
    }

    @Test
    void theModelsAreDistinctFromEachOther() throws Exception {
        Set<String> shapes = new HashSet<>();
        for (String id : DevilCanonRules.IDS) shapes.add(Files.readString(new File(ASSETS, DevilCanonRules.geoPath(id)).toPath()).replace(id, ""));
        assertEquals(DevilCanonRules.IDS.length, shapes.size(), "no two devils share a geometry");
    }

    @Test
    void theBlockbenchFormatSourcesAreKeptAndTheGeneratorSaysItIsTheFallback() throws Exception {
        for (String id : DevilCanonRules.IDS) {
            File f = new File("tools/blockbench/devil_canon/" + id + ".bbmodel");
            assertTrue(f.isFile(), "the .bbmodel source of " + id + " is kept (rule 15)");
            JsonObject bb = json(f);
            assertEquals(DevilCanonRules.PREFIX + id, bb.get("name").getAsString());
            assertEquals(5, bb.getAsJsonArray("animations").size(), id + ": idle, walk, attack, rise, roar");
            assertTrue(bb.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString().startsWith("data:image/png;base64,"), id + ": the sheet is embedded");
        }
        String gen = read("tools/blockbench/gen_devil_canon.py");
        assertTrue(gen.contains("could not run") && gen.contains("FALLBACK") && gen.contains("NOT opened in Blockbench"), "the generator says plainly it is the fallback (rule 15)");
    }

    // ---------------------------------------------------------------- the renderers
    @Test
    void theTrialAndTheSummoningFigureShareOneModelOneSheetAndOneRankScale() throws Exception {
        String trial = read(ROOT + "client/devil/DevilTrialGeoRenderer.java"), npc = read(ROOT + "client/devil/DevilNpcGeoRenderer.java");
        assertTrue(trial.contains("DevilCanonAssets.model(") && trial.contains("DevilCanonAssets.texture(") && trial.contains("DevilCanonAssets.animation("), "the trial devil picks the canon assets");
        assertTrue(trial.contains("return DevilCanonRules.rankScale(id)") && npc.contains("DevilCanonRules.rankScale(e.devilId())"), "one rank scale for both");
        assertTrue(npc.contains("DevilTrialGeoRenderer.modelOf(") && npc.contains("DevilTrialGeoRenderer.textureOf(") && npc.contains("DevilTrialGeoRenderer.animationOf("), "the figure reads the very assets the trial devil reads");
        assertTrue(read(ROOT + "client/devil/DevilCanonAssets.java").contains("PlayerModelOptions.devilCanonLooks()"), "the canon models are behind the option");
        assertEquals(1.08f, DevilCanonRules.rankScale("lucifero"), 0f);
        assertEquals(1.0f, DevilCanonRules.rankScale("liebe"), 0f);
        assertEquals(0.95f, DevilCanonRules.rankScale("fox_devil"), 0f);
        assertEquals(0.9f, DevilCanonRules.rankScale("kin_of_astaroth"), 0f);
        assertEquals(1.0f, DevilCanonRules.rankScale("unknown"), 0f);
    }

    @Test
    void theSummoningFigureIsAGeoModelWithItsWingsAsBonesAndTheOldFigureIsKeptBehindTheOption() throws Exception {
        String npc = read(ROOT + "client/devil/DevilNpcGeoRenderer.java"), old = read(ROOT + "client/devil/DevilNpcRenderer.java");
        assertTrue(npc.contains("extends GeoEntityRenderer<DevilNpc>") && npc.contains("AutoGlowingGeoLayer"), "a GeckoLib figure with the glow layer");
        assertTrue(npc.contains("180f - Mth.rotLerp(partial, e.yRotO, e.getYRot())"), "it faces the way the figure was turned (the library turns a plain entity by nothing)");
        assertFalse(npc.contains("Minecraft.getInstance()"), "no static game access");
        String render = npc.substring(npc.indexOf("public void render("), npc.indexOf("private static final class FadingGlow"));
        assertFalse(render.contains(" new "), "the figure's render allocates nothing per frame");
        assertTrue(npc.contains("DevilCanonRules.sink(") && npc.contains("DevilCanonRules.shrink(") && npc.contains("DevilCanonRules.slide("), "rise, merge and dissolve are drawn");
        assertTrue(old.contains("PlayerModelOptions.devilRitualFigure()") && old.contains("figure.render("), "the new figure is chosen by the option");
        assertTrue(old.contains("this.body.young = false") && old.contains("this.slimBody.young = false"), "the player models are no longer a child's (the 'child under wings' bug)");
        assertTrue(old.contains("class DevilNpcRenderer") && old.contains("drawLook(") && old.contains("fallback("), "the earlier figure stays (rule 2)");
        assertTrue(read(ROOT + "client/devil/DevilClient.java").contains("DevilNpcRenderer::new"), "the renderer is registered");
        for (String wing : new String[]{"wing_r", "wing_l"}) {
            int found = 0;
            for (String id : DevilCanonRules.IDS) if (read(ASSETS.getPath() + "/" + DevilCanonRules.geoPath(id)).contains("\"" + wing + "\"")) found++;
            assertTrue(found >= 14, "most devils have the bone " + wing + " (attached to the body, not drawn on top), " + found);
        }
    }

    @Test
    void theOptionsAreClientConfigsOnByDefaultWithTheirTexts() throws Exception {
        String o = read(ROOT + "client/playermodel/PlayerModelOptions.java");
        assertTrue(o.contains(".define(\"devilCanonLooks\", true)") && o.contains(".define(\"devilRitualFigure\", true)"), "both default on");
        assertTrue(o.contains("public static boolean devilCanonLooks()") && o.contains("public static boolean devilRitualFigure()"));
        String en = read("src/main/resources/assets/anime_multiverse/lang/en_us.json"), frag = read("tools/lang/devil_canon.json");
        for (String k : new String[]{"devilCanonLooks", "devilCanonLooks.tooltip", "devilRitualFigure", "devilRitualFigure.tooltip"}) {
            String key = "anime_multiverse.configuration." + k;
            assertTrue(frag.contains("\"" + key + "\""), key + " in the fragment");
            assertTrue(en.contains("\"" + key + "\""), key + " in en_us.json");
        }
    }

    // ---------------------------------------------------------------- the pure numbers
    @Test
    void theFigureFadesInRisesOutOfTheCircleAndMergesOrDissolvesAway() {
        int r = DevilNpcRules.RISING, w = DevilNpcRules.WAITING, o = DevilNpcRules.OFFER, m = DevilNpcRules.MERGE, d = DevilNpcRules.DISSOLVE;
        assertEquals(0f, DevilCanonRules.fade(r, 0f), 0f);
        assertEquals(1f, DevilCanonRules.fade(r, DevilNpcRules.RISE_TICKS / 2f), 1e-6f);
        assertEquals(1f, DevilCanonRules.fade(r, 1000f), 0f);
        float prev = -1f;
        for (int t = 0; t <= DevilNpcRules.RISE_TICKS; t++) {
            float f = DevilCanonRules.fade(r, t);
            assertTrue(f >= prev, "the fade in never goes back");
            prev = f;
        }
        assertEquals(DevilCanonRules.SINK_BLOCKS, DevilCanonRules.sink(r, 0f), 1e-6f);
        assertEquals(0f, DevilCanonRules.sink(r, DevilNpcRules.RISE_TICKS), 1e-6f);
        assertEquals(0f, DevilCanonRules.sink(w, 0f), 0f);
        assertEquals(1f, DevilCanonRules.fade(w, 5f), 0f);
        assertEquals(1f, DevilCanonRules.fade(o, 5f), 0f);
        assertEquals(1f, DevilCanonRules.fade(m, 0f), 0f);
        assertEquals(0f, DevilCanonRules.fade(m, DevilNpcRules.MERGE_TICKS), 1e-6f);
        assertEquals(0f, DevilCanonRules.fade(d, DevilNpcRules.DISSOLVE_TICKS * 2f), 1e-6f);
        assertEquals(0.4f, DevilCanonRules.shrink(m, DevilNpcRules.MERGE_TICKS), 1e-6f);
        assertEquals(1f, DevilCanonRules.shrink(d, 10f), 0f);
        assertEquals(0f, DevilCanonRules.slide(r, 10f), 0f);
        assertEquals(1f, DevilCanonRules.slide(m, DevilNpcRules.MERGE_TICKS), 1e-6f);
        assertEquals(0, DevilCanonRules.fadeLevel(-0.5f, 32));
        assertEquals(32, DevilCanonRules.fadeLevel(7f, 32));
        assertEquals(16, DevilCanonRules.fadeLevel(0.5f, 32));
    }

    @Test
    void theAppearanceEffectIsFewParticlesOnFewTicksAndNothingOutsideTheRise() {
        int ticks = 0, total = 0, max = 0;
        for (int t = -3; t < 200; t++) {
            int n = DevilCanonRules.riseBurst(t);
            if (n > 0) ticks++;
            total += n;
            max = Math.max(max, n);
            if (t <= 0 || t > DevilNpcRules.RISE_TICKS) assertEquals(0, n, "nothing outside the rise, tick " + t);
        }
        assertEquals(DevilNpcRules.RISE_TICKS / DevilCanonRules.FX_EVERY, ticks, "one burst every FX_EVERY ticks");
        assertEquals(DevilCanonRules.riseParticlesTotal(), total);
        assertTrue(total <= 80, "at most 80 dust particles (and half as many smoke) per ritual, it is " + total);
        assertEquals(DevilCanonRules.FX_LAND, max, "the larger burst is the last one");
        assertEquals(DevilCanonRules.FX_LAND, DevilCanonRules.riseBurst(DevilNpcRules.RISE_TICKS));
    }

    @Test
    void theFigureKeepsItsServerRulesAndAnimatesFromItsSyncedState() throws Exception {
        String e = read(ROOT + "devil/DevilNpc.java");
        assertTrue(e.contains("implements GeoEntity") && e.contains("GeckoLibUtil.createInstanceCache(this)"), "a GeckoLib entity");
        assertTrue(e.contains("shouldBeSaved() { return false; }") && e.contains("isInvulnerableTo(DamageSource source) { return true; }") && e.contains("DevilNpcs.check(this)"), "never saved, untouchable, still checked");
        assertTrue(e.contains("PerfParticles.send(") && !e.contains("sendParticles("), "the appearance goes through the particle budget");
        assertTrue(e.contains("riseFx();") && e.contains("DevilCanonRules.riseBurst(stateTicks)"), "the effect only runs during the rise, on the server");
        assertTrue(e.contains("case DevilNpcRules.RISING -> RISE") && e.contains("case DevilNpcRules.OFFER -> ROAR"), "the clips follow the state");
        assertFalse(e.contains("net.minecraft.client") || e.contains("com.newuniverse.nusmp.client"), "no client class in common code (rule 9)");
        assertEquals(30, DevilNpcRules.RISE_TICKS, "the rise lasts as before");
    }
}
