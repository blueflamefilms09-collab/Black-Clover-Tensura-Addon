package com.newuniverse.nusmp.labyrinth.mega.flow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the decennial Metal Vessel is a one-of-a-kind artifact handed out outside the Crucible, so two things must be exactly right and cannot be
 * tried in the game by the author: which relics are allowed to be handed out (a consumable, a pair half, a relic that cannot carry a Djinn must never be
 * chosen), and that the weighted draw lands where the weights say, including at both ends of the roll, with an empty pool, and with weights that are
 * zero, negative or not numbers. The once-per-sector memory is pinned here as well.
 */
class VesselRulesTest {
    private static List<VesselRules.Candidate> pool(Object... idAndWeight) {
        List<VesselRules.Candidate> l = new ArrayList<>();
        for (int i = 0; i < idAndWeight.length; i += 2) l.add(new VesselRules.Candidate((String) idAndWeight[i], ((Number) idAndWeight[i + 1]).doubleValue()));
        return l;
    }

    @Test
    void onlyAOneOfAKindFlaggedDjinnCapableNonPairRelicIsEligible() {
        int eligible = 0;
        for (int bits = 0; bits < 16; bits++) {
            boolean unique = (bits & 1) != 0, vessel = (bits & 2) != 0, capable = (bits & 4) != 0, pair = (bits & 8) != 0;
            boolean e = VesselRules.eligible(unique, vessel, capable, pair);
            if (e) eligible++;
            assertEquals(unique && vessel && capable && !pair, e, "bits " + bits);
        }
        assertEquals(1, eligible, "exactly one of the sixteen combinations is a candidate");
    }

    @Test
    void anEmptyOrUnusablePoolPicksNothing() {
        assertEquals(-1, VesselRules.pick(null, 0.5));
        assertEquals(-1, VesselRules.pick(new ArrayList<>(), 0.5));
        assertEquals(-1, VesselRules.pick(pool("a", 0.0, "b", -1.0), 0.5), "no positive weight, no pick");
        assertEquals(-1, VesselRules.pick(pool("a", Double.NaN, "b", Double.POSITIVE_INFINITY), 0.5));
    }

    @Test
    void theRollWalksTheWeightsInListOrder() {
        List<VesselRules.Candidate> p = pool("a", 4.0, "b", 2.0, "c", 2.0);   // total 8.0, all exact in binary: a [0,0.5), b [0.5,0.75), c [0.75,1)
        assertEquals(0, VesselRules.pick(p, 0.0));
        assertEquals(0, VesselRules.pick(p, 0.4999));
        assertEquals(1, VesselRules.pick(p, 0.5), "the boundary belongs to the next candidate");
        assertEquals(1, VesselRules.pick(p, 0.7499));
        assertEquals(2, VesselRules.pick(p, 0.75));
        assertEquals(2, VesselRules.pick(p, 0.999999));
        assertEquals(2, VesselRules.pick(p, 1.0), "a roll of exactly 1 clamps to the last candidate instead of walking off the end");
        assertEquals(0, VesselRules.pick(p, -3.0), "a negative roll clamps to the first");
        assertEquals(0, VesselRules.pick(p, Double.NaN), "an unreadable roll is the first");
    }

    @Test
    void candidatesThatCannotBePickedAreSkippedWhereverTheyStand() {
        List<VesselRules.Candidate> p = pool("zero", 0.0, "a", 2.0, "neg", -1.0, "b", 2.0, "nan", Double.NaN);
        assertEquals(1, VesselRules.pick(p, 0.0));
        assertEquals(1, VesselRules.pick(p, 0.49));
        assertEquals(3, VesselRules.pick(p, 0.5));
        assertEquals(3, VesselRules.pick(p, 1.0), "the last PICKABLE candidate, not the last in the list");
    }

    @Test
    void theDrawFollowsTheWeights() {
        List<VesselRules.Candidate> p = pool("legend", 4.0, "god", 1.5, "genesis", 0.5);
        int[] hits = new int[3];
        int n = 60000;
        for (int i = 0; i < n; i++) hits[VesselRules.pick(p, (i + 0.5) / n)]++;
        assertEquals(40000, hits[0], 2, "legend is 4 of 6");
        assertEquals(15000, hits[1], 2, "god is 1.5 of 6");
        assertEquals(5000, hits[2], 2, "genesis is 0.5 of 6");
    }

    @Test
    void aSingleCandidateIsAlwaysThePick() {
        List<VesselRules.Candidate> p = pool("only", 0.25);
        for (double r : new double[] {0.0, 0.3, 0.9999, 1.0}) assertEquals(0, VesselRules.pick(p, r));
    }

    // ---------------------------------------------------------------- the once-per-sector memory
    @Test
    void aSectorPaysOnlyOnce() {
        RewardLog log = new RewardLog();
        assertFalse(log.contains(3));
        assertNull(log.relicOf(3));
        assertTrue(log.claim(3, "godking_crown"));
        assertFalse(log.claim(3, "hunger"), "the second claim is refused");
        assertEquals("godking_crown", log.relicOf(3), "and the first relic is kept");
        assertTrue(log.claim(0, "frostfang"));
        assertEquals(2, log.size());
        assertEquals(List.of(3, 0), new ArrayList<>(log.entries().keySet()), "in the order the sectors paid");
    }

    @Test
    void restoringFromSavedDataDoesNotOverwriteAndToleratesNull() {
        RewardLog log = new RewardLog();
        log.restore(1, "a");
        log.restore(1, "b");
        log.restore(2, null);
        assertEquals("a", log.relicOf(1));
        assertEquals("", log.relicOf(2));
        assertFalse(log.claim(1, "c"), "a restored sector has paid");
        assertTrue(log.claim(5, null), "a null relic id is stored as empty, not a crash");
        boolean threw = false;
        try { log.entries().put(9, "x"); } catch (UnsupportedOperationException e) { threw = true; }
        assertTrue(threw, "the entries view is read-only");
    }
}
