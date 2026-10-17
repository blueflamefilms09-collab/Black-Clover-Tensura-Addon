package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Demon Mark ladder, the Hellblaze numbers and the Demon King's nova: monotone, honest and always possible to walk out of. */
class DemonKingMathTest {
    private static final int[] STEPS = {20, 40, 60, 80, 100};

    @Test
    void theMarkFollowsThePowerSteps() {
        assertEquals(0, DemonKingMath.mark(20));
        assertEquals(0, DemonKingMath.mark(39));
        assertEquals(1, DemonKingMath.mark(40));
        assertEquals(2, DemonKingMath.mark(60));
        assertEquals(3, DemonKingMath.mark(80));
        assertEquals(4, DemonKingMath.mark(100));
        assertEquals(4, DemonKingMath.mark(250));
        assertFalse(DemonKingMath.assault(79));
        assertTrue(DemonKingMath.assault(80) && DemonKingMath.assault(99));
        assertFalse(DemonKingMath.assault(100));
        assertTrue(DemonKingMath.king(100) && !DemonKingMath.king(99));
    }

    @Test
    void heGrowsStrongerAndNeverWeaker() {
        double d = 0, h = 0, r = 0;
        for (int p = 20; p <= 100; p += 5) {
            assertTrue(DemonKingMath.damageScale(p) >= d, "damage at " + p);
            assertTrue(DemonKingMath.fieldRadius(p) >= r, "Hellblaze radius at " + p);
            assertTrue(DemonKingMath.fieldTicks(p) >= h);
            d = DemonKingMath.damageScale(p);
            r = DemonKingMath.fieldRadius(p);
            h = DemonKingMath.fieldTicks(p);
        }
        assertEquals(1.00, DemonKingMath.damageScale(20), 1e-9);
        assertEquals(1.30, DemonKingMath.damageScale(100), 1e-9);
        assertEquals(1.22, DemonKingMath.damageScale(80), 1e-9);
        assertTrue(DemonKingMath.speedBonus(80) > DemonKingMath.speedBonus(60), "Assault Mode is the fastest he gets");
        assertTrue(DemonKingMath.speedBonus(100) > 0);
    }

    @Test
    void assaultModeGivesUpHisGuardAndHisFullCounter() {
        assertFalse(DemonKingMath.canCounter(80));
        assertFalse(DemonKingMath.canCounter(99));
        for (int p : new int[]{20, 40, 60, 100}) assertTrue(DemonKingMath.canCounter(p), "he counters at " + p);
        assertEquals(0, DemonKingMath.stanceTicks(80));
        assertEquals(0.0, DemonKingMath.breakFraction(80), 0.0);
        assertTrue(DemonKingMath.takenFactor(80) > 1.0, "the party's window: he takes more");
        assertEquals(1.0, DemonKingMath.takenFactor(60), 0.0);
        assertEquals(1.0, DemonKingMath.takenFactor(100), 0.0, "the Demon King has his guard back");
    }

    @Test
    void theStanceGrowsLongerAndHoldsMoreAsHeGrows() {
        assertEquals(50, DemonKingMath.stanceTicks(20));
        assertEquals(60, DemonKingMath.stanceTicks(40));
        assertEquals(70, DemonKingMath.stanceTicks(60));
        assertEquals(80, DemonKingMath.stanceTicks(100));
        assertTrue(DemonKingMath.breakFraction(40) > DemonKingMath.breakFraction(20));
        assertTrue(DemonKingMath.breakFraction(100) > DemonKingMath.breakFraction(60));
        assertTrue(DemonKingMath.stanceCooldown(100) < DemonKingMath.stanceCooldown(20), "the Demon King counters more often");
        for (int p : STEPS) if (DemonKingMath.canCounter(p)) assertTrue(DemonKingMath.stanceCooldown(p) > DemonKingMath.stanceTicks(p) * 3, "the window after a stance is longer than the stance: " + p);
    }

