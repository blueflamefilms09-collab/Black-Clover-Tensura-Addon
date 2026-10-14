package com.newuniverse.nusmp.client.relics.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The particle budget and the reading of the shared quality option. */
class SocketFxRulesTest {
    @Test
    void intervalsGetShorterWithQuality() {
        assertEquals(0, SocketFxRules.interval(SocketFxRules.OFF));
        assertEquals(8, SocketFxRules.interval(SocketFxRules.LOW));
        assertEquals(4, SocketFxRules.interval(SocketFxRules.MEDIUM));
        assertEquals(2, SocketFxRules.interval(SocketFxRules.HIGH));
        assertEquals(2, SocketFxRules.interval(99));
        assertEquals(0, SocketFxRules.interval(-5));
    }

    @Test
    void dueIsStaggeredAndNeverAtQualityZero() {
        for (int t = 0; t < 40; t++) assertFalse(SocketFxRules.due(t, 3, 0));
        int bursts = 0;
        for (int t = 0; t < 40; t++) if (SocketFxRules.due(t, 3, SocketFxRules.MEDIUM)) bursts++;
        assertEquals(10, bursts);
        assertTrue(SocketFxRules.due(1, 3, SocketFxRules.MEDIUM));       // (1 + 3) % 4 == 0
        assertFalse(SocketFxRules.due(1, 4, SocketFxRules.MEDIUM));
        assertTrue(SocketFxRules.due(-4, 0, SocketFxRules.MEDIUM));      // negative times do not break the modulus
    }

    @Test
    void theBudgetIsSmall() {
        assertTrue(SocketFxRules.MAX_HOLDERS <= 8 && SocketFxRules.MAX_PARTICLES_PER_TICK <= 10);
        assertEquals(1, SocketFxRules.perBurst(SocketFxRules.MEDIUM));
        assertEquals(2, SocketFxRules.perBurst(SocketFxRules.HIGH));
    }

    @Test
    void distanceIsTheViewDistanceCappedAt24() {
        assertEquals(16.0 * 16.0, SocketFxRules.maxDistanceSq(1), 1e-9);
        assertEquals(24.0 * 24.0, SocketFxRules.maxDistanceSq(12), 1e-9);
        assertEquals(24.0 * 24.0, SocketFxRules.maxDistanceSq(32), 1e-9);
        assertEquals(16.0 * 16.0, SocketFxRules.maxDistanceSq(0), 1e-9);
    }

    @Test
    void qualityParsesWhateverShapeTheOptionHas() {
        assertEquals(SocketFxRules.DEFAULT_QUALITY, SocketFxRules.parseQuality(null));
        assertEquals(0, SocketFxRules.parseQuality(0));
        assertEquals(3, SocketFxRules.parseQuality(7));
        assertEquals(0, SocketFxRules.parseQuality(-1));
        assertEquals(2, SocketFxRules.parseQuality(2.4));
        assertEquals(SocketFxRules.OFF, SocketFxRules.parseQuality(false));
        assertEquals(SocketFxRules.DEFAULT_QUALITY, SocketFxRules.parseQuality(true));
        assertEquals(SocketFxRules.LOW, SocketFxRules.parseQuality("Low"));
        assertEquals(SocketFxRules.HIGH, SocketFxRules.parseQuality(" ULTRA "));
        assertEquals(SocketFxRules.OFF, SocketFxRules.parseQuality("off"));
        assertEquals(SocketFxRules.DEFAULT_QUALITY, SocketFxRules.parseQuality("gibberish"));
    }
}
