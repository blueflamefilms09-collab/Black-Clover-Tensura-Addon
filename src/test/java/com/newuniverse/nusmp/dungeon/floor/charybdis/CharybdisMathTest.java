package com.newuniverse.nusmp.dungeon.floor.charybdis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The geometry of the moves: the fan, the whirlpool's push, the arcs and circles, the megalodons' spots, the rise. */
class CharybdisMathTest {
    @Test
    void aFanIsCentredAndSymmetricAndWideEnoughToBeADodge() {
        for (int n = 1; n <= 9; n++) {
            double[] f = CharybdisMath.fan(n);
            assertEquals(n, f.length);
            double sum = 0;
            for (double v : f) sum += v;
            assertEquals(0.0, sum, 1e-9, "centred on the aim at " + n);
            for (int i = 1; i < n; i++) assertEquals(CharybdisMath.FAN_STEP, f[i] - f[i - 1], 1e-9);
        }
        assertEquals(0.0, CharybdisMath.fan(5)[2], 1e-12, "an odd fan has a scale dead on the aim");
        double across = CharybdisMath.fan(9)[8] - CharybdisMath.fan(9)[0];
        assertTrue(Math.toDegrees(across) > 50 && Math.toDegrees(across) < 90, "nine scales cover about 68 degrees: " + Math.toDegrees(across));
        assertEquals(1, CharybdisMath.fan(0).length, "never an empty fan");
        assertEquals(0.0, CharybdisMath.fanPitch(2, 5), 1e-12, "the middle scale flies level");
        assertEquals(0.0, CharybdisMath.fanPitch(0, 3), 1e-12, "small fans stay flat");
        assertNotEquals(CharybdisMath.fanPitch(0, 7), CharybdisMath.fanPitch(1, 7), "neighbours alternate up and down");
    }

    @Test
    void rotateTurnsAboutTheVerticalAxis() {
        double[] r = CharybdisMath.rotate(1, 0, Math.PI / 2);
        assertEquals(0.0, r[0], 1e-9);
        assertEquals(1.0, r[1], 1e-9);
        double[] same = CharybdisMath.rotate(3, 4, 0);
        assertEquals(3.0, same[0], 1e-9);
        assertEquals(4.0, same[1], 1e-9);
    }

    /** The average drift a whirlpool makes, with water's drag of 0.8 a tick and a push every second tick. */
    private static double drift(double push) {
        double v = 0, travelled = 0;
        for (int t = 0; t < 400; t++) {
            if (t % CharybdisMath.WHIRL_EVERY == 0) v += push;
            v *= 0.8;
            travelled += v;
        }
        return travelled / 400;
    }

    @Test
    void theWhirlpoolPullsInwardTurnsAndIsGentleEnoughToSwimOutOf() {
        double[] rim = CharybdisMath.whirlpool(5.5, 0, CharybdisMath.WHIRL_RADIUS);
        assertTrue(rim[0] < 0, "pulled towards the middle (foe on +x, so -x)");
        assertTrue(rim[1] > 0, "and round anticlockwise (+z when on +x)");
        double[] west = CharybdisMath.whirlpool(-5.5, 0, CharybdisMath.WHIRL_RADIUS);
        assertTrue(west[0] > 0 && west[1] < 0, "the mirror image on the other side");
        double[] mid = CharybdisMath.whirlpool(0.5, 0, CharybdisMath.WHIRL_RADIUS);
        assertTrue(Math.hypot(mid[0], mid[1]) < Math.hypot(rim[0], rim[1]) * 0.5, "weak at the middle, so nobody is shaken about there");
        assertArrayEquals(new double[]{0, 0}, CharybdisMath.whirlpool(6.5, 0, CharybdisMath.WHIRL_RADIUS), "nothing outside the ring");
        assertArrayEquals(new double[]{0, 0}, CharybdisMath.whirlpool(0, 0, CharybdisMath.WHIRL_RADIUS), "nothing exactly at the centre");
        double inward = Math.abs(CharybdisMath.whirlpool(CharybdisMath.WHIRL_RADIUS, 0, CharybdisMath.WHIRL_RADIUS)[0]);
        double swimSpeed = 0.11;                                                        // a plain swim, blocks a tick
        double slowedSwim = swimSpeed * (1 - 0.15);                                     // Slowness I
        assertTrue(drift(inward) < slowedSwim * 0.5, "even slowed, a swimmer easily outswims the pull: " + drift(inward) + " vs " + slowedSwim);
        double total = Math.hypot(rim[0], rim[1]);
        assertTrue(drift(total) < swimSpeed * (1 - 0.3) * 0.7, "at the Gates (Slowness II) pull and swirl together are still under a slowed swim: " + drift(total));
    }

