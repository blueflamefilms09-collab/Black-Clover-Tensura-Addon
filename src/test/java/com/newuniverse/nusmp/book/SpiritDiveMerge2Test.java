package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules.Models;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: merge 2 (slim spirits, calm entry, first person merge, small crowns) is pure rules the painters only draw, so the numbers are pinned here: the vertex budgets of the slim spirits and the crowns, the calm
 * entry (a third in the merge window, full at the start, a burst after the contact), the first person merge (never more than 35 percent of the screen, at any field of view or aspect, no inner silhouette), the
 * crowns (about 62 percent of the old ones, cheaper), and the option defaults.
 */
class SpiritDiveMerge2Test {
    private static final float[] FOVS = {30f, 70f, 110f}, ASPECTS = {1f, 16f / 9f, 32f / 9f};

    // ------------------------------------------------------------------ 1. the slim spirits
    @Test
    void slimGhostsAreWithinTheirBudgetsAndTheSilhouetteWithinItsOwn() {
        for (Element e : Element.values()) {
            for (boolean ch : new boolean[]{false, true}) {
                int v = SpiritDiveMerge2Rules.ghostVerts(e, ch, Models.SLIM);
                assertTrue(v > 0, e + " ghost mesh is empty");
                assertTrue(v <= SpiritDiveMerge2Rules.ghostBudget(ch), e + (ch ? " char " : " std ") + v + " vertices");
            }
            int core = SpiritDiveMerge2Rules.coreVerts(e, Models.SLIM);
            assertTrue(core > 0 && core <= SpiritDiveMerge2Rules.CORE_VERTS, e + " silhouette " + core);
        }
        assertEquals(900, SpiritDiveMerge2Rules.CHAR_VERTS);
        assertEquals(500, SpiritDiveMerge2Rules.STD_VERTS);
        assertEquals(300, SpiritDiveMerge2Rules.CORE_VERTS);
    }

    @Test
    void theSlimGhostOfTheCharacterTierIsTheCanonFormAndEarthHasNoCharacterTier() {
        assertEquals("wind_char", SpiritDiveMerge2Rules.slimMesh(Element.WIND, true));
        assertEquals("wind_std", SpiritDiveMerge2Rules.slimMesh(Element.WIND, false));
        assertEquals("fire_char", SpiritDiveMerge2Rules.slimMesh(Element.FIRE, true));
        assertEquals("water_char", SpiritDiveMerge2Rules.slimMesh(Element.WATER, true));
        assertEquals("earth_std", SpiritDiveMerge2Rules.slimMesh(Element.EARTH, true));
        assertEquals("earth_core", SpiritDiveMerge2Rules.slimCore(Element.EARTH));
        assertEquals(11, SpiritDiveMerge2Rules.meshes().size());
    }

    @Test
    void theWholeMergeOfOneDiverStaysBoundedWithEitherModels() {
        for (Models models : Models.values())
            for (Element e : Element.values())
                for (boolean ch : new boolean[]{false, true}) {
                    int cap = (models == Models.SLIM ? SpiritDiveMerge2Rules.ghostBudget(ch) : SpiritDiveMergeRules.GHOST_VERTS) + SpiritDiveMergeRules.strips(ch) * SpiritDiveMergeRules.STRIP_SEGS * 4 + SpiritDiveMergeRules.FLASH_SPRITES * 4 + SpiritDiveMergeRules.COVER_VERTS
                            + SpiritDiveMerge2Rules.CORE_VERTS + 50;
                    assertTrue(SpiritDiveMerge2Rules.worst(e, ch, models) <= cap, e + " " + models + " worst " + SpiritDiveMerge2Rules.worst(e, ch, models));
                    assertEquals(0, SpiritDiveMerge2Rules.vertices(e, ch, Lod.CULLED, 12f, true, models));
                    assertTrue(SpiritDiveMerge2Rules.vertices(e, ch, Lod.FULL, 12f, false, models) <= SpiritDiveMerge2Rules.CORE_VERTS, "no ghost beyond 32 blocks");
                    assertEquals(0, SpiritDiveMerge2Rules.vertices(e, ch, Lod.MID, SpiritDiveMergeRules.ENTRY_TICKS, true, models), "no steady state beyond full detail");
                }
    }

