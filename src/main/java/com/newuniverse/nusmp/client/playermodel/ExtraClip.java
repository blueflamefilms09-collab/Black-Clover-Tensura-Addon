package com.newuniverse.nusmp.client.playermodel;

import java.util.Set;

/**
 * Why: the extras of a character (wings, tails, capes, floating weapons) are GeoLite parts with the clips of the arc art kit (idle, walk, run, attack, cast, cast_big, hurt, transform, death), and
 * which clip they play is a decision of the entity's state, not of the model. This picks it in one pure place (so the tests and every renderer agree) and falls back down a chain when the file
 * lacks a clip: a clip the file does not have is never asked for, so a part that was extracted from an older model still plays.
 */
public final class ExtraClip {
    private ExtraClip() {}

    public static final String IDLE = "idle", WALK = "walk", RUN = "run", ATTACK = "attack", CAST = "cast", CAST_BIG = "cast_big", HURT = "hurt", TRANSFORM = "transform", DEATH = "death";
    /** Ground speed in blocks per tick at which the walk starts and the run starts. */
    public static final float WALK_ON = 0.02f, RUN_ON = 0.17f;

    /**
     * The clip to play. {@code have} is the set of clips the animation file has (an empty set means none: the answer is then {@link #IDLE}, which the loader treats as the rest pose).
     */
    public static String pick(Set<String> have, boolean dead, boolean transforming, int cast, boolean swinging, float speed) {
        String want;
        if (dead) want = DEATH;
        else if (transforming) want = TRANSFORM;
        else if (cast >= 2) want = CAST_BIG;
        else if (cast == 1) want = CAST;
        else if (swinging) want = ATTACK;
        else if (speed >= RUN_ON) want = RUN;
        else if (speed >= WALK_ON) want = WALK;
        else want = IDLE;
        return fallback(have, want);
    }

    /** The clip or the nearest one the file has: cast_big falls back to cast, run to walk, everything else to idle. */
    public static String fallback(Set<String> have, String want) {
        if (have.contains(want)) return want;
        return switch (want) {
            case CAST_BIG -> have.contains(CAST) ? CAST : IDLE;
            case RUN -> have.contains(WALK) ? WALK : IDLE;
            default -> IDLE;
        };
    }

    /**
     * The seconds into a clip: a looping clip runs on the entity's clock, a one-shot clip (death, transform, attack, hurt) is held on its last frame once it has run. {@code length} is the
     * clip's length in seconds, {@code loop} whether it loops, {@code seconds} the entity's clock.
     */
    public static float time(float seconds, float length, boolean loop) {
        if (length <= 1e-6f) return 0f;
        return loop ? seconds % length : Math.min(seconds, length);
    }
}
