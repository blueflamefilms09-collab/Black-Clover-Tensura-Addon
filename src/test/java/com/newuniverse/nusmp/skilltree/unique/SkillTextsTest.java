package com.newuniverse.nusmp.skilltree.unique;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the words a unique skill shows in Tensura's screen come from one pure class, so they are pinned: the keys follow the {@code nusmp.skill.line_<id>} pattern the earlier stand-ins used (a
 * lang entry written for them still names the skill), the description lists the arts and passives only when there are any, and a skill with no art still has one generic mode (a skill needs at least one).
 */
class SkillTextsTest {
    private static LineIndex.Line line(List<String> arts, List<String> passives, String desc) {
        return new LineIndex.Line("test_line", "nusmp:test_line", "Test Line", 0xFFFFFF, "tensura", "nusmp:test_line_book", "Test Book", desc, "", arts, passives);
    }

    @Test
    void theKeysFollowTheStandInPattern() {
        assertEquals("nusmp.skill.line_test_line", SkillTexts.nameKey("test_line"));
        assertEquals("nusmp.skill.line_test_line.description", SkillTexts.descKey("test_line"));
    }

    @Test
    void theDescriptionListsArtsAndPassivesInOrder() {
        String d = SkillTexts.description(line(List.of("First", "Second"), List.of("Hide"), "A line."));
        assertEquals("A line.\nArts (press the skill key, scroll to change): First, Second.\nWhile switched on: Hide.", d);
    }

    @Test
    void anEmptyPartIsLeftOut() {
        assertEquals("Just words.", SkillTexts.description(line(List.of(), List.of(), "Just words.")));
        assertEquals("Arts (press the skill key, scroll to change): One.", SkillTexts.description(line(List.of("One"), List.of(), "")));
        assertEquals("", SkillTexts.description(line(List.of(), List.of(), "")));
    }

    @Test
    void aSkillWithNoArtHasOneGenericMode() {
        assertEquals(List.of("Test Line"), SkillTexts.modeNames(line(List.of(), List.of("P"), "d")));
        assertEquals(List.of("A", "B"), SkillTexts.modeNames(line(List.of("A", "B"), List.of(), "d")));
    }

    @Test
    void theLangForAListHasANameAndADescriptionPerLine() {
        Map<String, String> lang = SkillTexts.langFor(List.of(line(List.of("A"), List.of(), "d")));
        assertEquals(2, lang.size());
        assertEquals("Test Line", lang.get("nusmp.skill.line_test_line"));
        assertTrue(lang.get("nusmp.skill.line_test_line.description").startsWith("d\nArts"));
        assertFalse(lang.containsKey("nusmp.skill.line_other"));
    }
}
