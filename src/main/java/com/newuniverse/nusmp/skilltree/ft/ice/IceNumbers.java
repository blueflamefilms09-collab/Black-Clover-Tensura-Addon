package com.newuniverse.nusmp.skilltree.ft.ice;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.kit.FtBudget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: everything about Ice-Make that is arithmetic and not the game lives here as pure functions, so it can be tested without a server and so the owner's rules are written down once.
 * (1) The FALLBACK table: the level-1 value of every number a technique reads from the route file. The numbers belong to the file (a datapack can retune them), but a technique must never throw
 * or cast for nothing because a key is missing, so each reads the registry first and this table second; a unit test compares the table with the shipped file key by key. (2) The shapes of the
 * techniques that need geometry: the fan of the Lance ({@link #lanceAngles}), the standing wall of the Shield ({@link Wall}, an oriented box, because a wall faces the way the caster faced and
 * a plain bounding box would be a huge square for a diagonal wall), the timing of the Saucer's cuts so each foe is hit when the disc reaches it and not before ({@link #saucerDelay}), and how
 * the Eagles are shared out among the foes so that no foe takes more than two ({@link #eagleAssignment}). (3) The construct budget ({@link #hasRoom}, {@link #record}): the owner allows at most
 * four short-lived constructs at once and none longer than ten seconds, and the ledger of the live ones is a plain array of expiry ticks, so it can be kept in the player's own saved data
 * (no static state to clear) and checked by a test. (4) How a chill adds up without ever passing what the body can take ({@link #chilled}). (5) {@link #problems}: the shape and budget check of
 * the whole route, which both the unit test and the in-game self-test run on the real data.
 */
public final class IceNumbers {
    private IceNumbers() {}

    private static final Map<String, Map<String, Double>> FALLBACK = new HashMap<>();

    private static void node(String id, Object... keyValue) {
        Map<String, Double> m = new HashMap<>();
        for (int i = 0; i + 1 < keyValue.length; i += 2) m.put((String) keyValue[i], ((Number) keyValue[i + 1]).doubleValue());
        FALLBACK.put(id, m);
    }

    static {
        node(IceRoute.LANCE, "damage", 5, "range", 14, "count", 1, "spread", 0, "width", 0.9, "chill_seconds", 1, "targets", 4, "cost", 0.02, "cooldown", 80);
        node(IceRoute.FLOOR, "radius", 3.5, "life", 80, "slow_amp", 0, "chill", 20, "reach", 8, "targets", 6, "cost", 0.025, "cooldown", 200);
        node(IceRoute.SHIELD, "width", 4, "height", 3, "life", 60, "absorb", 4, "cost", 0.03, "cooldown", 300);
        node(IceRoute.CANNON, "damage", 4, "shots", 3, "interval", 18, "range", 12, "life", 60, "reach", 4, "cost", 0.03, "cooldown", 260);
        node(IceRoute.FROST_BODY, "chill_seconds", 0, "chill_ticks", 0, "rest", 0);
        node(IceRoute.SAUCER, "damage", 5, "range", 12, "width", 1.0, "returns", 0, "speed", 1.4, "targets", 6, "cost", 0.025, "cooldown", 100);
        node(IceRoute.EAGLE, "damage", 4, "count", 2, "range", 16, "chill_seconds", 1, "speed", 1.0, "cost", 0.03, "cooldown", 140);
        node(IceRoute.HAMMER, "damage", 8, "radius", 2.5, "windup", 16, "range", 14, "slow_seconds", 2, "targets", 6, "cost", 0.03, "cooldown", 180);
        node(IceRoute.EXCALIBUR, "damage", 20, "range", 9, "half_angle", 50, "windup", 18, "slow_seconds", 4, "targets", 8, "cost", 0.06, "cooldown", 1100);
    }

    /** The shipped level-1 value of {@code key} for {@code nodeId}; 0 when the pair is unknown (a test makes sure no pair a technique reads is unknown). */
    public static double fallback(String nodeId, String key) {
        Map<String, Double> m = FALLBACK.get(nodeId);
        Double v = m == null ? null : m.get(key);
        return v == null ? 0.0 : v;
    }

    public static Set<String> fallbackKeys(String nodeId) {
        Map<String, Double> m = FALLBACK.get(nodeId);
        return m == null ? Set.of() : new HashSet<>(m.keySet());
    }

    // ------------------------------------------------------------------ the lance
    /** The yaw offsets in degrees of {@code count} spears fanned {@code spread} degrees apart and centred on the gaze (one spear: straight ahead). */
    public static double[] lanceAngles(int count, double spread) {
        int n = Math.max(1, count);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = (i - (n - 1) / 2.0) * spread;
        return out;
    }

    // ------------------------------------------------------------------ the saucer
    /** Ticks until the disc, flying at {@code speed} blocks a tick, reaches a foe {@code along} blocks down a path of {@code reach} blocks: on the way out, or on the way back (it turns at the end). */
    public static int saucerDelay(double along, double reach, double speed, boolean back) {
        double distance = back ? 2.0 * reach - along : along;
        return (int) Math.ceil(Math.max(0.0, distance) / Math.max(0.1, speed));
    }

    /** The distance of a point along the unit direction (dx, dy, dz) from the start (sx, sy, sz), clamped to the path (0 up to {@code reach}). */
    public static double along(double sx, double sy, double sz, double dx, double dy, double dz, double px, double py, double pz, double reach) {
        double t = (px - sx) * dx + (py - sy) * dy + (pz - sz) * dz;
        return Math.max(0.0, Math.min(reach, t));
    }

    // ------------------------------------------------------------------ the eagle
    /** Which foe each eagle flies at: the eagles take the foes in turn (nearest first), at most {@code perFoe} each, so with few foes some eagles are not launched. */
    public static int[] eagleAssignment(int eagles, int foes, int perFoe) {
        if (eagles <= 0 || foes <= 0 || perFoe <= 0) return new int[0];
        int total = Math.min(eagles, foes * perFoe);
        int[] out = new int[total];
        for (int k = 0; k < total; k++) out[k] = k % foes;
        return out;
    }

    // ------------------------------------------------------------------ the shield
    /**
     * A standing wall: a box centred on ({@code cx}, {@code cz}) at ground height {@code cy}, facing the unit direction ({@code fx}, {@code fz}), {@code halfWidth} to each side across the
     * facing and {@code halfDepth} each way along it, {@code height} tall. It is an ORIENTED box: the wall turns with the caster.
     */
    public record Wall(double cx, double cy, double cz, double fx, double fz, double halfWidth, double halfDepth, double height) {
        /** Is the point inside the wall? A little below the ground height counts, so a spear stuck in the floor line is still stopped. */
        public boolean contains(double x, double y, double z) {
            double dx = x - cx, dz = z - cz;
            double along = dx * fx + dz * fz;
            double across = -dx * fz + dz * fx;
            return Math.abs(across) <= halfWidth && Math.abs(along) <= halfDepth && y >= cy - 0.5 && y <= cy + height;
        }

        /**
         * Does the straight move from the first point to the second touch the wall? It is sampled every 0.4 blocks (both ends included), which is finer than the wall is thick, so an arrow
         * that flies several blocks a tick cannot slip through between two checks: the wall tests where the arrow is about to be, not only where it is.
         */
        public boolean crossedBy(double x0, double y0, double z0, double x1, double y1, double z1) {
            double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / 0.4));
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                if (contains(x0 + dx * t, y0 + dy * t, z0 + dz * t)) return true;
            }
            return false;
        }

        /** The radius of a circle round the centre that surely holds the whole wall (to search for projectiles). */
        public double reach() { return Math.hypot(halfWidth, halfDepth) + 1.0; }
    }

    // ------------------------------------------------------------------ the construct budget
    /** A construct never stands longer than the 10 seconds of the owner's limit, nor less than a tick. */
    public static int clampLife(int life) { return Math.max(1, Math.min(life, FtBudget.LIFE_MAX)); }

    /** How many of the recorded constructs are still standing at game tick {@code now}. */
    public static int liveCount(long[] liveUntil, long now) {
        int n = 0;
        for (long until : liveUntil) if (until > now) n++;
        return n;
    }

    /** Is there room for another construct (fewer than {@link FtBudget#SPAWNS_MAX} stand)? */
    public static boolean hasRoom(long[] liveUntil, long now) { return liveCount(liveUntil, now) < FtBudget.SPAWNS_MAX; }

    /** The ledger after a new construct of {@code life} ticks is built at {@code now}: the ones that have fallen are forgotten, the new one is added. */
    public static long[] record(long[] liveUntil, long now, int life) {
        long[] kept = Arrays.stream(liveUntil).filter(until -> until > now).toArray();
        long[] out = Arrays.copyOf(kept, kept.length + 1);
        out[kept.length] = now + clampLife(life);
        return out;
    }

    // ------------------------------------------------------------------ the chill
    /** A frozen-tick count after adding {@code add} to {@code current}, never above what freezes a body completely ({@code required}). */
    public static int chilled(int current, int add, int required) { return Math.min(Math.max(1, required), Math.max(0, current) + Math.max(0, add)); }

    // ------------------------------------------------------------------ the whole route
    /** The nodes of the standing branch and of the moving branch (the Secret Art must need one of each). */
    public static final Set<String> STANDING = Set.of(IceRoute.FLOOR, IceRoute.SHIELD, IceRoute.CANNON, IceRoute.FROST_BODY), MOVING = Set.of(IceRoute.SAUCER, IceRoute.EAGLE, IceRoute.HAMMER);

    /** Everything wrong with a loaded Ice-Make route: the shared shape and budget law, the ids, the fallback table, and the two-branch Secret Art. Empty when all is well. */
    public static List<String> problems(SkillRoute route) {
        List<String> bad = new ArrayList<>(FtBudget.problems(route));
        if (!IceRoute.ID.equals(route.id())) bad.add("the route id is " + route.id() + ", not " + IceRoute.ID);
        if (!IceRoute.CLASS_ID.equals(route.classId())) bad.add("the class id is " + route.classId() + ", not " + IceRoute.CLASS_ID);
        if (!"nusmp:ice_make_book".equals(route.book())) bad.add("the book is " + route.book());
        for (SkillNode n : route.nodes()) {
            Set<String> keys = fallbackKeys(n.id());
            if (!keys.equals(n.values().keySet())) bad.add(n.id() + ": the fallback keys " + keys + " differ from the file's " + n.values().keySet());
            else for (String k : keys) {
                if (Math.abs(fallback(n.id(), k) - n.value(k, 1, Double.NaN)) > 1e-9) bad.add(n.id() + "." + k + ": the fallback " + fallback(n.id(), k) + " is not the file's level 1 value " + n.value(k, 1, Double.NaN));
            }
        }
        SkillNode secret = route.node(IceRoute.EXCALIBUR);
        if (secret == null) bad.add("the Secret Art " + IceRoute.EXCALIBUR + " is missing");
        else {
            boolean standing = false, moving = false;
            for (SkillNode.Parent p : secret.parents()) {
                standing |= STANDING.contains(p.node());
                moving |= MOVING.contains(p.node());
            }
            if (!standing || !moving) bad.add(IceRoute.EXCALIBUR + " must need one node of the standing branch and one of the moving branch");
        }
        return bad;
    }
}
