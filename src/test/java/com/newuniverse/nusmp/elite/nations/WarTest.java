package com.newuniverse.nusmp.elite.nations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.elite.nations.war.AllianceBook;
import com.newuniverse.nusmp.elite.nations.war.PvpRules;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.war.WarBook;
import com.newuniverse.nusmp.elite.nations.war.WarPhase;
import com.newuniverse.nusmp.elite.nations.war.WarRules;
import java.util.List;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

/** Why: the war state machine, the score handler, the alliance rules and the PvP protection are the rules players fight over; every transition and refusal is covered. */
class WarTest {
    private static final WarRules.Timings T = new WarRules.Timings(100, 200, 1000, 300, 50);

    private static War newWar() { return WarRules.declare(1, "red", "blue", 0, T, "border dispute"); }

    private static WarRules.DeclareContext ctx() { return new WarRules.DeclareContext(true, false, false, false, 0, 3, 2, 1, false); }

    @Test
    void phasesRunByTheClock() {
        War w = newWar();
        assertEquals(WarPhase.DECLARED, w.phase);
        assertTrue(WarRules.advance(w, 99, T).isEmpty());
        assertEquals(List.of(WarRules.Transition.TO_PREPARATION), WarRules.advance(w, 100, T));
        assertEquals(WarPhase.PREPARATION, w.phase);
        assertTrue(w.hostile());
        assertFalse(w.fighting());
        assertEquals(List.of(WarRules.Transition.TO_ACTIVE), WarRules.advance(w, 300, T));
        assertTrue(w.fighting());
        assertEquals(1300L, w.phaseEnds);
        assertTrue(WarRules.advance(w, 1299, T).isEmpty());
        assertEquals(List.of(WarRules.Transition.ACTIVE_ENDED), WarRules.advance(w, 1300, T));
        assertEquals(WarPhase.CEASEFIRE, w.phase);
        assertEquals(War.Outcome.DRAW, w.outcome, "0:0 at the end is a draw");
        assertFalse(w.hostile());
        assertEquals(List.of(WarRules.Transition.TO_ENDED), WarRules.advance(w, 1600, T));
        assertEquals(WarPhase.ENDED, w.phase);
        assertTrue(WarRules.advance(w, 9999, T).isEmpty());
    }

    @Test
    void aLongGapRunsSeveralPhasesAtOnce() {
        War w = newWar();
        var tr = WarRules.advance(w, 10_000, T);
        assertEquals(List.of(WarRules.Transition.TO_PREPARATION, WarRules.Transition.TO_ACTIVE, WarRules.Transition.ACTIVE_ENDED), tr);
        assertEquals(WarPhase.CEASEFIRE, w.phase, "the truce window starts when the fight ended, not in the past");
        assertEquals(10_300L, w.phaseEnds);
        assertEquals(List.of(WarRules.Transition.TO_ENDED), WarRules.advance(w, 10_300, T));
        assertEquals(WarPhase.ENDED, w.phase);
    }

    @Test
    void targetScoreEndsTheWarEarlyWithAWinner() {
        War w = newWar();
        WarRules.advance(w, 300, T);
        assertEquals(30, WarRules.scoreKill(w, "red", "v1", 310, 30, 600));
        assertTrue(WarRules.advance(w, 320, T).isEmpty());
        WarRules.scoreKill(w, "red", "v2", 330, 30, 600);
        assertEquals(60, w.scoreAttacker);
        assertEquals(List.of(WarRules.Transition.ACTIVE_ENDED), WarRules.advance(w, 331, T));
        assertEquals(War.Outcome.VICTORY, w.outcome);
        assertEquals("red", w.winner);
        assertEquals(WarPhase.CEASEFIRE, w.phase);
        assertEquals(631L, w.phaseEnds);
    }

    @Test
    void timeoutHigherScoreWins() {
        War w = newWar();
        WarRules.advance(w, 300, T);
        WarRules.scoreKill(w, "blue", "v1", 400, 10, 600);
        WarRules.advance(w, 1300, T);
        assertEquals("blue", w.winner);
    }

