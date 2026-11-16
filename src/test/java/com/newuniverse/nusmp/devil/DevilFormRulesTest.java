package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.client.bossmesh.MeshPose;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Devil forms and the devil's figure (docs/devil_forms.md): the pure rules, and the lines of the eight worlds' devils. No game, no asset (those are {@link DevilFormAssetsTest}). */
class DevilFormRulesTest {
    @Test
    void theRolesAreTheMeshPosesAndStayInOrder() {
        assertEquals(MeshPose.ALL_ROLE_NAMES.length, DevilFormRules.COUNT);
        for (int i = 0; i < DevilFormRules.COUNT; i++) assertEquals(MeshPose.ALL_ROLE_NAMES[i], DevilFormRules.ROLES[i], "role " + i);
        assertEquals(MeshPose.WING_L, DevilFormRules.WING_L);
        assertEquals(MeshPose.TAIL, DevilFormRules.TAIL);
    }

    @Test
    void theStagesFollowTheAssimilationAndTheUnionWearsEverything() {
        assertEquals(0, DevilFormRules.stage(19f, false));
        assertEquals(1, DevilFormRules.stage(20f, false));
        assertEquals(2, DevilFormRules.stage(50f, false));
        assertEquals(3, DevilFormRules.stage(100f, false));
        assertEquals(3, DevilFormRules.stage(0f, true));
        for (int r = 0; r < DevilFormRules.COUNT; r++) {
            assertTrue(DevilFormRules.wants(r, 100f, false), "the whole form at 100: " + r);
            assertTrue(DevilFormRules.wants(r, 0f, true), "the union wears every part: " + r);
            assertFalse(DevilFormRules.wants(r, 0f, false), "nothing at 0: " + r);
        }
        assertTrue(DevilFormRules.wants(DevilFormRules.HEAD, 20f, false));
        assertFalse(DevilFormRules.wants(DevilFormRules.WING_L, 49f, false));
        assertTrue(DevilFormRules.wants(DevilFormRules.WING_L, 50f, false));
        assertFalse(DevilFormRules.wants(DevilFormRules.TORSO, 99f, false));
        assertEquals(DevilRules.HORNS_AT, DevilFormRules.HEAD_AT);
        assertEquals(DevilRules.WINGS_AT, DevilFormRules.LIMBS_AT);
        assertEquals(DevilRules.UNION_AT, DevilFormRules.FULL_AT);
    }

    @Test
    void everyRoleHasAnAnchorADelayAndAFlyInDirection() {
        Set<Integer> anchors = new HashSet<>();
        for (int r = 0; r < DevilFormRules.COUNT; r++) {
            int a = DevilFormRules.anchorOf(r);
            assertTrue(a >= DevilFormRules.A_HEAD && a <= DevilFormRules.A_LEG_L, "anchor of " + r);
            anchors.add(a);
            float d = DevilFormRules.delay(r);
            assertTrue(d >= 0f && d <= 12f, "the Djinn's stagger is 0 to 12 ticks: " + r);
            for (int axis = 0; axis < 3; axis++) assertTrue(Math.abs(DevilFormRules.fly(r, axis)) <= 16f, "fly-in " + r + "/" + axis);
        }
        assertEquals(6, anchors.size(), "every body part carries something");
        assertEquals(DevilFormRules.A_BODY, DevilFormRules.anchorOf(DevilFormRules.WING_L));
        assertEquals(DevilFormRules.A_BODY, DevilFormRules.anchorOf(DevilFormRules.TAIL));
        assertEquals(DevilFormRules.A_ARM_L, DevilFormRules.anchorOf(DevilFormRules.FOREARM_L));
        assertEquals(DevilFormRules.TORSO, DevilFormRules.leaderOf(DevilFormRules.WING_R));
    }

