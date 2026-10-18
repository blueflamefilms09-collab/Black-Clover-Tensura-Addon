package com.newuniverse.nusmp.client.heroweapon.carriage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageNumbers;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Why: the carriage you ride and the small carriage in the hand are one mesh file made by the art builder to the mesh-space contract of the carriage page
 * (+y the nose, -x the roof, z the width, the wheels' ground contact at the highest x), and the entity's seats, hitbox and renderer assume it. If a later change
 * of that file broke the contract the carriage would drive backwards, upside down or with its riders in the air and nobody would see it before playing. This test
 * reads the shipped mesh (when it is in the tree: the art arrives with the lead's merge, and from then on this test guards it) and checks, in the entity's frame:
 * the length is the model length, the wheels (the widest part) stand on the ground and are low, the width fits the hitbox, the roof is above the seats, the nose end (the draught pole) is
 * narrower than the tail end (the hull), and under every seat point there is a bench surface at the seat's height.
 */
class CarriageMeshContractTest {
    private static final Path MESH = Path.of("src/main/resources/assets/nusmp/weapon_meshes/vassal_carriage.txt");

    @Test
    void theShippedMeshFollowsTheMeshSpaceContractOfTheCarriagePage() throws Exception {
        if (!Files.exists(MESH)) return;                                    // the art is not in this tree yet
        MeshBake.Baked b = MeshBake.bake(Files.readAllLines(MESH), CarriageNumbers.MODEL_LENGTH);
        assertNotNull(b, "the shipped carriage mesh has no usable body");
        double minY = 1e9, maxY = -1e9, minZ = 1e9, maxZ = -1e9, width = 0.0, frontWidth = 0.0, rearWidth = 0.0;
        for (float[] v : b.body) {
            minY = Math.min(minY, v[1]);
            maxY = Math.max(maxY, v[1]);
            minZ = Math.min(minZ, v[2]);
            maxZ = Math.max(maxZ, v[2]);
            width = Math.max(width, Math.abs(v[0]));
            if (v[2] > 1.3) frontWidth = Math.max(frontWidth, Math.abs(v[0]));
            if (v[2] < -1.3) rearWidth = Math.max(rearWidth, Math.abs(v[0]));
        }
        assertEquals(CarriageNumbers.MODEL_LENGTH, maxZ - minZ, 0.01, "the model is drawn at its length");
        assertTrue(minY >= -0.01 && minY <= 0.05, "the wheels rest on the ground: lowest point " + minY);
        assertTrue(maxY >= 1.5 && maxY <= 2.8, "the roof is a believable height above the ground: " + maxY);
        assertTrue(width >= 0.6 && width <= CarriageNumbers.WIDTH / 2.0, "the width over the wheels fits the hitbox: half width " + width);
        // the outermost parts are the two wheels: they touch the ground and stay low (a roof and floor swapped would put them at the top)
        double wheelBottom = 1e9, wheelTop = -1e9;
        int wheelVertices = 0;
        for (float[] v : b.body) {
            if (Math.abs(v[0]) <= 0.94 * width) continue;
            wheelVertices++;
            wheelBottom = Math.min(wheelBottom, v[1]);
            wheelTop = Math.max(wheelTop, v[1]);
        }
        assertTrue(wheelVertices >= 100, "the wheels are the widest part of the carriage: " + wheelVertices + " vertices");
        assertTrue(wheelBottom <= 0.05, "the wheels touch the ground: their lowest point is " + wheelBottom);
        assertTrue(wheelTop <= 1.3, "the wheels are low: their highest point is " + wheelTop);
        assertTrue(frontWidth < rearWidth - 0.15, "the nose end (pole and yoke) is narrower than the tail end (the hull): " + frontWidth + " against " + rearWidth);
        for (double[] seat : CarriageNumbers.SEATS) {
            boolean bench = false, roof = false;
            for (float[] v : b.body) {
                if (Math.hypot(v[0] - seat[0], v[2] - seat[2]) > 0.4) continue;
                if (Math.abs(v[1] - seat[1]) <= 0.12) bench = true;
                if (v[1] >= 1.3) roof = true;
            }
            assertTrue(bench, "no bench surface under the seat point " + seat[0] + ", " + seat[1] + ", " + seat[2]);
            assertTrue(roof, "no roof above the seat point " + seat[0] + ", " + seat[2]);
        }
    }
}
