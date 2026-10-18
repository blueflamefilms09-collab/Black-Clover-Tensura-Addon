package com.newuniverse.nusmp.heroweapon.weapon.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the Book's words are read by the tooltip, the Weapon Book, the action bar and the language file, so they are held to the house rules (short names,
 * descriptions that fit a tooltip, plain ASCII, a sentence that ends) and to the numbers (a description that says "9 damage" really holds the constant), the
 * pages have names, and the language table has exactly the keys the dispatcher and the Weapon Book look up.
 */
class BookTextTest {
    @Test
    void everySkillHasAShortNameAndATooltipSizedDescription() {
        for (String id : BookText.skillIds()) {
            String name = BookText.skillName(id), desc = BookText.skillDesc(id);
            assertTrue(name.length() >= 3 && name.length() <= 28, id + " name length " + name.length());
            assertTrue(desc.length() >= 40 && desc.length() <= 380, id + " description length " + desc.length());
            assertTrue(desc.endsWith("."), id + " description ends with a full stop");
            assertTrue(plain(name) && plain(desc), id + " is plain ASCII");
            assertFalse(desc.contains("  "), id + " has no double space");
        }
    }

    @Test
    void everyPassiveIsDescribedAndPlain() {
        assertEquals(5, BookText.passiveIds().size());
        for (String id : BookText.passiveIds()) {
            assertTrue(BookText.passiveName(id).length() >= 3 && BookText.passiveName(id).length() <= 28, id);
            assertTrue(BookText.passiveDesc(id).length() >= 20 && BookText.passiveDesc(id).length() <= 230, id);
            assertTrue(plain(BookText.passiveName(id)) && plain(BookText.passiveDesc(id)), id);
            assertTrue(BookText.passiveDesc(id).endsWith("."), id);
        }
    }

    @Test
    void everyPageHasAName() {
        assertEquals(6, BookText.pageIds().size());
        for (String p : BookText.pageIds()) assertTrue(BookText.pageName(p).length() >= 4 && plain(BookText.pageName(p)), p);
        assertEquals("Bird of Flame", BookText.pageName(BookNumbers.BIRD));
        assertEquals("nothing", BookText.pageName(null));
        assertEquals("odd", BookText.pageName("odd"), "an unknown page keeps its id");
    }

    @Test
    void theDescriptionsHoldTheRealNumbers() {
        assertTrue(BookText.skillDesc(BookNumbers.READ_COMPOSITION).contains("6 damage along a line of 14 blocks"));
        assertTrue(BookText.skillDesc(BookNumbers.READ_COMPOSITION).contains("4 volleys of 2.5"));
        assertTrue(BookText.skillDesc(BookNumbers.READ_COMPOSITION).contains("a beam of 9 damage"));
        assertTrue(BookText.skillDesc(BookNumbers.READ_VERSE).contains("3 pulses"));
        assertTrue(BookText.skillDesc(BookNumbers.READ_VERSE).contains("25 percent"));
        assertTrue(BookText.skillDesc(BookNumbers.PAPER_BURST).contains("within 3.5 blocks take 9 damage"));
        assertTrue(BookText.skillDesc(BookNumbers.LIFE_FORCE_BURST).contains("14 damage"));
        assertTrue(BookText.skillDesc(BookNumbers.LIFE_FORCE_BURST).contains("1.2 seconds"));
        assertTrue(BookText.skillDesc(BookNumbers.CONSTRICTION).contains("up to 2 seconds"));
        assertTrue(BookText.skillDesc(BookNumbers.CONSTRICTION).contains("6 damage"));
        assertTrue(BookText.skillDesc(BookNumbers.LIBRARIA).startsWith("Ultimate."));
        assertTrue(BookText.skillDesc(BookNumbers.LIBRARIA).contains("8 volleys"));
        assertTrue(BookText.skillDesc(BookNumbers.LIBRARIA).contains("(28 in all)"));
        assertTrue(BookText.passiveDesc(BookNumbers.P_REVISED).contains("25 percent"));
        assertTrue(BookText.passiveDesc(BookNumbers.P_GRAND).contains("10 volleys"));
    }

    @Test
    void theLangTableHasExactlyTheKeysTheGameLooksUp() {
        Map<String, String> lang = BookText.lang();
        assertEquals(2 * BookText.skillIds().size() + 2 * BookText.passiveIds().size(), lang.size());
        for (String id : BookText.skillIds()) {
            assertEquals(BookText.skillName(id), lang.get("hero.skill." + id + ".name"));
            assertEquals(BookText.skillDesc(id), lang.get("hero.skill." + id + ".desc"));
        }
        for (String id : BookText.passiveIds()) {
            assertEquals(BookText.passiveName(id), lang.get("hero.passive.book." + id));
            assertEquals(BookText.passiveDesc(id), lang.get("hero.passive.book." + id + ".desc"));
        }
    }

    private static boolean plain(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < 32 || s.charAt(i) > 126) return false;
        return true;
    }
}
