package com.newuniverse.nusmp.undine;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Why: how the Undine's sounds get to the players, all on the server (the client only plays what it is sent). A sound goes through the budget ({@link UndineVoiceBudget}: a per-cue cooldown per owner, a
 * short gap between any two, a cap per second across the server) and is then sent as one sound packet to the players within the cue's range only ({@link UndineRules#FX_RADIUS} at most), never to the whole
 * server. Nothing here scans the world: it asks the player list for the players near a point, which the server does for every vanilla sound anyway.
 */
public final class UndineAudio {
    private UndineAudio() {}

    static final UndineVoiceBudget BUDGET = new UndineVoiceBudget();

    /** The least ticks between two of one cue for one owner. */
    public static long cooldown(UndineSoundSet.Cue cue) {
        return switch (cue) {
            case AMBIENT -> 200;
            case LINK, UNLINK, DEATH -> 20;
            case CAST -> 8;
            case FULL, CRASH -> 40;
            case HURT -> 10;
        };
    }

    /** The radius (blocks) a cue is sent to: its own range, never more than the fx radius. */
    public static double radius(UndineSoundSet.Cue cue) { return Math.min(UndineRules.FX_RADIUS, cue.range()); }

    /**
     * Plays a cue at a point for the owner's Undine, if the budget allows. {@code full} is the big form (louder, lower). Returns true when the packet went out.
     */
    public static boolean play(ServerLevel sl, UUID owner, double x, double y, double z, UndineSoundSet.Cue cue, boolean full) {
        if (!UndineConfig.enabled() || owner == null) return false;
        if (!BUDGET.tryAcquire(owner, cue.id(), sl.getGameTime(), cooldown(cue), UndineConfig.soundsPerSecond())) return false;
        float pitch = cue.pitch(sl.random.nextDouble()) * UndineSoundSet.formPitch(full);
        float volume = UndineSoundSet.volume(cue, full);
        ClientboundSoundPacket packet = new ClientboundSoundPacket(UndineSounds.holder(cue), SoundSource.NEUTRAL, x, y, z, volume, pitch, sl.random.nextLong());
        sl.getServer().getPlayerList().broadcast(null, x, y, z, radius(cue), sl.dimension(), packet);
        return true;
    }

    /** A cue at an entity's position (the Spirit Lord's own body). */
    public static boolean play(LivingEntity at, UUID owner, UndineSoundSet.Cue cue, boolean full) {
        return at.level() instanceof ServerLevel sl && play(sl, owner, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), cue, full);
    }

    public static void forget(UUID owner) { BUDGET.forget(owner); }

    public static void clearAll() { BUDGET.clear(); }
}
