package com.newuniverse.nusmp.avatar.appearance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The skin arithmetic: flesh takes the skin tone, the iris the eye colour, the outfit goes over both, and the colour quantiser bounds the texture count. */
class SkinComposeTest {
    private static int argb(int a, int r, int g, int b) { return (a << 24) | (r << 16) | (g << 8) | b; }

    @Test
    void fleshTakesTheSkinToneAndKeepsItsShading() {
        int[] base = {argb(255, 236, 236, 236), argb(255, 118, 118, 118)}, mask = {argb(255, 255, 0, 0), argb(255, 255, 0, 0)};
        int[] out = SkinCompose.compose(base, mask, null, 0xC08A60, 0);
        assertEquals(0xC08A60, out[0] & 0xFFFFFF, "full grey gives exactly the tone");
        assertTrue(((out[1] >> 16) & 255) < ((out[0] >> 16) & 255), "darker shading stays darker");
        assertEquals(255, out[0] >>> 24);
    }

    @Test
    void pixelsOutsideTheMaskAreUntouched() {
        int[] base = {argb(255, 10, 200, 30)}, mask = {0};
        assertEquals(base[0], SkinCompose.compose(base, mask, null, 0xFFFFFF, 0xFF0000)[0]);
    }

    @Test
    void theIrisTakesTheEyeColour() {
        int[] base = {argb(255, 150, 150, 150)}, mask = {argb(255, 0, 255, 0)};
        int out = SkinCompose.compose(base, mask, null, 0xFFFFFF, 0x2040FF)[0];
        assertTrue((out & 255) > ((out >> 16) & 255), "blue eyes are bluer than red");
    }

    @Test
    void anOpaqueOutfitPixelReplacesAndATransparentOneDoesNot() {
        int[] base = {argb(255, 236, 236, 236), argb(255, 236, 236, 236), 0}, mask = {argb(255, 255, 0, 0), argb(255, 255, 0, 0), 0};
        int[] outfit = {argb(255, 1, 2, 3), 0, argb(255, 9, 9, 9)};
        int[] out = SkinCompose.compose(base, mask, outfit, 0x808080, 0);
        assertEquals(argb(255, 1, 2, 3), out[0]);
        assertEquals(0x808080, out[1] & 0xFFFFFF);
        assertEquals(argb(255, 9, 9, 9), out[2], "an outfit may fill the second skin layer where the body has nothing");
    }

    @Test
    void aHalfTransparentOutfitBlendsOverTheBody() {
        int[] base = {argb(255, 0, 0, 0)}, outfit = {argb(128, 255, 255, 255)};
        int out = SkinCompose.compose(base, null, outfit, 0, 0)[0];
        int r = (out >> 16) & 255;
        assertTrue(r > 100 && r < 160, "r=" + r);
        assertEquals(255, out >>> 24);
    }

    @Test
    void quantizingBoundsTheNumberOfTextures() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int r = 0; r < 256; r += 3) for (int g = 0; g < 256; g += 5) for (int b = 0; b < 256; b += 7) seen.add(SkinCompose.quantize((r << 16) | (g << 8) | b));
        assertTrue(seen.size() <= 4096, "" + seen.size());
        assertEquals(0xFFFFFF, SkinCompose.quantize(0xFFFFFF));
        assertEquals(0, SkinCompose.quantize(0));
        assertEquals(SkinCompose.quantize(0x123456), SkinCompose.quantize(SkinCompose.quantize(0x123456)));
    }
}