    // ------------------------------------------------------------------ 2. the calm entry
    @Test
    void theEntryIsAThirdInTheMergeWindowAndFullAtTheStart() {
        for (float u = SpiritDiveMerge2Rules.WINDOW_FROM; u <= SpiritDiveMerge2Rules.WINDOW_TO + 1e-4f; u += 0.125f) {
            float alpha = SpiritDiveMerge2Rules.flashAlpha(u, SpiritDiveReelRules.flash(u), SpiritDiveReelRules.reveal(u));
            assertTrue(alpha <= 0.35f, "flash alpha " + alpha + " at " + u);
            assertTrue(SpiritDiveMerge2Rules.flashSizeK(u) <= 0.35f + 1e-4f, "flash size at " + u);
            assertTrue(SpiritDiveMerge2Rules.ribbonK(u) <= 0.35f, "ribbons at " + u);
            assertTrue(alpha <= Math.max(SpiritDiveReelRules.flash(u), SpiritDiveReelRules.reveal(u)) + 1e-4f, "never louder than the old entry");
        }
        for (float u = 0f; u <= SpiritDiveMerge2Rules.START_FULL; u += 0.25f) {
            assertEquals(Math.max(SpiritDiveReelRules.flash(u), SpiritDiveReelRules.reveal(u)), SpiritDiveMerge2Rules.flashAlpha(u, SpiritDiveReelRules.flash(u), SpiritDiveReelRules.reveal(u)), 1e-5f, "the start flash is as before at " + u);
            assertEquals(1f, SpiritDiveMerge2Rules.flashSizeK(u), 1e-5f);
            assertEquals(1f, SpiritDiveMerge2Rules.ribbonK(Math.min(u, 5f)), 1e-5f);
        }
    }

    @Test
    void aShortBurstFollowsTheContact() {
        float peak = 0f;
        for (float u = 14.5f; u <= 22f; u += 0.1f) {
            float a = SpiritDiveMerge2Rules.flashAlpha(u, SpiritDiveReelRules.flash(u), SpiritDiveReelRules.reveal(u));
            if (u > 17f && u < 20f) peak = Math.max(peak, a);
            if (u >= 20f) assertEquals(0f, a, 1e-4f, "the burst is over by 20");
        }
        assertTrue(peak >= 0.8f, "the burst after the contact peaks at " + peak);
        assertTrue(SpiritDiveMerge2Rules.flashSizeK(18.4f) >= 0.85f);
        assertEquals(0f, SpiritDiveMerge2Rules.burst(17f), 1e-4f);
        assertEquals(0f, SpiritDiveMerge2Rules.burst(19.9f), 1e-4f);
        assertTrue(SpiritDiveMerge2Rules.BURST_AT > SpiritDiveMergeRules.FLASH_AT && SpiritDiveMerge2Rules.BURST_AT < 20f);
    }

    @Test
    void theRibbonsStayAThirdOnceTheSpiritFlowsAndAreNeverLouderThanBefore() {
        for (float u = 0f; u <= 22f; u += 0.25f) {
            float k = SpiritDiveMerge2Rules.ribbonK(u);
            assertTrue(k > 0.29f && k <= 1f, "ribbonK " + k);
            if (u >= 8f) assertEquals(SpiritDiveMerge2Rules.CALM, k, 1e-5f);
            assertTrue(SpiritDiveReelRules.ribbonAlpha(u) * k <= SpiritDiveReelRules.ribbonAlpha(u) + 1e-6f);
        }
    }

