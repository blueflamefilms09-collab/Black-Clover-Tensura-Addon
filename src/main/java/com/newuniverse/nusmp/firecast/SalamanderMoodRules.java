package com.newuniverse.nusmp.firecast;

/**
 * Why: Fuegoleon's Salamander does not speak, so it shows how it feels with its body: a pose (an animation clip), a face (a plate swap on the head) and a creature sound (a trill, a hiss, a roar,
 * never a word). Every decision about WHICH pose, face and sound goes with WHICH situation lives here as pure functions and one small pure state class (no game classes, time is a tick number the
 * caller passes in), so the table, the cooldowns and the way the clients read the pose back are unit tested once (docs/magic/fire_casting.md, section 4.2).
 *
 * <p>The server decides ({@link Tracker#decide}, {@link Tracker#idle}); the decision travels in the prop's one synced {@code param} int, which already carried the variant, the hidden bit and a stamp,
 * and is now read as: bits 0-3 variant, bits 4-7 pose, bit 8 hidden, bits 9-11 face, bits 12-31 the stamp (the game tick the pose began, low 20 bits; for the big form the dragon's own life tick, which
 * is what its breath already used). A pose is only an id and a start tick: each client works out the clip and its time from the clock, so one pose costs one tiny entity-data update and nothing else, and
 * the pose ends by itself (the client drops back to the base loop when its duration is over; the server sends nothing at the end).</p>
 *
 * <p>Nothing flickers: a pose is held until it ends unless something of a higher priority comes, two poses are at least {@link #MIN_POSE_GAP} ticks apart, every situation has its own cooldown, and
 * a creature sound is at most one per {@link #SOUND_GAP} ticks for one Salamander ({@link SalamanderSoundSet} and the per listener gate add the rest).</p>
 */
public final class SalamanderMoodRules {
    private SalamanderMoodRules() {}

    // ================================================================================================ the pose and face ids (what the param carries)
    /** Pose 0 is "no pose": the base loop (perch for the small form, fly for the dragon). Pose 1 is the fire breath, the clip both forms already had. */
    public static final int POSE_NONE = 0, POSE_BREATHE = 1;
    /** The small form's own poses (ids 2..9). */
    public static final int GRIP_TIGHTEN = 2, HEAD_TILT = 3, TAIL_FLICK = 4, WING_STRETCH = 5, YAWN = 6, FLINCH = 7, PUFF_UP = 8, SLEEPY_CURL = 9;
    /** The dragon's own poses (ids 2..8; descend, hover and land are never stored in the param: the client picks them from the dragon's life). */
    public static final int ROAR = 2, WING_DISPLAY = 3, CLAW_SWIPE = 4, HURT = 5, DESCEND = 6, HOVER = 7, LAND = 8;

    /** The clip name of every pose id, per form. Index 0 is the base loop; every name is a clip of the form's animation file (a test reads the files). */
    private static final String[] CLIPS_SMALL = {"perch", "breathe", "grip_tighten", "head_tilt", "tail_flick", "wing_stretch", "yawn", "flinch", "puff_up", "sleepy_curl"};
    private static final String[] CLIPS_BIG = {"fly", "breathe", "roar", "wing_display", "claw_swipe", "hurt", "descend", "hover", "land"};

    /** How many ticks a pose lasts (the clip's length for the one-shot clips; for the looping sleepy curl and the hover, how long the mood is held). Index = pose id. */
    private static final int[] TICKS_SMALL = {0, 32, 28, 36, 24, 44, 48, 14, 36, 400};
    private static final int[] TICKS_BIG = {0, 40, 32, 32, 18, 16, 8, 0, 12};
    /** Does the clip loop (the client then plays it from the entity's age, not from the pose's start)? Index = pose id. */
    private static final boolean[] LOOP_SMALL = {true, false, false, false, false, false, false, false, false, true};
    private static final boolean[] LOOP_BIG = {true, false, false, false, false, false, false, true, false};

    /** The faces, as plate swaps on the head: 0 is the model's own (calm) eyes, the others are drawn on top of them at the full level of detail. */
    public static final int FACE_CALM = 0, FACE_ALERT = 1, FACE_ANGRY = 2, FACE_SLEEPY = 3, FACE_SMUG = 4, FACE_ROARING = 5, FACES = 6;
    private static final String[] FACE_NAMES = {"calm", "alert", "angry", "sleepy", "smug", "roaring"};

