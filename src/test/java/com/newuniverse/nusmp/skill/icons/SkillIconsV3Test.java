package com.newuniverse.nusmp.skill.icons;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: tools/skills/gen_icons_v3.py repainted 121 skill icons and animated the Ultimates. Every icon the mod ships at textures/skill/ must be covered (repainted, or a known legacy icon that no longer
 * belongs to a registered skill), the old picture must be kept under classic/v440 and differ, every animated strip must be 32 px wide with at most 8 square frames and a valid .mcmeta, the class
 * frame must match the class (Unique cyan, Extra / Ultimate gold), and the index the game reads must match the manifest. Reads files only.
 */
class SkillIconsV3Test {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path TEX = ASSETS.resolve("textures/skill");
    /** Icons of skills that were removed (3.44.0 / 4.3.0): kept as they were, not repainted (rule 2). */
    private static final Set<String> LEGACY = Set.of("black_wings", "gold_experience", "gold_experience_requiem", "instant_death", "limitless", "six_eyes", "vector_manipulation",
            // the Saiyan Prodigy (docs/saiyan_prodigy.md): its own painted icon, outside the repaint manifest
            "saiyan_prodigy",
            // the five Berserk weapon skills (docs/berserk_weapons.md): painted by tools/gen_sin_skill_icons.py, outside the repaint manifest like the Saiyan Prodigy
            "lord_of_slothful_weight", "lord_of_green_eyed_envy", "lord_of_empty_vanity", "lord_of_the_twin_rings", "seifort_holy_judgement");

    private static Map<String, String[]> manifest() throws Exception {
        Map<String, String[]> m = new LinkedHashMap<>();
        for (String line : Files.readAllLines(Path.of("tools/skills/icons_v3.txt"), StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] p = line.split("\\|");
            assertEquals(3, p.length, "bad manifest line: " + line);
            assertNull(m.put(p[0], p), "duplicate id " + p[0]);
        }
        return m;
    }

    private static BufferedImage png(Path p) throws Exception {
        assertTrue(Files.exists(p), "missing " + p);
        return ImageIO.read(p.toFile());
    }

