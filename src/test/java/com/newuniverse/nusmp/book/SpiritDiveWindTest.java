package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the PACK Wind of the Spirit Dive can only be seen in a game, so its numbers are pinned here: it never costs more than the REEL Wind it replaces (every tier, level of detail and stage), the
 * tables are inside the caps of the look, the crown has the bars of the pack, the geometry is finite and bounded, the option parses and keeps REEL reachable, the weapon rule picks the sword at rest and
 * the bow in a cast, and the half dive (left side only) draws less than the full one.
 */
class SpiritDiveWindTest {
    @Test
    void neverDearerThanTheReelWindAtAnyTierLevelAndStage() {
        for (boolean ch : new boolean[]{false, true})
            for (Stage st : Stage.values())
                for (Lod lod : Lod.values()) {
                    int v = SpiritDiveWindRules.vertices(ch, lod, st), reel = SpiritDiveReelRules.vertices(Element.WIND, ch, lod, st);
                    assertTrue(v <= reel, (ch ? "Yuno " : "standard ") + st + " " + lod + ": " + v + " > REEL Wind " + reel);
                }
    }

    @Test
    void withinTheCapsOfTheLookAndUnderTheV2Worst() {
        assertTrue(SpiritDiveWindRules.worstVertices(true) <= SpiritDiveReelRules.CAP_CHARACTER);
        assertTrue(SpiritDiveWindRules.worstVertices(false) <= SpiritDiveReelRules.CAP_STANDARD);
        assertTrue(SpiritDiveWindRules.worstVertices(true) < SpiritDiveReelRules.V2_WORST_VERTICES);
        assertTrue(SpiritDiveWindRules.worstVertices(true) >= SpiritDiveWindRules.worstVertices(false), "Yuno draws at least what the standard tier draws");
        assertEquals(0, SpiritDiveWindRules.vertices(true, Lod.CULLED, Stage.FULL));
    }

    @Test
    void theFarLevelIsTheOutlineAndTheGroundRingOnly() {
        int far = SpiritDiveWindRules.vertices(false, Lod.FAR, Stage.FULL), mid = SpiritDiveWindRules.vertices(false, Lod.MID, Stage.FULL), full = SpiritDiveWindRules.vertices(false, Lod.FULL, Stage.FULL);
        assertTrue(far < mid && mid < full, far + " < " + mid + " < " + full);
        assertEquals(SpiritDiveAnimeRules.shellVertices(Element.WIND, Stage.FULL, Lod.FAR) + SpiritDiveWindRules.RING_VERTS, far);
    }

    @Test
    void theHalfDiveDrawsTheLeftSideOnly() {
        for (boolean ch : new boolean[]{false, true}) {
            assertTrue(SpiritDiveWindRules.leaves(ch, Lod.FULL, Stage.HALF) < SpiritDiveWindRules.leaves(ch, Lod.FULL, Stage.FULL));
            assertEquals(ch ? 2 : 1, SpiritDiveWindRules.leaves(ch, Lod.FULL, Stage.HALF));
            assertTrue(SpiritDiveWindRules.vertices(ch, Lod.FULL, Stage.HALF) < SpiritDiveWindRules.vertices(ch, Lod.FULL, Stage.FULL));
        }
        assertEquals(2, SpiritDiveWindRules.leaves(false, Lod.FULL, Stage.FULL), "standard: the big leaf and the small one");
        assertEquals(4, SpiritDiveWindRules.leaves(true, Lod.FULL, Stage.FULL), "Yuno: both on both sides");
        assertTrue(SpiritDiveWindGeometry.barCount(SpiritDiveWindGeometry.BARS_MAX, true) < SpiritDiveWindGeometry.barCount(SpiritDiveWindGeometry.BARS_MAX, false));
    }

