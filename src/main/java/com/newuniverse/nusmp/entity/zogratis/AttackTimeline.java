package com.newuniverse.nusmp.entity.zogratis;

import java.util.List;
import java.util.Map;

/**
 * Why: Leon's melee (docs/boss_reference_leon.md, 1.2) lands its damage on a stated tick of a clip made for that attack, so the animation is the telegraph. Our melee struck on a bare
 * cooldown and the swing was drawn 0.2 s into a fixed clip, so the arm and the damage only agreed by luck. This is the table (clip ticks, hit tick, optional second hit tick, damage
 * multiplier per hit) and the little clock that says on which tick a hit is due. Pure: no game classes (the client's pose clock reads the same table, rule 9), unit tested.
 *
 * <p>Damage stays neutral: every swing's total multiplier is its clip length over 20 ticks (the old fixed cooldown), so a boss that plays this table deals the damage per second it dealt
 * before, only in a different rhythm; {@link #neutral} pins that in a test. The damage still runs through {@code ZogratisDamage} (power, balance law) and the boss's budget and clamps.
 * A boss with no row here, or with the server key {@code attackTimeline} off, keeps the old fixed cooldown and the old client lead.
 */
public final class AttackTimeline {
    /** The shared attack clip's blow starts this many seconds into it (the old client lead {@code ZogratisPose.ATTACK_LEAD}). */
    public static final float CLIP_PEAK_SECONDS = 0.2f;
    /** The longest a swing may take: the server drops a swing that is older than this. */
    public static final int MAX_CLIP_TICKS = 60;

    /**
     * One melee move. {@code secondHitTick} is 0 for a one-hit move; {@code damageMult} applies to each hit.
     */
    public record Swing(String name, int clipTicks, int hitTick, int secondHitTick, float damageMult) {
        public Swing {
            clipTicks = Math.max(8, Math.min(clipTicks, MAX_CLIP_TICKS));
            hitTick = Math.max(1, Math.min(hitTick, clipTicks - 1));
            secondHitTick = secondHitTick <= hitTick ? 0 : Math.min(secondHitTick, clipTicks - 1);
            damageMult = Math.max(0.1f, Math.min(damageMult, 3f));
        }

        public int hits() { return secondHitTick > 0 ? 2 : 1; }

        /** The damage of the whole move as a multiple of one old plain blow. */
        public float totalMult() { return damageMult * hits(); }
    }

    private static Swing s(String n, int clip, int hit, float mult) { return new Swing(n, clip, hit, 0, mult); }
    private static Swing s2(String n, int clip, int hit, int hit2, float mult) { return new Swing(n, clip, hit, hit2, mult); }

    /** Leon's six clips, as each boss's own rotation (multiplier = clip / 20 per move, per hit /2 for a two-hit move: damage neutral). */
    private static final Map<String, List<Swing>> TABLE = Map.of(
            "meliodas", List.of(s("slash_horizontal", 28, 12, 1.4f), s("slash_rising", 30, 13, 1.5f), s2("cross_combo", 38, 12, 22, 0.95f)),
            "ban", List.of(s("jab", 26, 11, 1.3f), s("slash_rising", 30, 13, 1.5f), s2("cross_combo", 36, 11, 21, 0.9f)),
            "escanor", List.of(s("cleave", 34, 16, 1.7f), s("slash_horizontal", 30, 13, 1.5f), s("cleave_heavy", 38, 17, 1.9f)),
            "heath_grice", List.of(s("slash_horizontal", 28, 12, 1.4f), s("slash_rising", 30, 13, 1.5f)),
            "vetto", List.of(s("claw", 24, 10, 1.2f), s("slash_advancing", 28, 12, 1.4f), s2("cross_combo", 34, 10, 20, 0.85f)),
            "mars", List.of(s("slash_horizontal", 30, 13, 1.5f), s("slash_advancing", 32, 14, 1.6f)),
            "udaeus", List.of(s("slam", 36, 17, 1.8f), s("cleave_heavy", 40, 19, 2.0f)),
            "minotaur", List.of(s("axe_chop", 36, 17, 1.8f), s("cleave_heavy", 42, 20, 2.1f)),
            "black_goliath", List.of(s("fist_slam", 40, 19, 2.0f), s("cleave_heavy", 44, 22, 2.2f)));

    private AttackTimeline() {}

    /** The rotation of one character (empty = no timeline, keep the fixed cooldown). */
    public static List<Swing> forBoss(String id) { return id == null ? List.of() : TABLE.getOrDefault(id, List.of()); }

    public static boolean has(String id) { return !forBoss(id).isEmpty(); }

    /** Swing number 'count' of the rotation, or null when the boss has none. */
    public static Swing pick(String id, int count) {
        List<Swing> l = forBoss(id);
        return l.isEmpty() ? null : l.get(Math.floorMod(count, l.size()));
    }

    /** The row at a synced index (as the client reads it); null for an index out of range, -1 meaning 'no timeline'. */
    public static Swing at(String id, int index) {
        List<Swing> l = forBoss(id);
        return index < 0 || index >= l.size() ? null : l.get(index);
    }

    /** The index of swing number 'count' in the rotation (what the server syncs). */
    public static int indexOf(String id, int count) {
        int n = forBoss(id).size();
        return n == 0 ? -1 : Math.floorMod(count, n);
    }

    /** True when the swing deals no more damage per tick of its clip than the old plain blow (1 per 20 ticks), up to rounding. */
    public static boolean neutral(Swing s) { return s.totalMult() * 20f / s.clipTicks() <= 1.0f + 1e-3f; }

    /**
     * Client side: the second of the shared attack clip to show 'ticksSince' ticks after the swing began, for a swing whose blow lands on 'hitTick'. The arm raise of the clip (0 to
     * {@link #CLIP_PEAK_SECONDS}) is stretched over the lead-in so it ends on the hit tick, then the clip runs at its own speed. A swing with no timeline (hitTick 0) plays as before.
     */
    public static float clipSeconds(int hitTick, float ticksSince, float oldLead) {
        if (hitTick <= 0) return oldLead + ticksSince / 20f;
        if (ticksSince < hitTick) return Math.max(0f, ticksSince) / hitTick * CLIP_PEAK_SECONDS;
        return CLIP_PEAK_SECONDS + (ticksSince - hitTick) / 20f;
    }

    /** One boss's swing in progress: starts a swing, and says on which tick each hit is due. At most one swing at a time. */
    public static final class Runner {
        private Swing swing;
        private long start;
        private int done;

        public boolean busy(long t) { return swing != null && t - start <= swing.clipTicks(); }

        public void start(long t, Swing s) {
            swing = s;
            start = t;
            done = 0;
        }

        /** 1 or 2 when the first or second hit is due at 't' (once each), 0 otherwise; a swing that ran out of its clip is forgotten (a stalled boss never lands a stale hit). */
        public int due(long t) {
            if (swing == null) return 0;
            long n = t - start;
            if (n > swing.clipTicks()) { swing = null; return 0; }
            if (done == 0 && n >= swing.hitTick()) { done = 1; return 1; }
            if (done == 1 && swing.hits() == 2 && n >= swing.secondHitTick()) { done = 2; return 2; }
            return 0;
        }

        /** The swing in progress (null when none), for its multiplier. */
        public Swing current() { return swing; }

        public void cancel() { swing = null; }
    }
}
