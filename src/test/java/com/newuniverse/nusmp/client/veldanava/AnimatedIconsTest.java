package com.newuniverse.nusmp.client.veldanava;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the animated Ultimate skill icons (3.17.0) are a pure arithmetic (IconStrip) plus five sets of files made by tools/gen_*_icon_anim.py. A strip that is not a whole number of square frames,
 * an icon with a missing file, a tinted pixel hiding under transparency (a dark fringe once the game filters the texture), or a tick that allocates would only show up in a game, so they are checked here.
 */
class AnimatedIconsTest {
    private static final Path ASSETS = Paths.get("src/main/resources/assets/nusmp");
    private static final Path SRC = Paths.get("src/main/java/com/newuniverse/nusmp");

    @Test
    void framesAreCountedFromTheStripShape() {
        assertEquals(32, IconStrip.frames(32, 1024));
        assertEquals(32, IconStrip.frames(128, 4096));
        assertEquals(1, IconStrip.frames(32, 32));
        assertEquals(0, IconStrip.frames(32, 1000), "not a whole number of frames");
        assertEquals(0, IconStrip.frames(64, 32), "wider than tall");
        assertEquals(0, IconStrip.frames(0, 0));
        assertEquals(0, IconStrip.frames(8, 8 * (IconStrip.MAX_FRAMES + 1)), "too many frames");
    }

    @Test
    void theFrameFollowsTheTicksAndLoops() {
        assertEquals(0, IconStrip.frameAt(0, 32, 2));
        assertEquals(0, IconStrip.frameAt(1, 32, 2));
        assertEquals(1, IconStrip.frameAt(2, 32, 2));
        assertEquals(31, IconStrip.frameAt(63, 32, 2));
        assertEquals(0, IconStrip.frameAt(64, 32, 2), "the loop is 64 ticks = 3.2 s");
        assertEquals(0, IconStrip.frameAt(5, 1, 2));
        assertEquals(3, IconStrip.frameAt(3, 32, 0), "a frametime below 1 counts as 1");
        assertEquals(0, IconStrip.frameAt(-5, 32, 2));
    }

    @Test
    void theFrametimeIsReadFromTheMcmetaAndClamped() {
        assertEquals(2, IconStrip.frameTime(null));
        assertEquals(2, IconStrip.frameTime(""));
        assertEquals(2, IconStrip.frameTime("not json"));
        assertEquals(2, IconStrip.frameTime("{}"));
        assertEquals(5, IconStrip.frameTime("{\"animation\":{\"frametime\":5}}"));
        assertEquals(1, IconStrip.frameTime("{\"animation\":{\"frametime\":0}}"));
        assertEquals(40, IconStrip.frameTime("{\"animation\":{\"frametime\":999}}"));
    }

    @Test
    void thePathsMatchTheSkillIconPath() {
        // VeldanavaSkillBase names textures/skill/veldanava/<id>.png as the icon: the animation is registered under exactly that path
        assertEquals("textures/skill/veldanava/jibril.png", IconStrip.stillPath("jibril"));
        assertEquals("textures/skill/veldanava/jibril_anim.png", IconStrip.stripPath("jibril"));
        assertEquals("textures/skill/veldanava/jibril_anim_hd.png", IconStrip.hdStripPath("jibril"));
        assertEquals("textures/skill/veldanava/jibril_classic.png", IconStrip.classicPath("jibril"));
        assertTrue(IconStrip.known("hercules"));
        assertFalse(IconStrip.known("../x"));
    }

    @Test
    void theFiveIdsAreTheFiveCardsInOrder() throws IOException {
        String rules = Files.readString(SRC.resolve("entity/veldanava/skills/JudgmentRules.java"));
        List<String> ids = new ArrayList<>();
        var m = java.util.regex.Pattern.compile("new Card\\(\"(\\w+)\"").matcher(rules);
        while (m.find()) ids.add(m.group(1));
        assertEquals(ids, IconStrip.IDS, "a card was added or reordered: the icon list must follow");
    }

    private static Path tex(String path) { return ASSETS.resolve(path); }

    private static BufferedImage read(String path) throws IOException {
        return ImageIO.read(tex(path).toFile());
    }

