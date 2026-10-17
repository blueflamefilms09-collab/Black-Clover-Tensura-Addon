package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the numbers of the fight must grow with the acts, stay in sane ranges and be shared by the telegraph and the blow, and one wrong number would be felt by every player. */
class ZagredMathTest {
    @Test
    void theActFollowsThePowerSteps() {
        assertEquals(1, ZagredMath.act(20));
        assertEquals(1, ZagredMath.act(39));
        assertEquals(2, ZagredMath.act(40));
        assertEquals(3, ZagredMath.act(60));
        assertEquals(4, ZagredMath.act(80));
        assertEquals(5, ZagredMath.act(100));
        for (int a = 1; a <= 5; a++) assertFalse(ZagredMath.actName(a).isBlank());
    }

    @Test
    void theHaltBandHasAWayOutOnBothSides() {
        for (int stage = 0; stage <= 4; stage++) {
            assertTrue(ZagredMath.haltOuter(stage) > ZagredMath.haltCore() + 4, "the band is wide enough to matter at stage " + stage);
            assertTrue(ZagredMath.haltOuter(stage) <= 14, "and not wider than a player can leave in time");
        }
        assertTrue(ZagredMath.haltOuter(4) > ZagredMath.haltOuter(0));
        assertTrue(ZagredMath.haltBindTicks() <= 40, "a bind is short; the control law shortens it further");
    }

    @Test
    void theWaveReachesFarAndMovesAtAJumpablePace() {
        assertEquals(8, ZagredMath.fallRings(0));
        assertEquals(12, ZagredMath.fallRings(4));
        assertEquals(2.0, ZagredMath.fallRingRadius(0), 1e-9);
        assertEquals(1.6, ZagredMath.fallRingRadius(1) - ZagredMath.fallRingRadius(0), 1e-9);
        assertTrue(ZagredMath.fallRingRadius(ZagredMath.fallRings(0) - 1) >= 12, "it runs out past the Halt band");
        assertTrue(ZagredMath.fallHeight() < 1.2, "a normal jump clears it (a jump rises about 1.25 blocks)");
        assertTrue(ZagredMath.fallPullReach(0) >= ZagredMath.haltOuter(0));
    }

    @Test
    void swordsScaleWithTheCastSlotsWithinBounds() {
        assertEquals(8, ZagredMath.swordCount(1));
        assertEquals(14, ZagredMath.swordCount(4));
        assertEquals(8, ZagredMath.swordCount(0), "below one slot counts as one");
        assertEquals(14, ZagredMath.swordCount(9), "above four counts as four");
        assertTrue(ZagredMath.swordSpread(4) > ZagredMath.swordSpread(1));
        assertTrue(ZagredMath.swordSpread(1) > ZagredMath.swordImpact() * 2, "room for the marks to be apart");
    }

    @Test
    void theSentenceGrowsAndIsWrittenTwiceAtTheGates() {
        assertEquals(5, ZagredMath.redactLetters(0));
        assertEquals(5, ZagredMath.redactLetters(1));
        assertEquals(6, ZagredMath.redactLetters(2));
        assertEquals(7, ZagredMath.redactLetters(3));
        assertEquals(9, ZagredMath.redactLetters(4));
        assertEquals(1, ZagredMath.sentences(false));
        assertEquals(2, ZagredMath.sentences(true));
        assertTrue(ZagredMath.letterRadius(true) > ZagredMath.letterRadius(false));
    }

    @Test
    void theWardHasLayersUntilTheGatesWhereTheHeartTakesOver() {
        assertEquals(3, ZagredMath.wardLayers(0));
        assertEquals(3, ZagredMath.wardLayers(1));
        assertEquals(3, ZagredMath.wardLayers(2));
        assertEquals(4, ZagredMath.wardLayers(3));
        assertEquals(0, ZagredMath.wardLayers(4));
        for (int s = 0; s <= 3; s++) assertTrue(ZagredMath.wardLayers(s) * ZagredMath.wardLayerShare() < 0.25f, "the ward is a shell, not a second life");
        assertTrue(ZagredMath.wardLayers(3) <= ZagredWard.MAX);
    }

