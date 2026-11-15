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
 * Why: the Water hybrid cannot be profiled or seen here, so its rules are pinned by reading the real source. (1) Server safety (rule 9): the common classes name no client class and no packet, keep no
 * per-player state; the option is a CLIENT config with its own file; there is no new packet and no server work. (2) Frame cost (rule 11): the scene, the toon shading and the buffers allocate nothing per
 * frame (every {@code new} is a field or static initialiser), the painter builds no location, string, random or vector per frame, reads the effect's own age, the mesh draw is three passes with the depth
 * written only for the solid one and the glass sprites are drawn after it. (3) Wiring: one registration after the old water layers, V1 hands everything back to them, the frame plan runs after the manager's
 * own plan, the mesh flush runs just before the sprite flush, the old layers, the old WaterSpellLayer shapes and the Time Magic exemption are not edited, the self-test and the tools are there.
 */
class WaterBcSourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");
    private static final String[] PURE = {"vfx/WaterBc.java", "vfx/WaterBcMeshes.java", "vfx/WaterBcMeshBuffer.java", "vfx/WaterBcToon.java", "vfx/WaterBcScene.java", "vfx/WaterBcBudget.java", "vfx/WaterBcSelfTests.java"};

    /** The body of a method: from its signature to the matching closing brace. */
    private static String method(String src, String name) {
        java.util.regex.Matcher decl = Pattern.compile("\\b(?:void|int|float|boolean|ResourceLocation|AbstractVfxLayer|Piece)\\s+" + name + "\\(").matcher(src);
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

    private static void noAllocationOutsideFieldInitialisers(String file, String src) {
        for (String line : code(src).split("\n")) {
            if (!line.contains("new ")) continue;
            String t = line.trim();
            assertTrue(t.startsWith("private final ") || t.startsWith("public final ") || t.startsWith("private static final ") || t.startsWith("public static final ") || t.startsWith("static final "),
                    file + ": an allocation outside a field initialiser: " + t);
        }
    }

    @Test
    void theCommonClassesAreServerSafePurePacketFreeAndTheOptionIsAClientConfig() {
        for (String f : PURE) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " names a client class");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload") || c.contains("VfxSpawn") || c.contains("ServerPlayer"), f + " sends or keeps per-player state");
            if (!f.endsWith("SelfTests.java")) assertFalse(c.contains("net.minecraft"), f + " must stay pure");
        }
        String cfg = code(read("vfx/client/WaterBcConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT, SPEC, \"nusmp-water-bc-client.toml\""), "a client option with its own file name, written as a literal");
        assertFalse(cfg.contains("Type.SERVER") || cfg.contains("Type.COMMON"));
        assertTrue(cfg.contains("b.push(\"vfx\")") && cfg.contains("b.push(\"element\")") && cfg.contains(".defineEnum(\"water\", WaterBc.DEFAULT_STYLE)"), "vfx.element.water");
        assertTrue(code(read("vfx/WaterBc.java")).contains("\"nusmp-water-bc-client.toml\""));
        String main = code(read("NUSMP.java"));
        assertEquals(1, main.split("WaterBcConfig\\.init\\(modEventBus, modContainer\\)", -1).length - 1, "the option is registered once, in the client block");
        assertTrue(main.indexOf("WaterBcConfig.init") > main.indexOf("FMLEnvironment.dist == Dist.CLIENT"), "inside the client block of the constructor");
        assertEquals(1, main.split("WaterBcSelfTests\\.register\\(\\)", -1).length - 1);
        assertTrue(read("vfx/WaterBcSelfTests.java").contains("\"magic.water_bc\""));
        for (String f : new String[]{"vfx/client/WaterBcLayer.java", "vfx/client/layer/WaterBcLayer.java", "vfx/client/WaterBcFrame.java", "vfx/client/WaterBcDraw.java"}) {
            if (!Files.exists(JAVA.resolve(f))) continue;
            String c = code(read(f));
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload") || c.contains("VfxSpawn"), f + " sends something");
        }
        assertFalse(code(read("book/WaterArts.java")).contains("WaterBc"), "the server side of the water magic is not touched");
    }

    @Test
    void theSceneTheToonShadingAndTheBuffersAllocateNothingPerFrame() {
        noAllocationOutsideFieldInitialisers("WaterBcScene", read("vfx/WaterBcScene.java"));
        noAllocationOutsideFieldInitialisers("WaterBcToon", read("vfx/WaterBcToon.java"));
        String buf = code(read("vfx/WaterBcMeshBuffer.java"));
        for (String line : buf.split("\n")) if (line.contains("new ")) assertTrue(line.contains("public WaterBcMeshBuffer FRAME") || line.contains("FRAME =") || line.contains("data[p] = new") || line.contains("colour[p] = new") || line.contains("new float[PASSES]")
                || line.contains("new int[PASSES]") || line.contains("new int[3]") || line.contains("new float[capacity") || line.contains("new int[capacity"), "buffer allocation outside the constructor: " + line.trim());
        for (String m : new String[]{"add", "reset"}) assertFalse(method(buf, m).contains("new "), m);
        String meshes = code(read("vfx/WaterBcMeshes.java"));
        assertFalse(method(meshes, "get").contains("new "), "the piece lookup of a frame allocates");
        String scene = code(read("vfx/WaterBcScene.java"));
        assertFalse(scene.contains("String.format") || scene.contains("\" +") || scene.contains("ArrayList") || scene.contains("HashMap") || scene.contains("Random"), "the scene builds strings, lists or randoms");
        String toon = code(read("vfx/WaterBcToon.java"));
        assertFalse(method(toon, "emit").contains("new "), "emit allocates");
        assertTrue(scene.contains("float TAU = (float) (Math.PI * 2.0)"));
    }

    @Test
    void thePainterAllocatesNothingPerFrameAndReadsTheEffectsOwnAge() {
        String c = code(read("vfx/client/layer/WaterBcLayer.java"));
        for (String line : c.split("\n")) {
            if (!line.contains("new ")) continue;
            String t = line.trim();
            assertTrue(t.startsWith("private static final") || t.startsWith("private final") || t.contains("TEX = new ResourceLocation[2]") || t.contains("OLD_FX = new WaterFxLayer()"), "an allocation outside a static initialiser: " + t);
        }
        for (String m : new String[]{"render", "sprite", "mesh", "kindOf", "old", "vertexBudget"}) {
            String b = method(c, m);
            assertFalse(b.contains("new ") || b.contains("String.format") || b.contains("\" +") || b.contains("Minecraft.getInstance") || b.contains("ResourceLocation.") || b.contains("byName(")
                    || b.contains("ctx.rel(") || b.contains("inst.random()") || b.contains("toLowerCase"), m + " allocates or builds something per frame");
        }
        assertTrue(c.contains("inst.ageTicks(ctx.partialTick)"), "the effect's own age");
        assertTrue(c.contains("EnumSet.copyOf(WaterBc.SHAPES)"));
        assertTrue(method(c, "vertexBudget").contains("WaterBc.vertexBudget(s)"), "the activation budget comes from the caps");
        assertTrue(method(c, "sprite").contains("WaterBcFrame.spriteBudgetLeft() <= 0") && method(c, "sprite").contains("buf.hasBudget(4)"), "the sprites are counted against the frame ceiling and the activation budget");
        assertTrue(method(c, "render").contains("WaterBcFrame.tierOf(inst)") && method(c, "render").contains("WaterBc.SKIP"), "the tier comes from the frame plan");
        assertTrue(method(c, "mesh").contains("WaterBcMeshBuffer.FRAME"), "the cores go to the capped mesh buffer");
        assertFalse(c.contains("TimeMagic"), "the Time Magic VFX is not touched");
        String frame = code(read("vfx/client/WaterBcFrame.java"));
        for (String line : frame.split("\n")) if (line.contains("new ")) assertTrue(line.contains("private static final"), "WaterBcFrame allocates: " + line.trim());
        assertFalse(method(frame, "plan").contains("new "));
        assertTrue(method(frame, "plan").contains("WaterBc.admit(") && method(frame, "plan").contains("WaterBcMeshBuffer.FRAME.reset("), "the plan admits nearest first and sizes the mesh buffer");
    }

    @Test
    void theMeshDrawIsThreePassesDepthWrittenOnlyForTheSolidOne() {
        String d = code(read("vfx/client/WaterBcDraw.java"));
        String flush = method(d, "flush");
        int solid = flush.indexOf("WaterBcMeshBuffer.SOLID"), glass = flush.indexOf("WaterBcMeshBuffer.TRANSLUCENT"), glow = flush.indexOf("WaterBcMeshBuffer.GLOW");
        assertTrue(solid > 0 && glass > solid && glow > glass, "solid first, then the translucent hulls, then the glow");
        assertEquals(2, flush.split("RenderSystem\\.depthMask\\(false\\)", -1).length - 1, "the glass and the glow never write depth");
        assertEquals(2, flush.split("RenderSystem\\.depthMask\\(true\\)", -1).length - 1, "the solid pass writes it, and the state is restored at the end");
        assertTrue(flush.contains("RenderSystem.disableCull()") && flush.contains("RenderSystem.enableCull()"), "culling is set in full, and restored");
        assertTrue(flush.contains("buf.reset()"), "the buffer is emptied after the draw");
        assertFalse(method(d, "pass").contains("new ") && !method(d, "pass").contains("Tesselator"), "pass");
        assertEquals(1, d.split("Tesselator\\.getInstance\\(\\)\\.begin", -1).length - 1, "one builder per pass, a few draw calls");
        assertTrue(d.contains("modelView.transformPosition("), "view space on the CPU, like the sprite buffer");
        String mgr = code(read("vfx/client/VfxManager.java"));
        int plan = mgr.indexOf("int culled = plan(ctx, quality, n, base);"), hook = mgr.indexOf("WaterBcFrame.plan(active, distance, lod, left, n, quality.frameVertices);"), loop = mgr.indexOf("inst.layer.render(inst, ctx, buffer);"),
                mesh = mgr.indexOf("WaterBcDraw.flush(modelView);"), sprites = mgr.indexOf("buffer.flush(modelView);");
        assertTrue(plan > 0 && hook > plan && loop > hook, "the frame plan runs after the manager's own plan and before the effects are drawn");
        assertTrue(mesh > loop && sprites > mesh, "the 3D cores are drawn just before the sprite buffer (the sprites are depth tested against them)");
        assertEquals(1, mgr.split("WaterBcDraw\\.flush", -1).length - 1);
        assertEquals(1, mgr.split("WaterBcFrame\\.plan", -1).length - 1);
        assertEquals(2, mgr.split("instanceof TimeMagicLayer", -1).length - 1, "the Time Magic exemption of the manager is as it was");
    }

    @Test
    void oneRegistrationAfterTheOldLayersAndV1HandsEverythingBack() {
        String mgr = code(read("vfx/client/VfxManager.java"));
        int fx = mgr.indexOf("register(new com.newuniverse.nusmp.vfx.client.layer.WaterFxLayer())"), magic = mgr.indexOf("register(new WaterMagicLayer())"), spell = mgr.indexOf("register(new WaterSpellLayer())"),
                bc = mgr.indexOf("register(new com.newuniverse.nusmp.vfx.client.layer.WaterBcLayer())");
        assertTrue(fx > 0 && magic > 0 && spell > 0 && bc > fx && bc > magic && bc > spell, "WaterBcLayer is registered after the old water layers (the manager keeps the last one registered for a shape)");
        assertEquals(1, mgr.split("register\\(new com\\.newuniverse\\.nusmp\\.vfx\\.client\\.layer\\.WaterBcLayer\\(\\)\\)", -1).length - 1, "registered once");
        String c = code(read("vfx/client/layer/WaterBcLayer.java"));
        String render = method(c, "render");
        assertTrue(render.indexOf("!WaterBc.bcLook()") >= 0 && render.indexOf("old(inst.shape).render(inst, ctx, buf)") > render.indexOf("!WaterBc.bcLook()"), "V1: the old layer draws");
        assertTrue(method(c, "old").contains("s == VfxShape.WATER_SPLASH || s == VfxShape.WATER_RING ? OLD_MAGIC : OLD_FX"), "each shape goes back to the layer that owned it");
        assertTrue(method(c, "defaultDuration").contains("old(s).defaultDuration(s)") && method(c, "defaultColor").contains("old(s).defaultColor(s)") && method(c, "onSpawn").contains("old(inst.shape).onSpawn(inst)"));
        assertTrue(method(c, "vertexBudget").contains("old(s).vertexBudget(s)"));
    }

    @Test
    void theOldLayersAndTheOldWaterSpellShapesAreNotEditedAndNoValkyrieIsBuilt() {
        String spell = read("vfx/client/layer/WaterSpellLayer.java");
        assertFalse(spell.contains("WaterBc"), "WaterSpellLayer is not edited");
        assertTrue(spell.contains("EnumSet.of(VfxShape.WATER_DRAGON, VfxShape.WATER_CRADLE, VfxShape.WATER_BURST,"), "WaterSpellLayer still owns its six shapes");
        assertTrue(spell.contains("case WATER_DRESS -> dress(inst, ctx, buf);"), "the old Valkyrie Dress is still there");
        String fx = read("vfx/client/layer/WaterFxLayer.java"), magic = read("vfx/client/layer/WaterMagicLayer.java");
        assertFalse(fx.contains("WaterBc") || magic.contains("WaterBc"), "the two old layers are not edited");
        assertTrue(fx.contains("EnumSet.of(VfxShape.WATERFX_CAST, VfxShape.WATERFX_ZONE, VfxShape.WATERFX_BURST)") && magic.contains("EnumSet.of(VfxShape.WATER_SPLASH, VfxShape.WATER_RING)"));
        String rules = code(read("vfx/WaterBc.java"));
        String owned = rules.substring(rules.indexOf("List<VfxShape> SHAPES"), rules.indexOf(";", rules.indexOf("List<VfxShape> SHAPES")));
        assertFalse(owned.contains("WATER_DRESS") || owned.contains("WATER_DRAGON") || owned.contains("WATER_JAVELIN"), "the owned shapes are the five: " + owned);
        assertFalse(code(read("vfx/client/layer/WaterBcLayer.java")).contains("WATER_DRESS"), "the painter knows nothing of the Valkyrie Dress");
        assertFalse(code(read("vfx/WaterBcScene.java")).toLowerCase().contains("wing") || code(read("vfx/WaterBcScene.java")).toLowerCase().contains("valk"), "the wings are deferred: nothing of them in the scene");
    }

    @Test
    void theDocsToolsAndPreviewsAreThere() {
        for (String f : new String[]{"tools/gen_water_bc_textures.py", "tools/gen_water_bc_meshes.py", "tools/vfx_preview/water_bc_ingame.py", "tools/vfx_preview/scenes.d/water_bc.json", "docs/vfx_remake/water_ingame.md",
                "docs/previews/vfx_remake/water_new/water_ingame.png", "docs/previews/vfx_remake/water_new/water_ingame_budget.json", "docs/vfx_remake/water_bc.md", "docs/vfx_remake/water_new.md"})
            assertTrue(Files.exists(ROOT.resolve(f)), f);
    }
}
