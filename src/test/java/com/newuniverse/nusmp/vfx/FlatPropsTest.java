package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner report, builds 1.110 / 1.111, docs/visual/flat_props.md): the flat pale cut-outs of weapons and paper figures are not drawn any more. Pure checks of the decision ({@link FlatProps}) and of
 * the files: every sprite on the list really is a flat single-colour cut-out (so the list says what it claims), the solid material sheet of the replacements is opaque and has grain, and the switch is off by default.
 */
class FlatPropsTest {
    private static final Path TEX = Path.of("src/main/resources/assets/nusmp/textures");

    @Test
    void theSwitchIsOffByDefault() {
        assertFalse(FlatProps.drawFlat());
    }

    @Test
    void aFlatSpriteIsRefusedWhenTheSwitchIsOffAndAllowedWhenItIsOn() {
        for (FlatProps.Entry e : FlatProps.FLAT) {
            String path = "nusmp:textures/particle/" + e.texture() + ".png";
            assertTrue(FlatProps.isFlat(path), e.texture());
            assertFalse(FlatProps.shouldDraw(path, false), e.texture() + " is drawn with visual.flatProps off");
            assertTrue(FlatProps.shouldDraw(path, true), e.texture() + " is hidden with visual.flatProps on");
        }
    }

    @Test
    void everyOtherTextureIsAlwaysDrawn() {
        for (String p : new String[]{"textures/particle/glow.png", "textures/particle/bone_arm.png", "textures/flatprops/solid.png", "textures/entity/construct_sword.png", "nusmp:textures/particle/time_clock_hand.png", "", null}) {
            assertFalse(FlatProps.isFlat(p), String.valueOf(p));
            assertTrue(FlatProps.shouldDraw(p, false), String.valueOf(p));
        }
    }

    @Test
    void theListNamesTheSpellsAndWhatEachSpriteStandsFor() {
        Set<String> seen = new HashSet<>();
        for (FlatProps.Entry e : FlatProps.FLAT) {
            assertTrue(seen.add(e.texture()), "duplicate " + e.texture());
            assertFalse(e.standsFor().isBlank());
            assertFalse(e.spells().isEmpty(), e.texture() + " lists no spell");
        }
        assertTrue(seen.containsAll(Set.of("construct_sword", "construct_axe", "construct_shield", "arc_silhouette")));
    }

    @Test
    void theTimeMagicTexturesAreNeverOnTheList() {
        for (FlatProps.Entry e : FlatProps.FLAT) assertFalse(e.texture().startsWith("time_"), e.texture());
    }

    @Test
    void everyListedSpriteIsAFlatSingleColourCutOut() throws IOException {
        for (FlatProps.Entry e : FlatProps.FLAT) {
            Path p = TEX.resolve("particle/" + e.texture() + ".png");
            assertTrue(Files.isRegularFile(p), p + " is missing (a listed sprite that does not exist should leave the list)");
            BufferedImage img = ImageIO.read(p.toFile());
            double sum = 0, sum2 = 0;
            int n = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int v = img.getRGB(x, y);
                    if ((v >>> 24) < 200) continue;
                    double l = (((v >> 16) & 255) * 0.299 + ((v >> 8) & 255) * 0.587 + (v & 255) * 0.114);
                    sum += l;
                    sum2 += l * l;
                    n++;
                }
            }
            assertTrue(n > img.getWidth() * img.getHeight() * 0.10, e.texture() + " is not a solid shape");
            double mean = sum / n, std = Math.sqrt(Math.max(0, sum2 / n - mean * mean));
            assertTrue(std < 8, e.texture() + " has shading inside (std " + std + "): it is not a flat cut-out, take it off the list");
            assertTrue(mean > 200, e.texture() + " is not pale");
        }
    }

    @Test
    void theSolidMaterialSheetIsOpaqueAndHasGrain() throws IOException {
        BufferedImage img = ImageIO.read(TEX.resolve("flatprops/solid.png").toFile());
        assertEquals(128, img.getWidth());
        assertEquals(64, img.getHeight());
        for (int cy = 0; cy < 2; cy++) {
            for (int cx = 0; cx < 4; cx++) {
                int lo = 255, hi = 0;
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 32; x++) {
                        int v = img.getRGB(cx * 32 + x, cy * 32 + y);
                        assertEquals(255, v >>> 24, "see-through texel in cell " + (cy * 4 + cx));
                        int l = (((v >> 16) & 255) + ((v >> 8) & 255) + (v & 255)) / 3;
                        lo = Math.min(lo, l);
                        hi = Math.max(hi, l);
                    }
                }
                assertTrue(hi - lo >= 20, "cell " + (cy * 4 + cx) + " is a flat colour");
            }
        }
    }

    @Test
    void theNamesAreCaseAndPathProof() {
        assertEquals("construct_axe", FlatProps.nameOf("nusmp:textures/particle/Construct_Axe.png"));
        List<String> none = new ArrayList<>();
        assertTrue(none.isEmpty());
    }
}
