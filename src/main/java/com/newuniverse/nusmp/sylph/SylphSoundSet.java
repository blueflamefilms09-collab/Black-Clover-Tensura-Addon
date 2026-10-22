package com.newuniverse.nusmp.sylph;

import java.util.List;

/**
 * Why: the Sylph must not sound like the basic Tensura Spirit Lord, so she has a sound set of her own: wind chimes, light wing flutter, a giggle that tinkles, and gust whooshes. This is the
 * CATALOGUE, pure data with no Minecraft type so JUnit can pin it: the cues, how many variants each has, their loudness, pitch range, cooldown per listener and the id of the sound event
 * ({@code nusmp:sylph.<cue>}), which {@link SylphSounds} registers one by one. {@code tools/gen_sylph_sounds.py} writes the {@code .ogg} files and the {@code sounds.json} entries from the same
 * table; {@code SylphSoundsTest} keeps the three in step (a cue without an entry plays nothing, an entry without a file logs an error, a file nothing names is dead weight).
 */
public final class SylphSoundSet {
    private SylphSoundSet() {}

    /**
     * One cue. {@code cooldown} is the least number of ticks between two plays of this cue for one listener, {@code volume} the playback volume, {@code pitch} and {@code spread} the pitch
     * centre and how far a random pitch may stray from it, {@code variants} how many files it has.
     */
    public record Cue(String id, String what, int variants, float volume, float pitch, float spread, int cooldown) {
        public String event() { return eventId(id); }

        public String subtitleKey() { return "subtitles.nusmp.sylph." + id; }

        /** The variant file path inside the sounds folder: {@code sylph/chime_1}. */
        public String file(int variant) { return "sylph/" + id + "_" + variant; }

        public float pitchMin() { return clamp(pitch - spread); }

        public float pitchMax() { return clamp(pitch + spread); }
    }

    private static float clamp(float p) { return Math.max(0.5f, Math.min(2.0f, p)); }

    private static final List<Cue> CUES = List.of(
            new Cue("link", "a glittering run of wind chimes as she takes her place", 2, 0.9f, 1.0f, 0.03f, 40),
            new Cue("unlink", "a falling run of chimes as she drifts away", 2, 0.8f, 1.0f, 0.03f, 40),
            new Cue("chime", "a soft wind chime now and then while she floats beside the caster", 3, 0.55f, 1.0f, 0.06f, 120),
            new Cue("flutter", "the light, fast flutter of dragonfly wings", 3, 0.5f, 1.0f, 0.08f, 30),
            new Cue("giggle", "a small tinkling giggle", 3, 0.8f, 1.0f, 0.07f, 100),
            new Cue("gust", "a gust whooshing out of her hand with a flick of chimes", 3, 0.9f, 1.0f, 0.06f, 14),
            new Cue("surge", "the powered form arriving: a rising swell of wind and bells", 2, 1.0f, 1.0f, 0.03f, 60),
            new Cue("gale", "the powered form's big gale: a deep whoosh with a ring of chimes", 2, 1.1f, 1.0f, 0.03f, 40),
            // the moods' wordless stand-ins for spoken lines that are missing ({@code SylphVoiceLines#fallbackCue}); appended, so the first eight keep their place
            new Cue("hum", "a little tune hummed to herself", 2, 0.6f, 1.0f, 0.04f, 200),
            new Cue("gasp", "a quick startled breath and a small 'oh'", 2, 0.8f, 1.0f, 0.05f, 100),
            new Cue("whimper", "a small sob, two falling wavering notes", 2, 0.7f, 1.0f, 0.04f, 200),
            new Cue("cheer", "a bright 'yay' with a run of chimes over it", 2, 0.85f, 1.0f, 0.04f, 100),
            new Cue("pout", "a huffy 'hmph'", 2, 0.7f, 1.0f, 0.04f, 200));

    public static List<Cue> cues() { return CUES; }

    public static Cue cue(String id) {
        for (Cue c : CUES) if (c.id.equals(id)) return c;
        return null;
    }

    public static boolean has(String id) { return cue(id) != null; }

    /** The sound event path of a cue: {@code sylph.chime}. */
    public static String eventId(String cue) { return "sylph." + cue; }

    /** A random pitch for a cue, {@code r01} being a random number in [0, 1). */
    public static float pitch(Cue c, double r01) { return (float) (c.pitchMin() + (c.pitchMax() - c.pitchMin()) * Math.max(0.0, Math.min(1.0, r01))); }

    /** A random variant for a cue, {@code r01} being a random number in [0, 1): 1..variants. */
    public static int variant(Cue c, double r01) { return 1 + (int) Math.min(c.variants - 1, Math.max(0.0, r01) * c.variants); }

    /** How many sound packets one listener may be sent from Sylphs in any one second (the per player budget). */
    public static final int MAX_PER_SECOND = 3;
}
