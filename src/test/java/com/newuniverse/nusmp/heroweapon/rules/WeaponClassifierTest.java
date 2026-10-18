package com.newuniverse.nusmp.heroweapon.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.rules.WeaponClassifier.Facts;
import org.junit.jupiter.api.Test;

/**
 * Why: the rejection rule is the owner's first restriction and it is a table, so the table is pinned: which facts make which class (in the documented order),
 * which Holy wielder is shocked by which class, and the cases that matter for fairness (tools are never weapons, the allow tag beats everything, a Vassal hero
 * weapon is never rejected, a Holy wielder's own class is allowed).
 */
class WeaponClassifierTest {
    private static Facts f(int flags) { return new Facts(null, flags, 0.0); }

    @Test
    void theFactsMakeTheDocumentedClass() {
        assertEquals(WeaponClass.SWORD, WeaponClassifier.classify(f(WeaponClassifier.SWORD)));
        assertEquals(WeaponClass.AXE, WeaponClassifier.classify(f(WeaponClassifier.AXE)));
        assertEquals(WeaponClass.SPEAR, WeaponClassifier.classify(f(WeaponClassifier.SPEAR)));
        assertEquals(WeaponClass.BOW, WeaponClassifier.classify(f(WeaponClassifier.BOW)));
        assertEquals(WeaponClass.SHIELD, WeaponClassifier.classify(f(WeaponClassifier.SHIELD)));
        assertEquals(WeaponClass.BLUNT, WeaponClassifier.classify(f(WeaponClassifier.BLUNT)));
        assertEquals(WeaponClass.NONE, WeaponClassifier.classify(f(0)), "an apple");
        assertEquals(WeaponClass.NONE, WeaponClassifier.classify(null));
    }

    @Test
    void theOrderOfTheRulesIsTheDocumentedOne() {
        // how it is used beats what it is called: a shield that is also tagged a sword is a shield; a bow that has a spear tag is a bow
        assertEquals(WeaponClass.SHIELD, WeaponClassifier.classify(f(WeaponClassifier.SHIELD | WeaponClassifier.SWORD)));
        assertEquals(WeaponClass.BOW, WeaponClassifier.classify(f(WeaponClassifier.BOW | WeaponClassifier.SPEAR)));
        assertEquals(WeaponClass.SPEAR, WeaponClassifier.classify(f(WeaponClassifier.SPEAR | WeaponClassifier.SWORD)));
        assertEquals(WeaponClass.SWORD, WeaponClassifier.classify(f(WeaponClassifier.SWORD | WeaponClassifier.AXE)));
        // an item that declares its class is believed, even against its tags
        assertEquals(WeaponClass.BOW, WeaponClassifier.classify(new Facts(WeaponClass.BOW, WeaponClassifier.SWORD, 9.0)));
        // the allow tag beats everything, including a declared class
        assertEquals(WeaponClass.NONE, WeaponClassifier.classify(new Facts(WeaponClass.BOW, WeaponClassifier.ALLOWED | WeaponClassifier.BOW, 9.0)));
    }

    @Test
    void toolsAreNotWeaponsAndHardHittersAre() {
        assertEquals(WeaponClass.NONE, WeaponClassifier.classify(new Facts(null, WeaponClassifier.TOOL, 5.0)), "a netherite pickaxe hits hard but is a tool");
        assertEquals(WeaponClass.OTHER_WEAPON, WeaponClassifier.classify(new Facts(null, 0, 4.0)), "exactly 4 is a weapon");
        assertEquals(WeaponClass.NONE, WeaponClassifier.classify(new Facts(null, 0, 3.99)), "just under 4 is not");
        assertEquals(WeaponClass.OTHER_WEAPON, WeaponClassifier.classify(new Facts(null, 0, 9.0)));
        assertEquals(WeaponClass.AXE, WeaponClassifier.classify(new Facts(null, WeaponClassifier.AXE | WeaponClassifier.TOOL, 9.0)), "an axe is an axe even though it is also a tool");
    }

    @Test
    void ownClassOfTheHolyWeapons() {
        assertEquals(WeaponClass.SHIELD, WeaponClassifier.ownClass(HeroKind.SHIELD));
        assertEquals(WeaponClass.SWORD, WeaponClassifier.ownClass(HeroKind.SWORD));
        assertEquals(WeaponClass.SPEAR, WeaponClassifier.ownClass(HeroKind.SPEAR));
        assertEquals(WeaponClass.BOW, WeaponClassifier.ownClass(HeroKind.BOW));
        for (HeroKind k : HeroKind.values()) if (!k.isHoly()) assertEquals(WeaponClass.NONE, WeaponClassifier.ownClass(k), k + " has no class to defend");
        assertEquals(WeaponClass.NONE, WeaponClassifier.ownClass(null));
    }

    @Test
    void aHolyWielderIsShockedByAnyOtherWeaponClassAndOnlyThat() {
        for (HeroKind w : HeroKind.values()) {
            for (WeaponClass held : WeaponClass.values()) {
                boolean want = w.isHoly() && held != WeaponClass.NONE && held != WeaponClassifier.ownClass(w);
                assertEquals(want, WeaponClassifier.rejects(w, held), w + " holding " + held);
            }
        }
        assertTrue(WeaponClassifier.rejects(HeroKind.SWORD, WeaponClass.AXE));
        assertTrue(WeaponClassifier.rejects(HeroKind.SHIELD, WeaponClass.OTHER_WEAPON));
        assertFalse(WeaponClassifier.rejects(HeroKind.SWORD, WeaponClass.SWORD), "the Sword hero may use any sword");
        assertFalse(WeaponClassifier.rejects(HeroKind.BOW, WeaponClass.BOW), "the Bow hero may use any bow or crossbow");
        assertFalse(WeaponClassifier.rejects(HeroKind.SCYTHE, WeaponClass.AXE), "a Vassal hero is never shocked");
        assertFalse(WeaponClassifier.rejects(HeroKind.HAMMER, WeaponClass.SWORD), "a Seven Star hero is never shocked");
        assertFalse(WeaponClassifier.rejects(null, WeaponClass.SWORD));
        assertFalse(WeaponClassifier.rejects(HeroKind.SHIELD, null));
    }

    @Test
    void onlyBowsAndSpearsAreFiredWhenUsed() {
        for (WeaponClass c : WeaponClass.values()) {
            assertEquals(c == WeaponClass.BOW || c == WeaponClass.SPEAR, WeaponClassifier.firesWhenUsed(c), c.toString());
        }
    }
}
