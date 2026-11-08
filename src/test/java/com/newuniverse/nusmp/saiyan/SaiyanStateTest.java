package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: Battle Power, the daily budget, the repeat-kill memory and the cooldowns are state with rules; they are pinned here with no player. */
class SaiyanStateTest {
    @Test
    void aFreshStateIsEmptyAndStartsAtTheFirstRank() {
        SaiyanState s = new SaiyanState();
        assertTrue(s.isEmpty());
        assertEquals(SaiyanRules.Rank.LOW_CLASS, s.rank());
        s.saiyan = true;
        assertFalse(s.isEmpty());
    }

    @Test
    void theDailyBudgetHoldsAndRollsOverWithTheDay() {
        SaiyanState s = new SaiyanState();
        assertEquals(20.0, s.addBattlePower(500.0, 0, 0.10, 20), 1e-9);          // cut at the floor of 20
        assertEquals(0.0, s.addBattlePower(5.0, 100, 0.10, 20), 1e-9);            // nothing left today
        assertEquals(120.0, s.battlePower, 1e-9);
        assertTrue(s.addBattlePower(5.0, SaiyanRules.DAY_TICKS + 1, 0.10, 20) > 0.0);   // a new day
    }

    @Test
    void battlePowerNeverPassesTheHardCapOrDropsBelowTheStart() {
        SaiyanState s = new SaiyanState();
        s.setBattlePower(5.0e15);
        assertEquals(SaiyanRules.BP_HARD_CAP, s.battlePower, 1e-3);
        s.setBattlePower(-4);
        assertEquals(SaiyanRules.START_BP, s.battlePower, 1e-9);
        s.setBattlePower(SaiyanRules.BP_HARD_CAP);
        assertEquals(0.0, s.addBattlePower(10, 0, 1.0, 1.0e9), 1e-9);
    }

    @Test
    void theRepeatMemoryCountsInsideItsWindowAndStaysBounded() {
        SaiyanState s = new SaiyanState();
        s.noteKill("minecraft:zombie", 100, 1000);
        s.noteKill("minecraft:zombie", 200, 1000);
        assertEquals(2, s.recentKills("minecraft:zombie", 300, 1000));
        assertEquals(0, s.recentKills("minecraft:zombie", 5000, 1000));           // the window is over
        assertEquals(0, s.recentKills("minecraft:cow", 300, 1000));
        for (int i = 0; i < 100; i++) s.noteKill("t" + i, 100, 1000);
        assertTrue(s.recent.size() <= SaiyanState.MAX_RECENT);
    }

    @Test
    void cooldownsRunOutAreBoundedAndAResetClockDoesNotLeaveYears() {
        SaiyanState s = new SaiyanState();
        assertTrue(s.ready("blast", 0));
        s.startCooldown("blast", 100, 10);
        assertFalse(s.ready("blast", 105));
        assertEquals(5, s.left("blast", 105));
        assertTrue(s.ready("blast", 110));
        for (int i = 0; i < 100; i++) s.startCooldown("k" + i, 1000, 5000);
        assertTrue(s.readyAt.size() <= SaiyanState.MAX_COOLDOWNS + 1);
        s.startCooldown("far", 10, 9_000_000);
        s.clampClock(10, 24000);
        assertTrue(s.left("far", 10) <= 24000);
    }

    @Test
    void theSaveFormatRoundTripsAndClampsADamagedSave() {
        SaiyanState s = new SaiyanState();
        s.saiyan = true;
        s.awakened = true;
        s.stamina = 55.5;
        s.setBattlePower(4321.0);
        s.noteKill("minecraft:husk", 700, 12000);
        s.startCooldown("barrage", 700, 60);
        SaiyanState b = SaiyanTag.fromTag(SaiyanTag.toTag(s));
        assertTrue(b.saiyan && b.awakened);
        assertEquals(55.5, b.stamina, 1e-9);
        assertEquals(4321.0, b.battlePower, 1e-9);
        assertEquals(1, b.recentKills("minecraft:husk", 800, 12000));
        assertFalse(b.ready("barrage", 710));
        net.minecraft.nbt.CompoundTag bad = SaiyanTag.toTag(s);
        bad.putDouble("bp", Double.NaN);
        bad.putDouble("stamina", 1.0e300);
        SaiyanState c = SaiyanTag.fromTag(bad);
        assertEquals(SaiyanRules.START_BP, c.battlePower, 1e-9);
        assertTrue(c.stamina <= 1.0e9);
    }
}
