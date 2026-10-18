package com.newuniverse.nusmp.heroweapon.weapon.katana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the Katana's words are read by the tooltip, the Weapon Book and the language file, so they are held to the house rules (short names, descriptions that
 * fit a tooltip, plain ASCII, a sentence that ends) and to the numbers (a description that says "7 damage" really holds the constant), and the language table has
 * exactly the keys the dispatcher and the Weapon Book look up.
 */
class KatanaTextTest {
    @Test
    void everySkillHasAShortNameAndATooltipSizedDescription() {
        for (String id : KatanaText.skillIds()) {
            String name = KatanaText.skillName(id), desc = KatanaText.skillDesc(id);
            assertTrue(name.length() >= 3 && name.length() <= 28, id + " name length " + name.length());
            assertTrue(desc.length() >= 40 && desc.length() <= 330, id + " description length " + desc.length());
            assertTrue(desc.endsWith("."), id + " description ends with a full stop");
            assertTrue(plain(name) && plain(desc), id + " is plain ASCII");
            assertFalse(desc.contains("  "), id + " has no double space");
        }
    }

    @Test
    void everyPassiveIsDescribedAndPlain() {
        assertEquals(3, KatanaText.passiveIds().size());
        for (String id : KatanaText.passiveIds()) {
            assertTrue(KatanaText.passiveName(id).length() >= 3 && KatanaText.passiveName(id).length() <= 28, id);
            assertTrue(KatanaText.passiveDesc(id).length() >= 20 && KatanaText.passiveDesc(id).length() <= 230, id);
            assertTrue(plain(KatanaText.passiveName(id)) && plain(KatanaText.passiveDesc(id)), id);
            assertTrue(KatanaText.passiveDesc(id).endsWith("."), id);
        }
    }

    @Test
    void theDescriptionsHoldTheRealNumbers() {
        assertTrue(KatanaText.skillDesc(KatanaNumbers.CRESCENT_MOON).contains("9 blocks long for 6.5 damage"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.INSTANT_BLADE).contains("up to 7 blocks"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.INSTANT_BLADE).contains("for 9 damage"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.COUNTER_STANCE).contains("0.7 seconds"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.COUNTER_STANCE).contains("60 percent of your health"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.COUNTER_STANCE).contains("4 to 12 damage"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.TRIPLE_THRUST).contains("3 times"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.STARDUST_BLADE).contains("(12 in all)"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.DOUBLE_SWORD).contains("6 seconds"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.DOUBLE_SWORD).contains("next 4 melee hits"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.SOUL_SLICE).contains("take back 50 percent"));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.SAKURA_DANCE).startsWith("Ultimate."));
        assertTrue(KatanaText.skillDesc(KatanaNumbers.SAKURA_DANCE).contains("(up to 30 in all)"));
        assertTrue(KatanaText.passiveDesc(KatanaNumbers.P_IAI_TEMPO).contains("4 seconds"));
    }

    @Test
    void theLangTableHasExactlyTheKeysTheGameLooksUp() {
        Map<String, String> lang = KatanaText.lang();
        assertEquals(2 * KatanaText.skillIds().size() + 2 * KatanaText.passiveIds().size(), lang.size());
        for (String id : KatanaText.skillIds()) {
            assertEquals(KatanaText.skillName(id), lang.get("hero.skill." + id + ".name"));
            assertEquals(KatanaText.skillDesc(id), lang.get("hero.skill." + id + ".desc"));
        }
        for (String id : KatanaText.passiveIds()) {
            assertEquals(KatanaText.passiveName(id), lang.get("hero.passive.katana." + id));
            assertEquals(KatanaText.passiveDesc(id), lang.get("hero.passive.katana." + id + ".desc"));
        }
    }

    private static boolean plain(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < 32 || s.charAt(i) > 126) return false;
        return true;
    }
}
