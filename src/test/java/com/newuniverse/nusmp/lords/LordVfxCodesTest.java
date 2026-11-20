package com.newuniverse.nusmp.lords;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the effect of a Lord weapon travels in one float (the power of the shared effect packet): sin, style and radius must survive the round trip for every combination, and the radius must stay bounded.
 */
class LordVfxCodesTest {
    @Test
    void sinStyleAndRadiusSurviveTheRoundTrip() {
        for (LordRules.Sin s : LordRules.Sin.values()) {
            for (int style = 0; style < 8; style++) {
                for (float r = 0; r <= LordVfxCodes.MAX_RADIUS; r += 3.25f) {
                    float p = LordVfxCodes.power(s, style, r);
                    assertEquals(s.ordinal(), LordVfxCodes.sinOf(p), s.id + " " + style + " " + r);
                    assertEquals(style, LordVfxCodes.styleOf(p), s.id + " " + style + " " + r);
                    assertEquals(r, LordVfxCodes.radiusOf(p), 0.06f, s.id + " " + style + " " + r);
                }
            }
        }
    }

    @Test
    void theRadiusIsClampedAndEveryEffectIsShort() {
        float huge = LordVfxCodes.power(LordRules.Sin.SLOTH, LordVfxCodes.DECREE, 1e9f);
        assertTrue(LordVfxCodes.radiusOf(huge) <= LordVfxCodes.MAX_RADIUS + 0.1f);
        for (int style = 0; style < LordVfxCodes.STYLES; style++) assertTrue(LordVfxCodes.duration(style) > 0 && LordVfxCodes.duration(style) <= 120, "style " + style);
    }
}
