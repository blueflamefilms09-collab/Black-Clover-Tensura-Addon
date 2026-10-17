package com.newuniverse.nusmp.arc.story;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The stored story of one player: once-only records, normalised ids, bounded quest stages, independent copies. */
class StoryDataTest {
    @Test
    void aDefeatAndAMeetingCountOnceWhateverTheCase() {
        StoryData d = new StoryData();
        assertTrue(d.isEmpty());
        assertTrue(d.defeat("Heath_Grice"));
        assertFalse(d.defeat("heath_grice"));
        assertFalse(d.defeat("  HEATH_GRICE "));
        assertTrue(d.hasDefeated("HEATH_GRICE"));
        assertEquals(List.of("heath_grice"), d.defeatedList());
        assertTrue(d.meet("Yami"));
        assertFalse(d.meet("yami"));
        assertTrue(d.hasMet("YAMI"));
        assertFalse(d.hasMet("asta"));
        assertFalse(d.isEmpty());
    }

    @Test
    void blankAndNullIdsAreIgnored() {
        StoryData d = new StoryData();
        assertFalse(d.defeat(null));
        assertFalse(d.defeat("  "));
        assertFalse(d.meet(""));
        assertFalse(d.setQuestStage(null, 2));
        assertFalse(d.hasDefeated(null));
        assertTrue(d.isEmpty());
    }

    @Test
    void theOrderTheyFellInIsKept() {
        StoryData d = new StoryData();
        d.defeat("c");
        d.defeat("a");
        d.defeat("b");
        d.defeat("a");
        assertEquals(List.of("c", "a", "b"), d.defeatedList());
    }

    @Test
    void questStagesAreBoundedAndZeroForgets() {
        StoryData d = new StoryData();
        assertEquals(0, d.questStage("yami"));
        assertTrue(d.setQuestStage("Yami", 2));
        assertFalse(d.setQuestStage("yami", 2), "the same stage again changes nothing");
        assertEquals(2, d.questStage("YAMI"));
        d.setQuestStage("yami", 99);
        assertEquals(StoryData.QUEST_MAX, d.questStage("yami"));
        d.setQuestStage("yami", -4);
        assertEquals(0, d.questStage("yami"));
        assertTrue(d.questMap().isEmpty(), "stage 0 is not stored");
    }

    @Test
    void ofRebuildsFromStoredPartsAndToleratesNulls() {
        Map<String, Integer> quest = new HashMap<>();
        quest.put("Yami", 3);
        quest.put("bad", null);
        StoryData d = StoryData.of(List.of("A", "a", "b"), List.of("Yami"), quest, "Spade_Raid");
        assertEquals(List.of("a", "b"), d.defeatedList());
        assertEquals(List.of("yami"), d.metList());
        assertEquals(3, d.questStage("yami"));
        assertEquals("spade_raid", d.saga());
        StoryData empty = StoryData.of(null, null, null, null);
        assertTrue(empty.isEmpty());
        assertEquals("", empty.saga());
    }

    @Test
    void aCopyIsEqualAndIndependent() {
        StoryData d = new StoryData();
        d.defeat("a");
        d.meet("n");
        d.setQuestStage("n", 1);
        d.setSaga("elf_reincarnation");
        StoryData c = d.copy();
        assertEquals(d, c);
        assertEquals(d.hashCode(), c.hashCode());
        c.defeat("b");
        assertNotEquals(d, c);
        assertFalse(d.hasDefeated("b"));
        d.clear();
        assertTrue(d.isEmpty());
        assertTrue(c.hasDefeated("a"));
    }

    @Test
    void theSagaAloneMakesTheStoryNonEmpty() {
        StoryData d = new StoryData();
        d.setSaga("spade_raid");
        assertFalse(d.isEmpty(), "it would otherwise not be saved");
        d.setSaga(null);
        assertTrue(d.isEmpty());
    }
}
