package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the kind table is read by five different places (the use logic, the creative tab, the dungeon drops, the model overrides and the Black Market), so a
 * wrong id, a renumbered index or a weight that sums to zero would break them all in ways nobody sees until a sprite is wrong or a drop never happens. The test
 * pins the five ids and their guaranteed amounts as the design lists them, the stable 1-based numbering the item model is written against, the lookup edges
 * (null, empty, unknown, wrong case), the cumulative boundaries of the weighted pick with scripted dice, the proportions of a long seeded run, and the means that the
 * Javadoc and the docs quote, so a retuned weight that changes the economy shows up here first.
 */
class SpKindTest {
    private static Dice fixed(double v) { return () -> v; }

    @Test
    void theFiveKindsAndTheirAmountsAreTheDesignsTable() {
        SpKind[] k = SpKind.values();
        assertEquals(5, k.length);
        String[] ids = {"beginner_archery", "elemental_primer", "magic_booster", "jack_in_the_box", "greater_primer"};
        int[] amounts = {2, 3, 5, 1, 10};
        for (int i = 0; i < 5; i++) {
            assertEquals(ids[i], k[i].id());
            assertEquals(amounts[i], k[i].amount());
            assertEquals(i + 1, k[i].index(), "the model overrides are numbered by this: never renumber");
        }
        assertTrue(SpKind.JACK_IN_THE_BOX.isGamble());
        for (SpKind x : k) if (x != SpKind.JACK_IN_THE_BOX) assertFalse(x.isGamble(), x.id());
    }

    @Test
    void idsAreUniqueLowerSnakeAndLookUpBack() {
        Set<String> seen = new HashSet<>();
        for (SpKind k : SpKind.values()) {
            assertTrue(k.id().matches("[a-z]+(_[a-z]+)*"), k.id());
            assertTrue(seen.add(k.id()), "duplicate id " + k.id());
            assertSame(k, SpKind.byId(k.id()));
        }
    }

    @Test
    void lookupEdges() {
        assertNull(SpKind.byId(null));
        assertNull(SpKind.byId(""));
        assertNull(SpKind.byId("nothing"));
        assertNull(SpKind.byId("MAGIC_BOOSTER"), "ids are stored lower case and matched exactly");
        assertNull(SpKind.byId(" magic_booster"));
    }

    @Test
    void weightsArePositiveAndTotalOneHundred() {
        for (SpKind k : SpKind.values()) {
            assertTrue(k.monsterWeight() > 0, k.id());
            assertTrue(k.bossWeight() > 0, k.id());
        }
        assertEquals(100, SpKind.totalWeight(false));
        assertEquals(100, SpKind.totalWeight(true));
    }

    @Test
    void theWeightedPickFollowsTheCumulativeBoundaries() {
        // monster weights 30 25 20 20 5: boundaries at 30, 55, 75, 95
        assertSame(SpKind.BEGINNER_ARCHERY, SpKind.pickDrop(fixed(0.0), false));
        assertSame(SpKind.BEGINNER_ARCHERY, SpKind.pickDrop(fixed(0.299), false));
        assertSame(SpKind.ELEMENTAL_PRIMER, SpKind.pickDrop(fixed(0.30), false));
        assertSame(SpKind.ELEMENTAL_PRIMER, SpKind.pickDrop(fixed(0.549), false));
        assertSame(SpKind.MAGIC_BOOSTER, SpKind.pickDrop(fixed(0.55), false));
        assertSame(SpKind.JACK_IN_THE_BOX, SpKind.pickDrop(fixed(0.75), false));
        assertSame(SpKind.GREATER_PRIMER, SpKind.pickDrop(fixed(0.95), false));
        assertSame(SpKind.GREATER_PRIMER, SpKind.pickDrop(fixed(0.9999999), false));
        // boss weights 5 15 35 20 25: boundaries at 5, 20, 55, 75
        assertSame(SpKind.BEGINNER_ARCHERY, SpKind.pickDrop(fixed(0.049), true));
        assertSame(SpKind.ELEMENTAL_PRIMER, SpKind.pickDrop(fixed(0.05), true));
        assertSame(SpKind.MAGIC_BOOSTER, SpKind.pickDrop(fixed(0.20), true));
        assertSame(SpKind.JACK_IN_THE_BOX, SpKind.pickDrop(fixed(0.55), true));
        assertSame(SpKind.GREATER_PRIMER, SpKind.pickDrop(fixed(0.75), true));
    }

    @Test
    void aLongSeededRunLandsOnTheWeights() {
        for (boolean boss : new boolean[] {false, true}) {
            Random r = new Random(62_0_0L + (boss ? 1 : 0));
            Dice d = r::nextDouble;
            int n = 200_000;
            int[] count = new int[5];
            for (int i = 0; i < n; i++) count[SpKind.pickDrop(d, boss).ordinal()]++;
            for (SpKind k : SpKind.values()) {
                double p = (boss ? k.bossWeight() : k.monsterWeight()) / 100.0, sigma = Math.sqrt(n * p * (1 - p));
                assertTrue(Math.abs(count[k.ordinal()] - n * p) < 5 * sigma, k.id() + " boss=" + boss + ": " + count[k.ordinal()] + " of " + n);
            }
        }
    }

    @Test
    void theMeansTheDocsQuote() {
        double monster = SpKind.meanDrop(false), boss = SpKind.meanDrop(true);
        assertTrue(monster > 3.7 && monster < 4.0, "monster mean " + monster);
        assertTrue(boss > 5.6 && boss < 6.0, "boss mean " + boss);
        assertTrue(boss > monster, "a Guardian's SP Books are better than a monster's");
    }
}
