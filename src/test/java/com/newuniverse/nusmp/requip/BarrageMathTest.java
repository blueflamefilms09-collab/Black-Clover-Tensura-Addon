package com.newuniverse.nusmp.requip;

import com.newuniverse.nusmp.relics.GradeDamage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the sword rain must stay between 8 and 24 swords, stand in a ring round the caster, leave one after another inside two seconds, hit by the weapon's grade, and never take more than its share of a boss. */
class BarrageMathTest {
    @Test
    void theSwordCountIsEightToTwentyFour() {
        assertEquals(8, BarrageMath.swordCount(0.0, 25_000));
        assertEquals(10, BarrageMath.swordCount(0.0, 50_000));
        assertEquals(18, BarrageMath.swordCount(1.0, 25_000));
        assertEquals(24, BarrageMath.swordCount(1.0, 1_000_000));
        for (double m = 0; m <= 1.0001; m += 0.05)
            for (double ep : new double[]{0, 1_000, 50_000, 200_000, 1_000_000, 1e9, Double.NaN}) {
                int n = BarrageMath.swordCount(m, ep);
                assertTrue(n >= BarrageMath.MIN_SWORDS && n <= BarrageMath.MAX_SWORDS, "count " + n);
            }
        // more mastery or more EP never gives fewer swords
        int last = 0;
        for (double m = 0; m <= 1.0; m += 0.1) { int n = BarrageMath.swordCount(m, 300_000); assertTrue(n >= last); last = n; }
    }

    @Test
    void theRingStandsAtTheRadiusAndAroundTheCaster() {
        for (int count : new int[]{8, 12, 13, 18, 24}) {
            double[][] ring = BarrageMath.ring(count, 3.0, 0.3);
            assertEquals(count, ring.length);
            double cx = 0, cz = 0;
            for (int i = 0; i < count; i++) {
                double r = Math.hypot(ring[i][0], ring[i][2]);
                assertTrue(r <= 3.0 + 1e-9 && r >= 3.0 * 0.62 - 1e-9, "radius " + r);
                assertTrue(ring[i][1] > 0.8 && ring[i][1] < 2.4, "height " + ring[i][1]);
                cx += ring[i][0]; cz += ring[i][2];
            }
            assertEquals(0.0, cx / count, 0.45, "the ring is centred on the caster (count " + count + ")");
            assertEquals(0.0, cz / count, 0.45);
        }
        // up to twelve swords stand on one ring; above that on two
        double[][] one = BarrageMath.ring(12, 3.0, 0);
        for (double[] p : one) assertEquals(3.0, Math.hypot(p[0], p[2]), 1e-9);
        double[][] two = BarrageMath.ring(24, 3.0, 0);
        int inner = 0;
        for (double[] p : two) if (Math.hypot(p[0], p[2]) < 2.5) inner++;
        assertEquals(12, inner);
        // no two swords on the same ring share a place
        for (int i = 0; i < two.length; i++)
            for (int j = i + 1; j < two.length; j++)
                assertTrue(Math.hypot(two[i][0] - two[j][0], two[i][2] - two[j][2]) + Math.abs(two[i][1] - two[j][1]) > 0.2, "swords " + i + " and " + j + " overlap");
        // the phase turns the whole ring
        assertEquals(Math.cos(1.0) * 3.0, BarrageMath.ring(8, 3.0, 1.0)[0][0], 1e-9);
    }

    @Test
    void theVolleyLeavesOneSwordAtATimeAndIsOverInsideAboutThreeSeconds() {
        for (int count = BarrageMath.MIN_SWORDS; count <= BarrageMath.MAX_SWORDS; count++) {
            int prev = -1;
            for (int i = 0; i < count; i++) {
                int t = BarrageMath.launchTick(i, count);
                assertTrue(t > prev, "swords leave in order");
                assertTrue(t >= BarrageMath.HOVER_TICKS, "they hover first");
                prev = t;
            }
            assertTrue(BarrageMath.lastLandingTick(count) <= 70, "the volley of " + count + " ends at tick " + BarrageMath.lastLandingTick(count));
        }
        assertEquals(2, BarrageMath.stagger(12));
        assertEquals(1, BarrageMath.stagger(13));
        assertEquals(BarrageMath.MIN_FLIGHT, BarrageMath.flightTicks(0.0));
        assertEquals(BarrageMath.MAX_FLIGHT, BarrageMath.flightTicks(500.0));
        assertEquals(10, BarrageMath.flightTicks(22.0));
    }

    @Test
    void theAimSpreadStaysInsideItsRadiusAndDoesNotRepeat() {
        for (int count : new int[]{8, 24}) {
            for (int i = 0; i < count; i++) {
                double[] a = BarrageMath.aimOffset(i, count, 2.0);
                assertTrue(Math.hypot(a[0], a[1]) <= 2.0 + 1e-9);
                for (int j = i + 1; j < count; j++) {
                    double[] b = BarrageMath.aimOffset(j, count, 2.0);
                    assertTrue(Math.hypot(a[0] - b[0], a[1] - b[1]) > 0.05);
                }
            }
        }
        assertEquals(0.0, BarrageMath.aimOffset(0, 1, 2.0)[0], 0);
    }

