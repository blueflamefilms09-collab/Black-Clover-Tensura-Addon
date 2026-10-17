package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Absolute Cancel as a stance: what it erases, what crosses it, how it closes, what she devours and sends back. */
class CancelStanceTest {
    private static CancelStance open(int charges) {
        CancelStance s = new CancelStance();
        s.open(100, 44, charges, CancelStance.HALF_ARC);
        return s;
    }

    @Test
    void aClosedStanceChangesNothing() {
        CancelStance s = new CancelStance();
        assertFalse(s.isOpen(0));
        assertEquals(CancelStance.Result.CLOSED, s.judge(0, true, false, 0, 10f).result());
        s = open(1);
        assertTrue(s.isOpen(143));
        assertFalse(s.isOpen(144), "44 ticks from tick 100");
        assertEquals(CancelStance.Result.CLOSED, s.judge(144, true, false, 0, 10f).result());
        assertEquals(44, s.ticksLeft(100));
        assertEquals(0, s.ticksLeft(144));
    }

    @Test
    void aSpellFromTheFrontIsErasedAndDevoured() {
        CancelStance s = open(1);
        CancelStance.Verdict v = s.judge(110, true, false, Math.toRadians(30), 24f);
        assertEquals(CancelStance.Result.CANCELLED, v.result());
        assertEquals(24f, v.erased(), 1e-6f);
        assertTrue(v.closedNow(), "one charge: the stance is spent");
        assertFalse(s.isOpen(111));
        assertEquals(24f, s.devoured(), 1e-6f);
    }

    @Test
    void aBladeAnArrowOrASpellFromTheSideCrossesIt() {
        CancelStance s = open(1);
        assertEquals(CancelStance.Result.NOT_A_SPELL, s.judge(110, false, false, 0, 30f).result(), "her hands cannot erase a sword");
        assertEquals(CancelStance.Result.BEHIND, s.judge(111, true, false, Math.toRadians(100), 30f).result(), "outside 65 degrees");
        assertEquals(CancelStance.Result.BEHIND, s.judge(112, true, false, -Math.toRadians(170), 30f).result(), "the sign of the angle does not matter");
        assertTrue(s.isOpen(113), "none of those spent the charge");
        assertEquals(0f, s.devoured(), 1e-6f);
    }

    @Test
    void antiMagicBreaksTheStanceAndDevoursNothing() {
        CancelStance s = open(2);
        CancelStance.Verdict v = s.judge(110, true, true, 0, 50f);
        assertEquals(CancelStance.Result.BROKEN, v.result());
        assertEquals(0f, v.erased(), 1e-6f, "the anti-magic blow is not erased: it lands");
        assertTrue(v.closedNow());
        assertFalse(s.isOpen(111));
        assertEquals(0f, s.devoured(), 1e-6f);
    }

    @Test
    void theGatesStanceHoldsTwoSpellsAndAWiderArc() {
        CancelStance s = new CancelStance();
        s.open(0, 64, 2, CancelStance.HALF_ARC_GATES);
        assertEquals(CancelStance.Result.CANCELLED, s.judge(1, true, false, Math.toRadians(80), 10f).result(), "80 degrees is inside the wider arc");
        assertTrue(s.isOpen(2), "one charge left");
        CancelStance.Verdict second = s.judge(3, true, false, 0, 12f);
        assertTrue(second.closedNow());
        assertEquals(22f, s.devoured(), 1e-6f);
    }

    @Test
    void whatSheDevoursIsCappedAndSentBackAsAMeasuredShare() {
        CancelStance s = new CancelStance();
        s.open(0, 64, 5, CancelStance.HALF_ARC);
        for (int i = 0; i < 5; i++) s.judge(i, true, false, 0, 40f);
        assertEquals(CancelStance.DEVOUR_CAP, s.devoured(), 1e-6f, "she cannot swallow more than the cap");
        assertEquals(CancelStance.RETURN_MAX, CancelStance.returnBase(60f), 1e-6f, "33 would be 55%: the ceiling holds it at 26");
        assertEquals(CancelStance.RETURN_MIN, CancelStance.returnBase(4f), 1e-6f, "a cheap spell still stings a little");
        assertEquals(0f, CancelStance.returnBase(0f), 1e-6f, "nothing devoured, nothing returned");
        assertEquals(0f, CancelStance.returnBase(Float.NaN), 1e-6f);
        assertEquals(13.2f, CancelStance.returnBase(24f), 1e-5f);
    }

    @Test
    void takingTheReturnSpendsTheDevouredPower() {
        CancelStance s = open(1);
        s.judge(110, true, false, 0, 24f);
        assertEquals(13.2f, s.takeReturn(), 1e-5f);
        assertEquals(0f, s.devoured(), 1e-6f);
        assertEquals(0f, s.takeReturn(), 1e-6f);
        s.open(200, 44, 1, CancelStance.HALF_ARC);
        s.judge(201, true, false, 0, 10f);
        s.reset();
        assertEquals(0f, s.devoured(), 1e-6f);
        assertFalse(s.isOpen(202));
    }

    @Test
    void badDamageDevoursNothing() {
        CancelStance s = open(1);
        CancelStance.Verdict v = s.judge(110, true, false, 0, Float.NaN);
        assertEquals(CancelStance.Result.CANCELLED, v.result());
        assertEquals(0f, v.erased(), 1e-6f);
        assertEquals(0f, s.devoured(), 1e-6f);
    }
}
