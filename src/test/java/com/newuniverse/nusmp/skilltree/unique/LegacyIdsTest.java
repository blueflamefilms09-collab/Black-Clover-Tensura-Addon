package com.newuniverse.nusmp.skilltree.unique;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the removed ids are the contract with old worlds (a registry id that vanishes can make a save fail to load) and with the migration (a removed line must never become a skill), so the lists are pinned: the
 * counts the removal covered (29 magics, 30 techniques), that nothing is listed twice, that every id is a legal registry path and none collides with a live skill, and that the book id of a removed magic is
 * recognised while the book id of a live one never is.
 */
class LegacyIdsTest {
    @Test
    void thereAreTwentyNineRemovedMagicsAndThirtyTechniques() {
        assertEquals(29, LegacyIds.REMOVED_LINES.size());
        assertEquals(30, LegacyIds.REMOVED_TECHNIQUES.size());
        assertEquals(29, new HashSet<>(LegacyIds.REMOVED_LINES).size(), "no line twice");
        assertEquals(30, new HashSet<>(LegacyIds.REMOVED_TECHNIQUES).size(), "no technique twice");
    }

    @Test
    void everyIdIsALegalRegistryPathAndNoneCollidesWithALiveSkill() {
        Set<String> live = new HashSet<>();
        for (LineIndex.Line l : LineIndex.shipped()) live.add(l.skillPath());
        for (String t : LegacyIds.REMOVED_TECHNIQUES) {
            assertTrue(t.matches("[a-z0-9_]+"), t);
            assertFalse(live.contains(t), t);
        }
        for (String r : LegacyIds.REMOVED_LINES) {
            assertTrue(LineIndex.validBare(r), r);
            assertFalse(live.contains(LineIndex.skillPath(r)), r + " is a live skill and a removed one");
        }
    }

    @Test
    void theBookOfARemovedMagicIsRecognisedAndALiveOneNever() {
        assertTrue(LegacyIds.isRemovedBook("nusmp:fire_dragon_slayer_book"));
        assertTrue(LegacyIds.isRemovedBook(LegacyIds.bookId("celestial_spirit_magic")));
        assertFalse(LegacyIds.isRemovedBook("nusmp:lady_luck_book"));
        assertFalse(LegacyIds.isRemovedBook("othermod:fire_dragon_slayer_book"));
        assertFalse(LegacyIds.isRemovedBook(null));
        assertFalse(LegacyIds.isRemovedBook(""));
        assertFalse(LegacyIds.isRemovedBook("nusmp:_book"));
        for (LineIndex.Line l : LineIndex.shipped()) assertFalse(LegacyIds.isRemovedBook(l.book()), l.book());
    }

    @Test
    void aRemovedLineIsRecognisedBareOrNamespaced() {
        assertTrue(LegacyIds.isRemovedLine("ice_make"));
        assertTrue(LegacyIds.isRemovedLine("nusmp:ice_make"));
        assertFalse(LegacyIds.isRemovedLine("lady_luck"));
        assertFalse(LegacyIds.isRemovedLine(null));
    }
}
