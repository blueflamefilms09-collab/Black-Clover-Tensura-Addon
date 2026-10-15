package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, part 4, wave 24: the pure rules of the twelve charms. */
class Wave24RulesTest {
    @Test
    void diceStayInRange() {
        for (int i = -200; i < 200; i++) {
            final int seed = i;
            for (int sides : new int[]{4, 6, 8, 12, 20}) {
                int v = Wave24Rules.d(n -> seed, sides);
                assertTrue(v >= 1 && v <= sides, "d" + sides + " " + v);
            }
        }
        assertEquals(10, Wave24Rules.roll(n -> 0, 10, 6) - 0);
    }

    @Test
    void savingThrowHasNaturalTwentyAndOne() {
        assertTrue(Wave24Rules.saves(20, -10, 40));
        assertFalse(Wave24Rules.saves(1, 20, 5));
        assertTrue(Wave24Rules.saves(10, 5, 15));
        assertFalse(Wave24Rules.saves(9, 5, 15));
        assertEquals(0, Wave24Rules.saveBonus(8));
        assertEquals(5, Wave24Rules.saveBonus(400));
        assertEquals(60, Wave24Rules.afterSave(120, true, 20));
        assertEquals(30, Wave24Rules.afterSave(120, true, 200), "a boss halves it again");
    }

    @Test
    void cookingTableAndTime() {
        assertEquals("minecraft:cooked_beef", Wave24Rules.cooked("minecraft:beef"));
        assertEquals("minecraft:baked_potato", Wave24Rules.cooked("minecraft:potato"));
        assertNull(Wave24Rules.cooked("minecraft:stone"));
        assertNull(Wave24Rules.cooked(null));
        assertEquals(100, Wave24Rules.cookTicks(1));
        assertEquals(400, Wave24Rules.cookTicks(4));
        assertEquals(400, Wave24Rules.cookTicks(9), "clamped");
        assertEquals(16, Wave24Rules.cookCount(64));
        assertEquals(0, Wave24Rules.cookCount(-3));
    }

    @Test
    void luckyEggNeverMakesItWorse() {
        for (double r = 0; r <= 1.0; r += 0.05) {
            float again = Wave24Rules.rerolled(10f, r);
            assertTrue(again >= 3.0f - 1e-4 && again < 8.0f + 1e-4, "reroll " + again);
        }
    }

    @Test
    void lumpyTalksOnceAMinute() {
        assertTrue(Wave24Rules.lumpyMayTalk(100, 0));
        assertFalse(Wave24Rules.lumpyMayTalk(1000, 500));
        assertTrue(Wave24Rules.lumpyMayTalk(1700, 500));
        assertTrue(Wave24Rules.lumpyMayTalk(10, 500), "the clock went back: free to speak");
        for (Wave24Rules.Mood m : Wave24Rules.Mood.values()) for (int i = -3; i < 9; i++) assertFalse(Wave24Rules.lumpyLine(m, i).isEmpty());
        assertTrue(Wave24Rules.lowHealth(4f, 20f));
        assertFalse(Wave24Rules.lowHealth(10f, 20f));
        assertFalse(Wave24Rules.lowHealth(0f, 20f));
    }

    @Test
    void compassThreatAndBearing() {
        assertEquals(0, Wave24Rules.threat(20, 3));
        assertEquals(2, Wave24Rules.threat(60, 5));
        assertEquals(4, Wave24Rules.threat(300, 0), "a boss is black");
        assertEquals("black", Wave24Rules.eye(4));
        assertEquals("pale gold", Wave24Rules.eye(-1));
        // facing south (yaw 0): a target to the south is ahead, to the west on the right, to the north behind
        assertEquals(0, Wave24Rules.bearing(0, 10, 0));
        assertEquals(2, Wave24Rules.bearing(-10, 0, 0));
        assertEquals(4, Wave24Rules.bearing(0, -10, 0));
        assertEquals(6, Wave24Rules.bearing(10, 0, 0));
        assertEquals(0, Wave24Rules.bearing(-10, 0, 90), "facing west the western target is ahead");
        assertEquals("↑", Wave24Rules.arrow(0));
        assertEquals("↑", Wave24Rules.arrow(8));
    }

