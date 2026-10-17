package com.newuniverse.nusmp.avatar.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The weapon AI's decision table: which way an Avatar fights from what it holds, where a shooter stands, when the shield is up, how a bow is drawn. */
class CombatPlannerTest {
    private static CombatPlanner.Facts f(WeaponKind main, WeaponKind off, boolean ammo, boolean cast) { return new CombatPlanner.Facts(main, off, ammo, cast); }

    // ---------------------------------------------------------------- style
    @Test
    void aSwordOrFistsMeanMelee() {
        assertEquals(CombatStyle.MELEE, CombatPlanner.choose(f(WeaponKind.MELEE, WeaponKind.NONE, true, true)));
        assertEquals(CombatStyle.MELEE, CombatPlanner.choose(f(WeaponKind.NONE, WeaponKind.NONE, true, true)));
        assertEquals(CombatStyle.MELEE, CombatPlanner.choose(f(WeaponKind.TRIDENT, WeaponKind.SHIELD, false, false)));
    }

    @Test
    void aBowOrCrossbowWithAmmunitionMeansRanged() {
        assertEquals(CombatStyle.BOW, CombatPlanner.choose(f(WeaponKind.BOW, WeaponKind.NONE, true, false)));
        assertEquals(CombatStyle.CROSSBOW, CombatPlanner.choose(f(WeaponKind.CROSSBOW, WeaponKind.SHIELD, true, false)));
        assertEquals(CombatStyle.BOW, CombatPlanner.choose(f(WeaponKind.BOW, WeaponKind.FOCUS, true, true)), "a bow in the main hand beats a focus in the other");
    }

    @Test
    void aBowWithoutAmmunitionIsAClubUnlessThereIsAFocus() {
        assertEquals(CombatStyle.MELEE, CombatPlanner.choose(f(WeaponKind.BOW, WeaponKind.NONE, false, false)));
        assertEquals(CombatStyle.MAGE, CombatPlanner.choose(f(WeaponKind.BOW, WeaponKind.FOCUS, false, true)));
    }

    @Test
    void aFocusInEitherHandMakesAMageButOnlyIfItCanCast() {
        assertEquals(CombatStyle.MAGE, CombatPlanner.choose(f(WeaponKind.FOCUS, WeaponKind.NONE, false, true)));
        assertEquals(CombatStyle.MAGE, CombatPlanner.choose(f(WeaponKind.MELEE, WeaponKind.FOCUS, false, true)));
        assertEquals(CombatStyle.MELEE, CombatPlanner.choose(f(WeaponKind.FOCUS, WeaponKind.NONE, false, false)), "nothing to cast: it draws steel instead");
    }

    @Test
    void nullKindsReadAsNothing() {
        assertEquals(CombatStyle.MELEE, CombatPlanner.choose(new CombatPlanner.Facts(null, null, false, false)));
    }

    @Test
    void switchingTheHeldItemChangesTheStyle() {
        assertEquals(CombatStyle.MELEE, CombatPlanner.choose(f(WeaponKind.MELEE, WeaponKind.NONE, true, true)));
        assertEquals(CombatStyle.BOW, CombatPlanner.choose(f(WeaponKind.BOW, WeaponKind.NONE, true, true)));
        assertEquals(CombatStyle.MAGE, CombatPlanner.choose(f(WeaponKind.FOCUS, WeaponKind.NONE, true, true)));
    }

    // ---------------------------------------------------------------- distances
    @Test
    void meleeHasNoBandAndShootersDo() {
        CombatPlanner.Range m = CombatPlanner.range(CombatStyle.MELEE, false, false, 6, 18);
        assertEquals(0.0, m.max(), 1e-9);
        assertFalse(CombatPlanner.tooClose(1.0, m), "a melee fighter is never too close");
        CombatPlanner.Range b = CombatPlanner.range(CombatStyle.BOW, false, false, 6, 18);
        assertEquals(6.0, b.min(), 1e-9);
        assertEquals(18.0, b.max(), 1e-9);
        assertTrue(CombatPlanner.tooClose(3.0, b));
        assertFalse(CombatPlanner.tooClose(10.0, b));
        assertTrue(CombatPlanner.inRange(10.0, b));
        assertFalse(CombatPlanner.inRange(25.0, b));
    }

    @Test
    void aCowardStandsFartherAndAnAggressiveOneCloser() {
        CombatPlanner.Range normal = CombatPlanner.range(CombatStyle.BOW, false, false, 6, 18);
        CombatPlanner.Range coward = CombatPlanner.range(CombatStyle.BOW, true, false, 6, 18);
        CombatPlanner.Range bold = CombatPlanner.range(CombatStyle.BOW, false, true, 6, 18);
        assertTrue(coward.min() > normal.min() && coward.max() > normal.max());
        assertTrue(bold.min() < normal.min() && bold.max() < normal.max());
        assertTrue(bold.min() >= 2.0, "never closer than two blocks");
    }

