package com.newuniverse.nusmp.heroweapon.weapon.fan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the magic residue is the Fan's loop, so its rules are pinned: it starts empty, a stack is added by name, every fourth full swing adds one, it stops at the
 * cap of six (and stops counting swings there), spending empties it and says how much it held, and a reset forgets the half-counted swings too.
 */
class ResidueLedgerTest {
    @Test
    void startsEmptyAndAddsByName() {
        ResidueLedger r = new ResidueLedger();
        assertEquals(0, r.stacks());
        assertFalse(r.full());
        assertTrue(r.add(2));
        assertEquals(2, r.stacks());
        assertFalse(r.add(0), "adding nothing changes nothing");
        assertFalse(r.add(-3));
        assertEquals(2, r.stacks());
    }

    @Test
    void everyFourthFullSwingAddsAStack() {
        ResidueLedger r = new ResidueLedger();
        for (int i = 1; i <= FanNumbers.SWINGS_PER_RESIDUE - 1; i++) assertFalse(r.swing(), "swing " + i);
        assertEquals(0, r.stacks());
        assertTrue(r.swing(), "the fourth swing completes a stack");
        assertEquals(1, r.stacks());
        for (int i = 1; i <= FanNumbers.SWINGS_PER_RESIDUE; i++) r.swing();
        assertEquals(2, r.stacks(), "and the count starts over");
    }

    @Test
    void theCapHoldsAndSwingsStopCountingAtTheCap() {
        ResidueLedger r = new ResidueLedger();
        assertTrue(r.add(99));
        assertEquals(FanNumbers.RESIDUE_MAX, r.stacks());
        assertTrue(r.full());
        assertFalse(r.add(1), "nothing to add at the cap");
        assertFalse(r.swing());
        assertEquals(FanNumbers.RESIDUE_MAX, r.stacks());
        assertEquals(FanNumbers.RESIDUE_MAX, r.spend());
        assertEquals(0, r.stacks());
        for (int i = 1; i <= FanNumbers.SWINGS_PER_RESIDUE - 1; i++) assertFalse(r.swing(), "swings made at the cap were not counted: swing " + i);
    }

    @Test
    void spendingAndResettingForgetEverything() {
        ResidueLedger r = new ResidueLedger();
        r.add(3);
        r.swing();
        r.swing();
        assertEquals(3, r.spend());
        assertEquals(0, r.spend(), "spending an empty ledger gives nothing");
        r.swing();
        r.reset();
        for (int i = 1; i <= FanNumbers.SWINGS_PER_RESIDUE - 1; i++) assertFalse(r.swing(), "the half-counted swing was forgotten: " + i);
        r.add(4);
        r.reset();
        assertEquals(0, r.stacks());
    }
}
