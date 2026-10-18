package com.newuniverse.nusmp.skilltree.ft.sky;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.kit.FtBudget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: everything about the Sky Dragon Slayer that is arithmetic and not the game lives here as pure functions, so it can be tested without a server and so the rules the owner asked
 * for are written down once. First, the FALLBACK table: the level-1 value of every number a technique reads from the route file. The numbers themselves belong to the file (a
 * datapack can retune them), but a technique must never throw or cast for nothing because a key is missing, so each reads the registry first and this table second; a unit test compares the
 * table with the shipped file key by key, so the two cannot drift apart. Second, the healing rules: a heal is a share of the TARGET's maximum health capped at the 25 percent of the balance
 * law whatever the data says ({@link #healAmount}), and when more allies are wounded than one cast can mend the most wounded come first ({@link #mostWounded}), because spending a cast on the
 * nearly healthy while a friend is dying is the one thing a healer must not do. Third, the cleanse order ({@link #pickStrips}): nausea first (the Troia of the story cures the motion
 * sickness), then poison and the other ills, so a low level still removes the worst thing. Fourth, the timeline of the Secret Art ({@link #drillStep}): pulses ten ticks apart, then
 * the burst, and the radius the wall visibly shrinks to ({@link #squeezeRadius}). Last, {@link #problems}: the shape and budget check of the whole route, which both the unit test and the
 * in-game self-test run on the real data.
 */
public final class SkyNumbers {
    private SkyNumbers() {}

    /** Ticks between the pulses of the Sky Drill (and between its last pulse and its burst). */
    public static final int DRILL_INTERVAL = 10;

    /** {@link #drillStep} answer for a tick on which the drill does nothing. A pulse answers its number (1 up), the burst answers pulses + 1. */
    public static final int DRILL_NONE = -1;

    /** How far the wall of the drill has shrunk when it bursts, as a share of its starting radius (the foes are squeezed, never hit outside the circle they started in). */
    public static final double DRILL_SQUEEZE = 0.4;

    /** The caster's share of the healing the drill gives its allies: sky magic mends others better than the one who casts it. */
    public static final double DRILL_SELF = 0.5;

    private static final Map<String, Map<String, Double>> FALLBACK = new HashMap<>();

    private static void node(String id, Object... keyValue) {
        Map<String, Double> m = new HashMap<>();
        for (int i = 0; i + 1 < keyValue.length; i += 2) m.put((String) keyValue[i], ((Number) keyValue[i + 1]).doubleValue());
        FALLBACK.put(id, m);
    }

    static {
        node(SkyRoute.ROAR, "damage", 5, "range", 7, "half_angle", 20, "knockback", 0.6, "heal", 0, "targets", 6, "cost", 0.02, "cooldown", 100);
        node(SkyRoute.WING_ATTACK, "damage", 4, "radius", 3.5, "knockback", 0.8, "targets", 6, "cost", 0.02, "cooldown", 90);
        node(SkyRoute.WAVE_WIND, "damage", 4, "radius", 1.8, "distance", 9, "hits", 2, "targets", 8, "cost", 0.03, "cooldown", 160);
        node(SkyRoute.CUSHION, "fall_reduction", 0.4);
        node(SkyRoute.BREATH, "heal", 0.08, "radius", 6, "targets", 4, "self_share", 0.5, "cost", 0.03, "cooldown", 200);
        node(SkyRoute.TROIA, "strips", 1, "radius", 5, "targets", 3, "extinguish", 0, "cost", 0.02, "cooldown", 200);
        node(SkyRoute.ENCHANT, "seconds", 10, "speed_amp", 0, "radius", 6, "targets", 4, "cost", 0.03, "cooldown", 400);
        node(SkyRoute.AURA, "radius", 6);
        node(SkyRoute.SKY_DRILL, "radius", 5, "windup", 10, "pulses", 3, "pulse_damage", 4, "burst_damage", 12, "heal", 0.12, "targets", 8, "cost", 0.06, "cooldown", 1200);
    }

    /** The shipped level-1 value of {@code key} for {@code nodeId}; 0 when the pair is unknown (a test makes sure no pair a technique reads is unknown). */
    public static double fallback(String nodeId, String key) {
        Map<String, Double> m = FALLBACK.get(nodeId);
        Double v = m == null ? null : m.get(key);
        return v == null ? 0.0 : v;
    }

    /** The node ids that have a fallback table, and the keys of one (for the tests). */
    public static Set<String> fallbackNodes() { return new HashSet<>(FALLBACK.keySet()); }

    public static Set<String> fallbackKeys(String nodeId) {
        Map<String, Double> m = FALLBACK.get(nodeId);
        return m == null ? Set.of() : new HashSet<>(m.keySet());
    }

    // ------------------------------------------------------------------ healing
    /** The hit points one heal restores: {@code share} of the target's maximum health, never more than the 25 percent of the balance law, never negative. */
    public static float healAmount(float maxHealth, double share) {
        double s = Math.max(0.0, Math.min(share, FtBudget.HEAL_MAX));
        return (float) (Math.max(0f, maxHealth) * s);
    }

    /**
     * Which of the candidates a heal should reach: only the wounded (health fraction below 1), the most wounded first, the nearer first among equals, at most {@code cap}. The arrays are
     * parallel (index i is one candidate); the answer is a list of those indices in the order to heal them.
     */
    public static int[] mostWounded(double[] healthFraction, double[] distance, int cap) {
        List<Integer> wounded = new ArrayList<>();
        for (int i = 0; i < healthFraction.length; i++) if (healthFraction[i] < 1.0 - 1e-6) wounded.add(i);
        wounded.sort(Comparator.<Integer>comparingDouble(i -> healthFraction[i]).thenComparingDouble(i -> distance[i]).thenComparingInt(i -> i));
        int n = Math.max(0, Math.min(cap, wounded.size()));
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = wounded.get(i);
        return out;
    }

    // ------------------------------------------------------------------ cleansing
    /** How urgently an effect should be removed by Troia (lower first), by its registry path: nausea is the one the spell is named for, then the poisons, then the rest. */
    public static int cleansePriority(String effectPath) {
        return switch (effectPath) {
            case "nausea" -> 0;
            case "poison" -> 1;
            case "wither" -> 2;
            case "blindness" -> 3;
            case "darkness" -> 4;
            case "slowness" -> 5;
            case "weakness" -> 6;
            case "hunger" -> 7;
            case "mining_fatigue" -> 8;
            case "levitation" -> 9;
            default -> 50;
        };
    }

    /** Of the harmful effects a creature has (their registry paths), the {@code count} to remove, most urgent first. */
    public static List<String> pickStrips(List<String> harmfulPaths, int count) {
        List<String> sorted = new ArrayList<>(harmfulPaths);
        sorted.sort(Comparator.comparingInt(SkyNumbers::cleansePriority));
        return new ArrayList<>(sorted.subList(0, Math.max(0, Math.min(count, sorted.size()))));
    }

    // ------------------------------------------------------------------ the Secret Art
    /**
     * What the Sky Drill does on a tick of its zone (the zone starts when the wind-up ends): a pulse number (1 to {@code pulses}) at age 0 and at every {@link #DRILL_INTERVAL} after
     * that, the burst ({@code pulses + 1}) one interval after the last pulse, and {@link #DRILL_NONE} on any other age.
     */
    public static int drillStep(int age, int pulses) {
        if (age < 0 || age % DRILL_INTERVAL != 0) return DRILL_NONE;
        int step = age / DRILL_INTERVAL + 1;
        return step <= pulses + 1 ? step : DRILL_NONE;
    }

    /** The life of the drill's zone in ticks: just long enough for its burst to be the last step. */
    public static int drillLife(int pulses) { return Math.max(0, pulses) * DRILL_INTERVAL + 1; }

    /** The radius the wall is drawn at on pulse {@code step} (1 up to {@code pulses + 1} for the burst): the full radius first, shrinking evenly to the squeezed radius at the burst. */
    public static double squeezeRadius(double radius, int step, int pulses) {
        int s = Math.max(1, Math.min(step, pulses + 1));
        return radius * (1.0 - DRILL_SQUEEZE * (s - 1) / Math.max(1, pulses));
    }

    // ------------------------------------------------------------------ passives
    /** The factor a fall's damage is multiplied by for a given reduction (0.4 means 40 percent less); clamped so a bad table cannot heal the player by falling. */
    public static float fallMultiplier(double reduction) { return (float) (1.0 - Math.max(0.0, Math.min(1.0, reduction))); }

    // ------------------------------------------------------------------ the whole route
    /** The nodes of the offence branch and of the healing branch (the Secret Art must need one of each). */
    public static final Set<String> OFFENCE = Set.of(SkyRoute.WING_ATTACK, SkyRoute.WAVE_WIND), HEALING = Set.of(SkyRoute.BREATH, SkyRoute.TROIA, SkyRoute.ENCHANT);

    /** Everything wrong with a loaded Sky Dragon Slayer route: the shared shape and budget law, the ids, the fallback table, and the two-branch Secret Art. Empty when all is well. */
    public static List<String> problems(SkillRoute route) {
        List<String> bad = new ArrayList<>(FtBudget.problems(route));
        if (!SkyRoute.ID.equals(route.id())) bad.add("the route id is " + route.id() + ", not " + SkyRoute.ID);
        if (!SkyRoute.CLASS_ID.equals(route.classId())) bad.add("the class id is " + route.classId() + ", not " + SkyRoute.CLASS_ID);
        if (!"nusmp:sky_dragon_slayer_book".equals(route.book())) bad.add("the book is " + route.book());
        for (SkillNode n : route.nodes()) {
            Set<String> keys = fallbackKeys(n.id());
            if (!keys.equals(n.values().keySet())) bad.add(n.id() + ": the fallback keys " + keys + " differ from the file's " + n.values().keySet());
            else for (String k : keys) {
                if (Math.abs(fallback(n.id(), k) - n.value(k, 1, Double.NaN)) > 1e-9) bad.add(n.id() + "." + k + ": the fallback " + fallback(n.id(), k) + " is not the file's level 1 value " + n.value(k, 1, Double.NaN));
            }
        }
        SkillNode drill = route.node(SkyRoute.SKY_DRILL);
        if (drill == null) bad.add("the Secret Art " + SkyRoute.SKY_DRILL + " is missing");
        else {
            boolean offence = false, healing = false;
            for (SkillNode.Parent p : drill.parents()) {
                offence |= OFFENCE.contains(p.node());
                healing |= HEALING.contains(p.node());
            }
            if (!offence || !healing) bad.add(SkyRoute.SKY_DRILL + " must need one node of the offence branch and one of the healing branch");
        }
        return bad;
    }
}