    @Test
    void theCrowdBudgetIsFourFullSixFarAndNothingBeyondFortyEightBlocks() {
        assertEquals(NoelleFormRules.Lod.STANDARD, DevilFormRules.wanted(10 * 10));
        assertEquals(NoelleFormRules.Lod.STANDARD, DevilFormRules.wanted(24 * 24));
        assertEquals(NoelleFormRules.Lod.FAR, DevilFormRules.wanted(30 * 30));
        assertEquals(NoelleFormRules.Lod.FAR, DevilFormRules.wanted(48 * 48));
        assertEquals(NoelleFormRules.Lod.NONE, DevilFormRules.wanted(49 * 49));
        assertEquals(NoelleFormRules.Lod.STANDARD, NoelleFormRules.budgeted(NoelleFormRules.Lod.STANDARD, 0, 0, 0));
        assertEquals(NoelleFormRules.Lod.FAR, NoelleFormRules.budgeted(NoelleFormRules.Lod.STANDARD, 0, NoelleFormRules.MAX_FULL_PER_FRAME, 0));
        assertEquals(NoelleFormRules.Lod.NONE, NoelleFormRules.budgeted(NoelleFormRules.Lod.STANDARD, 0, NoelleFormRules.MAX_FULL_PER_FRAME, NoelleFormRules.MAX_FAR_PER_FRAME));
        assertEquals(3000, DevilFormRules.MAX_QUADS);
    }

    @Test
    void theGlowGrowsWithTheStagesAndTheAttributesHaveARingAndAMotion() {
        assertEquals(0f, DevilFormRules.glow(10f, false, 0f));
        assertTrue(DevilFormRules.glow(20f, false, 0f) > 0f);
        assertTrue(DevilFormRules.glow(50f, false, 0f) > DevilFormRules.glow(20f, false, 0f));
        assertTrue(DevilFormRules.glow(100f, false, 0f) > DevilFormRules.glow(50f, false, 0f));
        for (DevilAttribute a : DevilAttribute.values()) {
            assertTrue(DevilFormRules.ringOf(a) >= 0 && DevilFormRules.ringOf(a) <= 4, a.name());
            assertTrue(DevilFormRules.moteMode(a) >= 0 && DevilFormRules.moteMode(a) <= 3, a.name());
        }
        assertEquals(6, DevilFormRules.motes(NoelleFormRules.Lod.STANDARD, true));
        assertEquals(0, DevilFormRules.motes(NoelleFormRules.Lod.FAR, true));
        assertEquals(0, DevilFormRules.motes(NoelleFormRules.Lod.STANDARD, false));
    }

    @Test
    void theFilesOfADevilAreNamedByItsId() {
        assertEquals("devil_god_hand", DevilFormRules.meshId("god_hand"));
        assertEquals("assets/anime_multiverse/entity_meshes/devil_god_hand.txt", DevilFormRules.meshPath("god_hand"));
        assertEquals("assets/anime_multiverse/entity_meshes/devil_god_hand_lod.txt", DevilFormRules.lodPath("god_hand"));
        assertEquals("assets/anime_multiverse/textures/entity/boss_mesh/devil_god_hand.png", DevilFormRules.skinPath("god_hand"));
        assertEquals("assets/anime_multiverse/textures/entity/boss_mesh/devil_god_hand_glow.png", DevilFormRules.glowPath("god_hand"));
    }

    // ---------------------------------------------------------------- the figure
    @Test
    void theFigureRulesBoundItsLifeAndItsCount() {
        assertTrue(DevilNpcRules.mayStart(true, false, 0, 8));
        assertFalse(DevilNpcRules.mayStart(false, false, 0, 8), "the setting off");
        assertFalse(DevilNpcRules.mayStart(true, true, 0, 8), "one per player");
        assertFalse(DevilNpcRules.mayStart(true, false, 8, 8), "the server cap");
        assertFalse(DevilNpcRules.mayStart(true, false, 0, 0), "a cap of 0 is off");
        assertEquals(DevilNpcRules.MAX_MAX, DevilNpcRules.clampMax(10_000));
        assertEquals(0, DevilNpcRules.clampMax(-3));
        assertEquals(60 * 20L, DevilNpcRules.lifetimeTicks(DevilNpcRules.DEFAULT_LIFE_SECONDS));
        assertEquals(DevilNpcRules.MIN_LIFE_SECONDS * 20L, DevilNpcRules.lifetimeTicks(0));
        assertEquals(DevilNpcRules.MAX_LIFE_SECONDS * 20L, DevilNpcRules.lifetimeTicks(100_000));
        assertTrue(DevilNpcRules.DEFAULT_LIFE_SECONDS * 20 > 30 * 20 + 10 * 20, "the life covers a ritual and the 30 second offer");
        for (int s = 0; s < DevilNpcRules.STATES; s++) assertEquals(s, DevilNpcRules.clampState(s));
        assertEquals(0, DevilNpcRules.clampState(-4));
        assertEquals(DevilNpcRules.STATES - 1, DevilNpcRules.clampState(99));
        assertTrue(DevilNpcRules.terminal(DevilNpcRules.MERGE) && DevilNpcRules.terminal(DevilNpcRules.DISSOLVE));
        assertFalse(DevilNpcRules.terminal(DevilNpcRules.WAITING));
        assertEquals(DevilNpcRules.MERGE_TICKS, DevilNpcRules.endTicks(DevilNpcRules.MERGE));
        assertEquals(DevilNpcRules.DISSOLVE_TICKS, DevilNpcRules.endTicks(DevilNpcRules.DISSOLVE));
    }

