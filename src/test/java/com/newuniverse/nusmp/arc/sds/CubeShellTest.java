package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Perfect Cube: what it reflects, what crosses it, how many times it holds, and what breaks it. */
class CubeShellTest {
    private static CubeShell open(int charges) {
        CubeShell c = new CubeShell();
        c.open(100, 80, charges);
        return c;
    }

    @Test
    void aClosedCubeLetsEverythingThrough() {
        CubeShell c = new CubeShell();
        CubeShell.Verdict v = c.judge(5, true, true, false, 20f);
        assertEquals(CubeShell.Result.CLOSED, v.result());
        assertEquals(20f, v.passed(), 1e-6f);
        c = open(3);
        assertEquals(CubeShell.Result.CLOSED, c.judge(180, true, true, false, 20f).result(), "80 ticks from tick 100");
        assertEquals(80, open(3).ticksLeft(100));
    }

    @Test
    void aSpellOrAShotIsReflectedForAShareOfItsPower() {
        CubeShell c = open(3);
        CubeShell.Verdict v = c.judge(110, true, false, false, 20f);
        assertEquals(CubeShell.Result.REFLECTED, v.result());
        assertEquals(0f, v.passed(), 1e-6f, "nothing reaches her");
        assertEquals(7f, v.reflectBase(), 1e-5f, "35% of 20");
        assertEquals(2, c.chargesLeft());
        assertEquals(CubeShell.Result.REFLECTED, c.judge(130, false, true, false, 4f).result(), "an arrow from afar is a shot");
        assertEquals(1, c.chargesLeft());
    }

    @Test
    void theReflectionIsMeasured() {
        assertEquals(CubeShell.REFLECT_MIN, CubeShell.reflectBase(1f), 1e-6f);
        assertEquals(CubeShell.REFLECT_MAX, CubeShell.reflectBase(500f), 1e-6f);
        assertEquals(CubeShell.REFLECT_MIN, CubeShell.reflectBase(Float.NaN), 1e-6f);
    }

    @Test
    void aBladeAtArmsLengthCrossesDulled() {
        CubeShell c = open(3);
        CubeShell.Verdict v = c.judge(110, false, false, false, 10f);
        assertEquals(CubeShell.Result.DULLED, v.result());
        assertEquals(6f, v.passed(), 1e-6f);
        assertEquals(3, c.chargesLeft(), "melee costs the cube nothing");
    }

    @Test
    void severalBlowsInOneInstantCostOneReflection() {
        CubeShell c = open(3);
        CubeShell.Verdict first = c.judge(110, true, false, false, 20f);
        assertTrue(first.reflectBase() > 0f);
        CubeShell.Verdict second = c.judge(111, true, false, false, 20f);
        assertEquals(CubeShell.Result.REFLECTED, second.result());
        assertEquals(0f, second.reflectBase(), 1e-6f, "only the blow that paid sends something back");
        assertEquals(0f, second.passed(), 1e-6f);
        assertEquals(2, c.chargesLeft(), "a multi-hit spell is one reflection");
        c.judge(110 + CubeShell.COMBO_TICKS, true, false, false, 20f);
        assertEquals(1, c.chargesLeft(), "after the combo window the next blow pays again");
    }

    @Test
    void theLastReflectionShattersTheCube() {
        CubeShell c = open(1);
        CubeShell.Verdict v = c.judge(110, true, false, false, 30f);
        assertTrue(v.shattered());
        assertFalse(c.isOpen(111));
        assertEquals(CubeShell.Result.CLOSED, c.judge(112, true, false, false, 30f).result());
    }

    @Test
    void antiMagicBreaksItAndLandsInFull() {
        CubeShell c = open(3);
        CubeShell.Verdict v = c.judge(110, true, false, true, 30f);
        assertEquals(CubeShell.Result.BROKEN_BY_ANTI_MAGIC, v.result());
        assertEquals(30f, v.passed(), 1e-6f);
        assertTrue(v.shattered());
        assertFalse(c.isOpen(111));
    }

    @Test
    void openingAgainRefillsTheCharges() {
        CubeShell c = open(1);
        c.judge(110, true, false, false, 10f);
        assertFalse(c.isOpen(111));
        c.open(300, 110, 4);
        assertEquals(4, c.chargesLeft());
        assertTrue(c.isOpen(409));
        assertFalse(c.isOpen(410));
    }
}
