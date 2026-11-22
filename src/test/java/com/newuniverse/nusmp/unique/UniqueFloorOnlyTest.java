package com.newuniverse.nusmp.unique;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 4.103.2: with floorOnly a unique that is merely not seen (a backpack, a storage mod) is never gone; only a destroyed floor drop is. */
class UniqueFloorOnlyTest {
    private static UniqueRecord missing(boolean confirmed) {
        UniqueRecord r = new UniqueRecord("t:x");
        r.state = UniqueState.HELD;
        r.missingSince = 0L;
        r.confirmed = confirmed;
        return r;
    }

    @Test void anUnaccountedCopyNeverGoesBackWhenFloorOnly() {
        long grace = UniqueRules.graceTicks(300);
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(missing(false), grace * 100, grace, true));
        assertEquals(UniqueRules.Verdict.RELEASE, UniqueRules.verdict(missing(false), grace, grace, false));   // the old clock is still there
    }

    @Test void aDestroyedFloorDropStillGoesBackAfterFiveSeconds() {
        long grace = UniqueRules.graceTicks(300);
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(missing(true), UniqueRules.CONFIRM_TICKS - 1, grace, true));
        assertEquals(UniqueRules.Verdict.RELEASE, UniqueRules.verdict(missing(true), UniqueRules.CONFIRM_TICKS, grace, true));
    }

    @Test void nothingMissingOrNullIsNone() {
        UniqueRecord ok = new UniqueRecord("t:y");
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(ok, 99999, 6000, true));
        assertEquals(UniqueRules.Verdict.NONE, UniqueRules.verdict(null, 99999, 6000, true));
    }
}
