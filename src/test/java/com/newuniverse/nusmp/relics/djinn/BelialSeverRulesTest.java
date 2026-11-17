package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the canon Belial's sever and exile (owner notes): caps per hit and per second, bosses cut down, exile bounds, who is refused. */
class BelialSeverRulesTest {
    private static final BelialSeverRules.Kind MOB = BelialSeverRules.Kind.MOB, PLAYER = BelialSeverRules.Kind.PLAYER, BOSS = BelialSeverRules.Kind.BOSS;

    @Test
    void aSingleHitNeverPassesItsCap() {
        for (double max : new double[]{1, 20, 300, 5000, 1.0E6}) {
            for (BelialSeverRules.Kind k : BelialSeverRules.Kind.values()) {
                double hit = BelialSeverRules.hit(k, 1.0E9, max, 1.0);
                assertTrue(hit <= BelialSeverRules.hitCap(k, max) + 1.0E-9, k + " at " + max);
                assertTrue(hit <= BelialSeverRules.HARD_HIT_CAP + 1.0E-9, "the hard cap holds for " + k);
            }
        }
    }

    @Test
    void aBossTakesFarLessThanAMobOrAPlayer() {
        double max = 2000, power = 40;
        double mob = BelialSeverRules.hit(MOB, power, max, BelialSeverRules.ACT_FRAC);
        double player = BelialSeverRules.hit(PLAYER, power, max, BelialSeverRules.ACT_FRAC);
        double boss = BelialSeverRules.hit(BOSS, power, max, BelialSeverRules.ACT_FRAC);
        assertTrue(boss < player && player < mob, "boss " + boss + " < player " + player + " < mob " + mob);
        assertTrue(boss <= BelialSeverRules.BOSS_HIT_SHARE * max + 1.0E-9, "a boss loses at most 3% of its health per hit");
    }

    @Test
    void theSecondWindowCapsTheSumOfManyHits() {
        for (BelialSeverRules.Kind k : BelialSeverRules.Kind.values()) {
            double max = 1000;
            double cap = BelialSeverRules.secondCap(k, max);
            BelialSeverRules.Window w = new BelialSeverRules.Window();
            double sum = 0;
            for (int i = 0; i < 40; i++) sum += w.grant(100 + i / 4, 50.0, cap);            // 40 hits inside 10 ticks
            assertEquals(cap, sum, 1.0E-6, k + ": the budget is spent, not exceeded");
        }
        double bossSecond = BelialSeverRules.secondCap(BOSS, 1000);
        assertEquals(1000 * BelialSeverRules.BOSS_SECOND_SHARE, bossSecond, 1.0E-9, "a boss: 6% per second, no floor");
    }

    @Test
    void theWindowOpensAgainAfterASecond() {
        BelialSeverRules.Window w = new BelialSeverRules.Window();
        assertEquals(10.0, w.grant(0, 10.0, 10.0), 1.0E-9);
        assertEquals(0.0, w.grant(5, 10.0, 10.0), 1.0E-9, "spent");
        assertEquals(10.0, w.grant(BelialSeverRules.WINDOW_TICKS, 10.0, 10.0), 1.0E-9, "a new second");
        assertEquals(10.0, w.grant(3, 10.0, 10.0), 1.0E-9, "a clock that went back opens a fresh window instead of locking the target");
    }

    @Test
    void theWindowTableIsSweptAndBounded() {
        Map<UUID, BelialSeverRules.Window> map = BelialSeverRules.newWindows();
        for (int i = 0; i < 10; i++) map.computeIfAbsent(UUID.randomUUID(), k -> new BelialSeverRules.Window()).grant(0, 1, 5);
        BelialSeverRules.sweep(map, 5);
        assertEquals(10, map.size(), "young windows stay");
        BelialSeverRules.sweep(map, 1000);
        assertTrue(map.isEmpty(), "old windows go");
        for (int i = 0; i < BelialSeverRules.MAX_WINDOWS + 5; i++) map.computeIfAbsent(UUID.randomUUID(), k -> new BelialSeverRules.Window()).grant(2000, 1, 5);
        BelialSeverRules.sweep(map, 2001);
        assertTrue(map.size() <= BelialSeverRules.MAX_WINDOWS, "an oversized table is cleared");
    }

    @Test
    void exileLastsThreeToSixSecondsAndAPlayerAtMostFour() {
        for (boolean player : new boolean[]{false, true})
            for (boolean full : new boolean[]{false, true})
                for (boolean dark : new boolean[]{false, true}) {
                    int t = BelialSeverRules.exileTicks(player, full, dark);
                    assertTrue(t >= 60 && t <= 120, "3 to 6 s: " + t);
                    if (player) assertTrue(t <= 80, "a player: at most 4 s: " + t);
                }
        assertEquals(60, BelialSeverRules.exileTicks(false, false, false));
    }

    @Test
    void exileRefusals() {
        BelialSeverRules.Refusal ok = BelialSeverRules.canExile(true, MOB, false, false, false, false, 0, 0);
        assertEquals(BelialSeverRules.Refusal.OK, ok);
        assertEquals(BelialSeverRules.Refusal.DISABLED, BelialSeverRules.canExile(false, MOB, false, false, false, false, 0, 0));
        assertEquals(BelialSeverRules.Refusal.BOSS, BelialSeverRules.canExile(true, BOSS, false, false, false, false, 0, 0), "never a boss");
        assertEquals(BelialSeverRules.Refusal.CREATIVE, BelialSeverRules.canExile(true, PLAYER, true, false, false, false, 0, 0));
        assertEquals(BelialSeverRules.Refusal.VETO, BelialSeverRules.canExile(true, PLAYER, false, true, false, false, 0, 0), "a safe area or boss fight vetoes");
        assertEquals(BelialSeverRules.Refusal.ALREADY, BelialSeverRules.canExile(true, MOB, false, false, false, true, 0, 0));
        assertEquals(BelialSeverRules.Refusal.IMMUNE, BelialSeverRules.canExile(true, MOB, false, false, true, false, 0, 0));
        assertEquals(BelialSeverRules.Refusal.CASTER_CAP, BelialSeverRules.canExile(true, MOB, false, false, false, false, BelialSeverRules.MAX_PER_CASTER, 0));
        assertEquals(BelialSeverRules.Refusal.GLOBAL_CAP, BelialSeverRules.canExile(true, MOB, false, false, false, false, 0, BelialSeverRules.MAX_GLOBAL));
    }

    @Test
    void theImmuneTableIsSweptAndBounded() {
        Map<UUID, Long> immune = new HashMap<>();
        immune.put(UUID.randomUUID(), 50L);
        immune.put(UUID.randomUUID(), 500L);
        BelialSeverRules.sweepImmune(immune, 100);
        assertEquals(1, immune.size());
        for (int i = 0; i < BelialSeverRules.MAX_IMMUNE + 5; i++) immune.put(UUID.randomUUID(), 900L);
        BelialSeverRules.sweepImmune(immune, 100);
        assertTrue(immune.size() <= BelialSeverRules.MAX_IMMUNE);
    }

    @Test
    void thePerCastCostIsBounded() {
        assertTrue(BelialSeverRules.MAX_VICTIMS <= 3);
        assertTrue(BelialSeverRules.MAX_VICTIMS <= BelialSeverRules.MAX_PER_CASTER);
        assertTrue(BelialSeverRules.MAX_PER_CASTER <= BelialSeverRules.MAX_GLOBAL);
        assertTrue(BelialSeverRules.MAX_GLOBAL <= 32, "the global exile count stays small");
    }
}
