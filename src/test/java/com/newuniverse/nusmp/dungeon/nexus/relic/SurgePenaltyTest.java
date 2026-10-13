package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The day a Wild Magic Surge costs a tenth of the mana pool for: the expiry kept in the player's data (docs/living_dungeon.md section 7). */
class SurgePenaltyTest {
    @Test
    void itLastsAWholeDay() {
        assertEquals(24000, SurgePenalty.TICKS);
        assertEquals(WildSurge.PENALTY_TICKS, SurgePenalty.TICKS);
        assertEquals(1000L + 24000L, SurgePenalty.until(1000L));
    }

    @Test
    void itIsOnUntilTheStampAndOffFromThen() {
        long now = 50_000L, until = SurgePenalty.until(now);
        assertTrue(SurgePenalty.active(now, until));
        assertTrue(SurgePenalty.active(now + 1, until));
        assertTrue(SurgePenalty.active(until - 1, until), "the last tick of the day");
        assertFalse(SurgePenalty.active(until, until), "the moment the stamp is reached it is over");
        assertFalse(SurgePenalty.active(until + 5000, until));
    }

    @Test
    void noStampMeansNoPenalty() {
        assertFalse(SurgePenalty.active(0L, 0L));
        assertFalse(SurgePenalty.active(123_456L, 0L), "a player who never backfired");
        assertEquals(0L, SurgePenalty.remaining(123_456L, 0L));
        assertEquals(0, SurgePenalty.minutesLeft(123_456L, 0L));
    }

    @Test
    void aStampFromAClockThatRanLaterCannotHoldItForMoreThanADay() {
        long now = 1_000L;
        assertTrue(SurgePenalty.active(now, now + SurgePenalty.TICKS), "exactly a day ahead is real");
        assertFalse(SurgePenalty.active(now, now + SurgePenalty.TICKS + 1), "a day and a tick ahead is not");
        assertFalse(SurgePenalty.active(now, 9_999_999_999L), "a stamp from a world that had run for years");
        assertFalse(SurgePenalty.active(now, Long.MAX_VALUE));
        assertEquals(0L, SurgePenalty.remaining(now, Long.MAX_VALUE));
    }

    @Test
    void aRelogAndARestartKeepTheSameExpiry() {
        long start = 777_000L, until = SurgePenalty.until(start);
        long afterRelog = start + 3 * 1200;                                                             // three minutes later
        assertTrue(SurgePenalty.active(afterRelog, until));
        assertEquals(24000 - 3600, SurgePenalty.remaining(afterRelog, until));
        long nextMorning = start + 24000;
        assertFalse(SurgePenalty.active(nextMorning, until));
    }

    @Test
    void theMessageCountsWholeMinutesRoundedUp() {
        long now = 0L, until = 24000L;
        assertEquals(20, SurgePenalty.minutesLeft(now, until));
        assertEquals(20, SurgePenalty.minutesLeft(now + 1, until), "a tick in: still 19 min 59 s, shown as 20");
        assertEquals(1, SurgePenalty.minutesLeft(until - 1, until));
        assertEquals(1, SurgePenalty.minutesLeft(until - 1200, until));
        assertEquals(2, SurgePenalty.minutesLeft(until - 1201, until));
        assertEquals(0, SurgePenalty.minutesLeft(until, until));
    }
}
