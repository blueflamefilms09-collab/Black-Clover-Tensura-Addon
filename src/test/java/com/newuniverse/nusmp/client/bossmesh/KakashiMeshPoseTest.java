package com.newuniverse.nusmp.client.bossmesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Kakashi poses appended to {@link MeshPose}: they move the right roles, stay finite, and the older actions keep their numbers. */
class KakashiMeshPoseTest {
    @Test
    void theOlderActionsKeepTheirNumbersAndTheNewOnesAreAppended() {
        assertEquals(11, MeshPose.BOW);
        assertEquals(12, MeshPose.RAIKIRI);
        assertEquals(16, MeshPose.SUSANOO);
        assertEquals(8, MeshPose.ROLES);
    }

    @Test
    void everyNewPoseMovesTheBodyAndStaysFinite() {
        MeshPose p = new MeshPose();
        for (int a : new int[]{MeshPose.RAIKIRI, MeshPose.SEALS, MeshPose.THROW, MeshPose.KAMUI_EYE, MeshPose.SUSANOO}) {
            for (float t = 0f; t <= 1.0001f; t += 0.1f) {
                p.solve(40f, 0f, 0f, a, t);
                for (float r : p.rot) assertTrue(Float.isFinite(r), "action " + a + " t " + t);
                assertTrue(Float.isFinite(p.rootY) && Float.isFinite(p.rootPitch));
            }
        }
        MeshPose rest = new MeshPose(), pose = new MeshPose();
        rest.solve(40f, 0f, 0f, MeshPose.NONE, 0.5f);
        pose.solve(40f, 0f, 0f, MeshPose.RAIKIRI, 0.5f);
        assertNotEquals(rest.rot[MeshPose.ARM_R * 3], pose.rot[MeshPose.ARM_R * 3], "the Raikiri hand is drawn back");
        pose.solve(40f, 0f, 0f, MeshPose.SEALS, 0.5f);
        assertNotEquals(rest.rot[MeshPose.ARM_L * 3], pose.rot[MeshPose.ARM_L * 3], "the hands come together for the seals");
    }

    @Test
    void theNewMovesOfTheCanonKitAlsoPoseTheClassicBlenderBody() {
        assertEquals(MeshPose.RAIKIRI, BossMeshRouting.actionOf("lightning_cutter"));
        assertEquals(MeshPose.RAIKIRI, BossMeshRouting.actionOf("purple_strike"));
        assertEquals(MeshPose.KAMUI_EYE, BossMeshRouting.actionOf("kamui_warp"));
        assertEquals(MeshPose.SEALS, BossMeshRouting.actionOf("earth_fang"));
        assertEquals(MeshPose.SEALS, BossMeshRouting.actionOf("great_fireball"));
        assertEquals(MeshPose.THROW, BossMeshRouting.actionOf("thunder_god"));
        assertEquals(MeshPose.SUSANOO, BossMeshRouting.actionOf("six_paths"));
        assertEquals(MeshPose.SUSANOO, BossMeshRouting.actionOf("eight_gates"));
        assertEquals(MeshPose.RAIKIRI, BossMeshRouting.actionOf("raikiri"), "the older moves keep their pose");
    }

    /** The kit's player model is the default; the Blender body is the classic look behind {@code bossLooks.kakashiPlayerModel}. */
    @Test
    void thePlayerModelIsTheDefaultAndTheMeshIsOnlyTheClassicFallback() throws Exception {
        String options = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/newuniverse/nusmp/client/playermodel/PlayerModelOptions.java"));
        assertTrue(options.contains("if (mesh.equals(\"kakashi\")) return !kakashiPlayerModel();"), "the mesh is drawn only when the setting is off");
        assertTrue(options.contains(".define(\"kakashiPlayerModel\", true)"), "on by default");
        String routing = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/newuniverse/nusmp/client/bossmesh/BossMeshRouting.java"));
        assertTrue(routing.contains("new PlayerBodies.Kakashi()"), "the fallback of the mesh renderer is the kit's Kakashi");
        assertTrue(routing.contains("en -> \"kakashi\""), "the old Blender body stays registered (nothing removed)");
    }
}
