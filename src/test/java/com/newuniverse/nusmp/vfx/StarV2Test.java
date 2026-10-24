package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Star V2 look cannot be seen here, so its pure rules are pinned: the seven shapes it owns are the appended ones (and never STAR_MAGIC, which about 24 other files use), every effect is under its quad cap
 * and the vertex budget is the cap times four and at most the engine's 400, the placed star has a level of detail and a per-frame ceiling, the options default to V2 / flashes on, and the reduced-flashes rules
 * hold (no hard flash, a soft fade that never rises, no twinkle, no spin, at most three frame changes a second).
 */
class StarV2Test {
    @Test
    void theOwnedShapesAreTheSevenAppendedOnesAndNeverTheOldStarMagic() {
        assertEquals(List.of(VfxShape.STAR_NODE, VfxShape.STAR_LINK, VfxShape.STAR_CONJ, VfxShape.STAR_PANE, VfxShape.STAR_LANCE, VfxShape.STAR_WHIP, VfxShape.STAR_DOME), StarV2.SHAPES);
        assertFalse(StarV2.owns(VfxShape.STAR_MAGIC));
        VfxShape[] all = VfxShape.values();
        assertEquals(VfxShape.STAR_DOME, all[all.length - 1], "appended last");
        assertEquals(VfxShape.STAR_NODE.ordinal(), VfxShape.RUNE_WRITING.ordinal() + 1);
        assertTrue(VfxShape.STAR_MAGIC.ordinal() < VfxShape.STORM_CANCEL.ordinal(), "the old shape did not move");
        assertTrue(StarV2.isCel(VfxShape.STAR_NODE) && StarV2.isCel(VfxShape.STAR_LINK) && StarV2.isCel(VfxShape.STAR_PANE) && StarV2.isCel(VfxShape.STAR_WHIP), "A is the base of the star, lines, pane and whip");
        assertFalse(StarV2.isCel(VfxShape.STAR_CONJ), "B is Conjunction");
        assertFalse(StarV2.isCel(VfxShape.STAR_DOME));
    }

    @Test
    void everyEffectIsUnderItsCapAndItsVertexBudgetIsUnderTheEngineLimit() {
        for (VfxShape s : StarV2.SHAPES) {
            assertTrue(StarV2.worstQuads(s) > 0 && StarV2.worstQuads(s) <= StarV2.capQuads(s), s + " " + StarV2.worstQuads(s) + " vs " + StarV2.capQuads(s));
            assertEquals(StarV2.capQuads(s) * 4, StarV2.vertexBudget(s));
            assertTrue(StarV2.vertexBudget(s) <= 400, s + " budget");
        }
        assertEquals(0, StarV2.worstQuads(VfxShape.STAR_MAGIC), "the old shape is not drawn by this look");
        assertEquals(0, StarV2.capQuads(VfxShape.FIRE_LION));
    }

    @Test
    void aPlacedStarHasALevelOfDetailAndACrowdedViewStaysBounded() {
        assertEquals(StarV2.NODE_QUADS_NEAR, StarV2.nodeQuads(5, 0));
        assertEquals(StarV2.NODE_QUADS_FAR, StarV2.nodeQuads(30, 0), "far: the bloom sprite");
        assertEquals(StarV2.NODE_QUADS_FAR, StarV2.nodeQuads(5, StarV2.NODE_NEAR_PER_FRAME), "past the per-frame ceiling of full stars");
        assertEquals(0, StarV2.nodeQuads(StarV2.NODE_MAX_BLOCKS + 1, 0), "beyond the draw distance nothing");
        assertTrue(StarV2.NODE_QUADS_FAR < StarV2.NODE_QUADS_NEAR);
        // 20 Star mages in view with 6 stars each, plus the eight panes the server can hold
        int worst = StarV2.worstFrameQuads(120, 8);
        assertTrue(worst <= 600, "worst crowded frame: " + worst + " quads");
        assertEquals(48 * StarV2.NODE_QUADS_NEAR + 72 * StarV2.NODE_QUADS_FAR + 8 * StarV2.PANE_QUADS, worst);
        assertTrue(StarV2.worstFrameQuads(6, 0) == 6 * StarV2.NODE_QUADS_NEAR, "a few stars are all drawn in full");
    }

