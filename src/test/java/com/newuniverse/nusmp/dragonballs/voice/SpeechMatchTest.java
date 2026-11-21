package com.newuniverse.nusmp.dragonballs.voice;

import com.newuniverse.nusmp.dragonballs.BallSet;
import com.newuniverse.nusmp.dragonballs.DbRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpeechMatchTest {
    private static List<String> vocab() {
        List<String> phrases = new ArrayList<>();
        for (BallSet s : BallSet.values()) phrases.addAll(s.defaultPhrases());
        phrases.addAll(List.of("revive", "power", "cancel", "never mind", "destroy", "eye of truth"));
        return SpeechMatch.vocabulary(phrases);
    }

    private static boolean earth(String heard) { return DbRules.matchesAny(SpeechMatch.canonicalize(heard, vocab()), BallSet.EARTH.defaultPhrases()); }

    private static boolean superSet(String heard) { return DbRules.matchesAny(SpeechMatch.canonicalize(heard, vocab()), BallSet.SUPER.defaultPhrases()); }

    @Test
    void toleranceByLength() {
        assertEquals(0, SpeechMatch.tolerance(3));
        assertEquals(1, SpeechMatch.tolerance(4));
        assertEquals(1, SpeechMatch.tolerance(7));
        assertEquals(2, SpeechMatch.tolerance(8));
        assertEquals(1, SpeechMatch.distance("kitten", "sitten", 2));
        assertEquals(3, SpeechMatch.distance("abcdef", "uvwxyz", 2), "above the limit it only says 'more than max'");
    }

    @Test
    void shortWordsMustBeExactLongWordsMayDrift() {
        assertFalse(SpeechMatch.wordMatches("pee", "pea"));
        assertTrue(SpeechMatch.wordMatches("shenran", "shenron"));
        assertTrue(SpeechMatch.wordMatches("takkaradopo", "takkaradoppo"));
        assertTrue(SpeechMatch.wordMatches("yaikanan", "yaikanaan"));
        assertFalse(SpeechMatch.wordMatches("banana", "shenron"));
    }

    @Test
    void soundsAlike() {
        assertEquals(SpeechMatch.phoneticKey("takkaradoppo"), SpeechMatch.phoneticKey("takaradopo"));
        assertEquals(SpeechMatch.phoneticKey("phone"), SpeechMatch.phoneticKey("fone"));
        assertNotEquals(SpeechMatch.phoneticKey("shenron"), SpeechMatch.phoneticKey("dragon"));
    }

    @Test
    void theCanonicalPhrasesSurviveAndCaseAndPunctuationAreIgnored() {
        for (BallSet s : BallSet.values())
            for (String p : s.defaultPhrases())
                assertTrue(DbRules.matchesAny(SpeechMatch.canonicalize("Hey everyone: " + p.toUpperCase() + "!!", vocab()), s.defaultPhrases()), p);
    }

    @Test
    void aMangledNamekianChantStillMatches() {
        assertTrue(earth("takara doppo pakkora doppo yaikanan"));       // split words and dropped letters
        assertTrue(earth("takkaradopo pakkoradopo yaikanaan"));
        assertTrue(earth("Takkara Doppo, Pakkora Doppo, Yaikanaan!"));
        assertTrue(earth("ideyoshenron"));                              // fused
        assertTrue(earth("ide yo shenron"));                            // split up
        assertTrue(earth("come fourth shenron"));
    }

    @Test
    void theSuperCallSurvivesAWrongWord() {
        assertTrue(superSet("come forth dragon of the gods and grant my wish pretty peas"));
        assertTrue(superSet("come fourth dragon of the gods"));
        assertTrue(superSet("come forth devine dragon"));
    }

    @Test
    void ordinaryTalkAndOtherWordsDoNotMatch() {
        assertFalse(earth("what a lovely day for a walk"));
        assertFalse(superSet("come forth my friend"));
        assertFalse(earth("arise"));
        assertEquals("", SpeechMatch.canonicalize("   ", vocab()));
        assertEquals("", SpeechMatch.canonicalize(null, vocab()));
    }

    @Test
    void wishWordsAreBroughtHome() {
        assertEquals("revive", SpeechMatch.canonicalize("revive", vocab()));
        assertTrue(SpeechMatch.canonicalize("revise me", vocab()).startsWith("revive"));
        assertTrue(DbRules.contains(SpeechMatch.canonicalize("never mind", vocab()), "never mind"));
        assertTrue(DbRules.contains(SpeechMatch.canonicalize("destroi the thing", vocab()), "destroy"), "a refused word must still be seen as one");
    }

    @Test
    void theOutputIsBoundedAndTheInputToo() {
        String huge = "takara ".repeat(500);
        assertTrue(SpeechMatch.canonicalize(huge, vocab()).split(" ").length <= SpeechMatch.MAX_TOKENS);
        assertTrue(SpeechMatch.vocabulary(java.util.Collections.nCopies(1, "a ".repeat(2000) + "b".repeat(100))).size() <= SpeechMatch.MAX_VOCAB);
    }

    @Test
    void grammarIsAJsonListEndingInUnk() {
        assertEquals("[\"come\",\"forth\",\"[unk]\"]", SpeechMatch.grammarJson(List.of("come", "forth")));
        assertEquals("[\"[unk]\"]", SpeechMatch.grammarJson(List.of()));
        assertDoesNotThrow(() -> com.google.gson.JsonParser.parseString(SpeechMatch.grammarJson(vocab())));
    }
}
