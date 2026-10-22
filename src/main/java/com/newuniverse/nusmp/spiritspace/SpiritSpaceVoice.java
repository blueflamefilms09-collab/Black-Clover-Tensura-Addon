package com.newuniverse.nusmp.spiritspace;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Spirit of Space has no words; it answers with small creature sounds (whispers, glassy chimes, soft murmurs, a curious chirp, a distress warble, a happy trill, a sigh), synthesised in
 * tools/gen_spirit_space_sounds.py. A cue goes to the players within {@link SpaceMoodRules#VOICE_RADIUS} blocks only, and each listener hears at most one every {@link SpaceMoodRules#VOICE_GAP_LISTENER}
 * ticks (three seconds). The server sends the one small sound packet to each qualifying listener itself (so the per-listener rule can be kept), after the owner's spirit passed the shared
 * {@link SoundGate} (a gap per owner and a cap per tick for the whole system). A cue costs one pass over the level's players, once per mood change (moods have their own cooldowns), never per tick.
 * Pure rules live in {@link SpaceMoodRules#hears}; this class only does the sending. Server thread only; the memory of who heard what is dropped with the listener.
 */
final class SpiritSpaceVoice {
    private SpiritSpaceVoice() {}

    private static final Map<UUID, Long> HEARD = new HashMap<>();

    /** Plays a cue at a place to the listeners near it. True when at least one listener got it. */
    static boolean say(ServerLevel sl, ServerPlayer owner, SpaceMoodRules.Tracker tracker, SpaceMoodRules.Voice voice, Vec3 at, float volume) {
        if (voice == SpaceMoodRules.Voice.NONE) return false;
        long now = sl.getGameTime();
        if (!tracker.voiceReady(now) || !SpiritSpaceSounds.GATE.allow(owner.getUUID(), SoundGate.Kind.VOICE, now)) return false;
        tracker.markVoice(now);
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SpiritSpaceSounds.voiceEvent(voice));
        float pitch = 0.94f + sl.random.nextFloat() * 0.12f;
        boolean any = false;
        for (ServerPlayer pl : sl.players()) {
            if (pl.connection == null) continue;                                              // a stand-in player of a self-test has no connection
            long last = HEARD.getOrDefault(pl.getUUID(), Long.MIN_VALUE / 4);
            if (!SpaceMoodRules.hears(pl.distanceToSqr(at), now, last)) continue;
            HEARD.put(pl.getUUID(), now);
            pl.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, at.x, at.y, at.z, volume, pitch, sl.random.nextLong()));
            any = true;
        }
        return any;
    }

    static void forgetListener(UUID id) { HEARD.remove(id); }

    static void clearAll() { HEARD.clear(); }
}
