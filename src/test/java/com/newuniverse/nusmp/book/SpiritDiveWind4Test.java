package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules.Ver;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the final Wind A (default of {@code spiritDive.windLook}, a real 3D crown layer, a calm steady state) is pure tables plus four generated mesh files; this test pins what no game is needed for: the
 * default and the option values, the crown meshes (they load, finite, a body, a glow shell, every face on an opaque texel of the atlas, green and white, the bars and rings the owner asked for: 14 and a
 * second ring for Yuno, 8 for the standard tier, the half meshes are the left half), the energy function (loud in the entry, quiet in the steady state, loud again for 2 s after a cast and in the exit,
 * always in 0..1, smooth), the quiet set (no ribbons, no circle, a quarter of the sparkles), the vertex budgets (entry worst under the cap, quiet steady state far below), and the cast clock (bounded slots,
 * per diver, no growth).
 */
class SpiritDiveWind4Test {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    @Test
    void windADefaultsForBothTiersAndTheOlderLooksStayChoosable() {
        assertEquals(SpiritDiveWindRules.WindLook.A_ANIME, SpiritDiveWindRules.DEFAULT_LOOK);
        assertEquals(Ver.A, SpiritDiveWind3Rules.ver(SpiritDiveWindRules.parse("garbage")));
        assertEquals("[PACK, REEL, A_ANIME, B_CLIP, C_SAINT]", java.util.Arrays.toString(SpiritDiveWindRules.WindLook.values()), "append only; B, C, PACK and REEL stay selectable");
        for (String s : new String[]{"pack", "reel", "b_clip", "c_saint", "a_anime"}) assertEquals(s, SpiritDiveWindRules.parse(s).name().toLowerCase());
    }

    @Test
    void theCrownMeshesLoadAndAreWhatWasAskedFor() throws IOException {
        BufferedImage skin = ImageIO.read(ASSETS.resolve("textures/" + SpiritDiveWind3Rules.SKIN + ".png").toFile());
        for (String n : SpiritDiveWind4Rules.NAMES) {
            assertTrue(SpiritDiveWind3Mesh.existsIn(SpiritDiveWind3Mesh.FOLDER4, n), n + ".txt must exist");
            SpiritDiveWind3Mesh m = SpiritDiveWind3Mesh.getIn(SpiritDiveWind3Mesh.FOLDER4, n);
            assertTrue(m.quads(SpiritDiveWind3Mesh.SOLID) >= 20 && m.quads(SpiritDiveWind3Mesh.GLOW) >= 8, n + " has a body and a glow shell");
            assertTrue(m.radius > 5f && m.radius < 25f, n + " radius " + m.radius);
            for (int l = 0; l < SpiritDiveWind3Mesh.LAYERS; l++) {
                for (float f : m.pos[l]) assertTrue(!Float.isNaN(f) && !Float.isInfinite(f), n + " finite");
                for (int q = 0; q < m.quads(l); q++) {
                    float u = 0, v = 0;
                    for (int k = 0; k < 4; k++) { u += m.uv[l][q * 8 + k * 2] / 4f; v += m.uv[l][q * 8 + k * 2 + 1] / 4f; }
                    assertTrue((skin.getRGB(Math.min(63, (int) (u * 64f)), Math.min(63, (int) (v * 64f))) >>> 24) >= 250, n + " layer " + l + " quad " + q + " on a transparent texel");
                    int c = m.tint[l][q];
                    assertTrue((c >> 8 & 255) >= (c & 255), n + " tint has blue over green");
                }
            }
        }
        SpiritDiveWind3Mesh y = SpiritDiveWind4Rules.crown(true, false), s = SpiritDiveWind4Rules.crown(false, false);
        assertTrue(y.quads(SpiritDiveWind3Mesh.ORNAMENT) > 0, "Yuno has the lower second ring");
        assertEquals(0, s.quads(SpiritDiveWind3Mesh.ORNAMENT), "the standard tier has one ring");
        // 24 vertices a bar (6 quads), the ring 2 quads a segment: Yuno 14 bars + 12 segments, standard 8 bars + 12 segments
        assertEquals(14 * 24 + 12 * 8, y.verts(SpiritDiveWind3Mesh.SOLID), "14 bars and the ring");
        assertEquals(8 * 24 + 12 * 8, s.verts(SpiritDiveWind3Mesh.SOLID), "8 bars and the ring");
        assertTrue(SpiritDiveWind4Rules.crownVerts(true, false) <= 800 && SpiritDiveWind4Rules.crownVerts(false, false) <= 500, "crown budgets");
        // the halves are the left half (x >= 0) and about half the size
        for (boolean ch : new boolean[]{true, false}) {
            SpiritDiveWind3Mesh h = SpiritDiveWind4Rules.crown(ch, true), f = SpiritDiveWind4Rules.crown(ch, false);
            for (int l = 0; l < 3; l++) for (int i = 0; i < h.pos[l].length; i += 3) assertTrue(h.pos[l][i] > -1.6f, "the half crown stays on the left side");
            assertTrue(h.totalVerts() < f.totalVerts() * 0.62f && h.totalVerts() > f.totalVerts() * 0.35f, "half is about half: " + h.totalVerts() + " of " + f.totalVerts());
        }
        // the crown floats a hand's width (about 4 px) above the hat layer (-8.5 px; y is down) and bars stand taller than the sprite crown (7.6 px)
        assertTrue(SpiritDiveWind4Rules.HOVER <= -12f && SpiritDiveWind4Rules.HOVER >= -14f);
        assertTrue(y.radius > 12f, "the tall bars of the Yuno crown");
    }

