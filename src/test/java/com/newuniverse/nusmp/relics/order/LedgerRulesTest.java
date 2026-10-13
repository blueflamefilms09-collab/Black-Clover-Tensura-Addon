package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.relics.order.LedgerRules.Action;
import com.newuniverse.nusmp.relics.order.LedgerRules.Known;
import com.newuniverse.nusmp.relics.order.LedgerRules.Mode;
import com.newuniverse.nusmp.relics.order.LedgerRules.Verdict;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The decision table of the Order's ledger: canonical, adopted, revived, duplicate, and what the census does with a duplicate. */
class LedgerRulesTest {
    private static final UUID S = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void aConsumableOrAnythingThatIsNoRelicIsIgnored() {
        for (Known k : new Known[]{Known.NONE, Known.live(S), Known.lost(S)})
            for (UUID s : new UUID[]{null, S, OTHER})
                assertEquals(Verdict.IGNORE, LedgerRules.judge(false, k, s, false));
    }

    @Test
    void aRelicTheLedgerNeverHeardOfIsAdoptedWhateverItCarries() {
        assertEquals(Verdict.ADOPT, LedgerRules.judge(true, Known.NONE, null, false), "a grandfathered or creative first copy");
        assertEquals(Verdict.ADOPT, LedgerRules.judge(true, Known.NONE, S, false), "a serial from nowhere: the ledger takes it over");
        assertEquals(Verdict.ADOPT, LedgerRules.judge(true, null, null, false), "no ledger data at all behaves like none");
    }

    @Test
    void theLiveCopyIsCanonicalAndEverythingElseIsADuplicate() {
        Known live = Known.live(S);
        assertEquals(Verdict.CANONICAL, LedgerRules.judge(true, live, S, false));
        assertEquals(Verdict.DUPLICATE, LedgerRules.judge(true, live, null, false), "a stack without a serial while a copy lives");
        assertEquals(Verdict.DUPLICATE, LedgerRules.judge(true, live, OTHER, false), "a stack with another serial");
    }

    @Test
    void aStackCopiedWithItsSerialIsCaughtWhenBothAreMetInOnePass() {
        Known live = Known.live(S);
        assertEquals(Verdict.CANONICAL, LedgerRules.judge(true, live, S, false), "the first one met is kept");
        assertEquals(Verdict.DUPLICATE, LedgerRules.judge(true, live, S, true), "the second one is the copy");
    }

    @Test
    void aLostRelicComesBackOrIsReplaced() {
        Known lost = Known.lost(S);
        assertEquals(Verdict.REVIVE, LedgerRules.judge(true, lost, S, false), "the copy thought destroyed was found");
        assertEquals(Verdict.ADOPT, LedgerRules.judge(true, lost, null, false), "nothing lives: the new copy is the copy");
        assertEquals(Verdict.ADOPT, LedgerRules.judge(true, lost, OTHER, false));
        assertEquals(Verdict.DUPLICATE, LedgerRules.judge(true, lost, S, true), "found twice at once: only one revives");
    }

    @Test
    void theCensusOnlyReportsDuplicatesAndTheEnforcementActsOnThem() {
        assertEquals(Action.WARN, LedgerRules.action(Verdict.DUPLICATE, Mode.CENSUS));
        assertEquals(Action.ORDER, LedgerRules.action(Verdict.DUPLICATE, Mode.ENFORCE));
        for (Mode m : Mode.values()) {
            assertEquals(Action.NONE, LedgerRules.action(Verdict.IGNORE, m));
            assertEquals(Action.TOUCH, LedgerRules.action(Verdict.CANONICAL, m));
            assertEquals(Action.ADOPT, LedgerRules.action(Verdict.ADOPT, m));
            assertEquals(Action.REVIVE, LedgerRules.action(Verdict.REVIVE, m));
        }
    }

    @Test
    void theCensusRunsTenMinutesFromItsStartAndAnAdminCanOverrideIt() {
        long end = 1000 + LedgerRules.CENSUS_TICKS;
        assertEquals(12_000, LedgerRules.CENSUS_TICKS, "ten minutes");
        assertEquals(Mode.CENSUS, LedgerRules.modeAt(null, end, 1000));
        assertEquals(Mode.CENSUS, LedgerRules.modeAt(null, end, end - 1));
        assertEquals(Mode.ENFORCE, LedgerRules.modeAt(null, end, end));
        assertEquals(Mode.ENFORCE, LedgerRules.modeAt(null, -1, 5), "no census was ever started");
        assertEquals(Mode.ENFORCE, LedgerRules.modeAt(Mode.ENFORCE, end, 1000), "an admin ended it early");
        assertEquals(Mode.CENSUS, LedgerRules.modeAt(Mode.CENSUS, end, end + 99999), "an admin extended it");
    }

