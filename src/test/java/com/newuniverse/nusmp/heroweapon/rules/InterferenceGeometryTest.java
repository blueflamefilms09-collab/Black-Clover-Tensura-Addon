package com.newuniverse.nusmp.heroweapon.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.rules.InterferenceGeometry.Wielder;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: "two heroes too close gain no experience" is geometry with sharp edges: exactly at the radius, another dimension, three heroes in a row, a hero alone,
 * the same hero listed twice. Each edge is a case here.
 */
class InterferenceGeometryTest {
    private static Wielder w(String id, String dim, double x, double y, double z) { return new Wielder(id, dim, x, y, z); }

    @Test
    void twoHeroesInRangeBlockEachOtherAndOneAloneIsFree() {
        Set<String> b = InterferenceGeometry.blocked(List.of(w("a", "overworld", 0, 64, 0), w("b", "overworld", 30, 64, 0)), 48);
        assertEquals(Set.of("a", "b"), b);
        assertTrue(InterferenceGeometry.blocked(List.of(w("a", "overworld", 0, 64, 0)), 48).isEmpty(), "a hero alone");
        assertTrue(InterferenceGeometry.blocked(List.of(), 48).isEmpty());
    }

    @Test
    void theRadiusIsInclusiveAndFarHeroesAreFree() {
        assertEquals(Set.of("a", "b"), InterferenceGeometry.blocked(List.of(w("a", "d", 0, 0, 0), w("b", "d", 48, 0, 0)), 48), "exactly at the radius");
        assertTrue(InterferenceGeometry.blocked(List.of(w("a", "d", 0, 0, 0), w("b", "d", 48.01, 0, 0)), 48).isEmpty(), "just outside");
        assertEquals(Set.of("a", "b"), InterferenceGeometry.blocked(List.of(w("a", "d", 0, 0, 0), w("b", "d", 27, 27, 27)), 48), "the distance is in three dimensions: 27,27,27 is 46.8 away");
        assertTrue(InterferenceGeometry.blocked(List.of(w("a", "d", 0, 0, 0), w("b", "d", 28, 28, 28)), 48).isEmpty(), "28,28,28 is 48.5 away: out");
    }

    @Test
    void anotherDimensionNeverBlocks() {
        assertTrue(InterferenceGeometry.blocked(List.of(w("a", "overworld", 0, 0, 0), w("b", "the_nether", 1, 0, 0)), 48).isEmpty());
    }

    @Test
    void aChainBlocksEveryoneWhoHasANeighbourAndNobodyElse() {
        // a - b in range, b - c in range, a - c out of range, d far away
        Set<String> b = InterferenceGeometry.blocked(List.of(w("a", "d", 0, 0, 0), w("b", "d", 40, 0, 0), w("c", "d", 80, 0, 0), w("d", "d", 500, 0, 0)), 48);
        assertEquals(Set.of("a", "b", "c"), b);
    }

    @Test
    void theSameHeroListedTwiceDoesNotBlockHimself() {
        assertTrue(InterferenceGeometry.blocked(List.of(w("a", "d", 0, 0, 0), w("a", "d", 1, 0, 0)), 48).isEmpty());
    }

    @Test
    void aZeroOrNegativeRadiusSwitchesTheRuleOff() {
        List<Wielder> two = List.of(w("a", "d", 0, 0, 0), w("b", "d", 0, 0, 0));
        assertTrue(InterferenceGeometry.blocked(two, 0).isEmpty());
        assertTrue(InterferenceGeometry.blocked(two, -5).isEmpty());
        assertTrue(InterferenceGeometry.blocked(null, 48).isEmpty());
    }
}
