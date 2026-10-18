package com.newuniverse.nusmp.heroweapon.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: a player who cannot start the Choosing must be told the first thing that is wrong, always the same one, and a player who can start it must never be
 * refused. Every refusal path of {@link GemRules#decide} is pinned here with its priority, together with the small helpers around it (the operator and
 * creative exception to a closed route, the common-reason test, the shuffle).
 */
class GemRulesTest {
    private static final double SPEED = 0.1;
    private static final int CLEAR = 4;

    /** A player who may start: the route is open, nothing carried, a weapon free, standing still in the open. */
    private static GemRules.Facts good() {
        return new GemRules.Facts(true, false, false, 4, false, false, false, true, 0.0, 16);
    }

    private static GemRules.Facts with(GemRules.Facts f, java.util.function.UnaryOperator<Builder> edit) {
        Builder b = edit.apply(new Builder(f));
        return b.build();
    }

    /** A tiny copy-with helper so each test changes exactly one fact. */
    private static final class Builder {
        boolean routeOpen, bound, sealed, gateRefused, riding, submerged, onGround;
        int free, clear;
        double speed;

        Builder(GemRules.Facts f) {
            routeOpen = f.routeOpen(); bound = f.bound(); sealed = f.sealed(); free = f.freeCandidates(); gateRefused = f.gateRefused();
            riding = f.riding(); submerged = f.submerged(); onGround = f.onGround(); speed = f.speed(); clear = f.clearAbove();
        }

        GemRules.Facts build() { return new GemRules.Facts(routeOpen, bound, sealed, free, gateRefused, riding, submerged, onGround, speed, clear); }
    }

    @Test
    void aPlayerWhoIsStillAndFreeMayStart() {
        assertEquals(GemRules.Denial.NONE, GemRules.decide(good(), SPEED, CLEAR));
    }

