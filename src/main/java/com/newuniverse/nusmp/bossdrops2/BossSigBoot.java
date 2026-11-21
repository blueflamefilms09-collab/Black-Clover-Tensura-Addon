package com.newuniverse.nusmp.bossdrops2;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the worthwhile boss drops (docs/boss_drops_worthwhile.md) are one self-contained feature, so they have one entry point: the single line the mod constructor calls. It registers the two items, the
 * server config {@code nusmp-bossdrops2-server.toml} (the one setting {@code bossDrops.worthwhile}), the table reload listener, the damage and death hooks and the CI self-tests ({@code bossdrops2.*}).
 * Safe to call twice. The command ({@link SignatureCommand#node()}) and the creative stacks ({@link BossSigItems#addTo}) are appended to their own registries by one line each.
 */
public final class BossSigBoot {
    private BossSigBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        BossSigItems.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SignatureConfig.SPEC, "nusmp-bossdrops2-server.toml");
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new SignatureData()));
        SignatureEvents.register(NeoForge.EVENT_BUS);
        SignatureSelfTests.register();
    }
}