    @Test
    void theCrownIsTallBarsOnAThinRing() {
        assertEquals(SpiritDiveWindGeometry.BARS_MAX, SpiritDiveWindGeometry.BARS.length);
        assertTrue(SpiritDiveWindGeometry.BARS_STANDARD < SpiritDiveWindGeometry.BARS_MAX);
        for (float[] b : SpiritDiveWindGeometry.BARS) {
            assertTrue(Math.abs(b[0]) <= SpiritDiveWindGeometry.RING_HALF + 1e-3f && Math.abs(b[1]) <= SpiritDiveWindGeometry.RING_HALF + 1e-3f, "a bar stands on the ring");
            assertTrue(Math.max(Math.abs(b[0]), Math.abs(b[1])) > SpiritDiveWindGeometry.RING_HALF - 1e-3f, "a bar stands ON the ring, not inside it");
            assertTrue(b[2] >= 4f && b[2] <= 12f, "a bar is tall but not a pole: " + b[2]);
        }
        assertTrue(SpiritDiveWindGeometry.RING_THICK < 2f, "a thin ring, not a block");
        assertTrue(SpiritDiveWindGeometry.RING_Y < -8f, "the ring floats above the head");
        assertEquals(SpiritDiveWindGeometry.BARS_STANDARD, SpiritDiveWindRules.spec(false, Lod.FULL).bars());
        assertEquals(SpiritDiveWindGeometry.BARS_MAX, SpiritDiveWindRules.spec(true, Lod.FULL).bars());
        assertEquals(1, SpiritDiveWindRules.spec(false, Lod.FULL).rings());
        assertEquals(2, SpiritDiveWindRules.spec(true, Lod.FULL).rings());
    }

    @Test
    void oneBigLeafAndASmallerOne() {
        float[] big = SpiritDiveWindGeometry.leaf(SpiritDiveWindGeometry.BIG), small = SpiritDiveWindGeometry.leaf(SpiritDiveWindGeometry.SMALL);
        assertTrue(big[9] > small[9] * 1.5f && big[10] > small[10] * 1.5f, "the big leaf is clearly bigger");
        assertEquals(1, SpiritDiveWindRules.leafMask(false, Lod.FULL, true), "standard: the big leaf on the left");
        assertEquals(2, SpiritDiveWindRules.leafMask(false, Lod.FULL, false), "standard: the small leaf on the right");
        assertEquals(3, SpiritDiveWindRules.leafMask(true, Lod.FULL, true));
        assertEquals(0, SpiritDiveWindRules.leafMask(true, Lod.FAR, true));
        assertEquals(1, SpiritDiveWindRules.crossesDrawn(false, Lod.FULL, Stage.HALF), "the cross sits on the big leaf");
        assertEquals(2, SpiritDiveWindRules.crossesDrawn(true, Lod.FULL, Stage.FULL));
        assertEquals(0, SpiritDiveWindRules.crossesDrawn(false, Lod.MID, Stage.FULL), "no cross at the middle level of detail");
    }

    @Test
    void theGeometryIsFiniteAndBounded() {
        float[] spine = new float[(SpiritDiveReelGeometry.MAX_SEGS + 1) * 3];
        for (int r = 0; r < SpiritDiveWindGeometry.leafRows(); r++)
            for (float open : new float[]{0f, 0.3f, 1f, 1.12f})
                for (float swing : new float[]{0f, 1f}) {
                    SpiritDiveReelGeometry.leafSpine(spine, 10, SpiritDiveWindGeometry.leaf(r), -1f, SpiritDiveWindGeometry.LEAF_CHARACTER, open, 77f, swing);
                    for (int i = 0; i < 33; i++) assertTrue(Float.isFinite(spine[i]) && Math.abs(spine[i]) < 120f, "leaf " + r + " point " + i + " = " + spine[i]);
                }
        for (int i = 0; i < 3; i++)
            for (float age : new float[]{0f, 50f, 1e5f}) {
                SpiritDiveWindGeometry.ribbonSpine(spine, 12, i, 3, age);
                for (int k = 0; k < 39; k++) assertTrue(Float.isFinite(spine[k]) && Math.abs(spine[k]) < 60f, "ribbon " + i + " point " + k + " = " + spine[k]);
            }
        assertTrue(SpiritDiveWindGeometry.reach() < 110f, "reach " + SpiritDiveWindGeometry.reach());
        assertTrue(SpiritDiveWindGeometry.SWORD[6] > 40f, "the sword is long (the pack's is longer than the arm)");
        assertTrue(SpiritDiveWindRules.spec(true, Lod.FULL).leafSegs() <= SpiritDiveReelGeometry.MAX_SEGS && SpiritDiveWindRules.spec(true, Lod.FULL).ribbonSegs() <= SpiritDiveReelGeometry.MAX_SEGS);
    }

