package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.naming.NameRules;
import com.newuniverse.nusmp.avatar.naming.NamingMath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a name reaches the server from a text box, and the naming surge and tax are pure numbers the owner specified; both are checked here. */
class NamingTest {
    private static final double EPS = 1e-9;

    @Test
    void epBoostStaysInsideTheConfiguredBand() {
        for (double luck = -10; luck <= 40; luck += 5)
            for (int in = 3; in <= 20; in += 3)
                for (double r = 0; r <= 1.0; r += 0.25) {
                    double b = NamingMath.epBoost(luck, in, r, 0.5, 2.0);
                    assertTrue(b >= 0.5 - EPS && b <= 2.0 + EPS, "boost " + b);
                }
    }

    @Test
    void luckAndIntAndTheDiceAllRaiseTheBoost() {
        double base = NamingMath.epBoost(0, 3, 0, 0.5, 2.0);
        assertEquals(0.5, base, EPS);
        assertTrue(NamingMath.epBoost(20, 3, 0, 0.5, 2.0) > base);
        assertTrue(NamingMath.epBoost(0, 18, 0, 0.5, 2.0) > base);
        assertTrue(NamingMath.epBoost(0, 3, 1, 0.5, 2.0) > base);
        assertEquals(2.0, NamingMath.epBoost(50, 30, 5, 0.5, 2.0), EPS);
    }

    @Test
    void anInvertedBandIsRepaired() { assertEquals(0.5, NamingMath.epBoost(0, 3, 0, 2.0, 0.5), EPS); }

    @Test
    void multiplierIsOnePlusBoost() {
        assertEquals(1.5, NamingMath.epMultiplier(0.5), EPS);
        assertEquals(1.0, NamingMath.epMultiplier(-3), EPS);
    }

    @Test
    void taxIsAFractionNeverTheWholePool() {
        assertEquals(0.10, NamingMath.taxFraction(10), EPS);
        assertEquals(0.9, NamingMath.taxFraction(500), EPS);
        assertEquals(0.0, NamingMath.taxFraction(-4), EPS);
    }

    @Test
    void namingAddsToTheTaxTheForgeAlreadyCharged() {
        assertEquals(0.25, NamingMath.taxAfterNaming(0.15, 0.10), EPS);
        assertEquals(0.10, NamingMath.taxAfterNaming(0.0, 0.10), EPS);
        assertEquals(0.9, NamingMath.taxAfterNaming(0.85, 0.10), EPS);
        assertEquals(0.10, NamingMath.taxAfterNaming(-1.0, 0.10), EPS);
    }

    @Test
    void anEvolutionDoublesTheSleep() {
        assertEquals(3600, NamingMath.sleepTicks(180, false));
        assertEquals(7200, NamingMath.sleepTicks(180, true));
        assertEquals(0, NamingMath.sleepTicks(-1, true));
    }

    @Test
    void validNamesPass() {
        for (String n : new String[]{"Ra", "Aria", "Jean-Luc", "O'Neil", "Kira 2", "Abcdefghijklmnopqrstuvwx"})
            assertEquals(NameRules.Verdict.OK, NameRules.check(n, List.of()), n);
    }

    @Test
    void badNamesAreRefusedWithTheRightReason() {
        assertEquals(NameRules.Verdict.TOO_SHORT, NameRules.check("A", List.of()));
        assertEquals(NameRules.Verdict.TOO_SHORT, NameRules.check("   ", List.of()));
        assertEquals(NameRules.Verdict.TOO_SHORT, NameRules.check(null, List.of()));
        assertEquals(NameRules.Verdict.TOO_LONG, NameRules.check("A".repeat(25), List.of()));
        assertEquals(NameRules.Verdict.BAD_CHARACTERS, NameRules.check("Bad<Name>", List.of()));
        assertEquals(NameRules.Verdict.BAD_CHARACTERS, NameRules.check("§cRed", List.of()));
        assertEquals(NameRules.Verdict.BAD_CHARACTERS, NameRules.check("semi;colon", List.of()));
    }

    @Test
    void namesAreUniquePerCreatorIgnoringCase() {
        assertEquals(NameRules.Verdict.DUPLICATE, NameRules.check("aria", List.of("Aria")));
        assertEquals(NameRules.Verdict.DUPLICATE, NameRules.check("  ARIA ", List.of("Aria")));
        assertEquals(NameRules.Verdict.OK, NameRules.check("Aria", List.of("Bran")));
    }

    @Test
    void cleaningCollapsesSpaces() {
        assertEquals("Jean Luc", NameRules.clean("  Jean    Luc "));
        assertEquals("", NameRules.clean(null));
        assertFalse(NameRules.message(NameRules.Verdict.DUPLICATE).isEmpty());
        assertEquals("", NameRules.message(NameRules.Verdict.OK));
    }

    @Test
    void aNativeNamingEndsWithTheLargerOfTensurasGainAndTheSurge() {
        assertEquals(2.0, NamingMath.nativeTopUp(100, 150, 3.0), 1e-9, "100 x 3 = 300 beats Tensura's 150: scale 150 up to 300");
        assertEquals(1.0, NamingMath.nativeTopUp(100, 400, 3.0), 1e-9, "Tensura gave more than the surge");
        assertEquals(1.0, NamingMath.nativeTopUp(100, 300, 3.0), 1e-9, "equal: nothing to add");
        assertEquals(1.0, NamingMath.nativeTopUp(-1, 150, 3.0), 1e-9, "an unknown starting EP changes nothing");
        assertEquals(1.0, NamingMath.nativeTopUp(100, 0, 3.0), 1e-9);
        assertEquals(1.0, NamingMath.nativeTopUp(100, 150, 1.0), 1e-9, "no surge, no top-up");
        assertEquals(1.0, NamingMath.nativeTopUp(Double.NaN, 150, 3.0), 1e-9);
        double ends = 150 * NamingMath.nativeTopUp(100, 150, 3.0);
        assertTrue(ends >= 150 && ends >= 100 * 3.0 - 1e-9);
    }
}
