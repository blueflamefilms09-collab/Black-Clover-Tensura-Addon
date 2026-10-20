package com.newuniverse.nusmp.particle.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Why: the one payload of every custom particle: an optional colour (0xRRGGBB, or -1 for the texture's own colours) and a size multiplier, so that a single registered particle can be a
 * red ember in a Flame spell and a blue one in an Ice spell. It is what the server sends in the particle packet and what the client's provider reads. {@code dark} is NOT sent: it only
 * remembers, on the server, that these options came from the dark flavour of a theme ({@link Fx#toneOf}). The data rules (clamps, quantising, colour parsing) live in the pure {@link FxTint};
 * this class is the glue to the game's codec system. The type is a {@link TintedParticleType}.
 */
public final class TintedParticleOptions implements ParticleOptions {
    private final ParticleType<TintedParticleOptions> type;
    private final FxTint tint;
    private final boolean dark;
    private final boolean tintable;

    public TintedParticleOptions(ParticleType<TintedParticleOptions> type, int rgb, float scale) {
        this(type, rgb, scale, false, true);
    }

    public TintedParticleOptions(ParticleType<TintedParticleOptions> type, int rgb, float scale, boolean dark, boolean tintable) {
        this.type = type;
        this.tint = new FxTint(rgb, scale);
        this.dark = dark;
        this.tintable = tintable;
    }

    @Override
    public ParticleType<TintedParticleOptions> getType() {
        return type;
    }

    public FxTint tint() {
        return tint;
    }

    public int rgb() {
        return tint.rgb();
    }

    public float scale() {
        return tint.scale();
    }

    public boolean dark() {
        return dark;
    }

    /** True if the particle's texture is neutral, so a colour dyes it (the catalog's {@code tint}); not sent, set by the facade. */
    public boolean tintable() {
        return tintable;
    }

    /** The same particle with another colour and size. */
    public TintedParticleOptions with(int rgb, float scale) {
        return new TintedParticleOptions(type, rgb, scale, dark, tintable);
    }

    /** The same particle dyed {@code rgb}, but only if its texture is tintable (a coloured texture keeps its own colours). */
    public TintedParticleOptions withRgb(int rgb) {
        return tintable && rgb >= 0 ? new TintedParticleOptions(type, rgb, tint.scale(), dark, true) : this;
    }

    public static MapCodec<TintedParticleOptions> codec(ParticleType<TintedParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("color", -1).forGetter(TintedParticleOptions::rgb),
                Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(TintedParticleOptions::scale)
        ).apply(i, (c, s) -> new TintedParticleOptions(type, c, s)));
    }

    public static StreamCodec<io.netty.buffer.ByteBuf, TintedParticleOptions> streamCodec(ParticleType<TintedParticleOptions> type) {
        return StreamCodec.composite(ByteBufCodecs.INT, TintedParticleOptions::rgb, ByteBufCodecs.FLOAT, TintedParticleOptions::scale, (c, s) -> new TintedParticleOptions(type, c, s));
    }
}
