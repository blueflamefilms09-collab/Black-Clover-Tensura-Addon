package com.newuniverse.nusmp.avatar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The personality table and the rules for mixing two personalities. */
class AvatarPersonalityTraitsTest {
    @Test
    void everyPersonalityHasSaneNumbers() {
        for (AvatarPersonality p : AvatarPersonality.values()) {
            var t = AvatarPersonalityTraits.of(p);
            assertTrue(t.followDistance() >= 1 && t.followDistance() <= 12, p + " follow distance");
            assertTrue(t.aggroRange() >= 4 && t.aggroRange() <= 32, p + " aggro range");
            assertTrue(t.retreatHp() >= 0 && t.retreatHp() < 1, p + " retreat hp");
            assertTrue(t.attackSpeed() > 0.2 && t.attackSpeed() < 2, p + " attack speed");
            assertTrue(t.cooldownMult() > 0.2 && t.cooldownMult() <= 1.5, p + " cooldown mult");
            assertFalse(t.styles().isEmpty(), p + " has speech style tags");
        }
    }

    @Test
    void theBriefsNumbersHold() {
        var lazy = AvatarPersonalityTraits.of(AvatarPersonality.LAZY);
        assertEquals(0.7, lazy.cooldownMult(), 1e-9);
        assertEquals(0.6, lazy.attackSpeed(), 1e-9);
        assertTrue(lazy.idlesWithCreator());
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.SADISTIC).avoidsWeakPoints());
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.COWARDLY).fleesBosses());
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.MERCHANT).pullsLoot());
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.GLUTTONOUS).eatsFood());
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.STOIC).oneWordSpeech());
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.CHUNIBYO).renamesAttacks());
        assertEquals(0.5, AvatarPersonalityTraits.of(AvatarPersonality.TSUNDERE).protectBelowHp(), 1e-9);
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.HEROIC).defendsVillagers());
        assertTrue(AvatarPersonalityTraits.of(AvatarPersonality.SCHOLAR).commentsOnRelics());
    }

    @Test
    void aSinglePersonalityIsItself() {
        assertSame(AvatarPersonalityTraits.of(AvatarPersonality.LOYAL), AvatarPersonalityTraits.combine(AvatarPersonality.LOYAL, null));
        assertSame(AvatarPersonalityTraits.of(AvatarPersonality.LOYAL), AvatarPersonalityTraits.combine(AvatarPersonality.LOYAL, AvatarPersonality.LOYAL));
        assertEquals(AvatarPersonalityTraits.of(AvatarPersonality.LOYAL), AvatarPersonalityTraits.combine(null, null), "no personality reads as loyal");
    }

    @Test
    void combiningFollowsTheDocumentedRules() {
        var a = AvatarPersonalityTraits.of(AvatarPersonality.LAZY);
        var b = AvatarPersonalityTraits.of(AvatarPersonality.AGGRESSIVE);
        var c = AvatarPersonalityTraits.combine(AvatarPersonality.LAZY, AvatarPersonality.AGGRESSIVE);
        assertEquals((a.followDistance() + b.followDistance()) / 2, c.followDistance(), 1e-9, "follow distance is averaged");
        assertEquals(Math.max(a.aggroRange(), b.aggroRange()), c.aggroRange(), 1e-9, "aggro range takes the larger");
        assertEquals(Math.min(a.cooldownMult(), b.cooldownMult()), c.cooldownMult(), 1e-9, "cooldown takes the lower");
        assertEquals((a.attackSpeed() + b.attackSpeed()) / 2, c.attackSpeed(), 1e-9, "attack speed is averaged");
        assertTrue(c.idlesWithCreator(), "flags are OR-ed");
        assertTrue(c.styles().containsAll(a.styles()) && c.styles().containsAll(b.styles()), "styles are united");
    }

    @Test
    void protectionThresholdIgnoresAParentWithoutOne() {
        var c = AvatarPersonalityTraits.combine(AvatarPersonality.TSUNDERE, AvatarPersonality.LOYAL);
        assertEquals(0.5, c.protectBelowHp(), 1e-9);
        var d = AvatarPersonalityTraits.combine(AvatarPersonality.LOYAL, AvatarPersonality.LOYAL);
        assertEquals(0.0, d.protectBelowHp(), 1e-9);
    }

    @Test
    void combiningIsSymmetricForNumbersThatAverageOrPickAnExtreme() {
        var x = AvatarPersonalityTraits.combine(AvatarPersonality.SCHOLAR, AvatarPersonality.COWARDLY);
        var y = AvatarPersonalityTraits.combine(AvatarPersonality.COWARDLY, AvatarPersonality.SCHOLAR);
        assertEquals(x.followDistance(), y.followDistance(), 1e-9);
        assertEquals(x.aggroRange(), y.aggroRange(), 1e-9);
        assertEquals(x.cooldownMult(), y.cooldownMult(), 1e-9);
        assertEquals(x.retreatHp(), y.retreatHp(), 1e-9);
    }

    @Test
    void dndModifiersAndMentalCooldown() {
        assertEquals(0, AvatarPersonalityTraits.modifier(10));
        assertEquals(0, AvatarPersonalityTraits.modifier(11));
        assertEquals(1, AvatarPersonalityTraits.modifier(12));
        assertEquals(-1, AvatarPersonalityTraits.modifier(9));
        assertEquals(-1, AvatarPersonalityTraits.modifier(8));
        assertEquals(5, AvatarPersonalityTraits.modifier(20));
        assertEquals(1.0, AvatarPersonalityTraits.mentalCooldownMult(10, 10), 1e-9);
        assertTrue(AvatarPersonalityTraits.mentalCooldownMult(18, 16) < 1.0, "a clever wise caster is faster");
        assertTrue(AvatarPersonalityTraits.mentalCooldownMult(6, 6) > 1.0, "a dull one is slower");
        assertEquals(0.5, AvatarPersonalityTraits.mentalCooldownMult(40, 40), 1e-9, "never faster than half");
        assertEquals(1.25, AvatarPersonalityTraits.mentalCooldownMult(1, 1), 1e-9, "never slower than x1.25");
    }
}
