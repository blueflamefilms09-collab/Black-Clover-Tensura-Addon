package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the ten Hercules effects (Lord of Martial Prowess) are drawn by {@code HerculesLayer}. The enum ordinals go over the network, so the ten shapes must stay appended right after SOLOMON_ABSORB in this order; the
 * layer must draw all ten and give each a duration; every texture it names must exist (a missing one is a magenta quad in game) and be a power-of-two size (or an expected 2:1 .. 8:1 strip or atlas); the layer stays inside
 * the rule-11 render limits (static textures, no allocation of collections or locations per frame, the long-running re-sent Invictus asks for a small vertex budget of its own, every budget is within the 400-vertex
 * activation cap, optional groups check the buffer budget, far things are left out by distance, every loop count is bounded) and it shares nothing with the Solomon, Jibril, Sandalphon, Trial or Time Magic looks. Pure:
 * reads the real sources and PNGs, the working directory is the project root.
 */
class HerculesShapesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer/HerculesLayer.java");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static final List<String> TEN = List.of("HERCULES_LEAP", "HERCULES_TREMOR", "HERCULES_INVICTUS", "HERCULES_GRIP", "HERCULES_THROW", "HERCULES_ROAR", "HERCULES_STRIKE", "HERCULES_SHOCKWAVE", "HERCULES_BODY", "HERCULES_MOMENTUM");

    private static String layer() throws IOException { return Files.readString(LAYER, StandardCharsets.UTF_8); }

    private static String code() throws IOException { return layer().replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    @Test
    void theTenShapesFollowSolomonAbsorbInOrder() {
        VfxShape[] all = VfxShape.values();
        int at = VfxShape.SOLOMON_ABSORB.ordinal();
        for (int i = 0; i < TEN.size(); i++) {
            assertTrue(at + 1 + i < all.length, "missing " + TEN.get(i));
            assertEquals(TEN.get(i), all[at + 1 + i].name(), "the shapes are append-only (ordinals go over the network)");
        }
        for (String s : TEN) assertEquals(s, VfxShape.valueOf(s).name());
    }

    @Test
    void theLayerDrawsAllTenAndGivesEachADuration() throws IOException {
        String src = layer();
        Matcher m = Pattern.compile("public Set<VfxShape> shapes\\(\\) \\{(.*?)\\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "HerculesLayer.shapes()");
        for (String s : TEN) {
            assertTrue(m.group(1).contains("VfxShape." + s), "shapes() lacks " + s);
            assertTrue(Pattern.compile("case " + s + " ->").matcher(src).find(), "no render case for " + s);
        }
        Matcher d = Pattern.compile("public int defaultDuration\\(VfxShape s\\) \\{(.*?)\\n    \\}", Pattern.DOTALL).matcher(src);
        assertTrue(d.find(), "defaultDuration");
        for (String s : TEN) {
            if (s.equals("HERCULES_INVICTUS")) continue;                                   // the long one is the default
            assertTrue(d.group(1).contains(s), "no duration for " + s);
        }
        assertTrue(d.group(1).contains("default -> 100"), "the long Invictus defaults to 100 ticks");
        assertTrue(d.group(1).contains("HERCULES_TREMOR -> 44") && d.group(1).contains("HERCULES_STRIKE -> 44") && d.group(1).contains("HERCULES_SHOCKWAVE -> 12"), "the durations of the contract");
    }

    @Test
    void everyTextureTheLayerNamesExistsAndIsAPowerOfTwo() throws IOException {
        Matcher m = Pattern.compile("t\\(\"(hercules_[a-z_]+)\"\\)").matcher(layer());
        TreeSet<String> names = new TreeSet<>();
        while (m.find()) names.add(m.group(1));
        assertTrue(names.size() >= 17, "the layer names its textures: " + names);
        List<String> bad = new ArrayList<>();
        for (String n : names) {
            Path p = TEX.resolve(n + ".png");
            if (!Files.isRegularFile(p)) { bad.add(n + " is missing"); continue; }
            BufferedImage img = ImageIO.read(p.toFile());
            int w = img.getWidth(), h = img.getHeight();
            if (!pow2(w) || !pow2(h)) bad.add(n + " is " + w + "x" + h + ", not a power of two");
            int hi = Math.max(w, h), lo = Math.min(w, h);
            if (hi / lo != 1 && hi / lo != 2 && hi / lo != 4 && hi / lo != 8) bad.add(n + " has an odd aspect " + w + "x" + h);
            if (hi > 1024) bad.add(n + " is bigger than 1024 px");
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyHerculesTextureOnDiskIsUsedByTheLayerAndCarriesNoColourUnderTransparency() throws IOException {
        String src = layer();
        List<String> bad = new ArrayList<>();
        int seen = 0;
        try (var s = Files.list(TEX)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.getFileName().toString().startsWith("hercules_") && x.toString().endsWith(".png")).sorted()::iterator) {
                seen++;
                String n = p.getFileName().toString().replace(".png", "");
                if (!src.contains("\"" + n + "\"")) bad.add(n + " is not used by HerculesLayer");
                BufferedImage img = ImageIO.read(p.toFile());
                outer:
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int argb = img.getRGB(x, y);
                        if ((argb >>> 24) == 0 && (argb & 0xFFFFFF) != 0) { bad.add(n + " carries colour under a transparent pixel"); break outer; }
                    }
                }
            }
        }
        assertTrue(seen >= 17, "the Hercules textures are on disk (" + seen + ")");
        assertEquals(List.of(), bad);
    }

    @Test
    void theLayerAvoidsPerFrameAllocationOfCollectionsAndLocations() throws IOException {
        String code = code();
        assertFalse(Pattern.compile("new (ArrayList|HashMap|HashSet|LinkedList|int\\[|float\\[|Object\\[)\\b?").matcher(code).find(), "a collection or array per frame");
        assertFalse(code.contains("ResourceLocation.fromNamespaceAndPath"), "a location built from a string");
        assertFalse(code.contains("Minecraft.getInstance"), "no game-instance access in a layer");
        assertFalse(code.contains("net.minecraft.client.Minecraft"), "no game-instance access in a layer");
        assertFalse(code.contains("com.mojang.blaze3d"), "no GL access in a layer");
    }

    @Test
    void theInvictusAsksForASmallVertexBudgetAndNoBudgetPassesTheCap() throws IOException {
        String src = layer();
        Matcher m = Pattern.compile("public int vertexBudget\\(VfxShape s\\) \\{(.*?)\\n    \\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "the layer sets its vertex budgets");
        Matcher r = Pattern.compile("case HERCULES_INVICTUS -> (\\d+);").matcher(m.group(1));
        assertTrue(r.find(), "the Invictus has a budget of its own");
        int rb = Integer.parseInt(r.group(1));
        assertTrue(rb > 0 && rb <= 200, "the Invictus is cheap: " + rb + " vertices (two overlap for 20 ticks)");
        assertTrue(m.group(1).contains("default -> VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION"), "every other effect uses the usual cap");
        assertFalse(Pattern.compile("case HERCULES_[A-Z]+ -> (\\d{3,})").matcher(m.group(1).replace("case HERCULES_INVICTUS -> " + rb, "")).find(), "no other shape names a budget of its own");
    }

    @Test
    void theStrikeHoldsStillAndTheGroundShapesAreBounded() throws IOException {
        String code = code();
        String raw = layer();
        Matcher sh = Pattern.compile("STRIKE_IMPACT = (\\d+), STRIKE_HOLD = (\\d+)").matcher(raw);
        assertTrue(sh.find(), "the impact tick and the hold of the World-Breaking Strike are constants");
        assertTrue(Integer.parseInt(sh.group(1)) < 44 && Integer.parseInt(sh.group(2)) >= 1 && Integer.parseInt(sh.group(2)) <= 6, "the impact is inside the 44 ticks and the hit-stop is a few ticks");
        Matcher mx = Pattern.compile("TREMOR_MAX = (\\d+)f, ROAR_MAX = (\\d+)f").matcher(raw);
        assertTrue(mx.find() && Integer.parseInt(mx.group(1)) <= 24 && Integer.parseInt(mx.group(2)) <= 30, "the big rings stop at a draw distance");
        assertTrue(Pattern.compile("buf\\.hasBudget\\(").matcher(code).results().count() >= 3, "optional groups check the activation budget first");
        assertTrue(code.contains("VfxShake.add("), "the heavy beats shake the camera (it falls off with the distance on the client)");
    }

    @Test
    void farThingsAreCulledAndCountsAreBounded() throws IOException {
        String code = code();
        assertTrue(code.contains("smooth(40f, 90f, horizontal(") || code.contains("smooth(50f, 100f, horizontal("), "decorative extras fall away with the camera distance");
        // every ctx.seg(full, min) in the layer stays small: no unbounded loop counts
        Matcher m = Pattern.compile("ctx\\.seg\\((\\d+), (\\d+)\\)").matcher(code);
        int found = 0;
        while (m.find()) {
            found++;
            assertTrue(Integer.parseInt(m.group(1)) <= 16, "a count above 16 at full detail: " + m.group());
            assertTrue(Integer.parseInt(m.group(2)) <= Integer.parseInt(m.group(1)), "min above full: " + m.group());
        }
        assertTrue(found >= 12, "the layer thins its counts through ctx.seg (" + found + ")");
        String raw = layer();
        assertTrue(raw.contains("Worst case quads per effect per frame"), "the class comment states the worst case");
        assertFalse(Pattern.compile("LEAP 0,").matcher(raw).find(), "the worst-case numbers are measured, not the placeholder");
        Matcher w = Pattern.compile("LEAP (\\d+), TREMOR (\\d+), INVICTUS (\\d+) \\(two overlap for 20 ticks: (\\d+)\\), GRIP (\\d+), THROW (\\d+), ROAR (\\d+), STRIKE (\\d+), SHOCKWAVE (\\d+), BODY (\\d+), MOMENTUM (\\d+)", Pattern.DOTALL).matcher(raw);
        assertTrue(w.find(), "the class comment lists every shape's worst case");
        for (int i = 1; i <= 11; i++) {
            int q = Integer.parseInt(w.group(i));
            assertTrue(q > 0 && q <= 100, "a worst case above the 100-quad activation cap: " + q);
        }
        assertTrue(Integer.parseInt(w.group(3)) <= 50, "the Invictus stays inside its 50-quad budget");
        assertTrue(Integer.parseInt(w.group(4)) <= 100, "two overlapping Invictus stay inside 100 quads");
    }

    @Test
    void theMartialLookHasItsOwnTexturesAndNothingOfTheOtherLooks() throws IOException {
        String src = layer();
        for (String n : List.of("hercules_fist", "hercules_hand", "hercules_laurel", "hercules_leaf", "hercules_ring", "hercules_wave", "hercules_cracks", "hercules_web", "hercules_dust", "hercules_chips", "hercules_streak", "hercules_chain", "hercules_flash", "hercules_mote", "hercules_plates", "hercules_flame", "hercules_crater", "hercules_model_atlas", "hercules_model_fissure"))
            assertTrue(src.contains("\"" + n + "\""), "the martial look uses " + n);
        for (String n : List.of("hercules_dust", "hercules_chips")) {
            BufferedImage img = ImageIO.read(TEX.resolve(n + ".png").toFile());
            assertTrue(img.getWidth() >= img.getHeight(), n + " is an atlas of cells");
        }
        String code = code();
        assertFalse(code.contains("JudgmentLayer") || code.contains("TrialLayer") || code.contains("JibrilLayer(") || code.contains("JibrilLayer.") || code.contains("SolomonLayer"), "Hercules does not borrow other layers' drawing code");
        assertFalse(Pattern.compile("t\\(\"(judgment|trial|jibril|israfil|solomon)_").matcher(code).find(), "no Sandalphon, Trial, Jibril, Israfil or Solomon texture");
        assertEquals(0, code.lines().filter(l -> l.contains("inst.color")).count(), "the packet colour is ignored");
    }

    @Test
    void theLayerIsRegisteredAfterSolomonAndNeverTouchesTimeMagic() throws IOException {
        String mgr = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java"), StandardCharsets.UTF_8);
        String solomon = "register(new com.newuniverse.nusmp.vfx.client.layer.SolomonLayer())";
        String hercules = "register(new com.newuniverse.nusmp.vfx.client.layer.HerculesLayer())";
        assertTrue(mgr.contains(hercules), "HerculesLayer is registered");
        assertEquals(mgr.indexOf(hercules), mgr.lastIndexOf(hercules), "it is registered once");
        assertTrue(mgr.indexOf(hercules) > mgr.indexOf(solomon), "it is registered right after SolomonLayer");
        String code = code();
        assertFalse(code.contains("TimeMagicLayer"), "Hercules must not use the Time Magic layer");
        assertFalse(Pattern.compile("t\\(\"time_").matcher(code).find(), "no Time Magic texture");
        assertFalse(layer().contains("time_"), "the layer never names a Time Magic texture");
        String gen = Files.readString(ROOT.resolve("tools/gen_hercules_vfx_textures.py"), StandardCharsets.UTF_8);
        assertFalse(gen.contains("time_"), "the generator never writes a Time Magic texture");
    }

    @Test
    void thePreviewScenesAndTheCallDocExistForEveryShape() throws IOException {
        String scenes = Files.readString(ROOT.resolve("tools/vfx_preview/scenes.d/hercules.json"), StandardCharsets.UTF_8);
        String doc = Files.readString(ROOT.resolve("docs/vfx_remake/hercules_fx_calls.md"), StandardCharsets.UTF_8);
        for (String s : TEN) {
            assertTrue(scenes.contains("\"" + s + "\""), "a preview scene for " + s);
            assertTrue(doc.contains(s), "the call doc lists " + s);
        }
        for (String sc : List.of("hercules_throw_chunk", "hercules_aftershock", "hercules_grip_close", "hercules_strike_close", "hercules_throw_close", "hercules_invictus_close", "hercules_tremor_close"))
            assertTrue(scenes.contains("\"" + sc + "\""), "3.20.0 preview scene " + sc);
        assertTrue(doc.contains("3.20.0"), "the call doc has a 3.20.0 section");
    }

    @Test
    void the3dModelsOfTheUltimateAreWiredIn() throws IOException {
        String code = code();
        String raw = layer();
        assertTrue(code.contains("HerculesModels.enabled()"), "the 3D models sit behind the client setting");
        assertTrue(code.contains("private final HerculesMeshDraw mesh = new HerculesMeshDraw();"), "one drawer per layer, no per-frame object");
        assertTrue(Pattern.compile("THROW_CHUNK_POWER = 2f").matcher(raw).find(), "power 2 of HERCULES_THROW = a hurled chunk of ground (the boulder)");
        assertTrue(Pattern.compile("AFTERSHOCK_MAX = 10f").matcher(raw).find(), "a tremor of power 10 or less = the aftershock (lighter, quieter)");
        assertTrue(code.contains("modelsOn(ctx"), "every model checks the setting and the distance first");
        assertTrue(Pattern.compile("hasBudget\\(").matcher(code).results().count() >= 20, "the optional groups still check the activation budget");
        assertTrue(raw.contains("3.20.0 3D models"), "the class comment tells about the models");
        for (String n : List.of("hercules_model_atlas", "hercules_model_fissure")) {
            Path png = TEX.resolve(n + ".png");
            assertTrue(Files.exists(png), n + ".png exists");
            try {
                BufferedImage img = ImageIO.read(png.toFile());
                assertTrue(pow2(img.getWidth()) && pow2(img.getHeight()) && img.getWidth() <= 1024 && img.getHeight() <= 1024, n + " is power-of-two and at most 1024: " + img.getWidth() + "x" + img.getHeight());
                assertTrue(Math.max(img.getWidth(), img.getHeight()) <= 8 * Math.min(img.getWidth(), img.getHeight()), n + " aspect at most 8:1");
            } catch (IOException e) { fail(n + " unreadable"); }
        }
    }

    private static boolean pow2(int v) { return v > 0 && (v & (v - 1)) == 0; }
}
