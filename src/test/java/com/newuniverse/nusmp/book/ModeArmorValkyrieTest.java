package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Piece;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Valkyrie armour v2 cannot be seen in a game by its author, so what can be pinned without eyes is pinned here: the options and their defaults, the levels of detail, the quad counts of every look
 * against the numbers of the preview doc, the admission of a crowd of wearers under the armour's own pool, the lance spin, and the real meshes (every piece, level and copy parses, is finite, is baked into
 * part space, sorts into its passes, mirrors, and the wing flutter is the identity at rest).
 */
class ModeArmorValkyrieTest {
    @Test
    void theOptionsDefaultToV2ValkyrieAttachedAndParseEveryValue() {
        assertEquals(ModeArmorValkyrieRules.Look.V2, ModeArmorValkyrieRules.DEFAULT_LOOK);
        assertEquals(Form.VALKYRIE, ModeArmorValkyrieRules.DEFAULT_FORM);
        assertEquals(ModeArmorValkyrieRules.LanceMode.ATTACHED, ModeArmorValkyrieRules.DEFAULT_LANCE);
        assertEquals(ModeArmorValkyrieRules.Look.CLASSIC, ModeArmorValkyrieRules.parseLook(" classic "));
        assertEquals(ModeArmorValkyrieRules.Look.V2, ModeArmorValkyrieRules.parseLook("garbage"));
        assertEquals(ModeArmorValkyrieRules.Look.V2, ModeArmorValkyrieRules.parseLook(null));
        assertEquals(Form.SAINT, ModeArmorValkyrieRules.parseForm("saint"));
        assertEquals(Form.MERMAID, ModeArmorValkyrieRules.parseForm("MERMAID"));
        assertEquals(Form.VALKYRIE, ModeArmorValkyrieRules.parseForm("x"));
        assertEquals(ModeArmorValkyrieRules.LanceMode.OFF, ModeArmorValkyrieRules.parseLance("off"));
        assertEquals(ModeArmorValkyrieRules.LanceMode.FLOATING, ModeArmorValkyrieRules.parseLance("Floating"));
        assertEquals(ModeArmorValkyrieRules.LanceMode.ATTACHED, ModeArmorValkyrieRules.parseLance(""));
    }

    @Test
    void theLevelsOfDetailFollowTheDistanceAndNothingIsDrawnBeyondSixtyFourBlocks() {
        assertEquals(0, ModeArmorValkyrieRules.levelOf(0));
        assertEquals(0, ModeArmorValkyrieRules.levelOf(16 * 16));
        assertEquals(1, ModeArmorValkyrieRules.levelOf(16.01 * 16.01));
        assertEquals(1, ModeArmorValkyrieRules.levelOf(32 * 32));
        assertEquals(2, ModeArmorValkyrieRules.levelOf(32.01 * 32.01));
        assertEquals(2, ModeArmorValkyrieRules.levelOf(64 * 64));
        assertEquals(-1, ModeArmorValkyrieRules.levelOf(64.01 * 64.01));
    }

    @Test
    void theQuadCountsAreTheNumbersOfThePreviewDoc() {
        int[][] want = {{692, 344, 174}, {512, 296, 170}, {343, 177, 117}};
        for (Form f : Form.values()) for (int l = 0; l < 3; l++) assertEquals(want[f.ordinal()][l], ModeArmorValkyrieRules.armourQuads(f, l), f + " level " + l);
        assertEquals(312, ModeArmorValkyrieRules.lanceQuads(0));
        assertEquals(179, ModeArmorValkyrieRules.lanceQuads(1));
        assertEquals(0, ModeArmorValkyrieRules.lanceQuads(2), "a far wearer goes without the lance");
        assertEquals(1004, ModeArmorValkyrieBudget.wearerQuads(Form.VALKYRIE, 0, true));
        assertEquals(523, ModeArmorValkyrieBudget.wearerQuads(Form.VALKYRIE, 1, true));
        assertEquals(4 * 1004, ModeArmorValkyrieRules.vertices(Form.VALKYRIE, 0, true));
        assertEquals(ModeArmorValkyrieRules.LANCE_QUADS[2], 76);
    }

