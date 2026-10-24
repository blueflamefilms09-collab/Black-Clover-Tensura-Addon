package com.newuniverse.nusmp.mana.method;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
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
    void aFarRuneIsNotDrawnAndAnOwnRuneAlwaysIs() {
        double far = RuneGeneratorUtil.CULL_DISTANCE + 1;
        assertEquals(0f, RuneGeneratorUtil.distanceDetailSq(far * far, false));
        assertEquals(1f, RuneGeneratorUtil.distanceDetailSq(far * far, true));
        assertEquals(1f, RuneGeneratorUtil.distanceDetailSq(4.0, false));
        assertTrue(RuneGeneratorUtil.distanceDetailSq(40 * 40, false) < 1f);
        assertEquals(RuneGeneratorUtil.MAX_RUNES_DRAWN, 6);
        assertEquals(32.0, RuneGeneratorUtil.BOOK_DISTANCE, 0.0);
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
