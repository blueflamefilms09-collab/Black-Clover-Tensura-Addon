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
        assertEquals("twin_hook_swords", WeaponClasses.baseId("twin_hook_swords_left"), "1.34.0: the left half of a pair follows its right half");
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
        assertEquals(2.8f, c.classOf("dead_blade").min(), 1e-6);
    }

    @Test
    void theBuiltInTableHasTheRangesOfTheOwnersSizeRule() {
        WeaponClasses c = WeaponClasses.defaults();
        assertEquals(0.45f, c.classOf("x_dagger").min(), 1e-6);
        assertEquals(0.70f, c.classOf("x_dagger").max(), 1e-6);
        assertEquals(1.40f, c.classOf("x_longsword").min(), 1e-6);
        assertEquals(1.80f, c.classOf("x_katana").max(), 1e-6);
        assertEquals(2.20f, c.classOf("x_greatsword").min(), 1e-6);
        assertEquals(3.00f, c.classOf("x_greatsword").max(), 1e-6);
        assertEquals(0.50f, c.classOf("x_hand_axe").headMin(), 1e-6);
        assertEquals(0.75f, c.classOf("x_axe").headMin(), 1e-6);
        assertEquals(1.10f, c.classOf("x_greataxe").headMin(), 1e-6);
        assertEquals(2.80f, c.classOf("x_spear").min(), 1e-6);
        assertEquals(3.60f, c.classOf("x_spear").max(), 1e-6);
        assertEquals(2.00f, c.classOf("x_staff").min(), 1e-6);
        assertEquals(1.00f, c.classOf("x_crossbow").min(), 1e-6);
        assertEquals(0.90f, c.classOf("x_shield").min(), 1e-6);
        assertEquals("greathammer", WeaponClasses.guess("x_greathammer"));
        assertEquals(2.20f, c.classOf("x_greathammer").min(), 1e-6);
        Map<String, String> w = new HashMap<>();
        w.put("viper_whip", "coilwhip");
        assertEquals(1.10f, new WeaponClasses(null, w).classOf("viper_whip").min(), 1e-6, "a coiled whip is not held to the extended-whip length");
        w.put("lantern", "handitem");
        WeaponClasses held = new WeaponClasses(null, w);
        assertTrue(held.isItem(held.classOf("lantern")));
        assertEquals(0.50f, held.classOf("lantern").min(), 1e-6, "a held item is 0.5 to 0.9 blocks");
        assertEquals(0.90f, held.classOf("lantern").max(), 1e-6);
        assertTrue(c.isItem(c.classOf("fortune_die")));
        assertEquals(0.50f, WeaponClasses.ITEM_MIN_EXTENT, 1e-6);
        for (WeaponClasses.Cls k : WeaponClasses.DEFAULTS.values()) {
            assertTrue(k.max() >= k.min(), k.name());
            assertTrue(k.grip() > 0f && k.grip() < 1f, k.name());
        }
    }
}
