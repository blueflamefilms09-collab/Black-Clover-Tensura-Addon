package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Budget;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Spirit Dive remake has two tiers, and which mage gets which, how much each may draw and from how far, are decisions that fail silently in play (a Fuegoleon look on the wrong mage, a character
 * tier that costs more than its budget, a style number that no longer carries the old dives). This test pins the pure rules of {@link SpiritDiveTier} and the style number of
 * {@link SpiritDivePieces}: the tier follows the bound canon grimoire and the element, the old style numbers are unchanged, the budgets are ordered and bounded, the levels of detail cut at their
 * distances, the colour grading belongs to its character, the particle trail is bounded. Pure (no game classes).
 */
class SpiritDiveTierTest {
    // ------------------------------------------------------------------ which mage gets which tier
    @Test
    void theCharacterTierBelongsToTheCanonCharacterOfTheElement() {
        assertEquals(Grade.YUNO, SpiritDiveTier.gradeFor(Element.WIND, "yuno"));
        assertEquals(Grade.FUEGOLEON, SpiritDiveTier.gradeFor(Element.FIRE, "fuegoleon"));
        assertEquals(Grade.NOELLE, SpiritDiveTier.gradeFor(Element.WATER, "noelle"));
        assertEquals(Grade.YUNO, SpiritDiveTier.gradeFor(Element.WIND, "YUNO"), "the id is read in any case");
    }

