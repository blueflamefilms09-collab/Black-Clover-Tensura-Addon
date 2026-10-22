package com.newuniverse.nusmp.spiritspace;

/**
 * Why: the Spirit of Space has no human face and no speech, so it shows what it "feels" with poses, with the colour of its sky and with small creature sounds. Which pose and which expression a
 * situation gets, how long they last, how often the same situation may change them, what the pose does to the drawing (a handful of numbers) and which sound cue goes with it are decided HERE, pure (no
 * game classes), so they are unit tested; the server only calls {@link #moodFor}, {@link Tracker#ready} and {@link #withMood} and puts the result in the prop's param; the clients read the param and call
 * {@link #pose} and {@link #tint}. No pose or expression adds a quad: they only change scale, squash, brightness, tint, the speed of the existing animation clips and the spine's bend.
 *
 * <p>Param bits: 0-3 the variant (see {@link SpiritSpaceRules}), 4-7 the mood, 8 hidden, 9-10 the expression, 12 and up the tick the mood began (low 20 bits).</p>
 */
public final class SpaceMoodRules {
    private SpaceMoodRules() {}

    /** The poses. Orb poses and wisp poses share one list (COIL, ASCEND and ROAR are the wisp's; the orb draws them as FLARE-like swells). The ordinal goes into the param: only ever append. */
    public enum Mood {
        CALM, CURIOUS, FLARE, PORTAL_PULSE, SNAP, SQUEEZE, HURT, SLEEPY, COIL, ASCEND, ROAR, FAREWELL;

        public static Mood of(int id) { Mood[] v = values(); return id >= 0 && id < v.length ? v[id] : CALM; }
    }

    /** The expressions: the colour and strength of the starfield, the ring's shape, the glow of the dragon's eyes. */
    public enum Expression {
        CALM, EXCITED, ANGRY, SLEEPY;

        public static Expression of(int id) { Expression[] v = values(); return id >= 0 && id < v.length ? v[id] : CALM; }
    }

    /** What happened. */
    public enum Situation { LINK, PORTAL_CHANNEL, PORTAL_OPEN, CUT, GRAVITY, DAMAGED, IDLE, OWNER_LOW, OWNER_KILL, OWNER_DIED, UNLINK }

    /** The creature sound cues (synthesised, no words): each has several variants in sounds.json. NONE = no voice. */
    public enum Voice {
        WHISPER, CHIME, MURMUR, CHIRP, WARBLE, TRILL, SIGH, NONE;

        public String id() { return "voice_" + name().toLowerCase(java.util.Locale.ROOT); }

        /** The sound event path of this cue: {@code spirit_space.voice_whisper}. */
        public String eventId() { return "spirit_space." + id(); }
    }

    // ================================================================================================ the choice
    /** The pose for a situation. {@code wisp} = the powered form is out; {@code cycle} varies idle behaviour (any counter, e.g. game time / 400). */
    public static Mood moodFor(Situation s, boolean wisp, int cycle) {
        return switch (s) {
            case LINK -> Mood.CURIOUS;
            case PORTAL_CHANNEL -> wisp ? Mood.COIL : Mood.PORTAL_PULSE;
            case PORTAL_OPEN -> wisp ? Mood.ASCEND : Mood.PORTAL_PULSE;
            case CUT -> wisp ? Mood.ROAR : Mood.SNAP;
            case GRAVITY -> wisp ? Mood.ROAR : Mood.SQUEEZE;
            case DAMAGED -> Mood.HURT;
            case IDLE -> Math.floorMod(cycle, 3) == 2 ? Mood.CURIOUS : Mood.CALM;
            case OWNER_LOW -> Mood.CURIOUS;
            case OWNER_KILL -> Mood.FLARE;
            case OWNER_DIED, UNLINK -> Mood.FAREWELL;
        };
    }

