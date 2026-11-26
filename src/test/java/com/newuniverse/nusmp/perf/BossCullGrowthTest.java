package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BossCullGrowthTest {
    @Test
    public void defaultsAndClamps() {
        double before = PerfSettings.bossCullGrowth();
        try {
            PerfSettings.setBossCullGrowth(PerfSettings.DEFAULT_BOSS_CULL_GROWTH);
            assertEquals(3.0, PerfSettings.bossCullGrowth());
            PerfSettings.setBossCullGrowth(-4.0);
            assertEquals(0.0, PerfSettings.bossCullGrowth());
            PerfSettings.setBossCullGrowth(99.0);
            assertEquals(16.0, PerfSettings.bossCullGrowth());
        } finally {
            PerfSettings.setBossCullGrowth(before);
        }
    }
}
