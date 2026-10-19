package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 1.93.0 magic rebuild: the page-order rule (old ids keep their places, new ids follow, ids are unique), and the real page lists of the five rebuilt books are pinned in the doc tests of each magic. */
class SpatialPageOrderTest {
    @Test
    void keptOldPagesAndAppendedNewOnesAreFine() {
        List<String> old = List.of("a", "b", "c");
        assertTrue(SpatialPageOrder.problems(List.of("a", "b", "c", "n1", "n2", "x"), old, List.of("n1", "n2")).isEmpty());
    }

    @Test
    void aMovedOldPageIsReported() {
        List<String> bad = SpatialPageOrder.problems(List.of("b", "a", "c", "n1"), List.of("a", "b", "c"), List.of("n1"));
        assertEquals(2, bad.size());
    }

    @Test
    void aNewPageInsertedBeforeAnOldOneIsReported() {
        List<String> bad = SpatialPageOrder.problems(List.of("a", "n1", "b", "c"), List.of("a", "b", "c"), List.of("n1"));
        assertTrue(bad.size() >= 2);
    }

    @Test
    void duplicatesAndMissingPagesAreReported() {
        assertEquals(1, SpatialPageOrder.problems(List.of("a", "a"), List.of("a"), List.of()).size());
        assertEquals(1, SpatialPageOrder.problems(List.of("a"), List.of("a", "b"), List.of()).size());
    }
}