    @Test
    void badgeIgnoresBossesAndGiants() {
        assertTrue(Wave24Rules.respects(20));
        assertFalse(Wave24Rules.respects(60));
        assertFalse(Wave24Rules.respects(500));
    }

    @Test
    void orbChargesRegainOnceADay() {
        assertEquals(9, Wave24Rules.regained(2, 3, 4, 4));
        assertEquals(10, Wave24Rules.regained(8, 3, 4, 6), "capped at ten");
        assertEquals(2, Wave24Rules.regained(2, 4, 4, 6), "same day: nothing");
        assertEquals(10, Wave24Rules.regained(99, 0, 0, 1), "never above ten");
        assertTrue(Wave24Rules.shatters(1));
        assertFalse(Wave24Rules.shatters(2));
        assertEquals(12, Wave24Rules.plagueDc(2));
        assertEquals(10, Wave24Rules.plagueDc(-5));
    }

    @Test
    void diseasesAreSixAndWellFormed() {
        assertEquals(6, Wave24Rules.Disease.values().length);
        for (int r = -2; r < 12; r++) assertNotNull(Wave24Rules.disease(r));
        for (Wave24Rules.Disease d : Wave24Rules.Disease.values()) {
            assertTrue(d.effects.length >= 2);
            for (String e : d.effects) {
                assertTrue(Wave24Rules.effectId(e).contains(":"));
                assertTrue(Wave24Rules.effectAmp(e) >= 0);
            }
        }
        assertEquals("minecraft:wither", Wave24Rules.effectId("minecraft:wither:0"));
        assertEquals(1, Wave24Rules.effectAmp("minecraft:weakness:1"));
        assertEquals(0, Wave24Rules.effectAmp("minecraft:weakness:x"));
    }

    @Test
    void dryadHealAndWalls() {
        assertEquals(2f, Wave24Rules.dryadHeal(1));
        assertEquals(5f, Wave24Rules.dryadHeal(4));
        assertEquals(5f, Wave24Rules.dryadHeal(9));
        assertFalse(Wave24Rules.dryadBlocked(1));
        assertTrue(Wave24Rules.dryadBlocked(2));
        assertEquals(1, Wave24Rules.flightTicks(0.2));
        assertEquals(60, Wave24Rules.flightTicks(500));
    }

    @Test
    void pocketTimeLandsOnTheRolledHour() {
        // tick 0 reads 06:00; the next 09:00 (or 21:00) is three hours on
        assertEquals(6, Wave24Rules.clockHour(0));
        assertEquals(3000L, Wave24Rules.ticksForward(0, 9));
        // at 06:00 a roll of 6 jumps twelve hours, to 18:00, never zero
        assertEquals(12000L, Wave24Rules.ticksForward(0, 6));
        for (long t = 0; t < 24000; t += 700) {
            for (int r = 1; r <= 12; r++) {
                long fwd = Wave24Rules.ticksForward(t, r);
                assertTrue(fwd > 0 && fwd <= 12000, "t=" + t + " r=" + r + " fwd=" + fwd);
                assertEquals(r % 12, Wave24Rules.clockHour(t + fwd) % 12, "lands on the hour t=" + t + " r=" + r);
            }
        }
        assertEquals("06:00", Wave24Rules.clockText(0));
        assertEquals("12:30", Wave24Rules.clockText(6500));
    }

    @Test
    void endlessRageNeverKills() {
        assertTrue(Wave24Rules.lethal(5f, 0f, 5f));
        assertFalse(Wave24Rules.lethal(5f, 4f, 5f));
        assertFalse(Wave24Rules.lethal(5f, 0f, 0f));
        assertEquals(4f, Wave24Rules.rageGuard(5f, 100f), 1e-6);
        assertEquals(2f, Wave24Rules.rageGuard(5f, 2f), 1e-6);
        assertEquals(0f, Wave24Rules.rageGuard(1f, 9f), 1e-6, "at one health nothing more gets through");
        assertEquals(3, Wave24Rules.deathSaves(n -> 19));
        assertEquals(0, Wave24Rules.deathSaves(n -> 0));
        assertEquals(Wave24Rules.Aftermath.STABLE_GOOD, Wave24Rules.aftermath(3));
        assertEquals(Wave24Rules.Aftermath.STABLE, Wave24Rules.aftermath(2));
        assertEquals(Wave24Rules.Aftermath.BARELY, Wave24Rules.aftermath(1));
        assertEquals(Wave24Rules.Aftermath.EXHAUSTED, Wave24Rules.aftermath(0));
        assertEquals(0.0, Wave24Rules.aftermathHealth(Wave24Rules.Aftermath.EXHAUSTED), 1e-9, "the caller floors it at half a heart");
        assertTrue(Wave24Rules.aftermathHealth(Wave24Rules.Aftermath.STABLE_GOOD) > Wave24Rules.aftermathHealth(Wave24Rules.Aftermath.STABLE));
    }