    @Test
    void theFiguresOwnCheckVanishesDissolvesOrStays() {
        long life = 1200;
        assertEquals(DevilNpcRules.KEEP, DevilNpcRules.check(true, true, DevilNpcRules.WAITING, 100, life));
        assertEquals(DevilNpcRules.VANISH_NOW, DevilNpcRules.check(false, true, DevilNpcRules.WAITING, 100, life), "the owner is gone");
        assertEquals(DevilNpcRules.VANISH_NOW, DevilNpcRules.check(true, true, DevilNpcRules.WAITING, life, life), "past its lifetime");
        assertEquals(DevilNpcRules.DISSOLVE_NOW, DevilNpcRules.check(true, false, DevilNpcRules.OFFER, 100, life), "nothing keeps it");
        assertEquals(DevilNpcRules.KEEP, DevilNpcRules.check(true, false, DevilNpcRules.MERGE, 100, life), "merging is left to finish");
        assertEquals(DevilNpcRules.KEEP, DevilNpcRules.check(true, false, DevilNpcRules.DISSOLVE, 100, life), "dissolving is left to finish");
    }

    @Test
    void theLinesAreSaidInsideTheRitualInOrder() {
        for (int i = 0; i < WorldDevil.count(); i++) {
            int ticks = WorldDevil.at(i).world().ticks;
            int a = DevilNpcRules.lineTick(0, ticks), b = DevilNpcRules.lineTick(1, ticks), c = DevilNpcRules.lineTick(2, ticks);
            assertTrue(a >= 2 && a < b && b < c && c < ticks, WorldDevil.at(i).id() + ": " + a + " " + b + " " + c + " of " + ticks);
        }
    }

    @Test
    void everyDevilHasFourLinesAndTheLangFileHoldsThemAll() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("tools/lang/devil_npc.json"))).getAsJsonObject();
        assertEquals(WorldDevil.count() * DevilNpcRules.LINES, lang.size(), "one key per line");
        for (int i = 0; i < WorldDevil.count(); i++) {
            String id = WorldDevil.at(i).id();
            String[] lines = DevilNpcLines.lines(id);
            assertEquals(DevilNpcRules.LINES, lines.length, id);
            for (int k = 0; k < lines.length; k++) {
                assertFalse(lines[k].isBlank(), id + " line " + k);
                assertTrue(lines[k].length() <= 140, id + " line " + k + " is too long for a chat line: " + lines[k].length());
                assertTrue(lang.has(DevilNpcLines.key(id, k)), "lang key " + DevilNpcLines.key(id, k));
                assertEquals(lines[k], lang.get(DevilNpcLines.key(id, k)).getAsString(), "lang text of " + id + " " + k);
            }
        }
        assertEquals("", DevilNpcLines.line("nobody", 0));
        assertEquals("", DevilNpcLines.line(WorldDevil.at(0).id(), 9));
    }

    @Test
    void theVisualPacketsDevilIndexIsInTheRosterOrAbsent() {
        assertEquals(-1, WorldDevil.indexOf(""));
        assertEquals(-1, WorldDevil.indexOf(null));
        assertEquals(-1, WorldDevil.indexOf("nobody"));
        for (int i = 0; i < WorldDevil.count(); i++) assertEquals(i, WorldDevil.indexOf(WorldDevil.at(i).id()));
    }
}
