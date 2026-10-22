package com.newuniverse.nusmp.undine;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Undine has a sound set of her own, so she never sounds like the basic Tensura spirit (which the owner asked for). This is the CATALOGUE, pure data with no Minecraft type so JUnit can pin
 * it: the cues, how many variants each has, their loudness, pitch range and how far they carry. {@link UndineSounds} registers one {@code SoundEvent} per cue ({@code nusmp:undine.ambient}...),
 * {@code tools/gen_undine_sounds.py} synthesises the {@code .ogg} files ({@code sounds/undine/<cue>_<n>.ogg}: flowing water, soft glass bells, a low watery choir hum and a wave crash, all
 * generated from nothing) and writes the entries of {@code sounds.json}; {@code UndineSoundsTest} keeps the three in step.
 */
public final class UndineSoundSet {
    private UndineSoundSet() {}

    /** The moments of her life that have a voice. */
    public enum Cue {
        /** The companion's idle: running water with a soft bell and a low hum, now and then. */
        AMBIENT("ambient", 3, 0.55f, 1.0f, 0.06f, 16, "flows and hums"),
        /** The link: she rises out of a swirl of water to the caster's side. */
        LINK("link", 2, 0.9f, 1.0f, 0.04f, 24, "rises from the water"),
        /** The link ends: the water falls back. */
        UNLINK("unlink", 2, 0.7f, 1.0f, 0.04f, 20, "sinks into the water"),
        /** A water spell leaves her palm: a swish and a glass chime. */
        CAST("cast", 3, 0.85f, 1.0f, 0.08f, 24, "casts a water spell"),
        /** The full form rises: a swell and a choir. */
        FULL("full", 2, 1.1f, 1.0f, 0.03f, 48, "rises as a wave"),
        /** The big wave crash that comes with the full form's spell. */
        CRASH("crash", 2, 1.2f, 1.0f, 0.05f, 56, "crashes as a wave"),
        /** The spirit lord's body is hurt. */
        HURT("hurt", 2, 0.8f, 1.0f, 0.06f, 20, "splashes in pain"),
        /** The spirit lord's body falls apart into water. */
        DEATH("death", 1, 1.0f, 1.0f, 0.03f, 24, "dissolves into water"),
        /** A soft sung phrase without words: what she says when a moment has no recorded spoken line. */
        HUM("hum", 3, 0.95f, 1.0f, 0.05f, 16, "hums a soft tune");

        private final String id;
        private final int variants;
        private final float volume, pitch, spread;
        private final int range;
        private final String verb;

        Cue(String id, int variants, float volume, float pitch, float spread, int range, String verb) {
            this.id = id; this.variants = variants; this.volume = volume; this.pitch = pitch; this.spread = spread; this.range = range; this.verb = verb;
        }

        public String id() { return id; }

        public int variants() { return variants; }

        public float volume() { return volume; }

        /** How far (blocks) the cue carries (the {@code attenuation_distance} of its sounds.json entry). */
        public int range() { return range; }

        /** The verb of the subtitle line: "Undine {verb}". */
        public String verb() { return verb; }

        /** The sound event path: {@code undine.ambient}. */
        public String event() { return "undine." + id; }

        /** The subtitle key ({@code tools/lang/undine.json} has the text). */
        public String subtitleKey() { return "subtitles.nusmp.undine." + id; }

        /** The resource name of variant {@code n} (1-based) without the extension: {@code nusmp:undine/ambient_1}. */
        public String file(int n) { return "nusmp:undine/" + id + "_" + n; }

        /** A random pitch for the cue, {@code r01} being a random number in [0, 1). */
        public float pitch(double r01) {
            float lo = pitch - spread, hi = pitch + spread;
            return (float) (lo + (hi - lo) * Math.max(0.0, Math.min(1.0, r01)));
        }

        public static Cue byId(String id) {
            for (Cue c : values()) if (c.id.equals(id)) return c;
            return null;
        }
    }

    /** Every event path in catalogue order. */
    public static List<String> allEventIds() {
        List<String> out = new ArrayList<>();
        for (Cue c : Cue.values()) out.add(c.event());
        return out;
    }

    /** Every sound file name (resource form, without extension) in catalogue order. */
    public static List<String> allFiles() {
        List<String> out = new ArrayList<>();
        for (Cue c : Cue.values()) for (int i = 1; i <= c.variants(); i++) out.add(c.file(i));
        return out;
    }

    /** The loudness of a cue for a form: the full form is a little louder (never above 1.4). */
    public static float volume(Cue cue, boolean full) { return Math.min(1.4f, cue.volume() * (full ? 1.25f : 1.0f)); }

    /** The pitch factor of a form: the full form sounds lower (she is bigger), by the 0.4th power of her size ratio (3.2 / 1.9 blocks). */
    public static float formPitch(boolean full) { return full ? 0.81f : 1.0f; }
}
