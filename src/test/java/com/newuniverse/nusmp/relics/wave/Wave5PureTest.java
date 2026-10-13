package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 5: the neck relics' pure rules (charge pools, the eight souls, Hemlock's tiers, the lunar record, the dragon's lethal-hit rule). */
class Wave5PureTest {
    @Test
    void chargesComeBackOnceANewDayBeginsAndNeverPastTheCap() {
        assertEquals(1, Wave5Pure.regen(1, 4, 5, 5, 4), "same day: nothing");
        assertEquals(3, Wave5Pure.regen(1, 4, 5, 6, 2), "a new day gives the roll");
        assertEquals(4, Wave5Pure.regen(3, 4, 5, 9, 4), "capped");
        assertEquals(1, Wave5Pure.regen(0, 4, 0, 1, 0), "a roll below 1 still gives 1");
        assertEquals(4, Wave5Pure.regen(9, 4, 5, 5, 1), "an over-full pool is trimmed to the cap");
        assertEquals(10, Wave5Pure.regen(9, Wave5Pure.SOULS_MAX, 0, 10, 10));
        assertEquals(0, Wave5Pure.day(-5));
        assertEquals(3, Wave5Pure.day(3 * 24000L + 5));
    }

    @Test
    void theLastChargeFreesASoulOnlyOnANaturalOne() {
        assertTrue(Wave5Pure.freesSoul(0, 1));
        assertFalse(Wave5Pure.freesSoul(0, 2));
        assertFalse(Wave5Pure.freesSoul(0, 20));
        assertFalse(Wave5Pure.freesSoul(1, 1), "only the LAST charge risks it");
        assertTrue(Wave5Pure.canAfford(3, 3));
        assertFalse(Wave5Pure.canAfford(2, 3));
        assertFalse(Wave5Pure.canAfford(5, 0));
    }

    @Test
    void theEightSoulsTakeTurnsAndFreedOnesAreSkipped() {
        assertEquals(8, Wave5Pure.SOUL_NAMES.length);
        assertEquals(8, Wave5Pure.SOUL_KINDS.length);
        assertEquals(8, Wave5Pure.SOUL_SPELLS.length);
        for (String[] s : Wave5Pure.SOUL_SPELLS) assertEquals(3, s.length);
        assertEquals(1, Wave5Pure.nextSoul(0, 0));
        assertEquals(0, Wave5Pure.nextSoul(0, 7), "wraps round");
        int mask = Wave5Pure.free(0, 1);
        assertTrue(Wave5Pure.isFreed(mask, 1));
        assertFalse(Wave5Pure.isFreed(mask, 2));
        assertEquals(2, Wave5Pure.nextSoul(mask, 0), "a freed soul is skipped");
        for (int i = 0; i < 8; i++) mask = Wave5Pure.free(mask, i);
        assertTrue(Wave5Pure.allFreed(mask));
        assertEquals(-1, Wave5Pure.nextSoul(mask, 3), "once the last is freed the ruby shatters");
        assertEquals(5, Wave5Pure.free(5, 99), "an unknown soul changes nothing");
        assertFalse(Wave5Pure.allFreed(Wave5Pure.free(0, 3)));
    }

    @Test
    void hemlocksAmuletGrowsWithTheEnergyItDrinks() {
        assertEquals(0, Wave5Pure.tier(0));
        assertEquals(0, Wave5Pure.tier(Wave5Pure.AWAKEN_AT - 1));
        assertEquals(1, Wave5Pure.tier(Wave5Pure.AWAKEN_AT));
        assertEquals(1, Wave5Pure.tier(Wave5Pure.EXALT_AT - 1));
        assertEquals(2, Wave5Pure.tier(Wave5Pure.EXALT_AT));
        assertEquals(Wave5Pure.ENERGY_CAP, Wave5Pure.addEnergy(990, 50));
        assertEquals(10, Wave5Pure.addEnergy(10, -4), "it never loses energy");
        assertTrue(Wave5Pure.veilTicks(2) > Wave5Pure.veilTicks(1) && Wave5Pure.veilTicks(1) > Wave5Pure.veilTicks(0));
        assertTrue(Wave5Pure.healthBonus(2) > Wave5Pure.healthBonus(1) && Wave5Pure.healthBonus(0) == 0.0);
        assertTrue(Wave5Pure.toughnessBonus(2) > Wave5Pure.toughnessBonus(1));
        assertEquals(0.0, Wave5Pure.soulBonus(0), 1e-9);
        assertTrue(Wave5Pure.soulBonus(2) > Wave5Pure.soulBonus(1) && Wave5Pure.soulBonus(1) > 0.0, "the Reliquary holds more of the soul");
    }

