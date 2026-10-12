package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The arithmetic of Vanica's moves: lances, drain, beasts, the spreading field, spots and geometry. */
class VanicaMathTest {
    static final int[] POWERS = {20, 30, 40, 50, 60, 70, 80, 90, 100};

    @Test
    void lancesMultiplyWithPowerAndAtTheGates() {
        int prev = 0;
        for (int p : POWERS) {
            PowerCurve.Tuning t = PowerCurve.tuning(p);
            int n = VanicaMath.lanceCount(t.castSlots(), t.gatesOpen());
            assertTrue(n >= prev && n >= 2 && n <= VanicaMath.MAX_LANCES, "lances at " + p + ": " + n);
            prev = n;
        }
        assertEquals(2, VanicaMath.lanceCount(PowerCurve.tuning(20).castSlots(), false));
        assertEquals(6, VanicaMath.lanceCount(PowerCurve.tuning(100).castSlots(), true), "the Gates add one more");
        assertEquals(VanicaMath.MAX_LANCES, VanicaMath.lanceCount(99, true), "capped");
    }

    @Test
    void theFanIsSymmetricAndCentred() {
        for (int n = 1; n <= VanicaMath.MAX_LANCES; n++) {
            double[] f = VanicaMath.lanceFan(n);
            assertEquals(n, f.length);
            double sum = 0;
            for (double a : f) sum += a;
            assertEquals(0, sum, 1e-9, "centred on the aim for " + n);
            for (int i = 1; i < n; i++) assertEquals(Math.toRadians(9), f[i] - f[i - 1], 1e-9);
        }
        assertEquals(0, VanicaMath.lanceFan(1)[0], 1e-12);
        assertEquals(1, VanicaMath.lanceFan(-4).length, "never empty");
    }

    @Test
    void drainReturnsALittleAndNeverMuch() {
        assertEquals(0.15, VanicaMath.drainShare(20), 1e-9);
        assertEquals(0.40, VanicaMath.drainShare(100), 1e-9);
        assertTrue(VanicaMath.drainShare(60) > VanicaMath.drainShare(40));
        assertEquals(3.0, VanicaMath.drainHeal(20, 900, 20, false), 1e-9);
        assertEquals(9.0, VanicaMath.drainHeal(500, 900, 100, false), 1e-9, "one hit heals at most 1% of her max health");
        assertEquals(18.0, VanicaMath.drainHeal(500, 900, 100, true), 1e-9, "twice that in a frenzy");
        assertEquals(0, VanicaMath.drainHeal(-5, 900, 50, false), 1e-9);
        assertTrue(VanicaMath.drainHeal(20, 900, 50, true) > VanicaMath.drainHeal(20, 900, 50, false));
    }

    @Test
    void theDrainLimiterCapsAWindow() {
        VanicaMath.Drain d = new VanicaMath.Drain();
        double got = 0;
        for (int i = 0; i < 20; i++) got += d.take(100 + i, 9, 900, false);          // twenty maximal hits inside one window
        assertEquals(900 * VanicaMath.DRAIN_WINDOW_CAP, got, 1e-9, "4% of her health per window");
        assertEquals(9, d.take(100 + VanicaMath.DRAIN_WINDOW, 9, 900, false), 1e-9, "a new window opens");
        d.reset();
        double frenzy = 0;
        for (int i = 0; i < 20; i++) frenzy += d.take(500, 18, 900, true);
        assertEquals(900 * VanicaMath.DRAIN_WINDOW_CAP * 2, frenzy, 1e-9, "double in a frenzy");
        assertEquals(0, new VanicaMath.Drain().take(0, 0, 900, false), 1e-9);
    }

