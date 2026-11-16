package com.newuniverse.nusmp.boss.rework;

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
 * Why (docs/bosses/lucifero_v2.md): Lucifero's three new Blender bodies (the king, wings spread, the Gates) are shipped files, so they are read with the game's own parser: every part the pose solver moves is
 * there, the separate armour parts hang from the part they follow (so they move with it), the quads fit the 2800 budget, the far copy is lighter and has no glow pass, he stands on the floor, the skins are
 * square powers of two and the previews are small. The old bodies must still be there (the {@code lucifero.newBody} switch falls back to them).
 */
class LuciferoV2Test {
    private static final String ASSETS = "src/main/resources/assets/anime_multiverse/";
    private static final String[] BODIES = {"lucifero_v2", "lucifero_v2_form2", "lucifero_v2_gates"};
    private static final String[][] ARMOUR = {{"chest_plate", "torso"}, {"tasset", "torso"}, {"pauldron_l", "arm_l"}, {"pauldron_r", "arm_r"}, {"bracer_l", "forearm_l"},
            {"bracer_r", "forearm_r"}, {"greave_l", "leg_l"}, {"greave_r", "leg_r"}, {"crown", "head"}};

    private static EntityMeshData mesh(String file) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(Path.of(ASSETS + "entity_meshes/" + file))) {
            return EntityMeshData.parse(in);
        }
    }

    @Test
    void everyBodyHasTheRigTheArmourPartsAndFitsTheBudget() throws IOException {
        for (String id : BODIES) {
            EntityMeshData m = mesh(id + ".txt");
            assertEquals(id, m.id);
            for (String role : MeshPose.ROLE_NAMES) assertTrue(m.part(role) >= 0 && m.body[m.part(role)].length > 0, id + " part " + role);
            assertEquals(-1, m.parent[m.part("torso")]);
            assertEquals(m.part("torso"), m.parent[m.part("wing_l")], id + ": the wings hang from the back");
            assertEquals(m.part("torso"), m.parent[m.part("wing_r")]);
            assertTrue(m.body[m.part("wing_l")].length > 0 && m.body[m.part("wing_r")].length > 0, id + ": both wings have quads");
            for (String[] a : ARMOUR) {
                assertTrue(m.part(a[0]) >= 0 && m.body[m.part(a[0])].length > 0, id + ": armour part " + a[0]);
                assertEquals(m.part(a[1]), m.parent[m.part(a[0])], id + ": " + a[0] + " follows " + a[1]);
            }
            assertTrue(m.quads() <= 2800, id + " within the build budget: " + m.quads());
            assertTrue(m.quads(EntityMeshData.GLOW) > 0, id + " glows (eyes, third eye, rune gem)");
            float[] h = m.heightBlocks();
            assertTrue(h[0] > -0.2f && h[0] < 0.2f, id + " stands on the floor: " + h[0]);
            assertTrue(h[1] > 2.4f && h[1] < 4.6f, id + " is a tall devil, not a giant: " + h[1]);
            EntityMeshData lod = mesh(id + "_lod.txt");
            assertTrue(lod.quads() < m.quads() * 0.6, id + " far copy is lighter: " + lod.quads());
            assertEquals(0, lod.quads(EntityMeshData.GLOW), id + " far copy has no glow pass");
            for (String role : MeshPose.ROLE_NAMES) assertTrue(lod.part(role) >= 0, id + " LOD part " + role);
        }
    }

    @Test
    void theGatesBodyAddsTheHalo() throws IOException {
        EntityMeshData g = mesh("lucifero_v2_gates.txt");
        assertTrue(g.part("halo") >= 0 && g.body[g.part("halo")].length > 0, "the halo of horns");
        assertEquals(g.part("head"), g.parent[g.part("halo")]);
        assertTrue(g.quads() >= mesh("lucifero_v2.txt").quads() - 200, "the Gates body is at least as rich as the king's");
    }

    @Test
    void theSkinsAndThePreviews() throws IOException {
        for (String id : BODIES) {
            for (String s : new String[]{id + ".png", id + "_glow.png"}) {
                BufferedImage img = ImageIO.read(Path.of(ASSETS + "textures/entity/boss_mesh/" + s).toFile());
                assertNotNull(img, s);
                assertEquals(img.getWidth(), img.getHeight(), s + " is square");
                assertTrue(img.getWidth() <= 1024 && Integer.bitCount(img.getWidth()) == 1, s + " is a power of two up to 1024 px");
            }
            for (String p : new String[]{id + "_blender.png", id + "_blender_back.png"}) {
                Path f = Path.of("docs/bosses/previews/" + p);
                assertTrue(Files.exists(f), p);
                assertTrue(Files.size(f) < 300 * 1024, p + " under 300 KB");
            }
        }
    }

    @Test
    void theOldBodiesAreKept() {
        for (String old : new String[]{"lucifero", "lucifero_form2", "lucifero_lod", "lucifero_form2_lod"}) {
            assertTrue(Files.exists(Path.of(ASSETS + "entity_meshes/" + old + ".txt")), old + " stays (the switch falls back to it)");
        }
    }
}
