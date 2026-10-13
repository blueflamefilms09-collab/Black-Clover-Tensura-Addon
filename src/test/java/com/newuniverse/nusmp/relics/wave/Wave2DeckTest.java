package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Pact of the Deck: the 54-card deck. */
class Wave2DeckTest {
    @Test
    void theDeckHasFiftyFourDistinctCards() {
        assertEquals(54, Wave2Deck.remaining(Wave2Deck.FULL));
        Set<String> names = new HashSet<>();
        for (int c = 0; c < Wave2Deck.SIZE; c++) names.add(Wave2Deck.name(c));
        assertEquals(53, names.size(), "52 cards and the (identical) jokers");
        int aces = 0, kings = 0, queens = 0, jacks = 0, jokers = 0, numbers = 0;
        for (int c = 0; c < Wave2Deck.SIZE; c++) {
            switch (Wave2Deck.kind(c)) {
                case ACE -> aces++;
                case KING -> kings++;
                case QUEEN -> queens++;
                case JACK -> jacks++;
                case JOKER -> jokers++;
                default -> numbers++;
            }
        }
        assertEquals(4, aces);
        assertEquals(4, kings);
        assertEquals(4, queens);
        assertEquals(4, jacks);
        assertEquals(2, jokers);
        assertEquals(36, numbers);
    }

    @Test
    void suitsAndRanksMatchTheLayout() {
        assertEquals(Wave2Deck.Suit.SPADES, Wave2Deck.suit(0));
        assertEquals(1, Wave2Deck.rank(0));
        assertEquals(13, Wave2Deck.rank(12));
        assertEquals(Wave2Deck.Suit.HEARTS, Wave2Deck.suit(13));
        assertEquals(Wave2Deck.Suit.CLUBS, Wave2Deck.suit(26));
        assertEquals(Wave2Deck.Suit.DIAMONDS, Wave2Deck.suit(51));
        assertEquals(13, Wave2Deck.rank(51));
        assertEquals(Wave2Deck.Suit.JOKER, Wave2Deck.suit(52));
        assertEquals(Wave2Deck.Suit.JOKER, Wave2Deck.suit(53));
        assertEquals(0, Wave2Deck.rank(53));
        assertEquals("Queen of Hearts", Wave2Deck.name(13 + 11));
        assertEquals("Ace of Spades", Wave2Deck.name(0));
        assertEquals("7 of Clubs", Wave2Deck.name(26 + 6));
    }

    @Test
    void drawingEmptiesTheDeckWithoutRepeats() {
        Random r = new Random(42);
        long mask = Wave2Deck.FULL;
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 54; i++) {
            int card = Wave2Deck.draw(mask, r::nextInt);
            assertTrue(card >= 0 && card < 54);
            assertTrue(seen.add(card), "no card twice: " + card);
            mask = Wave2Deck.take(mask, card);
            assertEquals(53 - i, Wave2Deck.remaining(mask));
        }
        assertEquals(0L, mask);
        assertEquals(-1, Wave2Deck.draw(mask, r::nextInt));
    }

    @Test
    void takeIgnoresNonsense() {
        assertEquals(Wave2Deck.FULL, Wave2Deck.take(Wave2Deck.FULL, -1));
        assertEquals(Wave2Deck.FULL, Wave2Deck.take(Wave2Deck.FULL, 54));
        assertEquals(53, Wave2Deck.remaining(Wave2Deck.take(Wave2Deck.FULL, 0)));
    }

    @Test
    void aRngThatMisbehavesStillDrawsARealCard() {
        long mask = 0b1011L;
        assertTrue(Set.of(0, 1, 3).contains(Wave2Deck.draw(mask, n -> -7)));
        assertTrue(Set.of(0, 1, 3).contains(Wave2Deck.draw(mask, n -> 1_000_003)));
    }

    @Test
    void theDeckComesBackWholeOnANewDay() {
        assertEquals(Wave2Deck.FULL, Wave2Deck.forDay(false, 0, 0, 5));
        assertEquals(Wave2Deck.FULL, Wave2Deck.forDay(true, 0b11, 4, 5));
        assertEquals(0b11L, Wave2Deck.forDay(true, 0b11, 5, 5));
        assertEquals(0L, Wave2Deck.forDay(true, 0, 5, 5), "spent, same day");
    }

    @Test
    void numberCardsHitForHalfTheirValue() {
        assertEquals(0.0, Wave2Deck.damage(1));
        assertEquals(1.0, Wave2Deck.damage(2));
        assertEquals(5.0, Wave2Deck.damage(10));
        assertEquals(0.0, Wave2Deck.damage(11));
        assertEquals(0.0, Wave2Deck.damage(0));
        assertTrue(Wave2Deck.damage(10) <= 6.0, "a card never costs a player more than 30%");
    }

    @Test
    void suitsHaveTheirDamageTypes() {
        assertEquals("minecraft:on_fire", Wave2Deck.Suit.HEARTS.damageType());
        assertEquals("minecraft:freeze", Wave2Deck.Suit.DIAMONDS.damageType());
        assertEquals("minecraft:magic", Wave2Deck.Suit.CLUBS.damageType());
        assertEquals("minecraft:thrown", Wave2Deck.Suit.SPADES.damageType());
    }

    @Test
    void drawingThePactPaysMoreForAces() {
        assertEquals(0.02, Wave2Deck.channelFraction(0), 1e-9);
        assertEquals(0.08, Wave2Deck.channelFraction(1), 1e-9);
        assertEquals(0.28, Wave2Deck.channelFraction(2), 1e-9, "two aces cast for free: doubled");
        assertEquals(0.40, Wave2Deck.channelFraction(3), 1e-9);
        assertEquals(0.02, Wave2Deck.channelFraction(-4), 1e-9);
        assertEquals(3, Wave2Deck.PACT_DRAW);
    }

    @Test
    void theSurgeTableHasEightEntriesAndWrapsAround() {
        assertEquals(8, Wave2Deck.Surge.values().length);
        Set<Wave2Deck.Surge> seen = new HashSet<>();
        for (int i = -16; i < 16; i++) seen.add(Wave2Deck.surge(i));
        assertEquals(8, seen.size());
        assertEquals(Wave2Deck.surge(3), Wave2Deck.surge(11));
        for (Wave2Deck.Surge s : Wave2Deck.Surge.values()) assertFalse(s.line().isBlank());
    }
}
