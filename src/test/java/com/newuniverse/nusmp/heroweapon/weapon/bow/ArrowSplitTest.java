package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Why: the damage of a hero arrow is two hits (physical = the number the hitscan arrow dealt, magic = a share of it) and the number of arrows is capped per
 * wielder and for the server; both are pure rules a test can pin: the split never produces a negative or an over-scaled number, a share of 0 turns the
 * magic hit off, the child arrows of the Shower are a bounded small share, and the budget never goes above a cap or below zero whatever order the
 * requests and returns come in.
 */
class ArrowSplitTest {
    @AfterEach
    void clean() { ArrowBudget.clear(); }

    @Test
    void thePhysicalPartIsTheRawDamageAndTheMagicPartIsAShareOfIt() {
        assertEquals(7.0, ArrowSplit.physical(7.0), 1e-9);
        assertEquals(2.1, ArrowSplit.magic(7.0, 0.30, 1.0), 1e-9);
        assertEquals(0.0, ArrowSplit.magic(7.0, 0.30, 0.0), 1e-9, "scale 0 is physical only");
        assertEquals(4.2, ArrowSplit.magic(7.0, 0.30, 2.0), 1e-9);
        assertEquals(4.2, ArrowSplit.magic(7.0, 0.30, 99.0), 1e-9, "the scale is capped");
        assertEquals(7.0, ArrowSplit.magic(7.0, 5.0, 1.0), 1e-9, "a share is never above the physical hit");
    }

    @Test
    void negativeInputsNeverGiveNegativeDamage() {
        assertEquals(0.0, ArrowSplit.physical(-3.0));
        assertEquals(0.0, ArrowSplit.magic(-3.0, 0.3, 1.0));
        assertEquals(0.0, ArrowSplit.magic(3.0, -0.3, 1.0));
        assertEquals(0.0, ArrowSplit.magic(3.0, 0.3, -1.0));
        assertEquals(0.0, ArrowSplit.child(-1.0));
    }

    @Test
    void everyFormShareKeepsTheWorstCaseBelowTwiceThePhysicalHit() {
        for (String id : BowArrowForms.ids()) {
            double share = BowArrowForms.of(id).magicShare();
            assertTrue(ArrowSplit.worst(BowNumbers.BASIC_MAX_RAW, share) <= BowNumbers.BASIC_MAX_RAW * 2.0 + 1e-9, id);
            assertTrue(ArrowSplit.worst(BowNumbers.BASIC_MAX_RAW, share) > BowNumbers.BASIC_MAX_RAW, id + " must add a magic part");
        }
    }

    @Test
    void theShowerSplitsIntoAFewSmallChildren() {
        assertTrue(ArrowSplit.SPLIT_CHILDREN >= 2 && ArrowSplit.SPLIT_CHILDREN <= 4);
        assertTrue(ArrowSplit.SPLIT_CHILDREN * ArrowSplit.SPLIT_SHARE <= 1.3, "the children together never out-damage the parent by much");
        assertEquals(2.0, ArrowSplit.child(5.0), 1e-9);
        assertTrue(ArrowSplit.SCATTER_SHARE > 0.0 && ArrowSplit.SCATTER_SHARE < 1.0);
    }

    @Test
    void thePerWielderCapHolds() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        for (int i = 0; i < 24; i++) assertTrue(ArrowBudget.tryAcquire(a, 24, 256), "arrow " + i);
        assertFalse(ArrowBudget.tryAcquire(a, 24, 256), "the 25th is refused");
        assertTrue(ArrowBudget.tryAcquire(b, 24, 256), "another wielder has their own cap");
        assertEquals(24, ArrowBudget.count(a));
        assertEquals(25, ArrowBudget.total());
        ArrowBudget.release(a);
        assertTrue(ArrowBudget.tryAcquire(a, 24, 256), "a place given back can be taken again");
    }

    @Test
    void theGlobalCapHoldsAcrossWielders() {
        int taken = 0;
        for (int i = 0; i < 20; i++) if (ArrowBudget.tryAcquire(UUID.randomUUID(), 24, 10)) taken++;
        assertEquals(10, taken);
        assertEquals(10, ArrowBudget.total());
        assertFalse(ArrowBudget.tryAcquire(UUID.randomUUID(), 24, 10));
    }

    @Test
    void releasesNeverGoBelowZeroAndAnUnknownWielderIsIgnored() {
        UUID a = UUID.randomUUID();
        ArrowBudget.release(a);
        ArrowBudget.release(null);
        assertEquals(0, ArrowBudget.total());
        assertTrue(ArrowBudget.tryAcquire(a, 2, 5));
        ArrowBudget.release(a);
        ArrowBudget.release(a);
        assertEquals(0, ArrowBudget.count(a));
        assertEquals(0, ArrowBudget.total());
        assertFalse(ArrowBudget.tryAcquire(null, 2, 5));
        assertFalse(ArrowBudget.tryAcquire(a, 0, 5), "a cap of zero shoots nothing");
        assertFalse(ArrowBudget.tryAcquire(a, 5, 0));
    }

    @Test
    void clearForgetsEverything() {
        UUID a = UUID.randomUUID();
        ArrowBudget.tryAcquire(a, 4, 4);
        ArrowBudget.clear();
        assertEquals(0, ArrowBudget.total());
        assertEquals(0, ArrowBudget.count(a));
    }
}
