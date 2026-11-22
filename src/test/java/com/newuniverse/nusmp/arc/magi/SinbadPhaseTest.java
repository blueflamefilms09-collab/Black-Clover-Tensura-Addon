package com.newuniverse.nusmp.arc.magi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The phase rules of the Sinbad boss (4.105.0), with no game. */
class SinbadPhaseTest {
    @Test
    void thePhaseFollowsThePower() {
        assertEquals(0, SinbadEquip.phaseOf(20, false));
        assertEquals(0, SinbadEquip.phaseOf(40, false));
        assertEquals(1, SinbadEquip.phaseOf(60, false));
        assertEquals(1, SinbadEquip.phaseOf(80, false));
        assertEquals(2, SinbadEquip.phaseOf(100, true));
    }

    @Test
    void equipsGetShorterWithEachPhaseAndPhaseZeroKeepsTheOldNumbers() {
        assertEquals(SinbadEquip.equipTicks(false), SinbadEquip.equipTicks(false, 0));
        assertEquals(SinbadEquip.equipTicks(true), SinbadEquip.equipTicks(true, 2));
        assertTrue(SinbadEquip.equipTicks(false, 0) > SinbadEquip.equipTicks(false, 1));
        assertTrue(SinbadEquip.equipTicks(false, 1) > SinbadEquip.equipTicks(true, 2));
    }

    @Test
    void theSwitchWindowWidensInThePhaseButNeverOutlastsTheEquip() {
        assertEquals(SinbadTable.SWITCH_WINDOW, SinbadEquip.switchWindow(0));
        assertTrue(SinbadEquip.switchWindow(1) > SinbadEquip.switchWindow(0));
        assertTrue(SinbadEquip.switchWindow(1) < SinbadEquip.equipTicks(true, 2));
        SinbadTable.Spec equip = SinbadTable.byId(SinbadTable.DJINN_EQUIP);
        assertFalse(SinbadEquip.allowed(equip, SinbadDjinn.BAAL, false, 100, 0), "100 ticks left: not yet in phase 0");
        assertTrue(SinbadEquip.allowed(equip, SinbadDjinn.BAAL, false, 100, 1), "100 ticks left: already in phase 1");
        assertTrue(SinbadEquip.allowed(equip, SinbadDjinn.NONE, false, 400, 0));
    }

    @Test
    void theOldFilterIsUnchanged() {
        SinbadTable.Spec bararaq = SinbadTable.byId(SinbadTable.BARARAQ);
        assertTrue(SinbadEquip.allowed(bararaq, SinbadDjinn.BAAL, false, 200, 1));
        assertFalse(SinbadEquip.allowed(bararaq, SinbadDjinn.NONE, false, 200, 1));
        assertFalse(SinbadEquip.allowed(SinbadTable.byId(SinbadTable.SEVEN_SEAS), SinbadDjinn.NONE, false, 200, 1), "the finale needs the Gates");
    }
}
