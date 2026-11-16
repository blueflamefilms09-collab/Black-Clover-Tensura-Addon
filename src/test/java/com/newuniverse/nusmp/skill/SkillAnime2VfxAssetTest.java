package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.layer.SkillAnime2Vfx;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: VFX wave 4 (SkillAnime2Layer and its four drawers, docs/vfx_remake/skill_anime_wave4.md) reads its 15 textures by name, the server sends its 4 shapes by ordinal and packs a style, a family and
 * element, a motif and variant or an attribute and phase into the power, so all of it is pinned: every texture exists with a power-of-two size, visible and clear texels, clear corners and no black clear
 * texel next to a visible one, no stray skillanime2_ file, every name drawn and made by the generator; the four shapes are appended after SKILL_SIGIL in order; the layer is registered, keeps the engine's
 * 400-vertex cap, draws nothing with skills.classicVfx2 on and uses no vanilla particle; the server side (SkillAnimeFx2) and the client side (SkillAnime2Vfx) agree on every table and round-trip every
 * encoding; each wired skill sends through SkillAnimeFx2 and keeps its old effects; the repeating senders go through the throttle; the self-test is on the smoke list.
 */
class SkillAnime2VfxAssetTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path PARTICLE = ROOT.resolve("src/main/resources/assets/anime_multiverse/textures/particle");
    private static final Path LAYER_DIR = ROOT.resolve("src/main/java/com/newuniverse/nusmp/vfx/client/layer");
    private static final Path MAIN = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path SKILL_DIR = MAIN.resolve("skill");
    private static final String[] LAYER_FILES = {"SkillAnime2Layer.java", "SkillAnime2More.java", "SkillAnime2Beast.java", "SkillAnime2Hero.java", "SkillAnime2Grim.java"};
    private static final VfxShape[] SHAPES = {VfxShape.ANIME2_SKILL, VfxShape.ANIME2_BEAST, VfxShape.ANIME2_HERO, VfxShape.ANIME2_GRIMOIRE};
    /** The skills that send a wave 4 effect from their own file, and whether they keep an old SkillFx call next to it. */
    private static final String[][] WIRED = {{"LiarisFreeseSkill", "SkillAnimeFx2.liaris("}, {"HatiSkill", "SkillAnimeFx2.hati("}, {"HawkEyeSkill", "SkillAnimeFx2.hawk("},
            {"SpiritCardSkill", "SkillAnimeFx2.spiritCard("}, {"SummonerSkill", "SkillAnimeFx2.summon("}, {"BookMakerSkill", "SkillAnimeFx2.screw("}, {"DevilsFootprintsSkill", "SkillAnimeFx2.devilKick("},
            {"GateOfBabylonSkill", "SkillAnimeFx2.gateArmor("}, {"GateOfBabylonSkill", "SkillAnimeFx2.gateVimana("}, {"GateOfBabylonSkill", "SkillAnimeFx2.gateKing("},
            {"GateOfBabylonSkill", "SkillAnimeFx2.gateElixir("}, {"TheAlmightySkill", "SkillAnimeFx2.almighty("}, {"TheVisionarySkill", "SkillAnimeFx2.visionStar("},
            {"TheVisionarySkill", "SkillAnimeFx2.visionVoid("}, {"TheVisionarySkill", "SkillAnimeFx2.visionSelf("}, {"BeastMoves", "SkillAnimeFx2.beast("}};
    /** Of those, the ones that had an old SkillFx effect, which has to stay (it is what skills.classicVfx2 shows). */
    private static final String[] KEEP_OLD = {"LiarisFreeseSkill", "HatiSkill", "SpiritCardSkill", "SummonerSkill", "BookMakerSkill", "DevilsFootprintsSkill", "GateOfBabylonSkill", "TheAlmightySkill",
            "TheVisionarySkill", "BeastMoves"};

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    private static String layers() throws IOException {
        StringBuilder b = new StringBuilder();
        for (String f : LAYER_FILES) b.append(code(LAYER_DIR.resolve(f))).append('\n');
        return b.toString();
    }

    @Test
    void everyTextureIsThereWithAPowerOfTwoSizeAndSomethingToDraw() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String name : SkillAnime2Vfx.TEXTURE_NAMES) {
            Path f = PARTICLE.resolve(SkillAnime2Vfx.fileName(name) + ".png");
            assertTrue(Files.isRegularFile(f), "missing " + name);
            BufferedImage img = ImageIO.read(f.toFile());
            assertNotNull(img, name);
            int w = img.getWidth(), h = img.getHeight();
            if (Integer.bitCount(w) != 1 || Integer.bitCount(h) != 1 || w > 256 || h > 256) bad.add(name + " is " + w + "x" + h);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            int visible = 0, clear = 0;
            for (int p : px) if ((p >>> 24) > 20) visible++; else clear++;
            if (visible == 0 || clear == 0) bad.add(name + " has no visible or no clear texel");
            for (int c : new int[]{0, w - 1, (h - 1) * w, h * w - 1}) if ((px[c] >>> 24) != 0) bad.add(name + " corner is not clear");
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
        for (String p : SkillAnime2Vfx.allTextures()) expected.add(p.substring(p.lastIndexOf('/') + 1));
        Set<String> found = new HashSet<>();
        File[] files = PARTICLE.toFile().listFiles((d, n) -> n.startsWith(SkillAnime2Vfx.PREFIX));
        assertNotNull(files);
        for (File f : files) found.add(f.getName());
        assertEquals(expected, found, "the skillanime2_ files and the list in SkillAnime2Vfx must be the same set");
        String gen = Files.readString(ROOT.resolve("tools/gen_skill_anime2_vfx_textures.py"), StandardCharsets.UTF_8);
        assertTrue(gen.contains("PREFIX = \"skillanime2_\""));
        for (String n : SkillAnime2Vfx.TEXTURE_NAMES) assertTrue(gen.contains("\"" + n + "\""), "the generator does not make " + n);
    }

    @Test
    void everyTextureIsDrawnByTheLayer() throws IOException {
        String layer = code(LAYER_DIR.resolve("SkillAnime2Layer.java")), all = layers();
        for (String n : SkillAnime2Vfx.TEXTURE_NAMES) {
            String constant = n.toUpperCase(Locale.ROOT);
            assertTrue(layer.contains(constant + " = t(\"" + n + "\")"), n + " has no constant");
            assertTrue(all.split("\\b" + constant + "\\b", -1).length - 1 >= 2, n + " is generated but never drawn (a dead asset)");
        }
    }

    @Test
    void theShapesAreAppendedAfterSkillSigilAndTheLayerIsRegistered() throws IOException {
        for (int i = 0; i < SHAPES.length; i++) assertEquals(VfxShape.SKILL_SIGIL.ordinal() + 1 + i, SHAPES[i].ordinal(), "appended in order: " + SHAPES[i]);
        assertEquals(VfxShape.DEVIL_HIT.ordinal() + 9, VfxShape.SKILL_SIGIL.ordinal(), "wave 3 keeps its nine shapes before wave 4");
        String layer = code(LAYER_DIR.resolve("SkillAnime2Layer.java"));
        for (VfxShape s : SHAPES) assertTrue(layer.contains("VfxShape." + s.name()), s + " is not handled by SkillAnime2Layer");
        assertTrue(code(MAIN.resolve("vfx/client/VfxManager.java")).contains("register(new com.newuniverse.nusmp.vfx.client.layer.SkillAnime2Layer())"), "the layer is registered");
        assertFalse(layer.contains("hardVertexCap") || layer.contains("vertexBudget"), "the layer keeps the engine's 400-vertex cap per effect");
        assertTrue(layer.contains("if (SkillAnime2Vfx.classic()) return;"), "skills.classicVfx2 must leave the layer drawing nothing");
        String cfg = code(MAIN.resolve("vfx/client/SkillAnimeVfxConfig.java"));
        assertTrue(cfg.contains("SkillAnime2Vfx.setClassic(CLASSIC2.get())") && cfg.contains("\"classicVfx2\"") && cfg.contains(".define(\"classicVfx2\", false)"), "the client setting reaches the switch and is off by default");
        assertTrue(cfg.contains("\"classicVfx\""), "the wave 3 key stays");
        String both = layers();
        assertFalse(both.contains("ParticleTypes") || both.contains("net.minecraft.core.particles") || both.contains("Minecraft.getInstance"), "no vanilla particles and no client singleton in the layer");
        assertFalse(both.contains("new ArrayList") || both.contains("new HashMap"), "no per-frame collections in the layer");
    }

    @Test
    void theServerAndTheClientAgreeOnEveryTable() {
        assertEquals(SkillAnime2Vfx.STEP, SkillAnimeFx2.STEP);
        String[] skill = {"LIARIS", "HATI", "HAWK", "SPIRIT_CARD", "SUMMON", "SHISHOU", "GATE_ARMOR", "GATE_VIMANA", "GATE_KING", "GATE_ELIXIR", "DEVIL_KICK", "ALMIGHTY", "VISION_STAR", "VISION_VOID", "VISION_SELF"};
        int[] skillIds = {SkillAnimeFx2.LIARIS, SkillAnimeFx2.HATI, SkillAnimeFx2.HAWK, SkillAnimeFx2.SPIRIT_CARD, SkillAnimeFx2.SUMMON, SkillAnimeFx2.SHISHOU, SkillAnimeFx2.GATE_ARMOR, SkillAnimeFx2.GATE_VIMANA,
                SkillAnimeFx2.GATE_KING, SkillAnimeFx2.GATE_ELIXIR, SkillAnimeFx2.DEVIL_KICK, SkillAnimeFx2.ALMIGHTY, SkillAnimeFx2.VISION_STAR, SkillAnimeFx2.VISION_VOID, SkillAnimeFx2.VISION_SELF};
        assertEquals(skill.length, SkillAnime2Vfx.SKILL_STYLES.length);
        for (int i = 0; i < skill.length; i++) {
            assertEquals(skill[i], SkillAnime2Vfx.SKILL_STYLES[skillIds[i]], "skill style " + skill[i]);
            assertEquals(skillIds[i], SkillAnime2Vfx.skillStyle(SkillAnimeFx2.skillPower(skillIds[i], 0)), "skill style round trip " + skill[i]);
        }
        assertEquals(0, SkillAnime2Vfx.skillStyle(0f));
        assertEquals(0, SkillAnime2Vfx.skillStyle(99f));
        assertEquals(9f, SkillAnime2Vfx.skillExtra(SkillAnimeFx2.skillPower(SkillAnimeFx2.VISION_STAR, 9)), 0.05f);
        assertEquals(SkillAnimeFx2.VISION_STAR, SkillAnime2Vfx.skillStyle(SkillAnimeFx2.skillPower(SkillAnimeFx2.VISION_STAR, 49)), "the radius never spills into the next style");
        assertEquals(49f, SkillAnime2Vfx.skillExtra(SkillAnimeFx2.skillPower(SkillAnimeFx2.VISION_STAR, 200)), 0.05f, "a radius is clamped to 49");

        assertEquals(List.of(SkillAnimeFx2.ELEMENTS), List.of(SkillAnime2Vfx.ELEMENTS), "the beast elements");
        String[] fam = {"LINE", "RUSH", "BURST", "VOLLEY", "SEAL", "AURA"};
        int[] famIds = {SkillAnimeFx2.LINE, SkillAnimeFx2.RUSH, SkillAnimeFx2.BURST, SkillAnimeFx2.VOLLEY, SkillAnimeFx2.SEAL, SkillAnimeFx2.AURA};
        assertEquals(fam.length, SkillAnime2Vfx.BEAST_FAMILIES.length);
        for (int f = 0; f < fam.length; f++) {
            assertEquals(fam[f], SkillAnime2Vfx.BEAST_FAMILIES[famIds[f]]);
            for (int e = 0; e < SkillAnime2Vfx.ELEMENTS.length; e++) for (boolean big : new boolean[]{false, true}) {
                float p = SkillAnimeFx2.beastPower(famIds[f], e, big);
                assertEquals(famIds[f], SkillAnime2Vfx.beastFamily(p), "family " + fam[f]);
                assertEquals(e, SkillAnime2Vfx.beastElement(p), "element " + e);
                assertEquals(big, SkillAnime2Vfx.beastBig(p), "finisher " + big);
            }
        }
        String[] motifs = {"SLASH", "THRUST", "SHOCK", "WARD", "CAST", "THREAD", "SHARD"};
        int[] motifIds = {SkillAnimeFx2.SLASH, SkillAnimeFx2.THRUST, SkillAnimeFx2.SHOCK, SkillAnimeFx2.WARD, SkillAnimeFx2.CAST, SkillAnimeFx2.THREAD, SkillAnimeFx2.SHARD};
        assertEquals(motifs.length, SkillAnime2Vfx.HERO_MOTIFS.length);
        for (int m = 0; m < motifs.length; m++) {
            assertEquals(motifs[m], SkillAnime2Vfx.HERO_MOTIFS[motifIds[m]]);
            for (int v = 0; v <= 8; v++) for (boolean ult : new boolean[]{false, true}) {
                float p = SkillAnimeFx2.heroPower(motifIds[m], v, ult);
                assertEquals(motifIds[m], SkillAnime2Vfx.heroMotif(p), "motif " + motifs[m]);
                assertEquals(v, SkillAnime2Vfx.heroVariant(p), "variant " + v);
                assertEquals(ult, SkillAnime2Vfx.heroUltimate(p), "ultimate " + ult);
            }
        }
        String[] grim = {"COTTON", "MIRROR", "PLANT", "SEALING", "THREAD"};
        int[] grimIds = {SkillAnimeFx2.COTTON, SkillAnimeFx2.MIRROR, SkillAnimeFx2.PLANT, SkillAnimeFx2.SEALING, SkillAnimeFx2.THREAD_BOOK};
        assertEquals(grim.length, SkillAnime2Vfx.GRIMOIRE_STYLES.length);
        for (int a = 0; a < grim.length; a++) {
            assertEquals(grim[a], SkillAnime2Vfx.GRIMOIRE_STYLES[grimIds[a]]);
            for (boolean hit : new boolean[]{false, true}) for (double scale : new double[]{0, 0.6, 1, 1.4, 3, 9}) {
                float p = SkillAnimeFx2.grimPower(grimIds[a], hit, scale);
                assertEquals(grimIds[a], SkillAnime2Vfx.grimoireStyle(p), "attribute " + grim[a]);
                assertEquals(hit ? 1 : 0, SkillAnime2Vfx.grimoirePhase(p), "phase");
                assertEquals(Math.min(3.0, scale), SkillAnime2Vfx.grimoireScale(p), 1e-4, "scale " + scale);
            }
        }
        assertEquals(0, SkillAnime2Vfx.mode(-3f, 6));
        assertEquals(0, SkillAnime2Vfx.mode(999f, 6), "an unknown mode reads as the first");
    }

    @Test
    void everyHeroWeaponAndEveryBeastHasALook() {
        for (HeroKind k : HeroKind.values()) {
            int[] look = SkillAnimeFx2.heroLook(k);
            assertTrue(look[0] >= 0 && look[0] < SkillAnime2Vfx.HERO_MOTIFS.length && look[1] >= 0 && look[1] <= 4, k + " has no look");
        }
        for (var s : com.newuniverse.nusmp.skill.rules.BeastSkillRules.SPECS) {
            int e = SkillAnimeFx2.beastElement(s.key());
            assertTrue(e >= 0 && e < SkillAnime2Vfx.ELEMENTS.length, s.key());
        }
        Set<Integer> used = new HashSet<>();
        for (var s : com.newuniverse.nusmp.skill.rules.BeastSkillRules.SPECS) used.add(SkillAnimeFx2.beastElement(s.key()));
        assertEquals(SkillAnime2Vfx.ELEMENTS.length, used.size(), "all twelve element looks are used by the 27 beasts");
        for (var p : com.newuniverse.nusmp.skill.rules.DbRules.Prim.values()) {
            int f = SkillAnimeFx2.beastFamily(p);
            assertTrue(f >= 0 && f < SkillAnime2Vfx.BEAST_FAMILIES.length, p.name());
        }
    }

    @Test
    void theThrottleBoundsTheRepeatingSenders() throws IOException {
        SkillAnimeFx2.Throttle t = new SkillAnimeFx2.Throttle();
        UUID who = new UUID(7L, 9L);
        assertTrue(t.allow(who, 100, 60, 1));
        assertFalse(t.allow(who, 120, 60, 1), "once per window");
        assertTrue(t.allow(who, 160, 60, 1), "the next window");
        assertTrue(t.allow(who, 50, 60, 1), "a clock that went back (a world reload) starts a new window");
        assertFalse(t.allow(null, 1, 60, 1));
        for (int i = 0; i < 2000; i++) t.allow(new UUID(i, i), 1, 60, 1);
        String fx = code(SKILL_DIR.resolve("SkillAnimeFx2.java"));
        for (String gate : new String[]{"LIARIS_T.allow(", "HATI_T.allow(", "HAWK_T.allow(", "SPIRIT_T.allow(", "GRIM_CAST_T.allow(", "GRIM_HIT_T.allow(", "HERO_T.allow(", "SCREW_T.allow(", "BEAST_T.allow(", "SHISHOU_T.allow("})
            assertTrue(fx.contains(gate), gate + " is gone: a repeating sender must stay behind its throttle");
        assertTrue(fx.contains("if (seen.size() > CAP) seen.clear();"), "the throttle table is bounded");
        assertTrue(fx.contains("HAWK_T.allow(p.getUUID(), p.level().getGameTime(), 60, 1)") && fx.contains("SPIRIT_T.allow(p.getUUID(), p.level().getGameTime(), 60, 1)"), "the pulses draw once per 3 seconds at most");
    }

    @Test
    void everyShapeIsSentThroughTheHelperNextToTheOldEffects() throws IOException {
        String fx = code(SKILL_DIR.resolve("SkillAnimeFx2.java"));
        assertTrue(fx.contains("VfxSpawn.send(") && fx.contains("VfxSpawn.sendFollowing("), "SkillAnimeFx2 must go through VfxSpawn (the per-tick effect budget)");
        for (VfxShape s : SHAPES) assertTrue(fx.contains("VfxShape." + s.name()), s + " is never sent");
        assertTrue(fx.contains("SelfTests.register(\"skill.vfx2\""), "the self-test is registered under skill.vfx2");
        assertTrue(code(MAIN.resolve("NUSMP.java")).contains("SkillAnimeFx2.registerSelfTests()"));
        for (String[] w : WIRED) {
            String c = code(SKILL_DIR.resolve(w[0] + ".java"));
            assertTrue(c.contains(w[1]), w[0] + " does not send " + w[1]);
            assertFalse(c.contains("VfxShape.ANIME2_"), w[0] + " must send the ANIME2_ shapes through SkillAnimeFx2");
        }
        for (String f : KEEP_OLD) {
            String c = code(SKILL_DIR.resolve(f + ".java"));
            assertTrue(c.contains("SkillFx."), f + " lost its old effects (they stay and are what skills.classicVfx2 shows)");
        }
        // the wave 3 calls stay next to the new ones
        assertTrue(code(SKILL_DIR.resolve("GateOfBabylonSkill.java")).contains("SkillAnimeFx.gateRain("), "wave 3's gate effects stay");
        assertTrue(code(SKILL_DIR.resolve("DevilsFootprintsSkill.java")).contains("SkillAnimeFx.sigil("), "wave 3's kick jets stay");
        String hero = code(MAIN.resolve("heroweapon/skill/HeroUse.java"));
        assertTrue(hero.contains("SkillAnimeFx2.hero(player, c.kind, skill.ultimate())") && hero.contains("HeroSync.sendCooldown("), "the hero dispatcher sends one effect per technique, after the cast really happened");
        String shishou = code(MAIN.resolve("shishou/ShishouGrantService.java"));
        assertTrue(shishou.contains("ShishouService.cast(p, cur);") && shishou.contains("SkillAnimeFx2.shishou(p, modeIndex)"));
        for (String book : new String[]{"CottonBook", "MirrorBook", "PlantBook", "SealingBook", "ThreadBook"}) {
            String c = code(MAIN.resolve("book/" + book + ".java"));
            assertTrue(c.contains("SkillAnimeFx2.grimCast(") && c.contains("SkillAnimeFx2.grimImpact("), book + " lost its wave 4 cast or impact");
        }
        assertTrue(code(MAIN.resolve("book/PlantBook.java")).contains("VfxShape.PLANT_FX2") && code(MAIN.resolve("book/PlantBook.java")).contains("VfxShape.PLANT_FX3"), "Plant keeps its old circle and burst");
        for (String book : new String[]{"CottonBook", "MirrorBook", "SealingBook", "ThreadBook"})
            assertTrue(code(MAIN.resolve("book/" + book + ".java")).contains("super.castCircle(p, power);") && code(MAIN.resolve("book/" + book + ".java")).contains("super.impact(p, at, power);"), book + " keeps the old circle and burst");
        String smoke = Files.readString(ROOT.resolve(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(smoke.contains(",skill.vfx,skill.vfx2,"), "the skill.vfx2 self-test is on the smoke list, right after skill.vfx");
    }
}
