package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the merge of the diver with the spirit is drawn by a client painter nobody can run here, so everything it reads is a pure function that is pinned: the clock (forward on the entry, backwards on
 * the exit), the path of the spirit (finite, hovers beside the wearer, spirals into the chest), the strengths (the ghost fades in and is gone at the contact, the flash peaks at the contact, the
 * inner silhouette never above 12 percent), the budgets (a ghost mesh at most 700 vertices, the silhouette at most 300, nothing culled, the steady state is the silhouette alone and only at full detail)
 * and the tables (every element has a spirit, the character tier is the dive's own tier and never Earth).
 */
class SpiritDiveMergeTest {
    @Test
    void theClockRunsForwardsOnTheEntryAndBackwardsOnTheExit() {
        assertEquals(0f, SpiritDiveMergeRules.clock(0f, 1000f), 1e-6f);
        assertEquals(11f, SpiritDiveMergeRules.clock(11f, 1000f), 1e-6f);
        assertEquals(22f, SpiritDiveMergeRules.clock(400f, 1000f), 1e-6f);
        assertEquals(22f, SpiritDiveMergeRules.clock(400f, 20f), 1e-6f);
        assertEquals(11f, SpiritDiveMergeRules.clock(400f, 10f), 1e-6f);
        assertEquals(0f, SpiritDiveMergeRules.clock(400f, 0f), 1e-6f);
        assertFalse(SpiritDiveMergeRules.exiting(5f, 1000f));
        assertTrue(SpiritDiveMergeRules.exiting(400f, 10f));
        assertEquals(0f, SpiritDiveMergeRules.clock(-5f, -5f), 1e-6f);
        float prev = -1f;
        for (float t = 0; t <= 30; t += 0.5f) { float m = SpiritDiveMergeRules.clock(t, 1000f); assertTrue(m >= prev); prev = m; }
    }

    @Test
    void thePathIsFiniteHoversBesideTheWearerAndEndsAtTheChest() {
        float[] p = new float[4];
        for (float side : new float[]{1f, -1f})
            for (boolean ch : new boolean[]{false, true})
                for (float m = -5f; m <= 40f; m += 0.25f) {
                    SpiritDiveMergeRules.path(m, side, ch, p);
                    for (float v : p) assertTrue(Float.isFinite(v), "path at " + m);
                    assertTrue(Math.abs(p[0]) <= SpiritDiveMergeRules.radius(true) + 1f && Math.abs(p[2]) <= SpiritDiveMergeRules.radius(true) + 1f);
                    assertTrue(p[1] > -20f && p[1] < 24f, "it stays between above the head and the feet: " + p[1]);
                }
        SpiritDiveMergeRules.path(4f, 1f, false, p);
        assertTrue(Math.hypot(p[0], p[2]) > 20f, "phase 1 hovers beside the wearer");
        SpiritDiveMergeRules.path(SpiritDiveMergeRules.FLOW_END, 1f, false, p);
        assertTrue(Math.hypot(p[0] - SpiritDiveMergeRules.CHEST_X, p[2] - SpiritDiveMergeRules.CHEST_Z) < 4f, "the spirit ends at the chest");
        assertTrue(Math.abs(p[1] - SpiritDiveMergeRules.CHEST_Y) < 2f);
        // the radius only closes during the flow
        float last = 1e9f;
        for (float m = 8f; m <= 18f; m += 0.5f) {
            SpiritDiveMergeRules.path(m, 1f, true, p);
            float r = (float) Math.hypot(p[0], p[2]);
            assertTrue(r <= last + 1e-3f, "the spiral closes at " + m);
            last = r;
        }
        SpiritDiveMergeRules.path(3f, 1f, false, p);
        float x1 = p[0];
        SpiritDiveMergeRules.path(3f, -1f, false, p);
        assertEquals(-x1, p[0], 1e-3f, "the other side mirrors");
        assertEquals(1f, SpiritDiveMergeRules.side(0), 0f);
        assertEquals(-1f, SpiritDiveMergeRules.side(1), 0f);
    }

    @Test
    void theStrengthsFollowTheStory() {
        assertEquals(0f, SpiritDiveMergeRules.ghostAlpha(0f), 1e-6f);
        assertTrue(SpiritDiveMergeRules.ghostAlpha(4f) > 0.3f && SpiritDiveMergeRules.ghostAlpha(4f) < 0.7f);
        assertEquals(1f, SpiritDiveMergeRules.ghostAlpha(8f), 1e-3f);
        assertTrue(SpiritDiveMergeRules.ghostAlpha(SpiritDiveMergeRules.FLOW_END) < 0.01f, "gone at the contact");
        assertTrue(SpiritDiveMergeRules.ghostScale(SpiritDiveMergeRules.FLOW_END) < 0.2f && SpiritDiveMergeRules.ghostScale(8f) > 0.99f && SpiritDiveMergeRules.ghostScale(0f) < 0.8f, "it grows in, then shrinks into the chest");
        float peak = 0f, at = 0f;
        for (float m = 0f; m <= 22f; m += 0.1f) if (SpiritDiveMergeRules.flash(m) > peak) { peak = SpiritDiveMergeRules.flash(m); at = m; }
        assertEquals(SpiritDiveMergeRules.FLASH_AT, at, 0.15f);
        assertEquals(0f, SpiritDiveMergeRules.flash(10f), 1e-6f);
        assertEquals(0f, SpiritDiveMergeRules.flash(19f), 1e-6f);
        assertEquals(0f, SpiritDiveMergeRules.bodyGlow(12f), 1e-6f);
        assertEquals(1f, SpiritDiveMergeRules.bodyGlow(SpiritDiveMergeRules.FLASH_AT), 1e-3f);
        assertEquals(0f, SpiritDiveMergeRules.bodyGlow(22f), 1e-6f);
        assertEquals(0f, SpiritDiveMergeRules.inner(10f, 0f), 1e-6f);
        for (float age = 0f; age < 200f; age += 0.7f)
            for (float m = 0f; m <= 22f; m += 0.5f) {
                float in = SpiritDiveMergeRules.inner(m, age) * SpiritDiveMergeRules.INNER_ALPHA;
                assertTrue(in >= 0f && in <= 0.12f, "the silhouette stays at or under 12 percent: " + in);
                float s = SpiritDiveMergeRules.strips(m), g = SpiritDiveMergeRules.ghostAlpha(m), r = SpiritDiveMergeRules.ring(m);
                assertTrue(s >= 0f && s <= 1.001f && g >= 0f && g <= 1.001f && r >= 0f && r <= 1.001f);
            }
        assertTrue(SpiritDiveMergeRules.INNER_ALPHA <= 0.12f);
        assertEquals(0f, SpiritDiveMergeRules.strips(2f), 1e-6f);
        assertFalse(SpiritDiveMergeRules.flowing(0f));
        assertTrue(SpiritDiveMergeRules.flowing(10f));
        assertFalse(SpiritDiveMergeRules.flowing(22f), "the steady state has no ghost");
    }

    @Test
    void theBudgetsHold() {
        int worstAny = 0;
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                SpiritDiveWind3Mesh ghost = SpiritDiveMergeMesh.get(SpiritDiveMergeRules.ghostMesh(e, ch));
                assertTrue(ghost.totalVerts() > 100 && ghost.totalVerts() <= SpiritDiveMergeRules.GHOST_VERTS, e + " ghost " + ghost.totalVerts());
                int w = SpiritDiveMergeRules.worst(e, ch);
                worstAny = Math.max(worstAny, w);
                assertTrue(w <= SpiritDiveMergeRules.GHOST_VERTS + 5 * SpiritDiveMergeRules.STRIP_SEGS * 4 + SpiritDiveMergeRules.FLASH_SPRITES * 4 + SpiritDiveMergeRules.COVER_VERTS + SpiritDiveMergeRules.INNER_VERTS);
                assertEquals(0, SpiritDiveMergeRules.vertices(e, ch, Lod.CULLED, 12f, true));
                assertEquals(0, SpiritDiveMergeRules.vertices(e, ch, Lod.FULL, 12f, false), "no ghost beyond 32 blocks");
                assertEquals(0, SpiritDiveMergeRules.vertices(e, ch, Lod.MID, 22f, true), "the steady state only at full detail");
                assertEquals(0, SpiritDiveMergeRules.vertices(e, ch, Lod.FAR, 22f, true));
                int steady = SpiritDiveMergeRules.vertices(e, ch, Lod.FULL, 22f, true);
                assertEquals(SpiritDiveMergeMesh.get(SpiritDiveMergeRules.coreMesh(e)).totalVerts(), steady, "the steady state is the silhouette alone");
                assertTrue(steady <= SpiritDiveMergeRules.INNER_VERTS);
            }
        assertTrue(worstAny <= 1300, "one merge is at most about 1,300 vertices (ghost 696 + strips 120 + contact 12 + cover 144 + silhouette 288): " + worstAny);
        assertEquals(3, SpiritDiveMergeRules.MERGES_PER_FRAME);
        assertEquals(32.0, SpiritDiveMergeRules.GHOST_RANGE, 0.0);
    }

    @Test
    void theTablesCoverEveryElementAndTheCharacterTierIsTheDivesOwn() {
        for (Element e : Element.values()) {
            assertFalse(SpiritDiveMergeRules.spirit(e).isEmpty());
            assertTrue(SpiritDiveMergeMesh.exists(SpiritDiveMergeRules.ghostMesh(e, false)) && SpiritDiveMergeMesh.exists(SpiritDiveMergeRules.ghostMesh(e, true)) && SpiritDiveMergeMesh.exists(SpiritDiveMergeRules.coreMesh(e)));
            assertTrue(SpiritDiveMergeRules.ghostSize(e, true) >= SpiritDiveMergeRules.ghostSize(e, false), "the character spirit is larger");
            assertTrue(SpiritDiveMergeRules.strips(true) > SpiritDiveMergeRules.strips(false));
            for (int sh = 0; sh <= 16; sh += 8) assertTrue((SpiritDiveMergeRules.flashRgb(e) >> sh & 255) >= (SpiritDiveMergeRules.rgb(e) >> sh & 255), "the flash is the colour mixed toward white");
        }
        assertEquals("Sylph", SpiritDiveMergeRules.spirit(Element.WIND));
        assertEquals("Undine", SpiritDiveMergeRules.spirit(Element.WATER));
        assertEquals("Salamander", SpiritDiveMergeRules.spirit(Element.FIRE));
        assertEquals("Gnome", SpiritDiveMergeRules.spirit(Element.EARTH));
        assertTrue(SpiritDiveMergeRules.character(Element.WIND, Grade.YUNO));
        assertTrue(SpiritDiveMergeRules.character(Element.FIRE, Grade.FUEGOLEON));
        assertTrue(SpiritDiveMergeRules.character(Element.WATER, Grade.NOELLE));
        assertFalse(SpiritDiveMergeRules.character(Element.EARTH, Grade.YUNO));
        assertFalse(SpiritDiveMergeRules.character(Element.WIND, Grade.STANDARD));
        assertEquals("wind_char", SpiritDiveMergeRules.ghostMesh(Element.WIND, true));
        assertEquals("earth_std", SpiritDiveMergeRules.ghostMesh(Element.EARTH, true), "Earth has no character mesh: the Gnome");
        assertTrue(SpiritDiveMergeRules.DEFAULT_MERGE);
    }
}