    @Test
    void killScoringOnlyWhenActiveAndNotFarmed() {
        War w = newWar();
        assertEquals(0, WarRules.scoreKill(w, "red", "v1", 5, 10, 600), "declared: nothing counts");
        WarRules.advance(w, 300, T);
        assertEquals(10, WarRules.scoreKill(w, "red", "v1", 400, 10, 600));
        assertEquals(0, WarRules.scoreKill(w, "red", "v1", 500, 10, 600), "same victim inside the cooldown");
        assertEquals(10, WarRules.scoreKill(w, "red", "v1", 1000, 10, 600));
        assertEquals(10, WarRules.scoreKill(w, "blue", "r1", 1000, 10, 600));
        assertEquals(0, WarRules.scoreKill(w, "green", "x", 1000, 10, 600), "a third nation scores nothing");
        assertEquals(0, WarRules.scoreKill(w, "red", null, 1000, 10, 600));
        assertEquals(20, w.scoreAttacker);
        assertEquals(2, w.killsAttacker);
        assertEquals(1, w.killsDefender);
    }

    @Test
    void siegeHoldAndCapture() {
        War w = newWar();
        WarRules.advance(w, 300, T);
        var r1 = WarRules.scoreSiege(w, true, 3, 1, 1, 20, 3);
        assertEquals(1, r1.points());
        assertFalse(r1.captured());
        WarRules.scoreSiege(w, true, 3, 1, 1, 20, 3);
        var r3 = WarRules.scoreSiege(w, true, 3, 1, 1, 20, 3);
        assertTrue(r3.captured());
        assertEquals(21, r3.points());
        assertEquals(23, w.scoreAttacker);
        assertEquals(1, w.capturesAttacker);
        assertEquals(0, w.siegeAttacker);
        // defenders at home hold: no progress, counter falls
        WarRules.scoreSiege(w, true, 3, 1, 1, 20, 3);
        assertEquals(1, w.siegeAttacker);
        var held = WarRules.scoreSiege(w, true, 1, 1, 1, 20, 3);
        assertEquals(0, held.points());
        assertEquals(0, w.siegeAttacker);
        assertEquals(0, WarRules.scoreSiege(w, false, 0, 5, 1, 20, 3).points());
        assertEquals(1, WarRules.scoreSiege(w, false, 2, 1, 1, 20, 3).points());
        assertEquals(1, w.scoreDefender);
        War cold = newWar();
        assertEquals(0, WarRules.scoreSiege(cold, true, 9, 0, 1, 20, 3).points(), "not active");
    }

    @Test
    void endingByAgreement() {
        War w = newWar();
        assertFalse(WarRules.proposeCeasefire(w, "red"), "only while active");
        WarRules.advance(w, 300, T);
        assertTrue(WarRules.proposeCeasefire(w, "red"));
        assertFalse(WarRules.proposeCeasefire(w, "blue"), "one pending proposal");
        assertFalse(WarRules.acceptCeasefire(w, "red", 400, T), "cannot accept your own");
        assertTrue(WarRules.declineCeasefire(w, "blue"));
        assertTrue(WarRules.proposeCeasefire(w, "blue"));
        assertTrue(WarRules.acceptCeasefire(w, "red", 400, T));
        assertEquals(War.Outcome.CEASEFIRE, w.outcome);
        assertEquals("", w.winner);
        assertEquals(WarPhase.CEASEFIRE, w.phase);
        assertEquals(700L, w.phaseEnds);
    }

    @Test
    void surrenderGivesTheOtherSideTheWin() {
        War w = newWar();
        WarRules.advance(w, 300, T);
        assertFalse(WarRules.surrender(w, "green", 400, T));
        assertTrue(WarRules.surrender(w, "red", 400, T));
        assertEquals("blue", w.winner);
        assertEquals(War.Outcome.SURRENDER, w.outcome);
    }

    @Test
    void cancelOnlyByAttackerBeforeFighting() {
        War w = newWar();
        assertFalse(WarRules.cancel(w, "blue"));
        assertTrue(WarRules.cancel(w, "red"));
        assertEquals(WarPhase.ENDED, w.phase);
        assertEquals(War.Outcome.CANCELLED, w.outcome);
        War active = newWar();
        WarRules.advance(active, 300, T);
        assertFalse(WarRules.cancel(active, "red"));
    }

    @Test
    void forceEndsWithNoWinner() {
        War w = newWar();
        WarRules.advance(w, 300, T);
        WarRules.force(w, 500, T);
        assertEquals(War.Outcome.FORCED, w.outcome);
        assertEquals(WarPhase.CEASEFIRE, w.phase);
        assertEquals("", w.winner);
    }

