package com.newuniverse.nusmp.arc.tier;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 3.36.0: the registry of running Death Note scenes: keyed by player UUID, bounded, cleaned up on logout / death / dimension change / boss gone / server stop. */
class LightScenesTest {
    private static void clean() { LightScenes.reset(); }

    private static UUID id() { return UUID.randomUUID(); }

    @Test
    void oneSceneAtAPlayerAndOnePerLight() {
        clean();
        UUID light = id(), p = id();
        LightScene[] out = new LightScene[1];
        assertEquals(LightScenes.Start.STARTED, LightScenes.start(light, p, 0, out));
        assertSame(out[0], LightScenes.ofTarget(p));
        assertSame(out[0], LightScenes.ofLight(light));
        assertEquals(LightScenes.Start.LIGHT_BUSY, LightScenes.start(light, id(), 0, null));
        assertEquals(LightScenes.Start.TARGET_BUSY, LightScenes.start(id(), p, 0, null));
        assertEquals(1, LightScenes.active());
    }

    @Test
    void theNumberOfRunningScenesIsCapped() {
        clean();
        for (int i = 0; i < LightScenes.MAX_ACTIVE; i++) assertEquals(LightScenes.Start.STARTED, LightScenes.start(id(), id(), 0, null));
        assertEquals(LightScenes.Start.FULL, LightScenes.start(id(), id(), 0, null));
        assertEquals(LightScenes.MAX_ACTIVE, LightScenes.active());
    }

    @Test
    void aPlayerCoolsDownAfterAScene() {
        clean();
        UUID light = id(), p = id();
        LightScene[] out = new LightScene[1];
        LightScenes.start(light, p, 1000, out);
        LightScenes.end(out[0], 1200);
        assertEquals(0, LightScenes.active());
        assertEquals(LightScenes.Start.TARGET_COOLING, LightScenes.start(id(), p, 1200 + LightScenes.TARGET_COOLDOWN - 1, null));
        assertEquals(LightScenes.Start.STARTED, LightScenes.start(id(), p, 1200 + LightScenes.TARGET_COOLDOWN, null));
    }

    @Test
    void logoutDeathAndDimensionChangeCancelTheScene() {
        clean();
        UUID p = id();
        LightScene[] out = new LightScene[1];
        LightScenes.start(id(), p, 0, out);
        LightScenes.playerGone(p);                       // all three events call this
        assertEquals(LightScene.Phase.CANCELLED, out[0].phase());
        assertEquals(LightScene.Cancel.TARGET_GONE, out[0].cancelReason());
        assertNull(LightScenes.ofTarget(p));
        assertEquals(0, LightScenes.active());
        assertFalse(LightScenes.listed(out[0]));
        assertEquals(LightScenes.Start.STARTED, LightScenes.start(id(), p, 1, null), "a player who came back is not on cooldown");
    }

    @Test
    void theBossLeavingTheWorldCancelsAndReturnsTheScene() {
        clean();
        UUID light = id();
        LightScene[] out = new LightScene[1];
        LightScenes.start(light, id(), 0, out);
        LightScene gone = LightScenes.lightGone(light, LightScene.Cancel.LIGHT_DIED);
        assertSame(out[0], gone);
        assertEquals(LightScene.Cancel.LIGHT_DIED, gone.cancelReason());
        assertEquals(0, LightScenes.active());
        assertNull(LightScenes.lightGone(light, LightScene.Cancel.LIGHT_DIED));
    }

    @Test
    void theCooldownTableIsBounded() {
        clean();
        for (int i = 0; i < LightScenes.MAX_COOLDOWNS * 3; i++) {
            LightScene[] out = new LightScene[1];
            LightScenes.start(id(), id(), 0, out);
            LightScenes.end(out[0], 0);
        }
        assertTrue(LightScenes.cooldowns() <= LightScenes.MAX_COOLDOWNS);
    }

    @Test
    void resetForgetsEverything() {
        clean();
        LightScenes.start(id(), id(), 0, null);
        LightScene[] out = new LightScene[1];
        LightScenes.start(id(), id(), 0, out);
        LightScenes.end(out[0], 0);
        LightScenes.reset();
        assertEquals(0, LightScenes.active());
        assertEquals(0, LightScenes.cooldowns());
    }
}