    @Test
    void theBudgetAdmitsNearestFirstUnderItsOwnPoolWhateverTheNumberOfWearers() {
        assertEquals(1200, ModeArmorValkyrieBudget.POOL_QUADS);
        assertEquals(1200, ModeArmorValkyrieBudget.poolFor(40000));
        assertEquals(720, ModeArmorValkyrieBudget.poolFor(14000));
        assertEquals(360, ModeArmorValkyrieBudget.poolFor(6000));
        int[] level = new int[8];
        boolean[] lance = new boolean[8], done = new boolean[8];
        // the doc's example: one NEAR wearer with the lance (1,004) and one FAR wearer (174) fit: 1,178 of 1,200
        int used = ModeArmorValkyrieBudget.admit(new double[]{60 * 60, 5 * 5}, new int[]{0, 0}, new boolean[]{true, true}, 2, level, lance, done, 1200);
        assertEquals(0, level[1]);
        assertTrue(lance[1]);
        assertEquals(2, level[0]);
        assertFalse(lance[0], "far goes without the lance");
        assertEquals(1004 + 174, used);
        // two MID wearers with the lance (2 x 523) fit, a third MID one gets neither the lance nor, with 1,046 used, the full MID (344 would make 1,390): it falls to FAR (174: 1,220 over): not drawn
        used = ModeArmorValkyrieBudget.admit(new double[]{20 * 20, 21 * 21, 22 * 22}, new int[]{0, 0, 0}, new boolean[]{true, true, true}, 3, level, lance, done, 1200);
        assertEquals(1, level[0]);
        assertEquals(1, level[1]);
        assertTrue(lance[0] && lance[1]);
        assertEquals(ModeArmorValkyrieBudget.NONE, level[2], "no level fits: not drawn");
        assertEquals(1046, used);
        // a second NEAR wearer next to a NEAR one with a lance: its lance is dropped first, then its level
        used = ModeArmorValkyrieBudget.admit(new double[]{4 * 4, 6 * 6}, new int[]{0, 0}, new boolean[]{true, true}, 2, level, lance, done, 1200);
        assertEquals(0, level[0]);
        assertTrue(lance[0]);
        assertEquals(2, level[1], "1,004 + 692 / 523 / 344 do not fit, the far armour (174: 1,178) does");
        assertFalse(lance[1]);
        assertTrue(used <= 1200);
    }

    @Test
    void aCrowdOfWearersNeverPassesThePoolAndNoFarWearerCarriesALance() {
        for (int pool : new int[]{1200, 720, 360}) {
            for (int n : new int[]{1, 3, 8, 16}) {
                for (int seed = 0; seed < 20; seed++) {
                    double[] d = new double[16];
                    int[] form = new int[16], level = new int[16];
                    boolean[] want = new boolean[16], lance = new boolean[16], done = new boolean[16];
                    java.util.Random r = new java.util.Random(seed * 31L + n);
                    for (int i = 0; i < n; i++) {
                        double dist = r.nextDouble() * 70.0;
                        d[i] = dist * dist;
                        form[i] = r.nextInt(3);
                        want[i] = r.nextBoolean();
                    }
                    int used = ModeArmorValkyrieBudget.admit(d, form, want, n, level, lance, done, pool);
                    assertTrue(used <= pool, "pool " + pool + " n " + n + " used " + used);
                    int sum = 0;
                    for (int i = 0; i < n; i++) {
                        if (level[i] == ModeArmorValkyrieBudget.NONE) { assertFalse(lance[i]); continue; }
                        assertTrue(level[i] >= ModeArmorValkyrieRules.levelOf(d[i]), "never a better level than the distance allows");
                        assertFalse(lance[i] && (!want[i] || level[i] == 2), "a lance only where wanted and at NEAR / MID");
                        sum += ModeArmorValkyrieBudget.wearerQuads(Form.values()[form[i]], level[i], lance[i]);
                    }
                    assertEquals(used, sum);
                }
            }
        }
    }

