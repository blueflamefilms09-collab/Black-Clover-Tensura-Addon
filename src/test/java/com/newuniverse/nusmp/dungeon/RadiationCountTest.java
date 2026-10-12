package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Golden City's radiation, counted a second at a time: Mana Zone and Golden Ward shut it out, Magic Sense dulls it. */
class RadiationCountTest {
    @Test
    void aShieldAlwaysLetsTheRadiationEbb() {
        for (boolean sense : new boolean[]{false, true})
            for (int pass = 0; pass < 4; pass++) assertEquals(-1, RadiationCount.step(true, sense, pass));
    }

    @Test
    void anExposedPlayerIsSoakedEverySecond() {
        for (int pass = 0; pass < 4; pass++) assertEquals(1, RadiationCount.step(false, false, pass));
    }

    @Test
    void magicSenseLetsTheRadiationBuildEveryOtherSecond() {
        int pass = 0, total = 0;
        for (int second = 0; second < 10; second++) {
            int step = RadiationCount.step(false, true, pass);
            assertEquals(second % 2 == 0 ? 0 : 1, step, "second " + second);
            total += step;
            pass = RadiationCount.next(pass);
        }
        assertEquals(5, total);
    }

    @Test
    void theCounterOnlyTellsOddFromEven() {
        assertEquals(1, RadiationCount.next(0));
        assertEquals(0, RadiationCount.next(1));
        assertEquals(1, RadiationCount.next(2));
        assertEquals(0, RadiationCount.next(7));
    }

    @Test
    void aMinuteInTheCityBuildsHalfAsMuchWithMagicSense() {
        int bare = 0, dulled = 0, pass = 0;
        for (int second = 0; second < 60; second++) {
            bare = Math.max(0, bare + RadiationCount.step(false, false, 0));
            dulled = Math.max(0, dulled + RadiationCount.step(false, true, pass));
            pass = RadiationCount.next(pass);
        }
        assertEquals(60, bare);
        assertEquals(30, dulled);
    }

    @Test
    void aWardDrainsWhatHadBuiltButNeverBelowNothing() {
        int rad = 5;
        for (int second = 0; second < 8; second++) rad = Math.max(0, rad + RadiationCount.step(true, false, 0));
        assertEquals(0, rad);
    }
}
