package com.newuniverse.nusmp.unique;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the Unique Guardian's state machine (lost -> back in the pool, theft is not loss, an old copy dissolves, no duplicate while a valid copy exists, the flap guard, the absence rule) without a game. */
class UniqueDataTest {
    private static final long GRACE = UniqueRules.graceTicks(300);
    private static final UUID A = UUID.randomUUID(), B = UUID.randomUUID();

    private static UniqueSpot heldBy(UUID p) { return UniqueSpot.held(p, "minecraft:overworld", 1, 64, 1); }

    @Test
    void aDestroyedCopyReturnsToThePoolAfterTheConfirmClock() {
        UniqueData d = new UniqueData();
        d.see("greed", heldBy(A), 0, 1_000);
        UniqueSpot drop = UniqueSpot.dropped(A, "minecraft:overworld", 1, 64, 1, UUID.randomUUID());
        d.see("greed", drop, 10, 2_000);
        assertTrue(d.markMissing("greed", 100, true));
        UniqueRecord r = d.get("greed");
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(r, 100 + UniqueRules.CONFIRM_TICKS - 1, GRACE), "not confirmed long enough");
        assertEquals(UniqueRules.Verdict.RELEASE, UniqueRules.verdict(r, 100 + UniqueRules.CONFIRM_TICKS, GRACE));
        assertEquals(UniqueData.Release.RELEASED, d.release("greed", 5_000, "destroyed"));
        assertEquals(UniqueState.POOL, r.state);
        assertEquals(1, r.epoch);
        assertEquals(1, r.releases);
    }

    @Test
    void aCopyThatOnlyVanishedFromAScanWaitsTheFullGrace() {
        UniqueData d = new UniqueData();
        d.see("relic:x", heldBy(A), 0, 1_000);
        assertTrue(d.markMissing("relic:x", 40, false));
        UniqueRecord r = d.get("relic:x");
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(r, 40 + GRACE - 1, GRACE));
        assertEquals(UniqueRules.Verdict.RELEASE, UniqueRules.verdict(r, 40 + GRACE, GRACE));
        assertFalse(d.markMissing("relic:x", 50, false), "the clock is not restarted by a second miss");
        assertEquals(40, r.missingSince);
    }

    @Test
    void aSightingDuringTheGraceCancelsTheLoss() {
        UniqueData d = new UniqueData();
        d.see("relic:x", heldBy(A), 0, 1_000);
        d.markMissing("relic:x", 40, false);
        d.see("relic:x", UniqueSpot.contained(A, "minecraft:overworld", 5, 60, 5, 12345L), 100, 2_000);
        UniqueRecord r = d.get("relic:x");
        assertFalse(r.missing());
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(r, 40 + GRACE * 10, GRACE));
        assertEquals(UniqueState.CONTAINED, r.state);
        assertEquals(12345L, r.block);
    }

    @Test
    void theftIsNotLoss() {
        UniqueData d = new UniqueData();
        d.see("shishou", heldBy(A), 0, 1_000);
        assertTrue(d.see("shishou", heldBy(B), 20, 2_000), "a new holder is a change worth saving");
        UniqueRecord r = d.get("shishou");
        assertEquals(B, r.holder);
        assertEquals(UniqueState.HELD, r.state);
        assertFalse(r.missing());
        assertEquals(0, r.epoch);
        assertEquals(0, r.releases);
        // the old holder's scan no longer shows it, but the record is the new holder's: the caller only marks a record missing for ITS holder, and a thief's sighting always clears the clock
        d.markMissing("shishou", 30, false);
        d.see("shishou", heldBy(B), 40, 3_000);
        assertFalse(r.missing());
    }

    @Test
    void aCopyInThePoolIsNeverReleasedTwiceAndIsNotMarkedMissing() {
        UniqueData d = new UniqueData();
        d.see("greed", heldBy(A), 0, 1_000);
        assertEquals(UniqueData.Release.RELEASED, d.release("greed", 2_000, "absent"));
        assertEquals(UniqueData.Release.UNKNOWN, d.release("greed", 3_000, "absent"), "no second release while it is in the pool");
        assertFalse(d.markMissing("greed", 10, true));
        assertFalse(d.retire("greed", "x"));
        assertEquals(1, d.get("greed").epoch);
    }

    @Test
    void aNewValidCopyAfterTheReleaseIsTrackedAgainAndTheOldEpochIsOld() {
        UniqueData d = new UniqueData();
        d.see("charter:dante", heldBy(A), 0, 1_000);
        d.release("charter:dante", 2_000, "destroyed");
        int epoch = d.epoch("charter:dante");
        assertEquals(1, epoch);
        assertTrue(UniqueRules.oldCopy(0, epoch), "a copy stamped before the release is an old copy");
        assertFalse(UniqueRules.oldCopy(1, epoch));
        assertFalse(UniqueRules.oldCopy(-1, epoch), "an unstamped copy is adopted, not dissolved");
        d.see("charter:dante", heldBy(B), 50, 9_000_000);
        assertEquals(UniqueState.HELD, d.get("charter:dante").state);
        assertEquals(B, d.holderOf("charter:dante"));
        assertEquals(1, d.epoch("charter:dante"));
    }

    @Test
    void noSecondValidCopyWhileOneIsSeen() {
        UniqueData d = new UniqueData();
        d.see("greed", heldBy(A), 0, 1_000);
        // a destroyed removal of a stale item entity cannot release a copy that was seen again meanwhile
        d.markMissing("greed", 10, true);
        d.see("greed", heldBy(A), 20, 1_500);
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(d.get("greed"), 10_000, GRACE));
        assertEquals(0, d.get("greed").releases);
    }

    @Test
    void theFlapGuardStopsAFoundAndDestroyedLoop() {
        UniqueData d = new UniqueData();
        long t = 1_000;
        for (int i = 0; i < UniqueRules.MAX_RELEASES_PER_HOUR; i++) {
            d.see("relic:x", heldBy(A), i, t);
            assertEquals(UniqueData.Release.RELEASED, d.release("relic:x", t + 10, "destroyed"));
            t += 1_000;
        }
        d.see("relic:x", heldBy(A), 99, t);
        assertEquals(UniqueData.Release.FLAPPING, d.release("relic:x", t + 10, "destroyed"));
        assertEquals(UniqueState.HELD, d.get("relic:x").state, "a refused release leaves the record alone");
        assertEquals(UniqueData.Release.RELEASED, d.release("relic:x", t + UniqueRules.HOUR_MS + 5_000, "destroyed"), "an hour later it may return again");
    }

    @Test
    void absenceOnlyCountsForAnOfflineHolderOfAHeldCopy() {
        UniqueData d = new UniqueData();
        d.see("greed", heldBy(A), 0, 1_000);
        UniqueRecord r = d.get("greed");
        long days = UniqueRules.absenceMs(21);
        assertFalse(UniqueRules.absent(r, false, 1_000 + days - 1, days));
        assertTrue(UniqueRules.absent(r, false, 1_000 + days, days));
        assertFalse(UniqueRules.absent(r, true, 1_000 + days * 2, days), "an online holder is never absent");
        assertFalse(UniqueRules.absent(r, false, 1_000 + days * 2, 0), "0 days switches the rule off");
        d.see("greed", UniqueSpot.dropped(A, "minecraft:overworld", 0, 0, 0, UUID.randomUUID()), 5, 2_000);
        assertFalse(UniqueRules.absent(r, false, 2_000 + days * 2, days), "a copy lying in the world is not held by anybody");
    }

    @Test
    void retireNotesAReturnTheOwningSystemMadeItselfWithoutTheFlapGuard() {
        UniqueData d = new UniqueData();
        d.see("shishou", heldBy(A), 0, 1_000);
        assertTrue(d.retire("shishou", "reset"));
        UniqueRecord r = d.get("shishou");
        assertEquals(UniqueState.POOL, r.state);
        assertEquals(0, r.releases);
        assertEquals(1, r.epoch);
    }

    @Test
    void aSightingMustBeAPlaceNotThePool() {
        UniqueData d = new UniqueData();
        assertThrows(IllegalArgumentException.class, () -> d.see("x", new UniqueSpot(UniqueState.POOL, null, "d", 0, 0, 0, null, UniqueRecord.NO_BLOCK), 0, 0));
        assertNull(d.get("x"));
    }

    @Test
    void theVoidLineAndTheIdShape() {
        assertTrue(UniqueRules.inVoid(-63.5, -64));
        assertFalse(UniqueRules.inVoid(10, -64));
        assertTrue(UniqueRules.validId("charter:dante"));
        assertTrue(UniqueRules.validId("dragonball:earth:3"));
        assertFalse(UniqueRules.validId("Bad Id"));
        assertFalse(UniqueRules.validId(""));
        assertFalse(UniqueRules.validId(null));
    }
}
