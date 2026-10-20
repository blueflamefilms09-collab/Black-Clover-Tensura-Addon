package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Why: the pockets name sounds by string ({@code nusmp:rift.crackle}, {@code nusmp:pocket.ember_sanctum.ambient}) because the mod's sound engine is another builder's, filled in as the audio is made;
 * a string is resolved at the moment of use, and when the registry does not know it yet the vanilla fallback plays, so the tombs are never silent and never crash on a sound that is not there.
 * The strings the data uses are listed in docs/labyrinth/rift_pockets.md for the audio builder.
 */
public final class SoftSound {
    private SoftSound() {}

    /** The vanilla sounds that stand in for the mod's own while they are absent. */
    public static final SoundEvent CRACKLE_FALLBACK = SoundEvents.AMETHYST_BLOCK_RESONATE;

    /** The sound event of a registry id, or the fallback when the id is unknown or malformed. */
    public static SoundEvent resolve(String id, SoundEvent fallback) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        if (rl != null) {
            var found = BuiltInRegistries.SOUND_EVENT.getOptional(rl);
            if (found.isPresent()) return found.get();
        }
        return fallback;
    }

    /** Plays a sound by id (or its fallback) at a block for everyone near. */
    public static void play(ServerLevel level, BlockPos at, String id, SoundEvent fallback, SoundSource source, float volume, float pitch) {
        if (level == null || at == null) return;
        SoundEvent e = resolve(id, fallback);
        if (e != null) level.playSound(null, at, e, source, volume, pitch);
    }

    /** Plays a sound by vanilla id (a string of the element table) at a block. */
    public static void playVanilla(ServerLevel level, BlockPos at, String id, float volume, float pitch) { play(level, at, id, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, volume, pitch); }

    /** True when the registry knows this sound id (the self-test reports how many of the pocket sounds are already real). */
    public static boolean known(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.SOUND_EVENT.containsKey(rl);
    }

    /** A holder is not needed by callers; kept so the registry lookup stays in one place. */
    static Holder<SoundEvent> holder(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.SOUND_EVENT.getHolder(rl).orElse(null);
    }
}
