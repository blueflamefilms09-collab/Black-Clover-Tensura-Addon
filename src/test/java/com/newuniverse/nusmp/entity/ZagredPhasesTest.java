package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.boss.refill.PhaseRefill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Zagred's four acts and the second health bar of the last one (1.35.0). */
class ZagredPhasesTest {
    @Test
    void theActsFollowTheBarUntilTheSecondBarIsDrawn() {
        assertEquals(1, ZagredPhases.phaseOf(1.0f, false));
        assertEquals(1, ZagredPhases.phaseOf(0.76f, false));
        assertEquals(2, ZagredPhases.phaseOf(0.75f, false));
        assertEquals(3, ZagredPhases.phaseOf(0.5f, false));
        assertEquals(4, ZagredPhases.phaseOf(0.25f, false));
        assertEquals(4, ZagredPhases.phaseOf(0.01f, false));
    }

    @Test
    void onceTheSecondBarIsDrawnTheLastActHoldsWhateverTheHealth() {
        for (float f = 0.02f; f <= 1.0f; f += 0.07f) assertEquals(4, ZagredPhases.phaseOf(f, true), "a refilled bar must not send him back to act I");
    }

    @Test
    void theSecondBarStartsOnceOnEnteringTheLastAct() {
        assertTrue(ZagredPhases.startsSecondBar(4, false));
        assertFalse(ZagredPhases.startsSecondBar(4, true));
        assertFalse(ZagredPhases.startsSecondBar(3, false));
        assertFalse(ZagredPhases.startsSecondBar(2, false));
    }

    @Test
    void theRefillRunsOnceAndFillsTheBar() {
        PhaseRefill r = new PhaseRefill();
        float max = 800f, hp = 190f;                                // a quarter of the bar: act IV is entered
        boolean second = false;
        int ph = ZagredPhases.phaseOf(hp / max, second);
        assertEquals(4, ph);
        assertTrue(ZagredPhases.startsSecondBar(ph, second));
        assertTrue(r.begin("final", hp, max, PhaseRefill.DEFAULT_TICKS));
        second = true;
        int ticks = 0;
        float last = hp;
        while (r.active()) {
            float h = r.tick();
            assertTrue(h >= last - 1e-3f, "health only rises during the refill");
            assertEquals(4, ZagredPhases.phaseOf(Math.max(h, 0f) / max, second));
            last = Math.max(last, h);
            ticks++;
            assertTrue(ticks <= PhaseRefill.DEFAULT_TICKS + 1);
        }
        assertEquals(max, last, 1e-3f, "the bar ends full");
        assertFalse(r.begin("final", 100f, max, 40), "never a second refill in one fight");
        assertFalse(ZagredPhases.startsSecondBar(4, second));
    }

    @Test
    void theBuffsAreNumbers() {
        assertEquals(5, ZagredPhases.redactLetters(1));
        assertEquals(7, ZagredPhases.redactLetters(3));
        assertEquals(9, ZagredPhases.redactLetters(4), "act IV writes two letters more than before");
        assertEquals(8, ZagredPhases.echoLetters(4));
        assertTrue(ZagredPhases.echoLetters(1) >= 2);
        assertEquals(0.6f, ZagredPhases.cooldownScale(4), 1e-6f);
        assertTrue(ZagredPhases.cooldownScale(4) < ZagredPhases.cooldownScale(3));
        assertTrue(ZagredPhases.cooldownScale(3) < ZagredPhases.cooldownScale(2));
    }
}
