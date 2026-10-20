package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the square-bug fix chooses its blend from {@link TextureAlpha#classify}. The cases here are the exact ones of the bug: a ring painted "colour everywhere, alpha shapes it" (transparent corners
 * that still carry white) is COLOUR_UNDER_ALPHA, the same ring with its colour premultiplied (black corners) is BLACK_UNDER_ALPHA, a ring that lost its alpha is an opaque square.
 */
class TextureAlphaTest {
    private static final int N = 32;

    /** A disc of the given colour with alpha 255 inside radius 8 and {@code outside} everywhere else. */
    private static int[] disc(int discArgb, int outsideArgb) {
        int[] px = new int[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double d = Math.hypot(x - 15.5, y - 15.5);
                px[y * N + x] = d < 8 ? discArgb : outsideArgb;
            }
        }
        return px;
    }

    @Test
    void aRingWithColourUnderItsTransparentCornersIsTheSquareProneKind() {
        int[] px = disc(0xFFFFFFFF, 0x00FFFFFF);                           // white disc, white under alpha 0: air_ring and flamefx_ring look like this
        assertEquals(TextureAlpha.Kind.COLOUR_UNDER_ALPHA, TextureAlpha.classify(N, N, px));
        assertTrue(TextureAlpha.cornersTransparent(N, N, px, 0));
    }

    @Test
    void aRingWithBlackUnderItsTransparentCornersNeedsNoFix() {
        int[] px = disc(0xFFFFFFFF, 0x00000000);
        assertEquals(TextureAlpha.Kind.BLACK_UNDER_ALPHA, TextureAlpha.classify(N, N, px));
    }

    @Test
    void aFaintTraceOfColourUnderAlphaStaysBelowTheThreshold() {
        int[] px = disc(0xFFFFFFFF, 0x00000000);
        for (int i = 0; i < 20; i++) px[i * 7] = px[i * 7] == 0xFFFFFFFF ? px[i * 7] : 0x00FFFFFF;      // 20 of ~800 transparent texels
        assertEquals(TextureAlpha.Kind.BLACK_UNDER_ALPHA, TextureAlpha.classify(N, N, px));
    }

    @Test
    void aTextureThatLostItsAlphaIsAnOpaqueSquare() {
        assertEquals(TextureAlpha.Kind.OPAQUE_BRIGHT_EDGE, TextureAlpha.classify(N, N, disc(0xFFFFFFFF, 0xFFE0E0E0)));
        assertFalse(TextureAlpha.cornersTransparent(N, N, disc(0xFFFFFFFF, 0xFFE0E0E0), 16));
    }

    @Test
    void aSkinOnBlackIsOpaqueButNotASquare() {
        assertEquals(TextureAlpha.Kind.OPAQUE, TextureAlpha.classify(N, N, disc(0xFFFFFFFF, 0xFF000000)));
    }

    @Test
    void aTextureWithNothingInItIsEmpty() {
        assertEquals(TextureAlpha.Kind.EMPTY, TextureAlpha.classify(N, N, new int[N * N]));
        assertEquals(TextureAlpha.Kind.EMPTY, TextureAlpha.classify(0, 0, new int[0]));
        assertEquals(TextureAlpha.Kind.EMPTY, TextureAlpha.classify(N, N, null));
    }

    @Test
    void theAlphaThresholdTreatsNearlyInvisibleTexelsAsTransparent() {
        int[] px = disc(0xFFFFFFFF, 0x08FFFFFF);                           // alpha 8: still counted as transparent
        assertEquals(TextureAlpha.Kind.COLOUR_UNDER_ALPHA, TextureAlpha.classify(N, N, px));
        px = disc(0xFFFFFFFF, 0x09FFFFFF);                                 // alpha 9: a faint but real surface
        assertEquals(TextureAlpha.Kind.OPAQUE_BRIGHT_EDGE, TextureAlpha.classify(N, N, px));
    }
}