    public static String faceName(int face) { return FACE_NAMES[Math.max(0, Math.min(FACES - 1, face))]; }

    public static int poseCount(boolean dragon) { return (dragon ? CLIPS_BIG : CLIPS_SMALL).length; }

    public static String clipName(boolean dragon, int pose) {
        String[] t = dragon ? CLIPS_BIG : CLIPS_SMALL;
        return t[Math.max(0, Math.min(t.length - 1, pose))];
    }

    public static int poseTicks(boolean dragon, int pose) {
        int[] t = dragon ? TICKS_BIG : TICKS_SMALL;
        return t[Math.max(0, Math.min(t.length - 1, pose))];
    }

    public static boolean looping(boolean dragon, int pose) {
        boolean[] t = dragon ? LOOP_BIG : LOOP_SMALL;
        return t[Math.max(0, Math.min(t.length - 1, pose))];
    }

    // ================================================================================================ the param word
    public static final int POSE_SHIFT = 4, POSE_MASK = 15, FACE_SHIFT = 9, FACE_MASK = 7, STAMP_MASK = 0xFFFFF;

    /** Builds the prop param: variant (FireCastRules.V_*), the hidden bit, a pose id, a face id and the stamp (low 20 bits are kept). */
    public static int encode(int variant, boolean hidden, int pose, int face, int stamp) {
        stamp &= STAMP_MASK;
        if (stamp == 0) stamp = 1;                                                              // a stamp of 0 means "no pose"
        return (variant & FireCastRules.V_MASK) | ((pose & POSE_MASK) << POSE_SHIFT) | (hidden ? FireCastRules.HIDDEN : 0) | ((face & FACE_MASK) << FACE_SHIFT) | (stamp << FireCastRules.EXTRA_SHIFT);
    }

    public static int poseOf(int param) { return (param >>> POSE_SHIFT) & POSE_MASK; }

    public static int faceOf(int param) { return (param >>> FACE_SHIFT) & FACE_MASK; }

    public static int stampOf(int param) { return param >>> FireCastRules.EXTRA_SHIFT; }

    // ================================================================================================ what a client draws (pure, allocation free)
    /** The dragon arrives (the first ticks of its life) and leaves (the last ones). */
    public static final int DRAGON_ARRIVE_TICKS = 8, DRAGON_LEAVE_TICKS = 12;

    /** A look packs the clip index (bits 0-7), the face (bits 8-11) and the ticks since the clip began (bits 12-31). */
    public static int pack(int pose, int face, int elapsed) { return (pose & 0xFF) | ((face & 15) << 8) | (Math.max(0, Math.min(STAMP_MASK, elapsed)) << 12); }

    public static int poseOfLook(int look) { return look & 0xFF; }

    public static int faceOfLook(int look) { return (look >>> 8) & 15; }

    public static int elapsedOfLook(int look) { return look >>> 12; }

    /** A stamp may lead the client's clock by this many ticks and still count as "now" (the client's game time trails the server's by a tick or two); a stamp further ahead means "not yet". */
    public static final int STAMP_SLACK = 2;

