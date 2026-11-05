package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Nen rework (3.57.0) turns the six Hatsu into signature moves; every number and choice of them is pure, so the bounds a public server relies on are pinned without a world. */
class NenReworkRulesTest {
    @Test
    void koChargeGrowsWithWaitingAndIsBounded() {
        long until = 1000 + NenRules.duration(NenRules.KO);
        assertEquals(0.0, NenRules.koCharge(until, 1000), 1e-9);
        assertEquals(0.5, NenRules.koCharge(until, 1000 + NenRules.koFullTicks() / 2), 1e-9);
        assertEquals(1.0, NenRules.koCharge(until, 1000 + NenRules.koFullTicks()), 1e-9);
        assertEquals(0.0, NenRules.koCharge(until, until), "a closed window gathered nothing");
        assertEquals(0.0, NenRules.koCharge(0, 5));
        for (double c = -1; c <= 2; c += 0.25) {
            assertTrue(NenRules.koStrike(c) >= 22 && NenRules.koStrike(c) <= 44, "Ko strike stays in [22, 44]");
            assertTrue(NenRules.fistMultiplier(c) >= 1.0 && NenRules.fistMultiplier(c) <= 2.4 + 1e-9);
        }
    }

    @Test
    void theFistWearsArmourButNeverBreaksIt() {
        for (double c = 0; c <= 1; c += 0.25) {
            assertTrue(NenRules.shatterFraction(c, false) > 0 && NenRules.shatterFraction(c, true) <= 0.5, "a piece never loses more than half");
            assertTrue(NenRules.shatterFraction(c, true) >= NenRules.shatterFraction(c, false));
        }
        assertTrue(NenRules.fistReach() <= 8 && NenRules.fistLunge() <= 1.5);
    }

    @Test
    void theVolleyIsASmallBoundedBurst() {
        assertTrue(NenRules.volleyBullets(false) == 4 && NenRules.volleyBullets(true) == 6);
        assertTrue(NenRules.volleyBullets(true) <= 8 && NenRules.volleyGap() >= 2 && NenRules.bulletRange() <= 32);
        assertEquals(-1, NenRules.bulletPick(3, 0), "no foes, no pick");
        for (int n = 0; n < 10; n++) for (int f = 1; f <= 5; f++) assertTrue(NenRules.bulletPick(n, f) >= 0 && NenRules.bulletPick(n, f) < f);
        assertEquals(0, NenRules.bulletPick(0, 3));
        assertEquals(1, NenRules.bulletPick(4, 3), "bullets are dealt out in turn");
    }

    @Test
    void transmutationHasThreeNaturesChosenByStance() {
        assertEquals(NenRules.GUM, NenRules.transProperty(true, false));
        assertEquals(NenRules.GUM, NenRules.transProperty(true, true), "sneaking wins");
        assertEquals(NenRules.RUBBER, NenRules.transProperty(false, true));
        assertEquals(NenRules.ELECTRIC, NenRules.transProperty(false, false));
        assertTrue(NenRules.elecJumps(true) <= 6 && NenRules.gumMax(true) <= 8 && NenRules.rubberMax() <= 12, "every nature looks at a handful of creatures");
        assertTrue(NenRules.elecFalloff() < 1.0, "each jump hits weaker");
    }

    @Test
    void theSurgeEncodingRoundTripsAndNeverReadsAsZero() {
        for (int prop = 0; prop < 3; prop++) {
            for (double r = 0; r <= 9; r += 1.5) {
                float power = NenRules.surgePower(prop, r);
                assertTrue(power >= 1f, "the client reads a power of 0 as 1");
                assertEquals(prop, NenRules.surgeProperty(power), "nature survives the trip");
                assertEquals((float) Math.min(9.0, r), NenRules.surgeRadius(power), 0.05f, "radius survives the trip");
            }
        }
    }

    @Test
    void chainsNeedleAndFieldAreBounded() {
        assertTrue(NenRules.chains(true) <= NenRules.chainSlots() && NenRules.chains(false) < NenRules.chains(true));
        assertTrue(NenRules.chainTicks() <= 200 && NenRules.chainEvery() >= 2 && NenRules.chainHitEvery() >= NenRules.chainEvery());
        assertTrue(NenRules.needleTicks(true) <= 300 && NenRules.needleEvery() >= 5 && NenRules.needleMark(true) <= 1.5f);
        assertTrue(NenRules.fieldRadius(true) <= 16 && NenRules.fieldTicks(true) <= 400 && NenRules.fieldPulses(true) <= 20 && NenRules.fieldSteals() <= 3);
        assertEquals(NenRules.fieldTicks(true) / NenRules.fieldEvery(), NenRules.fieldPulses(true));
    }

    @Test
    void aStolenBuffIsNeverInfiniteNorStronger() {
        assertEquals(200, NenRules.stealTicks(0));
        assertEquals(200, NenRules.stealTicks(-1), "an endless buff is given a normal length");
        assertEquals(250, NenRules.stealTicks(100000));
        assertEquals(80, NenRules.stealTicks(80));
        assertEquals(0, NenRules.stealAmp(-4));
        assertEquals(2, NenRules.stealAmp(9));
    }

    @Test
    void theAuraColoursAreOpaqueAndDistinctPerType() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int t = 0; t < 6; t++) {
            int c = NenRules.typeColor(t);
            assertEquals(0xFF, c >>> 24, "opaque");
            assertTrue(seen.add(c), "each type has its own colour");
            assertEquals(c, NenRules.auraColor(0, t));
            assertEquals(NenRules.typeColor(t), NenRules.typeColor(t + 6), "the type wraps");
        }
        assertEquals(NenRules.auraColor(4, 0), NenRules.auraColor(4, 5), "Ko is always the fist's fire");
        assertEquals(8, NenRules.AURA_MODES.length);
        assertTrue(NenRules.MAX_AURAS_SENT <= 3);
    }

    @Test
    void theNewDefencesAndHealsAreModest() {
        assertTrue(NenRules.tenHeal() > 0 && NenRules.tenHeal() <= 2 && NenRules.tenHealEvery() >= 20);
        assertTrue(NenRules.zetsuAmbush() > 1.0 && NenRules.zetsuAmbush() <= 2.0);
        assertTrue(NenRules.renBurstMax() <= 24 && NenRules.renBurstRadius() <= 8);
        assertTrue(NenRules.kenReflectMax() <= 12 && NenRules.kenReflectGap() >= 5);
        assertTrue(NenRules.enDodgeMultiplier() < 1.0 && NenRules.enDodgeMultiplier() >= 0.25);
        assertTrue(NenRules.enDodgeReady(0, 1000) && !NenRules.enDodgeReady(1000, 1001), "the dodge has a gap");
    }
}
