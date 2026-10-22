package com.newuniverse.nusmp.sylph;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.audio.voice.VoiceLimiter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Sylph's own sound set ({@link SylphSoundSet}) as sound events, and the one way she makes a sound. The events are registered in a loop from the catalogue, so the registry, the
 * catalogue, {@code sounds.json} and the files cannot drift apart (a fifth register next to the mob, voice and music ones, so none of the files other builders append to is touched).
 *
 * <p>Sounds are SERVER driven and go to NEARBY players only: {@link #play} walks the level's players, skips those further than {@link SylphRules#SOUND_RADIUS}, asks the per listener
 * {@link VoiceLimiter} (the same limiter the spoken lines use: a cooldown per cue, at most {@link SylphSoundSet#MAX_PER_SECOND} Sylph sounds a second, never two on top of each other) and
 * sends the allowed ones a sound packet. Nothing is sent to a far player, nothing is played twice in a row, a crowd of Sylphs cannot flood one listener.</p>
 */
public final class SylphSounds {
    private SylphSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NUSMP.MODID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new LinkedHashMap<>();

    static {
        for (SylphSoundSet.Cue c : SylphSoundSet.cues()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, c.event());
            EVENTS.put(c.id(), SOUNDS.register(c.event(), () -> SoundEvent.createVariableRangeEvent(rl)));
        }
    }

    /** Every registered event by cue id, in catalogue order. */
    public static Map<String, DeferredHolder<SoundEvent, SoundEvent>> all() { return Collections.unmodifiableMap(EVENTS); }

    /** The event of a cue, or null for an id that is not in the catalogue. */
    public static SoundEvent event(String cue) {
        DeferredHolder<SoundEvent, SoundEvent> h = EVENTS.get(cue);
        return h == null ? null : h.get();
    }

    /** The per listener limiter (server thread only; keyed by the listener's UUID, dropped on logout). */
    static final VoiceLimiter LIMITER = new VoiceLimiter(0);

    /**
     * Plays a cue at a point to the players near it, through the limiter. Returns how many players were sent it. {@code level} must be a server level; a cue that is not in the catalogue plays
     * nothing.
     */
    public static int play(ServerLevel level, double x, double y, double z, String cue) {
        SylphSoundSet.Cue c = SylphSoundSet.cue(cue);
        SoundEvent ev = c == null ? null : event(cue);
        if (ev == null) return 0;
        long now = level.getGameTime();
        double r2 = SylphRules.SOUND_RADIUS * SylphRules.SOUND_RADIUS;
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ev);
        float pitch = SylphSoundSet.pitch(c, level.getRandom().nextDouble());
        long seed = level.getRandom().nextLong();
        int sent = 0;
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
            if (dx * dx + dy * dy + dz * dz > r2) continue;
            if (!LIMITER.tryAcquire(p.getUUID(), c.id(), now, SylphSoundSet.MAX_PER_SECOND, c.cooldown())) continue;
            p.connection.send(new ClientboundSoundPacket(holder, SoundSource.NEUTRAL, x, y, z, c.volume(), pitch, seed));
            sent++;
        }
        return sent;
    }

    /** A listener is gone (logout): its limiter record goes. */
    public static void forget(UUID listener) { LIMITER.forget(listener); }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() { LIMITER.clear(); }
}
