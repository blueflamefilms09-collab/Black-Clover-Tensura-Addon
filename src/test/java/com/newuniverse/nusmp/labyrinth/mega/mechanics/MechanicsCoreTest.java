package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the small pure rules of the mechanics library, each pinned on its own: depth scaling and the signature buckets, the seed maths, the weights, the conflict table, the
 * blacklist, the counter conditions, the shapes and the monster rosters.
 */
class MechanicsCoreTest {
    @Test
    void tiersAndScalingFollowTheFiveTiersOfTwoHundredFloors() {
        assertEquals(1, Scaling.tierOf(1));
        assertEquals(1, Scaling.tierOf(-200));
        assertEquals(2, Scaling.tierOf(201));
        assertEquals(5, Scaling.tierOf(-1000));
        assertEquals(5, Scaling.tierOf(5000));
        assertEquals(1, Scaling.tierOf(0));
        assertEquals(0.0, Scaling.s(1), 1e-9);
        assertEquals(4.995, Scaling.s(1000), 1e-9);
        assertEquals(Scaling.s(77), Scaling.s(-77), 1e-12);
        assertEquals(10.0 + 2.0 * Scaling.s(401), Scaling.value(10, 2, 401), 1e-9);
        assertTrue(Scaling.value(3, 1, 900) > Scaling.value(3, 1, 10));
    }

    @Test
    void bucketsGroupNearbyNumbersAndSeparateDistantOnes() {
        assertEquals(Scaling.bucket(13), Scaling.bucket(15));
        assertTrue(Scaling.bucket(2) < Scaling.bucket(3));
        assertTrue(Scaling.bucket(2) < Scaling.bucket(8));
        assertEquals(0, Scaling.bucket(0));
        assertEquals(-Scaling.bucket(5), Scaling.bucket(-5));
        assertEquals(Scaling.bucket(100000), Scaling.bucket(9999999));
    }

    @Test
    void difficultyIsClampedAndNeverZeroesAPositiveAmount() {
        assertEquals(0.25, Scaling.clampDifficulty(0.0), 1e-9);
        assertEquals(4.0, Scaling.clampDifficulty(99), 1e-9);
        assertEquals(1.0, Scaling.byDifficulty(1.0, 0.25, 1.0), 1e-9);
        assertEquals(0.0, Scaling.byDifficulty(0.0, 2.0, 1.0), 1e-9);
        assertEquals(6.0, Scaling.byDifficulty(3.0, 2.0, 1.0), 1e-9);
    }

    @Test
    void seedsAreStableAndSpreadOut() {
        assertEquals(Seeds.floorSeed(12345L, 7), Seeds.floorSeed(12345L, 7));
        Set<Long> seen = new HashSet<>();
        for (int f = -50; f <= 50; f++) assertTrue(seen.add(Seeds.floorSeed(99L, f)), "floor seeds collide at " + f);
        assertTrue(Seeds.slotSeed(5L, 1, 0) != Seeds.slotSeed(5L, 2, 0));
        assertTrue(Seeds.slotSeed(5L, 1, 0) != Seeds.slotSeed(5L, 1, 1));
        SplittableRandom a = Seeds.rng(42), b = Seeds.rng(42);
        for (int i = 0; i < 20; i++) assertEquals(a.nextInt(1000), b.nextInt(1000));
    }

    @Test
    void weightsFollowTierRoadThemeAndRoomKind() {
        MechanicDef d = new MechanicDef("x.y", "n", "d", "", MechCategory.HAZARD, "S", "y", Scope.ROOM, 2, 3, Road.HOLY, List.of("flame"), 3, 10,
                new MechanicDef.Trigger("timer", Args.EMPTY), List.of(), new MechanicDef.Counter("none", Args.EMPTY), "none", "");
        assertEquals(0.0, Weights.weight(d, 1, Polarity.HOLY, "flame", 1.0), 1e-9, "below its tier range");
        assertEquals(0.0, Weights.weight(d, 650, Polarity.HOLY, "flame", 1.0), 1e-9, "above its tier range");
        assertEquals(0.0, Weights.weight(d, 250, Polarity.ABYSSAL, "flame", 1.0), 1e-9, "the other road");
        assertEquals(30.0, Weights.weight(d, 250, Polarity.HOLY, "flame", 1.0), 1e-9);
        assertEquals(3.5, Weights.weight(d, 250, Polarity.HOLY, "frost", 1.0), 1e-9);
        assertEquals(10.0, Weights.weight(d, 250, null, null, 1.0) * (Road.HOLY.allows(null) ? 1 : 0), 1e-9);
        assertTrue(Weights.roomBias("trap", MechCategory.HAZARD) > Weights.roomBias("trap", MechCategory.PUZZLE));
        assertTrue(Weights.roomBias("puzzle", MechCategory.PUZZLE) > Weights.roomBias("puzzle", MechCategory.COMBAT));
        assertEquals(0.0, Weights.roomBias("arena", MechCategory.PUZZLE), 1e-9);
        assertTrue(Weights.floorBudget(5) > Weights.floorBudget(1));
    }

