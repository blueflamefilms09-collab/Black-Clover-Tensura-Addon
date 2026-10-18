package com.newuniverse.nusmp.prop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the painters of the six rebuilt summon sets read their model and clip from this word; a wrong shift would show the wrong summon or restart a clip every frame. */
class AirPropWordTest {
    @Test
    void roundTrips() {
        for (int v = 0; v < 4; v++) for (int s = 0; s < 3; s++) {
            int w = AirPropWord.pack(v, s, 123456L);
            assertEquals(v, AirPropWord.variant(w));
            assertEquals(s, AirPropWord.state(w));
            assertEquals(5, AirPropWord.since(w, 123461L));
        }
    }

    @Test
    void theClockWrapsWithoutGoingNegative() {
        int w = AirPropWord.pack(1, 1, 65535L);
        assertEquals(3, AirPropWord.since(w, 65538L));
        assertEquals(0, AirPropWord.since(w, 65535L));
        for (long t = 65535L; t < 65535L + 200; t++) assertTrue(AirPropWord.since(w, t) >= 0);
    }

    @Test
    void variantsAndStatesDoNotBleed() {
        int w = AirPropWord.pack(255, 3, 0xFFFFL);
        assertEquals(255, AirPropWord.variant(w));
        assertEquals(3, AirPropWord.state(w));
        assertEquals(0, AirPropWord.since(w, 0xFFFFL));
    }
}
