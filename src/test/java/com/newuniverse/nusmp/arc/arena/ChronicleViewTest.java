package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.story.StoryData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the lectern and the sigil read the same table, and the table decides which boss a player may be carried to: these tests pin the states (defeated, open,
 * locked with the names still missing), the story gate switched off, the pages (one per saga that has a boss, in the story's order, none for a saga of friends)
 * and the pick rules of {@link ChroniclePick} (a stale pick falls back to the next boss of the story, replays, cycling) with fake rosters.
 */
class ChronicleViewTest {
    private static List<ArcCharacter> roster() {
        List<ArcCharacter> r = new ArrayList<>();
        r.add(ArcCharacter.boss("a_one", "A One").saga(ArcSaga.ELF_REINCARNATION).build());
        r.add(ArcCharacter.boss("a_two", "A Two").saga(ArcSaga.ELF_REINCARNATION).after("a_one").build());
        r.add(ArcCharacter.npc("a_friend", "A Friend").saga(ArcSaga.ELF_REINCARNATION).build());
        r.add(ArcCharacter.boss("b_one", "B One").saga(ArcSaga.SPADE_RAID).after("a_two", "ghost").build());
        r.add(ArcCharacter.boss("b_two", "B Two").saga(ArcSaga.SPADE_RAID).after("b_one", "a_two").build());
        r.add(ArcCharacter.npc("z_ally", "Z Ally").saga(ArcSaga.ALLIES).build());
        return r;
    }

    private static ChronicleView.Row row(List<ChronicleView.Row> rows, String id) {
        for (ChronicleView.Row r : rows) if (r.id().equals(id)) return r;
        throw new AssertionError("no row " + id);
    }

    @Test
    void aFreshStoryOpensTheFirstBossAndLocksTheRestWithTheirMissingNames() {
        List<ChronicleView.Row> rows = ChronicleView.rows(roster(), new StoryData(), true);
        assertEquals(4, rows.size(), "only bosses are listed, in roster order");
        assertEquals(ChronicleView.State.OPEN, row(rows, "a_one").state());
        assertEquals(ChronicleView.State.LOCKED, row(rows, "a_two").state());
        assertEquals(List.of("A One"), row(rows, "a_two").missing());
        assertEquals(List.of("A Two"), row(rows, "b_one").missing(), "an id that is not in the roster is never waited for");
        assertEquals(List.of("B One", "A Two"), row(rows, "b_two").missing(), "in the order the character lists them");
        assertTrue(row(rows, "a_one").missing().isEmpty());
    }

    @Test
    void aDefeatedBossIsDefeatedAndOpensTheNext() {
        StoryData d = new StoryData();
        d.defeat("a_one");
        List<ChronicleView.Row> rows = ChronicleView.rows(roster(), d, true);
        assertEquals(ChronicleView.State.DEFEATED, row(rows, "a_one").state());
        assertEquals(ChronicleView.State.OPEN, row(rows, "a_two").state());
        assertEquals(ChronicleView.State.LOCKED, row(rows, "b_one").state());
    }

    @Test
    void withTheGateOffEverythingNotDefeatedIsOpen() {
        StoryData d = new StoryData();
        d.defeat("b_one");
        List<ChronicleView.Row> rows = ChronicleView.rows(roster(), d, false);
        for (ChronicleView.Row r : rows) {
            if (r.id().equals("b_one")) assertEquals(ChronicleView.State.DEFEATED, r.state());
            else assertEquals(ChronicleView.State.OPEN, r.state(), r.id());
            assertTrue(r.missing().isEmpty());
        }
    }

    @Test
    void pagesAreOnePerSagaWithBossesInTheStorysOrder() {
        StoryData d = new StoryData();
        d.defeat("a_one");
        d.defeat("a_two");
        List<ChronicleView.Page> pages = ChronicleView.pages(ChronicleView.rows(roster(), d, true));
        assertEquals(2, pages.size(), "the saga of friends has no page");
        assertEquals(ArcSaga.ELF_REINCARNATION, pages.get(0).saga());
        assertEquals(ArcSaga.SPADE_RAID, pages.get(1).saga());
        assertTrue(pages.get(0).cleared());
        assertEquals(2, pages.get(0).defeated());
        assertFalse(pages.get(1).cleared());
        assertEquals(1, ChronicleView.pageIndex(pages, ArcSaga.SPADE_RAID));
        assertEquals(-1, ChronicleView.pageIndex(pages, ArcSaga.DANMACHI));
        assertEquals(1, ChronicleView.firstPage(pages, ArcSaga.SPADE_RAID));
        assertEquals(0, ChronicleView.firstPage(pages, ArcSaga.DANMACHI), "a saga without a page opens the first one");
        assertEquals(1, ChronicleView.nextPage(pages, 0));
        assertEquals(0, ChronicleView.nextPage(pages, 1), "wraps round");
        assertEquals(0, ChronicleView.nextPage(List.of(), 5));
    }

    @Test
    void aPickThatCanStillBeFoughtIsKeptAndAStaleOneFallsBackToTheNextBossOfTheStory() {
        StoryData d = new StoryData();
        d.defeat("a_one");
        List<ChronicleView.Row> rows = ChronicleView.rows(roster(), d, true);
        assertEquals("a_two", ChroniclePick.choose(rows, "a_two", false), "an open pick is kept");
        assertEquals("a_two", ChroniclePick.choose(rows, null, false), "no pick: the next boss");
        assertEquals("a_two", ChroniclePick.choose(rows, "", false));
        assertEquals("a_two", ChroniclePick.choose(rows, "b_two", false), "a locked pick is dropped");
        assertEquals("a_two", ChroniclePick.choose(rows, "gone", false), "a removed boss is dropped");
        assertEquals("a_two", ChroniclePick.choose(rows, "a_one", false), "a defeated one is dropped when replays are off");
        assertEquals("a_one", ChroniclePick.choose(rows, "a_one", true), "and kept when they are on");
    }

    @Test
    void whenNothingIsOpenReplaysOfferTheFirstDefeatedOneAndOtherwiseNothing() {
        StoryData d = new StoryData();
        for (ArcCharacter c : roster()) if (c.isBoss()) d.defeat(c.id());
        List<ChronicleView.Row> rows = ChronicleView.rows(roster(), d, true);
        assertNull(ChroniclePick.choose(rows, null, false));
        assertNull(ChroniclePick.choose(rows, "a_one", false));
        assertEquals("a_one", ChroniclePick.choose(rows, null, true));
        assertNull(ChroniclePick.choose(List.of(), null, true), "an empty roster has nobody");
    }

    @Test
    void cyclingWalksTheBossesThatCanBeFoughtAndWrapsRound() {
        StoryData d = new StoryData();
        d.defeat("a_one");
        d.defeat("a_two");
        List<ChronicleView.Row> rows = ChronicleView.rows(roster(), d, true);
        // a_one, a_two defeated; b_one open; b_two locked
        assertEquals("b_one", ChroniclePick.cycle(rows, "a_one", false));
        assertEquals("b_one", ChroniclePick.cycle(rows, "b_one", false), "the only boss to fight: the cycle comes back to it");
        assertEquals("a_one", ChroniclePick.cycle(rows, "b_one", true), "with replays the walk wraps to the first");
        assertEquals("a_two", ChroniclePick.cycle(rows, "a_one", true));
        assertEquals("b_one", ChroniclePick.cycle(rows, null, false), "no current: the first that can be fought");
        assertEquals("b_one", ChroniclePick.cycle(rows, "nonsense", false));
        assertNull(ChroniclePick.cycle(List.of(), "x", true));
        List<ChronicleView.Row> locked = ChronicleView.rows(roster(), new StoryData(), true);
        assertEquals("a_one", ChroniclePick.cycle(locked, "b_two", false), "only the first is open");
    }

    @Test
    void pickableMatchesSelectable() {
        StoryData d = new StoryData();
        d.defeat("a_one");
        List<ChronicleView.Row> rows = ChronicleView.rows(roster(), d, true);
        assertTrue(ChroniclePick.pickable(rows, "a_two", false));
        assertFalse(ChroniclePick.pickable(rows, "b_two", false));
        assertFalse(ChroniclePick.pickable(rows, "a_one", false));
        assertTrue(ChroniclePick.pickable(rows, "a_one", true));
        assertFalse(ChroniclePick.pickable(rows, "a_friend", true), "a friend is not on the list");
        assertFalse(ChroniclePick.pickable(rows, "nobody", true));
    }
}