    public static Expression expressionFor(Situation s) {
        return switch (s) {
            case LINK, PORTAL_CHANNEL, PORTAL_OPEN, GRAVITY, OWNER_KILL -> Expression.EXCITED;
            case CUT, DAMAGED, OWNER_LOW -> Expression.ANGRY;
            case OWNER_DIED, UNLINK -> Expression.SLEEPY;
            case IDLE -> Expression.CALM;
        };
    }

    /** The sound cue of a situation; {@code cycle} varies the idle voice. */
    public static Voice voiceFor(Situation s, int cycle) {
        return switch (s) {
            case LINK -> Voice.CHIRP;
            case PORTAL_CHANNEL -> Voice.WHISPER;
            case PORTAL_OPEN -> Voice.CHIME;
            case CUT -> Voice.NONE;                                                                   // the cut has its own tear sound
            case GRAVITY -> Voice.MURMUR;
            case DAMAGED, OWNER_LOW -> Voice.WARBLE;
            case OWNER_KILL -> Voice.TRILL;
            case OWNER_DIED, UNLINK -> Voice.SIGH;
            case IDLE -> Math.floorMod(cycle, 3) == 2 ? new Voice[]{Voice.MURMUR, Voice.WHISPER, Voice.CHIRP}[Math.floorMod(cycle / 3, 3)] : Voice.NONE;      // an idle spirit speaks about once a minute
        };
    }

    /** The sleepy companion: after the owner stood still this long (one minute) the orb dims and sinks; any situation wakes it. */
    public static final int SLEEP_AFTER_TICKS = 1200;

    /** An idle spirit is sleepy when its owner has not moved for {@link #SLEEP_AFTER_TICKS}. */
    public static boolean sleepy(long stillTicks) { return stillTicks >= SLEEP_AFTER_TICKS; }

    /** Low health: below this share of the owner's maximum the spirit shows alarm. */
    public static final double LOW_HEALTH = 0.30;

    public static boolean ownerLow(double health, double maxHealth) { return maxHealth > 0 && health > 0 && health / maxHealth < LOW_HEALTH; }

    // ================================================================================================ how long, how often
    /** Ticks a mood lasts; 0 = until something else happens (calm, sleepy). */
    public static int duration(Mood m) {
        return switch (m) {
            case CALM, SLEEPY -> 0;
            case CURIOUS -> 50;
            case FLARE -> 28;
            case PORTAL_PULSE, COIL, ASCEND -> 40;
            case SNAP -> 16;
            case SQUEEZE -> 24;
            case HURT -> 16;
            case ROAR -> 28;
            case FAREWELL -> 24;
        };
    }

    /** The mood in effect {@code since} ticks after it began: once its time is up it is calm again (sleepy and calm last). */
    public static Mood effective(Mood m, int since) {
        int d = duration(m);
        return d > 0 && since > d ? Mood.CALM : m;
    }

    /** The expression in effect: it outlasts its mood by two seconds, then it is calm (a sleepy mood keeps the sleepy look). */
    public static Expression effective(Expression e, Mood m, int since) {
        if (m == Mood.SLEEPY) return Expression.SLEEPY;
        int d = duration(m);
        return d > 0 && since > d + 40 ? Expression.CALM : e;
    }

    /** The least ticks between two changes caused by the same situation, and between two voice cues of one spirit. */
    public static int gap(Situation s) {
        return switch (s) {
            case DAMAGED -> 40;
            case IDLE -> 400;
            case OWNER_LOW -> 600;
            case OWNER_KILL -> 120;
            default -> 0;
        };
    }

    public static final int VOICE_GAP_OWNER = 60;
    /** A voice cue is heard within this many blocks only, and one listener hears at most one per {@link #VOICE_GAP_LISTENER} ticks (3 s). */
    public static final double VOICE_RADIUS = 32.0;
    public static final int VOICE_GAP_LISTENER = 60;

    /** Can a situation change the pose now? Weak situations (idle, low health, a kill) never interrupt a pose that is still running; nothing interrupts a farewell. */
    public static boolean canReplace(Situation s, Mood current, int since) {
        if (current == Mood.FAREWELL && effective(current, since) == Mood.FAREWELL) return false;
        boolean weak = s == Situation.IDLE || s == Situation.OWNER_LOW || s == Situation.OWNER_KILL;
        if (!weak) return true;
        Mood e = effective(current, since);
        return e == Mood.CALM || e == Mood.SLEEPY;
    }

