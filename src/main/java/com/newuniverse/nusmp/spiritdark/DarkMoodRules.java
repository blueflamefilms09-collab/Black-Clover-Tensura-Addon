package com.newuniverse.nusmp.spiritdark;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the Spirit of Darkness has moods: poses (a lounging aristocrat, a bow, a smug fold of the arms, claws, a feast, a recoil, a cackle, a battle stance, a farewell), faces (smug, grin, angry,
 * bored, surprised, sinister, amused: the same plates with another small sheet, so a face costs no quads) and spoken lines. This pure class (no game classes, ticks passed in) decides WHICH, per
 * situation, with cooldowns, so the spirit is expressive without being a nuisance: the server asks {@link Director#decide} when something happens (the owner links, casts, shadow steps, is hurt,
 * kills, is low on health, dies, unlinks, ...) and gets back a pose to play, a face to wear and the group of voice lines to draw one from (or none). The clients only read the pose and the
 * face from the prop word ({@link SpiritDarkRules#param}); the pose clips and the face sheets are the generated assets (tools/gen_spirit_dark_geo.py).
 *
 * <p>Rule 11 (no lag): one small object per player, a handful of longs; {@code decide} does a table lookup and a few comparisons, no allocation beyond the returned record.</p>
 */
public final class DarkMoodRules {
    private DarkMoodRules() {}

    /** The seven faces; the id is the sheet name ({@code spirit_dark_<form>_face_<id>.png}). The ordinal is what the prop word carries (3 bits). */
    public enum Face {
        SMUG("smug"), GRIN("grin"), ANGRY("angry"), BORED("bored"), SURPRISED("surprised"), SINISTER("sinister"), AMUSED("amused");

        public final String id;

        Face(String id) { this.id = id; }

        public static Face of(int ordinal) { Face[] v = values(); return v[Math.floorMod(ordinal, v.length)]; }
    }

    /**
     * The poses; the clip is the name in the animation files, {@code ticks} how long it is shown before the form returns to its resting clip (the lounge for the companion, the battle stance for
     * the powered form), {@code priority} which pose may interrupt which (a pose is only replaced by one of equal or higher priority while it plays). The ordinal goes into the prop word (4 bits).
     */
    public enum Pose {
        LOUNGE("idle", 0, 0), BOW("bow", 36, 2), SMUG("smug", 60, 1), VANISH("vanish", 9, 4), APPEAR("appear", 9, 4), CAST("cast", 18, 2), FEAST("feast", 30, 2), RECOIL("recoil", 12, 3),
        CACKLE("cackle", 50, 2), BATTLE("battle", 120, 1), FAREWELL("farewell", 36, 5);

        public final String clip;
        public final int ticks, priority;

        Pose(String clip, int ticks, int priority) { this.clip = clip; this.ticks = ticks; this.priority = priority; }

        public static Pose of(int ordinal) { Pose[] v = values(); return v[Math.floorMod(ordinal, v.length)]; }
    }

    /** What happened to make the spirit react. */
    public enum Event { LINK, UNLINK, CAST, STEP, DAMAGED, STRIKE, IDLE, LOW_HEALTH, KILL, OWNER_DIED, POWER_CALL, POWER_END }

    /** The groups of spoken lines; every group has a fallback cue of the synthesised set when no spoken line of it exists. */
    public static final String G_LINK = "link", G_UNLINK = "unlink", G_CAST = "cast", G_STEP = "step", G_HURT = "hurt", G_FEAST = "feast", G_IDLE = "idle", G_LOW = "low", G_KILL = "kill",
            G_DIED = "died", G_POWER = "power", G_POWER_END = "power_end";

    /** One row of the table: the pose and face an event gives, the voice group, the chance (percent) that a line is spoken, the shortest gap (ticks) before the event may be answered again, and the shortest gap for its voice. */
    public record Row(Pose pose, Face face, String group, int chance, int gap, int voiceGap) {}

    private static final Map<Event, Row> TABLE = new EnumMap<>(Event.class);

    static {
        TABLE.put(Event.LINK, new Row(Pose.BOW, Face.SMUG, G_LINK, 100, 40, 0));
        TABLE.put(Event.UNLINK, new Row(Pose.FAREWELL, Face.AMUSED, G_UNLINK, 100, 40, 0));
        TABLE.put(Event.CAST, new Row(Pose.CAST, Face.GRIN, G_CAST, 35, 30, 600));
        TABLE.put(Event.STEP, new Row(Pose.APPEAR, Face.SMUG, G_STEP, 40, 20, 400));
        TABLE.put(Event.DAMAGED, new Row(Pose.RECOIL, Face.SURPRISED, G_HURT, 50, 60, 400));
        TABLE.put(Event.STRIKE, new Row(Pose.FEAST, Face.SINISTER, G_FEAST, 25, 100, 600));
        TABLE.put(Event.IDLE, new Row(Pose.SMUG, Face.BORED, G_IDLE, 60, 900, 900));
        TABLE.put(Event.LOW_HEALTH, new Row(Pose.BATTLE, Face.ANGRY, G_LOW, 100, 900, 900));
        TABLE.put(Event.KILL, new Row(Pose.CACKLE, Face.AMUSED, G_KILL, 40, 200, 500));
        TABLE.put(Event.OWNER_DIED, new Row(Pose.FAREWELL, Face.SURPRISED, G_DIED, 100, 0, 0));
        TABLE.put(Event.POWER_CALL, new Row(Pose.APPEAR, Face.ANGRY, G_POWER, 100, 100, 0));
        TABLE.put(Event.POWER_END, new Row(Pose.FAREWELL, Face.BORED, G_POWER_END, 100, 40, 0));
    }

    public static Row row(Event e) { return TABLE.get(e); }

    /** The idle moods, rotated through: what the spirit does when nothing is happening (pose, face). */
    private static final Pose[] IDLE_POSES = {Pose.SMUG, Pose.BOW, Pose.LOUNGE, Pose.SMUG};
    private static final Face[] IDLE_FACES = {Face.BORED, Face.AMUSED, Face.SINISTER, Face.SMUG};

    /** The resting pose of a form: the lounge for the companion, the battle stance for the powered form. */
    public static Pose rest(boolean powered) { return powered ? Pose.BATTLE : Pose.LOUNGE; }

    /** The face a form wears when it has not been given one lately. */
    public static Face baseFace(boolean powered) { return powered ? Face.SINISTER : Face.SMUG; }

    /** How long a face is worn after the event that gave it (15 s), then the form's base face comes back. */
    public static final int FACE_HOLD = 300;
    /** The shortest gap between two spoken lines of one owner, whatever they are (3 s), and the owner's health fraction under which they are in trouble. */
    public static final int VOICE_GAP_ANY = 60;
    public static final float LOW_HEALTH_FRACTION = 0.30f;
    /** How often the idle mood is considered (ticks between the idle moods of one spirit): this much plus up to the same again. */
    public static final int IDLE_EVERY = 900;

    // ================================================================================================ what a client shows
    /** The pose a client plays: the pose of the prop word while it lasts, else the form's resting pose. */
    public static Pose activePose(int param, long now, boolean powered) {
        Pose p = SpiritDarkRules.pose(param);
        if (p == Pose.LOUNGE) return rest(powered);
        return SpiritDarkRules.since(param, now) < p.ticks ? p : rest(powered);
    }

    /** The face a client draws: the prop word's face while it is worn, else the form's base face. */
    public static Face activeFace(int param, long now, boolean powered) {
        return SpiritDarkRules.since(param, now) < FACE_HOLD && SpiritDarkRules.stamp(param) != 0 ? SpiritDarkRules.face(param) : baseFace(powered);
    }

    /** Seconds into the active clip: a one-shot pose counts from its start, a held pose and the resting loops use the clock. */
    public static float clipSeconds(int param, long now, float partial, float ageSeconds, boolean powered) {
        Pose p = activePose(param, now, powered);
        if (p == rest(powered) || p == Pose.SMUG || p == Pose.CACKLE || p == Pose.BATTLE) return ageSeconds;
        return (SpiritDarkRules.since(param, now) + partial) / 20f;
    }

    // ================================================================================================ the director (server, one per player)
    /** What the director decided: pose and face to set (null pose: leave the current one), the voice group to draw a line from (null: stay silent). */
    public record Decision(Pose pose, Face face, String voiceGroup) {}

    public static final class Director {
        private final long[] lastEvent = new long[Event.values().length], lastVoice = new long[Event.values().length];
        private long poseUntil = Long.MIN_VALUE / 4, lastAnyVoice = Long.MIN_VALUE / 4, nextIdle;
        private int poseRank, idleIndex;

        public Director() {
            java.util.Arrays.fill(lastEvent, Long.MIN_VALUE / 4);
            java.util.Arrays.fill(lastVoice, Long.MIN_VALUE / 4);
        }

        /**
         * The answer to an event at tick {@code now}. {@code roll} is a random number in [0, 100) the caller draws (kept outside so this stays pure and tests choose it); {@code powered} says the
         * powered form is out (it keeps its stance and does not lounge or bow idly); {@code ownerHealth} is the owner's health fraction (only the low health event reads it). Returns null when the
         * event is answered by nothing (still cooling down).
         */
        public Decision decide(Event e, boolean powered, long now, int roll, float ownerHealth) {
            Row r = row(e);
            int i = e.ordinal();
            if (now - lastEvent[i] < r.gap()) return null;
            if (e == Event.LOW_HEALTH && !(ownerHealth <= LOW_HEALTH_FRACTION)) return null;
            lastEvent[i] = now;
            Pose pose = r.pose();
            Face face = r.face();
            if (e == Event.IDLE) {
                if (powered) { pose = null; face = Face.SINISTER; }
                else { pose = IDLE_POSES[idleIndex % IDLE_POSES.length]; face = IDLE_FACES[idleIndex % IDLE_FACES.length]; }
                idleIndex++;
            }
            if (pose != null) {
                boolean playing = now < poseUntil;
                if (playing && pose.priority < poseRank) pose = null;                           // a stronger pose is still playing: this one does not interrupt it
                else { poseUntil = now + pose.ticks; poseRank = pose.priority; }
            }
            String group = null;
            if (r.chance() > 0 && roll >= 0 && roll < r.chance() && now - lastVoice[i] >= r.voiceGap() && now - lastAnyVoice >= VOICE_GAP_ANY) {
                group = r.group();
                lastVoice[i] = now;
                lastAnyVoice = now;
            }
            return new Decision(pose, face, group);
        }

        /** Is the idle mood due (and when is the next one: {@code extra} is a random 0..IDLE_EVERY)? Call once every few ticks. */
        public boolean idleDue(long now, int extra) {
            if (now < nextIdle) return false;
            nextIdle = now + IDLE_EVERY + Math.max(0, Math.min(IDLE_EVERY, extra));
            return true;
        }

        /** Arms the first idle mood (after a link). */
        public void armIdle(long now, int extra) { nextIdle = now + IDLE_EVERY / 2 + Math.max(0, Math.min(IDLE_EVERY, extra)); }
    }
}
