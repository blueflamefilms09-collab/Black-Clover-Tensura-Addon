package com.newuniverse.nusmp.client.skilltree.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the lift is what keeps a route book's name readable on the shop's dark panel, and the ways it can go wrong are all arithmetic: a bright colour that is changed anyway, a dark
 * one that stays dark, a hue that drifts (a purple that comes out blue-grey is the bug the function exists to avoid), and a channel that overflows. So it is tested on bright colours,
 * on dark ones, on the extremes (black, white, pure blue) and over the whole colour cube for the invariants.
 */
class ShopColorsTest {
    private static int ch(int rgb, int shift) {
        return (rgb >> shift) & 0xFF;
    }

    @Test
    void aColourThatIsAlreadyBrightEnoughIsUntouched() {
        assertEquals(0xF0D060, ShopColors.lift(0xF0D060), "a bright gold stays exactly as it is");
        assertEquals(0xFFFFFF, ShopColors.lift(0xFFFFFF));
        assertEquals(0xE0E0E0, ShopColors.lift(0xE0E0E0));
    }

    @Test
    void aDarkColourIsLiftedAndKeepsItsHue() {
        int purple = ShopColors.lift(0x6A0DAD);
        assertTrue(ShopColors.luminance(purple) >= ShopColors.MIN_LUMINANCE - 1f, "bright enough: " + Integer.toHexString(purple));
        assertTrue(ch(purple, 16) > ch(purple, 8) && ch(purple, 0) > ch(purple, 8), "still a purple (red and blue above green): " + Integer.toHexString(purple));
        int green = ShopColors.lift(0x0A4020);
        assertTrue(ch(green, 8) > ch(green, 16) && ch(green, 8) > ch(green, 0), "a dark green stays a green: " + Integer.toHexString(green));
    }

    @Test
    void aSaturatedBlueIsStillDimToTheEyeSoItIsMixedTowardsWhite() {
        int blue = ShopColors.lift(0x0000FF);
        assertTrue(ShopColors.luminance(blue) >= 100f, "a pure blue is brighter after the lift: " + Integer.toHexString(blue));
        assertTrue(ch(blue, 0) == 255 && ch(blue, 16) > 0, "mixed with white, still blue at the peak: " + Integer.toHexString(blue));
    }

    @Test
    void blackAndNearBlackBecomeAPlainLightGreyAndAlphaIsIgnored() {
        assertEquals(0xB0B0B0, ShopColors.lift(0x000000));
        assertEquals(0xB0B0B0, ShopColors.lift(0x050505));
        assertEquals(ShopColors.lift(0x6A0DAD), ShopColors.lift(0xFF6A0DAD), "an alpha byte does not change the result");
    }

    @Test
    void everyColourOfTheCubeComesOutInRangeAndNeverDarker() {
        for (int r = 0; r < 256; r += 15) {
            for (int g = 0; g < 256; g += 15) {
                for (int b = 0; b < 256; b += 15) {
                    int in = (r << 16) | (g << 8) | b;
                    int out = ShopColors.lift(in);
                    assertTrue(out >= 0 && out <= 0xFFFFFF, "in range: " + Integer.toHexString(in));
                    if (Math.max(r, Math.max(g, b)) >= 8) {
                        assertTrue(ch(out, 16) >= r && ch(out, 8) >= g && ch(out, 0) >= b, "no channel gets darker: " + Integer.toHexString(in) + " -> " + Integer.toHexString(out));
                        int again = ShopColors.lift(out);
                        assertTrue(Math.abs(ch(again, 16) - ch(out, 16)) <= 2 && Math.abs(ch(again, 8) - ch(out, 8)) <= 2 && Math.abs(ch(again, 0) - ch(out, 0)) <= 2,
                                "lifting a lifted colour again moves it by rounding at most: " + Integer.toHexString(out) + " -> " + Integer.toHexString(again));
                    }
                }
            }
        }
    }
}
