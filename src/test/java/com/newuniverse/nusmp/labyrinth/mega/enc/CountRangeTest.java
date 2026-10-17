package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the encounter tables write a group as "2-4" and a hand-edited file will contain nonsense sooner or later; these tests pin what the parser accepts and
 * refuses, how a count is rolled, and the rule that scaling a pack for a bigger party never empties it.
 */
class CountRangeTest {
    @Test
    void aSingleNumberAndARangeAreParsed() {
        CountRange one = CountRange.parse("3");
        assertEquals(3, one.min());
        assertEquals(3, one.max());
        CountRange r = CountRange.parse(" 2-4 ");
        assertEquals(2, r.min());
        assertEquals(4, r.max());
        assertEquals("2-4", r.text());
        assertEquals("3", one.text());
        assertEquals(0, CountRange.parse("0").max());
        assertEquals(0, CountRange.parse("0-2").min());
    }

    @Test
    void nonsenseIsRefused() {
        for (String bad : new String[] {null, "", " ", "x", "2--4", "-3", "4-2", "1-2-3", "2.5", "1-x", "-", "33", "1-99", "5-"})
            assertNull(CountRange.parse(bad), "'" + bad + "'");
    }

    @Test
    void theLimitIsInclusive() {
        assertEquals(CountRange.LIMIT, CountRange.parse(Integer.toString(CountRange.LIMIT)).max());
        assertNull(CountRange.parse(Integer.toString(CountRange.LIMIT + 1)));
    }

    @Test
    void theConstructorNormalisesItsEnds() {
        CountRange c = new CountRange(5, 2);
        assertEquals(5, c.min());
        assertEquals(5, c.max());
        CountRange neg = new CountRange(-4, 1);
        assertEquals(0, neg.min());
        CountRange big = new CountRange(1, 5000);
        assertEquals(CountRange.LIMIT, big.max());
    }

    @Test
    void rollingStaysInsideTheRangeAndReachesBothEnds() {
        CountRange r = new CountRange(2, 5);
        Random rnd = new Random(1);
        boolean lo = false, hi = false;
        for (int i = 0; i < 1000; i++) {
            int n = r.roll(rnd);
            assertTrue(n >= 2 && n <= 5, "rolled " + n);
            lo |= n == 2;
            hi |= n == 5;
        }
        assertTrue(lo && hi);
        assertEquals(3, new CountRange(3, 3).roll(rnd));
    }

    @Test
    void scalingMultipliesBothEndsButKeepsAtLeastOne() {
        CountRange r = new CountRange(2, 4).scaled(1.6);
        assertEquals(3, r.min());
        assertEquals(6, r.max());
        CountRange small = new CountRange(1, 1).scaled(0.3);
        assertEquals(1, small.min(), "a pack for a weak party is smaller, never empty");
        assertEquals(1, small.max());
        CountRange none = new CountRange(0, 2).scaled(2.0);
        assertEquals(0, none.min(), "an optional extra stays optional");
        assertEquals(4, none.max());
        assertSame(r, r.scaled(1.0));
        assertSame(r, r.scaled(0.0));
        assertSame(r, r.scaled(Double.NaN));
    }

    @Test
    void bandsKnowTheirNeighboursAndTheirNames() {
        assertEquals(Band.MINOR, Band.NOTHING.up());
        assertEquals(Band.AMBUSH, Band.AMBUSH.up());
        assertEquals(Band.MINOR, Band.STANDARD.down());
        assertEquals(Band.NOTHING, Band.NOTHING.down());
        assertEquals(Band.HARD, Band.byName("Hard"));
        assertEquals(Band.AMBUSH, Band.byName(" ambush "));
        assertNull(Band.byName("hrad"));
        assertNull(Band.byName(null));
        assertEquals("standard", Band.STANDARD.label());
        for (Band b : Band.values()) assertSame(b, Band.byName(b.label()));
    }

    @Test
    void theBudgetFactorsAndHuntTimesRiseWithTheBand() {
        Band[] all = Band.values();
        for (int i = 1; i < all.length; i++) {
            assertTrue(all[i].budgetFactor() > all[i - 1].budgetFactor(), all[i].name());
            assertTrue(all[i].huntTicks() > all[i - 1].huntTicks(), all[i].name());
        }
        assertEquals(0.0, Band.NOTHING.budgetFactor(), 0.0);
        assertEquals(1.0, Band.STANDARD.budgetFactor(), 0.0);
    }
}
