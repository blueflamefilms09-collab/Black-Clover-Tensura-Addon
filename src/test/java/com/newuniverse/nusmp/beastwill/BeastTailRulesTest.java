package com.newuniverse.nusmp.beastwill;

import com.newuniverse.nusmp.mesh.EntityMeshData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/beastwill_tails.md): the Blender tails and tentacles of the Beast Will forms. Pins the part-name to role mapping and the sway bounds, and reads the shipped files: every form in
 * {@link BeastTailRules} has its mesh (and the far copy), within the 1500-quad cap, with exactly the chains the table says and no part outside {@code sw<chain>_<segment>}; its skin and glow are square
 * powers of two; the previews are small; and the Kraken / Scylla body pieces without cube tentacles exist. Pure (file reads, project root).
 */
class BeastTailRulesTest {
    private static final File RES = new File("src/main/resources/assets/anime_multiverse");

    @Test
    void roleNumbersFollowThePartNames() {
        assertEquals(100, BeastTailRules.role("sw0_0"));
        assertEquals(100 + 16 * 11 + 3, BeastTailRules.role("sw11_3"));
        assertEquals(-1, BeastTailRules.role("body"));
        assertEquals(-1, BeastTailRules.role("sw_1"));
        assertEquals(-1, BeastTailRules.role("swx_1"));
        assertEquals(-1, BeastTailRules.role("sw16_0"));
        assertEquals(-1, BeastTailRules.role("sw0_16"));
        assertEquals(-1, BeastTailRules.role(null));
        int r = BeastTailRules.role("sw5_7");
        assertTrue(BeastTailRules.isRole(r));
        assertEquals(5, BeastTailRules.chainOf(r));
        assertEquals(7, BeastTailRules.segOf(r));
        assertFalse(BeastTailRules.isRole(10));
    }

    @Test
    void theSwayIsGentleAndNeverNaN() {
        float[] out = new float[3];
        for (int kind = 0; kind <= 3; kind++)
            for (int chain = 0; chain < BeastTailRules.MAX_CHAIN; chain++)
                for (int seg = 0; seg < BeastTailRules.MAX_SEG; seg++)
                    for (float t = 0f; t < 20f; t += 0.37f) {
                        BeastTailRules.angles(kind, chain, seg, t, out);
                        for (float a : out) assertTrue(Math.abs(a) <= 0.25f, "turn " + a + " is too large (kind " + kind + ")");
                        assertEquals(0f, out[2]);
                    }
    }

    @Test
    void theTableHasTheNineForms() {
        assertEquals(Set.of("dragon", "nidhoggr", "jormungandr", "serpent", "apophis", "typhon", "kitsune", "kraken", "scylla_charybdis"), BeastTailRules.stems());
        assertEquals(12, BeastTailRules.of("kraken").chains());
        assertEquals(9, BeastTailRules.of("kitsune").chains());
        for (String s : BeastTailRules.stems()) {
            assertTrue(BeastWillFormTable.stems().contains(s), s + " is not a form");
            assertTrue(BeastTailRules.of(s).chains() <= BeastTailRules.MAX_CHAIN);
        }
    }

    @Test
    void everyMeshFileIsThereWithinTheCapsAndNamedAsTheTableSays() throws Exception {
        for (String stem : BeastTailRules.stems()) {
            String id = BeastTailRules.meshId(stem);
            int quads = -1;
            for (String suffix : new String[]{"", "_lod"}) {
                File f = new File(RES, "entity_meshes/" + id + suffix + ".txt");
                assertTrue(f.isFile(), f + " missing");
                EntityMeshData m;
                try (BufferedReader r = new BufferedReader(new FileReader(f))) { m = EntityMeshData.parse(r); }
                assertTrue(m.quads() > 0 && m.quads() <= BeastTailRules.MAX_QUADS, id + suffix + ": " + m.quads() + " quads");
                if (suffix.isEmpty()) quads = m.quads(); else assertTrue(m.quads() < quads, id + ": the far copy is lighter");
                Set<Integer> chains = new HashSet<>();
                for (String p : m.parts) {
                    int role = BeastTailRules.role(p);
                    assertTrue(role >= 0, id + ": part " + p + " is not sw<chain>_<segment>");
                    chains.add(BeastTailRules.chainOf(role));
                }
                assertEquals(BeastTailRules.of(stem).chains(), chains.size(), id + suffix + ": chains");
            }
            for (String suffix : new String[]{".png", "_glow.png"}) {
                File f = new File(RES, "textures/entity/boss_mesh/" + id + suffix);
                assertTrue(f.isFile(), f + " missing");
                BufferedImage img = ImageIO.read(f);
                assertEquals(img.getWidth(), img.getHeight(), f + " is not square");
                assertTrue(Integer.bitCount(img.getWidth()) == 1, f + " is not a power of two");
                assertTrue(f.length() < 400_000L, f + " is " + f.length() + " bytes");
            }
            for (String v : new String[]{"side", "back"}) {
                File f = new File("docs/beastwill_tails/previews/" + stem + "_" + v + ".png");
                assertTrue(f.isFile(), f + " missing");
                assertTrue(f.length() < 300_000L, f + " is " + f.length() + " bytes");
            }
        }
    }

    @Test
    void theBodiesWithoutCubeTentaclesExist() {
        for (String stem : new String[]{"kraken", "scylla_charybdis"}) {
            assertTrue(BeastTailRules.of(stem).bodyNoTail());
            assertTrue(new File(RES, "geo/entity/bwf_" + stem + "_arm_body_nt.geo.json").isFile());
            assertTrue(new File(RES, "animations/entity/bwf_" + stem + "_arm_body_nt.animation.json").isFile());
        }
        for (String stem : new String[]{"dragon", "kitsune", "apophis"}) assertFalse(BeastTailRules.of(stem).bodyNoTail());
    }
}
