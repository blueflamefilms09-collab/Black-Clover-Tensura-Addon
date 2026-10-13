package com.newuniverse.nusmp.dungeon.nexus.nav;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The cone maths and the flame choice of the Ki-Resonance Lantern. */
class ConeTest {
    @Test
    void anglesBetweenVectors() {
        assertEquals(0.0, Cone.angleDeg(1, 0, 0, 5, 0, 0), 1e-9);
        assertEquals(90.0, Cone.angleDeg(1, 0, 0, 0, 3, 0), 1e-9);
        assertEquals(180.0, Cone.angleDeg(0, 0, 2, 0, 0, -7), 1e-9);
        assertEquals(45.0, Cone.angleDeg(1, 0, 0, 1, 1, 0), 1e-9);
        assertEquals(0.0, Cone.angleDeg(0, 0, 0, 1, 0, 0), "a vector with no length has no angle");
    }

    @Test
    void withinTheConeOfThirtyDegrees() {
        assertTrue(Cone.within(0, 0, 1, 0, 0, 10, FlameMode.CONE_DEG));
        assertTrue(Cone.within(0, 0, 1, 5, 0, 10, FlameMode.CONE_DEG), "26.6 degrees off");
        assertFalse(Cone.within(0, 0, 1, 7, 0, 10, FlameMode.CONE_DEG), "35 degrees off");
        assertFalse(Cone.within(0, 0, 1, 0, 0, -10, FlameMode.CONE_DEG), "behind");
        assertTrue(Cone.within(0, 0, 1, 0, 0, 0, FlameMode.CONE_DEG), "standing on it counts as ahead");
    }

    @Test
    void theFanCoversTheConeAndNeverLeavesIt() {
        double[][] dirs = {{0, 0, 1}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0.3, 0.9, 0.3}, {-0.7, -0.2, 0.4}, {0.05, 0.999, 0.0}, {0, 0, 0}};
        for (double[] f : dirs) {
            double[][] fan = Cone.fan(f[0], f[1], f[2], FlameMode.CONE_DEG);
            assertEquals(Cone.FAN_SIZE, fan.length);
            double len = Math.sqrt(f[0] * f[0] + f[1] * f[1] + f[2] * f[2]);
            double[] unit = len < 1e-9 ? new double[]{0, 0, 1} : new double[]{f[0] / len, f[1] / len, f[2] / len};
            assertArrayEquals(unit, fan[0], 1e-9, "the first ray is the direction itself");
            Set<String> seen = new HashSet<>();
            double widest = 0;
            for (double[] d : fan) {
                assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1e-9, "unit length");
                double a = Cone.angleDeg(unit[0], unit[1], unit[2], d[0], d[1], d[2]);
                assertTrue(a <= FlameMode.CONE_DEG + 1e-6, "inside the cone: " + a);
                widest = Math.max(widest, a);
                seen.add(String.format("%.5f,%.5f,%.5f", d[0], d[1], d[2]));
            }
            assertEquals(FlameMode.CONE_DEG, widest, 1e-6, "the outer ring reaches the edge of the cone");
            assertEquals(Cone.FAN_SIZE, seen.size(), "no two rays are the same");
        }
    }

    @Test
    void theFanOfANarrowConeIsNarrow() {
        double[][] fan = Cone.fan(0, 0, 1, 6.0);
        for (double[] d : fan) assertTrue(Cone.angleDeg(0, 0, 1, d[0], d[1], d[2]) <= 6.0 + 1e-6);
    }

    @Test
    void theFlameIsOutAmberBlueOrRed() {
        assertEquals(FlameMode.OFF, FlameMode.choose(false, true, true), "no fuel, no flame");
        assertEquals(FlameMode.AMBER, FlameMode.choose(true, false, false));
        assertEquals(FlameMode.BLUE, FlameMode.choose(true, true, false));
        assertEquals(FlameMode.RED, FlameMode.choose(true, false, true));
        assertEquals(FlameMode.BLUE, FlameMode.choose(true, true, true), "blue takes priority when both");
    }

    @Test
    void thePropertyValuesPickTheRightModelOverrides() {
        // the model overrides (relic-art) switch at >= 0.17 amber, >= 0.5 blue, >= 0.84 red
        assertEquals(0f, FlameMode.OFF.property);
        assertTrue(FlameMode.AMBER.property >= 0.17f && FlameMode.AMBER.property < 0.5f);
        assertTrue(FlameMode.BLUE.property >= 0.5f && FlameMode.BLUE.property < 0.84f);
        assertTrue(FlameMode.RED.property >= 0.84f && FlameMode.RED.property <= 1f);
        for (FlameMode m : FlameMode.values()) assertEquals(m, FlameMode.of(m.ordinal()));
        assertEquals(FlameMode.OFF, FlameMode.of(-1));
        assertEquals(FlameMode.OFF, FlameMode.of(99));
        assertFalse(FlameMode.OFF.lit());
        assertTrue(FlameMode.RED.lit());
    }

    @Test
    void aPropertyValueReadsBackAsItsFlame() {
        for (FlameMode m : FlameMode.values()) assertEquals(m, FlameMode.ofProperty(m.property), m.name());
        assertEquals(FlameMode.OFF, FlameMode.ofProperty(0.169f));
        assertEquals(FlameMode.AMBER, FlameMode.ofProperty(0.17f));
        assertEquals(FlameMode.AMBER, FlameMode.ofProperty(0.499f));
        assertEquals(FlameMode.BLUE, FlameMode.ofProperty(0.5f));
        assertEquals(FlameMode.BLUE, FlameMode.ofProperty(0.839f));
        assertEquals(FlameMode.RED, FlameMode.ofProperty(0.84f));
        assertEquals(FlameMode.RED, FlameMode.ofProperty(7f), "too large is still red");
        assertEquals(FlameMode.OFF, FlameMode.ofProperty(-1f));
        assertEquals(FlameMode.OFF, FlameMode.ofProperty(Float.NaN), "garbage is out");
        assertEquals(FlameMode.OFF, FlameMode.ofProperty(Float.NEGATIVE_INFINITY));
    }

    @Test
    void theMeshOfAPropertyValueFollowsTheFlame() {
        // what the renderer does: the mesh is chosen from the same number the model overrides read
        assertEquals("ki_resonance_lantern_off", FlameMode.ofProperty(0f).meshId());
        assertEquals("ki_resonance_lantern", FlameMode.ofProperty(0.34f).meshId());
        assertEquals("ki_resonance_lantern_blue", FlameMode.ofProperty(0.67f).meshId());
        assertEquals("ki_resonance_lantern_red", FlameMode.ofProperty(1.0f).meshId());
    }

    @Test
    void theMeshIdsAreTheRelicArtNames() {
        assertEquals("ki_resonance_lantern", FlameMode.AMBER.meshId());
        assertEquals("ki_resonance_lantern_off", FlameMode.OFF.meshId());
        assertEquals("ki_resonance_lantern_blue", FlameMode.BLUE.meshId());
        assertEquals("ki_resonance_lantern_red", FlameMode.RED.meshId());
    }
}
