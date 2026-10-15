package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.wave.Wave28Rules.Charges;
import com.newuniverse.nusmp.relics.wave.Wave28Rules.Held;
import com.newuniverse.nusmp.relics.wave.Wave28Rules.Ward;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the numbers and small state machines of wave 28 are plain Java so that every rule the cards were turned into (dice, saves, moon, dawn, charges)
 * is checked here without a running game.
 */
class Wave28RulesTest {
    @Test
    void chargesFillAtFirstSpendAndComeBackByTheirGain() {
        Charges c = new Charges(3, 3, 0L).at(10, 3);
        assertEquals(3, c.left());
        c = c.spend(2);
        assertEquals(1, c.left());
        assertFalse(c.canSpend(2));
        assertEquals(1, c.at(10, 3).left(), "same day: nothing back");
        assertEquals(3, c.at(11, 3).left(), "the next dawn refills a three charge pool");
        Charges sun = new Charges(10, 10, 0L).at(5, 0).spend(7);
        assertEquals(3, sun.at(50, 0).left(), "the sunheart never refills by the day");
        assertEquals(10, sun.add(99).left(), "its own act fills it, capped at ten");
        assertEquals(sun, sun.spend(4), "a refused spend changes nothing");
    }

    @Test
    void hellfireDiceAndAwakening() {
        for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) assertTrue(Wave28Rules.twoD4(a, b) >= 2 && Wave28Rules.twoD4(a, b) <= 8);
        assertEquals(8, Wave28Rules.twoD4(3, 3));
        assertEquals(2, Wave28Rules.twoD4(-5, -5));
        assertEquals(8, Wave28Rules.d8(7));
        assertEquals(1, Wave28Rules.d8(-3));
        assertTrue(Wave28Rules.shoves(8, 1));
        assertTrue(Wave28Rules.shoves(3, 8));
        assertFalse(Wave28Rules.shoves(7, 7));
        assertFalse(Wave28Rules.awakened(59));
        assertTrue(Wave28Rules.awakened(60));
        assertFalse(Wave28Rules.hardy(20.0));
        assertTrue(Wave28Rules.hardy(24.0));
        assertTrue(Wave28Rules.mediumOrSmaller(1.0, 1.0));
        assertFalse(Wave28Rules.mediumOrSmaller(5.0, 1.0));
    }

    @Test
    void fireboltGrowsAtTheCantripLevels() {
        assertEquals(1, Wave28Rules.fireboltDice(0));
        assertEquals(1, Wave28Rules.fireboltDice(4));
        assertEquals(2, Wave28Rules.fireboltDice(5));
        assertEquals(3, Wave28Rules.fireboltDice(11));
        assertEquals(4, Wave28Rules.fireboltDice(17));
        assertEquals(16f, Wave28Rules.fireboltDamage(40));
    }

    @Test
    void proficiencyFollowsWhatIsHeld() {
        assertFalse(Wave28Rules.proficiency(Held.NOTHING).any());
        assertTrue(Wave28Rules.proficiency(Held.BLADE).attack() > 0);
        assertEquals(0, Wave28Rules.proficiency(Held.AXE).hasteAmp());
        assertEquals(1, Wave28Rules.proficiency(Held.DIGGER).hasteAmp());
        assertEquals("tensura:sniper", Wave28Rules.proficiency(Held.RANGED).skill());
        assertTrue(Wave28Rules.proficiency(Held.SHIELD).armor() > 0);
        for (Held h : Held.values()) assertNotNull(Wave28Rules.proficiency(h).name());
    }

    @Test
    void gamelonHasTwelveSpellsInFourGems() {
        assertEquals(12, Wave28Rules.GAMELON_SLOTS);
        assertEquals(1, Wave28Rules.gamelonNext(0));
        assertEquals(0, Wave28Rules.gamelonNext(11));
        assertEquals(0, Wave28Rules.gem(2));
        assertEquals(1, Wave28Rules.gem(3));
        assertEquals(3, Wave28Rules.gem(11));
        assertEquals(2, Wave28Rules.spell(11));
        assertEquals(0, Wave28Rules.spell(-3 + 12));
        assertEquals("Water gem: Ice Bridge", Wave28Rules.gamelonName(6));
        for (int i = 0; i < 12; i++) assertFalse(Wave28Rules.gamelonName(i).isBlank());
    }

    @Test
    void chromaticRollsAndWards() {
        assertEquals(1, Wave28Rules.chromaRoll(0));
        assertEquals(6, Wave28Rules.chromaRoll(5));
        assertEquals(6, Wave28Rules.chromaRoll(99));
        assertEquals(Ward.ACID, Wave28Rules.wardOf(1, 3));
        assertEquals(Ward.COLD, Wave28Rules.wardOf(5, 1));
        assertEquals(Ward.FIRE, Wave28Rules.wardOf(6, 3), "a six takes the chosen ward");
        assertEquals(Ward.COLD, Wave28Rules.wardOf(6, 99));
        assertEquals(1.0, Wave28Rules.chromaArmor(6));
        assertEquals(0.0, Wave28Rules.chromaArmor(5));
        assertTrue(Wave28Rules.chromaActive(10, 11));
        assertFalse(Wave28Rules.chromaActive(11, 11));
    }

    @Test
    void dragonsKnowTheirKindButNotTheBosses() {
        assertTrue(Wave28Rules.dragonKin("red_dragon", 80));
        assertTrue(Wave28Rules.dragonKin("wyvern", 40));
        assertFalse(Wave28Rules.dragonKin("ender_dragon", 200), "the great dragon is a boss");
        assertFalse(Wave28Rules.dragonKin("zombie", 20));
        assertFalse(Wave28Rules.dragonKin(null, 20));
    }

    @Test
    void charmIsOneWisdomSaveAsExistence() {
        assertFalse(Wave28Rules.charmSaves(100, 100));
        assertFalse(Wave28Rules.charmSaves(200, 100));
        assertTrue(Wave28Rules.charmSaves(201, 100));
        assertTrue(Wave28Rules.charmable(20));
        assertFalse(Wave28Rules.charmable(300));
    }

    @Test
    void theWolfIsHeldUnderTheFullMoonOrAfterAVigil() {
        assertTrue(Wave28Rules.controlled(0, true, -1, 40), "full moon at night");
        assertFalse(Wave28Rules.controlled(0, false, -1, 40), "the full moon's daytime is not enough");
        assertFalse(Wave28Rules.controlled(3, true, -1, 43));
        long cycle = Wave28Rules.cycleOf(40);
        assertEquals(5, cycle);
        assertTrue(Wave28Rules.controlled(4, false, cycle, 43), "the vigil holds through the cycle");
        assertTrue(Wave28Rules.controlled(4, false, cycle, 47));
        assertFalse(Wave28Rules.controlled(4, false, cycle, 48), "and ends with the next one");
        assertEquals(Wave28Rules.FORM_FREE_TICKS, Wave28Rules.formTicks(true));
        assertEquals(Wave28Rules.FORM_CURSED_TICKS, Wave28Rules.formTicks(false));
        assertTrue(Wave28Rules.FORM_FREE_TICKS > Wave28Rules.FORM_CURSED_TICKS);
    }

    @Test
    void sunheartDawnAndCosts() {
        assertEquals(4, Wave28Rules.dawnRegain(0));
        assertEquals(11, Wave28Rules.dawnRegain(7));
        assertTrue(Wave28Rules.inDawn(0));
        assertTrue(Wave28Rules.inDawn(1499));
        assertFalse(Wave28Rules.inDawn(1500));
        assertFalse(Wave28Rules.inDawn(12000));
        assertTrue(Wave28Rules.inDawn(22500));
        assertTrue(Wave28Rules.inDawn(23999 + 24000));
        assertTrue(Wave28Rules.canGreetDawn(4, 5));
        assertFalse(Wave28Rules.canGreetDawn(5, 5));
        assertEquals(10, Wave28Rules.SUN_MAX);
        assertTrue(Wave28Rules.COST_REGENERATE <= Wave28Rules.SUN_MAX);
        assertEquals(0f, Wave28Rules.evilBurn(6f), "never under three hearts");
        assertEquals(4f, Wave28Rules.evilBurn(7f));
    }

    @Test
    void witcherHumAndCompass() {
        assertEquals(0, Wave28Rules.humLevel(0));
        assertEquals(1, Wave28Rules.humLevel(2));
        assertEquals(2, Wave28Rules.humLevel(5));
        assertEquals(3, Wave28Rules.humLevel(6));
        assertEquals("ahead", Wave28Rules.compass(0, 5, 0), "facing south, a thing to the south is ahead");
        assertEquals("behind you", Wave28Rules.compass(0, -5, 0));
        assertEquals("to your right", Wave28Rules.compass(-5, 0, 0), "facing south, west is on the right hand");
        assertEquals("to your left", Wave28Rules.compass(5, 0, 0));
        assertEquals("ahead", Wave28Rules.compass(5, 0, -90), "facing east");
    }

    @Test
    void pantaloonsFallOnlyOnANaturalOne() {
        assertTrue(Wave28Rules.naturalOne(0));
        for (int i = 1; i < 20; i++) assertFalse(Wave28Rules.naturalOne(i));
    }

    @Test
    void beheadedLethalBlowsLeaveOneHitPointOnceAtATime() {
        assertEquals(5f, Wave28Rules.beheadedDamage(20f, 5f, 100, 0), "a blow that does not kill is untouched");
        assertEquals(19f, Wave28Rules.beheadedDamage(20f, 25f, 100, 0), "a killing blow leaves one point");
        assertTrue(Wave28Rules.beheadedSaved(20f, 25f, 100, 0));
        assertEquals(25f, Wave28Rules.beheadedDamage(20f, 25f, 100, 150), "a second one inside the gap goes through");
        assertFalse(Wave28Rules.beheadedSaved(20f, 25f, 100, 150));
        assertEquals(0f, Wave28Rules.beheadedDamage(1f, 3f, 100, 0), "on one point the blow is nothing");
        assertEquals(5f, Wave28Rules.collapseDamage(10f));
        assertEquals(0f, Wave28Rules.collapseDamage(1f), "the collapse never takes the last point");
        assertEquals(3000, Wave28Rules.HEAD_TICKS);
    }

    @Test
    void thePoisonSaveReadsConstitutionFromHealth() {
        assertEquals(0, Wave28Rules.conMod(20));
        assertEquals(2, Wave28Rules.conMod(28));
        assertEquals(-1, Wave28Rules.conMod(16));
        assertTrue(Wave28Rules.failsSave(0, 0));
        assertTrue(Wave28Rules.failsSave(13, 0), "a 14 is under 15");
        assertFalse(Wave28Rules.failsSave(14, 0), "a 15 passes");
        assertFalse(Wave28Rules.failsSave(12, 2), "the modifier counts");
        assertEquals(20, Wave28Rules.Puke.values().length);
        assertEquals(Wave28Rules.Puke.RAINBOW_SLIME, Wave28Rules.puke(0));
        assertEquals(Wave28Rules.Puke.COFFIN, Wave28Rules.puke(19));
        assertEquals(Wave28Rules.Puke.COFFIN, Wave28Rules.puke(99));
        assertEquals(1, Wave28Rules.bloodDamage(0));
        assertEquals(4, Wave28Rules.bloodDamage(3));
        assertEquals(6, Wave28Rules.carrots(5));
        assertEquals(6, Wave28Rules.PUKE_COUNT);
    }
}