    @Test
    void theWhirlpoolIsAtTheRimItsStrongest() {
        double prev = 0;
        for (double d = 1; d <= CharybdisMath.WHIRL_RADIUS; d += 0.5) {
            double[] p = CharybdisMath.whirlpool(d, 0, CharybdisMath.WHIRL_RADIUS);
            double m = Math.hypot(p[0], p[1]);
            assertTrue(m >= prev - 1e-12, "never weaker further out");
            prev = m;
        }
        assertTrue(CharybdisMath.MAELSTROM_RADIUS > CharybdisMath.WHIRL_RADIUS && CharybdisMath.MAELSTROM_TICKS > CharybdisMath.WHIRL_TICKS);
        assertEquals(60, CharybdisMath.SLOW_TICKS);
    }

    @Test
    void theSweepIsAWideArcInFrontOfTheBoss() {
        double r = CharybdisMath.SWEEP_REACH, half = CharybdisMath.SWEEP_HALF_ARC;
        assertTrue(CharybdisMath.inArc(0, 0, 1, 0, 3, 0, r, half), "dead ahead");
        assertTrue(CharybdisMath.inArc(0, 0, 1, 0, 0, 3, r, half), "to the side: the arc is 200 degrees");
        assertTrue(CharybdisMath.inArc(0, 0, 1, 0, -0.3, 3, r, half), "a little behind the side line is still in");
        assertFalse(CharybdisMath.inArc(0, 0, 1, 0, -1, 3, r, half), "but 108 degrees round is out");
        assertFalse(CharybdisMath.inArc(0, 0, 1, 0, -3, 0, r, half), "directly behind is out");
        assertFalse(CharybdisMath.inArc(0, 0, 1, 0, r + 0.1, 0, r, half), "out of reach");
        assertTrue(CharybdisMath.inArc(0, 0, 1, 0, 0, 0, r, half), "on top of the boss");
        assertTrue(CharybdisMath.inArc(0, 0, 0, 0, 2, 2, r, half), "no direction: everything in reach");
        assertEquals(200.0, Math.toDegrees(2 * half), 1e-9);
    }

    @Test
    void theCrushCircleIsAnExactDisc() {
        assertTrue(CharybdisMath.inDisc(10, 10, 10, 10, 3.6));
        assertTrue(CharybdisMath.inDisc(10, 10, 13.5, 10, 3.6));
        assertFalse(CharybdisMath.inDisc(10, 10, 14, 10, 3.6));
        assertFalse(CharybdisMath.inDisc(10, 10, 12.6, 12.6, 3.6), "diagonal 3.67 away");
        assertTrue(CharybdisMath.CRUSH_RADIUS <= 4.0, "small enough to swim out of in the wind-up");
    }

    @Test
    void theCrushCanBeEscapedInTheWindUp() {
        // the wind-up (40 ticks, 30 at the Gates) against the swim speeds of a player: sprint-swimming 0.165 blocks a tick
        int windAtGates = Math.round(CharybdisTable.byId(CharybdisTable.TIDAL_CRUSH).windUpTicks() * 0.75f);
        double reachable = (windAtGates - 5) * 0.11;                                     // a quarter of a second to notice, then a plain swim
        assertTrue(reachable > CharybdisMath.CRUSH_RADIUS, "even at the Gates a plain swimmer out of the middle gets clear of the circle: " + reachable);
        int windSweep = Math.round(CharybdisTable.byId(CharybdisTable.TENTACLE_SWEEP).windUpTicks() * 0.75f);
        assertTrue(windSweep >= 8);
    }

    @Test
    void megalodonsComeInPairsUpToFour() {
        assertEquals(2, CharybdisMath.minionsToSummon(0));
        assertEquals(2, CharybdisMath.minionsToSummon(2));
        assertEquals(1, CharybdisMath.minionsToSummon(3));
        assertEquals(0, CharybdisMath.minionsToSummon(4));
        assertEquals(0, CharybdisMath.minionsToSummon(9));
        assertEquals(2, CharybdisMath.minionsToSummon(-3), "a negative count is nobody");
    }

