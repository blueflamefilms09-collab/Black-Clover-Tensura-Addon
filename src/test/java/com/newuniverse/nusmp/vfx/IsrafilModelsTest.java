package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.vfx.client.layer.IsrafilModels;
import com.newuniverse.nusmp.vfx.client.layer.IsrafilModels.Mesh;
import com.newuniverse.nusmp.vfx.client.layer.IsrafilModels.Posed;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: 3.21.0 draws the Israfil ultimate (Lord of Trials) with real 3D models (the Grand Trumpet, the weight plate, the maze walls and pillars, the reality shards, the dark core, the coordinate gates) inside
 * {@code TrialLayer}. They are pure geometry ({@code IsrafilModels}), posed and drawn without allocation ({@code IsrafilMeshDraw}) and switched by the CLIENT setting {@code visual.israfilModels}. This test guards the
 * rule-11 limits (every model keeps at most its quad constant from any view, the atlas is real and sized right, nothing is built per frame, a model that does not fit the budget is skipped whole), the "keep the old"
 * rule (every 3.20.0 sprite and its draw call is still there as the fallback), the switch (default on, registered once, a CLIENT config), the new sample scenes and the doc. Pure: runs the real mesh code and reads the real
 * sources and PNGs, the working directory is the project root.
 */
class IsrafilModelsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String read0(Path p) { try { return read(p); } catch (IOException e) { throw new IllegalStateException(e); } }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static final Mesh[] ALL = {IsrafilModels.HORN, IsrafilModels.PLATE, IsrafilModels.WALL_A, IsrafilModels.WALL_B, IsrafilModels.PILLAR, IsrafilModels.SHARD, IsrafilModels.CORE, IsrafilModels.GATE};

    // ---------------------------------------------------------------- the meshes
    @Test
    void everyMeshHasFacesInsideTheAtlasAndTheLimitsAreTheDocumentedOnes() {
        assertEquals(68, IsrafilModels.HORN_MAX);
        assertEquals(36, IsrafilModels.PLATE_MAX);
        assertEquals(8, IsrafilModels.WALL_MAX);
        assertEquals(10, IsrafilModels.PILLAR_MAX);
        assertEquals(3, IsrafilModels.SHARD_MAX);
        assertEquals(12, IsrafilModels.CORE_MAX);
        assertEquals(14, IsrafilModels.GATE_MAX);
        int[] limits = {IsrafilModels.HORN_MAX, IsrafilModels.PLATE_MAX, IsrafilModels.WALL_MAX, IsrafilModels.WALL_MAX, IsrafilModels.PILLAR_MAX, IsrafilModels.SHARD_MAX, IsrafilModels.CORE_MAX, IsrafilModels.GATE_MAX};
        for (int m = 0; m < ALL.length; m++) {
            Mesh mesh = ALL[m];
            assertEquals(limits[m], mesh.maxDrawn, mesh.name + " carries its quad limit");
            assertTrue(mesh.faces > 0 && mesh.faces <= Posed.MAX_FACES, mesh.name + " fits the face arrays of the poser: " + mesh.faces);
            assertTrue(mesh.parts >= 1 && mesh.parts <= Posed.MAX_PARTS, mesh.name + " fits the part arrays: " + mesh.parts);
            for (int f = 0; f < mesh.faces; f++) {
                int cell = mesh.cellOf(f);
                assertTrue(cell >= 0 && cell < 16, mesh.name + " face " + f + " uses a cell of the 4 x 4 atlas: " + cell);
                float[] r = mesh.rectOf(f);
                assertTrue(Math.min(r[0], r[2]) >= 0f && Math.min(r[1], r[3]) >= 0f && Math.max(r[0], r[2]) <= 1f && Math.max(r[1], r[3]) <= 1f && r[0] != r[2] && r[1] != r[3], mesh.name + " face " + f + " rectangle inside its cell (a mirrored one runs backwards)");
                for (float c : mesh.corners(f)) assertFalse(Float.isNaN(c) || Float.isInfinite(c), mesh.name + " face " + f + " corner is a number");
            }
        }
        assertTrue(IsrafilModels.HORN.parts >= 1, "the horn is posed as a model");
        assertEquals(2, IsrafilModels.PLATE.parts, "the plate has a slab and a turning rune ring");
        assertTrue(IsrafilModels.HORN_MOUTH_Z > 1f && IsrafilModels.PLATE_APEX > 0f, "the mouth of the horn and the apex of the plate are known to the layer");
        // the biggest model of an effect plus the flat parts it keeps stays under the 100-quad activation cap
        assertTrue(IsrafilModels.PLATE_MAX + 12 <= 100 && IsrafilModels.CORE_MAX + 6 <= 100 && IsrafilModels.GATE_MAX * 2 + 12 <= 100, "the plate, the core and the two gates leave room");
        assertTrue(Pattern.compile("HORN_KEEP = 44").matcher(read0(LAYER_DIR.resolve("TrialLayer.java"))).find(), "the horn keeps 44 quads of the budget free for the rays and the rings");
    }

    @Test
    void noViewKeepsMoreQuadsThanTheModelsLimit() {
        Random r = new Random(20261);   // views from 1.5 to 15 blocks
        for (Mesh mesh : ALL) {
            Posed p = new Posed();
            int worst = 0;
            for (int k = 0; k < 3000; k++) {
                p.clearPose();
                if (mesh == IsrafilModels.PLATE) p.spread()[IsrafilModels.P_RING] = r.nextFloat() * 6.28f;
                switch (k % 3) {
                    case 0 -> p.look((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), r.nextFloat() * 6.28f, 0.5f + 1.5f * r.nextFloat());
                    case 1 -> p.tumble((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), r.nextFloat() * 6.28f, 0.5f + r.nextFloat(), 0.5f + r.nextFloat(), 0.5f + r.nextFloat());
                    default -> p.standing((float) r.nextGaussian(), (float) r.nextGaussian(), 0.8f + r.nextFloat(), 0.8f + r.nextFloat(), 0.8f + r.nextFloat());
                }
                float d = 1.5f + r.nextFloat() * 14f, a1 = r.nextFloat() * 6.28f, a2 = r.nextFloat() * 3.14f - 1.57f;
                int n = p.prepare(mesh, (float) (Math.cos(a1) * Math.cos(a2) * d), (float) (Math.sin(a2) * d), (float) (Math.sin(a1) * Math.cos(a2) * d), 0xFFFFFFFF, r.nextFloat());
                worst = Math.max(worst, n);
                assertTrue(n <= mesh.maxDrawn, mesh.name + " kept " + n + " quads from one view, the limit is " + mesh.maxDrawn);
            }
            assertTrue(worst >= mesh.maxDrawn / 3, mesh.name + " limit " + mesh.maxDrawn + " is not wildly above what a view keeps (" + worst + ")");
        }
    }

    @Test
    void preparedFacesHaveUvInsideTheirCellAndAreSortedFarToNear() {
        Random r = new Random(7);
        for (Mesh mesh : ALL) {
            Posed p = new Posed();
            for (int k = 0; k < 40; k++) {
                p.clearPose();
                p.look((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), 0f, 1f, 0f, r.nextFloat() * 6.28f, 1f);
                int n = p.prepare(mesh, 4f + r.nextFloat() * 4f, r.nextFloat() * 3f - 1.5f, -6f - r.nextFloat() * 4f, 0x80FFFFFF, 0.5f);
                float last = Float.MAX_VALUE;
                for (int q = 0; q < n; q++) {
                    int i = p.at(q);
                    assertTrue(i >= 0 && i < n, "an index inside the kept faces");
                    float u0 = p.uv[i * 4], v0 = p.uv[i * 4 + 1], u1 = p.uv[i * 4 + 2], v1 = p.uv[i * 4 + 3];
                    for (float v : new float[]{u0, v0, u1, v1}) assertTrue(v >= 0f && v <= 1f, mesh.name + " uv inside 0..1: " + v);
                    assertEquals(0x80, p.colour[i] >>> 24, "the opacity of the tint is kept");
                    float cx = 0, cy = 0, cz = 0;
                    for (int c = 0; c < 4; c++) { cx += p.pts[i * 12 + c * 3]; cy += p.pts[i * 12 + c * 3 + 1]; cz += p.pts[i * 12 + c * 3 + 2]; }
                    float dist = (cx * cx + cy * cy + cz * cz) / 16f;
                    assertTrue(dist <= last + 1e-3f, mesh.name + " is drawn far to near (the buffer has no depth write)");
                    last = dist;
                }
            }
        }
    }

    @Test
    void theRuneRingOfThePlateTurnsOnItsOwn() {
        Posed p = new Posed();
        p.clearPose();
        p.upright(0f, 0f, 0f, 0f, 1f);
        int n0 = p.prepare(IsrafilModels.PLATE, 0f, 0f, -6f, 0xFFFFFFFF, 0f);
        float[] a = java.util.Arrays.copyOf(p.pts, n0 * 12);
        p.spread()[IsrafilModels.P_RING] = 0.8f;
        p.prepare(IsrafilModels.PLATE, 0f, 0f, -6f, 0xFFFFFFFF, 0f);
        boolean moved = false;
        for (int i = 0; i < Math.min(a.length, n0 * 12); i++) if (Math.abs(a[i] - p.pts[i]) > 1e-3f) { moved = true; break; }
        assertTrue(moved, "turning the ring part moves faces of the plate");
        p.clearPose();
        assertEquals(0f, p.spread()[IsrafilModels.P_RING], "clearPose resets the parts");
    }

    // ---------------------------------------------------------------- the switch
    @Test
    void theSwitchDefaultsToOnAndIsAClientConfigRegisteredOnce() throws IOException {
        assertTrue(IsrafilModels.enabled(), "the 3D models are on by default");
        IsrafilModels.setEnabled(false);
        assertFalse(IsrafilModels.enabled());
        IsrafilModels.setEnabled(true);
        assertTrue(IsrafilModels.enabled());
        String cfg = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/IsrafilModelsConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT"), "it only changes how things look: a CLIENT config");
        assertTrue(cfg.contains("define(\"israfilModels\", true)"), "default on");
        assertTrue(cfg.contains("\"nusmp-israfil-client.toml\""), "its own file name");
        assertTrue(cfg.contains("IsrafilModels.setEnabled("), "it is mirrored into the pure switch");
        String main = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        Matcher m = Pattern.compile("IsrafilModelsConfig\\.init\\(modEventBus, modContainer\\)").matcher(main);
        assertTrue(m.find(), "NUSMP registers the config");
        assertFalse(m.find(), "and only once");
    }

    // ---------------------------------------------------------------- the textures
    @Test
    void theAtlasIsARealSizedTexture() throws IOException {
        BufferedImage atlas = ImageIO.read(TEX.resolve("israfil_model_atlas.png").toFile());
        assertEquals(512, atlas.getWidth());
        assertEquals(512, atlas.getHeight());
        int cell = 128;
        List<Integer> cutOut = List.of(IsrafilModels.MOUTH, IsrafilModels.CHAIN, IsrafilModels.CRYSTAL_A, IsrafilModels.CRYSTAL_B, IsrafilModels.CORE_CELL, IsrafilModels.FEATHER);
        for (int c = 0; c < 16; c++) {
            long opaque = 0, lit = 0;
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) {
                    int argb = atlas.getRGB((c % 4) * cell + x, (c / 4) * cell + y);
                    int al = argb >>> 24;
                    if (al > 200) opaque++;
                    if (al > 0) lit++;
                    else assertEquals(0, argb & 0xFFFFFF, "no colour under alpha 0 in cell " + c + " (it bleeds into the mipmaps)");
                }
            }
            assertTrue(lit > 3000, "cell " + c + " has content: " + lit);
            if (!cutOut.contains(c)) assertEquals(cell * cell, opaque, "cell " + c + " is a solid material");
            else assertTrue(opaque > 1000, "cut-out cell " + c + " has solid parts: " + opaque);
        }
    }

    // ---------------------------------------------------------------- the code
    @Test
    void nothingIsBuiltPerFrameAndTheMeshCodeStaysPure() throws IOException {
        String models = code(LAYER_DIR.resolve("IsrafilModels.java"));
        assertFalse(models.contains("net.minecraft") || models.contains("com.mojang") || models.contains("Minecraft.getInstance"), "the mesh code is pure geometry");
        assertFalse(models.contains("java.util.ArrayList") || models.contains("java.util.HashMap"), "no collections in the mesh code");
        int a = models.indexOf("public int prepare(");
        int b = models.indexOf("public int at(");
        assertTrue(a > 0 && b > a, "prepare is found");
        assertFalse(models.substring(a, b).contains("new "), "Posed.prepare allocates nothing");
        for (String m : List.of("public Posed look(", "public Posed tumble(", "public Posed upright(", "public Posed axes(", "public Posed standing(")) {
            int s = models.indexOf(m);
            assertTrue(s > 0, m);
            int e = models.indexOf("\n        }", s);
            assertFalse(models.substring(s, e).contains("new "), m + " allocates nothing");
        }
        String draw = code(LAYER_DIR.resolve("IsrafilMeshDraw.java"));
        String body = draw.substring(draw.indexOf("public int draw(VfxVertexBuffer buf, ResourceLocation atlas, IsrafilModels.Mesh mesh, float ox, float oy, float oz, int tint, float heat, int reserve)"));
        assertFalse(body.contains("new "), "IsrafilMeshDraw.draw allocates nothing");
        assertTrue(draw.contains("buf.hasBudget((n + reserve) * 4)"), "a model that does not fit the activation budget (and the room kept for the rays) is skipped whole");
        assertTrue(draw.contains("return 0"), "and reports 0 so the layer draws the flat sprite");
        assertFalse(draw.contains("Minecraft.getInstance") || draw.contains("com.mojang.blaze3d"), "no client singletons");
        String layer = code(LAYER_DIR.resolve("TrialLayer.java"));
        assertFalse(layer.contains("new IsrafilModels") || layer.contains("new Posed"), "the layer never builds a poser per frame");
        assertEquals(1, Pattern.compile("new IsrafilMeshDraw\\(\\)").matcher(layer).results().count(), "one drawer per layer");
        assertEquals(0, Pattern.compile("new float\\[[^8]").matcher(layer).results().count(), "no arrays built in the layer beyond the eight-slot maze fields");
    }

    @Test
    void everyModelIsBehindTheSwitchTheDistanceAndTheBudgetAndKeepsItsOldSprite() throws IOException {
        String src = read(LAYER_DIR.resolve("TrialLayer.java"));
        String layer = code(LAYER_DIR.resolve("TrialLayer.java"));
        assertTrue(layer.contains("IsrafilModels.enabled() && ctx.detail >= 0.5f"), "the setting and the particle setting gate the models");
        assertTrue(layer.contains("x * x + y * y + z * z < range * range"), "and the camera distance");
        assertTrue(Pattern.compile("HORN_RANGE = 80f, PLATE_RANGE = 52f, MAZE_RANGE = 60f, CORE_RANGE = 64f, SHARD_RANGE = 44f, GATE_RANGE = 44f").matcher(src).find(), "the ranges are constants");
        for (String model : List.of("IsrafilModels.HORN", "IsrafilModels.PLATE", "IsrafilModels.WALL_A", "IsrafilModels.WALL_B", "IsrafilModels.PILLAR", "IsrafilModels.SHARD", "IsrafilModels.CORE", "IsrafilModels.GATE"))
            assertTrue(layer.contains(model), "the layer draws " + model);
        // the old 3.20.0 draw calls are all still in the code as the fallback
        for (String old : List.of("sprite(buf, HORN, VfxBlend.ADD", "buf.plane(GRID, VfxBlend.ADD, plate.spin(", "buf.plane(VOID, VfxBlend.ALPHA", "buf.billboard(ctx, VOID, VfxBlend.ALPHA", "shard(ctx, buf,", "buf.plane(GRID, VfxBlend.ADD, flipped(hoopA", "buf.plane(GRID, VfxBlend.ADD, flipped(floorA", "quad(buf, PANE, VfxBlend.ALPHA"))
            assertTrue(layer.contains(old), "the 3.20.0 draw call is kept: " + old);
        for (String n : List.of("trial_void", "israfil_model_atlas")) {
            assertTrue(src.contains("\"" + n + "\""), "the layer still names " + n);
            assertTrue(Files.size(TEX.resolve(n + ".png")) > 200, n + ".png exists and is not empty");
        }
        // the flat parts are only skipped when the model really was drawn
        assertTrue(layer.contains("if (!horn3d && hornA > 0.01f)"), "the flat horn is the fallback of the 3D horn");
        assertTrue(layer.contains("plate3d == 0"), "the flat plate is the fallback of the 3D plate");
    }

    // ---------------------------------------------------------------- the owner's rules
    @Test
    void theTimeMagicLookIsUntouchedAndTheGeneratorIsItsOwnFile() throws IOException {
        for (String f : List.of("IsrafilModels.java", "IsrafilMeshDraw.java"))
            assertFalse(read(LAYER_DIR.resolve(f)).contains("time_"), f + " never names a Time Magic texture");
        String gen = read(ROOT.resolve("tools/gen_israfil_model_textures.py"));
        assertFalse(gen.contains("time_"), "the generator never writes a Time Magic texture");
        assertTrue(gen.contains("israfil_model_atlas"), "it writes the atlas");
    }

    @Test
    void theNewPreviewScenesAndTheDocExist() throws IOException {
        String scenes = read(ROOT.resolve("tools/vfx_preview/scenes.d/israfil_models.json"));
        for (String s : List.of("israfil_trumpet_close", "israfil_trumpet_break", "israfil_trumpet_view", "israfil_plate_close", "israfil_plate_view", "israfil_maze_close", "israfil_maze_view",
                "israfil_shards_close", "israfil_core_close", "israfil_collapse_view", "israfil_gate_close", "israfil_gate_view"))
            assertTrue(scenes.contains("\"" + s + "\""), "a preview scene: " + s);
        assertTrue(read(ROOT.resolve("tools/vfx_preview/scenes.d/trial.json")).contains("\"trial_trumpet\""), "the old scenes are kept");
        String doc = read(ROOT.resolve("docs/vfx_remake/israfil_models.md"));
        for (String s : List.of("HORN", "PLATE", "WALL", "PILLAR", "SHARD", "CORE", "GATE", "israfilModels", "NEW", "REPLACED", "KEPT", "worst"))
            assertTrue(doc.toLowerCase().contains(s.toLowerCase()), "the doc covers " + s);
        String calls = read(ROOT.resolve("docs/vfx_remake/trial_fx_calls.md"));
        assertTrue(calls.contains("3.21.0") && calls.contains("IsrafilModels"), "the call doc has the 3.21.0 section");
    }
}
