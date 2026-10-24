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
 * Why: the Star V2 look cannot be profiled or seen here, so its rules are pinned by reading the real source. (1) Server safety (rule 9): the common classes name no client class and no packet; the options are
 * CLIENT configs with their own file. (2) Frame cost (rule 11): the layer and the prop painter allocate nothing per frame in the V2 path, build no string or location, draw only alpha or additive quads (no
 * old FIRE / WIND blend grade, no bloom), count through {@code ctx.seg}, read their counts and budget from {@link StarV2}. (3) Wiring: one registration, V1 draws the old starlight beam, STAR_MAGIC and its layer
 * are not edited. (4) Reduced flashes gate every hard flash, ray and twinkle. (5) The Time Magic VFX is not touched.
 */
class StarV2SourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    /** The body of a method: from its signature to the matching closing brace. */
    private static String method(String src, String name) {
        java.util.regex.Matcher decl = Pattern.compile("\\b(?:void|int|float|boolean|ResourceLocation)\\s+" + name + "\\(").matcher(src);
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
    void theCommonClassesAreServerSafePureAndTheOptionsAreAClientConfig() {
        for (String f : new String[]{"vfx/StarV2.java", "vfx/StarV2SelfTests.java"}) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " names a client class");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload") || c.contains("VfxSpawn") || c.contains("ServerPlayer"), f + " sends or keeps per-player state");
        }
        assertFalse(code(read("vfx/StarV2.java")).contains("net.minecraft"), "the rules stay pure");
        String cfg = code(read("vfx/client/StarV2Config.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT, SPEC, \"nusmp-star-v2-client.toml\")"), "a client option with its own file");
        assertFalse(cfg.contains("Type.SERVER") || cfg.contains("Type.COMMON"));
        assertTrue(cfg.contains("b.push(\"vfx\")") && cfg.contains("b.push(\"element\")") && cfg.contains(".defineEnum(\"star\", StarV2.DEFAULT_STYLE)") && cfg.contains(".define(\"starReducedFlashes\", StarV2.DEFAULT_REDUCED_FLASHES)"),
                "vfx.element.star and vfx.element.starReducedFlashes");
        assertTrue(code(read("vfx/StarV2.java")).contains("\"nusmp-star-v2-client.toml\""));
        String main = code(read("NUSMP.java"));
        assertEquals(1, main.split("StarV2Config\\.init\\(modEventBus, modContainer\\)", -1).length - 1, "the options are registered once, in the client block");
        assertTrue(main.indexOf("StarV2Config.init") > main.indexOf("FMLEnvironment.dist == Dist.CLIENT"), "inside the client block of the constructor");
    }

    @Test
    void theLayerAndThePainterAllocateNothingPerFrameInTheNewPath() {
        String c = code(read("vfx/client/layer/StarMagicLayer.java"));
        String v2only = c.replace(method(c, "oldBeam"), "");                           // the V1 path is the old code and allocates like it did
        for (String line : v2only.split("\n")) {
            if (!line.contains("new ")) continue;
            assertTrue(line.contains("static final") || line.contains("new ResourceLocation[StarV2"), "an allocation outside a static initialiser and the V1 path: " + line.trim());
        }
        for (String m : new String[]{"node", "link", "conjunction", "flare", "pane", "lance", "whip", "dome", "rx", "whiteArgb", "goldArgb", "sprite", "ground", "strip", "stripUV"}) {
            String b = method(c, m);
            assertFalse(b.contains("new ") || b.contains("String.format") || b.contains("\" +") || b.contains("Minecraft.getInstance") || b.contains("ResourceLocation.") || b.contains("byName(")
                    || b.contains("ctx.rel(") || b.contains("inst.random()") || b.contains("toLowerCase") || b.contains("VfxPose.") || b.contains("VfxBloom"), m + " allocates or builds something per frame");
        }
        assertFalse(c.contains("VfxBlend.FIRE") || c.contains("VfxBlend.WIND") || c.contains("VfxBlend.WATER") || c.contains("VfxBlend.NEGATIVE") || c.contains("VfxBloom"), "alpha and additive quads only");
        assertTrue(c.split("ctx\\.seg\\(").length - 1 >= 6, "counts shrink with the level of detail");
        for (String k : new String[]{"StarV2.NODE_COMET_SPARKS", "StarV2.LINK_SPARKS", "StarV2.CONJ_DUST", "StarV2.PANE_SHARDS", "StarV2.LANCE_SPARKS", "StarV2.WHIP_PIECES", "StarV2.DOME_STARS", "StarV2.DOME_DUST"})
            assertTrue(c.contains(k), "the layer reads " + k + " from the rules");
        assertTrue(method(c, "vertexBudget").contains("StarV2.vertexBudget(s)"), "the activation budget is the cap in quads times four");
        assertTrue(c.contains("EnumSet.copyOf(StarV2.SHAPES)"));

        String p = code(read("client/prop/StarPropPainter.java"));
        for (String line : p.split("\n")) assertFalse(line.contains(" new ") || line.contains("(new "), "the painter allocates: " + line.trim());
        for (String m : new String[]{"node", "pane", "inOut"}) {
            String b = method(p, m);
            assertFalse(b.contains("String.format") || b.contains("\" +") || b.contains("AuraRender.tex(") || b.contains("toLowerCase"), m + " builds a string or location per frame");
        }
        assertTrue(method(p, "register").contains("AuraRender.tex("), "the textures are resolved once, in register");
        assertTrue(p.contains("StarV2.nodeQuads(") && p.contains("drawnNear"), "the per-frame ceiling of full stars");
    }

    @Test
    void oneRegistrationAndV1DrawsTheOldStarlightBeam() {
        String mgr = code(read("vfx/client/VfxManager.java"));
        assertTrue(mgr.contains("register(new com.newuniverse.nusmp.vfx.client.layer.StarMagicLayer())"));
        assertEquals(1, mgr.split("StarMagicLayer", -1).length - 1, "registered once");
        String c = code(read("vfx/client/layer/StarMagicLayer.java"));
        String render = method(c, "render");
        assertTrue(render.indexOf("!StarV2.v2Look()") >= 0 && render.indexOf("oldBeam(inst, ctx, buf)") > render.indexOf("!StarV2.v2Look()"), "V1: the old beam");
        String old = method(c, "oldBeam");
        assertTrue(old.contains("VfxTextures.GLOW") && old.contains("VfxTextures.SPARK") && old.contains("ctx.seg(12, 5)"), "the old look of STAR_MAGIC: a glow beam and 12 sparks");
        assertTrue(read("vfx/client/layer/ElementIdentityLayer.java").contains("case STAR_MAGIC ->"), "the old layer still owns STAR_MAGIC");
        assertFalse(read("vfx/client/layer/ElementIdentityLayer.java").contains("Star" + "V2"), "and is not edited");
        assertTrue(method(c, "vertexBudget").contains("StarV2.v2Look() ? StarV2.vertexBudget(s) : 160"), "V1 keeps a small budget");
        String p = code(read("client/prop/StarPropPainter.java"));
        assertTrue(p.contains("if (!StarV2.v2Look())"), "the painter has its V1 path");
    }

    @Test
    void reducedFlashesGateEveryHardFlashRayFlareAndTwinkle() {
        String c = code(read("vfx/client/layer/StarMagicLayer.java"));
        assertTrue(c.contains("StarV2.hardFlash(") && c.contains("StarV2.softFlash(") && c.contains("StarV2.twinkle(") && c.contains("StarV2.spin("), "the layer uses the pure rules");
        assertTrue(method(c, "flare").contains("if (reduced)"), "the lens flare becomes a soft glow");
        String conj = method(c, "conjunction");
        assertTrue(conj.contains("if (!reduced)") && conj.indexOf("TEX[RAYS]") > conj.indexOf("if (!reduced)"), "speed lines are never drawn with reduced flashes");
        assertTrue(method(c, "dome").contains("if (!reduced) sprite(TEX[BRAYS]"), "the light column of the dome is not drawn with reduced flashes");
        assertTrue(method(c, "lance").contains("if (reduced) sprite(TEX[GLOW]"), "the lance head is a soft glow with reduced flashes");
        assertTrue(c.contains("reduced = StarV2.reducedFlashes()"));
        String p = code(read("client/prop/StarPropPainter.java"));
        assertTrue(p.contains("StarV2.reducedFlashes()") && p.contains("StarV2.twinkle(") && p.contains("StarV2.spin(") && p.contains("!reduced && e.maxLife() - age < 60f"), "the painter follows the option too (no blinking, no twinkle, no spin)");
        assertFalse(c.contains("TimeMagic") || p.contains("TimeMagic") || code(read("vfx/StarV2.java")).contains("TimeMagic"), "the Time Magic VFX is not touched");
    }
}
