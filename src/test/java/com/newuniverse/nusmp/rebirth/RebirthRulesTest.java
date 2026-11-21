package com.newuniverse.nusmp.rebirth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.newuniverse.nusmp.rebirth.RebirthRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of the two rebirth rites: checks, cooldown, confirm window, marks, the second book's magic, the fusion setting. */
class RebirthRulesTest {
    private static final long DAY = TICKS_PER_DAY;

    private static Verdict v(Rite rite, boolean on, long now, long last, int grimoires, int tier, int resolve, boolean wizard, boolean waiting) {
        return RebirthRules.verdict(rite, on, now, last, 7, grimoires, tier, 4, resolve, 3, wizard, 0, 0, waiting);
    }

    @Test
    void rebirthNeedsTheSettingAGrimoireAndNoCooldown() {
        assertEquals(Verdict.OK, v(Rite.REBIRTH, true, 10 * DAY, -1, 1, 3, RESOLVE_UNKNOWN, true, false));
        assertEquals(Verdict.DISABLED, v(Rite.REBIRTH, false, 10 * DAY, -1, 1, 3, RESOLVE_UNKNOWN, true, false));
        assertEquals(Verdict.NO_GRIMOIRE, v(Rite.REBIRTH, true, 10 * DAY, -1, 0, 0, RESOLVE_UNKNOWN, true, false));
        assertEquals(Verdict.COOLDOWN, v(Rite.REBIRTH, true, 3 * DAY, 0, 1, 3, RESOLVE_UNKNOWN, true, false));
        assertEquals(Verdict.OK, v(Rite.REBIRTH, true, 7 * DAY, 0, 1, 3, RESOLVE_UNKNOWN, true, false));
        assertEquals(Verdict.ALREADY_WAITING, v(Rite.REBIRTH, true, 10 * DAY, -1, 1, 3, RESOLVE_UNKNOWN, true, true));
        assertEquals(Verdict.OK, v(Rite.REBIRTH, true, 10 * DAY, -1, 2, 3, RESOLVE_UNKNOWN, false, false), "rebirth does not need the wizard setting");
    }

    @Test
    void secondBookNeedsFourLeavesOneBookAndResolveIfKnown() {
        assertEquals(Verdict.TIER_TOO_LOW, v(Rite.SECOND_BOOK, true, 99 * DAY, -1, 1, 3, RESOLVE_UNKNOWN, true, false));
        assertEquals(Verdict.OK, v(Rite.SECOND_BOOK, true, 99 * DAY, -1, 1, 4, RESOLVE_UNKNOWN, true, false), "a missing resolve counts as fine");
        assertEquals(Verdict.OK, v(Rite.SECOND_BOOK, true, 99 * DAY, -1, 1, 5, 6, true, false));
        assertEquals(Verdict.RESOLVE_TOO_LOW, v(Rite.SECOND_BOOK, true, 99 * DAY, -1, 1, 5, 2, true, false));
        assertEquals(Verdict.ALREADY_TWO, v(Rite.SECOND_BOOK, true, 99 * DAY, -1, 2, 5, 6, true, false));
        assertEquals(Verdict.NO_WIZARD, v(Rite.SECOND_BOOK, true, 99 * DAY, -1, 1, 5, 6, false, false));
        assertEquals(Verdict.NO_GRIMOIRE, v(Rite.SECOND_BOOK, true, 99 * DAY, -1, 0, 0, 6, true, false));
        assertEquals(Verdict.OK, RebirthRules.verdict(Rite.SECOND_BOOK, true, 99 * DAY, -1, 7, 1, 4, 4, 0, 0, true, 0, 0, false), "minResolve 0 switches the check off");
    }

    @Test
    void xpCostIsChecked() {
        assertEquals(Verdict.NOT_ENOUGH_XP, RebirthRules.verdict(Rite.REBIRTH, true, 99 * DAY, -1, 7, 1, 3, 4, -1, 3, true, 4, 5, false));
        assertEquals(Verdict.OK, RebirthRules.verdict(Rite.REBIRTH, true, 99 * DAY, -1, 7, 1, 3, 4, -1, 3, true, 5, 5, false));
    }

