package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Why: a tabletop DM does not say "roll for initiative, there are six goblins"; he says "you hear something scrape in the dark". The Director
 * therefore never tells a party that an encounter was spawned (unless a table entry carries a message): it gives a CUE, a sound that seems to
 * come from the direction of the monsters and a little dust where the party stands, and lets the players decide what that means. The cue is
 * data: an entry names a sound and a particle by registry id; an entry without one gets the default of its band (a gravel scrape for a minor
 * encounter, grinding stone for a standard one, the Warden's distant heartbeat for a hard one and its angry one for an ambush). Ids are looked
 * up at run time and an unknown id is skipped with one log line, so a cue naming another mod's sound never breaks an encounter. Only simple
 * particles are accepted (those that need options, like dust colours, cannot be written in a data file). The sound is placed twelve blocks from
 * the party's middle towards the first monster, so it has a direction; the dust is sprinkled round each member.
 */
final class EncounterCue {
    private EncounterCue() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();
    private static final Map<Band, EncounterTable.Cue> DEFAULTS = new EnumMap<>(Band.class);

    static {
        DEFAULTS.put(Band.MINOR, new EncounterTable.Cue("minecraft:block.gravel.break", 0.5f, 0.6f, "", 0));
        DEFAULTS.put(Band.STANDARD, new EncounterTable.Cue("minecraft:block.stone.break", 0.7f, 0.55f, "minecraft:smoke", 6));
        DEFAULTS.put(Band.HARD, new EncounterTable.Cue("minecraft:entity.warden.nearby_close", 0.8f, 0.8f, "minecraft:large_smoke", 8));
        DEFAULTS.put(Band.AMBUSH, new EncounterTable.Cue("minecraft:entity.warden.nearby_closer", 1.0f, 0.7f, "minecraft:poof", 16));
    }

    /** The cue an entry plays: its own when it wrote one, else its band's default. */
    static EncounterTable.Cue cueOf(EncounterTable.Entry entry, Band band) {
        if (entry.cue() != EncounterTable.Cue.NONE) return entry.cue();
        EncounterTable.Cue d = DEFAULTS.get(band);
        return d == null ? EncounterTable.Cue.NONE : d;
    }

    /** Plays the cue to the party: the sound towards {@code towards} (a cell, or null), the dust round every member. */
    static void play(ServerLevel level, List<? extends ServerPlayer> party, SpawnPicker.Cell towards, EncounterTable.Entry entry, Band band, Random rnd) {
        if (party.isEmpty()) return;
        EncounterTable.Cue cue = cueOf(entry, band);
        double cx = 0, cy = 0, cz = 0;
        for (ServerPlayer p : party) {
            cx += p.getX();
            cy += p.getY();
            cz += p.getZ();
        }
        cx /= party.size();
        cy /= party.size();
        cz /= party.size();
        SoundEvent sound = cue.sound().isEmpty() ? null : sound(cue.sound());
        if (sound != null) {
            double sx = cx, sy = cy + 1.0, sz = cz;
            if (towards != null) {
                double dx = towards.x() + 0.5 - cx, dz = towards.z() + 0.5 - cz, len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1.0) {
                    double reach = Math.min(12.0, len * 0.5);
                    sx += dx / len * reach;
                    sz += dz / len * reach;
                }
            }
            level.playSound(null, sx, sy, sz, sound, SoundSource.HOSTILE, cue.volume(), cue.pitch());
        }
        SimpleParticleType particle = cue.particle().isEmpty() || cue.particles() <= 0 ? null : particle(cue.particle());
        if (particle != null) {
            int each = Math.max(1, Math.min(24, cue.particles()));
            for (ServerPlayer p : party) level.sendParticles(particle, p.getX() + (rnd.nextDouble() - 0.5) * 4.0, p.getY() + 1.6, p.getZ() + (rnd.nextDouble() - 0.5) * 4.0, each, 1.2, 0.6, 1.2, 0.01);
        }
    }

    /**
     * One line on the action bar of a player. A player without a connection (the stand-in of the CI smoke test) cannot be told anything, and that
     * must not undo an encounter whose monsters are already placed, so a failure here is swallowed.
     */
    static void tell(ServerPlayer p, String translationKey) {
        try {
            p.displayClientMessage(Component.translatable(translationKey), true);
        } catch (RuntimeException unreachable) {
            // nobody to tell
        }
    }

    private static SoundEvent sound(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Optional<SoundEvent> s = rl == null ? Optional.empty() : BuiltInRegistries.SOUND_EVENT.getOptional(rl);
        if (s.isEmpty() && WARNED.add("sound:" + id)) LOG.warn("[nusmp] An encounter cue names the unknown sound {}", id);
        return s.orElse(null);
    }

    private static SimpleParticleType particle(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Object p = rl == null ? null : BuiltInRegistries.PARTICLE_TYPE.getOptional(rl).orElse(null);
        if (p instanceof SimpleParticleType simple) return simple;
        if (WARNED.add("particle:" + id)) LOG.warn("[nusmp] An encounter cue names the unknown or option-bearing particle {}", id);
        return null;
    }
}