    @Test
    void theGradeOfAWeaponComesFromTheGradeTable() {
        assertEquals(-1, BarrageMath.gradeOf(7.0));                       // a vanilla diamond sword
        assertEquals(0, BarrageMath.gradeOf(20.0));
        assertEquals(0, BarrageMath.gradeOf(31.0));                       // between two ranges: the lower grade
        assertEquals(1, BarrageMath.gradeOf(35.0));
        assertEquals(3, BarrageMath.gradeOf(100.0));
        assertEquals(6, BarrageMath.gradeOf(350.0));
        assertEquals(6, BarrageMath.gradeOf(999.0));
        for (int g = 0; g < 7; g++) {
            assertEquals(g, BarrageMath.gradeOf(GradeDamage.range(g)[0]));
            assertEquals(g, BarrageMath.gradeOf(GradeDamage.range(g)[1]));
        }
    }

    @Test
    void damageGrowsWithTheGradeAndStaysInAFairBand() {
        double last = 0;
        for (int g = -1; g < 7; g++) {
            double dmg = g < 0 ? 7.0 : GradeDamage.range(g)[0];
            double raw = BarrageMath.perSwordRaw(dmg, 12, 0.0);
            assertTrue(raw > last, "grade " + g + " hits harder than the grade below");
            assertTrue(raw >= 3.0 && raw <= 14.0 * 1.15 * 1.4, "per sword " + raw);
            last = raw;
        }
        // a weapon high in its grade hits a little harder than one low in it
        assertTrue(BarrageMath.perSwordRaw(115, 12, 0) > BarrageMath.perSwordRaw(90, 12, 0));
        // Weapon Affinity: nothing below 20 percent, then up to +40 percent
        assertEquals(1.0, BarrageMath.affinity(0.19), 0);
        assertEquals(1.4, BarrageMath.affinity(1.0), 1e-9);
        assertTrue(BarrageMath.perSwordRaw(100, 12, 1.0) > BarrageMath.perSwordRaw(100, 12, 0.0));
        // a big volley shares its power: 24 swords are not twice 12 swords
        double total12 = 12 * BarrageMath.perSwordRaw(100, 12, 0.5), total24 = 24 * BarrageMath.perSwordRaw(100, 24, 0.5);
        assertTrue(total24 > total12 && total24 < total12 * 1.6, total12 + " vs " + total24);
        // the worst case (Genesis weapon, full mastery, 24 swords) is bounded
        assertTrue(24 * BarrageMath.perSwordRaw(350, 24, 1.0) < 24 * 14 * 1.15 * 1.4);
    }

    @Test
    void theStrikeAndTheCutHitHarderThanOneSword() {
        assertTrue(BarrageMath.strikeRaw(100, 0.5) > BarrageMath.perSwordRaw(100, 8, 0.5) * 2.0);
        assertTrue(BarrageMath.cutRaw(100, 0.5) > BarrageMath.strikeRaw(100, 0.5));
    }

    @Test
    void aSwordPiercesOneToThree() {
        assertEquals(1, BarrageMath.pierce(0.0));
        assertEquals(2, BarrageMath.pierce(0.5));
        assertEquals(3, BarrageMath.pierce(0.9));
        assertEquals(3, BarrageMath.pierce(5.0));
        assertEquals(1, BarrageMath.pierce(Double.NaN));
    }

    @Test
    void aVolleyTakesAtMostATenthOfABossAndNothingIsCappedElsewhere() {
        assertEquals(500.0, BarrageMath.volleyCap(5000.0, true), 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, BarrageMath.volleyCap(5000.0, false), 0);
        assertTrue(BarrageMath.volleyCap(0.0, true) >= 1.0);
        double cap = BarrageMath.volleyCap(1000.0, true), dealt = 0;
        for (int i = 0; i < 24; i++) {
            double hit = BarrageMath.allowed(40.0, dealt, cap);
            dealt += hit;
            assertTrue(dealt <= cap + 1e-9);
        }
        assertEquals(cap, dealt, 1e-9);                                  // 24 hits of 40 = 960 would be asked; only the cap lands
        assertEquals(0.0, BarrageMath.allowed(40.0, cap + 5, cap), 0);
        assertEquals(40.0, BarrageMath.allowed(40.0, 0.0, Double.POSITIVE_INFINITY), 0);
    }

    @Test
    void theDimensionCutBypassesThirtyPercentOfArmourAndNeverMoreThanTheCap() {
        assertEquals(0.30, BarrageMath.cutBypass(0.0), 1e-9);
        assertTrue(BarrageMath.cutBypass(1.0) <= BarrageMath.CUT_BYPASS_CAP + 1e-9);
        assertEquals(0.40, BarrageMath.CUT_BYPASS_CAP, 1e-9);
        for (double m = 0; m <= 3.0; m += 0.1) assertTrue(BarrageMath.cutBypass(m) >= 0.30 && BarrageMath.cutBypass(m) <= 0.40 + 1e-9);
    }

    @Test
    void theShapesOfTheFinishers() {
        double[][] c = BarrageMath.circle(12, 5.0, 0.0);
        assertEquals(12, c.length);
        for (double[] p : c) assertEquals(5.0, Math.hypot(p[0], p[1]), 1e-9);
        for (int i = 0; i <= 40; i++) {
            double[] r = BarrageMath.rose(5, i / 40.0, 8.0);
            assertTrue(Math.hypot(r[0], r[1]) <= 8.0 + 1e-9);
        }
        // the petals reach the full radius and come back to the centre
        double max = 0, min = 99;
        for (int i = 0; i < 400; i++) { double[] r = BarrageMath.rose(5, i / 400.0, 8.0); double d = Math.hypot(r[0], r[1]); max = Math.max(max, d); min = Math.min(min, d); }
        assertTrue(max > 7.9 && min < 0.2);
    }
}
