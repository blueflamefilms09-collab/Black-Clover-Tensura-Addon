package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: a sector's theme should be felt before a single block is read: crimson spores drift over a Blood sector, snowflakes over a Frost one, end
 * rods flicker in an Astral one and Light Rukh drift through a Radiant one. It is the cheapest way to make the palettes read as different places, and it costs almost nothing: every two seconds
 * (40 ticks) each player (spectators excepted) inside the Mega-Dungeon gets one {@code sendParticles} call of a handful of particles (the count is
 * capped by a constant, not by config, so no setting can turn a sector into a particle storm) of the particle their sector's theme names. Only a
 * {@code SimpleParticleType} can be drawn from an id (dust, blocks and items need options), so a theme that names anything else simply has no
 * ambient; the id is resolved once and cached. The tick handler returns before any lookup when the switch is off, when the tick is not a multiple of
 * 40, or when nobody is in the dimension, so an empty dungeon costs one modulo.
 */
final class ThemeAmbient {
    private ThemeAmbient() {}

    /** Particles per player per pulse: a few, never a storm. */
    static final int COUNT = 6;
    private static final Map<String, Optional<SimpleParticleType>> CACHE = new ConcurrentHashMap<>();

    static void clear() { CACHE.clear(); }

    static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 40 != 0 || !ThemeConfig.get(ThemeConfig.AMBIENT_ENABLED)) return;
        ServerLevel level = MegaDungeon.level(server);
        if (level == null || level.players().isEmpty()) return;
        try {
            MegaData data = MegaData.get(server);
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator()) continue;
                Sector s = data.sectorAt(p.blockPosition());
                if (s == null) continue;
                SimpleParticleType type = particle(ThemeDefs.get(s.theme).ambient());
                if (type == null) continue;
                level.sendParticles(type, p.getX(), p.getY() + 1.2, p.getZ(), COUNT, 4.5, 1.8, 4.5, 0.02);
            }
        } catch (RuntimeException ignored) {
            // an ambient particle must never disturb the tick
        }
    }

    /** The simple particle type with this id, or null (empty id, unknown id, or a type that needs options). */
    static SimpleParticleType particle(String id) {
        if (id == null || id.isEmpty()) return null;
        return CACHE.computeIfAbsent(id, key -> {
            ResourceLocation rl = ResourceLocation.tryParse(key);
            ParticleType<?> type = rl == null ? null : BuiltInRegistries.PARTICLE_TYPE.getOptional(rl).orElse(null);
            return type instanceof SimpleParticleType simple ? Optional.of(simple) : Optional.empty();
        }).orElse(null);
    }
}
