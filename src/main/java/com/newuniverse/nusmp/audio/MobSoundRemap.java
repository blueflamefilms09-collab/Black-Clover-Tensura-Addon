package com.newuniverse.nusmp.audio;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

import java.util.List;

/**
 * Why: the mobs of the dungeon that are NOT this mod's own classes (the stock zombies, skeletons, spiders, slimes, pillagers, wolves and the rest that the generator and the
 * encounter tables put into the rooms, and the vanilla voice sounds the Foundry's ability rows name, such as {@code entity.ravager.roar} or {@code entity.evoker.cast_spell}) cannot
 * have their sound methods overridden. So the server turns their voices into the bespoke ones on the way out: when a sound event of the stock hostile mobs is about to be played
 * ({@link PlayLevelSoundEvent}, both the "at an entity" form that {@code makeSound} uses for ambient, hurt and death, and the "at a position" form that steps and ability sounds use),
 * in a dungeon level, and the mob that made it is found (the entity itself, or the one standing at that exact spot), the event is replaced by the family's cue and its pitch drawn from
 * the family's range. The family comes from the mob that made the sound, not from the vanilla sound: a Foundry kobold that plays {@code entity.wolf.growl} sounds like a kobold. Outside the
 * dungeon nothing changes, and a vanilla sound that is not a creature's voice (a bow, a door, an item) is never touched. See {@code docs/audio/mob_audio.md}.
 */
public final class MobSoundRemap {
    private MobSoundRemap() {}

    /** Registers the two listeners on the game bus. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener(MobSoundRemap::onAtPosition);
        gameBus.addListener(MobSoundRemap::onAtEntity);
    }

    /** Is this a world whose mobs get the bespoke voices: the Labyrinth dimensions and every other dimension of this mod's own except the Sanctuary Hub (rifts and pocket dungeons included). */
    public static boolean dungeonLevel(Level level) {
        if (level == null) return false;
        if (Labyrinth.in(level)) return true;
        return !Labyrinth.inSanctuary(level) && NUSMP.MODID.equals(level.dimension().location().getNamespace());
    }

    private static MobSoundSet.Voice voiceOf(Holder<SoundEvent> sound) {
        if (sound == null) return null;
        var key = sound.unwrapKey();
        if (key.isEmpty()) return null;
        ResourceLocation rl = key.get().location();
        return "minecraft".equals(rl.getNamespace()) ? MobSoundSet.parseVanillaVoice(rl.getPath()) : null;
    }

    public static void onAtPosition(PlayLevelSoundEvent.AtPosition e) {
        Level level = e.getLevel();
        if (level == null || level.isClientSide() || !dungeonLevel(level)) return;
        MobSoundSet.Voice v = voiceOf(e.getSound());
        if (v == null) return;
        Mob m = mobAt(level, e.getPosition());
        if (m != null) replace(e, m, v.cue());
    }

    public static void onAtEntity(PlayLevelSoundEvent.AtEntity e) {
        Entity who = e.getEntity();
        if (!(who instanceof Mob m) || who.level().isClientSide() || !dungeonLevel(who.level())) return;
        MobSoundSet.Voice v = voiceOf(e.getSound());
        if (v != null) replace(e, m, v.cue());
    }

    /** The mob standing exactly where a sound was played (an entity plays its sounds at its own position), or null. */
    static Mob mobAt(Level level, Vec3 at) {
        List<Mob> near = level.getEntitiesOfClass(Mob.class, new AABB(at.x - 0.4, at.y - 0.4, at.z - 0.4, at.x + 0.4, at.y + 0.4, at.z + 0.4));
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob m : near) {
            double d = m.distanceToSqr(at);
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    private static void replace(PlayLevelSoundEvent e, Mob m, Cue cue) {
        String family = MobVoice.familyOf(m);
        if (family == null) return;
        e.setSound(ModSounds.holder(family, cue));
        float vol = MobVoice.volume(family, cue);
        e.setNewVolume(cue == Cue.STEP ? vol : vol * Math.min(1.0f, e.getOriginalVolume()));
        e.setNewPitch(MobVoice.pitch(m, family, cue));
    }
}
