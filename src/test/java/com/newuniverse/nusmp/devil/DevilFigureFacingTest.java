package com.newuniverse.nusmp.devil;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the devil's figure and the Devil Union must face the way they are told to (docs/devil_double_magic.md, "model orientation"): the figure of the player-model look stood with its back to the caller. Pure. */
class DevilFigureFacingTest {
    private static final double EPS = 1e-6;

    @Test
    void theFigureLooksBackAtTheCallerForEveryDirectionAndEveryModel() {
        for (int deg = 0; deg < 360; deg += 15) {
            double fx = -Math.sin(Math.toRadians(deg)), fz = Math.cos(Math.toRadians(deg));         // the way the caller looks (the game's yaw convention)
            float yaw = DevilNpcRules.yawToward(-fx, -fz);                                          // the figure stands in front of the caller and looks back
            for (boolean playerModel : new boolean[] {false, true}) {
                double[] f = DevilNpcRules.drawnFront(yaw, playerModel);
                assertEquals(-fx, f[0], EPS, "x at " + deg + (playerModel ? " (player model)" : " (mesh)"));
                assertEquals(-fz, f[1], EPS, "z at " + deg + (playerModel ? " (player model)" : " (mesh)"));
            }
        }
    }

    @Test
    void withoutTheTurnThePlayerModelWouldFaceAway() {
        double[] f = DevilNpcRules.drawnFront(0f, true);                                            // yaw 0 faces +z
        assertEquals(1.0, f[1], EPS);
        assertEquals(180f, DevilNpcRules.PLAYER_MODEL_TURN);
        assertEquals(0f, DevilNpcRules.yawToward(0, 0));
        assertEquals(0f, DevilNpcRules.yawToward(0, 1), 1e-4f);
        assertEquals(-90f, DevilNpcRules.yawToward(1, 0), 1e-4f);
    }

    @Test
    void theFigureStandsOnGroundWithinReachOrWhereTheRitualBegan() {
        assertEquals(64.0, DevilNpcRules.standY(64.0, 64.0));
        assertEquals(64.5, DevilNpcRules.standY(64.5, 64.0));
        assertEquals(65.0, DevilNpcRules.standY(65.0, 64.0));
        assertEquals(61.0, DevilNpcRules.standY(61.0, 64.0));
        assertEquals(64.0, DevilNpcRules.standY(66.0, 64.0), "too high to step up");
        assertEquals(64.0, DevilNpcRules.standY(40.0, 64.0), "a drop is not ground in reach");
        assertEquals(64.0, DevilNpcRules.standY(Double.NaN, 64.0), "no ground found");
    }

    @Test
    void theRendererTurnsBothPlayerModelPathsAndTheMeshFrameStaysFlipped() throws IOException {
        Path root = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp/client/devil");
        String npc = Files.readString(root.resolve("DevilNpcRenderer.java"));
        int turns = npc.split("DevilNpcRules\\.PLAYER_MODEL_TURN", -1).length - 1;
        assertTrue(turns >= 2, "the player-model look and the generic figure both need the turn (found " + turns + ")");
        String layer = Files.readString(root.resolve("DevilFormLayer.java"));
        assertTrue(layer.contains("rotationX((float) Math.PI)"), "the layer's frame (y down, front -z) is turned to the mesh's (y up, front +z) by a half turn about x");
        assertFalse(layer.contains("PLAYER_MODEL_TURN"), "the player layer is drawn inside the living renderer, which already turns the model");
    }
}
