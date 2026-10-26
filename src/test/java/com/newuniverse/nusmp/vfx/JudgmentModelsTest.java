package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.vfx.client.layer.JudgmentModels;
import com.newuniverse.nusmp.vfx.client.layer.JudgmentModels.Mesh;
import com.newuniverse.nusmp.vfx.client.layer.JudgmentModels.Posed;
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
 * Why: 3.21.0 draws the Sandalphon (Lord of Judgment) ultimate with real 3D models (the guillotine blade, the scales of judgment, the law tablet and its shards, the lance head, the sword of Final Judgment) inside
 * {@code JudgmentLayer}. They are pure geometry ({@code JudgmentModels}), posed and drawn without allocation ({@code JudgmentMeshDraw}) and switched by the CLIENT setting {@code visual.judgmentModels}. This test guards the
 * rule-11 limits (every model keeps at most its quad constant from any view, the textures are real and sized right, nothing is built per frame, a model that does not fit the budget is skipped whole), the "keep the old" rule
 * (every 3.20.0 flat part and its draw call is still there as the fallback), the switch (default on, registered once, a CLIENT config), the new sample scenes and the doc. Pure: runs the real mesh code and reads the real
 * sources and PNGs, the working directory is the project root.
 */
class JudgmentModelsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static final Mesh[] ALL = {JudgmentModels.GUILLOTINE, JudgmentModels.SCALES, JudgmentModels.TABLET, JudgmentModels.SHARD, JudgmentModels.LANCE, JudgmentModels.SWORD};

    // ---------------------------------------------------------------- the meshes
    @Test
    void everyMeshHasFacesInsideTheAtlasAndTheLimitsAreTheDocumentedOnes() {
        assertEquals(18, JudgmentModels.GUILLOTINE_MAX);
        assertEquals(56, JudgmentModels.SCALES_MAX);
        assertEquals(14, JudgmentModels.TABLET_MAX);
        assertEquals(4, JudgmentModels.SHARD_MAX);
        assertEquals(22, JudgmentModels.LANCE_MAX);
        assertEquals(36, JudgmentModels.SWORD_MAX);
        int[] limits = {JudgmentModels.GUILLOTINE_MAX, JudgmentModels.SCALES_MAX, JudgmentModels.TABLET_MAX, JudgmentModels.SHARD_MAX, JudgmentModels.LANCE_MAX, JudgmentModels.SWORD_MAX};
        for (int m = 0; m < ALL.length; m++) {
            Mesh mesh = ALL[m];
            assertEquals(limits[m], mesh.maxDrawn, mesh.name + " carries its quad limit");
            assertTrue(mesh.faces > 0 && mesh.faces <= Posed.MAX_FACES, mesh.name + " fits the face arrays of the poser: " + mesh.faces);
            assertTrue(mesh.parts >= 1 && mesh.parts <= Posed.MAX_PARTS, mesh.name + " fits the part arrays: " + mesh.parts);
            for (int f = 0; f < mesh.faces; f++) {
                int cell = mesh.cellOf(f);
                assertTrue(cell >= 0 && cell < 16, mesh.name + " face " + f + " uses a cell of the 4 x 4 atlas: " + cell);
                float[] r = mesh.rectOf(f);
                assertTrue(r[0] >= 0f && r[1] >= 0f && r[2] <= 1f && r[3] <= 1f && r[0] < r[2] && r[1] < r[3], mesh.name + " face " + f + " rectangle inside its cell");
                for (float c : mesh.corners(f)) assertFalse(Float.isNaN(c) || Float.isInfinite(c), mesh.name + " face " + f + " corner is a number");
            }
        }
        assertEquals(4, JudgmentModels.SCALES.parts, "the scales are a base with a beam and two pans");
        assertEquals(1, JudgmentModels.SWORD.parts, "the sword is one rigid piece");
    }

    @Test
    void noViewKeepsMoreQuadsThanTheModelsLimit() {
        Random r = new Random(20260);   // views from 1.5 to 15 blocks, random pose of every part
        for (Mesh mesh : ALL) {
            Posed p = new Posed();
            int worst = 0;
            for (int k = 0; k < 3000; k++) {
                p.clearPose();
                for (int i = 0; i < mesh.parts; i++) { p.bend()[i] = (r.nextFloat() - 0.5f) * 0.3f; p.roll()[i] = (r.nextFloat() - 0.5f) * 0.6f; }
                p.look((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), r.nextFloat() * 6.28f, 0.5f + 1.5f * r.nextFloat());
                float d = 1.5f + r.nextFloat() * 14f, a1 = r.nextFloat() * 6.28f, a2 = r.nextFloat() * 3.14f - 1.57f;
                int n = p.prepare(mesh, (float) (Math.cos(a1) * Math.cos(a2) * d), (float) (Math.sin(a2) * d), (float) (Math.sin(a1) * Math.cos(a2) * d), 0xFFFFFFFF, r.nextFloat());
                worst = Math.max(worst, n);
                assertTrue(n <= mesh.maxDrawn, mesh.name + " kept " + n + " quads from one view, the limit is " + mesh.maxDrawn);
            }
            assertTrue(worst >= mesh.maxDrawn / 2, mesh.name + " limit " + mesh.maxDrawn + " is not wildly above what a view keeps (" + worst + ")");
        }
        // the layers put these under budget checks; the biggest model of an effect plus the old parts that stay is measured below the 100-quad cap (see the doc)
        assertTrue(JudgmentModels.SCALES_MAX + 40 <= 100 + 2, "the scales leave room for the field's floor and rim");
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
    void theScalesTiltAndEveryModelStandsWhereItIsMeant() {
        // rolling the beam moves the pan ends: the scales really are a balance (the upright pose keeps the local up axis up)
        Posed p = new Posed();
        p.clearPose();
        p.upright(0.7f, 0f, 1f);
        assertEquals(1f, p.wy(0f, 1f, 0f), 1e-3f, "an upright model keeps its local y as the world up axis");
        assertEquals(0f, p.wy(JudgmentModels.SCALES_ARM, 0f, 0f), 1e-3f, "and its local x is horizontal");
        Mesh sc = JudgmentModels.SCALES;
        p.look(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f);
        p.clearPose();
        int n0 = p.prepare(sc, 0f, 0f, 5f, 0xFFFFFFFF, 0f);
        float flat = 0f;
        for (int i = 0; i < n0 * 12; i++) flat += p.pts[i] * (1 + i % 7);
        p.roll()[JudgmentModels.S_BEAM] = 0.4f;
        int n1 = p.prepare(sc, 0f, 0f, 5f, 0xFFFFFFFF, 0f);
        float tilted = 0f;
        for (int i = 0; i < n1 * 12; i++) tilted += p.pts[i] * (1 + i % 7);
        assertNotEquals(flat, tilted, 1e-3f, "rolling the beam moves the geometry");
        assertTrue(JudgmentModels.SCALES_PIVOT_Y > 1f, "the beam pivots above the base");
        assertTrue(JudgmentModels.LANCE_TIP > 0f && JudgmentModels.LANCE_BACK < 0f, "the lance head extends both ways of its centre");
        assertTrue(JudgmentModels.SWORD_TOP_Z > JudgmentModels.SWORD_GUARD_Z, "the sword has a hilt above its guard");
        assertTrue(JudgmentModels.TABLET_H > 2f && JudgmentModels.TABLET_HALF_W > 0.2f, "the tablet is tall and slim");
        assertTrue(JudgmentModels.BLADE_EDGE_LOW < 0f && JudgmentModels.BLADE_TOP > 0f, "the blade edge hangs lower than its back");
    }

    // ---------------------------------------------------------------- the switch
    @Test
    void theSwitchDefaultsToOnAndIsAClientConfigRegisteredOnce() throws IOException {
        assertTrue(JudgmentModels.enabled(), "the 3D models are on by default");
        JudgmentModels.setEnabled(false);
        assertFalse(JudgmentModels.enabled());
        JudgmentModels.setEnabled(true);
        assertTrue(JudgmentModels.enabled());
        String cfg = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/JudgmentModelsConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT"), "it only changes how things look: a CLIENT config");
        assertTrue(cfg.contains("define(\"judgmentModels\", true)"), "default on");
        assertTrue(cfg.contains("\"nusmp-judgment-client.toml\""), "its own file name");
        assertTrue(cfg.contains("JudgmentModels.setEnabled("), "it is mirrored into the pure switch");
        String main = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        Matcher m = Pattern.compile("JudgmentModelsConfig\\.init\\(modEventBus, modContainer\\)").matcher(main);
        assertTrue(m.find(), "NUSMP registers the config");
        assertFalse(m.find(), "and only once");
    }

    // ---------------------------------------------------------------- the textures
    @Test
    void theAtlasIsARealSizedTexture() throws IOException {
        BufferedImage atlas = ImageIO.read(TEX.resolve("judgment_model_atlas.png").toFile());
        assertEquals(512, atlas.getWidth());
        assertEquals(512, atlas.getHeight());
        int cell = 128;
        for (int c = 0; c < 16; c++) {
            long lit = 0;
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) {
                    int argb = atlas.getRGB((c % 4) * cell + x, (c / 4) * cell + y);
                    int al = argb >>> 24;
                    if (al > 0) lit++;
                    // a cut-out keeps a gold colour under its clear pixels so the mipmaps get no black fringe: nothing in the atlas is black
                    assertNotEquals(0, argb & 0xFFFFFF, "no black colour in cell " + c + " (it bleeds into the mipmaps)");
                }
            }
            assertTrue(lit > (c == JudgmentModels.CHAIN ? 1000 : 4000), "cell " + c + " has content (the chain cell is a thin strip): " + lit);
        }
    }

    // ---------------------------------------------------------------- the code
    @Test
    void nothingIsBuiltPerFrameAndTheMeshCodeStaysPure() throws IOException {
        String models = code(LAYER_DIR.resolve("JudgmentModels.java"));
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
        String draw = code(LAYER_DIR.resolve("JudgmentMeshDraw.java"));
        String body = draw.substring(draw.indexOf("public int draw("));
        assertFalse(body.contains("new "), "JudgmentMeshDraw.draw allocates nothing");
        assertTrue(draw.contains("buf.hasBudget(n * 4)"), "a model that does not fit the activation budget is skipped whole");
        assertTrue(draw.contains("return 0"), "and reports 0 so the layer draws the flat part");
        assertFalse(draw.contains("Minecraft.getInstance") || draw.contains("com.mojang.blaze3d"), "no client singletons");
        String layer = code(LAYER_DIR.resolve("JudgmentLayer.java"));
        assertFalse(layer.contains("new JudgmentModels") || layer.contains("new Posed"), "the layer never builds a poser per frame");
        assertEquals(1, Pattern.compile("new JudgmentMeshDraw\\(\\)").matcher(layer).results().count(), "one drawer per layer");
    }

    @Test
    void everyModelIsBehindTheSwitchTheDistanceAndTheBudgetAndKeepsItsOldPart() throws IOException {
        String src = read(LAYER_DIR.resolve("JudgmentLayer.java"));
        String layer = code(LAYER_DIR.resolve("JudgmentLayer.java"));
        assertTrue(layer.contains("JudgmentModels.enabled() && ctx.detail >= 0.5f"), "the setting and the particle setting gate the models");
        assertTrue(layer.contains("x * x + y * y + z * z < range * range"), "and the camera distance");
        assertTrue(Pattern.compile("MODEL_RANGE = 64f").matcher(src).find(), "the range is a constant");
        for (String model : List.of("JudgmentModels.GUILLOTINE", "JudgmentModels.SCALES", "JudgmentModels.TABLET", "JudgmentModels.SHARD", "JudgmentModels.LANCE", "JudgmentModels.SWORD"))
            assertTrue(layer.contains(model), "the layer draws " + model);
        // the 3.20.0 flat parts are all still in the code as the fallback
        for (String old : List.of("flare(", "ringAt(buf, floor, R,", "buf.plane(CLOCK,", "buf.plane(CRACKS_GROUND,", "VfxBloom.glow("))
            assertTrue(layer.contains(old), "the 3.20.0 draw call is kept: " + old);
        for (String n : List.of("judgment_model_atlas")) {
            assertTrue(src.contains("\"" + n + "\""), "the layer names " + n);
            assertTrue(Files.size(TEX.resolve(n + ".png")) > 200, n + ".png exists and is not empty");
        }
        // every old texture the 3.20.0 layer named is still named
        Matcher t = Pattern.compile("t\\(\"(judgment_[a-z_]+)\"\\)").matcher(src);
        int named = 0;
        while (t.find()) { named++; assertTrue(Files.exists(TEX.resolve(t.group(1) + ".png")), t.group(1) + ".png exists"); }
        assertTrue(named >= 8, "the layer still names its textures (" + named + ")");
        assertTrue(Pattern.compile("ctx\\.seg\\(\\d+, \\d+\\)").matcher(layer).results().count() >= 10, "counts are thinned by distance and the particle setting");
    }

    // ---------------------------------------------------------------- the owner's rules
    @Test
    void theTimeMagicLookIsUntouchedAndTheGeneratorIsItsOwnFile() throws IOException {
        for (String f : List.of("JudgmentModels.java", "JudgmentMeshDraw.java"))
            assertFalse(read(LAYER_DIR.resolve(f)).contains("time_"), f + " never names a Time Magic texture");
        String gen = read(ROOT.resolve("tools/gen_judgment_model_textures.py"));
        assertFalse(gen.contains("time_"), "the generator never writes a Time Magic texture");
        assertTrue(gen.contains("judgment_model_atlas"), "it writes the atlas");
        assertTrue(Files.exists(ROOT.resolve("tools/gen_judgment_vfx_textures.py")), "the old generator is kept");
    }

    @Test
    void theNewPreviewScenesAndTheDocExist() throws IOException {
        String scenes = read(ROOT.resolve("tools/vfx_preview/scenes.d/judgment_models.json"));
        for (String s : List.of("judgment_cleave_close", "judgment_cleave_side", "judgment_field_close", "judgment_field_scales", "judgment_edict_close", "judgment_edict_shards", "judgment_bullet_close", "judgment_bullet_head",
                "judgment_cross_close", "judgment_cross_stand", "judgment_cross_guard"))
            assertTrue(scenes.contains("\"" + s + "\""), "a preview scene: " + s);
        assertTrue(read(ROOT.resolve("tools/vfx_preview/scenes.d/judgment.json")).contains("\"judgment_cleave\""), "the old scenes are kept");
        String doc = read(ROOT.resolve("docs/vfx_remake/judgment_models.md"));
        for (String s : List.of("GUILLOTINE", "SCALES", "TABLET", "LANCE", "SWORD", "judgmentModels", "NEW", "REPLACED", "KEPT", "worst"))
            assertTrue(doc.toLowerCase().contains(s.toLowerCase()), "the doc covers " + s);
    }
}
