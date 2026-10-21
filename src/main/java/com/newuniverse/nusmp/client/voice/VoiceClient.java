package com.newuniverse.nusmp.client.voice;

import com.newuniverse.nusmp.audio.voice.VoiceCatalog;
import com.newuniverse.nusmp.audio.voice.VoiceSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * Why: the client half of the spoken lines. The server sends the id of a line ({@code audio/voice/VoiceNet}); this plays it ONCE as a flat, in-the-head sound in the VOICE category (so
 * the Voice slider and the Master slider scale it with no code of ours), times the {@code voice.volume} option, and not at all when {@code voice.enabled} is off, when the volume is zero,
 * or when the Voice slider is at zero (no file is streamed that nobody can hear). A new line replaces the one still playing, so two announcers never talk over each other. Nothing is decided
 * here: the server chose the line, the limits and the audience. Client only; started from the client block of the mod class.
 */
public final class VoiceClient {
    private VoiceClient() {}

    private static SoundInstance current;

    /** Client block of the mod class: registers {@code nusmp-voice-client.toml}. */
    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, VoiceClientConfig.SPEC, "nusmp-voice-client.toml");
    }

    /** Plays a line (called on the client thread by the payload handler). Never throws. */
    public static void play(String line) {
        try {
            if (!VoiceCatalog.has(line) || !VoiceClientConfig.enabled()) return;
            float volume = VoiceClientConfig.volume();
            Minecraft mc = Minecraft.getInstance();
            if (volume <= 0.0f || mc.level == null || mc.player == null) return;
            if (mc.options.getSoundSourceVolume(SoundSource.VOICE) <= 0.0f) return;
            SoundEvent event = VoiceSounds.event(line);
            if (event == null) return;
            if (current != null) mc.getSoundManager().stop(current);
            current = new Line(event, volume);
            mc.getSoundManager().play(current);
        } catch (RuntimeException ignored) {
            // a sound that cannot play must never break the game
        }
    }

    /** One spoken line: VOICE category, relative, no attenuation. */
    private static final class Line extends AbstractSoundInstance {
        Line(SoundEvent event, float volume) {
            super(event, SoundSource.VOICE, SoundInstance.createUnseededRandom());
            this.volume = volume;
            this.pitch = 1.0f;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.looping = false;
            this.delay = 0;
        }
    }
}
