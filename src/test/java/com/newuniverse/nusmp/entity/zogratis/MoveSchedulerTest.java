package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The move scheduler: determinism, cooldowns, range and power gates, mana, and the repeat and ultimate rules. */
class MoveSchedulerTest {
    record Spec(String id, int minPower, int windUp, int cooldown, double minRange, double maxRange, int weight, boolean ultimate, double cost, boolean crowd) implements MoveSpec {
        Spec(String id, int minPower, double minRange, double maxRange, int weight, boolean ultimate) { this(id, minPower, 20, 100, minRange, maxRange, weight, ultimate, 0, false); }
        @Override public String label() { return id; }
        @Override public int windUpTicks() { return windUp; }
        @Override public int cooldownTicks() { return cooldown; }
        @Override public int minPower() { return minPower; }
        @Override public double minRange() { return minRange; }
        @Override public double maxRange() { return maxRange; }
        @Override public int weight() { return weight; }
        @Override public boolean ultimate() { return ultimate; }
        @Override public double manaCost() { return cost; }
        @Override public boolean crowd() { return crowd; }
    }

    static MoveScheduler.Situation at(long now, double dist, int power) {
        return new MoveScheduler.Situation(dist, 0.8f, power, 1, now, Double.POSITIVE_INFINITY, 900);
    }

    static final List<Spec> BOOK = List.of(
            new Spec("a", 20, 0, 12, 3, false),
            new Spec("b", 20, 4, 30, 3, false),
            new Spec("c", 40, 0, 40, 2, false),
            new Spec("ult", 60, 0, 40, 5, true));

    /** Plays a fight: at each decision tick pick and commit with the move's own cooldown. */
    static List<String> play(long seed, int decisions, int power) { return play(seed, decisions, power, -1); }

