package com.newuniverse.nusmp.skill.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: All Fiction was rebuilt from two modes to ten (3.46.0). The first two must stay exactly where they were, the rest must be bounded, and the switch must restore the old wheel. */
class FictionRebuildRulesTest {
    @Test
    void theOldTwoModesKeepTheirPlacesIdsAndNumbers() {
        assertEquals(0, FictionRules.UNDO);
        assertEquals(1, FictionRules.ERASE);
        assertEquals(2, FictionRules.FICTION_DEATH, "the old constant is kept");
        assertEquals("fiction.undo", FictionRules.ALL_IDS[FictionRules.UNDO]);
        assertEquals("fiction.erase", FictionRules.ALL_IDS[FictionRules.ERASE]);
        assertEquals(FictionRules.undoCost(), FictionRules.cost(FictionRules.UNDO));
        assertEquals(FictionRules.eraseCost(), FictionRules.cost(FictionRules.ERASE));
        assertEquals(FictionRules.undoCooldown(), FictionRules.cooldown(FictionRules.UNDO));
        assertEquals(FictionRules.eraseCooldown(), FictionRules.cooldown(FictionRules.ERASE));
        assertEquals(2, FictionRules.modes(), "Book Maker still has its two modes");
    }

    @Test
    void theWheelIsTenWhenRebuiltAndTwoWhenSwitchedOff() {
        assertEquals(10, FictionRules.wheel(true));
        assertEquals(2, FictionRules.wheel(false));
        assertEquals(FictionRules.ALL_MODES, FictionRules.ALL_IDS.length);
        Set<String> ids = new HashSet<>();
        for (String id : FictionRules.ALL_IDS) assertTrue(ids.add(id), "duplicate mode id " + id);
        assertEquals(2, FictionRules.ANNUL);
        assertEquals(9, FictionRules.WORLD, "new modes were only appended");
    }

    @Test
    void everyNewModeCostsAndWaitsExceptStandUp() {
        for (int m = 0; m < FictionRules.ALL_MODES; m++) {
            if (m == FictionRules.STAND_UP) { assertEquals(0.0, FictionRules.cost(m)); continue; }
            assertTrue(FictionRules.cost(m) > 0 && FictionRules.cost(m) <= 10000, "cost of mode " + m);
            assertTrue(FictionRules.cooldown(m) > 0 && FictionRules.cooldown(m) <= 6000, "cooldown of mode " + m);
        }
        assertTrue(FictionRules.cost(FictionRules.WORLD) > FictionRules.cost(FictionRules.ANNUL), "the zone is the dearest of the new ones");
    }

    @Test
    void annulTakesHitsOutOfAPoolOfHalfTheMaxHealth() {
        assertEquals(10.0, FictionRules.annulPool(20.0), 1e-9);
        assertEquals(0.0, FictionRules.annulPool(-5.0));
        assertEquals(4.0, FictionRules.annulAbsorb(10.0, 4.0), 1e-9);
        assertEquals(6.0, FictionRules.annulAbsorb(6.0, 30.0), 1e-9, "never more than the pool");
        assertEquals(0.0, FictionRules.annulAbsorb(0.0, 5.0));
        assertEquals(0.0, FictionRules.annulAbsorb(5.0, -2.0), "a negative hit absorbs nothing");
        assertTrue(FictionRules.annulTicks() <= 200 && FictionRules.nullifyTicks() <= 400);
    }

    @Test
    void rewindOnlyGoesToAPlaceBetweenOneAndAHalfAndFortyEightBlocksAway() {
        assertFalse(FictionRules.rewindOk(0.5));
        assertFalse(FictionRules.rewindOk(1.49));
        assertTrue(FictionRules.rewindOk(1.5));
        assertTrue(FictionRules.rewindOk(48.0));
        assertFalse(FictionRules.rewindOk(48.01));
    }

    @Test
    void windowsAreOpenUntilTheirEndTime() {
        assertTrue(FictionRules.windowOpen(100, 99));
        assertFalse(FictionRules.windowOpen(100, 100));
        assertFalse(FictionRules.windowOpen(0, 5));
    }

    @Test
    void theZonePulsesOnceASecondWhileItIsOpen() {
        assertTrue(FictionRules.worldPulseDue(500, 100, 40));
        assertFalse(FictionRules.worldPulseDue(500, 100, 41), "only on a whole second of the owner's tick count");
        assertFalse(FictionRules.worldPulseDue(100, 100, 40), "closed");
        int pulses = FictionRules.worldTicks() / 20;
        assertEquals(6, pulses, "six pulses over six seconds");
    }

    @Test
    void perPressCapsStayBounded() {
        assertTrue(FictionRules.unmakeMax() <= 32 && FictionRules.worldPulseMaxProjectiles() <= 16 && FictionRules.shareMax() <= 8);
        assertTrue(FictionRules.unmakeRadius() <= 12 && FictionRules.worldRadius() <= 10 && FictionRules.shareRadius() <= 10 && FictionRules.unpickRange() <= 32);
        assertTrue(FictionRules.shareHeal() > 0 && FictionRules.shareHeal() <= 0.5);
        assertTrue(FictionRules.rewindMax() <= 64);
    }

    @Test
    void standUpReportsTheSecondsLeft() {
        assertEquals(0, FictionRules.standUpSeconds(0, 500));
        assertEquals(0, FictionRules.standUpSeconds(500, 500));
        assertEquals(1, FictionRules.standUpSeconds(501, 500));
        assertEquals(1, FictionRules.standUpSeconds(520, 500));
        assertEquals(2, FictionRules.standUpSeconds(521, 500));
        assertEquals(900, FictionRules.standUpSeconds(18000, 0));
    }

    @Test
    void positionHistoryShiftsNewestFirst() {
        double[] h = {1, 2, 3, 4, 5};
        FictionRules.pushD(h, 9);
        assertArrayEquals(new double[]{9, 1, 2, 3, 4}, h);
        double[] empty = {};
        FictionRules.pushD(empty, 1);
        assertEquals(0, empty.length);
    }
}
