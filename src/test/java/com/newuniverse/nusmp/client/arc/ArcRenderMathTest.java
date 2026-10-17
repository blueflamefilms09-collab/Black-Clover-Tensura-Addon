package com.newuniverse.nusmp.client.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of the arc renderers: how big a boss is drawn, how far a stride goes, when the spawn clip plays. */
class ArcRenderMathTest {
    @Test
    void aBossWhoseHitboxIsItsModelsHeightIsDrawnAtOne() {
        assertEquals(1f, ArcRenderMath.scale(2.1f, 2.1f), 1e-6);
        assertEquals(1.5f, ArcRenderMath.scale(3.15f, 2.1f), 1e-6, "a hitbox grown by a phase grows the model");
    }

    @Test
    void theScaleIsClampedAndBadInputsDrawAtOne() {
        assertEquals(ArcRenderMath.MAX_SCALE, ArcRenderMath.scale(500f, 1f), 1e-6);
        assertEquals(ArcRenderMath.MIN_SCALE, ArcRenderMath.scale(0.001f, 2f), 1e-6);
        assertEquals(1f, ArcRenderMath.scale(0f, 2f), 1e-6);
        assertEquals(1f, ArcRenderMath.scale(2f, 0f), 1e-6);
        assertEquals(1f, ArcRenderMath.scale(Float.NaN, 2f), 1e-6);
        assertEquals(1f, ArcRenderMath.scale(2f, Float.NaN), 1e-6);
    }

    @Test
    void aStrideIsTwoStepsOfTwiceTheLegTimesTheSineOfTheSwing() {
        // legs 12.5 px from the hip, a 24 degree swing: 4 * 12.5 * sin 24 / 16 = 1.27 blocks of ground per cycle
        assertEquals(1.271f, ArcRenderMath.strideBlocks(12.5f, 24f), 0.005f);
        assertTrue(ArcRenderMath.strideBlocks(12.5f, 46f) > ArcRenderMath.strideBlocks(12.5f, 24f), "a run covers more ground per cycle");
        assertTrue(ArcRenderMath.strideBlocks(18.5f, 20f) > ArcRenderMath.strideBlocks(8f, 20f), "longer legs, longer stride");
    }

    @Test
    void aStrideNeedsLegsAndASwing() {
        assertEquals(0f, ArcRenderMath.strideBlocks(0f, 24f), 0f);
        assertEquals(0f, ArcRenderMath.strideBlocks(12.5f, 0f), 0f);
        assertEquals(0f, ArcRenderMath.strideBlocks(-3f, 24f), 0f);
        assertEquals(ArcRenderMath.strideBlocks(12.5f, 80f), ArcRenderMath.strideBlocks(12.5f, 170f), 1e-6, "an absurd swing is capped");
    }

    @Test
    void aMissingStrideFallsBackToTheDefault() {
        assertEquals(1.3f, ArcRenderMath.strideOr(0f, 1.3f), 0f);
        assertEquals(1.3f, ArcRenderMath.strideOr(0.01f, 1.3f), 0f);
        assertEquals(1.8f, ArcRenderMath.strideOr(1.8f, 1.3f), 0f);
    }

    @Test
    void theSpawnClipPlaysOnceWhenTheBodyAppears() {
        assertEquals(0f, ArcRenderMath.spawnSeconds(0f, 2f, false), 1e-6);
        assertEquals(1f, ArcRenderMath.spawnSeconds(20f, 2f, false), 1e-6);
        assertEquals(-1f, ArcRenderMath.spawnSeconds(40f, 2f, false), 1e-6, "it is over after its length");
        assertEquals(-1f, ArcRenderMath.spawnSeconds(10f, 0f, false), 1e-6, "a model without the clip skips it");
        assertEquals(-1f, ArcRenderMath.spawnSeconds(10f, 2f, true), 1e-6, "a dying body does not spawn");
        assertEquals(-1f, ArcRenderMath.spawnSeconds(-5f, 2f, false), 1e-6, "a clock that ran backwards does not play it");
    }
}
