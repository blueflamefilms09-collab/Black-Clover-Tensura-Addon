package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.newuniverse.nusmp.relics.djinn.HouseholdRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** Household lending and where the Seals come from. */
class HouseholdRulesTest {
    @Test
    void anOwnerLendsToTwoAlliesWhoHoldNothingElse() {
        assertEquals(Verdict.OK, invite(true, false, 0, false, 0, false));
        assertEquals(Verdict.OK, invite(true, false, 1, false, 0, false));
        assertEquals(Verdict.FULL, invite(true, false, 2, false, 0, false));
        assertEquals(Verdict.NOT_OWNER, invite(false, false, 0, false, 0, false));
        assertEquals(Verdict.SELF, invite(true, true, 0, false, 0, false));
        assertEquals(Verdict.ALREADY, invite(true, false, 1, true, 0, false));
        assertEquals(Verdict.ALLY_BONDED, invite(true, false, 0, false, BondRules.SPIRIT, false));
        assertEquals(Verdict.ALLY_BONDED, invite(true, false, 0, false, BondRules.DEVIL, false));
        assertEquals(Verdict.OK, invite(true, false, 0, false, BondRules.DEVIL, true), "the multi-bond relic lifts it");
        assertEquals(Verdict.ALLY_HAS_DJINN, invite(true, false, 0, false, BondRules.DJINN, true));
    }

    @Test
    void sealsGoToTheSleepingOnes() {
        assertNull(pickSleeping(List.of(), 0.5));
        assertEquals(Djinn.BAAL, pickSleeping(List.of(Djinn.BAAL, Djinn.AMON, Djinn.ZAGAN), 0.0));
        assertEquals(Djinn.AMON, pickSleeping(List.of(Djinn.BAAL, Djinn.AMON, Djinn.ZAGAN), 0.5));
        assertEquals(Djinn.ZAGAN, pickSleeping(List.of(Djinn.BAAL, Djinn.AMON, Djinn.ZAGAN), 0.999999));
        assertEquals(Djinn.ZAGAN, pickSleeping(List.of(Djinn.BAAL, Djinn.AMON, Djinn.ZAGAN), 5.0), "a bad roll is clamped");
        assertEquals(Djinn.BAAL, pickSleeping(List.of(Djinn.BAAL, Djinn.AMON, Djinn.ZAGAN), -1.0));
    }

    @Test
    void fourBossesDropASealOnTheirFirstFall() {
        assertTrue(isSealBoss("GOLIATH_PIT"));
        assertTrue(isSealBoss("CHARYBDIS_BASIN"));
        assertTrue(isSealBoss("UNDERWORLD_GATE"));
        assertTrue(isSealBoss("MONARCH_VAULT"));
        assertFalse(isSealBoss("TEMPEST_MIRAGE"));
        assertFalse(isSealBoss("GOLDEN_CITY"));
        assertFalse(isSealBoss(null));
        assertTrue(VAULT_CHANCE > 0 && VAULT_CHANCE <= 0.05, "very rare");
    }
}
