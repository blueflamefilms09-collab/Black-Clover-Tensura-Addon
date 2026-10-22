package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner report, builds 1.110 / 1.111, docs/visual/flat_props.md): nothing visual can be run here, so the rule "no flat pale single-quad silhouette is drawn while visual.flatProps is off" is pinned from
 * the sources. (1) The vertex buffer has the one gate. (2) A source that names a flat sprite must ask {@code FlatProps.drawFlat()} (the old drawing is only the switch-on branch). (3) No prop painter or aura
 * names a flat sprite, and the single flat quads the prop painters draw are exactly the reviewed ring / shadow decals. (4) The layers that lost a sprite draw the solid models, and the solid parts keep their
 * cost bounds (budget check, distance cull, nothing allocated while drawing a face).
 */
class FlatPropsSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    /** The only files that may name a flat sprite without branching on the switch: the list itself and the texture constants. */
    private static final Set<String> NAMES_ONLY = Set.of("vfx/FlatProps.java", "vfx/FlatPropsSelfTests.java", "vfx/client/VfxTextures.java",
            "vfx/client/layer/SoulCorpseFxLayer.java", "vfx/client/layer/SoulFxLayer.java");   // 1.112.0: these two only draw through VfxVertexBuffer, whose one gate refuses the flat sprites

    /** The single flat quads the prop painters draw: ground shadows and ring decals (soft textured, dark or ring-shaped, never an object cut-out). Review before adding to this list. */
    private static final Set<String> PROP_QUAD_FILES = Set.of("TreeDraw", "BeastDraw", "RecombinationPropPainter", "ShakudoPropDraw", "SealDraw", "EarthPropPainter", "CorundumDraw", "KeyDraw",
            "SnowPropPainter", "VortexPropPainter", "AshPropPainter", "BronzePropPainter", "SmokePropPainter", "AirPropPainter", "LightJaPropPainter", "RecombAnimePainter");   // 1.112.0 reviewed: glowing ground rings / pools and soft shadow decals, no object silhouettes

    private static String code(Path p) throws IOException {
        return Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    private static List<Path> javaFiles(String sub) throws IOException {
        try (Stream<Path> s = Files.walk(SRC.resolve(sub))) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String rel(Path p) { return SRC.relativize(p).toString().replace('\\', '/'); }

    private static boolean namesFlatSprite(String c) {
        for (FlatProps.Entry e : FlatProps.FLAT) {
            if (c.contains("\"" + e.texture() + "\"") || c.contains("particle/" + e.texture())) return true;
        }
        return c.contains("VfxTextures.CONSTRUCT_");
    }

    @Test
    void theVertexBufferHasTheOneGate() throws IOException {
        String c = code(SRC.resolve("vfx/client/VfxVertexBuffer.java"));
        assertTrue(c.contains("FlatProps.isFlat(") && c.contains("b.flat && !FlatProps.drawFlat()"), "the vertex buffer lost the flat-sprite gate");
        int gate = c.indexOf("b.flat && !FlatProps.drawFlat()"), count = c.indexOf("activationVertices += 4");
        assertTrue(gate > 0 && gate < count, "the gate must refuse the quad before it is counted or added");
    }

    @Test
    void everySourceThatNamesAFlatSpriteBranchesOnTheSwitch() throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path p : javaFiles("")) {
            String rel = rel(p);
            if (NAMES_ONLY.contains(rel)) continue;
            String c = code(p);
            if (namesFlatSprite(c) && !c.contains("FlatProps.drawFlat()")) bad.add(rel);
        }
        assertTrue(bad.isEmpty(), "these files draw a flat object cut-out without asking visual.flatProps: " + bad);
    }

    @Test
    void noPropPainterOrAuraNamesAFlatSprite() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String dir : new String[]{"client/prop", "client/aura", "prop"}) {
            for (Path p : javaFiles(dir)) { String c = code(p); if (namesFlatSprite(c) && !c.contains("FlatProps.drawFlat()")) bad.add(rel(p)); }
        }
        assertTrue(bad.isEmpty(), "a prop painter or aura draws a flat cut-out: " + bad);
    }

    @Test
    void theSingleFlatQuadsOfThePropPaintersAreTheReviewedRingAndShadowDecals() throws IOException {
        List<String> found = new ArrayList<>();
        for (Path p : javaFiles("client/prop")) {
            String name = p.getFileName().toString().replace(".java", "");
            if (name.equals("PropDraw")) continue;
            if (code(p).contains("PropDraw.quad(") && !PROP_QUAD_FILES.contains(name)) found.add(name);
        }
        assertTrue(found.isEmpty(), "a prop painter draws a new single flat quad: review it (a flat pale silhouette is not allowed), then list it here: " + found);
    }

    @Test
    void theLayersThatLostASpriteDrawSolidModelsAndKeepTheOldBranchBehindTheSwitch() throws IOException {
        String aura = code(SRC.resolve("vfx/client/layer/AuraLayer.java"));
        assertTrue(aura.contains("if (!FlatProps.drawFlat())") && aura.contains("solidConstructs("));
        assertTrue(aura.contains("SolidModels.sword(") && aura.contains("SolidModels.axe(") && aura.contains("SolidModels.shield("), "the constructs lost a solid model");
        assertTrue(aura.indexOf("if (!FlatProps.drawFlat())") < aura.indexOf("VfxTextures.CONSTRUCT_SWORD"), "the old sprites must come after the solid branch returned");
        for (String f : new String[]{"vfx/client/layer/MirrorLayer.java", "vfx/client/layer/ArcaneSpellLayer2.java"}) {
            String c = code(SRC.resolve(f));
            assertTrue(c.contains("if (FlatProps.drawFlat())") || c.contains("if (flat)"), f + " does not branch on the switch");
            assertTrue(c.contains("SolidModels.figure("), f + " lost the solid figure");
        }
    }

    @Test
    void theSolidPartsKeepTheirCostBounds() throws IOException {
        String c = code(SRC.resolve("vfx/client/SolidParts.java"));
        assertTrue(c.contains("MAX_DISTANCE") && c.contains("centre.lengthSquared() > MAX_DISTANCE * MAX_DISTANCE"), "the distance cull is gone");
        assertTrue(c.contains("MAX_FACES = 100"), "a model must never exceed the 100 quads of an effect");
        assertTrue(c.contains("buf.hasBudget(count * 4)"), "a model must be skipped whole when the budget cannot hold it");
        int draw = c.indexOf("public boolean draw("), end = c.indexOf("private void swap(", draw);
        assertTrue(draw > 0 && end > draw);
        assertFalse(c.substring(draw, end).contains("new "), "draw() allocates");
        int face = c.indexOf("private void face("), faceEnd = c.indexOf("public Model box(", face);
        assertFalse(c.substring(face, faceEnd).contains("new "), "face() allocates");
        assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d"), "SolidParts must stay free of client classes so the preview can run it");
    }

    @Test
    void theSolidModelsAddNoBlendOrRenderTypeOfTheirOwn() throws IOException {
        for (String f : new String[]{"vfx/client/SolidParts.java", "vfx/client/SolidModels.java", "vfx/client/FlatPropsConfig.java"}) {
            String c = code(SRC.resolve(f));
            for (String banned : new String[]{"RenderType.eyes(", "RenderType.lightning(", "ADDITIVE_TRANSPARENCY", "ParticleTypes.", "Minecraft.getInstance"})
                assertFalse(c.contains(banned), f + " uses " + banned);
        }
        assertTrue(code(SRC.resolve("vfx/client/SolidParts.java")).contains("VfxBlend.ALPHA"), "solid parts are alpha-blended through the shared buffer only");
    }

    @Test
    void theTimeMagicFilesAreUntouched() throws IOException {
        String time = Files.readString(SRC.resolve("vfx/client/layer/TimeMagicLayer.java"));
        assertFalse(time.contains("FlatProps") || time.contains("SolidParts"));
        assertTrue(Files.readString(Path.of("tools/vfx_preview/scenes.d/flat_props.json")).contains("constructs_flat"), "the before / after scenes are missing");
    }
}
