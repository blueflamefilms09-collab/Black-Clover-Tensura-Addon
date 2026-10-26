package com.newuniverse.nusmp.vfx;

import com.newuniverse.nusmp.vfx.client.layer.JibrilModels;
import com.newuniverse.nusmp.vfx.client.layer.JibrilModels.Mesh;
import com.newuniverse.nusmp.vfx.client.layer.JibrilModels.Posed;
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
 * Why: 3.21.0 draws the Jibril ultimate with real 3D models (the Holy Shackle cuff and chain link, the Spear of Heaven, the halo, the law plate, the tome of law and the command crown) inside {@code JibrilLayer}. They are
 * pure geometry ({@code JibrilModels}), posed and drawn without allocation ({@code JibrilMeshDraw}) and switched by the CLIENT setting {@code visual.jibrilModels}. This test guards the rule-11 limits (every model keeps at
 * most its quad constant from any view, the atlas is real and sized right, nothing is built per frame, a model that does not fit the budget is skipped whole), the "keep the old" rule (every 3.20.0 sprite and its draw
 * call is still there as the fallback), the switch (default on, registered once, a CLIENT config), the new sample scenes and the doc. Pure: runs the real mesh code and reads the real sources and PNGs, the working directory
 * is the project root.
 */
class JibrilModelsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path TEX = ROOT.resolve("src/main/resources/assets/nusmp/textures/particle");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    private static final Mesh[] ALL = {JibrilModels.CUFF, JibrilModels.LINK, JibrilModels.SPEAR, JibrilModels.HALO, JibrilModels.PLATE, JibrilModels.TOME, JibrilModels.CROWN};

    // ---------------------------------------------------------------- the meshes
    @Test
    void everyMeshHasFacesInsideTheAtlasAndTheLimitsAreTheDocumentedOnes() {
        assertEquals(18, JibrilModels.CUFF_MAX);
        assertEquals(6, JibrilModels.LINK_MAX);
        assertEquals(36, JibrilModels.SPEAR_MAX);
        assertEquals(22, JibrilModels.HALO_MAX);
        assertEquals(3, JibrilModels.PLATE_MAX);
        assertEquals(24, JibrilModels.TOME_MAX);
        assertEquals(30, JibrilModels.CROWN_MAX);
        int[] limits = {JibrilModels.CUFF_MAX, JibrilModels.LINK_MAX, JibrilModels.SPEAR_MAX, JibrilModels.HALO_MAX, JibrilModels.PLATE_MAX, JibrilModels.TOME_MAX, JibrilModels.CROWN_MAX};
        for (int m = 0; m < ALL.length; m++) {
            Mesh mesh = ALL[m];
            assertEquals(limits[m], mesh.maxDrawn, mesh.name + " carries its quad limit");
            assertTrue(mesh.faces > 0 && mesh.faces <= Posed.MAX_FACES, mesh.name + " fits the face arrays of the poser: " + mesh.faces);
            assertTrue(mesh.parts >= 1 && mesh.parts <= Posed.MAX_PARTS, mesh.name + " fits the part arrays: " + mesh.parts);
            assertTrue(mesh.maxDrawn <= 100, mesh.name + " fits one activation");
            for (int f = 0; f < mesh.faces; f++) {
                int cell = mesh.cellOf(f);
                assertTrue(cell >= 0 && cell < 16, mesh.name + " face " + f + " uses a cell of the 4 x 4 atlas: " + cell);
                float[] r = mesh.rectOf(f);
                assertTrue(r[0] >= 0f && r[1] >= 0f && r[2] <= 1f && r[3] <= 1f && r[0] < r[2] && r[1] < r[3], mesh.name + " face " + f + " rectangle inside its cell");
                for (float c : mesh.corners(f)) assertFalse(Float.isNaN(c) || Float.isInfinite(c), mesh.name + " face " + f + " corner is a number");
            }
        }
        assertEquals(2, JibrilModels.CUFF.parts, "the cuff is a base half and a hinged lid");
        assertEquals(JibrilModels.T_PARTS, JibrilModels.TOME.parts, "the tome is a spine, two halves and four leaves");
        assertEquals(1, JibrilModels.SPEAR.parts, "the spear is one rigid piece");
    }

    @Test
    void noViewKeepsMoreQuadsThanTheModelsLimit() {
        Random r = new Random(20261);   // views from 1.5 to 15 blocks
        for (Mesh mesh : ALL) {
            Posed p = new Posed();
            int worst = 0;
            for (int k = 0; k < 3000; k++) {
                p.clearPose();
                if (mesh == JibrilModels.CUFF) p.spread()[JibrilModels.C_LID] = r.nextFloat() * JibrilModels.CUFF_OPEN;
                if (mesh == JibrilModels.TOME) JibrilModels.tomePose(r.nextFloat(), r.nextFloat(), p.bend(), p.spread());
                p.look((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian(), r.nextFloat() * 6.28f, 0.5f + 1.5f * r.nextFloat());
                float d = 1.5f + r.nextFloat() * 14f, a1 = r.nextFloat() * 6.28f, a2 = r.nextFloat() * 3.14f - 1.57f;
                int n = p.prepare(mesh, (float) (Math.cos(a1) * Math.cos(a2) * d), (float) (Math.sin(a2) * d), (float) (Math.sin(a1) * Math.cos(a2) * d), 0xFFFFFFFF, r.nextFloat());
                worst = Math.max(worst, n);
                assertTrue(n <= mesh.maxDrawn, mesh.name + " kept " + n + " quads from one view, the limit is " + mesh.maxDrawn);
            }
            assertTrue(worst >= mesh.maxDrawn / 2, mesh.name + " limit " + mesh.maxDrawn + " is not wildly above what a view keeps (" + worst + ")");
        }
    }

    @Test
    void preparedFacesHaveUvInsideTheirCellAndAreSortedFarToNear() {
        Random r = new Random(8);
        for (Mesh mesh : ALL) {
            Posed p = new Posed();
            for (int k = 0; k < 40; k++) {
                p.clearPose();
                if (mesh == JibrilModels.TOME) JibrilModels.tomePose(1f, r.nextFloat(), p.bend(), p.spread());
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
    void theCuffLidAndTheTomeLeavesMove() {
        Posed p = new Posed();
        p.clearPose();
        p.look(0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f);
        p.prepare(JibrilModels.CUFF, 0f, 0f, 6f, 0xFFFFFFFF, 0f);
        float[] shut = java.util.Arrays.copyOf(p.pts, 12 * 8);
        p.spread()[JibrilModels.C_LID] = JibrilModels.CUFF_OPEN;
        p.prepare(JibrilModels.CUFF, 0f, 0f, 6f, 0xFFFFFFFF, 0f);
        float[] open = java.util.Arrays.copyOf(p.pts, 12 * 8);
        assertFalse(java.util.Arrays.equals(shut, open), "turning the lid moves the front half of the cuff");
        float[] b = new float[Posed.MAX_PARTS], s0 = new float[Posed.MAX_PARTS], s1 = new float[Posed.MAX_PARTS];
        JibrilModels.tomePose(1f, 0f, b, s0);
        JibrilModels.tomePose(1f, 1f, b, s1);
        for (int i = 0; i < 4; i++) assertTrue(Math.abs(s0[JibrilModels.T_LEAF0 + i] - s1[JibrilModels.T_LEAF0 + i]) > 1f, "leaf " + i + " turns over the spine");
        JibrilModels.tomePose(0f, 0f, b, s0);
        assertTrue(s0[JibrilModels.T_LEFT] > 2.9f, "a shut tome folds the left half over the right");
    }

    // ---------------------------------------------------------------- the switch
    @Test
    void theSwitchDefaultsToOnAndIsAClientConfigRegisteredOnce() throws IOException {
        assertTrue(JibrilModels.enabled(), "the 3D models are on by default");
        JibrilModels.setEnabled(false);
        assertFalse(JibrilModels.enabled());
        JibrilModels.setEnabled(true);
        assertTrue(JibrilModels.enabled());
        String cfg = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/JibrilModelsConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT"), "it only changes how things look: a CLIENT config");
        assertTrue(cfg.contains("\"jibrilModels\", true") || cfg.contains("define(\"jibrilModels\", true)"), "default on");
        assertTrue(cfg.contains("\"nusmp-jibril-client.toml\""), "its own file name");
        assertTrue(cfg.contains("JibrilModels.setEnabled("), "it is mirrored into the pure switch");
        String main = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        Matcher m = Pattern.compile("JibrilModelsConfig\\.init\\(modEventBus, modContainer\\)").matcher(main);
        assertTrue(m.find(), "NUSMP registers the config");
        assertFalse(m.find(), "and only once");
    }

    // ---------------------------------------------------------------- the textures
    @Test
    void theAtlasIsARealSizedTexture() throws IOException {
        BufferedImage atlas = ImageIO.read(TEX.resolve("jibril_model_atlas.png").toFile());
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
            assertTrue(lit > 2000, "cell " + c + " has content: " + lit);
            boolean feather = c == JibrilModels.FEATHER || c == JibrilModels.FEATHER_B;
            if (!feather) assertEquals(cell * cell, opaque, "cell " + c + " is a solid material");
            else assertTrue(opaque > 2000 && opaque < cell * cell / 2, "the feather cell is a blade on a clear ground: " + opaque);
        }
    }

    // ---------------------------------------------------------------- the code
    @Test
    void nothingIsBuiltPerFrameAndTheMeshCodeStaysPure() throws IOException {
        String models = code(LAYER_DIR.resolve("JibrilModels.java"));
        assertFalse(models.contains("net.minecraft") || models.contains("com.mojang") || models.contains("Minecraft.getInstance"), "the mesh code is pure geometry");
        assertFalse(models.contains("java.util.ArrayList") || models.contains("java.util.HashMap"), "no collections in the mesh code");
        int a = models.indexOf("public int prepare(");
        int b = models.indexOf("public int at(");
        assertTrue(a > 0 && b > a, "prepare is found");
        assertFalse(models.substring(a, b).contains("new "), "Posed.prepare allocates nothing");
        for (String m : List.of("public Posed look(", "public Posed tumble(", "public Posed upright(", "public Posed axes(")) {
            int s = models.indexOf(m);
            assertTrue(s > 0, m);
            int e = models.indexOf("\n        }", s);
            assertFalse(models.substring(s, e).contains("new "), m + " allocates nothing");
        }
        String draw = code(LAYER_DIR.resolve("JibrilMeshDraw.java"));
        String body = draw.substring(draw.indexOf("public int draw("));
        assertFalse(body.contains("new "), "JibrilMeshDraw.draw allocates nothing");
        assertTrue(draw.contains("buf.hasBudget(n * 4)"), "a model that does not fit the activation budget is skipped whole");
        assertTrue(draw.contains("return 0"), "and reports 0 so the layer draws the flat sprite");
        assertFalse(draw.contains("Minecraft.getInstance") || draw.contains("com.mojang.blaze3d"), "no client singletons");
        String layer = code(LAYER_DIR.resolve("JibrilLayer.java"));
        assertFalse(layer.contains("new JibrilModels") || layer.contains("new Posed"), "the layer never builds a poser per frame");
        assertEquals(1, Pattern.compile("new JibrilMeshDraw\\(\\)").matcher(layer).results().count(), "one drawer per layer");
    }

    @Test
    void everyModelIsBehindTheSwitchTheDistanceAndKeepsItsOldSprite() throws IOException {
        String src = read(LAYER_DIR.resolve("JibrilLayer.java"));
        String layer = code(LAYER_DIR.resolve("JibrilLayer.java"));
        assertTrue(layer.contains("JibrilModels.enabled() && ctx.detail >= 0.5f"), "the setting and the particle setting gate the models");
        assertTrue(layer.contains("x * x + y * y + z * z < range * range"), "and the camera distance");
        assertTrue(Pattern.compile("MODEL_RANGE = 64f, SMALL_RANGE = 42f").matcher(src).find(), "the ranges are constants");
        for (String model : List.of("JibrilModels.CUFF", "JibrilModels.LINK", "JibrilModels.SPEAR", "JibrilModels.HALO", "JibrilModels.PLATE", "JibrilModels.TOME", "JibrilModels.CROWN"))
            assertTrue(layer.contains(model), "the layer draws " + model);
        // the old 3.20.0 draw calls are all still in the code as the fallback
        for (String old : List.of("buf.plane(CHAINRING, VfxBlend.ADD", "scrollPanel(buf,", "haloAt(buf, tilted(c.x, haloY + 0.06f * hs, c.z, 0.75f)", "haloAt(buf, tilted(c.x, hy, c.z, 0.75f)", "haloAt(buf, tilted(c.x, hy + 0.05f, c.z, 0.75f)",
                "haloAt(buf, face.lift(-0.04f)", "haloAt(buf, face.lift(-0.05f)", "ribbon(buf, PILLAR,", "wingQuad(buf, VfxBlend.ALPHA"))
            assertTrue(layer.contains(old), "the 3.20.0 call is kept: " + old);
        for (String n : List.of("jibril_ring", "jibril_glyph", "jibril_seal", "jibril_halo", "jibril_wave", "jibril_rays", "jibril_pillar", "jibril_bars", "jibril_curtain", "jibril_beam", "jibril_chain", "jibril_chainring", "jibril_feather",
                "jibril_wing", "jibril_flare", "jibril_mote", "jibril_scroll", "jibril_emblem", "jibril_petal", "jibril_ribbon", "jibril_spiral", "jibril_prism", "jibril_model_atlas")) {
            assertTrue(src.contains("\"" + n + "\""), "the layer still names " + n);
            assertTrue(Files.size(TEX.resolve(n + ".png")) > 200, n + ".png exists and is not empty");
        }
        assertEquals(200, Integer.parseInt(Pattern.compile("case JIBRIL_KINGDOM -> (\\d+);").matcher(layer).results().findFirst().orElseThrow().group(1)), "the Kingdom budget stays 200 vertices");
        assertEquals(200, Integer.parseInt(Pattern.compile("case JIBRIL_WINGS -> (\\d+);").matcher(layer).results().findFirst().orElseThrow().group(1)), "the Wings budget stays 200 vertices");
        // every model call is a helper that returns the quads drawn, and the layer never asks for more than one cuff pair, two links, one spear, one halo, three plates, one tome, one crown
        assertEquals(1, Pattern.compile("private int (cuff|link|spear|halo3d|plate|tome|crown)\\(").matcher(layer).results().map(m -> m.group(1)).filter("tome"::equals).count());
        assertTrue(layer.contains("m3d && k != 1"), "the shackles use two cuffs and keep the middle sprite ring");
        assertTrue(layer.contains("i < 3; i++") && layer.contains("plate(buf,"), "three law plates over the Kingdom");
    }

    // ---------------------------------------------------------------- the owner's rules
    @Test
    void theTimeMagicLookIsUntouchedAndTheGeneratorIsItsOwnFile() throws IOException {
        for (String f : List.of("JibrilModels.java", "JibrilMeshDraw.java"))
            assertFalse(read(LAYER_DIR.resolve(f)).contains("time_"), f + " never names a Time Magic texture");
        String gen = read(ROOT.resolve("tools/gen_jibril_model_textures.py"));
        assertFalse(gen.contains("time_"), "the generator never writes a Time Magic texture");
        assertTrue(gen.contains("jibril_model_atlas"), "it writes the atlas");
        assertTrue(Files.exists(ROOT.resolve("tools/gen_jibril_vfx_textures.py")), "the old generator is kept");
    }

    @Test
    void theNewPreviewScenesAndTheDocExist() throws IOException {
        String scenes = read(ROOT.resolve("tools/vfx_preview/scenes.d/jibril_models.json"));
        for (String s : List.of("jibril_models_shackles", "jibril_models_cuff_close", "jibril_models_link_close", "jibril_models_spear_fall", "jibril_models_spear_close", "jibril_models_judgment_far", "jibril_models_word", "jibril_models_command",
                "jibril_models_kingdom", "jibril_models_wings"))
            assertTrue(scenes.contains("\"" + s + "\""), "a preview scene: " + s);
        String doc = read(ROOT.resolve("docs/vfx_remake/jibril_models.md"));
        for (String s : List.of("CUFF", "LINK", "SPEAR", "HALO", "PLATE", "TOME", "CROWN", "jibrilModels", "NEW", "REPLACED", "KEPT", "worst"))
            assertTrue(doc.toLowerCase().contains(s.toLowerCase()), "the doc covers " + s);
    }
}