    // ------------------------------------------------------------------ 3. first person
    @Test
    void theFirstPersonMergeNeverCoversMoreThanThirtyFivePercentOfTheScreen() {
        float[] p = new float[3];
        for (float fov : FOVS)
            for (float aspect : ASPECTS)
                for (boolean ch : new boolean[]{false, true})
                    for (float side : new float[]{1f, -1f})
                        for (float m = 0f; m <= SpiritDiveMergeRules.ENTRY_TICKS + 1e-3f; m += 0.125f) {
                            SpiritDiveMerge2Rules.fpPath(m, side, p);
                            for (float v : p) assertFalse(Float.isNaN(v) || Float.isInfinite(v));
                            float d = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
                            float ext = SpiritDiveMerge2Rules.fpExtent(m, d, fov, aspect, ch);
                            assertTrue(SpiritDiveMerge2Rules.coverage(ext, d, fov, aspect) <= SpiritDiveMerge2Rules.FP_MAX_COVERAGE + 1e-3f, "spirit at " + m + " fov " + fov + " aspect " + aspect);
                            float r = SpiritDiveMerge2Rules.fpFlashRadius(m, d, fov, aspect);
                            assertTrue(SpiritDiveMerge2Rules.coverage(2f * r, d, fov, aspect) <= SpiritDiveMerge2Rules.FP_MAX_COVERAGE + 1e-3f, "flash at " + m);
                        }
    }

    @Test
    void theSpiritComesInAtTheUpperSideAndEndsBelowTheViewAtTheChest() {
        float[] p = new float[3];
        SpiritDiveMerge2Rules.fpPath(2f, 1f, p);
        assertTrue(p[0] > 0.3f && p[1] > 0.2f && p[2] > 1f, "upper side, ahead");
        SpiritDiveMerge2Rules.fpPath(2f, -1f, p);
        assertTrue(p[0] < -0.3f, "the other side");
        SpiritDiveMerge2Rules.fpPath(SpiritDiveMergeRules.FLOW_END, 1f, p);
        assertEquals(SpiritDiveMerge2Rules.FP_CHEST_Y, p[1], 0.02f);
        assertTrue(Math.abs(p[1]) > p[2] * Math.tan(Math.toRadians(35)), "below the bottom edge of a 70 degree view at the end");
        float x0 = 0f;
        for (float m = 8f; m <= 18f; m += 0.5f) {
            SpiritDiveMerge2Rules.fpPath(m, 1f, p);
            assertTrue(p[1] <= 0.5f && p[2] >= 0.1f);
            x0 = p[0];
        }
        assertTrue(Math.abs(x0) < 0.2f);
    }

    @Test
    void theFirstPersonSpiritIsFainterThanTheThirdPersonOneAndFadesNearTheEye() {
        for (float m = 0f; m <= 22f; m += 0.5f) {
            assertTrue(SpiritDiveMerge2Rules.fpAlpha(m, 2f) <= SpiritDiveMergeRules.ghostAlpha(m) * SpiritDiveMergeRules.GHOST_ALPHA * 0.61f + 1e-5f);
            assertEquals(0f, SpiritDiveMerge2Rules.fpAlpha(m, 0.2f), 1e-5f, "nothing blocks the eye");
        }
        assertTrue(SpiritDiveMerge2Rules.fpFlashAlpha(SpiritDiveMergeRules.FLASH_AT) <= SpiritDiveMerge2Rules.FP_FLASH_ALPHA + 1e-6f);
        assertEquals(0f, SpiritDiveMerge2Rules.fpFlashAlpha(8f), 1e-6f);
        assertEquals(0f, SpiritDiveMerge2Rules.fpFlashAlpha(20f), 1e-6f);
    }

