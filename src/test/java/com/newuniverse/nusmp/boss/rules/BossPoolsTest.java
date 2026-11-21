package com.newuniverse.nusmp.boss.rules;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 4.96.0: Tensura's attackerless energy-loss damage is named exactly, a nearly empty pool is refilled and a full one is left alone, and everything is wired behind one setting. */
class BossPoolsTest {
    @Test
    void onlyTensurasEnergyDrainTypeIsStarvation() {
        assertTrue(BossEnvironment.isEnergyStarvationType("tensura:energy_drain"));
        assertFalse(BossEnvironment.isEnergyStarvationType("minecraft:generic"));
        assertFalse(BossEnvironment.isEnergyStarvationType("tensura:heat"));
        assertFalse(BossEnvironment.isEnergyStarvationType(""));
        assertFalse(BossEnvironment.energyStarvation(null));
    }

    @Test
    void aPoolIsRefilledOnlyWhenLow() {
        assertTrue(BossPools.needsRefill(0.0, 1000.0));
        assertTrue(BossPools.needsRefill(850.0, 1000.0));
        assertFalse(BossPools.needsRefill(950.0, 1000.0));
        assertFalse(BossPools.needsRefill(0.0, 0.0), "no pool, nothing to fill");
    }

    @Test
    void theRuleIsWiredBehindItsSettingAndRunsOnceASecond() throws Exception {
        String hooks = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/boss/rules/BossRulesHooks.java"));
        assertTrue(hooks.contains("KEEP_POOLS_FULL.get() && BossEnvironment.energyStarvation"));
        assertTrue(hooks.contains("BossPools.topUp"));
        assertEquals(20, BossRulesHooks.POOL_TICKS);
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/boss/rules/BossRulesConfig.java"));
        assertTrue(cfg.contains("\"keepPoolsFull\", true"));
    }
}