    @Test
    void thePagesScaleWithThePartyAndTheAct() {
        assertEquals(0, ZagredMath.lesserCap(0, 3));
        assertEquals(0, ZagredMath.greaterCap(1, 3));
        assertEquals(0, ZagredMath.archCap(2));
        assertEquals(1, ZagredMath.archCap(3));
        assertEquals(2, ZagredMath.archCap(4));
        for (int s = 1; s < 4; s++) assertTrue(ZagredMath.lesserCap(s + 1, 2) >= ZagredMath.lesserCap(s, 2), "more pages in later acts");
        assertTrue(ZagredMath.lesserCap(3, 5) > ZagredMath.lesserCap(3, 1), "more fighters, more pages");
        assertTrue(ZagredMath.lesserCap(1, 1) >= 1, "at least one page once they are allowed");
        assertEquals(0.6 + 0.4, ZagredMath.partyScale(1), 1e-9);
        assertEquals(1.5, ZagredMath.partyScale(40), 1e-9, "capped");
        assertTrue(ZagredMath.lesserCap(4, 40) <= 12, "never the swarm of the old fight");
    }

    @Test
    void theRuleAndTheStonesMatchTheTwelveSecondsOfTheDesign() {
        assertEquals(240, ZagredMath.ruleTicks());
        assertEquals(5, ZagredMath.stones());
        assertEquals(3, ZagredMath.stonesNeeded());
        assertTrue(ZagredMath.stonesNeeded() < ZagredMath.stones());
        assertTrue(ZagredMath.ruleGraceTicks() >= 20, "the rule bites after a full second of breaking it");
    }

    @Test
    void crowdsAndWindowsAreSane() {
        assertTrue(ZagredMath.crowdSize() >= 3 && ZagredMath.crowdSize() <= 5);
        assertTrue(ZagredMath.banishRadius() > ZagredMath.crowdRadius(), "the shove covers the crowd that triggers it");
        assertTrue(ZagredMath.shatterTicks() <= 60 && ZagredMath.reverseTicks() <= 100, "the defensive Words are short windows");
        assertEquals(0.5f, ZagredMath.reflectShare(), 1e-9f);
        assertTrue(ZagredMath.pagesPerCall(1) <= ZagredMath.pagesPerCall(4));
    }

    @Test
    void damageBasesAreOrderedLikeTheFight() {
        assertTrue(ZagredMath.DEVIL_CLAW > ZagredMath.CLAW, "the devil hits harder");
        assertTrue(ZagredMath.CLAW > ZagredMath.TRIDENT && ZagredMath.TRIDENT > ZagredMath.LETTER, "the claw is the heavy blow, the trident the heavy ranged one");
        assertTrue(ZagredMath.SWORD * ZagredMath.swordCount(4) > ZagredMath.TRIDENT, "a whole storm is worth more than one lance, if it all lands");
        assertTrue(ZagredMath.SEAL < ZagredMath.TRIDENT, "the seal is about the lock, not the damage");
        assertTrue(ZagredMath.RULE_BREAK < ZagredMath.LETTER);
        assertTrue(ZagredMath.LAST_LETTER_FACTOR > 1f);
    }

    @Test
    void hisSpeechHasARhythmThatNeverRunsAwayAndNeverDrops() {
        for (int stage = 0; stage <= 4; stage++) {
            assertTrue(ZagredMath.wordGap(stage) >= 70, "at least three and a half seconds of quiet between two Words");
            if (stage > 0) assertTrue(ZagredMath.wordGap(stage) <= ZagredMath.wordGap(stage - 1), "the fight never slows down");
        }
        assertEquals(ZagredMath.wordGap(0), ZagredMath.wordGap(-5), "a stage below the first is the first");
        assertEquals(ZagredMath.wordGap(4), ZagredMath.wordGap(99), "a stage above the last is the last");
    }

    @Test
    void theClawsReachIsShorterForTheManAndTheLeapIsLongerThanBoth() {
        assertTrue(ZagredMath.clawReach(true) > ZagredMath.clawReach(false));
        assertTrue(ZagredMath.clawLeap() > ZagredMath.clawReach(true), "the leap is what closes the distance");
    }
}
