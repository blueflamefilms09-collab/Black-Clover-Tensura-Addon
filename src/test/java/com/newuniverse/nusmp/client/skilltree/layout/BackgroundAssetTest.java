package com.newuniverse.nusmp.client.skilltree.layout;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: {@code TreeBackground} reads a texture's size from the first 24 bytes of its PNG (signature, then the IHDR width and height) and treats anything else as "no picture", falling back to the plain canvas.
 * The one placeholder background shipped with the engine ({@code fire_dragon_slayer.png}, found under the default name made from the route id) must satisfy exactly that reader, or the loader would silently never
 * show it; this test applies the same byte arithmetic to the file. It also pins the default location rule: {@code nusmp:fire_dragon_slayer} -> {@code assets/nusmp/textures/gui/skill_trees/fire_dragon_slayer.png}.
 */
class BackgroundAssetTest {
    @Test
    void thePlaceholderIsAPngTheLoaderCanMeasure() throws IOException {
        Path p = Path.of("src/main/resources/assets/nusmp/textures/gui/skill_trees/fire_dragon_slayer.png");
        byte[] h = Files.readAllBytes(p);
        assertTrue(h.length > 24);
        assertTrue((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G', "PNG signature");
        int w = ((h[16] & 0xFF) << 24) | ((h[17] & 0xFF) << 16) | ((h[18] & 0xFF) << 8) | (h[19] & 0xFF);
        int ht = ((h[20] & 0xFF) << 24) | ((h[21] & 0xFF) << 16) | ((h[22] & 0xFF) << 8) | (h[23] & 0xFF);
        assertTrue(w >= 256, "the background is at least 256 wide (the generated art is 256 x 256), was " + w);
        assertTrue(ht >= 256, "height at least 256, was " + ht);
    }
}