    /** 'cooldown' -1 uses each move's own. */
    static List<String> play(long seed, int decisions, int power, int cooldown) {
        MoveScheduler s = new MoveScheduler(seed);
        s.reset(0);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < decisions; i++) {
            long now = 1000L + i * 10L;
            MoveScheduler.Situation sit = at(now, 8, power);
            int idx = s.pick(BOOK, sit);
            if (idx < 0) { out.add("-"); continue; }
            s.commit(BOOK.get(idx), sit, cooldown >= 0 ? cooldown : BOOK.get(idx).cooldown());
            out.add(BOOK.get(idx).id());
        }
        return out;
    }

    @Test
    void sameSeedSamePicks() {
        assertEquals(play(7, 200, 100), play(7, 200, 100));
        assertEquals(play(7, 200, 100, 0), play(7, 200, 100, 0));
        assertNotEquals(play(7, 200, 100, 0), play(8, 200, 100, 0), "a different seed is a different fight");
    }

    @Test
    void rangeGatesTheMoves() {
        MoveScheduler s = new MoveScheduler(1);
        s.reset(0);
        for (int i = 0; i < 100; i++) {
            int idx = s.pick(BOOK, at(5000 + i, 2, 20));
            assertEquals("a", BOOK.get(idx).id(), "at 20% only 'a' reaches 2 blocks");
        }
        assertEquals(-1, s.pick(BOOK, at(5000, 50, 100)), "nothing reaches 50 blocks: close in");
        Spec b = BOOK.get(1);                                   // 4 .. 30 blocks
        assertFalse(s.eligible(b, at(1, 3.99, 20)));
        assertTrue(s.eligible(b, at(1, 4, 20)), "the band is inclusive");
        assertTrue(s.eligible(b, at(1, 30, 20)));
        assertFalse(s.eligible(b, at(1, 30.01, 20)));
    }

    @Test
    void powerGatesTheMoves() {
        MoveScheduler s = new MoveScheduler(2);
        s.reset(0);
        for (int i = 0; i < 200; i++) {
            int idx = s.pick(BOOK, at(5000, 8, 20));
            assertTrue(idx == 0 || idx == 1, "at 20% only a and b: " + idx);
        }
        assertFalse(s.eligible(BOOK.get(2), at(5000, 8, 39)));
        assertTrue(s.eligible(BOOK.get(2), at(5000, 8, 40)));
        assertFalse(s.eligible(BOOK.get(3), at(5000, 8, 59)), "the ultimate waits for 60%");
    }

    @Test
    void cooldownIsHonoured() {
        MoveScheduler s = new MoveScheduler(3);
        s.reset(0);
        Spec a = BOOK.get(0);
        MoveScheduler.Situation t = at(1000, 8, 20);
        s.commit(a, t, 100);
        assertEquals(100, s.cooldownLeft("a", 1000));
        assertFalse(s.eligible(a, at(1099, 8, 20)));
        assertTrue(s.eligible(a, at(1100, 8, 20)));
        assertEquals(0, s.cooldownLeft("a", 1100));
        assertEquals(0, s.cooldownLeft("never_used", 1100));
        s.holdUntil("b", 2000);
        assertFalse(s.eligible(BOOK.get(1), at(1500, 8, 20)));
    }

    @Test
    void manaGatesTheMoves() {
        MoveScheduler s = new MoveScheduler(4);
        s.reset(0);
        Spec pricey = new Spec("pricey", 20, 20, 100, 0, 30, 1, false, 30, false);
        assertFalse(s.eligible(pricey, new MoveScheduler.Situation(5, 1f, 20, 1, 1000, 29.9, 900)));
        assertTrue(s.eligible(pricey, new MoveScheduler.Situation(5, 1f, 20, 1, 1000, 30, 900)));
        assertTrue(s.eligible(pricey, at(1000, 5, 60)), "unbound mana is infinite");
    }

    @Test
    void neverTheSameOrdinaryMoveThreeTimesInARow() {
        // only 'a' is allowed by range, and no cooldown: it can be used twice, then the scheduler offers nothing
        MoveScheduler s = new MoveScheduler(5);
        s.reset(0);
        Spec a = BOOK.get(0);
        List<Spec> only = List.of(a);
        int streak = 0, none = 0;
        for (int i = 0; i < 20; i++) {
            MoveScheduler.Situation sit = at(1000 + i, 5, 20);
            int idx = s.pick(only, sit);
            if (idx < 0) { none++; streak = 0; s.commit(BOOK.get(1), sit, 0); continue; }   // another move breaks the streak
            s.commit(a, sit, 0);
            streak++;
            assertTrue(streak <= 2, "three in a row at decision " + i);
        }
        assertTrue(none > 0, "the third repeat was refused");
    }

    @Test
    void noMoveRepeatsThreeTimesInLongRandomFights() {
        for (long seed = 0; seed < 40; seed++) {
            List<String> fight = play(seed, 400, 100, seed % 2 == 0 ? 0 : -1);
            int run = 0;
            String prev = "";
            for (String m : fight) {
                if (m.equals("-")) continue;
                if (BOOK.stream().filter(b -> b.id().equals(m)).findFirst().orElseThrow().ultimate()) { run = 0; prev = m; continue; }
                run = m.equals(prev) ? run + 1 : 1;
                prev = m;
                assertTrue(run <= 2, "seed " + seed + " repeated " + m + " " + run + " times");
            }
        }
    }

    @Test
    void ultimatesAreSpacedAndNeverBackToBack() {
        MoveScheduler s = new MoveScheduler(6);
        s.reset(0);
        Spec ult = BOOK.get(3);
        assertFalse(s.eligible(ult, at(399, 8, 100)), "not before the first delay");
        assertTrue(s.eligible(ult, at(400, 8, 100)));
        s.commit(ult, at(1000, 8, 100), 40);
        assertFalse(s.eligible(ult, at(1000 + 899, 8, 100)), "gap of 900 ticks");
        // after the gap, but the last move was the ultimate: not twice in a row
        assertFalse(s.eligible(ult, at(1000 + 900, 8, 100)));
        s.commit(BOOK.get(0), at(1100, 8, 100), 12);
        assertFalse(s.eligible(ult, at(1000 + 899, 8, 100)));
        assertTrue(s.eligible(ult, at(1000 + 900, 8, 100)), "gap over and another move in between");
    }

    @Test
    void ultimateCountOverALongFightRespectsTheGap() {
        for (long seed = 0; seed < 20; seed++) {
            MoveScheduler s = new MoveScheduler(seed);
            s.reset(0);
            List<Long> ults = new ArrayList<>();
            for (long now = 0; now < 20000; now += 5) {
                MoveScheduler.Situation sit = at(now, 8, 100);
                int idx = s.pick(BOOK, sit);
                if (idx < 0) continue;
                s.commit(BOOK.get(idx), sit, BOOK.get(idx).cooldown());
                if (BOOK.get(idx).ultimate()) ults.add(now);
            }
            assertFalse(ults.isEmpty(), "an ultimate comes eventually (seed " + seed + ")");
            assertTrue(ults.get(0) >= MoveScheduler.FIRST_ULTIMATE_DELAY);
            for (int i = 1; i < ults.size(); i++) assertTrue(ults.get(i) - ults.get(i - 1) >= 900, "ultimates " + ults.get(i - 1) + " and " + ults.get(i));
        }
    }

    @Test
    void crowdMovesLikeCrowds() {
        MoveScheduler s = new MoveScheduler(9);
        s.reset(0);
        Spec solo = new Spec("solo", 20, 20, 100, 0, 30, 4, false, 0, false);
        Spec crowd = new Spec("crowd", 20, 20, 100, 0, 30, 4, false, 0, true);
        MoveScheduler.Situation one = new MoveScheduler.Situation(10, 1f, 20, 1, 1000, Double.POSITIVE_INFINITY, 900);
        MoveScheduler.Situation many = new MoveScheduler.Situation(10, 1f, 20, 4, 1000, Double.POSITIVE_INFINITY, 900);
        assertTrue(s.weightOf(crowd, many) > s.weightOf(solo, many));
        assertTrue(s.weightOf(crowd, one) < s.weightOf(solo, one));
    }

    @Test
    void ultimatesGetLikelierWhenHurt() {
        MoveScheduler s = new MoveScheduler(10);
        s.reset(0);
        Spec ult = BOOK.get(3);
        double healthy = s.weightOf(ult, new MoveScheduler.Situation(8, 1f, 100, 1, 5000, Double.POSITIVE_INFINITY, 900));
        double hurt = s.weightOf(ult, new MoveScheduler.Situation(8, 0.1f, 100, 1, 5000, Double.POSITIVE_INFINITY, 900));
        assertTrue(hurt > healthy * 2);
    }

    @Test
    void noMovesNoPick() {
        MoveScheduler s = new MoveScheduler(11);
        assertEquals(-1, s.pick(List.of(), at(1000, 5, 100)));
    }

    @Test
    void resetClearsTheHistory() {
        MoveScheduler s = new MoveScheduler(12);
        s.reset(0);
        s.commit(BOOK.get(0), at(1000, 8, 20), 5000);
        assertTrue(s.cooldownLeft("a", 1001) > 0);
        s.reset(2000);
        assertEquals(0, s.cooldownLeft("a", 2001));
        assertEquals("", s.last());
        assertFalse(s.eligible(BOOK.get(3), at(2399, 8, 100)));
        assertTrue(s.eligible(BOOK.get(3), at(2400, 8, 100)));
    }
}
