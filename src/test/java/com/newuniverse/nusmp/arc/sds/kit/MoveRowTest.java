package com.newuniverse.nusmp.arc.sds.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the one move record of the four Sins is copied by every builder call, so each copy must keep every field and the mana rule must follow the framework's. */
class MoveRowTest {
    private static final MoveRow BASE = MoveRow.of("volley", "Volley", 20, 18, 100, 4, 30, 5, 8);

    @Test
    void anOrdinaryMovePaysTheFrameworksManaRule() {
        assertEquals(6 + 100 / 16.0, BASE.manaCost(), 1e-9);
        assertEquals(32.0, MoveRow.of("big", "Big", 20, 18, 1000, 0, 9, 1, 8).manaCost(), 1e-9, "at most 32");
        assertFalse(BASE.ultimate() || BASE.crowd() || BASE.free());
        assertEquals(0, BASE.group());
    }

    @Test
    void anUltimatePaysSixtyAndAFreeMoveNothingAtAnyPower() {
        assertEquals(60.0, BASE.ult().manaCost(), 1e-9);
        assertEquals(0.0, BASE.noMana().manaCost(), 1e-9);
        assertEquals(0.0, BASE.ult().noMana().manaCost(), 1e-9, "free wins");
    }

    @Test
    void everyCopyKeepsEveryOtherField() {
        MoveRow m = BASE.crowdMove().ult().noMana().inGroup(2);
        assertEquals("volley", m.id());
        assertEquals("Volley", m.label());
        assertEquals(20, m.minPower());
        assertEquals(18, m.windUpTicks());
        assertEquals(100, m.cooldownTicks());
        assertEquals(4.0, m.minRange(), 1e-12);
        assertEquals(30.0, m.maxRange(), 1e-12);
        assertEquals(5, m.weight());
        assertEquals(8, m.recovery());
        assertTrue(m.crowd() && m.ultimate() && m.free());
        assertEquals(2, m.group());
        MoveRow g = BASE.inGroup(1).crowdMove();
        assertEquals(1, g.group(), "a later copy keeps the group");
        assertTrue(g.crowd());
    }

    @Test
    void theOriginalIsNeverChangedByACopy() {
        BASE.ult();
        BASE.crowdMove();
        BASE.noMana();
        BASE.inGroup(2);
        assertFalse(BASE.ultimate() || BASE.crowd() || BASE.free());
        assertEquals(0, BASE.group());
    }
}
