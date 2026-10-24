package com.newuniverse.nusmp.client.ui.gs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the pure timelines and tables behind the Great Spirit effects (compile animation, clover tiers, ripple, chat allow-list) are pinned so the drawing layers stay thin. */
class GsEffectsTest {
    // ------------------------------------------------------------ compile
    @Test
    void theCompileSweepGoesFromInkToDataAndNeverBackwards() {
        float h = 100f;
        assertEquals(0f, GsCompile.lineMix(50f, 0f, h), 1e-6, "at the start nothing is compiled");
        assertEquals(1f, GsCompile.lineMix(50f, 1f, h), 1e-6, "at the end everything is");
        float last = -1f;
        for (float p = 0f; p <= 1f; p += 0.02f) { float m = GsCompile.lineMix(50f, p, h); assertTrue(m >= last - 1e-6); last = m; }
        assertTrue(GsCompile.lineMix(10f, 0.5f, h) >= GsCompile.lineMix(90f, 0.5f, h), "the band reaches the top lines first");
    }

    @Test
    void layersAndScrambleOnlyHappenInsideTheBand() {
        assertEquals(0, GsCompile.layers(0f));
        assertEquals(1, GsCompile.layers(1f));
        assertEquals(2, GsCompile.layers(0.4f));
        assertEquals(0f, GsCompile.scrambleFrac(0f));
        assertEquals(0f, GsCompile.scrambleFrac(1f));
        assertEquals(1f, GsCompile.scrambleFrac(0.5f), 1e-6);
        assertFalse(GsCompile.scrambled(3, 5, 0f));
        assertEquals(GsCompile.scrambled(7, 3, 0.5f), GsCompile.scrambled(7, 3, 0.5f), "deterministic");
        for (int i = 0; i < 50; i++) assertTrue("0123456789ABCDEF<>/#+=".indexOf(GsCompile.scrambleGlyph(i, i * 3)) >= 0);
        assertTrue(GsCompile.done(GsCompile.DURATION));
        assertFalse(GsCompile.done(GsCompile.DURATION - 1f));
        assertEquals(0f, GsCompile.progress(-5f));
        assertEquals(1f, GsCompile.progress(1e9f));
    }

    @Test
    void theGlyphBudgetHoldsForAnyPageSize() {
        for (int lines = 1; lines <= 40; lines++) {
            for (int perLine = 1; perLine <= 200; perLine += 7) {
                int cap = GsCompile.glyphCap(lines, perLine, 9, GsCompile.PAGE_GLYPHS);
                assertTrue(cap >= 0 && cap <= perLine);
                assertTrue(cap == 0 || GsCompile.worstGlyphs(lines, cap, 9) <= GsCompile.PAGE_GLYPHS, lines + "x" + perLine + " cap " + cap);
            }
        }
        assertEquals(24, GsCompile.glyphCap(8, 24, 9, GsCompile.PAGE_GLYPHS), "a normal page is not cut");
        assertEquals(900, GsCompile.GLYPH_BUDGET);
        assertTrue(3 * GsCompile.PAGE_GLYPHS <= GsCompile.GLYPH_BUDGET, "three pages can show during a turn");
        assertEquals("abcdefg...", GsCompile.fit("abcdefghijklmnop", 10));
        assertEquals("abc", GsCompile.fit("abc", 10));
    }

    // ------------------------------------------------------------ clover
    @Test
    void cloverLeavesFollowTheCoverTierAndTheSkillType() {
        assertEquals(3, GsClover.leavesForTier(3));
        assertEquals(3, GsClover.leavesForTier(0));
        assertEquals(4, GsClover.leavesForTier(4));
        assertEquals(5, GsClover.leavesForTier(5));
        assertEquals(5, GsClover.leavesForTier(9));
        assertEquals(5, GsClover.leavesForSkillType("ULTIMATE"));
        assertEquals(4, GsClover.leavesForSkillType("unique"));
        for (String t : new String[]{"EXTRA", "COMMON", "RESISTANCE", "nonsense", "", null}) assertEquals(3, GsClover.leavesForSkillType(t), String.valueOf(t));
    }

    @Test
    void theFiveLeafFrameAlwaysWearsTheSlimeCrest() {
        assertEquals(GsSkin.C, GsClover.skinFor(5, GsSkin.B));
        assertEquals(GsSkin.C, GsClover.skinFor(5, GsSkin.C));
        assertEquals(GsSkin.B, GsClover.skinFor(3, GsSkin.B));
        assertEquals(GsSkin.B, GsClover.skinFor(4, GsSkin.B));
        assertEquals(GsSkin.C, GsClover.skinFor(4, GsSkin.C));
        for (float t = 0; t < 200; t += 3.3f) { float p = GsClover.pulse(t, 2); assertTrue(p >= 0f && p <= 1f); }
        assertEquals(25, GsClover.MAX_ANIMATED);
    }

    // ------------------------------------------------------------ ripple
    @Test
    void theRippleHasAtMostThreeRingsThatGrowAndFade() {
        assertEquals(3, GsRipple.MAX_RINGS);
        assertTrue(GsRipple.age(10f, 10f, 1) < 0f, "the second ring has not started at the entry tick");
        assertEquals(GsRipple.STEP, GsRipple.age(10f + 2 * GsRipple.STEP, 10f, 1), 1e-6);
        assertTrue(GsRipple.radius(10f) > GsRipple.radius(2f));
        assertEquals(1f, GsRipple.alpha(0f), 1e-6);
        assertEquals(0f, GsRipple.alpha(GsRipple.LIFE), 1e-6);
        assertEquals(0f, GsRipple.alpha(-1f), 1e-6);
        assertTrue(GsRipple.anyAlive(5f, 0f));
        assertFalse(GsRipple.anyAlive(1000f, 0f));
        assertFalse(GsRipple.anyAlive(-5f, 0f));
    }

    // ------------------------------------------------------------ chat allow-list
    @Test
    void onlyAllowListedKeysAreMirroredFromChat() {
        assertTrue(GsNoticeAllow.allowed("nusmp.zone.enter"));
        assertTrue(GsNoticeAllow.allowed("nusmp.spiritdive.tier.gold"));
        assertTrue(GsNoticeAllow.allowed("nusmp.ui.notice.page_open"));
        for (String k : new String[]{"chat.type.text", "commands.give.success", "nusmp.zone.enter.extra", "nusmp.mana_zone.cooldown", "", null, "x".repeat(300)})
            assertFalse(GsNoticeAllow.allowed(k), String.valueOf(k).length() > 40 ? "long" : String.valueOf(k));
        assertEquals(GsNoticeQueue.Kind.WARN, GsNoticeAllow.kind("nusmp.mana_zone.refused_head"));
        assertEquals(GsNoticeQueue.Kind.OK, GsNoticeAllow.kind("nusmp.mana_zone.attuned"));
        assertEquals(GsNoticeQueue.Kind.INFO, GsNoticeAllow.kind(null));
    }
}