    /**
     * What a client draws for a Salamander prop: the pose (clip index), the face and the ticks since the clip began, packed ({@link #pack}). {@code now} is the world's game tick, {@code life} the
     * prop's age in ticks and {@code maxLife} its whole life (both counted on the client too).
     *
     * <p>A pose the server set plays from its stamp (a game tick) for its duration; anything else is the base loop with the calm face. The old encoding (no pose, only a stamp) is still read the way it
     * always was: for the small form the stamp is a game tick and means the breath, for the dragon the stamp is the life tick the breath begins. The dragon also has a timeline of its own that needs no
     * packet: the first ticks are the descent, the last ones the landing, in between it hovers.</p>
     */
    public static int look(boolean dragon, int param, long now, int life, int maxLife) {
        int pose = poseOf(param), face = faceOf(param), stamp = stampOf(param);
        if (!dragon) {
            if (stamp == 0) return pack(POSE_NONE, FACE_CALM, 0);
            if (pose == POSE_NONE) { pose = POSE_BREATHE; face = FACE_CALM; }                    // the old encoding: a stamp alone meant "breathing"
            int dt = since(now, stamp);
            if (pose < CLIPS_SMALL.length && dt >= 0 && dt < poseTicks(false, pose)) return pack(pose, face, dt);
            return pack(POSE_NONE, FACE_CALM, 0);
        }
        if (maxLife > 0 && life >= maxLife - DRAGON_LEAVE_TICKS) return pack(LAND, FACE_CALM, life - (maxLife - DRAGON_LEAVE_TICKS));         // the landing outranks everything: it is leaving
        if (pose == POSE_NONE && stamp != 0) {                                                   // the old encoding: the stamp is the life tick the breath begins
            if (life >= stamp && life - stamp < poseTicks(true, POSE_BREATHE)) return pack(POSE_BREATHE, FACE_ROARING, life - stamp);
        } else if (pose != POSE_NONE && pose < CLIPS_BIG.length && pose != DESCEND && pose != LAND && pose != HOVER) {
            int dt = since(now, stamp);
            if (dt >= 0 && dt < poseTicks(true, pose)) return pack(pose, face, dt);
        }
        if (life < DRAGON_ARRIVE_TICKS) return pack(DESCEND, FACE_ALERT, life);
        return pack(HOVER, FACE_CALM, 0);
    }

    /** The param the big form is spawned with: the breath pose with the roaring face, stamped for the tick the breath begins ({@link FireCastRules#DRAGON_BREATH_TICK} ticks after the summon, plus the clock slack so the descent is not cut short). */
    public static int dragonSummonParam(long now) {
        return encode(FireCastRules.V_DRAGON, false, POSE_BREATHE, FACE_ROARING, (int) (now + FireCastRules.DRAGON_BREATH_TICK + STAMP_SLACK));
    }

    /** Ticks since a stamp (the low 20 bits of a game tick), or -1 when the stamp is still ahead of the clock by more than the slack. */
    public static int since(long now, int stamp) {
        int dt = (int) ((now - stamp) & STAMP_MASK);
        if (dt > STAMP_MASK - STAMP_SLACK) return 0;
        return dt > STAMP_MASK / 2 ? -1 : dt;
    }

    // ================================================================================================ the situations
    /** The situations a Salamander reacts to. New ones are only appended. */
    public enum Event {
        LINK, UNLINK, CAST_LIGHT, CAST_MID, CAST_HEAVY, DAMAGED, IDLE, LOW_HEALTH, OWNER_KILL, OWNER_DEATH, OWNER_MELEE, DRAGON_COME
    }

    /** A page of this cost (percent of the caster's mana) is a light, mid or heavy cast; the heavy ones (20 and up) are the ones that call the big form. */
    public static final double MID_COST = 8.0;

    public static Event castTier(double costPercent) {
        if (costPercent >= FireCastRules.DRAGON_COST_PERCENT) return Event.CAST_HEAVY;
        return costPercent >= MID_COST ? Event.CAST_MID : Event.CAST_LIGHT;
    }

    /** One row of the table: the pose, the face, the sound cue (null = silent), the priority, and the cooldown of the situation in ticks. */
    public record Mood(int pose, int face, String cue, int priority, int cooldown) {}

