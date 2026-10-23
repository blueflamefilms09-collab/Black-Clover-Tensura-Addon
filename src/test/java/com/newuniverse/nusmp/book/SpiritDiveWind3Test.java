package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules.Ver;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the three designs of the Wind remake (A, B, C) with real 3D pieces are pure tables plus mesh files; this test pins what no game is needed for: the option values (append only, PACK and REEL stay
 * first), the mapping, every mesh (it loads, finite, a body, a glow shell where the painter draws one, bounded size), the weapon budgets, the vertex budget of every design, tier, level of detail and stage
 * under its cap and under the order the levels of detail promise (full >= mid >= far >= 0), a standard dive never dearer than a Yuno one, the palettes (no blue), and the geometry (finite, bounded).
 */
class SpiritDiveWind3Test {
    private static final Lod[] LODS = {Lod.FULL, Lod.MID, Lod.FAR};

    @Test
    void theOptionKeepsTheOldValuesAndAppendsTheThreeDesigns() {
        assertEquals("[PACK, REEL, A_ANIME, B_CLIP, C_SAINT]", java.util.Arrays.toString(WindLook.values()), "append only");
        assertNull(SpiritDiveWind3Rules.ver(WindLook.PACK));
        assertNull(SpiritDiveWind3Rules.ver(WindLook.REEL));
        assertNull(SpiritDiveWind3Rules.ver(null));
        for (Ver v : Ver.values()) assertEquals(v, SpiritDiveWind3Rules.ver(SpiritDiveWind3Rules.look(v)));
        assertNotNull(SpiritDiveWind3Rules.ver(SpiritDiveWindRules.DEFAULT_LOOK), "the default is one of the three designs");
        assertEquals(SpiritDiveWindRules.DEFAULT_LOOK, SpiritDiveWindRules.parse("nonsense"));
        assertEquals(WindLook.C_SAINT, SpiritDiveWindRules.parse(" c_saint "));
        assertEquals(WindLook.PACK, SpiritDiveWindRules.parse("pack"));
    }

    @Test
    void everyMeshLoadsAndIsSane() {
        for (String n : SpiritDiveWind3Mesh.NAMES) {
            assertTrue(SpiritDiveWind3Mesh.exists(n), n + ".txt must exist");
            SpiritDiveWind3Mesh m = SpiritDiveWind3Mesh.get(n);
            assertTrue(m.quads(SpiritDiveWind3Mesh.SOLID) >= 4, n + " has a body");
            assertTrue(m.radius > 1f && m.radius < 80f, n + " radius " + m.radius);
            for (int l = 0; l < SpiritDiveWind3Mesh.LAYERS; l++) {
                int q = m.quads(l);
                assertEquals(q * 12, m.pos[l].length);
                assertEquals(q * 8, m.uv[l].length);
                assertEquals(q * 3, m.nrm[l].length);
                for (float f : m.pos[l]) assertTrue(!Float.isNaN(f) && !Float.isInfinite(f), n + " finite");
                for (int i = 0; i < q; i++) {
                    float x = m.nrm[l][i * 3], y = m.nrm[l][i * 3 + 1], z = m.nrm[l][i * 3 + 2];
                    assertEquals(1f, x * x + y * y + z * z, 1e-2f, n + " unit normal");
                }
                for (float u : m.uv[l]) assertTrue(u >= 0f && u <= 1f, n + " uv in the atlas");
            }
        }
        assertEquals(0, SpiritDiveWind3Mesh.get("no_such_piece").totalVerts(), "a missing mesh draws nothing");
    }

