package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.client.bossmesh.MeshPose;
import com.newuniverse.nusmp.mesh.EntityMeshData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Devil forms (docs/devil_forms.md): every devil of {@link WorldDevil} has its own Blender mesh, a low-detail copy, a skin and a glow map, and each mesh is one the form layer can wear: the quads fit the
 * budget, the part names are the eleven roles of {@link MeshPose} (the basic eight are all there), a parent always comes before its children, the low-detail copy has the same roles and is not heavier,
 * the textures are square powers of two. Reads the shipped files with the game's own parser. This test fails until the four asset sets (the meshes and skins made in Blender) are merged; until then the
 * game draws the old generic horns and wings for a devil without them (the layer and the figure both fall back).
 */
class DevilFormAssetsTest {
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

    private static void checkMesh(String id, EntityMeshData m, boolean lod) {
        String what = id + (lod ? " (lod)" : "");
        assertTrue(m.parts.length > 0, what + " has no parts");
        assertTrue(m.quads() <= DevilFormRules.MAX_QUADS, what + " has " + m.quads() + " quads, the cap is " + DevilFormRules.MAX_QUADS);
        Set<String> names = new HashSet<>();
        for (int i = 0; i < m.parts.length; i++) {
            assertTrue(names.add(m.parts[i]), what + ": part twice: " + m.parts[i]);
            assertTrue(m.parent[i] < i, what + ": the pivot of " + m.parts[i] + " must come after its parent's");
        }
        for (String role : MeshPose.ROLE_NAMES) {
            assertTrue(m.part(role) >= 0, what + " has no part named " + role);
            assertTrue(m.body[m.part(role)].length > 0, what + ": part " + role + " has no quads");
        }
        for (String name : names) {
            boolean known = false;
            for (String role : DevilFormRules.ROLES) known |= role.equals(name);
            if (name.startsWith("wing") || name.startsWith("tail") || name.startsWith("arm") || name.startsWith("leg") || name.startsWith("forearm") || name.startsWith("head") || name.startsWith("torso"))
                assertTrue(known, what + ": '" + name + "' looks like a role but is not one of " + String.join(", ", DevilFormRules.ROLES));
        }
    }

    @Test
    void everyDevilHasItsMeshItsLowDetailCopyItsSkinAndItsGlowMap() throws IOException {
        for (int i = 0; i < WorldDevil.count(); i++) {
            String id = WorldDevil.at(i).id();
            assertTrue(Files.exists(ROOT.resolve(DevilFormRules.meshPath(id))), "missing mesh of " + id + ": " + DevilFormRules.meshPath(id));
            assertTrue(Files.exists(ROOT.resolve(DevilFormRules.lodPath(id))), "missing low-detail mesh of " + id);
            EntityMeshData full = mesh(DevilFormRules.meshPath(id)), lod = mesh(DevilFormRules.lodPath(id));
            checkMesh(id, full, false);
            checkMesh(id, lod, true);
            assertTrue(lod.quads() <= full.quads(), id + ": the far copy must not be heavier (" + lod.quads() + " > " + full.quads() + ")");
            for (String role : DevilFormRules.ROLES) assertEquals(full.part(role) >= 0, lod.part(role) >= 0, id + ": the far copy has the same roles (" + role + ")");
            texture(DevilFormRules.skinPath(id));
            texture(DevilFormRules.glowPath(id));
        }
    }

    @Test
    void theFeetAreOnTheFloorAndTheHeightIsAPlayersOrMore() throws IOException {
        for (int i = 0; i < WorldDevil.count(); i++) {
            String id = WorldDevil.at(i).id();
            if (!Files.exists(ROOT.resolve(DevilFormRules.meshPath(id)))) continue;              // the first test names the missing file
            float[] h = mesh(DevilFormRules.meshPath(id)).heightBlocks();
            assertTrue(h[0] > -0.3f && h[0] < 0.3f, id + ": feet on the floor (the form is worn on a player): " + h[0]);
            assertTrue(h[1] > 1.4f && h[1] < 6.5f, id + ": about a player's height or a little more (" + h[1] + ")");
        }
    }
}
