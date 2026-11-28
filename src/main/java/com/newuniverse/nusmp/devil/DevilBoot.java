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
        container.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.devil.phase2.Phase2Config.SPEC, "nusmp-devil-phase2-server.toml");      // phase 2: summon chance, EP, trial, kinds, stacking, sharing, sever, sabotage (docs/devil_contract_phase2.md)
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) -> e.put(DevilEntities.TRIAL.get(), com.newuniverse.nusmp.devil.phase2.DevilTrialEntity.createAttributes().build()));
        com.newuniverse.nusmp.devil.dual.ConfigCondition.register(modBus);                    // 4.40.0: the data condition of the craftable Devil Contract
        modBus.addListener(DevilNet::registerPayloads);
        DevilContracts.register(NeoForge.EVENT_BUS);
        DevilCombat.register(NeoForge.EVENT_BUS);                                            // 3.76.0: the attribute's combat effect, the Brand
        DevilCommands.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.devil.dual.DevilOwnership.register(NeoForge.EVENT_BUS);        // 4.40.0: One of One and the corruption: login, logout, death, kill
        com.newuniverse.nusmp.devil.dual.DevilDualSelfTests.register();                      // 4.40.0: devil.double.*
        com.newuniverse.nusmp.devil.phase2.Phase2.register(NeoForge.EVENT_BUS);              // phase 2: login, logout, death, dimension and blows between holders
        com.newuniverse.nusmp.devil.phase2.Phase2SelfTests.register();                       // phase 2: devil.phase2.*
        com.newuniverse.nusmp.devil.mastery.Mastery.register(NeoForge.EVENT_BUS);             // Devil Awakening: mastery, blows, kills (docs/devil_mastery.md)
        DevilCalling.register(NeoForge.EVENT_BUS);                                           // 5.7.0: a devil may ask a player for a contract (2 percent)
        DevilSelfTests.register();
        DevilHitFx.registerSelfTests();                                                      // VFX wave 2: the devil hits in their anime style: devil.vfx
    }
}
