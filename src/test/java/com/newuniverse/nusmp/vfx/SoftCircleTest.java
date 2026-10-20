package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the soft circle replaces a ring or glow that is missing or opaque, so it must be what that texture should have been: transparent corners, a bright centre, white RGB, and a smooth fall-off. */
class SoftCircleTest {
    @Test
    void theCornersAndTheEdgesAreTransparentAndTheCentreIsSolid() {
        int n = SoftCircle.SIZE;
        int[] px = SoftCircle.argb(n);
        assertEquals(n * n, px.length);
        assertTrue(TextureAlpha.cornersTransparent(n, n, px, 0), "the corners must have alpha 0");
        for (int i = 0; i < n; i++) {                                           // the outer rows and columns are transparent too: no hard edge at the border of the square
            assertEquals(0, px[i] >>> 24);
            assertEquals(0, px[(n - 1) * n + i] >>> 24);
            assertEquals(0, px[i * n] >>> 24);
            assertEquals(0, px[i * n + n - 1] >>> 24);
        }
        assertTrue((px[n / 2 * n + n / 2] >>> 24) >= 240, "the middle is nearly opaque");
    }

    @Test
    void alphaFallsOffMonotonicallyFromTheCentreAndTheRgbIsWhite() {
        int n = SoftCircle.SIZE;
        int[] px = SoftCircle.argb(n);
        int row = n / 2;
        int last = 256;
        for (int x = n / 2; x < n; x++) {
            int a = px[row * n + x] >>> 24;
            assertTrue(a <= last, "alpha must not rise towards the edge (x=" + x + ")");
            last = a;
            assertEquals(0xFFFFFF, px[row * n + x] & 0xFFFFFF);
        }
    }

    @Test
    void theCircleClassifiesAsAWellBehavedSprite() {
        int n = SoftCircle.SIZE;
        assertEquals(TextureAlpha.Kind.COLOUR_UNDER_ALPHA, TextureAlpha.classify(n, n, SoftCircle.argb(n)), "white under alpha: it takes the alpha-weighted blend, and its corners add nothing");
    }
}
