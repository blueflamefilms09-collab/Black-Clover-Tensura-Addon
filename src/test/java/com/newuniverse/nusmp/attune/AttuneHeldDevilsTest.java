package com.newuniverse.nusmp.attune;

import static org.junit.jupiter.api.Assertions.*;

import com.newuniverse.nusmp.devil.WorldDevil;
import org.junit.jupiter.api.Test;

/** 4.92.1: the meditation screen lists every devil a player holds, not only the active one. */
class AttuneHeldDevilsTest {
    @Test void aHeldDevilGetsItsOwnValidId() {
        WorldDevil w = WorldDevil.byId("future_devil");
        assertNotNull(w);
        AttuneRules.Power p = AttunePowers.heldDevil(w);
        assertEquals("devil:future_devil", p.id());
        assertEquals(AttuneRules.DEVIL, p.system());
        assertTrue(AttuneRules.validId(p.id()));
        assertTrue(p.rank() >= 1);
    }

    @Test void everyRosterDevilHasAValidAttuneId() {
        for (int i = 0; i < WorldDevil.count(); i++) { WorldDevil w = WorldDevil.at(i); assertTrue(AttuneRules.validId(AttunePowers.heldDevil(w).id()), w.id()); }
    }
}
