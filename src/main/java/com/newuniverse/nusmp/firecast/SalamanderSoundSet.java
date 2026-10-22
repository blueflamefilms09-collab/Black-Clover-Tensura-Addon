package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.audio.voice.VoiceLimiter;

import java.util.List;
import java.util.UUID;

/**
 * Why: the Salamander does not speak, so its sounds are creature sounds with no words: a soft growl, a trill and a chirp, a hiss, a small flame whoosh, a content purr-rumble, a yawn, a whimper, the
 * flap of small wings, and for the big form a roar and the heavy beat of its wings. This is the CATALOGUE, pure data with no Minecraft type so JUnit can pin it: the cues, how many variants each has,
 * their loudness, pitch range and cooldown per listener, and the id of the sound event ({@code nusmp:salamander.<cue>}), which {@link SalamanderSounds} registers one by one.
 * {@code tools/gen_salamander_sounds.py} synthesises the {@code .ogg} files and writes the {@code sounds.json} entries from the same table; {@code SalamanderSoundsTest} keeps the three in step.
 *
 * <p>{@link Gate} is the per listener rule, built on the same {@link VoiceLimiter} the spoken lines use: a listener hears at most one Salamander sound every {@link #LISTENER_GAP} ticks (3 seconds), and a
 * cue never repeats inside its own cooldown. The state is keyed by the listener's UUID and dropped on logout.</p>
 */
public final class SalamanderSoundSet {
    private SalamanderSoundSet() {}

    /** One cue. {@code cooldown} is the least number of ticks between two plays of this cue for one listener, {@code volume} the playback volume, {@code pitch} and {@code spread} the pitch centre and how far a random pitch may stray. */
    public record Cue(String id, String what, int variants, float volume, float pitch, float spread, int cooldown) {
        public String event() { return eventId(id); }

        public String subtitleKey() { return "subtitles.nusmp.salamander." + id; }

        /** The variant file path inside the sounds folder: {@code salamander/trill_1}. */
        public String file(int variant) { return "salamander/" + id + "_" + variant; }

        public float pitchMin() { return clamp(pitch - spread); }

        public float pitchMax() { return clamp(pitch + spread); }
    }

    private static float clamp(float p) { return Math.max(0.5f, Math.min(2.0f, p)); }

    private static final List<Cue> CUES = List.of(
            new Cue("trill", "a rising trill of a small reptile: greeting, curiosity, worry", 3, 0.7f, 1.0f, 0.06f, 100),
            new Cue("chirp", "a short happy chirp: pride", 3, 0.7f, 1.0f, 0.07f, 100),
            new Cue("growl", "a soft growl in the throat before a cast", 2, 0.8f, 1.0f, 0.05f, 80),
            new Cue("hiss", "a sharp hiss of pain or warning", 2, 0.8f, 1.0f, 0.05f, 80),
            new Cue("whoosh", "a small flame whoosh", 2, 0.7f, 1.0f, 0.06f, 40),
            new Cue("purr", "a content purr-rumble while it curls up", 2, 0.6f, 1.0f, 0.04f, 200),
            new Cue("yawn", "a small squeaky yawn", 2, 0.6f, 1.0f, 0.05f, 400),
            new Cue("whimper", "a low whimper: its caster has fallen", 2, 0.8f, 1.0f, 0.04f, 200),
            new Cue("flap", "a few quick flaps of small leathery wings", 2, 0.6f, 1.0f, 0.06f, 120),
            new Cue("roar", "the roar of the big golden form", 3, 1.1f, 1.0f, 0.04f, 100),
            new Cue("dragon_flap", "the heavy beats of the big form's wings", 2, 0.9f, 1.0f, 0.04f, 100));

    public static List<Cue> cues() { return CUES; }

    public static Cue cue(String id) {
        if (id == null) return null;
        for (Cue c : CUES) if (c.id.equals(id)) return c;
        return null;
    }

    public static boolean has(String id) { return cue(id) != null; }

    /** The sound event path of a cue: {@code salamander.trill}. */
    public static String eventId(String cue) { return "salamander." + cue; }

    /** The files of the whole set (every variant of every cue). */
    public static int totalVariants() {
        int n = 0;
        for (Cue c : CUES) n += c.variants;
        return n;
    }

    /** A random pitch for a cue, {@code r01} being a random number in [0, 1). */
    public static float pitch(Cue c, double r01) { return (float) (c.pitchMin() + (c.pitchMax() - c.pitchMin()) * Math.max(0.0, Math.min(1.0, r01))); }

    /** A random variant for a cue, {@code r01} being a random number in [0, 1): 1..variants. */
    public static int variant(Cue c, double r01) { return 1 + (int) Math.min(c.variants - 1, Math.max(0.0, r01) * c.variants); }

    /** Listeners within this many blocks of the Salamander hear it; nobody further is sent anything. */
    public static final double RADIUS = 32.0;
    /** A listener hears at most one Salamander sound every this many ticks (3 seconds). */
    public static final int LISTENER_GAP = 60;

    /**
     * The per listener rule: at most one sound per {@link #LISTENER_GAP} ticks for a listener, and no cue inside its own cooldown (the cue part is the voice limiter, which records only a play it allows;
     * the three second gap is peeked first and recorded only when the play goes through, so a refused cue never uses up the listener's gap). Server thread only; keyed by the listener's UUID.
     */
    public static final class Gate {
        private final VoiceLimiter cues = new VoiceLimiter(0);
        private final java.util.Map<UUID, Long> lastAny = new java.util.HashMap<>();

        /** Asks whether {@code listener} may be sent {@code cue} at tick {@code now}; if yes, records it. A cue that is not in the catalogue is never allowed. */
        public boolean tryAcquire(UUID listener, String cue, long now) {
            Cue c = cue(cue);
            if (c == null || listener == null) return false;
            Long last = lastAny.get(listener);
            if (last != null && now - last < LISTENER_GAP) return false;
            if (!cues.tryAcquire(listener, c.id(), now, 1, c.cooldown())) return false;
            lastAny.put(listener, now);
            return true;
        }

        public void forget(UUID listener) {
            cues.forget(listener);
            lastAny.remove(listener);
        }

        public void clear() {
            cues.clear();
            lastAny.clear();
        }

        public int tracked() { return Math.max(cues.tracked(), lastAny.size()); }
    }
}
