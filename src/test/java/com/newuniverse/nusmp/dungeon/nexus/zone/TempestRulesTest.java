package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.TempestRules.Situation;
import com.newuniverse.nusmp.dungeon.nexus.zone.TempestRules.Step;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The life of a fake house: it warns, it gives way, it comes back. */
class TempestRulesTest {
    private static Situation standing(boolean armed, long armedAt, long now, boolean triggered, boolean near, long lastSprung, int down) {
        return new Situation(true, armed, armedAt, now, triggered, near, true, false, Long.MIN_VALUE, lastSprung, down);
    }

    private static Situation down(long now, boolean away, boolean occupied, long downSince) {
        return new Situation(false, false, 0, now, false, false, away, occupied, downSince, Long.MIN_VALUE, 1);
    }

    @Test
    void aFakeThatNobodyIsNearJustStands() {
        assertEquals(Step.NONE, TempestRules.decide(standing(false, 0, 1000, false, false, Long.MIN_VALUE, 0)));
    }

    @Test
    void someoneComingCloseArmsItAndItWarnsBeforeItGivesWay() {
        assertEquals(Step.ARM, TempestRules.decide(standing(false, 0, 1000, true, true, Long.MIN_VALUE, 0)));
        assertEquals(Step.NONE, TempestRules.decide(standing(true, 1000, 1000, true, true, 1000, 0)), "just armed: the warning is on");
        assertEquals(Step.NONE, TempestRules.decide(standing(true, 1000, 1000 + TempestRules.TELEGRAPH - 1, true, true, 1000, 0)));
        assertEquals(Step.DISSOLVE, TempestRules.decide(standing(true, 1000, 1000 + TempestRules.TELEGRAPH, true, true, 1000, 0)), "still there when the warning ends");
    }

    @Test
    void stepAwayInTimeAndNothingHappens() {
        assertEquals(Step.DISARM, TempestRules.decide(standing(true, 1000, 1010, false, false, 1000, 0)));
        assertEquals(Step.DISARM, TempestRules.decide(standing(true, 1000, 1000 + TempestRules.TELEGRAPH + 50, false, false, 1000, 0)), "even late: nobody is near, so no ambush");
    }

    @Test
    void aVillageSpringsATrapOnlyNowAndThenAndNotTooManyAtOnce() {
        assertEquals(Step.NONE, TempestRules.decide(standing(false, 0, 1000, true, true, 1000 - TempestRules.COOLDOWN + 1, 0)), "too soon after the last trap");
        assertEquals(Step.ARM, TempestRules.decide(standing(false, 0, 1000, true, true, 1000 - TempestRules.COOLDOWN, 0)));
        assertEquals(Step.NONE, TempestRules.decide(standing(false, 0, 1000, true, true, Long.MIN_VALUE, TempestRules.MAX_DOWN)), "enough are down already");
        assertEquals(Step.ARM, TempestRules.decide(standing(false, 0, 1000, true, true, Long.MIN_VALUE, TempestRules.MAX_DOWN - 1)));
    }

    @Test
    void aFakeThatIsDownFormsAgainOnlyWhenNobodyIsAboutAndItHasBeenDownAWhile() {
        assertEquals(Step.NONE, TempestRules.decide(down(1000, true, false, 1000 - TempestRules.DOWN_FOR + 1)), "too soon");
        assertEquals(Step.REBUILD, TempestRules.decide(down(1000, true, false, 1000 - TempestRules.DOWN_FOR)));
        assertEquals(Step.NONE, TempestRules.decide(down(5000, false, false, 0)), "someone is about");
        assertEquals(Step.NONE, TempestRules.decide(down(5000, true, true, 0)), "something stands where it would form");
        assertEquals(Step.REBUILD, TempestRules.decide(down(5000, true, false, Long.MIN_VALUE)), "after a restart nobody knows when it fell: it was long ago");
    }

    @Test
    void aClockThatWentBackDoesNotLockAFakeUp() {
        assertEquals(Step.NONE, TempestRules.decide(down(10, true, false, 100_000)), "waits");
        assertEquals(Step.REBUILD, TempestRules.decide(down(100_000 + TempestRules.DOWN_FOR, true, false, 100_000)));
    }

    @Test
    void theNumbersAreSane() {
        assertTrue(TempestRules.TRIGGER < TempestRules.DISSOLVE_RANGE, "you can step out of the trigger and still be near: the warning is the way out");
        assertTrue(TempestRules.DISSOLVE_RANGE < TempestRules.REBUILD_RANGE);
        assertTrue(TempestRules.TELEGRAPH >= 20, "a second at least");
        assertTrue(TempestRules.BIOMASS > 300f && TempestRules.BIOMASS <= 400f);
        assertTrue(TempestRules.MAGICULE <= 1000f);
    }
}
