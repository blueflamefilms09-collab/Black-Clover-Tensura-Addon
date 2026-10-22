package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.audio.voice.VoiceCatalog;
import com.newuniverse.nusmp.audio.voice.VoiceLimiter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Gnome is a character, so it reacts: it kneels and rises from the earth when it is linked, hammers a fist for a mid page and slams both for a big one, shrugs off a hit that lands on its master,
 * crosses its arms and says "Gnome stands firm" when its master is nearly dead, pounds its chest when its master kills something, dozes off when nothing has happened for half a minute, and mourns
 * when its master dies. Which POSE (a clip), which FACE (an expression: calm, amused, angry, sleepy, surprised, stern) and which VOICE (a spoken line, a grunt or a rumble) go with each situation is
 * decided here, as pure data and pure functions (no Minecraft class), so the table, the cooldowns, the thresholds and the line pools are unit tested ({@code GnomeMoodRulesTest}) and the server only asks.
 *
 * <p>Every situation has its own cooldown per owner ({@link Cooldowns}); the spoken lines have a further limit per listener ({@link LineGate}: at most one line every 4 seconds to one player, however
 * many Gnomes talk) and are heard within {@link #LINE_RADIUS} blocks only. State is keyed by the owner's UUID by the caller; nothing here is static state.</p>
 */
public final class GnomeMoodRules {
    private GnomeMoodRules() {}

    /** What happened. The order is append-only (the cooldown table is indexed by it). */
    public enum Situation { LINK, UNLINK, CAST_SMALL, CAST_MID, CAST_BIG, HURT, SURPRISE, LOW_HEALTH, KILL, OWNER_DEATH, IDLE, SLEEP, WAKE }

    /**
     * The reaction to a situation: the pose (a clip id), the face, the group of spoken lines and the non-verbal cue (a cue of {@link GnomeSoundRules}, or null), the chances in percent that the
     * line and the cue are used this time, and the cooldown in ticks before the same situation may react again for the same owner.
     */
    public record Mood(int pose, int face, String lineGroup, String cue, int lineChance, int cueChance, int cooldown) {}

    private static Mood m(int pose, int face, String group, String cue, int line, int cueChance, int cd) { return new Mood(pose, face, group, cue, line, cueChance, cd); }

    private static final Mood[] TABLE = {
            /* LINK */ m(GnomeRules.CLIP_KNEEL, GnomeRules.FACE_CALM, "greet", null, 100, 0, 20),
            /* UNLINK */ m(GnomeRules.CLIP_SINK, GnomeRules.FACE_CALM, "farewell", null, 100, 0, 20),
            /* CAST_SMALL */ m(GnomeRules.CLIP_PUNCH, GnomeRules.FACE_STERN, "cast", "grunt", 8, 40, 6),
            /* CAST_MID */ m(GnomeRules.CLIP_SLAM, GnomeRules.FACE_STERN, "cast", "grunt", 18, 60, 6),
            /* CAST_BIG */ m(GnomeRules.CLIP_CAST, GnomeRules.FACE_ANGRY, "cast_big", "grunt", 60, 100, 6),
            /* HURT */ m(GnomeRules.CLIP_SHRUG, GnomeRules.FACE_STERN, "hurt", "huff", 35, 80, 100),
            /* SURPRISE */ m(GnomeRules.CLIP_SHRUG, GnomeRules.FACE_SURPRISED, "surprised", "huff", 70, 100, 160),
            /* LOW_HEALTH */ m(GnomeRules.CLIP_GUARD, GnomeRules.FACE_STERN, "lowhp", null, 80, 0, 400),
            /* KILL */ m(GnomeRules.CLIP_POUND, GnomeRules.FACE_AMUSED, "victory", "chuckle", 55, 60, 160),
            /* OWNER_DEATH */ m(GnomeRules.CLIP_DOZE, GnomeRules.FACE_SLEEPY, "death", null, 100, 0, 0),
            /* IDLE */ m(GnomeRules.CLIP_IDLE, GnomeRules.FACE_AMUSED, "idle", "chuckle", 100, 100, 2400),
            /* SLEEP */ m(GnomeRules.CLIP_DOZE, GnomeRules.FACE_SLEEPY, "doze", "snore", 60, 100, 600),
            /* WAKE */ m(GnomeRules.CLIP_STOMP, GnomeRules.FACE_SURPRISED, "wake", "huff", 100, 100, 100),
    };

    public static Mood of(Situation s) { return TABLE[s.ordinal()]; }

    // ================================================================================================ the decisions
    /** A page of this cost tier (percent of the mana pool): a starter punches, a mid page slams one fist, a zone or signature page (20 and up) slams both and calls the giant. */
    public static Situation fromTier(double costPercent) {
        if (costPercent >= GnomeRules.FULL_COST_PERCENT) return Situation.CAST_BIG;
        return costPercent >= MID_COST_PERCENT ? Situation.CAST_MID : Situation.CAST_SMALL;
    }

    /** The cost tier from which a page is a mid page. */
    public static final double MID_COST_PERCENT = 12.0;
    /** Below this share of the maximum health the master is in danger; a single hit of at least this much damage (hit points) is a shock. */
    public static final float LOW_HEALTH_FRACTION = 0.30f, SURPRISE_DAMAGE = 8.0f, MIN_DAMAGE = 1.0f;
    /** How long the arms-crossed guard is held after the last low-health event. */
    public static final int GUARD_TICKS = 100;
    /** Ticks without any movement of the master before the Gnome dozes off (30 s), and before it makes one idle remark first (20 s). */
    public static final int DOZE_AFTER_TICKS = 600, IDLE_REMARK_AFTER_TICKS = 400;
    /** How far a spoken line carries, how long one listener is left alone after a line, and how long before the same line is said to the same listener again. */
    public static final double LINE_RADIUS = 32.0;
    public static final int LINE_GAP_TICKS = 80, LINE_REPEAT_TICKS = 600;

    /** What a hit on the master means: null for a scratch, else LOW_HEALTH (the health after the hit is at or under 30 percent), SURPRISE (a hit of 8 or more) or HURT. */
    public static Situation fromDamage(float damage, float healthAfter, float maxHealth) {
        if (damage < MIN_DAMAGE || maxHealth <= 0f) return null;
        if (healthAfter <= maxHealth * LOW_HEALTH_FRACTION) return Situation.LOW_HEALTH;
        return damage >= SURPRISE_DAMAGE ? Situation.SURPRISE : Situation.HURT;
    }

    public static boolean dozeDue(int stillTicks) { return stillTicks >= DOZE_AFTER_TICKS; }

    public static boolean idleRemarkDue(int stillTicks, int ticksSinceAnyEvent) { return stillTicks >= IDLE_REMARK_AFTER_TICKS && stillTicks < DOZE_AFTER_TICKS && ticksSinceAnyEvent >= IDLE_REMARK_AFTER_TICKS; }

    // ================================================================================================ per-owner state
    /** The last tick each situation reacted, for one owner. */
    public static final class Cooldowns {
        private final long[] last = new long[Situation.values().length];
        private boolean any;
        private long lastAny;

        public Cooldowns() { java.util.Arrays.fill(last, Long.MIN_VALUE / 4); }

        /** True when the situation's own cooldown is over. */
        public boolean ready(Situation s, long now) { return now - last[s.ordinal()] >= of(s).cooldown(); }

        /** Records the reaction (also the tick of the last reaction of any kind). */
        public void mark(Situation s, long now) { last[s.ordinal()] = now; any = true; lastAny = now; }

        /** Ticks since any reaction, or a large number when there was none. */
        public int sinceAny(long now) { return any ? (int) Math.min(Integer.MAX_VALUE / 2, now - lastAny) : Integer.MAX_VALUE / 2; }
    }

    /** How long the master has not moved: the position is compared with a tolerance, so standing and looking round counts as still. */
    public static final class Stillness {
        private double x, y, z;
        private long since = Long.MIN_VALUE;

        /** Feeds the position at a tick; returns true when the master moved (more than 0.15 blocks since the last still position). */
        public boolean update(long now, double px, double py, double pz) {
            if (since == Long.MIN_VALUE) { x = px; y = py; z = pz; since = now; return false; }
            double dx = px - x, dy = py - y, dz = pz - z;
            if (dx * dx + dy * dy + dz * dz > 0.0225) { x = px; y = py; z = pz; since = now; return true; }
            return false;
        }

        public int stillTicks(long now) { return since == Long.MIN_VALUE ? 0 : (int) Math.max(0, Math.min(Integer.MAX_VALUE / 2, now - since)); }
    }

    // ================================================================================================ the voice
    /** The spoken lines of each group (suffixes of the ids of {@link VoiceCatalog#GNOME_LINES}; the ids are {@code gnome_<suffix>}). */
    private static final Map<String, List<String>> GROUPS = Map.ofEntries(
            Map.entry("greet", List.of("greet_1", "greet_2")),
            Map.entry("farewell", List.of("farewell_1", "farewell_2")),
            Map.entry("cast", List.of("cast_1", "cast_2", "cast_3")),
            Map.entry("cast_big", List.of("cast_big_1", "cast_big_2")),
            Map.entry("hurt", List.of("hurt_1", "hurt_2", "hurt_3")),
            Map.entry("surprised", List.of("surprised_1")),
            Map.entry("lowhp", List.of("lowhp_1", "lowhp_2")),
            Map.entry("victory", List.of("victory_1", "victory_2")),
            Map.entry("death", List.of("death_1")),
            Map.entry("doze", List.of("doze_1")),
            Map.entry("wake", List.of("wake_1")),
            Map.entry("idle", List.of("idle_1")));

    /** The group names, for the tests. */
    public static java.util.Set<String> groups() { return GROUPS.keySet(); }

    /** The line ids of a group (full catalogue ids), only those the catalogue really has (a line whose recording is missing is not registered, so it is not said). */
    public static List<String> lines(String group) {
        List<String> suffixes = GROUPS.get(group);
        if (suffixes == null) return List.of();
        return suffixes.stream().map(VoiceCatalog::gnomeId).filter(VoiceCatalog::has).toList();
    }

    /** The planned suffixes of a group (for the tests). */
    public static List<String> planned(String group) { return GROUPS.getOrDefault(group, List.of()); }

    /** The line to say for a roll: one of the group's lines, not the one said last when there is another; null when the group has no line. */
    public static String pick(String group, int roll, String last) {
        List<String> pool = lines(group);
        if (pool.isEmpty()) return null;
        int i = Math.floorMod(roll, pool.size());
        if (pool.size() > 1 && pool.get(i).equals(last)) i = (i + 1) % pool.size();
        return pool.get(i);
    }

    /** Does a roll (any int) say the line this time? */
    public static boolean says(Mood m, int roll) { return Math.floorMod(roll, 100) < m.lineChance(); }

    /** Does a roll (any int) make the non-verbal cue this time? */
    public static boolean makesCue(Mood m, int roll) { return m.cue() != null && Math.floorMod(roll, 100) < m.cueChance(); }

    /**
     * The limit of the spoken lines per LISTENER: one line every {@link #LINE_GAP_TICKS} ticks to one player however many Gnomes there are. The pattern of
     * {@code audio.voice.VoiceLimiter} (state per player UUID, dropped at logout, time passed in by the caller so tests drive it).
     */
    public static final class LineGate {
        private final VoiceLimiter limiter = new VoiceLimiter(LINE_GAP_TICKS);

        public boolean allow(UUID listener, long now) { return limiter.tryAcquire(listener, "gnome", now, 1, LINE_GAP_TICKS); }

        public void forget(UUID listener) { limiter.forget(listener); }

        public void clear() { limiter.clear(); }

        public int tracked() { return limiter.tracked(); }
    }
}