    @Test
    void theBandIsAlwaysAWellFormedWindow() {
        for (CombatStyle s : CombatStyle.values())
            for (int c = 0; c < 4; c++)
                for (double min : new double[]{0, 2, 6, 12, 40})
                    for (double max : new double[]{0, 6, 18, 60}) {
                        CombatPlanner.Range r = CombatPlanner.range(s, (c & 1) != 0, (c & 2) != 0, min, max);
                        if (s == CombatStyle.MELEE) continue;
                        assertTrue(r.min() >= 2.0, s + " " + min + " " + max);
                        assertTrue(r.max() >= r.min() + 3 - 1e-9, s + " " + min + " " + max);
                        assertTrue(r.max() <= 28.0 + 1e-9, s + " " + min + " " + max);
                    }
    }

    @Test
    void strafingHasHysteresis() {
        double reach = 16;
        assertFalse(CombatPlanner.strafeBackwards(reach * reach * 0.9, reach, true), "far: stop backing away");
        assertTrue(CombatPlanner.strafeBackwards(reach * reach * 0.1, reach, false), "close: back away");
        assertTrue(CombatPlanner.strafeBackwards(reach * reach * 0.5, reach, true), "in between: keep doing what it did");
        assertFalse(CombatPlanner.strafeBackwards(reach * reach * 0.5, reach, false));
    }

    // ---------------------------------------------------------------- the shield
    @Test
    void theShieldIsUpOnlyWithAShieldAndAMeleeEnemy() {
        assertFalse(CombatPlanner.shieldUp(false, true, 0, 0));
        assertFalse(CombatPlanner.shieldUp(true, false, 0, 0));
        assertTrue(CombatPlanner.shieldUp(true, true, 0, 0));
    }

    @Test
    void theShieldCyclesSoTheAvatarCanStrike() {
        int up = 0;
        for (long t = 0; t < CombatPlanner.SHIELD_CYCLE * 10; t++) if (CombatPlanner.shieldUp(true, true, t, 7)) up++;
        assertEquals(CombatPlanner.SHIELD_UP * 10, up, "up for SHIELD_UP of every SHIELD_CYCLE ticks");
        assertFalse(CombatPlanner.shieldUp(true, true, CombatPlanner.SHIELD_UP, 0), "the window to strike comes");
        assertTrue(CombatPlanner.shieldUp(true, true, -1, 1), "a negative tick still has a defined phase");
    }

    // ---------------------------------------------------------------- the bow
    @Test
    void bowPowerFollowsVanillasCurve() {
        assertEquals(0.0f, CombatPlanner.bowPower(0), 1e-6f);
        assertEquals(1.0f, CombatPlanner.bowPower(20), 1e-6f);
        assertEquals(1.0f, CombatPlanner.bowPower(200), 1e-6f, "capped at full power");
        assertEquals((0.25f + 1.0f) / 3.0f, CombatPlanner.bowPower(10), 1e-6f);
        assertEquals(0.0f, CombatPlanner.bowPower(-5), 1e-6f);
        float last = -1;
        for (int t = 0; t <= 25; t++) { float p = CombatPlanner.bowPower(t); assertTrue(p >= last, "monotonic at " + t); last = p; }
    }

    @Test
    void aBetterDexterityShootsStraighter() {
        assertTrue(CombatPlanner.inaccuracy(18) < CombatPlanner.inaccuracy(10));
        assertTrue(CombatPlanner.inaccuracy(10) < CombatPlanner.inaccuracy(4));
        assertEquals(1.5f, CombatPlanner.inaccuracy(30), 1e-6f, "never perfect");
        assertEquals(14.0f, CombatPlanner.inaccuracy(1), 1e-6f, "never worse than an easy skeleton");
    }

    @Test
    void shotIntervalsAreShorterForAggressiveShootersAndNeverTiny() {
        assertTrue(CombatPlanner.shotInterval(CombatStyle.BOW, true) < CombatPlanner.shotInterval(CombatStyle.BOW, false));
        assertTrue(CombatPlanner.shotInterval(CombatStyle.CROSSBOW, false) < CombatPlanner.shotInterval(CombatStyle.BOW, false));
        assertTrue(CombatPlanner.shotInterval(CombatStyle.CROSSBOW, true) >= 8);
    }

    @Test
    void arrowsHitAsHardAsTheBodyThatShootsThem() {
        assertEquals(1.0, CombatPlanner.arrowDamageScale(4.0), 1e-9, "a plain attacker shoots vanilla arrows");
        assertEquals(1.0, CombatPlanner.arrowDamageScale(0.0), 1e-9, "never weaker than vanilla");
        assertEquals(2.5, CombatPlanner.arrowDamageScale(10.0), 1e-9);
        assertEquals(4.0, CombatPlanner.arrowDamageScale(400.0), 1e-9, "capped at four times");
        double last = 0;
        for (double atk = 0; atk <= 40; atk += 1) { double v = CombatPlanner.arrowDamageScale(atk); assertTrue(v >= last, "monotonic at " + atk); last = v; }
    }
}
