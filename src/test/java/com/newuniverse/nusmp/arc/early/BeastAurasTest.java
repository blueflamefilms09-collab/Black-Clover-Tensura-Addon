package com.newuniverse.nusmp.arc.early;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Vetto's auras: what they give, and how they come off. */
class BeastAurasTest {
    private static final double MAX = 2000;

    @Test
    void theAurasAreFixedSetsOfFactors() {
        BeastAuras.Stats bare = BeastAuras.stats(BeastAuras.Aura.NONE);
        assertEquals(1.0, bare.damage(), 1e-9);
        assertEquals(1.0, bare.speed(), 1e-9);
        assertEquals(0.0, bare.mitigation(), 1e-9);
        BeastAuras.Stats bear = BeastAuras.stats(BeastAuras.Aura.BEAR), cheetah = BeastAuras.stats(BeastAuras.Aura.CHEETAH);
        assertTrue(bear.damage() > cheetah.damage() && bear.mitigation() > cheetah.mitigation(), "the bear is the strong and armoured one");
        assertTrue(cheetah.speed() > bear.speed() && cheetah.speed() > 1.2, "the cheetah is the fast one");
        BeastAuras.Stats myth = BeastAuras.stats(BeastAuras.Aura.MYTHICAL);
        for (BeastAuras.Aura a : new BeastAuras.Aura[]{BeastAuras.Aura.BEAR, BeastAuras.Aura.CHEETAH, BeastAuras.Aura.EAGLE}) {
            assertTrue(myth.damage() >= BeastAuras.stats(a).damage(), "the mythical aura is at least as strong as each of the three");
        }
    }

    @Test
    void theCycleGoesBearCheetahEagle() {
        assertEquals(BeastAuras.Aura.BEAR, BeastAuras.next(BeastAuras.Aura.NONE));
        assertEquals(BeastAuras.Aura.CHEETAH, BeastAuras.next(BeastAuras.Aura.BEAR));
        assertEquals(BeastAuras.Aura.EAGLE, BeastAuras.next(BeastAuras.Aura.CHEETAH));
        assertEquals(BeastAuras.Aura.BEAR, BeastAuras.next(BeastAuras.Aura.EAGLE));
        assertEquals(BeastAuras.Aura.BEAR, BeastAuras.next(BeastAuras.Aura.MYTHICAL));
    }

    @Test
    void antiMagicTearsTheAuraOffAndOpensTheWound() {
        BeastAuras b = new BeastAuras();
        assertTrue(b.wear(BeastAuras.Aura.BEAR, 0));
        assertEquals(BeastAuras.Hit.ANTI_MAGIC, b.hit(10, 3, true, MAX), "even a tiny anti-magic blow");
        assertEquals(BeastAuras.Aura.NONE, b.current());
        assertTrue(b.torn(11));
        assertEquals(BeastAuras.TORN_ANTI_MAGIC, b.tornLeft(10));
        assertFalse(b.wear(BeastAuras.Aura.CHEETAH, 50), "he cannot put an aura back on while the wound is open");
        assertEquals(BeastAuras.Aura.NONE, b.current());
        assertTrue(b.wear(BeastAuras.Aura.CHEETAH, 10 + BeastAuras.TORN_ANTI_MAGIC));
        assertEquals(BeastAuras.Aura.CHEETAH, b.current());
        assertEquals(BeastAuras.Hit.ANTI_MAGIC, b.hit(300, 3, true, MAX), "and it can be torn off again");
    }

    @Test
    void aBurstOverloadsButATrickleDoesNot() {
        BeastAuras b = new BeastAuras();
        b.wear(BeastAuras.Aura.BEAR, 0);
        double need = BeastAuras.overloadThreshold(MAX, BeastAuras.Aura.BEAR);
        assertEquals(160.0, need, 1e-9);
        for (int t = 0; t < 600; t += 20) assertEquals(BeastAuras.Hit.NONE, b.hit(t, 40, false, MAX), "40 every second is a trickle");
        assertEquals(BeastAuras.Aura.BEAR, b.current());
        assertEquals(BeastAuras.Hit.NONE, b.hit(1000, 100, false, MAX));
        assertEquals(BeastAuras.Hit.OVERLOAD, b.hit(1010, 70, false, MAX), "170 inside three seconds");
        assertEquals(BeastAuras.Aura.NONE, b.current());
        assertEquals(BeastAuras.TORN_OVERLOAD, b.tornLeft(1010));
    }

    @Test
    void theMythicalAuraNeedsMoreToOverloadAndBareHeHasNothingToTear() {
        BeastAuras b = new BeastAuras();
        assertEquals(BeastAuras.Hit.NONE, b.hit(0, 9999, true, MAX), "no aura, nothing to tear");
        assertFalse(b.torn(1));
        assertTrue(BeastAuras.overloadThreshold(MAX, BeastAuras.Aura.MYTHICAL) > BeastAuras.overloadThreshold(MAX, BeastAuras.Aura.BEAR));
        b.wear(BeastAuras.Aura.MYTHICAL, 0);
        assertEquals(BeastAuras.Hit.NONE, b.hit(5, 200, false, MAX), "enough for a bear, not for the mythical aura");
        assertEquals(BeastAuras.Hit.OVERLOAD, b.hit(6, 100, false, MAX));
    }

    @Test
    void aTornVettoIsWeakerThanABareOneAndAHealedOneIsBare() {
        BeastAuras b = new BeastAuras();
        b.wear(BeastAuras.Aura.BEAR, 0);
        assertEquals(1.25, b.now(1).damage(), 1e-9);
        b.strip(10, 100);
        BeastAuras.Stats torn = b.now(11);
        assertTrue(torn.damage() < 1.0 && torn.speed() < 1.0 && torn.mitigation() == 0.0, "the wound makes him worse than bare");
        assertEquals(1.0, b.now(200).damage(), 1e-9, "wound closed, bare");
        b.wound(300, 50);
        assertTrue(b.torn(310));
        b.reset();
        assertFalse(b.torn(310));
        assertEquals(BeastAuras.Aura.NONE, b.current());
    }
}
