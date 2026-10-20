package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Def;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Spirit Dive of the four spirits is drawn from lists (book.SpiritDivePieces: which pieces an element shows at a stage), and a wrong list fails silently in play: a half dive that
 * shows the right half of the body would break Yuno's canon (only the LEFT half transforms at first), a Saint Valkyrie Dress that keeps the water wings would draw two pairs of wings, a piece
 * with no model file would draw nothing. This test checks the pure selection (stage to pieces, the upgrade that forges only the missing half, the style number the server sends the clients) and
 * that every piece the lists name has its geo model, texture and glow map in the resources. Pure (no game classes); the working directory is the project root.
 */
class SpiritDivePiecesTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    @Test
    void styleNumberRoundTripsForEveryElementAndStage() {
        Set<Integer> seen = new HashSet<>();
        for (Element e : Element.values()) {
            for (Stage s : Stage.values()) {
                int style = SpiritDivePieces.style(e, s);
                assertTrue(SpiritDivePieces.valid(style), e + "/" + s);
                assertEquals(e, SpiritDivePieces.element(style));
                assertEquals(s, SpiritDivePieces.stage(style));
                assertTrue(style >= 0 && style < 16, "the style is a small number: it travels in a byte");
                assertTrue(seen.add(style), "two dives share the style " + style);
            }
        }
        assertEquals(12, seen.size());
    }

    @Test
    void aBadStyleByteNeverThrows() {
        for (int bad : new int[]{-1, 3 + 12, 15, 99, 127, -128}) {
            if (SpiritDivePieces.valid(bad)) continue;
            assertEquals(Element.WIND, SpiritDivePieces.element(bad));
            assertEquals(Stage.HALF, SpiritDivePieces.stage(bad));
        }
        assertFalse(SpiritDivePieces.valid(12));          // stage 3 does not exist
        assertTrue(SpiritDivePieces.valid(11));           // earth / upgrade
    }

    @Test
    void theStageComesFromTheDiveScaleOfTheArmourRules() {
        assertEquals(Stage.HALF, SpiritDivePieces.stageFor(ArmourRules.diveScale(0, false)));
        assertEquals(Stage.HALF, SpiritDivePieces.stageFor(ArmourRules.diveScale(ArmourRules.HALF_DIVES - 1, false)));
        assertEquals(Stage.FULL, SpiritDivePieces.stageFor(ArmourRules.diveScale(ArmourRules.HALF_DIVES, false)));
        assertEquals(Stage.FULL, SpiritDivePieces.stageFor(ArmourRules.diveScale(0, true)));     // mastery makes any dive full
    }

    @Test
    void theMagicsMapToTheirElement() {
        assertEquals(Element.WIND, SpiritDivePieces.elementOf("WIND"));
        assertEquals(Element.WATER, SpiritDivePieces.elementOf("water"));
        assertEquals(Element.FIRE, SpiritDivePieces.elementOf("FLAME"));
        assertEquals(Element.EARTH, SpiritDivePieces.elementOf("EARTH"));
        assertNull(SpiritDivePieces.elementOf("LIGHT"));
        assertNull(SpiritDivePieces.elementOf("BRIAR"));
        assertNull(SpiritDivePieces.elementOf(null));
    }

    // ------------------------------------------------------------------ wind: Yuno
    @Test
    void theFirstWindDiveTransformsOnlyTheLeftHalf() {
        List<String> half = SpiritDivePieces.pieces(Element.WIND, Stage.HALF);
        assertFalse(half.isEmpty());
        int wings = 0, crowns = 0, rings = 0;
        for (String id : half) {
            assertTrue(id.endsWith("_l"), "a right or centre piece in the half dive: " + id);
            if (id.contains("wind_wing_")) wings++;
            if (id.contains("wind_crown_")) crowns++;
            if (id.contains("wind_ring_")) rings++;
        }
        assertEquals(1, wings, "ONE wind wing");
        assertEquals(1, crowns, "HALF a crown");
        assertEquals(1, rings, "a ring round ONE wrist");
        assertTrue(SpiritDivePieces.halfBody(Element.WIND, Stage.HALF));
        assertFalse(SpiritDivePieces.halfBody(Element.WIND, Stage.FULL));
        assertFalse(SpiritDivePieces.halfBody(Element.FIRE, Stage.HALF), "only Yuno's dive is a half body");
    }

    @Test
    void theFullWindDiveHasTwoWingsAFullCrownAndTwoRings() {
        List<String> full = SpiritDivePieces.pieces(Element.WIND, Stage.FULL);
        assertTrue(full.containsAll(SpiritDivePieces.pieces(Element.WIND, Stage.HALF)), "the full dive keeps everything of the half");
        for (String side : new String[]{"l", "r"}) {
            assertTrue(full.contains("spiritdive:wind_wing_" + side));
            assertTrue(full.contains("spiritdive:wind_crown_" + side));
            assertTrue(full.contains("spiritdive:wind_ring_" + side));
            assertTrue(full.contains("spiritdive:wind_mantle_" + side));
        }
        assertTrue(full.contains("spiritdive:wind_crown_c"), "the full crown joins the halves");
        // symmetrical: every left piece has its right twin
        for (String id : full) if (id.endsWith("_l")) assertTrue(full.contains(id.substring(0, id.length() - 1) + "r"), "no right twin of " + id);
    }

    @Test
    void anUpgradeKeepsTheLeftHalfOnAndForgesOnlyTheMissingHalf() {
        assertEquals(SpiritDivePieces.pieces(Element.WIND, Stage.FULL), SpiritDivePieces.pieces(Element.WIND, Stage.UPGRADE));
        for (String id : SpiritDivePieces.halfPieces(Element.WIND)) assertTrue(SpiritDivePieces.alreadyOn(Element.WIND, Stage.UPGRADE, id), id + " must stay on");
        List<String> forging = SpiritDivePieces.forging(Element.WIND, Stage.UPGRADE);
        assertFalse(forging.isEmpty());
        for (String id : forging) assertTrue(id.endsWith("_r") || id.endsWith("_c"), "a left piece forges again in an upgrade: " + id);
        assertEquals(SpiritDivePieces.pieces(Element.WIND, Stage.FULL).size() - SpiritDivePieces.halfPieces(Element.WIND).size(), forging.size());
        // outside an upgrade nothing is "already on": every piece forges in from the start
        for (Stage s : new Stage[]{Stage.HALF, Stage.FULL})
            for (String id : SpiritDivePieces.pieces(Element.WIND, s)) assertFalse(SpiritDivePieces.alreadyOn(Element.WIND, s, id));
    }

    @Test
    void aPieceTheFullDiveDropsIsNotKeptInAnUpgrade() {
        // fire's small wings are replaced by the full pair: they are no longer drawn in an upgrade, so they cannot be "already on"
        String bud = "spiritdive:fire_wingbud_r";
        assertTrue(SpiritDivePieces.pieces(Element.FIRE, Stage.HALF).contains(bud));
        assertFalse(SpiritDivePieces.pieces(Element.FIRE, Stage.UPGRADE).contains(bud));
        assertFalse(SpiritDivePieces.alreadyOn(Element.FIRE, Stage.UPGRADE, bud));
        assertTrue(SpiritDivePieces.alreadyOn(Element.FIRE, Stage.UPGRADE, "spiritdive:fire_arm_r"));
    }

    // ------------------------------------------------------------------ water: Noelle
    @Test
    void theHalfWaterDiveIsTheValkyrieArmourAndTheFullOneTheSaintDress() {
        List<String> valk = SpiritDivePieces.pieces(Element.WATER, Stage.HALF), saint = SpiritDivePieces.pieces(Element.WATER, Stage.FULL);
        assertFalse(SpiritDivePieces.saint(Element.WATER, Stage.HALF));
        assertTrue(SpiritDivePieces.saint(Element.WATER, Stage.FULL));
        assertTrue(SpiritDivePieces.saint(Element.WATER, Stage.UPGRADE));
        assertFalse(SpiritDivePieces.saint(Element.FIRE, Stage.FULL));
        for (String id : valk) assertTrue(id.startsWith("water:"), "the Valkyrie dive is the old water armour only: " + id);
        assertTrue(valk.contains("water:wing_r") && valk.contains("water:halo"));
        // the saint dress: the same water armour ...
        for (String keep : new String[]{"water:chest", "water:skirt", "water:pauldron_r", "water:pauldron_l", "water:vambrace_r", "water:gauntlet_l", "water:greave_r", "water:helm", "water:tail"})
            assertTrue(saint.contains(keep), "the Saint Valkyrie Dress keeps her usual water armour: " + keep);
        // ... plus feathered wing-like parts and a halo, without the water wings and the droplet halo
        for (String add : new String[]{"spiritdive:saint_wing_r", "spiritdive:saint_wing_l", "spiritdive:saint_halo", "spiritdive:saint_sleeve_r", "spiritdive:saint_sleeve_l", "spiritdive:saint_sash"})
            assertTrue(saint.contains(add), add);
        for (String gone : new String[]{"water:wing_r", "water:wing_l", "water:halo", "water:backdrop"}) assertFalse(saint.contains(gone), gone);
    }

    // ------------------------------------------------------------------ fire and earth
    @Test
    void fireHasArmsClawsAndWingsAtBothStages() {
        for (Stage s : new Stage[]{Stage.HALF, Stage.FULL}) {
            List<String> l = SpiritDivePieces.pieces(Element.FIRE, s);
            for (String side : new String[]{"r", "l"}) {
                assertTrue(l.contains("spiritdive:fire_arm_" + side), "draconic arm " + s);
                assertTrue(l.contains("spiritdive:fire_claws_" + side), "claws " + s);
                assertTrue(l.contains("spiritdive:fire_" + (s == Stage.HALF ? "wingbud_" : "wing_") + side), "fiery wing " + s);
            }
        }
        assertTrue(SpiritDivePieces.pieces(Element.FIRE, Stage.FULL).contains("spiritdive:fire_greave_r"));
        assertFalse(SpiritDivePieces.pieces(Element.FIRE, Stage.HALF).contains("spiritdive:fire_greave_r"));
    }

    @Test
    void theEarthDiveIsGenericStoneAndGrows() {
        List<String> half = SpiritDivePieces.pieces(Element.EARTH, Stage.HALF), full = SpiritDivePieces.pieces(Element.EARTH, Stage.FULL);
        assertTrue(full.containsAll(half));
        assertTrue(full.size() > half.size());
        for (String id : full) assertTrue(id.startsWith("spiritdive:earth_"), "nothing but earth plates: " + id);
        assertTrue(full.contains("spiritdive:earth_orbit") && full.contains("spiritdive:earth_helm"));
    }

    // ------------------------------------------------------------------ the lists as a whole
    @Test
    void everyListIsWellFormed() {
        Set<String> known = new HashSet<>();
        for (Def d : SpiritDivePieces.all()) assertTrue(known.add(d.id()), "listed twice: " + d.id());
        for (Element e : Element.values()) {
            for (Stage s : Stage.values()) {
                List<String> l = SpiritDivePieces.pieces(e, s);
                assertEquals(new HashSet<>(l).size(), l.size(), "a piece twice in " + e + "/" + s);
                for (String id : l) assertNotNull(SpiritDivePieces.def(id), "no definition of " + id);
            }
        }
        for (Def d : SpiritDivePieces.all()) {
            assertTrue(d.delay() >= 0f && d.delay() <= 12f, d.id() + ": the stagger of EquipAnimation is 0 to 12 ticks");
            assertFalse(d.key().isEmpty() || d.name().isEmpty(), d.id());
            assertTrue(d.key().equals("water") || d.key().equals("spiritdive"), d.id());
        }
    }

    @Test
    void theBodyOverlayFollowsTheTransformedHalf() {
        int left = SpiritDivePieces.shellParts(Element.WIND, Stage.HALF);
        assertEquals(SpiritDivePieces.SHELL_ARM_L | SpiritDivePieces.SHELL_LEG_L, left, "the half wind dive covers the left arm and leg only");
        assertEquals(0, left & SpiritDivePieces.SHELL_ARM_R);
        assertEquals(SpiritDivePieces.SHELL_ALL, SpiritDivePieces.shellParts(Element.WIND, Stage.FULL));
        assertEquals(SpiritDivePieces.SHELL_ALL, SpiritDivePieces.shellParts(Element.WIND, Stage.UPGRADE));
        assertEquals(SpiritDivePieces.SHELL_ARM_R | SpiritDivePieces.SHELL_ARM_L, SpiritDivePieces.shellParts(Element.FIRE, Stage.HALF), "fire: the arms are cloaked");
        for (Element e : Element.values()) for (Stage s : Stage.values()) assertTrue(SpiritDivePieces.shellParts(e, s) != 0);
    }

    // ------------------------------------------------------------------ assets
    @Test
    void everyPieceHasItsModelTextureAndGlowMap() {
        List<String> missing = new ArrayList<>();
        SpiritDivePieces.forEachId(id -> {
            String stem = SpiritDivePieces.key(id) + "_arm_" + SpiritDivePieces.name(id);
            for (String rel : new String[]{"geo/entity/" + stem + ".geo.json", "textures/entity/" + stem + ".png", "textures/entity/" + stem + "_glow.png"})
                if (!Files.isRegularFile(ASSETS.resolve(rel))) missing.add(rel);
        });
        assertTrue(missing.isEmpty(), "missing: " + missing);
    }

    @Test
    void everyDiveHasItsBodyOverlayTextures() {
        List<String> missing = new ArrayList<>();
        for (String el : new String[]{"wind", "water", "fire", "earth"})
            for (String part : new String[]{"shell", "haze"})
                for (String suffix : new String[]{".png", "_glow.png"})
                    if (!Files.isRegularFile(ASSETS.resolve("textures/aura/spirit_" + el + "_" + part + suffix))) missing.add("spirit_" + el + "_" + part + suffix);
        assertTrue(missing.isEmpty(), "missing: " + missing);
    }

    @Test
    void thePainterOnlyNamesTexturesThatExist() throws IOException {
        String src = Files.readString(Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp/client/aura/SpiritDiveAura.java"), StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("AuraRender\\.tex\\(\"([a-z0-9_/]+)\"\\)").matcher(src);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(Files.isRegularFile(ASSETS.resolve("textures/" + m.group(1) + ".png")), "the painter names a texture that does not exist: " + m.group(1));
        }
        assertTrue(n >= 6, "the painter names its sprites with AuraRender.tex (found " + n + ")");
    }

    @Test
    void theNewClassesNeverTouchFoodOrHunger() throws IOException {
        String[] files = {"book/SpiritDivePieces.java", "book/SpiritDiveForm.java", "client/aura/SpiritDiveAura.java", "client/SpiritLooks.java", "client/GreatSpiritModels.java"};
        for (String f : files) {
            String src = Files.readString(Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp").resolve(f), StandardCharsets.UTF_8);
            String code = src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
            for (String bad : new String[]{"causeFoodExhaustion", "FoodData", "getFoodData", "setFoodLevel", "addExhaustion", "setSaturation"})
                assertFalse(code.contains(bad), f + " touches hunger: " + bad);
        }
    }
}
