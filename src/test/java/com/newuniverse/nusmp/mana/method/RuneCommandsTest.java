package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.mana.method.RuneCommands.BoltState;
import com.newuniverse.nusmp.mana.method.RuneCommands.Command;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The spell filter: which commands a rune writes, what the bolt carries, and that the 3.4.0 spell (setting off) is the old numbers value by value. */
class RuneCommandsTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;

    private static RuneSpec spec(RuneElement e, int mods) { return new RuneSpec(e, mods, false); }

    @Test
    void theCommandsAreWhatTheRunesWrite() {
        assertEquals(List.of(), RuneCommands.commandsOf(spec(RuneElement.FIRE, RuneMods.NONE)));
        assertEquals(List.of(Command.PURSUE), RuneCommands.commandsOf(spec(RuneElement.WATER, RuneMods.HOMING)));
        assertEquals(List.of(Command.GROW), RuneCommands.commandsOf(spec(RuneElement.FIRE, RuneMods.GROWTH)));
        assertEquals(List.of(Command.PURSUE, Command.GROW), RuneCommands.commandsOf(spec(RuneElement.WIND, RuneMods.ALL)));
    }

    @Test
    void theLegacyBoltIsTheOldNumbersValueByValue() {
        for (int mods : new int[]{RuneMods.NONE, RuneMods.HOMING, RuneMods.GROWTH, RuneMods.ALL})
            for (boolean tm : new boolean[]{false, true}) {
                RuneSpec s = new RuneSpec(RuneElement.FIRE, mods, tm);
                BoltState b = RuneCommands.legacy(s);
                assertEquals(s.homing(), b.isHoming());
                assertEquals(s.growth(), b.grows());
                assertEquals(RuneRules.HOMING_TURN, b.turn(), 1e-12);
                assertEquals(1.0, b.damageMult(), 1e-12);
                for (int age = 0; age <= RuneRules.HOMING_TICKS + 5; age++) assertEquals(RuneRules.homingActive(s, age), b.homingActive(age), "homing at age " + age);
                for (double d = 0.0; d <= RuneRules.MAX_RANGE + 4; d += 0.75) {
                    assertEquals(RuneRules.hitRadius(s, d), b.hitRadius(d), 1e-12, "hit radius at " + d);
                    assertEquals(RuneRules.damageFactor(s, d), b.damageFactor(d), 1e-12, "damage factor at " + d);
                    assertEquals(RuneRules.splashRadius(s, d), b.splashRadius(d), 1e-12, "splash at " + d);
                    if (s.growth()) assertEquals(RuneRules.growthScale(d), b.growthScale(d), 1e-12, "growth scale at " + d);
                }
            }
    }

    @Test
    void aBiggerChargeMakesABiggerBoltAndATapASmallerOne() {
        RuneSpec grow = spec(RuneElement.FIRE, RuneMods.GROWTH);
        BoltState tap = RuneCommands.inject(grow, RuneCharge.of(0, MIN, MAX)), full = RuneCommands.inject(grow, RuneCharge.of(MAX, MIN, MAX)), old = RuneCommands.legacy(grow);
        for (double d : new double[]{0.0, 5.0, 15.0, 30.0}) {
            assertTrue(tap.hitRadius(d) < old.hitRadius(d) && old.hitRadius(d) < full.hitRadius(d) + 1e-12, "radius at " + d);
            assertTrue(tap.damageFactor(d) <= old.damageFactor(d) && old.damageFactor(d) <= full.damageFactor(d) + 1e-12, "growth of the damage at " + d);
        }
        assertTrue(tap.damageMult() < 1.0 && full.damageMult() > 1.0);
        assertTrue(tap.splashRadius(10) < full.splashRadius(10));
        assertTrue(tap.turn() < old.turn() && old.turn() < full.turn(), "a tap pursues lazily, a full hold sharply");
    }

    @Test
    void withoutGrowNothingScalesWithTheDistance() {
        RuneSpec plain = spec(RuneElement.EARTH, RuneMods.NONE);
        for (Charge c : new Charge[]{Charge.LEGACY, RuneCharge.of(0, MIN, MAX), RuneCharge.of(MAX, MIN, MAX)}) {
            BoltState b = RuneCommands.inject(plain, c);
            assertFalse(b.grows());
            assertEquals(1.0, b.growthScale(30.0), 1e-12);
            assertEquals(1.0, b.damageFactor(30.0), 1e-12);
            assertEquals(b.hitRadius(0.0), b.hitRadius(30.0), 1e-12);
        }
    }

    @Test
    void everythingIsCappedWhateverTheCharge() {
        RuneSpec all = spec(RuneElement.FIRE, RuneMods.ALL);
        Charge huge = new Charge(RuneCharge.Stage.FULL, 200, 1.0, 50.0, 50.0, 50.0, false);
        BoltState b = RuneCommands.inject(all, huge);
        for (double d = 0; d <= 1000; d += 50) {
            assertTrue(b.hitRadius(d) <= RuneCommands.MAX_HIT_RADIUS + 1e-12, "hit radius cap at " + d);
            assertTrue(b.splashRadius(d) <= RuneCommands.MAX_SPLASH + 1e-12, "splash cap at " + d);
        }
        assertTrue(b.damageMult() <= RuneCommands.MAX_DAMAGE_MULT + 1e-12);
        assertEquals(0.0, RuneCommands.inject(all, new Charge(RuneCharge.Stage.BASIC, 0, 0.0, -3.0, 1.0, 1.0, false)).damageMult(), "a negative power is no damage, never a heal");
    }

    @Test
    void nanAndNegativeDistancesAreSafe() {
        BoltState b = RuneCommands.legacy(spec(RuneElement.FIRE, RuneMods.GROWTH));
        assertEquals(b.hitRadius(0.0), b.hitRadius(Double.NaN), 1e-12);
        assertEquals(b.hitRadius(0.0), b.hitRadius(-20.0), 1e-12);
        assertEquals(1.0, b.damageFactor(Double.NaN), 1e-12);
    }

    @Test
    void theHomingWindowIsTheSameForEveryCharge() {
        RuneSpec h = spec(RuneElement.WATER, RuneMods.HOMING);
        for (Charge c : new Charge[]{RuneCharge.of(0, MIN, MAX), RuneCharge.of(MAX, MIN, MAX)}) {
            BoltState b = RuneCommands.inject(h, c);
            assertTrue(b.homingActive(0) && b.homingActive(RuneRules.HOMING_TICKS - 1) && !b.homingActive(RuneRules.HOMING_TICKS));
        }
    }
}