    @Test
    void theFrontConeIsALittleOverAHemisphere() {
        assertTrue(DemonKingMath.frontal(1.0));
        assertTrue(DemonKingMath.frontal(0.0));
        assertTrue(DemonKingMath.frontal(-0.17), "up to 100 degrees round");
        assertFalse(DemonKingMath.frontal(-0.2));
        assertFalse(DemonKingMath.frontal(-1.0), "behind him is behind him");
    }

    @Test
    void imagesAndDashesAppearWithTheirPower() {
        assertEquals(0, DemonKingMath.imageCount(40));
        assertEquals(3, DemonKingMath.imageCount(60));
        assertEquals(3, DemonKingMath.imageCount(80));
        assertEquals(4, DemonKingMath.imageCount(100));
        assertEquals(0, DemonKingMath.rushDashes(60));
        assertEquals(3, DemonKingMath.rushDashes(80));
        assertEquals(4, DemonKingMath.rushDashes(100));
    }

    @Test
    void theImagesStandEquallySpacedOnARing() {
        for (int n = 3; n <= 4; n++) {
            double[][] spots = DemonKingMath.imageSpots(n, 10, -4, 6.5, 0.7);
            assertEquals(n, spots.length);
            for (double[] p : spots) assertEquals(6.5, Math.hypot(p[0] - 10, p[1] + 4), 1e-9, "on the ring");
            for (int i = 0; i < n; i++) {
                double[] a = spots[i], b = spots[(i + 1) % n];
                assertEquals(2 * 6.5 * Math.sin(Math.PI / n), Math.hypot(a[0] - b[0], a[1] - b[1]), 1e-9, "equal gaps");
            }
        }
        assertEquals(0, DemonKingMath.imageSpots(0, 0, 0, 5, 0).length);
        assertEquals(8, DemonKingMath.imageSpots(99, 0, 0, 5, 0).length, "capped");
    }

    @Test
    void hellblazeIsBiggerAndLongerLastingWithTheMark() {
        assertEquals(3.4, DemonKingMath.fieldRadius(20), 1e-9);
        assertEquals(5.2, DemonKingMath.fieldRadius(100), 1e-9);
        assertEquals(80, DemonKingMath.fieldTicks(20));
        assertEquals(128, DemonKingMath.fieldTicks(100));
        assertTrue(DemonKingMath.burnDamage(100) > DemonKingMath.burnDamage(20));
        assertTrue(DemonKingMath.lingerTicks(100) > DemonKingMath.lingerTicks(20));
        assertTrue(DemonKingMath.lingerTicks(100) <= 100, "the burn does not follow anyone for ever");
    }

    @Test
    void theNovaGrowsWithTheMagicThrownIntoItButCanAlwaysBeWalkedOut() {
        assertEquals(7.0, DemonKingMath.novaRadius(0.0), 1e-9);
        assertEquals(11.0, DemonKingMath.novaRadius(0.30), 1e-9);
        assertEquals(11.0, DemonKingMath.novaRadius(5.0), 1e-9, "capped");
        assertEquals(9.0, DemonKingMath.novaRadius(0.15), 1e-9);
        assertEquals(7.0, DemonKingMath.novaRadius(-1.0), 1e-9);
        double last = 0;
        for (double a = 0; a <= 0.4; a += 0.01) {
            assertTrue(DemonKingMath.novaRadius(a) >= last);
            last = DemonKingMath.novaRadius(a);
            assertTrue(DemonKingMath.walkable(DemonKingMath.novaRadius(a), DemonKingMath.NOVA_LOCK_TICKS), "walkable at feed " + a);
        }
        assertFalse(DemonKingMath.walkable(DemonKingMath.NOVA_MAX_RADIUS, 20), "and the check really checks");
        assertEquals(26f, DemonKingMath.novaDamage(0.0), 1e-4f);
        assertEquals(40f, DemonKingMath.novaDamage(1.0), 1e-4f);
    }

    @Test
    void enoughPhysicalDamageBreaksTheChannel() {
        assertFalse(DemonKingMath.broken(0.119));
        assertTrue(DemonKingMath.broken(0.12));
        assertTrue(DemonKingMath.broken(0.5));
        assertTrue(DemonKingMath.NOVA_BREAK < 0.18, "within what a party deals in the 60-tick channel under the 6% a second budget");
    }
}
