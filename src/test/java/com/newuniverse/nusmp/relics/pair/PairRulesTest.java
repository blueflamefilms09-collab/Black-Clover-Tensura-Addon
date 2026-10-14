package com.newuniverse.nusmp.relics.pair;

import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure rules of weapon pairs and the RelicDef pair builder. */
class PairRulesTest {
    @Test
    void idsOfTheTwoHalves() {
        assertEquals("twin_hook_swords", PairRules.baseId("twin_hook_swords"));
        assertEquals("twin_hook_swords", PairRules.baseId("twin_hook_swords_left"));
        assertEquals("", PairRules.baseId(null));
        assertEquals("_left", PairRules.baseId("_left"), "a bare suffix is no pair id");
        assertTrue(PairRules.isLeftId("a_left"));
        assertFalse(PairRules.isLeftId("a"));
        assertEquals("a_left", PairRules.partnerId("a"));
        assertEquals("a", PairRules.partnerId("a_left"));
    }

    @Test
    void aPairNeedsOneRightAndOneLeftOfTheSameBase() {
        assertTrue(PairRules.pairActive("a", "a_left"));
        assertTrue(PairRules.pairActive("a_left", "a"));
        assertFalse(PairRules.pairActive("a", "a"), "two right halves are no pair");
        assertFalse(PairRules.pairActive("a_left", "a_left"));
        assertFalse(PairRules.pairActive("a", "b_left"), "halves of two different pairs");
        assertFalse(PairRules.pairActive("a", ""));
        assertFalse(PairRules.pairActive(null, "a_left"));
    }

    @Test
    void theOffHandBlowIsSixTenthsAndCapped() {
        assertEquals(6.0f, PairRules.offHandDamage(10f), 1e-4);
        assertEquals(0f, PairRules.offHandDamage(0f));
        assertEquals(0f, PairRules.offHandDamage(-3f));
        assertEquals(0f, PairRules.offHandDamage(Float.NaN));
        assertEquals(0f, PairRules.offHandDamage(Float.POSITIVE_INFINITY));
        assertEquals(PairRules.OFFHAND_MAX, PairRules.offHandDamage(1000f), 1e-4);
    }

    @Test
    void theOffHandBlowHasItsOwnCooldownAndAClockJumpDoesNotLockIt() {
        long now = 1000;
        assertTrue(PairRules.ready(now, 0));
        long next = PairRules.nextAllowed(now);
        assertFalse(PairRules.ready(now + 1, next));
        assertTrue(PairRules.ready(next, next));
        assertTrue(PairRules.ready(now, now + 10_000_000L), "a stored time from the far future (a changed clock) must not lock the blow for ever");
    }

    @Test
    void aLoneHalfLosesALittle() {
        assertEquals(1.0f, PairRules.factor(true));
        assertEquals(0.85f, PairRules.factor(false), 1e-6);
    }

    @Test
    void completingAndRollingAPair() {
        assertEquals(0, PairRules.missingHalves(true, true));
        assertEquals(1, PairRules.missingHalves(false, true), "the right half was lost");
        assertEquals(2, PairRules.missingHalves(true, false), "the left half was never issued (an old world)");
        assertEquals(3, PairRules.missingHalves(false, false));
        assertTrue(PairRules.rollable(false, false));
        assertFalse(PairRules.rollable(true, false), "a half lives: one pair exists, never a second");
        assertFalse(PairRules.rollable(false, true));
        assertTrue(PairRules.needsLeftGrant(true, false));
        assertFalse(PairRules.needsLeftGrant(true, true), "granted once only");
        assertFalse(PairRules.needsLeftGrant(false, false), "no right half in hand: nothing to complete");
    }

    @Test
    void theDefBuilderMakesTheLeftHalf() {
        RelicDef right = RelicDef.of("twin_x", "Twin X (Right)", Grade.RARE, RelicSlot.MAIN_HAND).pair("Twin X (Left)").melee(4.0, 2.0).engrave("swift").attune().model3d();
        assertTrue(right.hasPair());
        assertFalse(right.isPairLeft());
        assertEquals("twin_x_left", right.pairPartnerId());
        assertEquals("twin_x", right.pairBaseId());
        RelicDef left = right.pairLeft();
        assertNotNull(left);
        assertEquals("twin_x_left", left.id());
        assertEquals("Twin X (Left)", left.cardName());
        assertTrue(left.isPairLeft());
        assertFalse(left.hasPair(), "the left half does not spawn another half");
        assertNull(left.pairLeft());
        assertEquals("twin_x", left.pairPartnerId());
        assertEquals("twin_x", left.pairBaseId());
        assertEquals(right.meleeDamage(), left.meleeDamage());
        assertEquals(right.engravings(), left.engravings());
        assertEquals(right.grade(), left.grade());
        assertTrue(left.hasModel3d());
        assertTrue(left.needsAttunement());
        assertFalse(left.isVessel());
        assertTrue(left.isUnique() && right.isUnique(), "each half is a one-of-a-kind stack in the Order's ledger");
        assertNull(RelicDef.of("plain", "Plain", Grade.RARE, RelicSlot.MAIN_HAND).pairLeft());
    }

    @Test
    void onlyAHandWeaponCanBeAPair() {
        boolean threw = false;
        try {
            RelicDef.of("ring_x", "Ring", Grade.RARE, RelicSlot.HEAD).pair("Ring 2");
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue(threw);
    }
}
