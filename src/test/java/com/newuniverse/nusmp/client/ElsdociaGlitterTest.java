package com.newuniverse.nusmp.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElsdociaGlitterTest {
    @Test
    void farHoldersNeverGlitter() {
        assertEquals(0, ElsdociaGlitter.count(25.0 * 25.0, 0.0f));
        assertEquals(0, ElsdociaGlitter.count(1000.0 * 1000.0, 0.0f));
    }

    @Test
    void nearHoldersGlitterMoreOftenThanFarOnes() {
        int near = 0, far = 0;
        for (int i = 0; i < 1000; i++) {
            float roll = i / 1000f;
            near += ElsdociaGlitter.count(1.0, roll);
            far += ElsdociaGlitter.count(20.0 * 20.0, roll);
        }
        assertTrue(near > far, "near " + near + " far " + far);
        assertTrue(far > 0);
    }

    @Test
    void aSingleHolderNeverExceedsTwoPerTick() {
        for (int i = 0; i <= 1000; i++) assertTrue(ElsdociaGlitter.count(4.0, i / 1000f) <= 2);
    }

    @Test
    void particlesStayOnTheCrystal() {
        for (int i = 0; i <= 1000; i++) {
            float a = ElsdociaGlitter.along(i / 1000f);
            assertTrue(a >= ElsdociaGlitter.BLADE_START - 1e-4 && a <= ElsdociaGlitter.BLADE_END + 1e-4, "along " + a);
        }
        assertEquals(ElsdociaGlitter.BLADE_START, ElsdociaGlitter.along(0f), 1e-4);
        assertEquals(ElsdociaGlitter.BLADE_END, ElsdociaGlitter.along(1f), 1e-4);
    }

    @Test
    void theBudgetIsCapped() {
        assertTrue(ElsdociaGlitter.budgetLeft(0));
        assertTrue(ElsdociaGlitter.budgetLeft(ElsdociaGlitter.MAX_PER_TICK - 1));
        assertFalse(ElsdociaGlitter.budgetLeft(ElsdociaGlitter.MAX_PER_TICK));
    }
}