    @Test
    void theFirstPersonMergeNeverDrawsTheInnerSilhouetteAndIsBoundedByOneGhost() {
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true})
                for (float m = 0f; m <= 22f; m += 0.25f) {
                    int v = SpiritDiveMerge2Rules.fpVertices(e, ch, m, Models.SLIM);
                    assertTrue(v <= SpiritDiveMerge2Rules.ghostBudget(ch) + 4, e + " fp " + v + " at " + m);
                    if (m > 19f) assertEquals(0, v, "nothing but the end of the flash after the merge, and no silhouette");
                }
    }

    // ------------------------------------------------------------------ 4. the crowns
    @Test
    void theCrownsAreAboutSixtyTwoPercentOfTheOldOnesAndSitCloserToTheHead() {
        assertEquals(0.62f, SpiritDiveMerge2Rules.CROWN_K, 1e-6f);
        assertTrue(SpiritDiveMerge2Rules.NEW_RING / SpiritDiveMerge2Rules.OLD_RING <= 0.65f);
        assertTrue(SpiritDiveMerge2Rules.NEW_TALL / SpiritDiveMerge2Rules.OLD_TALL <= 0.75f);
        assertTrue(SpiritDiveMerge2Rules.HOVER > SpiritDiveWind4Rules.HOVER, "closer to the head (y is down)");
        assertTrue(SpiritDiveMerge2Rules.HOVER < SpiritDiveMerge2Rules.HEAD_TOP, "still floating above the head");
        assertEquals(0f, SpiritDiveMerge2Rules.SPIN, "the front bar stays at the front");
        assertEquals(7, SpiritDiveMerge2Rules.BARS_CHAR);
        assertEquals(5, SpiritDiveMerge2Rules.BARS_STD);
    }

    @Test
    void theCrownMeshesAreWithinBudgetAndTheWindCrownIsCheaperThanTheOldOne() {
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && !SpiritDiveMergeRules.hasCharacterMesh(e)) continue;
                int v = SpiritDiveMerge2Rules.crownVerts(e, ch, false);
                assertTrue(v > 0 && v <= SpiritDiveMerge2Rules.crownBudget(ch), e + " crown " + v);
            }
        for (boolean ch : new boolean[]{false, true})
            for (boolean half : new boolean[]{false, true}) {
                assertTrue(SpiritDiveMerge2Rules.windCrownDelta(ch, half) < 0, "Wind crown delta " + SpiritDiveMerge2Rules.windCrownDelta(ch, half));
                assertTrue(SpiritDiveMerge2Rules.crownVerts(Element.WIND, ch, half) <= 400);
            }
        assertTrue(SpiritDiveMerge2Rules.crownVerts(Element.WIND, true, true) < SpiritDiveMerge2Rules.crownVerts(Element.WIND, true, false), "the half crown is half");
        assertEquals(9, SpiritDiveMerge2Rules.crownMeshes().size());
    }

    @Test
    void crownNamesAreOnePerElementAndTierAndOnlyWindHasAHalf() {
        assertEquals("crown_wind_char_half", SpiritDiveMerge2Rules.crownName(Element.WIND, true, true));
        assertEquals("crown_fire_std", SpiritDiveMerge2Rules.crownName(Element.FIRE, false, true));
        assertEquals("crown_water_char", SpiritDiveMerge2Rules.crownName(Element.WATER, true, false));
        assertEquals("crown_earth_std", SpiritDiveMerge2Rules.crownName(Element.EARTH, true, false));
    }

    // ------------------------------------------------------------------ 5. the options
    @Test
    void theOptionsDefaultToTheNewBehaviourAndTheOldOnesStayAvailable() {
        assertEquals(Models.SLIM, SpiritDiveMerge2Rules.DEFAULT_MODELS);
        assertTrue(SpiritDiveMerge2Rules.DEFAULT_CALM_ENTRY && SpiritDiveMerge2Rules.DEFAULT_FIRST_PERSON && SpiritDiveMerge2Rules.DEFAULT_CROWN);
        assertEquals(2, Models.values().length, "SLIM and BOXY");
        assertTrue(SpiritDiveMergeRules.DEFAULT_MERGE, "spiritDive.merge keeps working");
        for (Element e : Element.values())
            assertTrue(SpiritDiveMerge2Rules.ghostVerts(e, true, Models.BOXY) > 0, "the boxy meshes stay");
    }
}
