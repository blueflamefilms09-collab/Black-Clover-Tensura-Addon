package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 1.110.0: the blend maths of the central additive helper (docs/visual_upgrade/safe_additive.md), and the classifier rule that sends a straight-alpha white card to it. */
class SafeAdditiveMathTest {
    @Test
    void aTransparentTexelAddsNothingWhateverColourItCarries() {
        assertEquals(0.3f, SafeAdditiveMath.alphaAdd(0.3f, 1f, 0f, 1f), 0f);
        assertTrue(SafeAdditiveMath.addsNothing(0f, 1f));
        assertTrue(SafeAdditiveMath.addsNothing(1f, 0f));
        assertFalse(SafeAdditiveMath.addsNothing(0.2f, 0.5f));
        assertEquals(1f, SafeAdditiveMath.plainAdd(0.3f, 1f), 0f, "the plain add lights a colour-under-alpha texel fully: the box");
    }

    @Test
    void theAlphaWeightedAddScalesByTextureAlphaAndVertexAlpha() {
        assertEquals(0.5f, SafeAdditiveMath.alphaAdd(0f, 1f, 0.5f, 1f), 1e-6f);
        assertEquals(0.25f, SafeAdditiveMath.alphaAdd(0f, 1f, 0.5f, 0.5f), 1e-6f);
        assertEquals(1f, SafeAdditiveMath.alphaAdd(0.9f, 1f, 1f, 1f), 0f, "clamped");
        assertTrue(SafeAdditiveMath.alphaAdd(0.2f, -1f, 1f, 1f) >= 0f, "never negative");
    }

    @Test
    void aPremultipliedTexelIsWeighedTwiceButNeverBrighterThanTheStraightOne() {
        float straight = SafeAdditiveMath.alphaAdd(0f, 1f, 0.5f, 1f), premult = SafeAdditiveMath.alphaAddPremultiplied(0f, 0.5f, 0.5f, 1f);
        assertTrue(premult <= straight);
        assertEquals(0.25f, premult, 1e-6f);
    }

    @Test
    void theEyesShaderDropsFaintTexels() {
        assertFalse(SafeAdditiveMath.eyesKeeps(0.05f));
        assertTrue(SafeAdditiveMath.eyesKeeps(0.1f));
    }

    @Test
    void cardAndFlareCapsAreRelativeToTheWeapon() {
        assertTrue(SafeAdditiveMath.cardEdgeOk(84f, 125.6f), "Elsdocia's light shaft");
        assertFalse(SafeAdditiveMath.cardEdgeOk(300f, 125.6f));
        assertEquals(1f, SafeAdditiveMath.cardScale(84f, 125.6f), 0f);
        assertEquals(0.9f * 100f / 300f, SafeAdditiveMath.cardScale(300f, 100f), 1e-6f);
        assertTrue(SafeAdditiveMath.flareOk(6f, 125.6f));
        assertFalse(SafeAdditiveMath.flareOk(40f, 100f));
    }

    private static int[] card(int rgb, int alpha, int n) {
        int[] px = new int[n * n];
        for (int i = 0; i < px.length; i++) px[i] = alpha << 24 | rgb;
        return px;
    }

    @Test
    void aWhiteCardWithASoftStraightAlphaIsNotPremultipliedSoItTakesTheAlphaBlend() {
        int n = 32;
        int[] px = card(0xFFFFFF, 40, n);                                  // white at alpha 40 everywhere: the old halo cell
        px[0] = 0;                                                         // one fully transparent texel so the classifier looks at the semi-transparent ones
        assertEquals(TextureAlpha.Kind.COLOUR_UNDER_ALPHA, TextureAlpha.classify(n, n, px));
        assertEquals(AdditivePolicy.Blend.ALPHA_ADD, AdditivePolicy.choose(TextureAlpha.classify(n, n, px), false));
    }

    @Test
    void aPremultipliedGlowKeepsTheEyesTypeAsBefore() {
        int n = 32;
        int[] px = new int[n * n];
        for (int i = 0; i < px.length; i++) { int a = i % 200 + 20; px[i] = a << 24 | a << 16 | a << 8 | a; }   // rgb equal to alpha
        for (int i = 0; i < 40; i++) px[i] = 0;
        assertEquals(TextureAlpha.Kind.BLACK_UNDER_ALPHA, TextureAlpha.classify(n, n, px));
    }
}
