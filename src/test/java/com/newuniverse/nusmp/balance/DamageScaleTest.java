package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the damage scale-down (docs/damage_scale_down.md) is pure arithmetic in {@link DamageScale}: the Djinn band (the weakest of 36 at 4,000, the strongest at 8,000, falling with the rank), the
 * per-hit ceilings (10,000 for the mod, 15,000 for the apex weapon, never above the hard maximum whatever the settings say), the swing floor of a Djinn weapon and the old damage when the switch is off.
 */
class DamageScaleTest {
    private static final DamageScale.Limits L = DamageScale.Limits.defaults();

    @Test
    void theBandRunsFromTheWeakestToTheStrongestDjinnAndFallsWithTheRank() {
        assertEquals(8000.0, DamageScale.djinnHit(1, 36, 4000, 8000), 1e-9);
        assertEquals(4000.0, DamageScale.djinnHit(36, 36, 4000, 8000), 1e-9);
        double prev = Double.MAX_VALUE;
        for (int r = 1; r <= 36; r++) {
            double h = DamageScale.djinnHit(r, 36, 4000, 8000);
            assertTrue(h >= 4000 && h <= 8000, "rank " + r + " = " + h);
            assertTrue(h < prev, "rank " + r + " must be below the rank before it");
            prev = h;
        }
    }

    @Test
    void badInputsStayInTheBand() {
        assertEquals(8000.0, DamageScale.djinnHit(0, 36, 4000, 8000), 1e-9);
        assertEquals(4000.0, DamageScale.djinnHit(99, 36, 4000, 8000), 1e-9);
        assertEquals(8000.0, DamageScale.djinnHit(1, 1, 4000, 8000), 1e-9);
        assertEquals(4000.0, DamageScale.djinnHit(36, 36, 4000, 3000), 1e-9, "a band with its max under its min is flat at the min");
        assertEquals(DamageScale.DEFAULT_DJINN_MIN, DamageScale.djinnHit(36, 36, Double.NaN, 8000), 1e-9);
        assertEquals(DamageScale.ABSOLUTE_MAX, DamageScale.djinnHit(1, 36, 4000, 1e9), 1e-9, "the band max is clamped to the hard maximum");
    }

    @Test
    void everyCeilingHoldsAndNothingPassesTheHardMaximum() {
        int n = 36;
        for (DamageScale.Kind k : DamageScale.Kind.values())
            for (int r : new int[]{1, 18, 36}) {
                double c = DamageScale.ceiling(L, k, r, n);
                assertTrue(c <= DamageScale.ABSOLUTE_MAX, k + " " + r);
                if (k != DamageScale.Kind.APEX) assertTrue(c <= 10_000.0, k + " " + r + " = " + c);
                assertTrue(DamageScale.apply(L, k, r, n, 1e12, 1.0, false) <= c + 1e-9, k + " " + r + " is lowered to its ceiling");
            }
        assertEquals(10_000.0, DamageScale.ceiling(L, DamageScale.Kind.GENERAL, 0, 0), 1e-9);
        assertEquals(15_000.0, DamageScale.ceiling(L, DamageScale.Kind.APEX, 0, 0), 1e-9);
        assertEquals(8_000.0, DamageScale.ceiling(L, DamageScale.Kind.EXTREME, 1, n), 1e-9);
        assertEquals(4_000.0, DamageScale.ceiling(L, DamageScale.Kind.EXTREME, 36, n), 1e-9);
    }

    @Test
    void aRaisedSettingNeverPassesFifteenThousand() {
        DamageScale.Limits wild = new DamageScale.Limits(true, 1e9, 1e9, 1e9, 1e9, 1e9, true);
        for (DamageScale.Kind k : DamageScale.Kind.values()) assertTrue(DamageScale.apply(wild, k, 1, 36, 1e12, 1.0, false) <= DamageScale.ABSOLUTE_MAX, k.name());
        assertEquals(15_000.0, DamageScale.softCeiling(wild), 1e-9);
        assertEquals(15_000.0, DamageScale.softCeiling(L), 1e-9);
    }

