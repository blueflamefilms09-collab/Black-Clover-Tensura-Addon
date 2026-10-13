package com.newuniverse.nusmp.dungeon.nexus.nav;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What the Ki-Resonance Lantern burns and for how long. */
class LanternFuelTest {
    @Test
    void theFuelsAreWorthWhatTheDesignSays() {
        assertEquals(8 * 60 * 20, LanternFuel.ticksFor("tensura:magic_stone"), "a stone is about 8 minutes");
        assertEquals(2 * 60 * 20, LanternFuel.ticksFor("minecraft:slime_ball"), "a slime ball is 2 minutes");
        assertEquals(15 * 60 * 20, LanternFuel.ticksFor("tensura:low_quality_magic_crystal"));
        assertEquals(20 * 60 * 20, LanternFuel.ticksFor("tensura:medium_quality_magic_crystal"));
        assertEquals(30 * 60 * 20, LanternFuel.ticksFor("tensura:high_quality_magic_crystal"));
        assertEquals(LanternFuel.DEFAULT_TICKS, LanternFuel.ticksFor("somemod:glowing_goo"), "an item a pack adds to the tag");
        assertEquals(LanternFuel.DEFAULT_TICKS, LanternFuel.ticksFor(null));
        assertTrue(LanternFuel.ticksFor("tensura:magic_ore_shard") > 0);
        assertTrue(LanternFuel.ticksFor("tensura:chilled_slime") >= LanternFuel.ticksFor("minecraft:slime_ball"));
    }

    @Test
    void theHighCrystalsAreWorthMoreThanTheLow() {
        assertTrue(LanternFuel.ticksFor("tensura:high_quality_magic_crystal") > LanternFuel.ticksFor("tensura:medium_quality_magic_crystal"));
        assertTrue(LanternFuel.ticksFor("tensura:medium_quality_magic_crystal") > LanternFuel.ticksFor("tensura:low_quality_magic_crystal"));
        assertTrue(LanternFuel.ticksFor("tensura:low_quality_magic_crystal") > LanternFuel.ticksFor("tensura:magic_stone"));
    }

    @Test
    void anyOneItemFitsAnEmptyLantern() {
        for (String id : new String[]{"tensura:high_quality_magic_crystal", "tensura:magic_stone", "minecraft:slime_ball", "x:y"})
            assertEquals(1, LanternFuel.howMany(0, LanternFuel.ticksFor(id), 64, 1), id);
    }

    @Test
    void theLanternTakesOnlyWhatFitsUnderItsCap() {
        int stone = LanternFuel.ticksFor("tensura:magic_stone");
        assertEquals(7, LanternFuel.howMany(0, stone, 64, 64), "an hour is seven and a half stones");
        assertEquals(3, LanternFuel.howMany(0, stone, 3, 64), "only what is offered");
        assertEquals(2, LanternFuel.howMany(0, stone, 64, 2), "only what is wanted");
        assertEquals(0, LanternFuel.howMany(LanternFuel.CAP, stone, 64, 64), "full");
        assertEquals(0, LanternFuel.howMany(LanternFuel.CAP - stone + 1, stone, 64, 64), "not even one more fits");
        assertEquals(1, LanternFuel.howMany(LanternFuel.CAP - stone, stone, 64, 64), "exactly one more fits");
        assertEquals(0, LanternFuel.howMany(0, 0, 64, 64), "worthless fuel is not taken");
        assertEquals(0, LanternFuel.howMany(0, stone, 0, 5));
        assertEquals(0, LanternFuel.howMany(0, stone, 5, 0));
        assertEquals(7, LanternFuel.howMany(-50, stone, 64, 64), "a corrupted negative fuel counts as empty");
    }

    @Test
    void fuelNeverGoesPastTheCapWhateverIsFed() {
        int fuel = 0;
        int crystal = LanternFuel.ticksFor("tensura:high_quality_magic_crystal");
        for (int i = 0; i < 10; i++) fuel += crystal * LanternFuel.howMany(fuel, crystal, 64, 1);
        assertTrue(fuel <= LanternFuel.CAP, "" + fuel);
        assertEquals(2 * crystal, fuel);
    }

    @Test
    void burningCountsDownToZeroAndStops() {
        assertEquals(80, LanternFuel.burn(100, 20));
        assertEquals(0, LanternFuel.burn(10, 20));
        assertEquals(0, LanternFuel.burn(0, 20));
        assertEquals(100, LanternFuel.burn(100, -5), "negative time does not refuel");
        int fuel = LanternFuel.ticksFor("minecraft:slime_ball");
        int seconds = 0;
        while (fuel > 0) { fuel = LanternFuel.burn(fuel, 20); seconds++; }
        assertEquals(120, seconds, "a slime ball burns two minutes");
    }

    @Test
    void theBarAndTheClock() {
        assertEquals(0f, LanternFuel.fill(0));
        assertEquals(1f, LanternFuel.fill(LanternFuel.CAP));
        assertEquals(1f, LanternFuel.fill(LanternFuel.CAP * 3));
        assertEquals(0f, LanternFuel.fill(-4));
        assertEquals(0.5f, LanternFuel.fill(LanternFuel.CAP / 2), 1e-6);
        assertEquals("8m 00s", LanternFuel.clock(9600));
        assertEquals("0m 01s", LanternFuel.clock(1));
        assertEquals("0m 00s", LanternFuel.clock(0));
        assertEquals("0m 00s", LanternFuel.clock(-3));
        assertEquals("1m 05s", LanternFuel.clock(1300));
    }
}
