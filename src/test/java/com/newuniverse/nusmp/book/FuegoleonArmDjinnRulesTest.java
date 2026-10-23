package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.Cap;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.Lod;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.Look;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.State;
import com.newuniverse.nusmp.book.FuegoleonArmRules.View;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Fuegoleon's flame arm in the Djinn style is only ever seen on a client, so what can be pinned is pinned here: (1) the pure choices (which look, which strength, how many tongues and sparks at each
 * level of detail, the per-frame cap, the forge-in numbers); (2) the budget, by cube sums over the real model files, held under the Djinn arm assembly the owner called the standard (pauldron +
 * vambrace + gauntlet of a unique set, counted from its mesh files); (3) the assets (five piece models with their clip, textures, glow maps with nothing under alpha 0, the tongue sheet).
 */
class FuegoleonArmDjinnRulesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/nusmp");

    private static String read(Path p) {
        try { return Files.readString(p, StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + p, e); }
    }

    // ------------------------------------------------------------------ which look
    @Test
    void theDjinnLookIsTheDefaultAndTheOldLooksAreKept() {
        assertEquals(Look.DJINN, FuegoleonArmDjinnRules.DEFAULT_LOOK);
        assertEquals(Look.DJINN, FuegoleonArmDjinnRules.pick(null));
        assertEquals(List.of(Look.DJINN, Look.V3, Look.CLASSIC), List.of(Look.values()), "the look names and their order are the config values");
        for (Look l : Look.values()) assertEquals(l, FuegoleonArmDjinnRules.pick(l));
    }

    @Test
    void theLookIsParsedFromTextAnyCaseAndTheDefaultCatchesTheRest() {
        assertEquals(Look.V3, FuegoleonArmDjinnRules.parse("v3"));
        assertEquals(Look.CLASSIC, FuegoleonArmDjinnRules.parse(" Classic "));
        assertEquals(Look.DJINN, FuegoleonArmDjinnRules.parse("djinn"));
        for (String bad : new String[]{null, "", "plates", "V4", "3.0.0"}) assertEquals(Look.DJINN, FuegoleonArmDjinnRules.parse(bad), String.valueOf(bad));
    }

    @Test
    void onlyTheDjinnLookDrawsTheLimbAndOnlyV3DrawsTheSheetsAndTheMane() {
        assertTrue(FuegoleonArmDjinnRules.drawsLimb(Look.DJINN));
        assertFalse(FuegoleonArmDjinnRules.drawsLimb(Look.V3));
        assertFalse(FuegoleonArmDjinnRules.drawsLimb(Look.CLASSIC));
        assertTrue(FuegoleonArmDjinnRules.drawsSheets(Look.V3));
        assertFalse(FuegoleonArmDjinnRules.drawsSheets(Look.CLASSIC), "CLASSIC is the molten model alone");
        assertFalse(FuegoleonArmDjinnRules.drawsSheets(Look.DJINN));
        assertTrue(FuegoleonArmDjinnRules.drawsLimb(null), "an unset look is the default");
    }

    // ------------------------------------------------------------------ the strengths
    @Test
    void theStateIsQuietAtRestOutWithTheSalamanderAndFlareWhileCastingOrDiving() {
        assertEquals(State.QUIET, FuegoleonArmDjinnRules.state(false, false, false));
        assertEquals(State.OUT, FuegoleonArmDjinnRules.state(true, false, false));
        assertEquals(State.FLARE, FuegoleonArmDjinnRules.state(false, true, false));
        assertEquals(State.FLARE, FuegoleonArmDjinnRules.state(false, false, true));
        assertEquals(State.FLARE, FuegoleonArmDjinnRules.state(true, true, true));
    }

    @Test
    void theViewDecidesWhetherAnythingIsDrawn() {
        for (State s : State.values()) {
            assertEquals(0f, FuegoleonArmDjinnRules.visible(View.OFF, s), 0f);
            assertEquals(1f, FuegoleonArmDjinnRules.visible(View.ALWAYS, s), 0f);
        }
        assertEquals(0f, FuegoleonArmDjinnRules.visible(View.FLARE, State.QUIET), 0f, "FLARE: not at rest");
        assertEquals(1f, FuegoleonArmDjinnRules.visible(View.FLARE, State.OUT), 0f);
        assertEquals(1f, FuegoleonArmDjinnRules.visible(View.FLARE, State.FLARE), 0f);
        assertEquals(0f, FuegoleonArmDjinnRules.visible(null, State.FLARE), 0f);
    }

    @Test
    void everyNumberGrowsFromQuietToOutToFlare() {
        State[] order = {State.QUIET, State.OUT, State.FLARE};
        for (int i = 1; i < order.length; i++) {
            State a = order[i - 1], b = order[i];
            assertTrue(FuegoleonArmDjinnRules.glow(b) > FuegoleonArmDjinnRules.glow(a), "glow " + b);
            assertTrue(FuegoleonArmDjinnRules.haze(b) > FuegoleonArmDjinnRules.haze(a), "haze " + b);
            assertTrue(FuegoleonArmDjinnRules.tongueLength(b) > FuegoleonArmDjinnRules.tongueLength(a), "tongue length " + b);
            assertTrue(FuegoleonArmDjinnRules.tongueCount(b) >= FuegoleonArmDjinnRules.tongueCount(a), "tongue count " + b);
            assertTrue(FuegoleonArmDjinnRules.sparkCount(b) > FuegoleonArmDjinnRules.sparkCount(a), "sparks " + b);
            assertTrue(FuegoleonArmDjinnRules.sparkEvery(b) < FuegoleonArmDjinnRules.sparkEvery(a), "spark rate " + b);
        }
        assertTrue(FuegoleonArmDjinnRules.glow(State.FLARE) <= 1.0f && FuegoleonArmDjinnRules.glow(State.QUIET) > 0.3f, "quiet is still lit, flare does not exceed full");
    }

    // ------------------------------------------------------------------ the tongue table
    @Test
    void theTongueTableIsWellFormedAndHeldByThePieces() {
        float[][] t = FuegoleonArmDjinnRules.TONGUE;
        assertEquals(FuegoleonArmDjinnRules.TONGUES, t.length);
        for (int i = 0; i < t.length; i++) {
            assertEquals(13, t[i].length, "row " + i);
            float[] r = t[i];
            float h = r[FuegoleonArmDjinnRules.T_DX] * r[FuegoleonArmDjinnRules.T_DX] + r[FuegoleonArmDjinnRules.T_DY] * r[FuegoleonArmDjinnRules.T_DY] + r[FuegoleonArmDjinnRules.T_DZ] * r[FuegoleonArmDjinnRules.T_DZ];
            assertTrue(h > 0.2f, "row " + i + " has no heading");
            assertTrue(r[FuegoleonArmDjinnRules.T_LEN] > 3f && r[FuegoleonArmDjinnRules.T_LEN] <= 14f, "row " + i + " length");
            assertTrue(r[FuegoleonArmDjinnRules.T_WID] > 2f && r[FuegoleonArmDjinnRules.T_WID] <= 8f, "row " + i + " width");
            int piece = (int) r[FuegoleonArmDjinnRules.T_PIECE];
            assertTrue(piece >= 0 && piece < FuegoleonArmDjinnRules.PIECE_FILES.length, "row " + i + " piece");
            assertTrue(Math.abs(r[FuegoleonArmDjinnRules.T_X]) < 8f && r[FuegoleonArmDjinnRules.T_Y] > -6f && r[FuegoleonArmDjinnRules.T_Y] < 16f && Math.abs(r[FuegoleonArmDjinnRules.T_Z]) < 6f, "row " + i + " is not on the arm");
        }
        for (int p = 0; p < FuegoleonArmDjinnRules.PIECE_FILES.length; p++) {
            boolean held = false;
            for (float[] r : t) held |= (int) r[FuegoleonArmDjinnRules.T_PIECE] == p;
            assertTrue(held, "piece " + p + " carries no tongue");
        }
        assertTrue(t[0][FuegoleonArmDjinnRules.T_LEN] >= 10f, "the first tongue is the big hook off the elbow");
        assertEquals(FuegoleonArmDjinnRules.Piece.ELBOW.ordinal(), (int) t[0][FuegoleonArmDjinnRules.T_PIECE], "the big hooked tongue grows with the elbow");
    }

    @Test
    void thePieceTablesHaveOneRowPerPieceInForgeOrder() {
        int n = FuegoleonArmDjinnRules.Piece.values().length;
        assertEquals(n, FuegoleonArmDjinnRules.PIECE_FILES.length);
        assertEquals(n, FuegoleonArmDjinnRules.PIECE_DELAY.length);
        assertEquals(n, FuegoleonArmDjinnRules.PIECE_FROM_Y.length);
        assertEquals(n, FuegoleonArmDjinnRules.PIECE_CENTRE.length);
        assertEquals("shoulder", FuegoleonArmDjinnRules.PIECE_FILES[0]);
        assertEquals("fist", FuegoleonArmDjinnRules.PIECE_FILES[n - 1]);
        for (int i = 1; i < n; i++) {
            assertTrue(FuegoleonArmDjinnRules.PIECE_DELAY[i] > FuegoleonArmDjinnRules.PIECE_DELAY[i - 1], "the flame kindles from the shoulder down to the fist");
            assertTrue(FuegoleonArmDjinnRules.PIECE_CENTRE[i][1] > FuegoleonArmDjinnRules.PIECE_CENTRE[i - 1][1], "pieces run down the arm (y grows downward)");
        }
        for (float d : FuegoleonArmDjinnRules.PIECE_DELAY) assertTrue(d >= 0f && d <= 12f, "the Djinn rule: staggers of 0 to 12 ticks");
        assertEquals(20f, FuegoleonArmDjinnRules.forgeTotalTicks(), 0f, "the last piece is whole at tick 20, long before the lease's shatter window (the last 18 of 100 ticks)");
    }

    @Test
    void theForgeFlashPeaksWhileAPieceIsForgedAndIsZeroOtherwise() {
        assertEquals(0f, FuegoleonArmDjinnRules.flash(0f), 0f);
        assertEquals(0f, FuegoleonArmDjinnRules.flash(1f), 0f);
        assertEquals(0f, FuegoleonArmDjinnRules.flash(-0.5f), 0f, "a piece shattering out does not flash");
        float peak = 0f;
        for (float a = 0.01f; a < 1f; a += 0.01f) peak = Math.max(peak, FuegoleonArmDjinnRules.flash(a));
        assertEquals(1f, peak, 0.02f);
        assertTrue(FuegoleonArmDjinnRules.flash(0.35f) > FuegoleonArmDjinnRules.flash(0.1f) && FuegoleonArmDjinnRules.flash(0.35f) > FuegoleonArmDjinnRules.flash(0.9f));
    }

    // ------------------------------------------------------------------ level of detail and the per-frame cap
    @Test
    void theLevelOfDetailFollowsTheDistanceAndFarArmsAreSkipped() {
        assertEquals(Lod.FULL, FuegoleonArmDjinnRules.lod(0));
        assertEquals(Lod.FULL, FuegoleonArmDjinnRules.lod(24.0 * 24.0));
        assertEquals(Lod.HALF, FuegoleonArmDjinnRules.lod(24.1 * 24.1));
        assertEquals(Lod.HALF, FuegoleonArmDjinnRules.lod(48.0 * 48.0));
        assertEquals(Lod.CULLED, FuegoleonArmDjinnRules.lod(48.1 * 48.1));
        for (State s : State.values()) {
            assertEquals(0, FuegoleonArmDjinnRules.tonguesAt(s, Lod.CULLED));
            assertEquals(0, FuegoleonArmDjinnRules.sparksAt(s, Lod.CULLED));
            assertEquals(0, FuegoleonArmDjinnRules.sparksAt(s, Lod.HALF), "no sparks beyond the near range");
            assertTrue(FuegoleonArmDjinnRules.tonguesAt(s, Lod.HALF) <= 5 && FuegoleonArmDjinnRules.tonguesAt(s, Lod.HALF) <= FuegoleonArmDjinnRules.tonguesAt(s, Lod.FULL));
            assertTrue(FuegoleonArmDjinnRules.tonguesAt(s, Lod.FULL) <= FuegoleonArmDjinnRules.TONGUES);
            assertTrue(FuegoleonArmDjinnRules.sparksAt(s, Lod.FULL) <= FuegoleonArmDjinnRules.MAX_SPARKS);
        }
        assertTrue(FuegoleonArmDjinnRules.segmentsAt(Lod.HALF) < FuegoleonArmDjinnRules.segmentsAt(Lod.FULL));
        assertEquals(FuegoleonArmDjinnRules.TONGUE_SEGMENTS, FuegoleonArmDjinnRules.segmentsAt(Lod.FULL));
    }

    @Test
    void aFrameAdmitsAtMostFourFullArmsAndAPausedCrowdKeepsItsFourFullOnes() {
        Cap cap = new Cap();
        int full = 0;
        for (int id = 1; id <= 12; id++) if (cap.admit(100L, id)) full++;
        assertEquals(FuegoleonArmDjinnRules.FULL_PER_FRAME, full, "a crowd of twelve draws four full arms");
        assertTrue(cap.admit(101L, 5), "a new frame starts the count over");
        // the clock does not move (the game is paused): the first painter coming round again starts the next frame
        Cap paused = new Cap();
        boolean[] first = new boolean[8], second = new boolean[8];
        for (int id = 1; id <= 8; id++) first[id - 1] = paused.admit(500L, id);
        for (int id = 1; id <= 8; id++) second[id - 1] = paused.admit(500L, id);
        for (int i = 0; i < 8; i++) assertEquals(first[i], second[i], "the same arms stay full while the clock is stopped (arm " + (i + 1) + ")");
        int fullSecond = 0;
        for (boolean b : second) if (b) fullSecond++;
        assertEquals(FuegoleonArmDjinnRules.FULL_PER_FRAME, fullSecond);
    }

    @Test
    void sparkParticlesAreBoundedAndStaggered() {
        assertEquals(4, FuegoleonArmDjinnRules.SPARK_ARMS);
        assertTrue(FuegoleonArmDjinnRules.worstSparksPerSecond() <= 40.0, "at most 40 spark particles a second before the client budget");
        for (State s : State.values()) {
            int hits = 0;
            for (long t = 0; t < 200; t++) if (FuegoleonArmDjinnRules.sparkNow(s, t, 7)) hits++;
            assertEquals(200 / FuegoleonArmDjinnRules.sparkEvery(s), hits, s.name());
        }
        int[] perTick = new int[FuegoleonArmDjinnRules.sparkEvery(State.OUT)];
        for (int id = 0; id < 50; id++) for (long t = 0; t < perTick.length; t++) if (FuegoleonArmDjinnRules.sparkNow(State.OUT, t, id)) perTick[(int) t]++;
        for (int c : perTick) assertEquals(10, c, "a crowd is spread evenly over the period");
    }

    // ------------------------------------------------------------------ the budget, from the real files
    private static int cubes(String piece) {
        Matcher m = Pattern.compile("\"origin\"").matcher(read(ASSETS.resolve("geo/entity/" + FuegoleonArmDjinnRules.GEO_KEY + "_" + piece + ".geo.json")));
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static int quads(String file) {
        int n = 0;
        for (String line : read(ASSETS.resolve("djinn/u_amon/" + file + ".txt")).split("\n")) if (line.startsWith("qn ")) n++;
        return n;
    }

    @Test
    void theLimbStaysUnderTheDjinnArmAssemblyCountedFromItsMeshFiles() {
        int cubes = 0;
        for (String f : FuegoleonArmDjinnRules.PIECE_FILES) cubes += cubes(f);
        assertTrue(cubes > 15 && cubes <= FuegoleonArmDjinnRules.LIMB_CUBES_MAX, "the limb has " + cubes + " cubes (budget " + FuegoleonArmDjinnRules.LIMB_CUBES_MAX + ")");
        int djinn = (quads("pauldron_r") + quads("vambrace_r") + quads("gauntlet_r")) * 4;
        assertEquals(djinn, FuegoleonArmDjinnRules.DJINN_ARM_VERTICES, "the constant is what the mesh files of a unique Djinn set add up to (pauldron, vambrace, gauntlet)");
        int worst = FuegoleonArmDjinnRules.worstVertices(cubes);
        assertTrue(worst <= FuegoleonArmDjinnRules.DJINN_ARM_VERTICES, "one arm at the full level costs " + worst + " vertices, the Djinn arm " + djinn);
        assertEquals(cubes * 48 + FuegoleonArmDjinnRules.worstQuads() * 4, worst);
        assertEquals(10 * 3 + 8 + 1 + 5, FuegoleonArmDjinnRules.worstQuads(), "tongues x segments + sparks + haze + flashes");
    }

    @Test
    void theArmCostsNoMorePacketsThanBefore() {
        assertEquals(FuegoleonArmRules.packetsPerSecond(), FuegoleonArmDjinnRules.packetsPerSecond(), 0.0);
        assertTrue(FuegoleonArmDjinnRules.packetsPerSecond() <= 0.5, "one lease renewal per 2 seconds at most");
        assertEquals(40, FuegoleonArmRules.RENEW_TICKS, "the lease cadence of the server is unchanged");
        assertEquals(100, FuegoleonArmRules.LEASE_TICKS);
    }

    // ------------------------------------------------------------------ the assets
    @Test
    void everyPieceHasItsModelClipTextureAndGlowMap() throws IOException {
        for (String f : FuegoleonArmDjinnRules.PIECE_FILES) {
            String id = FuegoleonArmDjinnRules.GEO_KEY + "_" + f;
            for (String rel : new String[]{"geo/entity/" + id + ".geo.json", "animations/entity/" + id + ".animation.json", "textures/entity/" + id + ".png", "textures/entity/" + id + "_glow.png"})
                assertTrue(Files.isRegularFile(ASSETS.resolve(rel)), rel);
            assertTrue(read(ASSETS.resolve("animations/entity/" + id + ".animation.json")).contains("\"idle\""), id + " has no idle clip (the painter plays it)");
            assertTrue(cubes(f) >= 2, id + " is empty");
        }
        assertTrue(Files.isRegularFile(Path.of("").toAbsolutePath().resolve("tools/gen_fuegoleon_arm_djinn.py")), "the generator is the source of the assets");
    }

    @Test
    void theGlowMapsHaveNothingUnderAlphaZeroAndTheBodyIsSolid() throws IOException {
        for (String f : FuegoleonArmDjinnRules.PIECE_FILES) {
            String id = FuegoleonArmDjinnRules.GEO_KEY + "_" + f;
            BufferedImage glow = ImageIO.read(ASSETS.resolve("textures/entity/" + id + "_glow.png").toFile()), body = ImageIO.read(ASSETS.resolve("textures/entity/" + id + ".png").toFile());
            assertEquals(glow.getWidth(), body.getWidth());
            assertEquals(glow.getHeight(), body.getHeight());
            for (int y = 0; y < glow.getHeight(); y++)
                for (int x = 0; x < glow.getWidth(); x++) {
                    int g = glow.getRGB(x, y);
                    if ((g >>> 24) == 0) assertEquals(0, g & 0xFFFFFF, id + " glow has a colour under alpha 0 at " + x + "," + y);
                }
        }
    }

    @Test
    void theTongueSheetHasFourFramesAndNoDarkFringeUnderTheClearTexels() throws IOException {
        BufferedImage sheet = ImageIO.read(ASSETS.resolve("textures/aura/fad_tongue.png").toFile());
        assertEquals(FuegoleonArmDjinnRules.FRAMES * 48, sheet.getWidth());
        assertEquals(96, sheet.getHeight());
        int solid = 0, clear = 0;
        List<String> dark = new ArrayList<>();
        for (int y = 0; y < sheet.getHeight(); y++)
            for (int x = 0; x < sheet.getWidth(); x++) {
                int p = sheet.getRGB(x, y), a = p >>> 24;
                if (a > 200) solid++;
                if (a == 0) {
                    clear++;
                    if ((p & 0xFFFFFF) == 0) dark.add(x + "," + y);                              // black under a clear texel would blend as a dark fringe at the sprite's edge
                }
            }
        assertTrue(solid > 800 && clear > 800, "the frames have a body and a clear background");
        assertTrue(dark.isEmpty(), "black under alpha 0: " + dark.subList(0, Math.min(5, dark.size())));
        // the four frames differ (a flicker, not four copies)
        int diff = 0;
        for (int y = 0; y < 96; y++) for (int x = 0; x < 48; x++) if (sheet.getRGB(x, y) != sheet.getRGB(48 + x, y)) diff++;
        assertTrue(diff > 150, "frame 0 and frame 1 are alike");
    }
}
