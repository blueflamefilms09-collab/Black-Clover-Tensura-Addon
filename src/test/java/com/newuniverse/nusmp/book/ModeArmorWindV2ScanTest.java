package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the overlay v2 cannot be profiled here, so its rules are pinned by reading the real source. (1) Server safety (rule 9): the common classes (rules, meshes, self-test, the mode enum) name no client class;
 * the meshes and rules stay pure; nothing is stored per player on the server beyond one boolean and nothing new is sent (the Yuno variant rides in the existing mode packet). (2) Frame cost (rule 11): the painter
 * allocates nothing of its own per frame (every {@code new} is a static initialiser), builds no string, asks the camera nowhere but in the one aura hook, takes its render types from SafeAdditive and the
 * vanilla entity types only, and is admitted by the per-frame budget. (3) Wiring: the layer, the aura hook, the client option (its own config file), the self-test and the tools are all there, and the old overlay
 * and the old Wind A pieces are still reachable.
 */
class ModeArmorWindV2ScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path JAVA = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) {
        try { return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8); } catch (IOException e) { throw new AssertionError("cannot read " + rel, e); }
    }

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", ""); }

    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(\\)");

    /** The body of a method: from its signature to the matching closing brace. */
    private static String method(String src, String name) {
        int i = src.indexOf(" " + name + "(");
        assertTrue(i >= 0, "method " + name + " not found");
        int open = src.indexOf('{', i), depth = 0, j = open;
        for (; j < src.length(); j++) {
            char c = src.charAt(j);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) break;
        }
        return src.substring(open, j + 1);
    }

    @Test
    void theCommonClassesAreServerSafeAndPure() {
        for (String f : new String[]{"book/ModeArmorWindV2Rules.java", "book/ModeArmorWindV2Mesh.java", "book/ModeArmorWindV2SelfTests.java"})
            assertFalse(CLIENT.matcher(code(read(f))).find(), f + " names a client class");
        for (String f : new String[]{"book/ModeArmorWindV2Rules.java", "book/ModeArmorWindV2Mesh.java"})
            assertFalse(code(read(f)).contains("net.minecraft"), f + " stays pure");
        String mode = code(read("blackclover/ModeArmor.java"));
        for (String m : new String[]{"start", "wire", "yunoTier", "onStartTracking", "onLogin"}) assertFalse(CLIENT.matcher(method(mode, m)).find(), "ModeArmor." + m + " names a client class");
        assertTrue(mode.contains("putBoolean(YUNO, yuno)") && mode.contains("wire(mode, yuno).ordinal()"), "the Yuno variant rides in the existing mode packet");
        assertFalse(mode.contains("UUID") || mode.contains("static ServerPlayer"), "no per-player static state");
        assertTrue(mode.contains("catch (RuntimeException | LinkageError"), "a failing grimoire read never breaks a mode start");
    }

    @Test
    void thePainterAllocatesNothingPerFrameAndReadsNoCameraButInTheAuraHook() {
        String c = code(read("client/mode/ModeArmorWindV2.java"));
        for (String line : c.split("\n")) {
            if (!line.contains("new ")) continue;
            assertTrue(line.contains("static final"), "an allocation outside a static initialiser: " + line.trim());
        }
        for (String m : new String[]{"draw", "capture", "drawEntry", "emit", "leaves", "vert", "setSway", "buffer"}) {
            String b = method(c, m);
            assertFalse(b.contains("new ") || b.contains("String.format") || b.contains("\" +") || b.contains("Minecraft.getInstance") || b.contains("ResourceLocation"), m + " allocates or reads the game per frame");
        }
        assertTrue(method(c, "replacesAuraPieces").contains("Minecraft.getInstance().level"), "the one place that reads the level: the aura hook");
        assertTrue(c.contains("SpiritDiveFrame.admitModeV2()"), "admitted by the per-frame budget");
        assertTrue(c.contains("ModeArmorWindV2Rules.lod(distSq)") && c.contains("Lod.CULLED"), "level of detail by distance, nothing beyond 64 blocks");
        assertFalse(c.contains("RenderType.eyes") || c.contains("entityTranslucentEmissive") || c.contains("RenderType.lightning"), "additive and emissive types come from SafeAdditive only");
        assertTrue(c.contains("SafeAdditive.additive(GLOW)") && c.contains("SafeAdditive.emissive(FX)"), "render types through SafeAdditive");
    }

    @Test
    void theLayerTheAuraHookTheConfigAndTheSelfTestAreWired() {
        String layer = code(read("client/mode/ModeArmorLayer.java"));
        assertTrue(layer.contains("ModeArmorWindV2.active(mode)") && layer.contains("ModeArmorWindV2.draw("), "the layer draws the v2 overlay");
        assertTrue(layer.contains("case WIND_SPIRIT_DIVE, WIND_SPIRIT_DIVE_YUNO -> wind(c, m);"), "the classic overlay is still drawn for every other Wind user and for CLASSIC");
        assertTrue(layer.contains("private static void wind(Ctx c, PlayerModel<AbstractClientPlayer> m)"), "the old overlay is kept");
        String aura = code(read("client/aura/SpiritDiveWind.java"));
        assertTrue(aura.contains("ModeArmorWindV2.replacesAuraPieces(c.player().getId())") && aura.contains("SpiritDiveWindRules.character(grade)") && aura.contains("if (!overlayWings) leaves("), "only the Yuno tier drops its leaf wings and crown");
        assertTrue(aura.contains("SpiritDiveWind3.weaponsA(") && aura.contains("wrists(c, sp, pal, u, uk, mask, keptMask, lit, e)"), "the sword, the bow and the wrist circle stay");
        String cfg = code(read("client/mode/ModeArmorWindV2Config.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT, SPEC, \"nusmp-modearmor-client.toml\"") && cfg.contains(".defineEnum(\"windLook\", Look.V2)"), "a client option with its own file, default V2");
        assertTrue(code(read("NUSMP.java")).contains("ModeArmorWindV2Config.init(modEventBus, modContainer)"));
        assertTrue(code(read("book/SpiritDiveBoot.java")).contains("ModeArmorWindV2SelfTests.register()"));
        assertTrue(read("book/ModeArmorWindV2SelfTests.java").contains("\"magic.mode_wind_v2\""));
        assertTrue(Files.exists(ROOT.resolve("tools/gen_mwv2_textures.py")) && Files.exists(ROOT.resolve("tools/mwv2_layout.json")) && Files.exists(ROOT.resolve("docs/magic/mode_armor_wind_v2.md")), "generator, layout and doc");
    }
}