    @Test
    void theEnergyIsLoudInTheEntryQuietInTheSteadyStateAndLoudAfterACast() {
        float never = 1e9f;
        for (float t = 0; t < 22f; t += 1f) assertEquals(1f, SpiritDiveWind4Rules.energy(t, 100f, never), 1e-4f, "entry t=" + t);
        assertTrue(SpiritDiveWind4Rules.energy(30f, 100f, never) < 1f && SpiritDiveWind4Rules.energy(30f, 100f, never) > 0f, "settling");
        assertEquals(0f, SpiritDiveWind4Rules.energy(40f, 100f, never), 1e-4f, "quiet from t = 22 + 12");
        assertEquals(0f, SpiritDiveWind4Rules.energy(500f, 5000f, never), 1e-4f);
        // after a cast: held 40 ticks, fading over 20 more
        assertEquals(1f, SpiritDiveWind4Rules.energy(200f, 5000f, 0f), 1e-4f);
        assertEquals(1f, SpiritDiveWind4Rules.energy(200f, 5000f, SpiritDiveWind4Rules.CAST_HOLD), 1e-4f);
        float mid = SpiritDiveWind4Rules.energy(200f, 5000f, SpiritDiveWind4Rules.CAST_HOLD + SpiritDiveWind4Rules.CAST_FADE * 0.5f);
        assertTrue(mid > 0.1f && mid < 0.9f);
        assertEquals(0f, SpiritDiveWind4Rules.energy(200f, 5000f, SpiritDiveWind4Rules.CAST_HOLD + SpiritDiveWind4Rules.CAST_FADE + 1f), 1e-4f);
        // the exit: loud again as the clock runs back
        assertEquals(0f, SpiritDiveWind4Rules.energy(200f, SpiritDiveReelRules.EXIT_TICKS, never), 1e-4f, "the exit starts quiet");
        assertEquals(1f, SpiritDiveWind4Rules.energy(200f, 2f, never), 1e-4f, "the exit is loud");
        // always 0..1 and never a jump bigger than a smooth ramp over the whole window
        float prev = SpiritDiveWind4Rules.energy(0f, 200f, never);
        for (float t = 0; t < 200f; t += 0.5f) {
            float e = SpiritDiveWind4Rules.energy(t, 200f - t, never);
            assertTrue(e >= 0f && e <= 1f, "range at " + t);
            assertTrue(Math.abs(e - prev) < 0.2f, "smooth at " + t);
            prev = e;
        }
    }

    @Test
    void theQuietSetCutsTheLoudEffectsByAtLeastSeventyPercent() {
        for (boolean ch : new boolean[]{true, false}) {
            SpiritDiveWindRules.Spec loud = SpiritDiveWindRules.spec(ch, Lod.FULL), calm = SpiritDiveWind4Rules.calmSpec(loud);
            assertEquals(0, calm.ribbons(), "no steady ribbons");
            assertTrue(!calm.backCircle(), "no circle behind the back");
            assertTrue(calm.sparks() >= 1 && calm.sparks() * 100 <= loud.sparks() * 30, "sparkles cut by at least 70 percent: " + loud.sparks() + " -> " + calm.sparks());
            assertEquals(loud.leafSegs(), calm.leafSegs(), "the wings stay");
            assertEquals(loud.wrists(), calm.wrists(), "the wrist circle stays");
            assertEquals(loud.weapon(), calm.weapon());
            assertEquals(loud.shellPasses(), calm.shellPasses(), "the body light stays");
            int loudLoud = loud.ribbons() * loud.ribbonSegs() * 4 + loud.sparks() * 4 + (loud.backCircle() ? 8 : 0);
            int calmLoud = calm.sparks() * 4;
            assertTrue(calmLoud * 100 <= loudLoud * 30, "the loud vertices of the steady state are cut by at least 70 percent: " + loudLoud + " -> " + calmLoud);
        }
    }

