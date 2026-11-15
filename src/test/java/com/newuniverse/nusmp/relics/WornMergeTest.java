package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The rule that joins the client's own look at a wearer with the list the server sends: a relic both know is drawn once, a relic only the server knows is added. */
class WornMergeTest {
    @Test
    void whatTheClientFoundIsNotAddedAgain() {
        assertEquals(List.of(), WornMerge.missing(List.of("anime_multiverse:a", "anime_multiverse:b"), List.of("anime_multiverse:a", "anime_multiverse:b")));
    }

    @Test
    void whatOnlyTheServerKnowsIsAdded() {
        assertEquals(List.of(1), WornMerge.missing(List.of("anime_multiverse:a"), List.of("anime_multiverse:a", "anime_multiverse:b")));
        assertEquals(List.of(0, 1), WornMerge.missing(List.of(), List.of("anime_multiverse:a", "anime_multiverse:b")), "a client whose Curios read failed finds nothing: everything comes from the server");
    }

    @Test
    void theSameItemTwiceIsMatchedOneForOne() {
        assertEquals(List.of(1), WornMerge.missing(List.of("anime_multiverse:ring"), List.of("anime_multiverse:ring", "anime_multiverse:ring")));
        assertEquals(List.of(), WornMerge.missing(List.of("anime_multiverse:ring", "anime_multiverse:ring"), List.of("anime_multiverse:ring", "anime_multiverse:ring")));
        assertEquals(List.of(), WornMerge.missing(List.of("anime_multiverse:ring", "anime_multiverse:ring", "anime_multiverse:ring"), List.of("anime_multiverse:ring")), "more found than told: nothing to add");
    }

    @Test
    void aMobPackTheClientCanNeverReadComesEntirelyFromTheServer() {
        assertEquals(List.of(0, 1, 2), WornMerge.missing(List.of("minecraft:iron_helmet"), Arrays.asList("anime_multiverse:ring_a", "anime_multiverse:cloak_b", "anime_multiverse:belt_c")));
    }

    @Test
    void nullKeysAreIgnored() {
        assertEquals(List.of(1), WornMerge.missing(Arrays.asList("anime_multiverse:a", null), Arrays.asList(null, "anime_multiverse:b")));
    }

    @Test
    void theSignatureChangesWhenTheListChangesAndNotOtherwise() {
        assertEquals(WornMerge.signature(List.of("a", "b")), WornMerge.signature(List.of("a", "b")));
        assertNotEquals(WornMerge.signature(List.of("a", "b")), WornMerge.signature(List.of("b", "a")));
        assertNotEquals(WornMerge.signature(List.of("a")), WornMerge.signature(List.of("a", "a")));
        assertEquals("", WornMerge.signature(List.of()), "an empty list is the state nothing needs sending for");
    }
}
