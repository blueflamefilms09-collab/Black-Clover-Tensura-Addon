package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.DamageKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Heavy Metal is a flat cut per blow, a rule with a counter, so its edges (the fifth that always passes, fire that is not cut, the melt) are pinned. */
class MetalSkinTest {
    private static final float FLAT = 10f, MELT = 100f;

    private static MetalSkin open() {
        MetalSkin m = new MetalSkin();
        m.open(100, 120);
        return m;
    }

    @Test
    void aSmallBlowIsNearlyWalledAndABigOnePassesAlmostWhole() {
        MetalSkin m = open();
        MetalSkin.Verdict small = m.judge(110, DamageKind.MELEE, 6f, FLAT, MELT);
        assertEquals(MetalSkin.Result.REDUCED, small.result());
        assertEquals(6f * MetalSkin.MIN_PASS, small.passed(), 1e-6f, "a blow smaller than the cut still gets its fifth through");
        MetalSkin.Verdict big = m.judge(110, DamageKind.MELEE, 60f, FLAT, MELT);
        assertEquals(50f, big.passed(), 1e-6f);
        MetalSkin.Verdict mid = m.judge(110, DamageKind.PROJECTILE, 12f, FLAT, MELT);
        assertEquals(2.4f, mid.passed(), 1e-6f, "12 - 10 = 2 is less than a fifth, so the fifth passes");
    }

    @Test
    void manySmallBlowsDoLessThanOneBigBlowOfTheSameTotal() {
        MetalSkin m = open();
        float small = 0f;
        for (int i = 0; i < 10; i++) small += m.judge(110, DamageKind.MELEE, 10f, FLAT, MELT).passed();
        float big = m.judge(110, DamageKind.MELEE, 100f, FLAT, MELT).passed();
        assertTrue(small < big / 2f, "ten blows of 10 do " + small + ", one blow of 100 does " + big);
    }

    @Test
    void spellsAndEverythingElseAreCutTheSameWay() {
        MetalSkin m = open();
        for (DamageKind k : new DamageKind[]{DamageKind.SPELL, DamageKind.ICE, DamageKind.LIGHTNING, DamageKind.EXPLOSION, DamageKind.OTHER})
            assertEquals(30f, m.judge(110, k, 40f, FLAT, MELT).passed(), 1e-6f, k.name());
    }

    @Test
    void fireIsNotCutAndEnoughOfItMeltsTheMetalForGood() {
        MetalSkin m = open();
        MetalSkin.Verdict a = m.judge(110, DamageKind.FIRE, 40f, FLAT, MELT);
        assertEquals(MetalSkin.Result.HEATED, a.result());
        assertEquals(40f, a.passed(), 1e-6f);
        assertEquals(MetalSkin.Result.HEATED, m.judge(120, DamageKind.FIRE, 40f, FLAT, MELT).result());
        MetalSkin.Verdict c = m.judge(130, DamageKind.FIRE, 40f, FLAT, MELT);
        assertEquals(MetalSkin.Result.MELTED, c.result(), "120 of fire inside three seconds melts a skin that melts at 100");
        assertEquals(40f, c.passed(), 1e-6f);
        assertFalse(m.isOpen(131));
        assertEquals(MetalSkin.Result.CLOSED, m.judge(131, DamageKind.MELEE, 40f, FLAT, MELT).result());
    }

    @Test
    void fireSpreadOverTimeDoesNotMelt() {
        MetalSkin m = open();
        for (int i = 0; i < 4; i++) assertEquals(MetalSkin.Result.HEATED, m.judge(100 + i * 30, DamageKind.FIRE, 40f, FLAT, MELT).result(), "tick " + (100 + i * 30));
    }

    @Test
    void theSkinEndsByItself() {
        MetalSkin m = open();
        assertTrue(m.isOpen(219));
        assertEquals(1, m.ticksLeft(219));
        assertFalse(m.isOpen(220));
        assertEquals(MetalSkin.Result.CLOSED, m.judge(220, DamageKind.MELEE, 10f, FLAT, MELT).result());
        assertEquals(10f, m.judge(220, DamageKind.MELEE, 10f, FLAT, MELT).passed(), 1e-6f);
    }
}
