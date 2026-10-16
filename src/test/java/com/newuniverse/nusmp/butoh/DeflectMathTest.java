package com.newuniverse.nusmp.butoh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The deflect dash numbers: a 12 tick window, a stronger return while dancing, a cap on how often one missile is turned. */
class DeflectMathTest {
    @Test
    void theWindowIsTwelveTicksAndTheCooldownAboutSixSeconds() {
        assertEquals(12, DeflectMath.WINDOW_TICKS);
        assertEquals(120, DeflectMath.COOLDOWN_TICKS);
        long dash = 1000, until = dash + DeflectMath.WINDOW_TICKS;
        assertTrue(DeflectMath.open(dash, until));
        assertTrue(DeflectMath.open(until, until));
        assertFalse(DeflectMath.open(until + 1, until));
        assertFalse(DeflectMath.open(5, 0), "no dash yet means no window");
    }

    @Test
    void danceMakesItStronger() {
        assertTrue(DeflectMath.speedFactor(true) > DeflectMath.speedFactor(false));
        assertTrue(DeflectMath.spreadDegrees(true) < DeflectMath.spreadDegrees(false), "a dancer's return is tighter");
        assertTrue(DeflectMath.dashDistance(true) > DeflectMath.dashDistance(false));
    }

    @Test
    void aMissileIsTurnedAtMostTwice() {
        assertTrue(DeflectMath.mayTurn(0));
        assertTrue(DeflectMath.mayTurn(1));
        assertFalse(DeflectMath.mayTurn(2));
    }
}
