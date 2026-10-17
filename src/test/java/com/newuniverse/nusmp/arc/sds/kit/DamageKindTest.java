package com.newuniverse.nusmp.arc.sds.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: three of the Sins answer a blow by what it is, so the one sorting of damage into kinds is tested, including the traps in the words of ids ("icicle" is ice, "slice" is not). */
class DamageKindTest {
    private static DamageKind kind(boolean freezing, boolean fire, boolean explosion, boolean lightning, boolean projectile, boolean spell, boolean melee, String path) {
        return DamageKind.of(new DamageKind.Facts(freezing, fire, explosion, lightning, projectile, spell, melee, path));
    }

    @Test
    void aPlainSwordBlowIsMeleeAndAnArrowIsAProjectile() {
        assertEquals(DamageKind.MELEE, kind(false, false, false, false, false, false, true, "player_attack"));
        assertEquals(DamageKind.PROJECTILE, kind(false, false, false, false, true, false, false, "arrow"));
        assertEquals(DamageKind.OTHER, kind(false, false, false, false, false, false, false, "fall"));
    }

    @Test
    void anElementBeatsHowItTravelled() {
        assertEquals(DamageKind.FIRE, kind(false, true, false, false, true, false, false, "arrow"), "a fire arrow is fire");
        assertEquals(DamageKind.ICE, kind(true, true, false, false, false, false, false, "x"), "freezing is read before fire");
        assertEquals(DamageKind.EXPLOSION, kind(false, false, true, false, false, true, false, "x"));
        assertEquals(DamageKind.LIGHTNING, kind(false, false, false, true, false, true, false, "x"));
    }

    @Test
    void aSpellWithNoElementIsASpell() {
        assertEquals(DamageKind.SPELL, kind(false, false, false, false, false, true, true, "magic"));
        assertEquals(DamageKind.SPELL, kind(false, false, false, false, false, true, false, "gravity_press"));
    }

    @Test
    void anIdFromAnotherModIsSortedByItsWords() {
        assertEquals(DamageKind.ICE, kind(false, false, false, false, false, true, false, "Icicle_Lance"));
        assertEquals(DamageKind.ICE, kind(false, false, false, false, false, true, false, "blizzard"));
        assertEquals(DamageKind.FIRE, kind(false, false, false, false, false, true, false, "hellfire_burst"));
        assertEquals(DamageKind.FIRE, kind(false, false, false, false, false, false, true, "flame_sword"));
        assertEquals(DamageKind.LIGHTNING, kind(false, false, false, false, false, true, false, "thunder_lance"));
        assertEquals(DamageKind.EXPLOSION, kind(false, false, false, false, false, true, false, "explosion"));
        assertEquals(DamageKind.PROJECTILE, kind(false, false, false, false, false, false, false, "thrown_trident"));
    }

    @Test
    void aWordMustStartTheTokenNotJustAppearInsideOne() {
        assertEquals(DamageKind.SPELL, kind(false, false, false, false, false, true, false, "slice"), "slice is not ice");
        assertEquals(DamageKind.SPELL, kind(false, false, false, false, false, true, false, "device_ray"), "device is not ice");
        assertEquals(DamageKind.ICE, kind(false, false, false, false, false, true, false, "tensura:ice_spear"), "a namespace and a colon separate words too");
        assertEquals(DamageKind.FIRE, kind(false, false, false, false, false, true, false, "black.flame"));
    }

    @Test
    void aNullPathIsHarmless() {
        assertEquals(DamageKind.OTHER, kind(false, false, false, false, false, false, false, null));
    }

    @Test
    void physicalKindsAreBladesArrowsAndBlasts() {
        assertTrue(DamageKind.MELEE.physical() && DamageKind.PROJECTILE.physical() && DamageKind.EXPLOSION.physical());
        assertFalse(DamageKind.SPELL.physical() || DamageKind.FIRE.physical() || DamageKind.ICE.physical() || DamageKind.LIGHTNING.physical() || DamageKind.OTHER.physical());
        for (DamageKind k : DamageKind.values()) assertFalse(k.label().isBlank());
    }
}
