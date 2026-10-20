package com.newuniverse.nusmp.particle.fx;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: registers the mod's own particle types on {@code Registries.PARTICLE_TYPE} through a DeferredRegister, one {@link TintedParticleType} per entry of the catalog
 * ({@link FxCatalog}: 70 particles in themed families), so NeoForge binds them at the right moment and the packet codec can send their ids. The ids are the catalog ids ({@code nusmp:ember},
 * {@code nusmp:rune_sol}, ...). Lookups never throw: before the registry is bound (a call from a static initialiser) {@link #type} answers null and the facade falls back to its vanilla
 * particle. The holders are collected in a map built once, in catalog order.
 */
public final class FxTypes {
    private FxTypes() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, NUSMP.MODID);

    private static final Map<String, DeferredHolder<ParticleType<?>, TintedParticleType>> HOLDERS;

    static {
        Map<String, DeferredHolder<ParticleType<?>, TintedParticleType>> m = new LinkedHashMap<>();
        for (FxCatalog.Def d : FxCatalog.all()) m.put(d.id(), PARTICLES.register(d.id(), () -> new TintedParticleType(false)));
        HOLDERS = Collections.unmodifiableMap(m);
    }

    public static void init(IEventBus modBus) {
        PARTICLES.register(modBus);
    }

    /** All registered holders by catalog id, in catalog order. */
    public static Map<String, DeferredHolder<ParticleType<?>, TintedParticleType>> holders() {
        return HOLDERS;
    }

    /** The type of a catalog id, or null if the id is unknown or the registry is not bound yet. */
    public static TintedParticleType type(String id) {
        DeferredHolder<ParticleType<?>, TintedParticleType> h = HOLDERS.get(id);
        return h != null && h.isBound() ? h.get() : null;
    }

    /** True once every type is bound (the game finished registering). */
    public static boolean bound() {
        for (DeferredHolder<ParticleType<?>, TintedParticleType> h : HOLDERS.values()) if (!h.isBound()) return false;
        return !HOLDERS.isEmpty();
    }
}
