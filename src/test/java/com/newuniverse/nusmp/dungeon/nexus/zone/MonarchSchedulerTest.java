package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The Monarch's choice of move. */
class MonarchSchedulerTest {
    private static MonarchScheduler.Situation s(int phase, int minions, int foes, double nearest) { return new MonarchScheduler.Situation(phase, minions, foes, nearest); }

    @Test
    void nothingHappensBeforeTheFirstMoveIsDue() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(1000, 60);
        assertNull(m.pick(1059, s(1, 0, 1, 10), 0.5));
        assertNotNull(m.pick(1060, s(1, 0, 1, 10), 0.5));
        assertNull(m.pick(5000, s(1, 0, 0, 10), 0.5), "no foes, no move");
    }

    @Test
    void aMoveIsNeverRepeatedAndAlwaysCoolsDown() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        MonarchMove prev = null;
        long now = 0;
        Map<MonarchMove, long[]> lastUse = new EnumMap<>(MonarchMove.class);
        int moves = 0;
        for (int i = 0; i < 600; i++) {
            int phase = 1 + (i / 200) % 3;
            MonarchMove mv = m.pick(now, s(phase, i % 5, 1 + i % 3, 6 + i % 20), (i * 0.6180339887) % 1.0);
            if (mv == null) {
                now += 5;
                continue;
            }
            assertNotEquals(prev, mv, "never the same move twice running");
            long[] used = lastUse.get(mv);
            if (used != null) assertTrue(now - used[0] >= MonarchRules.cooldown(mv, (int) used[1]), mv + " used again after " + (now - used[0]));
            lastUse.put(mv, new long[]{now, phase});
            m.started(mv, now, phase);
            moves++;
            assertTrue(m.nextAt() >= now + MonarchRules.freeze(mv, phase) + MonarchRules.gap(phase), "the next move waits until this one is over");
            prev = mv;
            now = m.nextAt();
        }
        assertTrue(moves > 30, "moves: " + moves);
    }

    @Test
    void cooldownsAreHonoured() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        m.started(MonarchMove.BEAM, 0, 1);
        m.started(MonarchMove.RAIN, 100, 1);
        assertEquals(0.0, m.weight(MonarchMove.BEAM, 200, s(1, 3, 2, 10)), "still cooling down (210 ticks)");
        assertEquals(0.0, m.weight(MonarchMove.RAIN, 120, s(1, 3, 2, 10)), "the last move may not repeat at once");
        assertTrue(m.weight(MonarchMove.BEAM, 211, s(1, 3, 2, 10)) > 0.0);
        assertTrue(m.weight(MonarchMove.SLAM, 0, s(1, 3, 2, 10)) > 0.0);
    }

    @Test
    void soldiersAreNotSummonedPastTheCap() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        for (int ph = 1; ph <= 3; ph++) {
            int cap = MonarchRules.minionCap(ph);
            assertEquals(0.0, m.weight(MonarchMove.SUMMON, 0, s(ph, cap, 2, 10)), "full");
            assertEquals(0.0, m.weight(MonarchMove.SUMMON, 0, s(ph, cap - 1, 2, 10)), "one short: not worth a summoning");
            assertTrue(m.weight(MonarchMove.SUMMON, 0, s(ph, 0, 2, 10)) > m.weight(MonarchMove.SUMMON, 0, s(ph, 1, 2, 10)), "wanted most when it is alone");
            assertTrue(m.weight(MonarchMove.SUMMON, 0, s(ph, cap - 2, 2, 10)) > 0.0);
        }
    }

    @Test
    void theSituationShapesTheChoice() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        assertTrue(m.weight(MonarchMove.SLAM, 0, s(2, 3, 1, 5)) > m.weight(MonarchMove.SLAM, 0, s(2, 3, 1, 30)), "the slam punishes those who stand close");
        assertTrue(m.weight(MonarchMove.BEAM, 0, s(2, 3, 1, 20)) > m.weight(MonarchMove.BEAM, 0, s(2, 3, 1, 3)), "the line punishes those who keep away");
        assertTrue(m.weight(MonarchMove.RAIN, 0, s(2, 3, 4, 10)) > m.weight(MonarchMove.RAIN, 0, s(2, 3, 1, 10)), "the rain suits a crowd");
        assertTrue(m.weight(MonarchMove.AUTHORITY, 0, s(3, 3, 1, 10)) > m.weight(MonarchMove.AUTHORITY, 0, s(1, 3, 1, 10)), "the field is a late-fight move");
        double before = m.weight(MonarchMove.SLAM, 100, s(2, 3, 2, 12));
        m.started(MonarchMove.AUTHORITY, 100, 2);
        assertTrue(m.weight(MonarchMove.SLAM, 150, s(2, 3, 2, 12)) > before || before == 0.0, "after the field comes a slam or a line");
        assertTrue(m.weight(MonarchMove.BEAM, 150, s(2, 3, 2, 12)) >= 9.0 * 2 - 1e-9);
        assertEquals(0.0, m.weight(MonarchMove.AUTHORITY, 150, s(2, 3, 2, 12)));
    }

    @Test
    void everyMoveCanBeChosenAndTheDiceDecide() {
        Map<MonarchMove, Integer> seen = new EnumMap<>(MonarchMove.class);
        for (int i = 0; i < 1000; i++) {
            MonarchScheduler m = new MonarchScheduler();
            m.reset(0, 0);
            MonarchMove mv = m.pick(0, s(2, 0, 2, 12), i / 1000.0);
            assertNotNull(mv);
            seen.merge(mv, 1, Integer::sum);
            MonarchScheduler twin = new MonarchScheduler();
            twin.reset(0, 0);
            assertSame(mv, twin.pick(0, s(2, 0, 2, 12), i / 1000.0), "same roll, same move");
        }
        assertEquals(MonarchMove.values().length, seen.size(), "all five are reachable: " + seen);
        for (int n : seen.values()) assertTrue(n > 30, "none is vanishingly rare: " + seen);
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        assertNotNull(m.pick(0, s(1, 0, 1, 10), 1.0), "a roll at the top of the range still picks something");
        assertNotNull(m.pick(0, s(1, 0, 1, 10), -3.0));
    }

    @Test
    void theFightGetsFasterWithEachPhase() {
        long[] span = new long[4];
        for (int ph = 1; ph <= 3; ph++) {
            MonarchScheduler m = new MonarchScheduler();
            m.reset(0, 0);
            long now = 0;
            int moves = 0;
            for (int i = 0; i < 4000; i++) {
                MonarchMove mv = m.pick(now, s(ph, 2, 2, 8 + (i % 10)), (i * 0.6180339887) % 1.0);
                if (mv != null) {
                    m.started(mv, now, ph);
                    moves++;
                }
                now++;
            }
            span[ph] = moves;
        }
        assertTrue(span[1] < span[2] && span[2] < span[3], "moves in 4000 ticks: " + span[1] + " " + span[2] + " " + span[3]);
    }

    @Test
    void holdingBackPutsTheNextMoveOff() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        m.holdUntil(500);
        assertNull(m.pick(499, s(1, 0, 1, 10), 0.5));
        assertNotNull(m.pick(500, s(1, 0, 1, 10), 0.5));
        m.holdUntil(100);
        assertEquals(500, m.nextAt(), "holding never brings a move closer");
    }

    @Test
    void theWordsAreChosenWithCare() {
        assertTrue(MonarchText.woken("Alex").contains("Alex"));
        assertFalse(MonarchText.woken(null).isBlank());
        assertTrue(MonarchText.fallen("Alex").contains("Alex"));
        assertTrue(MonarchText.fallen(null).startsWith("The Monarch has fallen"));
        assertTrue(MonarchText.fallenDuringCollapse("Alex").contains("Alex") && !MonarchText.fallenDuringCollapse("Alex").contains("come down"), "a hall already coming down is not announced twice");
        assertTrue(MonarchText.fallenDuringCollapse(null).startsWith("The Monarch has fallen"));
        assertTrue(MonarchText.phase(2).length() > 0 && MonarchText.phase(3).length() > 0 && MonarchText.phase(1).isEmpty());
        assertTrue(MonarchText.bar(2, 3).contains("Phase 2") && MonarchText.bar(2, 3).contains("3"));
        assertFalse(MonarchText.bar(1, 0).contains("Shadows"));
        assertEquals("THE MONARCH", MonarchText.title());
        for (String t : new String[]{MonarchText.woken("x"), MonarchText.fallen("x"), MonarchText.fallenDuringCollapse("x"), MonarchText.subtitle(), MonarchText.phase(3)}) {
            assertTrue(t.length() > 8 && t.length() < 120, "a line that fits the screen: " + t);
        }
    }
}
