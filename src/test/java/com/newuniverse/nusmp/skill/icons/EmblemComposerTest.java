package com.newuniverse.nusmp.skill.icons;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: every grimoire magic and every devil attribute has an emblem, and the composed icon is deterministic, bounded and loops (docs/devil_double_magic.md). Pure plus file reads; the working directory is the project root. */
class EmblemComposerTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path EMB = ROOT.resolve("src/main/resources/assets/anime_multiverse/textures/skill/emblem");

    private static int[] load(Path p) throws IOException {
        var im = ImageIO.read(p.toFile());
        int[] px = new int[16 * 16];
        im.getRGB(0, 0, 16, 16, px, 0, 16);
        return px;
    }

    private static List<String> magicKeys() throws IOException {
        String src = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java"));
        Matcher m = Pattern.compile("(?m)^    ([A-Z_]+)\\(\"[^\"]*\", \"[^\"]*\", \"[A-Z_]+\"").matcher(src);
        List<String> out = new ArrayList<>();
        while (m.find()) out.add(m.group(1).toLowerCase());
        return out;
    }

    private static List<String> devilKeys() throws IOException {
        String src = Files.readString(ROOT.resolve("src/main/java/com/newuniverse/nusmp/devil/DevilAttribute.java"));
        String body = src.substring(src.indexOf("public enum DevilAttribute")).split(";", 2)[0].replaceAll("(?s)/\\*.*?\\*/", "");
        Matcher m = Pattern.compile("\\b([A-Z][A-Z_]+)\\b").matcher(body);
        List<String> out = new ArrayList<>();
        while (m.find()) if (!m.group(1).equals("NONE")) out.add(m.group(1).toLowerCase());
        return out;
    }

    @Test
    void everyMagicAndEveryDevilAttributeHasAnEmblemOf16x16() throws IOException {
        List<String> magics = magicKeys(), devils = devilKeys();
        assertTrue(magics.size() >= 100, "found " + magics.size() + " grimoire magics");
        assertEquals(17, devils.size());
        for (String k : magics) { assertTrue(EmblemComposer.validKey(k), k); assertTrue(Files.exists(EMB.resolve("magic/" + k + ".png")), "emblem for magic " + k); }
        for (String k : devils) { assertTrue(EmblemComposer.validKey(k), k); assertTrue(Files.exists(EMB.resolve("devil/" + k + ".png")), "emblem for devil attribute " + k); }
        for (String k : devils) {
            var im = ImageIO.read(EMB.resolve("devil/" + k + ".png").toFile());
            assertEquals(16, im.getWidth());
            assertEquals(16, im.getHeight());
        }
    }

    @Test
    void noTwoEmblemsAreTheSamePicture() throws IOException {
        List<int[]> all = new ArrayList<>();
        for (String k : magicKeys()) all.add(load(EMB.resolve("magic/" + k + ".png")));
        for (String k : devilKeys()) all.add(load(EMB.resolve("devil/" + k + ".png")));
        for (int i = 0; i < all.size(); i++) {
            int solid = 0;
            for (int p : all.get(i)) if ((p >>> 24) > 40) solid++;
            assertTrue(solid >= 12 && solid <= 200, "emblem " + i + " has " + solid + " solid pixels");
            for (int j = i + 1; j < all.size(); j++) assertFalse(Arrays.equals(all.get(i), all.get(j)), "emblems " + i + " and " + j + " are the same");
        }
    }

    @Test
    void theFrameIsDeterministicBoundedAndDifferentFromFrameToFrame() throws IOException {
        int[] devil = load(EMB.resolve("devil/time.png")), magic = load(EMB.resolve("magic/time.png"));
        int[] a = EmblemComposer.frame(devil, 0xF5D76E, magic, 0xE0C060, 0, EmblemComposer.FRAMES);
        assertArrayEquals(a, EmblemComposer.frame(devil, 0xF5D76E, magic, 0xE0C060, 0, EmblemComposer.FRAMES), "same input, same picture");
        assertEquals(EmblemComposer.SIZE * EmblemComposer.SIZE, a.length);
        assertNotEquals(Arrays.hashCode(a), Arrays.hashCode(EmblemComposer.frame(devil, 0xF5D76E, magic, 0xE0C060, 3, EmblemComposer.FRAMES)), "the ring breathes and the spark moves");
        assertArrayEquals(a, EmblemComposer.frame(devil, 0xF5D76E, magic, 0xE0C060, EmblemComposer.FRAMES, EmblemComposer.FRAMES), "the loop is closed");
        assertEquals(0, a[0] >>> 24, "the corner outside the disc is clear");
        int[] alone = EmblemComposer.frame(devil, 0xF5D76E, null, 0, 0, EmblemComposer.FRAMES);
        assertNotEquals(Arrays.hashCode(a), Arrays.hashCode(alone), "the grimoire badge shows when there is a grimoire");
        assertEquals(EmblemComposer.SIZE * EmblemComposer.SIZE * EmblemComposer.FRAMES, EmblemComposer.strip(devil, 1, null, 0, 99).length, "a strip is at most FRAMES frames");
    }

    @Test
    void badInputNeverThrows() {
        assertEquals(1024, EmblemComposer.frame(null, 0, null, 0, -5, 0).length);
        assertEquals(1024, EmblemComposer.frame(new int[3], 0xFFFFFF, new int[1], 0xFFFFFF, 7, 3).length);
        assertFalse(EmblemComposer.validKey("../x"));
        assertFalse(EmblemComposer.validKey(null));
        assertEquals(0xB0B0B0, EmblemComposer.soulColor("nonsense"));
        assertEquals("a|", EmblemComposer.cacheKey("a", null));
    }
}
