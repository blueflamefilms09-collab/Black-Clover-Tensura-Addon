package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the skeletal dragon's grip (owner notes): at most 2 s, one victim per caster, a cooldown of at least 10 s, never a boss. */
class BelialGripRulesTest {
    private static final BelialSeverRules.Kind MOB = BelialSeverRules.Kind.MOB, BOSS = BelialSeverRules.Kind.BOSS;

    private static BelialGripRules.Refusal grip(BelialSeverRules.Kind k, boolean creative, boolean vetoed, boolean exiled, boolean immune, boolean already, boolean cooldown, boolean busy, int global) {
        return BelialGripRules.canGrip(true, k, creative, vetoed, exiled, immune, already, cooldown, busy, global);
    }

    @Test
    void theHoldIsTwoSecondsAtMostAndTheCooldownAtLeastTen() {
        assertEquals(40, BelialGripRules.HOLD_TICKS);
        assertEquals(40, BelialGripRules.holdTicks(100000));
        assertEquals(1, BelialGripRules.holdTicks(-5));
        assertTrue(BelialGripRules.COOLDOWN_TICKS >= BelialGripRules.MIN_COOLDOWN_TICKS && BelialGripRules.MIN_COOLDOWN_TICKS >= 200, "the owner's floor: 10 seconds");
        assertTrue(BelialGripRules.COOLDOWN_TICKS > BelialGripRules.HOLD_TICKS + BelialGripRules.IMMUNE_TICKS, "a grip is never chained into the next");
    }

    @Test
    void nothingForbiddenIsGripped() {
        assertEquals(BelialGripRules.Refusal.OK, grip(MOB, false, false, false, false, false, false, false, 0));
        assertEquals(BelialGripRules.Refusal.DISABLED, BelialGripRules.canGrip(false, MOB, false, false, false, false, false, false, false, 0));
        assertEquals(BelialGripRules.Refusal.BOSS, grip(BOSS, false, false, false, false, false, false, false, 0));
        assertEquals(BelialGripRules.Refusal.CREATIVE, grip(MOB, true, false, false, false, false, false, false, 0));
        assertEquals(BelialGripRules.Refusal.VETO, grip(MOB, false, true, false, false, false, false, false, 0));
        assertEquals(BelialGripRules.Refusal.EXILED, grip(MOB, false, false, true, false, false, false, false, 0));
        assertEquals(BelialGripRules.Refusal.IMMUNE, grip(MOB, false, false, false, true, false, false, false, 0));
        assertEquals(BelialGripRules.Refusal.ALREADY, grip(MOB, false, false, false, false, true, false, false, 0));
        assertEquals(BelialGripRules.Refusal.NOT_READY, grip(MOB, false, false, false, false, false, true, false, 0));
        assertEquals(BelialGripRules.Refusal.CASTER_BUSY, grip(MOB, false, false, false, false, false, false, true, 0), "one victim per caster");
        assertEquals(BelialGripRules.Refusal.GLOBAL_CAP, grip(MOB, false, false, false, false, false, false, false, BelialGripRules.MAX_GLOBAL));
        assertEquals(BelialGripRules.Refusal.BOSS, grip(BOSS, true, true, true, true, true, true, true, 99), "a boss is refused first, whatever else is true");
    }

    @Test
    void aTimeFurtherAheadThanAnyWaitIsAClockThatWentBack() {
        assertTrue(BelialGripRules.running(150, 100, BelialGripRules.COOLDOWN_TICKS));
        assertFalse(BelialGripRules.running(100, 100, BelialGripRules.COOLDOWN_TICKS), "over at the very tick");
        assertFalse(BelialGripRules.running(1_000_000, 100, BelialGripRules.COOLDOWN_TICKS), "a level whose time was set back never locks a caster");
    }

    @Test
    void theTablesAreSweptAndBounded() {
        Map<UUID, Long> t = new HashMap<>();
        t.put(new UUID(0, 1), 50L);
        t.put(new UUID(0, 2), 150L);
        BelialGripRules.sweep(t, 100);
        assertEquals(1, t.size(), "the ended one is gone");
        for (int i = 0; i < BelialGripRules.MAX_TABLE + 10; i++) t.put(new UUID(1, i), 150L);
        BelialGripRules.sweep(t, 100);
        assertTrue(t.size() <= BelialGripRules.MAX_TABLE, "never more than the bound");
    }
}