    @Test
    void cooldownMath() {
        assertEquals(0, cooldownLeft(100, -1, 7), "never used");
        assertEquals(0, cooldownLeft(100, 0, 0), "no cooldown set");
        assertEquals(7 * DAY - 100, cooldownLeft(100, 0, 7));
        assertEquals(0, cooldownLeft(7 * DAY, 0, 7));
        assertEquals(7 * DAY, cooldownLeft(5, 10, 7), "a clock that went backwards is a full cooldown");
        assertEquals(0, daysLeft(0));
        assertEquals(1, daysLeft(1));
        assertEquals(1, daysLeft(DAY));
        assertEquals(2, daysLeft(DAY + 1));
        assertEquals(365, cooldownDays(99999));
        assertEquals(0, cooldownDays(-4));
    }

    @Test
    void confirmWindow() {
        int w = windowTicks(15);
        assertEquals(300, w);
        assertTrue(confirmed(1000, 1000 + w, w, true));
        assertFalse(confirmed(1000, 1000 + w + 1, w, true), "too late");
        assertFalse(confirmed(1000, 1100, w, false), "not sneaking");
        assertFalse(confirmed(-1, 1100, w, true), "no warning given");
        assertFalse(confirmed(1000, 900, w, true), "a warning from the future");
        assertEquals(100, windowTicks(1), "clamped to 5 s");
        assertEquals(2400, windowTicks(9999), "clamped to 120 s");
    }

    @Test
    void marksAndTransitions() {
        assertEquals(Mode.NONE, parse(null));
        assertEquals(Mode.NONE, parse("junk"));
        assertEquals(Mode.RESET, parse("RESET"));
        assertEquals(Mode.RESET, afterUse(Rite.REBIRTH));
        assertEquals(Mode.SECOND, afterUse(Rite.SECOND_BOOK));
        assertEquals(Mode.DONE, afterQuestionnaire(Mode.RESET));
        assertEquals(Mode.DONE, afterQuestionnaire(Mode.SECOND));
        assertEquals(Mode.NONE, afterQuestionnaire(Mode.NONE));
        assertTrue(waitsForQuestionnaire(Mode.RESET, 0));
        assertFalse(waitsForQuestionnaire(Mode.RESET, 1), "a reborn mage who got a grimoire another way is not re-questioned");
        assertTrue(waitsForQuestionnaire(Mode.SECOND, 1));
        assertFalse(waitsForQuestionnaire(Mode.SECOND, 2));
        assertFalse(waitsForQuestionnaire(Mode.SECOND, 0));
        assertFalse(waitsForQuestionnaire(Mode.DONE, 0));
    }

    @Test
    void secondBookMagicDiffersFromTheFirst() {
        assertEquals("FLAME", secondMagic("FLAME", null, null, "WATER"));
        assertEquals("B", secondMagic("A", "A", "B", "A"), "a blend equal to the first book falls back to a parent");
        assertNull(secondMagic("A", "A", null, "A"));
        assertEquals(List.of("B"), poolWithout(List.of("A", "B"), "A"));
        assertEquals(List.of("A"), poolWithout(List.of("A"), "A"), "never an empty pool");
        assertEquals(List.of("A", "B"), poolWithout(List.of("A", "B"), null));
    }

    @Test
    void twoBooksOnFusionOnlyWhenSetStrongAndNotDevil() {
        assertFalse(twoBooksOnFusion(false, true, 5, false), "off by default: nothing changes");
        assertTrue(twoBooksOnFusion(true, true, 4, false));
        assertTrue(twoBooksOnFusion(true, true, 5, false));
        assertFalse(twoBooksOnFusion(true, true, 3, false));
        assertFalse(twoBooksOnFusion(true, false, 5, false));
        assertFalse(twoBooksOnFusion(true, true, 5, true), "a devil cover stays one book");
    }

    @Test
    void confirmMapHoldsOneEntryPerPlayerAndIsCleared() {
        int before = RebirthState.size();
        java.util.UUID a = java.util.UUID.randomUUID(), b = java.util.UUID.randomUUID();
        RebirthState.warn(a, Rite.REBIRTH, 1);
        RebirthState.warn(a, Rite.REBIRTH, 2);
        RebirthState.warn(b, Rite.SECOND_BOOK, 3);
        assertEquals(before + 2, RebirthState.size());
        assertEquals(2, RebirthState.warnedAt(a, Rite.REBIRTH));
        assertEquals(-1, RebirthState.warnedAt(a, Rite.SECOND_BOOK), "a warning only confirms its own rite");
        RebirthState.forget(a);
        RebirthState.forget(b);
        assertEquals(before, RebirthState.size());
    }
}
