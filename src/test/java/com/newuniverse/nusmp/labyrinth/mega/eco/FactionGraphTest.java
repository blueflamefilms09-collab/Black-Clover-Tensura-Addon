package com.newuniverse.nusmp.labyrinth.mega.eco;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the graph decides every quarrel and every home, so its promises are pinned here: rivalry mirrors unless the faction says one-way, alliance is
 * symmetric and beats a rivalry, a faction never fights itself or an unknown id, and the seeded native-faction draw is deterministic, follows the
 * theme weights, never picks a weight-0 faction, and does NOT collapse to one answer for neighbouring room ids (the failure a naive
 * {@code new Random(seed ^ id)} would have).
 */
class FactionGraphTest {
    private static FactionGraph graph(FactionDef... defs) { return FactionGraph.of(List.of(defs)); }

    @Test void rivalryIsMirroredByDefault() {
        FactionGraph g = graph(Fx.def("goblin", List.of("ogre"), List.of()), Fx.def("ogre", List.of(), List.of()));
        assertTrue(g.hostile("goblin", "ogre"));
        assertTrue(g.hostile("ogre", "goblin"), "the ogre file lists nobody, but the goblin file lists the ogres");
        assertTrue(g.rivalsOf("ogre").contains("goblin"));
        assertTrue(g.problems().isEmpty(), g.problems().toString());
    }

    @Test void oneWayRivalryIsNotMirrored() {
        FactionGraph g = graph(Fx.oneWay("beast", List.of("slime")), Fx.def("slime", List.of(), List.of()));
        assertTrue(g.hostile("beast", "slime"));
        assertFalse(g.hostile("slime", "beast"));
    }

    @Test void aFactionNeverFightsItselfOrAStranger() {
        FactionGraph g = graph(Fx.def("goblin", List.of("goblin", "ogre"), List.of()), Fx.def("ogre", List.of(), List.of()));
        assertFalse(g.hostile("goblin", "goblin"));
        assertFalse(g.hostile("goblin", "nobody"));
        assertFalse(g.hostile(null, "ogre"));
        assertFalse(g.hostile("goblin", null));
        assertTrue(g.sameOrAllied("goblin", "goblin"));
        assertFalse(g.sameOrAllied("goblin", "ogre"));
        assertFalse(g.sameOrAllied(null, null));
    }

    @Test void alliesAreSymmetricAndNeverRivals() {
        FactionGraph g = graph(Fx.def("cultist", List.of(), List.of("demon")), Fx.def("demon", List.of(), List.of()));
        assertTrue(g.allied("cultist", "demon"));
        assertTrue(g.allied("demon", "cultist"), "an alliance listed on one side holds on both");
        assertTrue(g.sameOrAllied("demon", "cultist"));
        assertFalse(g.allied("cultist", "cultist"));
        assertFalse(g.hostile("cultist", "demon"));
    }

    @Test void anAllianceBeatsARivalityAndIsReported() {
        FactionGraph g = graph(Fx.def("a", List.of("b"), List.of()), Fx.def("b", List.of("a"), List.of("a")));
        assertTrue(g.allied("a", "b"));
        assertFalse(g.hostile("a", "b"));
        assertFalse(g.hostile("b", "a"));
        assertFalse(g.problems().isEmpty());
    }

    @Test void unknownReferencesAreDroppedAndReported() {
        FactionGraph g = graph(Fx.def("goblin", List.of("dragon"), List.of("fairy")));
        assertTrue(g.rivalsOf("goblin").isEmpty());
        assertTrue(g.alliesOf("goblin").isEmpty());
        assertEquals(2, g.problems().size());
        assertFalse(g.knows("dragon"));
        assertTrue(g.knows("goblin"));
        assertNull(g.def("dragon"));
        assertNull(g.def(null));
    }

