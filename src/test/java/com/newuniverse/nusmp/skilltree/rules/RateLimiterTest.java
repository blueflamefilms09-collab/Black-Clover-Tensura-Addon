package com.newuniverse.nusmp.skilltree.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the purchase cap is the only thing between a flooding client and the server's purchase handler, so it is proved to hold at its edges (exactly the cap passes, one more does not), to
 * recover when the window slides, to be per player, and above all NOT to extend its own lock-out: refused attempts must not be recorded, otherwise a client that keeps hammering would never be
 * served again. The clock is the server tick count passed in by the caller, so the tests are deterministic.
 */
class RateLimiterTest {
    @Test
    void exactlyTheCapPassesInOneTickAndTheNextOneDoesNot() {
        RateLimiter<String> rl = new RateLimiter<>();
        for (int i = 0; i < 10; i++) assertTrue(rl.allow("a", 100, 10), "action " + (i + 1));
        assertFalse(rl.allow("a", 100, 10), "the 11th in the same tick");
        assertFalse(rl.allow("a", 100, 10));
    }

    @Test
    void theWindowSlidesAfterTwentyTicks() {
        RateLimiter<String> rl = new RateLimiter<>();
        for (int i = 0; i < 10; i++) assertTrue(rl.allow("a", 100, 10));
        assertFalse(rl.allow("a", 119, 10), "19 ticks later is still inside the second");
        assertTrue(rl.allow("a", 120, 10), "20 ticks later the first ten have aged out");
        assertTrue(rl.allow("a", 120, 10));
    }

    @Test
    void refusedAttemptsAreNotRecordedSoAFloodDoesNotExtendItsOwnLockout() {
        RateLimiter<String> rl = new RateLimiter<>();
        for (int i = 0; i < 10; i++) rl.allow("a", 0, 10);
        for (int i = 0; i < 500; i++) assertFalse(rl.allow("a", 10, 10), "hammering inside the window");
        assertTrue(rl.allow("a", 20, 10), "the ten real actions aged out at tick 20; the 500 refusals left no trace");
    }

    @Test
    void eachKeyHasItsOwnBudget() {
        RateLimiter<String> rl = new RateLimiter<>();
        for (int i = 0; i < 3; i++) assertTrue(rl.allow("a", 5, 3));
        assertFalse(rl.allow("a", 5, 3));
        assertTrue(rl.allow("b", 5, 3), "another player is not affected");
    }

    @Test
    void aSpreadOutPlayerIsNeverRefused() {
        RateLimiter<String> rl = new RateLimiter<>();
        for (int t = 0; t < 2000; t += 3) assertTrue(rl.allow("a", t, 10), "one action every 3 ticks is about 7 per second");
    }

    @Test
    void aCapBelowOneIsTreatedAsOneAndTheCapCanChangeBetweenCalls() {
        RateLimiter<String> rl = new RateLimiter<>();
        assertTrue(rl.allow("a", 0, 0));
        assertFalse(rl.allow("a", 0, 0));
        assertFalse(rl.allow("a", 0, -5));
        assertTrue(rl.allow("a", 0, 2), "raising the cap (a config reload) frees the next action at once");
        assertFalse(rl.allow("a", 0, 2));
    }

    @Test
    void aClockThatWentBackwardsDoesNotLockAPlayerOut() {
        RateLimiter<String> rl = new RateLimiter<>();
        for (int i = 0; i < 5; i++) rl.allow("a", 100000, 5);
        assertTrue(rl.allow("a", 40, 5), "the server restarted with a tick count of 40: the old stamps are from the future and are dropped");
    }

    @Test
    void forgetAndClearEmptyTheMemory() {
        RateLimiter<String> rl = new RateLimiter<>();
        rl.allow("a", 0, 5);
        rl.allow("b", 0, 5);
        assertEquals(2, rl.size());
        rl.forget("a");
        assertEquals(1, rl.size());
        rl.clear();
        assertEquals(0, rl.size());
    }
}
