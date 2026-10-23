package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Def;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Look;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Djinn-style Spirit Dive is chosen by a client option, drawn from tables, and bounded by numbers; every one of those fails silently in play (the wrong look by default, a half dive that dresses the
 * wrong side, a plate on the arm that Fuegoleon's flame replaces, a set that costs more than the Djinn it is modelled on). This test pins the pure rules: look selection, tier mapping, the piece lists, the
 * forge order, the levels of detail, the budget arithmetic, the forge sounds, the particle trail. Pure (no game classes): {@link SpiritDiveLook} only holds flags.
 */
class SpiritDiveDjinnTest {
    // ------------------------------------------------------------------ look selection
    @Test
    void theDefaultLookIsTheDjinnStyleAndTheOldLooksStayAChoice() {
        assertEquals(Look.REEL, SpiritDiveDjinnPieces.DEFAULT_LOOK, "3.3: the default look is REEL (3.2: ANIME, the owner rejected the plates); DJINN stays choosable");
        assertEquals(Look.REEL, SpiritDiveLook.look(), "a client that never read its config draws the REEL look");
        assertEquals(5, Look.values().length, "DJINN, V2, CLASSIC, ANIME and REEL: nothing was removed, ANIME and REEL were appended");
        assertEquals("V2", Look.V2.name());
        assertEquals("CLASSIC", Look.CLASSIC.name());
    }

    @Test
    void theOldBooleanFormStillMeansWhatItMeantBefore() {
        try {
            SpiritDiveLook.set(true, true, true, FuegoleonArmRules.View.ALWAYS);
            assertEquals(Look.CLASSIC, SpiritDiveLook.look());
            assertTrue(SpiritDiveLook.classic());
            SpiritDiveLook.set(false, true, true, FuegoleonArmRules.View.ALWAYS);
            assertEquals(Look.V2, SpiritDiveLook.look(), "false was the remake of 3.0.0, which is look V2 now");
            assertFalse(SpiritDiveLook.classic());
            SpiritDiveLook.set(Look.DJINN, true, true, FuegoleonArmRules.View.ALWAYS);
            assertFalse(SpiritDiveLook.classic());
            assertFalse(SpiritDiveLook.v2());
            SpiritDiveLook.set((Look) null, true, true, null);
            assertEquals(Look.REEL, SpiritDiveLook.look(), "a missing value is the default");
        } finally {
            SpiritDiveLook.set(Look.DJINN, true, true, FuegoleonArmRules.View.ALWAYS);
        }
    }

    // ------------------------------------------------------------------ tier mapping
    @Test
    void theCharacterSetBelongsToTheCharacterGradeOfTheElementAndEarthHasNone() {
        assertTrue(SpiritDiveDjinnPieces.character(Element.WIND, Grade.YUNO));
        assertTrue(SpiritDiveDjinnPieces.character(Element.FIRE, Grade.FUEGOLEON));
        assertTrue(SpiritDiveDjinnPieces.character(Element.WATER, Grade.NOELLE));
        for (Element e : Element.values()) assertFalse(SpiritDiveDjinnPieces.character(e, Grade.STANDARD));
        assertFalse(SpiritDiveDjinnPieces.character(Element.FIRE, Grade.YUNO), "a stray grade never earns another character's set");
        assertFalse(SpiritDiveDjinnPieces.character(Element.EARTH, Grade.NOELLE));
        assertEquals("sdj_wind_c", SpiritDiveDjinnPieces.key(Element.WIND, true));
        assertEquals("sdj_earth_s", SpiritDiveDjinnPieces.key(Element.EARTH, false));
        assertEquals("sdj_water_c_arm_helm", SpiritDiveDjinnPieces.modelId(Element.WATER, true, "helm"));
        assertEquals("arm_helm", SpiritDiveDjinnPieces.modelName("helm"));
    }

    // ------------------------------------------------------------------ the piece lists
    @Test
    void everyNamedPieceHasADefinitionAndTheListsHaveNoDuplicates() {
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true})
                for (List<String> names : List.of(SpiritDiveDjinnPieces.fullNames(e, ch), SpiritDiveDjinnPieces.halfNames(e, ch))) {
                    Set<String> seen = new HashSet<>();
                    for (String n : names) {
                        assertNotNull(SpiritDiveDjinnPieces.def(n, ch), "no definition for " + n);
                        assertTrue(seen.add(n), "duplicate " + n + " in " + names);
                    }
                }
        assertEquals(null, SpiritDiveDjinnPieces.def("nonsense", false));
    }

    @Test
    void theHalfSetIsAPartOfTheFullSetAndTheFullSetIsAFullBody() {
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                List<String> full = SpiritDiveDjinnPieces.fullNames(e, ch), half = SpiritDiveDjinnPieces.halfNames(e, ch);
                assertTrue(full.containsAll(half), e + "/" + ch + ": the half dive's pieces are on in the full dive (an upgrade keeps them): " + half);
                assertTrue(half.size() < full.size());
                for (String must : new String[]{"helm", "chest", "backplate", "belt", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "gauntlet_r", "gauntlet_l", "cuisse_r", "cuisse_l", "greave_r", "greave_l",
                        "sabaton_r", "sabaton_l"})
                    assertTrue(full.contains(must), e + "/" + ch + " has no " + must + ": plates on every limb");
            }
    }

    @Test
    void theCharacterSetAddsTheOrnateAndSignaturePiecesTheStandardSetLeavesOut() {
        for (Element e : new Element[]{Element.WIND, Element.FIRE, Element.WATER}) {
            List<String> s = SpiritDiveDjinnPieces.fullNames(e, false), c = SpiritDiveDjinnPieces.fullNames(e, true);
            assertTrue(c.size() > s.size(), e + ": the character set is the bigger one");
            assertTrue(c.contains("gorget") && !s.contains("gorget"));
            assertTrue(c.contains("wing_r") && c.contains("wing_l") && !s.contains("wing_r"), e + ": wings of plates are the character's");
            assertTrue(c.contains("skirt"));
        }
        assertTrue(SpiritDiveDjinnPieces.fullNames(Element.FIRE, true).containsAll(List.of("plume", "mantle")), "Fuegoleon's plume and mantle");
        assertTrue(SpiritDiveDjinnPieces.fullNames(Element.WIND, true).contains("crest_c"), "Yuno's mounted crown");
        assertTrue(SpiritDiveDjinnPieces.fullNames(Element.EARTH, false).contains("backcrystals"), "the Gnome's crystals");
        assertTrue(SpiritDiveDjinnPieces.fullNames(Element.WATER, false).contains("skirt"), "the Undine's plated skirt panels are in both tiers");
    }

    @Test
    void theFirstWindDiveIsTheLeftHalfOfTheBodyOnly() {
        for (boolean ch : new boolean[]{false, true}) {
            List<String> half = SpiritDiveDjinnPieces.names(Element.WIND, Stage.HALF, ch ? Grade.YUNO : Grade.STANDARD, ch);
            assertFalse(half.isEmpty());
            for (String n : half) assertFalse(n.endsWith("_r") || n.equals("crest_c") || n.equals("helm") || n.equals("chest"), "the first Wind dive dresses only the left half: " + n);
            assertTrue(half.containsAll(List.of("crest_l", "pauldron_l", "vambrace_l", "gauntlet_l", "greave_l")));
        }
    }

    @Test
    void theOtherHalfDivesAreTheCrestAndTheArmsLikeTheDjinnsHalfEquip() {
        for (Element e : new Element[]{Element.FIRE, Element.WATER, Element.EARTH}) {
            List<String> half = SpiritDiveDjinnPieces.halfNames(e, false);
            assertTrue(half.containsAll(List.of("crest", "gauntlet_r", "gauntlet_l", "vambrace_r", "vambrace_l", "pauldron_r", "pauldron_l")), e + ": " + half);
            assertFalse(half.contains("chest") || half.contains("greave_r"), "the half equip has no cuirass and no legs");
        }
    }

    @Test
    void fuegoleonsRightArmIsHisFlameSoNoPlateIsListedForIt() {
        for (Stage st : Stage.values()) {
            List<String> names = SpiritDiveDjinnPieces.names(Element.FIRE, st, Grade.FUEGOLEON, true);
            for (String n : names) assertFalse(SpiritDiveDjinnPieces.rightArmPlate(n), "Fuegoleon dive has the right-arm plate " + n);
            assertTrue(names.contains("gauntlet_l") && names.contains("pauldron_l"), "his left arm is plated");
        }
        assertTrue(SpiritDiveDjinnPieces.names(Element.FIRE, Stage.FULL, Grade.STANDARD, false).contains("gauntlet_r"), "every other Fire dive plates both arms");
        assertTrue(SpiritDiveDjinnPieces.names(Element.WIND, Stage.FULL, Grade.FUEGOLEON, false).contains("gauntlet_r"), "a stray grade removes nothing");
        assertEquals(SpiritDivePieces.SHELL_ALL & ~SpiritDivePieces.SHELL_ARM_R, SpiritDiveDjinnPieces.veinParts(Element.FIRE, Stage.FULL, Grade.FUEGOLEON), "no veins on the flame arm either");
        assertEquals(SpiritDivePieces.SHELL_ALL, SpiritDiveDjinnPieces.veinParts(Element.FIRE, Stage.FULL, Grade.STANDARD));
        assertEquals(SpiritDivePieces.SHELL_ARM_L | SpiritDivePieces.SHELL_LEG_L, SpiritDiveDjinnPieces.veinParts(Element.WIND, Stage.HALF, Grade.YUNO));
        assertEquals(0, SpiritDiveDjinnPieces.veinParts(Element.WIND, Stage.FULL, Grade.STANDARD) & ~SpiritDivePieces.SHELL_ALL);
    }

    @Test
    void anUpgradeKeepsTheHalfPiecesOnAndForgesOnlyTheMissingOnes() {
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true}) {
                if (ch && SpiritDiveTier.gradeOfElement(e) == Grade.STANDARD) continue;
                Grade g = ch ? SpiritDiveTier.gradeOfElement(e) : Grade.STANDARD;
                int forging = 0;
                for (String n : SpiritDiveDjinnPieces.names(e, Stage.UPGRADE, g, ch)) {
                    boolean half = SpiritDiveDjinnPieces.halfNames(e, ch).contains(n) && !(SpiritDiveDjinnPieces.moltenRightArm(e, g) && SpiritDiveDjinnPieces.rightArmPlate(n));
                    assertEquals(half, SpiritDiveDjinnPieces.alreadyOn(e, Stage.UPGRADE, g, ch, n), e + " " + n);
                    if (!half) forging++;
                    assertFalse(SpiritDiveDjinnPieces.alreadyOn(e, Stage.FULL, g, ch, n), "only an UPGRADE keeps pieces on");
                    assertFalse(SpiritDiveDjinnPieces.alreadyOn(e, Stage.HALF, g, ch, n));
                }
                assertTrue(forging > 0, e + ": an upgrade must have something to forge");
                assertEquals(SpiritDiveDjinnPieces.names(e, Stage.FULL, g, ch), SpiritDiveDjinnPieces.names(e, Stage.UPGRADE, g, ch), "UPGRADE lists the pieces of FULL");
            }
    }

    // ------------------------------------------------------------------ the forge order (the Djinn's: light layers first, plates, the cuirass, then the back)
    @Test
    void theForgeFollowsTheWeightOfAPieceAndStaysInsideTheDjinnsTwelveTicks() {
        float belt = delay("belt"), gaunt = delay("gauntlet_r"), vamb = delay("vambrace_r"), pauld = delay("pauldron_r"), chest = delay("chest"), back = delay("backplate"), wing = delay("wing_r"),
                crystals = delay("backcrystals");
        assertTrue(belt < gaunt && gaunt <= vamb && vamb < pauld && pauld < chest && chest < back && back <= wing && wing <= crystals, "belt " + belt + " gauntlet " + gaunt + " vambrace " + vamb + " pauldron "
                + pauld + " chest " + chest + " backplate " + back + " wing " + wing);
        for (Element e : Element.values())
            for (boolean ch : new boolean[]{false, true})
                for (String n : SpiritDiveDjinnPieces.fullNames(e, ch)) {
                    Def d = SpiritDiveDjinnPieces.def(n, ch);
                    assertTrue(d.delay() >= 0f && d.delay() <= 12f, n + " delay " + d.delay());
                    float mag = Math.abs(d.dx()) + Math.abs(d.dy()) + Math.abs(d.dz());
                    assertTrue(mag >= 2f && mag <= 36f || mag == 0f, n + " fly-in " + mag);
                }
        List<Def> all = SpiritDiveDjinnPieces.pieces(Element.WIND, Stage.FULL, Grade.YUNO, true);
        assertTrue(SpiritDiveDjinnPieces.forgeEnd(all) <= 20f, "the last piece is on 20 ticks after the start: before the shatter window of an 40 tick lease");
    }

    private static float delay(String n) { return SpiritDiveDjinnPieces.def(n, true).delay(); }

    @Test
    void theAnchorsMatchTheBodyPartsTheModelsWereBuiltFor() {
        for (String n : SpiritDiveDjinnPieces.fullNames(Element.FIRE, true)) {
            Def d = SpiritDiveDjinnPieces.def(n, true);
            SpiritDivePieces.Anchor want = n.equals("helm") || n.startsWith("crest") || n.equals("plume") ? SpiritDivePieces.Anchor.HEAD
                    : n.endsWith("_r") && (n.startsWith("pauldron") || n.startsWith("vambrace") || n.startsWith("gauntlet")) ? SpiritDivePieces.Anchor.ARM_R
                    : n.endsWith("_l") && (n.startsWith("pauldron") || n.startsWith("vambrace") || n.startsWith("gauntlet")) ? SpiritDivePieces.Anchor.ARM_L
                    : n.endsWith("_r") && (n.startsWith("cuisse") || n.startsWith("greave") || n.startsWith("sabaton")) ? SpiritDivePieces.Anchor.LEG_R
                    : n.endsWith("_l") && (n.startsWith("cuisse") || n.startsWith("greave") || n.startsWith("sabaton")) ? SpiritDivePieces.Anchor.LEG_L : SpiritDivePieces.Anchor.BODY;
            assertEquals(want, d.anchor(), n);
        }
    }

    // ------------------------------------------------------------------ levels of detail
    @Test
    void theLevelsOfDetailDropTheDetailThenEverythingButTheSilhouette() {
        List<Def> all = SpiritDiveDjinnPieces.pieces(Element.WIND, Stage.FULL, Grade.YUNO, true);
        int full = 0, mid = 0, far = 0;
        for (Def d : all) {
            if (SpiritDiveDjinnPieces.drawn(d, Lod.FULL)) full++;
            if (SpiritDiveDjinnPieces.drawn(d, Lod.MID)) mid++;
            if (SpiritDiveDjinnPieces.drawn(d, Lod.FAR)) far++;
            assertFalse(SpiritDiveDjinnPieces.drawn(d, Lod.CULLED), "nothing is drawn beyond 64 blocks");
            assertEquals(1, SpiritDiveDjinnPieces.passes(d, Lod.FAR), "no glow pass at the far level");
        }
        assertEquals(all.size(), full);
        assertTrue(mid < full && mid > far && far > 0, "full " + full + " mid " + mid + " far " + far);
        assertEquals(Lod.FULL, SpiritDiveTier.lod(24.0 * 24.0));
        assertEquals(Lod.MID, SpiritDiveTier.lod(24.1 * 24.1));
        assertEquals(Lod.FAR, SpiritDiveTier.lod(48.5 * 48.5));
        assertEquals(Lod.CULLED, SpiritDiveTier.lod(64.5 * 64.5));
        assertEquals(3, SpiritDiveTier.CHARACTER_PER_FRAME, "at most three character dives a frame");
        for (Lod lod : Lod.values()) assertTrue(SpiritDiveDjinnPieces.extrasQuads(true, lod) >= SpiritDiveDjinnPieces.extrasQuads(false, lod) || lod == Lod.FAR);
        assertEquals(0, SpiritDiveDjinnPieces.extrasQuads(true, Lod.CULLED));
        assertTrue(SpiritDiveDjinnPieces.extrasQuads(true, Lod.FULL) > SpiritDiveDjinnPieces.extrasQuads(true, Lod.MID));
    }

    // ------------------------------------------------------------------ budgets
    @Test
    void theCapsAreTheDjinnFullEquipsOwnNumbersAndTheWorstCaseIsBounded() {
        assertEquals(4351, SpiritDiveDjinnPieces.DJINN_QUADS_MAX);
        assertEquals(2856, SpiritDiveDjinnPieces.DJINN_QUADS_MIN);
        int c = SpiritDiveDjinnPieces.pieceVertexCap(true), s = SpiritDiveDjinnPieces.pieceVertexCap(false);
        assertTrue(c > s && s > 8000);
        assertEquals(4351 * 4, SpiritDiveDjinnPieces.worstVertices(true, c), "the character dive at its cap is exactly the Djinn's largest set");
        assertEquals(2856 * 4, SpiritDiveDjinnPieces.worstVertices(false, s), "the standard dive at its cap is exactly the Djinn's smallest set");
        // a crowd: three character dives and the rest standard
        int crowd = SpiritDiveDjinnPieces.worstFrameVertices(4351 * 4, 2856 * 4, 20);
        assertEquals(3 * 4351 * 4 + 17 * 2856 * 4, crowd);
        assertEquals(4351 * 4 * 2, SpiritDiveDjinnPieces.worstFrameVertices(4351 * 4, 2856 * 4, 2) );
        assertEquals(576, SpiritDiveDjinnPieces.veinVertices());
        assertEquals(2, SpiritDiveDjinnPieces.flashRings(false));
        assertEquals(3, SpiritDiveDjinnPieces.flashRings(true));
        assertEquals(12, SpiritDiveDjinnPieces.motes(true, Lod.FULL));
        assertEquals(0, SpiritDiveDjinnPieces.motes(false, Lod.FAR));
    }

    @Test
    void thePassesCountTheGlowMapOnlyForPiecesThatHaveGlow() {
        for (String n : SpiritDiveDjinnPieces.fullNames(Element.WIND, true)) {
            Def d = SpiritDiveDjinnPieces.def(n, true);
            assertEquals(d.glow() > 0f ? 2 : 1, d.passes(), n);
        }
        assertEquals(1, SpiritDiveDjinnPieces.def("belt", true).passes());
        assertEquals(2, SpiritDiveDjinnPieces.def("chest", false).passes());
    }

    // ------------------------------------------------------------------ the forge flash and the particle trail
    @Test
    void theFlashOpensTheForgeAndDiesAway() {
        assertEquals(1f, SpiritDiveDjinnPieces.flash(0f));
        assertEquals(0f, SpiritDiveDjinnPieces.flash(SpiritDiveDjinnPieces.FLASH_TICKS));
        assertTrue(SpiritDiveDjinnPieces.flash(4f) > SpiritDiveDjinnPieces.flash(9f));
        assertTrue(SpiritDiveDjinnPieces.bigForge(true) > SpiritDiveDjinnPieces.bigForge(false), "the character tier forges with the bigger shock");
    }

    @Test
    void theTrailIsRestrainedAndBounded() {
        int std = 0, chr = 0;
        for (long t = 0; t < 40; t++) { std += SpiritDiveDjinnPieces.trailCount(false, t, 7); chr += SpiritDiveDjinnPieces.trailCount(true, t, 7); }
        assertEquals(10, std, "5 a second for the standard set");
        assertEquals(20, chr, "10 a second for the character set: the Djinn's own ambient is 6 to 13");
        assertTrue(SpiritDiveDjinnPieces.entryBurst(true) > SpiritDiveDjinnPieces.entryBurst(false));
        assertTrue(SpiritDiveDjinnPieces.entryBurst(true) <= 16);
        assertTrue(SpiritDiveDjinnPieces.worstTrailPerSecond() <= SpiritDiveTier.worstTrailPerSecond(), "fewer particles than the remake's trail");
        for (int id = 0; id < 8; id++) for (long t = 0; t < 8; t++) assertTrue(SpiritDiveDjinnPieces.trailCount(true, t, id) <= 1);
    }

    // ------------------------------------------------------------------ the forge sounds
    @Test
    void theForgeSoundsAreFewAndKnownAndRateLimited() {
        for (SpiritDiveForge.Kind k : SpiritDiveForge.Kind.values())
            for (Stage st : Stage.values())
                for (boolean ch : new boolean[]{false, true}) {
                    SpiritDiveForge.Cue[] cues = SpiritDiveForge.cue(k, st, ch);
                    assertTrue(cues.length >= 1 && cues.length <= SpiritDiveForge.MAX_SOUNDS, k + "/" + st + "/" + ch);
                    for (SpiritDiveForge.Cue c : cues) assertTrue(c.volume() > 0f && c.volume() <= 2f && c.pitchScale() > 0.4f && c.pitchScale() < 2f);
                }
        assertTrue(SpiritDiveForge.cue(SpiritDiveForge.Kind.BEGIN, Stage.FULL, true).length > SpiritDiveForge.cue(SpiritDiveForge.Kind.BEGIN, Stage.HALF, true).length, "the full dive sounds bigger than the half dive");
        assertTrue(SpiritDiveForge.pitch(Element.WIND) > SpiritDiveForge.pitch(Element.WATER) && SpiritDiveForge.pitch(Element.WATER) > SpiritDiveForge.pitch(Element.FIRE)
                && SpiritDiveForge.pitch(Element.FIRE) > SpiritDiveForge.pitch(Element.EARTH));
        assertTrue(SpiritDiveForge.allowed(0L, 5L), "never played: allowed");
        assertFalse(SpiritDiveForge.allowed(100L, 110L), "less than a second after the last cue");
        assertTrue(SpiritDiveForge.allowed(100L, 100L + SpiritDiveForge.COOLDOWN_TICKS));
        assertTrue(SpiritDiveForge.allowed(100L, 50L), "a clock that went back (a world reload) never silences a player for good");
    }
}
