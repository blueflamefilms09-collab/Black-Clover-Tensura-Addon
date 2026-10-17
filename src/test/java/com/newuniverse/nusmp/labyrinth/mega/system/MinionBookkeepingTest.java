package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the shadows' bookkeeping (at most three per owner, the oldest released first; thirty minutes after the owner left; one sync per ten ticks; the charges
 * of the core) is where a duplicated-minion or an immortal-minion bug would hide, and none of it needs a game to be tested. These pin the cap and its
 * eviction order, the sentinel and boundary of the lifetime, the throttle's gap, and the charge arithmetic.
 */
class MinionBookkeepingTest {
    static UUID id(long n) { return new UUID(0, n); }

    static final UUID OWNER = id(100), OTHER = id(200);

    // ---------------------------------------------------------------- the roster
    @Test void aFourthShadowReleasesTheOldest() {
        MinionRoster r = new MinionRoster();
        assertEquals(List.of(), r.add(OWNER, id(1), 3));
        assertEquals(List.of(), r.add(OWNER, id(2), 3));
        assertEquals(List.of(), r.add(OWNER, id(3), 3));
        assertEquals(List.of(id(1)), r.add(OWNER, id(4), 3));
        assertEquals(List.of(id(2), id(3), id(4)), r.of(OWNER));
    }

    @Test void loweringTheCapReleasesSeveralAtOnceOldestFirst() {
        MinionRoster r = new MinionRoster();
        for (int i = 1; i <= 3; i++) r.add(OWNER, id(i), 5);
        assertEquals(List.of(id(1), id(2)), r.add(OWNER, id(4), 2));
        assertEquals(List.of(id(3), id(4)), r.of(OWNER));
    }

    @Test void aCapOfZeroReleasesTheNewShadowItself() {
        MinionRoster r = new MinionRoster();
        assertEquals(List.of(id(1)), r.add(OWNER, id(1), 0));
        assertTrue(r.isEmpty());
        assertEquals(List.of(id(2)), r.add(OWNER, id(2), -4));
    }

    @Test void addingTheSameShadowTwiceDoesNotDuplicateIt() {
        MinionRoster r = new MinionRoster();
        r.add(OWNER, id(1), 3);
        r.add(OWNER, id(1), 3);
        assertEquals(1, r.count(OWNER));
    }

    @Test void ownersAreIndependent() {
        MinionRoster r = new MinionRoster();
        for (int i = 1; i <= 3; i++) r.add(OWNER, id(i), 3);
        assertEquals(List.of(), r.add(OTHER, id(10), 3), "another owner's cap is its own");
        assertEquals(3, r.count(OWNER));
        assertEquals(1, r.count(OTHER));
        assertEquals(2, r.owners().size());
    }

    @Test void removingAShadowFreesItsPlaceAndForgetsEmptyOwners() {
        MinionRoster r = new MinionRoster();
        r.add(OWNER, id(1), 3);
        r.add(OWNER, id(2), 3);
        assertTrue(r.contains(id(2)));
        assertTrue(r.remove(id(2)));
        assertFalse(r.remove(id(2)));
        assertFalse(r.contains(id(2)));
        assertEquals(List.of(), r.add(OWNER, id(3), 2), "room was made");
        r.remove(id(1));
        r.remove(id(3));
        assertTrue(r.isEmpty());
        assertEquals(List.of(), r.owners());
    }

    @Test void clearingAnOwnerReturnsTheirShadows() {
        MinionRoster r = new MinionRoster();
        r.add(OWNER, id(1), 3);
        r.add(OWNER, id(2), 3);
        r.add(OTHER, id(9), 3);
        assertEquals(List.of(id(1), id(2)), r.clearOwner(OWNER));
        assertEquals(List.of(), r.clearOwner(OWNER));
        assertEquals(1, r.count(OTHER));
        r.clear();
        assertTrue(r.isEmpty());
    }

    // ---------------------------------------------------------------- the lifetime
    @Test void thirtyMinutesIsThirtyThousandSixHundredTicks() {
        assertEquals(36000L, MinionLife.lifetimeTicks(30));
        assertEquals(0L, MinionLife.lifetimeTicks(0));
        assertEquals(0L, MinionLife.lifetimeTicks(-5));
    }

    @Test void aShadowDiesExactlyAtTheLifetimeAfterTheOwnerLeft() {
        long life = MinionLife.lifetimeTicks(30);
        long left = MinionLife.awayStamp(5000);
        assertFalse(MinionLife.expired(left, 5000, life));
        assertFalse(MinionLife.expired(left, 5000 + life - 1, life));
        assertTrue(MinionLife.expired(left, 5000 + life, life));
        assertTrue(MinionLife.expired(left, 5000 + life + 99999, life));
    }

    @Test void zeroMeansNotAwayAndNeverExpires() {
        assertFalse(MinionLife.expired(0L, Long.MAX_VALUE / 2, 36000L));
        assertEquals(1L, MinionLife.awayStamp(0), "a stamp is never the sentinel");
        assertEquals(77L, MinionLife.awayStamp(77));
    }

    // ---------------------------------------------------------------- the sync throttle
    @Test void theFirstChangeIsSentAtOnceAndLaterOnesWaitForTheGap() {
        SyncThrottle t = new SyncThrottle(10);
        assertFalse(t.due(0), "nothing changed");
        t.markDirty();
        assertTrue(t.due(0), "the first change goes out immediately");
        t.sent(0);
        assertFalse(t.isDirty());
        t.markDirty();
        assertFalse(t.due(5));
        assertFalse(t.due(9));
        assertTrue(t.due(10));
        t.sent(10);
        assertFalse(t.due(11), "nothing changed since");
    }

    @Test void manyChangesInOneGapMakeOneSync() {
        SyncThrottle t = new SyncThrottle(10);
        t.markDirty();
        t.sent(100);
        int sends = 0;
        for (long now = 101; now < 140; now++) {
            t.markDirty();
            if (t.due(now)) {
                sends++;
                t.sent(now);
            }
        }
        assertEquals(3, sends, "ticks 110, 120 and 130 in thirty-nine ticks of changes");
    }

    // ---------------------------------------------------------------- the core's charges
    @Test void aCoreIsSpentAfterThreeExtractions() {
        int c = CoreCharges.START;
        assertEquals(3, c);
        c = CoreCharges.spend(c);
        assertFalse(CoreCharges.spent(c));
        c = CoreCharges.spend(c);
        assertFalse(CoreCharges.spent(c));
        c = CoreCharges.spend(c);
        assertTrue(CoreCharges.spent(c));
        assertEquals(0, CoreCharges.spend(c), "never below zero");
    }

    @Test void aRewardRechargesUpToTheMaximumOnly() {
        assertTrue(CoreCharges.canRecharge(0));
        assertTrue(CoreCharges.canRecharge(2));
        assertFalse(CoreCharges.canRecharge(3));
        assertFalse(CoreCharges.canRecharge(99));
        assertEquals(3, CoreCharges.recharge(2));
        assertEquals(3, CoreCharges.recharge(3));
        assertEquals(1, CoreCharges.recharge(0));
    }

    @Test void aDamagedChargeCountIsBroughtIntoRange() {
        assertEquals(0, CoreCharges.clamp(-4));
        assertEquals(3, CoreCharges.clamp(40));
        assertTrue(CoreCharges.spent(-1));
        assertEquals(2, CoreCharges.spend(40), "a core with a silly count is a full core");
    }
}
