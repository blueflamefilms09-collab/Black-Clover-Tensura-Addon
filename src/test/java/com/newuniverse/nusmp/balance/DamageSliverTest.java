package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageSliverTest {
    @Test
    void aWeaponScaleHitAddsNothingToABoss() {
        assertEquals(0.0, DamageSliver.of(400, 1_000_000, true), 0.0);
        assertEquals(0.0, DamageSliver.of(400, 1_000_000, false), 0.0, "even when the flag is missing, a weapon-scale hit gets no sliver");
        assertEquals(0.0, DamageSliver.of(350, 1_000_000, false), 0.0);
    }

    @Test
    void aBossNeverGetsASliver() {
        assertEquals(0.0, DamageSliver.of(10, 1_000_000, true), 0.0);
        assertEquals(0.0, DamageSliver.of(1, 5000, true), 0.0);
    }

    @Test
    void aSmallSpellOnASmallTargetStillScratches() {
        double s = DamageSliver.of(10, 100, false);
        assertEquals(1.0, s, 1e-9);
        assertTrue(DamageSliver.of(15, 100, false) > DamageSliver.of(5, 100, false));
    }

    @Test
    void theSliverIsCappedAtTwoPercentOfMaxHealth() {
        for (double raw = 0.5; raw <= 20; raw += 0.5)
            for (double hp : new double[]{20, 100, 299, 5000})
                assertTrue(DamageSliver.of(raw, hp, false) <= hp * DamageSliver.MAX_FRACTION + 1e-9, raw + " on " + hp);
    }

    @Test
    void nonsenseInputsGiveNothing() {
        assertEquals(0.0, DamageSliver.of(-5, 100, false), 0.0);
        assertEquals(0.0, DamageSliver.of(5, 0, false), 0.0);
    }
}
