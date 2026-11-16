package com.newuniverse.nusmp.labyrinth.mega.cores;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Boss Cores (cores dropped by the deep Decennial bosses and the four armour families made from them, docs/boss_cores.md) are one self-contained feature, so it has one entry point: the single line
 * the mod constructor calls. It registers the seven cores, the twenty-eight armour pieces and their seven materials, the server config {@code nusmp-boss-cores-server.toml}, the game events and the CI
 * self-tests ({@code cores.rules}, {@code cores.items}, {@code cores.world}). The boss hook is one line in {@code DecennialLayer.onDeath}; the creative menu lines are in {@code NUCreativeTab}. Safe to call twice.
 */
public final class BossCoreBoot {
    private BossCoreBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        BossCoreItems.MATERIALS.register(modBus);
        BossCoreItems.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, BossCoreConfig.SPEC, "nusmp-boss-cores-server.toml");
        BossCoreEvents.register(NeoForge.EVENT_BUS);
        BossCoreSelfTests.register();
    }
}
