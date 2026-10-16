package com.newuniverse.nusmp.shadow;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The Devil Unite stat table, the damage classes, the old-page signatures, the on/off slot and the call numbers. */
class UniteModesTest {
    @Test
    void equusIsAFortress() {
        UniteKind k = UniteKind.EQUUS;
        assertEquals(10, k.armor);
        assertEquals(6, k.toughness);
        assertEquals(0.40, k.physicalReduction, 1e-9);
        assertEquals(-0.25, k.speed, 1e-9);
        assertEquals(1.0, k.knockbackResistance, 1e-9);
        assertEquals(60.0, k.afterReduction(100, true), 1e-9, "40 percent off physical damage");
        assertEquals(100.0, k.afterReduction(100, false), 1e-9, "magic is not reduced");
        assertEquals(0.0, k.afterReduction(-5, true), 1e-9);
    }

    @Test
    void felisKeepsItsPromises() {
        UniteKind k = UniteKind.FELIS;
        assertEquals(0.30, k.speed, 1e-9);
        assertEquals(8, k.lonerRange);
        assertEquals(24, k.sightRange);
        assertFalse(k.mobLosesTarget(8.0));
        assertTrue(k.mobLosesTarget(8.1));
        assertFalse(UniteKind.CANIS.mobLosesTarget(100), "only Felis is a loner");
        assertTrue(k.dodges(0.19, false, 0.10));
        assertFalse(k.dodges(0.21, false, 0.10));
        assertTrue(k.dodges(0.09, true, 0.10), "a boss is dodged at the reduced chance");
        assertFalse(k.dodges(0.15, true, 0.10));
        assertFalse(UniteKind.EQUUS.dodges(0.0, false, 0.1), "no dodge in the table, no dodge");
    }

    @Test
    void everyEntryStaysInsideTheClamps() {
        for (UniteKind k : UniteKind.values()) {
            assertTrue(k.physicalReduction >= 0 && k.physicalReduction <= UniteKind.MAX_REDUCTION, k.id);
            assertTrue(k.dodge >= 0 && k.dodge <= UniteKind.MAX_DODGE, k.id);
            assertTrue(k.speed >= UniteKind.MIN_SPEED, k.id);
            assertTrue(k.armor >= 0 && k.toughness >= 0, k.id);
            assertEquals(600, UniteKind.TICKS);
            assertTrue(k.overlay.startsWith("DEVIL_"), k.id);
        }
    }

    @Test
    void ordinalsAreAppendOnly() {
        assertEquals(UniteKind.EQUUS, UniteKind.of(0));
        assertEquals(UniteKind.CANIS, UniteKind.of(1));
        assertEquals(UniteKind.FELIS, UniteKind.of(2));
        assertEquals(UniteKind.GALLUS, UniteKind.of(3));
        assertNull(UniteKind.of(-1));
        assertNull(UniteKind.of(4));
    }

    @Test
    void damageClassSeparatesPhysicalFromMagic() {
        assertTrue(DamageClass.isPhysical("mob_attack", false, false));
        assertTrue(DamageClass.isPhysical("player_attack", false, false));
        assertTrue(DamageClass.isPhysical("arrow", false, false));
        assertTrue(DamageClass.isPhysical("severer_blade", false, false));
        assertFalse(DamageClass.isPhysical("magic", true, false), "vanilla magic bypasses armour");
        assertFalse(DamageClass.isPhysical("darkness_elemental", false, false));
        assertFalse(DamageClass.isPhysical("magic_generic", false, false));
        assertFalse(DamageClass.isPhysical("fire_elemental", false, false));
        assertFalse(DamageClass.isPhysical("curse", false, false));
        assertFalse(DamageClass.isPhysical("out_of_world", false, true));
        assertFalse(DamageClass.isPhysical("fall", true, false));
        assertTrue(DamageClass.isPhysical(null, false, false));
    }

