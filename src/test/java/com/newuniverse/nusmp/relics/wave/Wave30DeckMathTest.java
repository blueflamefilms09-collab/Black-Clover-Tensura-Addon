package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Wave 30: the Deck of Many Things as pure maths (the 22 cards, the shuffle, the allowance, the chain, the crumbling). */
class Wave30DeckMathTest {
    @Test
    void theDeckHasTwentyTwoDistinctCards() {
        assertEquals(22, Wave30DeckMath.SIZE);
        Set<String> names = new HashSet<>();
        for (Wave30DeckMath.Card c : Wave30DeckMath.Card.values()) names.add(c.title);
        assertEquals(22, names.size());
    }

    @Test
    void everyShuffleHoldsEachCardExactlyOnce() {
        for (long seed = 1; seed < 400; seed += 7) {
            Wave30DeckMath.Card[] o = Wave30DeckMath.order(seed);
            assertEquals(22, o.length);
            Set<Wave30DeckMath.Card> seen = EnumSet.noneOf(Wave30DeckMath.Card.class);
            for (Wave30DeckMath.Card c : o) assertTrue(seen.add(c), "a card came up twice in seed " + seed);
        }
    }

    @Test
    void aSeedAlwaysGivesTheSameShuffleAndOthersDiffer() {
        assertTrue(java.util.Arrays.equals(Wave30DeckMath.order(42), Wave30DeckMath.order(42)));
        int different = 0;
        for (long s = 1; s <= 30; s++) if (!java.util.Arrays.equals(Wave30DeckMath.order(s), Wave30DeckMath.order(s + 1000))) different++;
        assertTrue(different >= 29);
    }

    @Test
    void theGreatAndTerribleCardsSitDeeperThanTheCommonOnes() {
        // over many seeds the Sun (weight 0.4) should on average be dealt later than the Fool (weight 1.2)
        double sun = 0, fool = 0;
        int n = 2000;
        for (long s = 1; s <= n; s++) {
            Wave30DeckMath.Card[] o = Wave30DeckMath.order(s * 31 + 5);
            for (int i = 0; i < o.length; i++) {
                if (o[i] == Wave30DeckMath.Card.SUN) sun += i;
                if (o[i] == Wave30DeckMath.Card.FOOL) fool += i;
            }
        }
        assertTrue(sun / n > fool / n + 2.0, "sun " + sun / n + " fool " + fool / n);
        assertTrue(sun / n < 21 && sun / n > 5, "but the Sun is not always last");
    }

    @Test
    void theAllowanceIsBoundedByTheRequestTheDeckAndTheDay() {
        assertEquals(1, Wave30DeckMath.allowed(1, 0, 0));
        assertEquals(3, Wave30DeckMath.allowed(3, 0, 0));
        assertEquals(3, Wave30DeckMath.allowed(9, 0, 0));
        assertEquals(1, Wave30DeckMath.allowed(3, 0, 2), "two already drawn today");
        assertEquals(0, Wave30DeckMath.allowed(1, 0, 3), "three drawn today: no more");
        assertEquals(2, Wave30DeckMath.allowed(3, 20, 0), "only two cards are left");
        assertEquals(0, Wave30DeckMath.allowed(1, 22, 0), "an empty deck gives nothing");
        assertEquals(1, Wave30DeckMath.allowed(0, 0, 0), "a request is at least one");
    }

    @Test
    void aDrawNeverRepeatsACardAndEndsInTheDeck() {
        for (long seed = 1; seed < 300; seed++) {
            int drawn = 0;
            Set<Wave30DeckMath.Card> seen = EnumSet.noneOf(Wave30DeckMath.Card.class);
            while (drawn < 22) {
                Wave30DeckMath.Draw d = Wave30DeckMath.draw(seed, drawn, 3);
                assertTrue(d.drawnAfter() > drawn && d.drawnAfter() <= 22);
                assertEquals(d.cards().size(), d.drawnAfter() - drawn);
                for (Wave30DeckMath.Card c : d.cards()) assertTrue(seen.add(c), "repeat in seed " + seed);
                drawn = d.drawnAfter();
            }
            assertEquals(22, seen.size());
            assertTrue(Wave30DeckMath.crumbles(drawn));
        }
    }

    @Test
    void theChainOfTheFoolAndTheJesterIsBounded() {
        for (long seed = 1; seed < 2000; seed++) {
            int drawn = 0;
            while (drawn < 22) {
                Wave30DeckMath.Draw d = Wave30DeckMath.draw(seed, drawn, 1);
                assertTrue(d.extra() <= Wave30DeckMath.MAX_EXTRA, "extra " + d.extra());
                assertTrue(d.cards().size() <= 1 + Wave30DeckMath.MAX_EXTRA);
                drawn = d.drawnAfter();
            }
        }
    }

    @Test
    void aFoolDrawsAgainAndAJesterMayDrawTwoMore() {
        boolean fool = false, jester = false, jesterXp = false;
        for (long seed = 1; seed < 500; seed++) {
            Wave30DeckMath.Card[] o = Wave30DeckMath.order(seed);
            for (int i = 0; i < 20; i++) {
                if (o[i] == Wave30DeckMath.Card.FOOL) {
                    Wave30DeckMath.Draw d = Wave30DeckMath.draw(seed, i, 1);
                    assertTrue(d.cards().size() >= 2 && d.extra() >= 1);
                    if (o[i + 1] != Wave30DeckMath.Card.FOOL && o[i + 1] != Wave30DeckMath.Card.JESTER) {
                        assertEquals(2, d.cards().size());
                        assertEquals(1, d.extra());
                        fool = true;
                    }
                }
                if (o[i] == Wave30DeckMath.Card.JESTER && i < 19) {
                    Wave30DeckMath.Draw d = Wave30DeckMath.draw(seed, i, 1);
                    if (Wave30DeckMath.jesterDraws(seed, i)) {
                        assertTrue(d.cards().size() >= 2);
                        jester = true;
                    } else {
                        assertEquals(1, d.cards().size());
                        jesterXp = true;
                    }
                }
            }
        }
        assertTrue(fool && jester && jesterXp);
    }

    @Test
    void theMoonGrantsOneToThreeBoons() {
        boolean[] seen = new boolean[4];
        for (long s = 1; s < 200; s++) {
            int b = Wave30DeckMath.moonBoons(s, 5);
            assertTrue(b >= 1 && b <= 3);
            seen[b] = true;
        }
        assertTrue(seen[1] && seen[2] && seen[3]);
    }

    @Test
    void freshSeedsAreNeverZeroAndDiffer() {
        assertNotEquals(0L, Wave30DeckMath.freshSeed(0, 0));
        assertNotEquals(Wave30DeckMath.freshSeed(1, 2), Wave30DeckMath.freshSeed(2, 1));
    }

    @Test
    void theDrawnNamesFollowTheShuffle() {
        long seed = 77;
        Wave30DeckMath.Card[] o = Wave30DeckMath.order(seed);
        assertEquals(3, Wave30DeckMath.names(seed, 3).size());
        assertEquals(o[0].title, Wave30DeckMath.names(seed, 3).get(0));
        assertEquals(22, Wave30DeckMath.names(seed, 99).size());
    }

    @Test
    void theRemainingCountsDown() {
        assertEquals(22, Wave30DeckMath.remaining(0));
        assertEquals(19, Wave30DeckMath.remaining(3));
        assertEquals(0, Wave30DeckMath.remaining(30));
        assertFalse(Wave30DeckMath.crumbles(21));
        assertTrue(Wave30DeckMath.crumbles(22));
    }
}
