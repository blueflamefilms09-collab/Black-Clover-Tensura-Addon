package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one place that wires the Dungeon's spawn filter into the game (one call from the mod constructor): its server config, its three event nets and
 * its self-tests.
 */
public final class SpawnFilterBoot {
    private SpawnFilterBoot() {}

    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SpawnFilterConfig.SPEC, "nusmp-labyrinth-mega-spawnfilter-server.toml");
        SpawnFilterEvents.register(NeoForge.EVENT_BUS);
        SpawnFilterSelfTests.register();
    }
}
