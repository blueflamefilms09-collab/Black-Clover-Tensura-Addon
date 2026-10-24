package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout.Layout;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The glyph-ring layout (staged): glyphs face the caster and read along the rings, the rings counter-rotate, the glyphs light in sequence, and every count is bounded. */
class RuneGlyphLayoutTest {
    private static final RuneSpec TRUE = new RuneSpec(RuneElement.FIRE, RuneMods.ALL, true), PLAIN = new RuneSpec(RuneElement.WATER, RuneMods.HOMING, false);

    private static Layout fill(RuneSpec s, double age, int level, double[] look) {
        Layout l = new Layout();
        Params p = RuneGeneratorUtil.paramsFor(s);
        Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, look[0], look[1], look[2]);
        RuneGlyphLayout.fill(l, s, p, f, age, RuneRules.chargeTicks(s), RuneGlyphLayout.LEVEL_FULL, 10, 72, 10);
        return l;
    }

    private static double dot(float[] a, int i, double x, double y, double z) { return a[i * 3] * x + a[i * 3 + 1] * y + a[i * 3 + 2] * z; }

    @Test
    void everyGlyphFacesTheCasterAndReadsAlongItsRing() {
        for (double[] look : new double[][]{{0, 0, 1}, {1, -0.4, 0.2}, {-0.3, 0.8, -0.5}, {0.01, -1, 0.02}}) {
            Layout l = fill(TRUE, 30, RuneGlyphLayout.LEVEL_FULL, look);
            Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, look[0], look[1], look[2]);
            assertTrue(l.glyphs > 30);
            for (int i = 0; i < l.glyphs; i++) {
                // the glyph's normal (right x up) points the way the plane's normal does (toward the caster) for every ring glyph, tilted ones within 30 degrees
                double rx = l.gr[i * 3], ry = l.gr[i * 3 + 1], rz = l.gr[i * 3 + 2], ux = l.gu[i * 3], uy = l.gu[i * 3 + 1], uz = l.gu[i * 3 + 2];
                double nx = ry * uz - rz * uy, ny = rz * ux - rx * uz, nz = rx * uy - ry * ux, nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (nl < 1e-9) continue;                                                    // a glyph that has not lit yet has no size
                double cos = (nx * f.nx() + ny * f.ny() + nz * f.nz()) / nl;
                assertTrue(cos > 0.85, "glyph " + i + " (ring " + l.ring[i] + ") faces away: " + cos);
                assertEquals(0.0, rx * ux + ry * uy + rz * uz, 1e-5, "right and up are perpendicular");
            }
        }
    }

    @Test
    void theRingsAreCirclesInThePlaneAndTheGlyphsStandOnThemPointingOutward() {
        Layout l = fill(TRUE, 40, RuneGlyphLayout.LEVEL_FULL, new double[]{0.3, -0.2, 1});
        Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, 0.3, -0.2, 1);
        double[] radius = {-1, -1};
        for (int i = 0; i < l.glyphs; i++) {
            if (l.ring[i] > 1) continue;
            double dx = l.gc[i * 3] - f.cx(), dy = l.gc[i * 3 + 1] - f.cy(), dz = l.gc[i * 3 + 2] - f.cz();
            assertEquals(0.0, dx * f.nx() + dy * f.ny() + dz * f.nz(), 1e-4, "ring " + l.ring[i] + " lies in the plane");
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (radius[l.ring[i]] < 0) radius[l.ring[i]] = d;
            assertEquals(radius[l.ring[i]], d, 1e-4, "ring " + l.ring[i] + " is a circle");
            double up = Math.sqrt(Math.pow(l.gu[i * 3], 2) + Math.pow(l.gu[i * 3 + 1], 2) + Math.pow(l.gu[i * 3 + 2], 2));
            assertEquals(1.0, (dx * l.gu[i * 3] + dy * l.gu[i * 3 + 1] + dz * l.gu[i * 3 + 2]) / (d * up), 1e-4, "the glyph's up is the outward radius");
        }
        assertTrue(radius[0] > 0 && radius[1] > radius[0], "the hand ring is inside the middle ring");
        double p = RuneGeneratorUtil.paramsFor(TRUE).radius();
        assertTrue(radius[1] < p, "inside the rune's radius");
    }

    @Test
    void theRingsTurnAgainstEachOther() {
        Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, 0, 0, 1);
        double[] a0 = new double[3], a1 = new double[3];
        Layout l0 = fill(TRUE, 20, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1}), l1 = fill(TRUE, 28, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1});
        f = RuneGeneratorUtil.frameFacing(3, 70, -2, 0, 0, 1);
        double[] turn = new double[2];
        for (int ring = 0; ring < 2; ring++) {
            int first = -1;
            for (int i = 0; i < l0.glyphs && first < 0; i++) if (l0.ring[i] == ring) first = i;
            double ang0 = angle(l0, first, f), ang1 = angle(l1, first, f);
            double d = ang1 - ang0;
            while (d > Math.PI) d -= 2 * Math.PI;
            while (d < -Math.PI) d += 2 * Math.PI;
            turn[ring] = d;
        }
        assertTrue(turn[0] > 0.3 && turn[1] < -0.1, "the hand ring turns one way (" + turn[0] + "), the middle ring the other (" + turn[1] + ")");
    }

    private static double angle(Layout l, int i, Frame f) {
        double dx = l.gc[i * 3] - f.cx(), dy = l.gc[i * 3 + 1] - f.cy(), dz = l.gc[i * 3 + 2] - f.cz();
        return Math.atan2(dx * f.vx() + dy * f.vy() + dz * f.vz(), dx * f.ux() + dy * f.uy() + dz * f.uz());
    }

    @Test
    void theGlyphsLightUpOneAfterTheOtherHandRingFirst() {
        int charge = RuneRules.chargeTicks(TRUE);
        int prevLit = -1;
        double[] look = {0, 0, 1};
        int firstOuter = -1, firstHand = -1;
        for (int age = 0; age <= charge + 2; age++) {
            Layout l = fill(TRUE, age, RuneGlyphLayout.LEVEL_FULL, look);
            int lit = 0;
            boolean handLit = false, outerLit = false;
            for (int i = 0; i < l.glyphs; i++) {
                if (l.light[i] > 0.01f) lit++;
                if (l.light[i] > 0.01f && l.ring[i] == 0) handLit = true;
                if (l.light[i] > 0.01f && l.ring[i] == 2) outerLit = true;
            }
            assertTrue(lit >= prevLit, "the number of lit glyphs never falls (age " + age + ")");
            prevLit = lit;
            if (handLit && firstHand < 0) firstHand = age;
            if (outerLit && firstOuter < 0) firstOuter = age;
        }
        assertTrue(firstHand >= 0 && firstOuter > firstHand, "the hand ring lights before the outer ring (" + firstHand + " < " + firstOuter + ")");
        Layout done = fill(TRUE, charge + 2, RuneGlyphLayout.LEVEL_FULL, look);
        for (int i = 0; i < done.glyphs; i++) assertTrue(done.light[i] >= 0.99f, "glyph " + i + " is lit when the charge is over");
        Layout none = fill(TRUE, 0, RuneGlyphLayout.LEVEL_FULL, look);
        for (int i = 0; i < none.glyphs; i++) assertEquals(0.0f, none.light[i], 0.0f, "nothing is lit at the first tick");
    }

    @Test
    void aGlyphFlashesWhenItLightsAndSettles() {
        assertEquals(0.0, RuneGlyphLayout.lightAt(0.5, 0.4, 18), 0.0);
        assertEquals(0.0, RuneGlyphLayout.lightAt(0.5, 0.5, 18), 0.0);
        double peak = 0;
        for (double t = 0; t < 12; t += 0.25) peak = Math.max(peak, RuneGlyphLayout.lightAt(0.2, 0.2 + t / 18.0, 18));
        assertEquals(1.6, peak, 1e-9);
        assertEquals(1.0, RuneGlyphLayout.lightAt(0.2, 1.0, 18), 1e-9);
        double prev = 0;
        for (double t = 0; t <= 3; t += 0.25) { double v = RuneGlyphLayout.lightAt(0.0, t / 18.0, 18); assertTrue(v >= prev); prev = v; }
    }

    @Test
    void everyCountIsBoundedAtEveryLevelAndShrinksWithTheDetail() {
        for (RuneSpec s : new RuneSpec[]{TRUE, PLAIN, new RuneSpec(RuneElement.EARTH, 0, false), new RuneSpec(RuneElement.LIGHTNING, RuneMods.ALL, true)}) {
            Params p = RuneGeneratorUtil.paramsFor(s);
            int prev = Integer.MAX_VALUE;
            for (int level = RuneGlyphLayout.LEVEL_FULL; level <= RuneGlyphLayout.LEVEL_MIN; level++) {
                int q = RuneGlyphLayout.quadsAt(p, level);
                assertTrue(q <= RuneGlyphLayout.MAX_QUADS && q <= prev, "level " + level + " has " + q + " quads");
                prev = q;
                for (double age : new double[]{0, 5, 9, 14, 18, 22, 60}) {
                    Layout l = new Layout();
                    Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, 0.2, 0.1, 1);
                    RuneGlyphLayout.fill(l, s, p, f, age, RuneRules.chargeTicks(s), level, 5, 5, -5);
                    assertTrue(l.glyphs <= RuneGlyphLayout.MAX_GLYPHS && l.arcQuads <= RuneGlyphLayout.MAX_ARC_QUADS && l.bookQuads <= RuneGlyphLayout.BOOK_QUADS);
                    assertTrue(l.quads() <= q, "the filled quads never exceed the level's count");
                    assertTrue(l.quads() <= RuneGlyphLayout.MAX_QUADS);
                }
            }
            assertEquals(0, RuneGlyphLayout.quadsAt(p, RuneGlyphLayout.LEVEL_NONE));
        }
        assertTrue(RuneGlyphLayout.MAX_QUADS * RuneGeneratorUtil.MAX_RUNES_DRAWN <= 440, "six runes at most 432 quads = 1728 vertices a frame");
        assertEquals(72, RuneGlyphLayout.MAX_QUADS);
        assertEquals(RuneGlyphLayout.LEVEL_NONE, RuneGlyphLayout.levelFor(0f));
        assertEquals(RuneGlyphLayout.LEVEL_NONE, RuneGlyphLayout.levelFor(Float.NaN));
        assertEquals(RuneGlyphLayout.LEVEL_FULL, RuneGlyphLayout.levelFor(1f));
        assertEquals(RuneGlyphLayout.LEVEL_MEDIUM, RuneGlyphLayout.levelFor(0.75f));
        assertEquals(RuneGlyphLayout.LEVEL_LOW, RuneGlyphLayout.levelFor(0.5f));
        assertEquals(RuneGlyphLayout.LEVEL_MIN, RuneGlyphLayout.levelFor(0.1f));
        assertTrue(RuneGlyphLayout.book(RuneGlyphLayout.LEVEL_FULL) && !RuneGlyphLayout.book(RuneGlyphLayout.LEVEL_MEDIUM));
    }

    @Test
    void theLightningArcsComeLateAreBoundedAndFaceTheCamera() {
        Layout early = fill(TRUE, 3, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1});
        assertEquals(0, early.arcQuads, "no arcs while the rings are still forming");
        int most = 0;
        for (double age = 12; age < 60; age += 1) {
            Layout l = fill(TRUE, age, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1});
            most = Math.max(most, l.arcQuads);
            assertTrue(l.arcQuads <= RuneGlyphLayout.MAX_ARC_QUADS && l.arcQuads % RuneGlyphLayout.ARC_SEGMENTS == 0);
            for (int q = 0; q < l.arcQuads; q++) {
                double wx = l.aq[q * 12 + 3] - l.aq[q * 12], wy = l.aq[q * 12 + 4] - l.aq[q * 12 + 1], wz = l.aq[q * 12 + 5] - l.aq[q * 12 + 2];
                assertEquals(0.06, Math.sqrt(wx * wx + wy * wy + wz * wz), 1e-4, "the strip is 0.06 wide");
                double mx = (l.aq[q * 12] + l.aq[q * 12 + 3] + l.aq[q * 12 + 6] + l.aq[q * 12 + 9]) / 4, my = (l.aq[q * 12 + 1] + l.aq[q * 12 + 4] + l.aq[q * 12 + 7] + l.aq[q * 12 + 10]) / 4,
                        mz = (l.aq[q * 12 + 2] + l.aq[q * 12 + 5] + l.aq[q * 12 + 8] + l.aq[q * 12 + 11]) / 4;
                assertEquals(0.0, (wx * (10 - mx) + wy * (72 - my) + wz * (10 - mz)) / (0.06 * Math.sqrt(Math.pow(10 - mx, 2) + Math.pow(72 - my, 2) + Math.pow(10 - mz, 2))), 0.2, "the strip is turned to the camera");
            }
        }
        assertTrue(most >= RuneGlyphLayout.ARC_SEGMENTS, "arcs do appear");
        Layout none = new Layout();
        RuneGlyphLayout.fill(none, TRUE, RuneGeneratorUtil.paramsFor(TRUE), RuneGeneratorUtil.frameFacing(0, 0, 0, 0, 0, 1), 40, 26, RuneGlyphLayout.LEVEL_MIN, 0, 0, 0);
        assertEquals(0, none.arcQuads, "the lowest level has no arcs");
    }

    @Test
    void theBookHoversAtTheCastersSideOnlyAtFullDetail() {
        Layout l = fill(TRUE, 5, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1});
        assertEquals(RuneGlyphLayout.BOOK_QUADS, l.bookQuads);
        Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, 0, 0, 1);
        double dx = l.bq[24] - f.cx(), dz = l.bq[26] - f.cz();
        assertTrue(Math.sqrt(dx * dx + dz * dz) < 3.0, "the book is next to the rune");
        assertEquals(0, fill(TRUE, 0.5, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1}).bookQuads, "it appears once the rune has begun");
        Layout medium = new Layout();
        RuneGlyphLayout.fill(medium, TRUE, RuneGeneratorUtil.paramsFor(TRUE), f, 10, 26, RuneGlyphLayout.LEVEL_MEDIUM, 0, 0, 0);
        assertEquals(0, medium.bookQuads);
    }

    @Test
    void theLayoutIsDeterministicAndAllocationFreeInItsApi() throws IOException {
        Layout a = fill(TRUE, 13, RuneGlyphLayout.LEVEL_FULL, new double[]{0.4, 0.1, 0.9}), b = fill(TRUE, 13, RuneGlyphLayout.LEVEL_FULL, new double[]{0.4, 0.1, 0.9});
        assertArrayEquals(a.gc, b.gc, 0f);
        assertArrayEquals(a.aq, b.aq, 0f);
        assertArrayEquals(a.light, b.light, 0f);
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/mana/method/RuneGlyphLayout.java"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        int at = src.indexOf("public static void fill(");
        String body = src.substring(at, src.indexOf("private static void scale("));
        assertFalse(body.contains("new "), "fill allocates nothing");
        assertFalse(src.contains("Math.random") || src.contains("new Random"), "no unseeded randomness");
        Set<Integer> cells = new HashSet<>();
        for (int i = 0; i < a.glyphs; i++) { assertTrue(a.cell[i] >= 0 && a.cell[i] < RuneGlyphLayout.GLYPH_COUNT); cells.add(a.cell[i]); }
        assertTrue(cells.size() >= 12, "the glyphs of one rune are not all the same symbol (" + cells.size() + ")");
    }

    @Test
    void theGlyphAtlasIsThirtyTwoDistinctGlyphsPerTint() throws IOException {
        Path dir = Path.of("docs/previews/mana_method/glyph_atlas");
        try (Stream<Path> s = Files.list(dir)) { assertTrue(s.count() >= 7, "gold plus six element tints"); }
        for (String tint : new String[]{"gold", "flame", "water", "wind", "earth", "lightning", "plant"}) {
            BufferedImage im = ImageIO.read(dir.resolve("rune_glyphs_" + tint + ".png").toFile());
            assertEquals(RuneGlyphLayout.ATLAS_COLS * 32, im.getWidth());
            assertEquals(RuneGlyphLayout.ATLAS_ROWS * 32, im.getHeight());
            Set<String> seen = new HashSet<>();
            for (int cell = 0; cell < RuneGlyphLayout.GLYPH_COUNT; cell++) {
                StringBuilder sb = new StringBuilder();
                int ink = 0;
                for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
                    int argb = im.getRGB((cell % 8) * 32 + x, (cell / 8) * 32 + y);
                    int a = argb >>> 24;
                    if (a > 128) ink++;
                    sb.append(a > 128 ? '1' : '0');
                }
                assertTrue(ink > 60 && ink < 600, tint + " glyph " + cell + " has " + ink + " inked pixels");
                assertTrue(seen.add(sb.toString()), tint + " glyph " + cell + " repeats an earlier one");
            }
        }
    }
}
