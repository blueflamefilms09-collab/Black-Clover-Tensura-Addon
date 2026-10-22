package com.newuniverse.nusmp.shishou;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: Shishou is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the item, the entity, the data components and the Black Flame effect, the server
 * config {@code nusmp-shishou-server.toml} ({@code relic.shishouEnabled}, {@code relic.shishouVoice}, a switch per skill), the payloads, the capability, the events and the CI self-tests
 * ({@code relic.shishou}, {@code relic.shishou_claim}, {@code relic.shishou_skills}). Safe to call twice. The client half is {@code client.shishou.ShishouClient}, called from the client block of the
 * constructor; the weapon renderer lists {@link ShishouItems#renderables()}.
 */
public final class ShishouBoot {
    private ShishouBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        ShishouItems.REGISTER.register(modBus);
        ShishouEntities.ENTITIES.register(modBus);
        ShishouComponents.COMPONENTS.register(modBus);
        ShishouEffects.EFFECTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ShishouConfig.SPEC, "nusmp-shishou-server.toml");
        modBus.addListener(ShishouNet::registerPayloads);
        modBus.addListener(ShishouEntities::attributes);
        modBus.addListener(ShishouEvents::registerCapabilities);
        ShishouEvents.register(NeoForge.EVENT_BUS);
        ShishouSelfTests.register();
    }
}
