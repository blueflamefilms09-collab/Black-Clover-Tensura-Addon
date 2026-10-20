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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
 * ambient; the id is resolved once and cached. The theme's mood sound ({@code "sound"} in its file: a vanilla ambience such as the crimson forest's
 * mood or a far-off peal of thunder) is played on the same pulse, once in a few pulses per player at a random spot a few blocks away, so a sector also
 * SOUNDS different with the eyes shut; an id the game does not know is skipped. The tick handler returns before any lookup when the switch is off, when
 * the tick is not a multiple of 40, or when nobody is in the dimension, so an empty dungeon costs one modulo.
 */
final class ThemeAmbient {
    private ThemeAmbient() {}

    /** Particles per player per pulse: a few, never a storm. */
    static final int COUNT = 6;
    private static final Map<String, Optional<SimpleParticleType>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<SoundEvent>> SOUNDS = new ConcurrentHashMap<>();

    static void clear() {
        CACHE.clear();
        SOUNDS.clear();
    }

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
                ThemeDef def = ThemeDefs.get(s.theme);
                mood(level, p, def.mood(), server.getTickCount() / 40);
                SimpleParticleType type = particle(def.ambient());
                if (type == null) continue;
                com.newuniverse.nusmp.perf.PerfParticles.send(level, type, p.getX(), p.getY() + 1.2, p.getZ(), COUNT, 4.5, 1.8, 4.5, 0.02);   // 1.99.0: per-tick trail: budgeted (perf.PerfParticles)
            }
        } catch (RuntimeException ignored) {
            // an ambient particle must never disturb the tick
        }
    }

    /**
     * The mood sound of the theme: once in {@code every} pulses (a different pulse for every player, so a party does not hear it in unison) one vanilla ambience,
     * played five to nine blocks from the player in a random direction, so it seems to come from the stone around them and not from their own head.
     */
    static void mood(ServerLevel level, ServerPlayer p, ThemeDef.Mood mood, int pulse) {
        if (mood == null || !mood.any() || (pulse + p.getId()) % Math.max(1, mood.every()) != 0) return;
        SoundEvent sound = sound(mood.sound());
        if (sound == null) return;
        RandomSource rng = level.getRandom();
        double ang = rng.nextDouble() * Math.PI * 2.0, dist = 5.0 + rng.nextDouble() * 4.0;
        level.playSound(null, p.getX() + Math.cos(ang) * dist, p.getY() + 1.0, p.getZ() + Math.sin(ang) * dist, sound, SoundSource.AMBIENT, mood.volume(), mood.pitch());
    }

    /** The sound event with this id, or null (unknown id: an optional pack, a typo). */
    static SoundEvent sound(String id) {
        if (id == null || id.isEmpty()) return null;
        return SOUNDS.computeIfAbsent(id, key -> {
            ResourceLocation rl = ResourceLocation.tryParse(key);
            return rl == null ? Optional.<SoundEvent>empty() : BuiltInRegistries.SOUND_EVENT.getOptional(rl);
        }).orElse(null);
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
