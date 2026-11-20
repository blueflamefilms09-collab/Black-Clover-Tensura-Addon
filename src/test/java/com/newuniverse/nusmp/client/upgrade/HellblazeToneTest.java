package com.newuniverse.nusmp.client.upgrade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the purple that Hellblaze's glow takes on a hit (docs/hellblaze_katana.md) is plain arithmetic: full on the hit, gone after about half a second, never outside 0..1, falling all the way; the colour blend is exact at both ends. */
class HellblazeToneTest {
    @Test
    void fullOnTheHitAndGoneAfterTheWindow() {
        assertEquals(1f, HellblazeTone.tone(0f, 10), 1e-6f);
        assertEquals(0f, HellblazeTone.tone(10f, 10), 1e-6f);
        assertEquals(0f, HellblazeTone.tone(25f, 10), 1e-6f);
        assertEquals(0f, HellblazeTone.tone(-1f, 10), 1e-6f, "a hit that has not happened yet");
        assertEquals(0f, HellblazeTone.tone(3f, 0), 1e-6f, "no window, no tone");
    }

    @Test
    void fallsMonotonicallyAndStaysInRange() {
        float last = 1f;
        for (float age = 0f; age <= 10f; age += 0.25f) {
            float t = HellblazeTone.tone(age, 10);
            assertTrue(t >= 0f && t <= 1f, "range at " + age);
            assertTrue(t <= last + 1e-6f, "never rises again at " + age);
            last = t;
        }
        assertEquals(0.5f, HellblazeTone.tone(5f, 10), 1e-5f, "the smoothstep is half way at half the time");
    }

    @Test
    void theBlendIsExactAtTheEndsAndBetweenTheChannels() {
        int red = 0xA8142A, purple = 0x9632C8;
        assertEquals(red, HellblazeTone.mix(red, purple, 0f));
        assertEquals(purple, HellblazeTone.mix(red, purple, 1f));
        assertEquals(red, HellblazeTone.mix(red, purple, -3f));
        assertEquals(purple, HellblazeTone.mix(red, purple, 7f));
        int mid = HellblazeTone.mix(0x000000, 0xFEFEFE, 0.5f);
        assertEquals(0x7F, mid >> 16 & 255);
        assertEquals(0x7F, mid >> 8 & 255);
        assertEquals(0x7F, mid & 255);
        assertEquals(0, HellblazeTone.mix(0xFF000000 | red, purple, 0f) >>> 24, "only the rgb survives");
    }
}
