package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the pure parts of the particle library, with no game classes: the size and alpha curves of a particle's life, which animation frame it shows, how a request is thinned by quality
 * and distance, how a themed burst is shared over the theme's particles, the Rukh-sized caps of the directed emitters, the profile parser (defaults, clamps, wrong types) and the tint data
 * that rides in the particle packet (clamping, quantising, the byte round trip, colour parsing). A wrong curve here is a particle that pops, never fades or fills the screen.
 */
class FxMathTest {
    @Test
    void sizeFollowsItsCurveAndStaysInsideTheLife() {
        for (FxProfile.Curve c : FxProfile.Curve.values()) {
            for (float t = 0f; t <= 1.0001f; t += 0.05f) {
                float s = FxMath.sizeAt(c, 0.4f, 0.1f, t);
                assertTrue(s >= 0f && s <= 0.6f, c + " at " + t + " = " + s);
            }
        }
        assertEquals(0.4f, FxMath.sizeAt(FxProfile.Curve.LINEAR, 0.4f, 0.1f, 0f), 1e-6f);
        assertEquals(0.1f, FxMath.sizeAt(FxProfile.Curve.LINEAR, 0.4f, 0.1f, 1f), 1e-6f);
        assertEquals(0.4f, FxMath.sizeAt(FxProfile.Curve.HOLD, 0.4f, 0.1f, 0.5f), 1e-6f, "hold keeps the start size for three quarters of the life");
        assertEquals(0.1f, FxMath.sizeAt(FxProfile.Curve.HOLD, 0.4f, 0.1f, 1f), 1e-6f);
        assertTrue(FxMath.sizeAt(FxProfile.Curve.POP, 0.4f, 0.1f, 0.0f) < FxMath.sizeAt(FxProfile.Curve.POP, 0.4f, 0.1f, 0.2f), "pop swells in the first fifth");
        assertTrue(FxMath.sizeAt(FxProfile.Curve.EASE_OUT, 0f, 1f, 0.5f) > 0.5f, "ease-out is ahead of linear at the middle");
        assertEquals(FxMath.sizeAt(FxProfile.Curve.LINEAR, 0.4f, 0.1f, 1f), FxMath.sizeAt(FxProfile.Curve.LINEAR, 0.4f, 0.1f, 7f), "a life fraction past 1 is clamped");
    }

    @Test
    void alphaRisesAndFalls() {
        assertEquals(0f, FxMath.alphaAt(0.2f, 0.4f, 0f), 1e-6f);
        assertEquals(1f, FxMath.alphaAt(0.2f, 0.4f, 0.4f), 1e-6f);
        assertEquals(0.5f, FxMath.alphaAt(0.2f, 0.4f, 0.1f), 1e-6f);
        assertEquals(0.5f, FxMath.alphaAt(0.2f, 0.4f, 0.8f), 1e-6f);
        assertEquals(0f, FxMath.alphaAt(0.2f, 0.4f, 1f), 1e-6f);
        assertEquals(1f, FxMath.alphaAt(0f, 0f, 0.5f), 1e-6f, "no fade at all keeps full alpha");
    }

    @Test
    void framesAdvanceAsTheModeSays() {
        assertEquals(0, FxMath.frameAt(FxProfile.Anim.AGE, 0, 20, 6, 1, 0));
        assertEquals(5, FxMath.frameAt(FxProfile.Anim.AGE, 19, 20, 6, 1, 0));
        assertEquals(5, FxMath.frameAt(FxProfile.Anim.AGE, 99, 20, 6, 1, 0), "an age past the life shows the last frame");
        assertEquals(2, FxMath.frameAt(FxProfile.Anim.AGE, 7, 18, 6, 1, 0));
        assertEquals(0, FxMath.frameAt(FxProfile.Anim.LOOP, 0, 20, 6, 3, 0));
        assertEquals(1, FxMath.frameAt(FxProfile.Anim.LOOP, 3, 20, 6, 3, 0));
        assertEquals(0, FxMath.frameAt(FxProfile.Anim.LOOP, 18, 40, 6, 3, 0), "the loop wraps");
        assertEquals(4, FxMath.frameAt(FxProfile.Anim.LOOP, 0, 20, 6, 3, 4), "the phase offsets the loop");
        assertEquals(3, FxMath.frameAt(FxProfile.Anim.RANDOM, 50, 20, 6, 3, 9), "random is fixed at birth: only the phase counts");
        assertEquals(0, FxMath.frameAt(FxProfile.Anim.LOOP, 5, 20, 1, 3, 0), "one frame is always frame 0");
        assertTrue(FxMath.frameAt(FxProfile.Anim.RANDOM, 0, 20, 6, 3, -7) >= 0, "a negative phase never gives a negative frame");
    }