    @Test
    void ringOfPowerNumbers() {
        assertEquals(8f, Wave24Rules.forceBonus(8, false), 1e-6);
        assertEquals(4f, Wave24Rules.forceBonus(8, true), 1e-6, "held down on a player");
        assertEquals(1f, Wave24Rules.forceBonus(-3, false), 1e-6);
        int dis = (int) Wave24Rules.disintegrate(n -> 5);
        assertEquals(60, dis);
        assertEquals(10, (int) Wave24Rules.disintegrate(n -> 0));
    }

    @Test
    void regenerationHealsAndRegrows() {
        assertEquals(1f, Wave24Rules.regenAmount(0));
        assertEquals(6f, Wave24Rules.regenAmount(9));
        assertEquals(0, Wave24Rules.regrown(100));
        assertEquals(0, Wave24Rules.regrown(40));
        assertEquals(300, Wave24Rules.regrown(600));
        assertEquals(12, Wave24Rules.CRIPPLING.length);
    }

    @Test
    void trailKeepsTheLatestAndForgetsAfterAWeek() {
        long[] t = new long[0];
        for (int i = 0; i < Wave24Rules.TRAIL_MAX + 25; i++) t = Wave24Rules.push(t, 1000L + i * 40L, i, 64, -i);
        assertEquals(Wave24Rules.TRAIL_MAX, Wave24Rules.samples(t));
        assertEquals(25, t[1], "the oldest 25 samples were dropped");
        assertEquals(Wave24Rules.TRAIL_MAX + 24, t[(Wave24Rules.TRAIL_MAX - 1) * Wave24Rules.TRAIL_STRIDE + 1]);
        assertEquals(0, Wave24Rules.samples(Wave24Rules.push(null, 0, 0, 0, 0)) - 1);
        long[] old = Wave24Rules.push(Wave24Rules.push(null, 0, 1, 1, 1), 200000L, 2, 2, 2);
        long[] kept = Wave24Rules.prune(old, 200000L + 10);
        assertEquals(1, Wave24Rules.samples(kept), "the sample over a week old is gone");
        assertEquals(2L, kept[1]);
        assertTrue(Wave24Rules.storable(Wave24Rules.WEEK, 0));
        assertFalse(Wave24Rules.storable(Wave24Rules.WEEK + 1, 0));
        assertFalse(Wave24Rules.storable(5, 10), "from the future");
        assertEquals(0, Wave24Rules.samples(Wave24Rules.prune(null, 5)));
        assertEquals(0L, Wave24Rules.span(null));
        assertEquals(80L, Wave24Rules.span(Wave24Rules.push(Wave24Rules.push(Wave24Rules.push(null, 10, 0, 0, 0), 50, 0, 0, 0), 90, 0, 0, 0)));
    }

    @Test
    void tranceShowsEverySampleOnce() {
        for (int n : new int[]{0, 3, 10, 37, 150}) {
            int shown = 0, prevTo = 0;
            for (int s = 0; s < Wave24Rules.TRANCE_STEPS; s++) {
                int[] r = Wave24Rules.tranceRange(n, s);
                assertEquals(prevTo, r[0], "contiguous n=" + n + " step " + s);
                assertTrue(r[1] >= r[0]);
                shown += r[1] - r[0];
                prevTo = r[1];
            }
            assertEquals(n, shown, "every sample once, n=" + n);
        }
        assertEquals(1, Wave24Rules.clarityStride(5));
        assertEquals(8, Wave24Rules.clarityStride(5000));
        assertTrue(Wave24Rules.clarityStride(100) < Wave24Rules.clarityStride(300));
    }
}
