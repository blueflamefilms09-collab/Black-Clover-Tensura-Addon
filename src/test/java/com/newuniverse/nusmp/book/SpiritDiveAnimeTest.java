package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry.Shape;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules.Spec;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the ANIME Spirit Dive (the default look since 3.2) cannot be seen in a test, so its decisions are pinned where they are pure: which look a client draws and what the option parses to, the
 * tier of every element and grade, the entry and exit timeline, the budget arithmetic (every vertex of every dive is counted from the tables and kept under the 3.0.0 V2 dive's worst case), and the
 * ribbon geometry (every spine finite and inside its bound at every moment, every strip with a positive width, the winding rule). The painters themselves are read by SpiritDiveAnimeScanTest.
 */
class SpiritDiveAnimeTest {

    // ------------------------------------------------------------------ the look option
    @Test
    void animeIsTheDefaultLookAndTheOldLooksAreStillThere() {
        assertEquals(Look.ANIME, SpiritDiveDjinnPieces.DEFAULT_LOOK);
        assertEquals(Look.ANIME, SpiritDiveAnimeRules.DEFAULT_LOOK);
        assertEquals(Look.ANIME, SpiritDiveLook.look(), "a client that never read its config");
        assertEquals("[DJINN, V2, CLASSIC, ANIME]", java.util.Arrays.toString(Look.values()), "the option values are append only: ANIME was appended");
        assertEquals(Look.ANIME, SpiritDiveAnimeRules.parse(null));
        assertEquals(Look.ANIME, SpiritDiveAnimeRules.parse("nonsense"));
        assertEquals(Look.DJINN, SpiritDiveAnimeRules.parse("djinn"));
        assertEquals(Look.V2, SpiritDiveAnimeRules.parse(" V2 "));
        assertEquals(Look.CLASSIC, SpiritDiveAnimeRules.parse("CLASSIC"));
        assertEquals(Look.ANIME, SpiritDiveAnimeRules.parse("anime"));
    }

    @Test
    void theLookFlagsFollowTheOption() {
        try {
            SpiritDiveLook.set(Look.ANIME, true, true, com.newuniverse.nusmp.book.FuegoleonArmRules.View.ALWAYS);
            assertTrue(SpiritDiveLook.anime());
            assertFalse(SpiritDiveLook.classic());
            assertFalse(SpiritDiveLook.v2());
            for (Look l : new Look[]{Look.DJINN, Look.V2, Look.CLASSIC}) {
                SpiritDiveLook.set(l, true, true, com.newuniverse.nusmp.book.FuegoleonArmRules.View.ALWAYS);
                assertFalse(SpiritDiveLook.anime(), l + " is not the anime look");
                assertEquals(l, SpiritDiveLook.look());
            }
            SpiritDiveLook.set((Look) null, true, true, null);
            assertEquals(Look.ANIME, SpiritDiveLook.look(), "a missing value is the default");
        } finally {
            SpiritDiveLook.set(Look.ANIME, true, true, com.newuniverse.nusmp.book.FuegoleonArmRules.View.ALWAYS);
        }
    }

    // ------------------------------------------------------------------ tiers
    @Test
    void theCharacterTierBelongsToTheCharactersOfWindFireAndWaterOnly() {
        assertTrue(SpiritDiveAnimeRules.character(Element.WIND, Grade.YUNO));
        assertTrue(SpiritDiveAnimeRules.character(Element.FIRE, Grade.FUEGOLEON));
        assertTrue(SpiritDiveAnimeRules.character(Element.WATER, Grade.NOELLE));
        assertFalse(SpiritDiveAnimeRules.character(Element.WIND, Grade.NOELLE), "Noelle's grimoire on Wind is the standard tier");
        assertFalse(SpiritDiveAnimeRules.character(Element.FIRE, Grade.YUNO));
        for (Grade g : Grade.values()) assertFalse(SpiritDiveAnimeRules.character(Element.EARTH, g), "Earth has no canon character");
        for (Element e : Element.values()) assertFalse(SpiritDiveAnimeRules.character(e, Grade.STANDARD));
    }

    @Test
    void theCharacterTierHasMoreOfEverythingAndAllTheExtras() {
        Spec s = SpiritDiveAnimeRules.spec(false, Lod.FULL), c = SpiritDiveAnimeRules.spec(true, Lod.FULL);
        assertTrue(c.plumeRows() > s.plumeRows() && c.locks() > s.locks() && c.halos() > s.halos() && c.trails() > 0 && s.trails() == 0);
        assertTrue(c.backdrop() && !s.backdrop(), "the spirit behind the wearer is the character tier's");
        assertTrue(s.eyes() && s.cap() && s.halos() >= 1 && s.plumeRows() >= 2, "the standard tier has hair, eyes, a halo and plumes (A, B, C, D simplified, no E)");
    }

    @Test
    void theLevelsOfDetailThinTheLookOutAndCullBeyondRange() {
        for (boolean ch : new boolean[]{false, true}) {
            Spec f = SpiritDiveAnimeRules.spec(ch, Lod.FULL), m = SpiritDiveAnimeRules.spec(ch, Lod.MID), far = SpiritDiveAnimeRules.spec(ch, Lod.FAR), cull = SpiritDiveAnimeRules.spec(ch, Lod.CULLED);
            assertTrue(f.plumeSegs() >= m.plumeSegs() && f.locks() >= m.locks() && f.plumeLayers() >= m.plumeLayers());
            assertEquals(0, far.plumeRows() + far.locks() + far.trails() + far.halos(), "far: the aura outline only");
            assertFalse(far.eyes() || far.cap() || far.backdrop());
            assertEquals(0, cull.plumeRows() + cull.locks());
        }
        assertEquals(3, SpiritDiveAnimeRules.shellPasses(Lod.FULL));
        assertEquals(2, SpiritDiveAnimeRules.shellPasses(Lod.MID));
        assertEquals(1, SpiritDiveAnimeRules.shellPasses(Lod.FAR));
        assertEquals(0, SpiritDiveAnimeRules.shellPasses(Lod.CULLED));
        assertEquals(Lod.FULL, SpiritDiveTier.lod(24.0 * 24.0));
        assertEquals(Lod.MID, SpiritDiveTier.lod(48.0 * 48.0));
        assertEquals(Lod.FAR, SpiritDiveTier.lod(64.0 * 64.0));
        assertEquals(Lod.CULLED, SpiritDiveTier.lod(64.1 * 64.1), "culled beyond 64 blocks");
        assertEquals(3, SpiritDiveTier.CHARACTER_PER_FRAME, "at most three character tier dives a frame");
    }

    // ------------------------------------------------------------------ entry and exit
    @Test
    void theEntryIsEighteenTicksAndTheExitReversesIt() {
        assertEquals(18, SpiritDiveAnimeRules.ENTRY_TICKS);
        assertEquals(0f, SpiritDiveAnimeRules.progress(0f, 100f), 1e-6f);
        assertEquals(0.5f, SpiritDiveAnimeRules.progress(9f, 100f), 1e-6f);
        assertEquals(1f, SpiritDiveAnimeRules.progress(18f, 100f), 1e-6f);
        assertEquals(1f, SpiritDiveAnimeRules.progress(60f, 60f), 1e-6f);
        assertEquals(0.5f, SpiritDiveAnimeRules.progress(100f, 9f), 1e-6f, "the exit mirrors the entry");
        assertEquals(0f, SpiritDiveAnimeRules.progress(100f, 0f), 1e-6f);
        float last = -1f;
        for (int t = 0; t <= 18; t++) {
            float p = SpiritDiveAnimeRules.progress(t, 1000f);
            assertTrue(p >= last, "the progress only grows during the entry");
            last = p;
        }
    }

    @Test
    void theShellGrowsFromTheFeetUp() {
        for (float p = 0f; p <= 1.0001f; p += 0.05f) {
            assertTrue(SpiritDiveAnimeRules.partAlpha(p, 0) >= SpiritDiveAnimeRules.partAlpha(p, 1), "legs before body at " + p);
            assertTrue(SpiritDiveAnimeRules.partAlpha(p, 1) >= SpiritDiveAnimeRules.partAlpha(p, 2), "body before head at " + p);
        }
        assertEquals(0f, SpiritDiveAnimeRules.partAlpha(0f, 0), 1e-6f);
        for (int order = 0; order < 3; order++) assertEquals(1f, SpiritDiveAnimeRules.partAlpha(1f, order), 1e-6f, "everything is on at the end of the entry");
    }

    @Test
    void theEntryEffectsStayInTheirRanges() {
        assertEquals(1f, SpiritDiveAnimeRules.flash(0f), 1e-6f);
        assertEquals(0f, SpiritDiveAnimeRules.flash(6f), 1e-6f);
        assertEquals(0f, SpiritDiveAnimeRules.ringPulse(0f));
        assertEquals(0f, SpiritDiveAnimeRules.ringPulse(15f));
        assertTrue(SpiritDiveAnimeRules.ringPulse(9f) > 0f);
        assertEquals(0f, SpiritDiveAnimeRules.burst(0f));
        assertEquals(0f, SpiritDiveAnimeRules.burst(12f));
        for (float p = 0f; p <= 1f; p += 0.02f) {
            float lift = SpiritDiveAnimeRules.hairLift(p), open = SpiritDiveAnimeRules.openScale(p);
            assertTrue(lift >= 0f && lift <= 1.13f, "hair lift " + lift);
            assertTrue(open >= 0.14f && open <= 1.13f, "open scale " + open);
        }
        assertEquals(1f, SpiritDiveAnimeRules.hairLift(1f), 1e-5f);
        assertEquals(1f, SpiritDiveAnimeRules.openScale(1f), 1e-5f);
    }

    // ------------------------------------------------------------------ the shell
    @Test
    void theSwirlStepsStayInTheCachedRangeAndTurnAtTheirOwnSpeed() {
        Set<Integer> seen = new HashSet<>();
        for (Element e : Element.values()) {
            for (int t = 0; t < 400; t++) {
                int step = SpiritDiveAnimeRules.swirlStep(e, t + 0.37f);
                assertTrue(step >= 0 && step < SpiritDiveAnimeRules.SWIRL_STEPS, "step " + step);
                seen.add(step);
                float v = SpiritDiveAnimeRules.swirlV(e, step);
                assertTrue(Math.abs(v) < 1f);
            }
            assertEquals(SpiritDiveAnimeRules.SWIRL_STEPS, seen.size(), "every step is reached");
            assertEquals(SpiritDiveAnimeRules.swirlStep(e, 5.3f), SpiritDiveAnimeRules.swirlStep(e, 5.3f + SpiritDiveAnimeRules.SWIRL_PERIOD[e.ordinal()]), "a full period comes back to the same step");
            seen.clear();
        }
        assertTrue(SpiritDiveAnimeRules.SWIRL_DIR[Element.FIRE.ordinal()] < 0 && SpiritDiveAnimeRules.SWIRL_DIR[Element.WIND.ordinal()] < 0, "fire and wind flow up the body");
        assertEquals(24, SpiritDiveAnimeRules.SWIRL_STEPS, "at most 4 x 24 cached render types for the life of the client");
    }

    @Test
    void theShellPartsFollowTheStage() {
        assertEquals(2, SpiritDiveAnimeRules.shellModelParts(Element.WIND, Stage.HALF), "the first dive of Wind: the left arm and the left leg");
        assertEquals(5, SpiritDiveAnimeRules.shellModelParts(Element.WIND, Stage.FULL));
        assertEquals(5, SpiritDiveAnimeRules.shellModelParts(Element.WATER, Stage.UPGRADE));
        assertEquals(6 * SpiritDiveAnimeRules.PART_VERTICES, SpiritDiveAnimeRules.PART_BOX.length * SpiritDiveAnimeRules.PART_VERTICES);
    }

    // ------------------------------------------------------------------ the budget
    /** An independent count of the vertices of one dive from the tables, the way the painter draws it. */
    private static int count(Element e, boolean ch, Lod lod, Stage st) {
        Spec s = SpiritDiveAnimeRules.spec(ch, lod);
        int n = SpiritDiveAnimeRules.shellModelParts(e, st), heads = SpiritDivePieces.full(st) ? 1 : 0;
        int shell = lod == Lod.FULL ? 4 * n + heads : lod == Lod.MID ? 3 * n + heads : lod == Lod.FAR ? n : 0;
        int v = shell * 24;
        int sides = SpiritDivePieces.halfBody(e, st) ? 1 : 2, rows = Math.min(s.plumeRows(), SpiritDiveAnimeGeometry.rows(e, ch));
        v += rows * sides * s.plumeLayers() * s.plumeSegs() * 4 * 2;
        v += s.locks() * s.lockSegs() * 4 * 2 + s.trails() * s.trailSegs() * 4 * 2;
        if (s.cap()) v += 24;
        if (s.eyes()) v += 16;
        v += s.halos() * 4 + (s.ground() ? 8 : 0);
        if (s.backdrop()) v += SpiritDiveAnimeRules.BACKDROP_QUADS[e.ordinal()] * 4;
        if (lod == Lod.FULL) v += SpiritDiveAnimeRules.ENTRY_QUADS * 4;
        return v;
    }

    @Test
    void everyDiveIsCountedAndStaysUnderTheV2Worst() {
        int worstChar = 0, worstStd = 0;
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == Grade.STANDARD) continue;
                for (Stage st : Stage.values()) {
                    int last = Integer.MAX_VALUE;
                    for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR, Lod.CULLED}) {
                        int v = SpiritDiveAnimeRules.vertices(e, ch, lod, st);
                        assertEquals(count(e, ch, lod, st), v, e + "/" + st + "/" + lod + (ch ? "/character" : "/standard"));
                        assertTrue(v <= last, "a farther level never draws more: " + e + " " + lod);
                        last = v;
                        if (lod == Lod.CULLED) assertEquals(0, v);
                    }
                    int full = SpiritDiveAnimeRules.vertices(e, ch, Lod.FULL, st);
                    if (ch) worstChar = Math.max(worstChar, full); else worstStd = Math.max(worstStd, full);
                    assertTrue(full < SpiritDiveAnimeRules.V2_WORST_VERTICES, e + " " + st + ": " + full + " >= the V2 dive's " + SpiritDiveAnimeRules.V2_WORST_VERTICES);
                }
            }
        assertEquals(worstChar, SpiritDiveAnimeRules.worstVertices(true));
        assertEquals(worstStd, SpiritDiveAnimeRules.worstVertices(false));
        assertTrue(worstChar <= SpiritDiveAnimeRules.CAP_CHARACTER, "character tier worst " + worstChar);
        assertTrue(worstStd <= SpiritDiveAnimeRules.CAP_STANDARD, "standard tier worst " + worstStd);
        assertTrue(worstChar < SpiritDiveAnimeRules.V2_WORST_VERTICES && worstChar * 10 < SpiritDiveAnimeRules.V2_WORST_VERTICES * 9, "the character dive is at least 10 percent under V2");
    }

    @Test
    void aCrowdOfDiversIsBoundedByTheCharacterCapPerFrame() {
        int frame = SpiritDiveTier.CHARACTER_PER_FRAME * SpiritDiveAnimeRules.worstVertices(true) + 50 * SpiritDiveAnimeRules.worstVertices(false);
        assertTrue(frame < 3 * SpiritDiveAnimeRules.CAP_CHARACTER + 50 * SpiritDiveAnimeRules.CAP_STANDARD, "three character dives and fifty standard ones: " + frame + " vertices");
    }

    // ------------------------------------------------------------------ colours
    @Test
    void everyDiveHasItsColours() {
        for (Element e : Element.values())
            for (Grade g : Grade.values()) {
                SpiritDiveAnimeRules.Pal p = SpiritDiveAnimeRules.pal(e, g, false);
                assertNotNull(p);
                for (int c : new int[]{p.tint(), p.rim(), p.glow(), p.hair(), p.eye(), p.halo()}) assertTrue(c >= 0 && c <= 0xFFFFFF);
            }
        assertEquals(0xA8FFD8, SpiritDiveAnimeRules.pal(Element.WIND, Grade.YUNO, false).glow(), "Yuno's colour grading");
        assertEquals(0xFF7A2A, SpiritDiveAnimeRules.pal(Element.FIRE, Grade.FUEGOLEON, false).glow(), "Fuegoleon's");
        assertEquals(0xA8E4FF, SpiritDiveAnimeRules.pal(Element.WATER, Grade.NOELLE, false).glow(), "Noelle's");
    }

    // ------------------------------------------------------------------ the ribbons
    private static boolean finite(float[] a, int n) {
        for (int i = 0; i < n; i++) if (!Float.isFinite(a[i])) return false;
        return true;
    }

    @Test
    void everyPlumeSpineIsFiniteAndInsideItsBoundAtEveryMoment() {
        float[] out = new float[(SpiritDiveAnimeGeometry.MAX_SEGS + 1) * 3];
        for (Element e : Element.values())
            for (int r = 0; r < SpiritDiveAnimeGeometry.rows(e, true); r++) {
                float[] row = SpiritDiveAnimeGeometry.row(e, r);
                assertEquals(SpiritDiveAnimeGeometry.ROW, row.length);
                assertTrue(row[12] > 0f && row[13] > 0f);
                for (float side : new float[]{1f, -1f})
                    for (int segs : new int[]{3, 5, 8, 10, 12})
                        for (int age = 0; age < 300; age += 7)
                            for (float swing : new float[]{0f, 0.5f, 1f})
                                for (float vy : new float[]{-0.9f, 0f, 0.6f}) {
                                    SpiritDiveAnimeGeometry.plumeSpine(out, segs, row, side, 1.0f, age + 0.5f, swing, vy);
                                    assertTrue(finite(out, (segs + 1) * 3));
                                    float bound = SpiritDiveAnimeGeometry.reach(row, 1.0f) * 1.35f + 12f;
                                    for (int k = 0; k <= segs; k++) {
                                        float d = (float) Math.sqrt(out[k * 3] * out[k * 3] + out[k * 3 + 1] * out[k * 3 + 1] + out[k * 3 + 2] * out[k * 3 + 2]);
                                        assertTrue(d <= bound, e + " row " + r + " point " + k + " at " + d + " > " + bound);
                                    }
                                    assertTrue(Math.signum(out[0]) == side || Math.abs(out[0]) < 0.01f, "the root is on its own side");
                                }
            }
    }

    @Test
    void everyLockAndTrailSpineIsFiniteAndBounded() {
        float[] out = new float[(SpiritDiveAnimeGeometry.MAX_SEGS + 1) * 3];
        for (Element e : Element.values())
            for (int i = 0; i < SpiritDiveAnimeGeometry.lockRows(e); i++) {
                float[] row = SpiritDiveAnimeGeometry.lockRow(e, i);
                assertEquals(SpiritDiveAnimeGeometry.LOCK_ROW, row.length);
                for (float lift : new float[]{0f, 0.4f, 1f, 1.12f})
                    for (int age = 0; age < 200; age += 11) {
                        SpiritDiveAnimeGeometry.lockSpine(out, 5, row, 1.15f, lift, age, 1f, 0.5f);
                        assertTrue(finite(out, 18));
                        for (int k = 0; k <= 5; k++) {
                            float d = (float) Math.sqrt(out[k * 3] * out[k * 3] + out[k * 3 + 1] * out[k * 3 + 1] + out[k * 3 + 2] * out[k * 3 + 2]);
                            assertTrue(d < 70f, e + " lock " + i + " reaches " + d + " px from the neck");
                        }
                    }
            }
        for (float[] t : SpiritDiveAnimeGeometry.TRAIL)
            for (float side : new float[]{1f, -1f}) {
                SpiritDiveAnimeGeometry.trailSpine(out, 6, t, side, 33f, 1f, 0.7f);
                assertTrue(finite(out, 21));
                assertTrue(Math.abs(out[18]) < 40f && Math.abs(out[20]) < 60f);
            }
    }

    @Test
    void theProfilesStayBetweenZeroAndAboutOneAndTaperToThePoint() {
        for (Shape sh : Shape.values()) {
            for (float s = 0f; s <= 1.0001f; s += 0.02f) {
                float w = SpiritDiveAnimeGeometry.profile(sh, s, 1.3f);
                assertTrue(w >= 0f && w <= 1.4f, sh + " at " + s + " is " + w);
            }
            assertTrue(SpiritDiveAnimeGeometry.profile(sh, 1f, 0f) < 0.05f, sh + " ends in a point");
        }
        assertTrue(SpiritDiveAnimeGeometry.profile(Shape.LEAF, 0.4f, 0f) > 0.7f, "a leaf is broad in the middle");
    }

    @Test
    void theStripEdgesAreFiniteSeparatedAndTheWindingRuleHolds() {
        int n = 9;
        float[] spine = new float[n * 3], l = new float[n * 3], r = new float[n * 3], w = new float[n];
        SpiritDiveAnimeGeometry.plumeSpine(spine, n - 1, SpiritDiveAnimeGeometry.row(Element.WIND, 0), 1f, 1f, 10f, 0.3f, 0f);
        for (int k = 0; k < n; k++) w[k] = 2f;
        float[] look = {0f, 0f, 1f};
        SpiritDiveAnimeGeometry.edges(spine, n, w, 0f, 0.3f, -1f, look, 0.45f, l, r);
        assertTrue(finite(l, n * 3) && finite(r, n * 3));
        for (int k = 0; k < n; k++) {
            float dx = l[k * 3] - r[k * 3], dy = l[k * 3 + 1] - r[k * 3 + 1], dz = l[k * 3 + 2] - r[k * 3 + 2];
            assertEquals(4f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz), 0.01f, "the strip is twice the half width wide at point " + k);
        }
        // the front of the strip faces one way or the other, and flipping the viewer flips the answer
        boolean a = SpiritDiveAnimeGeometry.facing(l, r, n, look);
        boolean b = SpiritDiveAnimeGeometry.facing(l, r, n, new float[]{0f, 0f, -1f});
        assertTrue(a != b, "the same strip faces exactly one of two opposite viewers");
    }

    @Test
    void theHalfDiveDrawsOneSideAndTheFirstWindDiveIsTheLeftOne() {
        assertTrue(SpiritDivePieces.halfBody(Element.WIND, Stage.HALF));
        assertFalse(SpiritDivePieces.halfBody(Element.WIND, Stage.FULL));
        assertFalse(SpiritDivePieces.halfBody(Element.FIRE, Stage.HALF));
        int half = SpiritDiveAnimeRules.vertices(Element.WIND, false, Lod.FULL, Stage.HALF), full = SpiritDiveAnimeRules.vertices(Element.WIND, false, Lod.FULL, Stage.FULL);
        assertTrue(half < full);
        // the left arm is at +x in the layer space: every plume root of the table is on the +x side (mirrored for the right)
        for (Element e : Element.values()) for (int r = 0; r < SpiritDiveAnimeGeometry.rows(e, true); r++) assertTrue(SpiritDiveAnimeGeometry.row(e, r)[0] > 0f);
    }

    @Test
    void theSalamandersHornsAreTheFirstTwoLocksOfFire() {
        for (int i = 0; i < 2; i++) {
            float[] r = SpiritDiveAnimeGeometry.lockRow(Element.FIRE, i);
            assertEquals(3f, r[13], "SHARD: a horn is an angular spike");
            assertTrue(Math.abs(r[0]) > 3f && r[5] > 0f, "on the sides of the forehead, swept back");
        }
        assertEquals(-SpiritDiveAnimeGeometry.lockRow(Element.FIRE, 1)[0], SpiritDiveAnimeGeometry.lockRow(Element.FIRE, 0)[0], 1e-5f, "one on each side");
    }

    @Test
    void everyTexturePathIsUniqueAndNamedByElement() {
        Set<String> all = new HashSet<>(SpiritDiveAnimeRules.allTextures());
        assertEquals(SpiritDiveAnimeRules.allTextures().size(), all.size());
        assertEquals(4 * 7 + 4, all.size());
        for (Element e : Element.values()) {
            assertTrue(all.contains(SpiritDiveAnimeRules.plumeTexture(e)) && all.contains(SpiritDiveAnimeRules.haloTexture(e)) && all.contains(SpiritDiveAnimeRules.hairTexture(e)));
            assertFalse(SpiritDiveAnimeRules.additive(SpiritDiveAnimeRules.plumeTexture(e)), "the plume body is alpha blended, with its outline");
            assertTrue(SpiritDiveAnimeRules.additive(SpiritDiveAnimeRules.plumeGlowTexture(e)) && SpiritDiveAnimeRules.additive(SpiritDiveAnimeRules.haloTexture(e)));
        }
    }
}