    @Test
    void requestsAreThinnedByQualityDistanceAndTheCallCap() {
        assertEquals(0, FxMath.scaledCount(0, 1f, 1f, 400));
        assertEquals(0, FxMath.scaledCount(10, 0f, 1f, 400), "a zero detail drops everything");
        assertEquals(0, FxMath.scaledCount(10, 1f, 0f, 400), "past the cull distance nothing is spawned");
        assertEquals(10, FxMath.scaledCount(10, 1f, 1f, 400));
        assertEquals(5, FxMath.scaledCount(10, 0.5f, 1f, 400));
        assertEquals(3, FxMath.scaledCount(10, 0.5f, 0.5f, 400), "quality times distance (2.5 rounds up)");
        assertEquals(1, FxMath.scaledCount(1, 0.5f, 0.5f, 400), "a request is never thinned to nothing");
        assertEquals(48, FxMath.scaledCount(400, 1f, 1f, FxMath.perCallCap("LOW")));
        assertEquals(400, FxMath.perCallCap("HIGH"));
        assertEquals(140, FxMath.perCallCap("medium"));
        assertEquals(400, FxMath.perCallCap(null));
        assertEquals(0.9, FxMath.clampSpeed(5.0), 1e-9);
        assertEquals(-0.9, FxMath.clampSpeed(-5.0), 1e-9);
        assertEquals(0.3, FxMath.clampSpeed(0.3), 1e-9);
    }

    @Test
    void aThemedBurstIsSharedOverTheParticlesOfTheTheme() {
        for (int particles = 1; particles <= 4; particles++) {
            for (int count = 0; count < 60; count++) {
                int[] s = FxGeo.shares(count, particles);
                assertEquals(particles, s.length);
                int sum = 0;
                for (int x : s) {
                    assertTrue(x >= 0);
                    sum += x;
                }
                assertEquals(count, sum, "shares of " + count + " over " + particles);
            }
        }
        int[] s = FxGeo.shares(100, 4);
        assertTrue(s[0] > s[1] && s[1] > s[2] && s[2] > s[3], "the signature particle gets the biggest share");
        assertTrue(FxGeo.shares(5, 4)[3] >= 1, "every slot of a small burst still gets one while the count lasts");
    }

    @Test
    void theDirectedEmittersKeepTheOldRukhCapsAndGeometry() {
        assertEquals(12, FxGeo.clamp(400, FxGeo.MAX_BURST));
        assertEquals(0, FxGeo.clamp(-3, FxGeo.MAX_BURST));
        double[] v = FxGeo.toward(18, 0, 0, FxGeo.FLIGHT_TICKS);
        assertEquals(1.0 > FxGeo.MAX_SPEED ? FxGeo.MAX_SPEED : 1.0, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-9);
        double[] far = FxGeo.toward(1000, 0, 0, 18);
        assertEquals(FxGeo.MAX_SPEED, far[0], 1e-9, "a far target is limited to the maximum speed");
        double[] r = FxGeo.ring(0.5, 0.0);
        assertEquals((FxGeo.RING_MIN + FxGeo.RING_MAX) / 2, r[0], 1e-9);
        assertEquals(0.0, FxGeo.slotAngle(0, 4, 0.0), 1e-9);
        assertEquals(Math.PI, FxGeo.slotAngle(2, 4, 0.0), 1e-9);
    }

