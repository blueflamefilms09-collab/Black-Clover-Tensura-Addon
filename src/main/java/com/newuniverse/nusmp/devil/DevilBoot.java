package com.newuniverse.nusmp.devil;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Devil Contract system (docs/devil_contract.md, 3.48.0) is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the save attachment, the
 * server config {@code nusmp-devil-server.toml}, the three packets, the game events, the {@code /devil} command and the CI self-tests {@code devil.*}. Safe to call twice. The client half (the key, the
 * HUD, the Weg layer, the eye shade) is {@code client.devil.DevilClient}, wired by the client setup; this class names no client class.
 */
public final class DevilBoot {
    private DevilBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        DevilStore.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, DevilConfig.SPEC, "nusmp-devil-server.toml");
        modBus.addListener(DevilNet::registerPayloads);
        DevilContracts.register(NeoForge.EVENT_BUS);
        DevilCommands.register(NeoForge.EVENT_BUS);
        DevilSelfTests.register();
    }
}
