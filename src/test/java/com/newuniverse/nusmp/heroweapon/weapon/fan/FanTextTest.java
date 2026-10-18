package com.newuniverse.nusmp.heroweapon.weapon.fan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the Fan's words are read by the tooltip, the Weapon Book and the language file, so they are held to the house rules (short names, descriptions that fit a
 * tooltip, plain ASCII, a sentence that ends) and to the numbers (a description that says "5 damage" really holds the constant), and the language table has
 * exactly the keys the dispatcher and the Weapon Book look up.
 */
class FanTextTest {
    @Test
    void everySkillHasAShortNameAndATooltipSizedDescription() {
        for (String id : FanText.skillIds()) {
            String name = FanText.skillName(id), desc = FanText.skillDesc(id);
            assertTrue(name.length() >= 3 && name.length() <= 28, id + " name length " + name.length());
            assertTrue(desc.length() >= 40 && desc.length() <= 260, id + " description length " + desc.length());
            assertTrue(desc.endsWith("."), id + " description ends with a full stop");
            assertTrue(plain(name) && plain(desc), id + " is plain ASCII");
            assertFalse(desc.contains("  "), id + " has no double space");
        }
    }

    @Test
    void everyPassiveIsDescribedAndPlain() {
        assertEquals(6, FanText.passiveIds().size());
        for (String id : FanText.passiveIds()) {
            assertTrue(FanText.passiveName(id).length() >= 3 && FanText.passiveName(id).length() <= 28, id);
            assertTrue(FanText.passiveDesc(id).length() >= 20 && FanText.passiveDesc(id).length() <= 200, id);
            assertTrue(plain(FanText.passiveName(id)) && plain(FanText.passiveDesc(id)), id);
            assertTrue(FanText.passiveDesc(id).endsWith("."), id);
        }
    }

    @Test
    void theDescriptionsHoldTheRealNumbers() {
        assertTrue(FanText.skillDesc(FanNumbers.FLOWER_WIND).contains("within 5.5 blocks"));
        assertTrue(FanText.skillDesc(FanNumbers.FLOWER_WIND).contains("take 5 damage"));
        assertTrue(FanText.skillDesc(FanNumbers.MOON_BREAK).contains("14 blocks"));
        assertTrue(FanText.skillDesc(FanNumbers.MOON_BREAK).contains("8 damage"));
        assertTrue(FanText.skillDesc(FanNumbers.MIST_VEIL).contains("4 seconds"));
        assertTrue(FanText.skillDesc(FanNumbers.SNOW_PLUM).contains("25 percent"));
        assertTrue(FanText.skillDesc(FanNumbers.TORTOISE_SHELL_CRACKER).contains("10 damage"));
        assertTrue(FanText.skillDesc(FanNumbers.SNOW_MOON_FLOWER).contains("3 seconds"));
        assertTrue(FanText.skillDesc(FanNumbers.SNOW_MOON_FLOWER).startsWith("Ultimate."));
        assertTrue(FanText.passiveDesc(FanNumbers.P_FEATHERFALL).contains("60 percent"));
        assertTrue(FanText.passiveDesc(FanNumbers.P_FEATHERFALL).contains("holding sneak"));
        assertTrue(FanText.passiveDesc(FanNumbers.P_KEYSTONE_WARD).contains("40 percent"));
    }

    @Test
    void theLangTableHasExactlyTheKeysTheGameLooksUp() {
        Map<String, String> lang = FanText.lang();
        assertEquals(2 * FanText.skillIds().size() + 2 * FanText.passiveIds().size(), lang.size());
        for (String id : FanText.skillIds()) {
            assertEquals(FanText.skillName(id), lang.get("hero.skill." + id + ".name"));
            assertEquals(FanText.skillDesc(id), lang.get("hero.skill." + id + ".desc"));
        }
        for (String id : FanText.passiveIds()) {
            assertEquals(FanText.passiveName(id), lang.get("hero.passive.fan." + id));
            assertEquals(FanText.passiveDesc(id), lang.get("hero.passive.fan." + id + ".desc"));
        }
    }

    private static boolean plain(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < 32 || s.charAt(i) > 126) return false;
        return true;
    }
}