    @Test
    void theBudgetsHoldAtEveryTierStageAndDetail() {
        for (boolean ch : new boolean[]{true, false})
            for (Stage st : Stage.values()) {
                int prev = Integer.MAX_VALUE;
                for (Lod lod : new Lod[]{Lod.FULL, Lod.MID, Lod.FAR}) {
                    int worst = SpiritDiveWind3Rules.vertices(Ver.A, ch, lod, st), calm = SpiritDiveWind4Rules.steadyVertices(ch, lod, st, true), loud = SpiritDiveWind4Rules.steadyVertices(ch, lod, st, false);
                    assertTrue(worst <= SpiritDiveWind3Rules.cap(Ver.A, ch), "entry worst under the cap " + ch + " " + st + " " + lod + ": " + worst);
                    assertTrue(calm <= loud && loud <= worst, "quiet <= loud steady <= entry worst: " + calm + " " + loud + " " + worst);
                    assertTrue(worst <= prev, "level of detail order");
                    prev = worst;
                }
                assertEquals(0, SpiritDiveWind4Rules.steadyVertices(ch, Lod.CULLED, st, true));
            }
        // the quiet steady state of a full dive is far under the caps: at most three Yuno dives a frame (SpiritDiveFrame.admitCharacter)
        assertTrue(SpiritDiveWind4Rules.worstSteady(true, true) <= 2800, "Yuno quiet steady " + SpiritDiveWind4Rules.worstSteady(true, true));
        assertTrue(SpiritDiveWind4Rules.worstSteady(false, true) <= 1300, "standard quiet steady " + SpiritDiveWind4Rules.worstSteady(false, true));
        assertTrue(SpiritDiveWind3Rules.CAP_CHARACTER_A <= 4600 && SpiritDiveWind3Rules.cap(Ver.B, true) == SpiritDiveWind3Rules.CAP_CHARACTER, "B and C keep their caps");
        // the 3.5 numbers the harness measured at t = 50 (sprite crown, loud): Yuno 2596 and standard 944, within the formula's few vertices of rounding
        int yuno35 = SpiritDiveWind4Rules.worstSteady(true, false) - SpiritDiveWind4Rules.crownDelta(true, Lod.FULL, Stage.FULL), std35 = SpiritDiveWind4Rules.worstSteady(false, false) - SpiritDiveWind4Rules.crownDelta(false, Lod.FULL, Stage.FULL);
        assertTrue(Math.abs(yuno35 - 2596) <= 16 && Math.abs(std35 - 944) <= 16, "3.5 steady " + yuno35 + " / " + std35);
    }

    @Test
    void theCastClockIsPerDiverBoundedAndForgetsNothingItShouldKeep() {
        SpiritDiveWind4Rules.CastClock c = new SpiritDiveWind4Rules.CastClock();
        assertTrue(c.since(7, 100f, false) > 1e8f, "never cast");
        assertEquals(0f, c.since(7, 100f, true), 1e-4f);
        assertEquals(30f, c.since(7, 130f, false), 1e-4f);
        assertTrue(c.since(8, 130f, false) > 1e8f, "another diver is separate");
        for (int i = 100; i < 140; i++) c.since(i, 130f, false);                               // more divers than slots: the oldest are recycled, no growth, no exception
        assertEquals(0f, c.since(7, 131f, true), 1e-4f);
        assertEquals(5f, c.since(7, 136f, false), 1e-4f);
        assertTrue(c.since(7, 10f, false) > 1e8f, "a clock that went backwards (respawn) forgets the cast");
        assertEquals(16, SpiritDiveWind4Rules.CastClock.SLOTS);
    }

    @Test
    void theGeneratorAndTheToolsAreInTheRepo() throws IOException {
        for (String f : new String[]{"tools/gen_sdw4_models.py", "tools/spirit_dive_wind_final_scenes.py", "tools/spirit_dive_wind_final_sheets.py", "tools/render_preview/scenes.d/spirit_dive_wind_final.json",
                "tools/render_preview/examples/SdwV4A.java", "tools/render_preview/examples/SdwV4Loud.java", "docs/magic/spirit_dives_wind_final.md"})
            assertTrue(Files.exists(Path.of("").toAbsolutePath().resolve(f)), f + " must exist");
        List<String> miss = new ArrayList<>();
        for (String n : SpiritDiveWind4Rules.NAMES) if (!Files.exists(ASSETS.resolve("sdw4_meshes/" + n + ".txt"))) miss.add(n);
        assertTrue(miss.isEmpty(), "missing meshes " + miss);
    }
}
