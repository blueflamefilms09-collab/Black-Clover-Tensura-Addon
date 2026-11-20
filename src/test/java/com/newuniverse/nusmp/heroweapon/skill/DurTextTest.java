package com.newuniverse.nusmp.heroweapon.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Why: the Weapon Book must show the real length after the scale, and must never touch a number that was not scaled (a wind-up, a window). */
class DurTextTest {
    @Test
    void rewritesOnlyTheListedLength() {
        String text = "Lasts 8 seconds, then waits 3 seconds.";
        assertEquals("Lasts 16 seconds, then waits 3 seconds.", DurText.apply("skill.shield.cover", text, 2.0));
    }

    @Test
    void identityAtOneAndForUnlistedKeys() {
        String text = "Lasts 8 seconds.";
        assertEquals(text, DurText.apply("skill.shield.cover", text, 1.0));
        assertEquals(text, DurText.apply("skill.shield.unknown", text, 2.0));
        assertEquals(null, DurText.apply("skill.shield.cover", null, 2.0));
    }

    @Test
    void decimalsAndShortUnits() {
        assertEquals("rush 5 seconds", DurText.apply("skill.spear.filo_rush", "rush 2.5 seconds", 2.0));
        assertEquals("shield 8 s long", DurText.apply("passive.staff.wide_prism", "shield 4 s long", 2.0));
        assertEquals("wall 3 s, step 1 s", DurText.apply("passive.staff.deep_mirror", "wall 2 s, step 1 s", 1.5));
    }

    @Test
    void bothLengthsOfARule() {
        assertEquals("8 seconds or 12 seconds", DurText.apply("skill.mirror.release_rebound", "4 seconds or 6 seconds", 2.0));
    }

    @Test
    void keysAreWellFormed() {
        assertTrue(DurText.keys().size() > 30);
        for (String k : DurText.keys()) assertTrue(k.matches("(skill|passive)\\.[a-z_]+\\.[a-z_]+"), k);
    }
}