    private static double meanDiff(BufferedImage a, int ay, BufferedImage b, int by, int size) {
        double sum = 0;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                int p = a.getRGB(x, ay + y), q = b.getRGB(x, by + y);
                for (int s = 0; s <= 16; s += 8) sum += Math.abs(((p >> s) & 255) - ((q >> s) & 255));
            }
        return sum / (size * size * 3.0);
    }

    @Test
    void everyRootSkillIconIsCoveredOrLegacy() throws Exception {
        Map<String, String[]> m = manifest();
        File[] files = TEX.toFile().listFiles((d, n) -> n.endsWith(".png"));
        assertNotNull(files);
        List<String> missing = new ArrayList<>();
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - 4);
            if (!m.containsKey(id) && !LEGACY.contains(id)) missing.add(id);
        }
        assertTrue(missing.isEmpty(), "skill icons that are neither repainted nor legacy: " + missing);
        assertEquals(121, m.size(), "37 root + 54 Beast Will + 30 Dragon Ball icons");
    }

    @Test
    void everyIconIs32pxOpaqueWithRoundedCornersAndTheOldOneIsKept() throws Exception {
        for (String id : manifest().keySet()) {
            BufferedImage now = png(TEX.resolve(id + ".png")), old = png(TEX.resolve("classic/v440/" + id + ".png"));
            assertEquals(32, now.getWidth(), id);
            assertEquals(32, now.getHeight(), id);
            assertEquals(0, now.getRGB(0, 0) >>> 24, id + ": the corner pixel is transparent like Tensura's frames");
            assertEquals(255, now.getRGB(16, 16) >>> 24, id + ": the middle is opaque");
            assertEquals(32, old.getWidth(), id + " classic");
            assertTrue(meanDiff(now, 0, old, 0, 32) > 4.0, id + ": the new icon must differ from the old one");
        }
    }

    @Test
    void theFrameColourSaysTheClass() throws Exception {
        for (String[] e : manifest().values()) {
            BufferedImage im = png(TEX.resolve(e[0] + ".png"));
            int p = im.getRGB(1, 16), r = (p >> 16) & 255, b = p & 255;     // the lit pixel of the ring on the left edge
            if (e[1].equals("unique")) assertTrue(b > r, e[0] + ": a Unique has the cyan frame");
            else assertTrue(r > b + 40, e[0] + ": an Extra / Ultimate has the gold frame");
        }
    }

    @Test
    void ultimatesAreAnimatedAndNothingElseIs() throws Exception {
        for (String[] e : manifest().values()) {
            boolean ult = e[1].equals("ultimate");
            assertEquals(ult ? 8 : 1, Integer.parseInt(e[2]), e[0]);
            Path strip = TEX.resolve("anim/" + e[0] + "_anim.png");
            assertEquals(ult, Files.exists(strip), e[0] + ": only Ultimates have a strip");
        }
    }

    @Test
    void animatedStripsAreValid() throws Exception {
        for (String[] e : manifest().values()) {
            if (!e[1].equals("ultimate")) continue;
            Path strip = TEX.resolve("anim/" + e[0] + "_anim.png");
            BufferedImage im = png(strip);
            assertEquals(32, im.getWidth(), e[0]);
            assertEquals(32 * SkillIconIndex.MAX_FRAMES, im.getHeight(), e[0] + ": 8 frames of 32 x 32");
            JsonObject meta = JsonParser.parseString(Files.readString(Path.of(strip + ".mcmeta"))).getAsJsonObject();
            int ft = meta.getAsJsonObject("animation").get("frametime").getAsInt();
            assertTrue(ft >= 1 && ft <= 40, e[0] + " frametime");
            double moved = 0;
            for (int f = 1; f < 8; f++) moved = Math.max(moved, meanDiff(im, 0, im, f * 32, 32));
            assertTrue(moved > 0.3, e[0] + ": the frames must not all be the same picture");
            double loop = meanDiff(im, 7 * 32, im, 0, 32), step = meanDiff(im, 3 * 32, im, 4 * 32, 32);
            assertTrue(loop <= Math.max(step * 2.5, 6.0), e[0] + ": the loop must be seamless (last to first " + loop + ", a normal step " + step + ")");
        }
    }

    @Test
    void theIndexMatchesTheManifest() throws Exception {
        SkillIconIndex.Index idx = SkillIconIndex.parse(Files.readString(ASSETS.resolve("skill_icons.json")));
        Map<String, String[]> m = manifest();
        assertEquals(List.copyOf(m.keySet()), idx.styled());
        List<String> anim = new ArrayList<>();
        m.forEach((k, v) -> { if (v[1].equals("ultimate")) anim.add(k); });
        assertEquals(anim, idx.animated());
        assertEquals(8, idx.frames());
        assertTrue(idx.animated().size() * 20.0 / idx.frameTime() <= 400, "bounded upload rate");
    }

    @Test
    void badIndexesAreEmptyNeverAnException() {
        assertTrue(SkillIconIndex.parse(null).isEmpty());
        assertTrue(SkillIconIndex.parse("").isEmpty());
        assertTrue(SkillIconIndex.parse("not json").isEmpty());
        assertTrue(SkillIconIndex.parse("{\"styled\":[\"a\"],\"animated\":[\"b\"],\"frames\":8}").isEmpty(), "animated must be styled");
        assertTrue(SkillIconIndex.parse("{\"styled\":[\"A B\"],\"animated\":[],\"frames\":8}").isEmpty(), "bad id");
        assertTrue(SkillIconIndex.parse("{\"styled\":[\"a\"],\"animated\":[\"a\"],\"frames\":9}").isEmpty(), "too many frames");
        assertTrue(SkillIconIndex.parse("{\"styled\":[\"a\",\"a\"],\"animated\":[],\"frames\":8}").isEmpty(), "duplicate");
        SkillIconIndex.Index ok = SkillIconIndex.parse("{\"styled\":[\"a\",\"b\"],\"animated\":[\"b\"],\"frames\":8,\"frametime\":999}");
        assertEquals(2, ok.styled().size());
        assertEquals(40, ok.frameTime(), "frametime is clamped");
        assertEquals("textures/skill/anim/b_anim.png", SkillIconIndex.stripPath("b"));
        assertEquals("textures/skill/classic/v440/b.png", SkillIconIndex.classicPath("b"));
    }

    @Test
    void theShippedIndexLoadsFromTheClasspath() {
        assertFalse(SkillIconIndex.load().isEmpty());
    }

    @Test
    void theClientHelperIsBounded() throws Exception {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/skillicons/SkillIconSwap.java"));
        assertTrue(src.contains("sinceCheck < 20"), "the check runs once a second, not every tick");
        assertFalse(src.contains("RenderSystem"), "no GL state of its own");
    }
}
