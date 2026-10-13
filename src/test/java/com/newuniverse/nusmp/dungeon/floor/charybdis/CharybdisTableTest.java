package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Charybdis Spawn's move table, and played-out fights through the scheduler: what it does at each power and distance. */
class CharybdisTableTest {
    static final List<CharybdisTable.Spec> SPECS = CharybdisTable.specs();

    @Test
    void theTableIsWellFormed() {
        assertTrue(SPECS.size() >= 6 && SPECS.size() <= 8, "six to eight moves besides the plain strike: " + SPECS.size());
        Set<String> ids = new HashSet<>();
        for (CharybdisTable.Spec s : SPECS) {
            assertTrue(ids.add(s.id()), "unique id " + s.id());
            assertFalse(s.label().isBlank());
            assertTrue(s.minPower() >= 20 && s.minPower() <= 80, s.id() + " is available before the Gates");
            assertTrue(s.windUpTicks() > 0, s.id() + " has a telegraph");
            assertTrue(Math.round(s.windUpTicks() * 0.75f) >= 8, s.id() + " keeps 8 ticks of tell even at the Gates");
            assertTrue(s.cooldownTicks() >= 60 && s.weight() >= 1);
            assertTrue(s.minRange() >= 0 && s.maxRange() > s.minRange());
            assertTrue(s.recovery() >= 8, "a beat to breathe after every move");
            assertTrue(s.manaCost() > 0);
        }
        assertSame(CharybdisTable.byId(CharybdisTable.MAELSTROM), SPECS.get(SPECS.size() - 1), "the ultimate is last");
        assertNull(CharybdisTable.byId("nope"));
        for (String id : new String[]{CharybdisTable.SCALE_VOLLEY, CharybdisTable.TENTACLE_SWEEP, CharybdisTable.WHIRLPOOL, CharybdisTable.BEAK_LUNGE,
                CharybdisTable.SUMMON, CharybdisTable.TIDAL_CRUSH, CharybdisTable.MAELSTROM})
            assertNotNull(CharybdisTable.byId(id), id);
    }

    @Test
    void theMixIsScalesMegalodonsWaterAndOneUltimate() {
        assertEquals(1, SPECS.stream().filter(CharybdisTable.Spec::ultimate).count());
        CharybdisTable.Spec ult = CharybdisTable.byId(CharybdisTable.MAELSTROM);
        assertTrue(ult.ultimate() && ult.minPower() >= 60, "only usable at 60% and above");
        assertTrue(ult.windUpTicks() >= 40, "a ritual, not a flick");
        assertTrue(ult.recovery() >= 40, "it is exposed afterwards: 3 s");
        assertTrue(ult.cooldownTicks() >= PowerCurve.tuning(100).ultimateGapTicks());
        assertTrue(SPECS.stream().anyMatch(s -> s.maxRange() <= 8), "melee: the sweep");
        assertTrue(SPECS.stream().anyMatch(s -> s.minRange() >= 5 && s.maxRange() >= 26), "ranged: the scales");
        assertTrue(SPECS.stream().anyMatch(CharybdisTable.Spec::crowd), "crowd moves");
        assertEquals(CharybdisTable.SCALE_VOLLEY, SPECS.get(0).id(), "the scales are the signature and come first");
    }

    @Test
    void onlyTheUltimateLeavesItExposed() {
        for (CharybdisTable.Spec s : SPECS) if (!s.ultimate()) assertTrue(s.recovery() < 40, s.id() + " must not expose it (recovery under 40 ticks)");
    }

    @Test
    void thereIsNoDeadRangeAtTheOpening() {
        for (double d = 0; d <= 40; d += 0.5) {
            double dist = d;
            assertTrue(SPECS.stream().anyMatch(s -> !s.ultimate() && s.minPower() <= 20 && dist >= s.minRange() && dist <= s.maxRange()
                            && !s.id().equals(CharybdisTable.SUMMON)),
                    "an attack to throw at " + d + " blocks at 20%");
        }
    }

    @Test
    void manaMatchesTheFramework() {
        for (CharybdisTable.Spec s : SPECS) {
            if (s.ultimate()) assertEquals(60, s.manaCost());
            else assertEquals(Math.min(32, 6 + s.cooldownTicks() / 16.0), s.manaCost(), 1e-9);
        }
        double all = SPECS.stream().filter(s -> !s.ultimate() && s.minPower() <= 20).mapToDouble(CharybdisTable.Spec::manaCost).sum();
        assertTrue(all > PowerCurve.tuning(20).manaCap() * 0.3 && all < PowerCurve.tuning(20).manaCap() * 3, "the opening pool pays for a few moves, not for everything at once: " + all);
    }

