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
 * Why: the six Trial effects of Israfil are drawn by {@code TrialLayer}. The enum ordinals go over the network, so the six shapes must stay appended right after JUDGMENT_CROSS in this order; the layer must draw all
 * six; every texture it names must exist (a missing one is a magenta quad in game); every texture is a power-of-two size (or the expected 2:1 / 1:4 strip); and the layer stays inside the rule-11 render limits (static textures,
 * a ring of any size is one quad, no allocation of lists or maps per frame). Pure: reads the real sources and PNGs, the working directory is the project root.
 */
class TrialShapesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer/TrialLayer.java");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static final List<String> SIX = List.of("TRIAL_LABYRINTH", "TRIAL_GRAVITY", "TRIAL_COLLAPSE", "TRIAL_SHIFT", "TRIAL_TRUMPET", "TRIAL_RIFT");

    private static String layer() throws IOException { return Files.readString(LAYER, StandardCharsets.UTF_8); }

    @Test
    void theSixShapesFollowJudgmentCrossInOrder() {
        VfxShape[] all = VfxShape.values();
        int at = VfxShape.JUDGMENT_CROSS.ordinal();
        for (int i = 0; i < SIX.size(); i++) {
            assertTrue(at + 1 + i < all.length, "missing " + SIX.get(i));
            assertEquals(SIX.get(i), all[at + 1 + i].name(), "the shapes are append-only (ordinals go over the network)");
        }
        for (String s : SIX) assertEquals(s, VfxShape.valueOf(s).name());
    }

    @Test
    void theLayerDrawsAllSixAndGivesEachADuration() throws IOException {
        String src = layer();
        Matcher m = Pattern.compile("public Set<VfxShape> shapes\\(\\) \\{(.*?)\\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "TrialLayer.shapes()");
        for (String s : SIX) {
            assertTrue(m.group(1).contains("VfxShape." + s), "shapes() lacks " + s);
            assertTrue(Pattern.compile("case " + s + "\\b").matcher(src).find(), "no render case for " + s);
        }
        assertTrue(src.contains("defaultDuration"), "defaultDuration");
    }

    @Test
    void everyTextureTheLayerNamesExistsAndIsAPowerOfTwo() throws IOException {
        Matcher m = Pattern.compile("t\\(\"(trial_[a-z_]+)\"\\)").matcher(layer());
        TreeSet<String> names = new TreeSet<>();
        while (m.find()) names.add(m.group(1));
        assertTrue(names.size() >= 15, "the layer names its textures: " + names);
        List<String> bad = new ArrayList<>();
        for (String n : names) {
            Path p = TEX.resolve(n + ".png");
            if (!Files.isRegularFile(p)) { bad.add(n + " is missing"); continue; }
            BufferedImage img = ImageIO.read(p.toFile());
            int w = img.getWidth(), h = img.getHeight();
            if (!pow2(w) || !pow2(h)) bad.add(n + " is " + w + "x" + h + ", not a power of two");
            int hi = Math.max(w, h), lo = Math.min(w, h);
            if (hi / lo != 1 && hi / lo != 2 && hi / lo != 4 && hi / lo != 8) bad.add(n + " has an odd aspect " + w + "x" + h);
        }
        assertEquals(List.of(), bad);
    }

    @Test
    void everyTrialTextureOnDiskIsUsedByTheLayer() throws IOException {
        String src = layer();
        try (var s = Files.list(TEX)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.getFileName().toString().startsWith("trial_") && x.toString().endsWith(".png")).sorted()::iterator) {
                String n = p.getFileName().toString().replace(".png", "");
                assertTrue(src.contains("\"" + n + "\""), n + " is not used by TrialLayer");
            }
        }
    }

    @Test
    void theLayerAvoidsPerFrameAllocationOfCollectionsAndLocations() throws IOException {
        String code = layer().replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
        assertFalse(Pattern.compile("new (ArrayList|HashMap|HashSet|LinkedList)\\b").matcher(code).find(), "a collection per frame");
        assertFalse(code.contains("ResourceLocation.fromNamespaceAndPath"), "a location built from a string");
        assertFalse(code.contains("Minecraft.getInstance"), "no game-instance access in a layer");
    }

    @Test
    void theLayerIsRegisteredAndNeverTouchesTimeMagic() throws IOException {
        String mgr = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java"), StandardCharsets.UTF_8);
        assertTrue(mgr.contains("register(new com.newuniverse.nusmp.vfx.client.layer.TrialLayer())"), "TrialLayer is registered");
        String code = layer().replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
        assertFalse(code.contains("TimeMagicLayer"), "Trial must not use the Time Magic layer");
        assertFalse(Pattern.compile("t\\(\"time_").matcher(code).find(), "no Time Magic texture");
    }

    private static boolean pow2(int v) { return v > 0 && (v & (v - 1)) == 0; }
}
