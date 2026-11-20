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
        DevilEntities.init(modBus);                                                          // 3.97.0: the devil's figure (anime_multiverse:devil_npc), npc.* in nusmp-devil-server.toml
        container.registerConfig(ModConfig.Type.SERVER, DevilConfig.SPEC, "nusmp-devil-server.toml");
        container.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.devil.dual.DualConfig.SPEC, "nusmp-devil-dual-server.toml");      // 4.40.0: Double Magic, One of One, corruption (docs/devil_double_magic.md)
        com.newuniverse.nusmp.devil.dual.ConfigCondition.register(modBus);                    // 4.40.0: the data condition of the craftable Devil Contract
        modBus.addListener(DevilNet::registerPayloads);
        DevilContracts.register(NeoForge.EVENT_BUS);
        DevilCombat.register(NeoForge.EVENT_BUS);                                            // 3.76.0: the attribute's combat effect, the Brand
        DevilCommands.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.devil.dual.DevilOwnership.register(NeoForge.EVENT_BUS);        // 4.40.0: One of One and the corruption: login, logout, death, kill
        com.newuniverse.nusmp.devil.dual.DevilDualSelfTests.register();                      // 4.40.0: devil.double.*
        DevilSelfTests.register();
        DevilHitFx.registerSelfTests();                                                      // VFX wave 2: the devil hits in their anime style: devil.vfx
    }
}
