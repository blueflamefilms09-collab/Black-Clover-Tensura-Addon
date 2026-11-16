package com.newuniverse.nusmp.bossdrops;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the boss drop framework (data tables, trophies, Trophy Plate and Lucifero's armour, docs/bosses/boss_drops.md) is one self-contained feature, so it has one entry point: the single line the mod
 * constructor calls. It registers the items and materials, the server config {@code nusmp-bossdrops-server.toml}, the table reload listener, the death hook and the CI self-tests ({@code bossdrops.*}).
 * Safe to call twice.
 */
public final class BossDropBoot {
    private BossDropBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        BossDropItems.MATERIALS.register(modBus);
        BossDropItems.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, BossDropConfig.SPEC, "nusmp-bossdrops-server.toml");
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new BossDropData()));
        BossDropEvents.register(NeoForge.EVENT_BUS);
        BossDropSelfTests.register();
    }
}
