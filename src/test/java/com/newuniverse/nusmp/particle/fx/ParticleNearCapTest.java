package com.newuniverse.nusmp.particle.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 1.110.0: the first-person size cap (pure): near-camera shrink, the view fraction, the spawn skip, and what the real catalog does with it. */
class ParticleNearCapTest {
    private static double sq(double d) {
        return d * d;
    }

    @Test
    void aParticleVanishesAtTheCameraAndIsFullSizeFromOneAndAHalfBlocks() {
        assertEquals(0f, ParticleNearCap.nearFactor(0.0), 0f);
        assertEquals(0f, ParticleNearCap.nearFactor(sq(ParticleNearCap.NEAR_ZERO)), 0f);
        assertEquals(1f, ParticleNearCap.nearFactor(sq(ParticleNearCap.NEAR_FULL)), 0f);
        assertEquals(1f, ParticleNearCap.nearFactor(sq(30)), 0f);
        float last = 0f;
        for (double d = 0.0; d <= 2.0; d += 0.05) {
            float f = ParticleNearCap.nearFactor(sq(d));
            assertTrue(f >= last - 1e-6f, "monotonic at " + d);
            assertTrue(f >= 0f && f <= 1f);
            last = f;
        }
        assertEquals(0.5f, ParticleNearCap.nearFactor(sq((ParticleNearCap.NEAR_ZERO + ParticleNearCap.NEAR_FULL) / 2)), 1e-5f);
    }

    @Test
    void theFlameOfTheScreenshotsCannotFillTheViewAnyMore() {
        // the catalog flame: width 0.28 at scale 1 -> half-size 0.14; vanilla's was about 0.2 and sat at the eye
        float half = 0.14f;
        for (double d = 0.0; d <= 1.2; d += 0.05) {
            float c = ParticleNearCap.cap(half, sq(d));
            assertTrue(c <= d * ParticleNearCap.MAX_RATIO + 1e-6, "at " + d + " blocks the half-size is " + c);
            assertTrue(c <= half + 1e-6f);
        }
        assertEquals(0f, ParticleNearCap.cap(half, sq(0.2)), 0f, "at the eye it is gone");
        assertEquals(half, ParticleNearCap.cap(half, sq(5)), 1e-6f, "far away it is untouched");
        // a big scale (a table asking for 2.0) is capped by the absolute limit
        assertEquals(ParticleNearCap.MAX_HALF_SIZE, ParticleNearCap.cap(3f, sq(10)), 1e-6f);
        assertEquals(0f, ParticleNearCap.cap(0f, sq(10)), 0f);
        // the share of the view: half-size over distance never passes the ratio, so the sprite is at most about a fifth of the view height
        for (double d = 0.4; d < 4.0; d += 0.1) assertTrue(ParticleNearCap.cap(0.45f, sq(d)) / d <= ParticleNearCap.MAX_RATIO + 1e-6);
    }

    @Test
    void anEffectSkipsASpawnInsideTheFirstPersonBubble() {
        assertTrue(ParticleNearCap.skipSpawn(0.0));
        assertTrue(ParticleNearCap.skipSpawn(sq(1.1)));
        assertFalse(ParticleNearCap.skipSpawn(sq(1.2)));
        assertFalse(ParticleNearCap.skipSpawn(sq(3)));
        assertTrue(ParticleNearCap.SPAWN_SKIP < ParticleNearCap.NEAR_FULL);
    }

    @Test
    void onlyAmbientSizedParticlesAreCappedAndTheShapesKeepTheirSize() throws IOException {
        for (FxCatalog.Def d : FxCatalog.all()) {
            boolean ambient = ParticleNearCap.applies(d.profile().sizeStart(), d.profile().sizeEnd());
            boolean shape = d.family().equals("pulse") || d.family().equals("combat");
            if (shape) assertFalse(ambient, d.id() + " is a shape and must keep its size");
            if (d.id().equals("flame_tongue") || d.id().equals("ember") || d.id().equals("mote") || d.id().equals("spark")) assertTrue(ambient, d.id() + " is ambient");
        }
        assertTrue(ParticleNearCap.applies(0.28f, 0.08f));
        assertFalse(ParticleNearCap.applies(1.6f, 1.9f));
    }

    @Test
    void theParticleClassUsesTheCap() throws IOException {
        String src = Files.readString(Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp/client/particle/fx/FxParticle.java"), StandardCharsets.UTF_8);
        assertTrue(src.contains("ParticleNearCap.cap(") && src.contains("ParticleNearCap.applies("), "FxParticle lost the near-camera cap");
    }
}
