package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The per-target cooldown behind the meal attunement's riders. */
class CooldownsTest {
    @Test
    void aKeyIsReadyOnceACooldownHasPassed() {
        Cooldowns<String> c = new Cooldowns<>(20);
        assertTrue(c.tryUse("wolf", 100));
        assertFalse(c.tryUse("wolf", 101));
        assertFalse(c.tryUse("wolf", 119));
        assertTrue(c.tryUse("wolf", 120));
        assertFalse(c.tryUse("wolf", 125));
    }

    @Test
    void keysCoolIndependently() {
        Cooldowns<String> c = new Cooldowns<>(20);
        assertTrue(c.tryUse("a", 5));
        assertTrue(c.tryUse("b", 5));
        assertFalse(c.tryUse("a", 6));
        assertFalse(c.tryUse("b", 6));
    }

    @Test
    void aClockThatWentBackwardsNeverLocksAKeyOut() {
        Cooldowns<String> c = new Cooldowns<>(20);
        assertTrue(c.tryUse("a", 100000));
        assertTrue(c.tryUse("a", 50), "another world, an earlier time");
        assertFalse(c.tryUse("a", 51));
    }

    @Test
    void oldEntriesAreSweptWhenTheTableGrows() {
        Cooldowns<Integer> c = new Cooldowns<>(20);
        for (int i = 0; i < 600; i++) assertTrue(c.tryUse(i, 0));
        assertEquals(600, c.size(), "all of them are still cooling");
        assertTrue(c.tryUse(9999, 100));
        assertEquals(1, c.size(), "the 600 old ones were swept");
    }

    @Test
    void clearForgetsEverything() {
        Cooldowns<String> c = new Cooldowns<>(20);
        c.tryUse("a", 1);
        c.clear();
        assertEquals(0, c.size());
        assertTrue(c.tryUse("a", 2));
    }
}
