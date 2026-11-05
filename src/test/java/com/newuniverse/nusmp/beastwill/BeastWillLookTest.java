package com.newuniverse.nusmp.beastwill;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/beastwill_vfx.md): the animal spirits ride on the one int of the aura lease, so the packing has to be exact and old-compatible; every shipped will must have a spirit; the textures the painter
 * names must exist and be safe for the additive pass (transparent border, no colour under a transparent pixel); and the cost promises of the painter and the abilities are pinned from the source (nothing can
 * be profiled here). If one of these is changed on purpose, change the pin in the same commit and say why. Pure (file reads); the working directory is the project root.
 */
class BeastWillLookTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");
    private static final Path DATA = ROOT.resolve("src/main/resources/data/nusmp/beast_will");
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    @Test
    void thePackingRoundTripsAndAnOldStyleKeepsTheOldLook() {
        for (int phase = 1; phase <= 2; phase++) {
            for (int kind = 0; kind < BeastWillLook.COUNT; kind++) {
                for (int tint : new int[]{0, 0x123456, 0xFFFFFF}) {
                    int s = BeastWillLook.style(phase, kind, tint);
                    assertEquals(phase, BeastWillLook.phase(s));
                    assertEquals(kind, BeastWillLook.kind(s));
                    assertEquals(tint, BeastWillLook.tint(s));
                }
            }
        }
        int old = (2 << 24) | 0x7FD6FF;                                                           // what 3.45.0 wrote
        assertEquals(2, BeastWillLook.phase(old));
        assertEquals(BeastWillLook.UNKNOWN, BeastWillLook.kind(old), "an old style has no kind: the painter keeps the old look");
        assertEquals(0x7FD6FF, BeastWillLook.tint(old));
        assertEquals(BeastWillLook.UNKNOWN, BeastWillLook.kind(BeastWillLook.style(1, 15, 0)), "a kind the table does not list is unknown");
        assertEquals(BeastWillLook.UNKNOWN, BeastWillLook.kindOf("some_pack_will"));
        assertEquals(BeastWillLook.UNKNOWN, BeastWillLook.kindOf(null));
    }

    @Test
    void everyShippedWillHasItsOwnSpirit() throws IOException {
        Set<Integer> kinds = new HashSet<>();
        int files = 0;
        try (Stream<Path> s = Files.list(DATA)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".json")).toList()) {
                files++;
                String id = p.getFileName().toString().replace(".json", "");
                int k = BeastWillLook.kindOf(id);
                assertNotEquals(BeastWillLook.UNKNOWN, k, id + " has no look kind");
                assertTrue(kinds.add(k), id + " shares a kind with another will");
            }
        }
        assertEquals(9, files, "nine shipped wills");
        assertEquals(BeastWillLook.COUNT - 1, kinds.size());
        assertEquals(BeastWillLook.COUNT, BeastWillLook.STEM.length);
    }

    @Test
    void theManagerWritesTheKindIntoTheStyle() throws IOException {
        String m = code(SRC.resolve("beastwill/BeastWillManager.java"));
        assertTrue(m.contains("BeastWillLook.style(phase, BeastWillLook.kindOf(def.id()), def.tint())"), "the server passes the look kind to the client in the style");
    }

    private static BufferedImage read(String name) throws IOException {
        Path p = TEX.resolve(name + ".png");
        assertTrue(Files.exists(p), name + " is missing");
        return ImageIO.read(p.toFile());
    }

    @Test
    void everySpiritTextureExistsAndIsSafeForTheAdditivePass() throws IOException {
        String painter = code(SRC.resolve("client/aura/BeastWillAura.java"));
        for (int k = 1; k < BeastWillLook.COUNT; k++) {
            for (String layer : new String[]{"a", "b"}) {
                String name = "bwspirit_" + BeastWillLook.STEM[k] + "_" + layer;
                assertTrue(painter.contains("\"particle/" + name + "\""), "the painter must ask for " + name);
                BufferedImage im = read(name);
                assertEquals(im.getWidth(), im.getHeight(), name + " is square");
                assertEquals(256, im.getWidth());
                assertSafe(name, im);
            }
        }
        for (String name : new String[]{"bwspirit_leaf", "bwspirit_shard", "bwspirit_bolt", "bwspirit_ember", "bwspirit_dust", "bwspirit_ribbon"}) {
            assertTrue(painter.contains("\"particle/" + name + "\""), "the painter must ask for " + name);
            assertSafe(name, read(name));
        }
    }

    private static void assertSafe(String name, BufferedImage im) {
        int w = im.getWidth(), h = im.getHeight();
        int[] corner = {0, 0, w - 1, 0, 0, h - 1, w - 1, h - 1};
        for (int i = 0; i < 8; i += 2) assertTrue((im.getRGB(corner[i], corner[i + 1]) >>> 24) < 16, name + " has an opaque corner");
        int lit = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = im.getRGB(x, y);
                if ((v >>> 24) == 0) assertEquals(0, v & 0xFFFFFF, name + " carries a colour under a transparent pixel at " + x + "," + y);
                if ((v >>> 24) > 100) lit++;
            }
        }
        assertTrue(lit > w * h / 200, name + " is nearly empty");
    }

    @Test
    void thePainterIsBoundedAndAllocatesNothingPerFrame() throws IOException {
        String c = code(SRC.resolve("client/aura/BeastWillAura.java"));
        assertTrue(c.contains("d2 > 48.0 * 48.0") && c.contains("d2 > 24.0 * 24.0"), "nothing beyond 48 blocks, the light level beyond 24");
        assertTrue(c.contains("FULL_PER_FRAME = 6") && c.contains("taken++ < FULL_PER_FRAME"), "at most six players a frame get the full spirit");
        assertTrue(c.contains("PerfProbe.section(\"c.beastwill.spirit\")"), "the spirit has its own probe section");
        assertTrue(c.contains("BeastWillLook.kind(") && c.contains("kind > 0"), "an unknown kind keeps the old look");
        assertTrue(c.contains("self1p") && c.contains("!self1p"), "no spirit from the owner's own first-person camera");
        assertTrue(Pattern.compile("\\bi < (8|4);").matcher(c).find() && !c.contains("i < 9") && !c.contains("i < 16"), "the specks are capped at eight (four ribbons)");
        assertEquals(3, count(c, "new ResourceLocation["), "only the three static tables allocate (once)");
        assertEquals(3, count(c, "new "), "no other allocation anywhere in the painter");
        assertFalse(c.contains("Minecraft.getInstance()") || c.contains("new float") || c.contains("new int"), "no client singleton, no scratch arrays");
    }

    @Test
    void everyAbilitySendsAtMostTwoPicturesAndOnePuff() throws IOException {
        String a = code(SRC.resolve("beastwill/BeastWillAbilities.java"));
        assertEquals(2, count(a, "VfxSpawn.send("), "the shape helper and the exclusion telegraph are the only one-shot senders");
        assertEquals(2, count(a, "VfxSpawn.sendFollowing("), "the two mantles of Mane Surge");
        assertTrue(a.contains("Math.min(count, 12)"), "the tinted puff is capped at twelve");
        assertTrue(a.contains("if (shown++ < 2)"), "a lash draws at most two pictures however many it hits");
        assertTrue(a.contains("one picture for the whole field, not one per target") || a.contains("burst(p.serverLevel(), def, p.position(), a.radius());"), "a field effect is one picture");
        assertFalse(a.contains("sendParticles(") || a.contains("ParticleTypes."), "particles only through the Fx facade");
        assertFalse(a.contains("TIME_STASIS") || a.contains("TIME_CLOCK") || a.contains("TIME_REWIND") || a.contains("TIME_ACCEL"), "the Time Magic effects are never used here");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + needle.length())) n++;
        return n;
    }
}
