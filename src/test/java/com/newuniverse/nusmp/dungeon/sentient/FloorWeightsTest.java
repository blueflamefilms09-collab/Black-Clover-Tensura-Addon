package com.newuniverse.nusmp.dungeon.sentient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** The spawn lists of the three floors: goblins, slimes and the kobold's stand-in; lizardmen, aqua slimes and water spirits; devils and disciples. */
class FloorWeightsTest {
    private static Map<String, Integer> asMap(FloorKind k) {
        Map<String, Integer> m = new HashMap<>();
        for (FloorWeights.Entry e : FloorWeights.defaults(k)) m.put(e.id(), e.weight());
        return m;
    }

    @Test
    void theListsAreTheOwnersFloors() {
        assertEquals(Map.of("tensura:goblin", 4, "tensura:slime", 3, "tensura:hound_dog", 2), asMap(FloorKind.CATACOMBS));
        assertEquals(Map.of("tensura:lizardman", 4, "tensura:slime", 3, "tensura:undine", 2), asMap(FloorKind.WATERWAYS));
        assertEquals(Map.of("tensura:lesser_daemon", 3, "nusmp:dark_disciple", 2), asMap(FloorKind.UNDERWORLD));
        assertTrue(FloorWeights.defaults(FloorKind.MONARCH).isEmpty());
        assertTrue(FloorWeights.defaults(FloorKind.LEGACY).isEmpty());
    }

    @Test
    void anIdOfAModpackGetsTheUnlistedWeight() {
        assertEquals(4, FloorWeights.weight(FloorKind.CATACOMBS, "tensura:goblin"));
        assertEquals(FloorWeights.UNLISTED, FloorWeights.weight(FloorKind.CATACOMBS, "somemod:kobold"));
        assertEquals(FloorWeights.UNLISTED, FloorWeights.weight(FloorKind.MONARCH, "tensura:goblin"));
    }

    @Test
    void onlyTheWaterwaysSlimeIsAnAquaSlime() {
        assertTrue(FloorWeights.aqua(FloorKind.WATERWAYS, "tensura:slime"));
        assertFalse(FloorWeights.aqua(FloorKind.CATACOMBS, "tensura:slime"));
        assertFalse(FloorWeights.aqua(FloorKind.WATERWAYS, "tensura:lizardman"));
        assertFalse(FloorWeights.aqua(FloorKind.UNDERWORLD, "tensura:slime"));
    }

    @Test
    void choosingFollowsTheWeights() {
        List<String> ids = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (FloorWeights.Entry e : FloorWeights.defaults(FloorKind.CATACOMBS)) {
            ids.add(e.id());
            weights.add(e.weight());
        }
        Dice d = new Random(3)::nextDouble;
        Map<String, Integer> seen = new HashMap<>();
        int n = 45000;
        for (int i = 0; i < n; i++) seen.merge(FloorWeights.pick(ids, weights, d), 1, Integer::sum);
        assertEquals(4.0 / 9.0, seen.get("tensura:goblin") / (double) n, 0.01);
        assertEquals(3.0 / 9.0, seen.get("tensura:slime") / (double) n, 0.01);
        assertEquals(2.0 / 9.0, seen.get("tensura:hound_dog") / (double) n, 0.01);
    }

    @Test
    void anEmptyOrZeroWeightListChoosesNothing() {
        Dice d = new Random(1)::nextDouble;
        assertNull(FloorWeights.pick(List.of(), List.of(), d));
        assertNull(FloorWeights.pick(List.of("a", "b"), List.of(0, 0), d));
        assertNull(FloorWeights.pick(List.of("a"), List.of(-4), d));
        assertEquals("b", FloorWeights.pick(List.of("a", "b"), List.of(0, 5), d), "an id of weight zero is never chosen");
    }
}