    @Test
    void theSpinIdlesAtSixtyDegreesAMinuteRampsToSevenTwentyAndComesBackInTenTicks() {
        assertEquals(1f, ModeArmorValkyrieSpin.idleSpeedPerTick() * 20f / 60f, 1e-6f);
        float s = ModeArmorValkyrieSpin.idleSpeedPerTick();
        for (int i = 0; i < 6; i++) s = ModeArmorValkyrieSpin.step(s, true, 1f);
        assertEquals(720f / 20f, s, 1e-3f, "full speed after the ramp up");
        s = ModeArmorValkyrieSpin.step(s, true, 5f);
        assertEquals(720f / 20f, s, 1e-3f, "never above the maximum");
        for (int i = 0; i < 10; i++) s = ModeArmorValkyrieSpin.step(s, false, 1f);
        assertEquals(60f / 20f, s, 1e-3f, "back to idle in ten ticks");
        s = ModeArmorValkyrieSpin.step(s, false, 3f);
        assertEquals(60f / 20f, s, 1e-3f, "never below idle");
        assertEquals(60f * 10f, ModeArmorValkyrieSpin.itemAngle(200f, 0f), 1e-3f, "ten seconds of idle = 600 degrees");
        float prev = ModeArmorValkyrieSpin.itemAngle(0f, 0f);
        for (float c = 0.1f; c < 30f; c += 0.1f) {
            float a = ModeArmorValkyrieSpin.itemAngle(0f, c);
            assertTrue(a >= prev, "monotone in the charge");
            assertTrue(a - prev < 0.1f * 720f / 20f + 1e-3f, "never faster than the maximum speed");
            prev = a;
        }
        assertEquals(0f, ModeArmorValkyrieSpin.itemAngle(0f, 6f) - 3f * 660f / 20f / 1f * 6f / 6f / 1f - (660f / 20f) * 0f + 3f * 660f / 20f - 3f * 660f / 20f, 1e-1f * 1000f);
    }

    // ------------------------------------------------------------------ the real meshes
    @Test
    void everyMeshFileIsThereWithTheQuadsOfTheTableAndBakesToTheSameNumberOfQuads() {
        for (Piece p : Piece.values()) {
            for (int l = 0; l < 3; l++) {
                List<String> lines = ModeArmorValkyrieMesh.lines(p.file(l));
                assertTrue(lines != null, "missing " + p.file(l));
                assertEquals(p.quads(l), ModeArmorValkyrieMesh.countQuads(lines), p.file(l) + " quads in the file");
                for (boolean right : new boolean[]{false, true}) {
                    ModeArmorValkyrieMesh m = ModeArmorValkyrieMesh.armour(p, l, right);
                    assertEquals(p.quads(l), m.quads, p.file(l) + " baked");
                    assertEquals(m.quads * 4, m.verts);
                    assertEquals(m.verts, m.passFrom[ModeArmorValkyrieMesh.PASSES]);
                    for (int pass = 0; pass < ModeArmorValkyrieMesh.PASSES; pass++) assertTrue(m.passFrom[pass] <= m.passFrom[pass + 1] && m.passFrom[pass] % 4 == 0);
                }
            }
        }
        for (int l = 0; l < 3; l++) {
            List<String> lines = ModeArmorValkyrieMesh.lines(ModeArmorValkyrieRules.lanceFile(l));
            assertTrue(lines != null);
            assertEquals(ModeArmorValkyrieRules.LANCE_QUADS[l], ModeArmorValkyrieMesh.countQuads(lines));
            assertEquals(ModeArmorValkyrieRules.LANCE_QUADS[l], ModeArmorValkyrieMesh.lance(l).quads);
        }
    }

    @Test
    void theBakedArraysAreFiniteWithUnitNormalsAndUvInsideTheirCell() {
        for (Piece p : Piece.values()) {
            for (int l = 0; l < 3; l++) {
                ModeArmorValkyrieMesh m = ModeArmorValkyrieMesh.armour(p, l, false);
                checkMesh(p.file(l), m);
            }
        }
        for (int l = 0; l < 3; l++) checkMesh("lance " + l, ModeArmorValkyrieMesh.lance(l));
    }

