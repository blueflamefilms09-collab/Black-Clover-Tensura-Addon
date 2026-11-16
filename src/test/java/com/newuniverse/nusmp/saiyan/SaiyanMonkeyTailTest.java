package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.client.bossmesh.MeshPose;
import com.newuniverse.nusmp.mesh.EntityMeshData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Saiyan monkey tail (docs/body_parts_pass.md): a furry brown tail, hanging or wrapped round the waist, instead of the old red lizard-like tube. Pure: reads the shipped mesh files with the game's
 * own parser and checks the caps (rule 11), the three-part chain the rig swings, where the tail sits on the body, that the skin is brown fur, and the small rules of {@link SaiyanHair}.
 */
class SaiyanMonkeyTailTest {
    private static final Path ROOT = Path.of("src/main/resources");

    private static EntityMeshData mesh(String path) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(ROOT.resolve(path))) {
            return EntityMeshData.parse(in);
        }
    }

    private static float[] box(EntityMeshData m) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] p : m.body) for (int i = 0; i + 2 < p.length; i += EntityMeshData.STRIDE) for (int k = 0; k < 3; k++) { b[k] = Math.min(b[k], p[i + k]); b[3 + k] = Math.max(b[3 + k], p[i + k]); }
        return b;
    }

    @Test
    void bothMonkeyTailsAreThreeChainedPartsInsideTheQuadCapWithALighterFarCopy() throws IOException {
        for (String id : new String[]{SaiyanHair.TAIL_MONKEY, SaiyanHair.TAIL_WRAP}) {
            EntityMeshData full = mesh(SaiyanHair.meshPath(id)), lod = mesh(SaiyanHair.lodPath(id));
            assertTrue(full.quads() <= SaiyanHair.MAX_TAIL_QUADS && full.quads() > 100, id + " " + full.quads());
            assertTrue(lod.quads() < full.quads() && lod.quads() <= SaiyanHair.MAX_TAIL_QUADS / 2, id + " far copy " + lod.quads());
            assertEquals(0, full.quads(EntityMeshData.GLOW), "fur does not glow");
            for (EntityMeshData m : new EntityMeshData[]{full, lod}) {
                assertEquals(3, m.parts.length);
                assertEquals("tail", m.parts[0]);
                assertEquals("wing_l", m.parts[1]);
                assertEquals("wing_r", m.parts[2]);
                assertEquals(-1, m.parent[0]);
                assertEquals(0, m.parent[1]);
                assertEquals(1, m.parent[2]);
                for (int i = 0; i < 3; i++) {
                    boolean role = false;
                    for (String r : MeshPose.ALL_ROLE_NAMES) role |= r.equals(m.parts[i]);
                    assertTrue(role, id + ": " + m.parts[i] + " is a role the mesh drawer routes");
                    assertTrue(m.body[i].length > 0, id + ": " + m.parts[i] + " has quads");
                }
                float y = m.pivot[1], z = m.pivot[2];
                assertTrue(y > 11f && y < 16f && z < 0f && z > -3f, id + ": the root is on the lower back: y " + y + " z " + z);
            }
        }
    }

    @Test
    void theHangingTailReachesBackWithAGentleCurveAndStaysAboveTheGround() throws IOException {
        EntityMeshData m = mesh(SaiyanHair.meshPath(SaiyanHair.TAIL_MONKEY));
        float[] b = box(m);
        assertTrue(b[2] < -20f && b[2] > -32f, "reaches out behind (about 1.5 blocks): zmin " + b[2]);
        assertTrue(b[1] > 3f && b[4] < 20f, "from just above the ground to below the shoulders: y " + b[1] + ".." + b[4]);
        assertTrue(b[3] - b[0] < 8f, "it hangs straight behind, not sprawling sideways: width " + (b[3] - b[0]));
    }

    @Test
    void theWrappedTailGoesAllAroundTheWaistAndNowhereElse() throws IOException {
        EntityMeshData m = mesh(SaiyanHair.meshPath(SaiyanHair.TAIL_WRAP));
        float[] b = box(m);
        assertTrue(b[0] < -4.5f && b[3] > 4.5f, "wider than the body (8 px) on both sides: x " + b[0] + ".." + b[3]);
        assertTrue(b[5] > 2.5f && b[2] < -2.5f, "in front of and behind the body (4 px deep): z " + b[2] + ".." + b[5]);
        assertTrue(b[1] > 9f && b[4] < 20f, "at the waist: y " + b[1] + ".." + b[4]);
        assertTrue(SaiyanHair.WRAP_LOOSE > 0f && SaiyanHair.WRAP_LOOSE < 0.5f, "tied round the waist: it hardly sways");
    }

    @Test
    void theSkinIsSquareBrownFurWithAnEmptyGlowMap() throws IOException {
        for (String id : new String[]{SaiyanHair.TAIL_MONKEY, SaiyanHair.TAIL_WRAP}) {
            BufferedImage skin = ImageIO.read(ROOT.resolve(SaiyanHair.skinPath(id)).toFile()), glow = ImageIO.read(ROOT.resolve(SaiyanHair.glowPath(id)).toFile());
            assertNotNull(skin);
            assertNotNull(glow);
            assertEquals(128, skin.getWidth());
            assertEquals(128, skin.getHeight());
            assertEquals(128, glow.getWidth());
            long r = 0, g = 0, bl = 0, n = 0;
            for (int y = 0; y < 128; y++) for (int x = 0; x < 56; x++) {                                    // the tube part of the sheet
                int argb = skin.getRGB(x, y);
                if ((argb >>> 24) < 200) continue;
                r += (argb >> 16) & 255; g += (argb >> 8) & 255; bl += argb & 255; n++;
            }
            assertTrue(n > 3000, "the tube columns are painted");
            assertTrue(r / n > 80 && r / n < 190 && r > g && g > bl, id + ": monkey brown, mean " + r / n + "," + g / n + "," + bl / n);
            for (int y = 0; y < 128; y += 7) for (int x = 0; x < 128; x += 7) assertEquals(0, glow.getRGB(x, y) >>> 24, "fur does not glow");
        }
    }

    @Test
    void theSettingsPickTheRightMeshAndTheTintOnlyWarmsTheBrown() {
        assertEquals(SaiyanHair.TAIL, SaiyanHair.tailId(false, false));
        assertEquals(SaiyanHair.TAIL, SaiyanHair.tailId(false, true), "wrapping needs the monkey tail");
        assertEquals(SaiyanHair.TAIL_MONKEY, SaiyanHair.tailId(true, false));
        assertEquals(SaiyanHair.TAIL_WRAP, SaiyanHair.tailId(true, true));
        assertEquals(0xFFFFFF, SaiyanHair.monkeyTint(0), "no fur colour: the plain brown");
        int t = SaiyanHair.monkeyTint(0xC02020);                                                           // the Super Saiyan 4 fur colour
        int r = (t >> 16) & 255, g = (t >> 8) & 255, b = t & 255;
        assertTrue(r > 230 && g > 160 && g < 200 && b > 160 && b < 200 && r > g, "a red cast, not red: " + Integer.toHexString(t));
        assertTrue(SaiyanHair.monkeyTint(0x102030) > 0x100000, "a dark colour never darkens the skin below 35 percent of the way to black");
    }
}
