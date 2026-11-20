package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (docs/heroweapon/flat_parts.md): the arrows of the Legendary Bow and the tips of the spears and the harpoon are solid now. The pure half (aim, per-frame budget, flight, the switch), the source rules
 * (appended shapes, one packet as before, a client switch that restores the old look, no client class in the pure file, server calls only send a shape) and the mesh files (the old flat ones are kept, the new
 * ones are not the same) are pinned here.
 */
class HeroSolidTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path MESHES = ROOT.resolve("src/main/resources/assets/anime_multiverse/weapon_meshes");

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void aimPointsTheModelAxisAlongTheDirection() {
        double[][] dirs = {{1, 0, 0}, {0, 0, -1}, {-1, 2, 3}, {0.3, -1, 0.2}, {0, 1, 0}, {0, -1, 0}, {5, 0.1, -4}};
        for (double[] d : dirs) {
            double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            float[] a = HeroSolid.aim(d[0], d[1], d[2]);
            double[] back = HeroSolid.axis(a[0], a[1]);
            for (int i = 0; i < 3; i++) assertEquals(d[i] / len, back[i], 1.0E-4, "axis " + i + " of " + d[0] + "," + d[1] + "," + d[2]);
        }
    }

    @Test
    void theFrameBudgetStopsAtItsCapAndStartsAgainOnANewFrame() {
        Object frame = new Object();
        int got = 0;
        for (int i = 0; i < HeroSolid.MAX_PER_FRAME * 3; i++) if (HeroSolid.takeSlot(frame)) got++;
        assertEquals(HeroSolid.MAX_PER_FRAME, got);
        assertTrue(HeroSolid.takeSlot(new Object()), "a new frame starts the count again");
    }

    @Test
    void theWorstCaseOfOneEffectStaysInsideTheActivationCap() {
        assertTrue(HeroSolid.VERTEX_BUDGET <= 400, "inside the 400 vertices of an activation");
        assertTrue(HeroSolid.MAX_QUADS_EACH * 4 + 64 <= HeroSolid.VERTEX_BUDGET, "the model plus a trail, glints and a glow (64 vertices) fit the budget");
        assertTrue(HeroSolid.MAX_PER_FRAME <= 16);
    }

    @Test
    void theFlightLandsAtSeventyPercentAndStays() {
        assertEquals(0f, HeroSolid.flight(0f, 10f), 1.0E-6f);
        assertEquals(0f, HeroSolid.flight(-3f, 10f), 1.0E-6f);
        assertEquals(1f, HeroSolid.flight(7f, 10f), 1.0E-4f);
        assertEquals(1f, HeroSolid.flight(50f, 10f), 1.0E-6f);
        assertEquals(0.5f, HeroSolid.flight(3.5f, 10f), 1.0E-4f);
        assertEquals(1f, HeroSolid.flight(5f, 0f), 1.0E-6f);
    }

    @Test
    void theSwitchDefaultsToSolidAndCanBeTurnedOff() {
        assertTrue(HeroSolid.solid());
        HeroSolid.setSolid(false);
        try { assertFalse(HeroSolid.solid()); } finally { HeroSolid.setSolid(true); }
        assertTrue(HeroSolid.solid());
    }

    @Test
    void thePureFileNamesNoGameOrClientClass() throws IOException {
        String c = read("src/main/java/com/newuniverse/nusmp/vfx/HeroSolid.java");
        assertFalse(Pattern.compile("(?m)^import ").matcher(c).find(), "HeroSolid must stay pure");
        assertFalse(c.contains("Minecraft.getInstance"));
    }

    @Test
    void theShapesAreAppendedTheLayerIsRegisteredAndTheSwitchIsAClientSetting() throws IOException {
        String shape = read("src/main/java/com/newuniverse/nusmp/vfx/VfxShape.java");
        assertTrue(shape.indexOf("HERO_ARROW") > shape.indexOf("GIANT_AVATAR") && shape.indexOf("HERO_SPEAR") > shape.indexOf("HERO_ARROW"), "appended at the end");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java").contains("HeroProjectileLayer"), "registered");
        String cfg = read("src/main/java/com/newuniverse/nusmp/vfx/client/FlatPropsConfig.java");
        assertTrue(cfg.contains("define(\"heroSolidParts\", true)") && cfg.contains("HeroSolid.setSolid("), "a client switch, default on");
        String layer = read("src/main/java/com/newuniverse/nusmp/vfx/client/layer/HeroProjectileLayer.java");
        assertTrue(layer.contains("HeroSolid.solid()") && layer.contains("legacy("), "off restores the old starlight beam");
        assertTrue(layer.contains("HeroSolid.takeSlot(") && layer.contains("HeroSolid.VERTEX_BUDGET"), "the layer is capped per frame and per effect");
    }

    @Test
    void theServerOnlySendsAShapeAndNothingMore() throws IOException {
        List<String> files = List.of("bow/BowShots", "bow/BowDirect", "bow/BowRain", "spear/SpearGuard", "spear/SpearMotion", "spear/SpearStrikes");
        int heroShapes = 0;
        for (String f : files) {
            String c = read("src/main/java/com/newuniverse/nusmp/heroweapon/weapon/" + f + ".java");
            heroShapes += c.split("VfxShape\\.HERO_(ARROW|SPEAR)", -1).length - 1;
            assertFalse(Pattern.compile("net\\.minecraft\\.client|com\\.newuniverse\\.nusmp\\.client|HeroSolid").matcher(c).find(), f + " is server code: no client class, no switch");
        }
        assertTrue(heroShapes >= 12, "the bow and spear effects send the new shapes: " + heroShapes);
    }

    @Test
    void theOldFlatMeshesAreKeptAndTheNewOnesDiffer() throws IOException {
        Path old = MESHES.resolve("flat_parts_old");
        long kept = 0;
        try (var s = Files.list(old)) { kept = s.filter(p -> p.toString().endsWith(".txt")).count(); }
        assertTrue(kept >= 44, "the 44 flat originals are all kept: " + kept);
        for (String m : List.of("legendary_spear", "legendary_spear_curse", "legendary_bow_pull1", "legendary_bow_pull2", "legendary_bow_pull3", "vassal_harpoon")) {
            String a = Files.readString(MESHES.resolve(m + ".txt")), b = Files.readString(old.resolve(m + ".txt"));
            assertNotEquals(a, b, m + " must be rebuilt solid");
        }
    }

    /** 4.55.0 (docs/heroweapon/flat_parts_blender.md): the flat-part meshes were rebuilt in Blender; the skin each old mesh was laid out for is kept next to it. */
    @Test
    void theSolidMeshesAreBlenderBuildsAndTheOldSkinsAreRetired() throws IOException {
        Path old = MESHES.resolve("flat_parts_old");
        Path oldSkins = ROOT.resolve("src/main/resources/assets/anime_multiverse/textures/entity/weapon/flat_parts_old");
        int rebuilt = 0;
        try (var s = Files.list(old)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".txt"))::iterator) {
                String id = p.getFileName().toString().replace(".txt", "");
                String now = Files.readString(MESHES.resolve(id + ".txt"));
                if (!now.contains("# blender-solid")) continue;
                rebuilt++;
                String base = id.replaceAll("_pull[123]$", "");
                assertTrue(!Files.exists(oldSkins.resolve(base + ".png")), id + ": the old skins were dropped at the owner's request (4.62.1), so the renderer keeps the new mesh for it");
                assertTrue(now.lines().filter(l -> l.startsWith("q ") || l.startsWith("qn ")).count() <= 5000, id + " is inside the quad-line budget");
            }
        }
        assertTrue(rebuilt >= 1, "at least one mesh is a Blender build");
        assertTrue(read("src/main/java/com/newuniverse/nusmp/client/WeaponRenderer.java").contains("RETIRED_OLD_SKINS.contains(base)"), "an old mesh without its skin is never drawn");
    }

    @Test
    void theSmokeListAndTheRegistrationHaveTheSelfTest() throws IOException {
        assertTrue(read(".github/workflows/smoke.yml").contains("visual.flat_props,visual.hero_solid,"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/NUSMP.java").contains("HeroSolidSelfTests.register()"));
        assertTrue(read("src/main/java/com/newuniverse/nusmp/vfx/HeroSolidSelfTests.java").contains("\"visual.hero_solid\""));
    }
}
