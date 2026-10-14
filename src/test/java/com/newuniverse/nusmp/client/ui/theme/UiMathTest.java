package com.newuniverse.nusmp.client.ui.theme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The pure maths of the UI theme: easing, angles, the typewriter, colour mixing, lang slugs. */
class UiMathTest {
    @Test
    void easingsStartAtZeroAndEndAtOne() {
        assertEquals(0f, UiMath.smooth(0f), 1e-6f);
        assertEquals(1f, UiMath.smooth(1f), 1e-6f);
        assertEquals(0.5f, UiMath.smooth(0.5f), 1e-6f);
        assertEquals(0f, UiMath.easeOutCubic(-3f), 1e-6f, "clamped below");
        assertEquals(1f, UiMath.easeOutCubic(9f), 1e-6f, "clamped above");
        assertEquals(1f, UiMath.easeOutBack(1f), 1e-5f);
        float max = 0f;
        for (int i = 0; i <= 100; i++) max = Math.max(max, UiMath.easeOutBack(i / 100f));
        assertTrue(max > 1.02f && max < 1.2f, "easeOutBack overshoots a little: " + max);
        float prev = -1f;
        for (int i = 0; i <= 100; i++) {
            float v = UiMath.easeOutCubic(i / 100f);
            assertTrue(v >= prev, "easeOutCubic never goes back");
            prev = v;
        }
    }

    @Test
    void pulseStaysBetweenZeroAndOne() {
        for (float t = -50f; t < 400f; t += 3.7f) {
            float p = UiMath.pulse(t, 90f, 0.2f);
            assertTrue(p >= 0f && p <= 1f, "pulse " + p);
        }
        assertEquals(0.5f, UiMath.pulse(0f, 90f, 0f), 1e-6f);
        assertEquals(1f, UiMath.pulse(22.5f, 90f, 0f), 1e-5f, "a quarter period later it peaks");
    }

    @Test
    void anglesAreKeptInOneTurnForBothDirections() {
        for (float t = 0f; t < 100000f; t += 997.3f) {
            float a = UiMath.angle(t, 0.3f, 12f), b = UiMath.angle(t, -1.3f, 12f);
            assertTrue(a >= 0f && a < 360f, "cw " + a);
            assertTrue(b >= 0f && b < 360f, "ccw " + b);
        }
        assertEquals(30f, UiMath.angle(0f, 1f, 30f), 1e-6f);
        assertEquals(350f, UiMath.angle(10f, -1f, 0f), 1e-4f, "turning the other way wraps below zero");
        assertEquals(90f, UiMath.angle(100f, 1f, 350f), 1e-3f);
    }

    @Test
    void anAnimationRunsTheSameAtAnyFrameRate() {
        // approach() applied in small steps equals one big step of the same total ticks
        float a = 0f;
        for (int i = 0; i < 10; i++) a = UiMath.approach(a, 1f, 0.12f, 0.5f, 0f);
        float b = UiMath.approach(0f, 1f, 0.12f, 5f, 0f);
        assertEquals(b, a, 1e-4f);
        assertEquals(0f, UiMath.approachFactor(0.5f, 0f), 0f, "no time, no move");
        assertEquals(1f, UiMath.approach(0.9995f, 1f, 0.1f, 0.01f, 0.001f), 0f, "snaps when close");
    }

    @Test
    void typewriterNeverPassesTheLength() {
        assertEquals(0, UiMath.typed(-1f, 2f, 10));
        assertEquals(0, UiMath.typed(0f, 2f, 10));
        assertEquals(3, UiMath.typed(1.5f, 2f, 10));
        assertEquals(10, UiMath.typed(1000f, 2f, 10));
        assertEquals(0, UiMath.typed(5f, 2f, 0));
        assertEquals(10, UiMath.typed(Float.MAX_VALUE, 2f, 10), "no overflow");
    }

    @Test
    void colourHelpersKeepTheirChannels() {
        assertEquals(0x80123456, UiMath.withAlpha(0xFF123456, 128 / 255f));
        assertEquals(0x00123456, UiMath.withAlpha(0x123456, 0f));
        assertEquals(0xFF123456, UiMath.withAlpha(0x123456, 5f), "clamped");
        assertEquals(0x80112233, UiMath.scaleAlpha(0xFF112233, 128 / 255f));
        assertEquals(0xFF000000, UiMath.mix(0xFF000000, 0xFFFFFFFF, 0f));
        assertEquals(0xFFFFFFFF, UiMath.mix(0xFF000000, 0xFFFFFFFF, 1f));
        assertEquals(0xFF808080, UiMath.mix(0xFF000000, 0xFFFFFFFF, 0.5f));
        assertEquals(0x00FF0000, UiMath.mix(0x00FF0000, 0x00FF0000, 0.3f));
    }

    @Test
    void slugsAreLangKeySafe() {
        assertEquals("spirit_lords", UiMath.slug("Spirit Lords"));
        assertEquals("you", UiMath.slug("You"));
        assertEquals("a_b_c", UiMath.slug("  A -- b / C  "));
        assertEquals("", UiMath.slug("***"));
        assertEquals("", UiMath.slug(null));
        assertEquals("page2", UiMath.slug("Page2"));
    }

    @Test
    void themeModeParsesAnyCaseAndDefaultsToAnime() {
        assertEquals(UiThemeMode.CLASSIC, UiThemeMode.parse("classic"));
        assertEquals(UiThemeMode.CLASSIC, UiThemeMode.parse(" Classic "));
        assertEquals(UiThemeMode.ANIME, UiThemeMode.parse("anime"));
        assertEquals(UiThemeMode.ANIME, UiThemeMode.parse("nonsense"));
        assertEquals(UiThemeMode.ANIME, UiThemeMode.parse(null));
    }
}
