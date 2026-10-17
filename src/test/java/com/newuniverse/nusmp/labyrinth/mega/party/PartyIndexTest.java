package com.newuniverse.nusmp.labyrinth.mega.party;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the party index decides whose floors a player walks into, so the book that hands the indices out is pinned rule by rule: the first party is index 0 (the shared /
 * legacy lane, which an upgraded world's floors already use), indices are handed out lowest first and never twice, a known party keeps its index whatever happens, the
 * book is never full by accident, lanes that floors already use are never given to a stranger, a new team may take over a member's lane exactly once and only when
 * that lane is the member's alone, and an admin's merge is possible and is never undone by anything else.
 */
public class PartyIndexTest {
    private static UUID id(int n) { return new UUID(0xABCDL, n); }

    private static PartyKey solo(int n) { return PartyKey.solo(id(n)); }

    private static PartyKey team(int n) { return PartyKey.team(id(1000 + n)); }

    private static final Set<Integer> NONE = Set.of();

    @Test
    public void theFirstPartyGetsTheSharedLaneAndTheNextOnesFollowInOrder() {
        PartyIndex book = new PartyIndex();
        assertEquals(0, book.allocate(solo(1), "a", 64, NONE, null).index());
        assertEquals(1, book.allocate(solo(2), "b", 64, NONE, null).index());
        assertEquals(2, book.allocate(team(1), "t", 64, NONE, List.of()).index());
        assertEquals(3, book.size());
    }

    @Test
    public void aKnownPartyKeepsItsIndexAndIsNotCountedTwice() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(1), "a", 64, NONE, null);
        book.allocate(solo(2), "b", 64, NONE, null);
        PartyIndex.Allocation again = book.allocate(solo(2), "other name", 64, NONE, null);
        assertEquals(1, again.index());
        assertFalse(again.created());
        assertEquals(2, book.size());
        assertEquals("b", book.get(id(2)).label(), "asking again does not rename it");
    }

    @Test
    public void theBookIsFullWhenMaxPartiesIsReachedAndSaysSoInsteadOfSharing() {
        PartyIndex book = new PartyIndex();
        for (int i = 0; i < 3; i++) assertEquals(i, book.allocate(solo(i), "", 3, NONE, null).index());
        PartyIndex.Allocation full = book.allocate(solo(99), "", 3, NONE, null);
        assertEquals(-1, full.index());
        assertFalse(full.ok());
        assertEquals(3, book.size(), "a refused party is not entered");
        assertEquals(-1, book.indexOf(id(99)));
    }

    @Test
    public void aLaneThatFloorsAlreadyUseIsNeverHandedToAStranger() {
        PartyIndex book = new PartyIndex();
        Set<Integer> reserved = Set.of(0, 1, 3);
        assertEquals(2, book.allocate(solo(1), "", 64, reserved, null).index());
        assertEquals(4, book.allocate(solo(2), "", 64, reserved, null).index());
        assertEquals(-1, new PartyIndex().lowestFree(3, Set.of(0, 1, 2)), "every index below the limit is in use");
    }

    @Test
    public void aFreedIndexIsTheFirstToBeHandedOutAgain() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(1), "", 64, NONE, null);
        book.allocate(solo(2), "", 64, NONE, null);
        book.allocate(solo(3), "", 64, NONE, null);
        assertTrue(book.forget(id(2)));
        assertFalse(book.forget(id(2)));
        assertEquals(1, book.allocate(solo(4), "", 64, NONE, null).index());
    }

    @Test
    public void aNewTeamTakesOverTheLaneOfAMemberWhoPlayedAlone() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(1), "", 64, NONE, null);           // the owner explored alone: lane 0
        book.allocate(solo(2), "", 64, NONE, null);           // a friend did too: lane 1
        PartyIndex.Allocation a = book.allocate(team(1), "Delvers", 64, NONE, List.of(id(1), id(2)));
        assertEquals(0, a.index(), "the owner comes first");
        assertTrue(a.inherited());
        assertEquals(id(1), a.inheritedFrom());
        assertTrue(a.created());
        assertEquals(-1, book.indexOf(id(1)), "the owner's own entry is gone: leaving the team must not lead back into the team's floors");
        assertEquals(1, book.indexOf(id(2)), "the friend's lane is untouched");
        assertEquals(0, book.indexOf(team(1).id()));
        assertTrue(book.get(team(1).id()).team());
        assertEquals(2, book.allocate(solo(1), "", 64, NONE, null).index(), "the owner, back alone, starts a fresh lane");
    }

    @Test
    public void aTeamTakesTheFirstMemberThatHasALaneWhenTheOwnerHasNone() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(2), "", 64, NONE, null);
        book.allocate(solo(3), "", 64, NONE, null);
        PartyIndex.Allocation a = book.allocate(team(1), "", 64, NONE, List.of(id(1), id(2), id(3)));
        assertEquals(0, a.index());
        assertEquals(id(2), a.inheritedFrom());
    }

    @Test
    public void aTeamDoesNotMoveIntoALaneThatIsNotTheMembersAlone() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(1), "", 64, NONE, null);
        book.assign(solo(2), "", 0);                           // an admin merged player 2 into lane 0
        assertEquals(2, book.holders(0).size());
        PartyIndex.Allocation a = book.allocate(team(1), "", 64, NONE, List.of(id(1), id(2)));
        assertEquals(1, a.index(), "a shared lane is a merge of the admin's; a new team gets a lane of its own");
        assertFalse(a.inherited());
        assertEquals(0, book.indexOf(id(1)));
    }

    @Test
    public void aSoloPartyAndAKnownTeamNeverInherit() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(1), "", 64, NONE, null);
        assertEquals(1, book.allocate(solo(2), "", 64, NONE, List.of(id(1))).index(), "a player alone takes no lane from anyone");
        book.allocate(team(1), "", 64, NONE, List.of());
        int t = book.indexOf(team(1).id());
        assertEquals(t, book.allocate(team(1), "", 64, NONE, List.of(id(1))).index(), "a known team keeps its lane");
        assertEquals(0, book.indexOf(id(1)));
    }

    @Test
    public void aTeamNeverTakesOverATeamsLane() {
        PartyIndex book = new PartyIndex();
        book.allocate(team(1), "", 64, NONE, null);
        // the candidate list names a TEAM key by mistake: it must not be taken over (only a player's own entry is)
        PartyIndex.Allocation a = book.allocate(team(2), "", 64, NONE, List.of(team(1).id()));
        assertFalse(a.inherited());
        assertEquals(1, a.index());
    }

    @Test
    public void anAdminCanMovePartiesAndMergeThemAndTheOldIndexIsReported() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(1), "a", 64, NONE, null);
        assertEquals(0, book.assign(solo(1), "", 5), "the previous index is reported");
        assertEquals(5, book.indexOf(id(1)));
        assertEquals("a", book.get(id(1)).label(), "moving keeps the name");
        assertEquals(-1, book.assign(solo(2), "b", 5), "a new party reports -1");
        assertEquals(2, book.holders(5).size());
        assertTrue(book.holds(5));
        assertFalse(book.holds(0));
        assertEquals(0, book.allocate(solo(3), "", 64, NONE, null).index(), "the emptied index 0 is free again");
    }

    @Test
    public void anIndexOutsideTheLaneMathsIsRefusedByAssign() {
        PartyIndex book = new PartyIndex();
        boolean threw = false;
        try {
            book.assign(solo(1), "", MegaGrid.MAX_PARTIES);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue(threw);
        threw = false;
        try {
            book.assign(solo(1), "", -1);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue(threw);
        assertEquals(0, book.size());
    }

    @Test
    public void entriesComeBackLowestIndexFirstAndRestoreRebuildsTheBook() {
        PartyIndex book = new PartyIndex();
        book.allocate(solo(1), "a", 64, NONE, null);
        book.allocate(team(1), "t", 64, NONE, null);
        book.allocate(solo(3), "c", 64, NONE, null);
        book.assign(solo(3), "", 0);
        List<PartyIndex.Entry> list = book.entries();
        assertEquals(3, list.size());
        assertEquals(0, list.get(0).index());
        assertEquals(0, list.get(1).index());
        assertEquals(1, list.get(2).index());
        PartyIndex copy = new PartyIndex();
        for (PartyIndex.Entry e : list) copy.restore(e);
        assertEquals(list, copy.entries());
        copy.restore(new PartyIndex.Entry(id(50), false, MegaGrid.MAX_PARTIES + 3, "bad"));
        copy.restore(new PartyIndex.Entry(id(51), false, -2, "bad"));
        copy.restore(new PartyIndex.Entry(id(1), false, 9, "duplicate"));
        copy.restore(null);
        assertEquals(3, copy.size(), "impossible and duplicate entries are ignored, the rest is kept");
        assertEquals(0, copy.indexOf(id(1)));
    }

    @Test
    public void relabelOnlyChangesTheNameOfAKnownParty() {
        PartyIndex book = new PartyIndex();
        book.allocate(team(1), "Old", 64, NONE, null);
        assertTrue(book.relabel(team(1).id(), "New"));
        assertFalse(book.relabel(team(1).id(), "New"));
        assertFalse(book.relabel(team(1).id(), ""));
        assertFalse(book.relabel(id(99), "x"));
        assertEquals("New", book.get(team(1).id()).label());
        assertNull(book.get(null));
        assertNotNull(book.get(team(1).id()));
    }

    @Test
    public void theLimitNeverExceedsTheLaneMaths() {
        PartyIndex book = new PartyIndex();
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < MegaGrid.MAX_PARTIES + 10; i++) {
            PartyIndex.Allocation a = book.allocate(solo(i), "", 10_000, NONE, null);
            if (i < MegaGrid.MAX_PARTIES) {
                assertTrue(a.ok());
                assertTrue(used.add(a.index()), "no index twice");
            } else {
                assertEquals(-1, a.index(), "party " + i + " does not fit the lane maths");
            }
        }
        assertEquals(MegaGrid.MAX_PARTIES, book.size());
    }
}
