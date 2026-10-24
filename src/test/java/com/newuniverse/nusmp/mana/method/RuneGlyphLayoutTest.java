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

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rune-writing layout (staged): glyphs are written stroke by stroke by a pen, read correctly (never mirrored) in the caster-facing plane, the rings are written at the same time
 * glyph after glyph, each element writes its own words, the glow cools behind the pen and every count is bounded.
 */
class RuneGlyphLayoutTest {
    private static final RuneSpec TRUE = new RuneSpec(RuneElement.FIRE, RuneMods.ALL, true), PLAIN = new RuneSpec(RuneElement.WATER, RuneMods.HOMING, false);
    private static final double[] LOOK = {0.2, -0.1, 1.0};

    private static Layout fill(RuneSpec s, double age, int level, double[] look) {
        Layout l = new Layout();
        Params p = RuneGeneratorUtil.paramsFor(s);
        Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, look[0], look[1], look[2]);
        RuneGlyphLayout.fill(l, s, p, f, age, RuneRules.chargeTicks(s), level, 10, 72, 10);
        return l;
    }

    private static double[] centre(float[] q, int i) {
        double[] c = new double[3];
        for (int k = 0; k < 4; k++) for (int a = 0; a < 3; a++) c[a] += q[i * 12 + k * 3 + a] / 4.0;
        return c;
    }

    /** The unit normal of a strip by its winding (start-left, start-right, end-right): +n of the glyph's frame when the glyph is written right, up. */
    private static double[] winding(float[] q, int i) {
        int b = i * 12;
        double ax = q[b + 3] - q[b], ay = q[b + 4] - q[b + 1], az = q[b + 5] - q[b + 2], bx = q[b + 6] - q[b + 3], by = q[b + 7] - q[b + 4], bz = q[b + 8] - q[b + 5];
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx, l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return new double[]{nx / l, ny / l, nz / l};
    }

    // ---------------------------------------------------------------- one glyph written by the pen
    @Test
    void thePenWalksTheStrokesAndTheLineGrowsWithIt() {
        Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, 0, 0, 1);
        Layout l = new Layout();
        for (int g = 0; g < RuneAlphabet.COUNT; g++) {
            int prev = 0;
            double prevLen = 0;
            for (double w = 0.043; w <= 1.0001; w += 0.05) {
                RuneGlyphLayout.writeSingle(l, g, w, f, 1.0, 10);
                assertTrue(l.strips >= prev, RuneAlphabet.name(g) + ": strips never disappear while the pen writes");
                double[] cum = RuneAlphabet.cumulative(g);
                int begun = 0;
                for (int s = 0; s < cum.length; s++) if ((s == 0 ? 0.0 : cum[s - 1]) < w * cum[cum.length - 1]) begun++;
                assertEquals(begun, l.strips, RuneAlphabet.name(g) + " at " + w + ": a strip per segment the pen has begun");
                assertTrue(RuneGlyphLayout.segmentsDone(g, w) <= l.strips);
                prev = l.strips;
                double len = 0;
                for (int i = 0; i < l.strips; i++) {
                    int b = i * 12;
                    len += Math.sqrt(Math.pow(l.sq[b + 6] - l.sq[b], 2) + Math.pow(l.sq[b + 7] - l.sq[b + 1], 2) + Math.pow(l.sq[b + 8] - l.sq[b + 2], 2));
                }
                assertTrue(len >= prevLen - 1e-4, "the written length never shrinks");
                prevLen = len;
                assertEquals(w < 1.0 - 1e-9 ? 1 : 0, l.pens, "a pen glow while writing, none when done");
            }
            assertEquals(RuneAlphabet.segments(g), l.strips, "a finished glyph has one strip per segment");
        }
        RuneGlyphLayout.writeSingle(l, 0, 0.0, f, 1.0, 10);
        assertEquals(0, l.strips);
        assertEquals(0, l.pens);
    }

    @Test
    void aGlyphIsReadRightWayRoundNeverMirrored() {
        // fehu: a stem on the left with two branches reaching to the right and up
        int fehu = 0;
        assertEquals("fehu", RuneAlphabet.name(fehu));
        for (double[] look : new double[][]{{0, 0, 1}, {1, -0.4, 0.2}, {-0.3, 0.8, -0.5}, {0.01, -1, 0.02}}) {
            Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, look[0], look[1], look[2]);
            Layout l = new Layout();
            RuneGlyphLayout.writeSingle(l, fehu, 1.0, f, 1.0, 10);
            double stem = 0, branch = 0;
            for (int i = 0; i < l.strips; i++) {
                double[] c = centre(l.sq, i);
                double[] n = winding(l.sq, i);
                assertTrue(n[0] * f.nx() + n[1] * f.ny() + n[2] * f.nz() > 0.99, "every stroke is wound toward the caster (a mirrored glyph would be wound away)");
                int b = i * 12;
                double ex = (l.sq[b + 6] + l.sq[b + 9]) / 2 - f.cx(), ey = (l.sq[b + 7] + l.sq[b + 10]) / 2 - f.cy(), ez = (l.sq[b + 8] + l.sq[b + 11]) / 2 - f.cz();
                double endU = ex * f.ux() + ey * f.uy() + ez * f.uz();                       // where the stroke ends, along the glyph's right axis
                if (i == 0) stem = c[0] * f.ux() + c[1] * f.uy() + c[2] * f.uz(); else branch += endU;
            }
            assertTrue(stem < -0.2, "the stem is on the left of the glyph (" + stem + ")");
            assertTrue(branch > 0.4, "the branches end on the right (" + branch + ")");
            double stemV = 0;
            double[] stemC = centre(l.sq, 0), branchC = centre(l.sq, 1);
            stemV = stemC[0] * f.vx() + stemC[1] * f.vy() + stemC[2] * f.vz();
            double branchV = branchC[0] * f.vx() + branchC[1] * f.vy() + branchC[2] * f.vz();
            assertTrue(branchV > stemV + 0.2, "the branches are above the middle of the stem: up is up");
        }
    }

    @Test
    void everyStrokeLiesInThePlaneInsideTheGlyphsBox() {
        for (double[] look : new double[][]{{0, 0, 1}, {1, -0.4, 0.2}, {0.01, -1, 0.02}}) {
            Frame f = RuneGeneratorUtil.frameFacing(5, 70, 5, look[0], look[1], look[2]);
            Layout l = new Layout();
            for (int g = 0; g < RuneAlphabet.COUNT; g++) {
                RuneGlyphLayout.writeSingle(l, g, 1.0, f, 2.0, 10);
                for (int i = 0; i < l.strips * 4; i++) {
                    double dx = l.sq[i * 3] - f.cx(), dy = l.sq[i * 3 + 1] - f.cy(), dz = l.sq[i * 3 + 2] - f.cz();
                    assertEquals(0.0, dx * f.nx() + dy * f.ny() + dz * f.nz(), 1e-4, "strokes are drawn in the plane");
                    double u = dx * f.ux() + dy * f.uy() + dz * f.uz(), v = dx * f.vx() + dy * f.vy() + dz * f.vz();
                    double lim = 2.0 * (2.0 / RuneGlyphLayout.MARGIN + RuneGlyphLayout.STROKE_HALF_WIDTH * 1.5) + 1e-6;
                    assertTrue(Math.abs(u) <= lim && Math.abs(v) <= lim, RuneAlphabet.name(g) + " leaves its box");
                }
            }
        }
    }

    @Test
    void theLineIsBrightestBehindThePenAndCoolsToItsRestingGlow() {
        assertEquals(RuneGlyphLayout.HEAD_LIGHT, RuneGlyphLayout.strokeLight(-1), 0.0);
        assertEquals(RuneGlyphLayout.HEAD_LIGHT, RuneGlyphLayout.strokeLight(0), 0.0);
        assertEquals(1.0, RuneGlyphLayout.strokeLight(RuneGlyphLayout.COOL_TICKS), 0.0);
        assertEquals(1.0, RuneGlyphLayout.strokeLight(100), 0.0);
        double prev = 9;
        for (double s = 0; s <= 4; s += 0.25) { double v = RuneGlyphLayout.strokeLight(s); assertTrue(v <= prev + 1e-12); prev = v; }
        for (int slot = 0; slot < 40; slot++) for (double age = 0; age < 100; age += 3) assertTrue(RuneGlyphLayout.breath(age, slot) >= 0.879 && RuneGlyphLayout.breath(age, slot) <= 1.121, "breathes by 12 % at most");
        // in a glyph being written, the strokes the pen left long ago are cooler than the one it is on
        Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, 0, 0, 1);
        Layout l = new Layout();
        RuneGlyphLayout.writeSingle(l, 17, 0.8, f, 1.0, 10);                               // berkana, 5 segments
        assertTrue(l.strips >= 4);
        assertEquals(RuneGlyphLayout.HEAD_LIGHT, l.stripLight[l.strips - 1], 1e-6, "the segment under the pen is at the writing glow");
        assertTrue(l.stripLight[0] < l.stripLight[l.strips - 1], "the first stroke has cooled");
    }

    // ---------------------------------------------------------------- the whole rune
    @Test
    void everyGlyphOfTheRingsIsReadTowardTheCaster() {
        for (double[] look : new double[][]{{0, 0, 1}, {1, -0.4, 0.2}, {-0.3, 0.8, -0.5}, {0.01, -1, 0.02}}) {
            Layout l = fill(TRUE, 40, RuneGlyphLayout.LEVEL_FULL, look);
            Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, look[0], look[1], look[2]);
            assertTrue(l.strips > 150, "strips " + l.strips);
            for (int i = 0; i < l.strips; i++) {
                double[] n = winding(l.sq, i);
                assertTrue(n[0] * f.nx() + n[1] * f.ny() + n[2] * f.nz() > 0.85, "strip " + i + " (ring " + l.stripRing[i] + ") is wound away from the caster");
            }
        }
    }

    @Test
    void theRingsAreWrittenAtOnceGlyphAfterGlyphHandRingFirstFloatingLast() {
        int charge = RuneRules.chargeTicks(TRUE);
        int[] first = {-1, -1, -1, -1};
        int prevStrips = 0;
        for (int age = 0; age <= charge + 1; age++) {
            Layout l = fill(TRUE, age, RuneGlyphLayout.LEVEL_FULL, LOOK);
            assertTrue(l.strips >= prevStrips, "nothing written is erased (age " + age + ")");
            prevStrips = l.strips;
            for (int i = 0; i < l.strips; i++) if (first[l.stripRing[i]] < 0) first[l.stripRing[i]] = age;
        }
        assertTrue(first[0] >= 0 && first[1] > first[0] - 1 && first[2] >= first[1] && first[3] > first[2], "the hand ring, then the middle, the outer ring, the floating glyphs: " + java.util.Arrays.toString(first));
        assertEquals(0, fill(TRUE, 0, RuneGlyphLayout.LEVEL_FULL, LOOK).strips, "nothing is written at the first tick");
        // glyph after glyph: at mid charge the hand ring is partly written, not all at once
        Layout mid = fill(TRUE, charge * 0.25, RuneGlyphLayout.LEVEL_FULL, LOOK);
        Set<Integer> handGlyphs = new HashSet<>();
        for (int i = 0; i < mid.strips; i++) if (mid.stripRing[i] == 0) handGlyphs.add(mid.stripGlyph[i]);
        assertTrue(handGlyphs.size() >= 1 && handGlyphs.size() < RuneGlyphLayout.ringGlyphs(0, RuneGlyphLayout.LEVEL_FULL), "a quarter in, some of the hand ring's glyphs are written, not all: " + handGlyphs.size());
        // when the charge is over, every glyph is complete
        Layout done = fill(TRUE, charge + 2, RuneGlyphLayout.LEVEL_FULL, LOOK);
        int expected = 0;
        for (int r = 0; r < 3; r++) for (int i = 0; i < RuneGlyphLayout.ringGlyphs(r, RuneGlyphLayout.LEVEL_FULL); i++) expected += RuneAlphabet.segments(RuneGlyphLayout.glyphFor(TRUE, r, i));
        int floating = RuneGlyphLayout.floatingGlyphs(RuneGeneratorUtil.paramsFor(TRUE), RuneGlyphLayout.LEVEL_FULL);
        for (int i = 0; i < floating; i++) expected += RuneAlphabet.segments(RuneGlyphLayout.glyphFor(TRUE, 3, i));
        assertEquals(expected, done.strips, "every glyph is written in full");
        assertEquals(0, done.pens, "no pen is left once everything is written");
    }

    @Test
    void eachElementWritesItsOwnWordsDeterministically() {
        Set<String> handWords = new HashSet<>();
        for (RuneElement e : RuneElement.values()) {
            for (boolean t : new boolean[]{false, true}) {
                RuneSpec s = new RuneSpec(e, RuneMods.GROWTH, t);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < RuneGlyphLayout.ringGlyphs(0, RuneGlyphLayout.LEVEL_FULL); i++) {
                    int g = RuneGlyphLayout.glyphFor(s, 0, i);
                    assertEquals(RuneAlphabet.phrase(e, t, i), g, "the hand ring writes the element's phrase");
                    assertEquals(g, RuneGlyphLayout.glyphFor(s, 0, i), "deterministic");
                    sb.append(g).append(',');
                }
                assertTrue(handWords.add(sb.toString()), e + (t ? " true" : "") + " writes the same words as another spell");
                for (int ring = 1; ring <= 3; ring++) for (int i = 0; i < 14; i++) { int g = RuneGlyphLayout.glyphFor(s, ring, i); assertTrue(g >= 0 && g < RuneAlphabet.COUNT); }
            }
        }
        int fromPhrase = 0, total = 0;
        for (int i = 0; i < 13; i++) {
            int g = RuneGlyphLayout.glyphFor(TRUE, 2, i);
            total++;
            for (int k = 0; k < RuneAlphabet.PHRASE_LENGTH; k++) if (RuneAlphabet.phrase(RuneElement.FIRE, true, k) == g) { fromPhrase++; break; }
        }
        assertTrue(fromPhrase >= 4, "the outer ring is mostly the spell's own words (" + fromPhrase + " of " + total + ")");
    }

    // ---------------------------------------------------------------- bounds
    @Test
    void everyCountIsBoundedAtEveryLevelAndShrinksWithTheDetail() {
        assertEquals(54, RuneGlyphLayout.MAX_GLYPH_SLOTS);
        assertEquals(54 * RuneAlphabet.MAX_SEGMENTS, RuneGlyphLayout.MAX_STRIPS);
        assertEquals(300, RuneGlyphLayout.MAX_QUADS);
        for (RuneSpec s : new RuneSpec[]{TRUE, PLAIN, new RuneSpec(RuneElement.EARTH, 0, false), new RuneSpec(RuneElement.LIGHTNING, RuneMods.ALL, true)}) {
            Params p = RuneGeneratorUtil.paramsFor(s);
            int prev = Integer.MAX_VALUE;
            for (int level = RuneGlyphLayout.LEVEL_FULL; level <= RuneGlyphLayout.LEVEL_MIN; level++) {
                int q = RuneGlyphLayout.quadsAt(p, level);
                assertTrue(q <= RuneGlyphLayout.MAX_QUADS && q <= prev, "level " + level + " allows " + q + " quads");
                prev = q;
                for (double age : new double[]{0, 5, 9, 14, 18, 22, 60}) {
                    Layout l = new Layout();
                    Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, 0.2, 0.1, 1);
                    RuneGlyphLayout.fill(l, s, p, f, age, RuneRules.chargeTicks(s), level, 5, 5, -5);
                    assertTrue(l.strips <= RuneGlyphLayout.glyphsAt(p, level) * RuneAlphabet.MAX_SEGMENTS, "strips");
                    assertTrue(l.pens <= RuneGlyphLayout.MAX_PENS && l.arcQuads <= RuneGlyphLayout.MAX_ARC_QUADS && l.lines <= RuneGlyphLayout.lines(level));
                    assertTrue(l.quads() <= q, "the filled quads never exceed the level's bound: " + l.quads() + " > " + q);
                }
            }
            assertEquals(0, RuneGlyphLayout.quadsAt(p, RuneGlyphLayout.LEVEL_NONE));
        }
        assertEquals(RuneGlyphLayout.LEVEL_NONE, RuneGlyphLayout.levelFor(0f));
        assertEquals(RuneGlyphLayout.LEVEL_NONE, RuneGlyphLayout.levelFor(Float.NaN));
        assertEquals(RuneGlyphLayout.LEVEL_FULL, RuneGlyphLayout.levelFor(1f));
        assertEquals(RuneGlyphLayout.LEVEL_MEDIUM, RuneGlyphLayout.levelFor(0.75f));
        assertEquals(RuneGlyphLayout.LEVEL_LOW, RuneGlyphLayout.levelFor(0.5f));
        assertEquals(RuneGlyphLayout.LEVEL_MIN, RuneGlyphLayout.levelFor(0.1f));
        assertTrue(RuneGlyphLayout.MAX_QUADS * RuneGeneratorUtil.MAX_RUNES_DRAWN <= 1800, "six runes at most 1800 quads = 7200 vertices a frame");
        assertEquals(1200, RuneGlyphLayout.MAX_QUADS * 4, "one rune is at most 1200 vertices: the layer asks the buffer for exactly that");
    }

    @Test
    void theLightningArcsComeLateAreBoundedAndFaceTheCamera() {
        assertEquals(0, fill(TRUE, 3, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1}).arcQuads, "no arcs while the rune is still being written");
        int most = 0;
        for (double age = 12; age < 60; age += 1) {
            Layout l = fill(TRUE, age, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1});
            most = Math.max(most, l.arcQuads);
            assertTrue(l.arcQuads <= RuneGlyphLayout.MAX_ARC_QUADS && l.arcQuads % RuneGlyphLayout.ARC_SEGMENTS == 0);
            for (int q = 0; q < l.arcQuads; q++) {
                double wx = l.aq[q * 12 + 3] - l.aq[q * 12], wy = l.aq[q * 12 + 4] - l.aq[q * 12 + 1], wz = l.aq[q * 12 + 5] - l.aq[q * 12 + 2];
                assertEquals(0.06, Math.sqrt(wx * wx + wy * wy + wz * wz), 1e-4, "the strip is 0.06 wide");
                double[] c = centre(l.aq, q);
                double dx = 10 - c[0], dy = 72 - c[1], dz = 10 - c[2];
                assertEquals(0.0, (wx * dx + wy * dy + wz * dz) / (0.06 * Math.sqrt(dx * dx + dy * dy + dz * dz)), 0.2, "the strip is turned to the camera");
            }
        }
        assertTrue(most >= RuneGlyphLayout.ARC_SEGMENTS, "arcs do appear");
        Layout none = new Layout();
        RuneGlyphLayout.fill(none, TRUE, RuneGeneratorUtil.paramsFor(TRUE), RuneGeneratorUtil.frameFacing(0, 0, 0, 0, 0, 1), 40, 26, RuneGlyphLayout.LEVEL_MIN, 0, 0, 0);
        assertEquals(0, none.arcQuads, "the lowest level has no arcs");
    }

    @Test
    void theRealBookHoversAtTheCastersSideOnlyAtFullDetail() {
        Layout l = fill(TRUE, 5, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1});
        assertTrue(l.bookLight > 0f, "the glow of the book is there once the rune has begun");
        Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, 0, 0, 1);
        double[] at = new double[3];
        RuneGlyphLayout.bookPos(f, RuneGeneratorUtil.paramsFor(TRUE).radius(), 5, at);
        assertEquals(at[0], l.bookX, 1e-9);
        assertEquals(at[1], l.bookY, 1e-9);
        assertEquals(at[2], l.bookZ, 1e-9);
        double dx = at[0] - f.cx(), dy = at[1] - f.cy(), dz = at[2] - f.cz();
        assertTrue(Math.sqrt(dx * dx + dy * dy + dz * dz) < 3.0, "the book is next to the rune");
        assertTrue(dx * f.ux() + dy * f.uy() + dz * f.uz() > 0.5, "on the caster's right");
        assertTrue(dx * f.vx() + dy * f.vy() + dz * f.vz() < -0.3, "and lower");
        assertEquals(0f, fill(TRUE, 0.5, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1}).bookLight, 0f, "it appears once the rune has begun");
        Layout medium = new Layout();
        RuneGlyphLayout.fill(medium, TRUE, RuneGeneratorUtil.paramsFor(TRUE), f, 10, 26, RuneGlyphLayout.LEVEL_MEDIUM, 0, 0, 0);
        assertEquals(0f, medium.bookLight, 0f);
    }

    @Test
    void theRingsFillTheirCircumferenceSoNeighboursNearlyTouch() {
        for (double R : new double[]{1.15, 1.55, 1.8}) {
            for (int r = 0; r < 3; r++) {
                double spacing = 2 * Math.PI * RuneGlyphLayout.ringRadius(r, R) / RuneGlyphLayout.RING_N[r], size = RuneGlyphLayout.glyphSize(r, R);
                assertEquals(RuneGlyphLayout.FILL, size / spacing, 1e-9, "ring " + r);
                assertTrue(size / spacing >= 0.9 && size / spacing < 1.0, "neighbours nearly touch but do not overlap");
            }
            assertTrue(RuneGlyphLayout.ringRadius(0, R) < RuneGlyphLayout.ringRadius(1, R) && RuneGlyphLayout.ringRadius(1, R) < RuneGlyphLayout.ringRadius(2, R) && RuneGlyphLayout.ringRadius(2, R) < R);
        }
        assertArrayEquals(new int[]{10, 16, 24}, RuneGlyphLayout.RING_N);
    }

    @Test
    void thereAreThinCircleLinesUnderEachRingAndABoundaryRingOutside() {
        Layout l = fill(TRUE, 40, RuneGlyphLayout.LEVEL_FULL, new double[]{0.2, -0.3, 1});
        Frame f = RuneGeneratorUtil.frameFacing(3, 70, -2, 0.2, -0.3, 1);
        double R = RuneGeneratorUtil.paramsFor(TRUE).radius();
        assertEquals(4, l.lines, "three ring lines and the boundary");
        double[] half = new double[4];
        for (int i = 0; i < 4; i++) {
            double[] c = centre(l.lq, i);
            double dx = c[0] - f.cx(), dy = c[1] - f.cy(), dz = c[2] - f.cz();
            double[] corner = {l.lq[i * 12] - c[0], l.lq[i * 12 + 1] - c[1], l.lq[i * 12 + 2] - c[2]};
            half[i] = Math.sqrt(corner[0] * corner[0] + corner[1] * corner[1] + corner[2] * corner[2]) / Math.sqrt(2.0);
            if (i < 2) assertEquals(0.0, dx * f.nx() + dy * f.ny() + dz * f.nz(), 1e-4, "the hand and middle lines lie in the plane");
            assertTrue(l.lineLight[i] > 0.3f && l.lineLight[i] <= 1.0f, "a thin, dim line");
        }
        for (int i = 1; i < 4; i++) assertTrue(half[i] > half[i - 1], "each line is outside the one before it (the last is the boundary)");
        assertTrue(half[3] * RuneGlyphLayout.LINE_RADIUS_IN_TEXTURE > RuneGlyphLayout.ringRadius(2, R), "the boundary is outside the outer ring's glyphs' baseline");
        assertEquals(0, fill(TRUE, 0, RuneGlyphLayout.LEVEL_FULL, new double[]{0, 0, 1}).lines, "no line before anything is written");
    }

    @Test
    void theLayoutIsDeterministicAndAllocatesNothing() throws IOException {
        Layout a = fill(TRUE, 13, RuneGlyphLayout.LEVEL_FULL, new double[]{0.4, 0.1, 0.9}), b = fill(TRUE, 13, RuneGlyphLayout.LEVEL_FULL, new double[]{0.4, 0.1, 0.9});
        assertArrayEquals(a.sq, b.sq, 0f);
        assertArrayEquals(a.aq, b.aq, 0f);
        assertArrayEquals(a.stripLight, b.stripLight, 0f);
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/mana/method/RuneGlyphLayout.java"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        for (String method : new String[]{"public static void fill(", "public static int write(", "private static void emit(", "private static void arc(", "public static void bookPos(", "private static void circle(", "public static void writeSingle("}) {
            int at = src.indexOf(method);
            assertTrue(at > 0, method);
            int open = src.indexOf('{', at), depth = 0, end = open;
            for (int i = open; i < src.length(); i++) {
                if (src.charAt(i) == '{') depth++;
                if (src.charAt(i) == '}' && --depth == 0) { end = i; break; }
            }
            assertFalse(src.substring(open, end).contains("new "), method + " allocates");
        }
        assertFalse(src.contains("Math.random") || src.contains("new Random"), "no unseeded randomness");
    }

    @Test
    void theTexturesOfTheWritingExistAndCannotDrawAsSquares() throws IOException {
        Path dir = Path.of("src/main/resources/assets/nusmp/textures/particle");
        for (String tint : new String[]{"gold", "flame", "water", "wind", "earth", "lightning", "plant"}) {
            BufferedImage im = ImageIO.read(dir.resolve("rune_stroke_" + tint + ".png").toFile());
            assertEquals(32, im.getWidth(), "across the stroke");
            assertEquals(48, im.getHeight(), "along the stroke");
            int centre = im.getRGB(16, 24), edge = im.getRGB(1, 24), end = im.getRGB(16, 0);
            assertTrue((centre >>> 24) > 200, tint + ": the core is solid");
            assertTrue((edge >>> 24) < (centre >>> 24), tint + ": the edge is softer than the core");
            assertTrue((end >>> 24) < 120, tint + ": the end of the stroke is rounded off");
            assertTrue(((centre >> 16) & 255) + ((centre >> 8) & 255) + (centre & 255) > ((edge >> 16) & 255) + ((edge >> 8) & 255) + (edge & 255), tint + ": a bright core in a darker edge");
            for (int x : new int[]{0, 31}) for (int y : new int[]{0, 47}) assertTrue((im.getRGB(x, y) >>> 24) < 16, tint + ": transparent corners");
        }
        BufferedImage ring = ImageIO.read(dir.resolve("rune_line_ring.png").toFile());
        assertEquals(128, ring.getWidth());
        assertEquals(128, ring.getHeight());
        for (int x : new int[]{0, 127}) for (int y : new int[]{0, 127}) assertTrue((ring.getRGB(x, y) >>> 24) < 8, "the ring line has transparent corners (it is drawn on a square quad)");
        assertTrue((ring.getRGB(64, 64) >>> 24) < 8, "and an empty middle");
        int on = (int) Math.round(RuneGlyphLayout.LINE_RADIUS_IN_TEXTURE * 64);
        assertTrue((ring.getRGB(64 + on, 64) >>> 24) > 150 || (ring.getRGB(64 + on - 1, 64) >>> 24) > 150, "the circle sits where the layout says (92 % of the half width)");
        BufferedImage arc = ImageIO.read(dir.resolve("rune_arc_strip.png").toFile());
        assertEquals(8, arc.getWidth());
        assertEquals(16, arc.getHeight());
    }
}
