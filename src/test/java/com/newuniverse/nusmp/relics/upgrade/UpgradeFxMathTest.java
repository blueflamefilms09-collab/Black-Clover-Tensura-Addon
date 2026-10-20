package com.newuniverse.nusmp.relics.upgrade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the ambient layers of the upgraded relics run on a client tick for every wearer in view; their placement, rate and thinning are pure arithmetic and are tested here. */
class UpgradeFxMathTest {
    private static UpgradeFxData data() {
        return UpgradeFxData.parse("""
                {"id": "t", "layers": [
                  {"name": "m", "kind": "motes", "particle": "dust", "color": "FFF0C0", "per_second": 10, "box": [-0.5, 0.5, 0.2, 0.5, 1.5, 0.8], "speed": 0.01},
                  {"name": "r", "kind": "ring", "particle": "minecraft:end_rod", "color": "FFFFFF", "per_second": 8, "center": [0, 1.0, 0], "radius": 0.5, "spin": 0.25, "plane": "flat"},
                  {"name": "v", "kind": "ring", "particle": "minecraft:end_rod", "color": "FFFFFF", "per_second": 8, "center": [0, 1.0, 0.4], "radius": 0.5, "spin": 0.0, "plane": "vertical"},
                  {"name": "l", "kind": "line", "particle": "dust", "color": "FFFFFF", "per_second": 6, "from": [0, 0, 0], "to": [0, 2, 0]},
                  {"name": "t", "kind": "trail", "particle": "dust", "color": "FFFFFF", "per_second": 6, "center": [0, 1, 0.2], "radius": 0.4},
                  {"name": "g", "kind": "ground", "particle": "dust", "color": "FFFFFF", "per_second": 6, "center": [0, 0, 0], "radius": 1.2}
                ], "acts": {"a": [{"shape": "MAGIC_CIRCLE", "color": "FF0000", "duration": 20, "power": 3.0, "count": 12}]}}
                """);
    }

    @Test
    void parsesLayersAndActs() {
        UpgradeFxData d = data();
        assertEquals(6, d.layers().size());
        assertEquals(0xFFF0C0, d.layers().get(0).color());
        assertEquals(10f, d.layers().get(0).perSecond(), 0f);
        assertEquals(1, d.actFx("a").size());
        assertEquals("MAGIC_CIRCLE", d.actFx("a").get(0).shape());
        assertTrue(d.actFx("none").isEmpty());
        assertTrue(d.problems().isEmpty(), d.problems().toString());
        assertFalse(UpgradeFxData.parse("{\"layers\": [{\"kind\": \"swirl\", \"per_second\": 5}]}").problems().isEmpty(), "an unknown kind is reported");
        assertFalse(UpgradeFxData.parse("{\"layers\": [{\"kind\": \"motes\", \"per_second\": 500, \"box\": [0,0,0,1,1,1]}]}").problems().isEmpty(), "an absurd rate is reported");
    }

    @Test
    void distanceAndQualityThinTheRate() {
        assertEquals(1f, UpgradeFxMath.distanceFactor(0.0), 0f);
        assertEquals(1f, UpgradeFxMath.distanceFactor(144.0), 0f);
        assertEquals(0f, UpgradeFxMath.distanceFactor(576.0), 0f);
        assertEquals(0.5f, UpgradeFxMath.distanceFactor(18.0 * 18.0), 1e-4f);
        assertEquals(0.3f, UpgradeFxMath.qualityFactor(0), 0f);
        assertEquals(0.6f, UpgradeFxMath.qualityFactor(1), 0f);
        assertEquals(1.0f, UpgradeFxMath.qualityFactor(2), 0f);
    }

