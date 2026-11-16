package com.newuniverse.nusmp.stones;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Infinity Stone slot is one self-contained feature with one entry point, the single line the mod constructor calls: the server config {@code nusmp-stones-server.toml}, the events and the
 * CI self-tests ({@code stones.*}). The relic hooks (catalog, engine, right-click equip) and the command are wired in their own files. Safe to call twice.
 */
public final class StoneBoot {
    private StoneBoot() {}

    private static boolean done;

    public static synchronized void init(ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, StoneConfig.SPEC, "nusmp-stones-server.toml");
        StoneEvents.register(NeoForge.EVENT_BUS);
        StoneSelfTests.register();
    }
}
