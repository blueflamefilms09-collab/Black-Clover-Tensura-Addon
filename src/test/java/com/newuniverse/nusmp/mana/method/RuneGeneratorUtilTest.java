package com.newuniverse.nusmp.mana.method;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Geometry;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure maths of the rune array: the plane faces the caster, the rings are circles, the figure is bounded and cached, the draw budget is a stride. */
class RuneGeneratorUtilTest {
    private static final double EPS = 1e-9;

    private static double[][] looks() {
        Random r = new Random(7);
        double[][] v = new double[60][];
        double[][] axes = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {1, 1, 0}, {0.3, -0.8, 0.5}, {-2, 0.1, 3}, {0, 0.9999, 0.01}};
        for (int i = 0; i < v.length; i++) v[i] = i < axes.length ? axes[i] : new double[]{r.nextGaussian(), r.nextGaussian(), r.nextGaussian()};
        return v;
    }

    private static double[] unit(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return new double[]{a[0] / l, a[1] / l, a[2] / l};
    }

    // ---------------------------------------------------------------- the plane faces the caster
    @Test
    void theNormalOfThePlaneIsMinusTheLookVectorForEveryLook() {
        for (double[] look : looks()) {
            double[] l = unit(look);
            Frame f = RuneGeneratorUtil.frameFacing(1, 2, 3, look[0], look[1], look[2]);
            assertEquals(-l[0], f.nx(), 1e-9, "nx for " + java.util.Arrays.toString(look));
            assertEquals(-l[1], f.ny(), 1e-9, "ny for " + java.util.Arrays.toString(look));
            assertEquals(-l[2], f.nz(), 1e-9, "nz for " + java.util.Arrays.toString(look));
        }
    }

    @Test
    void theAxesAreOrthonormalAndRightHanded() {
        for (double[] look : looks()) {
            Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, look[0], look[1], look[2]);
            assertEquals(1.0, f.ux() * f.ux() + f.uy() * f.uy() + f.uz() * f.uz(), 1e-9);
            assertEquals(1.0, f.vx() * f.vx() + f.vy() * f.vy() + f.vz() * f.vz(), 1e-9);
            assertEquals(0.0, f.ux() * f.vx() + f.uy() * f.vy() + f.uz() * f.vz(), 1e-9, "u.v");
            assertEquals(0.0, f.ux() * f.nx() + f.uy() * f.ny() + f.uz() * f.nz(), 1e-9, "u.n");
            assertEquals(0.0, f.vx() * f.nx() + f.vy() * f.ny() + f.vz() * f.nz(), 1e-9, "v.n");
            // u x v = n
            double cx = f.uy() * f.vz() - f.uz() * f.vy(), cy = f.uz() * f.vx() - f.ux() * f.vz(), cz = f.ux() * f.vy() - f.uy() * f.vx();
            assertEquals(f.nx(), cx, 1e-9);
            assertEquals(f.ny(), cy, 1e-9);
            assertEquals(f.nz(), cz, 1e-9);
        }
    }

    @Test
    void thePlaneHasNoRollSoTheSymbolsStayUpright() {
        for (double[] look : looks()) {
            double[] l = unit(look);
            if (Math.abs(l[1]) > 0.999) continue;                                         // looking straight up or down: roll is undefined
            Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, look[0], look[1], look[2]);
            assertEquals(0.0, f.uy(), 1e-9, "the plane's x axis is horizontal for " + java.util.Arrays.toString(look));
            assertTrue(f.vy() > 0.0, "the plane's up axis points up for " + java.util.Arrays.toString(look));
        }
    }

    @Test
    void everyPointOfTheFigureLiesInThePlane() {
        for (int complexity = 1; complexity <= RuneGeneratorUtil.MAX_COMPLEXITY; complexity++) {
            Params p = RuneGeneratorUtil.of(1.7, complexity, "rune_flame", 0xFF8800);
            Geometry g = RuneGeneratorUtil.geometry(p);
            double[] out = new double[3];
            for (double[] look : looks()) {
                Frame f = RuneGeneratorUtil.frameFacing(4, 70, -9, look[0], look[1], look[2]);
                for (int i = 0; i < g.count; i++) {
                    RuneGeneratorUtil.toWorld(g, i, p.radius(), f, 0.7, out);
                    double d = (out[0] - 4) * f.nx() + (out[1] - 70) * f.ny() + (out[2] + 9) * f.nz();
                    assertEquals(0.0, d, 1e-9, "point " + i + " is out of the plane");
                }
            }
        }
    }

    @Test
    void theRingsAreCirclesOfTheRightRadiusAroundTheCentre() {
        Params p = RuneGeneratorUtil.of(2.0, 3, "rune_wave", 0x3FA9FF);
        Geometry g = RuneGeneratorUtil.geometry(p);
        double[] out = new double[3];
        Frame f = RuneGeneratorUtil.frameFacing(10, 64, 10, 0.2, -0.3, 0.9);
        int outer = 0;
        for (int i = 0; i < g.count; i++) {
            if (g.kind[i] != RuneGeneratorUtil.KIND_RING) continue;
            RuneGeneratorUtil.toWorld(g, i, p.radius(), f, 1.3, out);
            double d = Math.sqrt(Math.pow(out[0] - 10, 2) + Math.pow(out[1] - 64, 2) + Math.pow(out[2] - 10, 2));
            double ringRadius = p.radius() * (1.0 - 0.17 * g.ring[i]);
            assertEquals(ringRadius, d, 1e-5, "ring " + g.ring[i] + " point " + i);
            if (g.ring[i] == 0) outer++;
        }
        assertEquals((int) Math.round(2 * Math.PI * RuneGeneratorUtil.RING_DENSITY), outer, "the outer ring has 2 pi r x density points");
    }

    @Test
    void theSymbolsAndTheStarKeepTheirRadiusWhenTheyTurn() {
        Params p = RuneGeneratorUtil.of(1.5, 2, "rune_key", 0xB59A6A);
        Geometry g = RuneGeneratorUtil.geometry(p);
        double[] a = new double[3], b = new double[3];
        Frame f = RuneGeneratorUtil.frameFacing(0, 0, 0, 1, 0.2, 0.3);
        for (int i = 0; i < g.count; i++) {
            RuneGeneratorUtil.toWorld(g, i, p.radius(), f, 0.0, a);
            RuneGeneratorUtil.toWorld(g, i, p.radius(), f, 2.1, b);
            assertEquals(Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]), Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]), 1e-9, "point " + i);
        }
    }

    @Test
    void aDegenerateLookGivesTheIdentityFacing() {
        for (double[] look : new double[][]{{0, 0, 0}, {Double.NaN, 1, 1}, {1e-12, 0, 0}}) {
            Quaterniond q = RuneGeneratorUtil.faceQuaternion(look[0], look[1], look[2]);
            Vector3d n = q.transform(new Vector3d(0, 0, 1));
            assertEquals(1.0, n.z, EPS);
            assertEquals(0.0, n.x, EPS);
            assertEquals(0.0, n.y, EPS);
        }
    }

    // ---------------------------------------------------------------- the figure
    @Test
    void theFigureIsBoundedCachedAndOrderedRingsFirst() {
        Set<Integer> sizes = new HashSet<>();
        for (int c = RuneGeneratorUtil.MIN_COMPLEXITY; c <= RuneGeneratorUtil.MAX_COMPLEXITY; c++) {
            Params p = RuneGeneratorUtil.of(1.0, c, "rune_flame", 0xFFFFFF);
            Geometry g = RuneGeneratorUtil.geometry(p);
            assertSame(g, RuneGeneratorUtil.geometry(RuneGeneratorUtil.of(3.0, c, "rune_bolt", 0x123456)), "the cache is keyed by rings and symbols only");
            assertTrue(g.count > 0 && g.count <= RuneGeneratorUtil.MAX_POINTS, "points " + g.count);
            assertEquals(c + 2, g.stageEnd.length, "one stage per ring, the symbols, the star");
            assertEquals(g.count, g.stageEnd[g.stageEnd.length - 1]);
            for (int s = 1; s < g.stageEnd.length; s++) assertTrue(g.stageEnd[s] > g.stageEnd[s - 1], "stage " + s + " is not empty");
            for (int i = 0; i < g.stageEnd[c - 1]; i++) assertEquals(RuneGeneratorUtil.KIND_RING, g.kind[i], "the rings come first");
            for (int i = g.stageEnd[c - 1]; i < g.stageEnd[c]; i++) assertEquals(RuneGeneratorUtil.KIND_GLYPH, g.kind[i], "then the symbols");
            for (int i = g.stageEnd[c]; i < g.count; i++) assertEquals(RuneGeneratorUtil.KIND_LINE, g.kind[i], "then the star");
            assertEquals(p.symbols(), g.stageEnd[c] - g.stageEnd[c - 1], "one mark per symbol");
            sizes.add(g.count);
            for (int i = 0; i < g.count; i++) assertTrue(g.x[i] * g.x[i] + g.y[i] * g.y[i] <= 1.0 + 1e-6, "inside the unit circle");
        }
        assertTrue(sizes.size() >= 4, "more complexity, more points");
        assertTrue(RuneGeneratorUtil.cacheSize() <= 64);
    }

    @Test
    void theParametersAreClampedAndGrowWithTheSpell() {
        Params wild = new Params(Double.NaN, 99, null, null, 0);
        assertEquals(1.0, wild.radius(), EPS);
        assertEquals(RuneGeneratorUtil.MAX_COMPLEXITY, wild.complexity());
        assertEquals(RuneElement.RING_PARTICLE, wild.ringParticle());
        assertEquals(RuneGeneratorUtil.MIN_COMPLEXITY, new Params(1.0, -4, null, null, 0).complexity());
        assertEquals(4.0, new Params(100.0, 1, null, null, 0).radius(), EPS);
        assertEquals(0.3, new Params(0.0, 1, null, null, 0).radius(), EPS);
        for (RuneElement e : RuneElement.values()) {
            Params plain = RuneGeneratorUtil.paramsFor(new RuneSpec(e, RuneMods.NONE, false));
            Params both = RuneGeneratorUtil.paramsFor(new RuneSpec(e, RuneMods.ALL, false));
            Params truth = RuneGeneratorUtil.paramsFor(new RuneSpec(e, RuneMods.ALL, true));
            assertTrue(plain.complexity() < both.complexity() && both.complexity() < truth.complexity() || both.complexity() == RuneGeneratorUtil.MAX_COMPLEXITY, e + " complexity");
            assertTrue(plain.radius() < both.radius() && both.radius() < truth.radius(), e + " radius");
            assertEquals(e.glyph, plain.glyphParticle());
            assertEquals(e.tint, plain.tint());
        }
    }

    // ---------------------------------------------------------------- drawing itself
    @Test
    void theRuneDrawsItselfAsAGrowingPrefix() {
        Geometry g = RuneGeneratorUtil.geometry(RuneGeneratorUtil.paramsFor(new RuneSpec(RuneElement.FIRE, RuneMods.ALL, true)));
        assertEquals(0, RuneGeneratorUtil.visible(g, 0.0));
        assertEquals(g.count, RuneGeneratorUtil.visible(g, 1.0));
        assertEquals(g.count, RuneGeneratorUtil.visible(g, 5.0));
        assertEquals(0, RuneGeneratorUtil.visible(g, Double.NaN));
        int prev = 0;
        for (int i = 0; i <= 40; i++) {
            int v = RuneGeneratorUtil.visible(g, i / 40.0);
            assertTrue(v >= prev, "monotone");
            prev = v;
        }
    }

    @Test
    void everyPointIsSpawnedOnceAtEveryStrideAndTheBudgetIsBounded() {
        Geometry g = RuneGeneratorUtil.geometry(RuneGeneratorUtil.paramsFor(new RuneSpec(RuneElement.WATER, RuneMods.ALL, true)));
        for (int stride = 1; stride <= 4; stride++) {
            for (int charge : new int[]{18, 22, 26, 30}) {
                Set<Integer> spawned = new HashSet<>();
                int prev = 0, total = 0, peak = 0;
                for (int age = 0; age <= charge; age++) {
                    int now = RuneGeneratorUtil.visible(g, age / (double) charge);
                    int n = RuneGeneratorUtil.newPoints(prev, now, stride);
                    assertTrue(n <= RuneGeneratorUtil.MAX_PER_TICK);
                    for (int j = 0; j < n; j++) {
                        int idx = RuneGeneratorUtil.newPointIndex(prev, stride, j);
                        assertTrue(idx >= prev && idx < now, "the new point lies in the newly visible part");
                        assertEquals(0, idx % stride);
                        assertTrue(spawned.add(idx), "point " + idx + " spawned twice");
                    }
                    total += n;
                    peak = Math.max(peak, n);
                    prev = now;
                }
                assertEquals((g.count + stride - 1) / stride, total, "stride " + stride + " charge " + charge);
                assertTrue(peak <= 16, "no more than " + peak + " new points in one tick");
            }
        }
    }

    @Test
    void theStrideFollowsTheDetailAndZeroDetailDrawsNothing() {
        assertEquals(0, RuneGeneratorUtil.strideFor(0f));
        assertEquals(0, RuneGeneratorUtil.strideFor(-1f));
        assertEquals(0, RuneGeneratorUtil.strideFor(Float.NaN));
        assertEquals(1, RuneGeneratorUtil.strideFor(1f));
        assertEquals(2, RuneGeneratorUtil.strideFor(0.75f));
        assertEquals(3, RuneGeneratorUtil.strideFor(0.5f));
        assertEquals(4, RuneGeneratorUtil.strideFor(0.1f));
        assertEquals(0, RuneGeneratorUtil.newPoints(0, 100, 0));
        assertEquals(0, RuneGeneratorUtil.newPoints(50, 50, 1));
        assertEquals(0, RuneGeneratorUtil.newPoints(50, 40, 1));
        assertEquals(RuneGeneratorUtil.MAX_PER_TICK, RuneGeneratorUtil.newPoints(0, 10_000, 1));
    }

    @Test
    void aFarRuneIsNotDrawnAndAnOwnRuneAlwaysIs() {
        double far = RuneGeneratorUtil.CULL_DISTANCE + 1;
        assertEquals(0f, RuneGeneratorUtil.distanceDetailSq(far * far, false));
        assertEquals(1f, RuneGeneratorUtil.distanceDetailSq(far * far, true));
        assertEquals(1f, RuneGeneratorUtil.distanceDetailSq(4.0, false));
        assertTrue(RuneGeneratorUtil.distanceDetailSq(40 * 40, false) < 1f);
        assertEquals(RuneGeneratorUtil.MAX_RUNES_DRAWN, 6);
    }

    // ---------------------------------------------------------------- the particles are real
    @Test
    void everyParticleTheRuneUsesIsInTheCatalog() throws IOException {
        JsonArray ps = JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/particle_catalog.json"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("particles");
        Set<String> ids = new HashSet<>();
        for (JsonElement e : ps) ids.add(e.getAsJsonObject().get("id").getAsString());
        assertTrue(ids.contains(RuneElement.RING_PARTICLE), RuneElement.RING_PARTICLE);
        Set<String> glyphs = new HashSet<>();
        for (RuneElement e : RuneElement.values()) {
            assertTrue(ids.contains(e.glyph), e + " uses the particle " + e.glyph);
            assertTrue(glyphs.add(e.glyph), "each element has its own glyph");
        }
    }
}
