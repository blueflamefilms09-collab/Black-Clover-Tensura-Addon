package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 1 of the Relics of Many Worlds: the pure rules (selection, cooldowns, uses per day, charge pools, dye cycle). */
class Wave1RulesTest {
    private static Wave1Rules.Bag bag() {
        Map<String, Long> m = new HashMap<>();
        return new Wave1Rules.Bag() {
            @Override public long get(String key) { return m.getOrDefault(key, 0L); }
            @Override public void put(String key, long value) { m.put(key, value); }
        };
    }

    private static final Wave1Rules.Pool MOON = new Wave1Rules.Pool("moon", 10, 4, 6);
    private static final Wave1Rules.Pool VIOLA = new Wave1Rules.Pool("viola", 5, 1, 4);
    private static final Wave1Rules.Pool DOOR = new Wave1Rules.Pool("walker", 3, 0, 0);

    @Test
    void theSelectedSpellCyclesAndWraps() {
        Wave1Rules.Bag b = bag();
        assertEquals(0, Wave1Rules.selected(b, 5));
        for (int i = 1; i <= 5; i++) assertEquals(i % 5, Wave1Rules.cycle(b, 5));
        assertEquals(0, Wave1Rules.selected(b, 5));
        b.put("sel", 9);
        assertEquals(1, Wave1Rules.selected(b, 4), "a stale index from a longer spell list wraps instead of overrunning");
        assertEquals(0, Wave1Rules.selected(b, 0));
    }

    @Test
    void cooldownsRunPerSpell() {
        Wave1Rules.Bag b = bag();
        assertTrue(Wave1Rules.ready(b, "door", 100));
        Wave1Rules.startCooldown(b, "door", 100, 40);
        assertFalse(Wave1Rules.ready(b, "door", 139));
        assertEquals(1, Wave1Rules.remaining(b, "door", 139));
        assertTrue(Wave1Rules.ready(b, "door", 140));
        assertTrue(Wave1Rules.ready(b, "scry", 101), "another spell is not held up");
        assertEquals(0, Wave1Rules.remaining(b, "door", 500));
    }

    @Test
    void usesPerDayResetAtTheNextDay() {
        Wave1Rules.Bag b = bag();
        long t = 5000;
        assertEquals(1, Wave1Rules.usesLeft(b, "air", t, 1));
        assertTrue(Wave1Rules.takeUse(b, "air", t, 1));
        assertFalse(Wave1Rules.takeUse(b, "air", t + 100, 1));
        assertEquals(0, Wave1Rules.usesLeft(b, "air", t + 100, 1));
        assertTrue(Wave1Rules.takeUse(b, "air", Wave1Rules.DAY + 10, 1), "a new day, a new use");
        assertTrue(Wave1Rules.takeUse(b, "stars", t, 3));
        assertTrue(Wave1Rules.takeUse(b, "stars", t, 3));
        assertTrue(Wave1Rules.takeUse(b, "stars", t, 3));
        assertFalse(Wave1Rules.takeUse(b, "stars", t, 3));
    }

    @Test
    void theDiceAreDeterministicAndInRange() {
        int[] seen = new int[7];
        for (long s = 0; s < 6000; s++) {
            int r = Wave1Rules.roll(s, 6);
            assertTrue(r >= 1 && r <= 6, "d6 " + r);
            assertEquals(r, Wave1Rules.roll(s, 6), "same seed, same roll");
            seen[r]++;
        }
        for (int face = 1; face <= 6; face++) assertTrue(seen[face] > 700, "face " + face + " comes up " + seen[face]);
        assertEquals(1, Wave1Rules.roll(77, 1));
        for (long d = 0; d < 500; d++) {
            int g = Wave1Rules.dawnGain(MOON, d);
            assertTrue(g >= 5 && g <= 10, "1d6+4 is 5..10, was " + g);
            int v = Wave1Rules.dawnGain(VIOLA, d);
            assertTrue(v >= 2 && v <= 5, "1d4+1 is 2..5, was " + v);
            assertEquals(3, Wave1Rules.dawnGain(DOOR, d), "a refill pool refills to its size");
        }
    }

    @Test
    void aPoolStartsFullSpendsAndRefillsAtDawn() {
        Wave1Rules.Bag b = bag();
        long day0 = 3000;
        assertEquals(10, Wave1Rules.charges(b, day0, MOON));
        assertTrue(Wave1Rules.spend(b, day0, MOON, 7));
        assertEquals(3, Wave1Rules.charges(b, day0 + 50, MOON));
        assertFalse(Wave1Rules.spend(b, day0 + 50, MOON, 5), "cannot overspend");
        assertEquals(3, Wave1Rules.charges(b, day0 + 60, MOON), "a failed spend costs nothing");
        assertEquals(3, Wave1Rules.charges(b, Wave1Rules.DAY - 1, MOON), "still the same day");
        int next = Wave1Rules.charges(b, Wave1Rules.DAY + 5, MOON);
        assertEquals(3 + Wave1Rules.dawnGain(MOON, 1), next, "the dawn of day 1 gives 1d6+4");
        assertTrue(next <= 10);
        assertEquals(next, Wave1Rules.charges(b, Wave1Rules.DAY + 900, MOON), "once per dawn");
    }