    @Test
    void beastsAreMoreNumerousAndToughestAtTheGates() {
        int prevCap = 0, prevHp = 0;
        for (int p : POWERS) {
            int cap = VanicaMath.beastCap(p);
            double hp = VanicaMath.beastHealth(p, false);
            assertTrue(cap >= prevCap, "cap grows");
            assertTrue(hp >= prevHp, "health grows");
            assertTrue(VanicaMath.beastAttack(p) >= 4 && VanicaMath.beastSpeed(p, false) > 0.3 && VanicaMath.beastSpeed(p, false) < 0.5);
            assertTrue(VanicaMath.beastsPerCast(PowerCurve.tuning(p).castSlots(), p) <= cap && VanicaMath.beastsPerCast(PowerCurve.tuning(p).castSlots(), p) >= 1);
            prevCap = cap;
            prevHp = (int) hp;
        }
        assertEquals(2, VanicaMath.beastCap(20));
        assertEquals(8, VanicaMath.beastCap(100));
        assertTrue(VanicaMath.beastHealth(100, false) > VanicaMath.beastHealth(80, false) * 1.2, "a step up at the Gates");
        assertTrue(VanicaMath.beastAttack(100) > VanicaMath.beastAttack(80) * 1.1);
        assertEquals(162, VanicaMath.beastHealth(100, false), 1e-9, "120 health and a third more at the Gates");
        assertEquals(9.6, VanicaMath.beastAttack(100), 1e-9, "8 attack and a fifth more at the Gates");
        assertEquals(28, VanicaMath.beastHealth(20, false), 1e-9);
        assertEquals(4, VanicaMath.beastAttack(20), 1e-9);
        assertTrue(VanicaMath.beastHealth(100, true) < VanicaMath.beastHealth(100, false), "stampede beasts are glass");
        assertTrue(VanicaMath.beastSpeed(100, true) > VanicaMath.beastSpeed(100, false), "and fast");
        assertTrue(VanicaMath.beastLife(true) < VanicaMath.beastLife(false));
        assertTrue(VanicaMath.biteCooldown(100) < VanicaMath.biteCooldown(20) && VanicaMath.pounceCooldown(100) < VanicaMath.pounceCooldown(20));
        assertTrue(VanicaMath.bleedTicks(100) > VanicaMath.bleedTicks(20));
        for (int p : POWERS) assertTrue(VanicaMath.stampedeCount(p) + VanicaMath.beastCap(p) <= VanicaMath.HARD_BEAST_CAP, "the Crown stays under the hard cap at " + p);
        assertEquals(6, VanicaMath.stampedeCount(100));
        assertTrue(VanicaMath.stampedeCount(60) >= 4 && VanicaMath.stampedeCount(60) < VanicaMath.stampedeCount(100));
        assertEquals(6, VanicaMath.stampedeRoom(6, 8), "a full cap and a full stampede fit");
        assertEquals(2, VanicaMath.stampedeRoom(6, 12), "the hard cap trims it");
        assertEquals(0, VanicaMath.stampedeRoom(6, 14));
        assertEquals(0, VanicaMath.stampedeRoom(6, 30));
        assertEquals(6, VanicaMath.stampedeRoom(6, -3));
    }

    @Test
    void theRotSpreadsFasterAtTheGatesAndNeverPastItsRadius() {
        assertTrue(VanicaMath.spreadTicks(100) < VanicaMath.spreadTicks(20) / 1.5, "it spreads faster at 100%");
        assertTrue(VanicaMath.rotMaxRadius(100) > VanicaMath.rotMaxRadius(80) * 1.2);
        for (int p : POWERS) {
            double max = VanicaMath.rotMaxRadius(p);
            assertTrue(max >= 6.5 && max <= 16, "radius at " + p);
            int spread = VanicaMath.spreadTicks(p);
            double prev = 0;
            for (int age = 0; age <= spread + 20; age += 2) {
                double r = VanicaMath.rotRadius(age, max, spread);
                assertTrue(r >= prev - 1e-12 && r <= max + 1e-12, "monotone and bounded at age " + age);
                prev = r;
            }
            assertEquals(max, VanicaMath.rotRadius(spread, max, spread), 1e-9, "full size when the spread time is up");
            assertEquals(1.5, VanicaMath.rotRadius(0, max, spread), 1e-9);
        }
        assertTrue(VanicaMath.rotTicks(100) > VanicaMath.rotTicks(20));
    }

