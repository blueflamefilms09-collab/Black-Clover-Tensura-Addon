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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Saiyan hair and the 3D tail (docs/saiyan_hair.md) are Blender meshes the painter wears through {@code BossMeshDraw}; this reads the shipped files with the game's own parser (the style of DevilFormAssetsTest):
 * every shape and the tail have their mesh, far copy, skin and glow map, the quads fit the caps, the parts are roles the drawer routes, the far copy is not heavier, the skins are square powers of two, and the
 * previews and the doc are in the repo.
 */
class SaiyanHairAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");

    private static EntityMeshData mesh(String path) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(ROOT.resolve(path))) {
            return EntityMeshData.parse(in);
        }
    }

    private static void texture(String path) throws IOException {
        Path p = ROOT.resolve(path);
        assertTrue(Files.exists(p), "missing " + path);
        BufferedImage img = ImageIO.read(p.toFile());
        assertNotNull(img, path + " is not an image");
        assertEquals(img.getWidth(), img.getHeight(), path + " must be square");
        assertTrue(Integer.bitCount(img.getWidth()) == 1 && img.getWidth() >= 32 && img.getWidth() <= 2048, path + " must be a power of two from 32 to 2048: " + img.getWidth());
    }

    private static void files(String id) throws IOException {
        for (String f : new String[]{SaiyanHair.meshPath(id), SaiyanHair.lodPath(id), SaiyanHair.skinPath(id), SaiyanHair.glowPath(id)}) assertTrue(Files.exists(ROOT.resolve(f)), "missing " + f);
        texture(SaiyanHair.skinPath(id));
        texture(SaiyanHair.glowPath(id));
    }

    private static void roles(String id, EntityMeshData m, List<String> allowed) {
        for (int i = 0; i < m.parts.length; i++) {
            assertTrue(allowed.contains(m.parts[i]), id + ": part " + m.parts[i] + " is not one of " + allowed);
            assertTrue(m.parent[i] < i, id + ": the pivot of " + m.parts[i] + " must come after its parent's");
            boolean role = false;
            for (String r : MeshPose.ALL_ROLE_NAMES) role |= r.equals(m.parts[i]);
            assertTrue(role, id + ": " + m.parts[i] + " must be a role name the mesh drawer routes");
            assertTrue(m.body[i].length > 0, id + ": part " + m.parts[i] + " has no quads");
        }
    }

    @Test
    void everyHairShapeHasItsFilesInsideTheQuadCapAndALighterFarCopy() throws IOException {
        for (String shape : SaiyanHair.SHAPES) {
            String id = SaiyanHair.meshId(shape);
            files(id);
            EntityMeshData full = mesh(SaiyanHair.meshPath(id)), lod = mesh(SaiyanHair.lodPath(id));
            assertTrue(full.quads() <= SaiyanHair.MAX_QUADS, id + " has " + full.quads() + " quad lines, the cap is " + SaiyanHair.MAX_QUADS);
            assertTrue(lod.quads() <= full.quads() && lod.quads() <= SaiyanHair.MAX_QUADS / 2, id + ": the far copy must be lighter (" + lod.quads() + " vs " + full.quads() + ")");
            assertEquals(0, lod.quads(EntityMeshData.GLOW), id + ": the far copy has no glow pass");
            roles(id, full, List.of("head", "tail"));
            roles(id, lod, List.of("head", "tail"));
            assertTrue(full.part("head") >= 0 && lod.part("head") >= 0, id + " has no head part");
            assertEquals(full.part("tail") >= 0, lod.part("tail") >= 0, id + ": the far copy has the same parts");
            float[] h = full.heightBlocks();
            assertTrue(h[0] > 0.1f && h[1] > 2.0f && h[1] < 3.6f, id + ": worn on the head: from " + h[0] + " to " + h[1] + " blocks");
        }
    }

    @Test
    void theLongHairHasItsHangingLocksAndTheShortHairDoesNot() throws IOException {
        for (String shape : new String[]{"ss3", "ss4"}) assertTrue(mesh(SaiyanHair.meshPath(SaiyanHair.meshId(shape))).part("tail") >= 0, shape + " has hanging locks");
        for (String shape : new String[]{"base", "ss", "ss2", "god", "ui"}) assertTrue(mesh(SaiyanHair.meshPath(SaiyanHair.meshId(shape))).part("tail") < 0, shape + " has none");
        assertTrue(mesh(SaiyanHair.meshPath(SaiyanHair.meshId("ss3"))).heightBlocks()[0] < 0.4f, "Super Saiyan 3 hangs to the knees (low end near the hips)");
    }

    @Test
    void theTailIsThreeChainedPartsWornOnTheLowerBack() throws IOException {
        String id = SaiyanHair.TAIL;
        files(id);
        EntityMeshData full = mesh(SaiyanHair.meshPath(id)), lod = mesh(SaiyanHair.lodPath(id));
        assertTrue(full.quads() <= SaiyanHair.MAX_TAIL_QUADS && lod.quads() <= full.quads(), id + " " + full.quads() + " / " + lod.quads());
        assertEquals(0, full.quads(EntityMeshData.GLOW), "fur does not glow");
        roles(id, full, List.of("tail", "wing_l", "wing_r"));
        assertEquals(3, full.parts.length);
        assertEquals("tail", full.parts[0]);
        assertEquals(-1, full.parent[0]);
        assertEquals(0, full.parent[1]);
        assertEquals(1, full.parent[2]);
        float y = full.pivot[1], z = full.pivot[2];
        assertTrue(y > 11f && y < 24f && z < 0f && z > -3f, "the root is on the back of the body: y " + y + " z " + z);
        float[] h = full.heightBlocks();
        assertTrue(h[0] > 0.2f && h[1] < 1.6f, "the tail hangs from the hips and stays below the shoulders: " + h[0] + ".." + h[1]);
        float zmin = Float.MAX_VALUE;
        for (float[] p : full.body) for (int i = 2; i < p.length; i += EntityMeshData.STRIDE) zmin = Math.min(zmin, p[i]);
        assertTrue(zmin < -14f && zmin > -30f, "the tail reaches out behind the player (about one and a half blocks): " + zmin);
    }

    @Test
    void thePreviewsAndTheDocAreInTheRepo() throws IOException {
        for (String shape : SaiyanHair.SHAPES) assertTrue(Files.size(Path.of("docs/saiyan_hair/previews/hair_" + shape + ".png")) > 10_000, "preview of " + shape);
        assertTrue(Files.size(Path.of("docs/saiyan_hair/previews/saiyan_tail.png")) > 10_000);
        assertTrue(Files.size(Path.of("docs/saiyan_hair/previews/all_forms.png")) > 10_000);
        String doc = Files.readString(Path.of("docs/saiyan_hair.md"));
        for (String w : new String[]{"NEW", "REPLACED", "KEPT", "saiyan.hair", "saiyan.tail3d", "Wikipedia", "quads"}) assertTrue(doc.contains(w), "docs/saiyan_hair.md says " + w);
        for (SaiyanForms.Form f : SaiyanForms.ALL) assertTrue(doc.contains(f.id()), "docs/saiyan_hair.md lists " + f.id());
    }
}
