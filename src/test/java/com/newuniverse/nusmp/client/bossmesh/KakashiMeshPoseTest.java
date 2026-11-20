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
}
