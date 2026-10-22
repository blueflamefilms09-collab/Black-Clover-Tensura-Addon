package com.newuniverse.nusmp.undine;

/**
 * Why: what the Undine DOES, SHOWS and SAYS for each thing that happens to her master, decided in one pure table (no Minecraft class, so it is unit tested and the doc's table is the code's): an
 * {@link Event} becomes a {@link Mood}: a pose ({@link UndinePose}), a face ({@link UndineFace}) and the group of voice line to say ({@link UndineLines.Group}). The rules keep her from flickering:
 * every event has a cooldown, a pose that is still held is not interrupted by an event of lower priority, two pose changes are at least {@link #MIN_CHANGE_GAP} ticks apart, and she only does her idle things
 * ({@link #IDLE_AFTER} ticks, plus a little more, after the last thing that happened). The state is per Undine ({@link State}), keyed by the owner's UUID by the caller; ticks are game ticks.
 */
public final class UndineMoodRules {
    private UndineMoodRules() {}

    /** The things that happen. */
    public enum Event {
        /** She was linked (called). */
        LINK(0),
        /** She was unlinked (sent away). */
        UNLINK(0),
        /** A page below the zone tier was cast through her. */
        CAST_SMALL(20),
        /** A zone or signature page was cast (her full form). */
        CAST_BIG(100),
        /** Her master took damage. */
        OWNER_HURT(60),
        /** Her master is at a third of their health or less. */
        OWNER_LOW_HP(400),
        /** Her master killed something. */
        OWNER_KILL(120),
        /** Her master died. */
        OWNER_DIED(0),
        /** Nothing has happened for a while. */
        IDLE(0);

        private final long cooldown;

        Event(long cooldown) { this.cooldown = cooldown; }

        /** Ticks before the same event may change her again. */
        public long cooldown() { return cooldown; }

        /** Does it always go through (a new link, a farewell, a death)? */
        public boolean forced() { return this == LINK || this == UNLINK || this == OWNER_DIED; }
    }

    /** What she does for an event: a null pose or face means "leave it as it is", a null group means "say nothing". */
    public record Mood(UndinePose pose, UndineFace face, UndineLines.Group speech) {}

    /** Two pose changes are at least this many ticks apart (unless the new pose is more important than the one held). */
    public static final int MIN_CHANGE_GAP = 10;
    /** She does her idle things this many ticks after the last event or pose, plus up to {@link #IDLE_JITTER} more. */
    public static final long IDLE_AFTER = 500, IDLE_JITTER = 256;
    /** A third of the health bar. */
    public static final float LOW_HEALTH_FRACTION = 1f / 3f;

    /** The idle things she does in turn: pose and face. */
    private static final UndinePose[] IDLE_POSES = {UndinePose.CLASP, UndinePose.WAVE, UndinePose.WELCOME, UndinePose.CLASP};
    private static final UndineFace[] IDLE_FACES = {UndineFace.SERENE, UndineFace.SMILE, UndineFace.CALM, UndineFace.SMILE};

    /** One Undine's memory: when each event last changed her, how long the held pose lasts and how important it is, when she may idle next. */
    public static final class State {
        private final long[] last = new long[Event.values().length];
        long poseUntil, lastChange = Long.MIN_VALUE / 4, nextIdle;
        int posePriority, idleCounter, speechCounter;

        public State() { java.util.Arrays.fill(last, Long.MIN_VALUE / 4); }

        public void reset(long now) {
            java.util.Arrays.fill(last, Long.MIN_VALUE / 4);
            poseUntil = 0; posePriority = 0; lastChange = Long.MIN_VALUE / 4; nextIdle = now + IDLE_AFTER;
        }

        /** The line counter for a round-robin pick (advances on every call). */
        public int nextSpeech() { return speechCounter++; }

        public long poseUntil() { return poseUntil; }

        public int posePriority() { return posePriority; }
    }

    /** The mood an event gives, before the rules of cooldown and priority. Null for IDLE (it picks from the table in turn). */
    public static Mood base(Event e) {
        return switch (e) {
            case LINK -> new Mood(UndinePose.RISE, UndineFace.SMILE, UndineLines.Group.LINK);
            case UNLINK -> new Mood(UndinePose.FAREWELL, UndineFace.SERENE, UndineLines.Group.FAREWELL);
            case CAST_SMALL -> new Mood(UndinePose.CAST, UndineFace.CALM, UndineLines.Group.CAST);
            case CAST_BIG -> new Mood(UndinePose.POWER, UndineFace.ANGRY, UndineLines.Group.WAVE);
            case OWNER_HURT -> new Mood(UndinePose.FLINCH, UndineFace.SURPRISED, UndineLines.Group.HURT);
            case OWNER_LOW_HP -> new Mood(UndinePose.SORROW, UndineFace.SAD, UndineLines.Group.LOWHP);
            case OWNER_KILL -> new Mood(UndinePose.WAVE, UndineFace.SMILE, UndineLines.Group.WIN);
            case OWNER_DIED -> new Mood(null, null, UndineLines.Group.DIED);
            case IDLE -> null;
        };
    }

    /**
     * What she does for an event at tick {@code now}; null when the rules say nothing happens (the event is on its cooldown, or an idle moment is not due). {@code rnd} is any random int (it only spreads
     * the next idle moment). The state is updated when something happens.
     */
    public static Mood decide(State s, Event e, long now, int rnd) {
        if (e == Event.IDLE) return idle(s, now, rnd);
        Mood m = base(e);
        long since = now - s.last[e.ordinal()];
        if (!e.forced() && since < e.cooldown()) return null;
        s.last[e.ordinal()] = now;
        if (m.pose() == null) return m;                                                           // a death: she only speaks (there is nobody to pose for)
        boolean held = now < s.poseUntil;
        boolean outranked = held && m.pose().priority() < s.posePriority;
        boolean tooSoon = now - s.lastChange < MIN_CHANGE_GAP && m.pose().priority() <= s.posePriority && !e.forced();
        if (!e.forced() && (outranked || tooSoon)) return new Mood(null, null, m.speech());      // she may still say her line, her body keeps doing what it does
        take(s, m.pose(), now, rnd);
        return m;
    }

    private static Mood idle(State s, long now, int rnd) {
        if (now < s.nextIdle || now < s.poseUntil) return null;
        int i = Math.floorMod(s.idleCounter++, IDLE_POSES.length);
        Mood m = new Mood(IDLE_POSES[i], IDLE_FACES[i], UndineLines.Group.IDLE);
        take(s, m.pose(), now, rnd);
        return m;
    }

    private static void take(State s, UndinePose pose, long now, int rnd) {
        s.poseUntil = now + pose.ticks();
        s.posePriority = pose.priority();
        s.lastChange = now;
        s.nextIdle = s.poseUntil + IDLE_AFTER + Math.floorMod(rnd, IDLE_JITTER);
    }

    /** Has the held pose run out (the server then clears the pose and the face in the prop word)? */
    public static boolean expired(State s, long now) { return s.poseUntil != 0 && now >= s.poseUntil; }

    /** The server cleared the pose: forget what was held. */
    public static void cleared(State s) { s.poseUntil = 0; s.posePriority = 0; }

    /** Is a health value low enough for the low-health event (a third of the bar or less, and above zero)? */
    public static boolean lowHealth(float health, float max) { return max > 0f && health > 0f && health <= max * LOW_HEALTH_FRACTION; }
}