    @Test
    void everyConditionRefusesWithItsOwnDenial() {
        assertEquals(GemRules.Denial.ROUTE_CLOSED, GemRules.decide(with(good(), b -> { b.routeOpen = false; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.ALREADY_BOUND, GemRules.decide(with(good(), b -> { b.bound = true; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.GATE_REFUSED, GemRules.decide(with(good(), b -> { b.gateRefused = true; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.NONE_FREE, GemRules.decide(with(good(), b -> { b.free = 0; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.RIDING, GemRules.decide(with(good(), b -> { b.riding = true; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.SUBMERGED, GemRules.decide(with(good(), b -> { b.submerged = true; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.AIRBORNE, GemRules.decide(with(good(), b -> { b.onGround = false; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.MOVING, GemRules.decide(with(good(), b -> { b.speed = 0.2; return b; }), SPEED, CLEAR));
        assertEquals(GemRules.Denial.CLOSED_ABOVE, GemRules.decide(with(good(), b -> { b.clear = 3; return b; }), SPEED, CLEAR));
    }

    @Test
    void aSealNeedsNoFreeHolyWeaponButAHolyGemDoes() {
        GemRules.Facts noneFree = with(good(), b -> { b.free = 0; return b; });
        assertEquals(GemRules.Denial.NONE_FREE, GemRules.decide(noneFree, SPEED, CLEAR));
        assertEquals(GemRules.Denial.NONE, GemRules.decide(with(noneFree, b -> { b.sealed = true; return b; }), SPEED, CLEAR), "a Seal names its weapon, the count of free Holy Weapons is irrelevant");
    }

    @Test
    void theFirstFailingConditionWins() {
        GemRules.Facts everythingWrong = new GemRules.Facts(false, true, false, 0, true, true, true, false, 9.0, 0);
        assertEquals(GemRules.Denial.ROUTE_CLOSED, GemRules.decide(everythingWrong, SPEED, CLEAR));
        GemRules.Facts open = with(everythingWrong, b -> { b.routeOpen = true; return b; });
        assertEquals(GemRules.Denial.ALREADY_BOUND, GemRules.decide(open, SPEED, CLEAR));
        GemRules.Facts free = with(open, b -> { b.bound = false; return b; });
        assertEquals(GemRules.Denial.GATE_REFUSED, GemRules.decide(free, SPEED, CLEAR));
        GemRules.Facts allowed = with(free, b -> { b.gateRefused = false; return b; });
        assertEquals(GemRules.Denial.NONE_FREE, GemRules.decide(allowed, SPEED, CLEAR));
        GemRules.Facts some = with(allowed, b -> { b.free = 2; return b; });
        assertEquals(GemRules.Denial.RIDING, GemRules.decide(some, SPEED, CLEAR));
        GemRules.Facts walking = with(some, b -> { b.riding = false; return b; });
        assertEquals(GemRules.Denial.SUBMERGED, GemRules.decide(walking, SPEED, CLEAR));
        GemRules.Facts dry = with(walking, b -> { b.submerged = false; return b; });
        assertEquals(GemRules.Denial.AIRBORNE, GemRules.decide(dry, SPEED, CLEAR));
        GemRules.Facts grounded = with(dry, b -> { b.onGround = true; return b; });
        assertEquals(GemRules.Denial.MOVING, GemRules.decide(grounded, SPEED, CLEAR));
        GemRules.Facts still = with(grounded, b -> { b.speed = 0.0; return b; });
        assertEquals(GemRules.Denial.CLOSED_ABOVE, GemRules.decide(still, SPEED, CLEAR));
    }

    @Test
    void theAnswerRechecksOnlyWhatCanChangeWhileThePlayerHoldsStill() {
        assertEquals(GemRules.Denial.NONE, GemRules.decideAtEnd(with(good(), b -> { b.riding = true; b.speed = 9.0; b.onGround = false; b.clear = 0; return b; })), "the body is not asked again at the end");
        assertEquals(GemRules.Denial.ROUTE_CLOSED, GemRules.decideAtEnd(with(good(), b -> { b.routeOpen = false; return b; })), "the switch may have been turned off meanwhile");
        assertEquals(GemRules.Denial.ALREADY_BOUND, GemRules.decideAtEnd(with(good(), b -> { b.bound = true; return b; })), "an operator may have bound a weapon meanwhile");
        assertEquals(GemRules.Denial.GATE_REFUSED, GemRules.decideAtEnd(with(good(), b -> { b.gateRefused = true; return b; })));
        assertEquals(GemRules.Denial.NONE_FREE, GemRules.decideAtEnd(with(good(), b -> { b.free = 0; return b; })), "the last free Holy Weapon may have been taken meanwhile");
        assertEquals(GemRules.Denial.NONE, GemRules.decideAtEnd(with(good(), b -> { b.free = 0; b.sealed = true; return b; })));
    }

    @Test
    void theLimitsAreInclusiveAndTheSkyRuleCanBeSwitchedOff() {
        assertEquals(GemRules.Denial.NONE, GemRules.decide(with(good(), b -> { b.speed = SPEED; return b; }), SPEED, CLEAR), "exactly the tolerated speed is fine");
        assertEquals(GemRules.Denial.NONE, GemRules.decide(with(good(), b -> { b.clear = CLEAR; return b; }), SPEED, CLEAR), "exactly the needed space is fine");
        assertEquals(GemRules.Denial.NONE, GemRules.decide(with(good(), b -> { b.clear = 0; return b; }), SPEED, 0), "a need of zero switches the rule off");
    }

    @Test
    void theClosedRouteStaysOpenForOperatorsAndCreative() {
        assertTrue(GemRules.routeOpen(true, false));
        assertTrue(GemRules.routeOpen(true, true));
        assertTrue(GemRules.routeOpen(false, true));
        assertFalse(GemRules.routeOpen(false, false));
    }

    @Test
    void aCommonReasonIsOnlyOneEveryEntryShares() {
        assertEquals("the hero weapons are switched off", GemRules.commonReason(Arrays.asList("the hero weapons are switched off", "the hero weapons are switched off")));
        assertNull(GemRules.commonReason(Arrays.asList("held by Alice", "held by Bob")));
        assertNull(GemRules.commonReason(Arrays.asList("held by Alice", null)), "an allowed weapon means no common refusal");
        assertNull(GemRules.commonReason(Arrays.asList(null, null)));
        assertNull(GemRules.commonReason(new ArrayList<>()));
        assertEquals("only", GemRules.commonReason(List.of("only")));
    }

    @Test
    void shuffleUsesTheSourceAndKeepsTheInputWhole() {
        List<String> in = List.of("a", "b", "c", "d");
        assertEquals(List.of("a", "b", "c", "d"), GemRules.shuffled(in, bound -> bound - 1), "always picking the last index swaps nothing");
        assertEquals(List.of("b", "c", "d", "a"), GemRules.shuffled(in, bound -> 0), "always picking the first index rotates");
        assertEquals(List.of("a", "b", "c", "d"), in, "the input is not touched");
        assertEquals(List.of("only"), GemRules.shuffled(List.of("only"), bound -> 0));
        assertTrue(GemRules.shuffled(List.of(), bound -> 0).isEmpty());
    }

    @Test
    void shuffleReachesEveryOrderAndLosesNothing() {
        List<Integer> in = List.of(1, 2, 3, 4);
        Set<List<Integer>> seen = new HashSet<>();
        java.util.Random rnd = new java.util.Random(7);                                  // a fixed seed: the same orders on every run
        for (int run = 0; run < 5000; run++) {
            List<Integer> out = GemRules.shuffled(in, rnd::nextInt);
            List<Integer> sorted = new ArrayList<>(out);
            sorted.sort(null);
            assertEquals(in, sorted, "no entry lost or doubled");
            seen.add(out);
        }
        assertEquals(24, seen.size(), "every one of the 24 orders of four entries can come out");
    }

    @Test
    void everyDenialHasAKey() {
        Set<String> keys = new HashSet<>();
        for (GemRules.Denial d : GemRules.Denial.values()) assertTrue(keys.add(d.key()), "duplicate key " + d.key());
    }
}
