package com.newuniverse.nusmp.client.music;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Why: one playing loop of the themed music. It plays in the MUSIC sound category (so the vanilla Music slider, and the "master" slider, scale it with no code of ours), is
 * RELATIVE with no attenuation (it is in the head, not in the world: it does not get quieter with distance or pan with the camera), loops, and may start silent (a fade-in starts at
 * volume 0, which the sound engine would otherwise refuse to start). The engine sets the target volume every tick ({@link #setLevel}); a tickable instance has its volume re-read by
 * the sound engine every tick, which is what makes the cross-fade smooth. {@link #finish} marks it stopped so the engine releases the channel.
 */
public final class ThemedMusicInstance extends AbstractTickableSoundInstance {
    private final long startedAt;

    public ThemedMusicInstance(SoundEvent event, long now) {
        super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0001f;
        this.pitch = 1.0f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.startedAt = now;
    }

    @Override
    public void tick() {
        // the volume is set from outside, once per client tick
    }

    @Override
    public boolean canStartSilent() { return true; }

    /** Sets the volume the sound engine will read on its next update (0 to 1). */
    public void setLevel(float level) { this.volume = Math.max(0.0001f, Math.min(1.0f, level)); }

    /** The client tick this instance was created on (the engine checks it is really playing a little later). */
    public long startedAt() { return startedAt; }

    /** Stops the loop. */
    public void finish() { stop(); }
}
