package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.avatar.net.RollLimiter;
import com.newuniverse.nusmp.companion.talk.TalkRules;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the talk screen's per-player rate limits are the shared {@code RollLimiter} fed with game ticks; this pins how that behaves for picks and openings: one per gap per player, players independent, forget resets. */
class CompanionLimiterTest {
    @Test
    void aPlayerMayPickOncePerGap() {
        RollLimiter picks = new RollLimiter();
        UUID a = UUID.randomUUID();
        assertTrue(picks.allow(a, 100, TalkRules.PICK_GAP_TICKS));
        assertFalse(picks.allow(a, 100, TalkRules.PICK_GAP_TICKS), "the same tick");
        assertFalse(picks.allow(a, 103, TalkRules.PICK_GAP_TICKS), "three ticks later");
        assertTrue(picks.allow(a, 104, TalkRules.PICK_GAP_TICKS), "four ticks later");
    }

    @Test
    void playersDoNotShareALimit() {
        RollLimiter picks = new RollLimiter();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(picks.allow(a, 100, TalkRules.PICK_GAP_TICKS));
        assertTrue(picks.allow(b, 100, TalkRules.PICK_GAP_TICKS));
    }

    @Test
    void forgettingAPlayerAllowsThemAtOnce() {
        RollLimiter opens = new RollLimiter();
        UUID a = UUID.randomUUID();
        assertTrue(opens.allow(a, 50, TalkRules.OPEN_GAP_TICKS));
        assertFalse(opens.allow(a, 51, TalkRules.OPEN_GAP_TICKS));
        opens.forget(a);
        assertTrue(opens.allow(a, 52, TalkRules.OPEN_GAP_TICKS));
    }

    @Test
    void aClockThatWentBackDoesNotBlockForever() {
        RollLimiter picks = new RollLimiter();
        UUID a = UUID.randomUUID();
        assertTrue(picks.allow(a, 5000, TalkRules.PICK_GAP_TICKS));
        assertTrue(picks.allow(a, 10, TalkRules.PICK_GAP_TICKS));
    }
}
