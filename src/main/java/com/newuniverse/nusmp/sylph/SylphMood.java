package com.newuniverse.nusmp.sylph;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Why: what Sylph FEELS is a decision, so it is pure (no game class; time is a tick number the caller passes, the randomness is a number the caller passes) and unit tested
 * ({@code SylphMoodRulesTest}). The server turns what happens to her owner into an {@link Event} (she is linked, a page is cast, the owner is hurt, drops low, kills something, dies, or nothing
 * happened for a long while); this class answers one {@link Decision}: the POSE she takes (a clip of her animation file, which also carries her FACE), the powered form's pose, how long it
 * lasts, and which group of spoken lines she may say (or none). Nothing flickers: a pose that is showing holds for its time and is only replaced by a more important one ({@link Spec#priority}),
 * the same event cannot restart its own pose inside {@link Spec#poseGap}, and every spoken group has its own cooldown and a chance, per owner. All state is per owner ({@link State}, kept by
 * {@code SylphCasting} per player UUID and dropped with the link).
 *
 * <p>The pose goes to the clients as four bits of the prop word ({@link SylphRules#POSE_SHIFT}); the clients map the code to a clip ({@link #smallClip}, {@link #poweredClip}). The server clears the
 * pose when its time is over. Voice lines are chosen by {@link SylphVoiceLines} and gated per listener by {@link SylphVoiceGate}.</p>
 */
public final class SylphMood {
    private SylphMood() {}

    /** What happened. */
    public enum Event { LINK, UNLINK, CAST, CAST_BIG, POWERED_ARRIVE, HURT, HURT_HEAVY, LOW_HEALTH, KILL, KILL_BOSS, DEATH, IDLE }

    /** Pose codes of the small form (0 = none: the clip is chosen by the clock and the cast, as before) and of the powered form. Appended only: a code is in a saved prop word. */
    public static final int NONE = 0, SWING = 1, WAVE = 2, CROSSED = 3, POUT = 4, VICTORY = 5, SLEEPY = 6, STARTLED = 7, HURT = 8, WORRY = 9, CRY = 10, CHEER = 11;
    public static final int P_BATTLE = 1, P_FURY = 2, P_TRIUMPH = 3;
    /** The clip each pose code plays (index = code; null = none). */
    public static final String[] SMALL_POSE_CLIP = {null, "swing", "wave", "crossed", "pout", "victory", "sleepy", "startled", "hurt", "worry", "cry", "cheer"};
    public static final String[] POWERED_POSE_CLIP = {null, "battle", "fury", "triumph"};

    /** The clip of a small pose code, or null for 0 / an unknown code (a forged or damaged param plays the ordinary clip). */
    public static String smallClip(int pose) { return pose > 0 && pose < SMALL_POSE_CLIP.length ? SMALL_POSE_CLIP[pose] : null; }

    public static String poweredClip(int pose) { return pose > 0 && pose < POWERED_POSE_CLIP.length ? POWERED_POSE_CLIP[pose] : null; }

    /** Nothing happened to her owner for this many ticks (45 seconds): she gets bored; she tries something new at most every {@link #IDLE_GAP} ticks (40 seconds). */
    public static final int IDLE_TICKS = 900, IDLE_GAP = 800;
    /** The owner is "low on health" at or under this fraction of their maximum (after a hit). A heavy hit takes at least this fraction of the maximum at once. */
    public static final float LOW_HEALTH = 0.30f, HEAVY_HIT = 0.30f;
    /** A killed monster with at least this much maximum health counts as a big kill (a boss). */
    public static final float BOSS_HEALTH = 100f;

    /** One row of the table: what an event does. {@code voiceChance} is a percent. */
    public record Spec(int pose, int poweredPose, int poseTicks, int priority, int poseGap, String voice, int voiceCooldown, int voiceChance) {}

    private static final Map<Event, Spec> TABLE = new EnumMap<>(Event.class);

    static {
        //                                 pose     powered      ticks prio gap   voice      cooldown chance
        TABLE.put(Event.LINK,           new Spec(WAVE,    NONE,        60, 4,   0,  "link",     0,   100));
        TABLE.put(Event.UNLINK,         new Spec(NONE,    NONE,         0, 0,   0,  "unlink",   0,   100));
        TABLE.put(Event.CAST,           new Spec(NONE,    NONE,         0, 0,   0,  "cast",   200,    40));
        TABLE.put(Event.CAST_BIG,       new Spec(NONE,    NONE,         0, 0,   0,  "big",    300,   100));
        TABLE.put(Event.POWERED_ARRIVE, new Spec(NONE,    P_BATTLE,    80, 5,   0,  "shout",  200,   100));
        TABLE.put(Event.HURT,           new Spec(HURT,    P_FURY,      16, 6,  40,  "hurt",   160,    60));
        TABLE.put(Event.HURT_HEAVY,     new Spec(STARTLED, P_FURY,     24, 7,  40,  "startle", 160,  100));
        TABLE.put(Event.LOW_HEALTH,     new Spec(WORRY,   P_FURY,     120, 8, 400,  "worry",  400,   100));
        TABLE.put(Event.KILL,           new Spec(CHEER,   P_TRIUMPH,   40, 5,  80,  "cheer",  160,    60));
        TABLE.put(Event.KILL_BOSS,      new Spec(VICTORY, P_TRIUMPH,   60, 5, 100,  "victory", 200,  100));
        TABLE.put(Event.DEATH,          new Spec(CRY,     NONE,        70, 10, 0,   "sad",      0,   100));
        TABLE.put(Event.IDLE,           new Spec(NONE,    NONE,         0, 3,   0,  null,       0,   100));      // the idle rotation fills this in
    }

    /** The idle rotation: pose, ticks, voice group. */
    private static final int[] IDLE_POSE = {SWING, SLEEPY, CROSSED, WAVE, POUT};
    private static final int[] IDLE_TICKS_OF = {160, 240, 140, 80, 140};
    private static final String[] IDLE_VOICE = {"idle", "sleepy", "smug", "idle", "pout"};

    public static Spec spec(Event e) { return TABLE.get(e); }

    /** The answer: the poses (0 = none), how long, and the voice group to say (null = say nothing). */
    public record Decision(int pose, int poweredPose, int poseTicks, String voice) {
        public boolean any() { return pose != 0 || poweredPose != 0 || voice != null; }
    }

    public static final Decision NOTHING = new Decision(0, 0, 0, null);

    /** The per-owner memory. Plain fields, no game class; one per linked player, dropped with the link. */
    public static final class State {
        public int pose, poweredPose, priority;
        public long poseUntil, poweredUntil;
        public long lastActivity, lastIdle = Long.MIN_VALUE / 4;
        public int idleStep;
        public double lastX, lastZ;
        private final EnumMap<Event, Long> lastEvent = new EnumMap<>(Event.class);
        private final Map<String, Long> lastVoice = new HashMap<>();
        private final Map<String, Integer> lastLine = new HashMap<>();

        public State() { }

        /** A moment of activity (the owner moved, cast, fought): the idle clock starts over. */
        public void active(long now) { lastActivity = now; }

        /** The pose that is showing at {@code now} (0 when none or over). */
        public int smallPose(long now) { return pose != 0 && now < poseUntil ? pose : 0; }

        public int poweredPose(long now) { return poweredPose != 0 && now < poweredUntil ? poweredPose : 0; }

        /** The index of the line said last in a group (-1 = none yet), and its record. */
        public int lastLine(String group) { return lastLine.getOrDefault(group, -1); }

        public void lineSaid(String group, int index) { lastLine.put(group, index); }
    }

    /**
     * Decides what an event does to her. {@code rnd} is any non-negative random number (the caller's). The state is updated when something is taken. The idle event is
     * {@link #idle}. An event whose pose is held back (a more important one is showing, or the same event came again too soon) still may give a line.
     */
    public static Decision decide(State st, Event e, long now, int rnd) {
        if (e == Event.IDLE) return idle(st, now, rnd);
        Spec s = TABLE.get(e);
        st.active(now);
        int pose = NONE, powered = NONE, ticks = 0;
        long last = st.lastEvent.getOrDefault(e, Long.MIN_VALUE / 4);
        boolean gapOk = now - last >= s.poseGap();
        if (s.pose() != NONE && gapOk && (st.smallPose(now) == 0 || s.priority() > st.priority)) {
            pose = s.pose();
            ticks = s.poseTicks();
            st.pose = pose;
            st.poseUntil = now + ticks;
            st.priority = s.priority();
        }
        if (s.poweredPose() != NONE && gapOk && (st.poweredPose(now) == 0 || s.priority() > st.priority)) {
            powered = s.poweredPose();
            ticks = Math.max(ticks, s.poseTicks());
            st.poweredPose = powered;
            st.poweredUntil = now + s.poseTicks();
        }
        if (pose != NONE || powered != NONE || s.pose() == NONE && s.poweredPose() == NONE) st.lastEvent.put(e, now);
        String voice = voice(st, s.voice(), s.voiceCooldown(), s.voiceChance(), now, rnd);
        if (e == Event.CAST && voice != null && (rnd >> 8) % 4 == 0) voice = "giggle";              // now and then the cast ends in a giggle
        return new Decision(pose, powered, ticks, voice);
    }

    /** The idle event: only when nothing happened for {@link #IDLE_TICKS} and she has not tried something for {@link #IDLE_GAP}; walks the rotation, starting at a random step. */
    public static Decision idle(State st, long now, int rnd) {
        if (now - st.lastActivity < IDLE_TICKS || now - st.lastIdle < IDLE_GAP) return NOTHING;
        if (st.smallPose(now) != 0) return NOTHING;
        if (st.lastIdle < 0) st.idleStep = Math.floorMod(rnd, IDLE_POSE.length);
        int i = st.idleStep % IDLE_POSE.length;
        st.idleStep = i + 1;
        st.lastIdle = now;
        st.pose = IDLE_POSE[i];
        st.poseUntil = now + IDLE_TICKS_OF[i];
        st.priority = TABLE.get(Event.IDLE).priority();
        String voice = voice(st, IDLE_VOICE[i], 300, 100, now, rnd >>> 4);
        return new Decision(st.pose, NONE, IDLE_TICKS_OF[i], voice);
    }

    private static String voice(State st, String group, int cooldown, int chance, long now, int rnd) {
        if (group == null) return null;
        if (cooldown > 0 && now - st.lastVoice.getOrDefault(group, Long.MIN_VALUE / 4) < cooldown) return null;
        if (chance < 100 && Math.floorMod(rnd, 100) >= chance) return null;
        st.lastVoice.put(group, now);
        return group;
    }

    /** What the owner's hit means: a heavy hit (a big share of their maximum health at once), a hit that leaves them low, or an ordinary hit. */
    public static Event hurtEvent(float damage, float healthAfter, float maxHealth) {
        if (maxHealth <= 0f) return Event.HURT;
        if (healthAfter > 0f && healthAfter / maxHealth <= LOW_HEALTH) return Event.LOW_HEALTH;
        return damage / maxHealth >= HEAVY_HIT ? Event.HURT_HEAVY : Event.HURT;
    }

    /** What a kill means: a boss-sized monster is a victory, anything else a cheer. */
    public static Event killEvent(float victimMaxHealth) { return victimMaxHealth >= BOSS_HEALTH ? Event.KILL_BOSS : Event.KILL; }

    /** True when the owner moved more than a little (squared distance on the ground since the last look), so idling does not count while they walk. */
    public static boolean moved(double dx, double dz) { return dx * dx + dz * dz > 0.04; }
}
