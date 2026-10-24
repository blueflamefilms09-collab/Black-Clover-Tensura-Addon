package com.newuniverse.nusmp.mana.method;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** The rune alphabet: 32 distinct, legible, bounded glyphs, and the words of each element. */
class RuneAlphabetTest {
    private static String shape(int g, boolean mirror) {
        Set<String> segs = new TreeSet<>();
        for (int s = 0; s < RuneAlphabet.segments(g); s++) {
            int x0 = RuneAlphabet.coord(g, s, 0), y0 = RuneAlphabet.coord(g, s, 1), x1 = RuneAlphabet.coord(g, s, 2), y1 = RuneAlphabet.coord(g, s, 3);
            if (mirror) { x0 = 4 - x0; x1 = 4 - x1; }
            String a = x0 + "," + y0, b = x1 + "," + y1;
            segs.add(a.compareTo(b) <= 0 ? a + ">" + b : b + ">" + a);       // a segment has no direction for the comparison
        }
        return String.join("|", segs);
    }

    @Test
    void thereAre32GlyphsEachWithAFewStrokesInsideTheLattice() {
        assertEquals(32, RuneAlphabet.COUNT);
        Set<String> names = new HashSet<>();
        for (int g = 0; g < RuneAlphabet.COUNT; g++) {
            assertTrue(names.add(RuneAlphabet.name(g)), "name " + RuneAlphabet.name(g));
            assertTrue(RuneAlphabet.name(g).matches("[a-z]{3,9}"), RuneAlphabet.name(g));
            int n = RuneAlphabet.segments(g);
            assertTrue(n >= 2 && n <= RuneAlphabet.MAX_SEGMENTS, RuneAlphabet.name(g) + " has " + n + " segments");
            assertEquals(5, RuneAlphabet.MAX_SEGMENTS);
            int minX = 9, maxX = -1, minY = 9, maxY = -1;
            for (int s = 0; s < n; s++) {
                for (int k = 0; k < 4; k++) assertTrue(RuneAlphabet.coord(g, s, k) >= 0 && RuneAlphabet.coord(g, s, k) < RuneAlphabet.LATTICE, "inside the lattice");
                assertTrue(RuneAlphabet.coord(g, s, 0) != RuneAlphabet.coord(g, s, 2) || RuneAlphabet.coord(g, s, 1) != RuneAlphabet.coord(g, s, 3), "no zero length segment");
                minX = Math.min(minX, Math.min(RuneAlphabet.coord(g, s, 0), RuneAlphabet.coord(g, s, 2)));
                maxX = Math.max(maxX, Math.max(RuneAlphabet.coord(g, s, 0), RuneAlphabet.coord(g, s, 2)));
                minY = Math.min(minY, Math.min(RuneAlphabet.coord(g, s, 1), RuneAlphabet.coord(g, s, 3)));
                maxY = Math.max(maxY, Math.max(RuneAlphabet.coord(g, s, 1), RuneAlphabet.coord(g, s, 3)));
            }
            assertTrue(maxY - minY >= 3 || maxX - minX >= 3, RuneAlphabet.name(g) + " is too small to read");
            double[] cum = RuneAlphabet.cumulative(g);
            assertEquals(n, cum.length);
            for (int s = 1; s < n; s++) assertTrue(cum[s] > cum[s - 1], "the pen path grows");
            assertEquals(cum[n - 1], RuneAlphabet.length(g), 0.0);
            assertTrue(RuneAlphabet.length(g) >= 4.0 && RuneAlphabet.length(g) <= 20.0, RuneAlphabet.name(g) + " path " + RuneAlphabet.length(g));
        }
    }

    @Test
    void noTwoGlyphsAreTheSameShapeOrMirrorImagesOfEachOther() {
        for (int a = 0; a < RuneAlphabet.COUNT; a++)
            for (int b = 0; b < RuneAlphabet.COUNT; b++) {
                if (a == b) continue;
                assertFalse(shape(a, false).equals(shape(b, false)), RuneAlphabet.name(a) + " = " + RuneAlphabet.name(b));
                if (shape(a, false).equals(shape(a, true))) continue;                         // a symmetric glyph is its own mirror image
                assertFalse(shape(a, true).equals(shape(b, false)), RuneAlphabet.name(b) + " is the mirror image of " + RuneAlphabet.name(a));
            }
    }

    @Test
    void everyElementHasItsOwnWordsAndTrueMagicWritesTwoMore() {
        Set<String> seen = new HashSet<>();
        for (RuneElement e : RuneElement.values()) {
            Set<Integer> glyphs = new HashSet<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < RuneAlphabet.PHRASE_LENGTH; i++) {
                int g = RuneAlphabet.phrase(e, false, i);
                assertTrue(g >= 0 && g < RuneAlphabet.COUNT);
                assertTrue(glyphs.add(g), e + " repeats a glyph in its phrase");
                assertEquals(g, RuneAlphabet.phrase(e, false, i + RuneAlphabet.PHRASE_LENGTH), "the phrase repeats");
                sb.append(g).append(',');
            }
            assertTrue(seen.add(sb.toString()), e + " has the phrase of another element");
            int differ = 0;
            for (int i = 0; i < RuneAlphabet.PHRASE_LENGTH; i++) if (RuneAlphabet.phrase(e, true, i) != RuneAlphabet.phrase(e, false, i)) differ++;
            assertEquals(2, differ, e + ": True Magic changes exactly two words");
            assertEquals(RuneAlphabet.phrase(RuneElement.FIRE, true, 0), RuneAlphabet.phrase(e, true, 0), "the two True Magic words are the same for every element");
        }
        // two phrases share few words: the spells read differently
        for (RuneElement a : RuneElement.values())
            for (RuneElement b : RuneElement.values()) {
                if (a.ordinal() >= b.ordinal()) continue;
                int same = 0;
                for (int i = 0; i < RuneAlphabet.PHRASE_LENGTH; i++) for (int j = 0; j < RuneAlphabet.PHRASE_LENGTH; j++) if (RuneAlphabet.phrase(a, false, i) == RuneAlphabet.phrase(b, false, j)) same++;
                assertTrue(same <= 2, a + " and " + b + " share " + same + " words");
            }
    }
}