    @Test
    void theBuildIsABigSlowToughLeviathan() {
        assertEquals(1100, CharybdisTable.HEALTH);
        assertEquals(6, CharybdisTable.ARMOR);
        assertEquals(4, CharybdisTable.TOUGHNESS);
        assertEquals(0.30, CharybdisTable.SPEED, 1e-9);
        assertEquals(24, CharybdisTable.ATTACK);
        assertTrue(CharybdisTable.WIDTH >= 3 && CharybdisTable.WIDTH <= 4 && CharybdisTable.HEIGHT >= 2.5f && CharybdisTable.HEIGHT <= 3.5f);
        assertTrue(CharybdisTable.DAMAGE_SCALE >= 1f && CharybdisTable.DAMAGE_SCALE <= 1.25f);
        assertTrue(CharybdisTable.MELEE_FACTOR > 0.5f && CharybdisTable.MELEE_FACTOR <= 1f);
    }

    @Test
    void damageNumbersAreBoundedAndTheCrushIsTheHeaviestBlow() {
        assertTrue(CharybdisTable.CRUSH > CharybdisTable.LUNGE && CharybdisTable.LUNGE > CharybdisTable.SWEEP && CharybdisTable.SWEEP > CharybdisTable.SCALE);
        assertTrue(CharybdisTable.SCALE <= 8, "each scale is small");
        float worstFan = CharybdisTable.SCALE * CharybdisTable.HITS_PER_VICTIM;
        assertTrue(worstFan < CharybdisTable.CRUSH, "a whole fan on one victim is less than the crush: " + worstFan);
        assertTrue(worstFan * 2 < CharybdisTable.CRUSH * 1.5f, "even the Maelstrom's two fans are not a one-shot: " + worstFan * 2);
    }

    @Test
    void theFanGrowsWithPowerFromFiveToNine() {
        assertEquals(5, CharybdisTable.fanCount(20));
        assertEquals(5, CharybdisTable.fanCount(40));
        assertEquals(5, CharybdisTable.fanCount(59));
        assertEquals(7, CharybdisTable.fanCount(60));
        assertEquals(7, CharybdisTable.fanCount(80));
        assertEquals(9, CharybdisTable.fanCount(100));
        int last = 0;
        for (int p = 0; p <= 100; p++) {
            int n = CharybdisTable.fanCount(p);
            assertTrue(n >= 5 && n <= 9 && n >= last, "never fewer with more power: " + p);
            last = n;
        }
        assertEquals(0, CharybdisTable.slowLevel(80));
        assertEquals(1, CharybdisTable.slowLevel(100));
        assertTrue(CharybdisTable.slowTicks(100) >= CharybdisTable.slowTicks(20) && CharybdisTable.slowTicks(20) >= 40);
    }

    @Test
    void theScalesAreSlowEnoughToReadAndFadeAfterSixSeconds() {
        assertEquals(0.5, CharybdisTable.SCALE_SPEED, 1e-9);
        assertEquals(120, CharybdisTable.SCALE_LIFE);
        assertTrue(CharybdisTable.SCALE_SPEED * 20 <= 12, "no faster than twelve blocks a second");
        assertTrue(CharybdisTable.HITS_PER_VICTIM <= 2);
    }

    @Test
    void theSurfacingIsThreeSecondsOfBubblesThenThreeSecondsOfRise() {
        assertEquals(60, CharybdisTable.BUBBLE_TICKS);
        assertEquals(60, CharybdisTable.RISE_TICKS);
        assertEquals(6.0, CharybdisTable.RISE_BLOCKS, 1e-9);
        assertEquals(4, CharybdisTable.MINION_CAP);
        assertEquals(2, CharybdisTable.MINIONS_PER_CAST);
    }

