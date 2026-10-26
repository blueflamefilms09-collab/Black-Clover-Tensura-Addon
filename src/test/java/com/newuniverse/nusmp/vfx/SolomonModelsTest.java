package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.vfx.client.layer.SolomonModels;
import com.newuniverse.nusmp.vfx.client.layer.SolomonModels.Mesh;
import com.newuniverse.nusmp.vfx.client.layer.SolomonModels.Posed;
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
 * Why: the Solomon ultimate is drawn with real 3D models (the ring of the 72 Pillars, the floating Grimoire, the links and the sigil head of the Spell Chain, the signet ring, the rune crystals and the glass
 * shards) inside {@code SolomonLayer}. They are pure geometry ({@code SolomonModels}), posed and drawn without allocation ({@code SolomonMeshDraw}) and switched by the CLIENT setting
 * {@code visual.solomonModels}. This test guards the rule-11 limits (every model keeps at most its quad constant from any view, the atlas is real and sized right, nothing is built per frame, a model that does not fit
 * the budget is skipped whole), the "keep the old" rule (every flat sprite and its draw call is still there as the fallback), the switch (default on, registered once, a CLIENT config), the new sample scenes and the
 * doc. Pure: runs the real mesh code and reads the real sources and PNGs, the working directory is the project root.
 */
class SolomonModelsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static final Mesh[] ALL = {SolomonModels.PILLAR, SolomonModels.GRIMOIRE, SolomonModels.LINK, SolomonModels.HEAD, SolomonModels.RING, SolomonModels.CRYSTAL,
            SolomonModels.SHARD_TRI, SolomonModels.SHARD_QUAD, SolomonModels.SHARD_LONG};

    // ---------------------------------------------------------------- the meshes
    @Test
    void everyMeshHasFacesInsideTheAtlasAndTheLimitsAreTheDocumentedOnes() {
        assertEquals(12, SolomonModels.PILLAR_MAX);
        assertEquals(19, SolomonModels.GRIMOIRE_MAX);
        assertEquals(10, SolomonModels.LINK_MAX);
        assertEquals(10, SolomonModels.HEAD_MAX);
        assertEquals(32, SolomonModels.RING_MAX);
        assertEquals(5, SolomonModels.CRYSTAL_MAX);
        assertEquals(4, SolomonModels.SHARD_MAX);
        int[] limits = {SolomonModels.PILLAR_MAX, SolomonModels.GRIMOIRE_MAX, SolomonModels.LINK_MAX, SolomonModels.HEAD_MAX, SolomonModels.RING_MAX, SolomonModels.CRYSTAL_MAX,
                SolomonModels.SHARD_MAX, SolomonModels.SHARD_MAX, SolomonModels.SHARD_MAX};
        for (int m = 0; m < ALL.length; m++) {
            Mesh mesh = ALL[m];
            assertEquals(limits[m], mesh.maxDrawn, mesh.name + " carries its quad limit");
            assertEquals(0, mesh.badWinding, mesh.name + " has faces wound the wrong way round");
            assertTrue(mesh.faces > 0 && mesh.faces <= Posed.MAX_FACES, mesh.name + " fits the face arrays of the poser: " + mesh.faces);
            assertTrue(mesh.parts >= 1 && mesh.parts <= Posed.MAX_PARTS, mesh.name + " fits the part arrays: " + mesh.parts);
            for (int f = 0; f < mesh.faces; f++) {
                int cell = mesh.cellOf(f);
                assertTrue(cell >= 0 && cell < SolomonModels.CELLS, mesh.name + " face " + f + " uses a cell of the 8 x 8 atlas: " + cell);
                float[] r = mesh.rectOf(f);
                assertTrue(r[0] >= 0f && r[1] >= 0f && r[2] <= 1f && r[3] <= 1f && r[0] < r[2] && r[1] < r[3], mesh.name + " face " + f + " rectangle inside its cell");
                for (float c : mesh.corners(f)) assertFalse(Float.isNaN(c) || Float.isInfinite(c), mesh.name + " face " + f + " corner is a number");
            }
        }
        assertEquals(SolomonModels.COLS * SolomonModels.ROWS, 64);
        assertTrue(SolomonModels.CELLS <= SolomonModels.COLS * SolomonModels.ROWS, "every cell has a place in the atlas");
        assertEquals(SolomonModels.G_PARTS, SolomonModels.GRIMOIRE.parts, "the grimoire has a skeleton (cover, front, four page leaves, two ribbon pieces)");
    }

    @Test
    void noViewKeepsMoreQuadsThanTheModelsLimit() {
        Random r = new Random(20260);   // views from 1.5 to 15 blocks
        float[] open = {0f, 0.5f, 1f};
        for (Mesh mesh : ALL) {
            Posed p = new Posed();
            int worst = 0;
            for (int k = 0; k < 3000; k++) {
                p.clearPose();
                if (mesh == SolomonModels.GRIMOIRE) SolomonModels.grimoirePose(open[k % 3], r.nextFloat(), r.nextFloat(), 0.1f, p.bend(), p.spread());
                p.look((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), r.nextFloat() * 6.28f, 0.5f + 1.5f * r.nextFloat());
                float d = 1.5f + r.nextFloat() * 14f, a1 = r.nextFloat() * 6.28f, a2 = r.nextFloat() * 3.14f - 1.57f;
                int n = p.prepare(mesh, (float) (Math.cos(a1) * Math.cos(a2) * d), (float) (Math.sin(a2) * d), (float) (Math.sin(a1) * Math.cos(a2) * d), 0xFFFFFFFF, r.nextFloat());
                worst = Math.max(worst, n);
                assertTrue(n <= mesh.maxDrawn, mesh.name + " kept " + n + " quads from one view, the limit is " + mesh.maxDrawn);
            }
            assertTrue(worst >= mesh.maxDrawn / 3, mesh.name + " limit " + mesh.maxDrawn + " is not wildly above what a view keeps (" + worst + ")");
        }
        // the layer puts these under budget checks: the biggest sum of limits of one effect (4 links + head, 6 pillars) leaves room inside the 100-quad activation cap
        assertTrue(4 * SolomonModels.LINK_MAX + SolomonModels.HEAD_MAX <= 60, "the chain leaves room for the circles");
        assertTrue(6 * SolomonModels.PILLAR_MAX <= 80, "the pillar ring leaves room for the old light");
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
                    assertTrue(i >= 0 && i < Posed.MAX_FACES, "an index inside the face arrays");
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
    void theGrimoireOpensAndClosesAndTheLinksAndRingHaveTheirSize() {
        float[] b0 = new float[Posed.MAX_PARTS], s0 = new float[Posed.MAX_PARTS], b1 = new float[Posed.MAX_PARTS], s1 = new float[Posed.MAX_PARTS];
        SolomonModels.grimoirePose(0f, 0f, 0f, 0f, b0, s0);
        SolomonModels.grimoirePose(1f, 0f, 0f, 0f, b1, s1);
        float shut = 0f, open = 0f;
        for (int i = 0; i < Posed.MAX_PARTS; i++) { shut += Math.abs(b0[i]) + Math.abs(s0[i]); open += Math.abs(b1[i]) + Math.abs(s1[i]); }
        assertNotEquals(shut, open, 1e-4f, "the pose of a shut book differs from an open one");
        assertTrue(SolomonModels.BOOK_HALF_W > 0.2f && SolomonModels.BOOK_HALF_H > 0.2f, "the book has a size");
        assertTrue(SolomonModels.LINK_LEN > 0.5f && SolomonModels.LINK_LEN < 3f, "a chain link is about a block long");
        assertTrue(SolomonModels.RING_R > 0.2f && SolomonModels.RING_R < 1f, "the ring has a size");
    }

    // ---------------------------------------------------------------- the switch
    @Test
    void theSwitchDefaultsToOnAndIsAClientConfigRegisteredOnce() throws IOException {
        assertTrue(SolomonModels.enabled(), "the 3D models are on by default");
        SolomonModels.setEnabled(false);
        assertFalse(SolomonModels.enabled());
        SolomonModels.setEnabled(true);
        assertTrue(SolomonModels.enabled());
        String cfg = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/SolomonModelsConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT"), "it only changes how things look: a CLIENT config");
        assertTrue(cfg.contains("define(\"solomonModels\", true)"), "default on");
        assertTrue(cfg.contains("\"nusmp-solomon-client.toml\""), "its own file name");
        assertTrue(cfg.contains("SolomonModels.setEnabled("), "it is mirrored into the pure switch");
        String main = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        Matcher m = Pattern.compile("SolomonModelsConfig\\.init\\(modEventBus, modContainer\\)").matcher(main);
        assertTrue(m.find(), "NUSMP registers the config");
        assertFalse(m.find(), "and only once");
    }

    // ---------------------------------------------------------------- the texture
    @Test
    void theAtlasIsARealSizedTexture() throws IOException {
        BufferedImage atlas = ImageIO.read(TEX.resolve("solomon_model_atlas.png").toFile());
        assertEquals(1024, atlas.getWidth());
        assertEquals(1024, atlas.getHeight());
        int cell = 1024 / SolomonModels.COLS;
        for (int c = 0; c < SolomonModels.CELLS; c++) {
            long lit = 0;
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) {
                    int argb = atlas.getRGB((c % SolomonModels.COLS) * cell + x, (c / SolomonModels.COLS) * cell + y);
                    if ((argb >>> 24) > 0) {
                        lit++;
                        assertNotEquals(0, argb & 0xFFFFFF, "no black colour under a visible pixel (cell " + c + ")");
                    } else assertEquals(0, argb & 0xFFFFFF, "no colour under alpha 0 in cell " + c + " (it bleeds into the mipmaps)");
                }
            }
            assertTrue(lit > 4000, "cell " + c + " has content: " + lit);
        }
        // the cells that are solid materials
        for (int c : new int[]{SolomonModels.STONE, SolomonModels.FLUTE, SolomonModels.COVER, SolomonModels.PAGE, SolomonModels.LINK_TOP, SolomonModels.RING_BAND}) {
            int solid = 0;
            for (int y = 0; y < cell; y += 4)
                for (int x = 0; x < cell; x += 4) if ((atlas.getRGB((c % SolomonModels.COLS) * cell + x, (c / SolomonModels.COLS) * cell + y) >>> 24) > 200) solid++;
            assertEquals((cell / 4) * (cell / 4), solid, "cell " + c + " is a solid material");
        }
    }

    // ---------------------------------------------------------------- the code
    @Test
    void nothingIsBuiltPerFrameAndTheMeshCodeStaysPure() throws IOException {
        String models = code(LAYER_DIR.resolve("SolomonModels.java"));
        assertFalse(models.contains("net.minecraft") || models.contains("com.mojang") || models.contains("Minecraft.getInstance") || models.contains("com.newuniverse.nusmp.client"), "the mesh code is pure geometry");
        assertFalse(models.contains("java.util.ArrayList") || models.contains("java.util.HashMap"), "no collections in the mesh code");
        int a = models.indexOf("public int prepare(");
        int b = models.indexOf("public int at(");
        assertTrue(a > 0 && b > a, "prepare is found");
        assertFalse(models.substring(a, b).contains("new "), "Posed.prepare allocates nothing");
        for (String m : List.of("public Posed look(", "public Posed tumble(", "public Posed stand(", "public Posed axes(")) {
            int s = models.indexOf(m);
            assertTrue(s > 0, m);
            int e = models.indexOf("\n        }", s);
            assertFalse(models.substring(s, e).contains("new "), m + " allocates nothing");
        }
        String draw = code(LAYER_DIR.resolve("SolomonMeshDraw.java"));
        String body = draw.substring(draw.indexOf("public int draw("));
        assertFalse(body.contains("new "), "SolomonMeshDraw.draw (and what follows it) allocates nothing");
        assertTrue(draw.contains("buf.hasBudget(n * 4)"), "a model that does not fit the activation budget is skipped whole");
        assertFalse(draw.contains("Minecraft.getInstance") || draw.contains("com.mojang.blaze3d"), "no client singletons");
        String layer = code(LAYER_DIR.resolve("SolomonLayer.java"));
        assertFalse(Pattern.compile("new (float|int)\\[").matcher(layer).find(), "no arrays built per frame in the layer");
        assertFalse(layer.contains("new SolomonModels") || layer.contains("new Posed"), "the layer never builds a poser per frame");
        assertEquals(1, Pattern.compile("new SolomonMeshDraw\\(\\)").matcher(layer).results().count(), "one drawer per layer");
    }

    @Test
    void everyModelIsBehindTheSwitchTheDistanceAndTheBudgetAndKeepsItsOldSprite() throws IOException {
        String src = read(LAYER_DIR.resolve("SolomonLayer.java"));
        String layer = code(LAYER_DIR.resolve("SolomonLayer.java"));
        assertTrue(layer.contains("SolomonModels.enabled() && ctx.detail >= 0.5f"), "the setting and the particle setting gate the models");
        assertTrue(layer.contains("x * x + y * y + z * z < range * range"), "and the camera distance");
        assertTrue(Pattern.compile("MODEL_RANGE = 56f, PILLAR_MODEL_RANGE = 72f").matcher(src).find(), "the ranges are constants");
        for (String model : List.of("SolomonModels.PILLAR", "SolomonModels.GRIMOIRE", "SolomonModels.LINK", "SolomonModels.HEAD", "SolomonModels.RING", "SolomonModels.CRYSTAL", "SolomonModels.SHARD_TRI", "SolomonModels.SHARD_QUAD", "SolomonModels.SHARD_LONG"))
            assertTrue(layer.contains(model), "the layer draws " + model);
        // the flat sprite calls of the older versions are all still in the code as the fallback
        for (String old : List.of("sheet(ctx, buf, PAGE,", "sheet(ctx, buf, BOOK,", "ribbon(buf, CHAIN,", "rune(ctx, buf,"))
            assertTrue(layer.contains(old), "the older draw call is kept: " + old);
        for (String n : List.of("solomon_ring", "solomon_hexagram", "solomon_circle", "solomon_page", "solomon_runes", "solomon_pillar", "solomon_rune_wave", "solomon_chain", "solomon_book", "solomon_beam", "solomon_flare",
                "solomon_mote", "solomon_shell", "solomon_crack", "solomon_sigils", "solomon_crater", "solomon_veil", "solomon_model_atlas")) {
            assertTrue(src.contains("\"" + n + "\""), "the layer still names " + n);
            assertTrue(Files.size(TEX.resolve(n + ".png")) > 200, n + ".png exists and is not empty");
        }
        // every model asks for room first
        assertTrue(layer.contains("4 * SolomonModels.GRIMOIRE_MAX"), "the grimoire checks its budget first");
        assertTrue(layer.contains("4 * SolomonModels.CRYSTAL_MAX"), "a crystal checks its budget first");
        assertTrue(layer.contains("4 * SolomonModels.SHARD_MAX"), "a shard checks its budget first");
        assertTrue(layer.contains("4 * SolomonModels.RING_MAX"), "the signet ring checks its budget first");
        assertTrue(layer.contains("SolomonModels.LINK_MAX") && layer.contains("SolomonModels.HEAD_MAX"), "the chain checks the room for its links and head first");
        assertTrue(layer.contains("SolomonModels.PILLAR_MAX"), "the pillar ring checks its budget first");
        assertEquals(200, Integer.parseInt(Pattern.compile("case SOLOMON_REALM -> (\\d+);").matcher(layer).results().findFirst().orElseThrow().group(1)), "the Realm budget stays 200 vertices");
        // loops are bounded
        Matcher m = Pattern.compile("ctx\\.seg\\((\\d+), (\\d+)\\)").matcher(layer);
        while (m.find()) assertTrue(Integer.parseInt(m.group(1)) <= 16, "a loop count of at most 16: " + m.group());
        assertTrue(layer.contains("ctx.seg(RING_PILLARS, 3)") && Pattern.compile("RING_PILLARS = (\\d+)").matcher(src).results().allMatch(x -> Integer.parseInt(x.group(1)) <= 8), "at most eight pillars in the ring");
    }

    // ---------------------------------------------------------------- the owner's rules
    @Test
    void theTimeMagicLookIsUntouchedAndTheGeneratorIsItsOwnFile() throws IOException {
        for (String f : List.of("SolomonModels.java", "SolomonMeshDraw.java"))
            assertFalse(read(LAYER_DIR.resolve(f)).contains("time_"), f + " never names a Time Magic texture");
        String gen = read(ROOT.resolve("tools/gen_solomon_model_textures.py"));
        assertFalse(gen.contains("time_"), "the generator never writes a Time Magic texture");
        assertTrue(gen.contains("solomon_model_atlas"), "it writes the atlas");
        assertTrue(Files.exists(ROOT.resolve("tools/gen_solomon_vfx_textures.py")), "the old generator is kept");
    }

    @Test
    void theNewPreviewScenesAndTheDocExist() throws IOException {
        String scenes = read(ROOT.resolve("tools/vfx_preview/scenes.d/solomon_models.json"));
        for (String s : List.of("solomon_models_pillar_close", "solomon_models_pillar_72", "solomon_models_copy_close", "solomon_models_realm_close", "solomon_models_chain_close", "solomon_models_chain_head",
                "solomon_models_mark_close", "solomon_models_convergence_close", "solomon_models_erasure_close"))
            assertTrue(scenes.contains("\"" + s + "\""), "a preview scene: " + s);
        assertTrue(Files.exists(ROOT.resolve("tools/vfx_preview/scenes.d/solomon.json")), "the older scenes are kept");
        String doc = read(ROOT.resolve("docs/vfx_remake/solomon_models.md"));
        for (String s : List.of("PILLAR", "GRIMOIRE", "LINK", "HEAD", "RING", "CRYSTAL", "SHARD", "solomonModels", "NEW", "REPLACED", "KEPT", "worst", "Not seen in a game"))
            assertTrue(doc.toLowerCase().contains(s.toLowerCase()), "the doc covers " + s);
    }
}
