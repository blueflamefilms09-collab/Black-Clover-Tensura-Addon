package net.minecraft.client.resources.sounds;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/** Why: harness shim. */
public class SimpleSoundInstance implements SoundInstance {
    public final String name; public final float pitch;
    private SimpleSoundInstance(String n, float p) { name = n; pitch = p; }
    public static SimpleSoundInstance forUI(Holder<SoundEvent> ev, float pitch) { return new SimpleSoundInstance(ev.value().name, pitch); }
    public static SimpleSoundInstance forUI(SoundEvent ev, float pitch) { return new SimpleSoundInstance(ev.name, pitch); }
}
