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
 * Why: the eight Jibril effects (Lord of Rigor) are drawn by {@code JibrilLayer}. The enum ordinals go over the network, so the eight shapes must stay appended right after TRIAL_RIFT in this order; the layer must draw
 * all eight and give each a duration; every texture it names must exist (a missing one is a magenta quad in game) and be a power-of-two size (or an expected 2:1 / 4:1 strip); the layer stays inside the rule-11 render
 * limits (static textures, no allocation of collections or locations per frame, the two long-running re-sent effects ask for a small vertex budget of their own, every budget is within the 400-vertex activation cap,
 * far things are left out by distance). Pure: reads the real sources and PNGs, the working directory is the project root.
 */
class JibrilShapesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer/JibrilLayer.java");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static final List<String> EIGHT = List.of("JIBRIL_COMMAND", "JIBRIL_JUDGMENT", "JIBRIL_PURGE", "JIBRIL_SHACKLES", "JIBRIL_WORD", "JIBRIL_REFLECT", "JIBRIL_KINGDOM", "JIBRIL_WINGS");

    private static String layer() throws IOException { return Files.readString(LAYER, StandardCharsets.UTF_8); }

    private static String code() throws IOException { return layer().replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    @Test
    void theEightShapesFollowTrialRiftInOrder() {
        VfxShape[] all = VfxShape.values();
        int at = VfxShape.TRIAL_RIFT.ordinal();
        for (int i = 0; i < EIGHT.size(); i++) {
            assertTrue(at + 1 + i < all.length, "missing " + EIGHT.get(i));
            assertEquals(EIGHT.get(i), all[at + 1 + i].name(), "the shapes are append-only (ordinals go over the network)");
        }
        for (String s : EIGHT) assertEquals(s, VfxShape.valueOf(s).name());
    }

    @Test
    void theLayerDrawsAllEightAndGivesEachADuration() throws IOException {
        String src = layer();
        Matcher m = Pattern.compile("public Set<VfxShape> shapes\\(\\) \\{(.*?)\\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "JibrilLayer.shapes()");
        for (String s : EIGHT) {
            assertTrue(m.group(1).contains("VfxShape." + s), "shapes() lacks " + s);
            assertTrue(Pattern.compile("case " + s + " ->").matcher(src).find(), "no render case for " + s);
        }
        Matcher d = Pattern.compile("public int defaultDuration\\(VfxShape s\\) \\{(.*?)\\n    \\}", Pattern.DOTALL).matcher(src);
        assertTrue(d.find(), "defaultDuration");
        for (String s : List.of("JIBRIL_COMMAND", "JIBRIL_JUDGMENT", "JIBRIL_PURGE", "JIBRIL_SHACKLES", "JIBRIL_WORD", "JIBRIL_REFLECT")) assertTrue(d.group(1).contains(s), "no duration for " + s);
        assertTrue(d.group(1).contains("default -> 100"), "the long effects (Kingdom, Wings) default to 100 ticks");
    }

    @Test
    void everyTextureTheLayerNamesExistsAndIsAPowerOfTwo() throws IOException {
        Matcher m = Pattern.compile("t\\(\"(jibril_[a-z_]+)\"\\)").matcher(layer());
        TreeSet<String> names = new TreeSet<>();
        while (m.find()) names.add(m.group(1));
        assertTrue(names.size() >= 22, "the layer names its textures: " + names);
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
    void everyJibrilTextureOnDiskIsUsedByTheLayerAndCarriesNoColourUnderTransparency() throws IOException {
        String src = layer();
        List<String> bad = new ArrayList<>();
        try (var s = Files.list(TEX)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.getFileName().toString().startsWith("jibril_") && x.toString().endsWith(".png")).sorted()::iterator) {
                String n = p.getFileName().toString().replace(".png", "");
                if (!src.contains("\"" + n + "\"")) bad.add(n + " is not used by JibrilLayer");
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
        assertEquals(List.of(), bad);
    }

    @Test
    void theLayerAvoidsPerFrameAllocationOfCollectionsAndLocations() throws IOException {
        String code = code();
        assertFalse(Pattern.compile("new (ArrayList|HashMap|HashSet|LinkedList|int\\[|float\\[|Object\\[)\\b?").matcher(code).find(), "a collection or array per frame");
        assertFalse(code.contains("ResourceLocation.fromNamespaceAndPath"), "a location built from a string");
        assertFalse(code.contains("Minecraft.getInstance"), "no game-instance access in a layer");
        assertFalse(code.contains("net.minecraft.client.Minecraft"), "no game-instance access in a layer");
    }

    @Test
    void theLongRunningEffectsAskForASmallVertexBudgetAndNoBudgetPassesTheCap() throws IOException {
        String src = layer();
        Matcher m = Pattern.compile("public int vertexBudget\\(VfxShape s\\) \\{(.*?)\\n    \\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "the layer sets its vertex budgets");
        Matcher k = Pattern.compile("case JIBRIL_KINGDOM -> (\\d+);").matcher(m.group(1));
        Matcher w = Pattern.compile("case JIBRIL_WINGS -> (\\d+);").matcher(m.group(1));
        assertTrue(k.find() && w.find(), "Kingdom and Wings have a budget of their own");
        int kb = Integer.parseInt(k.group(1)), wb = Integer.parseInt(w.group(1));
        assertTrue(kb > 0 && kb <= 200, "Kingdom is cheap: " + kb + " vertices");
        assertTrue(wb > 0 && wb <= 200, "Wings are cheap: " + wb + " vertices");
        assertTrue(m.group(1).contains("default -> VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION"), "every other effect uses the usual cap");
    }

    @Test
    void farThingsAreCulledAndCountsAreBounded() throws IOException {
        String code = code();
        assertTrue(code.contains("smooth(40f, 90f, horizontal("), "decorative extras fall away with the camera distance");
        assertTrue(code.contains("PURGE_VISUAL"), "the Purge picture stops at a draw distance");
        Matcher pv = Pattern.compile("PURGE_VISUAL = (\\d+)f").matcher(layer());
        assertTrue(pv.find() && Integer.parseInt(pv.group(1)) <= 40, "the Purge picture stops at 40 blocks");
        Matcher ph = Pattern.compile("PILLAR_HEIGHT = (\\d+)f").matcher(layer());
        assertTrue(ph.find() && Integer.parseInt(ph.group(1)) >= 64 && Integer.parseInt(ph.group(1)) <= 128, "the pillar is 64+ blocks and bounded");
        assertTrue(code.contains("final int N = 24;") && code.contains("ctx.seg(6, 2)"), "the Kingdom draws at most the six nearest of 24 curtains");
        // every ctx.seg(full, min) in the layer stays small: no unbounded loop counts
        Matcher m = Pattern.compile("ctx\\.seg\\((\\d+), (\\d+)\\)").matcher(code);
        int found = 0;
        while (m.find()) {
            found++;
            assertTrue(Integer.parseInt(m.group(1)) <= 16, "a count above 16 at full detail: " + m.group());
            assertTrue(Integer.parseInt(m.group(2)) <= Integer.parseInt(m.group(1)), "min above full: " + m.group());
        }
        assertTrue(found >= 25, "the layer thins its counts through ctx.seg (" + found + ")");
        assertTrue(layer().contains("Worst case quads per effect per frame"), "the class comment states the worst case");
    }

    @Test
    void theDawnLookHasItsOwnTexturesAndNothingOfSandalphon() throws IOException {
        String src = layer();
        for (String n : List.of("jibril_scroll", "jibril_emblem", "jibril_petal", "jibril_ribbon", "jibril_spiral", "jibril_prism"))
            assertTrue(src.contains("\"" + n + "\""), "the dawn look uses " + n);
        for (String n : List.of("jibril_glyph", "jibril_seal", "jibril_wing")) {
            BufferedImage img = ImageIO.read(TEX.resolve(n + ".png").toFile());
            assertTrue(Math.max(img.getWidth(), img.getHeight()) >= 1024, n + " is drawn at 1024 px");
        }
        String code = code();
        assertFalse(code.contains("JudgmentLayer") || code.contains("TrialLayer"), "Jibril does not borrow Sandalphon or Trial drawing code");
        assertFalse(Pattern.compile("t\\(\"(judgment|trial)_").matcher(code).find(), "no Sandalphon or Trial texture");
    }

    @Test
    void theLayerIsRegisteredAfterTrialAndNeverTouchesTimeMagic() throws IOException {
        String mgr = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java"), StandardCharsets.UTF_8);
        String trial = "register(new com.newuniverse.nusmp.vfx.client.layer.TrialLayer())";
        String jibril = "register(new com.newuniverse.nusmp.vfx.client.layer.JibrilLayer())";
        assertTrue(mgr.contains(jibril), "JibrilLayer is registered");
        assertTrue(mgr.indexOf(jibril) > mgr.indexOf(trial), "it is registered right after TrialLayer");
        String code = code();
        assertFalse(code.contains("TimeMagicLayer"), "Jibril must not use the Time Magic layer");
        assertFalse(Pattern.compile("t\\(\"time_").matcher(code).find(), "no Time Magic texture");
    }

    @Test
    void theSkillIconExists() throws IOException {
        Path icon = ROOT.resolve("src/main/resources/assets/nusmp/textures/skill/veldanava/jibril.png");
        assertTrue(Files.isRegularFile(icon), "the skill icon");
        BufferedImage img = ImageIO.read(icon.toFile());
        assertEquals(32, img.getWidth());
        assertEquals(32, img.getHeight());
    }

    private static boolean pow2(int v) { return v > 0 && (v & (v - 1)) == 0; }
}
