package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Cape of Nine Lives. */
class Wave2LivesTest {
    @Test
    void nineSavesAndTheCapeIsSpent() {
        Wave2Lives s = Wave2Lives.fresh();
        assertEquals(9, s.left());
        assertTrue(s.alive());
        for (int i = 1; i <= 9; i++) {
            Wave2Lives before = s;
            s = s.afterSave();
            assertEquals(9 - i, s.left());
            assertEquals(i, s.cat());
            assertEquals(i == 9, s.justSpent(before), "spent exactly on the ninth");
        }
        assertFalse(s.alive());
        assertEquals(0, s.afterSave().left(), "never below zero");
        assertEquals(9, s.afterSave().cat(), "never above nine");
    }

    @Test
    void removeCurseLiftsTheCatButNotTheLostLives() {
        Wave2Lives s = Wave2Lives.fresh().afterSave().afterSave().afterSave().afterSave();
        assertEquals(5, s.left());
        assertEquals(4, s.cat());
        Wave2Lives c = s.cured();
        assertEquals(5, c.left());
        assertEquals(0, c.cat());
        assertEquals(0.0, c.healthPenalty());
        assertEquals(Wave2Lives.fresh(), s.wished());
    }

    @Test
    void theCatPenaltyIsHalfAHeartPerTraitUpToFourHearts() {
        Wave2Lives s = Wave2Lives.fresh();
        assertEquals(0.0, s.healthPenalty());
        assertFalse(s.catEyes());
        s = s.afterSave();
        assertEquals(-0.5, s.healthPenalty(), 1e-9);
        assertTrue(s.catEyes());
        for (int i = 0; i < 8; i++) s = s.afterSave();
        assertEquals(-4.0, s.healthPenalty(), 1e-9);
    }

    @Test
    void theUrgeToKnockThingsOverStartsAtTheFourthTrait() {
        Wave2Lives s = Wave2Lives.fresh();
        for (int i = 1; i <= 9; i++) {
            s = s.afterSave();
            if (i < 4) {
                assertFalse(s.knocksThingsOver());
                assertEquals(0.0, s.knockChance());
            } else {
                assertTrue(s.knocksThingsOver());
                assertTrue(s.knockChance() >= 0.2 && s.knockChance() <= 0.45, "" + s.knockChance());
            }
        }
    }

    @Test
    void constructorClamps() {
        assertEquals(9, new Wave2Lives(40, 0).left());
        assertEquals(0, new Wave2Lives(-3, 0).left());
        assertEquals(9, new Wave2Lives(5, 99).cat());
    }
}
