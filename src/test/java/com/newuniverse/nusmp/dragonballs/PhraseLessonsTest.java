package com.newuniverse.nusmp.dragonballs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhraseLessonsTest {
    private static List<String> taught(List<String> words, int balls) {
        List<String> all = new ArrayList<>();
        for (int n = 1; n <= balls; n++) all.addAll(PhraseLessons.fresh(words, balls, n));
        return all;
    }

    @Test
    void shortPhraseGivesOneWordPerBallThenNothing() {
        List<String> w = PhraseLessons.words("takkaradoppo pakkoradoppo yaikanaan");
        assertEquals(List.of("takkaradoppo"), PhraseLessons.fresh(w, 7, 1));
        assertEquals(List.of("yaikanaan"), PhraseLessons.fresh(w, 7, 3));
        assertTrue(PhraseLessons.fresh(w, 7, 4).isEmpty());
        assertEquals(w, taught(w, 7));
    }

    @Test
    void longPhraseIsCutIntoEvenGroupsWithoutLosingOrRepeatingAWord() {
        List<String> w = PhraseLessons.words("come forth dragon of the gods and grant my wish pretty peas");
        for (int balls = 2; balls <= 8; balls++) assertEquals(w, taught(w, balls), "balls=" + balls);
        assertEquals(2, PhraseLessons.fresh(w, 7, 1).size());
    }

    @Test
    void theLastBallCompletesThePhraseAndAddsNoWordsOfItsOwn() {
        List<String> w = PhraseLessons.words("come forth shenron");
        assertEquals(w, PhraseLessons.known(w, 7, 7));
        assertEquals(w, PhraseLessons.known(w, 7, 9));
        assertTrue(PhraseLessons.fresh(w, 7, 7).isEmpty());
        assertEquals(List.of("come", "forth"), PhraseLessons.known(w, 7, 2));
        assertTrue(PhraseLessons.known(w, 7, 0).isEmpty());
    }

    @Test
    void emptyAndOddInputIsSafe() {
        assertTrue(PhraseLessons.words(null).isEmpty());
        assertTrue(PhraseLessons.words("   ").isEmpty());
        assertTrue(PhraseLessons.fresh(List.of(), 7, 1).isEmpty());
        assertTrue(PhraseLessons.fresh(List.of("a"), 1, 1).isEmpty());
        assertTrue(PhraseLessons.fresh(List.of("a", "b"), 7, 0).isEmpty());
        assertEquals("", PhraseLessons.line(List.of()));
    }

    @Test
    void lineIsPasteableAndSentenceCase() {
        assertEquals("Takkaradoppo pakkoradoppo", PhraseLessons.line(List.of("takkaradoppo", "pakkoradoppo")));
        assertTrue(DbRules.matchesAny("Takkaradoppo pakkoradoppo yaikanaan", List.of("takkaradoppo pakkoradoppo yaikanaan")));
    }

    @Test
    void maskCountsEachStarOnce() {
        int m = 0;
        for (int star = 1; star <= 8; star++) {
            assertFalse(PhraseLessons.has(m, star));
            m = PhraseLessons.with(m, star);
            m = PhraseLessons.with(m, star);
            assertTrue(PhraseLessons.has(m, star));
            assertEquals(star, PhraseLessons.count(m));
        }
        assertEquals(m, PhraseLessons.with(m, 0));
        assertEquals(m, PhraseLessons.with(m, 9));
        assertFalse(PhraseLessons.has(0, 9));
    }
}
