package com.newuniverse.nusmp.client.skilltree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the banner is drawn by code nobody can run in the build, so its timing is pinned as arithmetic: nothing shows before the burst or after the end, the words are
 * fully opaque for a while in the middle and fade over the last second, the slam starts big and settles to exactly normal size, the flash is at full strength at the
 * burst and gone long before the words, the line under the words draws itself from nothing to everything, the banner is over exactly when its life is, and every
 * value stays inside 0..1 (a value outside would show as a wrong colour or a negative size). The colour helpers are pinned on a known colour.
 */
class BannerTimelineTest {
    @Test
    void nothingShowsBeforeTheBurstOrAfterTheEnd() {
        assertEquals(0f, BannerTimeline.alpha(-5f), 0f);
        assertEquals(0f, BannerTimeline.alpha(0f), 0f);
        assertEquals(0f, BannerTimeline.alpha(BannerTimeline.LIFE), 0f);
        assertEquals(0f, BannerTimeline.alpha(BannerTimeline.LIFE + 40f), 0f);
        assertEquals(0f, BannerTimeline.flash(-1f), 0f);
        assertEquals(0f, BannerTimeline.flash(BannerTimeline.FLASH), 0f);
    }

    @Test
    void theLifeIsThreeSecondsAndTheBannerIsOverExactlyThen() {
        assertEquals(60f, BannerTimeline.LIFE, 0f);
        assertFalse(BannerTimeline.done(59.9f));
        assertTrue(BannerTimeline.done(60f));
        assertTrue(BannerTimeline.done(61f));
    }

    @Test
    void theWordsFadeInHoldAndFadeOut() {
        assertTrue(BannerTimeline.alpha(2f) > 0f && BannerTimeline.alpha(2f) < 1f);
        assertEquals(1f, BannerTimeline.alpha(BannerTimeline.FADE_IN), 1e-6f);
        assertEquals(1f, BannerTimeline.alpha(20f), 1e-6f);
        assertEquals(1f, BannerTimeline.alpha(BannerTimeline.LIFE - BannerTimeline.FADE_OUT), 1e-6f);
        float prev = 1f;
        for (float a = BannerTimeline.LIFE - BannerTimeline.FADE_OUT; a < BannerTimeline.LIFE; a += 1f) {
            float v = BannerTimeline.alpha(a);
            assertTrue(v <= prev + 1e-6f, "the fade out never rises: age " + a);
            prev = v;
        }
        assertTrue(BannerTimeline.alpha(BannerTimeline.LIFE - 1f) < 0.1f, "almost gone one tick before the end");
    }

    @Test
    void theSlamStartsBigAndSettlesToExactlyNormal() {
        assertEquals(BannerTimeline.POP_FROM, BannerTimeline.scale(0f), 1e-5f);
        assertEquals(1f, BannerTimeline.scale(BannerTimeline.POP), 1e-5f);
        assertEquals(1f, BannerTimeline.scale(BannerTimeline.POP + 30f), 1e-5f);
        assertTrue(BannerTimeline.scale(BannerTimeline.POP / 2) < BannerTimeline.POP_FROM);
        for (float a = 0f; a <= 20f; a += 0.5f) assertTrue(BannerTimeline.scale(a) > 0.5f && BannerTimeline.scale(a) <= BannerTimeline.POP_FROM + 1e-4f, "age " + a);
    }

    @Test
    void theFlashIsFullAtTheBurstAndGoneBeforeTheWords() {
        assertEquals(1f, BannerTimeline.flash(0f), 1e-6f);
        float prev = 1f;
        for (float a = 0f; a < BannerTimeline.FLASH; a += 0.5f) {
            float v = BannerTimeline.flash(a);
            assertTrue(v <= prev + 1e-6f && v >= 0f, "age " + a);
            prev = v;
        }
        assertTrue(BannerTimeline.FLASH < BannerTimeline.LIFE - BannerTimeline.FADE_OUT, "the flash is over before the words start to fade");
    }

    @Test
    void theLineDrawsItselfFromNothingToEverything() {
        assertEquals(0f, BannerTimeline.rule(0f), 0f);
        assertEquals(0f, BannerTimeline.rule(-3f), 0f);
        assertEquals(1f, BannerTimeline.rule(BannerTimeline.RULE), 1e-6f);
        assertEquals(1f, BannerTimeline.rule(BannerTimeline.RULE * 5), 1e-6f);
        assertTrue(BannerTimeline.rule(BannerTimeline.RULE / 2) > 0.5f, "fast at first, slower at the end");
    }

    @Test
    void everyValueStaysInRange() {
        for (float a = -10f; a < 100f; a += 0.25f) {
            assertTrue(BannerTimeline.alpha(a) >= 0f && BannerTimeline.alpha(a) <= 1f, "alpha " + a);
            assertTrue(BannerTimeline.flash(a) >= 0f && BannerTimeline.flash(a) <= 1f, "flash " + a);
            assertTrue(BannerTimeline.rule(a) >= 0f && BannerTimeline.rule(a) <= 1f, "rule " + a);
        }
    }

    @Test
    void theColourHelpers() {
        assertEquals(0xFFE8541C, BannerTimeline.argb(0xE8541C, 1f));
        assertEquals(0x00E8541C, BannerTimeline.argb(0xE8541C, 0f));
        assertEquals(0x80E8541C, BannerTimeline.argb(0xE8541C, 0.5f));
        assertEquals(0xFF0000FF, BannerTimeline.argb(0xFF000000 | 0x0000FF, 2f), "an opacity above 1 is clamped and stray high bits of the colour are dropped");
        assertEquals(0x000000, BannerTimeline.lighten(0x000000, 0f));
        assertEquals(0xFFFFFF, BannerTimeline.lighten(0x000000, 1f));
        assertEquals(0x808080, BannerTimeline.lighten(0x000000, 0.5f));
        assertEquals(0xE8541C, BannerTimeline.lighten(0xE8541C, 0f));
    }

    @Test
    void aRouteTheClientDoesNotKnowStillGetsAReadableName() {
        assertEquals("Ember Knight", BannerTimeline.fallbackName("nusmp:ember_knight"));
        assertEquals("Frost Knight", BannerTimeline.fallbackName("frost_knight"));
        assertEquals("Sky Dragon", BannerTimeline.fallbackName("pack:sky/dragon"));
        assertEquals("", BannerTimeline.fallbackName(""));
        assertEquals("", BannerTimeline.fallbackName(null));
        assertEquals("A", BannerTimeline.fallbackName("x:__a__"));
    }
}
