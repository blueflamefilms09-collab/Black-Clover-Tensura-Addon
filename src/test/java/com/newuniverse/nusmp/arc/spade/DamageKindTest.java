package com.newuniverse.nusmp.arc.spade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The one answer to "what kind of hit is this" that Morris's tuning and Lucifero's shroud both rely on. */
class DamageKindTest {
    @Test
    void aBlowSweptByHandIsMelee() {
        assertEquals(DamageKind.MELEE, DamageKind.classify(true, false, "player_attack", false));
        assertEquals(DamageKind.MELEE, DamageKind.classify(true, false, "mob_attack", false));
        assertEquals(DamageKind.MELEE, DamageKind.classify(true, false, "some_mod:thorns", false));
        assertEquals(DamageKind.MELEE, DamageKind.classify(true, false, null, false), "a living attacker and a type nobody listed is still a blow");
    }

    @Test
    void aProjectileIsRangedEvenIfItsTypeSoundsLikeMagic() {
        assertEquals(DamageKind.RANGED, DamageKind.classify(true, true, "arrow", false));
        assertEquals(DamageKind.RANGED, DamageKind.classify(true, true, "fireball", false), "the projectile tag wins over the word fire");
        assertEquals(DamageKind.RANGED, DamageKind.classify(true, true, "indirect_magic", false));
    }

    @Test
    void spellsAndSkillsAreMagic() {
        assertEquals(DamageKind.MAGIC, DamageKind.classify(true, false, "indirect_magic", false));
        assertEquals(DamageKind.MAGIC, DamageKind.classify(true, false, "gravity_press", false));
        assertEquals(DamageKind.MAGIC, DamageKind.classify(true, false, "holy_damage", false));
        assertEquals(DamageKind.MAGIC, DamageKind.classify(true, false, "Player_Explosion", false), "case does not matter");
        assertEquals(DamageKind.MAGIC, DamageKind.classify(true, false, "lightning_bolt", false));
    }

    @Test
    void antiMagicBeatsEverythingAndNoAttackerIsNotABlow() {
        assertEquals(DamageKind.ANTI_MAGIC, DamageKind.classify(true, false, "player_attack", true));
        assertEquals(DamageKind.ANTI_MAGIC, DamageKind.classify(true, true, "arrow", true));
        assertEquals(DamageKind.ANTI_MAGIC, DamageKind.classify(false, false, "magic", true), "the caller decided it is anti-magic");
        assertEquals(DamageKind.OTHER, DamageKind.classify(false, false, "fall", false));
        assertEquals(DamageKind.OTHER, DamageKind.classify(false, false, "magic", false), "a field left on the ground is nobody's spell");
        assertEquals(DamageKind.OTHER, DamageKind.classify(false, true, "arrow", false));
    }

    @Test
    void onlyTheThreeBlowKindsCanBeTuned() {
        assertTrue(DamageKind.MELEE.tunable() && DamageKind.RANGED.tunable() && DamageKind.MAGIC.tunable());
        assertFalse(DamageKind.ANTI_MAGIC.tunable());
        assertFalse(DamageKind.OTHER.tunable());
        assertEquals("anti-magic", DamageKind.ANTI_MAGIC.word());
        assertEquals("melee", DamageKind.MELEE.word());
    }
}
