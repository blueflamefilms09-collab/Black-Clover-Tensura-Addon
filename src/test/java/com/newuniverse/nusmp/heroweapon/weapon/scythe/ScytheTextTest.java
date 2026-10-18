package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the Scythe's words are read by the tooltip, the Weapon Book and the language file, so they are held to the house rules (short names, descriptions that
 * fit a tooltip, plain ASCII, a sentence that ends) and to the numbers (a description that says "6 damage" really holds the constant), and the language table has
 * exactly the keys the dispatcher and the Weapon Book look up.
 */
class ScytheTextTest {
    @Test
    void everySkillHasAShortNameAndATooltipSizedDescription() {
        for (String id : ScytheText.skillIds()) {
            String name = ScytheText.skillName(id), desc = ScytheText.skillDesc(id);
            assertTrue(name.length() >= 3 && name.length() <= 28, id + " name length " + name.length());
            assertTrue(desc.length() >= 40 && desc.length() <= 330, id + " description length " + desc.length());
            assertTrue(desc.endsWith("."), id + " description ends with a full stop");
            assertTrue(plain(name) && plain(desc), id + " is plain ASCII");
            assertFalse(desc.contains("  "), id + " has no double space");
        }
    }

    @Test
    void everyPassiveIsDescribedAndPlain() {
        assertEquals(3, ScytheText.passiveIds().size());
        for (String id : ScytheText.passiveIds()) {
            assertTrue(ScytheText.passiveName(id).length() >= 3 && ScytheText.passiveName(id).length() <= 28, id);
            assertTrue(ScytheText.passiveDesc(id).length() >= 20 && ScytheText.passiveDesc(id).length() <= 200, id);
            assertTrue(plain(ScytheText.passiveName(id)) && plain(ScytheText.passiveDesc(id)), id);
            assertTrue(ScytheText.passiveDesc(id).endsWith("."), id);
        }
    }

    @Test
    void theDescriptionsHoldTheRealNumbers() {
        assertTrue(ScytheText.skillDesc(ScytheNumbers.FULL_MOON_ARMY).contains("within 4.8 blocks take 6 damage"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.FLYING_CIRCLE).contains("16 blocks"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.REAP).contains("at or under 30 percent health"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.REAP).contains("heals you 3 percent, at most 12 percent"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.PROTECTION_BREAK).contains("9 damage plus 0.6 per point"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.DEADLY_DANCE).contains("(12 in all)"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.FLOATING_SCYTHE).contains("6 seconds"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.SOUL_HARVEST).startsWith("Ultimate."));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.SOUL_HARVEST).contains("20 percent more on the weak"));
        assertTrue(ScytheText.skillDesc(ScytheNumbers.SCYTHE_ZERO).contains("no damage"));
        assertTrue(ScytheText.passiveDesc(ScytheNumbers.P_SOUL_TITHE).contains("up to 10"));
        assertTrue(ScytheText.passiveDesc(ScytheNumbers.P_SOUL_TITHE).contains("120 seconds"));
        assertTrue(ScytheText.passiveDesc(ScytheNumbers.P_DRAGON_SOUL).contains("14 souls"));
    }

    @Test
    void theLangTableHasExactlyTheKeysTheGameLooksUp() {
        Map<String, String> lang = ScytheText.lang();
        assertEquals(2 * ScytheText.skillIds().size() + 2 * ScytheText.passiveIds().size(), lang.size());
        for (String id : ScytheText.skillIds()) {
            assertEquals(ScytheText.skillName(id), lang.get("hero.skill." + id + ".name"));
            assertEquals(ScytheText.skillDesc(id), lang.get("hero.skill." + id + ".desc"));
        }
        for (String id : ScytheText.passiveIds()) {
            assertEquals(ScytheText.passiveName(id), lang.get("hero.passive.scythe." + id));
            assertEquals(ScytheText.passiveDesc(id), lang.get("hero.passive.scythe." + id + ".desc"));
        }
    }

    private static boolean plain(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < 32 || s.charAt(i) > 126) return false;
        return true;
    }
}
