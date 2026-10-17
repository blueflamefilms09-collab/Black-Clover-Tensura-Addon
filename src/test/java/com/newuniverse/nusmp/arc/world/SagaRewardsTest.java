package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.story.StoryData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a saga's reward is owed exactly once, to the player whose story closes the saga, wherever the last boss fell; these tests pin the moment it is owed (every
 * boss defeated, nobody else's, NPCs never count), the once-only mark, the table (only vanilla items and the dungeon's own stone, growing with the saga) and the
 * pages of the commemorative book.
 */
class SagaRewardsTest {
    private static List<ArcCharacter> roster() {
        List<ArcCharacter> r = new ArrayList<>();
        r.add(ArcCharacter.boss("e_one", "Eone").saga(ArcSaga.ELF_REINCARNATION).build());
        r.add(ArcCharacter.boss("e_two", "Etwo").saga(ArcSaga.ELF_REINCARNATION).build());
        r.add(ArcCharacter.boss("s_one", "Sone").saga(ArcSaga.SPADE_RAID).build());
        r.add(ArcCharacter.npc("a_friend", "Friend").saga(ArcSaga.ELF_REINCARNATION).build());
        r.add(ArcCharacter.npc("an_ally", "Ally").saga(ArcSaga.ALLIES).build());
        return r;
    }

    @Test
    void aSagaIsClearedWhenEveryBossOfItIsDefeatedAndNpcsNeverCount() {
        StoryData d = new StoryData();
        assertFalse(SagaRewards.cleared(d, ArcSaga.ELF_REINCARNATION, roster()));
        d.defeat("e_one");
        assertFalse(SagaRewards.cleared(d, ArcSaga.ELF_REINCARNATION, roster()));
        d.meet("a_friend");
        assertFalse(SagaRewards.cleared(d, ArcSaga.ELF_REINCARNATION, roster()), "meeting a friend clears nothing");
        d.defeat("e_two");
        assertTrue(SagaRewards.cleared(d, ArcSaga.ELF_REINCARNATION, roster()));
        assertFalse(SagaRewards.cleared(d, ArcSaga.SPADE_RAID, roster()), "another saga is not cleared by it");
        assertFalse(SagaRewards.cleared(d, ArcSaga.ALLIES, roster()), "a saga with no boss is never cleared");
        assertFalse(SagaRewards.cleared(d, ArcSaga.DANMACHI, roster()), "a saga with nobody is not cleared");
    }

    @Test
    void theRewardIsOwedOnceAndTheMarkTravelsWithTheStory() {
        StoryData d = new StoryData();
        d.defeat("e_one");
        d.defeat("e_two");
        List<ArcSaga> owed = SagaRewards.owed(d, roster());
        assertEquals(List.of(ArcSaga.ELF_REINCARNATION), owed);
        assertTrue(SagaRewards.markPaid(d, ArcSaga.ELF_REINCARNATION), "the first mark changes the story");
        assertFalse(SagaRewards.markPaid(d, ArcSaga.ELF_REINCARNATION), "the second does not");
        assertTrue(SagaRewards.paid(d, ArcSaga.ELF_REINCARNATION));
        assertTrue(SagaRewards.owed(d, roster()).isEmpty());
        d.defeat("s_one");
        assertEquals(List.of(ArcSaga.SPADE_RAID), SagaRewards.owed(d, roster()));
        StoryData copy = d.copy();
        assertTrue(SagaRewards.paid(copy, ArcSaga.ELF_REINCARNATION), "the mark survives a copy (death)");
        assertEquals("saga:spade_raid", SagaRewards.paidKey(ArcSaga.SPADE_RAID));
        d.clear();
        assertFalse(SagaRewards.paid(d, ArcSaga.ELF_REINCARNATION), "a reset story forgets it was paid");
    }

    @Test
    void theTableHoldsOnlyVanillaItemsAndTheStoneAndGrowsWithTheStory() {
        int last = 0;
        for (ArcSaga s : ArcSaga.values()) {
            List<SagaRewards.Stack> r = SagaRewards.reward(s);
            if (s == ArcSaga.ALLIES) {
                assertTrue(r.isEmpty());
                continue;
            }
            assertFalse(r.isEmpty(), s.id());
            int stones = 0;
            for (SagaRewards.Stack st : r) {
                assertTrue(st.item().startsWith("minecraft:") || st.item().equals("nusmp:magic_stone"), s.id() + ": " + st.item());
                assertTrue(st.count() > 0 && st.count() <= 64, s.id() + ": " + st);
                if (st.item().equals("nusmp:magic_stone")) stones = st.count();
            }
            assertTrue(stones > 0 && stones <= 32, s.id());
            if (s == ArcSaga.WIZARD_KING) assertTrue(stones >= last, "the Wizard King's saga pays the most");
            last = Math.max(last, stones);
        }
        assertEquals(32, SagaRewards.reward(ArcSaga.WIZARD_KING).get(0).count());
    }

    @Test
    void theBookNamesTheSagaAndPagesALongList() {
        assertEquals("Chronicle: Spade Kingdom Raid Saga", SagaRewards.bookTitle(ArcSaga.SPADE_RAID));
        List<String> pages = SagaRewards.bookPages(ArcSaga.SPADE_RAID, List.of("A", "B", "C"));
        assertEquals(3, pages.size());
        assertTrue(pages.get(0).contains("Spade Kingdom Raid Saga"));
        assertTrue(pages.get(1).contains("A") && pages.get(1).contains("C"));
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 15; i++) names.add("Boss " + i);
        List<String> long15 = SagaRewards.bookPages(ArcSaga.SEVEN_DEADLY_SINS, names);
        assertEquals(1 + 3 + 1, long15.size(), "seven names to a page");
        for (String p : long15) assertTrue(p.length() < 256, "a page of a book holds 256 characters: " + p.length());
        assertEquals(2, SagaRewards.bookPages(ArcSaga.DANMACHI, null).size() - 0, "no names: the title page and the last page");
    }
}