    @Test
    void declareRefusals() {
        assertEquals(WarRules.Refusal.NONE, WarRules.canDeclare("red", "blue", ctx()));
        assertEquals(WarRules.Refusal.SELF, WarRules.canDeclare("red", "red", ctx()));
        assertEquals(WarRules.Refusal.UNKNOWN_NATION, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(false, false, false, false, 0, 3, 2, 1, false)));
        assertEquals(WarRules.Refusal.ALLIED, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(true, true, false, false, 0, 3, 2, 1, false)));
        assertEquals(WarRules.Refusal.ALREADY_AT_WAR, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(true, false, true, false, 0, 3, 2, 1, false)));
        assertEquals(WarRules.Refusal.TRUCE, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(true, false, false, true, 0, 3, 2, 1, false)));
        assertEquals(WarRules.Refusal.LEVEL_TOO_LOW, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(true, false, false, false, 0, 3, 1, 3, false)));
        assertEquals(WarRules.Refusal.DECLARER_COOLDOWN, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(true, false, false, false, 0, 3, 2, 1, true)));
        assertEquals(WarRules.Refusal.TOO_MANY_WARS, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(true, false, false, false, 3, 3, 2, 1, false)));
        assertEquals(WarRules.Refusal.NONE, WarRules.canDeclare("red", "blue", new WarRules.DeclareContext(true, false, false, false, 30, 0, 2, 1, false)), "0 = unlimited");
    }

    @Test
    void spoils() {
        assertEquals(100L, WarRules.spoilsCoins(1000, 10, 5000));
        assertEquals(50L, WarRules.spoilsCoins(1000, 10, 50));
        assertEquals(0L, WarRules.spoilsCoins(0, 10, 50));
        assertEquals(0L, WarRules.spoilsCoins(1000, 0, 50));
        assertEquals(1000L, WarRules.spoilsCoins(1000, 500, 0), "percent is clamped to 100, 0 cap = none");
    }

    @Test
    void warBookArchiveHistoryAndTruce() {
        WarBook b = new WarBook(2);
        for (int i = 0; i < 3; i++) {
            War w = b.add(WarRules.declare(b.newId(), "a" + i, "b" + i, 0, T, ""));
            WarRules.advance(w, 10_000, T);
            assertSame(w, b.find("a" + i, "b" + i));
            b.archive(w, 10_000, 500);
        }
        assertEquals(2, b.history.size(), "history is capped");
        assertEquals(2L, b.history.get(0).id());
        assertNull(b.find("a0", "b0"));
        assertTrue(b.truceBetween("b2", "a2", 10_100));
        assertFalse(b.truceBetween("a2", "b2", 10_500));
        assertEquals(1, b.historyOf("a2", 10).size());
        assertEquals(2, b.historyOf(null, 10).size());
        assertEquals(1, b.historyOf(null, 1).size());
    }

    @Test
    void warBookQueries() {
        WarBook b = new WarBook(10);
        War w = b.add(WarRules.declare(b.newId(), "red", "blue", 0, T, ""));
        assertNotNull(b.hostileBetween("blue", "red"));
        assertNull(b.fightingBetween("blue", "red"));
        assertEquals(1, b.openWarCount("red"));
        WarRules.advance(w, 300, T);
        assertNotNull(b.fightingBetween("red", "blue"));
        assertEquals(1, b.warsOf("blue").size());
        assertEquals(1, b.removeNation("blue").size());
        assertNull(b.find("red", "blue"));
        b.declareCooldown.put("red", 50L);
        assertTrue(b.declarerCoolingDown("red", 49));
        assertFalse(b.declarerCoolingDown("red", 50));
    }

    // ---------------------------------------------------------------- alliances
    private static final BiPredicate<String, String> NO_WAR = (a, b) -> false;

    @Test
    void allianceProposeAcceptBreak() {
        AllianceBook ab = new AllianceBook();
        assertEquals(AllianceBook.Result.SELF, ab.propose("a", "a", 0, 100, 3, NO_WAR));
        assertEquals(AllianceBook.Result.OK, ab.propose("a", "b", 0, 100, 3, NO_WAR));
        assertEquals(AllianceBook.Result.DUPLICATE, ab.propose("a", "b", 1, 100, 3, NO_WAR));
        assertEquals(List.of("a"), ab.proposalsTo("b"));
        assertEquals(List.of("b"), ab.proposalsFrom("a"));
        assertFalse(ab.areAllied("a", "b"));
        assertEquals(AllianceBook.Result.NO_PROPOSAL, ab.accept("a", "b", 2, 3, NO_WAR), "the proposer cannot accept their own");
        assertEquals(AllianceBook.Result.OK, ab.accept("b", "a", 2, 3, NO_WAR));
        assertTrue(ab.areAllied("b", "a"));
        assertEquals(AllianceBook.Result.ALREADY_ALLIED, ab.propose("b", "a", 3, 100, 3, NO_WAR));
        assertEquals(AllianceBook.Result.OK, ab.breakAlliance("a", "b", 10, 500));
        assertFalse(ab.areAllied("a", "b"));
        assertEquals(AllianceBook.Result.NOT_ALLIED, ab.breakAlliance("a", "b", 10, 500));
        assertEquals(AllianceBook.Result.COOLDOWN, ab.propose("a", "b", 100, 100, 3, NO_WAR));
        assertEquals(AllianceBook.Result.OK, ab.propose("a", "b", 600, 100, 3, NO_WAR));
    }

    @Test
    void allianceProposalExpires() {
        AllianceBook ab = new AllianceBook();
        ab.propose("a", "b", 0, 100, 3, NO_WAR);
        assertEquals(AllianceBook.Result.EXPIRED, ab.accept("b", "a", 100, 3, NO_WAR));
        ab.propose("a", "c", 0, 50, 3, NO_WAR);
        assertEquals(1, ab.expire(60));
        assertTrue(ab.proposals.isEmpty());
    }

    @Test
    void counterProposalAcceptsAtOnce() {
        AllianceBook ab = new AllianceBook();
        ab.propose("a", "b", 0, 100, 3, NO_WAR);
        assertEquals(AllianceBook.Result.OK, ab.propose("b", "a", 5, 100, 3, NO_WAR));
        assertTrue(ab.areAllied("a", "b"));
        assertTrue(ab.proposals.isEmpty());
    }

    @Test
    void allianceLimitsAndWarConflicts() {
        AllianceBook ab = new AllianceBook();
        ab.propose("a", "b", 0, 100, 1, NO_WAR);
        ab.accept("b", "a", 0, 1, NO_WAR);
        assertEquals(AllianceBook.Result.LIMIT_SELF, ab.propose("a", "c", 1, 100, 1, NO_WAR));
        assertEquals(AllianceBook.Result.LIMIT_OTHER, ab.propose("c", "a", 1, 100, 1, NO_WAR));
        BiPredicate<String, String> war = (x, y) -> (x.equals("c") && y.equals("b")) || (x.equals("b") && y.equals("c"));
        AllianceBook ab2 = new AllianceBook();
        assertEquals(AllianceBook.Result.AT_WAR, ab2.propose("b", "c", 0, 100, 3, war));
        ab2.propose("a", "b", 0, 100, 3, NO_WAR);
        ab2.accept("b", "a", 0, 3, NO_WAR);
        assertEquals(AllianceBook.Result.ALLY_AT_WAR, ab2.propose("c", "a", 1, 100, 3, war), "c is at war with a's ally b");
        assertEquals(1, ab2.alliesOf("a").size());
        ab2.removeNation("b");
        assertTrue(ab2.alliesOf("a").isEmpty());
    }

    // ---------------------------------------------------------------- protection
    @Test
    void pvpVerdicts() {
        assertEquals(PvpRules.Verdict.ALLOW_VANILLA, PvpRules.check("", "red", true, false, false, true), "no nation: vanilla");
        assertEquals(PvpRules.Verdict.ALLOW_VANILLA, PvpRules.check("red", "", true, false, false, true));
        assertEquals(PvpRules.Verdict.DENY_SAME_NATION, PvpRules.check("red", "red", true, false, false, true));
        assertEquals(PvpRules.Verdict.ALLOW_VANILLA, PvpRules.check("red", "red", false, false, false, true));
        assertEquals(PvpRules.Verdict.DENY_ALLIED, PvpRules.check("red", "blue", true, true, false, true));
        assertEquals(PvpRules.Verdict.DENY_ALLIED, PvpRules.check("red", "blue", true, true, true, true), "allies never fight, even if a stale war exists");
        assertEquals(PvpRules.Verdict.ALLOW_WAR, PvpRules.check("red", "blue", true, false, true, true));
        assertEquals(PvpRules.Verdict.DENY_NOT_AT_WAR, PvpRules.check("red", "blue", true, false, false, true));
        assertEquals(PvpRules.Verdict.ALLOW_VANILLA, PvpRules.check("red", "blue", true, false, false, false));
        assertTrue(PvpRules.denies(PvpRules.Verdict.DENY_NOT_AT_WAR));
        assertFalse(PvpRules.denies(PvpRules.Verdict.ALLOW_WAR));
    }
}
