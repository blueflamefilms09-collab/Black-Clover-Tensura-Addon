package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SoundInstance;
import java.util.ArrayList;
import java.util.List;

/** Why: harness shim: remembers what would have been played. */
public class SoundManager {
    public static final List<String> PLAYED = new ArrayList<>();
    public void play(SoundInstance s) { PLAYED.add(s instanceof net.minecraft.client.resources.sounds.SimpleSoundInstance i ? i.name + "@" + String.format("%.2f", i.pitch) : "?"); }
}