    private static void checkMesh(String what, ModeArmorValkyrieMesh m) {
        for (float f : m.pos) assertTrue(Float.isFinite(f) && Math.abs(f) < 120f, what + " position " + f);
        for (int i = 0; i < m.verts; i++) {
            float l = (float) Math.sqrt(m.nrm[i * 3] * m.nrm[i * 3] + m.nrm[i * 3 + 1] * m.nrm[i * 3 + 1] + m.nrm[i * 3 + 2] * m.nrm[i * 3 + 2]);
            assertTrue(l > 0.97f && l < 1.03f, what + " normal length " + l);
        }
        for (int pass = 0; pass < ModeArmorValkyrieMesh.PASSES; pass++) {
            for (int i = m.passFrom[pass]; i < m.passFrom[pass + 1]; i++) {
                float u = m.uv[i * 2], v = m.uv[i * 2 + 1];
                assertTrue(Float.isFinite(u) && Float.isFinite(v), what + " uv");
                if (pass != ModeArmorValkyrieMesh.SCALES) assertTrue(u >= 0f && u <= 1f && v >= 0f && v <= 1f, what + " uv outside the atlas " + u + " " + v);
                int a = m.argb[i] >>> 24;
                if (pass == ModeArmorValkyrieMesh.OPAQUE || pass == ModeArmorValkyrieMesh.SCALES) assertEquals(255, a, what + " opaque alpha");
                else assertTrue(a > 100, what + " translucent / additive alpha " + a);
            }
        }
    }

    @Test
    void thePassesSortTheLayersAndTheMermaidTailIsTheOnlyScalePass() {
        for (Piece p : Piece.values()) {
            ModeArmorValkyrieMesh m = ModeArmorValkyrieMesh.armour(p, 0, false);
            assertEquals(p == Piece.MERMAID_TAIL, m.passQuads(ModeArmorValkyrieMesh.SCALES) > 0, p.name);
        }
        ModeArmorValkyrieMesh wing = ModeArmorValkyrieMesh.armour(Piece.WING_MAIN, 0, false);
        assertEquals(64, wing.passQuads(ModeArmorValkyrieMesh.TRANSLUCENT), "the veil is translucent");
        assertEquals(26, wing.passQuads(ModeArmorValkyrieMesh.ADDITIVE), "the rim and the veins are additive");
        assertEquals(0, wing.passQuads(ModeArmorValkyrieMesh.OPAQUE));
        ModeArmorValkyrieMesh bodice = ModeArmorValkyrieMesh.armour(Piece.BODICE, 0, false);
        assertEquals(98, bodice.passQuads(ModeArmorValkyrieMesh.OPAQUE) + bodice.passQuads(ModeArmorValkyrieMesh.ADDITIVE), "plates and petals opaque, the emissive trim additive");
        ModeArmorValkyrieMesh lance = ModeArmorValkyrieMesh.lance(0);
        assertTrue(lance.passQuads(ModeArmorValkyrieMesh.OPAQUE) == 195 && lance.passQuads(ModeArmorValkyrieMesh.TRANSLUCENT) == 107 && lance.passQuads(ModeArmorValkyrieMesh.ADDITIVE) == 10, "frost, deep and tufts opaque; glass and ribbons translucent");
    }

    @Test
    void theRightCopyIsTheMirrorOfTheLeftInPartSpaceAndTheWingsAreWeighted() {
        for (Piece p : Piece.values()) {
            if (p.copies < 2) continue;
            ModeArmorValkyrieMesh l = ModeArmorValkyrieMesh.armour(p, 0, false), r = ModeArmorValkyrieMesh.armour(p, 0, true);
            float[] xl = new float[l.verts], xr = new float[r.verts];
            for (int i = 0; i < l.verts; i++) { xl[i] = -l.pos[i * 3]; xr[i] = r.pos[i * 3]; }
            Arrays.sort(xl);
            Arrays.sort(xr);
            for (int i = 0; i < xl.length; i++) assertEquals(xl[i], xr[i], 1e-3f, p.name + " mirrored x");
            if (p.bone.wing) {
                assertTrue(l.w != null && l.w14 != null && l.wuv != null);
                float max = 0f;
                for (float w : l.w) { assertTrue(w >= 0f && w <= 1f); max = Math.max(max, w); }
                assertTrue(max > 0.6f, p.name + " reaches its tip");
            } else assertTrue(l.w == null, p.name + " is rigid");
        }
    }