    /**
     * The table: the mood of a situation, per form. {@code null} when the form has no reaction to it. Small form: link grips and trills; a light cast breathes with an alert face and a whoosh, a mid cast
     * with an angry face and a growl, a heavy cast with a roaring face; damage makes it flinch and hiss; idle seconds give a curious tilt, a tail flick, a wing stretch or a yawn, and a long calm makes it
     * curl up and purr; the owner on low health makes it grip tight and trill; a kill makes it puff up proudly; the owner's death is a whimper (the prop is gone at once, so only the sound is heard);
     * unlinking is a small flame whoosh (the same). Big form: the breath, the roar on a kill, a swipe when the owner strikes, a display on low health, a hurt on damage.
     */
    public static Mood moodFor(Event ev, boolean dragon) {
        if (!dragon) {
            return switch (ev) {
                case LINK -> new Mood(GRIP_TIGHTEN, FACE_ALERT, "trill", 3, 40);
                case UNLINK -> new Mood(POSE_NONE, FACE_CALM, "whoosh", 3, 40);
                case CAST_LIGHT -> new Mood(POSE_BREATHE, FACE_ALERT, "whoosh", 4, 12);
                case CAST_MID -> new Mood(POSE_BREATHE, FACE_ANGRY, "growl", 4, 12);
                case CAST_HEAVY -> new Mood(POSE_BREATHE, FACE_ROARING, "growl", 4, 12);
                case DAMAGED -> new Mood(FLINCH, FACE_ANGRY, "hiss", 6, 50);
                case IDLE -> null;                                                                        // the idle poses come from Tracker#idle
                case LOW_HEALTH -> new Mood(GRIP_TIGHTEN, FACE_ALERT, "trill", 5, 400);
                case OWNER_KILL -> new Mood(PUFF_UP, FACE_SMUG, "chirp", 4, 100);
                case OWNER_DEATH -> new Mood(FLINCH, FACE_SLEEPY, "whimper", 7, 100);
                case OWNER_MELEE, DRAGON_COME -> null;
            };
        }
        return switch (ev) {
            case DRAGON_COME -> new Mood(POSE_NONE, FACE_CALM, "roar", 3, 100);
            case CAST_LIGHT, CAST_MID, CAST_HEAVY -> new Mood(POSE_BREATHE, FACE_ROARING, null, 4, 12);
            case DAMAGED -> new Mood(HURT, FACE_ANGRY, "roar", 6, 50);
            case LOW_HEALTH -> new Mood(WING_DISPLAY, FACE_ANGRY, "dragon_flap", 5, 400);
            case OWNER_KILL -> new Mood(ROAR, FACE_ROARING, "roar", 5, 100);
            case OWNER_MELEE -> new Mood(CLAW_SWIPE, FACE_ANGRY, null, 3, 40);
            case OWNER_DEATH -> new Mood(HURT, FACE_SLEEPY, "whimper", 7, 100);
            default -> null;
        };
    }

    /** The owner is on low health below this fraction of their maximum. */
    public static final float LOW_HEALTH_FRACTION = 0.3f;

    public static boolean lowHealth(float health, float max) { return max > 0f && health > 0f && health < max * LOW_HEALTH_FRACTION; }

    // ================================================================================================ the idle poses
    /** Seconds without anything happening (20 ticks each) before the first idle pose, the gaps between idle poses (a repeating table, so the show is never the same twice in a row), and the long calm that sends it to sleep. */
    public static final int IDLE_AFTER = 200, SLEEP_AFTER = 1800;
    private static final int[] IDLE_GAPS = {240, 320, 280, 360};
    /** The idle poses of the small form in turn: a curious head tilt, a tail flick, a wing stretch, a yawn; each with its face and (for three of them) a quiet creature sound. */
    private static final Mood[] IDLE_SMALL = {
            new Mood(HEAD_TILT, FACE_ALERT, "trill", 1, 0), new Mood(TAIL_FLICK, FACE_CALM, null, 1, 0), new Mood(WING_STRETCH, FACE_SMUG, "flap", 1, 0), new Mood(YAWN, FACE_SLEEPY, "yawn", 1, 0)};
    private static final Mood SLEEP_MOOD = new Mood(SLEEPY_CURL, FACE_SLEEPY, "purr", 1, 0);

    // ================================================================================================ the shared rhythm
    /** At least this many ticks between two poses (unless the new one outranks the one playing), and between two creature sounds of one Salamander. */
    public static final int MIN_POSE_GAP = 20, SOUND_GAP = 60;

    /** What the server does for a situation: the pose and face to put in the param, the sound to play, and for how long the pose lasts. {@link #NOTHING} when nothing is to change. */
    public record Decision(int pose, int face, String cue, int ticks, Event event) {
        /** True when the prop's param has to change (a pose to show; a face is only ever shown with a pose). */
        public boolean changesParam() { return pose != POSE_NONE; }

        public boolean any() { return pose != POSE_NONE || cue != null; }
    }

    public static final Decision NOTHING = new Decision(POSE_NONE, FACE_CALM, null, 0, null);