    @Test
    void theProfileParserKeepsDefaultsForMissingOrWrongKeys() {
        FxProfile d = FxProfile.DEFAULT;
        FxProfile p = FxProfile.fromJson(JsonParser.parseString("{\"gravity\":\"fast\",\"drag\":7,\"size\":[0.5],\"life\":[500,9],\"blend\":\"ADD\",\"light\":\"full\",\"anim\":\"nope\",\"curve\":\"pop\",\"collide\":true}").getAsJsonObject());
        assertEquals(d.gravity(), p.gravity(), "a word where a number belongs keeps the default");
        assertEquals(1.0f, p.drag(), 1e-6f, "drag is clamped to 1");
        assertEquals(d.sizeStart(), p.sizeStart(), "a short size list is ignored");
        assertEquals(FxProfile.MAX_LIFE, p.lifeMin(), "a lifetime over the maximum is clamped");
        assertTrue(p.lifeMax() >= p.lifeMin());
        assertEquals(FxProfile.Blend.ADD, p.blend());
        assertEquals(FxProfile.Light.FULL, p.light());
        assertEquals(d.anim(), p.anim(), "an unknown word keeps the default");
        assertEquals(FxProfile.Curve.POP, p.curve());
        assertTrue(p.collide());
        assertSame(FxProfile.DEFAULT, FxProfile.fromJson(null));
        FxProfile q = FxProfile.fromJson(JsonParser.parseString("{\"size\":[99,-3],\"loop\":0,\"fade\":[2,-1]}").getAsJsonObject());
        assertEquals(FxProfile.MAX_SIZE, q.sizeStart(), 1e-6f);
        assertEquals(0f, q.sizeEnd(), 1e-6f);
        assertEquals(1, q.loopTicks());
        assertEquals(1f, q.fadeIn(), 1e-6f);
        assertEquals(0f, q.fadeOut(), 1e-6f);
    }

    @Test
    void theTintSurvivesTheWire() {
        for (int rgb : new int[]{-1, 0, 0xFFFFFF, 0x33AAFF, 0x010203}) {
            for (float scale : new float[]{0.05f, 0.5f, 1f, 1.5f, 7.9f, 50f, Float.NaN}) {
                FxTint in = new FxTint(rgb, scale);
                ByteBuffer buf = ByteBuffer.allocate(16);
                in.write(buf);
                assertEquals(8, buf.position(), "the wire form is an int and a float");
                buf.flip();
                FxTint out = FxTint.read(buf);
                assertEquals(in, out, "round trip of " + rgb + " / " + scale);
                assertEquals(in, new FxTint(out.rgb(), out.scale()), "normalising twice changes nothing");
            }
        }
        assertEquals(FxTint.MAX_SCALE, new FxTint(0, 99f).scale(), 1e-6f);
        assertEquals(FxTint.MIN_SCALE, new FxTint(0, 0f).scale(), 0.01f);
        assertEquals(1.0f, new FxTint(0, Float.NaN).scale(), 1e-6f);
        assertEquals(-1, new FxTint(-5, 1f).rgb(), "any negative colour means no tint");
        assertEquals(0xABCDEF, new FxTint(0x1ABCDEF, 1f).rgb(), "the alpha byte is dropped");
        assertFalse(FxTint.NONE.tinted());
        assertTrue(new FxTint(0, 1f).tinted(), "black is a colour, not 'no tint'");
        assertEquals(1f, new FxTint(0xFF0000, 1f).red(), 1e-6f);
        assertEquals(0f, new FxTint(0xFF0000, 1f).green(), 1e-6f);
    }

    @Test
    void colourWordsAreParsedAndPrinted() {
        assertEquals(0xFF7A2E, FxTint.parseColor("#FF7A2E"));
        assertEquals(0xFF7A2E, FxTint.parseColor("0xff7a2e"));
        assertEquals(0x00AA00, FxTint.parseColor("00aa00"));
        assertEquals(-1, FxTint.parseColor("red"));
        assertEquals(-1, FxTint.parseColor("#12345"));
        assertEquals(-1, FxTint.parseColor(null));
        assertEquals("#FF7A2E", FxTint.format(0xFF7A2E));
        assertEquals("none", FxTint.format(-1));
        assertEquals(0x803D17, FxTint.scaleRgb(0xFF7A2E, 0.5f));
        assertEquals(0xFFFFFF, FxTint.scaleRgb(0xFFFFFF, 3f));
        assertEquals(-1, FxTint.scaleRgb(-1, 0.5f));
    }
}
