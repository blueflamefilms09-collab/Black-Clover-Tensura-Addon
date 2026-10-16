package com.newuniverse.nusmp.song.voice;

import com.newuniverse.nusmp.song.SingingState;
import com.newuniverse.nusmp.song.SongConfig;
import com.newuniverse.nusmp.song.SongMath;
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
 * Why: Song Magic is sung into a real microphone. Simple Voice Chat lets a server plugin see every microphone frame; this plugin
 * decodes the frame, measures how loud it is and writes only that number into {@link SingingState}. It runs on the voice thread, so it
 * never touches the game, and it is the ONLY class in the mod that names a voice-mod type: the mod loads and plays without Simple
 * Voice Chat, and this class is only ever instantiated by the voice mod itself (through the META-INF/services file).
 */
public final class SongVoicePlugin implements VoicechatPlugin {
    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private volatile VoicechatApi api;

    @Override public String getPluginId() { return "nusmp_song"; }

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
            MicrophonePacket packet = event.getPacket();
            if (packet.isWhispering() && !SongConfig.whisperCounts()) { SingingState.feedRms(id, 0f, System.currentTimeMillis()); return; }
            byte[] opus = packet.getOpusEncodedData();
            if (opus == null || opus.length == 0) {                      // an empty frame ends a transmission: silence
                SingingState.feedRms(id, 0f, System.currentTimeMillis());
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
            SingingState.feedRms(id, SongMath.rms(pcm), System.currentTimeMillis());
        } catch (RuntimeException ignored) {
            // a bad frame must never disturb the voice thread
        }
    }

    private void onDisconnected(PlayerDisconnectedEvent event) {
        UUID id = event.getPlayerUuid();
        OpusDecoder d = decoders.remove(id);
        if (d != null) synchronized (d) { if (!d.isClosed()) d.close(); }
        SingingState.clear(id);
    }

    private void onStopped(VoicechatServerStoppedEvent event) {
        for (OpusDecoder d : decoders.values()) synchronized (d) { if (!d.isClosed()) d.close(); }
        decoders.clear();
        SingingState.clearAll();
    }
}