    /**
     * The state of ONE Salamander's moods, kept per player (inside the player's fire-style state, so it goes with it on logout, death and dimension change). Pure: the caller passes {@code now}. It holds a
     * handful of numbers; nothing grows.
     */
    public static final class Tracker {
        private final long[] lastEvent = new long[Event.values().length];
        private long poseUntil, lastPoseAt, lastSoundAt, lastActive, nextIdleAt;
        private int priority, idleIndex;
        private boolean started;

        public Tracker() { reset(); }

        public void reset() {
            java.util.Arrays.fill(lastEvent, Long.MIN_VALUE / 4);
            poseUntil = 0;
            lastPoseAt = lastSoundAt = Long.MIN_VALUE / 4;
            lastActive = nextIdleAt = 0;
            priority = 0;
            idleIndex = 0;
            started = false;
        }

        /** The Salamander has just appeared (linked, or the dragon came): the idle clock starts now. */
        public void begin(long now) {
            started = true;
            lastActive = now;
            nextIdleAt = now + IDLE_AFTER;
        }

        /** The priority of the pose playing at tick {@code now} (0 when none). */
        public int playingPriority(long now) { return now < poseUntil ? priority : 0; }

        /** The server put a pose on the prop by itself (the dragon's breath at its summon): remember that it plays until {@code until}. */
        public void playing(int pose, long now, long until) {
            if (!started) begin(now);
            lastActive = now;
            lastPoseAt = now;
            poseUntil = until;
            priority = 4;
        }

        /** A situation happened: what to do about it. {@code NOTHING} when it is on cooldown, outranked, too close behind another pose, or the form has no reaction to it. */
        public Decision decide(Event ev, boolean dragon, long now) {
            Mood m = ev == Event.IDLE ? null : moodFor(ev, dragon);
            if (m == null) return NOTHING;
            int e = ev.ordinal();
            if (now - lastEvent[e] < m.cooldown()) return NOTHING;
            lastEvent[e] = now;
            if (!started) begin(now);
            lastActive = now;
            nextIdleAt = Math.max(nextIdleAt, now + IDLE_AFTER);
            return take(m, ev, dragon, now);
        }

        /**
         * The idle clock, asked every few ticks. After {@link #IDLE_AFTER} ticks of nothing it gives the next idle pose of the turn (small form only), then waits the next gap; after
         * {@link #SLEEP_AFTER} ticks of nothing it curls up and sleeps (and the long calm starts over when it wakes). {@code NOTHING} otherwise.
         */
        public Decision idle(boolean dragon, long now) {
            if (dragon) return NOTHING;
            if (!started) { begin(now); return NOTHING; }
            if (now < nextIdleAt || now < poseUntil) return NOTHING;
            Mood m;
            if (now - lastActive >= SLEEP_AFTER) {
                m = SLEEP_MOOD;
                lastActive = now + poseTicks(false, SLEEPY_CURL);                                   // it wakes after the curl; the long calm starts over from there
            } else {
                m = IDLE_SMALL[idleIndex % IDLE_SMALL.length];
                idleIndex++;
            }
            nextIdleAt = now + IDLE_GAPS[idleIndex % IDLE_GAPS.length] + poseTicks(false, m.pose());
            return take(m, Event.IDLE, false, now);
        }

        private Decision take(Mood m, Event ev, boolean dragon, long now) {
            boolean outranked = m.priority() <= priority && (now < poseUntil || now - lastPoseAt < MIN_POSE_GAP);
            if (m.pose() == POSE_NONE || outranked) return soundOnly(m, ev, now);                 // a pose that is not allowed now still lets its sound through (if the sound gap allows)
            int ticks = poseTicks(dragon, m.pose());
            poseUntil = now + ticks;
            priority = m.priority();
            lastPoseAt = now;
            return new Decision(m.pose(), m.face(), soundFor(m, now), ticks, ev);
        }

        private Decision soundOnly(Mood m, Event ev, long now) {
            String cue = soundFor(m, now);
            return cue == null ? NOTHING : new Decision(POSE_NONE, FACE_CALM, cue, 0, ev);
        }

        private String soundFor(Mood m, long now) {
            if (m.cue() == null || now - lastSoundAt < SOUND_GAP) return null;
            lastSoundAt = now;
            return m.cue();
        }

        public long lastActive() { return lastActive; }

        public long poseUntil() { return poseUntil; }
    }
}
