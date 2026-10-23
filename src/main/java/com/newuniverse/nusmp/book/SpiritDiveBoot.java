package com.newuniverse.nusmp.book;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Spirit Dive remake: the one entry point (a single line of the mod constructor, both sides): the server config {@code nusmp-spiritdive-server.toml} ({@link SpiritDiveConfig}), the events of
 * Fuegoleon's flaming arm ({@link FuegoleonArm}) and the CI self-tests ({@link SpiritDiveSelfTests}). The client half (the painters, the first-person arm, the options) is
 * {@code client.aura.SpiritDiveClient}, called from the client block of the constructor. Safe to call twice.
 */
public final class SpiritDiveBoot {
    private SpiritDiveBoot() {}

    private static boolean done;

    public static synchronized void init(ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, SpiritDiveConfig.SPEC, "nusmp-spiritdive-server.toml");
        NeoForge.EVENT_BUS.addListener(FuegoleonArm::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(FuegoleonArm::onDeath);
        SpiritDiveSelfTests.register();
        FuegoleonArmDjinnSelfTests.register();                                                       // 3.1: self-test magic.fuegoleon_arm_djinn
        SpiritDiveAnimeSelfTests.register();                                                         // 3.2: self-test magic.spirit_dive_anime
    }
}
