package com.newuniverse.nusmp.unique;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Unique Guardian is one self-contained feature with one entry point, the single line the mod constructor calls: the server config {@code nusmp-unique-server.toml}, the events and the CI self-tests
 * ({@code unique.*}). The command is wired in {@code MultiverseCommands}. Safe to call twice.
 */
public final class UniqueBoot {
    private UniqueBoot() {}

    private static boolean done;

    public static synchronized void init(ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, UniqueConfig.SPEC, "nusmp-unique-server.toml");
        UniqueEvents.register(NeoForge.EVENT_BUS);
        UniqueSelfTests.register();
    }
}