    @Test
    void theConflictTableComesFromTheChannels() {
        OpCatalog cat = TestLibrary.catalog();
        MechanicDef gravity = null, gravity2 = null, plain = null;
        for (MechanicDef d : TestLibrary.defs()) {
            Set<String> ch = Channels.of(d, cat);
            if (ch.contains("gravity")) { if (gravity == null) gravity = d; else if (gravity2 == null && !d.base().equals(gravity.base())) gravity2 = d; }
            if (ch.isEmpty() && plain == null) plain = d;
        }
        if (gravity != null && gravity2 != null) assertTrue(Channels.conflict(gravity, gravity2, cat), "two gravity mechanics must conflict");
        if (gravity != null && plain != null) assertFalse(Channels.conflict(gravity, plain, cat), "a gravity mechanic and a channel-free one must not conflict");
        for (MechanicDef d : TestLibrary.defs()) assertTrue(Channels.conflict(d, d, cat), "a mechanic conflicts with itself");
    }

    @Test
    void theBlacklistParsesAndMatchesWholeBases() {
        Set<String> l = Blacklist.parse(" Hazard.Spore_Bloom.* , floor.rim_curse ,, ");
        assertEquals(2, l.size());
        assertTrue(Blacklist.bans(l, "hazard.spore_bloom"));
        assertTrue(Blacklist.bans(l, "hazard.spore_bloom.e_fire"));
        assertTrue(Blacklist.bans(l, "floor.rim_curse"));
        assertFalse(Blacklist.bans(l, "floor.rim_curse.e_wither"));
        assertFalse(Blacklist.bans(Set.of(), "anything"));
        assertTrue(Blacklist.parse(null).isEmpty());
    }

    @Test
    void counterConditionsGateEffects() {
        Map<String, Integer> c = Map.of("wrong", 3, "waves", 1);
        assertTrue(Cond.test("wrong>=3", c));
        assertFalse(Cond.test("wrong>3", c));
        assertTrue(Cond.test("waves<2", c));
        assertTrue(Cond.test("never==0", c), "a counter never set reads 0");
        assertTrue(Cond.test("nonsense", c), "text that does not parse never disables an effect");
        assertTrue(Cond.valid("wrong>=3"));
        assertFalse(Cond.valid("wrong >> 3"));
    }

    @Test
    void shapesHaveTheirCounts() {
        SplittableRandom rng = Seeds.rng(1);
        assertEquals(9, Shapes.offsets("square", 1, 0, rng).size());
        assertEquals(9, Shapes.offsets("cross", 2, 0, rng).size());
        assertEquals(5, Shapes.offsets("line", 2, 0, rng).size());
        assertEquals(7, Shapes.offsets("scatter", 5, 7, rng).size());
        assertTrue(Shapes.offsets("ring", 4, 0, rng).size() >= 16);
        assertTrue(Shapes.offsets("disc", 3, 0, rng).size() >= 29);
        for (int[] o : Shapes.offsets("walls", 5, 0, rng)) assertTrue(Math.abs(o[0]) == 5 || Math.abs(o[1]) == 5);
        for (int[] o : Shapes.offsets("scatter", 99, 500, rng)) assertTrue(Math.abs(o[0]) <= Shapes.MAX_RADIUS && Math.abs(o[1]) <= Shapes.MAX_RADIUS);
        List<int[]> a = Shapes.offsets("scatter", 6, 5, Seeds.rng(9)), b = Shapes.offsets("scatter", 6, 5, Seeds.rng(9));
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i)[0] * 100 + a.get(i)[1], b.get(i)[0] * 100 + b.get(i)[1]);
    }

    @Test
    void monsterGroupsPickByTierAndParse() {
        SplittableRandom rng = Seeds.rng(3);
        for (String g : MobGroups.groups().keySet()) {
            MobGroups.Entry e = MobGroups.pick(g, 1, rng, x -> true);
            assertNotNull(e, g);
            assertTrue(e.tier() <= 1 || MobGroups.groups().get(g).stream().allMatch(x -> x.tier() > 1), g + " picked above the tier");
        }
        assertNull(MobGroups.pick("no_such_group", 3, rng, x -> true));
        List<String> problems = new java.util.ArrayList<>();
        Map<String, List<MobGroups.Entry>> m = MobGroups.parse("{\"a\":[{\"id\":\"minecraft:zombie\",\"tier\":2},{\"x\":1}],\"_why\":\"x\"}", problems);
        assertEquals(1, m.get("a").size());
        assertEquals(1, problems.size());
        MobGroups.install(m);
        MobGroups.Entry picked = MobGroups.pick("a", 5, rng, x -> true);
        MobGroups.install(null);
        assertNotNull(picked, "a parsed group can be picked from");
        assertEquals("minecraft:zombie", picked.id());
    }

    @Test
    void theCatalogueMatchesTheParserAndTheShippedArguments() {
        OpCatalog cat = TestLibrary.catalog();
        assertTrue(cat.ops().size() >= 80, "the library promises about eighty primitives: " + cat.ops().size());
        assertTrue(cat.triggers().size() >= 15);
        assertTrue(cat.counters().size() >= 15);
        assertTrue(cat.exclusiveChannels().contains("gravity"));
        assertEquals("vision", cat.effectChannel("minecraft:darkness"));
        for (MechanicDef d : TestLibrary.defs()) assertTrue(cat.check(d).isEmpty(), d.id() + ": " + cat.check(d));
    }
}
