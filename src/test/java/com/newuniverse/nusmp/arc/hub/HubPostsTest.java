package com.newuniverse.nusmp.arc.hub;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: who stands on which post must never reshuffle the ones already standing and must never cram more characters than there are posts; these tests pin the
 * order (roster order), the guide's place at the lectern whatever its place in the roster, and the report of the characters that found no post.
 */
class HubPostsTest {
    @Test
    void charactersTakeThePostsInRosterOrder() {
        HubPosts.Assignment a = HubPosts.assign(List.of("a", "b", "c"), null, 10);
        assertEquals(Integer.valueOf(0), a.posts().get("a"));
        assertEquals(Integer.valueOf(1), a.posts().get("b"));
        assertEquals(Integer.valueOf(2), a.posts().get("c"));
        assertTrue(a.unplaced().isEmpty());
    }

    @Test
    void theGuideTakesTheFirstPostWhereverItIsInTheRoster() {
        HubPosts.Assignment a = HubPosts.assign(List.of("a", "b", "guide", "c"), "guide", 10);
        assertEquals(Integer.valueOf(0), a.posts().get("guide"));
        assertEquals(Integer.valueOf(1), a.posts().get("a"));
        assertEquals(Integer.valueOf(2), a.posts().get("b"));
        assertEquals(Integer.valueOf(3), a.posts().get("c"));
    }

    @Test
    void aGuideWhoIsNotInTheRosterChangesNothing() {
        HubPosts.Assignment with = HubPosts.assign(List.of("a", "b"), "ghost", 5);
        HubPosts.Assignment without = HubPosts.assign(List.of("a", "b"), null, 5);
        assertEquals(without.posts(), with.posts());
        assertEquals(without.posts(), HubPosts.assign(List.of("a", "b"), "", 5).posts());
    }

    @Test
    void whenThereAreMoreCharactersThanPostsTheLastOnesAreReportedAndNobodyIsCrammedIn() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 7; i++) ids.add("n" + i);
        HubPosts.Assignment a = HubPosts.assign(ids, "n5", 4);
        assertEquals(4, a.posts().size());
        assertEquals(Integer.valueOf(0), a.posts().get("n5"), "the guide still gets a post");
        assertEquals(List.of("n3", "n4", "n6"), a.unplaced(), "the guide and the first three in roster order stand; the rest are reported");
        assertFalse(a.posts().containsKey("n6"));
        assertEquals(a.posts().size(), new java.util.HashSet<>(a.posts().values()).size(), "no two characters share a post");
    }

    @Test
    void noPostsMeansNobodyIsPlacedNotEvenTheGuide() {
        HubPosts.Assignment a = HubPosts.assign(List.of("guide", "x"), "guide", 0);
        assertTrue(a.posts().isEmpty());
        assertEquals(List.of("guide", "x"), a.unplaced());
    }

    @Test
    void anEmptyRosterIsFine() {
        HubPosts.Assignment a = HubPosts.assign(List.of(), "guide", 10);
        assertTrue(a.posts().isEmpty());
        assertTrue(a.unplaced().isEmpty());
    }

    @Test
    void addingACharacterAtTheEndNeverMovesTheOnesAlreadyStanding() {
        List<String> ids = new ArrayList<>(List.of("a", "b", "c", "d"));
        HubPosts.Assignment before = HubPosts.assign(ids, "c", 20);
        ids.add("e");
        HubPosts.Assignment after = HubPosts.assign(ids, "c", 20);
        for (String id : List.of("a", "b", "c", "d")) assertEquals(before.posts().get(id), after.posts().get(id), id + " moved");
        assertEquals(Integer.valueOf(4), after.posts().get("e"));
    }
}