    @Test
    void everyIconShipsItsFilesWithMatchingFrames() throws IOException {
        for (String id : IconStrip.IDS) {
            BufferedImage sd = read(IconStrip.stripPath(id)), hd = read(IconStrip.hdStripPath(id)), still = read(IconStrip.stillPath(id));
            assertEquals(32, sd.getWidth(), id + " 32 px strip width");
            assertEquals(128, hd.getWidth(), id + " HD strip width");
            int n = IconStrip.frames(sd.getWidth(), sd.getHeight());
            assertTrue(n >= 16, id + " has only " + n + " frames");
            assertEquals(n, IconStrip.frames(hd.getWidth(), hd.getHeight()), id + ": the two strips must have the same frame count");
            assertEquals(32, still.getWidth(), id + " still");
            assertEquals(32, still.getHeight(), id + " still");
            for (String suffix : new String[]{"_anim.png.mcmeta", "_anim_hd.png.mcmeta"}) {
                String meta = Files.readString(tex("textures/skill/veldanava/" + id + suffix));
                assertEquals(2, IconStrip.frameTime(meta), id + suffix);
            }
            assertTrue(IconStrip.WITH_CLASSIC.contains(id) == Files.exists(tex(IconStrip.classicPath(id))), id + ": the classic sprite is kept for exactly the three older skills");
        }
    }

    @Test
    void theStillIsOneOfTheFramesOfTheSmallStrip() throws IOException {
        for (String id : IconStrip.IDS) {
            BufferedImage sd = read(IconStrip.stripPath(id)), still = read(IconStrip.stillPath(id));
            boolean found = false;
            for (int f = 0; f < sd.getHeight() / 32 && !found; f++) {
                boolean same = true;
                for (int y = 0; y < 32 && same; y++) for (int x = 0; x < 32; x++) if (sd.getRGB(x, f * 32 + y) != still.getRGB(x, y)) { same = false; break; }
                found = same;
            }
            assertTrue(found, id + ".png must be a frame of " + id + "_anim.png (the hero frame)");
        }
    }

    @Test
    void transparentPixelsCarryNoColourAndTheCornersStayEmpty() throws IOException {
        for (String id : IconStrip.IDS) {
            for (String path : new String[]{IconStrip.stripPath(id), IconStrip.hdStripPath(id), IconStrip.stillPath(id)}) {
                BufferedImage img = read(path);
                int w = img.getWidth();
                for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    if ((argb >>> 24) == 0) assertEquals(0, argb & 0xFFFFFF, path + " has colour under a transparent pixel at " + x + "," + y);
                }
                for (int f = 0; f < img.getHeight() / w; f++) assertEquals(0, img.getRGB(0, f * w) >>> 24, path + " frame " + f + " corner is not transparent");
            }
        }
    }

    @Test
    void theLoopIsSeamlessEnough() throws IOException {
        for (String id : IconStrip.IDS) {
            BufferedImage hd = read(IconStrip.hdStripPath(id));
            int n = hd.getHeight() / 128;
            double wrap = diff(hd, n - 1, 0), sum = 0;
            for (int f = 0; f < n - 1; f++) sum += diff(hd, f, f + 1);
            double mean = sum / (n - 1);
            assertTrue(wrap <= mean * 1.5 + 0.05, id + ": the step from the last frame to the first (" + wrap + ") is bigger than 1.5 times an average step (" + mean + ")");
        }
    }

    private static double diff(BufferedImage img, int a, int b) {
        long total = 0;
        for (int y = 0; y < 128; y += 2) for (int x = 0; x < 128; x += 2) {
            int p = img.getRGB(x, a * 128 + y), q = img.getRGB(x, b * 128 + y);
            for (int s = 0; s <= 24; s += 8) total += Math.abs(((p >> s) & 255) - ((q >> s) & 255));
        }
        return total / (64.0 * 64 * 4);
    }

    @Test
    void theClientCodeIsClientOnlyAndTheTickAllocatesNothing() throws IOException {
        String tex = Files.readString(SRC.resolve("client/veldanava/AnimatedIconTexture.java"));
        int i = tex.indexOf("public void tick()");
        String tick = tex.substring(i, tex.indexOf("@Override", i));
        assertFalse(tick.contains("new "), "the texture tick must not allocate");
        String icons = Files.readString(SRC.resolve("client/veldanava/AnimatedIcons.java"));
        int j = icons.indexOf("private static void onClientTick()");
        String check = icons.substring(j, icons.indexOf("private static void apply", j));
        assertTrue(check.contains("sinceCheck < 20"), "the icon check must run once a second, not every tick");
        assertFalse(check.contains("ResourceLocation."), "the once-a-second check must not build locations");
        // the common code never names these classes: only the client block of NUSMP does
        try (var walk = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("client/")) continue;
                String src = Files.readString(p);
                if (src.contains("AnimatedIcon") || src.contains("IconStrip")) assertEquals("NUSMP.java", rel, rel + " must not touch the animated icon classes");
            }
        }
    }
}