    @Test
    void aLongAbsenceStillCapsAtTheMaximum() {
        Wave1Rules.Bag b = bag();
        Wave1Rules.charges(b, 10, MOON);
        assertTrue(Wave1Rules.spend(b, 10, MOON, 10));
        assertEquals(0, Wave1Rules.charges(b, 11, MOON));
        assertEquals(10, Wave1Rules.charges(b, Wave1Rules.DAY * 400, MOON), "a year away refills it, bounded work");
    }

    @Test
    void aGutteredStaffSkipsOneDawn() {
        Wave1Rules.Bag b = bag();
        Wave1Rules.charges(b, 10, MOON);
        assertTrue(Wave1Rules.spend(b, 10, MOON, 10));
        Wave1Rules.dim(b, MOON);
        assertTrue(Wave1Rules.dimmed(b, MOON));
        assertEquals(0, Wave1Rules.charges(b, Wave1Rules.DAY + 1, MOON), "the first dawn gives nothing");
        assertFalse(Wave1Rules.dimmed(b, MOON));
        assertEquals(Wave1Rules.dawnGain(MOON, 2), Wave1Rules.charges(b, 2 * Wave1Rules.DAY + 1, MOON), "the second dawn is normal");
    }

    @Test
    void theDimensionWalkerRefillsAllThree() {
        Wave1Rules.Bag b = bag();
        assertEquals(3, Wave1Rules.charges(b, 0, DOOR));
        assertTrue(Wave1Rules.spend(b, 0, DOOR, 3));
        assertEquals(0, Wave1Rules.charges(b, 100, DOOR));
        assertEquals(3, Wave1Rules.charges(b, Wave1Rules.DAY, DOOR));
        assertTrue(Wave1Rules.empty(bag(), 0, new Wave1Rules.Pool("x", 0, 0, 0)));
    }

    @Test
    void spellDamageIsToldFromBlows() {
        for (String spell : new String[]{"magic", "indirect_magic", "magic_generic", "light_elemental", "fire_elemental", "holy_damage", "curse"})
            assertTrue(Wave1Rules.isSpellPath(spell), spell);
        for (String blow : new String[]{"player_attack", "mob_attack", "arrow", "thorns", "fall", "explosion"})
            assertFalse(Wave1Rules.isSpellPath(blow), blow);
        assertFalse(Wave1Rules.isSpellPath(null));
    }

    @Test
    void theDyeCycleFollowsVanillaOrder() {
        assertEquals("orange_wool", Wave1Words.nextDye("white_wool"));
        assertEquals("white_wool", Wave1Words.nextDye("black_wool"));
        assertEquals("cyan_carpet", Wave1Words.nextDye("light_gray_carpet"));
        assertEquals("light_gray_concrete_powder", Wave1Words.nextDye("gray_concrete_powder"));
        assertEquals("white_terracotta", Wave1Words.nextDye("terracotta"));
        assertEquals("white_stained_glass", Wave1Words.nextDye("glass"));
        assertEquals("white_stained_glass_pane", Wave1Words.nextDye("glass_pane"));
        assertEquals("magenta_glazed_terracotta", Wave1Words.nextDye("orange_glazed_terracotta"));
        assertNull(Wave1Words.nextDye("stone"));
        assertNull(Wave1Words.nextDye("red_bed"), "beds and banners are not recoloured");
        assertNull(Wave1Words.nextDye("white_shulker_box"), "containers are never touched");
        assertNull(Wave1Words.nextDye(null));
        String p = "white_wool";
        for (int i = 0; i < 16; i++) p = Wave1Words.nextDye(p);
        assertEquals("white_wool", p, "sixteen steps come full circle");
    }

    @Test
    void theBookHasSevenPagesAndDadAlwaysAnswers() {
        assertEquals(7, Wave1Words.MISSPELLS.length);
        for (Wave1Words.Misspell m : Wave1Words.MISSPELLS) {
            assertFalse(m.shown().isBlank());
            assertNotEquals(m.shown(), m.truth());
            assertFalse(m.reveal().isBlank());
        }
        for (long s = -50; s < 500; s++) assertNotNull(Wave1Words.pick(Wave1Words.DAD, s));
    }
}
