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
 * Why: the Fire V2 look cannot be profiled or seen here, so its rules are pinned by reading the real source. (1) Server safety (rule 9): the common classes name no client class and no packet; the options are
 * CLIENT configs with their own file. (2) Frame cost (rule 11): the painter allocates nothing per frame (every {@code new} is a static initialiser), builds no location or string, draws only alpha or additive quads
 * (no old FIRE / WIND blend grade, no old bloom), counts through {@code ctx.seg}, reads its counts and its vertex budget from {@link FireV2}. (3) Wiring: one registration after the three old fire layers, V1 hands
 * everything back to them, the old layers are not edited, the kept sigils of the Flame Zone and the Flame Cast are still drawn from the old sprites. (4) Reduced flashes: hard flashes and rays are gated by the option.
 * (5) The tools and docs are there and the Time Magic VFX is not touched.
 */
class FireV2SourceScanTest {
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
    void theCommonClassesAreServerSafePurePacketFreeAndTheOptionsAreAClientConfig() {
        for (String f : new String[]{"vfx/FireV2.java", "vfx/FireV2SelfTests.java"}) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " names a client class");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload") || c.contains("VfxSpawn") || c.contains("ServerPlayer"), f + " sends or keeps per-player state");
        }
        assertFalse(code(read("vfx/FireV2.java")).contains("net.minecraft"), "the rules stay pure");
        String cfg = code(read("vfx/client/FireV2Config.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT, SPEC, \"nusmp-fire-v2-client.toml\")"), "a client option with its own file");
        assertFalse(cfg.contains("Type.SERVER") || cfg.contains("Type.COMMON"));
        assertTrue(cfg.contains("b.push(\"vfx\")") && cfg.contains("b.push(\"element\")") && cfg.contains(".defineEnum(\"fire\", FireV2.DEFAULT_STYLE)") && cfg.contains(".define(\"fireReducedFlashes\", FireV2.DEFAULT_REDUCED_FLASHES)"),
                "vfx.element.fire and vfx.element.fireReducedFlashes");
        assertTrue(code(read("vfx/FireV2.java")).contains("\"nusmp-fire-v2-client.toml\""));
        String main = code(read("NUSMP.java"));
        assertEquals(1, main.split("FireV2Config\\.init\\(modEventBus, modContainer\\)", -1).length - 1, "the options are registered once, in the client block");
        assertTrue(main.indexOf("FireV2Config.init") > main.indexOf("FMLEnvironment.dist == Dist.CLIENT"), "inside the client block of the constructor");
        assertEquals(1, main.split("FireV2SelfTests\\.register\\(\\)", -1).length - 1);
        assertTrue(read("vfx/FireV2SelfTests.java").contains("\"magic.fire_v2\""));
    }

    @Test
    void thePainterAllocatesNothingPerFrameAndDrawsOnlyAlphaAndAdditiveQuads() {
        String c = code(read("vfx/client/layer/FireV2Layer.java"));
        for (String line : c.split("\n")) {
            if (!line.contains("new ")) continue;
            assertTrue(line.contains("static final") || line.contains("new ResourceLocation[2]") && line.contains("TEX ="), "an allocation outside a static initialiser: " + line.trim());
        }
        for (String m : new String[]{"render", "lion", "spear", "pillar", "vortex", "burst", "spiral", "wild", "cast", "zone", "fxBurst", "trail", "explosion", "flashA", "flashTicks", "rays", "plane", "ground",
                "groundPose", "facing", "spinPlane", "strip", "upright", "dirSprite", "sprite", "sideOf", "argb", "glowArgb", "hotArgb", "whiteArgb", "tex", "texT", "fr"}) {
            String b = method(c, m);
            assertFalse(b.contains("new ") || b.contains("String.format") || b.contains("\" +") || b.contains("Minecraft.getInstance") || b.contains("ResourceLocation.") || b.contains("byName(")
                    || b.contains("ctx.rel(") || b.contains("inst.random()") || b.contains("toLowerCase") || b.contains("VfxPose.") || b.contains("VfxBloom"), m + " allocates or builds something per frame");
        }
        assertFalse(c.contains("VfxBlend.FIRE") || c.contains("VfxBlend.WIND") || c.contains("VfxBlend.WATER") || c.contains("VfxBlend.NEGATIVE") || c.contains("VfxBloom"), "alpha and additive quads only");
        assertTrue(c.split("ctx\\.seg\\(").length - 1 >= 25, "counts shrink with the level of detail");
        for (String k : new String[]{"FireV2.LION_TONGUES", "FireV2.SPEAR_CHIPS", "FireV2.PILLAR_LOOSE", "FireV2.VORTEX_STRANDS", "FireV2.BURST_SMOKE", "FireV2.SPIRAL_POINTS", "FireV2.WILD_BALLS", "FireV2.WILD_WAVES", "FireV2.WILD_UPRIGHTS",
                "FireV2.CAST_EMBERS", "FireV2.ZONE_TONGUES", "FireV2.FXBURST_CHIPS", "FireV2.TRAIL_PUFFS", "FireV2.EXPL_TONGUES"})
            assertTrue(c.contains(k), "the painter reads " + k + " from the rules");
        assertTrue(method(c, "vertexBudget").contains("FireV2.vertexBudget(s)"), "the activation budget is the cap in quads times four");
        assertTrue(c.contains("EnumSet.copyOf(FireV2.SHAPES)"));
    }

    @Test
    void oneRegistrationAfterTheThreeOldLayersAndV1HandsEverythingBack() {
        String mgr = code(read("vfx/client/VfxManager.java"));
        int magic = mgr.indexOf("register(new FlameMagicLayer())"), spell = mgr.indexOf("register(new FireSpellLayer())"), fx = mgr.indexOf("register(new com.newuniverse.nusmp.vfx.client.layer.FlameFxLayer())"),
                v2 = mgr.indexOf("register(new com.newuniverse.nusmp.vfx.client.layer.FireV2Layer())");
        assertTrue(magic > 0 && spell > 0 && fx > 0 && v2 > fx && v2 > spell && v2 > magic, "FireV2Layer is registered after the three old fire layers (the manager keeps the last one registered for a shape)");
        assertEquals(1, mgr.split("FireV2Layer", -1).length - 1, "registered once");
        String c = code(read("vfx/client/layer/FireV2Layer.java"));
        String render = method(c, "render");
        assertTrue(render.indexOf("!FireV2.v2Look()") >= 0 && render.indexOf("old(inst.shape).render(inst, ctx, buf)") > render.indexOf("!FireV2.v2Look()"), "V1: the old layer draws");
        assertTrue(c.contains("case FIRE_LION, FIRE_SPEAR, FIRE_PILLAR, FIRE_BURST, FIRE_SPIRAL, FIRE_WILD -> OLD_SPELL;") && c.contains("case FLAMEFX_CAST, FLAMEFX_ZONE, FLAMEFX_BURST -> OLD_FX;"), "each shape goes back to the layer that owned it");
        assertTrue(method(c, "defaultDuration").contains("old(s).defaultDuration(s)") && method(c, "defaultColor").contains("old(s).defaultColor(s)") && method(c, "onSpawn").contains("old(inst.shape).onSpawn(inst)"));
        assertTrue(method(c, "vertexBudget").contains("old(s).vertexBudget(s)"), "V1 keeps the old budget");
        assertTrue(c.contains("OLD_SPELL = new FireSpellLayer(), OLD_FX = new FlameFxLayer(), OLD_MAGIC = new FlameMagicLayer()"));
    }

    @Test
    void theOldLayersAreNotEditedAndTheKeptSigilsAreStillDrawnFromTheOldSprites() {
        for (String f : new String[]{"vfx/client/layer/FireSpellLayer.java", "vfx/client/layer/FlameFxLayer.java", "vfx/client/layer/FlameMagicLayer.java"})
            assertFalse(read(f).contains("FireV2"), f + " is not edited");
        assertTrue(read("vfx/client/layer/FireSpellLayer.java").contains("EnumSet.of(VfxShape.FIRE_LION, VfxShape.FIRE_SPEAR, VfxShape.FIRE_PILLAR, VfxShape.FIRE_BURST,"), "the old layer still owns its shapes");
        String c = code(read("vfx/client/layer/FireV2Layer.java"));
        assertTrue(c.contains("SIGIL_LION = VfxTextures.byName(\"flamefx_lion\")") && c.contains("SIGIL_RING = VfxTextures.byName(\"flamefx_ring\")"), "the old sun-lion sigil and ring sprites");
        String zone = method(c, "zone");
        assertTrue(zone.contains("groundPose(SIGIL_LION, VfxBlend.ADD, cx, gy, cz, R * 0.92f * open, ft * 0.018f,") && zone.contains("groundPose(SIGIL_RING, VfxBlend.ADD, cx, gy + 0.03f, cz, R * 1.04f * open, -ft * 0.03f,"),
                "the zone keeps its sun-lion sigil and ring with the old size and spin");
        String cast = method(c, "cast");
        assertTrue(cast.contains("spinPlane(SIGIL_LION, ox, oy, oz, ft * 0.07f, 0.65f * p * open") && cast.contains("spinPlane(SIGIL_RING,"), "the cast keeps its charge sigil");
        assertTrue(c.contains("lion()") && c.contains("FIRE_LION") && read("vfx/client/layer/FireSpellLayer.java").contains("FIRE_LION"), "the cel lion head is drawn");
        assertTrue(method(c, "lion").contains("tex(LION + fr(0, 2))"), "FIRE_LION draws the cel lion head");
    }

    @Test
    void reducedFlashesGateEveryHardFlashRayAndPulse() {
        String c = code(read("vfx/client/layer/FireV2Layer.java"));
        assertTrue(method(c, "rays").contains("if (reduced"), "radial speed lines are never drawn with reduced flashes");
        assertTrue(method(c, "flashA").contains("if (reduced)") && method(c, "flashA").contains("texT(HALO)"), "a flash becomes a soft halo with reduced flashes");
        assertTrue(c.contains("FireV2.hardFlash(ia, ticks, reduced)") && c.contains("FireV2.softFlash(ia, ticks, reduced)"), "first-tick flashes use the pure rules");
        assertTrue(c.contains("FireV2.reducedFlashes()") && c.contains("FireV2.frame(ia, offset, n, reduced)"), "the flip-book period follows the option");
        assertTrue(c.contains("FireV2.pulse(") && c.contains("fa = reduced ? ft * 0.4f : ft"), "pulses and flicker are slowed or flattened");
        assertFalse(c.contains("TimeMagic"), "the Time Magic VFX is not touched");
    }

    @Test
    void theDocsToolsAndPreviewsAreThere() {
        for (String f : new String[]{"tools/gen_fire_v2_textures.py", "tools/vfx_preview/fire_ingame.py", "tools/vfx_preview/scenes.d/fire_v2.json", "docs/vfx_remake/fire_ingame.md", "docs/previews/vfx_remake/fire_new/fire_ingame.png",
                "docs/previews/vfx_remake/fire_new/fire_wild_closeup.png", "docs/previews/vfx_remake/fire_new/fire_ingame_budget.json", "docs/vfx_remake/fire_new.md"})
            assertTrue(Files.exists(ROOT.resolve(f)), f);
        assertFalse(code(read("vfx/FireV2.java")).contains("TimeMagic"), "the Time Magic VFX is not touched");
    }
}
