package com.newuniverse.nusmp.particle.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Why: the particle type of every custom particle ({@link FxTypes} registers one per catalog entry). A type carries the codecs of its options; the constructor of the game's
 * {@code ParticleType} is protected, hence this small subclass. {@code overrideLimiter=false}: a decoration particle must obey the client's particle-distance culling.
 */
public final class TintedParticleType extends ParticleType<TintedParticleOptions> {
    private final MapCodec<TintedParticleOptions> codec = TintedParticleOptions.codec(this);
    private final StreamCodec<io.netty.buffer.ByteBuf, TintedParticleOptions> streamCodec = TintedParticleOptions.streamCodec(this);
    private final TintedParticleOptions plain = new TintedParticleOptions(this, -1, 1.0f);

    public TintedParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<TintedParticleOptions> codec() {
        return codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, TintedParticleOptions> streamCodec() {
        return streamCodec;
    }

    /** The options with no tint and scale 1: the texture's own look. */
    public TintedParticleOptions plain() {
        return plain;
    }

    /** These options with a colour and size (allocates; the facade caches the ones it uses every tick). */
    public TintedParticleOptions of(int rgb, float scale) {
        return new TintedParticleOptions(this, rgb, scale);
    }
}