    @Test
    void theWallSpotsStraddleTheWayToThePlayer() {
        double[] two = CharybdisMath.wallAngles(2, 1.0, 0.8);
        assertEquals(1.0 - 0.4, two[0], 1e-9);
        assertEquals(1.0 + 0.4, two[1], 1e-9);
        assertEquals(1.0, CharybdisMath.wallAngles(1, 1.0, 0.8)[0], 1e-9);
        double[] p = CharybdisMath.onRing(100, 200, Math.PI, 25);
        assertEquals(75.0, p[0], 1e-9);
        assertEquals(200.0, p[1], 1e-9);
    }

    @Test
    void theRiseEasesFromTheStartToSixBlocksUp() {
        double start = -12;
        assertEquals(start, CharybdisMath.riseY(start, 60, 60), 1e-9);
        assertEquals(start + CharybdisTable.RISE_BLOCKS, CharybdisMath.riseY(start, 0, 60), 1e-9);
        assertEquals(start + CharybdisTable.RISE_BLOCKS / 2, CharybdisMath.riseY(start, 30, 60), 1e-9);
        double last = Double.NEGATIVE_INFINITY;
        for (int left = 60; left >= 0; left--) {
            double y = CharybdisMath.riseY(start, left, 60);
            if (left < 60) {
                assertTrue(y >= last, "it only rises");
                assertTrue(y - last < 0.35, "and never lurches");
            }
            last = y;
        }
        assertEquals(start + 6, CharybdisMath.riseY(start, -5, 60), 1e-9, "past the end it stays up");
        assertEquals(start + 6, CharybdisMath.riseY(start, 10, 0), 1e-9, "a rise of no length is done");
    }

    @Test
    void aScaleIsAimedFromTheCentreLineByYawAndPitch() {
        double[] same = CharybdisMath.aim(3, 0, 4, 0, 0);
        assertEquals(0.6, same[0], 1e-9);
        assertEquals(0.0, same[1], 1e-9);
        assertEquals(0.8, same[2], 1e-9);
        double[] up = CharybdisMath.aim(1, 0, 0, 0, Math.toRadians(30));
        assertEquals(0.5, up[1], 1e-9, "tipped 30 degrees up");
        assertEquals(Math.cos(Math.toRadians(30)), up[0], 1e-9);
        double[] turned = CharybdisMath.aim(1, 0, 0, Math.PI / 2, 0);
        assertEquals(0.0, turned[0], 1e-9);
        assertEquals(1.0, turned[2], 1e-9, "a quarter turn from +x is +z");
        for (int n = 1; n <= 9; n++) {
            double[] f = CharybdisMath.fan(n);
            for (int i = 0; i < n; i++) {
                double[] d = CharybdisMath.aim(-2.5, 1.0, 4.0, f[i], CharybdisMath.fanPitch(i, n));
                assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1e-9, "always a unit vector");
            }
        }
        double[] straightUp = CharybdisMath.aim(0, 5, 0, 0.3, 0.2);
        assertEquals(1.0, Math.sqrt(straightUp[0] * straightUp[0] + straightUp[1] * straightUp[1] + straightUp[2] * straightUp[2]), 1e-9);
        assertTrue(straightUp[1] > 0.99, "looking straight up stays up");
        double[] none = CharybdisMath.aim(0, 0, 0, 0, 0);
        assertEquals(1.0, none[0], 1e-9, "no direction at all: level along +x");
    }

    @Test
    void theOuterScalesOfAFanLieTheFanAngleApartFromTheMiddleOne() {
        for (int n : new int[]{5, 7, 9}) {
            double[] f = CharybdisMath.fan(n);
            double[] mid = CharybdisMath.aim(0, 0, 1, f[(n - 1) / 2], 0), edge = CharybdisMath.aim(0, 0, 1, f[n - 1], 0);
            double dot = mid[0] * edge[0] + mid[1] * edge[1] + mid[2] * edge[2];
            assertEquals(f[n - 1] - f[(n - 1) / 2], Math.acos(dot), 1e-9, "the angle between the scales is the fan's own");
        }
    }
}
