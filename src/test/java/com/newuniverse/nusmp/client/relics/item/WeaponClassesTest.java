package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The class table, the id to class mapping and the keyword guess. */
class WeaponClassesTest {
    @Test
    void theKeywordGuessReadsTheId() {
        assertEquals("handaxe", WeaponClasses.guess("returning_hand_axe"));
        assertEquals("battleaxe", WeaponClasses.guess("axe_of_the_exile"));
        assertEquals("greataxe", WeaponClasses.guess("mountainguards_greataxe"));
        assertEquals("dagger", WeaponClasses.guess("unicorn_dagger"));
        assertEquals("greatsword", WeaponClasses.guess("buster_greatsword"));
        assertEquals("longsword", WeaponClasses.guess("some_new_sword"));
        assertEquals("staff", WeaponClasses.guess("catstaff"));
        assertEquals("crossbow", WeaponClasses.guess("hyperborean_crossbow"));
        assertEquals("item", WeaponClasses.guess("fortune_die"));
    }

    @Test
    void aStateMeshUsesItsBaseId() {
        assertEquals("bianca", WeaponClasses.baseId("bianca_pull2"));
        assertEquals("needler", WeaponClasses.baseId("needler_charged"));
        assertEquals("marids_valor", WeaponClasses.baseId("marids_valor_liquid"));
        assertEquals("ki_resonance_lantern", WeaponClasses.baseId("ki_resonance_lantern_red"));
        assertEquals("dragon_tooth_blade_red", WeaponClasses.baseId("dragon_tooth_blade_red"));
        assertEquals("returning_hand_axe", WeaponClasses.baseId("returning_hand_axe"));
    }

    @Test
    void theReviewedMappingBeatsTheGuessAndStateMeshesInherit() {
        Map<String, String> w = new HashMap<>();
        w.put("dead_blade", "glaive");
        w.put("needler", "crossbow");
        WeaponClasses c = new WeaponClasses(null, w);
        assertEquals("glaive", c.classOf("dead_blade").name());
        assertEquals("crossbow", c.classOf("needler_pull3").name());
        assertEquals("longsword", c.classOf("brand_new_blade").name(), "an unmapped id falls back to the guess");
        assertEquals(1.8f, c.classOf("dead_blade").min(), 1e-6);
    }

    @Test
    void theBuiltInTableHasTheRangesOfTheDocument() {
        WeaponClasses c = WeaponClasses.defaults();
        assertEquals(0.35f, c.classOf("x_dagger").min(), 1e-6);
        assertEquals(0.55f, c.classOf("x_dagger").max(), 1e-6);
        assertEquals(0.30f, c.classOf("x_hand_axe").headMin(), 1e-6);
        assertEquals(0.45f, c.classOf("x_axe").headMin(), 1e-6);
        assertEquals(0.70f, c.classOf("x_greataxe").headMin(), 1e-6);
        assertEquals(1.80f, c.classOf("x_spear").min(), 1e-6);
        Map<String, String> w = new HashMap<>();
        w.put("viper_whip", "coilwhip");
        assertEquals(0.90f, new WeaponClasses(null, w).classOf("viper_whip").min(), 1e-6, "a coiled whip is not held to the extended-whip length");
        assertTrue(c.isItem(c.classOf("fortune_die")));
        for (WeaponClasses.Cls k : WeaponClasses.DEFAULTS.values()) {
            assertTrue(k.max() >= k.min(), k.name());
            assertTrue(k.grip() > 0f && k.grip() < 1f, k.name());
        }
    }
}
