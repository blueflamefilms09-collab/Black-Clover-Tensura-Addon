package com.newuniverse.nusmp.client.vupgrade.godb;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The performance budget of the upgraded God melee weapons (release 1.102.0): every one has a level-of-detail mesh with at most 15% of the quads, the full-detail mesh (with its orbiting
 * pieces) stays within the God cap of 2500 welded vertices, the floor of 600 quads holds, the skins are bigger and the old files the fallback draws are still shipped.
 */
class GodMeleeBBudgetTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");
    static final List<String> IDS = UpgradeSpecTest.IDS;
    /** The God cap of the brief (2500 vertices of one item in hand), read as welded vertices of the full-detail mesh including the orbiting pieces. */
    static final int GOD_WELDED_CAP = 2500;
    static final int GOD_QUAD_FLOOR = 600;

    record MeshStat(int body, int glow, int welded) {}

    static MeshStat stat(Path p) throws IOException {
        int body = 0, glow = 0;
        Set<String> pts = new HashSet<>();
        for (String line : Files.readAllLines(p)) {
            String[] v = line.trim().split("\\s+");
            if (!v[0].equals("qn")) continue;
            int layer = Integer.parseInt(v[1]);
            if (layer == 2) {
                glow++;
                continue;
            }
            body++;
            for (int k = 0; k < 4; k++) {
                int o = 2 + 8 * k;
                pts.add(String.format("%.2f %.2f %.2f", Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2])));
            }
        }
        return new MeshStat(body, glow, pts.size());
    }

    static long painted(Path png) throws IOException {
        if (!Files.exists(png)) return 0;
        BufferedImage im = ImageIO.read(png.toFile());
        long n = 0;
        for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++) if ((im.getRGB(x, y) >>> 24) > 0) n++;
        return n;
    }

    @Test
    void everyWeaponHasALevelOfDetailMeshWithinTheBudget() throws IOException {
        for (String id : IDS) {
            UpgradeSpec s = UpgradeSpec.parse(UpgradeSpecTest.read(id));
            MeshStat full = stat(RES.resolve("weapon_meshes/" + s.mesh() + ".txt"));
            MeshStat lod = stat(RES.resolve("weapon_meshes/" + s.lodMesh() + ".txt"));
            MeshStat orbit = stat(RES.resolve("weapon_meshes/" + s.orbitMesh() + ".txt"));
            assertTrue(full.body() >= GOD_QUAD_FLOOR, id + ": " + full.body() + " quads, the floor of a God hand weapon is " + GOD_QUAD_FLOOR);
            assertTrue(lod.body() > 0, id + ": the level-of-detail mesh is empty");
            assertTrue(lod.body() * 4 <= 0.15 * full.body() * 4, id + ": the level-of-detail mesh has " + lod.body() + " of " + full.body() + " quads (limit 15%)");
            assertTrue(lod.glow() > 0 && lod.glow() <= lod.body(), id + ": the level-of-detail mesh keeps a glow layer");
            assertTrue(orbit.body() > 0 && orbit.body() <= 80, id + ": the orbiting piece has " + orbit.body() + " quads");
            int fullDetail = full.welded() + s.orbitPieces() * orbit.welded();
            assertTrue(fullDetail <= GOD_WELDED_CAP, id + ": " + fullDetail + " welded vertices at full detail, the God cap is " + GOD_WELDED_CAP);
        }
    }

    @Test
    void theNewSkinsAreBiggerAndTheOldFilesAreKept() throws IOException {
        for (String id : IDS) {
            UpgradeSpec s = UpgradeSpec.parse(UpgradeSpecTest.read(id));
            Path tex = RES.resolve("textures/entity/weapon");
            Path skin = tex.resolve(s.mesh() + ".png");
            assertTrue(Files.exists(skin) && Files.exists(tex.resolve(s.mesh() + "_glow.png")) && Files.exists(tex.resolve(s.mesh() + "_glow2.png")), id + ": skin, core glow and vein glow");
            assertTrue(Files.exists(RES.resolve("weapon_meshes/" + id + ".txt")) && Files.exists(tex.resolve(id + ".png")), id + ": the old mesh and skin are kept (add the new, keep the old)");
            BufferedImage im = ImageIO.read(skin.toFile());
            assertTrue(im.getWidth() >= 1024 && im.getWidth() == im.getHeight(), id + ": the skin is " + im.getWidth() + " px");
            long before = painted(tex.resolve(id + ".png")) + painted(tex.resolve(id + "_glow.png"));
            long after = painted(skin) + painted(tex.resolve(s.mesh() + "_glow.png")) + painted(tex.resolve(s.mesh() + "_glow2.png"));
            assertTrue(after >= 4 * before, id + ": painted texels " + before + " -> " + after + " (target 4x)");
        }
    }

    @Test
    void theMeshesCarryTheDirectivesTheRendererNeeds() throws IOException {
        for (String id : IDS) {
            for (String suffix : new String[]{"", "_lod"}) {
                List<String> lines = Files.readAllLines(RES.resolve("weapon_meshes/up/" + id + suffix + ".txt"));
                assertTrue(lines.stream().anyMatch(l -> l.startsWith("place ")), id + suffix + ": place line");
                assertTrue(lines.stream().anyMatch(l -> l.trim().equals("orient manual")), id + suffix + ": orient manual (the runtime normaliser never touches it, WeaponMeshesTest skips it)");
                assertTrue(lines.stream().anyMatch(l -> l.startsWith("glow ")), id + suffix + ": glow line");
            }
            // the full mesh sits in the hand exactly like the old one
            String oldPlace = Files.readAllLines(RES.resolve("weapon_meshes/" + id + ".txt")).stream().filter(l -> l.startsWith("place ")).findFirst().orElseThrow();
            String newPlace = Files.readAllLines(RES.resolve("weapon_meshes/up/" + id + ".txt")).stream().filter(l -> l.startsWith("place ")).findFirst().orElseThrow();
            assertEquals(oldPlace, newPlace, id + ": the placement in the hand is the old one");
        }
    }
}
