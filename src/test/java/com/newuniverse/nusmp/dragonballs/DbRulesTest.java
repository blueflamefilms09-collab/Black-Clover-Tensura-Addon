package com.newuniverse.nusmp.dragonballs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Pure rules of the Dragon Balls: ball count range, phrase matching (case, punctuation, several lines), lock timer maths, the bounded scatter picker. */
class DbRulesTest {
    @Test
    void ballCountIsSevenOrEightAndCanonIsSeven() {
        assertEquals(7, DbRules.CANON_COUNT);
        assertEquals(7, DbRules.clampCount(0));
        assertEquals(7, DbRules.clampCount(7));
        assertEquals(8, DbRules.clampCount(8));
        assertEquals(8, DbRules.clampCount(99));
    }

    @Test
    void phraseMatchingIgnoresCaseAndPunctuation() {
        assertEquals("takkaradoppo pakkoradoppo yaikanaan", DbRules.norm("Takkaradoppo, Pakkoradoppo -- YAIKANAAN!"));
        assertTrue(DbRules.matchesAny("COME FORTH, Shenron!!!", BallSet.EARTH.defaultPhrases()));
        assertTrue(DbRules.matchesAny("  arise   shenron  ", BallSet.EARTH.defaultPhrases()));
        assertTrue(DbRules.matchesAny("Come forth, dragon of the gods, and grant my wish... pretty peas!", BallSet.SUPER.defaultPhrases()));
    }

    @Test
    void phrasesMatchWholeWordsOnly() {
        assertFalse(DbRules.contains("come forthright shenron", "come forth shenron"));
        assertFalse(DbRules.contains("", "x"));
        assertFalse(DbRules.contains("anything", ""));
        assertFalse(DbRules.matchesAny(null, List.of("a")));
        assertEquals("", DbRules.norm(null));
        assertEquals("", DbRules.norm("?!..."));
    }

    @Test
    void theTwoSetsDoNotShareAPhrase() {
        for (String p : BallSet.EARTH.defaultPhrases()) assertFalse(DbRules.matchesAny(p, BallSet.SUPER.defaultPhrases()), p);
        for (String p : BallSet.SUPER.defaultPhrases()) assertFalse(DbRules.matchesAny(p, BallSet.EARTH.defaultPhrases()), p);
    }

    @Test
    void windowJoinsRecentLinesAndForgetsOldOnes() {
        DbRules.Window w = new DbRules.Window();
        w.add(1_000, "takkaradoppo");
        w.add(3_000, "pakkoradoppo");
        w.add(5_000, "yaikanaan");
        assertTrue(DbRules.contains(w.joined(6_000, 10_000), "takkaradoppo pakkoradoppo yaikanaan"));
        assertFalse(DbRules.contains(w.joined(16_000, 10_000), "takkaradoppo pakkoradoppo yaikanaan"));
        for (int i = 0; i < 20; i++) w.add(7_000 + i, "x" + i);
        assertFalse(w.joined(8_000, 10_000).contains("takkaradoppo"), "the window holds at most " + DbRules.WINDOW_MAX + " lines");
        w.clear();
        assertEquals("", w.joined(8_000, 10_000));
    }

    @Test
    void lockMaths() {
        long now = 1_000_000_000L;
        long until = DbRules.lockUntil(now, 60);
        assertEquals(now + 3_600_000L, until);
        assertTrue(DbRules.locked(until, now));
        assertTrue(DbRules.locked(until, until - 1));
        assertFalse(DbRules.locked(until, until));
        assertEquals(0L, DbRules.lockUntil(now, 0));
        assertEquals(0L, DbRules.lockUntil(now, -5));
        assertEquals(now + DbRules.MAX_LOCK_MINUTES * 60_000L, DbRules.lockUntil(now, Long.MAX_VALUE / 2));
        assertEquals(3_600_000L, DbRules.remainingMs(until, now));
        assertEquals(0L, DbRules.remainingMs(until, until + 5));
    }

    @Test
    void lockTextReadsAsAClock() {
        assertEquals("45s", DbRules.format(45_000));
        assertEquals("58m 12s", DbRules.format(58 * 60_000L + 12_000));
        assertEquals("1h 03m", DbRules.format(3_600_000L + 3 * 60_000L));
        assertEquals("0s", DbRules.format(0));
        assertEquals("0s", DbRules.format(-4));
    }

    @Test
    void scatterAlwaysGivesCountPointsInsideTheRingAndFarApart() {
        for (int count = 7; count <= 8; count++) {
            for (int seed = 0; seed < 40; seed++) {
                List<int[]> pts = DbRules.scatter(new Random(seed), count, 100, -50, 300, 3000, 500);
                assertEquals(count, pts.size());
                for (int[] p : pts) {
                    double d = Math.hypot(p[0] - 100, p[1] + 50);
                    assertTrue(d >= 299 && d <= 3001, "ring " + d);
                }
                for (int i = 0; i < pts.size(); i++)
                    for (int j = i + 1; j < pts.size(); j++)
                        assertTrue(Math.hypot(pts.get(i)[0] - pts.get(j)[0], pts.get(i)[1] - pts.get(j)[1]) >= 250 - 1, "separation at least half of 500 even in the worst case");
            }
        }
    }

    @Test
    void scatterIsDeterministicPerSeedAndTerminatesOnImpossibleAsks() {
        List<int[]> a = DbRules.scatter(new Random(5), 8, 0, 0, 10, 20, 100000);
        List<int[]> b = DbRules.scatter(new Random(5), 8, 0, 0, 10, 20, 100000);
        assertEquals(8, a.size());
        for (int i = 0; i < 8; i++) assertArrayEquals(a.get(i), b.get(i));
        assertEquals(8, DbRules.scatter(new Random(1), 8, 0, 0, 50, 10, -3).size(), "reversed or negative inputs are tolerated");
    }
}
