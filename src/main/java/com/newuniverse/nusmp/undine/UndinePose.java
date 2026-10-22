package com.newuniverse.nusmp.undine;

/**
 * Why: the poses of the Undine (docs/magic/undine.md): the named things her body does, each an animation clip of {@code undine_body.animation.json}, how long the server holds it, and how important it
 * is (a more important pose is not interrupted by a less important event). Pure data: the server stores the pose id in the prop word, the client turns it into a clip name, a test reads the clip names
 * from the animation file. The clips come in two flavours when they end in a resting pose: the companion rests with her arms relaxed, the full form with her arms spread, so those poses have a form
 * suffix ({@link #clip}).
 */
public enum UndinePose {
    /** No pose: her idle (the companion's relaxed one, the full form's open one). */
    NONE(0, "", 0, 0, false),
    /** Rising out of the waves: the entrance. */
    RISE(1, "rise", 24, 4, true),
    /** Arms open in welcome (the still of the owner's second image). */
    WELCOME(2, "welcome", 80, 1, false),
    /** Hands clasped in front of her. */
    CLASP(3, "clasp", 100, 1, false),
    /** A gentle wave of one hand. */
    WAVE(4, "wave", 50, 2, false),
    /** Looking down, sorrowful. */
    SORROW(5, "sorrow", 100, 3, false),
    /** Casting: the arms spread, palms up. */
    CAST(6, "cast", 24, 3, true),
    /** The powered swell: arms flung wide, hair and gown flaring. */
    POWER(7, "power", 40, 5, true),
    /** A flinch when her master is hurt. */
    FLINCH(8, "flinch", 10, 4, true),
    /** Farewell: she sinks and dissolves into the water. */
    FAREWELL(9, "farewell", 28, 6, true);

    private final int id;
    private final String clip;
    private final int ticks, priority;
    private final boolean perForm;

    UndinePose(int id, String clip, int ticks, int priority, boolean perForm) {
        this.id = id; this.clip = clip; this.ticks = ticks; this.priority = priority; this.perForm = perForm;
    }

    public int id() { return id; }

    /** How long the server keeps the pose before it clears it (ticks); the clip is about as long. */
    public int ticks() { return ticks; }

    /** Higher wins: an event whose pose has a lower priority does not interrupt a pose that is still held. */
    public int priority() { return priority; }

    /** The clip for a form: the pose's own name, with {@code _f} for the full form when the pose ends in a resting pose; NONE gives the idles. */
    public String clip(boolean full) {
        if (this == NONE) return full ? "full_idle" : "idle";
        return perForm && full ? clip + "_f" : clip;
    }

    /** Every clip name this pose can ask for (both forms). */
    public java.util.List<String> clips() { return this == NONE ? java.util.List.of("idle", "full_idle") : perForm ? java.util.List.of(clip, clip + "_f") : java.util.List.of(clip); }

    /** The pose with this id (out of range: NONE). */
    public static UndinePose of(int id) {
        for (UndinePose p : values()) if (p.id == id) return p;
        return NONE;
    }

    /** The largest id (the prop word keeps four bits for it). */
    public static int maxId() { return FAREWELL.id; }
}
