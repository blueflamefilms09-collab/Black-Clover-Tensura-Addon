package com.newuniverse.nusmp.core.magic.merge;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class MergeArgsTest {
    private static final Predicate<String> MAGIC = w -> Set.of("flame", "water", "wind", "earth", "ice", "soul").contains(w.toLowerCase());

    @Test
    void wordsThatAreMagicsAreParentsAndTheRestIsTheName() {
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water"), null), MergeArgs.parse("flame", "water", "", MAGIC));
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water"), null), MergeArgs.parse("flame", "water", null, MAGIC));
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water", "wind"), null), MergeArgs.parse("flame", "water", "wind", MAGIC));
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water", "wind", "earth"), null), MergeArgs.parse("flame", "water", "wind earth", MAGIC));
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water"), "Steam Lord"), MergeArgs.parse("flame", "water", "Steam Lord", MAGIC));
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water", "ice"), "Steam Lord"), MergeArgs.parse("flame", "water", "ice Steam Lord", MAGIC));
    }

    @Test
    void aQuotedWordIsAlwaysTheName() {
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water"), "Soul"), MergeArgs.parse("flame", "water", "\"Soul\"", MAGIC));
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water"), "Soul Mix"), MergeArgs.parse("flame", "water", "\"Soul Mix\"", MAGIC));
        assertEquals(new MergeArgs.Parsed(List.of("flame", "water", "ice"), "Soul Mix"), MergeArgs.parse("flame", "water", "ice \"Soul Mix\"", MAGIC));
    }

    @Test
    void aFifthMagicWordIsTreatedAsTheNameStart() {
        MergeArgs.Parsed p = MergeArgs.parse("flame", "water", "wind earth ice", MAGIC);
        assertEquals(4, p.parents().size());
        assertEquals("ice", p.name(), "the check for too many parents is the validator's job, the parser never loses a word");
    }

    @Test
    void tokenizeKeepsQuotedRunsTogether() {
        assertEquals(List.of("a", "\"b c\"", "d"), MergeArgs.tokenize("a \"b c\"  d"));
        assertTrue(MergeArgs.tokenize("").isEmpty());
        assertTrue(MergeArgs.tokenize(null).isEmpty());
        assertEquals(List.of("\"open run"), MergeArgs.tokenize("\"open run"), "an unclosed quote takes the rest");
    }
}