    @Test
    void emitCountIsTheRateWithAFractionalChanceAndACap() {
        assertEquals(0, UpgradeFxMath.emitCount(10f, 1f, 1f, 0.9, 4), "0.5 a tick, the dice say no");
        assertEquals(1, UpgradeFxMath.emitCount(10f, 1f, 1f, 0.2, 4), "0.5 a tick, the dice say yes");
        assertEquals(0, UpgradeFxMath.emitCount(10f, 0f, 1f, 0.0, 4), "out of range");
        assertEquals(4, UpgradeFxMath.emitCount(400f, 1f, 1f, 0.0, 4), "capped");
        assertEquals(2, UpgradeFxMath.emitCount(40f, 1f, 1f, 0.99, 4), "two a tick exactly");
        assertEquals(0, UpgradeFxMath.emitCount(0f, 1f, 1f, 0.0, 4));
    }

    @Test
    void pointsLandWhereTheKindsSayAndNeverOnNaN() {
        UpgradeFxData d = data();
        double[] out = new double[3];
        assertTrue(UpgradeFxMath.point(d.layers().get(0), 0, 0, 1, 0.5, 0.5, 0.5, 0.0, out));
        assertEquals(0.0, out[0], 1e-9);
        assertEquals(1.0, out[1], 1e-9);
        assertEquals(0.5, out[2], 1e-9);
        assertTrue(UpgradeFxMath.point(d.layers().get(1), 0, 0, 4, 0, 0, 0, 0.0, out));
        assertEquals(0.5, out[0], 1e-9, "a ring point at angle 0");
        assertEquals(1.0, out[1], 1e-9);
        assertEquals(0.0, out[2], 1e-9);
        assertTrue(UpgradeFxMath.point(d.layers().get(1), 0, 1, 4, 0, 0, 0, 0.0, out));
        assertEquals(0.0, out[0], 1e-9, "a quarter turn on");
        assertEquals(0.5, out[2], 1e-9);
        assertTrue(UpgradeFxMath.point(d.layers().get(2), 0, 1, 4, 0, 0, 0, 0.0, out));
        assertEquals(1.5, out[1], 1e-9, "a vertical ring stands up and keeps its depth");
        assertEquals(0.4, out[2], 1e-9);
        assertTrue(UpgradeFxMath.point(d.layers().get(3), 0, 0, 1, 0.25, 0.5, 0.5, 0.0, out));
        assertEquals(0.5, out[1], 1e-9, "a quarter of the way up the line");
        assertFalse(UpgradeFxMath.point(d.layers().get(4), 0, 0, 1, 0.5, 0.5, 0.5, 0.0, out), "no trail while standing still");
        assertTrue(UpgradeFxMath.point(d.layers().get(4), 0, 0, 1, 0.5, 0.5, 0.5, 0.3, out));
        assertTrue(out[2] > 0.2, "a trail falls behind the wearer");
        assertTrue(UpgradeFxMath.point(d.layers().get(5), 0, 0, 1, 0.3, 0.5, 0.5, 0.0, out));
        assertEquals(0.05, out[1], 1e-9, "the ground glow lies on the ground");
        assertTrue(Math.hypot(out[0], out[2]) <= 1.2 + 1e-9);
        for (int k = 0; k < d.layers().size(); k++) {
            UpgradeFxMath.point(d.layers().get(k), 123456L, 1, 3, 0.1, 0.9, 0.4, 0.2, out);
            for (double x : out) assertFalse(Double.isNaN(x));
        }
    }

    @Test
    void localPointsTurnWithTheBody() {
        double[] w = new double[3];
        UpgradeFxMath.toWorld(0.0, 1.0, 2.0, 3.0, w);                      // facing +z: the left hand is +x, behind is -z
        assertEquals(1.0, w[0], 1e-9);
        assertEquals(2.0, w[1], 1e-9);
        assertEquals(-3.0, w[2], 1e-9);
        UpgradeFxMath.toWorld(90.0, 0.0, 0.0, 1.0, w);                     // facing -x (west): behind is +x
        assertEquals(1.0, w[0], 1e-9);
        assertEquals(0.0, w[2], 1e-9);
        UpgradeFxMath.toWorld(180.0, 0.0, 0.0, 1.0, w);                    // facing -z: behind is +z
        assertEquals(0.0, w[0], 1e-9);
        assertEquals(1.0, w[2], 1e-9);
    }
}
