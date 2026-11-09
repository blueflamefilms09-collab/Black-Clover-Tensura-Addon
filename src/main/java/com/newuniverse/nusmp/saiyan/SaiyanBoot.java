package com.newuniverse.nusmp.saiyan;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Saiyan system (docs/saiyan.md) is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the save attachment, the server config
 * {@code nusmp-saiyan-server.toml}, the two packets, the game events, the commands and the CI self-tests {@code saiyan.*}. Safe to call twice. The client half (keys, HUD, hiding Tensura's magicule bar
 * for a Saiyan) is {@code client.saiyan.SaiyanClient}, wired by the client setup; this class names no client class.
 */
public final class SaiyanBoot {
    private SaiyanBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        SaiyanStore.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SaiyanConfig.SPEC, "nusmp-saiyan-server.toml");
        modBus.addListener(SaiyanNet::registerPayloads);
        SaiyanEvents.register(NeoForge.EVENT_BUS);
        SaiyanCommands.register(NeoForge.EVENT_BUS);
        SaiyanSelfTests.register();
        SaiyanStage2SelfTests.register();
        SaiyanTechSelfTests.register();
        SaiyanTech3SelfTests.register();
        SaiyanLookSelfTests.register();
    }
}
