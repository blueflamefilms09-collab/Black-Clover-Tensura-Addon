package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: these are the numbers the owner will argue about in play (what a match is worth, what a rejection costs, how often, whether it kills), so each
 * one is pinned here: every combination of flow and cast, the clamps of the multiplier, the arithmetic of the drain with and without the non-lethal
 * floor, the cooldown at its exact boundary and across a clock that goes backwards. A change to any of them fails with the rule's name.
 */
class FlowRulesTest {
    private static final Alignment L = Alignment.LIGHT, D = Alignment.DARK, N = Alignment.NEUTRAL;

    // ---------------------------------------------------------------- flow of a place
    @Test
    void theFlowOfAPlaceComesFromItsRoad() {
        assertEquals(L, FlowRules.flowOf(Polarity.HOLY, L, N));
        assertEquals(N, FlowRules.flowOf(Polarity.ABYSSAL, L, N), "the Abyssal road is neutral by default");
        assertEquals(D, FlowRules.flowOf(Polarity.ABYSSAL, L, D), "abyssalFlow = dark mirrors the rule");
        assertEquals(N, FlowRules.flowOf(null, L, D), "Floor 0 and everywhere outside the dungeon have no flow");
        assertEquals(N, FlowRules.flowOf(Polarity.HOLY, null, null), "an unreadable flow is neutral");
        assertEquals(N, FlowRules.flowOf(Polarity.ABYSSAL, null, null));
    }

    @Test
    void flowTextsAreReadStrictlyAndFallBack() {
        assertEquals(L, FlowRules.parseFlow("light", D));
        assertEquals(D, FlowRules.parseFlow(" DARK ", L));
        assertEquals(N, FlowRules.parseFlow("none", L));
        assertEquals(N, FlowRules.parseFlow("neutral", L));
        assertEquals(L, FlowRules.parseFlow("gold", L), "an unknown flow reads as the fallback");
        assertEquals(D, FlowRules.parseFlow(null, D));
        assertEquals(N, FlowRules.parseFlow("", N));
        assertTrue(FlowRules.validFlow("light") && FlowRules.validFlow("dark") && FlowRules.validFlow("none") && FlowRules.validFlow("neutral"));
        assertFalse(FlowRules.validFlow("gold"));
        assertFalse(FlowRules.validFlow(null));
    }

    // ---------------------------------------------------------------- a cast against a flow
    @Test
    void everyCombinationOfFlowAndCast() {
        FlowRules.Outcome none = FlowRules.Outcome.NONE, boost = FlowRules.Outcome.BOOST, reject = FlowRules.Outcome.REJECT;
        assertEquals(boost, FlowRules.outcome(L, L));
        assertEquals(boost, FlowRules.outcome(D, D));
        assertEquals(reject, FlowRules.outcome(L, D));
        assertEquals(reject, FlowRules.outcome(D, L));
        for (Alignment a : Alignment.values()) {
            assertEquals(none, FlowRules.outcome(N, a), "a place with no flow does nothing to " + a);
            assertEquals(none, FlowRules.outcome(a, N), "a neutral cast is untouched by " + a);
            assertEquals(none, FlowRules.outcome(null, a));
            assertEquals(none, FlowRules.outcome(a, null));
        }
    }

    @Test
    void aMatchIsMultipliedEverythingElseIsOne() {
        assertEquals(2.5, FlowRules.multiplier(L, L, 2.5));
        assertEquals(4.0, FlowRules.multiplier(D, D, 4.0));
        assertEquals(1.0, FlowRules.multiplier(L, D, 2.5), "a rejection is a penalty elsewhere, never a divisor");
        assertEquals(1.0, FlowRules.multiplier(L, N, 2.5));
        assertEquals(1.0, FlowRules.multiplier(N, L, 2.5));
    }

    @Test
    void theMultiplierIsKeptInsideOneAndTen() {
        assertEquals(1.0, FlowRules.multiplier(L, L, 0.2), "a boost can never be a penalty");
        assertEquals(1.0, FlowRules.multiplier(L, L, -5.0));
        assertEquals(10.0, FlowRules.multiplier(L, L, 99.0));
        assertEquals(10.0, FlowRules.multiplier(L, L, Double.POSITIVE_INFINITY));
        assertEquals(2.5, FlowRules.multiplier(L, L, Double.NaN), "a NaN reads as the default");
        assertEquals(1.0, FlowRules.clampMultiplier(1.0));
        assertEquals(10.0, FlowRules.clampMultiplier(10.0));
    }