    @Test
    void theWeaponsAreRealModelsWithTheirPartsAndBoundedBudgets() {
        SpiritDiveWind3Mesh sword = SpiritDiveWind3Mesh.get("sword"), bow = SpiritDiveWind3Mesh.get("bow"), arrow = SpiritDiveWind3Mesh.get("arrow"), spear = SpiritDiveWind3Mesh.get("spear");
        assertTrue(sword.verts(SpiritDiveWind3Mesh.SOLID) >= 300 && sword.verts(SpiritDiveWind3Mesh.GLOW) > 0 && sword.verts(SpiritDiveWind3Mesh.ORNAMENT) >= 200, "blade, guard, grip, a glow shell and the crossed rings");
        assertTrue(bow.verts(SpiritDiveWind3Mesh.SOLID) >= 200 && bow.verts(SpiritDiveWind3Mesh.ORNAMENT) > 0, "leaf limbs and the cross ornaments");
        assertTrue(arrow.totalVerts() > 100 && spear.totalVerts() > 100);
        assertTrue(sword.totalVerts() <= 1000, "the sword stays under 1000 vertices: " + sword.totalVerts());
        assertTrue(bow.totalVerts() <= 700 && arrow.totalVerts() <= 350 && spear.totalVerts() <= 450, "bow / arrow / spear budgets");
        for (String n : new String[]{"blade", "feather", "cube", "panel", "plate"}) assertTrue(SpiritDiveWind3Mesh.get(n).totalVerts() <= 140, n + " is a small piece");
        assertTrue(SpiritDiveWind3Mesh.get("crown").totalVerts() <= 750, "the crown of arrow bars");
        // the long pieces really are long: the sword's blade reaches 46 px along +y, the bow is about 50 px tall
        assertTrue(sword.radius > 45f && sword.radius < 60f, "sword reach " + sword.radius);
        assertTrue(bow.radius > 24f && bow.radius < 32f, "bow reach " + bow.radius);
    }

    @Test
    void theSpecsGrowWithTheTierAndShrinkWithTheDistance() {
        for (Ver v : Ver.values()) {
            for (boolean ch : new boolean[]{false, true}) {
                assertEquals(0, SpiritDiveWind3Rules.vertices(v, ch, Lod.CULLED, Stage.FULL));
                for (Stage st : Stage.values()) {
                    int full = SpiritDiveWind3Rules.vertices(v, ch, Lod.FULL, st), mid = SpiritDiveWind3Rules.vertices(v, ch, Lod.MID, st), far = SpiritDiveWind3Rules.vertices(v, ch, Lod.FAR, st);
                    assertTrue(full >= mid && mid >= far && far > 0, v + " " + st + (ch ? " Yuno" : " standard") + ": " + full + " / " + mid + " / " + far);
                    assertTrue(far <= 300, v + " far: only the body light and the ground ring: " + far);
                }
            }
            for (Stage st : Stage.values()) assertTrue(SpiritDiveWind3Rules.vertices(v, false, Lod.FULL, st) <= SpiritDiveWind3Rules.vertices(v, true, Lod.FULL, st), v + " standard never dearer than Yuno");
        }
        assertEquals(0, SpiritDiveWind3Rules.spec(Ver.B, true, Lod.FAR).cubes());
        assertEquals(1, SpiritDiveWind3Rules.spec(Ver.A, true, Lod.FULL).sword());
        assertEquals(2, SpiritDiveWind3Rules.spec(Ver.A, false, Lod.FULL).sword(), "the standard tier floats a small leaf blade");
        assertEquals(0, SpiritDiveWind3Rules.spec(Ver.C, true, Lod.FULL).sword(), "C has no sword: the arrows are the weapon");
        assertTrue(SpiritDiveWind3Rules.spec(Ver.C, true, Lod.FULL).orbiters() >= 6 && SpiritDiveWind3Rules.spec(Ver.C, true, Lod.FULL).orbiters() <= 8, "a halo of 6 to 8 arrows");
    }

