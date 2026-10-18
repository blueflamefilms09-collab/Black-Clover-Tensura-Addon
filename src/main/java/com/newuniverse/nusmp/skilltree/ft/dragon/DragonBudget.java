package com.newuniverse.nusmp.skilltree.ft.dragon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the balance rules of the skill tree (design.md section 6: raw damage per hit 4..14 and at most 30 for a Secret Art, a cooldown of at least 3 s and 45 s for a Secret
 * Art, a cost of 1..4 % of the magicules and at most 8 % for a Secret Art, at most 12 targets, a heal under a quarter of the health) only hold if somebody checks every number
 * of every route against them, and nothing can be played here to feel it. This PURE class (no Minecraft or NeoForge type, so a plain JUnit test and the in-game self-test can
 * both call it) turns the rules into one function from a node's {@code values} tables to a list of complaints. It runs at build time on the shipped JSON and on the code
 * defaults, and at server start on whatever the datapack holds now, so a retune that breaks the budget is named instead of discovered in a fight. It also pins the one rule
 * the owner's sentence "the SP you invest changes the technique" needs: a higher level is never worse (every power number rises, every delay falls). The passive limits
 * (heal, recharge and recoil per bite, evade chance) are this builder's own design, written down here so they are as testable as the owner's.
 */
public final class DragonBudget {
    private DragonBudget() {}

    /** What a node is: an ordinary technique, a Secret Art (the owner's higher caps), or a passive (no cooldown, no cost, small per-bite numbers). */
    public enum Kind { ACTIVE, SECRET_ART, PASSIVE }

    /** Above this many raw damage a technique must show a tell (design.md section 6 and the brief). */
    public static final double TELL_ABOVE_RAW = 8.0;
    public static final double MIN_RAW = 4.0, MAX_RAW = 14.0, SECRET_MAX_RAW = 30.0;
    public static final int MIN_COOLDOWN_TICKS = 60, SECRET_MIN_COOLDOWN_TICKS = 900;
    public static final double MIN_COST_PCT = 1.0, MAX_COST_PCT = 4.0, SECRET_MAX_COST_PCT = 8.0;
    public static final int MAX_TARGETS = 12;
    /** This builder's cap on the follow-up hits a technique schedules (the pulses of a barrage, the bolts of a storm): the timer queue is shared by every cast. */
    public static final int MAX_FOLLOW_UPS = 8;
    /** Two hits on the same foe closer than this are swallowed by the vanilla hurt-frames (a hit lands when the foe's frame counter is 10 or less; 11 keeps a margin). */
    public static final int MIN_PULSE_GAP_TICKS = 11;
    /** The owner's cap for an active heal, in percent of the maximum health per cast. */
    public static final double MAX_HEAL_PCT = 25.0;
    /** This builder's caps for the passives: percent of max health or magicules per bite (one bite a second), the recoil in raw damage, the evade chance in percent. */
    public static final double PASSIVE_MAX_HEAL_PCT = 5.0, PASSIVE_MAX_RECHARGE_PCT = 3.0, PASSIVE_MAX_RECOIL = 6.0, MAX_EVADE_PCT = 25.0, MAX_EAT_RECHARGE_PCT = 8.0;

    /** The tables that are a hit's raw damage (each entry is checked against the caps of the node's kind). */
    private static final Set<String> RAW_KEYS = Set.of("damage", "hit_damage", "final_damage", "splash_damage", "chain_damage");
    /** The tables where a bigger number is weaker (a longer wait), so a higher level must not raise them. */
    private static final Set<String> FALLING_KEYS = Set.of("cooldown", "windup", "delay", "gap", "icd", "evade_icd");
    /** The tables that may move either way with the level (the price of more power). */
    private static final Set<String> FREE_KEYS = Set.of("cost_pct");

    /** True when a hit of {@code raw} damage must be announced by a tell. */
    public static boolean needsTell(double raw) { return raw > TELL_ABOVE_RAW; }

    /** True when {@code key} names a table of raw hit damage. */
    public static boolean isRawKey(String key) { return RAW_KEYS.contains(key); }

