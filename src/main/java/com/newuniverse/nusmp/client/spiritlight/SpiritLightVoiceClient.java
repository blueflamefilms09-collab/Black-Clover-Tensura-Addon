package com.newuniverse.nusmp.client.spiritlight;

import com.newuniverse.nusmp.client.voice.VoiceClientConfig;
import com.newuniverse.nusmp.spiritlight.SpiritLightSounds;
import com.newuniverse.nusmp.spiritlight.SpiritLightVoiceLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * Why: the client half of the seraph's spoken lines. The server sends the id of a line and the entity id of its speaker ({@code spiritlight.SpiritLightNet.Say}) to the players within 32 blocks only;
 * this plays it ONCE in the VOICE category (so the Voice slider and the Master slider scale it with no code of ours), from where the speaker is (the voice comes from the seraph, behind its owner), times
 * the mod's {@code voice.volume} option, and not at all when {@code voice.enabled} is off, when the volume is zero or when the Voice slider is at zero (no file is streamed that nobody can hear). A new
 * line replaces the one still playing, so the seraph never talks over itself. Nothing is decided here: the server chose the line, the limits and the audience. Client only.
 */
public final class SpiritLightVoiceClient {
    private SpiritLightVoiceClient() {}

    private static SoundInstance current;

    /** Plays a line (called on the client thread by the payload handler). Never throws. */
    public static void play(String line, int speaker) {
        try {
            if (!SpiritLightVoiceLines.has(line) || !VoiceClientConfig.enabled()) return;
            float volume = VoiceClientConfig.volume();
            Minecraft mc = Minecraft.getInstance();
            if (volume <= 0.0f || mc.level == null || mc.player == null) return;
            if (mc.options.getSoundSourceVolume(SoundSource.VOICE) <= 0.0f) return;
            SoundEvent event = SpiritLightSounds.voice(line);
            if (event == null) return;
            Entity who = mc.level.getEntity(speaker);
            if (who == null) who = mc.player;
            if (current != null) mc.getSoundManager().stop(current);
            current = new Line(event, volume, who.getX(), who.getY() + 1.4, who.getZ());
            mc.getSoundManager().play(current);
        } catch (RuntimeException ignored) {
            // a sound that cannot play must never break the game
        }
    }

    /** One spoken line: VOICE category, at the speaker, a linear fall-off over the distance of its sounds.json entry (32 blocks). */
    private static final class Line extends AbstractSoundInstance {
        Line(SoundEvent event, float volume, double x, double y, double z) {
            super(event, SoundSource.VOICE, SoundInstance.createUnseededRandom());
            this.volume = volume;
            this.pitch = 1.0f;
            this.x = x;
            this.y = y;
            this.z = z;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.looping = false;
            this.delay = 0;
        }
    }
}