    /** The per-player memory of when each situation last changed the pose, so the same trigger cannot spam. Pure, single threaded, one per spirit. */
    public static final class Tracker {
        private final long[] last = new long[Situation.values().length];
        private long lastVoice = Long.MIN_VALUE / 4;

        public Tracker() { java.util.Arrays.fill(last, Long.MIN_VALUE / 4); }

        public boolean ready(Situation s, long now) { return now - last[s.ordinal()] >= gap(s); }

        public void mark(Situation s, long now) { last[s.ordinal()] = now; }

        public boolean voiceReady(long now) { return now - lastVoice >= VOICE_GAP_OWNER; }

        public void markVoice(long now) { lastVoice = now; }
    }

    /** The hearing rule: a listener hears a cue only within {@link #VOICE_RADIUS} blocks, and {@link #VOICE_GAP_LISTENER} ticks after the last one they heard. */
    public static boolean hears(double distSq, long now, long lastHeard) {
        return distSq <= VOICE_RADIUS * VOICE_RADIUS && now - lastHeard >= VOICE_GAP_LISTENER;
    }

    // ================================================================================================ the param word
    public static final int MOOD_SHIFT = 4, MOOD_MASK = 15 << MOOD_SHIFT, EXPR_SHIFT = 9, EXPR_MASK = 3 << EXPR_SHIFT;

    public static Mood moodOf(int param) { return Mood.of((param & MOOD_MASK) >> MOOD_SHIFT); }

    public static Expression expressionOf(int param) { return Expression.of((param & EXPR_MASK) >> EXPR_SHIFT); }

    /** The param with a new mood, expression and start stamp; the variant and the hidden bit are kept. */
    public static int withMood(int param, Mood m, Expression e, int stamp) {
        int keep = param & (SpiritSpaceRules.V_MASK | SpiritSpaceRules.HIDDEN);
        return keep | (m.ordinal() << MOOD_SHIFT) | (e.ordinal() << EXPR_SHIFT) | ((stamp & SpiritSpaceRules.EXTRA_MASK) << SpiritSpaceRules.EXTRA_SHIFT);
    }

    // ================================================================================================ what a pose does to the drawing
    public static final int P_SCALE = 0, P_SQUASH = 1, P_ALPHA = 2, P_SHAKE = 3, P_RADIUS = 4, P_HEIGHT = 5, P_COIL = 6, P_RISE = 7, P_CAST = 8, P_RING = 9, P_COUNT = 10;

    private static double bell(double u) { return Math.sin(Math.PI * Math.max(0.0, Math.min(1.0, u))); }

    private static double ease(double u) { u = Math.max(0.0, Math.min(1.0, u)); return u * u * (3 - 2 * u); }