    private static Map<String, int[]> fx(Object... kv) {
        Map<String, int[]> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 3) m.put((String) kv[i], new int[]{(Integer) kv[i + 1], (Integer) kv[i + 2]});
        return m;
    }

    @Test
    void theOldPagesAreRecognisedByTheirEffects() {
        assertEquals(UniteSignature.Source.CANIS, UniteSignature.detect(fx("speed", 2, 600, "strength", 0, 600, "jump", 1, 600)));
        assertEquals(UniteSignature.Source.GALLUS, UniteSignature.detect(fx("slow_falling", 0, 600, "jump", 4, 600, "speed", 1, 600)));
        assertEquals(UniteSignature.Source.CANIS_FELIS, UniteSignature.detect(fx("speed", 2, 600, "strength", 1, 600, "resistance", 0, 600, "jump", 1, 600)));
        assertEquals(UniteKind.CANIS, UniteSignature.kind(UniteSignature.Source.CANIS_FELIS));
        assertEquals(UniteKind.GALLUS, UniteSignature.kind(UniteSignature.Source.GALLUS));
        assertNull(UniteSignature.kind(UniteSignature.Source.NONE));
    }

    @Test
    void ordinaryPotionsAndOldEffectsDoNotMatch() {
        assertEquals(UniteSignature.Source.NONE, UniteSignature.detect(fx("speed", 1, 600)));
        assertEquals(UniteSignature.Source.NONE, UniteSignature.detect(fx("speed", 2, 300, "strength", 0, 300, "jump", 1, 300)), "an older effect is not a fresh cast");
        assertEquals(UniteSignature.Source.NONE, UniteSignature.detect(fx()));
        assertEquals(UniteSignature.Source.NONE, UniteSignature.detect(fx("speed", 2, 600, "strength", 0, 600)), "jump is missing");
        assertEquals(UniteSignature.Source.CANIS_FELIS, UniteSignature.detect(fx("speed", 2, 600, "strength", 1, 600, "resistance", 0, 600, "jump", 1, 600, "slow_falling", 0, 600)));
    }

    @Test
    void theSlotTurnsOnAndOffExactlyOnce() {
        UniteSlot s = UniteSlot.NONE;
        assertFalse(s.active(100));
        assertFalse(s.expired(100), "nothing set, nothing to clean up");
        s = UniteSlot.start(UniteKind.FELIS, 100, 600);
        assertTrue(s.active(100));
        assertEquals(UniteKind.FELIS, s.running(699));
        assertEquals(600, s.ticksLeft(100));
        assertFalse(s.expired(699));
        assertTrue(s.expired(700), "ends at start + 600");
        assertNull(s.running(700));
        assertEquals(0, s.ticksLeft(700));
        assertTrue(s.replaces(UniteKind.EQUUS, 200), "another mode replaces it");
        assertFalse(s.replaces(UniteKind.FELIS, 200), "the same mode only refreshes");
        assertFalse(s.replaces(UniteKind.EQUUS, 800), "an ended mode is not replaced, it is just over");
        UniteSlot early = s.stopped();
        assertFalse(early.active(200));
        assertFalse(early.expired(200));
        assertSame(UniteSlot.NONE, UniteSlot.start(UniteKind.CANIS, 5, 0));
        assertFalse(new UniteSlot(99, 10_000).active(0), "an unknown mode from the future is never active");
    }

    @Test
    void theCallStunsLongerWhenCloseAndNothingOutside() {
        assertEquals(50, CallMath.stunTicks(0));
        assertEquals(30, CallMath.stunTicks(8));
        assertEquals(40, CallMath.stunTicks(4));
        assertEquals(0, CallMath.stunTicks(8.01));
        assertEquals(0, CallMath.stunTicks(-1));
        for (double d = 0; d <= 8; d += 0.5) assertTrue(CallMath.stunTicks(d) >= 30 && CallMath.stunTicks(d) <= 50);
        assertEquals(80, CallMath.ROOT_TICKS, "4 seconds asked; the balance law clamps it (3 s players, 1 s bosses)");
    }
}
