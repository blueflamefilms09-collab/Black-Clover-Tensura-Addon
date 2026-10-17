package com.newuniverse.nusmp.arc.zagred;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the move table decides what he does at each act and distance, and a played-out fight through the framework's scheduler proves the shape the docs promise. */
class ZagredTableTest {
    static final List<ZagredTable.Spec> SPECS = ZagredTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertEquals(14, SPECS.size(), "fourteen moves");
        Set<String> ids = new HashSet<>();
        for (ZagredTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() >= 20 && s.minPower() <= 80, s.id() + " is available before the Gates");
            assertTrue(s.windUpTicks() >= 16, s.id() + " has a telegraph of at least sixteen ticks");
            assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 12, s.id() + " keeps twelve ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 6, "a beat to breathe after every move");
            assertTrue(s.manaCost() > 0);
            assertEquals(s.kind() == ZagredTable.Kind.WORD, s.word() != null, s.id() + ": exactly the Words carry a word");
            assertEquals(s.isWord(), s.word() != null);
        }
        assertNull(ZagredTable.byId("nope"));
        assertSame(SPECS.get(0), ZagredTable.byId(ZagredTable.CLAW));
        assertEquals(ZagredTable.OVERWRITE, SPECS.get(13).id(), "the ultimate is last (the client reads the casting move by index)");
    }

    @Test
    void theMixIsMeleeWordsSummonArtsAndOneUltimate() {
        assertEquals(1, SPECS.stream().filter(ZagredTable.Spec::ultimate).count());
        ZagredTable.Spec ult = ZagredTable.byId(ZagredTable.OVERWRITE);
        assertTrue(ult.ultimate() && ult.minPower() == 80, "only from 80%");
        assertTrue(Math.round(ult.windUpTicks() * 0.75f) >= 30, "a ritual, never a flick, even at the Gates");
        assertTrue(ult.recovery() >= 40, "he is exposed afterwards");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(SPECS.stream().anyMatch(s -> s.kind() == ZagredTable.Kind.MELEE), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.kind() == ZagredTable.Kind.SUMMON), "summon");
        assertTrue(SPECS.stream().anyMatch(s -> s.kind() == ZagredTable.Kind.ARTS), "borrowed art");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 30), "ranged");
        assertTrue(SPECS.stream().anyMatch(ZagredTable.Spec::crowd), "crowd moves");
        long words = SPECS.stream().filter(ZagredTable.Spec::isWord).count();
        assertEquals(11, words, "eleven spoken Words");
        // every Word of the table is a different word
        Set<ZagredWord> distinct = new HashSet<>();
        for (ZagredTable.Spec s : SPECS) if (s.word() != null) assertTrue(distinct.add(s.word()), s.id());
        assertFalse(distinct.contains(ZagredWord.COWER), "Cower is only ever a verdict");
    }

    @Test
    void thePowerStepsUnlockTheActsInOrder() {
        for (String id : new String[]{ZagredTable.CLAW, ZagredTable.HALT, ZagredTable.FALL, ZagredTable.TRIDENT, ZagredTable.REDACT, ZagredTable.ARTS})
            assertEquals(20, ZagredTable.byId(id).minPower(), id + " is an opening move");
        for (String id : new String[]{ZagredTable.SWORDS, ZagredTable.SEAL, ZagredTable.SHATTER, ZagredTable.BANISH, ZagredTable.PAGES})
            assertEquals(40, ZagredTable.byId(id).minPower(), id + " comes in act II");
        assertEquals(60, ZagredTable.byId(ZagredTable.REVERSE).minPower());
        assertEquals(60, ZagredTable.byId(ZagredTable.VERDICT).minPower(), "the Judge brings the verdict");
        assertEquals(80, ZagredTable.byId(ZagredTable.OVERWRITE).minPower(), "the Author brings the ultimate");
    }

    @Test
    void hardControlIsLimitedToTheBindingWords() {
        for (ZagredTable.Spec s : SPECS) {
            boolean expected = s.id().equals(ZagredTable.HALT) || s.id().equals(ZagredTable.SEAL);
            assertEquals(expected, s.hardControl(), s.id());
        }
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 34; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange() && s.kind() != ZagredTable.Kind.SUMMON),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (ZagredTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(ZagredTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.5 && all < PowerCurve.tuning(20).manaCap() * 3, "the opening pool pays for a few moves, not all at once: " + all);
    }

    /** Plays a fight: a decision every 5 ticks, the target wandering between 2 and 36 blocks. Returns how often each move was used. */
    static Map<String, Integer> play(long seed, int power, int ticks) {
        MoveScheduler sch = new MoveScheduler(seed);
        sch.reset(0);
        Map<String, Integer> used = new HashMap<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(power);
        long busyUntil = 0;
        String prev = "";
        int run = 0;
        for (long t = 20; t < ticks; t += 5) {
            if (t < busyUntil) continue;
            double dist = 2 + 17 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 2, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            ZagredTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, (int) Math.max(10, Math.round(m.cooldownTicks() * tu.cooldownMultiplier())));
            used.merge(m.id(), 1, Integer::sum);
            busyUntil = t + Math.max(8, Math.round(m.windUpTicks() * tu.windUpMultiplier())) + m.recovery();
            if (m.id().equals(prev)) run++;
            else { prev = m.id(); run = 1; }
            assertTrue(m.ultimate() || run <= MoveScheduler.REPEAT_LIMIT, "no move spammed: " + m.id());
        }
        return used;
    }

    @Test
    void atTheOpeningHeUsesOnlyTheOpeningMovesAndNeverTheUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000);
            for (String id : used.keySet()) assertTrue(ZagredTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(ZagredTable.OVERWRITE));
            assertNull(used.get(ZagredTable.VERDICT), "the verdict waits for 60%");
            assertTrue(used.size() >= 5, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void overwriteNeedsEightyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 79, 20000).get(ZagredTable.OVERWRITE));
            assertNotNull(play(seed, 80, 20000).get(ZagredTable.OVERWRITE), "he uses it at 80%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(ZagredTable.byId(ZagredTable.OVERWRITE), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndTheUltimateIsSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000);
            for (ZagredTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(ZagredTable.OVERWRITE);
            assertTrue(ults >= 3, "several Overwrites in a long fight");
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void upCloseTheClawIsTheBreadAndButter() {
        Map<String, Integer> used = new HashMap<>();
        MoveScheduler sch = new MoveScheduler(11);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        for (long t = 20; t < 4000; t += 5) {
            MoveScheduler.Situation s = new MoveScheduler.Situation(3.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            ZagredTable.Spec m = SPECS.get(idx);
            sch.commit(m, s, m.cooldownTicks());
            used.merge(m.id(), 1, Integer::sum);
        }
        assertTrue(used.getOrDefault(ZagredTable.CLAW, 0) >= used.getOrDefault(ZagredTable.REDACT, 0), "close in he claws: " + used);
        assertNull(used.get(ZagredTable.TRIDENT), "no trident at 3 blocks (it needs room)");
        assertNull(used.get(ZagredTable.SEAL), "no seal at 3 blocks");
    }
}
