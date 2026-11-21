package com.newuniverse.nusmp.dragonballs;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The fuzzy name matcher of the overpowered race / skill / item wishes, with stand-in lists (no game). */
class WishNamesTest {
    private static final List<WishNames.Entry> RACES = WishNames.build(List.of("tensura:human", "tensura:high_orc", "tensura:orc_lord", "tensura:demon_lord", "tensura:lesser_demon", "tensura:elf", "tensura:dark_elf"), Map.of("tensura:elf", "Elf Folk", "tensura:human", "tensura.race.human"), null);

    private static String id(WishNames.Result r) { return r.status() == WishNames.Status.OK ? r.first().id() : r.status().name(); }

    @Test
    void namesAreTitleCasedAndUnique() {
        assertEquals("High Orc", WishNames.titleOf("tensura:high_orc"));
        List<WishNames.Entry> two = WishNames.build(List.of("a:fire_resistance", "b:fire_resistance", "a:mind"), null, null);
        assertEquals("Fire Resistance (a)", two.get(0).name());
        assertEquals("Fire Resistance (b)", two.get(1).name());
        assertEquals("Mind", two.get(2).name());
        Set<String> names = new HashSet<>();
        for (WishNames.Entry e : two) assertTrue(names.add(e.name()));
    }

    @Test
    void aTranslatedNameIsAnAliasButALanguageKeyIsNot() {
        assertEquals(List.of("Elf Folk"), RACES.get(5).aliases());
        assertTrue(RACES.get(0).aliases().isEmpty(), "a raw lang key is not a name");
        assertTrue(WishNames.looksTranslated("Eye of Truth"));
        assertFalse(WishNames.looksTranslated("item.tensura.x"));
    }

    @Test
    void caseSpacesUnderscoresAndNamespaceDoNotMatter() {
        assertEquals("tensura:high_orc", id(WishNames.resolve(RACES, "HIGH ORC")));
        assertEquals("tensura:high_orc", id(WishNames.resolve(RACES, "high_orc")));
        assertEquals("tensura:high_orc", id(WishNames.resolve(RACES, "tensura:high_orc")));
        assertEquals("tensura:high_orc", id(WishNames.resolve(RACES, "highorc")));
        assertEquals("tensura:elf", id(WishNames.resolve(RACES, "elf folk")));
    }

    @Test
    void aSentenceWithFillerFindsTheName() {
        assertEquals("tensura:high_orc", id(WishNames.resolve(RACES, "I wish to become a high orc please")));
        assertEquals("tensura:demon_lord", id(WishNames.resolve(RACES, "turn me into the Demon Lord")));
        assertEquals("tensura:orc_lord", id(WishNames.resolve(RACES, "race orc_lord")));
    }

    @Test
    void theLongestNameInTheSentenceWins() {
        assertEquals("tensura:dark_elf", id(WishNames.resolve(RACES, "I want to be a dark elf")));
        assertEquals("tensura:elf", id(WishNames.resolve(RACES, "I want to be an elf")));
    }

    @Test
    void aPartialWordIsEnoughWhenItIsNotAmbiguous() {
        assertEquals("tensura:demon_lord", id(WishNames.resolve(RACES, "dem lord")));
        assertEquals("tensura:lesser_demon", id(WishNames.resolve(RACES, "less demon")));
    }

    @Test
    void anAmbiguousNameIsNeverGuessedAndOffersCandidates() {
        WishNames.Result r = WishNames.resolve(RACES, "lord");
        assertEquals(WishNames.Status.AMBIGUOUS, r.status());
        assertTrue(r.entries().size() >= 2 && r.entries().size() <= WishNames.MAX_CANDIDATES);
    }

    @Test
    void anUnknownNameIsNone() {
        assertEquals(WishNames.Status.NONE, WishNames.resolve(RACES, "zzzqqq nothing").status());
        assertEquals(WishNames.Status.NONE, WishNames.resolve(RACES, "").status());
        assertEquals(WishNames.Status.NONE, WishNames.resolve(List.of(), "human").status());
        assertEquals(WishNames.Status.NONE, WishNames.resolve(null, "human").status());
    }

    @Test
    void aSentenceOfFillerHasNoName() {
        assertTrue(WishNames.noName("race"));
        assertTrue(WishNames.noName("I wish for a skill please"));
        assertTrue(WishNames.noName("item 64"));
        assertFalse(WishNames.noName("race human"));
        assertEquals("high orc", WishNames.nameOf("become a High Orc"));
    }

    @Test
    void severalSkillsInOneSentenceAreAllFoundUpToTheMaximum() {
        List<WishNames.Entry> skills = WishNames.build(List.of("tensura:eye_of_truth", "tensura:thought_acceleration", "tensura:strengthen_body", "tensura:magic_sense"), null, null);
        WishNames.Result r = WishNames.resolveAll(skills, "skill eye of truth and thought acceleration and strengthen body", 3);
        assertEquals(WishNames.Status.OK, r.status());
        assertEquals(3, r.entries().size());
        assertEquals(2, WishNames.resolveAll(skills, "eye of truth, thought acceleration, strengthen body", 2).entries().size());
        assertEquals(1, WishNames.resolveAll(skills, "eye of truth", 3).entries().size());
        assertEquals(1, WishNames.resolveAll(skills, "eye of truth and eye of truth", 3).entries().size(), "no duplicates");
        assertEquals(WishNames.Status.NONE, WishNames.resolveAll(skills, "nothing here", 3).status());
    }

    @Test
    void commandNamesAreOneWordAndKeepTheNamespaceOnlyWhenNeeded() {
        List<WishNames.Entry> l = WishNames.build(List.of("a:mind", "b:mind", "a:body"), null, null);
        assertEquals(List.of("a:mind", "b:mind", "body"), WishNames.commandNames(l));
        for (String c : WishNames.commandNames(RACES)) assertFalse(c.contains(" "));
    }

    @Test
    void everyNameResolvesBackToItselfOrIsReportedAmbiguousWithItself() {
        for (WishNames.Entry e : RACES) {
            WishNames.Result r = WishNames.resolve(RACES, e.name());
            assertTrue(r.status() == WishNames.Status.OK ? r.first().equals(e) : r.entries().contains(e), e.name());
        }
    }

    @Test
    void theSpokenWordsAreSingleLowerCaseWordsAndBounded() {
        assertEquals(List.of("high", "orc"), WishNames.words("High-Orc"));
        assertEquals(List.of("eye", "truth"), WishNames.words("Eye 12 truth x"));
        Set<String> into = new java.util.LinkedHashSet<>();
        WishNames.addWords(RACES, into, 5);
        assertEquals(5, into.size());
    }
}
