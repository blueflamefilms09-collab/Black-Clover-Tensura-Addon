package com.newuniverse.nusmp.skilltree.unique;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the retired ids are the contract with old worlds (a registry id that vanishes can make a save fail to load), so the lists are pinned: the counts the removals covered (284 + 12 retired with the
 * Skill Books, 29 magics and 30 techniques before that), that nothing is listed twice, that every id is a legal registry path, and that the book id of a removed magic is recognised.
 */
class LegacyIdsTest {
    @Test
    void theCountsAreThoseOfTheRemovals() {
        assertEquals(284, LegacyIds.RETIRED_LINES.size());
        assertEquals(12, LegacyIds.RETIRED_TECHNIQUES.size());
        assertEquals(29, LegacyIds.REMOVED_LINES.size());
        assertEquals(30, LegacyIds.REMOVED_TECHNIQUES.size());
        for (List<String> l : List.of(LegacyIds.RETIRED_LINES, LegacyIds.RETIRED_TECHNIQUES, LegacyIds.REMOVED_LINES, LegacyIds.REMOVED_TECHNIQUES))
            assertEquals(l.size(), new HashSet<>(l).size(), "no id twice");
    }

    @Test
    void everyIdIsALegalRegistryPathAndNoneIsRegisteredTwice() {
        Set<String> paths = new HashSet<>();
        for (String r : LegacyIds.RETIRED_LINES) { assertTrue(r.matches("[a-z0-9_]+"), r); assertTrue(paths.add("line_" + r), r); }
        for (String r : LegacyIds.REMOVED_LINES) { assertTrue(r.matches("[a-z0-9_]+"), r); assertTrue(paths.add("line_" + r), r + " is in two lists"); }
        for (String t : LegacyIds.RETIRED_TECHNIQUES) { assertTrue(t.matches("[a-z0-9_]+"), t); assertTrue(paths.add(t), t); }
        for (String t : LegacyIds.REMOVED_TECHNIQUES) { assertTrue(t.matches("[a-z0-9_]+"), t); assertTrue(paths.add(t), t + " is in two lists"); }
    }

    @Test
    void theBookOfARemovedMagicIsRecognised() {
        assertTrue(LegacyIds.isRemovedBook("nusmp:fire_dragon_slayer_book"));
        assertTrue(LegacyIds.isRemovedBook(LegacyIds.bookId("celestial_spirit_magic")));
        assertFalse(LegacyIds.isRemovedBook("nusmp:lady_luck_book"));
        assertFalse(LegacyIds.isRemovedBook("othermod:fire_dragon_slayer_book"));
        assertFalse(LegacyIds.isRemovedBook(null));
        assertFalse(LegacyIds.isRemovedBook(""));
        assertFalse(LegacyIds.isRemovedBook("nusmp:_book"));
    }

    @Test
    void aRemovedLineIsRecognisedBareOrNamespaced() {
        assertTrue(LegacyIds.isRemovedLine("ice_make"));
        assertTrue(LegacyIds.isRemovedLine("nusmp:ice_make"));
        assertFalse(LegacyIds.isRemovedLine("lady_luck"));
        assertFalse(LegacyIds.isRemovedLine(null));
    }
}
