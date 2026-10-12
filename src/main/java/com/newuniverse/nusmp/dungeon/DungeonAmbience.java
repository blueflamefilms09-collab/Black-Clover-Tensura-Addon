package com.newuniverse.nusmp.dungeon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The sounds of the Living Dungeon (docs/living_dungeon.md section 7): now and then, at a gap that depends on the tier, one ambient sound comes
 * from somewhere 12 to 28 blocks from each player in it, and more rarely a Direwolf or Basilisk awake within 40 blocks growls from where it is.
 * What each tier plays is the table of {@link DungeonAtmosphere}; the sounds are vanilla ones. The timers live only in memory.
 */
public final class DungeonAmbience {
    private DungeonAmbience() {}

    /** A timer set further ahead than any gap we use is left over from another world (the game time starts again): it is dropped. */
    private static final long STALE_TICKS = 4000L;
    private static final Map<UUID, Long> NEXT_SOUND = new HashMap<>(), NEXT_GROWL = new HashMap<>();
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return;
        ServerLevel dungeon = server.getLevel(LivingDungeon.DIMENSION);
        if (dungeon == null) return;
        List<ServerPlayer> players = dungeon.players();
        long now = dungeon.getGameTime();
        if (now % 100 == 0) forgetAbsent(players);
        RandomSource random = dungeon.getRandom();
        for (ServerPlayer p : players) {
            if (!p.isAlive()) continue;
            ambient(dungeon, p, now, random);
            growl(dungeon, p, now, random);
        }
    }

    /** The timers of players who left the Dungeon go, so a return starts quietly again. */
    private static void forgetAbsent(List<ServerPlayer> inside) {
        Set<UUID> here = new HashSet<>();
        for (ServerPlayer p : inside) here.add(p.getUUID());
        NEXT_SOUND.keySet().retainAll(here);
        NEXT_GROWL.keySet().retainAll(here);
    }

    // ---------------------------------------------------------------- the tier's own sound
    private static void ambient(ServerLevel level, ServerPlayer p, long now, RandomSource random) {
        Long due = NEXT_SOUND.get(p.getUUID());
        if (due == null || due - now > STALE_TICKS) {
            NEXT_SOUND.put(p.getUUID(), now + DungeonAtmosphere.between(DungeonAtmosphere.FIRST_SOUND_MIN_TICKS, DungeonAtmosphere.FIRST_SOUND_MAX_TICKS, random.nextLong()));
            return;
        }
        if (now < due) return;
        DungeonAtmosphere.Mood mood = DungeonAtmosphere.mood(DungeonTier.at(level, p.blockPosition()).ordinal());
        NEXT_SOUND.put(p.getUUID(), now + DungeonAtmosphere.soundInterval(mood, random.nextLong()));
        DungeonAtmosphere.Sound sound = DungeonAtmosphere.pick(mood.sounds(), random.nextLong());
        if (sound == null) return;
        double[] ring = DungeonAtmosphere.ring(random.nextLong(), DungeonAtmosphere.SOUND_MIN_DISTANCE, DungeonAtmosphere.SOUND_MAX_DISTANCE);
        double y = p.getY() + 1.0 + (random.nextDouble() * 6.0 - 3.0);
        play(level, sound, p.getX() + ring[0], y, p.getZ() + ring[1], ring[2], random);
    }

    // ---------------------------------------------------------------- a predator, far off in the dark
    private static void growl(ServerLevel level, ServerPlayer p, long now, RandomSource random) {
        Long due = NEXT_GROWL.get(p.getUUID());
        if (due == null || due - now > STALE_TICKS) {
            NEXT_GROWL.put(p.getUUID(), now + DungeonAtmosphere.between(DungeonAtmosphere.GROWL_RECHECK_MIN_TICKS, DungeonAtmosphere.GROWL_RECHECK_MAX_TICKS, random.nextLong()));
            return;
        }
        if (now < due) return;
        DungeonCreature source = nearestPredator(level, p);
        if (source == null) {
            NEXT_GROWL.put(p.getUUID(), now + DungeonAtmosphere.between(DungeonAtmosphere.GROWL_RECHECK_MIN_TICKS, DungeonAtmosphere.GROWL_RECHECK_MAX_TICKS, random.nextLong()));
            return;
        }
        NEXT_GROWL.put(p.getUUID(), now + DungeonAtmosphere.between(DungeonAtmosphere.GROWL_MIN_TICKS, DungeonAtmosphere.GROWL_MAX_TICKS, random.nextLong()));
        DungeonAtmosphere.Sound sound = DungeonAtmosphere.pick(source instanceof BasiliskEntity ? DungeonAtmosphere.BASILISK_GROWLS : DungeonAtmosphere.DIREWOLF_GROWLS, random.nextLong());
        if (sound == null) return;
        play(level, sound, source.getX(), source.getY() + 0.5, source.getZ(), Math.sqrt(source.distanceToSqr(p)), random);
    }

    /** The nearest awake Direwolf or Basilisk that is within hearing but out of sight: not nearer than 12 blocks, not farther than 40. */
    private static DungeonCreature nearestPredator(ServerLevel level, ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(DungeonAtmosphere.GROWL_RANGE);
        double least = (double) DungeonAtmosphere.GROWL_MIN_DISTANCE * DungeonAtmosphere.GROWL_MIN_DISTANCE, most = (double) DungeonAtmosphere.GROWL_RANGE * DungeonAtmosphere.GROWL_RANGE;
        DungeonCreature best = null;
        double bestD = Double.MAX_VALUE;
        for (DungeonCreature c : level.getEntitiesOfClass(DungeonCreature.class, box, m -> (m instanceof DirewolfEntity || m instanceof BasiliskEntity) && m.isAlive() && !m.asleep())) {
            double d = c.distanceToSqr(p);
            if (d >= least && d <= most && d < bestD) {
                best = c;
                bestD = d;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- playing it
    private static void play(ServerLevel level, DungeonAtmosphere.Sound sound, double x, double y, double z, double distance, RandomSource random) {
        SoundEvent event = SOUNDS.computeIfAbsent(sound.id(), id -> BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse(id)).orElse(null));
        if (event == null) return;
        float volume = DungeonAtmosphere.audibleVolume(DungeonAtmosphere.between(sound.volumeMin(), sound.volumeMax(), random.nextLong()), distance);
        float pitch = DungeonAtmosphere.between(sound.pitchMin(), sound.pitchMax(), random.nextLong());
        level.playSound(null, x, y, z, event, SoundSource.AMBIENT, volume, pitch);
    }
}