    @Test
    void decideBoostsTheMatchAndLeavesTheRest() {
        FlowRules.Decision d = FlowRules.decide(L, L, 10f, 2.5, false);
        assertEquals(FlowRules.Outcome.BOOST, d.outcome());
        assertEquals(25f, d.amount());
        assertFalse(d.cancel());
        d = FlowRules.decide(L, null, 10f, 2.5, false);
        assertEquals(FlowRules.Outcome.NONE, d.outcome(), "no recent cast, no boost");
        assertEquals(10f, d.amount());
        d = FlowRules.decide(L, N, 10f, 2.5, true);
        assertEquals(10f, d.amount(), "a neutral cast is untouched even when rejected casts are cancelled");
        assertFalse(d.cancel());
        d = FlowRules.decide(N, L, 10f, 2.5, true);
        assertEquals(FlowRules.Outcome.NONE, d.outcome(), "a place with no flow boosts nothing");
        assertEquals(10f, d.amount());
        d = FlowRules.decide(D, D, 8f, 1.5, false);
        assertEquals(12f, d.amount(), "a dark flow boosts dark casts the same way");
    }

    @Test
    void decideRejectsTheOpposingCastAndCancelsOnlyWhenAsked() {
        FlowRules.Decision d = FlowRules.decide(L, D, 10f, 2.5, false);
        assertEquals(FlowRules.Outcome.REJECT, d.outcome());
        assertEquals(10f, d.amount(), "the drain is the penalty; the damage is left alone by default");
        assertFalse(d.cancel());
        d = FlowRules.decide(L, D, 10f, 2.5, true);
        assertEquals(FlowRules.Outcome.REJECT, d.outcome());
        assertEquals(0f, d.amount());
        assertTrue(d.cancel());
    }

    @Test
    void decideHandsBackANaNUntouched() {
        FlowRules.Decision d = FlowRules.decide(L, L, Float.NaN, 2.5, false);
        assertEquals(FlowRules.Outcome.NONE, d.outcome());
        assertTrue(Float.isNaN(d.amount()));
        assertFalse(d.cancel());
        assertEquals(0f, FlowRules.decide(L, L, 0f, 2.5, false).amount(), "zero damage stays zero");
    }

    // ---------------------------------------------------------------- the mod's own limits
    @Test
    void aBossBudgetPaysOnlyForTheExtraTheBoostAdds() {
        float[] left = {6f};
        java.util.function.DoubleUnaryOperator budget = x -> {
            double got = Math.min(x, left[0]);
            left[0] -= (float) got;
            return got;
        };
        assertEquals(25f, FlowRules.chargeExtra(10f, 25f, x -> x), "an unlimited budget lets the whole boost through");
        assertEquals(10f, FlowRules.chargeExtra(10f, 25f, x -> 0.0), "an empty budget leaves the hit as the boss already counted it");
        assertEquals(16f, FlowRules.chargeExtra(10f, 25f, budget), "6 left of the 15 asked: 10 + 6");
        assertEquals(0f, left[0], "and the budget is spent");
        assertEquals(10f, FlowRules.chargeExtra(10f, 25f, budget), "nothing left: no extra");
        assertEquals(25f, FlowRules.chargeExtra(10f, 25f, x -> 99.0), "a budget cannot grant more than was asked");
        assertEquals(10f, FlowRules.chargeExtra(10f, 25f, x -> -4.0), "a negative grant is read as nothing");
    }

    @Test
    void aBossBudgetIsNotAskedWhenThereIsNoBoost() {
        boolean[] asked = {false};
        java.util.function.DoubleUnaryOperator spy = x -> {
            asked[0] = true;
            return x;
        };
        assertEquals(10f, FlowRules.chargeExtra(10f, 10f, spy));
        assertEquals(8f, FlowRules.chargeExtra(10f, 8f, spy), "a hit that was lowered is not a boost, and is returned as it is");
        assertFalse(asked[0], "no extra, no charge");
        assertEquals(25f, FlowRules.chargeExtra(10f, 25f, null), "no budget at all: the boost stands");
        assertEquals(5f, FlowRules.chargeExtra(Float.NaN, 5f, spy), "an unreadable original hands back the boosted number untouched");
        assertTrue(Float.isNaN(FlowRules.chargeExtra(5f, Float.NaN, spy)), "and an unreadable boost is handed back as it came");
        assertFalse(asked[0], "the budget is never asked about a number it cannot read");
    }

    @Test
    void aBoostIsKeptUnderTheCeilingButNeverBelowTheOriginalHit() {
        assertEquals(25f, FlowRules.capBoost(10f, 25f, 40f), "under the ceiling: the whole boost");
        assertEquals(16f, FlowRules.capBoost(10f, 25f, 16f), "over the ceiling: held at it");
        assertEquals(12f, FlowRules.capBoost(12f, 25f, 8f), "a hit that was already over the ceiling is not lowered by the cap on the boost");
        assertEquals(10f, FlowRules.capBoost(10f, 10f, 4f), "no boost, no change");
        assertEquals(25f, FlowRules.capBoost(10f, 25f, Float.NaN), "an unreadable ceiling does not cut anything");
    }

