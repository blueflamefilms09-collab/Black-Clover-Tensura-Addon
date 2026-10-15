package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 23 of the Relics of Many Worlds: the pure rules (dice, saves, rests and distances turned into numbers). */
class Wave23RulesTest {
    @Test
    void radiantDoublesAgainstFiendsAndTheUndead() {
        assertEquals(7.0f, Wave23Rules.radiant(false));
        assertEquals(14.0f, Wave23Rules.radiant(true));
        assertTrue(Wave23Rules.isFiendOrUndead("zombie"));
        assertTrue(Wave23Rules.isFiendOrUndead("tensura:demon_lord"));
        assertFalse(Wave23Rules.isFiendOrUndead("cow"));
        assertFalse(Wave23Rules.isFiendOrUndead(null));
    }

    @Test
    void theSentenceNeedsAHundredHealthOrLessAndNeverKillsOutright() {
        assertTrue(Wave23Rules.canSentence(100.0));
        assertFalse(Wave23Rules.canSentence(100.5));
        assertFalse(Wave23Rules.canSentence(0.0));
        assertEquals(11.0f, Wave23Rules.sentenceDamage(20.0, false), 1e-4);
        assertEquals(5.5f, Wave23Rules.sentenceDamage(20.0, true), 1e-4);
        assertEquals(4.0f, Wave23Rules.sentenceDamage(2.0, false), 1e-4, "never under four");
        assertTrue(Wave23Rules.sentenceDamage(100.0, false) < 100.0f, "a heavy blow, not an instant death");
        assertTrue(Wave23Rules.sentenceSaved(0.1));
        assertFalse(Wave23Rules.sentenceSaved(0.9));
    }

    @Test
    void embrasureHasTwoUsesThenAShortRest() {
        Wave23Rules.Charges c = Wave23Rules.Charges.NONE;
        assertTrue(c.canUse(100));
        c = c.use(100);
        assertTrue(c.canUse(110), "one use left");
        c = c.use(110);
        assertFalse(c.canUse(120), "two used: resting");
        assertEquals(Wave23Rules.PROTECT_REST_TICKS - 10, c.restLeft(120));
        assertFalse(c.canUse(110 + Wave23Rules.PROTECT_REST_TICKS - 1));
        assertTrue(c.canUse(110 + Wave23Rules.PROTECT_REST_TICKS), "the rest is over");
        assertEquals(0, c.at(110 + Wave23Rules.PROTECT_REST_TICKS).used());
    }

    @Test
    void dragonstrikeIsHalvedBySaveAndTrapsAreRecognised() {
        assertEquals(14.0f, Wave23Rules.strikeDamage(false));
        assertEquals(7.0f, Wave23Rules.strikeDamage(true));
        assertTrue(Wave23Rules.isTrapBlock("tripwire_hook"));
        assertTrue(Wave23Rules.isTrapBlock("stone_pressure_plate"));
        assertFalse(Wave23Rules.isTrapBlock("stone"));
        assertEquals(20.0, Wave23Rules.STRIKE_LENGTH, "100 ft");
        assertEquals(6.0, Wave23Rules.SIGHT_RADIUS, "30 ft");
    }

    @Test
    void theReaverDragsOnlyLargeOrSmallerNonBossesThatFailTheirSave() {
        assertTrue(Wave23Rules.largeOrSmaller(0.6, 1.95));
        assertFalse(Wave23Rules.largeOrSmaller(0.6, 2.9), "an enderman is Huge");
        assertTrue(Wave23Rules.pulled(0.6, 1.95, 20.0, 0.9));
        assertFalse(Wave23Rules.pulled(0.6, 1.95, 20.0, 0.1), "it saved");
        assertFalse(Wave23Rules.pulled(0.6, 1.95, 300.0, 0.9), "a boss is never dragged");
        assertFalse(Wave23Rules.pulled(3.0, 3.0, 20.0, 0.9));
        assertTrue(Wave23Rules.barbDamage(5, true) > Wave23Rules.barbDamage(5, false));
        assertTrue(Wave23Rules.barbDamage(15, false) < Wave23Rules.barbDamage(5, false), "long range is a smaller blow");
    }

    @Test
    void theChordIsStrongerOnTheBeatAndOncePerCreaturePerDay() {
        assertTrue(Wave23Rules.rhythm(1000, 1100));
        assertFalse(Wave23Rules.rhythm(1000, 1000 + Wave23Rules.RHYTHM_WINDOW + 1));
        assertFalse(Wave23Rules.rhythm(0, 50), "never struck");
        assertEquals(1, Wave23Rules.chordAmp(true));
        assertEquals(0, Wave23Rules.chordAmp(false));
        long today = Wave23Rules.dayOf(50000);
        assertEquals(2, today);
        assertTrue(Wave23Rules.chordAllowed(0, today), "never had one");
        assertFalse(Wave23Rules.chordAllowed(today + 1, today), "had one today");
        assertTrue(Wave23Rules.chordAllowed(today, today), "had one yesterday");
    }