    @Test
    void aDjinnWeaponSwingLandsInTheBandAndNotOnPlayers() {
        // a weak natural hit is raised to the floor, a huge one lowered to the rank's number
        assertEquals(4000.0, DamageScale.apply(L, DamageScale.Kind.DJINN_WEAPON, 36, 36, 300, 1.0, false), 1e-9);
        assertEquals(8000.0, DamageScale.apply(L, DamageScale.Kind.DJINN_WEAPON, 1, 36, 90_000, 1.0, false), 1e-9);
        double mid = DamageScale.apply(L, DamageScale.Kind.DJINN_WEAPON, 18, 36, 500, 1.0, false);
        assertTrue(mid >= 4000 && mid <= 8000, "" + mid);
        // against a player the floor does not apply (the PvP hit cap decides), the ceiling does
        assertEquals(300.0, DamageScale.apply(L, DamageScale.Kind.DJINN_WEAPON, 36, 36, 300, 1.0, true), 1e-9);
        assertEquals(8000.0, DamageScale.apply(L, DamageScale.Kind.DJINN_WEAPON, 1, 36, 90_000, 1.0, true), 1e-9);
        // a half-charged swing is scaled like the vanilla swing
        assertEquals(DamageScale.charge(0.5) * 4000.0, DamageScale.apply(L, DamageScale.Kind.DJINN_WEAPON, 36, 36, 10, DamageScale.charge(0.5), false), 1e-9);
        // the bearer's other blows are only lowered
        assertEquals(10.0, DamageScale.apply(L, DamageScale.Kind.DJINN_BEARER, 1, 36, 10, 1.0, false), 1e-9);
        assertEquals(8000.0, DamageScale.apply(L, DamageScale.Kind.DJINN_BEARER, 1, 36, 50_000, 1.0, false), 1e-9);
    }

    @Test
    void theFloorCanBeSwitchedOff() {
        DamageScale.Limits noFloor = new DamageScale.Limits(true, 10_000, 10_000, 4000, 8000, 15_000, false);
        assertEquals(300.0, DamageScale.apply(noFloor, DamageScale.Kind.DJINN_WEAPON, 36, 36, 300, 1.0, false), 1e-9);
    }

    @Test
    void offIsTheOldDamageExactly() {
        DamageScale.Limits off = new DamageScale.Limits(false, 10_000, 10_000, 4000, 8000, 15_000, true);
        for (DamageScale.Kind k : DamageScale.Kind.values()) assertEquals(123_456.0, DamageScale.apply(off, k, 1, 36, 123_456.0, 1.0, false), 1e-9, k.name());
        assertEquals(Double.MAX_VALUE, DamageScale.softCeiling(off));
        assertEquals(5.0, DamageScale.apply(null, DamageScale.Kind.GENERAL, 1, 36, 5, 1.0, false), 1e-9);
    }

    @Test
    void badAmountsGiveNothing() {
        assertEquals(0.0, DamageScale.apply(L, DamageScale.Kind.GENERAL, 0, 0, Double.NaN, 1.0, false));
        assertEquals(0.0, DamageScale.apply(L, DamageScale.Kind.GENERAL, 0, 0, -5, 1.0, false));
        assertEquals(10_000.0, DamageScale.apply(L, DamageScale.Kind.GENERAL, 0, 0, Double.POSITIVE_INFINITY, 1.0, false), 1e-9);
    }

    @Test
    void theExtremeScopeIsSetAndCleared() {
        DamageScale.beginExtreme(5, 36);
        assertTrue(DamageScale.extremeActive());
        assertEquals(5, DamageScale.extremeRank());
        DamageScale.endExtreme();
        assertFalse(DamageScale.extremeActive());
    }

    @Test
    void theSwingFactorIsTheVanillaOne() {
        assertEquals(1.0, DamageScale.charge(1.0), 1e-9);
        assertEquals(0.2, DamageScale.charge(0.0), 1e-9);
        assertEquals(1.0, DamageScale.charge(7.0), 1e-9);
        assertEquals(1.0, DamageScale.charge(Double.NaN), 1e-9);
    }
}
