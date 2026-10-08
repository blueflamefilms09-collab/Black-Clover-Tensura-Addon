package com.newuniverse.nusmp.grimoire;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The grimoire design generator: stable per magic, distinct across the magics that have no hand-made palette, and wired into BookLook. */
class GrimoireDesignGeneratorTest {
    /** The wiki attributes of 0.89 (WikiBooks.newAttributes): none has a hand-made palette entry. */
    private static final String[] WIKI = {"AIR", "HAIR", "MEMORY", "MINERAL", "MODIFICATION", "MUCUS", "MUD", "NAIL", "PERMEATION", "POISON_PLANT",
            "RED_OCHRE", "ROCK", "SANDSTONE", "SCALE", "SHAKUDO", "SKIN", "SMOKE", "SNOW", "SONG", "SOUL_CORPSE", "SOUL", "SOUND", "SPIKE",
            "SWITCHING", "TONGUE", "TREE", "STONE", "VINE", "VORTEX", "WING"};

    private static int dist(int a, int b) {
        int dr = ((a >> 16) & 255) - ((b >> 16) & 255), dg = ((a >> 8) & 255) - ((b >> 8) & 255), db = (a & 255) - (b & 255);
        return (int) Math.sqrt(dr * dr + dg * dg + db * db);
    }

    @Test
    void sameMagicSameDesign() {
        assertEquals(GrimoireDesignGenerator.forMagic("AIR"), GrimoireDesignGenerator.forMagic("AIR"));
        assertNotEquals(GrimoireDesignGenerator.forMagic("AIR"), GrimoireDesignGenerator.forMagic("HAIR"));
    }

    @Test
    void generatedMagicsAreGeneratedAndTheOldOnesAreNot() {
        for (String m : WIKI) assertFalse(BookPalette.handMade(m), m);
        assertTrue(BookPalette.handMade("FLAME"));
        assertEquals(0x9C2117, BookPalette.cover("FLAME"));
        assertEquals(0x8A6A4A, BookPalette.cover(""), "a book with no magic keeps the old default");
    }

    @Test
    void everyWikiGrimoireLooksDifferent() {
        Set<Integer> studs = new HashSet<>(), metals = new HashSet<>();
        int minCover = 999, minGlow = 999;
        String worst = "";
        for (int i = 0; i < WIKI.length; i++) {
            var a = GrimoireDesignGenerator.forMagic(WIKI[i]);
            studs.add(a.studs());
            metals.add(a.trim());
            for (int j = i + 1; j < WIKI.length; j++) {
                var b = GrimoireDesignGenerator.forMagic(WIKI[j]);
                int d = dist(a.cover(), b.cover());
                if (d < minCover) { minCover = d; worst = WIKI[i] + "/" + WIKI[j]; }
                minGlow = Math.min(minGlow, dist(a.glow(), b.glow()));
            }
        }
        assertEquals(4, studs.size(), "all four stud patterns are used");
        assertTrue(metals.size() >= 3, "at least three metals");
        assertTrue(minCover >= 14, "closest two covers (" + worst + ") differ by only " + minCover);
        assertTrue(minGlow >= 5, "closest two glows differ by only " + minGlow);
    }

    @Test
    void bookLookUsesTheGeneratedDesign() {
        BookLook air = BookLook.resolve("THREE_LEAF", "AIR", null, 0), hair = BookLook.resolve("THREE_LEAF", "HAIR", null, 0);
        assertEquals(GrimoireDesignGenerator.forMagic("AIR").cover(), air.coverColor());
        assertEquals(GrimoireDesignGenerator.forMagic("AIR").glow(), air.glowColor());
        assertNotEquals(air.coverColor(), hair.coverColor());
    }

    @Test
    void studsOnlyOnGeneratedBooksAndStayInsideTheCover() {
        int withStuds = 0;
        for (String m : WIKI) {
            int n = GrimoireBookPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false, 0, m)).size();
            int base = GrimoireBookPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false, 0, "FLAME")).size();
            if (GrimoireDesignGenerator.forMagic(m).studs() > 0) assertTrue(n > base, m);
            withStuds += GrimoireDesignGenerator.forMagic(m).studs() > 0 ? 1 : 0;
            for (var q : GrimoireBookPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false, 0, m)))
                for (int i = 0; i < 4; i++) for (int k = 0; k < 3; k++) assertTrue(q.pos()[i * 3 + k] >= -0.01f && q.pos()[i * 3 + k] <= 16.01f);
        }
        assertTrue(withStuds > 10);
        assertEquals(GrimoireBookPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false, 0, "FLAME")).size(),
                GrimoireBookPlan.build(new BookLook.Key("THREE_LEAF", BookMotif.THREE_LEAF, false, false, 0, "")).size(), "hand-made books are untouched");
    }
}
