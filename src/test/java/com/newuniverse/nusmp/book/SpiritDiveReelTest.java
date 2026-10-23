package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Spec;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the REEL Spirit Dive (the default look since 3.3: the V2 design executed like the owner's videos) cannot be seen in a test, so its decisions are pinned where they are pure: the look option and
 * its append-only value, the tier of every character, the one clock of the entry and the exit (every effect is a function of it), the budget arithmetic (every vertex of every dive is counted from the
 * tables and kept under the V2 dive's worst case and under the cap of the look), and the geometry (every spine finite and inside its bound at every moment). The painters are read by SpiritDiveReelScanTest.
 */
class SpiritDiveReelTest {

    // ------------------------------------------------------------------ the look option
    @Test
    void reelIsTheDefaultLookAppendedLastAndEveryOldLookIsStillThere() {
        assertEquals("[DJINN, V2, CLASSIC, ANIME, REEL]", java.util.Arrays.toString(Look.values()), "append only: REEL is last");
        assertEquals(Look.REEL, SpiritDiveDjinnPieces.DEFAULT_LOOK);
        assertEquals(Look.REEL, SpiritDiveReelRules.DEFAULT_LOOK);
        assertEquals(Look.REEL, SpiritDiveReelRules.parse(null));
        assertEquals(Look.REEL, SpiritDiveReelRules.parse("nonsense"));
        for (Look l : Look.values()) assertEquals(l, SpiritDiveReelRules.parse(" " + l.name().toLowerCase() + " "), l + " is still choosable");
    }

    @Test
    void theLookFlagsFollowTheOption() {
        try {
            SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
            assertTrue(SpiritDiveLook.reel());
            assertFalse(SpiritDiveLook.anime());
            for (Look l : Look.values()) {
                SpiritDiveLook.set(l, true, true, FuegoleonArmRules.View.ALWAYS);
                assertEquals(l == Look.REEL, SpiritDiveLook.reel());
                assertEquals(l == Look.ANIME, SpiritDiveLook.anime());
                assertEquals(l == Look.CLASSIC, SpiritDiveLook.classic());
            }
            SpiritDiveLook.set((Look) null, true, true, null);
            assertEquals(Look.REEL, SpiritDiveLook.look(), "a missing value is the default");
        } finally {
            SpiritDiveLook.set(Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
        }
    }

    @Test
    void theCameraIsNeverTouched() {
        assertFalse(SpiritDiveReelRules.CAMERA_PUSH);
    }

    // ------------------------------------------------------------------ tiers
    @Test
    void theCharacterTierBelongsToTheCharactersOfWindFireAndWaterOnly() {
        assertTrue(SpiritDiveReelRules.character(Element.WIND, Grade.YUNO));
        assertTrue(SpiritDiveReelRules.character(Element.FIRE, Grade.FUEGOLEON));
        assertTrue(SpiritDiveReelRules.character(Element.WATER, Grade.NOELLE));
        assertFalse(SpiritDiveReelRules.character(Element.WIND, Grade.NOELLE));
        for (Grade g : Grade.values()) assertFalse(SpiritDiveReelRules.character(Element.EARTH, g));
        for (Element e : Element.values()) assertFalse(SpiritDiveReelRules.character(e, Grade.STANDARD));
    }

    @Test
    void theCharacterTierDrawsMoreAndOnlyItDrawsTheWeaponTheSpireAndTheHalo() {
        for (Element e : Element.values()) {
            Spec s = SpiritDiveReelRules.spec(e, false, Lod.FULL), c = SpiritDiveReelRules.spec(e, true, Lod.FULL);
            assertTrue(c.ribbons() >= s.ribbons() && c.cage() > s.cage() && c.crownCubes() > s.crownCubes() && c.sparks() > s.sparks(), e + ": the character has more of everything");
            assertTrue(c.weapon() && c.halo() && c.spire() && !s.weapon() && !s.halo() && !s.spire(), e + ": the weapon, halo and spire are the character's");
            assertTrue(s.crownCubes() > 0 && s.gauntlet() >= 2 && s.shellPasses() >= 3 && s.ground(), e + ": the standard tier has the cubes, the gauntlets, the shell and the ground ring");
        }
        // each element has its own wing: Wind the leaf, the others a comb of blades
        assertTrue(SpiritDiveReelRules.spec(Element.WIND, false, Lod.FULL).leaves() == 2 && SpiritDiveReelRules.spec(Element.WIND, false, Lod.FULL).blades() == 0);
        for (Element e : new Element[]{Element.WATER, Element.FIRE, Element.EARTH}) {
            Spec s = SpiritDiveReelRules.spec(e, false, Lod.FULL);
            assertTrue(s.leaves() == 0 && s.blades() >= 6, e + " draws a comb");
        }
        assertTrue(SpiritDiveReelRules.spec(Element.WATER, true, Lod.FULL).blades() >= 15, "Noelle's comb is a fan of 15 strips");
    }

    @Test
    void theLevelsOfDetailOnlyEverDrawLess() {
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true})
                for (Stage st : Stage.values()) {
                    int full = SpiritDiveReelRules.vertices(e, ch, Lod.FULL, st), mid = SpiritDiveReelRules.vertices(e, ch, Lod.MID, st), far = SpiritDiveReelRules.vertices(e, ch, Lod.FAR, st);
                    assertTrue(full >= mid && mid >= far && far >= 0, e + " " + st + ": " + full + " " + mid + " " + far);
                    assertEquals(0, SpiritDiveReelRules.vertices(e, ch, Lod.CULLED, st));
                    assertTrue(far <= 160, "a far player only gets the plain shell and a ring (" + far + ")");
                }
        assertEquals(0, SpiritDiveReelRules.spec(Element.WIND, true, Lod.CULLED).ribbons());
        assertFalse(SpiritDiveReelRules.spec(Element.WATER, true, Lod.MID).weapon(), "the weapon is for full detail");
        assertEquals(0, SpiritDiveReelRules.spec(Element.WATER, true, Lod.FAR).crownCubes());
    }

    // ------------------------------------------------------------------ the clock
    @Test
    void theClockRunsZeroToTwentyTwoOnTheEntryAndBackwardsOnTheExit() {
        assertEquals(22, SpiritDiveReelRules.ENTRY_TICKS);
        assertEquals(SpiritDiveTier.ENTRY_TICKS, SpiritDiveReelRules.ENTRY_TICKS, "the same window as the server's entry");
        assertEquals(0f, SpiritDiveReelRules.clock(0f, 100f));
        assertEquals(11f, SpiritDiveReelRules.clock(11f, 100f), 1e-4f);
        assertEquals(22f, SpiritDiveReelRules.clock(100f, 100f));
        assertEquals(22f, SpiritDiveReelRules.clock(1e4f, 1e4f));
        assertEquals(0f, SpiritDiveReelRules.clock(100f, 0f), "the dive is over");
        assertEquals(11f, SpiritDiveReelRules.clock(1e4f, 10f), 1e-3f, "the exit is the entry backwards: 10 of 20 ticks left is the half way point");
        float prev = -1f;
        for (float left = 0f; left <= 20f; left += 0.5f) { float u = SpiritDiveReelRules.clock(1e4f, left); assertTrue(u >= prev, "the exit never runs forwards"); prev = u; }
        assertFalse(SpiritDiveReelRules.exiting(5f, 100f));
        assertTrue(SpiritDiveReelRules.exiting(100f, 5f));
    }

    @Test
    void everyEffectOfTheEntryIsOnAtItsMomentAndGoneWhenTheFormIsComplete() {
        float end = SpiritDiveReelRules.ENTRY_TICKS;
        assertEquals(0f, SpiritDiveReelRules.flash(0f));
        assertEquals(1f, SpiritDiveReelRules.flash(1.2f), 1e-4f, "the flash peaks at the start");
        assertEquals(0f, SpiritDiveReelRules.flash(end));
        assertEquals(1f, SpiritDiveReelRules.reveal(13.5f), 1e-4f, "the second burst, when the form appears");
        assertEquals(0f, SpiritDiveReelRules.reveal(end));
        assertTrue(SpiritDiveReelRules.ribbonAlpha(6f) > 0.9f && SpiritDiveReelRules.ribbonAlpha(end) < 0.01f);
        assertTrue(SpiritDiveReelRules.cage(8f) > 0.9f && SpiritDiveReelRules.cage(end) < 0.01f && SpiritDiveReelRules.cage(0f) == 0f);
        assertEquals(0f, SpiritDiveReelRules.groundPulse(1f));
        assertEquals(0f, SpiritDiveReelRules.groundPulse(end));
        assertTrue(SpiritDiveReelRules.groundPulse(9f) > 0.4f && SpiritDiveReelRules.groundPulse(9f) < 0.6f);
        assertEquals(1f, SpiritDiveReelRules.wingScale(end), 1e-4f);
        assertEquals(1f, SpiritDiveReelRules.gauntlet(end), 1e-4f);
        assertEquals(1f, SpiritDiveReelRules.weapon(end), 1e-4f);
        assertEquals(1f, SpiritDiveReelRules.assemble(end), 1e-4f);
        assertEquals(1f, SpiritDiveReelRules.crown(end), 1e-4f);
        assertEquals(1f, SpiritDiveReelRules.settle(end), 1e-4f);
        float peak = 0f;
        for (float u = 0f; u <= end; u += 0.25f) peak = Math.max(peak, SpiritDiveReelRules.wingScale(u));
        assertTrue(peak > 1.04f && peak < 1.25f, "the wings unfold with an overshoot (" + peak + ")");
        peak = 0f;
        for (float u = 0f; u <= end; u += 0.25f) peak = Math.max(peak, SpiritDiveReelRules.gauntlet(u));
        assertTrue(peak > 1.05f && peak < 1.4f, "the gauntlets pop on (" + peak + ")");
    }

    @Test
    void theBodyAssemblesFromTheFeetToTheHeadAndTheCrownCubesIgniteOneAfterTheOther() {
        float prev = 0f;
        for (float u = 0f; u <= 22f; u += 0.5f) { float a = SpiritDiveReelRules.assemble(u); assertTrue(a >= prev); prev = a; }
        assertEquals(1, SpiritDiveReelRules.mosaicLevel(0f, 0f, 0.6f), "the first level is the first few patches");
        assertEquals(SpiritDiveReelRules.MOSAIC_LEVELS, SpiritDiveReelRules.mosaicLevel(1f, 0f, 0.6f));
        assertTrue(SpiritDiveReelRules.mosaicLevel(0.4f, 0f, 0.6f) > SpiritDiveReelRules.mosaicLevel(0.4f, 0.25f, 0.7f), "the legs are ahead of the body, the body ahead of the head");
        int n = 12;
        float first0 = -1f, first11 = -1f;
        for (float u = 0f; u <= 22f; u += 0.1f) {
            if (first0 < 0f && SpiritDiveReelRules.crownCube(u, 0, n) > 0.01f) first0 = u;
            if (first11 < 0f && SpiritDiveReelRules.crownCube(u, n - 1, n) > 0.01f) first11 = u;
        }
        assertTrue(first0 >= 11f && first0 < first11 && first11 <= 20f, "cube 0 lights (" + first0 + ") before cube 11 (" + first11 + "), inside the ignition window");
        for (int i = 0; i < n; i++) assertEquals(1f, SpiritDiveReelRules.crownCube(22f, i, n), 1e-4f, "every cube is on at the end");
        for (float u = 0f; u <= 22f; u += 0.25f) for (int i = 0; i < n; i++) { float k = SpiritDiveReelRules.crownCube(u, i, n); assertTrue(k >= 0f && k < 1.2f, "cube " + i + " at " + u + ": " + k); }
    }

    // ------------------------------------------------------------------ the budget
    @Test
    void everyDiveStaysUnderTheCapOfTheLookAndTheWorstCasesOfTheOlderLooks() {
        assertTrue(SpiritDiveReelRules.CAP_CHARACTER < SpiritDiveReelRules.ANIME_WORST_VERTICES, "the character cap is under the ANIME look's worst case");
        assertTrue(SpiritDiveReelRules.CAP_STANDARD < SpiritDiveReelRules.CAP_CHARACTER);
        int worstC = 0, worstS = 0;
        for (Element e : Element.values())
            for (Stage st : Stage.values())
                for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
                    int s = SpiritDiveReelRules.vertices(e, false, lod, st);
                    assertTrue(s <= SpiritDiveReelRules.CAP_STANDARD, e + " " + st + " " + lod + " standard: " + s);
                    worstS = Math.max(worstS, s);
                    if (e == Element.EARTH) continue;
                    int c = SpiritDiveReelRules.vertices(e, true, lod, st);
                    assertTrue(c <= SpiritDiveReelRules.CAP_CHARACTER, e + " " + st + " " + lod + " character: " + c);
                    worstC = Math.max(worstC, c);
                }
        assertEquals(worstC, SpiritDiveReelRules.worstVertices(true));
        assertEquals(worstS, SpiritDiveReelRules.worstVertices(false));
        assertTrue(worstC < SpiritDiveReelRules.ANIME_WORST_VERTICES && worstC < SpiritDiveReelRules.V2_WORST_VERTICES, "the worst full character dive (" + worstC + ") is under ANIME's " + SpiritDiveReelRules.ANIME_WORST_VERTICES);
        // the per frame ceiling: at most CHARACTER_PER_FRAME character dives, the rest standard
        int frame = SpiritDiveTier.CHARACTER_PER_FRAME * worstC;
        assertTrue(frame <= 3 * 3200, "three character dives a frame stay under " + frame);
    }

    @Test
    void theEntryAddsRibbonsCirclesAndTheFlashOnlyAtFullDetail() {
        Spec c = SpiritDiveReelRules.spec(Element.WIND, true, Lod.FULL);
        assertEquals(c.ribbons() * c.ribbonSegs() * 4 * SpiritDiveReelRules.RIBBON_PASSES, SpiritDiveReelRules.ribbonVertices(c));
        assertEquals(SpiritDiveReelRules.ribbonVertices(c) + c.cage() * SpiritDiveReelRules.RING + 3 * SpiritDiveReelRules.SPRITE + SpiritDiveReelRules.RING, SpiritDiveReelRules.entryVertices(c));
        assertTrue(c.ribbons() >= 6 && c.ribbons() <= 10, "the video's tornado is six to ten thin lines");
        assertTrue(c.ribbonSegs() <= SpiritDiveReelGeometry.MAX_SEGS && c.leafSegs() <= SpiritDiveReelGeometry.MAX_SEGS && c.bladeSegs() <= SpiritDiveReelGeometry.MAX_SEGS);
        assertEquals(0, SpiritDiveReelRules.spec(Element.WIND, true, Lod.FAR).ribbons());
    }

    @Test
    void theHalfDiveOfWindDressesTheLeftHalfOnly() {
        assertEquals(1, SpiritDiveReelRules.sides(Element.WIND, Stage.HALF));
        assertEquals(2, SpiritDiveReelRules.sides(Element.WIND, Stage.FULL));
        assertEquals(2, SpiritDiveReelRules.sides(Element.WATER, Stage.HALF));
        assertEquals(1, SpiritDiveReelRules.arms(Element.WIND, Stage.HALF), "the left gauntlet first");
        assertEquals(2, SpiritDiveReelRules.arms(Element.WIND, Stage.FULL));
        assertTrue(SpiritDiveReelRules.crowned(Element.WIND, Stage.HALF), "half a crown");
        assertFalse(SpiritDiveReelRules.crowned(Element.FIRE, Stage.HALF));
        assertTrue(SpiritDiveReelGeometry.crownCount(12, true) < SpiritDiveReelGeometry.crownCount(12, false));
        assertEquals(12, SpiritDiveReelGeometry.crownCount(12, false));
        assertTrue(SpiritDiveReelRules.vertices(Element.WIND, false, Lod.FULL, Stage.HALF) < SpiritDiveReelRules.vertices(Element.WIND, false, Lod.FULL, Stage.FULL));
    }

    // ------------------------------------------------------------------ the geometry
    @Test
    void everyLeafSpineIsFiniteAndInsideItsBoundAtEveryMoment() {
        float[] spine = new float[(SpiritDiveReelGeometry.MAX_SEGS + 1) * 3];
        float bound = SpiritDiveReelGeometry.reach(Element.WIND);
        for (int r = 0; r < SpiritDiveReelGeometry.leafRows(); r++)
            for (float open = 0f; open <= 1.2f; open += 0.1f)
                for (float age = 0f; age < 200f; age += 37f)
                    for (float side : new float[]{1f, -1f})
                        for (float swing : new float[]{0f, 1f}) {
                            SpiritDiveReelGeometry.leafSpine(spine, 8, SpiritDiveReelGeometry.leaf(r), side, SpiritDiveReelGeometry.LEAF_CHARACTER, open, age, swing);
                            for (int i = 0; i < 27; i++) { assertTrue(Float.isFinite(spine[i])); }
                            for (int k = 0; k <= 8; k++) assertTrue(Math.abs(spine[k * 3]) <= bound && Math.abs(spine[k * 3 + 1]) <= 60f && Math.abs(spine[k * 3 + 2]) <= 60f, "leaf " + r + " open " + open + " point " + k);
                        }
    }

    @Test
    void theCombIsAFanOfOverlappingBladesAndEveryBladeSpineIsFiniteAndBounded() {
        float[] row = new float[SpiritDiveReelGeometry.BLADE_ROW], spine = new float[(SpiritDiveReelGeometry.MAX_SEGS + 1) * 3], next = new float[SpiritDiveReelGeometry.BLADE_ROW];
        for (Element e : new Element[]{Element.WATER, Element.FIRE, Element.EARTH})
            for (int n : new int[]{3, 6, 9, 15}) {
                float lenMax = SpiritDiveReelGeometry.BLADE_LENGTH[e.ordinal()][1];
                float bound = SpiritDiveReelGeometry.reach(e);
                for (float spread : new float[]{0f, 0.5f, 1f})
                    for (int i = 0; i < n; i++) {
                        SpiritDiveReelGeometry.blade(row, i, n, 1f, lenMax, spread);
                        SpiritDiveReelGeometry.bladeSpine(spine, 2, row, i, 77f, 1f);
                        for (int k = 0; k < 9; k++) assertTrue(Float.isFinite(spine[k]) && Math.abs(spine[k]) <= bound, e + " blade " + i + "/" + n);
                        assertTrue(row[9] > 0f && row[10] > 0f && row[9] <= lenMax * 1.001f);
                    }
                if (n >= 9) {   // the fan: the angle rises from the lowest blade to the highest, so neighbours are one step apart and the half width covers the step
                    SpiritDiveReelGeometry.blade(row, 0, n, 1f, lenMax, 1f);
                    SpiritDiveReelGeometry.blade(next, 1, n, 1f, lenMax, 1f);
                    assertTrue(next[4] < row[4], "the fan climbs (y is down)");
                    float tipGap = Math.abs((next[4] - row[4]) * row[9]);
                    assertTrue(row[10] >= tipGap * 0.9f, e + ": the blades overlap (" + row[10] + " vs " + tipGap + ")");
                }
            }
    }

    @Test
    void theHelixRisesAroundTheBodyAndEveryRibbonIsDistinct() {
        float[] out = new float[(SpiritDiveReelGeometry.MAX_SEGS + 1) * 3];
        SpiritDiveReelGeometry.helix(out, 12, 30f, 24f, 1f, 1f);
        for (int k = 0; k <= 12; k++) assertTrue(Float.isFinite(out[k * 3]) && Math.abs(out[k * 3]) <= SpiritDiveReelGeometry.reach(Element.WIND) && Math.abs(out[k * 3 + 2]) <= SpiritDiveReelGeometry.reach(Element.WIND));
        assertTrue(out[1] > out[37], "the head of the ribbon is higher (y is down) than its tail");
        Set<String> angles = new HashSet<>();
        for (int i = 0; i < 10; i++) angles.add(String.format("%.3f", SpiritDiveReelGeometry.ribbonAngle(i, 10)));
        assertEquals(10, angles.size());
        for (int i = 0; i < 10; i++) { assertTrue(SpiritDiveReelGeometry.ribbonRadius(i) > 0.7f && SpiritDiveReelGeometry.ribbonRadius(i) < 1.3f); }
    }

    @Test
    void theCrownIsATwelveCubeRingWithBarsOnTheCornersAndTheFrontAndBack() {
        assertEquals(12, SpiritDiveReelGeometry.CROWN.length);
        for (float[] p : SpiritDiveReelGeometry.CROWN) assertTrue(Math.abs(p[0]) <= 5.6f && Math.abs(p[1]) <= 5.6f && (Math.abs(p[0]) > 4.9f || Math.abs(p[1]) > 4.9f), "on a square just outside the head (half side 4)");
        for (int b : SpiritDiveReelGeometry.BARS) assertTrue(b >= 0 && b < SpiritDiveReelGeometry.CROWN.length);
        assertEquals(6, SpiritDiveReelGeometry.barCount(6, false));
        assertTrue(SpiritDiveReelGeometry.barCount(6, true) < 6);
        for (float h : SpiritDiveReelGeometry.BAR_HEIGHT) assertTrue(h > 1.5f && h < 5f, "bars are short, a little higher than the cubes (B_09)");
    }

    @Test
    void theSparksStayRoundTheBody() {
        float[] p = new float[4];
        for (int i = 0; i < 8; i++)
            for (float age = 0f; age < 400f; age += 13f) {
                SpiritDiveReelGeometry.spark(p, i, age);
                assertTrue(Math.hypot(p[0], p[2]) < 1.1f && p[1] >= 0f && p[1] <= 2.2f && p[3] >= 0f && p[3] <= 1f, "spark " + i + " at " + age);
            }
    }

    // ------------------------------------------------------------------ colours, textures
    @Test
    void everyDiveHasAPaletteAndEveryTexturePathIsUnique() {
        for (Element e : Element.values())
            for (Grade g : Grade.values()) {
                SpiritDiveReelRules.Pal pal = SpiritDiveReelRules.pal(e, g);
                assertTrue(pal.body() != 0 && pal.glow() != 0 && pal.light() != 0 && pal.rim() != 0 && pal.gold() != 0, e + " " + g);
            }
        java.util.List<String> all = SpiritDiveReelRules.allTextures();
        assertEquals(all.size(), new HashSet<>(all).size(), "no path twice");
        assertEquals(4 * (5 + SpiritDiveReelRules.MOSAIC_LEVELS) + 12, all.size());
        for (String t : all) assertTrue(t.startsWith("aura/sdr_"), t);
        assertEquals(2, SpiritDiveReelRules.sharedTextures().size());
    }

    @Test
    void theMosaicIsLighterOverClothesAndSolidOnNoelle() {
        assertTrue(SpiritDiveReelRules.mosaicAlpha(Element.WATER, true) > SpiritDiveReelRules.mosaicAlpha(Element.WIND, true));
        assertTrue(SpiritDiveReelRules.mosaicAlpha(Element.WATER, true) >= SpiritDiveReelRules.mosaicAlpha(Element.WATER, false));
        for (Element e : Element.values()) for (boolean ch : new boolean[]{false, true}) { float a = SpiritDiveReelRules.mosaicAlpha(e, ch); assertTrue(a > 0.5f && a <= 1f); }
    }

    @Test
    void theWeaponsOfTheCharactersAreCountedAndEarthHasNone() {
        assertTrue(SpiritDiveReelRules.weaponVertices(Element.WIND) > 0 && SpiritDiveReelRules.weaponVertices(Element.FIRE) > 0 && SpiritDiveReelRules.weaponVertices(Element.WATER) > 0);
        assertEquals(0, SpiritDiveReelRules.weaponVertices(Element.EARTH));
        assertEquals(10, SpiritDiveReelRules.SPIKES_PER_DISC);
        // Yuno's sword is on his right hip and out and up; the circle sits at its hilt
        assertTrue(SpiritDiveReelGeometry.SWORD[0] < 0f && SpiritDiveReelGeometry.SWORD[4] > 0f && SpiritDiveReelGeometry.SWORD[6] >= 30f);
    }
}
