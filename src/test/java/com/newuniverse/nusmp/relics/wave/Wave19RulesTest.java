package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Relics of Many Worlds, wave 19: the pure rules of the scrolls, potions and the spellbook. */
class Wave19RulesTest {
    @Test
    void wildMagicTableCoversEveryRollAndEveryOutcome() {
        java.util.Set<Wave19Rules.Surge> seen = new java.util.HashSet<>();
        for (int r = 1; r <= 100; r++) seen.add(Wave19Rules.surge(r));
        assertEquals(Wave19Rules.Surge.values().length, seen.size(), "every outcome is reachable");
        assertEquals(Wave19Rules.Surge.GLITTER, Wave19Rules.surge(1));
        assertEquals(Wave19Rules.Surge.WISH, Wave19Rules.surge(100));
        assertEquals(Wave19Rules.Surge.GLITTER, Wave19Rules.surge(-5), "clamped low");
        assertEquals(Wave19Rules.Surge.WISH, Wave19Rules.surge(900), "clamped high");
        assertEquals(Wave19Rules.Surge.LEVITATE, Wave19Rules.surge(7));
        assertEquals(Wave19Rules.Surge.GLITTER, Wave19Rules.surge(6));
    }

    @Test
    void diceStayInRange() {
        for (int i = -300; i < 300; i++) {
            final int seed = i;
            int d = Wave19Rules.d100(n -> seed);
            assertTrue(d >= 1 && d <= 100, "d100 " + d);
            int t = Wave19Rules.d20(n -> seed);
            assertTrue(t >= 1 && t <= 20, "d20 " + t);
        }
    }

    @Test
    void savingThrowsHaveNaturalTwentyAndOne() {
        assertTrue(Wave19Rules.saves(20, -50, 30));
        assertFalse(Wave19Rules.saves(1, 50, 5));
        assertTrue(Wave19Rules.saves(10, 0, 10));
        assertFalse(Wave19Rules.saves(9, 0, 10));
        assertTrue(Wave19Rules.saves(12, 3, 15));
    }

    @Test
    void feyHoneyIsSparedByAFullPoolOrTheSave() {
        assertTrue(Wave19Rules.honeySpared(0.9, 2), "a pool of Fey magic shrugs it off");
        assertTrue(Wave19Rules.honeySpared(0.1, 12));
        assertFalse(Wave19Rules.honeySpared(0.1, 5));
        assertFalse(Wave19Rules.honeySpared(0.79, 9));
    }

    @Test
    void bloodChocolateDoublesOnlyRadiantDamageWhileVulnerable() {
        assertEquals(10f, Wave19Rules.radiantDamage(5f, true, true), 1e-6);
        assertEquals(5f, Wave19Rules.radiantDamage(5f, false, true), 1e-6);
        assertEquals(5f, Wave19Rules.radiantDamage(5f, true, false), 1e-6);
        assertTrue(Wave19Rules.isRadiantPath("holy_damage"));
        assertTrue(Wave19Rules.isRadiantPath("Light_Elemental"));
        assertFalse(Wave19Rules.isRadiantPath("fire_elemental"));
        assertFalse(Wave19Rules.isRadiantPath(null));
    }

    @Test
    void absorptionTakesSpellsNotBlades() {
        assertTrue(Wave19Rules.isSpellPath("magic"));
        assertTrue(Wave19Rules.isSpellPath("fire_bolt"));
        assertTrue(Wave19Rules.isSpellPath("thunder_lance"));
        assertFalse(Wave19Rules.isSpellPath("player_attack"));
        assertFalse(Wave19Rules.isSpellPath("arrow"));
        assertFalse(Wave19Rules.isSpellPath("fall"));
        assertEquals(Wave19Rules.ABSORB_CAP, Wave19Rules.released(999f), 1e-6, "a huge spell comes back capped");
        assertEquals(Wave19Rules.ABSORB_FALLBACK, Wave19Rules.released(0f), 1e-6, "a spell with no damage comes back as a small blast");
        assertEquals(9f, Wave19Rules.released(9f), 1e-6);
    }

    @Test
    void charmParsonSparesGreatBeasts() {
        assertTrue(Wave19Rules.charmTakes(20f));
        assertTrue(Wave19Rules.charmTakes(99f));
        assertFalse(Wave19Rules.charmTakes(100f));
    }

