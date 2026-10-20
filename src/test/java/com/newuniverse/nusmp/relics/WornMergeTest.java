package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The rule that joins the client's own look at a wearer with the list the server sends: a relic both know is drawn once, a relic only the server knows is added. */
class WornMergeTest {
    @Test
    void whatTheClientFoundIsNotAddedAgain() {
        assertEquals(List.of(), WornMerge.missing(List.of("nusmp:a", "nusmp:b"), List.of("nusmp:a", "nusmp:b")));
    }

    @Test
    void whatOnlyTheServerKnowsIsAdded() {
        assertEquals(List.of(1), WornMerge.missing(List.of("nusmp:a"), List.of("nusmp:a", "nusmp:b")));
        assertEquals(List.of(0, 1), WornMerge.missing(List.of(), List.of("nusmp:a", "nusmp:b")), "a client whose Curios read failed finds nothing: everything comes from the server");
    }

    @Test
    void theSameItemTwiceIsMatchedOneForOne() {
        assertEquals(List.of(1), WornMerge.missing(List.of("nusmp:ring"), List.of("nusmp:ring", "nusmp:ring")));
        assertEquals(List.of(), WornMerge.missing(List.of("nusmp:ring", "nusmp:ring"), List.of("nusmp:ring", "nusmp:ring")));
        assertEquals(List.of(), WornMerge.missing(List.of("nusmp:ring", "nusmp:ring", "nusmp:ring"), List.of("nusmp:ring")), "more found than told: nothing to add");
    }

    @Test
    void aMobPackTheClientCanNeverReadComesEntirelyFromTheServer() {
        assertEquals(List.of(0, 1, 2), WornMerge.missing(List.of("minecraft:iron_helmet"), Arrays.asList("nusmp:ring_a", "nusmp:cloak_b", "nusmp:belt_c")));
    }

    @Test
    void nullKeysAreIgnored() {
        assertEquals(List.of(1), WornMerge.missing(Arrays.asList("nusmp:a", null), Arrays.asList(null, "nusmp:b")));
    }

    @Test
    void theSignatureChangesWhenTheListChangesAndNotOtherwise() {
        assertEquals(WornMerge.signature(List.of("a", "b")), WornMerge.signature(List.of("a", "b")));
        assertNotEquals(WornMerge.signature(List.of("a", "b")), WornMerge.signature(List.of("b", "a")));
        assertNotEquals(WornMerge.signature(List.of("a")), WornMerge.signature(List.of("a", "a")));
        assertEquals("", WornMerge.signature(List.of()), "an empty list is the state nothing needs sending for");
    }
}
