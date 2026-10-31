package com.newuniverse.nusmp.quests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (3.44.0): the "Multiverse of Anime" advancement tab got its own seamless background. The game draws it by blitting one texture over and over in 16x16 GUI-pixel cells, with the
 * texture size declared as 16x16, so a texture that is not square, has transparent pixels (they show the black fill under the tiles) or is bright enough to wash out the advancement frames would
 * look wrong with no error anywhere. This reads root.json and the texture as files and pins those rules, and that the generator spec names the same texture as the shipped root.
 */
class AdvancementBackgroundTest {
    private static final Path ROOT = Path.of("src/main/resources/data/nusmp/advancement/root.json");
    private static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    private static final String NEW = "nusmp:textures/gui/advancements/multiverse_weave.png";

    private static String background() throws Exception {
        JsonObject o = JsonParser.parseString(Files.readString(ROOT)).getAsJsonObject();
        return o.getAsJsonObject("display").get("background").getAsString();
    }

    private static File file(String id) {
        return ASSETS.resolve(id.substring(id.indexOf(':') + 1)).toFile();
    }

    @Test
    void theRootUsesTheNewBackgroundAndItExists() throws Exception {
        assertEquals(NEW, background());
        assertTrue(file(background()).isFile(), "the background texture is in the jar");
    }

    @Test
    void everyTileIsSquareOpaqueDarkAndAMultipleOfSixteen() throws Exception {
        for (String name : new String[] {"multiverse_weave", "sigil_tiles", "grimoire_pages"}) {
            File f = file("nusmp:textures/gui/advancements/" + name + ".png");
            assertTrue(f.isFile(), name + " missing");
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, name + " unreadable");
            assertEquals(img.getWidth(), img.getHeight(), name + " must be square (the game squeezes it into a 16x16 cell)");
            assertEquals(0, img.getWidth() % 16, name + " side must be a multiple of 16");
            assertTrue(img.getWidth() <= 64, name + " stays small");
            long sum = 0;
            for (int y = 0; y < img.getHeight(); y++)
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    assertEquals(255, argb >>> 24, name + " has a transparent pixel at " + x + "," + y);
                    sum += (((argb >> 16) & 255) * 299 + ((argb >> 8) & 255) * 587 + (argb & 255) * 114) / 1000;
                }
            double mean = sum / (double) (img.getWidth() * img.getHeight());
            assertTrue(mean < 60, name + " must stay dark so the frames and lines read (mean luma " + mean + ")");
        }
    }

    @Test
    void theOldAltarTileIsKeptForTheAltarBlock() {
        assertTrue(file("nusmp:textures/block/grimoire_altar_top.png").isFile(), "rule 2: the old texture stays");
        assertTrue(Files.exists(ASSETS.resolve("models/block/grimoire_altar.json")));
    }

    @Test
    void theGeneratorSpecNamesTheSameBackgroundAsTheShippedRoot() throws Exception {
        String spec = Files.readString(Path.of("tools/quests/spec_adv.py"));
        assertTrue(spec.contains("ROOT_BG = \"" + NEW + "\""), "tools/quests/spec_adv.py must keep ROOT_BG in step with root.json");
    }
}
