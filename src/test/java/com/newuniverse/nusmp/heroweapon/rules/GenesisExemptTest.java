package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.relics.Grade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 4.88.1: only Genesis and above are exempt from the class rejection, only while the setting is on. */
class GenesisExemptTest {
    @Test
    void genesisAndAboveAreExemptWhileOn() {
        assertTrue(GenesisExempt.exempt(Grade.GENESIS, true));
        assertTrue(GenesisExempt.exempt(Grade.AKASHIC, true));
    }

    @Test
    void lowerGradesStayRejected() {
        for (Grade g : Grade.values()) if (g.tier() < Grade.GENESIS.tier()) assertFalse(GenesisExempt.exempt(g, true), g.name());
    }

    @Test
    void offRestoresTheOldBehaviourAndNullIsSafe() {
        for (Grade g : Grade.values()) assertFalse(GenesisExempt.exempt(g, false), g.name());
        assertFalse(GenesisExempt.exempt(null, true));
    }
}
