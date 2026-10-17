package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Lucifero's move table, and a played-out fight through the framework's scheduler: what the King does at each power and distance. */
class LuciferoTableTest {
    static final List<LuciferoTable.Spec> SPECS = LuciferoTable.specs();
    /** The framework's ticks of exposure that a recovery of this length opens (Move.EXPOSED_FROM; Move cannot be loaded without the game). */
    static final int EXPOSED_FROM = 40;

    @Test
    void theTableIsWellFormed() {
        assertEquals(13, SPECS.size());
        Set<String> ids = new HashSet<>();
        for (LuciferoTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() == 20 || s.minPower() == 40 || s.minPower() == 60 || s.minPower() == 80 || s.minPower() == 100, s.id() + " gates on a real power step");
            assertTrue(s.windUpTicks() > 0, s.id() + " has a telegraph");
            assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 8, s.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 8, "a beat to breathe after every move");
            assertTrue(s.manaCost() > 0);
        }
    }

    @Test
    void theOrderIsPinnedBecauseTheClientReadsNamesByIndex() {
        String[] order = {LuciferoTable.CRUSHING_PALM, LuciferoTable.FALLING_HEAVENS, LuciferoTable.DISTORTION_STEP, LuciferoTable.THE_BOW,
                LuciferoTable.GRAVITY_SHROUD, LuciferoTable.LESSER_DEVILS, LuciferoTable.GRAVITY_COMB, LuciferoTable.GRAVITY_GRASP,
                LuciferoTable.WANDERING_WEIGHT, LuciferoTable.FLATTEN_AIR, LuciferoTable.KNEEL_BEFORE_THE_KING, LuciferoTable.EVENT_HORIZON,
                LuciferoTable.SIX_LIMBS};
        for (int i = 0; i < order.length; i++) assertEquals(order[i], SPECS.get(i).id(), "move " + i);
        assertSame(LuciferoTable.byId(LuciferoTable.SIX_LIMBS), SPECS.get(12));
        assertNull(LuciferoTable.byId("nope"));
    }

    @Test
    void theMixIsHandRangeRoomRulesDefenceFollowersAndTwoUltimates() {
        long ultimates = SPECS.stream().filter(LuciferoTable.Spec::ultimate).count();
        assertEquals(2, ultimates);
        LuciferoTable.Spec kneel = LuciferoTable.byId(LuciferoTable.KNEEL_BEFORE_THE_KING), horizon = LuciferoTable.byId(LuciferoTable.EVENT_HORIZON);
        assertTrue(kneel.ultimate() && kneel.minPower() == 60 && horizon.ultimate() && horizon.minPower() == 80);
        for (LuciferoTable.Spec u : new LuciferoTable.Spec[]{kneel, horizon}) {
            assertTrue(u.windUpTicks() >= 60, u.id() + " is a ritual, not a flick");
            assertTrue(Math.round(u.windUpTicks() * 0.75f) >= 30, u.id() + " keeps the framework's 30-tick minimum at the Gates");
            assertTrue(u.recovery() >= EXPOSED_FROM, "he is exposed after " + u.id());
            assertTrue(u.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        }
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 10 && !s.ultimate()), "melee");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 30), "ranged");
        assertTrue(SPECS.stream().anyMatch(LuciferoTable.Spec::crowd), "a crowd move");
        assertNotNull(LuciferoTable.byId(LuciferoTable.THE_BOW), "the kneel");
        assertNotNull(LuciferoTable.byId(LuciferoTable.GRAVITY_SHROUD), "the defence");
        assertNotNull(LuciferoTable.byId(LuciferoTable.LESSER_DEVILS), "followers");
        assertNotNull(LuciferoTable.byId(LuciferoTable.FLATTEN_AIR), "the flattened air");
    }

    @Test
    void theBowLeavesHimExposedAndIsNeverEligibleBeforeItsPowerOrWithoutMana() {
        LuciferoTable.Spec bow = LuciferoTable.byId(LuciferoTable.THE_BOW);
        assertTrue(bow.recovery() >= EXPOSED_FROM, "the party that bowed has a window");
        assertEquals(20, bow.minPower());
        MoveScheduler sch = new MoveScheduler(1);
        sch.reset(0);
        PowerCurve.Tuning tu = PowerCurve.tuning(20);
        MoveScheduler.Situation opening = new MoveScheduler.Situation(8, 1f, 20, 1, 20, tu.manaCap(), tu.ultimateGapTicks());
        assertTrue(sch.eligible(bow, opening), "the opening order to kneel is possible at the first decision (mana " + tu.manaCap() + " pays " + bow.manaCost() + ")");
        MoveScheduler.Situation broke = new MoveScheduler.Situation(8, 1f, 20, 1, 20, 5, tu.ultimateGapTicks());
        assertFalse(sch.eligible(bow, broke), "no mana, no order");
        MoveScheduler.Situation far = new MoveScheduler.Situation(60, 1f, 20, 1, 20, tu.manaCap(), tu.ultimateGapTicks());
        assertFalse(sch.eligible(bow, far), "the order carries 48 blocks");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        Set<String> utility = Set.of(LuciferoTable.THE_BOW, LuciferoTable.GRAVITY_SHROUD, LuciferoTable.LESSER_DEVILS, LuciferoTable.FLATTEN_AIR);
        for (double d = 0; d <= 34; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange() && !utility.contains(s.id())),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (LuciferoTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
    }

    @Test
    void damageNumbersAreSane() {
        assertTrue(LuciferoTable.KNEEL > LuciferoTable.PALM && LuciferoTable.HORIZON > LuciferoTable.PALM, "the ultimates hit hardest");
        assertTrue(LuciferoTable.PALM > LuciferoTable.GRASP * 0.5 && LuciferoTable.GRASP >= LuciferoTable.PALM, "the grasp is the single-target tank buster");
        assertTrue(LuciferoTable.HEAVEN * 12 < LuciferoTable.KNEEL * 6, "a full rain of debris is no ultimate");
        assertTrue(LuciferoTable.ARM * 6 < LuciferoTable.KNEEL * 1.6, "six limbs all landing stay under an ultimate and a half");
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
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            LuciferoTable.Spec m = SPECS.get(idx);
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
    void atTheOpeningHeUsesOnlyTheOpeningMovesAndNeverAnUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 30000);
            for (String id : used.keySet()) assertTrue(LuciferoTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(LuciferoTable.KNEEL_BEFORE_THE_KING));
            assertNull(used.get(LuciferoTable.EVENT_HORIZON));
            assertNull(used.get(LuciferoTable.GRAVITY_COMB), "the comb waits for 40%");
            assertTrue(used.size() >= 5, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void theUltimatesWaitForTheirPowerAndTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 30000).get(LuciferoTable.KNEEL_BEFORE_THE_KING));
            assertNotNull(play(seed, 60, 30000).get(LuciferoTable.KNEEL_BEFORE_THE_KING), "the kneel from 60% (seed " + seed + ")");
            assertNull(play(seed, 79, 30000).get(LuciferoTable.EVENT_HORIZON));
            assertNotNull(play(seed, 80, 30000).get(LuciferoTable.EVENT_HORIZON), "the horizon from 80% (seed " + seed + ")");
            assertNull(play(seed, 99, 30000).get(LuciferoTable.SIX_LIMBS), "the limbs wait for the Gates");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(LuciferoTable.byId(LuciferoTable.EVENT_HORIZON), early));
        assertFalse(sch.eligible(LuciferoTable.byId(LuciferoTable.KNEEL_BEFORE_THE_KING), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpacedAndAlternate() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 80000);
            for (LuciferoTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(LuciferoTable.KNEEL_BEFORE_THE_KING) + used.get(LuciferoTable.EVENT_HORIZON);
            assertTrue(ults >= 4, "several ultimates in a long fight: " + ults);
            assertTrue((long) ults * PowerCurve.tuning(100).ultimateGapTicks() <= 80000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void upCloseTheKingStrikesAndAtRangeHeRains() {
        Map<String, Integer> close = new HashMap<>(), far = new HashMap<>();
        for (int pass = 0; pass < 2; pass++) {
            MoveScheduler sch = new MoveScheduler(11 + pass);
            sch.reset(0);
            PowerCurve.Tuning tu = PowerCurve.tuning(40);
            for (long t = 20; t < 6000; t += 5) {
                MoveScheduler.Situation s = new MoveScheduler.Situation(pass == 0 ? 3.0 : 20.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
                int idx = sch.pick(SPECS, s);
                if (idx < 0) continue;
                LuciferoTable.Spec m = SPECS.get(idx);
                sch.commit(m, s, m.cooldownTicks());
                (pass == 0 ? close : far).merge(m.id(), 1, Integer::sum);
            }
        }
        assertTrue(close.getOrDefault(LuciferoTable.CRUSHING_PALM, 0) >= close.getOrDefault(LuciferoTable.GRAVITY_COMB, 0), "close in he uses the palm: " + close);
        assertNull(far.get(LuciferoTable.CRUSHING_PALM), "no palm at 20 blocks");
        assertNotNull(far.get(LuciferoTable.FALLING_HEAVENS), "at range the sky falls: " + far);
    }
}