    @Test
    void firballHalvesOnADodge() {
        assertEquals(12f, Wave19Rules.firball(false), 1e-6);
        assertEquals(6f, Wave19Rules.firball(true), 1e-6);
    }

    @Test
    void rocksOffsetsAreReadable() {
        assertEquals("right here", Wave19Rules.offsetText(0, 0, 0));
        assertEquals("3 east, 2 down, 5 north", Wave19Rules.offsetText(3, -2, -5));
        assertEquals("1 up", Wave19Rules.offsetText(0, 1, 0));
        assertEquals("4 west, 7 south", Wave19Rules.offsetText(-4, 0, 7));
        for (int i = -20; i < 20; i++) assertNotNull(Wave19Rules.rockQuip(i));
    }

    @Test
    void facsimilesDegradeAndRunOut() {
        assertEquals(1, Wave19Rules.nextGeneration(0));
        assertEquals(3, Wave19Rules.nextGeneration(2));
        assertEquals(-1, Wave19Rules.nextGeneration(3), "a fourth copy cannot be made");
        assertTrue(Wave19Rules.facsimileLife(1) > Wave19Rules.facsimileLife(2));
        assertTrue(Wave19Rules.facsimileLife(2) > Wave19Rules.facsimileLife(3));
        assertTrue(Wave19Rules.facsimileLife(30) >= 600, "never under half a minute");
        assertTrue(Wave19Rules.facsimileQuality(1) > Wave19Rules.facsimileQuality(2));
        // a 100-durability tool with 20 used: the first copy keeps half of the 80 left
        assertEquals(60, Wave19Rules.facsimileDamage(100, 20, 1));
        assertEquals(0, Wave19Rules.facsimileDamage(0, 0, 1), "not damageable: nothing");
        int worst = Wave19Rules.facsimileDamage(100, 99, 3);
        assertTrue(worst >= 0 && worst <= 99, "a copy is never born broken: " + worst);
        assertEquals(99, Wave19Rules.facsimileDamage(100, 100, 1), "even a spent source gives a copy one point of durability");
    }

    @Test
    void theMugFillsOnceADay() {
        assertEquals(0, Wave19Rules.mugWait(100, 0), "never chugged");
        assertEquals(Wave19Rules.DAY - 50, Wave19Rules.mugWait(150, 100));
        assertEquals(0, Wave19Rules.mugWait(100 + Wave19Rules.DAY, 100));
        assertEquals(0, Wave19Rules.mugWait(100 + Wave19Rules.DAY + 5, 100));
        int t = 0;
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < Wave19Rules.TITLES.length * 2; i++) { seen.add(t); t = Wave19Rules.nextTitle(t); }
        assertEquals(Wave19Rules.TITLES.length, seen.size(), "the titles cycle through all five");
        assertEquals("Boss", Wave19Rules.title(0));
        assertEquals("Wizard", Wave19Rules.title(-1), "titles wrap");
        assertTrue(Wave19Rules.mugAwes(30f));
        assertFalse(Wave19Rules.mugAwes(60f));
    }

    @Test
    void theSpellbookPagesCycleAndHoldTwoSpellsEach() {
        for (int i = 0; i < 9; i++) {
            assertEquals(2, Wave19Rules.page(i).length);
            assertNotNull(Wave19Rules.pageName(i));
        }
        assertEquals(1, Wave19Rules.nextPage(0));
        assertEquals(0, Wave19Rules.nextPage(Wave19Rules.PAGES.length - 1));
        assertEquals(Wave19Rules.PAGES.length, Wave19Rules.PAGE_NAMES.length);
    }

    @Test
    void timeScalesAreConsistent() {
        assertEquals(24000, Wave19Rules.DAY);
        assertTrue(Wave19Rules.VULNERABLE_TICKS < Wave19Rules.HONEY_TICKS, "twenty minutes is shorter than an hour");
        assertTrue(Wave19Rules.CRAB_WARN >= 40, "the crab is telegraphed for at least two seconds");
        assertTrue(Wave19Rules.CRAB_TICKS <= 20 * 30);
    }
}
