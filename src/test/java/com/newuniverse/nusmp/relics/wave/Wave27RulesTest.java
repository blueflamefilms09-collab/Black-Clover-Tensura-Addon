package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 27 of the Relics of Many Worlds: the pure rules of the headgear and gloves (dawn charges, weekly and daily uses, alignments, fumbles, compliments). */
class Wave27RulesTest {
    @Test
    void chargesComeBackAtDawnUpToTheMaximum() {
        Wave27Rules.Charges c = Wave27Rules.Charges.fresh(3);
        assertEquals(3, c.at(5, 2).left(), "a never-touched pool reads full");
        Wave27Rules.Charges used = c.at(5, 2).spend(3);
        assertEquals(0, used.left());
        assertEquals(0, used.at(5, 2).left(), "same day: nothing back");
        assertEquals(2, used.at(6, 2).left(), "one dawn: the gain");
        assertEquals(3, used.at(9, 2).left(), "many dawns: capped");
        assertFalse(used.canSpend(1));
        assertSame(used, used.spend(1), "cannot spend what is not there");
    }

    @Test
    void dawnRollsAreOneToThreeAndTwoToFive() {
        assertEquals(1, Wave27Rules.dawnGainD3(0));
        assertEquals(3, Wave27Rules.dawnGainD3(2));
        assertEquals(3, Wave27Rules.dawnGainD3(99));
        assertEquals(2, Wave27Rules.dawnGainD4p1(0));
        assertEquals(5, Wave27Rules.dawnGainD4p1(3));
        assertEquals(2, Wave27Rules.dawnGainD4p1(-4));
        assertEquals(9, Wave27Rules.STORM_MAX);
        assertEquals(3, Wave27Rules.LIGHTNING_COST, "Call Lightning is three charges, Thunderwave one");
    }

    @Test
    void dailyAndWeeklyUses() {
        long today = Wave27Rules.dayOf(5 * 24000L + 100);
        assertEquals(5, today);
        assertTrue(Wave27Rules.dailyReady(0, today), "never used");
        long mark = Wave27Rules.dailyMark(today);
        assertFalse(Wave27Rules.dailyReady(mark, today), "used today");
        assertTrue(Wave27Rules.dailyReady(mark, today + 1), "ready at the next dawn");
        assertEquals(-1, Wave27Rules.dayOf(-1), "floor, not truncation");
        assertFalse(Wave27Rules.weekReady(1000, 999));
        assertTrue(Wave27Rules.weekReady(1000, 1000));
        assertEquals(168000L, Wave27Rules.WEEK);
    }

    @Test
    void alignmentsReadFromWhatIsKnown() {
        assertEquals("Lawful Good", Wave27Rules.alignment(false, true, false, false, 0, false).label());
        assertEquals("Lawful Evil", Wave27Rules.alignment(false, false, true, true, 0, false).label());
        assertEquals("Chaotic Evil", Wave27Rules.alignment(false, false, false, true, 0, false).label());
        assertEquals("True Neutral", Wave27Rules.alignment(false, false, false, false, 0, false).label());
        assertEquals("Lawful Good", Wave27Rules.alignment(true, false, false, false, 0, false).label());
        assertEquals("Lawful Neutral", Wave27Rules.alignment(true, false, false, false, 2, false).label());
        assertEquals("Lawful Evil", Wave27Rules.alignment(true, false, false, false, 3, false).label());
        assertEquals("Chaotic Evil", Wave27Rules.alignment(true, false, false, false, 0, true).label(), "a curse is chaotic and evil");
        assertTrue(Wave27Rules.tooDeep(201, 100));
        assertFalse(Wave27Rules.tooDeep(200, 100));
    }

    @Test
    void theCrownCursesOnlyAfterConstantWear() {
        long worn = 0, last = 0, now = 1000;
        for (int i = 0; i < 1; i++) worn = Wave27Rules.wornAfter(worn, last, now);
        assertEquals(20, worn);
        assertEquals(0, Wave27Rules.exhaustion(worn));
        assertEquals(1, Wave27Rules.exhaustion(Wave27Rules.NIGHTMARE_AFTER));
        assertEquals(0, Wave27Rules.exhaustion(Wave27Rules.NIGHTMARE_AFTER - 1));
        assertEquals(2, Wave27Rules.exhaustion(Wave27Rules.EXHAUSTED_AFTER));
        assertEquals(2, Wave27Rules.exhaustion(Long.MAX_VALUE / 2), "never more than 2");
        assertEquals(20 + 100, Wave27Rules.wornAfter(100, 900, 1000), "worn on a second ago: keeps counting");
        assertEquals(20, Wave27Rules.wornAfter(100000, 1000, 1000 + 24001), "off for over a day: starts again");
        assertEquals(100020, Wave27Rules.wornAfter(100000, 1000, 1000 + 24000), "exactly a day is still constant wear");
    }

