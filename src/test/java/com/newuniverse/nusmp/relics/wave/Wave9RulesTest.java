package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 9 of the Relics of Many Worlds: the pure rules of the ten weapons (saving throws, dice, curses and chances turned into numbers). */
class Wave9RulesTest {
    @Test
    void bossesResistLongEffectsByHalf() {
        assertFalse(Wave9Rules.isBoss(99.0));
        assertTrue(Wave9Rules.isBoss(100.0));
        assertEquals(80, Wave9Rules.controlTicks(80, 20.0));
        assertEquals(40, Wave9Rules.controlTicks(80, 300.0));
        assertEquals(1, Wave9Rules.controlTicks(1, 300.0), "never rounds to nothing");
    }

    @Test
    void nullBladeDrainIsCappedPerVictim() {
        assertEquals(3.0, Wave9Rules.drain(0, 20.0, true, 3), 1e-9);
        assertEquals(6.0, Wave9Rules.drain(0, 200.0, false, 6), 1e-9);
        assertEquals(1.0, Wave9Rules.drain(0, 20.0, true, 0), 1e-9, "a roll under one counts as one");
        assertEquals(6.0, Wave9Rules.drain(0, 20.0, true, 99), 1e-9, "a roll over six counts as six; 30% of 20 is 6");
        assertEquals(0.0, Wave9Rules.drain(6.0, 20.0, true, 4), 1e-9, "a player loses at most 30% of their health in all");
        assertEquals(2.0, Wave9Rules.drain(4.0, 20.0, true, 5), 1e-9, "only what is left under the cap");
        assertEquals(8.0, Wave9Rules.drain(0, 1000.0, true, 6) + 2.0, 1e-9, "a huge player pool still caps one hit at six");
        assertEquals(0.0, Wave9Rules.drain(10.0, 20.0, false, 6), 1e-9, "a mob loses at most half");
        assertTrue(Wave9Rules.drainTicks(true) < Wave9Rules.drainTicks(false), "players recover sooner than mobs");
    }

    @Test
    void temporaryHealthFromDrainHasACeiling() {
        assertEquals(3.0, Wave9Rules.tempFromDrain(3.0, 0.0), 1e-9);
        assertEquals(2.0, Wave9Rules.tempFromDrain(6.0, 8.0), 1e-9, "10 in all");
        assertEquals(0.0, Wave9Rules.tempFromDrain(6.0, 12.0), 1e-9);
    }

    @Test
    void awakeningNeedsTwentyLives() {
        assertFalse(Wave9Rules.awakened(19));
        assertTrue(Wave9Rules.awakened(20));
    }

    @Test
    void devouringGivesAsMuchAsTheHitUpToACap() {
        assertEquals(7.0, Wave9Rules.devourTemp(7.0, false), 1e-9);
        assertEquals(10.0, Wave9Rules.devourTemp(25.0, false), 1e-9);
        assertEquals(6.0, Wave9Rules.devourTemp(25.0, true), 1e-9);
        assertEquals(0.0, Wave9Rules.devourTemp(-3.0, false), 1e-9);
    }

    @Test
    void hungerGnawsOnlyWhenStarvedAndNeverToTheLastHearts() {
        assertEquals(0.0, Wave9Rules.gnaw(1000, 900, 0, 20.0), 1e-9, "fed a moment ago");
        assertEquals(1.0, Wave9Rules.gnaw(10000, 1000, 0, 20.0), 1e-9);
        assertEquals(0.0, Wave9Rules.gnaw(10000, 1000, 9800, 20.0), 1e-9, "not twice in twenty seconds");
        assertEquals(1.0, Wave9Rules.gnaw(10000, 1000, 9500, 20.0), 1e-9);
        assertEquals(0.0, Wave9Rules.gnaw(10000, 1000, 0, 5.0), 1e-9, "stops at two hearts");
        assertEquals(1.0, Wave9Rules.gnaw(10000, 1000, 0, 5.5), 1e-9);
    }

    @Test
    void theGladiatorsRebelOneTimeInTen() {
        int n = 0;
        for (int i = 0; i < 1000; i++) if (Wave9Rules.rebels(i / 1000.0)) n++;
        assertEquals(100, n);
        for (int r = -5000; r < 5000; r += 37) {
            int d = Wave9Rules.rebelDelay(r);
            assertTrue(d >= 600 && d < 1800, "between thirty and ninety seconds: " + d);
        }
        assertEquals(5 * 24000, Wave9Rules.GLADIUS_COOLDOWN, "once every five days");
    }

    @Test
    void theExileCircleIsHalfOpen() {
        assertTrue(Wave9Rules.inExile(0, 0));
        assertTrue(Wave9Rules.inExile(160, 0));
        assertFalse(Wave9Rules.inExile(160.5, 0));
        assertTrue(Wave9Rules.inExile(100, -100));
        assertFalse(Wave9Rules.inExile(120, 120));
    }

    @Test
    void twinSwordsCritOneTimeInTen() {
        int n = 0;
        for (int i = 0; i < 1000; i++) if (Wave9Rules.twinCrit(i / 1000.0)) n++;
        assertEquals(100, n);
    }

    @Test
    void theViperCompassNamesTheEightWinds() {
        assertEquals("N", Wave9Rules.compass(0, -10));
        assertEquals("S", Wave9Rules.compass(0, 10));
        assertEquals("E", Wave9Rules.compass(10, 0));
        assertEquals("W", Wave9Rules.compass(-10, 0));
        assertEquals("NE", Wave9Rules.compass(10, -10));
        assertEquals("SW", Wave9Rules.compass(-10, 10));
        assertEquals("NW", Wave9Rules.compass(-10, -10));
        assertEquals("SE", Wave9Rules.compass(10, 10));
        assertEquals("here", Wave9Rules.compass(0.1, 0.1));
        assertEquals("N", Wave9Rules.compass(1, -100), "a slight east of north is still north");
    }

    @Test
    void thornblightCostsOnlyWhileYouAreHealthy() {
        assertTrue(Wave9Rules.thornPrice(20.0));
        assertTrue(Wave9Rules.thornPrice(5.5));
        assertFalse(Wave9Rules.thornPrice(5.0), "a point is taken from six and no less: the last two hearts stay");
        assertFalse(Wave9Rules.thornPrice(4.5));
    }
}
