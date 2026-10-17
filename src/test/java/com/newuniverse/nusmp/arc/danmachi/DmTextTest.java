package com.newuniverse.nusmp.arc.danmachi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the lines of the fight must show the numbers the rules used (a Level that is not the real one would teach the player a wrong counter).
 */
class DmTextTest {
    @Test
    void theBarShowsLevelPowerAndTheStateOfTheGates() {
        assertEquals("Minotaur Lv.2 - 40%", DmText.bar("Minotaur", 2, 40, false, false));
        assertEquals("Minotaur Lv.2 - 100% - Gates open", DmText.bar("Minotaur", 2, 100, true, false));
        assertEquals("Minotaur Lv.3 - 100% - Leveled", DmText.bar("Minotaur", 3, 100, true, true));
        assertEquals("Udaeus Lv.9 - 20%", DmText.bar("Udaeus", 12, 20, false, false), "a Level is never shown above the top");
    }

    @Test
    void theLevelUpNoticeNamesBothLevelsAndTheSteps() {
        String one = DmText.levelUp("Udaeus", 6, 7, 1);
        assertTrue(one.contains("Lv.6") && one.contains("Lv.7") && !one.contains("steps"), one);
        String three = DmText.levelUp("Udaeus", 6, 9, 3);
        assertTrue(three.contains("Lv.9") && three.contains("3 steps"), three);
    }

    @Test
    void theHoldNoticeSaysWhatThePartyNeeds() {
        String weak = DmText.holds("Minotaur", 1, 2, 0);
        assertTrue(weak.contains("Lv.1") && weak.contains("Lv.2"), weak);
        assertTrue(DmText.holds("Minotaur", 0, 2, 0).contains("unmeasured"));
        assertTrue(DmText.holds("Udaeus", 3, 6, 2).contains("Lv.8"), "the margin raises what the party needs");
        assertTrue(DmText.holds("Udaeus", 3, 9, 4).contains("Lv.9"), "never above the top Level");
        assertTrue(DmText.holds("Udaeus", 3, 2, -4).contains("Lv.1"), "never under the first Level");
    }

    @Test
    void everyHintIsWordsAndNamesItsNumbers() {
        assertFalse(DmText.glance().isBlank());
        assertTrue(DmText.plate(2, 3).contains("2/3"));
        assertTrue(DmText.hideBroken("Black Goliath").contains("Black Goliath"));
        assertTrue(DmText.ribCracked(1, 3).contains("1/3"));
        assertTrue(DmText.cageBroken("Udaeus").contains("Udaeus"));
        assertTrue(DmText.headCut("Amphisbaena").contains("Amphisbaena"));
        assertTrue(DmText.markStolen("Bell").startsWith("Bell"));
        assertFalse(DmText.marked().isBlank());
        assertTrue(DmText.crashed("The Minotaur").startsWith("The Minotaur"));
    }
}