    @Test
    void onlyAnExistingWorldNeedsACensus() {
        assertFalse(LedgerRules.needsCensus(0));
        assertFalse(LedgerRules.needsCensus(LedgerRules.EXISTING_WORLD_TICKS));
        assertTrue(LedgerRules.needsCensus(LedgerRules.EXISTING_WORLD_TICKS + 1));
        assertTrue(LedgerRules.needsCensus(24_000L * 40));
    }

    @Test
    void minutesLeftRoundUpAndEndAtZero() {
        assertEquals(10, LedgerRules.censusMinutesLeft(12_000, 0));
        assertEquals(1, LedgerRules.censusMinutesLeft(12_000, 11_999));
        assertEquals(0, LedgerRules.censusMinutesLeft(12_000, 12_000));
        assertEquals(0, LedgerRules.censusMinutesLeft(-1, 5));
    }

    @Test
    void aCopyIsOnlyDeclaredLostWhenItsOnlinePlayerShouldHaveSeenIt() {
        long old = LedgerRules.STALE_REAL_MILLIS + 1, fresh = 1000;
        assertTrue(LedgerRules.staleLost(true, true, true, old));
        assertFalse(LedgerRules.staleLost(true, true, true, fresh), "seen lately");
        assertFalse(LedgerRules.staleLost(true, true, false, old), "the holder is offline: untouched");
        assertFalse(LedgerRules.staleLost(true, false, true, old), "in a chest, on the ground or on a mob: untouched");
        assertFalse(LedgerRules.staleLost(false, true, true, old), "already lost");
        assertEquals(14L * 24 * 3600 * 1000, LedgerRules.STALE_REAL_MILLIS);
    }

    @Test
    void aCopyThatWasIssuedAndNeverSeenIsGivenUpAfterAnHour() {
        assertTrue(LedgerRules.unseenExpired(true, false, LedgerRules.UNSEEN_GRACE_TICKS + 1));
        assertFalse(LedgerRules.unseenExpired(true, false, LedgerRules.UNSEEN_GRACE_TICKS));
        assertFalse(LedgerRules.unseenExpired(true, true, 10_000_000L), "it was seen once: only the stale rule can touch it");
        assertFalse(LedgerRules.unseenExpired(false, false, 10_000_000L));
    }

    @Test
    void theVisitCooldownIsFiveMinutesPerRelic() {
        assertTrue(LedgerRules.visitAllowed(-1, 0), "never visited");
        assertFalse(LedgerRules.visitAllowed(100, 100 + LedgerRules.VISIT_COOLDOWN_TICKS - 1));
        assertTrue(LedgerRules.visitAllowed(100, 100 + LedgerRules.VISIT_COOLDOWN_TICKS));
        assertTrue(LedgerRules.visitAllowed(5000, 10), "the clock went backwards (a new world time): never stuck");
        assertEquals(6_000, LedgerRules.VISIT_COOLDOWN_TICKS);
    }

    @Test
    void theSeenStampIsRewrittenOnAMoveOrAfterAWhile() {
        assertTrue(LedgerRules.rewriteSeen(true, 100, 101));
        assertFalse(LedgerRules.rewriteSeen(false, 100, 101));
        assertTrue(LedgerRules.rewriteSeen(false, 100, 100 + LedgerRules.TOUCH_EVERY_TICKS));
        assertTrue(LedgerRules.rewriteSeen(false, 100, 50), "the clock went backwards");
    }

    @Test
    void aCopyAdoptedInAPassIsCanonicalForTheNextStackOfThePass() {
        // pass: [unserialized, unserialized]: the first is adopted (ledger now LIVE with a new serial), the second is a duplicate
        assertEquals(Verdict.ADOPT, LedgerRules.judge(true, Known.NONE, null, false));
        UUID fresh = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        assertEquals(Verdict.DUPLICATE, LedgerRules.judge(true, Known.live(fresh), null, false));
    }
}
