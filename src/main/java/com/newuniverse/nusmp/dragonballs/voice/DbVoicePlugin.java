package com.newuniverse.nusmp.dragonballs.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the Dragon Ball words can be said into a real microphone. Simple Voice Chat lets a server plugin see every microphone frame. This plugin runs on the voice thread, never touches the game, and does
 * nothing for a player that is not being listened to ({@link DbVoice#listening}, a volatile set the server fills): such frames are dropped before they are decoded. For a listened-to player it decodes the
 * Opus frame (one decoder per player, closed on disconnect and on stop) and passes the PCM to {@link DbVoice#feed}. It is the ONLY class of this package that names a voice-mod type: the mod loads and plays
 * without Simple Voice Chat, and this class is only ever instantiated by the voice mod itself (META-INF/services). Modelled on {@code song.voice.SongVoicePlugin}.
 */
public final class DbVoicePlugin implements VoicechatPlugin {
    private static final int RATE = 48000;                          // Simple Voice Chat always sends 48 kHz mono
    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private volatile VoicechatApi api;

    @Override public String getPluginId() { return "nusmp_dragonballs"; }

    @Override public void initialize(VoicechatApi api) { this.api = api; }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onDisconnected);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onStopped);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        try {
            VoicechatConnection from = event.getSenderConnection();
            if (from == null || from.getPlayer() == null || api == null) return;
            UUID id = from.getPlayer().getUuid();
            if (!DbVoice.listening(id)) {                              // nobody listens to this player: no decoding at all
                if (!decoders.isEmpty()) closeDecoder(id);
                return;
            }
            MicrophonePacket packet = event.getPacket();
            if (!DbVoice.whisperOk(packet.isWhispering())) { DbVoice.endTransmission(id); return; }
            byte[] opus = packet.getOpusEncodedData();
            if (opus == null || opus.length == 0) {                    // an empty frame ends a transmission
                DbVoice.endTransmission(id);
                OpusDecoder d = decoders.get(id);
                if (d != null) synchronized (d) { d.resetState(); }
                return;
            }
            OpusDecoder decoder = decoders.computeIfAbsent(id, k -> api.createDecoder());
            short[] pcm;
            synchronized (decoder) {
                if (decoder.isClosed()) { decoders.remove(id, decoder); return; }
                pcm = decoder.decode(opus);
            }
            DbVoice.feed(id, pcm, RATE);
        } catch (RuntimeException ignored) {
            // a bad frame must never disturb the voice thread
        }
    }

    private void closeDecoder(UUID id) {
        OpusDecoder d = decoders.remove(id);
        if (d != null) synchronized (d) { if (!d.isClosed()) d.close(); }
    }

    private void onDisconnected(PlayerDisconnectedEvent event) {
        closeDecoder(event.getPlayerUuid());
        DbVoice.endTransmission(event.getPlayerUuid());
    }

    private void onStopped(VoicechatServerStoppedEvent event) {
        for (OpusDecoder d : decoders.values()) synchronized (d) { if (!d.isClosed()) d.close(); }
        decoders.clear();
    }
}
