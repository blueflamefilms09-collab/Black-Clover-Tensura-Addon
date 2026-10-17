package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: Overwrite's rules are announced to the players, so every rule must be real, distinct and judged exactly as its text says. */
class ZagredRuleTest {
    @Test
    void theNextRuleIsNeverTheLastAndEveryRuleComesUp() {
        Random rng = new Random(7);
        ZagredRule last = null;
        Set<ZagredRule> seen = new HashSet<>();
        for (int i = 0; i < 400; i++) {
            ZagredRule r = ZagredRule.next(rng, last);
            assertNotEquals(last, r);
            seen.add(r);
            last = r;
        }
        assertEquals(ZagredRule.values().length, seen.size());
    }

    @Test
    void everyRuleIsAnnouncedWithItsWayToObeyIt() {
        for (ZagredRule r : ZagredRule.values()) {
            assertFalse(r.text().isBlank());
            assertFalse(r.hint().isBlank());
            assertTrue(r.textKey().startsWith("message.nusmp.zagred.rule."));
            assertEquals(r.textKey() + ".hint", r.hintKey());
        }
        assertEquals("fire", ZagredRule.NO_FIRE.element());
        assertEquals("ice", ZagredRule.NO_ICE.element());
        assertEquals("lightning", ZagredRule.NO_LIGHTNING.element());
        assertNull(ZagredRule.NO_FLYING.element());
    }

    @Test
    void onlyTheMovementRulesCareHowAPlayerMoves() {
        assertTrue(ZagredRule.NO_FLYING.breaks(true, 0, 0), "an ability or an elytra is flying");
        assertTrue(ZagredRule.NO_FLYING.breaks(false, ZagredRule.AIRBORNE_LIMIT + 1, 0));
        assertFalse(ZagredRule.NO_FLYING.breaks(false, ZagredRule.AIRBORNE_LIMIT, 0), "a jump or a fall is not flying");
        assertFalse(ZagredRule.NO_FLYING.breaks(false, 12, 500), "standing still is allowed under this rule");
        assertTrue(ZagredRule.NO_STILL.breaks(false, 0, ZagredRule.STILL_LIMIT));
        assertFalse(ZagredRule.NO_STILL.breaks(false, 0, ZagredRule.STILL_LIMIT - 1));
        assertFalse(ZagredRule.NO_STILL.breaks(true, 999, 0), "flying is allowed under this rule");
        for (ZagredRule r : new ZagredRule[]{ZagredRule.NO_FIRE, ZagredRule.NO_ICE, ZagredRule.NO_LIGHTNING, ZagredRule.NO_HEALING})
            assertFalse(r.breaks(true, 999, 999), r + " is enforced elsewhere");
    }

    @Test
    void theAirborneLimitIsLongerThanAnyJump() {
        assertTrue(ZagredRule.AIRBORNE_LIMIT >= 30, "a jump lasts about twelve ticks, a long drop under thirty");
    }
}
