package com.newuniverse.nusmp.avatar.party;

import com.newuniverse.nusmp.avatar.naming.NameRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The NPC party: when an Avatar may name, which monster is a fit, what it costs, and that a generated name is always valid and fresh. */
class PartyRulesTest {
    private static PartyRules.Avatar base() { return new PartyRules.Avatar(true, true, false, 0, 3, 100_000, 0, 6000, 0.95, 0.8); }

    private static PartyRules.Avatar with(boolean sys, boolean setting, boolean betrayed, int members, int cap, long now, long last, long cd, double mag) {
        return new PartyRules.Avatar(sys, setting, betrayed, members, cap, now, last, cd, mag, 0.8);
    }

    @Test
    void itNamesWhenEverythingAllows() {
        assertTrue(PartyRules.mayName(base()));
    }

    @Test
    void theSwitchesAndBetrayalForbidIt() {
        assertFalse(PartyRules.mayName(with(false, true, false, 0, 3, 100_000, 0, 6000, 0.95)));
        assertFalse(PartyRules.mayName(with(true, false, false, 0, 3, 100_000, 0, 6000, 0.95)));
        assertFalse(PartyRules.mayName(with(true, true, true, 0, 3, 100_000, 0, 6000, 0.95)), "a betrayer builds no party");
    }

    @Test
    void thePartyIsCapped() {
        assertTrue(PartyRules.mayName(with(true, true, false, 2, 3, 100_000, 0, 6000, 0.95)));
        assertFalse(PartyRules.mayName(with(true, true, false, 3, 3, 100_000, 0, 6000, 0.95)));
        assertFalse(PartyRules.mayName(with(true, true, false, 0, 0, 100_000, 0, 6000, 0.95)), "a cap of zero means no party");
    }

    @Test
    void theCooldownOnlyCountsAfterAFirstNaming() {
        assertTrue(PartyRules.mayName(with(true, true, false, 0, 3, 100, 0, 6000, 0.95)), "never named anything: no wait, even early in the world");
        assertFalse(PartyRules.mayName(with(true, true, false, 1, 3, 7000, 2000, 6000, 0.95)), "named 5000 ticks ago");
        assertTrue(PartyRules.mayName(with(true, true, false, 1, 3, 8000, 2000, 6000, 0.95)), "6000 ticks later");
    }

    @Test
    void itNeedsExcessMagicules() {
        assertFalse(PartyRules.mayName(with(true, true, false, 0, 3, 100_000, 0, 6000, 0.79)));
        assertTrue(PartyRules.mayName(with(true, true, false, 0, 3, 100_000, 0, 6000, 0.80)));
    }

    @Test
    void aMonsterMustBeAFit() {
        assertTrue(PartyRules.suitable(1000, 500, PartyRules.DEFAULT_MIN_RATIO, PartyRules.DEFAULT_MAX_RATIO));
        assertFalse(PartyRules.suitable(1000, 50, PartyRules.DEFAULT_MIN_RATIO, PartyRules.DEFAULT_MAX_RATIO), "a mouse");
        assertFalse(PartyRules.suitable(1000, 5000, PartyRules.DEFAULT_MIN_RATIO, PartyRules.DEFAULT_MAX_RATIO), "a monster it could never command");
        assertFalse(PartyRules.suitable(0, 500, PartyRules.DEFAULT_MIN_RATIO, PartyRules.DEFAULT_MAX_RATIO));
        assertFalse(PartyRules.suitable(1000, 0, PartyRules.DEFAULT_MIN_RATIO, PartyRules.DEFAULT_MAX_RATIO), "an unknown EP is never a fit");
    }

    @Test
    void theNamingCostIsAShareOfTheMaximumAndAtLeastOne() {
        assertEquals(250.0, PartyRules.nameCost(1000, 0.25), 1e-9);
        assertEquals(1.0, PartyRules.nameCost(0, 0.25), 1e-9);
        assertEquals(1000.0, PartyRules.nameCost(1000, 7.0), 1e-9, "never more than all of it");
        assertEquals(1.0, PartyRules.nameCost(1000, -1.0), 1e-9);
    }

    @Test
    void eachMemberCostsAPermanentShareCappedAtHalf() {
        assertEquals(0.0, PartyRules.tax(0, 5.0), 1e-9);
        assertEquals(0.05, PartyRules.tax(1, 5.0), 1e-9);
        assertEquals(0.15, PartyRules.tax(3, 5.0), 1e-9);
        assertEquals(0.5, PartyRules.tax(40, 5.0), 1e-9);
        assertEquals(0.0, PartyRules.tax(3, 0.0), 1e-9);
    }

    @Test
    void aMemberIsForgottenWhenGoneOrCutLoose() {
        assertTrue(PartyRules.shouldForget(false, true));
        assertTrue(PartyRules.shouldForget(true, false));
        assertFalse(PartyRules.shouldForget(true, true));
    }

    @Test
    void generatedNamesAreValidFreshAndVaried() {
        Random rng = new Random(12345);
        Set<String> seen = new HashSet<>();
        List<String> taken = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String n = NameGen.next(rng, taken);
            assertEquals(NameRules.Verdict.OK, NameRules.check(n, List.of()), "valid: " + n);
            assertEquals(NameRules.Verdict.OK, NameRules.check(n, taken), "fresh: " + n);
            taken.add(n);
            seen.add(n.toLowerCase());
        }
        assertEquals(1000, seen.size(), "a thousand names in one party are all different");
    }

    @Test
    void aNameTakenNeverComesBack() {
        for (long seed = 0; seed < 50; seed++) {
            List<String> taken = new ArrayList<>(List.of(NameGen.build(new Random(seed))));
            assertFalse(taken.get(0).equalsIgnoreCase(NameGen.next(new Random(seed), taken)), "seed " + seed);
        }
    }
}
