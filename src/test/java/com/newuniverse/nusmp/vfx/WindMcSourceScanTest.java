package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Wind pixel look cannot be profiled or seen here, so its rules are pinned by reading the real source. (1) Server safety (rule 9): the common classes name no client class and no packet; the
 * option is a CLIENT config with its own file. (2) Frame cost (rule 11): the painter allocates nothing per frame (every {@code new} is a static initialiser), builds no location or string, reads the age of
 * the tick (never the partial tick, "on twos"), draws only alpha-blended quads, counts through {@code ctx.seg}, and has the activation budget of {@link WindMc#vertexBudget}. (3) Wiring: one registration
 * after the two old wind layers, V1 hands everything back to them, the Tornado and Spirit of Zephyr are not in the new layer and the old layers are not edited, FX_WIND_GUST falls back to dust under V1,
 * the pixel textures are sampled nearest, the self-test and the tools are there.
 */
class WindMcSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    /** The body of a method: from its signature to the matching closing brace. */
    private static String method(String src, String name) {
        java.util.regex.Matcher decl = Pattern.compile("\\b(?:void|int|float|boolean|ResourceLocation|AbstractVfxLayer)\\s+" + name + "\\(").matcher(src);
        assertTrue(decl.find(), "method " + name + " not found");
        int i = decl.start();
        int open = src.indexOf('{', i), depth = 0, j = open;
        for (; j < src.length(); j++) {
            char c = src.charAt(j);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, j + 1);
    }

    @Test
    void theCommonClassesAreServerSafePurePacketFreeAndTheOptionIsAClientConfig() {
        for (String f : new String[]{"vfx/WindMc.java", "vfx/WindMcSelfTests.java"}) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " names a client class");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload") || c.contains("VfxSpawn") || c.contains("ServerPlayer") || c.contains("static ServerPlayer"), f + " sends or keeps per-player state");
        }
        assertFalse(code(read("vfx/WindMc.java")).contains("net.minecraft"), "the rules stay pure");
        String cfg = code(read("vfx/client/WindMcConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT, SPEC, WindMc.CONFIG_FILE"), "a client option with its own file");
        assertFalse(cfg.contains("Type.SERVER") || cfg.contains("Type.COMMON"));
        assertTrue(cfg.contains("b.push(\"vfx\")") && cfg.contains("b.push(\"element\")") && cfg.contains(".defineEnum(\"wind\", WindMc.DEFAULT_STYLE)"), "vfx.element.wind");
        assertTrue(code(read("vfx/WindMc.java")).contains("\"nusmp-wind-mc-client.toml\""));
        String main = code(read("NUSMP.java"));
        assertEquals(1, main.split("WindMcConfig\\.init\\(modEventBus, modContainer\\)", -1).length - 1, "the option is registered once, in the client block");
        assertTrue(main.indexOf("WindMcConfig.init") > main.indexOf("FMLEnvironment.dist == Dist.CLIENT"), "inside the client block of the constructor");
        assertEquals(1, main.split("WindMcSelfTests\\.register\\(\\)", -1).length - 1);
        assertTrue(read("vfx/WindMcSelfTests.java").contains("\"magic.wind_mc\""));
    }

    @Test
    void thePainterAllocatesNothingPerFrameAndOnlyReadsTheTickAge() {
        String c = code(read("vfx/client/layer/WindMcLayer.java"));
        for (String line : c.split("\n")) {
            if (!line.contains("new ")) continue;
            assertTrue(line.contains("static final") || line.contains("new ResourceLocation[2]") && line.contains("TEX ="), "an allocation outside a static initialiser: " + line.trim());
        }
        for (String m : new String[]{"render", "slash", "emperor", "gale", "ring", "plane", "strip", "swallow", "crescentAxes", "sideOf", "argb", "tex"}) {
            String b = method(c, m);
            assertFalse(b.contains("new ") || b.contains("String.format") || b.contains("\" +") || b.contains("Minecraft.getInstance") || b.contains("ResourceLocation.") || b.contains("byName(")
                    || b.contains("ctx.rel(") || b.contains("inst.random()") || b.contains("toLowerCase"), m + " allocates or builds something per frame");
        }
        assertFalse(c.contains("partialTick"), "the pixel look is drawn on twos: it never reads the partial tick");
        assertTrue(c.contains("inst.age()") && c.contains("WindMc.onTwos(age)") && c.contains("WindMc.frame(age)") && c.contains("WindMc.step4("), "stepped time, flip-book and four alpha steps");
        assertFalse(c.contains("VfxBlend.ADD") || c.contains("VfxBlend.WIND") || c.contains("VfxBlend.FIRE") || c.contains("VfxBloom"), "hard alpha-blended pixels only: no additive glow");
        assertTrue(c.split("ctx\\.seg\\(").length - 1 >= 13, "counts shrink with the level of detail");
        assertTrue(c.contains("WindMc.SLASH_GHOSTS") && c.contains("WindMc.EMPEROR_RAYS") && c.contains("WindMc.GALE_SWALLOWS") && c.contains("WindMc.RING_RIBBONS"), "the painter reads its counts from the rules");
        assertTrue(method(c, "vertexBudget").contains("WindMc.vertexBudget(s)"), "the activation budget is the cap in quads times four");
        assertTrue(c.contains("EnumSet.copyOf(WindMc.SHAPES)"));
    }

    @Test
    void oneRegistrationAfterTheOldLayersAndV1HandsEverythingBack() {
        String mgr = code(read("vfx/client/VfxManager.java"));
        int magic = mgr.indexOf("register(new WindMagicLayer())"), spell = mgr.indexOf("register(new WindSpellLayer())"), mc = mgr.indexOf("register(new com.newuniverse.nusmp.vfx.client.layer.WindMcLayer())");
        assertTrue(magic > 0 && spell > 0 && mc > spell && mc > magic, "WindMcLayer is registered after both old wind layers (the manager keeps the last one registered for a shape)");
        assertEquals(1, mgr.split("WindMcLayer", -1).length - 1, "registered once");
        assertTrue(mgr.contains("if (layer != null && !layer.handles(shape)) layer = null;"), "a shape the layer gives up falls back to the dust");
        assertTrue(code(read("vfx/client/AbstractVfxLayer.java")).contains("public boolean handles(VfxShape shape) { return true; }"));
        String c = code(read("vfx/client/layer/WindMcLayer.java"));
        String render = method(c, "render");
        assertTrue(render.indexOf("!WindMc.pixelLook()") >= 0 && render.indexOf("o.render(inst, ctx, buf)") > render.indexOf("!WindMc.pixelLook()"), "V1: the old layer draws");
        assertTrue(c.contains("case WIND_SLASH, WIND_RING -> OLD_MAGIC;") && c.contains("case WIND_EMPEROR, WIND_GALE -> OLD_SPELL;"), "each shape goes back to the layer that owned it");
        assertTrue(c.contains("shape != VfxShape.FX_WIND_GUST || WindMc.pixelLook()"), "FX_WIND_GUST is a shape without a layer again under V1");
        assertTrue(method(c, "defaultDuration").contains("o.defaultDuration(s)") && method(c, "defaultColor").contains("o.defaultColor(s)") && method(c, "onSpawn").contains("o.onSpawn(inst)"));
    }

    @Test
    void theTornadoAndZephyrAreNotInTheNewLayerAndTheOldLayersAreNotEdited() {
        String c = code(read("vfx/client/layer/WindMcLayer.java"));
        assertFalse(c.contains("WIND_TORNADO") || c.contains("WIND_ZEPHYR") || c.contains("tornado(") || c.contains("zephyr("), "the new layer knows nothing of the Tornado or Spirit of Zephyr");
        String spell = read("vfx/client/layer/WindSpellLayer.java");
        assertFalse(spell.contains("WindMc"), "WindSpellLayer is not edited");
        assertTrue(spell.contains("EnumSet.of(VfxShape.WIND_TORNADO, VfxShape.WIND_GALE, VfxShape.WIND_EMPEROR, VfxShape.WIND_ZEPHYR)"), "WindSpellLayer still owns all four of its shapes");
        assertTrue(spell.contains("private void tornado(VfxInstance inst") && spell.contains("private void zephyr(VfxInstance inst") && spell.contains("WIND_BAND, VfxBlend.ALPHA, pose, rb, rt, hw, ctx.seg(12, 8), 3"), "the Tornado and Zephyr code is still there");
        String magic = read("vfx/client/layer/WindMagicLayer.java");
        assertFalse(magic.contains("WindMc"), "WindMagicLayer is not edited");
        assertTrue(magic.contains("EnumSet.of(VfxShape.WIND_SLASH, VfxShape.WIND_RING)"));
        assertFalse(code(read("vfx/client/VfxManager.java")).contains("WIND_TORNADO"), "the manager does not special-case the Tornado");
    }

    @Test
    void thePixelTexturesAreSampledNearestAndTheProbeStillWrapsEveryLayer() {
        String buf = code(read("vfx/client/VfxVertexBuffer.java"));
        assertTrue(buf.contains("setFilter(!com.newuniverse.nusmp.vfx.WindMc.isPixelTexture(tex.getPath()), false)"), "the pixel textures are not smoothed");
        assertEquals(1, buf.split("setFilter\\(", -1).length - 1, "one filter decision per batch, not per quad");
        String mgr = code(read("vfx/client/VfxManager.java"));
        assertTrue(mgr.contains("PerfProbe.end(layerSection(inst.layer), lt)"), "the performance probe times every layer, WindMcLayer included (c.vfx.layer.WindMcLayer)");
        assertTrue(mgr.contains("buffer.beginActivation(inst.layer.vertexBudget(inst.shape)"), "the activation budget is asked from the layer");
    }

    @Test
    void theDocsToolsAndPreviewsAreThere() {
        for (String f : new String[]{"tools/gen_wind_mc_textures.py", "tools/vfx_preview/wind_mc_ingame.py", "tools/vfx_preview/scenes.d/wind_mc.json", "docs/vfx_remake/wind_mc_ingame.md", "docs/previews/vfx_remake/mc/wind_mc_ingame.png",
                "docs/previews/vfx_remake/mc/wind_mc_ingame_budget.json", "docs/vfx_remake/wind_mc.md"})
            assertTrue(Files.exists(ROOT.resolve(f)), f);
        assertFalse(code(read("vfx/client/layer/WindMcLayer.java")).contains("TimeMagic") || code(read("vfx/WindMc.java")).contains("TimeMagic"), "the Time Magic VFX is not touched");
    }
}