    /** Plays a fight: a decision every 5 ticks, the target wandering between 2 and 38 blocks. Returns how often each move was used. */
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
            double dist = 2 + 18 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, power, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(SPECS, s);
            if (idx < 0) continue;
            CharybdisTable.Spec m = SPECS.get(idx);
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
    void atTheOpeningItUsesOnlyTheOpeningMovesAndNeverTheUltimate() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<String, Integer> used = play(seed, 20, 20000);
            for (String id : used.keySet()) assertTrue(CharybdisTable.byId(id).minPower() <= 20, id + " is not an opening move");
            assertNull(used.get(CharybdisTable.MAELSTROM));
            assertNull(used.get(CharybdisTable.SUMMON), "the megalodons wait for 40%");
            assertNull(used.get(CharybdisTable.TIDAL_CRUSH), "the crush waits for 40%");
            assertNull(used.get(CharybdisTable.BEAK_LUNGE), "the lunge waits for 40%");
            assertTrue(used.getOrDefault(CharybdisTable.SCALE_VOLLEY, 0) > 0, "the scales are there from the start: " + used);
            assertTrue(used.size() >= 3, "variety at 20%: " + used.keySet());
        }
    }

    @Test
    void theMaelstromNeedsSixtyPercentAndComesAfterTheOpeningDelay() {
        for (long seed = 1; seed <= 5; seed++) {
            assertNull(play(seed, 59, 20000).get(CharybdisTable.MAELSTROM));
            assertNotNull(play(seed, 60, 20000).get(CharybdisTable.MAELSTROM), "it uses it at 60%");
        }
        MoveScheduler sch = new MoveScheduler(3);
        sch.reset(0);
        MoveScheduler.Situation early = new MoveScheduler.Situation(10, 0.3f, 80, 1, MoveScheduler.FIRST_ULTIMATE_DELAY - 1, Double.POSITIVE_INFINITY, 1200);
        assertFalse(sch.eligible(CharybdisTable.byId(CharybdisTable.MAELSTROM), early));
    }

    @Test
    void atTheGatesEveryMoveGetsUsedAndUltimatesAreSpaced() {
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Integer> used = play(seed, 100, 60000);
            for (CharybdisTable.Spec s : SPECS) assertNotNull(used.get(s.id()), s.id() + " is used at the Gates (seed " + seed + ")");
            int ults = used.get(CharybdisTable.MAELSTROM);
            assertTrue(ults >= 3, "several maelstroms in a long fight");
            assertTrue(ults * PowerCurve.tuning(100).ultimateGapTicks() <= 60000 + PowerCurve.tuning(100).ultimateGapTicks(), "never faster than the gap");
        }
    }

    @Test
    void upCloseItSweepsAndFarAwayItThrowsScales() {
        PowerCurve.Tuning tu = PowerCurve.tuning(40);
        Map<String, Integer> near = new HashMap<>(), far = new HashMap<>();
        MoveScheduler a = new MoveScheduler(11), b = new MoveScheduler(12);
        a.reset(0);
        b.reset(0);
        for (long t = 20; t < 6000; t += 5) {
            MoveScheduler.Situation sn = new MoveScheduler.Situation(3.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int i = a.pick(SPECS, sn);
            if (i >= 0) { a.commit(SPECS.get(i), sn, SPECS.get(i).cooldownTicks()); near.merge(SPECS.get(i).id(), 1, Integer::sum); }
            MoveScheduler.Situation sf = new MoveScheduler.Situation(32.0, 0.8f, 40, 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int j = b.pick(SPECS, sf);
            if (j >= 0) { b.commit(SPECS.get(j), sf, SPECS.get(j).cooldownTicks()); far.merge(SPECS.get(j).id(), 1, Integer::sum); }
        }
        assertTrue(near.getOrDefault(CharybdisTable.TENTACLE_SWEEP, 0) > 0, "close in it sweeps: " + near);
        assertNull(near.get(CharybdisTable.SCALE_VOLLEY), "no scales at 3 blocks");
        assertNull(near.get(CharybdisTable.BEAK_LUNGE), "no lunge at 3 blocks");
        assertTrue(far.getOrDefault(CharybdisTable.SCALE_VOLLEY, 0) > 0, "far away it fires scales: " + far);
        assertNull(far.get(CharybdisTable.TENTACLE_SWEEP), "no sweep at 32 blocks");
    }

    @Test
    void aMegalodonsBlowTakesAtMostAShareOfAPlayersHealth() {
        assertEquals(36f, CharybdisTable.capMinion(100f, 200f), 1e-4, "18% of 200");
        assertEquals(3.6f, CharybdisTable.capMinion(100f, 20f), 1e-4, "18% of an ordinary 20 health");
        assertEquals(3f, CharybdisTable.capMinion(100f, 10f), 1e-4, "never under the floor of three points");
        assertEquals(2f, CharybdisTable.capMinion(2f, 20f), 1e-4, "a smaller blow is left as it is");
        assertEquals(0f, CharybdisTable.capMinion(0f, 20f), 1e-4);
        assertEquals(0f, CharybdisTable.capMinion(-4f, 20f), 1e-4, "a negative blow is nothing");
        assertEquals(5f, CharybdisTable.capMinion(5f, 0f), 1e-4, "no health known: not touched");
    }

    @Test
    void theLeviathanWakesInsideTheWaterOfTheBasin() {
        assertTrue(CharybdisTable.SURFACE_RANGE > Swim.ORBIT_MAX && CharybdisTable.SURFACE_RANGE < CharybdisBasinPlan.R,
                "past its own orbit and inside the water: " + CharybdisTable.SURFACE_RANGE);
        assertEquals(60, CharybdisTable.BUBBLE_TICKS);
        assertEquals(60, CharybdisTable.RISE_TICKS);
    }
}
