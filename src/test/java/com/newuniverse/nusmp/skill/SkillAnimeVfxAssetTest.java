package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.layer.SkillAnimeVfx;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: VFX wave 3 (SkillAnimeLayer + SkillAnimeSigils, docs/vfx_remake/skill_anime.md) reads its 20 textures by name, the server sends its 9 shapes by ordinal and packs a mode into the power, so all three
 * are pinned: every texture exists with a power-of-two size, visible and clear texels, clear corners on the round sprites, no black colour in a clear texel next to a visible one, no stray skillanime_ file,
 * every name drawn by the layer and made by the generator; the shapes are appended after DEVIL_HIT in order and SKILL_SIGIL is the last; the layer is registered, keeps the engine's 400-vertex cap and
 * draws nothing with skills.classicVfx on; the server side (SkillAnimeFx) and the client side (SkillAnimeVfx) agree on every mode, step and sigil style; every shape is sent only through SkillAnimeFx,
 * one call next to each skill's old effect, and the old SkillFx calls are still there; the self-test is on the smoke list.
 */
class SkillAnimeVfxAssetTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path PARTICLE = ROOT.resolve("src/main/resources/assets/anime_multiverse/textures/particle");
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path SKILL_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/skill");
    private static final VfxShape[] SHAPES = {VfxShape.SKILL_GATE, VfxShape.SKILL_FIREBOLT, VfxShape.SKILL_ARGONAUT, VfxShape.SKILL_FROST, VfxShape.SKILL_HOLY_FIRE, VfxShape.SKILL_ALLURE,
            VfxShape.SKILL_SCREW, VfxShape.SKILL_WINGS, VfxShape.SKILL_SIGIL};
    /** The skills wired in wave 3 (each sends its anime effect through SkillAnimeFx next to its old SkillFx calls). */
    private static final String[] WIRED = {"GateOfBabylonSkill", "FireboltSkill", "AdollaBurstSkill", "DevilsFootprintsSkill", "ArgonautSkill", "HerosArgonautSkill", "WynnFimbulvetrSkill",
            "HestiaSkill", "ReaLaevateinnSkill", "FreyaSkill", "AllFictionSkill", "FaithSkill", "GoddessOfFaithSkill", "HephaestusSkill", "UchideNoKozuchiSkill",
            "ImmortalWillSkill", "UnbreakableWillSkill", "HellWalkerSkill", "HellConquerorSkill", "FutsunomitamaSkill"};

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    private static String layers() throws IOException { return code(LAYER_DIR.resolve("SkillAnimeLayer.java")) + "\n" + code(LAYER_DIR.resolve("SkillAnimeSigils.java")); }

    @Test
    void everyTextureIsThereWithAPowerOfTwoSizeAndSomethingToDraw() throws IOException {
        List<String> bad = new ArrayList<>();
        Set<String> round = Set.of("core", "glow", "flash", "ripple", "snowflake", "mist", "petal", "halo", "rays", "sigil", "sparkle");
        for (String name : SkillAnimeVfx.TEXTURE_NAMES) {
            Path f = PARTICLE.resolve(SkillAnimeVfx.fileName(name) + ".png");
            assertTrue(Files.isRegularFile(f), "missing " + name);
            BufferedImage img = ImageIO.read(f.toFile());
            assertNotNull(img, name);
            int w = img.getWidth(), h = img.getHeight();
            if (Integer.bitCount(w) != 1 || Integer.bitCount(h) != 1 || w > 256 || h > 256) bad.add(name + " is " + w + "x" + h);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            int visible = 0, clear = 0;
            for (int p : px) if ((p >>> 24) > 20) visible++; else clear++;
            if (visible == 0 || clear == 0) bad.add(name + " has no visible or no clear texel");
            if (round.contains(name)) for (int c : new int[]{0, w - 1, (h - 1) * w, h * w - 1}) if ((px[c] >>> 24) != 0) bad.add(name + " corner is not clear");
            int dark = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) != 0 || (px[y * w + x] & 0xFFFFFF) != 0) continue;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int xx = x + dx, yy = y + dy;
                    if (xx >= 0 && yy >= 0 && xx < w && yy < h && (px[yy * w + xx] >>> 24) > 20) dark++;
                }
            }
            if (dark > 0) bad.add(name + " has black clear texels next to visible ones");
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void noStrayFileAndTheGeneratorMakesEveryName() throws IOException {
        Set<String> expected = new HashSet<>();
        for (String p : SkillAnimeVfx.allTextures()) expected.add(p.substring(p.lastIndexOf('/') + 1));
        Set<String> found = new HashSet<>();
        File[] files = PARTICLE.toFile().listFiles((d, n) -> n.startsWith(SkillAnimeVfx.PREFIX));
        assertNotNull(files);
        for (File f : files) found.add(f.getName());
        assertEquals(expected, found, "the skillanime_ files and the list in SkillAnimeVfx must be the same set");
        String gen = Files.readString(ROOT.resolve("tools/gen_skill_anime_vfx_textures.py"), StandardCharsets.UTF_8);
        assertTrue(gen.contains("PREFIX = \"skillanime_\""));
        for (String n : SkillAnimeVfx.TEXTURE_NAMES) assertTrue(gen.contains("\"" + n + "\""), "the generator does not make " + n);
    }

    @Test
    void everyTextureIsDrawnByTheLayer() throws IOException {
        String layer = code(LAYER_DIR.resolve("SkillAnimeLayer.java")), all = layers();
        for (String n : SkillAnimeVfx.TEXTURE_NAMES) {
            String constant = n.toUpperCase(Locale.ROOT);
            assertTrue(layer.contains(constant + " = t(\"" + n + "\")"), n + " has no constant");
            assertTrue(all.split("\\b" + constant + "\\b", -1).length - 1 >= 2, n + " is generated but never drawn (a dead asset)");
        }
    }

    @Test
    void theShapesAreAppendedAfterTheDevilHitAndTheLayerIsRegistered() throws IOException {
        for (int i = 0; i < SHAPES.length; i++) assertEquals(VfxShape.DEVIL_HIT.ordinal() + 1 + i, SHAPES[i].ordinal(), "appended in order: " + SHAPES[i]);
        VfxShape[] all = VfxShape.values();
        assertEquals(VfxShape.DEVIL_HIT.ordinal() + SHAPES.length, VfxShape.SKILL_SIGIL.ordinal(), "wave 3: SKILL_SIGIL closes the nine skill shapes (later families, like wave 4's ANIME2_*, are appended after it)");
        assertTrue(all[all.length - 1].ordinal() >= VfxShape.SKILL_SIGIL.ordinal(), "nothing may be inserted before SKILL_SIGIL");
        String layer = code(LAYER_DIR.resolve("SkillAnimeLayer.java"));
        for (VfxShape s : SHAPES) assertTrue(layer.contains("VfxShape." + s.name()) && layer.contains("case " + s.name()), s + " is not drawn by SkillAnimeLayer");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/VfxManager.java")).contains("register(new com.newuniverse.nusmp.vfx.client.layer.SkillAnimeLayer())"));
        assertFalse(layer.contains("hardVertexCap") || layer.contains("vertexBudget"), "the layer keeps the engine's 400-vertex cap per effect");
        assertTrue(layer.contains("public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {\n        if (SkillAnimeVfx.classic()) return;"), "skills.classicVfx must leave the layer drawing nothing");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/SkillAnimeVfxConfig.java")).contains("SkillAnimeVfx.setClassic(CLASSIC.get())"), "the client setting must reach the switch");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("SkillAnimeVfxConfig.init(modEventBus, modContainer)"), "the client config is registered");
        String both = layers();
        assertFalse(both.contains("ParticleTypes") || both.contains("net.minecraft.core.particles"), "no vanilla particles in the layer");
    }

    @Test
    void theServerAndTheClientAgreeOnThePowerEncoding() {
        assertEquals(SkillAnimeVfx.STEP, SkillAnimeFx.STEP);
        assertEquals(SkillAnimeVfx.GATE_RAIN, SkillAnimeFx.GATE_RAIN);
        assertEquals(SkillAnimeVfx.GATE_CHAINS, SkillAnimeFx.GATE_CHAINS);
        assertEquals(SkillAnimeVfx.GATE_EA, SkillAnimeFx.GATE_EA);
        assertEquals(SkillAnimeVfx.ARGO_STRIKE, SkillAnimeFx.ARGO_STRIKE);
        assertEquals(SkillAnimeVfx.ARGO_BEAM, SkillAnimeFx.ARGO_BEAM);
        assertEquals(SkillAnimeVfx.FIRE_FIELD, SkillAnimeFx.FIRE_FIELD);
        String[] styles = {"FAITH", "GODDESS", "FORGE", "MALLET", "WILL", "HELL", "GRAVITY", "FIRE_FORCE"};
        int[] ids = {SkillAnimeFx.FAITH, SkillAnimeFx.GODDESS, SkillAnimeFx.FORGE, SkillAnimeFx.MALLET, SkillAnimeFx.WILL, SkillAnimeFx.HELL, SkillAnimeFx.GRAVITY, SkillAnimeFx.FIRE_FORCE};
        assertEquals(styles.length, SkillAnimeVfx.SIGIL_STYLES.length);
        for (int i = 0; i < ids.length; i++) {
            assertEquals(styles[i], SkillAnimeVfx.SIGIL_STYLES[ids[i]]);
            assertEquals(ids[i], SkillAnimeVfx.sigil(ids[i] + 1f), "style " + styles[i] + " round trip");
        }
        assertEquals(0, SkillAnimeVfx.sigil(0f));
        assertEquals(0, SkillAnimeVfx.sigil(99f));
        // every mode of every shape survives the trip, the rest kept inside its step (a radius of 25 does not spill into the next mode)
        for (int modes : new int[]{2, 3, 4}) {
            for (int m = 0; m < modes; m++) {
                for (double rest : new double[]{0, 1, 6, 12.5, 19, 25}) {
                    float p = SkillAnimeFx.mode(m, rest);
                    assertEquals(m, SkillAnimeVfx.mode(p, modes), "mode " + m + " rest " + rest);
                    assertEquals(Math.min(rest, SkillAnimeVfx.STEP - 0.5), SkillAnimeVfx.rest(p, modes), 1e-4);
                }
            }
        }
        assertEquals(0, SkillAnimeVfx.mode(-3f, 4));
        assertEquals(0, SkillAnimeVfx.mode(999f, 4), "an unknown mode reads as the first");
    }

    @Test
    void everyShapeIsSentThroughTheHelperNextToTheOldEffects() throws IOException {
        String fx = code(SKILL_DIR.resolve("SkillAnimeFx.java"));
        assertTrue(fx.contains("VfxSpawn.send(") && fx.contains("VfxSpawn.sendFollowing("), "SkillAnimeFx must go through VfxSpawn (the per-tick effect budget)");
        for (VfxShape s : SHAPES) assertEquals(s != VfxShape.SKILL_WINGS, fx.contains("VfxShape." + s.name()), s + (s == VfxShape.SKILL_WINGS ? " is unused since Black Wings was removed" : " is never sent"));
        assertTrue(fx.contains("SelfTests.register(\"skill.vfx\""), "the self-test is registered under skill.vfx");
        assertTrue(code(ROOT.resolve("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("SkillAnimeFx.registerSelfTests()"));
        for (String f : WIRED) {
            String c = code(SKILL_DIR.resolve(f + ".java"));
            assertTrue(c.contains("SkillAnimeFx."), f + " does not send its anime effect");
            assertTrue(c.contains("SkillFx."), f + " lost its old effects (they stay as the accent and are what skills.classicVfx shows)");
            assertFalse(c.contains("VfxShape.SKILL_"), f + " must send the SKILL_ shapes through SkillAnimeFx");
        }
        assertTrue(Files.readString(ROOT.resolve(".github/workflows/smoke.yml"), StandardCharsets.UTF_8).contains(",greed,greed.vfx,devil.vfx,skill.vfx,"), "the skill.vfx self-test is on the smoke list");
        assertFalse(Files.exists(SKILL_DIR.resolve("BlackWingsSkill.java")), "Black Wings was removed");
    }
}