    @Test
    void everyDesignStaysUnderItsCapAtEveryStage() {
        List<String> bad = new ArrayList<>();
        for (Ver v : Ver.values())
            for (boolean ch : new boolean[]{false, true})
                for (Stage st : Stage.values())
                    for (Lod lod : LODS) {
                        int n = SpiritDiveWind3Rules.vertices(v, ch, lod, st);
                        if (n > SpiritDiveWind3Rules.cap(v, ch)) bad.add(v + " " + (ch ? "Yuno " : "standard ") + st + " " + lod + " " + n);
                    }
        assertTrue(bad.isEmpty(), bad.toString());
        assertEquals(4000, SpiritDiveWind3Rules.CAP_CHARACTER);
        assertEquals(2500, SpiritDiveWind3Rules.CAP_STANDARD);
        // the worst cases the doc quotes
        for (Ver v : Ver.values()) System.out.println("wind3 " + v + " worst full dive: Yuno " + SpiritDiveWind3Rules.worst(v, true) + ", standard " + SpiritDiveWind3Rules.worst(v, false)
                + "; MID " + SpiritDiveWind3Rules.vertices(v, true, Lod.MID, Stage.FULL) + " / " + SpiritDiveWind3Rules.vertices(v, false, Lod.MID, Stage.FULL)
                + "; FAR " + SpiritDiveWind3Rules.vertices(v, true, Lod.FAR, Stage.FULL));
    }

    @Test
    void theVersionAKeepsThePackLookExceptTheWeapons() {
        for (boolean ch : new boolean[]{false, true})
            for (Stage st : Stage.values())
                for (Lod lod : LODS) {
                    int pack = SpiritDiveWindRules.vertices(ch, lod, st), a = SpiritDiveWind3Rules.vertices(Ver.A, ch, lod, st);
                    boolean weapons = lod == Lod.FULL && SpiritDivePieces.full(st);
                    if (!weapons) assertEquals(pack, a, "A is the PACK look away from the weapons: " + ch + " " + st + " " + lod);
                    else assertTrue(a > pack - 16, "A draws the real weapons: " + a + " vs " + pack);
                }
    }

    @Test
    void thePalettesHaveNoBlue() {
        for (Ver v : Ver.values())
            for (boolean ch : new boolean[]{false, true}) {
                Pal p = SpiritDiveWind3Rules.pal(v, ch);
                for (int c : new int[]{p.body(), p.glow(), p.light(), p.rim(), p.gold(), SpiritDiveWind3Rules.cage(v)}) {
                    int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                    assertTrue(g >= b && g >= r - 40, v + " colour " + Integer.toHexString(c) + " must be green / white, not blue");
                }
            }
    }

    @Test
    void theGeometryIsFiniteAndBounded() {
        float[] g = new float[6];
        for (int i = 0; i < SpiritDiveWind3Geometry.CROWN_PLACES; i++) {
            SpiritDiveWind3Geometry.crownCube(g, i);
            for (float f : g) assertTrue(!Float.isNaN(f) && Math.abs(f) < 20f);
            assertTrue(g[3] > 0 && g[4] > 0 && g[5] > 0);
        }
        for (int n : new int[]{4, 6, 8})
            for (int i = 0; i < n; i++)
                for (float age : new float[]{0f, 33.3f, 1000f}) {
                    SpiritDiveWind3Geometry.orbiter(g, i, n, age);
                    float r = (float) Math.sqrt(g[0] * g[0] + g[2] * g[2]);
                    assertTrue(r > 10f && r < 20f, "the halo stays round the body: " + r);
                    assertEquals(1f, g[3] * g[3] + g[4] * g[4] + g[5] * g[5], 1e-3f);
                }
        for (float side : new float[]{1f, -1f})
            for (int i = 0; i < 5; i++) {
                SpiritDiveWind3Geometry.featherDir(g, i, 5, side, 0.1f);
                assertEquals(1f, g[0] * g[0] + g[1] * g[1] + g[2] * g[2], 1e-3f);
                assertTrue(g[1] < 0f, "every feather points up (y is down)");
                assertTrue(side * g[0] > -0.01f, "and out");
            }
        assertTrue(SpiritDiveWind3Geometry.reach() < 130f, "the reach of every piece: " + SpiritDiveWind3Geometry.reach());
        assertEquals(SpiritDiveWindGeometry.BARS_MAX, SpiritDiveWind3Geometry.CROWN_PLACES);
        // the sword's direction vectors point up (negative y) and forward (negative z)
        assertTrue(SpiritDiveWind3Geometry.SWORD[4] < 0f && SpiritDiveWind3Geometry.SWORD_B[4] < 0f && SpiritDiveWind3Geometry.SPEAR_B[4] < 0f);
    }
}