    @Test
    void aegisSwallowsUpToNinetyAndShattersAbove() {
        assertEquals(30f, Wave23Rules.absorbed(30f));
        assertEquals(90f, Wave23Rules.absorbed(120f));
        assertFalse(Wave23Rules.shatters(90f));
        assertTrue(Wave23Rules.shatters(90.5f));
        assertEquals(10f, Wave23Rules.absorbGain(20f));
        assertEquals(20f, Wave23Rules.absorbGain(500f));
        assertEquals(2, Wave23Rules.hungerLeft(5, 0, 5 * Wave23Rules.DAY + 10));
        assertEquals(1, Wave23Rules.hungerLeft(5, 1, 5 * Wave23Rules.DAY + 10));
        assertEquals(0, Wave23Rules.hungerLeft(5, 2, 5 * Wave23Rules.DAY + 10));
        assertEquals(2, Wave23Rules.hungerLeft(5, 2, 6 * Wave23Rules.DAY), "a new day");
        assertTrue(Wave23Rules.isSpell("magic"));
        assertTrue(Wave23Rules.isSpell("tensura:holy_damage".substring(8)));
        assertFalse(Wave23Rules.isSpell("player_attack"));
    }

    @Test
    void celestialSupportCountsHeavyBlowsAndSavesFromDeath() {
        assertTrue(Wave23Rules.isCritical(7.0, 20.0));
        assertFalse(Wave23Rules.isCritical(6.9, 20.0));
        assertFalse(Wave23Rules.isCritical(5.0, 0.0));
        assertEquals(1f, Wave23Rules.deathSave(30f, 12f) - 10f, 1e-4, "leaves one health point of the twelve");
        assertEquals(5f, Wave23Rules.deathSave(5f, 12f), "a survivable blow is untouched");
        assertEquals(0f, Wave23Rules.deathSave(5f, 0.5f), 1e-4);
    }

    @Test
    void heroAndThunderRules() {
        assertTrue(Wave23Rules.isWeaponPath("mob_attack"));
        assertTrue(Wave23Rules.isWeaponPath("arrow"));
        assertFalse(Wave23Rules.isWeaponPath("magic"));
        assertFalse(Wave23Rules.isWeaponPath("lightning_bolt"));
        assertEquals(8f, Wave23Rules.throwDamage(3));
        assertTrue(Wave23Rules.throwDamage(12) < Wave23Rules.throwDamage(3));
        assertTrue(Wave23Rules.isLightning("lightning_bolt"));
        assertTrue(Wave23Rules.isLightning("thunder_breath"));
        assertFalse(Wave23Rules.isLightning("in_fire"));
    }

    @Test
    void lyreChargesAndPerformance() {
        assertTrue(Wave23Rules.bewitched(true, true, 0.99), "a spent charge always works");
        assertTrue(Wave23Rules.bewitched(false, false, 0.4));
        assertFalse(Wave23Rules.bewitched(false, false, 0.6));
        assertFalse(Wave23Rules.bewitched(false, true, 0.4), "a boss saves more often");
        assertEquals(7, Wave23Rules.lyreAfter(8, true));
        assertEquals(0, Wave23Rules.lyreAfter(0, true));
        assertEquals(8, Wave23Rules.lyreAfter(8, false));
        assertFalse(Wave23Rules.performed(11, 0.0));
        assertTrue(Wave23Rules.performed(12, 0.5));
        assertFalse(Wave23Rules.performed(12, 0.9));
    }

    @Test
    void theRingRollsAFourSidedBoonAfterEachRest() {
        assertEquals(1, Wave23Rules.boon(0));
        assertEquals(4, Wave23Rules.boon(9));
        assertEquals(3, Wave23Rules.boon(3));
        for (int i = 1; i <= 4; i++) assertNotEquals("none", Wave23Rules.boonName(i));
        assertTrue(Wave23Rules.rerollDue(0, 10, false), "never rolled");
        assertTrue(Wave23Rules.rerollDue(5 + 1, 6, true), "slept into a new day");
        assertFalse(Wave23Rules.rerollDue(5 + 1, 5, true), "same day");
        assertFalse(Wave23Rules.rerollDue(5 + 1, 6, false), "awake: not yet");
        assertTrue(Wave23Rules.rerollDue(5 + 1, 7, false), "two days without a rest");
    }

    @Test
    void theHornWinsWithEightKillsAndCharismaIsCapped() {
        assertFalse(Wave23Rules.victory(7));
        assertTrue(Wave23Rules.victory(8));
        assertEquals(1, Wave23Rules.charismaAfterVictory(0));
        assertEquals(5, Wave23Rules.charismaAfterVictory(5));
        assertEquals(2.5, Wave23Rules.charismaLuck(5), 1e-9);
        assertEquals(2.5, Wave23Rules.charismaLuck(50), 1e-9);
        assertEquals(0.0, Wave23Rules.charismaLuck(-3), 1e-9);
        assertEquals(100.0, Wave23Rules.HORN_RADIUS, "500 ft");
        assertEquals(6000, Wave23Rules.HORN_TICKS, "6 of 24 hours");
        assertEquals(120000, Wave23Rules.HORN_COOLDOWN);
    }
}
