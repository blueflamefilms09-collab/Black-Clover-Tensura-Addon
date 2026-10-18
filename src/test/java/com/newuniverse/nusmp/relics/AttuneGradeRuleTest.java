package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner's rule of 1.84.0: relics of Normal to Unique grade need no attunement, Legend and above still do; and the Unbinding Seal frees an attuned relic. Both decisions are pure
 * functions of {@link AttuneRules}, so they are pinned here without a game: a regression that made a Rare relic demand a five-second ritual again, or let a seal work on nothing, fails here.
 */
class AttuneGradeRuleTest {
    @Test
    void relicsBelowLegendNeverNeedAttunementEvenWhenDefinedWithIt() {
        for (Grade g : Grade.values()) {
            boolean expected = g.ordinal() >= Grade.LEGEND.ordinal();
            assertEquals(expected, AttuneRules.requires(true, g), g + " defined with .attune()");
            assertFalse(AttuneRules.requires(false, g), g + " defined without .attune() never needs it");
        }
        assertFalse(AttuneRules.requires(true, Grade.NORMAL));
        assertFalse(AttuneRules.requires(true, Grade.SPECIAL));
        assertFalse(AttuneRules.requires(true, Grade.RARE));
        assertFalse(AttuneRules.requires(true, Grade.UNIQUE));
        assertTrue(AttuneRules.requires(true, Grade.LEGEND));
        assertTrue(AttuneRules.requires(true, Grade.GOD));
        assertTrue(AttuneRules.requires(true, Grade.GENESIS));
        assertFalse(AttuneRules.requires(true, null), "a definition with no grade is never forced into the ritual");
    }

    @Test
    void aRelicThatNeedsNoAttunementAlwaysWorks() {
        boolean[] ok = AttuneRules.working(new boolean[] {false, false, false, false, false}, new boolean[5], AttuneRules.CAP);
        for (boolean b : ok) assertTrue(b);
    }

    @Test
    void theSealFreesOnlyAnAttunedRelic() {
        assertEquals(AttuneRules.Unbind.NO_RELIC, AttuneRules.unbind(false, false));
        assertEquals(AttuneRules.Unbind.NO_RELIC, AttuneRules.unbind(false, true));
        assertEquals(AttuneRules.Unbind.NOT_ATTUNED, AttuneRules.unbind(true, false));
        assertEquals(AttuneRules.Unbind.RELEASED, AttuneRules.unbind(true, true));
    }
}
