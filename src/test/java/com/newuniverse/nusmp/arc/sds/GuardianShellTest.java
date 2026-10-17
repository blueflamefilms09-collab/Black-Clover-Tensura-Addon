package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.DamageKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Guardian is the part of King that a party answers with the right KIND of damage, so which kind passes, which is cut and which ends it is pinned. */
class GuardianShellTest {
    private static GuardianShell open() {
        GuardianShell g = new GuardianShell();
        g.open(100, 160);
        return g;
    }

    @Test
    void physicalBlowsPassAtAThirdAndTheShellStays() {
        GuardianShell g = open();
        for (DamageKind k : new DamageKind[]{DamageKind.MELEE, DamageKind.PROJECTILE, DamageKind.EXPLOSION}) {
            GuardianShell.Verdict v = g.judge(110, k, false, 20f);
            assertEquals(GuardianShell.Result.PARRIED, v.result(), k.name());
            assertEquals(20f * GuardianShell.PHYSICAL_PASS, v.passed(), 1e-6f);
            assertTrue(g.isOpen(110));
        }
    }

    @Test
    void fireIsDampedAndSpellsLandInFull() {
        GuardianShell g = open();
        GuardianShell.Verdict fire = g.judge(110, DamageKind.FIRE, false, 20f);
        assertEquals(GuardianShell.Result.DAMPED, fire.result());
        assertEquals(10f, fire.passed(), 1e-6f);
        for (DamageKind k : new DamageKind[]{DamageKind.SPELL, DamageKind.LIGHTNING, DamageKind.OTHER}) {
            GuardianShell.Verdict v = g.judge(110, k, false, 20f);
            assertEquals(GuardianShell.Result.PASSED, v.result(), k.name());
            assertEquals(20f, v.passed(), 1e-6f);
        }
        assertTrue(g.isOpen(111));
    }

    @Test
    void iceFreezesTheMossAndTheShellIsGone() {
        GuardianShell g = open();
        GuardianShell.Verdict v = g.judge(110, DamageKind.ICE, false, 30f);
        assertEquals(GuardianShell.Result.THAWED, v.result());
        assertEquals(30f, v.passed(), 1e-6f, "the blow that froze it lands in full");
        assertFalse(g.isOpen(111));
        assertEquals(GuardianShell.Result.CLOSED, g.judge(111, DamageKind.MELEE, false, 10f).result());
    }

    @Test
    void antiMagicBreaksTheCreatureWhateverTheKind() {
        GuardianShell g = open();
        GuardianShell.Verdict v = g.judge(110, DamageKind.MELEE, true, 15f);
        assertEquals(GuardianShell.Result.BROKEN, v.result());
        assertEquals(15f, v.passed(), 1e-6f);
        assertFalse(g.isOpen(111));
    }

    @Test
    void theShellEndsByItself() {
        GuardianShell g = open();
        assertTrue(g.isOpen(259));
        assertEquals(1, g.ticksLeft(259));
        assertFalse(g.isOpen(260));
        assertEquals(0, g.ticksLeft(300));
        assertEquals(GuardianShell.Result.CLOSED, g.judge(260, DamageKind.MELEE, false, 10f).result());
        assertEquals(10f, g.judge(260, DamageKind.MELEE, false, 10f).passed(), 1e-6f);
    }

    @Test
    void aBlowOfNothingChangesNothing() {
        GuardianShell g = open();
        assertEquals(GuardianShell.Result.CLOSED, g.judge(110, DamageKind.ICE, false, 0f).result());
        assertTrue(g.isOpen(111), "a zero-damage ice blow does not melt it");
    }

    @Test
    void reopeningRestartsTheClock() {
        GuardianShell g = open();
        g.close();
        assertFalse(g.isOpen(120));
        g.open(300, 10);
        assertTrue(g.isOpen(309) && !g.isOpen(310));
    }
}
