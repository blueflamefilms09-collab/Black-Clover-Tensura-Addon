package com.newuniverse.nusmp.devil;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the 4.40.0 devil look (tools/gen_devil_textures_v2.py) must stay a drop-in: right sizes, marks on the base layer only, glow only where there is a mark, the tiers growing, and the old files still there (rule 2). */
class DevilTexturesV2Test {
    private static final Path DIR = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/anime_multiverse/textures/entity/devil");
    private static final String[] TIERS = {"low", "mid", "high", "supreme"};
    private static final int[][] OUTER = {{32, 0, 64, 16}, {16, 32, 40, 48}, {40, 32, 56, 48}, {48, 48, 64, 64}, {0, 32, 16, 48}, {0, 48, 16, 64}};

    private static BufferedImage load(String name) throws IOException { return ImageIO.read(DIR.resolve(name + ".png").toFile()); }

    private static int alpha(BufferedImage i, int x, int y) { return i.getRGB(x, y) >>> 24; }

    @Test
    void theOldTexturesAreKept() {
        for (String t : TIERS) for (String s : new String[] {"", "_slim"}) assertTrue(Files.exists(DIR.resolve("weg_" + t + s + ".png")), "weg_" + t + s);
        assertTrue(Files.exists(DIR.resolve("horns.png")) && Files.exists(DIR.resolve("wings.png")) && Files.exists(DIR.resolve("wings_glow.png")));
    }

    @Test
    void theNewMarkingsAreDropInAndOnlyOnTheBaseLayer() throws IOException {
        int last = 0;
        for (String t : TIERS) {
            for (String s : new String[] {"", "_slim"}) {
                BufferedImage ink = load("weg2_" + t + s), glow = load("weg2_" + t + s + "_glow");
                assertEquals(64, ink.getWidth());
                assertEquals(64, ink.getHeight());
                assertEquals(64, glow.getWidth());
                for (int[] o : OUTER)
                    for (int y = o[1]; y < o[3]; y++)
                        for (int x = o[0]; x < o[2]; x++) assertEquals(0, alpha(ink, x, y) + alpha(glow, x, y), "outer layer " + t + s + " at " + x + "," + y);
                int marks = 0;
                for (int y = 0; y < 64; y++)
                    for (int x = 0; x < 64; x++) {
                        if (alpha(ink, x, y) > 0) marks++;
                        if (alpha(glow, x, y) > 0) assertTrue(alpha(ink, x, y) > 0, "glow without a mark " + t + s + " at " + x + "," + y);
                    }
                if (s.isEmpty()) {
                    assertTrue(marks > last, t + " must carry more marks than the tier below");
                    assertTrue(marks >= 25 && marks <= 700, t + " has " + marks + " marked pixels");
                    last = marks;
                }
            }
        }
    }

    @Test
    void theNewHornsAndWingsKeepTheLayoutOfTheModels() throws IOException {
        BufferedImage horns = load("horns2"), wings = load("wings2"), glow = load("wings2_glow");
        assertEquals(32, horns.getWidth());
        assertEquals(32, horns.getHeight());
        assertEquals(64, wings.getWidth());
        assertEquals(64, wings.getHeight());
        assertEquals(64, glow.getHeight());
        for (int seg = 0; seg < 4; seg++) assertTrue(alpha(horns, 3, 8 * seg + 2) == 255, "horn segment " + seg + " is painted at its texture offset");
        assertTrue(alpha(wings, 5, 3) == 255 && alpha(wings, 40, 3) == 255 && alpha(wings, 56, 45) == 255, "bone and both membrane halves are painted");
    }
}