    // ---------------------------------------------------------------- the rejection
    @Test
    void theDrainIsAShareOfMaxHealthWithAFloor() {
        assertEquals(2.0, FlowRules.rejectionDrain(20.0, 20.0, 8.0, 2.0, true), 1e-9, "8 percent of 20 is 1.6, raised to the 2 HP floor");
        assertEquals(3.2, FlowRules.rejectionDrain(40.0, 40.0, 8.0, 2.0, true), 1e-9);
        assertEquals(80.0, FlowRules.rejectionDrain(1000.0, 1000.0, 8.0, 2.0, true), 1e-9, "it scales with the caster's maximum health");
        assertEquals(2.0, FlowRules.rejectionDrain(10.0, 10.0, 0.0, 2.0, true), 1e-9, "a zero percent still pays the floor");
        assertEquals(0.0, FlowRules.rejectionDrain(100.0, 100.0, 0.0, 0.0, true), 1e-9, "the owner can switch the drain off with zeros");
    }

    @Test
    void nonLethalNeverTakesTheLastHeart() {
        assertEquals(3.2, FlowRules.rejectionDrain(40.0, 40.0, 8.0, 2.0, true), 1e-9);
        assertEquals(3.2, FlowRules.rejectionDrain(40.0, 4.2, 8.0, 2.0, true), 1e-9, "4.2 HP minus 3.2 leaves exactly 1");
        assertEquals(1.0, FlowRules.rejectionDrain(40.0, 2.0, 8.0, 2.0, true), 1e-9, "2 HP loses only 1");
        assertEquals(0.0, FlowRules.rejectionDrain(40.0, 1.0, 8.0, 2.0, true), 1e-9, "1 HP loses nothing");
        assertEquals(0.0, FlowRules.rejectionDrain(40.0, 0.5, 8.0, 2.0, true), 1e-9);
        assertEquals(0.0, FlowRules.rejectionDrain(40.0, 0.0, 8.0, 2.0, true), 1e-9);
        for (double hp = 0.0; hp <= 40.0; hp += 0.25) {
            double d = FlowRules.rejectionDrain(40.0, hp, 8.0, 2.0, true);
            assertTrue(hp - d >= Math.min(hp, 1.0) - 1e-9, "non-lethal left " + (hp - d) + " of " + hp);
            assertTrue(d >= 0.0);
        }
    }

    @Test
    void lethalIsPossibleOnlyWhenTheOwnerAllowsIt() {
        assertEquals(3.2, FlowRules.rejectionDrain(40.0, 2.0, 8.0, 2.0, false), 1e-9, "with nonLethal off the full price is asked");
        assertEquals(40.0, FlowRules.rejectionDrain(40.0, 1.0, 100.0, 2.0, false), 1e-9);
    }

    @Test
    void theDrainShrugsOffNonsense() {
        assertEquals(0.0, FlowRules.rejectionDrain(0.0, 10.0, 8.0, 2.0, true));
        assertEquals(0.0, FlowRules.rejectionDrain(-5.0, 10.0, 8.0, 2.0, true));
        assertEquals(0.0, FlowRules.rejectionDrain(Double.NaN, 10.0, 8.0, 2.0, true));
        assertEquals(0.0, FlowRules.rejectionDrain(20.0, Double.NaN, 8.0, 2.0, true));
        assertEquals(2.0, FlowRules.rejectionDrain(20.0, 20.0, Double.NaN, 2.0, true), "a NaN percent pays the floor only");
        assertEquals(1.6, FlowRules.rejectionDrain(20.0, 20.0, 8.0, Double.NaN, true), 1e-9, "a NaN floor is no floor");
        assertEquals(2.0, FlowRules.rejectionDrain(20.0, 20.0, -8.0, 2.0, true), "a negative percent pays the floor only");
        assertEquals(1.6, FlowRules.rejectionDrain(20.0, 20.0, 8.0, -3.0, true), 1e-9);
    }

    @Test
    void theCooldownRunsOutExactlyAtItsLength() {
        assertTrue(FlowRules.due(100, FlowRules.NEVER, 20), "never rejected: due");
        assertFalse(FlowRules.due(100, 100, 20), "the same tick: not due");
        assertFalse(FlowRules.due(119, 100, 20));
        assertTrue(FlowRules.due(120, 100, 20), "exactly 20 ticks later: due");
        assertTrue(FlowRules.due(121, 100, 20));
        assertTrue(FlowRules.due(100, 100, 0), "a zero cooldown is always due");
        assertTrue(FlowRules.due(100, 100, -4));
        assertTrue(FlowRules.due(50, 100, 20), "a clock set back never locks anybody out");
        assertTrue(FlowRules.due(Long.MAX_VALUE / 2, FlowRules.NEVER, 20), "NEVER cannot overflow the subtraction for any realistic game time");
    }

    // ---------------------------------------------------------------- the alignment enum
    @Test
    void alignmentIdsAndOpposites() {
        assertEquals("light", L.id());
        assertEquals(D, L.opposite());
        assertEquals(L, D.opposite());
        assertEquals(N, N.opposite());
        assertEquals(L, Alignment.fromId("LIGHT"));
        assertEquals(N, Alignment.fromId(" none "));
        assertNull(Alignment.fromId("shadow"));
        assertNull(Alignment.fromId(null));
    }
}
