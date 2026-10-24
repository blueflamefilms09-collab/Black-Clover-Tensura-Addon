package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.companion.net.CompanionNet;
import com.newuniverse.nusmp.companion.sample.CompanionEntities;
import com.newuniverse.nusmp.companion.sample.CompanionProfiles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the companion system is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the server config {@code nusmp-companions-server.toml}
 * ({@code companions.enabled} and friends), the five payloads, the sample companion (entity type and creative spawn egg), the tastes of the sample companions (at common setup), the game events and the CI self-tests
 * {@code magic.companions.*}. The client half is {@code client.companion.CompanionClient}, called from the client block of the mod constructor. Safe to call twice.
 */
public final class CompanionBoot {
    private CompanionBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, CompanionConfig.SPEC, "nusmp-companions-server.toml");
        CompanionEntities.init(modBus);
        modBus.addListener(CompanionNet::register);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(CompanionProfiles::registerAll));
        CompanionEvents.register(NeoForge.EVENT_BUS);
        CompanionSelfTests.register();
    }
}
