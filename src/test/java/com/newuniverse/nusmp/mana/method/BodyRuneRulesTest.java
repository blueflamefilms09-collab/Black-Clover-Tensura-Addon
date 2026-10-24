package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.BodyRuneRules.Mods;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 3.6.0, the Body Rune: small temporary modifiers that grow with the charge, hard caps, a glide only for a full hold, durations by the setting, fixed ids. */
class BodyRuneRulesTest {
    private static final int MIN = RuneCharge.DEFAULT_MIN_HOLD, MAX = RuneCharge.DEFAULT_MAX_HOLD;

    @Test
    void aTapGivesLittleAndAFullHoldGivesTheMost() {
        Mods tap = BodyRuneRules.modsFor(RuneCharge.of(0, MIN, MAX), 15), full = BodyRuneRules.modsFor(RuneCharge.of(MAX, MIN, MAX), 15);
        assertEquals(0.06, tap.speed(), 1e-9);
        assertEquals(0.20, full.speed(), 1e-9);
        assertEquals(0.05, tap.attack(), 1e-9);
        assertEquals(0.20, full.attack(), 1e-9);
        for (int i = 0; i < BodyRuneRules.IDS.length; i++) assertTrue(full.amount(i) >= tap.amount(i), "modifier " + i);
    }

    @Test
    void everyAmountStaysUnderItsHardCapAtEveryHold() {
        for (int h = 0; h <= RuneCharge.HOLD_CAP; h++) {
            for (Charge c : new Charge[]{RuneCharge.of(h, MIN, MAX), Charge.LEGACY}) {
                Mods m = BodyRuneRules.modsFor(c, 15);
                assertTrue(m.speed() > 0 && m.speed() <= BodyRuneRules.SPEED_MAX);
                assertTrue(m.attack() > 0 && m.attack() <= BodyRuneRules.ATTACK_MAX);
                assertTrue(m.jump() > 0 && m.jump() <= BodyRuneRules.JUMP_MAX);
                assertTrue(m.step() >= 0 && m.step() <= BodyRuneRules.STEP_MAX);
                assertTrue(m.fall() > 0 && m.fall() <= BodyRuneRules.FALL_MAX);
                assertTrue(m.knockback() > 0 && m.knockback() <= BodyRuneRules.KNOCKBACK_MAX);
            }
        }
    }

    @Test
    void theStepHeightNeedsTheSecondRingTier() {
        assertEquals(0.0, BodyRuneRules.modsFor(RuneCharge.of(0, MIN, MAX), 15).step(), 0.0, "a tap is tier 1");
        assertTrue(BodyRuneRules.modsFor(RuneCharge.of(MIN, MIN, MAX), 15).step() > 0.0, "a charged hold is tier 2");
        assertEquals(1, BodyRuneRules.pictures(1));
        assertEquals(2, BodyRuneRules.pictures(2));
        assertEquals(2, BodyRuneRules.pictures(3));
    }

    @Test
    void onlyAFullMeasuredHoldGlides() {
        assertEquals(BodyRuneRules.GLIDE_TICKS, BodyRuneRules.modsFor(RuneCharge.of(MAX, MIN, MAX), 15).glideTicks());
        assertEquals(0, BodyRuneRules.modsFor(RuneCharge.of(MAX - 1, MIN, MAX), 15).glideTicks());
        assertEquals(0, BodyRuneRules.modsFor(RuneCharge.of(0, MIN, MAX), 15).glideTicks());
        assertEquals(0, BodyRuneRules.modsFor(Charge.LEGACY, 15).glideTicks(), "the old cast has no measured hold");
        assertTrue(BodyRuneRules.GLIDE_TICKS <= 100, "a short slow fall, never flight");
    }

    @Test
    void theDurationFollowsTheSettingAndTheCharge() {
        assertEquals(150, BodyRuneRules.modsFor(RuneCharge.of(0, MIN, MAX), 15).ticks(), "a tap lasts half of the full 300 ticks");
        assertEquals(300, BodyRuneRules.modsFor(RuneCharge.of(MAX, MIN, MAX), 15).ticks());
        assertEquals(BodyRuneRules.MAX_SECONDS * 20, BodyRuneRules.modsFor(RuneCharge.of(MAX, MIN, MAX), 9999).ticks(), "the setting is clamped");
        assertEquals(BodyRuneRules.MIN_SECONDS * 20, BodyRuneRules.modsFor(RuneCharge.of(MAX, MIN, MAX), -4).ticks());
        assertTrue(BodyRuneRules.modsFor(RuneCharge.of(0, MIN, MAX), BodyRuneRules.MIN_SECONDS).ticks() >= 20, "at least a second");
    }

    @Test
    void theSixIdsAreFixedDistinctAndUnderTheModNamespacePath() {
        Set<String> seen = new HashSet<>();
        for (String id : BodyRuneRules.IDS) {
            assertTrue(id.startsWith("mana_method_body_"), id);
            assertTrue(seen.add(id), "duplicate " + id);
            assertTrue(id.matches("[a-z0-9_]+"), "a valid resource path: " + id);
        }
        assertEquals(6, BodyRuneRules.IDS.length);
        assertTrue(Mods.multiplied(0) && Mods.multiplied(1) && !Mods.multiplied(2) && !Mods.multiplied(5));
    }

    @Test
    void aSessionRunsOutAtItsEndTick() {
        assertFalse(BodyRuneRules.expired(99, 100));
        assertTrue(BodyRuneRules.expired(100, 100));
        assertTrue(BodyRuneRules.expired(5000, 100));
    }

    @Test
    void theNoMeasuredHoldIsTheMiddleOfTheRange() {
        Mods old = BodyRuneRules.modsFor(Charge.LEGACY, 15), tap = BodyRuneRules.modsFor(RuneCharge.of(0, MIN, MAX), 15), full = BodyRuneRules.modsFor(RuneCharge.of(MAX, MIN, MAX), 15);
        assertTrue(old.speed() > tap.speed() && old.speed() < full.speed());
        assertEquals(3, old.tier());
    }
}
