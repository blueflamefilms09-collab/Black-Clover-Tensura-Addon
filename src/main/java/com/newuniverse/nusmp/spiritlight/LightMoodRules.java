package com.newuniverse.nusmp.spiritlight;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Spirit of Light is not a statue. What it does with its body (a pose), its face and its voice depends on what just happened to its owner, and that decision is pure (no game class): the server asks
 * {@link Mood#react} with a {@link Situation} and gets the pose to play, the client only plays the clip of the pose. The lion cub has playful poses (pounce, roll, stretch, sit and lick a paw, a small roar,
 * curl up asleep, a flinch, a victory strut) and four faces (happy, sleepy, fierce, surprised) and NO spoken words, only creature sounds; the seraph has reverent poses (wings open, halo spin, a blessing,
 * a hovering prayer, a recoil, a victory, the beam cast), four serene faces (serene, stern, compassionate, radiant) and short spoken lines. A face always goes with its pose: the clip of the pose shows the
 * face ({@link #face}); the assets are checked against these tables (SpiritLightAssetsTest).
 *
 * <p>Every reaction has a cooldown, a priority and a hold time, so a pose is never cut short by something less important and nothing repeats in a crowd of events: a hit does not become a pose per tick,
 * a line is not said twice in a row, an idle fidget comes after a long quiet. The state is one small object per player (keyed by their UUID by the caller) with no allocation per call.</p>
 */
public final class LightMoodRules {
    private LightMoodRules() {}

    // ================================================================================================ the tables
    /** The poses of the two forms, by id (the id is in the prop param); the clip of a pose has the same name. */
    public static final String[] CUB_POSES = {"idle", "cast", "pounce", "roll", "stretch", "lick", "roar", "sleep", "hurt", "victory"};
    public static final String[] SERAPH_POSES = {"idle", "cast", "wings_open", "halo_spin", "bless", "prayer", "hurt", "victory"};
    public static final String[] CUB_FACES = {"happy", "sleepy", "fierce", "surprised"};
    public static final String[] SERAPH_FACES = {"serene", "stern", "compassionate", "radiant"};
    /** The face (index into the face names) each pose shows. */
    private static final int[] CUB_POSE_FACE = {0, 2, 2, 0, 1, 0, 2, 1, 3, 0};
    private static final int[] SERAPH_POSE_FACE = {0, 1, 3, 0, 2, 0, 1, 3};
    /** Length of each pose's clip in ticks (a looping pose holds until the next reaction: its length is 0). */
    private static final int[] CUB_POSE_TICKS = {0, 20, 26, 32, 44, 52, 22, 0, 12, 36};
    private static final int[] SERAPH_POSE_TICKS = {0, 24, 44, 48, 48, 0, 14, 48};
    public static final int POSE_IDLE = 0, POSE_CAST = 1;

    public static int poseCount(boolean seraph) { return seraph ? SERAPH_POSES.length : CUB_POSES.length; }

    public static String poseName(boolean seraph, int pose) {
        String[] n = seraph ? SERAPH_POSES : CUB_POSES;
        return n[Math.max(0, Math.min(n.length - 1, pose))];
    }

    /** The index of a pose by name, or -1. */
    public static int poseId(boolean seraph, String name) {
        String[] n = seraph ? SERAPH_POSES : CUB_POSES;
        for (int i = 0; i < n.length; i++) if (n[i].equals(name)) return i;
        return -1;
    }

    /** The face the pose shows, as an index into {@link #CUB_FACES} / {@link #SERAPH_FACES}. */
    public static int face(boolean seraph, int pose) {
        int[] f = seraph ? SERAPH_POSE_FACE : CUB_POSE_FACE;
        return f[Math.max(0, Math.min(f.length - 1, pose))];
    }

    public static String faceName(boolean seraph, int pose) { return (seraph ? SERAPH_FACES : CUB_FACES)[face(seraph, pose)]; }

    /** Ticks the clip of a pose lasts; 0 for a pose that loops until something else happens (curled asleep, a hovering prayer, the idle). */
    public static int poseTicks(boolean seraph, int pose) {
        int[] t = seraph ? SERAPH_POSE_TICKS : CUB_POSE_TICKS;
        return t[Math.max(0, Math.min(t.length - 1, pose))];
    }

    public static boolean poseLoops(boolean seraph, int pose) { return poseTicks(seraph, pose) == 0; }

    // ================================================================================================ situations
    /** What happened (decided on the server). */
    public enum Situation { LINK, UNLINK, POWER_UP, POWER_DOWN, CAST_BEAM, CAST_PULSE, HURT, IDLE_FIDGET, IDLE_REST, WAKE, OWNER_LOW, OWNER_KILL, OWNER_DIED }

    private static final int[] COOLDOWN = {40, 40, 40, 40, 20, 40, 60, 900, 0, 0, 400, 100, 0};
    private static final int[] PRIORITY = {2, 2, 2, 2, 3, 3, 4, 1, 0, 5, 3, 2, 5};

    /** Ticks before the same situation may change the pose again. */
    public static int cooldown(Situation s) { return COOLDOWN[s.ordinal()]; }

    /** Which situation may interrupt a pose that is still playing: a reaction replaces a pose only when it is at least as important. */
    public static int priority(Situation s) { return PRIORITY[s.ordinal()]; }

    /** Quiet ticks (no reaction) before the spirit fidgets, the gap between two fidgets, and how long the owner must stand still before it settles to rest. */
    public static final int FIDGET_AFTER = 400, FIDGET_GAP = 900, REST_AFTER = 1200;
    /** The owner counts as still when they moved less than this many blocks between two upkeeps. */
    public static final double STILL_DIST = 0.02;
    /** The owner is at low health at or under this fraction of their maximum (and above zero). */
    public static final float LOW_FRACTION = 0.30f;

    public static boolean lowHealth(float health, float max) { return max > 0f && health > 0f && health / max <= LOW_FRACTION; }

    private static final String[] CUB_PLAY = {"pounce", "roll", "lick", "stretch", "pounce", "roll"};

    /** The pose a situation asks for, by the reaction counter (so the playful ones rotate). Never an unknown pose; {@link #POSE_IDLE} when the situation has no pose of its own. */
    public static int poseFor(boolean seraph, Situation s, int counter) {
        int c = Math.floorMod(counter, 6);
        if (seraph) {
            return switch (s) {
                case POWER_UP -> id(true, "wings_open");
                case CAST_BEAM -> id(true, "cast");
                case CAST_PULSE -> id(true, "halo_spin");
                case HURT -> id(true, "hurt");
                case OWNER_LOW -> id(true, "bless");
                case OWNER_KILL -> id(true, "victory");
                case IDLE_FIDGET -> id(true, new String[]{"halo_spin", "bless", "wings_open"}[c % 3]);
                case IDLE_REST, OWNER_DIED -> id(true, "prayer");
                case LINK, POWER_DOWN, UNLINK, WAKE -> POSE_IDLE;
            };
        }
        return switch (s) {
            case LINK -> id(false, CUB_PLAY[c]);
            case POWER_DOWN -> id(false, c % 2 == 0 ? "stretch" : "victory");
            case CAST_BEAM -> id(false, "cast");
            case CAST_PULSE, OWNER_LOW -> id(false, "roar");
            case HURT, OWNER_DIED -> id(false, "hurt");
            case OWNER_KILL -> id(false, "victory");
            case IDLE_FIDGET -> id(false, CUB_PLAY[c]);
            case IDLE_REST -> id(false, "sleep");
            case POWER_UP, UNLINK, WAKE -> POSE_IDLE;
        };
    }

    private static int id(boolean seraph, String name) { return poseId(seraph, name); }

    // ================================================================================================ voice: the seraph's lines and the cub's creature sounds
    /** What a spoken line is about; the line ids are {@code spiritlight_<cue>_<n>} (tools/audio/spirit_light_lines.json). */
    public enum Cue {
        GREET, FAREWELL, RISE, SETTLE, BEAM, PULSE, DAMAGE, LOW, WIN, DEATH, IDLE;

        public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** Ticks before the same cue may speak again for one owner (a line is never said twice in a row either). */
    private static final int[] CUE_COOLDOWN = {600, 200, 200, 200, 240, 400, 300, 600, 300, 100, 2400};

    public static int cueCooldown(Cue c) { return CUE_COOLDOWN[c.ordinal()]; }

    /** The cue a situation speaks, or null when it is silent (the cub never speaks; the seraph is silent on a wake, a link and a plain unlink). {@code counter} alternates a first greeting and a later rising. */
    public static Cue cueFor(boolean seraph, Situation s, int counter) {
        if (!seraph) return null;
        return switch (s) {
            case POWER_UP -> counter % 3 == 0 ? Cue.GREET : Cue.RISE;
            case POWER_DOWN -> Cue.SETTLE;
            case UNLINK -> Cue.FAREWELL;
            case CAST_BEAM -> Cue.BEAM;
            case CAST_PULSE -> Cue.PULSE;
            case HURT -> Cue.DAMAGE;
            case OWNER_LOW -> Cue.LOW;
            case OWNER_KILL -> Cue.WIN;
            case OWNER_DIED -> Cue.DEATH;
            case IDLE_FIDGET -> Cue.IDLE;
            default -> null;
        };
    }

    /**
     * The line to speak for a cue: from the ids that exist ({@code available}: the catalogue of lines that have a file) the ones of this cue, rotated by {@code counter} and never the {@code last}
     * one when there is another. Returns null when the cue has no line (the caller plays a synthesised cue instead: nothing is ever faked).
     */
    public static String line(Cue cue, int counter, List<String> available, String last) {
        String prefix = "spiritlight_" + cue.key() + "_";
        List<String> mine = new ArrayList<>(4);
        for (String id : available) if (id.startsWith(prefix)) mine.add(id);
        if (mine.isEmpty()) return null;
        String pick = mine.get(Math.floorMod(counter, mine.size()));
        if (mine.size() > 1 && pick.equals(last)) pick = mine.get(Math.floorMod(counter + 1, mine.size()));
        return pick;
    }

    /** The synthesised cue (a kind of {@link SpiritLightRules#SOUND_IDS}) played when a cue has no recorded line, or -1 for none (the beam, the pulse and the like already make their own sound). Nothing is faked: no voice is imitated. */
    public static int fallbackSound(Cue c) {
        return switch (c) {
            case GREET, RISE, WIN -> SpiritLightRules.S_LINK;
            case FAREWELL, SETTLE, DEATH, LOW -> SpiritLightRules.S_UNLINK;
            default -> -1;
        };
    }

    /** The creature sound (an index of {@link SpiritLightRules#SOUND_IDS}) the cub makes for a situation, or -1 for none. The pose it plays matters for the idle ones (a yawn, a purr). */
    public static int cubSound(Situation s, int pose) {
        String name = poseName(false, pose);
        return switch (s) {
            case LINK -> SpiritLightRules.S_CHIRP;
            case UNLINK -> SpiritLightRules.S_MEW;
            case CAST_BEAM -> SpiritLightRules.S_CUB_ROAR;
            case CAST_PULSE, OWNER_LOW -> SpiritLightRules.S_CUB_ROAR;
            case HURT, OWNER_DIED -> SpiritLightRules.S_WHIMPER;
            case OWNER_KILL, POWER_DOWN -> SpiritLightRules.S_CHIRP;
            case IDLE_FIDGET -> name.equals("stretch") ? SpiritLightRules.S_YAWN : name.equals("lick") ? SpiritLightRules.S_MEW : SpiritLightRules.S_CHIRP;
            case IDLE_REST -> SpiritLightRules.S_PURR;
            default -> -1;
        };
    }

    // ================================================================================================ the state of one spirit
    /** One spirit's memory: when each situation last changed the pose and spoke, the pose now, how long it is held, a counter that rotates the variety. */
    public static final class Mood {
        private final long[] lastSituation = new long[Situation.values().length];
        private final long[] lastCue = new long[Cue.values().length];
        private long poseUntil, lastEvent, lastFidget;
        private int pose, priority = -1, counter;
        private boolean resting;
        private String lastLine = "";

        public Mood() {
            java.util.Arrays.fill(lastSituation, Long.MIN_VALUE / 4);
            java.util.Arrays.fill(lastCue, Long.MIN_VALUE / 4);
            lastEvent = 0;
            lastFidget = Long.MIN_VALUE / 4;
        }

        public int pose() { return pose; }

        public int counter() { return counter; }

        public boolean resting() { return resting; }

        public String lastLine() { return lastLine; }

        public void said(String line) { lastLine = line == null ? "" : line; }

        /** May this cue speak now (its cooldown is over)? Counts it when yes. */
        public boolean cueReady(Cue c, long now) {
            if (c == null || now - lastCue[c.ordinal()] < cueCooldown(c)) return false;
            lastCue[c.ordinal()] = now;
            return true;
        }

        /**
         * A situation happened at {@code now}: returns the pose to play, or -1 when nothing changes (its cooldown has not run out, or the pose that is playing is more important and still going).
         * The caller puts the pose in the prop param and plays the sound of it.
         */
        public int react(boolean seraph, Situation s, long now) {
            if (now - lastSituation[s.ordinal()] < cooldown(s)) return -1;
            boolean holding = now < poseUntil || (resting && s != Situation.WAKE);
            if (holding && priority(s) < priority) return -1;
            lastSituation[s.ordinal()] = now;
            counter++;
            int p = poseFor(seraph, s, counter);
            if (p < 0) p = POSE_IDLE;
            pose = p;
            int len = poseTicks(seraph, p);
            poseUntil = len == 0 ? (p == POSE_IDLE ? now : Long.MAX_VALUE) : now + len;
            priority = p == POSE_IDLE ? -1 : priority(s);
            lastEvent = now;
            resting = s == Situation.IDLE_REST;
            if (s == Situation.WAKE) resting = false;
            return p;
        }

        /**
         * The quiet clock, called with the owner's stillness from the upkeep: wakes the spirit when the owner moves, lets it rest when the owner has stood still long enough, and fidgets after a long quiet.
         * Returns the situation to react with, or null. {@code stillTicks}: how long the owner has not moved.
         */
        public Situation quiet(long now, int stillTicks) {
            if (resting) return stillTicks == 0 ? Situation.WAKE : null;
            if (stillTicks >= REST_AFTER) return Situation.IDLE_REST;
            if (now >= poseUntil && now - lastEvent >= FIDGET_AFTER && now - lastFidget >= FIDGET_GAP) {
                lastFidget = now;
                return Situation.IDLE_FIDGET;
            }
            return null;
        }

        /** After a pose that is not looping has ended the spirit is idle again. */
        public void settle(long now) {
            if (!resting && now >= poseUntil && poseUntil != Long.MAX_VALUE) { pose = POSE_IDLE; priority = -1; }
        }
    }
}