    @Test
    void theCharacterTierAddsTheCircleTheWeaponAndTheSparkles() {
        SpiritDiveWindRules.Spec std = SpiritDiveWindRules.spec(false, Lod.FULL), yuno = SpiritDiveWindRules.spec(true, Lod.FULL);
        assertFalse(std.backCircle() || std.weapon());
        assertTrue(yuno.backCircle() && yuno.weapon());
        assertTrue(yuno.sparks() > std.sparks() && yuno.wrists() > std.wrists() && yuno.ribbons() > std.ribbons());
        for (Lod lod : new Lod[]{Lod.MID, Lod.FAR}) assertFalse(SpiritDiveWindRules.spec(true, lod).weapon() || SpiritDiveWindRules.spec(true, lod).backCircle(), "no character parts beyond 24 blocks");
    }

    @Test
    void theWeaponIsTheSwordAtRestAndTheBowInACast() {
        assertEquals(SpiritDiveWindRules.Weapon.SWORD, SpiritDiveWindRules.weapon(false));
        assertEquals(SpiritDiveWindRules.Weapon.BOW, SpiritDiveWindRules.weapon(true));
        assertTrue(Math.max(SpiritDiveWindRules.SWORD_VERTICES, SpiritDiveWindRules.BOW_VERTICES) <= 20);
    }

    @Test
    void theOptionParsesAndKeepsTheReelWindReachable() {
        assertEquals(SpiritDiveWindRules.WindLook.A_ANIME, SpiritDiveWindRules.DEFAULT_LOOK, "4.x final: A_ANIME is the default Wind");
        assertEquals(SpiritDiveWindRules.DEFAULT_LOOK, SpiritDiveWindRules.parse(null));
        assertEquals(SpiritDiveWindRules.DEFAULT_LOOK, SpiritDiveWindRules.parse("garbage"));
        assertEquals(SpiritDiveWindRules.WindLook.REEL, SpiritDiveWindRules.parse(" reel "));
        assertEquals(SpiritDiveWindRules.WindLook.PACK, SpiritDiveWindRules.parse("PACK"));
        assertEquals("[PACK, REEL, A_ANIME, B_CLIP, C_SAINT]", java.util.Arrays.toString(SpiritDiveWindRules.WindLook.values()), "append only");
    }

    @Test
    void thePaletteIsGreenAndWhiteNeverBlue() {
        for (boolean ch : new boolean[]{false, true}) {
            SpiritDiveReelRules.Pal p = SpiritDiveWindRules.pal(ch);
            for (int rgb : new int[]{p.body(), p.glow(), p.light(), p.rim(), p.gold()}) {
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                assertTrue(g >= r && g >= b, Integer.toHexString(rgb) + " is green-white");
            }
        }
        int r = SpiritDiveWindRules.CAGE_RGB >> 16 & 255, g = SpiritDiveWindRules.CAGE_RGB >> 8 & 255, b = SpiritDiveWindRules.CAGE_RGB & 255;
        assertTrue(g >= r && g >= b);
        assertTrue(SpiritDiveWindRules.character(Grade.YUNO) && !SpiritDiveWindRules.character(Grade.STANDARD) && !SpiritDiveWindRules.character(Grade.NOELLE));
    }
}
