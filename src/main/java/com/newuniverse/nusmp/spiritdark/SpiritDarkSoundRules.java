package com.newuniverse.nusmp.spiritdark;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the sounds of the Spirit of Darkness are played by the SERVER to nearby players only, so they must be cheap on a crowded server. This pure class (no game classes, ticks are passed in)
 * holds the cue table (each cue's sound event path, how far it carries, its volume and pitch range, the shortest gap between two of the same cue for one source) and the gate that enforces it:
 * a per-source gap (the source is a player's UUID, never a static "current player"), plus a server-wide allowance per tick ({@link #PER_TICK}) so a crowd of casters cannot send hundreds of sound
 * packets in one tick. {@link #forget} drops a source (logout, death, dimension change).
 */
public final class SpiritDarkSoundRules {
    private SpiritDarkSoundRules() {}

    /** The cues; the event path is {@code spirit.dark.<cue>} and the files are {@code sounds/spirit/dark/<cue>_<n>.ogg} (tools/gen_spirit_dark_sounds.py). */
    public enum Cue {
        WHISPER("whisper", 18, 0.55f, 0.9f, 1.05f, 300), CHIME("chime", 24, 0.7f, 0.92f, 1.08f, 40), FLUTTER("flutter", 20, 0.6f, 0.9f, 1.15f, 70), STEP("step", 24, 0.8f, 0.95f, 1.1f, 20),
        ARRIVE("arrive", 24, 0.8f, 0.95f, 1.1f, 20), STRIKE("strike", 16, 0.6f, 0.9f, 1.15f, 14), UNLINK("unlink", 20, 0.6f, 0.95f, 1.05f, 40), POWERED("powered", 32, 0.9f, 0.95f, 1.05f, 60);

        public final String id;
        /** How far it carries (blocks; the same number as the sounds.json attenuation distance), the volume, the pitch range and the shortest gap (ticks) between two of this cue from one source. */
        public final int range;
        public final float volume, pitchLo, pitchHi;
        public final int gap;

        Cue(String id, int range, float volume, float lo, float hi, int gap) { this.id = id; this.range = range; this.volume = volume; this.pitchLo = lo; this.pitchHi = hi; this.gap = gap; }

        public String eventPath() { return "spirit.dark." + id; }
    }

    /** The most sounds the whole server may start in one tick for this feature. */
    public static final int PER_TICK = 6;

    /** The pitch for a cue given a unit random number in [0, 1). */
    public static float pitch(Cue c, float unit) { return c.pitchLo + (c.pitchHi - c.pitchLo) * Math.max(0f, Math.min(1f, unit)); }

    public static final class Gate {
        private final Map<UUID, long[]> last = new HashMap<>();
        private long tick = Long.MIN_VALUE;
        private int used;

        /** May this source play this cue at {@code now}? Counts it when yes. */
        public boolean tryPlay(UUID source, Cue c, long now) {
            if (source == null) return false;
            if (now != tick) { tick = now; used = 0; }
            if (used >= PER_TICK) return false;
            long[] a = last.computeIfAbsent(source, k -> fresh());
            if (now - a[c.ordinal()] < c.gap) return false;
            a[c.ordinal()] = now;
            used++;
            return true;
        }

        private static long[] fresh() {
            long[] a = new long[Cue.values().length];
            java.util.Arrays.fill(a, Long.MIN_VALUE / 4);
            return a;
        }

        public void forget(UUID source) { last.remove(source); }

        public void clear() { last.clear(); }

        public int tracked() { return last.size(); }
    }
}
