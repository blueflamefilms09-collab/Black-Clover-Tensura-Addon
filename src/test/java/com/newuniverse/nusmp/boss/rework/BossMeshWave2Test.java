package com.newuniverse.nusmp.boss.rework;

import com.newuniverse.nusmp.client.bossmesh.MeshPose;
import com.newuniverse.nusmp.mesh.EntityMeshData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boss rework wave 2 (docs/bosses/rework_wave2_models.md): the Blender-made Goliath body. Reads the shipped mesh with the game's own parser: every part the procedural
 * animation moves is there with its pivot, the quads fit the per-entity budget, the low-detail copy is lighter, the skins are square powers of two, the previews are
 * small; the pose solver starts and ends every blow at rest; the setting and the old model path are kept; the new common code has no client class.
 */
class BossMeshWave2Test {
    private static final String SRC = "src/main/java/com/newuniverse/nusmp/";
    private static final String ASSETS = "src/main/resources/assets/anime_multiverse/";

    private static String read(String path) throws IOException { return Files.readString(Path.of(path)); }

    private static EntityMeshData mesh(String file) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(Path.of(ASSETS + "entity_meshes/" + file))) {
            return EntityMeshData.parse(in);
        }
    }

    @Test
    void theGoliathMeshHasEveryPartWithinTheBudget() throws IOException {
        EntityMeshData m = mesh("goliath.txt");
        assertEquals("goliath", m.id);
        for (String role : MeshPose.ROLE_NAMES) assertTrue(m.part(role) >= 0, "part " + role);
        assertEquals(-1, m.parent[m.part("torso")]);
        assertEquals(m.part("arm_l"), m.parent[m.part("forearm_l")], "the forearm hangs from the arm");
        assertEquals(m.part("torso"), m.parent[m.part("head")]);
        for (String role : MeshPose.ROLE_NAMES) assertTrue(m.body[m.part(role)].length > 0, "the part " + role + " has quads");
        assertTrue(m.quads() <= 2800, "body + glow quads within the build budget: " + m.quads());
        assertTrue(m.quads(EntityMeshData.GLOW) > 0, "the eyes and veins glow");
        float[] h = m.heightBlocks();
        assertTrue(h[0] > -0.2f && h[0] < 0.2f, "feet on the floor: " + h[0]);
        assertTrue(h[1] > 6.0f && h[1] < 7.4f, "a 7 m giant: " + h[1]);
        EntityMeshData lod = mesh("goliath_lod.txt");
        assertTrue(lod.quads() < m.quads() / 2, "the far copy is less than half: " + lod.quads());
        assertEquals(0, lod.quads(EntityMeshData.GLOW), "the far copy has no glow pass");
        for (String role : MeshPose.ROLE_NAMES) assertTrue(lod.part(role) >= 0, "LOD part " + role);
    }

    @Test
    void theMinotaurMeshHasEveryPartWithinTheBudget() throws IOException {
        EntityMeshData m = mesh("minotaur.txt");
        assertEquals("minotaur", m.id);
        for (String role : MeshPose.ROLE_NAMES) assertTrue(m.part(role) >= 0 && m.body[m.part(role)].length > 0, "part " + role);
        assertTrue(m.quads() <= 2800, "body + glow quads within the build budget: " + m.quads());
        assertTrue(m.quads(EntityMeshData.GLOW) > 0, "the eyes glow");
        float[] h = m.heightBlocks();
        assertTrue(h[0] > -0.2f && h[0] < 0.2f, "hooves on the floor: " + h[0]);
        assertTrue(h[1] > 3.0f && h[1] < 4.0f, "about 3 blocks, horns included: " + h[1]);
        assertTrue(mesh("minotaur_lod.txt").quads() < m.quads() / 2, "the far copy is less than half");
    }

    @Test
    void theSkinsAndThePreviews() throws IOException {
        for (String s : new String[]{"goliath.png", "goliath_glow.png", "minotaur.png", "minotaur_glow.png"}) {
            BufferedImage img = ImageIO.read(Path.of(ASSETS + "textures/entity/boss_mesh/" + s).toFile());
            assertNotNull(img, s);
            assertEquals(img.getWidth(), img.getHeight(), s + " is square");
            assertTrue(img.getWidth() <= 1024 && Integer.bitCount(img.getWidth()) == 1, s + " is a power of two up to 1024 px");
        }
        for (String p : new String[]{"goliath_blender.png", "goliath_blender_side.png", "goliath_blender_slam.png", "minotaur_blender.png", "minotaur_blender_side.png",
                "minotaur_blender_slam.png"}) {
            Path f = Path.of("docs/bosses/previews/" + p);
            assertTrue(Files.exists(f), p);
            assertTrue(Files.size(f) < 300 * 1024, p + " under 300 KB");
        }
    }

    @Test
    void theParserRefusesBrokenFiles() {
        assertThrows(IllegalArgumentException.class, () -> EntityMeshData.parse(new BufferedReader(new StringReader("entity x 0.0625\npart a\n"))), "a part without a pivot");
        assertThrows(IllegalArgumentException.class, () -> EntityMeshData.parse(new BufferedReader(new StringReader("entity x 0.0625\npivot b a 0 0 0\npivot a - 0 0 0\n"))),
                "a parent after its child");
        StringBuilder big = new StringBuilder("entity x 0.0625\npivot a - 0 0 0\npart a\n");
        String q = "qn 0" + " 0 0 0 0 0 0 1 0".repeat(4) + "\n";
        big.append(q.repeat(EntityMeshData.MAX_QUADS + 1));
        assertThrows(IllegalArgumentException.class, () -> EntityMeshData.parse(new BufferedReader(new StringReader(big.toString()))), "over the per-entity budget");
    }

    @Test
    void everyBlowStartsAndEndsAtRest() {
        MeshPose rest = new MeshPose(), p = new MeshPose();
        rest.solve(40f, 0f, 0f, MeshPose.NONE, 0f);
        for (int a : new int[]{MeshPose.SWING, MeshPose.SLAM, MeshPose.STOMP, MeshPose.ROAR, MeshPose.PUNCH, MeshPose.HURL}) {
            for (float t : new float[]{0f, 1f}) {
                p.solve(40f, 0f, 0f, a, t);
                for (int i = 0; i < rest.rot.length; i++) assertEquals(rest.rot[i], p.rot[i], 1e-3, "action " + a + " at " + t + " joint " + i);
            }
        }
        p.solve(40f, 0f, 0f, MeshPose.SLAM, 0.72f);
        assertTrue(p.get(MeshPose.ARM_R, 0) < -2f && p.get(MeshPose.ARM_L, 0) < -2f, "both fists high before the slam");
        p.solve(40f, 0f, 0f, MeshPose.SLAM, 0.82f);
        assertTrue(p.get(MeshPose.TORSO, 0) > 0.3f, "leans into the slam when it lands");
        p.solve(40f, 0.25f, 1f, MeshPose.NONE, 0f);
        assertTrue(p.get(MeshPose.LEG_L, 0) > 0.3f && p.get(MeshPose.LEG_R, 0) < -0.3f, "the legs swing against each other");
        p.solve(40f, 0f, 0f, MeshPose.RISE, 0f);
        assertTrue(p.get(MeshPose.ARM_L, 0) < -2.5f, "the fists come up first");
        p.solve(40f, 0f, 0f, MeshPose.RISE, 1f);
        assertEquals(rest.get(MeshPose.ARM_L, 0), p.get(MeshPose.ARM_L, 0), 1e-3, "standing at the end of the rise");
        assertEquals(MeshPose.SLAM, MeshPose.action("slam"));
        assertEquals(MeshPose.STOMP, MeshPose.action("stomp"));
        assertEquals(MeshPose.NONE, MeshPose.action("walk"));
        assertEquals(MeshPose.NONE, MeshPose.action(null));
    }

    @Test
    void theSettingTheOldPathAndTheClientRule() throws IOException {
        String opts = read(SRC + "client/playermodel/PlayerModelOptions.java");
        assertTrue(opts.contains(".define(\"goliathBlenderModel\", true)"), "a client setting, on by default");
        assertTrue(opts.contains(".define(\"goliathStoneModel\", true)"), "the wave 1 setting stays");
        assertTrue(opts.contains(".define(\"minotaurBlenderModel\", true)"), "the Minotaur's client setting, on by default");
        String arc = read(SRC + "client/arc/ArcBossRenderer.java");
        assertTrue(arc.contains("!useSecond && PlayerModelOptions.minotaurBlender() && blender.available()"), "the Minotaur's Blender body only in its base form, behind its setting");
        assertTrue(arc.contains("paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);"), "the art kit path is kept");
        String r = read(SRC + "client/floor/goliath/GoliathRenderer.java");
        assertTrue(r.contains("PlayerModelOptions.goliathBlender() && blender.available()"), "the Blender body only when the setting is on and the mesh loaded");
        assertTrue(r.contains("paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);"), "the 3.96.0 stone geo path is kept");
        assertTrue(Files.exists(Path.of(ASSETS + "geo/entity/goliath_body.geo.json")) && Files.exists(Path.of(ASSETS + "animations/entity/goliath_body.animation.json")),
                "the 3.96.0 stone model and its clips stay (the clips also pace the Blender body)");
        String data = read(SRC + "mesh/EntityMeshData.java");
        assertFalse(data.contains("net.minecraft") || data.contains("com.mojang") || data.contains("nusmp.client"), "the mesh parser is pure common code");
        assertTrue(Files.exists(Path.of(SRC + "client/bossmesh/BossMeshDraw.java")), "the renderer lives under client/");
        String doc = read("docs/bosses/rework_wave2_models.md");
        for (String w : new String[]{"NEW", "REPLACED", "KEPT", "Blender", "goliathBlenderModel", "minotaurBlenderModel", "quads"}) assertTrue(doc.contains(w), w);
    }
}
