package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: 3.29.0 gives each of the ten Evolution Ultimates its own signature effect ({@code ULT_*} shapes, one layer each). This guards the rules that are easy to break without a game: every unique shape has its layer
 * registered and its preview scene, every layer keeps to the 400-vertex budget idiom (no per-frame collections, no client-only calls outside the layer API), the shipped line files name exactly one unique shape per
 * Ultimate, the old generic shapes are still in the enum (the "keep the old" rule) and the CLIENT setting {@code visual.uniqueUltimates} restores them. Pure: reads the repo's files, the working directory is the project root.
 */
class UniqueUltimatesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LAYERS = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path TREES = ROOT.resolve("src/main/resources/data/nusmp/skill_trees");

    private static final Set<VfxShape> UNIQUE = EnumSet.of(VfxShape.ULT_IRON_DRAGON, VfxShape.ULT_SATAN_CORE, VfxShape.ULT_ALL_CRUSH, VfxShape.ULT_GREAT_FLOOD, VfxShape.ULT_MONARCH_SHADOWS,
            VfxShape.ULT_CHAIRMAN_STAND, VfxShape.ULT_DEVOURING_SHADOW, VfxShape.ULT_FINAL_DOSE, VfxShape.ULT_DISASTER_FLOODS, VfxShape.ULT_THOUSAND_ELF);

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String className(VfxShape s) {
        StringBuilder b = new StringBuilder("Ult");
        for (String part : s.name().substring(4).split("_")) b.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        return b.append("Layer").toString();
    }

    @Test
    void tenUniqueShapesAreTheOnlyUltShapes() {
        Set<VfxShape> found = EnumSet.noneOf(VfxShape.class);
        for (VfxShape s : VfxShape.values()) if (s.name().startsWith("ULT_")) found.add(s);
        assertEquals(UNIQUE, found);
    }

    @Test
    void everyUniqueShapeHasARegisteredLayerAndAScene() throws IOException {
        String manager = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java"));
        for (VfxShape s : UNIQUE) {
            String cls = className(s);
            Path file = LAYERS.resolve(cls + ".java");
            assertTrue(Files.exists(file), cls + " exists");
            String src = read(file);
            assertTrue(src.contains("VfxShape." + s.name()), cls + " draws " + s.name());
            assertTrue(src.contains("render("), cls + " renders");
            assertTrue(src.length() > 4000, cls + " is a real effect, not the empty placeholder of 3.28.1");
            Matcher m = Pattern.compile("register\\(new " + cls + "\\(\\)\\)").matcher(manager);
            assertTrue(m.find(), cls + " is registered");
            assertFalse(m.find(), cls + " is registered once");
            String scene = s.name().substring(4).toLowerCase();
            assertTrue(Files.exists(ROOT.resolve("tools/vfx_preview/scenes.d").resolve("ult_" + scene + ".json")), "a preview scene for " + s.name());
        }
    }

    @Test
    void layersStayInsideTheRuleElevenIdioms() throws IOException {
        for (VfxShape s : UNIQUE) {
            String src = read(LAYERS.resolve(className(s) + ".java")).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
            assertFalse(src.contains("Minecraft.getInstance"), className(s) + " must not reach for the game singleton");
            assertFalse(src.contains("new ArrayList") || src.contains("new HashMap"), className(s) + " must not build collections per frame");
            assertFalse(src.contains("Math.random") || src.contains("new Random"), className(s) + " must be deterministic from the seed so every client agrees");
        }
    }

    @Test
    void theOldGenericShapesAreStillThereAndTheSwitchRestoresThem() {
        assertTrue(UltimateVfxFallback.uniqueEnabled(), "on by default");
        for (VfxShape s : UNIQUE) {
            VfxShape old = UltimateVfxFallback.oldShapeOf(s);
            assertNotNull(old, s + " has an old shape to fall back to");
            assertFalse(old.name().startsWith("ULT_"));
            assertEquals(s, UltimateVfxFallback.resolve(s), "on: itself");
        }
        UltimateVfxFallback.setUniqueEnabled(false);
        try {
            for (VfxShape s : UNIQUE) assertEquals(UltimateVfxFallback.oldShapeOf(s), UltimateVfxFallback.resolve(s), "off: the old shape");
            assertEquals(VfxShape.WATER_BURST, UltimateVfxFallback.resolve(VfxShape.WATER_BURST), "other shapes are never touched");
        } finally {
            UltimateVfxFallback.setUniqueEnabled(true);
        }
    }

    @Test
    void theSettingIsAClientConfigRegisteredOnce() throws IOException {
        String cfg = read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/UniqueUltimatesConfig.java"));
        assertTrue(cfg.contains("ModConfig.Type.CLIENT"), "it only changes how things look: a CLIENT config");
        assertTrue(cfg.contains("define(\"uniqueUltimates\", true)"), "default on");
        assertTrue(cfg.contains("\"nusmp-ultimates-client.toml\""), "its own file name");
        assertTrue(cfg.contains("UltimateVfxFallback.setUniqueEnabled("), "mirrored into the pure switch");
        Matcher m = Pattern.compile("UniqueUltimatesConfig\\.init\\(modEventBus, modContainer\\)").matcher(read(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java")));
        assertTrue(m.find(), "NUSMP registers the config");
        assertFalse(m.find(), "and only once");
    }

    @Test
    void eachShippedUltimateNamesExactlyOneUniqueShape() throws IOException {
        int[] count = new int[VfxShape.values().length];
        try (var st = Files.list(TREES)) {
            for (Path p : (Iterable<Path>) st::iterator) {
                Matcher m = Pattern.compile("\"vfx\": \"(ULT_[A-Z_]+)\"").matcher(read(p));
                while (m.find()) count[VfxShape.valueOf(m.group(1)).ordinal()]++;
            }
        }
        for (VfxShape s : UNIQUE) assertEquals(1, count[s.ordinal()], s + " is used by exactly one Ultimate");
    }
}
