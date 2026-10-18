package com.newuniverse.nusmp.client.relics.worn.lib;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: worn relics were drawn under the skin's second layer (a jacket, sleeve or hat layer hid them) because the on-body fit shrinks them toward the body. clearOf moves only the
 * vertices that would land inside that layer out through its nearest face; this pins that behaviour with a one-quad model on each kind of part.
 */
class WornMeshClearanceTest {
    private static WornMesh quad(String anchor, float x, float y, float z) {
        // one quad of four corners around (x, y, z), no groups beyond the static base
        String q = "q 0 0 0 %s %s %s 0 0 0 0 -1 %s %s %s 1 0 0 0 -1 %s %s %s 1 1 0 0 -1 %s %s %s 0 1 0 0 -1";
        String line = String.format(java.util.Locale.ROOT, q, x, y, z, x + 1, y, z, x + 1, y + 1, z, x, y + 1, z);
        return WornMesh.parse("t", List.of("model t", "anchor " + anchor, line));
    }

    private static float z0(WornMesh m) { return m.pos[2]; }

    @Test
    void aVertexInsideTheJacketLayerIsMovedOutAndOneOutsideIsLeftAlone() {
        WornMesh sunk = quad("body", 0f, 4f, -2.1f);               // 0.15 inside the torso's outer layer at the front (z = -2.25)
        WornMesh moved = sunk.clearOf(WornAnchor.BODY, false, 1f, 0f, 0f, 0f);
        assertTrue(z0(moved) < -2.25f, "pushed out through the front face, was " + z0(moved));
        WornMesh clear = quad("body", 0f, 4f, -3.0f);
        assertSame(clear, clear.clearOf(WornAnchor.BODY, false, 1f, 0f, 0f, 0f));
    }

    @Test
    void theShrinkOfTheFitIsAccountedFor() {
        WornMesh m = quad("body", 0f, 4f, -2.6f);                   // outside as authored, but 0.85 of it (-2.21) would sink
        WornMesh moved = m.clearOf(WornAnchor.BODY, false, 0.85f, 0f, 0f, 0f);
        assertTrue(0.85f * z0(moved) < -2.25f, "after the fit the vertex is outside the layer: " + 0.85f * z0(moved));
    }

    @Test
    void aMirroredLeftArmPieceIsCheckedInItsOwnArm() {
        WornMesh m = quad("rarm", -3.0f, 4f, 0f);                   // inside the right sleeve layer at its outer side (x = -3.25)
        WornMesh moved = m.clearOf(WornAnchor.LEFT_ARM, true, 1f, 0f, 0f, 0f);
        float drawnX = -moved.pos[0];                               // mirrored
        assertTrue(drawnX > 3.25f || drawnX < -1.25f, "outside the left arm's layer, was " + drawnX);
        assertEquals(m.quads, moved.quads);
    }
}
