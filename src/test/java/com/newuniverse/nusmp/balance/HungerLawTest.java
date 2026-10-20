package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the arithmetic of the owner's hunger ban (what counts as the bar getting worse, which effect id is the banned one) is pinned as plain numbers. */
class HungerLawTest {
    @Test
    void anUnchangedOrFedBarIsClean() {
        HungerLaw.Bar a = new HungerLaw.Bar(15, 2.0f, 1.5f);
        assertTrue(HungerLaw.violations(a, a).isEmpty());
        assertTrue(HungerLaw.violations(a, new HungerLaw.Bar(20, 5.0f, 0.0f)).isEmpty(), "feeding is fine, and a lower exhaustion is a food point spent by vanilla or a meal");
        assertTrue(HungerLaw.violations(null, a).isEmpty());
    }

    @Test
    void everyWorseningIsNamed() {
        HungerLaw.Bar a = new HungerLaw.Bar(15, 2.0f, 1.5f);
        List<String> food = HungerLaw.violations(a, new HungerLaw.Bar(14, 2.0f, 1.5f));
        assertEquals(1, food.size());
        assertTrue(food.get(0).contains("food level"));
        assertEquals(1, HungerLaw.violations(a, new HungerLaw.Bar(15, 1.0f, 1.5f)).size(), "saturation");
        assertEquals(1, HungerLaw.violations(a, new HungerLaw.Bar(15, 2.0f, 1.9f)).size(), "exhaustion rose");
        assertEquals(3, HungerLaw.violations(a, new HungerLaw.Bar(3, 0.0f, 3.9f)).size(), "all three at once");
    }

    @Test
    void aRepairIsNeverWorseThanTheStart() {
        HungerLaw.Bar a = new HungerLaw.Bar(15, 2.0f, 1.5f);
        HungerLaw.Bar bad = new HungerLaw.Bar(3, 0.0f, 3.9f);
        HungerLaw.Bar fixed = HungerLaw.repaired(a, bad);
        assertEquals(a, fixed);
        assertTrue(HungerLaw.violations(a, fixed).isEmpty());
        HungerLaw.Bar fed = new HungerLaw.Bar(20, 6.0f, 0.2f);
        assertEquals(fed, HungerLaw.repaired(a, fed), "a repair never takes a meal away");
    }

    @Test
    void theHungerEffectIdIsRecognised() {
        assertTrue(HungerLaw.isHungerEffectId("minecraft:hunger"));
        assertTrue(HungerLaw.isHungerEffectId("  Minecraft:Hunger "));
        assertTrue(HungerLaw.isHungerEffectId("hunger"));
        assertFalse(HungerLaw.isHungerEffectId("minecraft:saturation"));
        assertFalse(HungerLaw.isHungerEffectId("minecraft:mining_fatigue"));
        assertFalse(HungerLaw.isHungerEffectId("tensura:starved"));
        assertFalse(HungerLaw.isHungerEffectId(null));
    }
}