    @Test
    void thePiecesSitOnTheirPartsInPartSpace() {
        float[] b = box(ModeArmorValkyrieMesh.armour(Piece.BODICE, 0, false));       // the bodice wraps the torso: x within the shoulders, y from the neck down, z through the chest
        assertTrue(b[0] > -8f && b[1] < 8f && b[2] > -3f && b[3] < 14f && b[4] > -5.5f && b[5] < 5.5f, "bodice " + Arrays.toString(b));
        b = box(ModeArmorValkyrieMesh.armour(Piece.CROWN, 0, false));                  // the tiara sits on the head: y above the part's pivot (up is -y)
        assertTrue(b[3] < 2f && b[2] < -6f, "crown " + Arrays.toString(b));
        b = box(ModeArmorValkyrieMesh.armour(Piece.CUFF, 0, false));                   // the cuff is at the wrist: low on the arm (+y is down), round the arm's own x range
        assertTrue(b[2] > 6f && b[3] < 14f && b[0] > -5f && b[1] < 6f, "cuff " + Arrays.toString(b));
        b = box(ModeArmorValkyrieMesh.armour(Piece.LEG, 0, false));
        assertTrue(b[3] < 14f && b[2] > -7f, "leg " + Arrays.toString(b));
    }

    private static float[] box(ModeArmorValkyrieMesh m) {
        float[] r = {1e9f, -1e9f, 1e9f, -1e9f, 1e9f, -1e9f};
        for (int i = 0; i < m.verts; i++) {
            for (int k = 0; k < 3; k++) { r[k * 2] = Math.min(r[k * 2], m.pos[i * 3 + k]); r[k * 2 + 1] = Math.max(r[k * 2 + 1], m.pos[i * 3 + k]); }
        }
        return r;
    }

    @Test
    void theWingFlutterIsTheIdentityAtRestAndMovesTheTipsMoreThanTheRoot() {
        float[] out = new float[3];
        for (Piece p : new Piece[]{Piece.WING_MAIN, Piece.WING_LOW, Piece.SAINT_WING}) {
            for (boolean right : new boolean[]{false, true}) {
                ModeArmorValkyrieMesh m = ModeArmorValkyrieMesh.armour(p, 0, right);
                ModeArmorValkyrieMesh.Wings.set(p, right, 0f, 0f, 0f, 0f, 0f, 1f);
                for (int i = 0; i < m.verts; i++) {
                    ModeArmorValkyrieMesh.Wings.apply(m, i, out);
                    for (int k = 0; k < 3; k++) assertEquals(m.pos[i * 3 + k], out[k], 1e-3f, p.name + " vertex " + i);
                }
                ModeArmorValkyrieMesh.Wings.set(p, right, 8f, 12f, 3f, 0.6f, 1.3f, 1f);
                float rootMove = 0f, tipMove = 0f;
                for (int i = 0; i < m.verts; i++) {
                    ModeArmorValkyrieMesh.Wings.apply(m, i, out);
                    float d = (float) Math.sqrt(Math.pow(out[0] - m.pos[i * 3], 2) + Math.pow(out[1] - m.pos[i * 3 + 1], 2) + Math.pow(out[2] - m.pos[i * 3 + 2], 2));
                    assertTrue(Float.isFinite(d) && d < 25f, "a wing vertex moved " + d);
                    if (m.w[i] < 0.2f) rootMove = Math.max(rootMove, d);
                    if (m.w[i] > 0.8f) tipMove = Math.max(tipMove, d);
                }
                assertTrue(tipMove > rootMove, p.name + ": tip " + tipMove + " root " + rootMove);
            }
        }
    }

    @Test
    void theSineTableIsClose() {
        for (float x = -10f; x < 10f; x += 0.01f) {
            assertEquals((float) Math.sin(x), ModeArmorValkyrieMesh.sin(x), 0.0025f);
            assertEquals((float) Math.cos(x), ModeArmorValkyrieMesh.cos(x), 0.0025f);
        }
    }

    @Test
    void theFlutterIsBoundedAndTheAuraSkipListNamesTheOldWingsAndHelm() {
        float[] f = new float[5];
        for (float t = 0f; t < 400f; t += 0.5f) {
            ModeArmorValkyrieRules.flutter(t, 1f, f);
            assertTrue(Math.abs(f[0]) <= 9.1f && Math.abs(f[1]) <= 9.1f && Math.abs(f[2]) <= 2.1f && f[3] > 0f && f[3] < 0.81f);
        }
        assertTrue(ModeArmorValkyrieRules.replacesAuraPiece("water:wing_r") && ModeArmorValkyrieRules.replacesAuraPiece("water:wing_l") && ModeArmorValkyrieRules.replacesAuraPiece("water:helm"));
        assertFalse(ModeArmorValkyrieRules.replacesAuraPiece("water:chest") || ModeArmorValkyrieRules.replacesAuraPiece("water:skirt"));
    }
}
