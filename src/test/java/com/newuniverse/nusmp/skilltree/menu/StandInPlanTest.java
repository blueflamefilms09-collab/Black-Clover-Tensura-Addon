package com.newuniverse.nusmp.skilltree.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the stand-in a player holds must equal the lines the player owns, after any sequence of events. The plan is one comparison of two sets, so this tests the comparison on every shape that can occur:
 * nothing to do, a new line, a taken-away line, a stand-in that arrived without ownership (fog of war), an owned id that has no stand-in (a classic route added by a datapack), and the full scale of 283.
 */
class StandInPlanTest {
    private static final Set<String> ALL = Set.of("nusmp:a", "nusmp:b", "nusmp:c");

    @Test
    void whenTheSetsAgreeNothingIsDone() {
        assertTrue(StandInPlan.diff(List.of("nusmp:a"), List.of("nusmp:a"), ALL).isEmpty());
        assertTrue(StandInPlan.diff(List.of(), List.of(), ALL).isEmpty());
    }

    @Test
    void anOwnedLineIsTaughtAndALostLineIsForgotten() {
        StandInPlan.Plan p = StandInPlan.diff(List.of("nusmp:a"), List.of("nusmp:b"), ALL);
        assertEquals(List.of("nusmp:b"), p.learn());
        assertEquals(List.of("nusmp:a"), p.forget());
    }

    @Test
    void aStandInHeldWithoutOwnershipIsAlwaysForgotten() {
        StandInPlan.Plan p = StandInPlan.diff(List.of("nusmp:c"), List.of(), ALL);
        assertEquals(List.of("nusmp:c"), p.forget(), "the fog of war holds even for a skill that came by another route");
        assertTrue(p.learn().isEmpty());
    }

    @Test
    void anOwnedIdWithoutAStandInIsIgnored() {
        StandInPlan.Plan p = StandInPlan.diff(List.of(), List.of("nusmp:unknown_classic"), ALL);
        assertTrue(p.isEmpty());
    }

    @Test
    void theResultIsSortedAndFreeOfDuplicates() {
        StandInPlan.Plan p = StandInPlan.diff(List.of("nusmp:c", "nusmp:c"), List.of("nusmp:b", "nusmp:a", "nusmp:a"), ALL);
        assertEquals(List.of("nusmp:a", "nusmp:b"), p.learn());
        assertEquals(List.of("nusmp:c"), p.forget());
    }

    @Test
    void itScalesToEveryLine() {
        List<String> all = new ArrayList<>();
        for (int i = 0; i < 283; i++) all.add("nusmp:line_" + i);
        Set<String> known = Set.copyOf(all);
        StandInPlan.Plan p = StandInPlan.diff(List.of(), all, known);
        assertEquals(283, p.learn().size());
        StandInPlan.Plan back = StandInPlan.diff(all, List.of(), known);
        assertEquals(283, back.forget().size());
        assertTrue(StandInPlan.diff(all, all, known).isEmpty());
    }

    @Test
    void theSignatureIgnoresOrderAndNoticesChange() {
        List<String> a = List.of("nusmp:a", "nusmp:b", "nusmp:c"), b = new ArrayList<>(a);
        Collections.reverse(b);
        assertEquals(StandInPlan.signature(a), StandInPlan.signature(b));
        assertNotEquals(StandInPlan.signature(a), StandInPlan.signature(List.of("nusmp:a", "nusmp:b")));
        assertNotEquals(StandInPlan.signature(a), StandInPlan.signature(List.of("nusmp:a", "nusmp:b", "nusmp:d")));
    }
}
