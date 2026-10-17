package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Why: an illusion is only fair when it can be read, so there must always be a real ring under every foe, more illusions than real rings, nothing stacked, and the cap must drop illusions before real rings. */
class MarkMixTest {
    private static final double[][] TWO = {{0, 0}, {20, 5}};

    @Test
    void everyFoeHasARealRingUnderTheirFeetFirst() {
        List<MarkMix.Mark> m = MarkMix.plan(TWO, 2, 2, 4.0, 1.6, 18, new Random(7));
        assertTrue(m.get(0).real() && m.get(0).x() == 0.0 && m.get(0).z() == 0.0);
        assertTrue(m.get(1).real() && m.get(1).x() == 20.0 && m.get(1).z() == 5.0);
    }

    @Test
    void thereAreAlwaysMoreIllusionsThanRealRingsWhenThereIsRoom() {
        for (long seed = 1; seed <= 20; seed++) {
            List<MarkMix.Mark> m = MarkMix.plan(TWO, 2, 2, 4.0, 1.6, 18, new Random(seed));
            assertEquals(4, MarkMix.countReal(m), "two real rings for each of two foes");
            assertTrue(MarkMix.countFalse(m) > MarkMix.countReal(m), "the crowd of illusions is the misdirection (seed " + seed + ")");
        }
    }

    @Test
    void noTwoRingsStackAndTheCapHolds() {
        for (long seed = 1; seed <= 20; seed++) {
            List<MarkMix.Mark> m = MarkMix.plan(new double[][]{{0, 0}, {3, 0}, {6, 1}, {9, 0}}, 5, 2, 4.0, 1.6, 18, new Random(seed));
            assertTrue(m.size() <= 18, "capped (seed " + seed + ")");
            for (int i = 0; i < m.size(); i++)
                for (int j = i + 1; j < m.size(); j++) {
                    if (i < 4 && j < 4) continue;     // the rings under the feet of foes who stand close together may overlap: they are the foes' own places
                    assertTrue(Math.hypot(m.get(i).x() - m.get(j).x(), m.get(i).z() - m.get(j).z()) >= 1.6 - 1e-9, "rings " + i + " and " + j + " do not stack");
                }
        }
    }

    @Test
    void whenTheCapBitesTheIllusionsGoAndTheRealRingsStay() {
        List<MarkMix.Mark> m = MarkMix.plan(new double[][]{{0, 0}, {10, 0}, {20, 0}, {30, 0}}, 3, 2, 4.0, 1.6, 12, new Random(3));
        assertEquals(12, m.size());
        assertEquals(12, MarkMix.countReal(m), "four foes with three real rings each already fill the cap: not one illusion is drawn");
        assertEquals(0, MarkMix.countFalse(m));
        assertTrue(m.get(0).real());
    }

    @Test
    void theSamePlanComesFromTheSameSeed() {
        List<MarkMix.Mark> a = MarkMix.plan(TWO, 3, 2, 4.0, 1.6, 18, new Random(42));
        List<MarkMix.Mark> b = MarkMix.plan(TWO, 3, 2, 4.0, 1.6, 18, new Random(42));
        assertEquals(a, b);
    }

    @Test
    void aLoneFoeAndNoFoesAreHandled() {
        List<MarkMix.Mark> one = MarkMix.plan(new double[][]{{5, 5}}, 1, 2, 4.0, 1.6, 18, new Random(1));
        assertEquals(1, MarkMix.countReal(one));
        assertEquals(2, MarkMix.countFalse(one));
        assertTrue(MarkMix.plan(new double[0][], 2, 2, 4.0, 1.6, 18, new Random(1)).isEmpty());
        assertEquals(1, MarkMix.plan(new double[][]{{0, 0}}, 0, 0, 4.0, 1.6, 0, new Random(1)).size(), "a cap of zero still keeps one real ring");
    }
}