    /**
     * Checks one node's tables.
     *
     * @param node     the node id (only used in the messages)
     * @param kind     what the node is
     * @param maxLevel the node's maximum level: every table must have exactly this many entries, one per level
     * @param values   the node's {@code values} (key to the number at each level, index 0 is level 1)
     * @return the complaints, empty when the node obeys the budget
     */
    public static List<String> check(String node, Kind kind, int maxLevel, Map<String, List<Double>> values) {
        List<String> bad = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            bad.add(node + ": no values at all");
            return bad;
        }
        if (kind != Kind.PASSIVE) {
            if (!values.containsKey("cooldown")) bad.add(node + ": an active technique needs a cooldown table");
            if (!values.containsKey("cost_pct")) bad.add(node + ": an active technique needs a cost_pct table");
            boolean raw = false;
            for (String k : values.keySet()) raw |= RAW_KEYS.contains(k);
            if (!raw) bad.add(node + ": an active technique needs a damage table");
        }
        for (Map.Entry<String, List<Double>> e : values.entrySet()) {
            String key = e.getKey();
            List<Double> t = e.getValue();
            String at = node + "." + key;
            if (t == null || t.isEmpty()) { bad.add(at + ": empty table"); continue; }
            if (t.size() != maxLevel) bad.add(at + ": " + t.size() + " entries for max level " + maxLevel);
            boolean sane = true;
            for (double v : t) if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) { bad.add(at + ": " + v + " is not a finite non-negative number"); sane = false; break; }
            if (!sane) continue;
            for (int i = 0; i < t.size(); i++) rangeRules(at + "[" + (i + 1) + "]", key, kind, t.get(i), bad);
            monotone(at, key, t, bad);
        }
        return bad;
    }

    private static void rangeRules(String at, String key, Kind kind, double v, List<String> bad) {
        if (RAW_KEYS.contains(key)) {
            double cap = kind == Kind.SECRET_ART ? SECRET_MAX_RAW : MAX_RAW;
            if (v < MIN_RAW || v > cap) bad.add(at + ": raw damage " + v + " is outside " + MIN_RAW + ".." + cap);
            return;
        }
        switch (key) {
            case "recoil" -> { if (v < 1.0 || v > PASSIVE_MAX_RECOIL) bad.add(at + ": recoil " + v + " is outside 1.." + PASSIVE_MAX_RECOIL); }
            case "cooldown" -> {
                int min = kind == Kind.SECRET_ART ? SECRET_MIN_COOLDOWN_TICKS : MIN_COOLDOWN_TICKS;
                if (kind != Kind.PASSIVE && v < min) bad.add(at + ": cooldown " + v + " ticks is under " + min);
            }
            case "cost_pct" -> {
                double max = kind == Kind.SECRET_ART ? SECRET_MAX_COST_PCT : MAX_COST_PCT;
                if (v < MIN_COST_PCT || v > max) bad.add(at + ": cost " + v + "% is outside " + MIN_COST_PCT + ".." + max);
            }
            case "targets" -> { if (v < 1 || v > MAX_TARGETS) bad.add(at + ": " + v + " targets is outside 1.." + MAX_TARGETS); }
            case "chain_targets" -> { if (v > MAX_TARGETS) bad.add(at + ": " + v + " chained targets is over " + MAX_TARGETS); }
            case "hits", "strikes" -> { if (v < 1 || v > MAX_FOLLOW_UPS) bad.add(at + ": " + v + " follow-up hits is outside 1.." + MAX_FOLLOW_UPS); }
            case "heal_pct" -> {
                double max = kind == Kind.PASSIVE ? PASSIVE_MAX_HEAL_PCT : MAX_HEAL_PCT;
                if (v > max) bad.add(at + ": heal " + v + "% is over " + max);
            }
            case "recharge_pct" -> { if (v > PASSIVE_MAX_RECHARGE_PCT) bad.add(at + ": recharge " + v + "% a second is over " + PASSIVE_MAX_RECHARGE_PCT); }
            case "eat_recharge_pct" -> { if (v > MAX_EAT_RECHARGE_PCT) bad.add(at + ": eaten recharge " + v + "% is over " + MAX_EAT_RECHARGE_PCT); }
            case "eat_heal_ratio" -> { if (v > 1.0) bad.add(at + ": " + v + " of the eaten damage healed is over 1"); }
            case "evade_pct" -> { if (v > MAX_EVADE_PCT) bad.add(at + ": evade " + v + "% is over " + MAX_EVADE_PCT); }
            case "evade_icd", "icd" -> { if (v < 20) bad.add(at + ": " + v + " ticks between triggers is under 20"); }
            case "gap" -> { if (v < MIN_PULSE_GAP_TICKS) bad.add(at + ": " + v + " ticks between two hits on the same foes is under " + MIN_PULSE_GAP_TICKS + " (the vanilla hurt-frames would swallow the second)"); }
            case "speed_amp" -> { if (v > 2) bad.add(at + ": speed amplifier " + v + " is over 2"); }
            default -> { }
        }
    }

    private static void monotone(String at, String key, List<Double> t, List<String> bad) {
        if (FREE_KEYS.contains(key)) return;
        boolean falling = FALLING_KEYS.contains(key);
        for (int i = 1; i < t.size(); i++) {
            double a = t.get(i - 1), b = t.get(i);
            if (falling ? b > a : b < a) {
                bad.add(at + ": level " + (i + 1) + " (" + b + ") is " + (falling ? "slower" : "weaker") + " than level " + i + " (" + a + ")");
                return;
            }
        }
    }
}
