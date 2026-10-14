package com.newuniverse.nusmp.relics.djinn.dark;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The Dark Rukh Shard's art (tools/relics/dark_item_icon.py): a 16x16 sprite with some opaque pixels, and a plain generated model that points at it. */
class DarkAssetsTest {
    private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "nusmp");

    @Test
    void theShardHasASixteenBySixteenSpriteAndAPlainModel() throws IOException {
        Path png = ASSETS.resolve("textures/item/dark_rukh_shard.png");
        assertTrue(Files.exists(png), "missing " + png);
        BufferedImage im = ImageIO.read(png.toFile());
        assertEquals(16, im.getWidth());
        assertEquals(16, im.getHeight());
        int opaque = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((im.getRGB(x, y) >>> 24) > 0) opaque++;
        assertTrue(opaque > 40 && opaque < 200, "a splinter, not a block and not empty: " + opaque);
        Path model = ASSETS.resolve("models/item/dark_rukh_shard.json");
        assertTrue(Files.exists(model), "missing " + model);
        String json = Files.readString(model);
        assertTrue(json.contains("minecraft:item/generated"));
        assertTrue(json.contains("nusmp:item/dark_rukh_shard"));
    }
}
