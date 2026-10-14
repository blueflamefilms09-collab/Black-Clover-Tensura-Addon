package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pull fraction to mesh stage, with vanilla's thresholds and timing (docs/asset_pipeline.md "Bows and crossbows"). */
class RangedStageTest {
    private static final RangedStage.Kind BOW = RangedStage.Kind.BOW, XBOW = RangedStage.Kind.CROSSBOW;

    @Test
    void aBowNotBeingDrawnIsAtRest() {
        assertEquals(RangedStage.REST, RangedStage.stage(BOW, false, false, 0f));
        assertEquals(RangedStage.REST, RangedStage.stage(BOW, false, false, 0.95f), "a stale pull value cannot show a draw");
        assertEquals(RangedStage.REST, RangedStage.stage(BOW, false, true, 1f), "the charged flag means nothing to a bow");
    }

    @Test
    void bowStagesSwitchAtVanillasThresholds() {
        assertEquals(RangedStage.PULL1, RangedStage.stage(BOW, true, false, 0f), "drawing at all is stage 1");
        assertEquals(RangedStage.PULL1, RangedStage.stage(BOW, true, false, 0.649f));
        assertEquals(RangedStage.PULL2, RangedStage.stage(BOW, true, false, 0.65f));
        assertEquals(RangedStage.PULL2, RangedStage.stage(BOW, true, false, 0.899f));
        assertEquals(RangedStage.PULL3, RangedStage.stage(BOW, true, false, 0.9f));
        assertEquals(RangedStage.PULL3, RangedStage.stage(BOW, true, false, 1f));
    }

    @Test
    void bowPullIsTicksOverTwentyAndClamped() {
        assertEquals(0f, RangedStage.bowPull(72000, 72000), 1e-6);
        assertEquals(0.5f, RangedStage.bowPull(72000, 71990), 1e-6, "10 ticks of 20");
        assertEquals(1f, RangedStage.bowPull(72000, 71980), 1e-6);
        assertEquals(1f, RangedStage.bowPull(72000, 100), 1e-6, "held for ages: still 1");
        assertEquals(0f, RangedStage.bowPull(72000, 72005), 1e-6, "never negative");
        // 13 ticks = 0.65: the second model; 18 ticks = 0.9: the third
        assertEquals(RangedStage.PULL1, RangedStage.stage(BOW, true, false, RangedStage.bowPull(72000, 72000 - 12)));
        assertEquals(RangedStage.PULL2, RangedStage.stage(BOW, true, false, RangedStage.bowPull(72000, 72000 - 13)));
        assertEquals(RangedStage.PULL2, RangedStage.stage(BOW, true, false, RangedStage.bowPull(72000, 72000 - 17)));
        assertEquals(RangedStage.PULL3, RangedStage.stage(BOW, true, false, RangedStage.bowPull(72000, 72000 - 18)));
    }

    @Test
    void crossbowStagesFollowTheChargeDuration() {
        // Quick Charge shortens the duration: 25 ticks plain, 15 with Quick Charge II
        assertEquals(RangedStage.PULL1, RangedStage.stage(XBOW, true, false, RangedStage.crossbowPull(false, 28, 28, 25)));
        assertEquals(RangedStage.PULL1, RangedStage.stage(XBOW, true, false, RangedStage.crossbowPull(false, 28, 28 - 14, 25)));
        assertEquals(RangedStage.PULL2, RangedStage.stage(XBOW, true, false, RangedStage.crossbowPull(false, 28, 28 - 15, 25)), "15/25 = 0.6 >= 0.58");
        assertEquals(RangedStage.PULL2, RangedStage.stage(XBOW, true, false, RangedStage.crossbowPull(false, 28, 28 - 24, 25)));
        assertEquals(RangedStage.PULL3, RangedStage.stage(XBOW, true, false, RangedStage.crossbowPull(false, 28, 28 - 25, 25)), "fully charged: 1.0");
        assertEquals(RangedStage.PULL2, RangedStage.stage(XBOW, true, false, RangedStage.crossbowPull(false, 18, 18 - 9, 15)), "Quick Charge II: 9/15 = 0.6");
        assertEquals(0f, RangedStage.crossbowPull(true, 28, 10, 25), "a loaded crossbow has no pull");
        assertEquals(0f, RangedStage.crossbowPull(false, 28, 10, 0), "a zero duration cannot divide");
    }

    @Test
    void aLoadedCrossbowIsChargedWhoeverHoldsItAndBeatsTheDraw() {
        assertEquals(RangedStage.CHARGED, RangedStage.stage(XBOW, false, true, 0f), "in a frame, on the ground");
        assertEquals(RangedStage.CHARGED, RangedStage.stage(XBOW, true, true, 0f), "held and loaded");
        assertEquals(RangedStage.REST, RangedStage.stage(XBOW, false, false, 0f));
    }

    @Test
    void theMeshChainFallsBackStageByStageToTheRestMesh() {
        assertEquals(List.of("x"), RangedStage.chain("x", RangedStage.REST));
        assertEquals(List.of("x_pull1", "x"), RangedStage.chain("x", RangedStage.PULL1));
        assertEquals(List.of("x_pull2", "x_pull1", "x"), RangedStage.chain("x", RangedStage.PULL2));
        assertEquals(List.of("x_pull3", "x_pull2", "x_pull1", "x"), RangedStage.chain("x", RangedStage.PULL3));
        assertEquals(List.of("x_charged", "x"), RangedStage.chain("x", RangedStage.CHARGED));
        assertEquals(List.of("x_spent", "x"), RangedStage.chain("x", RangedStage.SPENT));
    }

    @Test
    void suffixesAreTheDocumentedNames() {
        assertEquals("", RangedStage.suffix(RangedStage.REST));
        assertEquals("_pull1", RangedStage.suffix(RangedStage.PULL1));
        assertEquals("_pull2", RangedStage.suffix(RangedStage.PULL2));
        assertEquals("_pull3", RangedStage.suffix(RangedStage.PULL3));
        assertEquals("_charged", RangedStage.suffix(RangedStage.CHARGED));
        assertEquals("_spent", RangedStage.suffix(RangedStage.SPENT));
    }
}