    /**
     * Writes the numbers of a pose {@code since} ticks after it began into {@code o} (length {@link #P_COUNT}): size (scale, squash of the height), brightness (alpha), shake, how the orb's orbit
     * changes (radius factor, height added), the wisp's coil (0 straight .. 1 a tight spiral) and rise in blocks, whether the cast clip plays (0 or 1) and the factor of the rift ring. The orb's and the
     * wisp's pose is read from the same table; none of the numbers adds geometry.
     */
    public static void pose(float[] o, Mood mood, float since) {
        o[P_SCALE] = 1f; o[P_SQUASH] = 1f; o[P_ALPHA] = 1f; o[P_SHAKE] = 0f; o[P_RADIUS] = 1f; o[P_HEIGHT] = 0f; o[P_COIL] = 0f; o[P_RISE] = 0f; o[P_CAST] = 0f; o[P_RING] = 1f;
        int d = duration(mood);
        double u = d > 0 ? Math.min(1.0, since / d) : 0.0, b = bell(u);
        switch (mood) {
            case CURIOUS -> {                                                                         // it drifts in toward the owner's face and tilts, then returns
                double k = Math.sqrt(b);
                o[P_RADIUS] = (float) (1.0 - 0.5 * k);
                o[P_HEIGHT] = (float) (0.15 * k);
                o[P_SCALE] = (float) (1.0 + 0.06 * b);
                o[P_SQUASH] = (float) (1.0 + 0.08 * Math.sin(since * 0.5) * b);
            }
            case FLARE -> { o[P_SCALE] = (float) (1.0 + 0.25 * b); o[P_RING] = (float) (1.0 + 0.5 * b); o[P_CAST] = 1f; }
            case PORTAL_PULSE -> {
                double pulse = Math.abs(Math.sin(u * 3 * Math.PI));
                o[P_SCALE] = (float) (1.0 + 0.14 * pulse * b);
                o[P_RING] = (float) (1.0 + 0.4 * pulse);
                o[P_CAST] = 1f;
            }
            case SNAP -> {                                                                            // a sharp contraction, then a spring back
                if (u < 0.2) o[P_SCALE] = (float) (1.0 - 0.4 * Math.sin(Math.PI * u / 0.2));
                else o[P_SCALE] = (float) (1.0 + 0.3 * Math.exp(-(u - 0.2) * 6) * Math.cos((u - 0.2) * 20));
                o[P_RING] = u < 0.2 ? 0.7f : (float) (1.0 + 0.3 * Math.exp(-(u - 0.2) * 6));
                o[P_CAST] = 1f;
            }
            case SQUEEZE -> { o[P_SQUASH] = (float) (1.0 - 0.35 * b); o[P_SCALE] = (float) (1.0 + 0.15 * b); o[P_RING] = (float) (1.0 - 0.3 * b); }
            case HURT -> {                                                                            // a flicker and a tremble
                o[P_ALPHA] = (((int) (since / 2)) & 1) == 0 ? 0.35f : 1f;
                o[P_SHAKE] = 0.03f * (float) (1.0 - u);
                o[P_SCALE] = 0.92f;
            }
            case SLEEPY -> { o[P_SCALE] = 0.82f; o[P_ALPHA] = 0.6f; o[P_HEIGHT] = -0.5f; o[P_RADIUS] = 0.8f; o[P_RING] = 0.7f; }
            case COIL -> o[P_COIL] = (float) (0.9 * ease(since / 20.0));
            case ASCEND -> { o[P_RISE] = (float) (0.9 * ease(u)); o[P_COIL] = (float) (0.5 * (1.0 - ease(u))); }
            case ROAR -> { o[P_SCALE] = (float) (1.0 + 0.1 * b); o[P_CAST] = 1f; }
            case FAREWELL -> {                                                                        // it winds into itself and goes out
                o[P_SCALE] = (float) (1.0 - 0.9 * ease(u));
                o[P_ALPHA] = (float) (1.0 - ease(u));
                o[P_COIL] = (float) ease(u);
                o[P_RADIUS] = (float) (1.0 - 0.8 * ease(u));
                o[P_RING] = (float) (1.0 + 0.6 * u);
            }
            default -> { }
        }
    }

    // ================================================================================================ what an expression does to the drawing
    /** The tint of the starfield layers (RGB), their strength, the factor of the ring and the colour of the glow parts (the dragon's eyes). */
    private static final int[] TINT = {0xB8C4FF, 0x7FF4FF, 0xFF6FB0, 0x7080C0}, EYE = {0x4FE3FF, 0xB8FFFF, 0xFF4A7A, 0x6A5AA8};
    private static final float[] STRENGTH = {0.9f, 1.2f, 1.15f, 0.5f}, RING = {1.0f, 1.1f, 0.9f, 0.85f};

    public static int tint(Expression e) { return TINT[e.ordinal()]; }

    public static int eye(Expression e) { return EYE[e.ordinal()]; }

    public static float strength(Expression e) { return STRENGTH[e.ordinal()]; }

    public static float ringShape(Expression e) { return RING[e.ordinal()]; }
}
