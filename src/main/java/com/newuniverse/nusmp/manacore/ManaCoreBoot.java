package com.newuniverse.nusmp.manacore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the rank Mana Cores (docs/mana_cores.md) are one self-contained feature, so one entry point: the single line the mod constructor calls (next to {@code BeastWillBoot.init}). It registers the 24 items, the server
 * config {@code nusmp-manacore-server.toml}, the game events, {@code /manacore} and the CI self-tests {@code manacore.*}. Safe to call twice. The client half (the 3D models) is separate and names nothing here.
 */
public final class ManaCoreBoot {
    private ManaCoreBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        ManaCoreItems.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ManaCoreConfig.SPEC, "nusmp-manacore-server.toml");
        ManaCoreEvents.register(NeoForge.EVENT_BUS);
        ManaCoreCommands.register(NeoForge.EVENT_BUS);
        ManaCoreSelfTests.register();
    }
}
