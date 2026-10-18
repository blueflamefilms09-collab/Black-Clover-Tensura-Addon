package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: this small rule decides whether the second and third cut of a flurry do any damage at all, and whether a flurry can stack on a player, so the edges are pinned
 * with numbers: a foe that is already open stays as it is, a mob or a single strike is cut to ten ticks so the blow lands in full, a rapid series on a player keeps
 * the vanilla immunity, and nothing is ever raised.
 */
class ImmunityTest {
    @Test
    void aFoeThatIsAlreadyOpenStaysAsItIs() {
        for (int t = 0; t <= 10; t++) {
            for (boolean rapid : new boolean[] {false, true}) {
                for (boolean player : new boolean[] {false, true}) assertEquals(t, Immunity.before(t, rapid, player), "at " + t);
            }
        }
    }

    @Test
    void aMobOrASingleStrikeIsCutSoTheBlowLandsInFull() {
        assertEquals(10, Immunity.before(20, false, false));
        assertEquals(10, Immunity.before(20, true, false), "a flurry on a mob lands every cut");
        assertEquals(10, Immunity.before(20, false, true), "one strike on a player lands in full");
        assertEquals(10, Immunity.before(11, false, false));
    }

    @Test
    void aRapidSeriesOnAPlayerKeepsTheVanillaImmunity() {
        assertEquals(20, Immunity.before(20, true, true));
        assertEquals(12, Immunity.before(12, true, true));
        assertEquals(11, Immunity.before(11, true, true));
    }

    @Test
    void nothingIsEverRaised() {
        for (int t = -3; t <= 40; t++) {
            for (boolean rapid : new boolean[] {false, true}) {
                for (boolean player : new boolean[] {false, true}) {
                    assertTrue(Immunity.before(t, rapid, player) <= t, "raised at " + t);
                }
            }
        }
        assertEquals(10, Immunity.LANDS_IN_FULL);
    }
}
