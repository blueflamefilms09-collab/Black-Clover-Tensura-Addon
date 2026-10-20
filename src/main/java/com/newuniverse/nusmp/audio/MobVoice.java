package com.newuniverse.nusmp.audio;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * Why: the small game-side layer between a mob class and the catalogue. A mob class overrides {@code getAmbientSound}, {@code getHurtSound}, {@code getDeathSound},
 * {@code playStepSound} and {@code getVoicePitch} with {@link #event}, {@link #volume} and {@link #pitch}, and plays the cues vanilla has no hook for (the attack, the moment it
 * notices a target, the wind-up of a spell) with {@link #play}. Every sound goes out through the level so everyone in range hears it from the mob, and a silenced mob
 * ({@code Silent: 1}) stays silent. Which family a mob is comes from {@link #familyOf}: its own answer if it is {@link SoundFamilied}, else its entity type.
 */
public final class MobVoice {
    private MobVoice() {}

    public static SoundEvent event(String family, Cue cue) { return ModSounds.event(family, cue); }

    /** The ambient, hurt and death events of a family, for the three sound hooks of a mob class. */
    public static SoundEvent ambient(String family) { return event(family, Cue.AMBIENT); }

    public static SoundEvent hurt(String family) { return event(family, Cue.HURT); }

    public static SoundEvent death(String family) { return event(family, Cue.DEATH); }

    public static float volume(String family, Cue cue) { return MobSoundSet.family(family).volume(cue); }

    /** A random pitch of the family's range for the cue. */
    public static float pitch(String family, Cue cue, RandomSource random) { return MobSoundSet.pitch(family, cue, random.nextDouble()); }

    /** The family of an entity, or null when it is not a mob the catalogue knows. */
    public static String familyOf(Entity e) {
        if (e instanceof SoundFamilied sf) return sf.soundFamily();
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key == null ? null : MobSoundSet.forEntityType(key.toString());
    }

    /** Plays a cue of a family from this entity (server side only; a silent entity plays nothing). */
    public static void play(Entity e, String family, Cue cue) {
        if (e.level().isClientSide || e.isSilent()) return;
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), event(family, cue), e.getSoundSource(), volume(family, cue), pitch(family, cue, e.getRandom()));
    }
}