    @Test void idsAreSortedWhateverTheLoadOrder() {
        FactionGraph g = graph(Fx.def("undead", List.of(), List.of()), Fx.def("beast", List.of(), List.of()), Fx.def("goblin", List.of(), List.of()));
        assertEquals(List.of("beast", "goblin", "undead"), g.ids());
        assertEquals(3, g.size());
    }

    @Test void theEmptyGraphAnswersNoAndPicksNothing() {
        assertEquals(0, FactionGraph.EMPTY.size());
        assertFalse(FactionGraph.EMPTY.hostile("a", "b"));
        assertNull(FactionGraph.EMPTY.pickNative(42L, "gravity"));
        assertEquals(0.0, FactionGraph.EMPTY.themeWeight("a", "gravity"));
    }

    @Test void theDrawIsDeterministic() {
        FactionGraph g = graph(Fx.def("a", List.of(), List.of()), Fx.def("b", List.of(), List.of()), Fx.def("c", List.of(), List.of()));
        for (long seed = -50; seed < 50; seed++) assertEquals(g.pickNative(seed, "gravity"), g.pickNative(seed, "gravity"));
    }

    @Test void theDrawFollowsTheThemeWeights() {
        FactionGraph g = graph(Fx.weighted("a", "blood", 3), Fx.weighted("b", "blood", 1), Fx.weighted("z", "blood", 0));
        Map<String, Integer> count = new HashMap<>();
        int n = 40000;
        for (int i = 0; i < n; i++) count.merge(g.pickNative(0xABCDEF123L * i + i, "blood"), 1, Integer::sum);
        assertNull(count.get("z"), "a weight of 0 must never win");
        assertEquals(0.75, count.get("a") / (double) n, 0.02);
        assertEquals(0.25, count.get("b") / (double) n, 0.02);
    }

    @Test void aThemeTheFileDoesNotMentionCountsOne() {
        FactionGraph g = graph(Fx.weighted("a", "blood", 5), Fx.def("b", List.of(), List.of()));
        assertEquals(1.0, g.themeWeight("b", "frost"));
        assertEquals(1.0, g.themeWeight("a", "frost"));
        assertEquals(5.0, g.themeWeight("a", "blood"));
        assertEquals(0.0, g.themeWeight("nobody", "frost"));
        assertEquals(1.0, g.themeWeight("b", null));
    }

    @Test void neighbouringRoomIdsDoNotCollapseToOneFaction() {
        FactionGraph g = graph(Fx.def("a", List.of(), List.of()), Fx.def("b", List.of(), List.of()), Fx.def("c", List.of(), List.of()), Fx.def("d", List.of(), List.of()));
        int sectors = 200, rooms = 60, sameAsNext = 0, pairs = 0;
        for (int s = 0; s < sectors; s++) {
            long sectorSeed = 0x5DEECE66DL * (s + 1) + 11L * s;
            Set<String> seen = new HashSet<>();
            String previous = null;
            for (int room = 0; room < rooms; room++) {
                String f = g.pickNative(sectorSeed ^ room, "gravity");
                seen.add(f);
                if (previous != null) { pairs++; if (previous.equals(f)) sameAsNext++; }
                previous = f;
            }
            assertEquals(4, seen.size(), "sector " + s + " used only " + seen);
        }
        assertEquals(0.25, sameAsNext / (double) pairs, 0.03, "four equal factions: a neighbour repeats about one time in four");
    }

    @Test void everyZeroWeightFallsBackToAUniformDraw() {
        FactionGraph g = graph(Fx.weighted("a", "blood", 0), Fx.weighted("b", "blood", 0));
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 400; i++) seen.add(g.pickNative(i * 7919L, "blood"));
        assertEquals(Set.of("a", "b"), seen);
    }

    @Test void aSingleFactionIsAlwaysPicked() {
        FactionGraph g = graph(Fx.def("only", List.of(), List.of()));
        for (int i = 0; i < 50; i++) assertEquals("only", g.pickNative(i, "storm"));
    }
}
