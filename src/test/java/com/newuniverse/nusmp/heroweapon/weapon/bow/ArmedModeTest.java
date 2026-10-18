package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the armed mode is stored in the bond as text, so an old or hand-edited save must read back as "nothing armed" and never throw, every mode must
 * survive the round trip through its id, and the thresholds (the sniper arrow needs a nearly full draw, the others less) are part of how the Bow plays.
 */
class ArmedModeTest {
    @Test
    void everyModeSurvivesItsOwnId() {
        Set<String> ids = new HashSet<>();
        for (ArmedMode m : ArmedMode.values()) {
            assertEquals(m, ArmedMode.byId(m.id()));
            assertTrue(ids.add(m.id()), "duplicate id " + m.id());
        }
        assertEquals(8, ArmedMode.values().length);
    }

    @Test
    void unknownOrMissingTextIsNothingArmed() {
        assertEquals(ArmedMode.NONE, ArmedMode.byId(null));
        assertEquals(ArmedMode.NONE, ArmedMode.byId(""));
        assertEquals(ArmedMode.NONE, ArmedMode.byId("flamethrower"));
        assertEquals(ArmedMode.SNIPER, ArmedMode.byId("SNIPER"), "case does not matter");
    }

    @Test
    void theThresholdsAreInTheRightOrder() {
        assertEquals(0.0, ArmedMode.NONE.minFraction(), 1e-9);
        assertTrue(ArmedMode.SNIPER.minFraction() >= 0.9, "the sniper arrow needs a nearly full draw");
        for (ArmedMode m : ArmedMode.values()) {
            assertTrue(m.minFraction() >= 0.0 && m.minFraction() <= 1.0, m.id());
            if (m != ArmedMode.NONE) assertTrue(m.minFraction() >= DrawMath.MIN_FRACTION, m.id() + " must be at least a draw that shoots");
            if (m != ArmedMode.NONE) assertTrue(!m.label().isEmpty(), m.id() + " has a label");
        }
    }
}
