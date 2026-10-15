package com.newuniverse.nusmp.client.particle.rukh;

import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Why: particle providers are a client-only, mod-bus event ({@code RegisterParticleProvidersEvent}); {@code registerSpriteSet} hands the
 * provider the {@code SpriteSet} the engine built from {@code assets/nusmp/particles/<id>.json}, so the six wing-pose textures are stitched
 * into the particle atlas and reloaded with resource packs. Call {@code init} only on the client dist (the lead's registration line sits
 * with the other client inits).
 */
public final class RukhClient {
    private RukhClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(RukhClient::onRegisterProviders);
    }

    private static void onRegisterProviders(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(RukhParticles.DARK_RUKH.get(), DarkRukhParticle.Provider::new);
        e.registerSpriteSet(RukhParticles.LIGHT_RUKH.get(), LightRukhParticle.Provider::new);
    }
}
