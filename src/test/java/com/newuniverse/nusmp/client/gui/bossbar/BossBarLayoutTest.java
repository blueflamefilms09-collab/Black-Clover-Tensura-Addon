package com.newuniverse.nusmp.client.gui.bossbar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The boss bar layout keeps vanilla's numbers for vanilla bars. */
class BossBarLayoutTest {
    @Test
    void vanillaSpacingAndTheThirdOfTheScreenRule() {
        assertEquals(19, BossBarLayout.step(false));
        assertTrue(BossBarLayout.step(true) > BossBarLayout.step(false));
        // a 1080p screen at GUI scale 3 is 360 high: bars at y = 12, 31, 50, 69, 88, 107, then 126 >= 120 stops
        int h = 360, y = BossBarLayout.START_Y, drawn = 1;
        while (BossBarLayout.continues(y + BossBarLayout.step(false), h)) { y += BossBarLayout.step(false); drawn++; }
        assertEquals(6, drawn);
        assertEquals(107, y);
        // a small screen still draws the first bar
        assertFalse(BossBarLayout.continues(BossBarLayout.START_Y + 19, 90));
    }

    @Test
    void fillWidthMatchesVanillasLerpDiscrete() {
        assertEquals(0, BossBarLayout.fillWidth(0f));
        assertEquals(1, BossBarLayout.fillWidth(0.0001f));
        assertEquals(182, BossBarLayout.fillWidth(1f));
        assertEquals(91, BossBarLayout.fillWidth(0.5f));
        assertEquals(182, BossBarLayout.fillWidth(7f));
        assertEquals(0, BossBarLayout.fillWidth(-2f));
        int last = 0;
        for (int i = 0; i <= 1000; i++) {
            int w = BossBarLayout.fillWidth(i / 1000f);
            assertTrue(w >= last && w <= 182);
            last = w;
        }
    }

    @Test
    void percentNeverReadsZeroWhileThereIsHealth() {
        assertEquals(0, BossBarLayout.percent(0f));
        assertEquals(1, BossBarLayout.percent(0.001f));
        assertEquals(50, BossBarLayout.percent(0.5f));
        assertEquals(100, BossBarLayout.percent(1f));
        assertEquals(100, BossBarLayout.percent(3f));
    }
}
