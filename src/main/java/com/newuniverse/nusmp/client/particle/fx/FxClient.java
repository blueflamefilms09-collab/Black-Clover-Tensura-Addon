package com.newuniverse.nusmp.client.particle.fx;

import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.FxTypes;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.fx.TintedParticleType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Why: particle providers are a client-only mod-bus event ({@code RegisterParticleProvidersEvent}); {@code registerSpriteSet} hands each provider the {@code SpriteSet} the engine built from
 * {@code assets/nusmp/particles/<id>.json}, so every animation frame is stitched into the particle atlas and reloaded with resource packs. One provider class serves all 70 types; it only
 * remembers its sprite set and the catalog entry of its type (one atlas lookup list per type, nothing allocated per particle but the particle). Call {@code init} only on the client dist
 * (the line sits with the other client inits in the mod constructor).
 */
public final class FxClient {
    private FxClient() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FxConfig.CLIENT, "nusmp-particles-client.toml");
        modBus.addListener(FxClient::onRegisterProviders);
        modBus.addListener((ModConfigEvent.Loading e) -> FxConfig.apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> FxConfig.apply());
    }

    private static void onRegisterProviders(RegisterParticleProvidersEvent e) {
        for (FxCatalog.Def def : FxCatalog.all()) {
            TintedParticleType type = FxTypes.type(def.id());
            if (type == null) continue;
            e.registerSpriteSet(type, sprites -> new Provider(sprites, def));
        }
    }

    /** The provider of every custom particle: turns the options of a particle packet into an {@link FxParticle}. */
    public static final class Provider implements ParticleProvider<TintedParticleOptions> {
        private final SpriteSet sprites;
        private final FxCatalog.Def def;

        public Provider(SpriteSet sprites, FxCatalog.Def def) {
            this.sprites = sprites;
            this.def = def;
        }

        @Override
        public Particle createParticle(TintedParticleOptions options, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new FxParticle(level, x, y, z, vx, vy, vz, sprites, def, options.tint());
        }
    }
}
