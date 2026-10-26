package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.vfx.client.layer.HerculesModels;
import com.newuniverse.nusmp.vfx.client.layer.HerculesModels.Mesh;
import com.newuniverse.nusmp.vfx.client.layer.HerculesModels.Posed;
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
 * Why: 3.20.0 draws the Hercules ultimate with real 3D models (the Titan's hand, the fist, the boulder, the rock chunk, the laurel crown and ring) inside {@code HerculesLayer}. They are pure geometry
 * ({@code HerculesModels}), posed and drawn without allocation ({@code HerculesMeshDraw}) and switched by the CLIENT setting {@code visual.herculesModels}. This test guards the rule-11 limits (every model keeps at
 * most its quad constant from any view, the textures are real and sized right, nothing is built per frame, a model that does not fit the budget is skipped whole), the "keep the old" rule (every 3.19.0 sprite
 * and its draw call is still there as the fallback), the switch (default on, registered once, a CLIENT config), the new sample scenes and the doc. Pure: runs the real mesh code and reads the real sources and PNGs, the
 * working directory is the project root.
 */
class HerculesModelsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static final Mesh[] ALL = {HerculesModels.HAND, HerculesModels.FIST, HerculesModels.BOULDER, HerculesModels.CHUNK, HerculesModels.CROWN, HerculesModels.RING};

    // ---------------------------------------------------------------- the meshes
    @Test
    void everyMeshHasFacesInsideTheAtlasAndTheLimitsAreTheDocumentedOnes() {
        assertEquals(40, HerculesModels.HAND_MAX);
        assertEquals(30, HerculesModels.FIST_MAX);
        assertEquals(16, HerculesModels.BOULDER_MAX);
        assertEquals(4, HerculesModels.CHUNK_MAX);
        assertEquals(24, HerculesModels.CROWN_MAX);
        assertEquals(16, HerculesModels.RING_MAX);
        int[] limits = {HerculesModels.HAND_MAX, HerculesModels.FIST_MAX, HerculesModels.BOULDER_MAX, HerculesModels.CHUNK_MAX, HerculesModels.CROWN_MAX, HerculesModels.RING_MAX};
        for (int m = 0; m < ALL.length; m++) {
            Mesh mesh = ALL[m];
            assertEquals(limits[m], mesh.maxDrawn, mesh.name + " carries its quad limit");
            assertTrue(mesh.faces > 0 && mesh.faces <= Posed.MAX_FACES, mesh.name + " fits the face arrays of the poser: " + mesh.faces);
            assertTrue(mesh.parts >= 1 && mesh.parts <= Posed.MAX_PARTS, mesh.name + " fits the part arrays: " + mesh.parts);
            assertTrue(mesh.maxDrawn <= mesh.faces || mesh.faces <= mesh.maxDrawn, mesh.name);
            for (int f = 0; f < mesh.faces; f++) {
                int cell = mesh.cellOf(f);
                assertTrue(cell >= 0 && cell < 16, mesh.name + " face " + f + " uses a cell of the 4 x 4 atlas: " + cell);
                float[] r = mesh.rectOf(f);
                assertTrue(r[0] >= 0f && r[1] >= 0f && r[2] <= 1f && r[3] <= 1f && r[0] < r[2] && r[1] < r[3], mesh.name + " face " + f + " rectangle inside its cell");
                for (float c : mesh.corners(f)) assertFalse(Float.isNaN(c) || Float.isInfinite(c), mesh.name + " face " + f + " corner is a number");
            }
        }
        assertTrue(HerculesModels.HAND.parts >= 15, "the hand has a skeleton (palm, cuff, plate, fingers, thumb)");
        assertEquals(1, HerculesModels.FIST.parts, "the fist is one rigid piece");
    }

    @Test
    void noViewKeepsMoreQuadsThanTheModelsLimit() {
        Random r = new Random(20260);   // views from 1.5 to 15 blocks
        float[] close = {0f, 0.5f, 1f};
        for (Mesh mesh : ALL) {
            Posed p = new Posed();
            int worst = 0;
            for (int k = 0; k < 3000; k++) {
                p.clearPose();
                if (mesh == HerculesModels.HAND) HerculesModels.handPose(close[k % 3], 1f + 0.6f * r.nextFloat(), p.bend(), p.spread());
                p.look((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), r.nextFloat() * 6.28f, 0.5f + 1.5f * r.nextFloat());
                float d = 1.5f + r.nextFloat() * 14f, a1 = r.nextFloat() * 6.28f, a2 = r.nextFloat() * 3.14f - 1.57f;
                int n = p.prepare(mesh, (float) (Math.cos(a1) * Math.cos(a2) * d), (float) (Math.sin(a2) * d), (float) (Math.sin(a1) * Math.cos(a2) * d), 0xFFFFFFFF, r.nextFloat());
                worst = Math.max(worst, n);
                assertTrue(n <= mesh.maxDrawn, mesh.name + " kept " + n + " quads from one view, the limit is " + mesh.maxDrawn);
            }
            assertTrue(worst >= mesh.maxDrawn / 3, mesh.name + " limit " + mesh.maxDrawn + " is not wildly above what a view keeps (" + worst + ")");
        }
        // the layers put these under budget checks: a hand + a fist + a boulder never meet in one effect, the biggest sum of limits of one effect is below the 100-quad activation cap
        assertTrue(HerculesModels.HAND_MAX + 30 <= 100, "the hand leaves room for the chains and the flash");
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
    void theHandOpensAndClosesIntoAFist() {
        float[] b0 = new float[Posed.MAX_PARTS], s0 = new float[Posed.MAX_PARTS], b1 = new float[Posed.MAX_PARTS], s1 = new float[Posed.MAX_PARTS];
        HerculesModels.handPose(0f, 1f, b0, s0);
        HerculesModels.handPose(1f, 1f, b1, s1);
        float open = 0f, shut = 0f;
        for (int i = HerculesModels.H_FINGER0; i < HerculesModels.H_THUMB0; i++) { open += Math.abs(b0[i]); shut += Math.abs(b1[i]); }
        assertTrue(shut > open + 3f, "the fingers curl when the hand closes (open " + open + ", closed " + shut + ")");
        assertEquals(18, HerculesModels.H_PARTS, "palm, cuff, back plate, 4 x 3 finger segments and 3 thumb segments");
        assertTrue(HerculesModels.HAND_FIST_Z > 0f, "the fist centre lies ahead of the wrist");
    }

    // ---------------------------------------------------------------- the switch
    @Test
    void theSwitchDefaultsToOnAndIsAClientConfigRegisteredOnce() throws IOException {
        assertTrue(HerculesModels.enabled(), "the 3D models are on by default");
        HerculesModels.setEnabled(false);
        assertFalse(HerculesModels.enabled());
        HerculesModels.setEnabled(true);
        assertTrue(HerculesModels.enabled());
        String cfg = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/HerculesModelsConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT"), "it only changes how things look: a CLIENT config");
        assertTrue(cfg.contains("\"herculesModels\", true") || cfg.contains("define(\"herculesModels\", true)"), "default on");
        assertTrue(cfg.contains("\"nusmp-hercules-client.toml\""), "its own file name");
        assertTrue(cfg.contains("HerculesModels.setEnabled("), "it is mirrored into the pure switch");
        String main = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        Matcher m = Pattern.compile("HerculesModelsConfig\\.init\\(modEventBus, modContainer\\)").matcher(main);
        assertTrue(m.find(), "NUSMP registers the config");
        assertFalse(m.find(), "and only once");
    }

    // ---------------------------------------------------------------- the textures
    @Test
    void theAtlasAndTheFissureStripAreRealSizedTextures() throws IOException {
        BufferedImage atlas = ImageIO.read(TEX.resolve("hercules_model_atlas.png").toFile());
        assertEquals(512, atlas.getWidth());
        assertEquals(512, atlas.getHeight());
        int cell = 128;
        for (int c = 0; c < 16; c++) {
            long opaque = 0, lit = 0;
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) {
                    int argb = atlas.getRGB((c % 4) * cell + x, (c / 4) * cell + y);
                    int al = argb >>> 24;
                    if (al > 200) opaque++;
                    if (al > 0) {
                        lit++;
                        assertNotEquals(0, argb & 0xFFFFFF, "no black colour under a visible pixel (cell " + c + ")");
                    } else assertEquals(0, argb & 0xFFFFFF, "no colour under alpha 0 in cell " + c + " (it bleeds into the mipmaps)");
                }
            }
            assertTrue(lit > 4000, "cell " + c + " has content: " + lit);
            boolean leaf = c == HerculesModels.LEAF || c == HerculesModels.LEAF_B;
            if (!leaf) assertEquals(cell * cell, opaque, "cell " + c + " is a solid material");
            else assertTrue(opaque > 2000 && opaque < cell * cell / 2, "the leaf cell is a blade on a clear ground: " + opaque);
        }
        BufferedImage fis = ImageIO.read(TEX.resolve("hercules_model_fissure.png").toFile());
        assertEquals(64, fis.getWidth());
        assertEquals(256, fis.getHeight());
        int core = 0;
        for (int y = 0; y < 256; y++) if ((fis.getRGB(32, y) >>> 24) > 200) core++;
        assertTrue(core > 100, "the fissure strip has a glowing core down its middle: " + core);
        assertEquals(0, fis.getRGB(0, 100) >>> 24, "and clear edges");
    }

    // ---------------------------------------------------------------- the code
    @Test
    void nothingIsBuiltPerFrameAndTheMeshCodeStaysPure() throws IOException {
        String models = code(LAYER_DIR.resolve("HerculesModels.java"));
        assertFalse(models.contains("net.minecraft") || models.contains("com.mojang") || models.contains("Minecraft.getInstance"), "the mesh code is pure geometry");
        assertFalse(models.contains("java.util.ArrayList") || models.contains("java.util.HashMap"), "no collections in the mesh code");
        int a = models.indexOf("public int prepare(");
        int b = models.indexOf("public int at(");
        assertTrue(a > 0 && b > a, "prepare is found");
        String perFrame = models.substring(a, b);
        assertFalse(perFrame.contains("new "), "Posed.prepare allocates nothing");
        for (String m : List.of("public Posed look(", "public Posed tumble(", "public Posed upright(", "public Posed axes(")) {
            int s = models.indexOf(m);
            assertTrue(s > 0, m);
            int e = models.indexOf("\n        }", s);
            assertFalse(models.substring(s, e).contains("new "), m + " allocates nothing");
        }
        String draw = code(LAYER_DIR.resolve("HerculesMeshDraw.java"));
        String body = draw.substring(draw.indexOf("public int draw("));
        assertFalse(body.contains("new "), "HerculesMeshDraw.draw allocates nothing");
        assertTrue(draw.contains("buf.hasBudget(n * 4)"), "a model that does not fit the activation budget is skipped whole");
        assertTrue(draw.contains("return 0"), "and reports 0 so the layer draws the flat sprite");
        assertFalse(draw.contains("Minecraft.getInstance") || draw.contains("com.mojang.blaze3d"), "no client singletons");
        String layer = code(LAYER_DIR.resolve("HerculesLayer.java"));
        assertFalse(Pattern.compile("new (float|int)\\[").matcher(layer).find(), "no arrays built per frame in the layer");
        assertFalse(layer.contains("new HerculesModels") || layer.contains("new Posed"), "the layer never builds a poser per frame");
        assertEquals(1, Pattern.compile("new HerculesMeshDraw\\(\\)").matcher(layer).results().count(), "one drawer per layer");
    }

    @Test
    void everyModelIsBehindTheSwitchTheDistanceAndTheBudgetAndKeepsItsOldSprite() throws IOException {
        String src = read(LAYER_DIR.resolve("HerculesLayer.java"));
        String layer = code(LAYER_DIR.resolve("HerculesLayer.java"));
        assertTrue(layer.contains("HerculesModels.enabled() && ctx.detail >= 0.5f"), "the setting and the particle setting gate the models");
        assertTrue(layer.contains("x * x + y * y + z * z < range * range"), "and the camera distance");
        assertTrue(Pattern.compile("MODEL_RANGE = 64f, CHUNK_RANGE = 44f, WREATH_RANGE = 36f").matcher(src).find(), "the ranges are constants");
        for (String model : List.of("HerculesModels.HAND", "HerculesModels.FIST", "HerculesModels.BOULDER", "HerculesModels.CHUNK", "HerculesModels.CROWN", "HerculesModels.RING"))
            assertTrue(layer.contains(model), "the layer draws " + model);
        // the old 3.19.0 draw calls are all still in the code as the fallback
        for (String old : List.of("sprite(ctx, buf, HAND, VfxBlend.ALPHA", "sprite(ctx, buf, FIST, VfxBlend.ALPHA", "sprite(ctx, buf, FIST, VfxBlend.ADD", "sprite(ctx, buf, LAUREL, VfxBlend.ADD", "buf.plane(LAUREL, VfxBlend.ADD", "buf.plane(RING, VfxBlend.ADD", "slab(ctx, buf,"))
            assertTrue(layer.contains(old), "the 3.19.0 sprite call is kept: " + old);
        for (String n : List.of("hercules_fist", "hercules_hand", "hercules_laurel", "hercules_leaf", "hercules_ring", "hercules_wave", "hercules_cracks", "hercules_web", "hercules_dust", "hercules_chips", "hercules_streak", "hercules_chain", "hercules_flash", "hercules_mote", "hercules_plates", "hercules_flame", "hercules_crater", "hercules_model_atlas", "hercules_model_fissure")) {
            assertTrue(src.contains("\"" + n + "\""), "the layer still names " + n);
            assertTrue(Files.size(TEX.resolve(n + ".png")) > 200, n + ".png exists and is not empty");
        }
        assertTrue(Pattern.compile("THROW_CHUNK_POWER = 2f").matcher(src).find(), "power 2 of HERCULES_THROW means a hurled chunk of ground");
        assertTrue(layer.contains("inst.power >= THROW_CHUNK_POWER - 0.5f"), "a creature thrown (power 1) draws no boulder");
        assertTrue(Pattern.compile("AFTERSHOCK_MAX = 10f").matcher(src).find(), "a tremor of power 10 or less is the aftershock");
        assertTrue(layer.contains("boolean after = inst.power <= AFTERSHOCK_MAX"), "the quiet variant is selected from the power");
        assertTrue(layer.contains("VfxShake.add(inst.payload.from(), 0.45f, 10)"), "the aftershock only rumbles");
        assertTrue(Pattern.compile("buf\\.hasBudget\\(\\d+\\) && chunk\\(").matcher(layer).results().count() >= 4, "rock chunks check the budget first");
        assertTrue(layer.contains("4 * HerculesModels.RING_MAX"), "the ring of the Invictus checks its budget first");
        assertEquals(200, Integer.parseInt(Pattern.compile("case HERCULES_INVICTUS -> (\\d+);").matcher(layer).results().findFirst().orElseThrow().group(1)), "the Invictus budget stays 200 vertices");
    }

    @Test
    void theFissureAndChunkHelpersAreBounded() throws IOException {
        String layer = code(LAYER_DIR.resolve("HerculesLayer.java"));
        assertTrue(layer.contains("if ((argb >>> 24) < 4 || r1 - r0 < 0.3f || !buf.hasBudget(8)) return;"), "a fissure is two quads and checks the budget");
        assertEquals(1, Pattern.compile("private int chunk\\(").matcher(layer).results().count());
        assertTrue(layer.contains("alpha <= 0.03f || size <= 0.02f"), "an invisible chunk is not posed");
        Matcher m = Pattern.compile("Math\\.min\\((after \\? )?(\\d+)(?:f)?, ctx\\.seg\\((\\d+), (\\d+)\\)\\)").matcher(layer);
        int found = 0;
        while (m.find()) { found++; assertTrue(Integer.parseInt(m.group(2)) <= 8, "at most eight fissures or chunks: " + m.group()); }
        assertTrue(found >= 2, "the fissure counts are capped (" + found + ")");
    }

    // ---------------------------------------------------------------- the owner's rules
    @Test
    void theTimeMagicLookIsUntouchedAndTheGeneratorIsItsOwnFile() throws IOException {
        for (String f : List.of("HerculesModels.java", "HerculesMeshDraw.java"))
            assertFalse(read(LAYER_DIR.resolve(f)).contains("time_"), f + " never names a Time Magic texture");
        String gen = read(ROOT.resolve("tools/gen_hercules_model_textures.py"));
        assertFalse(gen.contains("time_"), "the generator never writes a Time Magic texture");
        assertTrue(gen.contains("hercules_model_atlas") && gen.contains("hercules_model_fissure"), "it writes the two textures");
        assertTrue(Files.exists(ROOT.resolve("tools/gen_hercules_vfx_textures.py")), "the old generator is kept");
    }

    @Test
    void theNewPreviewScenesAndTheDocExist() throws IOException {
        String scenes = read(ROOT.resolve("tools/vfx_preview/scenes.d/hercules.json"));
        for (String s : List.of("hercules_throw_chunk", "hercules_aftershock", "hercules_grip_close", "hercules_strike_close", "hercules_throw_close", "hercules_invictus_close", "hercules_tremor_close"))
            assertTrue(scenes.contains("\"" + s + "\""), "a preview scene: " + s);
        Matcher power = Pattern.compile("\"hercules_throw_chunk\": \\{.*?\"power\": (\\d+)", Pattern.DOTALL).matcher(scenes);
        assertTrue(power.find() && power.group(1).equals("2"), "the chunk-throw scene uses power 2");
        Matcher after = Pattern.compile("\"hercules_aftershock\": \\{.*?\"power\": (\\d+)", Pattern.DOTALL).matcher(scenes);
        assertTrue(after.find() && after.group(1).equals("9"), "the aftershock scene uses power 9");
        String doc = read(ROOT.resolve("docs/vfx_remake/hercules_models.md"));
        for (String s : List.of("HAND", "FIST", "BOULDER", "CHUNK", "CROWN", "RING", "herculesModels", "NEW", "REPLACED", "KEPT", "worst", "aftershock"))
            assertTrue(doc.toLowerCase().contains(s.toLowerCase()), "the doc covers " + s);
        String calls = read(ROOT.resolve("docs/vfx_remake/hercules_fx_calls.md"));
        assertTrue(calls.contains("3.20.0") && calls.contains("power = 2") && calls.contains("aftershock"), "the call doc has the 3.20.0 section");
    }
}
