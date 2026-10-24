package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.companion.follow.TravelRules;
import com.newuniverse.nusmp.companion.talk.BubbleRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: how many followers cross a dimension with the player, how fast a speaker may talk and how long a bubble stays are the bounds that keep the system cheap on a busy server. */
class TravelAndBubbleRulesTest {
    @Test
    void followersCarriedAreCappedByTheSettingAndTheHardCap() {
        assertEquals(2, TravelRules.carried(5, 2));
        assertEquals(1, TravelRules.carried(1, 4));
        assertEquals(6, TravelRules.carried(50, 50), "never more than the hard cap");
        assertEquals(0, TravelRules.carried(5, 0));
        assertEquals(0, TravelRules.carried(5, -3));
        assertEquals(0, TravelRules.carried(0, 4));
    }

    @Test
    void collectingAndTeleportingDistances() {
        assertTrue(TravelRules.withinCollect(32.0 * 32.0));
        assertFalse(TravelRules.withinCollect(32.0 * 32.0 + 1));
        assertTrue(TravelRules.tooFarBehind(24.0 * 24.0 + 1));
        assertFalse(TravelRules.tooFarBehind(24.0 * 24.0));
    }

    @Test
    void aCrossingRecordGoesStaleAfterFiveSeconds() {
        assertFalse(TravelRules.stale(1000, 1100));
        assertTrue(TravelRules.stale(1000, 1101));
        assertTrue(TravelRules.stale(1000, 900), "a clock that went back");
    }

    @Test
    void bubbleLifeGrowsWithTheLineButStaysBounded() {
        assertEquals(BubbleRules.MIN_LIFE, BubbleRules.lifeTicks(0));
        assertEquals(BubbleRules.MIN_LIFE, BubbleRules.lifeTicks(5));
        assertTrue(BubbleRules.lifeTicks(40) > BubbleRules.MIN_LIFE);
        assertEquals(BubbleRules.MAX_LIFE, BubbleRules.lifeTicks(10_000));
        assertEquals(BubbleRules.MIN_LIFE, BubbleRules.lifeTicks(-9));
    }

    @Test
    void bubbleTextIsCutWithAnEllipsis() {
        assertEquals("", BubbleRules.cut(null));
        assertEquals("hi", BubbleRules.cut("  hi  "));
        String cut = BubbleRules.cut("w".repeat(500));
        assertEquals(BubbleRules.MAX_CHARS, cut.length());
        assertTrue(cut.endsWith("…"));
    }

    @Test
    void aSpeakerMaySpeakEveryTwoSeconds() {
        assertFalse(BubbleRules.mayFor(100, 139));
        assertTrue(BubbleRules.mayFor(100, 140));
        assertTrue(BubbleRules.mayFor(100, 50), "a clock that went back");
        assertTrue(BubbleRules.inDrawRange(24.0 * 24.0));
        assertFalse(BubbleRules.inDrawRange(24.0 * 24.0 + 1));
    }
}
