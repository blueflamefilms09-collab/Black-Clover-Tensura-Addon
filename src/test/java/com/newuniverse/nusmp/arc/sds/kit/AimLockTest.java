package com.newuniverse.nusmp.arc.sds.kit;

import com.newuniverse.nusmp.arc.sds.SdsAsserts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a lane that tracks its target and then locks is the fair middle between impossible and trivial to dodge, so the lock and the fraction it locks at are tested. */
class AimLockTest {
    @Test
    void itFollowsTheTargetUntilTheLastShareOfTheWindUpAndThenLocks() {
        AimLock a = new AimLock(0.4);
        assertFalse(a.update(30, 30, 0, 1, 0, 10, 0));
        SdsAsserts.assertNear(new double[]{1, 0}, a.dir(), 1e-12);
        assertFalse(a.update(18, 30, 0, 1, 0, 0, 10));
        SdsAsserts.assertNear(new double[]{0, 1}, a.dir(), 1e-12);
        assertTrue(a.update(12, 30, 0, 1, 0, -10, 0), "12 of 30 is the last 40%: locked");
        SdsAsserts.assertNear(new double[]{-1, 0}, a.dir(), 1e-12);
        assertTrue(a.update(6, 30, 0, 1, 0, 0, -10));
        SdsAsserts.assertNear(new double[]{-1, 0}, a.dir(), 1e-12);
        assertTrue(a.locked() && a.has());
    }

    @Test
    void theOriginIsTheBossAtTheLastUnlockedTick() {
        AimLock a = new AimLock(0.4);
        a.update(30, 30, 1, 2, 3, 10, 3);
        a.update(10, 30, 4, 5, 6, 10, 6);
        SdsAsserts.assertNear(new double[]{4, 5, 6}, a.origin(), 1e-12);
        a.update(3, 30, 9, 9, 9, 0, 0);
        SdsAsserts.assertNear(new double[]{4, 5, 6}, a.origin(), 1e-12);
    }

    @Test
    void aTargetOnTopOfTheOriginKeepsTheOldDirection() {
        AimLock a = new AimLock(0.4);
        a.update(30, 30, 0, 0, 0, 0, 5);
        a.update(27, 30, 0, 0, 0, 0, 0);
        SdsAsserts.assertNear(new double[]{0, 1}, a.dir(), 1e-12);
        AimLock b = new AimLock(0.4);
        b.update(30, 30, 2, 0, 2, 2, 2);
        SdsAsserts.assertNear(new double[]{0, 1}, b.dir(), 1e-12);
    }

    @Test
    void resetStartsAFreshLane() {
        AimLock a = new AimLock(0.5);
        a.update(1, 30, 0, 0, 0, 5, 0);
        assertTrue(a.locked());
        a.reset();
        assertFalse(a.locked() || a.has());
        SdsAsserts.assertNear(new double[]{0, 1}, a.dir(), 1e-12);
        assertFalse(a.update(30, 30, 0, 0, 0, -5, 0));
        SdsAsserts.assertNear(new double[]{-1, 0}, a.dir(), 1e-12);
    }

    @Test
    void theLockShareIsClampedToZeroAndOne() {
        assertTrue(new AimLock(7).update(30, 30, 0, 0, 0, 1, 0), "a share over 1 locks at once");
        AimLock never = new AimLock(-3);
        assertFalse(never.update(1, 30, 0, 0, 0, 1, 0), "a negative share locks only at 0 ticks left");
        assertTrue(never.update(0, 30, 0, 0, 0, 1, 0));
    }
}
