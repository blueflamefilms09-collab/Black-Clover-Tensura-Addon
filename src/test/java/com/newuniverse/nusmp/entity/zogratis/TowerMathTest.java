package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Grimoire Tower Access: page tiers become shapes, counts and damage are bounded, and the towers answer with every magic before any repeats. */
class TowerMathTest {
    @Test
    void pageTiersBecomeShapes() {
        assertEquals(TowerMath.Shape.BOLT, TowerMath.shapeFor(8, 0), "starter");
        assertEquals(TowerMath.Shape.BOLT, TowerMath.shapeFor(0, 0));
        assertEquals(TowerMath.Shape.LANCE, TowerMath.shapeFor(12, 0), "mid");
        assertEquals(TowerMath.Shape.NOVA, TowerMath.shapeFor(20, 0), "zone, even salt");
        assertEquals(TowerMath.Shape.FIELD, TowerMath.shapeFor(20, 1), "zone, odd salt");
        assertEquals(TowerMath.Shape.NOVA, TowerMath.shapeFor(20, -2));
        assertEquals(TowerMath.Shape.FIELD, TowerMath.shapeFor(20, -1), "negative salts are fine");
        assertEquals(TowerMath.Shape.RAIN, TowerMath.shapeFor(35, 0), "signature");
        assertEquals(TowerMath.Shape.RAIN, TowerMath.shapeFor(90, 3));
    }

    @Test
    void everyShapeIsBoundedAtEveryVolleySize() {
        for (TowerMath.Shape s : TowerMath.Shape.values()) {
            int last = 0;
            for (int slots = -1; slots <= 9; slots++) {
                int n = TowerMath.count(s, slots);
                assertTrue(n >= 1 && n <= 7, s + " count " + n + " at " + slots);
                assertTrue(n >= last, "never fewer with more power");
                last = n;
            }
            assertTrue(TowerMath.baseDamage(s) > 0 && TowerMath.baseDamage(s) <= 13);
            assertTrue(TowerMath.markTicks(s) >= 0);
            assertTrue(TowerMath.radius(s) >= 1.0);
        }
        assertEquals(6, TowerMath.count(TowerMath.Shape.BOLT, 4));
        assertEquals(7, TowerMath.count(TowerMath.Shape.RAIN, 4));
        assertEquals(1, TowerMath.count(TowerMath.Shape.FIELD, 4));
    }

    @Test
    void markedShapesAlwaysGiveTimeToLeave() {
        for (TowerMath.Shape s : new TowerMath.Shape[]{TowerMath.Shape.LANCE, TowerMath.Shape.NOVA, TowerMath.Shape.FIELD, TowerMath.Shape.RAIN})
            assertTrue(TowerMath.markTicks(s) >= 14, s + " is marked at least 14 ticks before it lands");
    }

    @Test
    void everyMarkCanBeWalkedOutOfBeforeItLands() {
        for (TowerMath.Shape s : new TowerMath.Shape[]{TowerMath.Shape.NOVA, TowerMath.Shape.FIELD, TowerMath.Shape.RAIN})
            assertTrue(TowerMath.radius(s) / TowerMath.markTicks(s) < TowerMath.WALK_BLOCKS_PER_TICK,
                    s + ": a foe standing in the middle of the mark must be able to walk out of it in the time the mark is shown");
    }

    @Test
    void aCloneCastsAWeakCopy() {
        for (TowerMath.Shape s : TowerMath.Shape.values()) assertTrue(TowerMath.cloneDamage(s) < TowerMath.baseDamage(s) * 0.5f);
    }

    @Test
    void theAnnouncementNamesTheMagicAndThePage() {
        assertEquals("The towers answer: Flame Magic - Flame Bullet", TowerMath.announcement("Flame Magic", "Flame Bullet"));
        assertEquals("The towers answer: Time Magic", TowerMath.announcement("Time Magic", ""));
        assertEquals("The towers answer: Time Magic", TowerMath.announcement("Time Magic", null));
    }

    @Test
    void theDeckDealsEveryIndexOnceBeforeAnyRepeats() {
        for (long seed = 1; seed <= 20; seed++) {
            int size = 17;
            TowerMath.Deck d = new TowerMath.Deck(size, seed);
            for (int round = 0; round < 5; round++) {
                Set<Integer> seen = new HashSet<>();
                for (int i = 0; i < size; i++) {
                    int c = d.next();
                    assertTrue(c >= 0 && c < size);
                    assertTrue(seen.add(c), "repeat inside a deck (seed " + seed + ")");
                }
                assertEquals(size, seen.size());
            }
        }
    }

    @Test
    void theFirstCardOfANewDeckIsNeverTheLastOfTheOldOne() {
        for (long seed = 1; seed <= 200; seed++) {
            TowerMath.Deck d = new TowerMath.Deck(5, seed);
            int prev = -1;
            for (int i = 0; i < 200; i++) {
                int c = d.next();
                assertNotEquals(prev, c, "back to back repeat (seed " + seed + ", card " + i + ")");
                prev = c;
            }
        }
    }

    @Test
    void theDeckIsDeterministicForASeedAndDiffersBetweenSeeds() {
        TowerMath.Deck a = new TowerMath.Deck(30, 42), b = new TowerMath.Deck(30, 42), c = new TowerMath.Deck(30, 43);
        boolean differs = false;
        for (int i = 0; i < 60; i++) {
            int x = a.next(), y = b.next(), z = c.next();
            assertEquals(x, y);
            if (x != z) differs = true;
        }
        assertTrue(differs);
    }

    @Test
    void aDeckOfOneAndOfZeroStillWorks() {
        TowerMath.Deck one = new TowerMath.Deck(1, 7);
        for (int i = 0; i < 5; i++) assertEquals(0, one.next());
        TowerMath.Deck zero = new TowerMath.Deck(0, 7);
        assertEquals(1, zero.size());
        assertEquals(0, zero.next());
    }
}
