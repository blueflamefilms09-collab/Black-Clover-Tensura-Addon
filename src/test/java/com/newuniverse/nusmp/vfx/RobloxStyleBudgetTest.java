package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (rules 9, 11, 2; docs/vfx_roblox_pass.md): the Roblox-style toolkit cannot be profiled here, so its promises are pinned by the numbers and by reading the real source.
 * (1) Cost: every effect's worst case (measured with tools/vfx_preview on a half-tick sweep) is under the engine's own 100-quad cap and no dearer than the old effect it replaces, except EARTH_SPIKES (real 3D
 * stone spikes, documented). (2) Server safety: the pure class names no client class, the setting is a CLIENT config with its own file, nothing is sent. (3) Frame cost: the toolkit allocates nothing per frame
 * (static scratch only), builds no string, takes every count through {@code ctx.seg}. (4) Wiring: every one of the seven layers calls the toolkit first and keeps its old drawing (rule 2); the setting off gives
 * the old layers. (5) The Time Magic VFX is not touched.
 */
class RobloxStyleBudgetTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.|Minecraft\\.getInstance\\(\\)");

    @Test
    void everyEffectIsUnderTheEngineCapAndNoDearerThanTheOldOneExceptTheStoneSpikes() {
        assertEquals(RobloxMath.SHAPES.length, RobloxMath.MAX_QUADS.length);
        assertEquals(RobloxMath.SHAPES.length, RobloxMath.OLD_QUADS.length);
        List<String> bad = new ArrayList<>();
        for (int i = 0; i < RobloxMath.SHAPES.length; i++) {
            VfxShape s = RobloxMath.SHAPES[i];
            int now = RobloxMath.MAX_QUADS[i], old = RobloxMath.OLD_QUADS[i];
            if (now >= RobloxMath.ENGINE_MAX_QUADS) bad.add(s + " is " + now + " quads, the engine caps one effect at " + RobloxMath.ENGINE_MAX_QUADS);
            if (s != VfxShape.EARTH_SPIKES && now > old) bad.add(s + " costs " + now + " quads, the old drawing " + old + ": the toolkit must be cheaper");
        }
        assertTrue(bad.isEmpty(), bad.toString());
        int i = java.util.Arrays.asList(RobloxMath.SHAPES).indexOf(VfxShape.EARTH_SPIKES);
        assertTrue(RobloxMath.MAX_QUADS[i] <= 30, "the stone spikes are the one dearer effect and stay small: " + RobloxMath.MAX_QUADS[i]);
        int sumNew = 0, sumOld = 0;
        for (int k = 0; k < RobloxMath.MAX_QUADS.length; k++) { sumNew += RobloxMath.MAX_QUADS[k]; sumOld += RobloxMath.OLD_QUADS[k]; }
        assertTrue(sumNew < sumOld, "all together the ten are cheaper: " + sumNew + " vs " + sumOld);
    }

    @Test
    void thePureClassIsServerSafeAndTheSettingIsAClientConfig() {
        String math = code(read("vfx/RobloxMath.java"));
        assertFalse(CLIENT.matcher(math).find(), "RobloxMath names a game or client class");
        assertFalse(math.contains("PacketDistributor") || math.contains("CustomPacketPayload") || math.contains("ServerPlayer"), "it sends nothing and keeps no per-player state");
        String cfg = code(read("vfx/client/RobloxStyleConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT, SPEC, \"nusmp-roblox-vfx-client.toml\")"), "a client option with its own file");
        assertFalse(cfg.contains("Type.SERVER") || cfg.contains("Type.COMMON"));
        assertTrue(cfg.contains("b.push(\"vfx\")") && cfg.contains(".define(\"robloxStyle\", true)"), "vfx.robloxStyle, default on");
        assertTrue(cfg.contains("RobloxMath.setEnabled(ROBLOX_STYLE.get())"), "mirrored into the pure switch");
        assertEquals("nusmp-roblox-vfx-client.toml", RobloxMath.CONFIG_FILE);
        assertTrue(code(read("NUSMP.java")).contains("RobloxStyleConfig.init(modEventBus, modContainer)"), "registered once in the client block");
    }

    @Test
    void theToolkitAllocatesNothingPerFrameAndBuildsNoStrings() {
        for (String f : new String[]{"vfx/client/RobloxStyle.java", "vfx/client/RobloxFx.java"}) {
            for (String line : code(read(f)).split("\n")) {
                String t = line.strip();
                if (t.contains("new ")) assertTrue(t.contains("static final"), f + " allocates outside a static field: " + t);
                assertFalse(t.contains("String.format") || t.contains(".stream(") || t.contains("fromNamespaceAndPath") || t.contains("new ResourceLocation"), f + " builds an object per frame: " + t);
                assertFalse(t.contains("Minecraft.getInstance()") || t.contains("PacketDistributor"), f + " reaches outside the render context: " + t);
            }
        }
        String fx = code(read("vfx/client/RobloxFx.java")), style = code(read("vfx/client/RobloxStyle.java"));
        assertTrue(fx.contains("ctx.seg("), "the counts go through ctx.seg (distance LOD and the particle setting)");
        assertTrue(style.contains("VfxVertexBuffer#quad") || style.contains("buf.quad("), "every quad goes through the buffer, which refuses one past the effect's vertex budget");
        assertTrue(fx.contains("RobloxMath.enabled()"), "the setting is read in one place");
    }

    @Test
    void everyLayerCallsTheToolkitFirstAndKeepsItsOldDrawing() {
        String[][] hooks = {{"vfx/client/layer/KiLayer.java", "RobloxFx.draw(inst, ctx, buf)"}, {"vfx/client/layer/ElementShapesLayer.java", "RobloxFx.draw(inst, ctx, buf)"},
                {"vfx/client/layer/AntiMagicLayer.java", "RobloxFx.draw(inst, ctx, buf)"}, {"vfx/client/layer/WindSpellLayer.java", "RobloxFx.draw(inst, ctx, buf)"},
                {"vfx/client/layer/IceLayer.java", "RobloxFx.draw(inst, ctx, buf)"}, {"vfx/client/layer/WaterSpellLayer.java", "RobloxFx.draw(inst, ctx, buf)"},
                {"vfx/client/layer/MagicCircleLayer.java", "RobloxFx.circleBurst(inst, ctx, buf"}};
        for (String[] h : hooks) {
            String c = code(read(h[0]));
            assertTrue(c.contains("if (" + h[1]), h[0] + " does not call the toolkit");
            assertTrue(c.contains("return;"), h[0] + " lost its own drawing path");
            assertTrue(c.contains("VfxVertexBuffer.") || c.contains("buf."), h[0] + " lost its old drawing (rule 2)");
            assertTrue(c.indexOf("return;") < c.lastIndexOf("buf."), h[0] + ": the old drawing must come after the hook");
        }
        // the old draw code is still in the layers: spot checks of old-only pieces
        assertTrue(code(read("vfx/client/layer/KiLayer.java")).contains("KI_BEAM"), "KiLayer keeps the old beam");
        assertTrue(code(read("vfx/client/layer/WindSpellLayer.java")).contains("WIND_TORNADO"), "WindSpellLayer keeps the old tornado");
        String draw = code(read("vfx/client/RobloxFx.java"));
        assertTrue(draw.contains("if (!RobloxMath.enabled()) return false;"), "the setting off falls through to the old layers");
        for (VfxShape s : RobloxMath.SHAPES) {
            if (s == VfxShape.MAGIC_CIRCLE_EXPLOSION) continue;                                       // routed through circleBurst
            assertTrue(draw.contains("case " + s.name() + " ->"), "RobloxFx.draw does not draw " + s);
        }
    }

    @Test
    void theTimeMagicVfxIsNotTouched() {
        for (String f : new String[]{"vfx/client/RobloxStyle.java", "vfx/client/RobloxFx.java", "vfx/RobloxMath.java"}) {
            String c = code(read(f));
            assertFalse(c.contains("TimeMagicLayer") || c.contains("time_"), f + " reaches the Time Magic VFX");
        }
        for (String f : new String[]{"vfx/client/layer/KiLayer.java", "vfx/client/layer/ElementShapesLayer.java", "vfx/client/layer/AntiMagicLayer.java", "vfx/client/layer/WindSpellLayer.java",
                "vfx/client/layer/IceLayer.java", "vfx/client/layer/WaterSpellLayer.java", "vfx/client/layer/MagicCircleLayer.java"}) {
            assertFalse(code(read(f)).contains("TimeMagicLayer"), f);
        }
    }
}
