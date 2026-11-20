package com.newuniverse.nusmp.client.playermodel;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kakashi Hatake on the Black Clover player-model kit (docs/bosses/kakashi.md), checked with no game: the four looks and their files pass the audit, the state picks the right look, every
 * move maps to a pose and every pose stays finite, the switch is a CLIENT config that restores the classic Blender body, and the table is merged without disturbing the others.
 */
class KakashiPlayerModelsTest {
    private static final String ASSETS = "src/main/resources/assets/anime_multiverse/";
    private static final String PKG = "src/main/java/com/newuniverse/nusmp/client/playermodel/";

    private static InputStream open(String path) {
        try {
            File f = new File(ASSETS + path);
            return f.isFile() ? Files.newInputStream(f.toPath()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }

    @Test
    void theAuditOfTheFourLooksFindsNothingAndTheBlackCloverAuditStillPasses() {
        assertTrue(KakashiLooksAudit.run(KakashiPlayerModelsTest::open).isEmpty(), "problems: " + KakashiLooksAudit.run(KakashiPlayerModelsTest::open));
        assertTrue(BcAudit.run(KakashiPlayerModelsTest::open).isEmpty(), "BcAudit: " + BcAudit.run(KakashiPlayerModelsTest::open));
    }

    @Test
    void theFourLooksAreMergedAndNothingElseMoved() throws Exception {
        Map<String, PlayerLook> looks = PlayerLookTables.load(KakashiPlayerModelsTest::open).looks();
        for (String id : KakashiLooks.ids()) {
            assertNotNull(looks.get(id), id);
            assertTrue(BcLooks.isBc(id) && KakashiLooks.isKakashi(id));
            assertNotNull(BcLooks.theme(id), "shimmer theme of " + id);
        }
        assertEquals(4, KakashiLooks.ids().size());
        assertFalse(KakashiLooks.isKakashi("bc_lucius_zogratis"));
        assertNull(KakashiLooks.theme("bc_lucius_zogratis"));
        List<String> f = PlayerLookTables.FILES;
        assertEquals("playermodel/looks.json", f.get(0), "the old tables stay first: earlier files win a duplicate");
        assertTrue(f.indexOf("playermodel/looks_bc_kakashi.json") > f.indexOf("playermodel/looks_bc.json"));
        assertTrue(f.contains("playermodel/looks_bc_npc.json") && f.contains("playermodel/looks_dv.json"), "the other tables are all still listed");
    }

    @Test
    void theStateDecidesTheLook() {
        assertEquals(KakashiLooks.COVERED, KakashiLooks.pick(false, 0, false));
        assertEquals(KakashiLooks.COVERED, KakashiLooks.pick(false, 1, false));
        assertEquals(KakashiLooks.OPEN, KakashiLooks.pick(false, 0, true), "the band is pushed up while a move is in hand");
        assertEquals(KakashiLooks.MANGEKYO, KakashiLooks.pick(false, 2, false));
        assertEquals(KakashiLooks.MANGEKYO, KakashiLooks.pick(false, 3, true));
        assertEquals(KakashiLooks.SUSANOO, KakashiLooks.pick(true, 0, false), "the Susanoo model wins");
        assertEquals("boss.kakashi.kamui", KakashiLooks.theme(KakashiLooks.MANGEKYO));
    }

    @Test
    void everyMoveOfTheBookHasAPoseAndEveryPoseStaysFinite() {
        for (String m : com.newuniverse.nusmp.arc.kakashi.KakashiMath.BOOK) {
            int a = KakashiLooks.actionOf(m);
            assertTrue(a >= KakashiLooks.NONE && a <= KakashiLooks.SLASH, m);
        }
        assertEquals(KakashiLooks.RAIKIRI, KakashiLooks.actionOf("raikiri"));
        assertEquals(KakashiLooks.RAIKIRI, KakashiLooks.actionOf("lightning_cutter"));
        assertEquals(KakashiLooks.KAMUI_EYE, KakashiLooks.actionOf("kamui_warp"));
        assertEquals(KakashiLooks.SEALS, KakashiLooks.actionOf("lightning_clone"));
        assertEquals(KakashiLooks.SUSANOO_POSE, KakashiLooks.actionOf("six_paths"));
        assertEquals(KakashiLooks.SLASH, KakashiLooks.actionOf("kubikiribocho"));
        assertEquals(KakashiLooks.THROW, KakashiLooks.actionOf("kunai_wire"));
        assertEquals(KakashiLooks.NONE, KakashiLooks.actionOf(null));
        PoseState p = new PoseState();
        for (int kind = 0; kind < 6; kind++) {
            for (float t = 0f; t <= 1.0001f; t += 0.05f) {
                p.clear();
                switch (kind) {
                    case 0 -> PosePatterns.raikiri(p, t, 37f);
                    case 1 -> PosePatterns.seals(p, t, 37f);
                    case 2 -> PosePatterns.throwing(p, t, 37f);
                    case 3 -> PosePatterns.kamuiEye(p, t, 37f);
                    case 4 -> PosePatterns.spread(p, t, 37f);
                    default -> PosePatterns.slash(p, t, 37f);
                }
                for (int part = 0; part < PoseState.PARTS; part++) {
                    assertTrue(Float.isFinite(p.blend[part]) && p.blend[part] >= 0f && p.blend[part] <= 1.0001f, "blend, pose " + kind);
                    for (int k = 0; k < 3; k++) assertTrue(Float.isFinite(p.target[part][k]) && Float.isFinite(p.add[part][k]), "pose " + kind + " t " + t);
                }
            }
        }
        p.clear();
        PosePatterns.raikiri(p, 1f, 10f);
        assertTrue(p.blend[PoseState.ARM_R] > 0.9f, "the Raikiri hand is drawn back");
        p.clear();
        PosePatterns.kamuiEye(p, 1f, 10f);
        assertTrue(p.blend[PoseState.ARM_L] > 0.9f, "the left hand goes to the eye");
    }

    @Test
    void theSwitchIsAClientConfigAndTheClassicBodyIsOneSwitchAway() throws Exception {
        String o = read(PKG + "PlayerModelOptions.java");
        assertTrue(o.contains(".define(\"kakashiPlayerModel\", true)"), "default on: the kit's Kakashi is the default");
        assertTrue(o.contains("if (mesh.equals(\"kakashi\")) return !kakashiPlayerModel();"), "off restores the Blender body");
        String r = read("src/main/java/com/newuniverse/nusmp/client/bossmesh/BossMeshRouting.java");
        assertTrue(r.contains("new PlayerBodies.Kakashi()"), "the kit's body is the one the routing builds");
        assertTrue(r.contains("en -> \"kakashi\""), "the Blender body is kept as the classic look");
        String b = read(PKG + "PlayerBodies.java");
        assertTrue(b.contains("class Kakashi implements PlayerBody<ArcBoss>"));
        assertTrue(read(PKG + "KakashiLooks.java").indexOf("net.minecraft") < 0, "the table is pure");
    }

    @Test
    void theLangHasTheSwitchAndItsTooltip() throws Exception {
        String lang = read(ASSETS + "lang/en_us.json");
        assertTrue(lang.contains("\"anime_multiverse.configuration.kakashiPlayerModel\""));
        assertTrue(lang.contains("\"anime_multiverse.configuration.kakashiPlayerModel.tooltip\""));
    }
}
