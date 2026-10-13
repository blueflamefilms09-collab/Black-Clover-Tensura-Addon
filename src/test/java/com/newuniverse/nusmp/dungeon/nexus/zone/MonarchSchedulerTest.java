package com.newuniverse.nusmp.dungeon.nexus.zone;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The Blood-Red Commander's choice of move, and its words. */
class MonarchSchedulerTest {
    private static MonarchScheduler.Situation s(int phase, int minions, int foes, double nearest) { return new MonarchScheduler.Situation(phase, minions, foes, nearest); }

    private static boolean art(MonarchMove m) { return m.isArt(); }

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
        for (int i = 0; i < 900; i++) {
            int phase = 1 + (i / 300) % 3;
            MonarchMove mv = m.pick(now, s(phase, i % 5, 1 + i % 3, 6 + i % 20), (i * 0.6180339887) % 1.0);
            if (mv == null) {
                now += 5;
                continue;
            }
            assertNotEquals(prev, mv, "never the same move twice running");
            assertNotEquals(MonarchMove.SUMMON, mv, "the soldiers are never called at random");
            long[] used = lastUse.get(mv);
            if (used != null) assertTrue(now - used[0] >= MonarchRules.cooldown(mv, (int) used[1]), mv + " used again after " + (now - used[0]));
            lastUse.put(mv, new long[]{now, phase});
            m.started(mv, now, phase);
            moves++;
            assertTrue(m.nextAt() >= now + MonarchRules.freeze(mv, phase) + MonarchRules.gap(phase), "the next move waits until this one is over");
            prev = mv;
            now = m.nextAt();
        }
        assertTrue(moves > 40, "moves: " + moves);
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
        // the arts too
        m.started(MonarchMove.CROSS_SLASH, 300, 2);
        m.started(MonarchMove.SLAM, 400, 2);
        assertEquals(0.0, m.weight(MonarchMove.CROSS_SLASH, 300 + MonarchRules.cooldown(MonarchMove.CROSS_SLASH, 2) - 1, s(2, 3, 2, 10)));
        assertTrue(m.weight(MonarchMove.CROSS_SLASH, 300 + MonarchRules.cooldown(MonarchMove.CROSS_SLASH, 2), s(2, 3, 2, 10)) > 0.0);
    }

    @Test
    void soldiersAreNeverSummonedAtRandomAnyMore() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        for (int ph = 1; ph <= 3; ph++) {
            for (int minions = 0; minions <= 8; minions++) {
                for (double near : new double[]{2, 8, 14, 30}) {
                    assertEquals(0.0, m.weight(MonarchMove.SUMMON, 0, s(ph, minions, 2, near)), "weight 0 in phase " + ph + " with " + minions + " standing");
                }
            }
        }
        // however the dice fall
        for (int i = 0; i < 2000; i++) {
            MonarchScheduler fresh = new MonarchScheduler();
            fresh.reset(0, 0);
            assertNotEquals(MonarchMove.SUMMON, fresh.pick(0, s(1 + i % 3, i % 9, 1 + i % 4, 2 + i % 30), i / 2000.0));
        }
        // the enum entry and its numbers are still there for the fight to use
        assertNotNull(MonarchMove.valueOf("SUMMON"));
        assertEquals(60, MonarchRules.freeze(MonarchMove.SUMMON, 1));
        m.started(MonarchMove.SUMMON, 100, 2);
        assertEquals(100 + 60 + MonarchRules.gap(2), m.nextAt(), "a forced summoning holds the next move off like any other");
    }

    @Test
    void theSituationShapesTheChoice() {
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        assertTrue(m.weight(MonarchMove.SLAM, 0, s(2, 3, 1, 5)) > m.weight(MonarchMove.SLAM, 0, s(2, 3, 1, 30)), "the slam punishes those who stand close");
        assertTrue(m.weight(MonarchMove.BEAM, 0, s(2, 3, 1, 20)) > m.weight(MonarchMove.BEAM, 0, s(2, 3, 1, 3)), "the line punishes those who keep away");
        assertTrue(m.weight(MonarchMove.RAIN, 0, s(2, 3, 4, 10)) > m.weight(MonarchMove.RAIN, 0, s(2, 3, 1, 10)), "the rain suits a crowd");
        assertTrue(m.weight(MonarchMove.AUTHORITY, 0, s(3, 3, 1, 10)) > m.weight(MonarchMove.AUTHORITY, 0, s(1, 3, 1, 10)), "the field is a late-fight move");
        assertTrue(m.weight(MonarchMove.SWEEP, 0, s(2, 3, 1, 6)) > m.weight(MonarchMove.SWEEP, 0, s(2, 3, 1, 12)), "the arc punishes those who stand close");
        assertTrue(m.weight(MonarchMove.SWEEP, 0, s(2, 3, 1, 12)) > m.weight(MonarchMove.SWEEP, 0, s(2, 3, 1, 28)), "and is wasted on those far away");
        assertTrue(m.weight(MonarchMove.LUNGE, 0, s(2, 3, 1, 20)) > m.weight(MonarchMove.LUNGE, 0, s(2, 3, 1, 10)), "the lunge wants room to run");
        assertTrue(m.weight(MonarchMove.LUNGE, 0, s(2, 3, 1, 10)) > m.weight(MonarchMove.LUNGE, 0, s(2, 3, 1, 3)), "and is no good point-blank");
        assertEquals(m.weight(MonarchMove.CROSS_SLASH, 0, s(2, 3, 1, 3)), m.weight(MonarchMove.CROSS_SLASH, 0, s(2, 3, 1, 30)), "the cross falls under the target wherever it stands");
        // after the field comes a slam, a line or one of the arts
        double slam = m.weight(MonarchMove.SLAM, 100, s(2, 3, 2, 12)), cross = m.weight(MonarchMove.CROSS_SLASH, 100, s(2, 3, 2, 12)),
                sweep = m.weight(MonarchMove.SWEEP, 100, s(2, 3, 2, 12)), lunge = m.weight(MonarchMove.LUNGE, 100, s(2, 3, 2, 12)), beam = m.weight(MonarchMove.BEAM, 100, s(2, 3, 2, 12));
        m.started(MonarchMove.AUTHORITY, 100, 2);
        assertEquals(slam * 2, m.weight(MonarchMove.SLAM, 150, s(2, 3, 2, 12)), 1e-9);
        assertEquals(beam * 2, m.weight(MonarchMove.BEAM, 150, s(2, 3, 2, 12)), 1e-9);
        assertEquals(cross * 2, m.weight(MonarchMove.CROSS_SLASH, 150, s(2, 3, 2, 12)), 1e-9, "the field makes the arts likelier");
        assertEquals(sweep * 2, m.weight(MonarchMove.SWEEP, 150, s(2, 3, 2, 12)), 1e-9);
        assertEquals(lunge * 2, m.weight(MonarchMove.LUNGE, 150, s(2, 3, 2, 12)), 1e-9);
        assertEquals(0.0, m.weight(MonarchMove.AUTHORITY, 150, s(2, 3, 2, 12)));
        assertEquals(cross, m.weight(MonarchMove.CROSS_SLASH, 100 + MonarchScheduler.COMBO_WINDOW + 1, s(2, 3, 2, 12)), 1e-9, "and only for a while");
    }

    @Test
    void everyMoveButTheSummoningCanBeChosenAndTheDiceDecide() {
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
        assertEquals(MonarchMove.values().length - 1, seen.size(), "all seven but the summoning are reachable: " + seen);
        assertFalse(seen.containsKey(MonarchMove.SUMMON));
        for (int n : seen.values()) assertTrue(n > 30, "none is vanishingly rare: " + seen);
        MonarchScheduler m = new MonarchScheduler();
        m.reset(0, 0);
        assertNotNull(m.pick(0, s(1, 0, 1, 10), 1.0), "a roll at the top of the range still picks something");
        assertNotNull(m.pick(0, s(1, 0, 1, 10), -3.0));
    }

    @Test
    void theArtsAreAboutHalfOfTheWeightInEveryPhase() {
        for (int ph = 1; ph <= 3; ph++) {
            MonarchScheduler m = new MonarchScheduler();
            m.reset(0, 0);
            MonarchScheduler.Situation ref = s(ph, 3, 2, 12);                        // two foes, the nearest a few blocks off
            double arts = 0, all = 0;
            for (MonarchMove mv : MonarchMove.values()) {
                double w = m.weight(mv, 0, ref);
                all += w;
                if (art(mv)) arts += w;
                else if (mv != MonarchMove.SUMMON) assertTrue(w > 0.0, mv + " keeps a share of the rest");
            }
            assertEquals(0.55, arts / all, 0.025, "the arts' share of the weight in phase " + ph + ": " + arts / all);
        }
    }

    @Test
    void theArtsAreMostOfWhatItActuallyDoes() {
        Random r = new Random(11);
        for (int ph = 1; ph <= 3; ph++) {
            long picks = 0, artPicks = 0;
            Map<MonarchMove, Integer> by = new EnumMap<>(MonarchMove.class);
            for (int fight = 0; fight < 120; fight++) {
                MonarchScheduler m = new MonarchScheduler();
                m.reset(0, 0);
                long now = 0;
                while (now < 5000) {
                    MonarchMove mv = m.pick(now, s(ph, r.nextInt(5), 1 + r.nextInt(3), 3 + r.nextDouble() * 25), r.nextDouble());
                    if (mv == null) {
                        now += 5;
                        continue;
                    }
                    m.started(mv, now, ph);
                    picks++;
                    by.merge(mv, 1, Integer::sum);
                    if (art(mv)) artPicks++;
                    now = m.nextAt();
                }
            }
            double share = (double) artPicks / picks;
            assertTrue(share > 0.46 && share < 0.62, "arts are " + share + " of the moves in phase " + ph + ": " + by);
            for (MonarchMove mv : MonarchMove.values()) if (mv != MonarchMove.SUMMON) assertTrue(by.getOrDefault(mv, 0) > picks / 40, mv + " still gets thrown: " + by);
        }
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
        assertEquals("The Blood-Red Commander", MonarchText.NAME);
        assertTrue(MonarchText.woken("Alex").contains("Alex") && MonarchText.woken("Alex").contains("Blood-Red Commander"));
        assertFalse(MonarchText.woken(null).isBlank());
        assertTrue(MonarchText.fallen("Alex").contains("Alex"));
        assertTrue(MonarchText.fallen(null).startsWith("The Blood-Red Commander has fallen"));
        assertTrue(MonarchText.fallenDuringCollapse("Alex").contains("Alex") && !MonarchText.fallenDuringCollapse("Alex").contains("come down"), "a hall already coming down is not announced twice");
        assertTrue(MonarchText.fallenDuringCollapse(null).startsWith("The Blood-Red Commander has fallen"));
        assertTrue(MonarchText.phase(2).length() > 0 && MonarchText.phase(3).length() > 0 && MonarchText.phase(1).isEmpty());
        assertTrue(MonarchText.bar(2, 3).contains("Phase 2") && MonarchText.bar(2, 3).contains("3"));
        assertTrue(MonarchText.bar(1, 0).startsWith("The Blood-Red Commander"));
        assertFalse(MonarchText.bar(1, 0).contains("Shadows"));
        assertEquals("THE BLOOD-RED COMMANDER", MonarchText.title());
        for (String t : new String[]{MonarchText.woken("x"), MonarchText.fallen("x"), MonarchText.fallenDuringCollapse("x"), MonarchText.subtitle(), MonarchText.phase(3), MonarchText.woken("Sixteen_Chars_OK_")}) {
            assertTrue(t.length() > 8 && t.length() < 120, "a line that fits the screen: " + t);
        }
        for (MonarchMove m : MonarchMove.values()) assertTrue(MonarchText.warning(m).length() > 10 && MonarchText.warning(m).length() < 70, "an action-bar line: " + MonarchText.warning(m));
        for (String t : new String[]{MonarchText.woken("x"), MonarchText.fallen("x"), MonarchText.title(), MonarchText.subtitle(), MonarchText.phase(2), MonarchText.phase(3), MonarchText.bar(2, 2)}) {
            assertFalse(t.contains("The Monarch") || t.contains("THE MONARCH"), "the old name is gone from what players see: " + t);
        }
    }
}