    @Test
    void everyoneElseDivesTheStandardTier() {
        for (Element e : Element.values()) {
            assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(e, null));
            assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(e, ""));
            assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(e, "asta"));
            assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(e, "mereoleona"), "another Fire user is not Fuegoleon");
        }
        assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(null, "yuno"));
    }

    @Test
    void aCharacterNeverBorrowsAnotherCharactersLook() {
        assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(Element.FIRE, "yuno"));
        assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(Element.WIND, "fuegoleon"));
        assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(Element.WATER, "yuno"));
        assertEquals(Grade.STANDARD, SpiritDiveTier.gradeFor(Element.WIND, "noelle"));
    }

    @Test
    void earthHasNoCharacterTier() {
        assertEquals(null, SpiritDiveTier.characterOf(Element.EARTH));
        assertEquals(Grade.STANDARD, SpiritDiveTier.gradeOfElement(Element.EARTH));
        for (Grade g : Grade.values()) assertFalse(SpiritDiveTier.character(Element.EARTH, g), "earth drew a character tier for " + g);
    }

    @Test
    void aGradeThatDoesNotBelongToTheElementIsTheStandardLook() {
        assertEquals(Grade.STANDARD, SpiritDiveTier.effective(Element.WATER, Grade.YUNO));
        assertEquals(Grade.YUNO, SpiritDiveTier.effective(Element.WIND, Grade.YUNO));
        assertEquals(Grade.STANDARD, SpiritDiveTier.effective(Element.WIND, null));
        assertTrue(SpiritDiveTier.character(Element.FIRE, Grade.FUEGOLEON));
        assertFalse(SpiritDiveTier.character(Element.FIRE, Grade.STANDARD));
    }

    // ------------------------------------------------------------------ the style number
    @Test
    void theStyleNumberCarriesElementStageAndGrade() {
        Set<Integer> seen = new HashSet<>();
        for (Element e : Element.values()) {
            for (Stage s : Stage.values()) {
                for (Grade g : Grade.values()) {
                    int style = SpiritDivePieces.style(e, s, g);
                    assertTrue(SpiritDivePieces.valid(style), e + "/" + s + "/" + g);
                    assertTrue(style >= 0 && style < 64, "the style travels in a byte: " + style);
                    assertEquals(e, SpiritDivePieces.element(style));
                    assertEquals(s, SpiritDivePieces.stage(style));
                    assertEquals(SpiritDiveTier.effective(e, g), SpiritDivePieces.grade(style));
                    assertTrue(seen.add(style), "two dives share the style " + style);
                }
            }
        }
        assertEquals(48, seen.size());
    }

    @Test
    void theOldStyleNumbersAreUnchanged() {
        for (Element e : Element.values())
            for (Stage s : Stage.values()) {
                assertEquals(e.ordinal() | (s.ordinal() << 2), SpiritDivePieces.style(e, s), "the standard tier must keep the number the 1.99 dive used");
                assertEquals(SpiritDivePieces.style(e, s), SpiritDivePieces.style(e, s, Grade.STANDARD));
                assertEquals(Grade.STANDARD, SpiritDivePieces.grade(SpiritDivePieces.style(e, s)));
            }
    }

    @Test
    void aBadByteIsTheStandardHalfWindDiveAndNeverThrows() {
        for (int bad : new int[]{-1, 64, 99, 127, -128, 12, 13, 14, 15}) {
            assertFalse(SpiritDivePieces.valid(bad), "valid? " + bad);
            assertEquals(Element.WIND, SpiritDivePieces.element(bad));
            assertEquals(Stage.HALF, SpiritDivePieces.stage(bad));
            assertEquals(Grade.STANDARD, SpiritDivePieces.grade(bad));
        }
    }

    @Test
    void aGradeOnTheWrongElementReadsAsStandard() {
        assertEquals(Grade.STANDARD, SpiritDivePieces.grade(SpiritDivePieces.style(Element.WATER, Stage.FULL, Grade.YUNO)));
        assertEquals(Grade.STANDARD, SpiritDivePieces.grade(SpiritDivePieces.style(Element.EARTH, Stage.FULL, Grade.NOELLE)));
    }

    // ------------------------------------------------------------------ the budgets
    @Test
    void theCharacterTierMayDrawMoreThanTheStandardTierAtEveryLevelAndNeverLess() {
        for (Lod l : Lod.values()) {
            Budget s = SpiritDiveTier.budget(false, l), c = SpiritDiveTier.budget(true, l);
            assertTrue(c.columns() >= s.columns() && c.pillars() >= s.pillars() && c.motes() >= s.motes() && c.bits() >= s.bits() && c.strands() >= s.strands() && c.spirals() >= s.spirals() && c.rings() >= s.rings()
                    && c.flares() >= s.flares(), "character below standard at " + l);
            assertTrue(c.quads() >= s.quads());
        }
    }

    @Test
    void nothingIsDrawnBeyondTheCullDistanceAndFarPlayersGetOnlyARing() {
        assertEquals(0, SpiritDiveTier.budget(true, Lod.CULLED).quads());
        assertEquals(0, SpiritDiveTier.budget(false, Lod.CULLED).quads());
        for (boolean ch : new boolean[]{false, true}) {
            Budget far = SpiritDiveTier.budget(ch, Lod.FAR);
            assertEquals(1, far.rings());
            assertEquals(0, far.columns() + far.pillars() + far.motes() + far.bits() + far.strands() + far.spirals() + far.flares());
        }
    }

    @Test
    void theEffectsHaveHardUpperBounds() {
        assertTrue(SpiritDiveTier.worstQuads(false) <= 72, "standard tier: " + SpiritDiveTier.worstQuads(false) + " quads");
        assertTrue(SpiritDiveTier.worstQuads(true) <= 160, "character tier: " + SpiritDiveTier.worstQuads(true) + " quads");
        for (Lod l : Lod.values())
            for (boolean ch : new boolean[]{false, true}) {
                Budget b = SpiritDiveTier.budget(ch, l);
                assertTrue(b.columns() >= 0 && b.pillars() >= 0 && b.motes() >= 0 && b.bits() >= 0 && b.strands() >= 0 && b.spirals() >= 0 && b.rings() >= 0 && b.flares() >= 0);
                assertTrue(b.quads() <= SpiritDiveTier.worstQuads(ch));
            }
        assertTrue(SpiritDiveTier.pieceCap(true) > SpiritDiveTier.pieceCap(false), "the character tier may have more pieces than the standard one");
        assertTrue(SpiritDiveTier.vertexCap(true) > SpiritDiveTier.vertexCap(false));
        assertTrue(SpiritDiveTier.vertexCap(false) > SpiritDiveTier.pieceCap(false), "the whole dive is more than its pieces");
    }

    @Test
    void theRunRingsAndTheEntryAreForTheCharacterTierAtFullDetail() {
        assertEquals(0, SpiritDiveTier.budget(false, Lod.FULL).pillars());
        assertTrue(SpiritDiveTier.budget(true, Lod.FULL).pillars() >= 1);
        assertEquals(0, SpiritDiveTier.budget(true, Lod.MID).pillars(), "no entry pillar past 24 blocks");
        assertTrue(SpiritDiveTier.extras(Lod.FULL));
        assertFalse(SpiritDiveTier.extras(Lod.MID));
        assertFalse(SpiritDiveTier.extras(Lod.FAR));
        assertFalse(SpiritDiveTier.extras(Lod.CULLED));
    }

    @Test
    void thereIsAFrameCapOnCharacterLooks() {
        assertTrue(SpiritDiveTier.CHARACTER_PER_FRAME >= 1 && SpiritDiveTier.CHARACTER_PER_FRAME <= 6);
    }

    // ------------------------------------------------------------------ the levels of detail
    @Test
    void theLevelsOfDetailCutAtTheirDistances() {
        assertEquals(Lod.FULL, SpiritDiveTier.lod(0));
        assertEquals(Lod.FULL, SpiritDiveTier.lod(24.0 * 24.0));
        assertEquals(Lod.MID, SpiritDiveTier.lod(24.0 * 24.0 + 1));
        assertEquals(Lod.MID, SpiritDiveTier.lod(48.0 * 48.0));
        assertEquals(Lod.FAR, SpiritDiveTier.lod(48.0 * 48.0 + 1));
        assertEquals(Lod.FAR, SpiritDiveTier.lod(64.0 * 64.0));
        assertEquals(Lod.CULLED, SpiritDiveTier.lod(64.0 * 64.0 + 1));
        assertEquals(Lod.CULLED, SpiritDiveTier.lod(1e9));
    }

    @Test
    void theLevelNeverGetsBetterWithDistance() {
        int last = 0;
        for (double d = 0; d < 80; d += 0.5) {
            int o = SpiritDiveTier.lod(d * d).ordinal();
            assertTrue(o >= last, "the level improved at " + d);
            last = o;
        }
    }

    // ------------------------------------------------------------------ the colour grading
    @Test
    void eachCharacterHasItsOwnColourGrading() {
        int yuno = SpiritDiveTier.glow(Element.WIND, Grade.YUNO, false), standard = SpiritDiveTier.glow(Element.WIND, Grade.STANDARD, false);
        int yp = SpiritDiveTier.primary(Element.WIND, Grade.YUNO);
        assertTrue(green(yp) > red(yp) && green(yp) > blue(yp), "Yuno is green");
        int fp = SpiritDiveTier.primary(Element.FIRE, Grade.FUEGOLEON);
        assertTrue(red(fp) > 0xC0 && red(fp) > green(fp) * 2 && red(fp) > blue(fp) * 2, "Fuegoleon is crimson");
        int fa = SpiritDiveTier.accent(Element.FIRE, Grade.FUEGOLEON);
        assertTrue(red(fa) > 0xE0 && green(fa) > 0xB0 && blue(fa) < 0x90, "Fuegoleon's accent is gold");
        int np = SpiritDiveTier.primary(Element.WATER, Grade.NOELLE), na = SpiritDiveTier.accent(Element.WATER, Grade.NOELLE);
        assertTrue(blue(np) > red(np) && blue(np) > green(np), "Noelle is blue");
        assertTrue(red(na) > 0xD0 && green(na) > 0xD0 && blue(na) > 0xE0, "Noelle's accent is silver");
        assertTrue(yuno != standard, "the glow of the character differs from the standard wind glow");
        for (Element e : Element.values())
            for (Grade g : Grade.values()) {
                for (int c : new int[]{SpiritDiveTier.glow(e, g, false), SpiritDiveTier.glow(e, g, true), SpiritDiveTier.primary(e, g), SpiritDiveTier.accent(e, g)})
                    assertTrue(c >= 0 && c <= 0xFFFFFF, "a colour is RGB: " + Integer.toHexString(c));
            }
    }

    @Test
    void theStandardColoursAreTheOnesOfThe199Dive() {
        assertEquals(0x98FFD0, SpiritDiveTier.glow(Element.WIND, Grade.STANDARD, false));
        assertEquals(0xFF8A2A, SpiritDiveTier.glow(Element.FIRE, Grade.STANDARD, false));
        assertEquals(0xCFF8FF, SpiritDiveTier.glow(Element.WATER, Grade.STANDARD, false));
        assertEquals(0xFFE49A, SpiritDiveTier.glow(Element.WATER, Grade.STANDARD, true), "the Saint Valkyrie Dress glows gold");
        assertEquals(0xFFB040, SpiritDiveTier.glow(Element.EARTH, Grade.STANDARD, false));
    }

    private static int red(int rgb) { return (rgb >> 16) & 255; }

    private static int green(int rgb) { return (rgb >> 8) & 255; }

    private static int blue(int rgb) { return rgb & 255; }

    // ------------------------------------------------------------------ the entry and the trail
    @Test
    void theEntryRunsFromZeroToOneAndStaysThere() {
        assertEquals(0f, SpiritDiveTier.entry(-3f));
        assertEquals(0f, SpiritDiveTier.entry(0f));
        assertEquals(0.5f, SpiritDiveTier.entry(SpiritDiveTier.ENTRY_TICKS / 2f), 1e-5f);
        assertEquals(1f, SpiritDiveTier.entry(SpiritDiveTier.ENTRY_TICKS));
        assertEquals(1f, SpiritDiveTier.entry(1000f));
    }

    @Test
    void theParticleTrailIsBounded() {
        int standard = 0;
        for (long t = 0; t < 30; t++) {
            standard += SpiritDiveTier.trailCount(false, t, 7);
            assertEquals(2, SpiritDiveTier.trailCount(true, t, 7));
        }
        assertEquals(10, standard, "the standard tier spawns one particle every third tick");
        // a crowd of standard dives is spread over the three phases of the ids
        int crowd = 0;
        for (int id = 0; id < 30; id++) crowd += SpiritDiveTier.trailCount(false, 5, id);
        assertEquals(10, crowd);
        assertTrue(SpiritDiveTier.entryBurst(true) > SpiritDiveTier.entryBurst(false));
        assertTrue(SpiritDiveTier.entryBurst(true) <= 40, "the entry burst stays small");
        assertTrue(SpiritDiveTier.worstTrailPerSecond() <= 300, "worst case " + SpiritDiveTier.worstTrailPerSecond() + " particles a second before the client budget");
        assertTrue(SpiritDiveTier.TRAIL_PLAYERS <= 8 && SpiritDiveTier.TRAIL_RANGE <= 24.0);
    }

    // ------------------------------------------------------------------ the pieces of the tiers
    @Test
    void theSignaturePiecesBelongToTheirCharacterOnly() {
        for (Stage s : new Stage[]{Stage.HALF, Stage.FULL}) {
            for (String id : new String[]{"spiritdive:wind_blades_l"}) {
                assertTrue(SpiritDivePieces.pieces(Element.WIND, s, Grade.YUNO).contains(id));
                assertFalse(SpiritDivePieces.pieces(Element.WIND, s, Grade.STANDARD).contains(id));
            }
            assertTrue(SpiritDivePieces.pieces(Element.FIRE, s, Grade.FUEGOLEON).contains("spiritdive:fire_mane"));
            assertFalse(SpiritDivePieces.pieces(Element.FIRE, s, Grade.STANDARD).contains("spiritdive:fire_mane"));
            for (Grade g : Grade.values()) assertFalse(SpiritDivePieces.pieces(Element.FIRE, s, g).contains("spiritdive:flame_arm_r"), "the molten arm is drawn by the arm aura, not by the dive (a second arm would double its glow)");
            assertTrue(SpiritDivePieces.pieces(Element.WATER, s, Grade.NOELLE).contains("spiritdive:water_ribbon_l"));
            assertFalse(SpiritDivePieces.pieces(Element.WATER, s, Grade.STANDARD).contains("spiritdive:water_ribbon_l"));
        }
        assertTrue(SpiritDivePieces.pieces(Element.WIND, Stage.FULL, Grade.YUNO).contains("spiritdive:wind_halo"));
        assertFalse(SpiritDivePieces.pieces(Element.WIND, Stage.HALF, Grade.YUNO).contains("spiritdive:wind_halo"), "the half dive has half a crown, not the halo");
        assertTrue(SpiritDivePieces.pieces(Element.WATER, Stage.FULL, Grade.NOELLE).contains("spiritdive:water_rings"));
        assertTrue(SpiritDivePieces.flameArm(Element.FIRE, Grade.FUEGOLEON));
        assertFalse(SpiritDivePieces.flameArm(Element.FIRE, Grade.STANDARD));
        assertFalse(SpiritDivePieces.flameArm(Element.WIND, Grade.FUEGOLEON));
    }

    @Test
    void yunosFirstDiveIsStillOnlyTheLeftHalf() {
        for (String id : SpiritDivePieces.pieces(Element.WIND, Stage.HALF, Grade.YUNO))
            assertTrue(id.endsWith("_l"), "a right or centre piece in Yuno's half dive: " + id);
        for (String id : SpiritDivePieces.pieces(Element.WIND, Stage.HALF, Grade.STANDARD))
            assertTrue(id.endsWith("_l"), "a right or centre piece in the half dive: " + id);
    }

    @Test
    void anUpgradeKeepsTheHalfAndForgesOnlyWhatIsMissing() {
        for (Grade g : new Grade[]{Grade.STANDARD, Grade.YUNO}) {
            List<String> half = SpiritDivePieces.halfPieces(Element.WIND, g), full = SpiritDivePieces.pieces(Element.WIND, Stage.UPGRADE, g);
            assertTrue(full.containsAll(half), "the full wind dive keeps everything of the half (" + g + ")");
            for (String id : half) assertTrue(SpiritDivePieces.alreadyOn(Element.WIND, Stage.UPGRADE, g, id));
            for (String id : full) if (!half.contains(id)) assertFalse(SpiritDivePieces.alreadyOn(Element.WIND, Stage.UPGRADE, g, id));
        }
        assertFalse(SpiritDivePieces.alreadyOn(Element.WIND, Stage.FULL, Grade.YUNO, "spiritdive:wind_wing_l"), "outside an upgrade every piece forges in");
    }

    @Test
    void everyRemadeListIsWellFormed() {
        for (Element e : Element.values())
            for (Stage s : Stage.values())
                for (Grade g : Grade.values()) {
                    List<String> l = SpiritDivePieces.pieces(e, s, g);
                    assertFalse(l.isEmpty());
                    assertEquals(new HashSet<>(l).size(), l.size(), "a piece twice in " + e + "/" + s + "/" + g);
                    for (String id : l) assertTrue(SpiritDivePieces.def(id) != null, "no definition of " + id);
                }
    }
}
