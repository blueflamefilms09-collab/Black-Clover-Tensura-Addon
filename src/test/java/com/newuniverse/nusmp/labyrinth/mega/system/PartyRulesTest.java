package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a party shares its kills, and the rule of who is credited decides whether a floor quest is a team effort or a free ride. The pure rule is pinned here: the
 * killer is always credited and first, a member on the same floor counts, one too far away, dead or in another place does not, and the group of a player is the
 * players with the same sector (none for a player in a gap, so a lone player never gets a stranger's credit). A tiny stand-in class replaces the game's player.
 */
class PartyRulesTest {
    private record P(String name, int sector, double distSq, boolean alive) {
        @Override public String toString() { return name; }
    }

    private static P p(String name, int sector, double dist, boolean alive) { return new P(name, sector, dist * dist, alive); }

    @Test void theGroupIsThePlayerAndTheOnesInTheSameSector() {
        P me = p("me", 4, 0, true), mate = p("mate", 4, 10, true), other = p("other", 5, 10, true), gap = p("gap", -1, 10, true);
        List<P> group = PartyRules.sameGroup(me, List.of(other, mate, me, gap), x -> x.sector() < 0 ? null : x.sector());
        assertEquals(List.of(me, mate), group, "self first, once; a player of another sector or in a gap is not in it");
    }

    @Test void aPlayerInAGapIsAloneEvenIfOthersAreInGaps() {
        P me = p("me", -1, 0, true), also = p("also", -1, 3, true);
        assertEquals(List.of(me), PartyRules.sameGroup(me, List.of(also), x -> x.sector() < 0 ? null : x.sector()));
    }

    @Test void theKillerIsAlwaysCreditedFirst() {
        P me = p("me", 1, 0, false);
        assertEquals(List.of(me), PartyRules.credit(me, List.of(), x -> x.alive(), P::distSq, 64), "even a killer who is no longer 'eligible' keeps their own kill");
        P mate = p("mate", 1, 5, true);
        assertEquals(List.of(me, mate), PartyRules.credit(me, List.of(mate, me), x -> x.alive(), P::distSq, 64));
    }

    @Test void aMemberTooFarOrDeadIsNotCredited() {
        P me = p("me", 1, 0, true), near = p("near", 1, 63, true), edge = p("edge", 1, 64, true), far = p("far", 1, 65, true), dead = p("dead", 1, 3, false);
        List<P> credited = PartyRules.credit(me, List.of(near, edge, far, dead), x -> x.alive(), P::distSq, 64);
        assertEquals(List.of(me, near, edge), credited, "64 blocks exactly is still inside; 65 is out; the dead are skipped");
    }

    @Test void aRadiusOfZeroMeansTheWholeFloor() {
        P me = p("me", 1, 0, true), far = p("far", 1, 5000, true);
        assertEquals(List.of(me, far), PartyRules.credit(me, List.of(far), x -> true, P::distSq, 0));
        assertEquals(List.of(me, far), PartyRules.credit(me, List.of(far), x -> true, P::distSq, -5));
    }

    @Test void nobodyIsCreditedTwiceWhateverTheMemberListHolds() {
        P me = p("me", 1, 0, true), mate = p("mate", 1, 2, true);
        List<P> dup = new ArrayList<>(List.of(mate, me, mate, me, mate));
        assertEquals(List.of(me, mate), PartyRules.credit(me, dup, x -> true, P::distSq, 64));
        assertEquals(List.of(me, mate), PartyRules.sameGroup(me, dup, x -> Map.of()));
    }
}
