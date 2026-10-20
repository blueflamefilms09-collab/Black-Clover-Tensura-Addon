package com.newuniverse.nusmp.client.relics.worn.lib;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** The records the visual upgrade adds to the worn model format: skin, glow map, star texture, lod, the three new quad flags and the TUMBLE animation. */
class WornMeshUpgradeTest {
    static String quad(int g, int slot, int flags) {
        StringBuilder b = new StringBuilder("q " + g + " " + slot + " " + flags);
        for (int k = 0; k < 4; k++) b.append(String.format(Locale.ROOT, " %.3f %.3f %.3f 0.25 0.5 0 -1 0", (float) k, 2.0f, 3.0f));
        return b.toString();
    }

    @Test
    void parsesTheTextureRecordsAndCountsTheNewFlags() {
        WornMesh m = WornMesh.parse("vu/t", List.of("model vu/t", "anchor body", "skin visual_upgrade/t.png", "glowtex visual_upgrade/t_glow.png", "startex visual_upgrade/t_star.png", "lod vu/t_lod",
                "grp 1 core tumble 0 0 0 0.7 0.3 0.5", quad(0, 0, 0), quad(0, 0, WornMesh.GLOWMAP), quad(1, 0, WornMesh.STAR), quad(1, 2, WornMesh.HALO), quad(0, 0, WornMesh.GLOWMAP | WornMesh.HOT)));
        assertTrue(m.custom());
        assertEquals("visual_upgrade/t.png", m.skin);
        assertEquals("visual_upgrade/t_glow.png", m.glowTex);
        assertEquals("visual_upgrade/t_star.png", m.starTex);
        assertEquals("vu/t_lod", m.lodName);
        assertEquals(5, m.quads);
        assertEquals(2, m.glowMapQuads);
        assertEquals(1, m.starQuads);
        assertEquals(1, m.haloQuads);
        assertEquals(WornMesh.Kind.TUMBLE, m.groups[1].kind());
        assertEquals(5 * 4 + 2 * 4, m.totalVertices(), "every quad once, the glow map quads once more");
    }

    @Test
    void aLibraryModelStaysAsItWas() {
        WornMesh m = WornMesh.parse("old", List.of("model old", "anchor head", quad(0, 0, 0), quad(0, 2, WornMesh.EMIT)));
        assertFalse(m.custom());
        assertNull(m.skin);
        assertNull(m.lodName);
        assertSame(m, m.lod(), "a model without a lod record is its own lod");
        WornMesh far = WornMesh.parse("far", List.of("model far", "anchor head", quad(0, 0, 0)));
        m.attachLod(far);
        assertSame(far, m.lod(), "an attached lod is the model drawn from far away");
        assertEquals(0, m.glowMapQuads);
        assertEquals(1, m.emitQuads);
        assertEquals(2 * 4 + 1 * 4, m.totalVertices());
    }

    @Test
    void tumbleTurnsAboutAllThreeAxes() {
        WornMesh.Group g = new WornMesh.Group("t", WornMesh.Kind.TUMBLE, 0f, 0f, 0f, 2f, 3f, 5f);
        WornAnim.Xf xf = new WornAnim.Xf();
        WornAnim.eval(g, 10f, 0f, false, xf);
        float deg = (float) (Math.PI / 180.0);
        assertEquals(20f * deg, xf.rx, 1e-5f);
        assertEquals(30f * deg, xf.rz, 1e-5f);
        assertEquals(50f * deg, xf.ry, 1e-5f);
        assertEquals(1f, xf.s, 0f);
    }
}
