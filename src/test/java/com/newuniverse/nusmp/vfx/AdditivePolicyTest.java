package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the decisions of the square-bug fix. Only a texture that carries a colour under its transparent texels (or one that cannot be read) leaves vanilla's eyes type, so every aura whose
 * textures were fine keeps exactly its old look; the forced safe path changes all of them; the Time Magic textures are never swapped for the soft circle.
 */
class AdditivePolicyTest {
    @Test
    void onlyTheSquareProneKindLeavesVanillaEyes() {
        for (TextureAlpha.Kind k : TextureAlpha.Kind.values()) {
            AdditivePolicy.Blend b = AdditivePolicy.choose(k, false);
            assertEquals(k == TextureAlpha.Kind.COLOUR_UNDER_ALPHA ? AdditivePolicy.Blend.ALPHA_ADD : AdditivePolicy.Blend.VANILLA_EYES, b, k.name());
        }
    }

    @Test
    void anUnreadableTextureTakesTheBlendThatCannotMakeASquare() {
        assertEquals(AdditivePolicy.Blend.ALPHA_ADD, AdditivePolicy.choose(null, false));
    }

    @Test
    void theForcedSafePathIsAlphaWeightedForEverything() {
        for (TextureAlpha.Kind k : TextureAlpha.Kind.values()) assertEquals(AdditivePolicy.Blend.ALPHA_ADD, AdditivePolicy.choose(k, true), k.name());
        assertEquals(AdditivePolicy.Blend.ALPHA_ADD, AdditivePolicy.choose(null, true));
    }

    @Test
    void aMissingTextureBecomesTheSoftCircleAnywhere() {
        assertTrue(AdditivePolicy.replaceWithSoftCircle(null, false, "textures/particle/gone.png", false));
        assertTrue(AdditivePolicy.replaceWithSoftCircle(null, true, "textures/particle/gone.png", false));
    }

    @Test
    void anOpaqueFlatSpriteIsSwappedOnlyInAnAdditiveBatchAndOnlyByName() {
        TextureAlpha.Kind k = TextureAlpha.Kind.OPAQUE_BRIGHT_EDGE;
        assertTrue(AdditivePolicy.replaceWithSoftCircle(k, true, "textures/particle/some_ring.png", false));
        assertFalse(AdditivePolicy.replaceWithSoftCircle(k, false, "textures/particle/some_ring.png", false), "an alpha-blended batch is left alone");
        assertFalse(AdditivePolicy.replaceWithSoftCircle(k, true, "textures/particle/some_pattern.png", false), "a texture that is not named like a sprite is left alone");
        assertTrue(AdditivePolicy.replaceWithSoftCircle(k, true, "textures/particle/some_pattern.png", true), "unless the player forced the safe path");
    }

    @Test
    void aTextureWithAlphaIsNeverSwapped() {
        for (TextureAlpha.Kind k : new TextureAlpha.Kind[]{TextureAlpha.Kind.COLOUR_UNDER_ALPHA, TextureAlpha.Kind.BLACK_UNDER_ALPHA, TextureAlpha.Kind.OPAQUE, TextureAlpha.Kind.EMPTY})
            assertFalse(AdditivePolicy.replaceWithSoftCircle(k, true, "textures/particle/some_ring.png", true), k.name());
    }

    @Test
    void timeMagicTexturesAreNeverTouched() {
        for (String t : new String[]{"time_sphere", "time_numeral_band", "time_clock_face", "time_clock_numerals", "time_clock_hand", "time_sparkle", "time_streak"}) {
            String path = "textures/particle/" + t + ".png";
            assertTrue(AdditivePolicy.isTimeMagic(path), path);
            assertFalse(AdditivePolicy.replaceWithSoftCircle(TextureAlpha.Kind.OPAQUE_BRIGHT_EDGE, true, path, true), path);
            assertFalse(AdditivePolicy.replaceWithSoftCircle(null, true, path, true), path + " (even a missing one: the Time Magic output is the standard and stays as it is)");
        }
        assertFalse(AdditivePolicy.isTimeMagic("textures/particle/glow.png"));
        assertFalse(AdditivePolicy.isTimeMagic("textures/particle/runtime_ring.png"), "only a file whose name starts with time_ counts");
    }
}
