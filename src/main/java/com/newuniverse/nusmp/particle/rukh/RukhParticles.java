package com.newuniverse.nusmp.particle.rukh;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: Dark Rukh and Light Rukh (the Magi birds of fate) are the only particles relics, equipment and active abilities may spawn
 * (docs/rukh/r1_core.md). They are plain {@link SimpleParticleType}s (no payload: colour and flight are decided by the client particle
 * class, the server only says "a Rukh here"), registered on {@code Registries.PARTICLE_TYPE} through a DeferredRegister so NeoForge binds
 * them at the right moment and the packet codec can send the type id. {@code overrideLimiter=false}: a Rukh is decoration, it must obey the
 * client's particle-distance culling. The constructor of SimpleParticleType is protected, hence the anonymous subclass.
 * <p>
 * The accessors never return null and never throw: the registry holder is only bound after registration, so a call from a static
 * initialiser (which the purge forbids) would otherwise crash class loading; before binding they hand back the vanilla ENCHANT type, which
 * lives in this out-of-scope class on purpose and can only ever be seen if somebody breaks the lazy-lookup rule.
 */
public final class RukhParticles {
    private RukhParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, NUSMP.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DARK_RUKH = PARTICLES.register("dark_rukh", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHT_RUKH = PARTICLES.register("light_rukh", () -> new SimpleParticleType(false) {});

    public static void init(IEventBus modBus) {
        PARTICLES.register(modBus);
    }

    /** The Dark Rukh type (shadow, soul, death, curse). Never null. */
    public static SimpleParticleType dark() {
        return DARK_RUKH.isBound() ? DARK_RUKH.get() : ParticleTypes.ENCHANT;
    }

    /** The Light Rukh type (holy, sun, wind, element). Never null. */
    public static SimpleParticleType light() {
        return LIGHT_RUKH.isBound() ? LIGHT_RUKH.get() : ParticleTypes.ENCHANT;
    }

    public static SimpleParticleType of(RukhFx.Alignment a) {
        return a == RukhFx.Alignment.DARK ? dark() : light();
    }
}