    @Test
    void auraLinesAreReadable() {
        assertEquals("", Wave27Rules.level(0));
        assertEquals(" II", Wave27Rules.level(1));
        assertEquals(" X", Wave27Rules.level(9));
        assertEquals(" 12", Wave27Rules.level(11));
        assertEquals("Zombie: Speed II, Strength", Wave27Rules.auraLine("Zombie", List.of("Speed II", "Strength")));
        assertEquals(24.0, Wave27Rules.ORB_RADIUS, 1e-9, "120 ft at 5 ft a block");
    }

    @Test
    void pickpocketOddsAreFairAndBounded() {
        assertEquals(0.8, Wave27Rules.stealChance(0), 1e-9);
        assertEquals(0.6, Wave27Rules.stealChance(20), 1e-9);
        assertEquals(0.25, Wave27Rules.stealChance(500), 1e-9);
        assertTrue(Wave27Rules.stealChance(20) > Wave27Rules.stealChance(100), "the frail are easier");
        assertTrue(Wave27Rules.steals(0.6, 59));
        assertFalse(Wave27Rules.steals(0.6, 60));
        assertTrue(Wave27Rules.fumbles(4));
        assertFalse(Wave27Rules.fumbles(5), "one in twenty");
    }

    @Test
    void theConeAndTheMimic() {
        assertTrue(Wave27Rules.inCone(3, 0, 0, 1, 0, 0, 5, 0.6), "dead ahead");
        assertFalse(Wave27Rules.inCone(0, 0, 3, 1, 0, 0, 5, 0.6), "to the side");
        assertFalse(Wave27Rules.inCone(6, 0, 0, 1, 0, 0, 5, 0.6), "too far");
        assertTrue(Wave27Rules.inCone(0, 0, 0, 1, 0, 0, 5, 0.6), "on top of you");
        assertEquals(Wave27Rules.Shape.PIERCING, Wave27Rules.Shape.BLUDGEONING.next());
        assertEquals(Wave27Rules.Shape.BLUDGEONING, Wave27Rules.Shape.SLASHING.next());
        assertEquals(Wave27Rules.Shape.SLASHING, Wave27Rules.Shape.of(99));
        assertEquals(3.5f, Wave27Rules.mimicDamage(Wave27Rules.Shape.SLASHING, 20), 1e-6);
        assertEquals(5.5f, Wave27Rules.mimicDamage(Wave27Rules.Shape.PIERCING, 8), 1e-6);
        assertEquals(3.5f, Wave27Rules.mimicDamage(Wave27Rules.Shape.PIERCING, 7), 1e-6);
        assertFalse(Wave27Rules.canSap(6f), "the hungry gauntlet never takes the wearer under two hearts");
        assertTrue(Wave27Rules.canSap(6.5f));
    }

    @Test
    void complimentsAreRecognisedAndRationed() {
        assertTrue(Wave27Rules.compliment("Thank you so much Steve!", "steve"));
        assertTrue(Wave27Rules.compliment("steve you were AMAZING back there", "Steve"));
        assertFalse(Wave27Rules.compliment("nice day", "Steve"), "must name the wearer");
        assertFalse(Wave27Rules.compliment("Steve go away", "Steve"), "no kind word");
        assertFalse(Wave27Rules.compliment(null, "Steve"));
        assertFalse(Wave27Rules.compliment("thanks", ""));
        assertTrue(Wave27Rules.giftAccepted(0, 0, 100));
        assertFalse(Wave27Rules.giftAccepted(5, 0, 100), "full glove");
        assertFalse(Wave27Rules.giftAccepted(1, 100, 100 + Wave27Rules.GLOVE_GAP - 1), "same giver too soon");
        assertTrue(Wave27Rules.giftAccepted(1, 100, 100 + Wave27Rules.GLOVE_GAP));
    }

    @Test
    void stunnedMeansHeldStill() {
        assertTrue(Wave27Rules.stunned(true, false, false, -1));
        assertTrue(Wave27Rules.stunned(false, false, true, -1));
        assertTrue(Wave27Rules.stunned(false, false, false, 4));
        assertFalse(Wave27Rules.stunned(false, false, false, 3));
        assertFalse(Wave27Rules.stunned(false, false, false, -1));
    }
}
