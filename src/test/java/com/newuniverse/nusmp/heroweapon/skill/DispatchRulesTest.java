package com.newuniverse.nusmp.heroweapon.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.skill.DispatchRules.Refusal;
import org.junit.jupiter.api.Test;

/**
 * Why: the order in which the dispatcher refuses a technique is what a player experiences ("it is not out", "nothing on that key", "still gathering", "wait
 * between techniques"), and the cooldown arithmetic is the owner's balance floor, so both are pinned: every refusal in its order, the level shortening a
 * cooldown by up to a quarter, and the floor that no skill can go under.
 */
class DispatchRulesTest {
    @Test
    void theChecksRunInTheDocumentedOrder() {
        assertEquals(Refusal.NONE, DispatchRules.check(true, true, true, 0, 0, false));
        assertEquals(Refusal.NOT_SUMMONED, DispatchRules.check(false, false, false, 99, 99, true), "not out beats everything");
        assertEquals(Refusal.NOT_HELD, DispatchRules.check(true, false, false, 99, 99, true));
        assertEquals(Refusal.NO_SKILL, DispatchRules.check(true, true, false, 99, 99, true));
        assertEquals(Refusal.BUSY, DispatchRules.check(true, true, true, 99, 99, true), "a wind-up running");
        assertEquals(Refusal.COOLDOWN, DispatchRules.check(true, true, true, 99, 99, false));
        assertEquals(Refusal.LOCK, DispatchRules.check(true, true, true, 0, 5, false));
    }

    @Test
    void theLevelShortensTheCooldownByUpToAQuarter() {
        assertEquals(200, DispatchRules.cooldownTicks(200, 0, 20));
        assertEquals(150, DispatchRules.cooldownTicks(200, 100, 20));
        assertEquals(150, DispatchRules.cooldownTicks(200, 400, 20), "levels above the maximum add nothing");
        assertEquals(175, DispatchRules.cooldownTicks(200, 50, 20));
    }

    @Test
    void noSkillGoesUnderTheFloor() {
        assertEquals(20, DispatchRules.cooldownTicks(5, 0, 20));
        assertEquals(20, DispatchRules.cooldownTicks(0, 100, 20), "a skill that asks for no cooldown still gets the floor");
        assertEquals(20, DispatchRules.cooldownTicks(-30, 0, 20));
        assertEquals(1, DispatchRules.cooldownTicks(0, 0, 0), "even a zero floor leaves one tick");
        assertTrue(DispatchRules.cooldownTicks(60, 100, 20) >= 20);
    }

    @Test
    void creativeCooldownsAreAQuarterWithAFloor() {
        assertEquals(50, DispatchRules.creativeTicks(200, 10));
        assertEquals(10, DispatchRules.creativeTicks(20, 10));
    }
}
