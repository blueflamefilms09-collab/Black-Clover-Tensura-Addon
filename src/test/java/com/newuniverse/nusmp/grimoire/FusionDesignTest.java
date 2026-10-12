package com.newuniverse.nusmp.grimoire;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 1.16.0: the generated look of a fused (two-attribute) grimoire. */
class FusionDesignTest {
    private static final String[] NAMES = {"FLAME", "WATER", "WIND", "EARTH", "LIGHT", "DARK", "TIME", "SPATIAL", "PLANT", "BLOOD", "CREATION", "ICE"};

    @Test
    void parentsAreReadFromTheFusionString() {
        assertArrayEquals(new String[]{"FLAME", "WATER"}, FusionDesign.parents("FLAME:WATER"));
        assertNull(FusionDesign.parents(""));
        assertNull(FusionDesign.parents(null));
        assertNull(FusionDesign.parents("FLAME"));
        assertNull(FusionDesign.parents("FLAME:"));
    }

    @Test
    void theSamePairAlwaysHasTheSameDesign() {
        assertEquals(FusionDesign.of("FLAME:WATER"), FusionDesign.of("FLAME:WATER"));
        assertNull(FusionDesign.of("FLAME"));
    }

    @Test
    void pairsHaveVariedDesignsWithinTheirRanges() {
        Set<FusionDesign.Spec> seen = new HashSet<>();
        for (String a : NAMES) {
            for (String b : NAMES) {
                if (a.equals(b)) continue;
                FusionDesign.Spec s = FusionDesign.of(a + ":" + b);
                assertTrue(s.gems() >= 5 && s.gems() <= 8, s.toString());
                assertTrue(s.style() >= 0 && s.style() <= 2, s.toString());
                assertTrue(s.ring() >= 3.5f && s.ring() <= 4.0f, s.toString());
                assertTrue(s.bracketLen() >= 1.4f && s.bracketLen() <= 2.7f, s.toString());
                assertTrue(s.spineGems() >= 3 && s.spineGems() <= 5, s.toString());
                seen.add(s);
            }
        }
        assertTrue(seen.size() > 40, "designs are varied: " + seen.size());
    }

    @Test
    void aFusedBookBlendsBothParentsColoursAndGrowsGeometry() {
        BookLook plain = BookLook.resolve("THREE_LEAF", "GLASS", null, 0);
        BookLook fused = BookLook.resolve("THREE_LEAF", "GLASS", null, 0, "FLAME:WATER");
        assertEquals("", plain.fusion());
        assertEquals("FLAME:WATER", fused.fusion());
        assertEquals(BookPalette.mix(BookPalette.cover("FLAME"), BookPalette.cover("WATER"), 0.5f), fused.coverColor());
        assertEquals(BookPalette.trim("WATER"), fused.trimColor());
        assertNotEquals(plain.coverColor(), fused.coverColor());
        for (boolean held : new boolean[]{false, true}) {
            int base = GrimoireBookPlan.build(plain.key(held)).size(), more = GrimoireBookPlan.build(fused.key(held)).size();
            assertTrue(more > base + 40, "the filigree adds quads: " + base + " -> " + more);
            for (var q : GrimoireBookPlan.build(fused.key(held))) assertTrue(q.tint() >= -1 && q.tint() <= 2, q.texture());
        }
        for (int open = 1; open <= GrimoireBookPlan.OPEN_STEPS; open++)
            assertTrue(GrimoireBookPlan.build(fused.key(false, open)).size() > GrimoireBookPlan.build(plain.key(false, open)).size());
    }

    @Test
    void canonBooksAndSingleAttributeBooksKeepTheirLook() {
        BookLook a = BookLook.resolve("THREE_LEAF", "FLAME", null, 0, "");
        assertEquals(BookLook.resolve("THREE_LEAF", "FLAME", null, 0), a);
        assertEquals("", BookLook.resolve("THREE_LEAF", "FLAME", null, 0, "garbage").fusion());
    }
}
