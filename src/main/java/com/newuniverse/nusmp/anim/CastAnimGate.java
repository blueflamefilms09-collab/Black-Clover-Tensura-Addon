package com.newuniverse.nusmp.anim;

/**
 * Client-side debounce of the casting body clips (pure Java, unit tested). The server names a clip on the key press, again on the
 * release and again for every fizzle; replaying the clip that is already on screen restarts it from its first frame (the arms snap
 * back and fade in again), which is the "flashing" a held or mashed cast key made the body do. This gate decides what a message
 * naming a clip does to the clip the entity is already playing.
 */
public final class CastAnimGate {
    private CastAnimGate() {}

    public enum Decision {
        /** Nothing changes: the same clip is already playing. */
        IGNORE,
        /** Begin the clip (fading in from whatever plays). */
        START,
        /** The same looping clip was on its way out: bring it back without restarting it. */
        RESUME
    }

    /** The fade-out length of the body pose, in ticks (CastAnimClient.FADE_OUT). */
    public static final float FADE_OUT = 6f;
    /** The same one-shot clip named again this soon after it began is a repeat of the same cast, not a new one. */
    public static final float REPEAT_WINDOW = 5f;

    /**
     * @param current   the clip the entity plays now, or null
     * @param loop      whether {@code current} is a looping clip
     * @param start     entity tick at which {@code current} began
     * @param stopAt    entity tick at which it was told to stop, or NaN while it has not been
     * @param requested the clip the message names
     * @param now       the entity's tick now
     */
    public static Decision decide(String current, boolean loop, float start, float stopAt, String requested, float now) {
        if (current == null || !current.equals(requested)) return Decision.START;
        boolean stopping = !Float.isNaN(stopAt);
        if (stopping) return loop && now - stopAt < FADE_OUT ? Decision.RESUME : Decision.START;
        if (loop) return Decision.IGNORE;
        return now - start < REPEAT_WINDOW ? Decision.IGNORE : Decision.START;
    }
}
