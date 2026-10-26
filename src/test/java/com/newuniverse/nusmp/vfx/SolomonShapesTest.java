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
 * Why: the nine Solomon effects (Lord of Sorcery) are drawn by {@code SolomonLayer}. The enum ordinals go over the network, so the nine shapes must stay appended right after JIBRIL_WINGS in this order; the layer must
 * draw all nine and give each a duration; every texture it names must exist (a missing one is a magenta quad in game) and be a power-of-two size (or an expected 2:1 .. 8:1 strip or atlas); the layer stays inside the
 * rule-11 render limits (static textures, no allocation of collections or locations per frame, the long-running re-sent Realm asks for a small vertex budget of its own, every budget is within the 400-vertex activation
 * cap, the Pillar draws a bounded number of pillars and checks the buffer budget, far things are left out by distance) and it shares nothing with the Jibril, Sandalphon, Trial or Time Magic looks. Pure: reads the real
 * sources and PNGs, the working directory is the project root.
 */
class SolomonShapesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer/SolomonLayer.java");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static final List<String> NINE = List.of("SOLOMON_COPY", "SOLOMON_CONVERGENCE", "SOLOMON_REALM", "SOLOMON_CHAIN", "SOLOMON_ERASURE", "SOLOMON_MARK", "SOLOMON_PILLAR", "SOLOMON_SKIN", "SOLOMON_ABSORB");

    private static String layer() throws IOException { return Files.readString(LAYER, StandardCharsets.UTF_8); }

    private static String code() throws IOException { return layer().replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    @Test
    void theNineShapesFollowJibrilWingsInOrder() {
        VfxShape[] all = VfxShape.values();
        int at = VfxShape.JIBRIL_WINGS.ordinal();
        for (int i = 0; i < NINE.size(); i++) {
            assertTrue(at + 1 + i < all.length, "missing " + NINE.get(i));
            assertEquals(NINE.get(i), all[at + 1 + i].name(), "the shapes are append-only (ordinals go over the network)");
        }
        for (String s : NINE) assertEquals(s, VfxShape.valueOf(s).name());
    }

    @Test
    void theLayerDrawsAllNineAndGivesEachADuration() throws IOException {
        String src = layer();
        Matcher m = Pattern.compile("public Set<VfxShape> shapes\\(\\) \\{(.*?)\\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "SolomonLayer.shapes()");
        for (String s : NINE) {
            assertTrue(m.group(1).contains("VfxShape." + s), "shapes() lacks " + s);
            assertTrue(Pattern.compile("case " + s + " ->").matcher(src).find(), "no render case for " + s);
        }
        Matcher d = Pattern.compile("public int defaultDuration\\(VfxShape s\\) \\{(.*?)\\n    \\}", Pattern.DOTALL).matcher(src);
        assertTrue(d.find(), "defaultDuration");
        for (String s : List.of("SOLOMON_COPY", "SOLOMON_CONVERGENCE", "SOLOMON_CHAIN", "SOLOMON_ERASURE", "SOLOMON_MARK", "SOLOMON_PILLAR", "SOLOMON_SKIN", "SOLOMON_ABSORB"))
            assertTrue(d.group(1).contains(s), "no duration for " + s);
        assertTrue(d.group(1).contains("default -> 100"), "the long Realm defaults to 100 ticks");
        assertTrue(d.group(1).contains("SOLOMON_PILLAR -> 120"), "the pillars run 120 ticks (the server's last landing is ~110)");
    }

    @Test
    void everyTextureTheLayerNamesExistsAndIsAPowerOfTwo() throws IOException {
        Matcher m = Pattern.compile("t\\(\"(solomon_[a-z_]+)\"\\)").matcher(layer());
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
    void everySolomonTextureOnDiskIsUsedByTheLayerAndCarriesNoColourUnderTransparency() throws IOException {
        String src = layer();
        List<String> bad = new ArrayList<>();
        int seen = 0;
        try (var s = Files.list(TEX)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.getFileName().toString().startsWith("solomon_") && x.toString().endsWith(".png")).sorted()::iterator) {
                seen++;
                String n = p.getFileName().toString().replace(".png", "");
                if (!src.contains("\"" + n + "\"")) bad.add(n + " is not used by SolomonLayer");
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
        assertTrue(seen >= 17, "the Solomon textures are on disk (" + seen + ")");
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
    void theRealmAsksForASmallVertexBudgetAndNoBudgetPassesTheCap() throws IOException {
        String src = layer();
        Matcher m = Pattern.compile("public int vertexBudget\\(VfxShape s\\) \\{(.*?)\\n    \\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "the layer sets its vertex budgets");
        Matcher r = Pattern.compile("case SOLOMON_REALM -> (\\d+);").matcher(m.group(1));
        assertTrue(r.find(), "the Realm has a budget of its own");
        int rb = Integer.parseInt(r.group(1));
        assertTrue(rb > 0 && rb <= 200, "the Realm is cheap: " + rb + " vertices (two overlap for 20 ticks)");
        assertTrue(m.group(1).contains("default -> VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION"), "every other effect uses the usual cap");
        assertFalse(Pattern.compile("case SOLOMON_[A-Z]+ -> (\\d{3,})").matcher(m.group(1).replace("case SOLOMON_REALM -> " + rb, "")).find(), "no other shape names a budget of its own");
    }

    @Test
    void thePillarsAreBoundedAndTheBufferBudgetIsChecked() throws IOException {
        String code = code();
        String raw = layer();
        Matcher f = Pattern.compile("PILLARS_FALLING = (\\d+), PILLARS_LANDING = (\\d+)").matcher(raw);
        assertTrue(f.find(), "the number of pillars drawn at once is a constant");
        assertTrue(Integer.parseInt(f.group(1)) <= 8 && Integer.parseInt(f.group(2)) <= 8, "never more than ~8 pillars at once");
        assertTrue(code.contains("ctx.seg(PILLARS_FALLING, 3)") && code.contains("ctx.seg(PILLARS_LANDING, 3)"), "the particle setting thins the pillars");
        assertTrue(code.contains("FALL = SolomonRules.PILLAR_DELAY_TICKS"), "the fall time is the server's delay, so the picture lands with the impact");
        assertTrue(code.contains("SolomonRules.pillarSpawnTick(") && code.contains("SolomonRules.pillarOffset(") && code.contains("SolomonRules.pillarElement("), "the picture uses the same pure functions as the server");
        assertTrue(Pattern.compile("buf\\.hasBudget\\(").matcher(code).results().count() >= 3, "every pillar part checks the activation budget first");
        Matcher ph = Pattern.compile("PILLAR_HEIGHT = (\\d+)f").matcher(raw);
        assertTrue(ph.find() && Integer.parseInt(ph.group(1)) >= 32 && Integer.parseInt(ph.group(1)) <= 128, "the pillar is tall and bounded");
        Matcher cv = Pattern.compile("CONVERGENCE_VISUAL = (\\d+)f, ERASURE_VISUAL = (\\d+)f").matcher(raw);
        assertTrue(cv.find() && Integer.parseInt(cv.group(1)) <= 24 && Integer.parseInt(cv.group(2)) <= 36, "the big rings stop at a draw distance");
    }

    @Test
    void farThingsAreCulledAndCountsAreBounded() throws IOException {
        String code = code();
        assertTrue(code.contains("smooth(40f, 90f, horizontal("), "decorative extras fall away with the camera distance");
        assertTrue(code.contains("smooth(60f, 120f, horizontal("), "the pillars fall away with the camera distance");
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
        assertFalse(Pattern.compile("COPY 0,").matcher(raw).find(), "the worst-case numbers are measured, not the placeholder");
        Matcher w = Pattern.compile("COPY (\\d+), CONVERGENCE (\\d+), REALM (\\d+) \\(two overlap for 20 ticks: (\\d+)\\), CHAIN (\\d+), ERASURE (\\d+), MARK (\\d+), PILLAR (\\d+) .*?SKIN (\\d+), ABSORB (\\d+)", Pattern.DOTALL).matcher(raw);
        assertTrue(w.find(), "the class comment lists every shape's worst case");
        for (int i = 1; i <= 10; i++) {
            int q = Integer.parseInt(w.group(i));
            assertTrue(q > 0 && q <= 100, "a worst case above the 100-quad activation cap: " + q);
        }
        assertTrue(Integer.parseInt(w.group(3)) <= 50, "the Realm stays inside its 50-quad budget");
    }

    @Test
    void theSorcerersNightHasItsOwnTexturesAndNothingOfTheOtherLooks() throws IOException {
        String src = layer();
        for (String n : List.of("solomon_hexagram", "solomon_circle", "solomon_page", "solomon_runes", "solomon_pillar", "solomon_chain", "solomon_book", "solomon_sigils"))
            assertTrue(src.contains("\"" + n + "\""), "the sorcery look uses " + n);
        for (String n : List.of("solomon_runes", "solomon_sigils")) {
            BufferedImage img = ImageIO.read(TEX.resolve(n + ".png").toFile());
            assertTrue(img.getWidth() / img.getHeight() == 2, n + " is a 4x2 atlas (2:1)");
        }
        String code = code();
        assertFalse(code.contains("JudgmentLayer") || code.contains("TrialLayer") || code.contains("JibrilLayer(") || code.contains("JibrilLayer."), "Solomon does not borrow other layers' drawing code");
        assertFalse(Pattern.compile("t\\(\"(judgment|trial|jibril|israfil)_").matcher(code).find(), "no Sandalphon, Trial, Jibril or Israfil texture");
        assertEquals(1, code.lines().filter(l -> l.contains("inst.color")).count(), "the packet colour is ignored (the Elemental Sovereignty spark is the one place that reads it)");
    }

    @Test
    void theLayerIsRegisteredAfterJibrilAndNeverTouchesTimeMagic() throws IOException {
        String mgr = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java"), StandardCharsets.UTF_8);
        String jibril = "register(new com.newuniverse.nusmp.vfx.client.layer.JibrilLayer())";
        String solomon = "register(new com.newuniverse.nusmp.vfx.client.layer.SolomonLayer())";
        assertTrue(mgr.contains(solomon), "SolomonLayer is registered");
        assertEquals(mgr.indexOf(solomon), mgr.lastIndexOf(solomon), "it is registered once");
        assertTrue(mgr.indexOf(solomon) > mgr.indexOf(jibril), "it is registered right after JibrilLayer");
        String code = code();
        assertFalse(code.contains("TimeMagicLayer"), "Solomon must not use the Time Magic layer");
        assertFalse(Pattern.compile("t\\(\"time_").matcher(code).find(), "no Time Magic texture");
        String gen = Files.readString(ROOT.resolve("tools/gen_solomon_vfx_textures.py"), StandardCharsets.UTF_8);
        assertFalse(gen.contains("time_"), "the generator never writes a Time Magic texture");
    }

    @Test
    void thePreviewScenesAndTheCallDocExistForEveryShape() throws IOException {
        String scenes = Files.readString(ROOT.resolve("tools/vfx_preview/scenes.d/solomon.json"), StandardCharsets.UTF_8);
        String doc = Files.readString(ROOT.resolve("docs/vfx_remake/solomon_fx_calls.md"), StandardCharsets.UTF_8);
        for (String s : NINE) {
            assertTrue(scenes.contains("\"" + s + "\""), "a preview scene for " + s);
            assertTrue(doc.contains(s), "the call doc lists " + s);
        }
    }

    private static boolean pow2(int v) { return v > 0 && (v & (v - 1)) == 0; }
}
