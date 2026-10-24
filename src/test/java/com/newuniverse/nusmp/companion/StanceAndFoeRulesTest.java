package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.companion.bond.FoeRules;
import com.newuniverse.nusmp.companion.bond.Stance;
import com.newuniverse.nusmp.companion.bond.StanceRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: "nobody attacks a companion or a person or an ally" and "what a stance does" are rules every companion shares; the truth tables are pinned here. */
class StanceAndFoeRulesTest {
    @Test
    void stancesRoundTripByNameAndOrdinal() {
        for (Stance s : Stance.values()) {
            assertEquals(s, Stance.byName(s.name()));
            assertEquals(s, Stance.byName(s.id()));
            assertEquals(s, Stance.byOrdinal(s.ordinal()));
        }
        assertEquals(Stance.GUARD, Stance.byName("nonsense"));
        assertEquals(Stance.GUARD, Stance.byName(null));
        assertEquals(Stance.GUARD, Stance.byOrdinal(77));
        assertEquals("GUARD", Stance.values()[0].name(), "constants are only ever appended");
        assertEquals("AGGRESSIVE", Stance.values()[1].name());
        assertEquals("PASSIVE", Stance.values()[2].name());
    }

    @Test
    void aFoeIsHostileOrTiedToTheOwner() {
        assertTrue(FoeRules.isFoe(true, false, false, false, false, false, true, false));
        assertTrue(FoeRules.isFoe(true, false, false, false, false, false, false, true));
        assertFalse(FoeRules.isFoe(true, false, false, false, false, false, false, false));
    }

    @Test
    void neverAPlayerAPersonACompanionAPetOrAnAlly() {
        assertFalse(FoeRules.isFoe(true, true, false, false, false, false, true, true), "a player");
        assertFalse(FoeRules.isFoe(true, false, true, false, false, false, true, true), "a person");
        assertFalse(FoeRules.isFoe(true, false, false, true, false, false, true, true), "a companion");
        assertFalse(FoeRules.isFoe(true, false, false, false, true, false, true, true), "the owner's pet");
        assertFalse(FoeRules.isFoe(true, false, false, false, false, true, true, true), "an ally");
        assertFalse(FoeRules.isFoe(false, false, false, false, false, false, true, true), "a dead thing");
    }

    @Test
    void aBlowCountsForTenSeconds() {
        assertTrue(FoeRules.recent(100, 150));
        assertTrue(FoeRules.recent(100, 299));
        assertFalse(FoeRules.recent(100, 300));
        assertFalse(FoeRules.recent(0, 50), "no blow ever");
        assertFalse(FoeRules.recent(100, 50), "the clock went back");
    }

    @Test
    void passiveNeverStartsAFight() {
        assertFalse(StanceRules.wants(Stance.PASSIVE, true, true, true, 1, 1));
        assertFalse(StanceRules.wants(null, true, true, true, 1, 1));
    }

    @Test
    void guardAnswersTheOwnersFightsAndCloseHostiles() {
        assertTrue(StanceRules.wants(Stance.GUARD, true, false, true, 400, 100), "it struck the owner, even far from the owner");
        assertTrue(StanceRules.wants(Stance.GUARD, true, true, false, 49, 100), "hostile near the owner");
        assertFalse(StanceRules.wants(Stance.GUARD, true, true, false, 81, 100), "hostile but far from the owner");
        assertFalse(StanceRules.wants(Stance.GUARD, false, true, true, 1, 1), "not a foe");
        assertFalse(StanceRules.wants(Stance.GUARD, true, true, true, 1, 300), "beyond sixteen blocks of her");
    }

    @Test
    void aggressiveGoesForAnyHostileInSight() {
        assertTrue(StanceRules.wants(Stance.AGGRESSIVE, true, true, false, 900, 200));
        assertTrue(StanceRules.wants(Stance.AGGRESSIVE, true, false, true, 900, 200));
        assertFalse(StanceRules.wants(Stance.AGGRESSIVE, true, false, false, 1, 1));
        assertFalse(StanceRules.wants(Stance.AGGRESSIVE, true, true, false, 1, 257), "beyond sixteen blocks");
        assertFalse(StanceRules.wants(Stance.AGGRESSIVE, false, true, true, 1, 1));
    }
}
