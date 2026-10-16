package com.newuniverse.nusmp.thread;

/**
 * Why: in 1.44.0 Rouge rested on the caster's shoulder, which the owner did not want: she belongs on the head, drawn exactly like the old
 * head cat. Two things have to agree for that to look right and never show two models at once: the server glues the entity to the head,
 * and the client draws EITHER the entity body (while she hops up or runs about) OR the head-cat layer (once she has settled), never both.
 * This pure class holds those shared rules (the settle time, the head height, the either-or switch, the name clean-up) so they can be tested
 * without the game. The state number is written in {@link RougeEntity#HEAD}.
 */
public final class RougePerch {
    private RougePerch() {}

    /** Ticks a resting cat is still drawn as the entity (so the hop up onto the head is seen) before the head-cat layer takes over. */
    public static final int SETTLE_TICKS = 8;

    /** Height of the top of a standing player's head above the feet: the model is 2.0 tall drawn at 0.9375, so 1.875. */
    public static final double HEAD_TOP = 1.875;
    /** How much lower the head sits while sneaking. */
    public static final double CROUCH_DROP = 0.25;

    /** The skill-tag key that keeps Rouge's name on the caster's thread grimoire. */
    public static final String NAME_TAG = "RougeName";
    public static final int MAX_NAME = 32;

    /** True when the head-cat layer (not the entity model) is the one drawn: resting on the head and settled. */
    public static boolean headCatDrawn(boolean restingOnHead, int stateAge) {
        return restingOnHead && stateAge >= SETTLE_TICKS;
    }

    /** True when the entity model is drawn. Always the opposite of {@link #headCatDrawn}, so there is never a double or an empty head. */
    public static boolean bodyDrawn(boolean restingOnHead, int stateAge) {
        return !headCatDrawn(restingOnHead, stateAge);
    }

    /** Where the entity rests above the caster's feet. */
    public static double headY(boolean crouching) { return HEAD_TOP - (crouching ? CROUCH_DROP : 0.0); }

    /** A name for Rouge from a name tag: trimmed, one line, at most {@link #MAX_NAME} characters; null when nothing is left. */
    public static String cleanName(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (s.length() > MAX_NAME) s = s.substring(0, MAX_NAME).trim();
        return s.isEmpty() ? null : s;
    }
}