    @Test
    void theFullMoonComesOncePerEightDaysAndTheRecordOncePerMonth() {
        long fullDay = 8 * 24000L;                       // day 8: phase 0
        assertEquals(0, Wave5Pure.moonPhase(fullDay));
        assertEquals(3, Wave5Pure.moonPhase(3 * 24000L + 100));
        assertTrue(Wave5Pure.fullMoonNight(fullDay + 14000));
        assertFalse(Wave5Pure.fullMoonNight(fullDay + 6000), "noon of the full-moon day is not night");
        assertFalse(Wave5Pure.fullMoonNight(fullDay + 24000 + 14000), "the next night has another phase");
        assertFalse(Wave5Pure.fullMoonNight(fullDay + 23500), "the dawn is not the dark hours");
        assertEquals(1, Wave5Pure.cycle(fullDay));
        assertEquals(0, Wave5Pure.cycle(7 * 24000L));
        assertTrue(Wave5Pure.canRecord(fullDay + 14000, -1, 0));
        assertFalse(Wave5Pure.canRecord(fullDay + 14000, 1, 0), "already recorded this month");
        assertFalse(Wave5Pure.canRecord(fullDay + 14000, -1, 3), "still mending");
        assertFalse(Wave5Pure.canRecord(16 * 24000L + 14000, 1, 3), "one month later it is still mending");
        assertTrue(Wave5Pure.canRecord(24 * 24000L + 14000, 1, 3), "two months later");
        assertEquals(3, Wave5Pure.mendsAt(fullDay + 14000));
        assertTrue(Wave5Pure.heldStill(1, 0, 1));
        assertFalse(Wave5Pure.heldStill(2, 0, 2));
    }

    @Test
    void theSurvivorKeepsOnlyWhatTheRecordHeldAndNothingIsEverAdded() {
        Map<String, Integer> rec = new HashMap<>();
        rec.put("minecraft:iron_sword", 1);
        rec.put("minecraft:cobblestone", 64);
        String[] ids = {"minecraft:iron_sword", "minecraft:cobblestone", "minecraft:cobblestone", "minecraft:diamond", "nusmp:relic", null};
        int[] counts = {1, 64, 40, 3, 1, 0};
        boolean[] exempt = {false, false, false, false, true, false};
        int[] out = Wave5Pure.excess(ids, counts, exempt, rec);
        assertArrayEquals(new int[]{0, 0, 40, 3, 0, 0}, out, "the second stack of cobblestone and the diamonds were not on the record; a relic never is dropped");
        assertEquals(64, rec.get("minecraft:cobblestone"), "the record itself is not consumed");
        int[] none = Wave5Pure.excess(ids, counts, exempt, new HashMap<>());
        assertEquals(1, none[0]);
        assertEquals(0, none[4]);
        int total = 0;
        for (int i = 0; i < ids.length; i++) { assertTrue(out[i] >= 0 && out[i] <= counts[i]); total += out[i]; }
        assertEquals(43, total);
    }

    @Test
    void theDragonsPoolTakesHitsAndTheRemainderIsTheRule() {
        assertEquals(Wave5Pure.Lethal.NONE, Wave5Pure.dragonHit(10, 40, 20, 40));
        assertEquals(Wave5Pure.Lethal.NONE, Wave5Pure.dragonHit(40, 40, 20, 40), "exactly the pool leaves nothing over");
        assertEquals(Wave5Pure.Lethal.REVERT, Wave5Pure.dragonHit(45, 40, 20, 40), "5 over: the form breaks, 5 is taken");
        assertEquals(Wave5Pure.Lethal.UNCONSCIOUS, Wave5Pure.dragonHit(65, 40, 20, 40), "25 over would take 20 HP: left at 1 HP");
        assertEquals(Wave5Pure.Lethal.DIE, Wave5Pure.dragonHit(95, 40, 20, 40), "55 over is more than the dragon's whole pool: instant death");
        assertEquals(Wave5Pure.Lethal.UNCONSCIOUS, Wave5Pure.dragonHit(30, 0, 20, 40), "an empty pool and a lethal blow");
        assertEquals(1.0f, 20f - Wave5Pure.leaveOneHp(0, 20) , 1e-6);
        assertEquals(0.0f, Wave5Pure.leaveOneHp(0, 0.5f), 1e-6);
        assertEquals(34.0f, Wave5Pure.leaveOneHp(15, 20), 1e-6);
    }

    @Test
    void huntressAndCantripNumbersStayInsideTheFairnessCap() {
        assertEquals(1.0f, Wave5Pure.huntressBonus(false, false), 1e-6);
        assertEquals(2.0f, Wave5Pure.huntressBonus(true, false), 1e-6, "doubled for claws and teeth");
        assertEquals(3.0f, Wave5Pure.huntressBonus(false, true), 1e-6);
        assertEquals(4.0f, Wave5Pure.cantrip(4, 0), 1e-6);
        assertEquals(6.0f, Wave5Pure.cantrip(5, 30), 1e-6, "capped at 6");
        assertEquals(6.0f, Wave5Pure.cap(99), 1e-6);
        assertEquals(0.0f, Wave5Pure.cap(-3), 1e-6);
        for (int lv = 0; lv < 200; lv += 7) assertTrue(Wave5Pure.cantrip(5, lv) <= Wave5Pure.DAMAGE_CAP);
    }
}
