package com.newuniverse.nusmp.labyrinth.mega.macro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the angular sectors decide which tile covers a block, so the arithmetic that cuts a ring into sectors must be exact where it matters: sector 0 is CENTRED
 * on the angle it is given (so a door in the middle of a sector is never cut in half), the numbering wraps seamlessly, every angle falls in exactly one sector,
 * and the fraction inside a sector runs from one edge to the other. {@link MacroParams} is covered here too, since a wrong clamp there would reach every model.
 */
public class GeoTest {
    @Test
    public void sectorZeroIsCentredOnTheGivenAngleAndNumberingWraps() {
        for (int n : new int[] {8, 19, 32, 42}) {
            for (double centre : new double[] {0.0, Math.PI, -2.0, 1.0}) {
                assertEquals(0, Geo.sector(centre, centre, n));
                double w = Geo.TWO_PI / n;
                assertEquals(0, Geo.sector(centre + 0.49 * w, centre, n));
                assertEquals(0, Geo.sector(centre - 0.49 * w, centre, n));
                assertEquals(1, Geo.sector(centre + 0.51 * w, centre, n));
                assertEquals(n - 1, Geo.sector(centre - 0.51 * w, centre, n));
                assertEquals(0.5, Geo.sectorFraction(centre, centre, n), 1e-9, "the centre of the sector is the middle of its fraction");
            }
        }
    }

    @Test
    public void everyAngleFallsInExactlyOneSectorAndTheFractionStaysInside() {
        int n = 31;
        int[] count = new int[n];
        for (int i = 0; i < 31000; i++) {
            double a = -Math.PI + (i + 0.5) * Geo.TWO_PI / 31000.0;
            int s = Geo.sector(a, 0.7, n);
            assertTrue(s >= 0 && s < n);
            count[s]++;
            double f = Geo.sectorFraction(a, 0.7, n);
            assertTrue(f >= 0.0 && f < 1.0, "fraction " + f);
        }
        for (int s = 0; s < n; s++) assertTrue(Math.abs(count[s] - 1000) <= 2, "equal sectors: " + s + " has " + count[s]);
    }

    @Test
    public void theMathHelpersAreWhatTheModelsExpect() {
        assertEquals(0.0, Geo.smoothstep(0, 1, -5));
        assertEquals(1.0, Geo.smoothstep(0, 1, 7));
        assertEquals(0.5, Geo.smoothstep(0, 1, 0.5), 1e-12);
        assertEquals(0.0, Geo.smoothstep(46, 30, 50), "a reversed range still clamps");
        assertEquals(-1, Geo.floorToInt(-0.5));
        assertEquals(2, Geo.floorToInt(2.9));
        assertEquals(-Math.PI, Geo.wrapPi(Math.PI), 1e-12);
        assertEquals(0.5, Geo.wrapPi(0.5 + 4 * Math.PI), 1e-9);
        assertEquals(Math.PI / 2, Geo.angle(0, 1), 1e-12, "south is +z");
    }

    @Test
    public void theParametersAreClampedIntoTheirRanges() {
        MacroParams wild = new MacroParams(99, -3.0, 99.0, 50.0, 0, true);
        assertEquals(8, wild.cell());
        assertEquals(0.0, wild.spireNoise());
        assertEquals(6.0, wild.chasmNoise());
        assertEquals(12.0, wild.hillAmplitude());
        assertEquals(1, wild.wfcTries());
        assertEquals(4, new MacroParams(1, 2, 3, 5, 12, true).cell());
        assertEquals(MacroParams.DEFAULT, new MacroParams(6, 2.0, 3.0, 5.0, 12, true));
    }

    @Test
    public void everySupportedCellSizeBuildsAWorkingHub() {
        // the config allows 4 to 8; the landmarks must still be walkable at every size, with the landings aligned to the cave floors
        for (int cell = 4; cell <= 8; cell++) {
            HubModel m = HubModel.fresh(11L + cell, new MacroParams(cell, 2.0, 3.0, 5.0, 12, true));
            assertTrue(!m.spire().gallery(0).wfc().fallbackUsed && !m.chasm().wfc().fallbackUsed, "no fallback at cell size " + cell);
            assertTrue(WalkabilityTest.toHolyGate(m::fillColumn).reached(), "the Holy gate is reachable at cell size " + cell);
            assertTrue(WalkabilityTest.toAbyssGate(m::fillColumn).reached(), "the Abyssal gate is reachable at cell size " + cell);
        }
    }
}