    @Test
    void theOptionsDefaultToV2AndFlashesOnAndParseV1() {
        assertEquals(StarV2.Style.V2, StarV2.DEFAULT_STYLE);
        assertFalse(StarV2.DEFAULT_REDUCED_FLASHES);
        assertEquals(StarV2.Style.V2, StarV2.parse(null));
        assertEquals(StarV2.Style.V2, StarV2.parse("nonsense"));
        assertEquals(StarV2.Style.V1, StarV2.parse("v1"));
        assertEquals(StarV2.Style.V1, StarV2.parse(" V1 "));
        assertEquals("vfx.element.star", StarV2.CONFIG_PATH);
        assertEquals("vfx.element.starReducedFlashes", StarV2.CONFIG_PATH_REDUCED);
        assertEquals("nusmp-star-v2-client.toml", StarV2.CONFIG_FILE);
        StarV2.Style before = StarV2.style();
        boolean red = StarV2.reducedFlashes();
        try {
            StarV2.setStyle(StarV2.Style.V1);
            assertFalse(StarV2.v2Look());
            StarV2.setStyle(null);
            assertTrue(StarV2.v2Look(), "null is the default");
            StarV2.setReducedFlashes(true);
            assertTrue(StarV2.reducedFlashes());
        } finally {
            StarV2.setStyle(before);
            StarV2.setReducedFlashes(red);
        }
    }

    @Test
    void reducedFlashesDropTheHardFlashSoftenTheFadeAndStopTheTwinkle() {
        assertEquals(1f, StarV2.hardFlash(0, 5, false));
        assertEquals(0f, StarV2.hardFlash(5, 5, false));
        assertEquals(0f, StarV2.hardFlash(-1, 5, false));
        for (int age = 0; age < 40; age++) assertEquals(0f, StarV2.hardFlash(age, 5, true), "never a hard flash with reduced flashes");
        assertEquals(0f, StarV2.softFlash(0, 5, false), "the soft fade replaces the flash only with the option");
        assertEquals(0.5f, StarV2.softFlash(0, 5, true));
        float last = 1f;
        for (int age = 0; age < 20; age++) { float f = StarV2.softFlash(age, 5, true); assertTrue(f <= last + 1e-6f && f >= 0f, "the soft fade never rises"); last = f; }
        assertTrue(StarV2.framesPerSecond(true) <= 3f && StarV2.framesPerSecond(false) == 10f);
        assertEquals(StarV2.twinkle(1f, 0.5f, 0.4f, true), StarV2.twinkle(50f, 0.5f, 0.4f, true), "constant with reduced flashes");
        Set<Float> seen = new HashSet<>();
        for (int i = 0; i < 40; i++) seen.add(StarV2.twinkle(i, 0.5f, 0.4f, false));
        assertTrue(seen.size() > 10, "it twinkles normally");
        assertEquals(0f, StarV2.spin(3f, true));
        assertEquals(3f, StarV2.spin(3f, false));
        assertEquals(0, StarV2.frame(0, 0, 2, false));
        assertEquals(1, StarV2.frame(2, 0, 2, false));
        assertEquals(0, StarV2.frame(6, 0, 2, true), "held for 7 ticks");
    }

    @Test
    void theTexturesAreNamedSizedAndCheckedLikeTheOtherLooks() {
        assertEquals(24, StarV2.TEXTURE_NAMES.length);
        assertEquals(24, StarV2.allTextures().size());
        Set<String> names = new HashSet<>(List.of(StarV2.TEXTURE_NAMES));
        assertEquals(24, names.size(), "no name twice");
        assertEquals("stv_a_star4", StarV2.fileName("a_star4"));
        int[] ok = new int[16 * 16];
        for (int i = 0; i < ok.length; i++) { int x = i % 16, y = i / 16; ok[i] = (x > 4 && x < 12 && y > 4 && y < 12) ? 0xFFFFFFFF : 0; }
        assertEquals("b_glow is 16x16, expected 128x128", StarV2.checkTexture("b_glow", 16, 16, ok));
        int[] fine = new int[128 * 128];
        for (int i = 0; i < fine.length; i++) { int x = i % 128, y = i / 128; fine[i] = Math.hypot(x - 64, y - 64) < 40 ? 0xFFFFFFFF : 0; }
        assertEquals(null, StarV2.checkTexture("b_glow", 128, 128, fine));
        int[] full = new int[128 * 128];
        java.util.Arrays.fill(full, 0xFFFFFFFF);
        assertTrue(StarV2.checkTexture("b_glow", 128, 128, full).contains("fully opaque"));
        fine[0] = 0xFF112233;
        assertTrue(StarV2.checkTexture("b_glow", 128, 128, fine).contains("visible corner"), "an opaque corner would draw a square");
    }
}
