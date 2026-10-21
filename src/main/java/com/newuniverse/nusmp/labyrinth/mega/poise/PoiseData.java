package com.newuniverse.nusmp.labyrinth.mega.poise;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

/**
 * Why: the attached data of a poise bearer ({@code nusmp:mega_poise}, a NeoForge entity attachment): which kind of bearer it is, its bar, whether it is desperate and how often its bar
 * broke. It is attached only to bearers (a plain zombie carries nothing, so the saved world does not grow), saved with the mob (a captain that comes back from a saved chunk keeps its
 * bar and its desperation), and the timers of the bar (armor, stagger) are deliberately not saved: a mob that reloads comes back standing. The codec is a compound so a field added later
 * does not break an old save.
 */
public final class PoiseData {
    public static final Codec<PoiseData> CODEC = CompoundTag.CODEC.xmap(PoiseData::fromTag, PoiseData::toTag);

    public PoiseBearers.Kind kind = PoiseBearers.Kind.NONE;
    public final PoiseState state = new PoiseState(PoiseTuning.DEFAULT.minMax());
    public boolean desperate;

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putString("kind", kind.name());
        t.putDouble("max", state.max());
        t.putDouble("poise", state.poise());
        t.putInt("breaks", state.breaks());
        t.putBoolean("desperate", desperate);
        return t;
    }

    public static PoiseData fromTag(CompoundTag t) {
        PoiseData d = new PoiseData();
        PoiseBearers.Kind k = PoiseBearers.Kind.NONE;
        try {
            k = PoiseBearers.Kind.valueOf(t.getString("kind"));
        } catch (IllegalArgumentException unknown) {
            // a kind written by a newer version: the mob is a plain one here
        }
        d.kind = k;
        d.state.restore(t.contains("max") ? t.getDouble("max") : PoiseTuning.DEFAULT.minMax(), t.contains("poise") ? t.getDouble("poise") : PoiseTuning.DEFAULT.minMax(), t.getInt("breaks"));
        d.desperate = t.getBoolean("desperate");
        return d;
    }
}