    @Test
    void bandsPartitionTheDiscWithoutOverlap() {
        double[] radii = {-1, 1.5, 3, 4.7, 6, 9.2, 12};
        Set<Long> seen = new HashSet<>();
        int total = 0;
        for (int i = 1; i < radii.length; i++) {
            List<int[]> band = VanicaMath.band(radii[i - 1], radii[i]);
            int lastD = -1;
            for (int[] o : band) {
                int d2 = o[0] * o[0] + o[1] * o[1];
                assertTrue(d2 >= lastD, "nearest first");
                lastD = d2;
                assertTrue(seen.add(((long) o[0] << 32) ^ (o[1] & 0xffffffffL)), "a column is visited once: " + o[0] + "," + o[1]);
                assertTrue(d2 <= radii[i] * radii[i] + 1e-9 && (radii[i - 1] < 0 || d2 > radii[i - 1] * radii[i - 1]));
            }
            total += band.size();
        }
        int expected = 0;
        for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) if (x * x + z * z <= 144) expected++;
        assertEquals(expected, total, "together the bands are the whole disc");
        assertTrue(seen.contains(0L), "the centre is in the first band");
        assertTrue(VanicaMath.band(5, 5).isEmpty());
        assertTrue(VanicaMath.band(0, 0).isEmpty());
    }

    @Test
    void theCurseGrowsWithPower() {
        assertEquals(1, VanicaMath.curseLevel(20));
        assertEquals(1, VanicaMath.curseLevel(59));
        assertEquals(2, VanicaMath.curseLevel(60));
        assertEquals(3, VanicaMath.curseLevel(100));
        for (int l = 1; l < 3; l++) {
            assertTrue(VanicaMath.healFactor(l + 1) < VanicaMath.healFactor(l), "healing is cut harder");
            assertTrue(VanicaMath.sigilRadius(l + 1) > VanicaMath.sigilRadius(l));
            assertTrue(VanicaMath.weaknessAmp(l + 1) >= VanicaMath.weaknessAmp(l));
        }
        assertEquals(0, VanicaMath.weaknessAmp(1));
        assertEquals(1, VanicaMath.weaknessAmp(3));
        assertTrue(VanicaMath.healFactor(3) > 0 && VanicaMath.healFactor(1) < 1);
        assertTrue(VanicaMath.curseTicks(100) > VanicaMath.curseTicks(20));
        assertTrue(VanicaMath.sigilRadius(0) == VanicaMath.sigilRadius(1) && VanicaMath.sigilRadius(9) == VanicaMath.sigilRadius(3), "levels are clamped");
    }

    @Test
    void theRainFallsOnTheTargetAndTheOthersAndNeverStacks() {
        double[][] others = {{10, 0}, {10.5, 0.5}, {-8, 6}};
        for (int count = 1; count <= 8; count++) {
            List<double[]> spots = VanicaMath.planSpots(count, 0, 0, others, 3.5, 2.6, 0.7);
            assertTrue(spots.size() <= count && spots.size() >= Math.min(count, 4), "spots for " + count + ": " + spots.size());
            assertArrayEquals(new double[]{0, 0}, spots.get(0), "the first falls on the target");
            for (int i = 0; i < spots.size(); i++)
                for (int j = i + 1; j < spots.size(); j++) {
                    double[] a = spots.get(i), b = spots.get(j);
                    assertTrue(Math.hypot(a[0] - b[0], a[1] - b[1]) >= 2.6 - 1e-9, "no two spots stacked");
                }
        }
        List<double[]> six = VanicaMath.planSpots(6, 0, 0, others, 3.5, 2.6, 0.7);
        boolean onFirstOther = false, onSecondOther = false;
        for (double[] s : six) {
            if (s[0] == 10 && s[1] == 0) onFirstOther = true;
            if (s[0] == 10.5 && s[1] == 0.5) onSecondOther = true;
        }
        assertTrue(onFirstOther, "a foe far away gets its own spot");
        assertFalse(onSecondOther, "a foe standing in the first one's spot does not stack another");
        assertTrue(VanicaMath.planSpots(0, 0, 0, null, 3, 2, 0).isEmpty());
        assertEquals(1, VanicaMath.planSpots(1, 5, 5, null, 3, 2, 0).size());
    }

    @Test
    void theRainAlwaysFindsRoomForItsSpotsInTheOpen() {
        for (int slots = 1; slots <= 4; slots++) {
            int count = 3 + slots;
            for (int k = 0; k < 24; k++) {
                List<double[]> spots = VanicaMath.planSpots(count, 12.5, -3.0, new double[0][], VanicaMath.RAIN_RING, VanicaMath.RAIN_SPACING, k * Math.PI / 12);
                assertEquals(count, spots.size(), "all " + count + " spots fit round a lone target (phase " + k + ")");
                for (int i = 1; i < spots.size(); i++)
                    assertTrue(Math.hypot(spots.get(i)[0] - 12.5, spots.get(i)[1] + 3.0) >= VanicaMath.RAIN_RING - 1e-9, "the scatter starts " + VanicaMath.RAIN_RING + " out");
            }
        }
        assertTrue(VanicaMath.RAIN_SPACING < 2 * VanicaMath.RAIN_RADIUS, "neighbouring spots may touch, never coincide");
        assertTrue(VanicaMath.RAIN_RING >= VanicaMath.RAIN_SPACING);
    }

    @Test
    void theCrownRingIsEvenAndGrowsWithPower() {
        for (int p : POWERS) {
            int n = VanicaMath.thornCount(p);
            assertTrue(n >= 8 && n <= 14, "thorns at " + p);
            List<double[]> ring = VanicaMath.thornRing(n, 3, -4, VanicaMath.crownRadius(p), 0.3);
            assertEquals(n, ring.size());
            double minGap = Double.MAX_VALUE, maxGap = 0;
            for (int i = 0; i < n; i++) {
                double[] a = ring.get(i), b = ring.get((i + 1) % n);
                double gap = Math.hypot(a[0] - b[0], a[1] - b[1]);
                minGap = Math.min(minGap, gap);
                maxGap = Math.max(maxGap, gap);
                assertEquals(VanicaMath.crownRadius(p), Math.hypot(a[0] - 3, a[1] + 4), 1e-9);
            }
            assertEquals(minGap, maxGap, 1e-9, "evenly spaced");
            assertTrue(minGap > 3.6, "wide enough between thorns (radius 1.9 each) to stand between them: " + minGap);
        }
        assertEquals(14, VanicaMath.thornCount(100));
        assertTrue(VanicaMath.thornCount(60) < VanicaMath.thornCount(100));
        assertTrue(VanicaMath.crownRadius(100) > VanicaMath.crownRadius(80));
    }

    @Test
    void geometryOfTheScytheTheWhipAndTheDash() {
        // scythe: a fan of 150 degrees, 5.2 blocks, facing +z
        double half = VanicaMath.SCYTHE_HALF_ANGLE, r = VanicaMath.SCYTHE_REACH;
        assertTrue(VanicaMath.inCone(0, 3, 0, 1, half, r, 0.3), "in front");
        assertTrue(VanicaMath.inCone(2.5, 2.5, 0, 1, half, r, 0.3), "front-right, 45 degrees");
        assertFalse(VanicaMath.inCone(0, -3, 0, 1, half, r, 0.3), "behind");
        assertFalse(VanicaMath.inCone(0, 6, 0, 1, half, r, 0.3), "too far");
        assertTrue(VanicaMath.inCone(0, 5.4, 0, 1, half, r, 0.3), "a body margin");
        assertTrue(VanicaMath.inCone(0, -1, 0, 1, half, r, 0.3), "close in counts all round");
        assertTrue(VanicaMath.inCone(1, 1, 0, 0, half, r, 0.3), "no facing: everything in reach");
        // segments
        assertEquals(1.0, VanicaMath.distToSegment(1, 5, 0, 0, 0, 10), 1e-9);
        assertEquals(5.0, VanicaMath.distToSegment(3, 14, 0, 0, 0, 10), 1e-9, "past the end it is the distance to the end");
        assertEquals(0.0, VanicaMath.distToSegment(0, 4, 0, 0, 0, 10), 1e-9);
        assertEquals(2.0, VanicaMath.distToSegment(2, 0, 0, 0, 0, 0), 1e-9, "a zero-length segment is a point");
        assertEquals(0.5, VanicaMath.alongSegment(4, 5, 0, 0, 0, 10), 1e-9, "only the position along the line counts");
        assertEquals(0.0, VanicaMath.alongSegment(0, -3, 0, 0, 0, 10), 1e-9);
        assertEquals(1.0, VanicaMath.alongSegment(0, 30, 0, 0, 0, 10), 1e-9);
        // the dash stops short of its target and never goes over its maximum
        assertEquals(3, VanicaMath.dashLength(2), 1e-9);
        assertEquals(8.4, VanicaMath.dashLength(10), 1e-9);
        assertEquals(VanicaMath.DASH_MAX, VanicaMath.dashLength(80), 1e-9);
        assertTrue(VanicaMath.dashLength(25) < 25);
    }

    /** Plays the yank with the game's own numbers (gravity 0.08, vertical drag 0.98, air drag 0.91, ground 0.546) and returns the distance travelled. */
    private static double yankTravel(double speed) {
        double x = 0, vx = speed, y = 0, vy = 0.25;
        boolean air = true;
        for (int tick = 0; tick < 200 && Math.abs(vx) > 1e-4; tick++) {
            x += vx;
            if (air) {
                y += vy;
                vy = (vy - 0.08) * 0.98;
                if (y <= 0) { y = 0; vy = 0; air = false; }
                vx *= 0.91;
            } else {
                vx *= 0.546;
            }
        }
        return x;
    }

    @Test
    void theYankBringsTheVictimToHerAndNotPastHer() {
        for (double d = 3; d <= 13; d += 0.5) {
            double need = d - VanicaMath.WHIP_STOP, v = VanicaMath.yankSpeed(d);
            assertTrue(v >= 0.3 && v <= 1.5, "bounded speed at " + d);
            double travelled = yankTravel(v);
            if (v < 1.5 && v > 0.3) assertEquals(need, travelled, 0.6, "comes to rest next to her from " + d + " blocks");
            if (v == 0.3) assertTrue(travelled >= need && travelled <= need + 1.5, "a short yank barely overshoots from " + d);
            if (v == 1.5) assertTrue(travelled <= need + 1.0, "the fastest yank still falls short of passing her from " + d);
        }
        assertTrue(VanicaMath.yankSpeed(5) < VanicaMath.yankSpeed(9), "farther victims are pulled faster");
        assertEquals(1.5, VanicaMath.yankSpeed(60), 1e-12);
        assertEquals(0.3, VanicaMath.yankSpeed(2.3), 1e-12);
    }

    @Test
    void fillIsZeroAtTheOpeningAndOneAtTheGates() {
        assertEquals(0, VanicaMath.fill(20), 1e-12);
        assertEquals(0, VanicaMath.fill(0), 1e-12);
        assertEquals(1, VanicaMath.fill(100), 1e-12);
        assertEquals(0.5, VanicaMath.fill(60), 1e-12);
        assertEquals(1, VanicaMath.fill(400), 1e-12);
    }
}
