package com.newuniverse.nusmp.meteor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one place that wires the shared meteor into the game (one call from the mod constructor, docs/meteor_fall.md): the entity type, the server config
 * ({@code nusmp-meteor-server.toml}), the cleanup events, the self-test {@code meteor.fall}, and on a client the renderer and the look config ({@code nusmp-meteor-client.toml}).
 * Every handler returns at once when no meteor is in the air.
 */
public final class MeteorBoot {
    private MeteorBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        MeteorEntities.ENTITIES.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, MeteorConfig.SPEC, "nusmp-meteor-server.toml");
        NeoForge.EVENT_BUS.addListener(MeteorFall::onLogout);
        NeoForge.EVENT_BUS.addListener(MeteorFall::onDimension);
        NeoForge.EVENT_BUS.addListener(MeteorFall::onDeath);
        NeoForge.EVENT_BUS.addListener(MeteorFall::onServerStopped);
        MeteorSelfTests.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            container.registerConfig(ModConfig.Type.CLIENT, MeteorClientConfig.SPEC, "nusmp-meteor-client.toml");
            com.newuniverse.nusmp.client.meteor.MeteorClient.init(modBus);
        }
    }
}
