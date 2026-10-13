package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The immune system's numbers: a 25 percent spell is 62.5 points, a 40 percent spell 160 (over the default threshold of 100 at once). */
class ThreatMeterTest {
    private static final double THRESHOLD = 100.0;

    @Test
    void threatFadesAtOneAndAHalfPointsASecond() {
        ThreatMeter m = new ThreatMeter();
        m.add(60, 1000);
        assertEquals(60.0, m.value(1000), 1e-9);
        assertEquals(60.0 - 1.5, m.value(1020), 1e-9, "one second");
        assertEquals(60.0 - 15.0, m.value(1200), 1e-9, "ten seconds");
        assertEquals(0.0, m.value(1000 + 20 * 41), 1e-9, "all gone after 40 seconds, and never below zero");
        assertEquals(0.0, m.value(1000 + 20 * 400), 1e-9);
    }

    @Test
    void aTwentyFivePercentSpellIsSixtyTwoAndAHalfPoints() {
        assertEquals(62.5, ThreatMeter.spellPoints(25.0), 1e-9);
        assertEquals(160.0, ThreatMeter.spellPoints(40.0), 1e-9);
        assertEquals(22.5, ThreatMeter.spellPoints(15.0), 1e-9);
        assertEquals(0.0, ThreatMeter.spellPoints(14.9), 1e-9, "a small spell is nothing to the Dungeon");
        assertEquals(0.0, ThreatMeter.spellPoints(0.0), 1e-9);
    }

    @Test
    void aFortyPercentSpellIsOverTheDefaultThresholdAtOnce() {
        ThreatMeter m = new ThreatMeter();
        m.add(ThreatMeter.spellPoints(40), 50);
        assertTrue(m.triggered(THRESHOLD, 50));
    }

    @Test
    void aSingleTwentyFivePercentSpellIsNot_ButTwoWithinTenSecondsAre() {
        ThreatMeter m = new ThreatMeter();
        m.add(ThreatMeter.spellPoints(25), 0);
        assertFalse(m.triggered(THRESHOLD, 0));
        m.add(ThreatMeter.spellPoints(25), 200);
        assertTrue(m.triggered(THRESHOLD, 200), "62.5 - 15 + 62.5");
        ThreatMeter slow = new ThreatMeter();
        slow.add(ThreatMeter.spellPoints(25), 0);
        slow.add(ThreatMeter.spellPoints(25), 20 * 60);
        assertFalse(slow.triggered(THRESHOLD, 20 * 60), "a minute apart: the first has faded away");
    }

    @Test
    void aRefusedBlastIsAPointABlockAtMostFortyAndOneBlastIsOneAddition() {
        assertEquals(0.0, ThreatMeter.blastPoints(0), 1e-9);
        assertEquals(1.0, ThreatMeter.blastPoints(1), 1e-9);
        assertEquals(17.0, ThreatMeter.blastPoints(17), 1e-9);
        assertEquals(40.0, ThreatMeter.blastPoints(40), 1e-9);
        assertEquals(40.0, ThreatMeter.blastPoints(5000), 1e-9);
        assertEquals(0.0, ThreatMeter.blastPoints(-3), 1e-9);
    }

    @Test
    void wallHitsCountOncePerFourTicks() {
        ThreatMeter m = new ThreatMeter();
        int counted = 0;
        for (int t = 0; t < 40; t++) if (m.wallHit(100 + t)) counted++;
        assertEquals(10, counted, "a held button fires every tick, one in four counts");
        assertTrue(m.value(139) > 0);
        assertTrue(m.wallHit(500), "after a pause it counts again");
    }

    @Test
    void holdingTheButtonOnAWallBringsAJuggernautInAboutFiveSeconds() {
        ThreatMeter m = new ThreatMeter();
        long t = 0;
        boolean sent = false;
        for (; t < 20 * 20 && !sent; t++) {
            m.wallHit(t);
            sent = m.triggered(THRESHOLD, t);
        }
        assertTrue(sent);
        assertTrue(t > 20 * 4 && t < 20 * 8, "tick " + t);
    }

    @Test
    void aSingleBlowIsFarFromTheThreshold() {
        ThreatMeter m = new ThreatMeter();
        assertTrue(m.wallHit(10));
        assertEquals(4.0, m.value(10), 1e-9, "a refused blow at a wall is 4 points");
        assertFalse(m.triggered(THRESHOLD, 10));
    }

    @Test
    void resettingStartsAgainFromNothing() {
        ThreatMeter m = new ThreatMeter();
        m.add(500, 10);
        m.reset(10);
        assertEquals(0.0, m.value(10), 1e-9);
        assertFalse(m.triggered(THRESHOLD, 11));
    }

    @Test
    void threatNeverPassesTheCeilingOrGoesNegative() {
        ThreatMeter m = new ThreatMeter();
        m.add(50_000, 0);
        assertEquals(ThreatMeter.CEILING, m.value(0), 1e-9);
        m.add(-70, 0);
        assertEquals(ThreatMeter.CEILING, m.value(0), 1e-9, "a negative addition is ignored");
    }

    @Test
    void theClockGoingBackwardsFadesNothing() {
        ThreatMeter m = new ThreatMeter();
        m.add(80, 5000);
        assertEquals(80.0, m.value(100), 1e-9);
        assertEquals(80.0 - 1.5, m.value(120), 1e-9);
    }

    @Test
    void aDueWallHitCanBeAskedAboutWithoutCountingIt() {
        ThreatMeter m = new ThreatMeter();
        assertTrue(m.wallHitDue(100));
        assertTrue(m.wallHitDue(100), "asking changes nothing");
        assertEquals(0.0, m.value(100), 1e-9);
        assertTrue(m.wallHit(100));
        assertFalse(m.wallHitDue(103));
        assertTrue(m.wallHitDue(104));
    }

    @Test
    void theMeterCanBeHeldBelowTheThreshold() {
        ThreatMeter m = new ThreatMeter();
        m.add(400, 10);
        m.clampTo(95, 10);
        assertEquals(95.0, m.value(10), 1e-9);
        m.clampTo(500, 10);
        assertEquals(95.0, m.value(10), 1e-9, "a clamp above the value changes nothing");
        m.clampTo(-3, 10);
        assertEquals(0.0, m.value(10), 1e-9);
    }

    @Test
    void theDungeonWarnsBeforeItActs() {
        assertEquals(0, ThreatMeter.warning(10, 100));
        assertEquals(0, ThreatMeter.warning(49.9, 100));
        assertEquals(1, ThreatMeter.warning(50, 100));
        assertEquals(1, ThreatMeter.warning(79.9, 100));
        assertEquals(2, ThreatMeter.warning(80, 100));
        assertEquals(2, ThreatMeter.warning(99, 100));
        assertEquals(0, ThreatMeter.warning(50, 0), "a threshold of zero warns nothing");
    }
}
