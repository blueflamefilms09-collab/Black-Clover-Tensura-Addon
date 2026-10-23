package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Entry;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Piece;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Wind Spirit Dive overlay v2 (Yuno's layered coat, arrow crown and leaf wings) is pure meshes plus tables; this test pins what no game is needed for: the option and the mode enum are append only
 * and default to V2, every mesh of every level of detail is finite with unit normals, uv inside the texture, front faces that look where their normals look and sway weights in 0..1; the mirrored pieces are
 * built on the left (+x) side; the crown is 60 to 65 percent of the old one; the vertex counts of a wearer stay under 3,200 / 1,200 / 150 at the full / mid / far level and nothing is drawn beyond 64 blocks;
 * the combined calm steady Yuno dive (aura without its wings and crown, plus the overlay) stays under 6,500; and the mode wire sends the Yuno variant only for the Wind Spirit Dive of a Yuno-tier wearer.
 */
class ModeArmorWindV2Test {
    @Test
    void theOptionAndTheModeEnumAreAppendOnlyAndDefaultToV2() {
        assertEquals("[V2, CLASSIC]", java.util.Arrays.toString(ModeArmorWindV2Rules.Look.values()));
        assertEquals(ModeArmorWindV2Rules.Look.V2, ModeArmorWindV2Rules.DEFAULT_LOOK);
        assertEquals(ModeArmorWindV2Rules.Look.V2, ModeArmorWindV2Rules.parse(null));
        assertEquals(ModeArmorWindV2Rules.Look.V2, ModeArmorWindV2Rules.parse("garbage"));
        assertEquals(ModeArmorWindV2Rules.Look.CLASSIC, ModeArmorWindV2Rules.parse(" classic "));
    }

    @Test
    void theModeEnumIsAppendOnlyAndTheYunoVariantIsLast() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/blackclover/ModeArmor.java")).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        Matcher m = Pattern.compile("enum Mode \\{(.*?)\\}", Pattern.DOTALL).matcher(src);
        assertTrue(m.find());
        String body = m.group(1).replaceAll("/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String t : body.split(",")) { String n = t.trim(); if (!n.isEmpty()) names.add(n); }
        assertEquals("[NONE, WIND_SPIRIT_DIVE, FIRE_SPIRIT_DIVE, DEMON, LIGHTNING_GOD, VALKYRIE, DEVIL_EQUUS, DEVIL_CANIS, DEVIL_FELIS, DEVIL_GALLUS, WIND_SPIRIT_DIVE_YUNO]", names.toString(),
                "the mode ordinal goes over the network: append only");
    }

    @Test
    void theModeWireSendsTheYunoVariantOnlyForTheWindDiveOfAYunoTierWearer() {
        assertEquals("WIND_SPIRIT_DIVE_YUNO", ModeArmorWindV2Rules.wireName("WIND_SPIRIT_DIVE", true));
        assertEquals("WIND_SPIRIT_DIVE", ModeArmorWindV2Rules.wireName("WIND_SPIRIT_DIVE", false));
        for (String m : new String[]{"NONE", "FIRE_SPIRIT_DIVE", "DEMON", "LIGHTNING_GOD", "VALKYRIE", "DEVIL_EQUUS", "WIND_SPIRIT_DIVE_YUNO"}) {
            assertEquals(m, ModeArmorWindV2Rules.wireName(m, true), m + " is never turned into the Yuno variant");
            assertEquals(m, ModeArmorWindV2Rules.wireName(m, false));
        }
    }

    @Test
    void everyMeshIsFiniteWithUnitNormalsUvInsideAndSwayWeightsInRange() {
        for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
            for (Entry e : ModeArmorWindV2Rules.entries(lod)) {
                ModeArmorWindV2Mesh m = ModeArmorWindV2Mesh.get(e.piece(), e.detail());
                assertTrue(m.verts > 0 && m.verts % 4 == 0, e + " is whole quads");
                for (int i = 0; i < m.verts; i++) {
                    for (int k = 0; k < 3; k++) assertTrue(Float.isFinite(m.pos[i * 3 + k]), e + " finite position");
                    float l = (float) Math.sqrt(m.nrm[i * 3] * m.nrm[i * 3] + m.nrm[i * 3 + 1] * m.nrm[i * 3 + 1] + m.nrm[i * 3 + 2] * m.nrm[i * 3 + 2]);
                    assertTrue(l > 0.97f && l < 1.03f, e + " unit normal, was " + l);
                    assertTrue(m.uv[i * 2] >= 0f && m.uv[i * 2] <= 1f && m.uv[i * 2 + 1] >= 0f && m.uv[i * 2 + 1] <= 1f, e + " uv inside the texture");
                    assertTrue(m.sw[i] >= 0f && m.sw[i] <= 1f, e + " sway weight");
                }
                for (int q = 0; q < m.verts; q += 4) {                                                       // the front face looks where the normal looks (the harness warns otherwise)
                    int a = q * 3;
                    float e1x = m.pos[a + 3] - m.pos[a], e1y = m.pos[a + 4] - m.pos[a + 1], e1z = m.pos[a + 5] - m.pos[a + 2];
                    float e2x = m.pos[a + 6] - m.pos[a], e2y = m.pos[a + 7] - m.pos[a + 1], e2z = m.pos[a + 8] - m.pos[a + 2];
                    float g = (e1y * e2z - e1z * e2y) * (m.nrm[a] + m.nrm[a + 6]) + (e1z * e2x - e1x * e2z) * (m.nrm[a + 1] + m.nrm[a + 7]) + (e1x * e2y - e1y * e2x) * (m.nrm[a + 2] + m.nrm[a + 8]);
                    assertTrue(g >= -1e-4f, e + " quad " + q / 4 + " is wound against its normal");
                }
            }
        }
    }

    @Test
    void theMirroredPiecesAreBuiltOnTheLeftAndTheCrownIsSixtyToSixtyFivePercentOfTheOldOne() {
        for (Piece p : new Piece[]{Piece.SLEEVE, Piece.CUFF, Piece.MANTLE, Piece.MANTLE_TRIM, Piece.WING_BIG, Piece.WING_SMALL, Piece.RIBBON_CUFF}) {
            ModeArmorWindV2Mesh m = ModeArmorWindV2Mesh.get(p, 0);
            float sum = 0;
            for (int i = 0; i < m.verts; i++) sum += m.pos[i * 3];
            assertTrue(sum / m.verts > 0.5f, p + " is built for the left (+x) side, mean x " + sum / m.verts);
        }
        float ratioRadius = ModeArmorWindV2Mesh.crownRadius() / ModeArmorWindV2Rules.OLD_CROWN_RADIUS;
        float ratioTall = ModeArmorWindV2Mesh.bladeHeight(0) / ModeArmorWindV2Rules.OLD_CROWN_TALL, ratioShort = ModeArmorWindV2Mesh.bladeHeight(1) / ModeArmorWindV2Rules.OLD_CROWN_SHORT;
        for (float r : new float[]{ratioRadius, ratioTall, ratioShort}) assertTrue(r >= 0.58f && r <= 0.68f, "the crown is 60 to 65 percent of the old one, was " + r);
        assertTrue(ModeArmorWindV2Mesh.bladeHeight(0) > ModeArmorWindV2Mesh.bladeHeight(1), "alternating heights: tall then short");
    }

    @Test
    void theVertexBudgetsHoldAtEveryLevelOfDetail() {
        int full = ModeArmorWindV2Rules.vertices(Lod.FULL), mid = ModeArmorWindV2Rules.vertices(Lod.MID), far = ModeArmorWindV2Rules.vertices(Lod.FAR);
        assertTrue(full <= 3200 && full > 2000, "full " + full);
        assertTrue(mid <= 1200 && mid > 300, "mid " + mid);
        assertTrue(far <= 150 && far > 0, "far " + far);
        assertTrue(full > mid && mid > far, "each level is cheaper than the one before");
        assertEquals(0, ModeArmorWindV2Rules.vertices(Lod.CULLED), "nothing beyond 64 blocks");
        assertEquals(Lod.FULL, ModeArmorWindV2Rules.lod(24.0 * 24.0));
        assertEquals(Lod.MID, ModeArmorWindV2Rules.lod(30.0 * 30.0));
        assertEquals(Lod.FAR, ModeArmorWindV2Rules.lod(60.0 * 60.0));
        assertEquals(Lod.CULLED, ModeArmorWindV2Rules.lod(65.0 * 65.0));
        assertEquals(Lod.MID, ModeArmorWindV2Rules.effective(Lod.FULL, false), "a wearer the frame budget does not admit draws the mid level");
        assertEquals(Lod.FULL, ModeArmorWindV2Rules.effective(Lod.FULL, true));
        assertEquals(Lod.FAR, ModeArmorWindV2Rules.effective(Lod.FAR, false));
        assertEquals(3, ModeArmorWindV2Rules.CHARACTER_PER_FRAME);
        int sum = 0;
        for (ModeArmorWindV2Rules.Pass p : ModeArmorWindV2Rules.Pass.values()) sum += ModeArmorWindV2Rules.vertices(Lod.FULL, p);
        assertEquals(full, sum, "the per-pass counts add up to the total");
    }

    @Test
    void theCombinedCalmYunoDiveStaysUnderTheCap() {
        int aura = SpiritDiveWind4Rules.steadyVertices(true, Lod.FULL, SpiritDivePieces.Stage.FULL, true);
        int without = ModeArmorWindV2Rules.yunoAuraSteadyWithoutWingsAndCrown();
        int combined = ModeArmorWindV2Rules.yunoCombinedSteady();
        assertTrue(without > 0 && without < aura, "the aura loses its wings and its crown: " + aura + " -> " + without);
        assertTrue(combined <= ModeArmorWindV2Rules.CAP_COMBINED_STEADY, "combined " + combined);
        assertEquals(6500, ModeArmorWindV2Rules.CAP_COMBINED_STEADY);
    }

    @Test
    void theFlagshipPiecesAreAllInTheFullLevel() {
        java.util.Set<Piece> drawn = new java.util.HashSet<>();
        for (Entry e : ModeArmorWindV2Rules.entries(Lod.FULL)) drawn.add(e.piece());
        for (Piece p : Piece.values()) assertTrue(drawn.contains(p), p + " is drawn at the full level");
        assertTrue(ModeArmorWindV2Rules.leaves(Lod.FULL) > 0 && ModeArmorWindV2Rules.leaves(Lod.FULL) <= 6, "a capped handful of drifting leaves");
        assertEquals(0, ModeArmorWindV2Rules.leaves(Lod.FAR));
    }
}
